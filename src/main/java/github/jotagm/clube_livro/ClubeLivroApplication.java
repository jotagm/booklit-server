package github.jotagm.clube_livro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClubeLivroApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClubeLivroApplication.class, args);
	}

}
