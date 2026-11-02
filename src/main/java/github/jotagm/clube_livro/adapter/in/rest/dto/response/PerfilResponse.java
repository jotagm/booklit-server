package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.estante.LeituraPessoal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O que os outros veem de um leitor. Não traz e-mail, nota, resenha nem "quero ler":
 * só o que a pessoa está lendo, o que já terminou e os clubes que vocês dividem.
 */
public record PerfilResponse(
        Integer id,
        String nome,
        String bio,
        String fotoUrl,
        LocalDateTime membroDesde,
        boolean ehVoce,
        int ano,
        long lidosNoAno,
        Integer metaLivros,
        long totalLidos,
        List<LivroDoPerfil> lendoAgora,
        List<LivroDoPerfil> ultimosLidos,
        List<ClubeEmComum> clubes
) {
    /**
     * {@code clubeId}/{@code clube}: o clube em que a pessoa está lendo o livro ("com Clube do
     * Café"), só quando é um clube que quem vê o perfil também enxerga.
     */
    public record LivroDoPerfil(String titulo, String autor, String capaUrl, Integer paginaAtual, Integer totalPaginas,
                                UUID clubeId, String clube) {
        public static LivroDoPerfil from(LeituraPessoal l) {
            return from(l, Map.of());
        }

        public static LivroDoPerfil from(LeituraPessoal l, Map<UUID, String> clubesVisiveis) {
            UUID clubeId = l.getClubeOrigemId();
            String clube = clubeId == null ? null : clubesVisiveis.get(clubeId);
            return new LivroDoPerfil(l.getLivroTitulo(), l.getLivroAutor(), l.getLivroCapaUrl(),
                    l.getPaginaAtual(), l.getTotalPaginas(), clube == null ? null : clubeId, clube);
        }
    }

    public record ClubeEmComum(UUID id, String nome) {}
}
