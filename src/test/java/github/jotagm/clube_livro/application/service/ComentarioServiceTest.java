package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.in.rest.dto.request.ComentarioAtualizarRequest;
import github.jotagm.clube_livro.adapter.in.rest.dto.request.ComentarioRequest;
import github.jotagm.clube_livro.adapter.out.persistence.ComentarioRepository;
import github.jotagm.clube_livro.domain.clube.Clube;
import github.jotagm.clube_livro.domain.clube.ClubePapel;
import github.jotagm.clube_livro.domain.clube.UsuarioClube;
import github.jotagm.clube_livro.domain.clube.leitura.Comentario;
import github.jotagm.clube_livro.domain.clube.leitura.LeituraClube;
import github.jotagm.clube_livro.domain.exceptions.AcessoNegadoException;
import github.jotagm.clube_livro.domain.exceptions.ComentarioAninhadoException;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import github.jotagm.clube_livro.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComentarioServiceTest {

    @Mock
    private ComentarioRepository comentarioRepository;

    @Mock
    private LeituraClubeService leituraClubeService;

    @Mock
    private UsuarioClubeService usuarioClubeService;

    @InjectMocks
    private ComentarioService comentarioService;

    private Usuario usuarioExemplo() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNome("Leitor");
        return usuario;
    }

    private LeituraClube leituraExemplo(UUID clubeId) {
        Clube clube = new Clube();
        clube.setId(clubeId);
        LeituraClube leitura = new LeituraClube();
        leitura.setId(UUID.randomUUID());
        leitura.setClube(clube);
        return leitura;
    }

    private UsuarioClube vinculo(ClubePapel papel) {
        return UsuarioClube.builder().papel(papel).build();
    }

    private Comentario comentarioExemplo(Usuario autor, LeituraClube leitura, Comentario pai) {
        return Comentario.builder()
                .id(UUID.randomUUID())
                .leituraClube(leitura)
                .usuario(autor)
                .comentarioPai(pai)
                .conteudo("Muito bom esse capítulo")
                .removido(false)
                .build();
    }

    @Test
    void criar_deveSalvarComentarioRaizQuandoUsuarioEhMembro() {
        UUID clubeId = UUID.randomUUID();
        Usuario autor = usuarioExemplo();
        LeituraClube leitura = leituraExemplo(clubeId);
        ComentarioRequest request = new ComentarioRequest(null, "Ótimo capítulo");

        when(leituraClubeService.buscarPorId(leitura.getId())).thenReturn(leitura);
        when(usuarioClubeService.buscarPorUsuarioEClube(autor.getId(), clubeId)).thenReturn(vinculo(ClubePapel.MEMBRO));
        when(comentarioRepository.save(any(Comentario.class))).thenAnswer(inv -> inv.getArgument(0));

        Comentario resultado = comentarioService.criar(leitura.getId(), request, autor);

        assertThat(resultado.getConteudo()).isEqualTo("Ótimo capítulo");
        assertThat(resultado.getComentarioPai()).isNull();
    }

    @Test
    void criar_deveLancarExcecaoQuandoUsuarioNaoEhMembro() {
        UUID clubeId = UUID.randomUUID();
        Usuario autor = usuarioExemplo();
        LeituraClube leitura = leituraExemplo(clubeId);
        ComentarioRequest request = new ComentarioRequest(null, "Ótimo capítulo");

        when(leituraClubeService.buscarPorId(leitura.getId())).thenReturn(leitura);
        when(usuarioClubeService.buscarPorUsuarioEClube(autor.getId(), clubeId))
                .thenThrow(new RecursoNaoEncontradoException("Usuário não é membro deste clube"));

        assertThatThrownBy(() -> comentarioService.criar(leitura.getId(), request, autor))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void criar_deveLancarExcecaoQuandoComentarioPaiJaEhResposta() {
        UUID clubeId = UUID.randomUUID();
        Usuario autor = usuarioExemplo();
        LeituraClube leitura = leituraExemplo(clubeId);
        Comentario raiz = comentarioExemplo(autor, leitura, null);
        Comentario resposta = comentarioExemplo(autor, leitura, raiz);
        ComentarioRequest request = new ComentarioRequest(resposta.getId(), "Concordo!");

        when(leituraClubeService.buscarPorId(leitura.getId())).thenReturn(leitura);
        when(usuarioClubeService.buscarPorUsuarioEClube(autor.getId(), clubeId)).thenReturn(vinculo(ClubePapel.MEMBRO));
        when(comentarioRepository.findById(resposta.getId())).thenReturn(Optional.of(resposta));

        assertThatThrownBy(() -> comentarioService.criar(leitura.getId(), request, autor))
                .isInstanceOf(ComentarioAninhadoException.class);
    }

    @Test
    void listarPorLeitura_deveLancarExcecaoQuandoNaoEhMembro() {
        UUID clubeId = UUID.randomUUID();
        Usuario solicitante = usuarioExemplo();
        LeituraClube leitura = leituraExemplo(clubeId);

        when(leituraClubeService.buscarPorId(leitura.getId())).thenReturn(leitura);
        when(usuarioClubeService.buscarPorUsuarioEClube(solicitante.getId(), clubeId))
                .thenThrow(new RecursoNaoEncontradoException("Usuário não é membro deste clube"));

        assertThatThrownBy(() -> comentarioService.listarPorLeitura(leitura.getId(), solicitante))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void editar_deveAtualizarConteudoQuandoAutor() {
        Usuario autor = usuarioExemplo();
        Comentario comentario = comentarioExemplo(autor, leituraExemplo(UUID.randomUUID()), null);
        ComentarioAtualizarRequest request = new ComentarioAtualizarRequest("Editado");

        when(comentarioRepository.findById(comentario.getId())).thenReturn(Optional.of(comentario));
        when(comentarioRepository.save(comentario)).thenReturn(comentario);

        Comentario resultado = comentarioService.editar(comentario.getId(), request, autor);

        assertThat(resultado.getConteudo()).isEqualTo("Editado");
        assertThat(resultado.getUpdatedAt()).isNotNull();
    }

    @Test
    void editar_deveLancarExcecaoQuandoNaoEhAutor() {
        Usuario autor = usuarioExemplo();
        Usuario outroUsuario = usuarioExemplo();
        Comentario comentario = comentarioExemplo(autor, leituraExemplo(UUID.randomUUID()), null);
        ComentarioAtualizarRequest request = new ComentarioAtualizarRequest("Editado");

        when(comentarioRepository.findById(comentario.getId())).thenReturn(Optional.of(comentario));

        assertThatThrownBy(() -> comentarioService.editar(comentario.getId(), request, outroUsuario))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void editar_deveLancarExcecaoQuandoComentarioRemovido() {
        Usuario autor = usuarioExemplo();
        Comentario comentario = comentarioExemplo(autor, leituraExemplo(UUID.randomUUID()), null);
        comentario.setRemovido(true);
        ComentarioAtualizarRequest request = new ComentarioAtualizarRequest("Editado");

        when(comentarioRepository.findById(comentario.getId())).thenReturn(Optional.of(comentario));

        assertThatThrownBy(() -> comentarioService.editar(comentario.getId(), request, autor))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void deletar_deveMarcarComoRemovidoQuandoAutor() {
        Usuario autor = usuarioExemplo();
        LeituraClube leitura = leituraExemplo(UUID.randomUUID());
        Comentario comentario = comentarioExemplo(autor, leitura, null);

        when(comentarioRepository.findById(comentario.getId())).thenReturn(Optional.of(comentario));

        comentarioService.deletar(comentario.getId(), autor);

        assertThat(comentario.isRemovido()).isTrue();
    }

    @Test
    void deletar_deveMarcarComoRemovidoQuandoLider() {
        Usuario autor = usuarioExemplo();
        Usuario lider = usuarioExemplo();
        LeituraClube leitura = leituraExemplo(UUID.randomUUID());
        Comentario comentario = comentarioExemplo(autor, leitura, null);

        when(comentarioRepository.findById(comentario.getId())).thenReturn(Optional.of(comentario));
        when(usuarioClubeService.buscarPorUsuarioEClube(lider.getId(), leitura.getClube().getId()))
                .thenReturn(vinculo(ClubePapel.LIDER));

        comentarioService.deletar(comentario.getId(), lider);

        assertThat(comentario.isRemovido()).isTrue();
    }

    @Test
    void deletar_deveLancarExcecaoQuandoNaoEhAutorNemLider() {
        Usuario autor = usuarioExemplo();
        Usuario outroMembro = usuarioExemplo();
        LeituraClube leitura = leituraExemplo(UUID.randomUUID());
        Comentario comentario = comentarioExemplo(autor, leitura, null);

        when(comentarioRepository.findById(comentario.getId())).thenReturn(Optional.of(comentario));
        when(usuarioClubeService.buscarPorUsuarioEClube(outroMembro.getId(), leitura.getClube().getId()))
                .thenReturn(vinculo(ClubePapel.MEMBRO));

        assertThatThrownBy(() -> comentarioService.deletar(comentario.getId(), outroMembro))
                .isInstanceOf(AcessoNegadoException.class);
    }
}
