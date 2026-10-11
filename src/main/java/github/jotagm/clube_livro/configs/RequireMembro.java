package github.jotagm.clube_livro.configs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige que o usuário autenticado seja membro (líder ou membro) do clube resolvido pela
 * expressão SpEL em {@link #value()} — mesmo mecanismo de {@link RequireLider}.
 *
 * <p>Usado nas leituras de dados de clube (leituras, registros, votações, membros): os
 * clubes nascem privados, então sem esta checagem bastava conhecer um id para ler tudo.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequireMembro {
    String value();

    /** Quando true, clubes públicos ({@code privado = false}) ficam visíveis a qualquer usuário autenticado. */
    boolean permitirClubePublico() default false;
}
