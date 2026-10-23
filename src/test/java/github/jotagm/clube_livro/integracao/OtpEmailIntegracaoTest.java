package github.jotagm.clube_livro.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OTP por e-mail ponta a ponta pela API: emissão, entrega pelo SES (mockado aqui; contra o
 * Floci é o mesmo cliente com outro endpoint), verificação, uso único, limite de reenvio e
 * resposta idêntica para e-mail sem conta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:otp;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1hcGVuYXMtcGFyYS1vLWNvbnRleHRvLWRlLXRlc3Rlcw==",
        "google.books.api-key=chave-de-teste",
        "booklit.auth.email.habilitado=true",
        "booklit.auth.otp.chave-hmac=Y2hhdmUtZGUtdGVzdGUtZG8taG1hYy1jb20tMzItYnl0ZXMhIQ=="
})
class OtpEmailIntegracaoTest {

    private static final Pattern CODIGO = Pattern.compile("\\b(\\d{6})\\b");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @MockitoBean private RestClient restClient;
    @MockitoBean private SesV2Client ses;

    private String email;

    @BeforeEach
    void preparar() throws Exception {
        when(ses.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("msg-teste").build());
        email = "otp-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.dev";
        mockMvc.perform(post("/usuarios").contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa\",\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isCreated());
        clearInvocations(ses);
    }

    @Test
    void emiteEntregaEVerificaUmaVezSo() throws Exception {
        String desafioId = emitir(email, "LOGIN");

        SendEmailRequest enviado = ultimoEnvio();
        assertEquals(email, enviado.destination().toAddresses().getFirst());
        assertFalse(CODIGO.matcher(enviado.content().simple().subject().data()).find(), "código não vai no assunto");
        String codigo = codigoDe(enviado);

        mockMvc.perform(post("/auth/otp/email/verificacao").contentType(APPLICATION_JSON)
                        .content(verificacao(desafioId, "LOGIN", codigo.substring(0, 3) + " " + codigo.substring(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(post("/auth/otp/email/verificacao").contentType(APPLICATION_JSON)
                        .content(verificacao(desafioId, "LOGIN", codigo)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void codigoErradoInformaTentativasRestantes() throws Exception {
        String desafioId = emitir(email, "LOGIN");
        String errado = codigoDe(ultimoEnvio()).equals("000000") ? "111111" : "000000";

        mockMvc.perform(post("/auth/otp/email/verificacao").contentType(APPLICATION_JSON)
                        .content(verificacao(desafioId, "LOGIN", errado)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Código incorreto. Tentativas restantes: 4."));
    }

    @Test
    void emailSemContaRecebeAMesmaRespostaMasNadaEEnviado() throws Exception {
        emitir("ninguem-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.dev", "LOGIN");

        verify(ses, never()).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void verificacaoDeEmailNaoExigeConta() throws Exception {
        emitir("novo-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.dev", "VERIFICAR_EMAIL");

        verify(ses, times(1)).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void reenvioImediatoDa429ComRetryAfter() throws Exception {
        emitir(email, "LOGIN");

        mockMvc.perform(post("/auth/otp/email").contentType(APPLICATION_JSON)
                        .content(pedido(email, "LOGIN")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void finalidadeInvalidaDa400() throws Exception {
        mockMvc.perform(post("/auth/otp/email").contentType(APPLICATION_JSON)
                        .content(pedido(email, "QUALQUER")))
                .andExpect(status().isBadRequest());
    }

    private String emitir(String para, String finalidade) throws Exception {
        String resposta = mockMvc.perform(post("/auth/otp/email").contentType(APPLICATION_JSON)
                        .content(pedido(para, finalidade)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.desafioId").exists())
                .andExpect(jsonPath("$.expiraEm").exists())
                .andReturn().getResponse().getContentAsString();
        JsonNode corpo = json.readTree(resposta);
        assertEquals(2, corpo.size(), "a resposta não pode variar entre quem tem e quem não tem conta");
        return corpo.get("desafioId").asText();
    }

    private SendEmailRequest ultimoEnvio() {
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(ses, times(1)).sendEmail(captor.capture());
        return captor.getValue();
    }

    private static String codigoDe(SendEmailRequest enviado) {
        Matcher m = CODIGO.matcher(enviado.content().simple().body().text().data());
        assertTrue(m.find(), "corpo sem código de 6 dígitos");
        return m.group(1);
    }

    private static String pedido(String email, String finalidade) {
        return "{\"email\":\"" + email + "\",\"finalidade\":\"" + finalidade + "\"}";
    }

    private static String verificacao(String desafioId, String finalidade, String codigo) {
        return "{\"desafioId\":\"" + desafioId + "\",\"finalidade\":\"" + finalidade + "\",\"codigo\":\"" + codigo + "\"}";
    }
}
