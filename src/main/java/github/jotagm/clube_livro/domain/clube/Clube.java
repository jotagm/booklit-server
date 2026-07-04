package github.jotagm.clube_livro.domain.clube;

import github.jotagm.clube_livro.domain.tema.Tema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "t_clube")

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Clube {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    String nome;
    String descricao;
    boolean privado;

    @Enumerated(EnumType.STRING)
    ClubeStatus status;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @ManyToMany
    @JoinTable(
        name = "t_clube_tema",
        joinColumns = @JoinColumn(name = "clube_id"),
        inverseJoinColumns = @JoinColumn(name = "tema_id")
    )
    List<Tema> temas;
}
