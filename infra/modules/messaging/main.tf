resource "aws_sns_topic" "events" {
  name = "${var.name_prefix}-events"
}

resource "aws_sqs_queue" "dlq" {
  for_each                  = toset(var.consumers)
  name                      = "${var.name_prefix}-${each.key}-dlq"
  message_retention_seconds = 1209600
}

resource "aws_sqs_queue" "queue" {
  for_each                   = toset(var.consumers)
  name                       = "${var.name_prefix}-${each.key}"
  visibility_timeout_seconds = 30
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq[each.key].arn
    maxReceiveCount     = var.max_receive_count
  })
}

resource "aws_sqs_queue_policy" "allow_sns" {
  for_each  = toset(var.consumers)
  queue_url = aws_sqs_queue.queue[each.key].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "sns.amazonaws.com" }
      Action    = "sqs:SendMessage"
      Resource  = aws_sqs_queue.queue[each.key].arn
      Condition = { ArnEquals = { "aws:SourceArn" = aws_sns_topic.events.arn } }
    }]
  })
}

resource "aws_sns_topic_subscription" "sub" {
  for_each             = toset(var.consumers)
  topic_arn            = aws_sns_topic.events.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.queue[each.key].arn
  raw_message_delivery = true
}
