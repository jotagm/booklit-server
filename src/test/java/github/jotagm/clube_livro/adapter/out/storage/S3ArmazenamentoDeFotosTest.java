package github.jotagm.clube_livro.adapter.out.storage;

import github.jotagm.clube_livro.application.port.out.ArmazenamentoDeFotos.EnvioAssinado;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A assinatura de URL é local (não chama a AWS), então dá para testar sem rede e sem Floci:
 * credenciais fixas, e só se confere o formato do que sai.
 */
class S3ArmazenamentoDeFotosTest {

    private S3ArmazenamentoDeFotos armazenamento() {
        var credenciais = StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIATESTE", "segredo-de-teste"));
        var s3 = S3Client.builder().region(Region.US_EAST_1).credentialsProvider(credenciais).build();
        var presigner = S3Presigner.builder().region(Region.US_EAST_1).credentialsProvider(credenciais).build();
        return new S3ArmazenamentoDeFotos(s3, presigner, "booklit-fotos-teste",
                Duration.ofMinutes(10), Duration.ofHours(1));
    }

    @Test
    void urlDeEnvioApontaParaOBucketEAChaveEAssinaOContentType() {
        EnvioAssinado envio = armazenamento().prepararEnvio("fotos/7/a.png", "image/png", 1234);

        assertEquals("fotos/7/a.png", envio.chave());
        // us-east-1 usa o endereço global (s3.amazonaws.com), sem o nome da região.
        assertTrue(envio.url().startsWith("https://booklit-fotos-teste.s3."), envio.url());
        assertTrue(envio.url().contains(".amazonaws.com/fotos/7/a.png?"), envio.url());
        assertTrue(envio.url().contains("X-Amz-Signature="), envio.url());
        // Tipo e tamanho assinados: o S3 recusa um envio diferente do que foi pedido.
        String url = envio.url().toLowerCase();
        assertTrue(url.contains("content-type"), "o Content-Type precisa estar assinado: " + envio.url());
        assertTrue(url.contains("content-length"), "o Content-Length precisa estar assinado: " + envio.url());
        assertTrue(envio.expiraEm().isAfter(java.time.Instant.now()));
    }

    @Test
    void urlDeLeituraTemAssinaturaEValidadeCurta() {
        String url = armazenamento().urlDeLeitura("fotos/7/a.png");

        assertTrue(url.startsWith("https://booklit-fotos-teste.s3."), url);
        assertTrue(url.contains(".amazonaws.com/fotos/7/a.png?"), url);
        assertTrue(url.contains("X-Amz-Signature="), url);
        assertTrue(url.contains("X-Amz-Expires=3600"), url);
    }

    @Test
    void bucketVazioFalhaNaSubida() {
        var s3 = S3Client.builder().region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("a", "b"))).build();
        var presigner = S3Presigner.builder().region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("a", "b"))).build();

        assertThrows(IllegalArgumentException.class,
                () -> new S3ArmazenamentoDeFotos(s3, presigner, " ", Duration.ofMinutes(1), Duration.ofMinutes(1)));
    }
}
