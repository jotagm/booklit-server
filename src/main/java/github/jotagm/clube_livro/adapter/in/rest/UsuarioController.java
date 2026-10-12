package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.PerfilRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.UsuarioRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.PerfilResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.UsuarioResponse;
import github.jotagm.clube_livro.application.service.PerfilService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.configs.AutorizacaoClube;
import github.jotagm.clube_livro.domain.exceptions.AcessoNegadoException;
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

@RestController
@RequestMapping("/usuarios")
@AllArgsConstructor
@Tag(name = "Usuários", description = "Cadastro e consulta de usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PerfilService perfilService;
    private final AutorizacaoClube autorizacaoClube;

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

    @Operation(summary = "Retorna o usuário autenticado", description = "Identifica o dono do token — é o \"quem sou eu\" dos clientes.")
    @ApiResponse(responseCode = "200", description = "Usuário autenticado")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> eu() {
        return ResponseEntity.ok(UsuarioResponse.from(autorizacaoClube.usuarioAutenticado()));
    }

    @Operation(summary = "Busca um usuário por id", description = "Só o próprio usuário pode consultar os próprios dados.")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(
            @Parameter(description = "Id do usuário") @PathVariable Integer id) {
        autorizacaoClube.exigirProprioUsuario(id);
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.buscarPorId(id)));
    }

    @Operation(summary = "Busca um usuário por e-mail", description = "Só o próprio e-mail pode ser consultado — evita que a API sirva para descobrir quem tem conta.")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @GetMapping("/email/{email}")
    public ResponseEntity<UsuarioResponse> buscarPorEmail(
            @Parameter(description = "E-mail cadastrado", example = "leitor@booklit.dev")
            @PathVariable String email) {
        Usuario usuario = autorizacaoClube.usuarioAutenticado();
        if (!usuario.getEmail().equalsIgnoreCase(email)) {
            throw new AcessoNegadoException("Acesso negado: este recurso pertence a outro usuário");
        }
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.buscarPorEmail(email)));
    }

    @Operation(
            summary = "Atualiza um usuário",
            description = "Substitui nome, e-mail e senha. Todos os campos são obrigatórios e só o próprio usuário pode se atualizar. Trocar o e-mail invalida o token atual (o e-mail é o subject do JWT): o cliente precisa autenticar de novo.")
    @ApiResponse(responseCode = "200", description = "Usuário atualizado")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(
            @Parameter(description = "Id do usuário") @PathVariable Integer id,
            @RequestBody @Valid UsuarioRequest request) {
        autorizacaoClube.exigirProprioUsuario(id);
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.atualizar(id, request)));
    }

    @Operation(summary = "Edita o próprio perfil", description = "Nome e bio (até 160 caracteres). Não exige senha nem invalida o token.")
    @ApiResponse(responseCode = "200", description = "Perfil atualizado")
    @PatchMapping("/me/perfil")
    public ResponseEntity<UsuarioResponse> editarPerfil(@RequestBody @Valid PerfilRequest request) {
        Usuario atualizado = perfilService.atualizarPerfil(
                autorizacaoClube.usuarioAutenticado(), request.nome(), request.bio());
        return ResponseEntity.ok(UsuarioResponse.from(atualizado));
    }

    @Operation(
            summary = "Perfil de leitor",
            description = "O que está lendo, o que já terminou e os clubes em comum. Visível para o próprio usuário e para quem divide ao menos um clube com ele; para os demais, 404.")
    @ApiResponse(responseCode = "200", description = "Perfil")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado ou sem clube em comum")
    @GetMapping("/{id}/perfil")
    public ResponseEntity<PerfilResponse> perfil(@Parameter(description = "Id do usuário") @PathVariable Integer id) {
        return ResponseEntity.ok(perfilService.perfil(autorizacaoClube.usuarioAutenticado(), id));
    }
}
