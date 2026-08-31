package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.VotacaoRequest;
import github.jotagm.clube_livro.adapter.out.persistence.VotacaoRepository;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus;
import github.jotagm.clube_livro.domain.exceptions.IntervaloInvalidoException;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VotacaoServiceTest {

    @Mock
    private VotacaoRepository votacaoRepository;

    @Mock
    private ClubeService clubeService;

    @InjectMocks
    private VotacaoService votacaoService;

    private Votacao votacaoExemplo(UUID id) {
        return new Votacao(id, null, VotacaoStatus.ABERTA, LocalDateTime.now(), null);
    }

    @Test
    void salvar_deveSalvarERetornarVotacao() {
        Votacao votacao = votacaoExemplo(UUID.randomUUID());
        when(votacaoRepository.save(votacao)).thenReturn(votacao);

        Votacao resultado = votacaoService.salvar(votacao);

        assertThat(resultado).isEqualTo(votacao);
        verify(votacaoRepository).save(votacao);
    }

    @Test
    void buscarPorId_deveRetornarVotacaoQuandoEncontrada() {
        UUID id = UUID.randomUUID();
        Votacao votacao = votacaoExemplo(id);
        when(votacaoRepository.findById(id)).thenReturn(Optional.of(votacao));

        Votacao resultado = votacaoService.buscarPorId(id);

        assertThat(resultado.getId()).isEqualTo(id);
    }

    @Test
    void buscarPorId_deveLancarExcecaoQuandoNaoEncontrada() {
        UUID id = UUID.randomUUID();
        when(votacaoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> votacaoService.buscarPorId(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Votação não encontrada");
    }

    @Test
    void listarPorClube_deveRetornarListaDeVotacoes() {
        UUID clubeId = UUID.randomUUID();
        List<Votacao> votacoes = List.of(votacaoExemplo(UUID.randomUUID()), votacaoExemplo(UUID.randomUUID()));
        when(votacaoRepository.findByClubeId(clubeId)).thenReturn(votacoes);

        List<Votacao> resultado = votacaoService.listarPorClube(clubeId);

        assertThat(resultado).hasSize(2);
    }

    @Test
    void atualizar_deveSalvarERetornarVotacaoAtualizada() {
        Votacao votacao = votacaoExemplo(UUID.randomUUID());
        votacao.encerrar();
        when(votacaoRepository.save(votacao)).thenReturn(votacao);

        Votacao resultado = votacaoService.atualizar(votacao);

        assertThat(resultado.getStatus()).isEqualTo(VotacaoStatus.ENCERRADA);
        verify(votacaoRepository).save(votacao);
    }

    @Test
    void deletar_deveChamarDeleteById() {
        UUID id = UUID.randomUUID();

        votacaoService.deletar(id);

        verify(votacaoRepository).deleteById(id);
    }

    private VotacaoRequest requestCom(LocalDateTime abertura, LocalDateTime encerramento) {
        return new VotacaoRequest(UUID.randomUUID(), abertura, encerramento);
    }

    @Test
    void criar_deveRecusarEncerramentoAntesDaAbertura() {
        LocalDateTime abertura = LocalDateTime.now();

        assertThatThrownBy(() -> votacaoService.criar(requestCom(abertura, abertura.minusDays(1))))
                .isInstanceOf(IntervaloInvalidoException.class);

        verify(votacaoRepository, never()).save(any());
    }

    @Test
    void criar_deveRecusarAberturaEEncerramentoIguais() {
        LocalDateTime instante = LocalDateTime.now();

        assertThatThrownBy(() -> votacaoService.criar(requestCom(instante, instante)))
                .isInstanceOf(IntervaloInvalidoException.class);
    }

    @Test
    void atualizar_deveRecusarIntervaloInvalido() {
        LocalDateTime abertura = LocalDateTime.now();

        assertThatThrownBy(() -> votacaoService.atualizar(UUID.randomUUID(),
                requestCom(abertura, abertura.minusHours(1))))
                .isInstanceOf(IntervaloInvalidoException.class);

        // A validação vem antes da busca: intervalo inválido não deve nem consultar o banco.
        verify(votacaoRepository, never()).findById(any());
        verify(votacaoRepository, never()).save(any());
    }

    @Test
    void criar_deveAceitarIntervaloValido() {
        LocalDateTime abertura = LocalDateTime.now();
        when(votacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> votacaoService.criar(requestCom(abertura, abertura.plusDays(7))))
                .doesNotThrowAnyException();

        verify(votacaoRepository).save(any());
    }
}
