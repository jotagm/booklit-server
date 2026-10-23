package github.jotagm.auth.otp;

import java.util.Optional;

/**
 * Normalização do código digitado pelo usuário.
 *
 * <p>Aceita só os dígitos ASCII {@code 0-9}. {@link Character#isDigit(char)} seria um erro
 * aqui: ele aceita dígitos de outros alfabetos ({@code ٣}, {@code ３}), e um parse que os
 * converte abre espaço para dois textos diferentes valerem o mesmo código. Espaços e hífen
 * são removidos porque é assim que as pessoas colam código de e-mail ("482 913", "482-913").
 */
public final class CodigoOtp {

    public static final int TAMANHO = 6;

    /** Teto antes de olhar caractere por caractere: entrada gigante não custa CPU. */
    private static final int TAMANHO_MAXIMO_BRUTO = 32;

    private CodigoOtp() {
    }

    public static Optional<String> normalizar(String digitado) {
        if (digitado == null || digitado.length() > TAMANHO_MAXIMO_BRUTO) {
            return Optional.empty();
        }
        StringBuilder digitos = new StringBuilder(TAMANHO);
        for (int i = 0; i < digitado.length(); i++) {
            char c = digitado.charAt(i);
            if (c >= '0' && c <= '9') {
                if (digitos.length() == TAMANHO) {
                    return Optional.empty();
                }
                digitos.append(c);
            } else if (c != ' ' && c != '-' && c != '\t') {
                return Optional.empty();
            }
        }
        return digitos.length() == TAMANHO ? Optional.of(digitos.toString()) : Optional.empty();
    }
}
