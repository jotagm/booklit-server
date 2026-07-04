package github.jotagm.clube_livro.domain.exceptions;

public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException() {
        super("Acesso negado: apenas o líder do clube pode realizar esta ação");
    }
}
