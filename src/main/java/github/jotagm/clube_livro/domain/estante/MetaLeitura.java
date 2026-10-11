package github.jotagm.clube_livro.domain.estante;

import github.jotagm.clube_livro.domain.exceptions.RegraDeNegocioException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Meta anual de livros lidos ("ler 20 livros em 2027"). Uma por usuário e ano. */
@Entity
@Table(name = "t_meta_leitura",
        uniqueConstraints = @UniqueConstraint(name = "uk_meta_leitura_usuario_ano",
                columnNames = {"usuario_id", "ano"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaLeitura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(optional = false)
    Usuario usuario;

    @Column(nullable = false)
    int ano;

    @Column(name = "quantidade_livros", nullable = false)
    int quantidadeLivros;

    public static MetaLeitura definir(Usuario usuario, int ano, int quantidadeLivros) {
        MetaLeitura meta = MetaLeitura.builder().usuario(usuario).ano(ano).build();
        meta.alterarQuantidade(quantidadeLivros);
        return meta;
    }

    public void alterarQuantidade(int quantidadeLivros) {
        if (quantidadeLivros < 1) {
            throw new RegraDeNegocioException("A meta precisa ser de pelo menos 1 livro");
        }
        this.quantidadeLivros = quantidadeLivros;
    }
}
