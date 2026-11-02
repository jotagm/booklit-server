-- Feed mais social e "ponto de novidade" (mocks 2b e 2c, implementacao_web).

-- Sugestão de livro vira item do feed ("Ana sugeriu X para a votação"). As sugestões antigas
-- ficam com created_at nulo e não aparecem no feed: não há como saber quando foram feitas.
ALTER TABLE t_opcao_voto ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6);

-- Última vez que cada membro abriu cada clube. O ponto de novidade conta a atividade depois disso.
CREATE TABLE IF NOT EXISTS t_visita_clube (
    id         UUID         PRIMARY KEY,
    usuario_id INTEGER      NOT NULL REFERENCES t_usuario (id) ON DELETE CASCADE,
    clube_id   UUID         NOT NULL REFERENCES t_clube (id) ON DELETE CASCADE,
    visto_em   TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_visita_clube_usuario UNIQUE (usuario_id, clube_id)
);

-- O feed busca as respostas das conversas pelo comentário pai.
CREATE INDEX IF NOT EXISTS idx_comentario_pai ON t_comentario (comentario_pai_id);
