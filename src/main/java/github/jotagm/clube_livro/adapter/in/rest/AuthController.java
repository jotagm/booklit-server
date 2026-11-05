package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.auth.otp.DesafioEmitido;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.EsqueciSenhaRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.LoginCodigoRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.RedefinirSenhaRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.DesafioOtpResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.LoginRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.LoginResponse;
import github.jotagm.clube_livro.application.service.LoginService;
import github.jotagm.clube_livro.application.service.ProtecaoDeLoginService;
import github.jotagm.clube_livro.application.service.RedefinicaoDeSenhaService;
import github.jotagm.clube_livro.configs.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
@Tag(name = "Autenticação", description = "Emissão do token JWT usado pelos demais endpoints")
@SecurityRequirements
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final LoginService loginService;
    private final ProtecaoDeLoginService protecaoDeLogin;
    private final RedefinicaoDeSenhaService redefinicaoDeSenha;

    @Operation(
            summary = "Esqueci minha senha: envia um código para o e-mail",
            description = """
                    Endpoint público. A resposta é a mesma exista conta para o e-mail ou não (sem conta,
                    nada é enviado). Limites: um envio a cada 60 s e no máximo 5 por hora. O código vale
                    10 minutos, aceita 5 tentativas e só pode ser usado uma vez.
                    """)
    @ApiResponse(responseCode = "202", description = "Pedido aceito; se houver conta, o código segue por e-mail")
    @ApiResponse(responseCode = "429", description = "Pedido antes do intervalo ou acima do limite; ver Retry-After",
            content = @io.swagger.v3.oas.annotations.media.Content)
    @PostMapping("/senha/esqueci")
    public ResponseEntity<DesafioOtpResponse> esqueciSenha(@RequestBody @Valid EsqueciSenhaRequest request) {
        DesafioEmitido desafio = redefinicaoDeSenha.solicitar(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new DesafioOtpResponse(desafio.desafioId(), desafio.expiraEm()));
    }

    @Operation(
            summary = "Redefine a senha com o código do e-mail",
            description = """
                    Endpoint público. Código certo troca a senha, confirma o e-mail, libera o login se
                    estava bloqueado por senhas erradas e encerra as sessões abertas em outros aparelhos.
                    A resposta traz um token novo: a pessoa já entra com a senha nova.
                    """)
    @ApiResponse(responseCode = "200", description = "Senha redefinida — token emitido")
    @ApiResponse(responseCode = "400", description = "Senha nova fora do tamanho aceito", content = @io.swagger.v3.oas.annotations.media.Content)
    @ApiResponse(responseCode = "401", description = "Código incorreto, expirado ou já usado", content = @io.swagger.v3.oas.annotations.media.Content)
    @PostMapping("/senha/redefinir")
    public ResponseEntity<LoginResponse> redefinirSenha(@RequestBody @Valid RedefinirSenhaRequest request) {
        return ResponseEntity.ok(LoginResponse.comToken(
                redefinicaoDeSenha.redefinir(request.desafioId(), request.codigo(), request.novaSenha())));
    }

    @Operation(
            summary = "Confere e-mail e senha (primeira etapa do login)",
            description = """
                    Endpoint público. Com o código por e-mail desligado, responde 200 com
                    `etapa=CONCLUIDO` e o `token` (enviar em `Authorization: Bearer <token>`).
                    Ligado, responde 202 com `etapa=CODIGO` e o `desafioId`: um código de 6
                    dígitos vai para o e-mail da conta e o token sai em `POST /auth/login/codigo`.
                    Chamar de novo reenvia o código (no máximo um a cada 60 s).
                    """)
    @ApiResponse(responseCode = "200", description = "Credenciais válidas — token emitido")
    @ApiResponse(responseCode = "202", description = "Credenciais válidas — código enviado por e-mail")
    @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos", content = @io.swagger.v3.oas.annotations.media.Content)
    @ApiResponse(responseCode = "429", description = "Muitas senhas erradas para o e-mail ou o IP, ou código pedido antes do intervalo; ver Retry-After", content = @io.swagger.v3.oas.annotations.media.Content)
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request,
                                               HttpServletRequest http) {
        // Atrás do Caddy, o IP real vem do X-Forwarded-For (server.forward-headers-strategy).
        // A tentativa conta como falha até a senha ser conferida; e-mail inexistente conta igual
        // a senha errada, para o limite não revelar quem tem conta.
        protecaoDeLogin.reservarTentativa(request.email(), http.getRemoteAddr());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha())
        );
        protecaoDeLogin.registrarSucesso(request.email());

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        if (!loginService.exigeCodigo()) {
            return ResponseEntity.ok(LoginResponse.comToken(jwtService.gerarToken(userDetails.getUsername())));
        }

        DesafioEmitido desafio = loginService.iniciarDesafio(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(LoginResponse.comDesafio(desafio.desafioId(), desafio.expiraEm()));
    }

    @Operation(
            summary = "Confere o código do e-mail e emite o token (segunda etapa do login)",
            description = "Endpoint público. O primeiro login concluído também marca o e-mail da conta como verificado.")
    @ApiResponse(responseCode = "200", description = "Código correto — token emitido")
    @ApiResponse(responseCode = "401", description = "Código incorreto, expirado ou já usado", content = @io.swagger.v3.oas.annotations.media.Content)
    @PostMapping("/login/codigo")
    public ResponseEntity<LoginResponse> concluirLogin(@RequestBody @Valid LoginCodigoRequest request) {
        return ResponseEntity.ok(LoginResponse.comToken(loginService.concluir(request.desafioId(), request.codigo())));
    }
}
