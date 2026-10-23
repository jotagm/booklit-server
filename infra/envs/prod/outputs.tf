# Os quatro primeiros viram variáveis do repositório no GitHub (Settings > Secrets and
# variables > Actions > Variables), com os nomes em maiúsculas que o workflow espera.

output "AWS_ROLE_ARN" {
  value = aws_iam_role.github_deploy.arn
}

output "AWS_REGION" {
  value = var.regiao
}

output "ECR_REPOSITORY" {
  value = aws_ecr_repository.api.repository_url
}

output "EC2_INSTANCE_ID" {
  value = aws_instance.api.id
}

output "DOCKER_PLATFORM" {
  value = "linux/${local.arquitetura}"
}

output "ip_publico" {
  description = "Aponte o registro A do api_dominio para este IP"
  value       = aws_eip.api.public_ip
}

output "url_api" {
  value = local.url_api
}

output "banco_endpoint" {
  value = aws_db_instance.banco.address
}

output "log_group" {
  value = aws_cloudwatch_log_group.api.name
}
