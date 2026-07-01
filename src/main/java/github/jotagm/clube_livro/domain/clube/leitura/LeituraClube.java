package github.jotagm.clube_livro.domain.clube.leitura;

import github.jotagm.clube_livro.domain.clube.Clube;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_leitura_clube")


@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeituraClube {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne
    Clube clube;

    @Column(name = "livro_google_id")
    String livroGoogleId;

    @Column(name = "livro_titulo")
    String livroTitulo;

    @Column(name = "livro_capa_url")
    String livroCapaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_meta")
    TipoMeta tipoMeta;

    @Column(name = "valor_meta")
    int valorMeta;

    @Column(name = "data_inicio")
    LocalDateTime dataInicio;

    @Column(name = "data_fim")
    LocalDateTime dataFim;
}
