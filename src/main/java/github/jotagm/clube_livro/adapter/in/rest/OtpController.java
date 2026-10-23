package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.auth.otp.DesafioEmitido;
import github.jotagm.auth.otp.Verificacao;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.OtpEmailRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.VerificarOtpRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.DesafioOtpResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.OtpVerificadoResponse;
import github.jotagm.clube_livro.application.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth/otp/email")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Emissão do token JWT usado pelos demais endpoints")
@SecurityRequirements
public class OtpController {

    private final OtpService otpService;

    @Operation(
            summary = "Envia um código de 6 dígitos para o e-mail",
            description = """
                    Endpoint público. A resposta é a mesma exista conta para o e-mail ou não.
                    Limites: um envio a cada 60 s e no máximo 5 por hora, por e-mail e finalidade.
                    O código vale 10 minutos, aceita 5 tentativas e só pode ser usado uma vez.
                    """)
    @ApiResponse(responseCode = "202", description = "Desafio criado; o código segue por e-mail")
    @ApiResponse(responseCode = "429", description = "Pedido antes do intervalo ou acima do limite; ver Retry-After",
            content = @Content)
    @PostMapping
    public ResponseEntity<DesafioOtpResponse> emitir(@RequestBody @Valid OtpEmailRequest request) {
        DesafioEmitido emitido = otpService.emitir(request.email(), request.finalidade());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new DesafioOtpResponse(emitido.desafioId(), emitido.expiraEm()));
    }

    @Operation(summary = "Confere o código recebido por e-mail")
    @ApiResponse(responseCode = "200", description = "Código correto; o desafio foi consumido")
    @ApiResponse(responseCode = "401", description = "Código incorreto, expirado, já usado ou sem tentativas",
            content = @Content)
    @PostMapping("/verificacao")
    public ResponseEntity<OtpVerificadoResponse> verificar(@RequestBody @Valid VerificarOtpRequest request) {
        Verificacao verificacao = otpService.verificar(request.desafioId(), request.finalidade(), request.codigo());
        return switch (verificacao.status()) {
            case VERIFICADO -> ResponseEntity.ok(new OtpVerificadoResponse(verificacao.destinatario()));
            case CODIGO_INCORRETO -> throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    verificacao.tentativasRestantes() > 0
                            ? "Código incorreto. Tentativas restantes: " + verificacao.tentativasRestantes() + "."
                            : "Código incorreto. Peça um novo código.");
            case DESAFIO_INDISPONIVEL -> throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Código expirado ou já utilizado. Peça um novo código.");
        };
    }
}
