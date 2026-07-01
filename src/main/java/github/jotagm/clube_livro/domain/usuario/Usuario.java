package github.jotagm.clube_livro.domain.usuario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_usuario")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    String nome;
    @Column(unique = true)
    String email;
    @Column(name = "senha_hash")
    String senhaHash;
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
