# blogs

Visualizador dos logs do booklit-server em produção (CloudWatch Logs `/booklit/prod`) no
terminal. É a versão em Go do `log_viewer.py` usado com o Railway: o mesmo fluxo de menus,
as mesmas cores e o mesmo campo de busca ao vivo, mas, no lugar dos serviços do Railway, o
primeiro menu lista as controllers do booklit-server. Go puro, sem dependências; busca os
eventos pelo AWS CLI (profile do `~/.aws`).

```
 API inteira │ /booklit/prod │ Modo: TODOS │ ● ao vivo · 23:46:01 │ 41 linhas
● 21:10:05 a1b2c3d4      RequestLoggingFilter │ GET /clubes -> 200 (35ms) usuario=jota@booklit.bio handler=ClubeController#listar
▲ 21:10:08 ee33ff44    GlobalExceptionHandler │ 409 em PUT /convites/8f1e/aceitar: Este convite já foi aceito
✖ 21:10:12 77aa66bb    GlobalExceptionHandler │ Erro inesperado em POST /votos
  │   ├─ org.springframework.dao.DataIntegrityViolationException: could not execute statement
  │     ├─ Caused by: org.hibernate.exception.ConstraintViolationException: could not execute statement
  │       └─▸ Caused by: org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint "uk_voto"
  │   at github.jotagm.clube_livro.application.service.VotoService.votar(VotoService.java:48)
  │   at github.jotagm.clube_livro.adapter.in.rest.VotoController.votar(VotoController.java:31)
  ╰──────────────────────────────────────────────────

Buscar: voto
```

## Instalar (WSL)

```bash
bash /mnt/d/Projetos/booklit-server/tools/blogs/instalar.sh
```

Cria `~/.local/bin/blogs` apontando para `tools/blogs/bin/blogs`. Se o WSL tiver Go, compila
antes; senão usa o binário que já está em `bin/`. Precisa do AWS CLI com o profile `booklit`
(outro: `blogs -perfil outro` ou `export BLOGS_PERFIL=outro`).

## Usar

`blogs` abre os menus. Primeiro a **controller**, já com as requisições, warns e erros de
cada uma nos últimos 15 minutos:

```
  ›  1 │ API inteira               7 req  4 warn  4 erros    (todas as controllers)
     2 │ AuthController            —
     3 │ ClubeController           1 req
     5 │ ConviteController         1 req  2 warn
    16 │ VotoController            1 req  2 erros
    17 │ Sem controller            2 req                     (boot, schedulers, outbox, 401/404 antes da controller)
    18 │ caddy                     1 req                     (proxy HTTPS)
```

Depois o **tipo de log**:

| Tipo | Mostra |
|---|---|
| Todos os logs | tudo |
| Informativos | só INFO, sem ruído (health check, api-docs) |
| Erros completos | ERROR e WARN em blocos, com a cadeia de causas e a causa raiz em destaque |

Uma controller mostra todas as linhas das requisições que ela atendeu: o log de acesso
(`handler=VotoController#votar`) e também as do service, do client e do
`GlobalExceptionHandler` da mesma requisição, ligadas pelo `requestId`.

Na tela de logs, o campo **Buscar:** fica sempre ativo: digite e a lista filtra na hora (sem
pausar a atualização). A busca procura também no stack trace.

| Tecla | Ação |
|---|---|
| digitar | filtra; `Backspace` apaga, `Esc` limpa |
| `↑` `↓` `PgUp` `PgDn` `Home` | rola; a tela **pausa** onde você parou |
| `End` | volta ao vivo (rolar até o fim também volta) |
| `Tab` | alterna stack trace resumido / completo |
| `Ctrl+C` | menu: outra controller, outro tipo de log, voltar ou sair |

Ao abrir, carrega os últimos 15 minutos e depois busca o que chegou a cada 2 segundos. A
busca roda o tempo todo, inclusive nos menus; trocar de controller só muda o filtro.

```bash
blogs                                  # menus
blogs -controller convite -modo erros  # pula os menus (aceita pedaço do nome)
blogs -desde 2h                        # carrega mais histórico ao abrir
blogs -texto -controller voto | less -R    # sem interface, acompanhando
blogs -texto -seguir=false -desde 1h > logs.txt
```

## Desenvolvimento

```bash
go test ./...
go run . -arquivo testdata/exemplo.json          # sem AWS, com dados de exemplo
CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -trimpath -ldflags "-s -w" -o bin/blogs .
```

Controller nova no booklit-server: acrescente o nome na lista de `escopo.go`. O pacote dos
frames do stack resumido (`pacotesDoProjeto`) e o ruído do modo Informativos (`ruido`) ficam
em `formato.go`.
