package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Size;

/** Link do Spotify da leitura; nulo ou vazio remove. */
public record SpotifyRequest(@Size(max = 300) String url) {}
