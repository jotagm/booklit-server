package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.LeituraClubeRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.VotacaoRequest;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.Voto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus.ENCERRADA;

@Service
@AllArgsConstructor
public class VotacaoEnceramentoService {

    private final VotacaoService votacaoService;
    private final VotoService votoService;
    private final LeituraClubeService leituraClubeService;

    public LeituraClube finalizarVotacao(UUID idVotacao) {
        Votacao votacao = votacaoService.buscarPorId(idVotacao);
        List<Voto> votos = votoService.listarPorVotacao(idVotacao);

        Map<OpcaoVoto, Integer> votosPorOpcao = votos.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getOpcaoVoto(),           // agrupa por essa chave
                        Collectors.summingInt(v -> v.getPeso())  // soma isso em cada grupo
                ));

        OpcaoVoto vencedora = votosPorOpcao.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get()
                .getKey();

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

}
