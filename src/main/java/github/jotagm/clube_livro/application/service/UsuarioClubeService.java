package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.UsuarioClubeRepository;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UsuarioClubeService {

    private final UsuarioClubeRepository usuarioClubeRepository;

    public UsuarioClube adicionar(Usuario usuario, Clube clube, ClubePapel papel) {
        UsuarioClube usuarioClube = UsuarioClube.builder()
                .usuario(usuario)
                .clube(clube)
                .papel(papel)
                .entrouEm(LocalDateTime.now())
                .build();

        return usuarioClubeRepository.save(usuarioClube);
    }

    public UsuarioClube adicionarLider(Usuario usuario, Clube clube) {
        return adicionar(usuario, clube, ClubePapel.LIDER);
    }

    public UsuarioClube buscarPorId(UUID id) {
        return usuarioClubeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Membro não encontrado"));
    }

    public List<UsuarioClube> listarPorUsuario(UUID usuarioId) {
        return usuarioClubeRepository.findByUsuarioId(usuarioId);
    }

    public List<UsuarioClube> listarPorClube(UUID clubeId) {
        return usuarioClubeRepository.findByClubeId(clubeId);
    }

    public UsuarioClube buscarPorUsuarioEClube(UUID usuarioId, UUID clubeId) {
        return usuarioClubeRepository.findByUsuarioIdAndClubeId(usuarioId, clubeId)
                .orElseThrow(() -> new RuntimeException("Usuário não é membro deste clube"));
    }

    public UsuarioClube atualizar(UsuarioClube usuarioClube) {
        return usuarioClubeRepository.save(usuarioClube);
    }

    public void deletar(UUID id) {
        usuarioClubeRepository.deleteById(id);
    }
}
