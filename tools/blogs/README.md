# blogs

Visualizador dos logs do booklit-server em produção (CloudWatch Logs `/booklit/prod`), no
terminal. Go puro, sem dependências: busca os eventos pelo AWS CLI (usa o profile do
`~/.aws`) e desenha a interface com sequências ANSI.

```
 blogs  /booklit/prod  últimos 1h  ● ao vivo  13/13 linhas  INFO 7  WARN 4  ERROR 2
 filtros: sem filtros
21:09:12 INFO  a1b2c3d4 Clube            200    35ms GET    /clubes  jota@booklit.bio
21:09:16 WARN  ee33ff44 Convite          409 em PUT /convites/8f1e/aceitar: Este convite já foi aceito
21:09:17 ERROR 99aa88bb LeituraClube     Falha ao consultar a API do Google Books ...  [+3 linhas]
21:09:20 ERROR 99aa88bb LeituraClube     502   812ms GET    /leituras/livros/buscar?q=Duna  jota@booklit.bio
```

## Instalar (WSL)

```bash
bash /mnt/d/Projetos/booklit-server/tools/blogs/instalar.sh
```

Cria `~/.local/bin/blogs` apontando para `tools/blogs/bin/blogs` e garante que essa pasta
está no PATH. Se o WSL tiver Go, compila antes; senão usa o binário que já está em `bin/`.

Precisa do AWS CLI no WSL com o profile `booklit` (o mesmo do Terraform). Outro profile:
`blogs -perfil outro` ou `export BLOGS_PERFIL=outro`.

## Usar

```bash
blogs                                   # interface ao vivo; ? mostra os atalhos
blogs -seguir=false                     # abre pausado
blogs -desde 6h -nivel ERROR            # já abre filtrado
blogs -texto -controller Convite        # imprime (bom para | grep, | less -R)
blogs -texto -seguir -status 500        # acompanha só as falhas 5xx
blogs -texto -req 99aa88bb              # todas as linhas de uma requisição
```

| Tecla | Ação |
|---|---|
| `/` | busca no texto (sem diferenciar maiúsculas); `re:` no começo usa regex |
| `c` | filtrar por controller (lista com contagem de linhas e erros) |
| `l` | nível mínimo: todos → INFO → WARN → ERROR |
| `e` | status HTTP: todos → 4xx e 5xx → só 5xx |
| `a` | só as linhas de requisição |
| `r` | só a requisição da linha selecionada (o fluxo inteiro dela) |
| `s` | stream: api + caddy → api → caddy |
| `t` | janela: 15min → 1h → 6h → 1d → 3d |
| `f` | pausa / retoma o ao vivo (abre ao vivo, atualizando a cada 2 s) |
| `v` | estatísticas: req, 4xx/5xx, média/p95/máx por controller e endpoint, mais lentas |
| `Enter` | detalhe: campos da requisição e stack trace inteiro (frames do Booklit em destaque) |
| `x` / `q` | limpa filtros / sai |

## Como o controller é descoberto

- Linha de requisição (`RequestLoggingFilter`): campo `handler=ClubeController#listar`.
  Em logs anteriores a esse campo, pela rota (tabela em `registro.go`, espelhando os
  `@RequestMapping`).
- Demais linhas (service, `GlobalExceptionHandler`, client): herdam do `requestId`.
- Sem requisição (boot, schedulers, outbox): aparecem como `(sem requisição)`.

Linhas de stack trace (o awslogs manda cada linha como um evento) são reagrupadas na linha
de log que as originou.

## Desenvolvimento

```bash
go test ./...
go run . -arquivo testdata/exemplo.json         # interface com dados de exemplo, sem AWS
CGO_ENABLED=0 go build -o bin/blogs .
```

Para guardar um trecho real e analisar offline:
`aws logs filter-log-events --log-group-name /booklit/prod --start-time <ms> > trecho.json`
e depois `blogs -arquivo trecho.json`.
