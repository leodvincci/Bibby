# SECTION 10: CREATIONAL PATTERNS - SINGLETON & PROTOTYPE

## The Most Controversial and Least Used Patterns

Welcome to Section 10. We're covering two creational patterns that are often misunderstood and misused: **Singleton** and **Prototype**.

**Fair warning**: Singleton is considered an anti-pattern by many modern developers. Prototype is rarely used in Java. But understanding both (and knowing when NOT to use them) is important for your growth as an engineer.

---

## Singleton Pattern

### Intent

**Ensure a class has only one instance and provide a global point of access to it.**

### The Classic Implementation

```java
public class Singleton {
    // Private static instance
    private static Singleton instance;

    // Private constructor prevents instantiation
    private Singleton() {
    }

    // Public static method to get instance
    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}

// Usage
Singleton obj1 = Singleton.getInstance();
Singleton obj2 = Singleton.getInstance();
// obj1 == obj2  (same instance!)
```

### Thread-Safe Singleton

The classic implementation isn't thread-safe. Here are proper implementations:

**Eager Initialization**:
```java
public class EagerSingleton {
    // Created at class loading time
    private static final EagerSingleton INSTANCE = new EagerSingleton();

    private EagerSingleton() {
    }

    public static EagerSingleton getInstance() {
        return INSTANCE;
    }
}
```

**Double-Checked Locking**:
```java
public class ThreadSafeSingleton {
    private static volatile ThreadSafeSingleton instance;

    private ThreadSafeSingleton() {
    }

    public static ThreadSafeSingleton getInstance() {
        if (instance == null) {  // First check (no locking)
            synchronized (ThreadSafeSingleton.class) {
                if (instance == null) {  // Second check (with locking)
                    instance = new ThreadSafeSingleton();
                }
            }
        }
        return instance;
    }
}
```

**Bill Pugh Singleton (Best)**:
```java
public class BillPughSingleton {
    private BillPughSingleton() {
    }

    // Inner static helper class
    private static class SingletonHelper {
        private static final BillPughSingleton INSTANCE = new BillPughSingleton();
    }

    public static BillPughSingleton getInstance() {
        return SingletonHelper.INSTANCE;
    }
}
```

**Enum Singleton (Most Secure)**:
```java
public enum EnumSingleton {
    INSTANCE;

    public void doSomething() {
        // Singleton operations
    }
}

// Usage
EnumSingleton.INSTANCE.doSomething();
```

---

## Why Singleton is Controversial

### Problem 1: Global State

Singletons are global variables in disguise:

```java
public class BookManager {
    public void processBook() {
        // Hidden dependency on global state
        Configuration config = Configuration.getInstance();

        // What is this method's behavior?
        // Depends on global state - hard to reason about!
    }
}
```

**Issues**:
- Hidden dependencies (not in constructor/parameters)
- Hard to test (global state persists between tests)
- Tight coupling (every class depends on singleton)
- Violates dependency injection principles

### Problem 2: Makes Testing Difficult

```java
@Test
void testBookProcessing() {
    Configuration.getInstance().setMode("test");  // Affects global state

    bookManager.processBook();

    // Next test might fail because of leftover state!
}
```

**Solution**: Use dependency injection instead.

### Problem 3: Violates Single Responsibility Principle

A singleton class has TWO responsibilities:
1. Its actual business logic
2. Managing its own lifecycle (ensuring single instance)

This violates SRP.

### Problem 4: Hard to Extend

```java
public class DatabaseConnection {
    private static DatabaseConnection instance;

    private DatabaseConnection() {
        // Connect to database
    }

    // What if I want to subclass this? Can't - private constructor!
}
```

---

## When Singleton is Acceptable

Despite the problems, Singleton can be appropriate in limited cases:

### 1. Logging

```java
public class Logger {
    private static final Logger INSTANCE = new Logger();

    private Logger() {
        // Initialize logging
    }

    public static Logger getInstance() {
        return INSTANCE;
    }

    public void log(String message) {
        // Log to file/console
    }
}
```

**Why OK**: Logging is inherently global, stateless operation.

### 2. Configuration (Read-Only)

```java
public class AppConfiguration {
    private static final AppConfiguration INSTANCE = new AppConfiguration();

    private final Properties properties;

    private AppConfiguration() {
        properties = loadProperties();  // Load once
    }

    public static AppConfiguration getInstance() {
        return INSTANCE;
    }

    public String get(String key) {
        return properties.getProperty(key);
    }
}
```

**Why OK**: Read-only, initialized once, never changes.

### 3. Resource Pools

```java
public class ConnectionPool {
    private static final ConnectionPool INSTANCE = new ConnectionPool();

    private final Queue<Connection> pool;

    private ConnectionPool() {
        pool = initializePool();
    }

    public static ConnectionPool getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() {
        return pool.poll();
    }
}
```

**Why OK**: Managing shared, limited resources.

---

## Singleton in Spring (The Right Way)

In Spring, **beans are singletons by default**, but Spring manages the lifecycle:

```java
@Service
public class BookService {
    // Spring creates ONE instance
    // Spring injects it wherever needed
    // Testable via constructor injection
}

@Repository
public class BookRepository {
    // Also a singleton (default scope)
}
```

**This is NOT the Singleton pattern!** It's **dependency injection with singleton scope**.

**Benefits**:
- No global state (dependencies injected)
- Testable (can inject mocks)
- Spring manages lifecycle
- Can change scope (@Scope("prototype"))

### When to Change Scope

```java
@Service
@Scope("prototype")  // New instance for each injection
public class ReportGenerator {
    // Stateful - needs new instance each time
}

@Service
@Scope("request")  // New instance per HTTP request
public class RequestContext {
    // Request-scoped state
}
```

---

## Singleton in Bibby: What NOT to Do

### ❌ Bad Example

```java
// DON'T DO THIS
public class LibraryManager {
    private static LibraryManager instance;
    private List<Book> books;  // Mutable state!

    private LibraryManager() {
        books = new ArrayList<>();
    }

    public static LibraryManager getInstance() {
        if (instance == null) {
            instance = new LibraryManager();
        }
        return instance;
    }

    public void addBook(Book book) {
        books.add(book);  // Modifying global state!
    }
}
```

**Problems**:
- Global mutable state
- Can't test in isolation
- Hard to reason about
- Violates DDD principles

### ✓ Better Approach

```java
// Use Spring DI instead
@Service
public class LibraryService {
    private final BookRepository bookRepository;

    // Dependencies injected
    public LibraryService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public void addBook(Book book) {
        bookRepository.save(book);  // State in database, not memory
    }
}
```

**Benefits**:
- Testable (inject mock repository)
- No global state
- Clear dependencies
- Follows DDD

---

## Prototype Pattern

### Intent

**Specify the kinds of objects to create using a prototypical instance, and create new objects by copying this prototype.**

### The Classic Implementation

```java
public interface Cloneable {
    Object clone();
}

public class Book implements Cloneable {
    private String title;
    private String isbn;

    public Book(String title, String isbn) {
        this.title = title;
        this.isbn = isbn;
    }

    @Override
    public Book clone() {
        try {
            return (Book) super.clone();  // Shallow copy
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

// Usage
Book original = new Book("Clean Code", "978-0132350884");
Book copy = original.clone();  // Creates a copy

// copy != original (different objects)
// copy.equals(original) (same content)
```

### Deep Copy vs. Shallow Copy

**Shallow Copy**: Copies primitive fields, references point to same objects
**Deep Copy**: Copies everything, including referenced objects

```java
public class Book implements Cloneable {
    private String title;
    private List<String> authors;  // Reference type

    @Override
    public Book clone() {
        try {
            Book cloned = (Book) super.clone();
            // Deep copy the list
            cloned.authors = new ArrayList<>(this.authors);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
```

---

## When to Use Prototype

### Use Case 1: Expensive Object Creation

If creating an object is expensive (complex initialization, database calls), clone a prototype instead:

```java
public class DatabaseConnection implements Cloneable {
    private Configuration config;  // Expensive to load

    public DatabaseConnection() {
        config = loadConfigurationFromDisk();  // Slow!
    }

    @Override
    public DatabaseConnection clone() {
        DatabaseConnection cloned = new DatabaseConnection();
        cloned.config = this.config;  // Reuse loaded config
        return cloned;
    }
}

// Create prototype once
DatabaseConnection prototype = new DatabaseConnection();

// Clone instead of creating new
DatabaseConnection conn1 = prototype.clone();  // Fast!
DatabaseConnection conn2 = prototype.clone();  // Fast!
```

### Use Case 2: Creating Variations

```java
public class DocumentTemplate implements Cloneable {
    private String header;
    private String footer;
    private String content;

    @Override
    public DocumentTemplate clone() {
        // Clone implementation
    }
}

// Create base template
DocumentTemplate baseTemplate = new DocumentTemplate();
baseTemplate.setHeader("Company Name");
baseTemplate.setFooter("Copyright 2024");

// Create variations
DocumentTemplate reportTemplate = baseTemplate.clone();
reportTemplate.setContent("Report Content");

DocumentTemplate invoiceTemplate = baseTemplate.clone();
invoiceTemplate.setContent("Invoice Content");
```

---

## Prototype in Bibby

Prototype is rarely needed in Bibby, but here's where it might apply:

### Use Case: Book Template for Series

```java
// If you're adding a book series with similar metadata
public class Book implements Cloneable {
    // Fields...

    @Override
    public Book clone() {
        return Book.builder()
            .id(BookId.generate())  // New ID!
            .title(this.title)
            .isbn(this.isbn)  // Will change per book
            .authors(new HashSet<>(this.authorIds))
            .publisher(this.publisher)  // Same publisher
            .publicationYear(this.publicationYear)  // Might be same
            .build();
    }
}

// Usage
Book designPatternsBase = Book.builder()
    .title("Design Patterns")
    .authors(gof)
    .publisher(new Publisher("Addison-Wesley"))
    .build();

// Clone for series books
Book volume1 = designPatternsBase.clone();
volume1.setIsbn(ISBN.fromString("978-0201633610"));
volume1.setTitle(new Title("Design Patterns: Elements"));

Book volume2 = designPatternsBase.clone();
volume2.setIsbn(ISBN.fromString("978-0201633627"));
volume2.setTitle(new Title("Design Patterns: Advanced"));
```

**Reality**: In Bibby, you'd probably just use a builder or factory. Prototype adds complexity without much benefit.

---

## Copy Constructors: The Java Way

Instead of Prototype pattern, Java idiom prefers **copy constructors**:

```java
public class Book {
    private final BookId id;
    private final Title title;
    private final ISBN isbn;

    // Regular constructor
    public Book(BookId id, Title title, ISBN isbn) {
        this.id = id;
        this.title = title;
        this.isbn = isbn;
    }

    // Copy constructor
    public Book(Book other) {
        this.id = BookId.generate();  // New ID
        this.title = other.title;
        this.isbn = other.isbn;
    }
}

// Usage
Book original = new Book(id, title, isbn);
Book copy = new Book(original);  // Explicit copy
```

**Benefits over clone()**:
- No need to implement Cloneable
- Type-safe (no casting)
- Clear intent
- Can customize what gets copied

---

## Action Items

### 1. Audit Your Code for Singletons (1-2 hours)

Search your codebase:
- Find any static instances
- Find getInstance() methods
- Identify global state

Document:
- Which are legitimate singletons?
- Which should use Spring DI instead?

### 2. Refactor to Spring DI (2-3 hours)

If you have any home-grown singletons:
- Convert to Spring @Service
- Use constructor injection
- Remove static methods
- Add tests

### 3. Understand Spring Scopes (1 hour)

Experiment with:
```java
@Service  // Default: singleton
@Service @Scope("prototype")  // New instance each injection
@Service @Scope("request")  // HTTP request scope
```

Create a simple service with different scopes and observe behavior.

### 4. Implement Copy Constructor (1-2 hours)

For your Book or Author class:
- Add copy constructor
- Test it
- Compare to builder pattern

Decide which is more appropriate for your use case.

### 5. Research Real-World Usage (1 hour)

Study:
- How Spring manages bean lifecycle
- When clone() is actually used in Java standard library
- Alternatives to Singleton pattern

---

## Key Takeaways

### 1. Singleton is Often an Anti-Pattern
- Global state is bad
- Makes testing hard
- Violates SRP and DI
- Use Spring DI instead

### 2. Spring Beans Are Not Singleton Pattern
- Dependency injection with singleton scope
- Testable, manageable lifecycle
- Can change scope if needed

### 3. Prototype Pattern is Rarely Needed in Java
- Java prefers copy constructors
- Builder pattern often better
- Only use for truly expensive object creation

### 4. Know When NOT to Use Patterns
- Don't use Singleton for convenience
- Don't use Prototype without good reason
- Prefer simpler solutions

### 5. Spring Handles Most Cases
- Use @Service, @Repository, @Component
- Let Spring manage lifecycle
- Use @Scope when you need different behavior

---

## Further Study

**Books**:
- "Design Patterns" by Gang of Four - Singleton (127), Prototype (117)
- "Effective Java" by Joshua Bloch - Item 3 (Singleton), Item 13 (Clone)
- "Spring in Action" - Bean scopes

**Articles**:
- "Singleton Pattern Considered Harmful"
- "Why Singletons are Evil"
- Spring documentation on bean scopes

---

## Mentor's Note

I've seen many junior engineers reach for Singleton because it seems easy. "Just make it static and I can access it anywhere!" But this is a trap.

**Real-world truth**:
- I've never written a Singleton in the last 5 years
- Every codebase I've inherited with Singletons was hard to test
- Spring DI solves 99% of cases where you'd think "I need a Singleton"

**When students ask "Should I use Singleton?"**, my answer is almost always: "No. Use Spring DI."

The value of learning Singleton isn't to use it—it's to:
1. Recognize it when you see it
2. Know why it's problematic
3. Refactor it away

Same with Prototype - know it exists, understand when it might apply, but reach for simpler solutions first.

In Section 11, we move to **Structural Patterns**: Adapter and Facade. These are FAR more useful in day-to-day development.

---

**Section 10 Complete** | Next: Section 11 - Structural Patterns: Adapter & Facade
