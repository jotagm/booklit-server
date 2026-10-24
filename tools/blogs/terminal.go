package main

import (
	"fmt"
	"os"
	"os/exec"
	"strconv"
	"strings"
	"time"
	"unicode/utf8"
)

// Terminal em modo raw via stty: a stdlib não expõe termios de forma portátil, e o stty
// existe em todo Linux (WSL incluso). Evita depender de pacotes externos.
type Terminal struct {
	tty     *os.File
	estado  string
	Largura int
	Altura  int
}

func AbreTerminal() (*Terminal, error) {
	tty, err := os.OpenFile("/dev/tty", os.O_RDWR, 0)
	if err != nil {
		return nil, fmt.Errorf("sem terminal interativo (/dev/tty): %w", err)
	}
	t := &Terminal{tty: tty}
	estado, err := t.stty("-g")
	if err != nil {
		tty.Close()
		return nil, err
	}
	t.estado = strings.TrimSpace(estado)
	if _, err := t.stty("raw", "-echo"); err != nil {
		tty.Close()
		return nil, err
	}
	// Tela alternativa (ao sair, o terminal volta como estava) e cursor escondido.
	t.Escreve("\x1b[?1049h\x1b[?25l")
	t.AtualizaTamanho()
	return t, nil
}

func (t *Terminal) stty(args ...string) (string, error) {
	cmd := exec.Command("stty", args...)
	cmd.Stdin = t.tty
	out, err := cmd.Output()
	if err != nil {
		return "", fmt.Errorf("stty %s: %w", strings.Join(args, " "), err)
	}
	return string(out), nil
}

func (t *Terminal) Fecha() {
	t.Escreve("\x1b[?25h\x1b[?1049l")
	if t.estado != "" {
		t.stty(t.estado)
	}
	t.tty.Close()
}

func (t *Terminal) AtualizaTamanho() {
	out, err := t.stty("size")
	if err == nil {
		if p := strings.Fields(out); len(p) == 2 {
			t.Altura, _ = strconv.Atoi(p[0])
			t.Largura, _ = strconv.Atoi(p[1])
		}
	}
	if t.Largura <= 0 {
		t.Largura = 100
	}
	if t.Altura <= 0 {
		t.Altura = 30
	}
}

func (t *Terminal) Escreve(s string) { t.tty.WriteString(s) }

// --- teclado --------------------------------------------------------------------------

type Tecla struct {
	Nome string // "up", "down", "pgup", "pgdn", "home", "end", "enter", "esc", "backspace", "tab", "char"
	Rune rune
}

// LeTeclas decodifica bytes do terminal em teclas. Um ESC sozinho só é reconhecido depois
// de uma pausa curta, porque é também o começo das sequências das setas.
func (t *Terminal) LeTeclas(saida chan<- Tecla) {
	bytesCh := make(chan byte, 64)
	go func() {
		buf := make([]byte, 64)
		for {
			n, err := t.tty.Read(buf)
			if err != nil {
				close(bytesCh)
				return
			}
			for _, b := range buf[:n] {
				bytesCh <- b
			}
		}
	}()

	proximo := func(espera time.Duration) (byte, bool) {
		select {
		case b, ok := <-bytesCh:
			return b, ok
		case <-time.After(espera):
			return 0, false
		}
	}

	for b := range bytesCh {
		switch {
		case b == 27:
			b2, ok := proximo(40 * time.Millisecond)
			if !ok {
				saida <- Tecla{Nome: "esc"}
				continue
			}
			if b2 != '[' && b2 != 'O' {
				saida <- Tecla{Nome: "esc"}
				continue
			}
			seq := []byte{}
			for {
				c, ok := proximo(40 * time.Millisecond)
				if !ok {
					break
				}
				seq = append(seq, c)
				if (c >= 'A' && c <= 'Z') || c == '~' {
					break
				}
			}
			switch string(seq) {
			case "A":
				saida <- Tecla{Nome: "up"}
			case "B":
				saida <- Tecla{Nome: "down"}
			case "C":
				saida <- Tecla{Nome: "right"}
			case "D":
				saida <- Tecla{Nome: "left"}
			case "5~":
				saida <- Tecla{Nome: "pgup"}
			case "6~":
				saida <- Tecla{Nome: "pgdn"}
			case "H", "1~", "7~":
				saida <- Tecla{Nome: "home"}
			case "F", "4~", "8~":
				saida <- Tecla{Nome: "end"}
			}
		case b == '\r' || b == '\n':
			saida <- Tecla{Nome: "enter"}
		case b == 127 || b == 8:
			saida <- Tecla{Nome: "backspace"}
		case b == '\t':
			saida <- Tecla{Nome: "tab"}
		case b == 3: // Ctrl+C
			saida <- Tecla{Nome: "ctrlc"}
		case b == 21: // Ctrl+U
			saida <- Tecla{Nome: "ctrlu"}
		case b < 32:
			// outros controles: ignora
		default:
			// UTF-8: junta os bytes de continuação (acentos na busca).
			p := []byte{b}
			for !utf8.FullRune(p) && len(p) < 4 {
				c, ok := proximo(20 * time.Millisecond)
				if !ok {
					break
				}
				p = append(p, c)
			}
			r, _ := utf8.DecodeRune(p)
			saida <- Tecla{Nome: "char", Rune: r}
		}
	}
	close(saida)
}

// --- desenho ---------------------------------------------------------------------------

const (
	reset     = "\x1b[0m"
	negrito   = "\x1b[1m"
	apagado   = "\x1b[2m"
	invertido = "\x1b[7m"
	vermelho  = "\x1b[31m"
	verde     = "\x1b[32m"
	amarelo   = "\x1b[33m"
	azul      = "\x1b[34m"
	magenta   = "\x1b[35m"
	ciano     = "\x1b[36m"
	cinza     = "\x1b[90m"
)

// corta limita s a n colunas (contando runas; a TUI não usa caracteres largos).
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

func preenche(s string, n int) string {
	c := utf8.RuneCountInString(s)
	if c >= n {
		return corta(s, n)
	}
	return s + strings.Repeat(" ", n-c)
}

// quebra divide s em linhas de até n colunas, preservando as quebras originais.
func quebra(s string, n int) []string {
	if n < 10 {
		n = 10
	}
	var out []string
	for _, linha := range strings.Split(strings.ReplaceAll(s, "\t", "    "), "\n") {
		r := []rune(linha)
		if len(r) == 0 {
			out = append(out, "")
			continue
		}
		for len(r) > n {
			out = append(out, string(r[:n]))
			r = r[n:]
		}
		out = append(out, string(r))
	}
	return out
}

func itoa(i int) string { return strconv.Itoa(i) }
