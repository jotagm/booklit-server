package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.application.port.out.CatalogoDeLivros;
import github.jotagm.clube_livro.domain.exceptions.LivroExternoIndisponivelException;
import github.jotagm.clube_livro.domain.livro.ConsultaDeLivros;
import github.jotagm.clube_livro.domain.livro.FonteDeLivros;
import github.jotagm.clube_livro.domain.livro.ResultadoBuscaDeLivros;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Busca de livros com reserva e cache.
 *
 * <p>Ordem: cache recente → Google Books (com novas tentativas) → Open Library quando o
 * Google falha ou não acha nada → cache antigo, se os dois falharem. Assim a busca só dá
 * erro quando nenhum catálogo responde e a consulta nunca foi feita antes.
 *
 * <p>O cache é em memória, por instância: some num deploy, o que é aceitável para uma busca.
 */
@Slf4j
@Service
public class BuscaDeLivrosService {

    /** Um resultado com menos tempo que isso é servido sem chamar ninguém. */
    static final Duration VALIDADE = Duration.ofMinutes(15);
    /** Até quanto tempo um resultado antigo ainda serve quando os catálogos falham. */
    static final Duration VALIDADE_EMERGENCIA = Duration.ofHours(24);
    static final int MAXIMO_NO_CACHE = 500;

    private final CatalogoDeLivros principal;
    private final CatalogoDeLivros reserva;
    private final Clock relogio;
    private final Map<ConsultaDeLivros, Guardado> cache = new ConcurrentHashMap<>();

    private record Guardado(ResultadoBuscaDeLivros resultado, Instant em) {}

    @Autowired
    public BuscaDeLivrosService(List<CatalogoDeLivros> catalogos, Clock relogio) {
        this(catalogo(catalogos, FonteDeLivros.GOOGLE_BOOKS), catalogo(catalogos, FonteDeLivros.OPEN_LIBRARY), relogio);
    }

    BuscaDeLivrosService(CatalogoDeLivros principal, CatalogoDeLivros reserva, Clock relogio) {
        this.principal = principal;
        this.reserva = reserva;
        this.relogio = relogio;
    }

    public ResultadoBuscaDeLivros buscar(ConsultaDeLivros consulta) {
        ConsultaDeLivros chave = consulta.normalizada();
        Instant agora = relogio.instant();

        Guardado guardado = cache.get(chave);
        if (guardado != null && guardado.em().plus(VALIDADE).isAfter(agora)) {
            return guardado.resultado();
        }

        ResultadoBuscaDeLivros resultado = consultarCatalogos(consulta);
        if (resultado != null) {
            guardar(chave, resultado, agora);
            return resultado;
        }

        if (guardado != null && guardado.em().plus(VALIDADE_EMERGENCIA).isAfter(agora)) {
            log.warn("Busca de livros: catálogos fora do ar; servindo do cache q='{}'", consulta.termo());
            return guardado.resultado().comoDesatualizado();
        }
        throw new LivroExternoIndisponivelException(
                "A busca de livros está instável agora. Tente de novo em alguns instantes.");
    }

    /** Devolve null quando nenhum catálogo respondeu. */
    private ResultadoBuscaDeLivros consultarCatalogos(ConsultaDeLivros consulta) {
        ResultadoBuscaDeLivros doPrincipal = null;
        try {
            doPrincipal = principal.buscar(consulta);
            // O Google às vezes responde 200 sem nada para títulos que existem ("Duna"). Só
            // vale tentar a reserva na primeira página: nas seguintes, vazio é fim de lista.
            if (!doPrincipal.vazio() || consulta.pagina() > 0) {
                return doPrincipal;
            }
        } catch (LivroExternoIndisponivelException ex) {
            log.info("Busca de livros: {} falhou, tentando {}", principal.fonte(), reserva.fonte());
        }

        try {
            ResultadoBuscaDeLivros daReserva = reserva.buscar(consulta);
            // Reserva vazia e principal vazio: a resposta honesta é "nada encontrado".
            return daReserva.vazio() && doPrincipal != null ? doPrincipal : daReserva;
        } catch (LivroExternoIndisponivelException ex) {
            return doPrincipal;
        }
    }

    private void guardar(ConsultaDeLivros chave, ResultadoBuscaDeLivros resultado, Instant agora) {
        cache.put(chave, new Guardado(resultado, agora));
        if (cache.size() > MAXIMO_NO_CACHE) {
            // Descarta o quarto mais antigo de uma vez, para não ordenar a cada inserção.
            cache.entrySet().stream()
                    .sorted(Comparator.comparing((Map.Entry<ConsultaDeLivros, Guardado> e) -> e.getValue().em()))
                    .limit(MAXIMO_NO_CACHE / 4)
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(cache::remove);
        }
    }

    private static CatalogoDeLivros catalogo(List<CatalogoDeLivros> catalogos, FonteDeLivros fonte) {
        return catalogos.stream().filter(c -> c.fonte() == fonte).findFirst()
                .orElseThrow(() -> new IllegalStateException("Catálogo de livros ausente: " + fonte));
    }
}
