package github.jotagm.clube_livro.adapter.out.client;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.GoogleBooksResponse;
import github.jotagm.clube_livro.domain.exceptions.LivroExternoIndisponivelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

    public GoogleBooksResponse buscarLivroGoogleBooks(String title, int page, int size) {
        GoogleBooksResponse response;

        try {
            response = restClient.get()
                    .uri(url + "?q={title}&startIndex={startIndex}&maxResults={maxResults}&orderBy=relevance&printType=books&key={key}",
                            title, page * size, size, apiKey)
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

        log.info("livro encontrado {}", response);
        return response;
    }
}
