package github.jotagm.clube_livro.adapter.in.messaging;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "t_evento_processado")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoProcessado {

    /** eventId do envelope. A PK garante que o mesmo evento só é processado uma vez. */
    @Id
    UUID id;

    @Column(name = "tipo", nullable = false)
    String tipo;

    @Column(name = "processado_em", nullable = false)
    Instant processadoEm;

    EventoProcessado(UUID id, String tipo, Instant processadoEm) {
        this.id = id;
        this.tipo = tipo;
        this.processadoEm = processadoEm;
    }
}
