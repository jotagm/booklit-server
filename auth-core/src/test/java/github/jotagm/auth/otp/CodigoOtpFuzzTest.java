package github.jotagm.auth.otp;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fuzz das invariantes do OTP.
 *
 * <p>Sem {@code JAZZER_FUZZ=1} cada método roda só com o corpus salvo, como teste de
 * regressão. Com a variável, o Jazzer muta entradas guiado por cobertura e grava em
 * {@code src/test/resources/github/jotagm/auth/otp/CodigoOtpFuzzTestInputs/} qualquer
 * entrada que quebre uma asserção: essa entrada vira regressão permanente no próximo build.
 */
class CodigoOtpFuzzTest {

    private static final byte[] CHAVE = "chave-de-teste-com-pelo-menos-32-bytes!!".getBytes(StandardCharsets.UTF_8);

    /** O que sai da normalização é sempre vazio ou exatamente 6 dígitos ASCII, e é idempotente. */
    @FuzzTest(maxDuration = "60s")
    void normalizacaoSoProduzSeisDigitosAscii(FuzzedDataProvider dados) {
        String entrada = dados.consumeRemainingAsString();

        Optional<String> normalizado = CodigoOtp.normalizar(entrada);

        normalizado.ifPresent(codigo -> {
            assertTrue(codigo.matches("[0-9]{6}"), () -> "saiu fora do formato: " + codigo);
            assertEquals(normalizado, CodigoOtp.normalizar(codigo));
        });
    }

    /**
     * Nenhuma entrada é aceita a não ser uma que normalize para o código emitido, e nunca há
     * mais verificações aceitas do que o limite de tentativas permite.
     */
    @FuzzTest(maxDuration = "60s")
    void soOCodigoEmitidoVerifica(FuzzedDataProvider dados) {
        String emitido = String.format("%06d", dados.consumeInt(0, 999_999));
        RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-10-23T20:00:00Z"));
        ServicoOtpEmail servico = new ServicoOtpEmail(new RepositorioDeDesafiosEmMemoria(), m -> { },
                () -> emitido, new HasherDeCodigo(CHAVE), PoliticaOtp.padrao(), relogio);
        DesafioEmitido desafio = servico.emitir("fuzz@teste.local", Finalidade.LOGIN);

        int aceitas = 0;
        int respondidas = 0;
        while (dados.remainingBytes() > 0 && respondidas < 8) {
            String tentativa = dados.consumeString(16);
            Verificacao v = servico.verificar(desafio.desafioId(), Finalidade.LOGIN, tentativa);
            respondidas++;
            if (v.sucesso()) {
                aceitas++;
                assertEquals(Optional.of(emitido), CodigoOtp.normalizar(tentativa),
                        () -> "aceitou entrada que não é o código: " + tentativa);
            }
        }
        assertTrue(aceitas <= 1, "um código só pode verificar uma vez");
    }
}
