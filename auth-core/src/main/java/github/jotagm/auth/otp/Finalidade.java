package github.jotagm.auth.otp;

/**
 * Para que o código foi emitido. Entra no hash do código, então um código emitido para
 * {@link #VERIFICAR_EMAIL} nunca serve para {@link #LOGIN}, mesmo com o mesmo desafio.
 */
public enum Finalidade {
    /** Step-up no login quando o motor de risco pede uma segunda prova. */
    LOGIN,
    /** Prova de posse do e-mail no cadastro ou na troca de e-mail. */
    VERIFICAR_EMAIL,
    /** Destrava a redefinição do PIN depois de bloqueio por tentativas. */
    REDEFINIR_PIN;

    /** Finalidades em que só faz sentido entregar o código se já existir conta para o e-mail. */
    public boolean exigeContaExistente() {
        return this != VERIFICAR_EMAIL;
    }
}
