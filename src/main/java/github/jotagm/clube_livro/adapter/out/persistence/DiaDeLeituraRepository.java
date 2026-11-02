package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.usuario.DiaDeLeitura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DiaDeLeituraRepository extends JpaRepository<DiaDeLeitura, UUID> {

    boolean existsByUsuarioIdAndDia(Integer usuarioId, LocalDate dia);

    @Query("select d.dia from DiaDeLeitura d where d.usuarioId = :usuarioId and d.dia >= :desde")
    List<LocalDate> diasDesde(@Param("usuarioId") Integer usuarioId, @Param("desde") LocalDate desde);

    @Query("select distinct d.usuarioId from DiaDeLeitura d where d.usuarioId in :usuarioIds and d.dia = :dia")
    List<Integer> quemLeuNoDia(@Param("usuarioIds") Collection<Integer> usuarioIds, @Param("dia") LocalDate dia);
}
