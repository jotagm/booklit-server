package github.jotagm.clube_livro;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

@SpringBootTest
@ActiveProfiles("test")
class ClubeLivroApplicationTests {

	// O RestClient real abre um socket na criação do bean; o contexto não depende dele.
	@MockitoBean
	private RestClient restClient;

	@Test
	void contextLoads() {
	}

}
