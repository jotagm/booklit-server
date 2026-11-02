package github.jotagm.clube_livro.domain.exceptions;

/** Recurso desligado ou sem a infraestrutura configurada (fotos sem bucket). Vira 503 na API. */
public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
