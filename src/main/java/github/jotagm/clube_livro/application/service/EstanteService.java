package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.LeituraPessoalRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.EstanteResumoResponse;
import github.jotagm.clube_livro.adapter.out.persistence.LeituraClubeRepository;
import github.jotagm.clube_livro.adapter.out.persistence.LeituraPessoalRepository;
import github.jotagm.clube_livro.adapter.out.persistence.MetaLeituraRepository;
import github.jotagm.clube_livro.adapter.out.persistence.RegistroRepository;
import github.jotagm.clube_livro.adapter.out.persistence.UsuarioClubeRepository;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.leitura.TipoMeta;
import github.jotagm.clube_livro.domain.estante.LeituraPessoal;
import github.jotagm.clube_livro.domain.estante.MetaLeitura;
import github.jotagm.clube_livro.domain.estante.StatusLeituraPessoal;
import github.jotagm.clube_livro.domain.exceptions.ConflitoException;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class EstanteService {

    private final LeituraPessoalRepository leituraPessoalRepository;
    private final MetaLeituraRepository metaLeituraRepository;
    private final UsuarioClubeRepository usuarioClubeRepository;
    private final LeituraClubeRepository leituraClubeRepository;
    private final RegistroRepository registroRepository;
    private final SequenciaDeLeituraService sequenciaDeLeituraService;
    private final Clock relogio;

    public List<LeituraPessoal> listar(Usuario usuario, StatusLeituraPessoal status) {
        garantirLivrosDosClubes(usuario);
        return status == null
                ? leituraPessoalRepository.findByUsuarioIdOrderByUpdatedAtDesc(usuario.getId())
                : leituraPessoalRepository.findByUsuarioIdAndStatusOrderByUpdatedAtDesc(usuario.getId(), status);
    }

    /** 404 (e não 403) para item de outra pessoa: a estante é privada, nem a existência vaza. */
    public LeituraPessoal buscar(UUID id, Usuario usuario) {
        return leituraPessoalRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado na sua estante"));
    }

    public LeituraPessoal adicionar(Usuario usuario, LeituraPessoalRequest request) {
        if (leituraPessoalRepository.findByUsuarioIdAndLivroGoogleId(usuario.getId(), request.livroGoogleId()).isPresent()) {
            throw new ConflitoException("Este livro já está na sua estante");
        }
        LeituraPessoal leitura = LeituraPessoal.adicionar(
                usuario, request.livroGoogleId(), request.livroTitulo(), request.livroAutor(),
                request.livroCapaUrl(), request.totalPaginas(),
                request.status() == null ? StatusLeituraPessoal.QUERO_LER : request.status(),
                hoje());
        LeituraPessoal salva = leituraPessoalRepository.save(leitura);
        log.info("Livro adicionado à estante id={} usuario={} livro='{}' status={}",
                salva.getId(), usuario.getEmail(), salva.getLivroTitulo(), salva.getStatus());
        return salva;
    }

    public LeituraPessoal mudarStatus(UUID id, Usuario usuario, StatusLeituraPessoal status) {
        LeituraPessoal leitura = buscar(id, usuario);
        leitura.mudarStatus(status, hoje());
        return leituraPessoalRepository.save(leitura);
    }

    public LeituraPessoal atualizarProgresso(UUID id, Usuario usuario, int pagina, Integer totalPaginas) {
        LeituraPessoal leitura = buscar(id, usuario);
        leitura.atualizarProgresso(pagina, totalPaginas, hoje());
        LeituraPessoal salva = leituraPessoalRepository.save(leitura);
        if (pagina > 0) sequenciaDeLeituraService.registrarHoje(usuario.getId());
        return salva;
    }

    public LeituraPessoal avaliar(UUID id, Usuario usuario, Integer nota, String resenha) {
        LeituraPessoal leitura = buscar(id, usuario);
        leitura.avaliar(nota, resenha);
        return leituraPessoalRepository.save(leitura);
    }

    public void remover(UUID id, Usuario usuario) {
        leituraPessoalRepository.delete(buscar(id, usuario));
    }

    public EstanteResumoResponse resumo(Usuario usuario, int ano) {
        garantirLivrosDosClubes(usuario);
        Integer meta = metaLeituraRepository.findByUsuarioIdAndAno(usuario.getId(), ano)
                .map(MetaLeitura::getQuantidadeLivros)
                .orElse(null);
        long lidos = leituraPessoalRepository.countByUsuarioIdAndStatusAndDataFimBetween(
                usuario.getId(), StatusLeituraPessoal.LIDO, LocalDate.of(ano, 1, 1), LocalDate.of(ano, 12, 31));
        long lendo = leituraPessoalRepository.countByUsuarioIdAndStatus(usuario.getId(), StatusLeituraPessoal.LENDO);
        long queroLer = leituraPessoalRepository.countByUsuarioIdAndStatus(usuario.getId(), StatusLeituraPessoal.QUERO_LER);
        return new EstanteResumoResponse(ano, meta, lidos, lendo, queroLer);
    }

    public MetaLeitura definirMeta(Usuario usuario, int ano, int quantidadeLivros) {
        MetaLeitura meta = metaLeituraRepository.findByUsuarioIdAndAno(usuario.getId(), ano)
                .orElseGet(() -> MetaLeitura.definir(usuario, ano, quantidadeLivros));
        meta.alterarQuantidade(quantidadeLivros);
        return metaLeituraRepository.save(meta);
    }

    /**
     * Espelha na estante pessoal o progresso registrado numa leitura do clube: o livro entra
     * como LENDO no primeiro avanço e vira LIDO quando a meta é atingida. Só anda para frente —
     * se a pessoa já tinha marcado o livro como lido por conta própria, nada muda.
     *
     * <p>Meta em capítulos não tem como virar página, então só o status é sincronizado.
     */
    @Transactional
    public void sincronizarProgressoDoClube(Usuario usuario, LeituraClube leituraClube, int valorAtual) {
        if (valorAtual <= 0 || leituraClube.getLivroGoogleId() == null) {
            return;
        }
        boolean emPaginas = leituraClube.getTipoMeta() == TipoMeta.PAGINA;
        boolean concluiu = valorAtual >= leituraClube.getValorMeta();
        LocalDate hoje = hoje();

        LeituraPessoal leitura = leituraPessoalRepository
                .findByUsuarioIdAndLivroGoogleId(usuario.getId(), leituraClube.getLivroGoogleId())
                .orElseGet(() -> LeituraPessoal.adicionar(
                        usuario, leituraClube.getLivroGoogleId(), leituraClube.getLivroTitulo(), null,
                        leituraClube.getLivroCapaUrl(), emPaginas ? leituraClube.getValorMeta() : null,
                        StatusLeituraPessoal.LENDO, hoje));

        if (leitura.getStatus() == StatusLeituraPessoal.LIDO) {
            return;
        }
        leitura.vincularClube(leituraClube.getClube() != null ? leituraClube.getClube().getId() : null);

        if (concluiu) {
            leitura.mudarStatus(StatusLeituraPessoal.LIDO, hoje);
        } else if (emPaginas && (leitura.getTotalPaginas() == null || valorAtual <= leitura.getTotalPaginas())) {
            Integer total = leitura.getTotalPaginas() == null ? leituraClube.getValorMeta() : null;
            leitura.atualizarProgresso(valorAtual, total, hoje);
        } else if (leitura.getStatus() != StatusLeituraPessoal.LENDO) {
            leitura.mudarStatus(StatusLeituraPessoal.LENDO, hoje);
        }
        leituraPessoalRepository.save(leitura);
    }

    /**
     * O livro que o clube está lendo também é da pessoa: ele aparece na estante sem esperar o
     * primeiro registro de página. A leitura em andamento entra (ou passa) para LENDO; a que o
     * clube escolheu e ainda não começou (votação encerrada, sem meta e datas) entra como
     * QUERO_LER. Nunca mexe no que a pessoa já marcou como lido ou abandonado.
     *
     * <p>Roda quando a estante (ou o perfil) é aberta, e não nos pontos em que a leitura nasce:
     * assim cobre também quem entrou no clube depois, leituras completadas mais tarde e as que
     * já existiam antes disto. É idempotente.
     */
    @Transactional
    public void garantirLivrosDosClubes(Usuario usuario) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDate hoje = hoje();
        for (UsuarioClube uc : usuarioClubeRepository.findByUsuarioId(usuario.getId())) {
            for (LeituraClube l : leituraClubeRepository.findByClubeIdOrderByDataInicioDesc(uc.getClube().getId())) {
                if (l.getLivroGoogleId() == null) continue;
                boolean semDatas = l.getDataInicio() == null || l.getDataFim() == null;
                boolean emAndamento = !semDatas && !l.getDataInicio().isAfter(agora) && !l.getDataFim().isBefore(agora);
                if (emAndamento) {
                    garantirNaEstante(usuario, l, StatusLeituraPessoal.LENDO, hoje);
                } else if (semDatas) {
                    garantirNaEstante(usuario, l, StatusLeituraPessoal.QUERO_LER, hoje);
                }
            }
        }
    }

    private void garantirNaEstante(Usuario usuario, LeituraClube l, StatusLeituraPessoal status, LocalDate hoje) {
        boolean emPaginas = l.getTipoMeta() == TipoMeta.PAGINA && l.getValorMeta() > 0;
        Optional<LeituraPessoal> existente = leituraPessoalRepository
                .findByUsuarioIdAndLivroGoogleId(usuario.getId(), l.getLivroGoogleId());
        LeituraPessoal leitura;
        if (existente.isEmpty()) {
            leitura = LeituraPessoal.adicionar(usuario, l.getLivroGoogleId(), l.getLivroTitulo(), l.getLivroAutor(),
                    l.getLivroCapaUrl(), emPaginas ? l.getValorMeta() : null, status, hoje);
            if (status == StatusLeituraPessoal.LENDO && emPaginas) {
                registroRepository.findByLeituraClubeIdAndUsuarioId(l.getId(), usuario.getId())
                        .filter(r -> r.getValorAtual() > 0 && r.getValorAtual() < l.getValorMeta())
                        .ifPresent(r -> leitura.atualizarProgresso(r.getValorAtual(), null, hoje));
            }
        } else {
            leitura = existente.get();
            boolean subir = status == StatusLeituraPessoal.LENDO && leitura.getStatus() == StatusLeituraPessoal.QUERO_LER;
            boolean vincular = leitura.getClubeOrigemId() == null
                    && (leitura.getStatus() == StatusLeituraPessoal.QUERO_LER || leitura.getStatus() == StatusLeituraPessoal.LENDO);
            if (!subir && !vincular) return;
            if (subir) leitura.mudarStatus(StatusLeituraPessoal.LENDO, hoje);
        }
        leitura.vincularClube(l.getClube() != null ? l.getClube().getId() : null);
        leituraPessoalRepository.save(leitura);
    }

    /** Para cada livro, quantos dos usuários informados o têm no "quero ler". */
    public Map<String, Long> contarQueroLer(Collection<Integer> usuarioIds, Collection<String> livroIds) {
        Map<String, Long> contagem = new HashMap<>();
        if (usuarioIds.isEmpty() || livroIds.isEmpty()) {
            return contagem;
        }
        for (Object[] linha : leituraPessoalRepository.contarQueroLerPorLivro(usuarioIds, livroIds)) {
            contagem.put((String) linha[0], ((Number) linha[1]).longValue());
        }
        return contagem;
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }
}
