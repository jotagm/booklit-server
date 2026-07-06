package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.leitura.Comentario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record ComentarioResponse(
        UUID id,
        UUID leituraClubeId,
        UUID usuarioId,
        String usuarioNome,
        String conteudo,
        boolean removido,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ComentarioResponse> respostas
) {

    public static ComentarioResponse deFolha(Comentario comentario) {
        return montar(comentario, List.of());
    }

    public static List<ComentarioResponse> montarArvore(List<Comentario> todos) {
        Map<UUID, List<Comentario>> respostasPorPai = todos.stream()
                .filter(c -> c.getComentarioPai() != null)
                .collect(Collectors.groupingBy(c -> c.getComentarioPai().getId()));

        return todos.stream()
                .filter(c -> c.getComentarioPai() == null)
                .map(raiz -> montar(raiz, respostasPorPai.getOrDefault(raiz.getId(), List.of()).stream()
                        .map(ComentarioResponse::deFolha)
                        .toList()))
                .toList();
    }

    private static ComentarioResponse montar(Comentario comentario, List<ComentarioResponse> respostas) {
        return new ComentarioResponse(
                comentario.getId(),
                comentario.getLeituraClube().getId(),
                comentario.getUsuario().getId(),
                comentario.getUsuario().getNome(),
                comentario.isRemovido() ? "[comentário removido]" : comentario.getConteudo(),
                comentario.isRemovido(),
                comentario.getCreatedAt(),
                comentario.getUpdatedAt(),
                respostas
        );
    }
}
