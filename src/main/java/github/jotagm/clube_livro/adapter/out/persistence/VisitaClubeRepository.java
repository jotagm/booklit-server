package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.VisitaClube;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitaClubeRepository extends JpaRepository<VisitaClube, UUID> {

    Optional<VisitaClube> findByUsuarioIdAndClubeId(Integer usuarioId, UUID clubeId);

    List<VisitaClube> findByUsuarioId(Integer usuarioId);
}
