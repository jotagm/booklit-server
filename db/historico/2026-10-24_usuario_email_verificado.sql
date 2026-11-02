-- Confirmação do e-mail da conta (login com código por e-mail).
-- Nulo = ainda não provou a posse do endereço; preenchido no primeiro login com código ou
-- no POST /auth/otp/email/verificacao com finalidade VERIFICAR_EMAIL.
-- O ddl-auto=update cria a coluna sozinho; o script documenta e serve para aplicar à mão.

ALTER TABLE t_usuario ADD COLUMN IF NOT EXISTS email_verificado_em TIMESTAMP;
