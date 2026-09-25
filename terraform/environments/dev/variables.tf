variable "mongodb_uri" {
  description = "MongoDB Atlas connection URI"
  type        = string
  sensitive   = true
}

variable "container_image" {
  description = "Full ECR image URI with tag"
  type        = string
}