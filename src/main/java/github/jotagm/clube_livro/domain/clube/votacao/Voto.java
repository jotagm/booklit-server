package github.jotagm.clube_livro.domain.clube.votacao;

import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "t_voto")

@Getter
@Setter
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
