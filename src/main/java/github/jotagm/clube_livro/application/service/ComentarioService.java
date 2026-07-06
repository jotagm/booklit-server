package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.ComentarioAtualizarRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.ComentarioRequest;
import github.jotagm.clube_livro.adapter.out.persistence.ComentarioRepository;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.leitura.Comentario;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.exceptions.AcessoNegadoException;
import github.jotagm.clube_livro.domain.exceptions.ComentarioAninhadoException;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final LeituraClubeService leituraClubeService;
    private final UsuarioClubeService usuarioClubeService;

    public Comentario criar(UUID leituraClubeId, ComentarioRequest request, Usuario autor) {
        LeituraClube leitura = leituraClubeService.buscarPorId(leituraClubeId);
        exigirMembro(autor.getId(), leitura.getClube().getId());

        Comentario comentarioPai = null;
        if (request.comentarioPaiId() != null) {
            comentarioPai = buscarPorId(request.comentarioPaiId());
            if (comentarioPai.getComentarioPai() != null) {
                throw new ComentarioAninhadoException();
            }
        }

        Comentario comentario = Comentario.builder()
                .leituraClube(leitura)
                .usuario(autor)
                .comentarioPai(comentarioPai)
                .conteudo(request.conteudo())
                .removido(false)
                .createdAt(LocalDateTime.now())
                .build();

        return comentarioRepository.save(comentario);
    }

    public List<Comentario> listarPorLeitura(UUID leituraClubeId, Usuario solicitante) {
        LeituraClube leitura = leituraClubeService.buscarPorId(leituraClubeId);
        exigirMembro(solicitante.getId(), leitura.getClube().getId());

        return comentarioRepository.findByLeituraClubeIdOrderByCreatedAtAsc(leituraClubeId);
    }

    public Comentario buscarPorId(UUID id) {
        return comentarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comentário não encontrado"));
    }

    public Comentario editar(UUID id, ComentarioAtualizarRequest request, Usuario solicitante) {
        Comentario comentario = buscarPorId(id);

        if (comentario.isRemovido()) {
            throw new AcessoNegadoException("Não é possível editar um comentário removido");
        }
        if (!comentario.getUsuario().getId().equals(solicitante.getId())) {
            throw new AcessoNegadoException("Acesso negado: apenas o autor pode editar este comentário");
        }

        comentario.setConteudo(request.conteudo());
        comentario.setUpdatedAt(LocalDateTime.now());
        return comentarioRepository.save(comentario);
    }

    public void deletar(UUID id, Usuario solicitante) {
        Comentario comentario = buscarPorId(id);

        boolean autor = comentario.getUsuario().getId().equals(solicitante.getId());
        boolean lider = !autor && isLider(solicitante.getId(), comentario.getLeituraClube().getClube().getId());

        if (!autor && !lider) {
            throw new AcessoNegadoException("Acesso negado: apenas o autor ou o líder do clube podem remover este comentário");
        }

        comentario.setRemovido(true);
        comentarioRepository.save(comentario);
    }

    private void exigirMembro(UUID usuarioId, UUID clubeId) {
        if (!isMembro(usuarioId, clubeId)) {
            throw new AcessoNegadoException("Acesso negado: apenas membros do clube podem acessar os comentários desta leitura");
        }
    }

    private boolean isMembro(UUID usuarioId, UUID clubeId) {
        try {
            usuarioClubeService.buscarPorUsuarioEClube(usuarioId, clubeId);
            return true;
        } catch (RecursoNaoEncontradoException e) {
            return false;
        }
    }

    private boolean isLider(UUID usuarioId, UUID clubeId) {
        try {
            return usuarioClubeService.buscarPorUsuarioEClube(usuarioId, clubeId).getPapel() == ClubePapel.LIDER;
        } catch (RecursoNaoEncontradoException e) {
            return false;
        }
    }
}
