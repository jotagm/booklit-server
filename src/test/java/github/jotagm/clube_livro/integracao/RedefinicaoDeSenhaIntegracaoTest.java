package github.jotagm.clube_livro.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.jotagm.clube_livro.adapter.out.persistence.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.containsString;

/**
 * "Esqueci minha senha" ponta a ponta: pedido do código, troca da senha, token novo, senha
 * antiga recusada, sessões antigas derrubadas e resposta idêntica para e-mail sem conta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:senha;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1hcGVuYXMtcGFyYS1vLWNvbnRleHRvLWRlLXRlc3Rlcw==",
        "google.books.api-key=chave-de-teste",
        "booklit.auth.email.habilitado=true",
        "booklit.auth.otp.chave-hmac=Y2hhdmUtZGUtdGVzdGUtZG8taG1hYy1jb20tMzItYnl0ZXMhIQ=="
})
class RedefinicaoDeSenhaIntegracaoTest {

    private static final Pattern CODIGO = Pattern.compile("\\b(\\d{6})\\b");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private UsuarioRepository usuarioRepository;
    @MockitoBean private RestClient restClient;
    @MockitoBean private SesV2Client ses;

    private String email;

    @BeforeEach
    void preparar() throws Exception {
        when(ses.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("msg-teste").build());
        email = "senha-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.dev";
        mockMvc.perform(post("/usuarios").contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa\",\"email\":\"" + email + "\",\"senha\":\"senha-antiga-1\",\"aceitouTermos\":true,\"maiorDeIdade\":true}"))
                .andExpect(status().isCreated());
        clearInvocations(ses);
    }

    @Test
    void codigoCertoTrocaASenhaEntraLogadoEDerrubaAsSessoesAntigas() throws Exception {
        String tokenAntigo = login(email, "senha-antiga-1", 200);
        mockMvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + tokenAntigo)).andExpect(status().isOk());
        // O iat do JWT tem segundos inteiros: sem esta pausa o token antigo e a troca caem no mesmo segundo.
        Thread.sleep(1100);

        String desafioId = esqueci(email);
        SendEmailRequest enviado = ultimoEnvio();
        assertEquals("Código para redefinir sua senha no Booklit", enviado.content().simple().subject().data());
        String codigo = codigoDe(enviado);

        String resposta = mockMvc.perform(post("/auth/senha/redefinir").contentType(APPLICATION_JSON)
                        .content(redefinir(desafioId, codigo, "senha-nova-2")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tokenNovo = json.readTree(resposta).get("token").asText();
        assertFalse(tokenNovo.isBlank());

        mockMvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + tokenNovo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        mockMvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + tokenAntigo))
                .andExpect(status().is4xxClientError());

        login(email, "senha-antiga-1", 401);
        login(email, "senha-nova-2", 200);
        assertTrue(usuarioRepository.findByEmailIgnoreCase(email).orElseThrow().emailVerificado(),
                "o código provou a caixa de entrada");

        // Uso único: o mesmo código não troca a senha de novo.
        mockMvc.perform(post("/auth/senha/redefinir").contentType(APPLICATION_JSON)
                        .content(redefinir(desafioId, codigo, "outra-senha-3")))
                .andExpect(status().isUnauthorized());
        login(email, "senha-nova-2", 200);
    }

    @Test
    void codigoErradoNaoTrocaASenhaEAvisaAsTentativas() throws Exception {
        String desafioId = esqueci(email);
        String codigo = codigoDe(ultimoEnvio());
        String errado = codigo.equals("000000") ? "111111" : "000000";

        mockMvc.perform(post("/auth/senha/redefinir").contentType(APPLICATION_JSON)
                        .content(redefinir(desafioId, errado, "senha-nova-2")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem", containsString("Tentativas restantes: 4")));
        login(email, "senha-antiga-1", 200);
    }

    @Test
    void emailSemContaRecebeAMesmaRespostaENadaEEnviado() throws Exception {
        esqueci("ninguem-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.dev");
        verify(ses, never()).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void codigoDeOutraFinalidadeNaoServeParaTrocarSenha() throws Exception {
        String resposta = mockMvc.perform(post("/auth/otp/email").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"finalidade\":\"VERIFICAR_EMAIL\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String desafioId = json.readTree(resposta).get("desafioId").asText();
        String codigo = codigoDe(ultimoEnvio());

        mockMvc.perform(post("/auth/senha/redefinir").contentType(APPLICATION_JSON)
                        .content(redefinir(desafioId, codigo, "senha-nova-2")))
                .andExpect(status().isUnauthorized());
        login(email, "senha-antiga-1", 200);
    }

    @Test
    void senhaNovaCurtaEhRecusadaAntesDeGastarOCodigo() throws Exception {
        String desafioId = esqueci(email);
        String codigo = codigoDe(ultimoEnvio());

        mockMvc.perform(post("/auth/senha/redefinir").contentType(APPLICATION_JSON)
                        .content(redefinir(desafioId, codigo, "curta")))
                .andExpect(status().isBadRequest());
        // O código continua valendo: a validação barrou antes da verificação.
        mockMvc.perform(post("/auth/senha/redefinir").contentType(APPLICATION_JSON)
                        .content(redefinir(desafioId, codigo, "senha-nova-2")))
                .andExpect(status().isOk());
    }

    private String esqueci(String para) throws Exception {
        String resposta = mockMvc.perform(post("/auth/senha/esqueci").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + para + "\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        JsonNode corpo = json.readTree(resposta);
        assertEquals(2, corpo.size(), "a resposta não pode variar entre quem tem e quem não tem conta");
        return corpo.get("desafioId").asText();
    }

    private String login(String email, String senha, int esperado) throws Exception {
        String resposta = mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().is(esperado))
                .andReturn().getResponse().getContentAsString();
        return esperado == 200 ? json.readTree(resposta).get("token").asText() : null;
    }

    private SendEmailRequest ultimoEnvio() {
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(ses, times(1)).sendEmail(captor.capture());
        return captor.getValue();
    }

    private static String codigoDe(SendEmailRequest enviado) {
        Matcher m = CODIGO.matcher(enviado.content().simple().body().text().data());
        assertTrue(m.find(), "corpo sem código de 6 dígitos");
        return m.group(1);
    }

    private static String redefinir(String desafioId, String codigo, String novaSenha) {
        return "{\"desafioId\":\"" + desafioId + "\",\"codigo\":\"" + codigo + "\",\"novaSenha\":\"" + novaSenha + "\"}";
    }
}
