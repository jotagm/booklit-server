package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OpcaoVotoRepository extends JpaRepository<OpcaoVoto, UUID> {

    List<OpcaoVoto> findByVotacaoId(UUID votacaoId);

    List<OpcaoVoto> findBySugeridoPorId(UUID usuarioId);
}
