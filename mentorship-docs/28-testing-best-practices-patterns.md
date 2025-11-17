# SECTION 28: TESTING BEST PRACTICES & PATTERNS

## Introduction: Patterns for Maintainable Tests

**"Tests are code. Treat them with the same care as production code."**

You've learned to write unit tests, integration tests, and use TDD. But as your test suite grows, new challenges emerge:
- Test setup becomes repetitive
- Tests become brittle (break when implementation changes)
- Test data creation is verbose
- Tests are hard to understand

This section covers **patterns and practices** that make tests maintainable, readable, and resilient.

**Learning Objectives**:
- Master the Test Data Builder pattern
- Use Object Mothers for complex test data
- Manage test fixtures effectively
- Organize tests for readability
- Write expressive assertions
- Apply DRY principle to tests
- Build a maintainable test suite for Bibby

**Time Investment**: 3-4 hours

---

## Part 1: Test Data Builders

### The Problem

**Verbose test data creation**:

```java
@Test
void shouldCheckOutBook() {
    // 15 lines just to create a book! 😱
    Book book = new Book();
    book.setId(BookId.generate());
    book.setTitle(Title.of("Clean Code"));
    book.setIsbn(ISBN.fromString("9780132350884"));
    book.setPublisher(Publisher.of("Prentice Hall"));
    book.setPublicationYear(2008);
    book.setStatus(AvailabilityStatus.AVAILABLE);
    book.setCheckoutCount(0);
    book.setCreatedAt(LocalDateTime.now());
    book.setUpdatedAt(LocalDateTime.now());
    Set<AuthorId> authorIds = new HashSet<>();
    authorIds.add(AuthorId.generate());
    book.setAuthorIds(authorIds);

    // Finally, the actual test!
    book.checkOut();

    assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
}
```

**Problems**:
- Most setup is irrelevant to this test (we only care about `AVAILABLE` status)
- Copy-pasted across many tests
- Hard to read - what's the test actually testing?
- Brittle - adding a required field breaks 50 tests

### The Solution: Test Data Builder

**Pattern**: Fluent API for creating test objects with sensible defaults.

```java
@Test
void shouldCheckOutBook() {
    Book book = aBook().available().build();  // ✅ One line!

    book.checkOut();

    assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
}
```

### Implementing Test Data Builder

**BookTestBuilder.java**:

```java
package com.penrose.bibby.library.book;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class BookTestBuilder {
    // Sensible defaults
    private BookId id = BookId.generate();
    private Title title = Title.of("Test Book");
    private ISBN isbn = ISBN.fromString("9780132350884");
    private Publisher publisher = Publisher.of("Test Publisher");
    private int publicationYear = 2024;
    private Set<AuthorId> authorIds = Set.of(AuthorId.generate());
    private AvailabilityStatus status = AvailabilityStatus.AVAILABLE;
    private int checkoutCount = 0;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Static factory method
    public static BookTestBuilder aBook() {
        return new BookTestBuilder();
    }

    // Preset methods for common scenarios
    public static BookTestBuilder anAvailableBook() {
        return new BookTestBuilder()
            .withStatus(AvailabilityStatus.AVAILABLE)
            .withCheckoutCount(0);
    }

    public static BookTestBuilder aCheckedOutBook() {
        return new BookTestBuilder()
            .withStatus(AvailabilityStatus.CHECKED_OUT)
            .withCheckoutCount(1);
    }

    public static BookTestBuilder aLostBook() {
        return new BookTestBuilder()
            .withStatus(AvailabilityStatus.LOST);
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

    public BookTestBuilder withStatus(AvailabilityStatus status) {
        this.status = status;
        return this;
    }

    public BookTestBuilder withCheckoutCount(int count) {
        this.checkoutCount = count;
        return this;
    }

    // Convenience methods (aliases)
    public BookTestBuilder available() {
        return withStatus(AvailabilityStatus.AVAILABLE);
    }

    public BookTestBuilder checkedOut() {
        return withStatus(AvailabilityStatus.CHECKED_OUT)
            .withCheckoutCount(1);
    }

    public BookTestBuilder lost() {
        return withStatus(AvailabilityStatus.LOST);
    }

    // Build method
    public Book build() {
        return Book.builder()
            .id(id)
            .title(title)
            .isbn(isbn)
            .publisher(publisher)
            .publicationYear(publicationYear)
            .authorIds(authorIds)
            .status(status)
            .checkoutCount(checkoutCount)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }
}
```

### Using Test Data Builder

**Simple case** (defaults):
```java
@Test
void shouldCreateBook() {
    Book book = aBook().build();

    assertThat(book.getTitle().value()).isEqualTo("Test Book");
}
```

**Customize what matters**:
```java
@Test
void shouldCheckOutAvailableBook() {
    Book book = aBook()
        .available()  // Only specify status
        .build();

    book.checkOut();

    assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
}
```

**Multiple customizations**:
```java
@Test
void shouldFindBookByIsbn() {
    ISBN targetIsbn = ISBN.fromString("9780321125217");

    Book book = aBook()
        .withTitle("Domain-Driven Design")
        .withIsbn(targetIsbn.value())
        .build();

    repository.save(book);

    Optional<Book> found = repository.findByIsbn(targetIsbn);
    assertThat(found).isPresent();
}
```

**Preset scenarios**:
```java
@Test
void shouldNotCheckOutLostBook() {
    Book book = aLostBook().build();  // Preset!

    assertThatThrownBy(() -> book.checkOut())
        .isInstanceOf(BookNotAvailableException.class);
}
```

### Benefits of Test Data Builders

1. **Readable tests** - Clear what's being tested
2. **DRY tests** - No repetitive setup
3. **Resilient** - Adding fields doesn't break tests (defaults handle it)
4. **Self-documenting** - Methods like `available()` document intent
5. **Flexible** - Customize only what you need

---

## Part 2: Object Mothers

### What is an Object Mother?

**Object Mother**: Factory methods that create specific, named test scenarios.

**Difference from Test Data Builder**:
- **Builder**: Flexible, customize anything
- **Mother**: Specific scenarios, named methods

### Example: BookMother

```java
public class BookMother {

    public static Book cleanCodeBook() {
        return Book.builder()
            .id(BookId.generate())
            .title(Title.of("Clean Code"))
            .isbn(ISBN.fromString("9780132350884"))
            .publisher(Publisher.of("Prentice Hall"))
            .publicationYear(2008)
            .authorIds(Set.of(AuthorMother.robertCMartin().getId()))
            .status(AvailabilityStatus.AVAILABLE)
            .build();
    }

    public static Book domainDrivenDesignBook() {
        return Book.builder()
            .id(BookId.generate())
            .title(Title.of("Domain-Driven Design"))
            .isbn(ISBN.fromString("9780321125217"))
            .publisher(Publisher.of("Addison-Wesley"))
            .publicationYear(2003)
            .authorIds(Set.of(AuthorMother.ericEvans().getId()))
            .status(AvailabilityStatus.AVAILABLE)
            .build();
    }

    public static Book refactoringBook() {
        return Book.builder()
            .id(BookId.generate())
            .title(Title.of("Refactoring"))
            .isbn(ISBN.fromString("9780201485677"))
            .publisher(Publisher.of("Addison-Wesley"))
            .publicationYear(1999)
            .authorIds(Set.of(AuthorMother.martinFowler().getId()))
            .status(AvailabilityStatus.AVAILABLE)
            .build();
    }

    // Scenario-based mothers
    public static Book overdueBook() {
        return aBook()
            .checkedOut()
            .withCheckoutCount(5)
            .build();
    }

    public static Book popularBook() {
        return aBook()
            .withCheckoutCount(50)
            .build();
    }
}
```

### When to Use Object Mothers

**Use Object Mothers when**:
- ✅ You need the same specific data across many tests
- ✅ Test data represents real-world scenarios
- ✅ Relationships between objects matter

**Example**:
```java
@Test
void shouldFindBooksByAuthor() {
    // Using Object Mothers for real books
    Book cleanCode = BookMother.cleanCodeBook();
    Book ddd = BookMother.domainDrivenDesignBook();
    Book refactoring = BookMother.refactoringBook();

    repository.saveAll(List.of(cleanCode, ddd, refactoring));

    AuthorId martinFowlerId = AuthorMother.martinFowler().getId();
    List<Book> martinBooks = repository.findByAuthorId(martinFowlerId);

    assertThat(martinBooks).containsExactly(refactoring);
}
```

### Combining Builders and Mothers

**Best approach**: Use both!

```java
// Specific scenario from Mother
Book book = BookMother.cleanCodeBook();

// Customize with Builder
Book customized = aBook()
    .basedOn(BookMother.cleanCodeBook())  // Start with mother
    .withStatus(AvailabilityStatus.LOST)   // Customize
    .build();
```

**Implementation**:
```java
public class BookTestBuilder {
    // ...

    public static BookTestBuilder basedOn(Book template) {
        return new BookTestBuilder()
            .withId(template.getId())
            .withTitle(template.getTitle().value())
            .withIsbn(template.getIsbn().value())
            .withStatus(template.getStatus());
    }
}
```

---

## Part 3: Test Fixture Management

### What is a Test Fixture?

**Test Fixture**: The known state of the test environment.

**Examples**:
- Test database with specific data
- Mock objects configured with stubs
- File system in a known state

### Managing Fixtures with @BeforeEach / @AfterEach

```java
@DisplayName("BookRepository")
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    private Book cleanCode;
    private Book ddd;

    @BeforeEach
    void setUp() {
        // Create fixture before each test
        cleanCode = BookMother.cleanCodeBook();
        ddd = BookMother.domainDrivenDesignBook();

        bookRepository.saveAll(List.of(cleanCode, ddd));
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        bookRepository.deleteAll();
    }

    @Test
    void shouldFindAllBooks() {
        List<Book> books = bookRepository.findAll();

        assertThat(books).hasSize(2);
    }

    @Test
    void shouldFindBookByIsbn() {
        Optional<Book> found = bookRepository.findByIsbn(cleanCode.getIsbn());

        assertThat(found).isPresent();
    }
}
```

### When to Use Fixtures

**Use @BeforeEach when**:
- ✅ Multiple tests need the same setup
- ✅ Setup is complex or expensive

**Don't use @BeforeEach when**:
- ❌ Each test needs different data
- ❌ Setup is simple (one line)

**Example of fixture overuse**:
```java
// ❌ BAD: Fixtures that aren't used by all tests
@BeforeEach
void setUp() {
    book1 = aBook().build();
    book2 = aBook().build();
    book3 = aBook().build();
}

@Test
void testA() {
    // Only uses book1
}

@Test
void testB() {
    // Only uses book2
}
// book3 is never used!
```

**Better**:
```java
// ✅ GOOD: Setup in each test
@Test
void testA() {
    Book book = aBook().build();
    // Use book
}

@Test
void testB() {
    Book book = aBook().build();
    // Use book
}
```

### Fixture Cleanup Strategies

**Strategy 1: @Transactional (Automatic)**
```java
@SpringBootTest
@Transactional  // Auto-rollback after each test
class BookServiceTest {
    // No manual cleanup needed
}
```

**Strategy 2: @AfterEach (Manual)**
```java
@SpringBootTest
class BookServiceTest {

    @AfterEach
    void cleanUp() {
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }
}
```

**Strategy 3: Database Reset Script**
```java
@SpringBootTest
class BookServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE books CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE authors CASCADE");
    }
}
```

---

## Part 4: Test Organization

### Pattern 1: @Nested for Hierarchical Tests

**Organize related tests together**:

```java
@DisplayName("Book")
class BookTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("should create with valid data")
        void shouldCreateWithValidData() {
            Book book = aBook().build();

            assertThat(book.getId()).isNotNull();
        }

        @Test
        @DisplayName("should throw when title is null")
        void shouldThrowWhenTitleNull() {
            assertThatThrownBy(() ->
                Book.builder().title(null).build()
            ).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("checkout")
    class Checkout {

        @Test
        @DisplayName("should check out available book")
        void shouldCheckOutAvailable() {
            Book book = aBook().available().build();

            book.checkOut();

            assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
        }

        @Test
        @DisplayName("should throw when already checked out")
        void shouldThrowWhenAlreadyCheckedOut() {
            Book book = aBook().checkedOut().build();

            assertThatThrownBy(() -> book.checkOut())
                .isInstanceOf(BookNotAvailableException.class);
        }
    }
}
```

**Output**:
```
Book
  creation
    ✓ should create with valid data
    ✓ should throw when title is null
  checkout
    ✓ should check out available book
    ✓ should throw when already checked out
```

### Pattern 2: Shared Setup in @Nested Classes

```java
@DisplayName("Loan")
class LoanTest {

    @Nested
    @DisplayName("when overdue")
    class WhenOverdue {

        private Loan loan;

        @BeforeEach
        void setUp() {
            // Shared setup for all "overdue" tests
            LocalDate past = LocalDate.now().minusDays(20);
            loan = Loan.create(BookId.generate(), "Alice", past);
        }

        @Test
        @DisplayName("should be marked as overdue")
        void shouldBeMarkedOverdue() {
            assertThat(loan.isOverdue(LocalDate.now())).isTrue();
        }

        @Test
        @DisplayName("should calculate days overdue")
        void shouldCalculateDaysOverdue() {
            assertThat(loan.daysOverdue(LocalDate.now())).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("when not overdue")
    class WhenNotOverdue {

        private Loan loan;

        @BeforeEach
        void setUp() {
            // Different setup for "not overdue" tests
            LocalDate recent = LocalDate.now().minusDays(5);
            loan = Loan.create(BookId.generate(), "Bob", recent);
        }

        @Test
        @DisplayName("should not be marked as overdue")
        void shouldNotBeMarkedOverdue() {
            assertThat(loan.isOverdue(LocalDate.now())).isFalse();
        }
    }
}
```

### Pattern 3: Test Suites

**Group related test classes**:

```java
@Suite
@SelectClasses({
    BookTest.class,
    LoanTest.class,
    ISBNTest.class,
    TitleTest.class
})
public class DomainModelTestSuite {
}
```

**Run entire suite**:
```bash
mvn test -Dtest=DomainModelTestSuite
```

---

## Part 5: Assertion Patterns

### Pattern 1: Custom Assertion Methods

**Extract repeated assertions**:

**Before**:
```java
@Test
void test1() {
    assertThat(book.getId()).isNotNull();
    assertThat(book.getTitle()).isNotNull();
    assertThat(book.getIsbn()).isNotNull();
}

@Test
void test2() {
    assertThat(book.getId()).isNotNull();  // Duplicated!
    assertThat(book.getTitle()).isNotNull();
    assertThat(book.getIsbn()).isNotNull();
}
```

**After**:
```java
@Test
void test1() {
    assertBookIsValid(book);
}

@Test
void test2() {
    assertBookIsValid(book);
}

private void assertBookIsValid(Book book) {
    assertThat(book.getId()).isNotNull();
    assertThat(book.getTitle()).isNotNull();
    assertThat(book.getIsbn()).isNotNull();
}
```

### Pattern 2: AssertJ Custom Assertions

**Create domain-specific assertions**:

```java
public class BookAssert extends AbstractAssert<BookAssert, Book> {

    public BookAssert(Book book) {
        super(book, BookAssert.class);
    }

    public static BookAssert assertThat(Book book) {
        return new BookAssert(book);
    }

    public BookAssert isAvailable() {
        isNotNull();
        if (actual.getStatus() != AvailabilityStatus.AVAILABLE) {
            failWithMessage("Expected book to be AVAILABLE but was <%s>",
                actual.getStatus());
        }
        return this;
    }

    public BookAssert isCheckedOut() {
        isNotNull();
        if (actual.getStatus() != AvailabilityStatus.CHECKED_OUT) {
            failWithMessage("Expected book to be CHECKED_OUT but was <%s>",
                actual.getStatus());
        }
        return this;
    }

    public BookAssert hasBeenCheckedOut(int times) {
        isNotNull();
        if (actual.getCheckoutCount() != times) {
            failWithMessage("Expected checkout count <%d> but was <%d>",
                times, actual.getCheckoutCount());
        }
        return this;
    }
}
```

**Usage**:
```java
import static com.penrose.bibby.test.BookAssert.assertThat;

@Test
void shouldCheckOutBook() {
    Book book = aBook().available().build();

    book.checkOut();

    assertThat(book)
        .isCheckedOut()
        .hasBeenCheckedOut(1);
}
```

### Pattern 3: Soft Assertions

**Assert multiple conditions, show all failures**:

```java
@Test
void shouldHaveCorrectBookDetails() {
    Book book = BookMother.cleanCodeBook();

    SoftAssertions softly = new SoftAssertions();

    softly.assertThat(book.getTitle().value()).isEqualTo("Clean Code");
    softly.assertThat(book.getIsbn().value()).isEqualTo("9780132350884");
    softly.assertThat(book.getPublicationYear()).isEqualTo(2008);
    softly.assertThat(book.getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);

    softly.assertAll();  // Shows ALL failures, not just first
}
```

**Without soft assertions** (stops at first failure):
```
Expected: 2008
But was:  2007
```

**With soft assertions** (shows all failures):
```
Expected: 2008, but was: 2007
Expected: AVAILABLE, but was: CHECKED_OUT
```

---

## Part 6: DRY Principle in Tests

### Applying DRY Carefully

**DRY in tests is different** - readability matters more.

**Too DRY (hard to understand)**:
```java
@Test
void test1() {
    Book book = setupBook(SCENARIO_A);
    performAction(ACTION_1);
    verifyOutcome(OUTCOME_X);
}
// What does this test do? Must read 3 helper methods!
```

**Right amount of DRY (clear and maintainable)**:
```java
@Test
void shouldCheckOutAvailableBook() {
    Book book = aBook().available().build();  // Clear setup

    book.checkOut();  // Clear action

    assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);  // Clear outcome
}
```

### Where to Apply DRY in Tests

**✅ DO extract**:
- Test data creation (builders, mothers)
- Common assertions (custom assertions)
- Test utilities (wait for condition, retry logic)

**❌ DON'T extract**:
- Test setup when it hurts readability
- AAA structure (keep Arrange-Act-Assert visible)
- Assertions when they're simple

---

## Part 7: Complete Example - Bibby Test Suite

### Organized Test Structure

```
src/test/java/
  com/penrose/bibby/
    library/
      book/
        domain/
          BookTest.java
          ISBNTest.java
          TitleTest.java
        repository/
          BookRepositoryIntegrationTest.java
        service/
          CheckOutBookServiceTest.java
          AddBookServiceTest.java
        builders/
          BookTestBuilder.java
          BookMother.java
        assertions/
          BookAssert.java
      loan/
        domain/
          LoanTest.java
        builders/
          LoanTestBuilder.java
```

### Complete BookTest with Best Practices

```java
package com.penrose.bibby.library.book.domain;

import org.junit.jupiter.api.*;
import static com.penrose.bibby.library.book.builders.BookTestBuilder.*;
import static com.penrose.bibby.library.book.assertions.BookAssert.assertThat;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Book")
class BookTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("should create book with all required fields")
        void shouldCreateWithRequiredFields() {
            Book book = aBook()
                .withTitle("Clean Code")
                .withIsbn("9780132350884")
                .build();

            assertThat(book.getTitle().value()).isEqualTo("Clean Code");
            assertThat(book.getIsbn().value()).isEqualTo("9780132350884");
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("should throw when title is null")
        void shouldThrowWhenTitleNull(String nullTitle) {
            assertThatThrownBy(() ->
                aBook().withTitle(nullTitle).build()
            ).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("checkout")
    class Checkout {

        @Test
        @DisplayName("should check out available book")
        void shouldCheckOutAvailableBook() {
            Book book = anAvailableBook().build();

            book.checkOut();

            assertThat(book)
                .isCheckedOut()
                .hasBeenCheckedOut(1);
        }

        @ParameterizedTest
        @EnumSource(
            value = AvailabilityStatus.class,
            names = {"AVAILABLE"},
            mode = EnumSource.Mode.EXCLUDE
        )
        @DisplayName("should throw when book is not available")
        void shouldThrowWhenNotAvailable(AvailabilityStatus status) {
            Book book = aBook().withStatus(status).build();

            assertThatThrownBy(() -> book.checkOut())
                .isInstanceOf(BookNotAvailableException.class)
                .hasMessageContaining(status.toString());
        }
    }
}
```

---

## Action Items: Improving Bibby's Test Suite

### Action Item 1: Create Test Data Builders (2-3 hours)

**Create builders for all domain objects**:
- `BookTestBuilder.java`
- `LoanTestBuilder.java`
- `AuthorTestBuilder.java`
- `ReservationTestBuilder.java`

### Action Item 2: Create Object Mothers (1-2 hours)

**Create mothers for common scenarios**:
- `BookMother` - cleanCodeBook(), dddBook(), overdueBook()
- `AuthorMother` - robertCMartin(), martinFowler(), ericEvans()
- `LoanMother` - overdueLoan(), activeLoan(), returnedLoan()

### Action Item 3: Add Custom Assertions (1 hour)

**Create AssertJ custom assertions**:
- `BookAssert` - isAvailable(), isCheckedOut(), hasBeenCheckedOut()
- `LoanAssert` - isOverdue(), isReturned()

### Action Item 4: Organize Tests with @Nested (1-2 hours)

**Refactor existing tests**:
- Group related tests
- Use @DisplayName for clarity
- Share setup in nested classes

### Action Item 5: Extract Common Test Utilities (1 hour)

**Create test utilities package**:
- `TestFixtures` - Common test data
- `TestUtils` - Wait for condition, retry logic
- `TestConstants` - Shared test constants

---

## Key Takeaways

### 1. Test Data Builders Improve Readability

`aBook().available().build()` is clearer than 15 lines of setup.

### 2. Object Mothers for Specific Scenarios

Use named methods like `cleanCodeBook()` for realistic test data.

### 3. Organize Tests Hierarchically

@Nested classes group related tests for better structure.

### 4. Custom Assertions Make Tests Expressive

`assertThat(book).isCheckedOut()` reads better than raw assertions.

### 5. DRY in Tests Requires Balance

Extract what's repetitive, but keep tests readable.

---

## Study Resources

### Books
1. **"Growing Object-Oriented Software, Guided by Tests"** - Freeman & Pryce
   - Test data builders pattern (Chapter 22)
2. **"xUnit Test Patterns"** by Gerard Meszaros
   - Comprehensive test patterns catalog

### Articles
1. **"Test Data Builders"** by Nat Pryce
2. **"Object Mother"** pattern on Martin Fowler's blog

---

## Coming Next

**Section 29: Testing Anti-Patterns** - What NOT to do

We'll cover:
- Common test smells
- Fragile tests
- Slow tests
- Over-mocking
- How to recognize and fix anti-patterns

---

**Section 28 Complete** | **Time Invested**: 3-4 hours

You now have patterns to build maintainable, readable tests. Apply these to Bibby, and your test suite will be as clean as your production code!
