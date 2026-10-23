package github.jotagm.auth.otp;

import java.time.Duration;

/** Pedido de código antes do intervalo mínimo ou acima do teto da janela. Vira 429 na API. */
public class LimiteDeEmissaoException extends RuntimeException {

    private final Duration tenteNovamenteEm;

    public LimiteDeEmissaoException(Duration tenteNovamenteEm) {
        super("Muitos códigos solicitados. Tente novamente em " + Math.max(1, tenteNovamenteEm.toSeconds()) + "s.");
        this.tenteNovamenteEm = tenteNovamenteEm;
    }

    public Duration tenteNovamenteEm() {
        return tenteNovamenteEm;
    }
}
