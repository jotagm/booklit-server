package github.jotagm.clube_livro.domain;

import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.convite.Convite;
import github.jotagm.clube_livro.domain.clube.convite.ConviteStatus;
import github.jotagm.clube_livro.domain.clube.leitura.Comentario;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.clube.leitura.Registro;
import github.jotagm.clube_livro.domain.clube.votacao.Votacao;
import github.jotagm.clube_livro.domain.clube.votacao.VotacaoStatus;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Antes estas mudanças de estado eram feitas por setters espalhados pelos serviços. Agora
 * cada uma tem nome de negócio e carrega os efeitos colaterais que a acompanham.
 */
class TransicoesDeDominioTest {

    @Test
    void conviteAceitoERecusadoMudamDeStatus() {
        Convite convite = Convite.novo(new Clube(), new Usuario(), "a@teste.dev",
                LocalDateTime.now().plusDays(1));

        convite.aceitar();
        assertThat(convite.getStatus()).isEqualTo(ConviteStatus.ACEITO);

        convite.recusar();
        assertThat(convite.getStatus()).isEqualTo(ConviteStatus.RECUSADO);

        convite.expirar();
        assertThat(convite.getStatus()).isEqualTo(ConviteStatus.EXPIRADO);
    }

    @Test
    void conviteSabeSeExpirouEDeQuemE() {
        LocalDateTime agora = LocalDateTime.now();
        Convite convite = Convite.novo(new Clube(), new Usuario(), "dono@teste.dev", agora.plusDays(1));

        assertThat(convite.estaExpirado(agora)).isFalse();
        assertThat(convite.estaExpirado(agora.plusDays(2))).isTrue();
        assertThat(convite.pertenceA("dono@teste.dev")).isTrue();
        assertThat(convite.pertenceA("outro@teste.dev")).isFalse();
    }

    @Test
    void votacaoEncerraEReagenda() {
        LocalDateTime abertura = LocalDateTime.now();
        Votacao votacao = Votacao.abrir(new Clube(), abertura, abertura.plusDays(7));

        votacao.encerrar();
        assertThat(votacao.getStatus()).isEqualTo(VotacaoStatus.ENCERRADA);

        LocalDateTime nova = abertura.plusDays(1);
        votacao.reagendar(nova, nova.plusDays(3));
        assertThat(votacao.getDataAbertura()).isEqualTo(nova);
        assertThat(votacao.getDataEncerramento()).isEqualTo(nova.plusDays(3));
    }

    @Test
    void progressoDoRegistroCarimbaAAtualizacao() {
        Registro registro = Registro.iniciar(new LeituraClube(), new Usuario());
        LocalDateTime antes = registro.getUpdatedAt();

        registro.registrarProgresso(120);

        assertThat(registro.getValorAtual()).isEqualTo(120);
        // O carimbo acompanhar o valor é o ponto de ter um método só para os dois.
        assertThat(registro.getUpdatedAt()).isAfterOrEqualTo(antes);
    }

    @Test
    void comentarioRemovidoPreservaOConteudoNoBanco() {
        Comentario comentario = Comentario.builder().conteudo("texto original").build();

        comentario.remover();

        assertThat(comentario.isRemovido()).isTrue();
        // Remoção é lógica: o texto continua lá, quem esconde é o ComentarioResponse.
        assertThat(comentario.getConteudo()).isEqualTo("texto original");
        assertThat(comentario.getUpdatedAt()).isNotNull();
    }

    @Test
    void comentarioEditadoTrocaConteudoECarimba() {
        Comentario comentario = Comentario.builder().conteudo("antes").build();

        comentario.editar("depois");

        assertThat(comentario.getConteudo()).isEqualTo("depois");
        assertThat(comentario.getUpdatedAt()).isNotNull();
    }

    @Test
    void membroMudaDePapel() {
        UsuarioClube vinculo = UsuarioClube.membro(new Usuario(), new Clube());
        assertThat(vinculo.ehLider()).isFalse();

        vinculo.mudarPapel(ClubePapel.LIDER);

        assertThat(vinculo.ehLider()).isTrue();
    }

    @Test
    void usuarioAtualizaDadosSemTocarNaSenha() {
        Usuario usuario = Usuario.novo("Antigo", "antigo@teste.dev", "hash-original");

        usuario.atualizarDados("Novo", "novo@teste.dev");

        assertThat(usuario.getNome()).isEqualTo("Novo");
        assertThat(usuario.getEmail()).isEqualTo("novo@teste.dev");
        assertThat(usuario.getSenhaHash()).isEqualTo("hash-original");
    }
}
