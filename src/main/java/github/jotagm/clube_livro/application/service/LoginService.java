package github.jotagm.clube_livro.application.service;

import github.jotagm.auth.otp.DesafioEmitido;
import github.jotagm.auth.otp.Finalidade;
import github.jotagm.auth.otp.ServicoOtpEmail;
import github.jotagm.auth.otp.Verificacao;
import github.jotagm.clube_livro.adapter.out.persistence.UsuarioRepository;
import github.jotagm.clube_livro.configs.JwtService;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Segunda etapa do login: depois da senha, um código enviado por e-mail.
 *
 * <p>O desafio de LOGIN só nasce aqui, depois da senha conferida (o endpoint público de OTP
 * recusa essa finalidade). Por isso desafio + código certo = senha + posse do e-mail: o
 * código sozinho nunca dá acesso. O primeiro login concluído também confirma o e-mail da
 * conta, o que cobre a verificação do cadastro sem um fluxo à parte.
 */
@Slf4j
@Service
public class LoginService {

    private final ServicoOtpEmail otp;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final boolean exigirCodigo;

    public LoginService(ServicoOtpEmail otp,
                        UsuarioRepository usuarioRepository,
                        JwtService jwtService,
                        @Value("${booklit.auth.login.exigir-codigo:false}") boolean exigirCodigo) {
        this.otp = otp;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.exigirCodigo = exigirCodigo;
    }

    public boolean exigeCodigo() {
        return exigirCodigo;
    }

    /** Chamado só depois da senha conferida. Repetir o login é o "reenviar código". */
    @Transactional
    public DesafioEmitido iniciarDesafio(String email) {
        return otp.emitir(email, Finalidade.LOGIN);
    }

    /**
     * O 401 de código errado não pode desfazer a transação: é nela que a tentativa gasta fica
     * gravada. Com rollback, o contador nunca baixaria e o código poderia ser chutado à vontade.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public String concluir(UUID desafioId, String codigo) {
        Verificacao verificacao = otp.verificar(desafioId, Finalidade.LOGIN, codigo);
        return switch (verificacao.status()) {
            case VERIFICADO -> emitirToken(verificacao.destinatario());
            case CODIGO_INCORRETO -> throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    verificacao.tentativasRestantes() > 0
                            ? "Código incorreto. Tentativas restantes: " + verificacao.tentativasRestantes() + "."
                            : "Código incorreto. Entre de novo para receber outro código.");
            case DESAFIO_INDISPONIVEL -> throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Código expirado ou já utilizado. Entre de novo para receber outro código.");
        };
    }

    private String emitirToken(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
        if (!usuario.emailVerificado()) {
            usuario.marcarEmailVerificado(LocalDateTime.now());
            usuarioRepository.save(usuario);
            log.info("E-mail verificado no login usuario={}", usuario.getId());
        }
        return jwtService.gerarToken(usuario.getEmail());
    }
}
