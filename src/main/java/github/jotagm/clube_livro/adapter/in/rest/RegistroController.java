package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.RegistroRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.RegistroResponse;
import github.jotagm.clube_livro.application.service.RegistroService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/registros")
@AllArgsConstructor
@Tag(name = "Registros de progresso", description = "Progresso individual de cada membro dentro de uma leitura, medido na unidade da meta (páginas ou capítulos).")
public class RegistroController {

    private final RegistroService registroService;
    private final UsuarioService usuarioService;

    @Operation(summary = "Busca um registro por id")
    @ApiResponse(responseCode = "200", description = "Registro encontrado")
    @ApiResponse(responseCode = "404", description = "Registro não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<RegistroResponse> buscarPorId(@Parameter(description = "Id do registro") @PathVariable UUID id) {
        return ResponseEntity.ok(RegistroResponse.from(registroService.buscarPorId(id)));
    }

    @Operation(
            summary = "Lista o progresso de todos os membros em uma leitura",
            description = "É a visão que alimenta o acompanhamento coletivo da leitura.")
    @ApiResponse(responseCode = "200", description = "Registros da leitura")
    @GetMapping("/leitura/{leituraClubeId}")
    public ResponseEntity<List<RegistroResponse>> listarPorLeitura(@Parameter(description = "Id da leitura") @PathVariable UUID leituraClubeId) {
        return ResponseEntity.ok(registroService.listarPorLeitura(leituraClubeId).stream()
                .map(RegistroResponse::from)
                .toList());
    }

    @Operation(summary = "Busca o progresso de um membro específico em uma leitura")
    @ApiResponse(responseCode = "200", description = "Registro encontrado")
    @ApiResponse(responseCode = "404", description = "Não há registro para essa combinação de leitura e usuário")
    @GetMapping("/leitura/{leituraClubeId}/usuario/{usuarioId}")
    public ResponseEntity<RegistroResponse> buscarPorLeituraEUsuario(
            @Parameter(description = "Id da leitura") @PathVariable UUID leituraClubeId,
            @Parameter(description = "Id do usuário") @PathVariable Integer usuarioId) {
        return ResponseEntity.ok(RegistroResponse.from(
                registroService.buscarPorLeituraEUsuario(leituraClubeId, usuarioId)));
    }

    @Operation(
            summary = "Atualiza o próprio progresso na leitura",
            description = "Sempre grava o progresso do usuário autenticado — não é possível registrar progresso de outro membro.")
    @ApiResponse(responseCode = "200", description = "Progresso atualizado")
    @ApiResponse(responseCode = "404", description = "Leitura não encontrada")
    @PutMapping("/leitura/{leituraId}")
    public ResponseEntity<RegistroResponse> atualizar(
            @Parameter(description = "Id da leitura") @PathVariable UUID leituraId,
            @RequestBody @Valid RegistroRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(RegistroResponse.from(
                registroService.atualizarProgresso(leituraId, usuario.getId(), request.valorAtual())));
    }
}
