output "secret_arn" {
  value = aws_secretsmanager_secret.mongodb.arn
}

output "secret_name" {
  value = aws_secretsmanager_secret.mongodb.name
}