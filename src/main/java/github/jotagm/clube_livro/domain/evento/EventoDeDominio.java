package github.jotagm.clube_livro.domain.evento;

import java.time.Instant;
import java.util.UUID;

public interface EventoDeDominio {
    UUID eventId();
    Instant ocorridoEm();
    String tipo();
    int versao();
    UUID agregadoId();
    Object dados();
}