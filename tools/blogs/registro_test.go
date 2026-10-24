package main

import (
	"encoding/json"
	"os"
	"strings"
	"testing"
)

func carregaExemplo(t *testing.T) *Montador {
	t.Helper()
	dados, err := os.ReadFile("testdata/exemplo.json")
	if err != nil {
		t.Fatal(err)
	}
	var r struct {
		Events []Evento `json:"events"`
	}
	if err := json.Unmarshal(dados, &r); err != nil {
		t.Fatal(err)
	}
	m := NovoMontador(0)
	m.Adiciona(r.Events)
	return m
}

func busca(m *Montador, trecho string) *Registro {
	for _, r := range m.Registros {
		if strings.Contains(r.Mensagem, trecho) {
			return r
		}
	}
	return nil
}

func TestLinhaDoBooklit(t *testing.T) {
	m := carregaExemplo(t)
	r := busca(m, "GET /clubes -> 200")
	if r == nil {
		t.Fatal("linha de acesso não encontrada")
	}
	if r.Hora != "21:10:05" || r.Nivel != Info || r.RequestID != "a1b2c3d4" || r.Classe != "RequestLoggingFilter" {
		t.Errorf("campos errados: %+v", r)
	}
	if r.Controller != "ClubeController" {
		t.Errorf("controller = %q", r.Controller)
	}
	if boot := busca(m, "Started ClubeLivroApplication"); boot.RequestID != "" {
		t.Errorf("requestId de ________ deveria ser vazio, veio %q", boot.RequestID)
	}
}

func TestStackTraceAgrupado(t *testing.T) {
	m := carregaExemplo(t)
	r := busca(m, "Erro inesperado em POST /votos")
	if r == nil || r.Nivel != Error {
		t.Fatalf("registro de erro não encontrado: %+v", r)
	}
	if len(r.Continua) != 9 {
		t.Fatalf("esperava 9 linhas de stack, veio %d: %q", len(r.Continua), r.Continua)
	}
	// A linha seguinte do log não pode ter virado continuação.
	if busca(m, "POST /votos -> 500") == nil {
		t.Error("a linha de acesso depois do stack sumiu")
	}
}

func TestControllerHerdadoPeloRequestId(t *testing.T) {
	m := carregaExemplo(t)
	// O handler de erro loga antes do log de acesso; recebe o controller quando ele chega.
	if r := busca(m, "Erro inesperado em POST /votos"); r.Controller != "VotoController" {
		t.Errorf("controller = %q", r.Controller)
	}
	if !strings.Contains(busca(m, "Erro inesperado").TextoBusca(), "votocontroller") {
		t.Error("buscar pelo controller deveria achar as linhas da requisição")
	}
}

func TestCaddy(t *testing.T) {
	m := carregaExemplo(t)
	r := busca(m, "GET /clubes -> 200 (41ms)")
	if r == nil {
		t.Fatal("log de acesso do caddy não foi resumido")
	}
	if r.Classe != "caddy:access" || r.Stream != "caddy" {
		t.Errorf("caddy: %+v", r)
	}
	if r := busca(m, "serving initial configuration"); r.Classe != "caddy" {
		t.Errorf("classe = %q", r.Classe)
	}
}

func TestEventosRepetidosIgnorados(t *testing.T) {
	m := NovoMontador(0)
	ev := []Evento{{ID: "1", Stream: "api", Timestamp: 1, Mensagem: "10:00:00.000  INFO [________] a.B   oi"}}
	if n := m.Adiciona(ev); n != 1 {
		t.Fatalf("novos = %d", n)
	}
	if n := m.Adiciona(ev); n != 0 || len(m.Registros) != 1 {
		t.Fatalf("o polling repete eventos; não podem duplicar (novos=%d, registros=%d)", n, len(m.Registros))
	}
}

func TestModos(t *testing.T) {
	m := carregaExemplo(t)
	conta := func(modo Modo) (n int) {
		for _, r := range m.Registros {
			if Mostra(r, modo) {
				n++
			}
		}
		return
	}
	if conta(ModoTodos) != len(m.Registros) {
		t.Error("modo todos deveria mostrar tudo")
	}
	for _, r := range m.Registros {
		if Mostra(r, ModoInfo) && r.Nivel != Info {
			t.Errorf("modo info mostrou %s", r.Nivel)
		}
		if Mostra(r, ModoErros) && r.Nivel < Warn {
			t.Errorf("modo erros mostrou %s: %s", r.Nivel, r.Mensagem)
		}
	}
}

func TestResumoDoStack(t *testing.T) {
	m := carregaExemplo(t)
	var texto []string
	for _, l := range Formata(busca(m, "Erro inesperado"), ModoErros, false) {
		texto = append(texto, Desenha(l, "", false))
	}
	todo := strings.Join(texto, "\n")
	if !strings.Contains(todo, "└─▸ Caused by: org.postgresql.util.PSQLException") {
		t.Errorf("a causa raiz deveria vir em destaque:\n%s", todo)
	}
	if strings.Contains(todo, "HibernateJpaDialect") {
		t.Errorf("o resumo mostra só frames do Booklit:\n%s", todo)
	}
	if !strings.Contains(todo, "VotoService.votar") {
		t.Errorf("faltou o frame do Booklit:\n%s", todo)
	}
}

func TestJSONNaMensagem(t *testing.T) {
	msg, linhas := separaJSON(`Resposta do SES {"MessageId":"abc","Status":200}`)
	if msg != "Resposta do SES" || len(linhas) != 4 {
		t.Errorf("msg=%q linhas=%q", msg, linhas)
	}
	if msg, linhas := separaJSON("500 Internal Server Error: [no body]"); linhas != nil || msg == "" {
		t.Errorf("não era JSON: %q %q", msg, linhas)
	}
}

func TestQuebraRespeitaLargura(t *testing.T) {
	l := Linha{Recuo: 4, Prefixo: []Span{{"  │ ", ""}}}
	l.add("  │ ", "")
	l.add(strings.Repeat("palavra ", 20), "")
	for _, q := range Quebra(l, 40) {
		if q.largura() > 40 {
			t.Errorf("linha com %d colunas", q.largura())
		}
		if !strings.HasPrefix(Desenha(q, "", false), "  │ ") {
			t.Errorf("continuação sem a barra: %q", Desenha(q, "", false))
		}
	}
}

func TestDestaqueDaBusca(t *testing.T) {
	var l Linha
	l.add("Convite criado", cVerde)
	saida := Desenha(l, "criado", true)
	if !strings.Contains(saida, cInvertido) {
		t.Errorf("busca sem destaque: %q", saida)
	}
}

func TestTextoSoImprimeRegistroFechado(t *testing.T) {
	m := NovoMontador(0)
	m.Adiciona([]Evento{
		{ID: "1", Stream: "api", Timestamp: 1, Mensagem: "10:00:00.000 ERROR [________] a.B   falhou"},
	})
	if ultimoFechado(m) != 0 {
		t.Fatal("o último registro ainda pode receber o stack trace")
	}
	m.Adiciona([]Evento{{ID: "2", Stream: "api", Timestamp: 2, Mensagem: "\tat x.Y.z(Y.java:1)"}})
	m.Adiciona(nil)
	if ultimoFechado(m) != 1 || len(m.Registros[0].Continua) != 1 {
		t.Fatal("depois de uma busca sem novidade o registro está fechado")
	}
}

func TestEscopoPorNome(t *testing.T) {
	casos := map[string]string{
		"convite":           "ConviteController",
		"ConviteController": "ConviteController",
		"usuario":           "UsuarioController",
		"usuarioclube":      "UsuarioClubeController",
		"api":               "API inteira",
		"sem":               "Sem controller",
		"caddy":             "caddy",
	}
	for entrada, esperado := range casos {
		e, ok := escopoPorNome(entrada)
		if !ok || e.Nome != esperado {
			t.Errorf("%q -> %q (ok=%v), esperava %q", entrada, e.Nome, ok, esperado)
		}
	}
	if _, ok := escopoPorNome("controller"); ok {
		t.Error("pedaço que casa com várias controllers deveria ser ambíguo")
	}
}

func TestFiltroPorController(t *testing.T) {
	m := carregaExemplo(t)
	voto, _ := escopoPorNome("voto")
	var msgs []string
	for _, r := range m.Registros {
		if voto.Filtra(r) {
			msgs = append(msgs, r.Mensagem)
		}
	}
	// O erro do handler (antes do log de acesso) e o próprio acesso.
	if len(msgs) != 2 || !strings.Contains(msgs[0], "Erro inesperado") {
		t.Errorf("VotoController: %q", msgs)
	}
	sem, _ := escopoPorNome("sem")
	for _, r := range m.Registros {
		if sem.Filtra(r) && r.RequestID != "" && r.Controller != "" {
			t.Errorf("linha com controller em 'sem controller': %+v", r)
		}
	}
}

func TestEOLDoSpring(t *testing.T) {
	m := NovoMontador(0)
	m.Adiciona([]Evento{
		{ID: "1", Stream: "api", Timestamp: 1, Mensagem: "10:00:00.000 ERROR [aa] a.GoogleBooksClient   Falha"},
		{ID: "2", Stream: "api", Timestamp: 2, Mensagem: `org.springframework.web.client.HttpClientErrorException$BadRequest: 400 Bad Request: "{<EOL>  "error": {<EOL>    "code": 400,<EOL>    "message": "API key not valid."<EOL>  }<EOL>}<EOL>"`},
	})
	c := m.Registros[0].Continua
	if len(c) != 1 || strings.Contains(c[0], "<EOL>") || !strings.Contains(c[0], `"{ "error": { "code": 400, "message": "API key not valid." } } "`) {
		t.Errorf("continuação: %q", c)
	}
}
