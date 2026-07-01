package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.UsuarioRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.UsuarioResponse;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
@AllArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@RequestBody @Valid UsuarioRequest request) {
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenhaHash(request.senha());
        usuario.setCreatedAt(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UsuarioResponse.from(usuarioService.salvar(usuario)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.buscarPorId(id)));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UsuarioResponse> buscarPorEmail(@PathVariable String email) {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.buscarPorEmail(email)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable UUID id,
                                                     @RequestBody @Valid UsuarioRequest request) {
        Usuario usuario = usuarioService.buscarPorId(id);
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenhaHash(request.senha());

        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.atualizar(usuario)));
    }
}
