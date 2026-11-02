package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.UsuarioRepository;
import github.jotagm.clube_livro.application.port.out.ArmazenamentoDeFotos;
import github.jotagm.clube_livro.application.port.out.ArmazenamentoDeFotos.EnvioAssinado;
import github.jotagm.clube_livro.application.port.out.ArmazenamentoDeFotos.Objeto;
import github.jotagm.clube_livro.domain.exceptions.AcessoNegadoException;
import github.jotagm.clube_livro.domain.exceptions.RegraDeNegocioException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FotoPerfilServiceTest {

    private static final long DOIS_MB = 2L * 1024 * 1024;

    private ArmazenamentoDeFotos armazenamento;
    private UsuarioRepository usuarioRepository;
    private FotoPerfilService servico;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        armazenamento = mock(ArmazenamentoDeFotos.class);
        usuarioRepository = mock(UsuarioRepository.class);
        when(armazenamento.habilitado()).thenReturn(true);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        servico = new FotoPerfilService(armazenamento, usuarioRepository, DOIS_MB);
        ana = Usuario.builder().id(7).nome("Ana").email("ana@email.com").build();
    }

    // ---- pedir o envio

    @Test
    void solicitarEnvio_geraChaveSobOPrefixoDoUsuarioComAExtensaoDoTipo() {
        when(armazenamento.prepararEnvio(anyString(), anyString(), anyLong()))
                .thenAnswer(inv -> new EnvioAssinado(inv.getArgument(0), "https://s3/x", Instant.now()));

        EnvioAssinado envio = servico.solicitarEnvio(ana, " IMAGE/PNG ", 1000);

        assertThat(envio.chave()).startsWith("fotos/7/").endsWith(".png");
        verify(armazenamento).prepararEnvio(envio.chave(), "image/png", 1000);
    }

    @Test
    void solicitarEnvio_recusaFormatoQueNaoEImagemAceita() {
        for (String tipo : new String[]{"image/gif", "image/svg+xml", "text/html", "application/pdf", "", null}) {
            assertThatThrownBy(() -> servico.solicitarEnvio(ana, tipo, 1000))
                    .as("tipo %s", tipo)
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Formato não aceito");
        }
        verify(armazenamento, never()).prepararEnvio(anyString(), anyString(), anyLong());
    }

    @Test
    void solicitarEnvio_recusaTamanhoZeroNegativoOuAcimaDoLimite() {
        for (long tamanho : new long[]{0, -1, DOIS_MB + 1}) {
            assertThatThrownBy(() -> servico.solicitarEnvio(ana, "image/jpeg", tamanho))
                    .as("tamanho %d", tamanho)
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("até 2 MB");
        }
    }

    @Test
    void solicitarEnvio_noLimiteExatoEAceito() {
        when(armazenamento.prepararEnvio(anyString(), anyString(), anyLong()))
                .thenReturn(new EnvioAssinado("fotos/7/a.jpg", "https://s3/x", Instant.now()));

        assertThat(servico.solicitarEnvio(ana, "image/jpeg", DOIS_MB)).isNotNull();
    }

    @Test
    void semArmazenamentoConfiguradoResponde503() {
        when(armazenamento.habilitado()).thenReturn(false);

        assertThatThrownBy(() -> servico.solicitarEnvio(ana, "image/jpeg", 10))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");
        assertThatThrownBy(() -> servico.confirmar(ana, "fotos/7/a.jpg"))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ---- confirmar

    @Test
    void confirmar_guardaAChaveEApagaAFotoAnterior() {
        ana.definirFoto("fotos/7/antiga.jpg");
        when(armazenamento.inspecionar("fotos/7/nova.png")).thenReturn(Optional.of(new Objeto(500, "image/png")));

        Usuario salvo = servico.confirmar(ana, "fotos/7/nova.png");

        assertThat(salvo.getFotoChave()).isEqualTo("fotos/7/nova.png");
        verify(usuarioRepository).save(ana);
        verify(armazenamento).apagar("fotos/7/antiga.jpg");
    }

    @Test
    void confirmar_primeiraFotoNaoApagaNada() {
        when(armazenamento.inspecionar("fotos/7/nova.png")).thenReturn(Optional.of(new Objeto(500, "image/png")));

        servico.confirmar(ana, "fotos/7/nova.png");

        verify(armazenamento, never()).apagar(anyString());
    }

    @Test
    void confirmar_recusaChaveDeOutroUsuarioMesmoSeOObjetoExistir() {
        for (String chave : new String[]{"fotos/8/x.jpg", "fotos/7/../8/x.jpg", "x.jpg", "fotos/70/x.jpg", null}) {
            assertThatThrownBy(() -> servico.confirmar(ana, chave))
                    .as("chave %s", chave)
                    .isInstanceOf(AcessoNegadoException.class);
        }
        verify(armazenamento, never()).inspecionar(anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void confirmar_objetoQueNaoFoiEnviadoDaRegraDeNegocio() {
        when(armazenamento.inspecionar("fotos/7/x.jpg")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.confirmar(ana, "fotos/7/x.jpg"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ainda não foi enviada");
        assertThat(ana.getFotoChave()).isNull();
    }

    @Test
    void confirmar_objetoMaiorQueOLimiteOuDeOutroTipoEApagadoERecusado() {
        when(armazenamento.inspecionar("fotos/7/grande.jpg")).thenReturn(Optional.of(new Objeto(DOIS_MB + 1, "image/jpeg")));
        when(armazenamento.inspecionar("fotos/7/falso.jpg")).thenReturn(Optional.of(new Objeto(10, "text/html")));

        assertThatThrownBy(() -> servico.confirmar(ana, "fotos/7/grande.jpg")).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> servico.confirmar(ana, "fotos/7/falso.jpg")).isInstanceOf(RegraDeNegocioException.class);

        verify(armazenamento).apagar("fotos/7/grande.jpg");
        verify(armazenamento).apagar("fotos/7/falso.jpg");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void confirmar_falhaAoApagarAFotoAntigaNaoDesfazATroca() {
        ana.definirFoto("fotos/7/antiga.jpg");
        when(armazenamento.inspecionar("fotos/7/nova.jpg")).thenReturn(Optional.of(new Objeto(10, "image/jpeg")));
        doThrow(new RuntimeException("s3 fora do ar")).when(armazenamento).apagar("fotos/7/antiga.jpg");

        Usuario salvo = servico.confirmar(ana, "fotos/7/nova.jpg");

        assertThat(salvo.getFotoChave()).isEqualTo("fotos/7/nova.jpg");
    }

    // ---- remover e ler

    @Test
    void remover_limpaAChaveEApagaOObjeto() {
        ana.definirFoto("fotos/7/a.jpg");

        Usuario salvo = servico.remover(ana);

        assertThat(salvo.getFotoChave()).isNull();
        verify(armazenamento).apagar("fotos/7/a.jpg");
    }

    @Test
    void remover_semFotoNaoFazNada() {
        servico.remover(ana);

        verify(usuarioRepository, never()).save(any());
        verify(armazenamento, never()).apagar(anyString());
    }

    @Test
    void urlDe_nulaSemFotoOuComArmazenamentoDesligado() {
        assertThat(servico.urlDe(ana)).isNull();

        ana.definirFoto("fotos/7/a.jpg");
        when(armazenamento.habilitado()).thenReturn(false);
        assertThat(servico.urlDe(ana)).isNull();
    }

    @Test
    void urlDe_devolveAUrlAssinadaDaChave() {
        ana.definirFoto("fotos/7/a.jpg");
        when(armazenamento.urlDeLeitura("fotos/7/a.jpg")).thenReturn("https://s3/assinada");

        assertThat(servico.urlDe(ana)).isEqualTo("https://s3/assinada");
    }
}
