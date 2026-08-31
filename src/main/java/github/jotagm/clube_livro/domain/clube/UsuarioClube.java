package github.jotagm.clube_livro.domain.clube;

import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_usuario_clube")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioClube {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne
    Usuario usuario;

    @ManyToOne
    Clube clube;
    @Enumerated(EnumType.STRING)
    ClubePapel papel;
    @Column(name = "entrou_em")
    LocalDateTime entrouEm;

    public void mudarPapel(ClubePapel papel) {
        this.papel = papel;
    }

    public boolean ehLider() {
        return papel == ClubePapel.LIDER;
    }

    public static UsuarioClube novo(Usuario usuario, Clube clube, ClubePapel papel) {
        return UsuarioClube.builder()
                .usuario(usuario)
                .clube(clube)
                .papel(papel)
                .entrouEm(LocalDateTime.now())
                .build();
    }

    public static UsuarioClube lider(Usuario usuario, Clube clube) {
        return novo(usuario, clube, ClubePapel.LIDER);
    }

    public static UsuarioClube membro(Usuario usuario, Clube clube) {
        return novo(usuario, clube, ClubePapel.MEMBRO);
    }
}
