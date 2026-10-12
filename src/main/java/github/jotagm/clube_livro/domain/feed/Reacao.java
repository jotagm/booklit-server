package github.jotagm.clube_livro.domain.feed;

import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reação de um membro a um item do feed. O alvo é identificado pelo id do item (ex.:
 * {@code comentario:<uuid>}), porque o feed é montado a partir de tabelas que já existem — não há
 * uma "tabela de eventos". Cada pessoa tem no máximo uma reação por item; trocar de reação
 * substitui a anterior.
 */
@Entity
@Table(name = "t_reacao",
        uniqueConstraints = @UniqueConstraint(name = "uk_reacao_alvo_usuario", columnNames = {"alvo_id", "usuario_id"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "clube_id", nullable = false)
    UUID clubeId;

    @Column(name = "alvo_id", nullable = false, length = 80)
    String alvoId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TipoReacao tipo;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    public void trocarPara(TipoReacao tipo) {
        this.tipo = tipo;
    }

    public static Reacao de(UUID clubeId, String alvoId, Usuario usuario, TipoReacao tipo) {
        return Reacao.builder()
                .clubeId(clubeId)
                .alvoId(alvoId)
                .usuario(usuario)
                .tipo(tipo)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
