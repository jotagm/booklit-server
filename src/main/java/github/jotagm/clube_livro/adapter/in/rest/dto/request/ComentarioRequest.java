package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ComentarioRequest(
        UUID comentarioPaiId,
        @NotBlank String conteudo
) {
}
