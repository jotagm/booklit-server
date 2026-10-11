package github.jotagm.clube_livro.domain.exceptions;

/** Violação de uma regra do domínio (ex: nota fora de 1 a 5). Vira 422 na API. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
