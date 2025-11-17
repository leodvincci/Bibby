# SECTIONS 18-23: THE PRAGMATIC PROGRAMMER PRINCIPLES

## Building Software Craftsmanship

Part 3 applies principles from "The Pragmatic Programmer" by Andrew Hunt and David Thomas to your Bibby codebase. These are timeless principles that separate good programmers from great ones.

---

## SECTION 18: THE PRAGMATIC PHILOSOPHY

### Core Principles

**1. Care About Your Craft**
- Pride in workmanship
- Code is communication with future developers
- Leave code better than you found it

**2. Think! About Your Work**
- Don't code on autopilot
- Question requirements and assumptions
- Understand *why* before *how*

**3. Provide Options, Don't Make Lame Excuses**
- Don't say "can't" - provide alternatives
- "We could do X, or Y, or Z. I recommend X because..."
- Own your code and decisions

**4. Don't Live with Broken Windows**
- Fix bad code immediately
- One broken window leads to more
- "I'll fix it later" = technical debt accumulates

**5. Be a Catalyst for Change**
- Show, don't tell
- Build a prototype, prove the concept
- Make incremental improvements

**6. Remember the Big Picture**
- Don't get lost in details
- Step back regularly
- Ask: "Does this serve the project's goals?"

**7. Make Quality a Requirements Issue**
- Define "good enough"
- Perfect is enemy of done
- Balance quality with delivery

### Applying to Bibby

**Audit Your Broken Windows**:
```java
// Broken window example in current Bibby code:
public void checkOutBook(BookEntity bookEntity) {
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");  // Mixing enum and string
        bookRepository.save(bookEntity);
    }
    // No else - silent failure!
}
```

**Fixed**:
```java
public void checkOut() {
    if (!this.status.canCheckOut()) {
        throw new BookNotAvailableException(
            "Cannot check out book in status: " + this.status.getDescription()
        );
    }
    this.status = AvailabilityStatus.CHECKED_OUT;
    this.checkoutCount++;
    registerEvent(new BookCheckedOutEvent(this.id, LocalDateTime.now()));
}
```

---

## SECTION 19: DRY - DON'T REPEAT YOURSELF

### The Principle

**Every piece of knowledge must have a single, unambiguous, authoritative representation within a system.**

DRY is NOT just about code duplication - it's about **knowledge duplication**.

### Types of Duplication

**1. Imposed Duplication** - Seems necessary
```java
// Documentation duplicates code
/**
 * Gets the book title.
 * @return the book's title as a string
 */
public String getTitle() {  // Comment says what code already says!
    return title;
}
```

**Fix**: Let code be self-documenting
```java
public Title getTitle() {  // Type says it all
    return title;
}
```

**2. Inadvertent Duplication** - Developers don't realize
```java
// Duplication in Book.java
public class Book {
    private Title title;
    private String titleString;  // Duplicate knowledge!
}
```

**Fix**: One source of truth
```java
public class Book {
    private Title title;  // Only one representation

    public String getTitleAsString() {
        return title.getValue();  // Derived, not stored
    }
}
```

**3. Impatient Duplication** - Lazy shortcut
```java
// Current Bibby - duplicated validation
public void addBook(AddBookCommand command) {
    if (command.getTitle() == null || command.getTitle().isBlank()) {
        throw new ValidationException("Title required");
    }
    if (command.getIsbn() == null || command.getIsbn().isBlank()) {
        throw new ValidationException("ISBN required");
    }
    // ... repeat in 5 other methods
}
```

**Fix**: Extract to validator
```java
public class BookCommandValidator {
    public void validate(AddBookCommand command) {
        validateTitle(command.getTitle());
        validateIsbn(command.getIsbn());
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Title required");
        }
    }
}
```

**4. Interdeveloper Duplication** - Multiple people, same functionality
```java
// Developer A creates:
public String formatBookTitle(Book book) { ... }

// Developer B doesn't know, creates:
public String getFormattedTitle(Book book) { ... }
```

**Fix**: Communication, code reviews, shared utilities

### DRY in Bibby: Real Examples

**Current Violation**:
```java
// Book.java
@Override
public String toString() {
    return "Book{id=" + id + ", title='" + title + "', ...}";
}

// BookMapper.java
public String bookToString(Book book) {
    return "Book{id=" + book.getId() + ", title='" + book.getTitle() + "', ...}";
}

// BookCommands.java
public String displayBook(Book book) {
    return "Book{id=" + book.getId() + ", title='" + book.getTitle() + "', ...}";
}
```

**Fixed**:
```java
// One place, one way
public class BookFormatter {
    public String format(Book book) {
        return String.format("Book{id=%s, title='%s', isbn='%s'}",
            book.getId(),
            book.getTitle(),
            book.getIsbn()
        );
    }
}
```

---

## SECTION 20: ORTHOGONALITY

### The Principle

**Eliminate effects between unrelated things. Components should be independent - changing one doesn't require changing others.**

In mathematics, orthogonal = perpendicular = no overlap. In software: no dependencies.

### Benefits of Orthogonality

1. **Increased Productivity** - Changes are localized
2. **Reduced Risk** - Isolated failures
3. **Better Testing** - Test components independently
4. **Easier Reuse** - Self-contained components

### Non-Orthogonal Code in Bibby

```java
// Current BookService - does EVERYTHING
@Service
public class BookService {
    // Database access
    final BookRepository bookRepository;

    // External API calls
    private final IsbnLookupService isbnService;

    // Domain logic
    public void checkOutBook(BookEntity book) { ... }

    // Presentation formatting
    public String formatBookList(List<BookEntity> books) { ... }

    // Validation
    public boolean isValidBook(BookEntity book) { ... }
}
```

**Problem**: Changing database affects presentation. Adding validation affects API calls. Not orthogonal!

### Orthogonal Refactoring

```java
// Separated concerns - orthogonal!

// Domain logic
public class Book {
    public void checkOut() { ... }  // Pure domain
}

// Persistence
@Repository
public interface BookRepository {
    void save(Book book);
}

// External integration
@Service
public class IsbnLookupAdapter implements IsbnMetadataService {
    // Only knows about external API
}

// Presentation
public class BookFormatter {
    public String format(Book book) { ... }
}

// Validation
public class BookValidator {
    public ValidationResult validate(Book book) { ... }
}
```

**Now**: Change persistence? Doesn't affect formatting. Change validation? Doesn't affect domain. Orthogonal!

### Testing Orthogonality

**Question**: Can you test each component in isolation?

```java
// Orthogonal - easy to test
@Test
void shouldCheckOutBook() {
    Book book = aBook().available().build();

    book.checkOut();  // No dependencies!

    assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
}

// Not orthogonal - hard to test
@Test
void shouldCheckOutBook() {
    // Need database, API, formatter, validator...
    BookService service = new BookService(repo, api, formatter, validator);
    // Complex setup
}
```

---

## SECTION 21: TRACER BULLETS & PROTOTYPES

### Tracer Bullets

**Build end-to-end functionality quickly to get immediate feedback.**

Tracer bullets are real bullets with phosphorus that show the trajectory. In code: thin vertical slices that work end-to-end.

**Example in Bibby**:

Instead of building all layers horizontally:
```
Week 1: Build all entities
Week 2: Build all repositories
Week 3: Build all services
Week 4: Build all controllers
Week 5: Integrate (doesn't work!)
```

Build one feature vertically:
```
Day 1: Add book feature end-to-end (minimal)
  - Book entity (basic)
  - Book repository (one method)
  - Add book service (simple)
  - CLI command (bare)
  - IT WORKS! Get feedback.

Day 2: Improve add book feature
  - Add validation
  - Add authors
  - Enhance UX

Day 3: New feature - Check out book
  - End-to-end again
  - Build on existing foundation
```

### Prototypes

**Disposable code to explore concepts.**

Prototypes are throwaway. Tracer bullets are production code you'll keep.

**When to Prototype**:
- Exploring new technology
- Validating an approach
- UI mockups
- Performance testing

**Prototype Example**: ISBN API Integration
```java
// Quick prototype - NOT production code!
public class IsbnPrototype {
    public static void main(String[] args) {
        String isbn = "978-0321125215";
        String url = "https://openlibrary.org/isbn/" + isbn + ".json";

        // Quick and dirty
        String json = HttpClient.newHttpClient()
            .send(HttpRequest.newBuilder().uri(URI.create(url)).build(),
                  HttpResponse.BodyHandlers.ofString())
            .body();

        System.out.println(json);  // See what we get
        // Now we know how to integrate properly!
    }
}
```

After prototype, build properly with adapter pattern, error handling, etc.

---

## SECTION 22: DEBUGGING & DEFENSIVE PROGRAMMING

### Pragmatic Debugging

**1. Don't Panic** - Calm, systematic approach
**2. Reproduce** - Must be repeatable
**3. Read the Error Message** - Actually read it!
**4. Binary Search** - Divide and conquer
**5. Explain to Someone Else** - Rubber duck debugging
**6. Fix the Cause, Not the Symptom**

### Defensive Programming

**Design by Contract**:
- **Preconditions**: What must be true before method runs
- **Postconditions**: What method guarantees after running
- **Invariants**: What's always true

**Example in Bibby**:
```java
public class Book {

    public void checkOut() {
        // Precondition
        if (this.status == null) {
            throw new IllegalStateException("Book status must be initialized");
        }

        // Precondition
        if (!this.status.canCheckOut()) {
            throw new BookNotAvailableException(
                "Precondition violated: book must be available to check out"
            );
        }

        // Operation
        this.status = AvailabilityStatus.CHECKED_OUT;
        this.checkoutCount++;

        // Postcondition (assertion)
        assert this.status == AvailabilityStatus.CHECKED_OUT :
            "Postcondition violated: status should be CHECKED_OUT";

        assert this.checkoutCount > 0 :
            "Postcondition violated: checkout count should be positive";
    }
}
```

**Assertions**:
```java
// Enable assertions during development
// java -ea com.penrose.bibby.BibbyApplication

public void placeOnShelf(ShelfId shelfId) {
    assert shelfId != null : "Shelf ID cannot be null";

    this.currentShelfId = shelfId;

    assert this.currentShelfId != null : "Shelf ID should be set";
}
```

### Fail Fast

```java
// Bad - silent failure
public void addBook(Book book) {
    if (book != null) {
        books.add(book);
    }
    // Null book silently ignored - bug hidden!
}

// Good - fail fast
public void addBook(Book book) {
    Objects.requireNonNull(book, "Book cannot be null");
    books.add(book);
    // Failure is immediate and loud
}
```

---

## SECTION 23: PRAGMATIC TESTING

### Test Early, Test Often, Test Automatically

**The Testing Pyramid**:
```
      /\
     /E2E\      Few, slow, expensive
    /─────\
   /Tests  \
  /  Inte-  \   Some, moderate speed
 /   gration \
/─────────────\
/  Unit Tests  \ Many, fast, cheap
/───────────────\
```

### Test-Infected: A Good Thing

Once you experience good tests, you can't code without them.

**Benefits**:
- Confidence to refactor
- Living documentation
- Design feedback
- Regression prevention

### Writing Testable Code

**Testable**:
```java
public class Book {
    public void checkOut() {
        if (!status.canCheckOut()) {
            throw new BookNotAvailableException();
        }
        this.status = AvailabilityStatus.CHECKED_OUT;
    }
}

// Test - easy!
@Test
void shouldCheckOutAvailableBook() {
    Book book = aBook().withStatus(AVAILABLE).build();

    book.checkOut();

    assertThat(book.getStatus()).isEqualTo(CHECKED_OUT);
}
```

**Not Testable**:
```java
public class BookService {
    public void checkOut(Long bookId) {
        BookEntity entity = bookRepository.findById(bookId);  // Database call
        if (entity.getStatus().equals("AVAILABLE")) {  // String comparison
            entity.setStatus("CHECKED_OUT");
            bookRepository.save(entity);  // Database call
            emailService.send("Checked out");  // Email call
        }
    }
}

// Test - nightmare!
@Test
void shouldCheckOutBook() {
    // Need: real database, mock repository, mock email, test data setup...
}
```

### Property-Based Testing

Instead of example-based tests, test properties:

```java
// Example-based test
@Test
void shouldCalculateLateFee() {
    Loan loan = createLoanOverdue(5);
    Money fee = calculator.calculateLateFee(loan);
    assertThat(fee).isEqualTo(Money.of(2.50));  // 5 days * $0.50
}

// Property-based test
@Property
void lateFeeIncreasesWithDays(@ForAll @IntRange(min = 1, max = 365) int daysLate) {
    Loan loan = createLoanOverdue(daysLate);
    Money fee = calculator.calculateLateFee(loan);

    // Property: fee should never exceed max
    assertThat(fee).isLessThanOrEqualTo(Money.of(25.00));

    // Property: more days = more fee (up to max)
    if (daysLate < 50) {
        Loan longerOverdue = createLoanOverdue(daysLate + 1);
        Money higherFee = calculator.calculateLateFee(longerOverdue);
        assertThat(higherFee).isGreaterThanOrEqualTo(fee);
    }
}
```

---

## Consolidated Action Items for Part 3

### Week 1: Philosophy & DRY
1. **Identify 10 broken windows** in Bibby - Create list, prioritize
2. **Find 5 DRY violations** - Document and fix
3. **Extract duplicated code** - Create shared utilities

### Week 2: Orthogonality & Design
1. **Measure coupling** - How many dependencies does each class have?
2. **Refactor one god class** - Split responsibilities
3. **Test orthogonality** - Can components be tested in isolation?

### Week 3: Practice & Testing
1. **Build one feature with tracer bullets** - Vertical slice approach
2. **Add assertions** to critical methods - Design by contract
3. **Achieve 80% test coverage** - Focus on domain logic

### Master Checklist

- [ ] Fix top 5 broken windows
- [ ] Eliminate DRY violations in core domain
- [ ] Reduce coupling in BookService
- [ ] Add precondition/postcondition assertions
- [ ] Create test data builders
- [ ] Write property-based tests for calculations
- [ ] Document design decisions
- [ ] Review code for orthogonality

---

## Key Takeaways from Part 3

### 1. Craftsmanship Matters
- Code is communication
- Fix broken windows immediately
- Leave code better than you found it

### 2. DRY is About Knowledge, Not Just Code
- Single source of truth for each concept
- Eliminate knowledge duplication
- Extract, don't repeat

### 3. Orthogonality Reduces Complexity
- Independent components
- Changes are localized
- Easier testing and reuse

### 4. Build Incrementally
- Tracer bullets: thin vertical slices
- Prototypes: throwaway exploration
- Get feedback early

### 5. Defend and Debug Systematically
- Design by contract
- Fail fast
- Systematic debugging approach

### 6. Test Everything
- Tests enable confidence
- Test early and often
- Testable code is better code

---

**Sections 18-23 Complete** | **Part 3: The Pragmatic Programmer Complete!**

Next: Part 4 - Java Testing Mastery (Sections 24-29)
