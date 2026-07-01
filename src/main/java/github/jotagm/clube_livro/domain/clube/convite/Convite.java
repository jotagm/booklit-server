package github.jotagm.clube_livro.domain.clube.convite;

import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_convite")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Convite {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne
    Clube clube;

    @ManyToOne
    @JoinColumn(name = "convidado_por")
    Usuario convidadoPor;

    @Column(name = "email_destinatario")
    String emailDestinatario;

    @Enumerated(EnumType.STRING)
    ConviteStatus status;

    @Column(name = "expira_em")
    LocalDateTime expiraEm;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}
