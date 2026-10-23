data "aws_caller_identity" "atual" {}

# Arquitetura derivada do tipo de instância: t4g -> arm64, t3 -> x86_64. A AMI e a
# plataforma da imagem Docker (saída plataforma_docker, usada pelo pipeline) seguem junto.
data "aws_ec2_instance_type" "api" {
  instance_type = var.tipo_instancia
}

locals {
  arquitetura = contains(data.aws_ec2_instance_type.api.supported_architectures, "arm64") ? "arm64" : "x86_64"
  log_group   = "/booklit/prod"
}

data "aws_ssm_parameter" "ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-${local.arquitetura}"
}

resource "aws_cloudwatch_log_group" "api" {
  name              = local.log_group
  retention_in_days = 14
}

# --- Papel da EC2 -------------------------------------------------------------------------

resource "aws_iam_role" "api" {
  name = "booklit-prod-api"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

# Session Manager (shell sem SSH) e o Run Command que o pipeline usa para o deploy.
resource "aws_iam_role_policy_attachment" "api_ssm" {
  role       = aws_iam_role.api.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy_attachment" "api_ecr" {
  role       = aws_iam_role.api.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_role_policy" "api" {
  name = "booklit-prod-api"
  role = aws_iam_role.api.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "LerConfiguracao"
        Effect = "Allow"
        Action = ["ssm:GetParametersByPath", "ssm:GetParameters", "ssm:GetParameter"]
        Resource = [
          "arn:aws:ssm:${var.regiao}:${data.aws_caller_identity.atual.account_id}:parameter${local.prefixo_ssm}",
          "arn:aws:ssm:${var.regiao}:${data.aws_caller_identity.atual.account_id}:parameter${local.prefixo_ssm}/*",
        ]
      },
      {
        Sid      = "PublicarEventos"
        Effect   = "Allow"
        Action   = ["sns:Publish"]
        Resource = [module.mensageria.topic_arn]
      },
      {
        Sid    = "ConsumirEventos"
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:ChangeMessageVisibility",
          "sqs:GetQueueAttributes", "sqs:GetQueueUrl",
        ]
        Resource = ["arn:aws:sqs:${var.regiao}:${data.aws_caller_identity.atual.account_id}:booklit-prod-*"]
      },
      {
        Sid      = "Logs"
        Effect   = "Allow"
        Action   = ["logs:CreateLogStream", "logs:PutLogEvents", "logs:DescribeLogStreams"]
        Resource = ["${aws_cloudwatch_log_group.api.arn}:*"]
      },
    ]
  })
}

resource "aws_iam_instance_profile" "api" {
  name = "booklit-prod-api"
  role = aws_iam_role.api.name
}

# --- Instância -----------------------------------------------------------------------------

resource "aws_instance" "api" {
  ami                    = data.aws_ssm_parameter.ami.insecure_value
  instance_type          = var.tipo_instancia
  subnet_id              = data.aws_subnet.api.id
  vpc_security_group_ids = [aws_security_group.api.id]
  iam_instance_profile   = aws_iam_instance_profile.api.name

  # IMDSv2 obrigatório: bloqueia o roubo de credenciais via SSRF no endpoint de metadados.
  # Hop limit 2 porque quem chama o IMDS de dentro do container (SDK da AWS) passa por um salto a mais.
  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = templatefile("${path.module}/arquivos/user_data.sh.tftpl", {
    regiao      = var.regiao
    log_group   = local.log_group
    prefixo_ssm = local.prefixo_ssm
    compose_b64 = base64encode(templatefile("${path.module}/arquivos/compose.yaml.tftpl", {
      regiao    = var.regiao
      log_group = local.log_group
    }))
    caddyfile_b64 = filebase64("${path.module}/arquivos/Caddyfile")
    deploy_b64 = base64encode(templatefile("${path.module}/arquivos/deploy.sh.tftpl", {
      regiao      = var.regiao
      prefixo_ssm = local.prefixo_ssm
    }))
  })

  # AMI nova ou mudança no user_data não devem recriar a máquina sozinhas num apply.
  # Para aplicar um user_data novo: terraform apply -replace=aws_instance.api
  lifecycle {
    ignore_changes = [ami, user_data]
  }

  tags = {
    Name = "booklit-prod-api"
  }
}

resource "aws_eip" "api" {
  instance = aws_instance.api.id
  domain   = "vpc"

  tags = {
    Name = "booklit-prod-api"
  }
}
