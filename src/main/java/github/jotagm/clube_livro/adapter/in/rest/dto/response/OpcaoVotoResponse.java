package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;

import java.util.UUID;

public record OpcaoVotoResponse(
        UUID id,
        UUID votacaoId,
        UUID sugeridoPorId,
        String nomeSugeridoPor,
        String livroGoogleId,
        String livroTitulo,
        String livroCapaUrl
) {
    public static OpcaoVotoResponse from(OpcaoVoto opcaoVoto) {
        return new OpcaoVotoResponse(
                opcaoVoto.getId(),
                opcaoVoto.getVotacao().getId(),
                opcaoVoto.getSugeridoPor().getId(),
                opcaoVoto.getSugeridoPor().getNome(),
                opcaoVoto.getLivroGoogleId(),
                opcaoVoto.getLivroTitulo(),
                opcaoVoto.getLivroCapaUrl()
        );
    }
}
