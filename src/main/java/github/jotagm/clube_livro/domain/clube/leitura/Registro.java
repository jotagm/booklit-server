package github.jotagm.clube_livro.domain.clube.leitura;

import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_registro")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Registro {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne
    LeituraClube leituraClube;

    @ManyToOne
    Usuario usuario;

    @Column(name = "valor_atual")
    int valorAtual;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    public void registrarProgresso(int valorAtual) {
        this.valorAtual = valorAtual;
        this.updatedAt = LocalDateTime.now();
    }

    public static Registro iniciar(LeituraClube leituraClube, Usuario usuario) {
        return Registro.builder()
                .leituraClube(leituraClube)
                .usuario(usuario)
                .valorAtual(0)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
