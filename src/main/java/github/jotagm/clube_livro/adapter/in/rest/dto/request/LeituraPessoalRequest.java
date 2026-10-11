package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import github.jotagm.clube_livro.domain.estante.StatusLeituraPessoal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Livro adicionado à estante. Sem {@code status}, entra como QUERO_LER. */
public record LeituraPessoalRequest(
        @NotBlank String livroGoogleId,
        @NotBlank String livroTitulo,
        String livroAutor,
        String livroCapaUrl,
        @Min(1) Integer totalPaginas,
        StatusLeituraPessoal status
) {}
