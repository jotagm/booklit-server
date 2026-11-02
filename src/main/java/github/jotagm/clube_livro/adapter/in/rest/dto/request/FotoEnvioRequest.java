package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Pedido de envio da foto: o tipo e o tamanho que o cliente vai mandar ao S3. */
public record FotoEnvioRequest(
        @NotBlank String contentType,
        @Positive long tamanho
) {}
