package github.jotagm.clube_livro.domain.clube.votacao;

import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "t_voto")

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Voto {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne
    Votacao votacao;

    @ManyToOne
    OpcaoVoto opcaoVoto;

    @ManyToOne
    Usuario usuario;

    int peso;
}
