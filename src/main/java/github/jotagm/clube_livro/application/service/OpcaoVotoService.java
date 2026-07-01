package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.OpcaoVotoRepository;
import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OpcaoVotoService {

    private final OpcaoVotoRepository opcaoVotoRepository;

    public OpcaoVoto salvar(OpcaoVoto opcaoVoto) {
        return opcaoVotoRepository.save(opcaoVoto);
    }

    public OpcaoVoto buscarPorId(UUID id) {
        return opcaoVotoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Opção de voto não encontrada"));
    }

    public List<OpcaoVoto> listarPorVotacao(UUID votacaoId) {
        return opcaoVotoRepository.findByVotacaoId(votacaoId);
    }

    public List<OpcaoVoto> listarPorUsuario(UUID usuarioId) {
        return opcaoVotoRepository.findBySugeridoPorId(usuarioId);
    }

    public OpcaoVoto atualizar(OpcaoVoto opcaoVoto) {
        return opcaoVotoRepository.save(opcaoVoto);
    }

    public void deletar(UUID id) {
        opcaoVotoRepository.deleteById(id);
    }
}
