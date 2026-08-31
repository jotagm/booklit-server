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

    public void redefinir(String livroGoogleId, String livroTitulo, String livroCapaUrl,
                          TipoMeta tipoMeta, int valorMeta,
                          LocalDateTime dataInicio, LocalDateTime dataFim) {
        this.livroGoogleId = livroGoogleId;
        this.livroTitulo = livroTitulo;
        this.livroCapaUrl = livroCapaUrl;
        this.tipoMeta = tipoMeta;
        this.valorMeta = valorMeta;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    public static LeituraClube iniciar(Clube clube, String livroGoogleId, String livroTitulo,
                                       String livroCapaUrl, TipoMeta tipoMeta, int valorMeta,
                                       LocalDateTime dataInicio, LocalDateTime dataFim) {
        return LeituraClube.builder()
                .clube(clube)
                .livroGoogleId(livroGoogleId)
                .livroTitulo(livroTitulo)
                .livroCapaUrl(livroCapaUrl)
                .tipoMeta(tipoMeta)
                .valorMeta(valorMeta)
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .build();
    }

    public static LeituraClube doLivroVencedor(Clube clube, String livroGoogleId,
                                               String livroTitulo, String livroCapaUrl) {
        return LeituraClube.builder()
                .clube(clube)
                .livroGoogleId(livroGoogleId)
                .livroTitulo(livroTitulo)
                .livroCapaUrl(livroCapaUrl)
                .build();
    }
}
