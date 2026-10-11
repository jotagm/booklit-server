package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Min;

/** Página em que a pessoa está; {@code totalPaginas} opcional corrige/define o tamanho do livro. */
public record ProgressoPessoalRequest(
        @Min(0) int paginaAtual,
        @Min(1) Integer totalPaginas
) {}
