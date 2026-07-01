package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.convite.Convite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConviteRepository extends JpaRepository <Convite, UUID>{
    Convite findByEmailDestinatario(String email);
    List<Convite> findByClubeId(UUID clubeId);
}
