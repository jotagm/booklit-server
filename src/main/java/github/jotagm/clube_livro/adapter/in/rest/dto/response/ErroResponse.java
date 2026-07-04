package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import java.time.LocalDateTime;

public record ErroResponse(int status, String mensagem, LocalDateTime timestamp) {
}
