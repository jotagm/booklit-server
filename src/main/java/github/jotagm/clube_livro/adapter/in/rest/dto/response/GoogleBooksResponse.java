package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import java.util.List;


public record GoogleBooksResponse(List<Item> items) {
    public record Item(String id, VolumeInfo volumeInfo) {
        public record VolumeInfo(String title, List<String> authors, ImageLinks imageLinks) {
            public record ImageLinks(String thumbnail) {}
        }
    }
}
