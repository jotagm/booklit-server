package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.UsuarioRequest;
import github.jotagm.clube_livro.adapter.out.persistence.UsuarioRepository;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = buscarPorEmail(email);
        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getSenhaHash())
                .roles("USER")
                .build();
    }

    public Usuario salvar(Usuario usuario) {
        usuario.definirSenhaHash(passwordEncoder.encode(usuario.getSenhaHash()));
        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuario criado id={} email={}", salvo.getId(), salvo.getEmail());
        return salvo;
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
    public Usuario buscarPorId(UUID id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
    /** Sobrecarga mantida para quem já tem a entidade em mãos. */
    public Usuario atualizar(Usuario usuario) {
        usuario.definirSenhaHash(passwordEncoder.encode(usuario.getSenhaHash()));
        return usuarioRepository.save(usuario);
    }

    /**
     * Atualiza a partir do request.
     *
     * <p>A senha em texto puro nunca chega à entidade: o hash é aplicado aqui, onde o
     * {@code PasswordEncoder} vive. Antes o controller enfiava a senha crua no campo
     * {@code senhaHash} e contava com o serviço para criptografá-la depois — funcionava, mas
     * qualquer caminho que salvasse sem passar por aqui gravaria a senha legível no banco.
     */
    public Usuario atualizar(UUID id, UsuarioRequest request) {
        Usuario usuario = buscarPorId(id);
        usuario.atualizarDados(request.nome(), request.email());
        usuario.definirSenhaHash(passwordEncoder.encode(request.senha()));

        log.info("Usuario atualizado id={} email={}", usuario.getId(), usuario.getEmail());

        return usuarioRepository.save(usuario);
    }

}
