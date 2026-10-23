-- Desafios de OTP por e-mail (auth-core, fatia 1).
-- O ddl-auto=update cria a tabela sozinho; o script documenta e serve para aplicar à mão
-- quando o banco migrar para Flyway/validate.
--
-- O código nunca é gravado: codigo_hash é HMAC-SHA256(chave do servidor, id|finalidade|codigo).

CREATE TABLE IF NOT EXISTS t_desafio_otp (
    id                   UUID PRIMARY KEY,
    destinatario         VARCHAR(254) NOT NULL,
    finalidade           VARCHAR(30)  NOT NULL,
    codigo_hash          VARCHAR(64)  NOT NULL,
    criado_em            TIMESTAMPTZ  NOT NULL,
    expira_em            TIMESTAMPTZ  NOT NULL,
    tentativas_restantes INTEGER      NOT NULL CHECK (tentativas_restantes >= 0),
    consumido_em         TIMESTAMPTZ
);

-- Limite de reenvio: último envio e contagem por janela, por e-mail e finalidade.
CREATE INDEX IF NOT EXISTS idx_desafio_otp_destinatario
    ON t_desafio_otp (destinatario, finalidade, criado_em);
