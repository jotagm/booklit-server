package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RedefinirSenhaRequest(
        @NotNull UUID desafioId,
        @Schema(description = "Os 6 dígitos do e-mail; espaços são ignorados")
        @NotBlank String codigo,
        @Schema(description = "Senha nova, com pelo menos 8 caracteres")
        @NotBlank @Size(min = 8, max = 72, message = "A senha precisa ter entre 8 e 72 caracteres")
        String novaSenha
) {}
