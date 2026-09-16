package github.jotagm.clube_livro.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Libera o front (que roda em outro dominio) para chamar a API pelo navegador.
 *
 * Sem isto, o preflight OPTIONS que o navegador dispara antes de todo POST/PUT com
 * Content-Type: application/json bate na regra anyRequest().authenticated() do
 * SecurityConfig e volta 401 - o preflight nao carrega o header Authorization.
 */
@Configuration
public class CorsConfig {

    /**
     * Lista separada por virgula. Aceita curinga por segmento, entao
     * "https://*.up.railway.app" cobre os dominios de preview do Railway.
     */
    @Value("${cors.allowed-origins}")
    private List<String> origensPermitidas;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Espaco depois da virgula na variavel de ambiente viraria parte do dominio e
        // faria o match falhar silenciosamente.
        List<String> origens = origensPermitidas.stream()
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origens);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // O token vai no header Authorization, nunca em cookie: nao ha credencial a enviar.
        config.setAllowCredentials(false);
        // Evita um preflight por requisicao enquanto o cache do navegador durar.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
