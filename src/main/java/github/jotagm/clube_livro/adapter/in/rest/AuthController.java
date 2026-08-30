package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.LoginRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.LoginResponse;
import github.jotagm.clube_livro.configs.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
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

    @Operation(
            summary = "Autentica um usuário e emite o token JWT",
            description = """
                    Endpoint público. O token retornado deve ser enviado nas demais requisições
                    no cabeçalho `Authorization: Bearer <token>`.
                    """)
    @ApiResponse(responseCode = "200", description = "Credenciais válidas — token emitido")
    @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos", content = @io.swagger.v3.oas.annotations.media.Content)
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.gerarToken(userDetails.getUsername());

        return ResponseEntity.ok(new LoginResponse(token));
    }
}
