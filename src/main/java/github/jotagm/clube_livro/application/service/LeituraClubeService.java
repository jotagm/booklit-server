package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.LeituraClubeRequest;
import github.jotagm.clube_livro.adapter.out.persistence.LeituraClubeRepository;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class LeituraClubeService {

    private final LeituraClubeRepository leituraClubeRepository;
    private final ClubeService clubeService;

    public LeituraClube salvar(LeituraClube leituraClube) {
        return leituraClubeRepository.save(leituraClube);
    }

    public LeituraClube criar(LeituraClubeRequest request) {
        LeituraClube leitura = new LeituraClube();
        leitura.setClube(clubeService.buscarPorId(request.clubeId()));
        leitura.setLivroGoogleId(request.livroGoogleId());
        leitura.setLivroTitulo(request.livroTitulo());
        leitura.setLivroCapaUrl(request.livroCapaUrl());
        leitura.setTipoMeta(request.tipoMeta());
        leitura.setValorMeta(request.valorMeta());
        leitura.setDataInicio(request.dataInicio());
        leitura.setDataFim(request.dataFim());
        return leituraClubeRepository.save(leitura);
    }

    public LeituraClube buscarPorId(UUID id) {
        return leituraClubeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leitura não encontrada"));
    }

    public List<LeituraClube> listarPorClube(UUID clubeId) {
        return leituraClubeRepository.findByClubeIdOrderByDataInicioDesc(clubeId);
    }

    public LeituraClube atualizar(UUID id, LeituraClubeRequest request) {
        LeituraClube leitura = buscarPorId(id);
        leitura.setLivroGoogleId(request.livroGoogleId());
        leitura.setLivroTitulo(request.livroTitulo());
        leitura.setLivroCapaUrl(request.livroCapaUrl());
        leitura.setTipoMeta(request.tipoMeta());
        leitura.setValorMeta(request.valorMeta());
        leitura.setDataInicio(request.dataInicio());
        leitura.setDataFim(request.dataFim());
        return leituraClubeRepository.save(leitura);
    }

    public void deletar(UUID id) {
        leituraClubeRepository.deleteById(id);
    }

    public Optional<LeituraClube> buscarLeituraAtiva(UUID clubeId){
        LocalDateTime horario = LocalDateTime.now();
        return leituraClubeRepository.findLeituraAtiva(clubeId, horario);
    }

}
