package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.ReacaoRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.FeedItemResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.ReacoesResponse;
import github.jotagm.clube_livro.application.service.FeedService;
import github.jotagm.clube_livro.application.service.ReacaoService;
import github.jotagm.clube_livro.configs.AutorizacaoClube;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@Tag(name = "Feed", description = """
        O que está acontecendo nos clubes do usuário: progressos, conclusões, comentários, votações, \
        leituras novas e entradas de membros, do mais recente para o mais antigo. Cada item aceita \
        uma reação por pessoa.""")
public class FeedController {

    private final FeedService feedService;
    private final ReacaoService reacaoService;
    private final AutorizacaoClube autorizacaoClube;

    @Operation(summary = "Feed de atividade", description = "Sem `clubeId`, junta todos os clubes do usuário; com `clubeId`, só aquele (exige ser membro).")
    @ApiResponse(responseCode = "200", description = "Itens do feed")
    @GetMapping("/feed")
    public ResponseEntity<List<FeedItemResponse>> feed(
            @Parameter(description = "Filtra por clube") @RequestParam(required = false) UUID clubeId,
            @Parameter(description = "Quantidade de itens (1 a 100, padrão 40)") @RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(feedService.feed(autorizacaoClube.usuarioAutenticado(), clubeId, limite));
    }

    @Operation(summary = "Reage a um item do feed", description = "Uma reação por pessoa por item; reagir de novo troca a anterior. Só membros do clube.")
    @ApiResponse(responseCode = "200", description = "Reações atualizadas")
    @PutMapping("/reacoes")
    public ResponseEntity<ReacoesResponse> reagir(@RequestBody @Valid ReacaoRequest request) {
        return ResponseEntity.ok(reacaoService.reagir(
                autorizacaoClube.usuarioAutenticado(), request.clubeId(), request.alvoId(), request.tipo()));
    }

    @Operation(summary = "Remove a própria reação", description = "Idempotente: sem reação, nada acontece.")
    @ApiResponse(responseCode = "200", description = "Reações atualizadas")
    @DeleteMapping("/reacoes")
    public ResponseEntity<ReacoesResponse> remover(
            @Parameter(description = "Id do item do feed") @RequestParam String alvoId) {
        return ResponseEntity.ok(reacaoService.remover(autorizacaoClube.usuarioAutenticado(), alvoId));
    }
}
