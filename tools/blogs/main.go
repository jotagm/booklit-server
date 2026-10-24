package main

import (
	"flag"
	"fmt"
	"os"
	"strings"
	"time"
)

const versao = "0.1.0"

func main() {
	var (
		grupo      = flag.String("grupo", "/booklit/prod", "log group do CloudWatch")
		perfil     = flag.String("perfil", padrao(os.Getenv("BLOGS_PERFIL"), "booklit"), "profile do AWS CLI (env BLOGS_PERFIL)")
		regiao     = flag.String("regiao", padrao(os.Getenv("BLOGS_REGIAO"), "us-east-1"), "região da AWS")
		stream     = flag.String("stream", "", "api, caddy ou vazio para os dois")
		desde      = flag.Duration("desde", time.Hour, "janela: 15m, 1h, 6h, 24h, 72h")
		nivel      = flag.String("nivel", "", "nível mínimo: INFO, WARN, ERROR")
		controller = flag.String("controller", "", "filtra por controller (ex.: ClubeController ou Clube)")
		busca      = flag.String("busca", "", "texto a buscar; \"re:\" no começo usa regex")
		req        = flag.String("req", "", "requestId: só as linhas dessa requisição")
		status     = flag.Int("status", 0, "só requisições com status >= N (ex.: 400, 500)")
		acessos    = flag.Bool("acessos", false, "só as linhas de requisição")
		seguir     = flag.Bool("seguir", false, "ao vivo (na TUI liga o modo; em texto fica imprimindo)")
		texto      = flag.Bool("texto", false, "imprime no terminal em vez de abrir a interface")
		arquivo    = flag.String("arquivo", "", "lê eventos de um JSON salvo do filter-log-events (offline)")
		limite     = flag.Int("limite", 20000, "máximo de eventos por busca")
		mostraVer  = flag.Bool("versao", false, "mostra a versão")
	)
	flag.Usage = func() {
		fmt.Fprintf(os.Stderr, "blogs %s — logs do booklit-server no CloudWatch\n\n", versao)
		fmt.Fprintf(os.Stderr, "uso: blogs [opções]        (sem opções abre a interface; ? mostra os atalhos)\n\n")
		flag.PrintDefaults()
		fmt.Fprintf(os.Stderr, "\nexemplos:\n  blogs\n  blogs -desde 6h -nivel ERROR\n  blogs -texto -controller Convite -busca \"já foi aceito\"\n  blogs -texto -seguir -status 500\n")
	}
	flag.Parse()
	if *mostraVer {
		fmt.Println(versao)
		return
	}

	fonte := &Fonte{Grupo: *grupo, Perfil: *perfil, Regiao: *regiao, Arquivo: *arquivo, Limite: *limite}
	if *stream != "" {
		fonte.Streams = []string{*stream}
	}

	f := Filtro{NivelMinimo: SemNivel, Busca: *busca, RequestID: *req, StatusMin: *status, SoAcessos: *acessos}
	if *nivel != "" {
		if f.NivelMinimo = parseNivel(*nivel); f.NivelMinimo == SemNivel {
			falha("nível inválido: %s (use INFO, WARN ou ERROR)", *nivel)
		}
	}
	if *controller != "" {
		f.Controller = nomeController(*controller)
	}

	janela := indiceJanela(*desde)
	var err error
	if *texto || !terminalInterativo() {
		err = RodaTexto(fonte, f, *desde, *seguir)
	} else {
		err = RodaTUI(fonte, f, janela, *seguir)
	}
	if err != nil {
		falha("%v", err)
	}
}

// "Clube" e "clubecontroller" viram "ClubeController".
func nomeController(s string) string {
	s = strings.TrimSpace(s)
	if !strings.HasSuffix(strings.ToLower(s), "controller") {
		s += "Controller"
	}
	base := s[:len(s)-len("controller")]
	if base == "" {
		return s
	}
	return strings.ToUpper(base[:1]) + base[1:] + "Controller"
}

// A TUI anda pelas janelas fixas; -desde escolhe a mais próxima que cobre o pedido.
func indiceJanela(d time.Duration) int {
	for i, j := range janelas {
		if d <= j {
			return i
		}
	}
	return len(janelas) - 1
}

func terminalInterativo() bool {
	info, err := os.Stdout.Stat()
	return err == nil && info.Mode()&os.ModeCharDevice != 0
}

func padrao(v, p string) string {
	if v == "" {
		return p
	}
	return v
}

func falha(formato string, args ...any) {
	fmt.Fprintf(os.Stderr, "blogs: "+formato+"\n", args...)
	os.Exit(1)
}
