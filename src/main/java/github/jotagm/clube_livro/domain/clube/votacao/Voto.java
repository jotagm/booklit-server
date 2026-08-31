package github.jotagm.clube_livro.domain.clube.votacao;

import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "t_voto")
@Getter
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

    public static Voto registrar(Votacao votacao, OpcaoVoto opcaoVoto, Usuario usuario, ClubePapel papel) {
        return Voto.builder()
                .votacao(votacao)
                .opcaoVoto(opcaoVoto)
                .usuario(usuario)
                .peso(pesoPara(papel))
                .build();
    }

    private static int pesoPara(ClubePapel papel) {
        return switch (papel) {
            case LIDER -> 2;
            case MEMBRO -> 1;
        };
    }
}
