package github.jotagm.clube_livro.application.service;

import github.jotagm.auth.otp.DesafioEmitido;
import github.jotagm.auth.otp.Finalidade;
import github.jotagm.auth.otp.ServicoOtpEmail;
import github.jotagm.auth.otp.Verificacao;
import github.jotagm.clube_livro.adapter.out.persistence.UsuarioRepository;
import github.jotagm.clube_livro.domain.exceptions.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fronteira transacional do OTP e a única regra que depende do Booklit: saber se o e-mail
 * tem conta. O resto (limites, hash, tentativas, uso único) é do auth-core.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final ServicoOtpEmail servico;
    private final UsuarioRepository usuarioRepository;

    /**
     * Para finalidades que exigem conta, um e-mail sem conta recebe a mesma resposta e passa
     * pelo mesmo limite, só que nada é enviado: quem sonda não descobre quem é cliente.
     */
    @Transactional
    public DesafioEmitido emitir(String email, Finalidade finalidade) {
        recusarLogin(finalidade);
        String destinatario = ServicoOtpEmail.normalizarEmail(email);
        boolean entregar = !finalidade.exigeContaExistente() || existeConta(destinatario);
        return entregar
                ? servico.emitir(destinatario, finalidade)
                : servico.emitirSemEntrega(destinatario, finalidade);
    }

    /** Sem rollback em código incorreto: a tentativa gasta precisa ficar gravada. */
    @Transactional
    public Verificacao verificar(UUID desafioId, Finalidade finalidade, String codigo) {
        recusarLogin(finalidade);
        Verificacao verificacao = servico.verificar(desafioId, finalidade, codigo);
        if (verificacao.sucesso() && finalidade == Finalidade.VERIFICAR_EMAIL) {
            usuarioRepository.findByEmailIgnoreCase(verificacao.destinatario()).ifPresent(usuario -> {
                usuario.marcarEmailVerificado(LocalDateTime.now());
                usuarioRepository.save(usuario);
            });
        }
        return verificacao;
    }

    /**
     * O código de LOGIN só sai depois da senha conferida (POST /auth/login). Se este endpoint
     * público emitisse LOGIN, quem tivesse acesso à caixa de e-mail entraria sem a senha.
     */
    private static void recusarLogin(Finalidade finalidade) {
        if (finalidade == Finalidade.LOGIN) {
            throw new RegraDeNegocioException("O código de login sai pelo POST /auth/login, depois da senha");
        }
    }

    private boolean existeConta(String email) {
        // O cadastro guarda o e-mail como digitado; comparar normalizado evita "Jota@" ≠ "jota@".
        return usuarioRepository.existsByEmailIgnoreCase(email);
    }
}
