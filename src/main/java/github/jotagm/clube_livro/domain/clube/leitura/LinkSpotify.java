package github.jotagm.clube_livro.domain.clube.leitura;

import github.jotagm.clube_livro.domain.exceptions.RegraDeNegocioException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Link do Spotify associado a uma leitura (trilha sonora, audiobook, podcast sobre o livro).
 *
 * <p>Usa só o player embutido do Spotify, sem a Web API: desde 2026 apps em Development Mode
 * ficam limitados a 5 usuários, então OAuth não serve para um produto aberto. O link é
 * normalizado para {@code https://open.spotify.com/<tipo>/<id>} — sem {@code intl-xx}, sem
 * {@code ?si=} de rastreio — e os clientes montam o embed trocando o host por
 * {@code open.spotify.com/embed}.
 */
public final class LinkSpotify {

    private static final Pattern FORMATO = Pattern.compile(
            "^https?://open\\.spotify\\.com/(?:intl-[a-zA-Z-]+/)?(playlist|album|track|episode|show|audiobook)/([A-Za-z0-9]{10,40})(?:[/?#].*)?$");

    private LinkSpotify() {}

    /** Devolve o link canônico, {@code null} para vazio, ou lança se não for um link do Spotify suportado. */
    public static String normalizar(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Matcher m = FORMATO.matcher(url.trim());
        if (!m.matches()) {
            throw new RegraDeNegocioException(
                    "Link do Spotify inválido: use um link de playlist, álbum, música, podcast ou audiobook (open.spotify.com)");
        }
        return "https://open.spotify.com/" + m.group(1) + "/" + m.group(2);
    }
}
