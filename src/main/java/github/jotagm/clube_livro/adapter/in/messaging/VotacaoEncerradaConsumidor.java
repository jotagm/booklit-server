package github.jotagm.clube_livro.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.jotagm.clube_livro.application.service.LeituraClubeService;
import github.jotagm.clube_livro.domain.clube.votacao.VotacaoEncerrada;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consome votacao.encerrada e cria os registros dos membros para a nova leitura.
 * A mensagem só é apagada depois de processar com sucesso; se falhar, volta para a
 * fila e, depois de 3 tentativas, vai para a DLQ.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "booklit.eventos.habilitado", havingValue = "true")
public class VotacaoEncerradaConsumidor {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final LeituraClubeService leituraClubeService;
    private final TransactionTemplate transactionTemplate;
    private final String filaUrl;

    public VotacaoEncerradaConsumidor(SqsClient sqsClient,
                                      ObjectMapper objectMapper,
                                      EventoProcessadoRepository eventoProcessadoRepository,
                                      LeituraClubeService leituraClubeService,
                                      TransactionTemplate transactionTemplate,
                                      @Value("${booklit.eventos.fila-url}") String filaUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
        this.leituraClubeService = leituraClubeService;
        this.transactionTemplate = transactionTemplate;
        this.filaUrl = filaUrl;
    }

    @Scheduled(fixedDelayString = "${booklit.eventos.consumidor.intervalo-ms:2000}")
    public void consumir() {
        List<Message> mensagens = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(filaUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(1)
                .build()).messages();

        for (Message mensagem : mensagens) {
            try {
                processar(mensagem.body());
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(filaUrl)
                        .receiptHandle(mensagem.receiptHandle())
                        .build());
            } catch (Exception e) {
                // Não apaga: a mensagem volta para a fila após o visibility timeout.
                log.warn("Falha ao processar mensagem {}: {}", mensagem.messageId(), e.getMessage());
            }
        }
    }

    private void processar(String body) throws Exception {
        JsonNode envelope = objectMapper.readTree(body);
        String tipo = envelope.path("type").asText();

        if (!VotacaoEncerrada.TIPO.equals(tipo)) {
            log.debug("Ignorando evento do tipo {}", tipo);
            return;
        }

        UUID eventId = UUID.fromString(envelope.path("eventId").asText());
        JsonNode leituraNode = envelope.path("data").path("leituraClubeId");
        if (leituraNode.isMissingNode() || leituraNode.isNull()) {
            throw new IllegalArgumentException("Evento " + eventId + " sem leituraClubeId");
        }
        UUID leituraId = UUID.fromString(leituraNode.asText());

        // Checagem, efeito e marcação na MESMA transação: ou tudo acontece, ou nada.
        transactionTemplate.executeWithoutResult(status -> {
            if (eventoProcessadoRepository.existsById(eventId)) {
                log.info("Evento {} já processado, ignorando duplicata", eventId);
                return;
            }
            int membros = leituraClubeService.iniciarRegistrosDosMembros(leituraId);
            eventoProcessadoRepository.save(new EventoProcessado(eventId, tipo, Instant.now()));
            log.info("Registros iniciados leitura={} membros={} eventId={}", leituraId, membros, eventId);
        });
    }
}
