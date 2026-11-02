package github.jotagm.clube_livro.domain.feed;

/** As reações disponíveis nos itens do feed: poucas de propósito, para ser um toque só. */
public enum TipoReacao {
    CURTIR,
    FOGO,
    PALMAS,
    /** Boas-vindas a quem acabou de entrar no clube (item MEMBRO_ENTROU do feed). */
    ACENO
}
