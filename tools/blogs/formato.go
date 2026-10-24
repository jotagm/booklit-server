package main

import (
	"bytes"
	"encoding/json"
	"strings"
	"unicode/utf8"
)

// Cores ANSI (as mesmas do log_viewer.py).
const (
	cReset         = "\x1b[0m"
	cNegrito       = "\x1b[1m"
	cApagado       = "\x1b[2m"
	cInvertido     = "\x1b[7m"
	cVermelho      = "\x1b[91m"
	cVerde         = "\x1b[92m"
	cAmarelo       = "\x1b[93m"
	cAzul          = "\x1b[94m"
	cMagenta       = "\x1b[95m"
	cCiano         = "\x1b[96m"
	cBranco        = "\x1b[97m"
	cCinza         = "\x1b[90m"
	cFundoVermelho = "\x1b[48;5;52m"
	cFundoAmarelo  = "\x1b[48;5;58m"
)

// Modo de exibição, como no log_viewer.py: tudo, só INFO (sem ruído) ou erros completos.
type Modo int

const (
	ModoTodos Modo = iota
	ModoInfo
	ModoErros
)

var nomesModo = []string{"TODOS", "INFO", "ERROS"}

func (m Modo) String() string { return nomesModo[m] }

func parseModo(s string) (Modo, bool) {
	switch strings.ToLower(s) {
	case "todos", "all", "":
		return ModoTodos, true
	case "info", "informativos":
		return ModoInfo, true
	case "erros", "erro", "error", "errors":
		return ModoErros, true
	}
	return 0, false
}

// Pacote do Booklit: no stack trace resumido, só os frames do nosso código.
var pacotesDoProjeto = []string{"github.jotagm"}

// Mensagens repetitivas escondidas no modo INFO.
var ruido = []string{"/actuator/health", "GET /v3/api-docs -> 200"}

const larguraClasse = 25

// Mostra diz se o registro aparece no modo escolhido.
func Mostra(r *Registro, modo Modo) bool {
	switch modo {
	case ModoInfo:
		return r.Nivel == Info && !r.Solta && !ehRuido(r.Mensagem)
	case ModoErros:
		return r.Nivel >= Warn || strings.Contains(r.Mensagem, "Exception") || len(r.Continua) > 0
	}
	return true
}

func ehRuido(msg string) bool {
	for _, n := range ruido {
		if strings.Contains(msg, n) {
			return true
		}
	}
	return false
}

// Span é um pedaço de texto com um estilo; Linha é uma linha da tela. Recuo é onde as
// continuações começam quando a linha não cabe na largura do terminal.
type Span struct {
	Texto  string
	Estilo string
}

type Linha struct {
	Spans   []Span
	Recuo   int
	Prefixo []Span // repetido no começo das continuações (a barra "│" dos blocos de erro)
}

func (l *Linha) add(texto, estilo string) { l.Spans = append(l.Spans, Span{texto, estilo}) }

func (l Linha) largura() int {
	n := 0
	for _, s := range l.Spans {
		n += utf8.RuneCountInString(s.Texto)
	}
	return n
}

// Formata monta as linhas de um registro. completo=true mostra o stack trace inteiro em vez
// do resumo (causas + 3 frames do Booklit).
func Formata(r *Registro, modo Modo, completo bool) []Linha {
	msg, jsonBonito := separaJSON(r.Mensagem)
	var out []Linha

	if r.Solta {
		var l Linha
		l.add("  "+corta(msg, 150), cVermelho+cApagado)
		out = append(out, l)
		return append(out, blocoDeErro(r, completo, cVermelho)...)
	}

	id := r.RequestID
	if id == "" {
		id = "        "
	}
	cor := corDoNivel(r.Nivel)

	if modo == ModoErros && r.Nivel >= Warn {
		// Em "erros completos" cada ocorrência vira um bloco com faixa colorida.
		selo, fundo := " ✖ ERROR ", cFundoVermelho
		if r.Nivel == Warn {
			selo, fundo = " ▲ WARN  ", cFundoAmarelo
		}
		var cab Linha
		cab.add(selo, fundo+cBranco+cNegrito)
		cab.add(" "+r.Hora+" ", cCinza)
		cab.add(id+" ", cCinza+cApagado)
		cab.add(r.Classe, cor+cNegrito)
		if r.Controller != "" && r.Controller != r.Classe {
			cab.add("  "+r.Controller, cCinza)
		}
		corpo := Linha{Recuo: 4, Prefixo: []Span{{"  │ ", cor}}}
		corpo.add("  │ ", cor)
		corpo.add(msg, cor)
		out = append(out, Linha{}, cab, corpo)
	} else {
		var l Linha
		simbolo, corMsg, corClasse := "●", "", cAzul
		switch r.Nivel {
		case Error:
			simbolo, corMsg, corClasse = "✖", cVermelho, cVermelho+cNegrito
		case Warn:
			simbolo, corMsg, corClasse = "▲", cAmarelo, cAmarelo
		case Debug:
			simbolo, corMsg = "·", cCinza
		}
		corSimbolo := cVerde
		if r.Nivel != Info {
			corSimbolo = cor + cNegrito
		}
		l.add(simbolo, corSimbolo)
		l.add(" "+r.Hora+" ", cCinza)
		l.add(id+" ", cCinza+cApagado)
		l.add(alinhaDireita(r.Classe, larguraClasse), corClasse)
		l.add(" │ ", "")
		l.Recuo = l.largura()
		l.add(msg, corMsg)
		out = append(out, l)
	}

	for _, j := range jsonBonito {
		var l Linha
		l.Recuo = 8
		l.add("      "+j, cCinza+cApagado)
		out = append(out, l)
	}
	return append(out, blocoDeErro(r, completo, cor)...)
}

func corDoNivel(n Nivel) string {
	switch n {
	case Error:
		return cVermelho
	case Warn:
		return cAmarelo
	case Debug:
		return cCinza
	}
	return cVerde
}

// blocoDeErro desenha o stack trace como o log_viewer.py: a cadeia de causas em árvore,
// com a causa raiz em destaque, e os primeiros frames do código do Booklit.
func blocoDeErro(r *Registro, completo bool, cor string) []Linha {
	if len(r.Continua) == 0 {
		return nil
	}
	if cor == cVerde || cor == cCinza {
		cor = cVermelho
	}
	barra := func() Linha {
		l := Linha{Recuo: 6, Prefixo: []Span{{"  │ ", cor}}}
		l.add("  │ ", cor)
		return l
	}
	var out []Linha

	if completo {
		for _, c := range r.Continua {
			l := barra()
			switch {
			case strings.HasPrefix(c, "at "):
				estilo := cCinza
				if doProjeto(c) {
					estilo = cBranco
				}
				l.add("    "+c, estilo)
			case reMais.MatchString(c):
				l.add("    "+c, cCinza+cApagado)
			default:
				l.add(c, cor+cNegrito)
			}
			out = append(out, l)
		}
	} else {
		var causas, frames, todosFrames []string
		for _, c := range r.Continua {
			switch {
			case strings.HasPrefix(c, "at "):
				todosFrames = append(todosFrames, c)
				if doProjeto(c) {
					frames = append(frames, c)
				}
			case reMais.MatchString(c):
			default:
				causas = append(causas, c)
			}
		}
		if len(frames) == 0 {
			frames = todosFrames
		}
		for i, c := range causas {
			l := barra()
			recuo := strings.Repeat("  ", i+1)
			if i == len(causas)-1 {
				l.add(recuo+"└─▸ "+c, cor+cNegrito)
			} else {
				l.add(recuo+"├─ "+c, cor+cApagado)
			}
			out = append(out, l)
		}
		for i, f := range frames {
			if i == 3 {
				l := barra()
				l.add("  ... +"+itoa(len(frames)-3)+" linhas (Tab mostra o stack completo)", cCinza)
				out = append(out, l)
				break
			}
			l := barra()
			l.add("  "+f, cCinza)
			out = append(out, l)
		}
	}
	var fim Linha
	fim.add("  ╰"+strings.Repeat("─", 50), cor)
	return append(out, fim)
}

func doProjeto(frame string) bool {
	for _, p := range pacotesDoProjeto {
		if strings.Contains(frame, p) {
			return true
		}
	}
	return false
}

// separaJSON acha um JSON dentro da mensagem e devolve a mensagem sem ele mais o JSON
// formatado, linha a linha (como o summarize_json do log_viewer.py).
func separaJSON(msg string) (string, []string) {
	for i := 0; i < len(msg); i++ {
		if msg[i] != '{' && msg[i] != '[' {
			continue
		}
		dec := json.NewDecoder(strings.NewReader(msg[i:]))
		var bruto json.RawMessage
		if err := dec.Decode(&bruto); err != nil {
			continue
		}
		fim := i + int(dec.InputOffset())
		if fim-i < 10 {
			continue
		}
		var buf bytes.Buffer
		if err := json.Indent(&buf, bruto, "", "  "); err != nil {
			continue
		}
		resto := strings.TrimSpace(msg[:i] + msg[fim:])
		return resto, strings.Split(buf.String(), "\n")
	}
	return msg, nil
}

func alinhaDireita(s string, n int) string {
	c := utf8.RuneCountInString(s)
	if c >= n {
		return corta(s, n)
	}
	return strings.Repeat(" ", n-c) + s
}

// Quebra divide uma linha que não cabe na largura; as continuações começam no Recuo.
func Quebra(l Linha, largura int) []Linha {
	if largura < 20 {
		largura = 20
	}
	if l.largura() <= largura {
		return []Linha{l}
	}
	recuo := l.Recuo
	if recuo > largura/2 {
		recuo = 4
	}
	var out []Linha
	atual := Linha{}
	col := 0
	for _, s := range l.Spans {
		r := []rune(s.Texto)
		for len(r) > 0 {
			cabe := largura - col
			if cabe <= 0 {
				out = append(out, atual)
				atual = Linha{}
				col = 0
				for _, p := range l.Prefixo {
					atual.add(p.Texto, p.Estilo)
					col += utf8.RuneCountInString(p.Texto)
				}
				if col < recuo {
					atual.add(strings.Repeat(" ", recuo-col), "")
					col = recuo
				}
				cabe = largura - col
			}
			n := len(r)
			if n > cabe {
				n = cabe
				// Quebra no último espaço, se ele não estiver longe demais.
				for i := n - 1; i > 0 && i >= n-20; i-- {
					if r[i] == ' ' {
						n = i + 1
						break
					}
				}
			}
			atual.add(string(r[:n]), s.Estilo)
			col += n
			r = r[n:]
		}
	}
	return append(out, atual)
}

// Desenha converte a linha em texto com ANSI, destacando as ocorrências da busca.
func Desenha(l Linha, busca string, comCor bool) string {
	var b strings.Builder
	for _, s := range l.Spans {
		if !comCor {
			b.WriteString(s.Texto)
			continue
		}
		destaca(&b, s, busca)
	}
	return b.String()
}

func destaca(b *strings.Builder, s Span, busca string) {
	escreve := func(texto, estilo string) {
		if texto == "" {
			return
		}
		if estilo == "" {
			b.WriteString(texto)
			return
		}
		b.WriteString(estilo)
		b.WriteString(texto)
		b.WriteString(cReset)
	}
	if busca == "" {
		escreve(s.Texto, s.Estilo)
		return
	}
	minusc := strings.ToLower(s.Texto)
	if len(minusc) != len(s.Texto) { // caixa muda o tamanho em bytes: não arrisca índices
		escreve(s.Texto, s.Estilo)
		return
	}
	resto, rmin := s.Texto, minusc
	for {
		i := strings.Index(rmin, busca)
		if i < 0 {
			escreve(resto, s.Estilo)
			return
		}
		escreve(resto[:i], s.Estilo)
		escreve(resto[i:i+len(busca)], s.Estilo+cInvertido+cNegrito)
		resto, rmin = resto[i+len(busca):], rmin[i+len(busca):]
	}
}

func corta(s string, n int) string {
	if n <= 0 {
		return ""
	}
	if utf8.RuneCountInString(s) <= n {
		return s
	}
	r := []rune(s)
	if n == 1 {
		return string(r[:1])
	}
	return string(r[:n-1]) + "…"
}
