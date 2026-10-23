# auth-core

Núcleo de autenticação em Java 21 puro, sem Spring. A lógica sensível mora aqui, isolada e
testável sem subir contexto; quem usa (hoje o booklit-server) implementa as portas.

## Fatia 1: OTP por e-mail

| Peça | Papel |
|---|---|
| `ServicoOtpEmail` | emite e verifica; aplica intervalo de reenvio e teto por janela |
| `RepositorioDeDesafios` | porta de persistência; `reservarTentativa` e `consumir` **precisam ser atômicos** |
| `EnviadorDeOtp` | porta de entrega; único ponto em que o código aparece em claro |
| `HasherDeCodigo` | HMAC-SHA256 amarrado a desafio + finalidade; comparação em tempo constante |
| `CodigoOtp` | normalização: só 6 dígitos ASCII, aceita espaço e hífen |
| `PoliticaOtp` | 10 min de validade, 5 tentativas, 60 s entre envios, 5 envios/hora |

Garantias (com o repositório cumprindo o contrato): o código nunca é persistido; no máximo N
verificações por desafio mesmo sob concorrência; código vale uma vez e só para a finalidade
com que foi emitido; `emitirSemEntrega` responde igual a `emitir`, para não revelar quem
tem conta.

## Build

```bash
mvn install          # instala no ~/.m2 para o booklit-server (pasta acima) enxergar
```

## Fuzzing (Jazzer)

```bash
mvn test                                              # corpus como regressão
JAZZER_FUZZ=1 mvn test -Dtest=CodigoOtpFuzzTest       # fuzz de verdade (60 s por alvo)
```

No PowerShell: `$env:JAZZER_FUZZ=1; mvn test -Dtest=CodigoOtpFuzzTest`. Entradas que
quebram uma asserção são gravadas em `src/test/resources/.../CodigoOtpFuzzTestInputs/` e
viram regressão nos próximos builds: commite essa pasta.
