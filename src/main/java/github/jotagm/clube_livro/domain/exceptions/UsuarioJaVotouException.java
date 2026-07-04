package github.jotagm.clube_livro.domain.exceptions;

public class UsuarioJaVotouException extends RuntimeException {

    public UsuarioJaVotouException() {
        super("Usuário já votou nesta votação");
    }
}
