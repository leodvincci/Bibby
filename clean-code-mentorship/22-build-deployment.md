# Section 22: Build & Deployment
## Clean Code + Spring Framework Mentorship

**Focus:** Maven builds, packaging, Docker containerization, and deployment strategies

**Estimated Time:** 2-3 hours to read and understand; 6-8 hours to implement

---

## Overview

You've built a working application locally. Now it's time to prepare it for **production deployment**. This section covers building, packaging, containerizing, and deploying your Spring Boot application.

---

## Your Current Build Configuration

**pom.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.7</version>
        <relativePath/>
    </parent>
    <groupId>com.penrose</groupId>
    <artifactId>Bibby</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>Bibby</name>
    <description>Bibby</description>

    <properties>
        <java.version>17</java.version>
        <spring-shell.version>3.4.1</spring-shell.version>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**What's Good:**
- ✅ Spring Boot parent POM (automatic dependency management)
- ✅ Java 17
- ✅ Spring Boot Maven Plugin (creates executable JAR)

**What's Missing:**
- ❌ No build information
- ❌ No resource filtering
- ❌ No production profiles
- ❌ No Docker configuration
- ❌ No deployment scripts

---

## Maven Build Improvements

### Enhanced pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.7</version>
        <relativePath/>
    </parent>

    <groupId>com.penrose</groupId>
    <artifactId>bibby</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Bibby</name>
    <description>Personal Library Management System</description>
    <url>https://github.com/leodvincci/Bibby</url>

    <developers>
        <developer>
            <name>Leo D. Penrose</name>
            <email>your.email@example.com</email>
        </developer>
    </developers>

    <properties>
        <java.version>17</java.version>
        <spring-shell.version>3.4.1</spring-shell.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

        <!-- Build timestamp for versioning -->
        <maven.build.timestamp.format>yyyy-MM-dd HH:mm</maven.build.timestamp.format>
        <build.timestamp>${maven.build.timestamp}</build.timestamp>
    </properties>

    <!-- Dependencies section here -->

    <build>
        <finalName>${project.artifactId}-${project.version}</finalName>

        <!-- Resource filtering for property substitution -->
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
                <includes>
                    <include>**/*.properties</include>
                    <include>**/*.yml</include>
                    <include>**/*.yaml</include>
                </includes>
            </resource>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>false</filtering>
                <excludes>
                    <exclude>**/*.properties</exclude>
                    <exclude>**/*.yml</exclude>
                    <exclude>**/*.yaml</exclude>
                </excludes>
            </resource>
        </resources>

        <plugins>
            <!-- Spring Boot Maven Plugin -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>build-info</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <executable>true</executable>
                    <layers>
                        <enabled>true</enabled>
                    </layers>
                </configuration>
            </plugin>

            <!-- Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                    <parameters>true</parameters>
                </configuration>
            </plugin>

            <!-- Surefire Plugin (for running tests) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <argLine>-Xmx1024m</argLine>
                </configuration>
            </plugin>

            <!-- JaCoCo Code Coverage -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.11</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- Git Commit ID Plugin -->
            <plugin>
                <groupId>io.github.git-commit-id</groupId>
                <artifactId>git-commit-id-maven-plugin</artifactId>
                <configuration>
                    <dotGitDirectory>${project.basedir}/.git</dotGitDirectory>
                    <generateGitPropertiesFile>true</generateGitPropertiesFile>
                    <failOnNoGitDirectory>false</failOnNoGitDirectory>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <!-- Build profiles -->
    <profiles>
        <!-- Development Profile -->
        <profile>
            <id>dev</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <spring.profiles.active>dev</spring.profiles.active>
            </properties>
        </profile>

        <!-- Production Profile -->
        <profile>
            <id>prod</id>
            <properties>
                <spring.profiles.active>prod</spring.profiles.active>
            </properties>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-surefire-plugin</artifactId>
                        <configuration>
                            <skip>false</skip>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>
```

### Key Improvements:

1. **Build Info:** `build-info` goal creates `META-INF/build-info.properties`
2. **Layered JARs:** Optimized for Docker
3. **Resource Filtering:** Substitute Maven properties in config files
4. **Git Info:** Track which commit is deployed
5. **Profiles:** Separate dev/prod builds

---

## Building the Application

### Development Build

```bash
# Clean and build
mvn clean package

# Skip tests (for quick iteration)
mvn clean package -DskipTests

# Specific profile
mvn clean package -Pdev

# Run without packaging
mvn spring-boot:run
```

### Production Build

```bash
# Full build with tests
mvn clean package -Pprod

# Build info
java -jar target/bibby-1.0.0.jar --version
```

### Build Output

```
target/
├── bibby-1.0.0.jar                    # Executable JAR
├── bibby-1.0.0.jar.original           # JAR without dependencies
├── classes/                            # Compiled classes
├── generated-sources/                  # Generated code
├── maven-archiver/                     # Maven metadata
├── maven-status/                       # Build status
└── site/
    └── jacoco/
        └── index.html                  # Coverage report
```

---

## Packaging Strategies

### Fat JAR (Default)

Spring Boot creates a "fat JAR" with all dependencies included.

**Pros:**
- ✅ Single file to deploy
- ✅ Easy to run: `java -jar bibby.jar`
- ✅ No dependency management on server

**Cons:**
- ❌ Large file size (~50-100MB)
- ❌ Longer build times
- ❌ Slower Docker builds (layers change frequently)

### Layered JARs (Recommended for Docker)

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <layers>
            <enabled>true</enabled>
        </layers>
    </configuration>
</plugin>
```

**View layers:**
```bash
java -Djarmode=layertools -jar target/bibby-1.0.0.jar list
```

**Output:**
```
dependencies
spring-boot-loader
snapshot-dependencies
application
```

**Why this matters:**
- Dependencies rarely change → cached Docker layer
- Your code changes frequently → small layer, fast rebuilds

---

## Docker Containerization

### Dockerfile (Multi-Stage Build)

Create `Dockerfile` in project root:

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy dependency definitions
COPY pom.xml .
COPY .mvn .mvn

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -Pprod

# Stage 2: Runtime (using layered approach)
FROM eclipse-temurin:17-jre-alpine

# Add metadata
LABEL maintainer="Leo D. Penrose <your.email@example.com>"
LABEL description="Bibby - Personal Library Management System"

# Create non-root user
RUN addgroup -S bibby && adduser -S bibby -G bibby

# Set working directory
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/bibby-*.jar app.jar

# Change ownership
RUN chown -R bibby:bibby /app

# Switch to non-root user
USER bibby

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=prod"]
```

### Optimized Dockerfile (with layered JARs)

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -Pprod

# Extract layers
FROM eclipse-temurin:17-jre-alpine AS builder
WORKDIR /app
COPY --from=build /app/target/bibby-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Runtime stage (layered)
FROM eclipse-temurin:17-jre-alpine

# Security
RUN addgroup -S bibby && adduser -S bibby -G bibby

WORKDIR /app

# Copy layers (each layer is cached independently)
COPY --from=builder --chown=bibby:bibby app/dependencies/ ./
COPY --from=builder --chown=bibby:bibby app/spring-boot-loader/ ./
COPY --from=builder --chown=bibby:bibby app/snapshot-dependencies/ ./
COPY --from=builder --chown=bibby:bibby app/application/ ./

USER bibby

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

### .dockerignore

Create `.dockerignore`:

```
target/
.git/
.idea/
*.iml
*.log
.DS_Store
node_modules/
.env
*.md
Dockerfile
docker-compose.yml
```

### Build Docker Image

```bash
# Build image
docker build -t bibby:latest .

# Build with version tag
docker build -t bibby:1.0.0 .

# Build with both tags
docker build -t bibby:latest -t bibby:1.0.0 .
```

### Run Docker Container

```bash
# Run with default settings
docker run -p 8080:8080 bibby:latest

# Run with environment variables
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/bibby \
  -e DATABASE_USER=bibby_user \
  -e DATABASE_PASSWORD=secret \
  bibby:latest

# Run in detached mode
docker run -d --name bibby-app -p 8080:8080 bibby:latest

# View logs
docker logs -f bibby-app

# Stop container
docker stop bibby-app

# Remove container
docker rm bibby-app
```

---

## Docker Compose

For local development with database:

**docker-compose.yml:**

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:16-alpine
    container_name: bibby-postgres
    environment:
      POSTGRES_DB: bibby
      POSTGRES_USER: bibby_user
      POSTGRES_PASSWORD: bibby_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U bibby_user -d bibby"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Bibby Application
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: bibby-app
    depends_on:
      postgres:
        condition: service_healthy
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: jdbc:postgresql://postgres:5432/bibby
      DATABASE_USER: bibby_user
      DATABASE_PASSWORD: bibby_pass
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

volumes:
  postgres-data:
```

**Usage:**

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down

# Stop and remove volumes (deletes data!)
docker-compose down -v

# Rebuild app
docker-compose up -d --build app
```

---

## Environment Configuration

### application.properties (with placeholders)

```properties
# Application info
spring.application.name=Bibby
info.app.name=@project.name@
info.app.version=@project.version@
info.app.description=@project.description@

# Database (using environment variables)
spring.datasource.url=${DATABASE_URL:jdbc:h2:mem:bibby}
spring.datasource.username=${DATABASE_USER:sa}
spring.datasource.password=${DATABASE_PASSWORD:}

# JPA
spring.jpa.hibernate.ddl-auto=${DDL_AUTO:validate}
spring.jpa.show-sql=${SHOW_SQL:false}

# Server
server.port=${SERVER_PORT:8080}

# Logging
logging.level.root=${LOG_LEVEL_ROOT:INFO}
logging.level.com.penrose.bibby=${LOG_LEVEL_APP:DEBUG}
```

### .env file (for local development)

Create `.env` (add to `.gitignore`):

```env
DATABASE_URL=jdbc:postgresql://localhost:5432/bibby
DATABASE_USER=bibby_user
DATABASE_PASSWORD=local_dev_password
DDL_AUTO=update
SHOW_SQL=true
SERVER_PORT=8080
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
```

---

## Deployment Strategies

### 1. Simple JAR Deployment

**Deploy to server:**

```bash
# Build locally
mvn clean package -Pprod

# Copy to server
scp target/bibby-1.0.0.jar user@server:/opt/bibby/

# SSH to server
ssh user@server

# Run as systemd service
sudo systemctl start bibby
```

**Create systemd service:**

`/etc/systemd/system/bibby.service`:

```ini
[Unit]
Description=Bibby Library Management System
After=syslog.target network.target

[Service]
User=bibby
Group=bibby
SuccessExitStatus=143

EnvironmentFile=/opt/bibby/.env
ExecStart=/usr/bin/java -Xmx512m -Xms256m -jar /opt/bibby/bibby-1.0.0.jar
ExecStop=/bin/kill -15 $MAINPID

Restart=on-failure
RestartSec=10

StandardOutput=journal
StandardError=journal
SyslogIdentifier=bibby

[Install]
WantedBy=multi-user.target
```

**Manage service:**

```bash
# Enable on boot
sudo systemctl enable bibby

# Start
sudo systemctl start bibby

# Check status
sudo systemctl status bibby

# View logs
sudo journalctl -u bibby -f

# Restart
sudo systemctl restart bibby
```

### 2. Docker Deployment

```bash
# On server
docker pull bibby:1.0.0

# Run with restart policy
docker run -d \
  --name bibby-app \
  --restart unless-stopped \
  -p 8080:8080 \
  --env-file /opt/bibby/.env \
  bibby:1.0.0

# Check logs
docker logs -f bibby-app
```

### 3. Cloud Deployment

**Heroku:**

```bash
# Login
heroku login

# Create app
heroku create bibby-app

# Add PostgreSQL
heroku addons:create heroku-postgresql:mini

# Deploy
git push heroku main

# View logs
heroku logs --tail
```

**AWS Elastic Beanstalk:**

```bash
# Install EB CLI
pip install awsebcli

# Initialize
eb init -p docker bibby-app

# Create environment
eb create bibby-prod

# Deploy
eb deploy

# View logs
eb logs
```

**Google Cloud Run:**

```bash
# Build and push to Google Container Registry
gcloud builds submit --tag gcr.io/PROJECT_ID/bibby

# Deploy
gcloud run deploy bibby \
  --image gcr.io/PROJECT_ID/bibby \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

---

## CI/CD Pipeline

### GitHub Actions

Create `.github/workflows/build.yml`:

```yaml
name: Build and Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven

    - name: Build with Maven
      run: mvn -B clean package -Pprod

    - name: Run tests
      run: mvn test

    - name: Generate coverage report
      run: mvn jacoco:report

    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ./target/site/jacoco/jacoco.xml

    - name: Build Docker image
      run: docker build -t bibby:${{ github.sha }} .

    - name: Upload artifact
      uses: actions/upload-artifact@v3
      with:
        name: bibby-jar
        path: target/bibby-*.jar
```

### GitLab CI

Create `.gitlab-ci.yml`:

```yaml
stages:
  - build
  - test
  - package
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

cache:
  paths:
    - .m2/repository

build:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn clean compile -Pprod
  artifacts:
    paths:
      - target/

test:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn test
  coverage: '/Total.*?([0-9]{1,3})%/'
  artifacts:
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml
      coverage_report:
        coverage_format: cobertura
        path: target/site/jacoco/jacoco.xml

package:
  stage: package
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn package -DskipTests -Pprod
  artifacts:
    paths:
      - target/*.jar
    expire_in: 1 week

docker:
  stage: package
  image: docker:latest
  services:
    - docker:dind
  script:
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA

deploy:
  stage: deploy
  image: alpine:latest
  script:
    - apk add --no-cache openssh-client
    - scp target/bibby-*.jar user@server:/opt/bibby/
    - ssh user@server "sudo systemctl restart bibby"
  only:
    - main
```

---

## Production Checklist

### Before Deployment

- [ ] All tests passing
- [ ] Code coverage > 80%
- [ ] Logging properly configured
- [ ] Secrets externalized
- [ ] Database migrations ready
- [ ] Health checks working
- [ ] Docker image built and tested
- [ ] Environment variables documented

### Deployment

- [ ] Database backup created
- [ ] Blue-green deployment or canary release
- [ ] Monitoring dashboards ready
- [ ] Rollback plan prepared
- [ ] Load balancer configured
- [ ] SSL certificates installed
- [ ] DNS configured

### Post-Deployment

- [ ] Health check returns 200
- [ ] Logs showing no errors
- [ ] Metrics baseline established
- [ ] Smoke tests passing
- [ ] Performance acceptable
- [ ] Database connections stable

---

## Action Items

### 🚨 Critical (Do First - 2-3 hours)

1. **Enhance pom.xml**
   - [ ] Add build-info plugin
   - [ ] Add git-commit-id plugin
   - [ ] Enable layered JARs
   - [ ] Add dev/prod profiles

2. **Create Dockerfile**
   - [ ] Create multi-stage Dockerfile
   - [ ] Create .dockerignore
   - [ ] Build and test locally
   - [ ] Verify image size

3. **Create docker-compose.yml**
   - [ ] Add PostgreSQL service
   - [ ] Add application service
   - [ ] Test full stack locally

### 🔶 High Priority (This Week - 3-4 hours)

4. **Environment Configuration**
   - [ ] Externalize all secrets
   - [ ] Create .env template
   - [ ] Document required variables
   - [ ] Test with different profiles

5. **Add CI/CD Pipeline**
   - [ ] Create GitHub Actions workflow
   - [ ] Run tests on PR
   - [ ] Build Docker image
   - [ ] Deploy to staging (optional)

6. **Create Deployment Scripts**
   - [ ] Create systemd service file
   - [ ] Write deployment documentation
   - [ ] Create rollback procedure

### 🔷 Medium Priority (This Month - 2-3 hours)

7. **Production Deployment**
   - [ ] Choose hosting platform
   - [ ] Setup production database
   - [ ] Configure monitoring
   - [ ] Deploy first version

8. **Monitoring Setup**
   - [ ] Configure health checks
   - [ ] Setup uptime monitoring
   - [ ] Configure alerts
   - [ ] Create dashboards

---

## Summary

### Your Current State
- ✅ Basic Maven build works
- ❌ No Docker configuration
- ❌ No CI/CD pipeline
- ❌ No deployment automation
- ❌ Hardcoded configuration

### After This Section
- ✅ Optimized Maven build
- ✅ Docker containerization
- ✅ Environment-based configuration
- ✅ CI/CD pipeline
- ✅ Production-ready deployment

---

## Resources

### Documentation
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [Maven in 5 Minutes](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)

### Tools
- **Maven:** Build tool
- **Docker:** Containerization
- **Docker Compose:** Local orchestration
- **GitHub Actions/GitLab CI:** CI/CD
- **Heroku/AWS/GCP:** Hosting platforms

---

## Mentor's Note

Leo, build and deployment is often an afterthought for developers, but it's **critical** for actually getting your application into users' hands.

**Key principles:**
1. **Repeatable builds:** Same source = same artifact
2. **Environment parity:** Dev looks like prod
3. **Automate everything:** No manual steps
4. **Fast feedback:** Know immediately if something breaks

**Your priorities:**
1. Dockerize your app (3 hours)
2. Setup docker-compose (1 hour)
3. Add CI/CD (2 hours)
4. Deploy somewhere (2 hours)

Once this is done, you can confidently say "my app is production-ready" in interviews.

---

**Next Section:** Spring Ecosystem & Advanced Topics

**Last Updated:** 2025-11-17
**Status:** Complete ✅
