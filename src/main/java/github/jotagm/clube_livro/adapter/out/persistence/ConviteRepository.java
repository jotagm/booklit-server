package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.convite.Convite;
import github.jotagm.clube_livro.domain.clube.convite.ConviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ConviteRepository extends JpaRepository <Convite, UUID>{
    Convite findByEmailDestinatario(String email);
    List<Convite> findByClubeId(UUID clubeId);
    List<Convite> findByStatusAndExpiraEmBefore(ConviteStatus status, LocalDateTime momento);
}
