package github.jotagm.clube_livro.adapter.out.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "t_outbox_evento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvento {

    @Id
    UUID id;

    @Column(name = "tipo", nullable = false)
    String tipo;

    @Column(name = "agregado_id", nullable = false)
    UUID agregadoId;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    String payload;

    @Column(name = "criado_em", nullable = false)
    Instant criadoEm;

    @Column(name = "publicado_em")
    Instant publicadoEm;

    OutboxEvento(UUID id, String tipo, UUID agregadoId, String payload, Instant criadoEm) {
        this.id = id;
        this.tipo = tipo;
        this.agregadoId = agregadoId;
        this.payload = payload;
        this.criadoEm = criadoEm;
    }

    void marcarPublicado(Instant agora) {
        this.publicadoEm = agora;
    }
}