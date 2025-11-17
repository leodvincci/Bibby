# SECTION 23: PRAGMATIC TESTING

## Introduction: Test Early, Test Often, Test Automatically

**"Code without tests is broken by design."** - Jacob Kaplan-Moss

You've built Bibby with domain models, value objects, and orthogonal services. But how do you **know** it works? How do you **keep** it working as you add features? How do you **refactor with confidence**?

**The answer**: Tests.

Not just any tests. **Pragmatic tests** that:
- Run fast (seconds, not minutes)
- Test the right things (business logic, not getters)
- Give clear feedback (exact failure reason)
- Enable refactoring (change implementation without changing tests)

This section covers the pragmatic approach to testing and applies it to your Bibby codebase.

**Learning Objectives**:
- Understand the testing pyramid
- Become "test-infected"
- Write testable code
- Apply property-based testing
- Build a comprehensive test suite for Bibby

**Time Investment**: 3-4 hours

---

## The Testing Pyramid

### Concept

The testing pyramid shows the **ideal distribution** of tests:

```
        /\
       /  \        E2E Tests
      /────\       (Few, Slow, Expensive)
     /      \
    /        \     Integration Tests
   /──────────\    (Some, Moderate Speed)
  /            \
 /              \  Unit Tests
/────────────────\ (Many, Fast, Cheap)
```

**Bottom (Unit Tests)**:
- **Many** (70-80% of total tests)
- **Fast** (milliseconds per test)
- **Cheap** (easy to write and maintain)
- **Focused** (test one unit in isolation)

**Middle (Integration Tests)**:
- **Some** (15-25% of total tests)
- **Moderate speed** (seconds per test)
- **Test interactions** (service + repository + database)

**Top (E2E Tests)**:
- **Few** (5-10% of total tests)
- **Slow** (seconds to minutes per test)
- **Expensive** (fragile, hard to maintain)
- **Test whole system** (CLI command → service → database → output)

### Why This Shape?

**Anti-Pattern: Inverted Pyramid**
```
        /────────────────\
       /  Many E2E Tests  \  ❌ SLOW!
      /                    \
     /  Few Integration     \
    /────────────────────────\
   /  Almost No Unit Tests    \
  /──────────────────────────────\
```

**Problems**:
- Test suite takes 30+ minutes to run
- Can't run tests during development
- Failures are cryptic ("Button click failed" - which layer broke?)
- Brittle (UI changes break 100 tests)

**Pragmatic: Testing Pyramid**
```
        /\  E2E: 5 critical paths
       /──\
      /    \  Integration: 20 key scenarios
     /──────\
    /        \  Unit: 200+ domain logic tests
   /──────────\
  /            \
```

**Benefits**:
- Test suite runs in **under 10 seconds**
- Tests during development (after every change)
- Failures pinpoint exact problem
- Refactor with confidence

---

## Test-Infected: A Good Thing

### What Does "Test-Infected" Mean?

**Test-infected**: Once you experience the power of good tests, **you can't code without them**.

**Before test-infection**:
```java
// Write code
public void checkOut() {
    this.status = AvailabilityStatus.CHECKED_OUT;
    this.checkoutCount++;
}

// Manually test by running app
// bibby> checkout --book-id 42
// "Looks good!"

// Ship it! 🚀
```

**After test-infection**:
```java
// Write test FIRST
@Test
void shouldCheckOutAvailableBook() {
    Book book = aBook().available().build();

    book.checkOut();

    assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
    assertThat(book.getCheckoutCount()).isEqualTo(1);
}

// Write implementation to make test pass
public void checkOut() {
    if (this.status != AvailabilityStatus.AVAILABLE) {
        throw new BookNotAvailableException();
    }
    this.status = AvailabilityStatus.CHECKED_OUT;
    this.checkoutCount++;
}

// Run test (instant feedback!)
// ✅ PASS

// Ship it with confidence! 🚀
```

### Benefits of Being Test-Infected

**1. Confidence to Refactor**

**Without tests**:
> "I want to refactor this god class, but I'm scared I'll break something..."

**With tests**:
> "I'll refactor, run tests. If they pass, I didn't break anything!"

**Example**:
```java
// Before refactoring
public class BookService {
    public void checkOut(Long bookId) {
        // 50 lines of code
    }
}

// After refactoring
public class CheckOutBookService {
    public void checkOut(BookId bookId) {
        // Cleaner code
    }
}

// Run tests
// ✅ All 20 checkout tests pass
// Refactoring successful!
```

**2. Living Documentation**

Tests show **how to use your code**:

```java
@Test
void shouldCalculateLateFeeForOverdueBook() {
    // GIVEN: Book checked out 20 days ago
    Loan loan = aLoan()
        .checkedOut(LocalDate.now().minusDays(20))
        .dueDate(LocalDate.now().minusDays(6))  // 6 days overdue
        .build();

    // WHEN: Calculate fee
    Money lateFee = loan.calculateLateFee(LocalDate.now());

    // THEN: Fee is $3.50 (first 7 days free, 6 days @ $0.50/day)
    assertThat(lateFee).isEqualTo(Money.of(3.00));
}
```

**This test documents**:
- How to create a loan
- How late fees are calculated
- What the business rules are

Better than comments (comments lie, tests don't).

**3. Design Feedback**

**Hard-to-test code = bad design.**

If you can't write a test easily, your code is probably:
- Too coupled
- Doing too much
- Missing abstractions

**Example**:

**Hard to test** (bad design):
```java
public class BookService {
    public void checkOut(Long bookId) {
        BookEntity book = bookRepository.findById(bookId).get();  // Database!
        book.setStatus("CHECKED_OUT");
        bookRepository.save(book);  // Database!
        emailService.sendEmail("Book checked out");  // Email!
        log.info("Checked out");  // Logging!
    }
}

// Test requires: database, email service, logger
// This is an INTEGRATION test, not a unit test!
```

**Easy to test** (good design):
```java
public class Book {
    public void checkOut() {
        if (this.status != AvailabilityStatus.AVAILABLE) {
            throw new BookNotAvailableException();
        }
        this.status = AvailabilityStatus.CHECKED_OUT;
        this.checkoutCount++;
    }
}

// Test requires: NOTHING! Pure domain logic
@Test
void shouldCheckOut() {
    Book book = aBook().available().build();
    book.checkOut();
    assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
}
```

**Tests guided you to better design** (domain logic in domain model, not service).

---

## Writing Testable Code

### Principle 1: Orthogonality Enables Testability

Remember **Section 20: Orthogonality**? Components that don't depend on each other are **easy to test**.

**Non-orthogonal** (hard to test):
```java
@Service
public class BookService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final EmailService emailService;
    private final LoggingService loggingService;

    public void checkOut(Long bookId) {
        // Uses all dependencies
        // Test requires: database, email server, logger
    }
}
```

**Orthogonal** (easy to test):
```java
// Domain model (no dependencies!)
public class Book {
    public void checkOut() {
        // Pure logic
    }
}

// Application service (thin orchestration)
@Service
public class CheckOutBookService {
    private final BookRepository bookRepository;

    public void checkOut(BookId bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow();
        book.checkOut();  // Domain logic tested separately!
        bookRepository.save(book);
    }
}
```

**Testing strategy**:
- **Unit test** `Book.checkOut()` - no mocks needed!
- **Integration test** `CheckOutBookService` - one dependency (repository)

### Principle 2: Dependency Injection Enables Mocking

**Hard to test** (concrete dependencies):
```java
public class BookService {
    private final PostgreSQLBookRepository bookRepository = new PostgreSQLBookRepository();

    // Can't test without PostgreSQL!
}
```

**Easy to test** (injected interfaces):
```java
public class CheckOutBookService {
    private final BookRepository bookRepository;  // Interface!

    public CheckOutBookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }
}

// Test with in-memory repository
@Test
void shouldCheckOutBook() {
    BookRepository repo = new InMemoryBookRepository();
    CheckOutBookService service = new CheckOutBookService(repo);

    // Test without database!
}
```

### Principle 3: Pure Functions Are Easiest to Test

**Pure function**: Same input → same output, no side effects.

**Impure** (hard to test):
```java
public Money calculateLateFee(Long loanId) {
    Loan loan = loanRepository.findById(loanId);  // Database call!
    LocalDate today = LocalDate.now();  // Non-deterministic!
    return loan.daysLate(today) * DAILY_FEE;
}

// How do you test "today"?
```

**Pure** (easy to test):
```java
public Money calculateLateFee(Loan loan, LocalDate today) {
    long daysLate = loan.daysLate(today);
    return DAILY_FEE.multiply(daysLate);
}

// Test with any date!
@Test
void shouldCalculateFee() {
    Loan loan = aLoan().dueDate(LocalDate.of(2024, 1, 1)).build();
    LocalDate today = LocalDate.of(2024, 1, 10);  // 9 days late

    Money fee = calculateLateFee(loan, today);

    assertThat(fee).isEqualTo(Money.of(4.50));  // 9 * $0.50
}
```

---

## Testing Bibby: Unit Tests

### Example 1: Testing Book.checkOut()

**Production code** (from Section 22):
```java
public class Book {
    private AvailabilityStatus status;
    private int checkoutCount;

    public void checkOut() {
        if (this.status != AvailabilityStatus.AVAILABLE) {
            throw new BookNotAvailableException(
                "Cannot check out book - status: " + this.status
            );
        }
        this.status = AvailabilityStatus.CHECKED_OUT;
        this.checkoutCount++;
    }
}
```

**Test suite**:
```java
import static com.penrose.bibby.library.book.BookTestBuilder.*;
import static org.assertj.core.api.Assertions.*;

class BookCheckOutTest {

    @Test
    void shouldCheckOutAvailableBook() {
        // GIVEN: An available book
        Book book = aBook()
            .withStatus(AvailabilityStatus.AVAILABLE)
            .build();

        // WHEN: Check out
        book.checkOut();

        // THEN: Status is checked out, count incremented
        assertThat(book.getStatus()).isEqualTo(AvailabilityStatus.CHECKED_OUT);
        assertThat(book.getCheckoutCount()).isEqualTo(1);
    }

    @Test
    void shouldIncrementCheckoutCountOnMultipleCheckouts() {
        Book book = aBook().available().build();

        book.checkOut();
        book.returnToLibrary();
        book.checkOut();

        assertThat(book.getCheckoutCount()).isEqualTo(2);
    }

    @Test
    void shouldThrowExceptionWhenCheckingOutCheckedOutBook() {
        Book book = aBook().available().build();
        book.checkOut();  // Already checked out

        assertThatThrownBy(() -> book.checkOut())
            .isInstanceOf(BookNotAvailableException.class)
            .hasMessageContaining("status: CHECKED_OUT");
    }

    @Test
    void shouldThrowExceptionWhenCheckingOutLostBook() {
        Book book = aBook().withStatus(AvailabilityStatus.LOST).build();

        assertThatThrownBy(() -> book.checkOut())
            .isInstanceOf(BookNotAvailableException.class)
            .hasMessageContaining("status: LOST");
    }

    @Test
    void shouldUpdateTimestampWhenCheckingOut() {
        Book book = aBook().available().build();
        LocalDateTime before = LocalDateTime.now();

        book.checkOut();

        LocalDateTime after = LocalDateTime.now();
        assertThat(book.getUpdatedAt()).isBetween(before, after);
    }
}
```

**5 tests, all run in milliseconds, no dependencies!**

### Example 2: Testing ISBN Value Object

**Production code**:
```java
public record ISBN(String value) {
    public ISBN {
        Objects.requireNonNull(value, "ISBN cannot be null");
        String cleaned = value.replaceAll("[\\s-]", "");

        if (!isValid(cleaned)) {
            throw new IllegalArgumentException("Invalid ISBN: " + value);
        }

        value = cleaned;
    }

    private static boolean isValid(String isbn) {
        return isbn.matches("^\\d{9}[\\dX]$") || isbn.matches("^\\d{13}$");
    }

    public static ISBN fromString(String raw) {
        return new ISBN(raw);
    }
}
```

**Test suite**:
```java
class ISBNTest {

    @Test
    void shouldCreateValidISBN10() {
        ISBN isbn = ISBN.fromString("0132350882");

        assertThat(isbn.value()).isEqualTo("0132350882");
    }

    @Test
    void shouldCreateValidISBN13() {
        ISBN isbn = ISBN.fromString("9780132350884");

        assertThat(isbn.value()).isEqualTo("9780132350884");
    }

    @Test
    void shouldAcceptISBN10WithXCheckDigit() {
        ISBN isbn = ISBN.fromString("043942089X");

        assertThat(isbn.value()).isEqualTo("043942089X");
    }

    @Test
    void shouldRemoveSpacesAndDashes() {
        ISBN isbn = ISBN.fromString("978-0-13-235088-4");

        assertThat(isbn.value()).isEqualTo("9780132350884");
    }

    @Test
    void shouldThrowExceptionForNullISBN() {
        assertThatThrownBy(() -> ISBN.fromString(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("ISBN cannot be null");
    }

    @Test
    void shouldThrowExceptionForInvalidFormat() {
        assertThatThrownBy(() -> ISBN.fromString("invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid ISBN: invalid");
    }

    @Test
    void shouldThrowExceptionForTooShort() {
        assertThatThrownBy(() -> ISBN.fromString("123"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowExceptionForTooLong() {
        assertThatThrownBy(() -> ISBN.fromString("12345678901234"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldBeEqualWhenSameISBN() {
        ISBN isbn1 = ISBN.fromString("978-0-13-235088-4");
        ISBN isbn2 = ISBN.fromString("9780132350884");

        assertThat(isbn1).isEqualTo(isbn2);
    }
}
```

**9 comprehensive tests covering all edge cases!**

---

## Property-Based Testing

### Concept

**Example-based testing** (what we've been doing):
```java
@Test
void shouldCalculateLateFee() {
    Loan loan = aLoan().dueDate(LocalDate.of(2024, 1, 1)).build();
    Money fee = loan.calculateLateFee(LocalDate.of(2024, 1, 10));
    assertThat(fee).isEqualTo(Money.of(4.50));  // Specific example
}
```

**Property-based testing** (test properties that should always hold):
```java
@Property
void lateFeeShouldNeverBeNegative(@ForAll Loan loan, @ForAll LocalDate today) {
    Money fee = loan.calculateLateFee(today);

    assertThat(fee.isNegativeOrZero()).isFalse();
    // Property: late fee is always >= 0
}

@Property
void lateFeeShouldIncreaseWithDays(
    @ForAll Loan loan,
    @ForAll @IntRange(min = 1, max = 30) int extraDays
) {
    LocalDate today = loan.getDueDate().plusDays(1);
    Money fee1 = loan.calculateLateFee(today);

    LocalDate later = today.plusDays(extraDays);
    Money fee2 = loan.calculateLateFee(later);

    assertThat(fee2).isGreaterThanOrEqualTo(fee1);
    // Property: later = higher fee (or equal if capped)
}
```

**Framework generates random inputs** and tests the property holds for all of them.

### Property-Based Testing in Bibby

**Example: ISBN properties**

```java
import net.jqwik.api.*;

class ISBNPropertiesTest {

    @Property
    void validISBN10ShouldBe10Digits(@ForAll("validISBN10") String isbn10) {
        ISBN isbn = ISBN.fromString(isbn10);

        assertThat(isbn.value()).hasSize(10);
        assertThat(isbn.value()).matches("\\d{9}[\\dX]");
    }

    @Property
    void validISBN13ShouldBe13Digits(@ForAll("validISBN13") String isbn13) {
        ISBN isbn = ISBN.fromString(isbn13);

        assertThat(isbn.value()).hasSize(13);
        assertThat(isbn.value()).matches("\\d{13}");
    }

    @Property
    void formattedAndUnformattedShouldBeEqual(
        @ForAll("validISBN13") String isbn
    ) {
        String formatted = formatISBN(isbn);  // Add dashes
        String unformatted = isbn;

        ISBN isbn1 = ISBN.fromString(formatted);
        ISBN isbn2 = ISBN.fromString(unformatted);

        assertThat(isbn1).isEqualTo(isbn2);
        // Property: formatting doesn't change ISBN
    }

    @Provide
    Arbitrary<String> validISBN10() {
        return Arbitraries.strings()
            .numeric()
            .ofLength(9)
            .map(s -> s + "X");  // Simple: always use X as check digit
    }

    @Provide
    Arbitrary<String> validISBN13() {
        return Arbitraries.strings()
            .numeric()
            .ofLength(13);
    }

    private String formatISBN(String isbn) {
        if (isbn.length() == 13) {
            return isbn.substring(0, 3) + "-" +
                   isbn.substring(3, 4) + "-" +
                   isbn.substring(4, 7) + "-" +
                   isbn.substring(7, 12) + "-" +
                   isbn.substring(12);
        }
        return isbn;
    }
}
```

**jqwik generates 1000 random ISBNs** and tests all properties hold!

---

## Action Items: Building Bibby's Test Suite

### Action Item 1: Set Up Testing Infrastructure (1 hour)

**Add dependencies** to `pom.xml`:
```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ (fluent assertions) -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.24.2</version>
        <scope>test</scope>
    </dependency>

    <!-- Mockito (mocking) -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.5.0</version>
        <scope>test</scope>
    </dependency>

    <!-- jqwik (property-based testing) -->
    <dependency>
        <groupId>net.jqwik</groupId>
        <artifactId>jqwik</artifactId>
        <version>1.8.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**Create test directory structure**:
```
src/test/java/com/penrose/bibby/
  ├── library/
  │   ├── book/
  │   │   ├── BookTest.java
  │   │   ├── ISBNTest.java
  │   │   └── BookTestBuilder.java
  │   └── author/
  │       └── AuthorTest.java
  └── application/
      └── book/
          └── CheckOutBookServiceTest.java
```

### Action Item 2: Write Unit Tests for Book Domain (2-3 hours)

**Create** `BookTest.java`:
```java
@DisplayName("Book")
class BookTest {

    @Nested
    @DisplayName("checkout")
    class CheckOut {

        @Test
        void shouldCheckOutAvailableBook() {
            Book book = aBook().available().build();

            book.checkOut();

            assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
        }

        @Test
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
        void shouldReturnCheckedOutBook() {
            Book book = aBook().checkedOut().build();

            book.returnToLibrary();

            assertThat(book.getStatus()).isEqualTo(AVAILABLE);
        }

        @Test
        void shouldThrowWhenNotCheckedOut() {
            Book book = aBook().available().build();

            assertThatThrownBy(() -> book.returnToLibrary())
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
```

**Target**: 20+ tests for Book domain logic.

### Action Item 3: Write Tests for Value Objects (1-2 hours)

**Test all value objects**:
- `ISBNTest` (9 tests - shown above)
- `TitleTest` (6 tests - null, blank, too long, trimming)
- `PublisherTest` (similar to Title)
- `BookIdTest` (equality, generation, parsing)

**Example** `TitleTest.java`:
```java
class TitleTest {

    @Test
    void shouldCreateValidTitle() {
        Title title = Title.of("Clean Code");

        assertThat(title.value()).isEqualTo("Clean Code");
    }

    @Test
    void shouldTrimWhitespace() {
        Title title = Title.of("  Clean Code  ");

        assertThat(title.value()).isEqualTo("Clean Code");
    }

    @Test
    void shouldThrowForNullTitle() {
        assertThatThrownBy(() -> Title.of(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldThrowForBlankTitle() {
        assertThatThrownBy(() -> Title.of("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowForTooLongTitle() {
        String longTitle = "x".repeat(501);

        assertThatThrownBy(() -> Title.of(longTitle))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("max 500");
    }

    @Test
    void shouldAcceptMaxLengthTitle() {
        String maxTitle = "x".repeat(500);

        Title title = Title.of(maxTitle);

        assertThat(title.value()).hasSize(500);
    }
}
```

### Action Item 4: Write Integration Tests (2 hours)

**Test service + repository** (but not full E2E):

```java
@SpringBootTest
@Transactional
class CheckOutBookServiceIntegrationTest {

    @Autowired
    private CheckOutBookService checkOutService;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldCheckOutExistingBook() {
        // GIVEN: Book exists in database
        Book book = aBook().available().build();
        bookRepository.save(book);

        // WHEN: Check out
        checkOutService.checkOut(book.getId());

        // THEN: Book is checked out in database
        Book updated = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CHECKED_OUT);
    }

    @Test
    void shouldThrowWhenBookDoesNotExist() {
        BookId nonExistentId = BookId.generate();

        assertThatThrownBy(() -> checkOutService.checkOut(nonExistentId))
            .isInstanceOf(BookNotFoundException.class);
    }
}
```

**Target**: 10-15 integration tests for critical paths.

### Action Item 5: Write Property-Based Tests (1 hour)

**Create** `ISBNPropertiesTest.java` (shown above).

**Add properties for**:
- Late fee calculation
- BookId generation (always unique)
- Title trimming (always produces trimmed result)

### Action Item 6: Measure Test Coverage (30 min)

**Add JaCoCo plugin** to `pom.xml`:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Run**:
```bash
mvn clean test
mvn jacoco:report
```

**Open**: `target/site/jacoco/index.html`

**Target coverage**:
- Domain model: 90%+
- Application services: 80%+
- Infrastructure (repositories): 60%+ (integration tests)
- Overall: 75%+

---

## Key Takeaways

### 1. The Testing Pyramid is Your Guide

- **Many fast unit tests** (70-80%)
- **Some integration tests** (15-25%)
- **Few E2E tests** (5-10%)

### 2. Tests Enable Confidence

- Refactor without fear
- Add features without breaking existing ones
- Document how code works

### 3. Testable Code is Better Code

- Orthogonal components test easily
- Pure functions test easily
- Dependency injection enables mocking

### 4. Test Business Logic, Not Frameworks

- Test `Book.checkOut()` (your code)
- Don't test `repository.save()` (Spring's code)
- Don't test getters/setters

### 5. Property-Based Testing Finds Edge Cases

- Example tests: "It works for this case"
- Property tests: "It works for ALL cases"

---

## Study Resources

### Books
1. **"Growing Object-Oriented Software, Guided by Tests"** by Freeman & Pryce
   - Test-driven development
   - Test data builders
   - Essential reading!

2. **"Unit Testing Principles, Practices, and Patterns"** by Vladimir Khorikov
   - What to test, what not to test
   - Test maintainability

3. **"The Pragmatic Programmer"** - Testing chapters
   - Test early, test often

### Frameworks
1. **JUnit 5** - Standard Java testing framework
   - https://junit.org/junit5/

2. **AssertJ** - Fluent assertions
   - https://assertj.github.io/doc/

3. **jqwik** - Property-based testing for Java
   - https://jqwik.net/

4. **Testcontainers** - Integration tests with Docker
   - https://www.testcontainers.org/

### Articles
1. **"Test Pyramid" by Martin Fowler**
   - https://martinfowler.com/bliki/TestPyramid.html

2. **"Property-Based Testing"** by Jessica Kerr
   - Introduction to property-based testing

---

## Coming Next

**Part 4: Java Testing Mastery** (Sections 24-29)

- Section 24: Unit Testing Fundamentals
- Section 25: Mocking & Test Doubles
- Section 26: Integration Testing
- Section 27: Test-Driven Development (TDD)
- Section 28: Testing Best Practices & Patterns
- Section 29: Testing Anti-Patterns

---

**Section 23 Complete** | **Part 3: The Pragmatic Programmer COMPLETE!** | **Time Invested**: 3-4 hours

You've completed Part 3! You now understand:
- Pragmatic philosophy (care about your craft)
- DRY (single source of truth)
- Orthogonality (independent components)
- Tracer bullets (incremental development)
- Debugging & defensive programming
- Pragmatic testing

**You're now a pragmatic programmer.** Time to dive deep into testing in Part 4!
