output "alb_dns_name" {
  description = "Public DNS name of the ALB (HTTP only); environments/dev exposes it as the alb_url output"
  value       = aws_lb.main.dns_name
}

output "target_group_arn" {
  description = "Target group (HTTP 8080, health check /api/health) that the ECS service registers its tasks with"
  value       = aws_lb_target_group.app.arn
}