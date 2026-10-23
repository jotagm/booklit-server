package github.jotagm.auth.otp;

import java.time.Instant;
import java.util.UUID;

/** O que volta para o cliente depois de pedir um código: nunca o código, só a referência. */
public record DesafioEmitido(UUID desafioId, Instant expiraEm) {
}
