package github.jotagm.clube_livro.adapter.out.persistence.auth;

import github.jotagm.auth.otp.DesafioOtp;
import github.jotagm.auth.otp.Finalidade;
import github.jotagm.auth.otp.RepositorioDeDesafios;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter Postgres da porta do auth-core.
 *
 * <p>Fica no Postgres e não no Redis porque o Booklit é um monólito com um banco só: o
 * {@code UPDATE ... WHERE} condicional já dá a atomicidade que a porta exige, sem mais uma
 * peça de infra no deploy. Se um dia o volume pedir, um adapter Redis (script Lua) entra no
 * lugar deste sem tocar no serviço.
 */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class RepositorioDeDesafiosJpa implements RepositorioDeDesafios {

    private final DesafioOtpJpaRepository jpa;

    @Override
    public void salvar(DesafioOtp desafio) {
        jpa.save(DesafioOtpEntity.de(desafio));
    }

    @Override
    public Optional<DesafioOtp> reservarTentativa(UUID id, Instant agora) {
        if (jpa.reservarTentativa(id, agora) == 0) {
            return Optional.empty();
        }
        // Mesma transação e linha já travada pelo UPDATE: o que vem aqui é o estado pós-desconto.
        return jpa.findById(id).map(DesafioOtpEntity::paraDominio);
    }

    @Override
    public boolean consumir(UUID id, Instant agora) {
        return jpa.consumir(id, agora) == 1;
    }

    @Override
    public Optional<Instant> ultimaEmissao(String destinatario, Finalidade finalidade) {
        return Optional.ofNullable(jpa.ultimaEmissao(destinatario, finalidade));
    }

    @Override
    public long contarEmissoesDesde(String destinatario, Finalidade finalidade, Instant desde) {
        return jpa.countByDestinatarioAndFinalidadeAndCriadoEmGreaterThanEqual(destinatario, finalidade, desde);
    }
}
