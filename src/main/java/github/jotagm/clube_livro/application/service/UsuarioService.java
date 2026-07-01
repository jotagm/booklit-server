package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.UsuarioRepository;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
        usuario.setSenhaHash(passwordEncoder.encode(usuario.getSenhaHash()));
        return usuarioRepository.save(usuario);
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
    public Usuario buscarPorId(UUID id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
    public Usuario atualizar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

}
