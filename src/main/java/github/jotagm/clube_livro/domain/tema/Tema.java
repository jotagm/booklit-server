package github.jotagm.clube_livro.domain.tema;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "t_tema")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tema {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(unique = true)
    String nome;

    public void renomear(String nome) {
        this.nome = nome;
    }

    public static Tema novo(String nome) {
        return Tema.builder().nome(nome).build();
    }
}
