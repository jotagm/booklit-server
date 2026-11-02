package github.jotagm.clube_livro.domain.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Um dia em que a pessoa registrou progresso de leitura, no clube ou na estante. É o histórico
 * que alimenta a sequência de semanas (🔥): o registro guarda só a posição atual, não quando
 * cada avanço aconteceu.
 */
@Entity
@Table(name = "t_dia_leitura",
        uniqueConstraints = @UniqueConstraint(name = "uk_dia_leitura_usuario", columnNames = {"usuario_id", "dia"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaDeLeitura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "usuario_id", nullable = false)
    Integer usuarioId;

    @Column(nullable = false)
    LocalDate dia;

    public static DiaDeLeitura de(Integer usuarioId, LocalDate dia) {
        return DiaDeLeitura.builder().usuarioId(usuarioId).dia(dia).build();
    }
}
