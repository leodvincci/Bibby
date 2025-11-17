# SECTION 22: DEBUGGING & DEFENSIVE PROGRAMMING

## Introduction: When Things Go Wrong

**"Everyone knows that debugging is twice as hard as writing a program in the first place."** - Brian Kernighan

Software will fail. Users will provide invalid input. External systems will be unavailable. Network connections will timeout. The question isn't "Will bugs occur?" but "How will you handle them when they do?"

This section covers two critical skills:
1. **Debugging**: Systematic approach to finding and fixing bugs
2. **Defensive Programming**: Writing code that fails gracefully and loudly

We'll apply these techniques to your **Bibby** codebase, showing where defensive programming is missing and how to add it.

**Learning Objectives**:
- Master systematic debugging techniques
- Understand Design by Contract
- Implement preconditions, postconditions, and invariants
- Apply fail-fast programming
- Add defensive validation to Bibby

**Time Investment**: 3-4 hours

---

## Part 1: Pragmatic Debugging

### The Debugging Mindset

**Rule #0: Don't Panic**

When you see an error, your first instinct might be:
- "This is impossible!"
- "But it worked yesterday!"
- "It must be the compiler/database/framework!"

**Stop**. Take a breath. Bugs are your fault (99% of the time). Accept it and fix it systematically.

### The Six-Step Debugging Process

#### Step 1: Reproduce the Bug

**If you can't reproduce it reliably, you can't fix it.**

**Bad approach**:
> "Sometimes when I check out a book, it stays available. But not always. I'll just add more code..."

**Good approach**:
```markdown
# Bug Reproduction Steps

1. Start Bibby
2. Run: `checkout --book-id 42`
3. Run: `list-books`
4. EXPECTED: Book 42 shows status "CHECKED_OUT"
5. ACTUAL: Book 42 shows status "AVAILABLE"
6. REPRODUCES: 100% of the time with book ID 42, but not with other IDs
```

Now you have something concrete to investigate.

**In Bibby**: Add a `BUGS.md` file to track reproduction steps for all bugs.

#### Step 2: Read the Error Message

**Actually read it.** Don't just glance at it.

**Example from Bibby**:

```
Exception in thread "main" java.lang.NullPointerException:
Cannot invoke "com.penrose.bibby.library.book.BookEntity.getBookStatus()"
because the return value of
"com.penrose.bibby.library.book.BookRepository.findByTitle(String)" is null
	at BookService.checkOutBook(BookService.java:57)
	at BookCommands.checkout(BookCommands.java:23)
```

**Reading comprehension**:
- **What**: NullPointerException
- **Where**: `BookService.java:57`
- **Why**: `findByTitle()` returned null, then called `.getBookStatus()` on null
- **Cause**: Book with that title doesn't exist

**The fix is obvious** once you read carefully: Check if book exists before calling methods on it.

#### Step 3: Binary Search the Problem

**Divide and conquer**. Cut the problem space in half repeatedly.

**Scenario**: Bibby is slow when listing books.

**Binary search**:
```
Is it slow in production? YES
Is it slow with 10 books? NO
Is it slow with 100 books? NO
Is it slow with 1000 books? YES

→ Performance degrades with large datasets

Is it the database query? (Add logging)
Query time: 5ms ✓ Fast

Is it the object mapping? (Add logging)
Mapping time: 20ms ✓ Fast

Is it the formatting? (Add logging)
Formatting time: 5000ms ❌ FOUND IT!
```

**Root cause**: Formatting loop calls database for each book's authors (N+1 query problem).

#### Step 4: Explain It to Someone Else (Rubber Duck Debugging)

**Literally explain the problem out loud to a rubber duck (or colleague).**

**Example**:
> "So I'm trying to checkout a book. The command calls `BookService.checkOutBook()` which takes a `BookEntity`. Wait... the command is passing a null BookEntity! I need to find the book first before checking it out!"

**Often, the act of explaining reveals the bug.**

#### Step 5: Check Your Assumptions

**Your code has bugs because your assumptions are wrong.**

**Common wrong assumptions in Bibby**:

**Assumption**: "findByTitle() always returns a book"
**Reality**: Returns null if book doesn't exist

**Assumption**: "Book status is always a valid enum"
**Reality**: It's a String in the database, could be anything

**Assumption**: "Authors array is never empty"
**Reality**: Could be empty if book creation failed partially

**Test every assumption.**

#### Step 6: Fix the Cause, Not the Symptom

**Bad fix** (treating symptom):
```java
public void checkOutBook(BookEntity bookEntity) {
    if (bookEntity != null) {  // Band-aid!
        if (bookEntity.getBookStatus() != null) {  // Another band-aid!
            if (!bookEntity.getBookStatus().equals("CHECKED_OUT")) {
                bookEntity.setBookStatus("CHECKED_OUT");
                bookRepository.save(bookEntity);
            }
        }
    }
}
```

**Good fix** (treating cause):
```java
public void checkOut(BookId bookId) {
    // Cause: We weren't checking if book exists
    Book book = bookRepository.findById(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    // Business logic in domain model (proper design)
    book.checkOut();

    bookRepository.save(book);
}
```

**The cause**: Not checking if book exists before operating on it.
**The fix**: Use `Optional` and throw explicit exception.

---

## Part 2: Design by Contract

### What is Design by Contract?

**Every function has a contract**:
- **Preconditions**: What must be true before the function runs
- **Postconditions**: What the function guarantees after it runs
- **Invariants**: What's always true about an object's state

**Example from Bibby**:

```java
/**
 * Check out this book.
 *
 * PRECONDITIONS:
 * - Book must exist (id != null)
 * - Book must be AVAILABLE status
 *
 * POSTCONDITIONS:
 * - Book status is CHECKED_OUT
 * - checkoutCount is incremented
 * - updatedAt is set to now
 *
 * INVARIANTS:
 * - checkoutCount >= 0 (always)
 */
public void checkOut() {
    // Precondition checks
    Objects.requireNonNull(this.id, "Book ID cannot be null");

    if (this.status != AvailabilityStatus.AVAILABLE) {
        throw new BookNotAvailableException(
            "Precondition violated: book must be available to check out. " +
            "Current status: " + this.status
        );
    }

    // Operation
    this.status = AvailabilityStatus.CHECKED_OUT;
    this.checkoutCount++;
    this.updatedAt = LocalDateTime.now();

    // Postcondition assertions (enabled in dev/test)
    assert this.status == AvailabilityStatus.CHECKED_OUT
        : "Postcondition violated: status should be CHECKED_OUT";

    assert this.checkoutCount > 0
        : "Postcondition violated: checkout count should be positive";

    // Invariant check
    assert this.checkoutCount >= 0
        : "Invariant violated: checkout count cannot be negative";
}
```

**Benefits**:
1. **Self-documenting**: Contract is explicit
2. **Fail-fast**: Violations are caught immediately
3. **Debugging**: Narrows down where bugs occur
4. **Testing**: Preconditions/postconditions guide test cases

### Preconditions: What Callers Must Guarantee

**Preconditions** are checks at the **start** of a method.

**Current Bibby Problem** from `BookService.java:73-77`:
```java
public void checkInBook(String bookTitle) {
    BookEntity bookEntity = findBookByTitle(bookTitle);
    bookEntity.setBookStatus(BookStatus.AVAILABLE.toString());
    updateBook(bookEntity);
}
```

**What if**:
- `bookTitle` is null?
- `bookTitle` is empty string?
- No book exists with that title?

**All three cases cause NullPointerException!**

**Defensive version with preconditions**:
```java
public void checkIn(BookId bookId) {
    // Precondition: bookId must not be null
    Objects.requireNonNull(bookId, "Book ID cannot be null");

    // Precondition: book must exist
    Book book = bookRepository.findById(bookId)
        .orElseThrow(() -> new BookNotFoundException(
            "Precondition violated: book with ID " + bookId + " does not exist"
        ));

    // Precondition: book must be checked out
    if (book.getStatus() != AvailabilityStatus.CHECKED_OUT) {
        throw new IllegalStateException(
            "Precondition violated: cannot check in book that is not checked out. " +
            "Current status: " + book.getStatus()
        );
    }

    // Now safe to proceed
    book.returnToLibrary();
    bookRepository.save(book);
}
```

**Now impossible to cause NullPointerException!** All preconditions are checked.

### Postconditions: What Your Method Guarantees

**Postconditions** are checks at the **end** of a method (or assertions about state).

**Example in Book domain**:
```java
public void placeOnShelf(ShelfId shelfId) {
    // Precondition
    Objects.requireNonNull(shelfId, "Shelf ID cannot be null");

    if (shelfId.equals(ShelfId.unassigned())) {
        throw new IllegalArgumentException("Cannot place on unassigned shelf");
    }

    // Operation
    this.currentShelfId = shelfId;
    this.updatedAt = LocalDateTime.now();

    // Postcondition: shelf must be set
    assert this.currentShelfId != null
        : "Postcondition violated: shelf ID should be set";

    assert this.currentShelfId.equals(shelfId)
        : "Postcondition violated: shelf ID should match provided ID";

    assert !this.currentShelfId.equals(ShelfId.unassigned())
        : "Postcondition violated: book should not be on unassigned shelf";
}
```

**Postconditions document what the caller can assume** after calling the method.

### Invariants: What's Always True

**Invariants** are conditions that must **always** hold for an object.

**Example: Book invariants**:
```java
public class Book {
    private final BookId id;
    private final Title title;
    private final ISBN isbn;
    private final Set<AuthorId> authorIds;

    private AvailabilityStatus status;
    private int checkoutCount;

    /**
     * Class invariants (always true):
     * - id is never null
     * - title is never null
     * - isbn is never null
     * - authorIds is never empty
     * - checkoutCount >= 0
     * - if status is CHECKED_OUT, checkoutCount > 0
     */

    private void checkInvariants() {
        assert id != null : "Invariant: id cannot be null";
        assert title != null : "Invariant: title cannot be null";
        assert isbn != null : "Invariant: isbn cannot be null";
        assert !authorIds.isEmpty() : "Invariant: must have at least one author";
        assert checkoutCount >= 0 : "Invariant: checkout count cannot be negative";

        if (status == AvailabilityStatus.CHECKED_OUT) {
            assert checkoutCount > 0
                : "Invariant: if checked out, checkout count must be positive";
        }
    }

    // Call after every state-changing method (in dev/test)
    public void checkOut() {
        // ... logic ...
        assert checkInvariants();
    }

    public void returnToLibrary() {
        // ... logic ...
        assert checkInvariants();
    }
}
```

**Invariants catch bugs early**: If an invariant is violated, the bug is in the code that just ran, making debugging easier.

### Enabling Assertions

**Java assertions are disabled by default!**

To enable:
```bash
java -ea com.penrose.bibby.BibbyApplication
```

Or in IntelliJ: Run → Edit Configurations → VM options: `-ea`

**Best practice**:
- Enable assertions during **development and testing**
- Optionally disable in **production** for performance (but consider keeping them!)

---

## Part 3: Fail Fast

### The Principle

**Fail fast** means: When something goes wrong, **fail immediately and loudly**.

**Don't**:
- Silently ignore errors
- Return null and hope caller checks
- Log error and continue
- Catch exception and do nothing

**Do**:
- Throw explicit exceptions
- Use `Objects.requireNonNull()`
- Use `Optional.orElseThrow()`
- Validate early, fail early

### Example from Bibby: Silent Failure

**Current code** (`BookService.java:56-62`):
```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
    // No else clause - silently does nothing if already checked out!
}
```

**Problem**: If book is already checked out, method silently succeeds. Caller thinks checkout worked!

**Fail-fast version**:
```java
public void checkOut() {
    if (this.status == AvailabilityStatus.CHECKED_OUT) {
        throw new BookAlreadyCheckedOutException(
            "Cannot check out book " + id + " - already checked out"
        );
    }

    if (this.status == AvailabilityStatus.LOST) {
        throw new BookLostException(
            "Cannot check out book " + id + " - marked as lost"
        );
    }

    if (this.status != AvailabilityStatus.AVAILABLE) {
        throw new BookNotAvailableException(
            "Cannot check out book " + id + " - status: " + status
        );
    }

    // Now safe to proceed
    this.status = AvailabilityStatus.CHECKED_OUT;
    this.checkoutCount++;
}
```

**Now impossible to silently fail!** Every error case throws explicit exception.

### Null Checks: The Most Common Precondition

**Bad** (NullPointerException waiting to happen):
```java
public void addBook(Book book) {
    books.add(book);  // If book is null, NPE!
}
```

**Good** (fail fast):
```java
public void addBook(Book book) {
    Objects.requireNonNull(book, "Book cannot be null");
    books.add(book);
}
```

**Even better** (use type system):
```java
// If you use Optional<Book>, null is impossible!
public void addBook(Optional<Book> book) {
    book.ifPresent(books::add);
}

// Or enforce non-null at compile time with annotations
public void addBook(@NonNull Book book) {
    books.add(book);  // IDE warns if you pass nullable
}
```

### Example: BookcaseService createNewBookCase

**Current code** (`BookcaseService.java:25-41`):
```java
public String createNewBookCase(String label, int capacity){
    BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);
    if(bookcaseEntity !=null){
        log.error("Failed to save Record - Record already exist",existingRecordError);
        throw existingRecordError;
    }
    else{
        bookcaseEntity = new BookcaseEntity(label,capacity);
        bookcaseRepository.save(bookcaseEntity);
        // ...
    }
}
```

**Missing preconditions**:
- What if `label` is null?
- What if `label` is empty string?
- What if `capacity` is negative?
- What if `capacity` is 0?
- What if `capacity` is 1000 (unreasonable)?

**Defensive version**:
```java
public BookcaseId createBookcase(String label, int capacity) {
    // Precondition: label must not be null or blank
    Objects.requireNonNull(label, "Bookcase label cannot be null");

    if (label.isBlank()) {
        throw new IllegalArgumentException("Bookcase label cannot be blank");
    }

    // Precondition: label length reasonable
    if (label.length() > 50) {
        throw new IllegalArgumentException(
            "Bookcase label too long (max 50 characters): " + label.length()
        );
    }

    // Precondition: capacity must be reasonable
    if (capacity < 1) {
        throw new IllegalArgumentException(
            "Bookcase capacity must be at least 1, got: " + capacity
        );
    }

    if (capacity > 100) {
        throw new IllegalArgumentException(
            "Bookcase capacity too large (max 100), got: " + capacity
        );
    }

    // Precondition: label must be unique
    if (bookcaseRepository.existsByLabel(label)) {
        throw new DuplicateBookcaseLabelException(
            "Bookcase with label '" + label + "' already exists"
        );
    }

    // Now safe to create
    Bookcase bookcase = Bookcase.create(
        BookcaseLabel.of(label),
        capacity
    );

    bookcaseRepository.save(bookcase);

    // Postcondition: bookcase was saved
    assert bookcaseRepository.existsById(bookcase.getId())
        : "Postcondition: bookcase should exist in repository";

    return bookcase.getId();
}
```

**Every assumption is validated!** Impossible to create invalid bookcase.

---

## Part 4: Defensive Programming in Bibby

### Problem Areas in Current Code

Let's audit Bibby for missing defensive programming:

#### Problem 1: AuthorEntity.setFullName

**Code** (`AuthorEntity.java:66-68`):
```java
public void setFullName(String fullName) {
    this.fullName = fullName;
}
```

**Missing defenses**:
- No null check
- No blank check
- No length limit

**What if someone calls**:
```java
author.setFullName(null);  // NullPointerException later!
author.setFullName("");    // Empty name!
author.setFullName("x".repeat(10000));  // Huge string!
```

**Defensive version**:
```java
public void setFullName(String fullName) {
    Objects.requireNonNull(fullName, "Full name cannot be null");

    if (fullName.isBlank()) {
        throw new IllegalArgumentException("Full name cannot be blank");
    }

    if (fullName.length() > 200) {
        throw new IllegalArgumentException(
            "Full name too long (max 200 characters): " + fullName.length()
        );
    }

    this.fullName = fullName.trim();
}
```

**Better**: Don't have `fullName` field at all (derive it from firstName + lastName).

#### Problem 2: BookService.findBookByTitle

**Code** (`BookService.java:43-54`):
```java
public BookEntity findBookByTitle(String title){
    Optional<BookEntity> bookEntity = Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
    List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
    for(BookEntity b : bookEntities){
        System.out.println(b.getTitle());  // Side effect!
    }

    if(bookEntity.isEmpty()){
        return null;  // Caller must check for null!
    }
    return bookEntity.get();
}
```

**Problems**:
- No null check on `title`
- Side effect (`System.out.println`) in query method
- Returns null instead of Optional
- Confusing: searches exactly AND by contains, but only uses exact match?

**Defensive version**:
```java
public Optional<Book> findByTitle(String title) {
    // Precondition
    Objects.requireNonNull(title, "Title cannot be null");

    if (title.isBlank()) {
        return Optional.empty();  // Blank title matches nothing
    }

    // Clear purpose: exact match
    return bookRepository.findByTitleIgnoreCase(title);
}

// Separate method for partial matching
public List<Book> searchByTitle(String titleFragment) {
    Objects.requireNonNull(titleFragment, "Title fragment cannot be null");

    if (titleFragment.isBlank()) {
        return List.of();  // Blank search returns nothing
    }

    return bookRepository.findByTitleContaining(titleFragment);
}
```

**Defensive improvements**:
- Null checks
- Returns `Optional` (forces caller to handle absence)
- No side effects
- Clear separation of exact vs. partial search

#### Problem 3: BookEntity.setAuthors

**Code** (`BookEntity.java:54-56`):
```java
public void setAuthors(AuthorEntity authors) {  // Parameter name wrong!
    this.authors.add(authors);
}
```

**Problems**:
- Parameter named `authors` (plural) but type is singular
- No null check
- Method name `setAuthors` implies replace, but it adds
- No validation that author isn't already in set

**Defensive version**:
```java
public void addAuthor(AuthorEntity author) {
    Objects.requireNonNull(author, "Author cannot be null");

    if (this.authors.contains(author)) {
        throw new IllegalArgumentException(
            "Author " + author.getFullName() + " already added to this book"
        );
    }

    this.authors.add(author);

    // Postcondition
    assert this.authors.contains(author)
        : "Postcondition: author should be in authors set";
}

public void setAuthors(Set<AuthorEntity> authors) {
    Objects.requireNonNull(authors, "Authors cannot be null");

    if (authors.isEmpty()) {
        throw new IllegalArgumentException("Book must have at least one author");
    }

    this.authors.clear();
    this.authors.addAll(authors);

    // Postcondition
    assert !this.authors.isEmpty()
        : "Postcondition: authors should not be empty";
}
```

---

## Action Items: Adding Defensive Programming to Bibby

### Action Item 1: Add Preconditions to All Public Methods (3-4 hours)

**Task**: Audit every public method and add null checks.

**Files to check**:
- `BookService.java`
- `AuthorService.java`
- `ShelfService.java`
- `BookcaseService.java`

**Template**:
```java
public void methodName(Type param) {
    // Add precondition
    Objects.requireNonNull(param, "Param cannot be null");

    // Existing logic
}
```

**Commit**: "Add null preconditions to all service methods"

### Action Item 2: Replace Null Returns with Optional (2-3 hours)

**Task**: Find all methods returning null and return `Optional` instead.

**Before**:
```java
public BookEntity findByTitle(String title) {
    // ...
    return null;  // Bad!
}
```

**After**:
```java
public Optional<Book> findByTitle(String title) {
    // ...
    return Optional.empty();  // Good!
}
```

**Update callers**:
```java
// Before
Book book = bookService.findByTitle("DDD");
if (book != null) {  // Easy to forget!
    // use book
}

// After
Optional<Book> bookOpt = bookService.findByTitle("DDD");
bookOpt.ifPresent(book -> {
    // use book
});

// Or
Book book = bookService.findByTitle("DDD")
    .orElseThrow(() -> new BookNotFoundException("DDD"));
```

### Action Item 3: Add Validation Value Objects (4-5 hours)

**Task**: Create value objects with validation for key concepts.

**ISBN Value Object** (already designed in Section 9):
```java
public record ISBN(String value) {
    public ISBN {
        Objects.requireNonNull(value, "ISBN cannot be null");

        String cleaned = value.replaceAll("[\\s-]", "");

        if (!isValid(cleaned)) {
            throw new IllegalArgumentException("Invalid ISBN: " + value);
        }

        value = cleaned;
    }

    private static boolean isValid(String isbn) {
        return isbn.matches("^\\d{9}[\\dX]$") || isbn.matches("^\\d{13}$");
    }
}
```

**BookcaseLabel Value Object**:
```java
public record BookcaseLabel(String value) {
    public BookcaseLabel {
        Objects.requireNonNull(value, "Bookcase label cannot be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("Bookcase label cannot be blank");
        }

        if (value.length() > 50) {
            throw new IllegalArgumentException(
                "Bookcase label too long (max 50): " + value.length()
            );
        }

        value = value.trim();
    }
}
```

**Now impossible to create invalid ISBN or label!**

### Action Item 4: Add Assertions for Invariants (2 hours)

**Task**: Add `assert` statements to check class invariants.

**Example in Book**:
```java
public class Book {
    // ... fields ...

    private void checkInvariants() {
        assert id != null;
        assert title != null;
        assert isbn != null;
        assert !authorIds.isEmpty();
        assert checkoutCount >= 0;
    }

    public void checkOut() {
        // ... logic ...
        checkInvariants();  // Verify invariants hold
    }

    public void returnToLibrary() {
        // ... logic ...
        checkInvariants();
    }
}
```

**Run with**:
```bash
java -ea -jar bibby.jar
```

### Action Item 5: Replace Silent Failures with Exceptions (3 hours)

**Task**: Find all places where errors are silently ignored.

**Search for**:
- Empty `if` blocks with no `else`
- `catch (Exception e) { }` with empty catch
- Methods that should throw but don't

**Example fix**:
```java
// Before: Silent failure
public void checkOut(BookEntity book) {
    if (book.getStatus().equals("AVAILABLE")) {
        book.setStatus("CHECKED_OUT");
    }
    // Silently does nothing if not available!
}

// After: Fail fast
public void checkOut() {
    if (this.status != AvailabilityStatus.AVAILABLE) {
        throw new BookNotAvailableException(
            "Cannot check out book - status: " + this.status
        );
    }
    this.status = AvailabilityStatus.CHECKED_OUT;
}
```

### Action Item 6: Create Custom Exceptions (1-2 hours)

**Task**: Create domain-specific exceptions.

**Location**: `src/main/java/com/penrose/bibby/library/book/exceptions/`

**Examples**:
```java
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(BookId id) {
        super("Book not found with ID: " + id);
    }

    public BookNotFoundException(String title) {
        super("Book not found with title: " + title);
    }
}

public class BookNotAvailableException extends RuntimeException {
    public BookNotAvailableException(String message) {
        super(message);
    }
}

public class BookAlreadyCheckedOutException extends RuntimeException {
    public BookAlreadyCheckedOutException(BookId id) {
        super("Book " + id + " is already checked out");
    }
}

public class InvalidIsbnException extends RuntimeException {
    public InvalidIsbnException(String isbn) {
        super("Invalid ISBN format: " + isbn);
    }
}
```

**Usage**:
```java
Book book = bookRepository.findById(bookId)
    .orElseThrow(() -> new BookNotFoundException(bookId));
```

---

## Key Takeaways

### 1. Debug Systematically, Not Randomly

- Reproduce reliably
- Read the error message
- Binary search the problem
- Check assumptions
- Fix cause, not symptom

### 2. Design by Contract Makes Intent Explicit

- **Preconditions**: What caller must guarantee
- **Postconditions**: What method guarantees
- **Invariants**: What's always true

### 3. Fail Fast, Fail Loudly

- Don't return null - use `Optional`
- Don't silently ignore errors - throw exceptions
- Don't catch exceptions and do nothing
- Validate early, validate often

### 4. Defensive Programming Prevents Bugs

- `Objects.requireNonNull()` everywhere
- Value objects with validation
- Assertions for invariants
- Custom exceptions for domain errors

### 5. Assertions Are Your Friend

- Enable with `-ea` flag
- Check preconditions, postconditions, invariants
- Catch bugs during development
- Document assumptions in code

---

## Study Resources

### Books
1. **"The Pragmatic Programmer"** - Debugging and Design by Contract chapters
   - Systematic debugging
   - Assertive programming

2. **"Code Complete" by Steve McConnell**
   - Chapter 8: Defensive Programming
   - Comprehensive coverage

3. **"Effective Java" by Joshua Bloch**
   - Item 72: Favor use of standard exceptions
   - Item 73: Throw exceptions appropriate to the abstraction

### Articles
1. **"Fail Fast" by Martin Fowler**
   - When and why to fail fast
   - https://martinfowler.com/ieeeSoftware/failFast.pdf

2. **"Design by Contract" by Bertrand Meyer**
   - Original DbC concept from Eiffel

### Tools
1. **IntelliJ IDEA inspections**
   - "Constant conditions & exceptions"
   - "Probable bugs"
   - Enable all inspections!

2. **SpotBugs** (formerly FindBugs)
   - Static analysis for Java
   - Finds potential null pointer exceptions

3. **Checker Framework**
   - Compile-time verification
   - `@NonNull`, `@Nullable` annotations

---

## Coming Next

**Section 23: Pragmatic Testing** - Test early, test often, test automatically

We'll cover:
- The testing pyramid
- Test-infected: a good thing
- Writing testable code
- Property-based testing
- Applying to Bibby

---

**Section 22 Complete** | **Time Invested**: 3-4 hours | **Files to Create**: `BUGS.md`, custom exception classes

Debugging is a skill you'll use every day. Defensive programming prevents bugs before they happen. Master both, and you'll spend less time debugging and more time building features.

Remember: **"The best time to fix a bug is before it's written."** - Design by Contract helps you do exactly that.
