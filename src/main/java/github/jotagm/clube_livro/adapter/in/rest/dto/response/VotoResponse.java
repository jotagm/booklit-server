package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.votacao.Voto;

import java.util.UUID;

public record VotoResponse(
        UUID id,
        UUID votacaoId,
        UUID opcaoVotoId,
        String livroTitulo,
        UUID usuarioId,
        String nomeUsuario,
        int peso
) {
    public static VotoResponse from(Voto voto) {
        return new VotoResponse(
                voto.getId(),
                voto.getVotacao().getId(),
                voto.getOpcaoVoto().getId(),
                voto.getOpcaoVoto().getLivroTitulo(),
                voto.getUsuario().getId(),
                voto.getUsuario().getNome(),
                voto.getPeso()
        );
    }
}
