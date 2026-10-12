package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.feed.Reacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReacaoRepository extends JpaRepository<Reacao, UUID> {

    List<Reacao> findByAlvoIdIn(Collection<String> alvoIds);

    Optional<Reacao> findByAlvoIdAndUsuarioId(String alvoId, Integer usuarioId);
}
