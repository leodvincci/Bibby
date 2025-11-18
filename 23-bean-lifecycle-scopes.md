# Section 23: Bean Lifecycle & Scopes
## From Creation to Destruction

**Discovery:** Bibby uses ALL default lifecycle settings (no custom hooks)
**Concept:** Bean lifecycle phases, scopes, initialization, destruction
**Time:** 50 min read

---

## What You'll Learn

By the end of this section, you'll understand:

- The complete bean lifecycle (7 phases from creation to destruction)
- Bean scopes (singleton, prototype, request, session, application)
- **CRITICAL:** Bibby uses singleton scope for everything (correct!)
- Initialization callbacks (@PostConstruct, InitializingBean)
- Destruction callbacks (@PreDestroy, DisposableBean)
- Lazy vs eager initialization (@Lazy)
- When to use lifecycle hooks (and when NOT to)
- Bean creation order (repositories → services → controllers)
- Application shutdown and bean cleanup

Every concept is explained using **Bibby's actual default lifecycle behavior**.

---

## Why Bean Lifecycle Matters

Understanding the bean lifecycle answers critical questions:

- **When** is BookService created? (At startup)
- **How** are dependencies injected? (Before initialization)
- **What** happens when Bibby shuts down? (Beans destroyed in reverse order)
- **Can** you run code after injection? (Yes, @PostConstruct)
- **Should** you create a new BookService for each request? (No, singleton is fine)

**Good lifecycle management:**
- Initializes resources correctly (database connections, caches)
- Cleans up properly (close files, release locks)
- Optimizes performance (reuse beans, don't recreate)

**Poor lifecycle management:**
- Resource leaks (unclosed connections)
- Performance issues (creating beans unnecessarily)
- Startup failures (initialization in wrong order)

---

## The 7-Phase Bean Lifecycle

**Complete lifecycle from component scan to destruction:**

```
1. Component Scanning
   └─ Spring finds @Service, @Repository, @Component classes

2. Bean Definition Registration
   └─ Spring registers metadata about each bean

3. Bean Instantiation
   └─ Spring calls constructor: new BookService(bookRepo, authorRepo)

4. Dependency Injection
   └─ Spring sets fields, calls setters (if using field/setter injection)

5. Bean Post-Processing (Pre-Initialization)
   └─ BeanPostProcessor.postProcessBeforeInitialization()

6. Initialization
   ├─ @PostConstruct methods called
   ├─ InitializingBean.afterPropertiesSet() called
   └─ Custom init-method called

7. Bean Post-Processing (Post-Initialization)
   └─ BeanPostProcessor.postProcessAfterInitialization()

   ↓

   BEAN READY FOR USE

   ↓

   APPLICATION RUNNING

   ↓

   SHUTDOWN TRIGGERED (Ctrl+C, system signal)

   ↓

8. Destruction
   ├─ @PreDestroy methods called
   ├─ DisposableBean.destroy() called
   └─ Custom destroy-method called
```

**Bibby uses phases 1-4, 8 (default behavior). No custom initialization hooks.**

---

## Bibby's Bean Lifecycle: Default Behavior

**Discovery:** Bibby has ZERO lifecycle customizations:

```bash
# Search results:
grep -r "@PostConstruct" src/  # No results
grep -r "@PreDestroy" src/      # No results
grep -r "@Lazy" src/            # No results
grep -r "@Scope" src/           # No results
grep -r "InitializingBean" src/ # No results
grep -r "DisposableBean" src/   # No results
```

**What this means:**

✅ All beans use **singleton scope** (default)
✅ All beans are **eagerly initialized** at startup (default)
✅ No custom **initialization** logic (@PostConstruct)
✅ No custom **destruction** logic (@PreDestroy)

**Is this bad?**

**NO! This is perfect for Bibby.** Most applications don't need lifecycle hooks. Bibby's beans:
- Are stateless (no initialization needed)
- Don't manage resources that need cleanup (Spring handles DB connections)
- Benefit from singleton scope (performance, shared state)

---

## Bean Scopes: Singleton vs Others

**Bean scope** determines how many instances Spring creates.

### Singleton Scope (Default)

**One instance per ApplicationContext** (shared by everyone)

```java
@Service  // Default scope = singleton
public class BookService {
    // ONE instance created at startup
    // ALL injections get the SAME object
}
```

**Verification:**

```java
@RestController
public class BookController {
    public BookController(BookService bookService) {
        System.out.println("BookController got: " + bookService);
        // Output: BookService@a1b2c3
    }
}

@Component
public class BookCommands {
    public BookCommands(BookService bookService) {
        System.out.println("BookCommands got: " + bookService);
        // Output: BookService@a1b2c3  ← SAME instance!
    }
}
```

**Benefits:**
- **Performance:** Created once, reused forever
- **Memory efficient:** One object, not millions
- **Shared state:** All components see same instance

**When to use:**
- Stateless services (BookService, AuthorService) ✅
- Repositories (BookRepository) ✅
- Controllers (BookController) ✅
- Utilities (StartupRunner) ✅

**Bibby's services are stateless → singleton is perfect.**

---

### Prototype Scope (New Instance Every Time)

**New instance created every time it's injected**

```java
@Service
@Scope("prototype")  // Or @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PrototypeService {
    // NEW instance every time it's requested
}
```

**Example:**

```java
@RestController
public class BookController {
    public BookController(PrototypeService service) {
        System.out.println("BookController got: " + service);
        // Output: PrototypeService@x1y2z3
    }
}

@Component
public class BookCommands {
    public BookCommands(PrototypeService service) {
        System.out.println("BookCommands got: " + service);
        // Output: PrototypeService@a4b5c6  ← DIFFERENT instance!
    }
}
```

**When to use:**
- Stateful beans (each needs its own state)
- Thread-specific beans
- Temporary objects

**For Bibby:** NOT needed. Services are stateless.

---

### Request Scope (Web Applications)

**One instance per HTTP request**

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestScopedBean {
    // New instance for EACH HTTP request
    // Same instance within a single request
}
```

**Use cases:**
- Store request-specific data
- Track user actions within a request
- Audit logging per request

**For Bibby:** Could be useful for REST API, but not currently needed.

---

### Session Scope (Web Applications)

**One instance per HTTP session**

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ShoppingCart {
    // New instance for EACH user session
    // Same instance for all requests from same user
}
```

**For Bibby:** Not applicable (no session management).

---

### Application Scope (Singleton for Entire Application)

**One instance per ServletContext** (essentially same as singleton for most apps)

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_APPLICATION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AppConfig {
}
```

**For Bibby:** Singleton is sufficient.

---

## Bibby's Scope Strategy: All Singleton

**Every bean in Bibby is singleton:**

```java
@Service  // Singleton (default)
public class BookService { }

@Service  // Singleton (default)
public class AuthorService { }

@RestController  // Singleton (default)
public class BookController { }

@Component  // Singleton (default)
public class BookCommands { }
```

**Why this works:**

1. **Stateless services:** No mutable state in services
2. **Thread-safe:** Spring handles concurrency for stateless beans
3. **Performance:** Beans created once, reused for all requests
4. **Memory efficient:** 17 beans total, not 17 × thousands

**Grade: A+** - Correct scope choice for stateless services.

---

## Bean Creation Order

**Spring creates beans in dependency order:**

**Bibby's dependency graph:**

```
BookCommands
├── BookService
│   ├── BookRepository ← Created FIRST (no dependencies)
│   └── AuthorRepository ← Created FIRST (no dependencies)
├── BookController
│   └── BookService ← Already created
├── BookcaseService
│   ├── BookcaseRepository ← Created FIRST
│   └── ShelfRepository ← Created FIRST
├── ShelfService
│   └── ShelfRepository ← Already created
└── AuthorService
    └── AuthorRepository ← Already created
```

**Creation order:**

```
1. Repositories (no dependencies)
   - BookRepository
   - AuthorRepository
   - BookcaseRepository
   - ShelfRepository
   - CatalogRepository

2. Services (depend on repositories)
   - BookService(bookRepo, authorRepo)
   - AuthorService(authorRepo)
   - BookcaseService(bookcaseRepo, shelfRepo)
   - ShelfService(shelfRepo)
   - CatalogService(catalogRepo)

3. Controllers (depend on services)
   - BookController(bookService)
   - BookCaseController(bookcaseService)
   - ShelfController(shelfService)

4. Commands (depend on multiple services)
   - BookCommands(bookService, bookController, bookcaseService, shelfService, authorService)
   - BookcaseCommands(bookcaseService, shelfService)

5. Utilities
   - StartupRunner(shelfService)
   - CustomPromptProvider()
```

**If dependency missing,** Spring fails at startup (fail-fast).

---

## Initialization Callbacks: @PostConstruct

**@PostConstruct** runs AFTER dependency injection:

```java
import jakarta.annotation.PostConstruct;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
        System.out.println("1. Constructor called");
    }

    @PostConstruct
    public void init() {
        System.out.println("2. @PostConstruct called");
        // Dependencies are NOW available
        // Safe to use bookRepository here
    }
}
```

**Output:**
```
1. Constructor called
2. @PostConstruct called
```

**Use cases:**

✅ **Initialize caches:**
```java
@PostConstruct
public void loadCache() {
    cache = bookRepository.findAll().stream()
        .collect(Collectors.toMap(BookEntity::getId, Function.identity()));
}
```

✅ **Validate configuration:**
```java
@PostConstruct
public void validateConfig() {
    if (maxBooks <= 0) {
        throw new IllegalStateException("maxBooks must be positive");
    }
}
```

✅ **Start background tasks:**
```java
@PostConstruct
public void startScheduler() {
    scheduledExecutor.scheduleAtFixedRate(this::cleanupOldData, 0, 1, TimeUnit.HOURS);
}
```

❌ **Don't use for:**
- Simple initialization (use constructor)
- Business logic (use regular methods)
- Dependency injection (Spring handles this)

---

## Destruction Callbacks: @PreDestroy

**@PreDestroy** runs when ApplicationContext shuts down:

```java
import jakarta.annotation.PreDestroy;

@Service
public class BookService {
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @PreDestroy
    public void cleanup() {
        System.out.println("Shutting down executor...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
```

**Use cases:**

✅ **Close resources:**
```java
@PreDestroy
public void closeConnections() {
    if (connection != null) {
        connection.close();
    }
}
```

✅ **Flush buffers:**
```java
@PreDestroy
public void flushCache() {
    cache.flush();
}
```

✅ **Release locks:**
```java
@PreDestroy
public void releaseLock() {
    lock.unlock();
}
```

❌ **Don't use for:**
- Database connections (Spring closes these automatically)
- File handles managed by try-with-resources
- Stateless beans (nothing to clean up)

---

## Bibby Doesn't Need Lifecycle Hooks

**Why Bibby has no @PostConstruct or @PreDestroy:**

1. **Stateless services:** No caches to load, no state to initialize
2. **Spring-managed resources:** DB connections closed by Spring automatically
3. **No custom resources:** No file handles, network connections, thread pools
4. **Constructor injection sufficient:** Dependencies available immediately

**This is the norm.** Most Spring Boot applications don't need lifecycle hooks.

**When you WOULD need them in Bibby:**

**Add a cache:**
```java
@Service
public class BookService {
    private Map<Long, BookEntity> cache;

    @PostConstruct
    public void loadCache() {
        cache = bookRepository.findAll().stream()
            .collect(Collectors.toMap(BookEntity::getBookId, Function.identity()));
        log.info("Loaded {} books into cache", cache.size());
    }
}
```

**Add scheduled tasks:**
```java
@Component
public class DataCleanupService {
    private ScheduledExecutorService executor;

    @PostConstruct
    public void start() {
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(this::cleanup, 0, 24, TimeUnit.HOURS);
    }

    @PreDestroy
    public void stop() {
        executor.shutdown();
    }

    private void cleanup() {
        // Delete old data
    }
}
```

---

## Lazy Initialization: @Lazy

**By default, all singleton beans are created at startup (eager initialization).**

**@Lazy defers creation until first use:**

```java
@Service
@Lazy  // Don't create until someone needs it
public class ExpensiveService {
    public ExpensiveService() {
        // Expensive initialization
        // Only runs when first injected/requested
    }
}
```

**When to use @Lazy:**

✅ **Expensive initialization that might not be needed:**
```java
@Service
@Lazy
public class ReportGeneratorService {
    // Loads large templates, connects to reporting server
    // Only needed if user requests reports
}
```

✅ **Breaking circular dependencies (last resort):**
```java
@Service
public class A {
    public A(@Lazy B b) { }  // Break cycle: don't inject B immediately
}

@Service
public class B {
    public B(A a) { }
}
```

❌ **Don't use for:**
- Critical services (fail at runtime instead of startup)
- Services with fast initialization
- Most application beans

**For Bibby:** Not needed. All services are lightweight and critical.

---

## Application Startup Sequence

**When you run `mvn spring-boot:run`:**

```
1. JVM starts
   ↓
2. BibbyApplication.main() runs
   ↓
3. SpringApplication.run(BibbyApplication.class)
   ↓
4. Spring Boot initialization
   ├─ Load application.properties
   ├─ Configure logging
   ├─ Enable auto-configuration
   └─ Start component scanning
   ↓
5. Component scanning (Section 22)
   ├─ Scan com.penrose.bibby.**
   ├─ Find 17 beans
   └─ Register bean definitions
   ↓
6. Bean creation (THIS SECTION)
   ├─ Create repositories (no dependencies)
   │  └─ Call constructor
   ├─ Create services (inject repositories)
   │  ├─ Call constructor with dependencies
   │  └─ Call @PostConstruct (if exists - Bibby has none)
   ├─ Create controllers (inject services)
   │  └─ Same process
   └─ Create commands (inject multiple dependencies)
      └─ Same process
   ↓
7. Post-processing
   ├─ AOP proxy creation
   ├─ Transaction management setup
   └─ Final bean initialization
   ↓
8. Start embedded servers
   ├─ Tomcat starts on port 8080 (REST API)
   └─ Spring Shell CLI ready
   ↓
9. Run CommandLineRunner beans
   ├─ StartupRunner.run() executes
   └─ ">>> Bibby is awake. Seeding data now..."
   ↓
10. APPLICATION READY
    └─ Waiting for user input / HTTP requests
```

**Total startup time:** ~3-5 seconds (depends on machine)

---

## Application Shutdown Sequence

**When you press Ctrl+C or kill the process:**

```
1. JVM receives shutdown signal
   ↓
2. Spring registers shutdown hook
   ↓
3. ApplicationContext.close() triggered
   ↓
4. @PreDestroy methods called (reverse creation order)
   ├─ Commands destroyed
   ├─ Controllers destroyed
   ├─ Services destroyed
   └─ Repositories destroyed
   ↓
5. DisposableBean.destroy() called
   ↓
6. Connection pools closed
   ├─ Hibernate session factory closed
   ├─ PostgreSQL connections released
   └─ HikariCP shutdown
   ↓
7. Embedded servers stopped
   ├─ Tomcat shutdown
   └─ Spring Shell shutdown
   ↓
8. Logging shutdown
   ↓
9. JVM exits
```

**Graceful shutdown:** Ensures resources are cleaned up properly.

**Forced shutdown (kill -9):** Resources NOT cleaned up (connections left open).

---

## InitializingBean and DisposableBean Interfaces

**Alternative to @PostConstruct and @PreDestroy:**

```java
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;

@Service
public class BookService implements InitializingBean, DisposableBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // Called after dependency injection (like @PostConstruct)
        System.out.println("BookService initialized");
    }

    @Override
    public void destroy() throws Exception {
        // Called at shutdown (like @PreDestroy)
        System.out.println("BookService destroyed");
    }
}
```

**Comparison:**

| Approach | Pros | Cons |
|----------|------|------|
| **@PostConstruct / @PreDestroy** | Standard Java (jakarta.annotation) | Requires dependency |
| **InitializingBean / DisposableBean** | Spring-specific, type-safe | Couples to Spring |
| **@Bean(initMethod, destroyMethod)** | XML/JavaConfig | Not for component scan |

**Recommendation:** Use `@PostConstruct` and `@PreDestroy` (industry standard).

---

## Bean Dependencies and @DependsOn

**Normally, Spring determines creation order from constructor parameters.**

**@DependsOn forces explicit ordering:**

```java
@Service
@DependsOn("dataSource")  // Create dataSource bean FIRST
public class CacheService {
    // Depends on dataSource being ready
}
```

**Use cases:**
- Bean A doesn't inject B, but needs B to be initialized first
- Initialization order matters for side effects

**For Bibby:** Not needed. Constructor injection handles all dependencies.

---

## Stateful vs Stateless Beans

### Stateless Beans (Bibby's Pattern)

**No mutable state, safe for singleton scope:**

```java
@Service
public class BookService {
    private final BookRepository bookRepository;  // ← Immutable (final)

    // No mutable fields
    // Thread-safe
    // Perfect for singleton
}
```

**Benefits:**
- Thread-safe automatically
- One instance shared by all
- High performance

---

### Stateful Beans (Anti-Pattern for Singletons)

**Has mutable state - DON'T do this with singleton:**

```java
@Service  // ← SINGLETON scope!
public class BadBookService {
    private String currentUser;  // ❌ MUTABLE state in singleton!

    public void processBook(String user, Long bookId) {
        this.currentUser = user;  // 💥 Race condition!
        // Thread 1 sets currentUser = "Alice"
        // Thread 2 sets currentUser = "Bob" (overwrites Alice!)
        // Thread 1 uses currentUser, sees "Bob" (WRONG!)
    }
}
```

**Fix 1: Make it stateless (best):**
```java
@Service
public class GoodBookService {
    public void processBook(String user, Long bookId) {
        // Pass user as parameter, don't store it
    }
}
```

**Fix 2: Use prototype scope (rare):**
```java
@Service
@Scope("prototype")  // New instance per injection
public class StatefulService {
    private String currentUser;  // OK now (each thread gets own instance)
}
```

**Bibby's beans are all stateless → singleton is safe.**

---

## Your Action Items

**Priority 1: Verify Bean Creation Order**

1. Add logging to trace bean creation:

```java
@Service
public class BookService {
    public BookService(BookRepository bookRepository, AuthorRepository authorRepository) {
        System.out.println("Creating BookService");
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }
}
```

2. Add to all services, controllers, commands

3. Run `mvn spring-boot:run` and observe creation order

4. Verify repositories created before services

**Priority 2: Experiment with @PostConstruct**

5. Add initialization hook to BookService:

```java
@PostConstruct
public void init() {
    log.info("BookService initialized with {} book repository", bookRepository.getClass().getSimpleName());
}
```

6. Run application, verify log appears AFTER constructor

7. Remove @PostConstruct (not needed for Bibby)

**Priority 3: Test Singleton Behavior**

8. Add verification to multiple injection points:

```java
@RestController
public class BookController {
    public BookController(BookService bookService) {
        System.out.println("BookController got BookService: " + System.identityHashCode(bookService));
    }
}

@Component
public class BookCommands {
    public BookCommands(BookService bookService) {
        System.out.println("BookCommands got BookService: " + System.identityHashCode(bookService));
    }
}
```

9. Run application, verify same hash code (same instance)

**Priority 4: Understand Shutdown**

10. Add @PreDestroy to a service:

```java
@PreDestroy
public void cleanup() {
    log.info("BookService shutting down");
}
```

11. Run application, press Ctrl+C

12. Verify cleanup log appears before shutdown

13. Remove @PreDestroy (not needed)

**Priority 5: Analyze Dependencies**

14. Draw Bibby's dependency graph:
    ```
    BookCommands → BookService → BookRepository
                → BookController → BookService (reused)
                → BookcaseService → BookcaseRepository
                                  → ShelfRepository
    ```

15. Identify beans with no dependencies (repositories)

16. Identify beans with most dependencies (BookCommands: 6!)

---

## Key Takeaways

**1. Seven-phase bean lifecycle:**
- Component scan → Registration → Instantiation → DI → Initialization → Ready → Destruction

**2. Singleton is default (and best for Bibby):**
- One instance per ApplicationContext
- Shared by all injections
- Perfect for stateless services

**3. Eager initialization by default:**
- All singletons created at startup
- Fail-fast if dependencies missing
- Use @Lazy only when truly needed

**4. Initialization callbacks:**
- @PostConstruct: After DI, before bean ready
- InitializingBean.afterPropertiesSet(): Alternative approach
- Use for: caches, validation, background tasks

**5. Destruction callbacks:**
- @PreDestroy: At application shutdown
- DisposableBean.destroy(): Alternative approach
- Use for: closing resources, flushing caches

**6. Bibby uses all defaults (perfect!):**
- No @PostConstruct (stateless, no initialization needed)
- No @PreDestroy (no resources to clean up)
- No @Lazy (all beans are lightweight)
- No @Scope (singleton is correct)

**7. Bean creation order matters:**
- Spring creates in dependency order
- Repositories first (no dependencies)
- Services next (depend on repositories)
- Controllers/commands last (depend on services)

---

## Bibby's Lifecycle Grade: A+

**What you're doing perfectly:**

✅ All beans are singletons (correct for stateless services)
✅ All beans are eagerly initialized (fail-fast)
✅ No unnecessary lifecycle hooks (keep it simple)
✅ Constructor injection (dependencies available immediately)
✅ Stateless beans (thread-safe, no mutable state)
✅ Clean dependency graph (no circular dependencies)

**No improvements needed!** Bibby uses Spring's defaults correctly.

---

## What's Next

**Section 24: Spring Boot Auto-Configuration**

Now that you understand the bean lifecycle (how beans are created and destroyed), we'll explore **auto-configuration** - the "magic" that makes Spring Boot work without XML configuration.

You'll learn:
- What auto-configuration is and how it works
- How `@EnableAutoConfiguration` finds configuration classes
- Conditional beans (@ConditionalOnClass, @ConditionalOnMissingBean)
- How Bibby gets DataSource, EntityManagerFactory, and TransactionManager automatically
- What Spring Boot auto-configures for you (100+ classes!)
- How to customize auto-configuration
- How to debug "Where did this bean come from?"
- Creating custom auto-configuration

We'll trace how Spring Boot configures Hibernate, PostgreSQL, JPA, and Spring Shell automatically just from your `pom.xml` dependencies.

**Ready when you are!**

---

**Mentor's Note:**

The bean lifecycle is Spring's **orchestration mechanism**. It ensures beans are created in the right order, initialized properly, and cleaned up on shutdown.

Most developers never think about the lifecycle - it "just works." But when you need to:
- Initialize a cache at startup
- Schedule background tasks
- Clean up resources properly
- Debug "when was this bean created?"

You now understand exactly what's happening.

Bibby's lifecycle is textbook perfect: Simple, uses defaults, no unnecessary complexity. The absence of @PostConstruct and @PreDestroy isn't a gap - it's evidence of good design.

Your services are stateless, your dependencies are explicit, and Spring handles the rest.

This is how Spring Boot should be used.

---

**Files Referenced:**
- Bibby has NO lifecycle customizations (searched entire codebase)
- All beans use default singleton scope
- All beans use eager initialization
- Constructor injection provides dependencies immediately

**Beans Analyzed:** All 17 beans (default singleton, eager, no hooks)

**Estimated Reading Time:** 50 minutes
**Estimated Action Items Time:** 30 minutes
**Total Section Time:** 80 minutes

---

*Section 23 Complete - Section 24: Spring Boot Auto-Configuration Next*
