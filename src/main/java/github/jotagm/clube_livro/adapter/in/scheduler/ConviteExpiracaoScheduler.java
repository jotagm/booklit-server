package github.jotagm.clube_livro.adapter.in.scheduler;

import github.jotagm.clube_livro.application.service.ConviteService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class ConviteExpiracaoScheduler {

    private final ConviteService conviteService;

    @Scheduled(cron = "${agendamento.convite.expiracao.cron:0 0 * * * *}")
    public void expirarConvitesVencidos() {
        int quantidade = conviteService.expirarVencidos();

        if (quantidade > 0) {
            log.info("{} convite(s) expirado(s) automaticamente", quantidade);
        }
    }
}
