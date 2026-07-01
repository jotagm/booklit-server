package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.UsuarioClubeRepository;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UsuarioClubeService {

    private final UsuarioClubeRepository usuarioClubeRepository;

    public UsuarioClube salvar(UsuarioClube usuarioClube) {
        return usuarioClubeRepository.save(usuarioClube);
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
