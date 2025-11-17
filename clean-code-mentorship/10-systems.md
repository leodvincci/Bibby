# Section 10: Systems
**Clean Code Principle:** *"Clean architecture begins at the system level. Separate concerns, isolate dependencies, and keep your system maintainable."*

---

## 📚 Principles Overview

### Why System Architecture Matters

Uncle Bob says: **"The architecture of a system is defined by the boundaries it creates and the dependencies it manages."**

You can have perfect functions, perfect classes, and perfect names—but if your system architecture is a mess, your codebase will still be unmaintainable.

System-level design answers questions like:
- How do components communicate?
- Where do cross-cutting concerns live (logging, security, validation)?
- How do you separate business logic from frameworks?
- How do you configure different environments (dev, test, prod)?

### Separation of Concerns at the System Level

At the system level, **separation of concerns** means organizing your code so that:
1. **Business logic** doesn't depend on frameworks
2. **Infrastructure** (databases, web frameworks) is pluggable
3. **Cross-cutting concerns** (logging, security) are centralized
4. **Configuration** is externalized and environment-specific

```
❌ BAD Architecture:
┌─────────────────────────────────┐
│   One Big Monolithic Class      │
│  - Business Logic               │
│  - Database Code                │
│  - Web Framework Code           │
│  - Logging                      │
│  - Configuration                │
└─────────────────────────────────┘

✅ GOOD Architecture:
┌──────────────┐
│ Presentation │  ← CLI/Web Controllers
└──────┬───────┘
       │
┌──────▼───────┐
│   Business   │  ← Pure business logic
│     Logic    │
└──────┬───────┘
       │
┌──────▼───────┐
│ Data Access  │  ← Repositories, Entities
└──────────────┘
```

---

## 🏗️ Layered Architecture

Your Bibby application follows a **layered architecture**:

```
┌─────────────────────────────────────┐
│         CLI Layer (Commands)        │  ← BookCommands, BookcaseCommands
│   @Command, ComponentFlow           │
└────────────────┬────────────────────┘
                 │ calls
┌────────────────▼────────────────────┐
│      Service Layer (Business)       │  ← BookService, BookcaseService
│   @Service, @Transactional          │
└────────────────┬────────────────────┘
                 │ calls
┌────────────────▼────────────────────┐
│    Repository Layer (Data Access)   │  ← BookRepository, ShelfRepository
│   @Repository, Spring Data JPA      │
└────────────────┬────────────────────┘
                 │ queries
┌────────────────▼────────────────────┐
│          Database (PostgreSQL)      │
└─────────────────────────────────────┘
```

**Layer Rules:**
1. **Upper layers** can call **lower layers** (Commands → Services → Repositories)
2. **Lower layers** NEVER call **upper layers** (Repositories should NOT know about Commands)
3. Each layer has a **specific responsibility**

---

## 🔧 Dependency Injection (How Spring Boot Wires Your System)

**Dependency Injection (DI)** is the foundation of clean system architecture.

### What is Dependency Injection?

**Bad (Hard-Coded Dependencies):**
```java
public class BookService {
    private BookRepository repository;

    public BookService() {
        this.repository = new PostgreSQLBookRepository();  // ❌ Hard-coded!
    }
}
```

**Problems:**
- Can't swap out the database (tightly coupled to PostgreSQL)
- Can't test with a mock repository
- Violates Dependency Inversion Principle (depends on concretion)

**Good (Dependency Injection):**
```java
@Service
public class BookService {
    private final BookRepository repository;

    public BookService(BookRepository repository) {  // ✅ Injected
        this.repository = repository;
    }
}
```

**Spring Boot automatically:**
1. Creates a `BookRepository` bean (from your `@Repository` interface)
2. Creates a `BookService` bean
3. Injects the repository into the service
4. Manages the lifecycle of both

**This is POWERFUL** because:
- You can swap implementations (PostgreSQL → MySQL → InMemory)
- You can inject mocks for testing
- Your code follows Dependency Inversion Principle

---

## 🔄 Spring Boot Application Lifecycle

Understanding the lifecycle helps you architect better systems:

```
1. Spring Boot Starts
   ↓
2. Component Scanning (@ComponentScan, @SpringBootApplication)
   - Finds all @Component, @Service, @Repository, @Controller classes
   ↓
3. Bean Creation
   - Creates instances of all components
   - Resolves dependencies via constructor injection
   ↓
4. Bean Wiring
   - Injects dependencies into constructors/@Autowired fields
   ↓
5. Post-Construction (@PostConstruct methods run)
   ↓
6. CommandLineRunner.run() executes
   ↓
7. Application Ready
   ↓
8. Shutdown (@PreDestroy methods run)
```

---

## 🔍 Your Code Analysis

### System Architecture Overview

**Your Package Structure:**
```
com.penrose.bibby/
├── BibbyApplication.java         # Entry point
├── cli/                          # Presentation layer
│   ├── BookCommands.java
│   ├── BookcaseCommands.java
│   └── ...
├── library/                      # Business + Data layers
│   ├── book/
│   │   ├── BookEntity.java       # Data model
│   │   ├── BookRepository.java   # Data access
│   │   └── BookService.java      # Business logic
│   ├── author/
│   ├── shelf/
│   ├── bookcase/
│   └── catalog/                  # ❌ Empty package
└── util/
    └── StartupRunner.java        # Initialization
```

**What's Good:**
✅ Clear layering (CLI → Service → Repository)
✅ Using Spring Boot's dependency injection
✅ Separate packages for different domains (book, author, shelf)

**What Needs Work:**
❌ No separation between entities, repositories, and services (all in same packages)
❌ No configuration classes (relying entirely on autoconfiguration)
❌ Hardcoded credentials in application.properties
❌ Cross-cutting concerns (logging, formatting) scattered throughout
❌ Empty/dead code (CatalogEntity)

---

### Issue #1: Hardcoded Database Credentials (🔴 SECURITY RISK)

**Location:** `src/main/resources/application.properties:3-5`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5332/amigos
spring.datasource.username=amigoscode  # ❌ HARDCODED
spring.datasource.password=password    # ❌ HARDCODED, IN VERSION CONTROL!
```

**Problems:**
1. **Security Risk:** Password is in version control (Git history)
2. **No Environment Separation:** Same credentials for dev/test/prod
3. **Not Portable:** Other developers can't run your app without editing this file
4. **Violates 12-Factor App principles**

**What Could Go Wrong:**
- Push to GitHub → Password exposed publicly
- Prod database uses same password → Compromised
- Can't use CI/CD pipelines (need different credentials per environment)

---

### Issue #2: Logging Completely Disabled

**Location:** `src/main/resources/application.properties:17-19`

```properties
logging.level.root=OFF           # ❌ ALL logging disabled!
logging.level.org.springframework=OFF
logging.level.org.hibernate=OFF
```

**Problems:**
1. **No Production Debugging:** When errors occur, you have zero visibility
2. **No Audit Trail:** Can't track who did what and when
3. **No Performance Monitoring:** Can't see slow queries or bottlenecks
4. **Hides Errors:** Exceptions might be swallowed silently

**Logging is a Cross-Cutting Concern** that should be configured properly, not disabled.

---

### Issue #3: Field Injection in StartupRunner

**Location:** `src/main/java/com/penrose/bibby/util/StartupRunner.java:9-10`

```java
@Component
public class StartupRunner implements CommandLineRunner {
    @Autowired
    ShelfService shelfService;  // ❌ Field injection

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> Bibby is awake. Seeding data now...");
        // TODO: Add data seeding logic here if needed
    }
}
```

**Problems:**
1. **Can't make field `final`** (immutability lost)
2. **Harder to test** (need to use reflection to inject mocks)
3. **Hides dependencies** (can't see what's required by looking at constructor)
4. **Not recommended by Spring team** (since Spring 4.3)

**Also:**
- Unused `shelfService` dependency (why inject it if you don't use it?)
- `System.out.println` instead of proper logging
- TODO comment suggests this class might not be needed

---

### Issue #4: Empty CatalogEntity (Dead Code)

**Location:** `src/main/java/com/penrose/bibby/library/catalog/CatalogEntity.java`

```java
package com.penrose.bibby.library.catalog;

public class CatalogEntity {
    // ❌ Completely empty class!
}
```

**Problems:**
1. **Dead code** pollutes the codebase
2. **Confusing** to other developers (what was this for?)
3. **Unclear intent** (planned feature? Abandoned feature?)

**If you're not using it, delete it.** Git history preserves it if you need it later.

---

### Issue #5: No Configuration Classes

Your application relies **entirely** on Spring Boot autoconfiguration. While that's fine for small projects, professional applications typically have:

**Missing:**
- `@Configuration` classes for custom beans
- Profile-specific configurations (`@Profile("dev")`, `@Profile("prod")`)
- Custom component scanning rules
- Bean validation configuration
- Data source configuration with connection pooling

**Example of what you might need:**
```java
@Configuration
public class DatabaseConfig {
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        return new HikariConfig();
    }

    @Bean
    public DataSource dataSource(HikariConfig config) {
        return new HikariDataSource(config);
    }
}
```

---

### Issue #6: No Environment Profiles

You have **one** `application.properties` file for all environments.

**Professional setup:**
```
application.properties           # Shared config
application-dev.properties       # Development-specific
application-test.properties      # Test-specific
application-prod.properties      # Production-specific
```

**Activate with:** `--spring.profiles.active=prod`

---

### Issue #7: Package Organization Could Be Improved

**Current (Feature-based):**
```
library/
├── book/
│   ├── BookEntity.java
│   ├── BookRepository.java
│   └── BookService.java
├── author/
│   ├── AuthorEntity.java
│   ├── AuthorRepository.java
│   └── AuthorService.java
```

This is **good** for small projects, but as you scale, consider:

**Alternative (Layer-based for larger projects):**
```
domain/
├── Book.java
├── Author.java
application/
├── BookService.java
├── AuthorService.java
infrastructure/
├── persistence/
│   ├── BookEntity.java
│   ├── BookRepository.java
```

**Or stick with feature-based** (what you have) and add sub-packages:
```
library/
├── book/
│   ├── domain/
│   │   └── Book.java
│   ├── application/
│   │   └── BookService.java
│   ├── infrastructure/
│   │   ├── BookEntity.java
│   │   └── BookRepository.java
│   └── api/
│       └── BookCommands.java
```

**For now, your structure is fine.** But be aware of alternatives as your system grows.

---

## 🛠️ Refactoring Examples

### Example 1: Externalize Database Credentials

**BEFORE (in application.properties):**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5332/amigos
spring.datasource.username=amigoscode
spring.datasource.password=password
```

**AFTER:**

**Step 1:** Create `.env` file (add to `.gitignore`!)
```bash
# .env (NOT committed to Git)
DB_URL=jdbc:postgresql://localhost:5332/amigos
DB_USERNAME=amigoscode
DB_PASSWORD=password
```

**Step 2:** Update `application.properties` to use environment variables
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

**Step 3:** Load environment variables when running

**IntelliJ:** Run Configurations → Environment Variables → Load from `.env`
**Terminal:** `export $(cat .env | xargs) && mvn spring-boot:run`
**Docker:** Use `env_file` in docker-compose.yml

**Add to `.gitignore`:**
```
.env
.env.local
*.env
```

**Benefits:**
- ✅ Credentials not in version control
- ✅ Each developer can have their own `.env`
- ✅ Different credentials per environment (dev, prod)
- ✅ Follows 12-Factor App principles

---

### Example 2: Fix StartupRunner to Use Constructor Injection

**BEFORE:**
```java
@Component
public class StartupRunner implements CommandLineRunner {
    @Autowired
    ShelfService shelfService;  // ❌ Field injection

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> Bibby is awake. Seeding data now...");
    }
}
```

**AFTER:**
```java
@Component
public class StartupRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);
    private final ShelfService shelfService;  // ✅ final field

    public StartupRunner(ShelfService shelfService) {  // ✅ Constructor injection
        this.shelfService = shelfService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Bibby is awake. Seeding data now...");  // ✅ Proper logging

        // Only seed if database is empty
        if (shelfService.getAllShelves(1L).isEmpty()) {
            seedInitialData();
        }
    }

    private void seedInitialData() {
        log.info("Database is empty. Seeding initial data...");
        // Actual seeding logic here
    }
}
```

**Benefits:**
- ✅ `final` field (immutable)
- ✅ Easy to test (just pass mock in constructor)
- ✅ Explicit dependencies
- ✅ Proper logging instead of `System.out.println`

---

### Example 3: Create Profile-Specific Configurations

**Create:** `src/main/resources/application-dev.properties`
```properties
# Development configuration
logging.level.root=DEBUG
logging.level.com.penrose.bibby=DEBUG
logging.level.org.springframework=INFO
logging.level.org.hibernate.SQL=DEBUG

spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

**Create:** `src/main/resources/application-prod.properties`
```properties
# Production configuration
logging.level.root=WARN
logging.level.com.penrose.bibby=INFO
logging.level.org.springframework=WARN

spring.jpa.show-sql=false
```

**Update:** `application.properties` (shared config)
```properties
spring.application.name=Bibby
spring.shell.interactive.enabled=true

# Database (credentials from environment variables)
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5332/amigos}
spring.datasource.username=${DB_USERNAME:amigoscode}
spring.datasource.password=${DB_PASSWORD:password}

# Sensible defaults
server.error.include-message=always
spring.output.ansi.enabled=ALWAYS
```

**Run with profile:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

### Example 4: Enable Proper Logging

**Remove from application.properties:**
```properties
# ❌ DELETE THESE LINES
logging.level.root=OFF
logging.level.org.springframework=OFF
logging.level.org.hibernate=OFF
```

**Add to application-dev.properties:**
```properties
logging.level.root=INFO
logging.level.com.penrose.bibby=DEBUG
logging.level.org.springframework.boot=INFO
logging.level.org.hibernate.SQL=DEBUG

# Log to file and console
logging.file.name=logs/bibby-dev.log
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

**Update services to use logging:**
```java
@Service
public class BookService {
    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    public Optional<BookEntity> findBookByTitle(String title) {
        log.debug("Searching for book with title: {}", title);
        Optional<BookEntity> result = Optional.ofNullable(
            bookRepository.findByTitleIgnoreCase(title)
        );

        if (result.isEmpty()) {
            log.warn("Book not found: {}", title);
        } else {
            log.info("Found book: {}", title);
        }

        return result;
    }
}
```

---

## 🎯 System Design Best Practices

### Practice 1: The Humble Object Pattern

**Keep framework code separate from business logic:**

```java
// ❌ BAD: Business logic mixed with Spring Shell code
@Command(command = "checkout")
public void checkOutBook() throws InterruptedException {
    ComponentFlow flow = componentFlowBuilder.clone().reset().build();
    ComponentFlow.ComponentFlowResult result = flow.run();
    String title = result.getContext().get("bookTitle");

    // Business logic embedded in command
    BookEntity book = bookRepository.findByTitle(title);
    if (book != null && !book.getStatus().equals("CHECKED_OUT")) {
        book.setStatus("CHECKED_OUT");
        book.setCheckoutDate(LocalDateTime.now());
        bookRepository.save(book);
        System.out.println("Checked out!");
    }
}

// ✅ GOOD: Framework code delegates to pure business logic
@Command(command = "checkout")
public void checkOutBook() throws InterruptedException {
    String title = promptService.promptText("Enter book title:");

    try {
        bookService.checkOutBook(title);  // ✅ Business logic in service
        System.out.println("Checked out!");
    } catch (BookNotFoundException e) {
        System.err.println("Book not found: " + title);
    }
}
```

**Benefits:**
- Business logic is testable without Spring Shell
- Command class is just a thin adapter
- Can swap CLI for Web API easily

---

### Practice 2: Use Constructor Injection (Not Field Injection)

```java
// ❌ BAD: Field injection
@Service
public class BookService {
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;
}

// ✅ GOOD: Constructor injection
@Service
public class BookService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository,
                       AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }
}
```

**Since Spring 4.3:** If a class has only one constructor, `@Autowired` is optional!

---

### Practice 3: Validate Configuration on Startup

```java
@Configuration
public class DatabaseConfig {

    @PostConstruct
    public void validateConfiguration() {
        String dbUrl = environment.getProperty("spring.datasource.url");

        if (dbUrl == null || dbUrl.isEmpty()) {
            throw new IllegalStateException(
                "Database URL not configured! Set DB_URL environment variable."
            );
        }

        log.info("Database configured: {}", dbUrl);
    }
}
```

**Benefit:** Fail fast at startup instead of at runtime.

---

## 🚨 Your Critical Issues & Solutions

### Issue Summary Table

| Issue | Severity | Risk | Fix Time |
|-------|----------|------|----------|
| Hardcoded credentials in Git | 🔴 CRITICAL | Security breach | 30 min |
| Logging completely disabled | 🔴 HIGH | No production debugging | 15 min |
| Field injection in StartupRunner | 🟡 MEDIUM | Testing difficulty | 10 min |
| No environment profiles | 🟡 MEDIUM | Can't separate dev/prod | 1 hour |
| Empty CatalogEntity | 🟢 LOW | Dead code clutter | 5 min |
| No configuration classes | 🟢 LOW | Limited customization | Future |

---

## ✅ Your Action Items

### 🔴 **Priority 1: Externalize Database Credentials** (30 minutes)

**Action:**
1. Create `.env` file with DB credentials
2. Add `.env` to `.gitignore`
3. Update `application.properties` to use `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`
4. **CRITICAL:** Remove hardcoded credentials from Git history (or rotate passwords)

**Files:**
- Create: `.env`
- Update: `.gitignore`
- Update: `src/main/resources/application.properties`

**Command to remove from Git history (if committed):**
```bash
# WARNING: Rewrites Git history! Coordinate with team first.
git filter-branch --force --index-filter \
  'git rm --cached --ignore-unmatch src/main/resources/application.properties' \
  --prune-empty --tag-name-filter cat -- --all
```

**Better:** Just rotate the passwords on the database server.

---

### 🔴 **Priority 2: Enable Logging** (15 minutes)

**Action:**
1. Remove `logging.level.root=OFF` and related lines from `application.properties`
2. Add sensible logging configuration
3. Create `application-dev.properties` with DEBUG logging
4. Replace `System.out.println` with `log.info()` in services

**Files:**
- Update: `src/main/resources/application.properties`
- Create: `src/main/resources/application-dev.properties`
- Update: All service classes (add `Logger`)

---

### 🟡 **Priority 3: Fix Field Injection** (10 minutes)

**Action:**
1. Change `@Autowired ShelfService shelfService` to constructor injection
2. Make field `final`
3. Replace `System.out.println` with `log.info()`

**Files:**
- Update: `src/main/java/com/penrose/bibby/util/StartupRunner.java`

---

### 🟡 **Priority 4: Create Environment Profiles** (1 hour)

**Action:**
1. Create `application-dev.properties` (DEBUG logging, SQL logging enabled)
2. Create `application-test.properties` (H2 in-memory database for tests)
3. Create `application-prod.properties` (WARN logging, no SQL logging)
4. Document how to run with profiles in README

**Files:**
- Create: `src/main/resources/application-dev.properties`
- Create: `src/main/resources/application-test.properties`
- Create: `src/main/resources/application-prod.properties`

---

### 🟢 **Priority 5: Delete Dead Code** (5 minutes)

**Action:**
Delete `CatalogEntity.java` and the entire `catalog` package if it's not used.

**Files:**
- Delete: `src/main/java/com/penrose/bibby/library/catalog/CatalogEntity.java`
- Delete: `src/main/java/com/penrose/bibby/library/catalog/` (if empty)

---

## 📊 System Health Scorecard

| Area | Current Grade | Target |
|------|---------------|--------|
| Dependency Injection | ✅ A- | A |
| Layering | ✅ B+ | A |
| Configuration Management | ⚠️ D | B+ |
| Logging | ⚠️ F | B+ |
| Security (Credentials) | ⚠️ F | A |
| Environment Separation | ⚠️ D | B+ |
| Dead Code | ⚠️ C | A |

**Overall System Architecture Grade: C+**

Good foundation (thanks to Spring Boot), but critical issues in configuration and security.

---

## 🎓 Key Takeaways

1. **System architecture is about boundaries and dependencies.** Keep concerns separated.

2. **Dependency Injection is the foundation of clean architecture.** Spring Boot makes this easy.

3. **NEVER commit credentials to version control.** Use environment variables.

4. **Logging is not optional.** It's critical for production debugging and monitoring.

5. **Use constructor injection, not field injection.** Makes code testable and explicit.

6. **Profile-specific configuration enables dev/test/prod separation.** Don't use one config for all environments.

7. **The Humble Object Pattern keeps business logic pure.** Framework code should be thin adapters.

8. **Validate configuration at startup.** Fail fast if environment is misconfigured.

9. **Dead code should be deleted, not commented out.** Git preserves history.

10. **Spring Boot's autoconfiguration is powerful, but understand what it's doing.** Don't treat it as magic.

---

## 📚 Further Study

### Books
- **"Clean Architecture"** - Robert C. Martin
  *The definitive guide to system-level design.*

- **"Building Microservices"** - Sam Newman
  *Learn about system boundaries and service design.*

- **"The 12-Factor App"**
  https://12factor.net/
  *Industry best practices for modern applications.*

### Spring Boot Resources
- **Spring Boot Reference Documentation**
  https://docs.spring.io/spring-boot/docs/current/reference/html/

- **Baeldung: Spring Boot Tutorials**
  https://www.baeldung.com/spring-boot

### Security
- **OWASP: Secrets Management**
  https://owasp.org/www-community/controls/Secrets_Management

---

## 💭 Mentor's Final Thoughts

Leo, I need to be blunt: **you have hardcoded database credentials in your Git repository.** If you push this to GitHub, those credentials are publicly visible forever (even if you delete them later—Git history preserves them).

**First thing Monday morning:** Remove those credentials and use environment variables.

The good news? Your **system architecture is solid** thanks to Spring Boot. You're using dependency injection correctly (mostly), you have clear layering, and your package structure makes sense.

But turning off ALL logging is like flying blind. In production, when something breaks, you'll have **zero visibility** into what went wrong. That's unacceptable.

These are **easy fixes** (1-2 hours total) that will dramatically improve your system's professionalism:
1. Externalize credentials (30 min)
2. Enable logging (15 min)
3. Fix constructor injection (10 min)
4. Add environment profiles (1 hour)

Do these ASAP. Your future self will thank you when debugging production issues.

You're 32% through the mentorship and making excellent progress. System architecture is the foundation everything else builds on—get this right, and everything else gets easier.

Keep going. You're building professional-grade skills.

— Your Mentor

---

**Next:** Section 11 - Emergence (simple design, emergent behavior)
**Previous:** [Section 9 - Classes](./09-classes.md)
**Home:** [Master Index](./00-master-index.md)
