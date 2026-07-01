package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.leitura.Registro;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegistroResponse(
        UUID id,
        UUID leituraClubeId,
        String livroTitulo,
        UUID usuarioId,
        String nomeUsuario,
        int valorAtual,
        LocalDateTime updatedAt
) {
    public static RegistroResponse from(Registro registro) {
        return new RegistroResponse(
                registro.getId(),
                registro.getLeituraClube().getId(),
                registro.getLeituraClube().getLivroTitulo(),
                registro.getUsuario().getId(),
                registro.getUsuario().getNome(),
                registro.getValorAtual(),
                registro.getUpdatedAt()
        );
    }
}
