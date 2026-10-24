package main

import (
	"testing"
	"time"
)

func montaExemplo(t *testing.T) *Montador {
	t.Helper()
	f := &Fonte{Arquivo: "testdata/exemplo.json"}
	ev, _, err := f.Busca(time.Unix(0, 0))
	if err != nil {
		t.Fatal(err)
	}
	m := NovoMontador()
	if n := m.Adiciona(ev); n != len(ev) {
		t.Fatalf("novos = %d, esperado %d", n, len(ev))
	}
	return m
}

func TestStackTraceViraContinuacao(t *testing.T) {
	m := montaExemplo(t)
	var erro *Registro
	for _, r := range m.Registros {
		if r.Logger == "g.j.c.a.out.client.GoogleBooksClient" {
			erro = r
		}
	}
	if erro == nil || len(erro.Continua) != 3 {
		t.Fatalf("esperava 3 linhas de continuação, veio %+v", erro)
	}
	if erro.Controller != "LeituraClubeController" {
		t.Errorf("linha sem acesso deveria herdar o controller pelo requestId, veio %q", erro.Controller)
	}
}

func TestControllerPorHandlerRotaERequest(t *testing.T) {
	m := montaExemplo(t)
	casos := map[string]string{
		"a1b2c3d4": "ClubeController",   // handler=
		"cc11dd22": "ConviteController", // sem handler: pela rota /convites
		"bf167f53": "OtpController",     // /auth/otp antes de /auth
		"12345678": "ClubeController",   // rota com UUID
	}
	for _, r := range m.Registros {
		if esperado, ok := casos[r.RequestID]; ok && r.Controller != esperado {
			t.Errorf("req %s (%s): controller %q, esperado %q", r.RequestID, r.Mensagem, r.Controller, esperado)
		}
	}
}

func TestAcessoParseado(t *testing.T) {
	m := montaExemplo(t)
	var ac *Acesso
	for _, r := range m.Registros {
		if r.RequestID == "99aa88bb" && r.Acesso != nil {
			ac = r.Acesso
		}
	}
	if ac == nil || ac.Status != 502 || ac.Duracao != 812 || ac.Metodo != "GET" || ac.Handler != "LeituraClubeController#buscarLivros" {
		t.Fatalf("acesso errado: %+v", ac)
	}
}

func TestDuplicadosIgnoradosNoModoSeguir(t *testing.T) {
	m := montaExemplo(t)
	antes := len(m.Registros)
	f := &Fonte{Arquivo: "testdata/exemplo.json"}
	ev, _, _ := f.Busca(time.Unix(0, 0))
	if n := m.Adiciona(ev); n != 0 || len(m.Registros) != antes {
		t.Fatalf("reenvio dos mesmos eventos criou %d novos", n)
	}
}

func TestFiltros(t *testing.T) {
	m := montaExemplo(t)
	conta := func(f Filtro) int { return len(Filtra(m.Registros, &f)) }

	if n := conta(Filtro{NivelMinimo: Error}); n != 2 {
		t.Errorf("ERROR: %d, esperado 2", n)
	}
	if n := conta(Filtro{NivelMinimo: SemNivel, Controller: "ConviteController"}); n != 4 {
		t.Errorf("ConviteController: %d, esperado 4", n)
	}
	if n := conta(Filtro{NivelMinimo: SemNivel, Busca: "JÁ FOI ACEITO"}); n != 1 {
		t.Errorf("busca sem diferenciar maiúsculas: %d", n)
	}
	if n := conta(Filtro{NivelMinimo: SemNivel, Busca: "re:google.*duna"}); n != 1 {
		t.Errorf("busca regex: %d", n)
	}
	if n := conta(Filtro{NivelMinimo: SemNivel, Busca: "GoogleBooksClient.buscar"}); n != 1 {
		t.Errorf("busca dentro do stack trace: %d", n)
	}
	if n := conta(Filtro{NivelMinimo: SemNivel, StatusMin: 400}); n != 3 {
		t.Errorf("status>=400: %d, esperado 3", n)
	}
	if n := conta(Filtro{NivelMinimo: SemNivel, Controller: semController}); n != 2 {
		t.Errorf("sem requisição: %d, esperado 2 (boot e outbox)", n)
	}
	if n := conta(Filtro{NivelMinimo: SemNivel, Stream: "caddy"}); n != 1 {
		t.Errorf("caddy: %d", n)
	}
}

func TestNomeController(t *testing.T) {
	for in, out := range map[string]string{"Clube": "ClubeController", "clubecontroller": "ClubeController", "OtpController": "OtpController"} {
		if got := nomeController(in); got != out {
			t.Errorf("%s -> %s, esperado %s", in, got, out)
		}
	}
}

func TestNormalizaRota(t *testing.T) {
	if got := normalizaRota("/clubes/3f2a9c1e-1111-2222-3333-444455556666/convite-link?x=1"); got != "/clubes/{id}/convite-link" {
		t.Errorf("uuid: %s", got)
	}
	if got := normalizaRota("/usuarios/14/perfil"); got != "/usuarios/{id}/perfil" {
		t.Errorf("numérico: %s", got)
	}
	if got := normalizaRota("/convites/link/kG6uqCMm2Z"); got != "/convites/link/{id}" {
		t.Errorf("código do link: %s", got)
	}
}
