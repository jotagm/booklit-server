package github.jotagm.clube_livro.adapter.out.persistence.auth;

import github.jotagm.auth.otp.Finalidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

interface DesafioOtpJpaRepository extends JpaRepository<DesafioOtpEntity, UUID> {

    /**
     * Gasta uma tentativa num UPDATE só. A condição inteira vai no WHERE: duas requisições
     * paralelas disputam o lock da linha e a segunda reavalia o WHERE depois do commit da
     * primeira (READ COMMITTED), então a última tentativa nunca é gasta duas vezes.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update DesafioOtpEntity d
               set d.tentativasRestantes = d.tentativasRestantes - 1
             where d.id = :id
               and d.consumidoEm is null
               and d.tentativasRestantes > 0
               and d.expiraEm > :agora
            """)
    int reservarTentativa(@Param("id") UUID id, @Param("agora") Instant agora);

    /** Uso único: só a primeira requisição encontra {@code consumido_em} nulo. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update DesafioOtpEntity d
               set d.consumidoEm = :agora
             where d.id = :id
               and d.consumidoEm is null
            """)
    int consumir(@Param("id") UUID id, @Param("agora") Instant agora);

    @Query("""
            select max(d.criadoEm) from DesafioOtpEntity d
             where d.destinatario = :destinatario and d.finalidade = :finalidade
            """)
    Instant ultimaEmissao(@Param("destinatario") String destinatario, @Param("finalidade") Finalidade finalidade);

    long countByDestinatarioAndFinalidadeAndCriadoEmGreaterThanEqual(String destinatario,
                                                                       Finalidade finalidade,
                                                                       Instant desde);
}
