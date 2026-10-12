package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.FeedItemResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.FeedItemResponse.TipoFeed;
import github.jotagm.clube_livro.adapter.out.persistence.*;
import github.jotagm.clube_livro.configs.AutorizacaoClube;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.leitura.Comentario;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.leitura.Registro;
import github.jotagm.clube_livro.domain.clube.leitura.TipoMeta;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus;
import github.jotagm.clube_livro.domain.feed.Reacao;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Feed de atividade dos clubes do usuário.
 *
 * <p>Não existe uma tabela de eventos: o feed é montado na hora a partir do que já é gravado
 * (progressos, comentários, votações, leituras e entradas de membros), juntando as fontes e
 * ordenando por data. Cada fonte é limitada ao tamanho da página, então o custo não cresce com o
 * histórico. O {@code id} de cada item é estável (tipo + id do registro de origem) e serve de alvo
 * para as reações.
 */
@Service
@AllArgsConstructor
public class FeedService {

    private static final int LIMITE_PADRAO = 40;
    private static final int LIMITE_MAXIMO = 100;
    private static final int TRECHO_COMENTARIO = 220;

    private final UsuarioClubeRepository usuarioClubeRepository;
    private final RegistroRepository registroRepository;
    private final ComentarioRepository comentarioRepository;
    private final VotacaoRepository votacaoRepository;
    private final LeituraClubeRepository leituraClubeRepository;
    private final ReacaoService reacaoService;
    private final AutorizacaoClube autorizacaoClube;

    /** Itens sem reações montadas ainda — o mesmo contêiner serve para ordenar antes de consultar reações. */
    private record Bruto(String id, TipoFeed tipo, Clube clube, Usuario autor, LocalDateTime quando,
                         String livroTitulo, String livroCapaUrl, Integer valorAtual, Integer valorMeta,
                         String unidade, String texto, UUID leituraId) {}

    public List<FeedItemResponse> feed(Usuario usuario, UUID clubeId, Integer limite) {
        int tamanho = limite == null ? LIMITE_PADRAO : Math.max(1, Math.min(limite, LIMITE_MAXIMO));

        List<UUID> clubes;
        if (clubeId != null) {
            autorizacaoClube.exigirMembro(usuario.getId(), clubeId);
            clubes = List.of(clubeId);
        } else {
            clubes = usuarioClubeRepository.findByUsuarioId(usuario.getId()).stream()
                    .map(uc -> uc.getClube().getId())
                    .toList();
        }
        if (clubes.isEmpty()) return List.of();

        LocalDateTime agora = LocalDateTime.now();
        Pageable pagina = PageRequest.of(0, tamanho);
        List<Bruto> brutos = new ArrayList<>();

        for (Registro r : registroRepository.recentesDosClubes(clubes, pagina)) {
            LeituraClube l = r.getLeituraClube();
            boolean concluiu = r.getValorAtual() >= l.getValorMeta();
            brutos.add(new Bruto("registro:" + r.getId(), concluiu ? TipoFeed.CONCLUIU : TipoFeed.PROGRESSO,
                    l.getClube(), r.getUsuario(), r.getUpdatedAt(), l.getLivroTitulo(), l.getLivroCapaUrl(),
                    r.getValorAtual(), l.getValorMeta(), unidade(l), null, l.getId()));
        }
        for (Comentario c : comentarioRepository.recentesDosClubes(clubes, pagina)) {
            LeituraClube l = c.getLeituraClube();
            brutos.add(new Bruto("comentario:" + c.getId(), TipoFeed.COMENTARIO, l.getClube(), c.getUsuario(),
                    c.getCreatedAt(), l.getLivroTitulo(), l.getLivroCapaUrl(), null, null, null,
                    trecho(c.getConteudo()), l.getId()));
        }
        for (LeituraClube l : leituraClubeRepository.recentesDosClubes(clubes, pagina)) {
            brutos.add(new Bruto("leitura:" + l.getId(), TipoFeed.LEITURA_INICIADA, l.getClube(), null,
                    l.getDataInicio(), l.getLivroTitulo(), l.getLivroCapaUrl(), null, l.getValorMeta(),
                    unidade(l), null, l.getId()));
        }
        for (Votacao v : votacaoRepository.recentesDosClubes(clubes, pagina)) {
            brutos.add(new Bruto("votacao-aberta:" + v.getId(), TipoFeed.VOTACAO_ABERTA, v.getClube(), null,
                    v.getDataAbertura(), null, null, null, null, null, null, null));
            if (v.getStatus() == VotacaoStatus.ENCERRADA && v.getDataEncerramento() != null) {
                brutos.add(new Bruto("votacao-encerrada:" + v.getId(), TipoFeed.VOTACAO_ENCERRADA, v.getClube(),
                        null, v.getDataEncerramento(), null, null, null, null, null, null, null));
            }
        }
        for (UsuarioClube uc : usuarioClubeRepository.entradasRecentes(clubes, pagina)) {
            brutos.add(new Bruto("entrou:" + uc.getId(), TipoFeed.MEMBRO_ENTROU, uc.getClube(), uc.getUsuario(),
                    uc.getEntrouEm(), null, null, null, null, null, null, null));
        }

        List<Bruto> pagina1 = brutos.stream()
                .filter(b -> b.quando() != null && !b.quando().isAfter(agora))
                .sorted(Comparator.comparing(Bruto::quando).reversed())
                .limit(tamanho)
                .toList();

        Map<String, List<Reacao>> reacoes = reacaoService.porAlvo(pagina1.stream().map(Bruto::id).toList());
        return pagina1.stream().map(b -> {
            List<Reacao> deste = reacoes.getOrDefault(b.id(), List.of());
            Usuario a = b.autor();
            return new FeedItemResponse(b.id(), b.tipo(), b.clube().getId(), b.clube().getNome(),
                    a == null ? null : a.getId(), a == null ? null : a.getNome(),
                    a != null && a.getId().equals(usuario.getId()), b.quando(),
                    b.livroTitulo(), b.livroCapaUrl(), b.valorAtual(), b.valorMeta(), b.unidade(), b.texto(),
                    b.leituraId(), ReacaoService.contar(deste), ReacaoService.minha(deste, usuario.getId()));
        }).toList();
    }

    private static String unidade(LeituraClube l) {
        return l.getTipoMeta() == TipoMeta.CAPITULO ? "capítulos" : "páginas";
    }

    private static String trecho(String conteudo) {
        if (conteudo == null) return "";
        String limpo = conteudo.strip();
        return limpo.length() <= TRECHO_COMENTARIO ? limpo : limpo.substring(0, TRECHO_COMENTARIO).strip() + "…";
    }
}
