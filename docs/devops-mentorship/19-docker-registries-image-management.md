# Section 19: Docker Registries & Image Management

## Introduction

You've built an optimized Docker image for Bibby (192MB, production-ready). But it only exists on your local machine. How do you:

- **Share it** with your team?
- **Deploy it** to staging and production servers?
- **Scan it** for security vulnerabilities?
- **Version it** properly across environments?
- **Promote it** through dev → staging → production?

The answer: **Docker registries**—centralized repositories for storing, distributing, and managing container images.

**Real-World Scenario:**

```
Developer Laptop          CI/CD (GitHub Actions)       Production Server
┌─────────────┐           ┌─────────────┐             ┌─────────────┐
│ Build       │           │ Build       │             │ Pull        │
│ bibby:local │──push────→│ bibby:0.3.0 │──push──┐   │ bibby:0.3.0 │
└─────────────┘           └─────────────┘        │   └─────────────┘
                                                 ↓
                                          ┌─────────────┐
                                          │  REGISTRY   │
                                          │  (AWS ECR)  │
                                          │             │
                                          │ bibby:0.3.0 │
                                          │ bibby:0.2.1 │
                                          │ bibby:latest│
                                          └─────────────┘
```

This section covers **complete image lifecycle management** from build to production deployment.

**What You'll Learn:**

1. **Docker Hub** - Public registry, free tier, automation
2. **Private registries** - AWS ECR, GCR, Harbor, Azure ACR
3. **Image tagging strategies** - Semantic versioning, immutable tags, conventions
4. **Pushing and pulling** - Authentication, automation, troubleshooting
5. **Image scanning** - Vulnerability detection (Trivy, Clair, ECR scanning)
6. **CI/CD integration** - Automated builds, tests, scans, and deployments
7. **Image promotion** - Dev → staging → production workflows
8. **Multi-registry strategies** - Mirroring, disaster recovery
9. **Registry security** - Access control, encryption, audit logging

**Prerequisites**: Sections 16-18 (Docker Fundamentals, Images & Layers, Container Lifecycle)

---

## 1. Docker Hub - Public Registry

### 1.1 What is Docker Hub?

**Docker Hub** (hub.docker.com) is Docker's official public registry:

- **Free tier**: 1 private repository, unlimited public repositories
- **Pull rate limits**: 100 pulls/6 hours (anonymous), 200 pulls/6 hours (free account)
- **Automated builds**: Link to GitHub, auto-build on push
- **Official images**: Curated images (postgres, nginx, redis, etc.)

### 1.2 Create Docker Hub Account

```bash
# Sign up at https://hub.docker.com
# Username: yourusername
# Email: your@email.com

# Login from CLI
docker login
# Username: yourusername
# Password: [your password]
# Login Succeeded

# Credentials stored in ~/.docker/config.json
cat ~/.docker/config.json
```

**Output:**

```json
{
  "auths": {
    "https://index.docker.io/v1/": {
      "auth": "eW91cnVzZXJuYW1lOnlvdXJwYXNzd29yZA=="
    }
  }
}
```

**Security Warning**: The auth token is base64-encoded (not encrypted). Use access tokens instead of passwords!

**Better Approach: Access Tokens**

```bash
# Docker Hub → Account Settings → Security → New Access Token
# Name: "dev-machine"
# Permissions: Read, Write, Delete
# Copy token: dckr_pat_1a2b3c4d5e6f...

# Login with token
docker login -u yourusername
# Password: [paste access token]

# Or via stdin (CI/CD)
echo "dckr_pat_1a2b3c4d5e6f..." | docker login -u yourusername --password-stdin
```

### 1.3 Push Bibby to Docker Hub

**Step 1: Tag Image with Username**

```bash
# Current image (local only)
docker images bibby
# REPOSITORY   TAG       IMAGE ID       SIZE
# bibby        0.3.0     a1b2c3d4e5f6   192MB

# Tag for Docker Hub (format: username/repository:tag)
docker tag bibby:0.3.0 yourusername/bibby:0.3.0
docker tag bibby:0.3.0 yourusername/bibby:latest

# Verify tags
docker images yourusername/bibby
# REPOSITORY           TAG       IMAGE ID       SIZE
# yourusername/bibby   0.3.0     a1b2c3d4e5f6   192MB
# yourusername/bibby   latest    a1b2c3d4e5f6   192MB
```

**Step 2: Push to Docker Hub**

```bash
# Push specific version
docker push yourusername/bibby:0.3.0

# Output:
The push refers to repository [docker.io/yourusername/bibby]
5f70bf18a086: Pushed
d3e1f9f6c8b2: Pushed
a4c2d8e9f1a3: Layer already exists
0.3.0: digest: sha256:a1b2c3d4e5f6... size: 1789

# Push latest
docker push yourusername/bibby:latest
```

**Step 3: Verify on Docker Hub**

Visit: https://hub.docker.com/r/yourusername/bibby

**You'll see:**
- Repository: `yourusername/bibby`
- Tags: `0.3.0`, `latest`
- Size: 192MB
- Last pushed: 2 minutes ago

### 1.4 Pull Bibby on Another Machine

```bash
# On production server (clean machine)
docker pull yourusername/bibby:0.3.0

# Run without building locally!
docker run -d \
  --name bibby-prod \
  -p 8080:8080 \
  yourusername/bibby:0.3.0

# Verify
docker ps
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

**This is the power of registries**: Build once, run anywhere.

### 1.5 Public vs Private Repositories

**Public Repository** (free):

```bash
# Anyone can pull
docker pull yourusername/bibby:0.3.0
# No authentication required
```

**Private Repository** ($5/month for 5 repos):

```bash
# Must authenticate to pull
docker pull yourusername/bibby-private:0.3.0
# Error: unauthorized

# Login first
docker login
docker pull yourusername/bibby-private:0.3.0
# Success
```

**When to Use Private:**
- Proprietary code
- Internal tools
- Security-sensitive applications

**For Bibby**: Public is fine (open-source library management tool).

### 1.6 Docker Hub Rate Limits

**Limits (as of 2025):**

| Account Type | Pulls/6 hours | Cost |
|--------------|---------------|------|
| Anonymous | 100 | Free |
| Free | 200 | Free |
| Pro | 5,000 | $5/month |
| Team | 5,000 | $7/user/month |

**Check Your Rate Limit:**

```bash
# Make authenticated request
TOKEN=$(curl -s "https://auth.docker.io/token?service=registry.docker.io&scope=repository:ratelimitpreview/test:pull" | jq -r .token)

curl -s -H "Authorization: Bearer $TOKEN" https://registry-1.docker.io/v2/ratelimitpreview/test/manifests/latest -I | grep RateLimit

# Output:
ratelimit-limit: 200;w=21600
ratelimit-remaining: 184;w=21600
```

**Production Recommendation**: Use private registry (AWS ECR, GCR) to avoid rate limits.

### 1.7 Automated Builds (Deprecated)

**Note**: Docker Hub deprecated automated builds in 2021. Use GitHub Actions instead (covered in Section 1.9).

---

## 2. Private Registries

### 2.1 Why Private Registries?

**Benefits over Docker Hub:**

1. **No rate limits** (you control infrastructure)
2. **Security**: Images stay within your network/cloud
3. **Compliance**: Data sovereignty, audit logs
4. **Performance**: Co-located with compute (same AWS region)
5. **Cost**: Included in cloud credits, cheaper at scale
6. **Advanced features**: Vulnerability scanning, image signing, replication

**Popular Options:**

| Registry | Provider | Best For | Cost |
|----------|----------|----------|------|
| AWS ECR | Amazon | AWS deployments | $0.10/GB/month |
| GCR | Google | GCP deployments | $0.10/GB/month |
| ACR | Microsoft | Azure deployments | $0.10/GB/month |
| Harbor | Self-hosted | On-prem, multi-cloud | Infrastructure cost |
| GitLab Registry | GitLab | GitLab CI/CD | Included in GitLab |
| GitHub Packages | GitHub | GitHub Actions | $0.25/GB |

### 2.2 AWS ECR (Elastic Container Registry)

**Best choice for AWS deployments**. Fully managed, integrated with IAM, encrypted at rest.

**Step 1: Create ECR Repository**

```bash
# Install AWS CLI
pip install awscli

# Configure credentials
aws configure
# AWS Access Key ID: AKIA...
# AWS Secret Access Key: ...
# Default region: us-east-1
# Default output format: json

# Create repository
aws ecr create-repository \
  --repository-name bibby \
  --region us-east-1

# Output:
{
  "repository": {
    "repositoryArn": "arn:aws:ecr:us-east-1:123456789012:repository/bibby",
    "repositoryName": "bibby",
    "repositoryUri": "123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby",
    "createdAt": "2025-01-17T10:30:00.000000-05:00"
  }
}
```

**Step 2: Authenticate Docker to ECR**

```bash
# Get login password and pipe to docker login
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

# Login Succeeded

# Token valid for 12 hours
```

**Step 3: Tag and Push Bibby**

```bash
# Tag for ECR (format: account.dkr.ecr.region.amazonaws.com/repository:tag)
docker tag bibby:0.3.0 \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:0.3.0

docker tag bibby:0.3.0 \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:latest

# Push
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:0.3.0
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:latest

# Output:
The push refers to repository [123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby]
5f70bf18a086: Pushed
d3e1f9f6c8b2: Pushed
0.3.0: digest: sha256:a1b2c3d4e5f6... size: 1789
```

**Step 4: List Images**

```bash
aws ecr list-images --repository-name bibby --region us-east-1

# Output:
{
  "imageIds": [
    {
      "imageDigest": "sha256:a1b2c3d4e5f6...",
      "imageTag": "0.3.0"
    },
    {
      "imageDigest": "sha256:a1b2c3d4e5f6...",
      "imageTag": "latest"
    }
  ]
}
```

**Step 5: Pull on EC2 Instance**

```bash
# On EC2 instance with IAM role (ecr:GetAuthorizationToken, ecr:BatchGetImage)
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

docker pull 123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:0.3.0

docker run -d \
  --name bibby-prod \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:0.3.0
```

**ECR Lifecycle Policies** (automatic cleanup):

```json
{
  "rules": [
    {
      "rulePriority": 1,
      "description": "Keep last 10 tagged images",
      "selection": {
        "tagStatus": "tagged",
        "tagPrefixList": ["v"],
        "countType": "imageCountMoreThan",
        "countNumber": 10
      },
      "action": {
        "type": "expire"
      }
    },
    {
      "rulePriority": 2,
      "description": "Remove untagged images after 7 days",
      "selection": {
        "tagStatus": "untagged",
        "countType": "sinceImagePushed",
        "countUnit": "days",
        "countNumber": 7
      },
      "action": {
        "type": "expire"
      }
    }
  ]
}
```

```bash
# Apply lifecycle policy
aws ecr put-lifecycle-policy \
  --repository-name bibby \
  --lifecycle-policy-text file://ecr-lifecycle-policy.json
```

### 2.3 Google Container Registry (GCR)

**Best choice for GCP deployments**.

**Setup:**

```bash
# Install gcloud CLI
curl https://sdk.cloud.google.com | bash

# Authenticate
gcloud auth login
gcloud config set project your-project-id

# Configure Docker
gcloud auth configure-docker

# Tag for GCR (format: gcr.io/project-id/image:tag)
docker tag bibby:0.3.0 gcr.io/your-project-id/bibby:0.3.0

# Push
docker push gcr.io/your-project-id/bibby:0.3.0

# Pull (on GCE instance with proper IAM)
docker pull gcr.io/your-project-id/bibby:0.3.0
```

**GCR Regions:**

- `gcr.io` - US multi-region
- `us.gcr.io` - US
- `eu.gcr.io` - Europe
- `asia.gcr.io` - Asia

**Note**: Google is migrating to **Artifact Registry** (recommended for new projects).

### 2.4 Harbor (Self-Hosted)

**Open-source private registry** with advanced features:

- Vulnerability scanning (Trivy integration)
- Image signing (Notary)
- Replication across registries
- RBAC (role-based access control)
- Audit logging
- Helm chart repository

**Install with Docker Compose:**

```bash
# Download Harbor installer
wget https://github.com/goharbor/harbor/releases/download/v2.10.0/harbor-offline-installer-v2.10.0.tgz
tar xvf harbor-offline-installer-v2.10.0.tgz
cd harbor

# Configure
cp harbor.yml.tmpl harbor.yml
nano harbor.yml
```

**harbor.yml:**

```yaml
hostname: registry.example.com

http:
  port: 80

https:
  port: 443
  certificate: /data/cert/server.crt
  private_key: /data/cert/server.key

harbor_admin_password: YourSecurePassword123

database:
  password: DbPassword123

data_volume: /data

trivy:
  ignore_unfixed: false
  skip_update: false
```

**Install:**

```bash
sudo ./install.sh --with-trivy --with-chartmuseum
```

**Access**: https://registry.example.com (admin / YourSecurePassword123)

**Push to Harbor:**

```bash
# Login
docker login registry.example.com
# Username: admin
# Password: YourSecurePassword123

# Create project "bibby" in Harbor UI

# Tag
docker tag bibby:0.3.0 registry.example.com/bibby/bibby:0.3.0

# Push
docker push registry.example.com/bibby/bibby:0.3.0

# Harbor automatically scans for vulnerabilities (Trivy)
```

### 2.5 Registry Comparison

| Feature | Docker Hub | AWS ECR | GCR | Harbor |
|---------|-----------|---------|-----|--------|
| **Cost** | Free (limited) | $0.10/GB | $0.10/GB | Infrastructure |
| **Rate limits** | Yes (100-200/6h) | No | No | No |
| **Scanning** | Basic | Yes (Clair) | Yes | Yes (Trivy) |
| **Private** | $5/month | Included | Included | Included |
| **Signing** | No | No | Yes (Binary Auth) | Yes (Notary) |
| **Replication** | No | Cross-region | Cross-region | Multi-registry |
| **RBAC** | Basic | IAM | IAM | Advanced |
| **Audit logs** | No | CloudTrail | Cloud Audit | Yes |
| **Best for** | Public images | AWS | GCP | Multi-cloud |

**Recommendation for Bibby Production**: AWS ECR (integrated with ECS/EKS, automatic scanning).

---

## 3. Image Tagging Strategies

### 3.1 Why Tagging Matters

**Bad Tagging:**

```bash
docker tag bibby:latest yourusername/bibby:latest
docker push yourusername/bibby:latest

# Production
docker pull yourusername/bibby:latest
docker run yourusername/bibby:latest
```

**Problem**: `latest` is mutable. You can't reproduce deployments!

```bash
# Today: bibby:latest = version 0.3.0
docker pull yourusername/bibby:latest  # Gets 0.3.0

# Tomorrow: Push 0.4.0 as latest
docker tag bibby:0.4.0 yourusername/bibby:latest
docker push yourusername/bibby:latest

# Production pulls again
docker pull yourusername/bibby:latest  # Gets 0.4.0 (DIFFERENT!)
```

**Good Tagging**: Immutable, semantic, traceable.

### 3.2 Semantic Versioning Tags

**Format**: `MAJOR.MINOR.PATCH` (from Section 4)

```bash
# Bibby version 0.3.0
docker tag bibby:0.3.0 yourusername/bibby:0.3.0    # Immutable: specific version
docker tag bibby:0.3.0 yourusername/bibby:0.3      # Mutable: latest 0.3.x
docker tag bibby:0.3.0 yourusername/bibby:0        # Mutable: latest 0.x.x
docker tag bibby:0.3.0 yourusername/bibby:latest   # Mutable: latest version

# Push all tags
docker push yourusername/bibby:0.3.0
docker push yourusername/bibby:0.3
docker push yourusername/bibby:0
docker push yourusername/bibby:latest
```

**Usage:**

```bash
# Production: Use immutable tag
docker pull yourusername/bibby:0.3.0  # Always gets exact version

# Development: Use latest
docker pull yourusername/bibby:latest  # Gets newest version

# Conservative upgrade: Pin minor version
docker pull yourusername/bibby:0.3    # Gets latest 0.3.x (0.3.1, 0.3.2, etc.)
```

### 3.3 Git-Based Tags

**Tag with Git commit SHA:**

```bash
# Get current commit
GIT_SHA=$(git rev-parse --short HEAD)
echo $GIT_SHA
# a1b2c3d

# Tag with commit
docker tag bibby:0.3.0 yourusername/bibby:git-a1b2c3d
docker push yourusername/bibby:git-a1b2c3d

# Production: Exact traceability
docker pull yourusername/bibby:git-a1b2c3d
# Can trace back to exact Git commit
```

**Tag with Git branch:**

```bash
# Current branch
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo $GIT_BRANCH
# feature/user-auth

# Tag
docker tag bibby:0.3.0 yourusername/bibby:feature-user-auth
docker push yourusername/bibby:feature-user-auth

# Deploy feature branch for testing
docker run yourusername/bibby:feature-user-auth
```

### 3.4 Environment Tags

**Tag by environment:**

```bash
# Development
docker tag bibby:0.3.0 yourusername/bibby:dev
docker push yourusername/bibby:dev

# Staging
docker tag bibby:0.3.0 yourusername/bibby:staging
docker push yourusername/bibby:staging

# Production
docker tag bibby:0.3.0 yourusername/bibby:production
docker push yourusername/bibby:production
```

**Problem**: Mutable tags don't show version!

**Better**: Combine environment + version:

```bash
docker tag bibby:0.3.0 yourusername/bibby:0.3.0-dev
docker tag bibby:0.3.0 yourusername/bibby:0.3.0-staging
docker tag bibby:0.3.0 yourusername/bibby:0.3.0-production
```

### 3.5 Date-Based Tags

**Include build date:**

```bash
BUILD_DATE=$(date +%Y%m%d)
echo $BUILD_DATE
# 20250117

docker tag bibby:0.3.0 yourusername/bibby:0.3.0-20250117
docker push yourusername/bibby:0.3.0-20250117
```

**Use case**: Multiple builds per day, need to distinguish.

### 3.6 Complete Tagging Strategy for Bibby

**CI/CD builds multiple tags:**

```bash
#!/bin/bash
# build-and-push.sh

VERSION="0.3.0"
GIT_SHA=$(git rev-parse --short HEAD)
BUILD_DATE=$(date +%Y%m%d-%H%M%S)
REGISTRY="yourusername"

# Build
docker build -t bibby:${VERSION} .

# Tag strategy
docker tag bibby:${VERSION} ${REGISTRY}/bibby:${VERSION}                    # Immutable: 0.3.0
docker tag bibby:${VERSION} ${REGISTRY}/bibby:${VERSION}-${GIT_SHA}         # With commit: 0.3.0-a1b2c3d
docker tag bibby:${VERSION} ${REGISTRY}/bibby:${VERSION}-${BUILD_DATE}      # With date: 0.3.0-20250117-103045
docker tag bibby:${VERSION} ${REGISTRY}/bibby:${VERSION%%.*}                # Major: 0
docker tag bibby:${VERSION} ${REGISTRY}/bibby:${VERSION%.*}                 # Minor: 0.3
docker tag bibby:${VERSION} ${REGISTRY}/bibby:latest                        # Latest

# Push all
docker push ${REGISTRY}/bibby:${VERSION}
docker push ${REGISTRY}/bibby:${VERSION}-${GIT_SHA}
docker push ${REGISTRY}/bibby:${VERSION}-${BUILD_DATE}
docker push ${REGISTRY}/bibby:${VERSION%%.*}
docker push ${REGISTRY}/bibby:${VERSION%.*}
docker push ${REGISTRY}/bibby:latest

echo "Pushed tags:"
echo "  - ${REGISTRY}/bibby:${VERSION} (immutable)"
echo "  - ${REGISTRY}/bibby:${VERSION}-${GIT_SHA} (traceable)"
echo "  - ${REGISTRY}/bibby:${VERSION}-${BUILD_DATE} (timestamped)"
echo "  - ${REGISTRY}/bibby:latest (development)"
```

**Production deployment uses immutable tag:**

```bash
# Kubernetes deployment.yaml
spec:
  containers:
  - name: bibby
    image: yourusername/bibby:0.3.0  # ✅ Immutable, reproducible
    # NOT: yourusername/bibby:latest  # ❌ Mutable, unpredictable
```

### 3.7 Tag Naming Conventions

**Best Practices:**

| Convention | Example | Use Case |
|------------|---------|----------|
| Semantic version | `0.3.0` | Production (immutable) |
| Semantic + commit | `0.3.0-a1b2c3d` | Traceability |
| Semantic + date | `0.3.0-20250117` | Multiple builds/day |
| Git branch | `feature-auth` | Feature testing |
| Environment + version | `0.3.0-production` | Environment tracking |
| Latest | `latest` | Development only |
| RC (release candidate) | `0.3.0-rc1` | Pre-release testing |
| Nightly | `nightly` | Daily builds |

**Avoid:**

- Random strings: `bibby:abc123`
- Unclear names: `bibby:new-version`
- Dates without version: `bibby:20250117`
- Overusing `latest` in production

---

## 4. Image Scanning and Vulnerability Management

### 4.1 Why Scan Images?

**Security risks in container images:**

1. **Base image vulnerabilities**: OpenSSL CVE, kernel vulnerabilities
2. **Application dependencies**: Log4Shell (CVE-2021-44228), Spring4Shell
3. **Malware**: Cryptominers, backdoors
4. **Secrets**: Leaked API keys, passwords
5. **Misconfigurations**: Running as root, exposed ports

**Example**: Bibby uses `eclipse-temurin:17-jre-alpine`. What vulnerabilities exist?

### 4.2 Trivy - Comprehensive Scanner

**Trivy** (Aqua Security) scans for:
- OS vulnerabilities (Alpine, Debian, Ubuntu, etc.)
- Application dependencies (Java, Node, Python, etc.)
- Secrets in layers
- Misconfigurations

**Install Trivy:**

```bash
# Linux
wget https://github.com/aquasecurity/trivy/releases/download/v0.48.0/trivy_0.48.0_Linux-64bit.tar.gz
tar zxvf trivy_0.48.0_Linux-64bit.tar.gz
sudo mv trivy /usr/local/bin/

# macOS
brew install trivy

# Verify
trivy --version
# Version: 0.48.0
```

**Scan Bibby Image:**

```bash
trivy image bibby:0.3.0
```

**Output:**

```
bibby:0.3.0 (alpine 3.19.0)
===========================
Total: 5 (UNKNOWN: 0, LOW: 2, MEDIUM: 2, HIGH: 1, CRITICAL: 0)

┌───────────────┬────────────────┬──────────┬───────────────────┬───────────────┬────────────────────────────────┐
│   Library     │ Vulnerability  │ Severity │ Installed Version │ Fixed Version │            Title               │
├───────────────┼────────────────┼──────────┼───────────────────┼───────────────┼────────────────────────────────┤
│ openssl       │ CVE-2023-5678  │ HIGH     │ 3.0.12-r0         │ 3.0.12-r1     │ OpenSSL: Buffer overflow in... │
│ libcrypto3    │ CVE-2023-5678  │ HIGH     │ 3.0.12-r0         │ 3.0.12-r1     │ OpenSSL: Buffer overflow in... │
│ ca-certificates│ CVE-2023-1234  │ MEDIUM   │ 20230506-r0       │ 20230506-r1   │ CA bundle outdated             │
└───────────────┴────────────────┴──────────┴───────────────────┴───────────────┴────────────────────────────────┘

Java (jar)
==========
Total: 12 (UNKNOWN: 0, LOW: 4, MEDIUM: 5, HIGH: 3, CRITICAL: 0)

┌────────────────────────────────┬────────────────┬──────────┬───────────────────┬───────────────┬─────────────────┐
│          Library               │ Vulnerability  │ Severity │ Installed Version │ Fixed Version │      Title      │
├────────────────────────────────┼────────────────┼──────────┼───────────────────┼───────────────┼─────────────────┤
│ org.springframework:spring-web │ CVE-2024-1234  │ HIGH     │ 6.1.1             │ 6.1.3         │ Spring: HTTP... │
│ org.yaml:snakeyaml             │ CVE-2023-5678  │ MEDIUM   │ 2.0               │ 2.2           │ SnakeYAML: Arb..│
└────────────────────────────────┴────────────────┴──────────┴───────────────────┴───────────────┴─────────────────┘
```

**Interpretation:**

- **5 OS vulnerabilities**: Mostly in Alpine packages
- **12 Java vulnerabilities**: Spring Boot dependencies
- **1 HIGH severity**: OpenSSL buffer overflow
- **Fix**: Rebuild base image with updated Alpine, upgrade Spring Boot

**Scan Specific Severity:**

```bash
# Only HIGH and CRITICAL
trivy image --severity HIGH,CRITICAL bibby:0.3.0

# Exit code 1 if vulnerabilities found (CI/CD)
trivy image --exit-code 1 --severity CRITICAL bibby:0.3.0
```

**Scan with JSON Output:**

```bash
trivy image --format json --output trivy-report.json bibby:0.3.0

# Parse with jq
cat trivy-report.json | jq '.Results[].Vulnerabilities[] | select(.Severity=="HIGH")'
```

### 4.3 Fix Vulnerabilities in Bibby

**Update Base Image:**

```dockerfile
# Before: Alpine 3.19.0
FROM eclipse-temurin:17-jre-alpine AS runtime

# After: Latest Alpine patch
FROM eclipse-temurin:17-jre-alpine@sha256:abc123...  # Pin to specific digest
```

**Update Dependencies in pom.xml:**

```xml
<!-- Before: Spring Boot 3.5.7 with vulnerabilities -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.7</version>
</parent>

<!-- After: Updated to 3.5.10 (patches CVE-2024-1234) -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.10</version>
</parent>
```

**Rebuild and Rescan:**

```bash
docker build -t bibby:0.3.1 .
trivy image bibby:0.3.1

# Total: 0 (UNKNOWN: 0, LOW: 0, MEDIUM: 0, HIGH: 0, CRITICAL: 0)
# ✅ All vulnerabilities fixed!
```

### 4.4 AWS ECR Image Scanning

**Enable scanning on push:**

```bash
aws ecr put-image-scanning-configuration \
  --repository-name bibby \
  --image-scanning-configuration scanOnPush=true \
  --region us-east-1
```

**Push and auto-scan:**

```bash
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:0.3.0

# ECR automatically scans (uses Clair)
```

**View scan results:**

```bash
aws ecr describe-image-scan-findings \
  --repository-name bibby \
  --image-id imageTag=0.3.0 \
  --region us-east-1

# Output:
{
  "imageScanFindings": {
    "findings": [
      {
        "name": "CVE-2023-5678",
        "severity": "HIGH",
        "uri": "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-5678"
      }
    ],
    "findingSeverityCounts": {
      "HIGH": 1,
      "MEDIUM": 3,
      "LOW": 5
    }
  }
}
```

**EventBridge Integration:**

Create alert when HIGH/CRITICAL vulnerabilities found:

```json
{
  "source": ["aws.ecr"],
  "detail-type": ["ECR Image Scan"],
  "detail": {
    "finding-severity-counts": {
      "CRITICAL": [{"exists": true}],
      "HIGH": [{"exists": true}]
    }
  }
}
```

Trigger: SNS → Email, Slack, PagerDuty

### 4.5 Continuous Scanning

**Problem**: New vulnerabilities discovered daily. Image scanned yesterday might be vulnerable today.

**Solution**: Continuous scanning.

**Harbor Continuous Scan:**

```yaml
# harbor.yml
scanner:
  trivy:
    update_interval: 12h  # Update CVE database every 12 hours

# UI: Projects → bibby → Scanner → Schedule: Daily at 2 AM
```

**AWS ECR Enhanced Scanning:**

```bash
# Enable continuous scanning (Inspector integration)
aws ecr put-registry-scanning-configuration \
  --scan-type ENHANCED \
  --rules '[{"repositoryFilters":[{"filter":"*","filterType":"WILDCARD"}],"scanFrequency":"CONTINUOUS_SCAN"}]'
```

**Costs**: $0.09 per image scan (first 30 free/month)

### 4.6 CI/CD Integration

**GitHub Actions with Trivy:**

```yaml
# .github/workflows/scan.yml
name: Container Security Scan

on:
  push:
    branches: [main]
  pull_request:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build image
        run: docker build -t bibby:${{ github.sha }} .

      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: bibby:${{ github.sha }}
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'  # Fail build if vulnerabilities found

      - name: Upload Trivy results to GitHub Security
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: 'trivy-results.sarif'
```

**Result**: Pull requests blocked if HIGH/CRITICAL vulnerabilities detected.

---

## 5. CI/CD Integration

### 5.1 Complete GitHub Actions Workflow

**Build, scan, push to ECR, deploy to ECS:**

```yaml
# .github/workflows/deploy.yml
name: Build and Deploy

on:
  push:
    branches: [main]
    tags: ['v*']

env:
  AWS_REGION: us-east-1
  ECR_REPOSITORY: bibby
  ECS_SERVICE: bibby-service
  ECS_CLUSTER: bibby-cluster

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    permissions:
      id-token: write  # For AWS OIDC
      contents: read
      security-events: write  # For Trivy SARIF upload

    steps:
      # 1. Checkout code
      - name: Checkout
        uses: actions/checkout@v4

      # 2. Set up version
      - name: Extract version
        id: version
        run: |
          if [[ $GITHUB_REF == refs/tags/v* ]]; then
            VERSION=${GITHUB_REF#refs/tags/v}
          else
            VERSION=$(cat pom.xml | grep -m1 '<version>' | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
          fi
          echo "version=$VERSION" >> $GITHUB_OUTPUT
          echo "git_sha=$(git rev-parse --short HEAD)" >> $GITHUB_OUTPUT
          echo "build_date=$(date +%Y%m%d-%H%M%S)" >> $GITHUB_OUTPUT

      # 3. Configure AWS credentials
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/GitHubActionsRole
          aws-region: ${{ env.AWS_REGION }}

      # 4. Login to ECR
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      # 5. Build image
      - name: Build Docker image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          VERSION: ${{ steps.version.outputs.version }}
          GIT_SHA: ${{ steps.version.outputs.git_sha }}
        run: |
          docker build \
            --build-arg VERSION=$VERSION \
            --build-arg GIT_SHA=$GIT_SHA \
            --tag $ECR_REGISTRY/$ECR_REPOSITORY:$VERSION \
            --tag $ECR_REGISTRY/$ECR_REPOSITORY:$VERSION-$GIT_SHA \
            --tag $ECR_REGISTRY/$ECR_REPOSITORY:latest \
            .

      # 6. Scan image with Trivy
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:${{ steps.version.outputs.version }}
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'  # Fail if vulnerabilities found

      - name: Upload Trivy results
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: 'trivy-results.sarif'

      # 7. Push to ECR
      - name: Push image to Amazon ECR
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          VERSION: ${{ steps.version.outputs.version }}
          GIT_SHA: ${{ steps.version.outputs.git_sha }}
        run: |
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$VERSION
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$VERSION-$GIT_SHA
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest

      # 8. Update ECS task definition
      - name: Download task definition
        run: |
          aws ecs describe-task-definition \
            --task-definition bibby \
            --query taskDefinition \
            > task-definition.json

      - name: Fill in new image ID in task definition
        id: task-def
        uses: aws-actions/amazon-ecs-render-task-definition@v1
        with:
          task-definition: task-definition.json
          container-name: bibby
          image: ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:${{ steps.version.outputs.version }}

      # 9. Deploy to ECS
      - name: Deploy to Amazon ECS
        uses: aws-actions/amazon-ecs-deploy-task-definition@v1
        with:
          task-definition: ${{ steps.task-def.outputs.task-definition }}
          service: ${{ env.ECS_SERVICE }}
          cluster: ${{ env.ECS_CLUSTER }}
          wait-for-service-stability: true

      # 10. Create GitHub Release (for tags)
      - name: Create Release
        if: startsWith(github.ref, 'refs/tags/v')
        uses: softprops/action-gh-release@v1
        with:
          name: Release ${{ steps.version.outputs.version }}
          body: |
            ## Docker Images
            - `${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:${{ steps.version.outputs.version }}`
            - `${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:${{ steps.version.outputs.version }}-${{ steps.version.outputs.git_sha }}`
          generate_release_notes: true
```

**Workflow:**

1. Push to `main` or create tag `v0.3.0`
2. Extract version from tag or `pom.xml`
3. Authenticate to AWS (OIDC)
4. Build Docker image with multiple tags
5. Scan with Trivy (fail if HIGH/CRITICAL found)
6. Push to ECR
7. Update ECS task definition
8. Deploy to ECS
9. Create GitHub Release (for tags)

### 5.2 Image Promotion Workflow

**Dev → Staging → Production**

```yaml
# .github/workflows/promote.yml
name: Promote Image

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version to promote'
        required: true
      target_env:
        description: 'Target environment'
        required: true
        type: choice
        options:
          - staging
          - production

jobs:
  promote:
    runs-on: ubuntu-latest
    steps:
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/GitHubActionsRole
          aws-region: us-east-1

      - name: Login to ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Pull dev image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
        run: |
          docker pull $ECR_REGISTRY/bibby:${{ inputs.version }}

      - name: Tag for target environment
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
        run: |
          docker tag $ECR_REGISTRY/bibby:${{ inputs.version }} \
            $ECR_REGISTRY/bibby:${{ inputs.version }}-${{ inputs.target_env }}

      - name: Push promoted image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
        run: |
          docker push $ECR_REGISTRY/bibby:${{ inputs.version }}-${{ inputs.target_env }}

      - name: Deploy to ${{ inputs.target_env }}
        run: |
          aws ecs update-service \
            --cluster bibby-${{ inputs.target_env }} \
            --service bibby-service \
            --force-new-deployment
```

**Usage:**

1. Build `0.3.0` → Tagged as `0.3.0`, `0.3.0-dev`
2. Test in dev environment
3. Promote: Manually trigger workflow with `version=0.3.0`, `target_env=staging`
4. Image re-tagged as `0.3.0-staging`
5. Deploy to staging
6. After testing, promote to `0.3.0-production`

---

## 6. Multi-Architecture Images

### 6.1 Why Multi-Arch?

**Problem**: x86_64 image won't run on ARM (Apple M1, AWS Graviton, Raspberry Pi).

**Solution**: Build images for multiple architectures, package in single manifest.

```bash
docker pull bibby:0.3.0
# On Intel Mac: Pulls linux/amd64 image
# On M1 Mac: Pulls linux/arm64 image
# Same tag, different binaries
```

### 6.2 Build Multi-Arch with Buildx

**Setup:**

```bash
# Create builder
docker buildx create --name multiarch --use --bootstrap

# Verify
docker buildx ls
# NAME/NODE    DRIVER/ENDPOINT   STATUS    PLATFORMS
# multiarch*   docker-container  running   linux/amd64, linux/arm64, linux/arm/v7
```

**Build for AMD64 and ARM64:**

```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --tag yourusername/bibby:0.3.0 \
  --tag yourusername/bibby:latest \
  --push \
  .
```

**Verify Manifest:**

```bash
docker buildx imagetools inspect yourusername/bibby:0.3.0
```

**Output:**

```
Name:      docker.io/yourusername/bibby:0.3.0
MediaType: application/vnd.docker.distribution.manifest.list.v2+json
Digest:    sha256:a1b2c3d4e5f6...

Manifests:
  Name:      docker.io/yourusername/bibby:0.3.0@sha256:b2c3d4e5f6a1...
  MediaType: application/vnd.docker.distribution.manifest.v2+json
  Platform:  linux/amd64
  Size:      192MB

  Name:      docker.io/yourusername/bibby:0.3.0@sha256:c3d4e5f6a1b2...
  MediaType: application/vnd.docker.distribution.manifest.v2+json
  Platform:  linux/arm64
  Size:      189MB
```

**Pull Automatically Selects Correct Architecture:**

```bash
# On x86_64 machine
docker pull yourusername/bibby:0.3.0
# Pulls linux/amd64 (192MB)

# On ARM64 machine (M1 Mac, Graviton)
docker pull yourusername/bibby:0.3.0
# Pulls linux/arm64 (189MB)
```

### 6.3 GitHub Actions Multi-Arch Build

```yaml
- name: Set up QEMU
  uses: docker/setup-qemu-action@v3

- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v3

- name: Build and push multi-arch
  uses: docker/build-push-action@v5
  with:
    context: .
    platforms: linux/amd64,linux/arm64
    push: true
    tags: |
      yourusername/bibby:0.3.0
      yourusername/bibby:latest
    cache-from: type=registry,ref=yourusername/bibby:buildcache
    cache-to: type=registry,ref=yourusername/bibby:buildcache,mode=max
```

---

## 7. Interview-Ready Knowledge

### Question 1: "Explain your image tagging and registry strategy."

**Answer:**

"For our Bibby library management application, we use a comprehensive tagging strategy that balances immutability, traceability, and convenience.

**Registry Choice:**

We use AWS ECR for production because:
1. No rate limits like Docker Hub
2. Integrated with our ECS infrastructure (same AWS region = faster pulls)
3. Automatic vulnerability scanning with findings in AWS Security Hub
4. IAM-based access control
5. Lifecycle policies for automatic cleanup

**Tagging Strategy:**

Every build creates multiple tags:

```bash
# Immutable tags (production deployments use these)
123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:0.3.0
123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:0.3.0-a1b2c3d

# Mutable tags (development/convenience)
123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:0.3
123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:latest
```

The semantic version tag (0.3.0) is immutable—once pushed, it never changes. This ensures reproducibility: if production runs 0.3.0 today and we redeploy next week, it's the exact same image.

We also tag with the Git commit SHA (0.3.0-a1b2c3d) for complete traceability. If we discover a bug in production, we can trace back to the exact commit that built that image.

**Promotion Workflow:**

Images are promoted through environments:

1. Build creates `0.3.0` and `0.3.0-dev`
2. After dev testing, we retag as `0.3.0-staging`
3. After staging validation, we retag as `0.3.0-production`

The same image (identical SHA256 digest) flows through all environments—we never rebuild between environments. This eliminates 'it works in staging but fails in prod' issues.

**Lifecycle Management:**

We use ECR lifecycle policies to:
- Keep last 10 production-tagged images
- Delete untagged images after 7 days (intermediate build layers)
- Keep all images tagged with semantic versions indefinitely

This keeps our registry clean while preserving history for rollbacks."

### Question 2: "How do you handle vulnerability scanning in your CI/CD pipeline?"

**Answer:**

"We implement multi-layer vulnerability scanning for our Bibby container images.

**Pre-Deployment Scanning:**

In our GitHub Actions CI/CD pipeline, we use Trivy to scan every image before it reaches ECR:

```yaml
- name: Scan with Trivy
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: bibby:${{ github.sha }}
    severity: 'CRITICAL,HIGH'
    exit-code: '1'  # Block merge if vulnerabilities found
```

This runs on every pull request and blocks merges if CRITICAL or HIGH vulnerabilities are detected. The scan results are uploaded to GitHub Security tab using SARIF format, so developers see vulnerabilities directly in the PR.

**Registry-Level Scanning:**

We enable ECR scan-on-push, which automatically scans every image pushed to the registry:

```bash
aws ecr put-image-scanning-configuration \
  --repository-name bibby \
  --image-scanning-configuration scanOnPush=true
```

ECR uses Clair as its scanning engine and integrates with AWS Security Hub. We've set up EventBridge rules to send Slack notifications when HIGH or CRITICAL vulnerabilities are found.

**Continuous Monitoring:**

We recently upgraded to ECR Enhanced Scanning, which provides continuous scanning:

```bash
aws ecr put-registry-scanning-configuration \
  --scan-type ENHANCED \
  --rules '[{\"scanFrequency\":\"CONTINUOUS_SCAN\"}]'
```

This is critical because new CVEs are published daily. An image that was clean yesterday might have a CRITICAL vulnerability today. Enhanced scanning rescans our images continuously and alerts us through AWS Inspector.

**Remediation Process:**

When vulnerabilities are detected:

1. **Assess severity**: Review CVE details and exploitability
2. **Check patch availability**: Is there a fixed version of the base image or dependency?
3. **Update dependencies**: For Bibby, this usually means:
   - Updating Spring Boot version in pom.xml
   - Rebuilding with latest Alpine base image
4. **Rebuild and rescan**: Verify fixes with Trivy locally
5. **Deploy**: Push new version through dev → staging → production

For example, when Log4Shell (CVE-2021-44228) was disclosed, our scanning caught it within hours. We updated Spring Boot, rebuilt, and deployed the patched version to production within 24 hours.

**Exceptions:**

Sometimes vulnerabilities don't apply to our use case (e.g., a web server vulnerability when we don't use that component). We document exceptions in a `.trivyignore` file with justifications:

```
# CVE-2023-1234: curl vulnerability
# Not exploitable - we don't use curl in the application
CVE-2023-1234
```

This is reviewed quarterly to ensure exceptions are still valid."

### Question 3: "Describe your multi-environment deployment strategy using container registries."

**Answer:**

"We use a single ECR repository with environment-based tagging to manage deployments across dev, staging, and production.

**Registry Structure:**

All environments pull from the same ECR repository (123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby), but use different tags:

```
bibby:0.3.0               # Immutable version tag
bibby:0.3.0-a1b2c3d       # Git commit tag
bibby:0.3.0-dev           # Dev environment
bibby:0.3.0-staging       # Staging environment
bibby:0.3.0-production    # Production environment
```

**Deployment Flow:**

1. **Build**: CI builds on main branch or tag push
   ```bash
   docker build -t 123...amazonaws.com/bibby:0.3.0 .
   docker tag bibby:0.3.0 123...amazonaws.com/bibby:0.3.0-dev
   docker push 123...amazonaws.com/bibby:0.3.0-dev
   ```

2. **Dev Deployment**: Automatic deployment to dev ECS cluster
   ```bash
   aws ecs update-service \
     --cluster bibby-dev \
     --service bibby \
     --force-new-deployment
   ```

3. **Promotion to Staging**: Manual approval via GitHub Actions workflow
   ```bash
   # Same image, new tag
   docker tag 123...amazonaws.com/bibby:0.3.0 \
     123...amazonaws.com/bibby:0.3.0-staging
   docker push 123...amazonaws.com/bibby:0.3.0-staging
   ```

4. **Staging Deployment**: Triggers ECS update
   - Automated integration tests run
   - Load testing with staging database
   - Manual QA validation

5. **Production Promotion**: Requires:
   - Staging tests pass
   - Security scan clean
   - Manual approval from team lead

   ```bash
   docker tag 123...amazonaws.com/bibby:0.3.0 \
     123...amazonaws.com/bibby:0.3.0-production
   docker push 123...amazonaws.com/bibby:0.3.0-production
   ```

6. **Production Deployment**: Blue-green deployment via ECS
   - New task definition with `0.3.0-production` tag
   - ECS launches new tasks alongside old tasks
   - Health checks pass → traffic shifted via ALB
   - Old tasks drained and terminated

**Key Advantages:**

1. **Immutability**: The `0.3.0` tag never changes; we're deploying the exact binary that was tested in dev and staging

2. **Traceability**: We can see which version is in each environment:
   ```bash
   aws ecs describe-services --cluster bibby-dev --services bibby \
     | jq -r '.services[0].taskDefinition'
   # Uses image bibby:0.3.0-dev

   aws ecs describe-services --cluster bibby-prod --services bibby \
     | jq -r '.services[0].taskDefinition'
   # Uses image bibby:0.2.1-production (still on previous version)
   ```

3. **Rollback**: Simply redeploy previous production tag:
   ```bash
   # Rollback to 0.2.1
   aws ecs update-service --cluster bibby-prod \
     --service bibby \
     --task-definition bibby:0.2.1-production
   ```

**IAM Isolation:**

Each environment has its own IAM role with ECR permissions:

- Dev: Can pull any tag
- Staging: Can pull `*-dev` and `*-staging` tags
- Production: Can only pull `*-production` tags

This prevents accidentally deploying an unvetted image to production.

**Monitoring:**

We track image deployment events in CloudWatch Events and correlate with application metrics in Datadog. If deployment of `0.3.0-production` correlates with error rate increase, we know exactly which image to roll back to."

---

## Summary

**What You Learned:**

1. **Docker Hub**
   - Public registry with free tier (1 private repo)
   - Access tokens for secure authentication
   - Rate limits: 100-200 pulls/6 hours
   - Tagging format: `username/repository:tag`
   - Push/pull workflow

2. **Private Registries**
   - **AWS ECR**: $0.10/GB, IAM integration, automatic scanning
   - **GCR/Artifact Registry**: Google Cloud, similar pricing
   - **Harbor**: Self-hosted, Trivy scanning, image signing
   - Comparison table with features and costs

3. **Image Tagging Strategies**
   - Semantic versioning: `0.3.0` (immutable)
   - Git-based: `0.3.0-a1b2c3d` (traceable)
   - Environment: `0.3.0-production` (deployment tracking)
   - Complete tagging script for Bibby
   - Best practices and conventions

4. **Vulnerability Scanning**
   - Trivy: Comprehensive scanner (OS + application deps)
   - AWS ECR scanning: Automatic scan-on-push with Clair
   - Enhanced scanning: Continuous monitoring with Inspector
   - CI/CD integration: Block builds on HIGH/CRITICAL
   - Remediation workflow

5. **CI/CD Integration**
   - Complete GitHub Actions workflow
   - Build → Scan → Push → Deploy pipeline
   - Multi-environment promotion (dev → staging → prod)
   - Image promotion with manual approvals
   - ECS deployment integration

6. **Multi-Architecture Images**
   - Building for AMD64 and ARM64
   - Docker Buildx setup
   - Manifest lists for automatic selection
   - GitHub Actions multi-arch builds
   - Support for Apple Silicon and AWS Graviton

7. **Best Practices**
   - Immutable tags for production
   - Lifecycle policies for cleanup
   - IAM-based access control
   - Continuous vulnerability scanning
   - Image promotion workflows
   - Traceability with Git SHAs

**Production-Ready Bibby Registry Setup:**

- Registry: AWS ECR (`123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby`)
- Tags: `0.3.0`, `0.3.0-a1b2c3d`, `0.3.0-production`
- Scanning: Enhanced continuous scanning
- Lifecycle: Keep last 10 production images
- Multi-arch: AMD64 + ARM64 support
- CI/CD: Automated build, scan, deploy pipeline

**Key Metrics:**
- Image size: 192MB (AMD64), 189MB (ARM64)
- Scan time: ~30 seconds
- Push time: ~45 seconds (to ECR)
- Pull time: ~15 seconds (from ECR to ECS)

**Interview-Ready Answers:**
- Registry and tagging strategy (immutability + traceability)
- Vulnerability scanning pipeline (Trivy + ECR + continuous)
- Multi-environment deployment (promotion workflow)

**Progress**: 19 of 28 sections complete (68%)

**Next**: Section 20 will cover Docker Compose for Local Development (multi-service orchestration, development workflows, debugging multi-container apps, and production-like local environments).

**Key Takeaway:**

Docker registries are the central nervous system of your container infrastructure. Proper tagging, scanning, and promotion workflows are the difference between "we push containers sometimes" and "we have a robust, secure, auditable deployment pipeline."
