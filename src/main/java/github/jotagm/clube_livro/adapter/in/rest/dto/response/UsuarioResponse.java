package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.usuario.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Integer id,
        String nome,
        String email,
        String bio,
        LocalDateTime createdAt,
        boolean emailVerificado,
        /** URL assinada de curta duração; nula sem foto. */
        String fotoUrl,
        /** Preferência da tela de configurações: lembretes por e-mail. */
        boolean receberLembretes
) {
    public static UsuarioResponse from(Usuario usuario) {
        return from(usuario, null);
    }

    public static UsuarioResponse from(Usuario usuario, String fotoUrl) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getBio(),
                usuario.getCreatedAt(),
                usuario.emailVerificado(),
                fotoUrl,
                usuario.recebeLembretes()
        );
    }
}
