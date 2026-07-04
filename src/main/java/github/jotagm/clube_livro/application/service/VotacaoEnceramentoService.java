package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.Voto;
import github.jotagm.clube_livro.domain.exceptions.VotacaoSemVotosException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus.ENCERRADA;

@Service
@AllArgsConstructor
public class VotacaoEnceramentoService {

    private static final int PESO_VOTO_LIDER = 2;

    private final VotacaoService votacaoService;
    private final VotoService votoService;
    private final LeituraClubeService leituraClubeService;

    public LeituraClube finalizarVotacao(UUID idVotacao) {
        Votacao votacao = votacaoService.buscarPorId(idVotacao);
        List<Voto> votos = votoService.listarPorVotacao(idVotacao);

        if (votos.isEmpty()) {
            throw new VotacaoSemVotosException();
        }

        Map<UUID, Integer> pesoPorOpcaoId = votos.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getOpcaoVoto().getId(),
                        Collectors.summingInt(Voto::getPeso)
                ));

        Map<UUID, OpcaoVoto> opcaoPorId = votos.stream()
                .collect(Collectors.toMap(
                        v -> v.getOpcaoVoto().getId(),
                        Voto::getOpcaoVoto,
                        (existente, novo) -> existente
                ));

        int maiorPeso = pesoPorOpcaoId.values().stream()
                .max(Integer::compareTo)
                .orElseThrow(VotacaoSemVotosException::new);

        List<UUID> opcoesEmpatadas = pesoPorOpcaoId.entrySet().stream()
                .filter(entrada -> entrada.getValue() == maiorPeso)
                .map(Map.Entry::getKey)
                .toList();

        OpcaoVoto vencedora = opcaoPorId.get(desempatar(opcoesEmpatadas, votos));

        votacao.setStatus(ENCERRADA);
        votacaoService.atualizar(votacao);

        LeituraClube leituraClube = LeituraClube.builder()
                .clube(votacao.getClube())
                .livroGoogleId(vencedora.getLivroGoogleId())
                .livroTitulo(vencedora.getLivroTitulo())
                .livroCapaUrl(vencedora.getLivroCapaUrl())
                .build();

        return leituraClubeService.salvar(leituraClube);
    }

    private UUID desempatar(List<UUID> opcoesEmpatadas, List<Voto> votos) {
        if (opcoesEmpatadas.size() == 1) {
            return opcoesEmpatadas.get(0);
        }

        return votos.stream()
                .filter(voto -> voto.getPeso() == PESO_VOTO_LIDER)
                .map(voto -> voto.getOpcaoVoto().getId())
                .filter(opcoesEmpatadas::contains)
                .findFirst()
                .orElseGet(() -> opcoesEmpatadas.stream()
                        .min(Comparator.comparing(UUID::toString))
                        .orElseThrow());
    }
}
