package github.jotagm.clube_livro.domain.clube.leitura;

import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_comentario")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne
    @JoinColumn(name = "leitura_clube_id")
    LeituraClube leituraClube;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "comentario_pai_id")
    Comentario comentarioPai;

    String conteudo;

    /**
     * Onde o autor estava na leitura (na unidade da meta) quando escreveu. O front usa para
     * esconder spoiler de quem ainda não chegou lá. Nulo em comentários anteriores a isto.
     */
    @Column(name = "pagina_autor")
    Integer paginaAutor;

    boolean removido;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    public void editar(String conteudo) {
        this.conteudo = conteudo;
        this.updatedAt = LocalDateTime.now();
    }

    public void remover() {
        this.removido = true;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean ehResposta() {
        return comentarioPai != null;
    }

    public boolean foiEscritoPor(Usuario usuario) {
        return this.usuario.getId().equals(usuario.getId());
    }
}
