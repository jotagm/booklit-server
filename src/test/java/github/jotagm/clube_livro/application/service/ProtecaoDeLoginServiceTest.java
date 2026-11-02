package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.application.port.out.RegistroDeFalhasDeLogin;
import github.jotagm.clube_livro.domain.exceptions.LoginBloqueadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtecaoDeLoginServiceTest {

    private static final Instant INICIO = Instant.parse("2026-11-01T12:00:00Z");

    private final RegistroEmMemoria registro = new RegistroEmMemoria();
    private RelogioAjustavel relogio;
    private ProtecaoDeLoginService protecao;

    @BeforeEach
    void preparar() {
        relogio = new RelogioAjustavel(INICIO);
        protecao = new ProtecaoDeLoginService(registro, relogio, Duration.ofMinutes(15), 5, 30, 50);
    }

    @Test
    void cincoTentativasPassamASextaDoMesmoIpEhBloqueada() {
        tentar("ana@teste.dev", "1.1.1.1", 5);

        assertThrows(LoginBloqueadoException.class, () -> protecao.reservarTentativa("ana@teste.dev", "1.1.1.1"));
    }

    @Test
    void bloqueioDuraAteAFalhaMaisAntigaSairDaJanela() {
        tentar("ana@teste.dev", "1.1.1.1", 1);
        relogio.avancar(Duration.ofMinutes(3));
        tentar("ana@teste.dev", "1.1.1.1", 4);

        LoginBloqueadoException ex = assertThrows(LoginBloqueadoException.class,
                () -> protecao.reservarTentativa("ana@teste.dev", "1.1.1.1"));
        // A primeira falha foi há 3 min: faltam 12 min (+1 s de folga).
        assertEquals(12 * 60 + 1, ex.segundosParaTentarDeNovo());

        relogio.avancar(Duration.ofMinutes(12).plusSeconds(1));
        assertDoesNotThrow(() -> protecao.reservarTentativa("ana@teste.dev", "1.1.1.1"));
    }

    /** Quem sabe o e-mail de alguém não tranca a conta: a dona entra de outro IP. */
    @Test
    void atacanteBloqueadoNaoTrancaADonaDaContaEmOutroIp() {
        tentar("ana@teste.dev", "6.6.6.6", 5);
        for (int i = 0; i < 100; i++) {
            assertThrows(LoginBloqueadoException.class, () -> protecao.reservarTentativa("ana@teste.dev", "6.6.6.6"));
        }

        assertDoesNotThrow(() -> protecao.reservarTentativa("ana@teste.dev", "2.2.2.2"));
    }

    @Test
    void tentativaRecusadaNaoContaComoFalha() {
        tentar("ana@teste.dev", "6.6.6.6", 5);
        int antes = registro.falhas.size();

        assertThrows(LoginBloqueadoException.class, () -> protecao.reservarTentativa("ana@teste.dev", "6.6.6.6"));

        assertEquals(antes, registro.falhas.size());
    }

    @Test
    void ataqueDistribuidoContraUmaContaParaNoLimitePorEmail() {
        for (int i = 0; i < 10; i++) {
            tentar("ana@teste.dev", "10.0.0." + i, 5);
        }

        assertThrows(LoginBloqueadoException.class, () -> protecao.reservarTentativa("ana@teste.dev", "2.2.2.2"));
    }

    @Test
    void emailComMaiusculasEEspacosContaComoOMesmo() {
        tentar(" Ana@Teste.dev ", "1.1.1.1", 5);

        assertThrows(LoginBloqueadoException.class, () -> protecao.reservarTentativa("ana@teste.dev", "1.1.1.1"));
    }

    @Test
    void ipQueErraEmMuitasContasEhBloqueadoMesmoNumaContaNova() {
        for (int i = 0; i < 30; i++) {
            tentar("conta" + i + "@teste.dev", "6.6.6.6", 1);
        }

        assertThrows(LoginBloqueadoException.class, () -> protecao.reservarTentativa("nova@teste.dev", "6.6.6.6"));
        assertDoesNotThrow(() -> protecao.reservarTentativa("nova@teste.dev", "7.7.7.7"));
    }

    @Test
    void loginCertoZeraOContadorDoEmail() {
        tentar("ana@teste.dev", "1.1.1.1", 4);
        protecao.registrarSucesso("ana@teste.dev");
        tentar("ana@teste.dev", "1.1.1.1", 5);

        assertThrows(LoginBloqueadoException.class, () -> protecao.reservarTentativa("ana@teste.dev", "1.1.1.1"));
    }

    @Test
    void limpezaApagaSoOQueTemMaisDeUmDia() {
        tentar("ana@teste.dev", "1.1.1.1", 2);
        relogio.avancar(Duration.ofHours(25));
        tentar("ana@teste.dev", "1.1.1.1", 1);

        assertEquals(2, protecao.limparAntigas());
        assertEquals(1, registro.falhas.size());
    }

    /** Cada tentativa que passa pela reserva e erra a senha (nunca chama registrarSucesso). */
    private void tentar(String email, String ip, int vezes) {
        for (int i = 0; i < vezes; i++) {
            protecao.reservarTentativa(email, ip);
        }
    }

    private record Falha(UUID id, String email, String ip, Instant quando) { }

    private static final class RegistroEmMemoria implements RegistroDeFalhasDeLogin {
        final List<Falha> falhas = new ArrayList<>();

        private java.util.stream.Stream<Falha> desde(Instant desde) {
            return falhas.stream().filter(f -> !f.quando().isBefore(desde));
        }

        @Override public UUID registrar(String email, String ip, Instant quando) {
            UUID id = UUID.randomUUID();
            falhas.add(new Falha(id, email, ip, quando));
            return id;
        }
        @Override public void apagar(UUID id) { falhas.removeIf(f -> f.id().equals(id)); }
        @Override public long contarPorEmailEIpDesde(String email, String ip, Instant desde) {
            return desde(desde).filter(f -> f.email().equals(email) && ip.equals(f.ip())).count();
        }
        @Override public long contarPorEmailDesde(String email, Instant desde) {
            return desde(desde).filter(f -> f.email().equals(email)).count();
        }
        @Override public long contarPorIpDesde(String ip, Instant desde) {
            return desde(desde).filter(f -> ip.equals(f.ip())).count();
        }
        @Override public Optional<Instant> maisAntigaPorEmailEIpDesde(String email, String ip, Instant desde) {
            return desde(desde).filter(f -> f.email().equals(email) && ip.equals(f.ip())).map(Falha::quando).min(Instant::compareTo);
        }
        @Override public Optional<Instant> maisAntigaPorEmailDesde(String email, Instant desde) {
            return desde(desde).filter(f -> f.email().equals(email)).map(Falha::quando).min(Instant::compareTo);
        }
        @Override public Optional<Instant> maisAntigaPorIpDesde(String ip, Instant desde) {
            return desde(desde).filter(f -> ip.equals(f.ip())).map(Falha::quando).min(Instant::compareTo);
        }
        @Override public void limparEmail(String email) { falhas.removeIf(f -> f.email().equals(email)); }
        @Override public int apagarAnterioresA(Instant limite) {
            int antes = falhas.size();
            falhas.removeIf(f -> f.quando().isBefore(limite));
            return antes - falhas.size();
        }
    }

    private static final class RelogioAjustavel extends Clock {
        private Instant agora;
        RelogioAjustavel(Instant inicio) { this.agora = inicio; }
        void avancar(Duration d) { agora = agora.plus(d); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }
}
