package github.jotagm.clube_livro.domain.livro;

import java.util.List;

/**
 * Um livro de um resultado de busca, no formato do Booklit, seja qual for o catálogo de
 * origem.
 *
 * @param id             id no catálogo de origem: o volume do Google ("zyTCAlFPjgYC") ou a
 *                       obra do Open Library ("OL893415W"). Vai para {@code livroGoogleId}.
 * @param dataPublicacao como o catálogo informa: "2017", "2017-05" ou "2017-05-31".
 * @param isbn           ISBN-13 quando houver; senão o ISBN-10.
 * @param capaUrl        sempre https.
 */
public record LivroEncontrado(
        String id,
        String titulo,
        String subtitulo,
        List<String> autores,
        String editora,
        String dataPublicacao,
        String descricao,
        Integer paginas,
        String idioma,
        String isbn,
        String capaUrl,
        String linkInfo,
        FonteDeLivros fonte) {

    public LivroEncontrado {
        autores = autores == null ? List.of() : List.copyOf(autores);
    }
}
