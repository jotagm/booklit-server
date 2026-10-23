package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import github.jotagm.auth.otp.Finalidade;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OtpEmailRequest(
        @NotBlank @Email @Size(max = 254)
        String email,
        @NotNull
        Finalidade finalidade
) {}
