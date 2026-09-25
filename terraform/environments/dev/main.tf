provider "aws" {
  region = "us-east-1"
}

module "networking" {
  source = "../../modules/networking"
}

module "secrets" {
  source      = "../../modules/secrets"
  mongodb_uri = var.mongodb_uri
}

module "iam" {
  source     = "../../modules/iam"
  secret_arn = module.secrets.secret_arn
}

module "alb" {
  source                 = "../../modules/alb"
  vpc_id                 = module.networking.vpc_id
  public_subnet_ids      = module.networking.public_subnet_ids
  alb_security_group_id  = module.networking.alb_security_group_id
}

module "ecs" {
  source                 = "../../modules/ecs"
  container_image        = var.container_image
  private_subnet_ids     = module.networking.private_subnet_ids
  ecs_security_group_id  = module.networking.ecs_security_group_id
  execution_role_arn     = module.iam.execution_role_arn
  task_role_arn          = module.iam.task_role_arn
  secret_arn             = module.secrets.secret_arn
  target_group_arn       = module.alb.target_group_arn
}