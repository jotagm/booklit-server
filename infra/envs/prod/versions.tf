terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    random = {
      source  = "hashicorp/random"
      version = ">= 3.6"
    }
  }

  # Estado local por enquanto (fica fora do git pelo .gitignore). Ele guarda as senhas
  # geradas: faça backup do terraform.tfstate. Quando quiser, migre para um backend S3.
}

provider "aws" {
  region = var.regiao

  default_tags {
    tags = {
      Projeto       = "booklit"
      Ambiente      = "prod"
      GerenciadoPor = "terraform"
    }
  }
}
