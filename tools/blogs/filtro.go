package main

import (
	"regexp"
	"strings"
)

// Filtro reúne tudo que restringe a lista. Valor zero = mostra tudo.
type Filtro struct {
	NivelMinimo Nivel  // SemNivel desligado; Warn mostra WARN e ERROR
	Controller  string // "" = todos; "(sem)" = linhas sem requisição
	Busca       string // texto; "re:" no começo vira expressão regular
	RequestID   string
	Stream      string // "" = todos
	SoAcessos   bool   // só as linhas de requisição (uma por chamada)
	StatusMin   int    // 400 = 4xx e 5xx, 500 = só 5xx

	reBusca    *regexp.Regexp
	reBuscaErr error
	buscaFonte string
}

const semController = "(sem requisição)"

func (f *Filtro) compila() {
	if f.buscaFonte == f.Busca {
		return
	}
	f.buscaFonte = f.Busca
	f.reBusca, f.reBuscaErr = nil, nil
	if expr, ok := strings.CutPrefix(f.Busca, "re:"); ok {
		f.reBusca, f.reBuscaErr = regexp.Compile("(?i)" + expr)
	}
}

// ErroBusca devolve o erro da expressão regular, para mostrar no rodapé.
func (f *Filtro) ErroBusca() error {
	f.compila()
	return f.reBuscaErr
}

func (f *Filtro) Aceita(r *Registro) bool {
	if f.Stream != "" && r.Stream != f.Stream {
		return false
	}
	if f.NivelMinimo != SemNivel && (r.Nivel == SemNivel || r.Nivel < f.NivelMinimo) {
		return false
	}
	if f.RequestID != "" && r.RequestID != f.RequestID {
		return false
	}
	if f.Controller != "" {
		if f.Controller == semController {
			if r.Controller != "" {
				return false
			}
		} else if r.Controller != f.Controller {
			return false
		}
	}
	if f.SoAcessos && r.Acesso == nil {
		return false
	}
	if f.StatusMin > 0 && (r.Acesso == nil || r.Acesso.Status < f.StatusMin) {
		return false
	}
	if f.Busca != "" {
		f.compila()
		texto := r.TextoBusca()
		if f.reBusca != nil {
			return f.reBusca.MatchString(texto)
		}
		if f.reBuscaErr != nil {
			return true // expressão ainda sendo digitada: não esconde nada
		}
		return strings.Contains(strings.ToLower(texto), strings.ToLower(f.Busca))
	}
	return true
}

func (f *Filtro) Ativo() bool {
	return f.NivelMinimo != SemNivel || f.Controller != "" || f.Busca != "" || f.RequestID != "" ||
		f.Stream != "" || f.SoAcessos || f.StatusMin > 0
}

// Descricao resume os filtros ativos para o cabeçalho.
func (f *Filtro) Descricao() string {
	var p []string
	if f.Stream != "" {
		p = append(p, "stream="+f.Stream)
	}
	if f.NivelMinimo != SemNivel {
		p = append(p, "nível≥"+f.NivelMinimo.String())
	}
	if f.Controller != "" {
		p = append(p, "controller="+f.Controller)
	}
	if f.SoAcessos {
		p = append(p, "só requisições")
	}
	if f.StatusMin > 0 {
		p = append(p, "status≥"+itoa(f.StatusMin))
	}
	if f.RequestID != "" {
		p = append(p, "req="+f.RequestID)
	}
	if f.Busca != "" {
		p = append(p, "busca=\""+f.Busca+"\"")
	}
	if len(p) == 0 {
		return "sem filtros"
	}
	return strings.Join(p, "  ")
}

func Filtra(registros []*Registro, f *Filtro) []*Registro {
	out := make([]*Registro, 0, len(registros))
	for _, r := range registros {
		if f.Aceita(r) {
			out = append(out, r)
		}
	}
	return out
}
