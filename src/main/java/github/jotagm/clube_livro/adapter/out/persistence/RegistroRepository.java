package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.leitura.Registro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistroRepository extends JpaRepository<Registro, UUID> {

    List<Registro> findByLeituraClubeId(UUID leituraClubeId);

    List<Registro> findByUsuarioId(UUID usuarioId);

    Optional<Registro> findByLeituraClubeIdAndUsuarioId(UUID leituraClubeId, UUID usuarioId);
}
