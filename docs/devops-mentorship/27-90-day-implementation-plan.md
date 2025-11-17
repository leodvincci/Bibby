# Section 27: 90-Day Implementation Plan - Your Complete Roadmap

## Introduction

You've read 26 sections covering everything from SDLC fundamentals to continuous learning.

**Now it's time to execute.**

Most people read guides like this and never implement them. They have information but not action.

This section is your **week-by-week, day-by-day implementation plan** to go from zero to production-grade DevOps engineer in 90 days.

**What You'll Get:**

1. **12-week detailed plan** - What to do every week
2. **Daily task breakdown** - Specific tasks (not vague goals)
3. **Time estimates** - Realistic hours per task
4. **Success criteria** - How to know you're on track
5. **Flexibility points** - Where you can adjust based on your pace
6. **Checkpoints** - Weekly reviews to stay on track

**Prerequisites**: All previous sections (read first, then implement)

**Time Commitment**: 15-20 hours per week (2-3 hours weekdays, 5-7 hours weekends)

---

## Overview: The 90-Day Journey

### Phase 1: Foundation (Weeks 1-4)
**Goal**: Build and containerize Bibby locally

```
Week 1: Project setup and Java application
Week 2: Local development environment
Week 3: Docker fundamentals
Week 4: Docker optimization
```

**Deliverable**: Bibby running in optimized Docker containers locally

---

### Phase 2: Cloud Deployment (Weeks 5-8)
**Goal**: Deploy Bibby to AWS with infrastructure as code

```
Week 5: AWS fundamentals and account setup
Week 6: Terraform and networking (VPC)
Week 7: ECS and database deployment
Week 8: Load balancing and DNS
```

**Deliverable**: Bibby running in AWS ECS with RDS

---

### Phase 3: Automation (Weeks 9-10)
**Goal**: Build complete CI/CD pipeline

```
Week 9: CI pipeline (build, test, scan)
Week 10: CD pipeline (deploy, rollback)
```

**Deliverable**: Automated deployments from Git push to production

---

### Phase 4: Excellence (Weeks 11-12)
**Goal**: Monitoring, optimization, and documentation

```
Week 11: Monitoring, alerting, DORA metrics
Week 12: Documentation, portfolio, and job applications
```

**Deliverable**: Complete portfolio and job applications sent

---

## Phase 1: Foundation (Weeks 1-4)

### Week 1: Project Setup and Java Application

**Goals:**
- Set up development environment
- Build basic Bibby CLI application
- Implement core features (add, list, search books)

**Monday (2 hours): Environment Setup**
- [ ] Install Java 17 JDK
- [ ] Install Maven 3.9+
- [ ] Install Git
- [ ] Set up IDE (IntelliJ IDEA Community or VS Code)
- [ ] Create GitHub repository: `Bibby`

**Tuesday (2 hours): Maven Project Structure**
- [ ] Initialize Maven project: `mvn archetype:generate`
- [ ] Configure `pom.xml`:
  - Spring Boot 3.1+
  - PostgreSQL driver
  - JUnit 5 for testing
- [ ] Create package structure:
  ```
  com.yourname.bibby/
  ├── cli/
  ├── model/
  ├── repository/
  └── service/
  ```
- [ ] Run: `mvn clean compile` (verify setup)

**Wednesday (3 hours): Core Domain Models**
- [ ] Create `Book` entity:
  - Fields: id, title, author, isbn, publicationYear
  - JPA annotations (@Entity, @Id, @GeneratedValue)
- [ ] Create `BookRepository` interface (JPA)
- [ ] Write unit tests for `Book` model
- [ ] Run: `mvn test` (all tests pass)

**Thursday (3 hours): CLI Commands - Part 1**
- [ ] Implement `add` command:
  - Parse user input (picocli)
  - Save book to database
  - Handle validation errors
- [ ] Implement `list` command:
  - Query all books
  - Format output (table format)
- [ ] Manual testing: Add 3 books, list them

**Friday (2 hours): CLI Commands - Part 2**
- [ ] Implement `search` command:
  - Search by title or author
  - Partial matching (LIKE query)
- [ ] Implement `delete` command (by ID or ISBN)
- [ ] Manual testing: Full CRUD workflow

**Weekend (5 hours): Database Integration & Testing**
- [ ] Set up local PostgreSQL (Docker is fine for now)
- [ ] Configure `application.properties` for database connection
- [ ] Write integration tests:
  - Test database saves
  - Test searches
  - Test edge cases (duplicate ISBN, null values)
- [ ] Run: `mvn verify` (all tests pass)
- [ ] Create initial README.md with usage instructions

**Success Criteria:**
- ✅ Bibby CLI works locally
- ✅ Can add, list, search, delete books
- ✅ All tests pass (`mvn verify`)
- ✅ Code committed to Git with clear commit messages

**Resources:**
- Section 1-2: SDLC and Agile practices
- Section 3: Build lifecycle fundamentals
- Spring Boot docs: https://spring.io/guides

---

### Week 2: Local Development Environment

**Goals:**
- Set up local development with Docker Compose
- Improve developer experience
- Implement database migrations

**Monday (2 hours): Docker Compose Setup**
- [ ] Create `docker-compose.yml`:
  ```yaml
  services:
    postgres:
      image: postgres:15-alpine
      environment:
        POSTGRES_USER: bibby
        POSTGRES_PASSWORD: dev_password
        POSTGRES_DB: bibby_dev
      ports:
        - "5432:5432"
  ```
- [ ] Create `.env` file for environment variables (add to `.gitignore`!)
- [ ] Test: `docker-compose up -d` and connect to database

**Tuesday (2 hours): Database Migrations**
- [ ] Add Flyway dependency to `pom.xml`
- [ ] Create migration: `V1__create_books_table.sql`
- [ ] Create migration: `V2__add_indexes.sql` (index on ISBN)
- [ ] Run migrations: `mvn flyway:migrate`
- [ ] Verify schema in PostgreSQL

**Wednesday (3 hours): Application Profiles**
- [ ] Create `application-dev.properties` (local development)
- [ ] Create `application-prod.properties` (production placeholder)
- [ ] Update Maven to use profiles:
  - `dev` profile: H2 in-memory (fast tests)
  - `prod` profile: PostgreSQL
- [ ] Test profile switching: `mvn spring-boot:run -Dspring.profiles.active=dev`

**Thursday (2 hours): Developer Experience Improvements**
- [ ] Add Spring Boot DevTools (auto-reload)
- [ ] Add actuator endpoints (`/actuator/health`, `/actuator/info`)
- [ ] Create `Makefile` for common commands:
  ```makefile
  run:
    mvn spring-boot:run

  test:
    mvn test

  db-up:
    docker-compose up -d
  ```
- [ ] Update README with Makefile usage

**Friday (3 hours): Testing Improvements**
- [ ] Add Testcontainers dependency
- [ ] Refactor integration tests to use Testcontainers (real PostgreSQL in tests)
- [ ] Achieve 80%+ code coverage
- [ ] Run: `mvn verify` (all tests pass)

**Weekend (5 hours): Code Quality & Documentation**
- [ ] Add checkstyle/spotless for code formatting
- [ ] Add SonarLint (or SonarQube) for code analysis
- [ ] Fix all code quality issues
- [ ] Write CONTRIBUTING.md (how to set up locally)
- [ ] Write ARCHITECTURE.md (high-level design)
- [ ] Tag release: `v0.1.0` (local development complete)

**Success Criteria:**
- ✅ Local development with Docker Compose
- ✅ Database migrations with Flyway
- ✅ 80%+ test coverage
- ✅ Clear documentation (README, CONTRIBUTING, ARCHITECTURE)

**Resources:**
- Section 3: Build lifecycle
- Section 8: Git mental model
- Flyway docs: https://flywaydb.org

---

### Week 3: Docker Fundamentals

**Goals:**
- Containerize Bibby application
- Understand Docker layers and caching
- Create multi-stage Dockerfile

**Monday (2 hours): Basic Dockerfile**
- [ ] Create `Dockerfile` (single-stage, not optimized yet):
  ```dockerfile
  FROM maven:3.9-openjdk-17
  WORKDIR /app
  COPY . .
  RUN mvn clean package -DskipTests
  CMD ["java", "-jar", "target/Bibby-0.1.0.jar"]
  ```
- [ ] Build: `docker build -t bibby:v0.1.0 .`
- [ ] Run: `docker run bibby:v0.1.0`
- [ ] Document build time and image size (likely 680MB+)

**Tuesday (3 hours): Multi-Stage Build**
- [ ] Refactor Dockerfile to multi-stage:
  - **Stage 1 (builder)**: Maven build
  - **Stage 2 (runtime)**: JRE only, copy JAR
- [ ] Use Alpine base image for runtime (`eclipse-temurin:17-jre-alpine`)
- [ ] Rebuild and measure:
  - Image size (should be ~200MB)
  - Build time
- [ ] Document improvements

**Wednesday (2 hours): Layer Caching Optimization**
- [ ] Optimize Dockerfile for layer caching:
  - Copy `pom.xml` first
  - Run `mvn dependency:go-offline`
  - Then copy source code
- [ ] Test caching:
  - Build once (slow)
  - Change source code only
  - Build again (should be fast, ~10-15s)
- [ ] Document build time improvement

**Thursday (3 hours): Docker Compose for Full Stack**
- [ ] Update `docker-compose.yml` to include app:
  ```yaml
  services:
    app:
      build: .
      ports:
        - "8080:8080"
      depends_on:
        - postgres
      environment:
        DATABASE_URL: jdbc:postgresql://postgres:5432/bibby_dev

    postgres:
      # ... existing config
  ```
- [ ] Test: `docker-compose up --build`
- [ ] Verify: App connects to database, CLI works

**Friday (2 hours): .dockerignore and Security**
- [ ] Create `.dockerignore`:
  ```
  target/
  .git/
  .idea/
  *.md
  .env
  ```
- [ ] Add non-root user to Dockerfile:
  ```dockerfile
  RUN addgroup -S bibby && adduser -S bibby -G bibby
  USER bibby
  ```
- [ ] Rebuild and test: `docker-compose up --build`

**Weekend (5 hours): Docker Deep Dive**
- [ ] Inspect image layers: `docker history bibby:v0.1.0`
- [ ] Experiment with BuildKit cache mounts:
  ```dockerfile
  RUN --mount=type=cache,target=/root/.m2 mvn package
  ```
- [ ] Measure final optimizations:
  - Image size: Target <200MB
  - Build time (cached): Target <15s
  - Build time (clean): Target <90s
- [ ] Write blog post draft: "How I Optimized My Docker Image" (publish later)
- [ ] Tag release: `v0.2.0` (Docker optimization complete)

**Success Criteria:**
- ✅ Multi-stage Dockerfile
- ✅ Image size <200MB (from 680MB+)
- ✅ Layer caching works (fast rebuilds)
- ✅ Non-root user for security
- ✅ Full stack runs with Docker Compose

**Resources:**
- Section 16-17: Docker fundamentals and layers
- Docker docs: https://docs.docker.com

---

### Week 4: Docker Optimization & Checkpoint

**Goals:**
- Advanced Docker optimizations
- Security scanning
- Phase 1 checkpoint and retrospective

**Monday (2 hours): Health Checks**
- [ ] Add health check to `Dockerfile`:
  ```dockerfile
  HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
  ```
- [ ] Add health check to `docker-compose.yml`
- [ ] Test: `docker ps` shows "healthy" status

**Tuesday (2 hours): Container Security Scanning**
- [ ] Install Trivy: `brew install trivy` (or equivalent)
- [ ] Scan image: `trivy image bibby:v0.2.0`
- [ ] Fix any CRITICAL or HIGH vulnerabilities:
  - Update base images
  - Update dependencies in `pom.xml`
- [ ] Re-scan: Zero CRITICAL/HIGH vulnerabilities

**Wednesday (3 hours): Docker Best Practices Review**
- [ ] Review Dockerfile against best practices:
  - [ ] Multi-stage build ✅
  - [ ] Layer caching optimized ✅
  - [ ] Non-root user ✅
  - [ ] Health check ✅
  - [ ] .dockerignore ✅
  - [ ] Alpine base image ✅
- [ ] Run hadolint (Dockerfile linter): `hadolint Dockerfile`
- [ ] Fix any issues

**Thursday (2 hours): Documentation Update**
- [ ] Update README with Docker instructions
- [ ] Add troubleshooting section (common errors)
- [ ] Create DOCKER.md with:
  - Architecture diagram
  - Build process
  - Optimization techniques used
  - Before/after metrics

**Friday (3 hours): Phase 1 Checkpoint**
- [ ] Review all Week 1-4 success criteria
- [ ] Run full test suite: `mvn verify`
- [ ] Run Docker Compose: `docker-compose up`
- [ ] Test full workflow (add, list, search, delete books)
- [ ] Check code quality (no SonarLint issues)
- [ ] Tag release: `v1.0.0` (Phase 1 complete)

**Weekend (5 hours): Retrospective & Planning**
- [ ] **Retrospective**: Answer these questions:
  - What went well?
  - What was harder than expected?
  - What would I do differently?
  - Am I on track (time-wise)?
- [ ] **Review progress**:
  - Java CLI application ✅
  - Docker optimization ✅
  - Local development environment ✅
  - Documentation ✅
- [ ] **Adjust plan if needed**:
  - If behind: Identify what to cut (polish later)
  - If ahead: Consider adding features
- [ ] **Prepare for Phase 2**:
  - Read Section 13-15 (AWS fundamentals)
  - Sign up for AWS Free Tier
  - Mentally prepare for cloud deployment

**Success Criteria:**
- ✅ All Phase 1 goals met
- ✅ Docker image optimized (<200MB, <15s cached builds)
- ✅ Zero CRITICAL/HIGH vulnerabilities
- ✅ Comprehensive documentation
- ✅ v1.0.0 tagged and pushed to GitHub

**Resources:**
- Section 18: Container lifecycle and operations
- Section 19: Docker registries

---

## Phase 2: Cloud Deployment (Weeks 5-8)

### Week 5: AWS Fundamentals and Account Setup

**Goals:**
- Set up AWS account securely
- Understand AWS core services
- Plan infrastructure architecture

**Monday (2 hours): AWS Account Setup**
- [ ] Sign up for AWS Free Tier
- [ ] Set up MFA on root account
- [ ] Create IAM admin user (don't use root!)
- [ ] Install AWS CLI: `aws --version`
- [ ] Configure AWS CLI: `aws configure`
- [ ] Test: `aws sts get-caller-identity`

**Tuesday (3 hours): AWS Cost Management**
- [ ] Set up billing alerts:
  - Alert at $10, $25, $50
- [ ] Enable Cost Explorer
- [ ] Review Free Tier limits:
  - 750 hours EC2 t2.micro
  - 750 hours RDS db.t2.micro
  - 20GB RDS storage
- [ ] Create budget: $60/month target
- [ ] Read Section 15: AWS Security & Cost Management

**Wednesday (2 hours): AWS Core Services Overview**
- [ ] Read AWS documentation:
  - IAM (users, roles, policies)
  - VPC (subnets, route tables, internet gateway)
  - EC2 (instances, security groups)
  - ECS (tasks, services, clusters)
  - RDS (databases, backups)
  - ECR (Docker registry)
- [ ] Draw architecture diagram (hand-drawn is fine):
  - VPC with public/private subnets
  - ALB in public subnets
  - ECS tasks in private subnets
  - RDS in private subnets

**Thursday (3 hours): Terraform Setup**
- [ ] Install Terraform: `terraform --version`
- [ ] Create `terraform/` directory:
  ```
  terraform/
  ├── main.tf
  ├── variables.tf
  ├── outputs.tf
  └── terraform.tfvars.example
  ```
- [ ] Configure AWS provider:
  ```hcl
  terraform {
    required_providers {
      aws = {
        source  = "hashicorp/aws"
        version = "~> 5.0"
      }
    }
  }

  provider "aws" {
    region = var.aws_region
  }
  ```
- [ ] Run: `terraform init`

**Friday (2 hours): Terraform State Backend**
- [ ] Create S3 bucket for Terraform state (via AWS Console for now):
  - Name: `bibby-terraform-state-<your-name>`
  - Enable versioning
  - Enable encryption
- [ ] Create DynamoDB table for state locking:
  - Name: `bibby-terraform-locks`
  - Partition key: `LockID` (String)
- [ ] Configure backend in `main.tf`:
  ```hcl
  terraform {
    backend "s3" {
      bucket         = "bibby-terraform-state-<your-name>"
      key            = "production/terraform.tfstate"
      region         = "us-east-1"
      encrypt        = true
      dynamodb_table = "bibby-terraform-locks"
    }
  }
  ```
- [ ] Run: `terraform init` (migrate state to S3)

**Weekend (5 hours): Infrastructure Planning**
- [ ] Refine architecture diagram:
  - VPC: 10.0.0.0/16
  - Public subnets: 10.0.1.0/24, 10.0.2.0/24 (2 AZs)
  - Private subnets: 10.0.10.0/24, 10.0.11.0/24 (2 AZs)
  - Internet Gateway for public subnets
  - NAT Gateway for private subnets (egress only)
  - ALB in public subnets
  - ECS tasks in private subnets
  - RDS in private subnets
- [ ] Create ADR (Architecture Decision Record):
  - ADR-001: Why AWS (vs GCP/Azure)
  - ADR-002: Why ECS Fargate (vs EKS, vs EC2)
  - ADR-003: Why RDS PostgreSQL (vs Aurora, vs self-managed)
- [ ] Document in `docs/architecture/`
- [ ] Read Section 13-14: AWS core concepts and deployment strategies

**Success Criteria:**
- ✅ AWS account set up with MFA and billing alerts
- ✅ Terraform configured with S3 backend
- ✅ Architecture diagram drawn
- ✅ ADRs documented

**Resources:**
- Section 13: AWS core concepts
- Section 15: AWS security and cost management
- AWS Free Tier: https://aws.amazon.com/free/

---

### Week 6: Terraform and Networking (VPC)

**Goals:**
- Create VPC with Terraform
- Understand networking (subnets, routing, gateways)
- Set up security groups

**Monday (3 hours): VPC and Subnets**
- [ ] Create `terraform/vpc.tf`:
  - VPC (10.0.0.0/16)
  - Public subnets (2 AZs)
  - Private subnets (2 AZs)
  - Internet Gateway
- [ ] Run: `terraform plan`
- [ ] Run: `terraform apply`
- [ ] Verify in AWS Console: VPC created

**Tuesday (3 hours): NAT Gateway and Routing**
- [ ] Add to `vpc.tf`:
  - NAT Gateway (in public subnet)
  - Elastic IP for NAT Gateway
  - Public route table (0.0.0.0/0 → Internet Gateway)
  - Private route table (0.0.0.0/0 → NAT Gateway)
  - Associate subnets with route tables
- [ ] Run: `terraform apply`
- [ ] Verify routing tables in AWS Console

**Wednesday (2 hours): Security Groups**
- [ ] Create `terraform/security_groups.tf`:
  - ALB security group (allow 80, 443 from internet)
  - ECS security group (allow 8080 from ALB only)
  - RDS security group (allow 5432 from ECS only)
- [ ] Run: `terraform apply`
- [ ] Review principle of least privilege

**Thursday (3 hours): Terraform Modules**
- [ ] Refactor into modules:
  ```
  terraform/
  ├── modules/
  │   ├── vpc/
  │   │   ├── main.tf
  │   │   ├── variables.tf
  │   │   └── outputs.tf
  │   └── security_groups/
  │       ├── main.tf
  │       ├── variables.tf
  │       └── outputs.tf
  └── main.tf (calls modules)
  ```
- [ ] Test: `terraform plan` (no changes)
- [ ] Commit modular code

**Friday (2 hours): Terraform Best Practices**
- [ ] Add `.gitignore`:
  ```
  .terraform/
  *.tfstate
  *.tfstate.backup
  .terraform.lock.hcl
  terraform.tfvars
  ```
- [ ] Create `terraform.tfvars.example` (no secrets!)
- [ ] Add validation to variables:
  ```hcl
  variable "aws_region" {
    type    = string
    default = "us-east-1"
    validation {
      condition     = contains(["us-east-1", "us-west-2"], var.aws_region)
      error_message = "Region must be us-east-1 or us-west-2"
    }
  }
  ```
- [ ] Run: `terraform fmt` (format code)
- [ ] Run: `terraform validate`

**Weekend (5 hours): Documentation and Testing**
- [ ] Test VPC setup:
  - Create test EC2 instance in public subnet
  - Verify internet access (curl google.com)
  - Create test EC2 instance in private subnet
  - Verify NAT Gateway works (curl google.com via NAT)
  - Terminate test instances
- [ ] Document Terraform usage:
  - README in `terraform/` directory
  - How to apply changes
  - How to destroy infrastructure
- [ ] Run: `terraform plan -out=tfplan` (save plan)
- [ ] Review costs: `infracost breakdown --path .` (optional tool)

**Success Criteria:**
- ✅ VPC with public/private subnets in 2 AZs
- ✅ NAT Gateway for private subnets
- ✅ Security groups following least privilege
- ✅ Terraform code modular and documented
- ✅ No secrets in Git

**Resources:**
- Section 13: AWS core concepts (VPC, subnets)
- Terraform docs: https://registry.terraform.io/providers/hashicorp/aws/latest/docs

---

### Week 7: ECS and Database Deployment

**Goals:**
- Deploy Bibby to AWS ECS Fargate
- Create RDS PostgreSQL database
- Connect application to database

**Monday (3 hours): ECR (Elastic Container Registry)**
- [ ] Create `terraform/ecr.tf`:
  - ECR repository: `bibby`
  - Image scanning enabled
  - Lifecycle policy (keep last 10 images)
- [ ] Run: `terraform apply`
- [ ] Push Docker image to ECR:
  ```bash
  aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
  docker tag bibby:v1.0.0 <account-id>.dkr.ecr.us-east-1.amazonaws.com/bibby:v1.0.0
  docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/bibby:v1.0.0
  ```
- [ ] Verify image in AWS Console

**Tuesday (3 hours): RDS PostgreSQL**
- [ ] Create `terraform/rds.tf`:
  - RDS PostgreSQL (db.t3.micro for Free Tier)
  - Allocated storage: 20GB
  - Multi-AZ: false (save cost)
  - In private subnets
  - Security group (allow 5432 from ECS)
  - Automated backups enabled
- [ ] Create `terraform/secrets.tf`:
  - AWS Secrets Manager for database password
- [ ] Run: `terraform apply` (takes 10-15 min)
- [ ] Note RDS endpoint (output from Terraform)

**Wednesday (3 hours): ECS Cluster and Task Definition**
- [ ] Create `terraform/ecs.tf`:
  - ECS Cluster (Fargate)
  - Task definition:
    - Image: ECR image
    - CPU: 512 (.5 vCPU)
    - Memory: 1024 MB
    - Environment variables (RDS endpoint)
    - Secrets (database password from Secrets Manager)
  - Task execution role (IAM)
  - Task role (IAM, for accessing AWS services)
- [ ] Run: `terraform apply`
- [ ] Manually test task: Run task via AWS Console, check logs

**Thursday (3 hours): ECS Service**
- [ ] Add to `ecs.tf`:
  - ECS Service:
    - Desired count: 2
    - Launch type: Fargate
    - Network: Private subnets
    - Security group: ECS security group
- [ ] Run: `terraform apply`
- [ ] Verify: 2 tasks running in ECS Console
- [ ] Check CloudWatch Logs: Look for application logs

**Friday (2 hours): Debugging and Troubleshooting**
- [ ] If tasks fail to start:
  - Check CloudWatch Logs for errors
  - Verify IAM permissions
  - Verify RDS security group allows ECS
  - Verify secrets are accessible
- [ ] Use ECS Exec (if needed):
  ```bash
  aws ecs execute-command --cluster bibby-production \
    --task <task-id> --interactive --command "/bin/sh"
  ```
- [ ] Document troubleshooting steps in runbook

**Weekend (5 hours): Database Migrations**
- [ ] Run Flyway migrations against RDS:
  - Option 1: From local machine (temporary security group rule)
  - Option 2: From ECS task (exec into task, run migrations)
- [ ] Verify schema in RDS
- [ ] Test application:
  - Add a book via ECS task
  - Verify in database
- [ ] Remove temporary security group rules (if used)
- [ ] Tag release: `v1.1.0` (deployed to ECS!)

**Success Criteria:**
- ✅ ECR repository created, image pushed
- ✅ RDS PostgreSQL running in private subnets
- ✅ ECS tasks running and connecting to RDS
- ✅ CloudWatch Logs showing application logs
- ✅ Database schema migrated

**Resources:**
- Section 14: AWS deployment strategies
- Section 18: Container lifecycle
- AWS ECS docs: https://docs.aws.amazon.com/ecs/

---

### Week 8: Load Balancing, DNS, and Phase 2 Checkpoint

**Goals:**
- Add Application Load Balancer (ALB)
- Configure custom domain (optional)
- Phase 2 checkpoint

**Monday (3 hours): Application Load Balancer**
- [ ] Create `terraform/alb.tf`:
  - ALB in public subnets
  - Target group (port 8080, health check `/actuator/health`)
  - Listener (HTTP port 80)
  - Security group (allow 80 from internet)
- [ ] Update ECS service to register with target group
- [ ] Run: `terraform apply`
- [ ] Test: `curl http://<alb-dns-name>` (should see Bibby response)

**Tuesday (2 hours): HTTPS Setup (Optional)**
- [ ] If you have a domain:
  - Create ACM certificate (AWS Certificate Manager)
  - Validate domain (DNS or email)
  - Add HTTPS listener to ALB (port 443)
  - Redirect HTTP → HTTPS
- [ ] If no domain:
  - Skip HTTPS for now (add later)
- [ ] Update security group to allow 443

**Wednesday (2 hours): Route 53 DNS (Optional)**
- [ ] If you have a domain:
  - Create Route 53 hosted zone
  - Create A record: `bibby.yourdomain.com` → ALB
  - Test: `curl https://bibby.yourdomain.com`
- [ ] If no domain:
  - Use ALB DNS name (functional but not pretty)

**Thursday (3 hours): Health Checks and Graceful Shutdown**
- [ ] Configure ALB health checks:
  - Path: `/actuator/health`
  - Interval: 30s
  - Healthy threshold: 2
  - Unhealthy threshold: 3
  - Timeout: 5s
- [ ] Update Spring Boot for graceful shutdown:
  ```properties
  server.shutdown=graceful
  spring.lifecycle.timeout-per-shutdown-phase=20s
  ```
- [ ] Deploy new version, verify zero downtime

**Friday (3 hours): Auto-Scaling**
- [ ] Create `terraform/autoscaling.tf`:
  - Auto-scaling target (min: 2, max: 10)
  - Auto-scaling policy (target CPU: 70%)
  - CloudWatch alarms (CPU >80%)
- [ ] Run: `terraform apply`
- [ ] Test scaling:
  - Use Apache Bench to load test: `ab -n 10000 -c 100 http://<alb-dns>/`
  - Watch ECS scale up in Console

**Weekend (5 hours): Phase 2 Checkpoint**
- [ ] Review all Week 5-8 success criteria
- [ ] Full infrastructure test:
  - Visit ALB URL (or custom domain)
  - Add books via API
  - Verify in database
  - Check CloudWatch Logs
  - Verify auto-scaling works
  - Check billing (should be <$30 so far)
- [ ] **Retrospective**:
  - What was harder than expected? (AWS? Terraform?)
  - What surprised you?
  - How's the budget?
- [ ] **Documentation update**:
  - Update README with deployed URL
  - Document infrastructure architecture
  - Add runbook for common operations
- [ ] Tag release: `v2.0.0` (Phase 2 complete!)

**Success Criteria:**
- ✅ ALB routing traffic to ECS tasks
- ✅ Health checks passing
- ✅ Auto-scaling configured and tested
- ✅ Optional: HTTPS and custom domain
- ✅ Comprehensive Terraform code (VPC, ECS, RDS, ALB)
- ✅ Infrastructure cost <$60/month

**Resources:**
- Section 14: AWS deployment strategies (blue-green, rolling)
- Section 15: AWS cost management

---

## Phase 3: Automation (Weeks 9-10)

### Week 9: CI Pipeline (Build, Test, Scan)

**Goals:**
- Build GitHub Actions CI pipeline
- Automate testing, security scanning
- Integrate with pull requests

**Monday (3 hours): GitHub Actions Basics**
- [ ] Create `.github/workflows/ci.yml`:
  ```yaml
  name: CI Pipeline

  on:
    push:
      branches: [main]
    pull_request:
      branches: [main]

  jobs:
    build:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v3
        - name: Set up JDK 17
          uses: actions/setup-java@v3
          with:
            java-version: '17'
        - name: Build with Maven
          run: mvn clean verify
  ```
- [ ] Push to GitHub, verify workflow runs
- [ ] Check: Green checkmark on commit

**Tuesday (3 hours): Testing and Code Coverage**
- [ ] Add test job to CI:
  - Run: `mvn test`
  - Generate coverage report (JaCoCo)
  - Upload coverage to Codecov (optional)
- [ ] Add coverage badge to README:
  ```markdown
  ![Coverage](https://codecov.io/gh/yourname/bibby/branch/main/graph/badge.svg)
  ```
- [ ] Set coverage threshold: 80% minimum

**Wednesday (3 hours): Security Scanning**
- [ ] Add Trivy scan to CI:
  ```yaml
  - name: Run Trivy vulnerability scanner
    uses: aquasecurity/trivy-action@master
    with:
      image-ref: 'bibby:${{ github.sha }}'
      format: 'sarif'
      output: 'trivy-results.sarif'

  - name: Upload Trivy results to GitHub Security
    uses: github/codeql-action/upload-sarif@v2
    with:
      sarif_file: 'trivy-results.sarif'
  ```
- [ ] Add Snyk for dependency scanning:
  - Sign up at snyk.io
  - Add Snyk GitHub Action
  - Add `SNYK_TOKEN` to GitHub Secrets
- [ ] Verify: Security tab shows vulnerabilities (if any)

**Thursday (3 hours): Docker Build in CI**
- [ ] Add Docker build step:
  ```yaml
  - name: Build Docker image
    run: docker build -t bibby:${{ github.sha }} .

  - name: Scan Docker image
    run: trivy image bibby:${{ github.sha }}
  ```
- [ ] Use GitHub Actions cache for Docker layers:
  ```yaml
  - name: Set up Docker Buildx
    uses: docker/setup-buildx-action@v2

  - name: Build with cache
    uses: docker/build-push-action@v4
    with:
      context: .
      push: false
      tags: bibby:${{ github.sha }}
      cache-from: type=gha
      cache-to: type=gha,mode=max
  ```
- [ ] Measure build time (should be <2 min with cache)

**Friday (2 hours): Branch Protection Rules**
- [ ] Enable branch protection on `main`:
  - Require pull request reviews (1 reviewer)
  - Require status checks to pass (CI must pass)
  - Require branches to be up to date
  - No direct pushes to main
- [ ] Create test PR to verify checks
- [ ] Document workflow in CONTRIBUTING.md

**Weekend (5 hours): CI Optimization**
- [ ] Optimize CI speed:
  - Cache Maven dependencies:
    ```yaml
    - name: Cache Maven packages
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    ```
  - Run jobs in parallel (build, test, scan)
- [ ] Add CI badges to README:
  ```markdown
  ![CI](https://github.com/yourname/bibby/actions/workflows/ci.yml/badge.svg)
  ```
- [ ] Measure total CI time: Target <5 min
- [ ] Write blog post draft: "How I built a complete CI pipeline"

**Success Criteria:**
- ✅ GitHub Actions CI runs on every PR and push
- ✅ Automated tests with 80%+ coverage
- ✅ Security scanning (Trivy, Snyk)
- ✅ Docker build in CI (cached, <2 min)
- ✅ Branch protection enforces CI checks
- ✅ Total CI time <5 minutes

**Resources:**
- Section 5: CI fundamentals
- Section 7: CI/CD best practices and security
- GitHub Actions docs: https://docs.github.com/en/actions

---

### Week 10: CD Pipeline (Deploy, Rollback)

**Goals:**
- Automate deployment to AWS ECS
- Implement blue-green deployment
- Add rollback capability

**Monday (3 hours): CD Pipeline Setup**
- [ ] Create `.github/workflows/cd.yml`:
  ```yaml
  name: CD Pipeline

  on:
    push:
      branches: [main]

  jobs:
    deploy:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v3

        - name: Configure AWS credentials
          uses: aws-actions/configure-aws-credentials@v2
          with:
            aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
            aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
            aws-region: us-east-1

        - name: Login to Amazon ECR
          id: login-ecr
          uses: aws-actions/amazon-ecr-login@v1
  ```
- [ ] Create IAM user for GitHub Actions (least privilege)
- [ ] Add AWS credentials to GitHub Secrets

**Tuesday (3 hours): Build and Push to ECR**
- [ ] Add to CD pipeline:
  ```yaml
  - name: Build, tag, and push image to Amazon ECR
    env:
      ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
      ECR_REPOSITORY: bibby
      IMAGE_TAG: ${{ github.sha }}
    run: |
      docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
      docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
      docker tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPOSITORY:latest
      docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest
  ```
- [ ] Push to main, verify image in ECR

**Wednesday (3 hours): Deploy to ECS**
- [ ] Add to CD pipeline:
  ```yaml
  - name: Update ECS task definition
    id: task-def
    uses: aws-actions/amazon-ecs-render-task-definition@v1
    with:
      task-definition: task-definition.json
      container-name: bibby
      image: ${{ env.ECR_REGISTRY }}/${{ env.ECR_REPOSITORY }}:${{ github.sha }}

  - name: Deploy to Amazon ECS
    uses: aws-actions/amazon-ecs-deploy-task-definition@v1
    with:
      task-definition: ${{ steps.task-def.outputs.task-definition }}
      service: bibby-production
      cluster: bibby-production
      wait-for-service-stability: true
  ```
- [ ] Export task definition from AWS: `aws ecs describe-task-definition --task-definition bibby > task-definition.json`
- [ ] Commit `task-definition.json` to repo
- [ ] Test: Push to main, watch deployment in ECS

**Thursday (3 hours): Blue-Green Deployment**
- [ ] Update CD pipeline to use CodeDeploy for blue-green:
  ```yaml
  - name: Deploy with CodeDeploy (Blue-Green)
    run: |
      aws deploy create-deployment \
        --application-name bibby-app \
        --deployment-group-name bibby-deployment-group \
        --revision revisionType=S3,s3Location={bucket=my-bucket,key=appspec.yml}
  ```
- [ ] Create `appspec.yml`:
  ```yaml
  version: 0.0
  Resources:
    - TargetService:
        Type: AWS::ECS::Service
        Properties:
          TaskDefinition: <TASK_DEFINITION>
          LoadBalancerInfo:
            ContainerName: "bibby"
            ContainerPort: 8080
  Hooks:
    - BeforeInstall: "LambdaFunctionToValidateBeforeInstall"
    - AfterInstall: "LambdaFunctionToValidateAfterInstall"
    - AfterAllowTestTraffic: "LambdaFunctionToValidateAfterTestTraffic"
  ```
- [ ] Test blue-green deployment

**Friday (2 hours): Rollback Mechanism**
- [ ] Create manual rollback workflow `.github/workflows/rollback.yml`:
  ```yaml
  name: Rollback

  on:
    workflow_dispatch:
      inputs:
        version:
          description: 'Version to rollback to (Git SHA or tag)'
          required: true

  jobs:
    rollback:
      runs-on: ubuntu-latest
      steps:
        # ... (similar to deploy, but use ${{ github.event.inputs.version }})
  ```
- [ ] Test rollback:
  - Deploy a "broken" version (intentionally fail health check)
  - Trigger rollback via GitHub Actions UI
  - Verify previous version is restored

**Weekend (5 hours): CD Polish and Documentation**
- [ ] Add deployment notifications:
  - Slack notification on successful deployment
  - Email notification on failed deployment
- [ ] Add deployment metrics:
  - Track deployment frequency (custom CloudWatch metric)
  - Track lead time (commit timestamp → deployment timestamp)
- [ ] Document deployment process:
  - README: "How Deployments Work"
  - Runbook: "How to Rollback"
  - Postmortem template
- [ ] Tag release: `v2.1.0` (CD complete!)

**Success Criteria:**
- ✅ Automated deployment on merge to main
- ✅ Blue-green deployment (zero downtime)
- ✅ Rollback mechanism (one-click)
- ✅ Deployment notifications (Slack/email)
- ✅ End-to-end: Git push → production in <30 minutes

**Resources:**
- Section 6: Continuous deployment and delivery
- Section 7: CI/CD best practices
- AWS CodeDeploy docs: https://docs.aws.amazon.com/codedeploy/

---

## Phase 4: Excellence (Weeks 11-12)

### Week 11: Monitoring, Alerting, DORA Metrics

**Goals:**
- Set up comprehensive monitoring
- Configure alerting
- Track DORA metrics

**Monday (3 hours): CloudWatch Dashboards**
- [ ] Create CloudWatch dashboard:
  - ECS CPU utilization (average, P95)
  - ECS memory utilization
  - ALB request count
  - ALB target response time (P50, P95, P99)
  - ALB HTTP 4xx/5xx errors
  - RDS CPU, connections, disk I/O
- [ ] Test: Generate load, watch metrics

**Tuesday (3 hours): Application Metrics (Prometheus)**
- [ ] Add Micrometer Prometheus dependency to `pom.xml`
- [ ] Expose `/actuator/prometheus` endpoint
- [ ] Set up Prometheus (Docker Compose locally first):
  ```yaml
  prometheus:
    image: prom/prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"
  ```
- [ ] Configure scraping:
  ```yaml
  scrape_configs:
    - job_name: 'bibby'
      static_configs:
        - targets: ['app:8080']
  ```
- [ ] Verify: Prometheus UI shows Bibby metrics

**Wednesday (3 hours): Grafana Dashboards**
- [ ] Add Grafana to Docker Compose:
  ```yaml
  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
  ```
- [ ] Create dashboard in Grafana:
  - HTTP request rate (by endpoint, status code)
  - Response time percentiles (P50, P95, P99)
  - JVM metrics (heap usage, GC pauses)
  - Custom business metrics (books created, searches)
- [ ] Save dashboard JSON to repo
- [ ] Screenshot dashboard for portfolio

**Thursday (2 hours): CloudWatch Alarms**
- [ ] Create `terraform/alarms.tf`:
  - ECS CPU >80% for 5 minutes → Warning
  - ALB 5xx errors >1% for 2 minutes → Critical
  - RDS CPU >80% for 10 minutes → Warning
  - ALB unhealthy targets >0 for 5 minutes → Critical
- [ ] Create SNS topic for alerts
- [ ] Subscribe to SNS (email)
- [ ] Run: `terraform apply`
- [ ] Test: Trigger an alarm (e.g., stop ECS tasks)

**Friday (3 hours): DORA Metrics Tracking**
- [ ] Implement DORA metrics:
  - **Deployment Frequency**: Count deployments (CloudWatch custom metric)
    ```bash
    aws cloudwatch put-metric-data --namespace Bibby --metric-name Deployments --value 1
    ```
  - **Lead Time**: Calculate (commit timestamp → deploy timestamp)
    ```yaml
    # In CD pipeline
    - name: Record lead time
      run: |
        COMMIT_TIME=$(git show -s --format=%ct $GITHUB_SHA)
        DEPLOY_TIME=$(date +%s)
        LEAD_TIME=$((DEPLOY_TIME - COMMIT_TIME))
        aws cloudwatch put-metric-data --namespace Bibby --metric-name LeadTime --value $LEAD_TIME
    ```
  - **Change Failure Rate**: Failed deploys / total deploys
  - **MTTR**: Time between alert and resolution (track in postmortems)
- [ ] Create DORA metrics dashboard in CloudWatch
- [ ] Document current DORA metrics

**Weekend (5 hours): Observability Improvements**
- [ ] Add structured logging:
  - Use Logback with JSON format
  - Include trace ID, user ID, endpoint in logs
- [ ] Set up CloudWatch Logs Insights:
  - Query examples for common issues
  - Save queries for quick debugging
- [ ] Create runbook:
  - "High CPU Alert" → Steps to investigate
  - "High 5xx Errors" → Steps to investigate
  - "Database Connection Issues" → Steps to investigate
- [ ] Write blog post draft: "How I Monitor My Production System"

**Success Criteria:**
- ✅ CloudWatch dashboards for infrastructure
- ✅ Grafana dashboards for application metrics
- ✅ CloudWatch alarms for critical issues
- ✅ DORA metrics tracked and visible
- ✅ Runbooks for common incidents

**Resources:**
- Section 22: DevOps metrics and KPIs
- Section 23: Documentation and runbooks
- Prometheus docs: https://prometheus.io/docs/

---

### Week 12: Documentation, Portfolio, and Job Applications

**Goals:**
- Polish all documentation
- Create portfolio showcase
- Apply to jobs

**Monday (3 hours): Documentation Audit**
- [ ] Review and update all documentation:
  - README (clear, compelling, with screenshots)
  - ARCHITECTURE.md (diagrams, decisions)
  - CONTRIBUTING.md (how to contribute)
  - DOCKER.md (Docker optimizations)
  - API.md (if you have a REST API)
- [ ] Add badges to README:
  - CI status
  - Code coverage
  - License
  - Last commit
- [ ] Spell check all documentation

**Tuesday (3 hours): Architecture Diagrams**
- [ ] Create professional diagrams:
  - System architecture (VPC, ALB, ECS, RDS)
  - CI/CD pipeline (GitHub → ECR → ECS)
  - Deployment flow (blue-green)
- [ ] Use tools:
  - Lucidchart (free tier)
  - Excalidraw (free, open source)
  - AWS Architecture Icons
- [ ] Add diagrams to ARCHITECTURE.md

**Wednesday (3 hours): DORA Metrics Report**
- [ ] Calculate final DORA metrics:
  - **Deployment Frequency**: Count deployments over last 2 weeks
  - **Lead Time**: Average time (commit → deploy)
  - **Change Failure Rate**: Failed deploys / total
  - **MTTR**: Average time to recover from incidents
- [ ] Create metrics report in README:
  ```markdown
  ## DORA Metrics (Elite Performance)

  - **Deployment Frequency**: 15 deploys/week
  - **Lead Time**: 3 hours
  - **Change Failure Rate**: 5%
  - **MTTR**: 12 minutes
  ```
- [ ] Compare to industry benchmarks (Section 22)

**Thursday (3 hours): Portfolio Website**
- [ ] Create portfolio page (or update existing):
  - Project: Bibby
  - Description: "Production-grade library management system achieving Elite DORA metrics"
  - Tech stack: Java, Spring Boot, Docker, AWS ECS, Terraform, GitHub Actions
  - Key achievements:
    - 72% Docker image size reduction
    - 99.97% uptime
    - $60/month infrastructure cost
    - Elite DORA metrics
  - Screenshots: Grafana dashboard, architecture diagram
  - Links: GitHub, live demo (if public)
- [ ] Use simple tools:
  - GitHub Pages + Jekyll
  - Or: dev.to profile
  - Or: Personal website (HTML/CSS)

**Friday (3 hours): Resume Update**
- [ ] Add Bibby to resume:
  ```
  Personal Projects

  Bibby - Cloud-Native Library Management System (GitHub.com/yourname/bibby)
  - Built production-grade CLI application achieving Elite DORA metrics (15 deploys/week, 3-hour lead time)
  - Reduced Docker image size by 72% (680MB → 192MB) through multi-stage builds and Alpine base images
  - Architected AWS infrastructure (VPC, ECS Fargate, RDS) using Terraform with 99.97% uptime
  - Implemented complete CI/CD pipeline with GitHub Actions (automated testing, security scanning, blue-green deployments)
  - Technologies: Java 17, Spring Boot, Docker, AWS (ECS, RDS, ECR, ALB), Terraform, GitHub Actions, PostgreSQL
  ```
- [ ] Update LinkedIn:
  - Add Bibby to projects
  - Update skills (add Docker, Terraform, AWS, etc.)
  - Update headline: "Software Engineer | DevOps | AWS | Docker | Terraform"

**Weekend (7 hours): Job Applications**
- [ ] Identify target companies:
  - Use LinkedIn, Indeed, AngelList
  - Filter: DevOps Engineer, Cloud Engineer, SRE (0-2 years exp)
  - Target: 10-20 companies
- [ ] Customize resume for each:
  - Match keywords from job posting
  - Highlight relevant Bibby features
- [ ] Prepare cover letter template:
  - Brief intro
  - "I built Bibby, a production-grade system achieving Elite DORA metrics..."
  - Link to GitHub and portfolio
  - Why this company
- [ ] Apply to 5-10 jobs
- [ ] Prepare for interviews (Section 25: Interview Preparation)
- [ ] **Celebrate!** You've completed 90 days. Reflect on journey.

**Success Criteria:**
- ✅ All documentation polished and professional
- ✅ Architecture diagrams clear and compelling
- ✅ DORA metrics documented and impressive
- ✅ Portfolio website live
- ✅ Resume updated with Bibby
- ✅ 5-10 job applications submitted

**Resources:**
- Section 24: Portfolio showcase
- Section 25: Interview preparation
- levels.fyi (salary research)

---

## Post-90-Day: What's Next?

### Immediate (Weeks 13-16)

**Continue applying:**
- Apply to 5-10 jobs per week
- Follow up on applications after 1 week
- Network on LinkedIn (connect with recruiters, engineers)

**Prepare for interviews:**
- Review Section 25 (Interview Preparation)
- Practice STAR method answers
- Rehearse Bibby demo (6 minutes)
- Prepare questions to ask interviewers

**Keep learning:**
- Read "Site Reliability Engineering" (Google)
- Take AWS Solutions Architect Associate exam (if not done)
- Start learning Kubernetes (next logical step)

---

### Short-Term (Months 4-6)

**After landing a job:**
- Focus on your job (ramp up quickly)
- Apply Bibby lessons to work projects
- Document your learnings (private journal)

**Continue side projects (optional):**
- Migrate Bibby to Kubernetes (learn K8s hands-on)
- Add new features (REST API, frontend, search)
- Contribute to open source

**Build public presence:**
- Publish blog posts (dev.to or Medium)
- Share on LinkedIn and Twitter
- Engage with DevOps community

---

### Long-Term (Year 1-2)

**Career growth:**
- Follow Section 26 (Continuous Learning Path)
- Pursue certifications (CKA, Terraform Associate)
- Mentor junior engineers (once you're comfortable)
- Speak at meetups (share Bibby story)

**Keep Bibby evolving:**
- Bibby v2: Kubernetes + Istio
- Bibby v3: Multi-region, multi-cloud
- Bibby becomes your portfolio centerpiece for years

---

## Appendix: Dealing with Common Challenges

### Challenge 1: "I'm Behind Schedule"

**Symptoms:**
- Week 4, still on Week 2 tasks
- Feeling overwhelmed
- Considering quitting

**Solutions:**
1. **Don't panic**: Most people underestimate time
2. **Adjust expectations**: 90 days is aggressive; 120 days is fine
3. **Identify blockers**: What's taking longest? (Usually AWS/Terraform)
4. **Cut scope**: Skip "nice-to-haves" (HTTPS, custom domain, Prometheus)
5. **Focus on core**: CLI + Docker + ECS + CI/CD = good enough for portfolio
6. **Extend timeline**: Add 2-4 weeks to each phase

**Remember**: Completing in 120 days is better than quitting at Day 45.

---

### Challenge 2: "I'm Spending Too Much on AWS"

**Symptoms:**
- AWS bill >$100/month
- NAT Gateway costing $32/month
- Fear of costs spiraling

**Solutions:**
1. **Review costs daily**: Use AWS Cost Explorer
2. **Biggest culprits**:
   - NAT Gateway ($32/month) → Use single NAT Gateway, not one per AZ
   - RDS Multi-AZ ($60/month) → Use single-AZ for learning
   - ECS tasks running 24/7 → Reduce to 1 task, or stop overnight
3. **Use Free Tier maximally**:
   - t2.micro EC2 (750 hours/month)
   - db.t2.micro RDS (750 hours/month)
   - ALB (750 hours/month)
4. **Destroy when not using**:
   - `terraform destroy` when not actively developing
   - Rebuild with `terraform apply` when ready
5. **Set up billing alerts**: $10, $25, $50

**Target**: <$60/month for learning. Destroy infrastructure when job hunting.

---

### Challenge 3: "Terraform Keeps Failing"

**Symptoms:**
- `terraform apply` errors
- State file conflicts
- Resources not creating correctly

**Solutions:**
1. **Read error messages carefully**: Terraform errors are usually descriptive
2. **Check AWS Console**: Verify resources are (or aren't) created
3. **Use `terraform plan` before `apply`**: Catch errors early
4. **Destroy and rebuild**: `terraform destroy && terraform apply` (nuclear option)
5. **Check IAM permissions**: CI/CD user might lack permissions
6. **Use Terraform docs**: https://registry.terraform.io/providers/hashicorp/aws/latest/docs
7. **Ask for help**: Reddit r/terraform, Stack Overflow, Discord

**Debugging tips:**
- `terraform state list` (see what Terraform knows about)
- `terraform state show <resource>` (inspect resource)
- `terraform refresh` (sync state with reality)

---

### Challenge 4: "I Don't Know What I Don't Know"

**Symptoms:**
- Stuck on a task, no idea where to start
- Googling doesn't help (too many conflicting answers)
- Feeling lost

**Solutions:**
1. **Start with official docs**: AWS docs, Terraform docs (not random blogs)
2. **Break down the problem**: "Deploy to ECS" → "What is ECS?" → "How do I create ECS cluster?" → etc.
3. **Use this guide**: Re-read relevant sections (don't skip ahead)
4. **Ask specific questions**: "How do I configure ECS task definition?" (not "How do I DevOps?")
5. **Join communities**:
   - Reddit: r/devops, r/aws, r/terraform
   - Discord: DevOps, AWS, Terraform servers
   - Stack Overflow
6. **Hire a mentor** (if budget allows): 1-2 hours with expert can save days

**Remember**: Everyone was a beginner once. Don't be afraid to ask "stupid questions"—they're not stupid if you don't know the answer.

---

### Challenge 5: "I'm Burned Out"

**Symptoms:**
- Dreading opening laptop
- Procrastinating on tasks
- Not enjoying what you used to love

**Solutions:**
1. **Take a break**: 3-7 days off completely
2. **Reduce hours**: 15-20 hours/week is sustainable; 40 hours/week is not
3. **Celebrate progress**: Look at what you've built (it's impressive!)
4. **Adjust expectations**: Completing in 120 days is fine
5. **Remember why you started**: Visualize getting your first DevOps job
6. **Talk to someone**: Friend, mentor, therapist

**This is a marathon, not a sprint.** Burning out helps no one. Rest is productive.

---

## Summary

### The 90-Day Journey (Recap)

**Phase 1 (Weeks 1-4): Foundation**
- Build Bibby CLI application
- Containerize with Docker
- Optimize Docker images
- **Deliverable**: Bibby running locally in Docker

**Phase 2 (Weeks 5-8): Cloud Deployment**
- Set up AWS account and Terraform
- Create VPC, ECS, RDS with Terraform
- Deploy Bibby to AWS ECS
- **Deliverable**: Bibby running in production on AWS

**Phase 3 (Weeks 9-10): Automation**
- Build CI pipeline (test, scan, build)
- Build CD pipeline (deploy, rollback)
- **Deliverable**: Automated deployments (Git → production)

**Phase 4 (Weeks 11-12): Excellence**
- Set up monitoring and alerting
- Track DORA metrics
- Polish documentation and portfolio
- Apply to jobs
- **Deliverable**: Complete portfolio and job applications

---

### Your Achievement

**If you complete this plan, you will have:**

✅ **A production-grade system**
- Java/Spring Boot CLI application
- Dockerized and optimized (192MB images)
- Deployed to AWS ECS with RDS
- 99%+ uptime

✅ **Complete CI/CD pipeline**
- Automated testing (80%+ coverage)
- Security scanning (Trivy, Snyk)
- Automated deployments
- Blue-green deployments
- Rollback capability

✅ **Infrastructure as Code**
- Terraform managing all AWS resources
- Modular, reusable code
- S3 backend with state locking

✅ **Observability**
- CloudWatch dashboards
- Grafana dashboards
- CloudWatch alarms
- DORA metrics

✅ **Elite DORA Metrics**
- Deployment frequency: 15+/week
- Lead time: <3 hours
- Change failure rate: <5%
- MTTR: <15 minutes

✅ **Professional portfolio**
- GitHub repo with impressive README
- Architecture documentation
- Blog posts
- Portfolio website

**This puts you in the top 5% of DevOps engineer candidates.**

Most candidates:
- Have theoretical knowledge (certifications, courses)
- Have no public portfolio
- Have never deployed to production

You:
- Have practical, hands-on experience
- Have public GitHub repo demonstrating skills
- Have operated a production system

**You are not a "junior" engineer. You are a production-ready DevOps engineer.**

---

### Final Thoughts

**This is just the beginning.**

Bibby is your foundation. Your career is a 40-year journey.

Use Bibby to get your first role. Then:
- Apply these skills at work
- Build Bibby v2 (Kubernetes)
- Contribute to open source
- Mentor others
- Speak at conferences
- Write blog posts

**In 5 years, you'll look back at Bibby and smile.**

You'll remember:
- The late nights debugging Terraform
- The excitement of your first successful deployment
- The fear before hitting `terraform apply` in production
- The pride when you achieved Elite DORA metrics

**Bibby is more than a project. It's proof that you can learn, build, and ship production systems.**

**Now go build it. And when you land your first DevOps job, remember:**

**You earned it.**

---

**End of Section 27. End of DevOps Mentorship Guide.**

**27 sections. 40,000+ lines of guidance. One goal: Turn you into a production-ready DevOps engineer.**

**The rest is up to you.**

**Good luck. You've got this.**
