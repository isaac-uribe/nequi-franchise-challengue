# Franchise Management API

A reactive REST API for managing franchises, branches, and products, built with Spring WebFlux and hexagonal architecture (Clean Architecture), developed as part of Pragma's TalentPool technical challenge for Nequi.

## Overview

The API models a three-level aggregate: a **Franchise** contains multiple **Branches**, and each Branch contains multiple **Products**. It supports creating franchises, adding/renaming branches and products, adjusting stock, removing products, and querying the top-stock product per branch within a franchise.

## Tech Stack

- **Java 21** (LTS)
- **Spring Boot 4.1.1** with **Spring WebFlux** (fully reactive, `RouterFunction`/`Handler` — no `@RestController`)
- **Project Reactor** (`Mono`/`Flux`)
- **MongoDB Atlas** (reactive driver) for persistence
- **Resilience4j** — Circuit Breaker, Timeout, Retry on the persistence adapter
- **MapStruct** — domain ↔ persistence document mapping
- **Lombok**
- **springdoc-openapi** — Swagger UI
- **JUnit 5 + StepVerifier + Mockito** — testing
- **JaCoCo + PITest** — coverage and mutation testing
- **Docker** (multi-stage build)
- **Terraform** — AWS infrastructure as code (ECS Fargate, ALB, VPC, Secrets Manager, IAM)

## Architecture

Generated with the [Bancolombia Clean Architecture Gradle plugin](https://github.com/bancolombia/scaffold-clean-architecture), enforcing a strict dependency rule: `model` has zero dependencies, `usecase` depends only on `model`, and `infrastructure` (entry-points, driven-adapters) can depend on both — never the other way around. Run `./gradlew validateStructure` to verify this rule holds.

```
domain/
├── model/                        → Franchise/Branch/Product aggregate, ports (gateways), domain exceptions
└── usecase/                      → One class per business operation, StepVerifier-tested
infrastructure/
├── entry-points/reactive-web/    → RouterFunction + Handler, DTOs, OpenAPI docs
└── driven-adapters/mongo-repository/ → MongoDB adapter, MapStruct mapper, Resilience4j wrapping
applications/
└── app-service/                  → Spring Boot wiring, the only module with main()
terraform/
├── backend-setup/                → Persistent: S3 state bucket, DynamoDB lock table, ECR repository
├── modules/                      → Reusable: networking, secrets, iam, ecs, alb
└── environments/dev/             → Root module composing all of the above
```

## Prerequisites

- Java 21 ([SDKMAN](https://sdkman.io/) recommended: `sdk install java 21.0.11-amzn`)
- Gradle 9.2.1+ (`sdk install gradle`)
- Docker (or Colima on macOS)
- A MongoDB Atlas cluster (free M0 tier is enough) — see [Local Setup](#local-setup)
- An AWS account, with the AWS CLI configured, if you intend to deploy (see [Deployment](#deployment-to-aws))
- Terraform 1.x (`brew install hashicorp/tap/terraform`), for deployment only

## Local Setup

1. Clone the repository and enter it:
```bash
   git clone https://github.com/isaac-uribe/nequi-franchise-challenge.git
   cd nequi-franchise-challenge
```

2. Create a MongoDB Atlas cluster (or reuse an existing one) and get its connection URI. Make sure your current IP is allowed under **Network Access** in Atlas.

3. Run the application, pointing at your Atlas cluster:
```bash
   export MONGODB_URI="mongodb+srv://<user>:<password>@<cluster-host>/franchiseDb?retryWrites=true&w=majority"
   ./gradlew bootRun --args="--spring.profiles.active=local --spring.mongodb.uri=${MONGODB_URI}"
```

> **Note:** the `local` profile disables the AWS Secrets Manager-based Mongo configuration
> (used in production/AWS deployment) so the app can run without AWS credentials on your
> machine. The connection URI is passed directly as a command-line argument, so no local
> config file is needed — nothing to create before running the command above.

4. Confirm it's up:
```bash
   curl http://localhost:8080/api/health
```

5. Explore the API interactively at:

http://localhost:8080/webjars/swagger-ui/index.html


### Running with Docker

```bash
docker build --platform linux/amd64 -t franchise-api:local .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e SPRING_MONGODB_URI="mongodb+srv://<user>:<password>@<cluster-host>/franchiseDb?retryWrites=true&w=majority" \
  franchise-api:local
```

## Running Tests

```bash
./gradlew clean build
```

Runs unit tests (StepVerifier for reactive flows), JaCoCo coverage, PITest mutation testing, and `validateStructure`.

## Deployment to AWS

Infrastructure is provisioned with Terraform, split into a persistent stack and a disposable one — this lets you tear down the costly parts (NAT Gateway, ALB, ECS) between sessions while keeping Terraform's own state and the container registry intact.

### One-time setup (persistent, low/no cost)

```bash
cd terraform/backend-setup
terraform init
terraform apply
```

Creates the S3 bucket + DynamoDB table used as Terraform's remote state backend, and the ECR repository for the application image.

### Build and push the image

```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker build --platform linux/amd64 -t franchise-api:local .
docker tag franchise-api:local <account-id>.dkr.ecr.us-east-1.amazonaws.com/franchise-api:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/franchise-api:latest
```

### Deploy the application stack

```bash
cd terraform/environments/dev
```

Create `terraform.tfvars` (git-ignored, never commit this):
```hcl
mongodb_uri     = "mongodb+srv://<user>:<password>@<cluster-host>/franchiseDb?retryWrites=true&w=majority"
container_image = "<account-id>.dkr.ecr.us-east-1.amazonaws.com/franchise-api:latest"
```

```bash
terraform init
terraform plan   # review before applying
terraform apply
```

This provisions the VPC, subnets, security groups, Secrets Manager secret, IAM roles, ECS cluster/task/service, ALB, and CPU-based auto scaling (1–3 tasks, target-tracking at 70% CPU).

Get the public URL:
```bash
terraform output alb_url
```

Wait 1–2 minutes for the ECS task to pass its health check, then:
```bash
curl <alb_url>/api/health
```

### Tearing it down

```bash
cd terraform/environments/dev
terraform destroy
```

This removes everything created in this stack (VPC, NAT Gateway, ALB, ECS, secrets) without touching the persistent backend/ECR stack from the one-time setup — safe to run between work sessions to avoid ongoing AWS charges.

## Design Decisions

A few choices worth calling out, since they were deliberate trade-offs rather than defaults:

- **Aggregate root pattern**: `Franchise` owns `Branch` and `Product` with no back-references, matching a single MongoDB document per franchise — avoids circular references in an immutable model and keeps all mutations behind one repository.
- **MapStruct over a generic reflection-based mapper**: explicit, predictable mapping for a two-level nested aggregate.
- **Trunk-based Git workflow**: direct commits to `main`, short-lived branches only for changes that could leave `main` temporarily broken.
- **Single NAT Gateway** (not one per AZ): a deliberate cost/availability trade-off appropriate for this challenge's scope.
- **HTTP-only ALB listener** (no HTTPS): out of scope for this challenge; a 443 listener with an ACM certificate would be the production equivalent.

## Commit Convention

This repository follows [Conventional Commits](https://www.conventionalcommits.org/): `<type>(<scope>): <description>`, English, imperative mood, no ticket references.