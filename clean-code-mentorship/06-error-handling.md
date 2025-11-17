# SECTION 6: ERROR HANDLING
## Clean Code Chapter 7 Applied to Your Bibby Codebase

---

## Table of Contents
1. [Principles Overview](#principles-overview)
2. [Your Code Analysis](#your-code-analysis)
3. [Creating Custom Exceptions](#creating-custom-exceptions)
4. [Refactoring Examples](#refactoring-examples)
5. [Patterns You Should Recognize](#patterns-you-should-recognize)
6. [Action Items](#action-items)
7. [Key Takeaways](#key-takeaways)
8. [Further Study](#further-study)

---

## Principles Overview

### Error Handling is Important

Error handling is one of the most important aspects of software. Uncle Bob states:

> "Error handling is important, but if it obscures logic, it's wrong."

Good error handling:
- Makes failures explicit and visible
- Provides useful context about what went wrong
- Keeps the happy path readable
- Prevents programs from continuing in invalid states

Bad error handling:
- Obscures the main logic
- Returns error codes that must be checked
- Returns null, forcing null checks everywhere
- Swallows exceptions without proper handling

### Use Exceptions Rather Than Return Codes

**Bad (error codes):**
```java
public int deleteBook(Long id) {
    if (id == null) return ERROR_NULL_ID;
    if (!exists(id)) return ERROR_NOT_FOUND;
    if (isCheckedOut(id)) return ERROR_CHECKED_OUT;

    repository.delete(id);
    return SUCCESS;
}

// Caller must check
int result = deleteBook(id);
if (result == ERROR_NOT_FOUND) {
    // handle
} else if (result == ERROR_CHECKED_OUT) {
    // handle
}
```

**Good (exceptions):**
```java
public void deleteBook(Long id) {
    if (id == null) {
        throw new IllegalArgumentException("Book ID cannot be null");
    }

    Book book = repository.findById(id)
        .orElseThrow(() -> new BookNotFoundException(id));

    if (book.isCheckedOut()) {
        throw new BookCheckedOutException(
            "Cannot delete checked out book: " + book.getTitle()
        );
    }

    repository.delete(id);
}

// Caller's happy path is clean
deleteBook(id);
```

### Write Your Try-Catch-Finally Statement First

When writing code that might throw exceptions, write the try-catch-finally first. This defines the scope and helps you think about what could go wrong.

### Provide Context with Exceptions

Each exception should provide enough context to determine the source and location of an error. Create informative error messages and pass them along with your exceptions.

### Don't Return Null

**Uncle Bob's Rule:** Don't return null from methods. Don't pass null to methods.

Returning null causes:
- Null checks scattered everywhere
- NullPointerExceptions when checks are forgotten
- Obscured business logic

**Instead of returning null:**
1. Return `Optional<T>` for optional values
2. Return empty collections instead of null collections
3. Throw an exception if the absence is exceptional
4. Use the Special Case pattern (NullObject pattern)

### Don't Pass Null

Passing null to methods is even worse than returning it. Unless an API expects null, avoid passing it.

---

## Your Code Analysis

Let's examine the error handling in your Bibby codebase. I have some concerning news: **You have essentially NO error handling**.

### Issue #1: No Exception Handling At All

**Discovery:**

I searched your entire codebase for:
- `try {` - **0 results**
- `catch (` - **0 results**
- `throw new` - **0 results**

**Why This Hurts:**

Your application has ZERO exception handling. This means:
- Database errors crash the application
- Invalid input crashes the application
- Missing data causes NullPointerExceptions
- Users see stack traces instead of helpful messages
- No recovery from errors

**Critical Missing Areas:**

1. **Database operations** - What if the database is down?
2. **User input** - What if they enter invalid data?
3. **File I/O** - What if configuration files are missing?
4. **Network calls** - What if external services fail?
5. **Business rule violations** - What if they try to checkout an unavailable book?

---

### Issue #2: Returning Null Instead of Optional

**Location:** `BookService.java:43-54`

**Current Code:**
```java
public BookEntity findBookByTitle(String title){
    Optional<BookEntity> bookEntity = Optional.ofNullable(
        bookRepository.findByTitleIgnoreCase(title)
    );
    List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
    for(BookEntity b : bookEntities){
        System.out.println(b.getTitle());  // Side effect!
    }

    if(bookEntity.isEmpty()){
        return null;  // ❌ RETURNING NULL
    }
    return bookEntity.get();
}
```

**Why This Hurts:**

The method internally uses `Optional`, then throws it away and returns `null`. Now every caller must remember to check for null:

```java
BookEntity book = bookService.findBookByTitle(title);
if (book == null) {  // Easy to forget this check
    System.out.println("Not found");
} else {
    // use book
}
```

**How to Fix:**

```java
public Optional<BookEntity> findBookByTitle(String title){
    if (title == null || title.isBlank()) {
        throw new IllegalArgumentException("Title cannot be null or blank");
    }

    return Optional.ofNullable(
        bookRepository.findByTitleIgnoreCase(title)
    );
}

// Usage
bookService.findBookByTitle(title)
    .ifPresentOrElse(
        book -> displayBook(book),
        () -> displayNotFound()
    );
```

---

### Issue #3: Dangerous .get() on Optional Without Checking

**Location:** `BookCommands.java:362-363, 475-477, 551-553`

**Current Code:**
```java
Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(
    shelfEntity.get().getBookcaseId()  // ❌ DANGEROUS!
);
String bookcaseLabel = bookcaseEntity.get().getBookcaseLabel();  // ❌ DANGEROUS!
String bookshelfLabel = shelfEntity.get().getShelfLabel();  // ❌ DANGEROUS!
```

**Why This Hurts:**

Calling `.get()` on an `Optional` without first checking if it's present throws `NoSuchElementException` if the value is absent. This is exactly what `Optional` was designed to prevent!

**How to Fix:**

```java
Optional<ShelfEntity> shelf = shelfService.findShelfById(bookEntity.getShelfId());

String location = shelf
    .map(s -> s.getFullLocation())
    .orElse("No location assigned");

System.out.println("Location: " + location);
```

Or better, push this logic into the domain model:

```java
// In Book class
public String getLocationDescription() {
    if (shelf == null) {
        return "No location assigned";
    }
    return shelf.getFullLocation();
}

// Usage (no Optional handling needed in UI layer)
System.out.println("Location: " + book.getLocationDescription());
```

---

### Issue #4: Null Checks Scattered in UI Layer

**Location:** `BookCommands.java:243, 356, 471, 547`

**Current Code:**
```java
BookEntity bookEnt = bookService.findBookByTitle(title);
if(bookEnt == null){
    System.out.println("Book Not Found In Library");
}else {
    // ... 20 lines of logic
}
```

**Why This Hurts:**

1. The UI layer is doing error handling that should happen in the service layer
2. Null checks obscure the business logic
3. Each caller must remember to check for null
4. Error messages are inconsistent

**How to Fix - Fail Fast with Exceptions:**

```java
// Service layer
public Book findBookByTitleOrThrow(String title) {
    return findBookByTitle(title)
        .orElseThrow(() -> new BookNotFoundException(
            "No book found with title: " + title
        ));
}

// Command layer (clean!)
try {
    Book book = bookService.findBookByTitleOrThrow(title);
    displayBookDetails(book);
} catch (BookNotFoundException e) {
    displayError(e.getMessage());
}
```

---

### Issue #5: No Custom Exceptions

**Discovery:**

Your codebase has **zero custom exception classes**.

**Why This Hurts:**

Without custom exceptions:
- You can't differentiate between different error types
- Error handling is generic and not specific to your domain
- You can't provide domain-specific error messages
- You can't have different recovery strategies for different errors

**What's Missing:**

```
com.penrose.bibby.exception/
├── BookNotFoundException.java
├── BookAlreadyCheckedOutException.java
├── BookAlreadyExistsException.java
├── ShelfNotFoundException.java
├── ShelfFullException.java
├── BookcaseNotFoundException.java
└── InvalidBookStateException.java
```

---

### Issue #6: No Validation

**Location:** Service methods throughout

**Current State:**

Methods accept parameters without validation:

```java
public void createNewBook(BookRequestDTO bookRequestDTO) {
    String firstName = bookRequestDTO.firstName();  // What if null?
    String lastName = bookRequestDTO.lastName();    // What if null?
    String title = bookRequestDTO.title();         // What if null or blank?

    // No validation, just proceed
    BookEntity bookEntity = bookRepository.findByTitle(title);
    // ...
}
```

**How to Fix - Validate Input:**

```java
public void createNewBook(BookRequestDTO bookRequestDTO) {
    validateBookRequest(bookRequestDTO);

    String title = bookRequestDTO.title();
    String firstName = bookRequestDTO.firstName();
    String lastName = bookRequestDTO.lastName();

    // Rest of logic...
}

private void validateBookRequest(BookRequestDTO dto) {
    if (dto == null) {
        throw new IllegalArgumentException("Book request cannot be null");
    }
    if (dto.title() == null || dto.title().isBlank()) {
        throw new IllegalArgumentException("Book title is required");
    }
    if (dto.firstName() == null || dto.firstName().isBlank()) {
        throw new IllegalArgumentException("Author first name is required");
    }
    if (dto.lastName() == null || dto.lastName().isBlank()) {
        throw new IllegalArgumentException("Author last name is required");
    }
}
```

Or use Bean Validation (even better):

```java
public record BookRequestDTO(
    @NotBlank(message = "Book title is required")
    String title,

    @NotBlank(message = "Author first name is required")
    String firstName,

    @NotBlank(message = "Author last name is required")
    String lastName
) {}
```

---

### Issue #7: Silent Failures

**Location:** `BookMapper.java:10-12, 33-35`

**Current Code:**
```java
public static Book toDomain(BookEntity e, AuthorEntity authorEntity, Shelf shelf, Genre genre){
    if (e == null){
        return null;  // Silent failure
    }
    // ...
}

public static BookEntity toEntity(Book book){
    if (book == null){
        return null;  // Silent failure
    }
    // ...
}
```

**Why This Hurts:**

If someone passes null to these methods, they get null back. The error is silent and might manifest much later in the code, far from the source of the problem.

**How to Fix - Fail Fast:**

```java
public static Book toDomain(BookEntity entity, AuthorEntity author, Shelf shelf, Genre genre){
    Objects.requireNonNull(entity, "BookEntity cannot be null");
    Objects.requireNonNull(author, "Author cannot be null");

    Book book = new Book();
    book.setId(entity.getBookId());
    book.setTitle(entity.getTitle());
    book.setAuthor(author);
    // ... rest of mapping

    return book;
}
```

Or better, use a mapping library like MapStruct that handles nulls properly.

---

## Creating Custom Exceptions

Let's create a proper exception hierarchy for your domain.

### Base Exception

```java
package com.penrose.bibby.exception;

/**
 * Base exception for all Bibby application exceptions.
 */
public class BibbyException extends RuntimeException {

    public BibbyException(String message) {
        super(message);
    }

    public BibbyException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Domain-Specific Exceptions

```java
package com.penrose.bibby.exception;

/**
 * Thrown when a requested book cannot be found.
 */
public class BookNotFoundException extends BibbyException {

    public BookNotFoundException(String title) {
        super("Book not found: " + title);
    }

    public BookNotFoundException(Long id) {
        super("Book not found with ID: " + id);
    }
}
```

```java
package com.penrose.bibby.exception;

/**
 * Thrown when attempting to check out a book that is already checked out.
 */
public class BookAlreadyCheckedOutException extends BibbyException {

    public BookAlreadyCheckedOutException(String title) {
        super("Book '" + title + "' is already checked out");
    }
}
```

```java
package com.penrose.bibby.exception;

/**
 * Thrown when attempting to add a book that already exists.
 */
public class BookAlreadyExistsException extends BibbyException {

    public BookAlreadyExistsException(String title) {
        super("Book already exists with title: " + title);
    }
}
```

```java
package com.penrose.bibby.exception;

/**
 * Thrown when attempting to add a book to a shelf that is already full.
 */
public class ShelfFullException extends BibbyException {

    public ShelfFullException(String shelfLabel, int capacity) {
        super(String.format(
            "Shelf '%s' is full (capacity: %d books)",
            shelfLabel,
            capacity
        ));
    }
}
```

```java
package com.penrose.bibby.exception;

/**
 * Thrown when a book is in an invalid state for the requested operation.
 */
public class InvalidBookStateException extends BibbyException {

    public InvalidBookStateException(String message) {
        super(message);
    }
}
```

---

## Refactoring Examples

### Example 1: Service Method with Proper Error Handling

**Before:**
```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

**After:**
```java
public void checkOutBook(Long bookId) {
    Objects.requireNonNull(bookId, "Book ID cannot be null");

    Book book = bookRepository.findById(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    book.checkout();  // Domain model handles business logic and validation

    bookRepository.save(book);
}

// In Book class
public void checkout() {
    if (isCheckedOut()) {
        throw new BookAlreadyCheckedOutException(this.title);
    }

    if (!isAvailable()) {
        throw new InvalidBookStateException(
            "Cannot checkout book in status: " + this.status
        );
    }

    this.status = BookStatus.CHECKED_OUT;
    this.checkoutCount++;
    this.updatedAt = LocalDate.now();
}
```

---

### Example 2: Command with Exception Handling

**Before:**
```java
public void checkOutBook(){
    ComponentFlow flow;
    flow = componentFlowBuilder.clone()
            .withStringInput("bookTitle" )
            .name("Book Title:")
            .and().build();
    ComponentFlow.ComponentFlowResult res = flow.run();

    String bookTitle = res.getContext().get("bookTitle");

    BookEntity bookEntity = bookService.findBookByTitle(bookTitle);
    if(bookEntity == null){
        System.out.println("Book Not Found.");
    }else if(bookEntity.getBookStatus().equals("CHECKED_OUT")){
        System.out.println("Already checked out");
    } else {
        // ... more logic
    }
}
```

**After:**
```java
@Command(command = "check-out", description = "Check out a book from the library")
public void checkOutBook(){
    String bookTitle = promptForBookTitle();

    try {
        bookService.checkOutBookByTitle(bookTitle);
        displayCheckoutSuccess(bookTitle);

    } catch (BookNotFoundException e) {
        displayError("Book not found: " + bookTitle);

    } catch (BookAlreadyCheckedOutException e) {
        displayWarning(e.getMessage());

    } catch (Exception e) {
        logger.error("Unexpected error during checkout", e);
        displayError("An unexpected error occurred. Please try again.");
    }
}

private void displayError(String message) {
    System.out.println(ConsoleColors.red("Error: " + message));
}

private void displayWarning(String message) {
    System.out.println(ConsoleColors.yellow("Warning: " + message));
}

private void displayCheckoutSuccess(String title) {
    System.out.println(ConsoleColors.green(
        "✓ Successfully checked out: " + title
    ));
}
```

---

### Example 3: Handling Optional Properly

**Before:**
```java
Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(
    shelfEntity.get().getBookcaseId()  // CRASH if empty!
);
System.out.println("Bookcase: " + bookcaseEntity.get().getBookcaseLabel());
```

**After (Option 1 - Explicit handling):**
```java
Optional<ShelfEntity> shelf = shelfService.findShelfById(book.getShelfId());

String location = shelf
    .flatMap(s -> bookcaseService.findBookCaseById(s.getBookcaseId()))
    .map(bookcase -> formatLocation(shelf.get(), bookcase))
    .orElse("No location assigned");

System.out.println("Location: " + location);
```

**After (Option 2 - Domain model handles it - BEST):**
```java
// In Book class
public String getLocationDescription() {
    return Optional.ofNullable(shelf)
        .map(Shelf::getFullLocation)
        .orElse("No location assigned");
}

// Usage
System.out.println("Location: " + book.getLocationDescription());
```

---

### Example 4: Using Special Case Pattern

Instead of null checks everywhere, use the Special Case pattern:

**Before:**
```java
Book book = findBookByTitle(title);
if (book == null) {
    System.out.println("No title");
} else {
    System.out.println(book.getTitle());
}
```

**After:**
```java
// Special case class
public class NullBook extends Book {
    public NullBook() {
        super();
    }

    @Override
    public String getTitle() {
        return "Unknown Book";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getLocationDescription() {
        return "Not in library";
    }

    public boolean isNull() {
        return true;
    }
}

// Service method
public Book findBookByTitleOrNull(String title) {
    return bookRepository.findByTitle(title)
        .orElse(new NullBook());
}

// Usage (no null check needed!)
Book book = findBookByTitleOrNull(title);
System.out.println(book.getTitle());  // Always works
```

---

## Patterns You Should Recognize

### Pattern #1: The Silent Failure

Your code has this pattern throughout:

```java
if (something == null) {
    return null;
}
```

This pushes the problem to the caller, who might forget to handle it.

### Pattern #2: Deep Nesting from Null Checks

```java
if (book != null) {
    if (book.getShelfId() != null) {
        Optional<Shelf> shelf = findShelf(book.getShelfId());
        if (shelf.isPresent()) {
            // Finally can do something
        }
    }
}
```

This is the "Arrow Anti-Pattern." Use Optional chaining or fail fast with exceptions.

### Pattern #3: Error Handling That Obscures Logic

Don't let error handling dominate your code:

**Bad:**
```java
public void doSomething() {
    if (a == null) return;
    if (b == null) return;
    if (c == null) return;
    if (d == null) return;

    // Actual logic (4 lines)
}
```

**Good:**
```java
public void doSomething() {
    validateInputs(a, b, c, d);

    // Actual logic (clearly visible)
}
```

---

## Action Items

### 🔴 High Priority (This Week)

#### 1. **Create Custom Exception Classes**
**New package:** `com.penrose.bibby.exception`
- [ ] Create `BibbyException` base class
- [ ] Create `BookNotFoundException`
- [ ] Create `BookAlreadyCheckedOutException`
- [ ] Create `ShelfFullException`
- [ ] Create `InvalidBookStateException`
- **Estimated time:** 45 minutes
- **Impact:** Foundation for proper error handling

#### 2. **Fix findBookByTitle to Return Optional**
**File:** `BookService.java:43-54`
- [ ] Change return type to `Optional<BookEntity>`
- [ ] Remove `return null` statement
- [ ] Remove side effect (println)
- [ ] Update all callers
- **Estimated time:** 1 hour
- **Impact:** Eliminates null returns, safer code

#### 3. **Add Validation to createNewBook**
**File:** `BookService.java:23-41`
- [ ] Validate BookRequestDTO is not null
- [ ] Validate title is not blank
- [ ] Validate author names are not blank
- [ ] Throw IllegalArgumentException for invalid input
- **Estimated time:** 30 minutes
- **Impact:** Prevents invalid data

#### 4. **Fix Dangerous .get() Calls**
**File:** `BookCommands.java:362-363, 475-477, 551-553`
- [ ] Replace `.get()` with `.map()` or `.orElse()`
- [ ] Or push logic into domain model (better)
- **Estimated time:** 1 hour
- **Impact:** Prevents NoSuchElementException crashes

---

### 🟡 Medium Priority (Next 2 Weeks)

#### 5. **Add Exception Handling to All Commands**
**Files:** `BookCommands.java`, `BookcaseCommands.java`
- [ ] Wrap service calls in try-catch
- [ ] Handle specific exceptions with user-friendly messages
- [ ] Log unexpected exceptions
- [ ] Display errors with ConsoleColors utility
- **Estimated time:** 3 hours
- **Impact:** User-friendly error messages

#### 6. **Add Business Logic Validation to Domain Models**
**Files:** `BookEntity.java`, `ShelfEntity.java`, `BookcaseEntity.java`
- [ ] Add `checkout()` method that throws exceptions
- [ ] Add `checkin()` method that throws exceptions
- [ ] Add `assignToShelf()` with validation
- [ ] Protect invariants
- **Estimated time:** 2 hours
- **Impact:** Domain models protect themselves

#### 7. **Replace Null Returns in BookMapper**
**File:** `BookMapper.java:10-12, 33-35`
- [ ] Use `Objects.requireNonNull()` instead of null checks
- [ ] Or throw IllegalArgumentException
- [ ] Fail fast rather than returning null
- **Estimated time:** 15 minutes
- **Impact:** Clearer error messages

---

### 🟢 Low Priority (Nice to Have)

#### 8. **Add Global Exception Handler for Spring**
- [ ] Create `@ControllerAdvice` for REST endpoints (future)
- [ ] Create custom error responses
- [ ] Log all exceptions centrally
- **Estimated time:** 1 hour
- **Impact:** Centralized error handling

#### 9. **Add Bean Validation**
- [ ] Add validation annotations to DTOs
- [ ] Add `@Valid` to service methods
- [ ] Configure validation messages
- **Estimated time:** 1.5 hours
- **Impact:** Declarative validation

---

## Key Takeaways

### What You're Doing Right

1. **Using Optional in some places:** You understand the concept
2. **Checking for null sometimes:** You're aware of the problem
3. **No swallowed exceptions:** You're not hiding errors (because you have no exception handling at all!)

### What Needs Work

1. **No exception handling:** Zero try-catch blocks in entire codebase
2. **Returning null:** Instead of Optional or exceptions
3. **No custom exceptions:** Can't differentiate error types
4. **No validation:** Methods accept invalid input silently
5. **Dangerous .get() calls:** Will crash if Optional is empty
6. **Null checks in UI layer:** Error handling in wrong place

### The Big Lesson

**Make failures explicit and handle them at the right level.**

Don't:
- Return null (use Optional or throw exception)
- Swallow errors silently
- Let errors propagate as generic exceptions
- Put error handling in the UI layer

Do:
- Use custom exceptions for domain errors
- Fail fast with validation
- Provide context in error messages
- Handle exceptions at boundaries (CLI, REST, etc.)
- Let happy path be clean and readable

### Uncle Bob's Rules

1. **Exceptions are better than return codes**
2. **Don't return null**
3. **Don't pass null**
4. **Provide context with exceptions**
5. **Define exception classes in terms of caller's needs**

---

## Further Study

### From Clean Code (Chapter 7)
- Re-read pages 103-114
- Focus on "Use Exceptions Rather Than Return Codes"
- Study "Don't Return Null" section carefully

### Effective Java (Joshua Bloch)
- Item 69: Use exceptions only for exceptional conditions
- Item 70: Use checked exceptions for recoverable conditions, runtime exceptions for programming errors
- Item 72: Favor the use of standard exceptions
- Item 73: Throw exceptions appropriate to the abstraction
- Item 75: Include failure-capture information in detail messages

### Java Optional
- Oracle's Optional documentation
- When to use Optional vs exceptions
- Optional chaining patterns

### Spring Exception Handling
- @ControllerAdvice for REST APIs
- @ExceptionHandler patterns
- ResponseStatusException

---

## Final Thoughts from Your Mentor

Your codebase has a critical gap: **no error handling whatsoever**.

This is actually pretty common for early-stage projects. You've been focused on getting features working, and error handling feels like overhead.

But here's the reality: **Production code without error handling is not production-ready.**

Right now, your application will crash with stack traces for common scenarios:
- Database connection fails
- User enters invalid input
- Book doesn't exist
- Shelf is full

The good news? Adding error handling is straightforward once you understand the patterns.

Start with the high-priority items:
1. Create custom exceptions
2. Fix `findBookByTitle` to return Optional
3. Add validation
4. Fix dangerous `.get()` calls

These four changes will dramatically improve your error handling.

Then add try-catch blocks to your commands so users see friendly error messages instead of stack traces.

Remember: **Errors are not exceptional. They're normal.** Users will enter bad input. Databases will go down. Networks will fail. Your code should handle these gracefully.

Error handling is not about preventing errors. It's about **controlling** how your program responds when things go wrong.

Do it right, and your users will trust your application. Do it wrong (or not at all), and they'll see you as unprofessional.

Let's fix that.

---

**End of Section 6: Error Handling**
