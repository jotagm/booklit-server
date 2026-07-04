package github.jotagm.clube_livro.configs;

import github.jotagm.clube_livro.application.service.UsuarioClubeService;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.exceptions.AcessoNegadoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@Component
@AllArgsConstructor
public class RequireLiderAspect {

    private final UsuarioService usuarioService;
    private final UsuarioClubeService usuarioClubeService;
    private final BeanFactory beanFactory;

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Before("@annotation(requireLider)")
    public void verificar(JoinPoint joinPoint, RequireLider requireLider) {
        UUID clubeId = resolverClubeId(joinPoint, requireLider.value());
        Usuario usuario = usuarioAutenticado();

        if (!isLider(usuario.getId(), clubeId)) {
            throw new AcessoNegadoException();
        }
    }

    private Usuario usuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.buscarPorEmail(email);
    }

    private boolean isLider(UUID usuarioId, UUID clubeId) {
        try {
            UsuarioClube usuarioClube = usuarioClubeService.buscarPorUsuarioEClube(usuarioId, clubeId);
            return usuarioClube.getPapel() == ClubePapel.LIDER;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private UUID resolverClubeId(JoinPoint joinPoint, String expressao) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String[] nomesParametros = parameterNameDiscoverer.getParameterNames(method);
        Object[] valores = joinPoint.getArgs();

        StandardEvaluationContext contexto = new StandardEvaluationContext();
        contexto.setBeanResolver(new BeanFactoryResolver(beanFactory));

        if (nomesParametros != null) {
            for (int i = 0; i < nomesParametros.length; i++) {
                contexto.setVariable(nomesParametros[i], valores[i]);
            }
        }

        Expression expression = parser.parseExpression(expressao);
        return expression.getValue(contexto, UUID.class);
    }
}
