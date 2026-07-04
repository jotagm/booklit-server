package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.LeituraClubeRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.GoogleBooksResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.LeituraClubeResponse;
import github.jotagm.clube_livro.adapter.out.client.GoogleBooksClient;
import github.jotagm.clube_livro.application.service.LeituraClubeService;
import github.jotagm.clube_livro.configs.RequireLider;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/leituras")
@AllArgsConstructor
public class LeituraClubeController {

    private final LeituraClubeService leituraClubeService;
    private final GoogleBooksClient googleBooksClient;

    @PostMapping
    @RequireLider("#request.clubeId()")
    public ResponseEntity<LeituraClubeResponse> criar(@RequestBody @Valid LeituraClubeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LeituraClubeResponse.from(leituraClubeService.criar(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeituraClubeResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(LeituraClubeResponse.from(leituraClubeService.buscarPorId(id)));
    }

    @GetMapping("/clube/{clubeId}")
    public ResponseEntity<List<LeituraClubeResponse>> listarPorClube(@PathVariable UUID clubeId) {
        return ResponseEntity.ok(leituraClubeService.listarPorClube(clubeId).stream()
                .map(LeituraClubeResponse::from)
                .toList());
    }

    @PutMapping("/{id}")
    @RequireLider("@leituraClubeService.buscarPorId(#id).clube.id")
    public ResponseEntity<LeituraClubeResponse> atualizar(@PathVariable UUID id,
                                                          @RequestBody @Valid LeituraClubeRequest request) {
        return ResponseEntity.ok(LeituraClubeResponse.from(leituraClubeService.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @RequireLider("@leituraClubeService.buscarPorId(#id).clube.id")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        leituraClubeService.deletar(id);
        return ResponseEntity.noContent().build();
    }

//    GET /leituras/clube/{clubeId}/ativa
    @GetMapping("/clube/{clubeId}/leitura-ativa")
    public ResponseEntity<LeituraClubeResponse> buscarLeituraAtiva(@PathVariable UUID clubeId) {
        return leituraClubeService.buscarLeituraAtiva(clubeId)
                .map(LeituraClubeResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/livros/buscar")
    public GoogleBooksResponse buscarLivroGoogle(@RequestParam String titulo,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return googleBooksClient.buscarLivroGoogleBooks(titulo, page, size);
    }
}
