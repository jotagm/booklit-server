-- Índices e restrições que existiam só nos scripts manuais (db/historico/) e que o Hibernate
-- não cria a partir das entidades. Tudo idempotente: roda tanto num banco novo quanto no de
-- produção, onde parte disto pode já ter sido aplicada à mão.

-- O OutboxRelay busca os pendentes a cada 5s, do mais antigo para o mais novo.
CREATE INDEX IF NOT EXISTS idx_outbox_evento_pendentes
    ON t_outbox_evento (criado_em)
    WHERE publicado_em IS NULL;

-- Estante: listar por usuário e status é a consulta principal da tela.
CREATE INDEX IF NOT EXISTS idx_leitura_pessoal_usuario_status
    ON t_leitura_pessoal (usuario_id, status);

-- A nota da estante vai de 1 a 5 (a entidade já valida; o banco garante).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_leitura_pessoal_nota') THEN
        ALTER TABLE t_leitura_pessoal
            ADD CONSTRAINT ck_leitura_pessoal_nota CHECK (nota IS NULL OR nota BETWEEN 1 AND 5);
    END IF;
END $$;
