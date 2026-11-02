package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.application.port.out.ArmazenamentoDeFotos.EnvioAssinado;

import java.time.Instant;
import java.util.Map;

/**
 * Como enviar a foto: um PUT na {@code url}, com estes cabeçalhos e a imagem como corpo.
 * Depois, {@code PUT /usuarios/me/foto} com a {@code chave}.
 */
public record FotoEnvioResponse(String chave, String url, Map<String, String> cabecalhos, Instant expiraEm) {

    public static FotoEnvioResponse from(EnvioAssinado envio, String contentType) {
        return new FotoEnvioResponse(envio.chave(), envio.url(),
                Map.of("Content-Type", contentType.strip().toLowerCase(java.util.Locale.ROOT)), envio.expiraEm());
    }
}
