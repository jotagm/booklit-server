package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.leitura.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComentarioRepository extends JpaRepository<Comentario, UUID> {
    List<Comentario> findByLeituraClubeIdOrderByCreatedAtAsc(UUID leituraClubeId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT c FROM Comentario c
            WHERE c.leituraClube.clube.id IN :clubeIds AND c.removido = false
            ORDER BY c.createdAt DESC
            """)
    List<Comentario> recentesDosClubes(@org.springframework.data.repository.query.Param("clubeIds") java.util.Collection<UUID> clubeIds,
                                       org.springframework.data.domain.Pageable pageable);

}
