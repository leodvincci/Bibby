# SECTION 9: CREATIONAL PATTERNS - FACTORY & BUILDER

## Creating Objects the Right Way

Welcome to Part 2! In Part 1, we learned Domain Driven Design - how to model your domain. Now we're learning the Gang of Four Design Patterns - proven solutions to common software design problems.

We're starting with **Creational Patterns** - patterns for creating objects. Specifically: **Factory Method**, **Abstract Factory**, and **Builder**.

You might think "Creating objects is easy - just use `new`!" But as your domain grows complex, object creation becomes a significant design challenge. These patterns solve that.

---

## Why Object Creation Needs Patterns

### The Problem with `new`

Look at this code from your potential future Bibby:

```java
// Creating a book - looks simple...
Book book = new Book();
book.setTitle("Domain-Driven Design");
book.setIsbn("978-0321125215");
book.setAuthors(authors);
book.setPublisher("Addison-Wesley");
book.setPublicationYear(2003);
book.setStatus(BookStatus.AVAILABLE);
book.setCheckoutCount(0);
book.setCreatedAt(LocalDateTime.now());
book.setUpdatedAt(LocalDateTime.now());

// Wait... did I forget to validate?
// Is the book in a valid state?
// What if I forget a required field?
```

**Problems**:
1. **No validation** - easy to create invalid objects
2. **Temporal coupling** - order of calls matters
3. **Boilerplate** - repetitive code everywhere
4. **No encapsulation** - internal structure exposed
5. **Hard to change** - if Book's constructor changes, hundreds of call sites break

### The Solution: Creational Patterns

Creational patterns solve these problems by:
- Encapsulating creation logic
- Ensuring objects are valid upon creation
- Providing flexible construction options
- Hiding complexity from clients

---

## Factory Method Pattern

### Intent

**Define an interface for creating objects, but let subclasses decide which class to instantiate.**

### The Classic Example

```java
// Creator interface
public abstract class BookFactory {
    // Factory method - subclasses implement
    protected abstract Book createBook();

    // Template method using factory method
    public Book orderBook() {
        Book book = createBook();  // Subclass decides which Book
        book.setCreatedAt(LocalDateTime.now());
        return book;
    }
}

// Concrete creators
public class FictionBookFactory extends BookFactory {
    @Override
    protected Book createBook() {
        return new FictionBook();
    }
}

public class TechnicalBookFactory extends BookFactory {
    @Override
    protected Book createBook() {
        return new TechnicalBook();
    }
}

// Usage
BookFactory factory = new FictionBookFactory();
Book book = factory.orderBook();  // Creates FictionBook
```

**Key insight**: The creation method returns an interface/abstract class, but the concrete type is determined by the subclass.

### When to Use Factory Method

1. **You don't know the exact type until runtime**
   - E.g., creating different book types based on genre

2. **You want to delegate creation to subclasses**
   - Each subclass decides what to create

3. **You want to promote loose coupling**
   - Client depends on interface, not concrete class

### Factory Method in Bibby

#### Use Case 1: Creating Books from Different Sources

```java
// domain/factory/BookFactory.java
public interface BookFactory {
    Book createBook(BookCreationData data);
}

// infrastructure/factory/ManualBookFactory.java
public class ManualBookFactory implements BookFactory {
    @Override
    public Book createBook(BookCreationData data) {
        // Create book from manual entry
        return new Book(
            BookId.generate(),
            new Title(data.getTitle()),
            ISBN.fromString(data.getIsbn()),
            parseAuthors(data.getAuthors())
        );
    }
}

// infrastructure/factory/IsbnLookupBookFactory.java
public class IsbnLookupBookFactory implements BookFactory {

    private final IsbnLookupService isbnService;

    @Override
    public Book createBook(BookCreationData data) {
        // Fetch metadata from external API
        BookMetadata metadata = isbnService.lookup(data.getIsbn());

        // Create book with enriched data
        return new Book(
            BookId.generate(),
            new Title(metadata.getTitle()),
            ISBN.fromString(data.getIsbn()),
            metadata.getAuthors()
        );
    }
}

// Usage in application service
@Service
public class AddBookService {
    private final BookFactory bookFactory;  // Injected!

    public BookId addBook(AddBookCommand command) {
        // Factory creates the book (manual or ISBN lookup)
        Book book = bookFactory.createBook(command.getData());

        bookRepository.save(book);
        return book.getId();
    }
}
```

**Benefits**:
- Switch between manual entry and ISBN lookup by configuration
- Add new creation strategies without changing AddBookService
- Open/Closed Principle: open for extension, closed for modification

#### Use Case 2: Creating Domain Events

```java
// domain/event/factory/DomainEventFactory.java
public interface DomainEventFactory {
    DomainEvent createEvent(Book book, EventType type);
}

public class BookEventFactory implements DomainEventFactory {
    @Override
    public DomainEvent createEvent(Book book, EventType type) {
        return switch (type) {
            case CHECKED_OUT -> new BookCheckedOutEvent(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                LocalDateTime.now(),
                book.getCheckoutCount()
            );
            case RETURNED -> new BookReturnedEvent(
                book.getId(),
                LocalDateTime.now()
            );
            case MOVED_TO_SHELF -> new BookMovedToShelfEvent(
                book.getId(),
                book.getCurrentShelfId(),
                LocalDateTime.now()
            );
        };
    }
}
```

---

## Abstract Factory Pattern

### Intent

**Provide an interface for creating families of related objects without specifying their concrete classes.**

### The Difference from Factory Method

- **Factory Method**: Creates one type of object
- **Abstract Factory**: Creates families of related objects

### Classic Example

```java
// Abstract factory
public interface UIFactory {
    Button createButton();
    TextField createTextField();
}

// Concrete factories for different themes
public class LightThemeFactory implements UIFactory {
    @Override
    public Button createButton() {
        return new LightButton();
    }

    @Override
    public TextField createTextField() {
        return new LightTextField();
    }
}

public class DarkThemeFactory implements UIFactory {
    @Override
    public Button createButton() {
        return new DarkButton();
    }

    @Override
    public TextField createTextField() {
        return new DarkTextField();
    }
}

// Usage
UIFactory factory = new DarkThemeFactory();
Button button = factory.createButton();  // DarkButton
TextField field = factory.createTextField();  // DarkTextField
```

**Key insight**: All products created by one factory are designed to work together.

### Abstract Factory in Bibby

#### Use Case: Creating Aggregates with Dependencies

```java
// domain/factory/LibraryAggregateFactory.java
public interface LibraryAggregateFactory {
    Book createBook(BookId id, Title title, ISBN isbn);
    Author createAuthor(AuthorId id, AuthorName name);
    Bookcase createBookcase(BookcaseId id, BookcaseLabel label);
    Shelf createShelf(ShelfId id, ShelfLabel label, BookcaseId bookcaseId);
}

// implementation
public class DefaultLibraryAggregateFactory implements LibraryAggregateFactory {

    @Override
    public Book createBook(BookId id, Title title, ISBN isbn) {
        Book book = new Book(id, title, isbn, new HashSet<>());
        // Set default values
        book.placeOnShelf(ShelfId.unassigned());
        return book;
    }

    @Override
    public Author createAuthor(AuthorId id, AuthorName name) {
        return new Author(id, name);
    }

    @Override
    public Bookcase createBookcase(BookcaseId id, BookcaseLabel label) {
        return new Bookcase(
            id,
            label,
            Location.defaultLocation(),
            10  // Default capacity
        );
    }

    @Override
    public Shelf createShelf(ShelfId id, ShelfLabel label, BookcaseId bookcaseId) {
        return new Shelf(id, label, 0, bookcaseId);
    }
}
```

**Benefits**:
- Consistent creation of related aggregates
- Easy to create test doubles (TestLibraryAggregateFactory)
- Centralized creation logic

---

## Builder Pattern

### Intent

**Separate the construction of a complex object from its representation, allowing the same construction process to create different representations.**

### Why Builder Matters

Remember from Section 2: Value objects and entities should be immutable. But how do you construct complex immutable objects?

❌ **Bad** (telescoping constructors):
```java
public Book(BookId id, Title title) { ... }
public Book(BookId id, Title title, ISBN isbn) { ... }
public Book(BookId id, Title title, ISBN isbn, Set<AuthorId> authors) { ... }
public Book(BookId id, Title title, ISBN isbn, Set<AuthorId> authors, Publisher publisher) { ... }
// 20 more constructors...
```

✓ **Good** (builder):
```java
Book book = Book.builder()
    .id(bookId)
    .title(title)
    .isbn(isbn)
    .authors(authors)
    .publisher(publisher)
    .build();
```

### Classic Builder Pattern

```java
public class Book {
    // Fields (final for immutability)
    private final BookId id;
    private final Title title;
    private final ISBN isbn;
    private final Set<AuthorId> authorIds;
    private final Publisher publisher;

    // Private constructor
    private Book(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.isbn = builder.isbn;
        this.authorIds = builder.authorIds;
        this.publisher = builder.publisher;

        // Validation
        validate();
    }

    private void validate() {
        if (id == null) throw new IllegalStateException("Book ID required");
        if (title == null) throw new IllegalStateException("Title required");
        if (authorIds.isEmpty()) throw new IllegalStateException("At least one author required");
    }

    // Static builder method
    public static Builder builder() {
        return new Builder();
    }

    // Builder class
    public static class Builder {
        private BookId id;
        private Title title;
        private ISBN isbn;
        private Set<AuthorId> authorIds = new HashSet<>();
        private Publisher publisher;

        // Fluent methods
        public Builder id(BookId id) {
            this.id = id;
            return this;
        }

        public Builder title(Title title) {
            this.title = title;
            return this;
        }

        public Builder isbn(ISBN isbn) {
            this.isbn = isbn;
            return this;
        }

        public Builder author(AuthorId authorId) {
            this.authorIds.add(authorId);
            return this;
        }

        public Builder authors(Set<AuthorId> authorIds) {
            this.authorIds = new HashSet<>(authorIds);
            return this;
        }

        public Builder publisher(Publisher publisher) {
            this.publisher = publisher;
            return this;
        }

        // Build method
        public Book build() {
            return new Book(this);
        }
    }
}

// Usage
Book book = Book.builder()
    .id(BookId.generate())
    .title(new Title("Clean Code"))
    .isbn(ISBN.fromString("978-0132350884"))
    .author(authorId)
    .publisher(new Publisher("Prentice Hall"))
    .build();
```

### Builder Benefits

1. **Readable code** - clear what each parameter is
2. **Optional parameters** - don't need to pass null
3. **Immutability** - object is immutable after creation
4. **Validation** - validate in build() method
5. **Flexible construction** - different combinations of parameters

### Builder Pattern in Bibby

#### Use Case 1: Building Books

```java
// domain/model/Book.java
public class Book {
    private final BookId id;
    private final Title title;
    private final ISBN isbn;
    private final Set<AuthorId> authorIds;
    private final Publisher publisher;
    private final PublicationYear publicationYear;
    private final Edition edition;
    private final Description description;

    private Book(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "ID required");
        this.title = Objects.requireNonNull(builder.title, "Title required");
        this.isbn = Objects.requireNonNull(builder.isbn, "ISBN required");
        this.authorIds = new HashSet<>(builder.authorIds);
        this.publisher = builder.publisher;
        this.publicationYear = builder.publicationYear;
        this.edition = builder.edition;
        this.description = builder.description;

        if (authorIds.isEmpty()) {
            throw new IllegalArgumentException("Book must have at least one author");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BookId id;
        private Title title;
        private ISBN isbn;
        private Set<AuthorId> authorIds = new HashSet<>();
        private Publisher publisher;
        private PublicationYear publicationYear;
        private Edition edition;
        private Description description;

        public Builder id(BookId id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = new Title(title);
            return this;
        }

        public Builder title(Title title) {
            this.title = title;
            return this;
        }

        public Builder isbn(String isbn) {
            this.isbn = ISBN.fromString(isbn);
            return this;
        }

        public Builder isbn(ISBN isbn) {
            this.isbn = isbn;
            return this;
        }

        public Builder addAuthor(AuthorId authorId) {
            this.authorIds.add(authorId);
            return this;
        }

        public Builder authors(Set<AuthorId> authorIds) {
            this.authorIds = new HashSet<>(authorIds);
            return this;
        }

        public Builder publisher(String publisher) {
            this.publisher = new Publisher(publisher);
            return this;
        }

        public Builder publisher(Publisher publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder publicationYear(int year) {
            this.publicationYear = new PublicationYear(year);
            return this;
        }

        public Builder edition(int edition) {
            this.edition = new Edition(edition);
            return this;
        }

        public Builder description(String description) {
            this.description = new Description(description);
            return this;
        }

        public Book build() {
            return new Book(this);
        }
    }
}

// Usage
Book book = Book.builder()
    .id(BookId.generate())
    .title("Domain-Driven Design")
    .isbn("978-0321125215")
    .addAuthor(ericEvansId)
    .publisher("Addison-Wesley")
    .publicationYear(2003)
    .edition(1)
    .description("The definitive guide to DDD")
    .build();
```

#### Use Case 2: Test Data Builders

Builders are ESSENTIAL for testing:

```java
// test/builders/BookTestBuilder.java
public class BookTestBuilder {

    private BookId id = BookId.generate();
    private Title title = new Title("Test Book");
    private ISBN isbn = ISBN.fromString("978-0000000000");
    private Set<AuthorId> authorIds = Set.of(AuthorId.generate());
    private Publisher publisher = new Publisher("Test Publisher");

    public static BookTestBuilder aBook() {
        return new BookTestBuilder();
    }

    public BookTestBuilder withId(BookId id) {
        this.id = id;
        return this;
    }

    public BookTestBuilder withTitle(String title) {
        this.title = new Title(title);
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

    public BookTestBuilder fiction() {
        this.title = new Title("A Fiction Book");
        return this;
    }

    public BookTestBuilder technical() {
        this.title = new Title("A Technical Book");
        return this;
    }

    public Book build() {
        return Book.builder()
            .id(id)
            .title(title)
            .isbn(isbn)
            .authors(authorIds)
            .publisher(publisher)
            .build();
    }
}

// Usage in tests
@Test
void shouldCheckOutBook() {
    // Readable test data creation
    Book book = aBook()
        .withTitle("Clean Code")
        .technical()
        .build();

    book.checkOut();

    assertThat(book.getStatus()).isEqualTo(AvailabilityStatus.CHECKED_OUT);
}
```

---

## Factory vs. Builder: When to Use Each

### Use Factory When:
- Creating different types based on input
- Encapsulating complex creation logic
- You need polymorphism (interface/abstract class return type)
- Creation logic varies by subclass

### Use Builder When:
- Many constructor parameters (4+)
- Many optional parameters
- Need readable object construction
- Building complex immutable objects
- Creating test data

### Both Together

You can combine patterns!

```java
// Factory that uses Builder
public class BookFactory {

    public Book createFictionBook(String title, String isbn) {
        return Book.builder()
            .id(BookId.generate())
            .title(title)
            .isbn(isbn)
            .addAuthor(AuthorId.unknown())
            .build();
    }

    public Book createTechnicalBook(String title, String isbn, Set<AuthorId> authors) {
        return Book.builder()
            .id(BookId.generate())
            .title(title)
            .isbn(isbn)
            .authors(authors)
            .build();
    }
}
```

---

## Connecting to DDD

These patterns support DDD principles:

### Factory Method → Aggregate Creation
- Factories create properly initialized aggregates
- Ensure invariants are met from the start
- Encapsulate complex creation logic

### Builder → Value Objects
- Value objects are immutable
- Builders make creating complex value objects readable
- Validation in build() ensures validity

### Abstract Factory → Bounded Contexts
- Different factories for different contexts
- Create families of related domain objects
- Easy to swap implementations

---

## Action Items

### 1. Implement Builder for Book (4-5 hours)

Create a proper Builder for your Book domain model:
- Inner static Builder class
- Fluent API
- Validation in build()
- Support both String and value object parameters
- Test it thoroughly

### 2. Create Test Data Builders (3-4 hours)

Build test data builders:
- `BookTestBuilder`
- `AuthorTestBuilder`
- `BookcaseTestBuilder`

Use them in at least 3 existing tests to see the readability improvement.

### 3. Implement Factory for Book Creation (2-3 hours)

Create `BookFactory` with methods:
- `createFromManualEntry()`
- `createFromIsbn()`
- Make it configurable via Spring

### 4. Refactor Existing Creation Code (3-4 hours)

Find places where you create Books:
- Replace with Builder pattern
- Ensure proper validation
- Remove telescoping constructors

### 5. Study Real-World Examples (1-2 hours)

Look at:
- Java's `StringBuilder` (builder pattern)
- Spring's `RestTemplateBuilder`
- Your favorite libraries' factory patterns

---

## Key Takeaways

### 1. Object Creation is Design
- How you create objects affects maintainability
- Patterns make creation explicit and flexible

### 2. Factory Method for Polymorphism
- Defer creation to subclasses
- Return interfaces, not concrete classes
- Promotes loose coupling

### 3. Builder for Complex Construction
- Readable, fluent API
- Immutable objects
- Essential for testing

### 4. Patterns Combine
- Use factory + builder together
- Abstract factory creates families
- Choose based on problem

### 5. DDD Benefits
- Factories ensure aggregates are valid
- Builders make value objects practical
- Patterns support domain model integrity

---

## Further Study

**Books**:
- "Design Patterns" by Gang of Four - Factory Method (107), Abstract Factory (87), Builder (97)
- "Effective Java" by Joshua Bloch - Item 1 (Static factories), Item 2 (Builders)
- "Head First Design Patterns" - Chapter 4 (Factory)

**Code Examples**:
- Study Spring Framework's use of factories
- Look at Lombok's `@Builder` annotation
- Review Java's Collections factory methods

---

## Mentor's Note

Factory and Builder might seem like overkill for simple objects. They are. Use them when complexity justifies them:

**When to use Builder**:
- 4+ constructor parameters
- Multiple optional fields
- You're creating test data

**When NOT to use Builder**:
- Simple 2-3 parameter objects
- Value objects with required fields only
- Over-engineering simple cases

The patterns exist to solve real problems. Use them when you have those problems, not "just because."

In Section 10, we'll cover Singleton and Prototype - two more creational patterns that are often misused. Understanding when NOT to use them is as important as understanding when to use them.

Great start to Part 2!

---

**Section 9 Complete** | Next: Section 10 - Creational Patterns: Singleton & Prototype
