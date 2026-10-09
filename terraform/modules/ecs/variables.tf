variable "project_name" {
  description = "Name prefix for the ECS cluster, service, task family, container, log group and scaling policy"
  type        = string
  default     = "franchise"
}

variable "container_image" {
  description = "Full ECR image URI, including tag"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnets the Fargate tasks run in, without public IPs; outbound traffic goes through the NAT Gateway"
  type        = list(string)
}

variable "ecs_security_group_id" {
  description = "Security group attached to the tasks; it only allows port 8080 from the ALB security group"
  type        = string
}

variable "execution_role_arn" {
  description = "Role used by the ECS agent to pull the image, write logs and inject the MongoDB secret"
  type        = string
}

variable "task_role_arn" {
  description = "Role assumed by the application code; it has no permissions because the app calls no AWS APIs"
  type        = string
}

variable "secret_arn" {
  description = "ARN of the Secrets Manager secret whose uri key is injected into the container as SPRING_MONGODB_URI"
  type        = string
}

variable "target_group_arn" {
  description = "ALB target group ARN this service registers with"
  type        = string
}

variable "task_cpu" {
  description = "CPU units reserved for the Fargate task (512 = 0.5 vCPU); must be a valid Fargate CPU/memory combination"
  type        = string
  default     = "512"
}

variable "task_memory" {
  description = "Memory in MiB reserved for the Fargate task (1024 = 1 GiB)"
  type        = string
  default     = "1024"
}

variable "desired_count" {
  description = "Initial number of tasks; Auto Scaling manages it afterwards and later changes to it are ignored"
  type        = number
  default     = 1
}

variable "min_capacity" {
  description = "Lower bound on the number of tasks for CPU-based Auto Scaling"
  type        = number
  default     = 1
}

variable "max_capacity" {
  description = "Upper bound on the number of tasks for CPU-based Auto Scaling"
  type        = number
  default     = 3
}