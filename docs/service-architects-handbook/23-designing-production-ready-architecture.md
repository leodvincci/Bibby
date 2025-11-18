# Section 23: Designing a Production-Ready Architecture

**Previous:** [Section 22: Economics of Microservice Architectures](22-economics-of-microservice-architectures.md)
**Next:** [Section 24: Becoming a Systems Engineer](24-becoming-a-systems-engineer.md)

---

## The Final Blueprint

You've learned the theory. You've seen the patterns. You've studied the failures. You've calculated the costs.

**Now let's build it.**

This section is the **complete reference architecture** for a production-ready microservices system based on Bibby. Everything you've learned in the previous 22 sections comes together here:

- Domain-driven design boundaries
- REST APIs with proper error handling
- Authentication and authorization
- Database-per-service with event-driven sync
- Resilience patterns (circuit breakers, retries, bulkheads)
- Containerization and orchestration
- CI/CD pipelines
- Full observability stack
- API Gateway and service mesh
- Saga pattern for distributed transactions
- Performance optimizations

**This is production-ready code you can deploy tomorrow.**

Let's build Bibby the right way.

---

## Part 1: Architecture Overview

### The Big Picture

```
                                    ┌─────────────────┐
                                    │   DNS / CDN     │
                                    │  (Cloudflare)   │
                                    └────────┬────────┘
                                             │
                                    ┌────────▼────────┐
                                    │  API Gateway    │
                                    │    (Kong)       │
                                    │  - Auth         │
                                    │  - Rate limit   │
                                    │  - Metrics      │
                                    └────────┬────────┘
                                             │
                        ┌────────────────────┼────────────────────┐
                        │                    │                    │
              ┌─────────▼─────────┐ ┌───────▼────────┐  ┌───────▼────────┐
              │ Catalog Service   │ │ Circulation    │  │  Notification  │
              │  (Books, Search)  │ │  Service       │  │    Service     │
              │                   │ │ (Checkouts)    │  │   (Emails)     │
              └─────────┬─────────┘ └───────┬────────┘  └───────┬────────┘
                        │                   │                    │
              ┌─────────▼─────────┐ ┌───────▼────────┐  ┌───────▼────────┐
              │ PostgreSQL        │ │ PostgreSQL     │  │  PostgreSQL    │
              │ (catalog_db)      │ │ (circulation)  │  │ (notifications)│
              └───────────────────┘ └────────────────┘  └────────────────┘
                        │                   │                    │
                        └───────────────────┼────────────────────┘
                                            │
                                   ┌────────▼─────────┐
                                   │  Kafka Cluster   │
                                   │  (3 brokers)     │
                                   │  - book-events   │
                                   │  - checkout-saga │
                                   └──────────────────┘
                                            │
                        ┌───────────────────┼───────────────────┐
                        │                   │                   │
              ┌─────────▼─────────┐ ┌───────▼────────┐ ┌──────▼──────────┐
              │  Redis Cache      │ │  Prometheus    │ │   Jaeger        │
              │  (search results) │ │  (metrics)     │ │  (tracing)      │
              └───────────────────┘ └───────┬────────┘ └─────────────────┘
                                            │
                                   ┌────────▼─────────┐
                                   │    Grafana       │
                                   │  (dashboards)    │
                                   └──────────────────┘
```

### Service Responsibilities

**Catalog Service:**
- Manage books, authors, shelves, bookcases
- Search and filtering
- Read-heavy (90% reads, 10% writes)
- Publishes: `BookCreated`, `BookUpdated`, `BookDeleted`

**Circulation Service:**
- Handle checkouts, returns, holds, fines
- Complex business logic (sagas)
- Write-heavy (60% writes, 40% reads)
- Publishes: `BookCheckedOut`, `BookReturned`, `HoldPlaced`, `FineAssessed`
- Consumes: `BookCreated` (to maintain local book metadata)

**Notification Service:**
- Send emails, SMS, push notifications
- Event-driven (no synchronous dependencies)
- Publishes: `NotificationSent`, `NotificationFailed`
- Consumes: All events that trigger notifications

---

## Part 2: Security Architecture

### Authentication Flow (OAuth2 + JWT)

```
┌─────────┐        ┌─────────────┐        ┌──────────────┐
│ Client  │──1──▶  │ API Gateway │──2──▶  │ Auth Service │
│  (Web)  │        │   (Kong)    │        │  (Keycloak)  │
└─────────┘        └─────────────┘        └──────────────┘
     │                    │                        │
     │                    │                        │
     │◀────────3──────────┼────────────────────────┘
     │              (JWT token)
     │
     │──────4─────▶ (Subsequent requests with JWT)
     │
     │◀─────5─────
     │          (Protected resource)
```

**Step-by-step:**

1. Client requests login
2. API Gateway forwards to Auth Service
3. Auth Service validates credentials, returns JWT
4. Client includes JWT in `Authorization: Bearer <token>` header
5. API Gateway validates JWT, forwards to service

### Kong API Gateway Configuration

```yaml
# kong.yaml
_format_version: "3.0"

services:
- name: catalog-service
  url: http://catalog-service.bibby.svc.cluster.local:8080
  routes:
  - name: catalog-route
    paths:
    - /api/v1/books
    - /api/v1/authors
    - /api/v1/search
    methods:
    - GET
    - POST
    - PUT
    - DELETE
  plugins:
  - name: jwt
    config:
      key_claim_name: iss
  - name: rate-limiting
    config:
      minute: 100
      policy: local
  - name: prometheus
  - name: correlation-id
    config:
      header_name: X-Correlation-ID

- name: circulation-service
  url: http://circulation-service.bibby.svc.cluster.local:8080
  routes:
  - name: circulation-route
    paths:
    - /api/v1/checkouts
    - /api/v1/returns
    - /api/v1/holds
    plugins:
  - name: jwt
  - name: rate-limiting
    config:
      minute: 50
  - name: request-size-limiting
    config:
      allowed_payload_size: 10  # 10 MB max

plugins:
- name: cors
  config:
    origins:
    - https://bibby.com
    - https://app.bibby.com
    methods:
    - GET
    - POST
    - PUT
    - DELETE
    - OPTIONS
    headers:
    - Authorization
    - Content-Type
    - X-Correlation-ID
    exposed_headers:
    - X-RateLimit-Remaining
    - X-RateLimit-Limit
    credentials: true
    max_age: 3600
```

### Service-to-Service Authentication (mTLS)

```yaml
# istio-mtls.yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: bibby
spec:
  mtls:
    mode: STRICT  # Enforce mTLS for all service-to-service communication

---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: catalog-authz
  namespace: bibby
spec:
  selector:
    matchLabels:
      app: catalog-service
  action: ALLOW
  rules:
  - from:
    - source:
        principals:
        - cluster.local/ns/bibby/sa/circulation-service
        - cluster.local/ns/bibby/sa/api-gateway
    to:
    - operation:
        methods: ["GET", "POST"]
        paths: ["/api/v1/*"]
```

---

## Part 3: Database Architecture

### Schema Design (Catalog Service)

```sql
-- catalog_db

CREATE TABLE books (
    book_id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    isbn VARCHAR(13) UNIQUE,
    publication_year INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INT DEFAULT 0  -- Optimistic locking
);

CREATE INDEX idx_books_title_lower ON books (LOWER(title));
CREATE INDEX idx_books_isbn ON books (isbn);

CREATE TABLE authors (
    author_id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_authors_name ON authors (last_name, first_name);

CREATE TABLE book_authors (
    book_id BIGINT REFERENCES books(book_id) ON DELETE CASCADE,
    author_id BIGINT REFERENCES authors(author_id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, author_id)
);

CREATE TABLE shelves (
    shelf_id BIGSERIAL PRIMARY KEY,
    shelf_label VARCHAR(50) NOT NULL,
    bookcase_id BIGINT NOT NULL,
    capacity INT DEFAULT 50,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shelves_bookcase ON shelves (bookcase_id);

CREATE TABLE bookcases (
    bookcase_id BIGSERIAL PRIMARY KEY,
    bookcase_label VARCHAR(100) NOT NULL,
    shelf_capacity INT DEFAULT 10,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Event outbox for reliable event publishing
CREATE TABLE event_outbox (
    event_id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING'
);

CREATE INDEX idx_outbox_status ON event_outbox (status, created_at);
```

### Schema Design (Circulation Service)

```sql
-- circulation_db

-- Local copy of book metadata (denormalized from Catalog)
CREATE TABLE circulation_books (
    book_id BIGINT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    last_synced TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE patrons (
    patron_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE checkouts (
    checkout_id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL REFERENCES circulation_books(book_id),
    patron_id BIGINT NOT NULL REFERENCES patrons(patron_id),
    checked_out_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP NOT NULL,
    returned_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, RETURNED, OVERDUE
    saga_id VARCHAR(36),  -- For saga coordination
    CONSTRAINT unique_active_checkout UNIQUE (book_id, status) WHERE status = 'ACTIVE'
);

CREATE INDEX idx_checkouts_patron ON checkouts (patron_id);
CREATE INDEX idx_checkouts_book ON checkouts (book_id);
CREATE INDEX idx_checkouts_saga ON checkouts (saga_id);

CREATE TABLE fines (
    fine_id BIGSERIAL PRIMARY KEY,
    patron_id BIGINT NOT NULL REFERENCES patrons(patron_id),
    checkout_id BIGINT REFERENCES checkouts(checkout_id),
    amount DECIMAL(10, 2) NOT NULL,
    reason VARCHAR(255),
    assessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'UNPAID'
);

CREATE INDEX idx_fines_patron ON fines (patron_id);

-- Saga state tracking
CREATE TABLE checkout_sagas (
    saga_id VARCHAR(36) PRIMARY KEY,
    book_id BIGINT NOT NULL,
    patron_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'STARTED',  -- STARTED, COMPLETED, FAILED, ROLLED_BACK
    current_step INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sagas_status ON checkout_sagas (status, created_at);

-- Event outbox
CREATE TABLE event_outbox (
    event_id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING'
);
```

### Connection Pool Configuration

```yaml
# catalog-service application.yml
spring:
  datasource:
    url: jdbc:postgresql://catalog-db.bibby.svc.cluster.local:5432/catalog
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 5000
      idle-timeout: 300000
      max-lifetime: 600000
      leak-detection-threshold: 60000

      # Connection test query
      connection-test-query: SELECT 1

      # Pool name for metrics
      pool-name: CatalogHikariPool

  jpa:
    hibernate:
      ddl-auto: validate  # Never auto-update schema in production
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
        query:
          in_clause_parameter_padding: true
```

---

## Part 4: Event-Driven Architecture

### Event Schema (Avro)

```avro
// book-created-event.avsc
{
  "type": "record",
  "name": "BookCreatedEvent",
  "namespace": "com.penrose.bibby.catalog.events",
  "fields": [
    {"name": "bookId", "type": "long"},
    {"name": "title", "type": "string"},
    {"name": "isbn", "type": ["null", "string"], "default": null},
    {"name": "authors", "type": {
      "type": "array",
      "items": {
        "type": "record",
        "name": "Author",
        "fields": [
          {"name": "authorId", "type": "long"},
          {"name": "firstName", "type": "string"},
          {"name": "lastName", "type": "string"}
        ]
      }
    }},
    {"name": "timestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "correlationId", "type": "string"}
  ]
}
```

### Outbox Pattern Implementation

```java
// CatalogService.java
@Service
@Transactional
public class CatalogService {

    private final BookRepository bookRepository;
    private final EventOutboxRepository outboxRepository;

    public BookEntity createBook(CreateBookRequest request) {
        // 1. Save to database
        BookEntity book = new BookEntity();
        book.setTitle(request.getTitle());
        book.setIsbn(request.getIsbn());
        book = bookRepository.save(book);

        // 2. Write event to outbox (same transaction)
        EventOutbox event = new EventOutbox();
        event.setAggregateType("Book");
        event.setAggregateId(book.getBookId());
        event.setEventType("BookCreated");
        event.setPayload(toJson(new BookCreatedEvent(book)));
        event.setStatus(OutboxStatus.PENDING);
        outboxRepository.save(event);

        // Both writes committed atomically
        return book;
    }
}

// EventPublisher.java (background job)
@Component
@Slf4j
public class EventPublisher {

    @Scheduled(fixedDelay = 1000)  // Poll every 1 second
    @Transactional
    public void publishPendingEvents() {
        List<EventOutbox> pending = outboxRepository.findByStatusOrderByCreatedAt(
            OutboxStatus.PENDING, PageRequest.of(0, 100)
        );

        for (EventOutbox event : pending) {
            try {
                // Publish to Kafka
                kafkaTemplate.send("book-events", event.getPayload());

                // Mark as published
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);

                log.info("Published event: {}", event.getEventType());

            } catch (Exception e) {
                log.error("Failed to publish event {}: {}", event.getEventId(), e.getMessage());
                // Event remains PENDING, will retry on next poll
            }
        }
    }
}
```

### Kafka Configuration

```yaml
# kafka-topics.yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: book-events
  namespace: bibby
  labels:
    strimzi.io/cluster: bibby-kafka
spec:
  partitions: 6
  replicas: 3
  config:
    retention.ms: 604800000  # 7 days
    compression.type: snappy
    min.insync.replicas: 2

---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: checkout-saga
  namespace: bibby
spec:
  partitions: 3
  replicas: 3
  config:
    retention.ms: 86400000  # 1 day
```

---

## Part 5: Resilience Patterns

### Circuit Breaker Configuration

```java
// Resilience4jConfig.java
@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerConfig catalogCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(5)
            .recordExceptions(Exception.class)
            .ignoreExceptions(BusinessException.class)
            .build();
    }

    @Bean
    public RetryConfig catalogRetryConfig() {
        return RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .retryExceptions(TimeoutException.class, ConnectException.class)
            .ignoreExceptions(BusinessException.class)
            .build();
    }

    @Bean
    public TimeLimiterConfig catalogTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
    }
}

// CatalogClient.java
@Service
public class CatalogClient {

    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "catalog", fallbackMethod = "getBookFallback")
    @Retry(name = "catalog")
    @TimeLimiter(name = "catalog")
    public CompletableFuture<Book> getBook(Long bookId) {
        return CompletableFuture.supplyAsync(() ->
            restTemplate.getForObject(
                "http://catalog-service:8080/api/v1/books/" + bookId,
                Book.class
            )
        );
    }

    private CompletableFuture<Book> getBookFallback(Long bookId, Throwable t) {
        log.warn("Catalog service unavailable, using fallback for book {}", bookId);

        // Return cached data or degraded response
        return CompletableFuture.completedFuture(
            cache.get(bookId).orElse(Book.unavailable(bookId))
        );
    }
}
```

---

## Part 6: Observability Stack

### Complete Monitoring Configuration

```yaml
# prometheus-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: bibby
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
      external_labels:
        cluster: 'bibby-production'

    rule_files:
      - '/etc/prometheus/rules/*.yml'

    alerting:
      alertmanagers:
      - static_configs:
        - targets:
          - alertmanager:9093

    scrape_configs:
      - job_name: 'kubernetes-pods'
        kubernetes_sd_configs:
        - role: pod
          namespaces:
            names:
            - bibby
        relabel_configs:
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
          action: keep
          regex: true
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
          action: replace
          target_label: __metrics_path__
          regex: (.+)
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_port]
          action: replace
          regex: ([^:]+)(?::\d+)?;(\d+)
          replacement: $1:$2
          target_label: __address__

      - job_name: 'kafka'
        static_configs:
        - targets:
          - kafka-0.kafka-brokers:9090
          - kafka-1.kafka-brokers:9090
          - kafka-2.kafka-brokers:9090

      - job_name: 'postgres'
        static_configs:
        - targets:
          - postgres-exporter:9187
```

### Alert Rules

```yaml
# alert-rules.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-alerts
  namespace: bibby
data:
  alerts.yml: |
    groups:
    - name: bibby-services
      interval: 30s
      rules:
      - alert: HighErrorRate
        expr: |
          (
            rate(http_server_requests_seconds_count{status=~"5..",namespace="bibby"}[5m])
            /
            rate(http_server_requests_seconds_count{namespace="bibby"}[5m])
          ) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate on {{ $labels.service }}"
          description: "Error rate is {{ $value | humanizePercentage }}"

      - alert: HighLatency
        expr: |
          histogram_quantile(0.95,
            rate(http_server_requests_seconds_bucket{namespace="bibby"}[5m])
          ) > 1
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High latency on {{ $labels.service }}"
          description: "P95 latency is {{ $value }}s"

      - alert: ServiceDown
        expr: up{namespace="bibby"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.service }} is down"

      - alert: PodCrashLooping
        expr: rate(kube_pod_container_status_restarts_total{namespace="bibby"}[15m]) > 0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Pod {{ $labels.pod }} is crash looping"

      - alert: HighMemoryUsage
        expr: |
          (
            container_memory_working_set_bytes{namespace="bibby"}
            /
            container_spec_memory_limit_bytes{namespace="bibby"}
          ) > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage on {{ $labels.pod }}"
          description: "Memory usage is {{ $value | humanizePercentage }}"

      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active >= hikaricp_connections_max
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool exhausted"

      - alert: KafkaConsumerLag
        expr: kafka_consumergroup_lag > 1000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High Kafka consumer lag"
          description: "Consumer lag is {{ $value }} messages"
```

### Grafana Dashboards

```json
{
  "dashboard": {
    "title": "Bibby Production Overview",
    "panels": [
      {
        "title": "Request Rate (RPS)",
        "targets": [{
          "expr": "sum(rate(http_server_requests_seconds_count{namespace='bibby'}[5m])) by (service)"
        }],
        "type": "graph"
      },
      {
        "title": "Error Rate",
        "targets": [{
          "expr": "sum(rate(http_server_requests_seconds_count{status=~'5..',namespace='bibby'}[5m])) by (service)"
        }]
      },
      {
        "title": "P50/P95/P99 Latency",
        "targets": [
          {
            "expr": "histogram_quantile(0.50, rate(http_server_requests_seconds_bucket{namespace='bibby'}[5m]))",
            "legendFormat": "P50"
          },
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{namespace='bibby'}[5m]))",
            "legendFormat": "P95"
          },
          {
            "expr": "histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{namespace='bibby'}[5m]))",
            "legendFormat": "P99"
          }
        ]
      },
      {
        "title": "Database Connection Pool",
        "targets": [{
          "expr": "hikaricp_connections_active",
          "legendFormat": "Active"
        }, {
          "expr": "hikaricp_connections_idle",
          "legendFormat": "Idle"
        }, {
          "expr": "hikaricp_connections_max",
          "legendFormat": "Max"
        }]
      },
      {
        "title": "Kafka Consumer Lag",
        "targets": [{
          "expr": "kafka_consumergroup_lag"
        }]
      },
      {
        "title": "JVM Memory Usage",
        "targets": [{
          "expr": "jvm_memory_used_bytes{area='heap'} / jvm_memory_max_bytes{area='heap'}"
        }]
      }
    ]
  }
}
```

---

## Part 7: Complete CI/CD Pipeline

```yaml
# .github/workflows/catalog-service-deploy.yml
name: Catalog Service CI/CD

on:
  push:
    branches: [main]
    paths:
      - 'services/catalog/**'
  pull_request:
    paths:
      - 'services/catalog/**'

env:
  SERVICE_NAME: catalog-service
  AWS_REGION: us-east-1
  ECR_REPOSITORY: bibby/catalog-service

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: catalog_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven

    - name: Run unit tests
      working-directory: services/catalog
      run: mvn test

    - name: Run integration tests
      working-directory: services/catalog
      run: mvn verify -P integration-tests
      env:
        DB_URL: jdbc:postgresql://localhost:5432/catalog_test
        DB_USERNAME: test
        DB_PASSWORD: test

    - name: Upload test coverage
      uses: codecov/codecov-action@v3
      with:
        files: services/catalog/target/site/jacoco/jacoco.xml

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.event_name == 'push'

    steps:
    - uses: actions/checkout@v3

    - name: Build JAR
      working-directory: services/catalog
      run: mvn clean package -DskipTests

    - name: Build Docker image
      run: |
        docker build -t $SERVICE_NAME:${{ github.sha }} \
          -f services/catalog/Dockerfile \
          services/catalog

    - name: Scan with Trivy
      uses: aquasecurity/trivy-action@master
      with:
        image-ref: ${{ env.SERVICE_NAME }}:${{ github.sha }}
        format: 'sarif'
        output: 'trivy-results.sarif'
        severity: 'CRITICAL,HIGH'
        exit-code: '1'

    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v2
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: ${{ env.AWS_REGION }}

    - name: Login to Amazon ECR
      run: |
        aws ecr get-login-password --region $AWS_REGION | \
        docker login --username AWS --password-stdin \
        ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.$AWS_REGION.amazonaws.com

    - name: Push to ECR
      run: |
        docker tag $SERVICE_NAME:${{ github.sha }} \
          ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:${{ github.sha }}

        docker tag $SERVICE_NAME:${{ github.sha }} \
          ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:latest

        docker push ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:${{ github.sha }}
        docker push ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:latest

  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    environment: staging

    steps:
    - uses: actions/checkout@v3

    - name: Update Kubernetes manifests
      run: |
        cd k8s/staging
        kustomize edit set image catalog-service=${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:${{ github.sha }}

    - name: Deploy to staging
      run: |
        kubectl apply -k k8s/staging

    - name: Wait for rollout
      run: |
        kubectl rollout status deployment/catalog-service -n bibby-staging --timeout=10m

    - name: Run smoke tests
      run: |
        ./scripts/smoke-tests.sh staging

  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment: production

    steps:
    - uses: actions/checkout@v3

    - name: Update Kubernetes manifests
      run: |
        cd k8s/production
        kustomize edit set image catalog-service=${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:${{ github.sha }}

    - name: Deploy to production (canary)
      run: |
        kubectl apply -k k8s/production/canary

    - name: Monitor canary (10% traffic for 10 minutes)
      run: |
        ./scripts/monitor-canary.sh 600

    - name: Promote canary or rollback
      run: |
        ERROR_RATE=$(./scripts/check-error-rate.sh)
        if [ "$ERROR_RATE" -lt "1" ]; then
          echo "Canary healthy, promoting to 100%"
          kubectl apply -k k8s/production
        else
          echo "Canary unhealthy, rolling back"
          kubectl rollout undo deployment/catalog-service -n bibby-production
          exit 1
        fi
```

---

## Part 8: Production Checklist

### Pre-Launch Requirements

**Security:**
- [ ] All endpoints require authentication
- [ ] Rate limiting configured
- [ ] CORS policies defined
- [ ] Secrets stored in vault (not environment variables)
- [ ] mTLS enabled between services
- [ ] Security scanning in CI pipeline
- [ ] Dependency vulnerability scanning

**Reliability:**
- [ ] Circuit breakers on all external calls
- [ ] Retry logic with exponential backoff
- [ ] Timeouts configured (5s max for synchronous calls)
- [ ] Bulkheads (separate thread pools)
- [ ] Health checks (liveness + readiness)
- [ ] Graceful shutdown (30s drain period)

**Observability:**
- [ ] Structured logging (JSON format)
- [ ] Correlation IDs on all requests
- [ ] Distributed tracing enabled
- [ ] Metrics exported (Prometheus format)
- [ ] Dashboards created (Grafana)
- [ ] Alerts configured (Prometheus Alertmanager)
- [ ] On-call rotation defined

**Performance:**
- [ ] Database indexes on frequently queried columns
- [ ] Connection pooling configured
- [ ] Caching strategy defined (Redis for read-heavy data)
- [ ] Load testing completed (target: 1000 RPS)
- [ ] P95 latency < 300ms
- [ ] P99 latency < 1s

**Data:**
- [ ] Database backups automated (daily)
- [ ] Backup restoration tested
- [ ] Data retention policies defined
- [ ] GDPR/data privacy compliance
- [ ] Database migrations versioned (Flyway/Liquibase)

**Deployment:**
- [ ] CI/CD pipeline automated
- [ ] Canary deployments configured
- [ ] Rollback procedure documented
- [ ] Blue-green deployment capability
- [ ] Zero-downtime deployments verified

**Disaster Recovery:**
- [ ] RTO (Recovery Time Objective) defined: 1 hour
- [ ] RPO (Recovery Point Objective) defined: 5 minutes
- [ ] Multi-region failover plan
- [ ] Runbooks for common incidents
- [ ] Chaos engineering tests (kill random pods)

---

## Summary: The Production-Ready Stack

**You now have:**

1. **3-service architecture** with clear boundaries
2. **Security** at every layer (API Gateway auth, mTLS, secrets management)
3. **Database-per-service** with event-driven synchronization
4. **Resilience** patterns (circuit breakers, retries, bulkheads)
5. **Full observability** (logs, metrics, traces, dashboards, alerts)
6. **Automated CI/CD** with canary deployments
7. **Performance optimizations** (caching, indexes, connection pooling)
8. **Disaster recovery** procedures

**Total infrastructure cost:** ~$560/month (AWS)
**Team requirement:** 2-3 backend engineers + 0.5 platform engineer
**Expected uptime:** 99.9% (< 44 minutes downtime/month)

**This is a production-ready architecture you can deploy tomorrow.**

---

## Further Reading

**Reference Architectures:**
- **AWS Well-Architected Framework:** https://aws.amazon.com/architecture/well-architected/
- **Google SRE Book:** https://sre.google/sre-book/table-of-contents/
- **12 Factor App:** https://12factor.net/

**Production Readiness:**
- **"Production-Ready Microservices"** by Susan J. Fowler
- **"Site Reliability Engineering"** by Google
- **"Accelerate"** by Forsgren, Humble, Kim

---

**Next:** [Section 24: Becoming a Systems Engineer](24-becoming-a-systems-engineer.md) — The final section. Your journey from junior developer to architect.
