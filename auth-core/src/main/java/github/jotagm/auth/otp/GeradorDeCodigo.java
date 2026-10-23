package github.jotagm.auth.otp;

import java.security.SecureRandom;

/** Gera o código de {@value CodigoOtp#TAMANHO} dígitos. Interface para os testes fixarem o valor. */
@FunctionalInterface
public interface GeradorDeCodigo {

    String gerar();

    /** Uniforme em 000000..999999 via {@link SecureRandom#nextInt(int)}, que não tem viés de módulo. */
    static GeradorDeCodigo seguro() {
        SecureRandom random = new SecureRandom();
        int limite = (int) Math.pow(10, CodigoOtp.TAMANHO);
        return () -> String.format("%0" + CodigoOtp.TAMANHO + "d", random.nextInt(limite));
    }
}
