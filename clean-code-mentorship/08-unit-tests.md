# Section 8: Unit Tests
**Clean Code Principle:** *"Test code is just as important as production code. It requires thought, design, and care."*

---

## 📚 Principles Overview

### Why Unit Tests Matter

Uncle Bob says: **"The true test of clean code is how easy it is to write tests for it."** If your code is hard to test, it's probably poorly designed.

Tests aren't just about catching bugs—they're **living documentation** that proves your code works and enables **fearless refactoring**. Without tests, every change is a gamble.

### The F.I.R.S.T. Principles

Clean tests follow **F.I.R.S.T.**:

1. **Fast** - Tests should run in milliseconds, not seconds. Slow tests won't get run.
2. **Independent** - Tests shouldn't depend on each other. Run them in any order.
3. **Repeatable** - Same input = same output, every time. No flaky tests.
4. **Self-Validating** - Tests return boolean (pass/fail). No manual inspection.
5. **Timely** - Write tests *before* production code (TDD) or *immediately after*.

### One Assert Per Test (The Single Concept Rule)

Each test should verify **one concept** (though that concept might need multiple assertions):

```java
// ❌ BAD: Testing multiple unrelated concepts
@Test
void testEverything() {
    Book book = new Book("1984");
    assertEquals("1984", book.getTitle());      // Concept 1: Titles work
    book.checkOut();
    assertEquals(CHECKED_OUT, book.getStatus()); // Concept 2: Checkout works
    book.checkIn();
    assertEquals(AVAILABLE, book.getStatus());   // Concept 3: Checkin works
}

// ✅ GOOD: One concept per test
@Test
void checkingOutAvailableBookShouldChangeStatusToCheckedOut() {
    Book book = new Book("1984");
    book.checkOut();
    assertEquals(CHECKED_OUT, book.getStatus());
}

@Test
void checkingInCheckedOutBookShouldChangeStatusToAvailable() {
    Book book = new Book("1984");
    book.checkOut();
    book.checkIn();
    assertEquals(AVAILABLE, book.getStatus());
}
```

### Test Naming: Readable Sentences

Test names should tell a **story** without reading the code:

```java
// ❌ BAD: Cryptic, requires reading code to understand
@Test
void test1() { ... }

@Test
void searchTest() { ... }

@Test
void testFindBookByTitle() { ... }

// ✅ GOOD: Describes exact behavior being tested
@Test
void findingBookByExactTitleShouldReturnBook() { ... }

@Test
void findingBookWithNonexistentTitleShouldReturnEmpty() { ... }

@Test
void findingBookWithNullTitleShouldThrowIllegalArgumentException() { ... }
```

**Pro Tip:** Use the format `methodName_scenario_expectedBehavior()` or write full sentences.

### Given-When-Then Structure

Clean tests follow a clear structure (also called **Arrange-Act-Assert**):

```java
@Test
void checkingOutAvailableBookShouldChangeStatusToCheckedOut() {
    // GIVEN (Arrange): Set up test data
    BookEntity book = new BookEntity();
    book.setTitle("Clean Code");
    book.setBookStatus(BookStatus.AVAILABLE.toString());

    // WHEN (Act): Execute the behavior being tested
    bookService.checkOutBook(book);

    // THEN (Assert): Verify the expected outcome
    assertEquals(BookStatus.CHECKED_OUT.toString(), book.getBookStatus());
}
```

---

## 🔍 Your Code Analysis

### Current Testing State: CRITICAL ISSUES

I analyzed your test suite. Here's the hard truth:

**Total Test Files:** 2
**Total Meaningful Tests:** 0
**Production Code Files:** 42
**Test Coverage:** ~0%

#### Issue #1: Empty Test Skeleton (BookCommandsTest.java)

**Location:** `src/test/java/com/penrose/bibby/cli/BookCommandsTest.java`

```java
class BookCommandsTest {
    @Test
    public void searchByTitleTest(){
        BookEntity bookEntity = null;  // ❌ Creates null, does nothing
        // NO ASSERTIONS
        // NO BEHAVIOR TESTED
    }
}
```

**Problems:**
1. Test has no assertions (always passes, even if code is broken)
2. Variable created but never used
3. Name doesn't describe what's being tested
4. No setup, no act, no assert

**This test is worse than no test** because it gives false confidence.

---

#### Issue #2: Only Boilerplate Application Test

**Location:** `src/test/java/com/penrose/bibby/BibbyApplicationTests.java`

```java
@SpringBootTest
class BibbyApplicationTests {
    @Test
    void contextLoads() {
        // Empty - just verifies Spring context starts
    }
}
```

**What This Tests:** That Spring Boot can start.
**What It Doesn't Test:** Any of your business logic.

This is the default Spring Boot test. It's useful, but you have **42 Java files** with real logic that aren't tested at all.

---

#### Issue #3: ZERO Service Layer Tests

Your `BookService` has **10 methods** with complex business logic. **None** are tested.

Let's look at the critical methods that desperately need tests:

**BookService.findBookByTitle() - DANGEROUS CODE**

```java
public BookEntity findBookByTitle(String title){
    Optional<BookEntity> bookEntity = Optional.ofNullable(
        bookRepository.findByTitleIgnoreCase(title)
    );
    List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
    for(BookEntity b : bookEntities){
        System.out.println(b.getTitle());  // ❌ Debug code left in production
    }

    if(bookEntity.isEmpty()){
        return null;  // ❌ RETURNING NULL (we covered this in Section 6)
    }
    return bookEntity.get();
}
```

**Untested Scenarios:**
- ✗ What happens when `title` is null?
- ✗ What happens when `title` is empty string?
- ✗ What happens when no book is found?
- ✗ What happens when multiple books match?
- ✗ Does it handle case insensitivity correctly?

**Without tests, you don't know if this method works.** And looking at it, I can tell you it has bugs:
1. Returns null instead of Optional
2. Has debug `System.out.println` that shouldn't be in production
3. Queries database twice (once with `findByTitleIgnoreCase`, once with `findByTitleContaining`)
4. Ignores the second query result

---

**BookService.checkInBook() - NULL POINTER TIME BOMB**

```java
public void checkInBook(String bookTitle) {
    BookEntity bookEntity = findBookByTitle(bookTitle);  // ❌ Can return null!
    bookEntity.setBookStatus(BookStatus.AVAILABLE.toString());  // 💥 CRASH if null
    updateBook(bookEntity);
}
```

**If you call this with a non-existent book title, your application crashes.**

**Untested Scenarios:**
- ✗ What happens when book doesn't exist?
- ✗ What happens when book is already available?
- ✗ What happens when bookTitle is null?

---

**BookService.createNewBook() - COMPLEX LOGIC, NO TESTS**

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
    bookEntity.setAuthors(authorEntity);
    bookRepository.save(bookEntity);
}
```

**This method has 4 different code paths:**
1. New book + new author
2. Existing book + new author
3. New book + existing author
4. Existing book + existing author

**Without tests, you don't know if any of these paths work correctly.**

---

#### Issue #4: Missing Mocking Framework

Your `pom.xml` includes `spring-boot-starter-test` which bundles **Mockito**, but you're not using it.

**Why You Need Mocking:**
- Service tests shouldn't hit the database (slow, not unit tests)
- Tests should be **independent** (FIRST principle)
- You need to test error scenarios (what if database is down?)

---

#### Issue #5: No Repository Tests

You have custom `@Query` methods in your repositories:

```java
@Query("SELECT b FROM BookEntity b WHERE b.title = :title")
BookEntity findByTitle(@Param("title") String title);
```

**Are these queries correct?** You don't know without tests.

Spring Data JPA can generate wrong queries. **Test them.**

---

#### Issue #6: No Integration Tests

Your command classes (`BookCommands`, `BookcaseCommands`) have **594 lines** and **184 lines** respectively.

**Zero integration tests** verify that:
- Commands wire up correctly with Spring
- ComponentFlow interactions work
- End-to-end user flows function

---

## 🛠️ Refactoring Examples

### Example 1: Testing BookService.findBookByTitle()

**Step 1: Fix the production code first** (remember Section 6: Error Handling)

```java
// BEFORE: Returns null, queries twice
public BookEntity findBookByTitle(String title) {
    Optional<BookEntity> bookEntity = Optional.ofNullable(
        bookRepository.findByTitleIgnoreCase(title)
    );
    List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
    for(BookEntity b : bookEntities){
        System.out.println(b.getTitle());
    }
    if(bookEntity.isEmpty()){
        return null;
    }
    return bookEntity.get();
}

// AFTER: Returns Optional, single query, no side effects
public Optional<BookEntity> findBookByTitle(String title) {
    if (title == null || title.isBlank()) {
        throw new IllegalArgumentException("Title cannot be null or blank");
    }
    return Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
}
```

**Step 2: Write comprehensive tests**

Create: `src/test/java/com/penrose/bibby/library/book/BookServiceTest.java`

```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // Enables Mockito
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;  // Mock the database

    @Mock
    private AuthorRepository authorRepository;

    private BookService bookService;  // The service we're testing

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, authorRepository);
    }

    // Test 1: Happy path - book exists
    @Test
    void findingBookByExactTitleShouldReturnBook() {
        // GIVEN: A book exists in the repository
        String title = "Clean Code";
        BookEntity expectedBook = new BookEntity();
        expectedBook.setTitle(title);
        when(bookRepository.findByTitleIgnoreCase(title))
            .thenReturn(expectedBook);

        // WHEN: We search for it
        Optional<BookEntity> result = bookService.findBookByTitle(title);

        // THEN: We get the book
        assertTrue(result.isPresent());
        assertEquals(title, result.get().getTitle());
        verify(bookRepository).findByTitleIgnoreCase(title);  // Verify interaction
    }

    // Test 2: Book doesn't exist
    @Test
    void findingBookWithNonexistentTitleShouldReturnEmpty() {
        // GIVEN: No book with this title exists
        String title = "Nonexistent Book";
        when(bookRepository.findByTitleIgnoreCase(title))
            .thenReturn(null);

        // WHEN: We search for it
        Optional<BookEntity> result = bookService.findBookByTitle(title);

        // THEN: We get empty Optional
        assertTrue(result.isEmpty());
    }

    // Test 3: Null title
    @Test
    void findingBookWithNullTitleShouldThrowException() {
        // GIVEN: A null title
        String title = null;

        // WHEN/THEN: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.findBookByTitle(title);
        });

        // Verify we never hit the database
        verify(bookRepository, never()).findByTitleIgnoreCase(any());
    }

    // Test 4: Blank title
    @Test
    void findingBookWithBlankTitleShouldThrowException() {
        // GIVEN: A blank title
        String title = "   ";

        // WHEN/THEN: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.findBookByTitle(title);
        });
    }

    // Test 5: Case insensitivity
    @Test
    void findingBookShouldBeCaseInsensitive() {
        // GIVEN: A book stored with mixed case
        BookEntity book = new BookEntity();
        book.setTitle("Clean Code");
        when(bookRepository.findByTitleIgnoreCase("clean code"))
            .thenReturn(book);

        // WHEN: We search with lowercase
        Optional<BookEntity> result = bookService.findBookByTitle("clean code");

        // THEN: We find the book
        assertTrue(result.isPresent());
        assertEquals("Clean Code", result.get().getTitle());
    }
}
```

**What This Demonstrates:**
- ✅ Each test has ONE concept (following Single Concept Rule)
- ✅ Test names are readable sentences
- ✅ Given-When-Then structure
- ✅ Uses Mockito to avoid hitting database (FAST)
- ✅ Tests edge cases (null, blank, case insensitivity)
- ✅ Verifies interactions with `verify()`

---

### Example 2: Testing BookService.checkOutBook()

**Current Code:**

```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

**Tests:**

```java
@Test
void checkingOutAvailableBookShouldChangeStatusToCheckedOut() {
    // GIVEN: An available book
    BookEntity book = new BookEntity();
    book.setBookStatus(BookStatus.AVAILABLE.toString());

    // WHEN: We check it out
    bookService.checkOutBook(book);

    // THEN: Status changes and repository saves
    assertEquals(BookStatus.CHECKED_OUT.toString(), book.getBookStatus());
    verify(bookRepository).save(book);
}

@Test
void checkingOutAlreadyCheckedOutBookShouldNotSave() {
    // GIVEN: A book already checked out
    BookEntity book = new BookEntity();
    book.setBookStatus(BookStatus.CHECKED_OUT.toString());

    // WHEN: We try to check it out again
    bookService.checkOutBook(book);

    // THEN: No save occurs (idempotent operation)
    verify(bookRepository, never()).save(any());
}

@Test
void checkingOutNullBookShouldThrowException() {
    // WHEN/THEN: Null book should throw
    assertThrows(NullPointerException.class, () -> {
        bookService.checkOutBook(null);
    });
}
```

**Lesson:** Notice the third test catches a bug! Your current code **will crash** if someone passes null.

**Fix in production code:**

```java
public void checkOutBook(BookEntity bookEntity) {
    if (bookEntity == null) {
        throw new IllegalArgumentException("Book cannot be null");
    }

    if (!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())) {
        bookEntity.setBookStatus(BookStatus.CHECKED_OUT.toString());
        bookRepository.save(bookEntity);
    }
}
```

---

### Example 3: Testing createNewBook() - Multiple Scenarios

This method has **4 code paths**. You need 4 tests (minimum):

```java
@Test
void creatingNewBookWithNewAuthorShouldSaveBoth() {
    // GIVEN: Book and author don't exist
    BookRequestDTO request = new BookRequestDTO("1984", "George", "Orwell");
    when(bookRepository.findByTitle("1984")).thenReturn(null);
    when(authorRepository.findByFirstNameAndLastName("George", "Orwell"))
        .thenReturn(null);

    // WHEN: We create the book
    bookService.createNewBook(request);

    // THEN: Both author and book are saved
    verify(authorRepository).save(any(AuthorEntity.class));
    verify(bookRepository).save(any(BookEntity.class));
}

@Test
void creatingExistingBookWithNewAuthorShouldOnlyCreateAuthor() {
    // GIVEN: Book exists, author doesn't
    BookRequestDTO request = new BookRequestDTO("1984", "George", "Orwell");
    BookEntity existingBook = new BookEntity();
    existingBook.setTitle("1984");

    when(bookRepository.findByTitle("1984")).thenReturn(existingBook);
    when(authorRepository.findByFirstNameAndLastName("George", "Orwell"))
        .thenReturn(null);

    // WHEN: We create the book
    bookService.createNewBook(request);

    // THEN: Only author is created, book is updated
    verify(authorRepository).save(any(AuthorEntity.class));
    verify(bookRepository).save(existingBook);
}

// Add 2 more tests for the other paths...
```

---

## 🎯 Clean Code Testing Patterns

### Pattern 1: Test Behavior, Not Implementation

```java
// ❌ BAD: Tests internal implementation details
@Test
void bookRepositoryShouldBeCalled() {
    bookService.findBookByTitle("Clean Code");
    verify(bookRepository).findByTitleIgnoreCase("Clean Code");
}

// ✅ GOOD: Tests observable behavior
@Test
void findingExistingBookShouldReturnBook() {
    BookEntity book = new BookEntity();
    book.setTitle("Clean Code");
    when(bookRepository.findByTitleIgnoreCase("Clean Code")).thenReturn(book);

    Optional<BookEntity> result = bookService.findBookByTitle("Clean Code");

    assertTrue(result.isPresent());
    assertEquals("Clean Code", result.get().getTitle());
}
```

**Why?** The first test is **brittle**. If you rename the repository method, the test breaks even though behavior is unchanged.

---

### Pattern 2: Don't Test Private Methods

```java
// ❌ BAD: Testing private methods by making them public/protected
@Test
void testValidateBookTitle() {
    bookService.validateBookTitle("Clean Code");  // Made public just for testing
}

// ✅ GOOD: Test private methods through public interface
@Test
void findingBookWithInvalidTitleShouldThrowException() {
    // validateBookTitle is tested indirectly
    assertThrows(IllegalArgumentException.class, () -> {
        bookService.findBookByTitle("");
    });
}
```

**Why?** Private methods are implementation details. They change frequently. Test the public API.

---

### Pattern 3: Use Test Fixtures for Repeated Setup

```java
// ❌ BAD: Duplicated setup in every test
@Test
void test1() {
    BookEntity book = new BookEntity();
    book.setTitle("Clean Code");
    book.setBookStatus(BookStatus.AVAILABLE.toString());
    // ...
}

@Test
void test2() {
    BookEntity book = new BookEntity();
    book.setTitle("Clean Code");
    book.setBookStatus(BookStatus.AVAILABLE.toString());
    // ...
}

// ✅ GOOD: Extract to helper method
private BookEntity createAvailableBook(String title) {
    BookEntity book = new BookEntity();
    book.setTitle(title);
    book.setBookStatus(BookStatus.AVAILABLE.toString());
    return book;
}

@Test
void test1() {
    BookEntity book = createAvailableBook("Clean Code");
    // ...
}

@Test
void test2() {
    BookEntity book = createAvailableBook("Clean Code");
    // ...
}
```

---

### Pattern 4: One Assert Per Concept (But Multiple Asserts OK)

```java
// ✅ GOOD: Multiple asserts testing ONE concept (book state after checkout)
@Test
void checkingOutBookShouldUpdateAllRelevantFields() {
    BookEntity book = createAvailableBook("Clean Code");
    LocalDateTime beforeCheckout = LocalDateTime.now();

    bookService.checkOutBook(book);

    // These asserts all verify the SAME concept: proper checkout state
    assertEquals(BookStatus.CHECKED_OUT.toString(), book.getBookStatus());
    assertNotNull(book.getCheckoutDate());
    assertTrue(book.getCheckoutDate().isAfter(beforeCheckout));
}
```

---

## 🚨 Your Critical Issues & Solutions

### Issue Summary Table

| Issue | Severity | Impact | Fix Time |
|-------|----------|--------|----------|
| Zero service tests | 🔴 CRITICAL | Production bugs go undetected | 6-8 hours |
| Empty test skeleton | 🔴 CRITICAL | False confidence in code quality | 30 min |
| No mocking setup | 🟡 HIGH | Tests will be slow, hit database | 1 hour |
| No repository tests | 🟡 HIGH | Custom queries untested | 2 hours |
| No integration tests | 🟡 MEDIUM | End-to-end flows unverified | 4 hours |

---

## ✅ Your Action Items

### 🔴 **Priority 1: Fix the Empty Test** (30 minutes)

**File:** `src/test/java/com/penrose/bibby/cli/BookCommandsTest.java`

**Current Code:**
```java
@Test
public void searchByTitleTest(){
    BookEntity bookEntity = null;
}
```

**Delete this test.** It provides zero value and gives false confidence.

**OR** implement it properly (after you refactor BookCommands to be testable):

```java
@Test
void searchingForExistingBookShouldDisplayBookDetails() {
    // This requires refactoring BookCommands first
    // See Section 2 (Functions) and Section 7 (Boundaries)
}
```

---

### 🔴 **Priority 2: Create BookServiceTest** (3-4 hours)

**Action:** Create comprehensive unit tests for BookService

**File:** `src/test/java/com/penrose/bibby/library/book/BookServiceTest.java`

**Test Coverage Needed:**
- [ ] `findBookByTitle()` - happy path
- [ ] `findBookByTitle()` - book not found
- [ ] `findBookByTitle()` - null/blank title
- [ ] `checkOutBook()` - available book
- [ ] `checkOutBook()` - already checked out
- [ ] `checkOutBook()` - null book
- [ ] `checkInBook()` - checked out book
- [ ] `checkInBook()` - already available
- [ ] `checkInBook()` - nonexistent book
- [ ] `createNewBook()` - new book, new author
- [ ] `createNewBook()` - existing book, new author
- [ ] `createNewBook()` - new book, existing author
- [ ] `createNewBook()` - existing book, existing author

**Use the examples above as templates.**

---

### 🟡 **Priority 3: Add Repository Tests** (2 hours)

**File:** `src/test/java/com/penrose/bibby/library/book/BookRepositoryTest.java`

```java
@DataJpaTest  // Configures in-memory database for repository tests
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void findByTitleIgnoreCaseShouldBeCaseInsensitive() {
        // GIVEN: A book stored with mixed case
        BookEntity book = new BookEntity();
        book.setTitle("Clean Code");
        bookRepository.save(book);

        // WHEN: We search with lowercase
        BookEntity result = bookRepository.findByTitleIgnoreCase("clean code");

        // THEN: We find the book
        assertNotNull(result);
        assertEquals("Clean Code", result.getTitle());
    }

    @Test
    void findByTitleContainingShouldReturnPartialMatches() {
        // GIVEN: Multiple books with "Code" in title
        bookRepository.save(createBook("Clean Code"));
        bookRepository.save(createBook("Code Complete"));
        bookRepository.save(createBook("The Pragmatic Programmer"));

        // WHEN: We search for "Code"
        List<BookEntity> results = bookRepository.findByTitleContaining("Code");

        // THEN: We get 2 matches
        assertEquals(2, results.size());
    }

    // Test your custom @Query methods...
}
```

**Why `@DataJpaTest`?**
- Automatically configures H2 in-memory database
- Rolls back after each test (tests stay independent)
- Faster than full `@SpringBootTest`

---

### 🟡 **Priority 4: Learn Mockito** (1 hour study)

**Key Concepts to Master:**

1. **`@Mock`** - Creates a mock object
2. **`when().thenReturn()`** - Stub method behavior
3. **`verify()`** - Verify interactions
4. **`any()`**, **`eq()`** - Argument matchers

**Study Resource:**
```java
// Mocking a method call
when(bookRepository.findByTitle("Clean Code"))
    .thenReturn(someBook);

// Verifying a method was called
verify(bookRepository).save(any(BookEntity.class));

// Verifying a method was NOT called
verify(bookRepository, never()).delete(any());

// Verifying exact number of calls
verify(bookRepository, times(2)).findByTitle(anyString());
```

---

### 🟢 **Priority 5: Add Integration Tests** (3-4 hours)

**File:** `src/test/java/com/penrose/bibby/integration/BookWorkflowIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
class BookWorkflowIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();  // Clean slate for each test
    }

    @Test
    @Transactional
    void completeBookLifecycleShouldWork() {
        // GIVEN: We create a new book
        BookRequestDTO request = new BookRequestDTO(
            "Clean Code", "Robert", "Martin"
        );
        bookService.createNewBook(request);

        // WHEN: We find, check out, and check in the book
        Optional<BookEntity> book = bookService.findBookByTitle("Clean Code");
        assertTrue(book.isPresent());

        bookService.checkOutBook(book.get());
        assertEquals(BookStatus.CHECKED_OUT.toString(),
                     book.get().getBookStatus());

        bookService.checkInBook("Clean Code");

        // THEN: Book is back to available
        Optional<BookEntity> checkedInBook = bookService
            .findBookByTitle("Clean Code");
        assertEquals(BookStatus.AVAILABLE.toString(),
                     checkedInBook.get().getBookStatus());
    }
}
```

---

## 📊 Measuring Success

### Test Coverage Goals

**Current:** ~0%
**Target (End of Week 1):** 40%
**Target (End of Week 2):** 60%
**Target (Professional):** 70-80%

**How to Measure:**
Add JaCoCo plugin to your `pom.xml`:

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

**Run:** `mvn clean test jacoco:report`
**View:** `target/site/jacoco/index.html`

---

## 🎓 Key Takeaways

1. **Tests are not optional.** They're part of the definition of "done."

2. **Good tests enable fearless refactoring.** You can improve code without breaking it.

3. **F.I.R.S.T. principles** keep tests maintainable and fast.

4. **One concept per test** makes failures easy to diagnose.

5. **Test names should be sentences** that describe behavior.

6. **Mock external dependencies** (databases, APIs) for unit tests.

7. **Test behavior, not implementation** to avoid brittle tests.

8. **Coverage is a metric, not a goal.** 80% thoughtful coverage beats 100% mindless coverage.

9. **Write tests BEFORE fixing bugs.** The test should fail first, then pass after your fix.

10. **Your test code quality matters.** Clean Code principles apply to tests too.

---

## 📚 Further Study

### Books
- **"Test Driven Development: By Example"** - Kent Beck
  *The bible of TDD. Short, practical, transformative.*

- **"Growing Object-Oriented Software, Guided by Tests"** - Freeman & Pryce
  *Advanced TDD techniques and mocking strategies.*

### Articles
- **Martin Fowler: "Mocks Aren't Stubs"**
  https://martinfowler.com/articles/mocksArentStubs.html

- **Uncle Bob: "The Three Laws of TDD"**
  Write tests first, always.

### Practice
- **Kata: String Calculator TDD** (search "String Calculator Kata")
  Practice writing tests first for a simple problem.

- **Refactoring Exercise:** Take your `BookService.findBookByTitle()` and refactor it using TDD:
  1. Write tests for current behavior
  2. Refactor code
  3. Verify tests still pass

---

## 💭 Mentor's Final Thoughts

Leo, I won't sugarcoat this: **your codebase has no meaningful tests.** This is the single biggest risk to your project's long-term health.

But here's the good news: **you're about to learn one of the most valuable skills in software engineering.** Writing clean, comprehensive tests separates junior developers from senior developers. It's the difference between "it works on my machine" and "I'm confident this code is correct."

Think about it: right now, you have **no way to know** if your refactorings from Sections 1-7 break existing functionality. Every change is a gamble. Tests eliminate that fear.

**Start with BookService.** Get those 13 test cases written. You'll immediately catch bugs you didn't know existed. Then, when you refactor (fixing the null returns, adding error handling, extracting methods), your tests will tell you if you broke anything.

**This is where the rubber meets the road.** Clean Code isn't just about pretty naming and small functions—it's about writing code you can trust. And trust comes from tests.

You've got this. One test at a time.

— Your Mentor

---

**Next:** Section 9 - Classes (design principles, SOLID, cohesion)
**Previous:** [Section 7 - Boundaries](./07-boundaries.md)
**Home:** [Master Index](./00-master-index.md)
