package github.jotagm.auth.otp;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relógio que os testes avançam na mão, para expiração e limites sem sleep. */
final class RelogioAjustavel extends Clock {

    private volatile Instant agora;

    RelogioAjustavel(Instant inicio) {
        this.agora = inicio;
    }

    void avancar(Duration duracao) {
        agora = agora.plus(duracao);
    }

    @Override
    public Instant instant() {
        return agora;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
