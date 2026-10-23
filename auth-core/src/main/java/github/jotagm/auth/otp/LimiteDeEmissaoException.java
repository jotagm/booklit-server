package github.jotagm.auth.otp;

import java.time.Duration;

/** Pedido de código antes do intervalo mínimo ou acima do teto da janela. Vira 429 na API. */
public class LimiteDeEmissaoException extends RuntimeException {

    private final Duration tenteNovamenteEm;

    public LimiteDeEmissaoException(Duration tenteNovamenteEm) {
        super("Muitos códigos solicitados. Tente novamente em " + arredondarParaCima(tenteNovamenteEm) + "s.");
        this.tenteNovamenteEm = tenteNovamenteEm;
    }

    public Duration tenteNovamenteEm() {
        return tenteNovamenteEm;
    }

    /**
     * Segundos inteiros, arredondados para cima (Retry-After). Truncar faria 59,99 s virar 59
     * e o cliente tentaria um instante cedo demais, levando outro 429.
     */
    public long segundosParaTentarDeNovo() {
        return arredondarParaCima(tenteNovamenteEm);
    }

    private static long arredondarParaCima(Duration duracao) {
        return Math.max(1, (duracao.toMillis() + 999) / 1000);
    }
}
