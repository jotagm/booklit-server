package github.jotagm.clube_livro.configs;

import github.jotagm.clube_livro.adapter.out.storage.S3ArmazenamentoDeFotos;
import github.jotagm.clube_livro.application.port.out.ArmazenamentoDeFotos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * Fotos de perfil. Com {@code booklit.fotos.bucket} preenchido, usa o S3; sem ele a API segue
 * de pé, os endpoints de foto respondem 503 e o perfil fica sem foto (dev e testes).
 */
@Configuration
public class FotosConfig {

    @Configuration
    @ConditionalOnExpression("!'${booklit.fotos.bucket:}'.isBlank()")
    static class ComS3 {

        @Bean
        ArmazenamentoDeFotos armazenamentoDeFotos(
                @Value("${booklit.fotos.bucket}") String bucket,
                @Value("${booklit.aws.regiao:us-east-1}") String regiao,
                @Value("${booklit.aws.endpoint:}") String endpoint,
                @Value("${booklit.fotos.validade-envio:10m}") Duration validadeEnvio,
                @Value("${booklit.fotos.validade-leitura:1h}") Duration validadeLeitura) {
            var region = Region.of(regiao);
            var s3 = S3Client.builder().region(region);
            var presigner = S3Presigner.builder().region(region);
            if (!endpoint.isBlank()) {
                // Floci e afins: endereço fixo e bucket no caminho, não no host.
                s3.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
                presigner.endpointOverride(URI.create(endpoint))
                        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
            }
            return new S3ArmazenamentoDeFotos(s3.build(), presigner.build(), bucket, validadeEnvio, validadeLeitura);
        }
    }

    @Configuration
    @ConditionalOnExpression("'${booklit.fotos.bucket:}'.isBlank()")
    static class Desligado {

        @Bean
        ArmazenamentoDeFotos armazenamentoDeFotos() {
            return new ArmazenamentoDeFotos() {
                @Override
                public boolean habilitado() {
                    return false;
                }

                @Override
                public EnvioAssinado prepararEnvio(String chave, String contentType, long tamanho) {
                    throw new IllegalStateException("fotos desligadas");
                }

                @Override
                public Optional<Objeto> inspecionar(String chave) {
                    return Optional.empty();
                }

                @Override
                public String urlDeLeitura(String chave) {
                    throw new IllegalStateException("fotos desligadas");
                }

                @Override
                public void apagar(String chave) {
                }
            };
        }
    }
}
