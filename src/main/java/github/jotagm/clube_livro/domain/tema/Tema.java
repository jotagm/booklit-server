package github.jotagm.clube_livro.domain.tema;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "t_tema")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tema {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(unique = true)
    String nome;
}
