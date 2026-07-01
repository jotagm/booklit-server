package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.ClubeStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ClubeResponse(
        UUID id,
        String nome,
        String descricao,
        boolean privado,
        ClubeStatus status,
        LocalDateTime createdAt,
        List<TemaResponse> temas
) {
    public static ClubeResponse from(Clube clube) {
        return new ClubeResponse(
                clube.getId(),
                clube.getNome(),
                clube.getDescricao(),
                clube.isPrivado(),
                clube.getStatus(),
                clube.getCreatedAt(),
                clube.getTemas() == null ? List.of() :
                        clube.getTemas().stream().map(TemaResponse::from).toList()
        );
    }
}
