# Section 11: Reliability, Resilience & Fault Tolerance

**Part III: Building Microservices**

---

## Production Doesn't Care About Your Feelings

Here's what happened at 2:47 AM on a Tuesday:

**Bibby-Catalog-Service** calls **Bibby-Library-Service** to get shelf locations. Library Service is slow (database connection pool exhausted). Catalog keeps calling. Library can't keep up. Requests queue. Timeout after 30 seconds. But Catalog spawns a new thread for each request. **200 threads.** **500 threads.** **1000 threads.** Memory exhausted. Catalog **crashes**. Now every service that depends on Catalog fails. Cascade failure. **Entire system down.**

**Root cause:** One slow database query brought down 5 services.

In this section, I'll teach you **how to build systems that survive production**. Not theory. **Battle-tested patterns** from companies that learned the hard way.

---

## Definitions: Reliability vs Resilience vs Fault Tolerance

These terms are often conflated. Let's be precise.

### Reliability
**Definition:** The probability that a system will perform correctly over a specified period.

**Measurement:** Uptime percentage, Mean Time Between Failures (MTBF).

**Example:** "Bibby's search API has 99.9% uptime" means it's down ~43 minutes/month.

### Resilience
**Definition:** The ability to recover quickly from failures.

**Measurement:** Mean Time To Recovery (MTTR).

**Example:** "When Bibby's database fails, the system switches to read replica in 5 seconds."

### Fault Tolerance
**Definition:** The ability to continue operating (possibly degraded) despite failures.

**Measurement:** Partial availability during failures.

**Example:** "When Library Service is down, Bibby still allows book searches but shows 'Location Unknown'."

**Mental model:**
- **Reliability:** How often do you fail?
- **Resilience:** How fast do you recover?
- **Fault tolerance:** Can you keep working while broken?

**All three matter.** Amazon's 2021 AWS outage was high fault tolerance (degraded but working), poor reliability (outage shouldn't have happened), moderate resilience (recovery took 3 hours).

---

## Bibby's Current State: A Reliability Audit

Let's examine what would happen under load or failures.

### Problem 1: No Global Error Handling

From `BookcaseService.java:18,28-29`:
```java
private final ResponseStatusException existingRecordError =
    new ResponseStatusException(HttpStatus.CONFLICT, "Bookcase with the label already exist");

public String createNewBookCase(String label, int capacity) {
    BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);
    if (bookcaseEntity != null) {
        log.error("Failed to save Record - Record already exist", existingRecordError);
        throw existingRecordError;  // ← Thrown but not caught globally
    }
    // ...
}
```

**What happens:**
- In a CLI app, user sees a stack trace
- In a REST API (if Bibby becomes one), client gets 500 Internal Server Error with no helpful message

**Missing:** `@ControllerAdvice` to handle exceptions globally.

### Problem 2: No Retry Logic

From `BookService.java:43-54`:
```java
public BookEntity findBookByTitle(String title) {
    Optional<BookEntity> bookEntity = Optional.ofNullable(
        bookRepository.findByTitleIgnoreCase(title)
    );  // ← Database query, no retry if DB connection fails

    if (bookEntity.isEmpty()) {
        return null;
    }
    return bookEntity.get();
}
```

**Failure scenario:** Database hiccups (network blip, connection pool temporary exhaustion). Query fails. **User gets "Book Not Found" even though book exists.**

**Solution:** Retry transient failures.

### Problem 3: Unbounded Concurrency

Bibby is single-threaded (CLI), but imagine it as a web service:

```java
// Hypothetical REST endpoint
@GetMapping("/api/v1/books/search")
public BookSearchResult searchByTitle(@RequestParam String title) throws InterruptedException {
    Thread.sleep(1000);  // Simulate UX delay from BookCommands.java:118
    BookEntity book = bookService.findBookByTitle(title);
    Thread.sleep(2000);  // More delays (line 136)
    // Each request holds a thread for 3+ seconds
}
```

**Under load (100 concurrent users):**
- Tomcat default thread pool: **200 threads**
- Each request takes 3+ seconds
- Throughput: **~66 requests/second**
- Request 201 **queues** (thread pool exhausted)
- Users wait 30+ seconds → timeout → retry → **death spiral**

**Missing:** Bulkheads, rate limiting, backpressure.

---

## Pattern 1: Bulkheads (Failure Isolation)

**Analogy:** Ships have bulkheads (watertight compartments). If one floods, others stay dry.

**In software:** Isolate resources so one failure doesn't cascade.

### Thread Pool Isolation

**Problem:** Single shared thread pool for all operations.

```java
// ❌ All operations share Tomcat's thread pool
@GetMapping("/search")   // Cheap, fast operation
@GetMapping("/export")   // Expensive, slow operation (generates PDF)
// Both compete for same 200 threads
```

**If `/export` gets hit with 200 concurrent requests:**
- All threads busy generating PDFs
- `/search` requests queue
- **Fast operations blocked by slow operations**

**Solution:** Separate thread pools (bulkheads).

```java
@Configuration
public class ThreadPoolConfig {

    @Bean(name = "searchExecutor")
    public Executor searchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("search-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "exportExecutor")
    public Executor exportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);   // Smaller pool for expensive ops
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("export-");
        executor.initialize();
        return executor;
    }
}
```

**Use bulkheads:**
```java
@Async("searchExecutor")  // ← Uses search thread pool
public CompletableFuture<BookSearchResult> searchByTitle(String title) {
    BookEntity book = bookRepository.findByTitle(title);
    return CompletableFuture.completedFuture(new BookSearchResult(book));
}

@Async("exportExecutor")  // ← Uses export thread pool
public CompletableFuture<byte[]> exportBooks() {
    // Expensive PDF generation
    return CompletableFuture.completedFuture(pdfBytes);
}
```

**Now:** Even if export pool is exhausted, search still works.

### Connection Pool Isolation

**Problem:** Bibby's single database connection pool (from `application.properties:3-5`).

```properties
spring.datasource.url=jdbc:postgresql://localhost:5332/amigos
# Default HikariCP pool: 10 connections
```

**If one slow query consumes all 10 connections:**
- All other queries wait
- Entire application grinds to a halt

**Solution:** Separate pools for critical vs non-critical operations.

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.critical")
    public DataSource criticalDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:postgresql://localhost:5332/amigos")
            .maximumPoolSize(15)  // Higher priority ops get more connections
            .build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.analytics")
    public DataSource analyticsDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:postgresql://localhost:5332/amigos")
            .maximumPoolSize(5)   // Lower priority ops get fewer
            .build();
    }
}
```

**Example usage:**
```java
// Critical: User-facing book checkout
@Transactional(transactionManager = "criticalTransactionManager")
public void checkOutBook(Long bookId) { ... }

// Non-critical: Analytics report
@Transactional(transactionManager = "analyticsTransactionManager")
public MonthlyReport generateReport() { ... }
```

**Result:** Analytics reports can't starve checkout operations.

---

## Pattern 2: Rate Limiting

**Problem:** Bibby has no protection against abuse.

**Scenario:** Malicious user sends 10,000 search requests/second. Database overwhelmed. Legitimate users can't search.

### Token Bucket Algorithm

**Concept:** Bucket holds N tokens. Each request consumes 1 token. Tokens refill at fixed rate.

**Implementation (using Guava):**

```java
@Component
public class RateLimiter {

    // Allow 100 requests/second per user
    private final LoadingCache<String, com.google.common.util.concurrent.RateLimiter> limiters =
        CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<>() {
                @Override
                public com.google.common.util.concurrent.RateLimiter load(String key) {
                    return com.google.common.util.concurrent.RateLimiter.create(100.0);
                }
            });

    public boolean allowRequest(String userId) {
        try {
            com.google.common.util.concurrent.RateLimiter limiter = limiters.get(userId);
            return limiter.tryAcquire();  // Returns false if rate exceeded
        } catch (ExecutionException e) {
            return true;  // Fail open (allow request if limiter fails)
        }
    }
}
```

**Controller usage:**
```java
@RestController
public class BookController {

    @Autowired
    private RateLimiter rateLimiter;

    @GetMapping("/api/v1/books/search")
    public ResponseEntity<BookSearchResult> searchByTitle(
        @RequestParam String title,
        @RequestHeader("X-User-ID") String userId
    ) {
        if (!rateLimiter.allowRequest(userId)) {
            return ResponseEntity.status(429)  // Too Many Requests
                .header("Retry-After", "1")
                .body(new ErrorResponse("Rate limit exceeded. Try again in 1 second."));
        }

        BookSearchResult result = bookService.searchByTitle(title);
        return ResponseEntity.ok(result);
    }
}
```

**Token bucket visualization:**

```
Bucket capacity: 100 tokens
Refill rate: 100 tokens/second

Time 0s:   [100 tokens] ← Full
Request 1: [99 tokens]
Request 2: [98 tokens]
...
Request 100: [0 tokens]
Request 101: ❌ REJECTED (429 Too Many Requests)

Time 0.01s: [1 token] ← Refilled
Request 102: [0 tokens] ✅ Allowed
```

**Advanced:** Distributed rate limiting with Redis.

```java
@Component
public class RedisRateLimiter {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean allowRequest(String userId, int maxRequests, int windowSeconds) {
        String key = "rate_limit:" + userId;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        return count <= maxRequests;
    }
}
```

**This works across multiple Catalog Service instances** (Redis is shared).

---

## Pattern 3: Backpressure

**Problem:** Producer (Kafka events) is faster than consumer (email service). Messages pile up. Consumer crashes (out of memory).

**Solutions:**

### 1. Reactive Streams (Project Reactor)

```java
@Service
public class EmailService {

    public Flux<EmailResult> sendCheckoutEmails(Flux<BookCheckedOutEvent> events) {
        return events
            .buffer(Duration.ofSeconds(5))  // Batch events every 5 seconds
            .flatMap(batch -> sendBatch(batch), 2)  // Max 2 concurrent batches
            .onBackpressureBuffer(1000)  // Buffer up to 1000 items
            .onBackpressureDrop(event -> log.warn("Dropped event: {}", event));  // Drop if buffer full
    }

    private Mono<EmailResult> sendBatch(List<BookCheckedOutEvent> events) {
        // Send batch email
        return Mono.fromCallable(() -> smtpClient.sendBatch(events));
    }
}
```

**Backpressure strategies:**
- **Buffer:** Queue up to N items, then apply strategy
- **Drop:** Discard new events when buffer full
- **Latest:** Keep only most recent N events
- **Error:** Throw exception (let upstream handle)

### 2. Kafka Consumer Backpressure

```java
@KafkaListener(
    topics = "book-checkout-events",
    concurrency = "3"  // Max 3 concurrent consumer threads
)
public void handleCheckout(BookCheckedOutEvent event) {
    // Process event
    emailService.send(event);

    // If processing is slow, Kafka will pause this consumer partition
    // until it catches up (automatic backpressure)
}
```

**Kafka configuration:**
```properties
# Fetch max 10 records per poll (prevents overwhelming consumer)
spring.kafka.consumer.max-poll-records=10

# If consumer can't process in 5 minutes, Kafka rebalances
spring.kafka.consumer.max-poll-interval-ms=300000
```

---

## Pattern 4: Graceful Degradation

**Philosophy:** Better to serve partial results than fail completely.

### Bibby Example: Search with Unavailable Location Service

From `BookCommands.java:333-364`, the search flow requires Library Service for location.

**Brittle approach:**
```java
// ❌ All-or-nothing
public BookSearchResult searchByTitle(String title) {
    Book book = catalogService.findBook(title);

    LocationInfo location = libraryClient.getLocation(book.getShelfId());
    // If Library Service is down, throws exception → user gets error

    return new BookSearchResult(book, location);
}
```

**Graceful degradation:**
```java
// ✅ Degrade to partial results
public BookSearchResult searchByTitle(String title) {
    Book book = catalogService.findBook(title);

    try {
        LocationInfo location = libraryClient.getLocation(book.getShelfId());
        return new BookSearchResult(book, location, ServiceStatus.FULL);

    } catch (ServiceUnavailableException e) {
        log.warn("Library Service unavailable, returning partial results", e);
        return new BookSearchResult(
            book,
            new LocationInfo("UNKNOWN", "UNKNOWN"),
            ServiceStatus.DEGRADED
        );
    }
}
```

**API response:**
```json
{
  "book": {
    "id": 42,
    "title": "The Pragmatic Programmer",
    "authors": ["David Thomas", "Andrew Hunt"]
  },
  "location": {
    "bookcase": "UNKNOWN",
    "shelf": "UNKNOWN"
  },
  "serviceStatus": "DEGRADED",
  "message": "Book found, but location service is temporarily unavailable."
}
```

**User sees:** "Book found! (Location info unavailable right now)" instead of "Error 500".

### Fallback Strategies

**1. Cached data:**
```java
@Cacheable(value = "locations", key = "#shelfId")
public LocationInfo getLocation(Long shelfId) {
    return libraryClient.getLocation(shelfId);
}

// If Library Service is down, return cached location (may be stale)
```

**2. Default values:**
```java
public BookRecommendations getRecommendations(Long bookId) {
    try {
        return mlService.getRecommendations(bookId);
    } catch (Exception e) {
        // ML service down? Return popular books
        return BookRecommendations.fromPopular();
    }
}
```

**3. Static content:**
```java
public List<Genre> getGenres() {
    try {
        return genreService.getAll();
    } catch (Exception e) {
        // Serve hardcoded list
        return List.of(
            new Genre("Fiction"),
            new Genre("Non-Fiction"),
            new Genre("Science")
        );
    }
}
```

---

## Pattern 5: Health Checks & Readiness Probes

**Problem:** Load balancer sends traffic to crashed instances.

**Spring Boot Actuator:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Health endpoint:** `GET /actuator/health`

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
        "free": 500000000,
        "threshold": 10485760
      }
    }
  }
}
```

**Custom health check:**
```java
@Component
public class LibraryServiceHealthIndicator implements HealthIndicator {

    @Autowired
    private LibraryServiceClient libraryClient;

    @Override
    public Health health() {
        try {
            libraryClient.ping();  // Simple health check endpoint
            return Health.up()
                .withDetail("libraryService", "reachable")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("libraryService", "unreachable")
                .withException(e)
                .build();
        }
    }
}
```

**Kubernetes liveness vs readiness:**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: bibby-catalog-service
spec:
  containers:
  - name: catalog
    image: bibby-catalog:1.0
    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10
      failureThreshold: 3  # Restart pod after 3 failures

    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 8080
      initialDelaySeconds: 10
      periodSeconds: 5
      failureThreshold: 2  # Remove from load balancer after 2 failures
```

**Liveness:** "Is the process alive?" (crash → restart)
**Readiness:** "Is the process ready to serve traffic?" (dependencies down → remove from LB)

---

## Pattern 6: Graceful Shutdown

**Problem:** Kubernetes sends SIGTERM, app exits immediately, **in-flight requests fail**.

**Default (bad):**
```
t=0s:   Kubernetes sends SIGTERM
t=0s:   App exits
t=0s:   50 in-flight requests → Connection refused
```

**Graceful shutdown:**
```java
@Configuration
public class GracefulShutdownConfig {

    @Bean
    public GracefulShutdownTomcat gracefulShutdownTomcat() {
        return new GracefulShutdownTomcat();
    }

    private static class GracefulShutdownTomcat
        implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {

        private volatile Connector connector;

        @Override
        public void customize(Connector connector) {
            this.connector = connector;
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            this.connector.pause();  // Stop accepting new requests

            Executor executor = this.connector.getProtocolHandler().getExecutor();
            if (executor instanceof ThreadPoolExecutor) {
                try {
                    ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                    threadPoolExecutor.shutdown();

                    // Wait up to 30 seconds for in-flight requests to finish
                    if (!threadPoolExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        log.warn("Tomcat thread pool did not shut down gracefully within 30 seconds");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
```

**Timeline with graceful shutdown:**
```
t=0s:   Kubernetes sends SIGTERM
t=0s:   App stops accepting new requests
t=0s:   50 in-flight requests continue processing
t=5s:   45 requests finished
t=10s:  All 50 requests finished
t=10s:  App exits cleanly
```

**Spring Boot 2.3+ makes this easier:**
```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

---

## SLAs, SLOs, SLIs

### Definitions

**SLA (Service Level Agreement):** Contract with customer about uptime/performance.
- Example: "Bibby guarantees 99.9% uptime. If we fail, you get 10% credit."

**SLO (Service Level Objective):** Internal target to meet SLA.
- Example: "Catalog Service targets 99.95% uptime" (buffer above 99.9% SLA)

**SLI (Service Level Indicator):** Actual measurement.
- Example: "Last month: 99.97% uptime" (met SLO)

### Calculating Uptime

**Formula:** `Uptime % = (Total Time - Downtime) / Total Time × 100`

**"Nines" table:**

| Uptime % | Downtime/year | Downtime/month | Downtime/week |
|----------|---------------|----------------|---------------|
| 90% ("one nine") | 36.5 days | 72 hours | 16.8 hours |
| 99% ("two nines") | 3.65 days | 7.2 hours | 1.68 hours |
| 99.9% ("three nines") | 8.76 hours | 43.2 minutes | 10.1 minutes |
| 99.99% ("four nines") | 52.6 minutes | 4.32 minutes | 1.01 minutes |
| 99.999% ("five nines") | 5.26 minutes | 25.9 seconds | 6.05 seconds |

**Reality check:** Google Cloud's SLA is 99.95% for most services. Amazon S3 is 99.99%. **Five nines is extremely expensive.**

### Setting Realistic SLOs

**For Bibby (hypothetical microservices):**

```
Catalog Service (critical path):
  SLO: 99.95% uptime
  SLI: Percentage of successful requests (HTTP 200-299)
  Latency SLO: p99 < 500ms

Library Service (non-critical):
  SLO: 99.5% uptime
  SLI: Percentage of successful requests
  Latency SLO: p99 < 1000ms

Email Service (async, non-critical):
  SLO: 99% delivery within 5 minutes
  SLI: Percentage of emails sent within 5 min
  No uptime SLO (emails can queue)
```

**Error budgets:**

If SLO is 99.9%, you have **0.1% error budget** = 43.2 min/month.

**If you burn through error budget:**
- Stop feature releases
- Focus on stability
- No risky deploys

**Google SRE mantra:** "Error budgets align incentives between devs (ship features) and ops (keep things stable)."

---

## Chaos Engineering

**Definition:** Deliberately breaking production to find weaknesses before they find you.

**Netflix Chaos Monkey (2011):** Randomly terminates EC2 instances during business hours.

**Result:** Engineers design for instance failure by default. When AWS has outages, Netflix stays up.

### Chaos Experiments for Bibby

**1. Kill random service instances**
```bash
# Kubernetes ChaosToolkit
kubectl delete pod -l app=bibby-catalog-service --random
```

**Expected:** Load balancer routes to healthy instances. No downtime.
**Actual:** If circuit breakers aren't configured, cascading failures.

**2. Inject network latency**
```yaml
# Simulate slow Library Service
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: library-latency
spec:
  action: delay
  mode: all
  selector:
    labelSelectors:
      app: bibby-library-service
  delay:
    latency: "500ms"
  duration: "5m"
```

**Expected:** Catalog Service timeouts work. Fallback to cached locations.
**Actual:** Without timeouts, requests hang for 30 seconds.

**3. Exhaust database connections**
```sql
-- Run 20 slow queries simultaneously
SELECT pg_sleep(60) FROM books;
```

**Expected:** Connection pool queues requests. Timeouts trigger.
**Actual:** All requests blocked. Health check fails. Pod restarted (but problem persists).

**Fix:** Connection pool monitoring, slow query alerts.

---

## Distributed Tracing

**Problem:** User reports "Search is slow." Which service is the bottleneck?

**Without tracing:**
```
Catalog → ??? → ??? → Response (2000ms)
```

**With tracing (OpenTelemetry/Jaeger):**

```
Catalog Service (150ms)
  ↓
  ├─ Database: findByTitle (50ms)
  ├─ Library Service (1800ms) ← BOTTLENECK
  │   ├─ Database: findShelfById (1750ms) ← Slow query!
  │   └─ Database: findBookcaseById (50ms)
  └─ Cache: updateCache (100ms)
```

**Implementation:**

```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
```

**Automatic instrumentation:**
```java
// No code changes needed! Spring Boot auto-configures tracing
@RestController
public class BookController {
    @GetMapping("/search")
    public BookSearchResult search(@RequestParam String title) {
        // This method is automatically traced
        return bookService.search(title);
    }
}
```

**Trace propagation:**
```
Request ID: abc-123

[Catalog Service]
  Span ID: span-1
  Headers: traceparent: 00-abc123-span1-01

    HTTP GET → Library Service
    Headers: traceparent: 00-abc123-span1-01  ← Propagated

    [Library Service]
      Parent Span: span-1
      Span ID: span-2
      Duration: 1800ms
```

**Jaeger UI shows:**
- Visual timeline of all spans
- Which service is slow
- Database queries executed
- Cache hit/miss rates

**Real-world:** Uber uses Jaeger to trace 1 trillion requests/day.

---

## Anti-Patterns to Avoid

### 1. Retry Storms

**Symptom:** Service is slow. Clients retry. More load. Service gets slower. **Death spiral.**

```java
// ❌ Retry immediately without backoff
for (int i = 0; i < 3; i++) {
    try {
        return libraryClient.getLocation(shelfId);
    } catch (Exception e) {
        // Retry immediately → Makes problem worse
    }
}
```

**Fix:** Exponential backoff + jitter (see Section 10).

### 2. Ignore Health Checks

**Symptom:** Service returns `200 OK` for health check, but database is down.

```java
// ❌ Useless health check
@GetMapping("/health")
public String health() {
    return "OK";  // Always returns OK, even if database is unreachable
}
```

**Fix:** Check dependencies.

### 3. No Timeout Budget

**Symptom:** Timeouts set arbitrarily ("2 seconds sounds good").

**Problem:** If Catalog calls Library (2s timeout) which calls Circulation (2s timeout), **total timeout is 4s**. Not 2s!

**Fix:** Timeout budget.
```
User-facing SLO: 3 seconds max
  Catalog Service: 2.5s timeout
    Library Service: 2s timeout
      Database: 1.5s timeout
```

---

## Real-World War Story: GitHub's 2018 Outage

**Incident:** Database cluster split due to network partition. Two primaries accepted writes. Data diverged.

**Recovery:** Took 24 hours to manually reconcile data.

**Root cause:** No automated split-brain detection. Failover scripts assumed network was reliable.

**Lesson:** "Reliable networks" don't exist. Design for partitions (CAP theorem).

**How they fixed it:**
1. Added consensus system (Raft) to elect single primary
2. Circuit breakers on database connections
3. Chaos testing for network partitions

---

## Action Items

**For Bibby:**

1. **Add global exception handler** (30 min)
   ```java
   @ControllerAdvice
   public class GlobalExceptionHandler {
       @ExceptionHandler(ResponseStatusException.class)
       public ResponseEntity<ErrorResponse> handle(ResponseStatusException e) {
           return ResponseEntity.status(e.getStatus())
               .body(new ErrorResponse(e.getReason()));
       }
   }
   ```

2. **Add health checks** (15 min)
   - Add `spring-boot-starter-actuator` dependency
   - Configure `/actuator/health` endpoint
   - Test `curl http://localhost:8080/actuator/health`

3. **Implement rate limiting** (1 hour)
   - Add Guava dependency
   - Create `RateLimiter` bean
   - Test with load tool (ApacheBench, wrk)

4. **Add retry logic** (30 min)
   - Add `@Retryable` to `findBookByTitle()`
   - Simulate database failure (stop PostgreSQL)
   - Verify retries happen

**For your project:**

1. **Audit timeouts** - Do all HTTP clients have timeouts?
2. **Define SLOs** - What uptime do you promise? Measure it.
3. **Add tracing** - Install OpenTelemetry, visualize slow paths
4. **Run chaos experiment** - Kill one pod. Does traffic shift gracefully?

---

## Further Reading

- **"Site Reliability Engineering"** by Google SRE team (free online)
- **"Release It!"** by Michael Nygard (resilience patterns bible)
- **Chaos Monkey:** https://netflix.github.io/chaosmonkey/
- **OpenTelemetry:** https://opentelemetry.io/
- **Resilience4j:** https://resilience4j.readme.io/

---

## Next Section Preview

**Section 12: Containers & Docker Fundamentals** will teach you:
- Containerization basics and why it matters
- Writing Dockerfiles for Java/Spring Boot apps
- Multi-stage builds for smaller images
- Docker Compose for local development
- Container registries and image management
- Security best practices (non-root users, scanning)

We'll containerize Bibby and run it locally with PostgreSQL in Docker.

Ready? Let's package your code for production.

---

**Word count:** ~4,600 words
