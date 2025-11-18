# Section 15: Cloud & Container Fundamentals
**Production Deployment at Scale**

**Week 15: From Local to Cloud**

---

## Overview

Section 14 covered Docker and CI/CD for local and simple cloud deployments. But in enterprise environments, you'll encounter:
- **Cloud platforms** (AWS, Azure, Google Cloud) with hundreds of services
- **Managed databases** that auto-scale and handle backups
- **Container orchestration** (Kubernetes) for running dozens or hundreds of containers
- **Cloud-native patterns** (serverless, microservices, event-driven)

This section provides the **foundation** you need to discuss cloud deployments in interviews and understand how enterprise systems operate at scale.

**Important:** You don't need to become a cloud expert. You need **conversational knowledge** and **hands-on experience with basics**. Interviewers want to see you understand cloud concepts, not that you've architected multi-region disaster recovery systems.

**This Week's Focus:**
- Cloud platform comparison (AWS vs Azure vs GCP)
- Managed database services
- Kubernetes fundamentals
- Cloud-native deployment patterns
- Cost optimization basics

---

## Part 1: Cloud Platform Overview

### The Three Major Providers

**AWS (Amazon Web Services):**
- Market leader (~32% market share)
- Most mature, most services
- Steeper learning curve
- **Target companies using it:** Most enterprise companies, startups

**Azure (Microsoft):**
- Strong in enterprise (integrates with Microsoft ecosystem)
- Good for companies already using Office 365, Active Directory
- ~23% market share
- **Target companies using it:** Large enterprises, government

**Google Cloud Platform (GCP):**
- Strong in data/ML services
- Cleaner UI, better developer experience
- ~10% market share
- **Target companies using it:** Data-heavy companies, ML/AI startups

### Industrial Analogy: Cloud as Utility Infrastructure

In pipeline operations, you don't build your own power plants—you buy electricity from the grid:
- **Pay for what you use** (metered billing)
- **Scalable** (request more capacity as needed)
- **Maintained by provider** (no need to manage generators)

Cloud is the same for computing:
- **Compute** (virtual machines, containers)
- **Storage** (databases, file storage, object storage)
- **Networking** (load balancers, CDN)
- **Managed by provider** (hardware, OS patches, backups)

### Core Services Comparison

| Service Type | AWS | Azure | GCP | Purpose |
|--------------|-----|-------|-----|---------|
| **Compute** | EC2 | Virtual Machines | Compute Engine | Run applications |
| **Containers** | ECS/EKS | AKS | GKE | Orchestrate containers |
| **Serverless** | Lambda | Functions | Cloud Functions | Event-driven code |
| **Database (SQL)** | RDS | Azure SQL | Cloud SQL | Managed PostgreSQL/MySQL |
| **Database (NoSQL)** | DynamoDB | Cosmos DB | Firestore | Managed NoSQL |
| **Object Storage** | S3 | Blob Storage | Cloud Storage | File storage |
| **Load Balancer** | ALB/ELB | Load Balancer | Cloud Load Balancing | Distribute traffic |
| **CDN** | CloudFront | CDN | Cloud CDN | Content delivery |
| **Monitoring** | CloudWatch | Monitor | Cloud Monitoring | Metrics & logs |

### For Bibby: Recommended Simple Stack

**Backend hosting:** Render or Railway (simpler than raw AWS)
- Why: Managed platform, automatic HTTPS, easy scaling
- Cost: Free tier available, $7-15/month for production

**Frontend hosting:** Vercel or Netlify
- Why: Built for React/static sites, global CDN
- Cost: Free for personal projects

**Database:** Render PostgreSQL or Supabase
- Why: Managed backups, automatic updates
- Cost: Free tier (1GB), $7/month for 10GB

**Redis:** Render Redis or Upstash
- Why: Managed, simple pricing
- Cost: Free tier available

**Total monthly cost:** $0-30 for low traffic

---

## Part 2: Managed Database Services

### Why Managed Databases?

**DIY approach (running PostgreSQL yourself):**
- ❌ Manual backups
- ❌ OS patching and updates
- ❌ Scaling requires downtime
- ❌ No automatic failover
- ❌ You're on call if database crashes at 3 AM

**Managed approach (RDS, Cloud SQL, etc.):**
- ✅ Automatic daily backups
- ✅ Automated patching
- ✅ Point-in-time recovery
- ✅ Vertical scaling with minimal downtime
- ✅ Read replicas for scaling reads
- ✅ Multi-AZ for high availability

### Industrial Analogy: Equipment Maintenance Contracts

In operations, critical equipment often has maintenance contracts:
- Vendor performs scheduled maintenance
- 24/7 support for failures
- Replacement parts included
- Predictive monitoring

**Managed databases are the same:** Provider handles maintenance, monitoring, and recovery.

### Setting Up Managed PostgreSQL (Render Example)

**Step 1: Create database via Render dashboard**
```yaml
Name: bibby-postgres
Plan: Starter ($7/month, 1GB storage)
Region: Oregon (close to backend)
PostgreSQL Version: 15
```

**Step 2: Get connection details**
```
Internal URL: postgresql://bibby_user:***@dpg-xxxxx-a/bibby_db
External URL: postgresql://bibby_user:***@oregon-postgres.render.com/bibby_db
```

**Step 3: Update application configuration**
```properties
# application-prod.properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}

# Flyway configuration
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=false

# Connection pooling
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```

**Step 4: Configure backups**
- Render: Automatic daily backups (7-day retention)
- Point-in-time recovery: Restore to any point in last 7 days

### Database Connection Pooling

**Problem:** Creating database connection is expensive (~50-100ms)

**Solution:** Connection pool maintains reusable connections

**HikariCP configuration (included in Spring Boot):**
```properties
# Connection pool settings
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000

# Validation query
spring.datasource.hikari.connection-test-query=SELECT 1
```

**Pool sizing formula:**
```
connections = ((core_count * 2) + effective_spindle_count)
```

For 2 CPU backend: `(2 * 2) + 1 = 5-10 connections` is reasonable

### Read Replicas for Scaling

**Problem:** Analytics queries slow down user-facing app

**Solution:** Route read-only queries to replica database

```
┌─────────────┐
│   Primary   │ ← Writes (checkouts, updates)
│  Database   │
└─────────────┘
       │
       │ Replication
       ▼
┌─────────────┐
│   Replica   │ ← Reads (analytics, reports)
│  Database   │
└─────────────┘
```

**Spring Boot multi-datasource configuration:**
```java
@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.replica")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create().build();
    }

    // Use @Transactional(readOnly=true) to route to replica
}
```

---

## Part 3: Container Orchestration with Kubernetes

### What is Kubernetes (K8s)?

**Problem:** You have 50 Docker containers running your microservices. How do you:
- Restart containers if they crash?
- Scale from 5 to 50 instances based on load?
- Update containers with zero downtime?
- Route traffic to healthy containers?

**Solution:** Kubernetes orchestrates containers at scale

### Industrial Analogy: Process Control System

In a refinery or chemical plant, a **Distributed Control System (DCS)** manages hundreds of control loops:
- Monitors setpoints and actual values
- Adjusts valves to maintain targets
- Restarts pumps if they fail
- Routes flow to backup equipment

**Kubernetes is DCS for containers:**
- Monitors desired state vs actual state
- Adjusts replicas to maintain targets
- Restarts containers if they crash
- Routes traffic to healthy instances

### Kubernetes Core Concepts

**Pod:** Smallest deployable unit (1+ containers)
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: bibby-backend
spec:
  containers:
  - name: backend
    image: bibby-backend:latest
    ports:
    - containerPort: 8080
```

**Deployment:** Manages replicas of pods
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bibby-backend
spec:
  replicas: 3  # Run 3 copies
  selector:
    matchLabels:
      app: bibby
  template:
    metadata:
      labels:
        app: bibby
    spec:
      containers:
      - name: backend
        image: bibby-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

**Service:** Load balancer for pods
```yaml
apiVersion: v1
kind: Service
metadata:
  name: bibby-backend-service
spec:
  type: LoadBalancer
  selector:
    app: bibby
  ports:
  - port: 80
    targetPort: 8080
```

**ConfigMap:** Configuration data
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: bibby-config
data:
  application.properties: |
    server.port=8080
    spring.jpa.hibernate.ddl-auto=validate
```

**Secret:** Sensitive data (encrypted)
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-credentials
type: Opaque
data:
  url: cG9zdGdyZXNxbDovL2xvY2FsaG9zdDo1NDMyL2JpYmJ5  # base64 encoded
  username: cG9zdGdyZXM=
  password: cGFzc3dvcmQ=
```

### Deploying to Kubernetes

**Option 1: Managed Kubernetes (Recommended for learning)**
- Google Kubernetes Engine (GKE) - Easiest, free tier
- Amazon EKS - Most features
- Azure AKS - Good for Microsoft shops
- DigitalOcean Kubernetes - Simplest, cheapest

**Option 2: Local Kubernetes (Development)**
- Minikube - Single-node cluster on laptop
- Docker Desktop - Built-in Kubernetes
- Kind - Kubernetes in Docker

### Simple Deployment Example

```bash
# Build Docker image
docker build -t your-dockerhub-username/bibby-backend:v1 .

# Push to registry
docker push your-dockerhub-username/bibby-backend:v1

# Create deployment
kubectl create deployment bibby-backend \
  --image=your-dockerhub-username/bibby-backend:v1 \
  --replicas=3

# Expose as service
kubectl expose deployment bibby-backend \
  --type=LoadBalancer \
  --port=80 \
  --target-port=8080

# Check status
kubectl get pods
kubectl get services

# View logs
kubectl logs -l app=bibby-backend

# Scale up
kubectl scale deployment bibby-backend --replicas=5

# Rolling update
docker build -t your-dockerhub-username/bibby-backend:v2 .
docker push your-dockerhub-username/bibby-backend:v2
kubectl set image deployment/bibby-backend \
  backend=your-dockerhub-username/bibby-backend:v2

# Rollback if issues
kubectl rollout undo deployment/bibby-backend
```

### When Do You Need Kubernetes?

**You probably DON'T need K8s if:**
- Single application (not microservices)
- <10,000 requests/day
- Team <5 engineers

**Use simpler platforms:** Render, Railway, Fly.io, Heroku

**You MIGHT need K8s if:**
- Microservices architecture (10+ services)
- Need auto-scaling based on metrics
- Multi-cloud or hybrid cloud strategy
- Team with K8s expertise

---

## Part 4: Cloud-Native Application Patterns

### 12-Factor App Principles

**1. Codebase:** One codebase in version control
- ✅ Bibby in Git, deployed to multiple environments

**2. Dependencies:** Explicitly declare dependencies
- ✅ `pom.xml` declares all dependencies

**3. Config:** Store config in environment variables
- ✅ `DATABASE_URL`, `REDIS_HOST` from environment

**4. Backing Services:** Treat as attached resources
- ✅ PostgreSQL, Redis configurable via URLs

**5. Build, Release, Run:** Strict separation
- ✅ Maven builds JAR, Docker packages, K8s runs

**6. Processes:** Execute app as stateless processes
- ✅ Spring Boot app stores no local state
- ⚠️ Sessions in Redis, not memory

**7. Port Binding:** Export services via port
- ✅ Spring Boot on port 8080

**8. Concurrency:** Scale out via process model
- ✅ Horizontal scaling (more instances)

**9. Disposability:** Fast startup, graceful shutdown
- ✅ Spring Boot starts in <30s
- ✅ Shutdown hooks for cleanup

**10. Dev/Prod Parity:** Keep environments similar
- ✅ Docker ensures consistency

**11. Logs:** Treat as event streams
- ✅ Structured JSON logs to stdout

**12. Admin Processes:** Run as one-off processes
- ✅ Flyway migrations as startup task

### Health Checks and Graceful Shutdown

**Liveness probe:** Is app alive? (Restart if fails)
```java
// Spring Boot Actuator automatically provides /actuator/health

// Custom health indicator
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(3)) {
                return Health.up().build();
            }
        } catch (SQLException e) {
            return Health.down(e).build();
        }
        return Health.down().build();
    }
}
```

**Readiness probe:** Is app ready for traffic?
```java
@Component
public class StartupHealthIndicator implements HealthIndicator {
    private volatile boolean ready = false;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        ready = true;
    }

    @Override
    public Health health() {
        return ready ? Health.up().build() : Health.down().build();
    }
}
```

**Graceful shutdown:**
```properties
# Allow 30 seconds for in-flight requests to complete
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

### Environment-Specific Configuration

**File:** `application.properties` (default)
```properties
spring.application.name=bibby
server.port=8080
```

**File:** `application-dev.properties` (development)
```properties
spring.jpa.show-sql=true
logging.level.com.penrose.bibby=DEBUG
recommendation.enabled=false  # Don't send emails in dev
```

**File:** `application-prod.properties` (production)
```properties
spring.jpa.show-sql=false
logging.level.com.penrose.bibby=INFO
recommendation.enabled=true

# Security headers
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
```

**Activate profile:**
```bash
# Development
java -jar app.jar --spring.profiles.active=dev

# Production
java -jar app.jar --spring.profiles.active=prod
```

---

## Part 5: Cost Optimization

### Understanding Cloud Costs

**Typical monthly costs for Bibby (low traffic):**

| Service | Provider | Cost |
|---------|----------|------|
| Backend hosting | Render Starter | $7 |
| PostgreSQL | Render Starter | $7 |
| Redis | Upstash Free | $0 |
| Frontend | Vercel Hobby | $0 |
| Domain | Namecheap | $1 |
| **Total** | | **$15/month** |

**Scaling to 10,000 users:**

| Service | Provider | Cost |
|---------|----------|------|
| Backend (2 instances) | Render Pro | $35 |
| PostgreSQL (10GB) | Render Pro | $15 |
| Redis (1GB) | Upstash | $10 |
| Frontend + CDN | Vercel Pro | $20 |
| **Total** | | **$80/month** |

### Cost Optimization Strategies

**1. Right-size resources**
```bash
# Don't over-provision
# Start small, scale up based on metrics

# Bad: "Let's get 8 CPU / 16GB RAM just in case"
# Good: "2 CPU / 4GB RAM, monitor, scale if needed"
```

**2. Use auto-scaling**
```yaml
# Scale based on actual load
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: bibby-backend-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: bibby-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

**3. Use spot/preemptible instances**
- AWS Spot: 70-90% discount
- GCP Preemptible: 80% discount
- Trade-off: Can be terminated with 30s notice
- Good for: Background jobs, batch processing

**4. Cache aggressively**
- Redis caching (Section 9) reduces database load
- CDN for static assets (images, JS, CSS)
- Browser caching with proper headers

**5. Monitor and alert**
```properties
# Set up cost alerts
# AWS: Budgets → Create alert for >$50/month
# GCP: Billing → Budget alerts
# Azure: Cost Management → Budgets
```

### Industrial Analogy: Energy Efficiency

In operations, you optimize for efficiency:
- **Variable frequency drives** adjust pump speed to demand (not full speed always)
- **Heat recovery** captures waste heat for reuse
- **Demand response** shifts usage to off-peak hours

Cloud cost optimization is the same:
- **Auto-scaling** adjusts capacity to demand
- **Caching** reuses computed results
- **Spot instances** use spare capacity at discount

---

## Part 6: Bibby Cloud Deployment Architecture

### Recommended Production Architecture

```
                         ┌─────────────┐
                         │  Cloudflare │
                         │     CDN     │
                         └─────────────┘
                                │
                    ┌───────────┴───────────┐
                    │                       │
                    ▼                       ▼
            ┌──────────────┐        ┌──────────────┐
            │   Vercel     │        │    Render    │
            │   Frontend   │        │   Backend    │
            │   (Static)   │        │  (2 inst.)   │
            └──────────────┘        └──────────────┘
                                            │
                    ┌───────────────────────┼───────────────────────┐
                    │                       │                       │
                    ▼                       ▼                       ▼
            ┌──────────────┐        ┌──────────────┐      ┌──────────────┐
            │   Render     │        │   Render     │      │   Upstash    │
            │  PostgreSQL  │        │   Redis      │      │     Redis    │
            │  (Primary)   │        │   (Cache)    │      │   (Cache)    │
            └──────────────┘        └──────────────┘      └──────────────┘
```

**Why this architecture:**
- **Vercel:** Global CDN, zero config
- **Render:** Simple PaaS, automatic deploys from Git
- **Cloudflare:** Free CDN, DDoS protection
- **Total cost:** ~$15-30/month for moderate traffic

### Deployment Checklist

**Pre-deployment:**
- ✅ All tests passing locally
- ✅ CI/CD pipeline green
- ✅ Environment variables configured
- ✅ Database migrations tested
- ✅ Health checks working
- ✅ Logging configured

**Deployment:**
1. Run database migrations (with backup)
2. Deploy backend (verify health check)
3. Deploy frontend (smoke test)
4. Monitor error rates for 1 hour
5. If errors spike, rollback

**Post-deployment:**
- Monitor error rates
- Check response times
- Verify core features work
- Review logs for warnings

---

## Action Items for Week 15

### Critical (Must Complete)

**1. Deploy to Managed Database** (3-4 hours)
- Create PostgreSQL instance on Render/Supabase
- Migrate data from local to cloud
- Update application to use cloud DB
- Verify connection pooling works

**Deliverable:** Production database running in cloud

**2. Understand K8s Basics** (4-5 hours)
- Install Minikube or use Docker Desktop K8s
- Deploy Bibby to local K8s cluster
- Create deployment, service, configmap
- Practice scaling and rolling updates

**Deliverable:** Can explain K8s concepts in interview

**3. Optimize Cloud Costs** (2-3 hours)
- Calculate current monthly costs
- Right-size resources (don't over-provision)
- Set up billing alerts
- Document cost breakdown

**Deliverable:** Cost-optimized deployment

**4. Implement Health Checks** (3-4 hours)
- Add custom health indicators
- Configure liveness and readiness probes
- Test graceful shutdown
- Verify in K8s

**Deliverable:** Production-ready health monitoring

**5. Create Deployment Documentation** (2-3 hours)
- Document cloud architecture
- Write deployment runbook
- Include rollback procedure
- Add to README

**Deliverable:** Clear deployment docs

### Important (Should Complete)

**6. Set Up Read Replica** (3-4 hours)
- Configure read replica in database
- Route analytics queries to replica
- Measure performance improvement

**7. Multi-Environment Setup** (2-3 hours)
- Create staging environment
- Configure environment-specific properties
- Test deployment to staging

### Bonus (If Time Permits)

**8. Kubernetes Deep Dive** (5-6 hours)
- Learn Helm charts
- Set up monitoring with Prometheus
- Configure auto-scaling

**9. Multi-Cloud Deployment** (4-5 hours)
- Deploy same app to AWS and GCP
- Compare costs and features

---

## Success Metrics for Week 15

By the end of this week, you should have:

✅ **Cloud Deployment:**
- Production database running on managed service
- Application deployed to cloud platform
- Environment variables properly configured

✅ **K8s Knowledge:**
- Can explain pods, deployments, services
- Deployed app to local K8s cluster
- Understand when to use K8s vs simpler platforms

✅ **Cost Optimization:**
- Monthly costs calculated and documented
- Resources right-sized for actual load
- Billing alerts configured

✅ **Production Readiness:**
- Health checks implemented
- Graceful shutdown configured
- Deployment runbook documented

---

## Interview Talking Points

### Cloud Platform Knowledge

> "I deployed Bibby to Render for simplicity—managed platform handles HTTPS, scaling, and monitoring. For the database, I use managed PostgreSQL with automatic backups and point-in-time recovery. Similar to how we had maintenance contracts for critical equipment—let the experts handle it."

### Kubernetes Understanding

> "Kubernetes orchestrates containers at scale. I've deployed Bibby to a local K8s cluster to understand the concepts—pods, deployments, services. For Bibby's scale, a managed platform like Render is simpler. But I understand when K8s makes sense: microservices, auto-scaling, multi-cloud. It's like a DCS system managing hundreds of control loops in a refinery—handles complexity that manual operation couldn't."

### Cost Optimization

> "I right-size resources based on actual metrics, not assumptions. Started with 2 CPU / 4GB RAM, monitored for a week, found 1 CPU / 2GB handles load fine. Similar to energy optimization in operations—measure first, then optimize. Set up billing alerts at $50/month to catch unexpected spikes."

---

## What's Next

**Section 16: Technical Portfolio Assembly**

You've built the projects (Sections 10-13) and deployment infrastructure (Sections 14-15). Section 16 focuses on **presentation**:
- Portfolio website showcasing projects
- GitHub profile optimization
- Project README best practices
- Technical writing samples
- Video demonstrations

---

**Progress Tracker:** 15/32 sections complete (46.875% - Nearly halfway!)

**Next Section:** Technical Portfolio Assembly — Making your work visible and impressive
