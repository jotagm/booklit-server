package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.UsuarioClubeRepository;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.ClubeStatus;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioClubeServiceTest {

    @Mock
    private UsuarioClubeRepository usuarioClubeRepository;

    @InjectMocks
    private UsuarioClubeService usuarioClubeService;

    private UsuarioClube membroExemplo(UUID id) {
        return new UsuarioClube(id, null, null, ClubePapel.MEMBRO, LocalDateTime.now());
    }

    @Test
    void adicionarLider_deveCriarVinculoComPapelLider() {
        Usuario usuario = new Usuario(UUID.randomUUID(), "João", "joao@email.com", "hash", null);
        Clube clube = new Clube(UUID.randomUUID(), "Clube do Livro", "Descrição", false, ClubeStatus.ATIVO, null, List.of());
        when(usuarioClubeRepository.save(any(UsuarioClube.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioClube resultado = usuarioClubeService.adicionarLider(usuario, clube);

        assertThat(resultado.getPapel()).isEqualTo(ClubePapel.LIDER);
        assertThat(resultado.getUsuario()).isEqualTo(usuario);
        assertThat(resultado.getClube()).isEqualTo(clube);
    }

    @Test
    void adicionar_deveCriarVinculoComPapelInformado() {
        Usuario usuario = new Usuario(UUID.randomUUID(), "João", "joao@email.com", "hash", null);
        Clube clube = new Clube(UUID.randomUUID(), "Clube do Livro", "Descrição", false, ClubeStatus.ATIVO, null, List.of());
        when(usuarioClubeRepository.save(any(UsuarioClube.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioClube resultado = usuarioClubeService.adicionar(usuario, clube, ClubePapel.MEMBRO);

        assertThat(resultado.getPapel()).isEqualTo(ClubePapel.MEMBRO);
        assertThat(resultado.getUsuario()).isEqualTo(usuario);
        assertThat(resultado.getClube()).isEqualTo(clube);
    }

    @Test
    void buscarPorId_deveRetornarMembroQuandoEncontrado() {
        UUID id = UUID.randomUUID();
        UsuarioClube membro = membroExemplo(id);
        when(usuarioClubeRepository.findById(id)).thenReturn(Optional.of(membro));

        UsuarioClube resultado = usuarioClubeService.buscarPorId(id);

        assertThat(resultado.getId()).isEqualTo(id);
    }

    @Test
    void buscarPorId_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(usuarioClubeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioClubeService.buscarPorId(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Membro não encontrado");
    }

    @Test
    void listarPorUsuario_deveRetornarListaDeMembros() {
        UUID usuarioId = UUID.randomUUID();
        List<UsuarioClube> membros = List.of(membroExemplo(UUID.randomUUID()), membroExemplo(UUID.randomUUID()));
        when(usuarioClubeRepository.findByUsuarioId(usuarioId)).thenReturn(membros);

        List<UsuarioClube> resultado = usuarioClubeService.listarPorUsuario(usuarioId);

        assertThat(resultado).hasSize(2);
    }

    @Test
    void listarPorClube_deveRetornarListaDeMembros() {
        UUID clubeId = UUID.randomUUID();
        List<UsuarioClube> membros = List.of(membroExemplo(UUID.randomUUID()));
        when(usuarioClubeRepository.findByClubeId(clubeId)).thenReturn(membros);

        List<UsuarioClube> resultado = usuarioClubeService.listarPorClube(clubeId);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void buscarPorUsuarioEClube_deveRetornarMembroQuandoEncontrado() {
        UUID usuarioId = UUID.randomUUID();
        UUID clubeId = UUID.randomUUID();
        UsuarioClube membro = membroExemplo(UUID.randomUUID());
        when(usuarioClubeRepository.findByUsuarioIdAndClubeId(usuarioId, clubeId)).thenReturn(Optional.of(membro));

        UsuarioClube resultado = usuarioClubeService.buscarPorUsuarioEClube(usuarioId, clubeId);

        assertThat(resultado).isEqualTo(membro);
    }

    @Test
    void buscarPorUsuarioEClube_deveLancarExcecaoQuandoNaoEMembro() {
        UUID usuarioId = UUID.randomUUID();
        UUID clubeId = UUID.randomUUID();
        when(usuarioClubeRepository.findByUsuarioIdAndClubeId(usuarioId, clubeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioClubeService.buscarPorUsuarioEClube(usuarioId, clubeId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não é membro deste clube");
    }

    @Test
    void atualizar_deveSalvarERetornarMembroAtualizado() {
        UsuarioClube membro = membroExemplo(UUID.randomUUID());
        membro.setPapel(ClubePapel.LIDER);
        when(usuarioClubeRepository.save(membro)).thenReturn(membro);

        UsuarioClube resultado = usuarioClubeService.atualizar(membro);

        assertThat(resultado.getPapel()).isEqualTo(ClubePapel.LIDER);
        verify(usuarioClubeRepository).save(membro);
    }

    @Test
    void deletar_deveChamarDeleteById() {
        UUID id = UUID.randomUUID();

        usuarioClubeService.deletar(id);

        verify(usuarioClubeRepository).deleteById(id);
    }
}
