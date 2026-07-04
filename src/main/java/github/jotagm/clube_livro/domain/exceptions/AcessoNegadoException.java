package github.jotagm.clube_livro.domain.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException() {
        super("Acesso negado: apenas o líder do clube pode realizar esta ação");
    }
}
