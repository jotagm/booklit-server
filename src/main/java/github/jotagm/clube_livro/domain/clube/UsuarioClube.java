package github.jotagm.clube_livro.domain.clube;

import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_usuario_clube")

@Getter
@Setter
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
}
