package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.ClubeRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.ClubeResponse;
import github.jotagm.clube_livro.application.service.ClubeService;
import github.jotagm.clube_livro.application.service.TemaService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.configs.RequireLider;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/clubes")
@AllArgsConstructor
@Tag(name = "Clubes", description = "Criação e gestão dos clubes de leitura")
public class ClubeController {

    private final ClubeService clubeService;
    private final TemaService temaService;
    private final UsuarioService usuarioService;

    @Operation(
            summary = "Cria um clube",
            description = "O usuário autenticado é registrado automaticamente como LIDER do clube.")
    @ApiResponse(responseCode = "201", description = "Clube criado")
    @ApiResponse(responseCode = "404", description = "Algum tema informado em `temaIds` não existe")
    @PostMapping()
    public ResponseEntity<ClubeResponse> criar(@AuthenticationPrincipal UserDetails userDetails, @RequestBody @Valid ClubeRequest request) {
        Usuario criador = usuarioService.buscarPorEmail(userDetails.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClubeResponse.from(clubeService.criar(request, criador)));
    }

    @Operation(
            summary = "Lista os clubes visíveis para o usuário autenticado",
            description = """
                    Retorna os clubes públicos somados aos clubes privados em que o usuário é membro.
                    Aceita os parâmetros de paginação padrão do Spring: `page`, `size` e `sort`.
                    """)
    @ApiResponse(responseCode = "200", description = "Página de clubes")
    @GetMapping
    public ResponseEntity<Page<ClubeResponse>> listar(@AuthenticationPrincipal UserDetails userDetails, Pageable pageable) {
        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());

        return ResponseEntity.ok(clubeService.listarVisiveis(usuario.getId(), pageable)
                .map(ClubeResponse::from));
    }

    @Operation(summary = "Busca um clube por id")
    @ApiResponse(responseCode = "200", description = "Clube encontrado")
    @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ClubeResponse> buscarPorId(
            @Parameter(description = "Id do clube") @PathVariable UUID id) {
        return ResponseEntity.ok(ClubeResponse.from(clubeService.buscarPorId(id)));
    }

    @Operation(summary = "Busca um clube por nome")
    @ApiResponse(responseCode = "200", description = "Clube encontrado")
    @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    @GetMapping("/nome/{nome}")
    public ResponseEntity<ClubeResponse> buscarPorNome(
            @Parameter(description = "Nome exato do clube") @PathVariable String nome) {
        return ResponseEntity.ok(ClubeResponse.from(clubeService.buscarPorNome(nome)));
    }

    @Operation(
            summary = "Atualiza um clube",
            description = "Restrito ao LIDER do clube. `temaIds` nulo mantém os temas atuais.")
    @ApiResponse(responseCode = "200", description = "Clube atualizado")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado não é líder do clube")
    @ApiResponse(responseCode = "404", description = "Clube ou tema não encontrado")
    @PutMapping("/{id}")
    @RequireLider("#id")
    public ResponseEntity<ClubeResponse> atualizar(
            @Parameter(description = "Id do clube") @PathVariable UUID id,
            @RequestBody @Valid ClubeRequest request) {
        Clube clube = clubeService.buscarPorId(id);
        clube.setNome(request.nome());
        clube.setDescricao(request.descricao());
        clube.setPrivado(request.privado());

        if (request.temaIds() != null) {
            clube.setTemas(request.temaIds().stream()
                    .map(temaService::buscarPorId)
                    .toList());
        }

        return ResponseEntity.ok(ClubeResponse.from(clubeService.atualizar(clube)));
    }

    @Operation(summary = "Remove um clube", description = "Restrito ao LIDER do clube.")
    @ApiResponse(responseCode = "204", description = "Clube removido")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado não é líder do clube")
    @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    @DeleteMapping("/{id}")
    @RequireLider("#id")
    public ResponseEntity<Void> deletar(
            @Parameter(description = "Id do clube") @PathVariable UUID id) {
        clubeService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
