# Section 2: Code Quality & Design

## Overview

This section covers **architectural and design issues** that affect code maintainability, testability, and extensibility. These issues violate SOLID principles, create tight coupling, and make the codebase harder to evolve.

**Key Statistics:**
- Issues Found: 8
- Critical: 1
- High: 4
- Medium: 3
- Low: 0

---

## 🔴 Issue 2.1: Null Dependency - Repository Not Injected

**Priority:** CRITICAL
**Location:** `AuthorService.java:8-9`

**Current Code:**

```java
@Service
public class AuthorService {
    AuthorRepository authorRepository;  // ❌ Declared but NEVER assigned!

    public List<AuthorEntity> findByBookId(Long id) {
        return authorRepository.findByBooks_BookId(id);  // ❌ NullPointerException!
    }
}
```

### Why It's Problematic

1. **Guaranteed Crash:** This code will throw `NullPointerException` immediately when called
2. **Shows No Testing:** This bug would be caught by even a basic unit test
3. **Incomplete Implementation:** Repository declared but never initialized

### What Happens at Runtime

```
User searches for authors by book:
  → authorService.findByBookId(123) is called
    → authorRepository is null
      → authorRepository.findByBooks_BookId(123) throws NullPointerException
        → Stack trace printed
          → Application crashes
```

### How This Happened

```java
// You probably started with:
@Service
public class AuthorService {
    @Autowired  // Then removed @Autowired
    AuthorRepository authorRepository;
}

// Or forgot to add constructor:
public AuthorService(AuthorRepository authorRepository) {
    // Never assigned this.authorRepository!
}
```

### Correct Approach

**Option 1: Constructor Injection (Recommended)**

```java
@Service
public class AuthorService {
    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<AuthorEntity> findByBookId(Long id) {
        return authorRepository.findByBooks_BookId(id);
    }
}
```

**Option 2: Using Lombok (Cleanest)**

```java
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor  // Generates constructor for final fields
public class AuthorService {
    private final AuthorRepository authorRepository;

    public List<AuthorEntity> findByBookId(Long id) {
        return authorRepository.findByBooks_BookId(id);
    }
}
```

**Option 3: Field Injection (Not Recommended, but Better Than Nothing)**

```java
@Service
public class AuthorService {
    @Autowired  // ⚠️ Field injection - harder to test
    private AuthorRepository authorRepository;

    public List<AuthorEntity> findByBookId(Long id) {
        return authorRepository.findByBooks_BookId(id);
    }
}
```

### Why Constructor Injection Is Best

| Aspect | Constructor Injection | Field Injection |
|--------|----------------------|-----------------|
| Immutability | ✅ Can use `final` | ❌ Cannot use `final` |
| Testing | ✅ Easy - pass mocks | ❌ Need Spring context or reflection |
| Null Safety | ✅ Fails at startup | ❌ Fails at runtime |
| Circular Dependencies | ✅ Detected at startup | ❌ Detected at runtime |
| IDE Support | ✅ Shows dependencies | ❌ Hidden dependencies |
| Spring 4.3+ | ✅ No `@Autowired` needed | ❌ Requires `@Autowired` |

### Complete Example with Testing

```java
// AuthorService.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorService {
    private final AuthorRepository authorRepository;

    public List<AuthorEntity> findByBookId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Book ID must be positive");
        }
        return authorRepository.findByBooks_BookId(id);
    }

    public Optional<AuthorEntity> findById(Long id) {
        return authorRepository.findById(id);
    }

    public List<AuthorEntity> findByName(String firstName, String lastName) {
        return authorRepository.findByFirstNameAndLastName(firstName, lastName);
    }
}

// AuthorServiceTest.java - Now easy to test!
@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorService authorService;

    @Test
    void shouldFindAuthorsByBookId() {
        // Arrange
        Long bookId = 1L;
        List<AuthorEntity> mockAuthors = List.of(
            new AuthorEntity("Robert", "Martin"),
            new AuthorEntity("Kent", "Beck")
        );
        when(authorRepository.findByBooks_BookId(bookId)).thenReturn(mockAuthors);

        // Act
        List<AuthorEntity> result = authorService.findByBookId(bookId);

        // Assert
        assertEquals(2, result.size());
        verify(authorRepository).findByBooks_BookId(bookId);
    }

    @Test
    void shouldThrowExceptionForInvalidBookId() {
        assertThrows(IllegalArgumentException.class,
            () -> authorService.findByBookId(-1L));
    }
}
```

### Learning Principle

> **Always use constructor injection in Spring services.** It makes dependencies explicit, enables immutability, facilitates testing, and catches configuration errors at startup time instead of runtime. Since Spring 4.3, `@Autowired` is not needed on single-constructor classes.

### Action Items

1. ✅ Add constructor to `AuthorService`
2. ✅ Make repository field `final`
3. ✅ Add Lombok `@RequiredArgsConstructor` if available
4. ✅ Write unit test to prevent regression

**Estimated Fix Time:** 5 minutes

---

## 🟠 Issue 2.2: Violation of Single Responsibility Principle (SRP)

**Priority:** HIGH
**Location:** `BookService.java`

**Current Code:**

```java
@Service
public class BookService {
    // RESPONSIBILITY 1: Create books
    @Transactional
    public void createNewBook(BookRequestDTO bookRequestDTO) { }

    // RESPONSIBILITY 2: Search/query books
    public BookEntity findBookByTitle(String title) { }

    // RESPONSIBILITY 3: Manage book state
    public void checkOutBook(BookEntity bookEntity) { }
    public void checkInBook(BookEntity bookEntity) { }

    // RESPONSIBILITY 4: Update books
    public void updateBook(BookEntity bookEntity) { }

    // RESPONSIBILITY 5: Query by relationships
    public List<BookEntity> findBooksByShelf(Long shelfId) { }
}
```

### Why It's Problematic

1. **Too Many Reasons to Change:** This class changes when:
   - Book creation logic changes
   - Search logic changes
   - Checkout rules change
   - Update logic changes
   - Query optimization needed

2. **Hard to Test:** Must mock many dependencies for any test

3. **Poor Cohesion:** Methods don't relate to a single concept

4. **Violates SRP:** "A class should have only ONE reason to change" - this has 5+

5. **Difficult to Extend:** Adding features requires modifying this large class

### The Single Responsibility Principle

> **A class should have one, and only one, reason to change.**
> - Robert C. Martin (Uncle Bob)

```
❌ BookService has multiple responsibilities:
├── Book Creation (business logic)
├── Book Querying (data access)
├── Book State Management (checkout/checkin)
├── Book Updates (persistence)
└── Relationship Queries (data access)

Each is a separate reason to change!
```

### Identifying Responsibilities

Ask: **"Why would I need to modify this class?"**

```java
// Scenario 1: Change search algorithm
// → Must modify BookService (contains search)

// Scenario 2: Change checkout rules
// → Must modify BookService (contains checkout)

// Scenario 3: Optimize database queries
// → Must modify BookService (contains queries)

// Scenario 4: Add book validation
// → Must modify BookService (contains creation)

// Four different reasons = Four responsibilities!
```

### Correct Approach - Split by Responsibility

**Create Separate Services:**

```java
// 1. BookCommandService - Handles write operations (CUD in CRUD)
@Service
@RequiredArgsConstructor
@Transactional
public class BookCommandService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    /**
     * Creates a new book with associated author.
     * If author doesn't exist, creates the author too.
     */
    public BookEntity createBook(BookRequestDTO dto) {
        // Validate input
        validateBookRequest(dto);

        // Find or create author
        AuthorEntity author = findOrCreateAuthor(dto.firstName(), dto.lastName());

        // Create book
        BookEntity book = new BookEntity();
        book.setTitle(dto.title());
        book.setAuthor(author);
        book.setStatus(BookStatus.AVAILABLE);

        return bookRepository.save(book);
    }

    /**
     * Updates an existing book.
     */
    public BookEntity updateBook(BookEntity book) {
        if (book.getBookId() == null) {
            throw new IllegalArgumentException("Cannot update book without ID");
        }
        return bookRepository.save(book);
    }

    /**
     * Deletes a book by ID.
     */
    public void deleteBook(Long bookId) {
        bookRepository.deleteById(bookId);
    }

    private AuthorEntity findOrCreateAuthor(String firstName, String lastName) {
        return authorRepository.findByFirstNameAndLastName(firstName, lastName)
            .orElseGet(() -> authorRepository.save(new AuthorEntity(firstName, lastName)));
    }

    private void validateBookRequest(BookRequestDTO dto) {
        if (dto.title() == null || dto.title().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        // More validation...
    }
}

// 2. BookQueryService - Handles read operations (R in CRUD)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookQueryService {
    private final BookRepository bookRepository;

    /**
     * Finds a book by exact title (case-insensitive).
     */
    public Optional<BookEntity> findByExactTitle(String title) {
        return Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
    }

    /**
     * Searches for books containing the query in title.
     */
    public List<BookEntity> searchByTitle(String query) {
        return bookRepository.findByTitleContainsIgnoreCase(query);
    }

    /**
     * Finds all books on a specific shelf.
     */
    public List<BookEntity> findByShelf(Long shelfId) {
        return bookRepository.findByShelfId(shelfId);
    }

    /**
     * Gets detailed view of a book including relationships.
     */
    public BookDetailView getBookDetails(Long bookId) {
        return bookRepository.getBookDetailView(bookId);
    }

    /**
     * Finds all available books.
     */
    public List<BookEntity> findAvailableBooks() {
        return bookRepository.findByStatus(BookStatus.AVAILABLE);
    }
}

// 3. BookCheckoutService - Handles checkout/checkin logic
@Service
@RequiredArgsConstructor
@Transactional
public class BookCheckoutService {
    private final BookRepository bookRepository;
    private final BookQueryService bookQueryService;

    /**
     * Checks out a book to a user.
     *
     * @throws BookNotAvailableException if book is already checked out
     */
    public void checkout(Long bookId, Long userId) {
        BookEntity book = bookQueryService.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        if (book.getStatus() != BookStatus.AVAILABLE) {
            throw new BookNotAvailableException(
                String.format("Book '%s' is not available (status: %s)",
                    book.getTitle(), book.getStatus())
            );
        }

        book.setStatus(BookStatus.CHECKED_OUT);
        book.setCheckedOutBy(userId);
        book.setCheckedOutAt(LocalDateTime.now());
        book.setDueDate(LocalDateTime.now().plusDays(14)); // 2 week checkout

        bookRepository.save(book);
    }

    /**
     * Checks in a previously checked out book.
     *
     * @throws BookNotCheckedOutException if book is not checked out
     */
    public void checkin(Long bookId) {
        BookEntity book = bookQueryService.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        if (book.getStatus() != BookStatus.CHECKED_OUT) {
            throw new BookNotCheckedOutException(
                String.format("Book '%s' is not checked out", book.getTitle())
            );
        }

        book.setStatus(BookStatus.AVAILABLE);
        book.setCheckedOutBy(null);
        book.setCheckedOutAt(null);
        book.setDueDate(null);

        bookRepository.save(book);
    }

    /**
     * Finds all overdue books.
     */
    public List<BookEntity> findOverdueBooks() {
        return bookRepository.findByStatusAndDueDateBefore(
            BookStatus.CHECKED_OUT,
            LocalDateTime.now()
        );
    }

    /**
     * Renews a checkout for another 14 days.
     */
    public void renewCheckout(Long bookId) {
        BookEntity book = bookQueryService.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        if (book.getStatus() != BookStatus.CHECKED_OUT) {
            throw new BookNotCheckedOutException("Cannot renew book that is not checked out");
        }

        book.setDueDate(book.getDueDate().plusDays(14));
        bookRepository.save(book);
    }
}
```

### Benefits of This Separation

**Before (Single Service):**
```
BookService (300 lines)
├── 7 methods
├── 3 dependencies
├── Hard to test (many scenarios)
└── Changes for any book-related feature

Tests: 30+ test cases in one file
```

**After (Separated Services):**
```
BookCommandService (100 lines)
├── 3 methods (create, update, delete)
├── 2 dependencies
├── Easy to test (focused scenarios)
└── Changes only for write operations

BookQueryService (80 lines)
├── 5 methods (various queries)
├── 1 dependency
├── Easy to test (just queries)
└── Changes only for query optimization

BookCheckoutService (120 lines)
├── 4 methods (checkout, checkin, renew, overdue)
├── 2 dependencies
├── Easy to test (business rules)
└── Changes only for checkout logic

Tests: 10-15 test cases per service (focused!)
```

### CQRS Pattern (Command Query Responsibility Segregation)

Your separated services follow CQRS pattern:

```
Commands (Change State):          Queries (Read State):
├── BookCommandService           ├── BookQueryService
│   ├── createBook()            │   ├── findByTitle()
│   ├── updateBook()            │   ├── searchByTitle()
│   └── deleteBook()            │   ├── findByShelf()
│                               │   └── getBookDetails()
└── BookCheckoutService          │
    ├── checkout()               └── Read-only transactions
    ├── checkin()                    ↓
    └── renewCheckout()              Performance optimization
        ↓                            Caching possible
        Write transactions
        Auditing/logging
```

### Usage Example

```java
// Before: Everything in BookService
@RestController
public class BookController {
    private final BookService bookService;  // Does everything

    @PostMapping("/books")
    public BookEntity createBook(@RequestBody BookRequestDTO dto) {
        return bookService.createNewBook(dto);
    }

    @GetMapping("/books/search")
    public List<BookEntity> search(@RequestParam String query) {
        return bookService.findBookByTitle(query);
    }

    @PostMapping("/books/{id}/checkout")
    public void checkout(@PathVariable Long id) {
        BookEntity book = bookService.findById(id);
        bookService.checkOutBook(book);
    }
}

// After: Focused services
@RestController
@RequiredArgsConstructor
public class BookController {
    private final BookCommandService bookCommandService;
    private final BookQueryService bookQueryService;
    private final BookCheckoutService bookCheckoutService;

    @PostMapping("/books")
    public ResponseEntity<BookEntity> createBook(@Valid @RequestBody BookRequestDTO dto) {
        BookEntity book = bookCommandService.createBook(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }

    @GetMapping("/books/search")
    public ResponseEntity<List<BookEntity>> search(@RequestParam String query) {
        List<BookEntity> books = bookQueryService.searchByTitle(query);
        return ResponseEntity.ok(books);
    }

    @PostMapping("/books/{id}/checkout")
    public ResponseEntity<Void> checkout(@PathVariable Long id, @RequestParam Long userId) {
        bookCheckoutService.checkout(id, userId);
        return ResponseEntity.ok().build();
    }
}
```

### Testing Becomes Easier

```java
// Before: Complex test with many scenarios
@SpringBootTest
class BookServiceTest {
    @Autowired BookService bookService;

    @Test
    void testEverything() {
        // Test creation
        // Test querying
        // Test checkout
        // Test updates
        // 100+ lines of test code
    }
}

// After: Focused tests
@ExtendWith(MockitoExtension.class)
class BookCommandServiceTest {
    @Mock BookRepository bookRepository;
    @Mock AuthorRepository authorRepository;
    @InjectMocks BookCommandService service;

    @Test
    void shouldCreateBookWithNewAuthor() {
        // Only test creation logic
        // 10 lines of focused test
    }
}

@ExtendWith(MockitoExtension.class)
class BookQueryServiceTest {
    @Mock BookRepository bookRepository;
    @InjectMocks BookQueryService service;

    @Test
    void shouldFindBookByTitle() {
        // Only test query logic
        // 8 lines of focused test
    }
}

@ExtendWith(MockitoExtension.class)
class BookCheckoutServiceTest {
    @Mock BookRepository bookRepository;
    @Mock BookQueryService bookQueryService;
    @InjectMocks BookCheckoutService service;

    @Test
    void shouldCheckoutAvailableBook() {
        // Only test checkout logic
        // 12 lines of focused test
    }

    @Test
    void shouldThrowExceptionWhenBookAlreadyCheckedOut() {
        // Test business rule
    }
}
```

### Learning Principle

> **Single Responsibility Principle: A class should have one, and only one, reason to change.** If you can describe a class with "AND" (creates books AND searches books AND manages checkout), it has too many responsibilities. Split it into focused services: Commands (write), Queries (read), and Domain Logic (business rules).

### Action Items

1. ✅ Create `BookCommandService` for create/update/delete
2. ✅ Create `BookQueryService` for all read operations
3. ✅ Create `BookCheckoutService` for checkout logic
4. ✅ Update controllers to use new services
5. ✅ Write focused unit tests for each service
6. ✅ Mark old `BookService` as `@Deprecated` then remove

**Estimated Fix Time:** 3 hours

---

## 🟠 Issue 2.3: Tight Coupling - Too Many Dependencies

**Priority:** HIGH
**Location:** `BookCommands.java:28-32`

**Current Code:**

```java
@Component
@ShellComponent
public class BookCommands {
    final BookService bookService;
    final BookController bookController;  // ❌ CLI depends on Controller?!
    final BookcaseService bookcaseService;
    final ShelfService shelfService;
    final AuthorService authorService;

    // Constructor with 5 dependencies!
    public BookCommands(BookService bookService,
                       BookController bookController,
                       BookcaseService bookcaseService,
                       ShelfService shelfService,
                       AuthorService authorService) {
        this.bookService = bookService;
        this.bookController = bookController;
        this.bookcaseService = bookcaseService;
        this.shelfService = shelfService;
        this.authorService = authorService;
    }

    @Command(command = "add")
    public void addBook() {
        // Uses bookService, shelfService, bookcaseService...
    }
}
```

### Why It's Problematic

1. **Too Many Dependencies:** 5 dependencies suggests design problem (rule of thumb: max 3-4)
2. **Wrong Dependency:** CLI should NEVER depend on Controller (breaks layering)
3. **Violates Dependency Inversion:** Depends on concrete services, not abstractions
4. **Hard to Test:** Must mock 5 different classes
5. **High Coupling:** Changes in any service break BookCommands

### Architecture Violation

```
❌ Current (Wrong):
┌──────────────────┐
│  BookCommands    │ (CLI Layer)
│   (depends on)   │
├──────────────────┤
│ BookController   │ (Web Layer) ← CLI shouldn't depend on Web!
│ BookService      │ (Service Layer)
│ ShelfService     │ (Service Layer)
│ BookcaseService  │ (Service Layer)
│ AuthorService    │ (Service Layer)
└──────────────────┘

Problems:
- Layer violation (CLI → Web)
- Too many direct dependencies
- CLI knows about implementation details
```

### Correct Approach - Facade Pattern

Create a **Facade** that provides a simplified interface:

```java
/**
 * Facade that coordinates book-related operations across multiple services.
 * Provides a simplified interface for complex library operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LibraryManagementFacade {
    // Focused dependencies
    private final BookCommandService bookCommandService;
    private final BookQueryService bookQueryService;
    private final BookCheckoutService bookCheckoutService;
    private final ShelfService shelfService;
    private final BookcaseService bookcaseService;

    /**
     * Adds a new book and assigns it to a shelf.
     * Coordinates: book creation + shelf assignment
     */
    @Transactional
    public BookEntity addBookToShelf(BookRequestDTO bookRequest, Long shelfId) {
        log.info("Adding book '{}' to shelf {}", bookRequest.title(), shelfId);

        // Validate shelf exists
        ShelfEntity shelf = shelfService.findById(shelfId)
            .orElseThrow(() -> new ShelfNotFoundException(shelfId));

        // Create book
        BookEntity book = bookCommandService.createBook(bookRequest);

        // Assign to shelf
        book.setShelfId(shelfId);
        bookCommandService.updateBook(book);

        log.info("Book '{}' (ID: {}) added to shelf {}",
            book.getTitle(), book.getBookId(), shelfId);

        return book;
    }

    /**
     * Searches for a book and returns its location details.
     * Coordinates: book search + shelf lookup + bookcase lookup
     */
    @Transactional(readOnly = true)
    public BookLocationView findBookWithLocation(String title) {
        BookEntity book = bookQueryService.findByExactTitle(title)
            .orElseThrow(() -> new BookNotFoundException(title));

        if (book.getShelfId() == null) {
            return new BookLocationView(book, null, null);
        }

        ShelfEntity shelf = shelfService.findById(book.getShelfId()).orElse(null);
        BookcaseEntity bookcase = shelf != null
            ? bookcaseService.findById(shelf.getBookcaseId()).orElse(null)
            : null;

        return new BookLocationView(book, shelf, bookcase);
    }

    /**
     * Checks out a book and validates its availability.
     * Coordinates: book lookup + availability check + checkout
     */
    @Transactional
    public void checkoutBook(String title, Long userId) {
        BookEntity book = bookQueryService.findByExactTitle(title)
            .orElseThrow(() -> new BookNotFoundException(title));

        bookCheckoutService.checkout(book.getBookId(), userId);

        log.info("Book '{}' checked out to user {}", title, userId);
    }

    /**
     * Moves a book from one shelf to another.
     * Coordinates: book lookup + shelf validation + update
     */
    @Transactional
    public void moveBook(Long bookId, Long newShelfId) {
        BookEntity book = bookQueryService.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        ShelfEntity newShelf = shelfService.findById(newShelfId)
            .orElseThrow(() -> new ShelfNotFoundException(newShelfId));

        Long oldShelfId = book.getShelfId();
        book.setShelfId(newShelfId);
        bookCommandService.updateBook(book);

        log.info("Book {} moved from shelf {} to shelf {}",
            bookId, oldShelfId, newShelfId);
    }

    /**
     * Gets summary statistics for the library.
     * Coordinates: multiple queries across services
     */
    @Transactional(readOnly = true)
    public LibraryStatistics getStatistics() {
        long totalBooks = bookQueryService.countAll();
        long availableBooks = bookQueryService.countByStatus(BookStatus.AVAILABLE);
        long checkedOutBooks = bookQueryService.countByStatus(BookStatus.CHECKED_OUT);
        long totalShelves = shelfService.countAll();
        long totalBookcases = bookcaseService.countAll();

        return new LibraryStatistics(
            totalBooks,
            availableBooks,
            checkedOutBooks,
            totalShelves,
            totalBookcases
        );
    }
}

// Supporting classes
public record BookLocationView(
    BookEntity book,
    ShelfEntity shelf,
    BookcaseEntity bookcase
) {
    public String formatLocation() {
        if (shelf == null) {
            return "Book is not assigned to a shelf";
        }
        return String.format("Bookcase: %s, Shelf: %s",
            bookcase != null ? bookcase.getBookcaseLabel() : "Unknown",
            shelf.getShelfLabel()
        );
    }
}

public record LibraryStatistics(
    long totalBooks,
    long availableBooks,
    long checkedOutBooks,
    long totalShelves,
    long totalBookcases
) {}
```

**Now BookCommands is Simple:**

```java
@Component
@ShellComponent
@RequiredArgsConstructor
public class BookCommands {
    // Only ONE dependency!
    private final LibraryManagementFacade libraryFacade;

    @Command(command = "add", description = "Add a book to the library")
    public void addBook() {
        // Collect input
        String title = promptForInput("Book title: ");
        String firstName = promptForInput("Author first name: ");
        String lastName = promptForInput("Author last name: ");
        Long shelfId = promptForShelfId();

        // Delegate to facade
        try {
            BookRequestDTO request = new BookRequestDTO(title, firstName, lastName);
            BookEntity book = libraryFacade.addBookToShelf(request, shelfId);
            System.out.println("✅ Book added successfully: " + book.getTitle());
        } catch (ShelfNotFoundException e) {
            System.err.println("❌ Shelf not found: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error adding book: " + e.getMessage());
        }
    }

    @Command(command = "search", description = "Search for a book")
    public void searchBook() {
        String title = promptForInput("Enter book title: ");

        try {
            BookLocationView location = libraryFacade.findBookWithLocation(title);
            System.out.println("📚 Book: " + location.book().getTitle());
            System.out.println("📍 Location: " + location.formatLocation());
            System.out.println("📊 Status: " + location.book().getStatus());
        } catch (BookNotFoundException e) {
            System.err.println("❌ Book not found: " + e.getMessage());
        }
    }

    @Command(command = "checkout", description = "Checkout a book")
    public void checkout() {
        String title = promptForInput("Book title: ");
        Long userId = promptForUserId();

        try {
            libraryFacade.checkoutBook(title, userId);
            System.out.println("✅ Book checked out successfully");
        } catch (BookNotFoundException e) {
            System.err.println("❌ Book not found: " + e.getMessage());
        } catch (BookNotAvailableException e) {
            System.err.println("❌ Book not available: " + e.getMessage());
        }
    }

    @Command(command = "stats", description = "Show library statistics")
    public void showStatistics() {
        LibraryStatistics stats = libraryFacade.getStatistics();

        System.out.println("📊 Library Statistics");
        System.out.println("═══════════════════════");
        System.out.printf("Total Books:      %d%n", stats.totalBooks());
        System.out.printf("Available:        %d%n", stats.availableBooks());
        System.out.printf("Checked Out:      %d%n", stats.checkedOutBooks());
        System.out.printf("Total Shelves:    %d%n", stats.totalShelves());
        System.out.printf("Total Bookcases:  %d%n", stats.totalBookcases());
    }

    // Helper methods
    private String promptForInput(String prompt) {
        // Implementation
    }

    private Long promptForShelfId() {
        // Implementation
    }

    private Long promptForUserId() {
        // Implementation
    }
}
```

### Benefits of Facade Pattern

**Before:**
```
BookCommands
├── Depends on 5 services
├── Coordinates complex operations
├── Knows about service implementation details
├── Hard to test (5 mocks needed)
└── 400+ lines of code
```

**After:**
```
BookCommands
├── Depends on 1 facade
├── Simple delegation
├── No knowledge of service details
├── Easy to test (1 mock needed)
└── 150 lines of code

LibraryManagementFacade
├── Coordinates operations
├── Provides high-level API
├── Handles transaction boundaries
├── Testable independently
└── Reusable by Web, CLI, API
```

### Testing Comparison

```java
// Before: Hard to test
@ExtendWith(MockitoExtension.class)
class BookCommandsTest {
    @Mock BookService bookService;
    @Mock ShelfService shelfService;
    @Mock BookcaseService bookcaseService;
    @Mock AuthorService authorService;
    @Mock BookController bookController;  // Why?!

    @InjectMocks BookCommands bookCommands;

    @Test
    void testAddBook() {
        // Setup 5 mocks
        // Complex test logic
    }
}

// After: Easy to test
@ExtendWith(MockitoExtension.class)
class BookCommandsTest {
    @Mock LibraryManagementFacade libraryFacade;
    @InjectMocks BookCommands bookCommands;

    @Test
    void shouldAddBookSuccessfully() {
        // Setup 1 mock
        BookRequestDTO request = new BookRequestDTO("Clean Code", "Robert", "Martin");
        BookEntity mockBook = new BookEntity();
        mockBook.setTitle("Clean Code");

        when(libraryFacade.addBookToShelf(any(), anyLong())).thenReturn(mockBook);

        // Test command
        bookCommands.addBook();

        verify(libraryFacade).addBookToShelf(any(), anyLong());
    }
}

// Facade has its own tests
@ExtendWith(MockitoExtension.class)
class LibraryManagementFacadeTest {
    @Mock BookCommandService bookCommandService;
    @Mock BookQueryService bookQueryService;
    @Mock ShelfService shelfService;

    @InjectMocks LibraryManagementFacade facade;

    @Test
    void shouldAddBookToShelf() {
        // Test coordination logic
    }
}
```

### Learning Principle

> **Facade Pattern simplifies complex subsystems.** When a class needs many dependencies (4+), create a facade that provides a simpler interface. This implements Dependency Inversion Principle - depend on high-level abstractions (facade), not low-level details (individual services). Limit dependencies to 3-4 per class.

### Action Items

1. ✅ Create `LibraryManagementFacade` service
2. ✅ Move coordination logic from `BookCommands` to facade
3. ✅ Update `BookCommands` to use only facade
4. ✅ Remove `BookController` dependency from `BookCommands`
5. ✅ Write tests for facade and commands separately

**Estimated Fix Time:** 2 hours

---

## 🟠 Issue 2.4: Duplicate Repository Query Methods

**Priority:** HIGH
**Location:** `BookRepository.java:14-20`

**Current Code:**

```java
public interface BookRepository extends JpaRepository<BookEntity, Long> {
    // ❌ Five methods that do nearly the same thing!
    BookEntity findBookEntityByTitle(String title);
    BookEntity findByTitle(String title);
    BookEntity findByTitleIgnoreCase(String title);
    List<BookEntity> findByTitleContaining(String title);
    List<BookEntity> findByTitleContains(String title);

    // Which one should I use? 🤔
}
```

### Why It's Problematic

1. **Confusion:** Which method should developers use?
2. **Inconsistency:** Code uses different methods in different places
3. **Maintenance Burden:** Bug fixes must be applied to all similar methods
4. **Dead Code:** Some methods probably never used
5. **No Clear Intent:** Methods don't clearly communicate their purpose

### Method Name Analysis

```java
findBookEntityByTitle(String title)
// ❓ What does "BookEntity" in name add? Redundant
// Returns: BookEntity (not null-safe)

findByTitle(String title)
// ❓ Case-sensitive or insensitive?
// Returns: BookEntity (not null-safe)

findByTitleIgnoreCase(String title)
// ✅ Clear: case-insensitive exact match
// ❌ Returns: BookEntity (not null-safe)

findByTitleContaining(String title)
// ❓ Case-sensitive or insensitive?
// Returns: List<BookEntity>

findByTitleContains(String title)
// ❓ Same as findByTitleContaining? What's the difference?
// Returns: List<BookEntity>
```

### Correct Approach

**Keep Only Clear, Necessary Methods:**

```java
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    /**
     * Finds a book by exact title match (case-insensitive).
     *
     * @param title the book title to search for
     * @return the book entity, or null if not found
     */
    BookEntity findByTitleIgnoreCase(String title);

    /**
     * Searches for books with titles containing the query string (case-insensitive).
     * Useful for partial title searches.
     *
     * @param titleFragment partial title to search for
     * @return list of matching books (empty list if none found)
     */
    List<BookEntity> findByTitleContainsIgnoreCase(String titleFragment);

    /**
     * Searches for books with pagination support.
     *
     * @param titleFragment partial title to search for
     * @param pageable pagination parameters (page number, size, sort)
     * @return page of matching books
     */
    Page<BookEntity> findByTitleContainsIgnoreCase(String titleFragment, Pageable pageable);

    /**
     * Finds all books on a specific shelf.
     *
     * @param shelfId the shelf ID
     * @return list of books on the shelf (empty if none)
     */
    List<BookEntity> findByShelfId(Long shelfId);

    /**
     * Finds all books with a specific status.
     *
     * @param status the book status (AVAILABLE, CHECKED_OUT, etc.)
     * @return list of books with the given status
     */
    List<BookEntity> findByStatus(BookStatus status);

    /**
     * Counts books by status.
     *
     * @param status the book status
     * @return count of books with that status
     */
    long countByStatus(BookStatus status);

    /**
     * Checks if a book with the given title exists (case-insensitive).
     * More efficient than findByTitleIgnoreCase when you only need existence check.
     *
     * @param title the book title
     * @return true if book exists, false otherwise
     */
    boolean existsByTitleIgnoreCase(String title);
}
```

### Naming Conventions for Repository Methods

Spring Data JPA uses method names to generate queries. Follow these conventions:

```java
// Query Keywords:
findBy...        // Returns entity or collection
getBy...         // Alias for findBy
readBy...        // Alias for findBy
queryBy...       // Alias for findBy
countBy...       // Returns count (long)
existsBy...      // Returns boolean
deleteBy...      // Deletes and returns count or void

// Property Keywords:
...IgnoreCase           // Case-insensitive
...Containing           // LIKE %value%
...StartingWith         // LIKE value%
...EndingWith           // LIKE %value
...Between              // value BETWEEN start AND end
...LessThan            // value < param
...GreaterThan         // value > param
...Before              // value < param (dates)
...After               // value > param (dates)
...IsNull              // value IS NULL
...IsNotNull           // value IS NOT NULL
...OrderBy...Asc       // ORDER BY ... ASC
...OrderBy...Desc      // ORDER BY ... DESC

// Logical Operators:
...And...              // WHERE field1 = ? AND field2 = ?
...Or...               // WHERE field1 = ? OR field2 = ?

// Examples:
findByTitleAndAuthor(String title, String author)
findByPriceLessThan(BigDecimal price)
findByPublishedDateBetween(LocalDate start, LocalDate end)
findByTitleContainingIgnoreCaseOrderByPublishedDateDesc(String title)
```

### Examples of Good Repository Methods

```java
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    // Exact match queries
    Optional<BookEntity> findByIsbn(String isbn);
    BookEntity findByTitleIgnoreCase(String title);

    // Search queries
    List<BookEntity> findByTitleContainsIgnoreCase(String query);
    List<BookEntity> findByAuthorLastName(String lastName);

    // Range queries
    List<BookEntity> findByPriceBetween(BigDecimal min, BigDecimal max);
    List<BookEntity> findByPublishedDateAfter(LocalDate date);

    // Combination queries
    List<BookEntity> findByStatusAndShelfId(BookStatus status, Long shelfId);
    List<BookEntity> findByTitleContainsAndStatus(String title, BookStatus status);

    // Aggregation
    long countByStatus(BookStatus status);
    long countByShelfId(Long shelfId);

    // Existence checks
    boolean existsByIsbn(String isbn);
    boolean existsByTitleIgnoreCase(String title);

    // Custom queries (when method names get too complex)
    @Query("SELECT b FROM BookEntity b WHERE b.status = :status AND b.dueDate < :date")
    List<BookEntity> findOverdueBooks(@Param("status") BookStatus status, @Param("date") LocalDateTime date);

    // Projections (DTO queries)
    @Query("SELECT new com.penrose.bibby.library.book.BookSummary(b.id, b.title, b.status) " +
           "FROM BookEntity b WHERE b.shelfId = :shelfId")
    List<BookSummary> findBookSummariesByShelf(@Param("shelfId") Long shelfId);

    // Native queries (when JPQL isn't enough)
    @Query(value = "SELECT * FROM books WHERE LOWER(title) LIKE LOWER(CONCAT('%', :query, '%'))",
           nativeQuery = true)
    List<BookEntity> searchByTitleNative(@Param("query") String query);
}
```

### Pagination Support

```java
// Add pagination to search methods
Page<BookEntity> findByTitleContainsIgnoreCase(String query, Pageable pageable);

// Usage:
Pageable pageable = PageRequest.of(
    0,          // page number (0-indexed)
    20,         // page size
    Sort.by("title").ascending()
);

Page<BookEntity> page = bookRepository.findByTitleContainsIgnoreCase("java", pageable);

System.out.println("Total results: " + page.getTotalElements());
System.out.println("Total pages: " + page.getTotalPages());
System.out.println("Current page: " + page.getNumber());
List<BookEntity> books = page.getContent();
```

### Learning Principle

> **Keep repository interfaces minimal and clear.** Each method should have a distinct, well-documented purpose. Remove duplicate methods. Use consistent naming (always `IgnoreCase` for case-insensitive). Add pagination for queries returning lists. When method names become too complex, use `@Query` instead.

### Action Items

1. ✅ Identify which methods are actually used (search codebase)
2. ✅ Delete unused methods
3. ✅ Rename ambiguous methods to be clear
4. ✅ Add Javadoc to all repository methods
5. ✅ Add pagination support for search methods

**Estimated Fix Time:** 30 minutes

---

## 🟡 Issue 2.5: Inconsistent Domain vs Entity Models

**Priority:** MEDIUM
**Locations:** `Book.java`, `BookEntity.java`

**Current Code:**

```java
// Book.java - Domain Model (190 lines)
public class Book {
    Long bookId;
    String title;
    AuthorEntity authorEntity;  // ❌ Domain model references entity
    BookStatus status;          // ✅ Uses enum
    // ... equals/hashCode implemented
}

// BookEntity.java - JPA Entity (120 lines)
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    private Long bookId;
    private String title;
    @ManyToOne
    private AuthorEntity authorEntity;
    private String bookStatus;  // ❌ Uses String, not enum!
    // ... no equals/hashCode
}
```

### Why It's Problematic

1. **Two Models for Same Concept:** Duplicates code and creates confusion
2. **Type Mismatch:** `Book.status` is enum, `BookEntity.bookStatus` is String
3. **Layer Violation:** Domain model (`Book`) references persistence layer (`AuthorEntity`)
4. **No Clear Usage Pattern:** When to use `Book` vs `BookEntity`?
5. **Mapping Overhead:** Constantly converting between the two

### The Two-Model Problem

```
Current State:
┌─────────────┐       ┌──────────────┐
│    Book     │←─?──→ │ BookEntity   │
│  (Domain)   │       │ (Persistence)│
├─────────────┤       ├──────────────┤
│ status: Enum│       │ status:String│
│ author:     │       │ author:      │
│   Entity❌  │       │   Entity✅   │
└─────────────┘       └──────────────┘
      ↑                      ↑
      │                      │
   When to use?          When to use?
```

### Decision: Choose Your Strategy

You have two viable options:

**Option 1: Use Entities Directly (Simpler - Recommended for Your App)**

For simple CRUD applications like yours, using JPA entities directly is cleaner:

```java
// Keep only BookEntity - delete Book.java
@Entity
@Table(name = "books")
@Data
@EqualsAndHashCode(of = "bookId")
public class Book {  // No "Entity" suffix
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;  // Also an entity

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookStatus status;

    @Column(name = "shelf_id")
    private Long shelfId;

    @Column(name = "checked_out_by")
    private Long checkedOutBy;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "due_date")
    private LocalDateTime dueDate;
}

// Author entity
@Entity
@Table(name = "authors")
@Data
@EqualsAndHashCode(of = "authorId")
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long authorId;

    private String firstName;
    private String lastName;

    @ManyToMany(mappedBy = "authors")
    private Set<Book> books = new HashSet<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }
}

// Use records for DTOs (API requests/responses)
public record BookResponse(
    Long bookId,
    String title,
    String authorName,
    String status
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
            book.getBookId(),
            book.getTitle(),
            book.getAuthor().getFullName(),
            book.getStatus().name()
        );
    }
}

public record BookRequest(
    @NotBlank String title,
    @NotBlank String firstName,
    @NotBlank String lastName
) {}
```

**Option 2: Separate Domain and Persistence (For Complex Apps)**

If you have complex business logic, separate the layers:

```java
// BookEntity.java - Persistence Layer (minimal, just data)
@Entity
@Table(name = "books")
@Data
@EqualsAndHashCode(of = "id")
class BookEntity {  // Package-private
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private BookStatus status;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private AuthorEntity author;

    private Long shelfId;
    private LocalDateTime checkedOutAt;
    private LocalDateTime dueDate;
}

// Book.java - Domain Model (rich behavior)
public class Book {
    private final Long id;
    private final String title;
    private final Author author;
    private BookStatus status;
    private Long shelfId;
    private LocalDateTime checkedOutAt;
    private LocalDateTime dueDate;

    // Rich domain methods
    public void checkout(Long userId) {
        if (status == BookStatus.CHECKED_OUT) {
            throw new BookAlreadyCheckedOutException(
                "Book is already checked out"
            );
        }
        this.status = BookStatus.CHECKED_OUT;
        this.checkedOutAt = LocalDateTime.now();
        this.dueDate = LocalDateTime.now().plusDays(14);
    }

    public void checkin() {
        if (status != BookStatus.CHECKED_OUT) {
            throw new InvalidOperationException("Book is not checked out");
        }
        this.status = BookStatus.AVAILABLE;
        this.checkedOutAt = null;
        this.dueDate = null;
    }

    public boolean isOverdue() {
        return status == BookStatus.CHECKED_OUT
            && dueDate != null
            && LocalDateTime.now().isAfter(dueDate);
    }

    public boolean isAvailable() {
        return status == BookStatus.AVAILABLE;
    }

    // Constructors, getters...
}

// BookMapper.java - Maps between layers
@Component
public class BookMapper {
    private final AuthorMapper authorMapper;

    public Book toDomain(BookEntity entity) {
        return new Book(
            entity.getId(),
            entity.getTitle(),
            authorMapper.toDomain(entity.getAuthor()),
            entity.getStatus(),
            entity.getShelfId(),
            entity.getCheckedOutAt(),
            entity.getDueDate()
        );
    }

    public BookEntity toEntity(Book domain) {
        BookEntity entity = new BookEntity();
        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setStatus(domain.getStatus());
        entity.setShelfId(domain.getShelfId());
        entity.setCheckedOutAt(domain.getCheckedOutAt());
        entity.setDueDate(domain.getDueDate());
        // Author mapped separately
        return entity;
    }
}
```

### Recommendation for Your Project

**Use Option 1** (Entities Directly) because:
- Your app is primarily CRUD operations
- No complex business rules in current code
- Simpler to maintain
- Less mapping overhead
- Faster development

**Only use Option 2** if:
- You have complex business logic
- You need to support multiple databases
- You're building a hexagonal/clean architecture
- Your domain model differs significantly from database schema

### Fix Type Mismatch (Critical)

Regardless of which option you choose, fix the enum vs String issue:

```java
// ❌ WRONG - String allows invalid values
@Entity
public class BookEntity {
    private String bookStatus;  // Can be "BANANA" 🍌
}

// ✅ CORRECT - Enum enforces valid values
@Entity
public class BookEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BookStatus status;  // Can only be AVAILABLE, CHECKED_OUT, etc.
}

// Migration script:
ALTER TABLE books
ADD COLUMN status VARCHAR(20);

UPDATE books
SET status = CASE book_status
    WHEN 'CHECKED_OUT' THEN 'CHECKED_OUT'
    WHEN 'AVAILABLE' THEN 'AVAILABLE'
    ELSE 'AVAILABLE'
END;

ALTER TABLE books
DROP COLUMN book_status;

ALTER TABLE books
ALTER COLUMN status SET NOT NULL;

ALTER TABLE books
ADD CONSTRAINT book_status_check
CHECK (status IN ('AVAILABLE', 'CHECKED_OUT', 'RESERVED'));
```

### Learning Principle

> **Choose your layering strategy and stick with it.** For simple CRUD apps, using entities directly is fine and reduces complexity. For complex business logic, separate domain from persistence. Never mix: domain models should NOT reference JPA entities. Always use enums for fixed sets of values, not Strings.

### Action Items

1. ✅ **Decision:** Choose Option 1 or Option 2 based on your needs
2. ✅ **If Option 1:** Delete `Book.java`, rename `BookEntity` to `Book`
3. ✅ **If Option 2:** Create mappers, separate entities (package-private)
4. ✅ **Either way:** Fix `String bookStatus` → `BookStatus status` (enum)
5. ✅ Update database schema
6. ✅ Update all references in services

**Estimated Fix Time:** 3 hours (includes database migration)

---

## 🟡 Issue 2.6: Dead Code and Empty Classes

**Priority:** MEDIUM
**Locations:** Multiple files

**Current Code:**

```java
// CatalogService.java
@Service
public class CatalogService {
    // Completely empty - 0 lines of code
}

// CatalogEntity.java
@Entity
public class CatalogEntity {
    // Empty
}

// Catalog.java
public class Catalog {
    // Empty
}

// CatalogController.java
@RestController
public class CatalogController {
    // Empty
}

// CatalogRepository.java
public interface CatalogRepository extends JpaRepository<CatalogEntity, Long> {
    // No custom methods
}

// AuthorController.java
@RestController
public class AuthorController {
    // Empty
}

// User.java
public class User {
    // Empty
}
```

### Why It's Problematic

1. **Code Clutter:** Empty classes confuse developers
2. **Misleading:** Suggests features exist when they don't
3. **Incomplete Design:** Indicates abandoned or half-finished work
4. **Compilation Overhead:** IDE/compiler must process these files
5. **Confusing Documentation:** What's implemented vs planned?

### Analysis

```
Empty/Unused Classes:
├── Catalog (3 files)
│   ├── CatalogService.java
│   ├── CatalogEntity.java
│   ├── Catalog.java
│   ├── CatalogController.java
│   └── CatalogRepository.java
├── AuthorController.java
└── User.java

Questions:
- Are these placeholders for future features?
- Were these started but abandoned?
- Did refactoring leave them behind?
```

### Correct Approach

**Option 1: Delete Dead Code (Recommended)**

```bash
# Remove empty classes
rm src/main/java/com/penrose/bibby/library/catalog/CatalogService.java
rm src/main/java/com/penrose/bibby/library/catalog/CatalogEntity.java
rm src/main/java/com/penrose/bibby/library/catalog/Catalog.java
rm src/main/java/com/penrose/bibby/library/catalog/CatalogController.java
rm src/main/java/com/penrose/bibby/library/catalog/CatalogRepository.java
rm src/main/java/com/penrose/bibby/library/author/AuthorController.java
rm src/main/java/com/penrose/bibby/library/user/User.java

# Remove empty directory
rmdir src/main/java/com/penrose/bibby/library/catalog

# Git will remember if you need to recover
git commit -m "Remove dead code and empty classes"
```

**Option 2: Mark as TODO (If Genuinely Planned)**

If these are real features you plan to implement:

```java
/**
 * Catalog functionality - PLANNED FOR PHASE 2
 *
 * <p>A catalog organizes books by category, genre, or topic.
 * This feature is not yet implemented.
 *
 * <p>See: https://github.com/yourorg/bibby/issues/42
 *
 * @author Your Name
 * @since 2.0 (planned)
 */
@Service
public class CatalogService {

    /**
     * Placeholder - to be implemented.
     * @throws UnsupportedOperationException always (not yet implemented)
     */
    public List<Catalog> getAllCatalogs() {
        throw new UnsupportedOperationException(
            "Catalog functionality not yet implemented. See issue #42"
        );
    }
}
```

**Option 3: Implement Properly (If Needed Now)**

If you actually need this feature:

```java
// CatalogEntity.java
@Entity
@Table(name = "catalogs")
@Data
@EqualsAndHashCode(of = "catalogId")
public class CatalogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long catalogId;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @ManyToMany
    @JoinTable(
        name = "catalog_books",
        joinColumns = @JoinColumn(name = "catalog_id"),
        inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private Set<BookEntity> books = new HashSet<>();

    @CreatedDate
    private LocalDateTime createdAt;
}

// CatalogService.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {
    private final CatalogRepository catalogRepository;

    public List<CatalogEntity> getAllCatalogs() {
        return catalogRepository.findAll();
    }

    public Optional<CatalogEntity> findById(Long id) {
        return catalogRepository.findById(id);
    }

    @Transactional
    public CatalogEntity createCatalog(String name, String description) {
        CatalogEntity catalog = new CatalogEntity();
        catalog.setName(name);
        catalog.setDescription(description);
        return catalogRepository.save(catalog);
    }

    @Transactional
    public void addBookToCatalog(Long catalogId, Long bookId) {
        // Implementation
    }
}

// CatalogController.java
@RestController
@RequestMapping("/api/v1/catalogs")
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<List<CatalogEntity>> getAllCatalogs() {
        return ResponseEntity.ok(catalogService.getAllCatalogs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CatalogEntity> getCatalog(@PathVariable Long id) {
        return catalogService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CatalogEntity> createCatalog(
            @RequestBody CatalogRequest request) {
        CatalogEntity catalog = catalogService.createCatalog(
            request.name(),
            request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog);
    }
}
```

### Git: Recovering Deleted Code

If you delete code and later need it:

```bash
# Find when file was deleted
git log --all --full-history -- path/to/File.java

# Restore file from specific commit
git checkout <commit-hash> -- path/to/File.java

# Or restore from previous commit before deletion
git checkout <commit-hash>~1 -- path/to/File.java
```

### Best Practices for TODOs

If you keep placeholder code:

```java
/**
 * @deprecated Not yet implemented. Placeholder for future catalog feature.
 * @see <a href="https://github.com/yourorg/bibby/issues/42">Issue #42</a>
 */
@Deprecated
@Service
public class CatalogService {
    // Empty for now
}

// Or use annotations:
@Component
@Profile("!production")  // Don't load in production
public class CatalogService {
    // Development only
}
```

### Learning Principle

> **Don't commit dead code.** Version control preserves history - you can always recover deleted files. Empty classes suggest poor planning or incomplete work. Either implement features properly, clearly mark them as TODO with tracking links, or delete them. Your codebase should only contain working, valuable code.

### Action Items

1. ✅ Search for empty classes: `find . -name "*.java" -type f -empty`
2. ✅ Identify which are truly unused vs planned features
3. ✅ Delete dead code or implement properly
4. ✅ If keeping TODOs, add clear documentation and issue tracker links
5. ✅ Update README with feature roadmap

**Estimated Fix Time:** 30 minutes (deletion) or 4+ hours (implementation)

---

## 🟡 Issue 2.7: Constructor Parameter Ignored

**Priority:** MEDIUM
**Location:** `BookController.java:13-17`

**Current Code:**

```java
// BookController.java
@RestController
public class BookController {
    final BookService bookService;
    final AuthorRepository authorRepository;

    public BookController(BookService bookService,
                         AuthorRepository authorRepository,
                         BookRepository bookRepository) {  // ❌ Declared but never used!
        this.bookService = bookService;
        this.authorRepository = authorRepository;
        // bookRepository never assigned!
    }
}
```

### Why It's Problematic

1. **Confusing:** Parameter declared but never used
2. **Wasted Resources:** Spring creates `bookRepository` bean unnecessarily
3. **Incomplete Implementation:** Suggests unfinished refactoring
4. **IDE Warnings:** Modern IDEs flag this as a problem
5. **Code Smell:** Dead parameters indicate poor code quality

### What Happened

```java
// Likely started as:
public BookController(BookService bookService,
                     AuthorRepository authorRepository,
                     BookRepository bookRepository) {
    this.bookService = bookService;
    this.authorRepository = authorRepository;
    this.bookRepository = bookRepository;  // Used bookRepository
}

// Then refactored to use service instead:
public BookController(BookService bookService, ...) {
    this.bookService = bookService;  // Service now handles repository
    // Forgot to remove bookRepository parameter!
}
```

### Correct Approach

**Option 1: Remove Unused Parameter**

```java
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final AuthorRepository authorRepository;

    // Lombok generates constructor with only these two fields
    // No unused parameters!
}
```

**Option 2: Use the Parameter (If Needed)**

```java
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;  // Now used

    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> bookExists(@PathVariable Long id) {
        boolean exists = bookRepository.existsById(id);
        return ResponseEntity.ok(exists);
    }
}
```

**Better Yet: Use Service Instead of Repository**

Controllers should NEVER directly use repositories:

```java
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {
    // ✅ Only inject services, not repositories
    private final BookQueryService bookQueryService;
    private final BookCommandService bookCommandService;

    @GetMapping("/{id}")
    public ResponseEntity<BookEntity> getBook(@PathVariable Long id) {
        return bookQueryService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BookEntity> createBook(
            @Valid @RequestBody BookRequest request) {
        BookEntity book = bookCommandService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }
}
```

### Finding Unused Parameters

```bash
# Using IntelliJ IDEA:
# Code → Inspect Code → Unused declaration → Unused parameter

# Using Maven:
mvn pmd:check

# Using SonarLint:
# Automatic highlighting in IDE
```

### Learning Principle

> **Pay attention to IDE warnings.** Unused parameters are code smells indicating incomplete refactoring or poor design. Controllers should depend on services, not repositories directly. Use static analysis tools (SonarLint, PMD, SpotBugs) to catch these issues early.

### Action Items

1. ✅ Remove unused `bookRepository` parameter from `BookController`
2. ✅ Review controller - should it use repositories directly? (Answer: No)
3. ✅ Refactor to use services instead of repositories
4. ✅ Enable static analysis in IDE (SonarLint)
5. ✅ Run `mvn clean compile` and fix all warnings

**Estimated Fix Time:** 5 minutes

---

## 🟡 Issue 2.8: Commented-Out Code

**Priority:** LOW
**Locations:** Multiple files

**Current Code:**

```java
// BookService.java - Line 32
//            authorEntity = AuthorMapper.toEntity(new Author(firstName,lastName));

// BookCommands.java - Lines 36-40
//    final BookCommands bookCommands;
//    final ShelfService shelfService;
//    final BookService bookService;
//    final BookcaseService bookcaseService;

// BookCommands.java - Lines 120-125
//        System.out.println("Book Title: " + title);
//        System.out.println("Author: " + authorFullName[0] + " " + authorFullName[1]);
//        BookRequestDTO bookRequestDTO = new BookRequestDTO(title,authorFullName[0], authorFullName[1]);

// BookCommands.java - Lines 186-189
//        for (ShelfSummary s : shelfSummaries) {
//            System.out.println(s.shelfLabel() + " " + s.bookcaseLabel());
//        }
```

### Why It's Problematic

1. **Clutters Codebase:** Makes files harder to read
2. **Confusing:** Is this code needed or not?
3. **Version Control:** Git already keeps history - no need for commented code
4. **Misleading:** Suggests uncertainty or incomplete work
5. **Maintenance:** Commented code doesn't evolve with rest of codebase

### Correct Approach

**Delete It!**

```java
// ❌ DON'T keep commented code
public void someMethod() {
    doSomething();
    // Old implementation:
    // doSomethingElse();
    // callAnotherMethod();
    // return result;
}

// ✅ Just delete it
public void someMethod() {
    doSomething();
}
```

**If You Need It Later:**

```bash
# Git remembers everything!
git log -p -- path/to/File.java

# Find when code was changed
git blame path/to/File.java

# Restore old version
git checkout <commit-hash> -- path/to/File.java
```

**Use TODOs for Future Work:**

```java
// ❌ Commented implementation
// public void futureFeature() {
//     // implementation
// }

// ✅ Clear TODO
/**
 * TODO: Implement author name validation
 * See: https://github.com/yourorg/bibby/issues/45
 */
public void validateAuthorName(String name) {
    throw new UnsupportedOperationException("Not yet implemented");
}
```

### Learning Principle

> **Don't commit commented-out code.** Version control preserves history - you can always recover old code. Commented code clutters files and confuses developers. Use TODOs for planned work, delete everything else. Trust git!

### Action Items

1. ✅ Search for commented code: `grep -r "^[[:space:]]*//" src/`
2. ✅ Delete all commented-out code
3. ✅ Replace with TODOs if representing planned work
4. ✅ Commit with message: "Remove commented-out code"

**Estimated Fix Time:** 15 minutes

---

## 📊 Summary Table

| Issue | Priority | Location | Fix Time | Impact |
|-------|----------|----------|----------|--------|
| Null repository dependency | 🔴 Critical | AuthorService.java | 5 min | Prevents crashes |
| SRP violation | 🟠 High | BookService.java | 3 hours | Better architecture |
| Tight coupling | 🟠 High | BookCommands.java | 2 hours | Easier testing |
| Duplicate repository methods | 🟠 High | BookRepository.java | 30 min | Clarity |
| Domain vs Entity confusion | 🟡 Medium | Book/BookEntity | 3 hours | Type safety |
| Dead code | 🟡 Medium | Catalog classes | 30 min | Cleaner codebase |
| Unused constructor parameter | 🟡 Medium | BookController.java | 5 min | Clean code |
| Commented code | 🟡 Low | Multiple files | 15 min | Readability |

**Total Estimated Time:** ~10 hours
**Expected Impact:** Much cleaner architecture, easier to test and maintain

---

## ✅ Action Checklist

### Critical (Do Immediately)
- [ ] Fix `AuthorService` null dependency (5 min)

### High Priority (This Week)
- [ ] Split `BookService` into Command/Query/Checkout services (3 hours)
- [ ] Create `LibraryManagementFacade` (2 hours)
- [ ] Clean up duplicate repository methods (30 min)

### Medium Priority (This Month)
- [ ] Fix domain vs entity model strategy (3 hours)
- [ ] Delete dead code or implement properly (30 min - 4 hours)
- [ ] Remove unused constructor parameters (5 min)

### Low Priority (When You Have Time)
- [ ] Delete all commented-out code (15 min)

---

**Recommended Next Section:**
- Section 3: Naming & Readability
- Section 4: Error Handling & Robustness (includes transaction fixes)
- Section 5: Performance & Efficiency
- Section 7: Security Concerns

Which would you like next?
