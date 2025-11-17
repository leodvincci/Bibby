# Section 19: Spring Boot + Spring Shell Architecture
## Clean Code + Spring Framework Mentorship

**Focus:** Dual-interface architecture, layer separation, dependency injection, and configuration management

**Estimated Time:** 3-4 hours to read and understand; 10-14 hours to implement recommendations

---

## Overview

Your Bibby application is **architecturally unique**: it's both a CLI application (Spring Shell) AND a REST API (Spring Web) in one codebase. This is actually an excellent design choice that demonstrates real-world architectural thinking.

However, your current implementation has **layer violations** and **architectural inconsistencies** that make the code harder to maintain and test.

This section will teach you how to properly architect a dual-interface Spring Boot application.

---

## Your Current Architecture

### Application Structure

```
bibby/
├── BibbyApplication.java          # @SpringBootApplication + @CommandScan
├── cli/                           # CLI layer (Spring Shell)
│   ├── BookCommands.java          # 594 lines! 🚨
│   ├── BookcaseCommands.java
│   └── CustomPromptProvider.java
├── library/                       # Domain layer
│   ├── book/
│   │   ├── Book.java              # Domain model
│   │   ├── BookEntity.java        # JPA entity
│   │   ├── BookRepository.java
│   │   ├── BookService.java
│   │   ├── BookController.java    # REST controller
│   │   └── BookRequestDTO.java
│   ├── author/
│   ├── shelf/
│   └── bookcase/
└── util/
    └── StartupRunner.java         # CommandLineRunner
```

### Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Shell (CLI) -->
    <dependency>
        <groupId>org.springframework.shell</groupId>
        <artifactId>spring-shell-starter</artifactId>
    </dependency>

    <!-- Spring Web (REST API) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Data JPA (Database) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
</dependencies>
```

**What this means:**
- ✅ Your app starts a web server (port 8080)
- ✅ Your app starts an interactive shell
- ✅ Both interfaces share the same services/database
- ⚠️ Both interfaces run simultaneously (may not want this)

---

## Critical Architectural Issues

### 🚨 SEVERITY: CRITICAL

#### 1. **Monolithic Command Classes** ⚠️ SRP VIOLATION
**Location:** BookCommands.java (594 lines)

**The Problem:**
One class handles:
- Book creation
- Book searching
- Book checkout
- Book check-in
- UI formatting
- Input validation
- Flow navigation

This violates the Single Responsibility Principle.

**Symptoms:**
```java
@Component
@Command(command = "book", group = "Book Commands")
public class BookCommands extends AbstractShellComponent {

    // Depends on EVERY service in the system
    private final BookService bookService;
    private final AuthorService authorService;
    private final ShelfService shelfService;
    private final BookcaseService bookcaseService;
    private final BookController bookController;  // ⚠️ Layer violation!
    private final ComponentFlow.Builder componentFlowBuilder;

    // 20+ methods doing everything
    public void addBook() { }
    public void searchBook() { }
    public void checkoutBook() { }
    public void listBooks() { }
    // ... 16 more methods
}
```

**Correct Architecture:**

Split into focused command classes:

```java
// Book management commands
@Command(command = "book", group = "Book Management")
public class BookManagementCommands {
    private final BookFacade bookFacade;  // Facade pattern

    @Command(command = "add")
    public void addBook() { }

    @Command(command = "search")
    public void searchBook() { }
}

// Checkout commands
@Command(command = "checkout", group = "Circulation")
public class CirculationCommands {
    private final CirculationFacade circulationFacade;

    @Command(command = "out")
    public void checkoutBook() { }

    @Command(command = "in")
    public void checkinBook() { }
}

// Browse/navigation commands
@Command(command = "browse", group = "Navigation")
public class BrowseCommands {
    private final BrowseFacade browseFacade;

    @Command(command = "books")
    public void browseBooks() { }

    @Command(command = "authors")
    public void browseAuthors() { }
}
```

---

#### 2. **Layer Violation: CLI → Web Controller** ⚠️ ARCHITECTURE VIOLATION
**Location:** BookCommands.java (constructor)

```java
@Component
public class BookCommands {

    private final BookController bookController;  // ❌ CLI depends on Web layer!

    public BookCommands(..., BookController bookController) {
        this.bookController = bookController;
    }
}
```

**The Problem:**
```
CLI Layer (BookCommands)
    ↓
Web Layer (BookController)  // ❌ Wrong direction!
    ↓
Service Layer (BookService)
```

**Correct Dependency Flow:**
```
CLI Layer (BookCommands)
    ↓
Service Layer (BookService)  // ✅ Both go to service
    ↑
Web Layer (BookController)
```

**Why This Matters:**
1. CLI should NOT know about web controllers
2. Tight coupling prevents independent testing
3. Can't disable web layer without breaking CLI
4. Violates Dependency Inversion Principle

**Correct Implementation:**
```java
@Component
@Command(command = "book")
public class BookCommands {

    private final BookService bookService;  // ✅ Depend on service, not controller
    private final AuthorService authorService;

    public BookCommands(BookService bookService, AuthorService authorService) {
        this.bookService = bookService;
        this.authorService = authorService;
    }

    @Command(command = "add")
    public void addBook() {
        // Call service directly, not through controller
        bookService.createBook(/*...*/);
    }
}
```

---

#### 3. **N+1 Queries in UI Building** ⚠️ PERFORMANCE DISASTER
**Location:** BookcaseCommands.java:60-74

```java
private Map<String, String> bookCaseOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases();  // Query 1

    for (BookcaseEntity b : bookcaseEntities) {  // Loop
        int shelfBookCount = 0;
        List<ShelfEntity> shelves = shelfService.findByBookcaseId(b.getBookcaseId());  // Query 2, 3, 4...

        for(ShelfEntity s : shelves){  // Nested loop!
            List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId());  // Query 5, 6, 7...
            shelfBookCount += bookList.size();
        }
        options.put(bookcaseRowFormater(b,shelfBookCount), b.getBookcaseId().toString());
    }
    return options;
}
```

**The Problem:**
- 1 query for bookcases
- N queries for shelves (one per bookcase)
- M queries for books (one per shelf)
- **Total: 1 + N + (N × M) queries**
- For 5 bookcases × 10 shelves = **51 database queries** to build a menu!

**Correct: Single Query with Aggregation**

Create a DTO:
```java
public record BookcaseMenuOption(
    Long bookcaseId,
    String bookcaseLabel,
    int shelfCount,
    int totalBooks
) {}
```

Repository method:
```java
@Repository
public interface BookcaseRepository extends JpaRepository<BookcaseEntity, Long> {

    @Query("""
        SELECT new com.penrose.bibby.dto.BookcaseMenuOption(
            bc.bookcaseId,
            bc.bookcaseLabel,
            bc.shelfCapacity,
            COUNT(DISTINCT b.bookId)
        )
        FROM BookcaseEntity bc
        LEFT JOIN ShelfEntity s ON s.bookcaseId = bc.bookcaseId
        LEFT JOIN BookEntity b ON b.shelf.shelfId = s.shelfId
        GROUP BY bc.bookcaseId, bc.bookcaseLabel, bc.shelfCapacity
        ORDER BY bc.bookcaseLabel
    """)
    List<BookcaseMenuOption> findAllWithBookCounts();
}
```

Service:
```java
@Service
public class BookcaseService {

    public List<BookcaseMenuOption> getBookcaseMenuOptions() {
        return bookcaseRepository.findAllWithBookCounts();
    }
}
```

Command:
```java
private Map<String, String> bookCaseOptions() {
    Map<String, String> options = new LinkedHashMap<>();

    for (BookcaseMenuOption option : bookcaseService.getBookcaseMenuOptions()) {
        String display = bookcaseRowFormater(
            option.bookcaseLabel(),
            option.shelfCount(),
            option.totalBooks()
        );
        options.put(display, option.bookcaseId().toString());
    }

    return options;
}
// Result: 1 query instead of 51!
```

---

### 🔶 SEVERITY: HIGH

#### 4. **No Facade Layer** ⚠️ TIGHT COUPLING
**Location:** All command classes

**Current State:**
Command classes directly call multiple services:

```java
@Component
public class BookcaseCommands {

    private final ComponentFlow.Builder componentFlowBuilder;
    private final BookcaseService bookcaseService;
    private final ShelfService shelfService;
    private final BookService bookService;  // Why does BookcaseCommands need BookService?

    // Command depends on 3+ services
    public void selectShelf(Long bookcaseId) {
        List<ShelfSummary> shelves = shelfService.getShelfSummariesForBookcase(bookcaseId);
        // ... build UI ...
        selectBookFromShelf(shelfId);
    }

    public void selectBookFromShelf(Long shelfId) {
        List<BookSummary> books = bookService.getBooksForShelf(shelfId);
        // ... build UI ...
    }
}
```

**Problems:**
1. Commands know about ALL services
2. No clear API for "browse bookcase flow"
3. Hard to test (must mock 3+ services)
4. Business logic scattered across commands

**Professional Pattern: Facade**

Create a facade for complex workflows:

```java
@Service
public class LibraryBrowseFacade {

    private final BookcaseService bookcaseService;
    private final ShelfService shelfService;
    private final BookService bookService;

    public LibraryBrowseFacade(
        BookcaseService bookcaseService,
        ShelfService shelfService,
        BookService bookService
    ) {
        this.bookcaseService = bookcaseService;
        this.shelfService = shelfService;
        this.bookService = bookService;
    }

    public List<BookcaseMenuOption> getBookcasesForBrowsing() {
        return bookcaseService.getBookcaseMenuOptions();
    }

    public List<ShelfMenuOption> getShelvesForBookcase(Long bookcaseId) {
        return shelfService.getShelfMenuOptionsForBookcase(bookcaseId);
    }

    public List<BookMenuOption> getBooksForShelf(Long shelfId) {
        return bookService.getBookMenuOptionsForShelf(shelfId);
    }

    public BookDetailView getBookDetails(Long bookId) {
        return bookService.getBookDetailView(bookId);
    }

    // Complete workflow method
    public BrowseResult browseFromBookcaseToBook(
        Long bookcaseId,
        Long shelfId,
        Long bookId
    ) {
        BookcaseEntity bookcase = bookcaseService.findById(bookcaseId);
        ShelfEntity shelf = shelfService.findById(shelfId);
        BookEntity book = bookService.findById(bookId);

        return new BrowseResult(bookcase, shelf, book);
    }
}
```

Updated command:
```java
@Component
@Command(command = "browse")
public class BrowseCommands {

    private final LibraryBrowseFacade browseFacade;  // ✅ Single dependency
    private final ComponentFlow.Builder flowBuilder;

    @Command(command = "bookcases")
    public void browseBookcases() {
        List<BookcaseMenuOption> bookcases = browseFacade.getBookcasesForBrowsing();
        // Build UI from options
    }
}
```

**Benefits:**
- ✅ Commands have ONE dependency (facade)
- ✅ Easy to test (mock one facade)
- ✅ Business logic centralized in facade
- ✅ Cleaner command code

---

#### 5. **No Abstraction for ComponentFlow** ⚠️ CODE DUPLICATION
**Location:** All command classes

**Current State:**
Every command repeats the same ComponentFlow pattern:

```java
// Pattern repeated 20+ times
ComponentFlow flow = componentFlowBuilder.clone()
    .withStringInput("fieldName")
    .name("Prompt Text")
    .and()
    .build();

ComponentFlow.ComponentFlowResult result = flow.run();
String value = result.getContext().get("fieldName");
```

**Solution: Create UI Service**

```java
@Service
public class ShellUIService {

    private final ComponentFlow.Builder flowBuilder;
    private final Terminal terminal;

    public ShellUIService(
        ComponentFlow.Builder flowBuilder,
        Terminal terminal
    ) {
        this.flowBuilder = flowBuilder;
        this.terminal = terminal;
    }

    // Simple string input
    public String promptForString(String fieldName, String promptText) {
        ComponentFlow flow = flowBuilder.clone()
            .withStringInput(fieldName)
            .name(promptText)
            .and()
            .build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        return result.getContext().get(fieldName);
    }

    // Single selection
    public String promptForSelection(
        String fieldName,
        String promptText,
        Map<String, String> options
    ) {
        ComponentFlow flow = flowBuilder.clone()
            .withSingleItemSelector(fieldName)
            .name(promptText)
            .selectItems(options)
            .and()
            .build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        return result.getContext().get(fieldName);
    }

    // Multi-field form
    public Map<String, String> promptForForm(FormDefinition formDef) {
        ComponentFlow.Builder builder = flowBuilder.clone();

        for (FormField field : formDef.getFields()) {
            builder = builder
                .withStringInput(field.getName())
                .name(field.getPrompt())
                .and();
        }

        ComponentFlow flow = builder.build();
        ComponentFlow.ComponentFlowResult result = flow.run();
        return result.getContext();
    }

    // Output helpers
    public void printSuccess(String message) {
        terminal.writer().println("✅ " + message);
        terminal.writer().flush();
    }

    public void printError(String message) {
        terminal.writer().println("❌ " + message);
        terminal.writer().flush();
    }

    public void printInfo(String message) {
        terminal.writer().println("ℹ️  " + message);
        terminal.writer().flush();
    }
}
```

**Usage in commands:**
```java
@Component
@Command(command = "bookcase")
public class BookcaseCommands {

    private final BookcaseService bookcaseService;
    private final ShellUIService ui;  // ✅ UI abstraction

    @Command(command = "add")
    public void addBookcase() {
        String label = ui.promptForString("bookcaseLabel", "Give this bookcase a label:_");
        String capacity = ui.promptForString("shelfCapacity", "How many shelves?:_");

        try {
            bookcaseService.createNewBookCase(label, Integer.parseInt(capacity));
            ui.printSuccess("Bookcase '" + label + "' created successfully!");
        } catch (Exception e) {
            ui.printError("Failed to create bookcase: " + e.getMessage());
        }
    }
}
```

**Benefits:**
- ✅ Eliminates 100+ lines of duplicate flow code
- ✅ Consistent UI behavior
- ✅ Easy to add validation
- ✅ Easy to test (mock UI service)

---

#### 6. **No Configuration Profiles** ⚠️ INFLEXIBLE DEPLOYMENT
**Location:** application.properties

**Current State:**
```properties
# One configuration for everything
spring.shell.interactive.enabled=true
spring.datasource.url=jdbc:postgresql://localhost:5332/amigos
logging.level.root=OFF
```

**Problems:**
1. Can't run as web-only (no shell)
2. Can't run as CLI-only (no web server)
3. Can't switch between dev/test/prod configs
4. Hardcoded database URL

**Professional Solution: Spring Profiles**

**application.properties** (default):
```properties
spring.application.name=Bibby
spring.output.ansi.enabled=ALWAYS

# Default to CLI mode
spring.profiles.active=cli

# Error handling
server.error.include-message=always
server.error.include-binding-errors=never
server.error.include-stacktrace=on-param
```

**application-cli.properties** (CLI mode):
```properties
# Enable shell, disable web
spring.shell.interactive.enabled=true
spring.main.web-application-type=none

# Minimal logging for clean CLI
logging.level.root=WARN
logging.level.com.penrose.bibby=INFO

# Use H2 for local testing
spring.datasource.url=jdbc:h2:mem:bibby
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
```

**application-web.properties** (Web API mode):
```properties
# Disable shell, enable web
spring.shell.interactive.enabled=false
server.port=8080

# Full logging for debugging
logging.level.root=INFO
logging.level.com.penrose.bibby=DEBUG
logging.level.org.springframework.web=DEBUG

# PostgreSQL for production
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USER}
spring.datasource.password=${DATABASE_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
```

**application-dual.properties** (Both CLI + Web):
```properties
# Enable both
spring.shell.interactive.enabled=true
server.port=8080

# Moderate logging
logging.level.root=INFO
logging.level.com.penrose.bibby=DEBUG
```

**application-test.properties** (For tests):
```properties
spring.shell.interactive.enabled=false
spring.main.web-application-type=none

# H2 in-memory database
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
```

**Run with different profiles:**
```bash
# CLI only
java -jar bibby.jar --spring.profiles.active=cli

# Web only
java -jar bibby.jar --spring.profiles.active=web

# Both
java -jar bibby.jar --spring.profiles.active=dual

# From Maven
mvn spring-boot:run -Dspring-boot.run.profiles=cli
```

---

### 🔷 SEVERITY: MEDIUM

#### 7. **Hardcoded Database Credentials** ⚠️ SECURITY RISK
**Location:** application.properties:3-5

```properties
spring.datasource.url=jdbc:postgresql://localhost:5332/amigos
spring.datasource.username=amigoscode
spring.datasource.password=password
```

**Problems:**
1. **Security:** Passwords in source control
2. **Inflexible:** Can't change DB without recompiling
3. **Multi-environment:** Same credentials for dev/prod?

**Solution: Environment Variables**

**application.properties:**
```properties
spring.datasource.url=${DATABASE_URL:jdbc:h2:mem:bibby}
spring.datasource.username=${DATABASE_USER:sa}
spring.datasource.password=${DATABASE_PASSWORD:}
```

The `:` syntax provides defaults:
- `${DATABASE_URL:jdbc:h2:mem:bibby}` = use env var or fall back to H2

**Set environment variables:**

Linux/Mac:
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/bibby
export DATABASE_USER=bibby_user
export DATABASE_PASSWORD=super_secret
```

Windows:
```cmd
set DATABASE_URL=jdbc:postgresql://localhost:5432/bibby
set DATABASE_USER=bibby_user
set DATABASE_PASSWORD=super_secret
```

**Or use .env file** (with dotenv library):
```
DATABASE_URL=jdbc:postgresql://localhost:5432/bibby
DATABASE_USER=bibby_user
DATABASE_PASSWORD=super_secret
```

**Production (Docker):**
```yaml
# docker-compose.yml
services:
  bibby:
    image: bibby:latest
    environment:
      DATABASE_URL: jdbc:postgresql://db:5432/bibby
      DATABASE_USER: bibby_prod
      DATABASE_PASSWORD: ${BIBBY_DB_PASSWORD}  # From secret manager
```

---

#### 8. **All Logging Disabled** ⚠️ DEBUGGING NIGHTMARE
**Location:** application.properties:17-19

```properties
logging.level.root=OFF
logging.level.org.springframework=OFF
logging.level.org.hibernate=OFF
```

**Problems:**
1. Can't debug issues in production
2. No audit trail
3. No error tracking
4. Can't see SQL queries

**Professional Logging Configuration:**

**application-dev.properties:**
```properties
# Development: verbose logging
logging.level.root=INFO
logging.level.com.penrose.bibby=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Show SQL with parameters
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

**application-prod.properties:**
```properties
# Production: errors + audit
logging.level.root=WARN
logging.level.com.penrose.bibby=INFO
logging.level.com.penrose.bibby.security=DEBUG

# Log to file
logging.file.name=/var/log/bibby/app.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} - %msg%n

# Rotate logs
logging.logback.rollingpolicy.max-file-size=10MB
logging.logback.rollingpolicy.max-history=30
```

**Add SLF4J logging in code:**
```java
@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    public Book createBook(BookRequestDTO request) {
        log.info("Creating book: title={}", request.title());

        try {
            Book book = // ... creation logic
            log.info("Book created successfully: id={}, title={}", book.getId(), book.getTitle());
            return book;
        } catch (Exception e) {
            log.error("Failed to create book: title={}", request.title(), e);
            throw e;
        }
    }
}
```

---

## Recommended Architecture

### Layered Architecture

```
┌─────────────────────────────────────────────────┐
│              Presentation Layer                  │
│  ┌──────────────────┐   ┌───────────────────┐  │
│  │  CLI Commands    │   │  REST Controllers  │  │
│  │  (Spring Shell)  │   │   (Spring Web)     │  │
│  └────────┬─────────┘   └────────┬──────────┘  │
└───────────┼──────────────────────┼──────────────┘
            │                      │
            ├──────────────────────┤
            ↓                      ↓
┌─────────────────────────────────────────────────┐
│              Facade Layer (Optional)             │
│    ┌────────────────────────────────────┐       │
│    │  LibraryBrowseFacade               │       │
│    │  CirculationFacade                 │       │
│    │  CatalogManagementFacade           │       │
│    └──────────┬─────────────────────────┘       │
└───────────────┼──────────────────────────────────┘
                ↓
┌─────────────────────────────────────────────────┐
│              Service Layer                       │
│    ┌──────────┬───────────┬──────────────┐     │
│    │ BookSvc  │ AuthorSvc │ BookcaseSvc  │     │
│    └────┬─────┴──────┬────┴───────┬──────┘     │
└─────────┼────────────┼────────────┼─────────────┘
          ↓            ↓            ↓
┌─────────────────────────────────────────────────┐
│            Repository Layer                      │
│    ┌──────────┬───────────┬──────────────┐     │
│    │ BookRepo │ AuthorRepo│ BookcaseRepo │     │
│    └────┬─────┴──────┬────┴───────┬──────┘     │
└─────────┼────────────┼────────────┼─────────────┘
          ↓            ↓            ↓
┌─────────────────────────────────────────────────┐
│              Database (PostgreSQL)               │
└─────────────────────────────────────────────────┘
```

### Package Structure (Recommended)

```
com.penrose.bibby/
├── BibbyApplication.java
│
├── cli/                          # CLI interface layer
│   ├── commands/
│   │   ├── BookManagementCommands.java
│   │   ├── CirculationCommands.java
│   │   └── BrowseCommands.java
│   ├── ui/
│   │   ├── ShellUIService.java
│   │   └── CustomPromptProvider.java
│   └── dto/
│       └── MenuOption.java
│
├── web/                          # Web interface layer
│   ├── controller/
│   │   ├── BookController.java
│   │   ├── AuthorController.java
│   │   └── BookcaseController.java
│   ├── dto/
│   │   ├── BookRequestDTO.java
│   │   └── BookResponseDTO.java
│   └── exception/
│       └── GlobalExceptionHandler.java
│
├── service/                      # Business logic layer
│   ├── BookService.java
│   ├── AuthorService.java
│   ├── BookcaseService.java
│   └── facade/                   # Facades for complex workflows
│       ├── LibraryBrowseFacade.java
│       └── CirculationFacade.java
│
├── domain/                       # Domain entities & repositories
│   ├── book/
│   │   ├── Book.java             # Domain model
│   │   ├── BookEntity.java       # JPA entity
│   │   ├── BookRepository.java
│   │   └── BookStatus.java       # Enum
│   ├── author/
│   ├── shelf/
│   └── bookcase/
│
└── config/                       # Configuration
    ├── JpaConfig.java
    ├── SecurityConfig.java
    └── WebConfig.java
```

---

## Action Items

### 🚨 Critical (Do First - 4-6 hours)

1. **Remove Layer Violation**
   - [ ] Remove `BookController` dependency from CLI commands
   - [ ] CLI should call services directly, not controllers
   - **File:** BookCommands.java constructor

2. **Fix N+1 Queries in Menu Building**
   - [ ] Create `BookcaseMenuOption` DTO
   - [ ] Add `findAllWithBookCounts()` repository method
   - [ ] Replace loop-based menu building with single query
   - **File:** BookcaseCommands.java:60-74

3. **Split Monolithic Commands**
   - [ ] Split BookCommands (594 lines) into 3-4 focused classes
   - [ ] Group related commands together
   - [ ] Each command class < 200 lines
   - **File:** BookCommands.java

### 🔶 High Priority (This Week - 6-8 hours)

4. **Create Facade Layer**
   - [ ] Create `LibraryBrowseFacade` for browse workflows
   - [ ] Create `CirculationFacade` for checkout/checkin
   - [ ] Update commands to use facades instead of multiple services

5. **Create UI Service Abstraction**
   - [ ] Create `ShellUIService` with prompt methods
   - [ ] Replace duplicate ComponentFlow code
   - [ ] Add consistent success/error messaging

6. **Add Configuration Profiles**
   - [ ] Create `application-cli.properties`
   - [ ] Create `application-web.properties`
   - [ ] Create `application-test.properties`
   - [ ] Extract database credentials to environment variables

### 🔷 Medium Priority (This Month - 4-6 hours)

7. **Improve Logging**
   - [ ] Add SLF4J logger to all services
   - [ ] Log important business events
   - [ ] Configure different logging for dev/prod profiles

8. **Reorganize Package Structure**
   - [ ] Move CLI commands to `cli.commands` package
   - [ ] Move controllers to `web.controller` package
   - [ ] Move services to dedicated `service` package
   - [ ] Create `service.facade` package

9. **Add Startup Configuration**
   - [ ] Make web server optional (controlled by profile)
   - [ ] Add health check endpoint
   - [ ] Add actuator for monitoring

### ⚪ Low Priority (Nice to Have)

10. **Advanced Features**
    - [ ] Add Spring Boot Admin for monitoring
    - [ ] Add metrics collection
    - [ ] Add distributed tracing
    - [ ] Add caching layer

---

## Testing Different Modes

### CLI Only
```bash
# application-cli.properties
spring.shell.interactive.enabled=true
spring.main.web-application-type=none

# Run
mvn spring-boot:run -Dspring-boot.run.profiles=cli
```

### Web Only
```bash
# application-web.properties
spring.shell.interactive.enabled=false

# Run
mvn spring-boot:run -Dspring-boot.run.profiles=web

# Access API
curl http://localhost:8080/api/v1/books
```

### Both (Current Behavior)
```bash
# application-dual.properties
spring.shell.interactive.enabled=true

# Run
mvn spring-boot:run -Dspring-boot.run.profiles=dual

# Use CLI in terminal AND access web API
```

---

## Summary

### Your Current State
- ❌ 594-line command class
- ❌ CLI depends on web controllers
- ❌ N+1 queries in menu building
- ❌ No facade layer
- ❌ Duplicate ComponentFlow code
- ❌ Hardcoded configuration
- ❌ All logging disabled

### After This Section
- ✅ Focused command classes (< 200 lines each)
- ✅ Clean layer separation
- ✅ Efficient single-query menu building
- ✅ Facade pattern for complex workflows
- ✅ Reusable UI service
- ✅ Profile-based configuration
- ✅ Proper logging

---

## Resources

### Official Docs
- [Spring Boot Reference](https://docs.spring.io/spring-boot/reference/)
- [Spring Shell Reference](https://docs.spring.io/spring-shell/docs/current/reference/html/)
- [Spring Profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html)

### Books
- **"Spring Boot in Action"** by Craig Walls
- **"Cloud Native Java"** by Josh Long

---

## Mentor's Note

Leo, your dual-interface architecture (CLI + Web) is **actually brilliant** for a portfolio project. It demonstrates:
1. Understanding of different user interfaces
2. Shared business logic
3. Real-world architectural thinking

**However**, your implementation has critical flaws:
1. The 594-line BookCommands class is unmaintainable
2. CLI depending on web controllers breaks layering
3. N+1 queries will kill performance with more data

**Good news:** These are all fixable in a weekend.

**Priority order:**
1. Remove layer violation (30 minutes)
2. Fix N+1 query in menu (2 hours)
3. Split BookCommands (3-4 hours)
4. Add profiles (1 hour)
5. Everything else (6-8 hours)

After this refactoring, you'll have a **clean, layered architecture** that can scale.

---

**Next Section:** Integration Testing Strategy

**Last Updated:** 2025-11-17
**Status:** Complete ✅
