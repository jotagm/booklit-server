package github.jotagm.clube_livro.configs;

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
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@Component
@AllArgsConstructor
public class RequireLiderAspect {

    private final AutorizacaoClube autorizacaoClube;
    private final BeanFactory beanFactory;

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Before("@annotation(requireLider)")
    public void verificar(JoinPoint joinPoint, RequireLider requireLider) {
        UUID clubeId = resolverClubeId(joinPoint, requireLider.value());
        Usuario usuario = autorizacaoClube.usuarioAutenticado();

        if (!autorizacaoClube.isLider(usuario.getId(), clubeId)) {
            throw new AcessoNegadoException();
        }
    }

    @Before("@annotation(requireMembro)")
    public void verificarMembro(JoinPoint joinPoint, RequireMembro requireMembro) {
        UUID clubeId = resolverClubeId(joinPoint, requireMembro.value());
        if (requireMembro.permitirClubePublico() && autorizacaoClube.isClubePublico(clubeId)) {
            return;
        }
        autorizacaoClube.exigirMembro(clubeId);
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
