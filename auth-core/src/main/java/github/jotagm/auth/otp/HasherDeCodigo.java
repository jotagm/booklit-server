package github.jotagm.auth.otp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

/**
 * HMAC-SHA256 do código, amarrado ao desafio e à finalidade.
 *
 * <p>Por que não guardar o código e nem um SHA puro: com só 10^6 valores possíveis, um
 * vazamento do banco quebraria um SHA-256 por força bruta em milissegundos. Com HMAC, sem a
 * chave do servidor o hash não serve para nada. E por entrar o id do desafio e a finalidade
 * na mensagem, o mesmo código em dois desafios gera hashes diferentes.
 */
public final class HasherDeCodigo {

    private static final String ALGORITMO = "HmacSHA256";
    private static final int TAMANHO_MINIMO_CHAVE = 32;

    private final SecretKeySpec chave;

    public HasherDeCodigo(byte[] chave) {
        if (chave == null || chave.length < TAMANHO_MINIMO_CHAVE) {
            throw new IllegalArgumentException("A chave do HMAC precisa de pelo menos 32 bytes");
        }
        this.chave = new SecretKeySpec(chave.clone(), ALGORITMO);
    }

    public String hash(UUID desafioId, Finalidade finalidade, String codigo) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(chave);
            String mensagem = desafioId + "|" + finalidade.name() + "|" + codigo;
            return Base64.getEncoder().encodeToString(mac.doFinal(mensagem.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 indisponível na JVM", e);
        }
    }

    /** Comparação em tempo constante: o tempo de resposta não revela quantos bytes bateram. */
    public boolean confere(String hashEsperado, UUID desafioId, Finalidade finalidade, String codigo) {
        byte[] esperado = hashEsperado.getBytes(StandardCharsets.US_ASCII);
        byte[] calculado = hash(desafioId, finalidade, codigo).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(esperado, calculado);
    }
}
