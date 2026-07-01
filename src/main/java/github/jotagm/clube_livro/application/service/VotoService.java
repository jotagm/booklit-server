package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.VotoRepository;
import github.jotagm.clube_livro.domain.clube.votacao.Voto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class VotoService {

    private final VotoRepository votoRepository;

    public Voto salvar(Voto voto) {
        return votoRepository.save(voto);
    }

    public Voto buscarPorId(UUID id) {
        return votoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voto não encontrado"));
    }

    public List<Voto> listarPorVotacao(UUID votacaoId) {
        return votoRepository.findByVotacaoId(votacaoId);
    }

    public List<Voto> listarPorOpcao(UUID opcaoVotoId) {
        return votoRepository.findByOpcaoVotoId(opcaoVotoId);
    }

    public Voto buscarPorVotacaoEUsuario(UUID votacaoId, UUID usuarioId) {
        return votoRepository.findByVotacaoIdAndUsuarioId(votacaoId, usuarioId)
                .orElseThrow(() -> new RuntimeException("Voto não encontrado para este usuário nesta votação"));
    }

    public Voto atualizar(Voto voto) {
        return votoRepository.save(voto);
    }

    public void deletar(UUID id) {
        votoRepository.deleteById(id);
    }
}
