package github.jotagm.auth.otp;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodigoOtpTest {

    @ParameterizedTest
    @ValueSource(strings = {"482913", " 482913 ", "482 913", "482-913", "\t482913"})
    void aceitaOsFormatosQueAsPessoasColam(String digitado) {
        assertEquals("482913", CodigoOtp.normalizar(digitado).orElseThrow());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "", "48291", "4829134", "48291a",
            "٤٨٢٩١٣",        // dígitos arábico-índicos
            "４８２９１３",     // dígitos de largura total
            "482913\n", "+482913", "4829 13 9"
    })
    void recusaOQueNaoSaoSeisDigitosAscii(String digitado) {
        assertTrue(CodigoOtp.normalizar(digitado).isEmpty());
    }

    @org.junit.jupiter.api.Test
    void recusaEntradaGiganteSemVarrerTudo() {
        assertTrue(CodigoOtp.normalizar(" ".repeat(10_000) + "482913").isEmpty());
    }
}
