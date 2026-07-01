package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.ConviteRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.ConviteResponse;
import github.jotagm.clube_livro.application.service.ClubeService;
import github.jotagm.clube_livro.application.service.ConviteService;
import github.jotagm.clube_livro.application.service.UsuarioClubeService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.convite.Convite;
import github.jotagm.clube_livro.domain.clube.convite.ConviteStatus;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/convites")
@AllArgsConstructor
public class ConviteController {

    private final ConviteService conviteService;
    private final ClubeService clubeService;
    private final UsuarioService usuarioService;
    private final UsuarioClubeService usuarioClubeService;

    @PostMapping
    public ResponseEntity<ConviteResponse> criar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ConviteRequest request) {
        var usuarioConvite = usuarioService.buscarPorEmail(userDetails.getUsername());
        Convite convite = new Convite();
        convite.setClube(clubeService.buscarPorId(request.clubeId()));
        convite.setConvidadoPor(usuarioConvite);
        convite.setEmailDestinatario(request.emailDestinatario());
        convite.setStatus(ConviteStatus.PENDENTE);
        convite.setExpiraEm(request.expiraEm());
        convite.setCreatedAt(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ConviteResponse.from(conviteService.salvar(convite)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConviteResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ConviteResponse.from(conviteService.buscarPorId(id)));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ConviteResponse> buscarPorEmail(@PathVariable String email) {
        return ResponseEntity.ok(ConviteResponse.from(conviteService.buscarPorEmailDestinatario(email)));
    }

    @GetMapping("/clube/{clubeId}")
    public ResponseEntity<List<ConviteResponse>> listarPorClube(@PathVariable UUID clubeId) {
        return ResponseEntity.ok(conviteService.listarPorClube(clubeId).stream()
                .map(ConviteResponse::from)
                .toList());
    }

    @PutMapping("/{id}/aceitar")
    public ResponseEntity<ConviteResponse> atualizarConviteAceito(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Convite convite = conviteService.aceitarConvite(id, userDetails.getUsername());
        return ResponseEntity.ok(ConviteResponse.from(convite));
    }
    @PutMapping("/{id}/recusado")
    public ResponseEntity<ConviteResponse> atualizarConviteRecusado(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Convite convite = conviteService.rejeitarConvite(id, userDetails.getUsername());
        return ResponseEntity.ok(ConviteResponse.from(convite));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        conviteService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
