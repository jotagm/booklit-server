package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.leitura.Comentario;
import github.jotagm.clube_livro.domain.feed.TipoReacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import java.util.stream.Collectors;

public record ComentarioResponse(
        UUID id,
        UUID leituraClubeId,
        Integer usuarioId,
        String usuarioNome,
        String usuarioFotoUrl,
        String conteudo,
        boolean removido,
        /** Página (ou capítulo) do autor ao comentar; nulo em comentários antigos ou removidos. */
        Integer paginaAutor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ComentarioResponse> respostas,
        /** Reações ao comentário (o alvo é {@code comentario:<id>}, o mesmo do feed). */
        Map<TipoReacao, Long> reacoes,
        TipoReacao minhaReacao
) {
    /** Reações de um comentário para quem está vendo. */
    public record Reacoes(Map<TipoReacao, Long> contagem, TipoReacao minha) {
        static final Reacoes NENHUMA = new Reacoes(Map.of(), null);
    }

    /** O alvo das reações de um comentário, igual ao id do item do feed. */
    public static String alvo(Comentario comentario) {
        return "comentario:" + comentario.getId();
    }


    public static ComentarioResponse deFolha(Comentario comentario) {
        return deFolha(comentario, u -> null);
    }

    public static ComentarioResponse deFolha(Comentario comentario, Function<Usuario, String> foto) {
        return montar(comentario, List.of(), foto, c -> Reacoes.NENHUMA);
    }

    public static List<ComentarioResponse> montarArvore(List<Comentario> todos) {
        return montarArvore(todos, u -> null);
    }

    public static List<ComentarioResponse> montarArvore(List<Comentario> todos, Function<Usuario, String> foto) {
        return montarArvore(todos, foto, c -> Reacoes.NENHUMA);
    }

    public static List<ComentarioResponse> montarArvore(List<Comentario> todos, Function<Usuario, String> foto,
                                                        Function<Comentario, Reacoes> reacoes) {
        Map<UUID, List<Comentario>> respostasPorPai = todos.stream()
                .filter(c -> c.getComentarioPai() != null)
                .collect(Collectors.groupingBy(c -> c.getComentarioPai().getId()));

        return todos.stream()
                .filter(c -> c.getComentarioPai() == null)
                .map(raiz -> montar(raiz, respostasPorPai.getOrDefault(raiz.getId(), List.of()).stream()
                        .map(c -> montar(c, List.of(), foto, reacoes))
                        .toList(), foto, reacoes))
                .toList();
    }

    private static ComentarioResponse montar(Comentario comentario, List<ComentarioResponse> respostas,
                                              Function<Usuario, String> foto, Function<Comentario, Reacoes> reacoes) {
        Reacoes r = comentario.isRemovido() ? Reacoes.NENHUMA : reacoes.apply(comentario);
        return new ComentarioResponse(
                comentario.getId(),
                comentario.getLeituraClube().getId(),
                comentario.getUsuario().getId(),
                comentario.getUsuario().getNome(),
                foto.apply(comentario.getUsuario()),
                comentario.isRemovido() ? "[comentário removido]" : comentario.getConteudo(),
                comentario.isRemovido(),
                comentario.isRemovido() ? null : comentario.getPaginaAutor(),
                comentario.getCreatedAt(),
                comentario.getUpdatedAt(),
                respostas,
                r.contagem(),
                r.minha()
        );
    }
}
