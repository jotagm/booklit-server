package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.tema.Tema;

import java.util.UUID;

public record TemaResponse(
        UUID id,
        String nome
) {
    public static TemaResponse from(Tema tema) {
        return new TemaResponse(tema.getId(), tema.getNome());
    }
}
