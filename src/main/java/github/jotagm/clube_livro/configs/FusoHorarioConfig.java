package github.jotagm.clube_livro.configs;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@Slf4j
@Configuration
public class FusoHorarioConfig {

    @Value("${app.timezone:America/Sao_Paulo}")
    private String timezone;

    @PostConstruct
    public void definirFusoPadrao() {
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));

        log.info("Fuso horário da aplicação: {})",
                timezone);
    }

    /**
     * Relógio único da aplicação, no fuso de referência do domínio.
     *
     * <p>Services pedem a hora a ele ({@code LocalDateTime.now(relogio)}) em vez de
     * {@code LocalDateTime.now()}: nos testes dá para fixar o tempo e testar expiração e
     * encerramento sem depender da hora em que a suíte roda. O fuso importa porque os
     * {@code LocalDateTime} do domínio não carregam fuso; para quem usa {@code instant()}
     * (auth-core, limite de login) tanto faz.
     */
    @Bean
    Clock relogio() {
        return Clock.system(ZoneId.of(timezone));
    }
}
