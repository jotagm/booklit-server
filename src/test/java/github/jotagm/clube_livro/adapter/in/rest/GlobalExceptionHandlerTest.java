package github.jotagm.clube_livro.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o contrato de erro da API. O handler de {@code Exception} é fácil de quebrar sem
 * perceber: ele pode roubar exceções que o Spring resolveria melhor sozinho.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:erros;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1hcGVuYXMtcGFyYS1vLWNvbnRleHRvLWRlLXRlc3Rlcw==",
        "google.books.api-key=chave-de-teste"
})
class GlobalExceptionHandlerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private RestClient restClient;

    @Test
    void corpoInvalidoRetorna400ComOsCamposQueFalharam() throws Exception {
        // Sem o handler de MethodArgumentNotValidException isto virava 403 com corpo vazio,
        // porque o dispatch para /error esbarra no SecurityConfig.
        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"x\",\"senha\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value(containsString("email")));
    }

    @Test
    void loginComUsuarioInexistenteRetorna401SemRevelarSeOEmailExiste() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"naoexiste@teste.dev\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(not(containsString("não encontrado"))));
    }

    @Test
    void rotaProtegidaSemTokenRetorna401ComCorpo() throws Exception {
        // Antes do AuthenticationEntryPoint isto era 403 de corpo vazio, e o front tratava
        // token expirado como falta de permissão em vez de deslogar.
        mockMvc.perform(get("/clubes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void rotaProtegidaComTokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/clubes").header("Authorization", "Bearer token-que-nao-vale-nada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void erroDeNegocioMantemOStatusDeDominio() throws Exception {
        // Usuário criado com sucesso é 201; o mesmo e-mail de novo estoura violação de
        // unicidade, que chega embrulhada em DataIntegrityViolationException.
        String corpo = "{\"nome\":\"Fulano\",\"email\":\"dup@teste.dev\",\"senha\":\"senha12345\"}";

        mockMvc.perform(post("/usuarios").contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/usuarios").contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.status").value(500));
    }
}
