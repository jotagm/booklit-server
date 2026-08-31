package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.VotacaoRequest;
import github.jotagm.clube_livro.adapter.out.persistence.VotacaoRepository;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus;
import github.jotagm.clube_livro.domain.exceptions.IntervaloInvalidoException;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class VotacaoService {

    private final VotacaoRepository votacaoRepository;
    private final ClubeService clubeService;

    public Votacao salvar(Votacao votacao) {
        return votacaoRepository.save(votacao);
    }

    public Votacao criar(VotacaoRequest request) {
        validarIntervalo(request);

        Votacao votacao = Votacao.abrir(
                clubeService.buscarPorId(request.clubeId()),
                request.dataAbertura(),
                request.dataEncerramento());

        Votacao salva = votacaoRepository.save(votacao);

        log.info("Votação criada id={} clube={} abertura={} encerramento={}",
                salva.getId(), request.clubeId(), salva.getDataAbertura(), salva.getDataEncerramento());

        return salva;
    }

    public Votacao buscarPorId(UUID id) {
        return votacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Votação não encontrada"));
    }

    public List<Votacao> listarPorClube(UUID clubeId) {
        return votacaoRepository.findByClubeId(clubeId);
    }

    /** Sobrecarga usada internamente — o encerramento automático salva a entidade já montada. */
    public Votacao atualizar(Votacao votacao) {
        return votacaoRepository.save(votacao);
    }

    public Votacao atualizar(UUID id, VotacaoRequest request) {
        validarIntervalo(request);

        Votacao votacao = buscarPorId(id);
        votacao.reagendar(request.dataAbertura(), request.dataEncerramento());

        return votacaoRepository.save(votacao);
    }

    public void deletar(UUID id) {
        votacaoRepository.deleteById(id);
        log.info("Votação removida id={}", id);
    }

    public List<Votacao> listarAbertasVencidas(LocalDateTime momento) {
        return votacaoRepository.findByStatusAndDataEncerramentoBefore(VotacaoStatus.ABERTA, momento);
    }

    /**
     * Mesma regra da leitura: abertura no passado é permitida, mas o encerramento precisa vir
     * depois da abertura.
     *
     * <p>Um intervalo vazio seria salvo sem erro e a votação nunca abriria — e, como
     * {@code listarAbertasVencidas} só olha {@code dataEncerramento}, o agendador a encerraria
     * no primeiro ciclo, sem nenhum voto.
     */
    private void validarIntervalo(VotacaoRequest request) {
        if (!request.dataEncerramento().isAfter(request.dataAbertura())) {
            throw new IntervaloInvalidoException();
        }
    }
}
