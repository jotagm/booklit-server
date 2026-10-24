package github.jotagm.clube_livro.adapter.out.client;

import github.jotagm.clube_livro.domain.exceptions.LivroExternoIndisponivelException;
import github.jotagm.clube_livro.domain.livro.CampoDeBusca;
import github.jotagm.clube_livro.domain.livro.ConsultaDeLivros;
import github.jotagm.clube_livro.domain.livro.FonteDeLivros;
import github.jotagm.clube_livro.domain.livro.LivroEncontrado;
import github.jotagm.clube_livro.domain.livro.ResultadoBuscaDeLivros;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenLibraryClientTest {

    private static final String VAZIO = "{\"numFound\":0,\"docs\":[]}";

    private OpenLibraryClient client;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        client = new OpenLibraryClient(builder.build());
    }

    @Test
    void identificaOAppEPaginaAPartirDeUm() {
        // Sem User-Agent o Open Library limita a 1 requisição/s; a página dele começa em 1.
        servidor.expect(requestTo(Matchers.startsWith("https://openlibrary.org/search.json")))
                .andExpect(header(HttpHeaders.USER_AGENT, OpenLibraryClient.USER_AGENT))
                .andExpect(queryParam("page", "3"))
                .andExpect(queryParam("limit", "12"))
                .andRespond(withSuccess(VAZIO, MediaType.APPLICATION_JSON));

        client.buscar(new ConsultaDeLivros("Duna", CampoDeBusca.TUDO, 2, 12, null));

        servidor.verify();
    }

    @Test
    void consultaUsaOsCamposEOIdiomaDoOpenLibrary() {
        assertThat(OpenLibraryClient.consultaPara(new ConsultaDeLivros("Duna", CampoDeBusca.TITULO, 0, 10, "pt")))
                .isEqualTo("title:\"Duna\" language:por");
        assertThat(OpenLibraryClient.consultaPara(new ConsultaDeLivros("Le Guin", CampoDeBusca.AUTOR, 0, 10, null)))
                .isEqualTo("author:\"Le Guin\"");
        // ISBN já identifica a edição; filtrar idioma só esconderia o livro.
        assertThat(OpenLibraryClient.consultaPara(new ConsultaDeLivros("978-85-7657-313-5", CampoDeBusca.ISBN, 0, 10, "pt")))
                .isEqualTo("isbn:9788576573135");
        // Idioma que não está no mapa: sem filtro, em vez de um filtro errado.
        assertThat(OpenLibraryClient.consultaPara(new ConsultaDeLivros("Duna", CampoDeBusca.TUDO, 0, 10, "xx")))
                .isEqualTo("Duna");
    }

    @Test
    void documentoViraLivroDoBooklit() {
        String corpo = """
                {"numFound": 7, "docs": [{
                  "key": "/works/OL893415W", "title": "Duna", "author_name": ["Frank Herbert"],
                  "publisher": ["Aleph", "Ace"], "first_publish_year": 1965,
                  "number_of_pages_median": 680, "language": ["eng", "por"],
                  "isbn": ["0441172717", "9780441172719", "9788576573135"], "cover_i": 12345},
                  {"key": "/works/OL1W"}]}
                """;
        servidor.expect(requestTo(Matchers.containsString("search.json")))
                .andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));

        ResultadoBuscaDeLivros resultado = client.buscar(new ConsultaDeLivros("Duna", CampoDeBusca.TUDO, 0, 12, "pt"));

        assertThat(resultado.total()).isEqualTo(7);
        assertThat(resultado.fonte()).isEqualTo(FonteDeLivros.OPEN_LIBRARY);
        assertThat(resultado.livros()).hasSize(1);
        LivroEncontrado livro = resultado.livros().getFirst();
        assertThat(livro.id()).isEqualTo("OL893415W");
        assertThat(livro.autores()).containsExactly("Frank Herbert");
        assertThat(livro.editora()).isEqualTo("Aleph");
        assertThat(livro.dataPublicacao()).isEqualTo("1965");
        assertThat(livro.idioma()).isEqualTo("pt");
        // Entre os ISBNs de todas as edições, prefere o brasileiro.
        assertThat(livro.isbn()).isEqualTo("9788576573135");
        assertThat(livro.capaUrl()).isEqualTo("https://covers.openlibrary.org/b/id/12345-M.jpg");
        assertThat(livro.linkInfo()).isEqualTo("https://openlibrary.org/works/OL893415W");
    }

    @Test
    void isbnSemBrasileiroPrefereOTreze() {
        assertThat(OpenLibraryClient.isbn(List.of("0441172717", "9780441172719"))).isEqualTo("9780441172719");
        assertThat(OpenLibraryClient.isbn(List.of("0441172717"))).isEqualTo("0441172717");
        assertThat(OpenLibraryClient.isbn(null)).isNull();
    }

    @Test
    void falhaViraExcecaoDeDominio() {
        servidor.expect(requestTo(Matchers.containsString("search.json"))).andRespond(withServerError());

        assertThatThrownBy(() -> client.buscar(new ConsultaDeLivros("Duna", CampoDeBusca.TUDO, 0, 12, null)))
                .isInstanceOf(LivroExternoIndisponivelException.class);
    }
}
