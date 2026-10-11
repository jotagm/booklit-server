package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Nota de 1 a 5 e resenha curta, ambas opcionais (nulo limpa). */
public record AvaliacaoRequest(
        @Min(1) @Max(5) Integer nota,
        @Size(max = 5000) String resenha
) {}
