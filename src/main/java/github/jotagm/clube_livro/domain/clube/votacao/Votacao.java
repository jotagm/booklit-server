package github.jotagm.clube_livro.domain.clube.votacao;

import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.exceptions.VotacaoJaEncerradaException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_votacao")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Votacao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne
    Clube clube;

    @Enumerated(EnumType.STRING)
    VotacaoStatus status;

    @Column(name = "data_abertura")
    LocalDateTime dataAbertura;

    @Column(name = "data_encerramento")
    LocalDateTime dataEncerramento;

    public VotacaoEncerrada encerrar(OpcaoVoto vencedora, UUID leituraClubeId) {
        if (this.status == VotacaoStatus.ENCERRADA) {
            throw new VotacaoJaEncerradaException();
        }
        this.status = VotacaoStatus.ENCERRADA;
        return VotacaoEncerrada.de(this, vencedora, leituraClubeId);
    }

    public void reagendar(LocalDateTime dataAbertura, LocalDateTime dataEncerramento) {
        this.dataAbertura = dataAbertura;
        this.dataEncerramento = dataEncerramento;
    }

    public static Votacao abrir(Clube clube, LocalDateTime dataAbertura, LocalDateTime dataEncerramento) {
        return Votacao.builder()
                .clube(clube)
                .status(VotacaoStatus.ABERTA)
                .dataAbertura(dataAbertura)
                .dataEncerramento(dataEncerramento)
                .build();
    }
}