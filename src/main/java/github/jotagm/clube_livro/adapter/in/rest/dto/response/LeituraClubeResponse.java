package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.leitura.TipoMeta;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeituraClubeResponse(
        UUID id,
        UUID clubeId,
        String nomeClube,
        String livroGoogleId,
        String livroTitulo,
        String livroCapaUrl,
        TipoMeta tipoMeta,
        int valorMeta,
        LocalDateTime dataInicio,
        LocalDateTime dataFim
) {
    public static LeituraClubeResponse from(LeituraClube leitura) {
        return new LeituraClubeResponse(
                leitura.getId(),
                leitura.getClube().getId(),
                leitura.getClube().getNome(),
                leitura.getLivroGoogleId(),
                leitura.getLivroTitulo(),
                leitura.getLivroCapaUrl(),
                leitura.getTipoMeta(),
                leitura.getValorMeta(),
                leitura.getDataInicio(),
                leitura.getDataFim()
        );
    }
}
