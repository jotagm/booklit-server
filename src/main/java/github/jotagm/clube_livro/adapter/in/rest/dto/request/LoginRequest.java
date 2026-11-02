package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        // 254 é o máximo de um endereço de e-mail e o tamanho da coluna em t_falha_login.
        @NotBlank @Email @Size(max = 254)
        String email,
        @NotBlank
        String senha
) {}
