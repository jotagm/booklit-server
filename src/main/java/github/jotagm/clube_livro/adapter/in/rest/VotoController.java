package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.VotoRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.VotoResponse;
import github.jotagm.clube_livro.application.service.OpcaoVotoService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.application.service.VotacaoService;
import github.jotagm.clube_livro.application.service.VotoService;
import github.jotagm.clube_livro.domain.clube.votacao.Voto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/votos")
@AllArgsConstructor
public class VotoController {

    private final VotoService votoService;
    private final VotacaoService votacaoService;
    private final OpcaoVotoService opcaoVotoService;
    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<VotoResponse> votar(@RequestBody @Valid VotoRequest request) {
        Voto voto = new Voto();
        voto.setVotacao(votacaoService.buscarPorId(request.votacaoId()));
        voto.setOpcaoVoto(opcaoVotoService.buscarPorId(request.opcaoVotoId()));
        voto.setUsuario(usuarioService.buscarPorId(request.usuarioId()));
        voto.setPeso(request.peso());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VotoResponse.from(votoService.salvar(voto)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VotoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(VotoResponse.from(votoService.buscarPorId(id)));
    }

    @GetMapping("/votacao/{votacaoId}")
    public ResponseEntity<List<VotoResponse>> listarPorVotacao(@PathVariable UUID votacaoId) {
        return ResponseEntity.ok(votoService.listarPorVotacao(votacaoId).stream()
                .map(VotoResponse::from)
                .toList());
    }

    @GetMapping("/opcao/{opcaoVotoId}")
    public ResponseEntity<List<VotoResponse>> listarPorOpcao(@PathVariable UUID opcaoVotoId) {
        return ResponseEntity.ok(votoService.listarPorOpcao(opcaoVotoId).stream()
                .map(VotoResponse::from)
                .toList());
    }

    @GetMapping("/votacao/{votacaoId}/usuario/{usuarioId}")
    public ResponseEntity<VotoResponse> buscarPorVotacaoEUsuario(
            @PathVariable UUID votacaoId,
            @PathVariable UUID usuarioId) {
        return ResponseEntity.ok(VotoResponse.from(
                votoService.buscarPorVotacaoEUsuario(votacaoId, usuarioId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        votoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
