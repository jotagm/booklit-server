package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VotacaoRepository extends JpaRepository <Votacao, UUID>{

    List<Votacao> findByClubeId(UUID clubeId);
}
