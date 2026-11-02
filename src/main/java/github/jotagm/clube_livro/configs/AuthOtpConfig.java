package github.jotagm.clube_livro.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.jotagm.auth.otp.EnviadorDeOtp;
import github.jotagm.auth.otp.GeradorDeCodigo;
import github.jotagm.auth.otp.HasherDeCodigo;
import github.jotagm.auth.otp.PoliticaOtp;
import github.jotagm.auth.otp.RepositorioDeDesafios;
import github.jotagm.auth.otp.ServicoOtpEmail;
import github.jotagm.clube_livro.adapter.out.email.ClientGoEnviadorDeOtp;
import github.jotagm.clube_livro.adapter.out.email.SesEnviadorDeOtp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.SesV2ClientBuilder;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

/**
 * Liga o auth-core ao Spring: política, chave do HMAC, persistência e entrega.
 *
 * <p>O {@link ServicoOtpEmail} é puro; aqui ele só ganha as implementações das portas.
 */
@Slf4j
@Configuration
public class AuthOtpConfig {

    @Bean
    PoliticaOtp politicaOtp(@Value("${booklit.auth.otp.validade:10m}") Duration validade,
                            @Value("${booklit.auth.otp.max-tentativas:5}") int maxTentativas,
                            @Value("${booklit.auth.otp.intervalo-reenvio:60s}") Duration intervaloReenvio,
                            @Value("${booklit.auth.otp.max-envios-por-hora:5}") int maxEnviosPorHora) {
        return new PoliticaOtp(validade, maxTentativas, intervaloReenvio, maxEnviosPorHora, Duration.ofHours(1));
    }

    /**
     * Chave do HMAC em base64, 32 bytes ou mais ({@code openssl rand -base64 32}).
     *
     * <p>Sem a variável o contexto sobe com uma chave aleatória, para dev e testes não
     * dependerem de configuração. O custo: códigos emitidos antes de um restart deixam de
     * valer, e com mais de uma instância cada uma teria a sua. Em produção, defina.
     */
    @Bean
    HasherDeCodigo hasherDeCodigo(@Value("${booklit.auth.otp.chave-hmac:}") String chaveBase64) {
        if (chaveBase64.isBlank()) {
            log.warn("BOOKLIT_OTP_CHAVE_HMAC não definida: usando chave aleatória desta instância. "
                    + "Códigos OTP não sobrevivem a restart.");
            byte[] chave = new byte[32];
            new SecureRandom().nextBytes(chave);
            return new HasherDeCodigo(chave);
        }
        return new HasherDeCodigo(Base64.getDecoder().decode(chaveBase64.strip()));
    }

    @Bean
    ServicoOtpEmail servicoOtpEmail(RepositorioDeDesafios repositorio,
                                    EnviadorDeOtp enviador,
                                    HasherDeCodigo hasher,
                                    PoliticaOtp politica,
                                    Clock relogio) {
        return new ServicoOtpEmail(repositorio, enviador, GeradorDeCodigo.seguro(), hasher, politica, relogio);
    }

    @Configuration
    @ConditionalOnExpression("'${booklit.auth.email.habilitado:false}' == 'true' "
            + "and '${booklit.auth.email.provedor:ses}' == 'ses'")
    static class EnvioPorSes {

        @Bean
        SesV2Client sesV2Client(@Value("${booklit.aws.regiao:us-east-1}") String regiao,
                                @Value("${booklit.aws.endpoint:}") String endpoint) {
            SesV2ClientBuilder builder = SesV2Client.builder().region(Region.of(regiao));
            if (!endpoint.isBlank()) {
                builder.endpointOverride(URI.create(endpoint));
            }
            return builder.build();
        }

        @Bean
        EnviadorDeOtp enviadorDeOtp(SesV2Client ses,
                                    @Value("${booklit.auth.email.remetente:no-reply@booklit.bio}") String remetente) {
            return new SesEnviadorDeOtp(ses, remetente);
        }
    }

    /**
     * Entrega pelo Client_GO (SNS/SQS + SES, com histórico e retentativa) em vez de chamar o
     * SES direto. O auth-core não muda: só a implementação da porta {@link EnviadorDeOtp}.
     */
    @Configuration
    @ConditionalOnExpression("'${booklit.auth.email.habilitado:false}' == 'true' "
            + "and '${booklit.auth.email.provedor:ses}' == 'client-go'")
    static class EnvioPorClientGo {

        @Bean
        EnviadorDeOtp enviadorDeOtp(ObjectMapper json,
                                    @Value("${booklit.auth.email.client-go.url}") String url,
                                    @Value("${booklit.auth.email.client-go.token:}") String token,
                                    @Value("${booklit.auth.email.client-go.timeout:2s}") Duration timeout) {
            return new ClientGoEnviadorDeOtp(json, url, token, timeout);
        }
    }

    /**
     * Provedor desconhecido (um erro de digitação em BOOKLIT_EMAIL_PROVEDOR) derruba a subida:
     * cair em silêncio no SES, ou em "desligado", esconderia o erro até o primeiro login.
     */
    @Configuration
    @ConditionalOnExpression("'${booklit.auth.email.habilitado:false}' == 'true' "
            + "and '${booklit.auth.email.provedor:ses}' != 'ses' "
            + "and '${booklit.auth.email.provedor:ses}' != 'client-go'")
    static class EnvioProvedorInvalido {

        @Bean
        EnviadorDeOtp enviadorDeOtp(@Value("${booklit.auth.email.provedor}") String provedor) {
            throw new IllegalStateException("booklit.auth.email.provedor inválido: '" + provedor
                    + "' (use 'ses' ou 'client-go')");
        }
    }

    /**
     * Sem e-mail configurado o endpoint continua de pé, mas nada sai. O log avisa sem expor o
     * código: para ver o código em dev, ligue o envio apontando para o Floci.
     */
    @Configuration
    @ConditionalOnProperty(name = "booklit.auth.email.habilitado", havingValue = "false", matchIfMissing = true)
    static class EnvioDesligado {

        @Bean
        EnviadorDeOtp enviadorDeOtp() {
            return mensagem -> log.warn("Envio de e-mail desligado (booklit.auth.email.habilitado=false); "
                    + "OTP do desafio {} não foi entregue", mensagem.desafioId());
        }
    }
}
