package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Referência do desafio para a verificação. Igual exista conta ou não para o e-mail. */
public record DesafioOtpResponse(UUID desafioId, Instant expiraEm) {
}
