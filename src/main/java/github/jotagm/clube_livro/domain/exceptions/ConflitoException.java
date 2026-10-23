package github.jotagm.clube_livro.domain.exceptions;

/** A operação conflita com o estado atual (convite já respondido, duplicado...). Vira 409 na API. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
