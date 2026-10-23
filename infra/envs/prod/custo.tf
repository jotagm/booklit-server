# Alerta de gasto. Os créditos do plano Free zeram a fatura, então o orçamento olha o custo
# bruto (include_credit = false): é ele que diz se os créditos vão durar os 6 meses.
resource "aws_budgets_budget" "mensal" {
  count = var.email_alertas == "" ? 0 : 1

  name         = "booklit-prod-mensal"
  budget_type  = "COST"
  limit_amount = tostring(var.orcamento_mensal_usd)
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  cost_types {
    include_credit = false
    include_refund = false
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.email_alertas]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.email_alertas]
  }
}
