# Section 7: Constructors & Object Lifecycle

Welcome to Section 7! Constructors are **special methods** that create and initialize objects. Understanding constructor patterns, object lifecycle, and initialization strategies is crucial for writing robust, maintainable code.

Your Bibby application demonstrates several constructor patterns - some excellent, some that could be improved. Let's learn from your actual code!

---

## What Is a Constructor?

A **constructor** is a special method that:
- Has the **same name as the class**
- Has **no return type** (not even void)
- Is called **automatically** when you create an object with `new`
- Initializes the object's state

**Basic Example:**

```java
public class Book {
    private String title;

    // Constructor
    public Book(String title) {
        this.title = title;
    }
}

// Usage:
Book book = new Book("The Hobbit"); // Constructor called
```

---

## Constructor Types

### 1. No-Argument Constructor

```java
public class Book {
    public Book() {
        // Initialize with defaults
    }
}

// Usage:
Book book = new Book(); // No arguments
```

### 2. Parameterized Constructor

```java
public class Book {
    private String title;

    public Book(String title) {
        this.title = title;
    }
}

// Usage:
Book book = new Book("The Hobbit");
```

### 3. Copy Constructor

```java
public class Book {
    private String title;

    public Book(Book other) {
        this.title = other.title;
    }
}

// Usage:
Book original = new Book("The Hobbit");
Book copy = new Book(original); // Copy constructor
```

---

## In Your Code: Constructor Patterns

### ✅ Excellent: Constructor Overloading

📁 File: `src/main/java/com/penrose/bibby/library/bookcase/BookcaseEntity.java`
📍 Lines: 15-27

```java
public BookcaseEntity(String bookcaseLabel, int shelfCapacity) {
    this.bookcaseLabel = bookcaseLabel;
    this.shelfCapacity = shelfCapacity;
}

public BookcaseEntity(Long bookcaseId, String bookcaseLabel, int shelfCapacity) {
    this.bookcaseId = bookcaseId;
    this.bookcaseLabel = bookcaseLabel;
    this.shelfCapacity = shelfCapacity;
}

public BookcaseEntity() {

}
```

**Why This Is Good:**

You have **three constructors** for different use cases:

1. **No-args** - For JPA to instantiate entities from database
2. **Label + Capacity** - For creating new bookcases (ID will be generated)
3. **ID + Label + Capacity** - For reconstructing from database with existing ID

**Usage Scenarios:**

```java
// Creating new bookcase (no ID yet):
BookcaseEntity newBookcase = new BookcaseEntity("Main Bookcase", 5);
repository.save(newBookcase); // Database generates ID

// JPA reconstructing from database:
BookcaseEntity loaded = new BookcaseEntity(); // No-args constructor
// JPA then sets fields via reflection

// Manually creating with known ID (rare):
BookcaseEntity manual = new BookcaseEntity(1L, "Main Bookcase", 5);
```

**Well done!** This is proper constructor overloading.

---

### ⚠️ Constructor with Derived State

📁 File: `src/main/java/com/penrose/bibby/library/author/AuthorEntity.java`
📍 Lines: 24-28

```java
public AuthorEntity(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.fullName = String.format("%s %s", firstName, lastName);
}
```

**The Problem:**

You're **storing computed state** (`fullName`). This creates several issues:

1. **Data Duplication** - `fullName` is redundant (can be computed from firstName + lastName)
2. **Synchronization Risk** - If firstName or lastName change, fullName becomes stale
3. **Database Storage** - Wastes space storing derived data

**Example of the Bug:**

```java
AuthorEntity author = new AuthorEntity("J.R.R.", "Tolkien");
System.out.println(author.getFullName()); // "J.R.R. Tolkien" ✅

// Later, someone updates the name:
author.setFirstName("John");
author.setLastName("Smith");
System.out.println(author.getFullName()); // Still "J.R.R. Tolkien" ❌
// fullName is now WRONG!
```

**Refactored (Computed Property):**

```java
@Entity
@Table(name = "authors")
public class AuthorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long authorId;

    private String firstName;
    private String lastName;

    // No fullName field!

    public AuthorEntity(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    protected AuthorEntity() {
        // For JPA
    }

    // Computed on-the-fly (no storage)
    @Transient // Tell JPA not to persist this
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return "";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return String.format("%s %s", firstName, lastName);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        // fullName automatically updates via getFullName()!
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        // fullName automatically updates via getFullName()!
    }
}
```

**What Changed:**
- ✅ Removed `fullName` field (no duplication)
- ✅ Added `@Transient` to `getFullName()` (computed, not stored)
- ✅ Always consistent (computed from current firstName/lastName)
- ✅ Less database storage
- ✅ No synchronization bugs

**When to Store Derived Data:**
- Performance-critical calculations (complex computations)
- Frequently queried fields (database indexes)
- Aggregations from other tables

For simple string concatenation like this, **always compute it**.

---

### ⚠️ No-Arg Constructor for JPA

📁 File: `src/main/java/com/penrose/bibby/library/book/BookEntity.java`
📍 Lines: 30-31

```java
public BookEntity() {
}
```

**Why This Exists:**

JPA (Hibernate) requires a **no-argument constructor** to instantiate entities when loading from the database.

**The Process:**

```java
// What JPA does internally (simplified):
BookEntity book = new BookEntity(); // Calls no-arg constructor
book.setBookId(1L);                 // Sets fields via reflection
book.setTitle("The Hobbit");
book.setAuthors(authors);
// ... etc
```

**Best Practice: Make It Protected**

```java
// Instead of:
public BookEntity() {
}

// Use:
protected BookEntity() {
    // For JPA use only
}
```

**Why Protected?**

- ✅ JPA can still access it (same package or subclass)
- ✅ Other code can't accidentally create uninitialized entities
- ✅ Forces use of proper constructors

**Complete Pattern:**

```java
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookId;

    private String title;
    private String isbn;

    // Protected no-arg for JPA
    protected BookEntity() {
    }

    // Public constructor with required fields
    public BookEntity(String title, String isbn) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException("ISBN cannot be blank");
        }

        this.title = title;
        this.isbn = isbn;
    }
}

// Usage:
BookEntity book = new BookEntity(); // ❌ Compile error! Constructor is protected
BookEntity book = new BookEntity("The Hobbit", "123"); // ✅ Must provide required fields
```

---

## Constructor Chaining

Constructors can call other constructors using `this()`:

**Example:**

```java
public class BookcaseEntity {
    private Long bookcaseId;
    private String bookcaseLabel;
    private int shelfCapacity;

    // Primary constructor
    public BookcaseEntity(Long bookcaseId, String bookcaseLabel, int shelfCapacity) {
        this.bookcaseId = bookcaseId;
        this.bookcaseLabel = bookcaseLabel;
        this.shelfCapacity = shelfCapacity;
    }

    // Delegates to primary constructor
    public BookcaseEntity(String bookcaseLabel, int shelfCapacity) {
        this(null, bookcaseLabel, shelfCapacity); // Calls 3-arg constructor
    }

    // Delegates with default capacity
    public BookcaseEntity(String bookcaseLabel) {
        this(bookcaseLabel, 5); // Calls 2-arg constructor
    }

    protected BookcaseEntity() {
        // For JPA
    }
}
```

**Benefits:**
- ✅ DRY (Don't Repeat Yourself) - validation/initialization in one place
- ✅ Easier to maintain (change logic once, affects all constructors)
- ✅ Clear delegation chain

**Your Current Code Could Use This:**

📁 File: `src/main/java/com/penrose/bibby/library/bookcase/BookcaseEntity.java`

```java
// CURRENT (repetitive):
public BookcaseEntity(String bookcaseLabel, int shelfCapacity) {
    this.bookcaseLabel = bookcaseLabel;
    this.shelfCapacity = shelfCapacity;
}

public BookcaseEntity(Long bookcaseId, String bookcaseLabel, int shelfCapacity) {
    this.bookcaseId = bookcaseId;
    this.bookcaseLabel = bookcaseLabel;
    this.shelfCapacity = shelfCapacity;
}

// IMPROVED (chained):
public BookcaseEntity(Long bookcaseId, String bookcaseLabel, int shelfCapacity) {
    // Validation in one place
    if (bookcaseLabel == null || bookcaseLabel.isBlank()) {
        throw new IllegalArgumentException("Label cannot be blank");
    }
    if (shelfCapacity < 1) {
        throw new IllegalArgumentException("Capacity must be at least 1");
    }

    this.bookcaseId = bookcaseId;
    this.bookcaseLabel = bookcaseLabel;
    this.shelfCapacity = shelfCapacity;
}

public BookcaseEntity(String bookcaseLabel, int shelfCapacity) {
    this(null, bookcaseLabel, shelfCapacity); // Delegates to 3-arg
}

protected BookcaseEntity() {
    // For JPA
}
```

---

## The Builder Pattern

When a class has many optional parameters, constructors become unwieldy.

**The Problem:**

```java
public class Book {
    private String title;
    private String isbn;
    private String publisher;
    private Integer publicationYear;
    private String genre;
    private Integer edition;
    private String description;

    // Constructor with all parameters (yuck!)
    public Book(String title, String isbn, String publisher,
                Integer publicationYear, String genre, Integer edition,
                String description) {
        this.title = title;
        this.isbn = isbn;
        this.publisher = publisher;
        this.publicationYear = publicationYear;
        this.genre = genre;
        this.edition = edition;
        this.description = description;
    }

    // Usage (hard to read):
    Book book = new Book("The Hobbit", "123", "Publisher", 1937, "Fiction", 1, "A classic");
    // Which parameter is which? Hard to tell!
}
```

**The Solution: Builder Pattern**

```java
public class BookEntity {
    private String title;
    private String isbn;
    private String publisher;
    private Integer publicationYear;
    private String genre;
    private Integer edition;
    private String description;

    // Private constructor - force use of builder
    private BookEntity(Builder builder) {
        this.title = builder.title;
        this.isbn = builder.isbn;
        this.publisher = builder.publisher;
        this.publicationYear = builder.publicationYear;
        this.genre = builder.genre;
        this.edition = builder.edition;
        this.description = builder.description;
    }

    // Protected for JPA
    protected BookEntity() {
    }

    // Builder class (nested)
    public static class Builder {
        // Required parameters
        private final String title;
        private final String isbn;

        // Optional parameters with defaults
        private String publisher;
        private Integer publicationYear;
        private String genre;
        private Integer edition = 1;
        private String description;

        // Constructor with required fields
        public Builder(String title, String isbn) {
            this.title = title;
            this.isbn = isbn;
        }

        // Fluent setters for optional fields
        public Builder publisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder publicationYear(Integer year) {
            this.publicationYear = year;
            return this;
        }

        public Builder genre(String genre) {
            this.genre = genre;
            return this;
        }

        public Builder edition(Integer edition) {
            this.edition = edition;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        // Build method with validation
        public BookEntity build() {
            // Validate required fields
            if (title == null || title.isBlank()) {
                throw new IllegalStateException("Title is required");
            }
            if (isbn == null || isbn.isBlank()) {
                throw new IllegalStateException("ISBN is required");
            }

            return new BookEntity(this);
        }
    }

    // Static factory method (optional)
    public static Builder builder(String title, String isbn) {
        return new Builder(title, isbn);
    }
}
```

**Usage (Beautiful!):**

```java
BookEntity book = new BookEntity.Builder("The Hobbit", "978-0547928227")
    .publisher("Houghton Mifflin")
    .publicationYear(1937)
    .genre("Fantasy")
    .edition(1)
    .description("A classic tale of adventure")
    .build();

// Or with static factory:
BookEntity book = BookEntity.builder("The Hobbit", "978-0547928227")
    .publisher("Houghton Mifflin")
    .publicationYear(1937)
    .build(); // Other fields use defaults or remain null

// Only required fields:
BookEntity minimal = BookEntity.builder("The Hobbit", "978-0547928227")
    .build();
```

**Benefits:**
- ✅ Named parameters (self-documenting)
- ✅ Immutable objects (fields can be final)
- ✅ Validation in one place (build() method)
- ✅ Optional parameters with defaults
- ✅ Fluent, readable API

**When to Use Builder:**
- 4+ constructor parameters
- Many optional parameters
- Need for immutability
- Complex validation logic

---

## Records: Immutable by Default

Java 14+ introduced **records** - compact syntax for immutable data carriers.

### ✅ Your Excellent Use of Records

📁 File: `src/main/java/com/penrose/bibby/library/book/BookRequestDTO.java`
📍 Lines: 1-3

```java
public record BookRequestDTO(String title, String firstName, String lastName) {}
```

**What You Get For Free:**

```java
// Equivalent to this verbose class:
public final class BookRequestDTO {
    private final String title;
    private final String firstName;
    private final String lastName;

    public BookRequestDTO(String title, String firstName, String lastName) {
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String title() { return title; }
    public String firstName() { return firstName; }
    public String lastName() { return lastName; }

    @Override
    public boolean equals(Object o) { /* generated */ }

    @Override
    public int hashCode() { /* generated */ }

    @Override
    public String toString() { /* generated */ }
}
```

**All in 1 line!** This is why records are perfect for DTOs.

---

### 🚀 Records with Validation

Records can have **compact constructors** for validation:

```java
public record BookRequestDTO(String title, String firstName, String lastName) {
    // Compact constructor - no parameter list
    public BookRequestDTO {
        // Validate before assignment
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name cannot be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name cannot be blank");
        }

        // Can normalize data
        title = title.trim();
        firstName = firstName.trim();
        lastName = lastName.trim();
    }
}

// Usage:
BookRequestDTO dto = new BookRequestDTO("The Hobbit", "J.R.R.", "Tolkien");
// ✅ Validated and normalized

BookRequestDTO invalid = new BookRequestDTO("", "J.R.R.", "Tolkien");
// ❌ Throws IllegalArgumentException
```

**Your Records Could Benefit From Validation:**

```java
// CURRENT:
public record BookRequestDTO(String title, String firstName, String lastName) {}

// IMPROVED:
public record BookRequestDTO(String title, String firstName, String lastName) {
    public BookRequestDTO {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name cannot be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name cannot be blank");
        }

        // Normalize
        title = title.trim();
        firstName = firstName.trim();
        lastName = lastName.trim();
    }
}
```

---

## Object Initialization Strategies

### 1. Direct Field Initialization

```java
public class BookEntity {
    private BookStatus status = BookStatus.AVAILABLE; // Default value
    private Integer checkoutCount = 0;
    private LocalDate createdAt = LocalDate.now();

    public BookEntity() {
        // Fields already initialized!
    }
}
```

**Benefits:** Clear defaults, always initialized

**Drawbacks:** Executes for every constructor, even if overridden

---

### 2. Constructor Initialization

```java
public class BookEntity {
    private BookStatus status;
    private Integer checkoutCount;
    private LocalDate createdAt;

    public BookEntity() {
        this.status = BookStatus.AVAILABLE;
        this.checkoutCount = 0;
        this.createdAt = LocalDate.now();
    }
}
```

**Benefits:** Control over initialization, can vary by constructor

---

### 3. Initialization Blocks

```java
public class BookEntity {
    private BookStatus status;
    private Integer checkoutCount;

    // Instance initialization block
    {
        status = BookStatus.AVAILABLE;
        checkoutCount = 0;
    }

    public BookEntity() {
        // Block runs before constructor body
    }
}
```

**Rarely Used** - constructors are clearer

---

### 4. Static Initialization Blocks

```java
public class Constants {
    private static final Map<String, Genre> GENRES;

    // Static initialization block
    static {
        Map<String, Genre> map = new HashMap<>();
        map.put("FICTION", new Genre("Fiction", "Fictional works"));
        map.put("NON_FICTION", new Genre("Non-Fiction", "Factual works"));
        GENRES = Collections.unmodifiableMap(map);
    }
}
```

**Use Case:** Complex static field initialization

---

## Object Lifecycle in JPA Context

JPA entities have a special lifecycle:

**States:**
1. **Transient** - New object, not associated with database
2. **Persistent** - Managed by EntityManager, tracked for changes
3. **Detached** - Was persistent, no longer managed
4. **Removed** - Marked for deletion

**Lifecycle:**

```java
// 1. TRANSIENT - just created
BookEntity book = new BookEntity("The Hobbit", "123");

// 2. PERSISTENT - saved to database
entityManager.persist(book);
// OR
book = bookRepository.save(book);

// Changes are automatically synchronized
book.setTitle("Updated Title"); // Automatically saved on transaction commit!

// 3. DETACHED - transaction ends, entity no longer managed
// (Outside transaction or after serialization)

// 4. REMOVED - marked for deletion
entityManager.remove(book);
// OR
bookRepository.delete(book);
```

**Key Point:** In JPA, you don't always need to call `save()` - changes to persistent entities are automatically saved!

**Example in Your Code:**

```java
@Transactional
public void updateBookTitle(Long bookId, String newTitle) {
    BookEntity book = bookRepository.findById(bookId).orElseThrow();
    // book is now PERSISTENT (managed by EntityManager)

    book.setTitle(newTitle);
    // No need to call save()! Change is tracked automatically

    // When method ends, transaction commits, changes are saved
}
```

---

## Immutability

**Immutable objects** cannot be changed after creation.

### Benefits of Immutability:

1. **Thread-safe** - No synchronization needed
2. **Cacheable** - Can be safely shared
3. **Simple** - No invalid states
4. **Predictable** - No side effects

### Your Records Are Immutable:

```java
public record BookRequestDTO(String title, String firstName, String lastName) {}

// Usage:
BookRequestDTO dto = new BookRequestDTO("The Hobbit", "J.R.R.", "Tolkien");

// Can't modify:
dto.title = "New Title"; // ❌ Compile error! No setter!

// To "change", create new object:
BookRequestDTO updated = new BookRequestDTO("New Title", dto.firstName(), dto.lastName());
```

### Making Entities Immutable (Advanced):

For value objects (not entities), consider immutability:

```java
public final class ISBN {
    private final String value;

    public ISBN(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid ISBN: " + value);
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    // No setters - immutable!

    private boolean isValid(String isbn) {
        // Validation logic
        return isbn != null && isbn.matches("\\d{3}-\\d{10}");
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ISBN)) return false;
        return value.equals(((ISBN) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
```

**Usage:**

```java
ISBN isbn = new ISBN("978-0547928227");
// isbn.value = "different"; // ❌ Can't change - final!

// To "change", create new:
ISBN newIsbn = new ISBN("978-0000000000");
```

---

## Key Takeaways

**Constructors:**
1. **No-arg for JPA** - Make it `protected`, not `public`
2. **Overloading** - Multiple constructors for different use cases
3. **Chaining** - Use `this()` to delegate to primary constructor
4. **Validation** - Check parameters, fail fast
5. **@Override for records** - Use compact constructor for validation

**Patterns:**
1. **Builder Pattern** - For classes with many optional parameters
2. **Records** - For immutable DTOs (perfect for your RequestDTO, Summary, DetailView)
3. **Factory Methods** - Static methods that return instances
4. **Defensive Copies** - Clone mutable parameters

**Initialization:**
1. **Field initialization** - For simple defaults
2. **Constructor initialization** - For complex logic
3. **Static blocks** - For static field setup
4. **JPA requires no-arg** - Protected constructor + public parameterized

**Immutability:**
1. **Final fields** - Can't be reassigned
2. **No setters** - No mutation
3. **Records** - Immutable by design
4. **Defensive copies** - Return copies of mutable fields

---

## Practice Exercise: Refactor AuthorEntity

**Your Task:**

Refactor `AuthorEntity` to eliminate the stored `fullName` field and compute it dynamically.

**Part 1: Remove Stored fullName**

📁 File: `src/main/java/com/penrose/bibby/library/author/AuthorEntity.java`

Current:
```java
private String fullName;

public AuthorEntity(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.fullName = String.format("%s %s", firstName, lastName);
}
```

**Part 2: Add Computed fullName**

```java
// Remove fullName field

public AuthorEntity(String firstName, String lastName) {
    if (firstName == null || firstName.isBlank()) {
        throw new IllegalArgumentException("First name cannot be blank");
    }
    if (lastName == null || lastName.isBlank()) {
        throw new IllegalArgumentException("Last name cannot be blank");
    }

    this.firstName = firstName.trim();
    this.lastName = lastName.trim();
}

protected AuthorEntity() {
    // For JPA
}

@Transient // Don't persist this
public String getFullName() {
    if (firstName == null && lastName == null) {
        return "";
    }
    if (firstName == null) {
        return lastName;
    }
    if (lastName == null) {
        return firstName;
    }
    return String.format("%s %s", firstName, lastName);
}
```

**Part 3: Update Database**

You'll need to:
1. Remove `full_name` column from `authors` table
2. Update any queries that reference `fullName`

**Benefits:**
- ✅ No data duplication
- ✅ Always consistent
- ✅ Less database storage
- ✅ Can't get out of sync

---

## Action Items for This Week

**1. Make JPA No-Arg Constructors Protected**
**Priority:** MEDIUM
**Estimated Time:** 15 minutes

Change all entity no-arg constructors from `public` to `protected`:

```java
// FROM:
public BookEntity() {
}

// TO:
protected BookEntity() {
}
```

**Files:**
- BookEntity.java
- AuthorEntity.java
- ShelfEntity.java
- BookcaseEntity.java

**2. Add Validation to BookRequestDTO**
**Priority:** HIGH
**Estimated Time:** 10 minutes
**File:** `src/main/java/com/penrose/bibby/library/book/BookRequestDTO.java`

Add compact constructor with validation:
```java
public record BookRequestDTO(String title, String firstName, String lastName) {
    public BookRequestDTO {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name cannot be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name cannot be blank");
        }

        title = title.trim();
        firstName = firstName.trim();
        lastName = lastName.trim();
    }
}
```

**3. Refactor AuthorEntity fullName**
**Priority:** MEDIUM
**Estimated Time:** 30 minutes
**File:** `src/main/java/com/penrose/bibby/library/author/AuthorEntity.java`

- Remove `fullName` field
- Add `@Transient getFullName()` method
- Update constructor to not set fullName
- Test that all usages still work

---

## Further Study

**Books:**
- *Effective Java* (3rd Ed) by Joshua Bloch - Item 1: "Consider static factory methods instead of constructors"
- *Effective Java* - Item 2: "Consider a builder when faced with many constructor parameters"
- *Effective Java* - Item 17: "Minimize mutability"
- *Design Patterns* by Gang of Four - Builder Pattern, Factory Pattern

**Articles:**
- Baeldung: "The Builder Pattern in Java" - https://www.baeldung.com/java-builder-pattern
- Oracle: "Java Records" - https://docs.oracle.com/en/java/javase/17/language/records.html
- Baeldung: "Immutability in Java" - https://www.baeldung.com/java-immutable-object

---

## Summary

You've learned:
- ✅ Constructors initialize object state
- ✅ Constructor overloading provides multiple creation paths
- ✅ JPA requires no-arg constructor (make it protected)
- ✅ Constructor chaining with `this()` reduces duplication
- ✅ Builder pattern for classes with many parameters
- ✅ Records are perfect for immutable DTOs
- ✅ Compact constructors validate record parameters
- ✅ Avoid storing derived/computed state
- ✅ Immutability provides thread-safety and simplicity
- ✅ JPA entity lifecycle: transient → persistent → detached → removed

**Your code showed:**
- **Strengths:** Constructor overloading in BookcaseEntity, excellent use of records (BookRequestDTO, BookSummary)
- **Opportunities:** Make no-arg constructors protected, remove derived fullName field, add validation to record constructors

**Next Up:** Section 8 - Packages & Visibility (examining package organization, access control, and module design in your codebase)

---

*Section created: 2025-11-17*
*Files analyzed: BookcaseEntity, AuthorEntity, BookEntity, BookRequestDTO, BookSummary*
*Constructor patterns identified: 7*
*Refactoring opportunities: 4*
