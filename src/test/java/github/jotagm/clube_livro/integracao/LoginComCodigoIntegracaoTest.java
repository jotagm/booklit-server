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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login em duas etapas com {@code booklit.auth.login.exigir-codigo=true}: a senha sozinha
 * não dá token, o código por e-mail sozinho também não, e o primeiro login confirma o e-mail.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logincodigo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1hcGVuYXMtcGFyYS1vLWNvbnRleHRvLWRlLXRlc3Rlcw==",
        "google.books.api-key=chave-de-teste",
        "booklit.auth.email.habilitado=true",
        "booklit.auth.login.exigir-codigo=true",
        "booklit.auth.otp.chave-hmac=Y2hhdmUtZGUtdGVzdGUtZG8taG1hYy1jb20tMzItYnl0ZXMhIQ=="
})
class LoginComCodigoIntegracaoTest {

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
        email = "login-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.dev";
        mockMvc.perform(post("/usuarios").contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa\",\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isCreated());
        clearInvocations(ses);
    }

    @Test
    void senhaCertaPedeCodigoECodigoCertoDaTokenEVerificaOEmail() throws Exception {
        JsonNode primeira = json.readTree(mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content(credenciais("senha12345")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.etapa").value("CODIGO"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn().getResponse().getContentAsString());
        String desafioId = primeira.get("desafioId").asText();

        String token = json.readTree(mockMvc.perform(post("/auth/login/codigo").contentType(APPLICATION_JSON)
                        .content(codigo(desafioId, codigoEnviado())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("CONCLUIDO"))
                .andReturn().getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.emailVerificado").value(true));

        mockMvc.perform(post("/auth/login/codigo").contentType(APPLICATION_JSON)
                        .content(codigo(desafioId, "000000")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void senhaErradaNaoEnviaCodigo() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(credenciais("errada123")))
                .andExpect(status().isUnauthorized());

        verify(ses, never()).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void codigoErradoNaoDaToken() throws Exception {
        String desafioId = json.readTree(mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content(credenciais("senha12345")))
                .andReturn().getResponse().getContentAsString()).get("desafioId").asText();
        String errado = codigoEnviado().equals("000000") ? "111111" : "000000";

        mockMvc.perform(post("/auth/login/codigo").contentType(APPLICATION_JSON).content(codigo(desafioId, errado)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Código incorreto. Tentativas restantes: 4."));
    }

    /** Garante que o 401 não desfaz a tentativa gasta: depois de 5 erros, nem o código certo entra. */
    @Test
    void cincoErrosQueimamODesafio() throws Exception {
        String desafioId = json.readTree(mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content(credenciais("senha12345")))
                .andReturn().getResponse().getContentAsString()).get("desafioId").asText();
        String certo = codigoEnviado();
        String errado = certo.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login/codigo").contentType(APPLICATION_JSON).content(codigo(desafioId, errado)))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/auth/login/codigo").contentType(APPLICATION_JSON).content(codigo(desafioId, certo)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Código expirado ou já utilizado. Entre de novo para receber outro código."));
    }

    @Test
    void repetirOLoginNaHoraDa429() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(credenciais("senha12345")))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(credenciais("senha12345")))
                .andExpect(status().isTooManyRequests());
    }

    private String codigoEnviado() {
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(ses, times(1)).sendEmail(captor.capture());
        Matcher m = CODIGO.matcher(captor.getValue().content().simple().body().text().data());
        assertTrue(m.find(), "corpo sem código");
        return m.group(1);
    }

    private String credenciais(String senha) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    }

    private static String codigo(String desafioId, String codigo) {
        return "{\"desafioId\":\"" + desafioId + "\",\"codigo\":\"" + codigo + "\"}";
    }
}
