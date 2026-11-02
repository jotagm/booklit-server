package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.feed.TipoReacao;

import java.time.LocalDateTime;
import java.util.List;
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
        String usuarioFotoUrl,
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
        TipoReacao minhaReacao,
        /** Quem reagiu, você por último ("Ana, Carla e você"); no máximo 10. */
        List<Pessoa> quemReagiu,
        /** COMENTARIO: quantas respostas a conversa tem (o "responder · 2"). */
        Integer respostas,
        /** COMENTARIO: a resposta mais recente, mostrada abaixo do comentário. */
        Resposta ultimaResposta,
        /** OPCAO_SUGERIDA: votação da sugestão (o "votar nesse →" leva para ela). */
        UUID votacaoId,
        /** OPCAO_SUGERIDA: se ainda dá para votar. */
        Boolean votacaoAberta,
        /** OPCAO_SUGERIDA: quantos membros têm o livro no "quero ler". */
        Long membrosQueremLer,
        /** CONCLUIU: foi a primeira pessoa do clube a terminar a leitura. */
        boolean primeiraDoClube
) {
    public record Pessoa(Integer usuarioId, String nome) {
    }

    public record Resposta(Integer usuarioId, String usuarioNome, String usuarioFotoUrl, String texto) {
    }

    public enum TipoFeed {
        PROGRESSO,
        CONCLUIU,
        COMENTARIO,
        OPCAO_SUGERIDA,
        LEITURA_INICIADA,
        VOTACAO_ABERTA,
        VOTACAO_ENCERRADA,
        MEMBRO_ENTROU
    }
}
