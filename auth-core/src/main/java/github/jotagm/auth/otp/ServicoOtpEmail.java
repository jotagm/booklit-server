package github.jotagm.auth.otp;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Emissão e verificação de OTP por e-mail.
 *
 * <p>Sem framework e sem transação: quem usa decide a fronteira transacional e implementa as
 * portas. O que este serviço garante, desde que o {@link RepositorioDeDesafios} cumpra o
 * contrato de atomicidade:
 * <ul>
 *   <li>o código nunca é persistido, só o HMAC;</li>
 *   <li>cada desafio aceita no máximo {@link PoliticaOtp#maxTentativas()} verificações,
 *       inclusive sob requisições paralelas;</li>
 *   <li>um código certo vale uma vez só e só para a finalidade com que foi emitido;</li>
 *   <li>pedidos de código respeitam intervalo mínimo e teto por janela.</li>
 * </ul>
 */
public final class ServicoOtpEmail {

    private final RepositorioDeDesafios repositorio;
    private final EnviadorDeOtp enviador;
    private final GeradorDeCodigo gerador;
    private final HasherDeCodigo hasher;
    private final PoliticaOtp politica;
    private final Clock relogio;

    public ServicoOtpEmail(RepositorioDeDesafios repositorio,
                           EnviadorDeOtp enviador,
                           GeradorDeCodigo gerador,
                           HasherDeCodigo hasher,
                           PoliticaOtp politica,
                           Clock relogio) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.enviador = Objects.requireNonNull(enviador);
        this.gerador = Objects.requireNonNull(gerador);
        this.hasher = Objects.requireNonNull(hasher);
        this.politica = Objects.requireNonNull(politica);
        this.relogio = Objects.requireNonNull(relogio);
    }

    /** Emite e entrega um código para o e-mail. */
    public DesafioEmitido emitir(String email, Finalidade finalidade) {
        return emitir(email, finalidade, true);
    }

    /**
     * Emite sem entregar: mesmo limite, mesma resposta, mesmo registro, só que nada sai.
     *
     * <p>Serve para não revelar se um e-mail tem conta: quando a finalidade exige conta e ela
     * não existe, quem chama usa este método e a resposta para o cliente é idêntica.
     */
    public DesafioEmitido emitirSemEntrega(String email, Finalidade finalidade) {
        return emitir(email, finalidade, false);
    }

    public Verificacao verificar(UUID desafioId, Finalidade finalidade, String codigoDigitado) {
        Objects.requireNonNull(desafioId, "desafioId");
        Objects.requireNonNull(finalidade, "finalidade");
        Instant agora = relogio.instant();

        // A tentativa é gasta antes de olhar o código, inclusive quando o formato é inválido:
        // senão entrada malformada viraria sonda gratuita.
        Optional<DesafioOtp> reservado = repositorio.reservarTentativa(desafioId, agora);
        if (reservado.isEmpty()) {
            return Verificacao.indisponivel();
        }
        DesafioOtp desafio = reservado.get();

        boolean confere = desafio.finalidade() == finalidade
                && CodigoOtp.normalizar(codigoDigitado)
                .map(codigo -> hasher.confere(desafio.codigoHash(), desafio.id(), finalidade, codigo))
                .orElse(false);

        if (!confere) {
            return Verificacao.incorreto(desafio.tentativasRestantes());
        }
        // Duas requisições com o código certo em paralelo: só a primeira consome.
        return repositorio.consumir(desafio.id(), agora)
                ? Verificacao.verificado(desafio.destinatario())
                : Verificacao.indisponivel();
    }

    /** E-mail como chave de limite e de desafio: sem espaço nas pontas e em minúsculas. */
    public static String normalizarEmail(String email) {
        Objects.requireNonNull(email, "email");
        String normalizado = email.strip().toLowerCase(Locale.ROOT);
        if (normalizado.isEmpty() || normalizado.indexOf('@') < 1 || normalizado.length() > 254) {
            throw new IllegalArgumentException("E-mail inválido");
        }
        return normalizado;
    }

    private DesafioEmitido emitir(String email, Finalidade finalidade, boolean entregar) {
        Objects.requireNonNull(finalidade, "finalidade");
        String destinatario = normalizarEmail(email);
        Instant agora = relogio.instant();

        verificarLimites(destinatario, finalidade, agora);

        UUID id = UUID.randomUUID();
        String codigo = gerador.gerar();
        if (CodigoOtp.normalizar(codigo).filter(codigo::equals).isEmpty()) {
            throw new IllegalStateException("Gerador devolveu código fora do formato");
        }
        Instant expiraEm = agora.plus(politica.validade());

        repositorio.salvar(new DesafioOtp(
                id, destinatario, finalidade,
                hasher.hash(id, finalidade, codigo),
                agora, expiraEm, politica.maxTentativas(), null));

        if (entregar) {
            enviador.enviar(new EnviadorDeOtp.Mensagem(id, destinatario, finalidade, codigo, expiraEm));
        }
        return new DesafioEmitido(id, expiraEm);
    }

    private void verificarLimites(String destinatario, Finalidade finalidade, Instant agora) {
        Optional<Instant> ultima = repositorio.ultimaEmissao(destinatario, finalidade);
        if (ultima.isPresent()) {
            Instant liberadoEm = ultima.get().plus(politica.intervaloEntreEmissoes());
            if (agora.isBefore(liberadoEm)) {
                throw new LimiteDeEmissaoException(Duration.between(agora, liberadoEm));
            }
        }
        Instant inicioJanela = agora.minus(politica.janelaDeEmissoes());
        long emitidos = repositorio.contarEmissoesDesde(destinatario, finalidade, inicioJanela);
        if (emitidos >= politica.maxEmissoesPorJanela()) {
            throw new LimiteDeEmissaoException(politica.janelaDeEmissoes());
        }
    }
}
