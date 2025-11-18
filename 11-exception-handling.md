# Section 11: Exception Handling

**Estimated Time:** 60 minutes
**Prerequisites:** Sections 1-10
**Complexity:** ⭐⭐⭐⭐ (Intermediate-Advanced)

---

## Learning Objectives

By the end of this section, you will:

1. ✅ Understand checked vs unchecked exceptions
2. ✅ Master `ResponseStatusException` usage in Spring Boot
3. ✅ Know when (and when NOT) to use try-catch
4. ✅ Create custom exception classes for Bibby
5. ✅ Understand exception hierarchy and inheritance
6. ✅ Handle transaction rollback on exceptions
7. ✅ Apply exception handling best practices
8. ✅ Use try-with-resources for automatic cleanup

---

## The Surprising Truth About Bibby's Exception Handling

**Let's start with a revelation:**

Your Bibby codebase has:
- ❌ **ZERO try-catch blocks**
- ❌ **ZERO custom exception classes**
- ✅ **ONE throw statement** (BookcaseService.java:29)
- ✅ **11 `throws InterruptedException` declarations**

**Is this a problem?**

**NO! This is actually CORRECT for a Spring Boot application.**

### Why So Few Exceptions?

**Spring Boot handles most exceptions automatically:**

1. ✅ **Database errors** - Spring Data JPA converts them to `DataAccessException`
2. ✅ **HTTP errors** - Spring MVC catches exceptions and returns proper status codes
3. ✅ **Validation errors** - Bean Validation framework handles them
4. ✅ **Transaction failures** - Spring rolls back automatically on unchecked exceptions

**You only need to handle exceptions when:**
- You have business logic that requires specific error handling
- You want to convert framework exceptions to domain exceptions
- You need to add context to an error
- You want custom error messages for users

---

## Exception Hierarchy in Java

```
                    Throwable
                   /         \
              Exception      Error
             /    |    \        \
  Checked: /     |     \         \
  IOException  SQLException ...   OutOfMemoryError
                |                 StackOverflowError
                |                 (DON'T CATCH!)
      RuntimeException
    (Unchecked Exceptions)
       /    |     \
      /     |      \
NullPointer Illegal  NoSuchElement
Exception   ArgumentEx Exception
```

### Checked vs Unchecked Exceptions

| Type | Must Declare? | When to Use | Examples |
|------|---------------|-------------|----------|
| **Checked** | YES (`throws` keyword) | Recoverable errors | `IOException`, `SQLException`, `InterruptedException` |
| **Unchecked** | NO | Programming errors | `NullPointerException`, `IllegalArgumentException`, `IllegalStateException` |
| **Error** | NO (Don't catch!) | JVM failures | `OutOfMemoryError`, `StackOverflowError` |

---

## The ONE Exception in Bibby: ResponseStatusException

### Current Code - BookcaseService.java:18-29

```java
@Service
public class BookcaseService {
    private static final Logger log = LoggerFactory.getLogger(BookcaseService.class);

    // ✅ Pre-created exception as a field
    private final ResponseStatusException existingRecordError =
        new ResponseStatusException(HttpStatus.CONFLICT, "Bookcase with the label already exist");
    //                              ↑                    ↑
    //                              HTTP 409             Error message

    public String createNewBookCase(String label, int capacity){
        BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);

        if(bookcaseEntity != null){
            log.error("Failed to save Record - Record already exist", existingRecordError);
            throw existingRecordError;  // ✅ Only throw in the codebase!
        }

        // ... create bookcase logic ...
    }
}
```

### What is `ResponseStatusException`?

**Spring Boot's built-in exception for HTTP errors.**

```java
public class ResponseStatusException extends NestedRuntimeException {
    private final HttpStatus status;  // HTTP status code
    private final String reason;      // Error message

    public ResponseStatusException(HttpStatus status, String reason) {
        super(null);
        this.status = status;
        this.reason = reason;
    }
}
```

**When thrown, Spring Boot automatically:**
1. Returns the specified HTTP status code (409 CONFLICT)
2. Includes the error message in the response
3. Logs the exception
4. Rolls back the transaction (if in a @Transactional method)

### How Spring Handles It

**Without any try-catch, Spring Boot does this:**

```
Client Request: POST /api/v1/bookcases
       ↓
BookCaseController.createBookCase(DTO)
       ↓
BookcaseService.createNewBookCase(label, capacity)
       ↓
if (duplicate) throw ResponseStatusException(409, "Already exists")
       ↓
Spring catches exception → HTTP Response:
{
    "status": 409,
    "error": "Conflict",
    "message": "Bookcase with the label already exist",
    "path": "/api/v1/bookcases"
}
```

### Common HTTP Status Codes

| Code | Name | Use Case | Example |
|------|------|----------|---------|
| 400 | Bad Request | Invalid input | `new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ISBN format")` |
| 404 | Not Found | Resource doesn't exist | `new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found")` |
| 409 | Conflict | Duplicate resource | `new ResponseStatusException(HttpStatus.CONFLICT, "Bookcase already exists")` ✅ |
| 500 | Internal Server Error | Unexpected error | `new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error")` |

---

## Issue #1: Pre-Creating Exception as Field (Anti-Pattern)

### Problem with Current Code

**BookcaseService.java:18**

```java
private final ResponseStatusException existingRecordError =
    new ResponseStatusException(HttpStatus.CONFLICT, "Bookcase with the label already exist");
```

**Why is this problematic?**

1. **Loses Stack Trace Context**

   Exception is created when the service is instantiated (startup), not when the error occurs.

   ```java
   // Stack trace shows:
   at BookcaseService.<init>(BookcaseService.java:18)  // ❌ Points to service creation
   // NOT:
   at BookcaseService.createNewBookCase(BookcaseService.java:29)  // Where it was thrown
   ```

2. **Same Exception Instance Reused**

   If two users trigger the error simultaneously, they get THE SAME exception object (shared state).

3. **No Dynamic Message**

   You can't include the duplicate label name in the error message.

### The Fix: Create Exception When Thrown

**BEFORE (Current):**

```java
@Service
public class BookcaseService {
    // ❌ Created once at startup
    private final ResponseStatusException existingRecordError =
        new ResponseStatusException(HttpStatus.CONFLICT, "Bookcase with the label already exist");

    public String createNewBookCase(String label, int capacity){
        BookcaseEntity existing = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);

        if(existing != null){
            throw existingRecordError;  // ❌ Reuses same instance
        }
        // ...
    }
}
```

**AFTER (Fixed):**

```java
@Service
public class BookcaseService {

    public String createNewBookCase(String label, int capacity){
        BookcaseEntity existing = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);

        if(existing != null){
            // ✅ Create exception at throw site
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Bookcase with label '" + label + "' already exists"  // ✅ Dynamic message!
            );
        }

        // ... create bookcase ...
        log.info("Created new bookcase: {}", label);
        return "Created New Bookcase " + label;
    }
}
```

**Benefits:**
- ✅ Stack trace points to actual error location
- ✅ Each exception is a new instance (thread-safe)
- ✅ Can include dynamic data in error message

---

## Checked Exceptions in Bibby: `InterruptedException`

### Current Code - Multiple CLI Commands

**BookCommands.java:89**

```java
@Command(command = "add", description = "Add new book")
public void addBook() throws InterruptedException {
    //                   ↑
    //                   Checked exception - MUST declare

    LoadingBar.showProgressBar("Adding book...", 10, 100);  // Calls Thread.sleep()
    // ...
}
```

**LoadingBar.java:5**

```java
public static void showProgressBar(String taskName, int totalSteps, int delayMs)
    throws InterruptedException {  // ↑ Must declare because Thread.sleep() throws it
    //     ↑ Checked exception

    System.out.println(taskName);
    for (int i = 0; i <= totalSteps; i++) {
        Thread.sleep(delayMs);  // ← Throws InterruptedException
        // ... update progress ...
    }
}
```

### Why `throws InterruptedException`?

**`Thread.sleep()` is a checked exception:**

```java
// Java API:
public static void sleep(long millis) throws InterruptedException {
    // If thread is interrupted while sleeping, throws InterruptedException
}
```

**You have two choices:**

#### Option 1: Declare `throws` (Your Current Approach)

```java
public void addBook() throws InterruptedException {  // ✅ Pass exception to caller
    LoadingBar.showProgressBar("Adding book...", 10, 100);
}
```

**Pros:**
- Simple, minimal code
- Caller can handle if needed

**Cons:**
- Pollutes method signatures
- Caller is forced to handle or re-declare

#### Option 2: Catch and Handle

```java
public void addBook() {  // ✅ No throws declaration
    try {
        LoadingBar.showProgressBar("Adding book...", 10, 100);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // Restore interrupt status
        log.warn("Progress bar interrupted", e);
        // Continue without progress bar
    }
}
```

**Pros:**
- Cleaner method signatures
- Handles interruption gracefully

**Cons:**
- More code
- Must decide how to handle interruption

**For CLI apps, your current approach (`throws`) is fine.**

---

## Creating Custom Exceptions

**Your Bibby codebase has NO custom exceptions.**

Let's create some domain-specific exceptions.

### Example 1: BookNotFoundException

**Create the exception:**

```java
package com.penrose.bibby.library.exception;

/**
 * Thrown when a book cannot be found by ID or title
 */
public class BookNotFoundException extends RuntimeException {
    //                                    ↑
    //                                    Extends RuntimeException (unchecked)

    private final Long bookId;
    private final String title;

    public BookNotFoundException(Long bookId) {
        super("Book not found with ID: " + bookId);
        this.bookId = bookId;
        this.title = null;
    }

    public BookNotFoundException(String title) {
        super("Book not found with title: " + title);
        this.bookId = null;
        this.title = title;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }
}
```

**Use it in BookService:**

```java
@Service
public class BookService {

    public BookEntity findBookByTitle(String title){
        Optional<BookEntity> bookEntity = Optional.ofNullable(
            bookRepository.findByTitleIgnoreCase(title)
        );

        // BEFORE:
        if(bookEntity.isEmpty()){
            return null;  // ❌ Caller has to null-check
        }
        return bookEntity.get();

        // AFTER:
        return bookEntity.orElseThrow(() ->
            new BookNotFoundException(title)  // ✅ Explicit error
        );
    }
}
```

### Example 2: DuplicateBookcaseException

**Create the exception:**

```java
package com.penrose.bibby.library.exception;

/**
 * Thrown when attempting to create a bookcase with a duplicate label
 */
public class DuplicateBookcaseException extends RuntimeException {

    private final String label;

    public DuplicateBookcaseException(String label) {
        super("Bookcase with label '" + label + "' already exists");
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
```

**Use it in BookcaseService:**

```java
public String createNewBookCase(String label, int capacity){
    BookcaseEntity existing = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);

    if(existing != null){
        // BEFORE:
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Already exists");

        // AFTER:
        throw new DuplicateBookcaseException(label);  // ✅ Domain-specific!
    }

    // ... create bookcase ...
}
```

**Then Spring Boot handles it:**

```java
// Add a global exception handler (optional):
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateBookcaseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateBookcase(DuplicateBookcaseException ex) {
        return new ErrorResponse(
            "DUPLICATE_BOOKCASE",
            ex.getMessage(),
            ex.getLabel()
        );
    }
}
```

---

## When to Use Checked vs Unchecked Exceptions

### Use Checked Exceptions (extends `Exception`) When:

✅ **Caller can reasonably recover**

```java
public class BookImportService {

    public void importBook(File csvFile) throws IOException {  // Checked
        // Caller can catch and show user: "File not found, please select another"
        BufferedReader reader = new BufferedReader(new FileReader(csvFile));
        // ...
    }
}
```

### Use Unchecked Exceptions (extends `RuntimeException`) When:

✅ **Programming error (caller's fault)**

```java
public void addBook(BookEntity book) {
    if (book == null) {
        throw new IllegalArgumentException("Book cannot be null");  // Unchecked
        // Caller should NOT pass null - this is a bug
    }
}
```

✅ **Business rule violation**

```java
public void checkOutBook(Long bookId) {
    BookEntity book = findBookOrThrow(bookId);

    if (book.getStatus() == BookStatus.CHECKED_OUT) {
        throw new BookAlreadyCheckedOutException(bookId);  // Unchecked
        // This is a business rule violation, not a programming error
    }

    book.setStatus(BookStatus.CHECKED_OUT);
    bookRepository.save(book);
}
```

### Guideline: Prefer Unchecked Exceptions

**Modern Java best practice: Use unchecked exceptions for most cases.**

**Why?**
1. ✅ Cleaner code (no forced `throws` declarations)
2. ✅ Easier to refactor (changing exceptions doesn't break callers)
3. ✅ Spring Boot expects RuntimeExceptions for transaction rollback

**Checked exceptions are falling out of favor** - even Java's own creators admit they were a mistake for most use cases.

---

## Exception Handling Best Practices

### 1. Never Swallow Exceptions

```java
// ❌ BAD: Silent failure
try {
    bookService.deleteBook(id);
} catch (Exception e) {
    // Do nothing - error is lost!
}

// ✅ GOOD: Log at minimum
try {
    bookService.deleteBook(id);
} catch (Exception e) {
    log.error("Failed to delete book {}", id, e);
    throw e;  // Re-throw if you can't handle
}

// ✅ BETTER: Handle or wrap
try {
    bookService.deleteBook(id);
} catch (DataAccessException e) {
    throw new BookDeletionException("Cannot delete book " + id, e);
}
```

### 2. Don't Catch Generic `Exception`

```java
// ❌ BAD: Too broad
try {
    bookService.saveBook(book);
} catch (Exception e) {  // Catches EVERYTHING (even OutOfMemoryError!)
    log.error("Error", e);
}

// ✅ GOOD: Catch specific exceptions
try {
    bookService.saveBook(book);
} catch (DataIntegrityViolationException e) {  // Specific!
    throw new DuplicateBookException(book.getIsbn(), e);
} catch (DataAccessException e) {  // Database errors
    throw new BookPersistenceException("Failed to save book", e);
}
```

### 3. Include Context in Exception Messages

```java
// ❌ BAD: Vague message
throw new IllegalArgumentException("Invalid input");

// ✅ GOOD: Specific context
throw new IllegalArgumentException(
    "Invalid ISBN format: '" + isbn + "'. Expected format: XXX-X-XXXX-XXXX-X"
);

// ✅ BETTER: Custom exception with fields
throw new InvalidIsbnException(isbn, "Expected format: XXX-X-XXXX-XXXX-X");
```

### 4. Use try-with-resources for AutoCloseable

```java
// ❌ BAD: Manual resource management
BufferedReader reader = null;
try {
    reader = new BufferedReader(new FileReader("books.csv"));
    String line = reader.readLine();
    // ... process ...
} catch (IOException e) {
    log.error("Error reading file", e);
} finally {
    if (reader != null) {
        try {
            reader.close();  // ❌ Nested try-catch!
        } catch (IOException e) {
            log.error("Error closing reader", e);
        }
    }
}

// ✅ GOOD: try-with-resources (Java 7+)
try (BufferedReader reader = new BufferedReader(new FileReader("books.csv"))) {
    String line = reader.readLine();
    // ... process ...
    // ✅ Automatically closed, even if exception thrown!
} catch (IOException e) {
    log.error("Error reading file", e);
}
```

### 5. Don't Use Exceptions for Flow Control

```java
// ❌ BAD: Exception for normal flow
public BookEntity findBookOrDefault(Long id) {
    try {
        return bookRepository.findById(id).get();  // Throws if not found
    } catch (NoSuchElementException e) {
        return getDefaultBook();  // Using exception for control flow
    }
}

// ✅ GOOD: Use Optional properly
public BookEntity findBookOrDefault(Long id) {
    return bookRepository.findById(id)
        .orElseGet(this::getDefaultBook);  // No exception thrown
}
```

---

## Transaction Rollback on Exceptions

**Spring automatically rolls back transactions on unchecked exceptions.**

### Default Behavior

```java
@Service
public class BookService {

    @Transactional
    public void transferBook(Long bookId, Long fromShelfId, Long toShelfId) {
        BookEntity book = findBookOrThrow(bookId);

        book.setShelfId(toShelfId);  // Update 1
        bookRepository.save(book);

        shelfService.updateBookCount(fromShelfId, -1);  // Update 2
        shelfService.updateBookCount(toShelfId, +1);    // Update 3

        // If ANY unchecked exception is thrown, ALL updates are rolled back
        if (toShelfId == 999L) {
            throw new RuntimeException("Test rollback");  // ✅ Rolls back!
        }
    }
}
```

### Rollback Rules

| Exception Type | Rollback? | Why? |
|----------------|-----------|------|
| **RuntimeException** (unchecked) | ✅ YES | Spring's default behavior |
| **Exception** (checked) | ❌ NO | Assumed to be recoverable |
| **Error** | ✅ YES | JVM failure |

### Custom Rollback Behavior

```java
@Transactional(rollbackFor = Exception.class)  // Roll back on checked exceptions too
public void importBooks(File csvFile) throws IOException {
    // ... read file ...
    // If IOException is thrown, transaction rolls back
}

@Transactional(noRollbackFor = InvalidIsbnException.class)  // Don't roll back for this
public void saveBook(BookEntity book) {
    if (isInvalidIsbn(book.getIsbn())) {
        throw new InvalidIsbnException(book.getIsbn());  // Transaction NOT rolled back
    }
    bookRepository.save(book);
}
```

---

## Global Exception Handling with @ControllerAdvice

**Instead of try-catch in every controller, handle exceptions globally.**

### Create a Global Handler

```java
package com.penrose.bibby.config;

import com.penrose.bibby.library.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookNotFound(BookNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            "BOOK_NOT_FOUND",
            ex.getMessage(),
            ex.getBookId()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateBookcaseException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateBookcase(DuplicateBookcaseException ex) {
        ErrorResponse error = new ErrorResponse(
            "DUPLICATE_BOOKCASE",
            ex.getMessage(),
            ex.getLabel()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
            "INVALID_INPUT",
            ex.getMessage(),
            null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

### Error Response DTO

```java
package com.penrose.bibby.library.exception;

public record ErrorResponse(
    String errorCode,
    String message,
    Object details
) {}
```

**Now controllers never need try-catch:**

```java
@RestController
public class BookController {

    @GetMapping("/api/v1/books/{id}")
    public ResponseEntity<BookSummary> getBook(@PathVariable Long id) {
        // No try-catch needed!
        // If BookNotFoundException is thrown, GlobalExceptionHandler catches it
        BookEntity book = bookService.findBookOrThrow(id);
        return ResponseEntity.ok(toSummary(book));
    }
}
```

---

## Testing Your Understanding

### Question 1: Checked vs Unchecked

Which should each of these be?

1. `UserNotFoundException` → **Answer:** Unchecked (RuntimeException) - business rule
2. `InvalidPasswordException` → **Answer:** Unchecked - validation error
3. `FileImportException` → **Answer:** Could be checked (caller can retry with different file)

### Question 2: Fix the Anti-Pattern

What's wrong with this code?

```java
@Service
public class BookService {
    private final RuntimeException bookNotFoundError =
        new RuntimeException("Book not found");

    public BookEntity findBookOrThrow(Long id) {
        return bookRepository.findById(id)
            .orElseThrow(() -> bookNotFoundError);
    }
}
```

**Answer:**
- ❌ Exception created at service initialization (not at throw site)
- ❌ Same instance reused (loses stack trace context)
- ❌ No dynamic information (which book ID was not found?)

**Fix:**
```java
public BookEntity findBookOrThrow(Long id) {
    return bookRepository.findById(id)
        .orElseThrow(() -> new BookNotFoundException(id));  // ✅ Create new each time
}
```

### Question 3: Transaction Rollback

Will this transaction roll back?

```java
@Transactional
public void saveBook(BookEntity book) throws IOException {
    bookRepository.save(book);
    exportToFile(book);  // Throws IOException
}
```

**Answer:** NO - `IOException` is checked, and Spring doesn't roll back on checked exceptions by default.

**To fix:**
```java
@Transactional(rollbackFor = IOException.class)  // ✅ Now rolls back
```

---

## Action Items for Your Codebase

### Priority 1: Fix Anti-Pattern

- [ ] **Fix exception pre-creation** (BookcaseService.java:18)
  ```java
  // Remove:
  private final ResponseStatusException existingRecordError = ...;

  // Change throw to:
  throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "Bookcase with label '" + label + "' already exists"
  );
  ```

### Priority 2: Create Custom Exceptions (Optional but Recommended)

- [ ] **Create BookNotFoundException**
  ```java
  public class BookNotFoundException extends RuntimeException {
      private final Long bookId;
      public BookNotFoundException(Long bookId) {
          super("Book not found with ID: " + bookId);
          this.bookId = bookId;
      }
  }
  ```

- [ ] **Create DuplicateBookcaseException**
- [ ] **Use in services** instead of ResponseStatusException

### Priority 3: Global Exception Handler (Optional)

- [ ] **Create @RestControllerAdvice class**
- [ ] **Add exception handlers** for custom exceptions
- [ ] **Create ErrorResponse record**

---

## Key Concepts Recap

| Concept | What It Means | Example in Bibby |
|---------|---------------|------------------|
| **Checked Exception** | Must declare with `throws` | `InterruptedException` in CLI commands |
| **Unchecked Exception** | Don't need to declare | `ResponseStatusException` in BookcaseService |
| **Custom Exception** | Domain-specific error type | (None yet - opportunity!) |
| **Exception Hierarchy** | All exceptions extend Throwable | RuntimeException → ResponseStatusException |
| **try-with-resources** | Auto-close resources | (Not used yet, but should for file I/O) |
| **@ControllerAdvice** | Global exception handler | (Not used yet - Spring handles automatically) |
| **Transaction Rollback** | Undo on unchecked exceptions | Automatic in @Transactional methods |
| **ResponseStatusException** | Spring's HTTP exception | BookcaseService duplicate bookcase check |

---

## Summary

### What You Learned

1. ✅ **Minimal exceptions is normal** - Spring Boot handles most errors
2. ✅ **Checked vs unchecked** - Prefer unchecked (RuntimeException)
3. ✅ **ResponseStatusException** - Spring's built-in HTTP error handling
4. ✅ **Anti-pattern** - Don't pre-create exceptions as fields
5. ✅ **Custom exceptions** - Create domain-specific error types
6. ✅ **Transaction rollback** - Automatic on unchecked exceptions
7. ✅ **Best practices** - Never swallow, catch specific, include context
8. ✅ **Global handlers** - @ControllerAdvice for consistent error responses

### Current State of Bibby

✅ **Good:**
- Minimal exception handling (Spring handles it)
- Uses ResponseStatusException appropriately
- Declares `throws InterruptedException` correctly

❌ **Needs Improvement:**
- Exception pre-created as field (loses stack trace)
- No custom exceptions (uses generic ResponseStatusException)
- Returns null instead of throwing exceptions (BookService.findBookByTitle)

---

## What's Next?

In **Section 12: Memory Model Basics**, we'll examine:
- Stack vs heap memory in Java
- How object references work
- String interning and the String pool
- Garbage collection basics
- Memory implications of collections
- Immutability and memory efficiency

**Your Progress:**
- ✅ Sections 1-11 Complete (33% of mentorship guide)
- ⏳ 22 sections remaining

---

**Section 11 Complete! 🎉**

**Key takeaway:** Less exception handling is often better. Spring Boot does the heavy lifting - you just throw exceptions when business rules are violated.

**Ready for Section 12?** Reply **"yes"** or **"continue"**.
