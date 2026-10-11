package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.estante.MetaLeitura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MetaLeituraRepository extends JpaRepository<MetaLeitura, UUID> {

    Optional<MetaLeitura> findByUsuarioIdAndAno(Integer usuarioId, int ano);
}
