package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.ConviteRepository;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.convite.Convite;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import github.jotagm.clube_livro.domain.clube.convite.ConviteStatus;
import github.jotagm.clube_livro.domain.exceptions.ConflitoException;
import github.jotagm.clube_livro.domain.exceptions.RegraDeNegocioException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConviteServiceTest {

    @Mock
    private ConviteRepository conviteRepository;

    @Mock
    private ClubeService clubeService;

    @Mock
    private UsuarioClubeService usuarioClubeService;

    @Mock
    private LeituraClubeService leituraClubeService;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private ConviteService conviteService;

    private Convite conviteExemplo(UUID id) {
        return new Convite(id, null, null, "convidado@email.com", ConviteStatus.PENDENTE,
                LocalDateTime.now().plusDays(7), LocalDateTime.now());
    }

    @Test
    void salvar_deveSalvarERetornarConvite() {
        Convite convite = conviteExemplo(UUID.randomUUID());
        when(conviteRepository.save(convite)).thenReturn(convite);

        Convite resultado = conviteService.salvar(convite);

        assertThat(resultado).isEqualTo(convite);
        verify(conviteRepository).save(convite);
    }

    @Test
    void buscarPorId_deveRetornarConviteQuandoEncontrado() {
        UUID id = UUID.randomUUID();
        Convite convite = conviteExemplo(id);
        when(conviteRepository.findById(id)).thenReturn(Optional.of(convite));

        Convite resultado = conviteService.buscarPorId(id);

        assertThat(resultado.getId()).isEqualTo(id);
    }

    @Test
    void buscarPorId_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(conviteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conviteService.buscarPorId(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Convite não encontrado");
    }

    @Test
    void buscarPorEmailDestinatario_deveRetornarConvite() {
        Convite convite = conviteExemplo(UUID.randomUUID());
        when(conviteRepository.findByEmailDestinatarioIgnoreCaseOrderByCreatedAtDesc("convidado@email.com"))
                .thenReturn(List.of(convite));

        Convite resultado = conviteService.buscarPorEmailDestinatario("convidado@email.com");

        assertThat(resultado.getEmailDestinatario()).isEqualTo("convidado@email.com");
    }

    @Test
    void listarPorClube_deveRetornarListaDeConvites() {
        UUID clubeId = UUID.randomUUID();
        List<Convite> convites = List.of(conviteExemplo(UUID.randomUUID()), conviteExemplo(UUID.randomUUID()));
        when(conviteRepository.findByClubeId(clubeId)).thenReturn(convites);

        List<Convite> resultado = conviteService.listarPorClube(clubeId);

        assertThat(resultado).hasSize(2);
    }

    @Test
    void atualizar_deveSalvarERetornarConviteAtualizado() {
        Convite convite = conviteExemplo(UUID.randomUUID());
        convite.aceitar();
        when(conviteRepository.save(convite)).thenReturn(convite);

        Convite resultado = conviteService.atualizar(convite);

        assertThat(resultado.getStatus()).isEqualTo(ConviteStatus.ACEITO);
        verify(conviteRepository).save(convite);
    }

    @Test
    void deletar_deveChamarDeleteById() {
        UUID id = UUID.randomUUID();

        conviteService.deletar(id);

        verify(conviteRepository).deleteById(id);
    }

    @Test
    void expirarVencidos_deveMarcarConvitesPendentesVencidosComoExpirado() {
        Convite vencido1 = conviteExemplo(UUID.randomUUID());
        Convite vencido2 = conviteExemplo(UUID.randomUUID());
        List<Convite> vencidos = List.of(vencido1, vencido2);

        when(conviteRepository.findByStatusAndExpiraEmBefore(eq(ConviteStatus.PENDENTE), any(LocalDateTime.class)))
                .thenReturn(vencidos);

        int quantidade = conviteService.expirarVencidos();

        assertThat(quantidade).isEqualTo(2);
        assertThat(vencido1.getStatus()).isEqualTo(ConviteStatus.EXPIRADO);
        assertThat(vencido2.getStatus()).isEqualTo(ConviteStatus.EXPIRADO);
        verify(conviteRepository).saveAll(vencidos);
    }

    @Test
    void expirarVencidos_deveRetornarZeroQuandoNaoHaConviteVencido() {
        when(conviteRepository.findByStatusAndExpiraEmBefore(eq(ConviteStatus.PENDENTE), any(LocalDateTime.class)))
                .thenReturn(List.of());

        int quantidade = conviteService.expirarVencidos();

        assertThat(quantidade).isZero();
        verify(conviteRepository).saveAll(List.of());
    }

    @Test
    void listarPendentesDoUsuario_deveIgnorarConvitesJaVencidos() {
        Convite valido = conviteExemplo(UUID.randomUUID());
        Convite vencido = new Convite(UUID.randomUUID(), null, null, "convidado@email.com", ConviteStatus.PENDENTE,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusDays(8));
        when(conviteRepository.findByEmailDestinatarioIgnoreCaseAndStatusOrderByCreatedAtDesc("convidado@email.com", ConviteStatus.PENDENTE))
                .thenReturn(List.of(valido, vencido));

        assertThat(conviteService.listarPendentesDoUsuario("convidado@email.com")).containsExactly(valido);
    }

    @Test
    void entrarPorCodigo_deveAdicionarComoMembroEIniciarRegistroNaLeituraAtiva() {
        Clube clube = Clube.builder().id(UUID.randomUUID()).nome("Clube").build();
        Usuario usuario = Usuario.builder().id(7).email("novo@email.com").build();
        when(clubeService.buscarPorCodigoConvite("abc123")).thenReturn(clube);
        when(usuarioClubeService.listarPorClube(clube.getId())).thenReturn(List.of());

        Clube resultado = conviteService.entrarPorCodigo("abc123", usuario);

        assertThat(resultado).isEqualTo(clube);
        verify(usuarioClubeService).adicionar(usuario, clube, ClubePapel.MEMBRO);
        verify(leituraClubeService).iniciarRegistroNaLeituraAtiva(clube.getId(), usuario);
    }

    @Test
    void entrarPorCodigo_deveSerIdempotenteParaQuemJaEMembro() {
        Clube clube = Clube.builder().id(UUID.randomUUID()).nome("Clube").build();
        Usuario usuario = Usuario.builder().id(7).email("membro@email.com").build();
        when(clubeService.buscarPorCodigoConvite("abc123")).thenReturn(clube);
        when(usuarioClubeService.listarPorClube(clube.getId()))
                .thenReturn(List.of(UsuarioClube.novo(usuario, clube, ClubePapel.MEMBRO)));

        conviteService.entrarPorCodigo("abc123", usuario);

        verify(usuarioClubeService, never()).adicionar(any(), any(), any());
    }

    @Test
    void pertenceA_deveIgnorarMaiusculas() {
        Convite convite = conviteExemplo(UUID.randomUUID());
        assertThat(convite.pertenceA("Convidado@Email.com")).isTrue();
    }

    // ---- respostas: um convite só é respondido uma vez ----

    private Convite conviteDoClube(Clube clube, ConviteStatus status) {
        return new Convite(UUID.randomUUID(), clube, null, "convidado@email.com", status,
                LocalDateTime.now().plusDays(7), LocalDateTime.now());
    }

    @Test
    void aceitar_deveAdicionarComoMembro() {
        Clube clube = Clube.builder().id(UUID.randomUUID()).nome("Clube").build();
        Usuario usuario = Usuario.builder().id(7).email("convidado@email.com").build();
        Convite convite = conviteDoClube(clube, ConviteStatus.PENDENTE);
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));
        when(usuarioService.buscarPorEmail("convidado@email.com")).thenReturn(usuario);
        when(usuarioClubeService.listarPorClube(clube.getId())).thenReturn(List.of());
        when(conviteRepository.save(convite)).thenReturn(convite);

        Convite resultado = conviteService.aceitarConvite(convite.getId(), "convidado@email.com");

        assertThat(resultado.getStatus()).isEqualTo(ConviteStatus.ACEITO);
        verify(usuarioClubeService).adicionar(usuario, clube, ClubePapel.MEMBRO);
    }

    @Test
    void aceitar_quemJaEntrouPeloLinkNaoGanhaSegundaLinhaDeMembro() {
        Clube clube = Clube.builder().id(UUID.randomUUID()).nome("Clube").build();
        Usuario usuario = Usuario.builder().id(7).email("convidado@email.com").build();
        Convite convite = conviteDoClube(clube, ConviteStatus.PENDENTE);
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));
        when(usuarioService.buscarPorEmail("convidado@email.com")).thenReturn(usuario);
        when(usuarioClubeService.listarPorClube(clube.getId()))
                .thenReturn(List.of(UsuarioClube.novo(usuario, clube, ClubePapel.MEMBRO)));
        when(conviteRepository.save(convite)).thenReturn(convite);

        Convite resultado = conviteService.aceitarConvite(convite.getId(), "convidado@email.com");

        assertThat(resultado.getStatus()).isEqualTo(ConviteStatus.ACEITO);
        verify(usuarioClubeService, never()).adicionar(any(), any(), any());
    }

    @Test
    void aceitar_conviteJaAceitoDaConflitoENaoMexeNosMembros() {
        Clube clube = Clube.builder().id(UUID.randomUUID()).nome("Clube").build();
        Convite convite = conviteDoClube(clube, ConviteStatus.ACEITO);
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));

        assertThatThrownBy(() -> conviteService.aceitarConvite(convite.getId(), "convidado@email.com"))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Este convite já foi aceito");
        verify(usuarioClubeService, never()).adicionar(any(), any(), any());
        verify(conviteRepository, never()).save(any());
    }

    @Test
    void aceitar_conviteRecusadoDaConflito() {
        Convite convite = conviteDoClube(Clube.builder().id(UUID.randomUUID()).build(), ConviteStatus.RECUSADO);
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));

        assertThatThrownBy(() -> conviteService.aceitarConvite(convite.getId(), "convidado@email.com"))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void recusar_conviteJaAceitoDaConflitoEMantemAceito() {
        Convite convite = conviteDoClube(Clube.builder().id(UUID.randomUUID()).build(), ConviteStatus.ACEITO);
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));

        assertThatThrownBy(() -> conviteService.rejeitarConvite(convite.getId(), "convidado@email.com"))
                .isInstanceOf(ConflitoException.class);
        assertThat(convite.getStatus()).isEqualTo(ConviteStatus.ACEITO);
    }

    // ---- criação ----

    @Test
    void criar_deveNormalizarEmailESalvarPendente() {
        Clube clube = Clube.builder().id(UUID.randomUUID()).nome("Clube").build();
        when(usuarioClubeService.listarPorClube(clube.getId())).thenReturn(List.of());
        when(conviteRepository.existsByClubeIdAndEmailDestinatarioIgnoreCaseAndStatusAndExpiraEmAfter(
                eq(clube.getId()), eq("novo@email.com"), eq(ConviteStatus.PENDENTE), any(LocalDateTime.class)))
                .thenReturn(false);
        when(conviteRepository.save(any(Convite.class))).thenAnswer(inv -> inv.getArgument(0));

        Convite convite = conviteService.criar(clube, null, "  Novo@Email.com ", LocalDateTime.now().plusDays(7));

        assertThat(convite.getEmailDestinatario()).isEqualTo("novo@email.com");
        assertThat(convite.getStatus()).isEqualTo(ConviteStatus.PENDENTE);
    }

    @Test
    void criar_expiracaoNoPassadoDaRegraDeNegocio() {
        Clube clube = Clube.builder().id(UUID.randomUUID()).build();

        assertThatThrownBy(() -> conviteService.criar(clube, null, "a@email.com", LocalDateTime.now().minusMinutes(1)))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(conviteRepository, never()).save(any());
    }

    @Test
    void criar_expiracaoAlemDeTrintaDiasDaRegraDeNegocio() {
        Clube clube = Clube.builder().id(UUID.randomUUID()).build();

        assertThatThrownBy(() -> conviteService.criar(clube, null, "a@email.com", LocalDateTime.now().plusDays(31)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("O convite pode valer no máximo 30 dias");
    }

    @Test
    void criar_paraQuemJaEMembroDaConflito() {
        Clube clube = Clube.builder().id(UUID.randomUUID()).build();
        Usuario membro = Usuario.builder().id(3).email("membro@email.com").build();
        when(usuarioClubeService.listarPorClube(clube.getId()))
                .thenReturn(List.of(UsuarioClube.novo(membro, clube, ClubePapel.MEMBRO)));

        assertThatThrownBy(() -> conviteService.criar(clube, null, "MEMBRO@email.com", LocalDateTime.now().plusDays(7)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Essa pessoa já é membro do clube");
    }

    @Test
    void criar_segundoConvitePendenteParaOMesmoEmailDaConflito() {
        Clube clube = Clube.builder().id(UUID.randomUUID()).build();
        when(usuarioClubeService.listarPorClube(clube.getId())).thenReturn(List.of());
        when(conviteRepository.existsByClubeIdAndEmailDestinatarioIgnoreCaseAndStatusAndExpiraEmAfter(
                eq(clube.getId()), eq("a@email.com"), eq(ConviteStatus.PENDENTE), any(LocalDateTime.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> conviteService.criar(clube, null, "a@email.com", LocalDateTime.now().plusDays(7)))
                .isInstanceOf(ConflitoException.class);
        verify(conviteRepository, never()).save(any());
    }
}
