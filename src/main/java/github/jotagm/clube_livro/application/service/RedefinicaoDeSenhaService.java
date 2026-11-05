package github.jotagm.clube_livro.application.service;

import github.jotagm.auth.otp.DesafioEmitido;
import github.jotagm.auth.otp.Finalidade;
import github.jotagm.auth.otp.ServicoOtpEmail;
import github.jotagm.auth.otp.Verificacao;
import github.jotagm.clube_livro.adapter.out.persistence.UsuarioRepository;
import github.jotagm.clube_livro.configs.JwtService;
import github.jotagm.clube_livro.domain.exceptions.NaoAutenticadoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * "Esqueci minha senha", em duas chamadas públicas:
 * <ol>
 *   <li>{@link #solicitar}: manda um código de 6 dígitos para o e-mail. A resposta é a mesma
 *   exista conta ou não (o {@link OtpService} só entrega quando existe), e os limites do
 *   auth-core valem igual: um envio a cada 60 s, 5 por hora, código de 10 minutos e 5
 *   tentativas.</li>
 *   <li>{@link #redefinir}: código certo + senha nova. Troca o hash, confirma o e-mail (o código
 *   provou a caixa de entrada), zera o bloqueio por senhas erradas e derruba os tokens
 *   anteriores. Devolve um token novo: a pessoa já sai da tela logada.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedefinicaoDeSenhaService {

    private final OtpService otpService;
    private final ServicoOtpEmail otp;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProtecaoDeLoginService protecaoDeLogin;
    private final JwtService jwtService;
    private final Clock relogio;

    @Transactional
    public DesafioEmitido solicitar(String email) {
        return otpService.emitir(email, Finalidade.REDEFINIR_SENHA);
    }

    /**
     * O 401 de código errado não desfaz a transação: é nela que a tentativa gasta fica gravada.
     * Com rollback, o contador nunca baixaria e o código poderia ser chutado à vontade.
     */
    @Transactional(noRollbackFor = NaoAutenticadoException.class)
    public String redefinir(UUID desafioId, String codigo, String novaSenha) {
        Verificacao verificacao = otp.verificar(desafioId, Finalidade.REDEFINIR_SENHA, codigo);
        return switch (verificacao.status()) {
            case VERIFICADO -> trocarSenha(verificacao.destinatario(), novaSenha);
            case CODIGO_INCORRETO -> throw new NaoAutenticadoException(
                    verificacao.tentativasRestantes() > 0
                            ? "Código incorreto. Tentativas restantes: " + verificacao.tentativasRestantes() + "."
                            : "Código incorreto. Peça um novo código.");
            case DESAFIO_INDISPONIVEL -> throw new NaoAutenticadoException(
                    "Código expirado ou já utilizado. Peça um novo código.");
        };
    }

    private String trocarSenha(String email, String novaSenha) {
        // Desafio de REDEFINIR_SENHA só é entregue a quem tem conta; não achar aqui é a conta
        // ter deixado de existir entre o pedido e a troca.
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NaoAutenticadoException("Conta não encontrada"));
        LocalDateTime agora = LocalDateTime.now(relogio);
        // O corte dos tokens antigos é comparado com o iat do JWT, que usa o relógio do sistema
        // (o JwtService não usa o Clock): o momento da troca sai do mesmo relógio.
        LocalDateTime corte = LocalDateTime.ofInstant(Instant.now(), relogio.getZone());
        usuario.redefinirSenha(passwordEncoder.encode(novaSenha), corte);
        usuario.marcarEmailVerificado(agora);
        usuarioRepository.save(usuario);
        protecaoDeLogin.registrarSucesso(usuario.getEmail());
        log.info("Senha redefinida usuario={}", usuario.getId());
        return jwtService.gerarToken(usuario.getEmail());
    }
}
