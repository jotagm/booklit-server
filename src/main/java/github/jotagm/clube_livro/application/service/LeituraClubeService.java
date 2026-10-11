package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.LeituraClubeRequest;
import github.jotagm.clube_livro.adapter.out.persistence.LeituraClubeRepository;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.leitura.Registro;
import github.jotagm.clube_livro.domain.exceptions.IntervaloInvalidoException;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        leitura.definirSpotify(request.spotifyUrl());

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

    @Transactional
    public int iniciarRegistrosDosMembros(UUID leituraId) {
        LeituraClube leitura = buscarPorId(leituraId);
        List<UsuarioClube> membros = usuarioClubeService.listarPorClube(leitura.getClube().getId());
        membros.forEach(uc -> registroService.salvar(Registro.iniciar(leitura, uc.getUsuario())));
        return membros.size();
    }

    /**
     * Quem entra no clube com uma leitura já em andamento não estava na lista de membros
     * quando os registros foram criados — sem isto o novo membro nunca conseguiria registrar
     * progresso (o PUT de registro responde 404).
     */
    public void iniciarRegistroNaLeituraAtiva(UUID clubeId, Usuario usuario) {
        buscarLeituraAtiva(clubeId).ifPresent(leitura -> {
            if (!registroService.existe(leitura.getId(), usuario.getId())) {
                registroService.salvar(Registro.iniciar(leitura, usuario));
                log.info("Registro iniciado para novo membro leitura={} usuario={}", leitura.getId(), usuario.getEmail());
            }
        });
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
        leitura.definirSpotify(request.spotifyUrl());
        return leituraClubeRepository.save(leitura);
    }

    /** Troca só o link do Spotify, sem exigir o corpo inteiro da leitura. */
    public LeituraClube definirSpotify(UUID id, String url) {
        LeituraClube leitura = buscarPorId(id);
        leitura.definirSpotify(url);
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
