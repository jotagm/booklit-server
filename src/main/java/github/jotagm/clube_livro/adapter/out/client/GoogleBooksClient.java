package github.jotagm.clube_livro.adapter.out.client;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.GoogleBooksResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.GoogleBooksResponse.Item.VolumeInfo;
import github.jotagm.clube_livro.application.port.out.CatalogoDeLivros;
import github.jotagm.clube_livro.domain.exceptions.LivroExternoIndisponivelException;
import github.jotagm.clube_livro.domain.livro.CampoDeBusca;
import github.jotagm.clube_livro.domain.livro.ConsultaDeLivros;
import github.jotagm.clube_livro.domain.livro.FonteDeLivros;
import github.jotagm.clube_livro.domain.livro.LivroEncontrado;
import github.jotagm.clube_livro.domain.livro.ResultadoBuscaDeLivros;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Catálogo principal: a API pública do Google Books.
 *
 * <p>Em 2026 ela anda instável (503 "backendFailed" em boa parte das chamadas, para qualquer
 * chave e projeto). Por isso cada busca tenta de novo nos erros 5xx e de rede, com pausas
 * curtas, antes de desistir; quem chama ({@code BuscaDeLivrosService}) cai no Open Library.
 */
@Slf4j
@Component
public class GoogleBooksClient implements CatalogoDeLivros {

    @Value("${google.books.api-key}")
    private String apiKey;

    @Value("${google.books.url}")
    private String url;

    /** Pausas entre as tentativas; o tamanho define quantas tentativas extras há. */
    private long[] pausasMs = {300, 800};

    private final RestClient restClient;

    public GoogleBooksClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public FonteDeLivros fonte() {
        return FonteDeLivros.GOOGLE_BOOKS;
    }

    @Override
    public ResultadoBuscaDeLivros buscar(ConsultaDeLivros consulta) {
        GoogleBooksResponse resposta = consultar(consulta);
        List<LivroEncontrado> livros = resposta.items() == null ? List.of()
                : resposta.items().stream().map(GoogleBooksClient::livro).filter(Objects::nonNull).toList();
        int total = resposta.totalItems() == null ? livros.size() : resposta.totalItems();
        return ResultadoBuscaDeLivros.de(total, livros, FonteDeLivros.GOOGLE_BOOKS);
    }

    GoogleBooksResponse consultar(ConsultaDeLivros consulta) {
        String q = consultaPara(consulta.campo(), consulta.termo());

        // Montar a URI aqui, em vez de concatenar o template, é o que permite omitir o
        // langRestrict quando ninguém pediu idioma - e deixa o encode de acento explícito.
        URI uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("q", q)
                .queryParam("startIndex", consulta.pagina() * consulta.tamanho())
                .queryParam("maxResults", consulta.tamanho())
                .queryParam("orderBy", "relevance")
                .queryParam("printType", "books")
                // Prioriza as edições vendidas no Brasil; segundo relatos de outros projetos,
                // também reduz os 503 da API.
                .queryParam("country", "BR")
                .queryParamIfPresent("langRestrict", Optional.ofNullable(consulta.idioma()))
                .queryParam("key", apiKey)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        for (int tentativa = 0; ; tentativa++) {
            try {
                GoogleBooksResponse resposta = restClient.get().uri(uri).retrieve().body(GoogleBooksResponse.class);
                if (resposta == null) {
                    throw new LivroExternoIndisponivelException("A busca de livros não retornou uma resposta válida");
                }
                int encontrados = resposta.items() == null ? 0 : resposta.items().size();
                log.info("Google Books: q='{}' idioma={} pagina={} retornou {} de {} resultado(s){}",
                        q, consulta.idioma() == null ? "todos" : consulta.idioma(), consulta.pagina(), encontrados,
                        resposta.totalItems() == null ? "?" : resposta.totalItems(),
                        tentativa > 0 ? " na tentativa " + (tentativa + 1) : "");
                return resposta;
            } catch (HttpServerErrorException | ResourceAccessException ex) {
                // 5xx e falha de rede costumam passar em instantes; 4xx (chave, cota) não.
                if (tentativa >= pausasMs.length) {
                    log.warn("Google Books indisponível para q='{}' após {} tentativas: {}", q, tentativa + 1,
                            resumo(ex));
                    throw new LivroExternoIndisponivelException("Não foi possível consultar a busca de livros no momento");
                }
                log.info("Google Books falhou para q='{}' ({}); tentando de novo", q, resumo(ex));
                pausar(pausasMs[tentativa]);
            } catch (RestClientException ex) {
                log.error("Falha ao consultar a API do Google Books para q='{}'", q, ex);
                throw new LivroExternoIndisponivelException("Não foi possível consultar a busca de livros no momento");
            }
        }
    }

    /**
     * Valor do parâmetro {@code q}.
     *
     * <p>As aspas prendem o termo ao operador: sem elas, {@code inauthor:Ursula K. Le Guin}
     * aplicaria o operador só em "Ursula" e trataria o resto como busca livre. O ISBN é a
     * exceção — o Google não aceita o código entre aspas.
     */
    static String consultaPara(CampoDeBusca campo, String termo) {
        String entreAspas = "\"" + termo.replace("\"", "") + "\"";
        return switch (campo) {
            case TUDO -> termo;
            case TITULO -> "intitle:" + entreAspas;
            case AUTOR -> "inauthor:" + entreAspas;
            case EDITORA -> "inpublisher:" + entreAspas;
            case ISBN -> "isbn:" + CampoDeBusca.somenteIsbn(termo);
        };
    }

    static LivroEncontrado livro(GoogleBooksResponse.Item item) {
        VolumeInfo v = item.volumeInfo();
        if (v == null || v.title() == null || v.title().isBlank()) return null;
        return new LivroEncontrado(item.id(), v.title(), v.subtitle(), v.authors(), v.publisher(),
                v.publishedDate(), v.description(), v.pageCount(), v.language(), isbn(v), capa(v),
                v.infoLink(), FonteDeLivros.GOOGLE_BOOKS);
    }

    private static String isbn(VolumeInfo v) {
        if (v.industryIdentifiers() == null) return null;
        return v.industryIdentifiers().stream().filter(i -> "ISBN_13".equals(i.type())).findFirst()
                .or(() -> v.industryIdentifiers().stream().filter(i -> "ISBN_10".equals(i.type())).findFirst())
                .map(VolumeInfo.IndustryIdentifier::identifier)
                .orElse(null);
    }

    /** O Google manda a capa em http (bloqueada num site https) e com a "orelha" desenhada. */
    static String capa(VolumeInfo v) {
        if (v.imageLinks() == null) return null;
        String capa = Optional.ofNullable(v.imageLinks().thumbnail()).orElse(v.imageLinks().smallThumbnail());
        if (capa == null) return null;
        return capa.replaceFirst("^http://", "https://").replace("&edge=curl", "");
    }

    private static String resumo(Exception ex) {
        return ex instanceof HttpServerErrorException h ? String.valueOf(h.getStatusCode().value())
                : ex.getClass().getSimpleName();
    }

    private static void pausar(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LivroExternoIndisponivelException("Busca de livros interrompida");
        }
    }
}
