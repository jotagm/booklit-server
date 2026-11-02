package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.application.port.out.EnviadorDeLembreteDeVotacao;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.exceptions.RegraDeNegocioException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * "Lembrar quem não votou": o líder dispara, cada membro sem voto recebe um e-mail. Limitado a
 * um lembrete por {@link Votacao#INTERVALO_ENTRE_LEMBRETES} (a regra fica na própria votação).
 */
@Slf4j
@Service
@AllArgsConstructor
public class LembreteDeVotacaoService {

    private final VotacaoService votacaoService;
    private final VotoService votoService;
    private final UsuarioClubeService usuarioClubeService;
    private final EnviadorDeLembreteDeVotacao enviador;
    private final Clock relogio;

    public record Resultado(int notificados, LocalDateTime lembradoEm) {
    }

    /** Quem chama já foi conferido como líder do clube da votação (ver o controller). */
    @Transactional
    public Resultado lembrarQuemNaoVotou(UUID votacaoId, Usuario lider) {
        Votacao votacao = votacaoService.buscarPorId(votacaoId);

        Set<Integer> votaram = votoService.listarPorVotacao(votacaoId).stream()
                .map(v -> v.getUsuario().getId())
                .collect(Collectors.toSet());
        // O próprio líder não recebe e-mail de si mesmo, mesmo que ainda não tenha votado.
        List<Usuario> faltam = usuarioClubeService.listarPorClube(votacao.getClube().getId()).stream()
                .map(UsuarioClube::getUsuario)
                .filter(u -> !votaram.contains(u.getId()) && !u.getId().equals(lider.getId()))
                .toList();
        if (faltam.isEmpty()) {
            throw new RegraDeNegocioException("Todos os membros já votaram");
        }
        // Quem desligou os lembretes nas configurações conta como lembrado, mas não recebe e-mail.
        List<Usuario> destinatarios = faltam.stream().filter(Usuario::recebeLembretes).toList();

        LocalDateTime agora = LocalDateTime.now(relogio);
        votacao.registrarLembrete(agora);
        votacaoService.atualizar(votacao);

        for (Usuario membro : destinatarios) {
            enviador.enviar(new EnviadorDeLembreteDeVotacao.Mensagem(
                    votacao.getId() + ":" + membro.getId() + ":" + agora,
                    votacao.getId(),
                    membro.getEmail(),
                    votacao.getClube().getNome(),
                    votacao.getDataEncerramento()));
        }

        log.info("Lembrete de votação votacao={} faltam={} notificados={}",
                votacao.getId(), faltam.size(), destinatarios.size());
        return new Resultado(destinatarios.size(), agora);
    }
}
