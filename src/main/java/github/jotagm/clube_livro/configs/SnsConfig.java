package github.jotagm.clube_livro.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "booklit.eventos.habilitado", havingValue = "true")
public class SnsConfig {

    @Bean
    SnsClient snsClient(@Value("${booklit.aws.regiao:us-east-1}") String regiao,
                        @Value("${booklit.aws.endpoint:}") String endpoint) {
        SnsClientBuilder builder = SnsClient.builder().region(Region.of(regiao));
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}