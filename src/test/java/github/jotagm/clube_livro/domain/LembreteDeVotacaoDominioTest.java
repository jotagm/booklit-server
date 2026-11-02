package github.jotagm.clube_livro.domain;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.ComentarioResponse;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.leitura.Comentario;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus;
import github.jotagm.clube_livro.domain.exceptions.RegraDeNegocioException;
import github.jotagm.clube_livro.domain.exceptions.VotacaoJaEncerradaException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras de domínio da Fase 3: lembrete de votação, autor do livro e página do comentário. */
class LembreteDeVotacaoDominioTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 2, 10, 0);

    private Votacao aberta() {
        return Votacao.abrir(new Clube(), AGORA.minusDays(1), AGORA.plusDays(2));
    }

    @Test
    void primeiroLembreteRegistraOMomento() {
        Votacao votacao = aberta();

        votacao.registrarLembrete(AGORA);

        assertThat(votacao.getUltimoLembreteEm()).isEqualTo(AGORA);
    }

    @Test
    void segundoLembreteAntesDe24hEhRecusado() {
        Votacao votacao = aberta();
        votacao.registrarLembrete(AGORA);

        assertThatThrownBy(() -> votacao.registrarLembrete(AGORA.plusHours(23).plusMinutes(59)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("24 horas");
        assertThat(votacao.getUltimoLembreteEm()).isEqualTo(AGORA);
    }

    @Test
    void depoisDe24hPodeLembrarDeNovo() {
        Votacao votacao = aberta();
        votacao.registrarLembrete(AGORA);

        votacao.registrarLembrete(AGORA.plusHours(24));

        assertThat(votacao.getUltimoLembreteEm()).isEqualTo(AGORA.plusHours(24));
    }

    @Test
    void votacaoEncerradaNaoTemLembrete() {
        Votacao votacao = Votacao.builder().status(VotacaoStatus.ENCERRADA).build();

        assertThatThrownBy(() -> votacao.registrarLembrete(AGORA)).isInstanceOf(VotacaoJaEncerradaException.class);
    }

    @Test
    void autorEhNormalizado() {
        assertThat(LeituraClube.normalizarAutor("  Clarice \n Lispector ")).isEqualTo("Clarice Lispector");
        assertThat(LeituraClube.normalizarAutor("   ")).isNull();
        assertThat(LeituraClube.normalizarAutor(null)).isNull();
        assertThat(LeituraClube.normalizarAutor("x".repeat(300))).hasSize(255);
    }

    @Test
    void leituraVencedoraLevaOAutorDaOpcao() {
        LeituraClube leitura = LeituraClube.doLivroVencedor(new Clube(), "g1", "Torto Arado", null, "Itamar Vieira Junior");

        assertThat(leitura.getLivroAutor()).isEqualTo("Itamar Vieira Junior");
    }

    @Test
    void comentarioRemovidoNaoExpoeAPagina() {
        LeituraClube leitura = LeituraClube.builder().id(UUID.randomUUID()).build();
        Usuario autor = Usuario.builder().id(1).nome("Ana").build();
        Comentario comentario = Comentario.builder()
                .id(UUID.randomUUID()).leituraClube(leitura).usuario(autor)
                .conteudo("spoiler").paginaAutor(64).removido(false).build();

        assertThat(ComentarioResponse.deFolha(comentario).paginaAutor()).isEqualTo(64);

        comentario.remover();

        assertThat(ComentarioResponse.deFolha(comentario).paginaAutor()).isNull();
    }
}
