package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.AvaliacaoRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.LeituraPessoalRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.MetaLeituraRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.ProgressoPessoalRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.StatusLeituraPessoalRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.EstanteResumoResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.LeituraPessoalResponse;
import github.jotagm.clube_livro.application.service.EstanteService;
import github.jotagm.clube_livro.configs.AutorizacaoClube;
import github.jotagm.clube_livro.domain.estante.StatusLeituraPessoal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/estante")
@AllArgsConstructor
@Tag(name = "Estante pessoal", description = """
        Livros que o usuário lê por conta própria, fora dos clubes: quero ler, lendo, lido, \
        abandonado. Tudo aqui é do usuário autenticado — não há como ver a estante de outra pessoa. \
        O progresso registrado nas leituras dos clubes é espelhado aqui automaticamente.""")
public class EstanteController {

    private final EstanteService estanteService;
    private final AutorizacaoClube autorizacaoClube;

    @Operation(summary = "Lista a estante", description = "Mais recentes primeiro. `status` filtra por situação.")
    @ApiResponse(responseCode = "200", description = "Livros da estante")
    @GetMapping
    public ResponseEntity<List<LeituraPessoalResponse>> listar(
            @Parameter(description = "Filtra por status") @RequestParam(required = false) StatusLeituraPessoal status) {
        return ResponseEntity.ok(estanteService.listar(autorizacaoClube.usuarioAutenticado(), status).stream()
                .map(LeituraPessoalResponse::from)
                .toList());
    }

    @Operation(summary = "Adiciona um livro à estante", description = "Sem `status`, entra como QUERO_LER. Um livro só aparece uma vez na estante.")
    @ApiResponse(responseCode = "201", description = "Livro adicionado")
    @ApiResponse(responseCode = "409", description = "O livro já está na estante")
    @PostMapping
    public ResponseEntity<LeituraPessoalResponse> adicionar(@RequestBody @Valid LeituraPessoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(LeituraPessoalResponse.from(
                estanteService.adicionar(autorizacaoClube.usuarioAutenticado(), request)));
    }

    @Operation(summary = "Resumo do ano", description = "Meta anual, livros lidos no ano e contagem do que está em andamento. Sem `ano`, usa o ano atual.")
    @ApiResponse(responseCode = "200", description = "Resumo")
    @GetMapping("/resumo")
    public ResponseEntity<EstanteResumoResponse> resumo(@RequestParam(required = false) Integer ano) {
        int anoConsultado = ano != null ? ano : LocalDate.now().getYear();
        return ResponseEntity.ok(estanteService.resumo(autorizacaoClube.usuarioAutenticado(), anoConsultado));
    }

    @Operation(summary = "Define a meta de livros do ano")
    @ApiResponse(responseCode = "200", description = "Resumo atualizado com a nova meta")
    @PutMapping("/meta/{ano}")
    public ResponseEntity<EstanteResumoResponse> definirMeta(@PathVariable int ano,
                                                             @RequestBody @Valid MetaLeituraRequest request) {
        var usuario = autorizacaoClube.usuarioAutenticado();
        estanteService.definirMeta(usuario, ano, request.quantidadeLivros());
        return ResponseEntity.ok(estanteService.resumo(usuario, ano));
    }

    @Operation(summary = "Busca um livro da estante")
    @ApiResponse(responseCode = "200", description = "Livro encontrado")
    @ApiResponse(responseCode = "404", description = "Não está na estante do usuário")
    @GetMapping("/{id}")
    public ResponseEntity<LeituraPessoalResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(LeituraPessoalResponse.from(
                estanteService.buscar(id, autorizacaoClube.usuarioAutenticado())));
    }

    @Operation(summary = "Muda o status", description = "LENDO marca o início; LIDO marca o fim e completa as páginas; QUERO_LER zera a leitura.")
    @ApiResponse(responseCode = "200", description = "Status alterado")
    @PutMapping("/{id}/status")
    public ResponseEntity<LeituraPessoalResponse> mudarStatus(@PathVariable UUID id,
                                                              @RequestBody @Valid StatusLeituraPessoalRequest request) {
        return ResponseEntity.ok(LeituraPessoalResponse.from(
                estanteService.mudarStatus(id, autorizacaoClube.usuarioAutenticado(), request.status())));
    }

    @Operation(summary = "Registra a página atual", description = "Avançar começa a leitura; chegar na última página marca como lido.")
    @ApiResponse(responseCode = "200", description = "Progresso registrado")
    @ApiResponse(responseCode = "422", description = "Página maior que o total do livro")
    @PutMapping("/{id}/progresso")
    public ResponseEntity<LeituraPessoalResponse> progresso(@PathVariable UUID id,
                                                            @RequestBody @Valid ProgressoPessoalRequest request) {
        return ResponseEntity.ok(LeituraPessoalResponse.from(estanteService.atualizarProgresso(
                id, autorizacaoClube.usuarioAutenticado(), request.paginaAtual(), request.totalPaginas())));
    }

    @Operation(summary = "Avalia o livro", description = "Nota de 1 a 5 e resenha curta; nulo limpa.")
    @ApiResponse(responseCode = "200", description = "Avaliação salva")
    @PutMapping("/{id}/avaliacao")
    public ResponseEntity<LeituraPessoalResponse> avaliar(@PathVariable UUID id,
                                                          @RequestBody @Valid AvaliacaoRequest request) {
        return ResponseEntity.ok(LeituraPessoalResponse.from(estanteService.avaliar(
                id, autorizacaoClube.usuarioAutenticado(), request.nota(), request.resenha())));
    }

    @Operation(summary = "Remove um livro da estante")
    @ApiResponse(responseCode = "204", description = "Removido")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        estanteService.remover(id, autorizacaoClube.usuarioAutenticado());
        return ResponseEntity.noContent().build();
    }
}
