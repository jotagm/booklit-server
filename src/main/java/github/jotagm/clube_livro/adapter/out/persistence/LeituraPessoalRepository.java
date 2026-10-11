package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.estante.LeituraPessoal;
import github.jotagm.clube_livro.domain.estante.StatusLeituraPessoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeituraPessoalRepository extends JpaRepository<LeituraPessoal, UUID> {

    List<LeituraPessoal> findByUsuarioIdOrderByUpdatedAtDesc(Integer usuarioId);

    List<LeituraPessoal> findByUsuarioIdAndStatusOrderByUpdatedAtDesc(Integer usuarioId, StatusLeituraPessoal status);

    Optional<LeituraPessoal> findByIdAndUsuarioId(UUID id, Integer usuarioId);

    Optional<LeituraPessoal> findByUsuarioIdAndLivroGoogleId(Integer usuarioId, String livroGoogleId);

    long countByUsuarioIdAndStatus(Integer usuarioId, StatusLeituraPessoal status);

    long countByUsuarioIdAndStatusAndDataFimBetween(Integer usuarioId, StatusLeituraPessoal status,
                                                    LocalDate inicio, LocalDate fim);

    /** Quantos dos usuários informados têm cada livro no "quero ler" — alimenta a votação do clube. */
    @Query("""
            SELECT l.livroGoogleId, COUNT(l) FROM LeituraPessoal l
            WHERE l.status = github.jotagm.clube_livro.domain.estante.StatusLeituraPessoal.QUERO_LER
              AND l.usuario.id IN :usuarioIds
              AND l.livroGoogleId IN :livroIds
            GROUP BY l.livroGoogleId
            """)
    List<Object[]> contarQueroLerPorLivro(@Param("usuarioIds") Collection<Integer> usuarioIds,
                                          @Param("livroIds") Collection<String> livroIds);
}
