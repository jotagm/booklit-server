package github.jotagm.clube_livro.adapter.out.email;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import github.jotagm.auth.otp.EnviadorDeOtp.Mensagem;
import github.jotagm.auth.otp.Finalidade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
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

/**
 * O adaptador do Client_GO contra um servidor HTTP de verdade (o do JDK), sem Spring: o que
 * vai no fio, quando vai (só depois do commit) e, principalmente, que nada do que der errado
 * chega à requisição de quem pediu o código nem ao log.
 */
class ClientGoEnviadorDeOtpTest {

    private static final String TOKEN = "t".repeat(40);
    private static final String CODIGO = "493817";
    private static final ObjectMapper JSON = new ObjectMapper();

    private record Pedido(String metodo, String caminho, Map<String, String> cabecalhos, String corpo) {
    }

    private final List<Pedido> recebidos = new CopyOnWriteArrayList<>();
    private final AtomicInteger status = new AtomicInteger(202);
    private volatile long atrasoMs = 0;
    private HttpServer servidor;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void subir() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            Map<String, String> cabecalhos = new java.util.HashMap<>();
            troca.getRequestHeaders().forEach((k, v) -> cabecalhos.put(k.toLowerCase(), v.getFirst()));
            recebidos.add(new Pedido(troca.getRequestMethod(), troca.getRequestURI().getPath(), cabecalhos,
                    new String(troca.getRequestBody().readAllBytes())));
            try {
                Thread.sleep(atrasoMs);
            } catch (InterruptedException ignorada) {
                Thread.currentThread().interrupt();
            }
            // Um corpo que repete o código: nada dele pode aparecer nos logs.
            byte[] resposta = ("{\"detalhe\":\"codigo " + CODIGO + " devolvido\"}").getBytes();
            try {
                troca.sendResponseHeaders(status.get(), resposta.length);
                troca.getResponseBody().write(resposta);
            } catch (IOException clienteDesistiu) {
                // timeout do lado de lá: esperado no teste do servidor lento
            }
            troca.close();
        });
        servidor.start();

        logger = (Logger) LoggerFactory.getLogger(ClientGoEnviadorDeOtp.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void descer() {
        logger.detachAppender(logs);
        servidor.stop(0);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void mandaOPedidoEsperadoParaCadaFinalidade() throws Exception {
        Map<Finalidade, String> templates = Map.of(
                Finalidade.LOGIN, "otp-login",
                Finalidade.VERIFICAR_EMAIL, "otp-verificar-email",
                Finalidade.REDEFINIR_PIN, "otp-redefinir-pin");

        for (var esperado : templates.entrySet()) {
            recebidos.clear();
            UUID desafio = UUID.randomUUID();
            enviador(url() + "/").enviar(mensagem(esperado.getKey(), desafio));

            assertEquals(1, recebidos.size(), esperado.getKey().name());
            Pedido pedido = recebidos.getFirst();
            assertEquals("POST", pedido.metodo());
            assertEquals("/notificacoes", pedido.caminho(), "a barra final da URL não pode duplicar");
            assertEquals("Bearer " + TOKEN, pedido.cabecalhos().get("authorization"));
            assertEquals(desafio.toString(), pedido.cabecalhos().get("idempotency-key"));
            assertTrue(pedido.cabecalhos().get("content-type").startsWith("application/json"));

            JsonNode corpo = JSON.readTree(pedido.corpo());
            assertEquals(esperado.getValue(), corpo.get("template").asText());
            assertEquals("ana@example.com", corpo.get("destinatario").asText());
            assertEquals("alta", corpo.get("prioridade").asText());
            assertEquals(CODIGO, corpo.get("variaveis").get("codigo").asText());
            assertEquals(1, corpo.get("variaveis").size(), "só o código; 'minutos' o Client_GO calcula");
            assertEquals("2030-01-01T10:10:00Z", corpo.get("expira_em").asText());
        }
    }

    @Test
    void respostaDeErroNaoLancaELogaSoOStatus() {
        for (int codigoHttp : new int[]{401, 422, 429, 500, 503}) {
            status.set(codigoHttp);
            assertDoesNotThrow(() -> enviador(url()).enviar(mensagem(Finalidade.LOGIN, UUID.randomUUID())),
                    "status " + codigoHttp);
        }
        assertTrue(logs.list.stream().anyMatch(e -> e.getFormattedMessage().contains("status=503")));
        assertSemSegredosNoLog();
    }

    @Test
    void servidorForaDoArNaoLanca() {
        String urlMorta = url();
        servidor.stop(0);

        assertDoesNotThrow(() -> enviador(urlMorta).enviar(mensagem(Finalidade.LOGIN, UUID.randomUUID())));
        assertTrue(logs.list.stream().anyMatch(e -> e.getFormattedMessage().contains("Falha ao enviar OTP")));
        assertSemSegredosNoLog();
    }

    @Test
    void servidorLentoEstouraNoTimeoutSemTravarARequisicao() {
        atrasoMs = 3_000;
        long inicio = System.nanoTime();

        assertDoesNotThrow(() -> new ClientGoEnviadorDeOtp(JSON, url(), TOKEN, Duration.ofMillis(300))
                .enviar(mensagem(Finalidade.LOGIN, UUID.randomUUID())));

        long ms = (System.nanoTime() - inicio) / 1_000_000;
        assertTrue(ms < 2_000, "o timeout de 300 ms tem que valer; levou " + ms + " ms");
    }

    @Test
    void threadInterrompidaNaoLancaEPreservaAFlag() {
        atrasoMs = 500;
        Thread.currentThread().interrupt();
        try {
            assertDoesNotThrow(() -> enviador(url()).enviar(mensagem(Finalidade.LOGIN, UUID.randomUUID())));
            assertTrue(Thread.currentThread().isInterrupted(), "quem chamou precisa saber que foi interrompido");
        } finally {
            Thread.interrupted(); // limpa para não contaminar os outros testes
        }
    }

    @Test
    void dentroDeTransacaoSoEnviaDepoisDoCommit() {
        TransactionSynchronizationManager.initSynchronization();
        enviador(url()).enviar(mensagem(Finalidade.LOGIN, UUID.randomUUID()));

        assertTrue(recebidos.isEmpty(), "nada pode sair antes do commit");

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertEquals(1, recebidos.size());
    }

    @Test
    void rollbackNaoEnviaNada() {
        TransactionSynchronizationManager.initSynchronization();
        enviador(url()).enviar(mensagem(Finalidade.LOGIN, UUID.randomUUID()));

        // rollback: o Spring chama afterCompletion(STATUS_ROLLED_BACK), nunca afterCommit
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertTrue(recebidos.isEmpty());
    }

    @Test
    void construtorRecusaConfiguracaoInvalida() {
        Duration ok = Duration.ofSeconds(1);
        assertThrows(IllegalArgumentException.class, () -> new ClientGoEnviadorDeOtp(JSON, "", TOKEN, ok));
        assertThrows(IllegalArgumentException.class, () -> new ClientGoEnviadorDeOtp(JSON, null, TOKEN, ok));
        assertThrows(IllegalArgumentException.class, () -> new ClientGoEnviadorDeOtp(JSON, "ftp://x", TOKEN, ok));
        assertThrows(IllegalArgumentException.class, () -> new ClientGoEnviadorDeOtp(JSON, "isto não é url", TOKEN, ok));
        assertThrows(IllegalArgumentException.class, () -> new ClientGoEnviadorDeOtp(JSON, "http://x", "curto", ok));
        assertThrows(IllegalArgumentException.class, () -> new ClientGoEnviadorDeOtp(JSON, "http://x", null, ok));
        assertThrows(IllegalArgumentException.class, () -> new ClientGoEnviadorDeOtp(JSON, "http://x", TOKEN, Duration.ZERO));
    }

    private void assertSemSegredosNoLog() {
        String tudo = String.join("\n", logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
        assertFalse(tudo.contains(CODIGO), "o código não pode aparecer no log");
        assertFalse(tudo.contains("devolvido"), "o corpo da resposta não pode aparecer no log");
        assertFalse(tudo.contains(TOKEN), "o token não pode aparecer no log");
    }

    private String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    private static ClientGoEnviadorDeOtp enviador(String url) {
        return new ClientGoEnviadorDeOtp(JSON, url, TOKEN, Duration.ofSeconds(2));
    }

    private static Mensagem mensagem(Finalidade finalidade, UUID desafio) {
        return new Mensagem(desafio, "ana@example.com", finalidade, CODIGO, Instant.parse("2030-01-01T10:10:00Z"));
    }
}
