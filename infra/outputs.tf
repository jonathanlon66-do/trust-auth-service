output "alb_dns" {
  description = "DNS del ALB — úsalo para probar el servicio"
  value       = aws_lb.main.dns_name
}

output "ecr_repository_url" {
  description = "URL del repositorio ECR"
  value       = aws_ecr_repository.auth_service.repository_url
}

output "ecs_cluster_name" {
  description = "Nombre del cluster ECS"
  value       = aws_ecs_cluster.main.name
}
