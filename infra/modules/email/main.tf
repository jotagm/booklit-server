# Identidade de envio do SES v2 usada pelo OTP por e-mail (auth-core).
# No Floci a identidade de e-mail fica verificada na hora; na AWS de verdade o caminho
# certo é uma identidade de domínio (booklit.bio) com DKIM, que entra aqui depois.
resource "aws_sesv2_email_identity" "remetente" {
  email_identity = var.remetente
}
