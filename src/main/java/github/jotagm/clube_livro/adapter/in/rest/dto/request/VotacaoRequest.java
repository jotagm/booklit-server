package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record VotacaoRequest(
        @NotNull UUID clubeId,
        @NotNull LocalDateTime dataAbertura,
        @NotNull LocalDateTime dataEncerramento
) {}
