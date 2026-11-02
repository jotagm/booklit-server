-- Foto de perfil: só a chave do objeto no S3 (fotos/<usuario>/<uuid>.<ext>).
-- Com ddl-auto=update o Hibernate cria a coluna sozinho; este script é para quem aplica à mão.
ALTER TABLE t_usuario ADD COLUMN IF NOT EXISTS foto_chave varchar(200);
