package github.jotagm.clube_livro.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Respostas de erro da cadeia de segurança, no mesmo formato {@link ErroResponse} que o
 * {@code GlobalExceptionHandler} usa no MVC.
 *
 * <p>Sem isto o Spring Security devolvia 403 com corpo vazio nos dois casos. O front trata
 * 401 como sessão expirada (e desloga) e 403 como falta de permissão — sem a distinção,
 * quem estava com token vencido via "você não tem permissão" e ficava presa na tela.
 *
 * <p>As duas interfaces vivem na mesma classe porque compartilham a escrita do JSON e
 * sempre são configuradas em par.
 */
@Slf4j
@Component
@AllArgsConstructor
public class RestAuthErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /** Não autenticado: falta token, ou ele está expirado/inválido. */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("401 em {} {}: {}", request.getMethod(), request.getRequestURI(), authException.getMessage());

        escrever(response, HttpStatus.UNAUTHORIZED, "Autenticação necessária. Faça login novamente.");
    }

    /** Autenticado, mas sem permissão para o recurso. */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        log.warn("403 em {} {}: {}", request.getMethod(), request.getRequestURI(),
                accessDeniedException.getMessage());

        escrever(response, HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso.");
    }

    private void escrever(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(),
                new ErroResponse(status.value(), mensagem, LocalDateTime.now()));
    }
}
