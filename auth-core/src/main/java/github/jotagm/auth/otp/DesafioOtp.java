package github.jotagm.auth.otp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Um código emitido e ainda sob controle do servidor. O código em si nunca fica aqui, só o
 * HMAC dele.
 *
 * @param tentativasRestantes quantas verificações ainda podem ser feitas; chega a zero e o
 *                            desafio morre, mesmo que o código certo apareça depois
 * @param consumidoEm         preenchido na verificação bem-sucedida; garante uso único
 */
public record DesafioOtp(
        UUID id,
        String destinatario,
        Finalidade finalidade,
        String codigoHash,
        Instant criadoEm,
        Instant expiraEm,
        int tentativasRestantes,
        Instant consumidoEm) {

    public DesafioOtp {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(destinatario, "destinatario");
        Objects.requireNonNull(finalidade, "finalidade");
        Objects.requireNonNull(codigoHash, "codigoHash");
        Objects.requireNonNull(criadoEm, "criadoEm");
        Objects.requireNonNull(expiraEm, "expiraEm");
        if (tentativasRestantes < 0) {
            throw new IllegalArgumentException("tentativasRestantes negativo");
        }
    }

    /** Ainda aceita uma tentativa: não expirou, não foi usado e não esgotou as tentativas. */
    public boolean ativoEm(Instant agora) {
        return consumidoEm == null && tentativasRestantes > 0 && agora.isBefore(expiraEm);
    }
}
