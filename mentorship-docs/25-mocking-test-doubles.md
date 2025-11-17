# SECTION 25: MOCKING & TEST DOUBLES

## Introduction: Testing with Dependencies

**"Don't mock everything. Mock what you must."**

In Section 24, you learned to write unit tests for pure domain logic - code with **no dependencies**. But what about code that **has dependencies**?

```java
// How do you test this without a real database?
@Service
public class CheckOutBookService {
    private final BookRepository bookRepository;  // Database dependency!

    public void checkOut(BookId bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow();
        book.checkOut();
        bookRepository.save(book);
    }
}
```

**Answer**: Use **test doubles** - stand-ins for real dependencies.

In this section, we'll master mocking and test doubles, learning when to use them (and when not to) in your **Bibby** application.

**Learning Objectives**:
- Understand the five types of test doubles
- Know when to use mocks vs. real objects
- Master Mockito framework
- Test services with mocked repositories
- Use argument matchers and verification
- Avoid over-mocking pitfalls

**Time Investment**: 3-4 hours

---

## Part 1: The Five Types of Test Doubles

**Test Double** = Generic term for any object that stands in for a real dependency during testing.

There are **five types**, each with a specific purpose:

### 1. Dummy

**Purpose**: Satisfy a parameter requirement, but never actually used.

**Example**:
```java
// EmailService is required but never called in this test
@Test
void shouldCreateBookWithoutSendingEmail() {
    EmailService dummyEmail = null;  // Dummy! Never used
    BookService service = new BookService(bookRepository, dummyEmail);

    service.addBook(command);  // Doesn't send email

    // Test passes - email was never called
}
```

**When to use**: When you need to pass something but it won't be used.

### 2. Stub

**Purpose**: Provide canned answers to calls.

**Example**:
```java
// Stub always returns the same book
BookRepository stubRepository = new BookRepositoryStub();

class BookRepositoryStub implements BookRepository {
    @Override
    public Optional<Book> findById(BookId id) {
        // Always returns same book, regardless of ID
        return Optional.of(aBook().build());
    }

    @Override
    public void save(Book book) {
        // Do nothing
    }
}

@Test
void shouldCheckOutBookUsingStub() {
    BookRepository stub = new BookRepositoryStub();
    CheckOutBookService service = new CheckOutBookService(stub);

    service.checkOut(BookId.generate());  // Uses stubbed book

    // Test behavior, not data
}
```

**When to use**: When you need simple, predictable responses.

### 3. Spy

**Purpose**: Real object that records how it was used.

**Example**:
```java
BookRepository spyRepository = Mockito.spy(new RealBookRepository());

@Test
void shouldCallSaveWhenCheckingOut() {
    CheckOutBookService service = new CheckOutBookService(spyRepository);

    service.checkOut(bookId);

    // Verify save was called (spy recorded it)
    verify(spyRepository).save(any(Book.class));
}
```

**When to use**: When you want mostly real behavior but need to verify calls.

### 4. Mock

**Purpose**: Verify that specific interactions occurred.

**Example**:
```java
BookRepository mockRepository = Mockito.mock(BookRepository.class);

@Test
void shouldSaveBookAfterCheckout() {
    when(mockRepository.findById(any())).thenReturn(Optional.of(aBook().build()));
    CheckOutBookService service = new CheckOutBookService(mockRepository);

    service.checkOut(bookId);

    // Verify save was called exactly once with any Book
    verify(mockRepository, times(1)).save(any(Book.class));
}
```

**When to use**: When you need to verify method calls and interactions.

### 5. Fake

**Purpose**: Working implementation, but takes shortcuts (e.g., in-memory database).

**Example**:
```java
// Fake repository - stores in HashMap, not real database
public class InMemoryBookRepository implements BookRepository {
    private final Map<BookId, Book> books = new HashMap<>();

    @Override
    public Optional<Book> findById(BookId id) {
        return Optional.ofNullable(books.get(id));
    }

    @Override
    public void save(Book book) {
        books.put(book.getId(), book);
    }
}

@Test
void shouldStoreAndRetrieveBook() {
    BookRepository fakeRepo = new InMemoryBookRepository();

    Book book = aBook().build();
    fakeRepo.save(book);

    Optional<Book> retrieved = fakeRepo.findById(book.getId());
    assertThat(retrieved).isPresent();
}
```

**When to use**: When you need realistic behavior without external dependencies.

### Comparison

| Type | Returns Values | Verifies Calls | Complexity |
|------|----------------|----------------|------------|
| **Dummy** | No | No | Trivial |
| **Stub** | Yes (fixed) | No | Simple |
| **Spy** | Yes (real) | Yes | Medium |
| **Mock** | Yes (configured) | Yes | Medium |
| **Fake** | Yes (realistic) | No | High |

---

## Part 2: When to Mock vs. When to Use Real Objects

### The Golden Rule

**"Don't mock what you don't own."** - Steve Freeman & Nat Pryce

**Mock**:
- ✅ Your own interfaces (`BookRepository`, `AuthorService`)
- ✅ Dependencies you control
- ✅ Slow operations (database, network, file I/O)

**Don't Mock**:
- ❌ Third-party libraries (Mockito, Spring, etc.)
- ❌ Value objects (`ISBN`, `Money`, `Title`)
- ❌ Entities (`Book`, `Author`)
- ❌ Simple objects (String, List, etc.)

### Example: What to Mock in Bibby

**Scenario**: Testing `CheckOutBookService`

```java
@Service
public class CheckOutBookService {
    private final BookRepository bookRepository;        // ✅ MOCK (database)
    private final DomainEventPublisher eventPublisher;  // ✅ MOCK (external)

    public void checkOut(BookId bookId) {               // ❌ DON'T MOCK (value object)
        Book book = bookRepository.findById(bookId)     // ❌ DON'T MOCK (entity)
            .orElseThrow();

        book.checkOut();                                // ❌ DON'T MOCK (domain logic)

        bookRepository.save(book);                      // ✅ MOCK (database)
        eventPublisher.publish(new BookCheckedOutEvent(bookId));  // ✅ MOCK
    }
}
```

**Test**:
```java
@Test
void shouldCheckOutBook() {
    // MOCK repositories and external services
    BookRepository mockRepo = mock(BookRepository.class);
    DomainEventPublisher mockPublisher = mock(DomainEventPublisher.class);

    // USE REAL value objects and entities
    BookId realBookId = BookId.generate();
    Book realBook = aBook().available().build();

    when(mockRepo.findById(realBookId)).thenReturn(Optional.of(realBook));

    CheckOutBookService service = new CheckOutBookService(mockRepo, mockPublisher);

    // ACT
    service.checkOut(realBookId);

    // ASSERT
    assertThat(realBook.getStatus()).isEqualTo(CHECKED_OUT);  // Real object!
    verify(mockRepo).save(realBook);  // Mock verified!
}
```

**Why this works**:
- Real `Book` and `BookId` - tests actual domain logic
- Mocked `BookRepository` - no database needed
- Mocked `DomainEventPublisher` - no event bus needed

### Prefer Real Objects When Possible

**Bad** (over-mocking):
```java
@Test
void shouldAddTwoNumbers() {
    Calculator mockCalc = mock(Calculator.class);
    when(mockCalc.add(2, 3)).thenReturn(5);  // ❌ Why mock this?!

    int result = mockCalc.add(2, 3);

    assertThat(result).isEqualTo(5);  // Tests mock, not real code!
}
```

**Good** (real object):
```java
@Test
void shouldAddTwoNumbers() {
    Calculator calc = new Calculator();  // ✅ Real object

    int result = calc.add(2, 3);

    assertThat(result).isEqualTo(5);  // Tests real logic!
}
```

---

## Part 3: Mockito Fundamentals

### Setup

**Add dependency** (`pom.xml`):
```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.5.0</version>
    <scope>test</scope>
</dependency>
```

**Import static methods**:
```java
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
```

### Creating Mocks

**Option 1: Manual creation**
```java
@Test
void test() {
    BookRepository mockRepo = mock(BookRepository.class);
}
```

**Option 2: @Mock annotation**
```java
@ExtendWith(MockitoExtension.class)
class CheckOutBookServiceTest {

    @Mock
    private BookRepository mockRepository;

    @Mock
    private DomainEventPublisher mockPublisher;

    @InjectMocks  // Injects mocks into this
    private CheckOutBookService service;

    @Test
    void test() {
        // mockRepository and mockPublisher are already created!
    }
}
```

**@InjectMocks** automatically injects all `@Mock` fields into the service's constructor.

### Stubbing Responses

**when().thenReturn()**:
```java
Book book = aBook().build();

when(mockRepository.findById(bookId)).thenReturn(Optional.of(book));

// Now when service calls findById, it returns our book
Optional<Book> result = mockRepository.findById(bookId);
assertThat(result).contains(book);
```

**Multiple calls**:
```java
when(mockRepository.findById(bookId))
    .thenReturn(Optional.of(book1))  // First call
    .thenReturn(Optional.of(book2))  // Second call
    .thenReturn(Optional.empty());   // Third call
```

**Throwing exceptions**:
```java
when(mockRepository.findById(bookId))
    .thenThrow(new DatabaseException("Connection failed"));

// Now when service calls findById, exception is thrown
assertThatThrownBy(() -> mockRepository.findById(bookId))
    .isInstanceOf(DatabaseException.class);
```

### Verification

**Verify method was called**:
```java
service.checkOut(bookId);

verify(mockRepository).save(any(Book.class));
// Verifies save was called with any Book argument
```

**Verify call count**:
```java
verify(mockRepository, times(1)).save(any(Book.class));  // Exactly 1 time
verify(mockRepository, times(2)).findById(bookId);       // Exactly 2 times
verify(mockRepository, never()).delete(any(Book.class)); // Never called
verify(mockRepository, atLeastOnce()).save(any());       // At least once
verify(mockRepository, atMost(3)).save(any());           // At most 3 times
```

**Verify no more interactions**:
```java
service.checkOut(bookId);

verify(mockRepository).findById(bookId);
verify(mockRepository).save(any());
verifyNoMoreInteractions(mockRepository);  // Fails if other methods were called
```

---

## Part 4: Testing Bibby Services with Mockito

### Example 1: CheckOutBookService

**Service code**:
```java
@Service
public class CheckOutBookService {
    private final BookRepository bookRepository;
    private final DomainEventPublisher eventPublisher;

    public CheckOutBookService(BookRepository bookRepository,
                               DomainEventPublisher eventPublisher) {
        this.bookRepository = bookRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void checkOut(BookId bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        book.checkOut();

        bookRepository.save(book);
        eventPublisher.publish(new BookCheckedOutEvent(bookId, LocalDateTime.now()));
    }
}
```

**Test suite**:
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("CheckOutBookService")
class CheckOutBookServiceTest {

    @Mock
    private BookRepository mockRepository;

    @Mock
    private DomainEventPublisher mockPublisher;

    @InjectMocks
    private CheckOutBookService service;

    @Test
    @DisplayName("should check out available book")
    void shouldCheckOutAvailableBook() {
        // ARRANGE
        BookId bookId = BookId.generate();
        Book book = aBook().withId(bookId).available().build();

        when(mockRepository.findById(bookId)).thenReturn(Optional.of(book));

        // ACT
        service.checkOut(bookId);

        // ASSERT
        assertThat(book.getStatus()).isEqualTo(AvailabilityStatus.CHECKED_OUT);
        verify(mockRepository).save(book);
        verify(mockPublisher).publish(any(BookCheckedOutEvent.class));
    }

    @Test
    @DisplayName("should throw when book not found")
    void shouldThrowWhenBookNotFound() {
        // ARRANGE
        BookId nonExistentId = BookId.generate();
        when(mockRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> service.checkOut(nonExistentId))
            .isInstanceOf(BookNotFoundException.class);

        // Verify save was never called
        verify(mockRepository, never()).save(any());
        verify(mockPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("should throw when book already checked out")
    void shouldThrowWhenBookAlreadyCheckedOut() {
        // ARRANGE
        BookId bookId = BookId.generate();
        Book checkedOutBook = aBook().withId(bookId).checkedOut().build();

        when(mockRepository.findById(bookId)).thenReturn(Optional.of(checkedOutBook));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.checkOut(bookId))
            .isInstanceOf(BookNotAvailableException.class);

        // Verify save was never called (checkout failed)
        verify(mockRepository, never()).save(any());
    }

    @Test
    @DisplayName("should publish event with correct book ID")
    void shouldPublishEventWithCorrectBookId() {
        // ARRANGE
        BookId bookId = BookId.generate();
        Book book = aBook().withId(bookId).available().build();

        when(mockRepository.findById(bookId)).thenReturn(Optional.of(book));

        ArgumentCaptor<BookCheckedOutEvent> eventCaptor =
            ArgumentCaptor.forClass(BookCheckedOutEvent.class);

        // ACT
        service.checkOut(bookId);

        // ASSERT
        verify(mockPublisher).publish(eventCaptor.capture());
        BookCheckedOutEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getBookId()).isEqualTo(bookId);
    }
}
```

### Example 2: AddBookService

**Service code**:
```java
@Service
public class AddBookService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookFactory bookFactory;

    @Transactional
    public BookId addBook(AddBookCommand command) {
        // Find or create authors
        Set<AuthorId> authorIds = findOrCreateAuthors(command.getAuthorNames());

        // Create book
        Book book = bookFactory.createBook(
            Title.of(command.getTitle()),
            ISBN.fromString(command.getIsbn()),
            authorIds
        );

        // Save
        bookRepository.save(book);

        return book.getId();
    }

    private Set<AuthorId> findOrCreateAuthors(Set<String> authorNames) {
        return authorNames.stream()
            .map(name -> findOrCreateAuthor(name))
            .collect(Collectors.toSet());
    }

    private AuthorId findOrCreateAuthor(String fullName) {
        return authorRepository.findByFullName(fullName)
            .map(Author::getId)
            .orElseGet(() -> createNewAuthor(fullName));
    }

    private AuthorId createNewAuthor(String fullName) {
        Author author = Author.create(AuthorId.generate(), fullName);
        authorRepository.save(author);
        return author.getId();
    }
}
```

**Test**:
```java
@ExtendWith(MockitoExtension.class)
class AddBookServiceTest {

    @Mock
    private BookRepository mockBookRepository;

    @Mock
    private AuthorRepository mockAuthorRepository;

    @Mock
    private BookFactory mockBookFactory;

    @InjectMocks
    private AddBookService service;

    @Test
    @DisplayName("should create book with existing author")
    void shouldCreateBookWithExistingAuthor() {
        // ARRANGE
        String authorName = "Robert C. Martin";
        AuthorId existingAuthorId = AuthorId.generate();
        Author existingAuthor = Author.create(existingAuthorId, authorName);

        when(mockAuthorRepository.findByFullName(authorName))
            .thenReturn(Optional.of(existingAuthor));

        Book createdBook = aBook().build();
        when(mockBookFactory.createBook(any(), any(), any())).thenReturn(createdBook);

        AddBookCommand command = new AddBookCommand(
            "Clean Code",
            "9780132350884",
            Set.of(authorName)
        );

        // ACT
        BookId bookId = service.addBook(command);

        // ASSERT
        assertThat(bookId).isEqualTo(createdBook.getId());

        // Verify author was NOT created (already exists)
        verify(mockAuthorRepository, never()).save(any(Author.class));

        // Verify book was saved
        verify(mockBookRepository).save(createdBook);
    }

    @Test
    @DisplayName("should create new author when not found")
    void shouldCreateNewAuthorWhenNotFound() {
        // ARRANGE
        String newAuthorName = "Martin Fowler";

        when(mockAuthorRepository.findByFullName(newAuthorName))
            .thenReturn(Optional.empty());  // Not found

        Book createdBook = aBook().build();
        when(mockBookFactory.createBook(any(), any(), any())).thenReturn(createdBook);

        AddBookCommand command = new AddBookCommand(
            "Refactoring",
            "9780201485677",
            Set.of(newAuthorName)
        );

        // ACT
        service.addBook(command);

        // ASSERT
        ArgumentCaptor<Author> authorCaptor = ArgumentCaptor.forClass(Author.class);
        verify(mockAuthorRepository).save(authorCaptor.capture());

        Author savedAuthor = authorCaptor.getValue();
        assertThat(savedAuthor.getFullName()).isEqualTo(newAuthorName);
    }
}
```

---

## Part 5: Argument Matchers

### Basic Matchers

```java
// Any instance of type
verify(mockRepository).save(any(Book.class));
verify(mockRepository).save(any());  // Any object

// Specific value
verify(mockRepository).findById(eq(bookId));

// Null
verify(mockRepository).save(isNull());
verify(mockRepository).save(notNull());

// Number matchers
verify(mockService).processBooks(anyInt());
verify(mockService).processBooks(gt(10));  // Greater than 10
verify(mockService).processBooks(lt(100)); // Less than 100

// String matchers
verify(mockService).searchBooks(anyString());
verify(mockService).searchBooks(startsWith("Clean"));
verify(mockService).searchBooks(contains("Code"));
verify(mockService).searchBooks(matches("\\d{13}"));  // Regex
```

### Custom Matchers with ArgumentMatchers

```java
@Test
void shouldSaveBookWithCorrectStatus() {
    service.checkOut(bookId);

    // Custom matcher: verify book has CHECKED_OUT status
    verify(mockRepository).save(argThat(book ->
        book.getStatus() == AvailabilityStatus.CHECKED_OUT
    ));
}

@Test
void shouldPublishEventWithRecentTimestamp() {
    service.checkOut(bookId);

    verify(mockPublisher).publish(argThat(event -> {
        LocalDateTime timestamp = event.getTimestamp();
        return timestamp.isAfter(LocalDateTime.now().minusSeconds(5));
    }));
}
```

### ArgumentCaptor for Complex Verification

```java
@Test
void shouldSaveBookWithIncrementedCheckoutCount() {
    // ARRANGE
    Book book = aBook().available().withCheckoutCount(5).build();
    when(mockRepository.findById(any())).thenReturn(Optional.of(book));

    ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);

    // ACT
    service.checkOut(bookId);

    // ASSERT
    verify(mockRepository).save(bookCaptor.capture());

    Book savedBook = bookCaptor.getValue();
    assertThat(savedBook.getCheckoutCount()).isEqualTo(6);
    assertThat(savedBook.getStatus()).isEqualTo(AvailabilityStatus.CHECKED_OUT);
}
```

---

## Part 6: Advanced Mockito Techniques

### Spying on Real Objects

```java
@Test
void shouldCallRealMethodButVerifyIt() {
    BookRepository realRepo = new InMemoryBookRepository();
    BookRepository spyRepo = spy(realRepo);

    CheckOutBookService service = new CheckOutBookService(spyRepo, mockPublisher);

    Book book = aBook().build();
    spyRepo.save(book);  // Real save!

    service.checkOut(book.getId());

    // Verify real repository was called
    verify(spyRepo).findById(book.getId());
    verify(spyRepo, times(2)).save(any());  // Once in setup, once in service
}
```

### Stubbing Void Methods

```java
@Test
void shouldHandleExceptionWhenPublishingEvent() {
    Book book = aBook().build();
    when(mockRepository.findById(any())).thenReturn(Optional.of(book));

    // Stub void method to throw exception
    doThrow(new EventPublishException("Queue full"))
        .when(mockPublisher).publish(any());

    assertThatThrownBy(() -> service.checkOut(book.getId()))
        .isInstanceOf(EventPublishException.class);
}
```

### Answering with Custom Logic

```java
@Test
void shouldUseCustomAnswerLogic() {
    // Return different books based on ID
    when(mockRepository.findById(any())).thenAnswer(invocation -> {
        BookId id = invocation.getArgument(0);
        if (id.value().startsWith("test")) {
            return Optional.of(aBook().withId(id).build());
        }
        return Optional.empty();
    });

    BookId testId = BookId.of("test-123");
    Optional<Book> book = mockRepository.findById(testId);

    assertThat(book).isPresent();
}
```

---

## Part 7: Common Pitfalls and Best Practices

### Pitfall 1: Over-Mocking

**Bad** (mocks everything):
```java
@Test
void overMockingExample() {
    Book mockBook = mock(Book.class);  // ❌ Don't mock entities!
    when(mockBook.getStatus()).thenReturn(AvailabilityStatus.AVAILABLE);
    when(mockBook.getId()).thenReturn(bookId);

    // You're testing mocks, not real logic!
}
```

**Good** (mocks only external dependencies):
```java
@Test
void properMockingExample() {
    Book realBook = aBook().available().build();  // ✅ Real entity
    when(mockRepository.findById(any())).thenReturn(Optional.of(realBook));

    // Tests real domain logic
}
```

### Pitfall 2: Testing Implementation Details

**Bad** (brittle test):
```java
@Test
void testImplementationDetails() {
    service.checkOut(bookId);

    // ❌ Tests HOW (implementation), not WHAT (behavior)
    InOrder inOrder = inOrder(mockRepository, mockPublisher);
    inOrder.verify(mockRepository).findById(bookId);
    inOrder.verify(mockRepository).save(any());
    inOrder.verify(mockPublisher).publish(any());
    // If you reorder these in service, test breaks!
}
```

**Good** (tests behavior):
```java
@Test
void testBehavior() {
    Book book = aBook().available().build();
    when(mockRepository.findById(bookId)).thenReturn(Optional.of(book));

    service.checkOut(bookId);

    // ✅ Tests WHAT (book is checked out)
    assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
    verify(mockRepository).save(book);
}
```

### Pitfall 3: Not Resetting Mocks Between Tests

**Bad** (shared mock state):
```java
class BadTest {
    private BookRepository mockRepo = mock(BookRepository.class);  // ❌ Shared!

    @Test
    void test1() {
        when(mockRepo.findById(any())).thenReturn(Optional.of(book1));
        // ...
    }

    @Test
    void test2() {
        // Still has stub from test1! 😱
        when(mockRepo.findById(any())).thenReturn(Optional.of(book2));
        // ...
    }
}
```

**Good** (fresh mocks):
```java
@ExtendWith(MockitoExtension.class)
class GoodTest {
    @Mock  // ✅ Fresh mock for each test!
    private BookRepository mockRepo;

    @Test
    void test1() { /* ... */ }

    @Test
    void test2() { /* ... */ }
}
```

### Best Practice: Verify Behavior, Not Calls

**Focus on outcomes, not method calls.**

**OK**:
```java
verify(mockRepository).save(book);
// Makes sense if saving is the purpose of the test
```

**Better**:
```java
assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
// Tests actual behavior change
```

---

## Action Items: Adding Mocking to Bibby Tests

### Action Item 1: Set Up Mockito (15 min)

Add Mockito dependency (already in `pom.xml` from Section 24).

Create base test class:
```java
@ExtendWith(MockitoExtension.class)
public abstract class ServiceTestBase {
    // Shared test utilities
}
```

### Action Item 2: Test CheckOutBookService (1-2 hours)

Create `CheckOutBookServiceTest.java` with:
- ✅ Happy path (book checked out successfully)
- ✅ Book not found exception
- ✅ Book already checked out exception
- ✅ Event published with correct data
- ✅ Repository save called

**Target**: 5+ tests

### Action Item 3: Test AddBookService (2 hours)

Create `AddBookServiceTest.java` with:
- ✅ Create book with existing author
- ✅ Create book with new author
- ✅ Create book with multiple authors
- ✅ Handle invalid ISBN

**Target**: 6+ tests

### Action Item 4: Create In-Memory Fake Repository (2 hours)

For tests that need realistic repository behavior:

```java
public class InMemoryBookRepository implements BookRepository {
    private final Map<BookId, Book> books = new ConcurrentHashMap<>();

    @Override
    public Optional<Book> findById(BookId id) {
        return Optional.ofNullable(books.get(id));
    }

    @Override
    public void save(Book book) {
        books.put(book.getId(), book);
    }

    @Override
    public void delete(Book book) {
        books.remove(book.getId());
    }

    // Other methods...
}
```

Use in tests that need stateful repository behavior.

### Action Item 5: Review and Reduce Over-Mocking (1 hour)

Audit existing tests:
- Are you mocking value objects? Replace with real instances.
- Are you mocking entities? Replace with real test builders.
- Are you mocking simple objects? Use real instances.

**Target**: 80% real objects, 20% mocks.

---

## Key Takeaways

### 1. Five Types of Test Doubles

- **Dummy**: Unused placeholder
- **Stub**: Fixed responses
- **Spy**: Real object with recording
- **Mock**: Verify interactions
- **Fake**: Realistic implementation

### 2. Don't Mock What You Don't Own

Mock your interfaces, not third-party libraries or value objects.

### 3. Prefer Real Objects

Only mock when necessary (slow operations, external dependencies).

### 4. Mockito is Powerful

`when().thenReturn()`, `verify()`, argument matchers, and captors.

### 5. Test Behavior, Not Implementation

Focus on what the code does, not how it does it.

---

## Study Resources

### Books
1. **"Growing Object-Oriented Software, Guided by Tests"** - Freeman & Pryce
   - Chapter on mocks
   - "Don't mock types you don't own"

2. **"Unit Testing Principles, Practices, and Patterns"** - Vladimir Khorikov
   - Chapter 5: Mocks and test fragility

### Documentation
1. **Mockito Documentation** - https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
2. **Mockito Wiki** - https://github.com/mockito/mockito/wiki

---

## Coming Next

**Section 26: Integration Testing** - Testing with real dependencies

We'll cover:
- When to write integration tests
- Spring Boot test support
- Testing with real database (Testcontainers)
- Transaction management in tests
- Integration test best practices

---

**Section 25 Complete** | **Time Invested**: 3-4 hours

You now understand when and how to use mocks effectively. Remember: mocks are a tool, not a goal. Use them sparingly, test real behavior, and your tests will be maintainable and valuable.
