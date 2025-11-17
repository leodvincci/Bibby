# SECTION 26: INTEGRATION TESTING

## Introduction: Testing Beyond Isolation

**"Unit tests test the parts. Integration tests test that the parts work together."**

You've mastered unit tests (Section 24) and mocking (Section 25). But there's a critical gap: **How do you know your service actually works with a real database?**

```java
// Unit test says this works
@Test
void shouldSaveBook() {
    when(mockRepo.save(book)).thenReturn(book);
    service.addBook(command);
    verify(mockRepo).save(book);  // ✅ Passes
}

// But does it REALLY work with PostgreSQL?
// What if there's a schema mismatch?
// What if the ISBN column is too short?
// What if there's a constraint violation?
```

**Integration tests** answer these questions by testing with **real dependencies**.

In this section, we'll build a comprehensive integration test suite for **Bibby** using Spring Boot test support and Testcontainers.

**Learning Objectives**:
- Understand integration vs. unit tests
- Master Spring Boot test annotations
- Use Testcontainers for real database testing
- Manage transactions in tests
- Test Bibby services end-to-end
- Apply integration test best practices

**Time Investment**: 4-5 hours

---

## Part 1: What Are Integration Tests?

### Definition

**Integration Test**: Tests multiple components working together with **real** (or realistic) dependencies.

**Spectrum of Test Types**:

```
Unit Test          Integration Test         E2E Test
    |                     |                     |
[Domain logic]    [Service + DB]      [Full application]
No dependencies   Real database       Real UI + DB + APIs
< 1ms             50-200ms            1-5 seconds
```

### What Integration Tests Test

**Unit test** (Section 24):
```java
@Test
void shouldCheckOutBook() {
    Book book = aBook().available().build();

    book.checkOut();  // Pure domain logic

    assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
}
// Tests: Domain logic only
// Duration: < 1ms
```

**Integration test**:
```java
@Test
@SpringBootTest
@Transactional
void shouldCheckOutBookAndPersistToDatabase() {
    // REAL Spring context, REAL database, REAL repositories
    Book book = aBook().build();
    bookRepository.save(book);  // Actually saves to database!

    checkOutService.checkOut(book.getId());

    Book retrieved = bookRepository.findById(book.getId()).orElseThrow();
    assertThat(retrieved.getStatus()).isEqualTo(CHECKED_OUT);
}
// Tests: Service + Repository + Database
// Duration: 50-200ms
```

### When to Write Integration Tests

**Write integration tests when**:
- ✅ Testing database queries (JPA, custom queries)
- ✅ Testing transaction behavior
- ✅ Testing Spring configuration
- ✅ Testing service orchestration
- ✅ Verifying schema matches entities

**Don't write integration tests for**:
- ❌ Pure domain logic (use unit tests)
- ❌ Simple CRUD (Spring Data already tested)
- ❌ Every service method (too slow)

**Rule of thumb**: 70% unit tests, 20% integration tests, 10% E2E tests.

---

## Part 2: Spring Boot Test Support

### @SpringBootTest - Full Application Context

**Loads entire Spring context** (all beans, configuration).

```java
@SpringBootTest
class CheckOutBookServiceIntegrationTest {

    @Autowired
    private CheckOutBookService service;  // Real service

    @Autowired
    private BookRepository bookRepository;  // Real repository

    @Test
    void shouldCheckOutBook() {
        // Test with real Spring beans
    }
}
```

**When to use**: Testing services that depend on multiple beans.

**Pros**:
- Full application context
- All beans available
- Realistic environment

**Cons**:
- Slow (loads entire Spring context)
- Heavy (all beans loaded)

### @DataJpaTest - Repository Testing Only

**Loads only JPA components** (entities, repositories).

```java
@DataJpaTest
class BookRepositoryIntegrationTest {

    @Autowired
    private BookRepository bookRepository;  // Available

    @Autowired
    private TestEntityManager entityManager;  // For test data setup

    // Services are NOT available (not loaded)

    @Test
    void shouldFindBookByIsbn() {
        Book book = aBook().build();
        entityManager.persist(book);
        entityManager.flush();

        Optional<Book> found = bookRepository.findByIsbn(book.getIsbn());

        assertThat(found).isPresent();
    }
}
```

**When to use**: Testing custom repository queries.

**Pros**:
- Fast (only JPA components loaded)
- Lightweight
- Auto-configured H2 in-memory database

**Cons**:
- No service layer
- May not test with real database (uses H2 by default)

### @Transactional - Auto-Rollback

**Automatically rolls back transactions** after each test.

```java
@SpringBootTest
@Transactional  // Each test runs in transaction, rolled back after
class BookServiceIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldSaveBook() {
        Book book = aBook().build();

        bookRepository.save(book);

        assertThat(bookRepository.findById(book.getId())).isPresent();
        // After test: ROLLED BACK! Database is clean for next test
    }

    @Test
    void shouldDeleteBook() {
        // Database is clean - previous test was rolled back
    }
}
```

**When to use**: Always, in integration tests.

**Pros**:
- Automatic cleanup
- Tests don't affect each other
- No manual database cleanup

**Cons**:
- Hides issues with transaction boundaries
- May not test actual commit behavior

### @TestConfiguration - Custom Test Beans

**Override beans for testing**.

```java
@SpringBootTest
class BookServiceWithTestConfigIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary  // Overrides production DomainEventPublisher
        public DomainEventPublisher testEventPublisher() {
            return new InMemoryEventPublisher();  // No real event bus
        }
    }

    @Autowired
    private CheckOutBookService service;  // Uses InMemoryEventPublisher

    @Test
    void test() {
        // Service uses test event publisher
    }
}
```

---

## Part 3: Testcontainers - Real Database Testing

### What is Testcontainers?

**Testcontainers** runs **real databases** (PostgreSQL, MySQL, etc.) in Docker containers during tests.

**Why?**
- ✅ Test with real database (not H2 in-memory)
- ✅ Catch database-specific bugs
- ✅ Test actual SQL queries
- ✅ Verify constraints, triggers, indexes

### Setup

**Add dependency** (`pom.xml`):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### Basic Testcontainers Test

```java
@SpringBootTest
@Testcontainers
@Transactional
class BookRepositoryTestcontainersTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldSaveAndRetrieveBook() {
        // This uses REAL PostgreSQL in Docker!
        Book book = aBook().withTitle("Clean Code").build();

        bookRepository.save(book);

        Optional<Book> retrieved = bookRepository.findById(book.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTitle().value()).isEqualTo("Clean Code");
    }
}
```

**What happens**:
1. Testcontainers starts PostgreSQL container
2. Spring Boot connects to it
3. Schema is created (via Flyway/Liquibase or JPA)
4. Test runs with real PostgreSQL
5. Container is stopped and removed

### Shared Container for Speed

**Problem**: Starting a container for each test class is slow.

**Solution**: Share one container across all tests.

**Create base class**:
```java
@SpringBootTest
@Testcontainers
@Transactional
public abstract class IntegrationTestBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withReuse(true);  // Reuse across test runs

    static {
        postgres.start();
    }
}
```

**Use in tests**:
```java
class BookRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldFindBookByIsbn() {
        // Uses shared PostgreSQL container
    }
}
```

**Now all tests share one container** - much faster!

---

## Part 4: Testing Bibby Services with Real Database

### Example 1: BookRepository Integration Test

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional
class BookRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("should find book by ISBN")
    void shouldFindBookByIsbn() {
        // ARRANGE
        ISBN isbn = ISBN.fromString("9780132350884");
        Book book = aBook().withIsbn(isbn).build();
        entityManager.persist(book);
        entityManager.flush();

        // ACT
        Optional<Book> found = bookRepository.findByIsbn(isbn);

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getIsbn()).isEqualTo(isbn);
    }

    @Test
    @DisplayName("should find all available books")
    void shouldFindAllAvailableBooks() {
        // ARRANGE
        Book available1 = aBook().available().build();
        Book available2 = aBook().available().build();
        Book checkedOut = aBook().checkedOut().build();

        entityManager.persist(available1);
        entityManager.persist(available2);
        entityManager.persist(checkedOut);
        entityManager.flush();

        // ACT
        List<Book> availableBooks = bookRepository.findByStatus(AvailabilityStatus.AVAILABLE);

        // ASSERT
        assertThat(availableBooks).hasSize(2);
        assertThat(availableBooks).containsExactlyInAnyOrder(available1, available2);
    }

    @Test
    @DisplayName("should delete book")
    void shouldDeleteBook() {
        // ARRANGE
        Book book = aBook().build();
        entityManager.persist(book);
        entityManager.flush();
        BookId bookId = book.getId();

        // ACT
        bookRepository.delete(book);
        entityManager.flush();

        // ASSERT
        assertThat(bookRepository.findById(bookId)).isEmpty();
    }

    @Test
    @DisplayName("should enforce unique ISBN constraint")
    void shouldEnforceUniqueIsbnConstraint() {
        // ARRANGE
        ISBN isbn = ISBN.fromString("9780132350884");
        Book book1 = aBook().withIsbn(isbn).build();
        Book book2 = aBook().withIsbn(isbn).build();  // Same ISBN!

        entityManager.persist(book1);
        entityManager.flush();

        // ACT & ASSERT
        assertThatThrownBy(() -> {
            entityManager.persist(book2);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

### Example 2: CheckOutBookService Integration Test

```java
@SpringBootTest
@Testcontainers
@Transactional
class CheckOutBookServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private CheckOutBookService checkOutService;

    @Autowired
    private BookRepository bookRepository;

    @Test
    @DisplayName("should check out available book and persist to database")
    void shouldCheckOutAvailableBook() {
        // ARRANGE
        Book book = aBook().available().build();
        bookRepository.save(book);
        BookId bookId = book.getId();

        // ACT
        checkOutService.checkOut(bookId);

        // ASSERT
        Book updated = bookRepository.findById(bookId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AvailabilityStatus.CHECKED_OUT);
        assertThat(updated.getCheckoutCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("should throw BookNotFoundException when book doesn't exist")
    void shouldThrowWhenBookNotFound() {
        // ARRANGE
        BookId nonExistentId = BookId.generate();

        // ACT & ASSERT
        assertThatThrownBy(() -> checkOutService.checkOut(nonExistentId))
            .isInstanceOf(BookNotFoundException.class)
            .hasMessageContaining(nonExistentId.toString());
    }

    @Test
    @DisplayName("should not persist changes when checkout fails")
    void shouldNotPersistWhenCheckoutFails() {
        // ARRANGE
        Book book = aBook().checkedOut().build();  // Already checked out
        bookRepository.save(book);
        BookId bookId = book.getId();
        int originalCheckoutCount = book.getCheckoutCount();

        // ACT
        assertThatThrownBy(() -> checkOutService.checkOut(bookId))
            .isInstanceOf(BookNotAvailableException.class);

        // ASSERT - transaction should be rolled back
        Book unchanged = bookRepository.findById(bookId).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AvailabilityStatus.CHECKED_OUT);
        assertThat(unchanged.getCheckoutCount()).isEqualTo(originalCheckoutCount);
    }
}
```

### Example 3: AddBookService Integration Test

```java
@SpringBootTest
@Testcontainers
@Transactional
class AddBookServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private AddBookService addBookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Test
    @DisplayName("should create book with new author")
    void shouldCreateBookWithNewAuthor() {
        // ARRANGE
        AddBookCommand command = new AddBookCommand(
            "Clean Code",
            "9780132350884",
            Set.of("Robert C. Martin")
        );

        // ACT
        BookId bookId = addBookService.addBook(command);

        // ASSERT
        Book savedBook = bookRepository.findById(bookId).orElseThrow();
        assertThat(savedBook.getTitle().value()).isEqualTo("Clean Code");
        assertThat(savedBook.getIsbn().value()).isEqualTo("9780132350884");

        // Verify author was created
        List<Author> authors = authorRepository.findAll();
        assertThat(authors).hasSize(1);
        assertThat(authors.get(0).getFullName()).isEqualTo("Robert C. Martin");
    }

    @Test
    @DisplayName("should reuse existing author when creating book")
    void shouldReuseExistingAuthor() {
        // ARRANGE
        Author existingAuthor = Author.create(AuthorId.generate(), "Martin Fowler");
        authorRepository.save(existingAuthor);

        AddBookCommand command = new AddBookCommand(
            "Refactoring",
            "9780201485677",
            Set.of("Martin Fowler")  // Existing author
        );

        // ACT
        BookId bookId = addBookService.addBook(command);

        // ASSERT
        Book savedBook = bookRepository.findById(bookId).orElseThrow();
        assertThat(savedBook.getAuthorIds()).contains(existingAuthor.getId());

        // Verify no duplicate author was created
        List<Author> authors = authorRepository.findAll();
        assertThat(authors).hasSize(1);  // Still only one author
    }

    @Test
    @DisplayName("should create book with multiple authors")
    void shouldCreateBookWithMultipleAuthors() {
        // ARRANGE
        AddBookCommand command = new AddBookCommand(
            "Domain-Driven Design",
            "9780321125217",
            Set.of("Eric Evans", "Martin Fowler")
        );

        // ACT
        BookId bookId = addBookService.addBook(command);

        // ASSERT
        Book savedBook = bookRepository.findById(bookId).orElseThrow();
        assertThat(savedBook.getAuthorIds()).hasSize(2);

        List<Author> authors = authorRepository.findAll();
        assertThat(authors).hasSize(2);
        assertThat(authors)
            .extracting(Author::getFullName)
            .containsExactlyInAnyOrder("Eric Evans", "Martin Fowler");
    }

    @Test
    @DisplayName("should fail transaction when book save fails")
    void shouldRollbackWhenBookSaveFails() {
        // ARRANGE
        AddBookCommand command = new AddBookCommand(
            "Test Book",
            "invalid-isbn",  // Invalid ISBN - will fail
            Set.of("Test Author")
        );

        // ACT & ASSERT
        assertThatThrownBy(() -> addBookService.addBook(command))
            .isInstanceOf(IllegalArgumentException.class);

        // Verify transaction was rolled back - no author created
        List<Author> authors = authorRepository.findAll();
        assertThat(authors).isEmpty();
    }
}
```

---

## Part 5: Transaction Management in Tests

### Understanding @Transactional in Tests

**Without @Transactional**:
```java
@SpringBootTest
class BookServiceTest {

    @Test
    void test1() {
        bookRepository.save(book1);  // Saved to database
    }

    @Test
    void test2() {
        // book1 is still in database! 😱
        List<Book> books = bookRepository.findAll();
        assertThat(books).hasSize(1);  // Fails if test1 ran first
    }
}
```

**With @Transactional**:
```java
@SpringBootTest
@Transactional  // Each test gets its own transaction
class BookServiceTest {

    @Test
    void test1() {
        bookRepository.save(book1);  // Saved in transaction
        // After test: ROLLED BACK
    }

    @Test
    void test2() {
        // Database is clean!
        List<Book> books = bookRepository.findAll();
        assertThat(books).isEmpty();  // ✅ Passes
    }
}
```

### Testing Actual Transaction Behavior

**Problem**: @Transactional hides transaction boundary issues.

**Solution**: Use @Commit to test actual commits.

```java
@SpringBootTest
class BookServiceTransactionTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Test
    @Transactional
    @Commit  // Actually commits instead of rolling back
    void shouldCommitTransaction() {
        bookService.addBook(command);

        // Transaction is committed - changes persisted
    }

    @AfterEach
    void cleanup() {
        bookRepository.deleteAll();  // Manual cleanup needed
    }
}
```

### Testing Transactional Boundaries

```java
@SpringBootTest
@Transactional
class TransactionalBoundaryTest {

    @Autowired
    private CheckOutBookService service;

    @Autowired
    private BookRepository repository;

    @Test
    @DisplayName("should rollback transaction when business rule fails")
    void shouldRollbackWhenBusinessRuleFails() {
        // ARRANGE
        Book book = aBook().checkedOut().build();  // Already checked out
        repository.save(book);
        repository.flush();  // Force write to database

        BookId bookId = book.getId();
        int originalCheckoutCount = book.getCheckoutCount();

        // ACT
        assertThatThrownBy(() -> service.checkOut(bookId))
            .isInstanceOf(BookNotAvailableException.class);

        // ASSERT
        repository.flush();  // Ensure no pending writes
        Book unchanged = repository.findById(bookId).orElseThrow();

        // Verify transaction was rolled back
        assertThat(unchanged.getCheckoutCount()).isEqualTo(originalCheckoutCount);
    }
}
```

---

## Part 6: Integration Test Best Practices

### 1. Test Critical Paths Only

**Don't test everything with integration tests** - they're slow.

**Test**:
- ✅ Complex queries
- ✅ Transaction behavior
- ✅ Database constraints
- ✅ Critical user flows

**Don't test**:
- ❌ Simple CRUD (already tested by Spring Data)
- ❌ Domain logic (use unit tests)
- ❌ Every service method

### 2. Use Test Data Builders

```java
// ❌ Bad: Inline test data setup
@Test
void test() {
    Book book = new Book();
    book.setTitle(Title.of("Test"));
    book.setIsbn(ISBN.fromString("9780132350884"));
    book.setStatus(AvailabilityStatus.AVAILABLE);
    // ... 10 more lines
}

// ✅ Good: Test data builder
@Test
void test() {
    Book book = aBook().available().build();
}
```

### 3. Clean Up After Tests

**With @Transactional** (automatic):
```java
@SpringBootTest
@Transactional  // ✅ Auto-rollback
class BookServiceTest {
    // No cleanup needed
}
```

**Without @Transactional** (manual):
```java
@SpringBootTest
class BookServiceTest {

    @AfterEach
    void cleanup() {
        bookRepository.deleteAll();  // ✅ Manual cleanup
    }
}
```

### 4. Use Testcontainers for CI/CD

**Testcontainers works in CI/CD pipelines** if Docker is available.

**GitHub Actions example**:
```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Run integration tests
        run: mvn verify -P integration-tests
```

**Maven profile** (`pom.xml`):
```xml
<profiles>
    <profile>
        <id>integration-tests</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <executions>
                        <execution>
                            <goals>
                                <goal>integration-test</goal>
                                <goal>verify</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

### 5. Separate Unit and Integration Tests

**Directory structure**:
```
src/
  test/
    java/
      com/penrose/bibby/
        library/book/
          BookTest.java                    # Unit test
          BookRepositoryIntegrationTest.java   # Integration test
          CheckOutBookServiceIntegrationTest.java
```

**Naming convention**:
- Unit tests: `*Test.java`
- Integration tests: `*IntegrationTest.java`

**Run separately**:
```bash
# Unit tests only (fast)
mvn test

# Integration tests only (slower)
mvn verify -P integration-tests
```

---

## Action Items: Building Bibby's Integration Test Suite

### Action Item 1: Set Up Testcontainers (1 hour)

**Add dependencies** (`pom.xml`):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

**Create base class**:
```java
@SpringBootTest
@Testcontainers
@Transactional
public abstract class IntegrationTestBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
}
```

### Action Item 2: Test BookRepository (2 hours)

Create `BookRepositoryIntegrationTest.java` with:
- ✅ Find by ISBN
- ✅ Find by status
- ✅ Delete book
- ✅ Unique ISBN constraint

**Target**: 5+ tests

### Action Item 3: Test CheckOutBookService (2 hours)

Create `CheckOutBookServiceIntegrationTest.java` with:
- ✅ Checkout available book
- ✅ Book not found exception
- ✅ Transaction rollback on failure

**Target**: 4+ tests

### Action Item 4: Test AddBookService (2-3 hours)

Create `AddBookServiceIntegrationTest.java` with:
- ✅ Create book with new author
- ✅ Reuse existing author
- ✅ Multiple authors
- ✅ Transaction rollback on failure

**Target**: 5+ tests

### Action Item 5: Set Up CI/CD Integration Tests (1 hour)

**Create Maven profile**:
```xml
<profile>
    <id>integration-tests</id>
    <!-- Configuration shown above -->
</profile>
```

**Add GitHub Actions** (`.github/workflows/integration-tests.yml`):
```yaml
name: Integration Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
      - run: mvn verify -P integration-tests
```

---

## Key Takeaways

### 1. Integration Tests Catch Different Bugs

Unit tests catch logic bugs. Integration tests catch integration bugs (schema mismatches, transaction issues, constraint violations).

### 2. Testcontainers Provides Real Database

Test with actual PostgreSQL, not H2 in-memory substitute.

### 3. @Transactional Auto-Cleans Tests

Each test runs in a transaction, rolled back afterward. Database stays clean.

### 4. Test Critical Paths, Not Everything

Integration tests are slower. Focus on complex queries, transactions, and critical flows.

### 5. Balance Speed and Realism

- Unit tests: Fast, isolated
- Integration tests: Slower, realistic
- E2E tests: Slowest, full system

Use all three in appropriate ratios.

---

## Study Resources

### Books
1. **"Spring Boot Up and Running"** by Mark Heckler
   - Chapter on testing

2. **"Testing Java Microservices"** by Alex Soto Bueno
   - Testcontainers patterns

### Documentation
1. **Spring Boot Testing** - https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing
2. **Testcontainers** - https://www.testcontainers.org/
3. **@DataJpaTest** - https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/test/autoconfigure/orm/jpa/DataJpaTest.html

---

## Coming Next

**Section 27: Test-Driven Development (TDD)** - Red, Green, Refactor

We'll cover:
- The TDD cycle
- Writing tests first
- Benefits and drawbacks
- Applying TDD to Bibby features
- When to use TDD vs. test-after

---

**Section 26 Complete** | **Time Invested**: 4-5 hours

You now understand how to test beyond isolation with real databases and Spring Boot. Integration tests give you confidence that your application actually works in a realistic environment.

Next, we'll learn Test-Driven Development - writing tests **before** writing code!

