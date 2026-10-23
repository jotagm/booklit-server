package github.jotagm.clube_livro.domain.clube.convite;

import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.exceptions.ConflitoException;
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
        exigirPendente();
        this.status = ConviteStatus.ACEITO;
    }

    public void recusar() {
        exigirPendente();
        this.status = ConviteStatus.RECUSADO;
    }

    public boolean estaPendente() {
        return status == ConviteStatus.PENDENTE;
    }

    /**
     * Convite é respondido uma vez só. Sem isto, aceitar de novo criava outra linha de membro
     * e recusar um convite aceito deixava status RECUSADO com a pessoa ainda no clube.
     */
    private void exigirPendente() {
        if (status == ConviteStatus.PENDENTE) {
            return;
        }
        throw new ConflitoException(switch (status) {
            case ACEITO -> "Este convite já foi aceito";
            case RECUSADO -> "Este convite já foi recusado";
            case EXPIRADO -> "Este convite expirou";
            default -> "Este convite não está mais pendente";
        });
    }

    public void expirar() {
        this.status = ConviteStatus.EXPIRADO;
    }

    public boolean estaExpirado(LocalDateTime momento) {
        return expiraEm.isBefore(momento);
    }

    public boolean pertenceA(String email) {
        return emailDestinatario != null && emailDestinatario.equalsIgnoreCase(email);
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
