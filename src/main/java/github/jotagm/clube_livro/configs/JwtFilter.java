package github.jotagm.clube_livro.configs;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@AllArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final Clock relogio;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            String email = jwtService.extrairEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValido(token, userDetails.getUsername()) && !revogado(token, userDetails)) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    request.setAttribute(RequestLoggingFilter.ATRIBUTO_USUARIO, userDetails.getUsername());
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar token JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Token emitido antes da última redefinição de senha não vale mais. O {@code iat} tem
     * segundos inteiros, por isso a comparação também: o token emitido no mesmo segundo da troca
     * (o que a própria redefinição devolve) continua valendo.
     */
    private boolean revogado(String token, UserDetails userDetails) {
        if (!(userDetails instanceof UsuarioAutenticado u) || u.getSenhaAlteradaEm() == null) return false;
        Instant alteradaEm = u.getSenhaAlteradaEm().atZone(relogio.getZone()).toInstant().truncatedTo(ChronoUnit.SECONDS);
        boolean revogado = jwtService.emitidoEm(token).isBefore(alteradaEm);
        if (revogado) log.info("Token anterior à redefinição de senha recusado");
        return revogado;
    }
}
