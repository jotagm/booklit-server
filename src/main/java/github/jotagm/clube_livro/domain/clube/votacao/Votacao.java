package github.jotagm.clube_livro.domain.clube.votacao;

import github.jotagm.clube_livro.domain.clube.Clube;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_votacao")

@Builder
@Getter
@Setter
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
}
