# Configuração da aplicação no SSM Parameter Store, em /booklit/prod/<VARIAVEL>.
# O deploy.sh da EC2 lê tudo deste caminho e monta o arquivo de ambiente do container:
# para mudar uma variável, edite aqui (ou no console) e rode o deploy de novo.

locals {
  prefixo_ssm = "/booklit/prod"
  url_api     = var.api_dominio == "" ? "http://${aws_eip.api.public_ip}" : "https://${var.api_dominio}"

  # As chaves do mapa precisam ser conhecidas no plan (for_each); por isso as opcionais
  # entram por merge condicionado só a variáveis, nunca a valores calculados no apply.
  configuracao = merge(
    {
      JDBC_DATABASE_URL          = "jdbc:postgresql://${aws_db_instance.banco.address}:${aws_db_instance.banco.port}/${aws_db_instance.banco.db_name}?sslmode=require"
      DATABASE_USERNAME          = aws_db_instance.banco.username
      APP_URL                    = local.url_api
      AWS_REGION                 = var.regiao
      BOOKLIT_EVENTOS_HABILITADO = "true"
      BOOKLIT_EVENTOS_TOPICO_ARN = module.mensageria.topic_arn
      BOOKLIT_EVENTOS_FILA_URL   = module.mensageria.queue_urls["notifications"]
      # Só ligue com o domínio verificado (DKIM) no SES; ver email.tf.
      BOOKLIT_EMAIL_HABILITADO = tostring(var.email_habilitado)
      BOOKLIT_EMAIL_REMETENTE  = "no-reply@${var.dominio_email}"
      # Login em duas etapas: só depois do acesso de produção do SES, senão ninguém entra.
      BOOKLIT_LOGIN_EXIGIR_CODIGO = tostring(var.login_exige_codigo)
    },
    # O SSM não aceita valor vazio: variável vazia simplesmente não vai para o container.
    var.api_dominio == "" ? {} : { API_DOMINIO = var.api_dominio },
    var.front_url == "" ? {} : { FRONT_URL = var.front_url },
    var.cors_origens == "" ? {} : { CORS_ALLOWED_ORIGINS = var.cors_origens },
    var.admin_emails == "" ? {} : { BOOKLIT_ADMIN_EMAILS = var.admin_emails }
  )

  segredos = {
    DATABASE_PASSWORD      = random_password.banco.result
    JWT_SECRET             = base64encode(random_password.jwt.result)
    BOOKLIT_OTP_CHAVE_HMAC = base64encode(random_password.otp_hmac.result)
    GOOGLE_BOOKS_API_KEY   = var.google_books_api_key
  }
}

# 48 caracteres alfanuméricos ~ 285 bits: acima dos 256 que o HMAC-SHA256 pede.
resource "random_password" "jwt" {
  length  = 48
  special = false
}

resource "random_password" "otp_hmac" {
  length  = 48
  special = false
}

resource "aws_ssm_parameter" "configuracao" {
  for_each = local.configuracao

  name  = "${local.prefixo_ssm}/${each.key}"
  type  = "String"
  value = each.value
}

resource "aws_ssm_parameter" "segredo" {
  # Lista literal: as chaves do for_each não podem derivar de um mapa com valores sensíveis.
  for_each = toset(["DATABASE_PASSWORD", "JWT_SECRET", "BOOKLIT_OTP_CHAVE_HMAC", "GOOGLE_BOOKS_API_KEY"])

  name  = "${local.prefixo_ssm}/${each.key}"
  type  = "SecureString"
  value = local.segredos[each.key]
}
