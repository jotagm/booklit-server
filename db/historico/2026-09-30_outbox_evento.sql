CREATE TABLE IF NOT EXISTS t_outbox_evento (
                                               id           UUID PRIMARY KEY,
                                               tipo         VARCHAR(100) NOT NULL,
    agregado_id  UUID         NOT NULL,
    payload      TEXT         NOT NULL,
    criado_em    TIMESTAMPTZ  NOT NULL,
    publicado_em TIMESTAMPTZ
    );

CREATE INDEX IF NOT EXISTS idx_outbox_evento_pendentes
    ON t_outbox_evento (criado_em)
    WHERE publicado_em IS NULL;