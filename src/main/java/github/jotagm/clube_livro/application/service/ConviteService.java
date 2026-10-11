package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.ConviteRepository;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.convite.Convite;
import github.jotagm.clube_livro.domain.clube.convite.ConviteStatus;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class ConviteService {

    private final ConviteRepository conviteRepository;
    private final UsuarioClubeService usuarioClubeService;
    private final UsuarioService usuarioService;
    private final ClubeService clubeService;
    private final LeituraClubeService leituraClubeService;

    public Convite salvar(Convite convite) {
        return conviteRepository.save(convite);
    }

    public Convite buscarPorId(UUID id) {
        return conviteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Convite não encontrado"));
    }

    public Convite aceitarConvite(UUID id, String emailUsuario) {
        Convite convite = buscarPorId(id);

        if (!convite.pertenceA(emailUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }
        if (convite.estaExpirado(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Convite expirado");
        }

        convite.aceitar();

        Usuario usuario = usuarioService.buscarPorEmail(emailUsuario);
        adicionarComoMembro(usuario, convite.getClube());

        log.info("Convite aceito id={} clube={} usuario={}",
                convite.getId(), convite.getClube().getId(), emailUsuario);

        return conviteRepository.save(convite);
    }

    public Convite rejeitarConvite(UUID id, String emailUsuario) {
        Convite convite = buscarPorId(id);

        if (!convite.pertenceA(emailUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        convite.recusar();
        log.info("Convite recusado id={} clube={} usuario={}", convite.getId(), convite.getClube().getId(), emailUsuario);
        return conviteRepository.save(convite);
    }

    /** Convite mais recente para o e-mail — um mesmo e-mail pode ter sido convidado por vários clubes. */
    public Convite buscarPorEmailDestinatario(String email) {
        return conviteRepository.findByEmailDestinatarioIgnoreCaseOrderByCreatedAtDesc(email).stream()
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhum convite para este e-mail"));
    }

    /** Convites pendentes e ainda válidos endereçados ao e-mail do usuário — a caixa de entrada dele. */
    public List<Convite> listarPendentesDoUsuario(String email) {
        LocalDateTime agora = LocalDateTime.now();
        return conviteRepository
                .findByEmailDestinatarioIgnoreCaseAndStatusOrderByCreatedAtDesc(email, ConviteStatus.PENDENTE)
                .stream()
                .filter(c -> !c.estaExpirado(agora))
                .toList();
    }

    /**
     * Entrada pelo link de convite. Idempotente: quem já é membro só é levado ao clube, sem
     * erro — o mesmo link costuma ser clicado mais de uma vez no grupo.
     */
    public Clube entrarPorCodigo(String codigo, Usuario usuario) {
        Clube clube = clubeService.buscarPorCodigoConvite(codigo);
        boolean jaMembro = usuarioClubeService.listarPorClube(clube.getId()).stream()
                .anyMatch(uc -> uc.getUsuario().getId().equals(usuario.getId()));
        if (!jaMembro) {
            adicionarComoMembro(usuario, clube);
            log.info("Entrada por link clube={} usuario={}", clube.getId(), usuario.getEmail());
        }
        return clube;
    }

    private void adicionarComoMembro(Usuario usuario, Clube clube) {
        usuarioClubeService.adicionar(usuario, clube, ClubePapel.MEMBRO);
        leituraClubeService.iniciarRegistroNaLeituraAtiva(clube.getId(), usuario);
    }

    public List<Convite> listarPorClube(UUID clubeId) {
        return conviteRepository.findByClubeId(clubeId);
    }

    public Convite atualizar(Convite convite) {
        return conviteRepository.save(convite);
    }

    public void deletar(UUID id) {
        conviteRepository.deleteById(id);
    }

    public int expirarVencidos() {
        List<Convite> vencidos = conviteRepository.findByStatusAndExpiraEmBefore(ConviteStatus.PENDENTE, LocalDateTime.now());
        vencidos.forEach(Convite::expirar);
        conviteRepository.saveAll(vencidos);
        if (!vencidos.isEmpty()) {
            log.info("Convites expirados automaticamente: {}", vencidos.size());
        }
        return vencidos.size();
    }
}