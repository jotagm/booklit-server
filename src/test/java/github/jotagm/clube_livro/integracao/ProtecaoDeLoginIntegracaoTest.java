package github.jotagm.clube_livro.integracao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Limite de senhas erradas no {@code POST /auth/login}, de ponta a ponta com o banco. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:protecaologin;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1hcGVuYXMtcGFyYS1vLWNvbnRleHRvLWRlLXRlc3Rlcw==",
        "google.books.api-key=chave-de-teste",
        "booklit.auth.login.protecao.max-falhas-por-email-e-ip=3",
        "booklit.auth.login.protecao.max-falhas-por-ip=1000",
        "booklit.auth.login.protecao.max-falhas-por-email=1000"
})
class ProtecaoDeLoginIntegracaoTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private RestClient restClient;

    private String email;

    @BeforeEach
    void cadastrar() throws Exception {
        email = "protecao-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.dev";
        mockMvc.perform(post("/usuarios").contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa\",\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void depoisDoLimiteNemASenhaCertaEntraERespondeRetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("errada123").andExpect(status().isUnauthorized());
        }

        login("senha12345")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", org.hamcrest.Matchers.matchesPattern("\\d+")))
                .andExpect(jsonPath("$.mensagem").value("Muitas tentativas de login. Tente de novo em alguns minutos."));
    }

    /** O atacante (um IP) fica bloqueado; a dona da conta, de outro IP, entra com a senha certa. */
    @Test
    void bloqueioDeUmIpNaoTrancaAContaParaOutroIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("errada123", "6.6.6.6").andExpect(status().isUnauthorized());
        }
        login("senha12345", "6.6.6.6").andExpect(status().isTooManyRequests());

        login("senha12345", "200.1.2.3").andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void emailMaiorQueOLimiteDa400() throws Exception {
        String longo = "a".repeat(250) + "@teste.dev";
        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + longo + "\",\"senha\":\"qualquer123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginCertoAntesDoLimiteZeraAsFalhas() throws Exception {
        login("errada123").andExpect(status().isUnauthorized());
        login("errada123").andExpect(status().isUnauthorized());
        login("senha12345").andExpect(status().isOk());

        login("errada123").andExpect(status().isUnauthorized());
        login("errada123").andExpect(status().isUnauthorized());
        login("senha12345").andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void emailInexistenteTambemEhLimitadoEComAMesmaResposta() throws Exception {
        String fantasma = "ninguem-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.dev";
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                            .content("{\"email\":\"" + fantasma + "\",\"senha\":\"qualquer123\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + fantasma + "\",\"senha\":\"qualquer123\"}"))
                .andExpect(status().isTooManyRequests());
    }

    private org.springframework.test.web.servlet.ResultActions login(String senha) throws Exception {
        return login(senha, "127.0.0.1");
    }

    private org.springframework.test.web.servlet.ResultActions login(String senha, String ip) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                .with(requisicao -> { requisicao.setRemoteAddr(ip); return requisicao; })
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"));
    }
}
