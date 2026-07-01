package github.jotagm.clube_livro.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KeepAliveScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveScheduler.class);

    @Value("${app.url:}")
    private String appUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void keepAlive() {
        if (appUrl == null || appUrl.isBlank()) return;

        try {
            restTemplate.getForObject(appUrl + "/actuator/health", String.class);
            log.info("Keep-alive ping enviado para {}", appUrl);
        } catch (Exception e) {
            log.warn("Keep-alive falhou: {}", e.getMessage());
        }
    }
}
