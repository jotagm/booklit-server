package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.ComentarioAtualizarRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.ComentarioRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.ComentarioResponse;
import github.jotagm.clube_livro.application.service.ComentarioService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.domain.clube.leitura.Comentario;
import github.jotagm.clube_livro.domain.usuario.Usuario;
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
public class ComentarioController {

    private final ComentarioService comentarioService;
    private final UsuarioService usuarioService;

    @PostMapping("/leituras/{leituraId}/comentarios")
    public ResponseEntity<ComentarioResponse> criar(@PathVariable UUID leituraId,
                                                     @RequestBody @Valid ComentarioRequest request,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        Usuario autor = usuarioService.buscarPorEmail(userDetails.getUsername());
        Comentario comentario = comentarioService.criar(leituraId, request, autor);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ComentarioResponse.deFolha(comentario));
    }

    @GetMapping("/leituras/{leituraId}/comentarios")
    public ResponseEntity<List<ComentarioResponse>> listar(@PathVariable UUID leituraId,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        Usuario solicitante = usuarioService.buscarPorEmail(userDetails.getUsername());
        List<Comentario> comentarios = comentarioService.listarPorLeitura(leituraId, solicitante);

        return ResponseEntity.ok(ComentarioResponse.montarArvore(comentarios));
    }

    @PutMapping("/comentarios/{id}")
    public ResponseEntity<ComentarioResponse> atualizar(@PathVariable UUID id,
                                                         @RequestBody @Valid ComentarioAtualizarRequest request,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        Usuario solicitante = usuarioService.buscarPorEmail(userDetails.getUsername());
        Comentario comentario = comentarioService.editar(id, request, solicitante);

        return ResponseEntity.ok(ComentarioResponse.deFolha(comentario));
    }

    @DeleteMapping("/comentarios/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        Usuario solicitante = usuarioService.buscarPorEmail(userDetails.getUsername());
        comentarioService.deletar(id, solicitante);

        return ResponseEntity.noContent().build();
    }
}
