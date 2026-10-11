package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.ConviteRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.ClubeResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.ConvitePreviewResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.ConviteResponse;
import github.jotagm.clube_livro.application.service.ClubeService;
import github.jotagm.clube_livro.application.service.ConviteService;
import github.jotagm.clube_livro.application.service.LeituraClubeService;
import github.jotagm.clube_livro.application.service.UsuarioClubeService;
import github.jotagm.clube_livro.configs.AutorizacaoClube;
import github.jotagm.clube_livro.configs.RequireLider;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.convite.Convite;
import github.jotagm.clube_livro.domain.exceptions.AcessoNegadoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/convites")
@AllArgsConstructor
@Tag(name = "Convites", description = """
        Duas formas de entrar em um clube: convite por e-mail (aparece em `GET /convites/meus` \
        para o destinatário) e link de convite do clube (`/convites/link/{codigo}`), pensado para \
        ser colado no grupo do WhatsApp. Convites por e-mail pendentes expiram pelo agendador.""")
public class ConviteController {

    private final ConviteService conviteService;
    private final ClubeService clubeService;
    private final UsuarioClubeService usuarioClubeService;
    private final LeituraClubeService leituraClubeService;
    private final AutorizacaoClube autorizacaoClube;

    @Operation(
            summary = "Cria um convite por e-mail",
            description = "Restrito ao LIDER do clube. O convite nasce PENDENTE e aparece para o destinatário em `GET /convites/meus` quando ele entra com esse e-mail.")
    @ApiResponse(responseCode = "201", description = "Convite criado")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado não é líder do clube")
    @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    @PostMapping
    @RequireLider("#request.clubeId()")
    public ResponseEntity<ConviteResponse> criar(@RequestBody @Valid ConviteRequest request) {
        Convite convite = Convite.novo(
                clubeService.buscarPorId(request.clubeId()),
                autorizacaoClube.usuarioAutenticado(),
                request.emailDestinatario().trim().toLowerCase(),
                request.expiraEm());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ConviteResponse.from(conviteService.salvar(convite)));
    }

    @Operation(summary = "Lista os convites pendentes do usuário autenticado", description = "Convites por e-mail ainda válidos endereçados ao e-mail da conta.")
    @ApiResponse(responseCode = "200", description = "Convites pendentes")
    @GetMapping("/meus")
    public ResponseEntity<List<ConviteResponse>> meus() {
        Usuario usuario = autorizacaoClube.usuarioAutenticado();
        return ResponseEntity.ok(conviteService.listarPendentesDoUsuario(usuario.getEmail()).stream()
                .map(ConviteResponse::from)
                .toList());
    }

    @Operation(summary = "Prévia pública do link de convite", description = "Endpoint público: é a página que o convidado vê antes de criar conta.")
    @ApiResponse(responseCode = "200", description = "Dados do clube")
    @ApiResponse(responseCode = "404", description = "Link inválido ou regenerado pelo líder")
    @SecurityRequirements
    @GetMapping("/link/{codigo}")
    public ResponseEntity<ConvitePreviewResponse> previewLink(@Parameter(description = "Código do link") @PathVariable String codigo) {
        Clube clube = clubeService.buscarPorCodigoConvite(codigo);
        List<UsuarioClube> membros = usuarioClubeService.listarPorClube(clube.getId());
        String nomeLider = membros.stream()
                .filter(m -> m.getPapel() == ClubePapel.LIDER)
                .map(m -> m.getUsuario().getNome())
                .findFirst()
                .orElse(null);
        ConvitePreviewResponse.LeituraAtual leituraAtual = leituraClubeService.buscarLeituraAtiva(clube.getId())
                .map(l -> new ConvitePreviewResponse.LeituraAtual(l.getLivroTitulo(), l.getLivroCapaUrl()))
                .orElse(null);
        List<String> temas = clube.getTemas() == null ? List.of()
                : clube.getTemas().stream().map(t -> t.getNome()).toList();

        return ResponseEntity.ok(new ConvitePreviewResponse(
                codigo, clube.getNome(), clube.getDescricao(), nomeLider, membros.size(), temas, leituraAtual));
    }

    @Operation(summary = "Entra no clube pelo link de convite", description = "O usuário autenticado vira MEMBRO. Idempotente: quem já é membro recebe o clube sem erro.")
    @ApiResponse(responseCode = "200", description = "Clube em que o usuário entrou")
    @ApiResponse(responseCode = "404", description = "Link inválido ou regenerado pelo líder")
    @PostMapping("/link/{codigo}/entrar")
    public ResponseEntity<ClubeResponse> entrarPorLink(@Parameter(description = "Código do link") @PathVariable String codigo) {
        Clube clube = conviteService.entrarPorCodigo(codigo, autorizacaoClube.usuarioAutenticado());
        return ResponseEntity.ok(ClubeResponse.from(clube));
    }

    @Operation(summary = "Busca um convite por id", description = "Visível para o destinatário e para o líder do clube.")
    @ApiResponse(responseCode = "200", description = "Convite encontrado")
    @ApiResponse(responseCode = "403", description = "O convite não é do usuário nem de um clube que ele lidera")
    @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ConviteResponse> buscarPorId(@Parameter(description = "Id do convite") @PathVariable UUID id) {
        Convite convite = conviteService.buscarPorId(id);
        Usuario usuario = autorizacaoClube.usuarioAutenticado();
        boolean destinatario = convite.getEmailDestinatario().equalsIgnoreCase(usuario.getEmail());
        if (!destinatario && !autorizacaoClube.isLider(usuario.getId(), convite.getClube().getId())) {
            throw new AcessoNegadoException("Acesso negado: este convite não é seu");
        }
        return ResponseEntity.ok(ConviteResponse.from(convite));
    }

    @Operation(summary = "Busca o convite mais recente para o e-mail", description = "Só o próprio e-mail pode ser consultado. Prefira `GET /convites/meus`.")
    @ApiResponse(responseCode = "200", description = "Convite encontrado")
    @ApiResponse(responseCode = "404", description = "Nenhum convite para o e-mail informado")
    @GetMapping("/email/{email}")
    public ResponseEntity<ConviteResponse> buscarPorEmail(@Parameter(description = "E-mail do destinatário") @PathVariable String email) {
        if (!autorizacaoClube.usuarioAutenticado().getEmail().equalsIgnoreCase(email)) {
            throw new AcessoNegadoException("Acesso negado: só é possível consultar os próprios convites");
        }
        return ResponseEntity.ok(ConviteResponse.from(conviteService.buscarPorEmailDestinatario(email)));
    }

    @Operation(summary = "Lista os convites de um clube", description = "Restrito ao LIDER do clube.")
    @ApiResponse(responseCode = "200", description = "Convites do clube")
    @GetMapping("/clube/{clubeId}")
    @RequireLider("#clubeId")
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
    public ResponseEntity<ConviteResponse> atualizarConviteAceito(@PathVariable UUID id) {
        Convite convite = conviteService.aceitarConvite(id, autorizacaoClube.usuarioAutenticado().getEmail());
        return ResponseEntity.ok(ConviteResponse.from(convite));
    }

    @Operation(summary = "Recusa um convite", description = "Move o convite para RECUSADO.")
    @ApiResponse(responseCode = "200", description = "Convite recusado")
    @ApiResponse(responseCode = "403", description = "O convite não pertence ao usuário autenticado")
    @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    @PutMapping("/{id}/recusado")
    public ResponseEntity<ConviteResponse> atualizarConviteRecusado(@PathVariable UUID id) {
        Convite convite = conviteService.rejeitarConvite(id, autorizacaoClube.usuarioAutenticado().getEmail());
        return ResponseEntity.ok(ConviteResponse.from(convite));
    }

    @Operation(summary = "Remove um convite", description = "Restrito ao LIDER do clube do convite.")
    @ApiResponse(responseCode = "204", description = "Convite removido")
    @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    @DeleteMapping("/{id}")
    @RequireLider("@conviteService.buscarPorId(#id).clube.id")
    public ResponseEntity<Void> deletar(@Parameter(description = "Id do convite") @PathVariable UUID id) {
        conviteService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
