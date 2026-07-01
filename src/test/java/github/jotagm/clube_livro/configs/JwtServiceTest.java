package github.jotagm.clube_livro.configs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "o23igTyFrqVJoZCu59Zm91ZmYq9lsSjSAqIdq95wQYq1admlpZXfh+6yNSt66s4SIGa/LP3lNSZWB7hEgST9dQ==";
    private static final long EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);
    }

    @Test
    void gerarToken_deveRetornarTokenNaoNulo() {
        String token = jwtService.gerarToken("usuario@email.com");

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extrairEmail_deveRetornarEmailCorretoDentroDoToken() {
        String token = jwtService.gerarToken("usuario@email.com");

        String emailExtraido = jwtService.extrairEmail(token);

        assertThat(emailExtraido).isEqualTo("usuario@email.com");
    }

    @Test
    void isTokenValido_deveRetornarTrueParaTokenValidoComEmailCorreto() {
        String token = jwtService.gerarToken("usuario@email.com");

        boolean valido = jwtService.isTokenValido(token, "usuario@email.com");

        assertThat(valido).isTrue();
    }

    @Test
    void isTokenValido_deveRetornarFalseParaEmailDivergente() {
        String token = jwtService.gerarToken("usuario@email.com");

        boolean valido = jwtService.isTokenValido(token, "outro@email.com");

        assertThat(valido).isFalse();
    }

    @Test
    void isTokenValido_deveRetornarFalseParaTokenExpirado() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.gerarToken("usuario@email.com");

        assertThatThrownBy(() -> jwtService.isTokenValido(token, "usuario@email.com"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void gerarToken_diferentesEmailsDevemGerarTokensDiferentes() {
        String token1 = jwtService.gerarToken("a@email.com");
        String token2 = jwtService.gerarToken("b@email.com");

        assertThat(token1).isNotEqualTo(token2);
    }
}
