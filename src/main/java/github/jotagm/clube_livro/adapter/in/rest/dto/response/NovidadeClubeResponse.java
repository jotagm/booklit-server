package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import java.util.UUID;

/** Clube com atividade que o usuário ainda não viu (o ponto na lista de clubes). */
public record NovidadeClubeResponse(UUID clubeId, int quantidade) {
}
