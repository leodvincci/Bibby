# SECTION 4: REPOSITORIES

## Persisting Aggregates the Domain Way

Welcome to Section 4. In the previous section, we learned about aggregates and aggregate roots. Now we need to answer a crucial question: **How do we persist and retrieve aggregates from the database while preserving the domain model?**

That's where the Repository pattern comes in - but not just any repository pattern. We're talking about **domain-centric repositories** that treat aggregates as collections of objects, not as database tables.

---

## What is a Repository in DDD?

A **Repository** is a domain pattern that provides a collection-like interface for accessing aggregates. It mediates between the domain and data mapping layers, acting like an in-memory collection of aggregates.

### The Collection Metaphor

Think of a repository like a `Set<Book>` in memory:

```java
Set<Book> books = new HashSet<>();

books.add(book);              // Add a book
books.remove(book);           // Remove a book
Book found = findById(id);    // Find by ID
List<Book> fiction = findByGenre("Fiction");  // Query
```

A repository should feel similar:

```java
BookRepository bookRepository = ...;

bookRepository.save(book);                    // Add
bookRepository.delete(book);                  // Remove
Book found = bookRepository.findById(id);     // Find
List<Book> fiction = bookRepository.findByGenre("Fiction");  // Query
```

**The key insight**: Your domain code shouldn't know or care whether books are stored in PostgreSQL, MongoDB, or memory. The repository abstracts that away.

---

## Repository vs. DAO: What's the Difference?

You might have heard of **DAO (Data Access Object)**. It sounds similar to Repository. What's the difference?

### DAO (Data Access Object)
- **Focus**: Database operations
- **Language**: Technical (insert, update, delete, select)
- **Returns**: Database records, DTOs
- **Driven by**: Database schema
- **Example**: `BookDAO.insertBook(BookRecord record)`

### Repository (DDD Pattern)
- **Focus**: Domain collection
- **Language**: Domain terms (save, find, remove)
- **Returns**: Domain aggregates
- **Driven by**: Domain model
- **Example**: `BookRepository.save(Book book)`

### The Fundamental Difference

**DAO** is data-centric: "I'm giving you access to the database table."
**Repository** is domain-centric: "I'm giving you access to the collection of aggregates."

With DAO, you think in terms of SQL and tables. With Repository, you think in terms of domain objects and collections.

---

## Repository Rules in DDD

Eric Evans and Vaughn Vernon established clear guidelines for repositories:

### Rule 1: One Repository per Aggregate Root

**Only create repositories for aggregate roots, never for internal entities.**

✓ Good:
```java
interface BookRepository { }          // Book is aggregate root
interface BookcaseRepository { }      // Bookcase is aggregate root
interface AuthorRepository { }        // Author is aggregate root
```

❌ Bad:
```java
interface ShelfRepository { }  // Shelf is internal to Bookcase - NO repository!
```

**Why?** Repositories are the entry point to aggregates. If Shelf is internal to Bookcase, you access it through BookcaseRepository.

### Rule 2: Repository Methods Use Domain Language

**Methods should be named using ubiquitous language, not database operations.**

✓ Good (domain language):
```java
interface BookRepository {
    void save(Book book);
    Optional<Book> findById(BookId id);
    List<Book> findAvailableBooks();
    void remove(Book book);
}
```

❌ Bad (database language):
```java
interface BookRepository {
    void insertBook(Book book);  // Too SQL-like
    void updateBook(Book book);  // Revealing implementation
    BookRecord selectById(Long id);  // Returning database record
}
```

### Rule 3: Repositories Return Domain Objects

**Repository methods return aggregates and value objects, never DTOs or database records.**

✓ Good:
```java
Optional<Book> findById(BookId id);
List<Book> findByAuthor(AuthorId authorId);
```

❌ Bad:
```java
BookDTO findById(Long id);  // DTO is application layer concern
BookRecord getBookRecord(Long id);  // Database record is infrastructure
```

### Rule 4: Hide Persistence Details

**The domain should not know how aggregates are persisted (SQL, NoSQL, in-memory, etc.).**

This is the **dependency inversion principle** in action:

```
Domain Layer (high level)
    ↓ depends on
Repository Interface (domain)
    ↑ implemented by
Repository Implementation (infrastructure)
```

The domain defines the repository interface. The infrastructure implements it.

---

## Analyzing Your Current Repositories

Let's review your Bibby repositories and see what needs to change.

### Current State: BookRepository.java

```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {
    BookEntity findBookEntityByTitle(String title);  // ❌ Returns Entity, uses String
    BookEntity findByTitle(String title);            // ❌ Duplicate method
    BookEntity findByTitleIgnoreCase(String title);  // ❌ Using String, not Title
    List<BookEntity> findByTitleContaining(String title);  // ❌ String again
    List<BookEntity> findByShelfId(Long id);        // ❌ Exposing internal reference

    @Query(value = "SELECT b.book_id, ...", nativeQuery = true)  // ❌ Native SQL in domain layer
    BookDetailView getBookDetailView(Long bookId);
}
```

**Problems identified**:
1. **Extends JpaRepository**: Exposes 20+ JPA-specific methods to domain layer
2. **Technical naming**: `findByTitleIgnoreCase` is implementation detail
3. **Primitive parameters**: `String title` instead of `Title`, `Long` instead of `BookId`
4. **Returns entities**: Should return domain `Book`, not `BookEntity`
5. **Native queries**: SQL in repository interface violates abstraction
6. **Crossing aggregate boundaries**: Joining across Book/Shelf/Bookcase in query

### Current State: ShelfRepository.java

```java
@Repository
public interface ShelfRepository extends JpaRepository<ShelfEntity, Long> {
    List<ShelfEntity> findByBookcaseId(Long bookcaseId);  // ❌ Exists, but...

    @Query("""
        SELECT new com.penrose.bibby.library.shelf.ShelfSummary(...)
        FROM ShelfEntity s
        LEFT JOIN BookEntity b ...
    """)
    List<ShelfSummary> findShelfSummariesByBookcaseId(...);  // ❌ Crossing boundaries
}
```

**The Big Problem**: This repository exists!

Remember from Section 3: **Shelf is an internal entity within the Bookcase aggregate**. It should NOT have its own repository. External code should access shelves through `BookcaseRepository`.

### Current State: BookcaseRepository.java

```java
@Repository
public interface BookcaseRepository extends JpaRepository<BookcaseEntity, Long> {
    BookcaseEntity findBookcaseEntityByBookcaseLabel(String s);  // ❌ Technical naming
}
```

**Problems**:
1. Method name includes "Entity"
2. Returns `BookcaseEntity` not domain `Bookcase`
3. Should return `Optional<Bookcase>` not nullable

---

## Designing Proper DDD Repositories

Let me show you how to design repositories the DDD way.

### Pattern 1: Interface in Domain, Implementation in Infrastructure

**Package structure**:
```
com.penrose.bibby/
├── domain/
│   ├── model/
│   │   ├── Book.java
│   │   ├── Bookcase.java
│   │   └── Author.java
│   └── repository/              ← Domain layer
│       ├── BookRepository.java  ← Interface only!
│       ├── BookcaseRepository.java
│       └── AuthorRepository.java
├── infrastructure/
│   └── persistence/
│       ├── jpa/
│       │   ├── JpaBookRepository.java  ← Implementation
│       │   ├── JpaBookcaseRepository.java
│       │   ├── JpaAuthorRepository.java
│       │   ├── BookJpaEntity.java      ← JPA entity
│       │   ├── BookcaseJpaEntity.java
│       │   └── AuthorJpaEntity.java
│       └── mapper/
│           ├── BookMapper.java
│           └── ...
```

### Pattern 2: Domain Repository Interface

```java
// domain/repository/BookRepository.java
package com.penrose.bibby.domain.repository;

import com.penrose.bibby.domain.model.*;
import java.util.List;
import java.util.Optional;

/**
 * Repository for the Book aggregate.
 * Provides collection-like access to books.
 */
public interface BookRepository {

    /**
     * Saves a book to the collection.
     * Works for both new books and updates to existing books.
     */
    void save(Book book);

    /**
     * Finds a book by its unique identifier.
     * @return Optional containing the book, or empty if not found
     */
    Optional<Book> findById(BookId id);

    /**
     * Finds a book by its ISBN.
     * @return Optional containing the book, or empty if not found
     */
    Optional<Book> findByIsbn(ISBN isbn);

    /**
     * Finds all books by a specific author.
     */
    List<Book> findByAuthor(AuthorId authorId);

    /**
     * Finds all books currently on a specific shelf.
     */
    List<Book> findByShelf(ShelfId shelfId);

    /**
     * Finds all books with the given availability status.
     */
    List<Book> findByStatus(AvailabilityStatus status);

    /**
     * Searches for books whose title contains the given text.
     */
    List<Book> searchByTitle(String titleFragment);

    /**
     * Finds all books in the collection.
     */
    List<Book> findAll();

    /**
     * Removes a book from the collection.
     */
    void delete(Book book);

    /**
     * Removes a book by ID from the collection.
     */
    void deleteById(BookId id);

    /**
     * Checks if a book with the given ISBN exists.
     */
    boolean existsByIsbn(ISBN isbn);

    /**
     * Returns the total number of books in the collection.
     */
    long count();
}
```

**Notice**:
- No Spring annotations (domain layer is framework-agnostic)
- Domain types (`BookId`, `ISBN`, `Book`)
- Collection-like methods (`save`, `findById`, `delete`)
- Domain language in method names
- Javadoc explains intent, not implementation

### Pattern 3: Infrastructure Implementation

```java
// infrastructure/persistence/jpa/JpaBookRepository.java
package com.penrose.bibby.infrastructure.persistence.jpa;

import com.penrose.bibby.domain.model.*;
import com.penrose.bibby.domain.repository.BookRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
class JpaBookRepositoryImpl implements BookRepository {

    private final SpringDataBookRepository springRepo;
    private final BookMapper mapper;

    public JpaBookRepositoryImpl(SpringDataBookRepository springRepo, BookMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(Book book) {
        BookJpaEntity entity = mapper.toJpaEntity(book);
        springRepo.save(entity);
    }

    @Override
    public Optional<Book> findById(BookId id) {
        return springRepo.findById(id.getValue())
            .map(mapper::toDomainModel);
    }

    @Override
    public Optional<Book> findByIsbn(ISBN isbn) {
        return springRepo.findByIsbn(isbn.getValue())
            .map(mapper::toDomainModel);
    }

    @Override
    public List<Book> findByAuthor(AuthorId authorId) {
        return springRepo.findByAuthorIds(authorId.getValue())
            .stream()
            .map(mapper::toDomainModel)
            .collect(Collectors.toList());
    }

    @Override
    public List<Book> findByShelf(ShelfId shelfId) {
        return springRepo.findByShelfId(shelfId.getValue())
            .stream()
            .map(mapper::toDomainModel)
            .collect(Collectors.toList());
    }

    @Override
    public List<Book> findByStatus(AvailabilityStatus status) {
        return springRepo.findByBookStatus(status.name())
            .stream()
            .map(mapper::toDomainModel)
            .collect(Collectors.toList());
    }

    @Override
    public List<Book> searchByTitle(String titleFragment) {
        return springRepo.findByTitleContainingIgnoreCase(titleFragment)
            .stream()
            .map(mapper::toDomainModel)
            .collect(Collectors.toList());
    }

    @Override
    public List<Book> findAll() {
        return springRepo.findAll()
            .stream()
            .map(mapper::toDomainModel)
            .collect(Collectors.toList());
    }

    @Override
    public void delete(Book book) {
        springRepo.deleteById(book.getId().getValue());
    }

    @Override
    public void deleteById(BookId id) {
        springRepo.deleteById(id.getValue());
    }

    @Override
    public boolean existsByIsbn(ISBN isbn) {
        return springRepo.existsByIsbn(isbn.getValue());
    }

    @Override
    public long count() {
        return springRepo.count();
    }
}

// Internal Spring Data interface (not exposed to domain)
interface SpringDataBookRepository extends JpaRepository<BookJpaEntity, Long> {
    Optional<BookJpaEntity> findByIsbn(String isbn);
    List<BookJpaEntity> findByAuthorIds(Long authorId);
    List<BookJpaEntity> findByShelfId(Long shelfId);
    List<BookJpaEntity> findByBookStatus(String status);
    List<BookJpaEntity> findByTitleContainingIgnoreCase(String title);
    boolean existsByIsbn(String isbn);
}
```

**Key points**:
- Implements domain `BookRepository` interface
- Uses internal Spring Data repository
- Mapper converts between domain and JPA entities
- Domain layer never sees JPA entities
- Spring-specific code isolated to infrastructure

### Pattern 4: Bookcase Repository with Aggregate Loading

Remember: **Shelves are internal to Bookcase**. The repository must load the entire aggregate.

```java
// domain/repository/BookcaseRepository.java
package com.penrose.bibby.domain.repository;

import com.penrose.bibby.domain.model.*;
import java.util.List;
import java.util.Optional;

public interface BookcaseRepository {

    /**
     * Saves a bookcase and all its shelves.
     */
    void save(Bookcase bookcase);

    /**
     * Finds a bookcase by ID, including all its shelves.
     */
    Optional<Bookcase> findById(BookcaseId id);

    /**
     * Finds a bookcase by its label.
     */
    Optional<Bookcase> findByLabel(BookcaseLabel label);

    /**
     * Finds all bookcases in a specific location.
     */
    List<Bookcase> findByLocation(Location location);

    /**
     * Finds all bookcases.
     */
    List<Bookcase> findAll();

    /**
     * Removes a bookcase and all its shelves.
     */
    void delete(Bookcase bookcase);

    /**
     * Checks if a bookcase exists.
     */
    boolean exists(BookcaseId id);
}
```

```java
// infrastructure/persistence/jpa/JpaBookcaseRepositoryImpl.java
@Repository
class JpaBookcaseRepositoryImpl implements BookcaseRepository {

    private final SpringDataBookcaseRepository bookcaseRepo;
    private final SpringDataShelfRepository shelfRepo;  // For loading shelves
    private final BookcaseMapper mapper;

    @Override
    public Optional<Bookcase> findById(BookcaseId id) {
        Optional<BookcaseJpaEntity> entity = bookcaseRepo.findById(id.getValue());

        if (entity.isEmpty()) {
            return Optional.empty();
        }

        // Load all shelves for this bookcase (aggregate boundary)
        List<ShelfJpaEntity> shelfEntities = shelfRepo.findByBookcaseId(id.getValue());

        // Map to domain model
        Bookcase bookcase = mapper.toDomainModel(entity.get(), shelfEntities);
        return Optional.of(bookcase);
    }

    @Override
    public void save(Bookcase bookcase) {
        // Save the bookcase
        BookcaseJpaEntity bookcaseEntity = mapper.toJpaEntity(bookcase);
        bookcaseRepo.save(bookcaseEntity);

        // Save all shelves (part of aggregate)
        List<ShelfJpaEntity> shelfEntities = mapper.shelvesToJpaEntities(bookcase.getShelves());
        shelfRepo.saveAll(shelfEntities);

        // Note: This violates "one table per aggregate" ideal, but is acceptable
        // for aggregates with internal entities
    }

    @Override
    public void delete(Bookcase bookcase) {
        // Delete shelves first (foreign key constraint)
        shelfRepo.deleteByBookcaseId(bookcase.getId().getValue());

        // Then delete bookcase
        bookcaseRepo.deleteById(bookcase.getId().getValue());
    }
}
```

**Important**: Even though we save to two tables (bookcases and shelves), we're saving **one aggregate**. This is acceptable because Shelf is internal to Bookcase.

---

## Spring Data JPA: Friend or Foe?

You're using Spring Data JPA with `JpaRepository`. Is this compatible with DDD?

**Short answer**: Yes, with careful design.

### The Problem with Extending JpaRepository Directly

```java
// ❌ Current approach
public interface BookRepository extends JpaRepository<BookEntity, Long> {
    // Now you have 20+ methods exposed: flush(), saveAndFlush(), deleteInBatch(), etc.
}
```

**Problems**:
1. Domain knows about JPA (implementation detail)
2. Methods leak into domain layer
3. Returns JPA entities, not domain objects
4. Tight coupling to Spring

### The Solution: Wrap Spring Data

**Option 1: Composition (Recommended)**

```java
// Domain interface (no Spring)
public interface BookRepository {
    void save(Book book);
    Optional<Book> findById(BookId id);
}

// Implementation uses Spring Data internally
@Repository
class JpaBookRepositoryImpl implements BookRepository {
    private final SpringDataBookRepository springRepo;  // Internal!

    // Implementation...
}

// Internal Spring Data interface
interface SpringDataBookRepository extends JpaRepository<BookJpaEntity, Long> {
    // Spring-specific methods
}
```

**Option 2: Selective Exposure**

```java
// Domain interface extends specific methods
public interface BookRepository extends Repository<BookEntity, Long> {
    // Only expose what you need
    void save(Book book);
    Optional<Book> findById(Long id);

    // Don't expose: flush, deleteInBatch, etc.
}
```

**Recommendation**: Use Option 1 (composition) for true DDD. Use Option 2 for pragmatic DDD in Spring-heavy projects.

---

## Specifications Pattern for Complex Queries

What about complex queries? DDD provides the **Specification** pattern.

### The Problem

```java
// ❌ Repository method explosion
List<Book> findByTitleContainingAndStatusAndGenre(String title, BookStatus status, Genre genre);
List<Book> findByAuthorAndPublicationYearGreaterThan(Author author, int year);
List<Book> findByStatusOrGenre(BookStatus status, Genre genre);
// ...hundreds more?
```

### The Solution: Specifications

```java
// domain/specification/BookSpecification.java
public interface BookSpecification {
    boolean isSatisfiedBy(Book book);
}

// Concrete specifications
public class AvailableBookSpec implements BookSpecification {
    @Override
    public boolean isSatisfiedBy(Book book) {
        return book.getStatus().isAvailable();
    }
}

public class GenreSpec implements BookSpecification {
    private final Genre genre;

    public GenreSpec(Genre genre) {
        this.genre = genre;
    }

    @Override
    public boolean isSatisfiedBy(Book book) {
        return book.getGenre().equals(genre);
    }
}

// Composite specifications
public class AndSpecification implements BookSpecification {
    private final BookSpecification left;
    private final BookSpecification right;

    public AndSpecification(BookSpecification left, BookSpecification right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean isSatisfiedBy(Book book) {
        return left.isSatisfiedBy(book) && right.isSatisfiedBy(book);
    }
}

// Usage
BookSpecification spec = new AndSpecification(
    new AvailableBookSpec(),
    new GenreSpec(Genre.FICTION)
);

List<Book> books = bookRepository.findAll();
List<Book> matching = books.stream()
    .filter(spec::isSatisfiedBy)
    .collect(Collectors.toList());
```

**For database-level filtering** (more efficient), use JPA Criteria API or Spring Data Specifications.

---

## Action Items

### 1. Redesign Repository Interfaces (3-4 hours)

Create domain repository interfaces:
- `BookRepository.java` in `domain.repository` package
- `BookcaseRepository.java`
- `AuthorRepository.java`
- Use domain types (Book, BookId, ISBN, etc.)
- Use domain language for methods
- NO Spring annotations

### 2. Remove ShelfRepository (1 hour)

- Delete `ShelfRepository.java`
- Move shelf loading into `BookcaseRepository`
- Update any code that used `ShelfRepository`

### 3. Create Mapper Classes (2-3 hours)

Build mappers to convert between domain and JPA entities:
- `BookMapper` - `Book` ↔ `BookJpaEntity`
- `BookcaseMapper` - `Bookcase` ↔ `BookcaseJpaEntity` + `ShelfJpaEntity[]`
- Handle value object conversions (ISBN, Title, etc.)

### 4. Refactor One Repository (4-5 hours)

Pick `BookRepository` and refactor it fully:
- Create domain interface
- Create JPA implementation
- Use mapper
- Update service layer to use new interface
- Test thoroughly

### 5. Document Repository Patterns (1 hour)

Create `repository-design.md`:
- Which aggregates have repositories
- Why Shelf doesn't have a repository
- How aggregates are loaded and saved
- Mapping strategy

---

## Key Takeaways

### 1. One Repository per Aggregate Root
- Only aggregate roots get repositories
- Internal entities accessed through root

### 2. Repositories Are Domain Concepts
- Interface in domain layer
- Implementation in infrastructure layer
- No framework dependencies in domain

### 3. Repositories Use Domain Language
- `save()` not `insert()` or `update()`
- Return domain objects, not DTOs or entities
- Parameters are domain types (BookId, ISBN)

### 4. Hide Persistence Implementation
- Domain doesn't know about JPA, SQL, or database
- Dependency inversion: domain defines interface
- Infrastructure implements interface

### 5. Load Entire Aggregates
- Don't load partial aggregates
- When you load Bookcase, load all Shelves
- Aggregate is unit of persistence

---

## Further Study

**Books**:
- "Domain-Driven Design" by Eric Evans - Chapter 6 (Repositories)
- "Implementing Domain-Driven Design" by Vaughn Vernon - Chapter 12 (Repositories)

**Articles**:
- Martin Fowler: "Repository"
- "Repository Pattern in DDD"
- Spring Data JPA and DDD compatibility

**Patterns**:
- Specification Pattern
- Unit of Work (often implicit in Spring)
- Mapper/Assembler pattern

---

## Mentor's Note

Repositories are where DDD meets the real world of databases and persistence. Getting this right requires balancing two competing forces:

1. **Purity**: Keeping domain layer clean and framework-agnostic
2. **Pragmatism**: Using Spring Data effectively without reinventing the wheel

My recommendation for Bibby:
- Start with proper domain interfaces (purity)
- Use Spring Data internally in implementations (pragmatism)
- Keep JPA entities separate from domain models (compromise)

Don't worry about perfect separation on your first try. As you build this out, you'll discover where the boundaries should be. The key is understanding the principles, then applying them pragmatically.

In Section 5, we'll learn about Domain Services - where to put logic that doesn't belong to any single entity.

You're making great progress. Repositories are often the most confusing part of DDD because they bridge domain and infrastructure. The fact that you're working through this shows real dedication.

See you in Section 5!

---

**Section 4 Complete** | Next: Section 5 - Domain Services
