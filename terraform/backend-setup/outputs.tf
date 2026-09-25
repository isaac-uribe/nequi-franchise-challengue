output "state_bucket_name" {
  value = aws_s3_bucket.terraform_state.id
}

output "lock_table_name" {
  value = aws_dynamodb_table.terraform_locks.name
}

output "ecr_repository_url" {
  value = aws_ecr_repository.franchise_api.repository_url
}