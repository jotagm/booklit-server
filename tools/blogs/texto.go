package main

import (
	"fmt"
	"os"
	"strings"
	"time"
)

// RodaTexto imprime as linhas filtradas: serve para pipe (grep, less) e para scripts.
func RodaTexto(fonte *Fonte, f Filtro, janela time.Duration, seguir bool) error {
	cor := terminalInterativo()
	m := NovoMontador()
	desde := time.Now().Add(-janela)
	impressos := map[*Registro]int{} // continuações já impressas por registro

	for {
		ev, trunc, err := fonte.Busca(desde)
		if err != nil {
			return err
		}
		if trunc {
			fmt.Fprintln(os.Stderr, "blogs: limite de eventos atingido; use -desde menor ou -limite maior")
		}
		m.Adiciona(ev)
		for _, e := range ev {
			if t := time.UnixMilli(e.Timestamp).Add(-20 * time.Second); t.After(desde) {
				desde = t
			}
		}
		for _, r := range Filtra(m.Registros, &f) {
			ja, visto := impressos[r]
			if !visto {
				fmt.Println(formataTexto(r, cor))
			}
			for _, c := range r.Continua[ja:] {
				fmt.Println("    " + c)
			}
			impressos[r] = len(r.Continua)
		}
		if !seguir {
			return nil
		}
		time.Sleep(3 * time.Second)
	}
}

func formataTexto(r *Registro, cor bool) string {
	ctrl := strings.TrimSuffix(r.Controller, "Controller")
	if ctrl == "" {
		ctrl = "-"
	}
	req := r.RequestID
	if req == "" {
		req = "-"
	}
	msg := r.Mensagem
	if !cor {
		return fmt.Sprintf("%s %-5s %-8s %-14s %s", r.Momento.Format("2006-01-02 15:04:05.000"), r.Nivel, req, ctrl, msg)
	}
	corMsg := ""
	if r.Acesso != nil {
		corMsg = corDoStatus(r.Acesso.Status)
	} else if r.Nivel == Error {
		corMsg = vermelho
	}
	return fmt.Sprintf("%s%s%s %s%-5s%s %s%-8s%s %s%-14s%s %s%s%s",
		cinza, r.Momento.Format("2006-01-02 15:04:05"), reset,
		corDoNivel(r.Nivel), r.Nivel, reset,
		apagado, req, reset,
		azul, ctrl, reset,
		corMsg, msg, reset)
}
