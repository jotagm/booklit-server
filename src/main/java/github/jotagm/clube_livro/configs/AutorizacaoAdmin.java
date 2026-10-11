package github.jotagm.clube_livro.configs;

import github.jotagm.clube_livro.domain.exceptions.AcessoNegadoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Administração do catálogo global (temas). Não existe papel de admin no banco ainda: os
 * e-mails vêm de {@code booklit.admin.emails}. Sem a property, ninguém altera nem apaga temas
 * — antes qualquer usuário logado podia apagar um tema usado por todos os clubes.
 */
@Component
public class AutorizacaoAdmin {

    private final Set<String> emailsAdmin;

    public AutorizacaoAdmin(@Value("${booklit.admin.emails:}") String emails) {
        this.emailsAdmin = Arrays.stream(emails.split(","))
                .map(String::trim)
                .filter(e -> !e.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public void exigirAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (email == null || !emailsAdmin.contains(email.toLowerCase())) {
            throw new AcessoNegadoException("Acesso negado: apenas administradores podem alterar temas");
        }
    }
}
