# Section 21: Spring IoC & Dependency Injection
## How Spring Wires Your Application Together

**Files:** `BookService.java`, `AuthorService.java`, `BookCommands.java`, `StartupRunner.java`
**Concept:** Inversion of Control, Dependency Injection, ApplicationContext, Bean Lifecycle
**Time:** 60 min read

---

## What You'll Learn

By the end of this section, you'll understand:

- What IoC (Inversion of Control) means and why it matters
- How Spring's ApplicationContext manages beans
- The three types of dependency injection (constructor, setter, field)
- Why constructor injection is superior (and Bibby uses it correctly)
- How Spring resolves dependencies automatically
- When you need `@Autowired` (and when you don't)
- What circular dependencies are and how to detect them
- The bean lifecycle (creation, injection, initialization, destruction)
- **CRITICAL:** AuthorService has a broken dependency injection

Every concept is explained using **Bibby's actual dependency injection patterns**.

---

## The Problem: Manual Dependency Management

**Without a framework**, you create objects manually:

```java
public class LibraryApp {
    public static void main(String[] args) {
        // Create dependencies manually
        AuthorRepository authorRepo = new AuthorRepository();
        BookRepository bookRepo = new BookRepository();

        // Inject dependencies manually
        BookService bookService = new BookService(bookRepo, authorRepo);

        // Create dependent services
        BookController controller = new BookController(bookService);
    }
}
```

**Problems:**
1. **You control object creation** (tedious, error-prone)
2. **Hard to test** (can't swap real DB for mock)
3. **Tight coupling** (change constructor = change all callers)
4. **No lifecycle management** (who closes database connections?)
5. **Initialization order** (what if BookService needs AuthorService which needs BookService?)

---

## The Solution: Inversion of Control (IoC)

**Inversion of Control** means:

> "Don't call us, we'll call you"

Instead of **YOU** creating objects, **Spring** creates them for you.

```java
// Before IoC: You control object creation
BookService bookService = new BookService(bookRepo, authorRepo);

// After IoC: Spring controls object creation
// You just declare what you need, Spring provides it
@Service
public class BookController {
    private final BookService bookService; // Spring injects this

    public BookController(BookService bookService) {
        this.bookService = bookService; // Spring calls this constructor
    }
}
```

**Benefits:**
- Spring creates objects in correct order
- Spring manages lifecycles (initialization, cleanup)
- You declare dependencies, Spring provides them
- Easy to swap implementations (real DB vs mock for testing)

---

## Spring's IoC Container: The ApplicationContext

Spring's **ApplicationContext** is the **IoC container** - a big map of all objects (beans) in your application:

```
ApplicationContext (Spring's Object Factory)
├── bookRepository (BookRepository bean)
├── authorRepository (AuthorRepository bean)
├── bookService (BookService bean)
├── bookController (BookController bean)
├── bookcaseService (BookcaseService bean)
├── shelfService (ShelfService bean)
├── authorService (AuthorService bean)
└── ... (100+ more beans)
```

**How it works:**

1. **Startup**: Spring scans your code for `@Component`, `@Service`, `@Repository` annotations
2. **Bean creation**: Spring creates instances of these classes
3. **Dependency injection**: Spring wires beans together by calling constructors
4. **Ready**: Your application is fully initialized and running

**You never write `new BookService()` - Spring does it for you.**

---

## Bean Definition: Telling Spring What to Create

**A bean is any object managed by Spring.**

You define beans with stereotyp

e annotations:

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@Component` | Generic bean | Custom utilities, helpers |
| `@Service` | Business logic | BookService, AuthorService |
| `@Repository` | Data access | BookRepository, AuthorRepository |
| `@RestController` | REST endpoints | BookController |
| `@Configuration` | Java config classes | Custom Spring configuration |

**All of these are specializations of `@Component`.**

---

## Bibby's Beans

Let's identify all beans in Bibby:

**Services (5 beans):**
```java
@Service
public class BookService { }

@Service
public class AuthorService { }

@Service
public class BookcaseService { }

@Service
public class ShelfService { }

@Service
public class CatalogService { }
```

**Repositories (5 beans - via JpaRepository):**
```java
@Repository  // Implicit from extending JpaRepository
public interface BookRepository extends JpaRepository<BookEntity, Long> { }

@Repository
public interface AuthorRepository extends JpaRepository<AuthorEntity, Long> { }

@Repository
public interface BookcaseRepository extends JpaRepository<BookcaseEntity, Long> { }

@Repository
public interface ShelfRepository extends JpaRepository<ShelfEntity, Long> { }

@Repository
public interface CatalogRepository extends JpaRepository<CatalogEntity, Long> { }
```

**Controllers (3 beans):**
```java
@RestController
public class BookController { }

@RestController
public class BookCaseController { }

@RestController
public class ShelfController { }
```

**CLI Commands (2 beans):**
```java
@Component
@Command(command = "book")
public class BookCommands { }

@Component
@Command(command = "bookcase")
public class BookcaseCommands { }
```

**Utilities (2 beans):**
```java
@Component
public class StartupRunner { }

@Component
public class CustomPromptProvider { }
```

**Total beans in Bibby:** ~17 of your own + 100+ from Spring Boot

---

## Dependency Injection: Three Flavors

Spring supports **3 ways** to inject dependencies:

### 1. Constructor Injection (BEST - Bibby uses this)

**BookService.java:14-20**
```java
@Service
public class BookService {
    final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository){
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }
}
```

**How it works:**
1. Spring sees `BookService` needs `BookRepository` and `AuthorRepository`
2. Spring looks in ApplicationContext for these beans
3. Spring finds `bookRepository` bean and `authorRepository` bean
4. Spring calls constructor: `new BookService(bookRepository, authorRepository)`

**Benefits:**
- ✅ **Immutable** (fields can be `final`)
- ✅ **Explicit** (can't create BookService without dependencies)
- ✅ **Testable** (just call constructor in tests)
- ✅ **No @Autowired needed** (Spring auto-detects single constructor)

---

### 2. Field Injection (OLD STYLE - avoid)

**StartupRunner.java:9-10**
```java
@Component
public class StartupRunner {
    @Autowired
    ShelfService shelfService;  // ❌ Not final, not explicit
}
```

**How it works:**
1. Spring creates `StartupRunner` using no-arg constructor
2. Spring uses reflection to set `shelfService` field

**Problems:**
- ❌ **Can't be `final`** (mutable state)
- ❌ **Hidden dependency** (looks like null, then magically becomes non-null)
- ❌ **Hard to test** (need Spring context to inject, can't just call constructor)
- ❌ **Requires `@Autowired`** (verbose)

**This pattern is deprecated.** Bibby should refactor this to constructor injection.

---

### 3. Setter Injection (rare, special cases only)

```java
@Service
public class BookService {
    private BookRepository bookRepository;

    @Autowired
    public void setBookRepository(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }
}
```

**Use cases:**
- Optional dependencies
- Re-configurable beans
- Circular dependencies (last resort)

**For Bibby:** Never needed. Use constructor injection.

---

## When You Need @Autowired (And When You Don't)

### Don't Need @Autowired:

**Single constructor** (Spring auto-detects):

```java
@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {  // ✅ No @Autowired needed!
        this.bookRepository = bookRepository;
    }
}
```

Spring sees **one constructor**, assumes it's for dependency injection.

---

### Need @Autowired:

**Multiple constructors** (Spring can't guess which one):

```java
@Service
public class BookService {
    private final BookRepository bookRepository;

    @Autowired  // ← Tell Spring to use THIS constructor
    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public BookService() {  // No-arg constructor for special cases
        this.bookRepository = null;
    }
}
```

**Field injection** (old style):

```java
@Autowired  // ← Required for field injection
private ShelfService shelfService;
```

**Setter injection**:

```java
@Autowired  // ← Required for setter injection
public void setBookRepository(BookRepository repo) {
    this.bookRepository = repo;
}
```

---

## Bibby's Dependency Injection Patterns

Let's analyze Bibby's actual code:

### ✅ EXCELLENT: BookService (Constructor Injection)

**BookService.java:14-20**
```java
@Service
public class BookService {
    final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository){
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }
}
```

**Grade: A+**

- Constructor injection ✅
- Fields are `final` ✅
- No `@Autowired` needed (single constructor) ✅
- Explicit dependencies ✅
- Testable (just call constructor with mocks) ✅

---

### ✅ EXCELLENT: ShelfService

**ShelfService.java:14-18**
```java
@Service
public class ShelfService {
    ShelfRepository shelfRepository;

    public ShelfService(ShelfRepository shelfRepository) {
        this.shelfRepository = shelfRepository;
    }
}
```

**Grade: A**

- Constructor injection ✅
- No `@Autowired` needed ✅
- **Could be improved:** Make `shelfRepository` `final`

**Better:**
```java
private final ShelfRepository shelfRepository;  // ← Add final
```

---

### ✅ EXCELLENT: BookCommands (6 Dependencies!)

**BookCommands.java:28-32, 62-69**
```java
@Component
@Command(command = "book")
public class BookCommands {
    final BookService bookService;
    final BookController bookController;
    final BookcaseService bookcaseService;
    final ShelfService shelfService;
    final AuthorService authorService;

    public BookCommands(ComponentFlow.Builder componentFlowBuilder,
                       BookService bookService,
                       BookController bookController,
                       BookcaseService bookcaseService,
                       ShelfService shelfService,
                       AuthorService authorService) {
        this.componentFlowBuilder = componentFlowBuilder;
        this.bookService = bookService;
        this.bookController = bookController;
        this.bookcaseService = bookcaseService;
        this.shelfService = shelfService;
        this.authorService = authorService;
    }
}
```

**Grade: A+**

- Constructor injection with **6 dependencies** ✅
- All fields `final` ✅
- No `@Autowired` (single constructor) ✅
- Spring automatically provides all 6 beans ✅

**This demonstrates Spring's power:** You declare 6 dependencies, Spring provides them. No manual wiring.

---

### ❌ OLD STYLE: StartupRunner (Field Injection)

**StartupRunner.java:9-10**
```java
@Component
public class StartupRunner {
    @Autowired
    ShelfService shelfService;  // ❌ Field injection
}
```

**Grade: C**

- Field injection (deprecated pattern) ❌
- Not `final` (mutable) ❌
- Hidden dependency ❌
- Requires `@Autowired` ❌

**Refactored (constructor injection):**
```java
@Component
public class StartupRunner {
    private final ShelfService shelfService;  // ✅ Final

    public StartupRunner(ShelfService shelfService) {  // ✅ Constructor
        this.shelfService = shelfService;
    }
}
```

---

### 🚨 CRITICAL BUG: AuthorService (No Injection!)

**AuthorService.java:8-15**
```java
@Service
public class AuthorService {
    AuthorRepository authorRepository;  // ❌ NO constructor! NO @Autowired!

    public List<AuthorEntity> findByBookId(Long id){
        return authorRepository.findByBooks_BookId(id);  // 💥 NullPointerException!
    }
}
```

**Grade: F - BROKEN**

**What's wrong:**
- `authorRepository` field exists but is **NEVER SET**
- No constructor
- No `@Autowired`
- Spring creates `AuthorService` bean, but `authorRepository` **stays null**

**What happens when you call `findByBookId()`:**

```java
authorService.findByBookId(123);
// ↓
return authorRepository.findByBooks_BookId(id);
//     ↑ NullPointerException! authorRepository is null!
```

**Fix (add constructor):**

```java
@Service
public class AuthorService {
    private final AuthorRepository authorRepository;  // ✅ Make final

    public AuthorService(AuthorRepository authorRepository) {  // ✅ Add constructor
        this.authorRepository = authorRepository;
    }

    public List<AuthorEntity> findByBookId(Long id){
        return authorRepository.findByBooks_BookId(id);
    }
}
```

**Why hasn't this crashed?**

Likely because `AuthorService.findByBookId()` is **never called** in Bibby. If you add a feature using this method, it will crash immediately.

---

## How Spring Resolves Dependencies

**Dependency graph for BookService:**

```
BookService
├── BookRepository
└── AuthorRepository
```

**Spring's resolution process:**

1. **Scan**: Find all `@Service`, `@Repository`, `@Component` classes
2. **Analyze**: Determine dependencies from constructors
3. **Create**: Create beans in correct order:
   ```
   1. Create BookRepository (no dependencies)
   2. Create AuthorRepository (no dependencies)
   3. Create BookService (needs BookRepository + AuthorRepository)
   ```
4. **Inject**: Call constructors with dependencies

**For BookCommands (complex example):**

```
BookCommands
├── ComponentFlow.Builder (from Spring Shell)
├── BookService
│   ├── BookRepository
│   └── AuthorRepository
├── BookController
│   └── BookService (already created)
├── BookcaseService
│   ├── BookcaseRepository
│   └── ShelfRepository
├── ShelfService
│   └── ShelfRepository (already created)
└── AuthorService
    └── AuthorRepository (already created)
```

Spring creates **all dependencies first**, then creates `BookCommands`.

**If a dependency is missing,** Spring fails at startup:

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Field bookRepository in com.penrose.bibby.library.book.BookService required a bean of type 'BookRepository' that could not be found.

Action:

Consider defining a bean of type 'BookRepository' in your configuration.
```

This is **fail-fast**: Errors at startup, not at runtime.

---

## Circular Dependencies (The Deadly Embrace)

**What if two beans depend on each other?**

```java
@Service
public class A {
    public A(B b) { }  // A needs B
}

@Service
public class B {
    public B(A a) { }  // B needs A
}
```

**Spring's dilemma:**
- Can't create A (needs B first)
- Can't create B (needs A first)

**Result:** Startup crash:

```
***************************
APPLICATION FAILED TO START
***************************

The dependencies of some of the beans in the application context form a cycle:

   A → B → A
```

---

**Solutions:**

### 1. Refactor (Best)

**Extract shared logic into a third service:**

```java
@Service
public class SharedService { }

@Service
public class A {
    public A(SharedService shared) { }
}

@Service
public class B {
    public B(SharedService shared) { }
}
```

Now no circular dependency.

---

### 2. Setter Injection (Last Resort)

```java
@Service
public class A {
    private B b;

    public A() { }  // No-arg constructor

    @Autowired
    public void setB(B b) {  // Setter injection
        this.b = b;
    }
}

@Service
public class B {
    public B(A a) { }  // Constructor injection for B
}
```

**How it works:**
1. Spring creates A (no dependencies in constructor)
2. Spring creates B (passes A)
3. Spring calls `A.setB(b)` to complete cycle

**This works but is ugly.** Prefer refactoring.

---

### 3. @Lazy (Deferred Injection)

```java
@Service
public class A {
    public A(@Lazy B b) { }  // Don't inject B immediately
}

@Service
public class B {
    public B(A a) { }
}
```

Spring creates a **proxy** for B, breaking the cycle.

---

## Bibby Doesn't Have Circular Dependencies (Good!)

I analyzed Bibby's dependency graph:

- BookService → BookRepository, AuthorRepository
- BookcaseService → BookcaseRepository, ShelfRepository
- ShelfService → ShelfRepository
- BookController → BookService
- BookCommands → BookService, BookController, BookcaseService, ShelfService, AuthorService

**All dependencies flow in one direction (acyclic graph). Excellent design!**

---

## Bean Lifecycle

**Spring bean lifecycle:**

```
1. Instantiation (call constructor)
   ↓
2. Dependency Injection (set fields, call setters)
   ↓
3. @PostConstruct (initialization method)
   ↓
4. Bean ready for use
   ↓
5. Application runs
   ↓
6. @PreDestroy (cleanup method)
   ↓
7. Bean destroyed
```

**Example:**

```java
@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {  // 1. Constructor
        this.bookRepository = bookRepository;
    }

    @PostConstruct  // 3. Called after dependency injection
    public void init() {
        System.out.println("BookService initialized!");
    }

    @PreDestroy  // 6. Called before shutdown
    public void cleanup() {
        System.out.println("BookService shutting down!");
    }
}
```

**Bibby doesn't use `@PostConstruct` or `@PreDestroy`** (not needed for simple services).

Use cases:
- `@PostConstruct`: Initialize caches, open connections, validate state
- `@PreDestroy`: Close connections, flush buffers, cleanup resources

---

## Scopes: Singleton vs Prototype

**By default, all Spring beans are singletons:**

```java
@Service
public class BookService { }

// Only ONE instance exists in ApplicationContext
// Every injection of BookService gets the SAME object
```

**Verify:**

```java
@RestController
public class BookController {
    public BookController(BookService bookService) {
        System.out.println(bookService);  // BookService@12345
    }
}

@Component
public class BookCommands {
    public BookCommands(BookService bookService) {
        System.out.println(bookService);  // BookService@12345 (SAME object!)
    }
}
```

**Singleton benefits:**
- **Performance**: One instance, no repeated initialization
- **Shared state**: All components see same instance
- **Memory efficient**: One object, not millions

---

**Prototype scope** (rare):

```java
@Service
@Scope("prototype")
public class BookService { }

// NEW instance created every time it's injected
```

**When to use prototype:**
- Stateful beans (each request needs its own instance)
- Threads (each thread needs isolated state)

**For Bibby:** Singleton is perfect. Services are stateless.

---

## Testing with Dependency Injection

**Constructor injection makes testing trivial:**

**Without Spring:**

```java
@Test
void createNewBook_WithNewAuthor_CreatesBoth() {
    // Arrange: Create mocks manually
    BookRepository mockBookRepo = mock(BookRepository.class);
    AuthorRepository mockAuthorRepo = mock(AuthorRepository.class);

    // Inject via constructor (no Spring needed!)
    BookService bookService = new BookService(mockBookRepo, mockAuthorRepo);

    // Act & Assert
    // ...
}
```

**Field injection makes this impossible:**

```java
@Service
public class BookService {
    @Autowired
    private BookRepository bookRepository;  // ❌ Can't set in tests!
}

@Test
void test() {
    BookService service = new BookService();
    // How do we set bookRepository? We can't! It's private!
    // We'd need Spring's test context (slow, complex)
}
```

**This is why constructor injection is superior for testing.**

---

## Your Action Items

**Priority 1: Fix AuthorService (CRITICAL)**

1. Add constructor injection to `AuthorService.java`:
   ```java
   private final AuthorRepository authorRepository;

   public AuthorService(AuthorRepository authorRepository) {
       this.authorRepository = authorRepository;
   }
   ```

2. Test that `AuthorService.findByBookId()` works without NullPointerException

**Priority 2: Refactor Field Injection**

3. Refactor `StartupRunner.java` from field injection to constructor injection:
   ```java
   private final ShelfService shelfService;

   public StartupRunner(ShelfService shelfService) {
       this.shelfService = shelfService;
   }
   ```

**Priority 3: Make Fields Final**

4. Add `final` to `ShelfService.shelfRepository`:
   ```java
   private final ShelfRepository shelfRepository;
   ```

5. Add `final` to all service fields that are missing it

**Priority 4: Verify No Circular Dependencies**

6. Run `mvn spring-boot:run` and ensure application starts without circular dependency errors

7. If errors occur, analyze the dependency chain and refactor

**Priority 5: Understand Your Dependency Graph**

8. Draw Bibby's dependency graph on paper:
   - Start with `BookCommands`
   - Trace all dependencies recursively
   - Verify all paths lead to repositories (leaf nodes)

---

## Key Takeaways

**1. Inversion of Control:**
- Spring creates objects for you
- You declare dependencies, Spring provides them
- "Don't call us, we'll call you"

**2. ApplicationContext is the IoC Container:**
- Big map of all beans (objects)
- Scans for `@Component`, `@Service`, `@Repository`, `@RestController`
- Creates beans in dependency order
- Injects dependencies automatically

**3. Constructor Injection is Best:**
- Immutable (`final` fields)
- Explicit (can't create object without dependencies)
- Testable (just call constructor with mocks)
- No `@Autowired` needed for single constructor

**4. Field Injection is Deprecated:**
- Not `final`
- Hidden dependencies
- Hard to test
- Requires `@Autowired`

**5. @Autowired is Optional:**
- Single constructor: No `@Autowired` needed
- Multiple constructors: `@Autowired` required on one
- Field/setter injection: `@Autowired` required

**6. Beans are Singletons by Default:**
- One instance per ApplicationContext
- Shared across all injections
- Stateless services are perfect for singletons

**7. Fail-Fast on Missing Dependencies:**
- Application crashes at startup if bean missing
- Better than runtime NullPointerExceptions
- Errors are explicit and immediate

---

## Bibby's Dependency Injection Grade: B+

**What you're doing excellently:**

✅ BookService uses constructor injection
✅ ShelfService uses constructor injection
✅ BookcaseService uses constructor injection
✅ BookCommands injects 6 dependencies cleanly
✅ No circular dependencies
✅ No `@Autowired` on constructors (Spring auto-detects)

**What needs fixing:**

❌ **AuthorService has broken injection** (CRITICAL - NullPointerException waiting to happen)
❌ StartupRunner uses deprecated field injection
❌ Some fields not marked `final` (ShelfService)

**After fixing these → Grade: A**

---

## What's Next

**Section 22: Component Scanning & Stereotypes**

Now that you understand dependency injection (HOW beans are wired), we'll explore **component scanning** (HOW Spring finds your beans).

You'll learn:
- What component scanning is and how it works
- The difference between `@Component`, `@Service`, and `@Repository`
- How `@ComponentScan` configures package scanning
- Why `@SpringBootApplication` includes `@ComponentScan`
- How to customize component scanning
- What happens if Spring doesn't find your beans
- Meta-annotations and creating your own stereotypes

We'll analyze how Spring Boot finds Bibby's 17+ beans across your package structure and understand the invisible scanning that happens at startup.

**Ready when you are!**

---

**Mentor's Note:**

Dependency injection is Spring's **core superpower**. It transforms Java from "you manage everything" to "declare what you need, Spring provides it."

The difference between manual object creation and DI is like the difference between:
- **Manual:** Build your own car from parts (exhausting, error-prone)
- **DI:** Order a car, it arrives fully assembled (effortless, reliable)

You've discovered Bibby uses constructor injection excellently in most places. You've also found a **critical bug** in AuthorService that would cause crashes if that code path is ever executed.

This is the power of understanding the framework: You can spot bugs before they happen.

Fixing AuthorService takes 5 lines of code but prevents a production crash. That's engineering.

---

**Files Referenced:**
- `BookService.java:14-20` (Perfect constructor injection)
- `ShelfService.java:14-18` (Good constructor injection, missing final)
- `BookCommands.java:28-32, 62-69` (6 dependencies injected!)
- `StartupRunner.java:9-10` (Deprecated field injection)
- `AuthorService.java:8-15` (BROKEN - no injection!)

**Total Lines Analyzed:** 50+ lines across 5 files

**Estimated Reading Time:** 60 minutes
**Estimated Action Items Time:** 20 minutes
**Total Section Time:** 80 minutes

---

*Section 21 Complete - Section 22: Component Scanning & Stereotypes Next*
