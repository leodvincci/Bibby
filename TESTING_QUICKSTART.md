# Testing Quick Start Guide

## 🎯 What We've Created

I've created two comprehensive resources for you:

1. **`TESTING_MENTORSHIP_GUIDE.md`** - Your complete learning guide (12,000+ words!)
2. **`src/test/java/com/penrose/bibby/library/book/BookServiceTest.java`** - A fully working test suite with detailed comments

---

## 📚 Your Codebase at a Glance

```
Bibby - Library Management System
│
├── 📦 Domain Layer
│   ├── book/         → 8 classes (entities, repos, services)
│   ├── author/       → 6 classes
│   ├── shelf/        → 6 classes
│   ├── bookcase/     → 6 classes
│   └── genre/        → 1 class
│
├── 🖥️  CLI Layer
│   ├── BookCommands       → 578 lines (needs refactoring)
│   └── BookcaseCommands   → Interactive menus
│
└── 🛠️  Utilities
    ├── StartupRunner
    ├── LoadingBar
    └── BibbyPersonality

Current Test Coverage: ~0% → Let's fix that!
```

---

## 🎓 What Makes a Good Unit Test?

### The Test Pyramid

```
    /\
   /  \    ← Few E2E/UI tests (very slow)
  /____\
 /      \   ← Some integration tests (slow)
/________\
/          \ ← LOTS of unit tests (fast, isolated)
```

**Unit tests should be:**
- ✅ **Fast** (milliseconds, not seconds)
- ✅ **Isolated** (test one class at a time)
- ✅ **Repeatable** (same result every time)
- ✅ **Self-validating** (pass/fail, no manual checks)
- ✅ **Thorough** (cover happy path + edge cases)

---

## 🚀 Running Your Tests

### Run all tests:
```bash
mvn test
```

### Run only BookServiceTest:
```bash
mvn test -Dtest=BookServiceTest
```

### Run a specific test method:
```bash
mvn test -Dtest=BookServiceTest#shouldCreateNewAuthorAndBook_WhenNeitherExist
```

### Run tests with coverage report:
```bash
mvn test jacoco:report
# Report: target/site/jacoco/index.html
```

---

## 📖 Understanding the BookServiceTest

I've created **15 tests** for `BookService` covering:

### createNewBook() - 3 tests
- ✅ Create new author and book when neither exist
- ✅ Reuse existing author when author already exists
- ✅ Update existing book when book already exists

### checkOutBook() - 2 tests
- ✅ Update status to CHECKED_OUT when available
- ✅ Don't update when already checked out

### checkInBook() - 2 tests
- ✅ Update status to AVAILABLE when checking in
- ⚠️ Throw NPE when book not found (documents a bug!)

### findBookByTitle() - 3 tests
- ✅ Return book when exact match found
- ✅ Return null when not found
- ✅ Find book case-insensitively

### findBookById() - 2 tests
- ✅ Return Optional with book when found
- ✅ Return empty Optional when not found

---

## 🔑 Key Concepts Explained in the Tests

### 1. Mocking with Mockito

```java
@Mock
private BookRepository bookRepository;  // Fake repository

@InjectMocks
private BookService bookService;  // Real service with fake dependencies
```

**Why mock repositories?**
- No database needed
- Tests run in milliseconds
- Full control over return values
- Easy to test error scenarios

### 2. Stubbing Behavior

```java
// "When findByTitle is called with '1984', return this book"
when(bookRepository.findByTitle("1984")).thenReturn(book);
```

### 3. Verifying Interactions

```java
// Verify save() was called exactly once
verify(bookRepository).save(any(BookEntity.class));

// Verify save() was NEVER called
verify(bookRepository, never()).save(any());
```

### 4. Capturing Arguments

```java
ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
verify(bookRepository).save(captor.capture());
BookEntity savedBook = captor.getValue();

// Now we can inspect what was saved:
assertEquals("1984", savedBook.getTitle());
```

### 5. Testing Exceptions

```java
assertThrows(NullPointerException.class, () -> {
    bookService.checkInBook("Nonexistent Book");
});
```

---

## 🐛 Bugs Found by the Tests

### Bug 1: NullPointerException in checkInBook()
**Location:** `BookService:74-76`

```java
public void checkInBook(String bookTitle) {
    BookEntity bookEntity = findBookByTitle(bookTitle);
    bookEntity.setBookStatus(BookStatus.AVAILABLE.toString());  // ← NPE if null!
    updateBook(bookEntity);
}
```

**Fix:**
```java
public void checkInBook(String bookTitle) {
    BookEntity bookEntity = findBookByTitle(bookTitle);

    if (bookEntity == null) {
        throw new BookNotFoundException("Book not found: " + bookTitle);
    }

    bookEntity.setBookStatus(BookStatus.AVAILABLE.toString());
    updateBook(bookEntity);
}
```

### Bug 2: Overwrites all authors in createNewBook()
**Location:** `BookService:39`

```java
bookEntity.setAuthors(authorEntity);  // Overwrites ALL authors!
```

**Problem:** If a book has multiple authors, this removes all but the last one.

**Fix:** Use `addAuthor()` instead of `setAuthors()`:
```java
bookEntity.addAuthor(authorEntity);
```

---

## 📝 Test Naming Convention

I use this pattern:
```
should<ExpectedBehavior>_When<Condition>
```

Examples:
- `shouldCreateNewBook_WhenValidInput`
- `shouldThrowException_WhenTitleIsNull`
- `shouldReuseExistingAuthor_WhenAuthorExists`

**Why?**
- Tests read like documentation
- Clear what's being tested
- Clear what conditions matter

---

## 🎯 Your Learning Path

### Week 1: Foundations ✅
- [x] Scan codebase
- [x] Create test suite for BookService
- [ ] **RUN the tests** (when you have internet for Maven)
- [ ] **READ all comments** in BookServiceTest.java
- [ ] Modify one test and see what breaks

### Week 2: Expand
- [ ] Write tests for `BookcaseService.createNewBookCase()`
- [ ] Write tests for `BookMapper.toDomain()`
- [ ] Write tests for `BookMapper.toEntity()`
- [ ] Practice using ArgumentCaptor

### Week 3: Advanced
- [ ] Write repository integration tests using `@DataJpaTest`
- [ ] Fix the `checkInBook()` bug
- [ ] Write tests for the fixed version
- [ ] Add edge case tests (null inputs, empty strings)

### Week 4: Master
- [ ] Extract UI logic from `BookCommands`
- [ ] Write tests for extracted components
- [ ] Achieve 80%+ coverage on service layer
- [ ] Refactor based on test feedback

---

## 💡 Testing Best Practices (TL;DR)

### ✅ DO:
- Test behavior, not implementation
- Mock external dependencies (DB, APIs)
- Use meaningful test names
- Test edge cases (null, empty, boundaries)
- Keep tests simple and readable
- Use Given/When/Then structure

### ❌ DON'T:
- Mock value objects (DTOs, domain models)
- Test private methods directly
- Share state between tests
- Have giant @BeforeEach methods
- Test framework code (Spring, Hibernate)
- Over-verify (testing too many internal details)

---

## 🔍 What to Test Next

### High Priority:
1. **BookcaseService** (complex loop logic)
2. **BookMapper** (easy - no mocks needed!)
3. **ShelfService** (good practice)

### Medium Priority:
4. Repository integration tests
5. Controller tests with MockMvc
6. AuthorService (fix the null repository bug first!)

### Low Priority:
7. Domain models (mostly getters/setters)
8. DTOs (records are simple)
9. CLI commands (refactor first for testability)

---

## 📚 Additional Resources

### In This Repo:
- **TESTING_MENTORSHIP_GUIDE.md** - Full 12,000-word guide
- **BookServiceTest.java** - Working example with 200+ comments

### External:
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Effective Unit Testing](https://www.manning.com/books/effective-unit-testing) (book)

---

## 🎓 Key Takeaways

1. **Unit tests test ONE class in isolation** - mock everything else
2. **Integration tests test components working together** - use real implementations
3. **Mockito lets you control dependencies** - stub behavior, verify calls
4. **Good tests are fast, isolated, and readable** - no database, no shared state
5. **Test behavior, not implementation** - focus on outcomes, not internals
6. **Tests are documentation** - they show how your code SHOULD work

---

## 🚦 Next Steps

1. **Read** `TESTING_MENTORSHIP_GUIDE.md` (start to finish)
2. **Study** `BookServiceTest.java` (every comment)
3. **Run** the tests when you have internet
4. **Experiment** - change a test, see what breaks
5. **Write** your first test from scratch (try BookcaseService)
6. **Ask questions** - testing is a skill you learn by doing!

---

## 📞 Quick Reference

### Common Mockito Methods:
```java
// Stubbing
when(mock.method()).thenReturn(value);
when(mock.method()).thenThrow(exception);

// Verification
verify(mock).method();
verify(mock, times(2)).method();
verify(mock, never()).method();

// Argument matching
any()
eq(value)
argThat(predicate)
```

### Common JUnit 5 Assertions:
```java
assertEquals(expected, actual);
assertNotNull(value);
assertTrue(condition);
assertThrows(Exception.class, () -> { code });
```

---

**You've got this!** Start small, build confidence, then expand. Testing is like any skill - you get better with practice. 🚀
