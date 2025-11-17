# SECTION 7: LAYERED ARCHITECTURE & HEXAGONAL ARCHITECTURE

## Structuring Your DDD Application

Welcome to Section 7. We've learned the tactical patterns (entities, value objects, aggregates, repositories, services, events). Now we need to answer: **How do we organize all of these into a cohesive, maintainable architecture?**

This section covers two complementary approaches:
1. **Layered Architecture** - The traditional DDD structure
2. **Hexagonal Architecture (Ports & Adapters)** - A more flexible, testable approach

Both serve the same goal: **protect your domain model from infrastructure concerns**.

---

## The Dependency Rule

Before we dive into architectures, understand this fundamental principle:

**Dependencies point inward, toward the domain.**

```
UI/Infrastructure → Application → Domain
(depends on)       (depends on)   (depends on nothing)
```

**The domain layer has ZERO dependencies on:**
- Spring Framework
- JPA/Hibernate
- Database
- REST controllers
- External APIs

**Why?** The domain represents your business logic. Business logic doesn't care about HTTP, databases, or frameworks. Those are implementation details.

This is **Dependency Inversion Principle** in action.

---

## Layered Architecture

The classic DDD approach uses four layers:

### The Four Layers

```
┌─────────────────────────────────────────┐
│   USER INTERFACE / PRESENTATION         │ ← REST controllers, CLI commands
├─────────────────────────────────────────┤
│   APPLICATION LAYER                     │ ← Use cases, DTOs, orchestration
├─────────────────────────────────────────┤
│   DOMAIN LAYER                          │ ← Entities, value objects, domain services
├─────────────────────────────────────────┤
│   INFRASTRUCTURE LAYER                  │ ← JPA, external APIs, file I/O
└─────────────────────────────────────────┘
```

Let's examine each layer.

### Layer 1: User Interface / Presentation

**Purpose**: Handle user interaction (REST API, CLI, web UI)

**Responsibilities**:
- Receive user input
- Call application services
- Return responses
- Format output

**Contains**:
- REST controllers (`@RestController`)
- Spring Shell commands (`@ShellCommand`)
- Web pages / templates
- DTOs for API responses

**Example** (Bibby):
```java
// interfaces/rest/BookController.java
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final AddBookToCollectionService addBookService;
    private final CheckOutBookService checkOutBookService;

    @PostMapping
    public ResponseEntity<BookResponse> addBook(@RequestBody AddBookRequest request) {
        // Convert API DTO to application command
        AddBookCommand command = toCommand(request);

        // Call application service
        BookId bookId = addBookService.execute(command);

        // Convert domain result to API DTO
        BookResponse response = toResponse(bookId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<Void> checkOutBook(@PathVariable Long id) {
        checkOutBookService.execute(new BookId(id));
        return ResponseEntity.ok().build();
    }
}
```

**Key point**: Controllers are thin. They translate between HTTP and application layer.

### Layer 2: Application Layer

**Purpose**: Orchestrate use cases, coordinate domain objects

**Responsibilities**:
- Define application-specific operations (use cases)
- Manage transactions
- Convert between DTOs and domain objects
- Coordinate domain objects and domain services
- Handle application-level security

**Contains**:
- Application services (use case handlers)
- Commands and queries (CQRS pattern)
- Application DTOs
- Application exceptions

**Example** (Bibby):
```java
// application/service/AddBookToCollectionService.java
@Service
@Transactional
public class AddBookToCollectionService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final UniqueIsbnValidator isbnValidator;

    public BookId execute(AddBookCommand command) {
        // Parse value objects
        Title title = new Title(command.getTitle());
        ISBN isbn = ISBN.fromString(command.getIsbn());

        // Domain validation
        isbnValidator.ensureUnique(isbn);

        // Resolve authors (application orchestration)
        Set<AuthorId> authorIds = resolveAuthors(command.getAuthors());

        // Create domain object
        Book book = new Book(BookId.generate(), title, isbn, authorIds);

        // Persist via repository
        bookRepository.save(book);

        // Publish events (if using explicit publishing)
        book.getDomainEvents().forEach(eventPublisher::publishEvent);

        return book.getId();
    }

    private Set<AuthorId> resolveAuthors(List<AuthorCommand> authorCommands) {
        // Find or create authors - application logic
        // ...
    }
}
```

**Key point**: Application layer is thin orchestration. Domain layer has the business logic.

### Layer 3: Domain Layer

**Purpose**: Core business logic and domain model

**Responsibilities**:
- Define entities, value objects, aggregates
- Enforce business rules and invariants
- Domain services for cross-aggregate operations
- Repository interfaces (not implementations!)
- Domain events

**Contains**:
- Entities (Book, Author, Bookcase)
- Value objects (ISBN, Title, AuthorName)
- Aggregates
- Domain services
- Repository interfaces
- Domain events
- Domain exceptions

**Example** (Bibby):
```java
// domain/model/Book.java
public class Book {
    private final BookId id;
    private Title title;
    private ISBN isbn;
    private Set<AuthorId> authorIds;
    private AvailabilityStatus status;

    // Domain behavior
    public void checkOut() {
        if (!status.canCheckOut()) {
            throw new BookNotAvailableException();
        }
        this.status = AvailabilityStatus.CHECKED_OUT;
        this.checkoutCount++;

        registerEvent(new BookCheckedOutEvent(this.id, LocalDateTime.now()));
    }

    // More domain logic...
}

// domain/repository/BookRepository.java (interface only!)
public interface BookRepository {
    void save(Book book);
    Optional<Book> findById(BookId id);
    // ...
}
```

**CRITICAL**: Domain layer has NO framework dependencies. No `@Entity`, no `@Service`, no Spring at all!

### Layer 4: Infrastructure Layer

**Purpose**: Technical implementation details

**Responsibilities**:
- Implement repository interfaces (JPA, JDBC, etc.)
- External API clients
- File system access
- Email sending
- Caching
- Messaging

**Contains**:
- JPA repository implementations
- JPA entities (separate from domain!)
- Mappers (domain ↔ JPA)
- External service clients
- Configuration classes

**Example** (Bibby):
```java
// infrastructure/persistence/jpa/BookJpaEntity.java
@Entity
@Table(name = "books")
class BookJpaEntity {  // Package-private! Not exposed to domain.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String isbn;
    // ... JPA annotations everywhere

    // Getters/setters for JPA
}

// infrastructure/persistence/jpa/JpaBookRepositoryImpl.java
@Repository
class JpaBookRepositoryImpl implements BookRepository {  // Implements domain interface

    private final SpringDataBookRepository springRepo;
    private final BookMapper mapper;

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
}

// Internal Spring Data interface
interface SpringDataBookRepository extends JpaRepository<BookJpaEntity, Long> {
}
```

**Key point**: Infrastructure implements domain interfaces. Domain doesn't know about infrastructure.

---

## Package Structure for Layered Architecture

### Recommended Structure for Bibby

```
com.penrose.bibby/
├── domain/
│   ├── model/
│   │   ├── book/
│   │   │   ├── Book.java              (Entity)
│   │   │   ├── BookId.java            (Value Object)
│   │   │   ├── ISBN.java              (Value Object)
│   │   │   ├── Title.java             (Value Object)
│   │   │   └── AvailabilityStatus.java (Enum)
│   │   ├── author/
│   │   │   ├── Author.java
│   │   │   ├── AuthorId.java
│   │   │   └── AuthorName.java
│   │   ├── bookcase/
│   │   │   ├── Bookcase.java
│   │   │   ├── BookcaseId.java
│   │   │   ├── Shelf.java             (Internal entity)
│   │   │   └── ShelfId.java
│   │   └── shared/                    (Shared value objects)
│   │       ├── Description.java
│   │       └── Location.java
│   ├── repository/                    (Interfaces only!)
│   │   ├── BookRepository.java
│   │   ├── AuthorRepository.java
│   │   └── BookcaseRepository.java
│   ├── service/                       (Domain services)
│   │   ├── UniqueIsbnValidator.java
│   │   └── BookRelocator.java
│   ├── event/
│   │   ├── BookCheckedOutEvent.java
│   │   ├── BookReturnedEvent.java
│   │   └── BookMovedToShelfEvent.java
│   └── exception/
│       ├── BookNotFoundException.java
│       ├── BookNotAvailableException.java
│       └── DuplicateIsbnException.java
│
├── application/
│   ├── service/                       (Use cases)
│   │   ├── AddBookToCollectionService.java
│   │   ├── CheckOutBookService.java
│   │   ├── ReturnBookService.java
│   │   └── SearchBooksService.java
│   ├── command/                       (Input DTOs)
│   │   ├── AddBookCommand.java
│   │   └── CheckOutBookCommand.java
│   ├── query/
│   │   └── BookSearchQuery.java
│   └── dto/
│       ├── BookDto.java
│       └── AuthorDto.java
│
├── infrastructure/
│   ├── persistence/
│   │   ├── jpa/
│   │   │   ├── entity/
│   │   │   │   ├── BookJpaEntity.java
│   │   │   │   ├── AuthorJpaEntity.java
│   │   │   │   └── BookcaseJpaEntity.java
│   │   │   ├── repository/
│   │   │   │   ├── JpaBookRepositoryImpl.java
│   │   │   │   ├── SpringDataBookRepository.java
│   │   │   │   └── JpaAuthorRepositoryImpl.java
│   │   │   └── mapper/
│   │   │       ├── BookMapper.java
│   │   │       └── AuthorMapper.java
│   │   └── config/
│   │       └── JpaConfig.java
│   ├── external/                      (External APIs)
│   │   └── isbn/
│   │       └── IsbnLookupClient.java
│   └── config/
│       ├── SpringConfig.java
│       └── EventConfig.java
│
├── interfaces/                        (Presentation)
│   ├── rest/
│   │   ├── BookController.java
│   │   ├── AuthorController.java
│   │   └── dto/
│   │       ├── AddBookRequest.java
│   │       ├── BookResponse.java
│   │       └── ErrorResponse.java
│   └── cli/
│       ├── BookCommands.java
│       └── BookcaseCommands.java
│
└── BibbyApplication.java              (Spring Boot main)
```

### Current Problems in Your Package Structure

Looking at your current code:

```
com.penrose.bibby/
├── library/
│   ├── book/
│   │   ├── Book.java              ← Domain model
│   │   ├── BookEntity.java        ← JPA entity  ❌ Mixed!
│   │   ├── BookService.java       ← Mix of everything  ❌
│   │   ├── BookRepository.java    ← JPA repo directly  ❌
│   │   └── BookController.java    ← Presentation  ❌
```

**Problems**:
1. Domain and infrastructure mixed in same package
2. No clear layer separation
3. Can't tell what depends on what
4. Hard to test domain in isolation

---

## Hexagonal Architecture (Ports & Adapters)

Hexagonal architecture (also called "Ports and Adapters") is an evolution of layered architecture that makes dependencies even clearer.

### The Hexagon Concept

```
          ┌─────────────────────────┐
          │   Primary Adapters      │
          │  (UI, REST, CLI)        │
          └─────────┬───────────────┘
                    │ calls
         ┌──────────▼──────────────┐
         │     Primary Ports       │
         │  (Application Services) │
         │─────────────────────────│
         │      DOMAIN CORE        │  ← Pure business logic
         │  (Entities, Services)   │
         │─────────────────────────│
         │    Secondary Ports      │
         │  (Repository Interfaces)│
         └──────────┬──────────────┘
                    │ implemented by
          ┌─────────▼───────────────┐
          │  Secondary Adapters     │
          │ (JPA, External APIs)    │
          └─────────────────────────┘
```

### Terminology

**Port**: An interface that defines how to interact with the domain

**Adapter**: An implementation that adapts external systems to ports

**Primary (Driving) Port**: Exposes domain functionality (Application services)
**Primary (Driving) Adapter**: Calls the domain (REST controller, CLI)

**Secondary (Driven) Port**: Required by domain (Repository interface)
**Secondary (Driven) Adapter**: Implements what domain needs (JPA repository)

### Example: Bibby in Hexagonal Architecture

**Primary Port** (Domain exposes):
```java
// application/port/in/AddBookUseCase.java
public interface AddBookUseCase {
    BookId addBook(AddBookCommand command);
}

// application/service/AddBookService.java (implements port)
@Service
public class AddBookService implements AddBookUseCase {
    @Override
    public BookId addBook(AddBookCommand command) {
        // Use case implementation
    }
}
```

**Primary Adapter** (Calls domain):
```java
// interfaces/rest/BookController.java
@RestController
public class BookController {

    private final AddBookUseCase addBookUseCase;  // Depends on port, not implementation

    @PostMapping("/books")
    public BookResponse addBook(@RequestBody AddBookRequest request) {
        AddBookCommand command = toCommand(request);
        BookId id = addBookUseCase.addBook(command);  // Call through port
        return toResponse(id);
    }
}
```

**Secondary Port** (Domain requires):
```java
// domain/repository/BookRepository.java (port - interface)
public interface BookRepository {
    void save(Book book);
    Optional<Book> findById(BookId id);
}
```

**Secondary Adapter** (Implements what domain needs):
```java
// infrastructure/persistence/JpaBookRepositoryAdapter.java
@Repository
public class JpaBookRepositoryAdapter implements BookRepository {
    // Adapts JPA to domain repository interface
}
```

### Benefits of Hexagonal Architecture

1. **Testability**: Can test domain without any infrastructure
2. **Flexibility**: Easy to swap implementations (e.g., SQL → NoSQL)
3. **Clear dependencies**: All dependencies point toward domain
4. **Framework independence**: Domain knows nothing about Spring, JPA, etc.

---

## Current State Analysis: Bibby

Let's map your current code to proper architecture.

### What You Have Now (Mixed Layers)

```
BookController → BookService → BookRepository → Database
                      ↓
                 BookEntity (JPA)
```

**Problems**:
1. `BookService` mixes application + domain + infrastructure
2. `BookEntity` has JPA annotations (domain + infrastructure mixed)
3. No clear separation of concerns
4. Can't test domain without Spring/JPA

### What It Should Be (Layered/Hexagonal)

```
BookController → AddBookService → Book (domain) → BookRepository (interface)
                                                          ↑
                                                   JpaBookRepositoryImpl
                                                          ↓
                                                   BookJpaEntity
```

**Benefits**:
1. Domain (`Book`) is pure Java
2. Application service orchestrates
3. Infrastructure implements repositories
4. Can test each layer independently

---

## Refactoring Roadmap

### Phase 1: Separate Domain from Infrastructure

**Current**:
```java
@Entity
public class Book {  // Domain + JPA mixed
    @Id
    private Long id;
    // ... getters/setters
}
```

**Target**:
```java
// domain/model/Book.java (pure domain)
public class Book {
    private final BookId id;
    // ... domain logic
}

// infrastructure/persistence/jpa/BookJpaEntity.java
@Entity
class BookJpaEntity {
    @Id
    private Long id;
    // ... JPA stuff
}
```

### Phase 2: Create Proper Repository Interfaces

**Current**:
```java
public interface BookRepository extends JpaRepository<BookEntity, Long> {
    // Exposed to domain layer!
}
```

**Target**:
```java
// domain/repository/BookRepository.java
public interface BookRepository {
    void save(Book book);
    Optional<Book> findById(BookId id);
}

// infrastructure/persistence/jpa/JpaBookRepositoryImpl.java
@Repository
class JpaBookRepositoryImpl implements BookRepository {
    // Adapts Spring Data to domain interface
}
```

### Phase 3: Split BookService

**Current**:
```java
@Service
public class BookService {
    // Does everything!
}
```

**Target**:
```java
// application/service/AddBookService.java
@Service
public class AddBookService {
    // One use case
}

// application/service/CheckOutBookService.java
@Service
public class CheckOutBookService {
    // Another use case
}

// domain/model/Book.java
public class Book {
    public void checkOut() {
        // Domain logic here!
    }
}
```

### Phase 4: Reorganize Packages

Move files to proper packages:
- Domain models → `domain.model`
- Repository interfaces → `domain.repository`
- Application services → `application.service`
- JPA entities → `infrastructure.persistence.jpa.entity`
- Repository impls → `infrastructure.persistence.jpa.repository`
- Controllers → `interfaces.rest` or `interfaces.cli`

---

## Action Items

### 1. Draw Current Architecture (1-2 hours)

Create diagrams:
- Current package structure
- Current dependencies (what depends on what)
- Identify violations of layered architecture

### 2. Design Target Architecture (2-3 hours)

Create:
- Target package structure
- Layer diagram showing dependencies
- List of files to move/create/refactor

### 3. Create Domain Package (1 hour)

Create new package structure:
```
domain/
├── model/
├── repository/
├── service/
├── event/
└── exception/
```

Start moving pure domain code (no Spring, no JPA annotations).

### 4. Separate One Entity (4-5 hours)

Choose Book entity and separate:
- Create pure `Book` domain model
- Create `BookJpaEntity` infrastructure entity
- Create `BookMapper` to convert between them
- Update repository implementation

### 5. Extract One Application Service (3-4 hours)

Take one use case from `BookService`:
- Create dedicated application service class
- Move logic from `BookService` to new class
- Move domain logic to `Book` entity
- Test the separated concern

---

## Key Takeaways

### 1. Dependencies Point Inward
- Domain depends on nothing
- Application depends on domain
- Infrastructure depends on domain (implements interfaces)
- UI depends on application

### 2. Domain is Framework-Agnostic
- No Spring annotations in domain
- No JPA in domain
- Pure Java business logic

### 3. Infrastructure Implements Domain Interfaces
- Domain defines repository interfaces
- Infrastructure provides JPA implementations
- Dependency inversion in action

### 4. Application Layer Orchestrates
- Thin use case handlers
- Converts DTOs to domain objects
- Manages transactions
- Calls domain services

### 5. Hexagonal Architecture Clarifies Ports/Adapters
- Ports = interfaces (what the domain provides/requires)
- Adapters = implementations (how external systems connect)
- Makes testing easier

---

## Further Study

**Books**:
- "Domain-Driven Design" by Eric Evans - Chapter 4 (Layered Architecture)
- "Implementing Domain-Driven Design" by Vaughn Vernon - Chapter 4 (Architecture)
- "Clean Architecture" by Robert C. Martin
- "Hexagonal Architecture" by Alistair Cockburn

**Articles**:
- Martin Fowler: "Presentation Domain Data Layering"
- Alistair Cockburn: "Hexagonal Architecture"
- Netflix Tech Blog: "Hexagonal Architecture"

**Patterns**:
- Dependency Inversion Principle
- Ports and Adapters
- Onion Architecture (variation of hexagonal)

---

## Mentor's Note

Architecture might seem like over-engineering for a small project like Bibby. "Why not just put everything in one package?"

Here's the truth: for a 100-line project, architecture doesn't matter. For a 10,000-line project, it's the difference between maintainable and unmaintainable.

Bibby is your learning ground. Practice good architecture now, when the cost is low, so you know how to do it when the stakes are high.

The patterns we've covered (layered architecture, hexagonal architecture) are used by:
- Google
- Amazon
- Netflix
- Every serious enterprise application

Learning them makes you a professional software engineer, not just a coder.

In Section 8 (final section of Part 1!), we'll cover Bounded Contexts and Context Mapping - how to think about multiple domains and microservices.

You've learned SO much. One more section to complete Part 1!

---

**Section 7 Complete** | Next: Section 8 - Bounded Contexts & Context Mapping
