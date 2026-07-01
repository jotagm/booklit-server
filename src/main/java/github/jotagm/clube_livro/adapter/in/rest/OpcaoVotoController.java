package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.OpcaoVotoRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.OpcaoVotoResponse;
import github.jotagm.clube_livro.application.service.OpcaoVotoService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.application.service.VotacaoService;
import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/opcoes-voto")
@AllArgsConstructor
public class OpcaoVotoController {

    private final OpcaoVotoService opcaoVotoService;
    private final VotacaoService votacaoService;
    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<OpcaoVotoResponse> criar(@RequestBody @Valid OpcaoVotoRequest request) {
        OpcaoVoto opcaoVoto = new OpcaoVoto();
        opcaoVoto.setVotacao(votacaoService.buscarPorId(request.votacaoId()));
        opcaoVoto.setSugeridoPor(usuarioService.buscarPorId(request.sugeridoPorId()));
        opcaoVoto.setLivroGoogleId(request.livroGoogleId());
        opcaoVoto.setLivroTitulo(request.livroTitulo());
        opcaoVoto.setLivroCapaUrl(request.livroCapaUrl());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OpcaoVotoResponse.from(opcaoVotoService.salvar(opcaoVoto)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OpcaoVotoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(OpcaoVotoResponse.from(opcaoVotoService.buscarPorId(id)));
    }

    @GetMapping("/votacao/{votacaoId}")
    public ResponseEntity<List<OpcaoVotoResponse>> listarPorVotacao(@PathVariable UUID votacaoId) {
        return ResponseEntity.ok(opcaoVotoService.listarPorVotacao(votacaoId).stream()
                .map(OpcaoVotoResponse::from)
                .toList());
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<OpcaoVotoResponse>> listarPorUsuario(@PathVariable UUID usuarioId) {
        return ResponseEntity.ok(opcaoVotoService.listarPorUsuario(usuarioId).stream()
                .map(OpcaoVotoResponse::from)
                .toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<OpcaoVotoResponse> atualizar(@PathVariable UUID id,
                                                       @RequestBody @Valid OpcaoVotoRequest request) {
        OpcaoVoto opcaoVoto = opcaoVotoService.buscarPorId(id);
        opcaoVoto.setLivroGoogleId(request.livroGoogleId());
        opcaoVoto.setLivroTitulo(request.livroTitulo());
        opcaoVoto.setLivroCapaUrl(request.livroCapaUrl());

        return ResponseEntity.ok(OpcaoVotoResponse.from(opcaoVotoService.atualizar(opcaoVoto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        opcaoVotoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
