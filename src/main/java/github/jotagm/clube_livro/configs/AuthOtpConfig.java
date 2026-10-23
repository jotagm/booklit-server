package github.jotagm.clube_livro.configs;

import github.jotagm.auth.otp.EnviadorDeOtp;
import github.jotagm.auth.otp.GeradorDeCodigo;
import github.jotagm.auth.otp.HasherDeCodigo;
import github.jotagm.auth.otp.PoliticaOtp;
import github.jotagm.auth.otp.RepositorioDeDesafios;
import github.jotagm.auth.otp.ServicoOtpEmail;
import github.jotagm.clube_livro.adapter.out.email.SesEnviadorDeOtp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    Clock relogio() {
        return Clock.systemUTC();
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
    @ConditionalOnProperty(name = "booklit.auth.email.habilitado", havingValue = "true")
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
