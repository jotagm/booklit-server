package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.VotacaoRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.LeituraClubeResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.VotacaoResponse;
import github.jotagm.clube_livro.application.service.ClubeService;
import github.jotagm.clube_livro.application.service.VotacaoEnceramentoService;
import github.jotagm.clube_livro.application.service.VotacaoService;
import github.jotagm.clube_livro.configs.RequireLider;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Votações", description = "Votações para escolher o próximo livro do clube. O agendador encerra automaticamente as votações que passam de `dataEncerramento`.")
public class VotacaoController {

    private final VotacaoService votacaoService;
    private final ClubeService clubeService;
    private final VotacaoEnceramentoService votacaoEnceramentoService;

    @Operation(
            summary = "Abre uma votação no clube",
            description = "Restrito ao LIDER do clube. A votação nasce com status ABERTA.")
    @ApiResponse(responseCode = "201", description = "Votação criada")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado não é líder do clube")
    @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    @PostMapping
    @RequireLider("#request.clubeId()")
    public ResponseEntity<VotacaoResponse> criar(@RequestBody @Valid VotacaoRequest request) {
        Votacao votacao = new Votacao();
        votacao.setClube(clubeService.buscarPorId(request.clubeId()));
        votacao.setStatus(VotacaoStatus.ABERTA);
        votacao.setDataAbertura(request.dataAbertura());
        votacao.setDataEncerramento(request.dataEncerramento());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VotacaoResponse.from(votacaoService.salvar(votacao)));
    }

    @Operation(summary = "Busca uma votação por id")
    @ApiResponse(responseCode = "200", description = "Votação encontrada")
    @ApiResponse(responseCode = "404", description = "Votação não encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<VotacaoResponse> buscarPorId(@Parameter(description = "Id da votação") @PathVariable UUID id) {
        return ResponseEntity.ok(VotacaoResponse.from(votacaoService.buscarPorId(id)));
    }

    @Operation(summary = "Lista as votações de um clube")
    @ApiResponse(responseCode = "200", description = "Votações do clube")
    @GetMapping("/clube/{clubeId}")
    public ResponseEntity<List<VotacaoResponse>> listarPorClube(@Parameter(description = "Id do clube") @PathVariable UUID clubeId) {
        return ResponseEntity.ok(votacaoService.listarPorClube(clubeId).stream()
                .map(VotacaoResponse::from)
                .toList());
    }

    @Operation(
            summary = "Atualiza as datas de uma votação",
            description = "`clubeId` no corpo é ignorado: a votação permanece no clube em que foi criada.")
    @ApiResponse(responseCode = "200", description = "Votação atualizada")
    @ApiResponse(responseCode = "404", description = "Votação não encontrada")
    @PutMapping("/{id}")
    public ResponseEntity<VotacaoResponse> atualizar(@Parameter(description = "Id da votação") @PathVariable UUID id,
                                                     @RequestBody @Valid VotacaoRequest request) {
        Votacao votacao = votacaoService.buscarPorId(id);
        votacao.setDataAbertura(request.dataAbertura());
        votacao.setDataEncerramento(request.dataEncerramento());

        return ResponseEntity.ok(VotacaoResponse.from(votacaoService.atualizar(votacao)));
    }

    @Operation(summary = "Remove uma votação")
    @ApiResponse(responseCode = "204", description = "Votação removida")
    @ApiResponse(responseCode = "404", description = "Votação não encontrada")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@Parameter(description = "Id da votação") @PathVariable UUID id) {
        votacaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }


    @Operation(
            summary = "Encerra a votação e cria a leitura vencedora",
            description = """
                    Restrito ao LIDER do clube. Apura os votos — o voto do líder vale 2 — marca a votação
                    como ENCERRADA e cria a leitura do livro vencedor, que é o objeto retornado.
                    """)
    @ApiResponse(responseCode = "200", description = "Votação encerrada; retorna a leitura criada a partir do livro vencedor")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado não é líder do clube")
    @ApiResponse(responseCode = "404", description = "Votação não encontrada")
    @ApiResponse(responseCode = "422", description = "A votação não recebeu nenhum voto")
    @PostMapping("{id}/encerrar")
    @RequireLider("@votacaoService.buscarPorId(#id).clube.id")
    public ResponseEntity<LeituraClubeResponse> encerrarVotacao(@PathVariable UUID id) {
        return ResponseEntity.ok(LeituraClubeResponse.from(votacaoEnceramentoService.finalizarVotacao(id)));
    }
}
