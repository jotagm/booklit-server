package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.FeedItemResponse.TipoFeed;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.FeedItemResponse;
import github.jotagm.clube_livro.adapter.out.persistence.*;
import github.jotagm.clube_livro.configs.AutorizacaoClube;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.leitura.Comentario;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.leitura.Registro;
import github.jotagm.clube_livro.domain.clube.leitura.TipoMeta;
import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus;
import github.jotagm.clube_livro.domain.feed.Reacao;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Feed de atividade dos clubes do usuário.
 *
 * <p>Não existe uma tabela de eventos: o feed é montado na hora a partir do que já é gravado
 * (progressos, comentários, sugestões de livro, votações, leituras e entradas de membros), juntando
 * as fontes e ordenando por data. Cada fonte é limitada ao tamanho da página, então o custo não
 * cresce com o histórico. O {@code id} de cada item é estável (tipo + id do registro de origem) e
 * serve de alvo para as reações.
 *
 * <p>Comentários aparecem como conversa: um item por comentário raiz, com o número de respostas e a
 * última delas. Uma resposta nova traz a conversa de volta ao topo (ordena pela atividade mais
 * recente), mas o horário mostrado continua sendo o do comentário.
 */
@Service
@AllArgsConstructor
public class FeedService {

    private static final int LIMITE_PADRAO = 40;
    private static final int LIMITE_MAXIMO = 100;
    private static final int TRECHO_COMENTARIO = 220;
    private static final int TRECHO_RESPOSTA = 140;
    /** Nomes de quem reagiu que vão no item ("Ana, Carla e você"); o resto o front conta. */
    private static final int MAXIMO_QUEM_REAGIU = 10;

    private final UsuarioClubeRepository usuarioClubeRepository;
    private final RegistroRepository registroRepository;
    private final ComentarioRepository comentarioRepository;
    private final VotacaoRepository votacaoRepository;
    private final LeituraClubeRepository leituraClubeRepository;
    private final OpcaoVotoRepository opcaoVotoRepository;
    private final EstanteService estanteService;
    private final ReacaoService reacaoService;
    private final AutorizacaoClube autorizacaoClube;
    private final FotoPerfilService fotoPerfilService;
    private final Clock relogio;

    /**
     * Um acontecimento antes de virar resposta. {@code atividade} ordena (numa conversa, é a
     * resposta mais recente) e {@code ator} é quem fez essa última coisa — o que o "ponto de
     * novidade" precisa para não contar a própria atividade.
     */
    private record Bruto(String id, TipoFeed tipo, Clube clube, Usuario autor, LocalDateTime quando,
                         LocalDateTime atividade, Integer ator,
                         String livroTitulo, String livroCapaUrl, Integer valorAtual, Integer valorMeta,
                         String unidade, String texto, UUID leituraId,
                         Integer respostas, Comentario ultimaResposta,
                         OpcaoVoto opcao, boolean primeiraDoClube) {

        static Bruto simples(String id, TipoFeed tipo, Clube clube, Usuario autor, LocalDateTime quando,
                             String livroTitulo, String livroCapaUrl, Integer valorAtual, Integer valorMeta,
                             String unidade, String texto, UUID leituraId) {
            return new Bruto(id, tipo, clube, autor, quando, quando, autor == null ? null : autor.getId(),
                    livroTitulo, livroCapaUrl, valorAtual, valorMeta, unidade, texto, leituraId,
                    null, null, null, false);
        }

        Bruto comoPrimeiraDoClube() {
            return new Bruto(id, tipo, clube, autor, quando, atividade, ator, livroTitulo, livroCapaUrl,
                    valorAtual, valorMeta, unidade, texto, leituraId, respostas, ultimaResposta, opcao, true);
        }
    }

    /** O mínimo para o "ponto de novidade": de qual clube, quando e quem fez. */
    public record Atividade(UUID clubeId, LocalDateTime quando, Integer ator) {
    }

    public List<FeedItemResponse> feed(Usuario usuario, UUID clubeId, Integer limite) {
        int tamanho = limite == null ? LIMITE_PADRAO : Math.max(1, Math.min(limite, LIMITE_MAXIMO));

        List<UUID> clubes;
        if (clubeId != null) {
            autorizacaoClube.exigirMembro(usuario.getId(), clubeId);
            clubes = List.of(clubeId);
        } else {
            clubes = clubesDe(usuario);
        }
        if (clubes.isEmpty()) return List.of();

        List<Bruto> pagina1 = coletar(clubes, tamanho);

        Map<String, List<Reacao>> reacoes = reacaoService.porAlvo(pagina1.stream().map(Bruto::id).toList());
        Map<UUID, Long> queremLer = queremLer(pagina1);
        return pagina1.stream().map(b -> responder(b, usuario, reacoes.getOrDefault(b.id(), List.of()), queremLer))
                .toList();
    }

    /** Atividade recente dos clubes, já ordenada, sem reações nem fotos (mais barato que o feed). */
    public List<Atividade> atividades(List<UUID> clubes, int limite) {
        if (clubes.isEmpty()) return List.of();
        return coletar(clubes, Math.max(1, Math.min(limite, LIMITE_MAXIMO))).stream()
                .map(b -> new Atividade(b.clube().getId(), b.atividade(), b.ator()))
                .toList();
    }

    public List<UUID> clubesDe(Usuario usuario) {
        return usuarioClubeRepository.findByUsuarioId(usuario.getId()).stream()
                .map(uc -> uc.getClube().getId())
                .toList();
    }

    private List<Bruto> coletar(List<UUID> clubes, int tamanho) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        Pageable pagina = PageRequest.of(0, tamanho);
        List<Bruto> brutos = new ArrayList<>();

        List<Registro> registros = registroRepository.recentesDosClubes(clubes, pagina);
        Set<UUID> primeirosAConcluir = primeirosAConcluir(registros);
        for (Registro r : registros) {
            LeituraClube l = r.getLeituraClube();
            boolean concluiu = r.getValorAtual() >= l.getValorMeta();
            Bruto b = Bruto.simples("registro:" + r.getId(), concluiu ? TipoFeed.CONCLUIU : TipoFeed.PROGRESSO,
                    l.getClube(), r.getUsuario(), r.getUpdatedAt(), l.getLivroTitulo(), l.getLivroCapaUrl(),
                    r.getValorAtual(), l.getValorMeta(), unidade(l), null, l.getId());
            brutos.add(concluiu && primeirosAConcluir.contains(r.getId()) ? b.comoPrimeiraDoClube() : b);
        }

        brutos.addAll(conversas(comentarioRepository.recentesDosClubes(clubes, pagina)));

        List<OpcaoVoto> opcoes = opcaoVotoRepository.recentesDosClubes(clubes, pagina);
        for (OpcaoVoto o : opcoes) {
            Votacao v = o.getVotacao();
            brutos.add(new Bruto("opcao:" + o.getId(), TipoFeed.OPCAO_SUGERIDA, v.getClube(), o.getSugeridoPor(),
                    o.getCreatedAt(), o.getCreatedAt(), o.getSugeridoPor().getId(),
                    o.getLivroTitulo(), o.getLivroCapaUrl(), null, null, null, null, null,
                    null, null, o, false));
        }
        for (LeituraClube l : leituraClubeRepository.recentesDosClubes(clubes, pagina)) {
            brutos.add(Bruto.simples("leitura:" + l.getId(), TipoFeed.LEITURA_INICIADA, l.getClube(), null,
                    l.getDataInicio(), l.getLivroTitulo(), l.getLivroCapaUrl(), null, l.getValorMeta(),
                    unidade(l), null, l.getId()));
        }
        for (Votacao v : votacaoRepository.recentesDosClubes(clubes, pagina)) {
            brutos.add(Bruto.simples("votacao-aberta:" + v.getId(), TipoFeed.VOTACAO_ABERTA, v.getClube(), null,
                    v.getDataAbertura(), null, null, null, null, null, null, null));
            if (v.getStatus() == VotacaoStatus.ENCERRADA && v.getDataEncerramento() != null) {
                brutos.add(Bruto.simples("votacao-encerrada:" + v.getId(), TipoFeed.VOTACAO_ENCERRADA, v.getClube(),
                        null, v.getDataEncerramento(), null, null, null, null, null, null, null));
            }
        }
        for (UsuarioClube uc : usuarioClubeRepository.entradasRecentes(clubes, pagina)) {
            brutos.add(Bruto.simples("entrou:" + uc.getId(), TipoFeed.MEMBRO_ENTROU, uc.getClube(), uc.getUsuario(),
                    uc.getEntrouEm(), null, null, null, null, null, null, null));
        }

        return brutos.stream()
                .filter(b -> b.quando() != null && !b.quando().isAfter(agora))
                .sorted(Comparator.comparing(Bruto::atividade).reversed())
                .limit(tamanho)
                .toList();
    }

    /**
     * Junta os comentários recentes (raízes e respostas) em conversas: uma por comentário raiz, na
     * ordem da atividade mais recente, com a contagem e a última resposta. Raiz removida some.
     */
    private List<Bruto> conversas(List<Comentario> recentes) {
        Map<UUID, Comentario> raizes = new LinkedHashMap<>();
        Map<UUID, Comentario> maisRecente = new HashMap<>();
        for (Comentario c : recentes) {
            Comentario raiz = c.getComentarioPai() == null ? c : c.getComentarioPai();
            if (raiz.isRemovido()) continue;
            raizes.putIfAbsent(raiz.getId(), raiz);
            maisRecente.putIfAbsent(raiz.getId(), c);
        }
        if (raizes.isEmpty()) return List.of();

        Map<UUID, List<Comentario>> respostas = comentarioRepository
                .findByComentarioPaiIdInAndRemovidoFalseOrderByCreatedAtAsc(raizes.keySet()).stream()
                .collect(Collectors.groupingBy(c -> c.getComentarioPai().getId()));

        List<Bruto> conversas = new ArrayList<>();
        for (Comentario raiz : raizes.values()) {
            LeituraClube l = raiz.getLeituraClube();
            List<Comentario> deste = respostas.getOrDefault(raiz.getId(), List.of());
            Comentario ultima = deste.isEmpty() ? null : deste.get(deste.size() - 1);
            Comentario recente = maisRecente.get(raiz.getId());
            conversas.add(new Bruto("comentario:" + raiz.getId(), TipoFeed.COMENTARIO, l.getClube(), raiz.getUsuario(),
                    raiz.getCreatedAt(), recente.getCreatedAt(), recente.getUsuario().getId(),
                    // valorAtual = onde o autor estava ao comentar ("· pág. 96" no card).
                    l.getLivroTitulo(), l.getLivroCapaUrl(), raiz.getPaginaAutor(), null,
                    raiz.getPaginaAutor() == null ? null : unidade(l), trecho(raiz.getConteudo(), TRECHO_COMENTARIO),
                    l.getId(), deste.size(), ultima, null, false));
        }
        return conversas;
    }

    /**
     * Registro concluído que foi o primeiro do clube a terminar aquela leitura. Um registro
     * concluído não muda mais (não passa da meta nem volta), então o {@code updatedAt} dele é o
     * momento em que terminou.
     */
    private Set<UUID> primeirosAConcluir(List<Registro> registros) {
        Set<UUID> leituras = registros.stream()
                .filter(r -> r.getValorAtual() >= r.getLeituraClube().getValorMeta())
                .map(r -> r.getLeituraClube().getId())
                .collect(Collectors.toSet());
        Set<UUID> primeiros = new HashSet<>();
        for (UUID leituraId : leituras) {
            registroRepository.findByLeituraClubeId(leituraId).stream()
                    .filter(r -> r.getValorAtual() >= r.getLeituraClube().getValorMeta() && r.getUpdatedAt() != null)
                    .min(Comparator.comparing(Registro::getUpdatedAt))
                    .ifPresent(r -> primeiros.add(r.getId()));
        }
        return primeiros;
    }

    /** "2 membros querem ler" das sugestões da página, por id da opção. */
    private Map<UUID, Long> queremLer(List<Bruto> pagina) {
        List<OpcaoVoto> opcoes = pagina.stream().map(Bruto::opcao).filter(Objects::nonNull).toList();
        if (opcoes.isEmpty()) return Map.of();
        Map<UUID, Long> resultado = new HashMap<>();
        opcoes.stream()
                .collect(Collectors.groupingBy(o -> o.getVotacao().getClube().getId()))
                .forEach((clubeId, doClube) -> {
                    List<Integer> membros = usuarioClubeRepository.findByClubeId(clubeId).stream()
                            .map(uc -> uc.getUsuario().getId())
                            .toList();
                    Map<String, Long> porLivro = estanteService.contarQueroLer(
                            membros, doClube.stream().map(OpcaoVoto::getLivroGoogleId).distinct().toList());
                    doClube.forEach(o -> resultado.put(o.getId(), porLivro.getOrDefault(o.getLivroGoogleId(), 0L)));
                });
        return resultado;
    }

    private FeedItemResponse responder(Bruto b, Usuario usuario, List<Reacao> reacoes, Map<UUID, Long> queremLer) {
        Usuario a = b.autor();
        Comentario r = b.ultimaResposta();
        OpcaoVoto o = b.opcao();
        FeedItemResponse.Resposta ultima = r == null ? null : new FeedItemResponse.Resposta(
                r.getUsuario().getId(), r.getUsuario().getNome(), fotoPerfilService.urlDe(r.getUsuario()),
                trecho(r.getConteudo(), TRECHO_RESPOSTA));
        return new FeedItemResponse(b.id(), b.tipo(), b.clube().getId(), b.clube().getNome(),
                a == null ? null : a.getId(), a == null ? null : a.getNome(),
                a == null ? null : fotoPerfilService.urlDe(a),
                a != null && a.getId().equals(usuario.getId()), b.quando(),
                b.livroTitulo(), b.livroCapaUrl(), b.valorAtual(), b.valorMeta(), b.unidade(), b.texto(),
                b.leituraId(), ReacaoService.contar(reacoes), ReacaoService.minha(reacoes, usuario.getId()),
                quemReagiu(reacoes, usuario),
                b.respostas(), ultima,
                o == null ? null : o.getVotacao().getId(),
                o == null ? null : o.getVotacao().getStatus() == VotacaoStatus.ABERTA,
                o == null ? null : queremLer.getOrDefault(o.getId(), 0L),
                b.primeiraDoClube());
    }

    /** Quem reagiu, mais antigo primeiro e você por último ("Ana, Carla e você"). */
    private static List<FeedItemResponse.Pessoa> quemReagiu(List<Reacao> reacoes, Usuario usuario) {
        return reacoes.stream()
                .sorted(Comparator
                        .comparing((Reacao r) -> r.getUsuario().getId().equals(usuario.getId()))
                        .thenComparing(r -> r.getCreatedAt() == null ? LocalDateTime.MIN : r.getCreatedAt()))
                .limit(MAXIMO_QUEM_REAGIU)
                .map(r -> new FeedItemResponse.Pessoa(r.getUsuario().getId(), r.getUsuario().getNome()))
                .toList();
    }

    private static String unidade(LeituraClube l) {
        return l.getTipoMeta() == TipoMeta.CAPITULO ? "capítulos" : "páginas";
    }

    private static String trecho(String conteudo, int tamanho) {
        if (conteudo == null) return "";
        String limpo = conteudo.strip();
        return limpo.length() <= tamanho ? limpo : limpo.substring(0, tamanho).strip() + "…";
    }
}
