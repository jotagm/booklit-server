package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import github.jotagm.clube_livro.domain.clube.leitura.TipoMeta;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeituraClubeRequest(
        @NotNull
        UUID clubeId,

        @NotBlank
        String livroGoogleId,

        @NotBlank
        String livroTitulo,

        String livroCapaUrl,

        @NotNull
        TipoMeta tipoMeta,

        @Min(1)
        int valorMeta,

        @NotNull
        @Nullable
        LocalDateTime dataInicio,

        @NotNull
        @Nullable
        LocalDateTime dataFim
) {}
