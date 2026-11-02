package github.jotagm.clube_livro.adapter.out.storage;

import github.jotagm.clube_livro.application.port.out.ArmazenamentoDeFotos;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Optional;

/**
 * Fotos de perfil num bucket S3 privado. Não loga chave, URL nem nada que identifique o
 * conteúdo: a URL assinada é uma credencial de curta duração.
 */
public class S3ArmazenamentoDeFotos implements ArmazenamentoDeFotos {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration validadeEnvio;
    private final Duration validadeLeitura;

    public S3ArmazenamentoDeFotos(S3Client s3, S3Presigner presigner, String bucket,
                                  Duration validadeEnvio, Duration validadeLeitura) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("booklit.fotos.bucket é obrigatório");
        }
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket.strip();
        this.validadeEnvio = validadeEnvio;
        this.validadeLeitura = validadeLeitura;
    }

    @Override
    public boolean habilitado() {
        return true;
    }

    @Override
    public EnvioAssinado prepararEnvio(String chave, String contentType, long tamanho) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(chave)
                .contentType(contentType)
                .contentLength(tamanho)
                .build();
        var assinado = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(validadeEnvio)
                .putObjectRequest(put)
                .build());
        return new EnvioAssinado(chave, assinado.url().toString(), assinado.expiration());
    }

    @Override
    public Optional<Objeto> inspecionar(String chave) {
        try {
            HeadObjectResponse resposta = s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(chave).build());
            return Optional.of(new Objeto(resposta.contentLength(), resposta.contentType()));
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public String urlDeLeitura(String chave) {
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(validadeLeitura)
                        .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(chave).build())
                        .build())
                .url().toString();
    }

    @Override
    public void apagar(String chave) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(chave).build());
    }
}
