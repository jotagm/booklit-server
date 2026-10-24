package main

import "strings"

// Escopo é o que se escolhe no primeiro menu (o "serviço" do log_viewer.py): uma controller
// do booklit-server, a API inteira, as linhas fora de requisição ou o Caddy.
type Escopo struct {
	Nome    string
	Detalhe string
	Cor     string
	Filtra  func(r *Registro) bool
}

// Controllers do booklit-server (@RestController em src/main/java). Uma controller nova que
// não esteja aqui continua aparecendo em "API inteira" e na busca.
var controllers = []string{
	"AuthController",
	"ClubeController",
	"ComentarioController",
	"ConviteController",
	"EstanteController",
	"FeedController",
	"LeituraClubeController",
	"OpcaoVotoController",
	"OtpController",
	"RegistroController",
	"TemaController",
	"UsuarioClubeController",
	"UsuarioController",
	"VotacaoController",
	"VotoController",
}

func escopos() []Escopo {
	out := []Escopo{{
		Nome: "API inteira", Detalhe: "(todas as controllers)", Cor: cVerde,
		Filtra: func(r *Registro) bool { return r.Stream == "api" },
	}}
	for _, c := range controllers {
		c := c
		out = append(out, Escopo{Nome: c, Cor: cCiano,
			Filtra: func(r *Registro) bool { return r.Stream == "api" && r.Controller == c }})
	}
	return append(out,
		Escopo{Nome: "Sem controller", Detalhe: "(boot, schedulers, outbox, 401/404 antes da controller)", Cor: cMagenta,
			Filtra: func(r *Registro) bool { return r.Stream == "api" && r.Controller == "" }},
		Escopo{Nome: "caddy", Detalhe: "(proxy HTTPS)", Cor: cAzul,
			Filtra: func(r *Registro) bool { return r.Stream == "caddy" }},
	)
}

// escopoPorNome aceita o nome completo ou um pedaço: "convite" vira ConviteController.
func escopoPorNome(n string) (Escopo, bool) {
	n = strings.ToLower(strings.TrimSpace(n))
	todos := escopos()
	switch n {
	case "api", "todos", "todas", "":
		return todos[0], true
	case "sem", "nenhuma", "outros":
		return todos[len(todos)-2], true
	case "caddy":
		return todos[len(todos)-1], true
	}
	var achados []Escopo
	for _, e := range todos[1 : len(todos)-2] {
		nome := strings.ToLower(e.Nome)
		if nome == n || nome == n+"controller" {
			return e, true
		}
		if strings.Contains(nome, n) {
			achados = append(achados, e)
		}
	}
	if len(achados) == 1 {
		return achados[0], true
	}
	return Escopo{}, false
}
