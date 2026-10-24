package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record LoginCodigoRequest(
        @NotNull
        UUID desafioId,
        @NotBlank @Size(max = 32)
        String codigo
) {}
