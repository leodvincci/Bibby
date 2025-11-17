# SECTION 9: CREATIONAL PATTERNS - FACTORY & BUILDER

## Creating Objects the Right Way

Welcome to Part 2! In Part 1, we learned Domain Driven Design - how to model your domain. Now we're learning the Gang of Four Design Patterns - proven solutions to common software design problems.

We're starting with **Creational Patterns** - patterns for creating objects. Specifically: **Factory Method**, **Abstract Factory**, and **Builder**.

You might think "Creating objects is easy - just use `new`!" But as your domain grows complex, object creation becomes a significant design challenge. These patterns solve that.

**In this section**, we'll analyze **your actual Bibby code** (`Book.java`, `BookService.java`) and transform it using Factory and Builder patterns.

---

## Part 1: Analyzing Your Current Bibby Code

### Your Current Book Class (Book.java:1-179)

Let's look at what you have now:

```java
package com.penrose.bibby.library.book;

public class Book {
    private Long id;
    private int edition;
    private String title;              // ❌ Primitive! Should be Title value object
    private AuthorEntity authorEntity;
    private String isbn;                // ❌ Primitive! Should be ISBN value object
    private String publisher;           // ❌ Primitive! Should be Publisher value object
    private int publicationYear;
    private Genre genre;
    private Shelf shelf;
    private String description;
    private BookStatus status;
    private Integer checkoutCount;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    // Empty constructor
    public Book() { }

    // Partial constructor
    public Book(Long id, String title, AuthorEntity authorEntity) {
        this.id = id;
        this.title = title;
        this.authorEntity = authorEntity;
    }

    // 50+ lines of getters and setters...
    public void setTitle(String title) { this.title = title; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    // etc...
}
```

### Your Current Book Creation (BookService.java:23-41)

```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    String firstName = bookRequestDTO.firstName();
    String lastName = bookRequestDTO.lastName();
    String title = bookRequestDTO.title();

    BookEntity bookEntity = bookRepository.findByTitle(title);
    AuthorEntity authorEntity = authorRepository.findByFirstNameAndLastName(firstName, lastName);

    if (authorEntity == null) {
        authorEntity = new AuthorEntity(firstName, lastName);
        authorRepository.save(authorEntity);
    }

    if (bookEntity == null) {
        bookEntity = new BookEntity();       // ❌ Empty constructor
        bookEntity.setTitle(title);           // ❌ Setting field by field
    }
    bookEntity.setAuthors(authorEntity);      // ❌ More setters
    bookRepository.save(bookEntity);
}
```

### Problems with Your Current Code

Let's identify the issues (this is a learning exercise - don't feel bad, every junior developer writes code like this!):

#### Problem 1: Anemic Domain Model ❌

**Lines Book.java:35-48** - Your Book class is just a data holder:
```java
public Book() { }  // Anyone can create invalid books!

public void setTitle(String title) { this.title = title; }
public void setIsbn(String isbn) { this.isbn = isbn; }
```

**What's wrong**:
- No behavior, just getters/setters
- No validation
- Can create invalid books
- Violates Tell-Don't-Ask principle

#### Problem 2: Temporal Coupling ❌

**Lines BookService.java:36-40** - Order matters:
```java
bookEntity = new BookEntity();
bookEntity.setTitle(title);          // Must set title first?
bookEntity.setAuthors(authorEntity); // Then authors?
```

**What's wrong**:
- Easy to forget a required field
- No compile-time safety
- Can save incomplete books

#### Problem 3: No Validation ❌

**Lines Book.java:73-75**:
```java
public void setIsbn(String isbn) {
    this.isbn = isbn;  // ❌ No validation! Can set invalid ISBN
}
```

**What's wrong**:
- Can set ISBN to `null`, `""`, `"invalid"`
- No format validation
- Invalid state allowed

#### Problem 4: Primitive Obsession ❌

**Lines Book.java:13-15**:
```java
private String title;     // Should be Title value object
private String isbn;      // Should be ISBN value object
private String publisher; // Should be Publisher value object
```

**What's wrong**:
- Strings can be anything
- No type safety
- Validation scattered

#### Problem 5: No Immutability ❌

**Entire Book class** - Everything is mutable:
```java
Book book = new Book();
book.setTitle("Original");
// Later in code...
book.setTitle("Changed!");  // Unexpected mutation!
```

**What's wrong**:
- Hard to reason about state
- Concurrency issues
- No predictability

---

## Part 2: The Solution - Factory & Builder Patterns

Let's transform your code step by step.

---

## Factory Method Pattern

### What is Factory Method?

**Definition**: Define an interface for creating objects, but let subclasses decide which class to instantiate.

**In plain English**: Instead of `new Book()`, use `BookFactory.createBook()`.

### Why Your Code Needs Factories

Look at **BookService.java:36-40** again:
```java
if (bookEntity == null) {
    bookEntity = new BookEntity();
    bookEntity.setTitle(title);
}
bookEntity.setAuthors(authorEntity);
bookRepository.save(bookEntity);
```

**This creates books that**:
- Might be invalid (missing required fields)
- Have no validation
- Are inconsistently created across your codebase
- Mix creation logic with business logic

### Factory Method Solution for Bibby

#### Step 1: Define Factory Interface

Create `src/main/java/com/penrose/bibby/library/book/factory/BookFactory.java`:

```java
package com.penrose.bibby.library.book.factory;

import com.penrose.bibby.library.book.Book;
import com.penrose.bibby.library.author.Author;

/**
 * Factory for creating Book domain objects.
 * Ensures all books are created in a valid state.
 */
public interface BookFactory {

    /**
     * Create a book from manual entry
     * @param data The book creation data
     * @return A valid Book instance
     * @throws ValidationException if data is invalid
     */
    Book createBook(BookCreationData data);

    /**
     * Create a book with ISBN lookup from external API
     * @param isbn The ISBN to look up
     * @return A valid Book instance with enriched metadata
     * @throws IsbnNotFoundException if ISBN not found
     */
    Book createBookFromIsbn(String isbn);
}
```

#### Step 2: Create Value Objects

Before we can implement the factory, let's create value objects (from DDD Section 2):

**ISBN Value Object** (`src/main/java/com/penrose/bibby/library/book/ISBN.java`):
```java
package com.penrose.bibby.library.book;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object for ISBN (International Standard Book Number).
 * Enforces ISBN-10 or ISBN-13 format.
 */
public record ISBN(String value) {

    private static final Pattern ISBN_10_PATTERN =
        Pattern.compile("^\\d{9}[\\dX]$");
    private static final Pattern ISBN_13_PATTERN =
        Pattern.compile("^\\d{13}$");

    public ISBN {
        Objects.requireNonNull(value, "ISBN cannot be null");

        String cleaned = value.replaceAll("[\\s-]", "");

        if (!ISBN_10_PATTERN.matcher(cleaned).matches() &&
            !ISBN_13_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException(
                "Invalid ISBN format: " + value +
                ". Must be ISBN-10 (10 digits) or ISBN-13 (13 digits)"
            );
        }

        value = cleaned;  // Store cleaned version
    }

    public static ISBN fromString(String isbn) {
        return new ISBN(isbn);
    }

    public boolean isIsbn10() {
        return value.length() == 10;
    }

    public boolean isIsbn13() {
        return value.length() == 13;
    }
}
```

**Title Value Object** (`src/main/java/com/penrose/bibby/library/book/Title.java`):
```java
package com.penrose.bibby.library.book;

import java.util.Objects;

/**
 * Value object for book title.
 * Enforces non-blank titles with max length.
 */
public record Title(String value) {

    private static final int MAX_LENGTH = 500;

    public Title {
        Objects.requireNonNull(value, "Title cannot be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Title too long: " + value.length() +
                " characters (max " + MAX_LENGTH + ")"
            );
        }

        value = value.trim();
    }

    public static Title of(String title) {
        return new Title(title);
    }
}
```

(Continue in next message due to length...)
