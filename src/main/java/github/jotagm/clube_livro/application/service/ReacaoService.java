package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.ReacoesResponse;
import github.jotagm.clube_livro.adapter.out.persistence.ReacaoRepository;
import github.jotagm.clube_livro.configs.AutorizacaoClube;
import github.jotagm.clube_livro.domain.feed.Reacao;
import github.jotagm.clube_livro.domain.feed.TipoReacao;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reações aos itens do feed: uma por pessoa por item, só entre membros do clube. */
@Service
@AllArgsConstructor
public class ReacaoService {

    private final ReacaoRepository reacaoRepository;
    private final AutorizacaoClube autorizacaoClube;

    @Transactional
    public ReacoesResponse reagir(Usuario usuario, UUID clubeId, String alvoId, TipoReacao tipo) {
        autorizacaoClube.exigirMembro(usuario.getId(), clubeId);
        reacaoRepository.findByAlvoIdAndUsuarioId(alvoId, usuario.getId()).ifPresentOrElse(
                existente -> {
                    existente.trocarPara(tipo);
                    reacaoRepository.save(existente);
                },
                () -> reacaoRepository.save(Reacao.de(clubeId, alvoId, usuario, tipo)));
        return resumo(alvoId, usuario.getId());
    }

    @Transactional
    public ReacoesResponse remover(Usuario usuario, String alvoId) {
        reacaoRepository.findByAlvoIdAndUsuarioId(alvoId, usuario.getId()).ifPresent(reacaoRepository::delete);
        return resumo(alvoId, usuario.getId());
    }

    public ReacoesResponse resumo(String alvoId, Integer usuarioId) {
        List<Reacao> reacoes = reacaoRepository.findByAlvoIdIn(List.of(alvoId));
        return new ReacoesResponse(alvoId, contar(reacoes), minha(reacoes, usuarioId));
    }

    /** Reações de vários itens de uma vez (evita uma consulta por item do feed). */
    public Map<String, List<Reacao>> porAlvo(Collection<String> alvoIds) {
        if (alvoIds.isEmpty()) return Map.of();
        return reacaoRepository.findByAlvoIdIn(alvoIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(Reacao::getAlvoId));
    }

    public static Map<TipoReacao, Long> contar(List<Reacao> reacoes) {
        Map<TipoReacao, Long> contagem = new EnumMap<>(TipoReacao.class);
        for (Reacao r : reacoes) contagem.merge(r.getTipo(), 1L, Long::sum);
        return contagem;
    }

    public static TipoReacao minha(List<Reacao> reacoes, Integer usuarioId) {
        return reacoes.stream()
                .filter(r -> r.getUsuario().getId().equals(usuarioId))
                .map(Reacao::getTipo)
                .findFirst()
                .orElse(null);
    }
}
