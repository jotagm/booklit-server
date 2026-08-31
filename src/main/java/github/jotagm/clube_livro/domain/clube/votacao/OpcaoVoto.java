package github.jotagm.clube_livro.domain.clube.votacao;

import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "t_opcao_voto")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpcaoVoto {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne
    Votacao votacao;

    @ManyToOne
    @JoinColumn(name = "sugerido_por")
    Usuario sugeridoPor;

    @Column(name = "livro_google_id")
    String livroGoogleId;

    @Column(name = "livro_titulo")
    String livroTitulo;

    @Column(name = "livro_capa_url")
    String livroCapaUrl;

    public void trocarLivro(String livroGoogleId, String livroTitulo, String livroCapaUrl) {
        this.livroGoogleId = livroGoogleId;
        this.livroTitulo = livroTitulo;
        this.livroCapaUrl = livroCapaUrl;
    }

    public static OpcaoVoto sugerir(Votacao votacao, Usuario sugeridoPor,
                                    String livroGoogleId, String livroTitulo, String livroCapaUrl) {
        return OpcaoVoto.builder()
                .votacao(votacao)
                .sugeridoPor(sugeridoPor)
                .livroGoogleId(livroGoogleId)
                .livroTitulo(livroTitulo)
                .livroCapaUrl(livroCapaUrl)
                .build();
    }
}
