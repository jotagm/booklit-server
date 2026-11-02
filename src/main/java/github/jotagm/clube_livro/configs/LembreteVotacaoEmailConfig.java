package github.jotagm.clube_livro.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.jotagm.clube_livro.adapter.out.email.ClientGoEnviadorDeLembreteDeVotacao;
import github.jotagm.clube_livro.application.port.out.EnviadorDeLembreteDeVotacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Lembrete por e-mail de quem não votou. Chave própria
 * ({@code booklit.votacoes.lembrete.email.habilitado}), como a do convite; usa o mesmo Client_GO
 * ({@code booklit.auth.email.client-go.*}). Desligado, o lembrete continua registrado (e limita
 * o próximo), só não sai e-mail.
 */
@Configuration
public class LembreteVotacaoEmailConfig {

    private static final Logger log = LoggerFactory.getLogger(LembreteVotacaoEmailConfig.class);

    @Bean
    @ConditionalOnProperty(name = "booklit.votacoes.lembrete.email.habilitado", havingValue = "true")
    EnviadorDeLembreteDeVotacao enviadorDeLembreteDeVotacaoClientGo(
            ObjectMapper json,
            @Value("${booklit.auth.email.client-go.url}") String url,
            @Value("${booklit.auth.email.client-go.token:}") String token,
            @Value("${booklit.auth.email.client-go.timeout:2s}") Duration timeout,
            @Value("${booklit.convites.email.front-url:https://booklit.bio}") String frontUrl) {
        return new ClientGoEnviadorDeLembreteDeVotacao(json, url, token, timeout, frontUrl);
    }

    @Bean
    @ConditionalOnProperty(name = "booklit.votacoes.lembrete.email.habilitado", havingValue = "false", matchIfMissing = true)
    EnviadorDeLembreteDeVotacao enviadorDeLembreteDeVotacaoDesligado() {
        return mensagem -> log.debug("Lembrete de votação desligado (booklit.votacoes.lembrete.email.habilitado=false) votacao={}",
                mensagem.votacaoId());
    }
}
