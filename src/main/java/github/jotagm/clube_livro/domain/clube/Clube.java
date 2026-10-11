package github.jotagm.clube_livro.domain.clube;

import github.jotagm.clube_livro.domain.tema.Tema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "t_clube")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Clube {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    String nome;
    String descricao;
    boolean privado;

    @Enumerated(EnumType.STRING)
    ClubeStatus status;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @ManyToMany
    @JoinTable(
        name = "t_clube_tema",
        joinColumns = @JoinColumn(name = "clube_id"),
        inverseJoinColumns = @JoinColumn(name = "tema_id")
    )
    List<Tema> temas;

    /**
     * Código do link de convite ({@code booklit.bio/convite/<codigo>}). Quem tem o link entra
     * como MEMBRO — é o fluxo pensado para colar no grupo do WhatsApp. Nulo até o líder pedir
     * o link pela primeira vez; regenerar invalida o link antigo.
     */
    @Column(name = "codigo_convite", unique = true, length = 16)
    String codigoConvite;

    private static final String ALFABETO_CODIGO = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_CODIGO = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Gera um novo código de convite, invalidando o anterior. Sem caracteres ambíguos (0/O, 1/l/I). */
    public String gerarCodigoConvite() {
        StringBuilder sb = new StringBuilder(TAMANHO_CODIGO);
        for (int i = 0; i < TAMANHO_CODIGO; i++) {
            sb.append(ALFABETO_CODIGO.charAt(RANDOM.nextInt(ALFABETO_CODIGO.length())));
        }
        this.codigoConvite = sb.toString();
        return this.codigoConvite;
    }

    public void atualizarDados(String nome, String descricao, boolean privado, List<Tema> temas) {
        this.nome = nome;
        this.descricao = descricao;
        this.privado = privado;
        if (temas != null) {
            this.temas = temas;
        }
    }

    public static Clube novo(String nome, String descricao, boolean privado, List<Tema> temas) {
        return Clube.builder()
                .nome(nome)
                .descricao(descricao)
                .privado(privado)
                .status(ClubeStatus.ATIVO)
                .createdAt(LocalDateTime.now())
                .temas(temas)
                .build();
    }
}
