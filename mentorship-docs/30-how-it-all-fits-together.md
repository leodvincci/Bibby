# Section 30: How It All Fits Together

## Overview

You've learned a tremendous amount across the past 29 sections:
- **Domain-Driven Design** (Sections 1-8)
- **Gang of Four Design Patterns** (Sections 9-17)
- **The Pragmatic Programmer** (Sections 18-23)
- **Java Testing Mastery** (Sections 24-29)

Now it's time to **synthesize everything** and see how these concepts work together in practice. This section demonstrates how DDD, design patterns, pragmatic principles, and testing create a cohesive, maintainable system.

**Learning Objectives:**
- Understand how DDD provides the foundation for everything else
- See design patterns solving real DDD problems
- Apply pragmatic principles throughout the codebase
- Write tests that verify domain behavior
- Build a complete feature using all concepts together

---

## The Big Picture: Bibby's Architecture

Let's revisit Bibby's architecture with everything we've learned:

```
┌─────────────────────────────────────────────────────────────────┐
│                        Bibby CLI Application                     │
│                     (Spring Shell Commands)                      │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                     Application Services                         │
│  CheckOutBookService, ReturnBookService, AddBookService          │
│  (Command Pattern, Transaction Scripts)                          │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                      Domain Model (DDD)                          │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Book         │  │ Loan         │  │ Patron       │          │
│  │ (Aggregate)  │  │ (Aggregate)  │  │ (Aggregate)  │          │
│  │              │  │              │  │              │          │
│  │ - ISBN       │  │ - LoanId     │  │ - PatronId   │          │
│  │ - Title      │  │ - BookId     │  │ - Email      │          │
│  │ - Authors    │  │ - PatronId   │  │ - Name       │          │
│  │ - Status     │  │ - DueDate    │  │ - Holds      │          │
│  │              │  │ - Status     │  │              │          │
│  │ + checkOut() │  │ + return()   │  │ + placeHold()│          │
│  │ + return()   │  │ + renew()    │  │ + checkOut() │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                   │
│  Value Objects: ISBN, Title, Publisher, Author, Money            │
│  Domain Services: LateFeeCalculator, BookRecommendationEngine    │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                          │
│  Repositories (JPA), Email Service, External APIs                │
└─────────────────────────────────────────────────────────────────┘
```

**How Everything Connects:**

1. **DDD provides the foundation**
   - Rich domain model with behavior
   - Value Objects enforce constraints
   - Aggregates protect invariants
   - Ubiquitous Language

2. **Design Patterns solve specific problems**
   - Builder Pattern: Create complex domain objects
   - Strategy Pattern: Pluggable late fee calculation
   - Observer Pattern: Notify when book is returned
   - Repository Pattern: Persist aggregates

3. **Pragmatic Principles guide implementation**
   - DRY: Eliminate duplication in validation
   - Orthogonality: Decouple components
   - Defensive Programming: Validate preconditions
   - Tracer Bullets: Build incrementally

4. **Testing verifies everything works**
   - Unit tests: Domain object behavior
   - Service tests: Application logic
   - Integration tests: Database interactions
   - TDD: Drive design through tests

---

## Case Study: Building the "Book Check-Out" Feature

Let's build a complete feature from scratch using **all concepts together**.

### Requirements

**User Story:**
> As a library patron, I want to check out a book so that I can read it at home.

**Business Rules:**
1. Only available books can be checked out
2. A book can only be checked out to one patron at a time
3. Loan period is 14 days
4. Email confirmation sent after successful check-out
5. Book status changes from AVAILABLE to CHECKED_OUT
6. System records loan with due date

---

## Step 1: Domain Model (DDD Foundation)

### Value Objects

```java
// ISBN - Value Object with validation
public class ISBN {
    private final String value;

    public ISBN(String value) {
        if (value == null || !isValid(value)) {
            throw new InvalidIsbnException(
                "ISBN must be 13 digits: " + value
            );
        }
        this.value = normalize(value);
    }

    private boolean isValid(String isbn) {
        String digits = isbn.replaceAll("[^0-9]", "");
        return digits.length() == 13 && verifyChecksum(digits);
    }

    private String normalize(String isbn) {
        return isbn.replaceAll("[^0-9]", "");
    }

    // Equals, hashCode, toString...
}
```

**DDD Concepts Applied:**
- ✅ Value Object: Immutable, validated at construction
- ✅ Ubiquitous Language: "ISBN" not "String bookIdentifier"
- ✅ Self-validating: Impossible to create invalid ISBN

### Entities & Aggregates

```java
// Book - Aggregate Root
public class Book {
    private BookId id;
    private ISBN isbn;
    private Title title;
    private Publisher publisher;
    private List<Author> authors;
    private AvailabilityStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Private constructor - force use of Builder
    private Book(BookBuilder builder) {
        this.id = builder.id;
        this.isbn = Objects.requireNonNull(builder.isbn, "ISBN required");
        this.title = Objects.requireNonNull(builder.title, "Title required");
        this.publisher = Objects.requireNonNull(builder.publisher);
        this.authors = new ArrayList<>(builder.authors);
        this.status = AvailabilityStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        validate();
    }

    // Business logic - not just getters/setters!
    public void checkOut() {
        // Precondition (Defensive Programming)
        if (!isAvailable()) {
            throw new BookNotAvailableException(
                "Book is not available for check out: " + this.title.getValue()
            );
        }

        // State change
        this.status = AvailabilityStatus.CHECKED_OUT;
        this.updatedAt = LocalDateTime.now();

        // Postcondition (Design by Contract)
        assert !isAvailable() : "Book should not be available after check out";
    }

    public boolean isAvailable() {
        return this.status == AvailabilityStatus.AVAILABLE;
    }

    public void returnBook() {
        if (this.status != AvailabilityStatus.CHECKED_OUT) {
            throw new IllegalStateException("Book is not checked out");
        }

        this.status = AvailabilityStatus.AVAILABLE;
        this.updatedAt = LocalDateTime.now();
    }

    private void validate() {
        if (authors.isEmpty()) {
            throw new ValidationException("Book must have at least one author");
        }
    }

    // Builder Pattern (Gang of Four)
    public static class BookBuilder {
        private BookId id;
        private ISBN isbn;
        private Title title;
        private Publisher publisher;
        private List<Author> authors = new ArrayList<>();

        public BookBuilder withIsbn(String isbn) {
            this.isbn = new ISBN(isbn);
            return this;
        }

        public BookBuilder withTitle(String title) {
            this.title = new Title(title);
            return this;
        }

        public BookBuilder withPublisher(String publisher) {
            this.publisher = new Publisher(publisher);
            return this;
        }

        public BookBuilder withAuthor(String firstName, String lastName) {
            this.authors.add(new Author(firstName, lastName));
            return this;
        }

        public Book build() {
            return new Book(this);
        }
    }

    public static BookBuilder builder() {
        return new BookBuilder();
    }
}
```

**Concepts Applied:**
- ✅ **DDD:** Aggregate root with business logic
- ✅ **Design Pattern:** Builder for complex construction
- ✅ **Pragmatic:** Defensive programming (preconditions)
- ✅ **Pragmatic:** Design by Contract (postconditions)

```java
// Loan - Another Aggregate Root
public class Loan {
    private LoanId id;
    private BookId bookId;
    private PatronId patronId;
    private LocalDate checkedOutDate;
    private LocalDate dueDate;
    private LocalDate returnedDate;
    private LoanStatus status;

    public Loan(BookId bookId, PatronId patronId) {
        this.id = LoanId.generate();
        this.bookId = Objects.requireNonNull(bookId);
        this.patronId = Objects.requireNonNull(patronId);
        this.checkedOutDate = LocalDate.now();
        this.dueDate = checkedOutDate.plusDays(14);  // 14-day loan period
        this.status = LoanStatus.ACTIVE;
    }

    public void returnBook() {
        if (this.status != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Loan is not active");
        }

        this.returnedDate = LocalDate.now();
        this.status = LoanStatus.RETURNED;
    }

    public boolean isOverdue() {
        if (status != LoanStatus.ACTIVE) {
            return false;
        }
        return LocalDate.now().isAfter(dueDate);
    }

    public long getDaysOverdue() {
        if (!isOverdue()) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }

    // Getters...
}
```

**DDD Concepts:**
- ✅ Separate aggregate (Book and Loan have different lifecycles)
- ✅ Business logic in domain model (`isOverdue()`, `getDaysOverdue()`)
- ✅ Self-contained invariants

---

## Step 2: Domain Services

Some operations don't belong to a single entity - they're **Domain Services**.

```java
// LateFeeCalculator - Domain Service
public class LateFeeCalculator {
    private final LateFeePolicy policy;

    public LateFeeCalculator(LateFeePolicy policy) {
        this.policy = policy;
    }

    public Money calculateLateFee(Loan loan) {
        if (!loan.isOverdue()) {
            return Money.ZERO;
        }

        return policy.calculate(loan.getDaysOverdue());
    }
}

// Strategy Pattern for different late fee policies
public interface LateFeePolicy {
    Money calculate(long daysOverdue);
}

public class StandardLateFeePolicy implements LateFeePolicy {
    private static final Money DAILY_RATE = Money.dollars(0.50);

    @Override
    public Money calculate(long daysOverdue) {
        return DAILY_RATE.multiply(daysOverdue);
    }
}

public class GracePeriodLateFeePolicy implements LateFeePolicy {
    private static final int GRACE_DAYS = 3;
    private static final Money DAILY_RATE = Money.dollars(0.50);

    @Override
    public Money calculate(long daysOverdue) {
        long chargeableDays = Math.max(0, daysOverdue - GRACE_DAYS);
        return DAILY_RATE.multiply(chargeableDays);
    }
}
```

**Concepts Applied:**
- ✅ **DDD:** Domain Service (doesn't belong to Book or Loan)
- ✅ **Design Pattern:** Strategy Pattern (pluggable policies)
- ✅ **Pragmatic:** Open/Closed Principle (new policies without changing calculator)

---

## Step 3: Application Service (Orchestration)

Application services orchestrate domain objects and infrastructure.

```java
@Service
@Transactional
public class CheckOutBookService {
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final PatronRepository patronRepository;
    private final CheckOutNotificationService notificationService;

    public CheckOutBookService(
        BookRepository bookRepository,
        LoanRepository loanRepository,
        PatronRepository patronRepository,
        CheckOutNotificationService notificationService
    ) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.patronRepository = patronRepository;
        this.notificationService = notificationService;
    }

    public CheckOutResult checkOut(CheckOutCommand command) {
        // Fetch domain objects
        Book book = bookRepository.findById(command.bookId())
            .orElseThrow(() -> new BookNotFoundException(command.bookId()));

        Patron patron = patronRepository.findById(command.patronId())
            .orElseThrow(() -> new PatronNotFoundException(command.patronId()));

        // Business rule validation
        if (patron.hasOverdueBooks()) {
            return CheckOutResult.failure("Patron has overdue books");
        }

        if (patron.hasReachedCheckOutLimit()) {
            return CheckOutResult.failure("Patron has reached check out limit");
        }

        // Execute domain logic
        book.checkOut();  // Domain object behavior!

        // Create loan aggregate
        Loan loan = new Loan(book.getId(), patron.getId());

        // Persist changes
        bookRepository.save(book);
        loanRepository.save(loan);

        // Side effect - notification
        notificationService.sendCheckOutConfirmation(patron, book, loan);

        return CheckOutResult.success(loan.getId());
    }
}

// Command Pattern - encapsulates request
public record CheckOutCommand(BookId bookId, PatronId patronId) {
    public CheckOutCommand {
        Objects.requireNonNull(bookId, "bookId required");
        Objects.requireNonNull(patronId, "patronId required");
    }
}

// Result object - no exceptions for business failures
public sealed interface CheckOutResult {
    record Success(LoanId loanId) implements CheckOutResult {
        public boolean isSuccess() { return true; }
    }

    record Failure(String reason) implements CheckOutResult {
        public boolean isSuccess() { return false; }
    }

    static CheckOutResult success(LoanId loanId) {
        return new Success(loanId);
    }

    static CheckOutResult failure(String reason) {
        return new Failure(reason);
    }

    boolean isSuccess();
}
```

**Concepts Applied:**
- ✅ **DDD:** Application Service orchestrates domain objects
- ✅ **Design Pattern:** Command Pattern (CheckOutCommand)
- ✅ **Design Pattern:** Repository Pattern (data access)
- ✅ **Pragmatic:** Orthogonality (service doesn't handle notifications)
- ✅ **Pragmatic:** No exceptions for business failures (Result object)

---

## Step 4: Infrastructure (Repositories)

```java
// Repository Interface (Domain Layer)
public interface BookRepository {
    Optional<Book> findById(BookId id);
    Optional<Book> findByIsbn(ISBN isbn);
    List<Book> findByTitle(Title title);
    Book save(Book book);
    void delete(BookId id);
}

// JPA Implementation (Infrastructure Layer)
@Repository
public class JpaBookRepository implements BookRepository {
    private final SpringDataBookRepository springRepo;

    public JpaBookRepository(SpringDataBookRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<Book> findById(BookId id) {
        return springRepo.findById(id.getValue())
            .map(this::toDomain);
    }

    @Override
    public Book save(Book book) {
        BookEntity entity = toEntity(book);
        BookEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }

    // Mapping methods (DDD Anti-Corruption Layer)
    private Book toDomain(BookEntity entity) {
        return Book.builder()
            .withIsbn(entity.getIsbn())
            .withTitle(entity.getTitle())
            .withPublisher(entity.getPublisher())
            // ... map all fields
            .build();
    }

    private BookEntity toEntity(Book book) {
        BookEntity entity = new BookEntity();
        entity.setIsbn(book.getIsbn().getValue());
        entity.setTitle(book.getTitle().getValue());
        // ... map all fields
        return entity;
    }
}
```

**Concepts Applied:**
- ✅ **DDD:** Repository Pattern (domain interface)
- ✅ **DDD:** Anti-Corruption Layer (entity ↔ domain mapping)
- ✅ **Pragmatic:** Dependency Inversion (domain defines interface)

---

## Step 5: Notification (Observer Pattern)

```java
// Observer Pattern - notify interested parties
public interface CheckOutObserver {
    void onBookCheckedOut(BookCheckedOutEvent event);
}

// Event
public record BookCheckedOutEvent(
    BookId bookId,
    PatronId patronId,
    LoanId loanId,
    LocalDate dueDate
) {}

// Email Notification Observer
@Component
public class EmailNotificationObserver implements CheckOutObserver {
    private final EmailService emailService;
    private final PatronRepository patronRepository;
    private final BookRepository bookRepository;

    @Override
    public void onBookCheckedOut(BookCheckedOutEvent event) {
        Patron patron = patronRepository.findById(event.patronId()).orElseThrow();
        Book book = bookRepository.findById(event.bookId()).orElseThrow();

        Email email = Email.builder()
            .to(patron.getEmail())
            .subject("Book Checked Out: " + book.getTitle().getValue())
            .body(formatEmailBody(book, event.dueDate()))
            .build();

        emailService.send(email);
    }

    private String formatEmailBody(Book book, LocalDate dueDate) {
        return String.format("""
            You have successfully checked out:

            %s
            by %s

            Due date: %s

            Please return on time to avoid late fees.
            """,
            book.getTitle().getValue(),
            book.getAuthorsAsString(),
            dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
    }
}

// Analytics Observer
@Component
public class AnalyticsObserver implements CheckOutObserver {
    private final AnalyticsService analyticsService;

    @Override
    public void onBookCheckedOut(BookCheckedOutEvent event) {
        analyticsService.track("book_checked_out", Map.of(
            "book_id", event.bookId().getValue(),
            "patron_id", event.patronId().getValue(),
            "due_date", event.dueDate().toString()
        ));
    }
}

// Publisher
@Service
public class CheckOutNotificationService {
    private final List<CheckOutObserver> observers;

    public CheckOutNotificationService(List<CheckOutObserver> observers) {
        this.observers = observers;
    }

    public void sendCheckOutConfirmation(Patron patron, Book book, Loan loan) {
        BookCheckedOutEvent event = new BookCheckedOutEvent(
            book.getId(),
            patron.getId(),
            loan.getId(),
            loan.getDueDate()
        );

        // Notify all observers
        observers.forEach(observer ->
            observer.onBookCheckedOut(event)
        );
    }
}
```

**Concepts Applied:**
- ✅ **Design Pattern:** Observer Pattern (multiple listeners)
- ✅ **Pragmatic:** Open/Closed Principle (add observers without changing service)
- ✅ **Pragmatic:** Orthogonality (email logic separate from check-out logic)

---

## Step 6: Testing (TDD)

### Unit Test: Domain Object

```java
class BookTest {
    @Test
    @DisplayName("Available book can be checked out")
    void checkOut_availableBook_changesStatusToCheckedOut() {
        // Arrange
        Book book = Book.builder()
            .withIsbn("978-0-13-468599-1")
            .withTitle("The Pragmatic Programmer")
            .withPublisher("Addison-Wesley")
            .withAuthor("David", "Thomas")
            .build();

        assertThat(book.isAvailable()).isTrue();

        // Act
        book.checkOut();

        // Assert
        assertThat(book.isAvailable()).isFalse();
        assertThat(book.getStatus()).isEqualTo(AvailabilityStatus.CHECKED_OUT);
    }

    @Test
    @DisplayName("Checked out book cannot be checked out again")
    void checkOut_alreadyCheckedOutBook_throwsException() {
        // Arrange
        Book book = TestBooks.checkedOutBook();

        // Act & Assert
        assertThatThrownBy(() -> book.checkOut())
            .isInstanceOf(BookNotAvailableException.class)
            .hasMessageContaining("not available");
    }
}
```

**Testing Concepts:**
- ✅ Unit test: Fast, no dependencies
- ✅ Tests behavior, not implementation
- ✅ Clear AAA (Arrange-Act-Assert) structure

### Service Test: With Mocks

```java
@ExtendWith(MockitoExtension.class)
class CheckOutBookServiceTest {
    @Mock private BookRepository bookRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private PatronRepository patronRepository;
    @Mock private CheckOutNotificationService notificationService;

    @InjectMocks
    private CheckOutBookService checkOutService;

    @Test
    @DisplayName("Check out available book creates loan and sends notification")
    void checkOut_availableBook_createsLoanAndNotifies() {
        // Arrange
        Book book = TestBooks.availableBook();
        Patron patron = TestPatrons.regularPatron();

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(patronRepository.findById(patron.getId())).thenReturn(Optional.of(patron));

        CheckOutCommand command = new CheckOutCommand(book.getId(), patron.getId());

        // Act
        CheckOutResult result = checkOutService.checkOut(command);

        // Assert
        assertThat(result.isSuccess()).isTrue();

        // Verify domain behavior
        assertThat(book.isAvailable()).isFalse();

        // Verify persistence
        verify(bookRepository).save(book);
        verify(loanRepository).save(any(Loan.class));

        // Verify notification
        verify(notificationService).sendCheckOutConfirmation(
            eq(patron),
            eq(book),
            any(Loan.class)
        );
    }

    @Test
    @DisplayName("Check out by patron with overdue books fails")
    void checkOut_patronHasOverdueBooks_fails() {
        // Arrange
        Book book = TestBooks.availableBook();
        Patron patron = TestPatrons.patronWithOverdueBooks();

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(patronRepository.findById(patron.getId())).thenReturn(Optional.of(patron));

        CheckOutCommand command = new CheckOutCommand(book.getId(), patron.getId());

        // Act
        CheckOutResult result = checkOutService.checkOut(command);

        // Assert
        assertThat(result.isSuccess()).isFalse();
        assertThat(result).isInstanceOf(CheckOutResult.Failure.class);

        CheckOutResult.Failure failure = (CheckOutResult.Failure) result;
        assertThat(failure.reason()).contains("overdue books");

        // Verify book was NOT saved
        verify(bookRepository, never()).save(any());
        verify(loanRepository, never()).save(any());
    }
}
```

**Testing Concepts:**
- ✅ Mock infrastructure (repositories)
- ✅ Use real domain objects (Book, Patron)
- ✅ Test both success and failure scenarios
- ✅ Verify interactions with mocks

### Integration Test: Full Stack

```java
@SpringBootTest
@Testcontainers
class CheckOutIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private CheckOutBookService checkOutService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PatronRepository patronRepository;

    @Autowired
    private LoanRepository loanRepository;

    @BeforeEach
    void cleanDatabase() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        patronRepository.deleteAll();
    }

    @Test
    @DisplayName("Full check-out flow persists loan to database")
    void checkOut_fullIntegration_persistsLoan() {
        // Arrange
        Book book = TestBooks.pragmaticProgrammer();
        book = bookRepository.save(book);

        Patron patron = TestPatrons.regularPatron();
        patron = patronRepository.save(patron);

        CheckOutCommand command = new CheckOutCommand(book.getId(), patron.getId());

        // Act
        CheckOutResult result = checkOutService.checkOut(command);

        // Assert
        assertThat(result.isSuccess()).isTrue();

        // Verify database state
        Book savedBook = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(savedBook.isAvailable()).isFalse();

        List<Loan> loans = loanRepository.findByPatronId(patron.getId());
        assertThat(loans).hasSize(1);

        Loan loan = loans.get(0);
        assertThat(loan.getBookId()).isEqualTo(book.getId());
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getDueDate()).isEqualTo(LocalDate.now().plusDays(14));
    }
}
```

**Testing Concepts:**
- ✅ Integration test with real database (Testcontainers)
- ✅ Tests full stack (service → repository → database)
- ✅ Verifies persistence
- ✅ Clean database between tests

---

## The Complete Picture: All Concepts Working Together

Let's trace a single check-out request through the entire system:

### 1. User Input (CLI)

```java
@ShellComponent
public class LibraryCommands {
    private final CheckOutBookService checkOutService;

    @ShellMethod(key = "checkout", value = "Check out a book")
    public String checkOut(
        @ShellOption String isbn,
        @ShellOption String patronEmail
    ) {
        // Validate input
        ISBN bookIsbn = new ISBN(isbn);  // ✅ Value Object validation
        Email email = new Email(patronEmail);

        // Find entities
        Book book = bookRepository.findByIsbn(bookIsbn)
            .orElseThrow(() -> new BookNotFoundException(isbn));

        Patron patron = patronRepository.findByEmail(email)
            .orElseThrow(() -> new PatronNotFoundException(patronEmail));

        // Create command (Command Pattern)
        CheckOutCommand command = new CheckOutCommand(book.getId(), patron.getId());

        // Execute
        CheckOutResult result = checkOutService.checkOut(command);

        // Handle result
        return result.isSuccess()
            ? "✅ Book checked out successfully!"
            : "❌ Check out failed: " + ((CheckOutResult.Failure) result).reason();
    }
}
```

**Concepts:** Command Pattern, Value Objects, Error Handling

### 2. Application Service (Orchestration)

```java
public CheckOutResult checkOut(CheckOutCommand command) {
    // ✅ DDD: Fetch aggregates
    Book book = bookRepository.findById(command.bookId()).orElseThrow();
    Patron patron = patronRepository.findById(command.patronId()).orElseThrow();

    // ✅ Pragmatic: Defensive Programming - validate preconditions
    if (patron.hasOverdueBooks()) {
        return CheckOutResult.failure("Patron has overdue books");
    }

    // ✅ DDD: Execute domain logic
    book.checkOut();  // Business logic in domain object!

    // ✅ DDD: Create new aggregate
    Loan loan = new Loan(book.getId(), patron.getId());

    // ✅ DDD: Persist aggregates
    bookRepository.save(book);
    loanRepository.save(loan);

    // ✅ Design Pattern: Observer - notify interested parties
    notificationService.sendCheckOutConfirmation(patron, book, loan);

    return CheckOutResult.success(loan.getId());
}
```

**Concepts:** DDD Aggregates, Defensive Programming, Observer Pattern

### 3. Domain Object (Business Logic)

```java
public void checkOut() {
    // ✅ Pragmatic: Design by Contract - precondition
    if (!isAvailable()) {
        throw new BookNotAvailableException(
            "Book is not available for check out"
        );
    }

    // ✅ DDD: State change
    this.status = AvailabilityStatus.CHECKED_OUT;
    this.updatedAt = LocalDateTime.now();

    // ✅ Pragmatic: Design by Contract - postcondition
    assert !isAvailable() : "Book should not be available after check out";
}
```

**Concepts:** Design by Contract, Domain Logic, Invariant Protection

### 4. Repository (Persistence)

```java
@Override
public Book save(Book book) {
    // ✅ DDD: Anti-Corruption Layer - convert domain → entity
    BookEntity entity = toEntity(book);

    // ✅ Infrastructure: JPA persistence
    BookEntity saved = springRepo.save(entity);

    // ✅ DDD: Anti-Corruption Layer - convert entity → domain
    return toDomain(saved);
}
```

**Concepts:** Repository Pattern, Anti-Corruption Layer

### 5. Observer (Side Effects)

```java
public void sendCheckOutConfirmation(Patron patron, Book book, Loan loan) {
    // ✅ Design Pattern: Create event
    BookCheckedOutEvent event = new BookCheckedOutEvent(
        book.getId(), patron.getId(), loan.getId(), loan.getDueDate()
    );

    // ✅ Design Pattern: Notify all observers
    observers.forEach(observer -> observer.onBookCheckedOut(event));
}

// Email Observer
public void onBookCheckedOut(BookCheckedOutEvent event) {
    // ✅ Pragmatic: Orthogonality - separate concern
    emailService.send(buildEmail(event));
}
```

**Concepts:** Observer Pattern, Orthogonality, Open/Closed Principle

### 6. Tests (Verification)

```java
// ✅ Unit Test: Domain logic
@Test
void checkOut_availableBook_changesStatus() {
    Book book = TestBooks.availableBook();
    book.checkOut();
    assertThat(book.isAvailable()).isFalse();
}

// ✅ Service Test: Application logic
@Test
void checkOut_createsLoanAndNotifies() {
    CheckOutResult result = checkOutService.checkOut(command);
    verify(loanRepository).save(any(Loan.class));
    verify(notificationService).sendCheckOutConfirmation(any(), any(), any());
}

// ✅ Integration Test: Full stack
@Test
void checkOut_persistsToDatabase() {
    CheckOutResult result = checkOutService.checkOut(command);
    List<Loan> loans = loanRepository.findByPatronId(patronId);
    assertThat(loans).hasSize(1);
}
```

**Concepts:** Test Pyramid, Unit/Service/Integration Tests

---

## Summary: The Synergy

### DDD Provides the Foundation

- **Rich domain model** with real business logic
- **Value Objects** enforce constraints
- **Aggregates** protect invariants
- **Ubiquitous Language** improves communication

### Design Patterns Solve Specific Problems

- **Builder Pattern** constructs complex domain objects
- **Strategy Pattern** makes late fee calculation pluggable
- **Observer Pattern** decouples notifications
- **Command Pattern** encapsulates requests
- **Repository Pattern** abstracts persistence

### Pragmatic Principles Guide Implementation

- **DRY** eliminates duplication in validation
- **Orthogonality** decouples email from check-out logic
- **Defensive Programming** validates preconditions
- **Design by Contract** documents assumptions
- **Tracer Bullets** builds incrementally

### Testing Verifies Everything

- **Unit tests** verify domain object behavior
- **Service tests** verify application logic with mocks
- **Integration tests** verify database interactions
- **TDD** drives design through red-green-refactor

---

## The Complete Check-Out Feature: Code Structure

```
src/
├── main/
│   ├── java/com/penrose/bibby/
│   │   ├── domain/
│   │   │   ├── book/
│   │   │   │   ├── Book.java                 # Aggregate (DDD)
│   │   │   │   ├── ISBN.java                 # Value Object (DDD)
│   │   │   │   ├── Title.java                # Value Object (DDD)
│   │   │   │   ├── BookRepository.java       # Repository Interface (DDD)
│   │   │   │   └── BookNotAvailableException.java
│   │   │   ├── loan/
│   │   │   │   ├── Loan.java                 # Aggregate (DDD)
│   │   │   │   ├── LoanId.java               # Value Object (DDD)
│   │   │   │   ├── LoanRepository.java       # Repository Interface (DDD)
│   │   │   │   └── LateFeeCalculator.java    # Domain Service (DDD)
│   │   │   └── patron/
│   │   │       ├── Patron.java               # Aggregate (DDD)
│   │   │       └── PatronRepository.java     # Repository Interface (DDD)
│   │   ├── application/
│   │   │   ├── CheckOutBookService.java      # Application Service
│   │   │   ├── CheckOutCommand.java          # Command Pattern
│   │   │   ├── CheckOutResult.java           # Result Object
│   │   │   └── CheckOutNotificationService.java  # Observer Pattern
│   │   ├── infrastructure/
│   │   │   ├── persistence/
│   │   │   │   ├── JpaBookRepository.java    # Repository Implementation
│   │   │   │   ├── BookEntity.java           # JPA Entity
│   │   │   │   └── SpringDataBookRepository.java
│   │   │   └── notification/
│   │   │       ├── EmailNotificationObserver.java  # Observer Implementation
│   │   │       └── AnalyticsObserver.java    # Observer Implementation
│   │   └── cli/
│   │       └── LibraryCommands.java          # Spring Shell Commands
│   └── resources/
│       └── application.yml
└── test/
    └── java/com/penrose/bibby/
        ├── unit/
        │   ├── BookTest.java                 # Unit Test (fast!)
        │   ├── ISBNTest.java
        │   └── LoanTest.java
        ├── service/
        │   └── CheckOutBookServiceTest.java  # Service Test (mocks)
        └── integration/
            └── CheckOutIntegrationTest.java  # Integration Test (Testcontainers)
```

---

## Key Takeaways

1. **DDD is the foundation** - Everything starts with a rich domain model
2. **Patterns solve specific problems** - Don't use patterns for patterns' sake
3. **Pragmatic principles guide decisions** - DRY, Orthogonality, Defensive Programming
4. **Tests verify behavior** - Unit tests for domain, integration tests for infrastructure
5. **Everything works together** - No single concept works in isolation

The real power comes from **combining** these approaches:
- DDD gives you the structure
- Patterns give you proven solutions
- Pragmatic principles keep you grounded
- Tests give you confidence

---

## Action Items

### TASK 30A: Map Existing Bibby Code to Architecture (2 hours)

**Objective:** Understand how current Bibby code fits into this architecture.

**Steps:**

1. Identify aggregates in current code:
   - Book (aggregate root?)
   - Author (entity or value object?)
   - Shelf (aggregate root?)

2. Identify value objects:
   - What should be value objects? (ISBN, Title, etc.)
   - What's currently primitives that should be value objects?

3. Identify domain services:
   - What business logic is in services that should be in domain objects?
   - What truly needs to be a domain service?

4. Map to layers:
   - Domain layer (business logic)
   - Application layer (orchestration)
   - Infrastructure layer (persistence, email, etc.)

**Deliverable:** Architecture diagram of current Bibby code

---

### TASK 30B: Refactor One Feature Using All Concepts (4 hours)

**Objective:** Apply DDD + Patterns + Pragmatic + Testing to one feature.

**Choose one feature:**
- Book check-out
- Book return with late fees
- Book recommendations

**Refactor to include:**
1. Rich domain model (not anemic)
2. Value objects with validation
3. At least one design pattern (Builder, Strategy, Observer)
4. Defensive programming (preconditions)
5. Comprehensive tests (unit, service, integration)

**Deliverable:** Complete refactored feature with tests

---

### TASK 30C: Write Architecture Documentation (2 hours)

**Objective:** Document how Bibby uses DDD, patterns, and pragmatic principles.

**Create:** `docs/architecture.md`

**Include:**
1. Bounded contexts
2. Aggregate boundaries
3. Design patterns used and why
4. Testing strategy (pyramid)
5. Key architectural decisions

**Deliverable:** Architecture documentation for Bibby

---

## Study Resources

### Books (Synthesis)
- **"Domain-Driven Design"** - Evans (Chapters 1-3, 5-7)
- **"Implementing Domain-Driven Design"** - Vernon (Chapter 1, 4, 5)
- **"Design Patterns"** - Gang of Four (Introduction)
- **"The Pragmatic Programmer"** - Thomas & Hunt (All topics)

### Articles
- Martin Fowler: "Anemic Domain Model"
- Eric Evans: "Tackling Complexity in the Heart of Software"

---

## Summary

This section showed how all concepts work together:

1. **DDD** provides rich domain model (Book, Loan, Patron)
2. **Design Patterns** solve specific problems (Builder, Strategy, Observer)
3. **Pragmatic Principles** guide implementation (DRY, Orthogonality, Defensive)
4. **Testing** verifies everything (Unit, Service, Integration)

The check-out feature demonstrates:
- Value Objects enforcing constraints (ISBN)
- Aggregates protecting invariants (Book.checkOut())
- Domain Services for cross-aggregate logic (LateFeeCalculator)
- Design Patterns for flexibility (Strategy, Observer)
- Defensive Programming for robustness (preconditions)
- Comprehensive testing at all levels

**The key insight:** These concepts aren't separate - they work together to create maintainable, robust, testable software.

---

**Next:** Section 31 - Implementation Roadmap (week-by-week plan)

**Previous:** Section 29 - Testing Anti-Patterns
