output "secret_arn" {
  description = "ARN of the secret that stores the MongoDB URI under the JSON key uri; consumed by the iam and ecs modules"
  value       = aws_secretsmanager_secret.mongodb.arn
}

output "secret_name" {
  description = "Secret name (<project_name>/mongodb-uri), for lookup in the console or CLI; no other module consumes it"
  value       = aws_secretsmanager_secret.mongodb.name
}