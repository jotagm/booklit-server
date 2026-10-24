package github.jotagm.clube_livro.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.UUID;

/**
 * Registra uma linha por requisição com método, rota, status, duração, usuário e o handler
 * que atendeu ({@code ClubeController#buscarPorId}), no formato que a ferramenta
 * {@code tools/blogs} lê para filtrar por controller.
 *
 * <p>Coloca também um {@code requestId} no MDC, então todo log emitido durante a requisição
 * carrega o mesmo identificador — é o que permite juntar as linhas de uma chamada específica
 * quando várias acontecem ao mesmo tempo.
 */
@Slf4j
@Component
// Antes de tudo, inclusive do Spring Security (-100): com @Order(1) um 401 do Security
// encerrava a requisição antes deste filtro e o acesso nunca aparecia no log.
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID = "requestId";

    /** Preenchido pelo JwtFilter: quando o finally roda, o SecurityContext já foi limpo. */
    public static final String ATRIBUTO_USUARIO = RequestLoggingFilter.class.getName() + ".usuario";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long inicio = System.currentTimeMillis();
        MDC.put(REQUEST_ID, UUID.randomUUID().toString().substring(0, 8));

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duracao = System.currentTimeMillis() - inicio;
            int status = response.getStatus();
            String rota = request.getRequestURI();
            String query = request.getQueryString();

            Object autenticado = request.getAttribute(ATRIBUTO_USUARIO);
            String usuario = autenticado != null ? autenticado.toString() : "anonimo";
            String handler = handler(request);

            if (status >= 500) {
                log.error("{} {}{} -> {} ({}ms) usuario={} handler={}", request.getMethod(), rota,
                        query != null ? "?" + query : "", status, duracao, usuario, handler);
            } else if (status >= 400) {
                log.warn("{} {}{} -> {} ({}ms) usuario={} handler={}", request.getMethod(), rota,
                        query != null ? "?" + query : "", status, duracao, usuario, handler);
            } else {
                log.info("{} {} -> {} ({}ms) usuario={} handler={}", request.getMethod(), rota, status, duracao,
                        usuario, handler);
            }

            MDC.remove(REQUEST_ID);
        }
    }

    /**
     * Quem atendeu a requisição. O Spring guarda o handler escolhido num atributo do request,
     * que continua lá depois do filterChain. Sem handler (401 do Spring Security, 404 de rota
     * inexistente) fica "-".
     */
    private String handler(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handler instanceof HandlerMethod metodo) {
            return metodo.getBeanType().getSimpleName() + "#" + metodo.getMethod().getName();
        }
        return "-";
    }

    /** Documentação e health check poluiriam o log sem contar nada de útil. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String rota = request.getRequestURI();
        return rota.startsWith("/swagger-ui") || rota.startsWith("/v3/api-docs") || rota.equals("/favicon.ico");
    }
}
