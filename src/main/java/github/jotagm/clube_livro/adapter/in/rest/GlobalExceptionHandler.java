package github.jotagm.clube_livro.adapter.in.rest;

import github.jotagm.clube_livro.adapter.in.rest.dto.response.ErroResponse;
import github.jotagm.clube_livro.domain.exceptions.AcessoNegadoException;
import github.jotagm.clube_livro.domain.exceptions.ComentarioAninhadoException;
import github.jotagm.clube_livro.domain.exceptions.IntervaloInvalidoException;
import github.jotagm.clube_livro.domain.exceptions.LivroExternoIndisponivelException;
import github.jotagm.clube_livro.domain.exceptions.RecursoNaoEncontradoException;
import github.jotagm.clube_livro.domain.exceptions.UsuarioJaVotouException;
import github.jotagm.clube_livro.domain.exceptions.VotacaoSemVotosException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Identifica a coluna que estourou a constraint, não o valor.
     *
     * <p>Casa tanto com o Postgres ({@code Key (email)=(...)}) quanto com o H2 usado nos
     * testes ({@code T_USUARIO(EMAIL NULLS FIRST)}). Exigir que "email" venha logo depois do
     * parêntese é o que evita confundir com o valor que colidiu — ali o parêntese é seguido
     * do e-mail em si, como em {@code =(fulano@email.com)}.
     */
    private static final Pattern COLUNA_EMAIL = Pattern.compile("\\(\\s*email\\b", Pattern.CASE_INSENSITIVE);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex,
                                                                   HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> tratarAcessoNegado(AcessoNegadoException ex,
                                                            HttpServletRequest request) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(UsuarioJaVotouException.class)
    public ResponseEntity<ErroResponse> tratarUsuarioJaVotou(UsuarioJaVotouException ex,
                                                              HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(VotacaoSemVotosException.class)
    public ResponseEntity<ErroResponse> tratarVotacaoSemVotos(VotacaoSemVotosException ex,
                                                               HttpServletRequest request) {
        return construir(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(LivroExternoIndisponivelException.class)
    public ResponseEntity<ErroResponse> tratarLivroExternoIndisponivel(LivroExternoIndisponivelException ex,
                                                                        HttpServletRequest request) {
        return construir(HttpStatus.BAD_GATEWAY, ex.getMessage(), request);
    }

    @ExceptionHandler(ComentarioAninhadoException.class)
    public ResponseEntity<ErroResponse> tratarComentarioAninhado(ComentarioAninhadoException ex,
                                                                  HttpServletRequest request) {
        return construir(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(IntervaloInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarIntervaloInvalido(IntervaloInvalidoException ex,
                                                                 HttpServletRequest request) {
        return construir(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> tratarResponseStatus(ResponseStatusException ex,
                                                              HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return construir(status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(), request);
    }

    /**
     * Sem este handler a validação de {@code @Valid} escapa para o dispatch de /error, que o
     * SecurityConfig bloqueia — o cliente recebia 403 com corpo vazio em vez de 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return construir(HttpStatus.BAD_REQUEST, mensagem.isBlank() ? "Requisição inválida" : mensagem, request);
    }

    /**
     * Falha de autenticação vira 401 com mensagem genérica.
     *
     * <p>A mensagem é deliberadamente vaga: o {@code loadUserByUsername} lança
     * "Usuário não encontrado", e repassar isso diria a qualquer um quais e-mails existem
     * na base. Senha errada e e-mail inexistente respondem igual.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> tratarAutenticacao(AuthenticationException ex,
                                                            HttpServletRequest request) {
        log.warn("Falha de autenticação em {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErroResponse(HttpStatus.UNAUTHORIZED.value(),
                        "E-mail ou senha inválidos", LocalDateTime.now()));
    }

    /**
     * Violação de constraint do banco — e-mail repetido no cadastro, por exemplo — vira 409
     * em vez do 500 que a rede de segurança abaixo daria.
     *
     * <p>O tratamento fica aqui, e não numa checagem antes de salvar, porque quem de fato
     * garante a unicidade é a constraint: dois cadastros simultâneos passariam os dois por um
     * {@code existsByEmail} e só um sobreviveria ao insert.
     *
     * <p>A mensagem do driver nunca é repassada ao cliente: ela carrega o valor que colidiu
     * ({@code Key (email)=(fulano@email.com)}) e o nome interno da constraint.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarViolacaoDeIntegridade(DataIntegrityViolationException ex,
                                                                     HttpServletRequest request) {
        String causa = ex.getMostSpecificCause().getMessage();
        log.warn("Violação de integridade em {} {}: {}", request.getMethod(), request.getRequestURI(), causa);

        boolean emailDuplicado = causa != null && COLUNA_EMAIL.matcher(causa).find();
        String mensagem = emailDuplicado
                ? "Já existe uma conta com este e-mail."
                : "Já existe um registro com esses dados.";

        return construir(HttpStatus.CONFLICT, mensagem, request);
    }

    /**
     * Rede de segurança para o que ninguém tratou. Sem ela a exceção seguia para /error,
     * virava 403 vazio e não deixava rastro nenhum no log.
     *
     * <p>Antes de desistir, procura uma exceção de domínio na cadeia de causas. Sem isso este
     * handler roubaria o desempacotamento que o Spring faz sozinho quando não há
     * {@code @ExceptionHandler(Exception.class)} — foi assim que o 404 de recurso não
     * encontrado virou 403 vazio na primeira versão.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarInesperado(Exception ex, HttpServletRequest request) {
        if (ex instanceof AccessDeniedException) {
            throw (AccessDeniedException) ex;
        }

        for (Throwable causa = ex.getCause(); causa != null; causa = causa.getCause()) {
            HttpStatus status = statusDeDominio(causa);
            if (status != null) {
                return construir(status, causa.getMessage(), request);
            }
        }

        log.error("Erro não tratado em {} {}", request.getMethod(), request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Erro interno. Tente novamente.", LocalDateTime.now()));
    }

    private HttpStatus statusDeDominio(Throwable causa) {
        if (causa instanceof RecursoNaoEncontradoException) return HttpStatus.NOT_FOUND;
        if (causa instanceof AcessoNegadoException) return HttpStatus.FORBIDDEN;
        if (causa instanceof UsuarioJaVotouException) return HttpStatus.CONFLICT;
        if (causa instanceof VotacaoSemVotosException) return HttpStatus.UNPROCESSABLE_ENTITY;
        if (causa instanceof ComentarioAninhadoException) return HttpStatus.UNPROCESSABLE_ENTITY;
        if (causa instanceof IntervaloInvalidoException) return HttpStatus.UNPROCESSABLE_ENTITY;
        if (causa instanceof LivroExternoIndisponivelException) return HttpStatus.BAD_GATEWAY;
        return null;
    }

    private ResponseEntity<ErroResponse> construir(HttpStatus status, String mensagem, HttpServletRequest request) {
        // 5xx é problema nosso e merece stack trace; 4xx é o cliente errando e vira uma linha só.
        if (status.is5xxServerError()) {
            log.error("{} em {} {}: {}", status.value(), request.getMethod(), request.getRequestURI(), mensagem);
        } else {
            log.warn("{} em {} {}: {}", status.value(), request.getMethod(), request.getRequestURI(), mensagem);
        }

        return ResponseEntity.status(status)
                .body(new ErroResponse(status.value(), mensagem, LocalDateTime.now()));
    }
}
