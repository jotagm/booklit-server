-- Sequência de semanas de leitura (🔥 na home e no perfil, mocks 1a, 1e e 2b) e preferência de
-- lembretes por e-mail (tela de configurações).

-- Um dia em que a pessoa registrou progresso (no clube ou na estante). Só a data importa:
-- várias atualizações no mesmo dia viram uma linha.
CREATE TABLE IF NOT EXISTS t_dia_leitura (
    id         UUID    PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES t_usuario (id) ON DELETE CASCADE,
    dia        DATE    NOT NULL,
    CONSTRAINT uk_dia_leitura_usuario UNIQUE (usuario_id, dia)
);

-- Ponto de partida: o histórico de progresso não era guardado, então o melhor que dá é a
-- última atualização de cada registro de clube e de cada livro da estante.
INSERT INTO t_dia_leitura (id, usuario_id, dia)
SELECT gen_random_uuid(), usuario_id, dia
FROM (
    SELECT usuario_id, CAST(updated_at AS DATE) AS dia
    FROM t_registro
    WHERE usuario_id IS NOT NULL AND updated_at IS NOT NULL AND COALESCE(valor_atual, 0) > 0
    UNION
    SELECT usuario_id, CAST(updated_at AS DATE) AS dia
    FROM t_leitura_pessoal
    WHERE updated_at IS NOT NULL AND COALESCE(pagina_atual, 0) > 0
) dias
ON CONFLICT (usuario_id, dia) DO NOTHING;

-- Nulo conta como "sim": quem nunca mexeu na preferência continua recebendo.
ALTER TABLE t_usuario ADD COLUMN IF NOT EXISTS receber_lembretes BOOLEAN DEFAULT TRUE;
