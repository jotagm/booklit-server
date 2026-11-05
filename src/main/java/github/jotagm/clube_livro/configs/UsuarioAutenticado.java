package github.jotagm.clube_livro.configs;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * O {@link User} do Spring com o momento da última redefinição de senha, para o
 * {@link JwtFilter} recusar tokens emitidos antes dela sem uma segunda consulta ao banco.
 */
public class UsuarioAutenticado extends User {

    private final transient LocalDateTime senhaAlteradaEm;

    public UsuarioAutenticado(String email, String senhaHash, Collection<? extends GrantedAuthority> autoridades,
                              LocalDateTime senhaAlteradaEm) {
        super(email, senhaHash, autoridades);
        this.senhaAlteradaEm = senhaAlteradaEm;
    }

    public LocalDateTime getSenhaAlteradaEm() {
        return senhaAlteradaEm;
    }
}
