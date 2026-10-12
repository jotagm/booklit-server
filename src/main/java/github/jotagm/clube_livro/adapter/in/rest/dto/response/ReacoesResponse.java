package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.feed.TipoReacao;

import java.util.Map;

/** Estado das reações de um item depois de uma mudança: contagem por tipo e a reação de quem pediu. */
public record ReacoesResponse(String alvoId, Map<TipoReacao, Long> reacoes, TipoReacao minhaReacao) {}
