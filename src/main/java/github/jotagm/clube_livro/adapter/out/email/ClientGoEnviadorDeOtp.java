package github.jotagm.clube_livro.adapter.out.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.jotagm.auth.otp.EnviadorDeOtp;
import github.jotagm.auth.otp.Finalidade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entrega o código pelo Client_GO ({@code POST /notificacoes}), que renderiza o template e
 * manda pelo SES. Alternativa ao {@link SesEnviadorDeOtp}: a escolha é a propriedade
 * {@code booklit.auth.email.provedor}, e o auth-core não muda.
 *
 * <p>Igual ao envio direto pelo SES:
 * <ul>
 *   <li>o envio acontece <b>depois do commit</b> da transação que gravou o desafio;</li>
 *   <li>não passa pelo outbox (ele gravaria o código em claro no banco);</li>
 *   <li>falha não derruba a requisição: vira um aviso no log e a pessoa pede outro código.</li>
 * </ul>
 *
 * <p>A {@code Idempotency-Key} é o id do desafio: se algum dia houver reenvio automático, o
 * Client_GO devolve o mesmo pedido em vez de mandar dois e-mails.
 *
 * <p>Nada aqui loga o código nem o corpo da resposta, só o id do desafio e o status.
 */
public class ClientGoEnviadorDeOtp implements EnviadorDeOtp {

    private static final Logger log = LoggerFactory.getLogger(ClientGoEnviadorDeOtp.class);

    /** O Client_GO recusa tokens menores que isto; falhar aqui mostra o erro na subida, não no primeiro login. */
    static final int TAMANHO_MINIMO_TOKEN = 32;

    private final HttpClient http;
    private final ObjectMapper json;
    private final URI endpoint;
    private final String token;
    private final Duration timeout;

    public ClientGoEnviadorDeOtp(ObjectMapper json, String url, String token, Duration timeout) {
        if (token == null || token.strip().length() < TAMANHO_MINIMO_TOKEN) {
            throw new IllegalArgumentException(
                    "booklit.auth.email.client-go.token precisa ter " + TAMANHO_MINIMO_TOKEN + "+ caracteres");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("booklit.auth.email.client-go.timeout precisa ser positivo");
        }
        this.endpoint = montarEndpoint(url);
        this.json = json;
        this.token = token.strip();
        this.timeout = timeout;
        // HTTP/1.1 explícito: o Client_GO não fala h2c, e a tentativa de upgrade só atrapalha.
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public void enviar(Mensagem mensagem) {
        ExecutaAposCommit.executar(() -> enviarAgora(mensagem));
    }

    private void enviarAgora(Mensagem mensagem) {
        try {
            HttpRequest pedido = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .header("Idempotency-Key", mensagem.desafioId().toString())
                    .POST(HttpRequest.BodyPublishers.ofString(corpo(mensagem)))
                    .build();

            // O corpo da resposta é descartado de propósito: nada nele é útil e nada dele deve ir para log.
            HttpResponse<Void> resposta = http.send(pedido, HttpResponse.BodyHandlers.discarding());

            if (resposta.statusCode() == 202) {
                log.info("OTP entregue ao Client_GO desafio={} finalidade={} repetido={}",
                        mensagem.desafioId(), mensagem.finalidade(),
                        resposta.headers().firstValue("Idempotent-Replayed").orElse("false"));
            } else {
                log.warn("Client_GO recusou o OTP desafio={} finalidade={} status={}",
                        mensagem.desafioId(), mensagem.finalidade(), resposta.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Envio do OTP ao Client_GO interrompido desafio={}", mensagem.desafioId());
        } catch (Exception e) {
            // Qualquer falha (rede, timeout, JSON): a requisição que pediu o código já foi atendida.
            log.warn("Falha ao enviar OTP ao Client_GO desafio={}: {}", mensagem.desafioId(), e.toString());
        }
    }

    private String corpo(Mensagem mensagem) throws com.fasterxml.jackson.core.JsonProcessingException {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("template", template(mensagem.finalidade()));
        corpo.put("destinatario", mensagem.destinatario());
        // Código de login não pode esperar atrás de e-mail de lembrete: fila de prioridade alta.
        corpo.put("prioridade", "alta");
        corpo.put("variaveis", Map.of("codigo", mensagem.codigo()));
        // O Client_GO calcula "vale por N minutos" a partir disto e descarta o envio depois da hora.
        corpo.put("expira_em", mensagem.expiraEm().toString());
        return json.writeValueAsString(corpo);
    }

    /** Nomes dos templates em db/002_templates_otp.sql do Client_GO. */
    static String template(Finalidade finalidade) {
        return switch (finalidade) {
            case LOGIN -> "otp-login";
            case VERIFICAR_EMAIL -> "otp-verificar-email";
            case REDEFINIR_PIN -> "otp-redefinir-pin";
            case REDEFINIR_SENHA -> "otp-redefinir-senha";
        };
    }

    static URI montarEndpoint(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("booklit.auth.email.client-go.url é obrigatória");
        }
        String base = url.strip().replaceAll("/+$", "");
        URI uri;
        try {
            uri = URI.create(base + "/notificacoes");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("booklit.auth.email.client-go.url inválida: " + base, e);
        }
        if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw new IllegalArgumentException("booklit.auth.email.client-go.url precisa ser http(s)://host: " + base);
        }
        return uri;
    }
}
