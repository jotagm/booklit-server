package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.ComentarioAtualizarRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.ComentarioRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.ComentarioResponse;
import github.jotagm.clube_livro.application.service.ComentarioService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.domain.clube.leitura.Comentario;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@Tag(name = "Comentários", description = "Discussões contextualizadas por leitura. A árvore tem no máximo um nível: respostas não podem ser respondidas.")
public class ComentarioController {

    private final ComentarioService comentarioService;
    private final UsuarioService usuarioService;

    @Operation(
            summary = "Cria um comentário em uma leitura",
            description = "Informe `comentarioPaiId` para responder a um comentário raiz; omita para criar um comentário raiz.")
    @ApiResponse(responseCode = "201", description = "Comentário criado")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado não é membro do clube")
    @ApiResponse(responseCode = "404", description = "Leitura ou comentário pai não encontrado")
    @ApiResponse(responseCode = "422", description = "Tentativa de responder a um comentário que já é uma resposta")
    @PostMapping("/leituras/{leituraId}/comentarios")
    public ResponseEntity<ComentarioResponse> criar(@Parameter(description = "Id da leitura") @PathVariable UUID leituraId,
                                                     @RequestBody @Valid ComentarioRequest request,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        Usuario autor = usuarioService.buscarPorEmail(userDetails.getUsername());
        Comentario comentario = comentarioService.criar(leituraId, request, autor);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ComentarioResponse.deFolha(comentario));
    }

    @Operation(
            summary = "Lista os comentários de uma leitura",
            description = "Retorna os comentários raiz já com suas respostas aninhadas em `respostas`.")
    @ApiResponse(responseCode = "200", description = "Árvore de comentários")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado não é membro do clube")
    @ApiResponse(responseCode = "404", description = "Leitura não encontrada")
    @GetMapping("/leituras/{leituraId}/comentarios")
    public ResponseEntity<List<ComentarioResponse>> listar(@Parameter(description = "Id da leitura") @PathVariable UUID leituraId,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        Usuario solicitante = usuarioService.buscarPorEmail(userDetails.getUsername());
        List<Comentario> comentarios = comentarioService.listarPorLeitura(leituraId, solicitante);

        return ResponseEntity.ok(ComentarioResponse.montarArvore(comentarios));
    }

    @Operation(summary = "Edita um comentário", description = "Restrito ao autor do comentário.")
    @ApiResponse(responseCode = "200", description = "Comentário atualizado")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado não é o autor")
    @ApiResponse(responseCode = "404", description = "Comentário não encontrado")
    @PutMapping("/comentarios/{id}")
    public ResponseEntity<ComentarioResponse> atualizar(@Parameter(description = "Id do comentário") @PathVariable UUID id,
                                                         @RequestBody @Valid ComentarioAtualizarRequest request,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        Usuario solicitante = usuarioService.buscarPorEmail(userDetails.getUsername());
        Comentario comentario = comentarioService.editar(id, request, solicitante);

        return ResponseEntity.ok(ComentarioResponse.deFolha(comentario));
    }

    @Operation(
            summary = "Remove um comentário",
            description = "Remoção lógica: o comentário passa a exibir `[comentário removido]` e preserva as respostas.")
    @ApiResponse(responseCode = "204", description = "Comentário removido")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado não é o autor")
    @ApiResponse(responseCode = "404", description = "Comentário não encontrado")
    @DeleteMapping("/comentarios/{id}")
    public ResponseEntity<Void> deletar(@Parameter(description = "Id do comentário") @PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        Usuario solicitante = usuarioService.buscarPorEmail(userDetails.getUsername());
        comentarioService.deletar(id, solicitante);

        return ResponseEntity.noContent().build();
    }
}
