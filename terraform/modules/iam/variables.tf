variable "project_name" {
  description = "Name prefix for IAM roles"
  type        = string
  default     = "franchise"
}

variable "secret_arn" {
  description = "ARN of the Secrets Manager secret the app needs to read"
  type        = string
}