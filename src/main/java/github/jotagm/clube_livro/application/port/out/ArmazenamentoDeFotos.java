package github.jotagm.clube_livro.application.port.out;

import java.time.Instant;
import java.util.Optional;

/**
 * Onde ficam as fotos de perfil (S3). O navegador e o app enviam a imagem direto ao
 * armazenamento por uma URL assinada de curta duração; a API só decide quem pode enviar o quê
 * e guarda a chave. Assim a imagem não passa pela API nem pelo banco.
 */
public interface ArmazenamentoDeFotos {

    /** Pedido de envio já assinado: o cliente faz um PUT na {@code url} com o mesmo Content-Type. */
    record EnvioAssinado(String chave, String url, Instant expiraEm) {
    }

    /** O que o armazenamento diz de um objeto que já está lá. */
    record Objeto(long tamanho, String contentType) {
    }

    /** Falso quando o bucket não está configurado (dev sem S3): o perfil segue sem foto. */
    boolean habilitado();

    /** A assinatura inclui Content-Type e Content-Length, então o cliente não consegue mandar outra coisa. */
    EnvioAssinado prepararEnvio(String chave, String contentType, long tamanho);

    /** Vazio quando a chave não existe (o envio não aconteceu ou expirou). */
    Optional<Objeto> inspecionar(String chave);

    /** URL assinada de leitura, de vida curta: o bucket é privado. */
    String urlDeLeitura(String chave);

    void apagar(String chave);
}
