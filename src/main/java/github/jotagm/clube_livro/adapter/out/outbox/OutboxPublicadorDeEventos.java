package github.jotagm.clube_livro.adapter.out.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.jotagm.clube_livro.application.port.out.PublicadorDeEventos;
import github.jotagm.clube_livro.domain.evento.EventoDeDominio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxPublicadorDeEventos implements PublicadorDeEventos {

    private final OutboxEventoRepository repository;
    private final ObjectMapper objectMapper;

    // MANDATORY: falha se for chamado fora de uma transação
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publicar(EventoDeDominio evento) {
        repository.save(new OutboxEvento(
                evento.eventId(),
                evento.tipo(),
                evento.agregadoId(),
                envelope(evento),
                evento.ocorridoEm()));
    }

    private String envelope(EventoDeDominio evento) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", evento.eventId());
        envelope.put("type", evento.tipo());
        envelope.put("version", evento.versao());
        envelope.put("occurredAt", evento.ocorridoEm());
        envelope.put("data", evento.dados());
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar evento " + evento.tipo(), e);
        }
    }
}