package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.LeituraPessoalRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.EstanteResumoResponse;
import github.jotagm.clube_livro.adapter.out.persistence.LeituraPessoalRepository;
import github.jotagm.clube_livro.adapter.out.persistence.MetaLeituraRepository;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.leitura.TipoMeta;
import github.jotagm.clube_livro.domain.estante.LeituraPessoal;
import github.jotagm.clube_livro.domain.estante.MetaLeitura;
import github.jotagm.clube_livro.domain.estante.StatusLeituraPessoal;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class EstanteService {

    private final LeituraPessoalRepository leituraPessoalRepository;
    private final MetaLeituraRepository metaLeituraRepository;

    public List<LeituraPessoal> listar(Usuario usuario, StatusLeituraPessoal status) {
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este livro já está na sua estante");
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
        return leituraPessoalRepository.save(leitura);
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
        return LocalDate.now();
    }
}
