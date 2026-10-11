package github.jotagm.clube_livro.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MetaLeituraRequest(@Min(1) @Max(1000) int quantidadeLivros) {}
