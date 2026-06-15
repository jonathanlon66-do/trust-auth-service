variable "aws_region" {
  description = "Región de AWS"
  type        = string
  default     = "us-east-1"
}

variable "aws_account_id" {
  description = "AWS Account ID"
  type        = string
  default     = "188776115228"
}

variable "project" {
  description = "Nombre del proyecto"
  type        = string
  default     = "trust"
}

variable "service" {
  description = "Nombre del servicio"
  type        = string
  default     = "auth-service"
}

variable "environment" {
  description = "Ambiente (qa, prod)"
  type        = string
  default     = "qa"
}

variable "container_port" {
  description = "Puerto del contenedor"
  type        = number
  default     = 8081
}

variable "task_cpu" {
  description = "CPU para la task de ECS (unidades)"
  type        = string
  default     = "256"
}

variable "task_memory" {
  description = "Memoria para la task de ECS (MB)"
  type        = string
  default     = "512"
}

variable "desired_count" {
  description = "Número de instancias del servicio"
  type        = number
  default     = 1
}

locals {
  name_prefix = "${var.project}-${var.service}-${var.environment}"
  tags = {
    Project     = var.project
    Service     = var.service
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}
