package github.jotagm.clube_livro.adapter.in.scheduler;

import github.jotagm.clube_livro.application.service.VotacaoEncerramentoService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class VotacaoEncerramentoScheduler {

    private final VotacaoEncerramentoService votacaoEncerramentoService;

    @Scheduled(cron = "${agendamento.votacao.encerramento.cron:0 0 * * * *}")
    public void encerrarVotacoesVencidas() {
        int quantidade = votacaoEncerramentoService.encerrarVotacoesVencidas().size();

        if (quantidade > 0) {
            log.info("{} votação(ões) encerrada(s) automaticamente", quantidade);
        }
    }
}
