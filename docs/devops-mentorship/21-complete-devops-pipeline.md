# Section 21: Complete DevOps Pipeline - End-to-End Automation

## Introduction

You've learned Git workflows, CI/CD fundamentals, AWS infrastructure, and Docker containerization. Now it's time to **bring it all together** into a complete, automated DevOps pipeline for Bibby.

**Current State**: Manual, fragmented processes

```
Developer's Machine          CI (GitHub Actions)         Production (AWS)
┌──────────────┐            ┌──────────────┐           ┌──────────────┐
│ git push     │───────────→│ Build        │           │ Manual       │
│              │            │ Test         │           │ Deploy       │
│              │            │ ... stops    │    ???    │ SSH & copy   │
└──────────────┘            └──────────────┘           └──────────────┘
```

**Target State**: Fully automated DevOps pipeline

```
Developer Machine          CI/CD Pipeline                    AWS Production
┌──────────────┐          ┌─────────────────────────────────────────────┐
│ git push     │─────────→│ 1. Checkout code                            │
│              │          │ 2. Run tests (unit, integration)            │
│              │          │ 3. Build & scan Docker image                │
│              │          │ 4. Push to ECR                              │──→ ┌──────────────┐
│              │          │ 5. Update ECS task definition                │   │ ECS Cluster  │
│              │          │ 6. Deploy (blue-green)                      │   │ Running      │
│              │          │ 7. Health checks                            │   │ bibby:0.3.0  │
│              │          │ 8. Rollback on failure                      │   └──────────────┘
└──────────────┘          └─────────────────────────────────────────────┘
     3 min                           10 min                                   Always available
```

**One `git push` triggers everything**: Build → Test → Scan → Deploy → Monitor

This section implements the **complete end-to-end automation** for Bibby.

**What You'll Learn:**

1. **Pipeline architecture** - Full workflow from commit to production
2. **Infrastructure as Code** - Terraform for AWS resources
3. **Complete CI/CD implementation** - GitHub Actions workflows
4. **Database migrations** - Flyway with zero-downtime deployments
5. **Blue-green deployments** - Zero-downtime releases
6. **Rollback strategies** - Automated and manual rollback
7. **Monitoring integration** - CloudWatch, Datadog, PagerDuty
8. **Secrets management** - AWS Secrets Manager, GitHub Secrets
9. **Cost optimization** - Rightsizing, auto-scaling, spot instances
10. **Disaster recovery** - Backup, restore, multi-region

**Prerequisites**: All previous sections (1-20)

---

## 1. Pipeline Architecture Overview

### 1.1 Complete Workflow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           DEVELOPMENT PHASE                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Developer                                                              │
│     │                                                                   │
│     ├──→ Feature branch (feature/add-search)                          │
│     ├──→ Local dev (Docker Compose)                                   │
│     ├──→ Unit tests pass                                              │
│     ├──→ git push origin feature/add-search                           │
│     └──→ Open Pull Request                                            │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                          PR VALIDATION PHASE                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  GitHub Actions: PR Workflow                                           │
│     ├──→ Checkout code                                                 │
│     ├──→ Setup Java 17                                                 │
│     ├──→ Run Maven tests (unit + integration)                         │
│     ├──→ Code coverage check (JaCoCo >80%)                            │
│     ├──→ Build Docker image                                            │
│     ├──→ Scan image (Trivy)                                            │
│     ├──→ Security scan (Snyk, CodeQL)                                  │
│     └──→ ✅ PR checks pass → Enable merge                             │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                         BUILD & DEPLOY PHASE                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Merge to main                                                          │
│     ↓                                                                   │
│  GitHub Actions: Main Workflow                                         │
│     ├──→ 1. Build & tag Docker image                                   │
│     │    └─→ bibby:0.3.1, bibby:0.3.1-a1b2c3d                         │
│     ├──→ 2. Run full test suite                                        │
│     ├──→ 3. Scan for vulnerabilities                                   │
│     ├──→ 4. Push to ECR                                                │
│     ├──→ 5. Run database migrations (Flyway)                          │
│     ├──→ 6. Update ECS task definition                                 │
│     ├──→ 7. Deploy to DEV environment                                  │
│     ├──→ 8. Run smoke tests                                            │
│     └──→ 9. Tag release in Git                                         │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                        STAGING PROMOTION PHASE                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Manual Approval (GitHub Environment Protection)                       │
│     ├──→ Team lead reviews                                             │
│     ├──→ Approves deployment to staging                                │
│     └──→ Trigger staging workflow                                      │
│                                                                         │
│  Deploy to Staging                                                      │
│     ├──→ Update ECS staging cluster                                    │
│     ├──→ Run integration tests                                         │
│     ├──→ Load testing (k6)                                             │
│     ├──→ Security scan (OWASP ZAP)                                     │
│     └──→ QA validation                                                 │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                       PRODUCTION DEPLOYMENT PHASE                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Manual Approval (GitHub Environment Protection)                       │
│     ├──→ Senior engineer reviews                                       │
│     ├──→ Approves production deployment                                │
│     └──→ Trigger production workflow                                   │
│                                                                         │
│  Blue-Green Deployment                                                  │
│     ├──→ 1. Launch new tasks (green) with v0.3.1                      │
│     ├──→ 2. Health checks pass                                         │
│     ├──→ 3. Shift 10% traffic to green                                │
│     ├──→ 4. Monitor metrics (5 min)                                    │
│     ├──→ 5. Shift 50% traffic                                          │
│     ├──→ 6. Monitor metrics (5 min)                                    │
│     ├──→ 7. Shift 100% traffic                                         │
│     ├──→ 8. Drain old tasks (blue)                                     │
│     └──→ 9. Terminate blue deployment                                  │
│                                                                         │
│  Post-Deployment                                                        │
│     ├──→ Synthetic monitoring (canary tests)                          │
│     ├──→ CloudWatch alarms                                             │
│     ├──→ Datadog APM tracking                                          │
│     ├──→ Slack notification                                            │
│     └──→ Create GitHub Release with changelog                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Environments

| Environment | Purpose | Deployment | Infrastructure |
|-------------|---------|------------|----------------|
| **Local** | Developer workstation | Manual (`docker compose up`) | Docker Compose |
| **DEV** | Continuous integration | Automatic (every commit to main) | AWS ECS (1 task) |
| **Staging** | Pre-production testing | Manual approval required | AWS ECS (2 tasks, ALB) |
| **Production** | Live customer traffic | Manual approval + blue-green | AWS ECS (3+ tasks, ALB, Auto Scaling) |

### 1.3 Technology Stack

**Source Control**: GitHub
**CI/CD**: GitHub Actions
**Container Registry**: AWS ECR
**Infrastructure**: AWS (ECS, RDS, ALB, CloudWatch)
**IaC**: Terraform
**Database Migrations**: Flyway
**Monitoring**: CloudWatch + Datadog
**Secrets**: AWS Secrets Manager + GitHub Secrets
**Alerting**: PagerDuty + Slack

---

## 2. Infrastructure as Code (Terraform)

### 2.1 Terraform Project Structure

```
terraform/
├── environments/
│   ├── dev/
│   │   ├── main.tf
│   │   ├── terraform.tfvars
│   │   └── backend.tf
│   ├── staging/
│   │   ├── main.tf
│   │   ├── terraform.tfvars
│   │   └── backend.tf
│   └── production/
│       ├── main.tf
│       ├── terraform.tfvars
│       └── backend.tf
├── modules/
│   ├── networking/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   ├── ecs-cluster/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   ├── rds/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   └── alb/
│       ├── main.tf
│       ├── variables.tf
│       └── outputs.tf
└── scripts/
    ├── plan-all.sh
    ├── apply-all.sh
    └── destroy-all.sh
```

### 2.2 Core Infrastructure Module

**`terraform/modules/networking/main.tf`:**

```hcl
# VPC
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name        = "${var.project_name}-vpc"
    Environment = var.environment
  }
}

# Public Subnets (for ALB)
resource "aws_subnet" "public" {
  count                   = length(var.availability_zones)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone       = element(var.availability_zones, count.index)
  map_public_ip_on_launch = true

  tags = {
    Name        = "${var.project_name}-public-${element(var.availability_zones, count.index)}"
    Environment = var.environment
    Type        = "public"
  }
}

# Private Subnets (for ECS tasks and RDS)
resource "aws_subnet" "private" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 10)
  availability_zone = element(var.availability_zones, count.index)

  tags = {
    Name        = "${var.project_name}-private-${element(var.availability_zones, count.index)}"
    Environment = var.environment
    Type        = "private"
  }
}

# Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name        = "${var.project_name}-igw"
    Environment = var.environment
  }
}

# NAT Gateway (for private subnet internet access)
resource "aws_eip" "nat" {
  count  = var.enable_nat_gateway ? length(var.availability_zones) : 0
  domain = "vpc"

  tags = {
    Name        = "${var.project_name}-nat-eip-${count.index + 1}"
    Environment = var.environment
  }
}

resource "aws_nat_gateway" "main" {
  count         = var.enable_nat_gateway ? length(var.availability_zones) : 0
  allocation_id = element(aws_eip.nat[*].id, count.index)
  subnet_id     = element(aws_subnet.public[*].id, count.index)

  tags = {
    Name        = "${var.project_name}-nat-${count.index + 1}"
    Environment = var.environment
  }
}

# Route Tables
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name        = "${var.project_name}-public-rt"
    Environment = var.environment
  }
}

resource "aws_route_table" "private" {
  count  = var.enable_nat_gateway ? length(var.availability_zones) : 0
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = element(aws_nat_gateway.main[*].id, count.index)
  }

  tags = {
    Name        = "${var.project_name}-private-rt-${count.index + 1}"
    Environment = var.environment
  }
}

# Route Table Associations
resource "aws_route_table_association" "public" {
  count          = length(var.availability_zones)
  subnet_id      = element(aws_subnet.public[*].id, count.index)
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count          = var.enable_nat_gateway ? length(var.availability_zones) : 0
  subnet_id      = element(aws_subnet.private[*].id, count.index)
  route_table_id = element(aws_route_table.private[*].id, count.index)
}
```

### 2.3 ECS Cluster Module

**`terraform/modules/ecs-cluster/main.tf`:**

```hcl
# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name        = "${var.project_name}-${var.environment}"
    Environment = var.environment
  }
}

# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/${var.project_name}-${var.environment}"
  retention_in_days = var.log_retention_days

  tags = {
    Name        = "${var.project_name}-${var.environment}-logs"
    Environment = var.environment
  }
}

# ECS Task Definition
resource "aws_ecs_task_definition" "app" {
  family                   = "${var.project_name}-${var.environment}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = "app"
      image     = "${var.ecr_repository_url}:${var.app_version}"
      essential = true

      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = var.environment
        },
        {
          name  = "DATABASE_URL"
          value = "jdbc:postgresql://${var.db_endpoint}:5432/${var.db_name}"
        }
      ]

      secrets = [
        {
          name      = "DATABASE_PASSWORD"
          valueFrom = "${var.db_password_secret_arn}"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])

  tags = {
    Name        = "${var.project_name}-${var.environment}-task"
    Environment = var.environment
    Version     = var.app_version
  }
}

# ECS Service
resource "aws_ecs_service" "app" {
  name            = "${var.project_name}-${var.environment}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = "app"
    container_port   = 8080
  }

  deployment_configuration {
    maximum_percent         = 200
    minimum_healthy_percent = 100
    deployment_circuit_breaker {
      enable   = true
      rollback = true
    }
  }

  depends_on = [var.alb_listener_arn]

  tags = {
    Name        = "${var.project_name}-${var.environment}-service"
    Environment = var.environment
  }
}

# Auto Scaling
resource "aws_appautoscaling_target" "ecs" {
  max_capacity       = var.max_capacity
  min_capacity       = var.min_capacity
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.app.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "ecs_cpu" {
  name               = "${var.project_name}-${var.environment}-cpu-autoscaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs.service_namespace

  target_tracking_scaling_policy_configuration {
    target_value       = 70.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}

# Security Group for ECS Tasks
resource "aws_security_group" "ecs_tasks" {
  name        = "${var.project_name}-${var.environment}-ecs-tasks"
  description = "Allow inbound traffic from ALB"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [var.alb_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.project_name}-${var.environment}-ecs-tasks-sg"
    Environment = var.environment
  }
}

# IAM Roles
resource "aws_iam_role" "ecs_execution" {
  name = "${var.project_name}-${var.environment}-ecs-execution"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "ecs_execution_secrets" {
  name = "secrets-access"
  role = aws_iam_role.ecs_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = [
          var.db_password_secret_arn
        ]
      }
    ]
  })
}

resource "aws_iam_role" "ecs_task" {
  name = "${var.project_name}-${var.environment}-ecs-task"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}
```

### 2.4 Production Environment Configuration

**`terraform/environments/production/main.tf`:**

```hcl
terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "bibby-terraform-state"
    key            = "production/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "bibby-terraform-locks"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "bibby"
      Environment = "production"
      ManagedBy   = "terraform"
    }
  }
}

# Networking
module "networking" {
  source = "../../modules/networking"

  project_name       = var.project_name
  environment        = "production"
  vpc_cidr           = "10.0.0.0/16"
  availability_zones = ["us-east-1a", "us-east-1b", "us-east-1c"]
  enable_nat_gateway = true
}

# RDS PostgreSQL
module "database" {
  source = "../../modules/rds"

  project_name           = var.project_name
  environment            = "production"
  vpc_id                 = module.networking.vpc_id
  private_subnet_ids     = module.networking.private_subnet_ids
  db_name                = "bibby"
  db_username            = "bibby_admin"
  db_instance_class      = "db.t4g.medium"
  allocated_storage      = 100
  multi_az               = true
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
}

# Application Load Balancer
module "alb" {
  source = "../../modules/alb"

  project_name       = var.project_name
  environment        = "production"
  vpc_id             = module.networking.vpc_id
  public_subnet_ids  = module.networking.public_subnet_ids
  certificate_arn    = var.ssl_certificate_arn
  health_check_path  = "/actuator/health"
}

# ECS Cluster & Service
module "ecs" {
  source = "../../modules/ecs-cluster"

  project_name           = var.project_name
  environment            = "production"
  aws_region             = var.aws_region
  vpc_id                 = module.networking.vpc_id
  private_subnet_ids     = module.networking.private_subnet_ids
  alb_security_group_id  = module.alb.security_group_id
  target_group_arn       = module.alb.target_group_arn
  alb_listener_arn       = module.alb.listener_arn

  ecr_repository_url         = var.ecr_repository_url
  app_version                = var.app_version
  task_cpu                   = "1024"
  task_memory                = "2048"
  desired_count              = 3
  min_capacity               = 2
  max_capacity               = 10

  db_endpoint                = module.database.endpoint
  db_name                    = "bibby"
  db_password_secret_arn     = module.database.password_secret_arn

  log_retention_days         = 30
}

# CloudWatch Alarms
resource "aws_cloudwatch_metric_alarm" "high_cpu" {
  alarm_name          = "${var.project_name}-production-high-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  period              = 300
  statistic           = "Average"
  threshold           = 80

  dimensions = {
    ClusterName = module.ecs.cluster_name
    ServiceName = module.ecs.service_name
  }

  alarm_actions = [var.sns_topic_arn]
}

resource "aws_cloudwatch_metric_alarm" "high_memory" {
  alarm_name          = "${var.project_name}-production-high-memory"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "MemoryUtilization"
  namespace           = "AWS/ECS"
  period              = 300
  statistic           = "Average"
  threshold           = 85

  dimensions = {
    ClusterName = module.ecs.cluster_name
    ServiceName = module.ecs.service_name
  }

  alarm_actions = [var.sns_topic_arn]
}

resource "aws_cloudwatch_metric_alarm" "unhealthy_targets" {
  alarm_name          = "${var.project_name}-production-unhealthy-targets"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "UnHealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = 60
  statistic           = "Average"
  threshold           = 0

  dimensions = {
    TargetGroup  = module.alb.target_group_arn_suffix
    LoadBalancer = module.alb.load_balancer_arn_suffix
  }

  alarm_actions = [var.sns_topic_arn]
}
```

**`terraform/environments/production/terraform.tfvars`:**

```hcl
aws_region          = "us-east-1"
project_name        = "bibby"
ecr_repository_url  = "123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby"
app_version         = "0.3.0"
ssl_certificate_arn = "arn:aws:acm:us-east-1:123456789012:certificate/abc-123"
sns_topic_arn       = "arn:aws:sns:us-east-1:123456789012:bibby-alerts"
```

### 2.5 Deploy Infrastructure

```bash
# Initialize Terraform
cd terraform/environments/production
terraform init

# Plan changes
terraform plan -out=tfplan

# Apply changes
terraform apply tfplan

# Output
# Apply complete! Resources: 45 added, 0 changed, 0 destroyed.
#
# Outputs:
# alb_dns_name = "bibby-prod-alb-1234567890.us-east-1.elb.amazonaws.com"
# ecs_cluster_name = "bibby-production"
# rds_endpoint = "bibby-prod-db.abc123.us-east-1.rds.amazonaws.com"
```

---

## 3. Complete CI/CD Pipeline

### 3.1 GitHub Actions Workflow Structure

```
.github/workflows/
├── pr-validation.yml      # PR checks
├── main-build-deploy.yml  # Main branch → Dev
├── promote-staging.yml    # Dev → Staging
├── promote-production.yml # Staging → Production
└── rollback.yml           # Emergency rollback
```

### 3.2 PR Validation Workflow

**`.github/workflows/pr-validation.yml`:**

```yaml
name: PR Validation

on:
  pull_request:
    branches: [main]

env:
  JAVA_VERSION: '17'
  MAVEN_OPTS: -Xmx1024m

jobs:
  test:
    name: Run Tests
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: bibby_test
          POSTGRES_USER: bibby_test
          POSTGRES_PASSWORD: test_password
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Run unit tests
        run: mvn test -B

      - name: Run integration tests
        run: mvn verify -B -P integration-tests
        env:
          DATABASE_URL: jdbc:postgresql://localhost:5432/bibby_test
          DATABASE_USER: bibby_test
          DATABASE_PASSWORD: test_password

      - name: Generate coverage report
        run: mvn jacoco:report

      - name: Check coverage threshold
        run: |
          COVERAGE=$(mvn jacoco:check | grep -oP 'Total.*?(\d+)%' | grep -oP '\d+')
          echo "Code coverage: ${COVERAGE}%"
          if [ "$COVERAGE" -lt 80 ]; then
            echo "❌ Coverage ${COVERAGE}% is below threshold (80%)"
            exit 1
          fi
          echo "✅ Coverage ${COVERAGE}% meets threshold"

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml
          flags: unittests
          name: codecov-bibby

  security-scan:
    name: Security Scanning
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Run CodeQL analysis
        uses: github/codeql-action/init@v3
        with:
          languages: java

      - uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Build for CodeQL
        run: mvn compile -B -DskipTests

      - name: Perform CodeQL analysis
        uses: github/codeql-action/analyze@v3

      - name: Run Snyk security scan
        uses: snyk/actions/maven@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
        with:
          args: --severity-threshold=high

  docker-build:
    name: Build & Scan Docker Image
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: false
          load: true
          tags: bibby:pr-${{ github.event.pull_request.number }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: bibby:pr-${{ github.event.pull_request.number }}
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'

      - name: Upload Trivy results to GitHub Security
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: 'trivy-results.sarif'

  lint:
    name: Code Quality
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Run Checkstyle
        run: mvn checkstyle:check

      - name: Run SpotBugs
        run: mvn spotbugs:check

      - name: Run PMD
        run: mvn pmd:check
```

### 3.3 Main Build & Deploy Workflow

**`.github/workflows/main-build-deploy.yml`:**

```yaml
name: Build and Deploy to Dev

on:
  push:
    branches: [main]

env:
  AWS_REGION: us-east-1
  ECR_REPOSITORY: bibby
  ECS_CLUSTER: bibby-dev
  ECS_SERVICE: bibby-dev
  JAVA_VERSION: '17'

jobs:
  build-and-deploy:
    name: Build, Test, Push, Deploy
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: write
      security-events: write

    steps:
      # 1. Checkout code
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history for versioning

      # 2. Extract version
      - name: Extract version
        id: version
        run: |
          VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
          GIT_SHA=$(git rev-parse --short HEAD)
          BUILD_DATE=$(date +%Y%m%d-%H%M%S)

          echo "version=$VERSION" >> $GITHUB_OUTPUT
          echo "git_sha=$GIT_SHA" >> $GITHUB_OUTPUT
          echo "build_date=$BUILD_DATE" >> $GITHUB_OUTPUT
          echo "full_version=$VERSION-$GIT_SHA" >> $GITHUB_OUTPUT

          echo "Building version: $VERSION-$GIT_SHA"

      # 3. Set up Java
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      # 4. Run tests
      - name: Run full test suite
        run: mvn verify -B

      # 5. Build JAR
      - name: Build application
        run: mvn package -B -DskipTests

      # 6. Configure AWS credentials
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      # 7. Login to ECR
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      # 8. Build Docker image
      - name: Build Docker image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ steps.version.outputs.full_version }}
        run: |
          docker build \
            --build-arg VERSION=${{ steps.version.outputs.version }} \
            --build-arg GIT_SHA=${{ steps.version.outputs.git_sha }} \
            --build-arg BUILD_DATE=${{ steps.version.outputs.build_date }} \
            -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG \
            -t $ECR_REGISTRY/$ECR_REPOSITORY:latest \
            -t $ECR_REGISTRY/$ECR_REPOSITORY:dev \
            .

      # 9. Scan image
      - name: Scan Docker image
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:${{ steps.version.outputs.full_version }}
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'

      - name: Upload scan results
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: 'trivy-results.sarif'

      # 10. Push to ECR
      - name: Push Docker image to ECR
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ steps.version.outputs.full_version }}
        run: |
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:dev

      # 11. Run database migrations
      - name: Run Flyway migrations
        run: |
          mvn flyway:migrate -B \
            -Dflyway.url=jdbc:postgresql://${{ secrets.DEV_DB_ENDPOINT }}:5432/bibby \
            -Dflyway.user=${{ secrets.DEV_DB_USER }} \
            -Dflyway.password=${{ secrets.DEV_DB_PASSWORD }}

      # 12. Update ECS task definition
      - name: Download task definition
        run: |
          aws ecs describe-task-definition \
            --task-definition ${{ env.ECS_CLUSTER }} \
            --query taskDefinition \
            > task-definition.json

      - name: Fill in new image ID
        id: task-def
        uses: aws-actions/amazon-ecs-render-task-definition@v1
        with:
          task-definition: task-definition.json
          container-name: app
          image: ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:${{ steps.version.outputs.full_version }}

      # 13. Deploy to ECS
      - name: Deploy to Amazon ECS
        uses: aws-actions/amazon-ecs-deploy-task-definition@v1
        with:
          task-definition: ${{ steps.task-def.outputs.task-definition }}
          service: ${{ env.ECS_SERVICE }}
          cluster: ${{ env.ECS_CLUSTER }}
          wait-for-service-stability: true

      # 14. Run smoke tests
      - name: Smoke tests
        run: |
          # Wait for deployment to stabilize
          sleep 30

          # Get ALB DNS
          ALB_DNS=$(aws elbv2 describe-load-balancers \
            --names bibby-dev-alb \
            --query 'LoadBalancers[0].DNSName' \
            --output text)

          # Health check
          curl -f http://$ALB_DNS/actuator/health || exit 1

          # Basic API test
          curl -f http://$ALB_DNS/api/books || exit 1

          echo "✅ Smoke tests passed"

      # 15. Notify Slack
      - name: Notify Slack
        if: always()
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "${{ job.status == 'success' && '✅' || '❌' }} Deployment to DEV",
              "blocks": [
                {
                  "type": "section",
                  "text": {
                    "type": "mrkdwn",
                    "text": "*Bibby Deployment to DEV*\n*Status:* ${{ job.status }}\n*Version:* ${{ steps.version.outputs.full_version }}\n*Commit:* <${{ github.event.head_commit.url }}|${{ steps.version.outputs.git_sha }}>"
                  }
                }
              ]
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}

      # 16. Tag release
      - name: Create Git tag
        if: success()
        run: |
          git config user.name "GitHub Actions"
          git config user.email "actions@github.com"
          git tag -a v${{ steps.version.outputs.version }} -m "Release v${{ steps.version.outputs.version }}"
          git push origin v${{ steps.version.outputs.version }}
```

### 3.4 Production Deployment with Blue-Green

**`.github/workflows/promote-production.yml`:**

```yaml
name: Deploy to Production (Blue-Green)

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version to deploy (e.g., 0.3.0-a1b2c3d)'
        required: true

env:
  AWS_REGION: us-east-1
  ECR_REPOSITORY: bibby
  ECS_CLUSTER: bibby-production
  ECS_SERVICE: bibby-production

jobs:
  deploy:
    name: Blue-Green Deployment
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://bibby.example.com

    permissions:
      id-token: write
      contents: read

    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      # Verify image exists in ECR
      - name: Verify image exists
        run: |
          aws ecr describe-images \
            --repository-name $ECR_REPOSITORY \
            --image-ids imageTag=${{ inputs.version }} \
            || (echo "❌ Image not found in ECR" && exit 1)

          echo "✅ Image verified: ${{ steps.login-ecr.outputs.registry }}/$ECR_REPOSITORY:${{ inputs.version }}"

      # Run database migrations
      - name: Run Flyway migrations
        run: |
          mvn flyway:migrate -B \
            -Dflyway.url=jdbc:postgresql://${{ secrets.PROD_DB_ENDPOINT }}:5432/bibby \
            -Dflyway.user=${{ secrets.PROD_DB_USER }} \
            -Dflyway.password=${{ secrets.PROD_DB_PASSWORD }} \
            -Dflyway.validateOnMigrate=true \
            -Dflyway.outOfOrder=false

      # Get current task definition
      - name: Download task definition
        run: |
          aws ecs describe-task-definition \
            --task-definition $ECS_CLUSTER \
            --query taskDefinition \
            > task-definition.json

      # Create new task definition with new image
      - name: Update task definition
        id: task-def
        uses: aws-actions/amazon-ecs-render-task-definition@v1
        with:
          task-definition: task-definition.json
          container-name: app
          image: ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:${{ inputs.version }}

      # Deploy with CodeDeploy for blue-green
      - name: Deploy to ECS (Blue-Green)
        id: deploy
        uses: aws-actions/amazon-ecs-deploy-task-definition@v1
        with:
          task-definition: ${{ steps.task-def.outputs.task-definition }}
          service: ${{ env.ECS_SERVICE }}
          cluster: ${{ env.ECS_CLUSTER }}
          wait-for-service-stability: true
          codedeploy-appspec: appspec.yaml
          codedeploy-application: bibby
          codedeploy-deployment-group: bibby-production

      # Monitor deployment
      - name: Monitor deployment
        run: |
          DEPLOYMENT_ID=${{ steps.deploy.outputs.codedeploy-deployment-id }}

          echo "Monitoring deployment: $DEPLOYMENT_ID"

          while true; do
            STATUS=$(aws deploy get-deployment \
              --deployment-id $DEPLOYMENT_ID \
              --query 'deploymentInfo.status' \
              --output text)

            echo "Deployment status: $STATUS"

            case $STATUS in
              Succeeded)
                echo "✅ Deployment successful!"
                break
                ;;
              Failed)
                echo "❌ Deployment failed!"
                exit 1
                ;;
              Stopped)
                echo "❌ Deployment stopped!"
                exit 1
                ;;
              *)
                echo "Waiting... (status: $STATUS)"
                sleep 30
                ;;
            esac
          done

      # Run synthetic tests
      - name: Run synthetic monitoring
        run: |
          # Wait for traffic shift to complete
          sleep 60

          # Health check
          for i in {1..10}; do
            curl -f https://bibby.example.com/actuator/health || exit 1
            sleep 5
          done

          # API tests
          curl -f https://bibby.example.com/api/books || exit 1

          echo "✅ Synthetic tests passed"

      # Notify team
      - name: Notify Slack
        if: always()
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "${{ job.status == 'success' && '✅ PRODUCTION DEPLOYMENT SUCCESSFUL' || '❌ PRODUCTION DEPLOYMENT FAILED' }}",
              "blocks": [
                {
                  "type": "section",
                  "text": {
                    "type": "mrkdwn",
                    "text": "*Bibby Production Deployment*\n*Status:* ${{ job.status }}\n*Version:* ${{ inputs.version }}\n*Deployed by:* ${{ github.actor }}"
                  }
                },
                {
                  "type": "actions",
                  "elements": [
                    {
                      "type": "button",
                      "text": {
                        "type": "plain_text",
                        "text": "View Logs"
                      },
                      "url": "https://console.aws.amazon.com/ecs/home?region=us-east-1#/clusters/${{ env.ECS_CLUSTER }}/services/${{ env.ECS_SERVICE }}"
                    },
                    {
                      "type": "button",
                      "text": {
                        "type": "plain_text",
                        "text": "View Application"
                      },
                      "url": "https://bibby.example.com"
                    }
                  ]
                }
              ]
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}

      # Create GitHub Release
      - name: Create GitHub Release
        if: success()
        uses: softprops/action-gh-release@v1
        with:
          tag_name: v${{ inputs.version }}
          name: Release ${{ inputs.version }}
          body: |
            ## Production Deployment

            **Version:** ${{ inputs.version }}
            **Deployed:** ${{ github.event.head_commit.timestamp }}
            **Deployed by:** ${{ github.actor }}

            ## Docker Image
            ```
            ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:${{ inputs.version }}
            ```

            ## Changes
            See commit history for details.
          draft: false
          prerelease: false
```

---

**Due to length constraints, I'll create a summary section now and can continue with remaining topics (database migrations, rollback strategies, monitoring integration, etc.) in the next response if you'd like to continue.**

## Summary (Part 1 of Section 21)

**What You've Learned So Far:**

1. **Complete Pipeline Architecture**
   - End-to-end workflow from `git push` to production
   - 4 environments: Local, Dev, Staging, Production
   - Automated gates at each stage

2. **Infrastructure as Code (Terraform)**
   - Modular structure: networking, ECS, RDS, ALB
   - Complete production infrastructure (~800 lines)
   - State management with S3 + DynamoDB locking
   - Multi-environment configuration (dev/staging/prod)

3. **CI/CD Pipelines (GitHub Actions)**
   - **PR Validation**: Tests, security scans, Docker build
   - **Main Deploy**: Full pipeline to dev environment
   - **Production Deploy**: Blue-green with manual approval
   - Integrated: CodeQL, Snyk, Trivy, Codecov

4. **Key Features Implemented**
   - Automated testing (unit, integration, coverage >80%)
   - Security scanning (code + container vulnerabilities)
   - Database migrations with Flyway
   - ECS auto-scaling (CPU-based)
   - CloudWatch alarms
   - Slack notifications

**Progress**: Section 21 Part 1 complete (infrastructure + CI/CD pipelines)

**Remaining in Section 21**:
- Database migration strategies
- Rollback procedures
- Monitoring & observability integration
- Secrets management
- Cost optimization
- Disaster recovery

Would you like me to continue with the rest of Section 21?
