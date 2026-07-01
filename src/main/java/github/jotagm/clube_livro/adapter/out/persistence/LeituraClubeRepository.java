package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeituraClubeRepository extends JpaRepository <LeituraClube, UUID>{

    List<LeituraClube> findByClubeIdOrderByDataInicioDesc(UUID clubeId);

    @Query("SELECT l FROM LeituraClube l WHERE l.clube.id = ?1 AND l.dataFim IS NULL")
    List<LeituraClube> findByClubeIdAndSemDataFim(UUID clubeId);

    @Query("SELECT l FROM LeituraClube l WHERE l.clube.id = :clubeId AND l.dataInicio <= :agora AND l.dataFim >= :agora")
    Optional<LeituraClube> findLeituraAtiva(@Param("clubeId") UUID clubeId, @Param("agora") LocalDateTime agora);

    UUID clube(Clube clube);
}
