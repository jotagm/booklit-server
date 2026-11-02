package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;
import github.jotagm.clube_livro.domain.usuario.Usuario;

public record UsuarioClubeResponse(
        UUID id,
        Integer usuarioId,
        String nomeUsuario,
        String fotoUrl,
        UUID clubeId,
        String nomeClube,
        ClubePapel papel,
        LocalDateTime entrouEm
) {
    public static UsuarioClubeResponse from(UsuarioClube uc) {
        return from(uc, u -> null);
    }

    public static UsuarioClubeResponse from(UsuarioClube uc, Function<Usuario, String> foto) {
        return new UsuarioClubeResponse(
                uc.getId(),
                uc.getUsuario().getId(),
                uc.getUsuario().getNome(),
                foto.apply(uc.getUsuario()),
                uc.getClube().getId(),
                uc.getClube().getNome(),
                uc.getPapel(),
                uc.getEntrouEm()
        );
    }
}
