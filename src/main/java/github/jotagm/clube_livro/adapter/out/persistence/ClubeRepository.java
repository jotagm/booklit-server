package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.Clube;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClubeRepository extends JpaRepository <Clube, UUID>{
    Optional<Clube> findByNome(String nome);
}
