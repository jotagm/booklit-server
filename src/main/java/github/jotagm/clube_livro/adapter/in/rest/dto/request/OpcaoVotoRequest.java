package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OpcaoVotoRequest(
        @NotNull UUID votacaoId,
        /** Ignorado: quem sugere é sempre o usuário autenticado. Mantido para compatibilidade. */
        @Deprecated Integer sugeridoPorId,
        @NotBlank String livroGoogleId,
        @NotBlank String livroTitulo,
        String livroCapaUrl,
        /** Opcional: autor(es) do livro; segue para a leitura se a opção vencer. */
        @Size(max = 255) String livroAutor
) {
    /** Clientes antigos (sem autor). */
    public OpcaoVotoRequest(UUID votacaoId, Integer sugeridoPorId, String livroGoogleId, String livroTitulo,
                            String livroCapaUrl) {
        this(votacaoId, sugeridoPorId, livroGoogleId, livroTitulo, livroCapaUrl, null);
    }
}
