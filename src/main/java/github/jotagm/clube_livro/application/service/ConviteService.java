package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.ConviteRepository;
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
        usuarioClubeService.adicionar(usuario, convite.getClube(), ClubePapel.MEMBRO);

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

    public Convite buscarPorEmailDestinatario(String email) {
        return conviteRepository.findByEmailDestinatario(email);
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