package github.jotagm.clube_livro.domain.livro;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultaDeLivrosTest {

    @Test
    void normalizaTermoIdiomaETamanho() {
        ConsultaDeLivros c = new ConsultaDeLivros("  o   nome  do vento ", null, -1, 500, " PT ");

        assertThat(c.termo()).isEqualTo("o nome do vento");
        assertThat(c.campo()).isEqualTo(CampoDeBusca.TUDO);
        assertThat(c.pagina()).isZero();
        assertThat(c.tamanho()).isEqualTo(ConsultaDeLivros.TAMANHO_MAXIMO);
        assertThat(c.idioma()).isEqualTo("pt");
    }

    @Test
    void idiomaEmBrancoEhTodos() {
        assertThat(new ConsultaDeLivros("Duna", CampoDeBusca.TUDO, 0, 10, "  ").idioma()).isNull();
    }

    @Test
    void isbnEmTudoViraIsbn() {
        assertThat(new ConsultaDeLivros("978-85-7657-313-5", CampoDeBusca.TUDO, 0, 10, null).campo())
                .isEqualTo(CampoDeBusca.ISBN);
        assertThat(new ConsultaDeLivros("857657313X", CampoDeBusca.TUDO, 0, 10, null).campo())
                .isEqualTo(CampoDeBusca.ISBN);
        // Um número qualquer (um ano, "1984") continua texto livre.
        assertThat(new ConsultaDeLivros("1984", CampoDeBusca.TUDO, 0, 10, null).campo())
                .isEqualTo(CampoDeBusca.TUDO);
    }

    @Test
    void termoVazioEhRecusado() {
        assertThatThrownBy(() -> new ConsultaDeLivros("  ", CampoDeBusca.TUDO, 0, 10, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void chaveDoCacheIgnoraMaiusculas() {
        assertThat(new ConsultaDeLivros("DUNA", null, 0, 10, "pt").normalizada())
                .isEqualTo(new ConsultaDeLivros("duna", null, 0, 10, "PT").normalizada());
    }
}
