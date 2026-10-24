package main

import (
	"context"
	"fmt"
	"os"
	"time"
)

// modoTexto imprime no stdout, sem interface (para | grep, | less -R, ou sem terminal).
// Um registro só sai quando já chegou o seguinte do mesmo stream, para o stack trace dele
// estar completo.
func modoTexto(fonte Fonte, e Escopo, modo Modo, janela, intervalo time.Duration, seguir bool) error {
	comCor := stdoutEhTerminal()
	m := NovoMontador(0)
	impressos := 0
	imprime := func(ate int) {
		for ; impressos < ate; impressos++ {
			r := m.Registros[impressos]
			if !e.Filtra(r) || !Mostra(r, modo) {
				continue
			}
			for _, l := range Formata(r, modo, true) {
				fmt.Println(Desenha(l, "", comCor))
			}
		}
	}

	ctx := context.Background()
	cursor := time.Now().Add(-janela)
	if fonte.Arquivo != "" {
		cursor = time.Time{}
	}
	for {
		eventos, cheio, err := fonte.Busca(ctx, cursor)
		if err != nil {
			if !seguir {
				return err
			}
			fmt.Fprintln(os.Stderr, "blogs:", err)
		}
		m.Adiciona(eventos)
		for _, e := range eventos {
			if c := time.UnixMilli(e.Timestamp).Add(-15 * time.Second); c.After(cursor) {
				cursor = c
			}
		}
		if !seguir && !cheio {
			imprime(len(m.Registros))
			return nil
		}
		imprime(ultimoFechado(m))
		if !cheio {
			time.Sleep(intervalo)
		}
	}
}

// Índice até onde os registros estão fechados: o último de cada stream que acabou de chegar
// ainda pode receber linhas de stack trace na próxima busca.
func ultimoFechado(m *Montador) int {
	fim := len(m.Registros)
	for _, r := range m.ultimo {
		if r.rodada != m.rodada {
			continue
		}
		for i := fim - 1; i >= 0; i-- {
			if m.Registros[i] == r {
				fim = i
				break
			}
		}
	}
	return fim
}

func stdoutEhTerminal() bool {
	info, err := os.Stdout.Stat()
	return err == nil && info.Mode()&os.ModeCharDevice != 0
}
