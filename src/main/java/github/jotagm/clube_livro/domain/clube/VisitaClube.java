package github.jotagm.clube_livro.domain.clube;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Última vez que um membro abriu o clube. O "ponto de novidade" compara com a atividade do
 * clube: o que aconteceu depois disso (e não foi feito pela própria pessoa) é novidade.
 */
@Entity
@Table(name = "t_visita_clube",
        uniqueConstraints = @UniqueConstraint(name = "uk_visita_clube_usuario", columnNames = {"usuario_id", "clube_id"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitaClube {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "usuario_id", nullable = false)
    Integer usuarioId;

    @Column(name = "clube_id", nullable = false)
    UUID clubeId;

    @Column(name = "visto_em", nullable = false)
    LocalDateTime vistoEm;

    public void verEm(LocalDateTime momento) {
        this.vistoEm = momento;
    }

    public static VisitaClube primeira(Integer usuarioId, UUID clubeId, LocalDateTime momento) {
        return VisitaClube.builder().usuarioId(usuarioId).clubeId(clubeId).vistoEm(momento).build();
    }
}
