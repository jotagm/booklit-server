package github.jotagm.auth.otp;

import java.time.Duration;
import java.util.Objects;

/**
 * Os números da política de OTP, num lugar só.
 *
 * <p>A conta que justifica os padrões: com 5 tentativas por desafio e 5 desafios por hora,
 * quem chuta acerta com probabilidade de 25 em 10^6 por hora para um e-mail. O intervalo de
 * reenvio também impede usar o endpoint para encher a caixa de alguém.
 */
public record PoliticaOtp(
        Duration validade,
        int maxTentativas,
        Duration intervaloEntreEmissoes,
        int maxEmissoesPorJanela,
        Duration janelaDeEmissoes) {

    public PoliticaOtp {
        Objects.requireNonNull(validade, "validade");
        Objects.requireNonNull(intervaloEntreEmissoes, "intervaloEntreEmissoes");
        Objects.requireNonNull(janelaDeEmissoes, "janelaDeEmissoes");
        if (validade.isNegative() || validade.isZero()) {
            throw new IllegalArgumentException("validade precisa ser positiva");
        }
        if (maxTentativas < 1 || maxEmissoesPorJanela < 1) {
            throw new IllegalArgumentException("limites precisam ser pelo menos 1");
        }
    }

    public static PoliticaOtp padrao() {
        return new PoliticaOtp(Duration.ofMinutes(10), 5, Duration.ofSeconds(60), 5, Duration.ofHours(1));
    }
}
