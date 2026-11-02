package github.jotagm.clube_livro.configs;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * O CORS é aplicado também quando o front chama pelo proxy do nginx, que repassa o Origin:
 * um método fora da lista volta 403 antes do controller. Foi o que quebrou a edição do perfil
 * (PATCH /usuarios/me/perfil) em produção.
 */
class CorsConfigTest {

    private CorsConfiguration configuracao() {
        CorsConfig cors = new CorsConfig();
        ReflectionTestUtils.setField(cors, "origensPermitidas", List.of("https://booklit.bio"));
        UrlBasedCorsConfigurationSource fonte = (UrlBasedCorsConfigurationSource) cors.corsConfigurationSource();
        return fonte.getCorsConfiguration(new MockHttpServletRequest("GET", "/usuarios/me/perfil"));
    }

    @Test
    void todosOsMetodosQueOFrontUsaEstaoLiberados() {
        CorsConfiguration config = configuracao();

        for (HttpMethod metodo : List.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
                HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS)) {
            assertNotNull(config.checkHttpMethod(metodo), metodo + " precisa estar liberado no CORS");
        }
    }

    @Test
    void metodoForaDaListaContinuaBarrado() {
        assertNull(configuracao().checkHttpMethod(HttpMethod.TRACE));
    }

    @Test
    void origemDoSiteEhAceitaEOutraNao() {
        CorsConfiguration config = configuracao();

        assertNotNull(config.checkOrigin("https://booklit.bio"));
        assertNull(config.checkOrigin("https://outro-site.example"));
    }
}
