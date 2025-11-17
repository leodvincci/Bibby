# Section 20: Docker Compose for Local Development

## Introduction

Running Bibby locally currently requires:

```bash
# Terminal 1: Start PostgreSQL
docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=password postgres:15-alpine

# Terminal 2: Wait for DB, then start Bibby
docker run -d -p 8080:8080 -e DATABASE_URL=jdbc:postgresql://localhost:5432/bibby bibby:0.3.0

# Terminal 3: Maybe pgAdmin for database inspection?
docker run -d -p 5050:80 -e PGADMIN_DEFAULT_EMAIL=admin@bibby.com dpage/pgadmin4
```

**Problems:**

1. **Manual orchestration**: Start services in correct order
2. **Fragile networking**: localhost references break in containers
3. **No persistence**: Database data lost on container restart
4. **Configuration sprawl**: Environment variables scattered across terminals
5. **Difficult sharing**: Can't easily share setup with team

**Solution: Docker Compose** - Declarative multi-container orchestration.

```bash
# One command starts entire stack
docker compose up

# Output:
# ✅ PostgreSQL ready on port 5432
# ✅ Bibby connected to database
# ✅ pgAdmin accessible at http://localhost:5050
```

This section teaches you to build **production-like development environments** using Docker Compose.

**What You'll Learn:**

1. **Compose fundamentals** - YAML syntax, services, networks, volumes
2. **Complete Bibby stack** - App + PostgreSQL + pgAdmin + monitoring
3. **Networking** - Service discovery, DNS, port mapping
4. **Volumes** - Data persistence, bind mounts, named volumes
5. **Environment configuration** - .env files, overrides, profiles
6. **Development workflows** - Hot reload, debugging, live code sync
7. **Multi-environment setup** - Dev vs production compose files
8. **Debugging techniques** - Logs, exec, inspect, troubleshooting
9. **Best practices** - Security, performance, team workflows

**Prerequisites**: Sections 16-19 (Docker fundamentals through registries)

---

## 1. Docker Compose Fundamentals

### 1.1 What is Docker Compose?

**Docker Compose** is a tool for defining and running multi-container applications using a YAML file.

**Key Concepts:**

- **Service**: A container definition (e.g., "postgres", "bibby-app")
- **Network**: Virtual network connecting services
- **Volume**: Persistent or shared storage
- **Compose file**: `docker-compose.yml` (or `compose.yaml`)

**Install Docker Compose:**

```bash
# Docker Desktop (Mac/Windows): Already included

# Linux: Install plugin
sudo apt-get update
sudo apt-get install docker-compose-plugin

# Verify
docker compose version
# Docker Compose version v2.24.0
```

**Note**: Modern Docker uses `docker compose` (with space), not `docker-compose` (hyphen).

### 1.2 Basic Compose File Structure

```yaml
version: '3.9'  # Optional in modern Compose

services:
  service-name:
    image: image:tag
    # OR
    build: ./path/to/dockerfile
    ports:
      - "host:container"
    environment:
      - KEY=value
    volumes:
      - volume-name:/container/path
    networks:
      - network-name
    depends_on:
      - other-service

networks:
  network-name:
    driver: bridge

volumes:
  volume-name:
    driver: local
```

### 1.3 Simple Example: Bibby + PostgreSQL

**Create `docker-compose.yml` in Bibby root:**

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:15-alpine
    container_name: bibby-postgres
    environment:
      POSTGRES_DB: bibby
      POSTGRES_USER: bibby_admin
      POSTGRES_PASSWORD: dev_password_123
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  app:
    build: .
    container_name: bibby-app
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/bibby
      DATABASE_USER: bibby_admin
      DATABASE_PASSWORD: dev_password_123
    depends_on:
      - postgres

volumes:
  postgres-data:
```

**Start the stack:**

```bash
docker compose up
```

**Output:**

```
[+] Running 3/3
 ✔ Network bibby_default        Created
 ✔ Container bibby-postgres     Created
 ✔ Container bibby-app          Created
Attaching to bibby-app, bibby-postgres

bibby-postgres  | PostgreSQL init process complete; ready for start up.
bibby-postgres  | database system is ready to accept connections
bibby-app       | Started BibbyApplication in 3.456 seconds
```

**Access Bibby:**

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

**Stop the stack:**

```bash
# Ctrl+C in terminal

# Or detached mode
docker compose down
```

**Key Observations:**

1. **Automatic network**: `bibby_default` created, both containers connected
2. **Service DNS**: App references database as `postgres` (not `localhost`)
3. **Startup order**: `depends_on` ensures postgres starts before app
4. **Persistent data**: `postgres-data` volume survives container restarts

---

## 2. Complete Bibby Development Stack

### 2.1 Full Stack Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Docker Compose Stack                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │    Bibby     │───→│  PostgreSQL  │    │   pgAdmin    │ │
│  │     App      │    │   Database   │←───│   Web UI     │ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
│       :8080               :5432                :5050       │
│                                                             │
│  ┌──────────────┐    ┌──────────────┐                     │
│  │  Prometheus  │───→│   Grafana    │                     │
│  │   Metrics    │    │  Dashboard   │                     │
│  └──────────────┘    └──────────────┘                     │
│       :9090               :3000                            │
│                                                             │
│               All connected via bibby-network              │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Complete docker-compose.yml

**Create comprehensive development stack:**

```yaml
version: '3.9'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:15-alpine
    container_name: bibby-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME:-bibby}
      POSTGRES_USER: ${DB_USER:-bibby_admin}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-dev_password_123}
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8 --lc-collate=en_US.UTF-8 --lc-ctype=en_US.UTF-8"
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./docker/postgres/init:/docker-entrypoint-initdb.d:ro
    networks:
      - bibby-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-bibby_admin} -d ${DB_NAME:-bibby}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s

  # Bibby Application
  app:
    build:
      context: .
      dockerfile: Dockerfile
      target: runtime
      args:
        VERSION: ${APP_VERSION:-0.3.0}
    image: bibby:${APP_VERSION:-0.3.0}
    container_name: bibby-app
    restart: unless-stopped
    ports:
      - "${APP_PORT:-8080}:8080"
    environment:
      # Database connection
      DATABASE_URL: jdbc:postgresql://postgres:5432/${DB_NAME:-bibby}
      DATABASE_USER: ${DB_USER:-bibby_admin}
      DATABASE_PASSWORD: ${DB_PASSWORD:-dev_password_123}

      # Spring profiles
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILE:-development}

      # JVM tuning
      JAVA_OPTS: >-
        -Xms256m
        -Xmx512m
        -XX:+UseG1GC
        -XX:MaxGCPauseMillis=200
        -Djava.security.egd=file:/dev/./urandom

      # Logging
      LOGGING_LEVEL_ROOT: ${LOG_LEVEL:-INFO}
      LOGGING_LEVEL_COM_PENROSE_BIBBY: ${LOG_LEVEL_APP:-DEBUG}
    volumes:
      # Application logs
      - app-logs:/opt/bibby/logs

      # Hot reload (development only)
      - ./target/classes:/opt/bibby/classes:ro
    networks:
      - bibby-network
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  # pgAdmin for database management
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: bibby-pgadmin
    restart: unless-stopped
    environment:
      PGADMIN_DEFAULT_EMAIL: ${PGADMIN_EMAIL:-admin@bibby.local}
      PGADMIN_DEFAULT_PASSWORD: ${PGADMIN_PASSWORD:-admin}
      PGADMIN_CONFIG_SERVER_MODE: 'False'
      PGADMIN_CONFIG_MASTER_PASSWORD_REQUIRED: 'False'
    ports:
      - "${PGADMIN_PORT:-5050}:80"
    volumes:
      - pgadmin-data:/var/lib/pgadmin
      - ./docker/pgadmin/servers.json:/pgadmin4/servers.json:ro
    networks:
      - bibby-network
    depends_on:
      - postgres

  # Prometheus for metrics collection
  prometheus:
    image: prom/prometheus:latest
    container_name: bibby-prometheus
    restart: unless-stopped
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'
    ports:
      - "${PROMETHEUS_PORT:-9090}:9090"
    volumes:
      - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    networks:
      - bibby-network
    depends_on:
      - app

  # Grafana for visualization
  grafana:
    image: grafana/grafana:latest
    container_name: bibby-grafana
    restart: unless-stopped
    environment:
      GF_SECURITY_ADMIN_USER: ${GRAFANA_USER:-admin}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD:-admin}
      GF_INSTALL_PLUGINS: ''
    ports:
      - "${GRAFANA_PORT:-3000}:3000"
    volumes:
      - grafana-data:/var/lib/grafana
      - ./docker/grafana/provisioning:/etc/grafana/provisioning:ro
      - ./docker/grafana/dashboards:/var/lib/grafana/dashboards:ro
    networks:
      - bibby-network
    depends_on:
      - prometheus

networks:
  bibby-network:
    driver: bridge
    name: bibby-network

volumes:
  postgres-data:
    driver: local
  pgadmin-data:
    driver: local
  prometheus-data:
    driver: local
  grafana-data:
    driver: local
  app-logs:
    driver: local
```

### 2.3 Environment Configuration (.env)

**Create `.env` file:**

```bash
# Application
APP_VERSION=0.3.0
APP_PORT=8080
SPRING_PROFILE=development

# Database
DB_NAME=bibby
DB_USER=bibby_admin
DB_PASSWORD=dev_password_123
DB_PORT=5432

# pgAdmin
PGADMIN_EMAIL=admin@bibby.local
PGADMIN_PASSWORD=admin
PGADMIN_PORT=5050

# Prometheus
PROMETHEUS_PORT=9090

# Grafana
GRAFANA_USER=admin
GRAFANA_PASSWORD=admin
GRAFANA_PORT=3000

# Logging
LOG_LEVEL=INFO
LOG_LEVEL_APP=DEBUG
```

**Add to `.gitignore`:**

```bash
# Environment files
.env
.env.local
.env.*.local

# Docker volumes
docker/postgres/data/
docker/grafana/data/
```

**Provide example file:**

```bash
cp .env .env.example

# Edit .env.example to remove sensitive values
# DB_PASSWORD=CHANGE_ME_IN_DOT_ENV
```

### 2.4 Supporting Configuration Files

**Create directory structure:**

```bash
mkdir -p docker/{postgres/init,pgadmin,prometheus,grafana/{provisioning,dashboards}}
```

**PostgreSQL init script** (`docker/postgres/init/01-init.sql`):

```sql
-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create schema
CREATE SCHEMA IF NOT EXISTS bibby;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA bibby TO bibby_admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA bibby TO bibby_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA bibby TO bibby_admin;

-- Seed development data (optional)
-- INSERT INTO bibby.books (title, isbn) VALUES ('Test Book', '978-0-123456-78-9');
```

**pgAdmin servers config** (`docker/pgadmin/servers.json`):

```json
{
  "Servers": {
    "1": {
      "Name": "Bibby Local",
      "Group": "Servers",
      "Host": "postgres",
      "Port": 5432,
      "MaintenanceDB": "bibby",
      "Username": "bibby_admin",
      "SSLMode": "prefer",
      "PassFile": "/tmp/pgpassfile"
    }
  }
}
```

**Prometheus config** (`docker/prometheus/prometheus.yml`):

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'bibby-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
        labels:
          application: 'bibby'
          environment: 'development'

  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

**Grafana datasource** (`docker/grafana/provisioning/datasources/prometheus.yml`):

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
```

---

## 3. Development Workflows

### 3.1 Starting the Stack

**First-time setup:**

```bash
# Build images
docker compose build

# Start all services
docker compose up -d

# View logs
docker compose logs -f

# Check service health
docker compose ps
```

**Output:**

```
NAME               IMAGE                        STATUS              PORTS
bibby-app          bibby:0.3.0                  Up 2 minutes (healthy)  0.0.0.0:8080->8080/tcp
bibby-grafana      grafana/grafana:latest       Up 2 minutes        0.0.0.0:3000->3000/tcp
bibby-pgadmin      dpage/pgadmin4:latest        Up 2 minutes        0.0.0.0:5050->80/tcp
bibby-postgres     postgres:15-alpine           Up 2 minutes (healthy)  0.0.0.0:5432->5432/tcp
bibby-prometheus   prom/prometheus:latest       Up 2 minutes        0.0.0.0:9090->9090/tcp
```

**Access services:**

- Bibby App: http://localhost:8080
- pgAdmin: http://localhost:5050
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

### 3.2 Daily Development Workflow

**Morning: Start stack**

```bash
docker compose up -d
```

**During development:**

```bash
# Make code changes in IDE
# ... edit src/main/java/com/penrose/bibby/...

# Rebuild just the app
docker compose build app
docker compose up -d app

# Or rebuild and restart in one command
docker compose up -d --build app

# View app logs
docker compose logs -f app

# View all logs
docker compose logs -f
```

**End of day: Stop stack**

```bash
# Stop containers (keep volumes)
docker compose stop

# Stop and remove containers (keep volumes)
docker compose down

# Stop, remove containers AND volumes (fresh start)
docker compose down -v
```

### 3.3 Hot Reload Development

**For Java Spring Boot**, use Spring Boot DevTools:

**Update `pom.xml`:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

**Update Dockerfile for development:**

**Create `Dockerfile.dev`:**

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17-alpine

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Expose port and debug port
EXPOSE 8080 5005

# Run with DevTools and remote debugging
CMD ["mvn", "spring-boot:run", \
     "-Dspring-boot.run.jvmArguments='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'"]
```

**Create `docker-compose.dev.yml`:**

```yaml
version: '3.9'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile.dev
    volumes:
      # Live code sync
      - ./src:/app/src:ro
      - ./pom.xml:/app/pom.xml:ro
      # Maven cache
      - maven-cache:/root/.m2
    ports:
      - "8080:8080"
      - "5005:5005"  # Debug port
    environment:
      SPRING_DEVTOOLS_RESTART_ENABLED: "true"
      SPRING_DEVTOOLS_LIVERELOAD_ENABLED: "true"

volumes:
  maven-cache:
```

**Run development stack:**

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
```

**Now: Edit code → Auto-reload!**

```bash
# Edit BookController.java
# Save file
# DevTools detects change and reloads application (3-5 seconds)
```

### 3.4 Remote Debugging

**Connect IntelliJ IDEA:**

1. Run → Edit Configurations
2. Add New Configuration → Remote JVM Debug
3. Host: `localhost`
4. Port: `5005`
5. Click "Debug"

**Set breakpoints in code, trigger endpoint:**

```bash
curl http://localhost:8080/api/books
# IntelliJ pauses at breakpoint!
```

### 3.5 Running Specific Services

```bash
# Start only database and pgAdmin
docker compose up -d postgres pgadmin

# Start app locally (outside Docker)
mvn spring-boot:run -Dspring-boot.run.arguments='--spring.datasource.url=jdbc:postgresql://localhost:5432/bibby'

# Benefits:
# - Faster iteration (no Docker rebuild)
# - Full IDE debugging
# - Database still containerized
```

### 3.6 Database Operations

**Access PostgreSQL shell:**

```bash
docker compose exec postgres psql -U bibby_admin -d bibby
```

**Inside psql:**

```sql
-- List tables
\dt

-- Query books
SELECT * FROM books LIMIT 10;

-- Check connection count
SELECT count(*) FROM pg_stat_activity;

-- Exit
\q
```

**Run SQL script:**

```bash
docker compose exec -T postgres psql -U bibby_admin -d bibby < migration.sql
```

**Backup database:**

```bash
docker compose exec -T postgres pg_dump -U bibby_admin bibby > backup.sql
```

**Restore database:**

```bash
docker compose exec -T postgres psql -U bibby_admin -d bibby < backup.sql
```

**Reset database:**

```bash
# Stop app
docker compose stop app

# Drop and recreate database
docker compose exec postgres psql -U bibby_admin -d postgres -c "DROP DATABASE bibby;"
docker compose exec postgres psql -U bibby_admin -d postgres -c "CREATE DATABASE bibby;"

# Restart app (runs migrations)
docker compose up -d app
```

---

## 4. Networking in Compose

### 4.1 Default Network Behavior

When you run `docker compose up`, Compose automatically:

1. Creates a network named `{project}_default` (e.g., `bibby_default`)
2. Connects all services to this network
3. Enables DNS resolution (services can reach each other by name)

**Example:**

```yaml
services:
  postgres:
    image: postgres:15-alpine

  app:
    image: bibby:0.3.0
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/bibby
      #                                 ^^^^^^^^ Service name resolves to container IP
```

### 4.2 Custom Networks

**Explicit network definition:**

```yaml
services:
  postgres:
    networks:
      - backend

  app:
    networks:
      - backend
      - frontend

  nginx:
    networks:
      - frontend

networks:
  backend:
    driver: bridge
    internal: true  # No external access

  frontend:
    driver: bridge
```

**Result:**
- `postgres` and `app` can communicate (both on `backend`)
- `nginx` and `app` can communicate (both on `frontend`)
- `postgres` and `nginx` **cannot** communicate (no shared network)

### 4.3 Network Aliases

**Multiple DNS names for one service:**

```yaml
services:
  postgres:
    networks:
      backend:
        aliases:
          - database
          - db
          - postgres-primary

  app:
    environment:
      # All these work:
      DATABASE_URL: jdbc:postgresql://postgres:5432/bibby
      # OR
      DATABASE_URL: jdbc:postgresql://database:5432/bibby
      # OR
      DATABASE_URL: jdbc:postgresql://db:5432/bibby
```

### 4.4 External Networks

**Connect to existing network:**

```yaml
networks:
  existing-network:
    external: true
    name: my-preexisting-network

services:
  app:
    networks:
      - existing-network
```

**Use case**: Connect to containers started outside Compose.

### 4.5 Port Mapping vs Expose

```yaml
services:
  # Published to host (accessible from host machine)
  app:
    ports:
      - "8080:8080"  # Host 8080 → Container 8080

  # Only exposed within network (not accessible from host)
  internal-service:
    expose:
      - "8080"  # Other containers can reach internal-service:8080
                # Host cannot reach localhost:8080
```

**Best Practice**: Only publish ports for services you need to access from host.

### 4.6 Bibby Network Architecture

```yaml
networks:
  # Frontend network (public-facing)
  frontend:
    driver: bridge

  # Backend network (internal)
  backend:
    driver: bridge
    internal: true

services:
  # Public-facing reverse proxy
  nginx:
    networks:
      - frontend
    ports:
      - "80:80"

  # Application (bridges frontend and backend)
  app:
    networks:
      - frontend
      - backend
    expose:
      - "8080"  # Not published to host

  # Database (backend only)
  postgres:
    networks:
      - backend
    expose:
      - "5432"  # Not accessible from host or frontend

  # Admin tools (backend only)
  pgadmin:
    networks:
      - backend
    ports:
      - "5050:80"  # Exception: needed for dev access
```

---

## 5. Volumes and Data Persistence

### 5.1 Volume Types

**Named Volumes** (Docker-managed):

```yaml
services:
  postgres:
    volumes:
      - postgres-data:/var/lib/postgresql/data

volumes:
  postgres-data:  # Docker manages storage location
```

**Bind Mounts** (Host directory):

```yaml
services:
  app:
    volumes:
      - ./src:/app/src:ro  # Host ./src → Container /app/src (read-only)
      - ./logs:/app/logs   # Host ./logs → Container /app/logs (read-write)
```

**Anonymous Volumes**:

```yaml
services:
  app:
    volumes:
      - /app/temp  # Docker creates random volume
```

### 5.2 When to Use Each Type

| Type | Use Case | Example |
|------|----------|---------|
| Named Volume | Persistent data, managed by Docker | Database files, uploaded content |
| Bind Mount | Live code sync, configuration | Source code during development, config files |
| Anonymous Volume | Temporary data, caching | Build artifacts, temp files |

### 5.3 Volume Management

**List volumes:**

```bash
docker volume ls
```

**Inspect volume:**

```bash
docker volume inspect bibby_postgres-data

# Output:
[
  {
    "Driver": "local",
    "Mountpoint": "/var/lib/docker/volumes/bibby_postgres-data/_data",
    "Name": "bibby_postgres-data"
  }
]
```

**View volume contents:**

```bash
# On Linux
sudo ls -la /var/lib/docker/volumes/bibby_postgres-data/_data

# On Mac/Windows (Docker Desktop)
docker run --rm -v bibby_postgres-data:/data alpine ls -la /data
```

**Backup volume:**

```bash
docker run --rm \
  -v bibby_postgres-data:/source:ro \
  -v $(pwd):/backup \
  alpine tar czf /backup/postgres-backup.tar.gz -C /source .
```

**Restore volume:**

```bash
docker run --rm \
  -v bibby_postgres-data:/target \
  -v $(pwd):/backup \
  alpine sh -c "cd /target && tar xzf /backup/postgres-backup.tar.gz"
```

**Clean up volumes:**

```bash
# Remove volumes for stopped containers
docker compose down -v

# Remove all unused volumes
docker volume prune
```

### 5.4 Bibby Volume Strategy

```yaml
volumes:
  # Persistent data (keep across restarts)
  postgres-data:
    driver: local
    driver_opts:
      type: none
      device: ${PWD}/docker/postgres/data
      o: bind

  # Application logs (for analysis)
  app-logs:
    driver: local

  # Grafana dashboards (customization)
  grafana-data:
    driver: local

  # Prometheus metrics (time-series data)
  prometheus-data:
    driver: local

  # Maven cache (speed up builds)
  maven-cache:
    driver: local
```

### 5.5 Read-Only Mounts

**Security: Prevent container from modifying host files**

```yaml
services:
  app:
    volumes:
      - ./config/application.yml:/app/config/application.yml:ro
      - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      #                                                                    ^^^ read-only
```

**Use cases:**
- Configuration files
- SSL certificates
- Static content

---

## 6. Environment-Specific Configurations

### 6.1 Compose Override Files

**Structure:**

```
docker-compose.yml          # Base configuration
docker-compose.override.yml # Development overrides (auto-loaded)
docker-compose.prod.yml     # Production overrides (explicit)
docker-compose.test.yml     # Testing overrides (explicit)
```

**Base: `docker-compose.yml`**

```yaml
version: '3.9'

services:
  app:
    image: bibby:${APP_VERSION:-0.3.0}
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/bibby
```

**Development: `docker-compose.override.yml`** (automatically loaded):

```yaml
version: '3.9'

services:
  app:
    build: .  # Override image with local build
    volumes:
      - ./src:/app/src:ro  # Hot reload
    ports:
      - "5005:5005"  # Debug port
    environment:
      SPRING_PROFILES_ACTIVE: development
      LOGGING_LEVEL_COM_PENROSE_BIBBY: DEBUG
```

**Production: `docker-compose.prod.yml`** (explicit):

```yaml
version: '3.9'

services:
  app:
    image: 123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:${APP_VERSION}
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: production
      LOGGING_LEVEL_COM_PENROSE_BIBBY: INFO
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 640M
        reservations:
          cpus: '1.0'
          memory: 512M
```

**Usage:**

```bash
# Development (auto-loads override)
docker compose up

# Production
docker compose -f docker-compose.yml -f docker-compose.prod.yml up

# Testing
docker compose -f docker-compose.yml -f docker-compose.test.yml up
```

### 6.2 Environment Files

**Structure:**

```
.env                 # Default (gitignored)
.env.example         # Template (committed)
.env.development     # Dev-specific
.env.production      # Prod-specific (gitignored)
```

**Specify env file:**

```bash
docker compose --env-file .env.production up
```

### 6.3 Profiles (Compose v1.28+)

**Conditional service activation:**

```yaml
services:
  app:
    # Always runs
    image: bibby:0.3.0

  postgres:
    # Always runs
    image: postgres:15-alpine

  pgadmin:
    # Only runs with 'tools' profile
    image: dpage/pgadmin4
    profiles:
      - tools

  prometheus:
    # Only runs with 'monitoring' profile
    image: prom/prometheus
    profiles:
      - monitoring

  grafana:
    # Only runs with 'monitoring' profile
    image: grafana/grafana
    profiles:
      - monitoring
```

**Usage:**

```bash
# Start only app and postgres
docker compose up

# Start with pgAdmin
docker compose --profile tools up

# Start with monitoring
docker compose --profile monitoring up

# Start with all profiles
docker compose --profile tools --profile monitoring up
```

### 6.4 Complete Multi-Environment Setup for Bibby

**`.env.development`:**

```bash
COMPOSE_PROJECT_NAME=bibby-dev
APP_VERSION=latest
SPRING_PROFILE=development
LOG_LEVEL=DEBUG
DB_PASSWORD=dev_password_123
```

**`.env.production`:**

```bash
COMPOSE_PROJECT_NAME=bibby-prod
APP_VERSION=0.3.0
SPRING_PROFILE=production
LOG_LEVEL=INFO
DB_PASSWORD=${PROD_DB_PASSWORD}  # From secrets manager
```

**Command:**

```bash
# Development
docker compose --env-file .env.development up

# Production
docker compose --env-file .env.production \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  up -d
```

---

## 7. Debugging Multi-Container Applications

### 7.1 View Logs

```bash
# All services
docker compose logs

# Follow logs (live)
docker compose logs -f

# Specific service
docker compose logs -f app

# Last 100 lines
docker compose logs --tail=100 app

# Since timestamp
docker compose logs --since 2025-01-17T10:00:00 app

# Multiple services
docker compose logs -f app postgres
```

### 7.2 Service Status

```bash
# List running containers
docker compose ps

# All containers (including stopped)
docker compose ps -a

# Service details
docker compose ps app

# Output:
NAME        IMAGE          STATUS                  PORTS
bibby-app   bibby:0.3.0    Up 10 minutes (healthy) 0.0.0.0:8080->8080/tcp
```

### 7.3 Execute Commands

```bash
# Shell access
docker compose exec app sh

# Inside container
/opt/bibby # ps aux
/opt/bibby # netstat -tlnp
/opt/bibby # env | grep DATABASE

# One-off command
docker compose exec app curl http://localhost:8080/actuator/health

# PostgreSQL access
docker compose exec postgres psql -U bibby_admin -d bibby

# Run command in new container
docker compose run --rm app sh
```

### 7.4 Network Debugging

```bash
# Check network connectivity
docker compose exec app ping postgres
docker compose exec app curl http://postgres:5432

# DNS resolution
docker compose exec app nslookup postgres
docker compose exec app getent hosts postgres

# Network inspection
docker network inspect bibby_bibby-network

# Test database connection
docker compose exec app sh -c 'echo "SELECT 1" | psql -h postgres -U bibby_admin -d bibby'
```

### 7.5 Common Issues and Solutions

**Issue 1: "Connection refused" to postgres**

```bash
# Check if postgres is healthy
docker compose ps postgres
# STATUS: Up 30 seconds (health: starting)

# Wait for health check
docker compose logs -f postgres
# Wait for: "database system is ready to accept connections"

# Or use depends_on with condition
```

```yaml
services:
  app:
    depends_on:
      postgres:
        condition: service_healthy  # Wait for health check
```

**Issue 2: "Port already allocated"**

```bash
# Error: bind: address already in use

# Find what's using the port
sudo lsof -i :8080
# COMMAND    PID USER   FD   TYPE DEVICE
# java     12345 user   42u  IPv6  TCP *:8080

# Kill the process or change port
```

```yaml
services:
  app:
    ports:
      - "8081:8080"  # Use different host port
```

**Issue 3: Changes not reflected**

```bash
# Rebuild image
docker compose build app

# Recreate container
docker compose up -d --force-recreate app

# Or both
docker compose up -d --build --force-recreate app
```

**Issue 4: Volume data persists incorrectly**

```bash
# Remove volumes
docker compose down -v

# Remove specific volume
docker volume rm bibby_postgres-data

# Start fresh
docker compose up -d
```

### 7.6 Performance Monitoring

```bash
# Real-time resource usage
docker compose stats

# Output:
CONTAINER       CPU %   MEM USAGE / LIMIT   MEM %   NET I/O
bibby-app       2.5%    456MiB / 640MiB     71.2%   1.2MB / 850KB
bibby-postgres  1.2%    45MiB / 512MiB      8.8%    850KB / 1.2MB
bibby-grafana   0.5%    89MiB / 256MiB      34.8%   450KB / 320KB
```

---

## 8. Production-Like Local Environments

### 8.1 Simulating Production

**Production constraints in development:**

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 640M
        reservations:
          cpus: '0.5'
          memory: 256M
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

**Note**: `deploy` section requires **Swarm mode** or **Compose v2** with compatibility mode:

```bash
docker compose --compatibility up
```

### 8.2 Load Balancing (Multiple Replicas)

```yaml
services:
  app:
    image: bibby:0.3.0
    deploy:
      replicas: 3  # 3 instances of app

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - app
```

**nginx.conf:**

```nginx
upstream bibby_backend {
    server bibby-app-1:8080;
    server bibby-app-2:8080;
    server bibby-app-3:8080;
}

server {
    listen 80;
    location / {
        proxy_pass http://bibby_backend;
    }
}
```

**Run with Swarm:**

```bash
docker swarm init
docker stack deploy -c docker-compose.yml bibby
```

### 8.3 SSL/TLS Termination

```yaml
services:
  nginx:
    image: nginx:alpine
    ports:
      - "443:443"
      - "80:80"
    volumes:
      - ./nginx-ssl.conf:/etc/nginx/nginx.conf:ro
      - ./certs:/etc/nginx/certs:ro
```

**Generate self-signed cert for development:**

```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout certs/bibby.key \
  -out certs/bibby.crt \
  -subj "/CN=localhost"
```

### 8.4 Secrets Management

**Docker Compose Secrets:**

```yaml
services:
  app:
    secrets:
      - db_password
    environment:
      DATABASE_PASSWORD_FILE: /run/secrets/db_password

secrets:
  db_password:
    file: ./secrets/db_password.txt
```

**Create secret:**

```bash
mkdir secrets
echo "super_secret_password" > secrets/db_password.txt
chmod 600 secrets/db_password.txt

# Add to .gitignore
echo "secrets/" >> .gitignore
```

**Read in application:**

```java
// Read from file instead of environment variable
String passwordFile = System.getenv("DATABASE_PASSWORD_FILE");
String password = Files.readString(Path.of(passwordFile)).trim();
```

---

## 9. Best Practices

### 9.1 Compose File Organization

**Good:**

```yaml
version: '3.9'

# Service definitions grouped logically
services:
  # Application tier
  app:
    ...

  # Data tier
  postgres:
    ...
  redis:
    ...

  # Monitoring tier
  prometheus:
    ...
  grafana:
    ...

# Networks defined separately
networks:
  frontend:
    ...
  backend:
    ...

# Volumes at the end
volumes:
  postgres-data:
    ...
```

### 9.2 Use Health Checks

**Always define health checks:**

```yaml
services:
  postgres:
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s

  app:
    depends_on:
      postgres:
        condition: service_healthy
```

### 9.3 Pin Image Versions

**Bad:**

```yaml
services:
  postgres:
    image: postgres  # Uses 'latest' - unpredictable
```

**Good:**

```yaml
services:
  postgres:
    image: postgres:15-alpine  # Specific version
    # OR
    image: postgres:15.1-alpine  # Even more specific
    # OR
    image: postgres@sha256:abc123...  # Digest (immutable)
```

### 9.4 Use .dockerignore

**Prevent unnecessary files from build context:**

```
.git
.idea
target/
*.log
.env
docker-compose*.yml
README.md
docs/
```

### 9.5 Resource Limits

**Prevent resource exhaustion:**

```yaml
services:
  app:
    mem_limit: 640m
    mem_reservation: 512m
    cpus: 2.0
    pids_limit: 100
```

### 9.6 Logging Configuration

```yaml
services:
  app:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
        labels: "app,environment"
```

### 9.7 Container Naming

```yaml
services:
  app:
    container_name: bibby-app  # Explicit name
    # Not: ${PROJECT}-app (dynamic, less predictable)
```

### 9.8 Use Build Arguments

```yaml
services:
  app:
    build:
      context: .
      args:
        - VERSION=${APP_VERSION}
        - BUILD_DATE=${BUILD_DATE}
        - VCS_REF=${VCS_REF}
```

### 9.9 Security

```yaml
services:
  app:
    read_only: true  # Read-only root filesystem
    security_opt:
      - no-new-privileges:true
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE
    user: "1001:1001"  # Non-root user
```

---

## 10. Interview-Ready Knowledge

### Question 1: "How do you structure a Docker Compose setup for local development that mirrors production?"

**Answer:**

"For our Bibby library management application, I use a layered Compose configuration that balances development convenience with production parity.

**File Structure:**

```
docker-compose.yml           # Base configuration (shared)
docker-compose.override.yml  # Development (auto-loaded)
docker-compose.prod.yml      # Production (explicit)
.env.development             # Dev environment variables
.env.production              # Prod environment variables
```

**Base Configuration** defines services all environments need:

```yaml
services:
  app:
    image: bibby:${APP_VERSION}
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/bibby
    depends_on:
      postgres:
        condition: service_healthy

  postgres:
    image: postgres:15-alpine
    healthcheck:
      test: pg_isready -U bibby_admin
```

**Development Override** adds developer-friendly features:

```yaml
services:
  app:
    build: .  # Build locally instead of using image
    volumes:
      - ./src:/app/src:ro  # Hot reload
    ports:
      - "5005:5005"  # Remote debugging
    environment:
      SPRING_PROFILES_ACTIVE: development
      LOGGING_LEVEL_ROOT: DEBUG

  pgadmin:  # Additional dev tools
    image: dpage/pgadmin4
    ports:
      - "5050:80"
```

**Production Configuration** adds production constraints:

```yaml
services:
  app:
    image: 123456789012.dkr.ecr.us-east-1.amazonaws.com/bibby:${APP_VERSION}
    deploy:
      resources:
        limits:
          memory: 640M
          cpus: '2.0'
      restart_policy:
        condition: on-failure
    environment:
      SPRING_PROFILES_ACTIVE: production
      LOGGING_LEVEL_ROOT: INFO
    read_only: true
    security_opt:
      - no-new-privileges:true
```

**Key Principles:**

1. **Health Checks**: Production uses the same health checks as development. This catches issues early—if health checks fail locally, they'll fail in production.

2. **Networking**: Services communicate via DNS names (not localhost), identical to production service discovery.

3. **Resource Limits**: I can optionally enable production resource limits locally using `--compatibility` mode to test memory constraints.

4. **Data Persistence**: Named volumes in development mirror production storage patterns. When I run `docker compose down`, data persists just like production databases persist across deployments.

5. **Secrets**: Development uses `.env` files, production uses Docker secrets or AWS Secrets Manager. The application code reads both via environment variables, so the interface is identical.

**Running Different Environments:**

```bash
# Development (auto-loads override)
docker compose up

# Production simulation
docker compose --env-file .env.production \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  up --compatibility
```

This approach caught a critical issue: locally, Bibby worked fine with unlimited memory, but in production with a 640MB limit, the JVM hit OOM. By testing with `--compatibility` and resource limits locally, I discovered this before deployment."

### Question 2: "Explain your debugging strategy for multi-container applications in Docker Compose."

**Answer:**

"I use a systematic approach when debugging issues in our Bibby Compose stack, which includes the app, PostgreSQL, pgAdmin, Prometheus, and Grafana.

**Level 1: Service Health**

First, I check overall service health:

```bash
docker compose ps
```

This immediately shows me which services are unhealthy. Recently, I saw:

```
NAME            STATUS
bibby-app       Up 2 minutes (health: starting)
bibby-postgres  Up 3 minutes (healthy)
```

The app was stuck in 'starting' state. This told me the health check was failing.

**Level 2: Logs Analysis**

Next, I check logs for the unhealthy service:

```bash
docker compose logs --tail=50 app
```

The logs revealed:

```
org.postgresql.util.PSQLException: Connection to postgres:5432 refused
```

So the app couldn't reach the database, despite postgres being healthy.

**Level 3: Network Connectivity**

I verify network connectivity:

```bash
# Can the app container resolve 'postgres'?
docker compose exec app nslookup postgres
# Server: 127.0.0.11
# Address: 172.18.0.2

# Can it connect to port 5432?
docker compose exec app nc -zv postgres 5432
# postgres (172.18.0.2:5432) open
```

Network was fine, so the issue was likely the database itself.

**Level 4: Database Investigation**

```bash
# Check if PostgreSQL is actually ready
docker compose exec postgres pg_isready -U bibby_admin
# accepting connections

# Check database exists
docker compose exec postgres psql -U bibby_admin -l
# ... bibby database listed
```

Database was ready. The issue must be in the connection string.

**Level 5: Configuration Verification**

```bash
docker compose exec app env | grep DATABASE
# DATABASE_URL=jdbc:postgresql://postgres:5432/wrong_db_name
#                                              ^^^^^^^^^^^^^^
```

Found it! The database name was wrong (`wrong_db_name` instead of `bibby`).

**Root Cause**: I had a typo in `.env`:

```bash
# .env
DB_NAME=wrong_db_name  # Should be 'bibby'
```

**Fix and Verify:**

```bash
# Fix .env
echo "DB_NAME=bibby" > .env

# Recreate app container with new environment
docker compose up -d --force-recreate app

# Verify health
docker compose ps app
# STATUS: Up 10 seconds (healthy) ✅
```

**Advanced Debugging Techniques:**

For more complex issues, I use:

1. **Exec into container**: `docker compose exec app sh` to explore the filesystem, check file permissions, test connections manually

2. **Run commands in isolation**: `docker compose run --rm app sh` creates a new container for debugging without affecting the running service

3. **Network inspection**: `docker network inspect bibby_default` to see all connected containers and their IPs

4. **Dependency chain testing**: Disable `depends_on`, start services manually one-by-one to isolate startup order issues

5. **Attach to running process**: `docker attach bibby-app` to see stdout/stderr in real-time (useful for Java applications that don't log everything)

**Prevention:**

To prevent similar issues, I:
- Use health checks with `depends_on: condition: service_healthy`
- Validate `.env` files with a script before starting
- Add startup scripts that test connections before starting the main process
- Use structured logging (JSON) so I can parse logs with `jq` for better analysis"

### Question 3: "How do you handle data persistence and backups in Docker Compose?"

**Answer:**

"For Bibby, data persistence is critical—we can't lose library records. I use a multi-layered approach with Docker Compose.

**Persistence Strategy:**

We have two types of data:

1. **Critical data** (PostgreSQL): Must survive container restarts, reboots, even accidental `docker compose down`
2. **Operational data** (logs, metrics): Nice to keep, but can be regenerated

**Named Volumes for Critical Data:**

```yaml
services:
  postgres:
    volumes:
      - postgres-data:/var/lib/postgresql/data

volumes:
  postgres-data:
    driver: local
```

This creates a Docker-managed volume. The data persists even if I run:

```bash
docker compose down       # Containers removed, volume kept
docker compose down -v    # ⚠️  DELETES VOLUMES (dangerous!)
```

I explicitly avoid `-v` in my aliases to prevent accidental data loss.

**Backup Strategy:**

I have automated daily backups:

```bash
#!/bin/bash
# backup-database.sh

BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/bibby_${TIMESTAMP}.sql"

mkdir -p "$BACKUP_DIR"

# Dump database
docker compose exec -T postgres pg_dump -U bibby_admin bibby > "$BACKUP_FILE"

# Compress
gzip "$BACKUP_FILE"

# Keep only last 7 days
find "$BACKUP_DIR" -name "*.sql.gz" -mtime +7 -delete

echo "Backup created: ${BACKUP_FILE}.gz"
```

Run via cron:

```bash
# crontab -e
0 2 * * * /path/to/backup-database.sh
```

**Volume Backup (Full Filesystem):**

For complete point-in-time recovery:

```bash
# Backup volume as tar archive
docker run --rm \
  -v bibby_postgres-data:/source:ro \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/postgres-volume-$(date +%Y%m%d).tar.gz -C /source .
```

This captures the exact PostgreSQL data directory state, including configuration files and transaction logs.

**Restore Process:**

```bash
# Restore from SQL dump
docker compose exec -T postgres psql -U bibby_admin bibby < backups/bibby_20250117.sql

# Restore from volume backup
docker run --rm \
  -v bibby_postgres-data:/target \
  -v $(pwd)/backups:/backup \
  alpine sh -c "cd /target && tar xzf /backup/postgres-volume-20250117.tar.gz"

docker compose restart postgres
```

**Disaster Recovery Test:**

I test restoration monthly:

```bash
# Simulate disaster
docker compose down -v  # Delete everything

# Restore volume
docker volume create bibby_postgres-data
docker run --rm \
  -v bibby_postgres-data:/target \
  -v $(pwd)/backups:/backup \
  alpine tar xzf /backup/postgres-volume-latest.tar.gz -C /target

# Start stack
docker compose up -d

# Verify data
docker compose exec postgres psql -U bibby_admin -d bibby -c "SELECT COUNT(*) FROM books;"
# Should match pre-disaster count
```

**Production Considerations:**

In production, I wouldn't rely solely on local volumes. Instead:

1. **AWS RDS** (managed database) with automated backups
2. **S3 backup replication**: Daily dumps uploaded to S3 with versioning
3. **Cross-region replication**: S3 replicates to secondary region
4. **Point-in-time recovery**: RDS provides 5-minute RPO

But for local development, Docker volumes with daily SQL dumps provide good enough protection while keeping setup simple."

---

## Summary

**What You Learned:**

1. **Docker Compose Fundamentals**
   - YAML structure: services, networks, volumes
   - Simple Bibby + PostgreSQL stack
   - Automatic networking and DNS resolution

2. **Complete Development Stack**
   - Full Bibby stack: App + PostgreSQL + pgAdmin + Prometheus + Grafana
   - 5-service orchestration with health checks
   - Supporting configuration files (init scripts, Prometheus config, Grafana datasources)

3. **Development Workflows**
   - Starting/stopping stack: `docker compose up/down`
   - Hot reload with Spring Boot DevTools
   - Remote debugging on port 5005
   - Database operations: backups, restores, resets

4. **Networking**
   - Default network behavior and DNS resolution
   - Custom networks: frontend/backend separation
   - Network aliases for multiple DNS names
   - Port mapping vs expose

5. **Volumes and Persistence**
   - Named volumes (Docker-managed)
   - Bind mounts (live code sync)
   - Volume management: backup, restore, cleanup
   - Read-only mounts for security

6. **Multi-Environment Configuration**
   - Override files: `docker-compose.override.yml`, `docker-compose.prod.yml`
   - Environment files: `.env.development`, `.env.production`
   - Profiles for conditional services
   - Complete dev/prod separation

7. **Debugging**
   - Log aggregation: `docker compose logs -f`
   - Service status: `docker compose ps`
   - Exec commands: `docker compose exec`
   - Network debugging: ping, nslookup, curl
   - Common issues and solutions

8. **Production-Like Environments**
   - Resource limits and constraints
   - Load balancing with multiple replicas
   - SSL/TLS termination with nginx
   - Secrets management

9. **Best Practices**
   - Health checks with `depends_on` conditions
   - Pin image versions (avoid `latest`)
   - Use `.dockerignore`
   - Resource limits
   - Security hardening (read-only, no-new-privileges)

**Complete Bibby Development Stack:**

```bash
# Start entire stack
docker compose up -d

# Services running:
- Bibby App:       http://localhost:8080
- PostgreSQL:      localhost:5432
- pgAdmin:         http://localhost:5050
- Prometheus:      http://localhost:9090
- Grafana:         http://localhost:3000

# Development workflow:
1. Edit code in IDE
2. Auto-reload with DevTools (3-5 seconds)
3. Debug remotely on port 5005
4. Query database via pgAdmin
5. Monitor metrics in Grafana
```

**Key Commands:**

```bash
docker compose up -d              # Start stack (detached)
docker compose logs -f app        # Follow app logs
docker compose exec app sh        # Shell into app
docker compose exec postgres psql # PostgreSQL shell
docker compose down               # Stop and remove containers
docker compose down -v            # Stop and remove volumes
docker compose build              # Rebuild images
docker compose ps                 # Service status
```

**Interview-Ready Answers:**
- Multi-environment Compose setup (dev/prod parity)
- Systematic debugging strategy (5-level approach)
- Data persistence and backup strategy

**Progress**: 20 of 28 sections complete (71%)

**Next**: Section 21 will cover Dependency Isolation and Management (managing external dependencies, service versioning, dependency health checks, graceful degradation, and building resilient microservices).

**Key Takeaway:**

Docker Compose transforms development from "works on my machine" to "works identically on everyone's machine." A well-structured Compose setup provides production parity, automated orchestration, and a shared team environment—all defined in version-controlled YAML files.
