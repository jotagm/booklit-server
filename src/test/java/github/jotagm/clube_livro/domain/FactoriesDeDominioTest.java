package github.jotagm.clube_livro.domain;

import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.ClubeStatus;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.convite.Convite;
import github.jotagm.clube_livro.domain.clube.convite.ConviteStatus;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.leitura.Registro;
import github.jotagm.clube_livro.domain.clube.votacao.OpcaoVoto;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus;
import github.jotagm.clube_livro.domain.clube.votacao.Voto;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As factories existem para que o estado inicial não seja escolha de quem constrói. Estes
 * testes fixam justamente as invariantes que deixaram de estar espalhadas por controllers
 * e services.
 */
class FactoriesDeDominioTest {

    @Test
    void clubeNasceAtivoEDatado() {
        Clube clube = Clube.novo("Leitores", "descrição", true, null);

        assertThat(clube.getStatus()).isEqualTo(ClubeStatus.ATIVO);
        assertThat(clube.getCreatedAt()).isNotNull();
        assertThat(clube.isPrivado()).isTrue();
    }

    @Test
    void conviteNascePendenteEDatado() {
        Convite convite = Convite.novo(new Clube(), new Usuario(), "alguem@teste.dev",
                LocalDateTime.now().plusDays(7));

        assertThat(convite.getStatus()).isEqualTo(ConviteStatus.PENDENTE);
        assertThat(convite.getCreatedAt()).isNotNull();
    }

    @Test
    void votacaoNasceAberta() {
        LocalDateTime abertura = LocalDateTime.now();
        Votacao votacao = Votacao.abrir(new Clube(), abertura, abertura.plusDays(7));

        assertThat(votacao.getStatus()).isEqualTo(VotacaoStatus.ABERTA);
    }

    @Test
    void usuarioNasceDatado() {
        Usuario usuario = Usuario.novo("Fulano", "fulano@teste.dev", "senha-em-texto-puro");

        assertThat(usuario.getCreatedAt()).isNotNull();
        // O hash é responsabilidade do serviço; a factory só transporta o valor recebido.
        assertThat(usuario.getSenhaHash()).isEqualTo("senha-em-texto-puro");
    }

    @Test
    void registroNasceEmZero() {
        Registro registro = Registro.iniciar(new LeituraClube(), new Usuario());

        assertThat(registro.getValorAtual()).isZero();
        assertThat(registro.getUpdatedAt()).isNotNull();
    }

    @Test
    void votoDoLiderVale2EDoMembroVale1() {
        Voto doLider = Voto.registrar(new Votacao(), new OpcaoVoto(), new Usuario(), ClubePapel.LIDER);
        Voto doMembro = Voto.registrar(new Votacao(), new OpcaoVoto(), new Usuario(), ClubePapel.MEMBRO);

        assertThat(doLider.getPeso()).isEqualTo(2);
        assertThat(doMembro.getPeso()).isEqualTo(1);
    }

    @Test
    void fundadorDoClubeEntraComoLider() {
        UsuarioClube vinculo = UsuarioClube.lider(new Usuario(), new Clube());

        assertThat(vinculo.getPapel()).isEqualTo(ClubePapel.LIDER);
        assertThat(vinculo.getEntrouEm()).isNotNull();
    }

    @Test
    void leituraDoLivroVencedorNasceSemMetaNemDatas() {
        // A votação escolhe o livro, não o ritmo — o líder define isso depois.
        LeituraClube leitura = LeituraClube.doLivroVencedor(new Clube(), "g-1", "Duna", "capa");

        assertThat(leitura.getLivroTitulo()).isEqualTo("Duna");
        assertThat(leitura.getDataInicio()).isNull();
        assertThat(leitura.getDataFim()).isNull();
        assertThat(leitura.getValorMeta()).isZero();
    }
}
