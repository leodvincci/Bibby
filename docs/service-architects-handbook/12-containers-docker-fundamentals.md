# Section 12: Containers & Docker Fundamentals

**Part IV: Infrastructure & Deployment**

---

## "It Works on My Machine" Is Not a Deployment Strategy

**2018. Production outage. Root cause:** Developer's laptop had Java 11. Production server had Java 8. Code used Java 11 APIs. **ClassNotFoundException.**

**2019. Staging passes. Production fails.** Root cause: Staging PostgreSQL version 12. Production PostgreSQL version 10. Query used syntax added in v12.

**2020. Works in CI. Crashes in production.** Root cause: CI had 8GB RAM. Production had 4GB. Application OOM'd under load.

**The pattern:** Environment differences cause 70% of "it worked in dev" failures.

**The solution:** Containers. Package your app **and its entire runtime environment** into a single immutable artifact.

In this section, I'll containerize Bibby from scratch and show you why containers are the foundation of modern microservices.

---

## What Is a Container?

**Simple mental model:** A container is a **lightweight virtual machine** without the VM.

**Traditional VM:**
```
┌─────────────────────────────────┐
│  Application                    │
│  ├─ Java 17                     │
│  ├─ Libraries                   │
│  └─ Spring Boot 3.5.7          │
├─────────────────────────────────┤
│  Guest OS (Ubuntu 22.04)        │  ← Full OS (500MB+)
├─────────────────────────────────┤
│  Hypervisor (VMware, VirtualBox)│
├─────────────────────────────────┤
│  Host OS (macOS, Windows)       │
└─────────────────────────────────┘
```

**Container:**
```
┌─────────────────────────────────┐
│  Application                    │
│  ├─ Java 17                     │
│  ├─ Libraries                   │
│  └─ Spring Boot 3.5.7          │
├─────────────────────────────────┤
│  Container Runtime (Docker)     │  ← No guest OS!
├─────────────────────────────────┤
│  Host OS (Linux kernel)         │
└─────────────────────────────────┘
```

**Key difference:** Containers share the host OS kernel. VMs emulate entire machines.

**Benefits:**
- ✅ **Fast startup:** Containers start in milliseconds (VMs take minutes)
- ✅ **Small size:** 50-200MB containers vs 5-20GB VMs
- ✅ **Density:** Run 100 containers on same hardware as 10 VMs
- ✅ **Consistency:** Same image runs on dev laptop, CI, staging, production

**Trade-off:** Containers share kernel. If you need different kernel versions or OS families (Linux app on Windows), use VMs.

---

## Docker Basics

**Docker** is the most popular container runtime (90%+ market share).

**Core concepts:**

### 1. Image
**Definition:** Read-only template containing app + dependencies.

**Analogy:** A class in OOP. Defines what the container will be.

**Example:** `bibby:1.0` image contains Java 17 + Bibby JAR + PostgreSQL JDBC driver.

### 2. Container
**Definition:** Running instance of an image.

**Analogy:** An object in OOP. Instantiated from a class.

**Example:** `docker run bibby:1.0` creates a container from the image.

### 3. Dockerfile
**Definition:** Recipe for building an image.

**Analogy:** Build script. Defines how to assemble the image.

### 4. Registry
**Definition:** Repository for storing images.

**Examples:** Docker Hub (public), AWS ECR (private), Google Artifact Registry.

**Workflow:**
```
Developer writes Dockerfile
    ↓
docker build → creates Image
    ↓
docker push → uploads to Registry
    ↓
Production server pulls image from Registry
    ↓
docker run → creates Container
```

---

## Containerizing Bibby: The Naive Way

From `pom.xml:11-14, 30`:

```xml
<groupId>com.penrose</groupId>
<artifactId>Bibby</artifactId>
<version>0.0.1-SNAPSHOT</version>
...
<java.version>17</java.version>
```

**Naive Dockerfile:**

```dockerfile
# ❌ DON'T DO THIS
FROM ubuntu:22.04

# Install Java
RUN apt-get update && apt-get install -y openjdk-17-jdk maven

# Copy source code
COPY . /app
WORKDIR /app

# Build application
RUN mvn clean package

# Run application
CMD ["java", "-jar", "target/Bibby-0.0.1-SNAPSHOT.jar"]
```

**Problems:**

1. **Huge image size:** Ubuntu base (77MB) + JDK (300MB) + Maven (180MB) + dependencies = **800MB+**
2. **Slow builds:** Every code change rebuilds Maven dependencies (5 min)
3. **Security:** Running as root, contains build tools in production
4. **Caching:** Maven repo downloaded every build

**Better approach:** Multi-stage builds.

---

## Containerizing Bibby: The Right Way

### Multi-Stage Dockerfile

**Concept:** Use one image to build, another (smaller) image to run.

```dockerfile
# ────────────────────────────────────────────────────────
# Stage 1: Build the application (heavy image)
# ────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy POM first (for layer caching)
COPY pom.xml .

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build JAR
RUN mvn clean package -DskipTests

# ────────────────────────────────────────────────────────
# Stage 2: Run the application (lightweight image)
# ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S bibby && adduser -S bibby -G bibby

# Copy JAR from builder stage
COPY --from=builder /build/target/Bibby-0.0.1-SNAPSHOT.jar app.jar

# Change ownership
RUN chown -R bibby:bibby /app

# Switch to non-root user
USER bibby

# Expose port (if Bibby had REST APIs)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD java -cp app.jar org.springframework.boot.loader.JarLauncher --spring.main.web-application-type=none || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Let's break this down:**

### Layer Caching Explained

Docker builds images in **layers**. Each `RUN`, `COPY`, `ADD` instruction creates a layer.

**If a layer hasn't changed, Docker reuses the cached layer.**

```
Layer 1: FROM maven:3.9-eclipse-temurin-17   ← Cached (base image unchanged)
Layer 2: COPY pom.xml                         ← Cached (pom.xml unchanged)
Layer 3: RUN mvn dependency:go-offline        ← Cached (dependencies unchanged)
Layer 4: COPY src                             ← CHANGED (code modified)
Layer 5: RUN mvn clean package                ← Rebuild (depends on Layer 4)
```

**Result:** Changing Java code doesn't re-download Maven dependencies. **Build time: 5 min → 30 sec.**

### Why Alpine Linux?

**Standard JRE image:** `eclipse-temurin:17-jre` = 286MB
**Alpine-based JRE:** `eclipse-temurin:17-jre-alpine` = 189MB

**Alpine** is a minimal Linux distro (5MB base). Perfect for containers.

**Trade-off:** Uses musl libc instead of glibc. Rarely causes issues, but some native libraries might not work.

### Security: Non-Root User

**Problem:** Default container runs as root (UID 0). If attacker breaks out, they have root on host.

**Solution:** Create and use a non-privileged user.

```dockerfile
RUN addgroup -S bibby && adduser -S bibby -G bibby
USER bibby
```

**Now:** Container process runs as UID 1001 (not root).

---

## .dockerignore File

**Purpose:** Exclude files from Docker build context (faster uploads).

**Bibby's .dockerignore:**

```
# Version control
.git
.gitignore

# Build artifacts
target/
*.class
*.jar
*.war

# IDE files
.idea/
.vscode/
*.iml
.classpath
.project
.settings/

# OS files
.DS_Store
Thumbs.db

# Documentation
docs/
README.md

# Test reports
**/test-results/
**/coverage/

# Logs
*.log
logs/
```

**Why this matters:**

Without `.dockerignore`, Docker sends entire project (including `target/` with 100MB of build artifacts) to Docker daemon.

**With `.dockerignore`:**
- Build context: 500MB → 5MB
- Upload time: 30s → 1s

---

## Building and Running Bibby Container

### Build Image

```bash
# Build image
docker build -t bibby:1.0 .

# Output:
[+] Building 45.3s (15/15) FINISHED
 => [internal] load build definition from Dockerfile
 => [internal] load .dockerignore
 => [stage-1 2/6] COPY pom.xml .
 => [stage-1 3/6] RUN mvn dependency:go-offline          ← Downloaded dependencies
 => [stage-1 4/6] COPY src ./src
 => [stage-1 5/6] RUN mvn clean package                  ← Built JAR
 => [stage-2 3/6] COPY --from=builder /build/target...   ← Copied JAR to runtime image
 => exporting to image
 => => naming to docker.io/library/bibby:1.0
```

### Inspect Image

```bash
docker images bibby:1.0

# Output:
REPOSITORY   TAG   IMAGE ID       CREATED          SIZE
bibby        1.0   a1b2c3d4e5f6   10 seconds ago   245MB
```

**245MB total** (189MB JRE + 56MB JAR + dependencies).

Compare to VM: 5GB+. **20x smaller.**

### Run Container

```bash
# Run Bibby container
docker run -it --rm \
  --name bibby-app \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/amigos \
  -e SPRING_DATASOURCE_USERNAME=amigoscode \
  -e SPRING_DATASOURCE_PASSWORD=password \
  bibby:1.0

# Flags explained:
# -it: Interactive terminal (for Spring Shell CLI)
# --rm: Remove container when stopped (cleanup)
# --name bibby-app: Name the container
# -e: Set environment variables (override application.properties)
```

**Problem:** Bibby expects PostgreSQL at `localhost:5332`. Container network is isolated.

**Solution:** Docker Compose.

---

## Docker Compose: Multi-Container Applications

**Problem:** Bibby needs PostgreSQL. Managing two containers manually is tedious.

**Docker Compose:** Define multi-container applications in YAML.

### docker-compose.yml for Bibby

```yaml
version: '3.8'

services:
  # PostgreSQL database
  postgres:
    image: postgres:15-alpine
    container_name: bibby-postgres
    environment:
      POSTGRES_DB: amigos
      POSTGRES_USER: amigoscode
      POSTGRES_PASSWORD: password
    ports:
      - "5332:5432"  # Map host 5332 to container 5432
    volumes:
      - postgres-data:/var/lib/postgresql/data  # Persist data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U amigoscode"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - bibby-network

  # Bibby application
  bibby-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: bibby-app
    depends_on:
      postgres:
        condition: service_healthy  # Wait for DB to be ready
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/amigos
      SPRING_DATASOURCE_USERNAME: amigoscode
      SPRING_DATASOURCE_PASSWORD: password
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
    stdin_open: true  # Keep STDIN open for Spring Shell
    tty: true         # Allocate pseudo-TTY
    networks:
      - bibby-network

volumes:
  postgres-data:  # Named volume for database persistence

networks:
  bibby-network:
    driver: bridge
```

### Docker Compose Commands

```bash
# Start all services
docker-compose up

# Start in background (detached mode)
docker-compose up -d

# View logs
docker-compose logs -f bibby-app

# Stop all services
docker-compose down

# Stop and remove volumes (delete DB data)
docker-compose down -v

# Rebuild images
docker-compose build

# Rebuild and start
docker-compose up --build
```

**Network magic:** Containers can reach each other by service name. `postgres:5432` resolves to PostgreSQL container's IP.

**Volume persistence:** Even if you `docker-compose down`, database data survives in `postgres-data` volume.

---

## Container Registries

**Problem:** You've built `bibby:1.0` on your laptop. How does production get it?

**Solution:** Push to a registry.

### Docker Hub (Public)

```bash
# Login
docker login

# Tag image (username/repository:tag)
docker tag bibby:1.0 yourname/bibby:1.0

# Push
docker push yourname/bibby:1.0

# Pull from anywhere
docker pull yourname/bibby:1.0
```

**Free tier:** Unlimited public repos, 1 private repo.

### AWS Elastic Container Registry (ECR)

```bash
# Authenticate
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Create repository
aws ecr create-repository --repository-name bibby

# Tag
docker tag bibby:1.0 123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:1.0

# Push
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:1.0
```

**Benefits:**
- ✅ Private by default
- ✅ IAM integration (role-based access)
- ✅ Automated scanning for vulnerabilities
- ✅ Lifecycle policies (auto-delete old images)

**Cost:** $0.10/GB per month for storage.

### Google Artifact Registry

```bash
# Authenticate
gcloud auth configure-docker us-central1-docker.pkg.dev

# Tag
docker tag bibby:1.0 us-central1-docker.pkg.dev/my-project/bibby/bibby:1.0

# Push
docker push us-central1-docker.pkg.dev/my-project/bibby/bibby:1.0
```

**Benefits:** Similar to ECR, integrates with GCP IAM.

---

## Image Tagging Strategies

**Anti-pattern:** Only using `latest` tag.

```bash
docker build -t bibby:latest .
docker push bibby:latest

# 6 months later: What version is in production?
# Answer: No idea. "latest" has been overwritten 200 times.
```

**Best practices:**

### 1. Semantic Versioning

```bash
docker build -t bibby:1.2.3 .
docker tag bibby:1.2.3 bibby:1.2
docker tag bibby:1.2.3 bibby:1
docker tag bibby:1.2.3 bibby:latest

docker push bibby:1.2.3  # Specific version
docker push bibby:1.2    # Minor version
docker push bibby:1      # Major version
docker push bibby:latest # Latest stable
```

**Result:**
- `bibby:1.2.3` → Pinned version (production)
- `bibby:1.2` → Receives patch updates
- `bibby:latest` → Always newest

### 2. Git Commit SHA

```bash
GIT_SHA=$(git rev-parse --short HEAD)
docker build -t bibby:$GIT_SHA .
docker push bibby:$GIT_SHA

# Production: bibby:a3f19c2
# Exact code version deployed
```

**Benefit:** Traceability. Know exactly which commit is running.

### 3. Build Number + SHA

```bash
docker build -t bibby:build-456-a3f19c2 .
```

**Benefit:** Combines CI build number with code SHA.

---

## Security Scanning

**Problem:** Your base image contains vulnerabilities. You don't know.

**2021 example:** Log4Shell vulnerability (CVE-2021-44228) affected millions of Java containers.

### Docker Scout (Built-in)

```bash
# Scan image
docker scout cves bibby:1.0

# Output:
┌────────────────────────────────────────────────────────────┐
│ Target: bibby:1.0                                          │
├────────────────────────────────────────────────────────────┤
│   Critical: 0                                              │
│   High: 2   ← ⚠️ HIGH SEVERITY VULNERABILITIES             │
│   Medium: 15                                               │
│   Low: 32                                                  │
└────────────────────────────────────────────────────────────┘

HIGH: CVE-2023-1234 in libssl
  Affected package: libssl1.1 (version 1.1.1n-1)
  Fixed in: libssl1.1 (version 1.1.1q-1)
  Recommendation: Update base image to eclipse-temurin:17-jre-alpine (version 17.0.9)
```

### Trivy (Open Source)

```bash
# Install Trivy
brew install trivy  # macOS
apt-get install trivy  # Ubuntu

# Scan image
trivy image bibby:1.0

# Output includes:
# - CVE details
# - Severity (CRITICAL, HIGH, MEDIUM, LOW)
# - Fixed version (if available)
# - Links to CVE database
```

### Automated Scanning in CI

```yaml
# GitHub Actions example
name: Build and Scan

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build Docker image
        run: docker build -t bibby:${{ github.sha }} .

      - name: Scan with Trivy
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: bibby:${{ github.sha }}
          severity: 'CRITICAL,HIGH'
          exit-code: 1  # Fail build if vulnerabilities found
```

**Result:** Pull request blocked if critical CVEs detected.

---

## Image Size Optimization

**Why size matters:**
- **Pull time:** 1GB image takes 2 min to pull on slow networks
- **Storage cost:** AWS ECR charges $0.10/GB/month
- **Attack surface:** Smaller image = fewer packages = fewer vulnerabilities

### Technique 1: Use Distroless Images

**Distroless:** Minimal images containing only your app + runtime (no shell, no package manager).

```dockerfile
# Before: Alpine-based (189MB)
FROM eclipse-temurin:17-jre-alpine
# Contains: JRE + busybox + package manager + 30+ utilities

# After: Distroless (165MB)
FROM gcr.io/distroless/java17-debian11
# Contains: JRE only. No shell, no apt, no nothing.
```

**Benefit:** 24MB smaller, much smaller attack surface.

**Trade-off:** Can't `docker exec` into container (no shell). Debugging harder.

### Technique 2: Optimize Layers

**Bad:**
```dockerfile
RUN apt-get update
RUN apt-get install -y curl
RUN apt-get install -y vim
# 3 layers, each with full package index
```

**Good:**
```dockerfile
RUN apt-get update && \
    apt-get install -y curl vim && \
    rm -rf /var/lib/apt/lists/*
# 1 layer, cleaned up package cache
```

### Technique 3: Use .dockerignore Aggressively

**Without .dockerignore:**
```
COPY . /app
# Copies: src/ (5MB) + target/ (100MB) + .git/ (200MB) + docs/ (50MB)
# Layer size: 355MB
```

**With .dockerignore:**
```
COPY . /app
# Copies: src/ (5MB)
# Layer size: 5MB
```

**Result:** 350MB saved per build.

---

## Multi-Architecture Images

**Problem:** You build on M1 Mac (ARM64). Production runs on x86_64. **Image doesn't work.**

**Solution:** Build multi-architecture images.

```bash
# Enable buildx (multi-platform builder)
docker buildx create --use

# Build for both architectures
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t bibby:1.0 \
  --push \
  .
```

**Docker automatically pulls the right architecture:**
```bash
# On x86_64 server: pulls linux/amd64 image
# On ARM64 server: pulls linux/arm64 image
docker pull bibby:1.0
```

**Behind the scenes:** Docker manifest contains pointers to both images.

---

## Real-World War Story: npm Left-Pad (2016)

**Not Docker-specific, but illustrates why containers matter.**

**Incident:** Developer unpublished `left-pad` package from npm (11 lines of code). Thousands of builds broke worldwide.

**Root cause:** Builds pulled dependencies from npm at build time. Dependency disappeared. Builds failed.

**Container solution:** Multi-stage builds **lock dependencies** at image build time.

```dockerfile
# Install dependencies at build time
RUN mvn dependency:go-offline

# Build image
RUN mvn package

# Runtime image contains ONLY the built JAR
# No dependency on external repos at runtime
```

**Result:** Even if Maven Central goes down, your container still runs (it has everything baked in).

---

## Anti-Patterns to Avoid

### 1. Storing Secrets in Images

**❌ DON'T DO THIS:**
```dockerfile
ENV DB_PASSWORD=supersecret123  # ← Stored in image layer!
```

**Anyone can extract:**
```bash
docker history bibby:1.0
# IMAGE          CREATED BY                       SIZE
# a1b2c3d4e5f6  ENV DB_PASSWORD=supersecret123   0B  ← Exposed!
```

**✅ DO THIS:**
```bash
# Pass secrets at runtime
docker run -e DB_PASSWORD=supersecret123 bibby:1.0

# Or use Docker secrets (Swarm) or Kubernetes secrets
```

### 2. Running apt-get update in Production Image

**❌ BAD:**
```dockerfile
FROM ubuntu:22.04
RUN apt-get update && apt-get install -y curl
# Update runs every build → results change over time
```

**✅ GOOD:**
```dockerfile
FROM ubuntu:22.04@sha256:abc123...  # Pin by digest
RUN apt-get update && apt-get install -y curl=7.81.0-1
# Pinned versions → reproducible builds
```

### 3. Using :latest in Production

**❌ NEVER:**
```yaml
# docker-compose.yml
services:
  app:
    image: bibby:latest  # What version is this? Who knows!
```

**✅ ALWAYS:**
```yaml
services:
  app:
    image: bibby:1.2.3  # Explicit version
```

---

## Action Items

**For Bibby:**

1. **Create Dockerfile** (15 min)
   - Copy the multi-stage Dockerfile from this section
   - Place it in project root
   - Test: `docker build -t bibby:dev .`

2. **Create .dockerignore** (5 min)
   - Copy the .dockerignore from this section
   - Verify build context is small: `docker build --progress=plain .`

3. **Create docker-compose.yml** (10 min)
   - Copy the Docker Compose config from this section
   - Test: `docker-compose up`
   - Verify Bibby connects to PostgreSQL

4. **Scan for vulnerabilities** (5 min)
   - Install Trivy: `brew install trivy`
   - Scan: `trivy image bibby:dev`
   - Fix any CRITICAL or HIGH vulnerabilities

**For your project:**

1. **Containerize** - Write Dockerfile (use multi-stage builds)
2. **Measure** - Compare image sizes (dev vs production stage)
3. **Secure** - Run as non-root, scan with Trivy
4. **Compose** - Create docker-compose.yml for local dev
5. **Tag properly** - Use semantic versioning, not just `latest`

---

## Further Reading

- **Docker Official Docs:** https://docs.docker.com/
- **Best Practices:** https://docs.docker.com/develop/dev-best-practices/
- **Multi-Stage Builds:** https://docs.docker.com/build/building/multi-stage/
- **Distroless Images:** https://github.com/GoogleContainerTools/distroless
- **Dive (image analyzer):** https://github.com/wagoodman/dive

---

## Next Section Preview

**Section 13: Orchestration with Kubernetes** will teach you:
- What Kubernetes is and why you need it
- Pods, Services, Deployments, ConfigMaps, Secrets
- Deploying Bibby to Kubernetes
- Scaling applications (horizontal pod autoscaling)
- Rolling updates and rollbacks
- Service discovery and load balancing
- Health checks and self-healing

We'll deploy Bibby to a local Kubernetes cluster (Minikube) and simulate production scenarios.

Ready? Let's orchestrate containers at scale.

---

**Word count:** ~3,500 words
