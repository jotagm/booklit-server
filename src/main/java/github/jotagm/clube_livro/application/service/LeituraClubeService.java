package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.LeituraClubeRequest;
import github.jotagm.clube_livro.adapter.out.persistence.LeituraClubeRepository;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.leitura.Registro;
import github.jotagm.clube_livro.domain.exceptions.IntervaloInvalidoException;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
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
        validarIntervalo(request);

        LeituraClube leitura = LeituraClube.iniciar(
                clubeService.buscarPorId(request.clubeId()),
                request.livroGoogleId(),
                request.livroTitulo(),
                request.livroCapaUrl(),
                request.tipoMeta(),
                request.valorMeta(),
                request.dataInicio(),
                request.dataFim());

        LeituraClube leituraSalva = leituraClubeRepository.save(leitura);

        List<UsuarioClube> membros = usuarioClubeService.listarPorClube(request.clubeId());
        membros.forEach(uc -> registroService.salvar(Registro.iniciar(leituraSalva, uc.getUsuario())));

        log.info("Leitura criada id={} clube={} livro='{}' meta={} {} registros={}",
                leituraSalva.getId(), request.clubeId(), leituraSalva.getLivroTitulo(),
                leituraSalva.getValorMeta(), leituraSalva.getTipoMeta(), membros.size());

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
        validarIntervalo(request);

        LeituraClube leitura = buscarPorId(id);
        leitura.redefinir(
                request.livroGoogleId(),
                request.livroTitulo(),
                request.livroCapaUrl(),
                request.tipoMeta(),
                request.valorMeta(),
                request.dataInicio(),
                request.dataFim());
        return leituraClubeRepository.save(leitura);
    }

    public void deletar(UUID id) {
        leituraClubeRepository.deleteById(id);
    }

    /**
     * Data de início no passado é permitida de propósito — serve para registrar uma leitura
     * que já estava em andamento. O que não pode é fim antes do início: o intervalo nunca
     * conteria o instante atual, então a leitura seria salva e nunca apareceria.
     */
    private void validarIntervalo(LeituraClubeRequest request) {
        if (!request.dataFim().isAfter(request.dataInicio())) {
            throw new IntervaloInvalidoException();
        }
    }

    public Optional<LeituraClube> buscarLeituraAtiva(UUID clubeId){
        LocalDateTime horario = LocalDateTime.now();
        return leituraClubeRepository.findLeituraAtiva(clubeId, horario);
    }

}
