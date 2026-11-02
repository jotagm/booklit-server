package github.jotagm.clube_livro.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Onde ficam as tentativas de login com senha errada, por e-mail e por IP. */
public interface RegistroDeFalhasDeLogin {

    UUID registrar(String email, String ip, Instant quando);

    void apagar(UUID id);

    long contarPorEmailEIpDesde(String email, String ip, Instant desde);

    long contarPorEmailDesde(String email, Instant desde);

    long contarPorIpDesde(String ip, Instant desde);

    /** As falhas mais antigas ainda dentro da janela definem quando cada bloqueio acaba. */
    Optional<Instant> maisAntigaPorEmailEIpDesde(String email, String ip, Instant desde);

    Optional<Instant> maisAntigaPorEmailDesde(String email, Instant desde);

    Optional<Instant> maisAntigaPorIpDesde(String ip, Instant desde);

    /** Login certo zera o contador do e-mail (o do IP continua, para não virar atalho). */
    void limparEmail(String email);

    int apagarAnterioresA(Instant limite);
}
