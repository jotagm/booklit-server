package github.jotagm.clube_livro.configs;

import github.jotagm.clube_livro.adapter.out.persistence.ClubeRepository;
import github.jotagm.clube_livro.adapter.out.persistence.UsuarioClubeRepository;
import github.jotagm.clube_livro.application.service.UsuarioService;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.exceptions.AcessoNegadoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Ponto único das regras "quem pode o quê" em um clube. As anotações {@link RequireLider} e
 * {@link RequireMembro} cobrem os casos declarativos; os controllers chamam este componente
 * direto quando a regra depende de mais de um dado (ex: autor OU líder).
 */
@Component("autorizacaoClube")
@AllArgsConstructor
public class AutorizacaoClube {

    private final UsuarioService usuarioService;
    private final UsuarioClubeRepository usuarioClubeRepository;
    private final ClubeRepository clubeRepository;

    public Usuario usuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.buscarPorEmail(email);
    }

    public boolean isMembro(Integer usuarioId, UUID clubeId) {
        return usuarioClubeRepository.findByUsuarioIdAndClubeId(usuarioId, clubeId).isPresent();
    }

    public boolean isLider(Integer usuarioId, UUID clubeId) {
        return usuarioClubeRepository.findByUsuarioIdAndClubeId(usuarioId, clubeId)
                .map(uc -> uc.getPapel() == ClubePapel.LIDER)
                .orElse(false);
    }

    public boolean isClubePublico(UUID clubeId) {
        return clubeRepository.findById(clubeId).map(c -> !c.isPrivado()).orElse(false);
    }

    public void exigirMembro(UUID clubeId) {
        exigirMembro(usuarioAutenticado().getId(), clubeId);
    }

    public void exigirMembro(Integer usuarioId, UUID clubeId) {
        if (!isMembro(usuarioId, clubeId)) {
            throw new AcessoNegadoException("Acesso negado: apenas membros do clube podem acessar este recurso");
        }
    }

    public void exigirLider(UUID clubeId) {
        if (!isLider(usuarioAutenticado().getId(), clubeId)) {
            throw new AcessoNegadoException();
        }
    }

    /** Recurso pessoal: só o próprio usuário autenticado pode acessá-lo. */
    public void exigirProprioUsuario(Integer usuarioId) {
        if (!usuarioAutenticado().getId().equals(usuarioId)) {
            throw new AcessoNegadoException("Acesso negado: este recurso pertence a outro usuário");
        }
    }
}
