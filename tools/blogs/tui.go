package main

import (
	"fmt"
	"os"
	"os/signal"
	"sort"
	"syscall"
	"time"
)

type modo int

const (
	modoLista modo = iota
	modoDetalhe
	modoBusca
	modoControllers
	modoEstatisticas
	modoAjuda
)

var janelas = []time.Duration{15 * time.Minute, time.Hour, 6 * time.Hour, 24 * time.Hour, 72 * time.Hour}

type resultado struct {
	eventos  []Evento
	truncado bool
	err      error
	completo bool // recarga da janela inteira (e não incremento do modo seguir)
	pedidoEm time.Time
}

type App struct {
	term   *Terminal
	fonte  *Fonte
	m      *Montador
	filtro Filtro
	vis    []*Registro
	cursor int
	topo   int
	modo   modo

	janela     int // índice em janelas
	seguindo   bool
	carregando bool
	truncado   bool
	erro       string
	atualizado time.Time
	ultimoTS   int64

	buscaAntes string // para o Esc desfazer a edição

	picker       []itemPicker
	pickerCursor int
	pickerTopo   int

	detalheTopo int
	statsTopo   int

	resultados chan resultado
}

type itemPicker struct {
	valor  string
	rotulo string
}

func RodaTUI(fonte *Fonte, filtro Filtro, janela int, seguir bool) error {
	term, err := AbreTerminal()
	if err != nil {
		return err
	}
	defer term.Fecha()

	a := &App{term: term, fonte: fonte, m: NovoMontador(), filtro: filtro, janela: janela,
		seguindo: seguir, resultados: make(chan resultado, 4)}

	teclas := make(chan Tecla, 32)
	go term.LeTeclas(teclas)

	redim := make(chan os.Signal, 1)
	signal.Notify(redim, syscall.SIGWINCH)
	defer signal.Stop(redim)

	tick := time.NewTicker(3 * time.Second)
	defer tick.Stop()

	a.recarrega()
	a.desenha()
	for {
		select {
		case t, ok := <-teclas:
			if !ok || !a.tecla(t) {
				return nil
			}
		case r := <-a.resultados:
			a.recebe(r)
		case <-redim:
			term.AtualizaTamanho()
		case <-tick.C:
			if a.seguindo && !a.carregando {
				a.buscaNovos()
			}
		}
		a.desenha()
	}
}

// --- dados -----------------------------------------------------------------------------

func (a *App) recarrega() {
	a.carregando = true
	desde := time.Now().Add(-janelas[a.janela])
	go func() {
		ev, trunc, err := a.fonte.Busca(desde)
		a.resultados <- resultado{eventos: ev, truncado: trunc, err: err, completo: true, pedidoEm: time.Now()}
	}()
}

func (a *App) buscaNovos() {
	a.carregando = true
	// Volta 20 s: o CloudWatch pode indexar eventos com atraso. Os repetidos caem no eventId.
	desde := time.UnixMilli(a.ultimoTS).Add(-20 * time.Second)
	if a.ultimoTS == 0 {
		desde = time.Now().Add(-janelas[a.janela])
	}
	go func() {
		ev, trunc, err := a.fonte.Busca(desde)
		a.resultados <- resultado{eventos: ev, truncado: trunc, err: err, pedidoEm: time.Now()}
	}()
}

func (a *App) recebe(r resultado) {
	a.carregando = false
	if r.err != nil {
		a.erro = r.err.Error()
		return
	}
	a.erro = ""
	a.atualizado = r.pedidoEm
	noFim := len(a.vis) == 0 || a.cursor >= len(a.vis)-1
	if r.completo {
		a.m = NovoMontador()
		a.truncado = r.truncado
		noFim = true
	}
	a.m.Adiciona(r.eventos)
	for _, e := range r.eventos {
		if e.Timestamp > a.ultimoTS {
			a.ultimoTS = e.Timestamp
		}
	}
	a.refiltra()
	if noFim {
		a.vaiPara(len(a.vis) - 1)
	}
}

func (a *App) refiltra() {
	var atual *Registro
	if a.cursor >= 0 && a.cursor < len(a.vis) {
		atual = a.vis[a.cursor]
	}
	a.vis = Filtra(a.m.Registros, &a.filtro)
	a.cursor = len(a.vis) - 1
	if atual != nil {
		for i, r := range a.vis {
			if r == atual {
				a.cursor = i
				break
			}
		}
	}
	a.vaiPara(a.cursor)
}

// --- navegação --------------------------------------------------------------------------

func (a *App) alturaLista() int {
	h := a.term.Altura - 3
	if h < 1 {
		h = 1
	}
	return h
}

func (a *App) vaiPara(i int) {
	if len(a.vis) == 0 {
		a.cursor, a.topo = 0, 0
		return
	}
	if i < 0 {
		i = 0
	}
	if i >= len(a.vis) {
		i = len(a.vis) - 1
	}
	a.cursor = i
	h := a.alturaLista()
	if a.cursor < a.topo {
		a.topo = a.cursor
	}
	if a.cursor >= a.topo+h {
		a.topo = a.cursor - h + 1
	}
	if a.topo < 0 {
		a.topo = 0
	}
}

func (a *App) selecionado() *Registro {
	if a.cursor >= 0 && a.cursor < len(a.vis) {
		return a.vis[a.cursor]
	}
	return nil
}

// --- teclas -----------------------------------------------------------------------------

// tecla devolve false para sair.
func (a *App) tecla(t Tecla) bool {
	if t.Nome == "ctrlc" {
		return false
	}
	switch a.modo {
	case modoBusca:
		a.teclaBusca(t)
		return true
	case modoDetalhe:
		return a.teclaDetalhe(t)
	case modoControllers:
		a.teclaPicker(t)
		return true
	case modoEstatisticas, modoAjuda:
		switch {
		case t.Nome == "esc", t.Nome == "char" && (t.Rune == 'q' || t.Rune == 'v' || t.Rune == '?'):
			a.modo = modoLista
		case t.Nome == "down" || t.Nome == "char" && t.Rune == 'j':
			a.statsTopo++
		case t.Nome == "up" || t.Nome == "char" && t.Rune == 'k':
			if a.statsTopo > 0 {
				a.statsTopo--
			}
		}
		return true
	}

	h := a.alturaLista()
	switch t.Nome {
	case "up":
		a.vaiPara(a.cursor - 1)
	case "down":
		a.vaiPara(a.cursor + 1)
	case "pgup":
		a.vaiPara(a.cursor - h)
	case "pgdn":
		a.vaiPara(a.cursor + h)
	case "home":
		a.vaiPara(0)
	case "end":
		a.vaiPara(len(a.vis) - 1)
	case "enter":
		if a.selecionado() != nil {
			a.modo, a.detalheTopo = modoDetalhe, 0
		}
	case "esc":
		if a.filtro.RequestID != "" {
			a.filtro.RequestID = ""
			a.refiltra()
		}
	case "char":
		switch t.Rune {
		case 'q':
			return false
		case 'k':
			a.vaiPara(a.cursor - 1)
		case 'j':
			a.vaiPara(a.cursor + 1)
		case 'g':
			a.vaiPara(0)
		case 'G':
			a.vaiPara(len(a.vis) - 1)
		case '/':
			a.modo, a.buscaAntes = modoBusca, a.filtro.Busca
		case 'c':
			a.abrePicker()
		case 'l':
			a.filtro.NivelMinimo = proximoNivel(a.filtro.NivelMinimo)
			a.refiltra()
		case 'a':
			a.filtro.SoAcessos = !a.filtro.SoAcessos
			a.refiltra()
		case 'e':
			switch a.filtro.StatusMin {
			case 0:
				a.filtro.StatusMin = 400
			case 400:
				a.filtro.StatusMin = 500
			default:
				a.filtro.StatusMin = 0
			}
			a.refiltra()
		case 'r':
			a.alternaRequest()
		case 's':
			switch a.filtro.Stream {
			case "":
				a.filtro.Stream = "api"
			case "api":
				a.filtro.Stream = "caddy"
			default:
				a.filtro.Stream = ""
			}
			a.refiltra()
		case 't':
			a.janela = (a.janela + 1) % len(janelas)
			a.ultimoTS = 0
			a.recarrega()
		case 'f':
			a.seguindo = !a.seguindo
			if a.seguindo {
				a.vaiPara(len(a.vis) - 1)
				a.buscaNovos()
			}
		case 'R':
			a.ultimoTS = 0
			a.recarrega()
		case 'x':
			a.filtro = Filtro{NivelMinimo: SemNivel}
			a.refiltra()
		case 'v':
			a.modo, a.statsTopo = modoEstatisticas, 0
		case '?':
			a.modo, a.statsTopo = modoAjuda, 0
		}
	}
	return true
}

func proximoNivel(n Nivel) Nivel {
	switch n {
	case SemNivel:
		return Info
	case Info:
		return Warn
	case Warn:
		return Error
	}
	return SemNivel
}

func (a *App) alternaRequest() {
	if a.filtro.RequestID != "" {
		a.filtro.RequestID = ""
	} else if r := a.selecionado(); r != nil && r.RequestID != "" {
		a.filtro.RequestID = r.RequestID
	}
	a.refiltra()
}

func (a *App) teclaBusca(t Tecla) {
	switch t.Nome {
	case "enter":
		a.modo = modoLista
	case "esc":
		a.filtro.Busca = a.buscaAntes
		a.modo = modoLista
	case "backspace":
		if r := []rune(a.filtro.Busca); len(r) > 0 {
			a.filtro.Busca = string(r[:len(r)-1])
		}
	case "ctrlu":
		a.filtro.Busca = ""
	case "char":
		a.filtro.Busca += string(t.Rune)
	default:
		return
	}
	a.refiltra()
}

func (a *App) teclaDetalhe(t Tecla) bool {
	switch {
	case t.Nome == "esc", t.Nome == "char" && t.Rune == 'q':
		a.modo = modoLista
	case t.Nome == "down" || t.Nome == "char" && t.Rune == 'j':
		a.detalheTopo++
	case t.Nome == "up" || t.Nome == "char" && t.Rune == 'k':
		if a.detalheTopo > 0 {
			a.detalheTopo--
		}
	case t.Nome == "pgdn":
		a.detalheTopo += a.alturaLista()
	case t.Nome == "pgup":
		a.detalheTopo -= a.alturaLista()
		if a.detalheTopo < 0 {
			a.detalheTopo = 0
		}
	case t.Nome == "right" || t.Nome == "char" && t.Rune == 'n':
		a.vaiPara(a.cursor + 1)
		a.detalheTopo = 0
	case t.Nome == "left" || t.Nome == "char" && t.Rune == 'p':
		a.vaiPara(a.cursor - 1)
		a.detalheTopo = 0
	case t.Nome == "char" && t.Rune == 'r':
		a.alternaRequest()
		a.modo = modoLista
	}
	return true
}

// --- seletor de controller ------------------------------------------------------------

func (a *App) abrePicker() {
	total := map[string]int{}
	erros := map[string]int{}
	for _, r := range a.m.Registros {
		c := r.Controller
		if c == "" {
			c = semController
		}
		total[c]++
		if r.Nivel == Error {
			erros[c]++
		}
	}
	nomes := make([]string, 0, len(total))
	for c := range total {
		nomes = append(nomes, c)
	}
	sort.Slice(nomes, func(i, j int) bool {
		if total[nomes[i]] != total[nomes[j]] {
			return total[nomes[i]] > total[nomes[j]]
		}
		return nomes[i] < nomes[j]
	})
	a.picker = []itemPicker{{"", fmt.Sprintf("%-32s %6d", "(todos)", len(a.m.Registros))}}
	for _, c := range nomes {
		rot := fmt.Sprintf("%-32s %6d", c, total[c])
		if erros[c] > 0 {
			rot += fmt.Sprintf("  %d erro(s)", erros[c])
		}
		a.picker = append(a.picker, itemPicker{c, rot})
	}
	a.pickerCursor, a.pickerTopo = 0, 0
	for i, it := range a.picker {
		if it.valor == a.filtro.Controller {
			a.pickerCursor = i
		}
	}
	a.modo = modoControllers
}

func (a *App) teclaPicker(t Tecla) {
	switch {
	case t.Nome == "esc", t.Nome == "char" && t.Rune == 'q':
		a.modo = modoLista
	case t.Nome == "up" || t.Nome == "char" && t.Rune == 'k':
		if a.pickerCursor > 0 {
			a.pickerCursor--
		}
	case t.Nome == "down" || t.Nome == "char" && t.Rune == 'j':
		if a.pickerCursor < len(a.picker)-1 {
			a.pickerCursor++
		}
	case t.Nome == "enter":
		a.filtro.Controller = a.picker[a.pickerCursor].valor
		a.modo = modoLista
		a.refiltra()
	}
}
