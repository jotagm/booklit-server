package github.jotagm.clube_livro.domain.livro;

import java.util.List;

/**
 * @param total        total estimado pelo catálogo (o Google oscila entre páginas).
 * @param desatualizado veio do cache porque os catálogos falharam agora.
 */
public record ResultadoBuscaDeLivros(int total, List<LivroEncontrado> livros, FonteDeLivros fonte,
                                     boolean desatualizado) {

    public ResultadoBuscaDeLivros {
        livros = livros == null ? List.of() : List.copyOf(livros);
        total = Math.max(total, livros.size());
    }

    public static ResultadoBuscaDeLivros de(int total, List<LivroEncontrado> livros, FonteDeLivros fonte) {
        return new ResultadoBuscaDeLivros(total, livros, fonte, false);
    }

    public boolean vazio() {
        return livros.isEmpty();
    }

    public ResultadoBuscaDeLivros comoDesatualizado() {
        return new ResultadoBuscaDeLivros(total, livros, fonte, true);
    }
}
