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

    public void aceitar() {
        this.status = ConviteStatus.ACEITO;
    }

    public void recusar() {
        this.status = ConviteStatus.RECUSADO;
    }

    public void expirar() {
        this.status = ConviteStatus.EXPIRADO;
    }

    public boolean estaExpirado(LocalDateTime momento) {
        return expiraEm.isBefore(momento);
    }

    public boolean pertenceA(String email) {
        return emailDestinatario.equals(email);
    }

    public static Convite novo(Clube clube, Usuario convidadoPor, String emailDestinatario,
                               LocalDateTime expiraEm) {
        return Convite.builder()
                .clube(clube)
                .convidadoPor(convidadoPor)
                .emailDestinatario(emailDestinatario)
                .status(ConviteStatus.PENDENTE)
                .expiraEm(expiraEm)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
