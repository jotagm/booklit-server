# Scripts de antes do Flyway

Até nov/2026 o schema era criado pelo Hibernate (`ddl-auto: update`) e estes scripts eram
aplicados à mão quando o `update` não dava conta (troca de tipo, limpeza de duplicatas,
índices parciais).

Eles ficam aqui só como histórico. **Não rode nenhum deles.** O estado final que eles
produziam já está nas migrações do Flyway:

- `src/main/resources/db/migration/V1__schema_inicial.sql`: o schema que o Hibernate gerava
  a partir das entidades.
- `src/main/resources/db/migration/V2__reconcilia_bancos_antigos.sql`: cria, num banco antigo,
  as tabelas e colunas da V1 que ainda não existirem.
- `src/main/resources/db/migration/V3__indices_de_consulta.sql`: os índices e restrições que
  só existiam nestes scripts.

## Mudanças de schema daqui pra frente

1. Crie `src/main/resources/db/migration/V<n>__descricao.sql` com o próximo número.
2. Ajuste a entidade. Com `ddl-auto: validate`, a aplicação não sobe se as duas divergirem,
   e o `MigracoesPostgresTest` pega isso no CI antes do deploy.
3. Nunca edite uma migração que já rodou em produção: o Flyway confere o checksum e a
   aplicação não sobe. Corrija com uma migração nova.

Atenção a nomes de constraint: no banco de produção eles podem não ser os da V1. O script de
2026-09-17 recriou as FKs para `t_usuario` com o nome `fk_<tabela>_<coluna>`, e os checks de
enum foram criados quando cada tabela nasceu, com os valores daquela época. Uma migração que
mexa numa constraint existente deve usar `IF EXISTS` ou procurar o nome em `pg_constraint`,
não o nome que aparece na V1.

Atenção a enums gravados como texto: o Hibernate gera um `check (coluna in (...))` na criação
da tabela. Ao adicionar um valor ao enum, a migração precisa recriar esse check, senão o banco
recusa o valor novo.
