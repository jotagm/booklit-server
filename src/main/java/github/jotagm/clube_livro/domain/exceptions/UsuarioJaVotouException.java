package github.jotagm.clube_livro.domain.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UsuarioJaVotouException extends RuntimeException {

    public UsuarioJaVotouException() {
        super("Usuário já votou nesta votação");
    }
}