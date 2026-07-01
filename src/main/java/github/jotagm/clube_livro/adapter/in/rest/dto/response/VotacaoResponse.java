package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record VotacaoResponse(
        UUID id,
        UUID clubeId,
        String nomeClube,
        VotacaoStatus status,
        LocalDateTime dataAbertura,
        LocalDateTime dataEncerramento
) {
    public static VotacaoResponse from(Votacao votacao) {
        return new VotacaoResponse(
                votacao.getId(),
                votacao.getClube().getId(),
                votacao.getClube().getNome(),
                votacao.getStatus(),
                votacao.getDataAbertura(),
                votacao.getDataEncerramento()
        );
    }
}
