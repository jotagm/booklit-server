package github.jotagm.clube_livro.domain.exceptions;

public class VotacaoSemVotosException extends RuntimeException {

    public VotacaoSemVotosException() {
        super("Não é possível encerrar uma votação sem nenhum voto registrado");
    }
}
