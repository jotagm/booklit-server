package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import java.util.List;

/**
 * Recorte da resposta do Google Books.
 *
 * <p>O Jackson descarta o que não está declarado aqui, então este record é o que define o
 * que o front consegue mostrar. Só título e capa não bastavam: sem autor, ano e editora não
 * dá para distinguir duas edições do mesmo livro numa lista de resultados.
 */
public record GoogleBooksResponse(Integer totalItems, List<Item> items) {
    public record Item(String id, VolumeInfo volumeInfo) {
        public record VolumeInfo(
                String title,
                String subtitle,
                List<String> authors,
                String publisher,
                /** Vem como "2017", "2017-05" ou "2017-05-31", dependendo do volume. */
                String publishedDate,
                String description,
                /** Usado para pré-preencher a meta de páginas ao iniciar uma leitura. */
                Integer pageCount,
                String language,
                String infoLink,
                ImageLinks imageLinks) {
            public record ImageLinks(String smallThumbnail, String thumbnail) {}
        }
    }
}
