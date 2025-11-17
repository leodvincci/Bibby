# SECTION 5: DOMAIN SERVICES

## When Logic Doesn't Belong in Entities

Welcome to Section 5. So far, we've learned that domain logic should live in entities - the "rich domain model" approach. But what happens when you have domain logic that doesn't naturally fit in any single entity?

That's where **Domain Services** come in.

---

## What is a Domain Service?

A **Domain Service** is a stateless operation that performs domain logic which doesn't naturally belong to any entity or value object.

### The Key Question

When you have domain logic to implement, ask yourself:

**"Which entity is responsible for this behavior?"**

- **Clear answer**: Put the logic in that entity (rich domain model)
- **Unclear answer** (involves multiple entities, or no specific entity): Use a domain service

### Domain Service Characteristics

1. **Stateless**: No internal state, only operates on parameters
2. **Domain Logic**: Contains business rules, not technical operations
3. **Named with Ubiquitous Language**: Service names come from the domain
4. **Coordinates Aggregates**: Often works across multiple aggregates
5. **Part of Domain Layer**: Lives in domain, uses domain objects

---

## Domain Service vs. Application Service vs. Infrastructure Service

There are three types of services in a well-designed application. Understanding the difference is crucial.

### Domain Service

**Purpose**: Domain logic that doesn't belong in an entity
**Layer**: Domain layer
**Examples**:
- `TransferBookBetweenLibraries` - domain operation
- `CalculateLateFees` - domain calculation
- `RecommendBooksByReadingHistory` - domain algorithm

```java
// domain/service/BookTransferService.java
public class BookTransferService {
    public void transferBook(Book book, Library from, Library to) {
        // Domain logic for transferring ownership
        from.removeBook(book);
        to.addBook(book);
        book.recordTransfer(from, to);
    }
}
```

### Application Service

**Purpose**: Orchestrate use cases, coordinate domain objects
**Layer**: Application layer
**Examples**:
- `CheckOutBookUseCase` - orchestrates checkout flow
- `AddBookToCollectionUseCase` - application workflow
- `GenerateLibraryReportUseCase` - application feature

```java
// application/service/CheckOutBookUseCase.java
public class CheckOutBookUseCase {
    private final BookRepository bookRepository;

    public void execute(BookId bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        book.checkOut();  // Domain logic in entity

        bookRepository.save(book);
    }
}
```

### Infrastructure Service

**Purpose**: Technical operations (email, file I/O, external APIs)
**Layer**: Infrastructure layer
**Examples**:
- `EmailService` - sends emails
- `FileStorageService` - saves files
- `ISBNLookupService` - calls external API

```java
// infrastructure/service/EmailService.java
public class EmailService {
    public void sendEmail(String to, String subject, String body) {
        // Technical implementation: SMTP, etc.
    }
}
```

### The Crucial Differences

| Aspect | Domain Service | Application Service | Infrastructure Service |
|--------|---------------|-------------------|----------------------|
| **Layer** | Domain | Application | Infrastructure |
| **Purpose** | Business logic | Orchestration | Technical operations |
| **State** | Stateless | Stateless | May have state |
| **Language** | Ubiquitous language | Use case language | Technical language |
| **Example** | `TransferBook()` | `CheckOutBookUseCase` | `SendEmail()` |

---

## When to Use a Domain Service

### Scenario 1: Logic Spans Multiple Aggregates

**Problem**: Checking out a book might involve updating both Book and Member aggregates (in a future library with members).

**Question**: Does this logic belong in Book or Member?
**Answer**: Neither - it involves both.

**Solution**: Domain service

```java
// domain/service/BookLoanService.java
public class BookLoanService {
    public Loan createLoan(Book book, Member member, LoanPeriod period) {
        // Validate book is available
        if (!book.isAvailable()) {
            throw new BookNotAvailableException();
        }

        // Validate member can borrow
        if (!member.canBorrowBooks()) {
            throw new MemberSuspendedException();
        }

        // Create the loan (domain operation)
        Loan loan = Loan.create(book.getId(), member.getId(), period);

        // Update aggregates
        book.markAsLoaned();
        member.recordLoan(loan.getId());

        return loan;
    }
}
```

### Scenario 2: Complex Domain Calculation

**Problem**: Calculating recommended books based on reading history.

**Question**: Does this belong in Book? In Collection?
**Answer**: It's a domain algorithm not owned by any entity.

**Solution**: Domain service

```java
// domain/service/BookRecommendationService.java
public class BookRecommendationService {
    public List<BookId> recommendBooks(ReadingHistory history, int limit) {
        // Complex domain algorithm
        Map<Genre, Integer> genrePreferences = analyzeGenrePreferences(history);
        Map<Author, Integer> authorPreferences = analyzeAuthorPreferences(history);

        // Domain logic for recommendations
        return generateRecommendations(genrePreferences, authorPreferences, limit);
    }

    private Map<Genre, Integer> analyzeGenrePreferences(ReadingHistory history) {
        // Domain logic...
    }
}
```

### Scenario 3: Domain-Specific Validation

**Problem**: Validating that an ISBN is not only valid format, but also not already in the collection.

**Question**: Does this belong in ISBN value object? In Book?
**Answer**: ISBN validates format, repository checks existence, but the policy belongs in domain.

**Solution**: Domain service

```java
// domain/service/UniqueIsbnValidator.java
public class UniqueIsbnValidator {
    private final BookRepository bookRepository;

    public void ensureUnique(ISBN isbn) {
        if (bookRepository.existsByIsbn(isbn)) {
            throw new DuplicateIsbnException("ISBN already exists: " + isbn);
        }
    }
}
```

### Scenario 4: Entity Doesn't Make Sense as Subject

**Problem**: Generating a catalog number for a new book based on complex rules.

**Question**: Who generates the catalog number? The book being created? No, that's weird.
**Answer**: This is a domain operation that happens *to* a book, not *by* a book.

**Solution**: Domain service

```java
// domain/service/CatalogNumberGenerator.java
public class CatalogNumberGenerator {
    public CatalogNumber generateFor(Book book) {
        // Domain logic: genre code + year + sequence
        String genreCode = book.getGenre().getCode();
        int year = book.getPublicationYear().getValue();
        int sequence = getNextSequence(genreCode, year);

        return new CatalogNumber(genreCode, year, sequence);
    }

    private int getNextSequence(String genreCode, int year) {
        // Domain logic...
    }
}
```

---

## Analyzing Your Current Services

Let's look at your `BookService.java` and see what's really happening.

### Current State: BookService.java

```java
@Service
public class BookService {
    final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

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
            bookEntity = new BookEntity();
            bookEntity.setTitle(title);
        }
        bookEntity.setAuthors(authorEntity);
        bookRepository.save(bookEntity);
    }

    public BookEntity findBookByTitle(String title) {
        Optional<BookEntity> bookEntity = Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
        List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
        for(BookEntity b : bookEntities){
            System.out.println(b.getTitle());  // ❌ Side effect
        }

        if(bookEntity.isEmpty()){
            return null;
        }
        return bookEntity.get();
    }

    public void checkOutBook(BookEntity bookEntity) {
        if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
            bookEntity.setBookStatus("CHECKED_OUT");
            bookRepository.save(bookEntity);
        }
    }

    public void updateBook(BookEntity bookEntity){
        bookRepository.save(bookEntity);
    }

    // ... more methods
}
```

### Analysis: What Type of Service is This?

Let's categorize each method:

**`createNewBook()`**:
- Takes DTO parameter (application layer concern)
- Orchestrates creating Author if needed
- Coordinates two repositories
- **Verdict**: This is an APPLICATION SERVICE

**`findBookByTitle()`**:
- Thin wrapper around repository
- Has side effect (printing)
- **Verdict**: This shouldn't exist (use repository directly)

**`checkOutBook()`**:
- Domain logic (checkout rules)
- Currently in service, but should be in Book entity
- **Verdict**: Logic belongs in domain model, not service

**`updateBook()`**:
- Thin wrapper around repository save
- **Verdict**: Unnecessary (use repository directly)

### The Problem: Anemic Domain Model

Your current `BookService` is doing everything:
- Application orchestration (createNewBook)
- Domain logic (checkOutBook)
- Repository pass-through (updateBook)
- Query operations (findBookByTitle)

This is the **anemic domain model** anti-pattern. The Book entity has no behavior - it's all in the service.

---

## Refactoring: Rich Domain Model + Services

Let's refactor this properly, using the three service types.

### 1. Move Domain Logic to Entities

```java
// domain/model/Book.java
public class Book {
    // ... fields ...

    // Domain logic belongs HERE
    public void checkOut() {
        if (!status.canCheckOut()) {
            throw new BookNotAvailableException(
                "Cannot check out book with status: " + status
            );
        }
        this.status = AvailabilityStatus.CHECKED_OUT;
        this.checkoutCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void returnToLibrary() {
        if (status == AvailabilityStatus.AVAILABLE) {
            throw new IllegalStateException("Book already available");
        }
        this.status = AvailabilityStatus.AVAILABLE;
        this.updatedAt = LocalDateTime.now();
    }

    public void placeOnShelf(ShelfId shelfId) {
        this.currentShelfId = Objects.requireNonNull(shelfId);
        this.updatedAt = LocalDateTime.now();
    }
}
```

### 2. Create Application Services for Use Cases

```java
// application/service/AddBookToCollectionService.java
@Service
public class AddBookToCollectionService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final UniqueIsbnValidator isbnValidator;

    @Transactional
    public BookId addBook(AddBookCommand command) {
        // Parse/create value objects
        Title title = new Title(command.getTitle());
        ISBN isbn = ISBN.fromString(command.getIsbn());
        Set<AuthorId> authorIds = resolveAuthors(command.getAuthors());

        // Domain validation (via domain service)
        isbnValidator.ensureUnique(isbn);

        // Create domain object
        Book book = new Book(BookId.generate(), title, isbn, authorIds);

        // Save via repository
        bookRepository.save(book);

        return book.getId();
    }

    private Set<AuthorId> resolveAuthors(List<AuthorDto> authorDtos) {
        return authorDtos.stream()
            .map(dto -> {
                AuthorName name = new AuthorName(dto.getFirstName(), dto.getLastName());

                // Find or create author
                return authorRepository.findByName(name)
                    .orElseGet(() -> {
                        Author author = new Author(AuthorId.generate(), name);
                        authorRepository.save(author);
                        return author.getId();
                    });
            })
            .collect(Collectors.toSet());
    }
}
```

### 3. Create Application Service for Checkout

```java
// application/service/CheckOutBookService.java
@Service
public class CheckOutBookService {
    private final BookRepository bookRepository;

    @Transactional
    public void checkOutBook(BookId bookId) {
        // Load aggregate
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        // Domain logic in entity
        book.checkOut();

        // Save aggregate
        bookRepository.save(book);
    }
}
```

### 4. Create Domain Service for Complex Operations

```java
// domain/service/BookRelocator.java
public class BookRelocator {

    /**
     * Domain service for relocating a book to a new shelf.
     * Handles complex validation and rules.
     */
    public void relocate(Book book, Shelf targetShelf, Bookcase targetBookcase) {
        // Domain validation
        if (!targetBookcase.contains(targetShelf)) {
            throw new IllegalArgumentException("Shelf not in bookcase");
        }

        if (targetShelf.isFull()) {
            throw new ShelfFullException("Target shelf is full");
        }

        // Domain operation
        book.placeOnShelf(targetShelf.getId());

        // Could publish domain event here
        // domainEventPublisher.publish(new BookRelocatedEvent(...));
    }
}
```

---

## Domain Services vs. Application Services: When to Use Each

This is subtle but important.

### Use Domain Service When:
- Logic is pure domain behavior (business rules)
- Involves multiple aggregates
- Named using ubiquitous language from domain
- Could exist even if the application (UI/API) didn't exist
- Example: "Transfer book between libraries" is a domain concept

### Use Application Service When:
- Orchestrating a use case
- Handling application-specific concerns (DTOs, transactions)
- Named using use case language
- Wouldn't exist without the application
- Example: "AddBookFromISBNScanCommand" is application-specific

### Example Comparison

**Domain Service**:
```java
// domain/service/LateFeeCalculator.java
public class LateFeeCalculator {
    public Money calculateLateFee(Loan loan, LocalDate returnDate) {
        // Pure domain logic
        int daysLate = loan.getDaysLate(returnDate);
        if (daysLate <= 0) return Money.zero();

        // Domain rules
        Money perDayFee = Money.of(0.50);
        Money maxFee = Money.of(25.00);

        Money totalFee = perDayFee.multiply(daysLate);
        return totalFee.min(maxFee);
    }
}
```

**Application Service**:
```java
// application/service/ReturnBookUseCase.java
@Service
public class ReturnBookUseCase {
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final LateFeeCalculator lateFeeCalculator;  // Uses domain service

    @Transactional
    public ReturnBookResult execute(ReturnBookCommand command) {
        // Load aggregates
        Book book = bookRepository.findById(command.getBookId())...;
        Loan loan = loanRepository.findActiveByBook(command.getBookId())...;

        // Calculate late fee (domain service)
        Money lateFee = lateFeeCalculator.calculateLateFee(loan, LocalDate.now());

        // Domain operations
        book.returnToLibrary();
        loan.complete(lateFee);

        // Save aggregates
        bookRepository.save(book);
        loanRepository.save(loan);

        // Return application result
        return new ReturnBookResult(book.getId(), lateFee);
    }
}
```

Notice: Application service orchestrates, domain service does pure calculations.

---

## Common Mistakes with Domain Services

### Mistake 1: Everything is a Domain Service

```java
// ❌ Bad - this should be in Book entity
public class BookService {
    public void checkOut(Book book) {
        book.setStatus(BookStatus.CHECKED_OUT);
    }
}
```

**Fix**: If logic naturally belongs to an entity, put it there.

### Mistake 2: Domain Services with State

```java
// ❌ Bad - domain services should be stateless
public class BookProcessor {
    private List<Book> processedBooks = new ArrayList<>();  // State!

    public void process(Book book) {
        processedBooks.add(book);
        // ...
    }
}
```

**Fix**: Domain services should be stateless, operating only on parameters.

### Mistake 3: Technical Operations in Domain Services

```java
// ❌ Bad - sending email is infrastructure, not domain
public class BookNotifier {
    public void notifyAvailable(Book book) {
        emailService.send(...);  // Infrastructure concern!
    }
}
```

**Fix**: Domain service focuses on domain logic. Use infrastructure service for email.

### Mistake 4: Mixing Application and Domain Services

```java
// ❌ Bad - DTOs and transactions in domain service
public class BookCreationService {
    @Transactional  // Application concern
    public void create(BookDTO dto) {  // Application DTO
        // ...
    }
}
```

**Fix**: Keep application concerns (DTOs, transactions) in application layer.

---

## Action Items

### 1. Categorize Your Current Services (2-3 hours)

Review `BookService.java`:
- List every method
- Categorize each: Domain logic? Application orchestration? Repository wrapper?
- Create `service-audit.md` with findings

### 2. Move Domain Logic to Entities (3-4 hours)

Refactor:
- `checkOutBook()` → `Book.checkOut()`
- `checkInBook()` → `Book.returnToLibrary()`
- Any other logic that belongs in Book

### 3. Create Application Services for Use Cases (4-5 hours)

Create:
- `AddBookToCollectionService` (application service)
- `CheckOutBookService` (application service)
- `ReturnBookService` (application service)

Use `@Service` and `@Transactional` here.

### 4. Identify Domain Services Needed (2 hours)

Think about your domain:
- What operations span multiple aggregates?
- What complex calculations exist?
- What domain policies need enforcement?

Create stubs for 2-3 domain services you might need.

### 5. Update Package Structure (1-2 hours)

Reorganize:
```
com.penrose.bibby/
├── domain/
│   ├── model/
│   ├── repository/
│   └── service/         ← Domain services here
├── application/
│   └── service/         ← Application services here
└── infrastructure/
    └── service/         ← Infrastructure services here
```

---

## Key Takeaways

### 1. Three Types of Services
- **Domain**: Business logic across entities
- **Application**: Use case orchestration
- **Infrastructure**: Technical operations

### 2. Prefer Rich Domain Model
- Put logic in entities when possible
- Use domain services only when logic doesn't fit in entities

### 3. Domain Services Are Stateless
- No instance variables
- Operate only on parameters
- Can be shared safely

### 4. Named in Ubiquitous Language
- Domain services use domain terms
- `BookRelocator`, not `BookMover`
- `LateFeeCalculator`, not `FeeService`

### 5. Domain Services Coordinate Aggregates
- Cross-aggregate operations
- Enforce domain policies
- Complex domain calculations

---

## Further Study

**Books**:
- "Domain-Driven Design" by Eric Evans - Chapter 5 (Services)
- "Implementing Domain-Driven Design" by Vaughn Vernon - Chapter 7 (Services)

**Articles**:
- Martin Fowler: "Service Layer"
- "Anemic Domain Model" (anti-pattern to avoid)

**Concepts**:
- CQRS (Command Query Responsibility Segregation)
- Application layer vs. domain layer
- Service-oriented architecture vs. domain services

---

## Mentor's Note

Domain services are often overused by developers coming from procedural or service-oriented backgrounds. The default should always be: **try to put logic in an entity first**.

Only create a domain service when you have a clear reason:
1. Logic spans multiple aggregates
2. Logic doesn't conceptually belong to any entity
3. Creating an entity to hold the logic feels forced

Your current `BookService` is doing way too much. It's a god class that handles everything. As we refactor, you'll see that:
- 60% of logic moves into entities (rich domain model)
- 30% becomes application services (use case orchestration)
- 10% stays as domain services (cross-aggregate coordination)

This transformation - from anemic to rich domain model - is one of the most powerful refactorings in software engineering.

In Section 6, we'll learn about Domain Events - a powerful pattern for decoupling aggregates and making implicit concepts explicit.

Keep up the great work!

---

**Section 5 Complete** | Next: Section 6 - Domain Events
