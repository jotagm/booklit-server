package main

import (
	"context"
	"fmt"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"
	"unicode/utf8"
)

// App mantém o terminal aberto do começo ao fim (menus e tela de logs no mesmo modo raw) e
// um único polling do log group inteiro: trocar de controller não busca nada de novo, só
// muda o filtro, e o menu já mostra quantas requisições e erros cada uma teve.
type App struct {
	term      *Terminal
	teclas    chan Tecla
	resize    chan os.Signal
	fonte     Fonte
	janela    time.Duration
	intervalo time.Duration

	montador   *Montador
	resultados chan resultadoBusca
	erro       string
	carregou   bool
	atualizado time.Time
}

type resultadoBusca struct {
	eventos []Evento
	err     error
}

func NovoApp(t *Terminal, fonte Fonte, janela, intervalo time.Duration) *App {
	a := &App{term: t, teclas: make(chan Tecla, 32), resize: make(chan os.Signal, 1), fonte: fonte,
		janela: janela, intervalo: intervalo, montador: NovoMontador(20000),
		resultados: make(chan resultadoBusca, 1)}
	go t.LeTeclas(a.teclas)
	signal.Notify(a.resize, syscall.SIGWINCH)
	go consulta(context.Background(), &a.fonte, a.janela, a.intervalo, a.resultados)
	return a
}

func (a *App) recebe(r resultadoBusca) {
	a.carregou = true
	a.atualizado = time.Now().In(fuso)
	if r.err != nil {
		a.erro = r.err.Error()
		return
	}
	a.erro = ""
	a.montador.Adiciona(r.eventos)
}

// consulta busca a janela inicial e depois só o que chegou, com uma folga para eventos
// que o CloudWatch indexa fora de ordem (os repetidos são descartados pelo eventId).
func consulta(ctx context.Context, f *Fonte, janela, intervalo time.Duration, saida chan<- resultadoBusca) {
	const folga = 15 * time.Second
	cursor := time.Now().Add(-janela)
	if f.Arquivo != "" {
		cursor = time.Time{}
	}
	for {
		eventos, cheio, err := f.Busca(ctx, cursor)
		if ctx.Err() != nil {
			return
		}
		var maior int64
		for _, e := range eventos {
			if e.Timestamp > maior {
				maior = e.Timestamp
			}
		}
		if maior > 0 {
			if c := time.UnixMilli(maior).Add(-folga); c.After(cursor) {
				cursor = c
			}
		}
		select {
		case saida <- resultadoBusca{eventos, err}:
		case <-ctx.Done():
			return
		}
		if cheio && err == nil {
			continue // veio cortado pelo limite: busca o resto já
		}
		select {
		case <-time.After(intervalo):
		case <-ctx.Done():
			return
		}
	}
}

// --- menus -----------------------------------------------------------------------------

type opcao struct {
	rotulo, detalhe, cor string
}

// menu desenha uma lista no estilo do log_viewer.py. Escolhe pelo número (com 10 ou mais
// opções, dois dígitos ou número + Enter) ou ↑/↓ + Enter; devolve -1 em q, Esc ou Ctrl+C.
// opcoes é chamada a cada desenho, para os contadores acompanharem o polling.
func (a *App) menu(titulo, cor string, opcoes func() []opcao, selecionada int) int {
	n := len(opcoes())
	if selecionada < 0 || selecionada >= n {
		selecionada = 0
	}
	numero := ""
	for {
		ops := opcoes()
		a.desenhaMenu(titulo, cor, ops, selecionada, numero)
		select {
		case r := <-a.resultados:
			a.recebe(r)
		case <-a.resize:
			a.term.AtualizaTamanho()
		case k, ok := <-a.teclas:
			if !ok {
				return -1
			}
			switch k.Nome {
			case "up":
				selecionada, numero = (selecionada+n-1)%n, ""
			case "down", "tab":
				selecionada, numero = (selecionada+1)%n, ""
			case "pgup", "home":
				selecionada, numero = 0, ""
			case "pgdn", "end":
				selecionada, numero = n-1, ""
			case "enter":
				return selecionada
			case "backspace":
				if numero != "" {
					numero = numero[:len(numero)-1]
				}
			case "esc", "ctrlc":
				return -1
			case "char":
				switch {
				case k.Rune == 'q':
					return -1
				case k.Rune == 'k':
					selecionada, numero = (selecionada+n-1)%n, ""
				case k.Rune == 'j':
					selecionada, numero = (selecionada+1)%n, ""
				case k.Rune >= '0' && k.Rune <= '9':
					v := atoi(numero + string(k.Rune))
					if v < 1 || v > n {
						numero = "" // número que não existe: começa de novo
						continue
					}
					numero += string(k.Rune)
					selecionada = v - 1
					if v*10 > n { // nenhum número maior começa assim: escolhe já
						return selecionada
					}
				}
			}
		}
	}
}

func atoi(s string) int {
	v := 0
	for _, c := range s {
		v = v*10 + int(c-'0')
	}
	return v
}

func (a *App) desenhaMenu(titulo, cor string, opcoes []opcao, sel int, numero string) {
	H := a.term.Altura
	var b strings.Builder
	b.WriteString("\x1b[?25l\x1b[H\x1b[2J")
	linha := "  " + cor + strings.Repeat("═", 60) + cReset + "\r\n"
	b.WriteString("\r\n" + linha)
	b.WriteString("  " + cor + cNegrito + "  " + titulo + cReset + "\r\n")
	b.WriteString(linha + "\r\n")

	// Se a lista não couber, rola mantendo a opção selecionada visível.
	cabem := max(3, H-9)
	inicio := 0
	if len(opcoes) > cabem {
		inicio = min(max(0, sel-cabem/2), len(opcoes)-cabem)
	}
	largura := 0
	for _, o := range opcoes {
		largura = max(largura, utf8.RuneCountInString(o.rotulo))
	}
	for i := inicio; i < len(opcoes) && i < inicio+cabem; i++ {
		o := opcoes[i]
		marca := "  "
		rotulo := o.cor + cNegrito + " " + o.rotulo + " " + cReset
		if i == sel {
			marca = cor + cNegrito + "› " + cReset
			rotulo = cInvertido + o.cor + cNegrito + " " + o.rotulo + " " + cReset
		}
		fmt.Fprintf(&b, "  %s%s%2d%s %s│%s%s", marca, cNegrito, i+1, cReset, cCinza, cReset, rotulo)
		b.WriteString(strings.Repeat(" ", largura-utf8.RuneCountInString(o.rotulo)))
		if o.detalhe != "" {
			b.WriteString(" " + o.detalhe + cReset)
		}
		b.WriteString("\r\n")
	}
	rodape := "número ou ↑/↓ + Enter · q sai"
	if numero != "" {
		rodape = "número: " + numero + "  (Enter confirma)"
	}
	b.WriteString("\r\n  " + cCinza + rodape + cReset)
	if a.erro != "" {
		b.WriteString("\r\n  " + cVermelho + corta("✖ "+a.erro, a.term.Largura-4) + cReset)
	} else if !a.carregou {
		b.WriteString("\r\n  " + cCinza + "buscando os logs dos últimos " + duracaoCurta(a.janela) + "…" + cReset)
	}
	a.term.Escreve(b.String())
}

// Contagem por escopo, mostrada ao lado de cada opção do menu.
func (a *App) resumo(e Escopo) string {
	if !a.carregou {
		return ""
	}
	req, avisos, erros := 0, 0, 0
	for _, r := range a.montador.Registros {
		if !e.Filtra(r) {
			continue
		}
		if r.Classe == "RequestLoggingFilter" || r.Classe == "caddy:access" {
			req++
		}
		switch r.Nivel {
		case Error:
			erros++
		case Warn:
			avisos++
		}
	}
	if req == 0 && avisos == 0 && erros == 0 {
		return cCinza + "  —"
	}
	s := fmt.Sprintf("%s%3d req", cCinza, req)
	if avisos > 0 {
		s += fmt.Sprintf("  %s%d warn", cAmarelo, avisos)
	}
	if erros > 0 {
		s += fmt.Sprintf("  %s%d erro", cVermelho, erros)
		if erros > 1 {
			s += "s"
		}
	}
	return s
}

func (a *App) escolheEscopo(atual int) (int, bool) {
	lista := escopos()
	ops := func() []opcao {
		var out []opcao
		for _, e := range lista {
			d := a.resumo(e)
			if e.Detalhe != "" {
				// Contadores numa coluna fixa; a descrição vem depois.
				visivel := utf8.RuneCountInString(reAnsi.ReplaceAllString(d, ""))
				d += strings.Repeat(" ", max(1, 28-visivel)) + cCinza + e.Detalhe
			}
			out = append(out, opcao{e.Nome, d, e.Cor})
		}
		return out
	}
	titulo := "📋 blogs " + a.fonte.Grupo + " — Escolha a controller  " + cReset + cCinza +
		"(últimos " + duracaoCurta(a.janela) + ")"
	i := a.menu(titulo, cCiano, ops, atual)
	return i, i >= 0
}

func (a *App) escolheModo(e Escopo, atual Modo) (Modo, bool) {
	ops := func() []opcao {
		return []opcao{
			{"Todos os logs", "", cVerde},
			{"Informativos", cCinza + "(INFO sem ruído)", cAzul},
			{"Erros completos", cCinza + "(ERROR/WARN + causa raiz)", cVermelho},
		}
	}
	i := a.menu("📋 "+e.Nome+" — Tipo de log", cCiano, ops, int(atual))
	if i < 0 {
		return 0, false
	}
	return Modo(i), true
}

// Roda o fluxo do log_viewer.py: controller → modo → logs → (outra controller | outro modo | sair).
func (a *App) Roda(escopo *Escopo, modo *Modo) {
	lista := escopos()
	idx := 0
	if escopo == nil {
		i, ok := a.escolheEscopo(0)
		if !ok {
			return
		}
		idx = i
	} else {
		for i, e := range lista {
			if e.Nome == escopo.Nome {
				idx = i
			}
		}
	}
	atual := ModoTodos
	for {
		if modo == nil {
			m, ok := a.escolheModo(lista[idx], atual)
			if !ok {
				return
			}
			modo = &m
		}
		atual = *modo
		a.acompanha(lista[idx], *modo)

		ops := func() []opcao {
			return []opcao{
				{"Escolher outra controller", "", cCiano},
				{"Trocar o tipo de log", cCinza + "(mesma controller)", cMagenta},
				{"Voltar aos logs", cCinza + "(" + lista[idx].Nome + ", " + strings.ToLower(atual.String()) + ")", cAzul},
				{"Sair", "", cAmarelo},
			}
		}
		switch a.menu("↩ O que deseja fazer?", cCiano, ops, 2) {
		case 0:
			i, ok := a.escolheEscopo(idx)
			if !ok {
				return
			}
			idx, modo = i, nil
		case 1:
			modo = nil
		case 2:
		default:
			return
		}
	}
}

// --- tela de logs ----------------------------------------------------------------------

type tela struct {
	a        *App
	escopo   Escopo
	modo     Modo
	busca    string
	completo bool
	topo     int // -1 = ao vivo (sempre o fim); senão a linha fixa no alto da tela

	// do último desenho, para as teclas de rolagem
	inicio, maxTopo, corpo int
}

// acompanha mostra os logs ao vivo até o Ctrl+C, redesenhando a cada busca e a cada tecla.
func (a *App) acompanha(e Escopo, modo Modo) {
	t := &tela{a: a, escopo: e, modo: modo, topo: -1}
	t.desenha()
	for {
		select {
		case r := <-a.resultados:
			a.recebe(r)
		case <-a.resize:
			a.term.AtualizaTamanho()
		case k, ok := <-a.teclas:
			if !ok || !t.tecla(k) {
				return
			}
		}
		t.desenha()
	}
}

// tecla trata uma tecla; devolve false para sair da tela (Ctrl+C).
func (t *tela) tecla(k Tecla) bool {
	switch k.Nome {
	case "ctrlc":
		return false
	case "up":
		t.topo = max(0, t.inicio-1)
	case "down":
		t.topo = t.inicio + 1
	case "pgup":
		t.topo = max(0, t.inicio-max(1, t.corpo-1))
	case "pgdn":
		t.topo = t.inicio + max(1, t.corpo-1)
	case "home":
		t.topo = 0
	case "end":
		t.topo = -1
	case "tab":
		t.completo = !t.completo
	case "esc", "ctrlu":
		t.busca, t.topo = "", -1
	case "backspace":
		if t.busca != "" {
			_, n := utf8.DecodeLastRuneInString(t.busca)
			t.busca = t.busca[:len(t.busca)-n]
		}
		t.topo = -1
	case "char":
		t.busca += string(k.Rune)
		t.topo = -1 // busca nova sempre volta para o fim do resultado
	}
	return true
}

func (t *tela) linhas(largura int) []Linha {
	busca := strings.ToLower(t.busca)
	var out []Linha
	for _, r := range t.a.montador.Registros {
		if !t.escopo.Filtra(r) || !Mostra(r, t.modo) {
			continue
		}
		if busca != "" && !strings.Contains(r.TextoBusca(), busca) {
			continue
		}
		for _, l := range Formata(r, t.modo, t.completo) {
			out = append(out, Quebra(l, largura)...)
		}
	}
	return out
}

func (t *tela) desenha() {
	a := t.a
	L, H := a.term.Largura, a.term.Altura
	// Rodapé fixo de 3 linhas (status, separador, busca) para a tela não "pular".
	t.corpo = max(1, H-4)
	linhas := t.linhas(L)
	total := len(linhas)
	t.maxTopo = max(0, total-t.corpo)
	if t.topo >= t.maxTopo {
		t.topo = -1 // alcançou o fim: retoma o ao vivo
	}
	t.inicio = t.maxTopo
	if t.topo >= 0 {
		t.inicio = t.topo
	}
	fim := min(total, t.inicio+t.corpo)
	busca := strings.ToLower(t.busca)

	var b strings.Builder
	b.WriteString("\x1b[?25l")
	linhaEm := func(y int, conteudo string) {
		fmt.Fprintf(&b, "\x1b[%d;1H\x1b[2K%s", y, conteudo)
	}

	// Cabeçalho.
	estado := cVerde + "● ao vivo" + cReset + cInvertido
	if t.topo >= 0 {
		estado = cAmarelo + "⏸ pausado" + cReset + cInvertido
	}
	if !a.carregou {
		estado = "buscando no CloudWatch…"
	} else if a.erro != "" {
		estado = cVermelho + "✖ erro na busca" + cReset + cInvertido
	}
	quando := ""
	if !a.atualizado.IsZero() {
		quando = " · " + a.atualizado.Format("15:04:05")
	}
	cab := fmt.Sprintf(" %s │ %s │ Modo: %s │ ", t.escopo.Nome, a.fonte.Grupo, t.modo)
	resto := fmt.Sprintf("%s │ %d linhas ", quando, total)
	larguraVisivel := utf8.RuneCountInString(cab) + utf8.RuneCountInString(reAnsi.ReplaceAllString(estado, "")) +
		utf8.RuneCountInString(resto)
	linhaEm(1, cInvertido+cab+estado+cInvertido+resto+strings.Repeat(" ", max(0, L-larguraVisivel))+cReset)

	// Corpo.
	for i := 0; i < t.corpo; i++ {
		y := 2 + i
		switch {
		case t.inicio+i < fim:
			linhaEm(y, Desenha(linhas[t.inicio+i], busca, true))
		case i == 0 && total == 0:
			linhaEm(y, cCinza+"  "+t.mensagemVazia()+cReset)
		default:
			linhaEm(y, "")
		}
	}

	// Status.
	status := ""
	switch {
	case t.topo >= 0:
		status = cInvertido + corta(fmt.Sprintf(" ⏸ PAUSADO · linha %d-%d de %d · ↑/↓/PgUp/PgDn rola · End volta ao vivo ",
			t.inicio+1, fim, total), L) + cReset
	case a.erro != "":
		status = cVermelho + corta(" ✖ "+a.erro+" (tentando de novo)", L) + cReset
	case t.completo:
		status = cCinza + " stack trace completo (Tab volta ao resumo)" + cReset
	}
	linhaEm(H-2, status)
	linhaEm(H-1, "")

	// Busca.
	prompt := cNegrito + "Buscar: " + cReset
	cursor := 9
	if t.busca != "" {
		prompt += t.busca
		cursor += utf8.RuneCountInString(t.busca)
	} else {
		prompt += cCinza + corta("(digite para filtrar · ESC limpa · ↑/↓/PgUp/PgDn rola · End: ao vivo · "+
			"Tab: stack completo · Ctrl+C: menu)", L-9) + cReset
	}
	linhaEm(H, prompt)
	fmt.Fprintf(&b, "\x1b[%d;%dH\x1b[?25h", H, min(cursor, L))
	a.term.Escreve(b.String())
}

func (t *tela) mensagemVazia() string {
	a := t.a
	switch {
	case !a.carregou:
		return "buscando os logs dos últimos " + duracaoCurta(a.janela) + "…"
	case a.erro != "" && len(a.montador.Registros) == 0:
		return "não deu para buscar os logs — o erro está no rodapé"
	case t.busca != "":
		return "nenhuma linha com “" + t.busca + "” (ESC limpa a busca)"
	}
	return "nada de " + t.escopo.Nome + " nos últimos " + duracaoCurta(a.janela) +
		" neste modo — aguardando (atualiza a cada " + duracaoCurta(a.intervalo) + ")…"
}

func duracaoCurta(d time.Duration) string {
	s := d.String()
	s = strings.TrimSuffix(s, "0s")
	s = strings.TrimSuffix(s, "0m")
	if s == "" {
		return d.String()
	}
	return s
}
