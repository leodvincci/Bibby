# Section 29: Testing Anti-Patterns - What NOT to Do

## Overview

While we've covered testing best practices extensively, it's equally important to recognize **testing anti-patterns** - common mistakes that make tests fragile, slow, unclear, or unmaintainable. These anti-patterns often creep into codebases gradually, degrading test quality over time.

This section examines the most common testing anti-patterns, shows real examples from the Bibby codebase (or potential pitfalls), and provides actionable fixes for each problem.

**Learning Objectives:**
- Recognize test smells and anti-patterns in existing tests
- Understand why certain testing approaches are problematic
- Refactor fragile, brittle tests into maintainable ones
- Avoid common pitfalls when writing new tests
- Balance pragmatism with testing best practices

---

## The Cost of Bad Tests

Before diving into specific anti-patterns, let's understand **why bad tests are worse than no tests**:

### Problems with Poor Tests

1. **False Confidence**
   - Tests pass but bugs still reach production
   - Coverage metrics high, but quality low
   - Team believes code is well-tested when it isn't

2. **Maintenance Burden**
   - Tests break with every refactoring
   - More time spent fixing tests than writing code
   - Developers start ignoring or deleting tests

3. **Slow Feedback**
   - Test suite takes hours to run
   - Developers stop running tests locally
   - CI/CD pipeline becomes bottleneck

4. **Knowledge Loss**
   - Tests don't document behavior
   - Unclear what tests are actually testing
   - Hard to understand failures

**The Pragmatic Programmer's Testing Principle:**
> "Tests are code too. They deserve the same care, design, and refactoring as production code."

---

## Anti-Pattern #1: Testing Implementation Details

### The Problem

**Definition:** Tests that verify *how* something works internally rather than *what* behavior it provides.

**Why It's Bad:**
- Tests break when refactoring internal implementation
- Couples tests tightly to code structure
- Makes refactoring expensive and risky
- Doesn't verify actual user-facing behavior

### Bibby Example: Bad - Testing Internal State

```java
// ❌ BAD: Testing implementation details
@Test
void checkOutBook_setsInternalStatusField() {
    // Arrange
    Book book = new Book(
        new ISBN("978-0-13-468599-1"),
        new Title("The Pragmatic Programmer"),
        new Publisher("Addison-Wesley"),
        List.of(new Author("David Thomas", "Hunt"))
    );

    // Act
    book.checkOut();

    // Assert - testing internal field directly
    Field statusField = Book.class.getDeclaredField("status");
    statusField.setAccessible(true);
    AvailabilityStatus actualStatus = (AvailabilityStatus) statusField.get(book);

    assertThat(actualStatus).isEqualTo(AvailabilityStatus.CHECKED_OUT);
}
```

**Problems:**
1. Uses reflection to access private field
2. Test breaks if field is renamed
3. Test breaks if status is calculated instead of stored
4. Doesn't verify actual behavior - can the book be checked out again?

### Refactored: Good - Testing Behavior

```java
// ✅ GOOD: Testing observable behavior
@Test
void checkOutBook_makesBookUnavailableForCheckout() {
    // Arrange
    Book book = TestBooks.availableBook();

    // Act
    book.checkOut();

    // Assert - verify behavior through public API
    assertThat(book.isAvailable()).isFalse();

    // Verify the consequence - can't check out again
    assertThatThrownBy(() -> book.checkOut())
        .isInstanceOf(BookNotAvailableException.class)
        .hasMessage("Book is already checked out");
}
```

**Improvements:**
1. Tests only through public API (`isAvailable()`)
2. Verifies actual behavior - book can't be checked out twice
3. Won't break if internal implementation changes
4. Documents the real business rule

### Real Bibby Code Analysis

Let's look at `BookEntity.java`:

```java
public class BookEntity {
    private String bookStatus;  // String instead of enum - smell!

    public String getBookStatus() {
        return bookStatus;
    }

    public void setBookStatus(String bookStatus) {
        this.bookStatus = bookStatus;
    }
}
```

**Current Test Smell:**
```java
// ❌ Likely existing test in Bibby
@Test
void testCheckOut() {
    BookEntity book = new BookEntity();
    book.setBookStatus("AVAILABLE");

    bookService.checkOutBook(book);

    assertEquals("CHECKED_OUT", book.getBookStatus());  // Testing internal string
}
```

**Problems:**
1. Tests that status string equals "CHECKED_OUT"
2. Doesn't verify business rules (can book be checked out twice?)
3. Fragile - breaks if status representation changes
4. Doesn't test edge cases

**Better Approach:**
```java
// ✅ Test behavior, not internal state
@Test
void checkOutBook_preventsDoubleCheckOut() {
    // Arrange
    Book book = TestBooks.availableBook();

    // Act
    CheckOutResult firstCheckOut = checkOutService.checkOut(book.getId());

    // Assert
    assertThat(firstCheckOut.isSuccess()).isTrue();

    // Try to check out again
    CheckOutResult secondCheckOut = checkOutService.checkOut(book.getId());
    assertThat(secondCheckOut.isSuccess()).isFalse();
    assertThat(secondCheckOut.getErrorMessage())
        .contains("already checked out");
}
```

---

## Anti-Pattern #2: Over-Mocking (Mock Everything)

### The Problem

**Definition:** Using mocks for everything, even simple value objects or internal collaborators.

**Why It's Bad:**
- Tests don't verify real object interactions
- Gives false confidence - mocks might not match reality
- Brittle tests that break with refactoring
- Misses integration bugs
- Makes tests harder to understand

### The Mock Spectrum

```
Real Objects ←-------------- Use This Spectrum --------------→ Mocks
              Value Objects    Simple Objects    Complex Objects    External Systems
              ISBN, Title      Book, Author      BookService        Database, Email
              ✅ Never Mock    ✅ Rarely Mock    ⚠️ Sometimes      ✅ Always Mock
```

### Bibby Example: Bad - Over-Mocking

```java
// ❌ BAD: Mocking everything, even value objects
@ExtendWith(MockitoExtension.class)
class CheckOutBookServiceTest {
    @Mock private BookRepository bookRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private Book book;  // ❌ Don't mock domain objects!
    @Mock private ISBN isbn;  // ❌ Don't mock value objects!
    @Mock private Title title;  // ❌ Don't mock value objects!

    @InjectMocks
    private CheckOutBookService checkOutService;

    @Test
    void checkOut_createsLoan() {
        // Arrange - look at all this mock setup!
        when(isbn.getValue()).thenReturn("978-0-13-468599-1");
        when(title.getValue()).thenReturn("The Pragmatic Programmer");
        when(book.getIsbn()).thenReturn(isbn);
        when(book.getTitle()).thenReturn(title);
        when(book.isAvailable()).thenReturn(true);

        when(bookRepository.findById(any())).thenReturn(Optional.of(book));

        // Act
        checkOutService.checkOut(1L, "user@example.com");

        // Assert
        verify(loanRepository).save(any(Loan.class));
    }
}
```

**Problems:**
1. Mocks `Book` - a domain object that should have real behavior
2. Mocks `ISBN` and `Title` - simple value objects
3. Doesn't verify actual `Book` behavior (status changes, validation)
4. Brittle - breaks if `Book` getters are renamed
5. Doesn't test `Book.checkOut()` logic at all

### Refactored: Good - Use Real Objects Where Possible

```java
// ✅ GOOD: Use real domain objects, mock only infrastructure
@ExtendWith(MockitoExtension.class)
class CheckOutBookServiceTest {
    @Mock private BookRepository bookRepository;
    @Mock private LoanRepository loanRepository;

    @InjectMocks
    private CheckOutBookService checkOutService;

    @Test
    void checkOut_createsLoanAndMarksBookCheckedOut() {
        // Arrange - use real domain objects!
        Book book = TestBooks.availableBook()
            .withIsbn("978-0-13-468599-1")
            .withTitle("The Pragmatic Programmer")
            .build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        // Act
        CheckOutResult result = checkOutService.checkOut(1L, "user@example.com");

        // Assert - verify behavior on real object
        assertThat(result.isSuccess()).isTrue();
        assertThat(book.isAvailable()).isFalse();  // Real Book behavior!

        verify(loanRepository).save(argThat(loan ->
            loan.getBookId() == 1L &&
            loan.getUserEmail().equals("user@example.com") &&
            loan.getStatus() == LoanStatus.ACTIVE
        ));
    }
}
```

**Improvements:**
1. Uses real `Book` domain object - tests actual business logic
2. Uses real `ISBN` and `Title` value objects - cheap to create
3. Mocks only infrastructure (`BookRepository`, `LoanRepository`)
4. Verifies real domain behavior (`book.isAvailable()`)
5. Less brittle - survives refactoring

### Rule of Thumb: What to Mock

```java
// ✅ ALWAYS use real objects
Value Objects (ISBN, Title, Author, Publisher)
Domain Entities (Book, Shelf, Bookcase)
Simple DTOs and Data Structures

// ✅ ALWAYS mock
External Systems (Database, HTTP clients, Email)
Infrastructure (Repositories, External APIs)
Slow Operations (File I/O, Network calls)

// ⚠️ CONTEXT-DEPENDENT
Domain Services - mock if complex, use real if simple
```

---

## Anti-Pattern #3: Fragile Tests (Change Detector Tests)

### The Problem

**Definition:** Tests that break whenever any implementation detail changes, even if behavior is correct.

**Why It's Bad:**
- Refactoring becomes expensive - hundreds of tests break
- Developers stop refactoring to avoid breaking tests
- Tests become maintenance burden instead of safety net
- Team loses confidence in tests

### Bibby Example: Bad - Fragile Test

```java
// ❌ BAD: Overly specific assertions that break easily
@Test
void addBook_savesBookWithAllFields() {
    // Arrange
    BookDTO bookDTO = new BookDTO(
        "978-0-13-468599-1",
        "The Pragmatic Programmer",
        "Addison-Wesley",
        List.of("David Thomas", "Andrew Hunt"),
        "1999-10-20"
    );

    // Act
    bookService.addBook(bookDTO);

    // Assert - every single field, even computed ones!
    verify(bookRepository).save(argThat(book ->
        book.getIsbn().getValue().equals("978-0-13-468599-1") &&
        book.getTitle().getValue().equals("The Pragmatic Programmer") &&
        book.getPublisher().getValue().equals("Addison-Wesley") &&
        book.getAuthors().size() == 2 &&
        book.getAuthors().get(0).getFirstName().equals("David") &&
        book.getAuthors().get(0).getLastName().equals("Thomas") &&
        book.getAuthors().get(1).getFirstName().equals("Andrew") &&
        book.getAuthors().get(1).getLastName().equals("Hunt") &&
        book.getPublicationDate().getYear() == 1999 &&
        book.getPublicationDate().getMonthValue() == 10 &&
        book.getPublicationDate().getDayOfMonth() == 20 &&
        book.getStatus() == AvailabilityStatus.AVAILABLE &&
        book.getCreatedAt() != null &&  // ❌ Fragile!
        book.getUpdatedAt() != null &&  // ❌ Fragile!
        book.getId() == null  // ❌ Breaks if ID generation changes!
    ));
}
```

**Problems:**
1. Asserts every single field, including timestamps
2. Verifies computed values (full name splitting)
3. Breaks if default status changes
4. Breaks if ID generation strategy changes
5. Hard to read - what's the actual important assertion?

### Refactored: Good - Test Essential Behavior

```java
// ✅ GOOD: Test essential behavior, ignore implementation details
@Test
void addBook_savesPragmaticProgrammerWithCorrectISBN() {
    // Arrange
    BookDTO bookDTO = BookDTOs.pragmaticProgrammer();

    // Act
    Book savedBook = bookService.addBook(bookDTO);

    // Assert - only essential business rules
    assertThat(savedBook.getIsbn().getValue())
        .isEqualTo("978-0-13-468599-1");
    assertThat(savedBook.isAvailable()).isTrue();

    verify(bookRepository).save(any(Book.class));
}

@Test
void addBook_savesAllAuthors() {
    // Arrange
    BookDTO bookDTO = BookDTOs.bookWithMultipleAuthors(
        "David Thomas", "Andrew Hunt"
    );

    // Act
    Book savedBook = bookService.addBook(bookDTO);

    // Assert - specific to this test's concern
    assertThat(savedBook.getAuthors())
        .extracting(Author::getFullName)
        .containsExactly("David Thomas", "Andrew Hunt");
}
```

**Improvements:**
1. Separates concerns into focused tests
2. Ignores timestamps and IDs
3. Tests essential business rules only
4. Won't break if implementation details change
5. Clear test names document what matters

### Another Example: Fragile Verification Order

```java
// ❌ BAD: Tests exact call order (fragile!)
@Test
void checkOut_callsRepositoriesInOrder() {
    Book book = TestBooks.availableBook();
    when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

    checkOutService.checkOut(1L, "user@example.com");

    InOrder inOrder = inOrder(bookRepository, loanRepository, emailService);
    inOrder.verify(bookRepository).findById(1L);
    inOrder.verify(loanRepository).save(any());
    inOrder.verify(emailService).sendConfirmation(any());
    // ❌ Breaks if we optimize by calling save async!
}

// ✅ GOOD: Tests that all necessary calls happened
@Test
void checkOut_savesLoanAndSendsConfirmation() {
    Book book = TestBooks.availableBook();
    when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

    checkOutService.checkOut(1L, "user@example.com");

    verify(loanRepository).save(any(Loan.class));
    verify(emailService).sendConfirmation(argThat(email ->
        email.getTo().equals("user@example.com") &&
        email.getSubject().contains("Check out confirmation")
    ));
    // ✅ Order doesn't matter, only that both happened
}
```

---

## Anti-Pattern #4: Test Interdependence (No Test Isolation)

### The Problem

**Definition:** Tests that depend on other tests running first, or share mutable state.

**Why It's Bad:**
- Tests fail randomly depending on run order
- Can't run tests in parallel
- Can't run single test in isolation
- Debugging failures is nightmare
- CI/CD becomes unreliable

### Bibby Example: Bad - Shared State

```java
// ❌ BAD: Tests share mutable static state
class BookServiceTest {
    private static BookRepository bookRepository;
    private static BookService bookService;

    @BeforeAll
    static void setUp() {
        bookRepository = new InMemoryBookRepository();
        bookService = new BookService(bookRepository);
    }

    @Test
    void test1_addBook() {
        Book book = TestBooks.pragmaticProgrammer();
        bookService.add(book);

        assertThat(bookRepository.count()).isEqualTo(1);
    }

    @Test
    void test2_findBook() {
        // ❌ Depends on test1 running first!
        Optional<Book> book = bookService.findByIsbn("978-0-13-468599-1");

        assertThat(book).isPresent();
    }

    @Test
    void test3_checkOutBook() {
        // ❌ Depends on test1 AND test2!
        bookService.checkOut("978-0-13-468599-1", "user@example.com");

        Book book = bookService.findByIsbn("978-0-13-468599-1").get();
        assertThat(book.isAvailable()).isFalse();
    }
}
```

**Problems:**
1. Tests must run in specific order (test1 → test2 → test3)
2. Can't run `test2` or `test3` alone
3. If `test1` fails, all others fail mysteriously
4. Can't run in parallel
5. Test names have numbers (code smell!)

### Refactored: Good - Isolated Tests

```java
// ✅ GOOD: Each test is completely independent
@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    private BookRepository bookRepository;
    private BookService bookService;

    @BeforeEach  // ✅ Fresh state for EACH test
    void setUp() {
        bookRepository = new InMemoryBookRepository();
        bookService = new BookService(bookRepository);
    }

    @Test
    void addBook_savesToRepository() {
        // Arrange - complete setup in THIS test
        Book book = TestBooks.pragmaticProgrammer();

        // Act
        bookService.add(book);

        // Assert
        assertThat(bookRepository.count()).isEqualTo(1);
    }

    @Test
    void findByIsbn_returnsBookWhenExists() {
        // Arrange - THIS test sets up what it needs
        Book book = TestBooks.pragmaticProgrammer();
        bookRepository.save(book);

        // Act
        Optional<Book> found = bookService.findByIsbn("978-0-13-468599-1");

        // Assert
        assertThat(found).isPresent();
    }

    @Test
    void checkOut_marksBookUnavailable() {
        // Arrange - THIS test sets up what it needs
        Book book = TestBooks.availableBook();
        bookRepository.save(book);

        // Act
        bookService.checkOut(book.getIsbn().getValue(), "user@example.com");

        // Assert
        Book updated = bookRepository.findByIsbn(book.getIsbn()).get();
        assertThat(updated.isAvailable()).isFalse();
    }
}
```

**Improvements:**
1. `@BeforeEach` creates fresh state for every test
2. Each test can run independently
3. Tests can run in any order
4. Can run in parallel
5. Clear, descriptive test names

### Real Database Tests - Common Mistake

```java
// ❌ BAD: Tests share database state
@SpringBootTest
@Testcontainers
class BookRepositoryIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private BookRepository bookRepository;

    @Test
    void test1_save() {
        Book book = TestBooks.pragmaticProgrammer();
        bookRepository.save(book);

        assertThat(bookRepository.count()).isEqualTo(1);
    }

    @Test
    void test2_findAll() {
        // ❌ Depends on test1's data!
        List<Book> books = bookRepository.findAll();
        assertThat(books).hasSize(1);
    }
}
```

**Fix: Clean Database Between Tests**

```java
// ✅ GOOD: Clean database between tests
@SpringBootTest
@Testcontainers
class BookRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        bookRepository.deleteAll();
        entityManager.clear();  // Clear JPA cache
    }

    @Test
    void save_persistsBookToDatabase() {
        Book book = TestBooks.pragmaticProgrammer();

        Book saved = bookRepository.save(book);
        entityManager.flush();  // Force write to DB
        entityManager.clear();  // Clear cache

        Book found = bookRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getIsbn()).isEqualTo(book.getIsbn());
    }

    @Test
    void findAll_returnsEmptyListWhenNoBooksExist() {
        // This test doesn't depend on other tests!
        List<Book> books = bookRepository.findAll();
        assertThat(books).isEmpty();
    }
}
```

---

## Anti-Pattern #5: Ignoring Test Failures

### The Problem

**Definition:** Leaving broken tests in the codebase, commenting them out, or using `@Disabled` without fixing them.

**Why It's Bad:**
- Broken windows theory - one ignored test leads to many
- Team loses confidence in test suite
- Real bugs get masked
- Technical debt accumulates

### The Slippery Slope

```java
// Week 1: "I'll fix this later"
@Test
@Disabled("Fails sometimes, will investigate")
void checkOut_sendsEmailNotification() {
    // ...
}

// Week 2: "Too busy, mark as disabled"
@Test
@Disabled("Flaky test, needs fixing")
void returnBook_calculatesLateFee() {
    // ...
}

// Week 3: "Just comment it out"
// @Test
// void addBook_validatesDuplicateISBN() {
//     // ...
// }

// Month 2: "Delete the test suite, it's all broken anyway"
```

### The Fix: Zero Tolerance for Broken Tests

**Rule:** Never commit broken tests. Period.

**Options when a test fails:**

1. **Fix the Test** (preferred)
   - Test caught a real bug → fix the bug
   - Test is wrong → fix the test
   - Test is flaky → make it deterministic

2. **Fix the Code**
   - Test is correct, code is wrong
   - This is what tests are FOR!

3. **Delete the Test** (if truly not valuable)
   - Better no test than broken test
   - But ask: why did we write it in the first place?

### Bibby Example: Handling Flaky Tests

```java
// ❌ BAD: "I'll fix this later"
@Test
@Disabled("Sometimes fails in CI, race condition?")
void checkOut_concurrentAccess_onlyOneSucceeds() {
    Book book = TestBooks.availableBook();

    // Try to check out from 2 threads simultaneously
    CompletableFuture<Boolean> checkout1 =
        CompletableFuture.supplyAsync(() -> checkOutService.checkOut(book.getId()));
    CompletableFuture<Boolean> checkout2 =
        CompletableFuture.supplyAsync(() -> checkOutService.checkOut(book.getId()));

    Boolean result1 = checkout1.join();
    Boolean result2 = checkout2.join();

    // ❌ Flaky: sometimes both succeed!
    assertThat(result1 != result2).isTrue();
}
```

**Fix: Make Test Deterministic**

```java
// ✅ GOOD: Fixed race condition with proper synchronization test
@Test
void checkOut_concurrentAccess_onlyOneSucceeds() {
    // Arrange
    Book book = TestBooks.availableBook();
    bookRepository.save(book);

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(2);

    List<CheckOutResult> results = new CopyOnWriteArrayList<>();

    // Create 2 threads that start simultaneously
    Runnable checkOutTask = () -> {
        try {
            startLatch.await();  // Wait for signal
            CheckOutResult result = checkOutService.checkOut(book.getId());
            results.add(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            doneLatch.countDown();
        }
    };

    new Thread(checkOutTask).start();
    new Thread(checkOutTask).start();

    // Act - release both threads simultaneously
    startLatch.countDown();
    boolean finished = doneLatch.await(5, TimeUnit.SECONDS);

    // Assert
    assertThat(finished).isTrue();
    assertThat(results).hasSize(2);

    long successCount = results.stream()
        .filter(CheckOutResult::isSuccess)
        .count();

    assertThat(successCount).isEqualTo(1);  // Exactly one succeeds
}
```

---

## Anti-Pattern #6: Testing Multiple Concerns in One Test

### The Problem

**Definition:** A single test that verifies multiple unrelated behaviors.

**Why It's Bad:**
- Hard to understand what failed when test breaks
- Difficult to debug
- Violates Single Responsibility Principle
- Poor test documentation

### Bibby Example: Bad - Testing Everything

```java
// ❌ BAD: One giant test for entire feature
@Test
void testCompleteBookLifecycle() {
    // Add book
    Book book = new Book(
        new ISBN("978-0-13-468599-1"),
        new Title("The Pragmatic Programmer"),
        new Publisher("Addison-Wesley"),
        List.of(new Author("David Thomas", "Hunt"))
    );
    bookService.add(book);
    assertThat(bookRepository.count()).isEqualTo(1);

    // Check out book
    bookService.checkOut(book.getId(), "user@example.com");
    assertThat(book.isAvailable()).isFalse();

    // Try to check out again - should fail
    assertThatThrownBy(() -> bookService.checkOut(book.getId(), "other@example.com"))
        .isInstanceOf(BookNotAvailableException.class);

    // Return book
    bookService.returnBook(book.getId());
    assertThat(book.isAvailable()).isTrue();

    // Check out again - should work now
    bookService.checkOut(book.getId(), "user2@example.com");
    assertThat(book.isAvailable()).isFalse();

    // Return late
    clock.advance(Duration.ofDays(30));
    ReturnResult result = bookService.returnBook(book.getId());
    assertThat(result.getLateFee()).isGreaterThan(Money.ZERO);

    // Delete book
    bookService.delete(book.getId());
    assertThat(bookRepository.findById(book.getId())).isEmpty();
}
```

**Problems:**
1. If any assertion fails, can't tell which behavior is broken
2. Tests 7+ different behaviors in one test
3. Hard to run/debug individual scenarios
4. Failure message is unclear: "expected true but was false" - which assertion?

### Refactored: Good - One Test Per Behavior

```java
// ✅ GOOD: Separate tests for each concern
@Nested
@DisplayName("Book Lifecycle")
class BookLifecycleTest {

    @Test
    @DisplayName("New book can be added to library")
    void addBook_savesToRepository() {
        Book book = TestBooks.pragmaticProgrammer();

        bookService.add(book);

        assertThat(bookRepository.findById(book.getId()))
            .isPresent();
    }

    @Test
    @DisplayName("Available book can be checked out")
    void checkOut_availableBook_succeeds() {
        Book book = givenAvailableBookInRepository();

        CheckOutResult result = bookService.checkOut(book.getId(), "user@example.com");

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Checked out book cannot be checked out again")
    void checkOut_checkedOutBook_fails() {
        Book book = givenCheckedOutBookInRepository();

        assertThatThrownBy(() ->
            bookService.checkOut(book.getId(), "other@example.com"))
            .isInstanceOf(BookNotAvailableException.class)
            .hasMessageContaining("already checked out");
    }

    @Test
    @DisplayName("Returned book becomes available again")
    void returnBook_makesBookAvailableForCheckout() {
        Book book = givenCheckedOutBookInRepository();

        bookService.returnBook(book.getId());

        CheckOutResult recheckOut = bookService.checkOut(book.getId(), "user2@example.com");
        assertThat(recheckOut.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Late return incurs late fee")
    void returnBook_afterDueDate_calculatesLateFee() {
        Book book = givenOverdueBookInRepository(Duration.ofDays(30));

        ReturnResult result = bookService.returnBook(book.getId());

        assertThat(result.getLateFee())
            .isGreaterThan(Money.ZERO)
            .isEqualTo(Money.dollars(15.00));  // $0.50/day * 30 days
    }

    // Test helper methods
    private Book givenAvailableBookInRepository() {
        Book book = TestBooks.availableBook();
        return bookRepository.save(book);
    }

    private Book givenCheckedOutBookInRepository() {
        Book book = TestBooks.availableBook();
        book.checkOut();
        return bookRepository.save(book);
    }

    private Book givenOverdueBookInRepository(Duration overdueDuration) {
        Book book = TestBooks.availableBook();
        book.checkOut();
        clock.advance(STANDARD_LOAN_PERIOD.plus(overdueDuration));
        return bookRepository.save(book);
    }
}
```

**Improvements:**
1. Each test verifies ONE behavior
2. Clear test names document what's being tested
3. Failures are easy to understand
4. Can run individual scenarios
5. Test helpers reduce duplication

---

## Anti-Pattern #7: Testing the Framework

### The Problem

**Definition:** Tests that verify Spring Boot, JPA, or other framework behavior instead of your code.

**Why It's Bad:**
- Wastes time testing already-tested code
- Doesn't verify your business logic
- Increases maintenance burden
- False sense of security

### Bibby Example: Bad - Testing Spring/JPA

```java
// ❌ BAD: Testing that Spring autowiring works
@SpringBootTest
class BookServiceConfigurationTest {
    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void springContext_autowires_bookService() {
        assertThat(bookService).isNotNull();
    }

    @Test
    void springContext_autowires_bookRepository() {
        assertThat(bookRepository).isNotNull();
    }
}

// ❌ BAD: Testing JPA annotations
@DataJpaTest
class BookEntityJpaTest {
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void bookEntity_canBePersisted() {
        BookEntity book = new BookEntity();
        book.setIsbn("978-0-13-468599-1");
        book.setTitle("The Pragmatic Programmer");

        BookEntity saved = entityManager.persistAndFlush(book);

        assertThat(saved.getId()).isNotNull();  // ❌ Testing JPA generates IDs
    }

    @Test
    void bookEntity_hasOneToManyRelationshipWithAuthors() {
        BookEntity book = new BookEntity();
        AuthorEntity author = new AuthorEntity();
        book.setAuthors(List.of(author));

        entityManager.persistAndFlush(book);

        assertThat(book.getAuthors()).hasSize(1);  // ❌ Testing JPA relationships
    }
}
```

**Problems:**
1. These tests verify Spring/JPA work, not your code
2. If Spring is broken, you have bigger problems
3. Slow integration tests for no value
4. Doesn't test business logic

### Refactored: Good - Test Your Code

```java
// ✅ GOOD: Test business logic, not framework
class BookTest {
    @Test
    void checkOut_availableBook_changesStatusToCheckedOut() {
        Book book = TestBooks.availableBook();

        book.checkOut();

        assertThat(book.getStatus()).isEqualTo(AvailabilityStatus.CHECKED_OUT);
    }

    @Test
    void checkOut_alreadyCheckedOutBook_throwsException() {
        Book book = TestBooks.checkedOutBook();

        assertThatThrownBy(() -> book.checkOut())
            .isInstanceOf(BookNotAvailableException.class);
    }
}

// ✅ GOOD: Integration test for actual business behavior
@SpringBootTest
@Testcontainers
class BookRepositoryIntegrationTest {
    @Test
    void findByIsbn_returnsBookWithAllAuthors() {
        // This tests YOUR query logic, not JPA
        Book saved = bookRepository.save(
            TestBooks.bookWithMultipleAuthors("Thomas", "Hunt")
        );

        Optional<Book> found = bookRepository.findByIsbn(saved.getIsbn());

        assertThat(found)
            .isPresent()
            .get()
            .extracting(Book::getAuthors)
            .asList()
            .hasSize(2);
    }
}
```

---

## Anti-Pattern #8: Slow Test Suite

### The Problem

**Definition:** Test suite takes many minutes or hours to run, slowing down development.

**Why It's Bad:**
- Developers stop running tests locally
- Feedback loop becomes hours instead of seconds
- Refactoring becomes expensive
- CI/CD pipeline slows down

### The Test Speed Hierarchy

```
Milliseconds     │ Unit tests (pure logic)
                 │ Value object tests
─────────────────┼─────────────────────────
Seconds          │ Service tests with mocks
                 │ In-memory repository tests
─────────────────┼─────────────────────────
Seconds-Minutes  │ @SpringBootTest (partial context)
                 │ @DataJpaTest with H2
─────────────────┼─────────────────────────
Minutes          │ @SpringBootTest (full context)
                 │ Testcontainers integration tests
─────────────────┼─────────────────────────
Hours            │ End-to-end tests
                 │ Performance tests

Target: 70% in milliseconds, 20% in seconds, 10% in minutes
```

### Bibby Example: Bad - Slow Test

```java
// ❌ BAD: Boots entire Spring context for simple logic test
@SpringBootTest  // ❌ Takes 5-10 seconds to start
class IsbnValidationTest {
    @Autowired
    private BookService bookService;

    @Test
    void addBook_invalidIsbn_throwsException() {
        BookDTO book = new BookDTO();
        book.setIsbn("invalid");

        assertThatThrownBy(() -> bookService.addBook(book))
            .isInstanceOf(InvalidIsbnException.class);
    }
}
```

**Problems:**
1. Boots entire Spring context (5-10 seconds)
2. Testing simple validation logic
3. If you have 100 such tests, suite takes 8-16 minutes!

### Refactored: Good - Fast Unit Test

```java
// ✅ GOOD: Pure unit test, runs in milliseconds
class IsbnTest {
    @Test
    void constructor_invalidFormat_throwsException() {
        assertThatThrownBy(() -> new ISBN("invalid"))
            .isInstanceOf(InvalidIsbnException.class)
            .hasMessageContaining("ISBN must be 13 digits");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "123",           // Too short
        "12345678901234", // Too long
        "978-0-13-ABC",   // Letters
        ""                // Empty
    })
    void constructor_invalidFormats_throwsException(String invalidIsbn) {
        assertThatThrownBy(() -> new ISBN(invalidIsbn))
            .isInstanceOf(InvalidIsbnException.class);
    }
}
// Runs in < 10ms!
```

### Optimization Strategy for Bibby

**Current Problem:** If Bibby has many `@SpringBootTest` tests, suite is slow.

**Solution: Test Pyramid**

```java
// Layer 1: Fast unit tests (70% of tests)
// test/java/com/penrose/bibby/library/book/IsbnTest.java
class IsbnTest {
    // Pure value object logic - milliseconds
}

class BookTest {
    // Domain entity logic - milliseconds
}

// Layer 2: Service tests with mocks (20% of tests)
@ExtendWith(MockitoExtension.class)
class CheckOutBookServiceTest {
    @Mock private BookRepository bookRepository;
    @Mock private LoanRepository loanRepository;
    // Runs in milliseconds
}

// Layer 3: Integration tests (10% of tests)
@DataJpaTest  // ✅ Lighter than @SpringBootTest
class BookRepositoryTest {
    // Test actual DB queries - runs in seconds
}

@SpringBootTest  // ✅ Only for true integration tests
@Testcontainers
class CheckOutIntegrationTest {
    // Full end-to-end flow - runs in seconds to minutes
}
```

### Measuring Test Speed

```bash
# Run tests with timing
./mvnw test -Dsurefire.printSummary=true

# Identify slow tests
./mvnw test -Dtest=BookServiceTest -Dmaven.test.failure.ignore=true

# Goal: Each test class runs in < 1 second
```

---

## Anti-Pattern #9: Not Testing Edge Cases

### The Problem

**Definition:** Only testing the "happy path" and ignoring error conditions, null values, empty collections, etc.

**Why It's Bad:**
- Most bugs occur at boundaries
- Production failures from unexpected inputs
- Poor error handling

### The Edge Cases You Must Test

```java
// For every method, test:
1. Happy path (expected inputs)
2. Null inputs
3. Empty collections
4. Boundary values (0, -1, MAX_VALUE)
5. Invalid inputs
6. Exception scenarios
7. Concurrent access
```

### Bibby Example: Bad - Only Happy Path

```java
// ❌ BAD: Only tests the happy path
class BookServiceTest {
    @Test
    void addBook_validBook_savesToRepository() {
        Book book = TestBooks.pragmaticProgrammer();

        bookService.add(book);

        assertThat(bookRepository.count()).isEqualTo(1);
    }
}
```

**What's missing:**
- What if book is null?
- What if ISBN already exists?
- What if ISBN is invalid?
- What if title is empty?
- What if authors list is empty?

### Refactored: Good - Comprehensive Edge Cases

```java
// ✅ GOOD: Tests happy path AND edge cases
@Nested
@DisplayName("addBook()")
class AddBookTest {

    @Test
    @DisplayName("Valid book is saved to repository")
    void validBook_savedSuccessfully() {
        Book book = TestBooks.pragmaticProgrammer();

        bookService.add(book);

        assertThat(bookRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Null book throws IllegalArgumentException")
    void nullBook_throwsException() {
        assertThatThrownBy(() -> bookService.add(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Book cannot be null");
    }

    @Test
    @DisplayName("Duplicate ISBN throws DuplicateIsbnException")
    void duplicateIsbn_throwsException() {
        Book book = TestBooks.pragmaticProgrammer();
        bookService.add(book);

        Book duplicate = TestBooks.aBook()
            .withIsbn("978-0-13-468599-1")  // Same ISBN
            .withTitle("Different Title")
            .build();

        assertThatThrownBy(() -> bookService.add(duplicate))
            .isInstanceOf(DuplicateIsbnException.class);
    }

    @Test
    @DisplayName("Book with empty title throws ValidationException")
    void emptyTitle_throwsException() {
        Book book = TestBooks.aBook()
            .withTitle("")
            .build();

        assertThatThrownBy(() -> bookService.add(book))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Title cannot be empty");
    }

    @Test
    @DisplayName("Book with no authors throws ValidationException")
    void noAuthors_throwsException() {
        Book book = TestBooks.aBook()
            .withAuthors(Collections.emptyList())
            .build();

        assertThatThrownBy(() -> bookService.add(book))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Book must have at least one author");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("Book with whitespace-only title throws ValidationException")
    void whitespaceTitle_throwsException(String whitespace) {
        Book book = TestBooks.aBook()
            .withTitle(whitespace)
            .build();

        assertThatThrownBy(() -> bookService.add(book))
            .isInstanceOf(ValidationException.class);
    }
}
```

---

## Anti-Pattern #10: Copy-Paste Test Code

### The Problem

**Definition:** Duplicating test setup code across multiple test classes.

**Why It's Bad:**
- Violates DRY principle
- Hard to maintain - change one thing, fix 50 tests
- Obscures what's actually being tested

### Bibby Example: Bad - Duplicated Setup

```java
// In BookServiceTest.java
class BookServiceTest {
    private Book createTestBook() {
        return new Book(
            new ISBN("978-0-13-468599-1"),
            new Title("The Pragmatic Programmer"),
            new Publisher("Addison-Wesley"),
            List.of(new Author("David", "Thomas"))
        );
    }
}

// In CheckOutServiceTest.java - DUPLICATE!
class CheckOutServiceTest {
    private Book createTestBook() {
        return new Book(
            new ISBN("978-0-13-468599-1"),
            new Title("The Pragmatic Programmer"),
            new Publisher("Addison-Wesley"),
            List.of(new Author("David", "Thomas"))
        );
    }
}

// In LoanServiceTest.java - DUPLICATE AGAIN!
class LoanServiceTest {
    private Book createTestBook() {
        return new Book(
            new ISBN("978-0-13-468599-1"),
            new Title("The Pragmatic Programmer"),
            new Publisher("Addison-Wesley"),
            List.of(new Author("David", "Thomas"))
        );
    }
}
```

### Refactored: Good - Test Data Builders

```java
// test/java/com/penrose/bibby/test/TestBooks.java
public class TestBooks {

    public static Book pragmaticProgrammer() {
        return aBook()
            .withIsbn("978-0-13-468599-1")
            .withTitle("The Pragmatic Programmer")
            .withPublisher("Addison-Wesley")
            .withAuthors("David Thomas", "Andrew Hunt")
            .build();
    }

    public static Book cleanCode() {
        return aBook()
            .withIsbn("978-0-13-235088-4")
            .withTitle("Clean Code")
            .withPublisher("Prentice Hall")
            .withAuthors("Robert C. Martin")
            .build();
    }

    public static BookTestBuilder aBook() {
        return new BookTestBuilder();
    }

    public static Book availableBook() {
        return aBook()
            .available()
            .build();
    }

    public static Book checkedOutBook() {
        return aBook()
            .checkedOut()
            .build();
    }
}

// Now all tests use the same builder!
class BookServiceTest {
    @Test
    void test() {
        Book book = TestBooks.pragmaticProgrammer();
    }
}

class CheckOutServiceTest {
    @Test
    void test() {
        Book book = TestBooks.availableBook();
    }
}
```

---

## Refactoring Anti-Patterns: A Practical Guide

### Step 1: Identify Anti-Patterns in Bibby

**Run this analysis on your existing tests:**

```bash
# Find tests with @Disabled
grep -r "@Disabled" src/test/

# Find tests using @SpringBootTest (should be <10%)
grep -r "@SpringBootTest" src/test/ | wc -l

# Find tests with Thread.sleep (flaky!)
grep -r "Thread.sleep" src/test/

# Find tests with numbers in names (smell!)
grep -r "void test1" src/test/
grep -r "void test2" src/test/
```

### Step 2: Categorize Tests

```java
// Create this structure
src/test/java/
├── unit/              // Fast, isolated, no Spring
│   ├── book/
│   │   ├── IsbnTest.java
│   │   ├── BookTest.java
│   │   └── TitleTest.java
│   └── loan/
│       └── LoanTest.java
├── service/           // Service layer with mocks
│   ├── CheckOutBookServiceTest.java
│   └── ReturnBookServiceTest.java
└── integration/       // @SpringBootTest, Testcontainers
    ├── BookRepositoryIntegrationTest.java
    └── CheckOutIntegrationTest.java
```

### Step 3: Fix One Anti-Pattern at a Time

**Priority Order:**

1. **Remove @Disabled tests** (highest priority)
   - Either fix or delete
   - Never commit broken tests

2. **Isolate interdependent tests**
   - Add `@BeforeEach` cleanup
   - Make each test self-contained

3. **Speed up slow tests**
   - Move logic tests out of @SpringBootTest
   - Use @DataJpaTest instead of @SpringBootTest where possible

4. **Add missing edge cases**
   - Null checks
   - Empty collections
   - Invalid inputs

5. **Extract test data builders**
   - Create TestBooks, TestAuthors, etc.
   - Eliminate duplication

---

## Key Takeaways

### The 10 Testing Anti-Patterns

1. **Testing Implementation Details** - Test behavior through public API
2. **Over-Mocking** - Use real domain objects, mock only infrastructure
3. **Fragile Tests** - Test essential behavior, ignore implementation
4. **Test Interdependence** - Fresh state for every test
5. **Ignoring Failures** - Zero tolerance for broken tests
6. **Multiple Concerns** - One test, one behavior
7. **Testing the Framework** - Test your code, not Spring/JPA
8. **Slow Test Suite** - 70% unit, 20% service, 10% integration
9. **Missing Edge Cases** - Test null, empty, invalid inputs
10. **Copy-Paste Code** - Test data builders eliminate duplication

### The Red Flags

Watch for these test smells:
- `@Disabled` annotations
- Tests with numbers in names (`test1`, `test2`)
- `Thread.sleep()` in tests
- Every test uses `@SpringBootTest`
- Tests take > 1 second each
- Tests break when refactoring
- Tests fail randomly
- Test code duplicated everywhere

### The Refactoring Strategy

```
1. Identify anti-patterns (grep, manual review)
2. Fix critical issues first (@Disabled, interdependence)
3. Speed up slow tests (extract unit tests)
4. Add missing edge cases
5. Consolidate test data builders
6. Monitor test health (speed, flakiness)
```

---

## Action Items

### TASK 29A: Anti-Pattern Audit (2 hours)

**Objective:** Identify testing anti-patterns in current Bibby test suite.

**Steps:**

1. Run grep commands to find:
   - @Disabled tests
   - @SpringBootTest usage
   - Thread.sleep()
   - Numbered test names

2. Categorize findings:
   - Critical (broken tests)
   - High (slow tests)
   - Medium (brittle tests)
   - Low (duplication)

3. Create prioritized fix list

**Deliverable:** Document with list of anti-patterns found

---

### TASK 29B: Fix Test Isolation (2 hours)

**Objective:** Ensure all tests can run independently.

**Steps:**

1. Add `@BeforeEach` to all test classes:
   ```java
   @BeforeEach
   void setUp() {
       // Fresh state for each test
   }
   ```

2. For database tests, add cleanup:
   ```java
   @BeforeEach
   void cleanDatabase() {
       bookRepository.deleteAll();
       entityManager.clear();
   }
   ```

3. Remove static fields shared between tests

4. Run tests in random order to verify:
   ```bash
   mvn test -Dsurefire.runOrder=random
   ```

**Deliverable:** All tests pass in any order

---

### TASK 29C: Speed Up Test Suite (3 hours)

**Objective:** Reduce test suite time by 50%.

**Steps:**

1. Measure current speed:
   ```bash
   time mvn test
   ```

2. Identify slow tests (> 1 second each)

3. Refactor slow tests:
   - Pure logic → Unit test (no Spring)
   - Service layer → Mock dependencies
   - Database queries → @DataJpaTest (not @SpringBootTest)

4. Create test hierarchy:
   - unit/
   - service/
   - integration/

5. Measure improvement

**Deliverable:** Test suite runs in < 30 seconds for unit+service tests

---

### TASK 29D: Extract Test Data Builders (2 hours)

**Objective:** Eliminate test code duplication.

**Steps:**

1. Create `test/java/com/penrose/bibby/test/TestBooks.java`:
   ```java
   public class TestBooks {
       public static Book pragmaticProgrammer() { /* ... */ }
       public static Book availableBook() { /* ... */ }
       public static BookTestBuilder aBook() { /* ... */ }
   }
   ```

2. Create builders for:
   - TestBooks
   - TestAuthors
   - TestShelves
   - TestBookcases

3. Replace duplicated setup code with builders

4. Add semantic methods:
   ```java
   TestBooks.availableBook()
   TestBooks.checkedOutBook()
   TestBooks.overdueBook()
   ```

**Deliverable:** No duplicated test setup code

---

## Study Resources

### Books
- **"Growing Object-Oriented Software, Guided by Tests"** - Freeman & Pryce
  - Chapter 21: Test Readability
  - Chapter 22: Constructing Complex Test Data

- **"Effective Unit Testing"** - Lasse Koskela
  - Chapter 4: Testability
  - Chapter 7: Test Smells

### Articles
- Martin Fowler: "Test Pyramid"
- Kent Beck: "TDD by Example" - Red Bar Patterns chapter

### Videos
- "Integrated Tests Are a Scam" - J.B. Rainsberger (watch this!)

---

## Summary

Testing anti-patterns are insidious - they creep into codebases gradually, making test suites slow, brittle, and untrustworthy. The key to avoiding them:

1. **Test behavior, not implementation** - use public APIs only
2. **Use real domain objects** - mock only infrastructure
3. **Isolate tests completely** - fresh state for every test
4. **Fix broken tests immediately** - zero tolerance for @Disabled
5. **Speed matters** - 70% unit, 20% service, 10% integration
6. **Edge cases are where bugs hide** - test null, empty, invalid

Remember: **Bad tests are worse than no tests.** They provide false confidence while slowing down development. Invest the time to write maintainable, fast, focused tests.

---

**Next:** Section 30 - How It All Fits Together (synthesizing all concepts)

**Previous:** Section 28 - Testing Best Practices & Patterns
