package main

import (
	"fmt"
	"sort"
	"strings"
	"time"
)

func (a *App) desenha() {
	w, h := a.term.Largura, a.term.Altura
	var b strings.Builder
	b.WriteString("\x1b[H")

	linha := func(s string) {
		b.WriteString(s)
		b.WriteString(reset + "\x1b[K\r\n")
	}

	linha(invertido + negrito + preenche(a.titulo(), w))
	linha(a.linhaFiltros(w))

	corpo := h - 3
	var linhas []string
	switch a.modo {
	case modoDetalhe:
		linhas = a.linhasDetalhe(w, corpo)
	case modoControllers:
		linhas = a.linhasPicker(w, corpo)
	case modoEstatisticas:
		linhas = a.paginado(a.linhasEstatisticas(w), corpo, &a.statsTopo)
	case modoAjuda:
		linhas = a.paginado(linhasAjuda(), corpo, &a.statsTopo)
	default:
		linhas = a.linhasLista(w, corpo)
	}
	for i := 0; i < corpo; i++ {
		if i < len(linhas) {
			linha(linhas[i])
		} else {
			linha("")
		}
	}
	b.WriteString(a.rodape(w) + reset + "\x1b[K")
	a.term.Escreve(b.String())
}

func (a *App) titulo() string {
	estado := "pausado"
	if a.seguindo {
		estado = "● ao vivo"
	}
	if a.carregando {
		estado += " ⟳"
	}
	cont := map[Nivel]int{}
	for _, r := range a.m.Registros {
		cont[r.Nivel]++
	}
	t := fmt.Sprintf(" blogs  %s  últimos %s  %s  %d/%d linhas  INFO %d  WARN %d  ERROR %d",
		a.fonte.Grupo, duracaoCurta(janelas[a.janela]), estado, len(a.vis), len(a.m.Registros),
		cont[Info], cont[Warn], cont[Error])
	if !a.atualizado.IsZero() {
		t += "  " + a.atualizado.Format("15:04:05")
	}
	return t
}

func (a *App) linhaFiltros(w int) string {
	if a.erro != "" {
		return vermelho + negrito + corta(" erro: "+a.erro, w)
	}
	s := " filtros: " + a.filtro.Descricao()
	if err := a.filtro.ErroBusca(); err != nil {
		s += "   (regex inválida)"
	}
	if a.truncado {
		s += "   ⚠ limite de eventos atingido: diminua a janela (t)"
	}
	cor := cinza
	if a.filtro.Ativo() {
		cor = ciano
	}
	return cor + corta(s, w)
}

func (a *App) rodape(w int) string {
	switch a.modo {
	case modoBusca:
		dica := "  (Enter aplica · Esc desfaz · Ctrl+U limpa · re: para regex)"
		return negrito + "/" + reset + a.filtro.Busca + "\x1b[7m \x1b[27m" + cinza + corta(dica, w-len([]rune(a.filtro.Busca))-2)
	case modoDetalhe:
		return cinza + corta(" ↑↓ rolar · ←→ anterior/próximo · r só esta requisição · Esc voltar", w)
	case modoControllers:
		return cinza + corta(" ↑↓ escolher · Enter filtrar · Esc voltar", w)
	case modoEstatisticas, modoAjuda:
		return cinza + corta(" ↑↓ rolar · Esc voltar", w)
	}
	return cinza + corta(" / busca · c controller · l nível · e 4xx/5xx · a só requisições · r requisição · s stream · t janela · f pausar · v estatísticas · x limpar · ? ajuda · q sair", w)
}

// --- lista -----------------------------------------------------------------------------

func (a *App) linhasLista(w, altura int) []string {
	if len(a.vis) == 0 {
		msg := "  nenhuma linha"
		if a.carregando {
			msg = "  carregando…"
		} else if a.filtro.Ativo() {
			msg += " com esses filtros (x limpa)"
		}
		return []string{cinza + msg}
	}
	var out []string
	for i := a.topo; i < len(a.vis) && len(out) < altura; i++ {
		out = append(out, a.linhaRegistro(a.vis[i], w, i == a.cursor))
	}
	return out
}

func (a *App) linhaRegistro(r *Registro, w int, sel bool) string {
	hora := r.Momento.Format("15:04:05")
	if janelas[a.janela] > 24*time.Hour {
		hora = r.Momento.Format("02/01 15:04")
	}
	ctrl := strings.TrimSuffix(r.Controller, "Controller")
	if ctrl == "" {
		ctrl = "·"
	}
	req := r.RequestID
	if req == "" {
		req = "·"
	}
	fixo := fmt.Sprintf("%s %-5s %-8s %-16s ", hora, r.Nivel.String(), corta(req, 8), corta(ctrl, 16))
	resto := w - len([]rune(fixo))

	msg := r.Mensagem
	if r.Acesso != nil {
		ac := r.Acesso
		msg = fmt.Sprintf("%d %5dms %-6s %s  %s", ac.Status, ac.Duracao, ac.Metodo, ac.Rota, ac.Usuario)
	}
	if len(r.Continua) > 0 {
		msg += fmt.Sprintf("  [+%d linhas]", len(r.Continua))
	}
	msg = corta(strings.ReplaceAll(msg, "\t", " "), resto)

	if sel {
		return invertido + preenche(fixo+msg, w)
	}
	corNivel := corDoNivel(r.Nivel)
	corMsg := ""
	if r.Acesso != nil {
		corMsg = corDoStatus(r.Acesso.Status)
	} else if r.Nivel == Error {
		corMsg = vermelho
	}
	return cinza + hora + " " + reset + corNivel + fmt.Sprintf("%-5s", r.Nivel.String()) + reset + " " +
		apagado + fmt.Sprintf("%-8s", corta(req, 8)) + reset + " " +
		azul + fmt.Sprintf("%-16s", corta(ctrl, 16)) + reset + " " + corMsg + msg
}

func corDoNivel(n Nivel) string {
	switch n {
	case Error:
		return vermelho + negrito
	case Warn:
		return amarelo
	case Info:
		return verde
	case Debug, Trace:
		return cinza
	}
	return cinza
}

func corDoStatus(s int) string {
	switch {
	case s >= 500:
		return vermelho
	case s >= 400:
		return amarelo
	case s >= 300:
		return ciano
	}
	return ""
}

// --- detalhe ---------------------------------------------------------------------------

func (a *App) linhasDetalhe(w, altura int) []string {
	r := a.selecionado()
	if r == nil {
		return nil
	}
	campo := func(nome, valor string) string {
		if valor == "" {
			valor = "-"
		}
		return cinza + fmt.Sprintf(" %-11s", nome) + reset + valor
	}
	todas := []string{
		campo("momento", r.Momento.Format("02/01/2006 15:04:05.000 (MST)")),
		campo("nível", corDoNivel(r.Nivel)+r.Nivel.String()+reset),
		campo("stream", r.Stream),
		campo("requisição", r.RequestID),
		campo("controller", r.Controller),
		campo("logger", r.Logger),
	}
	if ac := r.Acesso; ac != nil {
		todas = append(todas,
			campo("rota", ac.Metodo+" "+ac.Rota),
			campo("status", corDoStatus(ac.Status)+itoa(ac.Status)+reset),
			campo("duração", itoa(ac.Duracao)+" ms"),
			campo("usuário", ac.Usuario),
			campo("handler", ac.Handler))
	}
	todas = append(todas, "", negrito+" mensagem"+reset)
	for _, l := range quebra(r.Mensagem, w-2) {
		todas = append(todas, " "+l)
	}
	if len(r.Continua) > 0 {
		todas = append(todas, "", negrito+fmt.Sprintf(" continuação (%d linhas)", len(r.Continua))+reset)
		for _, c := range r.Continua {
			for _, l := range quebra(c, w-2) {
				cor := ""
				if strings.HasPrefix(strings.TrimSpace(l), "at ") && !strings.Contains(l, "github.jotagm") {
					cor = cinza // frames do framework ficam apagados; os do Booklit em destaque
				}
				todas = append(todas, cor+" "+l)
			}
		}
	}
	if r.RequestID != "" {
		mesma := 0
		for _, o := range a.m.Registros {
			if o.RequestID == r.RequestID {
				mesma++
			}
		}
		todas = append(todas, "", cinza+fmt.Sprintf(" %d linha(s) nesta requisição — r filtra só ela", mesma))
	}
	return a.paginado(todas, altura, &a.detalheTopo)
}

func (a *App) paginado(todas []string, altura int, topo *int) []string {
	max := len(todas) - altura
	if max < 0 {
		max = 0
	}
	if *topo > max {
		*topo = max
	}
	fim := *topo + altura
	if fim > len(todas) {
		fim = len(todas)
	}
	return todas[*topo:fim]
}

// --- seletor ---------------------------------------------------------------------------

func (a *App) linhasPicker(w, altura int) []string {
	out := []string{negrito + " filtrar por controller" + reset, ""}
	h := altura - 2
	if a.pickerCursor < a.pickerTopo {
		a.pickerTopo = a.pickerCursor
	}
	if a.pickerCursor >= a.pickerTopo+h {
		a.pickerTopo = a.pickerCursor - h + 1
	}
	for i := a.pickerTopo; i < len(a.picker) && len(out) < altura; i++ {
		l := "   " + a.picker[i].rotulo
		if i == a.pickerCursor {
			out = append(out, invertido+preenche(" ▸ "+a.picker[i].rotulo, w))
		} else {
			out = append(out, corta(l, w))
		}
	}
	return out
}

// --- estatísticas ---------------------------------------------------------------------

type estat struct {
	nome      string
	n, c4, c5 int
	duracoes  []int
	erros     int
}

func (a *App) linhasEstatisticas(w int) []string {
	porCtrl := map[string]*estat{}
	porHandler := map[string]*estat{}
	pega := func(m map[string]*estat, k string) *estat {
		if m[k] == nil {
			m[k] = &estat{nome: k}
		}
		return m[k]
	}
	for _, r := range a.m.Registros {
		if r.Nivel == Error && r.Controller != "" {
			pega(porCtrl, r.Controller).erros++
		}
		if r.Acesso == nil {
			continue
		}
		for _, e := range []*estat{pega(porCtrl, r.Controller), pega(porHandler, rotuloEndpoint(r.Acesso))} {
			e.n++
			e.duracoes = append(e.duracoes, r.Acesso.Duracao)
			if r.Acesso.Status >= 500 {
				e.c5++
			} else if r.Acesso.Status >= 400 {
				e.c4++
			}
		}
	}

	cab := fmt.Sprintf(" %-36s %7s %6s %6s %7s %7s %7s", "", "req", "4xx", "5xx", "média", "p95", "máx")
	out := []string{negrito + fmt.Sprintf(" estatísticas — últimos %s (requisições do RequestLoggingFilter)", duracaoCurta(janelas[a.janela])) + reset, ""}
	tabela := func(titulo string, m map[string]*estat, limite int) {
		lista := make([]*estat, 0, len(m))
		for _, e := range m {
			if e.n > 0 {
				lista = append(lista, e)
			}
		}
		sort.Slice(lista, func(i, j int) bool {
			if lista[i].n != lista[j].n {
				return lista[i].n > lista[j].n
			}
			return lista[i].nome < lista[j].nome
		})
		out = append(out, negrito+" "+titulo+reset, cinza+cab+reset)
		if len(lista) == 0 {
			out = append(out, cinza+"   sem requisições nesta janela")
		}
		for i, e := range lista {
			if limite > 0 && i >= limite {
				break
			}
			media, p95, max := resumo(e.duracoes)
			cor := ""
			if e.c5 > 0 {
				cor = vermelho
			} else if e.c4 > 0 {
				cor = amarelo
			}
			out = append(out, cor+corta(fmt.Sprintf(" %-36s %7d %6d %6d %6dms %6dms %6dms",
				corta(e.nome, 36), e.n, e.c4, e.c5, media, p95, max), w))
		}
		out = append(out, "")
	}
	tabela("por controller", porCtrl, 0)
	tabela("endpoints mais chamados", porHandler, 20)

	lentos := []*Registro{}
	for _, r := range a.m.Registros {
		if r.Acesso != nil {
			lentos = append(lentos, r)
		}
	}
	sort.Slice(lentos, func(i, j int) bool { return lentos[i].Acesso.Duracao > lentos[j].Acesso.Duracao })
	out = append(out, negrito+" requisições mais lentas"+reset)
	for i, r := range lentos {
		if i >= 10 {
			break
		}
		ac := r.Acesso
		out = append(out, corta(fmt.Sprintf(" %6dms  %s  %d %s %s  req=%s", ac.Duracao, r.Momento.Format("15:04:05"),
			ac.Status, ac.Metodo, ac.Rota, r.RequestID), w))
	}
	return out
}

func rotuloEndpoint(ac *Acesso) string {
	if ac.Handler != "" {
		return strings.Replace(ac.Handler, "Controller#", ".", 1)
	}
	return ac.Metodo + " " + normalizaRota(ac.Rota)
}

// Troca ids por {id} para agrupar /clubes/3f2a... e /clubes/9b1c... na mesma linha.
func normalizaRota(rota string) string {
	if i := strings.IndexByte(rota, '?'); i >= 0 {
		rota = rota[:i]
	}
	partes := strings.Split(rota, "/")
	for i, p := range partes {
		if pareceId(p) {
			partes[i] = "{id}"
		}
	}
	return strings.Join(partes, "/")
}

func pareceId(p string) bool {
	if len(p) == 0 {
		return false
	}
	digitos := 0
	for _, c := range p {
		if c >= '0' && c <= '9' {
			digitos++
		}
	}
	return digitos == len(p) || (len(p) >= 8 && strings.Count(p, "-") >= 4) || (len(p) >= 8 && digitos >= 2 && !strings.ContainsAny(p, "_"))
}

func resumo(d []int) (media, p95, max int) {
	if len(d) == 0 {
		return
	}
	s := append([]int(nil), d...)
	sort.Ints(s)
	soma := 0
	for _, v := range s {
		soma += v
	}
	idx := (len(s)*95 + 99) / 100
	if idx < 1 {
		idx = 1
	}
	return soma / len(s), s[idx-1], s[len(s)-1]
}

func duracaoCurta(d time.Duration) string {
	switch {
	case d%(24*time.Hour) == 0:
		return fmt.Sprintf("%dd", int(d/(24*time.Hour)))
	case d%time.Hour == 0:
		return fmt.Sprintf("%dh", int(d/time.Hour))
	}
	return fmt.Sprintf("%dmin", int(d/time.Minute))
}

func linhasAjuda() []string {
	return []string{
		negrito + " blogs — logs do Booklit no CloudWatch" + reset,
		"",
		negrito + " navegação" + reset,
		"   ↑ ↓ / k j       linha anterior / próxima",
		"   PgUp PgDn       página",
		"   Home End / g G  início / fim",
		"   Enter           detalhe da linha (stack trace completo, campos da requisição)",
		"",
		negrito + " filtros (combinam entre si)" + reset,
		"   /               busca no texto (sem diferença de maiúsculas); \"re:\" no começo usa regex",
		"   c               escolher controller (com contagem de linhas e erros)",
		"   l               nível mínimo: todos → INFO → WARN → ERROR",
		"   e               status das requisições: todos → 4xx e 5xx → só 5xx",
		"   a               só as linhas de requisição (uma por chamada HTTP)",
		"   r               só a requisição da linha selecionada (todas as linhas dela); de novo desfaz",
		"   s               stream: api + caddy → só api → só caddy",
		"   x               limpar todos os filtros",
		"",
		negrito + " dados" + reset,
		"   t               janela: 15min → 1h → 6h → 1d → 3d (busca de novo)",
		"   f               pausa / retoma o ao vivo (ligado ao abrir; busca novidades a cada 2 s)",
		"   R               recarregar a janela",
		"   v               estatísticas: requisições, 4xx/5xx e latência por controller e endpoint",
		"",
		negrito + " como o controller é descoberto" + reset,
		"   linha de requisição: campo handler= do RequestLoggingFilter (ou a rota, em logs antigos);",
		"   demais linhas: herdam da linha de requisição com o mesmo requestId.",
		"",
		"   q ou Ctrl+C sai.",
	}
}
