# Section 14: AWS Deployment Strategies & Automation

## Introduction: From Manual to Automated Infrastructure

In Section 13, we manually deployed Bibby to AWS—clicking through the console, running CLI commands one by one. This works for learning, but it's not scalable or repeatable.

**The Problem with Manual Deployment:**
- ❌ Not reproducible (can't recreate exact environment)
- ❌ Error-prone (easy to miss a step)
- ❌ Not version-controlled (no history of changes)
- ❌ Slow (takes hours to deploy)
- ❌ No rollback (hard to undo changes)

**The Solution: Infrastructure as Code (IaC)**
- ✅ Reproducible (same code = same infrastructure)
- ✅ Version-controlled (track changes in Git)
- ✅ Automated (deploy with one command)
- ✅ Documented (code IS documentation)
- ✅ Testable (validate before applying)

**What You'll Learn:**
- Infrastructure as Code with CloudFormation and Terraform
- Automated CI/CD pipeline with GitHub Actions + AWS
- Blue-Green and Canary deployment strategies
- Auto Scaling Groups and Load Balancers
- CloudWatch monitoring and dashboards
- Complete production-ready architecture

**Real-World Context:**
We'll transform Bibby from a single manually-deployed EC2 instance to a production-grade architecture with auto-scaling, load balancing, zero-downtime deployments, and full observability.

---

## 1. Infrastructure as Code Fundamentals

### 1.1 Why Infrastructure as Code?

**Traditional (Manual):**
```bash
# Day 1
aws ec2 run-instances --instance-type t2.micro ...
# (copy-paste 20 parameters)

# Day 30: "What did I configure?"
# Look through CloudTrail logs, try to remember...

# Day 60: Need another environment
# Repeat all steps, hope you remember everything
```

**Infrastructure as Code:**
```bash
# Day 1
terraform apply

# Day 30: "What did I configure?"
cat main.tf  # All settings in version-controlled file

# Day 60: Need another environment
terraform workspace new staging
terraform apply  # Identical infrastructure in 5 minutes
```

### 1.2 IaC Tools Comparison

| Tool | Language | Provider | Maturity | Use Case |
|------|----------|----------|----------|----------|
| **CloudFormation** | YAML/JSON | AWS-native | Mature | AWS-only projects |
| **Terraform** | HCL | Multi-cloud | Very mature | Multi-cloud, community |
| **Pulumi** | Python/JS/Go | Multi-cloud | Growing | Developers who prefer real code |
| **CDK** | TypeScript/Python | AWS | Newer | AWS with programmatic constructs |

**For Bibby:** We'll use **Terraform** because:
- ✅ Industry standard (most job postings)
- ✅ Better state management than CloudFormation
- ✅ Rich ecosystem and modules
- ✅ Multi-cloud (if you expand beyond AWS)
- ✅ Plan before apply (preview changes)

---

## 2. Terraform Basics for Bibby

### 2.1 Installing Terraform

```bash
# Linux/macOS
wget https://releases.hashicorp.com/terraform/1.6.6/terraform_1.6.6_linux_amd64.zip
unzip terraform_1.6.6_linux_amd64.zip
sudo mv terraform /usr/local/bin/

# Verify
terraform version
# Terraform v1.6.6
```

### 2.2 Project Structure

```
Bibby/
├── terraform/
│   ├── main.tf              # Main configuration
│   ├── variables.tf         # Input variables
│   ├── outputs.tf           # Output values
│   ├── terraform.tfvars     # Variable values (gitignored)
│   ├── modules/
│   │   ├── vpc/             # VPC module
│   │   ├── ec2/             # EC2 module
│   │   ├── rds/             # RDS module
│   │   └── s3/              # S3 module
│   └── environments/
│       ├── dev/             # Development environment
│       ├── staging/         # Staging environment
│       └── prod/            # Production environment
```

### 2.3 Basic Terraform Configuration

**`terraform/main.tf`:**

```hcl
# Terraform configuration
terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Remote state storage (use after initial setup)
  backend "s3" {
    bucket         = "bibby-terraform-state"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "bibby-terraform-locks"
  }
}

# AWS Provider
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "Bibby"
      ManagedBy   = "Terraform"
      Environment = var.environment
    }
  }
}

# Data sources
data "aws_availability_zones" "available" {
  state = "available"
}

# VPC
resource "aws_vpc" "bibby" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "bibby-vpc-${var.environment}"
  }
}

# Internet Gateway
resource "aws_internet_gateway" "bibby" {
  vpc_id = aws_vpc.bibby.id

  tags = {
    Name = "bibby-igw-${var.environment}"
  }
}

# Public Subnets (2 for high availability)
resource "aws_subnet" "public" {
  count             = 2
  vpc_id            = aws_vpc.bibby.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  map_public_ip_on_launch = true

  tags = {
    Name = "bibby-public-subnet-${count.index + 1}-${var.environment}"
    Type = "Public"
  }
}

# Private Subnets (2 for RDS multi-AZ)
resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.bibby.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 100)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "bibby-private-subnet-${count.index + 1}-${var.environment}"
    Type = "Private"
  }
}

# Route Table for Public Subnets
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.bibby.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.bibby.id
  }

  tags = {
    Name = "bibby-public-rt-${var.environment}"
  }
}

# Route Table Associations
resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Security Group for Application
resource "aws_security_group" "app" {
  name        = "bibby-app-sg-${var.environment}"
  description = "Security group for Bibby application"
  vpc_id      = aws_vpc.bibby.id

  # SSH from anywhere (restrict to your IP in production)
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "SSH access"
  }

  # HTTP from load balancer
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
    description     = "HTTP from ALB"
  }

  # Outbound internet access
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All outbound traffic"
  }

  tags = {
    Name = "bibby-app-sg-${var.environment}"
  }
}

# Security Group for Load Balancer
resource "aws_security_group" "alb" {
  name        = "bibby-alb-sg-${var.environment}"
  description = "Security group for Bibby load balancer"
  vpc_id      = aws_vpc.bibby.id

  # HTTP from internet
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP from internet"
  }

  # HTTPS from internet (if SSL configured)
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS from internet"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "bibby-alb-sg-${var.environment}"
  }
}

# Security Group for Database
resource "aws_security_group" "db" {
  name        = "bibby-db-sg-${var.environment}"
  description = "Security group for Bibby database"
  vpc_id      = aws_vpc.bibby.id

  # PostgreSQL from application
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
    description     = "PostgreSQL from app"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "bibby-db-sg-${var.environment}"
  }
}

# RDS Subnet Group
resource "aws_db_subnet_group" "bibby" {
  name       = "bibby-db-subnet-group-${var.environment}"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "bibby-db-subnet-group-${var.environment}"
  }
}

# RDS Instance
resource "aws_db_instance" "bibby" {
  identifier     = "bibby-db-${var.environment}"
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = var.db_instance_class

  allocated_storage     = 20
  max_allocated_storage = 100  # Auto-scaling storage
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = "bibby"
  username = var.db_username
  password = var.db_password  # Use AWS Secrets Manager in production

  vpc_security_group_ids = [aws_security_group.db.id]
  db_subnet_group_name   = aws_db_subnet_group.bibby.name

  multi_az               = var.environment == "prod" ? true : false
  publicly_accessible    = false

  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "mon:04:00-mon:05:00"

  skip_final_snapshot       = var.environment != "prod"
  final_snapshot_identifier = "bibby-db-final-${var.environment}-${formatdate("YYYY-MM-DD-hhmm", timestamp())}"

  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  tags = {
    Name = "bibby-db-${var.environment}"
  }
}

# S3 Bucket for Backups
resource "aws_s3_bucket" "backups" {
  bucket = "bibby-backups-${var.environment}-${data.aws_caller_identity.current.account_id}"

  tags = {
    Name = "bibby-backups-${var.environment}"
  }
}

# S3 Bucket Versioning
resource "aws_s3_bucket_versioning" "backups" {
  bucket = aws_s3_bucket.backups.id

  versioning_configuration {
    status = "Enabled"
  }
}

# S3 Bucket Encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "backups" {
  bucket = aws_s3_bucket.backups.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# S3 Lifecycle Policy
resource "aws_s3_bucket_lifecycle_configuration" "backups" {
  bucket = aws_s3_bucket.backups.id

  rule {
    id     = "delete-old-backups"
    status = "Enabled"

    filter {
      prefix = "database-backups/"
    }

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    expiration {
      days = 365
    }
  }
}

# Current AWS Account ID
data "aws_caller_identity" "current" {}
```

**`terraform/variables.tf`:**

```hcl
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be dev, staging, or prod."
  }
}

variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_username" {
  description = "Database master username"
  type        = string
  default     = "bibby_admin"
  sensitive   = true
}

variable "db_password" {
  description = "Database master password"
  type        = string
  sensitive   = true
}

variable "app_version" {
  description = "Bibby application version"
  type        = string
  default     = "0.3.0"
}
```

**`terraform/outputs.tf`:**

```hcl
output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.bibby.id
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = aws_subnet.private[*].id
}

output "db_endpoint" {
  description = "RDS endpoint"
  value       = aws_db_instance.bibby.endpoint
  sensitive   = true
}

output "s3_backup_bucket" {
  description = "S3 backup bucket name"
  value       = aws_s3_bucket.backups.id
}

output "app_security_group_id" {
  description = "Application security group ID"
  value       = aws_security_group.app.id
}
```

**`terraform/terraform.tfvars`** (gitignored):

```hcl
aws_region  = "us-east-1"
environment = "prod"

db_username = "bibby_admin"
db_password = "CHANGE_ME_STRONG_PASSWORD_123!"

app_version = "0.3.0"
```

### 2.4 Deploying with Terraform

```bash
# Navigate to terraform directory
cd terraform/

# Initialize Terraform (downloads providers)
terraform init

# Validate configuration
terraform validate

# Format code
terraform fmt -recursive

# Plan changes (preview)
terraform plan -out=tfplan

# Output:
# Plan: 25 to add, 0 to change, 0 to destroy.

# Apply changes
terraform apply tfplan

# Output after 5-10 minutes:
# Apply complete! Resources: 25 added, 0 changed, 0 destroyed.
#
# Outputs:
# db_endpoint = "bibby-db-prod.abc123.us-east-1.rds.amazonaws.com:5432"
# s3_backup_bucket = "bibby-backups-prod-123456789012"
# vpc_id = "vpc-abc123def456"

# View state
terraform show

# List resources
terraform state list

# Destroy infrastructure (when needed)
terraform destroy  # Be careful!
```

---

## 3. Auto Scaling and Load Balancing

### 3.1 Why Auto Scaling?

**Problem with Single EC2 Instance:**
- ❌ If instance fails, application down
- ❌ Can't handle traffic spikes
- ❌ No zero-downtime deployments
- ❌ Manual replacement if issues

**Solution: Auto Scaling Group**
- ✅ Automatically replaces failed instances
- ✅ Scales out under load
- ✅ Scales in to save costs
- ✅ Integrated with Load Balancer

### 3.2 Application Load Balancer Configuration

**Add to `terraform/main.tf`:**

```hcl
# Application Load Balancer
resource "aws_lb" "bibby" {
  name               = "bibby-alb-${var.environment}"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  enable_deletion_protection = var.environment == "prod" ? true : false
  enable_http2              = true
  enable_cross_zone_load_balancing = true

  tags = {
    Name = "bibby-alb-${var.environment}"
  }
}

# Target Group
resource "aws_lb_target_group" "bibby" {
  name     = "bibby-tg-${var.environment}"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = aws_vpc.bibby.id

  health_check {
    enabled             = true
    path                = "/actuator/health"
    port                = "8080"
    protocol            = "HTTP"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200"
  }

  deregistration_delay = 30

  tags = {
    Name = "bibby-tg-${var.environment}"
  }
}

# Listener (HTTP)
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.bibby.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.bibby.arn
  }
}

# (Optional) HTTPS Listener with ACM certificate
resource "aws_lb_listener" "https" {
  count = var.enable_https ? 1 : 0

  load_balancer_arn = aws_lb.bibby.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = var.acm_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.bibby.arn
  }
}
```

### 3.3 Auto Scaling Group Configuration

**Add to `terraform/main.tf`:**

```hcl
# Launch Template
resource "aws_launch_template" "bibby" {
  name_prefix   = "bibby-lt-${var.environment}-"
  image_id      = data.aws_ami.amazon_linux_2.id
  instance_type = var.instance_type

  key_name = var.key_name

  vpc_security_group_ids = [aws_security_group.app.id]

  iam_instance_profile {
    name = aws_iam_instance_profile.bibby.name
  }

  user_data = base64encode(templatefile("${path.module}/user-data.sh", {
    environment     = var.environment
    app_version     = var.app_version
    db_endpoint     = aws_db_instance.bibby.endpoint
    db_name         = aws_db_instance.bibby.db_name
    db_username     = aws_db_instance.bibby.username
    s3_bucket       = aws_s3_bucket.backups.id
    aws_region      = var.aws_region
  }))

  monitoring {
    enabled = true
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"  # IMDSv2
    http_put_response_hop_limit = 1
  }

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name = "bibby-app-${var.environment}"
    }
  }
}

# Auto Scaling Group
resource "aws_autoscaling_group" "bibby" {
  name                = "bibby-asg-${var.environment}"
  vpc_zone_identifier = aws_subnet.public[*].id
  target_group_arns   = [aws_lb_target_group.bibby.arn]
  health_check_type   = "ELB"
  health_check_grace_period = 300

  min_size         = var.asg_min_size
  max_size         = var.asg_max_size
  desired_capacity = var.asg_desired_capacity

  launch_template {
    id      = aws_launch_template.bibby.id
    version = "$Latest"
  }

  enabled_metrics = [
    "GroupDesiredCapacity",
    "GroupInServiceInstances",
    "GroupMaxSize",
    "GroupMinSize",
    "GroupPendingInstances",
    "GroupStandbyInstances",
    "GroupTerminatingInstances",
    "GroupTotalInstances"
  ]

  tag {
    key                 = "Name"
    value               = "bibby-app-${var.environment}"
    propagate_at_launch = true
  }

  tag {
    key                 = "Environment"
    value               = var.environment
    propagate_at_launch = true
  }
}

# Auto Scaling Policy (CPU-based)
resource "aws_autoscaling_policy" "cpu_scale_up" {
  name                   = "bibby-cpu-scale-up-${var.environment}"
  scaling_adjustment     = 1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 300
  autoscaling_group_name = aws_autoscaling_group.bibby.name
}

resource "aws_autoscaling_policy" "cpu_scale_down" {
  name                   = "bibby-cpu-scale-down-${var.environment}"
  scaling_adjustment     = -1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 300
  autoscaling_group_name = aws_autoscaling_group.bibby.name
}

# CloudWatch Alarms for Auto Scaling
resource "aws_cloudwatch_metric_alarm" "cpu_high" {
  alarm_name          = "bibby-cpu-high-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = "300"
  statistic           = "Average"
  threshold           = "70"
  alarm_description   = "Triggers when CPU exceeds 70%"
  alarm_actions       = [aws_autoscaling_policy.cpu_scale_up.arn]

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.bibby.name
  }
}

resource "aws_cloudwatch_metric_alarm" "cpu_low" {
  alarm_name          = "bibby-cpu-low-${var.environment}"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = "300"
  statistic           = "Average"
  threshold           = "20"
  alarm_description   = "Triggers when CPU drops below 20%"
  alarm_actions       = [aws_autoscaling_policy.cpu_scale_down.arn]

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.bibby.name
  }
}

# Data source for Amazon Linux 2 AMI
data "aws_ami" "amazon_linux_2" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}
```

**`terraform/user-data.sh`:**

```bash
#!/bin/bash
# Bibby Auto Scaling Group user data script

set -e

# Variables from Terraform
ENVIRONMENT="${environment}"
APP_VERSION="${app_version}"
DB_ENDPOINT="${db_endpoint}"
DB_NAME="${db_name}"
DB_USERNAME="${db_username}"
S3_BUCKET="${s3_bucket}"
AWS_REGION="${aws_region}"

# Update system
yum update -y

# Install Java 17
amazon-linux-extras enable java-openjdk17
yum install java-17-openjdk -y

# Install CloudWatch agent
yum install amazon-cloudwatch-agent -y

# Install AWS CLI v2
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
./aws/install
rm -rf aws awscliv2.zip

# Create application directory
mkdir -p /opt/bibby

# Download application JAR from S3
aws s3 cp s3://$S3_BUCKET/releases/Bibby-$APP_VERSION.jar /opt/bibby/Bibby.jar

# Get database password from Secrets Manager
DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id bibby/db/password/$ENVIRONMENT \
  --region $AWS_REGION \
  --query SecretString \
  --output text)

# Create application configuration
cat > /opt/bibby/application.properties << EOF
spring.profiles.active=$ENVIRONMENT
spring.datasource.url=jdbc:postgresql://$DB_ENDPOINT/$DB_NAME
spring.datasource.username=$DB_USERNAME
spring.datasource.password=$DB_PASSWORD
spring.jpa.hibernate.ddl-auto=update
server.port=8080
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
EOF

# Create systemd service
cat > /etc/systemd/system/bibby.service << 'EOF'
[Unit]
Description=Bibby Library Management Application
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/bibby
ExecStart=/usr/bin/java -jar /opt/bibby/Bibby.jar \
  --spring.config.location=file:/opt/bibby/application.properties
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

# Set permissions
chown -R ec2-user:ec2-user /opt/bibby

# Start application
systemctl daemon-reload
systemctl enable bibby
systemctl start bibby

# Configure CloudWatch agent
cat > /opt/aws/amazon-cloudwatch-agent/etc/config.json << EOF
{
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/log/bibby/*.log",
            "log_group_name": "/aws/ec2/bibby/$ENVIRONMENT",
            "log_stream_name": "{instance_id}",
            "timezone": "UTC"
          }
        ]
      }
    }
  },
  "metrics": {
    "namespace": "Bibby/$ENVIRONMENT",
    "metrics_collected": {
      "cpu": {
        "measurement": [
          {"name": "cpu_usage_idle", "rename": "CPU_IDLE", "unit": "Percent"},
          {"name": "cpu_usage_iowait", "rename": "CPU_IOWAIT", "unit": "Percent"},
          "cpu_time_guest"
        ],
        "metrics_collection_interval": 60,
        "totalcpu": false
      },
      "disk": {
        "measurement": [
          {"name": "used_percent", "rename": "DISK_USED", "unit": "Percent"}
        ],
        "metrics_collection_interval": 60,
        "resources": ["*"]
      },
      "mem": {
        "measurement": [
          {"name": "mem_used_percent", "rename": "MEM_USED", "unit": "Percent"}
        ],
        "metrics_collection_interval": 60
      }
    }
  }
}
EOF

systemctl start amazon-cloudwatch-agent
systemctl enable amazon-cloudwatch-agent

echo "Bibby instance initialization complete!"
```

### 3.4 IAM Role for EC2 Instances

**Add to `terraform/main.tf`:**

```hcl
# IAM Role for EC2 instances
resource "aws_iam_role" "bibby_ec2" {
  name = "bibby-ec2-role-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

# IAM Policy for S3 and Secrets Manager access
resource "aws_iam_role_policy" "bibby_ec2_policy" {
  name = "bibby-ec2-policy-${var.environment}"
  role = aws_iam_role.bibby_ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject"
        ]
        Resource = [
          "${aws_s3_bucket.backups.arn}/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.backups.arn
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = [
          "arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:bibby/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "cloudwatch:PutMetricData",
          "ec2:DescribeVolumes",
          "ec2:DescribeTags",
          "logs:PutLogEvents",
          "logs:CreateLogGroup",
          "logs:CreateLogStream"
        ]
        Resource = "*"
      }
    ]
  })
}

# IAM Instance Profile
resource "aws_iam_instance_profile" "bibby" {
  name = "bibby-instance-profile-${var.environment}"
  role = aws_iam_role.bibby_ec2.name
}
```

---

## 4. Deployment Strategies

### 4.1 Blue-Green Deployment

**Concept:** Run two identical environments (Blue = current, Green = new). Switch traffic when ready.

**Benefits:**
- ✅ Zero downtime
- ✅ Instant rollback (switch back to Blue)
- ✅ Full testing in production environment before cutover

**Implementation with ASG:**

```hcl
# Blue Auto Scaling Group (current)
resource "aws_autoscaling_group" "bibby_blue" {
  name                = "bibby-asg-blue-${var.environment}"
  vpc_zone_identifier = aws_subnet.public[*].id
  target_group_arns   = var.active_deployment == "blue" ? [aws_lb_target_group.bibby.arn] : []

  min_size         = var.active_deployment == "blue" ? var.asg_min_size : 0
  max_size         = var.asg_max_size
  desired_capacity = var.active_deployment == "blue" ? var.asg_desired_capacity : 0

  launch_template {
    id      = aws_launch_template.bibby_blue.id
    version = "$Latest"
  }

  tag {
    key                 = "Deployment"
    value               = "blue"
    propagate_at_launch = true
  }
}

# Green Auto Scaling Group (new)
resource "aws_autoscaling_group" "bibby_green" {
  name                = "bibby-asg-green-${var.environment}"
  vpc_zone_identifier = aws_subnet.public[*].id
  target_group_arns   = var.active_deployment == "green" ? [aws_lb_target_group.bibby.arn] : []

  min_size         = var.active_deployment == "green" ? var.asg_min_size : 0
  max_size         = var.asg_max_size
  desired_capacity = var.active_deployment == "green" ? var.asg_desired_capacity : 0

  launch_template {
    id      = aws_launch_template.bibby_green.id
    version = "$Latest"
  }

  tag {
    key                 = "Deployment"
    value               = "green"
    propagate_at_launch = true
  }
}
```

**Deployment Process:**

```bash
# Step 1: Currently Blue is active
terraform apply -var="active_deployment=blue" -var="app_version=0.3.0"

# Step 2: Deploy new version to Green
terraform apply -var="active_deployment=blue" -var="green_app_version=0.4.0"
# Green ASG starts with new version (but receives no traffic)

# Step 3: Test Green environment
curl http://<green-instance-ip>:8080/actuator/health

# Step 4: Switch traffic to Green
terraform apply -var="active_deployment=green"
# Traffic now routed to Green, Blue still running

# Step 5: Monitor metrics, if good, scale down Blue
terraform apply -var="active_deployment=green" -var="blue_asg_min_size=0"

# Rollback if issues: Switch back to Blue
terraform apply -var="active_deployment=blue"
```

### 4.2 Canary Deployment

**Concept:** Gradually route traffic to new version (5% → 25% → 50% → 100%).

**Implementation with Target Groups:**

```hcl
# Target Group for old version
resource "aws_lb_target_group" "bibby_old" {
  name     = "bibby-tg-old-${var.environment}"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = aws_vpc.bibby.id

  health_check {
    path     = "/actuator/health"
    interval = 30
  }
}

# Target Group for new version (canary)
resource "aws_lb_target_group" "bibby_canary" {
  name     = "bibby-tg-canary-${var.environment}"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = aws_vpc.bibby.id

  health_check {
    path     = "/actuator/health"
    interval = 30
  }
}

# Listener with weighted target groups
resource "aws_lb_listener_rule" "weighted" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 100

  action {
    type = "forward"

    forward {
      target_group {
        arn    = aws_lb_target_group.bibby_old.arn
        weight = 95  # 95% to old version
      }

      target_group {
        arn    = aws_lb_target_group.bibby_canary.arn
        weight = 5   # 5% to canary
      }

      stickiness {
        enabled  = true
        duration = 3600
      }
    }
  }

  condition {
    path_pattern {
      values = ["/*"]
    }
  }
}
```

**Canary Rollout:**

```bash
# Phase 1: 5% canary
terraform apply -var="canary_weight=5"

# Monitor metrics for 15 minutes
# If metrics good, proceed

# Phase 2: 25% canary
terraform apply -var="canary_weight=25"

# Phase 3: 50% canary
terraform apply -var="canary_weight=50"

# Phase 4: 100% canary (full rollout)
terraform apply -var="canary_weight=100"
```

---

## 5. CI/CD Integration with GitHub Actions

### 5.1 Complete CI/CD Pipeline

**`.github/workflows/deploy-aws.yml`:**

```yaml
name: Deploy to AWS

on:
  push:
    branches: [main]
    tags: ['v*']
  workflow_dispatch:
    inputs:
      environment:
        description: 'Environment to deploy'
        required: true
        type: choice
        options:
          - dev
          - staging
          - prod

jobs:
  build:
    name: Build and Test
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Run Tests
        run: mvn clean verify

      - name: Build JAR
        run: mvn package -DskipTests

      - name: Upload Artifact
        uses: actions/upload-artifact@v4
        with:
          name: bibby-jar
          path: target/Bibby-*.jar

  terraform-plan:
    name: Terraform Plan
    runs-on: ubuntu-latest
    needs: build

    steps:
      - uses: actions/checkout@v4

      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: 1.6.6

      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1

      - name: Terraform Init
        working-directory: terraform
        run: terraform init

      - name: Terraform Plan
        working-directory: terraform
        run: |
          terraform plan \
            -var="environment=${{ github.event.inputs.environment || 'dev' }}" \
            -var="app_version=${{ github.ref_name }}" \
            -out=tfplan

      - name: Upload Plan
        uses: actions/upload-artifact@v4
        with:
          name: terraform-plan
          path: terraform/tfplan

  deploy:
    name: Deploy to AWS
    runs-on: ubuntu-latest
    needs: terraform-plan
    environment:
      name: ${{ github.event.inputs.environment || 'dev' }}

    steps:
      - uses: actions/checkout@v4

      - name: Download JAR
        uses: actions/download-artifact@v4
        with:
          name: bibby-jar
          path: target/

      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1

      - name: Upload JAR to S3
        run: |
          aws s3 cp target/Bibby-*.jar \
            s3://bibby-releases-${{ github.event.inputs.environment || 'dev' }}/releases/

      - name: Download Terraform Plan
        uses: actions/download-artifact@v4
        with:
          name: terraform-plan
          path: terraform/

      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v3

      - name: Terraform Init
        working-directory: terraform
        run: terraform init

      - name: Terraform Apply
        working-directory: terraform
        run: terraform apply tfplan

      - name: Get Load Balancer URL
        id: alb
        working-directory: terraform
        run: |
          ALB_DNS=$(terraform output -raw alb_dns_name)
          echo "url=http://$ALB_DNS" >> $GITHUB_OUTPUT

      - name: Wait for Deployment
        run: sleep 120

      - name: Health Check
        run: |
          curl -f ${{ steps.alb.outputs.url }}/actuator/health || exit 1

      - name: Notify Success
        if: success()
        run: |
          echo "✅ Deployment successful!"
          echo "URL: ${{ steps.alb.outputs.url }}"

      - name: Notify Failure
        if: failure()
        run: |
          echo "❌ Deployment failed!"
```

---

## 6. Monitoring and Observability

### 6.1 CloudWatch Dashboard

**Create comprehensive dashboard:**

```hcl
resource "aws_cloudwatch_dashboard" "bibby" {
  dashboard_name = "Bibby-${var.environment}"

  dashboard_body = jsonencode({
    widgets = [
      {
        type = "metric"
        properties = {
          metrics = [
            ["AWS/ApplicationELB", "TargetResponseTime", {
              stat = "Average"
              label = "Response Time (avg)"
            }],
            [".", "RequestCount", { stat = "Sum", label = "Request Count" }],
            [".", "HTTPCode_Target_2XX_Count", { stat = "Sum", label = "2XX Responses" }],
            [".", "HTTPCode_Target_5XX_Count", { stat = "Sum", label = "5XX Errors" }]
          ]
          period = 300
          region = var.aws_region
          title  = "Application Load Balancer Metrics"
        }
      },
      {
        type = "metric"
        properties = {
          metrics = [
            ["AWS/EC2", "CPUUtilization", {
              dimensions = { AutoScalingGroupName = aws_autoscaling_group.bibby.name }
              stat       = "Average"
            }]
          ]
          period = 300
          region = var.aws_region
          title  = "EC2 CPU Utilization"
        }
      },
      {
        type = "metric"
        properties = {
          metrics = [
            ["AWS/RDS", "CPUUtilization", {
              dimensions = { DBInstanceIdentifier = aws_db_instance.bibby.id }
              stat       = "Average"
            }],
            [".", "DatabaseConnections", {
              dimensions = { DBInstanceIdentifier = aws_db_instance.bibby.id }
              stat       = "Sum"
            }]
          ]
          period = 300
          region = var.aws_region
          title  = "RDS Metrics"
        }
      }
    ]
  })
}
```

### 6.2 Key Alarms

```hcl
# High Error Rate Alarm
resource "aws_cloudwatch_metric_alarm" "high_error_rate" {
  alarm_name          = "bibby-high-error-rate-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "HTTPCode_Target_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "300"
  statistic           = "Sum"
  threshold           = "10"
  alarm_description   = "Triggers when 5XX errors exceed 10 in 5 minutes"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    LoadBalancer = aws_lb.bibby.arn_suffix
  }
}

# SNS Topic for Alerts
resource "aws_sns_topic" "alerts" {
  name = "bibby-alerts-${var.environment}"
}

resource "aws_sns_topic_subscription" "email" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}
```

---

## 7. Summary & Production Checklist

### Complete Architecture

```
Internet
   ↓
Route 53 (DNS)
   ↓
Application Load Balancer (ALB)
   ↓
Auto Scaling Group (2-10 instances)
   ├─ EC2 Instance 1 (Bibby app)
   ├─ EC2 Instance 2 (Bibby app)
   └─ ...
   ↓
RDS PostgreSQL (Multi-AZ)
   ↓
S3 (Backups)

Monitoring:
- CloudWatch Logs
- CloudWatch Metrics
- CloudWatch Alarms
- SNS Notifications
```

### Production Readiness Checklist

**✅ Infrastructure:**
- [ ] VPC with public/private subnets
- [ ] Security groups properly configured
- [ ] Auto Scaling Group (min 2, max 10)
- [ ] Application Load Balancer
- [ ] RDS Multi-AZ enabled
- [ ] S3 bucket for backups
- [ ] All resources tagged

**✅ Security:**
- [ ] IAM roles (no hardcoded credentials)
- [ ] Secrets Manager for sensitive data
- [ ] Security groups follow least privilege
- [ ] SSL/TLS certificate on ALB
- [ ] WAF rules (optional but recommended)

**✅ Monitoring:**
- [ ] CloudWatch Dashboard
- [ ] Alarms for CPU, errors, latency
- [ ] SNS notifications configured
- [ ] Log aggregation to CloudWatch Logs

**✅ CI/CD:**
- [ ] GitHub Actions workflow
- [ ] Automated testing
- [ ] Blue-Green or Canary deployment
- [ ] Rollback procedure documented

**✅ Disaster Recovery:**
- [ ] RDS automated backups (7 days)
- [ ] Manual snapshots before major changes
- [ ] Tested restore procedure
- [ ] Multi-region backup (optional)

**Progress: 14 of 28 sections complete (50%)** 📊

**Section 14 Complete:** Bibby now has production-grade AWS infrastructure with auto-scaling, load balancing, and automated deployments! 🚀✨
