package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.UsuarioClubeRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.UsuarioClubeResponse;
import github.jotagm.clube_livro.application.service.UsuarioClubeService;
import github.jotagm.clube_livro.configs.RequireLider;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/membros")
@AllArgsConstructor
public class UsuarioClubeController {

    private final UsuarioClubeService usuarioClubeService;

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioClubeResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(UsuarioClubeResponse.from(usuarioClubeService.buscarPorId(id)));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<UsuarioClubeResponse>> listarPorUsuario(@PathVariable UUID usuarioId) {
        return ResponseEntity.ok(usuarioClubeService.listarPorUsuario(usuarioId).stream()
                .map(UsuarioClubeResponse::from)
                .toList());
    }

    @GetMapping("/clube/{clubeId}")
    public ResponseEntity<List<UsuarioClubeResponse>> listarPorClube(@PathVariable UUID clubeId) {
        return ResponseEntity.ok(usuarioClubeService.listarPorClube(clubeId).stream()
                .map(UsuarioClubeResponse::from)
                .toList());
    }

    @GetMapping("/clube/{clubeId}/usuario/{usuarioId}")
    public ResponseEntity<UsuarioClubeResponse> buscarPorUsuarioEClube(@PathVariable UUID clubeId,
                                                                        @PathVariable UUID usuarioId) {
        return ResponseEntity.ok(UsuarioClubeResponse.from(
                usuarioClubeService.buscarPorUsuarioEClube(usuarioId, clubeId)));
    }

    @PutMapping("/{id}")
    @RequireLider("@usuarioClubeService.buscarPorId(#id).clube.id")
    public ResponseEntity<UsuarioClubeResponse> atualizar(@PathVariable UUID id,
                                                          @RequestBody @Valid UsuarioClubeRequest request) {
        UsuarioClube uc = usuarioClubeService.buscarPorId(id);
        uc.setPapel(request.papel());

        return ResponseEntity.ok(UsuarioClubeResponse.from(usuarioClubeService.atualizar(uc)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        usuarioClubeService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
