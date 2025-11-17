# SECTION 20: ORTHOGONALITY

## Introduction: Eliminate Effects Between Unrelated Things

**"Eliminate effects between unrelated things."**

In mathematics, two lines are orthogonal if they're perpendicular - they meet at right angles and don't interfere with each other. In software, orthogonality means **independence** - changing one component doesn't require changing others.

Orthogonality is one of the most powerful principles for creating maintainable systems. When components are orthogonal:
- Changes are localized to one module
- Testing becomes easier (test components in isolation)
- Reuse increases (self-contained components)
- Risk decreases (bugs don't propagate)

In this section, we'll analyze your **Bibby** codebase for orthogonality violations and show how to refactor toward independent, focused components.

**Learning Objectives**:
- Understand what orthogonality means in software design
- Identify coupling and dependencies in existing code
- Recognize god classes and god methods
- Apply the Single Responsibility Principle
- Refactor toward orthogonal architecture
- Test components in isolation

**Time Investment**: 3-4 hours

---

## What is Orthogonality?

### The Mathematical Analogy

In geometry, orthogonal vectors are **perpendicular** - they form a 90° angle:

```
    Y (vertical)
    |
    |
    |__________ X (horizontal)
```

X and Y axes are orthogonal:
- Moving along X doesn't affect your Y position
- Moving along Y doesn't affect your X position
- They're **independent**

### Software Orthogonality

In software, orthogonality means **two or more components don't depend on each other's internals**:

**Orthogonal** (Good):
```java
// Changing BookRepository doesn't affect BookFormatter
public class BookRepository {
    public Book findById(BookId id) { ... }
}

// Changing BookFormatter doesn't affect BookRepository
public class BookFormatter {
    public String format(Book book) { ... }
}
```

**Non-Orthogonal** (Bad):
```java
// BookService does EVERYTHING - all coupled together!
public class BookService {
    public void saveBook(Book book) {
        // Database logic
        repository.save(book);
        // Formatting logic
        String formatted = formatBook(book);
        // Logging logic
        logger.info(formatted);
        // Email logic
        emailService.send("Book saved: " + formatted);
    }
}
```

If you need to change how books are formatted, you must modify `BookService`. If you need to change logging, you modify `BookService`. Everything is tangled - **not orthogonal**.

---

## The Reality Check: Your Current Bibby Code

Let's analyze your actual `BookService.java` for orthogonality:

### BookService.java - A God Class

From `BookService.java:12-95`:

```java
@Service
public class BookService {
    final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    // 1. Book creation logic
    @Transactional
    public void createNewBook(BookRequestDTO bookRequestDTO) { ... }

    // 2. Book search logic
    public BookEntity findBookByTitle(String title) { ... }

    // 3. Checkout business logic
    public void checkOutBook(BookEntity bookEntity) { ... }

    // 4. Book update logic
    public void updateBook(BookEntity bookEntity) { ... }

    // 5. Author query logic
    public List<AuthorEntity> findAuthorsByBookId(Long bookId) { ... }

    // 6. Check-in business logic
    public void checkInBook(String bookTitle) { ... }

    // 7. Shelf query logic
    public List<BookEntity> findBooksByShelf(Long id) { ... }

    // 8. Another shelf query
    public List<BookSummary> getBooksForShelf(Long shelfId) { ... }

    // 9. Book detail view logic
    public BookDetailView getBookDetails(Long bookId) { ... }

    // 10. Yet another book query
    public Optional<BookEntity> findBookById(Long bookId) { ... }
}
```

**Problem**: This class has **at least 10 different responsibilities**!

1. Creating books
2. Searching for books
3. Checking out books
4. Updating books
5. Finding authors
6. Checking in books
7. Finding books by shelf
8. Getting book summaries
9. Getting book details
10. Finding books by ID

**These are NOT orthogonal** - they're all tangled in one class.

### Consequences of Non-Orthogonality

**Scenario 1**: You want to change how book checkout works (add a 14-day limit).

**Non-Orthogonal Impact**:
- Must modify `BookService` (lines 56-62)
- But `BookService` also handles creation, search, updates, queries...
- Risk of breaking unrelated functionality
- Tests for `BookService` test EVERYTHING together
- Can't test checkout logic in isolation

**Scenario 2**: You want to add a new query (find books by genre).

**Non-Orthogonal Impact**:
- Add another method to already-large `BookService`
- Class grows to 15, 20, 30 methods...
- Becomes a "god class" - knows too much, does too much
- Every change requires understanding the entire class

---

## Benefits of Orthogonality

### 1. Increased Productivity

**Changes are localized**.

**Non-Orthogonal**:
```java
// Change checkout logic
// Must understand entire BookService (500 lines)
// Might accidentally break book creation or search
```

**Orthogonal**:
```java
// Change checkout logic
// Only need to understand CheckOutBookService (50 lines)
// Other services unaffected
```

**Development time**: Orthogonal code is faster to modify because you only need to understand one focused component.

### 2. Reduced Risk

**Isolated failures**.

**Non-Orthogonal**:
```java
// Bug in checkout logic
// Might affect book creation (same class, shared state)
// Entire BookService at risk
```

**Orthogonal**:
```java
// Bug in CheckOutBookService
// Doesn't affect AddBookService (different class, no shared state)
// Failure is contained
```

**Bug propagation**: Orthogonal systems contain failures; non-orthogonal systems spread them.

### 3. Better Testing

**Test components independently**.

**Non-Orthogonal**:
```java
@Test
void shouldCheckOutBook() {
    // Need to set up:
    // - BookRepository (for finding book)
    // - AuthorRepository (even though not used for checkout!)
    // - Full database state
    BookService service = new BookService(bookRepo, authorRepo);
    // Test intertwined with other responsibilities
}
```

**Orthogonal**:
```java
@Test
void shouldCheckOutBook() {
    // Only need Book domain object - no dependencies!
    Book book = aBook().available().build();

    book.checkOut();  // Test in isolation

    assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
}
```

**Test complexity**: Orthogonal code has simple, focused tests.

### 4. Easier Reuse

**Self-contained components**.

**Non-Orthogonal**:
```java
// Want to reuse checkout logic in a CLI tool?
// Can't - it's tied to BookService which needs database, Spring, etc.
```

**Orthogonal**:
```java
// Want to reuse checkout logic?
// Easy - Book.checkOut() is self-contained
Book book = loadFromSomewhere();
book.checkOut();  // Works in CLI, web, batch job, anywhere!
```

---

## Identifying Non-Orthogonal Code in Bibby

### Red Flag #1: God Classes

**Definition**: A class that knows too much or does too much.

**In Bibby**: `BookService` with 10 public methods doing unrelated things.

**Test**: Ask "What is this class's single responsibility?"

If you can't answer in one sentence, it's a god class.

- ❌ "BookService handles book creation, search, checkout, updates, and queries"
- ✅ "AddBookService creates new books"

### Red Flag #2: Many Dependencies

**From BookService.java:14-15**:
```java
final BookRepository bookRepository;
private final AuthorRepository authorRepository;
```

**Question**: Why does checkout logic need `AuthorRepository`?

**Answer**: It doesn't! But since `createNewBook` needs it, and they're in the same class, checkout is coupled to author management.

**Test**: Count constructor parameters. More than 3-4? Probably not orthogonal.

### Red Flag #3: Multiple Reasons to Change

**BookService changes when**:
1. Book creation rules change
2. Search algorithm changes
3. Checkout rules change
4. Database schema changes
5. Query performance requirements change

**Five reasons to change = not orthogonal.**

**Orthogonal equivalent**: Each service has ONE reason to change.

- `AddBookService` changes when: creation rules change
- `CheckOutBookService` changes when: checkout rules change
- `BookQueryService` changes when: query needs change

### Red Flag #4: Can't Test in Isolation

**Current checkout test would look like**:
```java
@Test
void shouldCheckOutBook() {
    // Need database
    @Autowired BookRepository bookRepository;
    @Autowired AuthorRepository authorRepository;  // Why?!

    // Need Spring context
    @SpringBootTest

    // Need test data in database
    BookEntity book = new BookEntity();
    book.setTitle("Test");
    // ... 10 more lines of setup
    bookRepository.save(book);

    // Finally, test
    BookService service = new BookService(bookRepository, authorRepository);
    service.checkOutBook(book);

    // Assert
    assertThat(book.getBookStatus()).isEqualTo("CHECKED_OUT");
}
```

**Problem**: Testing checkout requires database, Spring, authors (unrelated), and complex setup.

**Orthogonal test**:
```java
@Test
void shouldCheckOutBook() {
    Book book = aBook().available().build();

    book.checkOut();

    assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
}
```

**One line of setup, no dependencies!** That's orthogonality.

---

## Refactoring Toward Orthogonality

### Step 1: Identify Responsibilities

Look at `BookService` and list responsibilities:

1. **Commands** (change state):
   - Create book
   - Check out book
   - Check in book
   - Update book

2. **Queries** (read state):
   - Find book by title
   - Find book by ID
   - Find books by shelf
   - Get book details
   - Get book summaries
   - Find authors by book

**First separation**: Commands vs. Queries (CQRS pattern)

### Step 2: Create Focused Services

**Extract AddBookService**:
```java
package com.penrose.bibby.application.book;

import com.penrose.bibby.library.book.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for adding new books to the library.
 * Single Responsibility: Handle the "add book" use case.
 */
@Service
public class AddBookService {

    private final BookFactory bookFactory;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public AddBookService(
        BookFactory bookFactory,
        BookRepository bookRepository,
        AuthorRepository authorRepository
    ) {
        this.bookFactory = bookFactory;
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    /**
     * Add a new book to the library.
     * Creates authors if they don't exist.
     *
     * @param command The book details to add
     * @return The ID of the newly created book
     */
    @Transactional
    public BookId addBook(AddBookCommand command) {
        // Find or create authors
        Set<AuthorId> authorIds = findOrCreateAuthors(command.getAuthorNames());

        // Create book via factory
        BookCreationData data = buildCreationData(command, authorIds);
        Book book = bookFactory.createBook(data);

        // Persist
        bookRepository.save(book);

        return book.getId();
    }

    private Set<AuthorId> findOrCreateAuthors(Set<AuthorName> names) {
        return names.stream()
            .map(this::findOrCreateAuthor)
            .collect(Collectors.toSet());
    }

    private AuthorId findOrCreateAuthor(AuthorName name) {
        return authorRepository.findByName(name)
            .map(Author::getId)
            .orElseGet(() -> createAuthor(name));
    }

    private AuthorId createAuthor(AuthorName name) {
        Author author = Author.create(AuthorId.generate(), name);
        authorRepository.save(author);
        return author.getId();
    }

    private BookCreationData buildCreationData(AddBookCommand command, Set<AuthorId> authorIds) {
        return BookCreationData.builder()
            .title(command.getTitle())
            .isbn(command.getIsbn())
            .authorIds(authorIds)
            .publisher(command.getPublisher())
            .year(command.getYear())
            .edition(command.getEdition())
            .build();
    }
}
```

**Extract CheckOutBookService**:
```java
package com.penrose.bibby.application.book;

import com.penrose.bibby.library.book.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for checking out books.
 * Single Responsibility: Handle the "check out book" use case.
 */
@Service
public class CheckOutBookService {

    private final BookRepository bookRepository;
    private final DomainEventPublisher eventPublisher;

    public CheckOutBookService(
        BookRepository bookRepository,
        DomainEventPublisher eventPublisher
    ) {
        this.bookRepository = bookRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Check out a book from the library.
     *
     * @param bookId The ID of the book to check out
     * @throws BookNotFoundException if book doesn't exist
     * @throws BookNotAvailableException if book is not available
     */
    @Transactional
    public void checkOut(BookId bookId) {
        // Load book
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        // Business logic in domain model!
        book.checkOut();

        // Persist
        bookRepository.save(book);

        // Publish event
        eventPublisher.publish(new BookCheckedOutEvent(bookId, LocalDateTime.now()));
    }
}
```

**Extract BookQueryService**:
```java
package com.penrose.bibby.application.book;

import com.penrose.bibby.library.book.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Query service for retrieving book information.
 * Single Responsibility: Handle book queries/reads.
 */
@Service
@Transactional(readOnly = true)  // Read-only optimization
public class BookQueryService {

    private final BookRepository bookRepository;

    public BookQueryService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Find a book by its title (case-insensitive).
     *
     * @return Optional book, empty if not found
     */
    public Optional<Book> findByTitle(String title) {
        Objects.requireNonNull(title, "Title cannot be null");
        return bookRepository.findByTitleIgnoreCase(title);
    }

    /**
     * Find a book by its unique ID.
     */
    public Optional<Book> findById(BookId id) {
        Objects.requireNonNull(id, "Book ID cannot be null");
        return bookRepository.findById(id);
    }

    /**
     * Find all books on a specific shelf.
     */
    public List<Book> findByShelf(ShelfId shelfId) {
        Objects.requireNonNull(shelfId, "Shelf ID cannot be null");
        return bookRepository.findByShelfId(shelfId);
    }

    /**
     * Get book summary for display (optimized projection).
     */
    public List<BookSummary> getSummariesForShelf(ShelfId shelfId) {
        Objects.requireNonNull(shelfId, "Shelf ID cannot be null");
        return bookRepository.findBookSummariesByShelfIdOrderByTitleAsc(shelfId);
    }

    /**
     * Get detailed book view with all related data.
     */
    public BookDetailView getDetails(BookId bookId) {
        Objects.requireNonNull(bookId, "Book ID cannot be null");
        return bookRepository.getBookDetailView(bookId);
    }
}
```

### Step 3: Move Business Logic to Domain

**BEFORE (BookService.java:56-62)**:
```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

**Business logic in service** = not orthogonal. Service layer is coupled to domain logic.

**AFTER (Book.java domain model)**:
```java
/**
 * Check out this book.
 * Business rules enforced here, in the domain model.
 */
public void checkOut() {
    // Precondition
    if (this.status != AvailabilityStatus.AVAILABLE) {
        throw new BookNotAvailableException(
            "Cannot check out book " + id + " - current status: " + status
        );
    }

    // State change
    this.status = AvailabilityStatus.CHECKED_OUT;
    this.checkoutCount++;
    this.lastCheckoutDate = LocalDateTime.now();

    // Postcondition (optional assertion)
    assert this.status == AvailabilityStatus.CHECKED_OUT;
}
```

**Now orthogonal**:
- Domain logic in `Book` (can test in isolation, no database)
- Service logic in `CheckOutBookService` (orchestration only)
- Persistence logic in `BookRepository` (data access only)

Each layer is **orthogonal** - changing one doesn't require changing others.

---

## The Orthogonality Test

### Question 1: Can You Test It in Isolation?

**Non-Orthogonal**:
```java
// Testing BookService.checkOutBook requires:
// - Spring context
// - Database
// - AuthorRepository (even though not used!)
// - Test data setup

@SpringBootTest
class BookServiceTest {
    @Autowired BookService service;
    @Autowired BookRepository bookRepo;
    // Can't test checkout without full integration test
}
```

**Orthogonal**:
```java
// Testing Book.checkOut requires:
// - Nothing! Pure domain logic

class BookTest {
    @Test
    void shouldCheckOut() {
        Book book = aBook().available().build();

        book.checkOut();

        assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
    }
    // Unit test - fast, simple, isolated
}
```

### Question 2: How Many Classes Need to Change?

**Scenario**: Add a 14-day checkout limit.

**Non-Orthogonal**:
```
Change BookService.checkOutBook()
Change BookService.checkInBook() (to calculate days)
Change BookEntity (add checkoutDate field)
Change database schema
Update all tests that use BookService
```
**5 changes, potentially breaking unrelated code.**

**Orthogonal**:
```
Change Book.checkOut() (add validation)
Change Book domain model (add checkoutDate)
```
**2 changes, localized to Book domain.**

### Question 3: Can You Reuse Components?

**Non-Orthogonal**:
```java
// Want to use checkout logic in CLI tool?
// Can't - BookService needs Spring, database, web context
```

**Orthogonal**:
```java
// Book.checkOut() is self-contained
Book book = loadFromAnywhere();
book.checkOut();  // Works in CLI, web, mobile, anywhere!
```

---

## Orthogonality Violations in Bibby

Let's identify specific violations:

### Violation #1: BookService Does Everything

**Location**: `BookService.java`
**Problem**: 10 responsibilities in one class
**Fix**: Split into `AddBookService`, `CheckOutBookService`, `BookQueryService`

### Violation #2: BookService Has checkInBook(String title)

**Location**: `BookService.java:73-77`
```java
public void checkInBook(String bookTitle) {
    BookEntity bookEntity = findBookByTitle(bookTitle);  // Query mixed with command!
    bookEntity.setBookStatus(BookStatus.AVAILABLE.toString());
    updateBook(bookEntity);
}
```

**Problems**:
1. Query logic (`findByTitle`) mixed with command logic (`checkIn`)
2. Takes `String title` instead of `BookId` - less type-safe
3. Business logic (`setBookStatus`) should be in domain, not service

**Orthogonal Refactoring**:
```java
// In CheckInBookService (separate from queries)
@Service
public class CheckInBookService {

    private final BookRepository bookRepository;

    @Transactional
    public void checkIn(BookId bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        book.returnToLibrary();  // Business logic in domain!

        bookRepository.save(book);
    }
}

// In Book domain model
public void returnToLibrary() {
    if (this.status != AvailabilityStatus.CHECKED_OUT) {
        throw new IllegalStateException(
            "Cannot return book that is not checked out"
        );
    }
    this.status = AvailabilityStatus.AVAILABLE;
    this.lastReturnDate = LocalDateTime.now();
}
```

**Now orthogonal**:
- Query logic in `BookQueryService`
- Check-in logic in `CheckInBookService`
- Business rules in `Book`
- Each can change independently

### Violation #3: ShelfService Duplicate Methods

**Location**: `ShelfService.java:20-30`
```java
public List<ShelfEntity> getAllShelves(Long bookCaseId){
    return shelfRepository.findByBookcaseId(bookCaseId);
}

public List<ShelfEntity> findByBookcaseId(Long bookcaseId) {
    return shelfRepository.findByBookcaseId(bookcaseId);
}
```

**Problem**: Duplication (DRY violation), but also **not orthogonal** - if you need to change how shelves are retrieved, you must change both methods (or remember to delete one).

**Fix**: Keep one method, delete the other.

### Violation #4: BookcaseService createNewBookCase Does Too Much

**Location**: `BookcaseService.java:25-41`
```java
public String createNewBookCase(String label, int capacity){
    // 1. Validation
    BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);
    if(bookcaseEntity !=null){
        log.error("Failed to save Record - Record already exist",existingRecordError);
        throw existingRecordError;
    }
    else{
        // 2. Creation
        bookcaseEntity = new BookcaseEntity(label,capacity);
        bookcaseRepository.save(bookcaseEntity);

        // 3. Shelf creation (different responsibility!)
        for(int i = 0; i < bookcaseEntity.getShelfCapacity(); i++){
            addShelf(bookcaseEntity,i,i);
        }

        // 4. Logging
        log.info("Created new bookcase: {}",bookcaseEntity.getBookcaseLabel());

        // 5. User message
        return "Created New Bookcase " + label + " with shelf capacity of " + capacity;
    }
}
```

**Problems**:
1. Validation mixed with creation
2. Bookcase creation coupled to shelf creation
3. Logging mixed with business logic
4. Returns user-facing string from service layer

**Orthogonal Refactoring**:
```java
// In domain model
public class Bookcase {
    private BookcaseId id;
    private BookcaseLabel label;
    private int shelfCapacity;
    private List<ShelfId> shelves = new ArrayList<>();

    private Bookcase(BookcaseLabel label, int shelfCapacity) {
        this.id = BookcaseId.generate();
        this.label = Objects.requireNonNull(label);
        this.shelfCapacity = validateCapacity(shelfCapacity);

        // Create shelves as part of bookcase creation
        initializeShelves();
    }

    private void initializeShelves() {
        for (int i = 0; i < shelfCapacity; i++) {
            this.shelves.add(ShelfId.generate());
        }
    }

    public static Bookcase create(BookcaseLabel label, int capacity) {
        return new Bookcase(label, capacity);
    }

    private int validateCapacity(int capacity) {
        if (capacity < 1 || capacity > 100) {
            throw new IllegalArgumentException(
                "Shelf capacity must be between 1 and 100"
            );
        }
        return capacity;
    }

    public List<ShelfId> getShelves() {
        return Collections.unmodifiableList(shelves);
    }
}

// In application service
@Service
public class CreateBookcaseService {

    private final BookcaseRepository bookcaseRepository;
    private final ShelfRepository shelfRepository;

    @Transactional
    public BookcaseId createBookcase(CreateBookcaseCommand command) {
        // Check uniqueness
        if (bookcaseRepository.existsByLabel(command.getLabel())) {
            throw new BookcaseLabelAlreadyExistsException(command.getLabel());
        }

        // Domain creates itself with shelves
        Bookcase bookcase = Bookcase.create(
            BookcaseLabel.of(command.getLabel()),
            command.getCapacity()
        );

        // Persist bookcase
        bookcaseRepository.save(bookcase);

        // Persist shelves (could be done by aggregate persistence)
        bookcase.getShelves().forEach(shelfId -> {
            Shelf shelf = Shelf.createFor(bookcase.getId(), shelfId);
            shelfRepository.save(shelf);
        });

        return bookcase.getId();
    }
}
```

**Now orthogonal**:
- Validation in value object (`BookcaseLabel`)
- Business rule (shelves created with bookcase) in domain model
- Persistence orchestration in service
- Logging via aspect (cross-cutting concern)
- UI messages in controller/CLI handler

---

## Achieving Orthogonality: Practical Techniques

### Technique 1: Single Responsibility Principle (SRP)

**Every class should have one reason to change.**

**Test**: "This class is responsible for ___________"

If you need "and" in the answer, it's not orthogonal.

- ❌ "BookService is responsible for creating AND searching AND checking out books"
- ✅ "AddBookService is responsible for adding books"
- ✅ "CheckOutBookService is responsible for checking out books"

### Technique 2: Dependency Inversion

**Depend on abstractions, not concretions.**

**Non-Orthogonal**:
```java
public class AddBookService {
    private final PostgreSQLBookRepository bookRepository;  // ❌ Concrete

    public void addBook(Book book) {
        bookRepository.saveToPostgreSQL(book);  // ❌ Coupled to PostgreSQL
    }
}
```

**Orthogonal**:
```java
public class AddBookService {
    private final BookRepository bookRepository;  // ✅ Interface

    public void addBook(Book book) {
        bookRepository.save(book);  // ✅ Implementation agnostic
    }
}

// Can swap implementations without changing service
public interface BookRepository {
    void save(Book book);
}

// Implementation 1
public class JpaBookRepository implements BookRepository { ... }

// Implementation 2
public class InMemoryBookRepository implements BookRepository { ... }
```

**Now orthogonal**: `AddBookService` doesn't depend on how books are persisted.

### Technique 3: Separate Commands from Queries (CQRS)

**Commands** (write operations):
- `AddBookService`
- `CheckOutBookService`
- `CheckInBookService`
- `UpdateBookService`
- `DeleteBookService`

**Queries** (read operations):
- `BookQueryService`
- `ShelfQueryService`
- `AuthorQueryService`

**Benefits**:
- Read models can be optimized differently than write models
- Queries don't accidentally modify state
- Clear architectural boundaries

### Technique 4: Move Business Logic to Domain

**Non-Orthogonal** (business logic in service):
```java
@Service
public class LateFeeCal culationService {
    public Money calculateFee(Long bookId, LocalDate returnDate) {
        Book book = repository.findById(bookId);
        LocalDate checkoutDate = book.getCheckoutDate();
        long daysLate = ChronoUnit.DAYS.between(checkoutDate, returnDate) - 14;

        if (daysLate <= 0) {
            return Money.ZERO;
        }

        BigDecimal fee = BigDecimal.valueOf(daysLate).multiply(BigDecimal.valueOf(0.50));
        return Money.of(fee);
    }
}
```

**Orthogonal** (business logic in domain):
```java
public class Loan {
    private static final int CHECKOUT_PERIOD_DAYS = 14;
    private static final Money DAILY_LATE_FEE = Money.of(0.50);

    private LocalDate checkoutDate;
    private LocalDate dueDate;

    public Money calculateLateFee(LocalDate today) {
        if (today.isBefore(dueDate) || today.isEqual(dueDate)) {
            return Money.ZERO;
        }

        long daysLate = ChronoUnit.DAYS.between(dueDate, today);
        return DAILY_LATE_FEE.multiply(daysLate);
    }
}

// Service just orchestrates
@Service
public class ReturnBookService {
    public Money returnBook(BookId bookId) {
        Loan loan = loanRepository.findActiveByBookId(bookId);
        Money lateFee = loan.calculateLateFee(LocalDate.now());
        loan.complete();
        return lateFee;
    }
}
```

**Now orthogonal**:
- Business rule (late fee calculation) in `Loan`
- Can test `Loan.calculateLateFee()` without database, Spring, etc.
- Can change late fee logic without touching service layer

---

## Action Items: Refactoring Bibby for Orthogonality

### Action Item 1: Split BookService (2-3 hours)

**Task**: Create focused services from god class

**Steps**:
1. Create `AddBookService` with just book creation logic
2. Create `CheckOutBookService` with just checkout logic
3. Create `CheckInBookService` with just check-in logic
4. Create `BookQueryService` with all query methods
5. Delete original `BookService`
6. Update callers to use new services

**Files**:
- New: `AddBookService.java`
- New: `CheckOutBookService.java`
- New: `CheckInBookService.java`
- New: `BookQueryService.java`
- Delete: `BookService.java` (old god class)

**Test**: Can you test each service independently? Can you describe each in one sentence?

### Action Item 2: Move Business Logic to Book Domain (2 hours)

**Task**: Extract checkout/check-in logic to `Book` class

**Steps**:
1. Add `checkOut()` method to `Book` with validation
2. Add `returnToLibrary()` method to `Book`
3. Update `CheckOutBookService` to call `book.checkOut()`
4. Update `CheckInBookService` to call `book.returnToLibrary()`
5. Write unit tests for `Book.checkOut()` and `Book.returnToLibrary()`

**Files**:
- Modify: `Book.java` (add business methods)
- Modify: `CheckOutBookService.java` (use domain methods)
- New: `BookTest.java` (unit tests for domain logic)

**Test**: Can you test `Book.checkOut()` without Spring or database?

### Action Item 3: Apply Dependency Inversion (1 hour)

**Task**: Depend on interfaces, not implementations

**Steps**:
1. Ensure all repository fields are interface types (`BookRepository`, not `JpaBookRepository`)
2. All service dependencies should be interfaces
3. Use `@Qualifier` if multiple implementations exist

**Files**:
- Review all service constructors
- Check all field types

**Test**: Can you swap a repository implementation without changing service code?

### Action Item 4: Measure Coupling (30 min)

**Task**: Count dependencies per class

**Template**:
```markdown
# Coupling Metrics

## BookService (Before Refactoring)
- Constructor parameters: 2
- Field dependencies: 2
- Public methods: 10
- Lines of code: 95
- Reasons to change: 5+

## AddBookService (After Refactoring)
- Constructor parameters: 3
- Field dependencies: 3
- Public methods: 1
- Lines of code: 40
- Reasons to change: 1

**Improvement**: 10 methods → 1 method, 5+ reasons to change → 1 reason
```

Create `COUPLING_METRICS.md` and track before/after.

### Action Item 5: Write Orthogonality Tests (1-2 hours)

**Task**: Prove components can be tested in isolation

**Example**:
```java
// Orthogonality test for Book domain
class BookOrthogonalityTest {

    @Test
    void bookCheckoutShouldNotRequireDatabase() {
        // No @SpringBootTest
        // No @Autowired
        // No database setup

        Book book = aBook().available().build();

        book.checkOut();

        assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
        // Proves Book is orthogonal to persistence layer
    }

    @Test
    void bookCheckoutShouldNotRequireServices() {
        // No service injection

        Book book = aBook().available().build();

        book.checkOut();

        // Proves Book is orthogonal to application layer
    }
}
```

Write similar tests for other domain classes.

---

## Key Takeaways

### 1. Orthogonality = Independence

Components that don't depend on each other's internals can evolve independently.

### 2. God Classes Kill Orthogonality

Your `BookService` with 10 responsibilities is the opposite of orthogonal. Split it.

### 3. Business Logic Belongs in Domain

Move `checkOut()` from service to `Book`. Now it's testable in isolation.

### 4. Separate Commands from Queries

CQRS: read operations in query services, write operations in command services.

### 5. Orthogonality Enables Testing

If you can't test a component without Spring, database, and 10 dependencies - it's not orthogonal.

### 6. Orthogonality Reduces Risk

Bugs in checkout don't affect book creation when they're in separate services.

---

## Study Resources

### Books
1. **"The Pragmatic Programmer"** - Chapter on Orthogonality
   - Helicopter example
   - How to achieve orthogonality

2. **"Clean Architecture" by Robert C. Martin**
   - Dependency inversion
   - Screaming architecture
   - Independent components

3. **"Domain-Driven Design" by Eric Evans**
   - Layered architecture
   - Domain isolation

### Articles
1. **"SOLID Principles"** - Single Responsibility
   - Uncle Bob Martin
   - SRP is foundation of orthogonality

2. **"CQRS Pattern"** - Martin Fowler
   - Separating reads from writes
   - Orthogonal query and command models

### Videos
1. **"The Clean Code Talks" by Miško Hevery** (Google)
   - Testability
   - Dependency injection
   - Orthogonal design

---

## Coming Next

**Section 21: Tracer Bullets & Prototypes** - Building incrementally

We'll explore:
- Tracer bullets: thin vertical slices
- Prototypes: throwaway exploration
- How to get feedback early
- Applying to Bibby features

---

**Section 20 Complete** | **Time Invested**: 3-4 hours | **Files to Create**: `COUPLING_METRICS.md`, orthogonality tests

Orthogonality is about **independence**. When you change one component, how many others must you change? The answer should be **zero**. That's orthogonality.
