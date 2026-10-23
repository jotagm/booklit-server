resource "random_password" "banco" {
  length  = 32
  special = false
}

resource "aws_db_subnet_group" "banco" {
  name       = "booklit-prod"
  subnet_ids = data.aws_subnets.padrao.ids
}

resource "aws_db_instance" "banco" {
  identifier     = "booklit-prod"
  engine         = "postgres"
  engine_version = "16"
  instance_class = var.classe_banco

  allocated_storage     = 20
  max_allocated_storage = 50
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = "booklit"
  username = "booklit"
  password = random_password.banco.result

  db_subnet_group_name   = aws_db_subnet_group.banco.name
  vpc_security_group_ids = [aws_security_group.banco.id]
  publicly_accessible    = false
  multi_az               = false
  # Mesma AZ da EC2: sem tráfego entre AZs (que é cobrado) e menor latência.
  availability_zone = data.aws_subnet.api.availability_zone

  # Horários em UTC: 03h-04h e domingo 04h30-05h30 em Brasília.
  # O plano Free da AWS limita a retenção; no plano pago, suba para 7 (variável).
  backup_retention_period    = var.retencao_backup_dias
  backup_window              = "06:00-07:00"
  maintenance_window         = "sun:07:30-sun:08:30"
  auto_minor_version_upgrade = true
  copy_tags_to_snapshot      = true

  # Produção: apagar exige desligar a proteção antes, e sempre deixa um snapshot final.
  deletion_protection       = true
  skip_final_snapshot       = false
  final_snapshot_identifier = "booklit-prod-final"

  performance_insights_enabled = false
}
