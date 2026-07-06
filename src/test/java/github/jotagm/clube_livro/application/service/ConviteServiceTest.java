package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.ConviteRepository;
import github.jotagm.clube_livro.domain.clube.convite.Convite;
import github.jotagm.clube_livro.domain.clube.convite.ConviteStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConviteServiceTest {

    @Mock
    private ConviteRepository conviteRepository;

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
        when(conviteRepository.findByEmailDestinatario("convidado@email.com")).thenReturn(convite);

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
        convite.setStatus(ConviteStatus.ACEITO);
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
}
