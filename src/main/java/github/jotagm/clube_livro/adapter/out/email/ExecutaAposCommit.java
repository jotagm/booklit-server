package github.jotagm.clube_livro.adapter.out.email;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Roda a ação depois do commit da transação em andamento; sem transação, roda na hora.
 *
 * <p>Os enviadores de OTP usam isto para que nenhum código saia para um desafio cuja
 * transação deu rollback.
 */
final class ExecutaAposCommit {

    private ExecutaAposCommit() {
    }

    static void executar(Runnable acao) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    acao.run();
                }
            });
        } else {
            acao.run();
        }
    }
}
