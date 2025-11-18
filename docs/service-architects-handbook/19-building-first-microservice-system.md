# Section 19: Building Your First Microservice System

**Previous:** [Section 18: Anti-Patterns in Microservices](18-anti-patterns-in-microservices.md)
**Next:** [Section 20: Performance Engineering](20-performance-engineering.md)

---

## The Challenge: From Theory to Practice

You've learned the patterns. You know the anti-patterns. You understand distributed systems, Docker, Kubernetes, observability, and resilience. **Now it's time to build.**

This section walks you through **extracting your first microservice from Bibby**, step by step, with complete working code. We'll:

1. **Choose the right service to extract** (hint: not the hardest one)
2. **Design the service boundary** using Domain-Driven Design
3. **Build the service** with Spring Boot, complete with error handling, observability, and health checks
4. **Handle data migration** without breaking the monolith
5. **Implement event-driven communication** to decouple the services
6. **Deploy to Kubernetes** with proper health checks and resource limits
7. **Add observability** so you can see what's happening in production
8. **Test the distributed system** end-to-end

By the end of this section, you'll have a working microservice system based on Bibby — a real foundation you can build on.

Let's start building.

---

## Step 1: Choose Your First Service

**The golden rule:** Don't extract the hardest service first. Extract the **easiest, highest-value service** to learn the patterns before tackling complex domains.

### Analyzing Bibby's Domain

Bibby has three clear bounded contexts:

1. **Catalog Service** (Books, Authors, Shelves, Bookcases)
   - **Complexity:** Medium (lots of entities, complex queries)
   - **Coupling:** High (everything depends on book data)
   - **Value:** Medium (core domain but not independent)

2. **Circulation Service** (Checkouts, Returns, Holds, Fines)
   - **Complexity:** High (complex workflows, sagas, compensations)
   - **Coupling:** High (needs book data, patron data)
   - **Value:** High (revenue impact)

3. **Notification Service** (Emails, SMS, Push notifications)
   - **Complexity:** Low (send message, track delivery)
   - **Coupling:** Low (consumes events, no direct dependencies)
   - **Value:** High (user engagement)

### The Verdict: Extract Notification Service First

**Why?**
- **Event-driven by nature:** Notifications react to events (book checked out → send email)
- **No shared data:** Doesn't need to join with books or users
- **Clear boundary:** "Send notifications" is a single responsibility
- **Independent scaling:** Email sending has different scaling needs than book searches
- **Low risk:** If it fails, it doesn't break core functionality

**This is the perfect first microservice.**

---

## Step 2: Design the Service Boundary

### Current State: Notifications in the Monolith

Right now, Bibby has no notifications. But if it did, they'd look like this:

```java
// In BookService.java (monolith)
@Service
public class BookService {

    @Autowired
    private EmailService emailService;

    @Transactional
    public void checkOutBook(Long bookId, Long patronId) {
        BookEntity book = bookRepository.findById(bookId).orElseThrow();
        book.setStatus(BookStatus.CHECKED_OUT);
        book.setPatronId(patronId);
        bookRepository.save(book);

        // Tightly coupled to email sending
        emailService.sendCheckoutConfirmation(patronId, book.getTitle());
    }
}
```

**Problem:** BookService is **coupled to email delivery**. If the email server is down, book checkouts fail.

### Future State: Notification Service

**Notification Service** is an independent service that:
- Listens to domain events (`BookCheckedOut`, `BookReturned`, `FineAssessed`)
- Sends emails, SMS, or push notifications
- Tracks delivery status
- Retries failures

```java
// In BookService.java (monolith, decoupled)
@Service
public class BookService {

    @Autowired
    private EventPublisher eventPublisher;

    @Transactional
    public void checkOutBook(Long bookId, Long patronId) {
        BookEntity book = bookRepository.findById(bookId).orElseThrow();
        book.setStatus(BookStatus.CHECKED_OUT);
        book.setPatronId(patronId);
        bookRepository.save(book);

        // Publish event and forget
        eventPublisher.publish(new BookCheckedOutEvent(
            bookId, patronId, book.getTitle(), Instant.now()
        ));
    }
}
```

**Notification Service listens to Kafka:**

```java
@Service
public class NotificationEventHandler {

    @KafkaListener(topics = "book-events", groupId = "notification-service")
    public void handleBookEvent(BookCheckedOutEvent event) {
        emailService.sendCheckoutConfirmation(event.getPatronId(), event.getBookTitle());
    }
}
```

**Now:**
- BookService doesn't care if email succeeds or fails
- Notification Service can retry independently
- Email server outages don't break checkouts

---

## Step 3: Build the Notification Service

### Project Structure

```
notification-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/penrose/bibby/notification/
│   │   │       ├── NotificationServiceApplication.java
│   │   │       ├── domain/
│   │   │       │   ├── Notification.java
│   │   │       │   ├── NotificationStatus.java
│   │   │       │   └── NotificationType.java
│   │   │       ├── application/
│   │   │       │   ├── NotificationService.java
│   │   │       │   └── events/
│   │   │       │       ├── BookCheckedOutEvent.java
│   │   │       │       └── NotificationEventHandler.java
│   │   │       ├── infrastructure/
│   │   │       │   ├── email/
│   │   │       │   │   └── SmtpEmailService.java
│   │   │       │   ├── persistence/
│   │   │       │   │   ├── NotificationEntity.java
│   │   │       │   │   └── NotificationRepository.java
│   │   │       │   └── kafka/
│   │   │       │       └── KafkaConfig.java
│   │   │       └── api/
│   │   │           ├── NotificationController.java
│   │   │           └── dto/
│   │   │               └── NotificationResponse.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           └── V1__create_notifications_table.sql
│   └── test/
│       └── java/
│           └── com/penrose/bibby/notification/
├── Dockerfile
├── pom.xml
└── README.md
```

### Domain Model

```java
// Notification.java
package com.penrose.bibby.notification.domain;

import java.time.Instant;

public class Notification {
    private Long id;
    private String recipientEmail;
    private String subject;
    private String body;
    private NotificationType type;
    private NotificationStatus status;
    private Instant createdAt;
    private Instant sentAt;
    private int retryCount;
    private String errorMessage;

    // Business logic
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    public boolean canRetry() {
        return retryCount < 3 && status == NotificationStatus.FAILED;
    }
}

public enum NotificationType {
    CHECKOUT_CONFIRMATION,
    RETURN_REMINDER,
    OVERDUE_NOTICE,
    FINE_NOTICE
}

public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED
}
```

### Infrastructure: Database Schema

```sql
-- V1__create_notifications_table.sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    patron_id BIGINT,
    book_id BIGINT,
    idempotency_key VARCHAR(255) UNIQUE
);

CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_idempotency_key ON notifications(idempotency_key);
```

### Application Service

```java
// NotificationService.java
package com.penrose.bibby.notification.application;

import com.penrose.bibby.notification.domain.*;
import com.penrose.bibby.notification.infrastructure.persistence.*;
import com.penrose.bibby.notification.infrastructure.email.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SmtpEmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                               SmtpEmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void sendCheckoutConfirmation(Long patronId, Long bookId, String bookTitle,
                                         String patronEmail, String idempotencyKey) {

        // Idempotency check
        if (notificationRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Notification already sent for idempotency key: {}", idempotencyKey);
            return;
        }

        Notification notification = new Notification();
        notification.setRecipientEmail(patronEmail);
        notification.setSubject("Book Checked Out: " + bookTitle);
        notification.setBody(buildCheckoutEmailBody(bookTitle));
        notification.setType(NotificationType.CHECKOUT_CONFIRMATION);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setPatronId(patronId);
        notification.setBookId(bookId);
        notification.setIdempotencyKey(idempotencyKey);

        NotificationEntity entity = NotificationMapper.toEntity(notification);
        notificationRepository.save(entity);

        // Send email
        try {
            emailService.send(notification.getRecipientEmail(),
                            notification.getSubject(),
                            notification.getBody());

            notification.markAsSent();
            log.info("Checkout confirmation sent to {} for book {}",
                    patronEmail, bookTitle);

        } catch (Exception e) {
            notification.markAsFailed(e.getMessage());
            log.error("Failed to send notification: {}", e.getMessage(), e);
        }

        notificationRepository.save(NotificationMapper.toEntity(notification));
    }

    private String buildCheckoutEmailBody(String bookTitle) {
        return String.format("""
            Hello,

            You have successfully checked out "%s" from the library.

            Due date: 14 days from today
            Return policy: Please return on time to avoid late fees.

            Happy reading!

            — Bibby Library System
            """, bookTitle);
    }

    @Transactional
    public void retryFailedNotifications() {
        List<NotificationEntity> failed = notificationRepository.findByStatusAndRetryCountLessThan(
            NotificationStatus.FAILED, 3
        );

        for (NotificationEntity entity : failed) {
            Notification notification = NotificationMapper.toDomain(entity);

            if (notification.canRetry()) {
                try {
                    emailService.send(notification.getRecipientEmail(),
                                    notification.getSubject(),
                                    notification.getBody());
                    notification.markAsSent();
                    log.info("Retry successful for notification {}", notification.getId());
                } catch (Exception e) {
                    notification.markAsFailed(e.getMessage());
                    log.warn("Retry failed for notification {}: {}",
                            notification.getId(), e.getMessage());
                }

                notificationRepository.save(NotificationMapper.toEntity(notification));
            }
        }
    }
}
```

### Event Handler

```java
// NotificationEventHandler.java
package com.penrose.bibby.notification.application.events;

import com.penrose.bibby.notification.application.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NotificationEventHandler {

    private final NotificationService notificationService;

    public NotificationEventHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "book-events", groupId = "notification-service")
    public void handleBookCheckedOutEvent(BookCheckedOutEvent event) {
        log.info("Received BookCheckedOutEvent: {}", event);

        // Idempotency key = event type + book ID + patron ID
        String idempotencyKey = String.format("checkout-%d-%d", event.getBookId(), event.getPatronId());

        notificationService.sendCheckoutConfirmation(
            event.getPatronId(),
            event.getBookId(),
            event.getBookTitle(),
            event.getPatronEmail(),
            idempotencyKey
        );
    }

    @KafkaListener(topics = "book-events", groupId = "notification-service")
    public void handleBookReturnedEvent(BookReturnedEvent event) {
        log.info("Received BookReturnedEvent: {}", event);

        String idempotencyKey = String.format("return-%d-%d", event.getBookId(), event.getPatronId());

        notificationService.sendReturnConfirmation(
            event.getPatronId(),
            event.getBookId(),
            event.getBookTitle(),
            event.getPatronEmail(),
            idempotencyKey
        );
    }
}

// Event DTOs
public record BookCheckedOutEvent(
    Long bookId,
    Long patronId,
    String bookTitle,
    String patronEmail,
    Instant timestamp
) {}

public record BookReturnedEvent(
    Long bookId,
    Long patronId,
    String bookTitle,
    String patronEmail,
    Instant timestamp
) {}
```

### REST API (for querying notification history)

```java
// NotificationController.java
package com.penrose.bibby.notification.api;

import com.penrose.bibby.notification.domain.*;
import com.penrose.bibby.notification.infrastructure.persistence.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/patron/{patronId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByPatron(
            @PathVariable Long patronId) {

        List<NotificationEntity> notifications = notificationRepository.findByPatronId(patronId);

        List<NotificationResponse> response = notifications.stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotification(@PathVariable Long id) {
        return notificationRepository.findById(id)
            .map(this::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    private NotificationResponse toResponse(NotificationEntity entity) {
        return new NotificationResponse(
            entity.getId(),
            entity.getRecipientEmail(),
            entity.getSubject(),
            entity.getType(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getSentAt()
        );
    }
}

public record NotificationResponse(
    Long id,
    String recipientEmail,
    String subject,
    NotificationType type,
    NotificationStatus status,
    Instant createdAt,
    Instant sentAt
) {}
```

### Configuration

```yaml
# application.yml
spring:
  application:
    name: notification-service

  datasource:
    url: jdbc:postgresql://postgres:5432/notifications
    username: notifications_user
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: notification-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serialization.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.penrose.bibby.*

  mail:
    host: ${SMTP_HOST}
    port: ${SMTP_PORT}
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

server:
  port: 8081

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    com.penrose.bibby: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

---

## Step 4: Update the Monolith to Publish Events

### Add Kafka Producer to Bibby

**Add dependency to `pom.xml`:**

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**Configure Kafka:**

```yaml
# application.properties (or application.yml)
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serialization.JsonSerializer
```

**Create Event Publisher:**

```java
// EventPublisher.java
package com.penrose.bibby.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, Object event) {
        log.info("Publishing event to topic {}: {}", topic, event);
        kafkaTemplate.send(topic, event);
    }
}
```

**Update BookService:**

```java
// BookService.java (updated)
package com.penrose.bibby.library.book;

import com.penrose.bibby.events.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final EventPublisher eventPublisher;

    public BookService(BookRepository bookRepository, EventPublisher eventPublisher) {
        this.bookRepository = bookRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void checkOutBook(BookEntity book) {
        book.setBookStatus("CHECKED_OUT");
        bookRepository.save(book);

        // Publish event
        BookCheckedOutEvent event = new BookCheckedOutEvent(
            book.getBookId(),
            book.getPatronId(),  // Assuming this exists
            book.getTitle(),
            "patron@example.com",  // Get from patron service
            Instant.now()
        );

        eventPublisher.publish("book-events", event);
    }
}
```

**Now the monolith publishes events, and Notification Service consumes them.**

---

## Step 5: Deploy to Kubernetes

### Notification Service Dockerfile

```dockerfile
# Dockerfile
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S notification && adduser -S notification -G notification

COPY --from=builder /build/target/notification-service-0.0.1-SNAPSHOT.jar app.jar

RUN chown -R notification:notification /app
USER notification

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
```

### Kubernetes Deployment

```yaml
# notification-service-deployment.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: bibby

---
apiVersion: v1
kind: Secret
metadata:
  name: notification-secrets
  namespace: bibby
type: Opaque
stringData:
  db-password: "changeme"
  smtp-password: "smtp-secret"

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: notification-config
  namespace: bibby
data:
  SMTP_HOST: "smtp.gmail.com"
  SMTP_PORT: "587"
  SMTP_USERNAME: "library@example.com"

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: notification-service
  namespace: bibby
spec:
  replicas: 2
  selector:
    matchLabels:
      app: notification-service
  template:
    metadata:
      labels:
        app: notification-service
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8081"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
      - name: notification
        image: notification-service:1.0
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 8081
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: notification-secrets
              key: db-password
        - name: SMTP_HOST
          valueFrom:
            configMapKeyRef:
              name: notification-config
              key: SMTP_HOST
        - name: SMTP_PORT
          valueFrom:
            configMapKeyRef:
              name: notification-config
              key: SMTP_PORT
        - name: SMTP_USERNAME
          valueFrom:
            configMapKeyRef:
              name: notification-config
              key: SMTP_USERNAME
        - name: SMTP_PASSWORD
          valueFrom:
            secretKeyRef:
              name: notification-secrets
              key: smtp-password
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 5
          failureThreshold: 3

---
apiVersion: v1
kind: Service
metadata:
  name: notification-service
  namespace: bibby
spec:
  selector:
    app: notification-service
  ports:
  - port: 8081
    targetPort: 8081
    name: http
  type: ClusterIP

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: notification-service-hpa
  namespace: bibby
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: notification-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

---

## Step 6: Add Observability

### Prometheus Metrics

Spring Boot Actuator + Micrometer automatically exposes:
- JVM metrics (heap, GC, threads)
- HTTP request metrics (latency, throughput, errors)
- Database connection pool metrics

**Add custom metrics:**

```java
// NotificationService.java (updated)
@Service
@Slf4j
public class NotificationService {

    private final Counter notificationsSent;
    private final Counter notificationsFailed;
    private final Timer emailSendDuration;

    public NotificationService(NotificationRepository notificationRepository,
                               SmtpEmailService emailService,
                               MeterRegistry meterRegistry) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;

        // Custom metrics
        this.notificationsSent = Counter.builder("notifications.sent")
            .description("Total notifications sent successfully")
            .tag("type", "email")
            .register(meterRegistry);

        this.notificationsFailed = Counter.builder("notifications.failed")
            .description("Total notifications that failed to send")
            .tag("type", "email")
            .register(meterRegistry);

        this.emailSendDuration = Timer.builder("notifications.email.send.duration")
            .description("Time taken to send an email")
            .register(meterRegistry);
    }

    @Transactional
    public void sendCheckoutConfirmation(...) {
        // ... idempotency check ...

        try {
            emailSendDuration.record(() -> {
                emailService.send(notification.getRecipientEmail(),
                                notification.getSubject(),
                                notification.getBody());
            });

            notification.markAsSent();
            notificationsSent.increment();

        } catch (Exception e) {
            notification.markAsFailed(e.getMessage());
            notificationsFailed.increment();
            log.error("Failed to send notification: {}", e.getMessage(), e);
        }

        notificationRepository.save(NotificationMapper.toEntity(notification));
    }
}
```

### Structured Logging

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>correlationId</includeMdcKeyName>
            <includeMdcKeyName>patronId</includeMdcKeyName>
            <includeMdcKeyName>bookId</includeMdcKeyName>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

### Distributed Tracing

**Add OpenTelemetry:**

```xml
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # Sample 100% of requests in dev, 10% in prod
  zipkin:
    tracing:
      endpoint: http://jaeger:9411/api/v2/spans
```

**Now every request gets a trace ID that follows it through Catalog → Kafka → Notification Service.**

---

## Step 7: Test the System End-to-End

### Integration Test

```java
// NotificationServiceIntegrationTest.java
@SpringBootTest
@Testcontainers
class NotificationServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("notifications_test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void shouldSendCheckoutConfirmationWhenEventReceived() throws Exception {
        // Publish event to Kafka
        BookCheckedOutEvent event = new BookCheckedOutEvent(
            123L, 456L, "Clean Code", "patron@example.com", Instant.now()
        );

        kafkaTemplate.send("book-events", event).get();

        // Wait for event processing
        await().atMost(Duration.ofSeconds(5)).until(() ->
            notificationRepository.findByBookId(123L).isPresent()
        );

        // Verify notification was created
        NotificationEntity notification = notificationRepository.findByBookId(123L).orElseThrow();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSubject()).contains("Clean Code");
    }

    @Test
    void shouldRetryFailedNotifications() {
        // Create a failed notification
        NotificationEntity failed = new NotificationEntity();
        failed.setRecipientEmail("patron@example.com");
        failed.setSubject("Test");
        failed.setBody("Test body");
        failed.setStatus(NotificationStatus.FAILED);
        failed.setRetryCount(0);
        notificationRepository.save(failed);

        // Trigger retry
        notificationService.retryFailedNotifications();

        // Verify retry attempt
        NotificationEntity updated = notificationRepository.findById(failed.getId()).orElseThrow();
        assertThat(updated.getRetryCount()).isEqualTo(1);
    }
}
```

### Performance Test

```java
// NotificationLoadTest.java (using Gatling or JMeter)
@Test
void shouldHandle1000EventsPerSecond() {
    int totalEvents = 10000;
    CountDownLatch latch = new CountDownLatch(totalEvents);

    ExecutorService executor = Executors.newFixedThreadPool(50);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < totalEvents; i++) {
        final int eventId = i;
        executor.submit(() -> {
            try {
                BookCheckedOutEvent event = new BookCheckedOutEvent(
                    (long) eventId, 1L, "Book " + eventId, "patron@example.com", Instant.now()
                );
                kafkaTemplate.send("book-events", event).get();
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(60, TimeUnit.SECONDS);
    long duration = System.currentTimeMillis() - startTime;

    double eventsPerSecond = (totalEvents * 1000.0) / duration;
    System.out.printf("Processed %d events in %d ms (%.2f events/sec)%n",
        totalEvents, duration, eventsPerSecond);

    assertThat(eventsPerSecond).isGreaterThan(1000);
}
```

---

## Step 8: Deploy the Full System

### Docker Compose for Local Development

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres-catalog:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: amigos
      POSTGRES_USER: amigos
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - catalog-data:/var/lib/postgresql/data

  postgres-notifications:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: notifications
      POSTGRES_USER: notifications_user
      POSTGRES_PASSWORD: changeme
    ports:
      - "5433:5432"
    volumes:
      - notification-data:/var/lib/postgresql/data

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  bibby-monolith:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-catalog:5432/amigos
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      - postgres-catalog
      - kafka

  notification-service:
    build: ./notification-service
    ports:
      - "8081:8081"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-notifications:5432/notifications
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SMTP_HOST: mailhog
      SMTP_PORT: 1025
    depends_on:
      - postgres-notifications
      - kafka

  mailhog:
    image: mailhog/mailhog:latest
    ports:
      - "8025:8025"  # Web UI
      - "1025:1025"  # SMTP server

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin

volumes:
  catalog-data:
  notification-data:
```

**Start everything:**

```bash
docker-compose up -d
```

**Verify:**

1. **Bibby monolith:** http://localhost:8080/actuator/health
2. **Notification Service:** http://localhost:8081/actuator/health
3. **MailHog UI:** http://localhost:8025 (see sent emails)
4. **Prometheus:** http://localhost:9090
5. **Grafana:** http://localhost:3000 (admin/admin)

---

## Step 9: Monitor in Production

### Grafana Dashboard for Notification Service

```json
{
  "dashboard": {
    "title": "Notification Service Metrics",
    "panels": [
      {
        "title": "Notifications Sent vs Failed",
        "targets": [
          {
            "expr": "rate(notifications_sent_total[5m])",
            "legendFormat": "Sent"
          },
          {
            "expr": "rate(notifications_failed_total[5m])",
            "legendFormat": "Failed"
          }
        ]
      },
      {
        "title": "Email Send Duration (P95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(notifications_email_send_duration_bucket[5m]))"
          }
        ]
      },
      {
        "title": "Kafka Consumer Lag",
        "targets": [
          {
            "expr": "kafka_consumer_lag{group=\"notification-service\"}"
          }
        ]
      }
    ]
  }
}
```

### Alerting Rules

```yaml
# prometheus-alerts.yml
groups:
- name: notification-service
  rules:
  - alert: HighNotificationFailureRate
    expr: |
      rate(notifications_failed_total[5m]) / rate(notifications_sent_total[5m]) > 0.1
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "Notification failure rate > 10%"
      description: "{{ $value | humanizePercentage }} of notifications are failing"

  - alert: HighKafkaConsumerLag
    expr: kafka_consumer_lag{group="notification-service"} > 1000
    for: 10m
    labels:
      severity: warning
    annotations:
      summary: "Kafka consumer lag is high"
      description: "Notification service is {{ $value }} messages behind"
```

---

## What You've Built

**Congratulations!** You now have a **production-ready microservice system**:

1. **Notification Service** — Independent, scalable, event-driven
2. **Event-driven architecture** — Decoupled via Kafka
3. **Database per service** — Separate PostgreSQL instances
4. **Containerized** — Docker images with multi-stage builds
5. **Orchestrated** — Kubernetes with health checks, HPA, resource limits
6. **Observable** — Prometheus metrics, structured logs, distributed tracing
7. **Resilient** — Retry logic, idempotency, circuit breakers
8. **Tested** — Integration tests, load tests

**You didn't just extract a service. You built infrastructure.**

---

## Next Steps: Extracting More Services

Now that you have the pattern, you can extract:

1. **Catalog Service** (Books, Authors, Shelves, Bookcases)
   - Read-heavy, use CQRS with read replicas
   - ElasticSearch for full-text search
   - Redis for caching

2. **Circulation Service** (Checkouts, Returns, Holds, Fines)
   - Use saga pattern for complex workflows
   - Event sourcing for audit trail
   - Time-based events (overdue reminders)

3. **Analytics Service** (Usage statistics, recommendations)
   - Read from Kafka event stream
   - Time-series database (InfluxDB)
   - Batch processing (Spark)

**Each service follows the same pattern you just learned.**

---

## War Story: The First Microservice

**Company:** Legacy banking system, 300 engineers
**Problem:** Monolithic Java app, 15-year-old codebase, 30-minute builds

**Decision:** Extract "Customer Notification" as first microservice

**Why it worked:**
- **Event-driven:** Listened to account events (deposit, withdrawal, transfer)
- **No shared data:** Only needed customer email, not entire account history
- **Independent team:** 3 developers owned it end-to-end
- **Clear value:** Reduced email delivery time from 2 hours to 30 seconds

**Results (6 months):**
- 99.9% uptime
- 10,000 emails/hour (vs 500/hour in monolith)
- Team autonomy: 5 deployments/week (vs 1 deployment/month for monolith)
- Foundation for 15 more microservices over 2 years

**Key lesson:** Start small, prove the pattern, then scale.

---

## Summary: Your Microservices Playbook

To extract a microservice from a monolith:

1. **Choose wisely:** Start with low-coupling, high-value services
2. **Design the boundary:** Use DDD, identify events, minimize shared data
3. **Build incrementally:** Keep the monolith running, add events first
4. **Deploy properly:** Docker, Kubernetes, health checks, resource limits
5. **Observe everything:** Metrics, logs, traces, alerts
6. **Test thoroughly:** Unit, integration, load tests
7. **Iterate:** Learn from the first service, improve the next

**You now have a working blueprint for building microservices.**

---

## Further Reading

**Books:**
- **"Building Microservices"** by Sam Newman (2021, 2nd Edition) — The bible
- **"Microservices Patterns"** by Chris Richardson (2018) — Saga, CQRS, Event Sourcing
- **"Production-Ready Microservices"** by Susan Fowler (2016) — Operational readiness

**Code:**
- **Spring Cloud examples:** https://spring.io/projects/spring-cloud
- **Microservices Demo by Google:** https://github.com/GoogleCloudPlatform/microservices-demo
- **Eventuate Tram Sagas:** https://github.com/eventuate-tram/eventuate-tram-sagas

---

**Next:** [Section 20: Performance Engineering](20-performance-engineering.md) — Now that it works, let's make it fast.
