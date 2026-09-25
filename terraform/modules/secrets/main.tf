resource "aws_secretsmanager_secret" "mongodb" {
  name                    = "${var.project_name}/mongodb-uri"
  description             = "MongoDB Atlas connection URI for the Franchise API"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "mongodb" {
  secret_id     = aws_secretsmanager_secret.mongodb.id
  secret_string = jsonencode({
    uri = var.mongodb_uri
  })
}