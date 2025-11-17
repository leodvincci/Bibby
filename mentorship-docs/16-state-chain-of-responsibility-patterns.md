# SECTION 16: STATE & CHAIN OF RESPONSIBILITY PATTERNS

## Behavioral Patterns for State Management and Request Handling

Welcome to Section 16! We're exploring two more powerful behavioral patterns: **State** and **Chain of Responsibility**.

**State** helps you manage objects that change behavior based on their internal state - perfect for your book's availability status (Available, Checked Out, Lost).

**Chain of Responsibility** lets you pass requests along a chain of handlers until one handles it - ideal for validation, filtering, and middleware.

Both patterns eliminate messy conditional logic and make your code cleaner and more extensible.

---

## The State Pattern

### What is the State Pattern?

**Definition**: Allow an object to alter its behavior when its internal state changes. The object will appear to change its class.

**Key Insight**: Instead of using conditionals to check state and behave differently, encapsulate each state's behavior in a separate class. The object delegates to its current state object.

### The Problem: State Conditionals Everywhere

Look at your current Book entity. It probably has code like this:

```java
public class Book {
    private AvailabilityStatus status;  // enum: AVAILABLE, CHECKED_OUT, LOST

    public void checkOut() {
        if (status == AvailabilityStatus.AVAILABLE) {
            status = AvailabilityStatus.CHECKED_OUT;
            incrementCheckoutCount();
        } else if (status == AvailabilityStatus.CHECKED_OUT) {
            throw new IllegalStateException("Book is already checked out");
        } else if (status == AvailabilityStatus.LOST) {
            throw new IllegalStateException("Book is lost");
        }
    }

    public void returnToLibrary() {
        if (status == AvailabilityStatus.CHECKED_OUT) {
            status = AvailabilityStatus.AVAILABLE;
        } else if (status == AvailabilityStatus.AVAILABLE) {
            throw new IllegalStateException("Book is already available");
        } else if (status == AvailabilityStatus.LOST) {
            status = AvailabilityStatus.AVAILABLE;  // Found it!
        }
    }

    public void reportLost() {
        if (status == AvailabilityStatus.AVAILABLE) {
            status = AvailabilityStatus.LOST;
        } else if (status == AvailabilityStatus.CHECKED_OUT) {
            status = AvailabilityStatus.LOST;
        } else if (status == AvailabilityStatus.LOST) {
            // Already lost
        }
    }

    public boolean isAvailable() {
        return status == AvailabilityStatus.AVAILABLE;
    }

    public String getStatusDisplay() {
        if (status == AvailabilityStatus.AVAILABLE) {
            return "📗 Available";
        } else if (status == AvailabilityStatus.CHECKED_OUT) {
            return "📕 Checked Out";
        } else if (status == AvailabilityStatus.LOST) {
            return "📙 Lost";
        }
        return "Unknown";
    }
}
```

**Problems**:
1. **Conditional logic everywhere**: Every method checks state
2. **Hard to add states**: Adding "RESERVED" requires changing every method
3. **State transitions implicit**: Rules are scattered across methods
4. **Hard to test**: Must test all state combinations

### The Solution: State Objects

With State pattern, each state is a class:

```java
public class Book {
    private AvailabilityState state = new AvailableState();

    public void checkOut() {
        state.checkOut(this);  // Delegate to state
    }

    public void returnToLibrary() {
        state.returnToLibrary(this);
    }

    void setState(AvailabilityState newState) {
        this.state = newState;
    }
}
```

Each state knows what it can do:
```java
public class AvailableState implements AvailabilityState {
    @Override
    public void checkOut(Book book) {
        book.setState(new CheckedOutState());  // Transition
        book.incrementCheckoutCount();
    }

    @Override
    public void returnToLibrary(Book book) {
        throw new IllegalStateException("Book is already available");
    }
}
```

**Beautiful!** No conditionals. Each state encapsulates its own behavior.

---

## State Pattern Structure

### Components

1. **Context**: Object that changes behavior (Book)
2. **State Interface**: Common interface for all states
3. **Concrete States**: Specific state implementations
4. **State Transitions**: States change the context's state

### UML Diagram

```
┌──────────────────┐
│      Book        │  (Context)
├──────────────────┤
│- state           │──uses──>┌─────────────────┐
│+ checkOut()      │          │AvailabilityState│
│+ returnToLibrary │          ├─────────────────┤
│+ reportLost()    │          │+ checkOut()     │
└──────────────────┘          │+ returnTo()     │
                               │+ reportLost()   │
                               └────────┬────────┘
                                        │
                      ┌─────────────────┼─────────────────┐
                      │                 │                 │
              ┌───────▼──────┐  ┌──────▼──────┐  ┌──────▼─────┐
              │AvailableState│  │CheckedOutState│ │ LostState  │
              ├──────────────┤  ├─────────────┤  ├────────────┤
              │+ checkOut()  │  │+ checkOut() │  │+ checkOut()│
              │+ returnTo()  │  │+ returnTo() │  │+ returnTo()│
              │+ reportLost()│  │+ reportLost()│  │+ reportLost()│
              └──────────────┘  └─────────────┘  └────────────┘
```

---

## Implementing State in Bibby

### Step 1: State Interface

```java
package com.penrose.bibby.library.book.state;

public interface AvailabilityState {
    /**
     * Attempt to check out the book
     * @throws IllegalStateException if checkout not allowed in this state
     */
    void checkOut(Book book);

    /**
     * Attempt to return the book to library
     * @throws IllegalStateException if return not allowed in this state
     */
    void returnToLibrary(Book book);

    /**
     * Report the book as lost
     */
    void reportLost(Book book);

    /**
     * Get the display name of this state
     */
    String getStatusName();

    /**
     * Get the display icon for this state
     */
    String getStatusIcon();

    /**
     * Can this book be checked out in this state?
     */
    boolean isAvailable();
}
```

### Step 2: Concrete State - Available

```java
package com.penrose.bibby.library.book.state;

public class AvailableState implements AvailabilityState {

    @Override
    public void checkOut(Book book) {
        // Valid transition: Available → Checked Out
        book.setState(new CheckedOutState());
        book.incrementCheckoutCount();
        book.setLastCheckoutDate(LocalDateTime.now());

        log.info("Book {} checked out", book.getId());
    }

    @Override
    public void returnToLibrary(Book book) {
        // Invalid: book is already available
        throw new IllegalStateException(
            "Cannot return book " + book.getId() + " - it is already available"
        );
    }

    @Override
    public void reportLost(Book book) {
        // Valid transition: Available → Lost
        book.setState(new LostState());
        log.warn("Book {} reported lost while available", book.getId());
    }

    @Override
    public String getStatusName() {
        return "AVAILABLE";
    }

    @Override
    public String getStatusIcon() {
        return "📗";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
```

### Step 3: Concrete State - Checked Out

```java
package com.penrose.bibby.library.book.state;

public class CheckedOutState implements AvailabilityState {

    @Override
    public void checkOut(Book book) {
        // Invalid: book is already checked out
        throw new IllegalStateException(
            "Cannot check out book " + book.getId() + " - it is already checked out"
        );
    }

    @Override
    public void returnToLibrary(Book book) {
        // Valid transition: Checked Out → Available
        book.setState(new AvailableState());
        book.setLastReturnDate(LocalDateTime.now());

        log.info("Book {} returned to library", book.getId());
    }

    @Override
    public void reportLost(Book book) {
        // Valid transition: Checked Out → Lost
        book.setState(new LostState());
        log.warn("Book {} reported lost while checked out", book.getId());
    }

    @Override
    public String getStatusName() {
        return "CHECKED_OUT";
    }

    @Override
    public String getStatusIcon() {
        return "📕";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
```

### Step 4: Concrete State - Lost

```java
package com.penrose.bibby.library.book.state;

public class LostState implements AvailabilityState {

    @Override
    public void checkOut(Book book) {
        // Invalid: can't check out a lost book
        throw new IllegalStateException(
            "Cannot check out book " + book.getId() + " - it is lost"
        );
    }

    @Override
    public void returnToLibrary(Book book) {
        // Valid transition: Lost → Available (found it!)
        book.setState(new AvailableState());
        log.info("Lost book {} has been found and returned", book.getId());
    }

    @Override
    public void reportLost(Book book) {
        // Already lost - no-op
        log.debug("Book {} is already marked as lost", book.getId());
    }

    @Override
    public String getStatusName() {
        return "LOST";
    }

    @Override
    public String getStatusIcon() {
        return "📙";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
```

### Step 5: Context (Book)

```java
package com.penrose.bibby.library.book;

public class Book extends AggregateRoot<BookId> {
    private AvailabilityState state = new AvailableState();
    private int checkoutCount = 0;
    private LocalDateTime lastCheckoutDate;
    private LocalDateTime lastReturnDate;

    // Public methods delegate to state
    public void checkOut() {
        state.checkOut(this);  // State handles transition
        registerEvent(new BookCheckedOutEvent(this.id));
    }

    public void returnToLibrary() {
        state.returnToLibrary(this);
        registerEvent(new BookReturnedEvent(this.id));
    }

    public void reportLost() {
        state.reportLost(this);
        registerEvent(new BookLostEvent(this.id));
    }

    public boolean isAvailable() {
        return state.isAvailable();
    }

    public String getStatusDisplay() {
        return state.getStatusIcon() + " " + state.getStatusName();
    }

    // Package-private: only states can call this
    void setState(AvailabilityState newState) {
        this.state = newState;
    }

    void incrementCheckoutCount() {
        this.checkoutCount++;
    }

    void setLastCheckoutDate(LocalDateTime date) {
        this.lastCheckoutDate = date;
    }

    void setLastReturnDate(LocalDateTime date) {
        this.lastReturnDate = date;
    }
}
```

### Step 6: Usage

```java
Book book = new Book(/* ... */);

// Book starts in Available state
System.out.println(book.getStatusDisplay());  // "📗 AVAILABLE"

// Check out - transitions to CheckedOut
book.checkOut();
System.out.println(book.getStatusDisplay());  // "📕 CHECKED_OUT"

// Try to check out again - throws exception
try {
    book.checkOut();
} catch (IllegalStateException e) {
    System.out.println(e.getMessage());  // "Cannot check out book ... already checked out"
}

// Return - transitions to Available
book.returnToLibrary();
System.out.println(book.getStatusDisplay());  // "📗 AVAILABLE"

// Report lost - transitions to Lost
book.reportLost();
System.out.println(book.getStatusDisplay());  // "📙 LOST"

// Return found book - transitions to Available
book.returnToLibrary();
System.out.println(book.getStatusDisplay());  // "📗 AVAILABLE"
```

---

## State Transitions Diagram

```
        ┌──────────┐
   ┌───>│AVAILABLE │<────┐
   │    └────┬─────┘     │
   │         │           │
   │    checkOut()   returnToLibrary()
   │         │           │
   │    ┌────▼──────┐    │
   │    │CHECKED_OUT│────┘
   │    └────┬──────┘
   │         │
   │    reportLost()
   │         │
   │    ┌────▼────┐
   └────│  LOST   │
        └─────────┘
     returnToLibrary()
```

---

## Benefits of State Pattern

### 1. Eliminates Conditionals
No more `if/else` or `switch` on state. Each state handles its own behavior.

### 2. Single Responsibility
Each state class has one job: manage behavior for that state.

### 3. Open/Closed Principle
Add new states without modifying existing code.

### 4. Explicit State Transitions
State transitions are clear: `book.setState(new CheckedOutState())`.

### 5. Easy to Test
Test each state independently. Mock the context.

---

## When to Use State

**Use State when:**
1. Object behavior depends on its state
2. You have many conditionals checking state
3. State transitions are complex
4. You need to add new states frequently

**Real-world examples:**
- TCP connections (Established, Listening, Closed)
- Document workflow (Draft, Review, Approved, Published)
- Order status (Pending, Shipped, Delivered, Cancelled)
- Your book availability!

---

## The Chain of Responsibility Pattern

### What is the Chain of Responsibility Pattern?

**Definition**: Avoid coupling the sender of a request to its receiver by giving more than one object a chance to handle the request. Chain the receiving objects and pass the request along the chain until an object handles it.

**Key Insight**: Instead of one object handling a request, create a chain of handlers. Each handler decides whether to process the request or pass it to the next handler.

### The Problem: Tightly Coupled Request Handling

Suppose you want to validate a book before saving. You need to check:
1. Title is not blank
2. ISBN is valid
3. At least one author
4. ISBN is unique
5. Publication year is reasonable

**Without Chain of Responsibility**:
```java
public void validateBook(Book book) {
    // Check 1
    if (book.getTitle() == null || book.getTitle().getValue().isBlank()) {
        throw new ValidationException("Title is required");
    }

    // Check 2
    if (book.getIsbn() == null) {
        throw new ValidationException("ISBN is required");
    }
    if (!book.getIsbn().isValid()) {
        throw new ValidationException("ISBN format is invalid");
    }

    // Check 3
    if (book.getAuthorIds().isEmpty()) {
        throw new ValidationException("At least one author is required");
    }

    // Check 4
    if (bookRepository.existsByIsbn(book.getIsbn())) {
        throw new ValidationException("ISBN already exists");
    }

    // Check 5
    int year = book.getPublicationYear();
    if (year < 1000 || year > LocalDate.now().getYear()) {
        throw new ValidationException("Invalid publication year");
    }
}
```

**Problems**:
1. **Long method**: All validations in one place
2. **Hard to reuse**: Can't reuse individual validations
3. **Hard to extend**: Adding validation requires modifying this method
4. **No flexibility**: Can't skip or reorder validations

### The Solution: Validation Chain

With Chain of Responsibility, each validation is a handler:

```java
ValidationHandler chain = new TitleValidator();
chain.setNext(new IsbnValidator())
     .setNext(new AuthorValidator())
     .setNext(new UniqueIsbnValidator())
     .setNext(new PublicationYearValidator());

ValidationResult result = chain.validate(book);
if (!result.isValid()) {
    throw new ValidationException(result.getError());
}
```

Each handler validates one thing and passes to the next.

---

## Chain of Responsibility Pattern Structure

### Components

1. **Handler Interface**: Common interface for all handlers
2. **Concrete Handlers**: Specific handlers that process or pass requests
3. **Chain Setup**: Linking handlers together
4. **Client**: Initiates the request to the first handler

### UML Diagram

```
┌──────────────────┐
│ValidationHandler │  (Handler Interface)
├──────────────────┤
│- next: Handler   │
│+ validate(book)  │
│+ setNext(handler)│
└────────┬─────────┘
         │
         │ implements
         │
    ┌────┴───────────────┬──────────────┬───────────────┐
    │                    │              │               │
┌───▼──────┐  ┌──────────▼───┐  ┌──────▼─────┐  ┌─────▼────┐
│Title     │  │ ISBN         │  │Author      │  │UniqueIsbn│
│Validator │  │ Validator    │  │Validator   │  │Validator │
└──────────┘  └──────────────┘  └────────────┘  └──────────┘
```

---

## Implementing Chain of Responsibility in Bibby

### Step 1: Handler Interface

```java
package com.penrose.bibby.library.book.validation;

public interface ValidationHandler {
    /**
     * Validate the book
     * @return ValidationResult indicating success or failure
     */
    ValidationResult validate(Book book);

    /**
     * Set the next handler in the chain
     * @return the next handler (for fluent chaining)
     */
    ValidationHandler setNext(ValidationHandler next);
}
```

### Step 2: Abstract Handler Base Class

```java
package com.penrose.bibby.library.book.validation;

public abstract class AbstractValidationHandler implements ValidationHandler {
    private ValidationHandler next;

    @Override
    public ValidationHandler setNext(ValidationHandler next) {
        this.next = next;
        return next;  // Return next for fluent chaining
    }

    /**
     * Validate in this handler, then pass to next
     */
    protected ValidationResult validateNext(Book book) {
        if (next != null) {
            return next.validate(book);
        }
        return ValidationResult.valid();  // End of chain
    }
}
```

### Step 3: Concrete Handlers

```java
public class TitleValidator extends AbstractValidationHandler {
    @Override
    public ValidationResult validate(Book book) {
        if (book.getTitle() == null || book.getTitle().getValue().isBlank()) {
            return ValidationResult.invalid("Title is required and cannot be blank");
        }

        if (book.getTitle().getValue().length() > 500) {
            return ValidationResult.invalid("Title cannot exceed 500 characters");
        }

        // Pass to next validator
        return validateNext(book);
    }
}
```

```java
public class IsbnValidator extends AbstractValidationHandler {
    @Override
    public ValidationResult validate(Book book) {
        if (book.getIsbn() == null) {
            return ValidationResult.invalid("ISBN is required");
        }

        if (!book.getIsbn().isValid()) {
            return ValidationResult.invalid(
                "ISBN format is invalid: " + book.getIsbn().getValue()
            );
        }

        return validateNext(book);
    }
}
```

```java
public class AuthorValidator extends AbstractValidationHandler {
    @Override
    public ValidationResult validate(Book book) {
        if (book.getAuthorIds() == null || book.getAuthorIds().isEmpty()) {
            return ValidationResult.invalid("At least one author is required");
        }

        if (book.getAuthorIds().size() > 20) {
            return ValidationResult.invalid("Cannot have more than 20 authors");
        }

        return validateNext(book);
    }
}
```

```java
public class UniqueIsbnValidator extends AbstractValidationHandler {
    private final BookRepository bookRepository;

    public UniqueIsbnValidator(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public ValidationResult validate(Book book) {
        if (bookRepository.existsByIsbn(book.getIsbn())) {
            return ValidationResult.invalid(
                "A book with ISBN " + book.getIsbn().getValue() + " already exists"
            );
        }

        return validateNext(book);
    }
}
```

```java
public class PublicationYearValidator extends AbstractValidationHandler {
    @Override
    public ValidationResult validate(Book book) {
        int year = book.getPublicationYear();
        int currentYear = LocalDate.now().getYear();

        if (year < 1000) {
            return ValidationResult.invalid(
                "Publication year cannot be before 1000"
            );
        }

        if (year > currentYear) {
            return ValidationResult.invalid(
                "Publication year cannot be in the future"
            );
        }

        return validateNext(book);
    }
}
```

### Step 4: Validation Result

```java
package com.penrose.bibby.library.book.validation;

public class ValidationResult {
    private final boolean valid;
    private final String error;

    private ValidationResult(boolean valid, String error) {
        this.valid = valid;
        this.error = error;
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(String error) {
        return new ValidationResult(false, error);
    }

    public boolean isValid() {
        return valid;
    }

    public String getError() {
        return error;
    }
}
```

### Step 5: Building and Using the Chain

```java
@Service
public class BookValidationService {
    private final BookRepository bookRepository;
    private final ValidationHandler validationChain;

    public BookValidationService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;

        // Build the validation chain
        ValidationHandler chain = new TitleValidator();
        chain.setNext(new IsbnValidator())
             .setNext(new AuthorValidator())
             .setNext(new UniqueIsbnValidator(bookRepository))
             .setNext(new PublicationYearValidator());

        this.validationChain = chain;
    }

    public ValidationResult validate(Book book) {
        return validationChain.validate(book);
    }

    public void validateOrThrow(Book book) {
        ValidationResult result = validate(book);
        if (!result.isValid()) {
            throw new ValidationException(result.getError());
        }
    }
}
```

### Step 6: Usage

```java
@Service
public class BookApplicationService {
    private final BookValidationService validationService;
    private final BookRepository bookRepository;

    @Transactional
    public BookId addBook(AddBookCommand command) {
        Book book = Book.builder()
            .id(BookId.generate())
            .title(command.getTitle())
            .isbn(command.getIsbn())
            .authors(command.getAuthorIds())
            .publicationYear(command.getPublicationYear())
            .build();

        // Validate using chain
        validationService.validateOrThrow(book);

        // Save if valid
        bookRepository.save(book);

        return book.getId();
    }
}
```

---

## Chain Variants

### 1. **Stop on First Failure** (shown above)
First validator that fails stops the chain.

### 2. **Collect All Errors**
Chain continues, collecting all errors:

```java
public class ValidationChain {
    public List<String> validateAll(Book book) {
        List<String> errors = new ArrayList<>();

        ValidationHandler current = firstHandler;
        while (current != null) {
            ValidationResult result = current.validate(book);
            if (!result.isValid()) {
                errors.add(result.getError());
            }
            current = current.getNext();
        }

        return errors;
    }
}
```

### 3. **Optional Handling**
Each handler can choose to handle or skip:

```java
public interface RequestHandler {
    boolean canHandle(Request request);
    void handle(Request request);
}
```

---

## When to Use Chain of Responsibility

**Use Chain of Responsibility when:**
1. Multiple objects can handle a request
2. You don't know which handler will process the request
3. You want to decouple senders from receivers
4. You need dynamic handler composition

**Real-world examples:**
- Validation chains
- Logging frameworks (handlers for different log levels)
- Event bubbling in UI frameworks
- Middleware in web frameworks (Express.js, Spring Security filters)
- Exception handling

---

## Action Items

### 1. Implement State Pattern for Book Availability (3-4 hours)

Refactor your Book entity to use State pattern:
- Create `AvailabilityState` interface
- Implement `AvailableState`, `CheckedOutState`, `LostState`
- Add `setState()` method to Book
- Test all state transitions
- **Bonus**: Add `ReservedState` for books on hold

### 2. Build Validation Chain (2-3 hours)

Create validation chain for books:
- Implement all 5 validators shown above
- Build the chain in `BookValidationService`
- Test each validator independently
- Test the full chain

### 3. Add Logging Chain (2 hours)

Create a chain for logging with different severity levels:
```java
LogHandler errorHandler = new ErrorHandler();
errorHandler.setNext(new WarnHandler())
            .setNext(new InfoHandler())
            .setNext(new DebugHandler());
```

Each handler logs messages at its level and below.

### 4. Visualize State Transitions (1 hour)

Create a diagram showing all possible state transitions for books. Identify:
- Valid transitions
- Invalid transitions (should throw exceptions)
- Edge cases

---

## Key Takeaways

### 1. State Eliminates State Conditionals
- Each state is a class
- Context delegates to current state
- Clean, extensible code

### 2. Chain of Responsibility Decouples Request Processing
- Handlers linked in a chain
- Each handler processes or passes request
- Flexible, composable

### 3. Both Patterns Follow Open/Closed Principle
- State: Add new states without modifying context
- Chain: Add new handlers without modifying existing ones

### 4. State Manages Behavior, Chain Manages Processing
- State: Different behavior per state
- Chain: Sequential processing through handlers

### 5. Test Each Component Independently
- Test states in isolation
- Test handlers in isolation
- Integration test full chain/state machine

---

## Further Study

**Books**:
- "Design Patterns" by Gang of Four - State (p. 305), Chain of Responsibility (p. 223)
- "Head First Design Patterns" - Great visual explanations

**Articles**:
- Martin Fowler: "State Machine"
- Refactoring Guru: "State Pattern"
- Refactoring Guru: "Chain of Responsibility Pattern"

**Code Examples**:
- Java Servlet Filters (Chain of Responsibility)
- Spring Security Filter Chain
- TCP State Machine

---

## Mentor's Notes

**State** pattern is perfect when you have a state machine. Your book availability is a textbook example. Once you implement it, you'll wonder how you lived with all those conditionals.

**Chain of Responsibility** is everywhere in frameworks - logging, filters, middleware. Now you understand what's happening under the hood.

Both patterns make your code cleaner, more testable, and easier to extend. Use them!

Next: **Visitor, Iterator, and Memento** patterns - the final behavioral patterns!

---

**Section 16 Complete** | **Next**: Section 17 - Visitor, Iterator & Memento Patterns
