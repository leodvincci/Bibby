# Section 13: AWS Core Concepts & Services

## Introduction: Taking Bibby to the Cloud

You've built a solid application with professional DevOps practices. Now it's time to deploy it to production using **Amazon Web Services (AWS)**, the world's most comprehensive cloud platform.

This section isn't about memorizing every AWS service (there are 200+). It's about understanding **core concepts** and the **essential services** you'll use 80% of the time, applied specifically to deploying Bibby.

**What You'll Learn:**
- Cloud computing fundamentals (IaaS, PaaS, SaaS)
- AWS account setup and security best practices
- Core services: EC2, RDS, S3, VPC, IAM
- Deploying Bibby to AWS step-by-step
- Cost optimization with Free Tier
- AWS CLI and Infrastructure as Code basics

**Real-World Context:**
Currently, Bibby runs on `localhost:8080` with a local PostgreSQL database. To make it production-ready, you need:
- A server that runs 24/7 (EC2)
- A managed database (RDS)
- Secure networking (VPC)
- Access control (IAM)
- File storage for backups (S3)

By the end of this section, Bibby will be running on AWS, accessible from anywhere.

---

## 1. Cloud Computing Fundamentals

### 1.1 The Three Service Models

**IaaS (Infrastructure as a Service):**
- **What you get:** Virtual machines, storage, networking
- **What you manage:** OS, runtime, application, data
- **AWS Example:** EC2 (Elastic Compute Cloud)
- **Use case:** Full control over environment

**PaaS (Platform as a Service):**
- **What you get:** Runtime environment, managed services
- **What you manage:** Application and data
- **AWS Example:** Elastic Beanstalk, RDS
- **Use case:** Focus on code, not infrastructure

**SaaS (Software as a Service):**
- **What you get:** Complete application
- **What you manage:** Configuration and data
- **AWS Example:** Amazon WorkSpaces, Chime
- **Use case:** Use pre-built applications

**For Bibby, we'll use:**
- **IaaS:** EC2 for application server
- **PaaS:** RDS for managed PostgreSQL database
- **SaaS:** (future) Amazon SES for email notifications

### 1.2 Why AWS?

**Comparison:**

| Feature | AWS | Azure | Google Cloud |
|---------|-----|-------|--------------|
| Market Share | ~32% | ~23% | ~10% |
| Services | 200+ | 200+ | 100+ |
| Free Tier | 12 months | 12 months | 90 days |
| Regions | 32 | 60+ | 35 |
| Best For | Startups, enterprises | Microsoft shops | ML/Data |

**AWS Advantages:**
- ✅ Most mature (launched 2006)
- ✅ Largest ecosystem
- ✅ Best documentation
- ✅ Most third-party integrations
- ✅ Generous Free Tier

**For Bibby:** AWS Free Tier gives us:
- 750 hours/month of t2.micro EC2 (always free for 1 year)
- 20 GB of RDS PostgreSQL
- 5 GB of S3 storage
- Plenty for learning and small projects!

### 1.3 AWS Global Infrastructure

**Hierarchy:**

```
Region (e.g., us-east-1, eu-west-1)
  └─ Availability Zone (AZ) (e.g., us-east-1a, us-east-1b)
      └─ Data Center (physical building)
```

**Key Concepts:**

**Region:**
- Geographic area (e.g., N. Virginia, London, Tokyo)
- Completely independent
- Choose based on: latency, compliance, pricing
- **For Bibby:** us-east-1 (N. Virginia) - cheapest, most services

**Availability Zone (AZ):**
- One or more data centers
- Isolated from failures in other AZs
- Connected with high-speed networking
- **For Bibby:** We'll use multi-AZ RDS for database redundancy

**Edge Locations:**
- Content delivery endpoints (CloudFront CDN)
- 400+ locations worldwide
- Caches content close to users

---

## 2. AWS Account Setup & Security

### 2.1 Creating AWS Account

**Steps:**

1. **Sign up:** https://aws.amazon.com/free/
2. **Email:** Use your personal email
3. **Payment:** Credit card required (won't be charged in Free Tier)
4. **Verification:** Phone verification
5. **Support Plan:** Choose Basic (free)

**Account ID:** Note your 12-digit account ID (e.g., 123456789012)

### 2.2 Securing Root Account

**⚠️ Critical:** Root account has unlimited access. Never use for daily tasks!

**Immediate Security Steps:**

**1. Enable MFA (Multi-Factor Authentication):**

```
AWS Console → Account (top-right) → Security Credentials
→ Multi-factor authentication (MFA) → Activate MFA

Options:
- Virtual MFA (Google Authenticator, Authy) ← Recommended
- Hardware MFA device
- U2F security key
```

**2. Create IAM Admin User (Don't use root!):**

```
AWS Console → IAM → Users → Add user

Username: admin-yourname
Access type: ☑ Programmatic access ☑ AWS Management Console access
Permissions: Attach existing policy → AdministratorAccess
Tags: Role=Admin, Project=Bibby
Create user → Download credentials CSV
```

**3. Enable MFA for Admin User:**

```
IAM → Users → admin-yourname → Security credentials
→ Assigned MFA device → Manage → Virtual MFA device
```

**4. Set Up IAM Password Policy:**

```
IAM → Account settings → Password policy → Set password policy

Minimum length: 12 characters
☑ Require uppercase letters
☑ Require lowercase letters
☑ Require numbers
☑ Require symbols
☑ Enable password expiration (90 days)
☑ Prevent password reuse (5 passwords)
```

**5. Enable CloudTrail (Audit Logging):**

```
AWS Console → CloudTrail → Create trail

Trail name: bibby-audit-trail
Storage location: Create new S3 bucket
Log file SSE-KMS encryption: Enabled
CloudWatch Logs: Enabled (optional, costs extra)
```

### 2.3 IAM (Identity and Access Management) Fundamentals

**Core Concepts:**

**User:**
- Individual person or service
- Has credentials (password, access keys)
- Example: admin-yourname, bibby-deploy-user

**Group:**
- Collection of users with shared permissions
- Example: Developers, Admins, ReadOnly

**Role:**
- Set of permissions for AWS services
- Example: EC2 instance role to access S3

**Policy:**
- JSON document defining permissions
- Example: Allow EC2 read-only access

**Example Policy (S3 Read-Only):**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::bibby-backups",
        "arn:aws:s3:::bibby-backups/*"
      ]
    }
  ]
}
```

**Best Practices:**

1. **Least Privilege:** Grant minimum necessary permissions
2. **Use Groups:** Assign permissions to groups, not individual users
3. **Use Roles:** For services (EC2, Lambda), not access keys
4. **Rotate Credentials:** Change passwords and keys regularly
5. **Enable MFA:** For all users with console access

### 2.4 Setting Up AWS CLI

**Install AWS CLI:**

```bash
# Linux/macOS
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Verify
aws --version
# Output: aws-cli/2.15.0 Python/3.11.6 Linux/5.15.0
```

**Configure AWS CLI:**

```bash
aws configure

# Prompts:
AWS Access Key ID: <from IAM user creation>
AWS Secret Access Key: <from IAM user creation>
Default region name: us-east-1
Default output format: json

# Credentials stored in ~/.aws/credentials
# Config stored in ~/.aws/config
```

**Test Configuration:**

```bash
# List S3 buckets (should be empty initially)
aws s3 ls

# Get caller identity
aws sts get-caller-identity

# Output:
{
    "UserId": "AIDAEXAMPLE123456",
    "Account": "123456789012",
    "Arn": "arn:aws:iam::123456789012:user/admin-yourname"
}
```

**Multiple Profiles:**

```bash
# Create profile for Bibby project
aws configure --profile bibby

# Use specific profile
aws s3 ls --profile bibby

# Set default profile
export AWS_PROFILE=bibby
```

---

## 3. Core AWS Services for Bibby

### 3.1 VPC (Virtual Private Cloud) - Networking Foundation

**What is VPC?**
- Isolated virtual network in AWS cloud
- Complete control over IP addresses, subnets, routing, gateways
- Default VPC exists in each region (we'll use it for simplicity)

**VPC Components:**

```
VPC (10.0.0.0/16)
  ├─ Public Subnet (10.0.1.0/24) - AZ us-east-1a
  │   ├─ Internet Gateway (IGW)
  │   └─ EC2 instances (Bibby app)
  ├─ Private Subnet (10.0.2.0/24) - AZ us-east-1a
  │   └─ RDS database
  └─ Private Subnet (10.0.3.0/24) - AZ us-east-1b
      └─ RDS standby (multi-AZ)
```

**For Bibby (using default VPC):**

```bash
# View default VPC
aws ec2 describe-vpcs --filters "Name=is-default,Values=true"

# Output:
{
    "VpcId": "vpc-12345678",
    "CidrBlock": "172.31.0.0/16",
    "State": "available"
}

# List subnets in default VPC
aws ec2 describe-subnets --filters "Name=vpc-id,Values=vpc-12345678"
```

**Security Groups (Virtual Firewalls):**

```bash
# Create security group for Bibby app
aws ec2 create-security-group \
  --group-name bibby-app-sg \
  --description "Security group for Bibby application" \
  --vpc-id vpc-12345678

# Allow SSH from your IP
aws ec2 authorize-security-group-ingress \
  --group-id sg-abc12345 \
  --protocol tcp \
  --port 22 \
  --cidr $(curl -s ifconfig.me)/32

# Allow HTTP from anywhere
aws ec2 authorize-security-group-ingress \
  --group-id sg-abc12345 \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0
```

### 3.2 EC2 (Elastic Compute Cloud) - Virtual Servers

**What is EC2?**
- Virtual servers in the cloud
- Choose: CPU, RAM, storage, OS
- Pay per hour (or per second for some types)

**Instance Types:**

| Family | Use Case | Example | vCPU | RAM |
|--------|----------|---------|------|-----|
| t2/t3 | General purpose | t2.micro | 1 | 1 GB |
| t2/t3 | Burstable | t3.small | 2 | 2 GB |
| m5 | Balanced | m5.large | 2 | 8 GB |
| c5 | Compute-optimized | c5.xlarge | 4 | 8 GB |
| r5 | Memory-optimized | r5.large | 2 | 16 GB |

**For Bibby:** t2.micro (Free Tier eligible: 750 hours/month for 12 months)

**Launching EC2 Instance for Bibby:**

```bash
# 1. Find Amazon Linux 2 AMI ID
aws ec2 describe-images \
  --owners amazon \
  --filters "Name=name,Values=amzn2-ami-hvm-*-x86_64-gp2" \
  --query 'Images[0].ImageId' \
  --output text

# Output: ami-0c55b159cbfafe1f0

# 2. Create key pair for SSH
aws ec2 create-key-pair \
  --key-name bibby-key \
  --query 'KeyMaterial' \
  --output text > ~/.ssh/bibby-key.pem

chmod 400 ~/.ssh/bibby-key.pem

# 3. Launch instance
aws ec2 run-instances \
  --image-id ami-0c55b159cbfafe1f0 \
  --instance-type t2.micro \
  --key-name bibby-key \
  --security-group-ids sg-abc12345 \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=bibby-app},{Key=Project,Value=Bibby}]' \
  --user-data file://bibby-setup.sh

# 4. Get instance public IP
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=bibby-app" \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text

# Output: 54.123.45.67
```

**User Data Script (`bibby-setup.sh`):**

```bash
#!/bin/bash
# Bibby EC2 instance initialization script

# Update system
yum update -y

# Install Java 17
amazon-linux-extras enable java-openjdk17
yum install java-17-openjdk -y

# Install Docker
yum install docker -y
systemctl start docker
systemctl enable docker
usermod -a -G docker ec2-user

# Install Git
yum install git -y

# Create application directory
mkdir -p /opt/bibby
cd /opt/bibby

# Clone Bibby repository (use HTTPS, no auth needed for public repos)
git clone https://github.com/leodvincci/Bibby.git .
git checkout v0.3.0

# Note: For production, download release JAR instead of cloning
# wget https://github.com/leodvincci/Bibby/releases/download/v0.3.0/Bibby-0.3.0.jar

# Set environment variables (will be replaced with SSM Parameter Store later)
cat > /opt/bibby/.env << 'EOF'
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://bibby-db.xxxxx.us-east-1.rds.amazonaws.com:5432/bibby
DATABASE_USERNAME=bibby_admin
DATABASE_PASSWORD=REPLACE_WITH_SECURE_PASSWORD
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
EnvironmentFile=/opt/bibby/.env
ExecStart=/usr/bin/java -jar /opt/bibby/target/Bibby-0.3.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Note: For initial setup, we'll deploy manually
# systemctl daemon-reload
# systemctl enable bibby
# systemctl start bibby

echo "Bibby EC2 instance setup complete!"
```

**Connecting to EC2:**

```bash
# SSH into instance
ssh -i ~/.ssh/bibby-key.pem ec2-user@54.123.45.67

# Check Java installation
java -version

# Check Docker
docker --version

# Navigate to application
cd /opt/bibby
ls -la
```

### 3.3 RDS (Relational Database Service) - Managed PostgreSQL

**What is RDS?**
- Managed database service (PostgreSQL, MySQL, MariaDB, Oracle, SQL Server)
- AWS handles: backups, patching, scaling, monitoring
- Multi-AZ for high availability

**For Bibby:** PostgreSQL 15 (Free Tier: 20 GB, 750 hours/month)

**Creating RDS Instance:**

```bash
# 1. Create DB subnet group
aws rds create-db-subnet-group \
  --db-subnet-group-name bibby-db-subnet-group \
  --db-subnet-group-description "Subnet group for Bibby database" \
  --subnet-ids subnet-abc123 subnet-def456

# 2. Create security group for database
aws ec2 create-security-group \
  --group-name bibby-db-sg \
  --description "Security group for Bibby database" \
  --vpc-id vpc-12345678

# 3. Allow PostgreSQL from app security group
aws ec2 authorize-security-group-ingress \
  --group-id sg-db123456 \
  --protocol tcp \
  --port 5432 \
  --source-group sg-abc12345

# 4. Create RDS instance
aws rds create-db-instance \
  --db-instance-identifier bibby-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.4 \
  --master-username bibby_admin \
  --master-user-password 'REPLACE_WITH_STRONG_PASSWORD_Min8Chars!' \
  --allocated-storage 20 \
  --storage-type gp2 \
  --vpc-security-group-ids sg-db123456 \
  --db-subnet-group-name bibby-db-subnet-group \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "mon:04:00-mon:05:00" \
  --multi-az false \
  --publicly-accessible false \
  --storage-encrypted true \
  --enable-cloudwatch-logs-exports '["postgresql"]' \
  --tags Key=Name,Value=bibby-db Key=Project,Value=Bibby

# 5. Wait for instance to be available (takes 5-10 minutes)
aws rds wait db-instance-available --db-instance-identifier bibby-db

# 6. Get database endpoint
aws rds describe-db-instances \
  --db-instance-identifier bibby-db \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text

# Output: bibby-db.abc123xyz.us-east-1.rds.amazonaws.com
```

**RDS Configuration Explained:**

- **db.t3.micro:** Free Tier eligible (1 vCPU, 1 GB RAM)
- **allocated-storage 20:** 20 GB (Free Tier limit)
- **backup-retention-period 7:** Keep backups for 7 days
- **multi-az false:** Single AZ (for cost savings; use true for production)
- **publicly-accessible false:** Only accessible from VPC (secure)
- **storage-encrypted true:** Encrypt data at rest

**Connecting to RDS from EC2:**

```bash
# SSH into EC2
ssh -i ~/.ssh/bibby-key.pem ec2-user@54.123.45.67

# Install PostgreSQL client
sudo yum install postgresql15 -y

# Connect to RDS
psql -h bibby-db.abc123xyz.us-east-1.rds.amazonaws.com \
     -U bibby_admin \
     -d postgres

# Create Bibby database
CREATE DATABASE bibby;

# Switch to bibby database
\c bibby

# Bibby's tables will be created automatically by Spring Boot on first run
```

### 3.4 S3 (Simple Storage Service) - Object Storage

**What is S3?**
- Object storage (files, not block storage)
- Unlimited storage, pay per GB
- 99.999999999% (11 nines) durability
- Use cases: Backups, static files, logs, artifacts

**For Bibby:** Store database backups and application logs

**Creating S3 Bucket:**

```bash
# 1. Create bucket (name must be globally unique)
aws s3 mb s3://bibby-backups-$(date +%s) --region us-east-1

# Example: bibby-backups-1705234567

# 2. Enable versioning
aws s3api put-bucket-versioning \
  --bucket bibby-backups-1705234567 \
  --versioning-configuration Status=Enabled

# 3. Enable encryption
aws s3api put-bucket-encryption \
  --bucket bibby-backups-1705234567 \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

# 4. Set lifecycle policy (delete old backups)
aws s3api put-bucket-lifecycle-configuration \
  --bucket bibby-backups-1705234567 \
  --lifecycle-configuration file://lifecycle.json
```

**Lifecycle Policy (`lifecycle.json`):**

```json
{
  "Rules": [
    {
      "Id": "DeleteOldBackups",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "database-backups/"
      },
      "Transitions": [
        {
          "Days": 30,
          "StorageClass": "STANDARD_IA"
        },
        {
          "Days": 90,
          "StorageClass": "GLACIER"
        }
      ],
      "Expiration": {
        "Days": 365
      }
    }
  ]
}
```

**Uploading Backups:**

```bash
# Create database backup
pg_dump -h bibby-db.abc123xyz.us-east-1.rds.amazonaws.com \
        -U bibby_admin \
        -d bibby \
        -F c \
        -f bibby-backup-$(date +%Y%m%d).dump

# Upload to S3
aws s3 cp bibby-backup-$(date +%Y%m%d).dump \
  s3://bibby-backups-1705234567/database-backups/

# List backups
aws s3 ls s3://bibby-backups-1705234567/database-backups/
```

**S3 Storage Classes:**

| Class | Use Case | Cost | Availability |
|-------|----------|------|--------------|
| Standard | Frequent access | $$ | 99.99% |
| Intelligent-Tiering | Unknown access | Auto | 99.9% |
| Standard-IA | Infrequent access | $ | 99.9% |
| Glacier Instant | Archive, instant | $ | 99.9% |
| Glacier Flexible | Archive, minutes-hours | ¢ | 99.99% |
| Glacier Deep Archive | Long-term archive | ¢¢ | 99.99% |

**For Bibby:**
- Recent backups (< 30 days): Standard
- Old backups (30-90 days): Standard-IA
- Archive (90-365 days): Glacier
- Delete after 1 year

---

## 4. Deploying Bibby to AWS

### 4.1 Complete Deployment Checklist

**Prerequisites:**
- [ ] AWS account created
- [ ] Root account secured with MFA
- [ ] IAM admin user created
- [ ] AWS CLI configured
- [ ] SSH key pair created (bibby-key)

**Infrastructure:**
- [ ] VPC and subnets identified (using default VPC)
- [ ] Security groups created (bibby-app-sg, bibby-db-sg)
- [ ] EC2 instance launched (t2.micro)
- [ ] RDS instance created (db.t3.micro, PostgreSQL 15)
- [ ] S3 bucket created (bibby-backups)

**Application:**
- [ ] Bibby built locally (`mvn clean package`)
- [ ] JAR uploaded to EC2
- [ ] Environment variables configured
- [ ] Database initialized
- [ ] Application started as systemd service
- [ ] Health check verified

### 4.2 Step-by-Step Deployment

**Step 1: Build Bibby Locally**

```bash
# On your local machine
cd ~/Bibby
git checkout v0.3.0

# Update pom.xml with RDS connection
# (Already done in Section 6)

# Build JAR
mvn clean package -DskipTests

# Verify JAR created
ls -lh target/Bibby-0.3.0.jar
```

**Step 2: Upload to EC2**

```bash
# SCP JAR to EC2
scp -i ~/.ssh/bibby-key.pem \
    target/Bibby-0.3.0.jar \
    ec2-user@54.123.45.67:/opt/bibby/

# Upload application-prod.properties
scp -i ~/.ssh/bibby-key.pem \
    src/main/resources/application-prod.properties \
    ec2-user@54.123.45.67:/opt/bibby/config/
```

**Step 3: Configure Environment**

```bash
# SSH into EC2
ssh -i ~/.ssh/bibby-key.pem ec2-user@54.123.45.67

# Create config file
sudo mkdir -p /opt/bibby/config
sudo vi /opt/bibby/config/application-prod.properties
```

**`application-prod.properties`:**

```properties
# Database configuration
spring.datasource.url=jdbc:postgresql://bibby-db.abc123xyz.us-east-1.rds.amazonaws.com:5432/bibby
spring.datasource.username=bibby_admin
spring.datasource.password=${DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=false

# Logging
logging.level.root=INFO
logging.level.com.penrose.bibby=INFO
logging.file.name=/var/log/bibby/application.log

# Server configuration
server.port=8080
server.error.include-stacktrace=never
server.error.include-message=never

# Application configuration
spring.application.name=Bibby
spring.profiles.active=prod
```

**Step 4: Create Systemd Service**

```bash
# Create service file
sudo vi /etc/systemd/system/bibby.service
```

**`/etc/systemd/system/bibby.service`:**

```ini
[Unit]
Description=Bibby Library Management Application
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/bibby
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="DATABASE_PASSWORD=YOUR_SECURE_PASSWORD"
ExecStart=/usr/bin/java -jar /opt/bibby/Bibby-0.3.0.jar \
  --spring.config.location=file:/opt/bibby/config/application-prod.properties
SuccessExitStatus=143
StandardOutput=journal
StandardError=journal
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Step 5: Start Application**

```bash
# Create log directory
sudo mkdir -p /var/log/bibby
sudo chown ec2-user:ec2-user /var/log/bibby

# Reload systemd
sudo systemctl daemon-reload

# Enable service (start on boot)
sudo systemctl enable bibby

# Start service
sudo systemctl start bibby

# Check status
sudo systemctl status bibby

# View logs
sudo journalctl -u bibby -f
```

**Step 6: Verify Deployment**

```bash
# Check if application is running
curl http://localhost:8080/actuator/health

# Output:
# {"status":"UP"}

# From your local machine, access Bibby
curl http://54.123.45.67:8080/actuator/health

# If using Spring Shell (CLI app), SSH in and run
ssh -i ~/.ssh/bibby-key.pem ec2-user@54.123.45.67
cd /opt/bibby
java -jar Bibby-0.3.0.jar
# Bibby shell starts
```

### 4.3 Post-Deployment Configuration

**1. Set up CloudWatch Logging:**

```bash
# Install CloudWatch agent
sudo yum install amazon-cloudwatch-agent -y

# Configure agent
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-config-wizard

# Start agent
sudo systemctl start amazon-cloudwatch-agent
sudo systemctl enable amazon-cloudwatch-agent
```

**2. Create AMI (Amazon Machine Image) for Backup:**

```bash
# Create image of configured EC2 instance
aws ec2 create-image \
  --instance-id i-abc123xyz \
  --name "bibby-app-$(date +%Y%m%d)" \
  --description "Bibby application server backup" \
  --no-reboot

# List your AMIs
aws ec2 describe-images --owners self
```

**3. Set up Automated Backups:**

```bash
# Create backup script
cat > /opt/bibby/backup.sh << 'EOF'
#!/bin/bash
# Automated database backup script

BACKUP_FILE="bibby-backup-$(date +%Y%m%d-%H%M%S).dump"
S3_BUCKET="bibby-backups-1705234567"

# Create backup
PGPASSWORD=$DATABASE_PASSWORD pg_dump \
  -h bibby-db.abc123xyz.us-east-1.rds.amazonaws.com \
  -U bibby_admin \
  -d bibby \
  -F c \
  -f /tmp/$BACKUP_FILE

# Upload to S3
aws s3 cp /tmp/$BACKUP_FILE s3://$S3_BUCKET/database-backups/

# Clean up local file
rm /tmp/$BACKUP_FILE

echo "Backup completed: $BACKUP_FILE"
EOF

chmod +x /opt/bibby/backup.sh

# Add to crontab (daily at 2 AM)
crontab -e
# Add line: 0 2 * * * /opt/bibby/backup.sh >> /var/log/bibby/backup.log 2>&1
```

---

## 5. Cost Management & Free Tier

### 5.1 AWS Free Tier Details

**Always Free (No expiration):**
- Lambda: 1M requests/month
- DynamoDB: 25 GB storage
- SNS: 1M publishes/month
- CloudWatch: 10 custom metrics

**12 Months Free:**
- EC2: 750 hours/month of t2.micro (1 instance running 24/7)
- RDS: 750 hours/month of db.t3.micro
- S3: 5 GB standard storage
- Data Transfer: 100 GB outbound

**For Bibby (Monthly Free Tier):**
- ✅ EC2 t2.micro: 720 hours (1 instance × 30 days × 24 hours)
- ✅ RDS db.t3.micro: 720 hours
- ✅ S3: ~2 GB (database backups)
- ✅ Data Transfer: ~5 GB (minimal traffic)

**Total Monthly Cost: $0** (within Free Tier limits for 12 months)

### 5.2 Monitoring Costs

**Set up Billing Alerts:**

```bash
# Enable billing alerts (one-time setup)
aws ce put-cost-anomaly-monitor \
  --anomaly-monitor '{
    "MonitorName": "BibbySpendingAlert",
    "MonitorType": "CUSTOM",
    "MonitorSpecification": {
      "CostCategoryReference": "Service"
    }
  }'

# Create SNS topic for alerts
aws sns create-topic --name bibby-billing-alerts

# Subscribe your email
aws sns subscribe \
  --topic-arn arn:aws:sns:us-east-1:123456789012:bibby-billing-alerts \
  --protocol email \
  --notification-endpoint your.email@example.com

# Confirm subscription via email
```

**AWS Console Method:**

```
AWS Console → Billing → Billing preferences
☑ Receive Free Tier Usage Alerts
☑ Receive Billing Alerts
Email: your.email@example.com

CloudWatch → Alarms → Create alarm
Metric: EstimatedCharges
Threshold: > $5
Action: Send notification to bibby-billing-alerts
```

### 5.3 Cost Optimization Tips

**1. Stop EC2 when not needed:**

```bash
# Stop instance (preserves data, no compute charges)
aws ec2 stop-instances --instance-ids i-abc123xyz

# Start instance
aws ec2 start-instances --instance-ids i-abc123xyz

# Automate: Stop at night, start in morning
# Using Lambda + EventBridge (CloudWatch Events)
```

**2. Delete unused resources:**

```bash
# Delete old snapshots
aws ec2 describe-snapshots --owner-ids self \
  --query 'Snapshots[?StartTime<=`2024-01-01`].[SnapshotId]' \
  --output text | xargs -I {} aws ec2 delete-snapshot --snapshot-id {}

# Delete unused AMIs
aws ec2 deregister-image --image-id ami-old123
```

**3. Use S3 Lifecycle Policies:**
- Already configured in Section 3.4
- Automatic transition to cheaper storage classes

**4. Right-size resources:**
- Monitor CPU/memory usage with CloudWatch
- Downgrade if consistently < 30% utilization

---

## 6. Interview-Ready Knowledge

### Question: "Explain the difference between EC2, RDS, and Lambda"

**Bad Answer:** "EC2 is servers, RDS is databases, Lambda is functions."

**Good Answer:**
"They represent different levels of abstraction and management responsibility.

EC2 is Infrastructure as a Service—you get virtual machines and full control over the OS, but you're responsible for patching, scaling, and availability. For my Bibby project, I use a t2.micro EC2 instance running Amazon Linux 2, where I have complete control to install Java, configure the environment, and deploy the application JAR. I'm responsible for the OS updates, security patches, and setting up systemd for auto-restart.

RDS is Platform as a Service for databases—AWS manages backups, patching, replication, and scaling. I use db.t3.micro PostgreSQL for Bibby's database. AWS automatically handles backup retention, minor version upgrades, and I can enable multi-AZ for high availability with just a checkbox. I only manage the database schema and queries.

Lambda is Function as a Service—you provide code, AWS manages everything else: servers, scaling, patching, availability. You pay per execution, not per hour. For example, if I wanted to process Bibby's database backups or send email notifications, Lambda would be ideal because those are event-driven tasks that don't need a server running 24/7.

The tradeoff is control versus management burden. More control (EC2) means more responsibility; less control (Lambda) means AWS handles more but you have less flexibility."

**Why It's Good:**
- Explains IaaS/PaaS/FaaS distinction
- Real project examples
- Mentions specific instance types
- Discusses tradeoffs
- Shows when to use each

### Question: "How do you secure an AWS environment?"

**Bad Answer:** "Use strong passwords and security groups."

**Good Answer:**
"AWS security follows the shared responsibility model—AWS secures the infrastructure, I secure what I put in the cloud.

My approach starts with IAM: I never use the root account for daily operations—it's MFA-protected and locked away. I create IAM users with least-privilege permissions, organized into groups. For Bibby, I have separate roles: one for deployment with EC2 and RDS permissions, one read-only for monitoring. I use IAM roles for services like EC2 instances instead of embedding access keys, which prevents credential leakage.

For network security, I use security groups as stateful firewalls. My Bibby deployment has separate security groups: bibby-app-sg allows SSH from my IP and HTTP on port 8080 from anywhere; bibby-db-sg only allows PostgreSQL traffic from bibby-app-sg, not the internet. The database is in a private subnet with no public access.

I enable encryption everywhere: RDS encryption at rest, S3 bucket encryption, and SSL/TLS in transit. I also enable CloudTrail for audit logging—every API call is logged to detect suspicious activity.

For credentials, I use Systems Manager Parameter Store or Secrets Manager instead of hardcoding in application.properties. Database passwords are rotated quarterly.

Finally, I set up billing alerts and AWS Config rules to detect misconfigurations like publicly accessible RDS instances or S3 buckets with public write access."

**Why It's Good:**
- Mentions shared responsibility
- Multiple security layers (IAM, network, encryption)
- Specific examples from project
- Explains why (least privilege, audit trail)
- Operational practices (rotation, monitoring)

### Question: "How would you deploy a multi-region application?"

**Bad Answer:** "Run the application in multiple AWS regions."

**Good Answer:**
"Multi-region deployment involves several considerations: data consistency, latency, failover strategy, and cost.

For Bibby, if I needed multi-region, I'd start by identifying the regions based on user location—say us-east-1 for East Coast and eu-west-1 for European users. I'd deploy identical infrastructure in both regions: EC2 for the application, RDS for the database.

The challenge is data consistency. For the database, I have options: RDS read replicas for read-heavy workloads, or Aurora Global Database for fast replication across regions with < 1 second lag. For Bibby, since it's a library app without hard real-time requirements, I'd use Aurora Global with writes going to the primary region and reads served locally.

For routing, I'd use Route 53 with latency-based routing—users automatically connect to the nearest region. I'd set up health checks so if us-east-1 fails, traffic automatically routes to eu-west-1.

Application state is tricky—I'd need to ensure sessions are stateless or use DynamoDB Global Tables for session storage. For file storage, S3 with Cross-Region Replication ensures backups exist in both regions.

The deployment pipeline needs updates too—CI/CD must deploy to all regions, and I'd use canary deployments: deploy to one region, monitor, then deploy to others.

Cost is significant—you're essentially doubling infrastructure. I'd evaluate whether the SLA improvement justifies the cost. For a personal project like Bibby, single-region with good backups and documented recovery procedures is more pragmatic."

**Why It's Good:**
- Identifies key challenges (data, routing, state)
- Specific AWS services (Aurora Global, Route 53, DynamoDB Global Tables)
- Explains tradeoffs (complexity vs availability vs cost)
- Shows practical thinking (cost-benefit analysis)
- Realistic for different scales (personal vs enterprise)

---

## 7. Summary & Next Steps

### Key Takeaways

✅ **Cloud Fundamentals:**
- IaaS (EC2), PaaS (RDS), SaaS models
- AWS global infrastructure (regions, AZs)
- Shared responsibility model

✅ **Security:**
- Root account MFA and IAM users
- Least privilege principle
- Security groups as virtual firewalls
- Encryption at rest and in transit

✅ **Core Services:**
- EC2: Virtual servers (t2.micro for Bibby)
- RDS: Managed PostgreSQL (db.t3.micro)
- S3: Object storage (backups, logs)
- VPC: Network isolation

✅ **Deployment:**
- Complete Bibby deployment on AWS
- Systemd service configuration
- Automated backups to S3
- CloudWatch logging

✅ **Cost Management:**
- Free Tier utilization (12 months)
- Billing alerts
- Resource optimization

### Immediate Action Items

**Phase 1: Setup (2 hours)**
- [ ] Create AWS account
- [ ] Enable root MFA
- [ ] Create IAM admin user
- [ ] Install and configure AWS CLI

**Phase 2: Infrastructure (3 hours)**
- [ ] Create security groups
- [ ] Launch EC2 instance (t2.micro)
- [ ] Create RDS instance (db.t3.micro)
- [ ] Create S3 bucket

**Phase 3: Deployment (2 hours)**
- [ ] Build Bibby JAR
- [ ] Upload to EC2
- [ ] Configure environment
- [ ] Start systemd service
- [ ] Verify health check

**Phase 4: Monitoring (1 hour)**
- [ ] Set up CloudWatch logging
- [ ] Create billing alerts
- [ ] Set up automated backups

### Coming Up in Section 14

**AWS Deployment Strategies & Automation:**
- Infrastructure as Code with CloudFormation/Terraform
- CI/CD integration with AWS CodePipeline
- Blue-Green and Canary deployments
- Auto Scaling Groups and Load Balancers
- Monitoring with CloudWatch dashboards
- Complete production-ready AWS architecture

You'll learn how to automate your AWS infrastructure and deployments, making your application truly enterprise-grade.

---

**Section 13 Complete:** Bibby is now running in the cloud! You have hands-on AWS experience with the core services used in 90% of production deployments. ☁️✨
