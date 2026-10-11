package github.jotagm.clube_livro.domain.estante;

import github.jotagm.clube_livro.domain.exceptions.RegraDeNegocioException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um livro na estante pessoal do usuário — leitura feita por conta própria, fora (ou além)
 * dos clubes. É o que dá motivo para abrir o Booklit todo dia, não só quando o clube tem
 * leitura ativa.
 *
 * <p>Um livro aparece uma vez por usuário (único por {@code usuario + livroGoogleId}): relê-lo
 * reabre o mesmo item em vez de duplicar a estante.
 */
@Entity
@Table(name = "t_leitura_pessoal",
        uniqueConstraints = @UniqueConstraint(name = "uk_leitura_pessoal_usuario_livro",
                columnNames = {"usuario_id", "livro_google_id"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeituraPessoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(optional = false)
    Usuario usuario;

    @Column(name = "livro_google_id", nullable = false)
    String livroGoogleId;

    @Column(name = "livro_titulo", nullable = false)
    String livroTitulo;

    @Column(name = "livro_autor")
    String livroAutor;

    @Column(name = "livro_capa_url")
    String livroCapaUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    StatusLeituraPessoal status;

    /** Total de páginas; opcional porque o Google Books nem sempre informa. */
    @Column(name = "total_paginas")
    Integer totalPaginas;

    @Column(name = "pagina_atual")
    int paginaAtual;

    /** 1 a 5; nulo enquanto não avaliado. */
    Integer nota;

    @Column(columnDefinition = "TEXT")
    String resenha;

    @Column(name = "data_inicio")
    LocalDate dataInicio;

    @Column(name = "data_fim")
    LocalDate dataFim;

    /** Clube cuja leitura trouxe o livro para a estante — nulo quando a pessoa adicionou sozinha. */
    @Column(name = "clube_origem_id")
    UUID clubeOrigemId;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    public static LeituraPessoal adicionar(Usuario usuario, String livroGoogleId, String livroTitulo,
                                           String livroAutor, String livroCapaUrl, Integer totalPaginas,
                                           StatusLeituraPessoal status, LocalDate hoje) {
        LocalDateTime agora = LocalDateTime.now();
        LeituraPessoal leitura = LeituraPessoal.builder()
                .usuario(usuario)
                .livroGoogleId(livroGoogleId)
                .livroTitulo(livroTitulo)
                .livroAutor(livroAutor)
                .livroCapaUrl(livroCapaUrl)
                .totalPaginas(totalPaginas)
                .status(StatusLeituraPessoal.QUERO_LER)
                .paginaAtual(0)
                .createdAt(agora)
                .updatedAt(agora)
                .build();
        leitura.mudarStatus(status, hoje);
        return leitura;
    }

    /**
     * Transições de status com as datas que fazem sentido em cada uma: começar a ler marca o
     * início (se ainda não havia), terminar marca o fim e completa as páginas, voltar para
     * "quero ler" zera a leitura.
     */
    public void mudarStatus(StatusLeituraPessoal novo, LocalDate hoje) {
        switch (novo) {
            case QUERO_LER -> {
                this.paginaAtual = 0;
                this.dataInicio = null;
                this.dataFim = null;
            }
            case LENDO -> {
                if (this.dataInicio == null) this.dataInicio = hoje;
                this.dataFim = null;
            }
            case LIDO -> {
                if (this.dataInicio == null) this.dataInicio = hoje;
                this.dataFim = hoje;
                if (this.totalPaginas != null) this.paginaAtual = this.totalPaginas;
            }
            case ABANDONADO -> this.dataFim = hoje;
        }
        this.status = novo;
        tocar();
    }

    /**
     * Registra a página atual. Avançar a partir de "quero ler" começa a leitura; chegar na
     * última página conclui. Diferente do registro do clube, aqui voltar páginas é permitido —
     * a pessoa pode ter anotado errado, e a estante é só dela.
     */
    public void atualizarProgresso(int pagina, Integer totalPaginas, LocalDate hoje) {
        if (pagina < 0) {
            throw new RegraDeNegocioException("A página não pode ser negativa");
        }
        if (totalPaginas != null) {
            if (totalPaginas < 1) throw new RegraDeNegocioException("O total de páginas deve ser positivo");
            this.totalPaginas = totalPaginas;
        }
        if (this.totalPaginas != null && pagina > this.totalPaginas) {
            throw new RegraDeNegocioException("A página não pode passar do total de páginas do livro");
        }
        this.paginaAtual = pagina;

        if (this.totalPaginas != null && pagina == this.totalPaginas) {
            mudarStatus(StatusLeituraPessoal.LIDO, hoje);
        } else if (pagina > 0 && this.status != StatusLeituraPessoal.LENDO) {
            mudarStatus(StatusLeituraPessoal.LENDO, hoje);
        } else {
            tocar();
        }
    }

    public void avaliar(Integer nota, String resenha) {
        if (nota != null && (nota < 1 || nota > 5)) {
            throw new RegraDeNegocioException("A nota deve ser de 1 a 5");
        }
        this.nota = nota;
        this.resenha = resenha == null || resenha.isBlank() ? null : resenha.trim();
        tocar();
    }

    public void vincularClube(UUID clubeId) {
        if (this.clubeOrigemId == null) this.clubeOrigemId = clubeId;
    }

    public boolean concluidaNoAno(int ano) {
        return status == StatusLeituraPessoal.LIDO && dataFim != null && dataFim.getYear() == ano;
    }

    private void tocar() {
        this.updatedAt = LocalDateTime.now();
    }
}
