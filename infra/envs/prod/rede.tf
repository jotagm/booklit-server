# VPC padrão da conta: sub-redes públicas em todas as AZs, sem NAT Gateway (~US$ 32/mês que
# não cabem no orçamento). A EC2 fica numa sub-rede pública atrás do security group; o RDS
# fica sem IP público e só aceita conexão vinda do security group da API.

data "aws_vpc" "padrao" {
  default = true
}

data "aws_subnets" "padrao" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.padrao.id]
  }
  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

# Nem toda AZ oferece todo tipo de instância (us-east-1e não tem Graviton, por exemplo).
data "aws_ec2_instance_type_offerings" "api" {
  location_type = "availability-zone"
  filter {
    name   = "instance-type"
    values = [var.tipo_instancia]
  }
}

data "aws_subnets" "api" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.padrao.id]
  }
  filter {
    name   = "default-for-az"
    values = ["true"]
  }
  filter {
    name   = "availability-zone"
    values = data.aws_ec2_instance_type_offerings.api.locations
  }
}

data "aws_subnet" "api" {
  id = sort(data.aws_subnets.api.ids)[0]
}

resource "aws_security_group" "api" {
  name        = "booklit-prod-api"
  description = "API do Booklit: HTTP e HTTPS publicos (Caddy na frente do Spring)"
  vpc_id      = data.aws_vpc.padrao.id
}

resource "aws_vpc_security_group_ingress_rule" "api_http" {
  security_group_id = aws_security_group.api.id
  description       = "HTTP (desafio ACME e redirect para HTTPS)"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
}

resource "aws_vpc_security_group_ingress_rule" "api_https" {
  security_group_id = aws_security_group.api.id
  description       = "HTTPS"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}

resource "aws_vpc_security_group_egress_rule" "api_saida" {
  security_group_id = aws_security_group.api.id
  description       = "Saida livre (ECR, SSM, Google Books, Lets Encrypt)"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "aws_security_group" "banco" {
  name        = "booklit-prod-banco"
  description = "Postgres do Booklit: so a API entra"
  vpc_id      = data.aws_vpc.padrao.id
}

resource "aws_vpc_security_group_ingress_rule" "banco_da_api" {
  security_group_id            = aws_security_group.banco.id
  description                  = "Postgres vindo da EC2 da API"
  referenced_security_group_id = aws_security_group.api.id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
}
