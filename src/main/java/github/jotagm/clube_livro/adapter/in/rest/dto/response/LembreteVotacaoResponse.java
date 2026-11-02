package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import java.time.LocalDateTime;

/** Resultado do "lembrar quem não votou": quantos membros foram lembrados e quando. */
public record LembreteVotacaoResponse(int notificados, LocalDateTime lembradoEm) {
}
