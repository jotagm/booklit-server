package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import github.jotagm.clube_livro.domain.estante.LeituraPessoal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * O que os outros veem de um leitor. Não traz e-mail, nota, resenha nem "quero ler":
 * só o que a pessoa está lendo, o que já terminou e os clubes que vocês dividem.
 */
public record PerfilResponse(
        Integer id,
        String nome,
        String bio,
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
    public record LivroDoPerfil(String titulo, String autor, String capaUrl, Integer paginaAtual, Integer totalPaginas) {
        public static LivroDoPerfil from(LeituraPessoal l) {
            return new LivroDoPerfil(l.getLivroTitulo(), l.getLivroAutor(), l.getLivroCapaUrl(),
                    l.getPaginaAtual(), l.getTotalPaginas());
        }
    }

    public record ClubeEmComum(UUID id, String nome) {}
}
