package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Confirmação depois do envio: a chave devolvida por {@code POST /usuarios/me/foto/envio}. */
public record FotoConfirmacaoRequest(@NotBlank String chave) {}
