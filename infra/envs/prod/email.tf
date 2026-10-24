# E-mail transacional (códigos de login, convites) pelo SES v2.
#
# A identidade é o DOMÍNIO (não só no-reply@): assim qualquer endereço @booklit.bio pode
# enviar, e o DKIM assina tudo com a chave do domínio. Os 3 registros CNAME da saída
# "dns_dkim" vão na Name.com; sem eles o domínio fica "Pending" e nada é enviado.
#
# Conta nova começa no sandbox: só envia para endereços verificados. Para enviar para
# qualquer pessoa, peça acesso de produção no console (SES > Account dashboard >
# Request production access). Isso não tem recurso no Terraform.

resource "aws_sesv2_email_identity" "dominio" {
  email_identity = var.dominio_email
}

# Endereços que podem RECEBER enquanto a conta está no sandbox (a AWS manda um link de
# confirmação para cada um). Útil para testar o login com código antes do acesso de produção.
resource "aws_sesv2_email_identity" "teste_sandbox" {
  for_each       = toset(var.emails_teste_sandbox)
  email_identity = each.value
}

locals {
  dkim_tokens = aws_sesv2_email_identity.dominio.dkim_signing_attributes[0].tokens
}
