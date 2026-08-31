package github.jotagm.clube_livro.domain.clube;

import github.jotagm.clube_livro.domain.tema.Tema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "t_clube")
@Getter
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

    public void atualizarDados(String nome, String descricao, boolean privado, List<Tema> temas) {
        this.nome = nome;
        this.descricao = descricao;
        this.privado = privado;
        if (temas != null) {
            this.temas = temas;
        }
    }

    public static Clube novo(String nome, String descricao, boolean privado, List<Tema> temas) {
        return Clube.builder()
                .nome(nome)
                .descricao(descricao)
                .privado(privado)
                .status(ClubeStatus.ATIVO)
                .createdAt(LocalDateTime.now())
                .temas(temas)
                .build();
    }
}
