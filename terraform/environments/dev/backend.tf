terraform {
  backend "s3" {
    bucket         = "nequi-franchise-tfstate-isaac1"
    key            = "environments/dev/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "franchise-api-terraform-locks"
    encrypt        = true
  }
}