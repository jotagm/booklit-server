-- "Esqueci minha senha". Token JWT emitido antes da última redefinição deixa de valer, para
-- quem redefine a senha por ter perdido o controle da conta derrubar as sessões abertas.
ALTER TABLE t_usuario ADD COLUMN IF NOT EXISTS senha_alterada_em TIMESTAMP(6);
