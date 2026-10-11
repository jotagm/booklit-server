package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.estante.LeituraPessoal;
import github.jotagm.clube_livro.domain.estante.StatusLeituraPessoal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record LeituraPessoalResponse(
        UUID id,
        String livroGoogleId,
        String livroTitulo,
        String livroAutor,
        String livroCapaUrl,
        StatusLeituraPessoal status,
        Integer totalPaginas,
        int paginaAtual,
        Integer percentual,
        Integer nota,
        String resenha,
        LocalDate dataInicio,
        LocalDate dataFim,
        UUID clubeOrigemId,
        LocalDateTime updatedAt
) {
    public static LeituraPessoalResponse from(LeituraPessoal l) {
        Integer percentual = l.getTotalPaginas() == null || l.getTotalPaginas() == 0 ? null
                : Math.min(100, Math.round(l.getPaginaAtual() * 100f / l.getTotalPaginas()));
        return new LeituraPessoalResponse(
                l.getId(), l.getLivroGoogleId(), l.getLivroTitulo(), l.getLivroAutor(), l.getLivroCapaUrl(),
                l.getStatus(), l.getTotalPaginas(), l.getPaginaAtual(), percentual, l.getNota(), l.getResenha(),
                l.getDataInicio(), l.getDataFim(), l.getClubeOrigemId(), l.getUpdatedAt());
    }
}
