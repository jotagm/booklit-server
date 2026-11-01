package github.jotagm.clube_livro.adapter.in.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * O e-mail cuja posse acabou de ser provada. {@code token} só aparece na primeira verificação
 * de e-mail de uma conta (o cadastro), para entrar logado; nas demais fica de fora do JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtpVerificadoResponse(String email, String token) {
}
