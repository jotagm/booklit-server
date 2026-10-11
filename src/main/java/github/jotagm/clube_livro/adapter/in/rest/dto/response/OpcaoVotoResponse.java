package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;

import java.util.UUID;

public record OpcaoVotoResponse(
        UUID id,
        UUID votacaoId,
        Integer sugeridoPorId,
        String nomeSugeridoPor,
        String livroGoogleId,
        String livroTitulo,
        String livroCapaUrl,
        /** Quantos membros do clube têm este livro no "quero ler" da estante pessoal. */
        long membrosQueremLer
) {
    public static OpcaoVotoResponse from(OpcaoVoto opcaoVoto) {
        return from(opcaoVoto, 0);
    }

    public static OpcaoVotoResponse from(OpcaoVoto opcaoVoto, long membrosQueremLer) {
        return new OpcaoVotoResponse(
                opcaoVoto.getId(),
                opcaoVoto.getVotacao().getId(),
                opcaoVoto.getSugeridoPor().getId(),
                opcaoVoto.getSugeridoPor().getNome(),
                opcaoVoto.getLivroGoogleId(),
                opcaoVoto.getLivroTitulo(),
                opcaoVoto.getLivroCapaUrl(),
                membrosQueremLer
        );
    }
}
