# SECTION 24: UNIT TESTING FUNDAMENTALS

## Introduction: The Foundation of Quality Software

**"The best way to ensure your code works is to test it. The best way to ensure your tests work is to write good tests."**

You've learned about testing philosophy in Section 23. Now it's time to master the mechanics: **how to write excellent unit tests** that are fast, reliable, and maintainable.

In this section, we'll build a comprehensive test suite for your **Bibby** library management system, covering every fundamental technique and pattern.

**Learning Objectives**:
- Understand what makes a unit test "unit"
- Master the AAA pattern (Arrange, Act, Assert)
- Write clear, descriptive test names
- Use JUnit 5 effectively
- Write fluent assertions with AssertJ
- Create parameterized tests
- Organize tests with @Nested and @DisplayName
- Build complete test coverage for Bibby domain

**Time Investment**: 4-5 hours

---

## Part 1: What is a Unit Test?

### Definition

A **unit test** is a test that:
1. **Tests one unit** (class, method, or small group of related methods)
2. **Runs fast** (milliseconds)
3. **Runs in isolation** (no database, no network, no file system)
4. **Is deterministic** (same input → same output, always)
5. **Tests behavior**, not implementation

### Unit vs. Integration vs. E2E

**Unit Test**:
```java
@Test
void shouldCheckOutAvailableBook() {
    Book book = aBook().available().build();  // In-memory object

    book.checkOut();  // Pure domain logic

    assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
}
// ✅ Fast (< 1ms), no dependencies
```

**Integration Test**:
```java
@Test
@Transactional
void shouldSaveBookToDatabase() {
    Book book = aBook().build();

    bookRepository.save(book);  // Hits database!

    Book loaded = bookRepository.findById(book.getId()).orElseThrow();
    assertThat(loaded.getTitle()).isEqualTo(book.getTitle());
}
// ⚠️ Slower (50-200ms), requires database
```

**E2E Test**:
```java
@Test
void shouldCheckoutBookViaCommandLine() {
    String output = runCommand("checkout --book-id 42");  // Full app!

    assertThat(output).contains("Book checked out successfully");
}
// ❌ Slowest (1-5 seconds), fragile
```

### What to Unit Test in Bibby

**YES** (Unit Test These):
- ✅ Domain logic: `Book.checkOut()`, `Loan.calculateLateFee()`
- ✅ Value objects: `ISBN`, `Title`, `BookId`
- ✅ Business rules: `Bookcase.canAddShelf()`
- ✅ Calculations: `Money.add()`, `Money.multiply()`
- ✅ Validations: `Title.of()` throws for null

**NO** (Integration/E2E Test These):
- ❌ Repository methods (Spring Data - already tested)
- ❌ Database queries (test with integration tests)
- ❌ CLI commands (test with E2E tests)
- ❌ Spring configuration (not worth testing)
- ❌ Getters/setters (no logic to test)

---

## Part 2: Anatomy of a Good Test - The AAA Pattern

### The AAA Pattern

**AAA** = **Arrange, Act, Assert**

Every test should follow this structure:

```java
@Test
void shouldCheckOutAvailableBook() {
    // ARRANGE: Set up test data and preconditions
    Book book = aBook()
        .withStatus(AvailabilityStatus.AVAILABLE)
        .withCheckoutCount(0)
        .build();

    // ACT: Perform the action being tested
    book.checkOut();

    // ASSERT: Verify the outcome
    assertThat(book.getStatus()).isEqualTo(AvailabilityStatus.CHECKED_OUT);
    assertThat(book.getCheckoutCount()).isEqualTo(1);
}
```

**Why this structure?**
1. **Readability**: Anyone can understand the test
2. **Clarity**: Separates setup, action, and verification
3. **Maintainability**: Easy to modify each section independently

### Example: Testing ISBN Validation

**Good test with AAA**:
```java
@Test
void shouldRejectInvalidISBNFormat() {
    // ARRANGE
    String invalidIsbn = "invalid-isbn-123";

    // ACT & ASSERT (combined for exception testing)
    assertThatThrownBy(() -> ISBN.fromString(invalidIsbn))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid ISBN");
}
```

**Bad test without AAA**:
```java
@Test
void test1() {
    assertThatThrownBy(() -> ISBN.fromString("invalid-isbn-123"))
        .isInstanceOf(IllegalArgumentException.class);
}
// ❌ What are we testing? Why is it invalid? Unclear!
```

### Given-When-Then (Alternative Naming)

Some prefer **Given-When-Then** (same concept, different names):

```java
@Test
void shouldCalculateLateFeeForOverdueBook() {
    // GIVEN: A loan that's 10 days overdue
    LocalDate dueDate = LocalDate.of(2024, 1, 1);
    LocalDate today = LocalDate.of(2024, 1, 11);
    Loan loan = aLoan().withDueDate(dueDate).build();

    // WHEN: Calculate late fee
    Money lateFee = loan.calculateLateFee(today);

    // THEN: Fee is $5 (10 days * $0.50/day)
    assertThat(lateFee).isEqualTo(Money.of(5.00));
}
```

**Use whichever you prefer** - consistency matters more than the specific naming.

---

## Part 3: Test Naming Conventions

### Good Test Names

A test name should answer three questions:
1. **What** is being tested?
2. **Under what conditions**?
3. **What's the expected result**?

**Pattern**: `should[ExpectedBehavior]When[Condition]`

**Examples**:

```java
// ✅ GOOD: Clear what's being tested
@Test
void shouldCheckOutBookWhenAvailable()

@Test
void shouldThrowExceptionWhenCheckingOutAlreadyCheckedOutBook()

@Test
void shouldCalculateLateFeeWhenBookOverdue()

@Test
void shouldReturnEmptyOptionalWhenBookNotFound()

// ❌ BAD: Unclear what's being tested
@Test
void testCheckout()

@Test
void test1()

@Test
void bookTest()
```

### Using @DisplayName for Readability

JUnit 5 allows custom display names:

```java
@DisplayName("Book checkout")
class BookCheckoutTest {

    @Test
    @DisplayName("should change status to CHECKED_OUT when book is available")
    void shouldChangeStatusWhenAvailable() {
        // ...
    }

    @Test
    @DisplayName("should increment checkout count when checking out")
    void shouldIncrementCheckoutCount() {
        // ...
    }

    @Test
    @DisplayName("should throw BookNotAvailableException when book is already checked out")
    void shouldThrowWhenAlreadyCheckedOut() {
        // ...
    }
}
```

**Test output**:
```
Book checkout
  ✓ should change status to CHECKED_OUT when book is available
  ✓ should increment checkout count when checking out
  ✓ should throw BookNotAvailableException when book is already checked out
```

---

## Part 4: JUnit 5 Fundamentals

### Basic Test Structure

```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BookTest {

    @Test
    void shouldCreateBookWithValidData() {
        // Test code
    }
}
```

### Test Lifecycle Annotations

```java
class BookLifecycleTest {

    private Book book;

    @BeforeAll
    static void setUpClass() {
        // Runs ONCE before all tests in this class
        // Must be static
        System.out.println("Starting BookLifecycleTest");
    }

    @BeforeEach
    void setUp() {
        // Runs BEFORE each test
        book = aBook().available().build();
        System.out.println("Created fresh book");
    }

    @Test
    void test1() {
        book.checkOut();
        // book instance is fresh (from @BeforeEach)
    }

    @Test
    void test2() {
        book.checkOut();
        // book instance is fresh again (new instance from @BeforeEach)
    }

    @AfterEach
    void tearDown() {
        // Runs AFTER each test
        book = null;
        System.out.println("Cleaned up");
    }

    @AfterAll
    static void tearDownClass() {
        // Runs ONCE after all tests
        System.out.println("Finished BookLifecycleTest");
    }
}
```

**When to use**:
- **@BeforeEach**: Create test data that's needed by multiple tests
- **@AfterEach**: Clean up resources (close connections, delete files)
- **@BeforeAll/@AfterAll**: Expensive setup (starting embedded database) - use sparingly

### Assertion Methods

**Basic assertions**:
```java
@Test
void basicAssertions() {
    Book book = aBook().build();

    // assertEquals - JUnit style
    assertEquals(AvailabilityStatus.AVAILABLE, book.getStatus());

    // assertTrue/assertFalse
    assertTrue(book.isAvailable());
    assertFalse(book.isCheckedOut());

    // assertNotNull
    assertNotNull(book.getId());

    // assertThrows
    assertThrows(BookNotAvailableException.class, () -> {
        book.checkOut();
        book.checkOut();  // Second checkout throws
    });
}
```

**But we'll use AssertJ instead** (more fluent, better error messages).

---

## Part 5: AssertJ - Fluent Assertions

### Why AssertJ?

**JUnit assertions**:
```java
assertEquals(expected, actual);  // Which is which?
```

**AssertJ**:
```java
assertThat(actual).isEqualTo(expected);  // Clear!
```

### Basic AssertJ Assertions

```java
import static org.assertj.core.api.Assertions.*;

@Test
void assertJExamples() {
    Book book = aBook().available().build();

    // Equality
    assertThat(book.getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);

    // Null checks
    assertThat(book.getId()).isNotNull();
    assertThat(book.getDeletedAt()).isNull();

    // Boolean checks
    assertThat(book.isAvailable()).isTrue();
    assertThat(book.isCheckedOut()).isFalse();

    // String checks
    assertThat(book.getTitle().value()).isEqualTo("Clean Code");
    assertThat(book.getTitle().value()).contains("Clean");
    assertThat(book.getTitle().value()).startsWith("Clean");
    assertThat(book.getTitle().value()).endsWith("Code");

    // Number checks
    assertThat(book.getCheckoutCount()).isEqualTo(0);
    assertThat(book.getCheckoutCount()).isZero();
    assertThat(book.getCheckoutCount()).isGreaterThanOrEqualTo(0);

    // Collection checks
    assertThat(book.getAuthorIds()).isNotEmpty();
    assertThat(book.getAuthorIds()).hasSize(1);
    assertThat(book.getAuthorIds()).contains(authorId);
}
```

### Exception Assertions

```java
@Test
void shouldThrowExceptionWhenCheckingOutUnavailableBook() {
    Book book = aBook().checkedOut().build();

    assertThatThrownBy(() -> book.checkOut())
        .isInstanceOf(BookNotAvailableException.class)
        .hasMessage("Cannot check out book - status: CHECKED_OUT")
        .hasMessageContaining("CHECKED_OUT");
}

@Test
void shouldThrowExceptionWithNullISBN() {
    assertThatThrownBy(() -> ISBN.fromString(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("ISBN cannot be null");
}

@Test
void shouldNotThrowWhenValidISBN() {
    assertThatCode(() -> ISBN.fromString("978-0-321-12521-5"))
        .doesNotThrowAnyException();
}
```

### Collection Assertions

```java
@Test
void collectionAssertions() {
    List<Book> books = List.of(
        aBook().withTitle("Clean Code").build(),
        aBook().withTitle("Refactoring").build(),
        aBook().withTitle("DDD").build()
    );

    // Size
    assertThat(books).hasSize(3);
    assertThat(books).isNotEmpty();

    // Contains
    assertThat(books).contains(books.get(0));

    // Extract and assert on property
    assertThat(books)
        .extracting(book -> book.getTitle().value())
        .containsExactly("Clean Code", "Refactoring", "DDD");

    // Any match
    assertThat(books)
        .anyMatch(book -> book.getTitle().value().contains("Clean"));

    // All match
    assertThat(books)
        .allMatch(book -> book.getStatus() == AvailabilityStatus.AVAILABLE);
}
```

### Custom Assertions (Advanced)

For domain objects, create custom assertions:

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
            failWithMessage("Expected book to be available but was <%s>",
                actual.getStatus());
        }
        return this;
    }

    public BookAssert isCheckedOut() {
        isNotNull();
        if (actual.getStatus() != AvailabilityStatus.CHECKED_OUT) {
            failWithMessage("Expected book to be checked out but was <%s>",
                actual.getStatus());
        }
        return this;
    }

    public BookAssert hasCheckoutCount(int expected) {
        isNotNull();
        if (actual.getCheckoutCount() != expected) {
            failWithMessage("Expected checkout count <%d> but was <%d>",
                expected, actual.getCheckoutCount());
        }
        return this;
    }
}

// Usage
@Test
void customAssertionExample() {
    Book book = aBook().build();
    book.checkOut();

    BookAssert.assertThat(book)
        .isCheckedOut()
        .hasCheckoutCount(1);
}
```

---

## Part 6: Parameterized Tests

### What Are Parameterized Tests?

Run the **same test** with **different inputs**.

**Without parameterization** (repetitive):
```java
@Test
void shouldAcceptValidISBN10_1() {
    ISBN isbn = ISBN.fromString("0132350882");
    assertThat(isbn.value()).isEqualTo("0132350882");
}

@Test
void shouldAcceptValidISBN10_2() {
    ISBN isbn = ISBN.fromString("043942089X");
    assertThat(isbn.value()).isEqualTo("043942089X");
}

@Test
void shouldAcceptValidISBN13_1() {
    ISBN isbn = ISBN.fromString("9780132350884");
    assertThat(isbn.value()).isEqualTo("9780132350884");
}
// ... 10 more tests
```

**With parameterization** (clean):
```java
@ParameterizedTest
@ValueSource(strings = {
    "0132350882",
    "043942089X",
    "9780132350884",
    "9780321125217",
    "978-0-13-235088-4"  // With dashes
})
void shouldAcceptValidISBN(String validIsbn) {
    ISBN isbn = ISBN.fromString(validIsbn);

    assertThat(isbn.value()).isNotNull();
}
```

### @ValueSource - Simple Values

```java
@ParameterizedTest
@ValueSource(ints = {0, 1, 5, 10, 100})
void shouldAcceptValidShelfCapacity(int capacity) {
    Bookcase bookcase = Bookcase.create(
        BookcaseLabel.of("Test"),
        capacity
    );

    assertThat(bookcase.getShelfCapacity()).isEqualTo(capacity);
}

@ParameterizedTest
@ValueSource(strings = {"", "  ", "\t", "\n"})
void shouldRejectBlankTitle(String blankTitle) {
    assertThatThrownBy(() -> Title.of(blankTitle))
        .isInstanceOf(IllegalArgumentException.class);
}
```

### @CsvSource - Multiple Parameters

```java
@ParameterizedTest
@CsvSource({
    "1, 0.50",   // 1 day late = $0.50
    "5, 2.50",   // 5 days = $2.50
    "10, 5.00",  // 10 days = $5.00
    "30, 15.00"  // 30 days = $15.00
})
void shouldCalculateLateFee(int daysLate, double expectedFee) {
    Loan loan = aLoan()
        .withDueDate(LocalDate.now().minusDays(daysLate))
        .build();

    Money fee = loan.calculateLateFee(LocalDate.now());

    assertThat(fee).isEqualTo(Money.of(expectedFee));
}
```

### @MethodSource - Complex Objects

```java
@ParameterizedTest
@MethodSource("invalidISBNs")
void shouldRejectInvalidISBN(String invalidIsbn) {
    assertThatThrownBy(() -> ISBN.fromString(invalidIsbn))
        .isInstanceOf(IllegalArgumentException.class);
}

static Stream<String> invalidISBNs() {
    return Stream.of(
        "123",              // Too short
        "12345678901234",   // Too long
        "abcdefghij",       // Letters
        "123-456-789",      // Invalid format
        "",                 // Empty
        "978013235088",     // 12 digits (not 10 or 13)
        "97801323508840"    // 14 digits
    );
}
```

### @EnumSource - Test All Enum Values

```java
@ParameterizedTest
@EnumSource(AvailabilityStatus.class)
void shouldConvertStatusToString(AvailabilityStatus status) {
    String statusString = status.toString();

    assertThat(statusString).isNotBlank();
}

@ParameterizedTest
@EnumSource(
    value = AvailabilityStatus.class,
    names = {"CHECKED_OUT", "LOST", "DAMAGED"},
    mode = EnumSource.Mode.INCLUDE
)
void shouldNotAllowCheckoutWhenNotAvailable(AvailabilityStatus status) {
    Book book = aBook().withStatus(status).build();

    assertThatThrownBy(() -> book.checkOut())
        .isInstanceOf(BookNotAvailableException.class);
}
```

---

## Part 7: Organizing Tests with @Nested

### Why @Nested?

Group related tests together for better organization.

**Without @Nested** (flat):
```java
class BookTest {
    @Test void shouldCheckOutAvailableBook() {}
    @Test void shouldThrowWhenCheckingOutUnavailableBook() {}
    @Test void shouldReturnAvailableBook() {}
    @Test void shouldThrowWhenReturningAvailableBook() {}
    @Test void shouldPlaceOnShelf() {}
    @Test void shouldRemoveFromShelf() {}
    // 50 more tests...
}
```

**With @Nested** (organized):
```java
@DisplayName("Book")
class BookTest {

    @Nested
    @DisplayName("checkout")
    class Checkout {

        @Test
        @DisplayName("should check out available book")
        void shouldCheckOutAvailableBook() {
            Book book = aBook().available().build();

            book.checkOut();

            assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
        }

        @Test
        @DisplayName("should throw when book already checked out")
        void shouldThrowWhenAlreadyCheckedOut() {
            Book book = aBook().checkedOut().build();

            assertThatThrownBy(() -> book.checkOut())
                .isInstanceOf(BookNotAvailableException.class);
        }
    }

    @Nested
    @DisplayName("returnToLibrary")
    class ReturnToLibrary {

        @Test
        @DisplayName("should return checked out book")
        void shouldReturnCheckedOutBook() {
            Book book = aBook().checkedOut().build();

            book.returnToLibrary();

            assertThat(book.getStatus()).isEqualTo(AVAILABLE);
        }

        @Test
        @DisplayName("should throw when returning available book")
        void shouldThrowWhenReturningAvailable() {
            Book book = aBook().available().build();

            assertThatThrownBy(() -> book.returnToLibrary())
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("placeOnShelf")
    class PlaceOnShelf {

        @Test
        @DisplayName("should place book on shelf")
        void shouldPlaceOnShelf() {
            Book book = aBook().build();
            ShelfId shelfId = ShelfId.generate();

            book.placeOnShelf(shelfId);

            assertThat(book.getCurrentShelfId()).isEqualTo(shelfId);
        }

        @Test
        @DisplayName("should throw when shelf ID is null")
        void shouldThrowWhenShelfIdNull() {
            Book book = aBook().build();

            assertThatThrownBy(() -> book.placeOnShelf(null))
                .isInstanceOf(NullPointerException.class);
        }
    }
}
```

**Test output**:
```
Book
  checkout
    ✓ should check out available book
    ✓ should throw when book already checked out
  returnToLibrary
    ✓ should return checked out book
    ✓ should throw when returning available book
  placeOnShelf
    ✓ should place book on shelf
    ✓ should throw when shelf ID is null
```

---

## Part 8: Complete Test Examples for Bibby

### Example 1: Comprehensive Book Tests

```java
package com.penrose.bibby.library.book;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.LocalDateTime;

import static com.penrose.bibby.library.book.BookTestBuilder.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Book")
class BookTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("should create book with all required fields")
        void shouldCreateWithRequiredFields() {
            BookId id = BookId.generate();
            Title title = Title.of("Clean Code");
            ISBN isbn = ISBN.fromString("9780132350884");
            Set<AuthorId> authorIds = Set.of(AuthorId.generate());

            Book book = Book.builder()
                .id(id)
                .title(title)
                .isbn(isbn)
                .authorIds(authorIds)
                .build();

            assertThat(book.getId()).isEqualTo(id);
            assertThat(book.getTitle()).isEqualTo(title);
            assertThat(book.getIsbn()).isEqualTo(isbn);
            assertThat(book.getAuthorIds()).isEqualTo(authorIds);
        }

        @Test
        @DisplayName("should throw when title is null")
        void shouldThrowWhenTitleNull() {
            assertThatThrownBy(() ->
                Book.builder()
                    .id(BookId.generate())
                    .title(null)
                    .isbn(ISBN.fromString("9780132350884"))
                    .authorIds(Set.of(AuthorId.generate()))
                    .build()
            ).isInstanceOf(NullPointerException.class)
             .hasMessageContaining("Title is required");
        }

        @Test
        @DisplayName("should have default status AVAILABLE")
        void shouldHaveDefaultStatusAvailable() {
            Book book = aBook().build();

            assertThat(book.getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        }
    }

    @Nested
    @DisplayName("checkOut")
    class CheckOut {

        @Test
        @DisplayName("should change status to CHECKED_OUT")
        void shouldChangeStatus() {
            Book book = aBook().available().build();

            book.checkOut();

            assertThat(book.getStatus()).isEqualTo(AvailabilityStatus.CHECKED_OUT);
        }

        @Test
        @DisplayName("should increment checkout count")
        void shouldIncrementCheckoutCount() {
            Book book = aBook().available().build();
            int initialCount = book.getCheckoutCount();

            book.checkOut();

            assertThat(book.getCheckoutCount()).isEqualTo(initialCount + 1);
        }

        @Test
        @DisplayName("should update timestamp")
        void shouldUpdateTimestamp() {
            Book book = aBook().available().build();
            LocalDateTime before = LocalDateTime.now();

            book.checkOut();

            LocalDateTime after = LocalDateTime.now();
            assertThat(book.getUpdatedAt()).isBetween(before, after);
        }

        @ParameterizedTest
        @EnumSource(
            value = AvailabilityStatus.class,
            names = {"AVAILABLE"},
            mode = EnumSource.Mode.EXCLUDE
        )
        @DisplayName("should throw when status is not AVAILABLE")
        void shouldThrowWhenNotAvailable(AvailabilityStatus status) {
            Book book = aBook().withStatus(status).build();

            assertThatThrownBy(() -> book.checkOut())
                .isInstanceOf(BookNotAvailableException.class)
                .hasMessageContaining(status.toString());
        }
    }

    @Nested
    @DisplayName("returnToLibrary")
    class ReturnToLibrary {

        @Test
        @DisplayName("should change status to AVAILABLE")
        void shouldChangeStatus() {
            Book book = aBook().checkedOut().build();

            book.returnToLibrary();

            assertThat(book.getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        }

        @Test
        @DisplayName("should not decrement checkout count")
        void shouldNotDecrementCheckoutCount() {
            Book book = aBook().checkedOut().build();
            int countBefore = book.getCheckoutCount();

            book.returnToLibrary();

            assertThat(book.getCheckoutCount()).isEqualTo(countBefore);
        }

        @Test
        @DisplayName("should throw when not checked out")
        void shouldThrowWhenNotCheckedOut() {
            Book book = aBook().available().build();

            assertThatThrownBy(() -> book.returnToLibrary())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not checked out");
        }
    }
}
```

### Example 2: ISBN Value Object Tests

```java
@DisplayName("ISBN")
class ISBNTest {

    @Nested
    @DisplayName("valid ISBNs")
    class ValidISBNs {

        @ParameterizedTest
        @ValueSource(strings = {
            "0132350882",
            "043942089X",
            "9780132350884",
            "9780321125217"
        })
        @DisplayName("should accept valid ISBN")
        void shouldAcceptValidISBN(String valid) {
            ISBN isbn = ISBN.fromString(valid);

            assertThat(isbn.value()).isNotNull();
        }

        @Test
        @DisplayName("should remove spaces and dashes")
        void shouldRemoveFormatting() {
            ISBN isbn = ISBN.fromString("978-0-13-235088-4");

            assertThat(isbn.value()).isEqualTo("9780132350884");
        }
    }

    @Nested
    @DisplayName("invalid ISBNs")
    class InvalidISBNs {

        @ParameterizedTest
        @MethodSource("invalidISBNs")
        @DisplayName("should reject invalid ISBN")
        void shouldRejectInvalidISBN(String invalid) {
            assertThatThrownBy(() -> ISBN.fromString(invalid))
                .isInstanceOf(IllegalArgumentException.class);
        }

        static Stream<String> invalidISBNs() {
            return Stream.of(
                "123",              // Too short
                "12345678901234",   // Too long
                "abcdefghij",       // Letters
                "",                 // Empty
                "97801323508"       // 11 digits
            );
        }

        @Test
        @DisplayName("should throw for null ISBN")
        void shouldThrowForNull() {
            assertThatThrownBy(() -> ISBN.fromString(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ISBN cannot be null");
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("should be equal when same ISBN")
        void shouldBeEqualWhenSame() {
            ISBN isbn1 = ISBN.fromString("978-0-13-235088-4");
            ISBN isbn2 = ISBN.fromString("9780132350884");

            assertThat(isbn1).isEqualTo(isbn2);
        }

        @Test
        @DisplayName("should have same hashCode when equal")
        void shouldHaveSameHashCode() {
            ISBN isbn1 = ISBN.fromString("978-0-13-235088-4");
            ISBN isbn2 = ISBN.fromString("9780132350884");

            assertThat(isbn1.hashCode()).isEqualTo(isbn2.hashCode());
        }
    }
}
```

---

## Action Items: Building Bibby's Unit Test Suite

### Action Item 1: Create Test Infrastructure (30 min)

**Create test builder** for Book:

`src/test/java/com/penrose/bibby/library/book/BookTestBuilder.java`:
```java
public class BookTestBuilder {
    private BookId id = BookId.generate();
    private Title title = Title.of("Test Book");
    private ISBN isbn = ISBN.fromString("9780132350884");
    private Set<AuthorId> authorIds = Set.of(AuthorId.generate());
    private AvailabilityStatus status = AvailabilityStatus.AVAILABLE;
    private int checkoutCount = 0;

    public static BookTestBuilder aBook() {
        return new BookTestBuilder();
    }

    public BookTestBuilder withTitle(String title) {
        this.title = Title.of(title);
        return this;
    }

    public BookTestBuilder withStatus(AvailabilityStatus status) {
        this.status = status;
        return this;
    }

    public BookTestBuilder available() {
        this.status = AvailabilityStatus.AVAILABLE;
        return this;
    }

    public BookTestBuilder checkedOut() {
        this.status = AvailabilityStatus.CHECKED_OUT;
        this.checkoutCount = 1;
        return this;
    }

    public Book build() {
        return Book.builder()
            .id(id)
            .title(title)
            .isbn(isbn)
            .authorIds(authorIds)
            .status(status)
            .checkoutCount(checkoutCount)
            .build();
    }
}
```

### Action Item 2: Write Complete Book Tests (3-4 hours)

**Create** `BookTest.java` with:
- ✅ Creation tests (5 tests)
- ✅ Checkout tests (5 tests)
- ✅ Return tests (3 tests)
- ✅ Shelf placement tests (3 tests)

**Target**: 20+ tests for Book domain.

### Action Item 3: Write Value Object Tests (2 hours)

Test all value objects:
- **ISBNTest**: 15+ tests
- **TitleTest**: 10+ tests
- **BookIdTest**: 5+ tests
- **PublisherTest**: 8+ tests

### Action Item 4: Add Parameterized Tests (1 hour)

Convert repetitive tests to parameterized tests:
- ISBN validation (10 valid + 10 invalid = 1 test each)
- Title validation
- Late fee calculation

### Action Item 5: Run and Measure Coverage (30 min)

```bash
mvn clean test
mvn jacoco:report
open target/site/jacoco/index.html
```

**Target**:
- Book domain: 95%+
- Value objects: 100%
- Overall: 80%+

---

## Key Takeaways

### 1. Unit Tests are Fast and Isolated

Test pure domain logic without external dependencies.

### 2. AAA Pattern Makes Tests Clear

Arrange → Act → Assert. Every test follows this structure.

### 3. Good Test Names Document Behavior

`shouldCheckOutBookWhenAvailable` is better than `test1`.

### 4. AssertJ Makes Assertions Fluent

`assertThat(actual).isEqualTo(expected)` is more readable.

### 5. Parameterized Tests Reduce Duplication

One test, many inputs.

### 6. @Nested Organizes Tests

Group related tests for better structure.

---

## Study Resources

### Books
1. **"Unit Testing Principles, Practices, and Patterns"** by Vladimir Khorikov
   - Comprehensive guide
   - What to test, what not to test

2. **"Effective Unit Testing"** by Lasse Koskela
   - Practical advice
   - Real-world examples

### Documentation
1. **JUnit 5 User Guide** - https://junit.org/junit5/docs/current/user-guide/
2. **AssertJ Documentation** - https://assertj.github.io/doc/

---

## Coming Next

**Section 25: Mocking & Test Doubles** - Testing with dependencies

We'll cover:
- Types of test doubles (stub, mock, spy, fake)
- When to use mocks vs. real objects
- Mockito framework
- Testing services with mocked repositories
- Avoiding over-mocking

---

**Section 24 Complete** | **Time Invested**: 4-5 hours

You now have the fundamentals to write excellent unit tests. Master these techniques, and you'll build a rock-solid test suite that gives you confidence to refactor and add features fearlessly.
