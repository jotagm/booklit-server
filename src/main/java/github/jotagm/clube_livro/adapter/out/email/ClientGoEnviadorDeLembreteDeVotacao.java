package github.jotagm.clube_livro.adapter.out.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.jotagm.clube_livro.application.port.out.EnviadorDeLembreteDeVotacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manda o lembrete de votação pelo Client_GO ({@code POST /notificacoes}, template
 * {@code lembrete-votacao}). Mesmas regras do {@link ClientGoEnviadorDeConvite}: envia depois do
 * commit, falha vira aviso no log, o nome do clube (texto livre) fica só no corpo e o log não
 * leva e-mail nem nome — só o id da votação e o status.
 */
public class ClientGoEnviadorDeLembreteDeVotacao implements EnviadorDeLembreteDeVotacao {

    private static final Logger log = LoggerFactory.getLogger(ClientGoEnviadorDeLembreteDeVotacao.class);

    static final String TEMPLATE = "lembrete-votacao";
    private static final Locale PT_BR = Locale.of("pt", "BR");
    /** "quarta-feira, 5 de novembro, às 20:00" */
    static final DateTimeFormatter PRAZO = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM, 'às' HH:mm", PT_BR);

    private final HttpClient http;
    private final ObjectMapper json;
    private final URI endpoint;
    private final String token;
    private final Duration timeout;
    private final String frontUrl;

    public ClientGoEnviadorDeLembreteDeVotacao(ObjectMapper json, String url, String token, Duration timeout,
                                               String frontUrl) {
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
        // Reaproveita a validação do link do convite (http(s)://host) e fica só com a base.
        this.frontUrl = ClientGoEnviadorDeConvite.montarLink(frontUrl).replaceAll("/clubes$", "");
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
                    .header("Idempotency-Key", mensagem.chave())
                    .POST(HttpRequest.BodyPublishers.ofString(corpo(mensagem)))
                    .build();

            HttpResponse<Void> resposta = http.send(pedido, HttpResponse.BodyHandlers.discarding());

            if (resposta.statusCode() == 202) {
                log.info("Lembrete de votação entregue ao Client_GO votacao={} repetido={}",
                        mensagem.votacaoId(), resposta.headers().firstValue("Idempotent-Replayed").orElse("false"));
            } else {
                log.warn("Client_GO recusou o lembrete de votação votacao={} status={}",
                        mensagem.votacaoId(), resposta.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Envio do lembrete de votação interrompido votacao={}", mensagem.votacaoId());
        } catch (Exception e) {
            log.warn("Falha ao enviar o lembrete de votação votacao={}: {}", mensagem.votacaoId(), e.toString());
        }
    }

    String corpo(Mensagem mensagem) throws JsonProcessingException {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("template", TEMPLATE);
        corpo.put("destinatario", mensagem.destinatario());
        corpo.put("prioridade", "normal");
        Map<String, String> variaveis = new LinkedHashMap<>();
        variaveis.put("clube", ClientGoEnviadorDeConvite.limpar(mensagem.clube()));
        variaveis.put("prazo", prazo(mensagem.encerraEm()));
        variaveis.put("link", frontUrl + "/votacoes/" + mensagem.votacaoId());
        corpo.put("variaveis", variaveis);
        return json.writeValueAsString(corpo);
    }

    static String prazo(LocalDateTime encerraEm) {
        return encerraEm == null ? "em breve" : PRAZO.format(encerraEm);
    }
}
