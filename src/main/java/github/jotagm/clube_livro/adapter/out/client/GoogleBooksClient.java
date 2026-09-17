package github.jotagm.clube_livro.adapter.out.client;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.GoogleBooksResponse;
import github.jotagm.clube_livro.domain.exceptions.LivroExternoIndisponivelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
public class GoogleBooksClient {

    @Value("${google.books.api-key}")
    private String apiKey;

    @Value("${google.books.url}")
    private String url;

    private final RestClient restClient;

    public GoogleBooksClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @param campo  onde procurar o termo; nulo equivale a {@link CampoDeBusca#TUDO}.
     * @param idioma código ISO-639-1 para o {@code langRestrict} do Google Books ("pt" cobre
     *               as edições brasileiras). Em branco ou nulo, busca em todos os idiomas.
     */
    public GoogleBooksResponse buscarLivroGoogleBooks(String title, CampoDeBusca campo, int page, int size,
                                                      String idioma) {
        String consulta = (campo == null ? CampoDeBusca.TUDO : campo).consultaPara(title);

        // Montar a URI aqui, em vez de concatenar o template, é o que permite omitir o
        // langRestrict quando ninguém pediu idioma - e deixa o encode de acento explícito.
        URI uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("q", consulta)
                .queryParam("startIndex", page * size)
                .queryParam("maxResults", size)
                .queryParam("orderBy", "relevance")
                .queryParam("printType", "books")
                .queryParamIfPresent("langRestrict",
                        Optional.ofNullable(idioma).filter(valor -> !valor.isBlank()))
                .queryParam("key", apiKey)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        GoogleBooksResponse response;

        try {
            response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(GoogleBooksResponse.class);
        } catch (RestClientException ex) {
            log.error("Falha ao consultar a API do Google Books para o título '{}'", title, ex);
            throw new LivroExternoIndisponivelException("Não foi possível consultar a busca de livros no momento");
        }

        if (response == null) {
            log.error("API do Google Books retornou corpo vazio para o título '{}'", title);
            throw new LivroExternoIndisponivelException("A busca de livros não retornou uma resposta válida");
        }

        // O objeto inteiro em INFO despejava dezenas de linhas por busca; o que interessa
        // no dia a dia é se a chamada voltou e com quantos resultados.
        int encontrados = response.items() == null ? 0 : response.items().size();
        log.info("Google Books: q='{}' idioma={} pagina={} retornou {} de {} resultado(s)",
                consulta, idioma == null || idioma.isBlank() ? "todos" : idioma, page, encontrados,
                response.totalItems() == null ? "?" : response.totalItems());
        log.debug("Resposta completa do Google Books: {}", response);

        return response;
    }
}
