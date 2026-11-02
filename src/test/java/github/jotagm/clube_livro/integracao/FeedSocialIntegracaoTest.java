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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feed como conversa (respostas, quem reagiu), sugestão de livro como item, "primeira do clube"
 * e o ponto de novidade, de ponta a ponta pela API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:feedsocial;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1hcGVuYXMtcGFyYS1vLWNvbnRleHRvLWRlLXRlc3Rlcw==",
        "google.books.api-key=chave-de-teste",
        "booklit.front-url=https://booklit.bio"
})
class FeedSocialIntegracaoTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @MockitoBean private RestClient restClient;

    private String tokenLider;
    private String tokenAna;
    private int idLider;
    private int idAna;
    private String clubeId;
    private String leituraId;

    @BeforeEach
    void preparar() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        idLider = criarUsuario("Mariana Souza", "lider-" + sufixo + "@teste.dev");
        idAna = criarUsuario("Ana Lima", "ana-" + sufixo + "@teste.dev");
        tokenLider = login("lider-" + sufixo + "@teste.dev");
        tokenAna = login("ana-" + sufixo + "@teste.dev");

        clubeId = json.readTree(enviar(post("/clubes"), tokenLider,
                "{\"nome\":\"Leitoras " + sufixo + "\",\"descricao\":\"d\",\"privado\":true}", 201)).get("id").asText();
        String codigo = json.readTree(mockMvc.perform(get("/clubes/" + clubeId + "/convite-link")
                .header("Authorization", bearer(tokenLider))).andReturn().getResponse().getContentAsString())
                .get("codigo").asText();
        mockMvc.perform(post("/convites/link/" + codigo + "/entrar").header("Authorization", bearer(tokenAna)))
                .andExpect(status().isOk());

        leituraId = json.readTree(enviar(post("/leituras"), tokenLider, """
                {"clubeId":"%s","livroGoogleId":"g1","livroTitulo":"A Hora da Estrela","tipoMeta":"PAGINA",
                 "valorMeta":88,"dataInicio":"%s","dataFim":"%s"}
                """.formatted(clubeId, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(20)), 201))
                .get("id").asText();
    }

    @Test
    void comentarioViraConversaComRespostasEQuemReagiu() throws Exception {
        String raiz = json.readTree(enviar(post("/leituras/" + leituraId + "/comentarios"), tokenLider,
                "{\"conteudo\":\"A cena da faca muda tudo. Alguém mais releu?\"}", 201)).get("id").asText();
        enviar(post("/leituras/" + leituraId + "/comentarios"), tokenAna,
                "{\"comentarioPaiId\":\"" + raiz + "\",\"conteudo\":\"reli duas vezes\"}", 201);
        enviar(put("/reacoes"), tokenAna,
                "{\"clubeId\":\"" + clubeId + "\",\"alvoId\":\"comentario:" + raiz + "\",\"tipo\":\"CURTIR\"}", 200);
        enviar(put("/reacoes"), tokenLider,
                "{\"clubeId\":\"" + clubeId + "\",\"alvoId\":\"comentario:" + raiz + "\",\"tipo\":\"FOGO\"}", 200);

        List<JsonNode> comentarios = itens(feed(tokenLider), "COMENTARIO");

        // A resposta não vira um segundo item: entra na conversa do comentário.
        assertThat(comentarios).hasSize(1);
        JsonNode conversa = comentarios.get(0);
        assertThat(conversa.get("id").asText()).isEqualTo("comentario:" + raiz);
        assertThat(conversa.get("respostas").asInt()).isEqualTo(1);
        assertThat(conversa.get("ultimaResposta").get("usuarioNome").asText()).isEqualTo("Ana Lima");
        assertThat(conversa.get("ultimaResposta").get("texto").asText()).isEqualTo("reli duas vezes");
        // Quem reagiu, você por último.
        assertThat(conversa.get("quemReagiu")).extracting(p -> p.get("usuarioId").asInt())
                .containsExactly(idAna, idLider);
    }

    @Test
    void sugestaoDeLivroViraItemComLinkParaAVotacao() throws Exception {
        String votacaoId = json.readTree(enviar(post("/votacoes"), tokenLider, """
                {"clubeId":"%s","dataAbertura":"%s","dataEncerramento":"%s"}
                """.formatted(clubeId, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(2)), 201))
                .get("id").asText();
        enviar(post("/opcoes-voto"), tokenAna, """
                {"votacaoId":"%s","livroGoogleId":"g2","livroTitulo":"Ensaio sobre a cegueira","livroAutor":"José Saramago"}
                """.formatted(votacaoId), 201);

        List<JsonNode> sugestoes = itens(feed(tokenLider), "OPCAO_SUGERIDA");

        assertThat(sugestoes).hasSize(1);
        JsonNode s = sugestoes.get(0);
        assertThat(s.get("usuarioId").asInt()).isEqualTo(idAna);
        assertThat(s.get("livroTitulo").asText()).isEqualTo("Ensaio sobre a cegueira");
        assertThat(s.get("votacaoId").asText()).isEqualTo(votacaoId);
        assertThat(s.get("votacaoAberta").asBoolean()).isTrue();
        assertThat(s.get("membrosQueremLer").asLong()).isZero();
    }

    @Test
    void primeiraATerminarGanhaODestaque() throws Exception {
        enviar(put("/registros/leitura/" + leituraId), tokenAna, "{\"valorAtual\":88}", 200);
        enviar(put("/registros/leitura/" + leituraId), tokenLider, "{\"valorAtual\":88}", 200);

        List<JsonNode> concluiu = itens(feed(tokenLider), "CONCLUIU");

        assertThat(concluiu).hasSize(2);
        for (JsonNode item : concluiu) {
            assertThat(item.get("primeiraDoClube").asBoolean()).isEqualTo(item.get("usuarioId").asInt() == idAna);
        }
    }

    @Test
    void pontoDeNovidadeSomeAoAbrirOClubeEVoltaComAtividadeDosOutros() throws Exception {
        // A Ana entrou e a leitura começou depois da entrada da líder: há novidade.
        assertThat(novidades(tokenLider)).isEqualTo(clubeId);

        mockMvc.perform(put("/clubes/" + clubeId + "/visto").header("Authorization", bearer(tokenLider)))
                .andExpect(status().isNoContent());
        assertThat(novidades(tokenLider)).isNull();

        // O que a própria líder faz não é novidade para ela...
        enviar(post("/leituras/" + leituraId + "/comentarios"), tokenLider, "{\"conteudo\":\"oi\"}", 201);
        assertThat(novidades(tokenLider)).isNull();

        // ...mas o comentário da Ana é.
        enviar(post("/leituras/" + leituraId + "/comentarios"), tokenAna, "{\"conteudo\":\"olá\"}", 201);
        assertThat(novidades(tokenLider)).isEqualTo(clubeId);
    }

    @Test
    void naoMembroNaoMarcaClubeComoVisto() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        criarUsuario("Fora", "fora-" + sufixo + "@teste.dev");
        String tokenFora = login("fora-" + sufixo + "@teste.dev");

        mockMvc.perform(put("/clubes/" + clubeId + "/visto").header("Authorization", bearer(tokenFora)))
                .andExpect(status().isForbidden());
    }

    /** Id do único clube com novidade, ou null se não houver nenhum. */
    private String novidades(String token) throws Exception {
        JsonNode lista = json.readTree(mockMvc.perform(get("/feed/novidades").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        if (lista.isEmpty()) return null;
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).get("quantidade").asInt()).isPositive();
        return lista.get(0).get("clubeId").asText();
    }

    private JsonNode feed(String token) throws Exception {
        return json.readTree(mockMvc.perform(get("/feed").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private static List<JsonNode> itens(JsonNode feed, String tipo) {
        List<JsonNode> doTipo = new ArrayList<>();
        feed.forEach(i -> {
            if (tipo.equals(i.get("tipo").asText())) doTipo.add(i);
        });
        return doTipo;
    }

    private String enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder pedido,
                          String token, String corpo, int esperado) throws Exception {
        return mockMvc.perform(pedido.header("Authorization", bearer(token)).contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().is(esperado))
                .andReturn().getResponse().getContentAsString();
    }

    private int criarUsuario(String nome, String email) throws Exception {
        String resp = mockMvc.perform(post("/usuarios").contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
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
