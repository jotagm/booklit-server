package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import github.jotagm.clube_livro.domain.estante.StatusLeituraPessoal;
import jakarta.validation.constraints.NotNull;

public record StatusLeituraPessoalRequest(@NotNull StatusLeituraPessoal status) {}
