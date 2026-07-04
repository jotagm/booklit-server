package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.ClubeRequest;
import github.jotagm.clube_livro.adapter.out.persistence.ClubeRepository;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.ClubeStatus;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ClubeService {

    private final ClubeRepository clubeRepository;
    private final TemaService temaService;
    private final UsuarioClubeService usuarioClubeService;

    public Clube criar(ClubeRequest request, Usuario criador) {
        Clube clube = Clube.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .privado(request.privado())
                .status(ClubeStatus.ATIVO)
                .createdAt(LocalDateTime.now())
                .temas(request.temaIds() != null
                        ? request.temaIds().stream().map(temaService::buscarPorId).toList()
                        : null)
                .build();

        Clube clubeSalvo = clubeRepository.save(clube);
        usuarioClubeService.adicionarLider(criador, clubeSalvo);

        return clubeSalvo;
    }

    public Clube salvar(Clube clube) {
        return clubeRepository.save(clube);
    }

    public Clube buscarPorId(UUID id) {
        return clubeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Clube não encontrado"));
    }

    public Clube buscarPorNome(String nome) {
        return clubeRepository.findByNome(nome)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Clube não encontrado"));
    }

    public Page<Clube> listarVisiveis(UUID usuarioId, Pageable pageable) {
        List<UUID> clubesDoUsuario = usuarioClubeService.listarPorUsuario(usuarioId).stream()
                .map(uc -> uc.getClube().getId())
                .toList();

        return clubeRepository.findByPrivadoFalseOrIdIn(clubesDoUsuario, pageable);
    }

    public Clube atualizar(Clube clube) {
        return clubeRepository.save(clube);
    }

    public void deletar(UUID id) {
        clubeRepository.deleteById(id);
    }
}
