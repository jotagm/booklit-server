package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.livro.LivroEncontrado;
import github.jotagm.clube_livro.domain.livro.ResultadoBuscaDeLivros;

import java.util.List;

/**
 * Recorte da resposta do Google Books.
 *
 * <p>Tem dois papéis: é o formato em que o {@code GoogleBooksClient} lê o Google (o Jackson
 * descarta o que não está declarado aqui) e é a resposta do endpoint antigo
 * {@code GET /leituras/livros/buscar}, que o site e o app já consomem. Por isso, mesmo quando
 * o resultado vem do Open Library, ele sai neste formato ({@link #de}).
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
                ImageLinks imageLinks,
                List<IndustryIdentifier> industryIdentifiers) {
            public record ImageLinks(String smallThumbnail, String thumbnail) {}

            /** type: ISBN_13, ISBN_10 ou OTHER. */
            public record IndustryIdentifier(String type, String identifier) {}
        }
    }

    /** Converte o resultado do Booklit para o formato antigo, que espelha o Google. */
    public static GoogleBooksResponse de(ResultadoBuscaDeLivros resultado) {
        List<Item> itens = resultado.livros().stream().map(GoogleBooksResponse::item).toList();
        return new GoogleBooksResponse(resultado.total(), itens);
    }

    private static Item item(LivroEncontrado livro) {
        Item.VolumeInfo.ImageLinks capa = livro.capaUrl() == null ? null
                : new Item.VolumeInfo.ImageLinks(livro.capaUrl(), livro.capaUrl());
        List<Item.VolumeInfo.IndustryIdentifier> isbn = livro.isbn() == null ? null
                : List.of(new Item.VolumeInfo.IndustryIdentifier(
                        livro.isbn().length() == 13 ? "ISBN_13" : "ISBN_10", livro.isbn()));
        return new Item(livro.id(), new Item.VolumeInfo(
                livro.titulo(), livro.subtitulo(), livro.autores().isEmpty() ? null : livro.autores(),
                livro.editora(), livro.dataPublicacao(), livro.descricao(), livro.paginas(), livro.idioma(),
                livro.linkInfo(), capa, isbn));
    }
}
