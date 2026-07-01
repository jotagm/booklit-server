package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import github.jotagm.clube_livro.domain.clube.ClubePapel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UsuarioClubeRequest(
        @NotNull UUID usuarioId,
        @NotNull UUID clubeId,
        @NotNull ClubePapel papel
) {}
