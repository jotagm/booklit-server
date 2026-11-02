-- Fase 3 do front "mais social" (ver implementacao_web/GUIA.md).

-- Comentário sem spoiler: onde o autor estava na leitura (na unidade da meta) ao comentar.
-- Comentários antigos ficam nulos e o front os mostra sem página e sem borrão.
ALTER TABLE t_comentario ADD COLUMN IF NOT EXISTS pagina_autor INTEGER;

-- Autor do livro: na opção de voto (para seguir até a leitura se ela vencer) e na leitura.
ALTER TABLE t_leitura_clube ADD COLUMN IF NOT EXISTS livro_autor VARCHAR(255);
ALTER TABLE t_opcao_voto    ADD COLUMN IF NOT EXISTS livro_autor VARCHAR(255);

-- "Lembrar quem não votou": o último lembrete do líder limita o próximo (um a cada 24h).
ALTER TABLE t_votacao ADD COLUMN IF NOT EXISTS ultimo_lembrete_em TIMESTAMP(6);

-- Reação ACENO (👋, boas-vindas a quem entrou). O CHECK do tipo veio sem nome (inline), e
-- bancos criados pelo ddl-auto podem tê-lo com outro nome: derruba qualquer CHECK que olhe
-- para "tipo" em t_reacao e recria um só, com nome fixo.
DO $$
DECLARE
    restricao RECORD;
BEGIN
    FOR restricao IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 't_reacao'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%tipo%'
    LOOP
        EXECUTE format('ALTER TABLE t_reacao DROP CONSTRAINT %I', restricao.conname);
    END LOOP;
END $$;

ALTER TABLE t_reacao
    ADD CONSTRAINT ck_reacao_tipo CHECK (tipo IN ('CURTIR', 'FOGO', 'PALMAS', 'ACENO'));
