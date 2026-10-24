package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Resposta do login. Com o código por e-mail ligado, o {@code POST /auth/login} devolve
 * {@code etapa=CODIGO} e o desafio (sem token); o token vem em {@code POST /auth/login/codigo}.
 * Desligado, devolve {@code etapa=CONCLUIDO} e o token direto, como sempre foi.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(Etapa etapa, String token, UUID desafioId, Instant expiraEm) {

    public enum Etapa { CONCLUIDO, CODIGO }

    public static LoginResponse comToken(String token) {
        return new LoginResponse(Etapa.CONCLUIDO, token, null, null);
    }

    public static LoginResponse comDesafio(UUID desafioId, Instant expiraEm) {
        return new LoginResponse(Etapa.CODIGO, null, desafioId, expiraEm);
    }
}
