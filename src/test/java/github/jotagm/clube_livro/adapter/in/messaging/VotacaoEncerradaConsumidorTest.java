package github.jotagm.clube_livro.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.jotagm.clube_livro.application.service.LeituraClubeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VotacaoEncerradaConsumidorTest {

    private final SqsClient sqs = mock(SqsClient.class);
    private final EventoProcessadoRepository processados = mock(EventoProcessadoRepository.class);
    private final LeituraClubeService leituras = mock(LeituraClubeService.class);
    private final TransactionTemplate transacao = mock(TransactionTemplate.class);

    private VotacaoEncerradaConsumidor consumidor(int espera) {
        doAnswer(inv -> {
            Consumer<org.springframework.transaction.TransactionStatus> acao = inv.getArgument(0);
            acao.accept(null);
            return null;
        }).when(transacao).executeWithoutResult(any());
        return new VotacaoEncerradaConsumidor(sqs, new ObjectMapper(), processados, leituras, transacao,
                "https://sqs/fila", espera);
    }

    @Test
    void usaLongPollingDeVinteSegundos() {
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().build());

        consumidor(20).consumir();

        ArgumentCaptor<ReceiveMessageRequest> pedido = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqs).receiveMessage(pedido.capture());
        assertEquals(20, pedido.getValue().waitTimeSeconds());
        assertEquals(10, pedido.getValue().maxNumberOfMessages());
    }

    @Test
    void esperaForaDoIntervaloDoSqsEhAjustada() {
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().build());

        consumidor(60).consumir();

        ArgumentCaptor<ReceiveMessageRequest> pedido = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqs).receiveMessage(pedido.capture());
        assertEquals(20, pedido.getValue().waitTimeSeconds());
    }

    @Test
    void apagaAMensagemSoDepoisDeProcessar() {
        UUID leitura = UUID.randomUUID();
        String corpo = "{\"type\":\"votacao.encerrada\",\"eventId\":\"" + UUID.randomUUID()
                + "\",\"data\":{\"leituraClubeId\":\"" + leitura + "\"}}";
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder()
                .messages(Message.builder().messageId("m1").receiptHandle("r1").body(corpo).build()).build());

        consumidor(20).consumir();

        verify(leituras).iniciarRegistrosDosMembros(leitura);
        verify(sqs).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void falhaNoProcessamentoDeixaAMensagemNaFila() {
        String corpo = "{\"type\":\"votacao.encerrada\",\"eventId\":\"" + UUID.randomUUID() + "\",\"data\":{}}";
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder()
                .messages(Message.builder().messageId("m1").receiptHandle("r1").body(corpo).build()).build());

        consumidor(20).consumir();

        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
    }
}
