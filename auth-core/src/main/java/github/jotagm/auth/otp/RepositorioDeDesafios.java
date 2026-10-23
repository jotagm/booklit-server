package github.jotagm.auth.otp;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de persistência dos desafios.
 *
 * <p>O contrato que importa está em {@link #reservarTentativa} e {@link #consumir}: os dois
 * precisam ser <b>atômicos</b> no armazenamento. Ler, comparar e depois gravar em passos
 * separados deixa duas requisições paralelas passarem pela mesma última tentativa (ou pelo
 * mesmo código duas vezes). No Postgres é um {@code UPDATE ... WHERE} condicional; no Redis,
 * um script Lua.
 */
public interface RepositorioDeDesafios {

    void salvar(DesafioOtp desafio);

    /**
     * Gasta uma tentativa se o desafio estiver ativo em {@code agora}, numa operação só.
     *
     * @return o desafio já com a tentativa descontada, ou vazio se ele não existe, expirou,
     *         já foi usado ou está sem tentativas
     */
    Optional<DesafioOtp> reservarTentativa(UUID id, Instant agora);

    /**
     * Marca como usado, numa operação só, se ninguém marcou antes.
     *
     * @return {@code true} só para quem consumiu primeiro
     */
    boolean consumir(UUID id, Instant agora);

    /** Para o intervalo mínimo entre reenvios. */
    Optional<Instant> ultimaEmissao(String destinatario, Finalidade finalidade);

    /** Para o teto de emissões por janela. */
    long contarEmissoesDesde(String destinatario, Finalidade finalidade, Instant desde);
}
