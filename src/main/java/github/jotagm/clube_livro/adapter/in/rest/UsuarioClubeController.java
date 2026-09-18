package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.UsuarioClubeRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.UsuarioClubeResponse;
import github.jotagm.clube_livro.application.service.UsuarioClubeService;
import github.jotagm.clube_livro.configs.RequireLider;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/membros")
@AllArgsConstructor
@Tag(name = "Membros", description = "Vínculo entre usuário e clube, com o papel (LIDER ou MEMBRO). O vínculo é criado ao fundar um clube ou ao aceitar um convite.")
public class UsuarioClubeController {

    private final UsuarioClubeService usuarioClubeService;

    @Operation(summary = "Busca um vínculo de membro por id")
    @ApiResponse(responseCode = "200", description = "Vínculo encontrado")
    @ApiResponse(responseCode = "404", description = "Vínculo não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioClubeResponse> buscarPorId(@Parameter(description = "Id do vínculo") @PathVariable UUID id) {
        return ResponseEntity.ok(UsuarioClubeResponse.from(usuarioClubeService.buscarPorId(id)));
    }

    @Operation(summary = "Lista os clubes de um usuário")
    @ApiResponse(responseCode = "200", description = "Vínculos do usuário")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<UsuarioClubeResponse>> listarPorUsuario(@Parameter(description = "Id do usuário") @PathVariable Integer usuarioId) {
        return ResponseEntity.ok(usuarioClubeService.listarPorUsuario(usuarioId).stream()
                .map(UsuarioClubeResponse::from)
                .toList());
    }

    @Operation(summary = "Lista os membros de um clube")
    @ApiResponse(responseCode = "200", description = "Membros do clube")
    @GetMapping("/clube/{clubeId}")
    public ResponseEntity<List<UsuarioClubeResponse>> listarPorClube(@Parameter(description = "Id do clube") @PathVariable UUID clubeId) {
        return ResponseEntity.ok(usuarioClubeService.listarPorClube(clubeId).stream()
                .map(UsuarioClubeResponse::from)
                .toList());
    }

    @Operation(summary = "Busca o vínculo de um usuário em um clube", description = "Use para descobrir o papel do usuário no clube.")
    @ApiResponse(responseCode = "200", description = "Vínculo encontrado")
    @ApiResponse(responseCode = "404", description = "O usuário não é membro do clube")
    @GetMapping("/clube/{clubeId}/usuario/{usuarioId}")
    public ResponseEntity<UsuarioClubeResponse> buscarPorUsuarioEClube(@Parameter(description = "Id do clube") @PathVariable UUID clubeId,
                                                                        @Parameter(description = "Id do usuário") @PathVariable Integer usuarioId) {
        return ResponseEntity.ok(UsuarioClubeResponse.from(
                usuarioClubeService.buscarPorUsuarioEClube(usuarioId, clubeId)));
    }

    @Operation(
            summary = "Altera o papel de um membro",
            description = "Restrito ao LIDER do clube. Apenas `papel` é considerado; `usuarioId` e `clubeId` no corpo são ignorados.")
    @ApiResponse(responseCode = "200", description = "Papel atualizado")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado não é líder do clube")
    @ApiResponse(responseCode = "404", description = "Vínculo não encontrado")
    @PutMapping("/{id}")
    @RequireLider("@usuarioClubeService.buscarPorId(#id).clube.id")
    public ResponseEntity<UsuarioClubeResponse> atualizar(@PathVariable UUID id,
                                                          @RequestBody @Valid UsuarioClubeRequest request) {
        UsuarioClube uc = usuarioClubeService.buscarPorId(id);
        uc.mudarPapel(request.papel());

        return ResponseEntity.ok(UsuarioClubeResponse.from(usuarioClubeService.atualizar(uc)));
    }

    @Operation(summary = "Remove um membro do clube")
    @ApiResponse(responseCode = "204", description = "Membro removido")
    @ApiResponse(responseCode = "404", description = "Vínculo não encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@Parameter(description = "Id do vínculo") @PathVariable UUID id) {
        usuarioClubeService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
