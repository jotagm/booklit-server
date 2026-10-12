package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.feed.TipoReacao;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Um acontecimento nos clubes do usuário. {@code id} é estável (ex.: {@code registro:<uuid>}) e é
 * o que as reações usam como alvo. Os campos de livro/progresso/texto só vêm quando fazem sentido
 * para o {@code tipo}.
 */
public record FeedItemResponse(
        String id,
        TipoFeed tipo,
        UUID clubeId,
        String clubeNome,
        Integer usuarioId,
        String usuarioNome,
        boolean ehVoce,
        LocalDateTime quando,
        String livroTitulo,
        String livroCapaUrl,
        Integer valorAtual,
        Integer valorMeta,
        String unidade,
        String texto,
        /** Id da leitura do clube, para abrir a tela ao tocar (null em votação/entrada). */
        UUID leituraId,
        Map<TipoReacao, Long> reacoes,
        TipoReacao minhaReacao
) {
    public enum TipoFeed {
        PROGRESSO,
        CONCLUIU,
        COMENTARIO,
        LEITURA_INICIADA,
        VOTACAO_ABERTA,
        VOTACAO_ENCERRADA,
        MEMBRO_ENTROU
    }
}
