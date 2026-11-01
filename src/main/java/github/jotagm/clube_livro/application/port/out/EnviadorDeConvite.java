package github.jotagm.clube_livro.application.port.out;

import java.util.UUID;

/**
 * Avisa por e-mail quem foi convidado para um clube. É um aviso a mais: o convite já existe e
 * aparece na lista de convites da pessoa quando ela entra, então uma falha no envio nunca
 * desfaz nem impede o convite.
 */
public interface EnviadorDeConvite {

    /**
     * Só dados simples, sem entidades: o envio roda depois do commit, fora da sessão do banco.
     *
     * @param conviteId    vira a chave de idempotência: o mesmo convite nunca gera dois e-mails
     * @param destinatario e-mail convidado
     * @param convidadoPor nome de quem convidou
     * @param clube        nome do clube
     */
    record Mensagem(UUID conviteId, String destinatario, String convidadoPor, String clube) {
    }

    void enviar(Mensagem mensagem);
}
