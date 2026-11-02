package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.leitura.Registro;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;
import github.jotagm.clube_livro.domain.usuario.Usuario;

public record RegistroResponse(
        UUID id,
        UUID leituraClubeId,
        String livroTitulo,
        Integer usuarioId,
        String nomeUsuario,
        String fotoUrl,
        int valorAtual,
        LocalDateTime updatedAt
) {
    public static RegistroResponse from(Registro registro) {
        return from(registro, u -> null);
    }

    public static RegistroResponse from(Registro registro, Function<Usuario, String> foto) {
        return new RegistroResponse(
                registro.getId(),
                registro.getLeituraClube().getId(),
                registro.getLeituraClube().getLivroTitulo(),
                registro.getUsuario().getId(),
                registro.getUsuario().getNome(),
                foto.apply(registro.getUsuario()),
                registro.getValorAtual(),
                registro.getUpdatedAt()
        );
    }
}
