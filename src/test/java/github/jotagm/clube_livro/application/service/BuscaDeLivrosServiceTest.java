package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.application.port.out.CatalogoDeLivros;
import github.jotagm.clube_livro.domain.exceptions.LivroExternoIndisponivelException;
import github.jotagm.clube_livro.domain.livro.CampoDeBusca;
import github.jotagm.clube_livro.domain.livro.ConsultaDeLivros;
import github.jotagm.clube_livro.domain.livro.FonteDeLivros;
import github.jotagm.clube_livro.domain.livro.LivroEncontrado;
import github.jotagm.clube_livro.domain.livro.ResultadoBuscaDeLivros;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuscaDeLivrosServiceTest {

    private static final ConsultaDeLivros DUNA = new ConsultaDeLivros("Duna", CampoDeBusca.TUDO, 0, 12, "pt");

    private CatalogoFalso google;
    private CatalogoFalso openLibrary;
    private RelogioAjustavel relogio;
    private BuscaDeLivrosService servico;

    @BeforeEach
    void preparar() {
        google = new CatalogoFalso(FonteDeLivros.GOOGLE_BOOKS);
        openLibrary = new CatalogoFalso(FonteDeLivros.OPEN_LIBRARY);
        relogio = new RelogioAjustavel();
        servico = new BuscaDeLivrosService(List.of(openLibrary, google), relogio);
    }

    @Test
    void googleRespondendoNaoChamaAReserva() {
        google.responde(com(FonteDeLivros.GOOGLE_BOOKS, "Duna"));

        ResultadoBuscaDeLivros r = servico.buscar(DUNA);

        assertThat(r.fonte()).isEqualTo(FonteDeLivros.GOOGLE_BOOKS);
        assertThat(openLibrary.chamadas).isZero();
    }

    @Test
    void googleForaDoArCaiNoOpenLibrary() {
        google.falha();
        openLibrary.responde(com(FonteDeLivros.OPEN_LIBRARY, "Duna"));

        ResultadoBuscaDeLivros r = servico.buscar(DUNA);

        assertThat(r.fonte()).isEqualTo(FonteDeLivros.OPEN_LIBRARY);
        assertThat(r.livros()).extracting(LivroEncontrado::titulo).containsExactly("Duna");
    }

    @Test
    void googleVazioNaPrimeiraPaginaTentaAReserva() {
        // O Google tem respondido 200 sem nada para títulos que existem.
        google.responde(vazio(FonteDeLivros.GOOGLE_BOOKS));
        openLibrary.responde(com(FonteDeLivros.OPEN_LIBRARY, "Duna"));

        assertThat(servico.buscar(DUNA).fonte()).isEqualTo(FonteDeLivros.OPEN_LIBRARY);
    }

    @Test
    void googleVazioNasPaginasSeguintesEhFimDeLista() {
        google.responde(vazio(FonteDeLivros.GOOGLE_BOOKS));

        ResultadoBuscaDeLivros r = servico.buscar(new ConsultaDeLivros("Duna", CampoDeBusca.TUDO, 3, 12, "pt"));

        assertThat(r.vazio()).isTrue();
        assertThat(openLibrary.chamadas).isZero();
    }

    @Test
    void osDoisVaziosEhNadaEncontradoEnaoErro() {
        google.responde(vazio(FonteDeLivros.GOOGLE_BOOKS));
        openLibrary.responde(vazio(FonteDeLivros.OPEN_LIBRARY));

        assertThat(servico.buscar(DUNA).vazio()).isTrue();
    }

    @Test
    void googleVazioEReservaForaDoArDevolveOVazio() {
        google.responde(vazio(FonteDeLivros.GOOGLE_BOOKS));
        openLibrary.falha();

        assertThat(servico.buscar(DUNA).vazio()).isTrue();
    }

    @Test
    void osDoisForaDoArSemCacheEhErro() {
        google.falha();
        openLibrary.falha();

        assertThatThrownBy(() -> servico.buscar(DUNA)).isInstanceOf(LivroExternoIndisponivelException.class);
    }

    @Test
    void buscaRepetidaVemDoCache() {
        google.responde(com(FonteDeLivros.GOOGLE_BOOKS, "Duna"));

        servico.buscar(DUNA);
        relogio.avancar(Duration.ofMinutes(10));
        // Maiúsculas e espaços não fazem outra consulta.
        servico.buscar(new ConsultaDeLivros("  duna ", CampoDeBusca.TUDO, 0, 12, "PT"));

        assertThat(google.chamadas).isEqualTo(1);
    }

    @Test
    void cacheVencidoConsultaDeNovo() {
        google.responde(com(FonteDeLivros.GOOGLE_BOOKS, "Duna"));
        google.responde(com(FonteDeLivros.GOOGLE_BOOKS, "Duna"));

        servico.buscar(DUNA);
        relogio.avancar(Duration.ofMinutes(16));
        servico.buscar(DUNA);

        assertThat(google.chamadas).isEqualTo(2);
    }

    @Test
    void comTudoForaDoArServeOCacheAntigoComoDesatualizado() {
        google.responde(com(FonteDeLivros.GOOGLE_BOOKS, "Duna"));
        servico.buscar(DUNA);

        relogio.avancar(Duration.ofHours(2));
        google.falha();
        openLibrary.falha();
        ResultadoBuscaDeLivros r = servico.buscar(DUNA);

        assertThat(r.desatualizado()).isTrue();
        assertThat(r.livros()).extracting(LivroEncontrado::titulo).containsExactly("Duna");
    }

    @Test
    void cacheDeMaisDeUmDiaNaoServeMais() {
        google.responde(com(FonteDeLivros.GOOGLE_BOOKS, "Duna"));
        servico.buscar(DUNA);

        relogio.avancar(Duration.ofHours(25));
        google.falha();
        openLibrary.falha();

        assertThatThrownBy(() -> servico.buscar(DUNA)).isInstanceOf(LivroExternoIndisponivelException.class);
    }

    // --- apoio ------------------------------------------------------------------------

    private static ResultadoBuscaDeLivros com(FonteDeLivros fonte, String titulo) {
        LivroEncontrado livro = new LivroEncontrado("id-" + titulo, titulo, null, List.of("Autor"), null, null,
                null, null, "pt", null, null, null, fonte);
        return ResultadoBuscaDeLivros.de(1, List.of(livro), fonte);
    }

    private static ResultadoBuscaDeLivros vazio(FonteDeLivros fonte) {
        return ResultadoBuscaDeLivros.de(0, List.of(), fonte);
    }

    /** Responde, em ordem, o que foi programado; sem programação, falha. */
    private static final class CatalogoFalso implements CatalogoDeLivros {
        private final FonteDeLivros fonte;
        private final Deque<ResultadoBuscaDeLivros> respostas = new ArrayDeque<>();
        private boolean foraDoAr;
        int chamadas;

        CatalogoFalso(FonteDeLivros fonte) {
            this.fonte = fonte;
        }

        void responde(ResultadoBuscaDeLivros r) {
            foraDoAr = false;
            respostas.add(r);
        }

        void falha() {
            foraDoAr = true;
        }

        @Override
        public FonteDeLivros fonte() {
            return fonte;
        }

        @Override
        public ResultadoBuscaDeLivros buscar(ConsultaDeLivros consulta) {
            chamadas++;
            if (foraDoAr || respostas.isEmpty()) {
                throw new LivroExternoIndisponivelException("fora do ar");
            }
            return respostas.poll();
        }
    }

    private static final class RelogioAjustavel extends Clock {
        private Instant agora = Instant.parse("2026-10-24T12:00:00Z");

        void avancar(Duration d) {
            agora = agora.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
