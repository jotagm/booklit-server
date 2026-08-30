package github.jotagm.clube_livro.configs;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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
}
