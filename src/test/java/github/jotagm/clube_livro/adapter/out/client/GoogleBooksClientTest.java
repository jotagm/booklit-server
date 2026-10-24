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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Cobre a query que sai para o Google Books, as novas tentativas e a conversão do volume.
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
        ReflectionTestUtils.setField(client, "pausasMs", new long[]{0, 0});
    }

    private ResultadoBuscaDeLivros buscar(String termo, CampoDeBusca campo, int pagina, String idioma) {
        return client.buscar(new ConsultaDeLivros(termo, campo, pagina, 10, idioma));
    }

    @Test
    void comIdiomaRestringeABuscaAoIdiomaPedido() {
        servidor.expect(requestTo(Matchers.containsString("langRestrict=pt")))
                .andExpect(queryParam("q", "Duna"))
                .andExpect(queryParam("langRestrict", "pt"))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        assertThat(buscar("Duna", CampoDeBusca.TUDO, 0, "pt").vazio()).isTrue();
        servidor.verify();
    }

    @Test
    void semIdiomaNaoEnviaLangRestrict() {
        // Enviar langRestrict vazio faria o Google filtrar por um idioma inexistente e
        // devolver zero resultados — por isso o parâmetro precisa sumir da query.
        servidor.expect(requestTo(Matchers.not(Matchers.containsString("langRestrict"))))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        buscar("Dune", CampoDeBusca.TUDO, 0, "   ");

        servidor.verify();
    }

    @Test
    void pedeAsEdicoesDoBrasil() {
        servidor.expect(queryParam("country", "BR"))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        buscar("Duna", CampoDeBusca.TUDO, 0, "pt");

        servidor.verify();
    }

    @Test
    void tituloComAcentoVaiCodificadoEmUtf8() {
        // "coração" precisa chegar como %C3%A7%C3%A3o; em latin-1 o Google devolveria lixo.
        servidor.expect(requestTo(Matchers.containsString("q=cora%C3%A7%C3%A3o")))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        buscar("coração", CampoDeBusca.TUDO, 0, "pt");

        servidor.verify();
    }

    @Test
    void paginacaoViraStartIndexEmItens() {
        // page é índice de página, não de item: a página 2 começa no item 20.
        servidor.expect(queryParam("startIndex", "20"))
                .andExpect(queryParam("maxResults", "10"))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        buscar("Duna", CampoDeBusca.TUDO, 2, "pt");

        servidor.verify();
    }

    @Test
    void buscaPorAutorUsaOperadorComAspas() {
        // Sem as aspas, "inauthor:Ursula K. Le Guin" aplicaria o operador só em "Ursula" e
        // trataria o resto como busca livre, trazendo qualquer livro que cite o nome.
        // O matcher compara a query crua: as aspas viajam como %22 e os espaços como %20.
        servidor.expect(queryParam("q", "inauthor:%22Ursula%20K.%20Le%20Guin%22"))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        buscar("Ursula K. Le Guin", CampoDeBusca.AUTOR, 0, "pt");

        servidor.verify();
    }

    @Test
    void buscaPorTituloUsaIntitle() {
        servidor.expect(queryParam("q", "intitle:%22Duna%22"))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        buscar("  Duna  ", CampoDeBusca.TITULO, 0, "pt");

        servidor.verify();
    }

    @Test
    void isbnPerdeHifensEVaiSemAspas() {
        // O Google não aceita o ISBN entre aspas, e o código costuma ser copiado com hífens.
        servidor.expect(queryParam("q", "isbn:9788576573180"))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        buscar("978-85-7657-318-0", CampoDeBusca.ISBN, 0, null);

        servidor.verify();
    }

    @Test
    void isbnDigitadoEmTudoViraBuscaPorIsbn() {
        servidor.expect(queryParam("q", "isbn:9788576573180"))
                .andRespond(withSuccess(CORPO_VAZIO, MediaType.APPLICATION_JSON));

        buscar("978-85-7657-318-0", CampoDeBusca.TUDO, 0, "pt");

        servidor.verify();
    }

    @Test
    void volumeViraLivroDoBooklit() {
        String corpo = """
                {"totalItems": 42, "items": [{"id": "abc", "volumeInfo": {
                  "title": "Duna", "authors": ["Frank Herbert"], "publisher": "Aleph",
                  "publishedDate": "2017-05", "pageCount": 680, "language": "pt",
                  "description": "Um planeta desértico.",
                  "industryIdentifiers": [{"type": "ISBN_10", "identifier": "8576573130"},
                                          {"type": "ISBN_13", "identifier": "9788576573135"}],
                  "imageLinks": {"thumbnail": "http://books.google.com/capa?id=abc&edge=curl"}}},
                  {"id": "sem-titulo", "volumeInfo": {}}]}
                """;
        servidor.expect(queryParam("q", "Duna"))
                .andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));

        ResultadoBuscaDeLivros resultado = buscar("Duna", CampoDeBusca.TUDO, 0, "pt");

        assertThat(resultado.total()).isEqualTo(42);
        assertThat(resultado.fonte()).isEqualTo(FonteDeLivros.GOOGLE_BOOKS);
        // O volume sem título não serve para nada na lista.
        assertThat(resultado.livros()).hasSize(1);
        LivroEncontrado livro = resultado.livros().getFirst();
        assertThat(livro.id()).isEqualTo("abc");
        assertThat(livro.autores()).containsExactly("Frank Herbert");
        assertThat(livro.editora()).isEqualTo("Aleph");
        assertThat(livro.dataPublicacao()).isEqualTo("2017-05");
        assertThat(livro.paginas()).isEqualTo(680);
        assertThat(livro.descricao()).isEqualTo("Um planeta desértico.");
        assertThat(livro.isbn()).isEqualTo("9788576573135");
        // http seria bloqueado num site https; a "orelha" desenhada some.
        assertThat(livro.capaUrl()).isEqualTo("https://books.google.com/capa?id=abc");
    }

    @Test
    void tentaDeNovoQuandoOGoogleDa503() {
        servidor.expect(ExpectedCount.times(2), requestTo(Matchers.containsString("volumes")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        servidor.expect(requestTo(Matchers.containsString("volumes")))
                .andRespond(withSuccess("{\"totalItems\":1,\"items\":[{\"id\":\"x\",\"volumeInfo\":{\"title\":\"Duna\"}}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(buscar("Duna", CampoDeBusca.TUDO, 0, "pt").livros()).hasSize(1);
        servidor.verify();
    }

    @Test
    void desisteDepoisDasTentativas() {
        servidor.expect(ExpectedCount.times(3), requestTo(Matchers.containsString("volumes")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> buscar("Duna", CampoDeBusca.TUDO, 0, "pt"))
                .isInstanceOf(LivroExternoIndisponivelException.class);
        servidor.verify();
    }

    @Test
    void erroDoClienteNaoTentaDeNovo() {
        // 400 (chave inválida) e 429 (cota) não melhoram tentando de novo em seguida.
        servidor.expect(ExpectedCount.once(), requestTo(Matchers.containsString("volumes")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> buscar("Duna", CampoDeBusca.TUDO, 0, "pt"))
                .isInstanceOf(LivroExternoIndisponivelException.class);
        servidor.verify();
    }
}
