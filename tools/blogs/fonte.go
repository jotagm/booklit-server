package main

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"strconv"
	"strings"
	"time"
)

// Fonte busca eventos no CloudWatch Logs pelo AWS CLI. Usar o CLI (em vez do SDK) deixa o
// binário sem dependências e reaproveita o profile/credenciais que já estão no ~/.aws.
type Fonte struct {
	Grupo   string
	Streams []string
	Perfil  string
	Regiao  string
	Arquivo string // eventos de um JSON salvo (testes, análise offline)
	Limite  int
}

type respostaFiltro struct {
	Events    []Evento `json:"events"`
	NextToken string   `json:"NextToken"`
}

// Busca eventos a partir de "desde" (inclusive). Devolve também se bateu no limite.
func (f *Fonte) Busca(ctx context.Context, desde time.Time) ([]Evento, bool, error) {
	if f.Arquivo != "" {
		return f.doArquivo(desde)
	}
	args := []string{"logs", "filter-log-events",
		"--log-group-name", f.Grupo,
		"--start-time", strconv.FormatInt(desde.UnixMilli(), 10),
		"--max-items", strconv.Itoa(f.Limite),
		"--output", "json",
	}
	if len(f.Streams) > 0 {
		args = append(args, "--log-stream-names")
		args = append(args, f.Streams...)
	}
	if f.Regiao != "" {
		args = append(args, "--region", f.Regiao)
	}
	if f.Perfil != "" {
		args = append(args, "--profile", f.Perfil)
	}
	cmd := exec.CommandContext(ctx, "aws", args...)
	// O Floci deixa AWS_ENDPOINT_URL no ambiente; aqui o alvo é sempre a AWS de verdade.
	cmd.Env = semVariavel(os.Environ(), "AWS_ENDPOINT_URL")
	var saida, erro bytes.Buffer
	cmd.Stdout, cmd.Stderr = &saida, &erro
	if err := cmd.Run(); err != nil {
		if ctx.Err() != nil {
			return nil, false, ctx.Err()
		}
		var ee *exec.Error
		if errors.As(err, &ee) {
			return nil, false, fmt.Errorf("AWS CLI não encontrado no PATH (instale o aws v2)")
		}
		msg := strings.TrimSpace(erro.String())
		if msg == "" {
			msg = err.Error()
		}
		return nil, false, fmt.Errorf("aws logs: %s", ultimaLinha(msg))
	}
	var r respostaFiltro
	if err := json.Unmarshal(saida.Bytes(), &r); err != nil {
		return nil, false, fmt.Errorf("resposta inesperada do aws logs: %v", err)
	}
	return r.Events, r.NextToken != "", nil
}

func (f *Fonte) doArquivo(desde time.Time) ([]Evento, bool, error) {
	dados, err := os.ReadFile(f.Arquivo)
	if err != nil {
		return nil, false, err
	}
	var r respostaFiltro
	if err := json.Unmarshal(dados, &r); err != nil {
		// Aceita também uma lista pura de eventos.
		if err2 := json.Unmarshal(dados, &r.Events); err2 != nil {
			return nil, false, fmt.Errorf("%s: %v", f.Arquivo, err)
		}
	}
	var out []Evento
	for _, e := range r.Events {
		if e.Timestamp >= desde.UnixMilli() && (len(f.Streams) == 0 || contem(f.Streams, e.Stream)) {
			out = append(out, e)
		}
	}
	return out, false, nil
}

func semVariavel(env []string, nome string) []string {
	out := env[:0:0]
	for _, kv := range env {
		if !strings.HasPrefix(kv, nome+"=") {
			out = append(out, kv)
		}
	}
	return out
}

func ultimaLinha(s string) string {
	linhas := strings.Split(strings.TrimSpace(s), "\n")
	return strings.TrimSpace(linhas[len(linhas)-1])
}

func contem(lista []string, v string) bool {
	for _, x := range lista {
		if x == v {
			return true
		}
	}
	return false
}
