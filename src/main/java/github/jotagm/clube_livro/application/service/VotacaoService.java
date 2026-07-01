package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.VotacaoRepository;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class VotacaoService {

    private final VotacaoRepository votacaoRepository;

    public Votacao salvar(Votacao votacao) {
        return votacaoRepository.save(votacao);
    }

    public Votacao buscarPorId(UUID id) {
        return votacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Votação não encontrada"));
    }

    public List<Votacao> listarPorClube(UUID clubeId) {
        return votacaoRepository.findByClubeId(clubeId);
    }

    public Votacao atualizar(Votacao votacao) {
        return votacaoRepository.save(votacao);
    }

    public void deletar(UUID id) {
        votacaoRepository.deleteById(id);
    }
}
