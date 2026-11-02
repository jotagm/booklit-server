package github.jotagm.clube_livro.domain.exceptions;

/** Prova de identidade recusada (código OTP errado ou expirado, conta sumiu). Vira 401 na API. */
public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
