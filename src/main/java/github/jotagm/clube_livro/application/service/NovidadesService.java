package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.NovidadeClubeResponse;
import github.jotagm.clube_livro.adapter.out.persistence.UsuarioClubeRepository;
import github.jotagm.clube_livro.adapter.out.persistence.VisitaClubeRepository;
import github.jotagm.clube_livro.configs.AutorizacaoClube;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.VisitaClube;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * "Ponto de novidade" na lista de clubes: o que aconteceu num clube depois da última vez que a
 * pessoa o abriu, sem contar o que ela mesma fez. Quem nunca abriu o clube conta a partir de
 * quando entrou.
 *
 * <p>Usa a mesma atividade do feed (limitada a uma página por fonte), então é uma contagem do
 * que é recente, não do histórico inteiro — é o que o ponto precisa.
 */
@Service
@AllArgsConstructor
public class NovidadesService {

    private static final int JANELA = 100;

    private final FeedService feedService;
    private final VisitaClubeRepository visitaClubeRepository;
    private final UsuarioClubeRepository usuarioClubeRepository;
    private final AutorizacaoClube autorizacaoClube;
    private final Clock relogio;

    @Transactional
    public void marcarVisto(Usuario usuario, UUID clubeId) {
        autorizacaoClube.exigirMembro(usuario.getId(), clubeId);
        LocalDateTime agora = LocalDateTime.now(relogio);
        VisitaClube visita = visitaClubeRepository.findByUsuarioIdAndClubeId(usuario.getId(), clubeId)
                .orElseGet(() -> VisitaClube.primeira(usuario.getId(), clubeId, agora));
        visita.verEm(agora);
        visitaClubeRepository.save(visita);
    }

    public List<NovidadeClubeResponse> novidades(Usuario usuario) {
        List<UsuarioClube> filiacoes = usuarioClubeRepository.findByUsuarioId(usuario.getId());
        if (filiacoes.isEmpty()) return List.of();

        Map<UUID, LocalDateTime> desde = new HashMap<>();
        for (UsuarioClube uc : filiacoes) {
            desde.put(uc.getClube().getId(), uc.getEntrouEm() == null ? LocalDateTime.MIN : uc.getEntrouEm());
        }
        for (VisitaClube v : visitaClubeRepository.findByUsuarioId(usuario.getId())) {
            desde.computeIfPresent(v.getClubeId(), (id, entrou) -> v.getVistoEm());
        }

        Map<UUID, Integer> contagem = new LinkedHashMap<>();
        for (FeedService.Atividade a : feedService.atividades(new ArrayList<>(desde.keySet()), JANELA)) {
            boolean minha = usuario.getId().equals(a.ator());
            if (!minha && a.quando().isAfter(desde.get(a.clubeId()))) {
                contagem.merge(a.clubeId(), 1, Integer::sum);
            }
        }
        return contagem.entrySet().stream()
                .map(e -> new NovidadeClubeResponse(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
