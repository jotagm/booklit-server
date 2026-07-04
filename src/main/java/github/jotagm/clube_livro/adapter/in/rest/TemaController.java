package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.TemaRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.TemaResponse;
import github.jotagm.clube_livro.application.service.TemaService;
import github.jotagm.clube_livro.domain.tema.Tema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/temas")
@AllArgsConstructor
public class TemaController {

    private final TemaService temaService;

    @PostMapping
    public ResponseEntity<TemaResponse> criar(@RequestBody @Valid TemaRequest request) {
        Tema tema = new Tema();
        tema.setNome(request.nome());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TemaResponse.from(temaService.salvar(tema)));
    }

    @GetMapping
    public ResponseEntity<Page<TemaResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(temaService.listarTodos(pageable).map(TemaResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(TemaResponse.from(temaService.buscarPorId(id)));
    }

    @GetMapping("/nome/{nome}")
    public ResponseEntity<TemaResponse> buscarPorNome(@PathVariable String nome) {
        return ResponseEntity.ok(TemaResponse.from(temaService.buscarPorNome(nome)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemaResponse> atualizar(@PathVariable UUID id,
                                                  @RequestBody @Valid TemaRequest request) {
        Tema tema = temaService.buscarPorId(id);
        tema.setNome(request.nome());

        return ResponseEntity.ok(TemaResponse.from(temaService.atualizar(tema)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        temaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
