package github.jotagm.clube_livro.adapter.out.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "booklit.eventos.habilitado", havingValue = "true")
public class OutboxRelay {

    private final OutboxEventoRepository repository;
    private final SnsClient snsClient;
    private final String topicoArn;

    public OutboxRelay(OutboxEventoRepository repository,
                       SnsClient snsClient,
                       @Value("${booklit.eventos.topico-arn}") String topicoArn) {
        this.repository = repository;
        this.snsClient = snsClient;
        this.topicoArn = topicoArn;
    }

    @Scheduled(fixedDelayString = "${booklit.eventos.outbox.intervalo-ms:5000}")
    @Transactional
    public void publicarPendentes() {
        List<OutboxEvento> pendentes = repository.findTop50ByPublicadoEmIsNullOrderByCriadoEmAsc();

        for (OutboxEvento evento : pendentes) {
            try {
                snsClient.publish(PublishRequest.builder()
                        .topicArn(topicoArn)
                        .message(evento.getPayload())
                        .messageAttributes(Map.of("tipo", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(evento.getTipo())
                                .build()))
                        .build());
                evento.marcarPublicado(Instant.now());
                log.info("Evento publicado id={} tipo={}", evento.getId(), evento.getTipo());
            } catch (SdkException e) {
                // SNS fora do ar: para aqui, o evento continua pendente para a próxima rodada
                log.warn("Falha ao publicar evento id={}: {}", evento.getId(), e.getMessage());
                break;
            }
        }
    }
}