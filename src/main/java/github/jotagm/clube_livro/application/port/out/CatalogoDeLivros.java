package github.jotagm.clube_livro.application.port.out;

import github.jotagm.clube_livro.domain.exceptions.LivroExternoIndisponivelException;
import github.jotagm.clube_livro.domain.livro.ConsultaDeLivros;
import github.jotagm.clube_livro.domain.livro.FonteDeLivros;
import github.jotagm.clube_livro.domain.livro.ResultadoBuscaDeLivros;

/** Um catálogo externo de livros (Google Books, Open Library). */
public interface CatalogoDeLivros {

    FonteDeLivros fonte();

    /**
     * @throws LivroExternoIndisponivelException quando o catálogo não respondeu (fora do ar,
     *                                           timeout, resposta inválida).
     */
    ResultadoBuscaDeLivros buscar(ConsultaDeLivros consulta);
}
