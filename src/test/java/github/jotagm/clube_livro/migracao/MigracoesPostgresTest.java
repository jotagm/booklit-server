package github.jotagm.clube_livro.migracao;

import github.jotagm.auth.otp.Finalidade;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sobe a aplicação num Postgres vazio com o Flyway ligado e o Hibernate em {@code validate}:
 * se uma migração quebrar, ou se uma entidade mudar sem a migração correspondente, o contexto
 * não sobe e o teste falha antes do deploy.
 *
 * <p>Só roda quando {@code BOOKLIT_TEST_PG_URL} aponta para um banco descartável (o CI sobe um
 * Postgres de serviço). O resto da suíte continua em H2.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "BOOKLIT_TEST_PG_URL", matches = ".+")
@TestPropertySource(properties = {
        "spring.datasource.url=${BOOKLIT_TEST_PG_URL}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.username=${BOOKLIT_TEST_PG_USER:postgres}",
        "spring.datasource.password=${BOOKLIT_TEST_PG_PASSWORD:postgres}",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class MigracoesPostgresTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void todasAsMigracoesRodamEOSchemaBateComAsEntidades() {
        List<String> versoes = jdbc.queryForList(
                "select version from flyway_schema_history where success and version is not null order by installed_rank",
                String.class);

        assertTrue(versoes.size() >= 2, "esperava ao menos V1 e V2, veio " + versoes);
        assertEquals("1", versoes.get(0));
    }

    /**
     * O banco tem um CHECK nas finalidades do OTP. Finalidade nova no auth-core sem a migração
     * que a libera só aparecia em produção: todo pedido de código dela falhava ao gravar.
     */
    @Test
    void todasAsFinalidadesDoOtpPodemSerGravadas() {
        for (Finalidade finalidade : Finalidade.values()) {
            jdbc.update("""
                    insert into t_desafio_otp (id, destinatario, finalidade, codigo_hash, tentativas_restantes, criado_em, expira_em)
                    values (?, 'teste@teste.dev', ?, repeat('0', 64), 5, now(), now() + interval '10 minutes')
                    """, UUID.randomUUID(), finalidade.name());
        }
        jdbc.update("delete from t_desafio_otp where destinatario = 'teste@teste.dev'");
    }

    @Test
    void indiceDosEventosPendentesExiste() {
        Integer indices = jdbc.queryForObject(
                "select count(*) from pg_indexes where indexname = 'idx_outbox_evento_pendentes'", Integer.class);

        assertEquals(1, indices);
    }
}
