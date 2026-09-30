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
