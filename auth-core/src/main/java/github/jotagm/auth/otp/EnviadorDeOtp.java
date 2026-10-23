package github.jotagm.auth.otp;

import java.time.Instant;
import java.util.UUID;

/**
 * Porta de entrega do código (e-mail hoje; SMS ou push dariam outro adapter).
 *
 * <p>É o único ponto em que o código aparece em claro. Implementações não devem logar
 * {@link Mensagem#codigo()} nem o corpo montado com ele.
 */
@FunctionalInterface
public interface EnviadorDeOtp {

    void enviar(Mensagem mensagem);

    record Mensagem(UUID desafioId, String destinatario, Finalidade finalidade, String codigo, Instant expiraEm) {

        /** Sem o código: é o que pode ir para log e para exceção. */
        @Override
        public String toString() {
            return "Mensagem[desafioId=" + desafioId + ", finalidade=" + finalidade + ", expiraEm=" + expiraEm + "]";
        }
    }
}
