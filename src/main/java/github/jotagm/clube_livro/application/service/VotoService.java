package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.VotoRequest;
import github.jotagm.clube_livro.adapter.out.persistence.VotoRepository;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.Voto;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import github.jotagm.clube_livro.domain.exceptions.UsuarioJaVotouException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class VotoService {

    private final VotoRepository votoRepository;
    private final OpcaoVotoService opcaoVotoService;
    private final UsuarioService usuarioService;
    private final VotacaoService votacaoService;
    private final UsuarioClubeService usuarioClubeService;

    public Voto votar(VotoRequest request) {
        if (votoRepository.findByVotacaoIdAndUsuarioId(request.votacaoId(), request.usuarioId()).isPresent()) {
            throw new UsuarioJaVotouException();
        }

        Usuario usuario = usuarioService.buscarPorId(request.usuarioId());
        Votacao votacao = votacaoService.buscarPorId(request.votacaoId());
        OpcaoVoto opcaoVoto = opcaoVotoService.buscarPorId(request.opcaoVotoId());

        UsuarioClube usuarioClube = usuarioClubeService.buscarPorUsuarioEClube(usuario.getId(), votacao.getClube().getId());

        int peso = switch (usuarioClube.getPapel()) {
            case LIDER -> 2;
            case MEMBRO -> 1;
        };
        Voto voto = Voto.builder()
                .opcaoVoto(opcaoVoto)
                .usuario(usuario)
                .peso(peso)
                .votacao(votacao)
                .build();

        return votoRepository.save(voto);
    }

    public Voto buscarPorId(UUID id) {
        return votoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Voto não encontrado"));
    }

    public List<Voto> listarPorVotacao(UUID votacaoId) {
        return votoRepository.findByVotacaoId(votacaoId);
    }

    public List<Voto> listarPorOpcao(UUID opcaoVotoId) {
        return votoRepository.findByOpcaoVotoId(opcaoVotoId);
    }

    public Voto buscarPorVotacaoEUsuario(UUID votacaoId, UUID usuarioId) {
        return votoRepository.findByVotacaoIdAndUsuarioId(votacaoId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Voto não encontrado para este usuário nesta votação"));
    }

    public Voto atualizar(Voto voto) {
        return votoRepository.save(voto);
    }

    public void deletar(UUID id) {
        votoRepository.deleteById(id);
    }
}
