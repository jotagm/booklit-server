package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import github.jotagm.auth.otp.Finalidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** O formato do código ({@code 482913}, {@code 482 913}, {@code 482-913}) é validado no auth-core. */
public record VerificarOtpRequest(
        @NotNull
        UUID desafioId,
        @NotNull
        Finalidade finalidade,
        @NotBlank @Size(max = 32)
        String codigo
) {}
