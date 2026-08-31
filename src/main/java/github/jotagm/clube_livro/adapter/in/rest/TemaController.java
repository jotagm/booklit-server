package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.TemaRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.TemaResponse;
import github.jotagm.clube_livro.application.service.TemaService;
import github.jotagm.clube_livro.domain.tema.Tema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/temas")
@AllArgsConstructor
@Tag(name = "Temas", description = "Catálogo de temas usados para classificar clubes")
public class TemaController {

    private final TemaService temaService;

    @Operation(summary = "Cria um tema")
    @ApiResponse(responseCode = "201", description = "Tema criado")
    @PostMapping
    public ResponseEntity<TemaResponse> criar(@RequestBody @Valid TemaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TemaResponse.from(temaService.salvar(Tema.novo(request.nome()))));
    }

    @Operation(
            summary = "Lista temas paginados",
            description = "Aceita os parâmetros de paginação padrão do Spring: `page`, `size` e `sort`.")
    @ApiResponse(responseCode = "200", description = "Página de temas")
    @GetMapping
    public ResponseEntity<Page<TemaResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(temaService.listarTodos(pageable).map(TemaResponse::from));
    }

    @Operation(summary = "Busca um tema por id")
    @ApiResponse(responseCode = "200", description = "Tema encontrado")
    @ApiResponse(responseCode = "404", description = "Tema não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<TemaResponse> buscarPorId(
            @Parameter(description = "Id do tema") @PathVariable UUID id) {
        return ResponseEntity.ok(TemaResponse.from(temaService.buscarPorId(id)));
    }

    @Operation(summary = "Busca um tema por nome")
    @ApiResponse(responseCode = "200", description = "Tema encontrado")
    @ApiResponse(responseCode = "404", description = "Tema não encontrado")
    @GetMapping("/nome/{nome}")
    public ResponseEntity<TemaResponse> buscarPorNome(
            @Parameter(description = "Nome exato do tema", example = "Ficção científica")
            @PathVariable String nome) {
        return ResponseEntity.ok(TemaResponse.from(temaService.buscarPorNome(nome)));
    }

    @Operation(summary = "Atualiza o nome de um tema")
    @ApiResponse(responseCode = "200", description = "Tema atualizado")
    @ApiResponse(responseCode = "404", description = "Tema não encontrado")
    @PutMapping("/{id}")
    public ResponseEntity<TemaResponse> atualizar(
            @Parameter(description = "Id do tema") @PathVariable UUID id,
            @RequestBody @Valid TemaRequest request) {
        Tema tema = temaService.buscarPorId(id);
        tema.renomear(request.nome());

        return ResponseEntity.ok(TemaResponse.from(temaService.atualizar(tema)));
    }

    @Operation(summary = "Remove um tema")
    @ApiResponse(responseCode = "204", description = "Tema removido")
    @ApiResponse(responseCode = "404", description = "Tema não encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @Parameter(description = "Id do tema") @PathVariable UUID id) {
        temaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
