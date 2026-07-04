package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.UsuarioRepository;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void salvar_deveEncodarSenhaESalvarUsuario() {
        Usuario usuario = new Usuario(null, "João", "joao@email.com", "senha123", null);
        when(passwordEncoder.encode("senha123")).thenReturn("senhaEncoded");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Usuario resultado = usuarioService.salvar(usuario);

        assertThat(resultado.getSenhaHash()).isEqualTo("senhaEncoded");
        verify(passwordEncoder).encode("senha123");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void buscarPorEmail_deveRetornarUsuarioQuandoEncontrado() {
        Usuario usuario = new Usuario(null, "João", "joao@email.com", "hash", null);
        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuario));

        Usuario resultado = usuarioService.buscarPorEmail("joao@email.com");

        assertThat(resultado.getEmail()).isEqualTo("joao@email.com");
    }

    @Test
    void buscarPorEmail_deveLancarExcecaoQuandoNaoEncontrado() {
        when(usuarioRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorEmail("nao@existe.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    void buscarPorId_deveRetornarUsuarioQuandoEncontrado() {
        UUID id = UUID.randomUUID();
        Usuario usuario = new Usuario(id, "João", "joao@email.com", "hash", null);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        Usuario resultado = usuarioService.buscarPorId(id);

        assertThat(resultado.getId()).isEqualTo(id);
    }

    @Test
    void buscarPorId_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorId(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    void atualizar_deveEncodarSenhaESalvarUsuario() {
        Usuario usuario = new Usuario(UUID.randomUUID(), "João", "joao@email.com", "novaSenha", null);
        when(passwordEncoder.encode("novaSenha")).thenReturn("novaSenhaEncoded");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Usuario resultado = usuarioService.atualizar(usuario);

        assertThat(resultado.getSenhaHash()).isEqualTo("novaSenhaEncoded");
        verify(passwordEncoder).encode("novaSenha");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void loadUserByUsername_deveRetornarUserDetailsComEmailERoleUser() {
        Usuario usuario = new Usuario(null, "João", "joao@email.com", "senhaHash", null);
        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuario));

        UserDetails resultado = usuarioService.loadUserByUsername("joao@email.com");

        assertThat(resultado.getUsername()).isEqualTo("joao@email.com");
        assertThat(resultado.getPassword()).isEqualTo("senhaHash");
        assertThat(resultado.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }
}
