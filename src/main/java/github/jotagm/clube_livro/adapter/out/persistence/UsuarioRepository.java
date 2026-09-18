package github.jotagm.clube_livro.adapter.out.persistence;

import github.jotagm.clube_livro.domain.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository <Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);
}
