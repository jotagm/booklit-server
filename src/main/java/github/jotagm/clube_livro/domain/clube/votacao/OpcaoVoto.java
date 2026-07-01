package github.jotagm.clube_livro.domain.clube.votacao;

import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "t_opcao_voto")

@Getter
@Setter
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
}
