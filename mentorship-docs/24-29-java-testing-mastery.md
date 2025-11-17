# SECTIONS 24-29: JAVA TESTING MASTERY

## Writing Tests That Actually Matter

Part 4 focuses exclusively on testing - the skill that separates professional developers from hobbyists. These principles apply to Bibby and every project in your career.

---

## SECTION 24: UNIT TESTING FUNDAMENTALS

### F.I.R.S.T. Principles

Great unit tests are:
- **Fast**: Run in milliseconds
- **Independent**: No dependencies between tests
- **Repeatable**: Same result every time
- **Self-Validating**: Pass/fail, no manual inspection
- **Timely**: Written with (or before) production code

### AAA Pattern (Arrange-Act-Assert)

```java
@Test
void shouldCheckOutAvailableBook() {
    // Arrange - set up test data
    Book book = Book.builder()
        .id(BookId.generate())
        .title(new Title("Clean Code"))
        .isbn(ISBN.fromString("978-0132350884"))
        .authors(Set.of(authorId))
        .build();

    // Act - perform the operation
    book.checkOut();

    // Assert - verify the outcome
    assertThat(book.getStatus()).isEqualTo(AvailabilityStatus.CHECKED_OUT);
    assertThat(book.getCheckoutCount()).isEqualTo(1);
}
```

### Test Naming Conventions

❌ Bad:
```java
@Test void test1() { }
@Test void testBook() { }
@Test void checkOut() { }
```

✓ Good:
```java
@Test void shouldCheckOutAvailableBook() { }
@Test void shouldThrowExceptionWhenCheckingOutUnavailableBook() { }
@Test void shouldIncrementCheckoutCountWhenCheckingOut() { }

// Pattern: should[ExpectedBehavior]When[Condition]
```

### JUnit 5 Essentials for Bibby

```java
// Basic test
@Test
void shouldCreateBookWithValidData() {
    // test implementation
}

// Parameterized tests - test multiple inputs
@ParameterizedTest
@ValueSource(strings = {"978-0132350884", "978-0321125215", "0-13-468599-0"})
void shouldAcceptValidIsbn(String isbnString) {
    ISBN isbn = ISBN.fromString(isbnString);
    assertThat(isbn).isNotNull();
}

// Test exceptions
@Test
void shouldThrowExceptionForInvalidIsbn() {
    assertThatThrownBy(() -> ISBN.fromString("invalid"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid ISBN");
}

// Setup and teardown
@BeforeEach
void setUp() {
    // Runs before each test
    testBook = aBook().build();
}

@AfterEach
void tearDown() {
    // Runs after each test
    // Clean up resources
}

// Display names for better readability
@Test
@DisplayName("Should successfully check out an available book")
void checkOutTest() {
    // test implementation
}
```

---

## SECTION 25: MOCKING & TEST DOUBLES

### Types of Test Doubles

1. **Dummy**: Passed but never used
2. **Stub**: Returns canned answers
3. **Spy**: Records information
4. **Mock**: Expects specific interactions
5. **Fake**: Working implementation (simplified)

### Mockito Basics

```java
@ExtendWith(MockitoExtension.class)
class AddBookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AddBookService addBookService;

    @Test
    void shouldSaveBookWhenAddingNewBook() {
        // Arrange
        AddBookCommand command = new AddBookCommand(/*...*/);
        Book expectedBook = aBook().build();

        // Stub - define return value
        when(authorRepository.findByName(any()))
            .thenReturn(Optional.of(author));

        // Act
        BookId result = addBookService.execute(command);

        // Assert - verify interaction
        verify(bookRepository).save(any(Book.class));

        // Capture argument for detailed assertion
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(bookCaptor.capture());
        Book savedBook = bookCaptor.getValue();

        assertThat(savedBook.getTitle()).isEqualTo(command.getTitle());
    }
}
```

### When to Mock vs When to Use Real Objects

**Mock**:
- External dependencies (database, HTTP clients)
- Slow operations
- Non-deterministic behavior (time, random)
- Testing error conditions

**Use Real**:
- Value objects (ISBN, Title)
- Domain entities (Book, Author)
- Simple collaborators
- When testing integration

**Example**:
```java
@Test
void shouldCalculateLateFeeCorrectly() {
    // DON'T mock domain objects
    Loan loan = new Loan(/*real loan*/);  // Real
    LocalDate returnDate = LocalDate.now();  // Real

    // Calculate with real objects
    Money fee = lateFeeCalculator.calculate(loan, returnDate);

    // Assert on real result
    assertThat(fee).isEqualTo(Money.of(5.00));
}

@Test
void shouldFetchBookMetadataFromExternalApi() {
    // DO mock external API
    @Mock IsbnApiClient apiClient;

    when(apiClient.lookup(any())).thenReturn(mockResponse);

    // Test with mock
}
```

### Over-Mocking Anti-Pattern

❌ Bad - testing implementation, not behavior:
```java
@Test
void shouldProcessBook() {
    // Mocking everything - brittle test!
    when(book.getTitle()).thenReturn(title);
    when(book.getAuthors()).thenReturn(authors);
    when(book.getIsbn()).thenReturn(isbn);
    when(formatter.format(title)).thenReturn("formatted");

    String result = processor.process(book);

    verify(book).getTitle();
    verify(book).getAuthors();
    verify(formatter).format(any());
    // Testing internal implementation!
}
```

✓ Good - testing behavior:
```java
@Test
void shouldProcessBook() {
    Book book = aBook()  // Real test data
        .withTitle("Clean Code")
        .build();

    String result = processor.process(book);

    assertThat(result).contains("Clean Code");
    // Testing actual behavior!
}
```

---

## SECTION 26: INTEGRATION TESTING

### Spring Boot Test Slices

**@DataJpaTest** - Just database layer:
```java
@DataJpaTest
class BookRepositoryIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldFindBookByIsbn() {
        // Arrange - real database!
        BookJpaEntity entity = new BookJpaEntity(/*...*/);
        entityManager.persist(entity);
        entityManager.flush();

        // Act
        Optional<Book> found = bookRepository.findByIsbn(isbn);

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Clean Code");
    }
}
```

**@WebMvcTest** - Just web layer:
```java
@WebMvcTest(BookController.class)
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean  // Mock service layer
    private AddBookService addBookService;

    @Test
    void shouldAddBookViaApi() throws Exception {
        when(addBookService.execute(any()))
            .thenReturn(new BookId(1L));

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Clean Code\",\"isbn\":\"978-0132350884\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.bookId").value(1));
    }
}
```

**@SpringBootTest** - Full context:
```java
@SpringBootTest
class BookWorkflowIntegrationTest {

    @Autowired
    private LibraryManagementFacade library;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldCompleteFullBookLifecycle() {
        // Add book
        BookOperationResult addResult = library.addBookByIsbn("978-0132350884");
        assertThat(addResult.isSuccess()).isTrue();

        BookId bookId = addResult.getBookId();

        // Check out
        library.checkOutBook(bookId);

        // Verify in database
        Book book = bookRepository.findById(bookId).orElseThrow();
        assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);

        // Return
        library.returnBook(bookId);

        // Verify again
        book = bookRepository.findById(bookId).orElseThrow();
        assertThat(book.getStatus()).isEqualTo(AVAILABLE);
    }
}
```

### TestContainers - Real Database Tests

```java
@SpringBootTest
@Testcontainers
class BookRepositoryRealDatabaseTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword());
    }

    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldPersistAndRetrieveBook() {
        // Real PostgreSQL database running in Docker!
        Book book = aBook().build();

        bookRepository.save(book);

        Optional<Book> found = bookRepository.findById(book.getId());
        assertThat(found).isPresent();
    }
}
```

---

## SECTION 27: TEST-DRIVEN DEVELOPMENT (TDD)

### The Red-Green-Refactor Cycle

```
1. RED    → Write failing test
2. GREEN  → Make it pass (simplest way)
3. REFACTOR → Clean up code
4. Repeat
```

### TDD Example: Implementing Late Fee Calculator

**Step 1 - Red (Write failing test)**:
```java
@Test
void shouldCalculateLateFeeForOneDayLate() {
    Loan loan = createLoanDueYesterday();

    Money fee = calculator.calculateLateFee(loan, LocalDate.now());

    assertThat(fee).isEqualTo(Money.of(0.50));
}
// Compile error - LateFeeCalculator doesn't exist!
```

**Step 2 - Green (Make it pass)**:
```java
public class LateFeeCalculator {
    public Money calculateLateFee(Loan loan, LocalDate returnDate) {
        return Money.of(0.50);  // Hardcoded - simplest thing!
    }
}
// Test passes!
```

**Step 3 - Red (Another test)**:
```java
@Test
void shouldCalculateLateFeeForFiveDaysLate() {
    Loan loan = createLoanDue(5, ChronoUnit.DAYS);

    Money fee = calculator.calculateLateFee(loan, LocalDate.now());

    assertThat(fee).isEqualTo(Money.of(2.50));  // 5 * $0.50
}
// Test fails - still hardcoded!
```

**Step 4 - Green (Implement properly)**:
```java
public class LateFeeCalculator {
    private static final Money PER_DAY_FEE = Money.of(0.50);

    public Money calculateLateFee(Loan loan, LocalDate returnDate) {
        long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);

        if (daysLate <= 0) {
            return Money.zero();
        }

        return PER_DAY_FEE.multiply(daysLate);
    }
}
// Both tests pass!
```

**Step 5 - Red (Edge case)**:
```java
@Test
void shouldCapLateFeeAtMaximum() {
    Loan loan = createLoanDue(100, ChronoUnit.DAYS);

    Money fee = calculator.calculateLateFee(loan, LocalDate.now());

    assertThat(fee).isEqualTo(Money.of(25.00));  // Cap at $25
}
// Test fails!
```

**Step 6 - Green**:
```java
private static final Money MAX_FEE = Money.of(25.00);

public Money calculateLateFee(Loan loan, LocalDate returnDate) {
    long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);

    if (daysLate <= 0) {
        return Money.zero();
    }

    Money calculatedFee = PER_DAY_FEE.multiply(daysLate);
    return calculatedFee.min(MAX_FEE);  // Cap it!
}
// All tests pass!
```

**Step 7 - Refactor**:
```java
public class LateFeeCalculator {
    private static final Money PER_DAY_FEE = Money.of(0.50);
    private static final Money MAX_FEE = Money.of(25.00);

    public Money calculateLateFee(Loan loan, LocalDate returnDate) {
        long daysLate = calculateDaysLate(loan, returnDate);

        if (daysLate <= 0) {
            return Money.zero();
        }

        return calculateFee(daysLate);
    }

    private long calculateDaysLate(Loan loan, LocalDate returnDate) {
        return ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);
    }

    private Money calculateFee(long daysLate) {
        Money calculatedFee = PER_DAY_FEE.multiply(daysLate);
        return calculatedFee.min(MAX_FEE);
    }
}
// Refactored, all tests still pass!
```

### Benefits of TDD

1. **Better design** - Forces you to think about API first
2. **Confidence** - Tests prove it works
3. **Regression prevention** - Future changes won't break it
4. **Documentation** - Tests show how to use the code

---

## SECTION 28: TESTING BEST PRACTICES & PATTERNS

### Test Data Builders (Object Mother Pattern)

```java
// Test data builder for Book
public class BookTestBuilder {
    private BookId id = BookId.generate();
    private Title title = new Title("Default Title");
    private ISBN isbn = ISBN.fromString("978-0000000000");
    private Set<AuthorId> authorIds = Set.of(AuthorId.generate());
    private AvailabilityStatus status = AvailabilityStatus.AVAILABLE;

    public static BookTestBuilder aBook() {
        return new BookTestBuilder();
    }

    public BookTestBuilder withId(BookId id) {
        this.id = id;
        return this;
    }

    public BookTestBuilder withTitle(String title) {
        this.title = new Title(title);
        return this;
    }

    public BookTestBuilder available() {
        this.status = AvailabilityStatus.AVAILABLE;
        return this;
    }

    public BookTestBuilder checkedOut() {
        this.status = AvailabilityStatus.CHECKED_OUT;
        return this;
    }

    public BookTestBuilder sciFi() {
        this.title = new Title("The Foundation");
        this.authorIds = Set.of(asimovId);
        return this;
    }

    public Book build() {
        Book book = Book.builder()
            .id(id)
            .title(title)
            .isbn(isbn)
            .authors(authorIds)
            .build();

        // Set status via reflection or friend method
        setStatus(book, status);

        return book;
    }
}

// Usage - beautiful, readable tests!
@Test
void shouldNotCheckOutAlreadyCheckedOutBook() {
    Book book = aBook().checkedOut().build();

    assertThatThrownBy(() -> book.checkOut())
        .isInstanceOf(BookNotAvailableException.class);
}
```

### Custom Assertions

```java
// Custom assertion for Book
public class BookAssert extends AbstractAssert<BookAssert, Book> {

    public BookAssert(Book book) {
        super(book, BookAssert.class);
    }

    public static BookAssert assertThatBook(Book book) {
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

    public BookAssert hasCheckoutCount(int expectedCount) {
        isNotNull();
        if (actual.getCheckoutCount() != expectedCount) {
            failWithMessage("Expected checkout count to be <%d> but was <%d>",
                expectedCount, actual.getCheckoutCount());
        }
        return this;
    }
}

// Usage - fluent, domain-specific assertions!
@Test
void shouldTrackCheckouts() {
    Book book = aBook().available().build();

    book.checkOut();

    assertThatBook(book)
        .isCheckedOut()
        .hasCheckoutCount(1);
}
```

### Parameterized Tests for Value Objects

```java
@ParameterizedTest
@CsvSource({
    "978-0132350884, true",   // Valid ISBN-13
    "0-13-235088-2, true",    // Valid ISBN-10
    "invalid, false",          // Invalid
    "123, false",              // Too short
    "'', false"                // Empty
})
void shouldValidateIsbnFormat(String input, boolean expectedValid) {
    if (expectedValid) {
        assertThatCode(() -> ISBN.fromString(input))
            .doesNotThrowAnyException();
    } else {
        assertThatThrownBy(() -> ISBN.fromString(input))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

---

## SECTION 29: TESTING ANTI-PATTERNS

### Anti-Pattern 1: Testing Private Methods

❌ Bad:
```java
public class Book {
    private boolean isValidForCheckout() {
        return status.isAvailable();
    }

    public void checkOut() {
        if (isValidForCheckout()) {
            // ...
        }
    }
}

// Test
@Test
void testIsValidForCheckout() {
    // Using reflection to test private method - WRONG!
    Method method = Book.class.getDeclaredMethod("isValidForCheckout");
    method.setAccessible(true);
    boolean result = (boolean) method.invoke(book);
}
```

✓ Good - test through public interface:
```java
@Test
void shouldOnlyCheckOutAvailableBooks() {
    Book availableBook = aBook().available().build();
    Book checkedOutBook = aBook().checkedOut().build();

    assertThatCode(() -> availableBook.checkOut())
        .doesNotThrowAnyException();

    assertThatThrownBy(() -> checkedOutBook.checkOut())
        .isInstanceOf(BookNotAvailableException.class);

    // Private method tested indirectly!
}
```

### Anti-Pattern 2: Excessive Mocking

❌ Bad - mocking everything:
```java
@Test
void processBookTest() {
    when(book.getTitle()).thenReturn(mockTitle);
    when(mockTitle.getValue()).thenReturn("title");
    when(book.getAuthors()).thenReturn(mockAuthors);
    when(mockAuthors.isEmpty()).thenReturn(false);
    // ... 20 more mocks

    // Testing nothing real!
}
```

✓ Good - test with real objects:
```java
@Test
void shouldProcessBook() {
    Book book = aBook()
        .withTitle("Clean Code")
        .withAuthor(martinId)
        .build();

    processor.process(book);

    // Real test with real objects
}
```

### Anti-Pattern 3: Tests Depending on Execution Order

❌ Bad:
```java
private static Book sharedBook;

@Test
void test1_addBook() {
    sharedBook = aBook().build();
    bookRepository.save(sharedBook);
}

@Test
void test2_checkOutBook() {
    // Depends on test1 running first!
    sharedBook.checkOut();
}
```

✓ Good - independent tests:
```java
@Test
void shouldAddBook() {
    Book book = aBook().build();
    bookRepository.save(book);
    // Complete test
}

@Test
void shouldCheckOutBook() {
    Book book = aBook().available().build();  // Fresh data
    bookRepository.save(book);

    book.checkOut();
    // Independent test
}
```

### Anti-Pattern 4: Sleeps in Tests

❌ Bad:
```java
@Test
void shouldProcessAsync() {
    service.processAsync(book);

    Thread.sleep(1000);  // Hope it's done...

    verify(repository).save(any());
}
```

✓ Good - use async testing tools:
```java
@Test
void shouldProcessAsync() {
    service.processAsync(book);

    await().atMost(Duration.ofSeconds(1))
        .untilAsserted(() -> verify(repository).save(any()));
}
```

---

## Consolidated Action Items for Part 4

### Week 1: Unit Testing Foundation
1. **Write unit tests for all domain entities** - Achieve 80% coverage
2. **Create test data builders** - For Book, Author, Bookcase
3. **Add parameterized tests** - For all value objects

### Week 2: Integration Testing
1. **Write @DataJpaTest tests** - For all repositories
2. **Set up TestContainers** - Real PostgreSQL tests
3. **Write @SpringBootTest tests** - For critical workflows

### Week 3: TDD Practice
1. **Implement one feature with TDD** - Red-Green-Refactor
2. **Create custom assertions** - For your domain types
3. **Fix all testing anti-patterns** - Review and refactor

### Master Testing Checklist

- [ ] 80% code coverage on domain layer
- [ ] All repositories have integration tests
- [ ] Test data builders for all aggregates
- [ ] Custom assertions for domain types
- [ ] No private method testing
- [ ] No tests with sleeps
- [ ] All tests are independent
- [ ] TestContainers configured
- [ ] Practice TDD on next feature

---

**Sections 24-29 Complete** | **Part 4: Java Testing Mastery Complete!**

Next: Part 5 - Integration & Synthesis (Sections 30-34)
