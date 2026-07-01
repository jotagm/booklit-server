package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record ClubeRequest(
        @NotBlank String nome,
        String descricao,
        boolean privado,
        List<UUID> temaIds
) {}
