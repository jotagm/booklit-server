# Fotos de perfil: um bucket S3 privado. A API nunca recebe a imagem: ela entrega ao navegador
# (ou ao app) uma URL assinada de curta duração, o cliente envia direto ao S3 e depois
# confirma. Leitura também por URL assinada, então o bucket não tem nada público.

locals {
  # Origens que podem fazer PUT direto no bucket (o navegador exige CORS no S3 também).
  origens_fotos = [for o in split(",", var.cors_origens != "" ? var.cors_origens : var.front_url) : trimspace(o)]
}

resource "aws_s3_bucket" "fotos" {
  # Nome de bucket é global na AWS: o id da conta evita colisão.
  bucket = "booklit-prod-fotos-${data.aws_caller_identity.atual.account_id}"
}

resource "aws_s3_bucket_public_access_block" "fotos" {
  bucket                  = aws_s3_bucket.fotos.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "fotos" {
  bucket = aws_s3_bucket.fotos.id
  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "fotos" {
  bucket = aws_s3_bucket.fotos.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_cors_configuration" "fotos" {
  bucket = aws_s3_bucket.fotos.id
  cors_rule {
    allowed_methods = ["PUT"]
    allowed_origins = local.origens_fotos
    allowed_headers = ["*"]
    max_age_seconds = 3600
  }
}

# Upload em partes que ficou pela metade ocupa espaço e não aparece em lugar nenhum.
resource "aws_s3_bucket_lifecycle_configuration" "fotos" {
  bucket = aws_s3_bucket.fotos.id
  rule {
    id     = "limpar-uploads-incompletos"
    status = "Enabled"
    filter {}
    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }
}
