package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.UsuarioRepository;
import github.jotagm.clube_livro.application.port.out.ArmazenamentoDeFotos.EnvioAssinado;
import github.jotagm.clube_livro.application.port.out.ArmazenamentoDeFotos;
import github.jotagm.clube_livro.domain.exceptions.AcessoNegadoException;
import github.jotagm.clube_livro.domain.exceptions.RegraDeNegocioException;
import github.jotagm.clube_livro.domain.exceptions.ServicoIndisponivelException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Foto de perfil em três passos: o cliente pede um envio, manda a imagem direto ao S3 e
 * confirma. Só na confirmação a chave passa a valer, e só se o objeto existir, for de uma
 * imagem aceita e couber no tamanho máximo (o cliente pode mentir no pedido; o S3 não).
 */
@Service
public class FotoPerfilService {

    private static final Logger log = LoggerFactory.getLogger(FotoPerfilService.class);

    /** Tipos aceitos -> extensão da chave. */
    static final Map<String, String> TIPOS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    static final int TAMANHO_MAXIMO_CHAVE = 200;

    private final ArmazenamentoDeFotos armazenamento;
    private final UsuarioRepository usuarioRepository;
    private final long tamanhoMaximo;

    public FotoPerfilService(ArmazenamentoDeFotos armazenamento, UsuarioRepository usuarioRepository,
                             @Value("${booklit.fotos.tamanho-maximo:2097152}") long tamanhoMaximo) {
        this.armazenamento = armazenamento;
        this.usuarioRepository = usuarioRepository;
        this.tamanhoMaximo = tamanhoMaximo;
    }

    public EnvioAssinado solicitarEnvio(Usuario usuario, String contentType, long tamanho) {
        exigirHabilitado();
        String tipo = contentType == null ? "" : contentType.strip().toLowerCase(Locale.ROOT);
        String extensao = TIPOS.get(tipo);
        if (extensao == null) {
            throw new RegraDeNegocioException("Formato não aceito. Use JPEG, PNG ou WebP.");
        }
        if (tamanho <= 0 || tamanho > tamanhoMaximo) {
            throw new RegraDeNegocioException("A foto precisa ter até " + (tamanhoMaximo / (1024 * 1024)) + " MB.");
        }
        String chave = prefixo(usuario) + UUID.randomUUID() + "." + extensao;
        return armazenamento.prepararEnvio(chave, tipo, tamanho);
    }

    public Usuario confirmar(Usuario usuario, String chave) {
        exigirHabilitado();
        if (chave == null || chave.length() > TAMANHO_MAXIMO_CHAVE || chave.contains("..")
                || !chave.startsWith(prefixo(usuario))) {
            // Chave de outra pessoa (ou lixo): nunca confirma, mesmo que o objeto exista.
            throw new AcessoNegadoException("Foto não pertence a este usuário");
        }
        var objeto = armazenamento.inspecionar(chave)
                .orElseThrow(() -> new RegraDeNegocioException(
                        "A imagem ainda não foi enviada. Envie a foto e confirme de novo."));
        if (objeto.tamanho() > tamanhoMaximo || !TIPOS.containsKey(objeto.contentType())) {
            apagarSemFalhar(chave);
            throw new RegraDeNegocioException("Imagem recusada: use JPEG, PNG ou WebP de até "
                    + (tamanhoMaximo / (1024 * 1024)) + " MB.");
        }

        String antiga = usuario.getFotoChave();
        usuario.definirFoto(chave);
        Usuario salvo = usuarioRepository.save(usuario);
        if (antiga != null && !antiga.equals(chave)) {
            apagarSemFalhar(antiga);
        }
        return salvo;
    }

    public Usuario remover(Usuario usuario) {
        String antiga = usuario.getFotoChave();
        if (antiga == null) {
            return usuario;
        }
        usuario.removerFoto();
        Usuario salvo = usuarioRepository.save(usuario);
        apagarSemFalhar(antiga);
        return salvo;
    }

    /** URL de leitura da foto, ou nulo quando a pessoa não tem foto ou o S3 não está ligado. */
    public String urlDe(Usuario usuario) {
        String chave = usuario.getFotoChave();
        if (chave == null || !armazenamento.habilitado()) {
            return null;
        }
        return armazenamento.urlDeLeitura(chave);
    }

    private static String prefixo(Usuario usuario) {
        return "fotos/" + usuario.getId() + "/";
    }

    private void exigirHabilitado() {
        if (!armazenamento.habilitado()) {
            throw new ServicoIndisponivelException("Foto de perfil indisponível no momento");
        }
    }

    /** Sobra de objeto no bucket é só custo de armazenamento: não pode derrubar a troca de foto. */
    private void apagarSemFalhar(String chave) {
        try {
            armazenamento.apagar(chave);
        } catch (RuntimeException e) {
            log.warn("Não foi possível apagar uma foto antiga do bucket: {}", e.toString());
        }
    }
}
