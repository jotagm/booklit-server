package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.LeituraPessoalRepository;
import github.jotagm.clube_livro.adapter.out.persistence.MetaLeituraRepository;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.leitura.TipoMeta;
import github.jotagm.clube_livro.domain.estante.LeituraPessoal;
import github.jotagm.clube_livro.domain.estante.StatusLeituraPessoal;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstanteServiceTest {

    @Mock private LeituraPessoalRepository leituraPessoalRepository;
    @Mock private MetaLeituraRepository metaLeituraRepository;
    @InjectMocks private EstanteService estanteService;

    private final Usuario usuario = Usuario.builder().id(1).email("leitor@teste.dev").build();
    private final Clube clube = Clube.builder().id(UUID.randomUUID()).build();

    private LeituraClube leituraDoClube(TipoMeta tipo, int meta) {
        return LeituraClube.iniciar(clube, "g-duna", "Duna", "capa", tipo, meta,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
    }

    @Test
    void primeiroAvancoNoClubeColocaOLivroComoLendoNaEstante() {
        when(leituraPessoalRepository.findByUsuarioIdAndLivroGoogleId(1, "g-duna")).thenReturn(Optional.empty());

        estanteService.sincronizarProgressoDoClube(usuario, leituraDoClube(TipoMeta.PAGINA, 400), 40);

        ArgumentCaptor<LeituraPessoal> salvo = ArgumentCaptor.forClass(LeituraPessoal.class);
        verify(leituraPessoalRepository).save(salvo.capture());
        assertThat(salvo.getValue().getStatus()).isEqualTo(StatusLeituraPessoal.LENDO);
        assertThat(salvo.getValue().getPaginaAtual()).isEqualTo(40);
        assertThat(salvo.getValue().getTotalPaginas()).isEqualTo(400);
        assertThat(salvo.getValue().getClubeOrigemId()).isEqualTo(clube.getId());
    }

    @Test
    void atingirAMetaDoClubeMarcaComoLido() {
        when(leituraPessoalRepository.findByUsuarioIdAndLivroGoogleId(1, "g-duna")).thenReturn(Optional.empty());

        estanteService.sincronizarProgressoDoClube(usuario, leituraDoClube(TipoMeta.CAPITULO, 20), 20);

        ArgumentCaptor<LeituraPessoal> salvo = ArgumentCaptor.forClass(LeituraPessoal.class);
        verify(leituraPessoalRepository).save(salvo.capture());
        assertThat(salvo.getValue().getStatus()).isEqualTo(StatusLeituraPessoal.LIDO);
    }

    @Test
    void livroJaLidoNaEstanteNaoRegride() {
        LeituraPessoal lido = LeituraPessoal.adicionar(usuario, "g-duna", "Duna", null, null, 400,
                StatusLeituraPessoal.LIDO, LocalDate.now());
        when(leituraPessoalRepository.findByUsuarioIdAndLivroGoogleId(1, "g-duna")).thenReturn(Optional.of(lido));

        estanteService.sincronizarProgressoDoClube(usuario, leituraDoClube(TipoMeta.PAGINA, 400), 10);

        verify(leituraPessoalRepository, never()).save(any());
        assertThat(lido.getStatus()).isEqualTo(StatusLeituraPessoal.LIDO);
    }

    @Test
    void progressoZeroNaoMexeNaEstante() {
        estanteService.sincronizarProgressoDoClube(usuario, leituraDoClube(TipoMeta.PAGINA, 400), 0);
        verify(leituraPessoalRepository, never()).findByUsuarioIdAndLivroGoogleId(any(), any());
    }
}
