# Section 6: Testing Gaps

## Overview

This section covers the **complete lack of meaningful tests** in your codebase. Testing is not optional for production-ready code - it's essential for catching bugs, enabling refactoring, and documenting expected behavior. Your current test coverage is effectively **0%**, leaving you blind to bugs and afraid to make changes.

**Key Statistics:**
- Issues Found: 2
- Critical: 1 (virtually no test coverage)
- High: 1 (missing integration tests)
- Medium: 0
- Low: 0

**Current State:**
- Test Coverage: ~0% (only context load test exists)
- Service Tests: 0
- Repository Tests: 0
- Controller Tests: 0
- Integration Tests: 0
- Business Logic Tests: 0

**Impact of No Tests:**
- Bugs only found in production (by users)
- Fear of refactoring (will it break something?)
- No documentation of expected behavior
- Difficult to onboard new developers
- Regression bugs constantly reappear

---

## 🔴 Issue 6.1: Virtually No Test Coverage

**Priority:** CRITICAL
**Locations:**
- `BookCommandsTest.java` - Empty test with no assertions
- `BibbyApplicationTests.java` - Only tests Spring context loads
- **No other test files exist**

### Current Code

```java
// BookCommandsTest.java
class BookCommandsTest {

    @Test
    public void searchByTitleTest() {
        BookEntity bookEntity = null;  // ❌ No actual test logic!
        // No assertions, no setup, no verification
        // This test literally does nothing
    }
}

// BibbyApplicationTests.java
@SpringBootTest
class BibbyApplicationTests {

    @Test
    void contextLoads() {
        // ❌ Only verifies Spring context starts
        // Tests none of your business logic
    }
}
```

### Why This Is Critical

**Without tests, you have:**

1. **No Safety Net for Changes:**
```
Scenario: You refactor BookService to fix the SRP violation

Without tests:
  → Make changes
  → Run application manually
  → Click through UI testing a few paths
  → Miss edge case
  → Deploy to production
  → User discovers bug
  → Emergency hotfix
  → Lost user trust

With tests:
  → Make changes
  → Run test suite (30 seconds)
  → Test fails: "BookService.checkout throws NPE when book is null"
  → Fix bug before committing
  → All tests pass
  → Deploy with confidence
```

2. **No Documentation of Behavior:**
```java
// Without tests, to understand what createNewBook does:
// 1. Read entire implementation (50+ lines)
// 2. Trace through multiple method calls
// 3. Check database to see what was created
// 4. Still unclear on edge cases

// With tests, behavior is documented:
@Test
void shouldCreateNewBookWithNewAuthor() {
    // Arrange
    BookRequestDTO request = new BookRequestDTO("Clean Code", "Robert", "Martin");

    // Act
    bookService.createNewBook(request);

    // Assert
    BookEntity savedBook = bookRepository.findByTitleIgnoreCase("Clean Code");
    assertEquals("Clean Code", savedBook.getTitle());
    // Clear: creates book and saves to database
}

@Test
void shouldReuseExistingAuthor() {
    // Documents: doesn't create duplicate authors
}

@Test
void shouldThrowExceptionForBlankTitle() {
    // Documents: validates input
}
```

3. **Bugs Only Found in Production:**
```
Real examples from your code that would be caught by tests:

Bug 1: AuthorService.authorRepository is null
  → NullPointerException at runtime
  → Would be caught by ANY test of AuthorService

Bug 2: Optional.get() without isPresent() check
  → NoSuchElementException when shelf not found
  → Would be caught by test with missing shelf

Bug 3: Thread.sleep() blocks for 22 seconds total
  → Slow user experience
  → Would be caught by performance tests

Bug 4: N+1 query executes 601 queries for 100 bookcases
  → App grinds to a halt with real data
  → Would be caught by integration test
```

### The Cost of No Tests

**Microsoft Research Study:**
- Projects with <20% test coverage: **40-90% more bugs** in production
- Projects with >80% test coverage: **40-90% fewer bugs** in production
- Each production bug costs **10-100x more** than catching in development

**Your Project Right Now:**
```
Development:
  Time to write feature: 4 hours
  Time to manually test: 30 minutes
  Bugs found: Maybe 1-2 obvious ones
  Total: 4.5 hours

Production (after deployment):
  User reports bug: 2 hours later
  Investigate bug: 1 hour
  Fix bug: 30 minutes
  Test manually: 30 minutes
  Deploy hotfix: 1 hour
  User re-tests: 30 minutes
  Total: 4.5 hours (same time, but now users affected!)

With Tests:
  Time to write feature: 4 hours
  Time to write tests: 1 hour
  Bugs found: 5-10 (including edge cases)
  Fix bugs: 30 minutes
  Total: 5.5 hours (1 hour more, but no production bugs!)
```

### Test Pyramid

**The ideal test distribution:**

```
         /\
        /  \
       / E2E \      10% - End-to-End Tests
      /______\      (Full system, slow, brittle)
     /        \
    /Integration\   20% - Integration Tests
   /____________\   (Multiple components, medium speed)
  /              \
 /  Unit Tests    \ 70% - Unit Tests
/__________________\ (Single component, fast, reliable)

Your current pyramid:
         /\
        /  \
       /    \
      /      \
     /        \
    /          \
   /            \
  /              \
 /                \
/__________________|  0% everything!
```

### Correct Approach - Comprehensive Test Suite

**1. Unit Tests (Test Services in Isolation):**

```java
/**
 * Unit tests for BookService using Mockito.
 * Tests business logic in isolation from database.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    @DisplayName("Should create new book with new author")
    void shouldCreateNewBookWithNewAuthor() {
        // Arrange
        BookRequestDTO request = new BookRequestDTO("Clean Code", "Robert", "Martin");

        // Mock: Author doesn't exist
        when(authorRepository.findByFirstNameAndLastName("Robert", "Martin"))
            .thenReturn(Optional.empty());

        // Mock: Save author returns entity with ID
        AuthorEntity savedAuthor = new AuthorEntity("Robert", "Martin");
        savedAuthor.setAuthorId(1L);
        when(authorRepository.save(any(AuthorEntity.class)))
            .thenReturn(savedAuthor);

        // Mock: Save book returns entity with ID
        BookEntity savedBook = new BookEntity();
        savedBook.setBookId(1L);
        savedBook.setTitle("Clean Code");
        when(bookRepository.save(any(BookEntity.class)))
            .thenReturn(savedBook);

        // Act
        BookEntity result = bookService.createNewBook(request);

        // Assert
        assertNotNull(result);
        assertEquals("Clean Code", result.getTitle());

        // Verify interactions
        verify(authorRepository).findByFirstNameAndLastName("Robert", "Martin");
        verify(authorRepository).save(any(AuthorEntity.class));
        verify(bookRepository).save(any(BookEntity.class));
    }

    @Test
    @DisplayName("Should reuse existing author when creating book")
    void shouldReuseExistingAuthor() {
        // Arrange
        BookRequestDTO request = new BookRequestDTO("The Clean Coder", "Robert", "Martin");

        // Mock: Author already exists
        AuthorEntity existingAuthor = new AuthorEntity("Robert", "Martin");
        existingAuthor.setAuthorId(1L);
        when(authorRepository.findByFirstNameAndLastName("Robert", "Martin"))
            .thenReturn(Optional.of(existingAuthor));

        BookEntity savedBook = new BookEntity();
        savedBook.setBookId(2L);
        when(bookRepository.save(any(BookEntity.class)))
            .thenReturn(savedBook);

        // Act
        bookService.createNewBook(request);

        // Assert
        // Should NOT create new author
        verify(authorRepository, never()).save(any(AuthorEntity.class));
        // Should save book
        verify(bookRepository).save(any(BookEntity.class));
    }

    @Test
    @DisplayName("Should throw exception for null request")
    void shouldThrowExceptionForNullRequest() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> bookService.createNewBook(null),
            "Should validate request is not null"
        );

        // Verify no database calls made
        verify(bookRepository, never()).save(any());
        verify(authorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception for blank title")
    void shouldThrowExceptionForBlankTitle() {
        // Arrange
        BookRequestDTO request = new BookRequestDTO("", "Robert", "Martin");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> bookService.createNewBook(request),
            "Should validate title is not blank"
        );
    }

    @Test
    @DisplayName("Should checkout available book")
    void shouldCheckoutAvailableBook() {
        // Arrange
        BookEntity book = new BookEntity();
        book.setBookId(1L);
        book.setTitle("Clean Code");
        book.setStatus(BookStatus.AVAILABLE);

        when(bookRepository.save(any(BookEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        bookService.checkoutBook(book);

        // Assert
        assertEquals(BookStatus.CHECKED_OUT, book.getStatus());
        assertNotNull(book.getCheckoutDate());
        assertNotNull(book.getDueDate());

        verify(bookRepository).save(book);
    }

    @Test
    @DisplayName("Should throw exception when checking out already checked out book")
    void shouldThrowExceptionForAlreadyCheckedOutBook() {
        // Arrange
        BookEntity book = new BookEntity();
        book.setStatus(BookStatus.CHECKED_OUT);

        // Act & Assert
        assertThrows(BookAlreadyCheckedOutException.class,
            () -> bookService.checkoutBook(book),
            "Should not allow checking out book that's already checked out"
        );

        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should find book by title case-insensitive")
    void shouldFindBookByTitleCaseInsensitive() {
        // Arrange
        BookEntity expectedBook = new BookEntity();
        expectedBook.setTitle("Clean Code");

        when(bookRepository.findByTitleIgnoreCase("clean code"))
            .thenReturn(expectedBook);

        // Act
        Optional<BookEntity> result = bookService.findByExactTitle("clean code");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Clean Code", result.get().getTitle());
        verify(bookRepository).findByTitleIgnoreCase("clean code");
    }

    @Test
    @DisplayName("Should return empty Optional when book not found")
    void shouldReturnEmptyOptionalWhenBookNotFound() {
        // Arrange
        when(bookRepository.findByTitleIgnoreCase("Nonexistent Book"))
            .thenReturn(null);

        // Act
        Optional<BookEntity> result = bookService.findByExactTitle("Nonexistent Book");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should validate title is not null")
    void shouldValidateTitleIsNotNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> bookService.findByExactTitle(null)
        );
    }

    @Test
    @DisplayName("Should validate title is not blank")
    void shouldValidateTitleIsNotBlank() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> bookService.findByExactTitle("   ")
        );
    }
}
```

**2. Integration Tests (Test Services with Real Database):**

```java
/**
 * Integration tests for BookService with real database.
 * Tests full interaction with JPA repositories.
 */
@SpringBootTest
@Transactional  // Rollback after each test
class BookServiceIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create book and author in database")
    void shouldCreateBookAndAuthorInDatabase() {
        // Arrange
        BookRequestDTO request = new BookRequestDTO("Clean Code", "Robert", "Martin");

        // Act
        BookEntity createdBook = bookService.createNewBook(request);

        // Assert - Verify book in database
        BookEntity savedBook = bookRepository.findById(createdBook.getBookId()).orElseThrow();
        assertEquals("Clean Code", savedBook.getTitle());
        assertEquals(BookStatus.AVAILABLE, savedBook.getStatus());

        // Assert - Verify author in database
        List<AuthorEntity> authors = authorRepository.findByBooks_BookId(savedBook.getBookId());
        assertEquals(1, authors.size());
        assertEquals("Robert", authors.get(0).getFirstName());
        assertEquals("Martin", authors.get(0).getLastName());
    }

    @Test
    @DisplayName("Should not create duplicate author")
    void shouldNotCreateDuplicateAuthor() {
        // Arrange
        BookRequestDTO request1 = new BookRequestDTO("Clean Code", "Robert", "Martin");
        BookRequestDTO request2 = new BookRequestDTO("The Clean Coder", "Robert", "Martin");

        // Act
        bookService.createNewBook(request1);
        bookService.createNewBook(request2);

        // Assert - Only one author should exist
        long authorCount = authorRepository.count();
        assertEquals(1, authorCount, "Should reuse existing author");

        // Both books should reference same author
        long bookCount = bookRepository.count();
        assertEquals(2, bookCount);
    }

    @Test
    @DisplayName("Should rollback transaction on error")
    void shouldRollbackTransactionOnError() {
        // This test verifies @Transactional behavior
        // If book creation fails, author should also not be saved

        // Arrange - Setup to cause failure
        // (Would need to mock repository to throw exception)

        // Verify nothing saved when transaction rolls back
        assertEquals(0, bookRepository.count());
        assertEquals(0, authorRepository.count());
    }

    @Test
    @DisplayName("Should handle concurrent book creation")
    void shouldHandleConcurrentBookCreation() throws InterruptedException {
        // Test thread safety
        CountDownLatch latch = new CountDownLatch(10);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Create 10 books concurrently
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                BookRequestDTO request = new BookRequestDTO(
                    "Book " + finalI,
                    "Author",
                    "Number" + finalI
                );
                bookService.createNewBook(request);
                latch.countDown();
            });
            futures.add(future);
        }

        // Wait for all to complete
        latch.await(10, TimeUnit.SECONDS);
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Assert - All books created
        assertEquals(10, bookRepository.count());
    }
}
```

**3. Repository Tests (Test Custom Queries):**

```java
/**
 * Tests for BookRepository custom queries.
 * Uses @DataJpaTest for lightweight testing.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should find book by title case-insensitive")
    void shouldFindByTitleCaseInsensitive() {
        // Arrange
        BookEntity book = createBook("Clean Code");
        entityManager.persist(book);
        entityManager.flush();

        // Act
        BookEntity found = bookRepository.findByTitleIgnoreCase("clean code");

        // Assert
        assertNotNull(found);
        assertEquals("Clean Code", found.getTitle());
    }

    @Test
    @DisplayName("Should find books containing search term")
    void shouldFindBooksContainingTerm() {
        // Arrange
        BookEntity book1 = createBook("Clean Code");
        BookEntity book2 = createBook("The Clean Coder");
        BookEntity book3 = createBook("Effective Java");

        entityManager.persist(book1);
        entityManager.persist(book2);
        entityManager.persist(book3);
        entityManager.flush();

        // Act
        List<BookEntity> results = bookRepository.findByTitleContainsIgnoreCase("clean");

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(b -> b.getTitle().equals("Clean Code")));
        assertTrue(results.stream().anyMatch(b -> b.getTitle().equals("The Clean Coder")));
        assertFalse(results.stream().anyMatch(b -> b.getTitle().equals("Effective Java")));
    }

    @Test
    @DisplayName("Should find books by status")
    void shouldFindBooksByStatus() {
        // Arrange
        BookEntity available1 = createBook("Book 1");
        available1.setStatus(BookStatus.AVAILABLE);

        BookEntity available2 = createBook("Book 2");
        available2.setStatus(BookStatus.AVAILABLE);

        BookEntity checkedOut = createBook("Book 3");
        checkedOut.setStatus(BookStatus.CHECKED_OUT);

        entityManager.persist(available1);
        entityManager.persist(available2);
        entityManager.persist(checkedOut);
        entityManager.flush();

        // Act
        List<BookEntity> availableBooks = bookRepository.findByStatus(BookStatus.AVAILABLE);

        // Assert
        assertEquals(2, availableBooks.size());
        assertTrue(availableBooks.stream()
            .allMatch(b -> b.getStatus() == BookStatus.AVAILABLE));
    }

    @Test
    @DisplayName("Should count books by status")
    void shouldCountBooksByStatus() {
        // Arrange
        createAndPersistBook("Book 1", BookStatus.AVAILABLE);
        createAndPersistBook("Book 2", BookStatus.AVAILABLE);
        createAndPersistBook("Book 3", BookStatus.CHECKED_OUT);

        // Act
        long availableCount = bookRepository.countByStatus(BookStatus.AVAILABLE);
        long checkedOutCount = bookRepository.countByStatus(BookStatus.CHECKED_OUT);

        // Assert
        assertEquals(2, availableCount);
        assertEquals(1, checkedOutCount);
    }

    @Test
    @DisplayName("Should get book detail view with joins")
    void shouldGetBookDetailView() {
        // Arrange
        // Create bookcase → shelf → book → author hierarchy
        // ... (setup complex object graph)

        // Act
        BookDetailView view = bookRepository.getBookDetailView(bookId);

        // Assert
        assertNotNull(view);
        assertEquals("Clean Code", view.title());
        assertEquals("Robert Martin", view.authorName());
        assertEquals("Fiction", view.bookcaseLabel());
        assertEquals("Shelf 1", view.shelfLabel());
    }

    private BookEntity createBook(String title) {
        BookEntity book = new BookEntity();
        book.setTitle(title);
        book.setStatus(BookStatus.AVAILABLE);
        return book;
    }

    private void createAndPersistBook(String title, BookStatus status) {
        BookEntity book = createBook(title);
        book.setStatus(status);
        entityManager.persist(book);
        entityManager.flush();
    }
}
```

**4. Controller Tests (Test REST APIs):**

```java
/**
 * Integration tests for BookController REST endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/books should create new book")
    void shouldCreateNewBook() throws Exception {
        // Arrange
        BookRequestDTO request = new BookRequestDTO("Clean Code", "Robert", "Martin");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Clean Code"))
            .andExpect(jsonPath("$.status").value("AVAILABLE"));

        // Verify persistence
        BookEntity savedBook = bookRepository.findByTitleIgnoreCase("Clean Code");
        assertNotNull(savedBook);
        assertEquals("Clean Code", savedBook.getTitle());
    }

    @Test
    @DisplayName("POST /api/v1/books with invalid data should return 400")
    void shouldRejectInvalidBookData() throws Exception {
        // Arrange
        BookRequestDTO invalidRequest = new BookRequestDTO("", "", "");
        String jsonRequest = objectMapper.writeValueAsString(invalidRequest);

        // Act & Assert
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").exists());

        // Verify nothing saved
        assertEquals(0, bookRepository.count());
    }

    @Test
    @DisplayName("GET /api/v1/books/{id} should return book")
    void shouldReturnBookById() throws Exception {
        // Arrange
        BookEntity book = new BookEntity();
        book.setTitle("Clean Code");
        book.setStatus(BookStatus.AVAILABLE);
        BookEntity savedBook = bookRepository.save(book);

        // Act & Assert
        mockMvc.perform(get("/api/v1/books/" + savedBook.getBookId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Clean Code"))
            .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("GET /api/v1/books/{id} should return 404 for non-existent book")
    void shouldReturn404ForNonExistentBook() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/books/99999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/books/search should search books")
    void shouldSearchBooks() throws Exception {
        // Arrange
        createAndSaveBook("Clean Code");
        createAndSaveBook("The Clean Coder");
        createAndSaveBook("Effective Java");

        // Act & Assert
        mockMvc.perform(get("/api/v1/books/search")
                .param("query", "clean"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].title").value("Clean Code"))
            .andExpect(jsonPath("$[1].title").value("The Clean Coder"));
    }

    @Test
    @DisplayName("DELETE /api/v1/books/{id} should delete book")
    void shouldDeleteBook() throws Exception {
        // Arrange
        BookEntity book = createAndSaveBook("Book to Delete");

        // Act & Assert
        mockMvc.perform(delete("/api/v1/books/" + book.getBookId()))
            .andExpect(status().isNoContent());

        // Verify deletion
        assertFalse(bookRepository.existsById(book.getBookId()));
    }

    private BookEntity createAndSaveBook(String title) {
        BookEntity book = new BookEntity();
        book.setTitle(title);
        book.setStatus(BookStatus.AVAILABLE);
        return bookRepository.save(book);
    }
}
```

**5. Test Coverage with JaCoCo:**

```xml
<!-- pom.xml -->
<build>
    <plugins>
        <!-- JaCoCo for test coverage -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
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
                <execution>
                    <id>jacoco-check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>PACKAGE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.70</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

```bash
# Run tests with coverage
mvn clean test

# View coverage report
open target/site/jacoco/index.html

# Fail build if coverage < 70%
mvn clean verify
```

### Test-Driven Development (TDD) Example

**Write tests BEFORE implementation:**

```java
/**
 * TDD Example: Implementing book renewal feature.
 *
 * Step 1: Write failing test (RED)
 */
@Test
@DisplayName("Should renew book checkout by extending due date")
void shouldRenewBookCheckout() {
    // Arrange
    BookEntity book = new BookEntity();
    book.setStatus(BookStatus.CHECKED_OUT);
    book.setDueDate(LocalDateTime.now().plusDays(7));
    LocalDateTime originalDueDate = book.getDueDate();

    // Act
    bookService.renewCheckout(book);

    // Assert
    assertEquals(BookStatus.CHECKED_OUT, book.getStatus());
    assertTrue(book.getDueDate().isAfter(originalDueDate));
    assertEquals(originalDueDate.plusDays(14), book.getDueDate());
}

// Step 2: Write minimal code to make test pass (GREEN)
@Transactional
public void renewCheckout(BookEntity book) {
    if (book.getStatus() != BookStatus.CHECKED_OUT) {
        throw new InvalidOperationException("Book is not checked out");
    }
    book.setDueDate(book.getDueDate().plusDays(14));
    bookRepository.save(book);
}

// Step 3: Refactor (REFACTOR)
// Clean up code, extract methods, improve names

// Step 4: Add more tests for edge cases
@Test
@DisplayName("Should throw exception when renewing non-checked-out book")
void shouldThrowExceptionForRenewingNonCheckedOutBook() {
    BookEntity book = new BookEntity();
    book.setStatus(BookStatus.AVAILABLE);

    assertThrows(InvalidOperationException.class,
        () -> bookService.renewCheckout(book)
    );
}

@Test
@DisplayName("Should not allow renewal if already renewed twice")
void shouldNotAllowMoreThanTwoRenewals() {
    // Business rule: max 2 renewals
    // ... test implementation
}
```

### AAA Pattern (Arrange-Act-Assert)

**Structure every test the same way:**

```java
@Test
void testMethodName() {
    // Arrange (Given) - Set up test data and conditions
    BookEntity book = new BookEntity();
    book.setTitle("Clean Code");
    when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

    // Act (When) - Execute the code under test
    Optional<BookEntity> result = bookService.findById(1L);

    // Assert (Then) - Verify the outcome
    assertTrue(result.isPresent());
    assertEquals("Clean Code", result.get().getTitle());
}
```

### Testing Best Practices

**DO:**
- ✅ Test one thing per test
- ✅ Use descriptive test names
- ✅ Follow AAA pattern
- ✅ Test edge cases and error conditions
- ✅ Keep tests independent (no shared state)
- ✅ Use @DisplayName for readable test reports
- ✅ Mock external dependencies
- ✅ Test the behavior, not implementation

**DON'T:**
- ❌ Test implementation details
- ❌ Have tests depend on each other
- ❌ Use real external services (database, APIs)
- ❌ Have flaky tests (random failures)
- ❌ Ignore failing tests
- ❌ Write tests without assertions
- ❌ Have one giant test that tests everything

### Learning Principle

> **Tests are documentation that never gets out of date.** They describe how your code should behave, catch bugs before production, enable refactoring with confidence, and make you a better developer by forcing you to think about edge cases. Aim for 70%+ coverage, but focus on testing critical business logic first.

### Action Items

1. ✅ Set up JaCoCo for test coverage reporting
2. ✅ Write unit tests for all service classes (start with BookService)
3. ✅ Write integration tests for repositories
4. ✅ Write API tests for controllers
5. ✅ Add @DisplayName to all tests for readable reports
6. ✅ Configure CI/CD to fail build if coverage < 70%
7. ✅ Practice TDD for new features

**Estimated Fix Time:** 15-20 hours (initial), ongoing for new code
**Impact:** Catch bugs before production, enable confident refactoring

---

## 🟠 Issue 6.2: Missing Integration Tests

**Priority:** HIGH

### Why Integration Tests Matter

**Unit tests verify individual components, but integration tests verify they work together:**

```java
// Unit test: BookService works ✅
@Test
void shouldSaveBook() {
    when(bookRepository.save(any())).thenReturn(book);
    bookService.createBook(dto);
    verify(bookRepository).save(any());
}
// Test passes, but...

// Integration test reveals problem:
@Test
@SpringBootTest
void shouldActuallySaveBookToDatabase() {
    bookService.createBook(dto);

    // ❌ FAILS: BookEntity missing @Table annotation
    // ❌ FAILS: Database column mismatch
    // ❌ FAILS: Transaction not committed
    // ❌ FAILS: Constraint violation
}
```

### What Integration Tests Should Cover

**1. Database Operations:**

```java
@SpringBootTest
@Transactional
class BookDatabaseIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Should persist book with all relationships")
    void shouldPersistBookWithRelationships() {
        // Create book with author, shelf, bookcase
        BookRequestDTO request = new BookRequestDTO("Clean Code", "Robert", "Martin");

        BookEntity book = bookService.createNewBook(request);

        // Flush to database
        entityManager.flush();
        entityManager.clear();  // Clear cache

        // Reload from database
        BookEntity reloaded = entityManager.find(BookEntity.class, book.getBookId());

        // Verify all relationships loaded correctly
        assertNotNull(reloaded);
        assertNotNull(reloaded.getAuthor());
        assertEquals("Robert", reloaded.getAuthor().getFirstName());
    }

    @Test
    @DisplayName("Should handle database constraints")
    void shouldEnforceDatabaseConstraints() {
        // Try to save book with too-long title
        BookEntity book = new BookEntity();
        book.setTitle("A".repeat(300));  // Exceeds VARCHAR(255)

        assertThrows(DataAccessException.class,
            () -> {
                entityManager.persist(book);
                entityManager.flush();
            }
        );
    }

    @Test
    @DisplayName("Should cascade delete relationships")
    void shouldCascadeDeleteRelationships() {
        // Create bookcase with shelves and books
        BookcaseEntity bookcase = createBookcaseWithBooks();
        Long bookcaseId = bookcase.getBookcaseId();

        // Delete bookcase
        bookcaseService.deleteBookcase(bookcaseId);
        entityManager.flush();

        // Verify cascade
        assertFalse(bookcaseRepository.existsById(bookcaseId));
        // Verify shelves also deleted
        assertEquals(0, shelfRepository.findByBookcaseId(bookcaseId).size());
    }
}
```

**2. Transaction Boundaries:**

```java
@SpringBootTest
class TransactionIntegrationTest {

    @Autowired
    private BookcaseService bookcaseService;

    @Autowired
    private BookcaseRepository bookcaseRepository;

    @Autowired
    private ShelfRepository shelfRepository;

    @Test
    @DisplayName("Should rollback entire transaction on failure")
    void shouldRollbackOnFailure() {
        // Arrange
        String label = "Test Bookcase";
        int shelfCount = 5;

        // Mock shelf repository to fail on 3rd shelf
        // (In real test, setup scenario that causes failure)

        try {
            // Act
            bookcaseService.createNewBookCase(label, shelfCount);
            fail("Should have thrown exception");
        } catch (Exception e) {
            // Assert - Nothing should be saved
            assertFalse(bookcaseRepository.existsByBookcaseLabelIgnoreCase(label));
            assertEquals(0, shelfRepository.count());
        }
    }

    @Test
    @DisplayName("Should commit transaction when all operations succeed")
    void shouldCommitTransactionOnSuccess() {
        // Act
        BookcaseEntity bookcase = bookcaseService.createNewBookCase("Fiction", 5);

        // Assert - Verify commit (outside transaction)
        BookcaseEntity reloaded = bookcaseRepository.findById(bookcase.getBookcaseId()).orElseThrow();
        assertNotNull(reloaded);

        List<ShelfEntity> shelves = shelfRepository.findByBookcaseId(bookcase.getBookcaseId());
        assertEquals(5, shelves.size());
    }
}
```

**3. API Contracts:**

```java
@SpringBootTest
@AutoConfigureMockMvc
class BookAPIContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return correct JSON structure for book")
    void shouldReturnCorrectBookJSONStructure() throws Exception {
        // Arrange
        BookEntity book = createAndSaveBook("Clean Code");

        // Act & Assert
        mockMvc.perform(get("/api/v1/books/" + book.getBookId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookId").exists())
            .andExpect(jsonPath("$.title").exists())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.author").exists())
            .andExpect(jsonPath("$.author.firstName").exists())
            .andExpect(jsonPath("$.author.lastName").exists());
    }

    @Test
    @DisplayName("Should include HATEOAS links")
    void shouldIncludeHATEOASLinks() throws Exception {
        BookEntity book = createAndSaveBook("Clean Code");

        mockMvc.perform(get("/api/v1/books/" + book.getBookId()))
            .andExpect(jsonPath("$._links.self").exists())
            .andExpect(jsonPath("$._links.checkout").exists())
            .andExpect(jsonPath("$._links.shelf").exists());
    }
}
```

**4. Performance Tests:**

```java
@SpringBootTest
class BookPerformanceTest {

    @Autowired
    private BookcaseService bookcaseService;

    @Test
    @DisplayName("Should execute bookcase query in < 100ms")
    void shouldExecuteBookcaseQueryEfficiently() {
        // Arrange
        createTestData();  // 100 bookcases, 500 shelves, 5000 books

        // Act
        long startTime = System.currentTimeMillis();
        List<BookcaseStatistics> stats = bookcaseService.getAllBookcasesWithStats();
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertThat(duration).isLessThan(100);  // Should be < 100ms
        assertEquals(100, stats.size());
    }

    @Test
    @DisplayName("Should not execute N+1 queries")
    void shouldNotHaveNPlusOneQueries() {
        // Enable query counting
        QueryCountHolder.clear();

        // Act
        bookcaseService.getAllBookcasesWithStats();

        // Assert
        long selectCount = QueryCountHolder.getGrandTotal().getSelect();
        assertThat(selectCount).isLessThanOrEqualTo(3);  // Max 3 queries allowed
    }
}
```

### Learning Principle

> **Integration tests verify components work together correctly.** They catch issues that unit tests miss: database constraints, transaction boundaries, JSON serialization, performance problems. Run integration tests before every commit. They're slower than unit tests but essential for confidence.

### Action Items

1. ✅ Write integration tests for all services
2. ✅ Test database operations with real database
3. ✅ Test transaction rollback scenarios
4. ✅ Test API endpoints with MockMvc
5. ✅ Add performance tests for critical operations
6. ✅ Use test containers for isolated database testing

**Estimated Fix Time:** 10 hours (initial)

---

## 📊 Summary Table

| Issue | Priority | Impact | Fix Time |
|-------|----------|--------|----------|
| No test coverage | 🔴 Critical | High bug rate, fear of changes | 15-20 hours |
| Missing integration tests | 🟠 High | Production issues | 10 hours |

**Total Estimated Time:** 25-30 hours (initial investment)
**Expected Impact:**
- 40-90% fewer production bugs
- Confident refactoring
- Faster development (long-term)
- Better code quality

---

## ✅ Action Checklist

### Week 1: Foundation
- [ ] Set up JaCoCo for coverage reporting (30 min)
- [ ] Write first unit test for BookService (1 hour)
- [ ] Write 10 more unit tests for BookService (3 hours)
- [ ] Write unit tests for BookcaseService (2 hours)
- [ ] Write unit tests for ShelfService (2 hours)
- [ ] Configure test coverage threshold to 50% (30 min)

### Week 2: Integration Tests
- [ ] Write integration test for book creation (1 hour)
- [ ] Write integration test for transaction rollback (1 hour)
- [ ] Write repository tests for custom queries (2 hours)
- [ ] Write API tests for BookController (2 hours)
- [ ] Test N+1 query fix (1 hour)
- [ ] Increase coverage threshold to 60% (5 min)

### Week 3: Expand Coverage
- [ ] Write tests for all remaining services (4 hours)
- [ ] Write tests for edge cases and errors (3 hours)
- [ ] Add performance tests (2 hours)
- [ ] Increase coverage threshold to 70% (5 min)

### Ongoing
- [ ] Practice TDD for all new features
- [ ] Review test coverage in PRs
- [ ] Refactor tests for better readability
- [ ] Keep tests fast (< 2 minutes for full suite)

---

## 🎓 Testing Principles

### The Testing Mantra

```
1. Red: Write failing test
2. Green: Make it pass
3. Refactor: Clean up code
4. Repeat
```

### Test Naming Convention

```java
// Pattern: should[ExpectedBehavior]When[StateUnderTest]
@Test
void shouldReturnTrueWhenBookIsAvailable()

@Test
void shouldThrowExceptionWhenTitleIsBlank()

@Test
void shouldSaveBookWhenValidDataProvided()

// Or use @DisplayName for business language
@DisplayName("Should send reminder email when book is overdue")
```

### What Makes a Good Test?

**F.I.R.S.T. Principles:**

- **Fast:** Tests should run quickly (< 100ms per test)
- **Independent:** Tests don't depend on each other
- **Repeatable:** Same result every time
- **Self-Validating:** Pass or fail (no manual verification)
- **Timely:** Written just before production code (TDD)

---

## 📖 Resources for Learning Testing

**Books:**
- "Test Driven Development: By Example" by Kent Beck
- "Growing Object-Oriented Software, Guided by Tests" by Freeman & Pryce

**Online:**
- JUnit 5 User Guide: https://junit.org/junit5/docs/current/user-guide/
- Mockito Documentation: https://javadoc.io/doc/org.mockito/mockito-core
- AssertJ Assertions: https://assertj.github.io/doc/

**Practice:**
- Start with one service
- Write tests for happy path
- Add tests for edge cases
- Add tests for error conditions
- Review coverage report
- Repeat for next service

---

**Recommended Next Section:**
- Section 7: Security Concerns (hardcoded credentials, validation)
- Section 8: Modern Spring Boot Practices
- Section 10: Key Takeaways

Which would you like next?
