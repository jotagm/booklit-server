package github.jotagm.auth.otp;

/**
 * Resultado de {@link ServicoOtpEmail#verificar}.
 *
 * <p>{@link Status#DESAFIO_INDISPONIVEL} junta "não existe", "expirou", "já foi usado" e
 * "sem tentativas" de propósito: para quem está chutando, distinguir os casos só ajuda.
 *
 * @param destinatario o e-mail provado; preenchido só quando {@link Status#VERIFICADO}
 */
public record Verificacao(Status status, int tentativasRestantes, String destinatario) {

    public enum Status { VERIFICADO, CODIGO_INCORRETO, DESAFIO_INDISPONIVEL }

    static Verificacao verificado(String destinatario) {
        return new Verificacao(Status.VERIFICADO, 0, destinatario);
    }

    static Verificacao incorreto(int tentativasRestantes) {
        return new Verificacao(Status.CODIGO_INCORRETO, tentativasRestantes, null);
    }

    static Verificacao indisponivel() {
        return new Verificacao(Status.DESAFIO_INDISPONIVEL, 0, null);
    }

    public boolean sucesso() {
        return status == Status.VERIFICADO;
    }
}
