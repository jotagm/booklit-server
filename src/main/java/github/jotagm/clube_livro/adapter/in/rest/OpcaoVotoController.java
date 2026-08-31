package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.OpcaoVotoRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.OpcaoVotoResponse;
import github.jotagm.clube_livro.application.service.OpcaoVotoService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.application.service.VotacaoService;
import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;
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
@RequestMapping("/opcoes-voto")
@AllArgsConstructor
@Tag(name = "Opções de voto", description = "Livros candidatos de uma votação. Use `GET /leituras/livros/buscar` para obter os dados do livro no Google Books.")
public class OpcaoVotoController {

    private final OpcaoVotoService opcaoVotoService;
    private final VotacaoService votacaoService;
    private final UsuarioService usuarioService;

    @Operation(summary = "Adiciona um livro como opção de uma votação")
    @ApiResponse(responseCode = "201", description = "Opção criada")
    @ApiResponse(responseCode = "404", description = "Votação ou usuário sugerinte não encontrado")
    @PostMapping
    public ResponseEntity<OpcaoVotoResponse> criar(@RequestBody @Valid OpcaoVotoRequest request) {
        OpcaoVoto opcaoVoto = OpcaoVoto.sugerir(
                votacaoService.buscarPorId(request.votacaoId()),
                usuarioService.buscarPorId(request.sugeridoPorId()),
                request.livroGoogleId(),
                request.livroTitulo(),
                request.livroCapaUrl());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OpcaoVotoResponse.from(opcaoVotoService.salvar(opcaoVoto)));
    }

    @Operation(summary = "Busca uma opção de voto por id")
    @ApiResponse(responseCode = "200", description = "Opção encontrada")
    @ApiResponse(responseCode = "404", description = "Opção não encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<OpcaoVotoResponse> buscarPorId(@Parameter(description = "Id da opção de voto") @PathVariable UUID id) {
        return ResponseEntity.ok(OpcaoVotoResponse.from(opcaoVotoService.buscarPorId(id)));
    }

    @Operation(summary = "Lista as opções de uma votação", description = "É a cédula: os livros disponíveis para voto.")
    @ApiResponse(responseCode = "200", description = "Opções da votação")
    @GetMapping("/votacao/{votacaoId}")
    public ResponseEntity<List<OpcaoVotoResponse>> listarPorVotacao(@Parameter(description = "Id da votação") @PathVariable UUID votacaoId) {
        return ResponseEntity.ok(opcaoVotoService.listarPorVotacao(votacaoId).stream()
                .map(OpcaoVotoResponse::from)
                .toList());
    }

    @Operation(summary = "Lista as opções sugeridas por um usuário")
    @ApiResponse(responseCode = "200", description = "Opções sugeridas pelo usuário")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<OpcaoVotoResponse>> listarPorUsuario(@Parameter(description = "Id do usuário sugerinte") @PathVariable UUID usuarioId) {
        return ResponseEntity.ok(opcaoVotoService.listarPorUsuario(usuarioId).stream()
                .map(OpcaoVotoResponse::from)
                .toList());
    }

    @Operation(
            summary = "Atualiza o livro de uma opção de voto",
            description = "Apenas os dados do livro mudam; `votacaoId` e `sugeridoPorId` no corpo são ignorados.")
    @ApiResponse(responseCode = "200", description = "Opção atualizada")
    @ApiResponse(responseCode = "404", description = "Opção não encontrada")
    @PutMapping("/{id}")
    public ResponseEntity<OpcaoVotoResponse> atualizar(@Parameter(description = "Id da opção de voto") @PathVariable UUID id,
                                                       @RequestBody @Valid OpcaoVotoRequest request) {
        OpcaoVoto opcaoVoto = opcaoVotoService.buscarPorId(id);
        opcaoVoto.trocarLivro(request.livroGoogleId(), request.livroTitulo(), request.livroCapaUrl());

        return ResponseEntity.ok(OpcaoVotoResponse.from(opcaoVotoService.atualizar(opcaoVoto)));
    }

    @Operation(summary = "Remove uma opção de voto")
    @ApiResponse(responseCode = "204", description = "Opção removida")
    @ApiResponse(responseCode = "404", description = "Opção não encontrada")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@Parameter(description = "Id da opção de voto") @PathVariable UUID id) {
        opcaoVotoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
