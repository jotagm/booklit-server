package github.jotagm.clube_livro.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Relógio para testes de unidade. Sem {@link #fixarEm}, anda junto com o relógio do sistema
 * no fuso padrão da JVM, igual ao {@code LocalDateTime.now()} que os testes usam para montar
 * os dados; fixado, para de andar e só muda com {@link #avancar}.
 *
 * <p>Não é final para o Mockito poder usá-lo como {@code @Spy} e injetá-lo pelo
 * {@code @InjectMocks}.
 */
public class RelogioDeTeste extends Clock {

    private final ZoneId fuso;
    private Instant fixo;

    public RelogioDeTeste() {
        this(ZoneId.systemDefault());
    }

    public RelogioDeTeste(ZoneId fuso) {
        this.fuso = fuso;
    }

    public RelogioDeTeste fixarEm(Instant instante) {
        this.fixo = instante;
        return this;
    }

    public void avancar(Duration duracao) {
        fixo = instant().plus(duracao);
    }

    @Override
    public ZoneId getZone() {
        return fuso;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        RelogioDeTeste outro = new RelogioDeTeste(zone);
        outro.fixo = fixo;
        return outro;
    }

    @Override
    public Instant instant() {
        return fixo != null ? fixo : Instant.now();
    }
}
