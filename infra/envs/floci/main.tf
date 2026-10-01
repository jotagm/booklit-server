module "messaging" {
  source      = "../../modules/messaging"
  name_prefix = "booklit"
  consumers   = ["notifications"]
}

output "topic_arn" {
  value = module.messaging.topic_arn
}

output "queue_urls" {
  value = module.messaging.queue_urls
}

variable "db_password" {
  type      = string
  sensitive = true
}

module "database" {
  source      = "../../modules/database"
  name_prefix = "booklit"
  db_name     = "clube_livro"
  username    = "booklit"
  password    = var.db_password
}

output "db_endpoint" {
  value = module.database.endpoint
}

output "db_port" {
  value = module.database.port
}
