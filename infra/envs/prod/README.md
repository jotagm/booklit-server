# Booklit em produção na AWS

```
GitHub (push na main)
  └─ Actions: testes ─► imagem arm64 ─► ECR ─► SSM Run Command ─► EC2
                                                                   │
  Internet ──► Elastic IP :443 ─► Caddy (TLS Let's Encrypt) ─► Spring :8080
                                                                   │
                                     RDS Postgres 16 (privado) ◄───┤
                                     SNS ─► SQS (+DLQ) ◄───────────┤
                                     SSM Parameter Store (config) ─┘
                                     CloudWatch Logs /booklit/prod
```

| Peça | Escolha | Por quê |
|---|---|---|
| Computação | EC2 t4g.micro (ARM), Docker Compose | ~US$ 6/mês; cabe nos créditos |
| TLS | Caddy na própria EC2 | certificado grátis e automático; um ALB custaria ~US$ 16/mês |
| Banco | RDS db.t4g.micro, sem IP público | backup diário (7 dias), snapshot final, proteção contra exclusão |
| Rede | VPC padrão, sem NAT | NAT Gateway custaria ~US$ 32/mês |
| Segredos | SSM Parameter Store (SecureString) | gerados pelo Terraform; nunca passam pelo GitHub |
| CI → AWS | OIDC | nenhuma chave de acesso guardada no GitHub; só a `main` assume o papel |
| Deploy | SSM Run Command | sem SSH, sem porta 22 aberta |

Custo estimado em us-east-1: ~US$ 25/mês bruto (antes dos créditos).

## Primeiro deploy

**0. Pré-requisitos.** AWS CLI logado na conta com permissão de administrador
(`aws sts get-caller-identity` funcionando) e Terraform ≥ 1.6.

**1. Infra.**
```bash
cd infra/envs/prod
cp terraform.tfvars.example terraform.tfvars   # preencha google_books_api_key e email_alertas
terraform init
terraform apply
```
O RDS leva uns 10 minutos para ficar pronto. **Faça backup do `terraform.tfstate`**: ele fica só
na sua máquina e guarda as senhas geradas.

**2. Variáveis no GitHub.** Em *Settings → Secrets and variables → Actions → Variables*, crie
`AWS_ROLE_ARN`, `AWS_REGION`, `ECR_REPOSITORY`, `EC2_INSTANCE_ID` e `DOCKER_PLATFORM` com os
valores das saídas de mesmo nome (`terraform output`). Nenhuma delas é segredo.

**3. Deploy.** Espere uns 3 minutos depois do apply (a EC2 instala o Docker na primeira
inicialização) e faça push na `main`, ou rode o workflow à mão em *Actions → CI/CD → Run workflow*.
Teste: `curl http://<ip_publico>/v3/api-docs`.

**4. Domínio e HTTPS.** No DNS onde o `booklit.bio` está hoje, crie um registro
`A api.booklit.bio → <ip_publico>`. Depois:
```bash
terraform apply -var 'api_dominio=api.booklit.bio'   # ou ponha no terraform.tfvars
```
e rode o deploy de novo. O Caddy pede o certificado sozinho na primeira requisição.
Aponte o front para `https://api.booklit.bio`.

## Trazer os dados do banco atual

O RDS não tem IP público; a cópia passa pela EC2. Abra um shell nela sem SSH:
```bash
aws ssm start-session --target <EC2_INSTANCE_ID>
```
e lá dentro:
```bash
export PGPASSWORD=$(aws ssm get-parameter --name /booklit/prod/DATABASE_PASSWORD \
  --with-decryption --query Parameter.Value --output text)
sudo -E docker run --rm -e PGPASSWORD postgres:16-alpine sh -c \
  'pg_dump --no-owner --no-privileges "<URL do banco atual>" \
   | psql -v ON_ERROR_STOP=1 "host=<banco_endpoint> dbname=booklit user=booklit sslmode=require"'
```
Faça isso **antes** do primeiro deploy, ou com a API parada (`cd /opt/booklit && sudo docker compose stop api`):
senão o Hibernate cria as tabelas vazias primeiro e o restore colide com elas.

## Dia a dia

- **Mudar uma configuração:** edite no Terraform (ou direto no SSM) e rode o deploy de novo
  (`Run workflow`, ou `sudo /opt/booklit/deploy.sh <imagem>` pela sessão do SSM).
- **Voltar uma versão:** rode o `deploy.sh` com a tag (SHA) anterior; o ECR guarda as 10 últimas.
  Se a imagem nova não responder em 5 minutos, o próprio `deploy.sh` volta para a anterior.
- **Logs:** CloudWatch → Log groups → `/booklit/prod` (streams `api` e `caddy`), retenção de 14 dias.
- **Trocar a EC2** (AMI nova, mudança no user_data): `terraform apply -replace=aws_instance.api`
  e um deploy. A máquina não guarda estado; o certificado do Caddy é reemitido.

## Antes dos 6 meses

O plano Free da AWS fecha a conta quando os créditos acabam ou os 6 meses passam. Com o site em
produção, mude a conta para o *Paid plan* antes disso (Billing → Plano). O orçamento deste
Terraform (`email_alertas`) avisa quando o gasto bruto passa de 80% do limite mensal.
