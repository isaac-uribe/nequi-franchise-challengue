output "vpc_id" {
  description = "VPC that hosts every resource of the stack; consumed by the alb module for its target group"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "Public subnets, one per AZ, for the ALB (the NAT Gateway sits in the first); consumed by the alb module"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Private subnets, one per AZ, routed through the NAT Gateway; consumed by the ecs module for the tasks"
  value       = aws_subnet.private[*].id
}

output "alb_security_group_id" {
  description = "Security group that allows HTTP on port 80 from the internet; consumed by the alb module"
  value       = aws_security_group.alb.id
}

output "ecs_security_group_id" {
  description = "Security group that allows port 8080 only from the ALB security group; consumed by the ecs module"
  value       = aws_security_group.ecs_tasks.id
}