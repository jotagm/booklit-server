package github.jotagm.clube_livro.domain;

import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.leitura.LinkSpotify;
import github.jotagm.clube_livro.domain.estante.LeituraPessoal;
import github.jotagm.clube_livro.domain.estante.MetaLeitura;
import github.jotagm.clube_livro.domain.estante.StatusLeituraPessoal;
import github.jotagm.clube_livro.domain.exceptions.RegraDeNegocioException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstanteDominioTest {

    private final LocalDate hoje = LocalDate.of(2026, 10, 10);

    private LeituraPessoal livro(Integer totalPaginas, StatusLeituraPessoal status) {
        return LeituraPessoal.adicionar(new Usuario(), "g-1", "Duna", "Frank Herbert", null,
                totalPaginas, status, hoje);
    }

    @Test
    void adicionarComoQueroLerNaoTemDatas() {
        LeituraPessoal l = livro(400, StatusLeituraPessoal.QUERO_LER);
        assertThat(l.getStatus()).isEqualTo(StatusLeituraPessoal.QUERO_LER);
        assertThat(l.getDataInicio()).isNull();
        assertThat(l.getPaginaAtual()).isZero();
    }

    @Test
    void avancarPaginaComecaALeitura() {
        LeituraPessoal l = livro(400, StatusLeituraPessoal.QUERO_LER);
        l.atualizarProgresso(50, null, hoje);
        assertThat(l.getStatus()).isEqualTo(StatusLeituraPessoal.LENDO);
        assertThat(l.getDataInicio()).isEqualTo(hoje);
    }

    @Test
    void chegarNaUltimaPaginaConcluiOLivro() {
        LeituraPessoal l = livro(400, StatusLeituraPessoal.LENDO);
        l.atualizarProgresso(400, null, hoje.plusDays(9));
        assertThat(l.getStatus()).isEqualTo(StatusLeituraPessoal.LIDO);
        assertThat(l.getDataFim()).isEqualTo(hoje.plusDays(9));
        assertThat(l.concluidaNoAno(2026)).isTrue();
    }

    @Test
    void paginaAlemDoTotalERejeitada() {
        LeituraPessoal l = livro(400, StatusLeituraPessoal.LENDO);
        assertThatThrownBy(() -> l.atualizarProgresso(401, null, hoje))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void voltarParaQueroLerZeraALeitura() {
        LeituraPessoal l = livro(400, StatusLeituraPessoal.LENDO);
        l.atualizarProgresso(120, null, hoje);
        l.mudarStatus(StatusLeituraPessoal.QUERO_LER, hoje);
        assertThat(l.getPaginaAtual()).isZero();
        assertThat(l.getDataInicio()).isNull();
    }

    @Test
    void marcarComoLidoCompletaAsPaginas() {
        LeituraPessoal l = livro(300, StatusLeituraPessoal.LENDO);
        l.mudarStatus(StatusLeituraPessoal.LIDO, hoje);
        assertThat(l.getPaginaAtual()).isEqualTo(300);
    }

    @Test
    void notaForaDeUmACincoERejeitada() {
        LeituraPessoal l = livro(300, StatusLeituraPessoal.LIDO);
        assertThatThrownBy(() -> l.avaliar(6, "ótimo")).isInstanceOf(RegraDeNegocioException.class);
        l.avaliar(5, "  ótimo  ");
        assertThat(l.getResenha()).isEqualTo("ótimo");
    }

    @Test
    void metaPrecisaDeAoMenosUmLivro() {
        assertThatThrownBy(() -> MetaLeitura.definir(new Usuario(), 2027, 0))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void linkSpotifyENormalizado() {
        assertThat(LinkSpotify.normalizar("https://open.spotify.com/intl-pt/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc123"))
                .isEqualTo("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M");
        assertThat(LinkSpotify.normalizar("  ")).isNull();
    }

    @Test
    void linkQueNaoEDoSpotifyERejeitado() {
        assertThatThrownBy(() -> LinkSpotify.normalizar("https://evil.example.com/playlist/37i9dQZF1DXcBWIGoYBM5M"))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> LinkSpotify.normalizar("https://open.spotify.com/user/fulano"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void regenerarCodigoDeConviteTrocaOCodigo() {
        Clube clube = new Clube();
        String primeiro = clube.gerarCodigoConvite();
        String segundo = clube.gerarCodigoConvite();
        assertThat(primeiro).hasSize(10).isNotEqualTo(segundo);
        assertThat(clube.getCodigoConvite()).isEqualTo(segundo);
    }
}
