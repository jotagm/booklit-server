package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TemaRequest(
        @NotBlank String nome
) {}
