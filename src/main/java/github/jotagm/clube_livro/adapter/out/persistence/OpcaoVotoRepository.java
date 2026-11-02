package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OpcaoVotoRepository extends JpaRepository<OpcaoVoto, UUID> {

    List<OpcaoVoto> findByVotacaoId(UUID votacaoId);

    List<OpcaoVoto> findBySugeridoPorId(Integer usuarioId);

    /** Sugestões recentes dos clubes (alimenta o feed). As anteriores à data de criação ficam de fora. */
    @org.springframework.data.jpa.repository.Query("""
            SELECT o FROM OpcaoVoto o
            WHERE o.votacao.clube.id IN :clubeIds AND o.createdAt IS NOT NULL
            ORDER BY o.createdAt DESC
            """)
    List<OpcaoVoto> recentesDosClubes(@org.springframework.data.repository.query.Param("clubeIds") java.util.Collection<UUID> clubeIds,
                                      org.springframework.data.domain.Pageable pageable);
}
