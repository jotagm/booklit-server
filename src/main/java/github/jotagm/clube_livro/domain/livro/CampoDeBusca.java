package github.jotagm.clube_livro.domain.livro;

/**
 * Onde procurar o termo dentro do livro. Cada catálogo traduz para a própria sintaxe
 * ({@code intitle:} no Google, {@code title:} no Open Library).
 *
 * <p>{@link #TUDO} pesquisa em título, autor, editora e conteúdo ao mesmo tempo: é o mais
 * abrangente e o mais impreciso — procurar por um autor traz também os livros que só o citam.
 */
public enum CampoDeBusca {
    TUDO,
    TITULO,
    AUTOR,
    EDITORA,
    ISBN;

    /** ISBN como o catálogo espera: só dígitos (e o X final do ISBN-10), sem hífens. */
    public static String somenteIsbn(String termo) {
        return termo.replaceAll("[^0-9Xx]", "").toUpperCase();
    }

    /** Um termo digitado em "tudo" que é, na verdade, um ISBN-10 ou ISBN-13. */
    public static boolean pareceIsbn(String termo) {
        String limpo = termo.replaceAll("[\\s-]", "");
        return limpo.matches("\\d{13}") || limpo.matches("\\d{9}[\\dXx]");
    }
}
