package github.jotagm.clube_livro.domain.exceptions;

/**
 * Muitas senhas erradas em pouco tempo, para o e-mail ou para o IP. Vira 429 com Retry-After.
 *
 * <p>A mensagem não diz qual dos dois limites estourou nem se o e-mail existe.
 */
public class LoginBloqueadoException extends RuntimeException {

    private final long segundosParaTentarDeNovo;

    public LoginBloqueadoException(long segundosParaTentarDeNovo) {
        super("Muitas tentativas de login. Tente de novo em alguns minutos.");
        this.segundosParaTentarDeNovo = Math.max(1, segundosParaTentarDeNovo);
    }

    public long segundosParaTentarDeNovo() {
        return segundosParaTentarDeNovo;
    }
}
