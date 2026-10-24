package main

import (
	"encoding/json"
	"fmt"
	"regexp"
	"sort"
	"strings"
	"time"
)

// Nivel de uma linha de log. Linhas sem nível reconhecível contam como INFO, como no
// log_viewer.py do Railway.
type Nivel int

const (
	Debug Nivel = iota
	Info
	Warn
	Error
)

func (n Nivel) String() string {
	switch n {
	case Debug:
		return "DEBUG"
	case Warn:
		return "WARN"
	case Error:
		return "ERROR"
	}
	return "INFO"
}

func parseNivel(s string) Nivel {
	switch strings.ToUpper(strings.TrimSpace(s)) {
	case "TRACE", "DEBUG":
		return Debug
	case "WARN", "WARNING":
		return Warn
	case "ERROR", "FATAL", "PANIC", "DPANIC":
		return Error
	}
	return Info
}

// Evento cru do CloudWatch Logs (filter-log-events).
type Evento struct {
	ID        string `json:"eventId"`
	Stream    string `json:"logStreamName"`
	Timestamp int64  `json:"timestamp"`
	Mensagem  string `json:"message"`
}

// Registro é uma entrada lógica de log: a linha principal mais as continuações (stack
// trace), que o awslogs manda como eventos separados.
type Registro struct {
	Momento    time.Time
	Hora       string // HH:MM:SS
	Stream     string // api | caddy
	Nivel      Nivel
	RequestID  string // "" fora de requisição (boot, scheduler)
	Classe     string // nome curto do logger
	Mensagem   string
	Continua   []string
	Solta      bool   // linha de exceção sem a linha de log que a originou
	Controller string // do log de acesso da requisição (handler=XController#metodo)
	rodada     int    // última chamada de Adiciona que mexeu nele
}

// Padrão de console do application.yaml:
//
//	%d{HH:mm:ss.SSS} %5p [%X{requestId:-________}] %-40.40logger{39} %m
var reBooklit = regexp.MustCompile(`^(\d{2}:\d{2}:\d{2})\.\d{3}\s+(TRACE|DEBUG|INFO|WARN|ERROR)\s+\[([^\]]*)\]\s+(\S+)\s+(.*)$`)

// Padrões do Spring Boot sem o pattern customizado (por exemplo, se ele mudar).
var (
	reSpringISO = regexp.MustCompile(`^\d{4}-\d{2}-\d{2}T(\d{2}:\d{2}:\d{2})\.\d{3}\S*\s+(\w+)\s+\d+\s+---\s+(?:\[[^\]]*\]\s*)+(\S+)\s*:\s+(.*)$`)
	reSpring    = regexp.MustCompile(`^\d{4}-\d{2}-\d{2}\s+(\d{2}:\d{2}:\d{2})\.\d{3}\s+(\w+)\s+\d+\s+---\s+\[\s*[^\]]+?\s*\]\s+(\S+)\s+:\s+(.*)$`)
)

var (
	reAnsi    = regexp.MustCompile("\x1b\\[[0-9;]*m")
	reHandler = regexp.MustCompile(` handler=([A-Za-z0-9_]+)#`)
	reMais    = regexp.MustCompile(`^\.\.\. \d+ (more|common frames omitted)`)
)

var fuso = carregaFuso()

func carregaFuso() *time.Location {
	if l, err := time.LoadLocation("America/Sao_Paulo"); err == nil {
		return l
	}
	return time.FixedZone("-03", -3*3600)
}

// Montador transforma eventos em registros, juntando o stack trace à linha que o originou.
// É incremental: cada busca do polling acrescenta o que chegou de novo.
type Montador struct {
	Registros  []*Registro
	Maximo     int
	vistos     map[string]int64
	ultimo     map[string]*Registro // último registro por stream
	porRequest map[string]string    // requestId -> controller
	Versao     int                  // muda sempre que algo é acrescentado
	rodada     int
}

func NovoMontador(maximo int) *Montador {
	return &Montador{Maximo: maximo, vistos: map[string]int64{}, ultimo: map[string]*Registro{},
		porRequest: map[string]string{}}
}

// Adiciona eventos (em qualquer ordem, com repetidos) e devolve quantos eram novos.
func (m *Montador) Adiciona(eventos []Evento) int {
	sort.SliceStable(eventos, func(i, j int) bool { return eventos[i].Timestamp < eventos[j].Timestamp })
	m.rodada++
	novos := 0
	var maisRecente int64
	for _, e := range eventos {
		if e.ID != "" {
			if _, ok := m.vistos[e.ID]; ok {
				continue
			}
			m.vistos[e.ID] = e.Timestamp
		}
		novos++
		m.adicionaUm(e)
		if e.Timestamp > maisRecente {
			maisRecente = e.Timestamp
		}
	}
	if novos == 0 {
		return 0
	}
	m.Versao++
	// O polling repete uma folga de alguns segundos; ids mais antigos que isso já não voltam.
	if len(m.vistos) > 20000 {
		limite := maisRecente - int64(10*time.Minute/time.Millisecond)
		for id, ts := range m.vistos {
			if ts < limite {
				delete(m.vistos, id)
			}
		}
	}
	if m.Maximo > 0 && len(m.Registros) > m.Maximo {
		corte := len(m.Registros) - m.Maximo*4/5
		m.Registros = append([]*Registro(nil), m.Registros[corte:]...)
	}
	return novos
}

func (m *Montador) adicionaUm(e Evento) {
	texto := strings.TrimRight(reAnsi.ReplaceAllString(e.Mensagem, ""), "\r\n ")
	if strings.TrimSpace(texto) == "" {
		return
	}
	stream := e.Stream
	if stream == "" {
		stream = "api"
	}
	momento := time.UnixMilli(e.Timestamp).In(fuso)

	if r := parseLinha(texto); r != nil {
		r.Momento, r.Stream = momento, stream
		if r.Hora == "" {
			r.Hora = momento.Format("15:04:05")
		}
		if stream == "caddy" {
			r.Controller = "caddy"
			r.Classe = strings.TrimSuffix("caddy:"+r.Classe, ":")
		} else if r.RequestID != "" {
			if c := reHandler.FindStringSubmatch(r.Mensagem); c != nil {
				m.defineController(r.RequestID, c[1])
			}
			r.Controller = m.porRequest[r.RequestID]
		}
		r.rodada = m.rodada
		m.Registros = append(m.Registros, r)
		m.ultimo[stream] = r
		return
	}

	// Stack trace: "java.lang.X: msg", "\tat ...", "Caused by: ...", "\t... 12 more".
	if ant := m.ultimo[stream]; ant != nil && pareceContinuacao(texto, ant) {
		ant.Continua = append(ant.Continua, strings.TrimSpace(texto))
		ant.rodada = m.rodada
		return
	}

	r := &Registro{Momento: momento, Hora: momento.Format("15:04:05"), Stream: stream, Nivel: Info,
		Mensagem: texto, rodada: m.rodada}
	if strings.Contains(texto, "Exception") {
		r.Nivel, r.Solta = Error, true
	}
	m.Registros = append(m.Registros, r)
	m.ultimo[stream] = r
}

// O log de acesso é a última linha da requisição: as anteriores (service, handler de erro)
// só descobrem o controller quando ele chega.
func (m *Montador) defineController(requestID, controller string) {
	if m.porRequest[requestID] == controller {
		return
	}
	m.porRequest[requestID] = controller
	for i := len(m.Registros) - 1; i >= 0 && i >= len(m.Registros)-500; i-- {
		if r := m.Registros[i]; r.RequestID == requestID {
			r.Controller = controller
		}
	}
}

func pareceContinuacao(texto string, ant *Registro) bool {
	t := strings.TrimSpace(texto)
	if strings.HasPrefix(t, "at ") || strings.HasPrefix(t, "Caused by") || strings.HasPrefix(t, "Suppressed:") ||
		reMais.MatchString(t) {
		return true
	}
	if texto[0] == '\t' {
		return true
	}
	// A linha com a exceção em si vem logo depois de um ERROR/WARN.
	if strings.Contains(t, "Exception") || strings.Contains(t, "Error:") || strings.Contains(t, "Throwable") {
		return ant.Nivel >= Warn || len(ant.Continua) > 0
	}
	return false
}

func parseLinha(texto string) *Registro {
	if g := reBooklit.FindStringSubmatch(texto); g != nil {
		return &Registro{Hora: g[1], Nivel: parseNivel(g[2]), RequestID: limpaRequestID(g[3]),
			Classe: classeCurta(g[4]), Mensagem: g[5]}
	}
	if g := reSpringISO.FindStringSubmatch(texto); g != nil {
		return &Registro{Hora: g[1], Nivel: parseNivel(g[2]), Classe: classeCurta(g[3]), Mensagem: g[4]}
	}
	if g := reSpring.FindStringSubmatch(texto); g != nil {
		return &Registro{Hora: g[1], Nivel: parseNivel(g[2]), Classe: classeCurta(g[3]), Mensagem: g[4]}
	}
	if strings.HasPrefix(texto, "{") {
		return parseJSON(texto)
	}
	return nil
}

// Linha JSON estruturada: o Caddy loga assim ({"level","ts","logger","msg",...}).
func parseJSON(texto string) *Registro {
	var d map[string]any
	if err := json.Unmarshal([]byte(texto), &d); err != nil {
		return nil
	}
	nivel, temNivel := texto2(d, "level", "severity")
	msg, temMsg := texto2(d, "msg", "message")
	if !temNivel || !temMsg {
		return nil
	}
	r := &Registro{Nivel: parseNivel(nivel), Mensagem: msg}
	logger, _ := texto2(d, "logger", "logger_name")
	r.Classe = classeCurta(logger)
	acesso := strings.Contains(logger, ".access")
	if acesso {
		r.Classe = "access"
	}
	if ts, ok := d["ts"].(float64); ok {
		r.Hora = time.UnixMilli(int64(ts * 1000)).In(fuso).Format("15:04:05")
	}

	// Log de acesso do Caddy: vira "GET /rota -> 200 (12ms)", como o da API.
	if req, ok := d["request"].(map[string]any); ok && acesso {
		metodo, _ := req["method"].(string)
		uri, _ := req["uri"].(string)
		r.Mensagem = fmt.Sprintf("%s %s", metodo, uri)
		if st, ok := d["status"].(float64); ok {
			r.Mensagem += fmt.Sprintf(" -> %d", int(st))
		}
		if dur, ok := d["duration"].(float64); ok {
			r.Mensagem += fmt.Sprintf(" (%dms)", int(dur*1000))
		}
		if ip, ok := req["remote_ip"].(string); ok {
			r.Mensagem += " ip=" + ip
		}
		return r
	}

	// Demais linhas: a mensagem mais os campos simples (domínio do certificado, erro...).
	var extras []string
	for _, k := range ordenadas(d) {
		switch k {
		case "level", "severity", "ts", "logger", "logger_name", "msg", "message":
			continue
		}
		switch v := d[k].(type) {
		case string:
			extras = append(extras, k+"="+v)
		case float64, bool:
			extras = append(extras, fmt.Sprintf("%s=%v", k, v))
		case []any:
			if len(v) > 0 && len(v) <= 5 {
				extras = append(extras, fmt.Sprintf("%s=%v", k, v))
			}
		}
	}
	if len(extras) > 0 {
		r.Mensagem += "  " + strings.Join(extras, " ")
	}
	return r
}

func texto2(d map[string]any, chaves ...string) (string, bool) {
	for _, k := range chaves {
		if s, ok := d[k].(string); ok {
			return s, true
		}
	}
	return "", false
}

func ordenadas(d map[string]any) []string {
	out := make([]string, 0, len(d))
	for k := range d {
		out = append(out, k)
	}
	sort.Strings(out)
	return out
}

func classeCurta(nome string) string {
	if i := strings.LastIndexByte(nome, '.'); i >= 0 && i < len(nome)-1 {
		return nome[i+1:]
	}
	return nome
}

func limpaRequestID(s string) string {
	s = strings.TrimSpace(s)
	if strings.Trim(s, "_") == "" {
		return ""
	}
	return s
}

// TextoBusca é onde a busca procura: tudo que a linha mostra, mais o stack inteiro e o
// controller da requisição (buscar "ClubeController" traz o fluxo todo dela).
func (r *Registro) TextoBusca() string {
	var b strings.Builder
	for _, s := range []string{r.Hora, r.Stream, r.Nivel.String(), r.RequestID, r.Classe, r.Controller, r.Mensagem} {
		b.WriteString(s)
		b.WriteByte(' ')
	}
	for _, c := range r.Continua {
		b.WriteString(c)
		b.WriteByte('\n')
	}
	return strings.ToLower(b.String())
}
