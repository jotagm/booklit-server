package github.jotagm.clube_livro.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo ponta a ponta das regras de acesso: quem não é membro não lê dados do clube, ninguém
 * mexe na conta de outro, o link de convite funciona sem conta para a prévia e coloca a
 * pessoa no clube, e a estante pessoal é só do dono.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:autorizacao;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1hcGVuYXMtcGFyYS1vLWNvbnRleHRvLWRlLXRlc3Rlcw==",
        "google.books.api-key=chave-de-teste",
        "booklit.front-url=https://booklit.bio"
})
class AutorizacaoEConviteIntegracaoTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @MockitoBean private RestClient restClient;

    private String tokenLider;
    private String tokenOutro;
    private int idLider;
    private int idOutro;
    private String clubeId;

    @BeforeEach
    void preparar() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        idLider = criarUsuario("lider-" + sufixo + "@teste.dev");
        idOutro = criarUsuario("outro-" + sufixo + "@teste.dev");
        tokenLider = login("lider-" + sufixo + "@teste.dev");
        tokenOutro = login("outro-" + sufixo + "@teste.dev");

        String clube = mockMvc.perform(post("/clubes").header("Authorization", bearer(tokenLider))
                        .contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Clube " + sufixo + "\",\"descricao\":\"d\",\"privado\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        clubeId = json.readTree(clube).get("id").asText();
    }

    @Test
    void naoMembroNaoLeClubePrivadoNemSeusDados() throws Exception {
        mockMvc.perform(get("/clubes/" + clubeId).header("Authorization", bearer(tokenOutro)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/membros/clube/" + clubeId).header("Authorization", bearer(tokenOutro)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/leituras/clube/" + clubeId).header("Authorization", bearer(tokenOutro)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/votacoes/clube/" + clubeId).header("Authorization", bearer(tokenOutro)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ninguemAtualizaOuConsultaAContaDeOutro() throws Exception {
        mockMvc.perform(put("/usuarios/" + idLider).header("Authorization", bearer(tokenOutro))
                        .contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Invasor\",\"email\":\"invasor@teste.dev\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios/" + idLider).header("Authorization", bearer(tokenOutro)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios/me").header("Authorization", bearer(tokenOutro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idOutro));
    }

    @Test
    void naoLiderNaoCriaConviteNemVeLink() throws Exception {
        mockMvc.perform(post("/convites").header("Authorization", bearer(tokenOutro))
                        .contentType(APPLICATION_JSON)
                        .content("{\"clubeId\":\"" + clubeId + "\",\"emailDestinatario\":\"x@teste.dev\",\"expiraEm\":\""
                                + LocalDateTime.now().plusDays(7) + "\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/clubes/" + clubeId + "/convite-link").header("Authorization", bearer(tokenOutro)))
                .andExpect(status().isForbidden());
    }

    @Test
    void linkDeConviteMostraPreviaSemContaEColocaNoClube() throws Exception {
        String link = mockMvc.perform(get("/clubes/" + clubeId + "/convite-link").header("Authorization", bearer(tokenLider)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode linkJson = json.readTree(link);
        String codigo = linkJson.get("codigo").asText();
        org.assertj.core.api.Assertions.assertThat(linkJson.get("url").asText())
                .isEqualTo("https://booklit.bio/convite/" + codigo);

        // Prévia é pública e não vaza e-mails.
        mockMvc.perform(get("/convites/link/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembros").value(1))
                .andExpect(jsonPath("$.nomeLider").exists());

        // Entrar exige conta; duas vezes não duplica.
        mockMvc.perform(post("/convites/link/" + codigo + "/entrar"))
                .andExpect(status().isUnauthorized());
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/convites/link/" + codigo + "/entrar").header("Authorization", bearer(tokenOutro)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(clubeId));
        }
        mockMvc.perform(get("/membros/clube/" + clubeId).header("Authorization", bearer(tokenOutro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Regenerar invalida o link antigo.
        mockMvc.perform(post("/clubes/" + clubeId + "/convite-link/regenerar").header("Authorization", bearer(tokenLider)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/convites/link/" + codigo))
                .andExpect(status().isNotFound());
    }

    @Test
    void membroNaoPodeExpulsarOutroELiderNaoPodeSair() throws Exception {
        String codigo = json.readTree(mockMvc.perform(get("/clubes/" + clubeId + "/convite-link")
                        .header("Authorization", bearer(tokenLider)))
                .andReturn().getResponse().getContentAsString()).get("codigo").asText();
        mockMvc.perform(post("/convites/link/" + codigo + "/entrar").header("Authorization", bearer(tokenOutro)))
                .andExpect(status().isOk());

        JsonNode membros = json.readTree(mockMvc.perform(get("/membros/clube/" + clubeId)
                .header("Authorization", bearer(tokenLider))).andReturn().getResponse().getContentAsString());
        String vinculoLider = null;
        for (JsonNode m : membros) {
            if (m.get("usuarioId").asInt() == idLider) vinculoLider = m.get("id").asText();
        }

        mockMvc.perform(delete("/membros/" + vinculoLider).header("Authorization", bearer(tokenOutro)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/membros/" + vinculoLider).header("Authorization", bearer(tokenLider)))
                .andExpect(status().isForbidden());
    }

    @Test
    void estanteEPrivadaDoDono() throws Exception {
        String item = mockMvc.perform(post("/estante").header("Authorization", bearer(tokenOutro))
                        .contentType(APPLICATION_JSON)
                        .content("{\"livroGoogleId\":\"g-1\",\"livroTitulo\":\"Duna\",\"totalPaginas\":400}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("QUERO_LER"))
                .andReturn().getResponse().getContentAsString();
        String itemId = json.readTree(item).get("id").asText();

        mockMvc.perform(get("/estante/" + itemId).header("Authorization", bearer(tokenLider)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/estante/" + itemId + "/progresso").header("Authorization", bearer(tokenOutro))
                        .contentType(APPLICATION_JSON).content("{\"paginaAtual\":400}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIDO"))
                .andExpect(jsonPath("$.percentual").value(100));

        mockMvc.perform(put("/estante/meta/" + LocalDateTime.now().getYear()).header("Authorization", bearer(tokenOutro))
                        .contentType(APPLICATION_JSON).content("{\"quantidadeLivros\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metaLivros").value(12))
                .andExpect(jsonPath("$.lidosNoAno").value(1));

        mockMvc.perform(post("/estante").header("Authorization", bearer(tokenOutro))
                        .contentType(APPLICATION_JSON)
                        .content("{\"livroGoogleId\":\"g-1\",\"livroTitulo\":\"Duna\"}"))
                .andExpect(status().isConflict());
    }

    private int criarUsuario(String email) throws Exception {
        String resp = mockMvc.perform(post("/usuarios").contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa\",\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("id").asInt();
    }

    private String login(String email) throws Exception {
        String resp = mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
