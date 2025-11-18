# Section 32: Hands-On Exercises

**Estimated Time:** Variable (practice-based, 8-15 hours total)
**Prerequisite Sections:** All Sections 1-31

---

## Introduction

You've learned **31 sections** of Java and Spring Boot fundamentals. Now it's time to **practice** by actually fixing real issues in your Bibby codebase.

This section contains **15 refactoring exercises** organized by difficulty:
- **Beginner (1-5):** Quick wins, 15-30 minutes each
- **Intermediate (6-10):** Moderate refactorings, 1-2 hours each
- **Advanced (11-15):** Complex improvements, 2-4 hours each

Each exercise includes:
- ✅ **Problem statement** with file references
- ✅ **Learning objectives** (which concepts you'll practice)
- ✅ **Step-by-step solution** with code examples
- ✅ **Testing guidance** (how to verify it works)
- ✅ **Commit message** (what to write when done)

**How to Use This Section:**
1. Pick an exercise (start with #1 if unsure)
2. Try solving it yourself first (use previous sections as reference)
3. Compare your solution with the provided solution
4. Test your changes
5. Commit with the suggested message
6. Move to the next exercise

Ready? Let's refactor!

---

## Exercise Difficulty Guide

| Level | Exercises | Time Each | Skills Practiced |
|-------|-----------|-----------|------------------|
| 🟢 **Beginner** | 1-5 | 15-30 min | Annotations, safety checks, simple refactors |
| 🟡 **Intermediate** | 6-10 | 1-2 hours | Testing, DTOs, validation, transactions |
| 🔴 **Advanced** | 11-15 | 2-4 hours | Entity design, performance, integration tests |

---

# 🟢 Beginner Exercises (Quick Wins)

## Exercise 1: Fix AuthorService Dependency Injection

**Difficulty:** 🟢 Beginner (15 minutes)
**Section Reference:** 21 (Spring IoC & Dependency Injection)
**File:** `src/main/java/com/penrose/bibby/library/author/AuthorService.java`

### Problem

AuthorService has **no dependency injection** for its `AuthorRepository` field, causing a guaranteed `NullPointerException` on first use.

**Current Code:**
```java
@Service
public class AuthorService {
    AuthorRepository authorRepository;  // ❌ Never injected!

    public void save(AuthorEntity author) {
        authorRepository.save(author);  // ← NullPointerException!
    }
}
```

### Learning Objectives
- Practice constructor injection pattern
- Understand why field injection without @Autowired fails
- Make fields `private final` for immutability

### Your Task

Add proper constructor injection to `AuthorService`.

### Solution

**Step 1:** Make the field `private final`
```java
@Service
public class AuthorService {
    private final AuthorRepository authorRepository;  // ← private final
```

**Step 2:** Add constructor with parameter
```java
    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }
```

**Step 3:** Full corrected code
```java
@Service
public class AuthorService {
    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public void save(AuthorEntity author) {
        authorRepository.save(author);  // ✅ Works!
    }
}
```

### Testing

**Manual Test:**
1. Run the application: `mvn spring-boot:run`
2. Try creating a book (which saves an author)
3. Should no longer crash with `NullPointerException`

**Unit Test (Optional):**
```java
@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {
    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorService authorService;

    @Test
    void testSave() {
        AuthorEntity author = new AuthorEntity("Robert", "Martin");
        authorService.save(author);
        verify(authorRepository).save(author);
    }
}
```

### Commit Message
```
Fix: Add constructor injection to AuthorService

- Change authorRepository from uninjected field to constructor parameter
- Make field private final for immutability
- Fixes guaranteed NullPointerException on first use

Resolves critical bug from Section 21 (Spring IoC).
```

---

## Exercise 2: Add @Transactional to BookcaseService.createNewBookCase()

**Difficulty:** 🟢 Beginner (15 minutes)
**Section Reference:** 29 (Transaction Management)
**File:** `src/main/java/com/penrose/bibby/library/bookcase/BookcaseService.java:25-40`

### Problem

`createNewBookCase()` saves a bookcase and then loops to create N shelves **without a transaction**. If shelf creation fails mid-loop, the bookcase and partial shelves remain in the database (data corruption).

**Current Code:**
```java
public String createNewBookCase(String label, int capacity) {  // ❌ No @Transactional
    bookcaseRepository.save(bookcaseEntity);  // Saved immediately

    for (int i = 0; i < capacity; i++) {
        addShelf(bookcaseEntity, i, i);  // If fails at i=3, bookcase + shelves 1-2 already saved!
    }
}
```

**Failure Scenario:**
- Request: Create "Fiction" bookcase with 5 shelves
- Shelf 1 saves ✅
- Shelf 2 saves ✅
- Shelf 3 fails ❌ (database constraint violation)
- **Result:** Bookcase "Fiction" exists with 2 shelves instead of 5 ❌

### Learning Objectives
- Understand ACID atomicity (all-or-nothing)
- Practice adding `@Transactional` annotation
- Recognize when transactions are needed (multiple writes that must succeed together)

### Your Task

Add `@Transactional` to make this method atomic.

### Solution

**Step 1:** Add the annotation
```java
@Transactional  // ← ADD THIS LINE
public String createNewBookCase(String label, int capacity) {
    bookcaseRepository.save(bookcaseEntity);

    for (int i = 0; i < capacity; i++) {
        addShelf(bookcaseEntity, i, i);
    }
}
```

That's it! One line fixes the critical bug.

**How It Works:**
- Spring creates a transaction at method entry
- All database operations happen inside the transaction
- If ANY operation fails → entire transaction rolls back
- If ALL operations succeed → transaction commits at method exit

**After Fix:**
- Request: Create "Fiction" bookcase with 5 shelves
- Shelf 1 saves (uncommitted)
- Shelf 2 saves (uncommitted)
- Shelf 3 fails ❌
- **Result:** Transaction rolls back, bookcase and shelves 1-2 are NOT saved ✅

### Testing

**Manual Test:**
1. Trigger shelf creation that will fail (e.g., duplicate shelf position with unique constraint)
2. Check database after failure
3. Bookcase should NOT exist (rollback worked)

**Integration Test:**
```java
@SpringBootTest
@Transactional
class BookcaseServiceTest {
    @Autowired
    private BookcaseService bookcaseService;

    @Autowired
    private BookcaseRepository bookcaseRepository;

    @Test
    void testCreateNewBookCase_RollbackOnFailure() {
        // This test would need to simulate a failure during shelf creation
        // Verify that the bookcase is NOT in the database after rollback
    }
}
```

### Commit Message
```
Fix: Add @Transactional to createNewBookCase for atomicity

- Wrap bookcase + shelf creation in single transaction
- Prevents partial data on failure (bookcase with incomplete shelves)
- All-or-nothing: either all shelves save or entire operation rolls back

Resolves critical data corruption bug from Section 29 (Transaction Management).
```

---

## Exercise 3: Fix Unsafe Optional.get() in BookCommands.searchByTitle()

**Difficulty:** 🟢 Beginner (20 minutes)
**Section Reference:** 15 (Optional Best Practices)
**File:** `src/main/java/com/penrose/bibby/cli/BookCommands.java:361-363`

### Problem

The code calls `.get()` on `Optional<ShelfEntity>` and `Optional<BookcaseEntity>` **without checking if they're present**, causing `NoSuchElementException` if the shelf or bookcase doesn't exist.

**Current Code:**
```java
Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
System.out.println("\nBook Was Found \nBookcase: " + bookcaseEntity.get().getBookcaseLabel() + "\nShelf: " + shelfEntity.get().getShelfLabel() + "\n");
```

**What Breaks:**
- If `shelfEntity` is empty → `.get()` crashes on line 362
- If `bookcaseEntity` is empty → `.get()` crashes on line 363

### Learning Objectives
- Replace unsafe `.get()` with `.orElseThrow()`
- Practice custom exception messages
- Chain Optional operations safely

### Your Task

Refactor to use `.orElseThrow()` instead of `.get()`.

### Solution

**Step 1:** Replace first `.get()` with `.orElseThrow()`
```java
ShelfEntity shelf = shelfService.findShelfById(bookEntity.getShelfId())
    .orElseThrow(() -> new EntityNotFoundException("Shelf not found: " + bookEntity.getShelfId()));
```

**Step 2:** Replace second `.get()` with `.orElseThrow()`
```java
BookcaseEntity bookcase = bookcaseService.findBookCaseById(shelf.getBookcaseId())
    .orElseThrow(() -> new EntityNotFoundException("Bookcase not found: " + shelf.getBookcaseId()));
```

**Step 3:** Use the unwrapped objects
```java
System.out.println("\nBook Was Found \nBookcase: " + bookcase.getBookcaseLabel() + "\nShelf: " + shelf.getShelfLabel() + "\n");
```

**Full Corrected Code:**
```java
ShelfEntity shelf = shelfService.findShelfById(bookEntity.getShelfId())
    .orElseThrow(() -> new EntityNotFoundException("Shelf not found: " + bookEntity.getShelfId()));

BookcaseEntity bookcase = bookcaseService.findBookCaseById(shelf.getBookcaseId())
    .orElseThrow(() -> new EntityNotFoundException("Bookcase not found: " + shelf.getBookcaseId()));

System.out.println("\nBook Was Found \nBookcase: " + bookcase.getBookcaseLabel() + "\nShelf: " + shelf.getShelfLabel() + "\n");
```

**Alternative: flatMap Chaining (More Advanced)**
```java
Optional<String> bookcaseLabel = shelfService.findShelfById(bookEntity.getShelfId())
    .flatMap(shelf -> bookcaseService.findBookCaseById(shelf.getBookcaseId()))
    .map(BookcaseEntity::getBookcaseLabel);

bookcaseLabel.ifPresentOrElse(
    label -> System.out.println("Bookcase: " + label),
    () -> System.out.println("Shelf or bookcase not found")
);
```

### Testing

**Manual Test:**
1. Create a book without assigning it to a shelf (shelfId = null)
2. Search for that book
3. Should show clear error message instead of crashing

### Commit Message
```
Fix: Replace unsafe Optional.get() with orElseThrow in searchByTitle

- Replace shelfEntity.get() with orElseThrow + descriptive error
- Replace bookcaseEntity.get() with orElseThrow + descriptive error
- Prevents NoSuchElementException crash when shelf/bookcase not found

Resolves crash risk from Section 15 (Optional Best Practices).
```

**Repeat this pattern for the other 10 unsafe `.get()` calls identified in Section 31!**

---

## Exercise 4: Add equals() and hashCode() to BookEntity

**Difficulty:** 🟢 Beginner (30 minutes)
**Section Reference:** 26 (Entity Design & JPA)
**File:** `src/main/java/com/penrose/bibby/library/book/BookEntity.java`

### Problem

`BookEntity` doesn't override `equals()` and `hashCode()`, breaking `HashSet` and `HashMap` behavior. Two books with the same ISBN are considered different objects.

**Current Code:**
```java
@Entity
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookId;

    private String isbn;
    private String title;
    // ... other fields

    // ❌ No equals() or hashCode()
}
```

**What Breaks:**
```java
BookEntity book1 = new BookEntity("Clean Code", "978-0132350884");
BookEntity book2 = new BookEntity("Clean Code", "978-0132350884");

Set<BookEntity> books = new HashSet<>();
books.add(book1);
books.add(book2);

System.out.println(books.size());  // Expected: 1 (same ISBN), Actual: 2 ❌
```

### Learning Objectives
- Understand business key pattern (use ISBN, not database ID)
- Practice implementing equals() and hashCode()
- Use `Objects.equals()` and `Objects.hash()` utilities

### Your Task

Add `equals()` and `hashCode()` using `isbn` as the business key.

### Solution

**Step 1:** Add equals() method
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;  // Same reference
    if (!(o instanceof BookEntity that)) return false;  // Type check + cast (Java 16+)
    return Objects.equals(isbn, that.isbn);  // Compare business key
}
```

**Step 2:** Add hashCode() method
```java
@Override
public int hashCode() {
    return Objects.hash(isbn);  // Hash the business key
}
```

**Full Addition to BookEntity:**
```java
import java.util.Objects;

@Entity
public class BookEntity {
    // ... existing fields

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookEntity that)) return false;
        return Objects.equals(isbn, that.isbn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }
}
```

**Why Use isbn Instead of bookId?**
- `bookId` is `null` before saving → unstable hash code
- `isbn` is set at construction → stable across save operations
- `isbn` is the real-world unique identifier (business key)

### Testing

**Unit Test:**
```java
@Test
void testEquals_SameIsbn() {
    BookEntity book1 = new BookEntity("Clean Code", "978-0132350884");
    BookEntity book2 = new BookEntity("Clean Code", "978-0132350884");

    assertThat(book1).isEqualTo(book2);
}

@Test
void testEquals_DifferentIsbn() {
    BookEntity book1 = new BookEntity("Clean Code", "978-0132350884");
    BookEntity book2 = new BookEntity("Refactoring", "978-0134757599");

    assertThat(book1).isNotEqualTo(book2);
}

@Test
void testHashSet_NoDuplicates() {
    BookEntity book1 = new BookEntity("Clean Code", "978-0132350884");
    BookEntity book2 = new BookEntity("Clean Code", "978-0132350884");

    Set<BookEntity> books = new HashSet<>();
    books.add(book1);
    books.add(book2);

    assertThat(books).hasSize(1);  // ✅ No duplicates
}
```

### Commit Message
```
Add equals/hashCode to BookEntity using ISBN business key

- Override equals() comparing isbn field
- Override hashCode() using Objects.hash(isbn)
- Fixes HashSet duplicates and HashMap failures
- Uses business key instead of database ID for stability

Resolves entity comparison issue from Section 26 (Entity Design).
```

**Repeat for:** `AuthorEntity` (use `firstName + lastName`), `ShelfEntity`, `BookcaseEntity`

---

## Exercise 5: Remove System.out.println and Add SLF4J Logging

**Difficulty:** 🟢 Beginner (20 minutes)
**Section Reference:** 20 (Logging Strategy)
**File:** `src/main/java/com/penrose/bibby/library/book/BookService.java:47`

### Problem

`BookService.findBookByTitle()` uses `System.out.println()` for debugging instead of proper logging.

**Current Code:**
```java
public BookEntity findBookByTitle(String title) {
    Optional<BookEntity> bookEntity = bookRepository.findByTitle(title);

    System.out.println(title);  // ❌ Debug print

    if (bookEntity.isEmpty()) {
        return null;
    } else {
        return bookEntity.get();
    }
}
```

### Learning Objectives
- Replace `System.out.println` with SLF4J logger
- Use parameterized logging (avoid string concatenation)
- Choose appropriate log level (DEBUG vs INFO)

### Your Task

Replace the debug print with proper SLF4J logging.

### Solution

**Step 1:** Add SLF4J logger field at top of class
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BookService {
    private static final Logger log = LoggerFactory.getLogger(BookService.class);
```

**Step 2:** Replace System.out.println
```java
public BookEntity findBookByTitle(String title) {
    log.debug("Searching for book with title: {}", title);  // ✅ Parameterized logging

    Optional<BookEntity> bookEntity = bookRepository.findByTitle(title);

    if (bookEntity.isEmpty()) {
        log.debug("Book not found: {}", title);
        return null;
    } else {
        log.debug("Book found: {} (ID: {})", title, bookEntity.get().getBookId());
        return bookEntity.get();
    }
}
```

**Why DEBUG instead of INFO?**
- `DEBUG`: Diagnostic details for developers (search operations)
- `INFO`: Important business events (book created, checked out)

**Better Version with INFO for Business Events:**
```java
public BookEntity findBookByTitle(String title) {
    log.debug("Searching for book with title: {}", title);

    Optional<BookEntity> bookEntity = bookRepository.findByTitle(title);

    if (bookEntity.isEmpty()) {
        log.info("Book lookup failed: title='{}' not found", title);
        return null;
    }

    log.info("Book lookup successful: title='{}', id={}", title, bookEntity.get().getBookId());
    return bookEntity.get();
}
```

### Testing

**Enable Logging:**
In `src/main/resources/application.properties`:
```properties
logging.level.com.penrose.bibby.library.book=DEBUG
```

**Run the application and search for a book:**
```
2025-11-18 10:30:15.123 DEBUG 12345 --- [main] c.p.b.l.b.BookService : Searching for book with title: Clean Code
2025-11-18 10:30:15.234 INFO  12345 --- [main] c.p.b.l.b.BookService : Book lookup successful: title='Clean Code', id=42
```

### Commit Message
```
Replace System.out.println with SLF4J logging in BookService

- Add SLF4J logger field to BookService
- Replace debug print with log.debug() for search operation
- Add log.info() for search results (found/not found)
- Use parameterized logging to avoid string concatenation

Resolves logging gap from Section 20 (Logging Strategy).
```

**Repeat for:** All `System.out.println` statements in services and controllers

---

# 🟡 Intermediate Exercises (Moderate Refactoring)

## Exercise 6: Write First Unit Test for BookService.createNewBook()

**Difficulty:** 🟡 Intermediate (1.5 hours)
**Section Reference:** 17 (JUnit), 18 (Mockito)
**File:** Create `src/test/java/com/penrose/bibby/library/book/BookServiceTest.java`

### Problem

Bibby has **0% test coverage**. `BookService.createNewBook()` is a critical method that saves both an author and a book — it needs tests.

**Current Production Code:**
```java
@Service
public class BookService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    @Transactional
    public void createNewBook(BookRequestDTO bookRequestDTO) {
        AuthorEntity authorEntity = new AuthorEntity();
        authorEntity.setFirstName(bookRequestDTO.firstName());
        authorEntity.setLastName(bookRequestDTO.lastName());
        authorRepository.save(authorEntity);

        BookEntity bookEntity = new BookEntity();
        bookEntity.setTitle(bookRequestDTO.title());
        bookEntity.setAuthors(Set.of(authorEntity));
        bookRepository.save(bookEntity);
    }
}
```

### Learning Objectives
- Set up JUnit 5 test class with Mockito
- Use `@Mock` and `@InjectMocks` annotations
- Write AAA pattern tests (Arrange, Act, Assert)
- Verify repository interactions with `verify()`

### Your Task

Create `BookServiceTest` with tests for `createNewBook()`.

### Solution

**Step 1:** Create test class structure
```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookService bookService;

    // Tests go here
}
```

**Step 2:** Write test for successful creation
```java
@Test
void testCreateNewBook_Success() {
    // Arrange
    BookRequestDTO request = new BookRequestDTO(
        "Clean Code",
        "Robert",
        "Martin"
    );

    // Act
    bookService.createNewBook(request);

    // Assert
    verify(authorRepository).save(any(AuthorEntity.class));
    verify(bookRepository).save(any(BookEntity.class));
}
```

**Step 3:** Write test verifying author details
```java
@Test
void testCreateNewBook_AuthorFieldsSetCorrectly() {
    // Arrange
    BookRequestDTO request = new BookRequestDTO(
        "Clean Code",
        "Robert",
        "Martin"
    );

    // Act
    bookService.createNewBook(request);

    // Assert - capture the author that was saved
    ArgumentCaptor<AuthorEntity> authorCaptor = ArgumentCaptor.forClass(AuthorEntity.class);
    verify(authorRepository).save(authorCaptor.capture());

    AuthorEntity savedAuthor = authorCaptor.getValue();
    assertThat(savedAuthor.getFirstName()).isEqualTo("Robert");
    assertThat(savedAuthor.getLastName()).isEqualTo("Martin");
}
```

**Step 4:** Write test verifying book details
```java
@Test
void testCreateNewBook_BookFieldsSetCorrectly() {
    // Arrange
    BookRequestDTO request = new BookRequestDTO(
        "Clean Code",
        "Robert",
        "Martin"
    );

    // Act
    bookService.createNewBook(request);

    // Assert - capture the book that was saved
    ArgumentCaptor<BookEntity> bookCaptor = ArgumentCaptor.forClass(BookEntity.class);
    verify(bookRepository).save(bookCaptor.capture());

    BookEntity savedBook = bookCaptor.getValue();
    assertThat(savedBook.getTitle()).isEqualTo("Clean Code");
    assertThat(savedBook.getAuthors()).hasSize(1);
}
```

**Step 5:** Run the tests
```bash
mvn test
```

### Testing

You're writing tests, so this is meta! But verify:
- All 3 tests pass ✅
- Code coverage shows `createNewBook()` is now covered
- `mvn test` output shows green checkmarks

### Commit Message
```
Add unit tests for BookService.createNewBook()

- Create BookServiceTest with Mockito setup
- Test successful book creation (author + book saved)
- Test author fields set correctly from DTO
- Test book fields set correctly and author linked
- Use ArgumentCaptor for detailed verification

First tests in codebase! Addresses Section 17 (JUnit & Testing Strategy).
Coverage: 0% → ~15% for BookService.
```

---

## Exercise 7: Add Bean Validation to CreateBookRequest

**Difficulty:** 🟡 Intermediate (1 hour)
**Section Reference:** 28 (Validation)
**Files:** Create `CreateBookRequest.java`, modify `BookController.java`, add dependency to `pom.xml`

### Problem

Bibby has **no validation**. The API accepts empty strings, null values, and invalid data.

**Current State:**
```java
// This is accepted with no errors:
{
  "title": "",
  "firstName": "",
  "lastName": ""
}
```

### Learning Objectives
- Add `spring-boot-starter-validation` dependency
- Create validated DTO with constraint annotations
- Enable validation in controller with `@Valid`
- Create global exception handler for validation errors

### Your Task

Add comprehensive validation to book creation.

### Solution

**Step 1:** Add dependency to `pom.xml`
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**Step 2:** Create validated DTO
```java
package com.penrose.bibby.library.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBookRequest(
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    String title,

    @NotBlank(message = "Author first name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    String firstName,

    @NotBlank(message = "Author last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    String lastName
) {}
```

**Step 3:** Enable validation in controller
```java
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    @PostMapping
    public ResponseEntity<Void> createBook(@Valid @RequestBody CreateBookRequest request) {
        // ↑ @Valid triggers validation
        bookService.createNewBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
```

**Step 4:** Create global exception handler
```java
package com.penrose.bibby.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("errors", errors);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
}
```

### Testing

**Manual Test with curl:**
```bash
# Invalid request (empty title)
curl -X POST http://localhost:8080/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{"title":"","firstName":"Robert","lastName":"Martin"}'

# Expected response:
{
  "status": "error",
  "errors": {
    "title": "Title is required"
  }
}
```

**Unit Test:**
```java
@WebMvcTest(BookController.class)
class BookControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Test
    void testCreateBook_InvalidRequest_BlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "",
                        "firstName": "Robert",
                        "lastName": "Martin"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.title").value("Title is required"));

        verifyNoInteractions(bookService);  // Should not call service
    }
}
```

### Commit Message
```
Add Bean Validation to book creation endpoint

- Add spring-boot-starter-validation dependency
- Create CreateBookRequest DTO with @NotBlank and @Size constraints
- Enable validation in BookController with @Valid
- Create GlobalExceptionHandler for clean error responses
- Add unit test for validation failure

Prevents invalid data from entering the system.
Addresses Section 28 (Validation).
```

---

## Exercise 8: Refactor BookService to Return DTOs Instead of Entities

**Difficulty:** 🟡 Intermediate (2 hours)
**Section Reference:** 27 (DTO Pattern & Layer Boundaries)
**Files:** Create `BookResponse.java`, modify `BookService.java`, modify `BookController.java`

### Problem

`BookService` returns `BookEntity` directly, exposing JPA implementation details to the controller layer (tight coupling).

**Current Code:**
```java
@Service
public class BookService {
    public BookEntity findBookByTitle(String title) {  // ❌ Returns entity
        return bookRepository.findByTitle(title).orElse(null);
    }
}

@RestController
public class BookController {
    public void search(@RequestBody BookRequestDTO request) {
        BookEntity book = bookService.findBookByTitle(request.title());  // ❌ Controller sees entity
        System.out.println(book.getTitle());
    }
}
```

### Learning Objectives
- Create response DTOs
- Map entities to DTOs in service layer
- Practice layer separation (controllers only see DTOs)
- Use records for immutable DTOs

### Your Task

Create `BookResponse` DTO and refactor service to return it.

### Solution

**Step 1:** Create response DTO
```java
package com.penrose.bibby.library.book;

import java.util.List;

public record BookResponse(
    Long id,
    String title,
    String isbn,
    List<String> authorNames,
    String shelfLabel,
    String bookcaseLabel,
    String status
) {}
```

**Step 2:** Add mapping method to service
```java
@Service
public class BookService {

    private BookResponse mapToResponse(BookEntity entity) {
        // Get author names
        List<String> authorNames = entity.getAuthors().stream()
            .map(author -> author.getFirstName() + " " + author.getLastName())
            .toList();

        // Get shelf/bookcase labels (simplified - you'd use proper joins)
        String shelfLabel = entity.getShelfId() != null
            ? shelfService.findShelfById(entity.getShelfId())
                .map(ShelfEntity::getShelfLabel)
                .orElse("Unassigned")
            : "Unassigned";

        String bookcaseLabel = "TBD";  // Would need to fetch through shelf

        return new BookResponse(
            entity.getBookId(),
            entity.getTitle(),
            entity.getIsbn(),
            authorNames,
            shelfLabel,
            bookcaseLabel,
            entity.getBookStatus()
        );
    }
}
```

**Step 3:** Refactor service method to return DTO
```java
@Service
public class BookService {

    public BookResponse findBookByTitle(String title) {  // ✅ Returns DTO
        log.debug("Searching for book with title: {}", title);

        BookEntity entity = bookRepository.findByTitle(title)
            .orElseThrow(() -> new EntityNotFoundException("Book not found: " + title));

        return mapToResponse(entity);  // ✅ Map to DTO
    }
}
```

**Step 4:** Update controller to use DTO
```java
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    @GetMapping("/search")
    public ResponseEntity<BookResponse> search(@RequestParam String title) {  // ✅ Returns DTO
        BookResponse book = bookService.findBookByTitle(title);  // ✅ Receives DTO
        return ResponseEntity.ok(book);
    }
}
```

### Testing

**Manual Test:**
```bash
curl http://localhost:8080/api/v1/books/search?title=Clean%20Code

# Expected response:
{
  "id": 1,
  "title": "Clean Code",
  "isbn": "978-0132350884",
  "authorNames": ["Robert Martin"],
  "shelfLabel": "A-12",
  "bookcaseLabel": "Programming",
  "status": "AVAILABLE"
}
```

**Unit Test:**
```java
@Test
void testFindBookByTitle_ReturnsDTO() {
    // Arrange
    BookEntity entity = new BookEntity("Clean Code", "978-0132350884");
    entity.setBookId(1L);
    when(bookRepository.findByTitle("Clean Code")).thenReturn(Optional.of(entity));

    // Act
    BookResponse response = bookService.findBookByTitle("Clean Code");

    // Assert
    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.title()).isEqualTo("Clean Code");
    assertThat(response.isbn()).isEqualTo("978-0132350884");
}
```

### Commit Message
```
Refactor BookService to return DTOs instead of entities

- Create BookResponse DTO with all display fields
- Add mapToResponse() method to convert entity to DTO
- Change findBookByTitle() to return BookResponse
- Update BookController to work with DTOs only
- Controllers no longer depend on JPA entities

Addresses layer separation from Section 27 (DTO Pattern).
```

**Repeat for:** All service methods that currently return entities

---

## Exercise 9: Delete Duplicate Classes (Book.java, Shelf.java, Bookcase.java, Genre.java)

**Difficulty:** 🟡 Intermediate (1 hour)
**Section Reference:** 27 (DTO Pattern & Layer Boundaries)
**Files:** Delete 4 duplicate classes, fix any broken references

### Problem

Bibby has **duplicate classes** that create confusion:
- `Book.java` (181 lines) duplicates `BookEntity.java`
- `Shelf.java` duplicates `ShelfEntity.java`
- `Bookcase.java` duplicates `BookcaseEntity.java`
- `Genre.java` is unused

**Worse:** `Book.java` mixes concerns (references `AuthorEntity` directly!)

### Learning Objectives
- Clean up dead/duplicate code
- Verify what code is actually used
- Practice safe deletion (check references first)

### Your Task

Delete the 4 duplicate classes after verifying they're not used.

### Solution

**Step 1:** Search for usages of each class

```bash
# Check if Book.java is referenced anywhere
grep -r "import.*Book;" src/ | grep -v "BookEntity" | grep -v "BookRepository" | grep -v "BookService"

# Check Shelf.java
grep -r "import.*Shelf;" src/ | grep -v "ShelfEntity"

# Check Bookcase.java
grep -r "import.*Bookcase;" src/ | grep -v "BookcaseEntity"

# Check Genre.java
grep -r "import.*Genre;" src/
```

**Step 2:** If no usages found, delete the files
```bash
rm src/main/java/com/penrose/bibby/library/book/Book.java
rm src/main/java/com/penrose/bibby/library/shelf/Shelf.java
rm src/main/java/com/penrose/bibby/library/bookcase/Bookcase.java
rm src/main/java/com/penrose/bibby/library/genre/Genre.java
```

**Step 3:** Compile to verify nothing broke
```bash
mvn clean compile
```

**Step 4:** Run tests to verify nothing broke
```bash
mvn test
```

### Testing

If compilation and tests pass, you've successfully deleted dead code!

### Commit Message
```
Remove duplicate classes (Book, Shelf, Bookcase, Genre)

- Delete Book.java (181 lines) - duplicates BookEntity
- Delete Shelf.java - duplicates ShelfEntity
- Delete Bookcase.java - duplicates BookcaseEntity
- Delete Genre.java - unused

These classes created confusion between DTOs and entities.
All references now use proper entity classes.

Addresses code organization from Section 27 (DTO Pattern).
```

---

## Exercise 10: Fix N+1 Query in BookcaseCommands.bookCaseOptions()

**Difficulty:** 🟡 Intermediate (2 hours)
**Section Reference:** 30 (Spring Shell Commands), 25 (Spring Data JPA)
**Files:** Create `BookcaseSummary.java`, modify `BookcaseRepository.java`, modify `BookcaseCommands.java`

### Problem

`bookCaseOptions()` has a **nested loop** that queries the database 61 times for 10 bookcases:

**Current Code:**
```java
private Map<String, String> bookCaseOptions() {
    List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases();  // 1 query

    for (BookcaseEntity b : bookcaseEntities) {  // For each bookcase...
        List<ShelfEntity> shelves = shelfService.findByBookcaseId(b.getBookcaseId());  // 10 queries

        for (ShelfEntity s : shelves) {  // For each shelf...
            List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId());  // 50 queries
            shelfBookCount += bookList.size();
        }
    }
}
```

**Total:** 1 + 10 + 50 = **61 queries** for a single menu!

### Learning Objectives
- Recognize N+1 query problems
- Create projection DTOs with aggregate queries
- Use JOINs and GROUP BY for efficient counts
- Replace loops with single database query

### Your Task

Create `BookcaseSummary` projection and single aggregate query.

### Solution

**Step 1:** Create projection record
```java
package com.penrose.bibby.library.bookcase;

public record BookcaseSummary(
    Long bookcaseId,
    String bookcaseLabel,
    int shelfCapacity,
    long bookCount
) {}
```

**Step 2:** Add repository query with JOINs
```java
@Repository
public interface BookcaseRepository extends JpaRepository<BookcaseEntity, Long> {

    @Query("""
        SELECT new com.penrose.bibby.library.bookcase.BookcaseSummary(
            bc.bookcaseId,
            bc.bookcaseLabel,
            bc.shelfCapacity,
            COUNT(b.bookId)
        )
        FROM BookcaseEntity bc
        LEFT JOIN ShelfEntity s ON s.bookcaseId = bc.bookcaseId
        LEFT JOIN BookEntity b ON b.shelfId = s.shelfId
        GROUP BY bc.bookcaseId, bc.bookcaseLabel, bc.shelfCapacity
        ORDER BY bc.bookcaseLabel ASC
        """)
    List<BookcaseSummary> findBookcaseSummaries();
}
```

**Step 3:** Add service method
```java
@Service
public class BookcaseService {

    public List<BookcaseSummary> getBookcaseSummaries() {
        return bookcaseRepository.findBookcaseSummaries();
    }
}
```

**Step 4:** Refactor BookcaseCommands to use projection
```java
private Map<String, String> bookCaseOptions() {
    List<BookcaseSummary> summaries = bookcaseService.getBookcaseSummaries();  // 1 query!

    return summaries.stream()
        .collect(Collectors.toMap(
            s -> bookcaseRowFormater(s.bookcaseLabel(), s.shelfCapacity(), (int) s.bookCount()),
            s -> s.bookcaseId().toString(),
            (a, b) -> a,
            LinkedHashMap::new
        ));
}
```

**Step 5:** Update helper method signature
```java
public String bookcaseRowFormater(String label, int capacity, int bookCount) {
    return String.format(" %-12s \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mShelves    \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mBooks ",
        label.toUpperCase(), capacity, bookCount);
}
```

### Testing

**Before Fix:**
- Enable SQL logging: `logging.level.org.hibernate.SQL=DEBUG`
- Run the CLI command that shows bookcase menu
- Count queries in logs: Should see 61 queries

**After Fix:**
- Run same command
- Count queries: Should see **1 query** with JOINs and GROUP BY

**Performance Test:**
```java
@DataJpaTest
class BookcaseRepositoryTest {
    @Autowired
    private BookcaseRepository bookcaseRepository;

    @Test
    void testFindBookcaseSummaries() {
        List<BookcaseSummary> summaries = bookcaseRepository.findBookcaseSummaries();

        assertThat(summaries).isNotEmpty();
        assertThat(summaries.get(0).bookcaseLabel()).isNotNull();
        assertThat(summaries.get(0).bookCount()).isGreaterThanOrEqualTo(0);
    }
}
```

### Commit Message
```
Fix N+1 query bug in bookcase menu (61 queries → 1 query)

- Create BookcaseSummary projection with bookCount
- Add findBookcaseSummaries() query with LEFT JOINs and GROUP BY
- Refactor bookCaseOptions() to use projection instead of nested loops
- Performance: 61 database queries reduced to 1 aggregate query

Addresses critical performance issue from Section 30 (Spring Shell).
```

---

# 🔴 Advanced Exercises (Complex Refactoring)

## Exercise 11-15: Summary

The remaining advanced exercises are:

**Exercise 11:** Convert BookEntity.shelfId to @ManyToOne relationship (4 hours)
**Exercise 12:** Create comprehensive BookService test suite with 10 tests (3 hours)
**Exercise 13:** Add integration tests for ShelfRepository custom queries (2 hours)
**Exercise 14:** Implement custom @ValidISBN annotation with checksum (3 hours)
**Exercise 15:** Create full CI/CD test pipeline with 70% coverage (4 hours)

These exercises follow the same pattern:
- Problem statement with current code
- Learning objectives
- Step-by-step solution
- Testing guidance
- Commit message

---

## Summary & Next Steps

### What You've Practiced

**Beginner Exercises (1-5):**
✅ Constructor injection (AuthorService)
✅ Transaction management (@Transactional)
✅ Optional safety (orElseThrow)
✅ equals/hashCode (business key pattern)
✅ SLF4J logging (replace System.out)

**Intermediate Exercises (6-10):**
✅ Unit testing with Mockito (BookServiceTest)
✅ Bean Validation (@Valid, constraint annotations)
✅ DTO pattern (BookResponse, mapping)
✅ Code cleanup (delete duplicates)
✅ N+1 query optimization (projection)

**Advanced Exercises (11-15):**
- Entity relationships (@ManyToOne)
- Comprehensive test suites
- Integration testing
- Custom validators
- CI/CD pipeline

### Recommended Order

**Week 1 Priority:**
1. Exercise 1 (AuthorService injection) — **CRITICAL**
2. Exercise 2 (@Transactional) — **CRITICAL**
3. Exercise 6 (First unit test) — **START SAFETY NET**

**Week 2 Priority:**
4. Exercise 3 (Optional safety) — **HIGH**
5. Exercise 4 (equals/hashCode) — **HIGH**
6. Exercise 7 (Validation) — **HIGH**

**Week 3-4:**
7. Exercises 8-10 (DTOs, cleanup, performance)
8. Exercises 11-15 (Advanced topics)

### Progress Tracking

After each exercise:
- [ ] Mark exercise complete
- [ ] Run `mvn test` to verify nothing broke
- [ ] Commit with suggested message
- [ ] Update coverage: `mvn clean test jacoco:report`

### Final Goal

Complete all 15 exercises to achieve:
- ✅ 0 critical bugs
- ✅ 70%+ test coverage
- ✅ 100% validation coverage
- ✅ Clean layer separation
- ✅ Production-ready codebase

---

**Next:** Section 33 (Your Personalized Learning Roadmap) — 12-week plan, daily habits, resources, milestones.
