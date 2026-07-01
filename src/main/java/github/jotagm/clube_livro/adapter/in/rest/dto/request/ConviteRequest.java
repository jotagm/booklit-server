package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConviteRequest(
        @NotNull UUID clubeId,
        @NotBlank @Email String emailDestinatario,
        @NotNull LocalDateTime expiraEm
) {}
