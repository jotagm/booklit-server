package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.Clube;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ClubeRepository extends JpaRepository <Clube, UUID>{

    @EntityGraph(attributePaths = "temas")
    Optional<Clube> findById(UUID id);

    @EntityGraph(attributePaths = "temas")
    Optional<Clube> findByNome(String nome);

    @EntityGraph(attributePaths = "temas")
    Page<Clube> findByPrivadoFalseOrIdIn(Collection<UUID> ids, Pageable pageable);
}
