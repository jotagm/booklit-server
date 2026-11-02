package github.jotagm.clube_livro.domain.exceptions;

/** O recurso existiu mas já venceu (convite expirado). Vira 410 na API. */
public class RecursoExpiradoException extends RuntimeException {

    public RecursoExpiradoException(String mensagem) {
        super(mensagem);
    }
}
