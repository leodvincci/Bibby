# Section 31: Implementation Roadmap

## Overview

You've learned **DDD**, **Design Patterns**, **Pragmatic Programming**, and **Testing**. Now it's time to apply everything to refactor Bibby from an anemic domain model into a robust, well-designed application.

This roadmap provides a **12-week implementation plan** for transforming Bibby, with specific tasks, time estimates, and checkpoints. The plan is designed to be incremental - you'll see improvements every week while maintaining a working application.

**Learning Objectives:**
- Create a realistic implementation timeline
- Prioritize refactoring efforts by value
- Apply concepts incrementally (not all at once)
- Build momentum with quick wins
- Maintain working software throughout refactoring

---

## The Refactoring Strategy

### Principles

1. **Tracer Bullets, Not Big Bang**
   - Make small, incremental changes
   - Keep tests passing at all times
   - Deploy to production frequently

2. **Business Value First**
   - Refactor code you're actively changing
   - Don't refactor for refactoring's sake
   - Improve code that causes most bugs

3. **Test Before You Refactor**
   - Write characterization tests first
   - Refactor with green tests
   - Add better tests after refactoring

4. **One Pattern at a Time**
   - Don't try to apply everything at once
   - Master one concept before moving to next
   - Reflect on what you learned

---

## Pre-Work: Assessment (Week 0)

Before starting the roadmap, assess current state.

### Assessment Checklist

**Domain Model Assessment:**
- [ ] List all entities (BookEntity, AuthorEntity, etc.)
- [ ] Identify anemic entities (only getters/setters, no behavior)
- [ ] List all primitives that should be value objects
- [ ] Identify aggregates and their boundaries

**Code Quality Assessment:**
- [ ] Run `grep -r "public void set" src/main` - count public setters
- [ ] Identify god classes (> 500 lines, > 10 public methods)
- [ ] Find duplicate code (similar validation logic in multiple places)
- [ ] Identify broken windows (TODOs, commented code, bugs)

**Testing Assessment:**
- [ ] Current test count: `mvn test | grep "Tests run"`
- [ ] Current test time: `time mvn test`
- [ ] Current coverage: `mvn jacoco:report` (if configured)
- [ ] Count @SpringBootTest vs unit tests

**Technical Debt Assessment:**
- [ ] List known bugs
- [ ] List missing features
- [ ] List performance issues
- [ ] List unclear/confusing code

**Deliverable:** Assessment document with findings

---

## Week 1-2: Foundation - Value Objects

**Goal:** Replace primitive obsession with value objects.

**Why First:** Value objects are:
- Lowest risk refactoring
- Immediate quality improvement
- Foundation for later refactorings
- Easy wins build momentum

### Week 1: ISBN, Title, Publisher

**Tasks:**

#### TASK 1.1: Create ISBN Value Object (2 hours)

```java
// src/main/java/com/penrose/bibby/domain/book/ISBN.java
public class ISBN {
    private final String value;

    public ISBN(String value) {
        if (value == null || !isValid(value)) {
            throw new InvalidIsbnException("Invalid ISBN: " + value);
        }
        this.value = normalize(value);
    }

    private boolean isValid(String isbn) {
        String digits = isbn.replaceAll("[^0-9]", "");
        return digits.length() == 13 && verifyChecksum(digits);
    }

    private boolean verifyChecksum(String digits) {
        // ISBN-13 checksum algorithm
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(digits.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == Character.getNumericValue(digits.charAt(12));
    }

    private String normalize(String isbn) {
        return isbn.replaceAll("[^0-9]", "");
    }

    public String getValue() {
        return value;
    }

    public String getFormatted() {
        // 978-0-13-468599-1
        return String.format("%s-%s-%s-%s-%s",
            value.substring(0, 3),
            value.substring(3, 4),
            value.substring(4, 6),
            value.substring(6, 12),
            value.substring(12, 13)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISBN)) return false;
        ISBN isbn = (ISBN) o;
        return value.equals(isbn.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return getFormatted();
    }
}
```

**Tests:**

```java
class ISBNTest {
    @Test
    void validIsbn_createsSuccessfully() {
        ISBN isbn = new ISBN("978-0-13-468599-1");
        assertThat(isbn.getValue()).isEqualTo("9780134685991");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "123",              // Too short
        "12345678901234",   // Too long
        "978-0-13-ABC",     // Letters
        "978-0-13-468599-0" // Invalid checksum
    })
    void invalidIsbn_throwsException(String invalid) {
        assertThatThrownBy(() -> new ISBN(invalid))
            .isInstanceOf(InvalidIsbnException.class);
    }

    @Test
    void formatted_returnsHyphenatedFormat() {
        ISBN isbn = new ISBN("9780134685991");
        assertThat(isbn.getFormatted()).isEqualTo("978-0-13-468599-1");
    }
}
```

**Migration Strategy:**

```java
// Step 1: Add ISBN field to BookEntity (keep String isbn too)
public class BookEntity {
    private String isbn;  // Keep for now
    private ISBN isbnValue;  // New field

    // Temporary: sync both fields
    public void setIsbn(String isbn) {
        this.isbn = isbn;
        this.isbnValue = new ISBN(isbn);
    }

    public ISBN getIsbnValue() {
        if (isbnValue == null && isbn != null) {
            isbnValue = new ISBN(isbn);
        }
        return isbnValue;
    }
}

// Step 2: Update all code to use getIsbnValue()
// Step 3: Remove old String isbn field (next week)
```

**Checkpoint:**
- [ ] ISBN value object created and tested
- [ ] All code uses `getIsbnValue()` instead of `getIsbn()`
- [ ] Tests pass
- [ ] Commit: "Add ISBN value object"

---

#### TASK 1.2: Create Title and Publisher Value Objects (2 hours)

Similar process for Title and Publisher:

```java
public class Title {
    private final String value;

    public Title(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (value.length() > 500) {
            throw new IllegalArgumentException("Title too long");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    // equals, hashCode, toString
}

public class Publisher {
    private final String name;

    public Publisher(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Publisher name cannot be empty");
        }
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    // equals, hashCode, toString
}
```

**Checkpoint:**
- [ ] Title and Publisher value objects created
- [ ] Tests written for both
- [ ] BookEntity updated to use value objects
- [ ] Tests pass
- [ ] Commit: "Add Title and Publisher value objects"

---

### Week 2: Author Value Object and Aggregate Cleanup

#### TASK 2.1: Convert Author to Value Object (3 hours)

Currently `AuthorEntity` is an entity, but should it be?

**Analysis:**
- Do authors have identity? (Probably not in Bibby's context)
- Are authors shared across books? (Not currently)
- Do authors change independently? (No)

**Conclusion:** Author should be a value object embedded in Book.

```java
// src/main/java/com/penrose/bibby/domain/book/Author.java
public class Author {
    private final String firstName;
    private final String lastName;

    public Author(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name required");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name required");
        }
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getLastNameFirst() {
        return lastName + ", " + firstName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Author)) return false;
        Author author = (Author) o;
        return firstName.equals(author.firstName) &&
               lastName.equals(author.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName);
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
```

**Migration:**

```java
// JPA: Use @ElementCollection for value objects
@Entity
public class BookEntity {
    @ElementCollection
    @CollectionTable(name = "book_authors")
    private List<Author> authors = new ArrayList<>();

    public List<Author> getAuthors() {
        return Collections.unmodifiableList(authors);
    }

    public void addAuthor(Author author) {
        if (author == null) {
            throw new IllegalArgumentException("Author cannot be null");
        }
        this.authors.add(author);
    }
}
```

**Checkpoint:**
- [ ] Author converted to value object
- [ ] Database migration script created
- [ ] Tests updated
- [ ] All tests pass
- [ ] Commit: "Convert Author to value object"

---

## Week 3-4: Rich Domain Model - Book Aggregate

**Goal:** Move business logic from services into Book domain object.

### Week 3: Book Behavior - Check Out/Return

#### TASK 3.1: Add Business Logic to Book (4 hours)

**Current Problem:**

```java
// ❌ Anemic - business logic in service
public class BookService {
    public void checkOutBook(BookEntity book) {
        if (!book.getBookStatus().equals("AVAILABLE")) {
            // Silent failure!
        }
        book.setBookStatus("CHECKED_OUT");
    }
}
```

**Refactored:**

```java
// ✅ Rich domain model
public class Book {
    private BookId id;
    private ISBN isbn;
    private Title title;
    private Publisher publisher;
    private List<Author> authors;
    private AvailabilityStatus status;

    public void checkOut() {
        // Precondition: Defensive Programming
        if (!isAvailable()) {
            throw new BookNotAvailableException(
                "Cannot check out book: " + title.getValue() +
                " (current status: " + status + ")"
            );
        }

        // Business rule
        this.status = AvailabilityStatus.CHECKED_OUT;
        this.updatedAt = LocalDateTime.now();

        // Postcondition: Design by Contract
        assert this.status == AvailabilityStatus.CHECKED_OUT;
    }

    public void returnBook() {
        if (this.status != AvailabilityStatus.CHECKED_OUT) {
            throw new IllegalStateException(
                "Cannot return book that is not checked out"
            );
        }

        this.status = AvailabilityStatus.AVAILABLE;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAvailable() {
        return this.status == AvailabilityStatus.AVAILABLE;
    }

    public boolean isCheckedOut() {
        return this.status == AvailabilityStatus.CHECKED_OUT;
    }
}
```

**Tests:**

```java
class BookTest {
    @Nested
    @DisplayName("checkOut()")
    class CheckOutTest {
        @Test
        void availableBook_canBeCheckedOut() {
            Book book = TestBooks.availableBook();

            book.checkOut();

            assertThat(book.isAvailable()).isFalse();
            assertThat(book.isCheckedOut()).isTrue();
        }

        @Test
        void checkedOutBook_cannotBeCheckedOutAgain() {
            Book book = TestBooks.checkedOutBook();

            assertThatThrownBy(() -> book.checkOut())
                .isInstanceOf(BookNotAvailableException.class)
                .hasMessageContaining("Cannot check out book");
        }
    }

    @Nested
    @DisplayName("returnBook()")
    class ReturnBookTest {
        @Test
        void checkedOutBook_canBeReturned() {
            Book book = TestBooks.checkedOutBook();

            book.returnBook();

            assertThat(book.isAvailable()).isTrue();
        }

        @Test
        void availableBook_cannotBeReturned() {
            Book book = TestBooks.availableBook();

            assertThatThrownBy(() -> book.returnBook())
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
```

**Checkpoint:**
- [ ] Book has business logic methods
- [ ] BookService delegates to Book domain object
- [ ] Tests verify behavior
- [ ] All tests pass
- [ ] Commit: "Add business logic to Book domain object"

---

### Week 4: Builder Pattern for Book Construction

#### TASK 4.1: Implement Builder Pattern (3 hours)

**Why:** Creating Book objects is complex and error-prone.

```java
public class Book {
    // Private constructor
    private Book(BookBuilder builder) {
        this.id = builder.id;
        this.isbn = Objects.requireNonNull(builder.isbn, "ISBN required");
        this.title = Objects.requireNonNull(builder.title, "Title required");
        this.publisher = Objects.requireNonNull(builder.publisher, "Publisher required");
        this.authors = new ArrayList<>(builder.authors);
        this.status = AvailabilityStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;

        validate();
    }

    private void validate() {
        if (authors.isEmpty()) {
            throw new ValidationException("Book must have at least one author");
        }
    }

    // Builder inner class
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

        public BookBuilder withAuthors(Author... authors) {
            this.authors.addAll(Arrays.asList(authors));
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

**Usage:**

```java
// Clean, fluent API
Book book = Book.builder()
    .withIsbn("978-0-13-468599-1")
    .withTitle("The Pragmatic Programmer")
    .withPublisher("Addison-Wesley")
    .withAuthor("David", "Thomas")
    .withAuthor("Andrew", "Hunt")
    .build();
```

**Test Data Builder:**

```java
// src/test/java/com/penrose/bibby/test/TestBooks.java
public class TestBooks {
    public static Book pragmaticProgrammer() {
        return Book.builder()
            .withIsbn("978-0-13-468599-1")
            .withTitle("The Pragmatic Programmer")
            .withPublisher("Addison-Wesley")
            .withAuthor("David", "Thomas")
            .withAuthor("Andrew", "Hunt")
            .build();
    }

    public static Book cleanCode() {
        return Book.builder()
            .withIsbn("978-0-13-235088-4")
            .withTitle("Clean Code")
            .withPublisher("Prentice Hall")
            .withAuthor("Robert", "Martin")
            .build();
    }

    public static BookBuilder aBook() {
        return Book.builder()
            .withIsbn("978-0-00-000000-0")
            .withTitle("Test Book")
            .withPublisher("Test Publisher")
            .withAuthor("Test", "Author");
    }

    public static Book availableBook() {
        return aBook().build();
    }

    public static Book checkedOutBook() {
        Book book = aBook().build();
        book.checkOut();
        return book;
    }
}
```

**Checkpoint:**
- [ ] Builder pattern implemented for Book
- [ ] Tests use builder instead of constructors
- [ ] TestBooks utility class created
- [ ] All tests pass
- [ ] Commit: "Add Builder pattern for Book"

---

## Week 5-6: Application Services & Command Pattern

**Goal:** Separate orchestration (service) from business logic (domain).

### Week 5: Split God Service into Focused Services

#### TASK 5.1: Analyze BookService (1 hour)

**Current State:**

```bash
# Count public methods in BookService
grep "public" src/main/java/.../BookService.java | wc -l
```

**Identify Responsibilities:**
- Query operations (findByIsbn, findAll)
- Command operations (add, checkOut, return, delete)
- Mixed concerns

**Target Architecture:**

```
BookService (current god class)
    ↓
Split into:
    - AddBookService (command)
    - CheckOutBookService (command)
    - ReturnBookService (command)
    - BookQueryService (query)
```

---

#### TASK 5.2: Implement CheckOutBookService (4 hours)

```java
// Command object
public record CheckOutCommand(BookId bookId, String patronEmail) {
    public CheckOutCommand {
        Objects.requireNonNull(bookId, "bookId required");
        if (patronEmail == null || patronEmail.isBlank()) {
            throw new IllegalArgumentException("patronEmail required");
        }
    }
}

// Result object
public sealed interface CheckOutResult {
    record Success(LoanId loanId, LocalDate dueDate) implements CheckOutResult {}
    record Failure(String reason) implements CheckOutResult {}

    static CheckOutResult success(LoanId loanId, LocalDate dueDate) {
        return new Success(loanId, dueDate);
    }

    static CheckOutResult failure(String reason) {
        return new Failure(reason);
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }
}

// Service
@Service
@Transactional
public class CheckOutBookService {
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public CheckOutBookService(
        BookRepository bookRepository,
        LoanRepository loanRepository
    ) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    public CheckOutResult checkOut(CheckOutCommand command) {
        // Fetch book
        Book book = bookRepository.findById(command.bookId())
            .orElseThrow(() -> new BookNotFoundException(command.bookId()));

        // Business validation
        if (!book.isAvailable()) {
            return CheckOutResult.failure(
                "Book is not available: " + book.getTitle()
            );
        }

        // Execute domain logic
        book.checkOut();

        // Create loan
        Loan loan = new Loan(command.bookId(), command.patronEmail());

        // Persist
        bookRepository.save(book);
        loanRepository.save(loan);

        return CheckOutResult.success(loan.getId(), loan.getDueDate());
    }
}
```

**Tests:**

```java
@ExtendWith(MockitoExtension.class)
class CheckOutBookServiceTest {
    @Mock private BookRepository bookRepository;
    @Mock private LoanRepository loanRepository;
    @InjectMocks private CheckOutBookService service;

    @Test
    void checkOut_availableBook_succeeds() {
        Book book = TestBooks.availableBook();
        when(bookRepository.findById(book.getId()))
            .thenReturn(Optional.of(book));

        CheckOutCommand command = new CheckOutCommand(
            book.getId(),
            "user@example.com"
        );

        CheckOutResult result = service.checkOut(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(book.isAvailable()).isFalse();
        verify(bookRepository).save(book);
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    void checkOut_checkedOutBook_fails() {
        Book book = TestBooks.checkedOutBook();
        when(bookRepository.findById(book.getId()))
            .thenReturn(Optional.of(book));

        CheckOutCommand command = new CheckOutCommand(
            book.getId(),
            "user@example.com"
        );

        CheckOutResult result = service.checkOut(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result).isInstanceOf(CheckOutResult.Failure.class);

        CheckOutResult.Failure failure = (CheckOutResult.Failure) result;
        assertThat(failure.reason()).contains("not available");

        verify(bookRepository, never()).save(any());
        verify(loanRepository, never()).save(any());
    }
}
```

**Checkpoint:**
- [ ] CheckOutBookService created
- [ ] Command and Result objects implemented
- [ ] Tests written with mocks
- [ ] All tests pass
- [ ] Commit: "Extract CheckOutBookService with Command pattern"

---

### Week 6: Remaining Services

#### TASK 6.1: Create AddBookService, ReturnBookService, BookQueryService (4 hours)

Follow same pattern for remaining services.

**Checkpoint:**
- [ ] All services extracted from god class
- [ ] Each service has single responsibility
- [ ] All services tested
- [ ] Old BookService deleted
- [ ] Commit: "Complete service extraction"

---

## Week 7-8: Testing Infrastructure

**Goal:** Build comprehensive test suite.

### Week 7: Unit Test Coverage

#### TASK 7.1: Achieve 80% Unit Test Coverage (6 hours)

**Focus on domain objects:**
- Book
- ISBN, Title, Publisher, Author
- Loan
- All value objects

**Test structure:**

```java
class BookTest {
    @Nested
    @DisplayName("Construction")
    class ConstructionTest {
        @Test
        void validBook_createsSuccessfully() { /* ... */ }

        @Test
        void bookWithNoAuthors_throwsException() { /* ... */ }
    }

    @Nested
    @DisplayName("checkOut()")
    class CheckOutTest {
        // Multiple tests...
    }

    @Nested
    @DisplayName("returnBook()")
    class ReturnBookTest {
        // Multiple tests...
    }
}
```

**Checkpoint:**
- [ ] All domain objects have unit tests
- [ ] Coverage > 80%
- [ ] Tests run in < 10 seconds
- [ ] Commit: "Add comprehensive unit tests"

---

### Week 8: Integration Tests with Testcontainers

#### TASK 8.1: Set Up Testcontainers (2 hours)

```java
// src/test/java/integration/BaseIntegrationTest.java
@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("bibby_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    protected TestEntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        // Clean all tables
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        entityManager.clear();
    }
}
```

#### TASK 8.2: Write Integration Tests (4 hours)

```java
class BookRepositoryIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private BookRepository bookRepository;

    @Test
    void save_persistsBookWithAuthors() {
        Book book = TestBooks.pragmaticProgrammer();

        Book saved = bookRepository.save(book);
        entityManager.flush();
        entityManager.clear();

        Book found = bookRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getIsbn()).isEqualTo(book.getIsbn());
        assertThat(found.getAuthors()).hasSize(2);
    }

    @Test
    void findByIsbn_returnsBookWhenExists() {
        Book book = bookRepository.save(TestBooks.cleanCode());
        entityManager.flush();
        entityManager.clear();

        Optional<Book> found = bookRepository.findByIsbn(book.getIsbn());

        assertThat(found).isPresent();
    }
}
```

**Checkpoint:**
- [ ] Testcontainers configured
- [ ] Repository integration tests written
- [ ] Service integration tests written
- [ ] All tests pass
- [ ] Commit: "Add integration tests with Testcontainers"

---

## Week 9-10: Advanced Patterns

**Goal:** Apply Strategy and Observer patterns.

### Week 9: Strategy Pattern for Late Fees

#### TASK 9.1: Implement Late Fee Strategy (4 hours)

```java
// Domain service
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

// Strategy interface
public interface LateFeePolicy {
    Money calculate(long daysOverdue);
}

// Concrete strategies
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

// Configuration
@Configuration
public class LateFeeConfiguration {
    @Bean
    public LateFeePolicy lateFeePolicy(
        @Value("${bibby.late-fee.grace-period:3}") int gracePeriod
    ) {
        return gracePeriod > 0
            ? new GracePeriodLateFeePolicy(gracePeriod)
            : new StandardLateFeePolicy();
    }

    @Bean
    public LateFeeCalculator lateFeeCalculator(LateFeePolicy policy) {
        return new LateFeeCalculator(policy);
    }
}
```

**Tests:**

```java
class LateFeeCalculatorTest {
    @Test
    void standardPolicy_chargesFromDayOne() {
        LateFeePolicy policy = new StandardLateFeePolicy();
        LateFeeCalculator calculator = new LateFeeCalculator(policy);

        Loan loan = TestLoans.overdueLoan(5);

        Money lateFee = calculator.calculateLateFee(loan);

        assertThat(lateFee).isEqualTo(Money.dollars(2.50));
    }

    @Test
    void gracePeriodPolicy_waivesFirstThreeDays() {
        LateFeePolicy policy = new GracePeriodLateFeePolicy(3);
        LateFeeCalculator calculator = new LateFeeCalculator(policy);

        Loan loanTwoDaysLate = TestLoans.overdueLoan(2);
        assertThat(calculator.calculateLateFee(loanTwoDaysLate))
            .isEqualTo(Money.ZERO);

        Loan loanFiveDaysLate = TestLoans.overdueLoan(5);
        assertThat(calculator.calculateLateFee(loanFiveDaysLate))
            .isEqualTo(Money.dollars(1.00));  // (5-3) * $0.50
    }
}
```

**Checkpoint:**
- [ ] Strategy pattern implemented
- [ ] Multiple policies created
- [ ] Configuration allows switching policies
- [ ] Tests verify all policies
- [ ] Commit: "Add Strategy pattern for late fees"

---

### Week 10: Observer Pattern for Notifications

#### TASK 10.1: Implement Observer Pattern (4 hours)

(See Section 30 for complete code)

**Checkpoint:**
- [ ] Observer pattern implemented
- [ ] Email and analytics observers created
- [ ] Events published on check-out/return
- [ ] Tests verify observer notifications
- [ ] Commit: "Add Observer pattern for notifications"

---

## Week 11: Performance & Optimization

**Goal:** Optimize slow operations.

### TASK 11.1: Profile and Optimize (4 hours)

**Profile:**
```bash
# Run tests with profiling
mvn test -Dmaven.surefire.debug

# Identify slow queries
# Enable SQL logging
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

**Common Optimizations:**
- Add database indexes
- Fix N+1 queries with JOIN FETCH
- Add caching for frequent queries
- Batch inserts

**Checkpoint:**
- [ ] Performance profiling complete
- [ ] Top 3 slow operations optimized
- [ ] Tests still pass
- [ ] Commit: "Optimize database queries"

---

## Week 12: Documentation & Final Polish

**Goal:** Document architecture and prepare for future development.

### TASK 12.1: Write Architecture Documentation (4 hours)

Create `/docs/architecture.md`:

```markdown
# Bibby Architecture

## Overview
Bibby follows Domain-Driven Design principles...

## Domain Model
### Aggregates
- Book (root): ISBN, Title, Publisher, Authors
- Loan (root): LoanId, BookId, PatronId, DueDate
- Patron (root): PatronId, Email, Name

### Value Objects
- ISBN: 13-digit with checksum validation
- Title: Non-empty string, max 500 chars
- Author: First name + last name
- Money: Amount + currency

## Design Patterns Used
### Builder Pattern
Used for constructing complex Book objects...

### Strategy Pattern
Late fee calculation uses pluggable strategies...

### Observer Pattern
Notifications sent when books checked out/returned...

## Testing Strategy
### Test Pyramid
- 70% Unit Tests (domain objects)
- 20% Service Tests (with mocks)
- 10% Integration Tests (Testcontainers)

## Future Improvements
- Add Patron aggregate
- Implement book reservations
- Add search functionality
```

### TASK 12.2: Update README (1 hour)

Update project README with:
- Architecture overview
- How to run tests
- How to add new features
- Contribution guidelines

**Checkpoint:**
- [ ] Architecture documented
- [ ] README updated
- [ ] Code reviewed
- [ ] Final commit: "Complete 12-week refactoring roadmap"

---

## Summary: 12-Week Progress

### Weeks 1-2: Value Objects
- ✅ ISBN, Title, Publisher, Author
- ✅ Primitive obsession eliminated
- ✅ Validation centralized

### Weeks 3-4: Rich Domain Model
- ✅ Business logic in domain objects
- ✅ Builder pattern for construction
- ✅ Defensive programming

### Weeks 5-6: Application Services
- ✅ God class split into focused services
- ✅ Command pattern for operations
- ✅ Result objects for errors

### Weeks 7-8: Testing
- ✅ 80% unit test coverage
- ✅ Integration tests with Testcontainers
- ✅ Fast test suite (< 30 seconds)

### Weeks 9-10: Advanced Patterns
- ✅ Strategy pattern for late fees
- ✅ Observer pattern for notifications
- ✅ Open/Closed principle

### Week 11: Performance
- ✅ Database optimization
- ✅ Query profiling
- ✅ Indexing strategy

### Week 12: Documentation
- ✅ Architecture documented
- ✅ README updated
- ✅ Team onboarding guide

---

## Metrics: Before and After

### Code Quality

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Lines of Code | 5,000 | 6,500 | +30% (but better organized) |
| Test Coverage | 20% | 85% | +325% |
| Test Count | 15 | 150 | +900% |
| Test Time | 30s | 25s | 17% faster (despite 10x tests) |
| Public Setters | 50+ | 5 | 90% reduction |
| God Classes | 3 | 0 | 100% reduction |
| Value Objects | 0 | 5 | Infinite improvement! |

### Domain Model Richness

| Metric | Before | After |
|--------|--------|-------|
| Entities with business logic | 0 (anemic) | 5 (rich) |
| Validation in domain | 10% | 90% |
| Primitive types | 20+ | 5 |
| Design patterns used | 1 | 5+ |

### Maintainability

| Metric | Before | After |
|--------|--------|-------|
| Cyclomatic complexity (avg) | 15 | 8 |
| Methods > 50 lines | 10 | 2 |
| Duplicate code blocks | 25 | 3 |
| TODOs / FIXMEs | 30 | 5 |

---

## Key Takeaways

### Success Factors

1. **Incremental Progress**
   - Small changes every week
   - Always working software
   - Regular commits

2. **Test-Driven Refactoring**
   - Write tests before refactoring
   - Keep tests green
   - Add better tests after refactoring

3. **Focus on Value**
   - Fix code you're changing
   - Prioritize bug-prone areas
   - Don't refactor for perfection

4. **Team Communication**
   - Share progress weekly
   - Document decisions
   - Pair program on difficult refactorings

### Common Pitfalls to Avoid

1. **Big Bang Refactoring**
   - ❌ Don't rewrite everything at once
   - ✅ Do incremental improvements

2. **Over-Engineering**
   - ❌ Don't add patterns you don't need
   - ✅ Do solve real problems

3. **Ignoring Tests**
   - ❌ Don't refactor without tests
   - ✅ Do test before and after

4. **Perfectionism**
   - ❌ Don't aim for perfect code
   - ✅ Do aim for better code

---

## Action Items

### TASK 31A: Create Your Roadmap (2 hours)

**Objective:** Customize this roadmap for your situation.

**Steps:**

1. Complete Week 0 assessment
2. Prioritize areas based on:
   - Business value
   - Bug frequency
   - Code change frequency
3. Create custom 12-week plan
4. Share with team for feedback

**Deliverable:** Customized roadmap document

---

### TASK 31B: Week 1 Execution (4 hours)

**Objective:** Execute Week 1 tasks.

**Deliver:**
- [ ] ISBN value object
- [ ] Tests for ISBN
- [ ] Migration strategy
- [ ] Commit and push

**Deliverable:** Week 1 complete, ready for Week 2

---

## Study Resources

### Books
- **"Working Effectively with Legacy Code"** - Michael Feathers
  - Chapter 8: How Do I Add a Feature?
  - Chapter 13: I Need to Make a Change, but I Don't Know What Tests to Write

- **"Refactoring"** - Martin Fowler
  - Chapter 2: Principles in Refactoring
  - Chapter 3: Bad Smells in Code

### Articles
- Martin Fowler: "Strangler Fig Application"
- Joshua Kerievsky: "Refactoring to Patterns"

---

## Summary

This 12-week roadmap transforms Bibby from an anemic domain model into a robust, well-designed application using:

- **DDD:** Value objects, rich domain model, aggregates
- **Design Patterns:** Builder, Strategy, Observer, Command
- **Pragmatic Principles:** DRY, Orthogonality, Defensive Programming
- **Testing:** Comprehensive unit, service, and integration tests

The key is **incremental progress** - small improvements every week that compound into significant transformation.

**Start with Week 1, and good luck!**

---

**Next:** Section 32 - Study Resources (curated learning path)

**Previous:** Section 30 - How It All Fits Together
