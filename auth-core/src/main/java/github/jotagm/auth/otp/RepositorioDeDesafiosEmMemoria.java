package github.jotagm.auth.otp;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementação em memória, para testes e para quem roda uma instância só.
 *
 * <p>A atomicidade vem do {@link ConcurrentHashMap#computeIfPresent}, que executa a função
 * com a entrada travada: é o equivalente local do {@code UPDATE ... WHERE} condicional.
 */
public final class RepositorioDeDesafiosEmMemoria implements RepositorioDeDesafios {

    private final Map<UUID, DesafioOtp> desafios = new ConcurrentHashMap<>();

    @Override
    public void salvar(DesafioOtp desafio) {
        desafios.put(desafio.id(), desafio);
    }

    @Override
    public Optional<DesafioOtp> reservarTentativa(UUID id, Instant agora) {
        AtomicReference<DesafioOtp> reservado = new AtomicReference<>();
        desafios.computeIfPresent(id, (chave, atual) -> {
            if (!atual.ativoEm(agora)) {
                return atual;
            }
            DesafioOtp descontado = new DesafioOtp(atual.id(), atual.destinatario(), atual.finalidade(),
                    atual.codigoHash(), atual.criadoEm(), atual.expiraEm(),
                    atual.tentativasRestantes() - 1, atual.consumidoEm());
            reservado.set(descontado);
            return descontado;
        });
        return Optional.ofNullable(reservado.get());
    }

    @Override
    public boolean consumir(UUID id, Instant agora) {
        AtomicBoolean consumiu = new AtomicBoolean(false);
        desafios.computeIfPresent(id, (chave, atual) -> {
            if (atual.consumidoEm() != null) {
                return atual;
            }
            consumiu.set(true);
            return new DesafioOtp(atual.id(), atual.destinatario(), atual.finalidade(), atual.codigoHash(),
                    atual.criadoEm(), atual.expiraEm(), atual.tentativasRestantes(), agora);
        });
        return consumiu.get();
    }

    @Override
    public Optional<Instant> ultimaEmissao(String destinatario, Finalidade finalidade) {
        return desafios.values().stream()
                .filter(d -> d.destinatario().equals(destinatario) && d.finalidade() == finalidade)
                .map(DesafioOtp::criadoEm)
                .max(Comparator.naturalOrder());
    }

    @Override
    public long contarEmissoesDesde(String destinatario, Finalidade finalidade, Instant desde) {
        return desafios.values().stream()
                .filter(d -> d.destinatario().equals(destinatario) && d.finalidade() == finalidade)
                .filter(d -> !d.criadoEm().isBefore(desde))
                .count();
    }
}
