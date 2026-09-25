output "alb_url" {
  description = "Public URL of the application"
  value       = "http://${module.alb.alb_dns_name}"
}