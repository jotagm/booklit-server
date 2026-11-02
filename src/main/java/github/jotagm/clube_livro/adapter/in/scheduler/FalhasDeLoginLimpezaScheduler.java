package github.jotagm.clube_livro.adapter.in.scheduler;

import github.jotagm.clube_livro.application.service.ProtecaoDeLoginService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Apaga as senhas erradas com mais de um dia; a janela de bloqueio é bem menor que isso. */
@Slf4j
@Component
@AllArgsConstructor
public class FalhasDeLoginLimpezaScheduler {

    private final ProtecaoDeLoginService protecaoDeLogin;

    @Scheduled(cron = "${agendamento.falhas-login.limpeza.cron:0 30 3 * * *}")
    public void limpar() {
        int apagadas = protecaoDeLogin.limparAntigas();

        if (apagadas > 0) {
            log.info("{} falha(s) de login antiga(s) apagada(s)", apagadas);
        }
    }
}
