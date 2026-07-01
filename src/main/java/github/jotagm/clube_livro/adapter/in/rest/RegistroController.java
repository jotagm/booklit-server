package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.RegistroRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.RegistroResponse;
import github.jotagm.clube_livro.application.service.RegistroService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.domain.usuario.Usuario;
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
public class RegistroController {

    private final RegistroService registroService;
    private final UsuarioService usuarioService;

    @GetMapping("/{id}")
    public ResponseEntity<RegistroResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(RegistroResponse.from(registroService.buscarPorId(id)));
    }

    @GetMapping("/leitura/{leituraClubeId}")
    public ResponseEntity<List<RegistroResponse>> listarPorLeitura(@PathVariable UUID leituraClubeId) {
        return ResponseEntity.ok(registroService.listarPorLeitura(leituraClubeId).stream()
                .map(RegistroResponse::from)
                .toList());
    }

    @GetMapping("/leitura/{leituraClubeId}/usuario/{usuarioId}")
    public ResponseEntity<RegistroResponse> buscarPorLeituraEUsuario(
            @PathVariable UUID leituraClubeId,
            @PathVariable UUID usuarioId) {
        return ResponseEntity.ok(RegistroResponse.from(
                registroService.buscarPorLeituraEUsuario(leituraClubeId, usuarioId)));
    }

    @PutMapping("/leitura/{leituraId}")
    public ResponseEntity<RegistroResponse> atualizar(
            @PathVariable UUID leituraId,
            @RequestBody @Valid RegistroRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(RegistroResponse.from(
                registroService.atualizarProgresso(leituraId, usuario.getId(), request.valorAtual())));
    }
}
