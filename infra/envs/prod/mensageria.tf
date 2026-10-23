# Mesmo módulo do Floci: tópico de eventos (outbox -> SNS) e fila do consumidor com DLQ.
module "mensageria" {
  source      = "../../modules/messaging"
  name_prefix = "booklit-prod"
  consumers   = ["notifications"]
}
