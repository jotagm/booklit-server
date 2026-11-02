package github.jotagm.clube_livro.domain.exceptions;

/** Ação repetida demais num intervalo curto (convites por hora, por exemplo). Vira 429 na API. */
public class LimiteExcedidoException extends RuntimeException {

    public LimiteExcedidoException(String mensagem) {
        super(mensagem);
    }
}
