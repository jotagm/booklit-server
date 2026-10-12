package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.leitura.Registro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistroRepository extends JpaRepository<Registro, UUID> {

    List<Registro> findByLeituraClubeId(UUID leituraClubeId);

    List<Registro> findByUsuarioId(Integer usuarioId);

    Optional<Registro> findByLeituraClubeIdAndUsuarioId(UUID leituraClubeId, Integer usuarioId);

    /** Progressos mais recentes dos clubes informados (alimenta o feed). Ignora registros ainda zerados. */
    @org.springframework.data.jpa.repository.Query("""
            SELECT r FROM Registro r
            WHERE r.leituraClube.clube.id IN :clubeIds AND r.valorAtual > 0
            ORDER BY r.updatedAt DESC
            """)
    List<Registro> recentesDosClubes(@org.springframework.data.repository.query.Param("clubeIds") java.util.Collection<UUID> clubeIds,
                                     org.springframework.data.domain.Pageable pageable);

}
