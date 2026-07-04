package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VotoRequest(
        @NotNull UUID votacaoId,
        @NotNull UUID opcaoVotoId,
        @NotNull UUID usuarioId
) {}
