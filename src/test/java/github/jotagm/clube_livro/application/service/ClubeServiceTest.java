package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.ClubeRequest;
import github.jotagm.clube_livro.adapter.out.persistence.ClubeRepository;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.ClubeStatus;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubeServiceTest {

    @Mock
    private ClubeRepository clubeRepository;

    @Mock
    private TemaService temaService;

    @Mock
    private UsuarioClubeService usuarioClubeService;

    @InjectMocks
    private ClubeService clubeService;

    private Clube clubeExemplo() {
        return new Clube(UUID.randomUUID(), "Clube do Livro", "Descrição", false, ClubeStatus.ATIVO, null, List.of());
    }

    @Test
    void criar_deveSalvarClubeEAdicionarCriadorComoLider() {
        ClubeRequest request = new ClubeRequest("Clube do Livro", "Descrição", false, null);
        Usuario criador = new Usuario(UUID.randomUUID(), "João", "joao@email.com", "hash", null);
        Clube clubeSalvo = clubeExemplo();
        when(clubeRepository.save(any(Clube.class))).thenReturn(clubeSalvo);

        Clube resultado = clubeService.criar(request, criador);

        assertThat(resultado).isEqualTo(clubeSalvo);
        verify(usuarioClubeService).adicionarLider(criador, clubeSalvo);
    }

    @Test
    void salvar_deveSalvarERetornarClube() {
        Clube clube = clubeExemplo();
        when(clubeRepository.save(clube)).thenReturn(clube);

        Clube resultado = clubeService.salvar(clube);

        assertThat(resultado).isEqualTo(clube);
        verify(clubeRepository).save(clube);
    }

    @Test
    void buscarPorId_deveRetornarClubeQuandoEncontrado() {
        Clube clube = clubeExemplo();
        when(clubeRepository.findById(clube.getId())).thenReturn(Optional.of(clube));

        Clube resultado = clubeService.buscarPorId(clube.getId());

        assertThat(resultado.getId()).isEqualTo(clube.getId());
    }

    @Test
    void buscarPorId_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(clubeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubeService.buscarPorId(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Clube não encontrado");
    }

    @Test
    void buscarPorNome_deveRetornarClubeQuandoEncontrado() {
        Clube clube = clubeExemplo();
        when(clubeRepository.findByNome("Clube do Livro")).thenReturn(Optional.of(clube));

        Clube resultado = clubeService.buscarPorNome("Clube do Livro");

        assertThat(resultado.getNome()).isEqualTo("Clube do Livro");
    }

    @Test
    void buscarPorNome_deveLancarExcecaoQuandoNaoEncontrado() {
        when(clubeRepository.findByNome("Inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubeService.buscarPorNome("Inexistente"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Clube não encontrado");
    }

    @Test
    void listarTodos_deveRetornarListaDeClubes() {
        List<Clube> clubes = List.of(clubeExemplo(), clubeExemplo());
        when(clubeRepository.findAll()).thenReturn(clubes);

        List<Clube> resultado = clubeService.listarTodos();

        assertThat(resultado).hasSize(2);
    }

    @Test
    void atualizar_deveSalvarERetornarClubeAtualizado() {
        Clube clube = clubeExemplo();
        when(clubeRepository.save(clube)).thenReturn(clube);

        Clube resultado = clubeService.atualizar(clube);

        assertThat(resultado).isEqualTo(clube);
        verify(clubeRepository).save(clube);
    }

    @Test
    void deletar_deveChamarDeleteById() {
        UUID id = UUID.randomUUID();

        clubeService.deletar(id);

        verify(clubeRepository).deleteById(id);
    }
}
