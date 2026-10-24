package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.livro.FonteDeLivros;
import github.jotagm.clube_livro.domain.livro.LivroEncontrado;
import github.jotagm.clube_livro.domain.livro.ResultadoBuscaDeLivros;

import java.util.List;

/**
 * Resultado de {@code GET /livros/buscar}, no formato do Booklit (não no do Google).
 *
 * @param fonte         catálogo que respondeu: GOOGLE_BOOKS ou OPEN_LIBRARY (reserva).
 * @param desatualizado os catálogos falharam e o resultado veio de uma busca anterior.
 */
public record BuscaDeLivrosResponse(int total, FonteDeLivros fonte, boolean desatualizado, List<Livro> livros) {

    /**
     * @param id use em {@code livroGoogleId} ao criar uma leitura ou opção de voto.
     */
    public record Livro(
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

        static Livro de(LivroEncontrado l) {
            return new Livro(l.id(), l.titulo(), l.subtitulo(), l.autores(), l.editora(), l.dataPublicacao(),
                    l.descricao(), l.paginas(), l.idioma(), l.isbn(), l.capaUrl(), l.linkInfo(), l.fonte());
        }
    }

    public static BuscaDeLivrosResponse de(ResultadoBuscaDeLivros r) {
        return new BuscaDeLivrosResponse(r.total(), r.fonte(), r.desatualizado(),
                r.livros().stream().map(Livro::de).toList());
    }
}
