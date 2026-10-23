package github.jotagm.clube_livro.adapter.out.persistence.auth;

import github.jotagm.auth.otp.DesafioOtp;
import github.jotagm.auth.otp.Finalidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Linha de {@code t_desafio_otp}. O código nunca é gravado, só o HMAC dele (ver
 * {@code HasherDeCodigo} no auth-core).
 */
@Entity
@Table(name = "t_desafio_otp", indexes = @Index(
        name = "idx_desafio_otp_destinatario",
        columnList = "destinatario, finalidade, criado_em"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class DesafioOtpEntity {

    @Id
    UUID id;

    @Column(nullable = false, length = 254)
    String destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    Finalidade finalidade;

    @Column(name = "codigo_hash", nullable = false, length = 64)
    String codigoHash;

    @Column(name = "criado_em", nullable = false)
    Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    Instant expiraEm;

    @Column(name = "tentativas_restantes", nullable = false)
    int tentativasRestantes;

    @Column(name = "consumido_em")
    Instant consumidoEm;

    static DesafioOtpEntity de(DesafioOtp desafio) {
        DesafioOtpEntity e = new DesafioOtpEntity();
        e.id = desafio.id();
        e.destinatario = desafio.destinatario();
        e.finalidade = desafio.finalidade();
        e.codigoHash = desafio.codigoHash();
        e.criadoEm = desafio.criadoEm();
        e.expiraEm = desafio.expiraEm();
        e.tentativasRestantes = desafio.tentativasRestantes();
        e.consumidoEm = desafio.consumidoEm();
        return e;
    }

    DesafioOtp paraDominio() {
        return new DesafioOtp(id, destinatario, finalidade, codigoHash, criadoEm, expiraEm,
                tentativasRestantes, consumidoEm);
    }
}
