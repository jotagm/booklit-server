package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.UsuarioRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.UsuarioResponse;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Usuários", description = "Cadastro e consulta de usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(
            summary = "Cria um usuário",
            description = "Endpoint público — é o cadastro inicial. A senha é armazenada com hash.")
    @ApiResponse(responseCode = "201", description = "Usuário criado")
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@RequestBody @Valid UsuarioRequest request) {
        Usuario usuario = Usuario.novo(request.nome(), request.email(), request.senha());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UsuarioResponse.from(usuarioService.salvar(usuario)));
    }

    @Operation(summary = "Busca um usuário por id")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(
            @Parameter(description = "Id do usuário") @PathVariable UUID id) {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.buscarPorId(id)));
    }

    @Operation(summary = "Busca um usuário por e-mail")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @GetMapping("/email/{email}")
    public ResponseEntity<UsuarioResponse> buscarPorEmail(
            @Parameter(description = "E-mail cadastrado", example = "leitor@booklit.dev")
            @PathVariable String email) {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.buscarPorEmail(email)));
    }

    @Operation(
            summary = "Atualiza um usuário",
            description = "Substitui nome, e-mail e senha. Todos os campos são obrigatórios.")
    @ApiResponse(responseCode = "200", description = "Usuário atualizado")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(
            @Parameter(description = "Id do usuário") @PathVariable UUID id,
            @RequestBody @Valid UsuarioRequest request) {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.atualizar(id, request)));
    }
}
