package github.jotagm.clube_livro.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Registra uma linha por requisição com método, rota, status e duração.
 *
 * <p>Coloca também um {@code requestId} no MDC, então todo log emitido durante a requisição
 * carrega o mesmo identificador — é o que permite juntar as linhas de uma chamada específica
 * quando várias acontecem ao mesmo tempo.
 */
@Slf4j
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID = "requestId";

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

            // O usuário só existe depois que o JwtFilter rodou, por isso é lido aqui no finally.
            String usuario = usuarioAutenticado();

            if (status >= 500) {
                log.error("{} {}{} -> {} ({}ms) usuario={}", request.getMethod(), rota,
                        query != null ? "?" + query : "", status, duracao, usuario);
            } else if (status >= 400) {
                log.warn("{} {}{} -> {} ({}ms) usuario={}", request.getMethod(), rota,
                        query != null ? "?" + query : "", status, duracao, usuario);
            } else {
                log.info("{} {} -> {} ({}ms) usuario={}", request.getMethod(), rota, status, duracao, usuario);
            }

            MDC.remove(REQUEST_ID);
        }
    }

    private String usuarioAutenticado() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "anonimo";
        }
        return auth.getName();
    }

    /** Documentação e health check poluiriam o log sem contar nada de útil. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String rota = request.getRequestURI();
        return rota.startsWith("/swagger-ui") || rota.startsWith("/v3/api-docs") || rota.equals("/favicon.ico");
    }
}
