package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioClubeResponse(
        UUID id,
        UUID usuarioId,
        String nomeUsuario,
        UUID clubeId,
        String nomeClube,
        ClubePapel papel,
        LocalDateTime entrouEm
) {
    public static UsuarioClubeResponse from(UsuarioClube uc) {
        return new UsuarioClubeResponse(
                uc.getId(),
                uc.getUsuario().getId(),
                uc.getUsuario().getNome(),
                uc.getClube().getId(),
                uc.getClube().getNome(),
                uc.getPapel(),
                uc.getEntrouEm()
        );
    }
}
