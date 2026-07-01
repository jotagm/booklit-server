package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.tema.Tema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemaRepository extends JpaRepository <Tema, UUID>{
    Optional<Tema> findByNome(String nome);

}
