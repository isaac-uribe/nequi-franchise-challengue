output "state_bucket_name" {
  description = "S3 bucket that stores remote Terraform state; referenced by the backend block in environments/dev/backend.tf"
  value       = aws_s3_bucket.terraform_state.id
}

output "lock_table_name" {
  description = "DynamoDB table used for Terraform state locking; referenced by environments/dev/backend.tf"
  value       = aws_dynamodb_table.terraform_locks.name
}

output "ecr_repository_url" {
  description = "ECR repository to tag and push the application image to; the pushed image URI feeds container_image in dev"
  value       = aws_ecr_repository.franchise_api.repository_url
}