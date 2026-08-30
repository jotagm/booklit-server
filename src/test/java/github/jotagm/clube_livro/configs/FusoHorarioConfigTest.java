package github.jotagm.clube_livro.configs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O domínio usa LocalDateTime, que não carrega fuso. Se a JVM subir em UTC — como acontece
 * no container da Railway — tudo que o servidor grava e compara fica deslocado em relação
 * ao horário de parede que o usuário vê no navegador.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:fuso;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1hcGVuYXMtcGFyYS1vLWNvbnRleHRvLWRlLXRlc3Rlcw==",
        "google.books.api-key=chave-de-teste"
})
class FusoHorarioConfigTest {

    @MockitoBean private RestClient restClient;

    @Autowired private FusoHorarioConfig fusoHorarioConfig;

    @Test
    void jvmAssumeOFusoDoDominioIndependenteDoAmbiente() {
        assertThat(ZoneId.systemDefault().getId()).isEqualTo("America/Sao_Paulo");
    }

    @Test
    void agoraDoServidorBateComOHorarioDeParedeEmSaoPaulo() {
        LocalDateTime doServidor = LocalDateTime.now();
        LocalDateTime emSaoPaulo = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();

        // Rodando em UTC a diferença seria de 3 horas.
        assertThat(Duration.between(emSaoPaulo, doServidor).abs())
                .isLessThan(Duration.ofSeconds(5));
    }
}
