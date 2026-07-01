package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email
        String email,
        @NotBlank
        String senha
) {}
