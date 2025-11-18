# Section 18: Mockito & Mocking — Isolating Your Tests

**Estimated Reading Time:** 55 minutes
**Prerequisites:** Section 17 (JUnit & Testing Strategy)
**Applies To:** Your Bibby Library Management CLI Application

---

## Table of Contents

1. [What You'll Learn](#what-youll-learn)
2. [What Is Mocking?](#what-is-mocking)
3. [Why We Need Mocks](#why-we-need-mocks)
4. [Mockito Basics](#mockito-basics)
5. [Stubbing Behavior](#stubbing-behavior)
6. [Verifying Interactions](#verifying-interactions)
7. [Argument Matchers](#argument-matchers)
8. [Advanced Patterns](#advanced-patterns)
9. [Testing BookService (Complete Example)](#testing-bookservice-complete-example)
10. [When NOT to Mock](#when-not-to-mock)
11. [Common Pitfalls](#common-pitfalls)
12. [Action Items](#action-items)
13. [Key Takeaways](#key-takeaways)

---

## What You'll Learn

By the end of this section, you'll understand:

- **What mocking is** — Creating fake objects that simulate real dependencies
- **Why we mock** — Isolate unit tests, control behavior, verify interactions
- **Mockito basics** — `mock()`, `when()`, `thenReturn()`, `verify()`
- **Stubbing** — Define what mocks return when called
- **Verification** — Assert mocks were called correctly
- **Argument matchers** — `any()`, `eq()`, `argThat()` for flexible matching
- **When to mock** — External systems, slow operations, complex setup
- **When NOT to mock** — Value objects, simple data structures

**Most importantly**, you'll write real tests for BookService using Mockito.

---

## What Is Mocking?

**Mocking** = Creating fake objects that simulate real dependencies.

### The Problem

You want to test `BookService.createNewBook()`:

```java
public void createNewBook(BookRequestDTO dto) {
    AuthorEntity author = authorRepository.findByFirstNameAndLastName(dto.firstName(), dto.lastName());
    if (author == null) {
        author = new AuthorEntity(dto.firstName(), dto.lastName());
        authorRepository.save(author);
    }

    BookEntity book = bookRepository.findByTitle(dto.title());
    if (book == null) {
        book = new BookEntity();
        book.setTitle(dto.title());
    }
    book.setAuthors(author);
    bookRepository.save(book);
}
```

**To test this, you need:**
- `authorRepository` that can find/save authors
- `bookRepository` that can find/save books

**Without mocking:**
- Start a real database
- Insert test data
- Call method
- Query database to verify
- Clean up data

**Slow, brittle, complex!**

---

**With mocking:**
- Create fake `authorRepository` (mock)
- Define what it returns: "When `findByFirstNameAndLastName()` called, return null"
- Call method
- Verify mock was called: "Was `save()` called?"

**Fast, isolated, simple!**

---

### Mock vs Real Object

```java
// Real repository (needs database)
BookRepository realRepo = new JpaBookRepository();
realRepo.findByTitle("1984");  // Queries database

// Mock repository (no database)
BookRepository mockRepo = mock(BookRepository.class);
when(mockRepo.findByTitle("1984")).thenReturn(someBook);
mockRepo.findByTitle("1984");  // Returns someBook (no database)
```

**Mock objects:**
- ✅ No real implementation
- ✅ You control their behavior
- ✅ You can verify they were called
- ✅ Fast (no I/O)
- ✅ Isolated (no side effects)

---

## Why We Need Mocks

### 1. Isolate Unit Tests

**Unit test** = Test ONE unit in isolation.

```java
// ❌ Not isolated (depends on real database)
@Test
void createNewBook() {
    BookService service = new BookService(realBookRepo, realAuthorRepo);
    service.createNewBook(dto);
    // ← If this fails, is it BookService or the database?
}

// ✅ Isolated (mocked dependencies)
@Test
void createNewBook() {
    BookRepository mockBookRepo = mock(BookRepository.class);
    AuthorRepository mockAuthorRepo = mock(AuthorRepository.class);
    BookService service = new BookService(mockBookRepo, mockAuthorRepo);

    service.createNewBook(dto);
    // ← If this fails, it's definitely BookService logic
}
```

**Mocks ensure you're testing YOUR code, not Spring Data or PostgreSQL.**

---

### 2. Control Behavior

**You decide what mocks return:**

```java
// Test happy path: Author exists
when(mockAuthorRepo.findByFirstNameAndLastName("George", "Orwell"))
    .thenReturn(existingAuthor);

// Test edge case: Author doesn't exist
when(mockAuthorRepo.findByFirstNameAndLastName("George", "Orwell"))
    .thenReturn(null);

// Test error case: Database throws exception
when(mockAuthorRepo.findByFirstNameAndLastName("George", "Orwell"))
    .thenThrow(new DataAccessException("Database down"));
```

**You can test scenarios that are hard to reproduce with real objects.**

---

### 3. Verify Interactions

**Ensure methods were called correctly:**

```java
@Test
void createNewBook_WithNewAuthor_SavesAuthor() {
    // Arrange
    when(mockAuthorRepo.findByFirstNameAndLastName("George", "Orwell"))
        .thenReturn(null);  // Author doesn't exist

    // Act
    service.createNewBook(new BookRequestDTO("1984", "George", "Orwell"));

    // Assert: Verify author was saved
    verify(mockAuthorRepo).save(argThat(author ->
        author.getFirstName().equals("George") &&
        author.getLastName().equals("Orwell")
    ));
}
```

**You can verify behavior without checking database state.**

---

### 4. Fast Tests

```
Real database test:  500ms
Mocked test:         5ms
```

**100x faster!** This matters when you have hundreds of tests.

---

## Mockito Basics

### Creating Mocks

**Three ways to create mocks:**

**1. Mockito.mock() (Manual)**
```java
BookRepository mockRepo = Mockito.mock(BookRepository.class);
// or with static import:
import static org.mockito.Mockito.*;
BookRepository mockRepo = mock(BookRepository.class);
```

**2. @Mock Annotation (Recommended)**
```java
@ExtendWith(MockitoExtension.class)  // JUnit 5
class BookServiceTest {

    @Mock
    private BookRepository mockBookRepo;

    @Mock
    private AuthorRepository mockAuthorRepo;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(mockBookRepo, mockAuthorRepo);
    }
}
```

**3. @InjectMocks (Auto-injection)**
```java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository mockBookRepo;

    @Mock
    private AuthorRepository mockAuthorRepo;

    @InjectMocks  // Automatically injects mocks into BookService
    private BookService bookService;

    // No need for @BeforeEach setUp!
}
```

**Use @InjectMocks for cleaner tests.**

---

### Default Mock Behavior

**Unstubbed mocks return sensible defaults:**

```java
BookRepository mockRepo = mock(BookRepository.class);

mockRepo.findByTitle("1984");  // Returns null (not stubbed)
mockRepo.count();              // Returns 0
mockRepo.existsById(1L);       // Returns false
```

**Default returns:**
- Objects: `null`
- Primitives: `0`, `false`
- Collections: Empty collection

---

## Stubbing Behavior

**Stubbing** = Defining what a mock returns when called.

### Basic Stubbing

```java
// When findByTitle() is called with "1984", return this book
BookEntity book = new BookEntity();
book.setTitle("1984");

when(mockRepo.findByTitle("1984")).thenReturn(book);

// Now calling it returns the stubbed value
BookEntity result = mockRepo.findByTitle("1984");  // Returns book
BookEntity result2 = mockRepo.findByTitle("Animal Farm");  // Returns null (not stubbed)
```

---

### Stubbing with Arguments

```java
// Exact arguments
when(mockRepo.findByTitle("1984")).thenReturn(book1);
when(mockRepo.findByTitle("Animal Farm")).thenReturn(book2);

mockRepo.findByTitle("1984");  // Returns book1
mockRepo.findByTitle("Animal Farm");  // Returns book2
```

---

### Stubbing to Throw Exceptions

```java
// Throw exception when called
when(mockRepo.findByTitle("Error"))
    .thenThrow(new RuntimeException("Database error"));

// Or throw checked exception
when(mockRepo.findByTitle("Error"))
    .thenThrow(DataAccessException.class);

// Test it
assertThatThrownBy(() -> mockRepo.findByTitle("Error"))
    .isInstanceOf(RuntimeException.class);
```

---

### Stubbing Void Methods

```java
// For void methods, use doThrow()
doThrow(new RuntimeException("Save failed"))
    .when(mockRepo).save(any());

// Test it
assertThatThrownBy(() -> mockRepo.save(book))
    .isInstanceOf(RuntimeException.class);
```

---

### Stubbing Consecutive Calls

```java
// First call returns book1, second call returns book2
when(mockRepo.findById(1L))
    .thenReturn(Optional.of(book1))
    .thenReturn(Optional.of(book2));

mockRepo.findById(1L);  // Returns Optional.of(book1)
mockRepo.findById(1L);  // Returns Optional.of(book2)

// Or use multiple thenReturn()
when(mockRepo.findById(1L))
    .thenReturn(Optional.of(book1), Optional.of(book2));
```

---

### Stubbing with Callbacks

```java
// Execute custom logic when called
when(mockRepo.save(any())).thenAnswer(invocation -> {
    BookEntity book = invocation.getArgument(0);
    book.setBookId(999L);  // Set ID (like database would)
    return book;
});

BookEntity saved = mockRepo.save(book);
assertThat(saved.getBookId()).isEqualTo(999L);
```

---

## Verifying Interactions

**Verification** = Assert that methods were called (or not called).

### Basic Verification

```java
// Verify method was called
verify(mockRepo).save(book);

// Verify method was called with specific arguments
verify(mockRepo).findByTitle("1984");

// Verify method was never called
verify(mockRepo, never()).delete(any());

// Verify method was called exactly N times
verify(mockRepo, times(2)).save(any());

// Verify method was called at least/most N times
verify(mockRepo, atLeast(1)).save(any());
verify(mockRepo, atMost(3)).save(any());
```

---

### Verification Order

```java
// Verify calls happened in specific order
InOrder inOrder = inOrder(mockAuthorRepo, mockBookRepo);

inOrder.verify(mockAuthorRepo).save(author);
inOrder.verify(mockBookRepo).save(book);
```

---

### Verifying No Interactions

```java
// Verify mock was never used
verifyNoInteractions(mockRepo);

// Verify no more interactions after specific verifications
verify(mockRepo).findByTitle("1984");
verifyNoMoreInteractions(mockRepo);  // Fails if any other methods were called
```

---

## Argument Matchers

**Matchers** = Flexible argument matching in stubbing and verification.

### Common Matchers

```java
import static org.mockito.ArgumentMatchers.*;

// any() - matches any value
when(mockRepo.save(any())).thenReturn(book);
verify(mockRepo).save(any());

// anyString(), anyLong(), etc.
when(mockRepo.findByTitle(anyString())).thenReturn(book);
verify(mockAuthorRepo).findByFirstNameAndLastName(anyString(), anyString());

// eq() - matches exact value (mix with matchers)
verify(mockRepo).findByTitleAndAuthor(eq("1984"), any());

// isNull(), isNotNull()
when(mockRepo.findByTitle(isNull())).thenReturn(null);
```

---

### Custom Matchers with argThat()

```java
// Verify argument matches custom condition
verify(mockAuthorRepo).save(argThat(author ->
    author.getFirstName().equals("George") &&
    author.getLastName().equals("Orwell")
));

// Or create named matcher
ArgumentMatcher<AuthorEntity> isOrwell = author ->
    author.getFirstName().equals("George") &&
    author.getLastName().equals("Orwell");

verify(mockAuthorRepo).save(argThat(isOrwell));
```

---

### Capturing Arguments

```java
// Capture argument passed to mock
ArgumentCaptor<BookEntity> bookCaptor = ArgumentCaptor.forClass(BookEntity.class);

service.createNewBook(dto);

verify(mockRepo).save(bookCaptor.capture());
BookEntity capturedBook = bookCaptor.getValue();

assertThat(capturedBook.getTitle()).isEqualTo("1984");
assertThat(capturedBook.getAuthors()).contains(author);
```

---

### Matcher Rules

**Rule:** If you use ANY matcher, ALL arguments must use matchers.

```java
// ❌ WRONG: Mixing matcher and literal
verify(mockRepo).findByTitleAndAuthor("1984", any());

// ✅ CORRECT: All matchers
verify(mockRepo).findByTitleAndAuthor(eq("1984"), any());

// ✅ CORRECT: All literals
verify(mockRepo).findByTitleAndAuthor("1984", author);
```

---

## Advanced Patterns

### Spies (Partial Mocking)

**Spy** = Real object that you can selectively stub.

```java
// Create spy of real object
BookService realService = new BookService(mockRepo, mockAuthorRepo);
BookService spyService = spy(realService);

// Most methods call real implementation
spyService.findBookByTitle("1984");  // Calls real method

// But you can stub specific methods
when(spyService.checkOutBook(any())).thenReturn(/* custom behavior */);

// Use with caution! Usually prefer full mocks.
```

---

### @Captor for Argument Captors

```java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository mockRepo;

    @Captor
    private ArgumentCaptor<BookEntity> bookCaptor;

    @Test
    void test() {
        // Use captor
        verify(mockRepo).save(bookCaptor.capture());
        BookEntity captured = bookCaptor.getValue();
    }
}
```

---

### Mocking Static Methods (Mockito 3.4+)

```java
// Mock static method (use sparingly!)
try (MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class)) {
    LocalDate fixedDate = LocalDate.of(2024, 1, 1);
    mockedDate.when(LocalDate::now).thenReturn(fixedDate);

    // Test code that uses LocalDate.now()
}
```

**Note:** Avoid if possible. Indicates design issue.

---

## Testing BookService (Complete Example)

Let's write real tests for your BookService using all Mockito patterns.

### Test Class Setup

```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Tests")
class BookServiceTest {

    @Mock
    private BookRepository mockBookRepo;

    @Mock
    private AuthorRepository mockAuthorRepo;

    @InjectMocks
    private BookService bookService;

    // Tests go here...
}
```

---

### Test 1: createNewBook() with New Author

```java
@Test
@DisplayName("createNewBook() with new author and book should create both")
void createNewBook_WithNewAuthorAndBook_CreatesBoth() {
    // Arrange
    BookRequestDTO dto = new BookRequestDTO("1984", "George", "Orwell");

    when(mockAuthorRepo.findByFirstNameAndLastName("George", "Orwell"))
        .thenReturn(null);  // Author doesn't exist
    when(mockBookRepo.findByTitle("1984"))
        .thenReturn(null);  // Book doesn't exist

    // Act
    bookService.createNewBook(dto);

    // Assert: Verify author was saved
    verify(mockAuthorRepo).save(argThat(author ->
        author.getFirstName().equals("George") &&
        author.getLastName().equals("Orwell")
    ));

    // Assert: Verify book was saved
    verify(mockBookRepo).save(argThat(book ->
        book.getTitle().equals("1984")
    ));
}
```

---

### Test 2: createNewBook() with Existing Author

```java
@Test
@DisplayName("createNewBook() with existing author should reuse author and not save again")
void createNewBook_WithExistingAuthor_ReusesAuthor() {
    // Arrange
    BookRequestDTO dto = new BookRequestDTO("Animal Farm", "George", "Orwell");

    AuthorEntity existingAuthor = new AuthorEntity("George", "Orwell");
    existingAuthor.setAuthorId(1L);

    when(mockAuthorRepo.findByFirstNameAndLastName("George", "Orwell"))
        .thenReturn(existingAuthor);  // Author EXISTS
    when(mockBookRepo.findByTitle("Animal Farm"))
        .thenReturn(null);  // Book doesn't exist

    // Act
    bookService.createNewBook(dto);

    // Assert: Author should NOT be saved (already exists)
    verify(mockAuthorRepo, never()).save(any());

    // Assert: Book should be saved
    verify(mockBookRepo).save(any(BookEntity.class));
}
```

---

### Test 3: checkOutBook() When Available

```java
@Test
@DisplayName("checkOutBook() when book is available should change status to CHECKED_OUT")
void checkOutBook_WhenAvailable_ChangesStatusToCheckedOut() {
    // Arrange
    BookEntity book = new BookEntity();
    book.setBookId(1L);
    book.setTitle("1984");
    book.setBookStatus("AVAILABLE");

    // Act
    bookService.checkOutBook(book);

    // Assert: Status changed
    assertThat(book.getBookStatus()).isEqualTo("CHECKED_OUT");

    // Assert: Book was saved
    verify(mockBookRepo).save(book);
}
```

---

### Test 4: checkOutBook() When Already Checked Out

```java
@Test
@DisplayName("checkOutBook() when already checked out should not change status")
void checkOutBook_WhenAlreadyCheckedOut_DoesNothing() {
    // Arrange
    BookEntity book = new BookEntity();
    book.setBookId(1L);
    book.setTitle("1984");
    book.setBookStatus("CHECKED_OUT");

    // Act
    bookService.checkOutBook(book);

    // Assert: Status unchanged
    assertThat(book.getBookStatus()).isEqualTo("CHECKED_OUT");

    // Assert: Book was NOT saved
    verify(mockBookRepo, never()).save(any());
}
```

---

### Test 5: findBookByTitle() When Exists

```java
@Test
@DisplayName("findBookByTitle() when book exists should return the book")
void findBookByTitle_WhenExists_ReturnsBook() {
    // Arrange
    BookEntity expectedBook = new BookEntity();
    expectedBook.setBookId(1L);
    expectedBook.setTitle("1984");

    when(mockBookRepo.findByTitleIgnoreCase("1984"))
        .thenReturn(expectedBook);

    // Act
    BookEntity result = bookService.findBookByTitle("1984");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("1984");
    assertThat(result.getBookId()).isEqualTo(1L);

    // Verify repository was called
    verify(mockBookRepo).findByTitleIgnoreCase("1984");
}
```

**Note:** This assumes findBookByTitle() is refactored to return BookEntity (not null).

---

### Test 6: Using ArgumentCaptor

```java
@Test
@DisplayName("createNewBook() should save book with correct author relationship")
void createNewBook_SavesBookWithAuthor() {
    // Arrange
    BookRequestDTO dto = new BookRequestDTO("1984", "George", "Orwell");

    AuthorEntity author = new AuthorEntity("George", "Orwell");
    when(mockAuthorRepo.findByFirstNameAndLastName("George", "Orwell"))
        .thenReturn(author);
    when(mockBookRepo.findByTitle("1984"))
        .thenReturn(null);

    ArgumentCaptor<BookEntity> bookCaptor = ArgumentCaptor.forClass(BookEntity.class);

    // Act
    bookService.createNewBook(dto);

    // Assert: Capture and verify book
    verify(mockBookRepo).save(bookCaptor.capture());
    BookEntity savedBook = bookCaptor.getValue();

    assertThat(savedBook.getTitle()).isEqualTo("1984");
    assertThat(savedBook.getAuthors()).contains(author);
}
```

---

## When NOT to Mock

### 1. Don't Mock Value Objects

```java
// ❌ DON'T mock simple data objects
BookRequestDTO mockDto = mock(BookRequestDTO.class);

// ✅ Just create real ones
BookRequestDTO dto = new BookRequestDTO("1984", "George", "Orwell");
```

**Why:** No behavior to mock, just data.

---

### 2. Don't Mock What You're Testing

```java
// ❌ WRONG: Mocking the class under test
BookService mockService = mock(BookService.class);
when(mockService.findBookByTitle("1984")).thenReturn(book);

// You're not testing anything! You're testing the mock!

// ✅ CORRECT: Test the real service, mock its dependencies
BookService service = new BookService(mockBookRepo, mockAuthorRepo);
```

---

### 3. Don't Over-Mock

```java
// ❌ Over-mocking (testing implementation details)
@Test
void findBookByTitle_CallsRepositoryCorrectly() {
    service.findBookByTitle("1984");

    verify(mockRepo).findByTitleIgnoreCase("1984");
    // ← This just tests that service calls repository. So what?
}

// ✅ Test behavior, not implementation
@Test
void findBookByTitle_WhenExists_ReturnsBook() {
    when(mockRepo.findByTitleIgnoreCase("1984")).thenReturn(book);

    BookEntity result = service.findBookByTitle("1984");

    assertThat(result.getTitle()).isEqualTo("1984");
    // ← Tests actual behavior: Does service return the book?
}
```

---

### 4. Don't Mock Simple Collaborators

```java
// ❌ Overkill: Mocking a simple utility
DateFormatter mockFormatter = mock(DateFormatter.class);

// ✅ Just use the real one (if simple)
DateFormatter formatter = new DateFormatter();
```

**When to mock:**
- External systems (database, HTTP, file system)
- Slow operations
- Complex setup

**When NOT to mock:**
- Value objects (DTOs, records)
- Simple utilities
- What you're testing

---

## Common Pitfalls

### 1. Forgetting to Stub

```java
// ❌ Mock returns null (not stubbed!)
BookRepository mockRepo = mock(BookRepository.class);
BookEntity book = mockRepo.findByTitle("1984");  // Returns null!

assertThat(book).isNotNull();  // FAILS!

// ✅ Stub the mock
when(mockRepo.findByTitle("1984")).thenReturn(someBook);
```

---

### 2. Stubbing After Action

```java
// ❌ WRONG ORDER: Stub after calling method
service.findBookByTitle("1984");
when(mockRepo.findByTitle("1984")).thenReturn(book);  // Too late!

// ✅ CORRECT: Stub before calling
when(mockRepo.findByTitle("1984")).thenReturn(book);
service.findBookByTitle("1984");
```

**Remember AAA:** Arrange (stub), Act (call), Assert (verify).

---

### 3. Mixing Matchers and Literals

```java
// ❌ WRONG: Mixing
verify(mockRepo).findByTitleAndAuthor("1984", any());

// ✅ CORRECT: All matchers
verify(mockRepo).findByTitleAndAuthor(eq("1984"), any());
```

---

### 4. Verifying Mocks That Weren't Called

```java
@Test
void test() {
    // Service doesn't call repository in this scenario
    service.someMethod();

    // ❌ FAILS: Repository was never called
    verify(mockRepo).save(any());

    // ✅ CORRECT: Verify it wasn't called
    verify(mockRepo, never()).save(any());
}
```

---

### 5. Not Resetting Mocks Between Tests

**With @ExtendWith(MockitoExtension.class):** Mocks are auto-reset.

**Without it:** You might need to manually reset:

```java
@AfterEach
void tearDown() {
    reset(mockRepo);  // Clear all stubbing and interactions
}
```

---

## Action Items

### Priority 1 (Implement Tests from Section 17)

Now you have Mockito knowledge to write those 9 tests!

- [ ] **Create BookServiceTest.java**
  - Use `@ExtendWith(MockitoExtension.class)`
  - Use `@Mock` for repositories
  - Use `@InjectMocks` for BookService
  - Write 6 tests (see Section 17 and examples above)

- [ ] **Create BookcaseServiceTest.java**
  - Mock BookcaseRepository and ShelfRepository
  - Test createNewBookCase() happy path
  - Test createNewBookCase() duplicate label (verify exception thrown)
  - Test findBookCaseById()

### Priority 2 (Advanced Patterns)

- [ ] **Use ArgumentCaptor in at least one test**
  - Capture BookEntity saved to repository
  - Verify its properties

- [ ] **Use argThat() for complex verification**
  - Verify author saved with correct first/last name
  - Verify book saved with correct title

### Priority 3 (Cleanup)

- [ ] **Delete BookCommandsTest.java**
  - It's an empty placeholder (no value)
  - Or implement it properly (but low priority)

- [ ] **Run all tests**
  - `mvn test` or IDE runner
  - All should pass (green)!

---

## Key Takeaways

### 1. Mocking Isolates Unit Tests

```java
// With real dependencies: Testing BookService + Spring Data + PostgreSQL
BookService service = new BookService(realRepo, realAuthorRepo);

// With mocks: Testing ONLY BookService
BookService service = new BookService(mockRepo, mockAuthorRepo);
```

Unit tests should test **one unit**. Mocks achieve this.

---

### 2. The Mockito Trio

```java
// 1. CREATE mock
BookRepository mockRepo = mock(BookRepository.class);

// 2. STUB behavior
when(mockRepo.findByTitle("1984")).thenReturn(book);

// 3. VERIFY interactions
verify(mockRepo).save(book);
```

**Create → Stub → Verify.** That's 90% of Mockito usage.

---

### 3. Use @Mock and @InjectMocks

```java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository mockRepo;

    @InjectMocks  // Auto-injects mockRepo
    private BookService bookService;
}
```

Cleaner than manual `mock()` calls.

---

### 4. Argument Matchers

```java
// any() - matches anything
verify(mockRepo).save(any());

// eq() - matches exact value (when mixing)
verify(mockRepo).findByTitleAndAuthor(eq("1984"), any());

// argThat() - custom matcher
verify(mockRepo).save(argThat(book ->
    book.getTitle().equals("1984")
));
```

Flexible verification without exact object equality.

---

### 5. Verify Behavior, Not Implementation

```java
// ❌ Testing implementation detail
verify(mockRepo).findByTitleIgnoreCase("1984");

// ✅ Testing behavior
assertThat(result.getTitle()).isEqualTo("1984");
```

**Don't over-verify.** Focus on outcomes, not how code achieves them.

---

### 6. When to Mock

✅ **Mock:**
- External systems (database, HTTP, file I/O)
- Slow operations
- Complex setup

❌ **Don't Mock:**
- Value objects (DTOs, records)
- What you're testing
- Simple collaborators

---

### 7. AAA Pattern with Mocks

```
Arrange → Create and stub mocks
Act     → Call method under test
Assert  → Verify results AND interactions
```

Every test follows this structure.

---

### 8. Mockito + AssertJ = Powerful Tests

```java
// Mockito: Control dependencies
when(mockRepo.findByTitle("1984")).thenReturn(book);

// AssertJ: Readable assertions
assertThat(result).isNotNull();
assertThat(result.getTitle()).isEqualTo("1984");

// Mockito: Verify interactions
verify(mockRepo).findByTitle("1984");
```

---

## Practice Exercise

**Challenge:** Write a test for BookService.checkInBook()

**Requirements:**

1. Mock BookRepository
2. Stub `findBookByTitle()` to return a checked-out book
3. Call `checkInBook()`
4. Assert book status changed to "AVAILABLE"
5. Verify repository saved the book

**Solution:**

```java
@Test
@DisplayName("checkInBook() should change status to AVAILABLE and save")
void checkInBook_ChangesStatusToAvailable() {
    // Arrange
    BookEntity book = new BookEntity();
    book.setBookId(1L);
    book.setTitle("1984");
    book.setBookStatus("CHECKED_OUT");

    when(mockBookRepo.findByTitleIgnoreCase("1984")).thenReturn(book);

    // Act
    bookService.checkInBook("1984");

    // Assert
    assertThat(book.getBookStatus()).isEqualTo("AVAILABLE");
    verify(mockBookRepo).save(book);
}
```

---

**Mentor's Note:**

You now have everything you need to write comprehensive tests for Bibby.

**Section 17** taught you **why** to test and **what** to test.
**Section 18** taught you **how** to test with mocks.

The combination is powerful:
- JUnit provides the test framework
- AssertJ provides readable assertions
- Mockito provides isolated, fast tests

**Your next step:** Implement those 9 Priority 1 tests from Section 17.

Start with BookServiceTest. Create the file, add `@ExtendWith(MockitoExtension.class)`, mock the repositories, inject into BookService, and write one test.

Run it. Watch it pass. Feel that confidence grow.

Then write the next test. And the next.

Before you know it, you'll have a comprehensive test suite that gives you the confidence to refactor those unsafe Optional.get() calls, add new features, and ship code without fear.

**Tests aren't overhead. Tests are freedom.**

---

*Section 18 Complete — 55 min read*
*Next: Section 19 — Maven & Build Process*
*Progress: 18 / 33 sections (55%)*
