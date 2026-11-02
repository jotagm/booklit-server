package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.application.port.out.EnviadorDeLembreteDeVotacao;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.Voto;
import github.jotagm.clube_livro.domain.exceptions.RegraDeNegocioException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import github.jotagm.clube_livro.support.RelogioDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LembreteDeVotacaoServiceTest {

    @Mock private VotacaoService votacaoService;
    @Mock private VotoService votoService;
    @Mock private UsuarioClubeService usuarioClubeService;
    @Mock private EnviadorDeLembreteDeVotacao enviador;
    @Spy private Clock relogio = new RelogioDeTeste();

    @InjectMocks
    private LembreteDeVotacaoService service;

    private final Usuario lider = usuario(1, "Mariana");
    private final Usuario ana = usuario(2, "Ana");
    private final Usuario bruno = usuario(3, "Bruno");
    private final Usuario carla = usuario(4, "Carla");
    private Clube clube;
    private Votacao votacao;

    private static Usuario usuario(int id, String nome) {
        return Usuario.builder().id(id).nome(nome).email(nome.toLowerCase() + "@teste.dev").build();
    }

    @BeforeEach
    void montar() {
        clube = Clube.builder().id(UUID.randomUUID()).nome("Leitoras de Domingo").build();
        votacao = Votacao.builder().id(UUID.randomUUID()).clube(clube)
                .status(github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus.ABERTA)
                .dataAbertura(LocalDateTime.now().minusDays(1)).dataEncerramento(LocalDateTime.now().plusDays(2))
                .build();
        when(votacaoService.buscarPorId(votacao.getId())).thenReturn(votacao);
        when(usuarioClubeService.listarPorClube(clube.getId())).thenReturn(List.of(
                membro(lider, ClubePapel.LIDER), membro(ana, ClubePapel.MEMBRO),
                membro(bruno, ClubePapel.MEMBRO), membro(carla, ClubePapel.MEMBRO)));
    }

    private UsuarioClube membro(Usuario u, ClubePapel papel) {
        return UsuarioClube.builder().usuario(u).clube(clube).papel(papel).build();
    }

    private Voto votoDe(Usuario u) {
        return Voto.registrar(votacao, new OpcaoVoto(), u, ClubePapel.MEMBRO);
    }

    @Test
    void lembraSoQuemNaoVotouSemIncluirOProprioLider() {
        // O líder ainda não votou, mas não recebe e-mail de si mesmo.
        when(votoService.listarPorVotacao(votacao.getId())).thenReturn(List.of(votoDe(ana)));

        LembreteDeVotacaoService.Resultado r = service.lembrarQuemNaoVotou(votacao.getId(), lider);

        ArgumentCaptor<EnviadorDeLembreteDeVotacao.Mensagem> mensagens =
                ArgumentCaptor.forClass(EnviadorDeLembreteDeVotacao.Mensagem.class);
        verify(enviador, times(2)).enviar(mensagens.capture());
        assertThat(mensagens.getAllValues()).extracting(EnviadorDeLembreteDeVotacao.Mensagem::destinatario)
                .containsExactlyInAnyOrder("bruno@teste.dev", "carla@teste.dev");
        assertThat(mensagens.getAllValues()).allSatisfy(m -> {
            assertThat(m.votacaoId()).isEqualTo(votacao.getId());
            assertThat(m.clube()).isEqualTo("Leitoras de Domingo");
            assertThat(m.encerraEm()).isEqualTo(votacao.getDataEncerramento());
        });
        // Chaves distintas por membro: o Client_GO não descarta o segundo e-mail como repetido.
        assertThat(mensagens.getAllValues()).extracting(EnviadorDeLembreteDeVotacao.Mensagem::chave)
                .doesNotHaveDuplicates();
        assertThat(r.notificados()).isEqualTo(2);
        assertThat(votacao.getUltimoLembreteEm()).isEqualTo(r.lembradoEm());
        verify(votacaoService).atualizar(votacao);
    }

    @Test
    void todosVotaramNaoRegistraLembrete() {
        when(votoService.listarPorVotacao(votacao.getId()))
                .thenReturn(List.of(votoDe(ana), votoDe(bruno), votoDe(carla)));

        assertThatThrownBy(() -> service.lembrarQuemNaoVotou(votacao.getId(), lider))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já votaram");
        assertThat(votacao.getUltimoLembreteEm()).isNull();
        verify(enviador, never()).enviar(any());
    }

    @Test
    void segundoLembreteNoMesmoDiaNaoEnviaNada() {
        when(votoService.listarPorVotacao(votacao.getId())).thenReturn(List.of());
        service.lembrarQuemNaoVotou(votacao.getId(), lider);

        assertThatThrownBy(() -> service.lembrarQuemNaoVotou(votacao.getId(), lider))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(enviador, times(3)).enviar(any());
    }
}
