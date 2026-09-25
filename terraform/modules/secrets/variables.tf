variable "project_name" {
  description = "Name prefix for the secret"
  type        = string
  default     = "franchise"
}

variable "mongodb_uri" {
  description = "MongoDB Atlas connection URI"
  type        = string
  sensitive   = true
}
