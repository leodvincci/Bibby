# Section 5: OOP - Encapsulation & Abstraction

Welcome to Section 5! We're diving into two of the four pillars of Object-Oriented Programming: **Encapsulation** and **Abstraction**. These aren't just academic concepts - they're practical tools that determine whether your code is fragile or resilient, tightly coupled or flexible.

Your Bibby application demonstrates both good and problematic examples of these principles. Let's learn from your actual code!

---

## What Is Encapsulation?

**Encapsulation** means **bundling data with the methods that operate on that data**, and **restricting direct access** to some of an object's components.

**The Core Idea:**
- Hide the internal state of an object
- Expose only what's necessary through well-defined interfaces
- Protect object invariants (rules about valid states)

**Real-World Analogy:**
A car encapsulates its engine. You interact through the steering wheel, pedals, and dashboard - not by directly manipulating pistons and spark plugs. The internal complexity is hidden.

---

## Access Modifiers: The Four Levels

Java provides four levels of access control:

| Modifier | Class | Package | Subclass | World |
|----------|-------|---------|----------|-------|
| **private** | ✅ | ❌ | ❌ | ❌ |
| **(package-private)** | ✅ | ✅ | ❌ | ❌ |
| **protected** | ✅ | ✅ | ✅ | ❌ |
| **public** | ✅ | ✅ | ✅ | ✅ |

**Default (no modifier)** = Package-private

**When to Use Each:**
- **private** - Internal implementation details, helper methods, fields
- **package-private** - Classes/methods used within same package only
- **protected** - For inheritance (subclasses need access)
- **public** - Public API, meant for external use

**The Principle:** Use the **most restrictive** access level that makes sense. Default to private, open up only when necessary.

---

## In Your Code: Encapsulation Violations

### ⚠️ Package-Private Fields (Accidental Exposure)

📁 File: `src/main/java/com/penrose/bibby/library/genre/Genre.java`
📍 Lines: 5-7

```java
public class Genre {

    private Long id;        // ✅ Private
    String genreName;       // ❌ Package-private! Missing modifier
    String genreDescription; // ❌ Package-private! Missing modifier
```

**The Problem:**

You forgot the `private` modifier on two fields! This means:

```java
// In any class in the same package (com.penrose.bibby.library.genre):
Genre genre = new Genre("Fiction", "...");
genre.genreName = "Science Fiction"; // ← Direct field access! Bypasses setter!
```

**Why This Is Dangerous:**

1. **Bypasses Validation** - If you add validation to `setGenreName()` later, direct access bypasses it
2. **Breaks Encapsulation** - Internal state is exposed
3. **Tight Coupling** - Other code depends on internal representation
4. **Harder to Refactor** - Can't change field name without breaking other classes

**Refactored:**

```java
public class Genre {

    private Long id;
    private String genreName;        // ✅ Now private
    private String genreDescription; // ✅ Now private

    public Genre(String genreName, String genreDescription) {
        this.genreName = genreName;
        this.genreDescription = genreDescription;
    }

    // Getters and setters provide controlled access
    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        // Can add validation here
        if (genreName == null || genreName.isBlank()) {
            throw new IllegalArgumentException("Genre name cannot be blank");
        }
        this.genreName = genreName.trim();
    }

    public String getGenreDescription() {
        return genreDescription;
    }

    public void setGenreDescription(String genreDescription) {
        this.genreDescription = genreDescription;
    }
}
```

**What Changed:**
- ✅ All fields are `private` (consistent access control)
- ✅ Access only through getters/setters (controlled interface)
- ✅ Can add validation without breaking existing code
- ✅ Can change internal representation later

---

### ⚠️ Package-Private Service Field

📁 File: `src/main/java/com/penrose/bibby/library/shelf/ShelfService.java`
📍 Lines: 14

```java
@Service
public class ShelfService {

    ShelfRepository shelfRepository; // ❌ Package-private!
```

**The Problem:**

The `shelfRepository` field is package-private (no modifier). This means:

```java
// Any class in com.penrose.bibby.library.shelf package:
ShelfService service = ...;
service.shelfRepository = null; // ← Can modify directly! Very dangerous!
service.shelfRepository = new FakeRepository(); // ← Can swap implementation!
```

**Why This Is Critical:**

1. **Breaks Dependency Injection** - Spring injects via constructor, but field can be changed afterward
2. **No Immutability** - Can't make field `final` without private
3. **Testing Nightmare** - Tests could accidentally modify the repository
4. **Violates Encapsulation** - Internal dependency is exposed

**Refactored:**

```java
@Service
public class ShelfService {

    private final ShelfRepository shelfRepository; // ✅ Private and final

    public ShelfService(ShelfRepository shelfRepository) {
        this.shelfRepository = shelfRepository;
    }

    // ... methods ...
}
```

**What Changed:**
- ✅ `private` - Only ShelfService can access
- ✅ `final` - Can't be reassigned after construction
- ✅ Immutable dependency - Thread-safe, predictable

---

### ✅ Proper Encapsulation Example

📁 File: `src/main/java/com/penrose/bibby/library/book/BookService.java`
📍 Lines: 14-15

```java
final BookRepository bookRepository;
private final AuthorRepository authorRepository;
```

**Why This Is Good:**

- ✅ `private` modifier (bookRepository missing private but `final` helps)
- ✅ `final` modifier (immutable after construction)
- ✅ Constructor injection (controlled initialization)

**Minor Improvement:**

```java
private final BookRepository bookRepository;    // Add private
private final AuthorRepository authorRepository;
```

Consistency matters! Both should be `private final`.

---

## The Problem with Anemic Domain Models

Your entities expose all internal state via getters/setters. This is called an **"Anemic Domain Model"** - objects that are just data containers with no behavior.

### ⚠️ Anemic Entity Example

📁 File: `src/main/java/com/penrose/bibby/library/book/BookEntity.java`

```java
@Entity
@Table(name = "books")
public class BookEntity {
    private Long bookId;
    private String title;
    private String bookStatus;
    private Integer checkoutCount;

    // Just getters and setters - no business logic!
    public Long getBookId() { return bookId; }
    public void setBookId(Long id) { this.bookId = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBookStatus() { return bookStatus; }
    public void setBookStatus(String bookStatus) { this.bookStatus = bookStatus; }
    public Integer getCheckoutCount() { return checkoutCount; }
    public void setCheckoutCount(Integer count) { this.checkoutCount = count; }
}
```

**The Problem:**

Business logic lives in the service layer:

```java
// BookService.java
if (!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())) {
    bookEntity.setBookStatus("CHECKED_OUT");
    bookRepository.save(bookEntity);
}
```

This violates encapsulation because:
- Business rules are scattered across service classes
- BookEntity can be put in invalid states
- Difficult to ensure invariants

---

### 🚀 Rich Domain Model (Better Encapsulation)

**Refactored BookEntity:**

```java
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookId;

    private String title;

    @Enumerated(EnumType.STRING)
    private BookStatus bookStatus = BookStatus.AVAILABLE; // Default

    private Integer checkoutCount = 0; // Default

    // Private setters - only entity can modify itself
    private void setBookStatus(BookStatus status) {
        this.bookStatus = status;
    }

    private void incrementCheckoutCount() {
        this.checkoutCount++;
    }

    // Public behavior methods that enforce business rules
    public void checkOut() {
        if (this.bookStatus == BookStatus.CHECKED_OUT) {
            throw new IllegalStateException("Book is already checked out: " + this.title);
        }
        if (this.bookStatus == BookStatus.LOST) {
            throw new IllegalStateException("Cannot check out a lost book: " + this.title);
        }

        this.setBookStatus(BookStatus.CHECKED_OUT);
        this.incrementCheckoutCount();
    }

    public void checkIn() {
        if (this.bookStatus != BookStatus.CHECKED_OUT) {
            throw new IllegalStateException("Book is not checked out: " + this.title);
        }

        this.setBookStatus(BookStatus.AVAILABLE);
    }

    public void markAsLost() {
        this.setBookStatus(BookStatus.LOST);
    }

    public boolean isAvailable() {
        return this.bookStatus == BookStatus.AVAILABLE;
    }

    public boolean isCheckedOut() {
        return this.bookStatus == BookStatus.CHECKED_OUT;
    }

    // Read-only getters for state
    public Long getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public BookStatus getBookStatus() {
        return bookStatus; // Return enum, not String
    }

    public Integer getCheckoutCount() {
        return checkoutCount;
    }

    // Only needed for JPA
    protected BookEntity() {
    }

    public BookEntity(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        this.title = title.trim();
    }
}
```

**Now the Service is Simple:**

```java
// BookService.java
@Transactional
public void checkOutBook(Long bookId) {
    BookEntity book = bookRepository.findById(bookId)
        .orElseThrow(() -> new IllegalArgumentException("Book not found"));

    book.checkOut(); // Business logic is in the entity!

    bookRepository.save(book);
}

@Transactional
public void checkInBook(Long bookId) {
    BookEntity book = bookRepository.findById(bookId)
        .orElseThrow(() -> new IllegalArgumentException("Book not found"));

    book.checkIn(); // Business logic is in the entity!

    bookRepository.save(book);
}
```

**Benefits:**
- ✅ Business rules live with the data (Single Responsibility)
- ✅ Impossible to put BookEntity in invalid state
- ✅ Service layer is thin (orchestration, not business logic)
- ✅ Easy to test (test BookEntity.checkOut() directly)
- ✅ Self-documenting (methods describe what book can do)

---

## What Is Abstraction?

**Abstraction** means **hiding complex implementation details** and showing only the essential features of an object.

**Key Ideas:**
- Define "what" an object does, not "how" it does it
- Use interfaces and abstract classes to define contracts
- Program to interfaces, not implementations

**Real-World Analogy:**
You know a "save" button saves your work. You don't need to know whether it writes to disk, network, cloud, or database. The implementation is abstracted away.

---

## In Your Code: Good Abstractions

### ✅ Repository Interfaces (Excellent Abstraction)

📁 File: `src/main/java/com/penrose/bibby/library/author/AuthorRepository.java`
📍 Lines: 8-16

```java
@Repository
public interface AuthorRepository extends JpaRepository<AuthorEntity, Long> {

    AuthorEntity getByFullName(String fullName);

    AuthorEntity findByFirstNameAndLastName(String firstName, String lastName);

    List<AuthorEntity> findByBooks_BookId(Long bookId);
}
```

**Why This Is Excellent Abstraction:**

1. **Interface, Not Class** - Defines contract without implementation
2. **Spring Data Magic** - Implementation is generated at runtime
3. **Easy to Test** - Can mock this interface
4. **Easy to Swap** - Could change from JPA to MongoDB without changing interface
5. **Declarative** - Method names describe WHAT, not HOW

**Example Usage:**

```java
// Service depends on interface, not concrete class
@Service
public class AuthorService {
    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository; // Could be JPA, JDBC, or mock!
    }
}
```

**How Spring Generates Implementation:**

Spring Data JPA reads method names and creates queries:
- `findByFirstNameAndLastName` → `SELECT ... WHERE first_name = ? AND last_name = ?`
- `findByBooks_BookId` → Joins through the `books` relationship

You get **abstraction** (interface) without writing **implementation** (SQL)!

---

### ✅ Service Layer as Abstraction

📁 File: `src/main/java/com/penrose/bibby/library/shelf/ShelfService.java`

```java
@Service
public class ShelfService {

    ShelfRepository shelfRepository;

    public List<ShelfEntity> getAllShelves(Long bookCaseId){
        return shelfRepository.findByBookcaseId(bookCaseId);
    }

    public Optional<ShelfEntity> findShelfById(Long shelfId) {
        return shelfRepository.findById(shelfId);
    }

    public List<ShelfSummary> getShelfSummariesForBookcase(Long bookcaseId) {
        return shelfRepository.findShelfSummariesByBookcaseId(bookcaseId);
    }
}
```

**Why Services Are Abstractions:**

1. **Hides Repository Details** - Controllers don't know about JPA
2. **Transaction Boundaries** - Service defines transaction scope (could add @Transactional)
3. **Business Logic Layer** - Could add validation, authorization, caching
4. **Easier to Change** - Switch from SQL to NoSQL without changing controllers

**The Abstraction Layers:**

```
Controller (Web Layer)
    ↓ depends on
Service (Business Logic Abstraction)
    ↓ depends on
Repository (Data Access Abstraction)
    ↓ depends on
Database (Implementation Detail)
```

Each layer is **abstracted** from the one below.

---

### ⚠️ Leaky Abstraction Example

📁 File: `src/main/java/com/penrose/bibby/library/book/BookController.java`
📍 Lines: 18-22

```java
@PostMapping("api/v1/books")
public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
    bookService.createNewBook(requestDTO);
    return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
}
```

**This Looks Okay, But...**

The controller directly uses `BookRequestDTO.title()` instead of getting confirmation from the service. What if the service modifies the title (trimming, capitalizing, etc.)?

**Better Abstraction:**

```java
@PostMapping("api/v1/books")
public ResponseEntity<BookResponseDTO> addBook(@RequestBody BookRequestDTO requestDTO) {
    BookEntity created = bookService.createNewBook(requestDTO);

    // Map entity to response DTO (don't expose entity!)
    BookResponseDTO response = new BookResponseDTO(
        created.getBookId(),
        created.getTitle(),
        "Book added successfully"
    );

    return ResponseEntity.ok(response);
}
```

**Why This Is Better:**
- ✅ Service returns the created entity
- ✅ Controller converts to response DTO
- ✅ Entities don't leak to web layer
- ✅ Response reflects actual saved data

---

## The Liskov Substitution Principle (LSP)

Part of good abstraction is **substitutability**: if you have an interface or parent class, any implementation/subclass should work the same way.

**Example from Your Code:**

```java
// BookService depends on BookRepository (interface)
private final BookRepository bookRepository;

// Spring could inject ANY implementation:
// - JpaBookRepository (default)
// - MockBookRepository (for tests)
// - CachingBookRepository (with caching)
// - NetworkBookRepository (remote database)

// Service doesn't care - it just calls methods!
bookRepository.findById(id); // Works with any implementation
```

**The Principle:**
> If S is a subtype of T, then objects of type T may be replaced with objects of type S without altering program correctness.

In simpler terms: **Any implementation of an interface should behave according to the contract.**

---

## Information Hiding

**Information Hiding** is closely related to encapsulation. Hide:
- Internal representation (fields)
- Implementation details (how methods work)
- Complexity (expose simple interfaces)

### ⚠️ Exposing Internal Collections

📁 File: `src/main/java/com/penrose/bibby/library/book/BookEntity.java`
📍 Lines: 50-52

```java
public Set<AuthorEntity> getAuthors() {
    return authors; // ❌ Returns internal collection directly!
}
```

**The Danger:**

```java
BookEntity book = bookService.findBookById(1L);
Set<AuthorEntity> authors = book.getAuthors();

// Caller can modify the book's authors!
authors.clear(); // ← Just removed all authors from the book!
authors.add(someRandomAuthor); // ← Added author without going through addAuthor()
```

**Refactored (Defensive Copy):**

```java
public Set<AuthorEntity> getAuthors() {
    return new HashSet<>(authors); // Return a copy
}

// Or better - return unmodifiable view
public Set<AuthorEntity> getAuthors() {
    return Collections.unmodifiableSet(authors);
}

// Or best - don't expose collection at all, provide specific operations
public void addAuthor(AuthorEntity author) {
    if (author == null) {
        throw new IllegalArgumentException("Author cannot be null");
    }
    this.authors.add(author);
}

public void removeAuthor(AuthorEntity author) {
    this.authors.remove(author);
}

public boolean hasAuthor(AuthorEntity author) {
    return this.authors.contains(author);
}

public int getAuthorCount() {
    return this.authors.size();
}

// If you MUST expose, return unmodifiable
public Set<AuthorEntity> getAuthorsView() {
    return Collections.unmodifiableSet(authors);
}
```

---

## Access Modifier Guidelines for Your Project

### For Entities (JPA Classes)

```java
@Entity
public class BookEntity {
    @Id
    @GeneratedValue
    private Long id;              // ✅ Private

    private String title;         // ✅ Private

    @ManyToMany
    private Set<AuthorEntity> authors = new HashSet<>(); // ✅ Private

    // Public getters for read access
    public Long getId() { ... }
    public String getTitle() { ... }

    // Protected/private setters or behavior methods only
    protected void setTitle(String title) { ... } // For JPA/subclasses

    // Public behavior methods
    public void addAuthor(AuthorEntity author) { ... }

    // Protected no-arg constructor for JPA
    protected BookEntity() { }

    // Public constructor with required fields
    public BookEntity(String title) { ... }
}
```

### For Services

```java
@Service
public class BookService {
    private final BookRepository bookRepository;  // ✅ Private final
    private final AuthorRepository authorRepository;  // ✅ Private final

    // Public constructor for dependency injection
    public BookService(BookRepository bookRepository,
                       AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    // Public methods - service API
    public Optional<BookEntity> findById(Long id) { ... }

    @Transactional
    public BookEntity createBook(BookRequestDTO dto) { ... }

    // Private helper methods
    private AuthorEntity findOrCreateAuthor(String firstName, String lastName) { ... }
}
```

### For DTOs (Records)

```java
// Records have public constructor and getters by default
public record BookRequestDTO(
    String title,
    String firstName,
    String lastName
) {
    // Can add validation in compact constructor
    public BookRequestDTO {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title required");
        }
    }
}
```

---

## Key Takeaways

**Encapsulation:**
1. **Make fields private** - Always use `private` for fields (never package-private by accident)
2. **Use final for dependencies** - Immutable after construction
3. **Expose through methods** - Controlled access via getters/setters or behavior methods
4. **Validate in setters** - Maintain object invariants
5. **Defensive copies** - Don't expose internal collections directly
6. **Rich domain models** - Put business logic in entities, not just services

**Abstraction:**
1. **Program to interfaces** - Depend on abstractions, not implementations
2. **Repository pattern** - Abstract data access
3. **Service layer** - Abstract business logic
4. **DTOs for boundaries** - Don't expose entities to web layer
5. **Information hiding** - Hide implementation details
6. **Liskov substitution** - Any implementation should work the same

**The Golden Rules:**
- Default to **private**, open up only when needed
- Make fields **final** unless they must change
- **Never** expose mutable collections directly
- Put **behavior with data** (avoid anemic models)
- **Dependencies should be interfaces**, not classes

---

## Practice Exercise: Refactor Genre Class

**Your Task:**

Refactor the `Genre` class to properly encapsulate its state and add behavior.

📁 File: `src/main/java/com/penrose/bibby/library/genre/Genre.java`

**Current Issues:**
1. Package-private fields (missing `private`)
2. No validation in setters
3. Anemic model (just getters/setters, no behavior)
4. Uses String for description (could be more expressive)

**Part 1: Fix Access Modifiers**
- Make all fields `private`
- Consider making some fields `final` if appropriate

**Part 2: Add Validation**
- Genre name cannot be null or blank
- Description can be null but not blank (empty description vs no description)

**Part 3: Add Behavior Methods**
- `matches(String name)` - case-insensitive genre name matching
- `hasDescription()` - returns true if description is not null/blank
- `getDisplayName()` - returns capitalized genre name

**Part 4: Consider Immutability**
- Should Genre be mutable or immutable?
- If immutable, remove setters and make fields final

**Solution (Immutable Version):**

```java
package com.penrose.bibby.library.genre;

import java.util.Objects;

public class Genre {

    private final Long id;
    private final String genreName;
    private final String genreDescription;

    // Constructor for creating new genre (no ID yet)
    public Genre(String genreName, String genreDescription) {
        if (genreName == null || genreName.isBlank()) {
            throw new IllegalArgumentException("Genre name cannot be null or blank");
        }
        if (genreDescription != null && genreDescription.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank (use null for no description)");
        }

        this.id = null; // Will be assigned by database
        this.genreName = genreName.trim();
        this.genreDescription = genreDescription != null ? genreDescription.trim() : null;
    }

    // Constructor for reconstructing from database (has ID)
    public Genre(Long id, String genreName, String genreDescription) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null for existing genre");
        }
        if (genreName == null || genreName.isBlank()) {
            throw new IllegalArgumentException("Genre name cannot be null or blank");
        }

        this.id = id;
        this.genreName = genreName.trim();
        this.genreDescription = genreDescription != null ? genreDescription.trim() : null;
    }

    // Behavior methods
    public boolean matches(String name) {
        return name != null && this.genreName.equalsIgnoreCase(name.trim());
    }

    public boolean hasDescription() {
        return genreDescription != null && !genreDescription.isBlank();
    }

    public String getDisplayName() {
        // Capitalize first letter of each word
        String[] words = genreName.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    // Read-only getters (no setters - immutable!)
    public Long getId() {
        return id;
    }

    public String getGenreName() {
        return genreName;
    }

    public String getGenreDescription() {
        return genreDescription;
    }

    // equals and hashCode based on name (business key)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Genre)) return false;
        Genre genre = (Genre) o;
        return genreName.equalsIgnoreCase(genre.genreName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genreName.toLowerCase());
    }

    @Override
    public String toString() {
        return "Genre{" +
                "id=" + id +
                ", name='" + genreName + '\'' +
                ", hasDescription=" + hasDescription() +
                '}';
    }
}
```

**What Improved:**
- ✅ All fields private and final (immutability)
- ✅ Validation in constructors
- ✅ Behavior methods (matches, hasDescription, getDisplayName)
- ✅ No setters (can't modify after creation)
- ✅ equals/hashCode based on business key
- ✅ Better encapsulation (internal state protected)

---

## Action Items for This Week

**1. Fix Access Modifiers Across Codebase**
**Priority:** HIGH
**Estimated Time:** 20 minutes

Audit all classes and fix:
- Genre.java: Add `private` to genreName and genreDescription
- ShelfService.java: Add `private` to shelfRepository
- BookService.java: Add `private` to bookRepository (already has private on authorRepository)

Search for package-private fields:
```bash
grep -rn "^\s*[A-Z]" src/main/java --include="*.java" | grep -v "public\|private\|protected"
```

**2. Return Defensive Copies of Collections**
**Priority:** MEDIUM
**Estimated Time:** 30 minutes
**File:** `src/main/java/com/penrose/bibby/library/book/BookEntity.java`

Change:
```java
public Set<AuthorEntity> getAuthors() {
    return authors;
}
```

To:
```java
public Set<AuthorEntity> getAuthors() {
    return Collections.unmodifiableSet(authors);
}
```

Test that callers can't modify the internal collection.

**3. Add Business Logic Methods to BookEntity**
**Priority:** MEDIUM
**Estimated Time:** 45 minutes
**File:** `src/main/java/com/penrose/bibby/library/book/BookEntity.java`

Add these behavior methods:
- `checkOut()` - Sets status to CHECKED_OUT, increments count
- `checkIn()` - Sets status to AVAILABLE
- `isAvailable()` - Returns true if status is AVAILABLE
- `isCheckedOut()` - Returns true if status is CHECKED_OUT

Update BookService to use these methods instead of direct setters.

---

## Further Study

**Books:**
- *Clean Code* by Robert Martin - Chapter 6: "Objects and Data Structures"
- *Effective Java* (3rd Ed) by Joshua Bloch - Item 15: "Minimize the accessibility of classes and members"
- *Effective Java* - Item 16: "In public classes, use accessor methods, not public fields"
- *Effective Java* - Item 50: "Make defensive copies when needed"
- *Domain-Driven Design* by Eric Evans - Chapters on entities and value objects

**Articles:**
- Martin Fowler: "Anemic Domain Model" - https://martinfowler.com/bliki/AnemicDomainModel.html
- Baeldung: "Encapsulation in Java" - https://www.baeldung.com/java-encapsulation

**Principles:**
- SOLID Principles (especially Liskov Substitution)
- Information Hiding (David Parnas)
- Tell, Don't Ask principle

---

## Summary

You've learned:
- ✅ Encapsulation hides internal state, exposes controlled interfaces
- ✅ Access modifiers: private (default), package-private, protected, public
- ✅ Always use `private` for fields (never accidentally package-private)
- ✅ Make fields `final` when possible (immutability)
- ✅ Defensive copies prevent collection modification
- ✅ Rich domain models > anemic models (behavior with data)
- ✅ Abstraction hides implementation, exposes contracts
- ✅ Repository interfaces are excellent abstractions
- ✅ Service layers abstract business logic
- ✅ Program to interfaces, not implementations

**Your code showed:**
- **Strengths:** Repository interfaces, service layer abstraction, constructor injection
- **Opportunities:** Package-private fields (Genre), exposed collections (BookEntity.authors), anemic domain models

**Next Up:** Section 6 - OOP: Inheritance & Polymorphism (examining class hierarchies, method overriding, and polymorphic behavior in your domain model)

---

*Section created: 2025-11-17*
*Files analyzed: Genre, ShelfService, BookEntity, AuthorRepository, BookService*
*Encapsulation issues identified: 5*
*Refactoring patterns provided: 6*
