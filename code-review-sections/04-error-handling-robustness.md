# Section 4: Error Handling & Robustness

## Overview

This section covers issues related to **defensive programming** and **error handling**. Robust code anticipates failures, validates inputs, and handles errors gracefully. Poor error handling leads to crashes, data corruption, and terrible user experiences.

**Key Statistics:**
- Issues Found: 4
- Critical: 2
- High: 1
- Medium: 1
- Low: 0

**Cost of Poor Error Handling:**
- Production crashes and data loss
- Difficult debugging (unclear error messages)
- Security vulnerabilities (SQL injection, XSS)
- User frustration and lost trust

---

## 🔴 Issue 4.1: Missing Null Checks Leading to NullPointerException

**Priority:** CRITICAL
**Locations:**
- `BookCommands.java:250-260` - Parsing user input without validation
- `BookCommands.java:368-374` - Chained Optional.get() calls
- Multiple other locations throughout CLI commands

### Current Code

```java
// BookCommands.java - Lines 250-260
BookEntity bookEnt = bookService.findBookByTitle(title);
if (bookEnt == null) {
    System.out.println("Book Not Found In Library");
} else {
    Long shelfId = Long.parseLong(
        res.getContext().get("bookshelf", String.class)  // ❌ Can be null!
    );
    bookEnt.setShelfId(shelfId);  // ❌ What if shelfId parse failed?
    bookService.updateBook(bookEnt);
}

// BookCommands.java - Lines 368-374
if (bookEntity.getShelfId() == null) {
    System.out.println("Book is not on shelf");
} else {
    Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
    Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(
        shelfEntity.get().getBookcaseId()  // ❌ CRASHES if shelf not found!
    );

    System.out.println("Bookcase: " + bookcaseEntity.get().getBookcaseLabel());  // ❌ CRASHES again!
    System.out.println("Shelf: " + shelfEntity.get().getShelfLabel());
}
```

### Why It's Problematic

**1. Guaranteed Runtime Crashes:**

```
User Scenario: Enter invalid shelf ID

Input: User types "abc" instead of a number
  → res.getContext().get("bookshelf") returns "abc"
    → Long.parseLong("abc") throws NumberFormatException
      → Application crashes
        → User sees scary stack trace
          → User thinks app is broken

Total time to crash: < 1 second
User experience: Terrible
```

**2. Chained Null Dereferencing:**

```java
// What happens when shelf doesn't exist:
Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(999);
// shelfEntity = Optional.empty()

shelfEntity.get()  // ❌ NoSuchElementException: No value present!
// Application crashes before even checking bookcase
```

**3. No Error Recovery:**

```java
// Current: App crashes, user loses work
try {
    Long.parseLong(userInput);
} catch (Exception e) {
    // No catch block - exception propagates up
    // User session terminated
}

// Better: Catch, show error, let user retry
try {
    Long.parseLong(userInput);
} catch (NumberFormatException e) {
    System.err.println("Invalid input. Please enter a number.");
    // User can try again
}
```

### The Defensive Programming Mindset

> **"Never trust user input. Never trust external systems. Never trust yourself."**

```java
// Levels of defensive programming:

// Level 0: No validation (your current code)
Long id = Long.parseLong(input);  // YOLO!

// Level 1: Null check
if (input != null) {
    Long id = Long.parseLong(input);
}

// Level 2: Null + format check
if (input != null && input.matches("\\d+")) {
    Long id = Long.parseLong(input);
}

// Level 3: Full validation with error handling
try {
    if (input == null || input.isBlank()) {
        throw new IllegalArgumentException("Input required");
    }
    Long id = Long.parseLong(input.trim());
    if (id <= 0) {
        throw new IllegalArgumentException("ID must be positive");
    }
    return id;
} catch (NumberFormatException e) {
    throw new IllegalArgumentException("Invalid number: " + input, e);
}
```

### Correct Approach

**Solution 1: Validate User Input Before Parsing**

```java
/**
 * Safely adds a book to a shelf with full validation.
 */
public void addBookToShelf() {
    try {
        // Step 1: Get and validate book title
        String title = getUserInput("Enter book title: ");
        if (title == null || title.isBlank()) {
            System.err.println("❌ Book title is required");
            return;
        }

        // Step 2: Find book (validate it exists)
        BookEntity book = bookService.findByTitle(title)
            .orElseThrow(() -> new BookNotFoundException(
                "Book '" + title + "' not found in library"
            ));

        // Step 3: Get and validate shelf ID
        String shelfIdInput = res.getContext().get("bookshelf", String.class);
        Long shelfId = validateAndParseShelfId(shelfIdInput);

        // Step 4: Validate shelf exists
        ShelfEntity shelf = shelfService.findById(shelfId)
            .orElseThrow(() -> new ShelfNotFoundException(
                "Shelf with ID " + shelfId + " not found"
            ));

        // Step 5: Perform operation (all validations passed)
        book.setShelfId(shelfId);
        bookService.updateBook(book);

        System.out.println("✅ Successfully added '" + book.getTitle() +
                          "' to shelf " + shelf.getShelfLabel());

    } catch (BookNotFoundException e) {
        System.err.println("❌ " + e.getMessage());
        System.err.println("Tip: Use 'list books' to see available books");
    } catch (ShelfNotFoundException e) {
        System.err.println("❌ " + e.getMessage());
        System.err.println("Tip: Use 'list shelves' to see available shelves");
    } catch (IllegalArgumentException e) {
        System.err.println("❌ Invalid input: " + e.getMessage());
    } catch (Exception e) {
        System.err.println("❌ Unexpected error: " + e.getMessage());
        log.error("Error adding book to shelf", e);
    }
}

/**
 * Validates and parses shelf ID from user input.
 *
 * @param input the user input string
 * @return valid shelf ID (positive Long)
 * @throws IllegalArgumentException if input is invalid
 */
private Long validateAndParseShelfId(String input) {
    // Check for null or blank
    if (input == null || input.isBlank()) {
        throw new IllegalArgumentException("Shelf ID is required");
    }

    // Trim whitespace
    input = input.trim();

    // Try to parse
    Long shelfId;
    try {
        shelfId = Long.parseLong(input);
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Shelf ID must be a number. You entered: '" + input + "'",
            e
        );
    }

    // Validate range
    if (shelfId <= 0) {
        throw new IllegalArgumentException(
            "Shelf ID must be a positive number. You entered: " + shelfId
        );
    }

    return shelfId;
}
```

**Solution 2: Safe Optional Handling**

```java
/**
 * Displays book location with safe Optional handling.
 */
public void displayBookLocation(BookEntity book) {
    if (book.getShelfId() == null) {
        System.out.println("📚 Book is not assigned to a shelf");
        return;
    }

    // Option A: Using ifPresentOrElse
    shelfService.findById(book.getShelfId())
        .ifPresentOrElse(
            shelf -> displayShelfAndBookcase(shelf),
            () -> System.err.println("⚠️ Shelf data inconsistent - shelf ID exists but shelf not found")
        );
}

/**
 * Displays shelf and bookcase information.
 */
private void displayShelfAndBookcase(ShelfEntity shelf) {
    System.out.println("📍 Shelf: " + shelf.getShelfLabel());

    // Safe chaining with flatMap
    String bookcaseInfo = bookcaseService.findById(shelf.getBookcaseId())
        .map(bookcase -> "Bookcase: " + bookcase.getBookcaseLabel())
        .orElse("Bookcase: Unknown");

    System.out.println("📍 " + bookcaseInfo);
}

// Option B: Using map and orElse for one-liner
public String getBookLocationDescription(BookEntity book) {
    if (book.getShelfId() == null) {
        return "Not assigned to a shelf";
    }

    return shelfService.findById(book.getShelfId())
        .flatMap(shelf -> bookcaseService.findById(shelf.getBookcaseId())
            .map(bookcase -> String.format(
                "Bookcase '%s', Shelf '%s'",
                bookcase.getBookcaseLabel(),
                shelf.getShelfLabel()
            ))
        )
        .orElse("Location information unavailable");
}
```

**Solution 3: Create Reusable Validation Utilities**

```java
/**
 * Utility class for validating and parsing user input.
 */
public class InputValidator {

    /**
     * Parses and validates a positive Long ID from user input.
     *
     * @param input the user input string
     * @param fieldName the field name for error messages
     * @return validated positive Long ID
     * @throws IllegalArgumentException if input is invalid
     */
    public static Long parsePositiveId(String input, String fieldName) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        Long id;
        try {
            id = Long.parseLong(input.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                fieldName + " must be a number, got: '" + input + "'",
                e
            );
        }

        if (id <= 0) {
            throw new IllegalArgumentException(
                fieldName + " must be positive, got: " + id
            );
        }

        return id;
    }

    /**
     * Parses and validates an integer within a range.
     *
     * @param input the user input string
     * @param fieldName the field name for error messages
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @return validated integer
     * @throws IllegalArgumentException if input is invalid
     */
    public static int parseIntInRange(String input, String fieldName, int min, int max) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        int value;
        try {
            value = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                fieldName + " must be a number, got: '" + input + "'",
                e
            );
        }

        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("%s must be between %d and %d, got: %d",
                    fieldName, min, max, value)
            );
        }

        return value;
    }

    /**
     * Validates a non-blank string.
     *
     * @param input the string to validate
     * @param fieldName the field name for error messages
     * @return trimmed string
     * @throws IllegalArgumentException if input is null or blank
     */
    public static String requireNonBlank(String input, String fieldName) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return input.trim();
    }

    /**
     * Validates string length.
     *
     * @param input the string to validate
     * @param fieldName the field name for error messages
     * @param minLength minimum length
     * @param maxLength maximum length
     * @return trimmed string
     * @throws IllegalArgumentException if validation fails
     */
    public static String validateLength(String input, String fieldName,
                                       int minLength, int maxLength) {
        String trimmed = requireNonBlank(input, fieldName);

        if (trimmed.length() < minLength || trimmed.length() > maxLength) {
            throw new IllegalArgumentException(
                String.format("%s must be %d-%d characters, got: %d",
                    fieldName, minLength, maxLength, trimmed.length())
            );
        }

        return trimmed;
    }

    /**
     * Validates email format.
     */
    public static String validateEmail(String email) {
        String trimmed = requireNonBlank(email, "Email");

        if (!trimmed.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        return trimmed;
    }
}

// Usage in BookCommands:
public void addBookToShelf() {
    try {
        String title = InputValidator.requireNonBlank(
            getUserInput("Book title: "),
            "Book title"
        );

        Long shelfId = InputValidator.parsePositiveId(
            res.getContext().get("bookshelf", String.class),
            "Shelf ID"
        );

        int authorCount = InputValidator.parseIntInRange(
            res.getContext().get("author_count", String.class),
            "Author count",
            1,  // min
            10  // max
        );

        // All inputs validated - safe to proceed
        processBookAddition(title, shelfId, authorCount);

    } catch (IllegalArgumentException e) {
        System.err.println("❌ " + e.getMessage());
        System.err.println("Please try again with valid input.");
    }
}
```

### Custom Exceptions for Domain Errors

```java
/**
 * Base exception for library domain errors.
 */
public abstract class LibraryException extends RuntimeException {
    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Thrown when a book is not found.
 */
public class BookNotFoundException extends LibraryException {
    public BookNotFoundException(String title) {
        super("Book not found: " + title);
    }

    public BookNotFoundException(Long id) {
        super("Book with ID " + id + " not found");
    }
}

/**
 * Thrown when a shelf is not found.
 */
public class ShelfNotFoundException extends LibraryException {
    public ShelfNotFoundException(Long shelfId) {
        super("Shelf with ID " + shelfId + " not found");
    }
}

/**
 * Thrown when a book is not available for checkout.
 */
public class BookNotAvailableException extends LibraryException {
    public BookNotAvailableException(String message) {
        super(message);
    }
}

/**
 * Thrown when invalid input is provided.
 */
public class InvalidInputException extends LibraryException {
    public InvalidInputException(String message) {
        super(message);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Fail-Fast Principle

```java
// ❌ BAD - Fail late (exception buried deep in call stack)
public void processBook(String title, Long shelfId) {
    // Many lines of code...
    BookEntity book = findBook(title);
    // More code...
    if (book == null) {  // Check happens too late!
        throw new RuntimeException("Book not found");
    }
}

// ✅ GOOD - Fail fast (validate at entry point)
public void processBook(String title, Long shelfId) {
    // Validate immediately
    if (title == null || title.isBlank()) {
        throw new IllegalArgumentException("Title is required");
    }
    if (shelfId == null || shelfId <= 0) {
        throw new IllegalArgumentException("Valid shelf ID required");
    }

    // Now safe to proceed
    BookEntity book = findBook(title);
    // ...
}
```

### Null Safety with Java's Objects Utility

```java
import java.util.Objects;

public class BookService {

    /**
     * Creates a new book with null safety.
     */
    @Transactional
    public BookEntity createBook(BookRequestDTO dto) {
        // requireNonNull throws NPE with custom message if null
        Objects.requireNonNull(dto, "BookRequestDTO cannot be null");
        Objects.requireNonNull(dto.title(), "Book title cannot be null");
        Objects.requireNonNull(dto.firstName(), "Author first name cannot be null");
        Objects.requireNonNull(dto.lastName(), "Author last name cannot be null");

        // Now safe - no nulls possible
        BookEntity book = new BookEntity();
        book.setTitle(dto.title().trim());
        // ...
    }

    /**
     * Updates book with default value if status is null.
     */
    public void updateBookStatus(BookEntity book, BookStatus newStatus) {
        // requireNonNullElse provides default if null
        BookStatus status = Objects.requireNonNullElse(newStatus, BookStatus.AVAILABLE);
        book.setStatus(status);
    }

    /**
     * Gets book description with fallback.
     */
    public String getBookDescription(BookEntity book) {
        // requireNonNullElseGet evaluates supplier only if null (lazy)
        return Objects.requireNonNullElseGet(
            book.getDescription(),
            () -> "No description available"
        );
    }
}
```

### Learning Principle

> **Fail fast with meaningful errors.** Validate inputs at the boundary (as soon as they enter your system). Never trust user input - validate format, range, and business rules. Use custom exceptions for domain errors. Provide helpful error messages that tell users how to fix the problem. Handle errors gracefully - don't crash, guide users to success.

### Action Items

1. ✅ Create `InputValidator` utility class
2. ✅ Add validation to all user input parsing
3. ✅ Replace all `.get()` calls on Optional with safe methods
4. ✅ Create custom exception classes for domain errors
5. ✅ Add try-catch blocks with user-friendly error messages
6. ✅ Use `Objects.requireNonNull()` for method parameters

**Estimated Fix Time:** 2 hours

---

## 🟠 Issue 4.2: Incomplete Transaction Management

**Priority:** HIGH
**Location:** `BookcaseService.java:25-50`

### Current Code

```java
// BookcaseService.java - Lines 25-50
public String createNewBookCase(String label, int capacity) {
    BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);

    if (bookcaseEntity != null) {
        log.error("Failed to save Record - Record already exist", existingRecordError);
        throw existingRecordError;  // ❌ Exception as field instead of new instance
    } else {
        bookcaseEntity = new BookcaseEntity(label, capacity);
        bookcaseRepository.save(bookcaseEntity);  // ✅ Saved to database

        for (int i = 0; i < bookcaseEntity.getShelfCapacity(); i++) {
            addShelf(bookcaseEntity, i, i);  // ❌ What if this fails?
        }
        // If shelf creation fails, bookcase is already committed!
        // Database left in inconsistent state!

        return "Success! " + label + " created!";
    }
}
```

### Why It's Problematic

**The Transaction Problem:**

```
Scenario: Create bookcase with 5 shelves

Step 1: Create bookcase ✅
  → INSERT INTO bookcases (label, capacity) VALUES ('Fiction', 5)
  → Bookcase saved to database (COMMITTED)

Step 2: Create shelf 1 ✅
  → INSERT INTO shelves (bookcase_id, position) VALUES (1, 0)

Step 3: Create shelf 2 ✅
  → INSERT INTO shelves (bookcase_id, position) VALUES (1, 1)

Step 4: Create shelf 3 ❌ FAILS
  → Database constraint violation
  → Exception thrown

Result:
  → Database has bookcase with only 2 shelves instead of 5
  → Data corruption!
  → No rollback - bookcase and 2 shelves remain
```

**What Should Happen:**

```
All-or-nothing with @Transactional:

Step 1-4: Create bookcase + all shelves
  → If ALL succeed: COMMIT all changes
  → If ANY fail: ROLLBACK all changes (including bookcase)

Result:
  → Either complete bookcase with 5 shelves
  → OR nothing at all (no partial data)
```

### ACID Properties (What Transactions Guarantee)

**ACID = Atomicity, Consistency, Isolation, Durability**

```java
// Without @Transactional:
public void createBookcase() {
    save(bookcase);    // Committed immediately
    save(shelf1);      // Committed immediately
    save(shelf2);      // Committed immediately
    save(shelf3);      // FAILS - previous saves already committed!
    // Result: Partial data in database
}

// With @Transactional:
@Transactional
public void createBookcase() {
    save(bookcase);    // Not committed yet
    save(shelf1);      // Not committed yet
    save(shelf2);      // Not committed yet
    save(shelf3);      // FAILS - all changes rolled back!
    // Result: Nothing saved - database unchanged
}
```

### Additional Problems in Current Code

**1. No Input Validation:**

```java
// ❌ What if label is null? Blank? Too long?
// ❌ What if capacity is negative? Zero? Too large?
public String createNewBookCase(String label, int capacity) {
    // No validation!
}
```

**2. Exception as Field:**

```java
// ❌ Never reuse exception instances!
throw existingRecordError;  // Same instance thrown every time

// ✅ Create new exception with contextual information
throw new BookcaseAlreadyExistsException(
    "Bookcase with label '" + label + "' already exists"
);
```

**3. No Return Value:**

```java
// ❌ Returns String message - can't get created entity
return "Success! " + label + " created!";

// ✅ Return created entity
return bookcaseEntity;
```

### Correct Approach

**Complete Implementation with Transaction Management:**

```java
/**
 * Service for managing bookcases in the library.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookcaseService {

    private final BookcaseRepository bookcaseRepository;
    private final ShelfRepository shelfRepository;

    /**
     * Creates a new bookcase with specified number of shelves.
     *
     * <p>This operation is atomic - either the bookcase and ALL its shelves
     * are created, or nothing is persisted to the database.
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * BookcaseEntity bookcase = bookcaseService.createBookcase("Fiction", 5);
     * // Bookcase created with 5 shelves, or exception thrown
     * }</pre>
     *
     * @param label unique label for the bookcase (1-100 characters, cannot be blank)
     * @param shelfCount number of shelves to create (1-50)
     * @return the created bookcase entity
     * @throws IllegalArgumentException if input validation fails
     * @throws BookcaseAlreadyExistsException if bookcase with label already exists
     * @throws DataAccessException if database operation fails
     */
    @Transactional  // ✅ All-or-nothing transaction
    public BookcaseEntity createBookcase(String label, int shelfCount) {
        // Step 1: Validate inputs BEFORE any database operations
        validateBookcaseInput(label, shelfCount);

        // Step 2: Check uniqueness
        if (bookcaseRepository.existsByBookcaseLabelIgnoreCase(label)) {
            throw new BookcaseAlreadyExistsException(
                String.format("Bookcase with label '%s' already exists", label)
            );
        }

        // Step 3: Create bookcase (not committed yet)
        BookcaseEntity bookcase = new BookcaseEntity();
        bookcase.setBookcaseLabel(label);
        bookcase.setShelfCapacity(shelfCount);
        bookcaseRepository.save(bookcase);

        log.info("Creating bookcase '{}' with {} shelves", label, shelfCount);

        // Step 4: Create shelves (not committed yet)
        List<ShelfEntity> createdShelves = new ArrayList<>();
        for (int position = 0; position < shelfCount; position++) {
            ShelfEntity shelf = createShelf(bookcase, position);
            createdShelves.add(shelf);
        }

        // Step 5: If we reach here, all operations succeeded
        // Spring commits transaction when method returns normally
        log.info("Successfully created bookcase '{}' with {} shelves",
                label, createdShelves.size());

        return bookcase;
        // If any step fails:
        // - Spring automatically rolls back transaction
        // - Nothing is saved to database
        // - Exception propagates to caller
    }

    /**
     * Validates bookcase creation input.
     *
     * @param label the bookcase label
     * @param shelfCount the number of shelves
     * @throws IllegalArgumentException if validation fails
     */
    private void validateBookcaseInput(String label, int shelfCount) {
        // Validate label
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Bookcase label cannot be blank");
        }

        String trimmedLabel = label.trim();
        if (trimmedLabel.length() < 1 || trimmedLabel.length() > 100) {
            throw new IllegalArgumentException(
                String.format("Bookcase label must be 1-100 characters, got: %d",
                    trimmedLabel.length())
            );
        }

        // Validate shelf count
        if (shelfCount <= 0) {
            throw new IllegalArgumentException(
                String.format("Shelf count must be positive, got: %d", shelfCount)
            );
        }

        if (shelfCount > 50) {  // Business rule: max 50 shelves per bookcase
            throw new IllegalArgumentException(
                String.format("Shelf count cannot exceed 50, got: %d", shelfCount)
            );
        }
    }

    /**
     * Creates a single shelf for a bookcase.
     *
     * @param bookcase the bookcase to add shelf to
     * @param position the shelf position (0-based)
     * @return the created shelf
     */
    private ShelfEntity createShelf(BookcaseEntity bookcase, int position) {
        ShelfEntity shelf = new ShelfEntity();
        shelf.setBookcaseId(bookcase.getBookcaseId());
        shelf.setShelfLabel("Shelf " + (position + 1));  // User-friendly: 1-based
        shelf.setShelfPosition(position);  // Internal: 0-based

        return shelfRepository.save(shelf);
    }

    /**
     * Finds a bookcase by label (case-insensitive).
     *
     * @param label the bookcase label
     * @return Optional containing bookcase if found
     */
    @Transactional(readOnly = true)  // ✅ Read-only transaction for queries
    public Optional<BookcaseEntity> findByLabel(String label) {
        return bookcaseRepository.findByBookcaseLabelIgnoreCase(label);
    }

    /**
     * Deletes a bookcase and all its shelves.
     *
     * <p>This is a cascade delete - all shelves in the bookcase are also deleted.
     * Books on the shelves have their shelfId set to null (not deleted).
     *
     * @param bookcaseId the bookcase ID
     * @throws BookcaseNotFoundException if bookcase doesn't exist
     */
    @Transactional
    public void deleteBookcase(Long bookcaseId) {
        BookcaseEntity bookcase = bookcaseRepository.findById(bookcaseId)
            .orElseThrow(() -> new BookcaseNotFoundException(bookcaseId));

        // Delete all shelves (cascade)
        List<ShelfEntity> shelves = shelfRepository.findByBookcaseId(bookcaseId);
        shelfRepository.deleteAll(shelves);

        // Delete bookcase
        bookcaseRepository.delete(bookcase);

        log.info("Deleted bookcase {} and {} shelves",
                bookcase.getBookcaseLabel(), shelves.size());
    }
}

/**
 * Custom exception for duplicate bookcase labels.
 */
public class BookcaseAlreadyExistsException extends RuntimeException {
    public BookcaseAlreadyExistsException(String message) {
        super(message);
    }
}

/**
 * Custom exception for bookcase not found.
 */
public class BookcaseNotFoundException extends RuntimeException {
    public BookcaseNotFoundException(Long id) {
        super("Bookcase with ID " + id + " not found");
    }
}
```

### Transaction Isolation Levels

```java
/**
 * Different isolation levels for different scenarios.
 */
@Service
public class BookService {

    // Default isolation (READ_COMMITTED)
    @Transactional
    public void standardOperation() {
        // Prevents dirty reads
        // Allows non-repeatable reads
    }

    // Prevent non-repeatable reads
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void consistentReads() {
        // Ensures repeated queries return same results
        // Good for reports and multi-step operations
    }

    // Highest isolation (prevents phantom reads)
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void criticalOperation() {
        // Complete isolation (like running sequentially)
        // Slowest but safest
        // Use for financial transactions, inventory updates
    }

    // Read-only optimization
    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        // Tells database this is read-only
        // Allows optimizations
        // No flush/dirty checking
    }
}
```

### Transaction Propagation

```java
@Service
public class BookService {

    // Creates new transaction (or uses existing)
    @Transactional(propagation = Propagation.REQUIRED)  // Default
    public void createBook() {
        // Uses existing transaction if called from transactional method
        // Creates new transaction if called from non-transactional method
    }

    // Always creates new transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditLog() {
        // Creates new transaction even if caller has transaction
        // Audit log saved even if parent transaction rolls back
        // Use for logging, auditing
    }

    // Only works within existing transaction
    @Transactional(propagation = Propagation.MANDATORY)
    public void mustHaveTransaction() {
        // Throws exception if called without transaction
        // Use for operations that must be part of larger transaction
    }

    // Suspends current transaction
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void nonTransactional() {
        // Runs without transaction
        // Use for operations that don't need transactions
    }
}
```

### Rollback Behavior

```java
@Service
public class BookService {

    // Rolls back on RuntimeException (default)
    @Transactional
    public void defaultRollback() {
        // Rolls back on:
        // - RuntimeException and subclasses
        // - Error
        // Does NOT roll back on checked exceptions!
    }

    // Rollback on specific exceptions
    @Transactional(rollbackFor = {IOException.class, SQLException.class})
    public void rollbackOnCheckedExceptions() {
        // Rolls back on specified checked exceptions
    }

    // Don't rollback on specific exceptions
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void dontRollbackOnValidationError() {
        // Doesn't rollback for IllegalArgumentException
        // Useful for validation errors that don't corrupt data
    }
}
```

### Testing Transactional Behavior

```java
@SpringBootTest
@Transactional  // Rolls back after each test
class BookcaseServiceTest {

    @Autowired
    private BookcaseService bookcaseService;

    @Autowired
    private BookcaseRepository bookcaseRepository;

    @Autowired
    private ShelfRepository shelfRepository;

    @Test
    void shouldCreateBookcaseWithAllShelves() {
        // Arrange
        String label = "Test Bookcase";
        int shelfCount = 5;

        // Act
        BookcaseEntity bookcase = bookcaseService.createBookcase(label, shelfCount);

        // Assert
        assertNotNull(bookcase.getBookcaseId());
        assertEquals(label, bookcase.getBookcaseLabel());

        List<ShelfEntity> shelves = shelfRepository.findByBookcaseId(bookcase.getBookcaseId());
        assertEquals(shelfCount, shelves.size());
    }

    @Test
    void shouldRollbackWhenShelfCreationFails() {
        // Arrange
        String label = "Test Bookcase";

        // Mock shelf repository to fail on 3rd shelf
        // (In real test, you'd need to set up a scenario that causes failure)

        // Act & Assert
        assertThrows(DataAccessException.class,
            () -> bookcaseService.createBookcase(label, 5));

        // Verify nothing was saved (transaction rolled back)
        assertFalse(bookcaseRepository.existsByBookcaseLabelIgnoreCase(label));
        assertEquals(0, shelfRepository.findAll().size());
    }

    @Test
    void shouldThrowExceptionForDuplicateLabel() {
        // Arrange
        String label = "Duplicate";
        bookcaseService.createBookcase(label, 3);

        // Act & Assert
        assertThrows(BookcaseAlreadyExistsException.class,
            () -> bookcaseService.createBookcase(label, 3));
    }

    @Test
    void shouldValidateInput() {
        assertThrows(IllegalArgumentException.class,
            () -> bookcaseService.createBookcase(null, 5));

        assertThrows(IllegalArgumentException.class,
            () -> bookcaseService.createBookcase("", 5));

        assertThrows(IllegalArgumentException.class,
            () -> bookcaseService.createBookcase("Valid", 0));

        assertThrows(IllegalArgumentException.class,
            () -> bookcaseService.createBookcase("Valid", -1));

        assertThrows(IllegalArgumentException.class,
            () -> bookcaseService.createBookcase("Valid", 100));  // Exceeds max
    }
}
```

### Learning Principle

> **@Transactional ensures atomicity.** Use `@Transactional` on service methods that perform multiple database operations that must succeed or fail together. Validate inputs BEFORE starting transactions. Use `readOnly=true` for query methods (performance optimization). Create domain-specific exceptions with helpful messages. Remember: transactions incur overhead - don't make them too large or too small.

### Action Items

1. ✅ Add `@Transactional` to `createNewBookCase()` method
2. ✅ Add input validation before database operations
3. ✅ Create custom `BookcaseAlreadyExistsException`
4. ✅ Replace exception field with new exception instances
5. ✅ Change return type from String to BookcaseEntity
6. ✅ Add `@Transactional(readOnly=true)` to query methods
7. ✅ Write tests for transaction rollback behavior

**Estimated Fix Time:** 2 hours

---

## 🟡 Issue 4.3: Unsafe Type Parsing Without Validation

**Priority:** MEDIUM
**Locations:**
- `BookCommands.java:109` - Parsing author count
- `BookCommands.java:234` - Parsing bookcase ID
- `BookCommands.java:254` - Parsing shelf ID

### Current Code

```java
// BookCommands.java - Line 109
int authorCount = Integer.parseInt(
    result.getContext().get("author_count", String.class)
);
// ❌ What if user enters "five" instead of "5"?
// ❌ What if user enters "999999999"?
// ❌ What if user enters "-1"?

// BookCommands.java - Line 234
Long bookCaseId = Long.parseLong(
    res.getContext().get("bookcase", String.class)
);

// BookCommands.java - Line 254
Long shelfId = Long.parseLong(
    res.getContext().get("bookshelf", String.class)
);
```

### Why It's Problematic

**User Input Can Be Anything:**

```
User enters "abc" as author count:
  → Integer.parseInt("abc")
    → NumberFormatException: For input string: "abc"
      → Stack trace displayed
        → User confused

User enters "2147483648" (Integer.MAX_VALUE + 1):
  → Integer.parseInt("2147483648")
    → NumberFormatException: Value out of range
      → Stack trace displayed

User enters "-5" as shelf count:
  → Integer.parseInt("-5")
    → Returns -5 (valid parse!)
      → But negative shelf count doesn't make sense
        → Logic error downstream
```

### The Problem with No Validation

```java
// Current code flow:
User input "abc"
  → parseInt() throws NumberFormatException
    → No catch block
      → Exception propagates up
        → Command fails with stack trace
          → User sees:
              java.lang.NumberFormatException: For input string: "abc"
                  at java.lang.Integer.parseInt(Integer.java:652)
                  at BookCommands.add(BookCommands.java:109)
              ...30 more lines of stack trace...
```

### Correct Approach (Already Covered in Issue 4.1)

See **Issue 4.1** for the complete `InputValidator` utility class. Here's a quick reference:

```java
// Using InputValidator utility
public void addBook() {
    try {
        // Validate and parse with helpful errors
        int authorCount = InputValidator.parseIntInRange(
            result.getContext().get("author_count", String.class),
            "Author count",
            1,   // min: at least 1 author
            10   // max: no more than 10 authors
        );

        Long bookcaseId = InputValidator.parsePositiveId(
            res.getContext().get("bookcase", String.class),
            "Bookcase ID"
        );

        Long shelfId = InputValidator.parsePositiveId(
            res.getContext().get("bookshelf", String.class),
            "Shelf ID"
        );

        // All validated - safe to use
        processBookAddition(authorCount, bookcaseId, shelfId);

    } catch (IllegalArgumentException e) {
        // User-friendly error message
        System.err.println("❌ " + e.getMessage());
        System.err.println("Please try again with valid input.");
    }
}
```

### Learning Principle

> **Never parse user input without validation and error handling.** Use try-catch blocks around all parsing operations. Validate ranges and business rules. Provide helpful error messages that guide users to correct input. Create reusable validation utilities to avoid code duplication.

### Action Items

1. ✅ Use `InputValidator` from Issue 4.1 for all parsing
2. ✅ Wrap all parseInt/parseLong calls in try-catch
3. ✅ Validate ranges (positive numbers, reasonable limits)
4. ✅ Provide helpful error messages

**Estimated Fix Time:** 30 minutes (if using InputValidator)

---

## 🟡 Issue 4.4: Type Mismatch - String vs Enum for Status

**Priority:** MEDIUM
**Locations:**
- `BookEntity.java` - Uses String for status
- `Book.java` - Uses BookStatus enum
- `BookService.java:64` - Comparing String with enum

### Current Code

```java
// BookEntity.java
@Entity
public class BookEntity {
    private String bookStatus;  // ❌ String allows invalid values
}

// Book.java
public class Book {
    private BookStatus status;  // ✅ Enum is type-safe
}

// BookService.java - Line 64
public void checkOutBook(BookEntity bookEntity) {
    if (!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())) {
        bookEntity.setBookStatus("CHECKED_OUT");  // ❌ Magic string!
        bookRepository.save(bookEntity);
    }
}
```

### Why It's Problematic

**Strings Allow Invalid Values:**

```java
// ❌ With String - anything goes:
book.setBookStatus("AVAILABLE");       // OK
book.setBookStatus("CHECKED_OUT");     // OK
book.setBookStatus("available");       // Compiles! Wrong case
book.setBookStatus("CHECKED OUT");     // Compiles! Space instead of underscore
book.setBookStatus("BANANA");          // Compiles! Completely wrong
book.setBookStatus(null);              // Compiles! NullPointerException later
book.setBookStatus("");                // Compiles! Empty string

// ✅ With Enum - only valid values:
book.setStatus(BookStatus.AVAILABLE);      // OK
book.setStatus(BookStatus.CHECKED_OUT);    // OK
book.setStatus(BookStatus.BANANA);         // ❌ Compilation error! Type safety
```

**Comparing Strings with Enums is Error-Prone:**

```java
// Current code:
if (!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())) {
    // What if bookStatus is:
    // - null? NullPointerException!
    // - "checked_out"? Doesn't match (case sensitive)
    // - "CHECKED OUT"? Doesn't match (space vs underscore)
}
```

### The Cost of Using Strings for Enums

```
Problems with String-based status:

1. No compile-time safety
   → Typos not caught until runtime
   → "AVAILBLE" instead of "AVAILABLE" compiles fine

2. Inconsistent casing
   → "available", "AVAILABLE", "Available" all compile
   → Comparison fails, bugs in production

3. No IDE autocomplete
   → Must remember exact strings
   → Easy to make mistakes

4. Hard to refactor
   → Changing "CHECKED_OUT" to "ON_LOAN" requires finding all strings
   → With enum, rename refactoring updates all uses

5. Database allows garbage
   → Can insert invalid status directly in DB
   → No constraint enforcement
```

### Correct Approach

**Convert BookEntity to Use Enum:**

```java
// BookEntity.java - FIXED
@Entity
@Table(name = "books")
@Data
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    private String title;

    @Enumerated(EnumType.STRING)  // ✅ Store as string in DB ("AVAILABLE", "CHECKED_OUT")
    @Column(name = "status", nullable = false)
    private BookStatus status;  // ✅ Type-safe enum

    @ManyToOne
    private AuthorEntity author;

    private Long shelfId;
}

// BookStatus.java
public enum BookStatus {
    AVAILABLE,
    CHECKED_OUT,
    RESERVED,
    LOST,
    DAMAGED,
    IN_REPAIR;

    /**
     * Checks if book can be checked out.
     */
    public boolean isAvailableForCheckout() {
        return this == AVAILABLE || this == RESERVED;
    }

    /**
     * Checks if book is currently with a user.
     */
    public boolean isCheckedOut() {
        return this == CHECKED_OUT;
    }
}

// BookService.java - FIXED
@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    @Transactional
    public void checkoutBook(BookEntity book) {
        // ✅ Type-safe enum comparison
        if (book.getStatus() == BookStatus.CHECKED_OUT) {
            throw new BookAlreadyCheckedOutException(
                String.format("Book '%s' is already checked out", book.getTitle())
            );
        }

        // ✅ Type-safe enum assignment
        book.setStatus(BookStatus.CHECKED_OUT);
        book.setCheckoutDate(LocalDateTime.now());
        book.setDueDate(LocalDateTime.now().plusDays(14));

        bookRepository.save(book);
    }

    @Transactional
    public void checkinBook(BookEntity book) {
        if (book.getStatus() != BookStatus.CHECKED_OUT) {
            throw new InvalidOperationException(
                String.format("Book '%s' is not checked out", book.getTitle())
            );
        }

        book.setStatus(BookStatus.AVAILABLE);
        book.setCheckoutDate(null);
        book.setDueDate(null);

        bookRepository.save(book);
    }

    public List<BookEntity> findAvailableBooks() {
        // ✅ Type-safe query
        return bookRepository.findByStatus(BookStatus.AVAILABLE);
    }
}
```

### Database Migration Script

```sql
-- Step 1: Add new column with enum type
ALTER TABLE books
ADD COLUMN status VARCHAR(20);

-- Step 2: Migrate existing data
UPDATE books
SET status = CASE book_status
    WHEN 'CHECKED_OUT' THEN 'CHECKED_OUT'
    WHEN 'AVAILABLE' THEN 'AVAILABLE'
    WHEN 'RESERVED' THEN 'RESERVED'
    WHEN 'checked_out' THEN 'CHECKED_OUT'  -- Handle case variations
    WHEN 'available' THEN 'AVAILABLE'
    ELSE 'AVAILABLE'  -- Default for unknown values
END;

-- Step 3: Make new column non-nullable
ALTER TABLE books
ALTER COLUMN status SET NOT NULL;

-- Step 4: Add check constraint for valid values
ALTER TABLE books
ADD CONSTRAINT book_status_check
CHECK (status IN ('AVAILABLE', 'CHECKED_OUT', 'RESERVED', 'LOST', 'DAMAGED', 'IN_REPAIR'));

-- Step 5: Drop old column
ALTER TABLE books
DROP COLUMN book_status;

-- Step 6: Verify migration
SELECT status, COUNT(*) FROM books GROUP BY status;
```

### Benefits of Enum

```java
// 1. Compile-time safety
book.setStatus(BookStatus.AVAILABLE);  // ✅ OK
book.setStatus("AVAILABLE");           // ❌ Compilation error

// 2. IDE autocomplete
book.setStatus(BookStatus.  // IDE shows: AVAILABLE, CHECKED_OUT, RESERVED, etc.

// 3. Exhaustive switch (compiler warns if missing case)
switch (book.getStatus()) {
    case AVAILABLE -> handleAvailable();
    case CHECKED_OUT -> handleCheckedOut();
    case RESERVED -> handleReserved();
    // Compiler warns if you miss LOST, DAMAGED, IN_REPAIR
}

// 4. Enum methods
if (book.getStatus().isAvailableForCheckout()) {
    checkout(book);
}

// 5. Easy refactoring
// Rename CHECKED_OUT → ON_LOAN
// IDE updates all references automatically

// 6. Serialization (JSON, XML) works automatically
{
  "title": "Clean Code",
  "status": "AVAILABLE"  // Serialized as string
}
```

### JPA Enum Mapping Options

```java
// Option 1: EnumType.STRING (recommended)
@Enumerated(EnumType.STRING)
private BookStatus status;
// Stores: "AVAILABLE", "CHECKED_OUT" in database
// Pros: Human-readable, can add enum values, safe refactoring
// Cons: Uses more space than integer

// Option 2: EnumType.ORDINAL (avoid!)
@Enumerated(EnumType.ORDINAL)
private BookStatus status;
// Stores: 0, 1, 2 in database (based on enum order)
// Pros: Uses less space
// Cons: BREAKS if you reorder enum values, not human-readable

// Never use ORDINAL - if you add a value in middle of enum:
public enum BookStatus {
    AVAILABLE,     // Was 0, still 0 ✅
    NEW_STATUS,    // Now 1 (inserted in middle)
    CHECKED_OUT,   // Was 1, now 2 ❌ All existing data corrupted!
    RESERVED       // Was 2, now 3 ❌
}
```

### Learning Principle

> **Use enums for fixed sets of values.** Enums provide compile-time safety, prevent invalid values, enable IDE autocomplete, and make refactoring easier. Always use `@Enumerated(EnumType.STRING)` in JPA (never ORDINAL). Add database constraints to enforce valid values at the database level.

### Action Items

1. ✅ Change `BookEntity.bookStatus` from String to `BookStatus` enum
2. ✅ Add `@Enumerated(EnumType.STRING)` annotation
3. ✅ Create database migration script
4. ✅ Update all code using bookStatus String to use enum
5. ✅ Add database check constraint for valid status values
6. ✅ Add helper methods to enum (`isAvailableForCheckout()`, etc.)
7. ✅ Test migration with existing data

**Estimated Fix Time:** 1 hour (+ database migration)

---

## 📊 Summary Table

| Issue | Priority | Location | Fix Time | Impact |
|-------|----------|----------|----------|--------|
| Missing null checks | 🔴 Critical | BookCommands.java | 2 hours | Prevents crashes |
| Incomplete transactions | 🟠 High | BookcaseService.java | 2 hours | Data integrity |
| Unsafe type parsing | 🟡 Medium | BookCommands.java | 30 min | Better UX |
| String vs Enum | 🟡 Medium | BookEntity.java | 1 hour | Type safety |

**Total Estimated Time:** ~5.5 hours
**Expected Impact:** Significantly more robust, crash-resistant application

---

## ✅ Action Checklist

### Critical (Do This Week)
- [ ] Create `InputValidator` utility class (30 min)
- [ ] Add validation to all user input parsing (1 hour)
- [ ] Fix all unsafe Optional.get() calls (30 min)
- [ ] Add `@Transactional` to BookcaseService.createNewBookCase() (15 min)

### High Priority (This Month)
- [ ] Create custom exception classes (30 min)
  - [ ] BookNotFoundException
  - [ ] ShelfNotFoundException
  - [ ] BookcaseAlreadyExistsException
  - [ ] BookNotAvailableException
- [ ] Add input validation to BookcaseService (30 min)
- [ ] Convert BookEntity.bookStatus to enum (1 hour)
- [ ] Create database migration script (30 min)

### Medium Priority (Ongoing)
- [ ] Add try-catch blocks with user-friendly errors
- [ ] Use Objects.requireNonNull() for method parameters
- [ ] Add @Transactional(readOnly=true) to query methods
- [ ] Write tests for transaction rollback behavior

---

## 🎓 Key Principles

### Defensive Programming Rules

1. **Validate at the Boundary**
   - Check inputs as soon as they enter your system
   - Fail fast with meaningful errors

2. **Never Trust User Input**
   - Validate format, range, and business rules
   - Provide helpful error messages

3. **Use Type Safety**
   - Prefer enums over strings for fixed values
   - Use Optional instead of null

4. **Handle Errors Gracefully**
   - Don't crash - guide users to success
   - Log errors for debugging

5. **Transactions for Consistency**
   - Multiple operations → @Transactional
   - All succeed or all fail

---

## 📖 Quick Reference

### Input Validation Pattern

```java
public void operation(String input) {
    try {
        // 1. Validate
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input required");
        }

        // 2. Parse
        Long id = Long.parseLong(input.trim());

        // 3. Range check
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be positive");
        }

        // 4. Business logic
        process(id);

    } catch (NumberFormatException e) {
        System.err.println("Invalid number: " + input);
    } catch (IllegalArgumentException e) {
        System.err.println("Error: " + e.getMessage());
    }
}
```

### Transaction Pattern

```java
@Transactional
public Entity createWithRelations(DTO dto) {
    // 1. Validate
    validate(dto);

    // 2. Create main entity
    Entity entity = repository.save(new Entity(dto));

    // 3. Create related entities
    for (RelatedDTO related : dto.getRelated()) {
        createRelated(entity, related);
    }

    // 4. Return (commits if no exceptions)
    return entity;
}
```

---

**Recommended Next Section:**
- Section 5: Performance & Efficiency (N+1 queries, database optimization)
- Section 6: Testing Gaps
- Section 7: Security Concerns (builds on validation concepts)

Which would you like next?
