package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.VotacaoRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.LeituraClubeResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.VotacaoResponse;
import github.jotagm.clube_livro.application.service.ClubeService;
import github.jotagm.clube_livro.application.service.VotacaoEnceramentoService;
import github.jotagm.clube_livro.application.service.VotacaoService;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/votacoes")
@AllArgsConstructor
public class VotacaoController {

    private final VotacaoService votacaoService;
    private final ClubeService clubeService;
    private final VotacaoEnceramentoService votacaoEnceramentoService;

    @PostMapping
    public ResponseEntity<VotacaoResponse> criar(@RequestBody @Valid VotacaoRequest request) {
        Votacao votacao = new Votacao();
        votacao.setClube(clubeService.buscarPorId(request.clubeId()));
        votacao.setStatus(VotacaoStatus.ABERTA);
        votacao.setDataAbertura(request.dataAbertura());
        votacao.setDataEncerramento(request.dataEncerramento());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VotacaoResponse.from(votacaoService.salvar(votacao)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VotacaoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(VotacaoResponse.from(votacaoService.buscarPorId(id)));
    }

    @GetMapping("/clube/{clubeId}")
    public ResponseEntity<List<VotacaoResponse>> listarPorClube(@PathVariable UUID clubeId) {
        return ResponseEntity.ok(votacaoService.listarPorClube(clubeId).stream()
                .map(VotacaoResponse::from)
                .toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<VotacaoResponse> atualizar(@PathVariable UUID id,
                                                     @RequestBody @Valid VotacaoRequest request) {
        Votacao votacao = votacaoService.buscarPorId(id);
        votacao.setDataAbertura(request.dataAbertura());
        votacao.setDataEncerramento(request.dataEncerramento());

        return ResponseEntity.ok(VotacaoResponse.from(votacaoService.atualizar(votacao)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        votacaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("{id}/encerrar")
    public ResponseEntity<LeituraClubeResponse> encerrarVotacao(@PathVariable UUID id) {
        return ResponseEntity.ok(LeituraClubeResponse.from(votacaoEnceramentoService.finalizarVotacao(id)));
    }
}
