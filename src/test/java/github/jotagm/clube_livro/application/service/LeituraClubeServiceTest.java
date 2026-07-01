package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.LeituraClubeRequest;
import github.jotagm.clube_livro.adapter.out.persistence.LeituraClubeRepository;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.leitura.TipoMeta;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeituraClubeServiceTest {

    @Mock
    private LeituraClubeRepository leituraClubeRepository;

    @Mock
    private ClubeService clubeService;

    @InjectMocks
    private LeituraClubeService leituraClubeService;

    private LeituraClube leituraExemplo(UUID id) {
        return new LeituraClube(id, null, "google-123", "Dom Casmurro", "url-capa",
                TipoMeta.PAGINA, 300, LocalDateTime.now(), LocalDateTime.now().plusDays(30));
    }

    @Test
    void salvar_deveSalvarERetornarLeitura() {
        LeituraClube leitura = leituraExemplo(UUID.randomUUID());
        when(leituraClubeRepository.save(leitura)).thenReturn(leitura);

        LeituraClube resultado = leituraClubeService.salvar(leitura);

        assertThat(resultado).isEqualTo(leitura);
        verify(leituraClubeRepository).save(leitura);
    }

    @Test
    void buscarPorId_deveRetornarLeituraQuandoEncontrada() {
        UUID id = UUID.randomUUID();
        LeituraClube leitura = leituraExemplo(id);
        when(leituraClubeRepository.findById(id)).thenReturn(Optional.of(leitura));

        LeituraClube resultado = leituraClubeService.buscarPorId(id);

        assertThat(resultado.getId()).isEqualTo(id);
        assertThat(resultado.getLivroTitulo()).isEqualTo("Dom Casmurro");
    }

    @Test
    void buscarPorId_deveLancarExcecaoQuandoNaoEncontrada() {
        UUID id = UUID.randomUUID();
        when(leituraClubeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leituraClubeService.buscarPorId(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Leitura não encontrada");
    }

    @Test
    void listarPorClube_deveRetornarLeiturasOrdenadasPorDataInicio() {
        UUID clubeId = UUID.randomUUID();
        List<LeituraClube> leituras = List.of(leituraExemplo(UUID.randomUUID()), leituraExemplo(UUID.randomUUID()));
        when(leituraClubeRepository.findByClubeIdOrderByDataInicioDesc(clubeId)).thenReturn(leituras);

        List<LeituraClube> resultado = leituraClubeService.listarPorClube(clubeId);

        assertThat(resultado).hasSize(2);
        verify(leituraClubeRepository).findByClubeIdOrderByDataInicioDesc(clubeId);
    }

    @Test
    void atualizar_deveSalvarERetornarLeituraAtualizada() {
        UUID id = UUID.randomUUID();
        LeituraClube leitura = leituraExemplo(id);
        LeituraClubeRequest request = new LeituraClubeRequest(
                UUID.randomUUID(), "google-456", "Memórias Póstumas", "url-capa",
                TipoMeta.PAGINA, 300, LocalDateTime.now(), LocalDateTime.now().plusDays(30));

        when(leituraClubeRepository.findById(id)).thenReturn(Optional.of(leitura));
        when(leituraClubeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LeituraClube resultado = leituraClubeService.atualizar(id, request);

        assertThat(resultado.getLivroTitulo()).isEqualTo("Memórias Póstumas");
        verify(leituraClubeRepository).save(leitura);
    }

    @Test
    void deletar_deveChamarDeleteById() {
        UUID id = UUID.randomUUID();

        leituraClubeService.deletar(id);

        verify(leituraClubeRepository).deleteById(id);
    }
}
