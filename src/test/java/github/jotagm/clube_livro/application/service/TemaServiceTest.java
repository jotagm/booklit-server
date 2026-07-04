package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.TemaRepository;
import github.jotagm.clube_livro.domain.tema.Tema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemaServiceTest {

    @Mock
    private TemaRepository temaRepository;

    @InjectMocks
    private TemaService temaService;

    @Test
    void salvar_deveSalvarERetornarTema() {
        Tema tema = new Tema(null, "Ficção Científica");
        when(temaRepository.save(tema)).thenReturn(tema);

        Tema resultado = temaService.salvar(tema);

        assertThat(resultado).isEqualTo(tema);
        verify(temaRepository).save(tema);
    }

    @Test
    void buscarPorId_deveRetornarTemaQuandoEncontrado() {
        UUID id = UUID.randomUUID();
        Tema tema = new Tema(id, "Ficção Científica");
        when(temaRepository.findById(id)).thenReturn(Optional.of(tema));

        Tema resultado = temaService.buscarPorId(id);

        assertThat(resultado.getId()).isEqualTo(id);
    }

    @Test
    void buscarPorId_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(temaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> temaService.buscarPorId(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tema não encontrado");
    }

    @Test
    void buscarPorNome_deveRetornarTemaQuandoEncontrado() {
        Tema tema = new Tema(UUID.randomUUID(), "Romance");
        when(temaRepository.findByNome("Romance")).thenReturn(Optional.of(tema));

        Tema resultado = temaService.buscarPorNome("Romance");

        assertThat(resultado.getNome()).isEqualTo("Romance");
    }

    @Test
    void buscarPorNome_deveLancarExcecaoQuandoNaoEncontrado() {
        when(temaRepository.findByNome("Inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> temaService.buscarPorNome("Inexistente"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tema não encontrado");
    }

    @Test
    void listarTodos_deveRetornarPaginaDeTemas() {
        List<Tema> temas = List.of(new Tema(UUID.randomUUID(), "Romance"), new Tema(UUID.randomUUID(), "Terror"));
        Pageable pageable = PageRequest.of(0, 10);
        when(temaRepository.findAll(pageable)).thenReturn(new PageImpl<>(temas));

        Page<Tema> resultado = temaService.listarTodos(pageable);

        assertThat(resultado.getContent()).hasSize(2);
    }

    @Test
    void atualizar_deveSalvarERetornarTemaAtualizado() {
        Tema tema = new Tema(UUID.randomUUID(), "Terror");
        when(temaRepository.save(tema)).thenReturn(tema);

        Tema resultado = temaService.atualizar(tema);

        assertThat(resultado).isEqualTo(tema);
        verify(temaRepository).save(tema);
    }

    @Test
    void deletar_deveChamarDeleteById() {
        UUID id = UUID.randomUUID();

        temaService.deletar(id);

        verify(temaRepository).deleteById(id);
    }
}
