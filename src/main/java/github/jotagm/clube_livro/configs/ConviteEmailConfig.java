package github.jotagm.clube_livro.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.jotagm.clube_livro.adapter.out.email.ClientGoEnviadorDeConvite;
import github.jotagm.clube_livro.application.port.out.EnviadorDeConvite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Aviso por e-mail de convite de clube. Fica atrás da própria chave
 * ({@code booklit.convites.email.habilitado}), separada da do código OTP: dá para ligar um sem
 * o outro. Usa o mesmo Client_GO do OTP ({@code booklit.auth.email.client-go.*}).
 */
@Configuration
public class ConviteEmailConfig {

    private static final Logger log = LoggerFactory.getLogger(ConviteEmailConfig.class);

    @Bean
    @ConditionalOnProperty(name = "booklit.convites.email.habilitado", havingValue = "true")
    EnviadorDeConvite enviadorDeConviteClientGo(
            ObjectMapper json,
            @Value("${booklit.auth.email.client-go.url}") String url,
            @Value("${booklit.auth.email.client-go.token:}") String token,
            @Value("${booklit.auth.email.client-go.timeout:2s}") Duration timeout,
            @Value("${booklit.convites.email.front-url:https://booklit.bio}") String frontUrl) {
        return new ClientGoEnviadorDeConvite(json, url, token, timeout, frontUrl);
    }

    @Bean
    @ConditionalOnProperty(name = "booklit.convites.email.habilitado", havingValue = "false", matchIfMissing = true)
    EnviadorDeConvite enviadorDeConviteDesligado() {
        return mensagem -> log.debug("Aviso de convite desligado (booklit.convites.email.habilitado=false) convite={}",
                mensagem.conviteId());
    }
}
