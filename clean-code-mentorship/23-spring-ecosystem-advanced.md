# Section 23: Spring Ecosystem & Advanced Topics
## Clean Code + Spring Framework Mentorship

**Focus:** Optional Spring technologies, caching, async processing, security, and advanced patterns

**Estimated Time:** 2-3 hours to read; implement as needed

---

## Overview

You've mastered the core Spring Boot technologies for Bibby. This section introduces **optional but powerful** Spring ecosystem features that can enhance your application.

**Important:** These are **not required** for Bibby to function. Add them only when you have a clear need.

---

## Spring Caching

### When to Use Caching

✅ **Good use cases:**
- Book search results (rarely change)
- Author lists (rarely change)
- Book details (read frequently, updated rarely)

❌ **Bad use cases:**
- Book availability status (changes frequently)
- Current checkout count (changes frequently)

### Setup

**Add dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

**Enable caching:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
}
```

### Using @Cacheable

```java
@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    @Cacheable(value = "books", key = "#id")
    public BookEntity findById(Long id) {
        log.info("Cache miss: fetching book from database id={}", id);
        return bookRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }

    @CacheEvict(value = "books", key = "#book.bookId")
    public void updateBook(BookEntity book) {
        log.info("Evicting cache for book id={}", book.getBookId());
        bookRepository.save(book);
    }

    @CacheEvict(value = "books", allEntries = true)
    public void createBook(BookRequestDTO request) {
        log.info("Clearing all book caches");
        // Create book logic...
    }

    @Caching(evict = {
        @CacheEvict(value = "books", key = "#id"),
        @CacheEvict(value = "bookSummaries", allEntries = true)
    })
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }
}
```

### Cache Configuration

**Using Caffeine (recommended):**

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("books", "authors", "bookSummaries");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .recordStats());
        return cacheManager;
    }
}
```

**Cache metrics with Actuator:**
```properties
management.metrics.cache.instrument-cache=true
```

---

## Asynchronous Processing

### @Async Methods

For long-running operations that don't need immediate response:

**Enable async:**
```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("bibby-async-");
        executor.initialize();
        return executor;
    }
}
```

**Use async methods:**
```java
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Async
    public void sendCheckoutNotification(String userEmail, String bookTitle) {
        log.info("Sending checkout notification to {} for book {}", userEmail, bookTitle);

        try {
            // Simulate email sending
            Thread.sleep(2000);
            log.info("Notification sent successfully");
        } catch (InterruptedException e) {
            log.error("Failed to send notification", e);
        }
    }

    @Async
    public CompletableFuture<List<Book>> findBooksAsync(String query) {
        List<Book> books = bookRepository.findByTitleContaining(query);
        return CompletableFuture.completedFuture(books);
    }
}
```

**Usage:**
```java
@Service
public class BookService {

    private final NotificationService notificationService;

    public void checkOutBook(Long bookId) {
        BookEntity book = findById(bookId);
        book.checkOut();
        bookRepository.save(book);

        // Send notification asynchronously (doesn't block)
        notificationService.sendCheckoutNotification(
            "user@example.com",
            book.getTitle()
        );

        // Return immediately
    }
}
```

---

## Spring Events

Decouple components using application events.

### Publishing Events

```java
// Event class
public class BookCheckedOutEvent extends ApplicationEvent {

    private final Long bookId;
    private final String bookTitle;
    private final String userEmail;

    public BookCheckedOutEvent(Object source, Long bookId, String bookTitle, String userEmail) {
        super(source);
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.userEmail = userEmail;
    }

    // Getters
}
```

```java
// Publisher
@Service
public class BookService {

    private final ApplicationEventPublisher eventPublisher;

    public void checkOutBook(Long bookId) {
        BookEntity book = findById(bookId);
        book.checkOut();
        bookRepository.save(book);

        // Publish event
        eventPublisher.publishEvent(new BookCheckedOutEvent(
            this,
            book.getBookId(),
            book.getTitle(),
            "user@example.com"
        ));
    }
}
```

```java
// Listener
@Component
public class BookEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookEventListener.class);

    @EventListener
    @Async
    public void handleBookCheckedOut(BookCheckedOutEvent event) {
        log.info("Book checked out: id={}, title={}", event.getBookId(), event.getBookTitle());
        // Send email, update metrics, etc.
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookCreated(BookCreatedEvent event) {
        // Only fires after transaction commits successfully
        log.info("New book added to catalog: {}", event.getBookTitle());
    }
}
```

---

## Spring Security (Basic)

If you want to add authentication to the Web API:

**Add dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**Basic configuration:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/books/**").authenticated()
                .anyRequest().denyAll()
            )
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable());  // Disable for API-only apps

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("admin")
            .password("{noop}password")  // {noop} = no encryption (dev only!)
            .roles("USER")
            .build();

        return new InMemoryUserDetailsManager(user);
    }
}
```

**Production security:**
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

@Bean
public UserDetailsService userDetailsService(UserRepository userRepository) {
    return username -> userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
}
```

---

## Scheduled Tasks

Run tasks periodically:

**Enable scheduling:**
```java
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

**Create scheduled tasks:**
```java
@Component
public class MaintenanceTasks {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceTasks.class);

    private final BookRepository bookRepository;

    // Run every day at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void checkOverdueBooks() {
        log.info("Checking for overdue books...");
        // Find books checked out > 30 days ago
        // Send overdue notifications
    }

    // Run every hour
    @Scheduled(fixedRate = 3600000)  // 1 hour in milliseconds
    public void updateBookStatistics() {
        log.info("Updating book statistics...");
        long totalBooks = bookRepository.count();
        log.info("Total books in catalog: {}", totalBooks);
    }

    // Run 5 minutes after app starts, then every hour
    @Scheduled(initialDelay = 300000, fixedDelay = 3600000)
    public void cleanupOldLogs() {
        log.info("Cleaning up old log entries...");
    }
}
```

---

## Spring Validation (Advanced)

### Custom Validators

```java
// Custom annotation
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ISBNValidator.class)
public @interface ValidISBN {
    String message() default "Invalid ISBN format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

```java
// Validator implementation
public class ISBNValidator implements ConstraintValidator<ValidISBN, String> {

    private static final Pattern ISBN_13_PATTERN = Pattern.compile("^978-\\d{1,5}-\\d{1,7}-\\d{1,7}-\\d$");
    private static final Pattern ISBN_10_PATTERN = Pattern.compile("^\\d{1,5}-\\d{1,7}-\\d{1,7}-[\\dX]$");

    @Override
    public boolean isValid(String isbn, ConstraintValidatorContext context) {
        if (isbn == null || isbn.isBlank()) {
            return false;
        }

        return ISBN_13_PATTERN.matcher(isbn).matches() ||
               ISBN_10_PATTERN.matcher(isbn).matches();
    }
}
```

**Usage:**
```java
public record BookRequestDTO(
    @NotBlank String title,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @ValidISBN String isbn  // ✅ Custom validation
) {}
```

---

## Database Query Optimization

### Query Performance Tips

**1. Use Projections for Read-Only Queries:**
```java
// Instead of loading full entity
List<BookEntity> books = bookRepository.findAll();  // ❌ Loads all fields

// Use projection
public interface BookTitleProjection {
    Long getBookId();
    String getTitle();
}

List<BookTitleProjection> titles = bookRepository.findAllProjectedBy();  // ✅ Only title field
```

**2. Batch Fetching:**
```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=10
```

**3. Query Hints:**
```java
@QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
List<BookEntity> findByGenre(Genre genre);
```

**4. Read-Only Queries:**
```java
@Transactional(readOnly = true)
public Page<Book> findAll(Pageable pageable) {
    // Hibernate optimization: no dirty checking
    return bookRepository.findAll(pageable);
}
```

---

## Profiles for Different Environments

### Advanced Profile Configuration

**application.properties:**
```properties
# Default profile
spring.profiles.active=dev

# Include common configuration
spring.profiles.include=common
```

**application-common.properties:**
```properties
# Shared across all environments
spring.application.name=Bibby
spring.output.ansi.enabled=ALWAYS
```

**application-local.properties:**
```properties
# Local development
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
logging.level.com.penrose.bibby=DEBUG
```

**application-staging.properties:**
```properties
# Staging environment
spring.datasource.url=${STAGING_DATABASE_URL}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
logging.level.com.penrose.bibby=INFO
```

**application-prod.properties:**
```properties
# Production
spring.datasource.url=${PROD_DATABASE_URL}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
logging.level.root=WARN
logging.level.com.penrose.bibby=INFO
```

---

## Performance Monitoring

### Spring Boot Admin

Monitor multiple Spring Boot applications:

**Admin Server (separate project):**
```xml
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>spring-boot-admin-starter-server</artifactId>
    <version>3.1.8</version>
</dependency>
```

```java
@SpringBootApplication
@EnableAdminServer
public class AdminServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
    }
}
```

**Bibby (client):**
```xml
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>spring-boot-admin-starter-client</artifactId>
    <version>3.1.8</version>
</dependency>
```

```properties
spring.boot.admin.client.url=http://localhost:8081
management.endpoints.web.exposure.include=*
```

---

## Advanced Patterns

### 1. Specification Pattern (for dynamic queries)

```java
public class BookSpecifications {

    public static Specification<BookEntity> hasTitle(String title) {
        return (root, query, cb) ->
            title == null ? cb.conjunction() :
            cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<BookEntity> hasGenre(Genre genre) {
        return (root, query, cb) ->
            genre == null ? cb.conjunction() :
            cb.equal(root.get("genre"), genre);
    }

    public static Specification<BookEntity> hasStatus(BookStatus status) {
        return (root, query, cb) ->
            status == null ? cb.conjunction() :
            cb.equal(root.get("status"), status);
    }
}
```

**Usage:**
```java
public interface BookRepository extends JpaRepository<BookEntity, Long>,
                                        JpaSpecificationExecutor<BookEntity> {
}

// In service
public List<BookEntity> searchBooks(String title, Genre genre, BookStatus status) {
    Specification<BookEntity> spec = Specification
        .where(BookSpecifications.hasTitle(title))
        .and(BookSpecifications.hasGenre(genre))
        .and(BookSpecifications.hasStatus(status));

    return bookRepository.findAll(spec);
}
```

### 2. Template Method Pattern

```java
public abstract class AbstractBookProcessor {

    public final void processBook(BookEntity book) {
        validate(book);
        preProcess(book);
        doProcess(book);
        postProcess(book);
    }

    protected void validate(BookEntity book) {
        // Common validation
    }

    protected abstract void preProcess(BookEntity book);
    protected abstract void doProcess(BookEntity book);
    protected abstract void postProcess(BookEntity book);
}

@Component
public class CheckoutBookProcessor extends AbstractBookProcessor {

    @Override
    protected void preProcess(BookEntity book) {
        // Check availability
    }

    @Override
    protected void doProcess(BookEntity book) {
        book.checkOut();
    }

    @Override
    protected void postProcess(BookEntity book) {
        // Send notification
    }
}
```

---

## Action Items (Optional)

These are **enhancements**, not requirements. Implement only if needed:

### When You Need Performance

1. **Add Caching**
   - [ ] Enable caching for book searches
   - [ ] Cache author lists
   - [ ] Monitor cache hit rates

### When You Need Background Processing

2. **Add Async Processing**
   - [ ] Async email notifications
   - [ ] Async report generation

### When You Need Security

3. **Add Spring Security**
   - [ ] Basic authentication for API
   - [ ] Role-based access control
   - [ ] JWT tokens for stateless auth

### When You Need Automation

4. **Add Scheduled Tasks**
   - [ ] Daily statistics update
   - [ ] Weekly overdue book checks

---

## Summary

### Available Technologies

| Technology | Use When | Complexity |
|------------|----------|------------|
| **Caching** | Slow queries, frequent reads | Low |
| **Async** | Long-running tasks | Low |
| **Events** | Decoupling components | Medium |
| **Security** | Authentication needed | Medium |
| **Scheduling** | Periodic tasks | Low |
| **Specifications** | Complex dynamic queries | Medium |

### Recommendations for Bibby

**Add Now:**
- ✅ Caching (easy performance win)
- ✅ Events (cleaner architecture)

**Add Later:**
- ⏳ Async (when you add email notifications)
- ⏳ Security (when exposing API publicly)
- ⏳ Scheduling (when you add overdue books)

**Skip:**
- ❌ Spring Batch (no bulk processing needed)
- ❌ Spring Integration (no enterprise integration)
- ❌ Spring Cloud (single application, not microservices)

---

## Resources

### Documentation
- [Spring Caching](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Spring Async](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [Spring Events](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
- [Spring Security](https://docs.spring.io/spring-security/reference/index.html)

### Books
- **"Spring in Action"** by Craig Walls
- **"Pro Spring 5"** by Iuliana Cosmina

---

## Mentor's Note

Leo, this section covered **optional** advanced features. Don't feel pressured to implement everything!

**My recommendation:**
1. **Do add caching** - Easy performance boost
2. **Do add events** - Cleaner design
3. **Consider async** - Only if you add notifications
4. **Skip the rest** - Unless you have a specific need

Remember: **Premature optimization is the root of all evil.** Add features when you need them, not because they're "cool."

Focus on finishing the implementation roadmap (next section) before adding these enhancements.

---

**Next Section:** Real-World Scenarios & Design Patterns

**Last Updated:** 2025-11-17
**Status:** Complete ✅
