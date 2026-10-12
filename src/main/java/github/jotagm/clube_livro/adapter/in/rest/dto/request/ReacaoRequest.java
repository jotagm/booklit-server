package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import github.jotagm.clube_livro.domain.feed.TipoReacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReacaoRequest(
        @NotNull UUID clubeId,
        @NotBlank @Size(max = 80) String alvoId,
        @NotNull TipoReacao tipo
) {}
