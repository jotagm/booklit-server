package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.application.port.out.RegistroDeFalhasDeLogin;
import github.jotagm.clube_livro.domain.exceptions.LoginBloqueadoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Limite de senhas erradas no {@code POST /auth/login}, numa janela móvel, em três níveis:
 *
 * <ul>
 *   <li><b>e-mail + IP</b> (baixo, 5): segura força bruta contra uma conta. Só bloqueia quem está
 *       errando: a dona da conta, de outro IP, continua entrando com a senha certa. Sem isso,
 *       qualquer um que soubesse o e-mail de alguém trancaria a conta mandando senhas erradas.</li>
 *   <li><b>IP</b> (médio, 30): segura quem testa a mesma senha em muitas contas.</li>
 *   <li><b>e-mail</b> (alto, 50): segura um ataque distribuído contra uma conta. Para chegar
 *       nele, o atacante precisa de pelo menos 10 IPs, já que cada um para em 5.</li>
 * </ul>
 *
 * <p>A tentativa é reservada antes de conferir a senha (grava a linha e conta junto, na mesma
 * transação): requisições em paralelo não passam todas pela contagem antes de alguma gravar.
 * Bloqueado, a reserva é desfeita, para tentativas recusadas não estenderem o bloqueio dos
 * outros níveis. Senha certa apaga as falhas do e-mail.
 *
 * <p>Fica no Postgres (não em memória) para valer igual com mais de uma instância da API.
 */
@Slf4j
@Service
public class ProtecaoDeLoginService {

    private final RegistroDeFalhasDeLogin registro;
    private final Clock relogio;
    private final Duration janela;
    private final int maxPorEmailEIp;
    private final int maxPorIp;
    private final int maxPorEmail;

    public ProtecaoDeLoginService(RegistroDeFalhasDeLogin registro,
                                  Clock relogio,
                                  @Value("${booklit.auth.login.protecao.janela:15m}") Duration janela,
                                  @Value("${booklit.auth.login.protecao.max-falhas-por-email-e-ip:5}") int maxPorEmailEIp,
                                  @Value("${booklit.auth.login.protecao.max-falhas-por-ip:30}") int maxPorIp,
                                  @Value("${booklit.auth.login.protecao.max-falhas-por-email:50}") int maxPorEmail) {
        this.registro = registro;
        this.relogio = relogio;
        this.janela = janela;
        this.maxPorEmailEIp = maxPorEmailEIp;
        this.maxPorIp = maxPorIp;
        this.maxPorEmail = maxPorEmail;
    }

    /**
     * Reserva a tentativa (conta como falha até {@link #registrarSucesso}) ou lança
     * {@link LoginBloqueadoException} se algum dos limites já foi atingido.
     */
    @Transactional
    public void reservarTentativa(String email, String ip) {
        Instant agora = relogio.instant();
        Instant desde = agora.minus(janela);
        String chave = normalizar(email);

        UUID reserva = registro.registrar(chave, ip, agora);

        // As contagens já incluem a própria reserva: passa enquanto o total não excede o limite.
        Optional<Instant> fimDoBloqueio = Optional.empty();
        if (registro.contarPorEmailEIpDesde(chave, ip, desde) > maxPorEmailEIp) {
            fimDoBloqueio = maisTarde(fimDoBloqueio, registro.maisAntigaPorEmailEIpDesde(chave, ip, desde));
        }
        if (ip != null && registro.contarPorIpDesde(ip, desde) > maxPorIp) {
            fimDoBloqueio = maisTarde(fimDoBloqueio, registro.maisAntigaPorIpDesde(ip, desde));
        }
        if (registro.contarPorEmailDesde(chave, desde) > maxPorEmail) {
            fimDoBloqueio = maisTarde(fimDoBloqueio, registro.maisAntigaPorEmailDesde(chave, desde));
        }

        if (fimDoBloqueio.isPresent()) {
            // A exceção já desfaz a transação; apagar explicitamente deixa a regra clara
            // independente de como o registro guarda as linhas.
            registro.apagar(reserva);
            long segundos = Duration.between(agora, fimDoBloqueio.get().plus(janela)).toSeconds() + 1;
            log.warn("Login bloqueado por excesso de tentativas ip={} por mais {}s", ip, segundos);
            throw new LoginBloqueadoException(segundos);
        }
    }

    @Transactional
    public void registrarSucesso(String email) {
        registro.limparEmail(normalizar(email));
    }

    /** Guarda um dia de histórico (bem mais que a janela) e apaga o resto. */
    @Transactional
    public int limparAntigas() {
        return registro.apagarAnterioresA(relogio.instant().minus(Duration.ofDays(1)));
    }

    private static String normalizar(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    private static Optional<Instant> maisTarde(Optional<Instant> a, Optional<Instant> b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        return Optional.of(a.get().isAfter(b.get()) ? a.get() : b.get());
    }
}
