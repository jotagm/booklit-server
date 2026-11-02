package github.jotagm.clube_livro.domain.clube.votacao;

import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.exceptions.RegraDeNegocioException;
import github.jotagm.clube_livro.domain.exceptions.VotacaoJaEncerradaException;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
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

    /** Último "lembrar quem não votou" do líder; limita a um lembrete por {@link #INTERVALO_ENTRE_LEMBRETES}. */
    @Column(name = "ultimo_lembrete_em")
    LocalDateTime ultimoLembreteEm;

    public static final Duration INTERVALO_ENTRE_LEMBRETES = Duration.ofHours(24);

    /**
     * Marca que o líder lembrou quem falta votar. Só em votação aberta e no máximo um por
     * intervalo: o lembrete vira e-mail na caixa de cada membro, então não pode virar spam.
     */
    public void registrarLembrete(LocalDateTime agora) {
        if (this.status != VotacaoStatus.ABERTA) {
            throw new VotacaoJaEncerradaException();
        }
        if (ultimoLembreteEm != null && agora.isBefore(ultimoLembreteEm.plus(INTERVALO_ENTRE_LEMBRETES))) {
            throw new RegraDeNegocioException("Já foi enviado um lembrete nas últimas 24 horas");
        }
        this.ultimoLembreteEm = agora;
    }

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