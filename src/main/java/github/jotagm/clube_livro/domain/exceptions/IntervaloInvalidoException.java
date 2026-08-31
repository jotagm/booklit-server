package github.jotagm.clube_livro.domain.exceptions;

public class IntervaloInvalidoException extends RuntimeException {

    public IntervaloInvalidoException() {
        super("A data de fim precisa ser posterior à data de início");
    }
}
