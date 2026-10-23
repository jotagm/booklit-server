variable "regiao" {
  description = "Região da AWS"
  type        = string
  default     = "us-east-1"
}

variable "tipo_instancia" {
  description = "Tipo da EC2 da API. t4g = ARM (Graviton); o pipeline builda a imagem na arquitetura certa"
  type        = string
  default     = "t4g.micro"
}

variable "classe_banco" {
  description = "Classe da instância RDS"
  type        = string
  default     = "db.t4g.micro"
}

variable "github_repo" {
  description = "Repositório autorizado a fazer deploy (dono/nome)"
  type        = string
  default     = "jotagm/booklit-server"
}

variable "criar_oidc_github" {
  description = "Cria o provedor OIDC do GitHub na conta. Use false se a conta já tiver um (só pode existir um por URL)"
  type        = bool
  default     = true
}

variable "api_dominio" {
  description = "Domínio público da API (ex.: api.booklit.bio), com registro A apontando para o IP elástico. Vazio = só HTTP na porta 80, pelo IP. Mudou? rode o apply e um novo deploy"
  type        = string
  default     = ""
}

variable "front_url" {
  description = "Endereço público do front (base dos links de convite)"
  type        = string
  default     = "https://booklit.bio"
}

variable "cors_origens" {
  description = "Origens do front autorizadas a chamar a API, separadas por vírgula"
  type        = string
  default     = "https://booklit.bio"
}

variable "admin_emails" {
  description = "E-mails de administradores do Booklit, separados por vírgula"
  type        = string
  default     = ""
}

variable "google_books_api_key" {
  description = "Chave da Google Books API"
  type        = string
  sensitive   = true
}

variable "email_alertas" {
  description = "E-mail que recebe os alertas de orçamento. Vazio = sem alerta"
  type        = string
  default     = ""
}

variable "orcamento_mensal_usd" {
  description = "Gasto bruto mensal (sem descontar créditos) que dispara o alerta"
  type        = number
  default     = 30
}

variable "retencao_backup_dias" {
  description = "Dias de backup automático do RDS. O plano Free da AWS só aceita 1; no plano pago, use 7"
  type        = number
  default     = 1
}
