CREATE TABLE IF NOT EXISTS t_evento_processado (
    id            UUID         PRIMARY KEY,
    tipo          VARCHAR(100) NOT NULL,
    processado_em TIMESTAMPTZ  NOT NULL
);
