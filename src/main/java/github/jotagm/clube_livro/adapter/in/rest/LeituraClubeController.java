package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.LeituraClubeRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.GoogleBooksResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.LeituraClubeResponse;
import github.jotagm.clube_livro.adapter.out.client.GoogleBooksClient;
import github.jotagm.clube_livro.application.service.LeituraClubeService;
import github.jotagm.clube_livro.configs.RequireLider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/leituras")
@AllArgsConstructor
@Tag(name = "Leituras", description = """
        Leituras de um clube, com meta em páginas ou capítulos. Uma leitura está ativa quando
        `dataInicio <= agora <= dataFim` — o status é derivado do tempo, não persistido.
        """)
public class LeituraClubeController {

    private final LeituraClubeService leituraClubeService;
    private final GoogleBooksClient googleBooksClient;

    @Operation(
            summary = "Cria uma leitura para o clube",
            description = "Restrito ao LIDER do clube informado em `clubeId`.")
    @ApiResponse(responseCode = "201", description = "Leitura criada")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado não é líder do clube")
    @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    @PostMapping
    @RequireLider("#request.clubeId()")
    public ResponseEntity<LeituraClubeResponse> criar(@RequestBody @Valid LeituraClubeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LeituraClubeResponse.from(leituraClubeService.criar(request)));
    }

    @Operation(summary = "Busca uma leitura por id")
    @ApiResponse(responseCode = "200", description = "Leitura encontrada")
    @ApiResponse(responseCode = "404", description = "Leitura não encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<LeituraClubeResponse> buscarPorId(
            @Parameter(description = "Id da leitura") @PathVariable UUID id) {
        return ResponseEntity.ok(LeituraClubeResponse.from(leituraClubeService.buscarPorId(id)));
    }

    @Operation(summary = "Lista o histórico de leituras de um clube")
    @ApiResponse(responseCode = "200", description = "Leituras do clube")
    @GetMapping("/clube/{clubeId}")
    public ResponseEntity<List<LeituraClubeResponse>> listarPorClube(
            @Parameter(description = "Id do clube") @PathVariable UUID clubeId) {
        return ResponseEntity.ok(leituraClubeService.listarPorClube(clubeId).stream()
                .map(LeituraClubeResponse::from)
                .toList());
    }

    @Operation(
            summary = "Atualiza uma leitura",
            description = "Restrito ao LIDER do clube dono da leitura.")
    @ApiResponse(responseCode = "200", description = "Leitura atualizada")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado não é líder do clube")
    @ApiResponse(responseCode = "404", description = "Leitura não encontrada")
    @PutMapping("/{id}")
    @RequireLider("@leituraClubeService.buscarPorId(#id).clube.id")
    public ResponseEntity<LeituraClubeResponse> atualizar(
            @Parameter(description = "Id da leitura") @PathVariable UUID id,
            @RequestBody @Valid LeituraClubeRequest request) {
        return ResponseEntity.ok(LeituraClubeResponse.from(leituraClubeService.atualizar(id, request)));
    }

    @Operation(summary = "Remove uma leitura", description = "Restrito ao LIDER do clube dono da leitura.")
    @ApiResponse(responseCode = "204", description = "Leitura removida")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado não é líder do clube")
    @ApiResponse(responseCode = "404", description = "Leitura não encontrada")
    @DeleteMapping("/{id}")
    @RequireLider("@leituraClubeService.buscarPorId(#id).clube.id")
    public ResponseEntity<Void> deletar(
            @Parameter(description = "Id da leitura") @PathVariable UUID id) {
        leituraClubeService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Busca a leitura ativa do clube",
            description = "Retorna a leitura cujo intervalo `dataInicio`–`dataFim` contém o instante atual.")
    @ApiResponse(responseCode = "200", description = "Leitura ativa encontrada")
    @ApiResponse(responseCode = "404", description = "O clube não possui leitura ativa no momento")
    @GetMapping("/clube/{clubeId}/leitura-ativa")
    public ResponseEntity<LeituraClubeResponse> buscarLeituraAtiva(
            @Parameter(description = "Id do clube") @PathVariable UUID clubeId) {
        return leituraClubeService.buscarLeituraAtiva(clubeId)
                .map(LeituraClubeResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Busca livros no Google Books",
            description = """
                    Proxy para a API do Google Books, usada para preencher `livroGoogleId`,
                    `livroTitulo` e `livroCapaUrl` ao criar uma leitura ou opção de voto.
                    """)
    @ApiResponse(responseCode = "200", description = "Resultado da busca")
    @ApiResponse(responseCode = "502", description = "A API do Google Books está indisponível")
    @GetMapping("/livros/buscar")
    public GoogleBooksResponse buscarLivroGoogle(
            @Parameter(description = "Título a pesquisar", example = "Duna") @RequestParam String titulo,
            @Parameter(description = "Página do resultado (base zero)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de resultados por página") @RequestParam(defaultValue = "10") int size) {
        return googleBooksClient.buscarLivroGoogleBooks(titulo, page, size);
    }
}
