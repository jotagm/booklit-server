package github.jotagm.clube_livro.adapter.out.persistence.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Uma senha errada no login. Ver {@code V4__falhas_de_login.sql}. */
@Entity
@Table(name = "t_falha_login", indexes = {
        @Index(name = "idx_falha_login_email", columnList = "email, ip, ocorrida_em"),
        @Index(name = "idx_falha_login_ip", columnList = "ip, ocorrida_em")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class FalhaDeLoginEntity {

    @Id
    UUID id;

    @Column(nullable = false, length = 254)
    String email;

    @Column(length = 45)
    String ip;

    @Column(name = "ocorrida_em", nullable = false)
    Instant ocorridaEm;

    FalhaDeLoginEntity(String email, String ip, Instant ocorridaEm) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.ip = ip;
        this.ocorridaEm = ocorridaEm;
    }
}
