# Section 8: Modern Spring Boot Practices

## Overview
This section covers opportunities to leverage modern Spring Boot features, best practices, and conventions that make your code more maintainable, testable, and aligned with the Spring ecosystem.

**Issues Found: 4**
- Critical: 0
- High: 1
- Medium: 3
- Low: 0

---

## Issue 8.1: Inconsistent Use of @Transactional

**Priority:** 🟠 **HIGH**
**Effort:** 1 hour
**Files:** `BookService.java`, `BookcaseService.java`, `CatalogService.java`

### Current Code (WRONG ❌)

```java
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    // No @Transactional - each repository call is a separate transaction
    public BookEntity createNewBook(BookRequestDTO request) {
        Optional<AuthorEntity> existingAuthor = authorRepository
            .findByFirstNameAndLastName(request.authorFirstName(), request.authorLastName());

        AuthorEntity author;
        if (existingAuthor.isEmpty()) {
            author = new AuthorEntity();
            author.setFirstName(request.authorFirstName());
            author.setLastName(request.authorLastName());
            author = authorRepository.save(author);  // Transaction 1
        } else {
            author = existingAuthor.get();
        }

        BookEntity book = new BookEntity();
        book.setBookTitle(request.bookTitle());
        book.setAuthor(author);
        book.setBookStatus(BookStatus.AVAILABLE);
        return bookRepository.save(book);  // Transaction 2 (separate!)
    }
}
```

**What Goes Wrong:**

```
Timeline:
T1: authorRepository.save(author) → COMMIT ← Author saved
T2: Server crashes before bookRepository.save()
Result: Orphaned author in database with no associated book!
```

### Why This Is Problematic

1. **Data Inconsistency**: If the second operation fails, you have orphaned data
2. **No Rollback**: First operation commits even if second fails
3. **Concurrency Issues**: Another thread could see incomplete state
4. **Lost Updates**: Lazy-loaded collections may not be initialized outside transaction
5. **Performance**: Multiple small transactions have overhead vs. one larger transaction

**Real-World Scenario:**
```java
// User tries to create book
bookService.createNewBook(new BookRequestDTO("Clean Code", "Robert", "Martin"));

// Author "Robert Martin" gets created → COMMITTED
// Application crashes before book is created
// Now database has author with no books - orphaned data!
```

### Correct Approach

Use **@Transactional** to ensure all database operations within a method execute as a single atomic unit.

### Fixed Code (RIGHT ✅)

**Fix 1: Add @Transactional to Service Methods**

```java
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    @Transactional  // ← Single transaction for both operations
    public BookEntity createNewBook(BookRequestDTO request) {
        Optional<AuthorEntity> existingAuthor = authorRepository
            .findByFirstNameAndLastName(request.authorFirstName(), request.authorLastName());

        AuthorEntity author;
        if (existingAuthor.isEmpty()) {
            author = new AuthorEntity();
            author.setFirstName(request.authorFirstName());
            author.setLastName(request.authorLastName());
            author = authorRepository.save(author);  // Part of transaction
        } else {
            author = existingAuthor.get();
        }

        BookEntity book = new BookEntity();
        book.setBookTitle(request.bookTitle());
        book.setAuthor(author);
        book.setBookStatus(BookStatus.AVAILABLE);
        return bookRepository.save(book);  // Part of same transaction
        // Both operations commit together or rollback together!
    }

    @Transactional(readOnly = true)  // ← Optimization for read-only operations
    public List<BookEntity> findAllBooks() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<BookEntity> findBookById(Long id) {
        return bookRepository.findById(id);
    }

    @Transactional  // Required for write operations
    public void checkoutBook(Long bookId, String memberName) {
        BookEntity book = bookRepository.findById(bookId)
            .orElseThrow(() -> new EntityNotFoundException("Book not found: " + bookId));

        if (book.getBookStatus() != BookStatus.AVAILABLE) {
            throw new IllegalStateException("Book is not available for checkout");
        }

        book.setBookStatus(BookStatus.CHECKED_OUT);
        book.setCheckedOutBy(memberName);
        book.setCheckedOutDate(LocalDate.now());

        bookRepository.save(book);  // Automatic flush at transaction commit
    }
}
```

**Fix 2: Transaction Boundaries for Complex Operations**

```java
@Service
@RequiredArgsConstructor
public class BookcaseService {

    private final BookcaseRepository bookcaseRepository;
    private final ShelfRepository shelfRepository;
    private final BookRepository bookRepository;

    @Transactional  // Covers all operations atomically
    public void deleteBookcaseWithContents(Long bookcaseId) {
        BookcaseEntity bookcase = bookcaseRepository.findById(bookcaseId)
            .orElseThrow(() -> new EntityNotFoundException("Bookcase not found"));

        // Step 1: Find all shelves
        List<ShelfEntity> shelves = shelfRepository.findByBookcaseId(bookcaseId);

        // Step 2: Delete all books on each shelf
        for (ShelfEntity shelf : shelves) {
            List<BookEntity> books = bookRepository.findByShelfId(shelf.getShelfId());
            bookRepository.deleteAll(books);
        }

        // Step 3: Delete all shelves
        shelfRepository.deleteAll(shelves);

        // Step 4: Delete the bookcase
        bookcaseRepository.delete(bookcase);

        // If ANY step fails, ALL steps roll back!
    }

    @Transactional(readOnly = true)
    public BookcaseWithBooksDTO getBookcaseWithAllBooks(Long bookcaseId) {
        BookcaseEntity bookcase = bookcaseRepository.findById(bookcaseId)
            .orElseThrow(() -> new EntityNotFoundException("Bookcase not found"));

        List<ShelfEntity> shelves = shelfRepository.findByBookcaseId(bookcaseId);

        List<BookEntity> allBooks = new ArrayList<>();
        for (ShelfEntity shelf : shelves) {
            // Lazy collections initialized within transaction
            allBooks.addAll(bookRepository.findByShelfId(shelf.getShelfId()));
        }

        return new BookcaseWithBooksDTO(bookcase, shelves, allBooks);
    }
}
```

**Fix 3: Transaction Configuration for Different Scenarios**

```java
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final BookRepository bookRepository;
    private final CatalogRepository catalogRepository;

    // Default: propagation = REQUIRED, rollbackFor = RuntimeException
    @Transactional
    public void addBookToCatalog(Long bookId, Long catalogId) {
        // Standard transaction
    }

    // Read-only optimization
    @Transactional(readOnly = true)
    public List<BookEntity> searchCatalog(String query) {
        // Database can optimize for read-only (no flush, no dirty checking)
        return bookRepository.findByBookTitleContainingIgnoreCase(query);
    }

    // Rollback on checked exceptions too
    @Transactional(rollbackFor = Exception.class)
    public void importBooksFromFile(String filePath) throws IOException {
        // Will rollback even if IOException is thrown
    }

    // Requires new transaction (independent of caller's transaction)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuditEvent(String event) {
        // This commit is independent - even if parent transaction rolls back,
        // this audit log will be saved
    }

    // Timeout for long-running operations
    @Transactional(timeout = 30)  // 30 seconds
    public void bulkUpdateBooks(List<Long> bookIds) {
        // Will rollback if operation takes > 30 seconds
    }
}
```

**Fix 4: Transaction Testing**

```java
@SpringBootTest
@Transactional  // Each test runs in a transaction that rolls back
class BookServiceIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldRollbackBothOperationsOnFailure() {
        // Arrange
        long initialAuthorCount = authorRepository.count();
        long initialBookCount = bookRepository.count();

        // Act - simulate failure by providing invalid data
        assertThrows(DataIntegrityViolationException.class, () -> {
            bookService.createNewBook(
                new BookRequestDTO(null, "Robert", "Martin")  // null title → constraint violation
            );
        });

        // Assert - verify rollback
        assertEquals(initialAuthorCount, authorRepository.count(), "Author should not be created");
        assertEquals(initialBookCount, bookRepository.count(), "Book should not be created");
    }
}
```

### Learning Principle

**Use @Transactional to define transaction boundaries.** This ensures data consistency, enables rollback on failures, and is essential for maintaining database integrity. Use `readOnly = true` for queries to optimize performance.

---

## Issue 8.2: Manual Dependency Injection Instead of Constructor Injection

**Priority:** 🟡 **MEDIUM**
**Effort:** 15 minutes
**Files:** `AuthorService.java`, various command classes

### Current Code (WRONG ❌)

```java
@Service
public class AuthorService {

    AuthorRepository authorRepository;  // Not final, not injected!

    // No constructor - Spring can't inject dependency
    // Results in NullPointerException when method is called

    public Optional<AuthorEntity> findAuthorByFullName(String firstName, String lastName) {
        return authorRepository.findByFirstNameAndLastName(firstName, lastName);
        // → NullPointerException!
    }
}
```

### Why This Is Problematic

1. **NullPointerException**: Field is never initialized, always null
2. **No Immutability**: Fields can be accidentally reassigned
3. **Hard to Test**: Cannot easily inject mocks in unit tests
4. **Hidden Dependencies**: Not clear what dependencies a class requires
5. **Optional Dependencies**: Allows creation of objects in invalid state

### Correct Approach

Use **constructor injection** with `final` fields and `@RequiredArgsConstructor`.

### Fixed Code (RIGHT ✅)

**Fix 1: Use Constructor Injection**

```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor for final fields
public class AuthorService {

    private final AuthorRepository authorRepository;  // Final - immutable!

    public Optional<AuthorEntity> findAuthorByFullName(String firstName, String lastName) {
        return authorRepository.findByFirstNameAndLastName(firstName, lastName);
    }

    public AuthorEntity createAuthor(String firstName, String lastName) {
        AuthorEntity author = new AuthorEntity();
        author.setFirstName(firstName);
        author.setLastName(lastName);
        return authorRepository.save(author);
    }
}

// Lombok @RequiredArgsConstructor generates:
// public AuthorService(AuthorRepository authorRepository) {
//     this.authorRepository = authorRepository;
// }
```

**Fix 2: Multiple Dependencies**

```java
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final ShelfRepository shelfRepository;
    private final NotificationService notificationService;

    // All dependencies injected via constructor
    // Spring automatically calls:
    // new BookService(bookRepo, authorRepo, shelfRepo, notificationService)
}
```

**Fix 3: Without Lombok (Manual Constructor)**

```java
@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    // Spring automatically detects this constructor for injection
    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public Optional<AuthorEntity> findAuthorByFullName(String firstName, String lastName) {
        return authorRepository.findByFirstNameAndLastName(firstName, lastName);
    }
}
```

**Fix 4: Testing with Constructor Injection**

```java
@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks  // Mockito uses constructor to inject mocks
    private AuthorService authorService;

    @Test
    void shouldFindAuthorByFullName() {
        // Arrange
        AuthorEntity author = new AuthorEntity();
        author.setFirstName("Robert");
        author.setLastName("Martin");

        when(authorRepository.findByFirstNameAndLastName("Robert", "Martin"))
            .thenReturn(Optional.of(author));

        // Act
        Optional<AuthorEntity> result = authorService.findAuthorByFullName("Robert", "Martin");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Robert", result.get().getFirstName());
    }
}
```

**Fix 5: Optional Dependencies (Rare)**

```java
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final NotificationService notificationService;  // Optional

    // If NotificationService bean doesn't exist, inject null
    public BookService(
            BookRepository bookRepository,
            @Autowired(required = false) NotificationService notificationService) {
        this.bookRepository = bookRepository;
        this.notificationService = notificationService;
    }

    public BookEntity createBook(BookRequestDTO request) {
        BookEntity book = bookRepository.save(mapToEntity(request));

        // Null check for optional dependency
        if (notificationService != null) {
            notificationService.sendBookCreatedNotification(book);
        }

        return book;
    }
}
```

### Learning Principle

**Constructor injection is preferred over field injection** in Spring. It makes dependencies explicit, enables immutability with `final` fields, and simplifies testing. Use Lombok's `@RequiredArgsConstructor` to reduce boilerplate.

---

## Issue 8.3: Not Using Spring Boot Configuration Properties

**Priority:** 🟡 **MEDIUM**
**Effort:** 45 minutes
**Files:** `LoadingBar.java`, command classes

### Current Code (WRONG ❌)

```java
public class LoadingBar {

    // Hardcoded configuration values
    private static final int DEFAULT_STEPS = 40;
    private static final int DEFAULT_DELAY_MS = 150;

    public static void showProgressBar(String message, int steps, int delayMillis)
            throws InterruptedException {
        System.out.print(message + " [");
        for (int i = 0; i < steps; i++) {
            System.out.print("=");
            Thread.sleep(delayMillis);  // Hardcoded delay
        }
        System.out.println("] Done!");
    }
}

// In commands:
public void createBook() throws InterruptedException {
    LoadingBar.showProgressBar("Creating book...", 40, 150);  // Magic numbers!
}
```

### Why This Is Problematic

1. **Hardcoded Values**: Configuration scattered throughout codebase
2. **No Environment-Specific Config**: Cannot change values for dev/test/prod
3. **Recompile to Change**: Must rebuild application to adjust settings
4. **No Type Safety**: Raw strings instead of structured configuration
5. **No Documentation**: Values have no description or context

### Correct Approach

Use **@ConfigurationProperties** to externalize configuration into `application.yml`.

### Fixed Code (RIGHT ✅)

**Step 1: Create Configuration Properties Class**

```java
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "bibby.ui")
public class UIProperties {

    private ProgressBar progressBar = new ProgressBar();
    private Pagination pagination = new Pagination();

    @Data
    public static class ProgressBar {
        /** Number of steps in progress bar animation */
        private int steps = 40;

        /** Delay in milliseconds between each step */
        private int delayMillis = 150;

        /** Character to use for progress indicator */
        private String progressChar = "=";
    }

    @Data
    public static class Pagination {
        /** Default page size for list views */
        private int defaultPageSize = 10;

        /** Maximum allowed page size */
        private int maxPageSize = 100;
    }
}

@Data
@Component
@ConfigurationProperties(prefix = "bibby.business")
public class BusinessProperties {

    private Checkout checkout = new Checkout();

    @Data
    public static class Checkout {
        /** Default checkout duration in days */
        private int defaultDurationDays = 14;

        /** Maximum checkout duration in days */
        private int maxDurationDays = 90;

        /** Late fee per day in cents */
        private int lateFeePerDayCents = 50;
    }
}
```

**Step 2: Create application.yml**

```yaml
# application.yml
bibby:
  ui:
    progress-bar:
      steps: 40
      delay-millis: 150
      progress-char: "="
    pagination:
      default-page-size: 10
      max-page-size: 100
  business:
    checkout:
      default-duration-days: 14
      max-duration-days: 90
      late-fee-per-day-cents: 50

# Environment-specific overrides
---
spring.config.activate.on-profile: dev

bibby:
  ui:
    progress-bar:
      steps: 20  # Faster animations in development
      delay-millis: 50

---
spring.config.activate.on-profile: test

bibby:
  ui:
    progress-bar:
      steps: 1  # No animations in tests
      delay-millis: 0
```

**Step 3: Use Configuration in Services**

```java
@Component
@RequiredArgsConstructor
public class LoadingBar {

    private final UIProperties uiProperties;

    public void showProgressBar(String message) throws InterruptedException {
        UIProperties.ProgressBar config = uiProperties.getProgressBar();

        System.out.print(message + " [");
        for (int i = 0; i < config.getSteps(); i++) {
            System.out.print(config.getProgressChar());
            Thread.sleep(config.getDelayMillis());
        }
        System.out.println("] Done!");
    }
}

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final BusinessProperties businessProperties;
    private final BookRepository bookRepository;

    public CheckoutResult checkoutBook(Long bookId, String memberName) {
        BusinessProperties.Checkout config = businessProperties.getCheckout();

        BookEntity book = bookRepository.findById(bookId)
            .orElseThrow(() -> new EntityNotFoundException("Book not found"));

        book.setBookStatus(BookStatus.CHECKED_OUT);
        book.setCheckedOutBy(memberName);
        book.setCheckedOutDate(LocalDate.now());
        book.setDueDate(LocalDate.now().plusDays(config.getDefaultDurationDays()));

        bookRepository.save(book);

        return new CheckoutResult(book, config.getDefaultDurationDays());
    }

    public BigDecimal calculateLateFee(LocalDate dueDate, LocalDate returnDate) {
        if (!returnDate.isAfter(dueDate)) {
            return BigDecimal.ZERO;
        }

        long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate);
        int feePerDayCents = businessProperties.getCheckout().getLateFeePerDayCents();

        long totalCents = daysLate * feePerDayCents;
        return BigDecimal.valueOf(totalCents, 2);  // Convert cents to dollars
    }
}
```

**Step 4: Enable Configuration Properties (Application Class)**

```java
@SpringBootApplication
@EnableConfigurationProperties({UIProperties.class, BusinessProperties.class})
public class BibbyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibbyApplication.class, args);
    }
}
```

**Step 5: Validation on Configuration Properties**

```java
import jakarta.validation.constraints.*;

@Data
@Component
@ConfigurationProperties(prefix = "bibby.business")
@Validated  // Enable validation
public class BusinessProperties {

    private Checkout checkout = new Checkout();

    @Data
    public static class Checkout {

        @Min(value = 1, message = "Checkout duration must be at least 1 day")
        @Max(value = 365, message = "Checkout duration cannot exceed 365 days")
        private int defaultDurationDays = 14;

        @Min(value = 1, message = "Max duration must be at least 1 day")
        private int maxDurationDays = 90;

        @Min(value = 0, message = "Late fee cannot be negative")
        private int lateFeePerDayCents = 50;
    }
}
```

**Step 6: Testing with Configuration**

```java
@SpringBootTest(properties = {
    "bibby.business.checkout.default-duration-days=7",
    "bibby.business.checkout.late-fee-per-day-cents=100"
})
class CheckoutServiceTest {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private BusinessProperties businessProperties;

    @Test
    void shouldUseConfiguredCheckoutDuration() {
        // Test uses the overridden 7-day duration
        assertEquals(7, businessProperties.getCheckout().getDefaultDurationDays());
    }
}
```

### Learning Principle

**Externalize configuration using @ConfigurationProperties** instead of hardcoding values. This follows the **Twelve-Factor App** principles, making your application configurable without recompilation and supporting different configurations for different environments.

---

## Issue 8.4: Not Leveraging Spring Boot Actuator for Monitoring

**Priority:** 🟡 **MEDIUM**
**Effort:** 30 minutes
**Files:** Application monitoring infrastructure

### Current Code (WRONG ❌)

```java
// No monitoring or health checks
// Cannot tell if application is running properly
// No metrics on database connection pool, memory usage, etc.
```

### Why This Is Problematic

1. **No Health Checks**: Cannot verify application is running correctly
2. **No Metrics**: Cannot monitor performance or resource usage
3. **Difficult Debugging**: No insight into application internals
4. **No Alerting**: Cannot detect issues before users report them
5. **Production Blindness**: No visibility into production behavior

### Correct Approach

Add **Spring Boot Actuator** for production-ready monitoring and management endpoints.

### Fixed Code (RIGHT ✅)

**Step 1: Add Actuator Dependency**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Optional: For detailed metrics -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Step 2: Configure Actuator Endpoints**

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
          - metrics
          - prometheus
          - loggers
  endpoint:
    health:
      show-details: when-authorized  # Show detailed health info
      probes:
        enabled: true  # Enable liveness/readiness probes for Kubernetes
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}

# Application info shown in /actuator/info
info:
  app:
    name: Bibby Library Management System
    description: A comprehensive library management application
    version: @project.version@
  java:
    version: @java.version@
```

**Step 3: Custom Health Indicators**

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final BookRepository bookRepository;

    @Override
    public Health health() {
        try {
            // Test database connectivity
            long bookCount = bookRepository.count();

            return Health.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("bookCount", bookCount)
                .withDetail("message", "Database connection is healthy")
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        }
    }
}

@Component
@RequiredArgsConstructor
public class CheckoutSystemHealthIndicator implements HealthIndicator {

    private final BookRepository bookRepository;
    private final BusinessProperties businessProperties;

    @Override
    public Health health() {
        try {
            // Check for overdue books
            List<BookEntity> overdueBooks = bookRepository.findByBookStatusAndDueDateBefore(
                BookStatus.CHECKED_OUT,
                LocalDate.now()
            );

            int overdueCount = overdueBooks.size();
            int maxAllowed = businessProperties.getCheckout().getMaxDurationDays();

            if (overdueCount > 100) {
                return Health.down()
                    .withDetail("overdueBooks", overdueCount)
                    .withDetail("reason", "Too many overdue books")
                    .build();
            }

            return Health.up()
                .withDetail("overdueBooks", overdueCount)
                .withDetail("status", overdueCount > 50 ? "WARNING" : "OK")
                .build();

        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
```

**Step 4: Custom Metrics**

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final MeterRegistry meterRegistry;

    public BookEntity createNewBook(BookRequestDTO request) {
        // Count book creations
        Counter.builder("bibby.books.created")
            .description("Number of books created")
            .tag("author", request.authorLastName())
            .register(meterRegistry)
            .increment();

        // Measure creation time
        return Timer.builder("bibby.books.create.time")
            .description("Time taken to create a book")
            .register(meterRegistry)
            .record(() -> {
                // Actual creation logic
                return bookRepository.save(mapToEntity(request));
            });
    }

    public void checkoutBook(Long bookId, String memberName) {
        meterRegistry.counter("bibby.checkouts.total",
            "status", "success",
            "member", memberName
        ).increment();

        // Business logic...
    }
}
```

**Step 5: Access Actuator Endpoints**

```bash
# Health check
curl http://localhost:8080/actuator/health
# {
#   "status": "UP",
#   "components": {
#     "database": {
#       "status": "UP",
#       "details": {
#         "database": "PostgreSQL",
#         "bookCount": 1523,
#         "message": "Database connection is healthy"
#       }
#     },
#     "checkoutSystem": {
#       "status": "UP",
#       "details": {
#         "overdueBooks": 23,
#         "status": "OK"
#       }
#     }
#   }
# }

# Application info
curl http://localhost:8080/actuator/info
# {
#   "app": {
#     "name": "Bibby Library Management System",
#     "description": "A comprehensive library management application",
#     "version": "1.0.0"
#   }
# }

# Metrics
curl http://localhost:8080/actuator/metrics/bibby.books.created
# {
#   "name": "bibby.books.created",
#   "measurements": [
#     { "statistic": "COUNT", "value": 156.0 }
#   ]
# }

# Prometheus format (for monitoring tools)
curl http://localhost:8080/actuator/prometheus
# bibby_books_created_total 156.0
# bibby_checkouts_total{status="success"} 423.0
```

**Step 6: Integrate with Monitoring Tools**

```yaml
# docker-compose.yml for local monitoring stack
version: '3.8'
services:
  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
```

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'bibby-application'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### Learning Principle

**Use Spring Boot Actuator for production monitoring.** It provides out-of-the-box health checks, metrics, and management endpoints that are essential for operating applications in production. Integrate with tools like Prometheus and Grafana for comprehensive monitoring.

---

## Summary

| Issue | Priority | Effort | Impact | Fix |
|-------|----------|--------|--------|-----|
| Inconsistent @Transactional usage | 🟠 High | 1 hour | Prevents data inconsistency and enables rollback | Add @Transactional to service methods |
| Missing constructor injection | 🟡 Medium | 15 min | Fixes NPEs, enables testing, improves immutability | Use @RequiredArgsConstructor with final fields |
| Hardcoded configuration values | 🟡 Medium | 45 min | Enables environment-specific config without rebuild | Use @ConfigurationProperties with application.yml |
| No monitoring/health checks | 🟡 Medium | 30 min | Provides production visibility and alerting | Add Spring Boot Actuator |

**Total Estimated Effort:** ~2.5 hours

**Expected Impact After Fixes:**
- ✅ All database operations properly transactional
- ✅ Zero NullPointerExceptions from missing dependency injection
- ✅ Easy configuration changes without recompiling
- ✅ Production-ready health checks and monitoring
- ✅ Metrics for tracking application performance
- ✅ Better testability with constructor injection
- ✅ Environment-specific configurations (dev/staging/prod)

**Spring Boot Best Practices Checklist:**
- [ ] Add @Transactional to all service methods that modify data
- [ ] Use readOnly=true for queries
- [ ] Replace field injection with constructor injection
- [ ] Make all injected fields final
- [ ] Use @RequiredArgsConstructor (Lombok)
- [ ] Extract configuration to @ConfigurationProperties
- [ ] Create application.yml with environment-specific profiles
- [ ] Add Spring Boot Actuator dependency
- [ ] Create custom health indicators
- [ ] Add business metrics with Micrometer
- [ ] Configure Prometheus endpoint
- [ ] Set up monitoring dashboard (Grafana)

---

**Next Step:** Section 10 (Key Takeaways) - The top 10 most important lessons to internalize from this code review.
