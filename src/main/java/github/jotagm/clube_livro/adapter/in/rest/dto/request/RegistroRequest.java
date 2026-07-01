package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Min;

public record RegistroRequest(
        @Min(0) int valorAtual
) {}
