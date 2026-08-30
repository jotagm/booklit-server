package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.ConviteRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.ConviteResponse;
import github.jotagm.clube_livro.application.service.ClubeService;
import github.jotagm.clube_livro.application.service.ConviteService;
import github.jotagm.clube_livro.application.service.UsuarioClubeService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.domain.clube.convite.Convite;
import github.jotagm.clube_livro.domain.clube.convite.ConviteStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Convites", description = "Convites por e-mail para entrar em um clube. Convites pendentes expiram automaticamente pelo agendador.")
public class ConviteController {

    private final ConviteService conviteService;
    private final ClubeService clubeService;
    private final UsuarioService usuarioService;

    @Operation(
            summary = "Cria um convite para um clube",
            description = "O usuário autenticado é registrado como remetente. O convite nasce com status PENDENTE e expira em `expiraEm`.")
    @ApiResponse(responseCode = "201", description = "Convite criado")
    @ApiResponse(responseCode = "404", description = "Clube não encontrado")
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

    @Operation(summary = "Busca um convite por id")
    @ApiResponse(responseCode = "200", description = "Convite encontrado")
    @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ConviteResponse> buscarPorId(@Parameter(description = "Id do convite") @PathVariable UUID id) {
        return ResponseEntity.ok(ConviteResponse.from(conviteService.buscarPorId(id)));
    }

    @Operation(summary = "Busca um convite pelo e-mail do destinatário")
    @ApiResponse(responseCode = "200", description = "Convite encontrado")
    @ApiResponse(responseCode = "404", description = "Nenhum convite para o e-mail informado")
    @GetMapping("/email/{email}")
    public ResponseEntity<ConviteResponse> buscarPorEmail(@Parameter(description = "E-mail do destinatário") @PathVariable String email) {
        return ResponseEntity.ok(ConviteResponse.from(conviteService.buscarPorEmailDestinatario(email)));
    }

    @Operation(summary = "Lista os convites de um clube")
    @ApiResponse(responseCode = "200", description = "Convites do clube")
    @GetMapping("/clube/{clubeId}")
    public ResponseEntity<List<ConviteResponse>> listarPorClube(@Parameter(description = "Id do clube") @PathVariable UUID clubeId) {
        return ResponseEntity.ok(conviteService.listarPorClube(clubeId).stream()
                .map(ConviteResponse::from)
                .toList());
    }

    @Operation(
            summary = "Aceita um convite",
            description = "Move o convite para ACEITO e vincula o usuário autenticado ao clube como MEMBRO.")
    @ApiResponse(responseCode = "200", description = "Convite aceito")
    @ApiResponse(responseCode = "403", description = "O convite não pertence ao usuário autenticado ou já expirou")
    @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    @PutMapping("/{id}/aceitar")
    public ResponseEntity<ConviteResponse> atualizarConviteAceito(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Convite convite = conviteService.aceitarConvite(id, userDetails.getUsername());
        return ResponseEntity.ok(ConviteResponse.from(convite));
    }

    @Operation(summary = "Recusa um convite", description = "Move o convite para RECUSADO.")
    @ApiResponse(responseCode = "200", description = "Convite recusado")
    @ApiResponse(responseCode = "403", description = "O convite não pertence ao usuário autenticado")
    @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    @PutMapping("/{id}/recusado")
    public ResponseEntity<ConviteResponse> atualizarConviteRecusado(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Convite convite = conviteService.rejeitarConvite(id, userDetails.getUsername());
        return ResponseEntity.ok(ConviteResponse.from(convite));
    }

    @Operation(summary = "Remove um convite")
    @ApiResponse(responseCode = "204", description = "Convite removido")
    @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@Parameter(description = "Id do convite") @PathVariable UUID id) {
        conviteService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
