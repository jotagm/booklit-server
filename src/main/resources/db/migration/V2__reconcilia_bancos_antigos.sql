-- Reconcilia bancos criados pelo ddl-auto (antes do Flyway) com o schema da V1.
--
-- Em produção a V1 não roda (baseline), então nada garante que o banco tenha todas as colunas
-- que as entidades esperam: uma coluna nova que ainda não tinha subido para lá faria o
-- ddl-auto=validate recusar a subida, e o deploy.sh voltaria para a imagem anterior com o
-- Flyway já marcado como aplicado, sem jeito de se corrigir sozinho.
--
-- Por isso, aqui, toda tabela e coluna da V1 é criada se faltar. Num banco novo (que acabou de
-- rodar a V1) nada disto muda coisa alguma. As colunas entram sem NOT NULL/CHECK/UNIQUE de
-- propósito, como o ddl-auto=update fazia: uma restrição numa tabela com dados antigos poderia
-- falhar a migração.


-- t_clube
create table if not exists t_clube (
    privado boolean not null,
    created_at timestamp(6),
    codigo_convite varchar(16),
    id uuid not null,
    descricao varchar(255),
    nome varchar(255),
    status varchar(255) check (status in ('ATIVO','ENCERRADO')),
    primary key (id)
);
alter table t_clube add column if not exists privado boolean;
alter table t_clube add column if not exists created_at timestamp(6);
alter table t_clube add column if not exists codigo_convite varchar(16);
alter table t_clube add column if not exists id uuid;
alter table t_clube add column if not exists descricao varchar(255);
alter table t_clube add column if not exists nome varchar(255);
alter table t_clube add column if not exists status varchar(255);

-- t_clube_tema
create table if not exists t_clube_tema (
    clube_id uuid not null,
    tema_id uuid not null
);
alter table t_clube_tema add column if not exists clube_id uuid;
alter table t_clube_tema add column if not exists tema_id uuid;

-- t_comentario
create table if not exists t_comentario (
    removido boolean not null,
    usuario_id integer,
    created_at timestamp(6),
    updated_at timestamp(6),
    comentario_pai_id uuid,
    id uuid not null,
    leitura_clube_id uuid,
    conteudo varchar(255),
    primary key (id)
);
alter table t_comentario add column if not exists removido boolean;
alter table t_comentario add column if not exists usuario_id integer;
alter table t_comentario add column if not exists created_at timestamp(6);
alter table t_comentario add column if not exists updated_at timestamp(6);
alter table t_comentario add column if not exists comentario_pai_id uuid;
alter table t_comentario add column if not exists id uuid;
alter table t_comentario add column if not exists leitura_clube_id uuid;
alter table t_comentario add column if not exists conteudo varchar(255);

-- t_convite
create table if not exists t_convite (
    convidado_por integer,
    created_at timestamp(6),
    expira_em timestamp(6),
    clube_id uuid,
    id uuid not null,
    email_destinatario varchar(255),
    status varchar(255) check (status in ('PENDENTE','ACEITO','RECUSADO','EXPIRADO')),
    primary key (id)
);
alter table t_convite add column if not exists convidado_por integer;
alter table t_convite add column if not exists created_at timestamp(6);
alter table t_convite add column if not exists expira_em timestamp(6);
alter table t_convite add column if not exists clube_id uuid;
alter table t_convite add column if not exists id uuid;
alter table t_convite add column if not exists email_destinatario varchar(255);
alter table t_convite add column if not exists status varchar(255);

-- t_desafio_otp
create table if not exists t_desafio_otp (
    tentativas_restantes integer not null,
    consumido_em timestamp(6) with time zone,
    criado_em timestamp(6) with time zone not null,
    expira_em timestamp(6) with time zone not null,
    id uuid not null,
    finalidade varchar(30) not null check (finalidade in ('LOGIN','VERIFICAR_EMAIL','REDEFINIR_PIN')),
    codigo_hash varchar(64) not null,
    destinatario varchar(254) not null,
    primary key (id)
);
alter table t_desafio_otp add column if not exists tentativas_restantes integer;
alter table t_desafio_otp add column if not exists consumido_em timestamp(6) with time zone;
alter table t_desafio_otp add column if not exists criado_em timestamp(6) with time zone;
alter table t_desafio_otp add column if not exists expira_em timestamp(6) with time zone;
alter table t_desafio_otp add column if not exists id uuid;
alter table t_desafio_otp add column if not exists finalidade varchar(30);
alter table t_desafio_otp add column if not exists codigo_hash varchar(64);
alter table t_desafio_otp add column if not exists destinatario varchar(254);

-- t_evento_processado
create table if not exists t_evento_processado (
    processado_em timestamp(6) with time zone not null,
    id uuid not null,
    tipo varchar(255) not null,
    primary key (id)
);
alter table t_evento_processado add column if not exists processado_em timestamp(6) with time zone;
alter table t_evento_processado add column if not exists id uuid;
alter table t_evento_processado add column if not exists tipo varchar(255);

-- t_leitura_clube
create table if not exists t_leitura_clube (
    valor_meta integer,
    data_fim timestamp(6),
    data_inicio timestamp(6),
    clube_id uuid,
    id uuid not null,
    livro_capa_url varchar(255),
    livro_google_id varchar(255),
    livro_titulo varchar(255),
    spotify_url varchar(255),
    tipo_meta varchar(255) check (tipo_meta in ('PAGINA','CAPITULO')),
    primary key (id)
);
alter table t_leitura_clube add column if not exists valor_meta integer;
alter table t_leitura_clube add column if not exists data_fim timestamp(6);
alter table t_leitura_clube add column if not exists data_inicio timestamp(6);
alter table t_leitura_clube add column if not exists clube_id uuid;
alter table t_leitura_clube add column if not exists id uuid;
alter table t_leitura_clube add column if not exists livro_capa_url varchar(255);
alter table t_leitura_clube add column if not exists livro_google_id varchar(255);
alter table t_leitura_clube add column if not exists livro_titulo varchar(255);
alter table t_leitura_clube add column if not exists spotify_url varchar(255);
alter table t_leitura_clube add column if not exists tipo_meta varchar(255);

-- t_leitura_pessoal
create table if not exists t_leitura_pessoal (
    data_fim date,
    data_inicio date,
    nota integer,
    pagina_atual integer,
    total_paginas integer,
    usuario_id integer not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    clube_origem_id uuid,
    id uuid not null,
    livro_autor varchar(255),
    livro_capa_url varchar(255),
    livro_google_id varchar(255) not null,
    livro_titulo varchar(255) not null,
    resenha TEXT,
    status varchar(255) not null check (status in ('QUERO_LER','LENDO','LIDO','ABANDONADO')),
    primary key (id),
    constraint uk_leitura_pessoal_usuario_livro unique (usuario_id, livro_google_id)
);
alter table t_leitura_pessoal add column if not exists data_fim date;
alter table t_leitura_pessoal add column if not exists data_inicio date;
alter table t_leitura_pessoal add column if not exists nota integer;
alter table t_leitura_pessoal add column if not exists pagina_atual integer;
alter table t_leitura_pessoal add column if not exists total_paginas integer;
alter table t_leitura_pessoal add column if not exists usuario_id integer;
alter table t_leitura_pessoal add column if not exists created_at timestamp(6);
alter table t_leitura_pessoal add column if not exists updated_at timestamp(6);
alter table t_leitura_pessoal add column if not exists clube_origem_id uuid;
alter table t_leitura_pessoal add column if not exists id uuid;
alter table t_leitura_pessoal add column if not exists livro_autor varchar(255);
alter table t_leitura_pessoal add column if not exists livro_capa_url varchar(255);
alter table t_leitura_pessoal add column if not exists livro_google_id varchar(255);
alter table t_leitura_pessoal add column if not exists livro_titulo varchar(255);
alter table t_leitura_pessoal add column if not exists resenha TEXT;
alter table t_leitura_pessoal add column if not exists status varchar(255);

-- t_meta_leitura
create table if not exists t_meta_leitura (
    ano integer not null,
    quantidade_livros integer not null,
    usuario_id integer not null,
    id uuid not null,
    primary key (id),
    constraint uk_meta_leitura_usuario_ano unique (usuario_id, ano)
);
alter table t_meta_leitura add column if not exists ano integer;
alter table t_meta_leitura add column if not exists quantidade_livros integer;
alter table t_meta_leitura add column if not exists usuario_id integer;
alter table t_meta_leitura add column if not exists id uuid;

-- t_opcao_voto
create table if not exists t_opcao_voto (
    sugerido_por integer,
    id uuid not null,
    votacao_id uuid,
    livro_capa_url varchar(255),
    livro_google_id varchar(255),
    livro_titulo varchar(255),
    primary key (id)
);
alter table t_opcao_voto add column if not exists sugerido_por integer;
alter table t_opcao_voto add column if not exists id uuid;
alter table t_opcao_voto add column if not exists votacao_id uuid;
alter table t_opcao_voto add column if not exists livro_capa_url varchar(255);
alter table t_opcao_voto add column if not exists livro_google_id varchar(255);
alter table t_opcao_voto add column if not exists livro_titulo varchar(255);

-- t_outbox_evento
create table if not exists t_outbox_evento (
    criado_em timestamp(6) with time zone not null,
    publicado_em timestamp(6) with time zone,
    agregado_id uuid not null,
    id uuid not null,
    payload text not null,
    tipo varchar(255) not null,
    primary key (id)
);
alter table t_outbox_evento add column if not exists criado_em timestamp(6) with time zone;
alter table t_outbox_evento add column if not exists publicado_em timestamp(6) with time zone;
alter table t_outbox_evento add column if not exists agregado_id uuid;
alter table t_outbox_evento add column if not exists id uuid;
alter table t_outbox_evento add column if not exists payload text;
alter table t_outbox_evento add column if not exists tipo varchar(255);

-- t_reacao
create table if not exists t_reacao (
    usuario_id integer not null,
    created_at timestamp(6),
    clube_id uuid not null,
    id uuid not null,
    alvo_id varchar(80) not null,
    tipo varchar(255) not null check (tipo in ('CURTIR','FOGO','PALMAS')),
    primary key (id),
    constraint uk_reacao_alvo_usuario unique (alvo_id, usuario_id)
);
alter table t_reacao add column if not exists usuario_id integer;
alter table t_reacao add column if not exists created_at timestamp(6);
alter table t_reacao add column if not exists clube_id uuid;
alter table t_reacao add column if not exists id uuid;
alter table t_reacao add column if not exists alvo_id varchar(80);
alter table t_reacao add column if not exists tipo varchar(255);

-- t_registro
create table if not exists t_registro (
    usuario_id integer,
    valor_atual integer,
    updated_at timestamp(6),
    id uuid not null,
    leitura_clube_id uuid,
    primary key (id)
);
alter table t_registro add column if not exists usuario_id integer;
alter table t_registro add column if not exists valor_atual integer;
alter table t_registro add column if not exists updated_at timestamp(6);
alter table t_registro add column if not exists id uuid;
alter table t_registro add column if not exists leitura_clube_id uuid;

-- t_tema
create table if not exists t_tema (
    id uuid not null,
    nome varchar(255),
    primary key (id)
);
alter table t_tema add column if not exists id uuid;
alter table t_tema add column if not exists nome varchar(255);

-- t_usuario
create table if not exists t_usuario (
    id integer generated by default as identity,
    created_at timestamp(6),
    email_verificado_em timestamp(6),
    bio varchar(160),
    foto_chave varchar(200),
    email varchar(255),
    nome varchar(255),
    senha_hash varchar(255),
    primary key (id)
);
alter table t_usuario add column if not exists id integer;
alter table t_usuario add column if not exists created_at timestamp(6);
alter table t_usuario add column if not exists email_verificado_em timestamp(6);
alter table t_usuario add column if not exists bio varchar(160);
alter table t_usuario add column if not exists foto_chave varchar(200);
alter table t_usuario add column if not exists email varchar(255);
alter table t_usuario add column if not exists nome varchar(255);
alter table t_usuario add column if not exists senha_hash varchar(255);

-- t_usuario_clube
create table if not exists t_usuario_clube (
    usuario_id integer,
    entrou_em timestamp(6),
    clube_id uuid,
    id uuid not null,
    papel varchar(255) check (papel in ('LIDER','MEMBRO')),
    primary key (id),
    constraint uk_usuario_clube unique (usuario_id, clube_id)
);
alter table t_usuario_clube add column if not exists usuario_id integer;
alter table t_usuario_clube add column if not exists entrou_em timestamp(6);
alter table t_usuario_clube add column if not exists clube_id uuid;
alter table t_usuario_clube add column if not exists id uuid;
alter table t_usuario_clube add column if not exists papel varchar(255);

-- t_votacao
create table if not exists t_votacao (
    data_abertura timestamp(6),
    data_encerramento timestamp(6),
    clube_id uuid,
    id uuid not null,
    status varchar(255) check (status in ('ABERTA','ENCERRADA')),
    primary key (id)
);
alter table t_votacao add column if not exists data_abertura timestamp(6);
alter table t_votacao add column if not exists data_encerramento timestamp(6);
alter table t_votacao add column if not exists clube_id uuid;
alter table t_votacao add column if not exists id uuid;
alter table t_votacao add column if not exists status varchar(255);

-- t_voto
create table if not exists t_voto (
    peso integer not null,
    usuario_id integer,
    id uuid not null,
    opcao_voto_id uuid,
    votacao_id uuid,
    primary key (id)
);
alter table t_voto add column if not exists peso integer;
alter table t_voto add column if not exists usuario_id integer;
alter table t_voto add column if not exists id uuid;
alter table t_voto add column if not exists opcao_voto_id uuid;
alter table t_voto add column if not exists votacao_id uuid;
