package github.jotagm.clube_livro.adapter.in.rest.dto.response;

/** Números do ano para o topo da estante: meta, quanto já foi lido e o que está em andamento. */
public record EstanteResumoResponse(
        int ano,
        Integer metaLivros,
        long lidosNoAno,
        long lendo,
        long queroLer
) {}
