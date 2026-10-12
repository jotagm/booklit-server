package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Edição do perfil: só o que é público. E-mail e senha seguem pelo PUT /usuarios/{id}. */
public record PerfilRequest(
        @NotBlank @Size(max = 80) String nome,
        @Size(max = 160) String bio
) {}
