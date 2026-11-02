package github.jotagm.clube_livro.adapter.out.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.jotagm.clube_livro.application.port.out.EnviadorDeLembreteDeVotacao.Mensagem;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientGoEnviadorDeLembreteDeVotacaoTest {

    private static final String TOKEN = "t".repeat(40);
    private static final ObjectMapper JSON = new ObjectMapper();

    private ClientGoEnviadorDeLembreteDeVotacao enviador(String frontUrl) {
        return new ClientGoEnviadorDeLembreteDeVotacao(JSON, "http://localhost:8090", TOKEN, Duration.ofSeconds(1), frontUrl);
    }

    @Test
    void corpoLevaTemplateLinkDaVotacaoEPrazoEmPortugues() throws Exception {
        UUID votacaoId = UUID.fromString("00000000-0000-0000-0000-000000000042");
        Mensagem m = new Mensagem("chave", votacaoId, "ana@teste.dev", "Leitoras\nde Domingo",
                LocalDateTime.of(2026, 11, 4, 20, 0));

        JsonNode corpo = JSON.readTree(enviador("https://booklit.bio/").corpo(m));

        assertEquals("lembrete-votacao", corpo.get("template").asText());
        assertEquals("ana@teste.dev", corpo.get("destinatario").asText());
        JsonNode v = corpo.get("variaveis");
        assertEquals("https://booklit.bio/votacoes/" + votacaoId, v.get("link").asText());
        // Nome do clube é texto livre: perde a quebra de linha antes de ir para o e-mail.
        assertEquals("Leitoras de Domingo", v.get("clube").asText());
        assertEquals("quarta-feira, 4 de novembro, às 20:00", v.get("prazo").asText());
        assertEquals(3, v.size());
    }

    @Test
    void recusaConfiguracaoRuim() {
        assertThrows(IllegalArgumentException.class, () -> enviador("ftp://booklit.bio"));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientGoEnviadorDeLembreteDeVotacao(JSON, "http://localhost:8090", "curto", Duration.ofSeconds(1), "https://booklit.bio"));
    }
}
