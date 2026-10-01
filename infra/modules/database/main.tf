resource "aws_db_instance" "main" {
  identifier          = "${var.name_prefix}-db"
  engine              = "postgres"
  engine_version      = "16"
  instance_class      = "db.t4g.micro"
  allocated_storage   = 20
  db_name             = var.db_name
  username            = var.username
  password            = var.password
  skip_final_snapshot = true
}
