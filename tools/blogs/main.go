package main

import (
	"flag"
	"fmt"
	"os"
	"time"
)

const versao = "0.2.0"

func main() {
	var (
		grupo      = flag.String("grupo", "/booklit/prod", "log group do CloudWatch")
		perfil     = flag.String("perfil", padrao(os.Getenv("BLOGS_PERFIL"), "booklit"), "profile do AWS CLI (env BLOGS_PERFIL)")
		regiao     = flag.String("regiao", padrao(os.Getenv("BLOGS_REGIAO"), "us-east-1"), "região da AWS")
		controller = flag.String("controller", "", "controller (ou pedaço do nome: convite), api, sem ou caddy; pula o menu")
		modo       = flag.String("modo", "", "todos, info ou erros (pula o menu)")
		desde      = flag.Duration("desde", 15*time.Minute, "quanto do passado carregar ao abrir: 15m, 1h, 6h...")
		intervalo  = flag.Duration("intervalo", 2*time.Second, "intervalo entre buscas de linhas novas")
		texto      = flag.Bool("texto", false, "imprime no terminal em vez de abrir a interface (bom para | grep)")
		seguir     = flag.Bool("seguir", true, "no -texto, continua acompanhando (-seguir=false imprime e sai)")
		arquivo    = flag.String("arquivo", "", "lê eventos de um JSON salvo do filter-log-events (offline)")
		limite     = flag.Int("limite", 10000, "máximo de eventos por busca")
		mostraVer  = flag.Bool("versao", false, "mostra a versão")
	)
	flag.Usage = func() {
		fmt.Fprintf(os.Stderr, "blogs %s — logs do booklit-server no CloudWatch\n\n", versao)
		fmt.Fprintf(os.Stderr, "uso: blogs [opções]     (sem opções: menus de controller e tipo de log)\n\n")
		flag.PrintDefaults()
		fmt.Fprintf(os.Stderr, "\nexemplos:\n  blogs\n  blogs -controller convite -modo erros\n"+
			"  blogs -desde 2h\n  blogs -texto -controller voto | less -R\n")
	}
	flag.Parse()
	if *mostraVer {
		fmt.Println(versao)
		return
	}
	if *intervalo < time.Second {
		*intervalo = time.Second
	}

	var e *Escopo
	if *controller != "" {
		v, ok := escopoPorNome(*controller)
		if !ok {
			falha("controller desconhecida ou ambígua: %s (ex.: Convite, ClubeController, api, sem, caddy)", *controller)
		}
		e = &v
	}
	var m *Modo
	if *modo != "" {
		v, ok := parseModo(*modo)
		if !ok {
			falha("modo inválido: %s (use todos, info ou erros)", *modo)
		}
		m = &v
	}

	fonte := Fonte{Grupo: *grupo, Perfil: *perfil, Regiao: *regiao, Arquivo: *arquivo, Limite: *limite}

	var term *Terminal
	if !*texto {
		t, err := AbreTerminal()
		if err != nil {
			fmt.Fprintf(os.Stderr, "blogs: %v — usando o modo texto\n", err)
		} else {
			term = t
		}
	}
	if term == nil {
		if e == nil {
			v := escopos()[0]
			e = &v
		}
		if m == nil {
			v := ModoTodos
			m = &v
		}
		if err := modoTexto(fonte, *e, *m, *desde, *intervalo, *seguir); err != nil {
			falha("%v", err)
		}
		return
	}

	defer term.Fecha()
	NovoApp(term, fonte, *desde, *intervalo).Roda(e, m)
	term.Fecha()
	fmt.Println(cAmarelo + "⏹  Até mais!" + cReset)
	os.Exit(0)
}

func padrao(v, p string) string {
	if v == "" {
		return p
	}
	return v
}

func falha(f string, a ...any) {
	fmt.Fprintf(os.Stderr, "blogs: "+f+"\n", a...)
	os.Exit(1)
}
