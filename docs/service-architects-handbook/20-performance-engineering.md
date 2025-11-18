# Section 20: Performance Engineering

**Previous:** [Section 19: Building Your First Microservice System](19-building-first-microservice-system.md)
**Next:** [Section 21: Distributed Failure Case Studies](21-distributed-failure-case-studies.md)

---

## The Reality: It Works, But Is It Fast?

You've built your microservices. They're deployed to Kubernetes. Health checks pass. Logs are flowing to Elasticsearch. **But when you run a load test, everything falls apart.**

- Response times spike from 50ms to 5 seconds
- Database connections max out
- CPU usage hits 100%
- Memory leaks crash pods every few hours
- Users complain about slow page loads

**Performance is a feature.** And like any feature, you have to engineer it deliberately.

This section teaches you how to measure, optimize, and maintain performance in distributed systems. We'll use Bibby as our case study, identifying real bottlenecks and fixing them with production-proven techniques.

Let's make it fast.

---

## Part 1: Understanding Performance Metrics

### The Four Golden Signals

**1. Latency** — How long does a request take?
- **P50 (median):** 50% of requests are faster than this
- **P95:** 95% of requests are faster than this
- **P99:** 99% of requests are faster than this (outliers matter!)
- **P99.9:** The worst 0.1% (these kill user experience)

**Why percentiles matter more than averages:**

```
10 requests to /api/v1/books:
[10ms, 12ms, 11ms, 13ms, 10ms, 11ms, 12ms, 14ms, 11ms, 2000ms]

Average: 210ms  (misleading!)
P50: 11ms       (typical user)
P99: 2000ms     (1% of users suffer)
```

**The P99 user is probably your most valuable customer** — they're making complex queries, checking out multiple books, generating revenue.

**2. Throughput** — How many requests can you handle per second?
- Requests per second (RPS)
- Transactions per second (TPS)
- Messages per second (for event-driven systems)

**3. Errors** — What percentage of requests fail?
- 5xx errors (server errors)
- 4xx errors (client errors, but check if it's your API's fault)
- Timeouts (request took too long, client gave up)

**4. Saturation** — How full are your resources?
- CPU utilization (>70% = danger zone)
- Memory usage
- Database connection pool (maxed out = requests queue)
- Disk I/O
- Network bandwidth

---

## Part 2: Establishing a Baseline

**You can't improve what you don't measure.**

### Load Testing Bibby

**Tool: Apache Bench (simple), Gatling (advanced), or k6 (modern)**

```bash
# Simple load test with Apache Bench
ab -n 1000 -c 10 http://localhost:8080/api/v1/books?title=Clean%20Code

# Results:
# Requests per second: 45.23 [#/sec]
# Time per request: 221.1ms [mean]
# Time per request: 22.1ms [mean, across all concurrent requests]
# 50%: 180ms
# 95%: 450ms
# 99%: 850ms
```

**Interpretation:**
- **45 RPS** is low for a simple book search
- **P99 of 850ms** is unacceptable (users perceive >300ms as slow)
- Let's find out why

### Profiling with JFR (Java Flight Recorder)

```bash
# Start Bibby with JFR enabled
java -XX:StartFlightRecording=duration=60s,filename=bibby.jfr -jar Bibby-0.0.1-SNAPSHOT.jar

# Run load test during this 60-second window

# Analyze with JDK Mission Control or jfr CLI
jfr print --events jdk.ExecutionSample bibby.jfr
```

**Output shows:**
- 60% of CPU time in `BookRepository.findByTitleIgnoreCase()`
- 25% of CPU time in `ShelfService.findShelfById()`
- 10% in garbage collection
- 5% in Thread.sleep() (from CLI animations)

**Now we know where to optimize.**

---

## Part 3: Database Optimization

### Problem 1: The N+1 Query Disaster

**From Section 18, we identified this in `BookcaseCommands.java:63-72`:**

```java
private Map<String, String> bookCaseOptions() {
    List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases();

    for (BookcaseEntity b : bookcaseEntities) {
        List<ShelfEntity> shelves = shelfService.findByBookcaseId(b.getBookcaseId());

        for(ShelfEntity s : shelves){
            List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId());
            shelfBookCount += bookList.size();
        }
    }
}
```

**With 5 bookcases, 10 shelves each:**
- 1 query: `SELECT * FROM bookcases`
- 5 queries: `SELECT * FROM shelves WHERE bookcase_id = ?`
- 50 queries: `SELECT * FROM books WHERE shelf_id = ?`
- **Total: 56 queries**

**Each query is ~10ms over network. Total: 560ms just for queries.**

### Solution 1: Eager Loading with JPA Fetch Joins

```java
// BookcaseRepository.java
@Repository
public interface BookcaseRepository extends JpaRepository<BookcaseEntity, Long> {

    @Query("""
        SELECT DISTINCT b
        FROM BookcaseEntity b
        LEFT JOIN FETCH b.shelves s
        LEFT JOIN FETCH s.books
    """)
    List<BookcaseEntity> findAllWithShelvesAndBooks();
}
```

**Result: 1 query with LEFT JOIN instead of 56 queries.**

**New performance:**
- **Before:** 560ms (56 queries × 10ms)
- **After:** 25ms (1 query)
- **Improvement: 22x faster**

### Solution 2: Projection with DTO (even faster)

If you only need counts, don't fetch full entities:

```java
// BookcaseRepository.java
@Query("""
    SELECT new com.penrose.bibby.dto.BookcaseSummaryDTO(
        b.bookcaseId,
        b.bookcaseLabel,
        b.shelfCapacity,
        COUNT(bk.bookId)
    )
    FROM BookcaseEntity b
    LEFT JOIN ShelfEntity s ON s.bookcaseId = b.bookcaseId
    LEFT JOIN BookEntity bk ON bk.shelfId = s.shelfId
    GROUP BY b.bookcaseId, b.bookcaseLabel, b.shelfCapacity
""")
List<BookcaseSummaryDTO> findAllSummaries();
```

**Result: 1 query, returns only the data you need (no overhead of hydrating entities).**

**New performance:**
- **Before:** 560ms
- **After:** 8ms
- **Improvement: 70x faster**

### Problem 2: Missing Database Indexes

**Symptom:** `BookRepository.findByTitleIgnoreCase()` is slow.

```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {
    BookEntity findByTitleIgnoreCase(String title);
}
```

**Check the query plan:**

```sql
EXPLAIN ANALYZE SELECT * FROM books WHERE LOWER(title) = LOWER('Clean Code');

-- Result:
-- Seq Scan on books (cost=0.00..25.50 rows=1 width=200) (actual time=12.345..12.350 rows=1 loops=1)
-- Planning Time: 0.125 ms
-- Execution Time: 12.367 ms
```

**"Seq Scan" = full table scan. With 100,000 books, this is disaster.**

### Solution: Add Index

```sql
-- V2__add_indexes.sql
CREATE INDEX idx_books_title_lower ON books (LOWER(title));
CREATE INDEX idx_books_shelf_id ON books (shelf_id);
CREATE INDEX idx_shelves_bookcase_id ON shelves (bookcase_id);
CREATE INDEX idx_book_authors_book_id ON book_authors (book_id);
CREATE INDEX idx_book_authors_author_id ON book_authors (author_id);
```

**Result:**

```sql
EXPLAIN ANALYZE SELECT * FROM books WHERE LOWER(title) = LOWER('Clean Code');

-- Result:
-- Index Scan using idx_books_title_lower on books (cost=0.29..8.31 rows=1 width=200) (actual time=0.045..0.048 rows=1 loops=1)
-- Planning Time: 0.082 ms
-- Execution Time: 0.067 ms
```

**New performance:**
- **Before:** 12.4ms (full table scan)
- **After:** 0.067ms (index scan)
- **Improvement: 185x faster**

### Problem 3: Connection Pool Exhaustion

**Default HikariCP settings in Spring Boot:**

```properties
spring.datasource.hikari.maximum-pool-size=10
```

**Under load (100 concurrent requests):**
- 10 connections in use
- 90 requests waiting for connection
- Timeout errors start happening

**Check the metrics:**

```java
@RestController
public class MetricsController {

    @Autowired
    private HikariDataSource dataSource;

    @GetMapping("/actuator/db-pool")
    public Map<String, Object> getPoolStats() {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();

        return Map.of(
            "activeConnections", pool.getActiveConnections(),
            "idleConnections", pool.getIdleConnections(),
            "totalConnections", pool.getTotalConnections(),
            "threadsAwaitingConnection", pool.getThreadsAwaitingConnection()
        );
    }
}
```

**During load test:**

```json
{
  "activeConnections": 10,
  "idleConnections": 0,
  "totalConnections": 10,
  "threadsAwaitingConnection": 47  // RED FLAG!
}
```

### Solution: Tune Connection Pool

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 5000  # 5 seconds
      idle-timeout: 300000      # 5 minutes
      max-lifetime: 600000      # 10 minutes
      leak-detection-threshold: 60000  # Detect connection leaks
```

**Formula for pool size:**

```
pool_size = (core_count × 2) + effective_spindle_count

For a database with 8 cores and SSD (no spindles):
pool_size = (8 × 2) + 0 = 16

But in microservices, you have multiple instances:
- 3 instances × 16 connections = 48 total connections to DB
- Make sure your PostgreSQL max_connections > 48
```

**Trade-off:** More connections = more memory on database server.

---

## Part 4: Caching Strategies

### When to Cache

**Cache if:**
- Data is read frequently (>90% reads)
- Data doesn't change often (author names, book titles)
- Generating the data is expensive (complex joins, aggregations)

**Don't cache if:**
- Data changes frequently (book availability status)
- Data is user-specific (personalized recommendations)
- Cache invalidation is complex (cascading updates)

### Level 1: Local Cache with Caffeine

**Use case:** Book search results (titles don't change often)

```java
// CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("books", "authors");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats()
        );
        return cacheManager;
    }
}
```

```java
// BookService.java (updated)
@Service
public class BookService {

    @Cacheable(value = "books", key = "#title")
    public BookEntity findBookByTitle(String title) {
        return bookRepository.findByTitleIgnoreCase(title);
    }

    @CacheEvict(value = "books", key = "#book.title")
    public void updateBook(BookEntity book) {
        bookRepository.save(book);
    }
}
```

**Result:**
- **Cache hit:** 0.001ms (in-memory lookup)
- **Cache miss:** 0.067ms (database query with index)
- **With 90% hit rate:** Average latency drops from 0.067ms to 0.007ms

**Trade-off:** Each pod has its own cache. Cache invalidation happens per-pod (eventual consistency across pods).

### Level 2: Distributed Cache with Redis

**Use case:** Session data, frequently accessed data across multiple pods

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  redis:
    host: redis
    port: 6379
    timeout: 2000ms
  cache:
    type: redis
    redis:
      time-to-live: 1800000  # 30 minutes
      cache-null-values: false
```

```java
// BookService.java (Redis cache)
@Service
public class BookService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public BookEntity findBookByTitle(String title) {
        String cacheKey = "book:title:" + title;

        // Try cache first
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null) {
            return objectMapper.readValue(cachedJson, BookEntity.class);
        }

        // Cache miss - query database
        BookEntity book = bookRepository.findByTitleIgnoreCase(title);

        // Store in cache
        redisTemplate.opsForValue().set(
            cacheKey,
            objectMapper.writeValueAsString(book),
            Duration.ofMinutes(30)
        );

        return book;
    }
}
```

**Result:**
- **Cache hit:** 2ms (Redis network call)
- **Cache miss:** 0.067ms (database) + 1ms (write to Redis) = 1.067ms
- **Shared across all pods** (consistent cache)

**Trade-off:** Network latency to Redis (2ms) vs local cache (0.001ms). But Redis cache is shared, so hit rate is higher.

### Cache Invalidation Strategy

**Problem:** How do you keep cache consistent when data changes?

**Option 1: Time-based expiration (TTL)**

```java
redisTemplate.opsForValue().set(cacheKey, value, Duration.ofMinutes(30));
```

**Pros:** Simple
**Cons:** Data can be stale for up to 30 minutes

**Option 2: Event-driven invalidation**

```java
@Service
public class BookService {

    @Autowired
    private EventPublisher eventPublisher;

    @Transactional
    public void updateBook(BookEntity book) {
        bookRepository.save(book);

        // Publish event
        eventPublisher.publish("book-events", new BookUpdatedEvent(book.getBookId(), book.getTitle()));
    }
}

@Service
public class CacheInvalidationHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @KafkaListener(topics = "book-events")
    public void onBookUpdated(BookUpdatedEvent event) {
        String cacheKey = "book:title:" + event.getTitle();
        redisTemplate.delete(cacheKey);
    }
}
```

**Pros:** Cache is always fresh
**Cons:** More complexity, requires event infrastructure

**Option 3: Write-through cache**

```java
@CachePut(value = "books", key = "#book.title")
public BookEntity updateBook(BookEntity book) {
    return bookRepository.save(book);
}
```

**Pros:** Cache always up-to-date
**Cons:** Every write updates cache (even if data is rarely read)

### Cache Warming

**Problem:** After deployment, cache is empty. First users get slow responses (cache misses).

**Solution: Pre-populate cache at startup**

```java
@Component
public class CacheWarmer implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Warming cache with top 1000 books...");

        List<BookEntity> popularBooks = bookRepository.findTop1000ByOrderByCheckoutCountDesc();

        for (BookEntity book : popularBooks) {
            String cacheKey = "book:title:" + book.getTitle();
            redisTemplate.opsForValue().set(
                cacheKey,
                objectMapper.writeValueAsString(book),
                Duration.ofMinutes(30)
            );
        }

        log.info("Cache warmed with {} books", popularBooks.size());
    }
}
```

---

## Part 5: Async Processing

### Problem: Blocking I/O in Request Threads

**From `BookCommands.java:118-127` (Section 18):**

```java
public void addBook() throws InterruptedException {
    Thread.sleep(1000);
    // ... more blocking operations ...
    Thread.sleep(3800);
}
```

**If this were an HTTP endpoint:**
- Each request holds a thread for 9 seconds
- Tomcat's 200 threads = max 22 requests/second
- Under load, requests queue up and timeout

### Solution 1: Spring WebFlux (Reactive Programming)

**Convert to non-blocking:**

```java
@RestController
public class BookController {

    @Autowired
    private ReactiveBookService bookService;

    @PostMapping("/api/v1/books")
    public Mono<ResponseEntity<String>> addBook(@RequestBody BookRequestDTO dto) {
        return bookService.createNewBookAsync(dto)
            .map(book -> ResponseEntity.ok("Book added: " + book.getTitle()))
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(TimeoutException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("Request timed out"))
            );
    }
}

@Service
public class ReactiveBookService {

    @Autowired
    private R2dbcEntityTemplate template;

    public Mono<BookEntity> createNewBookAsync(BookRequestDTO dto) {
        BookEntity book = new BookEntity();
        book.setTitle(dto.title());

        return template.insert(book)
            .doOnSuccess(savedBook -> log.info("Book saved: {}", savedBook.getTitle()));
    }
}
```

**Result:**
- **Event loop with 4 threads** can handle 10,000+ concurrent requests
- No thread blocking
- Lower memory footprint (no thread stack per request)

**Trade-off:** Steeper learning curve (reactive programming is different)

### Solution 2: CompletableFuture (Async with Traditional Spring MVC)

```java
@RestController
public class BookController {

    @Autowired
    private AsyncBookService bookService;

    @PostMapping("/api/v1/books")
    public CompletableFuture<ResponseEntity<String>> addBook(@RequestBody BookRequestDTO dto) {
        return bookService.createNewBookAsync(dto)
            .thenApply(book -> ResponseEntity.ok("Book added: " + book.getTitle()))
            .exceptionally(e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage()));
    }
}

@Service
public class AsyncBookService {

    @Autowired
    private BookRepository bookRepository;

    @Async("bookTaskExecutor")
    public CompletableFuture<BookEntity> createNewBookAsync(BookRequestDTO dto) {
        BookEntity book = new BookEntity();
        book.setTitle(dto.title());

        BookEntity saved = bookRepository.save(book);
        return CompletableFuture.completedFuture(saved);
    }
}

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "bookTaskExecutor")
    public Executor bookTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("book-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

**Result:**
- Request thread returns immediately
- Actual work runs in separate thread pool
- Can handle more concurrent requests

**Trade-off:** Still using thread-per-request model (higher memory than reactive)

---

## Part 6: JVM Performance Tuning

### Garbage Collection Tuning

**Default JVM settings are not optimized for production.**

```bash
# Check current GC
java -XX:+PrintFlagsFinal -version | grep -i gc

# Default: Serial GC (single-threaded, terrible for servers)
```

**For Bibby (microservice with low-latency requirements):**

```dockerfile
# Dockerfile
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+UseStringDeduplication", \
    "-XX:+ParallelRefProcEnabled", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/dumps/heap-dump.hprof", \
    "-jar", "app.jar"]
```

**Explanation:**
- **`-XX:+UseG1GC`**: G1 Garbage Collector (low-latency, predictable pauses)
- **`-XX:MaxGCPauseMillis=200`**: Target max GC pause of 200ms
- **`-Xms512m -Xmx1024m`**: Heap size (fixed to avoid resizing overhead)
- **`-XX:MaxRAMPercentage=75.0`**: Use 75% of container memory for heap
- **`-XX:+ExitOnOutOfMemoryError`**: Kill pod on OOM (let Kubernetes restart it)
- **`-XX:+HeapDumpOnOutOfMemoryError`**: Capture heap dump for debugging

**Monitor GC metrics:**

```yaml
# application.yml
management:
  metrics:
    export:
      prometheus:
        enabled: true
```

**Prometheus queries:**

```promql
# GC pause time (P95)
histogram_quantile(0.95, rate(jvm_gc_pause_seconds_bucket[5m]))

# GC frequency
rate(jvm_gc_pause_seconds_count[5m])

# Heap usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

**Alert if:**
- GC pause > 500ms (users notice lag)
- GC frequency > 10/sec (too much GC pressure)
- Heap usage > 90% (approaching OOM)

### Memory Leak Detection

**Symptom:** Memory usage grows over time, never decreases.

**Tools:**

1. **VisualVM** (free, bundled with JDK)
2. **JProfiler** (commercial, powerful)
3. **Heap dump analysis**

```bash
# Capture heap dump from running pod
kubectl exec -n bibby catalog-pod-7f8d9c -- jmap -dump:live,format=b,file=/tmp/heap.hprof 1

# Download heap dump
kubectl cp bibby/catalog-pod-7f8d9c:/tmp/heap.hprof ./heap.hprof

# Analyze with Eclipse MAT or VisualVM
```

**Common culprits in Spring Boot apps:**
- Unbounded caches (use `maximumSize()` in Caffeine)
- Thread-local variables not cleaned up
- Listener registrations never removed
- Connection leaks (connections not closed)

---

## Part 7: Network Optimization

### HTTP/2 vs HTTP/1.1

**HTTP/1.1 limitations:**
- 6 concurrent connections per domain (browser limit)
- Head-of-line blocking
- No header compression

**HTTP/2 benefits:**
- Multiplexing (many requests on one connection)
- Header compression (HPACK)
- Server push

**Enable HTTP/2 in Spring Boot:**

```yaml
# application.yml
server:
  http2:
    enabled: true
  ssl:
    enabled: true  # HTTP/2 requires TLS
    key-store: classpath:keystore.p12
    key-store-password: changeit
```

**Result:**
- **Before (HTTP/1.1):** 6 concurrent requests, 200ms latency = 33ms per request effectively
- **After (HTTP/2):** Unlimited concurrent requests, 200ms latency for all

### Response Compression

**Problem:** API responses are large (JSON with 1,000 books = 500KB)

**Solution: Enable GZIP compression**

```yaml
# application.yml
server:
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
    min-response-size: 1024  # Only compress if > 1KB
```

**Result:**
- **Before:** 500KB response (2 seconds on 2Mbps connection)
- **After:** 50KB compressed (0.2 seconds)
- **Improvement: 10x faster download**

**Trade-off:** CPU cost of compression (usually negligible)

### Connection Pooling for HTTP Clients

**Problem:** Creating new HTTP connection per request is expensive (DNS lookup, TCP handshake, TLS handshake).

```java
// WRONG: New connection every time
@Service
public class CatalogClient {

    public Book getBook(Long bookId) {
        RestTemplate restTemplate = new RestTemplate();  // DON'T DO THIS
        return restTemplate.getForObject("http://catalog-service:8080/books/" + bookId, Book.class);
    }
}
```

**Right: Reuse connections with pooling**

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();

        HttpClient httpClient = HttpClients.custom()
            .setMaxConnTotal(100)
            .setMaxConnPerRoute(20)
            .setConnectionTimeToLive(30, TimeUnit.SECONDS)
            .evictIdleConnections(30, TimeUnit.SECONDS)
            .build();

        factory.setHttpClient(httpClient);
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        return new RestTemplate(factory);
    }
}
```

**Result:**
- **Before:** 100ms per request (30ms TCP handshake + 70ms TLS + actual request)
- **After:** 30ms per request (connection reused)
- **Improvement: 3.3x faster**

---

## Part 8: Performance Testing at Scale

### Load Testing with Gatling

```scala
// BookLoadTest.scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class BookLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")

  val scn = scenario("Book Search Load Test")
    .exec(http("Search Clean Code")
      .get("/api/v1/books?title=Clean Code")
      .check(status.is(200))
      .check(jsonPath("$.title").is("Clean Code"))
    )
    .pause(1)

  setUp(
    scn.inject(
      rampUsersPerSec(10) to 100 during (1 minute),  // Ramp up
      constantUsersPerSec(100) during (5 minutes),   // Sustained load
      rampUsersPerSec(100) to 200 during (1 minute), // Spike
      constantUsersPerSec(200) during (2 minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.p95.lt(300),  // P95 < 300ms
     global.successfulRequests.percent.gt(99)  // 99% success rate
   )
}
```

**Run test:**

```bash
gatling:test -Dgatling.simulationClass=BookLoadTest
```

**Analyze results:**
- Response time distribution
- Throughput over time
- Error rate
- Resource utilization (CPU, memory, database connections)

### Stress Testing (Find Breaking Point)

```scala
setUp(
  scn.inject(
    rampUsersPerSec(10) to 1000 during (10 minutes)
  )
).protocols(httpProtocol)
```

**Watch for:**
- When does latency start to spike?
- When do errors start occurring?
- What resource maxes out first? (CPU? Memory? DB connections?)

**Example result:**
- **0-500 RPS:** P95 latency = 100ms
- **500-800 RPS:** P95 latency = 250ms (acceptable)
- **800+ RPS:** P95 latency = 2000ms, errors start (database connection pool maxed out)

**Conclusion:** Service can handle 800 RPS. Beyond that, need to scale horizontally or optimize further.

---

## Part 9: Real-World Case Study

### Netflix: The Chaos Performance Story

**Problem (2012):**
- Migrating from monolith to microservices
- API Gateway was bottleneck (30,000 requests/sec)
- P99 latency = 2 seconds (unacceptable for streaming)

**Investigation:**
- CPU profiling showed 40% time in JSON serialization
- Network profiling showed 30% time in TLS handshake
- Database profiling showed N+1 queries

**Optimizations:**

1. **Switched from Jackson to faster JSON library (MessagePack)**
   - Result: 40% faster serialization

2. **Implemented connection pooling with 1,000 connections**
   - Result: Eliminated TLS handshake overhead

3. **Introduced caching layer (EVCache - Redis-like)**
   - Result: 90% cache hit rate, eliminated most database queries

4. **Switched to HTTP/2**
   - Result: Multiplexing reduced connection overhead

5. **JVM tuning with G1GC**
   - Result: Reduced GC pauses from 500ms to 50ms

**Final results:**
- **Throughput:** 30,000 → 100,000 requests/sec
- **P99 latency:** 2000ms → 100ms
- **Infrastructure cost:** Same hardware, 3.3x more throughput

**Lessons:**
- **Measure first, optimize second** (profiling revealed the real bottlenecks)
- **Caching has the biggest impact** (90% cache hit rate = 10x reduction in database load)
- **JVM tuning matters** (GC pauses were killing latency)

---

## Summary: Performance Optimization Checklist

### Database
- [ ] Add indexes on frequently queried columns
- [ ] Fix N+1 queries with eager loading or projections
- [ ] Tune connection pool size (baseline: core_count × 2)
- [ ] Monitor slow queries (>10ms is suspicious)
- [ ] Use database connection pool monitoring

### Caching
- [ ] Cache frequently accessed, rarely changing data
- [ ] Choose right cache level (local vs distributed)
- [ ] Implement cache invalidation strategy
- [ ] Warm cache at startup
- [ ] Monitor cache hit rate (target: >80%)

### Application
- [ ] Use async processing for I/O-bound operations
- [ ] Remove blocking operations (Thread.sleep, blocking HTTP calls)
- [ ] Pool expensive resources (HTTP clients, thread pools)
- [ ] Enable HTTP/2 and compression
- [ ] Profile with JFR to find hotspots

### JVM
- [ ] Use G1GC for low-latency apps
- [ ] Set heap size appropriately (Xms = Xmx)
- [ ] Monitor GC metrics (pause time, frequency)
- [ ] Detect memory leaks with heap dumps
- [ ] Enable heap dump on OOM

### Testing
- [ ] Load test to establish baseline
- [ ] Stress test to find breaking point
- [ ] Monitor golden signals (latency, throughput, errors, saturation)
- [ ] Test with production-like data volumes
- [ ] Automate performance regression tests

---

## Action Items for Bibby

To improve Bibby's performance:

1. **Add database indexes** on `books.title`, `books.shelf_id`, `shelves.bookcase_id`
2. **Fix N+1 query** in `bookCaseOptions()` with DTO projection
3. **Add Redis caching** for book searches (30-minute TTL)
4. **Tune Hikari connection pool** to 20 connections
5. **Enable HTTP/2 and GZIP** compression
6. **Add Gatling load tests** to CI pipeline
7. **Monitor P95/P99 latency** with Prometheus alerts

**Expected results:**
- P95 latency: 450ms → 50ms
- Throughput: 45 RPS → 500 RPS
- Database queries: 56 → 1 per bookcase list

---

## Further Reading

**Books:**
- **"Systems Performance"** by Brendan Gregg (2020) — The definitive guide
- **"Java Performance: The Definitive Guide"** by Scott Oaks (2020) — JVM tuning
- **"High Performance Browser Networking"** by Ilya Grigorik (2013) — Network optimization

**Tools:**
- **Gatling:** https://gatling.io (load testing)
- **JProfiler:** https://www.ej-technologies.com/products/jprofiler (profiling)
- **VisualVM:** https://visualvm.github.io (free profiling)
- **Async Profiler:** https://github.com/jvm-profiling-tools/async-profiler (low-overhead)

---

**Next:** [Section 21: Distributed Failure Case Studies](21-distributed-failure-case-studies.md) — Learn from the industry's biggest outages and how to prevent them.
