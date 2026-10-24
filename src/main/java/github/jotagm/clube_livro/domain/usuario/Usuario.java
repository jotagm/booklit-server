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
    /** Frase curta que aparece no perfil; opcional. */
    @Column(length = 160)
    String bio;
    /**
     * Quando o e-mail foi provado (código recebido por e-mail). Nulo = conta ainda não
     * confirmou a posse do endereço.
     */
    @Column(name = "email_verificado_em")
    LocalDateTime emailVerificadoEm;

    /** Construtor sem bio: é o que o cadastro e os testes sempre usaram. */
    public Usuario(Integer id, String nome, String email, String senhaHash, LocalDateTime createdAt) {
        this(id, nome, email, senhaHash, createdAt, null, null);
    }

    public void atualizarDados(String nome, String email) {
        this.nome = nome;
        this.email = email;
    }

    public void atualizarPerfil(String nome, String bio) {
        this.nome = nome.trim();
        this.bio = bio == null || bio.isBlank() ? null : bio.trim();
    }

    public boolean emailVerificado() {
        return emailVerificadoEm != null;
    }

    /** Idempotente: guarda só a primeira confirmação. */
    public void marcarEmailVerificado(LocalDateTime momento) {
        if (emailVerificadoEm == null) {
            emailVerificadoEm = momento;
        }
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
