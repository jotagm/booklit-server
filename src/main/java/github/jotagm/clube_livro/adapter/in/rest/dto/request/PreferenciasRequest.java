package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;

/** Tela de configurações → "preferências". */
public record PreferenciasRequest(@NotNull Boolean receberLembretes) {}
