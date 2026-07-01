package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OpcaoVotoRequest(
        @NotNull UUID votacaoId,
        @NotNull UUID sugeridoPorId,
        @NotBlank String livroGoogleId,
        @NotBlank String livroTitulo,
        String livroCapaUrl
) {}
