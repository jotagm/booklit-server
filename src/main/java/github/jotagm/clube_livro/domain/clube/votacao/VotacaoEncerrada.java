package github.jotagm.clube_livro.domain.clube.votacao;

import github.jotagm.clube_livro.domain.evento.EventoDeDominio;

import java.time.Instant;
import java.util.UUID;

public record VotacaoEncerrada(UUID eventId, Instant ocorridoEm, Dados dados) implements EventoDeDominio {

    public static final String TIPO = "votacao.encerrada";

    public record Dados(UUID votacaoId, UUID clubeId, UUID opcaoVencedoraId, UUID leituraClubeId,
                        String livroGoogleId, String livroTitulo) {
    }

    static VotacaoEncerrada de(Votacao votacao, OpcaoVoto vencedora, UUID leituraClubeId) {
        return new VotacaoEncerrada(
                UUID.randomUUID(),
                Instant.now(),
                new Dados(
                        votacao.getId(),
                        votacao.getClube().getId(),
                        vencedora.getId(),
                        leituraClubeId,
                        vencedora.getLivroGoogleId(),
                        vencedora.getLivroTitulo()));
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public int versao() {
        return 1;
    }

    @Override
    public UUID agregadoId() {
        return dados.votacaoId();
    }
}