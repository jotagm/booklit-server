package github.jotagm.clube_livro.adapter.out.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.jotagm.clube_livro.application.port.out.EnviadorDeConvite;
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
 * Manda o aviso de convite pelo Client_GO ({@code POST /notificacoes}, template
 * {@code convite-clube}). Mesmas regras do {@link ClientGoEnviadorDeOtp}: envia depois do
 * commit, falha vira aviso no log e nunca derruba a criação do convite, e a
 * {@code Idempotency-Key} é o id do convite.
 *
 * <p>Os nomes (de quem convidou e do clube) são texto livre digitado por usuários. Ficam só no
 * corpo do e-mail, nunca no assunto, e aqui perdem quebra de linha e passam por um teto de
 * tamanho. O log não leva e-mail nem nomes: só o id do convite e o status.
 */
public class ClientGoEnviadorDeConvite implements EnviadorDeConvite {

    private static final Logger log = LoggerFactory.getLogger(ClientGoEnviadorDeConvite.class);

    static final String TEMPLATE = "convite-clube";
    static final int TAMANHO_MAXIMO_NOME = 80;

    private final HttpClient http;
    private final ObjectMapper json;
    private final URI endpoint;
    private final String token;
    private final Duration timeout;
    private final String link;

    /**
     * @param frontUrl endereço do site (ex.: https://booklit.bio); o link do e-mail leva à lista
     *                 de clubes, onde os convites pendentes aparecem
     */
    public ClientGoEnviadorDeConvite(ObjectMapper json, String url, String token, Duration timeout, String frontUrl) {
        if (token == null || token.strip().length() < ClientGoEnviadorDeOtp.TAMANHO_MINIMO_TOKEN) {
            throw new IllegalArgumentException("booklit.auth.email.client-go.token precisa ter "
                    + ClientGoEnviadorDeOtp.TAMANHO_MINIMO_TOKEN + "+ caracteres");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("booklit.auth.email.client-go.timeout precisa ser positivo");
        }
        this.endpoint = ClientGoEnviadorDeOtp.montarEndpoint(url);
        this.json = json;
        this.token = token.strip();
        this.timeout = timeout;
        this.link = montarLink(frontUrl);
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
                    .header("Idempotency-Key", mensagem.conviteId().toString())
                    .POST(HttpRequest.BodyPublishers.ofString(corpo(mensagem)))
                    .build();

            HttpResponse<Void> resposta = http.send(pedido, HttpResponse.BodyHandlers.discarding());

            if (resposta.statusCode() == 202) {
                log.info("Aviso de convite entregue ao Client_GO convite={} repetido={}",
                        mensagem.conviteId(), resposta.headers().firstValue("Idempotent-Replayed").orElse("false"));
            } else {
                log.warn("Client_GO recusou o aviso de convite convite={} status={}",
                        mensagem.conviteId(), resposta.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Envio do aviso de convite interrompido convite={}", mensagem.conviteId());
        } catch (Exception e) {
            log.warn("Falha ao enviar o aviso de convite convite={}: {}", mensagem.conviteId(), e.toString());
        }
    }

    private String corpo(Mensagem mensagem) throws JsonProcessingException {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("template", TEMPLATE);
        corpo.put("destinatario", mensagem.destinatario());
        corpo.put("prioridade", "normal");
        Map<String, String> variaveis = new LinkedHashMap<>();
        variaveis.put("convidado_por", limpar(mensagem.convidadoPor()));
        variaveis.put("clube", limpar(mensagem.clube()));
        variaveis.put("link", link);
        corpo.put("variaveis", variaveis);
        return json.writeValueAsString(corpo);
    }

    /** Uma linha só, sem caracteres de controle, com teto de tamanho; nunca devolve vazio. */
    static String limpar(String texto) {
        String limpo = texto == null ? "" : texto.replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]+", " ").strip();
        if (limpo.length() > TAMANHO_MAXIMO_NOME) {
            limpo = limpo.substring(0, TAMANHO_MAXIMO_NOME).strip() + "…";
        }
        return limpo.isEmpty() ? "alguém" : limpo;
    }

    static String montarLink(String frontUrl) {
        if (frontUrl == null || frontUrl.isBlank()) {
            throw new IllegalArgumentException("booklit.convites.email.front-url é obrigatória");
        }
        String base = frontUrl.strip().replaceAll("/+$", "");
        URI uri;
        try {
            uri = URI.create(base);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("booklit.convites.email.front-url inválida: " + base, e);
        }
        if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw new IllegalArgumentException("booklit.convites.email.front-url precisa ser http(s)://host: " + base);
        }
        return base + "/clubes";
    }
}
