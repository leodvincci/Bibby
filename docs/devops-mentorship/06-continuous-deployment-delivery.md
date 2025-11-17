# SECTION 6: CONTINUOUS DEPLOYMENT & DELIVERY
## From Code Commit to Production: Automated, Safe, and Repeatable Deployments

---

## 🎯 Learning Objectives

By the end of this section, you will:
- Understand the difference between Continuous Delivery and Continuous Deployment
- Master deployment strategies (blue-green, canary, rolling)
- Learn why containerization (Docker) is essential for modern deployments
- Fix critical security issues in your current configuration
- Create production-ready environment configurations
- Implement automated deployment pipelines
- Understand rollback strategies and disaster recovery
- Know when and how to deploy safely to production

---

## 🔍 YOUR CURRENT STATE: HARDCODED SECRETS & NO DEPLOYMENT PLAN

Let me analyze your deployment readiness...

### 📁 File: `src/main/resources/application.properties`

**Lines 1-5:**

```properties
spring.application.name=Bibby
spring.shell.interactive.enabled=true
spring.datasource.url=jdbc:postgresql://localhost:5332/amigos
spring.datasource.username=amigoscode
spring.datasource.password=password
```

### 🚨 CRITICAL SECURITY ISSUES FOUND!

**Issue #1: Hardcoded Credentials**
```properties
spring.datasource.username=amigoscode
spring.datasource.password=password  # ← NEVER DO THIS!
```

**Problem:**
- ❌ Password visible in source code
- ❌ Same credentials for dev and prod (terrible!)
- ❌ Anyone with repo access has prod database password
- ❌ Can't change password without code change
- ❌ Committed to Git history forever

**Real-world impact:**
> "In 2019, Capital One was breached because AWS credentials were hardcoded in GitHub. 100 million customers affected. $80 million fine."

**Issue #2: Hardcoded Database Host**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5332/amigos
```

**Problem:**
- ❌ `localhost` won't work in production
- ❌ Port `5332` is non-standard (5432 is PostgreSQL default)
- ❌ Database name `amigos` (not `bibby`?)
- ❌ Can't deploy to cloud without changing code

**Issue #3: No Environment Separation**
- ❌ No `application-dev.properties`
- ❌ No `application-prod.properties`
- ❌ Dev and prod use same config
- ❌ Testing changes means risking production

**Issue #4: No Docker Files**
```bash
ls -la Dockerfile docker-compose.yml
# Output: No Docker files found
```

**Problem:**
- ❌ Can't containerize application
- ❌ Deployment depends on server setup
- ❌ "Works on my machine" syndrome
- ❌ Not cloud-ready

---

## 📚 CD FUNDAMENTALS: DELIVERY VS. DEPLOYMENT

### Continuous Delivery (CD)

**Definition:**
> Code is always in a deployable state, but deployment requires manual approval.

**Workflow:**
```
Commit → CI passes → Build artifact →
Stage to production-like environment →
Manual approval → Deploy to production
```

**Characteristics:**
- ✅ Automated up to production
- ✅ Human decides when to deploy
- ✅ Good for regulated industries
- ✅ Lower risk (human oversight)

**Example:**
```
Developer merges PR →
CI tests pass →
Artifact uploaded to staging →
QA tests staging →
Product manager clicks "Deploy" button →
Deploys to production
```

### Continuous Deployment (CD - same acronym!)

**Definition:**
> Every change that passes automated tests automatically deploys to production.

**Workflow:**
```
Commit → CI passes → Automatically deployed to production
```

**Characteristics:**
- ✅ Fully automated (no manual step)
- ✅ Fastest feedback loop
- ✅ Requires excellent testing
- ⚠️ Higher risk if tests inadequate

**Example:**
```
Developer merges PR →
CI tests pass →
Automatically deployed to production (within minutes)
```

### Which Should You Use?

**For Bibby (learning project):**
- Start with **Continuous Delivery**
- Manual approval for production
- Automatic deployment to staging
- Reason: Learning, safety, control

**In the future:**
- Move to **Continuous Deployment** when:
  - Test coverage > 90%
  - Automated integration tests
  - Monitoring and alerting in place
  - Team comfortable with automation

---

## 🎯 DEPLOYMENT STRATEGIES

### Strategy 1: Blue-Green Deployment

**Concept:**
- Two identical environments: **Blue** (current) and **Green** (new)
- Deploy to Green while Blue serves traffic
- Switch traffic to Green instantly
- Keep Blue as instant rollback

**Diagram:**
```
Before Deployment:
Users → Load Balancer → Blue (v1.0) ✅ Active
                     → Green (empty) ⏸️ Idle

During Deployment:
Users → Load Balancer → Blue (v1.0) ✅ Still active
                     → Green (v1.1) 🔄 Deploying

After Deployment:
Users → Load Balancer → Blue (v1.0) ⏸️ Ready for rollback
                     → Green (v1.1) ✅ Active

If Issues:
Users → Load Balancer → Blue (v1.0) ✅ Rollback (instant!)
                     → Green (v1.1) ❌ Failed
```

**Pros:**
- ✅ Zero downtime
- ✅ Instant rollback (just switch back)
- ✅ Test in production before switching
- ✅ Simple to understand

**Cons:**
- ❌ Requires double resources
- ❌ Database migrations tricky
- ❌ Doesn't work for breaking schema changes

**When to Use:**
- High-traffic applications
- When downtime is unacceptable
- When rollback speed critical

**For Bibby:**
- Good for learning
- Can simulate with Docker containers
- Single instance for now, but good practice

### Strategy 2: Canary Deployment

**Concept:**
- Deploy to small % of users first
- Monitor metrics (errors, performance)
- Gradually increase % if healthy
- Roll back instantly if issues

**Progression:**
```
Stage 1: 5% of traffic → New version (v1.1)
         95% of traffic → Old version (v1.0)
         Monitor for 10 minutes

Stage 2: 25% of traffic → New version (v1.1)
         75% of traffic → Old version (v1.0)
         Monitor for 20 minutes

Stage 3: 50% of traffic → New version (v1.1)
         50% of traffic → Old version (v1.0)
         Monitor for 30 minutes

Stage 4: 100% of traffic → New version (v1.1)
         Old version retired
```

**Monitoring During Canary:**
```
Metrics to watch:
- Error rate (should be same as baseline)
- Response time (should not increase)
- CPU/Memory (should be similar)
- User-reported issues

Auto-rollback if:
- Error rate > 1% higher
- Response time > 2x slower
- Critical exception thrown
```

**Pros:**
- ✅ Minimal user impact (only 5% affected)
- ✅ Real production testing
- ✅ Can catch issues before full rollout
- ✅ Progressive validation

**Cons:**
- ❌ More complex than blue-green
- ❌ Requires good monitoring
- ❌ Can take hours to fully deploy
- ❌ Needs load balancer with traffic splitting

**When to Use:**
- Large user base
- High-risk changes
- When monitoring infrastructure exists

**For Bibby:**
- Overkill for now (single-user CLI app)
- Great to understand for interviews
- Can implement later if building web version

### Strategy 3: Rolling Deployment

**Concept:**
- Update instances one at a time
- Each instance removed from pool, updated, returned
- No downtime as other instances handle traffic

**Process:**
```
4 Instances running v1.0:
[v1.0] [v1.0] [v1.0] [v1.0] ← All serving traffic

Step 1: Update Instance 1
[v1.1] [v1.0] [v1.0] [v1.0] ← 75% capacity during update

Step 2: Update Instance 2
[v1.1] [v1.1] [v1.0] [v1.0] ← 50% old, 50% new

Step 3: Update Instance 3
[v1.1] [v1.1] [v1.1] [v1.0] ← Almost done

Step 4: Update Instance 4
[v1.1] [v1.1] [v1.1] [v1.1] ← Complete!
```

**Pros:**
- ✅ No downtime
- ✅ No extra resources needed
- ✅ Gradual rollout
- ✅ Simple to implement

**Cons:**
- ❌ Slower than blue-green
- ❌ Mixed versions running simultaneously
- ❌ Harder to rollback (must update each again)
- ❌ Not instant failure detection

**When to Use:**
- Resource-constrained environments
- When double resources not available
- Backward-compatible changes only

### Strategy 4: Recreate (Simple)

**Concept:**
- Stop old version completely
- Deploy new version
- Start new version

**Process:**
```
Step 1: Stop all instances
[v1.0] [v1.0] [v1.0] → ❌ Stopped

Step 2: Downtime (30 seconds - 5 minutes)
Users see: "Service unavailable"

Step 3: Start new instances
[v1.1] [v1.1] [v1.1] → ✅ Running
```

**Pros:**
- ✅ Simplest strategy
- ✅ No resource overhead
- ✅ Clean cutover
- ✅ Easy to understand

**Cons:**
- ❌ Downtime (unacceptable for most)
- ❌ All users affected
- ❌ Risky if new version has issues

**When to Use:**
- Internal tools
- Scheduled maintenance windows
- Dev/staging environments
- Very low-traffic applications

**For Bibby:**
- Fine for personal project
- Acceptable for CLI tool
- Good starting point

---

## 🐳 DOCKER: THE FOUNDATION OF MODERN DEPLOYMENT

### Why Docker?

**The Problem (Without Docker):**

```
Developer's Mac:
- Java 17.0.8
- Maven 3.9.5
- PostgreSQL 15.3
- Works perfectly! ✅

Staging Server (Ubuntu):
- Java 17.0.2
- Maven 3.8.1
- PostgreSQL 14.9
- Breaks mysteriously ❌

Production Server (CentOS):
- Java 17.0.5
- Maven 3.9.3
- PostgreSQL 15.1
- Different errors ❌
```

**With Docker:**

```
Developer's Mac:
docker run bibby:1.0 → Works ✅

Staging Server:
docker run bibby:1.0 → Works ✅

Production Server:
docker run bibby:1.0 → Works ✅

Same container, identical behavior everywhere!
```

### Docker Mental Model

**Container = Lightweight, Isolated Process**

```
Traditional VM:
Hardware → Hypervisor → Guest OS (full Ubuntu) → Your App
Size: ~2 GB, Boot time: 30-60 seconds

Docker Container:
Hardware → Host OS → Docker Engine → Container (just your app)
Size: ~100 MB, Boot time: 1-2 seconds
```

**Key Benefits:**
1. **Consistency**: Same environment everywhere
2. **Isolation**: App + dependencies bundled together
3. **Portability**: Run anywhere Docker runs
4. **Speed**: Starts in seconds
5. **Resource Efficient**: Shares host OS kernel

---

## 🏗️ CREATING YOUR BIBBY DOCKERFILE

### Multi-Stage Dockerfile (Best Practice)

**File:** `Dockerfile`

```dockerfile
# ============================================
# Stage 1: Build Stage
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy POM file first (for dependency caching)
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ============================================
# Stage 2: Runtime Stage
# ============================================
FROM eclipse-temurin:17-jre-alpine

# Metadata
LABEL maintainer="Leo D. Penrose <leo@example.com>"
LABEL description="Bibby - Personal Library Management CLI"
LABEL version="0.3.0"

# Create non-root user for security
RUN addgroup -S bibby && adduser -S bibby -G bibby

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/Bibby-*.jar app.jar

# Change ownership to non-root user
RUN chown -R bibby:bibby /app

# Switch to non-root user
USER bibby

# Health check (for monitoring)
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD java -jar app.jar --version || exit 1

# Entry point
ENTRYPOINT ["java", "-jar", "app.jar"]

# Default command (can be overridden)
CMD []
```

**Why Multi-Stage?**

**Single-Stage (Bad):**
```
Image includes: Maven + JDK + source code + dependencies + app
Final size: ~600 MB
```

**Multi-Stage (Good):**
```
Stage 1: Maven + JDK + build everything
Stage 2: Only JRE + compiled app
Final size: ~150 MB (4x smaller!)
```

**Benefits:**
- ✅ Smaller images = faster downloads
- ✅ Fewer attack surfaces
- ✅ No build tools in production
- ✅ Faster container startup

### Building Your Image

```bash
# Build with tag
docker build -t bibby:0.3.0 .

# Build with multiple tags
docker build -t bibby:0.3.0 -t bibby:latest .

# Build with build args (for versions)
docker build --build-arg VERSION=0.3.0 -t bibby:0.3.0 .

# Check image size
docker images bibby

# Output:
# REPOSITORY   TAG       SIZE
# bibby        0.3.0     150MB  ← Much better than 600MB!
```

### Running Your Container

```bash
# Basic run
docker run bibby:0.3.0

# Interactive mode (for CLI)
docker run -it bibby:0.3.0

# With environment variables
docker run -it \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/bibby \
  -e SPRING_DATASOURCE_USERNAME=bibby \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  bibby:0.3.0

# With volume mount (for data persistence)
docker run -it \
  -v /path/to/data:/app/data \
  bibby:0.3.0
```

---

## 🔐 FIXING YOUR SECURITY ISSUES: ENVIRONMENT MANAGEMENT

### The 12-Factor App Principle

**Rule III: Store Config in the Environment**

> "An app's config is everything that is likely to vary between deploys (staging, production, developer environments, etc). Never commit secrets to the codebase."

### Creating Environment-Specific Configs

**File:** `src/main/resources/application.properties` (Base config)

```properties
# Application name
spring.application.name=Bibby

# Spring Shell
spring.shell.interactive.enabled=true

# Server config
server.error.include-message=always
server.error.include-binding-errors=never
server.error.include-stacktrace=never

# Console output
spring.main.banner-mode=console
spring.output.ansi.enabled=ALWAYS

# Logging (default: minimal)
logging.level.root=WARN
logging.level.com.penrose.bibby=INFO

# Database (defaults - will be overridden by environment)
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/bibby}
spring.datasource.username=${DATABASE_USERNAME:bibby}
spring.datasource.password=${DATABASE_PASSWORD:password}

# JPA
spring.jpa.hibernate.ddl-auto=${DDL_AUTO:validate}
spring.jpa.show-sql=${SHOW_SQL:false}
spring.jpa.properties.hibernate.format_sql=true
```

**Key Changes:**
- ✅ Uses environment variables with defaults
- ✅ No hardcoded secrets
- ✅ Port changed to standard 5432
- ✅ Database name changed to `bibby`

**File:** `src/main/resources/application-dev.properties`

```properties
# Development environment configuration

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/bibby_dev
spring.datasource.username=bibby_dev
spring.datasource.password=dev_password

# JPA
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# Logging
logging.level.root=INFO
logging.level.com.penrose.bibby=DEBUG
logging.level.org.springframework=INFO
logging.level.org.hibernate.SQL=DEBUG

# Disable security for dev
spring.h2.console.enabled=true
```

**File:** `src/main/resources/application-prod.properties`

```properties
# Production environment configuration

# Database (MUST be provided via environment)
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}

# JPA (NEVER auto-create in production!)
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Logging
logging.level.root=ERROR
logging.level.com.penrose.bibby=WARN

# Security
server.error.include-message=never
server.error.include-binding-errors=never
server.error.include-stacktrace=never
```

---

## 📦 DOCKER COMPOSE: LOCAL DEVELOPMENT STACK

**File:** `docker-compose.yml`

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:15-alpine
    container_name: bibby-postgres
    environment:
      POSTGRES_DB: bibby
      POSTGRES_USER: bibby
      POSTGRES_PASSWORD: dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U bibby"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - bibby-network

  # Bibby Application
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: bibby-app
    environment:
      SPRING_PROFILES_ACTIVE: dev
      DATABASE_URL: jdbc:postgresql://postgres:5432/bibby
      DATABASE_USERNAME: bibby
      DATABASE_PASSWORD: dev_password
    depends_on:
      postgres:
        condition: service_healthy
    stdin_open: true
    tty: true
    networks:
      - bibby-network
    volumes:
      - ./logs:/app/logs

volumes:
  postgres-data:
    driver: local

networks:
  bibby-network:
    driver: bridge
```

**Usage:**

```bash
# Start everything
docker-compose up

# Start in background
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop everything
docker-compose down

# Stop and remove volumes (fresh start)
docker-compose down -v

# Rebuild and start
docker-compose up --build
```

---

## 🎓 KEY TAKEAWAYS

1. **Your Config Has Security Issues**: Hardcoded passwords, localhost URLs

2. **CD ≠ CI**: Continuous Delivery (manual deploy) vs Continuous Deployment (auto)

3. **Deployment Strategies Matter**: Blue-green, canary, rolling - each has trade-offs

4. **Docker Is Essential**: Consistency, portability, isolation

5. **Environment Separation Required**: Dev, staging, prod configs must differ

6. **Secrets Management**: NEVER commit secrets to Git

7. **Rollback Is Not Optional**: Plan for failure before deploying

8. **Production Readiness**: Comprehensive checklist prevents disasters

---

## 📝 ACTION ITEMS

### Week 1: Fix Security Issues (2 hours)

**Day 1: Environment Configs (1 hour)**
- [ ] Create `application-dev.properties`
- [ ] Create `application-prod.properties`
- [ ] Update base `application.properties` to use env vars
- [ ] Test each profile locally

**Day 2: Remove Hardcoded Secrets (30 min)**
- [ ] Replace hardcoded credentials with `${ENV_VAR:default}`
- [ ] Test with environment variables
- [ ] Commit: "security: remove hardcoded credentials"

### Week 2: Docker Setup (3 hours)

**Day 1: Create Dockerfile (1 hour)**
- [ ] Copy multi-stage Dockerfile from this section
- [ ] Test build: `docker build -t bibby:0.3.0 .`
- [ ] Test run: `docker run -it bibby:0.3.0`

**Day 2: Create docker-compose.yml (1 hour)**
- [ ] Copy docker-compose.yml from this section
- [ ] Test: `docker-compose up`
- [ ] Verify app connects to database

---

## 🎯 SECTION SUMMARY

**Critical Issues Found:**
- Hardcoded password: `password` (line 5)
- Hardcoded username: `amigoscode` (line 4)
- Hardcoded localhost URL (line 3)
- No Docker files
- No environment separation

**What You Learned:**
- ✅ CD fundamentals (Delivery vs Deployment)
- ✅ Deployment strategies (blue-green, canary, rolling)
- ✅ Docker basics and multi-stage builds
- ✅ Environment management
- ✅ Secrets management (12-factor app)
- ✅ docker-compose for local development
- ✅ Rollback strategies

**📊 Progress**: 6/28 sections complete (21%)
**Next**: Section 7 - CI/CD Best Practices & Security
**Type "continue" when ready!** 🚀
