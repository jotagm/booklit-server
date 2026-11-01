package github.jotagm.clube_livro.adapter.out.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import github.jotagm.clube_livro.application.port.out.EnviadorDeConvite.Mensagem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O aviso de convite contra um servidor HTTP de verdade (o do JDK), sem Spring. */
class ClientGoEnviadorDeConviteTest {

    private static final String TOKEN = "t".repeat(40);
    private static final ObjectMapper JSON = new ObjectMapper();

    private record Pedido(String caminho, Map<String, String> cabecalhos, String corpo) {
    }

    private final List<Pedido> recebidos = new CopyOnWriteArrayList<>();
    private final AtomicInteger status = new AtomicInteger(202);
    private HttpServer servidor;

    @BeforeEach
    void subir() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            Map<String, String> cabecalhos = new java.util.HashMap<>();
            troca.getRequestHeaders().forEach((k, v) -> cabecalhos.put(k.toLowerCase(), v.getFirst()));
            recebidos.add(new Pedido(troca.getRequestURI().getPath(), cabecalhos,
                    new String(troca.getRequestBody().readAllBytes())));
            troca.sendResponseHeaders(status.get(), -1);
            troca.close();
        });
        servidor.start();
    }

    @AfterEach
    void descer() {
        servidor.stop(0);
    }

    private ClientGoEnviadorDeConvite enviador() {
        return new ClientGoEnviadorDeConvite(JSON, "http://127.0.0.1:" + servidor.getAddress().getPort(),
                TOKEN, Duration.ofSeconds(2), "https://booklit.bio/");
    }

    @Test
    void mandaOTemplateDoConviteComAsVariaveisEAChaveDeIdempotencia() throws Exception {
        UUID id = UUID.randomUUID();

        enviador().enviar(new Mensagem(id, "novo@email.com", "Ana", "Leitores de Sábado"));

        assertEquals(1, recebidos.size());
        Pedido p = recebidos.getFirst();
        assertEquals("/notificacoes", p.caminho());
        assertEquals("Bearer " + TOKEN, p.cabecalhos().get("authorization"));
        assertEquals(id.toString(), p.cabecalhos().get("idempotency-key"));
        JsonNode corpo = JSON.readTree(p.corpo());
        assertEquals("convite-clube", corpo.get("template").asText());
        assertEquals("novo@email.com", corpo.get("destinatario").asText());
        assertEquals("normal", corpo.get("prioridade").asText());
        assertEquals("Ana", corpo.get("variaveis").get("convidado_por").asText());
        assertEquals("Leitores de Sábado", corpo.get("variaveis").get("clube").asText());
        assertEquals("https://booklit.bio/clubes", corpo.get("variaveis").get("link").asText());
        assertFalse(corpo.has("expira_em"));
    }

    @Test
    void falhaDoClientGoNaoPropagaParaQuemCriouOConvite() {
        status.set(500);

        assertDoesNotThrow(() -> enviador().enviar(new Mensagem(UUID.randomUUID(), "a@email.com", "Ana", "Clube")));
        assertEquals(1, recebidos.size());
    }

    @Test
    void servidorForaDoArNaoPropaga() {
        servidor.stop(0);

        assertDoesNotThrow(() -> enviador().enviar(new Mensagem(UUID.randomUUID(), "a@email.com", "Ana", "Clube")));
    }

    @Test
    void nomeComQuebraDeLinhaViraUmaLinhaSo() {
        assertEquals("Clube Bom", ClientGoEnviadorDeConvite.limpar("Clube\r\nBom"));
        assertEquals("a b", ClientGoEnviadorDeConvite.limpar("a\u0000\u2028b"));
    }

    @Test
    void nomeVazioOuNuloVira_alguem_eNomeGrandeEhCortado() {
        assertEquals("alguém", ClientGoEnviadorDeConvite.limpar("   "));
        assertEquals("alguém", ClientGoEnviadorDeConvite.limpar(null));
        String cortado = ClientGoEnviadorDeConvite.limpar("x".repeat(500));
        assertEquals(ClientGoEnviadorDeConvite.TAMANHO_MAXIMO_NOME + 1, cortado.length());
        assertTrue(cortado.endsWith("…"));
    }

    @Test
    void linkSaiDoEnderecoDoSiteSemBarraFinal() {
        assertEquals("https://booklit.bio/clubes", ClientGoEnviadorDeConvite.montarLink("https://booklit.bio///"));
        assertThrows(IllegalArgumentException.class, () -> ClientGoEnviadorDeConvite.montarLink(""));
        assertThrows(IllegalArgumentException.class, () -> ClientGoEnviadorDeConvite.montarLink("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> ClientGoEnviadorDeConvite.montarLink("booklit.bio"));
    }

    @Test
    void tokenCurtoOuTimeoutInvalidoFalhamNaSubida() {
        assertThrows(IllegalArgumentException.class, () -> new ClientGoEnviadorDeConvite(
                JSON, "http://localhost:8081", "curto", Duration.ofSeconds(2), "https://booklit.bio"));
        assertThrows(IllegalArgumentException.class, () -> new ClientGoEnviadorDeConvite(
                JSON, "http://localhost:8081", TOKEN, Duration.ZERO, "https://booklit.bio"));
    }
}
