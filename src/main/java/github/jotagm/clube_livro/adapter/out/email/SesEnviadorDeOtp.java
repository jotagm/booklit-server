package github.jotagm.clube_livro.adapter.out.email;

import github.jotagm.auth.otp.EnviadorDeOtp;
import github.jotagm.auth.otp.Finalidade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

import java.time.Duration;
import java.time.Instant;

/**
 * Entrega o código pelo SES v2 (no Floci em dev, na AWS em produção; muda só o endpoint).
 *
 * <p>O envio acontece <b>depois do commit</b>: se a transação que gravou o desafio der
 * rollback, nenhum código sai para um desafio que não existe. Não passa pelo outbox de
 * propósito: o outbox gravaria o código em claro no banco, e garantia de entrega importa
 * pouco para algo que vale 10 minutos; se o SES falhar, a pessoa pede outro.
 *
 * <p>Nada aqui loga o código nem o corpo do e-mail, só o id do desafio.
 */
@Slf4j
public class SesEnviadorDeOtp implements EnviadorDeOtp {

    private final SesV2Client ses;
    private final String remetente;

    public SesEnviadorDeOtp(SesV2Client ses, String remetente) {
        this.ses = ses;
        this.remetente = remetente;
    }

    @Override
    public void enviar(Mensagem mensagem) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviarAgora(mensagem);
                }
            });
        } else {
            enviarAgora(mensagem);
        }
    }

    private void enviarAgora(Mensagem mensagem) {
        try {
            String messageId = ses.sendEmail(SendEmailRequest.builder()
                    .fromEmailAddress(remetente)
                    .destination(Destination.builder().toAddresses(mensagem.destinatario()).build())
                    .content(EmailContent.builder().simple(Message.builder()
                            .subject(texto(assunto(mensagem.finalidade())))
                            .body(Body.builder().text(texto(corpo(mensagem))).build())
                            .build()).build())
                    .build()).messageId();
            log.info("OTP enviado desafio={} finalidade={} sesMessageId={}",
                    mensagem.desafioId(), mensagem.finalidade(), messageId);
        } catch (SdkException e) {
            log.warn("Falha ao enviar OTP desafio={}: {}", mensagem.desafioId(), e.getMessage());
        }
    }

    /** Assunto sem o código: ele apareceria na notificação da tela bloqueada. */
    static String assunto(Finalidade finalidade) {
        return switch (finalidade) {
            case LOGIN -> "Seu código de acesso ao Booklit";
            case VERIFICAR_EMAIL -> "Confirme seu e-mail no Booklit";
            case REDEFINIR_PIN -> "Código para redefinir seu PIN no Booklit";
        };
    }

    /** Texto puro e sem link: e-mail de código com link é o formato que o phishing imita. */
    static String corpo(Mensagem mensagem) {
        long minutos = Math.max(1, Duration.between(Instant.now(), mensagem.expiraEm()).toMinutes());
        return """
                Seu código é %s

                Ele vale por %d minutos e só pode ser usado uma vez.
                Se não foi você que pediu, ignore este e-mail: sem o código ninguém entra na sua conta.

                Booklit
                """.formatted(mensagem.codigo(), minutos);
    }

    private static Content texto(String valor) {
        return Content.builder().data(valor).charset("UTF-8").build();
    }
}
