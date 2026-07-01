package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.convite.Convite;
import github.jotagm.clube_livro.domain.clube.convite.ConviteStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConviteResponse(
        UUID id,
        UUID clubeId,
        String nomeClube,
        String nomeConvidadoPor,
        String emailDestinatario,
        ConviteStatus status,
        LocalDateTime expiraEm,
        LocalDateTime createdAt
) {
    public static ConviteResponse from(Convite convite) {
        return new ConviteResponse(
                convite.getId(),
                convite.getClube().getId(),
                convite.getClube().getNome(),
                convite.getConvidadoPor().getNome(),
                convite.getEmailDestinatario(),
                convite.getStatus(),
                convite.getExpiraEm(),
                convite.getCreatedAt()
        );
    }
}
