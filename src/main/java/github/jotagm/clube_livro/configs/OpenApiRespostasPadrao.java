package github.jotagm.clube_livro.configs;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.ErroResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

/**
 * Evita repetir as respostas de erro comuns em cada endpoint: registra o schema de
 * {@link ErroResponse} uma vez e anexa 400/401/403 a todas as operações documentadas.
 */
@Configuration
public class OpenApiRespostasPadrao {

    private static final String ERRO_SCHEMA_REF = "#/components/schemas/ErroResponse";

    @Bean
    public OpenApiCustomizer registrarSchemaDeErro() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            ModelConverters.getInstance()
                    .readAll(ErroResponse.class)
                    .forEach(openApi.getComponents()::addSchemas);
        };
    }

    @Bean
    public OperationCustomizer respostasDeErroPadrao() {
        return (operation, handlerMethod) -> {
            ApiResponses respostas = operation.getResponses();

            adicionar(respostas, "400", "Requisição inválida — corpo malformado ou validação de campo falhou");

            if (!ehPublico(handlerMethod)) {
                adicionar(respostas, "401", "Token JWT ausente, expirado ou inválido");
                adicionar(respostas, "403", "Autenticado, mas sem permissão para o recurso");
            }

            return operation;
        };
    }

    private boolean ehPublico(HandlerMethod handlerMethod) {
        String classe = handlerMethod.getBeanType().getSimpleName();
        return classe.equals("AuthController")
                || (classe.equals("UsuarioController") && handlerMethod.getMethod().getName().equals("criar"));
    }

    private void adicionar(ApiResponses respostas, String codigo, String descricao) {
        if (respostas.containsKey(codigo)) {
            return;
        }
        respostas.addApiResponse(codigo, new ApiResponse()
                .description(descricao)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref(ERRO_SCHEMA_REF)))));
    }
}
