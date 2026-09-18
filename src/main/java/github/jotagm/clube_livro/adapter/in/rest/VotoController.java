package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.VotoRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.VotoResponse;
import github.jotagm.clube_livro.application.service.OpcaoVotoService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.application.service.VotacaoService;
import github.jotagm.clube_livro.application.service.VotoService;
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
@RequestMapping("/votos")
@AllArgsConstructor
@Tag(name = "Votos", description = "Votos registrados em uma votação. O voto do LIDER tem peso 2; dos demais membros, peso 1.")
public class VotoController {

    private final VotoService votoService;

    @Operation(
            summary = "Registra um voto",
            description = "Um usuário só pode votar uma vez por votação. O peso é definido pelo papel dele no clube.")
    @ApiResponse(responseCode = "201", description = "Voto registrado")
    @ApiResponse(responseCode = "404", description = "Votação, opção ou usuário não encontrado")
    @ApiResponse(responseCode = "409", description = "O usuário já votou nesta votação")
    @PostMapping
    public ResponseEntity<VotoResponse> votar(@RequestBody @Valid VotoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VotoResponse.from(votoService.votar(request)));
    }

    @Operation(summary = "Busca um voto por id")
    @ApiResponse(responseCode = "200", description = "Voto encontrado")
    @ApiResponse(responseCode = "404", description = "Voto não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<VotoResponse> buscarPorId(@Parameter(description = "Id do voto") @PathVariable UUID id) {
        return ResponseEntity.ok(VotoResponse.from(votoService.buscarPorId(id)));
    }

    @Operation(summary = "Lista todos os votos de uma votação")
    @ApiResponse(responseCode = "200", description = "Votos da votação")
    @GetMapping("/votacao/{votacaoId}")
    public ResponseEntity<List<VotoResponse>> listarPorVotacao(@Parameter(description = "Id da votação") @PathVariable UUID votacaoId) {
        return ResponseEntity.ok(votoService.listarPorVotacao(votacaoId).stream()
                .map(VotoResponse::from)
                .toList());
    }

    @Operation(summary = "Lista os votos recebidos por uma opção", description = "Útil para exibir a apuração parcial por livro.")
    @ApiResponse(responseCode = "200", description = "Votos da opção")
    @GetMapping("/opcao/{opcaoVotoId}")
    public ResponseEntity<List<VotoResponse>> listarPorOpcao(@Parameter(description = "Id da opção de voto") @PathVariable UUID opcaoVotoId) {
        return ResponseEntity.ok(votoService.listarPorOpcao(opcaoVotoId).stream()
                .map(VotoResponse::from)
                .toList());
    }

    @Operation(summary = "Busca o voto de um usuário em uma votação")
    @ApiResponse(responseCode = "200", description = "Voto encontrado")
    @ApiResponse(responseCode = "404", description = "O usuário ainda não votou nesta votação")
    @GetMapping("/votacao/{votacaoId}/usuario/{usuarioId}")
    public ResponseEntity<VotoResponse> buscarPorVotacaoEUsuario(
            @Parameter(description = "Id da votação") @PathVariable UUID votacaoId,
            @Parameter(description = "Id do usuário") @PathVariable Integer usuarioId) {
        return ResponseEntity.ok(VotoResponse.from(
                votoService.buscarPorVotacaoEUsuario(votacaoId, usuarioId)));
    }

    @Operation(summary = "Remove um voto")
    @ApiResponse(responseCode = "204", description = "Voto removido")
    @ApiResponse(responseCode = "404", description = "Voto não encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@Parameter(description = "Id do voto") @PathVariable UUID id) {
        votoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
