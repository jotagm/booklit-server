package github.jotagm.clube_livro.domain.exceptions;

public class VotacaoJaEncerradaException extends RuntimeException {
    public VotacaoJaEncerradaException() {
        super("Votação já está encerrada");
    }
}