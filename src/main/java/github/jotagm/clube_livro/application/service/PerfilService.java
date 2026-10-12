package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.PerfilResponse;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.PerfilResponse.ClubeEmComum;
import github.jotagm.clube_livro.adapter.in.rest.dto.response.PerfilResponse.LivroDoPerfil;
import github.jotagm.clube_livro.adapter.out.persistence.LeituraPessoalRepository;
import github.jotagm.clube_livro.adapter.out.persistence.MetaLeituraRepository;
import github.jotagm.clube_livro.adapter.out.persistence.UsuarioClubeRepository;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.estante.LeituraPessoal;
import github.jotagm.clube_livro.domain.estante.MetaLeitura;
import github.jotagm.clube_livro.domain.estante.StatusLeituraPessoal;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Monta o perfil público de um leitor para quem divide clube com ele (ou para ele mesmo). */
@Service
@AllArgsConstructor
public class PerfilService {

    private static final int MAX_LIDOS_RECENTES = 8;

    private final UsuarioService usuarioService;
    private final UsuarioClubeRepository usuarioClubeRepository;
    private final LeituraPessoalRepository leituraPessoalRepository;
    private final MetaLeituraRepository metaLeituraRepository;

    public Usuario atualizarPerfil(Usuario usuario, String nome, String bio) {
        return usuarioService.atualizarPerfil(usuario, nome, bio);
    }

    /**
     * 404 (e não 403) quando não há clube em comum: assim a API não serve para descobrir
     * quem tem conta.
     */
    public PerfilResponse perfil(Usuario quemPede, Integer alvoId) {
        boolean ehVoce = quemPede.getId().equals(alvoId);
        Usuario alvo = ehVoce ? quemPede : usuarioService.buscarPorId(alvoId);

        List<UsuarioClube> clubesDoAlvo = usuarioClubeRepository.findByUsuarioId(alvoId);
        Set<UUID> meusClubes = usuarioClubeRepository.findByUsuarioId(quemPede.getId()).stream()
                .map(uc -> uc.getClube().getId())
                .collect(Collectors.toSet());

        List<ClubeEmComum> clubes = clubesDoAlvo.stream()
                .filter(uc -> ehVoce || meusClubes.contains(uc.getClube().getId()))
                .map(uc -> new ClubeEmComum(uc.getClube().getId(), uc.getClube().getNome()))
                .toList();
        if (!ehVoce && clubes.isEmpty()) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado");
        }

        int ano = LocalDate.now().getYear();
        List<LeituraPessoal> lendo = leituraPessoalRepository
                .findByUsuarioIdAndStatusOrderByUpdatedAtDesc(alvoId, StatusLeituraPessoal.LENDO);
        List<LeituraPessoal> lidos = leituraPessoalRepository
                .findByUsuarioIdAndStatusOrderByUpdatedAtDesc(alvoId, StatusLeituraPessoal.LIDO).stream()
                .sorted(Comparator.comparing(LeituraPessoal::getDataFim,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        long lidosNoAno = lidos.stream().filter(l -> l.concluidaNoAno(ano)).count();
        Integer meta = metaLeituraRepository.findByUsuarioIdAndAno(alvoId, ano)
                .map(MetaLeitura::getQuantidadeLivros)
                .orElse(null);

        return new PerfilResponse(
                alvo.getId(), alvo.getNome(), alvo.getBio(), alvo.getCreatedAt(), ehVoce,
                ano, lidosNoAno, meta, lidos.size(),
                lendo.stream().map(LivroDoPerfil::from).toList(),
                lidos.stream().limit(MAX_LIDOS_RECENTES).map(LivroDoPerfil::from).toList(),
                clubes);
    }
}
