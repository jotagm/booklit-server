package github.jotagm.clube_livro.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME = "bearerAuth";

    @Value("${app.url:}")
    private String appUrl;

    @Bean
    public OpenAPI booklitOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Booklit API")
                        .version("v1")
                        .description("""
                                API do Booklit — clubes de leitura com metas, progresso individual e votação do próximo livro.

                                ## Autenticação
                                Todos os endpoints exigem um token JWT, exceto `POST /auth/login` e `POST /usuarios`.

                                1. Crie um usuário em `POST /usuarios`.
                                2. Autentique em `POST /auth/login` para receber o token.
                                3. Clique em **Authorize** e informe o token (sem o prefixo `Bearer`).

                                ## Leitura ativa
                                Leituras não possuem status persistido: uma leitura está ativa quando
                                `dataInicio <= agora <= dataFim`.
                                """)
                        .contact(new Contact().name("jotagm").url("https://github.com/jotagm"))
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtido em POST /auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));

        if (appUrl != null && !appUrl.isBlank()) {
            openAPI.servers(List.of(
                    new Server().url(appUrl).description("Ambiente configurado em APP_URL"),
                    new Server().url("http://localhost:8080").description("Local")));
        }

        return openAPI;
    }
}
