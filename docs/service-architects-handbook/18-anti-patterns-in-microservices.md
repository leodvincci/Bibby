# Section 18: Anti-Patterns in Microservices

**Previous:** [Section 17: Patterns for Coordination](17-patterns-for-coordination.md)
**Next:** [Section 19: Building Your First Microservice System](19-building-first-microservice-system.md)

---

## The Problem: When Good Intentions Go Wrong

You've learned the patterns. You know the best practices. You understand distributed systems. **But knowing what to do is only half the battle — you also need to know what NOT to do.**

Anti-patterns are common approaches that seem reasonable at first but lead to pain in production. They're seductive because they feel like progress: "We're building microservices!" But under the surface, they create maintenance nightmares, performance bottlenecks, and operational chaos.

**This section examines the most dangerous anti-patterns in microservices**, using real examples from Bibby's codebase to show you what to avoid and how to fix it.

Let's dive into the mistakes that kill microservices projects — and how to avoid them.

---

## Anti-Pattern #1: The Distributed Monolith

### What It Looks Like

You've split your monolith into services. You have separate repositories, separate deployments, independent teams. **But every service still talks to every other service.** Changing one line of code requires deploying all 15 services. Releases are coordinated nightmares.

**You got all the complexity of microservices with none of the benefits.**

### The Bibby Example

Bibby is currently a monolith with clear domain boundaries:

```
BookcaseService → ShelfService → BookService → AuthorService
```

If we naively split this into services without changing the dependencies, we'd create a distributed monolith:

```
[Bookcase Service] --REST--> [Shelf Service] --REST--> [Book Service] --REST--> [Author Service]
```

Every operation now requires **4 network hops instead of 4 method calls**. Latency explodes. Deployment coupling remains. You've made everything worse.

### Why It Happens

- **Splitting too early:** You decompose before understanding domain boundaries
- **Following code structure:** You split along package lines instead of business capabilities
- **Ignoring Conway's Law:** Your services mirror your org chart, not your domain model
- **No ownership boundaries:** Every team touches every service

### How to Fix It

**1. Start with domain boundaries, not code boundaries:**

```java
// WRONG: Services based on layers
UserService → DatabaseService → CacheService

// RIGHT: Services based on business capabilities
CatalogService (Books + Authors + Search)
CirculationService (Checkouts + Holds + Fines)
NotificationService (Emails + SMS)
```

**2. Apply the Single Responsibility Principle to services:**

Each service should have **one reason to change**. If changing how you send emails requires deploying the Catalog Service, you've failed.

**3. Use event-driven communication to decouple:**

```java
// WRONG: Synchronous coupling
@PostMapping("/api/v1/books")
public ResponseEntity<String> addBook(@RequestBody BookRequestDTO dto) {
    bookService.createNewBook(dto);
    catalogService.indexBook(dto);           // Coupled!
    recommendationService.updateModel(dto);   // Coupled!
    analyticsService.trackEvent(dto);         // Coupled!
    return ResponseEntity.ok("Book added");
}

// RIGHT: Event-driven decoupling
@PostMapping("/api/v1/books")
public ResponseEntity<String> addBook(@RequestBody BookRequestDTO dto) {
    BookEntity book = bookService.createNewBook(dto);

    // Publish event and forget
    eventPublisher.publish(new BookCreatedEvent(
        book.getBookId(), book.getTitle(), book.getAuthors(), Instant.now()
    ));

    return ResponseEntity.ok("Book added");
}
```

Now Catalog, Recommendations, and Analytics can subscribe to `BookCreatedEvent` **without coupling the Book Service to their existence.**

---

## Anti-Pattern #2: Shared Database

### What It Looks Like

You've split your code into services, but they all talk to the **same PostgreSQL database**. Services bypass each other's APIs and query tables directly. Schema changes break multiple services. Database locks create cascading failures.

**This is Bibby's current architecture.**

From `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/amigos
spring.datasource.username=amigos
spring.datasource.password=password
```

All entities (`BookEntity`, `AuthorEntity`, `ShelfEntity`, `BookcaseEntity`) live in one database. If we split into services without fixing this, we'll have:

```
[Catalog Service] ──┐
[Circulation Service] ─┼──> [Single PostgreSQL Database]
[Notification Service] ┘
```

### Why It's Dangerous

**1. No service boundaries:**

```java
// CirculationService bypasses CatalogService and queries books directly
@Service
public class CirculationService {
    @Autowired
    private BookRepository bookRepository;  // WRONG: Accessing another service's data!

    public void checkOutBook(Long bookId, Long patronId) {
        BookEntity book = bookRepository.findById(bookId).orElseThrow();
        // Now CirculationService depends on CatalogService's schema
    }
}
```

**2. Deployment coupling:**

Changing the `books` table schema requires coordinating deployments across Catalog, Circulation, Notification, and Analytics services.

**3. Database contention:**

All services compete for connection pool, locks, and I/O. A slow query in Analytics can deadlock Catalog.

**4. No independent scaling:**

You can't scale the Catalog database without scaling Circulation's data too.

### How to Fix It

**Use Database-Per-Service pattern (Section 9):**

```
[Catalog Service] ──> [catalog_db]
    ↓ (publishes BookCreatedEvent)

[Circulation Service] ──> [circulation_db]
    ↓ (subscribes to BookCreatedEvent, stores local copy of book metadata)
```

CirculationService maintains a **read replica** of book data it needs:

```java
@Service
public class CirculationService {
    @Autowired
    private CirculationBookRepository circulationBookRepo;  // Local replica

    @EventListener
    public void onBookCreated(BookCreatedEvent event) {
        // Store minimal book metadata locally for circulation operations
        CirculationBook book = new CirculationBook(
            event.getBookId(), event.getTitle(), event.getAuthors()
        );
        circulationBookRepo.save(book);
    }

    public void checkOutBook(Long bookId, Long patronId) {
        // Query local database (no cross-service call!)
        CirculationBook book = circulationBookRepo.findById(bookId).orElseThrow();
        // Proceed with checkout
    }
}
```

**Trade-off:** You've introduced **eventual consistency** and **data duplication**. But you've gained **independence** and **resilience**.

---

## Anti-Pattern #3: Chatty APIs (The N+1 Query Problem)

### What It Looks Like

Your frontend makes **hundreds of API calls** to render a single page. Every service call is a network round-trip. Page load times are measured in seconds.

**This is a performance killer in distributed systems.**

### The Bibby Example

From `BookcaseCommands.java:63-72`:

```java
private Map<String, String> bookCaseOptions() {
    Map<String, String> options = new LinkedHashMap<>();

    // 1 query: Fetch all bookcases
    List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases();

    for (BookcaseEntity b : bookcaseEntities) {
        int shelfBookCount = 0;

        // N queries: Fetch shelves for each bookcase
        List<ShelfEntity> shelves = shelfService.findByBookcaseId(b.getBookcaseId());

        for(ShelfEntity s : shelves){
            // N*M queries: Fetch books for each shelf
            List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId());
            shelfBookCount += bookList.size();
        }

        options.put(bookcaseRowFormater(b, shelfBookCount), b.getBookcaseId().toString());
    }
    return options;
}
```

**If you have 5 bookcases with 10 shelves each:**
- 1 query for bookcases
- 5 queries for shelves (one per bookcase)
- 50 queries for books (one per shelf)
- **Total: 56 queries to render a dropdown menu**

**In a monolith:** 56 in-process method calls (~1ms each) = **56ms total**
**In microservices:** 56 HTTP calls (~20ms each) = **1,120ms (1.12 seconds)**

### Why It Happens

- **Lazy loading in ORMs:** Hibernate fetches relationships one at a time
- **Service-oriented thinking:** "Each service should do one thing" leads to granular endpoints
- **No aggregation layer:** Frontend calls services directly

### How to Fix It

**Option 1: Eager loading with JPA joins**

```java
@Repository
public interface BookcaseRepository extends JpaRepository<BookcaseEntity, Long> {

    @Query("""
        SELECT b, s, bk FROM BookcaseEntity b
        LEFT JOIN FETCH ShelfEntity s ON s.bookcaseId = b.bookcaseId
        LEFT JOIN FETCH BookEntity bk ON bk.shelfId = s.shelfId
    """)
    List<BookcaseEntity> findAllWithShelvesAndBooks();
}
```

**Result:** 1 query with joins instead of 56 queries.

**Option 2: Create an aggregation endpoint**

```java
@GetMapping("/api/v1/bookcases/summary")
public List<BookcaseSummaryDTO> getBookcaseSummaries() {
    // Single optimized query returning exactly what the UI needs
    return bookcaseService.getBookcaseSummariesWithCounts();
}

public class BookcaseSummaryDTO {
    private Long bookcaseId;
    private String label;
    private int shelfCount;
    private int bookCount;
}
```

**Option 3: Use GraphQL to let clients specify what they need**

```graphql
query {
  bookcases {
    id
    label
    shelfCount
    bookCount
  }
}
```

GraphQL's DataLoader batches queries automatically.

**Option 4: Backend for Frontend (BFF) pattern**

Create a `LibraryUIService` that **aggregates data from Catalog, Circulation, and Notification services** into responses optimized for your frontend:

```java
@Service
public class LibraryUIService {

    public BookcaseWithStats getBookcaseStats(Long bookcaseId) {
        // Parallel calls to multiple services
        CompletableFuture<Bookcase> bookcase = catalogClient.getBookcaseAsync(bookcaseId);
        CompletableFuture<List<Shelf>> shelves = catalogClient.getShelvesAsync(bookcaseId);
        CompletableFuture<Integer> bookCount = catalogClient.getBookCountAsync(bookcaseId);

        // Wait for all responses
        CompletableFuture.allOf(bookcase, shelves, bookCount).join();

        return new BookcaseWithStats(
            bookcase.get(), shelves.get(), bookCount.get()
        );
    }
}
```

---

## Anti-Pattern #4: Blocking I/O on Request Threads

### What It Looks Like

Your API endpoints use `Thread.sleep()`, blocking JDBC calls, or synchronous HTTP clients. **Each request holds a thread hostage until the operation completes.** Under load, your thread pool exhausts and your service hangs.

**This is all over Bibby.**

From `BookCommands.java:118-127`:

```java
@Command(command = "add", description = "Add a new book to your library database")
public void addBook() throws InterruptedException {
    // ... user input code ...

    Thread.sleep(1000);  // Blocks thread for 1 second
    System.out.println("\n</> Ah, a brand-new book...");
    Thread.sleep(1750);  // Blocks for 1.75 seconds
    System.out.printf("</>:'%s', right?", title);
    Thread.sleep(2350);  // Blocks for 2.35 seconds
    System.out.println("\n</> I'll handle adding it to the database...");
    Thread.sleep(3800);  // Blocks for 3.8 seconds
}
```

**Total blocking time: 9 seconds per request.**

If this were an HTTP endpoint with Tomcat's default 200 threads:
- **After 200 concurrent requests, all threads are blocked**
- Request #201 waits in queue
- Under moderate load (500 req/min), the service becomes unresponsive

### Why It's Dangerous

**Tomcat's default thread pool: 200 threads**

Each thread consumes ~1MB of stack memory. If every thread is blocked waiting:
- **200 MB of memory is wasted on idle threads**
- **New requests queue up**, increasing latency
- **Eventually, requests time out** before even being processed

### How to Fix It

**Option 1: Use reactive programming (Spring WebFlux)**

```java
@RestController
public class BookController {

    @PostMapping("/api/v1/books")
    public Mono<ResponseEntity<String>> addBook(@RequestBody BookRequestDTO dto) {
        return bookService.createNewBookAsync(dto)
            .delayElement(Duration.ofSeconds(1))  // Non-blocking delay
            .map(book -> ResponseEntity.ok("Book added: " + book.getTitle()));
    }
}
```

Spring WebFlux uses **event loop threads** (like Node.js). A single thread can handle thousands of concurrent requests because it never blocks.

**Option 2: Use async servlets (if stuck with Spring MVC)**

```java
@RestController
public class BookController {

    @Autowired
    private AsyncBookService asyncBookService;

    @PostMapping("/api/v1/books")
    public CompletableFuture<ResponseEntity<String>> addBook(@RequestBody BookRequestDTO dto) {
        return asyncBookService.createNewBookAsync(dto)
            .thenApply(book -> ResponseEntity.ok("Book added: " + book.getTitle()));
    }
}

@Service
public class AsyncBookService {

    @Async("taskExecutor")
    public CompletableFuture<BookEntity> createNewBookAsync(BookRequestDTO dto) {
        BookEntity book = bookRepository.save(new BookEntity(dto.title(), dto.firstName(), dto.lastName()));
        return CompletableFuture.completedFuture(book);
    }
}
```

Configure a separate thread pool for async operations:

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-book-");
        executor.initialize();
        return executor;
    }
}
```

**Option 3: Remove unnecessary delays**

In Bibby's case, `Thread.sleep()` is used for **UX pacing in a CLI app**. This is fine for a CLI but **absolutely forbidden in HTTP endpoints.**

---

## Anti-Pattern #5: Missing Error Handling

### What It Looks Like

Your services throw raw exceptions that leak implementation details. There's no global error handler. Clients receive stack traces instead of structured error responses. **Every service handles errors differently.**

**Bibby has no centralized error handling.**

From `BookController.java:18-22`:

```java
@PostMapping("api/v1/books")
public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
    bookService.createNewBook(requestDTO);  // What if this throws?
    return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
}
```

If `createNewBook()` throws:
- **ConstraintViolationException** → 500 Internal Server Error with stack trace
- **DataIntegrityViolationException** → 500 Internal Server Error with SQL details
- **NullPointerException** → 500 Internal Server Error

**Clients have no idea what went wrong.**

### Why It's Dangerous

**1. Information leakage:**

Stack traces reveal your database schema, ORM queries, internal class names — **gold for attackers**.

**2. Poor client experience:**

How should the client retry? Is this a client error (400) or server error (500)? What field was invalid?

**3. No monitoring:**

Without structured errors, you can't alert on specific failure modes.

### How to Fix It

**1. Create a global exception handler**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookNotFound(BookNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            "BOOK_NOT_FOUND",
            ex.getMessage(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        ErrorResponse error = new ErrorResponse(
            "DUPLICATE_BOOK",
            "A book with this title already exists",
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Log full stack trace internally
        log.error("Unexpected error", ex);

        // Return sanitized error to client
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please contact support with request ID: " + UUID.randomUUID(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

public record ErrorResponse(
    String errorCode,
    String message,
    Instant timestamp
) {}
```

**2. Use RFC 7807 Problem Details**

```java
@ExceptionHandler(BookNotFoundException.class)
public ResponseEntity<ProblemDetail> handleBookNotFound(BookNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );
    problem.setTitle("Book Not Found");
    problem.setProperty("bookId", ex.getBookId());
    return ResponseEntity.of(problem).build();
}
```

Client receives:

```json
{
  "type": "about:blank",
  "title": "Book Not Found",
  "status": 404,
  "detail": "No book found with ID 42",
  "bookId": 42
}
```

**3. Create domain exceptions**

```java
public class BookNotFoundException extends RuntimeException {
    private final Long bookId;

    public BookNotFoundException(Long bookId) {
        super("No book found with ID: " + bookId);
        this.bookId = bookId;
    }

    public Long getBookId() {
        return bookId;
    }
}
```

Now your service layer throws **domain exceptions**, not raw JPA exceptions:

```java
@Service
public class BookService {

    public BookEntity findBookById(Long bookId) {
        return bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));
    }
}
```

---

## Anti-Pattern #6: Logging to stdout Instead of Structured Logging

### What It Looks Like

Your services use `System.out.println()` for logging. Logs have no context, no correlation IDs, no structured fields. **In production, you can't trace requests across services.**

**Bibby has 61 `System.out.println()` calls.**

From `BookController.java:24-28`:

```java
@GetMapping("api/v1/books")
public void findBookByTitle(@RequestBody BookRequestDTO requestDTO){
    System.out.println("Controller Search For " + requestDTO.title());
    bookService.findBookByTitle(requestDTO.title());
}
```

**Problems:**
- No timestamp
- No log level (INFO? DEBUG? ERROR?)
- No correlation ID to trace this request across services
- Can't filter by user, book ID, or operation
- Can't send to ELK stack for analysis

### How to Fix It

**1. Use SLF4J with structured logging**

```java
@RestController
@Slf4j  // Lombok annotation
public class BookController {

    @GetMapping("api/v1/books")
    public BookEntity findBookByTitle(@RequestParam String title, @RequestHeader("X-Request-ID") String requestId) {
        log.info("Searching for book",
            kv("title", title),
            kv("requestId", requestId)
        );

        BookEntity book = bookService.findBookByTitle(title);

        if (book == null) {
            log.warn("Book not found",
                kv("title", title),
                kv("requestId", requestId)
            );
            throw new BookNotFoundException(title);
        }

        log.info("Book found",
            kv("bookId", book.getBookId()),
            kv("title", title),
            kv("requestId", requestId)
        );

        return book;
    }
}
```

**2. Use Logback with JSON encoder**

`logback-spring.xml`:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>requestId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>bookId</includeMdcKeyName>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

**Output:**

```json
{
  "timestamp": "2025-11-18T14:32:01.234Z",
  "level": "INFO",
  "logger": "com.penrose.bibby.library.book.BookController",
  "message": "Searching for book",
  "requestId": "a1b2c3d4",
  "title": "Clean Code",
  "service": "catalog-service",
  "host": "catalog-pod-7f8d9c",
  "kubernetes": {
    "namespace": "production",
    "pod": "catalog-pod-7f8d9c"
  }
}
```

Now you can query logs in Kibana:

```
requestId:"a1b2c3d4" AND service:"catalog-service"
```

And trace the **entire request flow across Catalog → Circulation → Notification services**.

**3. Add correlation IDs with filters**

```java
@Component
public class CorrelationIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String correlationId = httpRequest.getHeader("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
```

Now **every log statement** automatically includes the correlation ID.

---

## Anti-Pattern #7: Premature Decomposition

### What It Looks Like

You start a greenfield project by immediately splitting it into 20 microservices. You spend 80% of your time building infrastructure (service mesh, API gateway, distributed tracing) and 20% building features. **Six months in, you're still setting up CI/CD pipelines.**

### Why It's Dangerous

**You don't understand your domain yet.**

Microservices are an **optimization for organizational complexity**, not a starting point. If you have:
- 1-3 developers
- Unclear requirements
- Rapidly changing features

**Start with a modular monolith.**

### The Right Approach

**Phase 1: Modular Monolith (Months 0-12)**

Build Bibby with clear module boundaries:

```
src/
├── catalog/
│   ├── domain/
│   ├── application/
│   └── infrastructure/
├── circulation/
│   ├── domain/
│   ├── application/
│   └── infrastructure/
└── notification/
    ├── domain/
    ├── application/
    └── infrastructure/
```

Enforce boundaries with **ArchUnit tests**:

```java
@ArchTest
public static final ArchRule circulationDoesNotDependOnCatalog =
    noClasses().that().resideInAPackage("..circulation..")
        .should().dependOnClassesThat().resideInAPackage("..catalog..");
```

**Phase 2: Extract High-Value Services (Months 12-18)**

Once you understand your domain:
- **Notification Service** extracts first (independent, event-driven)
- **Catalog Service** extracts next (read-heavy, can use CQRS)
- **Circulation Service** stays in monolith (complex transactions)

**Phase 3: Continuous Extraction (Months 18+)**

Extract services **only when:**
- Different scaling needs (Catalog needs 10x more reads than Circulation)
- Different deployment cadence (Notification deploys 5x/day, Circulation 1x/week)
- Team ownership boundaries solidify

---

## Anti-Pattern #8: No Health Checks

### What It Looks Like

Your services have no `/health` endpoint. Kubernetes doesn't know if a pod is healthy or stuck. **Dead pods receive traffic. Healthy pods are killed during deployment.**

**Bibby has no health checks.**

### How to Fix It

**Add Spring Boot Actuator:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

`application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
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
```

**Custom health check:**

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(1)) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("version", conn.getMetaData().getDatabaseProductVersion())
                    .build();
            }
        } catch (SQLException e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down().build();
    }
}
```

**Kubernetes deployment with probes:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: catalog-service
spec:
  template:
    spec:
      containers:
      - name: catalog
        image: catalog:1.0
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
          failureThreshold: 3
```

**Liveness:** Is the process alive? If not, **restart the pod**.
**Readiness:** Can the pod handle traffic? If not, **remove from load balancer** (but don't kill it).

---

## Anti-Pattern #9: Microservices Envy

### What It Looks Like

You read about how Netflix, Uber, and Amazon use microservices. You want the same architecture. **You ignore that they have thousands of engineers and you have five.**

### The Reality Check

**Netflix:**
- 2,500+ microservices
- 8,000+ engineers
- Dedicated platform teams
- Custom tooling (Eureka, Hystrix, Zuul)

**Your startup:**
- 5 services
- 5 engineers
- No platform team
- Uses off-the-shelf tools

**You don't need Netflix's architecture. You need an architecture that fits your team.**

### The Right Approach

**Ask yourself:**

1. **Do you have independent teams?** If everyone works on everything, microservices won't help.
2. **Do you have different scaling needs?** If everything scales together, one database is fine.
3. **Do you deploy independently?** If you coordinate releases, you haven't decoupled.
4. **Can you afford operational complexity?** Each service needs monitoring, alerting, logging, tracing.

If you answered "no" to most of these, **stick with a modular monolith until you grow into microservices.**

---

## Anti-Pattern #10: Ignoring the Fallacies of Distributed Computing

### The Eight Fallacies

1. **The network is reliable** → Use retries, circuit breakers, timeouts
2. **Latency is zero** → Minimize network hops, use caching, batch requests
3. **Bandwidth is infinite** → Use compression, paginate responses
4. **The network is secure** → Use mTLS, encrypt data in transit
5. **Topology doesn't change** → Use service discovery, don't hardcode IPs
6. **There is one administrator** → Automate deployments, use GitOps
7. **Transport cost is zero** → Monitor network I/O, optimize serialization
8. **The network is homogeneous** → Handle different protocols (HTTP, gRPC, Kafka)

**Every anti-pattern in this section violates one or more of these fallacies.**

---

## War Story: The Great Microservices Migration of 2018

**Company:** Mid-sized e-commerce platform, 50 engineers
**Problem:** Monolithic Rails app was "too slow"

**Decision:** Rewrite everything as microservices using Node.js + MongoDB

**Execution:**
- Split into 30 microservices in 6 months
- No shared database
- Event-driven architecture
- Docker + Kubernetes

**Results (12 months later):**
- **Latency increased 10x** (chatty APIs, N+1 queries)
- **Outages increased 5x** (cascading failures, no circuit breakers)
- **Development velocity decreased 60%** (coordinating changes across services)
- **AWS bill increased 3x** (30 services instead of 1 monolith)

**Root cause:** They split the code but not the team. Everyone still worked on everything. They got distributed system complexity without organizational benefits.

**Resolution:**
- **Merged 30 services back into 5**
- **Kept modular monolith for core business logic**
- **Extracted only high-value services:** Payment processing (PCI compliance), Image resizing (CPU-intensive), Notifications (event-driven)

**New results:**
- Latency returned to original levels
- Outages decreased 80%
- Development velocity recovered
- AWS bill decreased 40%

**Lesson:** Microservices are not a silver bullet. They trade development complexity for operational benefits. **Make sure you need those benefits before paying that cost.**

---

## Action Items: Avoiding Anti-Patterns in Bibby

If you were to evolve Bibby into microservices, here's what you'd do **differently** to avoid these anti-patterns:

### 1. Start with a Modular Monolith
- Keep all code in one repository with clear module boundaries
- Use ArchUnit to enforce separation
- Build in-process events before introducing message queues

### 2. Identify Service Boundaries Using Domain Events
- Catalog Service: `BookCreated`, `BookUpdated`, `BookDeleted`
- Circulation Service: `BookCheckedOut`, `BookReturned`, `FineAssessed`
- Notification Service: `EmailSent`, `SMSSent`

### 3. Extract Services Gradually
- **First:** Notification Service (event-driven, no shared data)
- **Second:** Catalog Service (read-heavy, can use CQRS)
- **Last:** Circulation Service (complex transactions, needs saga pattern)

### 4. Implement Patterns from Day One
- Global exception handler with RFC 7807
- Structured logging with correlation IDs
- Health checks (liveness + readiness)
- Retry logic with exponential backoff
- Circuit breakers for external calls

### 5. Avoid Premature Optimization
- Don't introduce Kafka until you have 1,000+ events/sec
- Don't introduce service mesh until you have 10+ services
- Don't introduce CQRS until you have read/write scaling issues

### 6. Measure Everything
- Latency (P50, P95, P99)
- Error rate (5xx responses)
- Throughput (requests/sec)
- Database query time (N+1 queries)

**If metrics don't show a problem, don't introduce complexity to solve it.**

---

## Summary: The Microservices Anti-Pattern Checklist

Before splitting into microservices, ask yourself:

- [ ] Do we have independent teams with clear ownership?
- [ ] Do we have different scaling needs for different components?
- [ ] Can we deploy services independently without coordination?
- [ ] Have we proven the modular monolith can't meet our needs?
- [ ] Do we have observability infrastructure (logging, metrics, tracing)?
- [ ] Do we have CI/CD pipelines for independent deployments?
- [ ] Can we afford 2-3x operational overhead?
- [ ] Have we identified clear service boundaries using DDD?

**If you answered "no" to more than half, you're not ready for microservices.**

Build a modular monolith first. Extract services when you have **evidence** (metrics, team structure, scaling needs) that justifies the complexity.

**Remember:** The goal is not microservices. The goal is **delivering value to users**. Choose the architecture that maximizes your team's productivity.

---

## Further Reading

**Books:**
- **"Monolith to Microservices"** by Sam Newman (2019) — The definitive guide to gradual migration
- **"Building Event-Driven Microservices"** by Adam Bellemare (2020) — Event-driven anti-patterns
- **"The Software Architect Elevator"** by Gregor Hohpe (2020) — Avoiding architecture astronaut syndrome

**Articles:**
- **"Death Star Architecture"** by Bruce Wong — When your microservices diagram looks like the Death Star
- **"Goodbye Microservices, Hello Monolith"** by Segment Engineering — Why they merged 20 services into 1
- **"Majestic Modular Monoliths"** by Kamil Grzybek — Building monoliths that can split later

**Case Studies:**
- **Istio** — When the solution is more complex than the problem
- **Amazon's Distributed Monolith** — When Conway's Law defeats architecture
- **Uber's Schemaless** — The hidden costs of microservices data

---

**Next:** [Section 19: Building Your First Microservice System](19-building-first-microservice-system.md) — Now that you know what NOT to do, let's build Bibby the right way, step by step.
