package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.ClubeRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.ClubeResponse;
import github.jotagm.clube_livro.application.service.ClubeService;
import github.jotagm.clube_livro.application.service.TemaService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.configs.RequireLider;
import github.jotagm.clube_livro.domain.clube.Clube;
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
@RequestMapping("/clubes")
@AllArgsConstructor
public class ClubeController {

    private final ClubeService clubeService;
    private final TemaService temaService;
    private final UsuarioService usuarioService;

    @PostMapping()
    public ResponseEntity<ClubeResponse> criar(@AuthenticationPrincipal UserDetails userDetails, @RequestBody @Valid ClubeRequest request) {
        Usuario criador = usuarioService.buscarPorEmail(userDetails.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClubeResponse.from(clubeService.criar(request, criador)));
    }

    @GetMapping
    public ResponseEntity<List<ClubeResponse>> listar() {
        return ResponseEntity.ok(clubeService.listarTodos().stream()
                .map(ClubeResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClubeResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ClubeResponse.from(clubeService.buscarPorId(id)));
    }

    @GetMapping("/nome/{nome}")
    public ResponseEntity<ClubeResponse> buscarPorNome(@PathVariable String nome) {
        return ResponseEntity.ok(ClubeResponse.from(clubeService.buscarPorNome(nome)));
    }

    @PutMapping("/{id}")
    @RequireLider("#id")
    public ResponseEntity<ClubeResponse> atualizar(@PathVariable UUID id,
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

    @DeleteMapping("/{id}")
    @RequireLider("#id")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        clubeService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
