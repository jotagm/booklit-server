package github.jotagm.clube_livro.adapter.out.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventoRepository extends JpaRepository<OutboxEvento, UUID> {

    List<OutboxEvento> findTop50ByPublicadoEmIsNullOrderByCriadoEmAsc();
}