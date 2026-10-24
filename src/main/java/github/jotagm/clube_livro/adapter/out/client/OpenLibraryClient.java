package github.jotagm.clube_livro.adapter.out.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.jotagm.clube_livro.application.port.out.CatalogoDeLivros;
import github.jotagm.clube_livro.domain.exceptions.LivroExternoIndisponivelException;
import github.jotagm.clube_livro.domain.livro.CampoDeBusca;
import github.jotagm.clube_livro.domain.livro.ConsultaDeLivros;
import github.jotagm.clube_livro.domain.livro.FonteDeLivros;
import github.jotagm.clube_livro.domain.livro.LivroEncontrado;
import github.jotagm.clube_livro.domain.livro.ResultadoBuscaDeLivros;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Catálogo reserva: a busca do Open Library (Internet Archive). Gratuita e sem chave; pede
 * um User-Agent que identifique o app (com ele o limite sobe de 1 para 3 requisições/s).
 *
 * <p>O acervo em português é menor que o do Google e quase nunca traz descrição, por isso
 * ele só entra quando o Google falha ou não acha nada.
 */
@Slf4j
@Component
public class OpenLibraryClient implements CatalogoDeLivros {

    static final String USER_AGENT = "Booklit/1.0 (https://booklit.bio)";
    private static final String CAMPOS =
            "key,title,subtitle,author_name,publisher,first_publish_year,number_of_pages_median,language,isbn,cover_i";

    /** ISO-639-1, que o Booklit usa, para o código de três letras do Open Library. */
    private static final Map<String, String> IDIOMAS = Map.of(
            "pt", "por", "en", "eng", "es", "spa", "fr", "fre", "de", "ger", "it", "ita");

    @Value("${openlibrary.url:https://openlibrary.org/search.json}")
    private String url = "https://openlibrary.org/search.json";

    private final RestClient restClient;

    public OpenLibraryClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public FonteDeLivros fonte() {
        return FonteDeLivros.OPEN_LIBRARY;
    }

    @Override
    public ResultadoBuscaDeLivros buscar(ConsultaDeLivros consulta) {
        String q = consultaPara(consulta);
        URI uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("q", q)
                .queryParam("fields", CAMPOS)
                .queryParam("limit", consulta.tamanho())
                .queryParam("page", consulta.pagina() + 1) // o Open Library começa em 1
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        Resposta resposta;
        try {
            resposta = restClient.get().uri(uri)
                    .header(HttpHeaders.USER_AGENT, USER_AGENT)
                    .retrieve()
                    .body(Resposta.class);
        } catch (RestClientException ex) {
            log.warn("Open Library indisponível para q='{}': {}", q, ex.getMessage());
            throw new LivroExternoIndisponivelException("Não foi possível consultar a busca de livros no momento");
        }
        if (resposta == null) {
            throw new LivroExternoIndisponivelException("A busca de livros não retornou uma resposta válida");
        }

        List<LivroEncontrado> livros = resposta.docs() == null ? List.of()
                : resposta.docs().stream().map(OpenLibraryClient::livro).filter(Objects::nonNull).toList();
        log.info("Open Library: q='{}' pagina={} retornou {} de {} resultado(s)", q, consulta.pagina(), livros.size(),
                resposta.numFound());
        return ResultadoBuscaDeLivros.de(resposta.numFound() == null ? livros.size() : resposta.numFound(), livros,
                FonteDeLivros.OPEN_LIBRARY);
    }

    /** Sintaxe de busca do Open Library (Solr): {@code title:"duna" language:por}. */
    static String consultaPara(ConsultaDeLivros consulta) {
        String termo = consulta.termo().replace("\"", "");
        String q = switch (consulta.campo()) {
            case TUDO -> termo;
            case TITULO -> "title:\"" + termo + "\"";
            case AUTOR -> "author:\"" + termo + "\"";
            case EDITORA -> "publisher:\"" + termo + "\"";
            case ISBN -> "isbn:" + CampoDeBusca.somenteIsbn(termo);
        };
        String idioma = consulta.idioma() == null ? null : IDIOMAS.get(consulta.idioma());
        // ISBN identifica uma edição: o filtro de idioma só atrapalharia.
        if (idioma != null && consulta.campo() != CampoDeBusca.ISBN) {
            q += " language:" + idioma;
        }
        return q;
    }

    static LivroEncontrado livro(Documento d) {
        if (d.title() == null || d.title().isBlank() || d.key() == null) return null;
        String id = d.key().substring(d.key().lastIndexOf('/') + 1); // "/works/OL893415W" -> "OL893415W"
        String capa = d.coverId() == null ? null
                : "https://covers.openlibrary.org/b/id/" + d.coverId() + "-M.jpg";
        String idioma = d.language() == null || d.language().isEmpty() ? null
                : d.language().contains("por") ? "pt" : d.language().getFirst();
        return new LivroEncontrado(id, d.title(), d.subtitle(), d.autores(),
                d.publisher() == null || d.publisher().isEmpty() ? null : d.publisher().getFirst(),
                d.firstPublishYear() == null ? null : String.valueOf(d.firstPublishYear()),
                null, d.paginas(), idioma, isbn(d.isbn()), capa,
                "https://openlibrary.org" + d.key(), FonteDeLivros.OPEN_LIBRARY);
    }

    /** Uma obra lista os ISBNs de todas as edições; prefere um ISBN-13 brasileiro (978-85 / 978-65). */
    static String isbn(List<String> isbns) {
        if (isbns == null || isbns.isEmpty()) return null;
        return isbns.stream().filter(i -> i.startsWith("97885") || i.startsWith("97865")).findFirst()
                .or(() -> isbns.stream().filter(i -> i.length() == 13).findFirst())
                .orElse(isbns.getFirst());
    }

    record Resposta(Integer numFound, List<Documento> docs) {}

    record Documento(
            String key,
            String title,
            String subtitle,
            @JsonProperty("author_name") List<String> autores,
            List<String> publisher,
            @JsonProperty("first_publish_year") Integer firstPublishYear,
            @JsonProperty("number_of_pages_median") Integer paginas,
            List<String> language,
            List<String> isbn,
            @JsonProperty("cover_i") Long coverId) {}
}
