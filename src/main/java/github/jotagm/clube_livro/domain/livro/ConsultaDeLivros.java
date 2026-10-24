package github.jotagm.clube_livro.domain.livro;

/**
 * Uma busca de livros, já normalizada: é também a chave do cache, então duas buscas que
 * diferem só em espaços ou maiúsculas precisam virar a mesma consulta.
 *
 * @param idioma código ISO-639-1 ("pt") ou nulo para todos os idiomas.
 * @param pagina base zero.
 */
public record ConsultaDeLivros(String termo, CampoDeBusca campo, int pagina, int tamanho, String idioma) {

    public static final int TAMANHO_MAXIMO = 40;

    public ConsultaDeLivros {
        if (termo == null || termo.isBlank()) {
            throw new IllegalArgumentException("Informe um termo para buscar");
        }
        termo = termo.trim().replaceAll("\\s+", " ");
        campo = campo == null ? CampoDeBusca.TUDO : campo;
        // Um ISBN colado no campo "tudo" vira busca por ISBN: em texto livre o Google acha
        // qualquer coisa que tenha esses dígitos, menos o livro.
        if (campo == CampoDeBusca.TUDO && CampoDeBusca.pareceIsbn(termo)) {
            campo = CampoDeBusca.ISBN;
        }
        pagina = Math.max(0, pagina);
        tamanho = Math.clamp(tamanho, 1, TAMANHO_MAXIMO);
        idioma = idioma == null || idioma.isBlank() ? null : idioma.trim().toLowerCase();
    }

    /** Chave do cache: o termo sem diferença de maiúsculas. */
    public ConsultaDeLivros normalizada() {
        return new ConsultaDeLivros(termo.toLowerCase(), campo, pagina, tamanho, idioma);
    }
}
