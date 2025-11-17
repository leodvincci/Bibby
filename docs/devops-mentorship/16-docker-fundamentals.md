# Section 16: Docker Fundamentals & Mental Model

## Introduction: Understanding Containerization

In Section 6, we created a Dockerfile for Bibby. But why Docker? And how does it actually work?

**The Problem Docker Solves:**
- "It works on my machine!" (different environments)
- Complex dependency management
- Slow environment setup
- Resource waste (full VMs for small apps)
- Difficult local testing of production-like environments

**The Docker Solution:**
- Consistent environments (dev, staging, prod identical)
- Isolated dependencies (no conflicts)
- Fast startup (seconds, not minutes)
- Efficient resource usage (share OS kernel)
- Easy local development

**What You'll Learn:**
- Docker's mental model (images, containers, layers)
- Containers vs VMs (technical differences)
- Docker architecture (daemon, client, registry)
- Creating production-ready Dockerfiles
- Multi-stage builds for optimization
- Volumes, networks, and compose
- Real Bibby containerization

**Real-World Context:**
Currently, deploying Bibby requires: Install Java, configure environment, copy JAR, set up systemd. With Docker: `docker run bibby:0.3.0` - done!

---

## 1. Containers vs Virtual Machines

### 1.1 Virtual Machines Architecture

```
┌─────────────────────────────────────────┐
│          Host Operating System          │
├─────────────────────────────────────────┤
│              Hypervisor                 │
├──────────┬──────────┬──────────────────┤
│  VM 1    │  VM 2    │      VM 3        │
│┌────────┐│┌────────┐│    ┌────────┐    │
││ App A  │││ App B  ││    │ App C  │    │
│├────────┤│├────────┤│    ├────────┤    │
││Guest OS│││Guest OS││    │Guest OS│    │
││(Linux) │││(Linux) ││    │(Linux) │    │
│└────────┘│└────────┘│    └────────┘    │
└──────────┴──────────┴──────────────────┘
    500MB     500MB         500MB
```

**Virtual Machine Characteristics:**
- Full OS per VM (kernel, drivers, libraries)
- Heavy (GBs per VM)
- Slow boot (minutes)
- Hardware-level isolation
- Managed by hypervisor (VMware, VirtualBox, KVM)

### 1.2 Containers Architecture

```
┌─────────────────────────────────────────┐
│          Host Operating System          │
├─────────────────────────────────────────┤
│            Docker Daemon                │
├──────────┬──────────┬──────────────────┤
│Container1│Container2│   Container 3    │
│┌────────┐│┌────────┐│    ┌────────┐    │
││ App A  │││ App B  ││    │ App C  │    │
││ + Libs │││ + Libs ││    │ + Libs │    │
│└────────┘│└────────┘│    └────────┘    │
└──────────┴──────────┴──────────────────┘
    50MB      50MB          50MB
```

**Container Characteristics:**
- Shares host OS kernel
- Lightweight (MBs per container)
- Fast boot (seconds)
- Process-level isolation
- Managed by container runtime (Docker, containerd)

### 1.3 Comparison Table

| Aspect | Virtual Machines | Containers |
|--------|------------------|------------|
| **Isolation** | Hardware-level (strong) | Process-level (good) |
| **Startup Time** | Minutes | Seconds |
| **Size** | GBs (OS + app) | MBs (app + libs) |
| **Resource Usage** | High (full OS overhead) | Low (shared kernel) |
| **Portability** | Lower (hypervisor-specific) | Higher (runs anywhere Docker runs) |
| **Use Case** | Running different OSes | Running same-OS apps |
| **Density** | 10-20 VMs per host | 100-1000 containers per host |

**For Bibby:**
- **VM:** 500 MB (Amazon Linux 2 + Java + Bibby)
- **Container:** 180 MB (Alpine + Java + Bibby)

**Key Insight:** Containers are **NOT lightweight VMs**. They're isolated processes sharing the host kernel.

---

## 2. Docker Architecture

### 2.1 Core Components

```
┌──────────────────┐         ┌──────────────────┐
│  Docker Client   │────────▶│  Docker Daemon   │
│   (docker CLI)   │  REST   │   (dockerd)      │
└──────────────────┘   API   └──────────────────┘
                                      │
                      ┌───────────────┼───────────────┐
                      ▼               ▼               ▼
              ┌───────────┐   ┌───────────┐  ┌──────────┐
              │  Images   │   │Containers │  │ Networks │
              └───────────┘   └───────────┘  └──────────┘
                      │
                      ▼
              ┌──────────────┐
              │   Registry   │
              │ (Docker Hub) │
              └──────────────┘
```

**1. Docker Client (`docker` command):**
- CLI tool you use
- Sends commands to daemon via REST API
- Can connect to remote daemons

**2. Docker Daemon (`dockerd`):**
- Background service
- Manages images, containers, networks, volumes
- Communicates with containerd (low-level runtime)

**3. Docker Registry:**
- Stores Docker images
- Public: Docker Hub, Amazon ECR, GitHub Container Registry
- Private: Self-hosted registry

### 2.2 How Docker Works

**Example: `docker run nginx`**

```
Step 1: Client sends "run nginx" to daemon
        │
        ▼
Step 2: Daemon checks local images
        ├─ Image exists? → Skip to Step 4
        └─ Image missing? → Continue
        │
        ▼
Step 3: Pull image from registry
        docker pull nginx:latest
        │
        ▼
Step 4: Create container from image
        - Allocate filesystem (union of layers)
        - Create network namespace
        - Set up cgroups (resource limits)
        - Execute ENTRYPOINT/CMD
        │
        ▼
Step 5: Container running!
        nginx process isolated in namespace
```

**Linux Kernel Features Docker Uses:**

**1. Namespaces (Isolation):**
- PID: Process isolation (container sees only its processes)
- NET: Network isolation (own IP, ports)
- MNT: Filesystem isolation
- UTS: Hostname isolation
- IPC: Inter-process communication isolation

**2. cgroups (Resource Limits):**
- CPU limit: `--cpus=1.5`
- Memory limit: `--memory=512m`
- Disk I/O limits

**3. Union Filesystems:**
- Layers stack on top of each other
- Copy-on-write (efficient storage)

---

## 3. Core Docker Concepts

### 3.1 Images

**What is a Docker Image?**
- Template for creating containers
- Read-only
- Made up of layers
- Each layer is a set of filesystem changes

**Image Layers:**

```
┌──────────────────────────────────────┐
│ Layer 5: ENTRYPOINT ["java", ...]   │  5 KB (metadata)
├──────────────────────────────────────┤
│ Layer 4: COPY Bibby-0.3.0.jar       │  30 MB (JAR file)
├──────────────────────────────────────┤
│ Layer 3: RUN apk add openjdk17      │  150 MB (Java)
├──────────────────────────────────────┤
│ Layer 2: RUN apk update              │  5 MB (package index)
├──────────────────────────────────────┤
│ Layer 1: FROM alpine:3.18            │  7 MB (base OS)
└──────────────────────────────────────┘
        Total: ~192 MB
```

**Why Layers Matter:**

```bash
# Build 1: Full download
docker build -t bibby:v1 .
# Downloads: alpine (7MB) + updates (5MB) + Java (150MB) + JAR (30MB) = 192MB

# Build 2: Changed only JAR
# Dockerfile: Updated COPY Bibby-0.3.1.jar
docker build -t bibby:v2 .
# Reuses: Layers 1-3 (cached)
# Downloads: Only layer 4 (30MB) = 30MB

# 85% smaller download!
```

**List Images:**

```bash
docker images

# Output:
REPOSITORY   TAG       IMAGE ID       CREATED         SIZE
bibby        0.3.0     abc123def456   2 hours ago     192MB
postgres     15        def456ghi789   3 days ago      379MB
alpine       3.18      ghi789jkl012   1 week ago      7MB
```

**Image Naming:**

```
registry.example.com:5000/bibby:0.3.0
└──────┬────────┘ └─┬─┘ └─┬──┘└─┬─┘
    Registry      Port  Name  Tag

Examples:
- bibby:latest              (Docker Hub, default registry)
- bibby:0.3.0               (specific version)
- myuser/bibby:latest       (Docker Hub, user repository)
- public.ecr.aws/bibby:0.3.0 (AWS ECR public)
```

### 3.2 Containers

**What is a Container?**
- Running instance of an image
- Writable layer on top of image layers
- Isolated process with own filesystem, network, PID namespace

**Container Lifecycle:**

```
         ┌──────────┐
    ┌───▶│ Created  │
    │    └────┬─────┘
docker      │ docker start
create      ▼
    │    ┌──────────┐
    │    │ Running  │◀─── docker run (create + start)
    │    └────┬─────┘
    │         │ docker stop
    │         ▼
    │    ┌──────────┐
    └────│ Stopped  │
         └────┬─────┘
              │ docker rm
              ▼
         ┌──────────┐
         │ Deleted  │
         └──────────┘
```

**Create and Run Container:**

```bash
# Run container (create + start in one command)
docker run -d \
  --name bibby-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/bibby \
  bibby:0.3.0

# Flags explained:
# -d: Detached mode (run in background)
# --name: Give container a name
# -p 8080:8080: Map host port 8080 to container port 8080
# -e: Set environment variable

# List running containers
docker ps

# Output:
CONTAINER ID   IMAGE         COMMAND                  CREATED         STATUS         PORTS                    NAMES
a1b2c3d4e5f6   bibby:0.3.0   "java -jar Bibby.jar"   30 seconds ago   Up 29 seconds  0.0.0.0:8080->8080/tcp   bibby-app

# List all containers (including stopped)
docker ps -a

# View logs
docker logs bibby-app

# Follow logs (like tail -f)
docker logs -f bibby-app

# Execute command in running container
docker exec -it bibby-app /bin/sh
# Now you're inside the container!

# Stop container
docker stop bibby-app

# Start stopped container
docker start bibby-app

# Restart container
docker restart bibby-app

# Remove container
docker rm bibby-app

# Remove running container (force)
docker rm -f bibby-app
```

### 3.3 Volumes

**Problem:** Container filesystem is ephemeral. When container is deleted, data is lost.

**Solution:** Volumes - persistent storage outside container.

**Three Types of Mounts:**

```
1. Volume (Docker-managed):
   /var/lib/docker/volumes/bibby-data/_data
                              ↓
                    Container: /data

2. Bind Mount (host directory):
   /home/user/bibby/logs
              ↓
   Container: /var/log/bibby

3. tmpfs (memory-only, Linux):
   RAM
    ↓
   Container: /tmp
```

**Using Volumes:**

```bash
# Create named volume
docker volume create bibby-data

# Run container with volume
docker run -d \
  --name bibby-app \
  -v bibby-data:/opt/bibby/data \
  -v /home/user/bibby/logs:/var/log/bibby \
  bibby:0.3.0

# List volumes
docker volume ls

# Inspect volume
docker volume inspect bibby-data

# Output:
[
    {
        "CreatedAt": "2025-01-15T10:30:00Z",
        "Driver": "local",
        "Mountpoint": "/var/lib/docker/volumes/bibby-data/_data",
        "Name": "bibby-data",
        "Scope": "local"
    }
]

# Remove volume
docker volume rm bibby-data

# Remove unused volumes
docker volume prune
```

**For Bibby Database (PostgreSQL):**

```bash
# Run PostgreSQL with persistent volume
docker run -d \
  --name bibby-postgres \
  -e POSTGRES_DB=bibby \
  -e POSTGRES_USER=bibby_admin \
  -e POSTGRES_PASSWORD=secure_password \
  -v bibby-postgres-data:/var/lib/postgresql/data \
  -p 5432:5432 \
  postgres:15-alpine

# Data persists even if container is deleted!
docker rm -f bibby-postgres
docker run -d --name bibby-postgres -v bibby-postgres-data:/var/lib/postgresql/data postgres:15-alpine
# Data still there!
```

### 3.4 Networks

**Default Networks:**

```bash
docker network ls

# Output:
NETWORK ID     NAME      DRIVER    SCOPE
abc123def456   bridge    bridge    local   # Default
def456ghi789   host      host      local
ghi789jkl012   none      null      local
```

**Bridge Network (Default):**
- Containers on same bridge can communicate
- Isolated from host network
- NAT to outside world

**Creating Custom Network:**

```bash
# Create network
docker network create bibby-network

# Run containers on same network
docker run -d --name bibby-postgres --network bibby-network postgres:15-alpine
docker run -d --name bibby-app --network bibby-network bibby:0.3.0

# Containers can reach each other by name!
# Inside bibby-app:
# DATABASE_URL=jdbc:postgresql://bibby-postgres:5432/bibby
```

---

## 4. Dockerfile for Bibby

### 4.1 Basic Dockerfile

**`Dockerfile`:**

```dockerfile
# Stage 1: Base image
FROM eclipse-temurin:17-jre-alpine

# Metadata
LABEL maintainer="your-email@example.com"
LABEL version="0.3.0"
LABEL description="Bibby Library Management Application"

# Create app user (don't run as root!)
RUN addgroup -g 1001 bibby && \
    adduser -D -u 1001 -G bibby bibby

# Set working directory
WORKDIR /opt/bibby

# Copy JAR file
COPY target/Bibby-0.3.0.jar ./Bibby.jar

# Change ownership
RUN chown -R bibby:bibby /opt/bibby

# Switch to non-root user
USER bibby

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Set JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar Bibby.jar"]
```

**Build and Run:**

```bash
# Build image
docker build -t bibby:0.3.0 .

# Output:
[+] Building 45.2s (10/10) FINISHED
 => [1/4] FROM eclipse-temurin:17-jre-alpine@sha256:abc...
 => [2/4] RUN addgroup -g 1001 bibby && adduser -D ...
 => [3/4] COPY target/Bibby-0.3.0.jar ./Bibby.jar
 => [4/4] RUN chown -R bibby:bibby /opt/bibby
 => exporting to image
 => => naming to docker.io/library/bibby:0.3.0

# Run container
docker run -d \
  --name bibby-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/bibby \
  -e DATABASE_USERNAME=bibby_admin \
  -e DATABASE_PASSWORD=secure_password \
  bibby:0.3.0

# Check health
docker inspect --format='{{.State.Health.Status}}' bibby-app
# Output: healthy

# View logs
docker logs bibby-app
```

### 4.2 Multi-Stage Build (Optimized)

**Problem:** Including build tools in production image wastes space.

**Solution:** Build in one stage, copy artifacts to smaller runtime stage.

**`Dockerfile` (Multi-Stage):**

```dockerfile
# ============================================
# Stage 1: Build stage
# ============================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy pom.xml first (for better caching)
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# ============================================
# Stage 2: Runtime stage
# ============================================
FROM eclipse-temurin:17-jre-alpine

# Metadata
LABEL maintainer="your-email@example.com"
LABEL version="0.3.0"
LABEL description="Bibby Library Management Application"

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create app user
RUN addgroup -g 1001 bibby && \
    adduser -D -u 1001 -G bibby bibby

WORKDIR /opt/bibby

# Copy JAR from builder stage
COPY --from=builder /build/target/Bibby-0.3.0.jar ./Bibby.jar

# Change ownership
RUN chown -R bibby:bibby /opt/bibby

# Switch to non-root user
USER bibby

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar Bibby.jar"]
```

**Size Comparison:**

```bash
# Single-stage (with Maven)
docker build -f Dockerfile.single -t bibby:single .
docker images bibby:single
# bibby:single   680 MB (includes Maven, JDK)

# Multi-stage (runtime only)
docker build -f Dockerfile.multi -t bibby:multi .
docker images bibby:multi
# bibby:multi    192 MB (only JRE + app)

# Savings: 72% smaller!
```

### 4.3 Dockerfile Best Practices

**1. Use Specific Tags (Not `latest`):**

```dockerfile
# ❌ BAD: Version can change
FROM eclipse-temurin:17-jre-alpine

# ✅ GOOD: Pinned version
FROM eclipse-temurin:17.0.9_9-jre-alpine
```

**2. Minimize Layers:**

```dockerfile
# ❌ BAD: Multiple RUN commands = multiple layers
RUN apk update
RUN apk add curl
RUN apk add wget

# ✅ GOOD: Single RUN with &&
RUN apk update && \
    apk add --no-cache curl wget && \
    rm -rf /var/cache/apk/*
```

**3. Leverage Build Cache:**

```dockerfile
# ❌ BAD: Copy everything, then install deps
COPY . .
RUN mvn dependency:go-offline

# ✅ GOOD: Copy pom.xml first, install deps, then copy code
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
```

**4. Use .dockerignore:**

```
# .dockerignore
target/
.git/
.idea/
*.md
.env
logs/
*.log
```

**5. Don't Run as Root:**

```dockerfile
# ✅ GOOD: Create and use non-root user
RUN adduser -D bibby
USER bibby
```

**6. Use Multi-Stage Builds:**

```dockerfile
# ✅ GOOD: Separate build and runtime
FROM maven AS builder
# ... build ...

FROM eclipse-temurin:17-jre-alpine
COPY --from=builder /build/target/*.jar ./app.jar
```

**7. Set Health Checks:**

```dockerfile
# ✅ GOOD: Container can report health
HEALTHCHECK --interval=30s CMD curl -f http://localhost:8080/health || exit 1
```

---

## 5. Docker Compose for Local Development

### 5.1 What is Docker Compose?

**Problem:** Running multiple containers manually is tedious:
```bash
docker network create bibby-net
docker run -d --name postgres --network bibby-net ...
docker run -d --name app --network bibby-net --depends-on postgres ...
```

**Solution:** Define all services in `docker-compose.yml`.

### 5.2 Complete docker-compose.yml for Bibby

**`docker-compose.yml`:**

```yaml
version: '3.9'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:15-alpine
    container_name: bibby-postgres
    restart: unless-stopped

    environment:
      POSTGRES_DB: bibby
      POSTGRES_USER: bibby_admin
      POSTGRES_PASSWORD: ${DB_PASSWORD:-dev_password_123}
      POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=C"

    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./docker/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql:ro

    ports:
      - "5432:5432"

    networks:
      - bibby-network

    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U bibby_admin -d bibby"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s

  # Bibby Application
  app:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        APP_VERSION: ${APP_VERSION:-0.3.0}

    image: bibby:${APP_VERSION:-0.3.0}
    container_name: bibby-app
    restart: unless-stopped

    depends_on:
      postgres:
        condition: service_healthy

    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      DATABASE_URL: jdbc:postgresql://postgres:5432/bibby
      DATABASE_USERNAME: bibby_admin
      DATABASE_PASSWORD: ${DB_PASSWORD:-dev_password_123}
      JAVA_OPTS: "-Xms256m -Xmx512m -XX:+UseG1GC"

    ports:
      - "8080:8080"

    networks:
      - bibby-network

    volumes:
      - app-logs:/var/log/bibby

    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 3s
      retries: 3
      start_period: 60s

  # pgAdmin (Database UI - optional for development)
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: bibby-pgadmin
    restart: unless-stopped

    environment:
      PGADMIN_DEFAULT_EMAIL: admin@bibby.local
      PGADMIN_DEFAULT_PASSWORD: admin
      PGADMIN_CONFIG_SERVER_MODE: 'False'

    ports:
      - "5050:80"

    networks:
      - bibby-network

    depends_on:
      - postgres

    volumes:
      - pgadmin-data:/var/lib/pgadmin

networks:
  bibby-network:
    driver: bridge
    name: bibby-network

volumes:
  postgres-data:
    name: bibby-postgres-data
  app-logs:
    name: bibby-app-logs
  pgadmin-data:
    name: bibby-pgadmin-data
```

**Environment File (`.env`):**

```bash
# .env (gitignored)
APP_VERSION=0.3.0
SPRING_PROFILES_ACTIVE=dev
DB_PASSWORD=secure_dev_password_123
```

**Database Init Script (`docker/postgres/init.sql`):**

```sql
-- docker/postgres/init.sql
-- Runs automatically on first startup

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);
CREATE INDEX IF NOT EXISTS idx_authors_name ON authors(name);

-- Insert sample data (for development)
INSERT INTO authors (id, name) VALUES
  (1, 'Robert C. Martin'),
  (2, 'Martin Fowler'),
  (3, 'Eric Evans')
ON CONFLICT DO NOTHING;

INSERT INTO books (id, title, book_status) VALUES
  (1, 'Clean Code', 'AVAILABLE'),
  (2, 'Refactoring', 'AVAILABLE'),
  (3, 'Domain-Driven Design', 'AVAILABLE')
ON CONFLICT DO NOTHING;
```

### 5.3 Using Docker Compose

**Start all services:**

```bash
# Start in foreground (see logs)
docker-compose up

# Start in background (detached)
docker-compose up -d

# Build images and start
docker-compose up --build

# Start specific service
docker-compose up postgres
```

**View logs:**

```bash
# All services
docker-compose logs

# Follow logs
docker-compose logs -f

# Specific service
docker-compose logs app

# Last 100 lines
docker-compose logs --tail=100 app
```

**Execute commands:**

```bash
# Execute command in running container
docker-compose exec app /bin/sh

# Run one-off command
docker-compose run app java -version
```

**Stop services:**

```bash
# Stop (containers remain)
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop, remove containers, and volumes
docker-compose down -v

# Stop, remove everything including images
docker-compose down --rmi all -v
```

**Scale services:**

```bash
# Run 3 instances of app (requires load balancer)
docker-compose up -d --scale app=3
```

---

## 6. Docker Commands Reference

### 6.1 Image Commands

```bash
# Build image
docker build -t bibby:0.3.0 .
docker build -t bibby:0.3.0 -f Dockerfile.prod .

# List images
docker images
docker image ls

# Remove image
docker rmi bibby:0.3.0
docker image rm bibby:0.3.0

# Remove unused images
docker image prune

# Remove all images
docker image prune -a

# Inspect image
docker inspect bibby:0.3.0

# View image history (layers)
docker history bibby:0.3.0

# Tag image
docker tag bibby:0.3.0 bibby:latest

# Save image to file
docker save -o bibby.tar bibby:0.3.0

# Load image from file
docker load -i bibby.tar
```

### 6.2 Container Commands

```bash
# Run container
docker run -d --name bibby-app -p 8080:8080 bibby:0.3.0

# List running containers
docker ps

# List all containers
docker ps -a

# Stop container
docker stop bibby-app

# Start container
docker start bibby-app

# Restart container
docker restart bibby-app

# Remove container
docker rm bibby-app

# Remove running container (force)
docker rm -f bibby-app

# Remove all stopped containers
docker container prune

# View logs
docker logs bibby-app
docker logs -f bibby-app
docker logs --tail=100 bibby-app

# Execute command in container
docker exec -it bibby-app /bin/sh
docker exec bibby-app ls -la /opt/bibby

# Copy files
docker cp bibby-app:/opt/bibby/logs/app.log ./app.log
docker cp ./config.yml bibby-app:/opt/bibby/config.yml

# Inspect container
docker inspect bibby-app

# View container stats (CPU, memory)
docker stats bibby-app

# View port mappings
docker port bibby-app
```

### 6.3 System Commands

```bash
# View Docker disk usage
docker system df

# Output:
TYPE            TOTAL     ACTIVE    SIZE      RECLAIMABLE
Images          15        5         2.5GB     1.8GB (72%)
Containers      20        3         500MB     450MB (90%)
Local Volumes   10        5         1GB       500MB (50%)
Build Cache     50        0         3GB       3GB (100%)

# Clean up everything
docker system prune

# Clean up everything including volumes
docker system prune -a --volumes

# View Docker version
docker version

# View Docker info
docker info
```

---

## 7. Interview-Ready Knowledge

### Question: "Explain the difference between containers and VMs"

**Bad Answer:** "Containers are lighter than VMs."

**Good Answer:**
"The fundamental difference is isolation level. Virtual machines provide hardware-level isolation—each VM runs a complete guest OS with its own kernel on top of a hypervisor. This is strong isolation but resource-intensive. A VM can be several gigabytes and takes minutes to boot.

Containers provide process-level isolation using Linux kernel features like namespaces and cgroups. They share the host OS kernel but have isolated filesystems, networks, and process trees. A container is typically megabytes and starts in seconds.

For my Bibby project, the VM approach would need ~500MB for Amazon Linux plus Java, while the container is 192MB with Alpine Linux and JRE. More importantly, I can run dozens of containers on my laptop for testing, but only a few VMs.

The tradeoff is isolation strength—VMs can run different operating systems, while containers must share the host kernel. For running multiple instances of the same application with strong isolation, containers are ideal. For running Windows apps on Linux hosts, you'd need VMs.

Containers aren't 'lightweight VMs'—they're a fundamentally different technology. Docker uses namespace isolation, cgroups for resources, and union filesystems for layering, all native Linux kernel features."

**Why It's Good:**
- Explains technical mechanism (namespaces, cgroups)
- Quantifies differences (size, speed)
- Real project example (Bibby)
- Discusses tradeoffs
- Clarifies common misconception

### Question: "What are Docker image layers and why do they matter?"

**Bad Answer:** "Layers are parts of an image that stack together."

**Good Answer:**
"Docker images are built from layers—each Dockerfile instruction (FROM, RUN, COPY, etc.) creates a new read-only layer. These layers stack using a union filesystem, appearing as a single filesystem to the container.

Layers matter for three reasons: efficiency, caching, and deduplication.

For efficiency, when I update my Bibby application, I only need to upload the changed layers. If my Dockerfile copies the JAR file last, rebuilding with a new version only changes that layer—maybe 30MB instead of 192MB. That's 85% less data transferred.

For caching, Docker reuses layers that haven't changed. In my multi-stage build, if pom.xml is unchanged, the 'mvn dependency:go-offline' layer is cached, saving minutes on every build. I structure Dockerfiles to copy dependencies first, then code, maximizing cache hits.

For deduplication, if ten images use 'FROM alpine:3.18', they share that base layer on disk. Instead of 70MB × 10 images = 700MB, it's 70MB + small deltas.

Each layer is immutable and identified by a SHA256 hash. When a container writes, it uses copy-on-write—the change exists in the container's writable layer, not modifying the image layers. This is why containers start instantly—no file copying, just create a writable layer on top.

Understanding layers changed how I write Dockerfiles—I optimize for cache hits and minimize layer count by combining RUN commands."

**Why It's Good:**
- Technical depth (union filesystem, SHA256, copy-on-write)
- Three clear benefits with explanations
- Quantified improvements
- Practical application (Dockerfile optimization)
- Shows how understanding influences practice

### Question: "How would you optimize a Docker image for production?"

**Bad Answer:** "Use a smaller base image and remove unnecessary files."

**Good Answer:**
"I use a multi-pronged approach for production optimization.

First, multi-stage builds. My Bibby Dockerfile has a Maven build stage and a runtime stage. This eliminates build tools from the production image—dropping from 680MB with full JDK to 192MB with just the JRE. The build artifacts copy over, but Maven, Git, and build caches stay behind.

Second, minimal base images. I use eclipse-temurin:17-jre-alpine instead of the full JDK image. Alpine Linux is 7MB versus Ubuntu's 77MB. For Java apps specifically, JRE is sufficient—JDK includes javac and other development tools we don't need in production.

Third, layer optimization. I structure the Dockerfile so frequently changing files come last—the base image and dependencies change rarely, so those layers are cached. I also combine RUN commands to reduce layer count and clean up in the same layer where files are created, since deleting in a later layer doesn't reduce size.

Fourth, security hardening: I run as a non-root user, set read-only root filesystem where possible, and use --no-cache when installing packages to avoid storing package indices.

Fifth, I enable health checks and proper signal handling. The HEALTHCHECK instruction lets orchestrators know if the container is functioning, and using exec form for ENTRYPOINT ensures SIGTERM reaches the Java process for graceful shutdown.

For Bibby specifically, these techniques reduced the image from 680MB to 192MB—72% smaller. This means faster deployments, lower storage costs, smaller attack surface, and quicker starts."

**Why It's Good:**
- Multiple specific techniques
- Quantified results (680MB → 192MB)
- Explains why each technique works
- Security considerations included
- Real project example

---

## 8. Summary & Docker Checklist

### Key Takeaways

✅ **Concepts:**
- Containers share host kernel (process isolation)
- Images are read-only layers
- Containers add writable layer on top
- Volumes provide persistent storage
- Networks enable container communication

✅ **Architecture:**
- Client (CLI) → Daemon (dockerd) → Registry
- Namespaces for isolation
- cgroups for resources
- Union filesystems for layers

✅ **Best Practices:**
- Multi-stage builds (build vs runtime)
- Minimal base images (Alpine, distroless)
- Layer optimization (cache-friendly order)
- Non-root user
- Health checks
- Specific version tags

### Production Docker Checklist

**✅ Dockerfile:**
- [ ] Multi-stage build used
- [ ] Minimal base image (Alpine, distroless)
- [ ] Specific version tags (not `latest`)
- [ ] .dockerignore file present
- [ ] Non-root user configured
- [ ] Health check defined
- [ ] Proper signal handling (exec form)
- [ ] Layers optimized (dependencies before code)

**✅ Security:**
- [ ] No secrets in image
- [ ] Read-only root filesystem (if possible)
- [ ] Drop unnecessary capabilities
- [ ] Scan for vulnerabilities
- [ ] Use trusted base images

**✅ Size:**
- [ ] Multi-stage build used
- [ ] Unnecessary files removed
- [ ] Package cache cleaned
- [ ] Image < 200MB (for Java apps)

**✅ Runtime:**
- [ ] Resource limits set (CPU, memory)
- [ ] Restart policy defined
- [ ] Logging configured
- [ ] Volumes for persistent data
- [ ] Network properly configured

**Progress: 16 of 28 sections complete (57%)** 📊

**Section 16 Complete:** You now understand Docker fundamentals with a solid mental model! 🐳✨