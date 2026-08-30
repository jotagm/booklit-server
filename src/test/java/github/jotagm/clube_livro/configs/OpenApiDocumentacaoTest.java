package github.jotagm.clube_livro.configs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que o documento OpenAPI continua sendo gerado e permanece acessível sem autenticação.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:openapi;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1hcGVuYXMtcGFyYS1vLWNvbnRleHRvLWRlLXRlc3Rlcw==",
        "google.books.api-key=chave-de-teste"
})
class OpenApiDocumentacaoTest {

    @Autowired
    private MockMvc mockMvc;

    // O RestClient real abre um socket na criação do bean; a documentação não depende dele.
    @MockitoBean
    private RestClient restClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void documentoOpenApiEhPublicoEDescreveAApi() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode doc = objectMapper.readTree(json);

        assertThat(doc.at("/info/title").asText()).isEqualTo("Booklit API");
        assertThat(doc.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(doc.at("/components/schemas/ErroResponse").isMissingNode()).isFalse();
    }

    @Test
    void endpointsPublicosNaoExigemToken() throws Exception {
        JsonNode paths = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString()).get("paths");

        assertThat(paths.at("/~1auth~1login/post/security").isMissingNode()).isFalse();
        assertThat(paths.at("/~1auth~1login/post/security")).isEmpty();
        assertThat(paths.at("/~1usuarios/post/security")).isEmpty();
    }

    @Test
    void endpointsProtegidosDeclaramRespostasDeErroPadrao() throws Exception {
        JsonNode paths = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString()).get("paths");

        JsonNode respostas = paths.at("/~1clubes/get/responses");
        assertThat(respostas.has("200")).isTrue();
        assertThat(respostas.has("401")).isTrue();
        assertThat(respostas.has("403")).isTrue();
    }

    @Test
    void todosOsEndpointsPossuemResumo() throws Exception {
        JsonNode paths = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString()).get("paths");

        paths.fields().forEachRemaining(caminho ->
                caminho.getValue().fields().forEachRemaining(operacao ->
                        assertThat(operacao.getValue().hasNonNull("summary"))
                                .as("%s %s sem summary", operacao.getKey(), caminho.getKey())
                                .isTrue()));
    }
}
