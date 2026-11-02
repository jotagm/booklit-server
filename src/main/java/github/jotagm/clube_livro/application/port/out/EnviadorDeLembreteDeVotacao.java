package github.jotagm.clube_livro.application.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lembra por e-mail um membro que ainda não votou na votação do próximo livro. Como o aviso
 * de convite, é um a mais: falhar no envio nunca desfaz o lembrete registrado.
 */
public interface EnviadorDeLembreteDeVotacao {

    /**
     * Só dados simples: o envio roda depois do commit, fora da sessão do banco.
     *
     * @param chave         idempotência: votação + membro + momento do lembrete; repetir o mesmo
     *                      pedido nunca gera dois e-mails
     * @param votacaoId     leva o link direto para a votação
     * @param destinatario  e-mail do membro
     * @param clube         nome do clube
     * @param encerraEm     quando a votação fecha
     */
    record Mensagem(String chave, UUID votacaoId, String destinatario, String clube, LocalDateTime encerraEm) {
    }

    void enviar(Mensagem mensagem);
}
