package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.leitura.Comentario;

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
        List<ComentarioResponse> respostas
) {

    public static ComentarioResponse deFolha(Comentario comentario) {
        return deFolha(comentario, u -> null);
    }

    public static ComentarioResponse deFolha(Comentario comentario, Function<Usuario, String> foto) {
        return montar(comentario, List.of(), foto);
    }

    public static List<ComentarioResponse> montarArvore(List<Comentario> todos) {
        return montarArvore(todos, u -> null);
    }

    public static List<ComentarioResponse> montarArvore(List<Comentario> todos, Function<Usuario, String> foto) {
        Map<UUID, List<Comentario>> respostasPorPai = todos.stream()
                .filter(c -> c.getComentarioPai() != null)
                .collect(Collectors.groupingBy(c -> c.getComentarioPai().getId()));

        return todos.stream()
                .filter(c -> c.getComentarioPai() == null)
                .map(raiz -> montar(raiz, respostasPorPai.getOrDefault(raiz.getId(), List.of()).stream()
                        .map(c -> deFolha(c, foto))
                        .toList(), foto))
                .toList();
    }

    private static ComentarioResponse montar(Comentario comentario, List<ComentarioResponse> respostas,
                                              Function<Usuario, String> foto) {
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
                respostas
        );
    }
}
