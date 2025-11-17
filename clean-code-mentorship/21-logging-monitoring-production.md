# Section 21: Logging, Monitoring & Production Readiness
## Clean Code + Spring Framework Mentorship

**Focus:** Professional logging, observability, health checks, and production deployment preparation

**Estimated Time:** 2-3 hours to read and understand; 8-10 hours to implement

---

## Overview

Your current application has **ALL logging turned OFF**. In production, this means:
- You can't debug issues
- No audit trail
- No performance metrics
- No way to know if the app is healthy

This section will transform your application from "it works on my machine" to **production-ready**.

---

## Your Current Logging Situation

**application.properties:**
```properties
# Turn off Spring Boot startup info
spring.main.banner-mode=console
logging.level.root=OFF          # ❌ Can't see anything
logging.level.org.springframework=OFF
logging.level.org.hibernate=OFF
```

**In Code:**
```java
// BookService.java
public BookEntity findBookByTitle(String title){
    List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
    for(BookEntity b : bookEntities){
        System.out.println(b.getTitle());  // ❌ Debug code in production
    }
    // ...
}

// BookcaseCommands.java
System.out.println(">>> Bibby is awake. Seeding data now...");  // ❌ System.out
```

**Problems:**
1. No visibility into production issues
2. `System.out.println` scattered everywhere
3. Can't distinguish dev/test/prod environments
4. No structured logging
5. No log aggregation possible

---

## Professional Logging

### SLF4J (Simple Logging Facade for Java)

**Why SLF4J?**
- Industry standard
- Framework-agnostic
- Supports placeholders (no string concatenation)
- Lazy evaluation (better performance)

**Add to Your Services:**

**BookService.java:**
```java
package com.penrose.bibby.library.book;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    @Transactional(readOnly = true)
    public BookEntity findById(Long id) {
        log.debug("Finding book by id: {}", id);

        BookEntity book = bookRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Book not found with id: {}", id);
                return new ResourceNotFoundException("Book not found with id: " + id);
            });

        log.debug("Found book: id={}, title={}", book.getBookId(), book.getTitle());
        return book;
    }

    @Transactional
    public BookEntity createBook(BookRequestDTO request) {
        log.info("Creating book: title={}, author={} {}",
            request.title(), request.firstName(), request.lastName());

        try {
            // Business logic...
            BookEntity savedBook = bookRepository.save(book);

            log.info("Book created successfully: id={}, title={}",
                savedBook.getBookId(), savedBook.getTitle());

            return savedBook;
        } catch (Exception e) {
            log.error("Failed to create book: title={}", request.title(), e);
            throw e;
        }
    }

    @Transactional
    public void checkOutBook(Long bookId) {
        log.info("Checking out book: id={}", bookId);

        BookEntity book = findById(bookId);

        if (book.getStatus() == BookStatus.CHECKED_OUT) {
            log.warn("Attempted to check out already checked-out book: id={}", bookId);
            throw new BusinessException("Book is already checked out");
        }

        book.checkOut();
        log.info("Book checked out successfully: id={}, checkoutCount={}",
            bookId, book.getCheckoutCount());
    }
}
```

**Log Levels (from most to least verbose):**

| Level | When to Use | Example |
|-------|-------------|---------|
| **TRACE** | Very detailed debugging | "Entering method X with params Y" |
| **DEBUG** | Detailed flow information | "Found book: id=123, title=Clean Code" |
| **INFO** | Important business events | "Book created successfully: id=123" |
| **WARN** | Potentially harmful situations | "Book not found with id: 999" |
| **ERROR** | Error events that allow app to continue | "Failed to create book" |

**Best Practices:**

✅ **DO:**
```java
log.info("User {} logged in", username);  // Placeholder
log.debug("Processing {} items", items.size());
log.error("Failed to save book: {}", book.getTitle(), exception);  // Include exception
```

❌ **DON'T:**
```java
log.info("User " + username + " logged in");  // String concatenation
System.out.println("Debug: " + data);  // System.out
log.debug(book.toString());  // Unnecessary toString()
log.info("Processing started");  // Too vague
```

---

## Logging Configuration

### Development Profile

**application-dev.properties:**
```properties
# Development: Verbose logging for debugging
logging.level.root=INFO
logging.level.com.penrose.bibby=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Show SQL with parameters
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Console output with colors
spring.output.ansi.enabled=ALWAYS
logging.pattern.console=%d{HH:mm:ss.SSS} %clr(%-5level) %clr([%15.15thread]){faint} %clr(%-40.40logger{39}){cyan} : %msg%n
```

### Production Profile

**application-prod.properties:**
```properties
# Production: Minimal console, comprehensive file logging
logging.level.root=WARN
logging.level.com.penrose.bibby=INFO

# Log to file
logging.file.name=/var/log/bibby/application.log

# Rolling file appender (10MB per file, keep 30 days)
logging.logback.rollingpolicy.max-file-size=10MB
logging.logback.rollingpolicy.max-history=30
logging.logback.rollingpolicy.total-size-cap=1GB

# Structured logging pattern
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

# Don't show SQL in production
spring.jpa.show-sql=false
```

### Custom Logback Configuration (Optional)

Create `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/bibby.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/bibby.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy
                class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>10MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- JSON Logging for Production -->
    <appender name="JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/bibby-json.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>requestId</includeMdcKeyName>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/bibby-json.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- Profile-specific configuration -->
    <springProfile name="dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
        </root>
        <logger name="com.penrose.bibby" level="DEBUG"/>
    </springProfile>

    <springProfile name="prod">
        <root level="WARN">
            <appender-ref ref="FILE"/>
            <appender-ref ref="JSON"/>
        </root>
        <logger name="com.penrose.bibby" level="INFO"/>
    </springProfile>
</configuration>
```

**Add JSON logging dependency:**
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

---

## Spring Boot Actuator

Actuator provides production-ready features: health checks, metrics, info endpoints.

**Add Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Configure Actuator:**

**application.properties:**
```properties
# Enable actuator endpoints
management.endpoints.web.exposure.include=health,info,metrics,loggers,env
management.endpoint.health.show-details=when-authorized

# Custom application info
info.app.name=Bibby
info.app.description=Personal Library Management System
info.app.version=@project.version@

# Health check details
management.health.db.enabled=true
management.health.diskspace.enabled=true
```

**Access Endpoints:**
```bash
# Health check
curl http://localhost:8080/actuator/health

# Application info
curl http://localhost:8080/actuator/info

# Metrics
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# View loggers
curl http://localhost:8080/actuator/loggers
```

### Custom Health Indicator

Create a custom health check:

```java
package com.penrose.bibby.health;

import com.penrose.bibby.library.book.BookRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final BookRepository bookRepository;

    public DatabaseHealthIndicator(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public Health health() {
        try {
            long bookCount = bookRepository.count();
            return Health.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("bookCount", bookCount)
                .withDetail("status", "Connected")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

**Response:**
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
    "database": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "bookCount": 127,
        "status": "Connected"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500107862016,
        "free": 250107862016,
        "threshold": 10485760
      }
    }
  }
}
```

---

## Metrics and Monitoring

### Micrometer Metrics

Spring Boot Actuator includes Micrometer for metrics.

**Common Metrics:**
```bash
# JVM memory
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP requests
curl http://localhost:8080/actuator/metrics/http.server.requests

# Database connections
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### Custom Metrics

**Add custom business metrics:**

```java
package com.penrose.bibby.library.book;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;
    private final Counter bookCreatedCounter;
    private final Counter bookCheckedOutCounter;
    private final Timer bookSearchTimer;

    public BookService(
        BookRepository bookRepository,
        MeterRegistry meterRegistry
    ) {
        this.bookRepository = bookRepository;

        // Custom counters
        this.bookCreatedCounter = Counter.builder("bibby.books.created")
            .description("Total number of books created")
            .register(meterRegistry);

        this.bookCheckedOutCounter = Counter.builder("bibby.books.checkedout")
            .description("Total number of books checked out")
            .register(meterRegistry);

        // Custom timer
        this.bookSearchTimer = Timer.builder("bibby.books.search")
            .description("Time taken to search for books")
            .register(meterRegistry);
    }

    @Transactional
    public BookEntity createBook(BookRequestDTO request) {
        BookEntity book = // ... creation logic
        bookRepository.save(book);

        bookCreatedCounter.increment();  // ✅ Increment custom metric
        log.info("Book created: id={}", book.getBookId());

        return book;
    }

    @Transactional
    public void checkOutBook(Long bookId) {
        // ... checkout logic

        bookCheckedOutCounter.increment();  // ✅ Increment custom metric
        log.info("Book checked out: id={}", bookId);
    }

    public List<BookEntity> searchBooks(String query) {
        return bookSearchTimer.record(() -> {  // ✅ Time the operation
            log.debug("Searching for books: query={}", query);
            return bookRepository.findByTitleContainingIgnoreCase(query);
        });
    }
}
```

**View custom metrics:**
```bash
curl http://localhost:8080/actuator/metrics/bibby.books.created
curl http://localhost:8080/actuator/metrics/bibby.books.checkedout
curl http://localhost:8080/actuator/metrics/bibby.books.search
```

---

## Production Readiness Checklist

### 1. Configuration Management

✅ **Externalize Secrets:**
```properties
# ❌ Don't hardcode
spring.datasource.password=password

# ✅ Use environment variables
spring.datasource.password=${DB_PASSWORD}
```

✅ **Use Profiles:**
```bash
# Development
java -jar bibby.jar --spring.profiles.active=dev

# Production
java -jar bibby.jar --spring.profiles.active=prod
```

### 2. Error Handling

✅ **Hide Stack Traces in Production:**
```properties
# application-prod.properties
server.error.include-stacktrace=never
server.error.include-message=always
server.error.include-binding-errors=never
```

✅ **Global Exception Handler:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unexpected error occurred", ex);  // ✅ Log full stack trace

        // Return sanitized error to client
        ErrorResponse response = new ErrorResponse(
            500,
            "An unexpected error occurred",  // Don't leak internal details
            null,
            Instant.now()
        );

        return ResponseEntity.internalServerError().body(response);
    }
}
```

### 3. Security Headers

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'"))
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
            );
        return http.build();
    }
}
```

### 4. Database Connection Pooling

**application.properties:**
```properties
# HikariCP (default in Spring Boot)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# Health check
spring.datasource.hikari.connection-test-query=SELECT 1
```

### 5. Graceful Shutdown

```properties
# Allow in-flight requests to complete
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

### 6. Request/Response Logging

**Create Logging Filter:**
```java
@Component
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        long startTime = System.currentTimeMillis();

        log.info("Incoming request: method={}, uri={}", req.getMethod(), req.getRequestURI());

        chain.doFilter(request, response);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Completed request: method={}, uri={}, status={}, duration={}ms",
            req.getMethod(), req.getRequestURI(), res.getStatus(), duration);
    }
}
```

---

## Observability Best Practices

### Structured Logging

Use MDC (Mapped Diagnostic Context) for request tracing:

```java
@Component
public class RequestIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

**Logging pattern with requestId:**
```properties
logging.pattern.console=%d{HH:mm:ss.SSS} [%X{requestId}] %-5level %logger{36} - %msg%n
```

**Output:**
```
10:23:45.123 [a1b2c3d4-e5f6-7890-abcd-ef1234567890] INFO  BookService - Creating book: title=Clean Code
10:23:45.456 [a1b2c3d4-e5f6-7890-abcd-ef1234567890] INFO  BookService - Book created: id=123
```

### Log Correlation Across Services

```java
@RestControllerAdvice
public class LoggingInterceptor {

    @ModelAttribute
    public void logRequest(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
    }
}
```

---

## Action Items

### 🚨 Critical (Do Immediately - 2-3 hours)

1. **Enable Proper Logging**
   - [ ] Remove `logging.level.root=OFF`
   - [ ] Add SLF4J logger to all service classes
   - [ ] Replace all `System.out.println` with proper logging
   - **Files:** application.properties, all service classes

2. **Add Logging Profiles**
   - [ ] Create `application-dev.properties` with DEBUG logging
   - [ ] Create `application-prod.properties` with INFO logging
   - [ ] Configure file logging for production

3. **Add Spring Boot Actuator**
   - [ ] Add actuator dependency
   - [ ] Expose health and metrics endpoints
   - [ ] Test `/actuator/health`

### 🔶 High Priority (This Week - 4-5 hours)

4. **Create Custom Health Checks**
   - [ ] Create `DatabaseHealthIndicator`
   - [ ] Add health check for database connectivity
   - [ ] Verify health endpoint returns detailed status

5. **Add Custom Metrics**
   - [ ] Add counter for books created
   - [ ] Add counter for checkouts
   - [ ] Add timer for search operations

6. **Configure Production Logging**
   - [ ] Setup rolling file appender
   - [ ] Configure log retention (30 days)
   - [ ] Add structured JSON logging

### 🔷 Medium Priority (This Month - 3-4 hours)

7. **Add Request Logging**
   - [ ] Create request/response logging filter
   - [ ] Add request ID to MDC
   - [ ] Log request duration

8. **Production Readiness**
   - [ ] Externalize database credentials
   - [ ] Hide stack traces in prod
   - [ ] Configure graceful shutdown
   - [ ] Setup connection pooling

9. **Monitoring Dashboard**
   - [ ] Add Prometheus endpoint (optional)
   - [ ] Configure Grafana dashboards (optional)
   - [ ] Setup alerts for critical metrics (optional)

---

## Summary

### Your Current State
- ❌ All logging disabled
- ❌ System.out.println everywhere
- ❌ No health checks
- ❌ No metrics
- ❌ No production monitoring

### After This Section
- ✅ Professional SLF4J logging
- ✅ Profile-based configuration
- ✅ Health checks with Actuator
- ✅ Custom business metrics
- ✅ Request tracing
- ✅ Production-ready observability

---

## Resources

### Official Docs
- [Spring Boot Logging](https://docs.spring.io/spring-boot/reference/features/logging.html)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/)
- [Micrometer Documentation](https://micrometer.io/docs)

### Tools
- **Logback:** Default logging framework
- **SLF4J:** Logging facade
- **Micrometer:** Metrics collection
- **Prometheus:** Metrics storage (optional)
- **Grafana:** Metrics visualization (optional)

---

## Mentor's Note

Leo, having **ALL logging turned off** is like flying blind. In production, when something breaks (and it will), you'll have ZERO information to debug it.

**Priority:**
1. Enable logging TODAY (30 minutes)
2. Add SLF4J to services (2 hours)
3. Add Actuator (30 minutes)
4. Everything else (4-5 hours)

With proper logging and monitoring, you'll be able to:
- Debug production issues
- Track performance
- Prove the app is healthy
- Audit user actions

This is **non-negotiable** for any production application.

---

**Next Section:** Build & Deployment

**Last Updated:** 2025-11-17
**Status:** Complete ✅
