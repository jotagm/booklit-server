package main

import (
	"regexp"
	"sort"
	"strconv"
	"strings"
	"time"
)

// Nivel do log do Spring. A ordem importa: o filtro "WARN" mostra WARN e ERROR.
type Nivel int

const (
	Trace Nivel = iota
	Debug
	Info
	Warn
	Error
	SemNivel // linha que não segue o padrão do Spring (Caddy, JVM no boot...)
)

func (n Nivel) String() string {
	switch n {
	case Trace:
		return "TRACE"
	case Debug:
		return "DEBUG"
	case Info:
		return "INFO"
	case Warn:
		return "WARN"
	case Error:
		return "ERROR"
	}
	return "-"
}

func parseNivel(s string) Nivel {
	switch strings.ToUpper(strings.TrimSpace(s)) {
	case "TRACE":
		return Trace
	case "DEBUG":
		return Debug
	case "INFO":
		return Info
	case "WARN", "WARNING":
		return Warn
	case "ERROR", "ERRO":
		return Error
	}
	return SemNivel
}

// Acesso é a linha do RequestLoggingFilter: uma por requisição.
type Acesso struct {
	Metodo  string
	Rota    string
	Status  int
	Duracao int // ms
	Usuario string
	Handler string // ClubeController#buscarPorId, ou "" em logs antigos
}

// Registro é uma entrada lógica de log: a linha principal mais as continuações
// (stack trace, "Caused by"...), que no CloudWatch chegam como eventos separados.
type Registro struct {
	Momento    time.Time
	Stream     string // api | caddy
	Nivel      Nivel
	RequestID  string // "" quando não há requisição (scheduler, boot)
	Logger     string
	Mensagem   string
	Continua   []string
	Acesso     *Acesso
	Controller string // derivado: handler, rota ou requestId
	ids        []string
}

// Linha do padrão de console do application.yaml:
//
//	%d{HH:mm:ss.SSS} %5p [%X{requestId:-________}] %-40.40logger{39} %m
var reLinhaSpring = regexp.MustCompile(`^(\d{2}:\d{2}:\d{2}\.\d{3})\s+(TRACE|DEBUG|INFO|WARN|ERROR)\s+\[([^\]]*)\]\s+(\S+)\s+(.*)$`)

// Mensagem do RequestLoggingFilter:
//
//	GET /clubes/abc -> 200 (12ms) usuario=x@y handler=ClubeController#buscarPorId
var reAcesso = regexp.MustCompile(`^(GET|POST|PUT|PATCH|DELETE|OPTIONS|HEAD) (\S+) -> (\d{3}) \((\d+)ms\) usuario=(\S+)(?: handler=(\S+))?`)

// Remove as cores ANSI caso o Spring as emita (spring.output.ansi.enabled=always).
var reAnsi = regexp.MustCompile("\x1b\\[[0-9;]*m")

// Evento cru do CloudWatch Logs (filter-log-events).
type Evento struct {
	ID        string `json:"eventId"`
	Stream    string `json:"logStreamName"`
	Timestamp int64  `json:"timestamp"`
	Mensagem  string `json:"message"`
}

// Montador transforma eventos em registros, juntando continuações à linha anterior
// do mesmo stream. É incremental: o modo "seguir" vai alimentando aos poucos.
type Montador struct {
	Registros  []*Registro
	vistos     map[string]bool
	ultimo     map[string]*Registro // último registro aberto por stream
	ultimoEm   map[string]time.Time // momento do último evento por stream
	porRequest map[string]string    // requestId -> controller
}

func NovoMontador() *Montador {
	return &Montador{vistos: map[string]bool{}, ultimo: map[string]*Registro{}, ultimoEm: map[string]time.Time{},
		porRequest: map[string]string{}}
}

// Adiciona eventos (em qualquer ordem) e devolve quantos eram novos.
func (m *Montador) Adiciona(eventos []Evento) int {
	sort.SliceStable(eventos, func(i, j int) bool { return eventos[i].Timestamp < eventos[j].Timestamp })
	novos := 0
	for _, e := range eventos {
		if e.ID != "" && m.vistos[e.ID] {
			continue
		}
		if e.ID != "" {
			m.vistos[e.ID] = true
		}
		novos++
		m.adicionaUm(e)
	}
	if novos > 0 {
		m.atribuiControllers()
	}
	return novos
}

func (m *Montador) adicionaUm(e Evento) {
	texto := strings.TrimRight(reAnsi.ReplaceAllString(e.Mensagem, ""), "\r\n")
	momento := time.UnixMilli(e.Timestamp)
	stream := e.Stream
	if stream == "" {
		stream = "api"
	}
	anterior := m.ultimoEm[stream]
	m.ultimoEm[stream] = momento

	if g := reLinhaSpring.FindStringSubmatch(texto); g != nil {
		r := &Registro{
			Momento:   momento,
			Stream:    stream,
			Nivel:     parseNivel(g[2]),
			RequestID: limpaRequestID(g[3]),
			Logger:    g[4],
			Mensagem:  g[5],
			ids:       []string{e.ID},
		}
		if a := reAcesso.FindStringSubmatch(r.Mensagem); a != nil {
			status, _ := strconv.Atoi(a[3])
			dur, _ := strconv.Atoi(a[4])
			handler := a[6]
			if handler == "-" {
				handler = ""
			}
			r.Acesso = &Acesso{Metodo: a[1], Rota: a[2], Status: status, Duracao: dur, Usuario: a[5], Handler: handler}
		}
		m.Registros = append(m.Registros, r)
		m.ultimo[stream] = r
		return
	}

	// Continuação (stack trace) só se vier logo depois do evento anterior do mesmo stream:
	// o awslogs manda cada linha do stack trace como um evento, com o mesmo instante.
	if ant := m.ultimo[stream]; ant != nil && pareceContinuacao(texto) && momento.Sub(anterior) < 2*time.Second {
		ant.Continua = append(ant.Continua, texto)
		return
	}

	r := &Registro{Momento: momento, Stream: stream, Nivel: nivelSolto(texto), Mensagem: texto, ids: []string{e.ID}}
	m.Registros = append(m.Registros, r)
	m.ultimo[stream] = r
}

func pareceContinuacao(t string) bool {
	if t == "" {
		return true
	}
	if t[0] == '\t' || t[0] == ' ' {
		return true
	}
	return strings.HasPrefix(t, "Caused by:") || strings.HasPrefix(t, "...") ||
		strings.Contains(t, "Exception") || strings.Contains(t, "Error:")
}

// Nível de linhas fora do padrão: o Caddy loga JSON com "level":"error".
func nivelSolto(t string) Nivel {
	l := strings.ToLower(t)
	switch {
	case strings.Contains(l, `"level":"error"`), strings.Contains(l, `"level":"fatal"`):
		return Error
	case strings.Contains(l, `"level":"warn"`):
		return Warn
	case strings.Contains(l, `"level":"info"`):
		return Info
	case strings.Contains(l, `"level":"debug"`):
		return Debug
	}
	return SemNivel
}

func limpaRequestID(s string) string {
	s = strings.TrimSpace(s)
	if strings.Trim(s, "_") == "" {
		return ""
	}
	return s
}

// Cada requisição ganha o controller do seu log de acesso; as outras linhas da mesma
// requisição (service, GlobalExceptionHandler...) herdam pelo requestId.
func (m *Montador) atribuiControllers() {
	for _, r := range m.Registros {
		if r.Acesso != nil && r.RequestID != "" {
			m.porRequest[r.RequestID] = controllerDoAcesso(r.Acesso)
		}
	}
	for _, r := range m.Registros {
		switch {
		case r.Acesso != nil:
			r.Controller = controllerDoAcesso(r.Acesso)
		case r.RequestID != "" && m.porRequest[r.RequestID] != "":
			r.Controller = m.porRequest[r.RequestID]
		case r.Stream == "caddy":
			r.Controller = "caddy"
		default:
			r.Controller = ""
		}
	}
}

func controllerDoAcesso(a *Acesso) string {
	if a.Handler != "" {
		if i := strings.IndexByte(a.Handler, '#'); i > 0 {
			return a.Handler[:i]
		}
		return a.Handler
	}
	return controllerPelaRota(a.Rota)
}

// Fallback para logs anteriores ao "handler=" no RequestLoggingFilter: mapeia pela rota,
// seguindo os @RequestMapping dos controllers do booklit-server.
var rotas = []struct {
	re         *regexp.Regexp
	controller string
}{
	{regexp.MustCompile(`^/auth/otp/`), "OtpController"},
	{regexp.MustCompile(`^/auth(/|$)`), "AuthController"},
	{regexp.MustCompile(`^/leituras/[^/]+/comentarios`), "ComentarioController"},
	{regexp.MustCompile(`^/comentarios(/|$)`), "ComentarioController"},
	{regexp.MustCompile(`^/(feed|reacoes)(/|$|\?)`), "FeedController"},
	{regexp.MustCompile(`^/clubes(/|$|\?)`), "ClubeController"},
	{regexp.MustCompile(`^/convites(/|$|\?)`), "ConviteController"},
	{regexp.MustCompile(`^/estante(/|$|\?)`), "EstanteController"},
	{regexp.MustCompile(`^/leituras(/|$|\?)`), "LeituraClubeController"},
	{regexp.MustCompile(`^/opcoes-voto(/|$|\?)`), "OpcaoVotoController"},
	{regexp.MustCompile(`^/registros(/|$|\?)`), "RegistroController"},
	{regexp.MustCompile(`^/temas(/|$|\?)`), "TemaController"},
	{regexp.MustCompile(`^/membros(/|$|\?)`), "UsuarioClubeController"},
	{regexp.MustCompile(`^/usuarios(/|$|\?)`), "UsuarioController"},
	{regexp.MustCompile(`^/votacoes(/|$|\?)`), "VotacaoController"},
	{regexp.MustCompile(`^/votos(/|$|\?)`), "VotoController"},
}

func controllerPelaRota(rota string) string {
	for _, r := range rotas {
		if r.re.MatchString(rota) {
			return r.controller
		}
	}
	return "outros"
}

// Texto em que a busca procura: tudo que aparece na tela e no detalhe.
func (r *Registro) TextoBusca() string {
	var b strings.Builder
	b.WriteString(r.Nivel.String())
	b.WriteByte(' ')
	b.WriteString(r.RequestID)
	b.WriteByte(' ')
	b.WriteString(r.Logger)
	b.WriteByte(' ')
	b.WriteString(r.Controller)
	b.WriteByte(' ')
	b.WriteString(r.Mensagem)
	for _, c := range r.Continua {
		b.WriteByte('\n')
		b.WriteString(c)
	}
	return b.String()
}
