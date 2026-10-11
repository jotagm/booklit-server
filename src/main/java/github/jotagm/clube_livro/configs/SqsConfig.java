package github.jotagm.clube_livro.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "booklit.eventos.habilitado", havingValue = "true")
public class SqsConfig {

    @Bean
    SqsClient sqsClient(@Value("${booklit.aws.regiao:us-east-1}") String regiao,
                        @Value("${booklit.aws.endpoint:}") String endpoint) {
        SqsClientBuilder builder = SqsClient.builder().region(Region.of(regiao));
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}
