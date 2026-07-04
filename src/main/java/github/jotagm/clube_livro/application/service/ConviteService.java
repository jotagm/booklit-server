package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.ConviteRepository;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.convite.Convite;
import github.jotagm.clube_livro.domain.clube.convite.ConviteStatus;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

        if (!convite.getEmailDestinatario().equals(emailUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }
        if(convite.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Convite expirado");
        }

        convite.setStatus(ConviteStatus.ACEITO);

        Usuario usuario = usuarioService.buscarPorEmail(emailUsuario);
        usuarioClubeService.adicionar(usuario, convite.getClube(), ClubePapel.MEMBRO);

        return conviteRepository.save(convite);
    }

    public Convite rejeitarConvite(UUID id, String emailUsuario) {
        Convite convite = buscarPorId(id);

        if (!convite.getEmailDestinatario().equals(emailUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        convite.setStatus(ConviteStatus.RECUSADO);
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
}