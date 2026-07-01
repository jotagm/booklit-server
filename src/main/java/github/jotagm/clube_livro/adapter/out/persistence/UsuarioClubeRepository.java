package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioClubeRepository extends JpaRepository <UsuarioClube, UUID>{
    List<UsuarioClube> findByUsuarioId(UUID usuarioId);
    List <UsuarioClube> findByClubeId(UUID clubeId);
    Optional<UsuarioClube> findByUsuarioIdAndClubeId(UUID usuarioId, UUID clubeId);

}
