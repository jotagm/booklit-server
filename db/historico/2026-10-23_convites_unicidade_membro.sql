-- Uma pessoa entra num clube uma vez só (uk_usuario_clube em UsuarioClube).
-- Antes, aceitar o mesmo convite duas vezes, ou aceitar depois de entrar pelo link,
-- criava outra linha de membro.
--
-- O ddl-auto=update cria a constraint sozinho num banco limpo (o RDS novo), mas falha se
-- já houver duplicatas. Para um banco antigo: rode o DELETE antes, que mantém a linha mais
-- antiga de cada par (usuario, clube) e preserva o papel de LIDER quando houver.

DELETE FROM t_usuario_clube uc
USING t_usuario_clube outra
WHERE uc.usuario_id = outra.usuario_id
  AND uc.clube_id = outra.clube_id
  AND uc.id <> outra.id
  AND (
        (uc.papel <> 'LIDER' AND outra.papel = 'LIDER')
     OR (uc.papel = outra.papel AND (uc.entrou_em, uc.id::text) > (outra.entrou_em, outra.id::text))
  );

ALTER TABLE t_usuario_clube
    ADD CONSTRAINT uk_usuario_clube UNIQUE (usuario_id, clube_id);
