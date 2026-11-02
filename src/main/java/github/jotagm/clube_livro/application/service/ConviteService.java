package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.ConviteRepository;
import github.jotagm.clube_livro.application.port.out.EnviadorDeConvite;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.convite.Convite;
import github.jotagm.clube_livro.domain.clube.convite.ConviteStatus;
import github.jotagm.clube_livro.domain.exceptions.AcessoNegadoException;
import github.jotagm.clube_livro.domain.exceptions.ConflitoException;
import github.jotagm.clube_livro.domain.exceptions.LimiteExcedidoException;
import github.jotagm.clube_livro.domain.exceptions.RecursoExpiradoException;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import github.jotagm.clube_livro.domain.exceptions.RegraDeNegocioException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class ConviteService {

    private final ConviteRepository conviteRepository;
    private final UsuarioClubeService usuarioClubeService;
    private final UsuarioService usuarioService;
    private final ClubeService clubeService;
    private final LeituraClubeService leituraClubeService;
    private final EnviadorDeConvite enviadorDeConvite;
    private final Clock relogio;

    /** Teto de validade: um convite por e-mail esquecido por meses não deveria continuar valendo. */
    static final Duration VALIDADE_MAXIMA = Duration.ofDays(30);

    /**
     * Teto de convites por e-mail que uma pessoa cria por hora. Cada convite dispara um e-mail
     * para um endereço de terceiros, então sem teto um líder (ou uma conta invadida) viraria
     * um disparador de spam com o domínio do Booklit.
     */
    static final int MAX_CONVITES_POR_HORA = 20;

    /**
     * Cria um convite por e-mail. Recusa expiração fora de (agora, agora + 30 dias], convite
     * para quem já é membro e um segundo convite pendente para o mesmo e-mail no mesmo clube.
     */
    @Transactional
    public Convite criar(Clube clube, Usuario convidadoPor, String email, LocalDateTime expiraEm) {
        String destinatario = email.strip().toLowerCase(Locale.ROOT);
        LocalDateTime agora = LocalDateTime.now(relogio);

        if (!expiraEm.isAfter(agora)) {
            throw new RegraDeNegocioException("A expiração do convite precisa estar no futuro");
        }
        if (expiraEm.isAfter(agora.plus(VALIDADE_MAXIMA))) {
            throw new RegraDeNegocioException("O convite pode valer no máximo " + VALIDADE_MAXIMA.toDays() + " dias");
        }
        boolean jaEMembro = usuarioClubeService.listarPorClube(clube.getId()).stream()
                .anyMatch(uc -> uc.getUsuario().getEmail().equalsIgnoreCase(destinatario));
        if (jaEMembro) {
            throw new ConflitoException("Essa pessoa já é membro do clube");
        }
        if (conviteRepository.existsByClubeIdAndEmailDestinatarioIgnoreCaseAndStatusAndExpiraEmAfter(
                clube.getId(), destinatario, ConviteStatus.PENDENTE, agora)) {
            throw new ConflitoException("Já existe um convite pendente para este e-mail neste clube");
        }

        if (conviteRepository.countByConvidadoPorIdAndCreatedAtAfter(convidadoPor.getId(), agora.minusHours(1))
                >= MAX_CONVITES_POR_HORA) {
            throw new LimiteExcedidoException("Muitos convites em pouco tempo. Tente de novo mais tarde.");
        }

        Convite convite = conviteRepository.save(Convite.novo(clube, convidadoPor, destinatario, expiraEm));
        log.info("Convite criado id={} clube={}", convite.getId(), clube.getId());
        // Depois do commit (o enviador cuida disso); se falhar, o convite continua valendo.
        enviadorDeConvite.enviar(new EnviadorDeConvite.Mensagem(
                convite.getId(), destinatario, convidadoPor.getNome(), clube.getNome()));
        return convite;
    }

    public Convite salvar(Convite convite) {
        return conviteRepository.save(convite);
    }

    public Convite buscarPorId(UUID id) {
        return conviteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Convite não encontrado"));
    }

    /**
     * Aceita uma vez só (já respondido vira 409). Quem já entrou no clube pelo link só tem o
     * convite marcado como aceito, sem ganhar uma segunda linha de membro.
     */
    @Transactional
    public Convite aceitarConvite(UUID id, String emailUsuario) {
        Convite convite = buscarPorId(id);

        if (!convite.pertenceA(emailUsuario)) {
            throw new AcessoNegadoException("Acesso negado");
        }
        if (convite.estaPendente() && convite.estaExpirado(LocalDateTime.now(relogio))) {
            throw new RecursoExpiradoException("Convite expirado");
        }

        convite.aceitar();

        Usuario usuario = usuarioService.buscarPorEmail(emailUsuario);
        if (!ehMembro(usuario, convite.getClube())) {
            adicionarComoMembro(usuario, convite.getClube());
        }

        log.info("Convite aceito id={} clube={} usuario={}",
                convite.getId(), convite.getClube().getId(), emailUsuario);

        return conviteRepository.save(convite);
    }

    @Transactional
    public Convite rejeitarConvite(UUID id, String emailUsuario) {
        Convite convite = buscarPorId(id);

        if (!convite.pertenceA(emailUsuario)) {
            throw new AcessoNegadoException("Acesso negado");
        }

        convite.recusar();
        log.info("Convite recusado id={} clube={} usuario={}", convite.getId(), convite.getClube().getId(), emailUsuario);
        return conviteRepository.save(convite);
    }

    /** Convite mais recente para o e-mail — um mesmo e-mail pode ter sido convidado por vários clubes. */
    public Convite buscarPorEmailDestinatario(String email) {
        return conviteRepository.findByEmailDestinatarioIgnoreCaseOrderByCreatedAtDesc(email).stream()
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhum convite para este e-mail"));
    }

    /** Convites pendentes e ainda válidos endereçados ao e-mail do usuário — a caixa de entrada dele. */
    public List<Convite> listarPendentesDoUsuario(String email) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        return conviteRepository
                .findByEmailDestinatarioIgnoreCaseAndStatusOrderByCreatedAtDesc(email, ConviteStatus.PENDENTE)
                .stream()
                .filter(c -> !c.estaExpirado(agora))
                .toList();
    }

    /**
     * Entrada pelo link de convite. Idempotente: quem já é membro só é levado ao clube, sem
     * erro — o mesmo link costuma ser clicado mais de uma vez no grupo.
     */
    @Transactional
    public Clube entrarPorCodigo(String codigo, Usuario usuario) {
        Clube clube = clubeService.buscarPorCodigoConvite(codigo);
        if (!ehMembro(usuario, clube)) {
            adicionarComoMembro(usuario, clube);
            log.info("Entrada por link clube={} usuario={}", clube.getId(), usuario.getEmail());
        }
        return clube;
    }

    private boolean ehMembro(Usuario usuario, Clube clube) {
        return usuarioClubeService.isMembro(usuario.getId(), clube.getId());
    }

    private void adicionarComoMembro(Usuario usuario, Clube clube) {
        usuarioClubeService.adicionar(usuario, clube, ClubePapel.MEMBRO);
        leituraClubeService.iniciarRegistroNaLeituraAtiva(clube.getId(), usuario);
    }

    public List<Convite> listarPorClube(UUID clubeId) {
        return conviteRepository.findByClubeId(clubeId);
    }

    public Convite atualizar(Convite convite) {
        return conviteRepository.save(convite);
    }

    public void deletar(UUID id) {
        conviteRepository.deleteById(id);
    }

    @Transactional
    public int expirarVencidos() {
        List<Convite> vencidos = conviteRepository.findByStatusAndExpiraEmBefore(ConviteStatus.PENDENTE, LocalDateTime.now(relogio));
        vencidos.forEach(Convite::expirar);
        conviteRepository.saveAll(vencidos);
        if (!vencidos.isEmpty()) {
            log.info("Convites expirados automaticamente: {}", vencidos.size());
        }
        return vencidos.size();
    }
}