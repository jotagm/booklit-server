-- A V1 (e a V2, para bancos antigos) criou t_desafio_otp com um CHECK fixo nas finalidades
-- do auth-core. O REDEFINIR_SENHA ("esqueci minha senha") ficou de fora e todo pedido de
-- código caía nessa regra. A regra é recriada com nome próprio e com todas as finalidades.
--
-- O nome da regra antiga foi gerado pelo Postgres (t_desafio_otp_finalidade_check, em geral),
-- por isso ela é achada pela definição, não pelo nome.
DO $$
DECLARE
    regra record;
BEGIN
    FOR regra IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 't_desafio_otp'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%finalidade%'
    LOOP
        EXECUTE format('ALTER TABLE t_desafio_otp DROP CONSTRAINT %I', regra.conname);
    END LOOP;
END $$;

ALTER TABLE t_desafio_otp ADD CONSTRAINT ck_desafio_otp_finalidade
    CHECK (finalidade IN ('LOGIN', 'VERIFICAR_EMAIL', 'REDEFINIR_PIN', 'REDEFINIR_SENHA'));
