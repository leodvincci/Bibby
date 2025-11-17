# Section 23: Documentation & Runbooks - Knowledge Transfer & Operational Excellence

## Introduction

You've built an impressive DevOps pipeline with excellent metrics. But what happens when:

- **You're on vacation** and production goes down?
- **A new team member joins** and needs to understand the system?
- **An incident occurs at 3 AM** and someone needs to troubleshoot fast?
- **You're interviewing** and need to explain your architecture clearly?

**The answer: Documentation and Runbooks.**

Good documentation is the difference between:

```
❌ "Only you know how to deploy"
✅ "Anyone can deploy using the runbook"

❌ "System is down, where do I start?"
✅ "Follow the incident response runbook step-by-step"

❌ "How does this work?" (digging through code for hours)
✅ "Check the architecture doc" (understand in 15 minutes)
```

This section teaches you to create **professional-grade documentation** that makes you look like a senior engineer and enables your team to operate independently.

**What You'll Learn:**

1. **Documentation types** - README, architecture, API, runbooks, postmortems
2. **README best practices** - What makes a great project README
3. **Architecture documentation** - Diagrams, ADRs, system design
4. **Runbooks** - Step-by-step operational procedures
5. **Incident response** - On-call playbooks and escalation
6. **API documentation** - OpenAPI/Swagger integration
7. **Knowledge base** - Organizing documentation for teams
8. **Documentation as code** - Keeping docs in sync with code

**Prerequisites**: All previous sections

---

## 1. Documentation Types & When to Use Them

### 1.1 Documentation Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│ Level 1: ONBOARDING (for new team members)                 │
├─────────────────────────────────────────────────────────────┤
│ • README.md - Project overview, quick start                │
│ • CONTRIBUTING.md - How to contribute                       │
│ • ARCHITECTURE.md - High-level system design               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Level 2: DEVELOPMENT (for daily work)                      │
├─────────────────────────────────────────────────────────────┤
│ • API documentation - Endpoints, schemas                    │
│ • Code comments - Inline explanations                       │
│ • ADRs - Architecture Decision Records                      │
│ • Setup guides - Local development environment             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Level 3: OPERATIONS (for production support)               │
├─────────────────────────────────────────────────────────────┤
│ • Runbooks - Deployment, troubleshooting procedures        │
│ • Monitoring guides - Interpreting metrics, alerts          │
│ • Incident response - On-call procedures                    │
│ • Postmortems - Learning from failures                      │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Documentation Matrix for Bibby

| Document | Audience | Frequency | Tool |
|----------|----------|-----------|------|
| README.md | Everyone | First visit | GitHub |
| ARCHITECTURE.md | Engineers | Onboarding | GitHub + Diagrams.net |
| API Docs | Frontend devs | Daily | Swagger UI |
| Deployment Runbook | DevOps/Ops | Weekly | Notion/Confluence |
| Incident Response | On-call | During incidents | PagerDuty + Runbook |
| Postmortems | Team | After incidents | Google Docs |
| ADRs | Architects | Major decisions | GitHub (docs/adr/) |

---

## 2. README.md - Your Project's Front Door

### 2.1 Anatomy of a Great README

```markdown
# Project Name
Brief description (1-2 sentences)

[Badges: Build status, coverage, license, etc.]

## What is Bibby?

Bibby is a command-line library management system built with Spring Boot...

## Features

- ✅ Add, update, delete books
- ✅ Search by title, author, ISBN
- ✅ Track reading status
- ✅ Export to various formats

## Quick Start

[Code block showing 3 commands to get started]

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [API Documentation](http://localhost:8080/swagger-ui.html)
- [Deployment Guide](docs/DEPLOYMENT.md)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md)

## License

MIT
```

### 2.2 Complete Bibby README.md

**Create `README.md` in repository root:**

```markdown
# Bibby - Library Management CLI

A modern, cloud-native library management system with complete DevOps pipeline.

[![Build Status](https://github.com/yourusername/Bibby/actions/workflows/main-build-deploy.yml/badge.svg)](https://github.com/yourusername/Bibby/actions)
[![Coverage](https://codecov.io/gh/yourusername/Bibby/branch/main/graph/badge.svg)](https://codecov.io/gh/yourusername/Bibby)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 🎯 What is Bibby?

Bibby is a Spring Boot-based library management system that demonstrates professional software engineering and DevOps practices. Built as a portfolio project, it showcases:

- **Modern Java Development**: Spring Boot 3.5.7, Spring Shell, Spring Data JPA
- **Cloud-Native Architecture**: Docker, AWS ECS, RDS PostgreSQL
- **Complete CI/CD**: GitHub Actions, automated testing, security scanning
- **Production Monitoring**: Prometheus, Grafana, CloudWatch
- **Elite DevOps Metrics**: 15 deployments/week, 3-hour lead time, 5% failure rate

## ✨ Features

### Core Functionality
- 📚 **Book Management**: Add, update, delete, search books
- 👤 **Author Management**: Track authors and their works
- 📖 **Reading Status**: Track reading progress (To Read, Reading, Completed)
- 🔍 **Advanced Search**: Filter by title, author, ISBN, status
- 📊 **Export**: Export library to CSV, JSON

### DevOps & Infrastructure
- 🚀 **Automated CI/CD**: Push to production in 20 minutes
- 🐳 **Docker**: Multi-stage builds (192MB optimized image)
- ☁️ **AWS Deployment**: ECS Fargate, RDS, ALB, auto-scaling
- 📈 **Monitoring**: 99.97% uptime, P95 response 320ms
- 🔒 **Security**: Automated vulnerability scanning, zero CRITICAL/HIGH issues

## 🚀 Quick Start

### Local Development (Docker Compose)

```bash
# Clone repository
git clone https://github.com/yourusername/Bibby.git
cd Bibby

# Start all services (app + database + monitoring)
docker compose up -d

# Access application
curl http://localhost:8080/actuator/health
# {"status":"UP"}

# View logs
docker compose logs -f app
```

### Local Development (Native)

```bash
# Prerequisites: Java 17, Maven 3.9+, PostgreSQL 15

# Start PostgreSQL
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=bibby \
  -e POSTGRES_USER=bibby_admin \
  -e POSTGRES_PASSWORD=dev_password \
  postgres:15-alpine

# Run application
mvn spring-boot:run

# Run tests
mvn test

# Build
mvn clean package
```

## 📚 Documentation

### For Developers
- **[Architecture Overview](docs/ARCHITECTURE.md)** - System design, component diagram
- **[API Documentation](http://localhost:8080/swagger-ui.html)** - Interactive API explorer
- **[Contributing Guide](CONTRIBUTING.md)** - How to contribute
- **[Development Setup](docs/DEVELOPMENT.md)** - Detailed local setup

### For Operations
- **[Deployment Runbook](docs/runbooks/DEPLOYMENT.md)** - Deploy to production
- **[Incident Response](docs/runbooks/INCIDENT_RESPONSE.md)** - On-call procedures
- **[Monitoring Guide](docs/MONITORING.md)** - Metrics, dashboards, alerts

### For DevOps Learning
- **[DevOps Mentorship Guide](docs/devops-mentorship/)** - 22 sections covering:
  - SDLC, Agile, Git workflows
  - CI/CD, testing, security
  - Docker, AWS, Terraform
  - Monitoring, metrics, KPIs

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Users / API Clients                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓
            ┌────────────────────────┐
            │  Application Load      │
            │  Balancer (ALB)        │
            └────────────┬───────────┘
                         │
         ┌───────────────┼───────────────┐
         ↓               ↓               ↓
    ┌────────┐     ┌────────┐     ┌────────┐
    │  ECS   │     │  ECS   │     │  ECS   │
    │  Task  │     │  Task  │     │  Task  │
    │ (App)  │     │ (App)  │     │ (App)  │
    └───┬────┘     └───┬────┘     └───┬────┘
        │              │              │
        └──────────────┼──────────────┘
                       │
                       ↓
            ┌──────────────────────┐
            │   RDS PostgreSQL     │
            │   (Multi-AZ)         │
            └──────────────────────┘
```

**Tech Stack:**
- **Backend**: Java 17, Spring Boot 3.5.7, Spring Shell, Spring Data JPA
- **Database**: PostgreSQL 15
- **Infrastructure**: AWS ECS Fargate, RDS, ALB, CloudWatch
- **CI/CD**: GitHub Actions, Docker, AWS ECR
- **Monitoring**: Prometheus, Grafana, CloudWatch
- **IaC**: Terraform

## 📊 DevOps Metrics (DORA)

| Metric | Value | Level |
|--------|-------|-------|
| **Deployment Frequency** | 15/week | High |
| **Lead Time for Changes** | 3 hours | Elite |
| **Change Failure Rate** | 5% | Elite |
| **Mean Time to Recovery** | 12 minutes | Elite |

**Infrastructure:**
- **Availability**: 99.97%
- **Response Time (P95)**: 320ms
- **Cost**: $60/month
- **Auto-scaling**: 3-7 tasks based on load

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html

# Run integration tests
mvn verify -P integration-tests
```

**Current Coverage**: 85% (target: 80%)

## 🚢 Deployment

### Production Deployment

Deployments are automated via GitHub Actions:

```bash
# Push to main → Deploys to dev automatically
git push origin main

# Deploy to staging (manual approval)
gh workflow run promote-staging.yml -f version=0.3.0-a1b2c3d

# Deploy to production (manual approval + blue-green)
gh workflow run promote-production.yml -f version=0.3.0-a1b2c3d
```

See [Deployment Runbook](docs/runbooks/DEPLOYMENT.md) for details.

### Manual Deployment (Emergency)

```bash
# Build Docker image
docker build -t bibby:0.3.0 .

# Tag for ECR
docker tag bibby:0.3.0 123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:0.3.0

# Push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:0.3.0

# Update ECS service
aws ecs update-service --cluster bibby-production --service bibby-production --force-new-deployment
```

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Code of conduct
- Development workflow
- Commit message conventions
- Pull request process

## 📄 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

Built as part of a comprehensive DevOps learning journey. Special thanks to:
- [DORA](https://dora.dev/) for DevOps metrics research
- [Spring Boot](https://spring.io/projects/spring-boot) team
- [Docker](https://www.docker.com/) and container ecosystem

## 📧 Contact

- **Author**: Your Name
- **GitHub**: [@yourusername](https://github.com/yourusername)
- **LinkedIn**: [your-profile](https://linkedin.com/in/your-profile)
- **Email**: your.email@example.com

---

**⭐ If you find this project helpful for learning DevOps, please star the repository!**
```

### 2.3 README Badges

**Add badges for credibility:**

```markdown
<!-- Build status -->
[![Build](https://github.com/yourusername/Bibby/actions/workflows/main-build-deploy.yml/badge.svg)](https://github.com/yourusername/Bibby/actions)

<!-- Code coverage -->
[![Coverage](https://codecov.io/gh/yourusername/Bibby/branch/main/graph/badge.svg)](https://codecov.io/gh/yourusername/Bibby)

<!-- Security -->
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=bibby&metric=security_rating)](https://sonarcloud.io/dashboard?id=bibby)

<!-- License -->
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

<!-- Tech stack -->
[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue)](https://www.docker.com/)
[![AWS](https://img.shields.io/badge/AWS-ECS%20%7C%20RDS-orange)](https://aws.amazon.com/)
```

---

## 3. Architecture Documentation

### 3.1 ARCHITECTURE.md

**Create `docs/ARCHITECTURE.md`:**

```markdown
# Bibby Architecture

## System Overview

Bibby is a cloud-native library management system deployed on AWS using a microservices-ready architecture with complete CI/CD automation.

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                         PRODUCTION (AWS)                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Route 53                                                        │
│     │                                                            │
│     └──→ ACM Certificate (SSL/TLS)                             │
│            │                                                     │
│            ↓                                                     │
│     ┌─────────────────┐                                         │
│     │  Application    │                                         │
│     │  Load Balancer  │                                         │
│     │  (ALB)          │                                         │
│     └────────┬────────┘                                         │
│              │                                                   │
│      ┌───────┼───────┐                                         │
│      ↓       ↓       ↓                                         │
│  ┌──────┐┌──────┐┌──────┐          ┌──────────────┐          │
│  │ ECS  ││ ECS  ││ ECS  │          │ CloudWatch   │          │
│  │ Task ││ Task ││ Task │─────────→│ Logs/Metrics │          │
│  └───┬──┘└───┬──┘└───┬──┘          └──────────────┘          │
│      │       │       │                     │                   │
│      └───────┼───────┘                     ↓                   │
│              │                   ┌──────────────────┐          │
│              ↓                   │   SNS Topics     │          │
│     ┌─────────────────┐         │   (Alerts)       │          │
│     │  RDS PostgreSQL │         └────────┬─────────┘          │
│     │  (Multi-AZ)     │                  │                     │
│     └─────────────────┘                  ↓                     │
│              │                   ┌──────────────────┐          │
│              ↓                   │  PagerDuty /     │          │
│     ┌─────────────────┐         │  Slack           │          │
│     │  RDS Read       │         └──────────────────┘          │
│     │  Replica        │                                        │
│     │  (Optional)     │                                        │
│     └─────────────────┘                                        │
│                                                                 │
└──────────────────────────────────────────────────────────────────┘
```

## Component Details

### Application Layer (ECS Fargate)

**Technology**: Java 17, Spring Boot 3.5.7, Spring Shell

**Responsibilities**:
- RESTful API endpoints
- Business logic
- Data validation
- Database access via JPA

**Configuration**:
- CPU: 1 vCPU per task
- Memory: 2GB per task
- Desired count: 3 tasks (production)
- Auto-scaling: 2-10 tasks based on CPU (target 70%)

**Health Checks**:
- Endpoint: `/actuator/health`
- Interval: 30s
- Timeout: 5s
- Healthy threshold: 2 consecutive successes
- Unhealthy threshold: 3 consecutive failures

### Database Layer (RDS PostgreSQL)

**Technology**: PostgreSQL 15

**Configuration**:
- Instance: db.t4g.medium (2 vCPU, 4GB RAM)
- Storage: 100GB GP3 SSD
- Multi-AZ: Enabled (production)
- Backup: Automated daily, 7-day retention
- Maintenance window: Sunday 4-5 AM EST

**Connection Pooling**:
- HikariCP (Spring Boot default)
- Maximum pool size: 10 connections per task
- Total: 30 connections max (3 tasks × 10)

### Load Balancer (ALB)

**Configuration**:
- Scheme: Internet-facing
- Listeners:
  - HTTP (port 80) → Redirect to HTTPS
  - HTTPS (port 443) → Forward to ECS tasks
- Health check: `/actuator/health`
- Stickiness: Enabled (7 days)

### Networking

**VPC**:
- CIDR: 10.0.0.0/16
- Availability Zones: 3 (us-east-1a, us-east-1b, us-east-1c)

**Subnets**:
- Public subnets (3): ALB, NAT Gateways
- Private subnets (3): ECS tasks, RDS

**Security Groups**:
- ALB: Inbound 80, 443 from internet
- ECS: Inbound 8080 from ALB only
- RDS: Inbound 5432 from ECS only

## Data Flow

### Request Flow

```
1. User → Route53 DNS resolution
2. Route53 → ALB (HTTPS)
3. ALB → ECS Task (HTTP internal)
4. ECS Task → Process request
5. ECS Task → RDS (query data)
6. RDS → ECS Task (return data)
7. ECS Task → ALB (response)
8. ALB → User (HTTPS)
```

### Deployment Flow

```
1. Developer → git push to main
2. GitHub Actions → Build & test
3. GitHub Actions → Build Docker image
4. GitHub Actions → Scan for vulnerabilities
5. GitHub Actions → Push to ECR
6. GitHub Actions → Update ECS task definition
7. ECS → Blue-green deployment
8. ALB → Shift traffic gradually (10% → 50% → 100%)
9. ECS → Terminate old tasks
10. CloudWatch → Metrics recorded
```

## Technology Decisions

### ADR-001: Why Spring Boot?

**Status**: Accepted

**Context**: Need a mature, production-ready Java framework.

**Decision**: Use Spring Boot 3.5.7 with Spring Shell for CLI.

**Consequences**:
- ✅ Extensive ecosystem (Data JPA, Actuator, Security)
- ✅ Built-in health checks, metrics
- ✅ Wide community support
- ❌ Learning curve for Spring specifics
- ❌ Heavier than lightweight frameworks

### ADR-002: Why ECS over Kubernetes?

**Status**: Accepted

**Context**: Need container orchestration, but Kubernetes is complex for a single application.

**Decision**: Use AWS ECS Fargate.

**Consequences**:
- ✅ Simpler than Kubernetes
- ✅ Tight AWS integration
- ✅ No server management (Fargate)
- ✅ Cost-effective for small scale
- ❌ AWS vendor lock-in
- ❌ Less portable than Kubernetes

### ADR-003: Why PostgreSQL over MySQL?

**Status**: Accepted

**Context**: Need relational database with good performance.

**Decision**: PostgreSQL 15.

**Consequences**:
- ✅ Superior query optimizer
- ✅ Better JSON support
- ✅ ACID compliance
- ✅ Advanced features (CTEs, window functions)
- ❌ Slightly higher memory usage than MySQL

## Scalability

### Current Capacity

- **Throughput**: 120 RPS average, tested up to 500 RPS
- **Concurrent users**: 450 DAU, supports up to 2,000
- **Database**: 12GB used, 100GB allocated
- **Cost**: $60/month current, scales linearly

### Scaling Strategy

**Horizontal Scaling** (preferred):
- ECS auto-scaling: 2-10 tasks based on CPU
- Database read replicas (if needed)
- CloudFront CDN (for static assets)

**Vertical Scaling** (if needed):
- ECS: Increase task CPU/memory
- RDS: Upgrade to db.r6g.large

**Limits**:
- Database connections: 100 max on db.t4g.medium
- With 10 tasks @ 10 connections each = 100 connections (at limit)
- Solution: Add RDS Proxy or upgrade instance class

## Security

### Network Security

- Private subnets for application and database
- Security groups allow minimum required ports
- NAT Gateway for outbound internet (updates, ECR)

### Application Security

- Secrets in AWS Secrets Manager (not environment variables)
- SSL/TLS termination at ALB
- HTTPS only in production
- Regular vulnerability scanning (Trivy, Snyk)

### Access Control

- IAM roles for ECS tasks (principle of least privilege)
- No hardcoded credentials
- MFA required for AWS console access
- GitHub branch protection on main

## Monitoring & Observability

### Metrics

- **Application**: Spring Boot Actuator → Prometheus → Grafana
- **Infrastructure**: CloudWatch (CPU, memory, requests)
- **Business**: Custom metrics (user actions, feature usage)

### Logging

- **Destination**: CloudWatch Logs
- **Format**: JSON structured logging
- **Retention**: 30 days
- **Aggregation**: CloudWatch Logs Insights

### Alerting

- CPU > 80% for 5 minutes → Warning
- Memory > 85% for 5 minutes → Warning
- Error rate > 1% for 5 minutes → Critical
- Unhealthy targets > 0 for 2 minutes → Critical
- Delivery: SNS → PagerDuty → Slack

## Disaster Recovery

### Backup Strategy

- **Database**: Automated daily backups, 7-day retention
- **Point-in-time recovery**: 5-minute RPO
- **Infrastructure**: Terraform state in S3
- **Secrets**: Secrets Manager with automatic rotation

### Recovery Procedures

- **RDS Failure**: Automatic failover to standby (Multi-AZ) - 60-120 seconds
- **AZ Failure**: ALB routes to healthy AZs automatically
- **Region Failure**: Manual failover to DR region (requires Terraform apply)

### RTO/RPO

- **Recovery Time Objective (RTO)**: <15 minutes
- **Recovery Point Objective (RPO)**: <5 minutes

## Cost Breakdown

| Service | Configuration | Monthly Cost |
|---------|---------------|--------------|
| ECS Fargate | 3 tasks × 1vCPU, 2GB | $28.80 |
| RDS | db.t4g.medium Multi-AZ | $18.90 |
| ALB | Internet-facing | $7.20 |
| NAT Gateway | 3 AZs | $3.60 |
| ECR | Image storage | $1.20 |
| CloudWatch | Logs + Metrics | $0.60 |
| **Total** | | **$60.30** |

## Future Enhancements

1. **Caching**: Add Redis for frequently accessed data
2. **Search**: Add Elasticsearch for advanced search
3. **Async Processing**: Add SQS for background jobs
4. **Multi-region**: Deploy to us-west-2 for DR
5. **CDN**: Add CloudFront for static assets

## References

- [AWS ECS Best Practices](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/intro.html)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [PostgreSQL Performance Tuning](https://wiki.postgresql.org/wiki/Performance_Optimization)
```

---

## 4. Runbooks - Operational Procedures

### 4.1 What Makes a Good Runbook?

**Characteristics**:
- ✅ **Step-by-step**: No assumptions, explicit commands
- ✅ **Copy-pasteable**: Commands ready to execute
- ✅ **Tested**: Actually works when followed
- ✅ **Updated**: Reflects current system state
- ✅ **Accessible**: Available during outages

### 4.2 Deployment Runbook

**Create `docs/runbooks/DEPLOYMENT.md`:**

```markdown
# Deployment Runbook

**Last Updated**: 2025-01-17
**Owner**: DevOps Team
**Audience**: Engineers with production access

## Overview

This runbook covers standard and emergency deployment procedures for Bibby.

## Prerequisites

- [ ] AWS CLI configured with production credentials
- [ ] GitHub access token with `repo` and `workflow` scopes
- [ ] PagerDuty access for creating maintenance windows
- [ ] Slack access for notifications

## Standard Deployment (Automated)

### 1. Pre-Deployment Checklist

- [ ] All tests passing on `main` branch
- [ ] Security scans clean (zero CRITICAL/HIGH)
- [ ] Staging deployment successful
- [ ] Load tests completed (if significant changes)
- [ ] Rollback plan identified

### 2. Create Maintenance Window

```bash
# In PagerDuty: Create maintenance window
# Duration: 30 minutes
# Services: Bibby Production
# Reason: Deploying version X.Y.Z
```

### 3. Trigger Deployment

```bash
# Get latest successful build version
gh run list --workflow="Build and Deploy to Dev" --limit 1 --json headSha --jq '.[0].headSha'

# Example output: a1b2c3d

# Trigger production deployment
gh workflow run promote-production.yml -f version=0.3.0-a1b2c3d

# Monitor deployment
gh run watch
```

### 4. Verify Deployment

**Check ECS Service**:
```bash
aws ecs describe-services \
  --cluster bibby-production \
  --services bibby-production \
  --query 'services[0].deployments' \
  --output table

# Expected: 1 deployment with status "PRIMARY" and desiredCount == runningCount
```

**Check Health**:
```bash
# Get ALB DNS
ALB_DNS=$(aws elbv2 describe-load-balancers \
  --names bibby-production-alb \
  --query 'LoadBalancers[0].DNSName' \
  --output text)

# Health check
curl -f https://$ALB_DNS/actuator/health

# Expected: {"status":"UP"}
```

**Check Logs**:
```bash
aws logs tail /ecs/bibby-production --follow --since 5m
```

**Check Metrics**:
```bash
# Open Grafana dashboard
open https://grafana.example.com/d/bibby-production
```

### 5. Post-Deployment

- [ ] Verify metrics stable (5 minutes observation)
- [ ] Run smoke tests
- [ ] Close maintenance window
- [ ] Post deployment announcement in Slack

**Slack Notification**:
```
:rocket: Bibby v0.3.0 deployed to production
• Deployment time: 18 minutes
• Health: ✅ All systems operational
• Metrics: ✅ Within normal ranges
• Rollback: Available if needed (use runbook)
```

## Emergency Deployment (Hotfix)

### When to Use

- Critical production bug
- Security vulnerability
- Data integrity issue

### Procedure

**1. Create hotfix branch**:
```bash
git checkout main
git pull
git checkout -b hotfix/critical-bug-fix
```

**2. Make minimal changes**:
```bash
# Fix the bug
# Update version in pom.xml (increment patch: 0.3.0 → 0.3.1)
git add .
git commit -m "hotfix: Fix critical bug causing data loss"
git push origin hotfix/critical-bug-fix
```

**3. Fast-track review**:
```bash
# Create PR with [HOTFIX] prefix
gh pr create --title "[HOTFIX] Fix critical bug" --body "Critical: Data loss in book deletion"

# Request immediate review
# Get approval from 1 senior engineer (not 2 as usual)
```

**4. Deploy immediately**:
```bash
# Merge to main
gh pr merge --squash

# Wait for CI to complete
gh run watch

# Deploy to production (skip staging if critical)
gh workflow run promote-production.yml -f version=0.3.1-$GIT_SHA
```

**5. Monitor closely**:
```bash
# Watch deployment
aws ecs wait services-stable --cluster bibby-production --services bibby-production

# Verify fix
# ... test specific functionality ...

# Monitor for 15 minutes before closing incident
```

## Rollback Procedures

### Automatic Rollback

ECS deployment circuit breaker triggers automatic rollback if:
- Health checks fail after deployment
- Tasks fail to start

**No action needed** - monitor ECS console for status.

### Manual Rollback

**1. Identify previous version**:
```bash
# List recent deployments
gh run list --workflow="Deploy to Production" --limit 5

# Get previous successful version
PREVIOUS_VERSION="0.2.1-xyz789"
```

**2. Trigger rollback**:
```bash
gh workflow run promote-production.yml -f version=$PREVIOUS_VERSION
```

**3. Verify rollback**:
```bash
# Check running tasks
aws ecs list-tasks --cluster bibby-production --service-name bibby-production
aws ecs describe-tasks --cluster bibby-production --tasks <task-arn>

# Verify image tag matches previous version
```

**4. Post-rollback**:
- [ ] Update incident ticket
- [ ] Notify team in Slack
- [ ] Schedule postmortem
- [ ] Create ticket to fix and redeploy

## Database Migrations

**Caution**: Database changes require extra care.

### Safe Migration Pattern

**1. Make changes backward-compatible**:
```sql
-- ✅ Good: Add nullable column
ALTER TABLE books ADD COLUMN publisher VARCHAR(255);

-- ❌ Bad: Add non-nullable column (breaks old code)
ALTER TABLE books ADD COLUMN publisher VARCHAR(255) NOT NULL;
```

**2. Deploy in two phases**:

**Phase 1: Deploy code that supports old AND new schema**:
```java
// Code works with or without 'publisher' column
if (resultSet.getMetaData().getColumnCount() > 5) {
    book.setPublisher(resultSet.getString("publisher"));
}
```

**Phase 2: Run migration after code deployment**:
```bash
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://$PROD_DB
```

**Phase 3: Deploy code that requires new schema**:
```java
// Now safe to require 'publisher' column
book.setPublisher(resultSet.getString("publisher"));
```

### Emergency Rollback with Schema Changes

**If you must rollback after schema change**:

```sql
-- Immediately run rollback migration
-- Example: Remove column
ALTER TABLE books DROP COLUMN publisher;

-- Then rollback application code
gh workflow run promote-production.yml -f version=$PREVIOUS_VERSION
```

## Common Issues

### Issue: Tasks failing health checks

**Symptoms**: New tasks start but immediately fail health checks.

**Diagnosis**:
```bash
# Check task logs
aws logs tail /ecs/bibby-production --follow --since 10m | grep ERROR
```

**Common causes**:
- Database connection failure → Check RDS security group
- Missing environment variable → Check task definition
- Application error → Check application logs

**Resolution**:
- Fix issue and redeploy OR
- Rollback to previous version

### Issue: High CPU after deployment

**Symptoms**: CPU spikes to 90%+ after deployment.

**Diagnosis**:
```bash
# Check CPU metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ClusterName,Value=bibby-production Name=ServiceName,Value=bibby-production \
  --start-time $(date -u -d '15 minutes ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 60 \
  --statistics Average
```

**Common causes**:
- Infinite loop in code
- N+1 query problem
- Excessive logging

**Resolution**:
- Rollback immediately
- Investigate in staging environment
- Fix and redeploy

### Issue: Database connection pool exhausted

**Symptoms**: "Cannot get connection from pool" errors.

**Diagnosis**:
```bash
# Check database connections
aws rds describe-db-instances \
  --db-instance-identifier bibby-production \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text

# Connect to database and check connections
psql -h <endpoint> -U bibby_admin -d bibby -c "SELECT count(*) FROM pg_stat_activity;"
```

**Resolution**:
```bash
# Increase connection pool size in task definition
# OR
# Restart tasks to reset connections
aws ecs update-service \
  --cluster bibby-production \
  --service bibby-production \
  --force-new-deployment
```

## Contacts

| Role | Name | Contact |
|------|------|---------|
| On-Call Engineer | PagerDuty | Auto-pages |
| DevOps Lead | [Name] | Slack: @devops-lead |
| Database Admin | [Name] | Slack: @dba |
| Security | [Name] | Slack: @security |

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2025-01-17 | Initial runbook | You |
```

---

Due to length, I'll create a summary and continue with remaining sections in the next response.

## Summary (Part 1 of Section 23)

**What You've Learned:**

1. **Documentation Types**
   - Hierarchy: Onboarding → Development → Operations
   - Matrix for different audiences and use cases

2. **Professional README.md**
   - Complete Bibby README with badges, quick start, features
   - Architecture diagram, metrics, testing, deployment
   - Contributing and licensing sections

3. **Architecture Documentation**
   - Complete ARCHITECTURE.md with component details
   - System diagrams, data flow, technology decisions (ADRs)
   - Scalability, security, DR, cost breakdown

4. **Deployment Runbook**
   - Standard deployment procedure (automated)
   - Emergency hotfix procedure
   - Rollback procedures (automatic and manual)
   - Database migration safety patterns
   - Common issues and troubleshooting

---

## 5. Incident Response Runbooks

### 5.1 Incident Response Framework

**Create `docs/runbooks/INCIDENT_RESPONSE.md`:**

```markdown
# Incident Response Runbook

**Last Updated**: 2025-01-17
**Owner**: On-Call Team
**Audience**: All engineers with production access

## Overview

This runbook provides step-by-step procedures for responding to production incidents in Bibby.

## Incident Severity Levels

| Level | Impact | Response Time | Examples |
|-------|--------|---------------|----------|
| **P1 - Critical** | Complete outage | 5 minutes | Production down, data loss |
| **P2 - High** | Major degradation | 15 minutes | High error rate, slow response |
| **P3 - Medium** | Minor degradation | 1 hour | Intermittent errors, single feature down |
| **P4 - Low** | No user impact | Next business day | Cosmetic issues, monitoring gaps |

## On-Call Schedule

- **Rotation**: Weekly (Monday to Monday)
- **Tool**: PagerDuty
- **Handoff**: Monday 9 AM EST
- **Handoff checklist**:
  - Review open incidents
  - Review upcoming deployments
  - Review recent postmortems

## Incident Response Process

```
┌─────────────────────────────────────────────────────────────┐
│ 1. DETECT                                                   │
│    • Alert fires (PagerDuty, Slack, Grafana)               │
│    • User report (Support, Slack)                           │
└────────────────────────┬────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. TRIAGE                                                   │
│    • Assess severity (P1-P4)                                │
│    • Create incident ticket                                 │
│    • Page appropriate team members                          │
└────────────────────────┬────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. COMMUNICATE                                              │
│    • Update status page                                     │
│    • Post in #incidents Slack channel                       │
│    • Notify stakeholders (for P1/P2)                        │
└────────────────────────┬────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. INVESTIGATE                                              │
│    • Gather data (logs, metrics, traces)                    │
│    • Form hypothesis                                        │
│    • Test hypothesis                                        │
└────────────────────────┬────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. MITIGATE                                                 │
│    • Apply temporary fix (rollback, scale, config)         │
│    • Verify mitigation working                              │
│    • Monitor for stability (15 min)                         │
└────────────────────────┬────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. RESOLVE                                                  │
│    • Implement permanent fix                                │
│    • Deploy fix following runbook                           │
│    • Verify resolution                                      │
└────────────────────────┬────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 7. POST-INCIDENT                                            │
│    • Update status page (resolved)                          │
│    • Close incident ticket                                  │
│    • Schedule postmortem (P1/P2 only)                       │
└─────────────────────────────────────────────────────────────┘
```

## P1 - Production Outage

### Scenario: "Production is completely down"

**Alert Example**:
```
PagerDuty: HIGH PRIORITY
Service: bibby-production
Alert: All health checks failing
Duration: 3 minutes
```

### Immediate Actions (First 5 minutes)

**1. Acknowledge alert**:
```bash
# Click "Acknowledge" in PagerDuty
# This stops escalation and shows you're responding
```

**2. Create incident**:
```bash
# In Slack #incidents channel
/incident create
Title: Production completely down
Severity: P1
Commander: @your-name
```

**3. Verify outage**:
```bash
# Check health endpoint
curl -f https://bibby.example.com/actuator/health
# Expected: Timeout or 503 error

# Check ALB target health
aws elbv2 describe-target-health \
  --target-group-arn arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/bibby-production/abc123

# Expected: All targets "unhealthy"
```

**4. Check recent changes**:
```bash
# Check recent deployments
gh run list --workflow="Deploy to Production" --limit 5

# Check ECS deployment status
aws ecs describe-services \
  --cluster bibby-production \
  --services bibby-production \
  --query 'services[0].deployments'
```

### Investigation (Next 10 minutes)

**5. Check application logs**:
```bash
# Tail recent logs
aws logs tail /ecs/bibby-production --follow --since 15m

# Look for:
# - Java exceptions (NullPointerException, SQLException)
# - "Application failed to start"
# - Database connection errors
```

**6. Check infrastructure**:
```bash
# ECS task status
aws ecs list-tasks \
  --cluster bibby-production \
  --service-name bibby-production \
  --desired-status RUNNING

# If no running tasks, check stopped tasks
aws ecs list-tasks \
  --cluster bibby-production \
  --desired-status STOPPED \
  --max-results 5

# Describe stopped task to see why it stopped
aws ecs describe-tasks \
  --cluster bibby-production \
  --tasks <task-arn> \
  --query 'tasks[0].stoppedReason'
```

**7. Check database**:
```bash
# RDS instance status
aws rds describe-db-instances \
  --db-instance-identifier bibby-production \
  --query 'DBInstances[0].DBInstanceStatus'

# Expected: "available"
# If "rebooting", "upgrading", "failed" → Database issue
```

### Mitigation (Based on Root Cause)

#### Scenario A: Bad Deployment

**Symptoms**: Deployment started <15 minutes ago, tasks crash on startup

**Fix: Rollback**
```bash
# Get previous successful version
PREVIOUS_VERSION=$(gh run list --workflow="Deploy to Production" --status success --limit 2 --json headSha --jq '.[1].headSha')

# Trigger rollback
gh workflow run promote-production.yml -f version=0.2.1-$PREVIOUS_VERSION

# Monitor rollback
aws ecs wait services-stable --cluster bibby-production --services bibby-production

# Verify health
curl -f https://bibby.example.com/actuator/health
```

**Expected MTTR**: 5-8 minutes

#### Scenario B: Database Connection Failure

**Symptoms**: Logs show "Connection refused" or "Connection timeout" to database

**Fix: Check database and security groups**
```bash
# 1. Verify RDS is running
aws rds describe-db-instances \
  --db-instance-identifier bibby-production \
  --query 'DBInstances[0].{Status:DBInstanceStatus,Endpoint:Endpoint.Address}'

# 2. Test database connectivity from ECS task
aws ecs execute-command \
  --cluster bibby-production \
  --task <task-arn> \
  --container app \
  --interactive \
  --command "nc -zv bibby-prod.abc123.us-east-1.rds.amazonaws.com 5432"

# 3. Check security group rules
aws ec2 describe-security-groups \
  --group-ids sg-rds123 \
  --query 'SecurityGroups[0].IpPermissions'

# 4. If security group is wrong, add correct rule
aws ec2 authorize-security-group-ingress \
  --group-id sg-rds123 \
  --protocol tcp \
  --port 5432 \
  --source-group sg-ecs456
```

**Expected MTTR**: 10-15 minutes

#### Scenario C: Out of Memory

**Symptoms**: Tasks being killed, logs show "OutOfMemoryError"

**Fix: Increase task memory**
```bash
# 1. Update task definition with more memory
# Current: 2GB → New: 4GB
aws ecs register-task-definition \
  --family bibby-production \
  --memory 4096 \
  --cpu 2048 \
  --container-definitions file://container-definitions.json

# 2. Update service to use new task definition
aws ecs update-service \
  --cluster bibby-production \
  --service bibby-production \
  --task-definition bibby-production:LATEST

# 3. Wait for new tasks to become healthy
aws ecs wait services-stable --cluster bibby-production --services bibby-production
```

**Expected MTTR**: 8-12 minutes

### Communication Templates

**Initial Notification** (within 5 minutes):
```
#incidents
🚨 P1 Incident: Production Outage
Status: INVESTIGATING
Impact: All users unable to access Bibby
Started: 2025-01-17 14:32 EST
Commander: @your-name
Current action: Checking recent deployments and logs
Next update: 14:45 EST (in 10 min)
```

**Update** (every 15 minutes during P1):
```
#incidents
Update on P1 Incident:
Status: MITIGATING
Root cause: Bad deployment (v0.3.1) introduced NPE in startup code
Action taken: Rollback to v0.3.0 in progress
ETA to resolution: 5 minutes
Next update: 15:00 EST
```

**Resolution**:
```
#incidents
✅ P1 Incident RESOLVED
Duration: 18 minutes (14:32-14:50 EST)
Root cause: NullPointerException in new feature code
Resolution: Rollback to previous version v0.3.0
Impact: ~450 users affected, 18 minutes downtime
Follow-up:
  • Postmortem scheduled for 2025-01-18 10 AM
  • Fix PR in progress
  • Enhanced testing added to prevent recurrence
```

## P2 - High Error Rate

### Scenario: "Error rate suddenly spiked to 15%"

**Investigation Steps**:

```bash
# 1. Check current error rate
aws cloudwatch get-metric-statistics \
  --namespace Bibby/Application \
  --metric-name ErrorRate \
  --start-time $(date -u -d '30 minutes ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 60 \
  --statistics Average

# 2. Identify which endpoints are failing
aws logs insights query --log-group-name /ecs/bibby-production --query-string '
fields @timestamp, @message
| filter status >= 500
| stats count() by endpoint
| sort count desc
' --start-time $(date -d '30 minutes ago' +%s) --end-time $(date +%s)

# 3. Check specific error messages
aws logs tail /ecs/bibby-production --since 30m --filter-pattern "ERROR"

# 4. Check database performance
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name DatabaseConnections \
  --dimensions Name=DBInstanceIdentifier,Value=bibby-production \
  --start-time $(date -u -d '30 minutes ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 60 \
  --statistics Maximum
```

**Common Fixes**:
- Database slow query → Add index, optimize query
- External API timeout → Increase timeout, add circuit breaker
- Memory leak → Restart tasks, investigate in staging

## Escalation Path

```
Level 1 (0-15 min):  On-call Engineer (PagerDuty auto-pages)
         ↓
Level 2 (15-30 min): DevOps Lead (manual page if needed)
         ↓
Level 3 (30-60 min): Engineering Manager + CTO (for P1 only)
```

## Post-Incident Actions

### For P1/P2 Incidents

1. **Schedule postmortem** within 24-48 hours
2. **Document timeline** in incident ticket
3. **Create action items** for prevention
4. **Update runbooks** with learnings

### For P3/P4 Incidents

1. **Document fix** in ticket
2. **Create prevention ticket** (if applicable)
3. **No postmortem required** (unless valuable learning)

## Useful Commands

### Quick Health Check
```bash
# All-in-one health check script
cat > health-check.sh << 'EOF'
#!/bin/bash
echo "=== Bibby Health Check ==="
echo "1. Application Health:"
curl -s https://bibby.example.com/actuator/health | jq .

echo -e "\n2. ECS Service Status:"
aws ecs describe-services --cluster bibby-production --services bibby-production \
  --query 'services[0].{Desired:desiredCount,Running:runningCount,Pending:pendingCount}' --output table

echo -e "\n3. RDS Status:"
aws rds describe-db-instances --db-instance-identifier bibby-production \
  --query 'DBInstances[0].{Status:DBInstanceStatus,CPU:ProcessorFeatures}' --output table

echo -e "\n4. Recent Errors (last 15 min):"
aws logs tail /ecs/bibby-production --since 15m --filter-pattern ERROR | head -20
EOF

chmod +x health-check.sh
./health-check.sh
```

### Force Task Restart
```bash
# Restart all tasks (rolling restart)
aws ecs update-service \
  --cluster bibby-production \
  --service bibby-production \
  --force-new-deployment
```

### Emergency Database Connection Reset
```bash
# Kill idle connections (use with caution!)
psql -h bibby-prod.abc123.us-east-1.rds.amazonaws.com -U bibby_admin -d bibby -c "
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'idle'
  AND state_change < current_timestamp - interval '5 minutes'
  AND pid <> pg_backend_pid();
"
```

## Contacts

| Role | Contact | When to Page |
|------|---------|--------------|
| On-Call Engineer | PagerDuty | Automatic for P1/P2 |
| DevOps Lead | Slack + Phone | P1 lasting >15 min |
| Database Admin | Slack | Database issues |
| Security Team | Slack | Security incidents |
| Engineering Manager | Phone | P1 lasting >30 min |

## References

- [Deployment Runbook](DEPLOYMENT.md)
- [Monitoring Dashboard](https://grafana.example.com/d/bibby-production)
- [PagerDuty Console](https://bibby.pagerduty.com)
```

---

## 6. API Documentation

### 6.1 Why API Documentation Matters

Even for a CLI application like Bibby, exposing HTTP endpoints (via Spring Boot Actuator and potential future REST API) requires clear documentation:

- **Internal teams** need to understand endpoints
- **Monitoring tools** need endpoint specs
- **Future integrations** need API contracts

### 6.2 Swagger/OpenAPI Integration

**For Bibby, we'll use SpringDoc** (replaces legacy Springfox):

**Step 1: Add dependency to `pom.xml`:**

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

**Step 2: Configure in `application.properties`:**

```properties
# OpenAPI Documentation
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true

# API Information
springdoc.info.title=Bibby Library Management API
springdoc.info.description=REST API for Bibby library management system
springdoc.info.version=0.3.0
springdoc.info.contact.name=DevOps Team
springdoc.info.contact.email=devops@example.com
```

**Step 3: Create OpenAPI configuration class:**

**Create `src/main/java/com/penrose/bibby/config/OpenApiConfig.java`:**

```java
package com.penrose.bibby.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bibbyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bibby Library Management API")
                        .description("""
                                RESTful API for Bibby library management system.

                                ## Features
                                - Book CRUD operations
                                - Author management
                                - Search and filtering
                                - Reading status tracking

                                ## Authentication
                                Currently no authentication (educational project).
                                Production would use JWT or OAuth2.
                                """)
                        .version("0.3.0")
                        .contact(new Contact()
                                .name("DevOps Team")
                                .email("devops@example.com")
                                .url("https://github.com/yourusername/Bibby"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development"),
                        new Server()
                                .url("https://bibby-dev.example.com")
                                .description("Development environment"),
                        new Server()
                                .url("https://bibby.example.com")
                                .description("Production environment")));
    }
}
```

**Step 4: Add annotations to REST controllers:**

**Example: If Bibby had a REST controller for books:**

```java
package com.penrose.bibby.api;

import com.penrose.bibby.library.book.Book;
import com.penrose.bibby.library.book.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Book management endpoints")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @Operation(
            summary = "Get all books",
            description = "Retrieves a list of all books in the library"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved books",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Book.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        List<Book> books = bookService.getAllBooks();
        return ResponseEntity.ok(books);
    }

    @Operation(
            summary = "Get book by ISBN",
            description = "Retrieves a specific book by its ISBN"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Book found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Book.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Book not found"
            )
    })
    @GetMapping("/{isbn}")
    public ResponseEntity<Book> getBookByIsbn(
            @Parameter(description = "ISBN of the book", example = "978-0-7475-3269-9")
            @PathVariable String isbn) {
        return bookService.findByIsbn(isbn)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Add a new book",
            description = "Adds a new book to the library"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Book created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Book already exists"
            )
    })
    @PostMapping
    public ResponseEntity<Book> addBook(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Book to add",
                    required = true,
                    content = @Content(schema = @Schema(implementation = Book.class))
            )
            @RequestBody Book book) {
        Book savedBook = bookService.addBook(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedBook);
    }

    @Operation(
            summary = "Search books",
            description = "Search books by title, author, or ISBN"
    )
    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooks(
            @Parameter(description = "Search query", example = "Harry Potter")
            @RequestParam String query) {
        List<Book> results = bookService.search(query);
        return ResponseEntity.ok(results);
    }
}
```

**Step 5: Access Swagger UI:**

```bash
# Start application
mvn spring-boot:run

# Open Swagger UI in browser
open http://localhost:8080/swagger-ui.html

# Download OpenAPI spec (JSON)
curl http://localhost:8080/api-docs -o openapi.json

# Download OpenAPI spec (YAML)
curl http://localhost:8080/api-docs.yaml -o openapi.yaml
```

### 6.3 Swagger UI Features

**Interactive Documentation:**

```
┌─────────────────────────────────────────────────────────────┐
│ Bibby Library Management API                               │
│ Version 0.3.0                                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Books                                                       │
│                                                             │
│ GET  /api/v1/books          Get all books                  │
│      [Try it out]                                           │
│                                                             │
│ POST /api/v1/books          Add a new book                 │
│      [Try it out]                                           │
│                                                             │
│ GET  /api/v1/books/{isbn}   Get book by ISBN               │
│      [Try it out]                                           │
│      Parameters:                                            │
│      isbn* (path) - ISBN of the book                       │
│                     Example: 978-0-7475-3269-9             │
│                                                             │
│ GET  /api/v1/books/search   Search books                   │
│      [Try it out]                                           │
│      Parameters:                                            │
│      query* (query) - Search query                         │
│                       Example: Harry Potter                 │
└─────────────────────────────────────────────────────────────┘
```

**Example: Testing an endpoint:**

Click "Try it out" on `GET /api/v1/books/{isbn}`:

```
Parameters:
isbn: 978-0-7475-3269-9

[Execute]

Response:
Code: 200
Body:
{
  "isbn": "978-0-7475-3269-9",
  "title": "Harry Potter and the Philosopher's Stone",
  "author": "J.K. Rowling",
  "publicationYear": 1997,
  "status": "AVAILABLE"
}
```

### 6.4 Documenting Spring Boot Actuator Endpoints

**Actuator endpoints are auto-documented:**

```yaml
# In application.properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus

# Swagger will auto-discover these as:
GET /actuator/health     - Application health check
GET /actuator/info       - Application information
GET /actuator/metrics    - Available metrics
GET /actuator/prometheus - Prometheus metrics
```

---

## 7. Postmortem Templates

### 7.1 Blameless Postmortem Culture

**Key Principles:**

1. **No blame** - Focus on systems and processes, not people
2. **Learning** - What can we improve?
3. **Action items** - Concrete steps to prevent recurrence
4. **Transparency** - Share postmortems widely

### 7.2 Postmortem Template

**Create `docs/postmortems/TEMPLATE.md`:**

```markdown
# Postmortem: [Brief Description]

**Date**: YYYY-MM-DD
**Authors**: [Names]
**Status**: Draft | Under Review | Complete
**Severity**: P1 | P2 | P3 | P4

---

## Summary

*One paragraph summary of what happened, impact, and resolution.*

Example:
> On 2025-01-17 at 14:32 EST, Bibby production experienced a complete outage lasting 18 minutes affecting approximately 450 users. The outage was caused by a NullPointerException in newly deployed code (v0.3.1). The issue was resolved by rolling back to the previous version (v0.3.0). No data was lost.

---

## Impact

| Metric | Value |
|--------|-------|
| **Duration** | 18 minutes (14:32-14:50 EST) |
| **Affected Users** | ~450 (100% of active users) |
| **Failed Requests** | ~2,700 requests |
| **Revenue Impact** | $0 (no e-commerce) |
| **Data Loss** | None |
| **SLA Breach** | Yes (99.9% monthly SLA) |

---

## Timeline (All times EST)

| Time | Event | Action Taken |
|------|-------|--------------|
| 14:25 | Deploy v0.3.1 started | CI/CD pipeline initiated |
| 14:30 | Deploy completed | New tasks starting |
| 14:32 | **OUTAGE BEGINS** | All tasks failing health checks |
| 14:32 | PagerDuty alert fires | On-call engineer paged |
| 14:33 | Engineer acknowledges | Incident created in Slack |
| 14:35 | Initial investigation | Checked logs, found NPE |
| 14:38 | Decision to rollback | Identified bad deployment |
| 14:40 | Rollback initiated | Triggered v0.3.0 deployment |
| 14:45 | New tasks healthy | Health checks passing |
| 14:50 | **OUTAGE RESOLVED** | All systems operational |
| 15:00 | Monitoring period | Confirmed stability |

---

## Root Cause

### What Happened

The deployment of v0.3.1 introduced a bug in the `BookService.getAllBooks()` method. The code assumed that the `author` field on the `Book` entity would never be null, but our database contained 3 legacy books with null authors from a data migration 2 weeks ago.

**Problematic Code** (v0.3.1):
```java
// src/main/java/com/penrose/bibby/library/book/BookService.java:42
public List<BookDTO> getAllBooks() {
    return bookRepository.findAll().stream()
            .map(book -> new BookDTO(
                    book.getIsbn(),
                    book.getTitle(),
                    book.getAuthor().getName(),  // ❌ NPE if author is null
                    book.getPublicationYear()
            ))
            .collect(Collectors.toList());
}
```

**Why It Wasn't Caught:**

1. **Test data didn't include null authors** - All test books had authors
2. **Integration tests passed** - Used test database without legacy data
3. **Staging deployment succeeded** - Staging database didn't have null authors
4. **Code review missed it** - Reviewers didn't consider null case

---

## Contributing Factors

1. **Insufficient test coverage** for edge cases (null values)
2. **Database state divergence** between staging and production
3. **No null-safety enforcement** (no Optional<> usage, no null checks)
4. **Fast-track deployment** - Skipped additional soak time in staging

---

## Resolution

### Immediate Fix (Mitigation)

Rolled back to v0.3.0 at 14:40 EST using existing rollback runbook.

```bash
gh workflow run promote-production.yml -f version=0.3.0-a1b2c3d
```

### Permanent Fix

**PR #127: Fix NPE in getAllBooks() and add null safety**

```java
// Fixed code
public List<BookDTO> getAllBooks() {
    return bookRepository.findAll().stream()
            .map(book -> new BookDTO(
                    book.getIsbn(),
                    book.getTitle(),
                    book.getAuthor() != null ? book.getAuthor().getName() : "Unknown",  // ✅ Null-safe
                    book.getPublicationYear()
            ))
            .collect(Collectors.toList());
}
```

**Additional changes:**
- Added database migration to set default author for legacy books
- Added tests for null author scenario
- Enabled NullAway static analysis tool

---

## Lessons Learned

### What Went Well ✅

1. **Fast detection** - Outage detected within 2 minutes via automated alerts
2. **Clear runbook** - Rollback procedure was well-documented and tested
3. **Quick mitigation** - Rollback completed in 8 minutes
4. **Good communication** - Stakeholders notified within 5 minutes

### What Went Wrong ❌

1. **Test coverage gaps** - Didn't test edge cases with null values
2. **Data inconsistency** - Staging and production databases diverged
3. **No null safety** - Java doesn't enforce null checks
4. **Review process** - Code reviewers didn't spot the null risk

---

## Action Items

| # | Action | Owner | Due Date | Priority | Status |
|---|--------|-------|----------|----------|--------|
| 1 | Add NullAway to Maven build | @engineer1 | 2025-01-20 | P0 | ✅ Done |
| 2 | Write test for null author case | @engineer1 | 2025-01-18 | P0 | ✅ Done |
| 3 | Sync staging DB from prod snapshot weekly | @dba | 2025-01-25 | P1 | In Progress |
| 4 | Add "edge case checklist" to PR template | @devops-lead | 2025-01-24 | P1 | Pending |
| 5 | Increase staging soak time from 1h to 4h | @devops-lead | 2025-01-22 | P2 | Pending |
| 6 | Migrate legacy books to have default author | @engineer2 | 2025-01-23 | P2 | Done |

---

## Supporting Information

### Related Documents
- [Deployment Runbook](../runbooks/DEPLOYMENT.md)
- [Incident Response Runbook](../runbooks/INCIDENT_RESPONSE.md)
- [PR #127 - Fix NPE](https://github.com/yourusername/Bibby/pull/127)

### Metrics
- [CloudWatch Dashboard](https://console.aws.amazon.com/cloudwatch/...)
- [Grafana Incident Timeline](https://grafana.example.com/...)

### Incident Ticket
- [JIRA INC-1234](https://bibby.atlassian.net/browse/INC-1234)

---

## Reviewer Sign-off

| Reviewer | Role | Approved | Date |
|----------|------|----------|------|
| @devops-lead | DevOps Lead | ✅ | 2025-01-18 |
| @engineering-manager | Engineering Manager | ✅ | 2025-01-19 |

---

## Postmortem Retrospective (30 days later)

*To be filled out 30 days after incident to verify action items completed and effective.*

- [ ] All action items completed?
- [ ] Similar incident occurred again?
- [ ] Monitoring improvements effective?
- [ ] Team learned from this incident?
```

### 7.3 Real Example Postmortem for Bibby

**Create `docs/postmortems/2025-01-17-production-outage.md`** using the template above with the NPE incident details.

---

## 8. Knowledge Base Organization

### 8.1 Documentation Structure

**Recommended structure for Bibby:**

```
Bibby/
├── README.md                          # Project front door
├── CONTRIBUTING.md                    # How to contribute
├── LICENSE                            # MIT License
├── docs/
│   ├── ARCHITECTURE.md                # System design
│   ├── DEVELOPMENT.md                 # Local setup guide
│   ├── MONITORING.md                  # Metrics and dashboards
│   │
│   ├── adr/                           # Architecture Decision Records
│   │   ├── 001-why-spring-boot.md
│   │   ├── 002-why-ecs-over-k8s.md
│   │   └── 003-why-postgresql.md
│   │
│   ├── runbooks/                      # Operational procedures
│   │   ├── DEPLOYMENT.md
│   │   ├── INCIDENT_RESPONSE.md
│   │   ├── DATABASE_MAINTENANCE.md
│   │   └── MONITORING.md
│   │
│   ├── postmortems/                   # Incident learnings
│   │   ├── TEMPLATE.md
│   │   └── 2025-01-17-production-outage.md
│   │
│   ├── api/                           # API documentation
│   │   └── openapi.yaml               # Generated from Swagger
│   │
│   └── devops-mentorship/             # Learning content
│       ├── 01-sdlc-fundamentals.md
│       ├── 02-agile-practices.md
│       └── ... (27 total sections)
│
├── terraform/                         # Infrastructure as Code
│   ├── README.md
│   └── modules/
│
└── .github/
    ├── workflows/                     # CI/CD pipelines
    └── PULL_REQUEST_TEMPLATE.md       # PR checklist
```

### 8.2 Documentation in Confluence/Notion

**For team wikis**, organize by audience:

```
Bibby Team Wiki (Confluence)
│
├── 🚀 Getting Started
│   ├── Onboarding Guide
│   ├── Local Development Setup
│   └── Team Roster
│
├── 🏗️ Architecture & Design
│   ├── System Architecture
│   ├── Database Schema
│   ├── Architecture Decision Records (ADRs)
│   └── Technology Stack
│
├── 📘 Development
│   ├── Coding Standards
│   ├── Git Workflow
│   ├── Testing Strategy
│   └── API Documentation
│
├── 🚢 Operations
│   ├── Deployment Runbooks
│   ├── Incident Response
│   ├── Monitoring & Alerts
│   └── On-Call Schedule
│
├── 📊 Metrics & KPIs
│   ├── DORA Metrics Dashboard
│   ├── Application Performance
│   └── Cost Tracking
│
└── 📚 Postmortems
    ├── 2025-01-17: Production Outage
    └── Postmortem Template
```

### 8.3 Keeping Documentation Up-to-Date

**Strategies:**

1. **Documentation as Code**
   - Store in Git alongside code
   - Review in PRs
   - Version control

2. **Documentation Requirements in PR Template**
   ```markdown
   ## Documentation Checklist
   - [ ] Updated README if new feature added
   - [ ] Updated API docs if endpoints changed
   - [ ] Updated runbook if deployment changed
   - [ ] Updated architecture doc if design changed
   ```

3. **Automated Documentation**
   - Swagger/OpenAPI auto-generated from code
   - Architecture diagrams from Terraform (using `terraform graph`)
   - Metrics dashboards from Prometheus queries

4. **Regular Documentation Audits**
   ```bash
   # Quarterly task: Review all docs for accuracy
   # Check:
   # - Do commands still work?
   # - Are screenshots current?
   # - Are links valid?
   ```

5. **Documentation Ownership**
   ```markdown
   # At top of each doc
   **Owner**: @devops-lead
   **Last Updated**: 2025-01-17
   **Review Frequency**: Monthly
   ```

---

## 9. Interview-Ready Knowledge

### Question 1: "How do you ensure documentation stays current in a fast-moving project?"

**Strong Answer:**

"I use a multi-layered approach to keep documentation fresh:

**1. Documentation as Code**
All documentation lives in Git alongside the code. When developers submit a PR that changes behavior, the PR template explicitly requires them to update relevant docs—whether that's the README, API docs, or runbooks. This is enforced during code review.

**2. Automated Documentation**
Where possible, I auto-generate docs. For example, in Bibby I use SpringDoc to generate OpenAPI specs directly from Java annotations. This eliminates drift between code and API docs. Similarly, I use Terraform's `graph` command to visualize infrastructure architecture.

**3. Documentation Reviews in PRs**
Code reviewers check not just code, but docs. If a deployment procedure changes, the deployment runbook must be updated in the same PR. No PR merges with outdated docs.

**4. Ownership and Review Cadence**
Each major document has an owner and a review frequency. For example, the deployment runbook is owned by the DevOps lead and reviewed monthly. We set calendar reminders to audit critical docs quarterly.

**5. Postmortem-Driven Updates**
After incidents, we update runbooks immediately. If a runbook was unclear or missing steps during an incident, that's captured in the postmortem action items and fixed within 48 hours.

In my Bibby project, I implemented this system and our documentation accuracy improved dramatically. During our last simulated incident drill, the on-call engineer successfully followed the runbook with zero clarifying questions—that's the gold standard."

---

### Question 2: "Walk me through how you'd respond to a production outage."

**Strong Answer:**

"I follow a structured incident response process. Let me walk you through a real example from Bibby:

**1. Detect & Acknowledge (0-2 minutes)**
When production went down, PagerDuty paged me immediately. I acknowledged the alert to stop escalation and signaled I was responding. First action: verify the outage by checking the health endpoint—it was timing out, confirming a real outage.

**2. Triage & Communicate (2-5 minutes)**
I created an incident ticket, classified it as P1 (complete outage), and posted in our #incidents Slack channel with a status update. This notifies stakeholders immediately and sets expectations for updates.

**3. Investigate (5-15 minutes)**
I checked recent changes—a deployment had completed 2 minutes before the outage. I tailed the application logs and found a NullPointerException in the BookService. This told me: bad deployment, need rollback.

**4. Mitigate (15-23 minutes)**
I executed our tested rollback runbook—triggered the previous version deployment via GitHub Actions. Monitored the rollback, confirmed new tasks became healthy, verified the health endpoint returned 200.

**5. Verify & Close (23-30 minutes)**
After rollback, I monitored for 10 minutes to ensure stability, then posted resolution in Slack. Total outage: 18 minutes, MTTR: 18 minutes.

**6. Post-Incident**
Within 24 hours, I facilitated a blameless postmortem. We identified root cause (NPE from null authors in DB), created action items (add null-safety tooling, sync staging DB from prod), and updated our deployment checklist.

**Key principles I follow:**
- Communicate early and often
- Follow tested runbooks, don't improvise
- Mitigate first, root cause later
- Learn and improve systems, not blame people

This process reduced our MTTR from 2 hours to 12 minutes."

---

### Question 3: "How do you document architecture decisions for future team members?"

**Strong Answer:**

"I use Architecture Decision Records (ADRs)—a lightweight, structured format for documenting important decisions.

**ADR Structure:**

Each ADR captures:
1. **Context**: What problem are we solving?
2. **Decision**: What did we choose?
3. **Consequences**: What are the trade-offs?
4. **Status**: Accepted, Deprecated, Superseded?

**Real Example from Bibby - ADR-002: Why ECS over Kubernetes?**

```markdown
## Context
We need container orchestration but Kubernetes has high complexity for a single application.

## Decision
Use AWS ECS Fargate.

## Consequences
✅ Simpler than K8s
✅ Tight AWS integration
✅ No server management
✅ Cost-effective at our scale
❌ AWS vendor lock-in
❌ Less portable than K8s

## Status
Accepted (2025-01-10)
```

**Why ADRs Are Powerful:**

1. **Prevents Re-litigation**
   When a new engineer suggests "Why don't we use Kubernetes?", we can point to ADR-002 which explains our rationale. No need to re-debate.

2. **Knowledge Transfer**
   New team members read ADRs during onboarding. They understand not just *what* we built, but *why* we made those choices.

3. **Evolution Over Time**
   If we outgrow ECS and migrate to K8s, we create a new ADR superseding ADR-002. This creates a decision history.

4. **Lightweight Process**
   ADRs are Markdown files in `docs/adr/`, reviewed in PRs. No heavy process overhead.

**Best Practices I Follow:**

- **One ADR per major decision** (not for every tiny choice)
- **Written when decision is made** (not retroactively)
- **Immutable** (never edit old ADRs; create new ones to supersede)
- **Linked from ARCHITECTURE.md** for discoverability

In my Bibby project, I created 3 ADRs covering Spring Boot, ECS, and PostgreSQL choices. These have been incredibly valuable for explaining the architecture to others and for my own portfolio presentations."

---

## 10. Summary

**Congratulations!** You've learned to create professional documentation and operational runbooks.

### What You've Accomplished

**1. Documentation Hierarchy**
- Onboarding (README, ARCHITECTURE) for new team members
- Development (API docs, ADRs) for daily work
- Operations (runbooks, incident response) for production support

**2. Professional README.md**
- Badges (build status, coverage, security)
- Clear features, quick start, architecture
- Links to detailed documentation
- DORA metrics showcase

**3. Architecture Documentation**
- Complete ARCHITECTURE.md with diagrams
- Component details (ECS, RDS, ALB, VPC)
- ADRs for technology decisions
- Scalability, security, disaster recovery plans
- Cost breakdown

**4. Operational Runbooks**
- Deployment runbook (standard, emergency, rollback)
- Database migration safety patterns
- Common issues and troubleshooting
- Copy-pasteable commands

**5. Incident Response**
- Structured response process (Detect → Triage → Communicate → Investigate → Mitigate → Resolve)
- Severity levels (P1-P4) with response times
- On-call procedures and escalation paths
- Communication templates
- Useful diagnostic commands

**6. API Documentation**
- SpringDoc/OpenAPI integration
- Swagger UI for interactive docs
- Annotated REST controllers
- Auto-generated API specs

**7. Postmortem Culture**
- Blameless postmortems
- Structured template (summary, timeline, root cause, lessons, action items)
- Real Bibby example (NPE incident)
- 30-day retrospective

**8. Knowledge Base Organization**
- Documentation structure in Git
- Team wiki organization (Confluence/Notion)
- Keeping docs current (documentation as code, PR requirements, automation)

### Key Takeaways

1. **Documentation is a Force Multiplier**
   - Enables team members to work independently
   - Reduces repetitive questions
   - Speeds up onboarding (days → hours)
   - Makes you look senior (thorough, thinks about knowledge transfer)

2. **Runbooks Save Time During Crises**
   - MTTR improvement: 2 hours → 12 minutes
   - No thinking required—just follow steps
   - Tested procedures, not improvisation

3. **Blameless Postmortems Build Trust**
   - Focus on systems, not people
   - Turn failures into learning opportunities
   - Action items prevent recurrence

4. **Documentation as Code Works**
   - Version controlled alongside code
   - Reviewed in PRs
   - Never out of sync

### Impact on Bibby

**Before Documentation:**
- ❌ Only you could deploy
- ❌ No incident response plan
- ❌ New team members take weeks to onboard
- ❌ Repeated mistakes, no learning

**After Documentation:**
- ✅ Anyone can deploy following runbook
- ✅ Structured incident response (18-minute MTTR)
- ✅ New team members productive in days
- ✅ Postmortems drive continuous improvement

### Your Portfolio Impact

**Recruiters and hiring managers love seeing:**
- Professional README with metrics
- Complete architecture documentation
- Real incident postmortems (shows you've operated production systems)
- API documentation (shows you think about developer experience)

**This section alone demonstrates:**
- Senior-level thinking (knowledge transfer, operational excellence)
- Production experience (incident response, runbooks)
- Communication skills (clear documentation)
- Team player (enables others, blameless culture)

### Next Steps

**Section 24: Portfolio Showcase** (Next)
Learn how to present Bibby in job applications, optimize your GitHub profile, and create compelling project demonstrations.

**Remaining Sections:**
- Section 25: Interview Preparation
- Section 26: Continuous Learning Path
- Section 27: 90-Day Implementation Plan

---

**You're now equipped to create professional documentation that impresses employers and enables teams to operate independently. Keep this skill sharp—great documentation is rare and highly valued.**
