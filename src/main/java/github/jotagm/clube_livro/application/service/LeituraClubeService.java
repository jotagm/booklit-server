package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.LeituraClubeRequest;
import github.jotagm.clube_livro.adapter.out.persistence.LeituraClubeRepository;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.leitura.Registro;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
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
    private final UsuarioClubeService usuarioClubeService;
    private final RegistroService registroService;

    public LeituraClube salvar(LeituraClube leituraClube) {
        return leituraClubeRepository.save(leituraClube);
    }

    public LeituraClube criar(LeituraClubeRequest request) {
        LeituraClube leitura = LeituraClube.builder()
                .clube(clubeService.buscarPorId(request.clubeId()))
                .livroGoogleId(request.livroGoogleId())
                .livroTitulo(request.livroTitulo())
                .livroCapaUrl(request.livroCapaUrl())
                .tipoMeta(request.tipoMeta())
                .valorMeta(request.valorMeta())
                .dataInicio(request.dataInicio())
                .dataFim(request.dataFim())
                .build();

        LeituraClube leituraSalva = leituraClubeRepository.save(leitura);

        List<UsuarioClube> membros = usuarioClubeService.listarPorClube(request.clubeId());
        membros.forEach(uc -> {
            Registro registro = Registro.builder()
                    .leituraClube(leituraSalva)
                    .usuario(uc.getUsuario())
                    .valorAtual(0)
                    .updatedAt(LocalDateTime.now())
                    .build();
            registroService.salvar(registro);
        });

        return leituraSalva;
    }

    public LeituraClube buscarPorId(UUID id) {
        return leituraClubeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Leitura não encontrada"));
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
