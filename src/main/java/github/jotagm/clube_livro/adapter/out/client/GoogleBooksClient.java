package github.jotagm.clube_livro.adapter.out.client;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.GoogleBooksResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GoogleBooksClient {
    @Value("${google.books.api-key}")
    private String apiKey;

    @Value("${google.books.url}")
    private String url;

    private final RestClient restClient = RestClient.create();


    public GoogleBooksResponse buscarLivroGoogleBooks(String title, int page, int size) {

            // Executar a requisição de forma fluida
        GoogleBooksResponse response = restClient.get()
                .uri(url + "?q={title}&startIndex={startIndex}&maxResults={maxResults}&orderBy=relevance&printType=books&key={key}",
                        title, page * size, size, apiKey)
                .retrieve()
                .body(GoogleBooksResponse.class);

            log.info("livro encontrado {}", response);
            return response;
        }

}
