# SECTION 12: DECORATOR & PROXY PATTERNS

## Structural Patterns for Enhanced Functionality

Welcome to Section 12! We're continuing our journey through the Gang of Four design patterns. In this section, we'll explore two powerful structural patterns: **Decorator** and **Proxy**. Both patterns involve wrapping objects to add behavior, but they serve different purposes.

These patterns are particularly useful in your Bibby application for adding display formatting, caching external API calls, lazy loading, and access control.

---

## The Decorator Pattern

### What is the Decorator Pattern?

**Definition**: The Decorator pattern attaches additional responsibilities to an object dynamically. Decorators provide a flexible alternative to subclassing for extending functionality.

**Key Insight**: Instead of creating dozens of subclasses for every combination of features, you wrap objects with decorators to add behavior at runtime.

### The Problem: Class Explosion

Imagine you want to display books in different formats:
- Just the title
- Title + authors
- Title + ISBN
- Title + authors + ISBN
- Title + publication year
- Title + authors + ISBN + publication year

**Without Decorator**, you'd need:
```java
class BasicBookDisplay { }
class BookDisplayWithAuthors { }
class BookDisplayWithIsbn { }
class BookDisplayWithAuthorsAndIsbn { }
class BookDisplayWithAuthorsIsbnAndYear { }
// ... 15+ more classes!
```

This is **class explosion** - creating a new subclass for every combination. It's unmaintainable.

### The Solution: Stack Decorators

With the Decorator pattern, you create small, focused decorators that can be stacked:

```java
BookDisplay display = new BasicBookDisplay(book);
display = new WithAuthorsDecorator(display);
display = new WithIsbnDecorator(display);
display = new WithYearDecorator(display);
```

Each decorator adds one responsibility. You combine them as needed.

---

## Decorator Pattern Structure

### Components

1. **Component Interface**: Common interface for both base objects and decorators
2. **Concrete Component**: The base object being decorated
3. **Decorator Base Class**: Abstract decorator that implements the component interface
4. **Concrete Decorators**: Specific decorators that add behavior

### UML Diagram

```
┌─────────────────┐
│  BookDisplay    │  (Component Interface)
│  -----------    │
│  + display()    │
└────────┬────────┘
         │
         ├──────────────────────┬──────────────────────┐
         │                      │                      │
┌────────▼────────┐    ┌────────▼──────────┐  ┌──────▼──────────┐
│ BasicBookDisplay│    │ BookDisplayDecorator│  (Abstract Decorator)
└─────────────────┘    └────────┬──────────┘
                                │
                    ┌───────────┼──────────┐
                    │           │          │
            ┌───────▼──┐  ┌─────▼────┐  ┌─▼───────┐
            │WithAuthors│  │WithIsbn  │  │WithYear │
            └───────────┘  └──────────┘  └─────────┘
```

---

## Implementing Decorator in Bibby

### Step 1: Component Interface

```java
package com.penrose.bibby.library.book.display;

public interface BookDisplay {
    String display();
}
```

### Step 2: Concrete Component (Base Object)

```java
package com.penrose.bibby.library.book.display;

public class BasicBookDisplay implements BookDisplay {
    private final Book book;

    public BasicBookDisplay(Book book) {
        this.book = book;
    }

    @Override
    public String display() {
        return book.getTitle().getValue();
    }
}
```

### Step 3: Abstract Decorator

```java
package com.penrose.bibby.library.book.display;

public abstract class BookDisplayDecorator implements BookDisplay {
    protected final BookDisplay decoratedDisplay;

    public BookDisplayDecorator(BookDisplay display) {
        this.decoratedDisplay = display;
    }

    @Override
    public String display() {
        return decoratedDisplay.display();
    }
}
```

**Key Point**: The decorator **wraps** another `BookDisplay` object and delegates to it.

### Step 4: Concrete Decorators

```java
package com.penrose.bibby.library.book.display;

public class WithAuthorsDecorator extends BookDisplayDecorator {
    private final Book book;

    public WithAuthorsDecorator(BookDisplay display, Book book) {
        super(display);
        this.book = book;
    }

    @Override
    public String display() {
        String base = decoratedDisplay.display();
        String authors = book.getAuthors().stream()
            .map(Author::getName)
            .collect(Collectors.joining(", "));
        return base + " by " + authors;
    }
}
```

```java
public class WithIsbnDecorator extends BookDisplayDecorator {
    private final Book book;

    public WithIsbnDecorator(BookDisplay display, Book book) {
        super(display);
        this.book = book;
    }

    @Override
    public String display() {
        String base = decoratedDisplay.display();
        return base + " [ISBN: " + book.getIsbn().getValue() + "]";
    }
}
```

```java
public class WithPublicationYearDecorator extends BookDisplayDecorator {
    private final Book book;

    public WithPublicationYearDecorator(BookDisplay display, Book book) {
        super(display);
        this.book = book;
    }

    @Override
    public String display() {
        String base = decoratedDisplay.display();
        return base + " (" + book.getPublicationYear() + ")";
    }
}
```

### Step 5: Usage

```java
// Just title
BookDisplay display = new BasicBookDisplay(book);
System.out.println(display.display());
// Output: "Clean Code"

// Title + authors
display = new BasicBookDisplay(book);
display = new WithAuthorsDecorator(display, book);
System.out.println(display.display());
// Output: "Clean Code by Robert C. Martin"

// Title + authors + ISBN
display = new BasicBookDisplay(book);
display = new WithAuthorsDecorator(display, book);
display = new WithIsbnDecorator(display, book);
System.out.println(display.display());
// Output: "Clean Code by Robert C. Martin [ISBN: 978-0132350884]"

// All decorators
display = new BasicBookDisplay(book);
display = new WithAuthorsDecorator(display, book);
display = new WithIsbnDecorator(display, book);
display = new WithPublicationYearDecorator(display, book);
System.out.println(display.display());
// Output: "Clean Code by Robert C. Martin [ISBN: 978-0132350884] (2008)"
```

**Beautiful!** You can mix and match decorators without creating dozens of subclasses.

---

## When to Use Decorator

**Use Decorator when:**
1. You need to add responsibilities to objects dynamically
2. Extension by subclassing is impractical (too many combinations)
3. You want to add behavior without affecting other objects
4. You need to add/remove responsibilities at runtime

**Real-world examples:**
- Java I/O streams: `new BufferedReader(new FileReader(file))`
- Spring's `@Transactional`, `@Cacheable` (via proxies, similar concept)
- UI components with borders, scrollbars, etc.

---

## The Proxy Pattern

### What is the Proxy Pattern?

**Definition**: Provide a surrogate or placeholder for another object to control access to it.

**Key Insight**: A proxy looks like the real object but adds control logic before/after delegating to it.

### Types of Proxies

1. **Virtual Proxy**: Lazy loading of expensive objects
2. **Protection Proxy**: Access control and permissions
3. **Remote Proxy**: Represents an object in a different address space (RMI, REST)
4. **Cache Proxy**: Cache results of expensive operations

---

## Virtual Proxy: Lazy Loading Book Metadata

Your Bibby app might fetch book metadata from external APIs (e.g., Open Library). API calls are slow and expensive.

**Problem**: Don't want to call the API every time you need metadata.

**Solution**: Use a **Virtual Proxy** with caching.

### Without Proxy (Always Hits API)

```java
public class ExternalApiMetadataProvider implements BookMetadataProvider {
    private final RestTemplate restTemplate;

    @Override
    public BookMetadata getMetadata(ISBN isbn) {
        // Expensive API call every time!
        String url = "https://openlibrary.org/api/books?bibkeys=ISBN:" + isbn.getValue();
        ApiResponse response = restTemplate.getForObject(url, ApiResponse.class);
        return mapToBookMetadata(response);
    }
}
```

Every call to `getMetadata` hits the external API. Slow!

### With Virtual Proxy (Caching)

```java
// Subject interface
public interface BookMetadataProvider {
    BookMetadata getMetadata(ISBN isbn);
}

// Real subject
public class ExternalApiMetadataProvider implements BookMetadataProvider {
    private final RestTemplate restTemplate;

    @Override
    public BookMetadata getMetadata(ISBN isbn) {
        log.info("Fetching metadata from API for ISBN: {}", isbn);
        String url = "https://openlibrary.org/api/books?bibkeys=ISBN:" + isbn.getValue();
        ApiResponse response = restTemplate.getForObject(url, ApiResponse.class);
        return mapToBookMetadata(response);
    }
}

// Proxy with caching
public class CachedMetadataProxy implements BookMetadataProvider {
    private final BookMetadataProvider realProvider;
    private final Map<ISBN, BookMetadata> cache = new ConcurrentHashMap<>();

    public CachedMetadataProxy(BookMetadataProvider realProvider) {
        this.realProvider = realProvider;
    }

    @Override
    public BookMetadata getMetadata(ISBN isbn) {
        // Check cache first
        if (cache.containsKey(isbn)) {
            log.info("Cache hit for ISBN: {}", isbn);
            return cache.get(isbn);
        }

        // Cache miss - delegate to real provider
        log.info("Cache miss for ISBN: {}", isbn);
        BookMetadata metadata = realProvider.getMetadata(isbn);
        cache.put(isbn, metadata);
        return metadata;
    }

    public void clearCache() {
        cache.clear();
    }
}
```

### Usage

```java
BookMetadataProvider provider = new CachedMetadataProxy(
    new ExternalApiMetadataProvider(restTemplate)
);

// First call - hits API
BookMetadata metadata1 = provider.getMetadata(isbn);  // Slow

// Second call - returns cached
BookMetadata metadata2 = provider.getMetadata(isbn);  // Fast!
```

**Benefit**: Transparent caching without changing the API client code.

---

## Protection Proxy: Access Control

Suppose you add user roles to Bibby (Admin, Member, Guest). Only admins can delete books.

### Protection Proxy Example

```java
// Subject interface
public interface BookRepository {
    Book findById(BookId id);
    void save(Book book);
    void delete(BookId id);
}

// Real subject
public class JpaBookRepository implements BookRepository {
    @Override
    public void delete(BookId id) {
        entityManager.remove(entityManager.find(BookEntity.class, id.getValue()));
    }
    // ... other methods
}

// Protection proxy
public class SecureBookRepositoryProxy implements BookRepository {
    private final BookRepository realRepository;
    private final UserContext userContext;

    @Override
    public void delete(BookId id) {
        if (!userContext.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Only admins can delete books");
        }
        realRepository.delete(id);
    }

    @Override
    public void save(Book book) {
        if (!userContext.hasRole(Role.ADMIN) && !userContext.hasRole(Role.MEMBER)) {
            throw new AccessDeniedException("Guests cannot modify books");
        }
        realRepository.save(book);
    }

    @Override
    public Book findById(BookId id) {
        // Anyone can read
        return realRepository.findById(id);
    }
}
```

**Usage**:
```java
BookRepository repository = new SecureBookRepositoryProxy(
    new JpaBookRepository(),
    userContext
);

repository.delete(bookId);  // Throws AccessDeniedException if not admin
```

---

## Spring AOP: Dynamic Proxies

Spring uses **dynamic proxies** extensively for `@Transactional`, `@Cacheable`, `@Async`, etc.

### Example: @Cacheable

```java
@Service
public class BookMetadataService {

    @Cacheable(value = "bookMetadata", key = "#isbn")
    public BookMetadata getMetadata(ISBN isbn) {
        // Expensive operation
        return callExternalApi(isbn);
    }
}
```

**What happens**: Spring creates a **proxy** around `BookMetadataService` that:
1. Checks cache before calling method
2. If cache hit, returns cached value
3. If cache miss, calls real method and caches result

This is the **Proxy pattern** in action!

---

## Decorator vs Proxy

**Similarities**:
- Both wrap another object
- Both implement the same interface

**Differences**:

| Aspect | Decorator | Proxy |
|--------|-----------|-------|
| **Intent** | Add behavior/responsibilities | Control access |
| **Focus** | Enhance functionality | Manage access, lazy loading, caching |
| **Awareness** | Client knows it's decorating | Client thinks it's the real object |
| **Stacking** | Can stack multiple decorators | Usually single proxy |
| **Example** | `BufferedReader(FileReader)` | `@Cacheable`, lazy loading |

---

## Action Items

### 1. Implement Book Display Decorators (2-3 hours)

Create the decorator pattern for book display:
- `BookDisplay` interface
- `BasicBookDisplay` concrete component
- `WithAuthorsDecorator`, `WithIsbnDecorator`, `WithPublicationYearDecorator`
- Test with different combinations

**Bonus**: Add `WithGenreDecorator`, `WithLocationDecorator` (shelf/bookcase).

### 2. Implement Metadata Caching Proxy (2-3 hours)

Create a caching proxy for external API calls:
- `BookMetadataProvider` interface
- `ExternalApiMetadataProvider` real subject
- `CachedMetadataProxy` with `ConcurrentHashMap` cache
- Test cache hits and misses
- Add cache expiration (use Guava Cache or Caffeine)

### 3. Refactor Book Display Command (1-2 hours)

Update your Spring Shell `book-display` command to use decorators:
```java
@ShellMethod("Display book details")
public String displayBook(
    @ShellOption Long id,
    @ShellOption(defaultValue = "false") boolean showAuthors,
    @ShellOption(defaultValue = "false") boolean showIsbn
) {
    Book book = bookRepository.findById(new BookId(id))
        .orElseThrow(() -> new BookNotFoundException(id));

    BookDisplay display = new BasicBookDisplay(book);

    if (showAuthors) {
        display = new WithAuthorsDecorator(display, book);
    }
    if (showIsbn) {
        display = new WithIsbnDecorator(display, book);
    }

    return display.display();
}
```

### 4. Study Spring AOP Proxies (1-2 hours)

Read about Spring's proxy mechanism:
- How `@Transactional` works
- JDK dynamic proxies vs CGLIB proxies
- Why `@Transactional` on private methods doesn't work (it's a proxy!)

---

## Key Takeaways

### 1. Decorator Adds Behavior, Proxy Controls Access
- **Decorator**: Enhance functionality (add authors, ISBN, formatting)
- **Proxy**: Control access (caching, lazy loading, security)

### 2. Avoid Class Explosion
- Don't create `BookDisplayWithAuthors`, `BookDisplayWithAuthorsAndIsbn`, etc.
- Use small, composable decorators instead

### 3. Proxy is Transparent
- Client code doesn't know it's talking to a proxy
- Proxy looks and acts like the real object

### 4. Spring Uses Proxies Everywhere
- `@Transactional`, `@Cacheable`, `@Async` all use dynamic proxies
- Understanding proxies helps debug Spring "magic"

### 5. Composition Over Inheritance
- Both patterns favor composition (wrapping) over inheritance (subclassing)
- More flexible and maintainable

---

## Further Study

**Books**:
- "Design Patterns" by Gang of Four - Decorator (p. 175), Proxy (p. 207)
- "Head First Design Patterns" - Very visual explanations

**Articles**:
- Baeldung: "Decorator Pattern in Java"
- Baeldung: "Proxy Pattern in Java"
- Spring AOP documentation (proxies)

**Code Examples**:
- Java I/O: `BufferedReader`, `BufferedWriter` (decorators)
- Spring Framework source code: `@Cacheable` implementation

---

## Mentor's Notes

You'll use **Decorator** when you need flexible, stackable enhancements. Think I/O streams, UI components, formatting.

You'll use **Proxy** when you need control: caching, security, lazy loading. Spring does this for you with annotations, but understanding the pattern helps you debug and extend it.

Both patterns are about **wrapping objects**. Master them, and you'll write cleaner, more flexible code.

Next up: **Composite** and **Bridge** patterns!

---

**Section 12 Complete** | **Next**: Section 13 - Composite & Bridge Patterns
