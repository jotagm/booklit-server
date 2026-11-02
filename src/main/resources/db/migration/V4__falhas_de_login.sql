-- Senhas erradas no POST /auth/login, para o limite de tentativas (ProtecaoDeLoginService).
-- A consulta é sempre "quantas desde X" por e-mail (com ou sem o IP) ou por IP, daí os dois índices.
-- O FalhasDeLoginLimpezaScheduler apaga o que tem mais de um dia.

CREATE TABLE t_falha_login (
    id          UUID         PRIMARY KEY,
    email       VARCHAR(254) NOT NULL,
    ip          VARCHAR(45),
    ocorrida_em TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_falha_login_email ON t_falha_login (email, ip, ocorrida_em);
CREATE INDEX idx_falha_login_ip    ON t_falha_login (ip, ocorrida_em);
