package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VotoRequest(
        @NotNull UUID votacaoId,
        @NotNull UUID opcaoVotoId,
        /** Ignorado: o voto é sempre do usuário autenticado. Mantido para compatibilidade com clientes antigos. */
        @Deprecated Integer usuarioId
) {}
