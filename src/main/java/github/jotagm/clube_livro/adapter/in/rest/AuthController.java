package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.auth.otp.DesafioEmitido;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.LoginCodigoRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.LoginRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.LoginResponse;
import github.jotagm.clube_livro.application.service.LoginService;
import github.jotagm.clube_livro.configs.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @ApiResponse(responseCode = "429", description = "Código pedido antes do intervalo; ver Retry-After", content = @io.swagger.v3.oas.annotations.media.Content)
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha())
        );

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
