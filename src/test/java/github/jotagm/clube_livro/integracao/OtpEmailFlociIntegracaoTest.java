package github.jotagm.clube_livro.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mesmo fluxo do {@link OtpEmailIntegracaoTest}, mas com o SES de verdade do Floci: o código
 * é lido da caixa local em {@code GET /_aws/ses}.
 *
 * <p>Só roda com o Floci no ar e a variável apontando para ele, e com o remetente
 * verificado lá ({@code aws sesv2 create-email-identity --email-identity no-reply@booklit.bio}):
 * <pre>FLOCI_ENDPOINT=http://localhost:4566 AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
 *     mvn test -Dtest=OtpEmailFlociIntegracaoTest</pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "FLOCI_ENDPOINT", matches = "https?://.+")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:otpfloci;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1hcGVuYXMtcGFyYS1vLWNvbnRleHRvLWRlLXRlc3Rlcw==",
        "google.books.api-key=chave-de-teste",
        "booklit.auth.email.habilitado=true",
        "booklit.auth.email.remetente=no-reply@booklit.bio"
})
class OtpEmailFlociIntegracaoTest {

    private static final String FLOCI = System.getenv("FLOCI_ENDPOINT");
    private static final Pattern CODIGO = Pattern.compile("\\b(\\d{6})\\b");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private RestClient restClient;

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void floci(DynamicPropertyRegistry registro) {
        registro.add("booklit.aws.endpoint", () -> FLOCI);
    }

    @Test
    void codigoChegaNaCaixaDoFlociEVerifica() throws Exception {
        String email = "otp-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.local";

        String resposta = mockMvc.perform(post("/auth/otp/email").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"finalidade\":\"VERIFICAR_EMAIL\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String desafioId = json.readTree(resposta).get("desafioId").asText();

        String codigo = esperarCodigo(email);

        mockMvc.perform(post("/auth/otp/email/verificacao").contentType(APPLICATION_JSON)
                        .content("{\"desafioId\":\"" + desafioId + "\",\"finalidade\":\"VERIFICAR_EMAIL\",\"codigo\":\""
                                + codigo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    /** O envio é depois do commit; na prática já chegou, mas dá uma folga curta. */
    private String esperarCodigo(String destinatario) throws Exception {
        Instant limite = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(limite)) {
            HttpResponse<String> caixa = http.send(
                    HttpRequest.newBuilder(URI.create(FLOCI + "/_aws/ses")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            for (JsonNode msg : json.readTree(caixa.body()).path("messages")) {
                if (msg.path("Destination").path("ToAddresses").toString().contains(destinatario)) {
                    Matcher m = CODIGO.matcher(msg.path("Body").path("text_part").asText());
                    assertTrue(m.find(), "e-mail sem código");
                    return m.group(1);
                }
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Nenhum e-mail para " + destinatario + " em " + FLOCI + "/_aws/ses");
    }
}
