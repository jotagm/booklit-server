package github.jotagm.clube_livro.domain.exceptions;

/**
 * Um intervalo em que o fim não vem depois do início nunca contém o instante atual — a
 * leitura ou votação seria aceita e simplesmente nunca apareceria, sem erro nenhum.
 */
public class IntervaloInvalidoException extends RuntimeException {

    public IntervaloInvalidoException() {
        super("A data de fim precisa ser posterior à data de início");
    }
}
