package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import java.util.List;

/**
 * O que um visitante (ainda sem conta) vê ao abrir o link de convite. É público, então
 * expõe só o necessário para convencer a pessoa a entrar: nada de e-mails nem ids de usuário.
 */
public record ConvitePreviewResponse(
        String codigo,
        String nomeClube,
        String descricao,
        String nomeLider,
        int totalMembros,
        List<String> temas,
        LeituraAtual leituraAtual
) {
    public record LeituraAtual(String livroTitulo, String livroCapaUrl) {}
}
