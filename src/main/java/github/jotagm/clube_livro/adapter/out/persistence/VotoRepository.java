package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.votacao.Voto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VotoRepository extends JpaRepository<Voto, UUID> {

    List<Voto> findByVotacaoId(UUID votacaoId);

    List<Voto> findByOpcaoVotoId(UUID opcaoVotoId);

    Optional<Voto> findByVotacaoIdAndUsuarioId(UUID votacaoId, UUID usuarioId);
}
