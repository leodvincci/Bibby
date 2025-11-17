# Section 15: Spring Boot Fundamentals & Best Practices
**Spring Principle:** *"Spring Boot makes it easy to create stand-alone, production-grade Spring applications with minimal configuration."*

---

## 🎉 Welcome to Spring Framework Mastery!

You've completed all **14 Clean Code Fundamentals** sections. Now we shift focus from general software craftsmanship to **Spring Boot mastery**.

Over the next 10 sections, you'll learn how to build professional, production-ready Spring Boot applications. We'll continue analyzing **your actual Bibby code**, but now through the lens of Spring best practices.

---

## 📚 Spring Boot Fundamentals

### What is Spring Boot?

**Spring Boot** is an opinionated framework that simplifies Spring application development by:
1. **Auto-configuration** - Automatically configures beans based on classpath dependencies
2. **Starter dependencies** - Pre-packaged dependency bundles
3. **Embedded servers** - No need to deploy to Tomcat/Jetty
4. **Production-ready features** - Health checks, metrics, externalized configuration

**Key insight:** Spring Boot doesn't replace Spring—it makes Spring easier to use.

### The Magic of @SpringBootApplication

**Your application class:**

```java
@CommandScan
@SpringBootApplication
public class BibbyApplication {
    public static void main(String[] args) {
        SpringApplication.run(BibbyApplication.class, args);
    }
}
```

**What @SpringBootApplication does:**

It's a meta-annotation combining three annotations:

```java
@SpringBootConfiguration  // Marks this as configuration class
@EnableAutoConfiguration  // Enables Spring Boot's auto-configuration
@ComponentScan           // Scans for @Component, @Service, @Repository, @Controller
```

**Auto-configuration magic:**
- Spring Boot scans your classpath
- Sees `spring-boot-starter-data-jpa` → configures DataSource, EntityManager, TransactionManager
- Sees `postgresql` → configures PostgreSQL dialect
- Sees `spring-boot-starter-web` → configures embedded Tomcat, DispatcherServlet
- Sees `spring-shell-starter` → configures interactive shell

**All automatic. Zero XML. Zero boilerplate.**

---

## 🔍 Your Spring Boot Configuration Analysis

### Your Dependencies (pom.xml)

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.7</version>  ✅ Latest version!
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.shell</groupId>
        <artifactId>spring-shell-starter</artifactId>  ✅ CLI support
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>  ⚠️ Do you need this for CLI?
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>  ✅ Database access
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>  ✅ PostgreSQL driver
    </dependency>
</dependencies>
```

**What's good:**
✅ Using Spring Boot parent POM (dependency management)
✅ Latest Spring Boot version (3.5.7)
✅ Using starter dependencies (not individual JARs)
✅ Test dependencies properly scoped

**What's questionable:**
⚠️ **spring-boot-starter-web** - Starts embedded Tomcat on port 8080. Do you need REST API for a CLI app?

---

### Issue #1: Unnecessary Web Starter (🟡 MEDIUM)

**Your dependencies include:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**This adds:**
- Embedded Tomcat server
- Spring MVC framework
- Jackson JSON library
- Validation framework

**You have REST controllers:**
```java
@RestController
public class BookController {
    @PostMapping("api/v1/books")
    public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) { ... }
}
```

**Decision point:** Are you building a CLI app or a web API?

**Option 1: Pure CLI (recommended for learning)**
- Remove `spring-boot-starter-web`
- Delete all `@RestController` classes
- Focus on Spring Shell commands

**Option 2: Hybrid CLI + REST API**
- Keep web starter
- Fix REST controller issues (covered in Section 17)
- Run CLI and API simultaneously

**Option 3: Separate modules**
```
bibby-cli/        (Spring Shell)
bibby-api/        (Spring Web)
bibby-core/       (Shared services, entities)
```

**For now, I'll assume Option 2** (both CLI and API) since you have both.

---

### Issue #2: Missing Application Metadata

**Your pom.xml:**
```xml
<description>Bibby</description>  ❌ Generic
<url/>                           ❌ Empty
<licenses>
    <license/>                   ❌ Empty
</licenses>
<developers>
    <developer/>                 ❌ Empty
</developers>
```

**Better:**
```xml
<description>Personal Library Management System - CLI and REST API</description>
<url>https://github.com/leodvincci/Bibby</url>
<licenses>
    <license>
        <name>MIT License</name>
        <url>https://opensource.org/licenses/MIT</url>
    </license>
</licenses>
<developers>
    <developer>
        <id>leodvincci</id>
        <name>Leo D. Penrose</name>
    </developer>
</developers>
```

**Why it matters:**
- Professional appearance
- Maven Central requirements (if you publish)
- Documentation for collaborators

---

## 🏗️ Dependency Injection Best Practices

### Your Current Usage: Mostly Good!

**Good example (BookService.java):**
```java
@Service
public class BookService {
    private final BookRepository bookRepository;  ✅ final field
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository,    ✅ Constructor injection
                       AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }
}
```

**This is perfect!** Constructor injection with final fields.

---

### Issue #3: Field Injection in StartupRunner

**Problem code:**
```java
@Component
public class StartupRunner implements CommandLineRunner {
    @Autowired
    ShelfService shelfService;  ❌ Field injection

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> Bibby is awake...");
    }
}
```

**Why field injection is bad:**
1. Can't make field `final` (mutability)
2. Harder to test (need reflection or Spring test context)
3. Hides dependencies (not visible in constructor signature)
4. Not recommended by Spring team since 4.3

**Fixed:**
```java
@Component
public class StartupRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);
    private final ShelfService shelfService;  ✅ final

    public StartupRunner(ShelfService shelfService) {  ✅ Constructor injection
        this.shelfService = shelfService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Bibby is awake. Checking database...");
        // Use shelfService if needed
    }
}
```

---

### Dependency Injection Patterns

**❌ BAD: Field Injection**
```java
@Service
public class BookService {
    @Autowired
    private BookRepository repository;  // Mutable, hard to test
}
```

**⚠️ OK: Setter Injection** (only for optional dependencies)
```java
@Service
public class BookService {
    private BookRepository repository;

    @Autowired  // Optional dependency
    public void setRepository(BookRepository repository) {
        this.repository = repository;
    }
}
```

**✅ BEST: Constructor Injection**
```java
@Service
public class BookService {
    private final BookRepository repository;  // Immutable

    public BookService(BookRepository repository) {  // Required dependency
        this.repository = repository;
    }
}
```

**Since Spring 4.3:** If a class has only one constructor, `@Autowired` is optional!

---

## 🔧 Application Properties Best Practices

### Your Current Configuration

**application.properties:**
```properties
spring.application.name=Bibby  ✅ Good
spring.shell.interactive.enabled=true  ✅ Good

# ❌ SECURITY ISSUE (covered in Section 10)
spring.datasource.url=jdbc:postgresql://localhost:5332/amigos
spring.datasource.username=amigoscode
spring.datasource.password=password

# ⚠️ Commented code
#spring.jpa.hibernate.ddl-auto=create-drop

# ✅ Error handling config
server.error.include-message=always
server.error.include-binding-errors=never
server.error.include-stacktrace=never

# ❌ LOGGING DISABLED (covered in Section 10)
logging.level.root=OFF
logging.level.org.springframework=OFF
logging.level.org.hibernate=OFF

# ✅ ANSI colors enabled
spring.output.ansi.enabled=ALWAYS
```

---

### Issue #4: No Environment-Specific Configuration

**Problem:** One configuration file for all environments (dev, test, prod).

**Spring Boot solution: Profiles**

Create multiple property files:

**application.properties** (shared config):
```properties
spring.application.name=Bibby
spring.shell.interactive.enabled=true
spring.output.ansi.enabled=ALWAYS

# Use environment variables for credentials
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

**application-dev.properties** (development):
```properties
# Development database
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/bibby_dev}

# Show SQL for debugging
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Auto-create schema (careful!)
spring.jpa.hibernate.ddl-auto=update

# Verbose logging
logging.level.root=INFO
logging.level.com.penrose.bibby=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

**application-test.properties** (testing):
```properties
# In-memory H2 database for tests
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver

# Create-drop schema for each test
spring.jpa.hibernate.ddl-auto=create-drop

# Silent logging (tests run fast)
logging.level.root=WARN
logging.level.com.penrose.bibby=INFO
```

**application-prod.properties** (production):
```properties
# Production database (from environment)
spring.datasource.url=${DB_URL}

# Never show SQL in production
spring.jpa.show-sql=false

# Validate schema only (no auto-DDL!)
spring.jpa.hibernate.ddl-auto=validate

# Conservative logging
logging.level.root=WARN
logging.level.com.penrose.bibby=INFO

# Production error handling
server.error.include-message=never
server.error.include-stacktrace=never
```

**Activate profile:**
```bash
# Development
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production
java -jar bibby.jar --spring.profiles.active=prod
```

---

### Issue #5: Dangerous Hibernate DDL Auto Setting

**Your commented code:**
```properties
#spring.jpa.hibernate.ddl-auto=create-drop
```

**What this does:**
- `create-drop` - Creates schema on startup, DROPS on shutdown
- **DANGER:** All data lost when app stops!

**Options:**
- `create` - Creates schema on startup (overwrites existing)
- `create-drop` - Creates on startup, drops on shutdown (TESTS ONLY)
- `update` - Updates schema (adds columns, never removes)
- `validate` - Validates schema matches entities (PRODUCTION)
- `none` - Does nothing (manual migrations)

**Best practice:**
```properties
# Development: Let Hibernate manage schema
spring.jpa.hibernate.ddl-auto=update

# Production: Use database migrations (Flyway/Liquibase)
spring.jpa.hibernate.ddl-auto=validate
```

**Even better: Use Flyway for migrations**
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

---

## 🎯 REST Controller Issues

### Issue #6: GET Endpoint with @RequestBody

**Your code:**
```java
@GetMapping("api/v1/books")
public void findBookByTitle(@RequestBody BookRequestDTO requestDTO){
    System.out.println("Controller Search For " + requestDTO.title());
    bookService.findBookByTitle(requestDTO.title());
}
```

**Problems:**
1. ❌ **GET requests should NOT have request bodies** (HTTP spec violation)
2. ❌ **Returns `void`** - No response to client
3. ❌ **System.out.println** instead of logging
4. ❌ **No @RequestMapping base path** - Endpoint is `/api/v1/books` (same as POST)

**Fixed:**
```java
@RestController
@RequestMapping("/api/v1/books")  // ✅ Base path
public class BookController {
    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping  // GET /api/v1/books?title=Clean+Code
    public ResponseEntity<BookEntity> findBookByTitle(@RequestParam String title) {
        log.info("Searching for book: {}", title);  // ✅ Logging

        return bookService.findBookByTitle(title)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());  // ✅ 404 if not found
    }

    @PostMapping  // POST /api/v1/books
    public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
        bookService.createNewBook(requestDTO);
        return ResponseEntity.ok("Book added: " + requestDTO.title());
    }
}
```

**Improvements:**
✅ GET uses `@RequestParam` (query string)
✅ Returns actual response (book or 404)
✅ Base path with `@RequestMapping`
✅ Proper HTTP semantics

---

### Issue #7: BookController Has Unused Constructor Parameter

**Your code:**
```java
public BookController(BookService bookService,
                      AuthorRepository authorRepository,
                      BookRepository bookRepository) {  // ❌ bookRepository unused!
    this.bookService = bookService;
    this.authorRepository = authorRepository;
    // bookRepository never stored or used
}
```

**Problems:**
1. Unnecessary dependency injection
2. Controllers shouldn't access repositories directly (bypass service layer)

**Fixed:**
```java
public BookController(BookService bookService) {  // ✅ Only what's needed
    this.bookService = bookService;
}
```

**Rule:** Controllers talk to Services. Services talk to Repositories.

```
Controller → Service → Repository → Database
```

**Never:**
```
Controller → Repository  ❌ Bypasses business logic!
```

---

## 🏛️ Application Structure Best Practices

### Your Current Structure

```
com.penrose.bibby/
├── BibbyApplication.java          ✅ Main class
├── cli/                            ✅ CLI commands
│   ├── BookCommands.java
│   ├── BookcaseCommands.java
│   └── LoadingBar.java
├── library/                        ✅ Feature-based packages
│   ├── book/
│   │   ├── Book.java              ⚠️ Duplicate of BookEntity?
│   │   ├── BookEntity.java
│   │   ├── BookRepository.java
│   │   ├── BookService.java
│   │   ├── BookController.java    ✅ REST API
│   │   └── BookMapper.java
│   ├── author/
│   ├── shelf/
│   └── bookcase/
└── util/
    └── StartupRunner.java
```

**What's good:**
✅ Feature-based packaging (book, author, shelf)
✅ Separate CLI and library layers
✅ Service + Repository pattern

**What could improve:**
⚠️ Duplicate models (`Book.java` and `BookEntity.java`)
⚠️ Controllers, services, repos all in same package (fine for small apps)
⚠️ No separation between CLI-only and API-only code

---

### Recommended Structure (as app grows)

**Option 1: Layer-first within features** (current approach is fine)
```
library/
├── book/
│   ├── domain/
│   │   └── Book.java              (domain model)
│   ├── api/
│   │   ├── BookController.java    (REST API)
│   │   └── BookRequestDTO.java
│   ├── cli/
│   │   └── BookCommands.java      (CLI commands)
│   ├── service/
│   │   └── BookService.java
│   └── persistence/
│       ├── BookEntity.java        (JPA entity)
│       └── BookRepository.java
```

**Option 2: Keep current structure** (good for small apps)
```
library/
├── book/
│   ├── BookEntity.java
│   ├── BookRepository.java
│   ├── BookService.java
│   ├── BookController.java
│   └── BookRequestDTO.java
```

**Your current structure is perfectly fine** for a small application. Don't over-engineer.

---

## 🔐 Component Scanning

### How Spring Finds Your Beans

**@SpringBootApplication on `BibbyApplication`:**
- Scans `com.penrose.bibby` package
- Scans all sub-packages (`library.book`, `cli`, etc.)
- Finds all `@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`

**Your components:**
```
@Component    - BookCommands, BookcaseCommands, LoadingBar, StartupRunner
@Service      - BookService, ShelfService, BookcaseService, AuthorService
@Repository   - BookRepository (interface, Spring Data creates impl)
@RestController - BookController, ShelfController, etc.
```

**All auto-discovered. No XML configuration needed.**

---

### Issue #8: @CommandScan Annotation Redundant?

**Your application:**
```java
@CommandScan
@SpringBootApplication
public class BibbyApplication { ... }
```

**What @CommandScan does:**
Scans for Spring Shell `@Command` annotations.

**Is it needed?**
- `@CommandScan` without arguments scans current package + sub-packages
- `@SpringBootApplication` already does this via `@ComponentScan`

**In most cases, @CommandScan is redundant** if your commands are in the same package tree.

**However:** Keep it if you have commands in external JARs or different packages.

---

## 🛠️ Configuration Classes (What You're Missing)

### Issue #9: No Custom @Configuration Classes

**Current state:** Relying 100% on Spring Boot auto-configuration.

**This is fine for simple apps**, but professional apps often need custom configuration.

**Example custom configurations you might add:**

**Database Configuration:**
```java
@Configuration
public class DatabaseConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        return config;
    }

    @Bean
    public DataSource dataSource(HikariConfig hikariConfig) {
        return new HikariDataSource(hikariConfig);
    }
}
```

**Async Configuration:**
```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
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

**OpenAPI Documentation:**
```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Bibby Library API")
                .version("1.0.0")
                .description("Personal library management system")
            );
    }
}
```

**For now, auto-configuration is fine.** Add custom configs when you need them.

---

## ✅ Your Action Items

### 🔴 **Priority 1: Fix StartupRunner Field Injection** (10 minutes)

**Action:**
1. Change `@Autowired ShelfService` to constructor injection
2. Make field `final`
3. Replace `System.out.println` with `log.info()`

**Files:**
- Update: `util/StartupRunner.java`

---

### 🔴 **Priority 2: Create Environment Profiles** (1 hour)

**Action:**
1. Create `application-dev.properties` with DEBUG logging
2. Create `application-test.properties` with H2 database
3. Create `application-prod.properties` with minimal logging
4. Update `application.properties` to use environment variables for DB credentials

**Files:**
- Create: `src/main/resources/application-dev.properties`
- Create: `src/main/resources/application-test.properties`
- Create: `src/main/resources/application-prod.properties`
- Update: `src/main/resources/application.properties`

---

### 🟡 **Priority 3: Fix BookController Issues** (30 minutes)

**Action:**
1. Add `@RequestMapping("/api/v1/books")` base path
2. Change GET endpoint to use `@RequestParam` instead of `@RequestBody`
3. Return `ResponseEntity<BookEntity>` instead of `void`
4. Remove unused `BookRepository` constructor parameter
5. Add SLF4J logger

**Files:**
- Update: `library/book/BookController.java`

---

### 🟡 **Priority 4: Add POM Metadata** (15 minutes)

**Action:**
Fill in description, URL, license, and developer information in `pom.xml`.

**Files:**
- Update: `pom.xml`

---

### 🟢 **Priority 5: Decide on CLI vs API Strategy** (Planning)

**Action:**
Decide on one of three approaches:
1. **Pure CLI** - Remove web starter, delete controllers
2. **Hybrid** - Keep both, fix REST controllers properly
3. **Separate modules** - Split into bibby-cli, bibby-api, bibby-core

**Recommendation:** Start with **Hybrid** for learning both Spring Shell and Spring Web.

---

## 📚 Further Study

### Official Documentation
- **Spring Boot Reference Guide**
  https://docs.spring.io/spring-boot/docs/current/reference/html/

- **Spring Boot Starters**
  https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.build-systems.starters

### Books
- **"Spring Boot in Action"** - Craig Walls
  *Excellent introduction to Spring Boot fundamentals.*

- **"Spring Boot Up and Running"** - Mark Heckler
  *Modern Spring Boot practices and patterns.*

### Spring Boot Guides
- **Building a RESTful Web Service**
  https://spring.io/guides/gs/rest-service/

- **Accessing Data with JPA**
  https://spring.io/guides/gs/accessing-data-jpa/

---

## 💭 Mentor's Final Thoughts

Leo, welcome to **Spring Framework Mastery**! This is where Clean Code principles meet professional Spring Boot development.

Your Spring Boot foundation is **solid**:
- ✅ Latest Spring Boot version (3.5.7)
- ✅ Using starter dependencies correctly
- ✅ Constructor injection in most places
- ✅ Sensible package structure

But there's room for improvement:
- 🔴 Fix field injection in StartupRunner
- 🔴 Create environment-specific profiles
- 🔴 Fix REST controller HTTP semantics
- 🟡 Add proper metadata to POM

**The most important lesson from this section:**

**Spring Boot is "opinionated" - it makes intelligent defaults so you can focus on business logic.** But "opinionated" doesn't mean "one size fits all." You need to understand:
- When to accept defaults (database connection pool)
- When to customize (logging levels per environment)
- When to override completely (custom security config)

**Over the next 9 sections**, you'll master:
- Spring Shell CLI design patterns
- RESTful API best practices
- Spring Data JPA optimization
- Production-ready configuration
- Testing strategies
- Deployment patterns

You've learned **Clean Code**. Now you'll learn **Clean Spring Boot**.

The journey continues. Let's build something professional.

— Your Mentor

---

**Next:** Section 16 - Spring Shell Mastery
**Previous:** [Section 14 - Smells and Heuristics](./14-smells-heuristics.md)
**Home:** [Master Index](./00-master-index.md)
