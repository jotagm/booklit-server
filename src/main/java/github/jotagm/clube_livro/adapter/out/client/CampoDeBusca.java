package github.jotagm.clube_livro.adapter.out.client;

/**
 * Onde procurar o termo dentro do volume, mapeado para os operadores do Google Books.
 *
 * <p>{@link #TUDO} não usa operador: pesquisa em título, autor, editora e conteúdo ao mesmo
 * tempo. É o mais abrangente e o mais impreciso — procurar por um autor traz também os livros
 * que apenas o citam.
 */
public enum CampoDeBusca {

    TUDO(""),
    TITULO("intitle:"),
    AUTOR("inauthor:"),
    EDITORA("inpublisher:"),
    ISBN("isbn:");

    private final String operador;

    CampoDeBusca(String operador) {
        this.operador = operador;
    }

    /**
     * Monta o valor do parâmetro {@code q}.
     *
     * <p>As aspas prendem o termo ao operador: sem elas, {@code inauthor:Ursula K. Le Guin}
     * aplicaria o operador só em "Ursula" e trataria o resto como busca livre. O ISBN é a
     * exceção — o Google não aceita o código entre aspas.
     */
    public String consultaPara(String termo) {
        String limpo = termo.trim();

        if (this == TUDO) return limpo;
        if (this == ISBN) return operador + limpo.replaceAll("[^0-9Xx]", "");

        return operador + "\"" + limpo.replace("\"", "") + "\"";
    }
}
