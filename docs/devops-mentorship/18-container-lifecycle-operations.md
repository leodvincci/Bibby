# Section 18: Container Lifecycle & Operations

## Introduction

You've built an optimized Docker image for Bibby (192MB, cached builds in 12s). Now comes the critical question: **How do you actually run it in production?**

Running `docker run bibby:0.3.0` works, but production containers need:
- **Resource limits** (prevent memory leaks from crashing the host)
- **Restart policies** (auto-recovery from crashes)
- **Health checks** (know when the app is actually ready)
- **Logging** (centralized, structured, queryable)
- **Monitoring** (CPU, memory, I/O metrics)
- **Graceful shutdown** (finish processing requests before terminating)

This section covers the **complete container lifecycle**—from creation to termination—with production-grade configuration for Bibby.

**What You'll Learn:**

1. **Container lifecycle states** - created, running, paused, stopped, dead
2. **docker run deep dive** - Every important flag explained with examples
3. **Resource limits** - CPU, memory, I/O quotas and throttling
4. **Logging strategies** - JSON, syslog, centralized logging
5. **Health checks** - Liveness, readiness, startup probes
6. **Container monitoring** - Metrics collection and analysis
7. **Debugging techniques** - Logs, exec, inspect, events
8. **Orchestration readiness** - Preparing for Kubernetes

**Prerequisites**: Sections 16-17 (Docker Fundamentals, Images & Layers)

---

## 1. Container Lifecycle States

### 1.1 State Diagram

Containers move through distinct states during their lifetime:

```
┌─────────┐
│ IMAGE   │
└────┬────┘
     │ docker create
     ↓
┌─────────┐
│ CREATED │────────────────────────┐
└────┬────┘                        │
     │ docker start               │
     ↓                            │
┌─────────┐                        │
│ RUNNING │←──────────┐            │
└────┬────┘           │            │
     │                │            │
     ├─→ docker pause │            │
     │   ┌─────────┐  │            │
     │   │ PAUSED  │──┘            │
     │   └─────────┘               │
     │   docker unpause            │
     │                             │
     ├─→ docker stop               │
     │   ┌─────────┐               │
     │   │ STOPPED │               │
     │   └────┬────┘               │
     │        │ docker start       │
     │        └────────────────────┘
     │
     ├─→ docker kill / crash
     │   ┌──────┐
     │   │ DEAD │
     │   └──────┘
     │
     └─→ docker rm
         [REMOVED]
```

### 1.2 State Transitions for Bibby

**Example: Complete Bibby Container Lifecycle**

```bash
# 1. CREATED state (container exists but not running)
docker create --name bibby-app bibby:0.3.0
# Container ID: a1b2c3d4e5f6
# Status: Created

# Verify state
docker ps -a --filter name=bibby-app
# CONTAINER ID   IMAGE          STATUS
# a1b2c3d4e5f6   bibby:0.3.0    Created

# 2. RUNNING state
docker start bibby-app
# bibby-app

docker ps --filter name=bibby-app
# STATUS: Up 3 seconds

# Check logs to see Spring Boot startup
docker logs -f bibby-app
# ... Spring Boot banner ...
# Started BibbyApplication in 3.456 seconds

# 3. PAUSED state (freeze all processes)
docker pause bibby-app

docker ps --filter name=bibby-app
# STATUS: Up 2 minutes (Paused)

# Container is frozen - doesn't respond to requests
curl http://localhost:8080/actuator/health
# [Hangs - no response]

# 4. Resume from PAUSED
docker unpause bibby-app

curl http://localhost:8080/actuator/health
# {"status":"UP"}

# 5. STOPPED state (graceful shutdown)
docker stop bibby-app
# Sends SIGTERM, waits 10s, then SIGKILL if needed

docker ps -a --filter name=bibby-app
# STATUS: Exited (143) 5 seconds ago
# Exit code 143 = 128 + 15 (SIGTERM)

# 6. Restart container
docker start bibby-app
# Back to RUNNING

# 7. DEAD state (forced kill)
docker kill bibby-app
# Sends SIGKILL immediately (no graceful shutdown)

docker ps -a --filter name=bibby-app
# STATUS: Exited (137) 1 second ago
# Exit code 137 = 128 + 9 (SIGKILL)

# 8. REMOVED state
docker rm bibby-app
# bibby-app

docker ps -a --filter name=bibby-app
# [Empty - container removed]
```

### 1.3 Exit Codes

Understanding container exit codes is critical for debugging:

| Exit Code | Signal | Meaning | Bibby Example |
|-----------|--------|---------|---------------|
| 0 | - | Success | Graceful shutdown via `docker stop` |
| 1 | - | Application error | Uncaught exception in Java code |
| 2 | - | Misuse | Invalid JVM arguments |
| 125 | - | Docker daemon error | Image not found |
| 126 | - | Command cannot execute | Entrypoint not executable |
| 127 | - | Command not found | Typo in ENTRYPOINT |
| 130 | SIGINT (2) | Ctrl+C | Manual interrupt |
| 137 | SIGKILL (9) | Killed | OOM kill or `docker kill` |
| 143 | SIGTERM (15) | Terminated | `docker stop` (graceful) |

**Check Bibby's exit code:**

```bash
# Run container that crashes
docker run --name bibby-test bibby:0.3.0

# Simulate OOM kill in another terminal
docker kill -s SIGKILL bibby-test

# Check exit code
docker inspect bibby-test --format='{{.State.ExitCode}}'
# 137

# Check what signal was used
docker inspect bibby-test --format='{{.State.Error}}'
# "OOMKilled" or signal details
```

**Common Bibby Exit Codes:**

```bash
# Exit 0: Normal shutdown
docker run --name bibby-success bibby:0.3.0
docker stop bibby-success
docker inspect bibby-success --format='{{.State.ExitCode}}'
# 0

# Exit 1: Database connection failed
docker run --name bibby-fail \
  -e DATABASE_URL=jdbc:postgresql://nonexistent:5432/bibby \
  bibby:0.3.0
# Application fails to connect to database
docker inspect bibby-fail --format='{{.State.ExitCode}}'
# 1

# Exit 137: OOM killed
docker run --name bibby-oom \
  --memory=50m \
  bibby:0.3.0
# JVM tries to allocate 256m (JAVA_OPTS) but limit is 50m
docker inspect bibby-oom --format='{{.State.ExitCode}}'
# 137
```

---

## 2. docker run Deep Dive

### 2.1 Basic Syntax

```bash
docker run [OPTIONS] IMAGE[:TAG] [COMMAND] [ARG...]
```

**Most Common Options:**

```bash
docker run \
  --name bibby-prod \              # Container name
  -d \                             # Detached mode (background)
  -p 8080:8080 \                   # Port mapping
  -e DATABASE_URL=... \            # Environment variable
  --restart unless-stopped \       # Restart policy
  --memory=512m \                  # Memory limit
  --cpus=1.0 \                     # CPU limit
  -v bibby-data:/data \            # Volume mount
  --health-cmd='curl -f ...' \     # Health check
  --log-driver json-file \         # Logging driver
  --log-opt max-size=10m \         # Log rotation
  bibby:0.3.0
```

### 2.2 Naming and Identification

**Option: `--name`**

```bash
# Without name (random name assigned)
docker run -d bibby:0.3.0
# Container name: nostalgic_darwin (random)

# With name (explicit)
docker run -d --name bibby-prod bibby:0.3.0
# Container name: bibby-prod

# Name must be unique
docker run -d --name bibby-prod bibby:0.3.0
# Error: name already in use

# Reference by name
docker logs bibby-prod
docker stop bibby-prod
docker exec bibby-prod ps aux
```

**Best Practice**: Always use `--name` in production for clear identification.

### 2.3 Port Mapping

**Option: `-p` / `--publish`**

```bash
# Format: -p HOST_PORT:CONTAINER_PORT

# Map Bibby's 8080 to host 8080
docker run -d -p 8080:8080 --name bibby bibby:0.3.0

# Access from host
curl http://localhost:8080/actuator/health
# {"status":"UP"}

# Map to different host port
docker run -d -p 3000:8080 --name bibby-alt bibby:0.3.0
curl http://localhost:3000/actuator/health

# Map specific interface (security)
docker run -d -p 127.0.0.1:8080:8080 --name bibby-local bibby:0.3.0
# Only accessible from localhost (not external network)

# Random host port
docker run -d -p 8080 --name bibby-random bibby:0.3.0
docker port bibby-random
# 8080/tcp -> 0.0.0.0:32768 (random high port)

# Multiple ports
docker run -d \
  -p 8080:8080 \
  -p 8081:8081 \
  --name bibby-multi \
  bibby:0.3.0
```

**Production Bibby Configuration:**

```bash
# Production: Use non-privileged port, bind to private IP
docker run -d \
  --name bibby-prod \
  -p 10.0.1.10:8080:8080 \
  bibby:0.3.0
```

### 2.4 Environment Variables

**Option: `-e` / `--env`**

Bibby uses environment variables for database configuration (from `application.properties`):

```bash
# Single variable
docker run -d \
  -e DATABASE_URL=jdbc:postgresql://db.example.com:5432/bibby \
  --name bibby \
  bibby:0.3.0

# Multiple variables
docker run -d \
  -e DATABASE_URL=jdbc:postgresql://db.prod:5432/bibby \
  -e DATABASE_USER=bibby_prod \
  -e DATABASE_PASSWORD=secret123 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e JAVA_OPTS="-Xms512m -Xmx1g" \
  --name bibby-prod \
  bibby:0.3.0

# From file (better for many variables)
cat > bibby.env <<EOF
DATABASE_URL=jdbc:postgresql://db.prod:5432/bibby
DATABASE_USER=bibby_prod
DATABASE_PASSWORD=secret123
SPRING_PROFILES_ACTIVE=production
JAVA_OPTS=-Xms512m -Xmx1g -XX:+UseG1GC
EOF

docker run -d \
  --env-file bibby.env \
  --name bibby-prod \
  bibby:0.3.0

# Verify environment variables
docker exec bibby-prod env | grep DATABASE
# DATABASE_URL=jdbc:postgresql://db.prod:5432/bibby
# DATABASE_USER=bibby_prod
```

**Security Note**: Don't commit `.env` files with secrets to Git!

```bash
# .gitignore
*.env
bibby.env
```

### 2.5 Restart Policies

**Option: `--restart`**

| Policy | Behavior | Use Case |
|--------|----------|----------|
| `no` | Never restart (default) | Development |
| `on-failure[:max]` | Restart if exit code != 0 | Most production apps |
| `always` | Always restart (even manual stop) | Critical services |
| `unless-stopped` | Always restart unless manually stopped | **Recommended for Bibby** |

**Examples:**

```bash
# No restart (default)
docker run -d --name bibby-dev bibby:0.3.0
docker stop bibby-dev
# Reboot host → bibby-dev NOT restarted

# Always restart
docker run -d --restart always --name bibby-critical bibby:0.3.0
docker stop bibby-critical
# Container immediately restarts (even after manual stop!)
# Reboot host → bibby-critical IS restarted

# Unless stopped (recommended)
docker run -d --restart unless-stopped --name bibby-prod bibby:0.3.0
docker stop bibby-prod
# Container stays stopped
# Reboot host → bibby-prod IS restarted

# On failure (max 3 attempts)
docker run -d --restart on-failure:3 --name bibby-test bibby:0.3.0
# Crashes due to DB connection → restarts up to 3 times
# After 3 failures → stops restarting
```

**Production Bibby:**

```bash
docker run -d \
  --restart unless-stopped \
  --name bibby-prod \
  bibby:0.3.0
```

**Check restart count:**

```bash
docker inspect bibby-prod \
  --format='{{.RestartCount}}'
# 0 (no restarts)

# After a crash
docker inspect bibby-prod \
  --format='{{.RestartCount}}'
# 3 (restarted 3 times)
```

### 2.6 Detached vs Interactive

**Option: `-d` (detached) or `-it` (interactive)**

```bash
# Detached (background)
docker run -d --name bibby-bg bibby:0.3.0
# Returns container ID immediately
# View logs: docker logs bibby-bg

# Interactive (foreground)
docker run -it --name bibby-fg bibby:0.3.0
# Attached to container output
# Ctrl+C stops container

# Interactive with shell (debugging)
docker run -it --name bibby-debug bibby:0.3.0 sh
# Override entrypoint, get shell
/opt/bibby # ls
Bibby.jar
/opt/bibby # java -jar Bibby.jar
# Manual startup

# Production: Always use -d
docker run -d \
  --restart unless-stopped \
  --name bibby-prod \
  bibby:0.3.0
```

### 2.7 Volume Mounts

**Option: `-v` / `--volume` or `--mount`**

Bibby doesn't persist data in the container filesystem (uses PostgreSQL), but you might want to:
- Mount configuration files
- Store application logs
- Share data with other containers

```bash
# Named volume (Docker-managed)
docker volume create bibby-logs

docker run -d \
  -v bibby-logs:/opt/bibby/logs \
  --name bibby \
  bibby:0.3.0

# Verify volume
docker volume inspect bibby-logs
# Mountpoint: /var/lib/docker/volumes/bibby-logs/_data

# Bind mount (host directory)
docker run -d \
  -v /opt/bibby/config:/opt/bibby/config:ro \
  --name bibby \
  bibby:0.3.0
# Mount host /opt/bibby/config → container /opt/bibby/config (read-only)

# Modern --mount syntax (preferred)
docker run -d \
  --mount type=volume,source=bibby-logs,target=/opt/bibby/logs \
  --name bibby \
  bibby:0.3.0

# Bind mount with --mount
docker run -d \
  --mount type=bind,source=/opt/bibby/config,target=/opt/bibby/config,readonly \
  --name bibby \
  bibby:0.3.0
```

**Production Example: External Configuration**

```bash
# Host: /opt/bibby/config/application-production.properties
cat > /opt/bibby/config/application-production.properties <<EOF
spring.datasource.url=jdbc:postgresql://db.prod:5432/bibby
spring.jpa.hibernate.ddl-auto=validate
logging.level.com.penrose.bibby=INFO
EOF

# Run Bibby with external config
docker run -d \
  --mount type=bind,source=/opt/bibby/config,target=/config,readonly \
  -e SPRING_CONFIG_LOCATION=file:/config/application-production.properties \
  --name bibby-prod \
  bibby:0.3.0
```

### 2.8 Network Configuration

**Option: `--network`**

```bash
# Default: bridge network
docker run -d --name bibby bibby:0.3.0
# Attached to default bridge

# Custom network (better DNS)
docker network create bibby-network

docker run -d \
  --network bibby-network \
  --name bibby-postgres \
  postgres:15-alpine

docker run -d \
  --network bibby-network \
  -e DATABASE_URL=jdbc:postgresql://bibby-postgres:5432/bibby \
  --name bibby-app \
  bibby:0.3.0
# Can reference postgres by name (bibby-postgres)

# Host network (no isolation - use with caution)
docker run -d \
  --network host \
  --name bibby-host \
  bibby:0.3.0
# Container uses host's network stack directly
# No -p needed, but less secure

# None (no network)
docker run -d \
  --network none \
  --name bibby-isolated \
  bibby:0.3.0
# No network access (completely isolated)
```

---

## 3. Resource Limits and Quotas

### 3.1 Why Resource Limits Matter

**Without limits:**
- Memory leak in Bibby → OOM kills host's kernel processes
- Infinite loop → consumes all CPU, starves other containers
- Disk writes → fills host filesystem

**With limits:**
- Container killed when exceeds memory limit (host protected)
- CPU throttled to fair share
- Disk I/O controlled

### 3.2 Memory Limits

**Options: `--memory`, `--memory-reservation`, `--memory-swap`**

```bash
# Hard limit: 512MB
docker run -d \
  --memory=512m \
  --name bibby \
  bibby:0.3.0

# Container killed if exceeds 512MB
docker stats bibby
# MEM USAGE / LIMIT
# 245MiB / 512MiB
```

**Bibby Memory Sizing:**

```bash
# Check Bibby's memory usage
docker run -d \
  --name bibby-test \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  bibby:0.3.0

# Monitor for 5 minutes
docker stats bibby-test --no-stream
# MEM USAGE: ~380MB (heap + metaspace + OS buffers)

# Set limit with headroom (20% above max heap)
# Max heap: 512MB → Container limit: 640MB
docker run -d \
  --memory=640m \
  -e JAVA_OPTS="-Xms256m -Xmx512m -XX:MaxMetaspaceSize=128m" \
  --name bibby-prod \
  bibby:0.3.0
```

**Memory Reservation (Soft Limit):**

```bash
# Reserve 256MB, allow burst to 512MB
docker run -d \
  --memory=512m \
  --memory-reservation=256m \
  --name bibby \
  bibby:0.3.0

# Under memory pressure:
# - Docker tries to keep container at 256MB
# - Allows burst to 512MB if host has free memory
```

**Swap Control:**

```bash
# Disable swap (recommended for production)
docker run -d \
  --memory=512m \
  --memory-swap=512m \
  --name bibby \
  bibby:0.3.0
# memory-swap = memory → no swap (0 swap)

# Allow 512MB swap
docker run -d \
  --memory=512m \
  --memory-swap=1024m \
  --name bibby \
  bibby:0.3.0
# 512MB physical + 512MB swap = 1024MB total
```

**OOM Behavior:**

```bash
# Default: Container killed on OOM
docker run -d \
  --memory=100m \
  --name bibby-oom \
  bibby:0.3.0
# JVM tries to allocate 256m → OOM killed

docker inspect bibby-oom --format='{{.State.OOMKilled}}'
# true

# Disable OOM killer (risky!)
docker run -d \
  --memory=512m \
  --oom-kill-disable \
  --name bibby-noom \
  bibby:0.3.0
# If exceeds limit, container hangs (not killed)
# Only use with memory-reservation
```

### 3.3 CPU Limits

**Options: `--cpus`, `--cpu-shares`, `--cpuset-cpus`**

```bash
# Limit to 1 CPU core
docker run -d \
  --cpus=1.0 \
  --name bibby \
  bibby:0.3.0

# Container can use max 100% of 1 core
# On 4-core system: max 25% total CPU

# Limit to 1.5 cores
docker run -d \
  --cpus=1.5 \
  --name bibby \
  bibby:0.3.0
# Can use 150% CPU (1.5 cores)

# Fractional CPUs
docker run -d \
  --cpus=0.5 \
  --name bibby-small \
  bibby:0.3.0
# Max 50% of 1 core
```

**CPU Shares (Relative Weight):**

```bash
# Default shares: 1024
# Container A: 1024 shares (50%)
# Container B: 1024 shares (50%)

docker run -d \
  --cpu-shares=2048 \
  --name bibby-high-priority \
  bibby:0.3.0

docker run -d \
  --cpu-shares=1024 \
  --name bibby-normal \
  bibby:0.3.0

# Under CPU contention:
# bibby-high-priority gets 2/3 CPU
# bibby-normal gets 1/3 CPU

# If only one container running:
# It can use 100% CPU regardless of shares
```

**CPU Pinning:**

```bash
# Pin to specific cores (CPUs 0 and 1)
docker run -d \
  --cpuset-cpus=0,1 \
  --name bibby \
  bibby:0.3.0

# Only runs on cores 0 and 1
# Useful for NUMA systems
```

**Production Bibby CPU Configuration:**

```bash
# 4-core host, Bibby allowed up to 2 cores
docker run -d \
  --cpus=2.0 \
  --memory=640m \
  --name bibby-prod \
  bibby:0.3.0
```

### 3.4 Disk I/O Limits

**Options: `--device-read-bps`, `--device-write-bps`, `--device-read-iops`, `--device-write-iops`**

```bash
# Limit write to 10 MB/s
docker run -d \
  --device-write-bps=/dev/sda:10mb \
  --name bibby \
  bibby:0.3.0

# Limit read IOPS to 1000 operations/sec
docker run -d \
  --device-read-iops=/dev/sda:1000 \
  --name bibby \
  bibby:0.3.0
```

**Rarely needed for Bibby** (database handles I/O), but useful for:
- Log-heavy applications
- File processing containers
- Noisy neighbor prevention

### 3.5 Production Resource Configuration

**Complete Production Bibby Container:**

```bash
docker run -d \
  --name bibby-prod \
  --restart unless-stopped \
  \
  # Network
  --network bibby-network \
  -p 127.0.0.1:8080:8080 \
  \
  # Resources
  --memory=640m \
  --memory-reservation=512m \
  --memory-swap=640m \
  --cpus=2.0 \
  \
  # Environment
  -e DATABASE_URL=jdbc:postgresql://bibby-db:5432/bibby \
  -e DATABASE_USER=bibby_prod \
  -e DATABASE_PASSWORD=$(cat /run/secrets/db_password) \
  -e SPRING_PROFILES_ACTIVE=production \
  -e JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxMetaspaceSize=128m" \
  \
  # Volumes
  --mount type=volume,source=bibby-logs,target=/opt/bibby/logs \
  --mount type=bind,source=/opt/bibby/config,target=/config,readonly \
  \
  # Health & Logging
  --health-cmd="wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1" \
  --health-interval=30s \
  --health-timeout=3s \
  --health-start-period=40s \
  --health-retries=3 \
  --log-driver json-file \
  --log-opt max-size=10m \
  --log-opt max-file=3 \
  \
  bibby:0.3.0
```

---

## 4. Logging Strategies

### 4.1 Docker Logging Drivers

Docker supports multiple logging drivers:

| Driver | Description | Use Case |
|--------|-------------|----------|
| `json-file` | JSON to local file (default) | Development, small deployments |
| `syslog` | Send to syslog | Traditional syslog infrastructure |
| `journald` | systemd journal | systemd-based systems |
| `gelf` | Graylog Extended Log Format | Centralized Graylog/ELK |
| `fluentd` | Forward to Fluentd | Kubernetes, centralized logging |
| `awslogs` | CloudWatch Logs | AWS deployments |
| `splunk` | Splunk HEC | Enterprise Splunk |
| `gcplogs` | Google Cloud Logging | GCP deployments |

### 4.2 JSON File Logging (Default)

```bash
# Default logging
docker run -d --name bibby bibby:0.3.0

# Logs stored in:
# /var/lib/docker/containers/<container-id>/<container-id>-json.log

# View logs
docker logs bibby
# Shows all logs since container start

# Follow logs (like tail -f)
docker logs -f bibby

# Show timestamps
docker logs -t bibby
# 2025-01-17T10:30:45.123Z  :: Spring Boot ::        (v3.5.7)

# Show last 100 lines
docker logs --tail 100 bibby

# Show logs since 1 hour ago
docker logs --since 1h bibby

# Show logs between timestamps
docker logs --since 2025-01-17T10:00:00 --until 2025-01-17T11:00:00 bibby
```

**Problem: Logs Grow Unbounded**

```bash
# Check log file size
docker inspect bibby --format='{{.LogPath}}'
# /var/lib/docker/containers/abc.../abc...-json.log

sudo ls -lh /var/lib/docker/containers/abc.../abc...-json.log
# -rw-r----- 1 root root 2.4G Jan 17 10:30 abc...-json.log
# 2.4GB of logs! Disk full risk
```

**Solution: Log Rotation**

```bash
docker run -d \
  --log-driver json-file \
  --log-opt max-size=10m \
  --log-opt max-file=3 \
  --name bibby \
  bibby:0.3.0

# Keeps max 3 files × 10MB = 30MB total
# Oldest logs rotated out automatically
```

**Global Configuration** (`/etc/docker/daemon.json`):

```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3",
    "compress": "true"
  }
}
```

```bash
sudo systemctl restart docker
```

### 4.3 Structured Logging for Bibby

**Problem**: Spring Boot logs are unstructured text:

```
2025-01-17 10:30:45.123  INFO 1 --- [main] c.p.b.BibbyApplication : Starting BibbyApplication...
```

**Solution**: Use JSON logging (Logback JSON encoder):

**Update `pom.xml`:**

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**Create `src/main/resources/logback-spring.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Console appender with JSON format -->
    <appender name="CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"application":"bibby","version":"0.3.0"}</customFields>
        </encoder>
    </appender>

    <!-- File appender (for volume mount) -->
    <appender name="FILE_JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/opt/bibby/logs/bibby.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/opt/bibby/logs/bibby.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE_JSON"/>
        <appender-ref ref="FILE_JSON"/>
    </root>
</configuration>
```

**Result: Structured JSON Logs**

```json
{
  "@timestamp": "2025-01-17T10:30:45.123Z",
  "application": "bibby",
  "version": "0.3.0",
  "level": "INFO",
  "logger_name": "com.penrose.bibby.BibbyApplication",
  "message": "Starting BibbyApplication",
  "thread_name": "main",
  "level_value": 20000
}
```

**Query with jq:**

```bash
# All ERROR level logs
docker logs bibby | jq -r 'select(.level=="ERROR")'

# Logs from specific class
docker logs bibby | jq -r 'select(.logger_name | contains("BookService"))'

# Count log levels
docker logs bibby | jq -r '.level' | sort | uniq -c
#   1234 INFO
#     56 WARN
#      3 ERROR
```

### 4.4 Centralized Logging with Fluentd

**Architecture:**

```
┌─────────────┐
│ Bibby       │──┐
│ Container   │  │
└─────────────┘  │
                 ├──→ ┌─────────────┐      ┌─────────────┐
┌─────────────┐  │    │  Fluentd    │─────→│ Elasticsearch│
│ Postgres    │──┤    │  Container  │      └─────────────┘
│ Container   │  │    └─────────────┘             │
└─────────────┘  │                                ↓
                 │                        ┌─────────────┐
┌─────────────┐  │                        │   Kibana    │
│ Other       │──┘                        │ (Dashboard) │
│ Containers  │                           └─────────────┘
└─────────────┘
```

**Setup Fluentd:**

```bash
# Create Fluentd config
mkdir -p /opt/fluentd/config

cat > /opt/fluentd/config/fluent.conf <<'EOF'
<source>
  @type forward
  port 24224
  bind 0.0.0.0
</source>

<match bibby.**>
  @type elasticsearch
  host elasticsearch
  port 9200
  logstash_format true
  logstash_prefix bibby
  include_tag_key true
  tag_key @log_name
  flush_interval 10s
</match>
EOF

# Run Fluentd
docker run -d \
  --name fluentd \
  --network bibby-network \
  -p 24224:24224 \
  -v /opt/fluentd/config:/fluentd/etc \
  fluent/fluentd:v1.16-1
```

**Run Bibby with Fluentd Logging:**

```bash
docker run -d \
  --name bibby-prod \
  --network bibby-network \
  --log-driver fluentd \
  --log-opt fluentd-address=localhost:24224 \
  --log-opt tag="bibby.{{.Name}}" \
  bibby:0.3.0
```

**Logs now flow**: Bibby → Fluentd → Elasticsearch → Kibana (searchable dashboard)

### 4.5 AWS CloudWatch Logs

For AWS deployments:

```bash
# IAM role required with logs:CreateLogStream, logs:PutLogEvents

docker run -d \
  --name bibby-prod \
  --log-driver awslogs \
  --log-opt awslogs-region=us-east-1 \
  --log-opt awslogs-group=/ecs/bibby \
  --log-opt awslogs-stream=bibby-prod-$(date +%s) \
  bibby:0.3.0
```

**View in CloudWatch:**
- Log Group: `/ecs/bibby`
- Log Stream: `bibby-prod-1705488645`
- Query with CloudWatch Insights

---

## 5. Health Checks

### 5.1 Why Health Checks Matter

**Without health checks:**
- Container running ≠ application healthy
- Spring Boot might be stuck in startup
- Database connection lost, but container still "up"

**With health checks:**
- Automated restart on unhealthy state
- Load balancers remove unhealthy instances
- Monitoring systems alerted

### 5.2 Dockerfile HEALTHCHECK

**Bibby's Current Dockerfile:**

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

**Parameters:**

- `--interval=30s`: Check every 30 seconds
- `--timeout=3s`: Consider failed if takes >3s
- `--start-period=40s`: Grace period for startup (Spring Boot takes ~30s)
- `--retries=3`: Mark unhealthy after 3 consecutive failures

**Health States:**

```
┌──────────┐
│ starting │ (during start-period)
└────┬─────┘
     │ first successful check
     ↓
┌──────────┐
│ healthy  │←──────────┐
└────┬─────┘           │
     │ 1 failure       │
     ↓                 │
┌──────────┐           │
│ healthy  │ (retries: 1/3)
└────┬─────┘           │
     │ 2nd failure     │ success
     ↓                 │
┌──────────┐           │
│ healthy  │ (retries: 2/3)
└────┬─────┘           │
     │ 3rd failure     │
     ↓                 │
┌──────────┐           │
│unhealthy │───────────┘
└──────────┘  (if success → healthy)
```

### 5.3 Spring Boot Actuator Health Endpoint

Bibby uses Spring Boot Actuator (from `pom.xml`):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Health Endpoint:**

```bash
curl http://localhost:8080/actuator/health
```

**Response (Healthy):**

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 107374182400,
        "free": 53687091200,
        "threshold": 10485760,
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**Response (Unhealthy - DB Down):**

```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "DOWN",
      "details": {
        "error": "org.postgresql.util.PSQLException: Connection refused"
      }
    }
  }
}
```

**Exit Code:**
- Status `UP` → HTTP 200 → Health check success (exit 0)
- Status `DOWN` → HTTP 503 → Health check failure (exit 1)

### 5.4 Custom Health Indicators

**Create `BookHealthIndicator.java`:**

```java
package com.penrose.bibby.health;

import com.penrose.bibby.library.book.BookRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class BookHealthIndicator implements HealthIndicator {

    private final BookRepository bookRepository;

    public BookHealthIndicator(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public Health health() {
        try {
            long bookCount = bookRepository.count();

            if (bookCount >= 0) {
                return Health.up()
                    .withDetail("bookCount", bookCount)
                    .build();
            } else {
                return Health.down()
                    .withDetail("reason", "Invalid book count")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

**Updated Health Response:**

```json
{
  "status": "UP",
  "components": {
    "book": {
      "status": "UP",
      "details": {
        "bookCount": 42
      }
    },
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### 5.5 Liveness vs Readiness vs Startup Probes

Kubernetes-style probes (Docker doesn't distinguish, but pattern is important):

**Liveness**: Is the app alive? (Should it be restarted?)

```bash
# Example: Deadlock detection
curl http://localhost:8080/actuator/health/liveness
# UP → Container should keep running
# DOWN → Container should be killed and restarted
```

**Readiness**: Is the app ready to serve traffic? (Should it receive requests?)

```bash
# Example: Still warming up cache, don't send traffic yet
curl http://localhost:8080/actuator/health/readiness
# UP → Send traffic
# DOWN → Don't send traffic (but don't kill container)
```

**Startup**: Has the app finished starting? (Extended grace period)

```bash
# Example: Spring Boot taking 60 seconds to start
curl http://localhost:8080/actuator/health/startup
# UP → App started, switch to liveness probe
# DOWN (during startup) → Keep waiting
# DOWN (after timeout) → Kill container
```

**Configure in Bibby:**

```properties
# src/main/resources/application.properties
management.endpoint.health.probes.enabled=true
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true
```

**Endpoints:**

```bash
# Liveness
curl http://localhost:8080/actuator/health/liveness
# {"status":"UP"}

# Readiness
curl http://localhost:8080/actuator/health/readiness
# {"status":"UP"}
```

### 5.6 Monitor Health Status

```bash
# Check current health status
docker inspect bibby-prod --format='{{.State.Health.Status}}'
# healthy

# View health check logs
docker inspect bibby-prod --format='{{json .State.Health}}' | jq
```

**Output:**

```json
{
  "Status": "healthy",
  "FailingStreak": 0,
  "Log": [
    {
      "Start": "2025-01-17T10:30:00Z",
      "End": "2025-01-17T10:30:01Z",
      "ExitCode": 0,
      "Output": ""
    },
    {
      "Start": "2025-01-17T10:30:30Z",
      "End": "2025-01-17T10:30:31Z",
      "ExitCode": 0,
      "Output": ""
    }
  ]
}
```

**Filter by health in docker ps:**

```bash
# Show only healthy containers
docker ps --filter health=healthy

# Show unhealthy containers
docker ps --filter health=unhealthy
```

---

## 6. Container Monitoring

### 6.1 docker stats

Real-time resource usage:

```bash
# Monitor Bibby
docker stats bibby-prod

# Output:
CONTAINER ID   NAME         CPU %   MEM USAGE / LIMIT   MEM %   NET I/O         BLOCK I/O
a1b2c3d4e5f6   bibby-prod   12.5%   456MiB / 640MiB     71.2%   1.2MB / 850KB   12MB / 4MB
```

**Continuous monitoring:**

```bash
# Update every 5 seconds
docker stats bibby-prod --no-stream
```

**All containers:**

```bash
docker stats $(docker ps --format='{{.Names}}')
```

**Export to CSV:**

```bash
# Collect stats every 10 seconds
while true; do
  docker stats --no-stream --format "{{.Name}},{{.CPUPerc}},{{.MemUsage}}" bibby-prod >> bibby-stats.csv
  sleep 10
done
```

### 6.2 Prometheus Metrics

**Add Micrometer Prometheus to `pom.xml`:**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Enable in `application.properties`:**

```properties
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.metrics.export.prometheus.enabled=true
```

**Metrics Endpoint:**

```bash
curl http://localhost:8080/actuator/prometheus
```

**Output (Prometheus format):**

```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space",} 4.5678912E7
jvm_memory_used_bytes{area="heap",id="G1 Old Gen",} 3.2145896E7
# HELP http_server_requests_seconds
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",status="200",uri="/actuator/health",} 1234
http_server_requests_seconds_sum{method="GET",status="200",uri="/actuator/health",} 12.345
```

**Scrape with Prometheus:**

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'bibby'
    static_configs:
      - targets: ['bibby-prod:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
```

**Run Prometheus:**

```bash
docker run -d \
  --name prometheus \
  --network bibby-network \
  -p 9090:9090 \
  -v /opt/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

**Grafana Dashboard:**

```bash
docker run -d \
  --name grafana \
  --network bibby-network \
  -p 3000:3000 \
  grafana/grafana

# Access: http://localhost:3000
# Default: admin / admin
# Add Prometheus data source
# Import dashboard: JVM (Micrometer) - ID 4701
```

### 6.3 cAdvisor (Container Advisor)

Google's container monitoring tool:

```bash
docker run -d \
  --name cadvisor \
  --volume=/:/rootfs:ro \
  --volume=/var/run:/var/run:ro \
  --volume=/sys:/sys:ro \
  --volume=/var/lib/docker/:/var/lib/docker:ro \
  --publish=8081:8080 \
  gcr.io/cadvisor/cadvisor:latest

# Access: http://localhost:8081
# View Bibby metrics in UI
```

**Export to Prometheus:**

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']
```

---

## 7. Debugging Techniques

### 7.1 Container Logs

```bash
# View all logs
docker logs bibby-prod

# Follow logs
docker logs -f bibby-prod

# Last 100 lines
docker logs --tail 100 bibby-prod

# With timestamps
docker logs -t bibby-prod

# Filter with grep
docker logs bibby-prod 2>&1 | grep ERROR

# JSON formatted logs (if using Logback JSON)
docker logs bibby-prod | jq -r 'select(.level=="ERROR") | "\(.@timestamp) \(.message)"'
```

### 7.2 Execute Commands in Running Container

```bash
# Interactive shell
docker exec -it bibby-prod sh

# Inside container
/opt/bibby # ps aux
# PID   USER     COMMAND
# 1     bibby    java -jar Bibby.jar

/opt/bibby # ls -la
# Bibby.jar

/opt/bibby # netstat -tlnp
# Active Internet connections
# tcp        0      0 :::8080        :::*       LISTEN      1/java

# Exit
/opt/bibby # exit
```

**Run specific command:**

```bash
# Check Java version
docker exec bibby-prod java -version

# View environment variables
docker exec bibby-prod env

# Check disk usage
docker exec bibby-prod df -h

# Test health endpoint internally
docker exec bibby-prod wget -qO- http://localhost:8080/actuator/health

# View running processes
docker exec bibby-prod ps aux

# Check network connectivity
docker exec bibby-prod ping -c 3 bibby-db

# View application properties
docker exec bibby-prod cat /opt/bibby/application.properties
```

### 7.3 Copy Files To/From Container

```bash
# Copy from container to host
docker cp bibby-prod:/opt/bibby/logs/bibby.log ./bibby.log

# Copy from host to container
docker cp ./new-config.properties bibby-prod:/opt/bibby/config/
```

### 7.4 Inspect Container Metadata

```bash
# Full JSON output
docker inspect bibby-prod

# Specific field (IP address)
docker inspect bibby-prod --format='{{.NetworkSettings.IPAddress}}'

# Environment variables
docker inspect bibby-prod --format='{{.Config.Env}}'

# Volumes
docker inspect bibby-prod --format='{{.Mounts}}'

# Health status
docker inspect bibby-prod --format='{{.State.Health.Status}}'

# Exit code
docker inspect bibby-prod --format='{{.State.ExitCode}}'

# Restart count
docker inspect bibby-prod --format='{{.RestartCount}}'
```

### 7.5 Container Events

Monitor Docker events:

```bash
# Watch all events
docker events

# Filter by container
docker events --filter container=bibby-prod

# Filter by event type
docker events --filter event=start --filter event=stop

# Since timestamp
docker events --since '2025-01-17T10:00:00'
```

**Example Output:**

```
2025-01-17T10:30:45.123Z container start a1b2c3d4e5f6 (image=bibby:0.3.0, name=bibby-prod)
2025-01-17T10:30:46.234Z container health_status: healthy a1b2c3d4e5f6 (name=bibby-prod)
2025-01-17T10:35:12.456Z container stop a1b2c3d4e5f6 (name=bibby-prod)
```

### 7.6 Attach to Running Container

```bash
# Attach to container's STDOUT/STDERR
docker attach bibby-prod

# See live output
# Ctrl+C stops container (be careful!)

# Detach without stopping: Ctrl+P, Ctrl+Q
```

### 7.7 Export Container Filesystem

```bash
# Export entire filesystem as tar
docker export bibby-prod > bibby-filesystem.tar

# Extract and inspect
mkdir bibby-fs
tar -xf bibby-filesystem.tar -C bibby-fs
ls bibby-fs/opt/bibby/
# Bibby.jar
```

---

## 8. Orchestration Readiness

### 8.1 12-Factor App Principles

Ensure Bibby follows 12-factor methodology for container orchestration:

**I. Codebase** ✓
- One codebase (Git), many deploys

**II. Dependencies** ✓
- Declared in `pom.xml`, isolated in container

**III. Config** ✓
- Environment variables (`DATABASE_URL`, etc.)

**IV. Backing Services** ✓
- PostgreSQL treated as attached resource

**V. Build, Release, Run** ✓
- Strict separation (multi-stage Dockerfile)

**VI. Processes** ✓
- Stateless (data in PostgreSQL, not container)

**VII. Port Binding** ✓
- Self-contained (embedded Tomcat on port 8080)

**VIII. Concurrency** ✓
- Scale via container replication

**IX. Disposability** ⚠️  **Needs improvement**
- Fast startup: ✓ (~30s)
- Graceful shutdown: ❌ **Need to handle SIGTERM**

**X. Dev/Prod Parity** ✓
- Same container in dev/prod

**XI. Logs** ✓
- Stream to STDOUT (Docker captures)

**XII. Admin Processes** ✓
- Run via `docker exec` or separate containers

### 8.2 Graceful Shutdown

**Problem**: `docker stop` sends SIGTERM, waits 10s, then SIGKILL.

Spring Boot needs to:
1. Stop accepting new requests
2. Finish processing existing requests
3. Close database connections
4. Exit cleanly

**Solution: Enable Graceful Shutdown in Spring Boot**

**Update `application.properties`:**

```properties
# Graceful shutdown with 30s timeout
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

**How It Works:**

```bash
docker stop bibby-prod
# 1. Docker sends SIGTERM to PID 1 (Java)
# 2. Spring Boot receives shutdown signal
# 3. Stops accepting new requests (returns 503)
# 4. Waits for in-flight requests (max 30s)
# 5. Closes database connections
# 6. Exits with code 0
# 7. Container stops
```

**Test:**

```bash
# Terminal 1: Start long-running request
curl http://localhost:8080/api/slow-operation

# Terminal 2: Stop container
docker stop bibby-prod

# Terminal 1: Request completes successfully
# Then connection closed

# Terminal 2: Container exits cleanly
docker inspect bibby-prod --format='{{.State.ExitCode}}'
# 0 (success)
```

### 8.3 Signal Handling

**Dockerfile Considerations:**

```dockerfile
# ❌ Bad: Shell form (doesn't propagate signals)
ENTRYPOINT java -jar Bibby.jar

# ❌ Bad: Shell script (needs exec)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar Bibby.jar"]
# PID 1 is 'sh', not 'java' → signals go to shell

# ✅ Good: Exec in shell script
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar Bibby.jar"]
# 'exec' replaces shell with Java → Java is PID 1

# ✅ Better: Exec form (if no variable expansion needed)
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "Bibby.jar"]
```

**Current Bibby Dockerfile:**

```dockerfile
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar Bibby.jar"]
```

**Improved:**

```dockerfile
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar Bibby.jar"]
```

**Verify PID 1:**

```bash
docker exec bibby-prod ps aux
# PID   USER     COMMAND
# 1     bibby    java -jar Bibby.jar  ✓ (Java is PID 1)
```

### 8.4 Horizontal Scaling

**Run Multiple Bibby Instances:**

```bash
# Instance 1
docker run -d \
  --name bibby-1 \
  --network bibby-network \
  -p 8081:8080 \
  bibby:0.3.0

# Instance 2
docker run -d \
  --name bibby-2 \
  --network bibby-network \
  -p 8082:8080 \
  bibby:0.3.0

# Instance 3
docker run -d \
  --name bibby-3 \
  --network bibby-network \
  -p 8083:8080 \
  bibby:0.3.0
```

**Load Balancer (Nginx):**

```nginx
# /etc/nginx/conf.d/bibby.conf
upstream bibby_backend {
    least_conn;
    server bibby-1:8080;
    server bibby-2:8080;
    server bibby-3:8080;
}

server {
    listen 80;

    location / {
        proxy_pass http://bibby_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /actuator/health {
        proxy_pass http://bibby_backend/actuator/health;
    }
}
```

```bash
docker run -d \
  --name nginx-lb \
  --network bibby-network \
  -p 80:80 \
  -v /etc/nginx/conf.d:/etc/nginx/conf.d:ro \
  nginx:alpine
```

**Requests now distributed** across 3 Bibby instances!

### 8.5 Kubernetes Readiness

**Translation: Docker → Kubernetes**

```yaml
# bibby-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bibby
spec:
  replicas: 3  # 3 instances
  selector:
    matchLabels:
      app: bibby
  template:
    metadata:
      labels:
        app: bibby
    spec:
      containers:
      - name: bibby
        image: bibby:0.3.0

        # Resource limits (from docker run --memory --cpus)
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "640Mi"
            cpu: "2000m"

        # Port (from -p 8080:8080)
        ports:
        - containerPort: 8080
          name: http

        # Environment (from -e)
        env:
        - name: DATABASE_URL
          value: "jdbc:postgresql://postgres:5432/bibby"
        - name: SPRING_PROFILES_ACTIVE
          value: "production"

        # Health checks
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 40
          periodSeconds: 30
          timeoutSeconds: 3
          failureThreshold: 3

        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 3

        # Graceful shutdown
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 15"]
```

**All Docker concepts translate to Kubernetes!**

---

## 9. Interview-Ready Knowledge

### Question 1: "Explain the container lifecycle and how signals work."

**Answer:**

"Containers go through several states: created, running, paused, stopped, and dead. Understanding the lifecycle is critical for proper production operations.

In our Bibby library management system, when we run `docker stop bibby-prod`, Docker sends SIGTERM (signal 15) to the process with PID 1 in the container. By default, Docker waits 10 seconds for graceful shutdown before sending SIGKILL (signal 9).

The key challenge is ensuring the application receives these signals correctly. In our Dockerfile, we use:

```dockerfile
ENTRYPOINT [\"sh\", \"-c\", \"exec java $JAVA_OPTS -jar Bibby.jar\"]
```

The `exec` keyword is critical—it replaces the shell process with the Java process, making Java PID 1. Without `exec`, the shell would be PID 1, and signals might not propagate correctly to the Java application.

We also configured Spring Boot for graceful shutdown:

```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

This ensures that when Docker sends SIGTERM:
1. Spring stops accepting new requests
2. Existing requests are completed (up to 30 seconds)
3. Database connections are closed cleanly
4. The application exits with code 0

We increased Docker's stop timeout to 40 seconds to accommodate this:

```bash
docker stop --time 40 bibby-prod
```

This prevents requests from being interrupted mid-processing and ensures data consistency."

### Question 2: "How do you size resource limits for a Java container?"

**Answer:**

"For Java applications like our Bibby system, resource sizing requires understanding both the JVM and container limits.

**Memory Sizing:**

The container needs memory for:
- JVM heap (controlled by -Xms/-Xmx)
- Metaspace (class metadata)
- Thread stacks
- Direct buffers
- OS buffers

For Bibby, we configured:

```bash
docker run --memory=640m \
  -e JAVA_OPTS=\"-Xms256m -Xmx512m -XX:MaxMetaspaceSize=128m\" \
  bibby:0.3.0
```

The calculation:
- Max heap: 512MB
- Metaspace: 128MB (Spring Boot needs ~100MB)
- Overhead: ~20% = 128MB
- Total: 512 + 128 + 128 = 768MB

We set the container limit to 640MB, which is slightly tight but prevents runaway memory growth. If the JVM tries to exceed this, the container is OOM killed, which is preferable to swapping and degrading host performance.

I validated this by load testing and monitoring with `docker stats`:

```bash
docker stats bibby-prod
# MEM USAGE: 580MiB / 640MiB (90%)
```

Running at 90% utilization is acceptable for a right-sized container.

**CPU Sizing:**

Spring Boot is generally not CPU-intensive for CRUD operations. We set:

```bash
--cpus=2.0
```

This allows bursting to 2 full cores during startup and heavy traffic, while typically using <0.5 cores at steady state.

I use CPU shares for relative priority rather than hard limits in production:

```bash
--cpu-shares=1024  # Standard priority
```

This allows Bibby to use available CPU when the host is idle, but yields to higher-priority containers under contention."

### Question 3: "How would you debug a container that's restarting constantly?"

**Answer:**

"I follow a systematic approach when diagnosing restart loops. Recently, we had this exact issue with Bibby in our staging environment.

**Step 1: Check the exit code**

```bash
docker inspect bibby-prod --format='{{.State.ExitCode}}'
# 1
```

Exit code 1 indicates an application error (not infrastructure like OOM=137).

**Step 2: View logs**

```bash
docker logs --tail 100 bibby-prod
```

This revealed:

```
org.postgresql.util.PSQLException: Connection to postgres:5432 refused
```

The database wasn't available.

**Step 3: Check restart count and timing**

```bash
docker inspect bibby-prod --format='{{.RestartCount}}'
# 47

docker inspect bibby-prod --format='{{.State.StartedAt}} {{.State.FinishedAt}}'
# 2025-01-17T10:30:45Z 2025-01-17T10:30:50Z
```

Container restarted 47 times, living only 5 seconds each time—classic startup failure.

**Step 4: Prevent automatic restart to investigate**

```bash
docker update --restart no bibby-prod
```

This stops the restart loop so I can debug.

**Step 5: Verify dependencies**

```bash
docker exec postgres psql -U bibby_admin -d bibby -c 'SELECT 1'
# psql: error: connection refused
```

Database container wasn't running! Started it:

```bash
docker start postgres
```

**Step 6: Verify fix**

```bash
docker start bibby-prod
docker logs -f bibby-prod
# Started BibbyApplication in 3.456 seconds
```

**Step 7: Restore restart policy**

```bash
docker update --restart unless-stopped bibby-prod
```

**Lessons learned:**

1. Dependencies must start before the application (use health checks + `depends_on` in docker-compose)
2. Implement retry logic with exponential backoff for transient failures
3. Use health checks to distinguish 'running' from 'ready'
4. Monitor restart counts as an early warning signal

For Bibby, I added connection retry logic and increased the health check `start-period` to 60 seconds to allow for slower database startups."

---

## Summary

**What You Learned:**

1. **Container Lifecycle States**
   - Created → Running → Paused → Stopped → Dead → Removed
   - Exit codes: 0 (success), 1 (error), 137 (SIGKILL/OOM), 143 (SIGTERM)
   - Signal handling with PID 1 and `exec`

2. **docker run Mastery**
   - Naming (`--name`), ports (`-p`), environment (`-e`), volumes (`-v`)
   - Restart policies: `unless-stopped` for production
   - Resource limits: `--memory=640m --cpus=2.0` for Bibby

3. **Resource Limits**
   - Memory: 640MB container (512MB heap + 128MB overhead)
   - CPU: 2.0 cores max, shares for relative priority
   - OOM killer behavior and prevention

4. **Logging Strategies**
   - JSON file with rotation (`max-size=10m`, `max-file=3`)
   - Structured logging with Logback JSON encoder
   - Centralized logging: Fluentd → Elasticsearch → Kibana
   - AWS CloudWatch integration

5. **Health Checks**
   - Dockerfile HEALTHCHECK vs runtime checks
   - Spring Boot Actuator: `/actuator/health`
   - Liveness, readiness, startup probes
   - Custom health indicators for domain logic

6. **Monitoring**
   - `docker stats` for real-time metrics
   - Prometheus + Grafana for visualization
   - cAdvisor for container-level monitoring
   - Micrometer for JVM metrics

7. **Debugging**
   - Logs, exec, inspect, events, attach
   - Copy files to/from containers
   - Export filesystem for forensics

8. **Orchestration Readiness**
   - 12-factor app compliance
   - Graceful shutdown (30s timeout)
   - Horizontal scaling with load balancing
   - Kubernetes translation

**Production-Ready Bibby Configuration:**

```bash
docker run -d \
  --name bibby-prod \
  --restart unless-stopped \
  --network bibby-network \
  -p 127.0.0.1:8080:8080 \
  --memory=640m \
  --memory-reservation=512m \
  --cpus=2.0 \
  -e DATABASE_URL=jdbc:postgresql://bibby-db:5432/bibby \
  -e SPRING_PROFILES_ACTIVE=production \
  -e JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC" \
  --mount type=volume,source=bibby-logs,target=/opt/bibby/logs \
  --log-driver json-file \
  --log-opt max-size=10m \
  --log-opt max-file=3 \
  --health-cmd="wget --spider http://localhost:8080/actuator/health || exit 1" \
  --health-interval=30s \
  --health-start-period=40s \
  bibby:0.3.0
```

**Next Section Preview:**

Section 19 will cover **Docker Registries & Image Management**—pushing images to Docker Hub, private registries (AWS ECR, GCR, Harbor), image scanning, vulnerability management, and CI/CD integration.

**Key Takeaway:**

Running containers in production is far more than `docker run`. Proper resource limits, health checks, logging, and monitoring are the difference between "it works on my machine" and true production readiness. Every configuration choice has operational implications.
