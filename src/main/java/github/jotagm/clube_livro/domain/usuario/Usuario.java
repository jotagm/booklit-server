package github.jotagm.clube_livro.domain.usuario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_usuario")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    String nome;
    @Column(unique = true)
    String email;
    @Column(name = "senha_hash")
    String senhaHash;
    @Column(name = "created_at")
    LocalDateTime createdAt;

    public void atualizarDados(String nome, String email) {
        this.nome = nome;
        this.email = email;
    }

    public void definirSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public static Usuario novo(String nome, String email, String senha) {
        return Usuario.builder()
                .nome(nome)
                .email(email)
                .senhaHash(senha)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
