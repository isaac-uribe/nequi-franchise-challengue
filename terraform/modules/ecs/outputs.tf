output "cluster_name" {
  description = "ECS cluster name, for aws ecs CLI commands and the console; no other module consumes it"
  value       = aws_ecs_cluster.main.name
}

output "service_name" {
  description = "ECS service name, for aws ecs CLI commands such as forcing a new deployment; no other module consumes it"
  value       = aws_ecs_service.app.name
}