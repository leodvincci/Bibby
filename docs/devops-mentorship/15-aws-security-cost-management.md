# Section 15: AWS Security & Cost Management

## Introduction: The Two Pillars of Production AWS

You've built a scalable, automated infrastructure for Bibby. But two critical concerns remain:

1. **Security:** Is your infrastructure secure from threats?
2. **Cost:** Will you get a surprise $10,000 AWS bill?

These aren't optional considerations—they're **mandatory** for production systems. A single security breach or cost overrun can sink a project or company.

**What You'll Learn:**
- AWS security best practices (IAM, encryption, network security)
- Shared Responsibility Model in depth
- Cost optimization strategies and tools
- AWS Cost Explorer, Budgets, and Trusted Advisor
- Security compliance and audit logging
- Real-world cost optimization for Bibby

**Real-World Context:**
- **Security incident:** Company exposes S3 bucket publicly, leaks 100M customer records
- **Cost disaster:** Developer leaves EC2 instances running, $50K bill in one month
- **Prevention:** Proper security controls and cost monitoring prevent both

By the end of this section, Bibby will be secure, cost-optimized, and ready for production deployment.

---

## 1. AWS Shared Responsibility Model

### 1.1 Understanding Shared Responsibility

**AWS is responsible for "Security OF the Cloud":**
- Physical security of data centers
- Hardware infrastructure
- Network infrastructure
- Hypervisor and virtualization layer

**You are responsible for "Security IN the Cloud":**
- Data encryption
- IAM (users, roles, policies)
- Application security
- Network configuration (security groups, NACLs)
- Operating system patches
- Firewall configuration

### 1.2 Responsibility by Service Type

**IaaS (EC2):**
- **AWS:** Hardware, network, hypervisor
- **You:** OS, application, data, firewall, encryption

**PaaS (RDS):**
- **AWS:** OS, database software, patches, backups
- **You:** Database configuration, access control, encryption, data

**SaaS (S3):**
- **AWS:** Infrastructure, durability, availability
- **You:** Access policies, encryption, data classification

**For Bibby:**
```
EC2 (IaaS):
  ✓ We patch OS (Amazon Linux 2)
  ✓ We configure security groups
  ✓ We manage application security
  ✓ We encrypt data in transit (SSL/TLS)

RDS (PaaS):
  ✓ AWS patches PostgreSQL
  ✓ We configure encryption at rest
  ✓ We manage database users and permissions
  ✓ We configure backup retention

S3 (SaaS):
  ✓ AWS handles durability (11 nines)
  ✓ We configure bucket policies
  ✓ We enable versioning and encryption
  ✓ We manage lifecycle policies
```

---

## 2. Identity and Access Management (IAM) Security

### 2.1 IAM Best Practices Deep Dive

**1. Root Account Protection:**

```bash
# NEVER use root account for daily operations
# Root account checklist:
✓ MFA enabled
✓ No access keys created
✓ Strong password (20+ characters)
✓ Used only for:
  - Account closure
  - Billing/payment changes
  - Support plan changes
  - IAM user recovery (emergency)

# Check root account access keys
aws iam get-account-summary | grep AccountAccessKeysPresent
# Should be 0
```

**2. Least Privilege Principle:**

```json
// ❌ BAD: Overly permissive policy
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": "*",
    "Resource": "*"
  }]
}

// ✅ GOOD: Specific permissions for Bibby deployment
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "BibbyEC2Permissions",
      "Effect": "Allow",
      "Action": [
        "ec2:RunInstances",
        "ec2:TerminateInstances",
        "ec2:DescribeInstances",
        "ec2:DescribeImages",
        "ec2:DescribeSecurityGroups"
      ],
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "aws:RequestedRegion": "us-east-1"
        }
      }
    },
    {
      "Sid": "BibbyS3Permissions",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::bibby-backups-prod",
        "arn:aws:s3:::bibby-backups-prod/*"
      ]
    },
    {
      "Sid": "BibbyRDSReadOnly",
      "Effect": "Allow",
      "Action": [
        "rds:DescribeDBInstances",
        "rds:ListTagsForResource"
      ],
      "Resource": "arn:aws:rds:us-east-1:123456789012:db:bibby-db-prod"
    }
  ]
}
```

**3. Use IAM Roles, Not Access Keys:**

```hcl
# Terraform: EC2 instance with IAM role (Section 14)
resource "aws_iam_role" "bibby_ec2" {
  name = "bibby-ec2-role-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}

# Instance uses role, no hardcoded keys!
resource "aws_instance" "bibby_app" {
  iam_instance_profile = aws_iam_instance_profile.bibby.name
  # ...
}
```

**4. Enable MFA for All Users:**

```bash
# Check MFA status for all users
aws iam get-credential-report

# Force MFA with IAM policy
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "DenyAllExceptListedIfNoMFA",
    "Effect": "Deny",
    "NotAction": [
      "iam:CreateVirtualMFADevice",
      "iam:EnableMFADevice",
      "iam:GetUser",
      "iam:ListMFADevices",
      "iam:ListVirtualMFADevices",
      "iam:ResyncMFADevice",
      "sts:GetSessionToken"
    ],
    "Resource": "*",
    "Condition": {
      "BoolIfExists": {
        "aws:MultiFactorAuthPresent": "false"
      }
    }
  }]
}
```

**5. Rotate Access Keys Regularly:**

```bash
# List access keys older than 90 days
aws iam list-access-keys --user-name bibby-deploy-user

# Create new key
aws iam create-access-key --user-name bibby-deploy-user

# Update applications with new key
# After verification, delete old key
aws iam delete-access-key --user-name bibby-deploy-user --access-key-id AKIAOLD123

# Automate rotation with Lambda (advanced)
```

### 2.2 IAM Policies for Bibby

**Development Team Policy:**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "BibbyDevelopers",
      "Effect": "Allow",
      "Action": [
        "ec2:Describe*",
        "rds:Describe*",
        "s3:ListBucket",
        "s3:GetObject",
        "logs:GetLogEvents",
        "logs:FilterLogEvents",
        "cloudwatch:GetMetricStatistics",
        "cloudwatch:ListMetrics"
      ],
      "Resource": "*"
    },
    {
      "Sid": "DenyProductionModifications",
      "Effect": "Deny",
      "Action": [
        "ec2:TerminateInstances",
        "rds:DeleteDBInstance",
        "s3:DeleteBucket"
      ],
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "ec2:ResourceTag/Environment": "prod"
        }
      }
    }
  ]
}
```

**CI/CD Pipeline Policy:**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "BibbyCI CDPermissions",
      "Effect": "Allow",
      "Action": [
        "ec2:RunInstances",
        "ec2:TerminateInstances",
        "ec2:CreateTags",
        "autoscaling:UpdateAutoScalingGroup",
        "elasticloadbalancing:RegisterTargets",
        "elasticloadbalancing:DeregisterTargets",
        "s3:PutObject",
        "s3:GetObject",
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:PutImage"
      ],
      "Resource": "*"
    }
  ]
}
```

### 2.3 IAM Access Analyzer

**Detect overly permissive policies:**

```bash
# Create IAM Access Analyzer
aws accessanalyzer create-analyzer \
  --analyzer-name bibby-analyzer \
  --type ACCOUNT

# List findings
aws accessanalyzer list-findings \
  --analyzer-arn arn:aws:access-analyzer:us-east-1:123456789012:analyzer/bibby-analyzer

# Example finding:
{
  "id": "12345",
  "resourceType": "AWS::S3::Bucket",
  "resource": "arn:aws:s3:::bibby-backups-prod",
  "condition": {},
  "action": ["s3:GetObject"],
  "principal": {"AWS": "*"},
  "isPublic": true,
  "status": "ACTIVE"
}

# Fix: Update bucket policy to restrict access
```

---

## 3. Data Encryption

### 3.1 Encryption at Rest

**For Bibby:**

**RDS Encryption:**

```hcl
# Already configured in Section 14
resource "aws_db_instance" "bibby" {
  storage_encrypted = true  # ✓ Encrypted with AWS KMS
  kms_key_id        = aws_kms_key.bibby_db.arn  # Optional: custom key
  # ...
}

# Custom KMS key for RDS
resource "aws_kms_key" "bibby_db" {
  description             = "KMS key for Bibby RDS encryption"
  deletion_window_in_days = 10
  enable_key_rotation     = true

  tags = {
    Name = "bibby-db-key-${var.environment}"
  }
}

resource "aws_kms_alias" "bibby_db" {
  name          = "alias/bibby-db-${var.environment}"
  target_key_id = aws_kms_key.bibby_db.key_id
}
```

**S3 Encryption:**

```hcl
# Server-side encryption (already configured)
resource "aws_s3_bucket_server_side_encryption_configuration" "backups" {
  bucket = aws_s3_bucket.backups.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.bibby_s3.arn
    }
    bucket_key_enabled = true  # Reduces KMS costs
  }
}

# Custom KMS key for S3
resource "aws_kms_key" "bibby_s3" {
  description             = "KMS key for Bibby S3 encryption"
  deletion_window_in_days = 10
  enable_key_rotation     = true
}
```

**EBS Encryption (EC2 volumes):**

```hcl
# Launch template with encrypted EBS
resource "aws_launch_template" "bibby" {
  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_size           = 20
      volume_type           = "gp3"
      delete_on_termination = true
      encrypted             = true
      kms_key_id            = aws_kms_key.bibby_ebs.arn
    }
  }
}
```

### 3.2 Encryption in Transit

**HTTPS/TLS for ALB:**

```hcl
# Request ACM certificate
resource "aws_acm_certificate" "bibby" {
  domain_name       = "bibby.example.com"
  validation_method = "DNS"

  subject_alternative_names = [
    "www.bibby.example.com",
    "api.bibby.example.com"
  ]

  lifecycle {
    create_before_destroy = true
  }
}

# HTTPS listener on ALB
resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.bibby.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = aws_acm_certificate.bibby.arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.bibby.arn
  }
}

# Redirect HTTP to HTTPS
resource "aws_lb_listener" "http_redirect" {
  load_balancer_arn = aws_lb.bibby.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}
```

**RDS SSL/TLS:**

```properties
# application-prod.properties
spring.datasource.url=jdbc:postgresql://bibby-db.abc123.us-east-1.rds.amazonaws.com:5432/bibby?sslmode=require
spring.datasource.hikari.connection-init-sql=SET ssl_mode='require'
```

---

## 4. Network Security

### 4.1 Security Groups Best Practices

**Principle: Deny by default, allow specific.**

**Bibby Security Groups (refined):**

```hcl
# Application security group (restrictive)
resource "aws_security_group" "app" {
  name        = "bibby-app-sg-${var.environment}"
  description = "Security group for Bibby application"
  vpc_id      = aws_vpc.bibby.id

  # SSH only from bastion or VPN
  ingress {
    from_port       = 22
    to_port         = 22
    protocol        = "tcp"
    security_groups = [aws_security_group.bastion.id]  # NOT 0.0.0.0/0
    description     = "SSH from bastion only"
  }

  # Application port only from ALB
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
    description     = "HTTP from ALB only"
  }

  # Outbound: Allow HTTPS to internet (for API calls, updates)
  egress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS to internet"
  }

  # Outbound: PostgreSQL to RDS only
  egress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.db.id]
    description     = "PostgreSQL to database"
  }

  tags = {
    Name = "bibby-app-sg-${var.environment}"
  }
}

# Database security group (most restrictive)
resource "aws_security_group" "db" {
  name        = "bibby-db-sg-${var.environment}"
  description = "Security group for Bibby database"
  vpc_id      = aws_vpc.bibby.id

  # PostgreSQL ONLY from application
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
    description     = "PostgreSQL from app only"
  }

  # NO outbound (database shouldn't call internet)
  # Remove default egress rule
  revoke_rules_on_delete = true

  tags = {
    Name = "bibby-db-sg-${var.environment}"
  }
}
```

### 4.2 Network ACLs (Optional Additional Layer)

```hcl
# Network ACL for database subnet (defense in depth)
resource "aws_network_acl" "db" {
  vpc_id     = aws_vpc.bibby.id
  subnet_ids = aws_subnet.private[*].id

  # Allow PostgreSQL from app subnet
  ingress {
    protocol   = "tcp"
    rule_no    = 100
    action     = "allow"
    cidr_block = aws_subnet.public[0].cidr_block
    from_port  = 5432
    to_port    = 5432
  }

  # Allow ephemeral ports for return traffic
  ingress {
    protocol   = "tcp"
    rule_no    = 110
    action     = "allow"
    cidr_block = aws_subnet.public[0].cidr_block
    from_port  = 1024
    to_port    = 65535
  }

  # Deny all other inbound
  ingress {
    protocol   = "-1"
    rule_no    = 200
    action     = "deny"
    cidr_block = "0.0.0.0/0"
    from_port  = 0
    to_port    = 0
  }

  tags = {
    Name = "bibby-db-nacl-${var.environment}"
  }
}
```

### 4.3 VPC Flow Logs

**Monitor network traffic:**

```hcl
# CloudWatch log group for VPC flow logs
resource "aws_cloudwatch_log_group" "vpc_flow_logs" {
  name              = "/aws/vpc/bibby-${var.environment}"
  retention_in_days = 7
}

# IAM role for VPC flow logs
resource "aws_iam_role" "vpc_flow_logs" {
  name = "bibby-vpc-flow-logs-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "vpc-flow-logs.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy" "vpc_flow_logs" {
  name = "bibby-vpc-flow-logs-policy"
  role = aws_iam_role.vpc_flow_logs.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:DescribeLogGroups",
        "logs:DescribeLogStreams"
      ]
      Effect   = "Allow"
      Resource = "*"
    }]
  })
}

# VPC Flow Logs
resource "aws_flow_log" "bibby" {
  iam_role_arn    = aws_iam_role.vpc_flow_logs.arn
  log_destination = aws_cloudwatch_log_group.vpc_flow_logs.arn
  traffic_type    = "ALL"
  vpc_id          = aws_vpc.bibby.id

  tags = {
    Name = "bibby-vpc-flow-logs-${var.environment}"
  }
}
```

**Analyze flow logs:**

```bash
# Query flow logs with CloudWatch Insights
aws logs start-query \
  --log-group-name /aws/vpc/bibby-prod \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --query-string 'fields @timestamp, srcAddr, dstAddr, srcPort, dstPort, action
| filter action = "REJECT"
| stats count() by srcAddr, dstAddr
| sort count desc
| limit 20'

# Output: Top 20 rejected connections (potential attacks)
```

---

## 5. AWS Secrets Manager

### 5.1 Storing Sensitive Data

**Never hardcode credentials!**

```hcl
# Store database password in Secrets Manager
resource "aws_secretsmanager_secret" "db_password" {
  name        = "bibby/db/password/${var.environment}"
  description = "Bibby database password for ${var.environment}"

  recovery_window_in_days = 7  # Can recover accidentally deleted secrets
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = var.db_password  # From terraform.tfvars (gitignored)
}

# Grant EC2 instances access via IAM role (already configured in Section 14)
```

**Retrieve secrets in application:**

```bash
# In user-data.sh (Section 14, already shown)
DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id bibby/db/password/prod \
  --region us-east-1 \
  --query SecretString \
  --output text)
```

**Java application (Spring Boot):**

```java
// Add dependency: AWS Secrets Manager SDK
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>secretsmanager</artifactId>
</dependency>

// Configuration class
@Configuration
public class SecretsManagerConfig {

    @Bean
    public DataSource dataSource() {
        String secretName = "bibby/db/password/prod";
        String region = "us-east-1";

        SecretsManagerClient client = SecretsManagerClient.builder()
            .region(Region.of(region))
            .build();

        GetSecretValueRequest request = GetSecretValueRequest.builder()
            .secretId(secretName)
            .build();

        GetSecretValueResponse response = client.getSecretValue(request);
        String password = response.secretString();

        // Configure DataSource with retrieved password
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("DATABASE_URL"));
        config.setUsername(System.getenv("DATABASE_USERNAME"));
        config.setPassword(password);

        return new HikariDataSource(config);
    }
}
```

### 5.2 Automatic Secret Rotation

```hcl
# Lambda function for rotating RDS password
resource "aws_secretsmanager_secret_rotation" "db_password" {
  secret_id           = aws_secretsmanager_secret.db_password.id
  rotation_lambda_arn = aws_lambda_function.rotate_secret.arn

  rotation_rules {
    automatically_after_days = 30  # Rotate every 30 days
  }
}

# Lambda function (simplified - full implementation would be larger)
resource "aws_lambda_function" "rotate_secret" {
  filename      = "rotate_secret.zip"
  function_name = "bibby-rotate-db-password-${var.environment}"
  role          = aws_iam_role.lambda_rotate.arn
  handler       = "index.handler"
  runtime       = "python3.11"
  timeout       = 30

  environment {
    variables = {
      DB_INSTANCE_ID = aws_db_instance.bibby.id
    }
  }
}
```

---

## 6. Compliance and Audit Logging

### 6.1 AWS CloudTrail

**Log all API calls:**

```hcl
# S3 bucket for CloudTrail logs
resource "aws_s3_bucket" "cloudtrail" {
  bucket = "bibby-cloudtrail-${var.environment}-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket_public_access_block" "cloudtrail" {
  bucket = aws_s3_bucket.cloudtrail.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# CloudTrail
resource "aws_cloudtrail" "bibby" {
  name                          = "bibby-trail-${var.environment}"
  s3_bucket_name                = aws_s3_bucket.cloudtrail.id
  include_global_service_events = true
  is_multi_region_trail         = true
  enable_log_file_validation    = true

  event_selector {
    read_write_type           = "All"
    include_management_events = true

    data_resource {
      type   = "AWS::S3::Object"
      values = ["${aws_s3_bucket.backups.arn}/"]
    }
  }

  tags = {
    Name = "bibby-cloudtrail-${var.environment}"
  }
}
```

**Query CloudTrail logs:**

```bash
# Who terminated an EC2 instance?
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=EventName,AttributeValue=TerminateInstances \
  --max-results 10

# Who modified security group?
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=EventName,AttributeValue=AuthorizeSecurityGroupIngress \
  --start-time "2025-01-15T00:00:00Z"
```

### 6.2 AWS Config

**Track resource configuration changes:**

```hcl
# S3 bucket for Config
resource "aws_s3_bucket" "config" {
  bucket = "bibby-config-${var.environment}-${data.aws_caller_identity.current.account_id}"
}

# IAM role for Config
resource "aws_iam_role" "config" {
  name = "bibby-config-role-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "config.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "config" {
  role       = aws_iam_role.config.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/ConfigRole"
}

# Config Recorder
resource "aws_config_configuration_recorder" "bibby" {
  name     = "bibby-config-recorder-${var.environment}"
  role_arn = aws_iam_role.config.arn

  recording_group {
    all_supported                 = true
    include_global_resource_types = true
  }
}

# Config Delivery Channel
resource "aws_config_delivery_channel" "bibby" {
  name           = "bibby-config-delivery-${var.environment}"
  s3_bucket_name = aws_s3_bucket.config.bucket

  depends_on = [aws_config_configuration_recorder.bibby]
}

# Config Rules
resource "aws_config_config_rule" "s3_bucket_public_read_prohibited" {
  name = "s3-bucket-public-read-prohibited"

  source {
    owner             = "AWS"
    source_identifier = "S3_BUCKET_PUBLIC_READ_PROHIBITED"
  }

  depends_on = [aws_config_configuration_recorder.bibby]
}

resource "aws_config_config_rule" "rds_encryption_enabled" {
  name = "rds-storage-encrypted"

  source {
    owner             = "AWS"
    source_identifier = "RDS_STORAGE_ENCRYPTED"
  }

  depends_on = [aws_config_configuration_recorder.bibby]
}
```

---

## 7. Cost Management

### 7.1 Cost Allocation Tags

**Tag all resources consistently:**

```hcl
# Default tags (Section 14, enhanced)
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "Bibby"
      ManagedBy   = "Terraform"
      Environment = var.environment
      CostCenter  = "Engineering"
      Owner       = "devops-team@example.com"
      Application = "LibraryManagement"
    }
  }
}
```

**Activate cost allocation tags:**

```bash
# Enable tags for cost tracking
aws ce update-cost-allocation-tags-status \
  --cost-allocation-tags-status TagKey=Project,Status=Active \
  --cost-allocation-tags-status TagKey=Environment,Status=Active \
  --cost-allocation-tags-status TagKey=CostCenter,Status=Active
```

### 7.2 AWS Cost Explorer

**Analyze costs programmatically:**

```bash
# Get cost by service (last month)
aws ce get-cost-and-usage \
  --time-period Start=$(date -d '1 month ago' +%Y-%m-01),End=$(date +%Y-%m-01) \
  --granularity MONTHLY \
  --metrics "UnblendedCost" \
  --group-by Type=DIMENSION,Key=SERVICE

# Output (example):
{
  "ResultsByTime": [{
    "TimePeriod": { "Start": "2025-01-01", "End": "2025-02-01" },
    "Groups": [
      { "Keys": ["Amazon Elastic Compute Cloud"], "Metrics": { "UnblendedCost": { "Amount": "15.42", "Unit": "USD" }}},
      { "Keys": ["Amazon Relational Database Service"], "Metrics": { "UnblendedCost": { "Amount": "18.67", "Unit": "USD" }}},
      { "Keys": ["Amazon Simple Storage Service"], "Metrics": { "UnblendedCost": { "Amount": "2.14", "Unit": "USD" }}}
    ]
  }]
}

# Get cost by tag (Environment)
aws ce get-cost-and-usage \
  --time-period Start=2025-01-01,End=2025-02-01 \
  --granularity MONTHLY \
  --metrics "UnblendedCost" \
  --group-by Type=TAG,Key=Environment

# Compare dev vs prod costs
```

### 7.3 AWS Budgets

**Set up cost alerts:**

```hcl
# SNS topic for budget alerts
resource "aws_sns_topic" "budget_alerts" {
  name = "bibby-budget-alerts-${var.environment}"
}

resource "aws_sns_topic_subscription" "budget_alerts_email" {
  topic_arn = aws_sns_topic.budget_alerts.arn
  protocol  = "email"
  endpoint  = var.budget_alert_email
}

# Monthly budget
resource "aws_budgets_budget" "bibby_monthly" {
  name              = "bibby-monthly-budget-${var.environment}"
  budget_type       = "COST"
  limit_amount      = var.monthly_budget_limit
  limit_unit        = "USD"
  time_period_start = "2025-01-01_00:00"
  time_unit         = "MONTHLY"

  cost_filter {
    name = "TagKeyValue"
    values = [
      "Project$Bibby",
      "Environment$${var.environment}"
    ]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.budget_alert_email]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.budget_alert_email]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 90
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.budget_alert_email]
  }
}
```

**Variables:**

```hcl
variable "monthly_budget_limit" {
  description = "Monthly budget limit in USD"
  type        = number
  default     = 100  # Adjust based on expected usage
}

variable "budget_alert_email" {
  description = "Email for budget alerts"
  type        = string
}
```

### 7.4 Cost Optimization Strategies for Bibby

**1. Right-Size EC2 Instances:**

```bash
# Use AWS Compute Optimizer
aws compute-optimizer get-ec2-instance-recommendations \
  --instance-arns $(aws ec2 describe-instances \
    --filters "Name=tag:Project,Values=Bibby" \
    --query 'Reservations[].Instances[].InstanceId' \
    --output text)

# Example recommendation:
# Current: t2.micro (1 vCPU, 1 GB)
# Recommended: t3a.nano (2 vCPU, 0.5 GB) - Save 15%
# Or: t3.micro (2 vCPU, 1 GB) - Better performance, same cost
```

**2. Use Spot Instances for Non-Production:**

```hcl
# Dev environment with Spot Instances
resource "aws_launch_template" "bibby_dev_spot" {
  count = var.environment == "dev" ? 1 : 0

  name_prefix   = "bibby-spot-lt-dev-"
  image_id      = data.aws_ami.amazon_linux_2.id
  instance_type = "t3.micro"

  instance_market_options {
    market_type = "spot"

    spot_options {
      max_price = "0.01"  # Set max price (t3.micro on-demand: ~$0.0104/hr)
      spot_instance_type = "one-time"
    }
  }

  # Cost savings: ~70% off on-demand price
}
```

**3. Schedule Non-Production Resources:**

```bash
# Lambda function to stop EC2 instances at night (dev/staging)
# Stop at 8 PM, start at 8 AM weekdays
# Savings: 12 hours/day * 5 days = 60 hours/week = ~35% cost reduction

# EventBridge rules
aws events put-rule \
  --name bibby-stop-dev-instances \
  --schedule-expression "cron(0 20 ? * MON-FRI *)" \
  --state ENABLED

aws events put-targets \
  --rule bibby-stop-dev-instances \
  --targets "Id"="1","Arn"="arn:aws:lambda:us-east-1:123456789012:function:StopDevInstances"
```

**4. Use S3 Intelligent-Tiering:**

```hcl
# Already configured in Section 14, but let's enhance it
resource "aws_s3_bucket_lifecycle_configuration" "backups" {
  bucket = aws_s3_bucket.backups.id

  rule {
    id     = "intelligent-tiering"
    status = "Enabled"

    transition {
      days          = 0
      storage_class = "INTELLIGENT_TIERING"
    }

    # Auto-archive to Glacier after 90 days
    transition {
      days          = 90
      storage_class = "GLACIER_IR"  # Instant retrieval
    }

    # Deep archive after 180 days
    transition {
      days          = 180
      storage_class = "DEEP_ARCHIVE"
    }

    expiration {
      days = 365  # Delete after 1 year
    }
  }
}

# Cost comparison for 100 GB:
# Standard: $2.30/month
# Intelligent-Tiering: $1.28/month (if infrequently accessed)
# Glacier Instant: $0.40/month
# Deep Archive: $0.10/month
```

**5. Delete Unused Resources:**

```bash
# Find unattached EBS volumes
aws ec2 describe-volumes \
  --filters Name=status,Values=available \
  --query 'Volumes[*].[VolumeId,Size,CreateTime]' \
  --output table

# Delete unused volumes
aws ec2 delete-volume --volume-id vol-abc123

# Find old snapshots (> 90 days)
aws ec2 describe-snapshots --owner-ids self \
  --query "Snapshots[?StartTime<='$(date -u -d '90 days ago' +%Y-%m-%d)'].[SnapshotId,StartTime,VolumeSize]" \
  --output table

# Delete old snapshots
aws ec2 delete-snapshot --snapshot-id snap-abc123
```

### 7.5 Reserved Instances & Savings Plans

**For Production (long-term commitment):**

```bash
# If Bibby runs 24/7 in production for 1+ years:

# Option 1: Reserved Instances (1-year, no upfront)
# t3.micro Reserved Instance: $0.0062/hr (vs $0.0104/hr on-demand)
# Savings: 40%

# Option 2: Compute Savings Plan
# Commit to $10/month, get ~20% discount on all compute
# More flexible than RIs

# Purchase RI via AWS Console:
# EC2 → Reserved Instances → Purchase Reserved Instances
# OR use AWS CLI:
aws ec2 purchase-reserved-instances-offering \
  --reserved-instances-offering-id abc123 \
  --instance-count 2
```

### 7.6 AWS Trusted Advisor

**Free tier checks:**

```bash
# Get cost optimization recommendations
aws support describe-trusted-advisor-checks \
  --language en

# Check for idle resources
aws support describe-trusted-advisor-check-result \
  --check-id Qch7DwouX1  # Low Utilization EC2 Instances

# Check for unassociated Elastic IPs
aws support describe-trusted-advisor-check-result \
  --check-id Z4AUBRNSmz  # Unassociated Elastic IP Addresses
```

---

## 8. Security Incident Response

### 8.1 Incident Response Plan

**1. Detection:**
- CloudWatch Alarms trigger
- AWS GuardDuty findings
- Unusual Cost spike
- User reports unauthorized access

**2. Containment:**

```bash
# Immediately revoke compromised IAM credentials
aws iam delete-access-key --user-name compromised-user --access-key-id AKIACOMPROMISED

# Isolate compromised EC2 instance (change security group)
aws ec2 modify-instance-attribute \
  --instance-id i-compromised123 \
  --groups sg-isolated

# Take snapshot before terminating
aws ec2 create-snapshot \
  --volume-id vol-abc123 \
  --description "Forensics snapshot - incident-2025-01-15"
```

**3. Investigation:**

```bash
# Review CloudTrail logs
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=Username,AttributeValue=compromised-user \
  --start-time "2025-01-15T00:00:00Z"

# Check VPC Flow Logs for suspicious traffic
# Analyze application logs in CloudWatch
```

**4. Eradication:**
- Rotate all credentials
- Patch vulnerabilities
- Update security groups

**5. Recovery:**
- Deploy clean instances
- Restore from backup if needed
- Monitor closely

**6. Post-Incident:**
- Document timeline
- Update security policies
- Train team on lessons learned

### 8.2 AWS GuardDuty

**Enable threat detection:**

```hcl
# Enable GuardDuty
resource "aws_guardduty_detector" "bibby" {
  enable = true

  datasources {
    s3_logs {
      enable = true
    }
    kubernetes {
      audit_logs {
        enable = false  # Not using EKS for Bibby
      }
    }
  }

  finding_publishing_frequency = "FIFTEEN_MINUTES"
}

# SNS topic for GuardDuty findings
resource "aws_sns_topic" "guardduty_alerts" {
  name = "bibby-guardduty-alerts-${var.environment}"
}

# EventBridge rule for high-severity findings
resource "aws_cloudwatch_event_rule" "guardduty_findings" {
  name        = "bibby-guardduty-high-severity"
  description = "Alert on high severity GuardDuty findings"

  event_pattern = jsonencode({
    source      = ["aws.guardduty"]
    detail-type = ["GuardDuty Finding"]
    detail = {
      severity = [7, 8, 8.9]  # High severity
    }
  })
}

resource "aws_cloudwatch_event_target" "guardduty_sns" {
  rule      = aws_cloudwatch_event_rule.guardduty_findings.name
  target_id = "SendToSNS"
  arn       = aws_sns_topic.guardduty_alerts.arn
}
```

---

## 9. Summary & Security Checklist

### Production Security Checklist

**✅ IAM:**
- [ ] Root account has MFA
- [ ] No root access keys
- [ ] IAM users have MFA
- [ ] IAM policies follow least privilege
- [ ] Access keys rotated quarterly
- [ ] IAM Access Analyzer enabled
- [ ] Service roles used (no embedded keys)

**✅ Encryption:**
- [ ] RDS encryption at rest (KMS)
- [ ] S3 encryption at rest (KMS)
- [ ] EBS encryption enabled
- [ ] HTTPS/TLS on ALB with ACM cert
- [ ] Database connections use SSL
- [ ] KMS key rotation enabled

**✅ Network:**
- [ ] Security groups deny by default
- [ ] No 0.0.0.0/0 on sensitive ports
- [ ] RDS in private subnet
- [ ] VPC Flow Logs enabled
- [ ] Network ACLs configured (optional)

**✅ Secrets:**
- [ ] No hardcoded credentials
- [ ] Secrets Manager for sensitive data
- [ ] Automatic secret rotation configured
- [ ] Application retrieves secrets at runtime

**✅ Monitoring:**
- [ ] CloudTrail enabled (all regions)
- [ ] AWS Config rules active
- [ ] GuardDuty threat detection enabled
- [ ] CloudWatch alarms for security events
- [ ] Log retention configured (7+ days)

**✅ Cost:**
- [ ] Cost allocation tags enabled
- [ ] AWS Budgets configured with alerts
- [ ] Monthly cost review scheduled
- [ ] Unused resources deleted
- [ ] Right-sized instances
- [ ] Production uses Reserved Instances/Savings Plans

### Monthly Budget Estimate for Bibby

**Production (optimized):**
```
EC2 t3.micro (2 instances, Reserved): $9.00/month
ALB: $16.20/month
RDS db.t3.micro (Multi-AZ, Reserved): $25.00/month
S3 (100 GB with lifecycle): $2.00/month
Data Transfer (50 GB): $4.50/month
CloudWatch Logs (5 GB): $2.50/month
KMS (3 keys, 1000 requests): $3.00/month
Secrets Manager (3 secrets): $1.20/month
-------------------------------------------
Total: ~$63/month

With Savings Plans: ~$50/month
Free Tier (first 12 months): ~$30/month
```

**Development (spot instances, no Multi-AZ):**
```
EC2 t3.micro Spot (1 instance): $2.50/month
RDS db.t3.micro (single-AZ): $11.00/month
S3 (10 GB): $0.25/month
-------------------------------------------
Total: ~$14/month
```

### Interview-Ready Knowledge

**Question: "How do you secure an AWS environment?"**

**Answer:** "I follow defense-in-depth with multiple security layers. For IAM, I implement least privilege—users and roles only get permissions they need. Root account is MFA-protected and never used. I use IAM roles for EC2 instances instead of embedding access keys.

For data protection, I enable encryption everywhere: RDS and EBS with KMS keys, S3 with server-side encryption, and ACM certificates on the load balancer for HTTPS. Database connections require SSL.

Network security uses security groups as stateful firewalls—deny by default, allow specific. My RDS instances are in private subnets with no internet access, only accessible from application security group. I enable VPC Flow Logs to detect anomalies.

For secrets, I use AWS Secrets Manager with automatic rotation every 30 days. Applications retrieve credentials at runtime, never hardcoded.

For audit and compliance, CloudTrail logs all API calls, Config tracks resource changes, and GuardDuty provides threat detection. I've set up CloudWatch alarms for security events like unauthorized API calls or configuration changes.

Finally, I implement AWS Budgets with alerts at 80% and 100% of monthly budget to prevent cost surprises. All resources are tagged for cost allocation tracking."

**Progress: 15 of 28 sections complete (54%)** 📊

**Section 15 Complete:** Bibby is now production-secure and cost-optimized! 🔒💰✨
