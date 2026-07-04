package github.jotagm.clube_livro.domain.exceptions;

public class LivroExternoIndisponivelException extends RuntimeException {

    public LivroExternoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
