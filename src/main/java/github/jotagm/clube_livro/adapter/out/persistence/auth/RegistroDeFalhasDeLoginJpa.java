package github.jotagm.clube_livro.adapter.out.persistence.auth;

import github.jotagm.clube_livro.application.port.out.RegistroDeFalhasDeLogin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class RegistroDeFalhasDeLoginJpa implements RegistroDeFalhasDeLogin {

    private final FalhaDeLoginJpaRepository jpa;

    @Override
    public UUID registrar(String email, String ip, Instant quando) {
        return jpa.save(new FalhaDeLoginEntity(email, ip, quando)).id;
    }

    @Override
    public void apagar(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public long contarPorEmailEIpDesde(String email, String ip, Instant desde) {
        // Sem IP (não deveria acontecer atrás do Caddy) cai no limite por e-mail.
        return ip == null ? 0 : jpa.countByEmailAndIpAndOcorridaEmGreaterThanEqual(email, ip, desde);
    }

    @Override
    public long contarPorEmailDesde(String email, Instant desde) {
        return jpa.countByEmailAndOcorridaEmGreaterThanEqual(email, desde);
    }

    @Override
    public long contarPorIpDesde(String ip, Instant desde) {
        return jpa.countByIpAndOcorridaEmGreaterThanEqual(ip, desde);
    }

    @Override
    public Optional<Instant> maisAntigaPorEmailEIpDesde(String email, String ip, Instant desde) {
        return ip == null ? Optional.empty() : Optional.ofNullable(jpa.maisAntigaPorEmailEIp(email, ip, desde));
    }

    @Override
    public Optional<Instant> maisAntigaPorEmailDesde(String email, Instant desde) {
        return Optional.ofNullable(jpa.maisAntigaPorEmail(email, desde));
    }

    @Override
    public Optional<Instant> maisAntigaPorIpDesde(String ip, Instant desde) {
        return Optional.ofNullable(jpa.maisAntigaPorIp(ip, desde));
    }

    @Override
    public void limparEmail(String email) {
        jpa.apagarPorEmail(email);
    }

    @Override
    public int apagarAnterioresA(Instant limite) {
        return jpa.apagarAnterioresA(limite);
    }
}
