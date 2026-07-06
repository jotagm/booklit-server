package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus;
import github.jotagm.clube_livro.domain.clube.votacao.Voto;
import github.jotagm.clube_livro.domain.exceptions.VotacaoSemVotosException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VotacaoEnceramentoServiceTest {

    @Mock
    private VotacaoService votacaoService;

    @Mock
    private VotoService votoService;

    @Mock
    private LeituraClubeService leituraClubeService;

    @InjectMocks
    private VotacaoEnceramentoService votacaoEnceramentoService;

    private OpcaoVoto opcaoExemplo(String titulo) {
        OpcaoVoto opcao = new OpcaoVoto();
        opcao.setId(UUID.randomUUID());
        opcao.setLivroTitulo(titulo);
        opcao.setLivroGoogleId("google-" + titulo);
        opcao.setLivroCapaUrl("capa-" + titulo);
        return opcao;
    }

    private Voto votoExemplo(OpcaoVoto opcao, int peso) {
        return Voto.builder().opcaoVoto(opcao).peso(peso).build();
    }

    private Votacao votacaoExemplo(UUID id) {
        Votacao votacao = new Votacao();
        votacao.setId(id);
        votacao.setClube(new Clube());
        votacao.setStatus(VotacaoStatus.ABERTA);
        return votacao;
    }

    @Test
    void finalizarVotacao_deveElegerOpcaoComMaiorPeso() {
        UUID votacaoId = UUID.randomUUID();
        Votacao votacao = votacaoExemplo(votacaoId);
        OpcaoVoto vencedora = opcaoExemplo("Vencedora");
        OpcaoVoto perdedora = opcaoExemplo("Perdedora");

        when(votacaoService.buscarPorId(votacaoId)).thenReturn(votacao);
        when(votoService.listarPorVotacao(votacaoId)).thenReturn(List.of(
                votoExemplo(vencedora, 2),
                votoExemplo(vencedora, 1),
                votoExemplo(perdedora, 1)
        ));
        when(leituraClubeService.salvar(any(LeituraClube.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeituraClube resultado = votacaoEnceramentoService.finalizarVotacao(votacaoId);

        assertThat(resultado.getLivroTitulo()).isEqualTo("Vencedora");
        assertThat(votacao.getStatus()).isEqualTo(VotacaoStatus.ENCERRADA);
        verify(votacaoService).atualizar(votacao);
    }

    @Test
    void finalizarVotacao_deveLancarExcecaoQuandoNaoHaVotos() {
        UUID votacaoId = UUID.randomUUID();
        when(votacaoService.buscarPorId(votacaoId)).thenReturn(votacaoExemplo(votacaoId));
        when(votoService.listarPorVotacao(votacaoId)).thenReturn(List.of());

        assertThatThrownBy(() -> votacaoEnceramentoService.finalizarVotacao(votacaoId))
                .isInstanceOf(VotacaoSemVotosException.class);
    }

    @Test
    void finalizarVotacao_emEmpateDeveVencerOpcaoDoVotoDoLider() {
        UUID votacaoId = UUID.randomUUID();
        Votacao votacao = votacaoExemplo(votacaoId);
        OpcaoVoto opcaoDoLider = opcaoExemplo("Escolha do líder");
        OpcaoVoto opcaoDosMembros = opcaoExemplo("Escolha dos membros");

        when(votacaoService.buscarPorId(votacaoId)).thenReturn(votacao);
        when(votoService.listarPorVotacao(votacaoId)).thenReturn(List.of(
                votoExemplo(opcaoDoLider, 2),
                votoExemplo(opcaoDosMembros, 1),
                votoExemplo(opcaoDosMembros, 1)
        ));
        when(leituraClubeService.salvar(any(LeituraClube.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeituraClube resultado = votacaoEnceramentoService.finalizarVotacao(votacaoId);

        assertThat(resultado.getLivroTitulo()).isEqualTo("Escolha do líder");
    }

    @Test
    void finalizarVotacao_emEmpateSemVotoDeLiderDeveSerDeterministico() {
        UUID votacaoId = UUID.randomUUID();
        Votacao votacao = votacaoExemplo(votacaoId);
        OpcaoVoto opcaoA = opcaoExemplo("Opção A");
        OpcaoVoto opcaoB = opcaoExemplo("Opção B");

        when(votacaoService.buscarPorId(votacaoId)).thenReturn(votacao);
        when(votoService.listarPorVotacao(votacaoId)).thenReturn(List.of(
                votoExemplo(opcaoA, 1),
                votoExemplo(opcaoB, 1)
        ));
        when(leituraClubeService.salvar(any(LeituraClube.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OpcaoVoto esperada = List.of(opcaoA, opcaoB).stream()
                .min(Comparator.comparing(o -> o.getId().toString()))
                .orElseThrow();

        LeituraClube resultado = votacaoEnceramentoService.finalizarVotacao(votacaoId);

        assertThat(resultado.getLivroTitulo()).isEqualTo(esperada.getLivroTitulo());
    }

    @Test
    void encerrarVotacoesVencidas_deveEncerrarTodasAsVotacoesAbertasVencidas() {
        Votacao votacao1 = votacaoExemplo(UUID.randomUUID());
        Votacao votacao2 = votacaoExemplo(UUID.randomUUID());
        OpcaoVoto opcao1 = opcaoExemplo("Livro 1");
        OpcaoVoto opcao2 = opcaoExemplo("Livro 2");

        when(votacaoService.listarAbertasVencidas(any(LocalDateTime.class))).thenReturn(List.of(votacao1, votacao2));
        when(votacaoService.buscarPorId(votacao1.getId())).thenReturn(votacao1);
        when(votacaoService.buscarPorId(votacao2.getId())).thenReturn(votacao2);
        when(votoService.listarPorVotacao(votacao1.getId())).thenReturn(List.of(votoExemplo(opcao1, 1)));
        when(votoService.listarPorVotacao(votacao2.getId())).thenReturn(List.of(votoExemplo(opcao2, 1)));
        when(leituraClubeService.salvar(any(LeituraClube.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<LeituraClube> resultado = votacaoEnceramentoService.encerrarVotacoesVencidas();

        assertThat(resultado).hasSize(2);
        assertThat(votacao1.getStatus()).isEqualTo(VotacaoStatus.ENCERRADA);
        assertThat(votacao2.getStatus()).isEqualTo(VotacaoStatus.ENCERRADA);
    }

    @Test
    void encerrarVotacoesVencidas_deveContinuarQuandoUmaVotacaoFalha() {
        Votacao votacaoSemVoto = votacaoExemplo(UUID.randomUUID());
        Votacao votacaoComVoto = votacaoExemplo(UUID.randomUUID());
        OpcaoVoto opcao = opcaoExemplo("Livro válido");

        when(votacaoService.listarAbertasVencidas(any(LocalDateTime.class))).thenReturn(List.of(votacaoSemVoto, votacaoComVoto));
        when(votacaoService.buscarPorId(votacaoSemVoto.getId())).thenReturn(votacaoSemVoto);
        when(votacaoService.buscarPorId(votacaoComVoto.getId())).thenReturn(votacaoComVoto);
        when(votoService.listarPorVotacao(votacaoSemVoto.getId())).thenReturn(List.of());
        when(votoService.listarPorVotacao(votacaoComVoto.getId())).thenReturn(List.of(votoExemplo(opcao, 1)));
        when(leituraClubeService.salvar(any(LeituraClube.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<LeituraClube> resultado = votacaoEnceramentoService.encerrarVotacoesVencidas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getLivroTitulo()).isEqualTo("Livro válido");
        assertThat(votacaoSemVoto.getStatus()).isEqualTo(VotacaoStatus.ABERTA);
        assertThat(votacaoComVoto.getStatus()).isEqualTo(VotacaoStatus.ENCERRADA);
    }
}
