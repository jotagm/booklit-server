package github.jotagm.clube_livro.adapter.out.client;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.GoogleBooksResponse;
import github.jotagm.clube_livro.domain.exceptions.LivroExternoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.hamcrest.Matchers;

/**
 * Cobre a query que sai para o Google Books.
 *
 * <p>Sem {@code langRestrict}, buscar "Duna" disputa relevância com o catálogo inteiro e as
 * edições em inglês ocupam as primeiras posições — a edição brasileira não aparecia na lista.
 */
class GoogleBooksClientTest {

    private static final String URL = "https://www.googleapis.com/books/v1/volumes";
    private static final String CORPO_VAZIO = "{\"items\":[]}";

    private GoogleBooksClient client;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();

        client = new GoogleBooksClient(builder.build());
        ReflectionTestUtils.setField(client, "url", URL);
        ReflectionTestUtils.setField(client, "apiKey", "chave-de-teste");
    }

    @Test
    void comIdiomaRestringeABuscaAoIdiomaPedido() {
        servidor.expect(requestTo(Matchers.containsString("langRestrict=pt")))
                .andExpect(queryParam("q", "Duna"))
                .andExpect(queryParam("langRestrict", "pt"))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        GoogleBooksResponse resposta = client.buscarLivroGoogleBooks("Duna", CampoDeBusca.TUDO, 0, 10, "pt");

        assertThat(resposta).isNotNull();
        servidor.verify();
    }

    @Test
    void semIdiomaNaoEnviaLangRestrict() {
        // Enviar langRestrict vazio faria o Google filtrar por um idioma inexistente e
        // devolver zero resultados — por isso o parâmetro precisa sumir da query.
        servidor.expect(requestTo(Matchers.not(Matchers.containsString("langRestrict"))))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        client.buscarLivroGoogleBooks("Dune", CampoDeBusca.TUDO, 0, 10, null);

        servidor.verify();
    }

    @Test
    void idiomaEmBrancoEquivaleASemIdioma() {
        servidor.expect(requestTo(Matchers.not(Matchers.containsString("langRestrict"))))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        client.buscarLivroGoogleBooks("Dune", CampoDeBusca.TUDO, 0, 10, "   ");

        servidor.verify();
    }

    @Test
    void tituloComAcentoVaiCodificadoEmUtf8() {
        // "coração" precisa chegar como %C3%A7%C3%A3o; em latin-1 o Google devolveria lixo.
        servidor.expect(requestTo(Matchers.containsString("q=cora%C3%A7%C3%A3o")))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        client.buscarLivroGoogleBooks("coração", CampoDeBusca.TUDO, 0, 10, "pt");

        servidor.verify();
    }

    @Test
    void paginacaoViraStartIndexEmItens() {
        // page é índice de página, não de item: a página 2 começa no item 20.
        servidor.expect(queryParam("startIndex", "20"))
                .andExpect(queryParam("maxResults", "10"))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        client.buscarLivroGoogleBooks("Duna", CampoDeBusca.TUDO, 2, 10, "pt");

        servidor.verify();
    }

    @Test
    void buscaPorAutorUsaOperadorComAspas() {
        // Sem as aspas, "inauthor:Ursula K. Le Guin" aplicaria o operador só em "Ursula" e
        // trataria o resto como busca livre, trazendo qualquer livro que cite o nome.
        // O matcher compara a query crua: as aspas viajam como %22 e os espaços como %20.
        servidor.expect(queryParam("q", "inauthor:%22Ursula%20K.%20Le%20Guin%22"))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        client.buscarLivroGoogleBooks("Ursula K. Le Guin", CampoDeBusca.AUTOR, 0, 10, "pt");

        servidor.verify();
    }

    @Test
    void buscaPorTituloUsaIntitle() {
        servidor.expect(queryParam("q", "intitle:%22Duna%22"))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        client.buscarLivroGoogleBooks("  Duna  ", CampoDeBusca.TITULO, 0, 10, "pt");

        servidor.verify();
    }

    @Test
    void isbnPerdeHifensEVaiSemAspas() {
        // O Google não aceita o ISBN entre aspas, e o código costuma ser copiado com hífens.
        servidor.expect(queryParam("q", "isbn:9788576573180"))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        client.buscarLivroGoogleBooks("978-85-7657-318-0", CampoDeBusca.ISBN, 0, 10, null);

        servidor.verify();
    }

    @Test
    void campoNuloEquivaleABuscarEmTudo() {
        servidor.expect(queryParam("q", "Duna"))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        client.buscarLivroGoogleBooks("Duna", null, 0, 10, null);

        servidor.verify();
    }

    @Test
    void camposExtrasDoVolumeChegamAoCliente() {
        // O record recorta a resposta do Google: o que não estiver declarado nele o front
        // nunca vê. Autor e ano são o que distingue duas edições do mesmo livro na lista.
        String corpo = """
                {"totalItems": 42, "items": [{"id": "abc", "volumeInfo": {
                  "title": "Duna", "authors": ["Frank Herbert"], "publisher": "Aleph",
                  "publishedDate": "2017-05", "pageCount": 680, "language": "pt",
                  "description": "Um planeta desértico.",
                  "imageLinks": {"thumbnail": "http://capa"}}}]}
                """;
        servidor.expect(queryParam("q", "Duna"))
                .andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));

        GoogleBooksResponse resposta = client.buscarLivroGoogleBooks("Duna", CampoDeBusca.TUDO, 0, 10, "pt");

        assertThat(resposta.totalItems()).isEqualTo(42);
        var volume = resposta.items().getFirst().volumeInfo();
        assertThat(volume.authors()).containsExactly("Frank Herbert");
        assertThat(volume.publisher()).isEqualTo("Aleph");
        assertThat(volume.publishedDate()).isEqualTo("2017-05");
        assertThat(volume.pageCount()).isEqualTo(680);
        assertThat(volume.description()).isEqualTo("Um planeta desértico.");
    }

    @Test
    void falhaDoGoogleViraExcecaoDeDominio() {
        servidor.expect(requestTo(Matchers.containsString("volumes")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.buscarLivroGoogleBooks("Duna", CampoDeBusca.TUDO, 0, 10, "pt"))
                .isInstanceOf(LivroExternoIndisponivelException.class);
    }
}
