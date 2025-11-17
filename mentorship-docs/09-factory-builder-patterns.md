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

**Publisher Value Object** (`src/main/java/com/penrose/bibby/library/book/Publisher.java`):
```java
package com.penrose.bibby.library.book;

import java.util.Objects;

public record Publisher(String value) {
    public Publisher {
        Objects.requireNonNull(value, "Publisher cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Publisher cannot be blank");
        }
        value = value.trim();
    }

    public static Publisher of(String publisher) {
        return new Publisher(publisher);
    }
}
```

**Additional Value Objects** (create these similarly):
- `PublicationYear` - validates year between 1000 and current year
- `Edition` - validates positive integers
- `Description` - optional, max 2000 characters
- `BookId`, `AuthorId`, `ShelfId` - type-safe IDs

---

## Builder Pattern - The Solution for Complex Object Creation

### The Problem with Your Current Approach

Even with value objects, creating books is still cumbersome:

```java
// Hypothetical constructor with value objects
Book book = new Book(
    bookId,
    title,
    isbn,
    authorIds,
    publisher,
    publicationYear,
    edition,
    description,
    genre,
    shelf
);
// Which parameter is which? Easy to mix up!
// What if some are optional?
```

**Problems**:
1. **Telescoping constructors** - need different constructors for different combinations
2. **Hard to read** - can't tell which value is which
3. **No optional parameters** - Java doesn't support them natively
4. **Error-prone** - easy to pass arguments in wrong order

### Builder Pattern Solution

The Builder pattern provides a fluent API for object construction:

```java
Book book = Book.builder()
    .id(BookId.generate())
    .title("Domain-Driven Design")
    .isbn("978-0321125215")
    .addAuthor(ericEvansId)
    .publisher("Addison-Wesley")
    .publicationYear(2003)
    .edition(1)
    .description("The definitive guide to DDD")
    .genre(Genre.TECHNICAL)
    .build();
```

**Benefits**:
- ✅ **Readable** - clear what each parameter is
- ✅ **Optional parameters** - only set what you need
- ✅ **Fluent** - chain method calls
- ✅ **Validation** - happens in `build()` method
- ✅ **Immutable** - object created once, correctly

---

## Step 3: Refactor Book with Builder

Now let's refactor your `Book.java` completely:

**Complete Refactored Book.java**:
```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorId;
import com.penrose.bibby.library.shelf.ShelfId;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

/**
 * Book aggregate root.
 * Represents a physical book in your personal library.
 *
 * Created using Builder pattern for readable, validated construction.
 */
public class Book {

    // Immutable fields (final)
    private final BookId id;
    private final Title title;
    private final ISBN isbn;
    private final Set<AuthorId> authorIds;
    private final Publisher publisher;
    private final PublicationYear publicationYear;
    private final Edition edition;
    private final Description description;
    private final Genre genre;

    // Mutable state (business logic changes these)
    private AvailabilityStatus status;
    private ShelfId currentShelfId;
    private int checkoutCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Private constructor - only Builder can create Book instances.
     * Ensures all books are created in a valid state.
     */
    private Book(Builder builder) {
        // Required fields
        this.id = Objects.requireNonNull(builder.id, "Book ID is required");
        this.title = Objects.requireNonNull(builder.title, "Title is required");
        this.isbn = Objects.requireNonNull(builder.isbn, "ISBN is required");
        this.authorIds = new HashSet<>(builder.authorIds);

        // Optional fields
        this.publisher = builder.publisher;
        this.publicationYear = builder.publicationYear;
        this.edition = builder.edition != null ? builder.edition : Edition.of(1);
        this.description = builder.description;
        this.genre = builder.genre;

        // Initialize mutable state
        this.status = AvailabilityStatus.AVAILABLE;
        this.currentShelfId = ShelfId.unassigned();
        this.checkoutCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // Validate
        validate();
    }

    private void validate() {
        if (authorIds.isEmpty()) {
            throw new IllegalArgumentException("Book must have at least one author");
        }
        if (publicationYear != null &&
            publicationYear.value() > LocalDateTime.now().getYear()) {
            throw new IllegalArgumentException(
                "Publication year cannot be in the future"
            );
        }
    }

    /**
     * Create a new builder for Book
     */
    public static Builder builder() {
        return new Builder();
    }

    // ==================== Business Methods ====================

    /**
     * Check out this book from the library.
     * Changes status to CHECKED_OUT and increments checkout count.
     *
     * @throws BookNotAvailableException if book is not available
     */
    public void checkOut() {
        if (this.status != AvailabilityStatus.AVAILABLE) {
            throw new BookNotAvailableException(
                "Cannot check out book " + id + " - status is " + status
            );
        }
        this.status = AvailabilityStatus.CHECKED_OUT;
        this.checkoutCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Return this book to the library.
     * Changes status back to AVAILABLE.
     *
     * @throws IllegalStateException if book is not checked out
     */
    public void returnToLibrary() {
        if (this.status != AvailabilityStatus.CHECKED_OUT) {
            throw new IllegalStateException(
                "Cannot return book " + id + " - it is not checked out"
            );
        }
        this.status = AvailabilityStatus.AVAILABLE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Place this book on a specific shelf.
     *
     * @param shelfId The shelf to place the book on
     */
    public void placeOnShelf(ShelfId shelfId) {
        this.currentShelfId = Objects.requireNonNull(shelfId, "Shelf ID required");
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Report this book as lost.
     */
    public void reportLost() {
        this.status = AvailabilityStatus.LOST;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this book is currently available for checkout.
     */
    public boolean isAvailable() {
        return this.status == AvailabilityStatus.AVAILABLE;
    }

    // ==================== Getters (No Setters!) ====================

    public BookId getId() { return id; }
    public Title getTitle() { return title; }
    public ISBN getIsbn() { return isbn; }
    public Set<AuthorId> getAuthorIds() {
        return Set.copyOf(authorIds); // Defensive copy
    }
    public Publisher getPublisher() { return publisher; }
    public PublicationYear getPublicationYear() { return publicationYear; }
    public Edition getEdition() { return edition; }
    public Description getDescription() { return description; }
    public Genre getGenre() { return genre; }
    public AvailabilityStatus getStatus() { return status; }
    public ShelfId getCurrentShelfId() { return currentShelfId; }
    public int getCheckoutCount() { return checkoutCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book book)) return false;
        return Objects.equals(id, book.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ==================== Builder ====================

    /**
     * Builder for Book aggregate.
     * Provides fluent API for creating books with validation.
     */
    public static class Builder {
        private BookId id;
        private Title title;
        private ISBN isbn;
        private Set<AuthorId> authorIds = new HashSet<>();
        private Publisher publisher;
        private PublicationYear publicationYear;
        private Edition edition;
        private Description description;
        private Genre genre;

        /**
         * Set the book ID (required)
         */
        public Builder id(BookId id) {
            this.id = id;
            return this;
        }

        /**
         * Set the title from string (convenience method)
         */
        public Builder title(String title) {
            this.title = Title.of(title);
            return this;
        }

        /**
         * Set the title from value object
         */
        public Builder title(Title title) {
            this.title = title;
            return this;
        }

        /**
         * Set ISBN from string (convenience method)
         */
        public Builder isbn(String isbn) {
            this.isbn = ISBN.fromString(isbn);
            return this;
        }

        /**
         * Set ISBN from value object
         */
        public Builder isbn(ISBN isbn) {
            this.isbn = isbn;
            return this;
        }

        /**
         * Add a single author
         */
        public Builder addAuthor(AuthorId authorId) {
            this.authorIds.add(authorId);
            return this;
        }

        /**
         * Set all authors at once
         */
        public Builder authors(Set<AuthorId> authorIds) {
            this.authorIds = new HashSet<>(authorIds);
            return this;
        }

        /**
         * Set publisher from string (convenience method)
         */
        public Builder publisher(String publisher) {
            this.publisher = publisher != null ? Publisher.of(publisher) : null;
            return this;
        }

        /**
         * Set publisher from value object
         */
        public Builder publisher(Publisher publisher) {
            this.publisher = publisher;
            return this;
        }

        /**
         * Set publication year
         */
        public Builder publicationYear(int year) {
            this.publicationYear = PublicationYear.of(year);
            return this;
        }

        /**
         * Set edition
         */
        public Builder edition(int edition) {
            this.edition = Edition.of(edition);
            return this;
        }

        /**
         * Set description (optional)
         */
        public Builder description(String description) {
            this.description = description != null ? Description.of(description) : null;
            return this;
        }

        /**
         * Set genre (optional)
         */
        public Builder genre(Genre genre) {
            this.genre = genre;
            return this;
        }

        /**
         * Build the Book.
         * Validates all required fields and creates the Book instance.
         *
         * @return A validated Book instance
         * @throws IllegalArgumentException if required fields missing or validation fails
         */
        public Book build() {
            return new Book(this);
        }
    }
}
```

**Key improvements over your original Book.java**:
1. ✅ **Value objects** instead of primitives (Title, ISBN, Publisher)
2. ✅ **Private constructor** - controlled creation only through Builder
3. ✅ **Validation** in constructor - impossible to create invalid books
4. ✅ **Business methods** - `checkOut()`, `returnToLibrary()`, `placeOnShelf()`
5. ✅ **No setters** for core fields - immutability where it matters
6. ✅ **Fluent Builder API** - readable object construction
7. ✅ **Defensive copying** - `getAuthorIds()` returns copy, not internal set

---

## Step 4: Implement Factory Using Builder

Now create the factory that uses the Builder:

**ManualEntryBookFactory.java**:
```java
package com.penrose.bibby.library.book.factory;

import com.penrose.bibby.library.book.*;
import com.penrose.bibby.library.author.AuthorId;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory for creating books from manual entry.
 * Validates all input and creates books in valid state using Builder.
 */
@Component("manualBookFactory")
public class ManualEntryBookFactory implements BookFactory {

    @Override
    public Book createBook(BookCreationData data) {
        // Parse and validate author IDs
        Set<AuthorId> authorIds = data.getAuthorIds().stream()
            .map(AuthorId::new)
            .collect(Collectors.toSet());

        if (authorIds.isEmpty()) {
            throw new ValidationException("At least one author required");
        }

        // Use Builder to create book - fluent and readable!
        return Book.builder()
            .id(BookId.generate())
            .title(data.getTitle())              // Convenience method
            .isbn(data.getIsbn())                 // Convenience method
            .authors(authorIds)
            .publisher(data.getPublisher())       // Convenience method
            .publicationYear(data.getYear())
            .edition(data.getEdition())
            .description(data.getDescription())   // Optional
            .genre(data.getGenre())               // Optional
            .build();  // Validation happens here!
    }

    @Override
    public Book createBookFromIsbn(String isbn) {
        throw new UnsupportedOperationException(
            "Manual factory does not support ISBN lookup. " +
            "Use IsbnLookupBookFactory instead."
        );
    }
}
```

---

## Step 5: Test Data Builders - Essential for Testing!

One of the **BEST** uses of Builder is creating test data. Create **BookTestBuilder.java**:

```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorId;
import java.util.Set;

/**
 * Test data builder for Book.
 * Provides sensible defaults for testing, making tests readable and maintainable.
 *
 * Usage:
 *   Book book = aBook().build();
 *   Book fiction = aFictionBook().withTitle("1984").build();
 */
public class BookTestBuilder {

    // Sensible defaults
    private BookId id = BookId.generate();
    private Title title = Title.of("Test Book");
    private ISBN isbn = ISBN.fromString("978-0000000000");
    private Set<AuthorId> authorIds = Set.of(AuthorId.generate());
    private Publisher publisher = Publisher.of("Test Publisher");
    private PublicationYear year = PublicationYear.of(2024);
    private Edition edition = Edition.of(1);
    private Genre genre = Genre.FICTION;
    private Description description = null;

    /**
     * Start building a book with defaults
     */
    public static BookTestBuilder aBook() {
        return new BookTestBuilder();
    }

    /**
     * Create a fiction book with preset values
     */
    public static BookTestBuilder aFictionBook() {
        return new BookTestBuilder()
            .withTitle("The Great Gatsby")
            .withPublisher("Scribner")
            .withGenre(Genre.FICTION)
            .withYear(1925);
    }

    /**
     * Create a technical book with preset values
     */
    public static BookTestBuilder aTechnicalBook() {
        return new BookTestBuilder()
            .withTitle("Clean Code")
            .withPublisher("Prentice Hall")
            .withGenre(Genre.TECHNICAL)
            .withYear(2008);
    }

    /**
     * Create a DDD book (Domain-Driven Design)
     */
    public static BookTestBuilder aDDDBook() {
        return new BookTestBuilder()
            .withTitle("Domain-Driven Design")
            .withIsbn("978-0321125215")
            .withPublisher("Addison-Wesley")
            .withGenre(Genre.TECHNICAL)
            .withYear(2003);
    }

    // Fluent setters

    public BookTestBuilder withId(BookId id) {
        this.id = id;
        return this;
    }

    public BookTestBuilder withTitle(String title) {
        this.title = Title.of(title);
        return this;
    }

    public BookTestBuilder withIsbn(String isbn) {
        this.isbn = ISBN.fromString(isbn);
        return this;
    }

    public BookTestBuilder withAuthor(AuthorId authorId) {
        this.authorIds = Set.of(authorId);
        return this;
    }

    public BookTestBuilder withAuthors(Set<AuthorId> authorIds) {
        this.authorIds = authorIds;
        return this;
    }

    public BookTestBuilder withPublisher(String publisher) {
        this.publisher = Publisher.of(publisher);
        return this;
    }

    public BookTestBuilder withYear(int year) {
        this.year = PublicationYear.of(year);
        return this;
    }

    public BookTestBuilder withEdition(int edition) {
        this.edition = Edition.of(edition);
        return this;
    }

    public BookTestBuilder withGenre(Genre genre) {
        this.genre = genre;
        return this;
    }

    public BookTestBuilder withDescription(String description) {
        this.description = Description.of(description);
        return this;
    }

    /**
     * Build the book using the production builder
     */
    public Book build() {
        Book.Builder builder = Book.builder()
            .id(id)
            .title(title)
            .isbn(isbn)
            .authors(authorIds)
            .publicationYear(year.value())
            .edition(edition.value())
            .genre(genre);

        if (publisher != null) {
            builder.publisher(publisher);
        }

        if (description != null) {
            builder.description(description);
        }

        return builder.build();
    }
}
```

**Usage in tests**:
```java
import static com.penrose.bibby.library.book.BookTestBuilder.*;

class BookTest {

    @Test
    void shouldCheckOutAvailableBook() {
        // BEFORE (your current approach): 15+ lines
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        book.setIsbn("978-0000000000");
        book.setStatus(BookStatus.AVAILABLE);
        book.setCheckoutCount(0);
        // ... 10 more lines ...

        // AFTER (with test builder): 1 line!
        Book book = aBook().build();

        book.checkOut();

        assertThat(book.getStatus()).isEqualTo(AvailabilityStatus.CHECKED_OUT);
        assertThat(book.getCheckoutCount()).isEqualTo(1);
    }

    @Test
    void shouldNotCheckOutUnavailableBook() {
        Book book = aBook().build();
        book.checkOut();  // Already checked out

        assertThatThrownBy(() -> book.checkOut())
            .isInstanceOf(BookNotAvailableException.class)
            .hasMessageContaining("status is CHECKED_OUT");
    }

    @Test
    void shouldCreateFictionBookWithCustomTitle() {
        Book book = aFictionBook()
            .withTitle("1984")
            .withYear(1949)
            .build();

        assertThat(book.getTitle().value()).isEqualTo("1984");
        assertThat(book.getGenre()).isEqualTo(Genre.FICTION);
        assertThat(book.getPublicationYear().value()).isEqualTo(1949);
    }

    @Test
    void shouldCreateDDDBook() {
        Book book = aDDDBook().build();

        assertThat(book.getTitle().value()).isEqualTo("Domain-Driven Design");
        assertThat(book.getIsbn().value()).isEqualTo("978-0321125215");
    }
}
```

**Test readability improvement**: From 15+ lines of manual setup to **1 line**!

---

## Step 6: Refactor BookService to Use Factory + Builder

**BEFORE (BookService.java:23-41) - Your current code**:
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
        bookEntity = new BookEntity();       // ❌ Manual creation
        bookEntity.setTitle(title);           // ❌ Setters
    }
    bookEntity.setAuthors(authorEntity);
    bookRepository.save(bookEntity);
}
```

**AFTER (Improved AddBookService.java) - Using Factory**:
```java
package com.penrose.bibby.application.book;

import com.penrose.bibby.library.book.*;
import com.penrose.bibby.library.book.factory.BookFactory;
import com.penrose.bibby.library.author.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AddBookService {

    private final BookFactory bookFactory;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public AddBookService(
        @Qualifier("manualBookFactory") BookFactory bookFactory,
        BookRepository bookRepository,
        AuthorRepository authorRepository
    ) {
        this.bookFactory = bookFactory;
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    @Transactional
    public BookId addBook(AddBookCommand command) {
        // Create or find authors first
        Set<AuthorId> authorIds = command.getAuthorNames().stream()
            .map(this::findOrCreateAuthor)
            .collect(Collectors.toSet());

        // Build creation data
        BookCreationData data = BookCreationData.builder()
            .title(command.getTitle())
            .isbn(command.getIsbn())
            .authorIds(authorIds.stream()
                .map(AuthorId::value)
                .collect(Collectors.toSet()))
            .publisher(command.getPublisher())
            .year(command.getYear())
            .edition(command.getEdition())
            .description(command.getDescription())
            .genre(command.getGenre())
            .build();

        // Factory creates valid book - no setters needed!
        Book book = bookFactory.createBook(data);

        // Save
        bookRepository.save(book);

        return book.getId();
    }

    private AuthorId findOrCreateAuthor(AuthorName name) {
        return authorRepository.findByName(name)
            .map(Author::getId)
            .orElseGet(() -> {
                Author author = Author.create(AuthorId.generate(), name);
                authorRepository.save(author);
                return author.getId();
            });
    }
}
```

**Key improvements**:
1. ✅ **No setters** - factory creates complete books
2. ✅ **Validation centralized** - in value objects and builder
3. ✅ **Separation of concerns** - author creation separate from book creation
4. ✅ **Testable** - mock factory in tests
5. ✅ **Flexible** - swap factories via Spring configuration

---

## Comparing Your Code: Before vs After

### Creating a Book

**BEFORE (your current code)**:
```java
Book book = new Book();
book.setTitle("Clean Code");
book.setIsbn("978-0132350884");
book.setPublisher("Prentice Hall");
book.setPublicationYear(2008);
book.setEdition(1);
book.setStatus(BookStatus.AVAILABLE);
book.setCheckoutCount(0);
book.setCreatedAt(LocalDate.now());
// Did we forget anything? No way to know until runtime!
```

**AFTER (with Builder)**:
```java
Book book = Book.builder()
    .id(BookId.generate())
    .title("Clean Code")
    .isbn("978-0132350884")
    .addAuthor(robertMartinId)
    .publisher("Prentice Hall")
    .publicationYear(2008)
    .edition(1)
    .build();  // Validation ensures book is complete!
```

### In Tests

**BEFORE**:
```java
Book book1 = new Book();
book1.setTitle("Book 1");
book1.setIsbn("111");
// ... 10 more lines

Book book2 = new Book();
book2.setTitle("Book 2");
book2.setIsbn("222");
// ... 10 more lines
```

**AFTER**:
```java
Book book1 = aBook().withTitle("Book 1").build();
Book book2 = aBook().withTitle("Book 2").build();
```

---

## Action Items

### Week 1: Create Value Objects (8-10 hours)

1. **Create `ISBN.java`** (2 hours)
   - Location: `src/main/java/com/penrose/bibby/library/book/ISBN.java`
   - Add validation for ISBN-10 and ISBN-13
   - Write tests: valid ISBNs, invalid ISBNs, null handling
   - Test cleaning (spaces, dashes)

2. **Create `Title.java`** (1 hour)
   - Max length 500, non-blank validation
   - Test: blank titles, too-long titles, trimming

3. **Create `Publisher.java`** (1 hour)
   - Similar to Title
   - Tests for validation

4. **Create `PublicationYear.java`** (1-2 hours)
   - Validate year range (1000 - current year)
   - Test edge cases (future years, too-old years)

5. **Create remaining value objects** (3-4 hours)
   - `Edition`, `Description`
   - `BookId`, `AuthorId`, `ShelfId`
   - `AvailabilityStatus` enum
   - `Genre` enum

### Week 2: Add Builder to Book (6-8 hours)

1. **Add Builder to Book.java** (4-5 hours)
   - Inner static `Builder` class
   - All fluent methods (both String and value object versions)
   - Validation in `build()` method
   - Private constructor

2. **Make fields final** (1 hour)
   - ID, title, ISBN, authors should be final
   - Only status, shelfId, checkoutCount are mutable

3. **Add business methods** (2-3 hours)
   - `checkOut()`, `returnToLibrary()`
   - `placeOnShelf()`, `reportLost()`
   - `isAvailable()`
   - Write tests for each

### Week 3: Create Factories (6-8 hours)

1. **Create `BookFactory` interface** (1 hour)
   - Define `createBook(BookCreationData)` method
   - Define `createBookFromIsbn(String)` method

2. **Create `BookCreationData` DTO** (1-2 hours)
   - Builder for creation data
   - Validation

3. **Implement `ManualEntryBookFactory`** (2-3 hours)
   - Use Book.Builder to create books
   - Full validation
   - Tests with valid and invalid data

4. **Configure Spring** (1 hour)
   - Add `@Component` annotations
   - Configure which factory to inject (manual vs ISBN lookup)

### Week 4: Create Test Builders (4-5 hours)

1. **Create `BookTestBuilder.java`** (3 hours)
   - All convenience methods
   - Preset builders: `aBook()`, `aFictionBook()`, `aTechnicalBook()`, `aDDDBook()`
   - Fluent API matching production builder

2. **Refactor existing tests** (2-3 hours)
   - Find all places creating books in tests
   - Replace with test builders
   - See readability improve dramatically!

3. **Create more test builders** (optional, 2-3 hours)
   - `AuthorTestBuilder`
   - `ShelfTestBuilder`
   - `BookcaseTestBuilder`

### Week 5: Refactor BookService (6-8 hours)

1. **Create AddBookService** (3-4 hours)
   - Inject `BookFactory`
   - Refactor `createNewBook()` to use factory
   - Remove all manual book creation
   - Write tests

2. **Refactor other services** (2-3 hours)
   - Move business logic from service to domain (`Book.checkOut()`)
   - Create `CheckOutBookService`
   - Create `ReturnBookService`

3. **Integration tests** (1-2 hours)
   - Test full flow: command → service → factory → repository
   - Verify validation works end-to-end

---

## Key Takeaways

### 1. Your Current Code Has Fundamental Problems
- ❌ Anemic domain model (getters/setters only)
- ❌ No validation
- ❌ Temporal coupling (setter order matters)
- ❌ Primitive obsession
- ❌ Can create invalid objects

### 2. Factory Pattern Solves Creation Complexity
- ✅ Centralized creation logic
- ✅ Guaranteed valid objects
- ✅ Switchable strategies
- ✅ Open/Closed Principle

### 3. Builder Pattern Solves Construction Complexity
- ✅ Readable object construction
- ✅ Optional parameters
- ✅ Fluent API
- ✅ Immutability becomes practical
- ✅ Essential for tests

### 4. Value Objects Are the Foundation
- ✅ Type safety (can't confuse ISBN with Title)
- ✅ Validation in one place
- ✅ No invalid state possible
- ✅ Clear domain model

### 5. Test Data Builders Transform Testing
- ✅ One-line test data creation
- ✅ Sensible defaults
- ✅ Extremely readable tests
- ✅ Easy to maintain

---

## Further Study

**Books**:
- "Design Patterns" by Gang of Four - Factory Method (p. 107), Builder (p. 97)
- "Effective Java" by Joshua Bloch - Item 1 (Static factories), Item 2 (Builders)
- "Domain-Driven Design" by Eric Evans - Factories chapter
- "Growing Object-Oriented Software, Guided by Tests" - Test Data Builders

**Articles**:
- Martin Fowler: "Test Data Builder"
- Joshua Bloch: "Effective Java" builder pattern
- Baeldung: "Builder Design Pattern in Java"

**Code Examples**:
- Spring Framework: `RestTemplateBuilder`, `WebClientBuilder`
- Lombok: `@Builder` annotation (study what it generates!)
- Your Bibby code: Look for creation anti-patterns

---

## Mentor's Final Notes

This was a **comprehensive** section because we transformed your actual code! Here's what we covered:

1. ✅ **Analyzed your current Bibby code** - identified 5 key problems with line references
2. ✅ **Created value objects** - ISBN, Title, Publisher with validation
3. ✅ **Refactored Book completely** - added Builder, business methods, made immutable
4. ✅ **Created BookFactory** - manual entry with full validation
5. ✅ **Created Test Builders** - `BookTestBuilder` for readable tests
6. ✅ **Refactored BookService** - used factory instead of manual creation
7. ✅ **Provided week-by-week roadmap** - 5 weeks of actionable tasks

**The transformation is dramatic**:

| Aspect | Before (Current Bibby) | After (Refactored) |
|--------|------------------------|-------------------|
| Creation | `new Book()` + setters | `Book.builder()...build()` |
| Validation | None | In value objects + builder |
| Immutability | None | Core fields final |
| Business logic | In service | In domain model |
| Test setup | 15+ lines | 1 line |
| Invalid state | Possible | Impossible |

**Don't try to do everything at once.** Pick one thing:
- Start with `ISBN` value object
- Or start with `BookTestBuilder`
- Or start with `Book.Builder`

Build momentum with small wins. Each improvement makes the next one easier.

**Next section**: Singleton & Prototype - we'll learn when **NOT** to use patterns (just as important!).

You're doing real engineering work. This is how professionals write code.

---

**Section 9 Complete** | **Next**: Section 10 - Singleton & Prototype Patterns
