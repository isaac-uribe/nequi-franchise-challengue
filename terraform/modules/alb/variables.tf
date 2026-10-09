variable "project_name" {
  description = "Name prefix for the ALB and its target group (AWS limits both names to 32 characters)"
  type        = string
  default     = "franchise"
}

variable "vpc_id" {
  description = "VPC the target group is created in; must be the VPC that contains the public subnets"
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnets the internet-facing ALB is placed in; at least two, in different availability zones"
  type        = list(string)
}

variable "alb_security_group_id" {
  description = "Security group attached to the ALB; it allows inbound HTTP on port 80 from the internet"
  type        = string
}