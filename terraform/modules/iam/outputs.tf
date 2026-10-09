output "execution_role_arn" {
  description = "Execution role for the ECS agent (image pull, logs, secret read); passed to the ecs module"
  value       = aws_iam_role.ecs_task_execution.arn
}

output "task_role_arn" {
  description = "Task role assumed by the application, with no policies attached; passed to the ecs module"
  value       = aws_iam_role.ecs_task.arn
}