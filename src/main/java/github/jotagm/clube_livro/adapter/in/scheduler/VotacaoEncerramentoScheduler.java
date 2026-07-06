package github.jotagm.clube_livro.adapter.in.scheduler;

import github.jotagm.clube_livro.application.service.VotacaoEnceramentoService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class VotacaoEncerramentoScheduler {

    private final VotacaoEnceramentoService votacaoEnceramentoService;

    @Scheduled(cron = "${agendamento.votacao.encerramento.cron:0 0 * * * *}")
    public void encerrarVotacoesVencidas() {
        int quantidade = votacaoEnceramentoService.encerrarVotacoesVencidas().size();

        if (quantidade > 0) {
            log.info("{} votação(ões) encerrada(s) automaticamente", quantidade);
        }
    }
}
