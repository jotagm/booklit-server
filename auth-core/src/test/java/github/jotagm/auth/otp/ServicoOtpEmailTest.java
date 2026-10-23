package github.jotagm.auth.otp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static github.jotagm.auth.otp.Verificacao.Status.CODIGO_INCORRETO;
import static github.jotagm.auth.otp.Verificacao.Status.DESAFIO_INDISPONIVEL;
import static github.jotagm.auth.otp.Verificacao.Status.VERIFICADO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicoOtpEmailTest {

    private static final byte[] CHAVE = "chave-de-teste-com-pelo-menos-32-bytes!!".getBytes(StandardCharsets.UTF_8);
    private static final String EMAIL = "Jota@Teste.Local ";

    private final List<EnviadorDeOtp.Mensagem> enviadas = new ArrayList<>();
    private RelogioAjustavel relogio;
    private RepositorioDeDesafiosEmMemoria repositorio;
    private ServicoOtpEmail servico;

    @BeforeEach
    void preparar() {
        relogio = new RelogioAjustavel(Instant.parse("2026-10-23T20:00:00Z"));
        repositorio = new RepositorioDeDesafiosEmMemoria();
        servico = new ServicoOtpEmail(repositorio, enviadas::add, GeradorDeCodigo.seguro(),
                new HasherDeCodigo(CHAVE), PoliticaOtp.padrao(), relogio);
    }

    @Test
    void emiteEntregaEVerificaUmaVez() {
        DesafioEmitido emitido = servico.emitir(EMAIL, Finalidade.LOGIN);

        assertEquals(1, enviadas.size());
        EnviadorDeOtp.Mensagem mensagem = enviadas.getFirst();
        assertEquals("jota@teste.local", mensagem.destinatario());
        assertEquals(emitido.desafioId(), mensagem.desafioId());
        assertEquals(relogio.instant().plus(Duration.ofMinutes(10)), emitido.expiraEm());

        Verificacao verificacao = servico.verificar(emitido.desafioId(), Finalidade.LOGIN, mensagem.codigo());
        assertEquals(VERIFICADO, verificacao.status());
        assertEquals("jota@teste.local", verificacao.destinatario());

        assertEquals(DESAFIO_INDISPONIVEL,
                servico.verificar(emitido.desafioId(), Finalidade.LOGIN, mensagem.codigo()).status(),
                "código certo não pode valer duas vezes");
    }

    @Test
    void naoGuardaOCodigoEmClaro() {
        DesafioEmitido emitido = servico.emitir(EMAIL, Finalidade.LOGIN);
        String codigo = enviadas.getFirst().codigo();

        DesafioOtp guardado = repositorio.reservarTentativa(emitido.desafioId(), relogio.instant()).orElseThrow();
        assertFalse(guardado.codigoHash().contains(codigo));
        assertFalse(enviadas.getFirst().toString().contains(codigo), "toString da mensagem vai parar em log");
    }

    @Test
    void codigoErradoGastaTentativaEAoEsgotarNemOCertoPassa() {
        DesafioEmitido emitido = servico.emitir(EMAIL, Finalidade.LOGIN);
        String certo = enviadas.getFirst().codigo();
        String errado = certo.equals("000000") ? "111111" : "000000";

        for (int restantes = 4; restantes >= 0; restantes--) {
            Verificacao v = servico.verificar(emitido.desafioId(), Finalidade.LOGIN, errado);
            assertEquals(CODIGO_INCORRETO, v.status());
            assertEquals(restantes, v.tentativasRestantes());
        }
        assertEquals(DESAFIO_INDISPONIVEL, servico.verificar(emitido.desafioId(), Finalidade.LOGIN, certo).status());
    }

    @Test
    void entradaMalformadaTambemGastaTentativa() {
        DesafioEmitido emitido = servico.emitir(EMAIL, Finalidade.LOGIN);

        Verificacao v = servico.verificar(emitido.desafioId(), Finalidade.LOGIN, "abc");

        assertEquals(CODIGO_INCORRETO, v.status());
        assertEquals(4, v.tentativasRestantes());
    }

    @Test
    void expira() {
        DesafioEmitido emitido = servico.emitir(EMAIL, Finalidade.LOGIN);
        relogio.avancar(Duration.ofMinutes(10));

        assertEquals(DESAFIO_INDISPONIVEL,
                servico.verificar(emitido.desafioId(), Finalidade.LOGIN, enviadas.getFirst().codigo()).status());
    }

    @Test
    void codigoDeUmaFinalidadeNaoServeParaOutra() {
        DesafioEmitido emitido = servico.emitir(EMAIL, Finalidade.VERIFICAR_EMAIL);

        assertEquals(CODIGO_INCORRETO,
                servico.verificar(emitido.desafioId(), Finalidade.LOGIN, enviadas.getFirst().codigo()).status());
    }

    @Test
    void desafioInexistente() {
        assertEquals(DESAFIO_INDISPONIVEL, servico.verificar(UUID.randomUUID(), Finalidade.LOGIN, "123456").status());
    }

    @Test
    void mesmoCodigoEmDesafiosDiferentesTemHashesDiferentes() {
        HasherDeCodigo hasher = new HasherDeCodigo(CHAVE);
        assertNotEquals(hasher.hash(UUID.randomUUID(), Finalidade.LOGIN, "123456"),
                hasher.hash(UUID.randomUUID(), Finalidade.LOGIN, "123456"));
    }

    @Test
    void respeitaIntervaloEntreEmissoes() {
        servico.emitir(EMAIL, Finalidade.LOGIN);

        LimiteDeEmissaoException ex = assertThrows(LimiteDeEmissaoException.class,
                () -> servico.emitir(EMAIL, Finalidade.LOGIN));
        assertEquals(Duration.ofSeconds(60), ex.tenteNovamenteEm());

        relogio.avancar(Duration.ofSeconds(60));
        servico.emitir(EMAIL, Finalidade.LOGIN);
        assertEquals(2, enviadas.size());
    }

    @Test
    void intervaloEPorFinalidade() {
        servico.emitir(EMAIL, Finalidade.LOGIN);
        servico.emitir(EMAIL, Finalidade.VERIFICAR_EMAIL);
        assertEquals(2, enviadas.size());
    }

    @Test
    void respeitaTetoPorJanela() {
        for (int i = 0; i < 5; i++) {
            servico.emitir(EMAIL, Finalidade.LOGIN);
            relogio.avancar(Duration.ofMinutes(2));
        }
        assertThrows(LimiteDeEmissaoException.class, () -> servico.emitir(EMAIL, Finalidade.LOGIN));

        relogio.avancar(Duration.ofHours(1));
        servico.emitir(EMAIL, Finalidade.LOGIN);
    }

    @Test
    void emissaoSemEntregaRegistraMasNaoEnvia() {
        DesafioEmitido emitido = servico.emitirSemEntrega(EMAIL, Finalidade.LOGIN);

        assertTrue(enviadas.isEmpty());
        assertThrows(LimiteDeEmissaoException.class, () -> servico.emitirSemEntrega(EMAIL, Finalidade.LOGIN),
                "o limite precisa valer igual, senão o 429 denuncia quem tem conta");
        assertEquals(CODIGO_INCORRETO, servico.verificar(emitido.desafioId(), Finalidade.LOGIN, "123456").status());
    }

    @Test
    void emailInvalido() {
        assertThrows(IllegalArgumentException.class, () -> servico.emitir("sem-arroba", Finalidade.LOGIN));
        assertThrows(IllegalArgumentException.class, () -> servico.emitir("   ", Finalidade.LOGIN));
    }

    @Test
    void codigoCertoEmParaleloVerificaUmaVezSo() throws Exception {
        DesafioEmitido emitido = servico.emitir(EMAIL, Finalidade.LOGIN);
        String certo = enviadas.getFirst().codigo();

        List<Verificacao> resultados = emParalelo(20, () -> servico.verificar(emitido.desafioId(), Finalidade.LOGIN, certo));

        assertEquals(1, resultados.stream().filter(Verificacao::sucesso).count());
    }

    @Test
    void chutesEmParaleloNaoPassamDoLimiteDeTentativas() throws Exception {
        DesafioEmitido emitido = servico.emitir(EMAIL, Finalidade.LOGIN);
        String errado = enviadas.getFirst().codigo().equals("000000") ? "111111" : "000000";

        List<Verificacao> resultados = emParalelo(50, () -> servico.verificar(emitido.desafioId(), Finalidade.LOGIN, errado));

        assertEquals(5, resultados.stream().filter(v -> v.status() == CODIGO_INCORRETO).count());
        assertEquals(45, resultados.stream().filter(v -> v.status() == DESAFIO_INDISPONIVEL).count());
    }

    @Test
    void geradorSeguroSempreDevolveSeisDigitos() {
        GeradorDeCodigo gerador = GeradorDeCodigo.seguro();
        for (int i = 0; i < 10_000; i++) {
            String codigo = gerador.gerar();
            assertEquals(codigo, CodigoOtp.normalizar(codigo).orElseThrow());
        }
    }

    @Test
    void chaveCurtaERecusada() {
        assertThrows(IllegalArgumentException.class, () -> new HasherDeCodigo(new byte[16]));
    }

    private static <T> List<T> emParalelo(int vezes, java.util.concurrent.Callable<T> tarefa) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(vezes);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<T>> futuros = new ArrayList<>();
            for (int i = 0; i < vezes; i++) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return tarefa.call();
                }));
            }
            largada.countDown();
            List<T> resultados = new ArrayList<>();
            for (Future<T> f : futuros) {
                resultados.add(f.get());
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }
}
