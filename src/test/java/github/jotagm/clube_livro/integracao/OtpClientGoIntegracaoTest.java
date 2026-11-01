package github.jotagm.clube_livro.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os fluxos de OTP com {@code booklit.auth.email.provedor=client-go}: o código sai pelo
 * Client_GO (um servidor HTTP falso aqui) em vez do SES, e o resto do auth-core não nota a
 * diferença. Nenhum {@code SesV2Client} é criado neste contexto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:otpclientgo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1hcGVuYXMtcGFyYS1vLWNvbnRleHRvLWRlLXRlc3Rlcw==",
        "google.books.api-key=chave-de-teste",
        "booklit.auth.email.habilitado=true",
        "booklit.auth.email.provedor=client-go",
        "booklit.auth.email.client-go.token=tttttttttttttttttttttttttttttttttttttttt",
        "booklit.auth.email.client-go.timeout=2s",
        "booklit.auth.login.exigir-codigo=true",
        "booklit.auth.otp.chave-hmac=Y2hhdmUtZGUtdGVzdGUtZG8taG1hYy1jb20tMzItYnl0ZXMhIQ=="
})
class OtpClientGoIntegracaoTest {

    private record Pedido(Map<String, String> cabecalhos, JsonNode corpo) {
    }

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<Pedido> RECEBIDOS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger STATUS = new AtomicInteger(202);
    private static final HttpServer CLIENT_GO = subirClientGoFalso();

    @DynamicPropertySource
    static void clientGo(DynamicPropertyRegistry registro) {
        registro.add("booklit.auth.email.client-go.url", () -> "http://127.0.0.1:" + CLIENT_GO.getAddress().getPort());
    }

    @AfterAll
    static void derrubar() {
        CLIENT_GO.stop(0);
    }

    @Autowired private MockMvc mockMvc;
    @MockitoBean private RestClient restClient;

    private String email;

    @BeforeEach
    void preparar() throws Exception {
        STATUS.set(202);
        email = "cgo-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.dev";
        mockMvc.perform(post("/usuarios").contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa\",\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isCreated());
        RECEBIDOS.clear();
    }

    @Test
    void codigoSaiPeloClientGoEServeParaVerificar() throws Exception {
        JsonNode emissao = JSON.readTree(mockMvc.perform(post("/auth/otp/email").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"finalidade\":\"REDEFINIR_PIN\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString());
        String desafioId = emissao.get("desafioId").asText();

        assertEquals(1, RECEBIDOS.size());
        Pedido pedido = RECEBIDOS.getFirst();
        assertEquals(desafioId, pedido.cabecalhos().get("idempotency-key"), "a chave de idempotência é o desafio");
        assertEquals("Bearer " + "t".repeat(40), pedido.cabecalhos().get("authorization"));
        assertEquals("otp-redefinir-pin", pedido.corpo().get("template").asText());
        assertEquals(email, pedido.corpo().get("destinatario").asText());
        assertEquals("alta", pedido.corpo().get("prioridade").asText());
        String codigo = pedido.corpo().get("variaveis").get("codigo").asText();
        assertTrue(codigo.matches("\\d{6}"), "código de 6 dígitos");

        mockMvc.perform(post("/auth/otp/email/verificacao").contentType(APPLICATION_JSON)
                        .content("{\"desafioId\":\"" + desafioId + "\",\"finalidade\":\"REDEFINIR_PIN\",\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void loginEmDuasEtapasComOCodigoVindoDoClientGo() throws Exception {
        JsonNode primeira = JSON.readTree(mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.etapa").value("CODIGO"))
                .andReturn().getResponse().getContentAsString());
        String desafioId = primeira.get("desafioId").asText();

        assertEquals(1, RECEBIDOS.size());
        assertEquals("otp-login", RECEBIDOS.getFirst().corpo().get("template").asText());
        assertEquals(desafioId, RECEBIDOS.getFirst().cabecalhos().get("idempotency-key"));
        String codigo = RECEBIDOS.getFirst().corpo().get("variaveis").get("codigo").asText();

        String token = JSON.readTree(mockMvc.perform(post("/auth/login/codigo").contentType(APPLICATION_JSON)
                        .content("{\"desafioId\":\"" + desafioId + "\",\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("CONCLUIDO"))
                .andReturn().getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerificado").value(true));
    }

    @Test
    void emailSemContaNaoChamaOClientGo() throws Exception {
        mockMvc.perform(post("/auth/otp/email").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"ninguem-" + UUID.randomUUID().toString().substring(0, 8)
                                + "@teste.dev\",\"finalidade\":\"REDEFINIR_PIN\"}"))
                .andExpect(status().isAccepted());

        assertTrue(RECEBIDOS.isEmpty());
    }

    @Test
    void clientGoRecusandoNaoQuebraARequisicao() throws Exception {
        STATUS.set(503);

        mockMvc.perform(post("/auth/otp/email").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"finalidade\":\"REDEFINIR_PIN\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.desafioId").exists());

        assertEquals(1, RECEBIDOS.size(), "a tentativa foi feita, só não deu certo");
    }

    private static HttpServer subirClientGoFalso() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidor.createContext("/notificacoes", troca -> {
                Map<String, String> cabecalhos = new java.util.HashMap<>();
                troca.getRequestHeaders().forEach((k, v) -> cabecalhos.put(k.toLowerCase(), v.getFirst()));
                RECEBIDOS.add(new Pedido(cabecalhos, JSON.readTree(troca.getRequestBody().readAllBytes())));
                troca.sendResponseHeaders(STATUS.get(), -1);
                troca.close();
            });
            servidor.start();
            return servidor;
        } catch (IOException e) {
            throw new IllegalStateException("não foi possível subir o Client_GO falso", e);
        }
    }
}
