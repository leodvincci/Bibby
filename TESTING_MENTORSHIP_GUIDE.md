# Unit Testing Mentorship Guide: JUnit 5 + Mockito

**Welcome!** Let's level up your testing game. Think of this as us pair-programming together, except I'm showing you the ropes on how to write solid unit tests for your Bibby library management system.

---

## Part 1: Understanding Your Codebase

### The 30,000-Foot View

Your project is a **Spring Boot library management CLI application** with these layers:

```
Bibby (Library Management System)
│
├── 📦 Domain Layer (library/*)
│   ├── book/         → Book entities, repos, services
│   ├── author/       → Author entities, repos, services
│   ├── shelf/        → Shelf entities, repos, services
│   ├── bookcase/     → Bookcase entities, repos, services
│   └── genre/        → Genre enum
│
├── 🖥️  CLI Layer (cli/*)
│   ├── BookCommands       → Interactive book management
│   └── BookcaseCommands   → Interactive bookcase management
│
└── 🛠️  Utilities (util/*)
    ├── StartupRunner       → Initialization logic
    ├── LoadingBar          → UI effects
    └── BibbyPersonality    → ASCII art & personality
```

**What you're doing right:**
- ✅ Clear package structure by domain
- ✅ Separation of entities vs domain models
- ✅ Using Spring's dependency injection (mostly)
- ✅ Repository pattern for data access

**Current test coverage:** ~0% (only a smoke test exists)

---

## Part 2: Where Should We Start Testing?

Let me prioritize this for you like I would in a real code review:

### 🔴 **HIGH PRIORITY** → Start here!

#### 1. **BookService** (`library/book/BookService.java`)
**Why this first?**
- Real business logic (not just CRUD)
- Complex method: `createNewBook()` creates authors if they don't exist
- State management: book checkout/check-in
- Multiple dependencies to mock (great learning opportunity)
- Only 95 lines - manageable scope

**What makes it interesting to test:**
```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    // 👀 This method does TWO things:
    // 1. Find or create an author
    // 2. Find or create a book
    // 3. Link them together

    // Question: What if the author lookup fails?
    // Question: What if the book already exists?
    // Question: What about duplicate books by different authors?
}
```

#### 2. **BookcaseService** (`library/bookcase/BookcaseService.java`)
**Why test this?**
- Loop-based shelf generation (edge cases: 0 shelves? 100 shelves?)
- Exception handling (`ResponseStatusException` for duplicates)
- Multiple repository interactions in one method

#### 3. **BookMapper** (static utility)
**Why test this?**
- Pure functions = easiest to test (no mocks needed!)
- Null handling logic
- Good place to practice assertion writing

---

### 🟡 **MEDIUM PRIORITY** → After you're comfortable

- Repository integration tests (requires test database)
- Controller tests (MockMvc)
- CLI commands (complex, we'll refactor first)

---

### 🟢 **LOW PRIORITY** → Nice to have

- Domain model tests (mostly getters/setters)
- DTOs (records are simple)

---

## Part 3: Testing Fundamentals (No BS, Just Practical Stuff)

Let me explain the core concepts using YOUR code, not generic examples.

### What's the Difference Between Unit Tests vs Integration Tests?

**Unit Test:**
> Tests ONE class in isolation. Mock everything it depends on.

**Example from your code:**
```java
// Unit test for BookService.createNewBook()
// We mock: BookRepository, AuthorRepository
// We test: The logic in createNewBook() method
// We DON'T: Actually hit the database
```

**Integration Test:**
> Tests multiple components working together. Use real (or test) implementations.

**Example from your code:**
```java
// Integration test for BookService
// We use: Real BookRepository connected to test database
// We use: Real AuthorRepository connected to test database
// We test: That saving a book actually persists to DB
```

**When to use which?**
- **Unit tests:** Fast, isolated, test business logic. Write LOTS of these.
- **Integration tests:** Slower, test components work together. Write FEWER of these for critical flows.

**Junior mistake I see all the time:**
> "I'll just write integration tests because they test everything!"

❌ **Wrong.** Integration tests are slow, brittle, and hard to debug. You want a **test pyramid**:

```
    /\
   /  \  ← Few integration tests (slow)
  /____\
 /      \  ← Some integration tests
/________\
/          \ ← LOTS of unit tests (fast)
```

---

### The Anatomy of a JUnit 5 Test

Every test follows the same pattern. Let's use `BookService.createNewBook()` as our example:

```java
import org.junit.jupiter.api.Test;              // JUnit 5 (NOT org.junit.Test - that's JUnit 4!)
import org.junit.jupiter.api.BeforeEach;         // Setup before each test
import static org.junit.jupiter.api.Assertions.*; // Assertions
import static org.mockito.Mockito.*;             // Mockito helpers

class BookServiceTest {

    // ========== SETUP ==========
    @BeforeEach  // Runs before EACH test
    void setUp() {
        // Initialize fresh mocks/objects for each test
        // This keeps tests isolated!
    }

    // ========== TEST METHODS ==========
    @Test  // Marks this as a test method
    void shouldCreateNewBookWhenAuthorDoesNotExist() {
        // ===== GIVEN (Arrange) =====
        // Set up test data and mock behavior
        BookRequestDTO request = new BookRequestDTO("1984", "George", "Orwell");

        // ===== WHEN (Act) =====
        // Execute the method under test
        bookService.createNewBook(request);

        // ===== THEN (Assert) =====
        // Verify the outcome
        verify(authorRepository).save(any(AuthorEntity.class));
        verify(bookRepository).save(any(BookEntity.class));
    }
}
```

**Three key sections:**
1. **GIVEN** (Arrange): Set up test data
2. **WHEN** (Act): Call the method you're testing
3. **THEN** (Assert): Check the result

Some people use `// Arrange/Act/Assert`, others use `// Given/When/Then`. Pick one and be consistent.

---

### Mockito 101: What, When, and Why

#### What is Mockito?

Mockito creates **fake objects** that you control. You tell them:
- "When someone calls `findById(1)`, return this fake book"
- "Track every time someone calls `save()`"

#### When do you mock something?

**Mock:**
- External dependencies (databases, APIs, file systems)
- Collaborators you're not testing
- Things that are slow or unreliable

**DON'T mock:**
- The class you're testing (you're testing the REAL thing)
- Simple value objects (DTOs, domain models)
- Final classes or static methods (Mockito can't mock these easily)

#### Example from YOUR code:

```java
// In BookService.createNewBook(), you have TWO dependencies:
@Service
public class BookService {
    final BookRepository bookRepository;           // ← MOCK THIS
    private final AuthorRepository authorRepository; // ← MOCK THIS

    public void createNewBook(BookRequestDTO dto) {
        // Test this REAL logic with FAKE repositories
    }
}
```

**Why mock repositories?**
1. **Speed:** No database = tests run in milliseconds
2. **Isolation:** Test BookService logic, not Hibernate
3. **Control:** You decide what the repository returns
4. **No setup:** No need to seed test data

---

### Mocking Patterns in Mockito

#### Pattern 1: `@Mock` + `@InjectMocks`

```java
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)  // Enables Mockito
class BookServiceTest {

    @Mock  // Create a fake BookRepository
    private BookRepository bookRepository;

    @Mock  // Create a fake AuthorRepository
    private AuthorRepository authorRepository;

    @InjectMocks  // Create a REAL BookService, inject the mocks above
    private BookService bookService;

    // Now bookService uses the fake repositories!
}
```

**What's happening?**
- `@ExtendWith(MockitoExtension.class)`: Tells JUnit 5 to use Mockito
- `@Mock`: Creates a fake object
- `@InjectMocks`: Creates the real object and injects the mocks into it (via constructor or fields)

#### Pattern 2: Manual Setup (More Control)

```java
class BookServiceTest {
    private BookRepository bookRepository;
    private AuthorRepository authorRepository;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        authorRepository = mock(AuthorRepository.class);
        bookService = new BookService(bookRepository, authorRepository);
    }
}
```

**When to use manual setup?**
- When you need more control over construction
- When `@InjectMocks` doesn't work (complex constructors)

I prefer manual setup for clarity, but `@Mock/@InjectMocks` is more concise.

---

### Stubbing: Teaching Mocks How to Behave

When you call a method on a mock, it returns `null` by default. You need to **stub** the behavior:

```java
@Test
void example() {
    // STUB: When findByTitle("1984") is called, return this fake book
    BookEntity fakeBook = new BookEntity();
    fakeBook.setTitle("1984");
    when(bookRepository.findByTitle("1984")).thenReturn(fakeBook);

    // Now this will return fakeBook:
    BookEntity result = bookRepository.findByTitle("1984");

    // But this will return null (not stubbed):
    BookEntity otherResult = bookRepository.findByTitle("Animal Farm");
}
```

**Common stubbing methods:**
```java
when(mock.method()).thenReturn(value);           // Return a value
when(mock.method()).thenThrow(new Exception());  // Throw an exception
when(mock.method()).thenAnswer(invocation -> {   // Custom logic
    return something;
});
```

---

### Verification: Did the Mock Get Called?

Sometimes you don't care about the RETURN value, you care about WHAT GOT CALLED.

```java
@Test
void shouldSaveAuthorWhenCreatingNewBook() {
    // Given
    BookRequestDTO dto = new BookRequestDTO("1984", "George", "Orwell");
    when(authorRepository.findByFirstNameAndLastName("George", "Orwell"))
        .thenReturn(null); // Author doesn't exist

    // When
    bookService.createNewBook(dto);

    // Then - verify save() was called
    verify(authorRepository).save(any(AuthorEntity.class));

    // You can also verify with exact arguments:
    ArgumentCaptor<AuthorEntity> captor = ArgumentCaptor.forClass(AuthorEntity.class);
    verify(authorRepository).save(captor.capture());
    AuthorEntity savedAuthor = captor.getValue();
    assertEquals("George", savedAuthor.getFirstName());
    assertEquals("Orwell", savedAuthor.getLastName());
}
```

**Common verification methods:**
```java
verify(mock).method();                    // Called exactly once
verify(mock, times(2)).method();          // Called exactly 2 times
verify(mock, never()).method();           // Never called
verify(mock, atLeastOnce()).method();     // Called at least once
verifyNoInteractions(mock);               // No methods called on this mock
```

---

## Part 4: Deep Dive - Testing BookService

Let's pick `BookService` as our first class to test. I'll walk you through this like we're sitting together.

### Step 1: Understanding the Class

```java
@Service
public class BookService {
    final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    // ... methods ...
}
```

**What does this class do?**
- Manages book lifecycle (create, find, update, check out, check in)
- Coordinates between books and authors
- Delegates persistence to repositories

**Dependencies:**
- `BookRepository` - talks to database for books
- `AuthorRepository` - talks to database for authors

**Key methods to test:**

| Method | Complexity | Why Test? |
|--------|-----------|-----------|
| `createNewBook()` | 🔴 High | Complex logic: find-or-create author, then book |
| `checkOutBook()` | 🟡 Medium | State change + conditional logic |
| `checkInBook()` | 🟡 Medium | Calls other service methods |
| `findBookByTitle()` | 🟡 Medium | Multiple search strategies + null handling |
| `findBookById()` | 🟢 Low | Direct delegation to repository |

---

### Step 2: Identifying Test Cases

Let's focus on `createNewBook()`:

```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    String firstName = bookRequestDTO.firstName();
    String lastName = bookRequestDTO.lastName();
    String title = bookRequestDTO.title();

    BookEntity bookEntity = bookRepository.findByTitle(title);
    AuthorEntity authorEntity = authorRepository.findByFirstNameAndLastName(firstName, lastName);

    if (authorEntity == null) {
        authorEntity = new AuthorEntity(firstName, lastName);
        authorRepository.save(authorEntity);
    }

    if (bookEntity == null) {
        bookEntity = new BookEntity();
        bookEntity.setTitle(title);
    }
    bookEntity.setAuthors(authorEntity);  // ⚠️ BUG: Always overwrites authors!
    bookRepository.save(bookEntity);
}
```

**Test cases we need:**

1. ✅ **Happy path:** Book and author don't exist → create both
2. ✅ **Author exists:** Author exists → reuse existing author
3. ✅ **Book exists:** Book exists → update it (link new author)
4. ⚠️ **Both exist:** Both exist → just link them
5. ❌ **Null inputs:** What if DTO has null values?
6. ❌ **Edge case:** Empty strings for names?

**How I'm thinking about this:**
- Look at the `if` statements (each branch = test case)
- Think about boundary conditions (null, empty)
- Think about the database state (exists vs doesn't exist)

---

### Step 3: Writing the Test Class

Let's build this incrementally. I'll explain every line.

```java
package com.penrose.bibby.library.book;

// JUnit 5 imports (note the jupiter package!)
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

// Mockito imports
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

// Assertions
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorRepository;

/**
 * Unit tests for BookService.
 *
 * These are UNIT tests, not integration tests:
 * - We mock all external dependencies (repositories)
 * - We test BookService logic in isolation
 * - No database required
 */
@ExtendWith(MockitoExtension.class)  // Enables Mockito annotations
@DisplayName("BookService Tests")     // Prettier test reports
class BookServiceTest {

    // ========== TEST DOUBLES ==========

    @Mock  // Fake repository - we control its behavior
    private BookRepository bookRepository;

    @Mock  // Fake repository - we control its behavior
    private AuthorRepository authorRepository;

    @InjectMocks  // Real BookService with mocked dependencies injected
    private BookService bookService;

    // ========== TESTS FOR createNewBook() ==========

    /**
     * Grouping related tests with @Nested.
     * This makes test reports easier to read:
     *
     * BookService Tests
     *   └── createNewBook()
     *       ├── should create new author when author does not exist
     *       ├── should reuse existing author when author exists
     *       └── ...
     */
    @Nested
    @DisplayName("createNewBook()")
    class CreateNewBookTests {

        @Test
        @DisplayName("should create new author and book when neither exist")
        void shouldCreateNewAuthorAndBook_WhenNeitherExist() {
            // ========== GIVEN ==========
            // Prepare test data
            BookRequestDTO request = new BookRequestDTO(
                "1984",        // title
                "George",      // firstName
                "Orwell"       // lastName
            );

            // Stub repository behavior: nothing exists yet
            when(bookRepository.findByTitle("1984")).thenReturn(null);
            when(authorRepository.findByFirstNameAndLastName("George", "Orwell"))
                .thenReturn(null);

            // ========== WHEN ==========
            // Execute the method under test
            bookService.createNewBook(request);

            // ========== THEN ==========
            // Verify author was saved
            ArgumentCaptor<AuthorEntity> authorCaptor =
                ArgumentCaptor.forClass(AuthorEntity.class);
            verify(authorRepository).save(authorCaptor.capture());

            AuthorEntity savedAuthor = authorCaptor.getValue();
            assertEquals("George", savedAuthor.getFirstName(),
                "Author first name should match request");
            assertEquals("Orwell", savedAuthor.getLastName(),
                "Author last name should match request");

            // Verify book was saved
            ArgumentCaptor<BookEntity> bookCaptor =
                ArgumentCaptor.forClass(BookEntity.class);
            verify(bookRepository).save(bookCaptor.capture());

            BookEntity savedBook = bookCaptor.getValue();
            assertEquals("1984", savedBook.getTitle(),
                "Book title should match request");
            assertNotNull(savedBook.getAuthors(),
                "Book should be linked to author");
        }

        @Test
        @DisplayName("should reuse existing author when author exists")
        void shouldReuseExistingAuthor_WhenAuthorExists() {
            // ========== GIVEN ==========
            BookRequestDTO request = new BookRequestDTO("Animal Farm", "George", "Orwell");

            // Create a pre-existing author
            AuthorEntity existingAuthor = new AuthorEntity("George", "Orwell");
            existingAuthor.setAuthorId(123L);  // Simulate DB-assigned ID

            // Stub: book doesn't exist, but author does
            when(bookRepository.findByTitle("Animal Farm")).thenReturn(null);
            when(authorRepository.findByFirstNameAndLastName("George", "Orwell"))
                .thenReturn(existingAuthor);

            // ========== WHEN ==========
            bookService.createNewBook(request);

            // ========== THEN ==========
            // Verify author was NOT saved again (already exists)
            verify(authorRepository, never()).save(any(AuthorEntity.class));

            // Verify book was saved with existing author
            ArgumentCaptor<BookEntity> bookCaptor =
                ArgumentCaptor.forClass(BookEntity.class);
            verify(bookRepository).save(bookCaptor.capture());

            BookEntity savedBook = bookCaptor.getValue();
            assertEquals("Animal Farm", savedBook.getTitle());
            assertEquals(existingAuthor, savedBook.getAuthors(),
                "Book should be linked to existing author, not a new one");
        }

        @Test
        @DisplayName("should update existing book when book exists")
        void shouldUpdateExistingBook_WhenBookExists() {
            // ========== GIVEN ==========
            BookRequestDTO request = new BookRequestDTO("1984", "George", "Orwell");

            // Pre-existing book
            BookEntity existingBook = new BookEntity();
            existingBook.setTitle("1984");
            existingBook.setBookId(456L);

            // Pre-existing author
            AuthorEntity existingAuthor = new AuthorEntity("George", "Orwell");
            existingAuthor.setAuthorId(123L);

            // Stub: both exist
            when(bookRepository.findByTitle("1984")).thenReturn(existingBook);
            when(authorRepository.findByFirstNameAndLastName("George", "Orwell"))
                .thenReturn(existingAuthor);

            // ========== WHEN ==========
            bookService.createNewBook(request);

            // ========== THEN ==========
            // Neither should be created
            verify(authorRepository, never()).save(any(AuthorEntity.class));

            // But book should be saved (to update the link)
            verify(bookRepository).save(existingBook);

            // Verify the link was updated
            assertEquals(existingAuthor, existingBook.getAuthors(),
                "Existing book should be linked to author");
        }
    }

    // ========== TESTS FOR checkOutBook() ==========

    @Nested
    @DisplayName("checkOutBook()")
    class CheckOutBookTests {

        @Test
        @DisplayName("should update book status to CHECKED_OUT when available")
        void shouldUpdateStatusToCheckedOut_WhenBookIsAvailable() {
            // ========== GIVEN ==========
            BookEntity book = new BookEntity();
            book.setBookId(1L);
            book.setTitle("1984");
            book.setBookStatus("AVAILABLE");  // Initially available

            // ========== WHEN ==========
            bookService.checkOutBook(book);

            // ========== THEN ==========
            assertEquals("CHECKED_OUT", book.getBookStatus(),
                "Book status should be updated to CHECKED_OUT");
            verify(bookRepository).save(book);
        }

        @Test
        @DisplayName("should not update book when already checked out")
        void shouldNotUpdate_WhenBookAlreadyCheckedOut() {
            // ========== GIVEN ==========
            BookEntity book = new BookEntity();
            book.setBookId(1L);
            book.setTitle("1984");
            book.setBookStatus("CHECKED_OUT");  // Already checked out

            // ========== WHEN ==========
            bookService.checkOutBook(book);

            // ========== THEN ==========
            // Status should remain unchanged
            assertEquals("CHECKED_OUT", book.getBookStatus());

            // save() should NOT be called (no state change)
            verify(bookRepository, never()).save(any(BookEntity.class));
        }
    }

    // ========== TESTS FOR checkInBook() ==========

    @Nested
    @DisplayName("checkInBook()")
    class CheckInBookTests {

        @Test
        @DisplayName("should update book status to AVAILABLE when checking in")
        void shouldUpdateStatusToAvailable_WhenCheckingIn() {
            // ========== GIVEN ==========
            String bookTitle = "1984";

            BookEntity checkedOutBook = new BookEntity();
            checkedOutBook.setTitle(bookTitle);
            checkedOutBook.setBookStatus("CHECKED_OUT");

            // Stub: findBookByTitle returns the checked-out book
            when(bookRepository.findByTitleIgnoreCase(bookTitle))
                .thenReturn(checkedOutBook);
            when(bookRepository.findByTitleContaining(bookTitle))
                .thenReturn(java.util.List.of(checkedOutBook));

            // ========== WHEN ==========
            bookService.checkInBook(bookTitle);

            // ========== THEN ==========
            assertEquals("AVAILABLE", checkedOutBook.getBookStatus(),
                "Book should be marked as AVAILABLE");
            verify(bookRepository).save(checkedOutBook);
        }

        @Test
        @DisplayName("should throw NullPointerException when book not found")
        void shouldThrowNPE_WhenBookNotFound() {
            // ========== GIVEN ==========
            String nonExistentTitle = "Nonexistent Book";

            // Stub: book doesn't exist
            when(bookRepository.findByTitleIgnoreCase(nonExistentTitle))
                .thenReturn(null);
            when(bookRepository.findByTitleContaining(nonExistentTitle))
                .thenReturn(java.util.List.of());

            // ========== WHEN & THEN ==========
            // This is a BUG in the code! checkInBook() assumes findBookByTitle()
            // returns a non-null book, but it can return null.
            // Our test DOCUMENTS this bug.
            assertThrows(NullPointerException.class, () -> {
                bookService.checkInBook(nonExistentTitle);
            }, "Should throw NPE when book is not found");

            // No save should happen
            verify(bookRepository, never()).save(any(BookEntity.class));
        }
    }

    // ========== TESTS FOR findBookByTitle() ==========

    @Nested
    @DisplayName("findBookByTitle()")
    class FindBookByTitleTests {

        @Test
        @DisplayName("should return book when exact match found")
        void shouldReturnBook_WhenExactMatchFound() {
            // ========== GIVEN ==========
            String title = "1984";
            BookEntity expectedBook = new BookEntity();
            expectedBook.setTitle(title);

            when(bookRepository.findByTitleIgnoreCase(title))
                .thenReturn(expectedBook);
            when(bookRepository.findByTitleContaining(title))
                .thenReturn(java.util.List.of(expectedBook));

            // ========== WHEN ==========
            BookEntity result = bookService.findBookByTitle(title);

            // ========== THEN ==========
            assertNotNull(result, "Should return a book");
            assertEquals(title, result.getTitle());
        }

        @Test
        @DisplayName("should return null when book not found")
        void shouldReturnNull_WhenBookNotFound() {
            // ========== GIVEN ==========
            String title = "Nonexistent Book";

            when(bookRepository.findByTitleIgnoreCase(title))
                .thenReturn(null);
            when(bookRepository.findByTitleContaining(title))
                .thenReturn(java.util.List.of());

            // ========== WHEN ==========
            BookEntity result = bookService.findBookByTitle(title);

            // ========== THEN ==========
            assertNull(result, "Should return null when book not found");
        }
    }
}
```

---

### Step 4: Running the Tests

To run these tests:

```bash
# Run all tests
mvn test

# Run only BookServiceTest
mvn test -Dtest=BookServiceTest

# Run a specific test method
mvn test -Dtest=BookServiceTest#shouldCreateNewAuthorAndBook_WhenNeitherExist
```

---

### Step 5: Understanding What We Just Did

Let me break down the key concepts:

#### 1. **@ExtendWith(MockitoExtension.class)**
This tells JUnit 5 to initialize Mockito. Without this, `@Mock` and `@InjectMocks` won't work.

#### 2. **@Mock vs @InjectMocks**
- `@Mock`: Creates a fake object you can control
- `@InjectMocks`: Creates a REAL object and injects the mocks into it

```java
@Mock
private BookRepository bookRepository;  // Fake

@InjectMocks
private BookService bookService;  // REAL, but uses fake repository
```

#### 3. **ArgumentCaptor**
This captures arguments passed to mocked methods so you can assert on them:

```java
ArgumentCaptor<AuthorEntity> captor = ArgumentCaptor.forClass(AuthorEntity.class);
verify(authorRepository).save(captor.capture());
AuthorEntity savedAuthor = captor.getValue();
assertEquals("George", savedAuthor.getFirstName());
```

**Why use ArgumentCaptor?**
- You want to verify not just THAT `save()` was called, but WHAT was saved.
- You want to inspect the state of the object that was passed.

**Alternative (using argument matchers):**
```java
verify(authorRepository).save(argThat(author ->
    author.getFirstName().equals("George") &&
    author.getLastName().equals("Orwell")
));
```

#### 4. **@Nested Tests**
Groups related tests together for better organization:

```
BookServiceTest
├── CreateNewBookTests
│   ├── shouldCreateNewAuthorAndBook_WhenNeitherExist
│   ├── shouldReuseExistingAuthor_WhenAuthorExists
│   └── shouldUpdateExistingBook_WhenBookExists
├── CheckOutBookTests
│   ├── shouldUpdateStatusToCheckedOut_WhenBookIsAvailable
│   └── shouldNotUpdate_WhenBookAlreadyCheckedOut
└── CheckInBookTests
    ├── shouldUpdateStatusToAvailable_WhenCheckingIn
    └── shouldThrowNPE_WhenBookNotFound
```

#### 5. **Testing Exception Scenarios**
```java
assertThrows(NullPointerException.class, () -> {
    bookService.checkInBook("Nonexistent Book");
});
```

This test DOCUMENTS a bug: `checkInBook()` doesn't handle null books gracefully.

---

## Part 5: Common Testing Mistakes (And How to Avoid Them)

### ❌ Mistake 1: Over-Mocking

**Bad:**
```java
@Test
void badTest() {
    // Mocking simple value objects
    BookRequestDTO dto = mock(BookRequestDTO.class);
    when(dto.title()).thenReturn("1984");
    when(dto.firstName()).thenReturn("George");
    when(dto.lastName()).thenReturn("Orwell");

    // Why are we mocking a simple DTO?!
}
```

**Good:**
```java
@Test
void goodTest() {
    // Just create the real object!
    BookRequestDTO dto = new BookRequestDTO("1984", "George", "Orwell");
}
```

**Rule of thumb:** Only mock dependencies that are:
- External (DB, API, file system)
- Complex
- Slow

---

### ❌ Mistake 2: Testing Implementation Details

**Bad:**
```java
@Test
void badTest() {
    bookService.createNewBook(dto);

    // Testing HOW it's implemented (internal details)
    verify(authorRepository).findByFirstNameAndLastName(any(), any());
    verify(bookRepository).findByTitle(any());
    // What if we refactor to use a different lookup method? Test breaks!
}
```

**Good:**
```java
@Test
void goodTest() {
    bookService.createNewBook(dto);

    // Testing WHAT happens (behavior)
    verify(authorRepository).save(any(AuthorEntity.class));
    verify(bookRepository).save(any(BookEntity.class));
    // As long as the author and book are saved, we don't care HOW
}
```

**Rule of thumb:** Test behavior (outcomes), not implementation (how you got there).

---

### ❌ Mistake 3: Giant Setup Methods

**Bad:**
```java
@BeforeEach
void setUp() {
    // 50 lines of setup
    // Configuring every possible mock behavior
    // Half of it isn't used in most tests
}
```

**Good:**
```java
@BeforeEach
void setUp() {
    // Minimal setup - just create objects
    bookService = new BookService(bookRepository, authorRepository);
}

@Test
void specificTest() {
    // Test-specific setup here
    when(bookRepository.findByTitle("1984")).thenReturn(book);
}
```

**Rule of thumb:** Only put in `@BeforeEach` what's common to ALL tests.

---

### ❌ Mistake 4: One Assertion Per Test (Too Strict)

Some purists say "one assertion per test." I disagree.

**Overly strict:**
```java
@Test
void testAuthorFirstName() {
    assertEquals("George", author.getFirstName());
}

@Test
void testAuthorLastName() {
    assertEquals("Orwell", author.getLastName());
}
// Now we have 20 tiny tests for one operation
```

**Pragmatic:**
```java
@Test
void shouldCreateAuthorWithCorrectNames() {
    assertEquals("George", author.getFirstName());
    assertEquals("Orwell", author.getLastName());
}
// Related assertions for ONE behavior
```

**Rule of thumb:** One LOGICAL assertion per test. It's okay to have multiple `assertEquals()` if they're verifying the same outcome.

---

### ❌ Mistake 5: Not Testing Edge Cases

**Incomplete:**
```java
@Test
void shouldCreateBook() {
    // Only tests happy path
}
```

**Complete:**
```java
@Test void shouldCreateBook_WhenValid() { }
@Test void shouldHandleNullTitle() { }
@Test void shouldHandleEmptyAuthorName() { }
@Test void shouldHandleDuplicateBook() { }
```

**Rule of thumb:** For each method, test:
- Happy path (1-2 tests)
- Boundary conditions (empty, null, zero, max)
- Error cases (exceptions, invalid input)

---

## Part 6: Refactoring for Testability

Let me show you how to refactor code that's hard to test.

### Problem: `checkInBook()` Assumes Non-Null

```java
// BEFORE: Hard to test, hides a bug
public void checkInBook(String bookTitle) {
    BookEntity bookEntity = findBookByTitle(bookTitle);
    bookEntity.setBookStatus(BookStatus.AVAILABLE.toString());  // NPE if null!
    updateBook(bookEntity);
}
```

**Why is this hard to test?**
- Throws `NullPointerException` instead of a meaningful exception
- Caller doesn't know what went wrong

**How to refactor:**

```java
// AFTER: Explicit error handling
public void checkInBook(String bookTitle) {
    BookEntity bookEntity = findBookByTitle(bookTitle);

    if (bookEntity == null) {
        throw new BookNotFoundException("Book not found: " + bookTitle);
    }

    bookEntity.setBookStatus(BookStatus.AVAILABLE.toString());
    updateBook(bookEntity);
}
```

**Now the test is clearer:**
```java
@Test
void shouldThrowBookNotFoundException_WhenBookNotFound() {
    when(bookRepository.findByTitleIgnoreCase("Missing")).thenReturn(null);

    assertThrows(BookNotFoundException.class, () -> {
        bookService.checkInBook("Missing");
    });
}
```

---

### Problem: Static Methods (BookMapper)

Your `BookMapper` uses static methods:

```java
public class BookMapper {
    public static Book toDomain(BookEntity entity) {
        // ...
    }
}
```

**Why is this hard to test?**
- You can't mock static methods easily with Mockito
- But... for PURE FUNCTIONS like mappers, this is actually OKAY!

**Pure functions don't need mocking:**
```java
@Test
void shouldMapBookEntityToDomain() {
    // No mocks needed! Just test the function
    BookEntity entity = new BookEntity();
    entity.setTitle("1984");
    entity.setBookStatus("AVAILABLE");

    Book domain = BookMapper.toDomain(entity);

    assertEquals("1984", domain.getTitle());
    assertEquals(BookStatus.AVAILABLE, domain.getStatus());
}
```

**When static methods ARE a problem:**
```java
// BAD: Static method with side effects
public static void sendEmail(String to, String body) {
    EmailService.send(to, body);  // Can't mock this!
}
```

**Rule of thumb:**
- Static methods for PURE FUNCTIONS (mappers, utils): OK
- Static methods with SIDE EFFECTS (I/O, state changes): BAD

---

### Problem: Too Many Responsibilities

Your `BookCommands` class is 578 lines. That's a code smell.

**Principle:** Single Responsibility Principle (SRP)
> A class should have one reason to change.

`BookCommands` does:
1. CLI interaction (collecting user input)
2. Formatting output
3. Business logic delegation
4. UI effects (loading bars, sleep)

**How to refactor:**
Extract each responsibility into its own class:

```java
// BEFORE: God class
public class BookCommands {
    public void addBook() {
        // 50 lines mixing UI, logic, formatting
    }
}

// AFTER: Separated concerns
public class BookCommandHandler {  // Thin orchestrator
    private final BookService bookService;
    private final BookOutputFormatter formatter;
    private final UserInputCollector inputCollector;

    public void addBook() {
        BookRequestDTO dto = inputCollector.collectBookInput();
        bookService.createNewBook(dto);
        formatter.showSuccess("Book added!");
    }
}

// Now you can unit test BookCommandHandler without CLI dependencies!
```

---

## Part 7: Next Steps - Expanding Your Test Suite

Now that you understand the fundamentals, let's map out what to test next.

### Priority 1: Finish BookService Tests

Add tests for:
- `findBookById()`
- `updateBook()`
- `findBooksByShelf()`

### Priority 2: Test BookcaseService

**Key test cases:**

```java
@Nested
class CreateNewBookCaseTests {
    @Test
    void shouldCreateBookcaseWithShelves() {
        // Test that creating a bookcase with capacity 5 generates 5 shelves
    }

    @Test
    void shouldThrowConflictException_WhenDuplicateLabel() {
        // Test duplicate detection
    }

    @Test
    void shouldHandleZeroCapacity() {
        // Edge case: 0 shelves
    }
}
```

**What to mock:**
- `BookcaseRepository`
- `ShelfRepository`

**What NOT to mock:**
- `BookcaseEntity` (just create real instances)
- `ShelfEntity` (just create real instances)

### Priority 3: Test Mappers (Easy Wins!)

**BookMapper tests:**

```java
class BookMapperTest {
    // No mocks needed! Pure functions!

    @Test
    void shouldMapEntityToDomain_WithAllFields() {
        // Create entity with all fields populated
        // Map to domain
        // Assert all fields transferred correctly
    }

    @Test
    void shouldHandleNullAuthor() {
        // Test null handling
    }

    @Test
    void shouldMapStatusStringToEnum() {
        // Test enum conversion
    }
}
```

### Priority 4: Integration Tests for Repositories

Use `@DataJpaTest` for repository tests:

```java
@DataJpaTest  // Spins up in-memory database
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldFindBookByTitle() {
        // Save a book
        BookEntity book = new BookEntity();
        book.setTitle("1984");
        bookRepository.save(book);

        // Find it
        BookEntity found = bookRepository.findByTitle("1984");

        // Assert
        assertNotNull(found);
        assertEquals("1984", found.getTitle());
    }
}
```

---

## Part 8: Test Naming Conventions

Good test names are documentation. Compare:

**❌ Bad:**
```java
@Test
void test1() { }

@Test
void testBook() { }

@Test
void testCreateNewBook() { }  // What about it?
```

**✅ Good:**
```java
@Test
void shouldCreateNewBook_WhenValidInput() { }

@Test
void shouldThrowException_WhenTitleIsNull() { }

@Test
void shouldReuseExistingAuthor_WhenAuthorExists() { }
```

**Naming template:**
```
should<ExpectedBehavior>_When<Condition>
```

Or:
```
<MethodName>_<Condition>_<ExpectedBehavior>
```

Pick one style and be consistent.

---

## Part 9: Mockito Cheat Sheet

```java
// ========== CREATING MOCKS ==========
@Mock private BookRepository repo;           // Annotation style
BookRepository repo = mock(BookRepository.class);  // Manual style

// ========== STUBBING ==========
when(repo.findById(1L)).thenReturn(Optional.of(book));  // Return value
when(repo.findById(1L)).thenThrow(new RuntimeException());  // Throw exception
when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));  // Custom logic

// ========== VERIFICATION ==========
verify(repo).save(book);                     // Called once
verify(repo, times(2)).save(any());          // Called twice
verify(repo, never()).delete(any());         // Never called
verify(repo, atLeastOnce()).findAll();       // At least once

// ========== ARGUMENT MATCHERS ==========
any()                           // Any object
any(BookEntity.class)           // Any BookEntity
eq("1984")                      // Exact match
argThat(book -> book.getTitle().equals("1984"))  // Custom matcher

// ========== ARGUMENT CAPTORS ==========
ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
verify(repo).save(captor.capture());
BookEntity captured = captor.getValue();
assertEquals("1984", captured.getTitle());

// ========== VOID METHODS ==========
doNothing().when(repo).deleteById(1L);       // Do nothing
doThrow(new RuntimeException()).when(repo).deleteById(1L);  // Throw exception
```

---

## Part 10: Resources for Further Learning

### Recommended Reading:
1. **Effective Unit Testing** by Lasse Koskela
2. **Growing Object-Oriented Software, Guided by Tests** by Freeman & Pryce
3. **Unit Testing Principles, Practices, and Patterns** by Vladimir Khorikov

### Online Resources:
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Baeldung JUnit 5](https://www.baeldung.com/junit-5)

---

## Part 11: Your Action Plan

Here's what I recommend you do next:

### Week 1: Foundations
- [ ] Read through this guide completely
- [ ] Set up your test environment (dependencies already in `pom.xml`)
- [ ] Write tests for `BookService.createNewBook()`
- [ ] Write tests for `BookService.checkOutBook()`
- [ ] Run the tests and see them pass

### Week 2: Expand
- [ ] Write tests for `BookcaseService.createNewBookCase()`
- [ ] Write tests for `BookMapper` (all methods)
- [ ] Practice using ArgumentCaptor
- [ ] Practice using @Nested tests

### Week 3: Advanced
- [ ] Write repository integration tests using `@DataJpaTest`
- [ ] Refactor `checkInBook()` to handle nulls gracefully
- [ ] Write tests for the refactored version
- [ ] Aim for 80%+ coverage on service layer

### Week 4: Master
- [ ] Extract UI logic from `BookCommands`
- [ ] Write tests for the extracted components
- [ ] Add test for edge cases (empty strings, very long inputs)
- [ ] Review and refactor your tests for clarity

---

## Part 12: Final Thoughts

Testing is a skill. You'll get better with practice. Here's what I want you to remember:

1. **Tests are documentation.** Write them so future-you understands what the code does.
2. **Test behavior, not implementation.** Focus on WHAT, not HOW.
3. **Fast tests = happy developers.** Keep tests fast by mocking external dependencies.
4. **Good tests catch bugs.** Bad tests just slow you down.
5. **Refactor code to make it testable.** If it's hard to test, it's probably hard to maintain.

You've got this! Start small (BookService), build confidence, then expand. Feel free to ask questions as you go.

---

**Next:** Let me create the actual test file for `BookService` that you can run right now!
