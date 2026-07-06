package github.jotagm.clube_livro.domain.exceptions;

public class ComentarioAninhadoException extends RuntimeException {

    public ComentarioAninhadoException() {
        super("Não é possível responder a uma resposta — o limite é de 1 nível de profundidade");
    }
}
