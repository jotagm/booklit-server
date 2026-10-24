package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.BuscaDeLivrosResponse;
import github.jotagm.clube_livro.application.service.BuscaDeLivrosService;
import github.jotagm.clube_livro.domain.livro.CampoDeBusca;
import github.jotagm.clube_livro.domain.livro.ConsultaDeLivros;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/livros")
@AllArgsConstructor
@Tag(name = "Livros", description = "Busca de livros nos catálogos externos (Google Books, com Open Library de reserva)")
public class LivroController {

    private final BuscaDeLivrosService buscaDeLivrosService;

    @Operation(
            summary = "Busca livros",
            description = """
                    Procura no Google Books; se ele falhar ou não achar nada, no Open Library. Buscas
                    iguais nos últimos 15 minutos vêm do cache, e com os dois catálogos fora do ar
                    uma busca já feita nas últimas 24 h volta com `desatualizado: true`.

                    Um ISBN digitado com `campo=TUDO` é tratado como busca por ISBN. Com `idioma=pt`
                    a busca fica nas edições em português.
                    """)
    @ApiResponse(responseCode = "200", description = "Resultado da busca")
    @ApiResponse(responseCode = "400", description = "Nenhum termo informado")
    @ApiResponse(responseCode = "502", description = "Nenhum catálogo de livros respondeu")
    @GetMapping("/buscar")
    public BuscaDeLivrosResponse buscar(
            @Parameter(description = "Termo a pesquisar", example = "Duna") @RequestParam(required = false) String termo,
            @Parameter(description = "Onde procurar o termo") @RequestParam(defaultValue = "TUDO") CampoDeBusca campo,
            @Parameter(description = "Página do resultado (base zero)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Resultados por página (máximo 40)") @RequestParam(defaultValue = "12") int size,
            @Parameter(description = "Código ISO-639-1 do idioma (ex: pt). Vazio busca em todos.", example = "pt")
            @RequestParam(required = false) String idioma) {
        if (termo == null || termo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um termo para buscar");
        }
        return BuscaDeLivrosResponse.de(
                buscaDeLivrosService.buscar(new ConsultaDeLivros(termo, campo, page, size, idioma)));
    }
}
