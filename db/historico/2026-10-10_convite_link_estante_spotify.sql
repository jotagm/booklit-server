-- Link de convite do clube, estante pessoal, meta anual e Spotify na leitura.
-- O ddl-auto=update cria tudo isto sozinho no próximo deploy; o script existe para
-- documentar a mudança e para aplicar à mão quando o banco migrar para Flyway/validate.

-- Link de convite (booklit.bio/convite/<codigo>)
ALTER TABLE t_clube ADD COLUMN IF NOT EXISTS codigo_convite VARCHAR(16);
CREATE UNIQUE INDEX IF NOT EXISTS uk_clube_codigo_convite ON t_clube (codigo_convite);

-- Spotify da leitura (player embutido; link normalizado)
ALTER TABLE t_leitura_clube ADD COLUMN IF NOT EXISTS spotify_url VARCHAR(255);

-- Estante pessoal
CREATE TABLE IF NOT EXISTS t_leitura_pessoal (
    id               UUID PRIMARY KEY,
    usuario_id       INTEGER      NOT NULL REFERENCES t_usuario (id),
    livro_google_id  VARCHAR(255) NOT NULL,
    livro_titulo     VARCHAR(255) NOT NULL,
    livro_autor      VARCHAR(255),
    livro_capa_url   VARCHAR(255),
    status           VARCHAR(20)  NOT NULL,
    total_paginas    INTEGER,
    pagina_atual     INTEGER      NOT NULL DEFAULT 0,
    nota             INTEGER,
    resenha          TEXT,
    data_inicio      DATE,
    data_fim         DATE,
    clube_origem_id  UUID,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    CONSTRAINT uk_leitura_pessoal_usuario_livro UNIQUE (usuario_id, livro_google_id),
    CONSTRAINT ck_leitura_pessoal_nota CHECK (nota IS NULL OR nota BETWEEN 1 AND 5)
);

CREATE INDEX IF NOT EXISTS idx_leitura_pessoal_usuario_status
    ON t_leitura_pessoal (usuario_id, status);

-- Meta anual de livros
CREATE TABLE IF NOT EXISTS t_meta_leitura (
    id                 UUID PRIMARY KEY,
    usuario_id         INTEGER NOT NULL REFERENCES t_usuario (id),
    ano                INTEGER NOT NULL,
    quantidade_livros  INTEGER NOT NULL,
    CONSTRAINT uk_meta_leitura_usuario_ano UNIQUE (usuario_id, ano)
);
