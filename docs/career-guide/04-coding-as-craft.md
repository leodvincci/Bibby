# Section 04: Coding as Craft

## Beyond "Making It Work"

There's a moment in every developer's journey when code shifts from being a means to an end (make the feature work) to being a craft (make the feature excellent).

You're at that moment.

Bibby works. It demonstrates solid understanding of Spring Boot, JPA, and backend patterns. But there's a difference between code that works and code that communicates intent, handles edge cases gracefully, and welcomes future changes.

**This section is about refining your craft.**

We'll examine actual Bibby code, identify patterns worth keeping, spot opportunities for improvement, and develop the instincts that separate junior code from production-ready engineering.

Think of this as the detailed inspection you'd do on critical equipment before deployment. The pipeline works under test conditions—now make sure it works under production stress.

## Bibby Code Review: What's Working Well

Let's start with what you're doing right. Recognizing good patterns is as important as fixing bad ones.

### Strength #1: Entity Modeling with JPA

**Your `BookEntity.java`:**

```java
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookId;

    @ManyToMany
    @JoinTable(
        name = "book_authors",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<AuthorEntity> authors = new HashSet<>();

    private String title;
    private String isbn;
    private Long shelfId;
    private String bookStatus;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}
```

**What's Good:**
- ✅ Proper JPA annotations
- ✅ Many-to-many relationship correctly modeled with join table
- ✅ Using `Set` instead of `List` for authors (prevents duplicates, more semantically correct)
- ✅ Audit fields (`createdAt`, `updatedAt`)
- ✅ Explicit table and column names (good for production)

**Industrial Parallel:**
"This is like equipment specification sheets—each field serves a purpose, relationships are clearly defined, audit trail is built in."

### Strength #2: Repository Pattern

**Your repository interfaces:**

```java
public interface BookRepository extends JpaRepository<BookEntity, Long> {
    // Spring Data JPA generates implementations
}
```

**What's Good:**
- ✅ Leveraging Spring Data JPA (don't reinvent the wheel)
- ✅ Clean separation of persistence layer
- ✅ Type-safe with generics

**What Could Be Added:**
Custom query methods for your domain:

```java
public interface BookRepository extends JpaRepository<BookEntity, Long> {
    Optional<BookEntity> findByTitle(String title);
    List<BookEntity> findByBookStatus(String status);
    List<BookEntity> findByShelfId(Long shelfId);

    @Query("SELECT b FROM BookEntity b JOIN FETCH b.authors WHERE b.title = :title")
    Optional<BookEntity> findByTitleWithAuthors(@Param("title") String title);
}
```

**Why the improvement:**
- Explicit query methods document your access patterns
- The `JOIN FETCH` prevents N+1 queries (performance optimization)
- Future developers see exactly how books are queried

**Industrial Connection:**
"Like standardizing equipment access procedures—everyone uses the same documented approach instead of improvising."

### Strength #3: Service Layer Logic

**Your service pattern separates business logic from persistence:**

```java
@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookEntity findBookByTitle(String title) {
        // Business logic here
    }

    public void checkOutBook(BookEntity book) {
        // State change logic
    }
}
```

**What's Good:**
- ✅ Clear separation of concerns
- ✅ Service layer owns business logic
- ✅ Repository handles only persistence

**This is the foundation. Now let's refine it.**

## The SOLID Principles in Practice

SOLID isn't academic theory—it's practical guidance for maintainable code. Let's apply each principle to Bibby.

### S - Single Responsibility Principle

**Definition:** A class should have one reason to change.

**Current Issue in `BookCommands.java`:**

Your command class does:
1. User interaction (ComponentFlow)
2. Business logic (finding books, validating)
3. Presentation (formatting output)
4. Navigation (search flows)

**One class, four responsibilities. Four reasons to change.**

**Refactoring Strategy:**

```java
// Current (simplified):
@Command
public class BookCommands {
    public void searchBook() {
        // Gets user input
        // Calls service
        // Formats results
        // Handles navigation
    }
}

// Refactored:

// 1. Command layer - user interaction only
@Command
public class BookCommands {
    private final BookService bookService;
    private final BookPresenter bookPresenter;
    private final BookSearchFlow bookSearchFlow;

    public void searchBook() {
        SearchCriteria criteria = bookSearchFlow.collectSearchCriteria();
        List<BookEntity> results = bookService.search(criteria);
        bookPresenter.displaySearchResults(results);
    }
}

// 2. Service layer - business logic
@Service
public class BookService {
    public List<BookEntity> search(SearchCriteria criteria) {
        // Business logic for search
    }
}

// 3. Presenter - formatting and display
@Component
public class BookPresenter {
    public void displaySearchResults(List<BookEntity> books) {
        // Format and print results
    }
}

// 4. Flow - user interaction patterns
@Component
public class BookSearchFlow {
    private final ComponentFlow.Builder flowBuilder;

    public SearchCriteria collectSearchCriteria() {
        // Handle ComponentFlow interactions
        // Return clean data object
    }
}
```

**Benefits:**
- Change presentation? Touch only `BookPresenter`
- Change search logic? Touch only `BookService`
- Change UI flow? Touch only `BookSearchFlow`
- Each class has one clear job

**Industrial Connection:**
"Like separating operational roles—the operator doesn't maintain equipment, the mechanic doesn't monitor dashboards, the supervisor doesn't log compliance. Each role has clear responsibility."

### O - Open/Closed Principle

**Definition:** Open for extension, closed for modification.

**Application: Search Strategies**

**Current (hypothetical):**

```java
public List<BookEntity> search(String type, String value) {
    if (type.equals("title")) {
        return repository.findByTitle(value);
    } else if (type.equals("author")) {
        return repository.findByAuthor(value);
    } else if (type.equals("isbn")) {
        return repository.findByIsbn(value);
    }
    // Every new search type requires modifying this method
}
```

**Refactored (extensible):**

```java
// Interface for search strategies
public interface BookSearchStrategy {
    List<BookEntity> search(String criteria);
}

// Concrete implementations
@Component("titleSearch")
public class TitleSearchStrategy implements BookSearchStrategy {
    private final BookRepository repository;

    public List<BookEntity> search(String title) {
        return repository.findByTitle(title);
    }
}

@Component("authorSearch")
public class AuthorSearchStrategy implements BookSearchStrategy {
    private final BookRepository repository;

    public List<BookEntity> search(String author) {
        return repository.findByAuthorsContaining(author);
    }
}

// Service uses strategy
@Service
public class BookService {
    private final Map<String, BookSearchStrategy> strategies;

    public BookService(Map<String, BookSearchStrategy> strategies) {
        this.strategies = strategies;
    }

    public List<BookEntity> search(String type, String value) {
        BookSearchStrategy strategy = strategies.get(type + "Search");
        if (strategy == null) {
            throw new UnsupportedSearchTypeException(type);
        }
        return strategy.search(value);
    }
}
```

**Benefits:**
- Add new search type? Create new strategy class, don't modify existing code
- Test each strategy independently
- Swap strategies at runtime if needed

**Industrial Connection:**
"Like modular equipment components—add new capabilities without redesigning existing systems."

### L - Liskov Substitution Principle

**Definition:** Subtypes must be substitutable for their base types.

**Application: State Transitions**

**Problem to avoid:**

```java
// Bad - violating LSP
public class Book {
    public void checkout() {
        // Works for normal books
    }
}

public class ReferenceBook extends Book {
    @Override
    public void checkout() {
        throw new UnsupportedOperationException("Reference books can't be checked out!");
    }
}
```

**This violates LSP:** A `ReferenceBook` can't substitute for a `Book` without breaking behavior.

**Better approach:**

```java
// Use composition over inheritance
public interface Checkable {
    void checkout();
}

public class Book {
    private final CheckoutPolicy checkoutPolicy;

    public void checkout() {
        checkoutPolicy.checkout(this);
    }
}

public class AllowCheckoutPolicy implements CheckoutPolicy {
    public void checkout(Book book) {
        // Perform checkout
    }
}

public class ProhibitCheckoutPolicy implements CheckoutPolicy {
    public void checkout(Book book) {
        throw new CheckoutNotAllowedException("This book type cannot be checked out");
    }
}
```

**Industrial Connection:**
"Like equipment with different operating modes—the interface is consistent, but behavior varies based on configuration."

### I - Interface Segregation Principle

**Definition:** Clients shouldn't depend on interfaces they don't use.

**Application: Entity Interfaces**

**Bad (fat interface):**

```java
public interface BookOperations {
    void checkout();
    void checkin();
    void archive();
    void updateMetadata();
    void generateReport();
    void sendNotification();
}

// Some implementations don't need all methods
public class SimpleBook implements BookOperations {
    public void sendNotification() {
        // Don't need this, but forced to implement
        throw new UnsupportedOperationException();
    }
}
```

**Good (segregated interfaces):**

```java
public interface Checkable {
    void checkout();
    void checkin();
}

public interface Archivable {
    void archive();
}

public interface Reportable {
    Report generateReport();
}

// Implement only what you need
public class StandardBook implements Checkable, Archivable {
    // Only implement checkout, checkin, archive
}

public class ReferenceBook implements Reportable {
    // Only implement reporting
}
```

**Industrial Connection:**
"Like role-based access—operators see operational controls, maintenance sees service menus, supervisors see reports. Each interface tailored to need."

### D - Dependency Inversion Principle

**Definition:** Depend on abstractions, not concretions.

**Already doing this well in Bibby:**

```java
@Service
public class BookService {
    private final BookRepository bookRepository;  // Interface, not implementation

    // Spring injects concrete implementation
    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }
}
```

**Why this matters:**
- Test with mock repository
- Swap implementations (in-memory, different database)
- No coupling to JPA specifics in service layer

**Industrial Connection:**
"Like standardized API interfaces—equipment from different vendors works together through common protocols."

## Naming Conventions: Making Code Self-Documenting

You've already done significant work here (PR #42 on variable naming). Let's codify the principles.

### The Variable Naming Hierarchy

**Level 1: Single Letters (Avoid except for loops)**
```java
// Only acceptable for loop counters
for (int i = 0; i < books.size(); i++) {
    // OK here, scope is tiny
}

// NOT OK
BookEntity b = findBook(t);  // What is 'b'? What is 't'?
```

**Level 2: Abbreviated (Use sparingly)**
```java
// OK for very common abbreviations
int numBooks = repository.count();
String isbn = book.getIsbn();  // ISBN is standard abbreviation

// NOT OK
int cnt = getCount();  // Just say 'count'
String ttl = book.getTitle();  // Just say 'title'
```

**Level 3: Descriptive (Your target)**
```java
// Clear what it represents
BookEntity requestedBook = findBookByTitle(bookTitle);
Optional<ShelfEntity> assignedShelf = findShelfById(shelfId);
List<AuthorEntity> contributingAuthors = book.getAuthors();
```

**Level 4: Contextual (Best for complex logic)**
```java
// Name indicates purpose and context
BookEntity checkoutCandidate = findAvailableBook(searchCriteria);
LocalDate checkoutDeadline = calculateDueDate(checkoutDate, loanPeriod);
boolean userHasOutstandingFines = checkUserFinancialStatus(userId);

if (checkoutCandidate.isAvailable() && !userHasOutstandingFines) {
    performCheckout(checkoutCandidate, checkoutDeadline);
}
```

**The code reads like English. That's the goal.**

### Method Naming Patterns

**Command methods (do something):**
```java
// Verb phrases
public void checkoutBook(BookEntity book) { }
public void updateBookStatus(Long bookId, BookStatus status) { }
public void archiveOldBooks(LocalDate cutoffDate) { }
```

**Query methods (ask something):**
```java
// Predicate or noun phrases
public boolean isAvailable(BookEntity book) { }
public Optional<BookEntity> findBookById(Long id) { }
public List<BookEntity> getBooksByAuthor(String author) { }
```

**Factory methods (create something):**
```java
// 'create' or 'build' prefixes
public static BookEntity createNewBook(String title, Set<AuthorEntity> authors) { }
public BookSearchCriteria buildSearchCriteria(String type, String value) { }
```

### Class Naming Patterns

**Entities (nouns):**
```java
BookEntity, AuthorEntity, ShelfEntity
// Clearly domain objects
```

**Services (noun + Service):**
```java
BookService, AuthorService, NotificationService
// Business logic containers
```

**Repositories (noun + Repository):**
```java
BookRepository, ShelfRepository
// Data access abstractions
```

**DTOs (noun + DTO/Request/Response):**
```java
BookRequestDTO, BookResponseDTO, SearchCriteriaDTO
// Transfer objects for API boundaries
```

**Strategies/Policies (noun + Strategy/Policy):**
```java
BookSearchStrategy, CheckoutPolicy, ValidationStrategy
```

### Constants and Enums

**Constants (SCREAMING_SNAKE_CASE):**
```java
public class BookConstants {
    public static final int MAX_CHECKOUT_PERIOD_DAYS = 14;
    public static final int MAX_BOOKS_PER_USER = 5;
    public static final String DEFAULT_BOOK_STATUS = "AVAILABLE";
}
```

**Enums (PascalCase for type, SCREAMING_SNAKE_CASE for values):**
```java
public enum BookStatus {
    AVAILABLE,
    CHECKED_OUT,
    RESERVED,
    LOST,
    ARCHIVED;

    public boolean isCheckoutAllowed() {
        return this == AVAILABLE || this == RESERVED;
    }
}
```

### Industrial Context in Naming

**Your names should reflect domain knowledge:**

```java
// Generic (weak)
public void changeStatus(Book book, String newStatus) { }

// Domain-specific (strong)
public void transitionBookState(BookEntity book, BookStatus targetStatus) {
    // Implies validation, workflow, audit trail
}

// Even better (operational context)
public void performCustodyTransfer(BookEntity book, User fromUser, User toUser) {
    // Immediately clear this is like equipment handoff in operations
}
```

**In interviews, this is gold:**
"I named this method `performCustodyTransfer` because it mirrors custody transfer workflows I managed at Kinder Morgan—tracking who has responsibility for assets, maintaining audit trails, ensuring proper handoff procedures."

## State Management Patterns

Your `BookStatus` enum is a state machine. Let's make it bulletproof.

### Current State Model

```java
public enum BookStatus {
    AVAILABLE,
    CHECKED_OUT,
    RESERVED,
    LOST,
    ARCHIVED
}
```

**This is good foundation. Now add behavior.**

### Enhanced State Model with Transitions

```java
public enum BookStatus {
    AVAILABLE {
        @Override
        public Set<BookStatus> allowedTransitions() {
            return Set.of(CHECKED_OUT, RESERVED, ARCHIVED, LOST);
        }

        @Override
        public boolean isCheckoutAllowed() {
            return true;
        }
    },

    CHECKED_OUT {
        @Override
        public Set<BookStatus> allowedTransitions() {
            return Set.of(AVAILABLE, LOST);  // Can return or be lost
        }

        @Override
        public boolean isCheckoutAllowed() {
            return false;
        }
    },

    RESERVED {
        @Override
        public Set<BookStatus> allowedTransitions() {
            return Set.of(CHECKED_OUT, AVAILABLE);  // Reserved → Checkout or cancel
        }

        @Override
        public boolean isCheckoutAllowed() {
            return true;  // Reservation can transition to checkout
        }
    },

    LOST {
        @Override
        public Set<BookStatus> allowedTransitions() {
            return Set.of(AVAILABLE, ARCHIVED);  // Found or written off
        }

        @Override
        public boolean isCheckoutAllowed() {
            return false;
        }
    },

    ARCHIVED {
        @Override
        public Set<BookStatus> allowedTransitions() {
            return Set.of();  // Terminal state
        }

        @Override
        public boolean isCheckoutAllowed() {
            return false;
        }
    };

    // Abstract methods each state must implement
    public abstract Set<BookStatus> allowedTransitions();
    public abstract boolean isCheckoutAllowed();

    // Common validation logic
    public void validateTransition(BookStatus targetStatus) {
        if (!allowedTransitions().contains(targetStatus)) {
            throw new InvalidStateTransitionException(
                String.format("Cannot transition from %s to %s", this, targetStatus)
            );
        }
    }
}
```

**Now in your service:**

```java
@Service
public class BookService {

    public void transitionBookState(BookEntity book, BookStatus targetStatus) {
        BookStatus currentStatus = BookStatus.valueOf(book.getBookStatus());

        // Validation built into the enum
        currentStatus.validateTransition(targetStatus);

        // If we get here, transition is valid
        book.setBookStatus(targetStatus.name());
        book.setUpdatedAt(LocalDate.now());
        bookRepository.save(book);

        // Log the transition
        auditLog.info("Book state transition: id={}, from={}, to={}",
            book.getBookId(), currentStatus, targetStatus);
    }

    public void checkoutBook(BookEntity book) {
        BookStatus currentStatus = BookStatus.valueOf(book.getBookStatus());

        if (!currentStatus.isCheckoutAllowed()) {
            throw new CheckoutNotAllowedException(
                String.format("Book in %s status cannot be checked out", currentStatus)
            );
        }

        transitionBookState(book, BookStatus.CHECKED_OUT);
    }
}
```

**Benefits:**
- ✅ Invalid transitions impossible (enforced at compile time)
- ✅ Each state knows its allowed behaviors
- ✅ Business rules centralized in enum
- ✅ Easy to visualize state machine
- ✅ Audit trail built in

**Industrial Connection:**
"Equipment state machines in SCADA systems work exactly like this—OPERATIONAL can't jump to DECOMMISSIONED without intermediate states. Safety interlocks prevent invalid transitions."

### State Transition Logging

For compliance and debugging:

```java
@Entity
@Table(name = "book_state_transitions")
public class BookStateTransition {
    @Id
    @GeneratedValue
    private Long id;

    private Long bookId;
    private String fromStatus;
    private String toStatus;
    private String triggeredBy;  // User or system
    private LocalDateTime transitionTime;
    private String reason;  // Optional context

    // Constructors, getters, setters
}

@Service
public class StateTransitionAuditService {
    private final StateTransitionRepository repository;

    public void recordTransition(BookEntity book, BookStatus from, BookStatus to, String triggeredBy) {
        BookStateTransition transition = new BookStateTransition();
        transition.setBookId(book.getBookId());
        transition.setFromStatus(from.name());
        transition.setToStatus(to.name());
        transition.setTriggeredBy(triggeredBy);
        transition.setTransitionTime(LocalDateTime.now());

        repository.save(transition);
    }
}
```

**Industrial Connection:**
"Regulatory compliance for pipeline operations requires logging every state change—when it happened, who authorized it, why it occurred. Same pattern."

## Refactoring Workflows

Refactoring isn't rewriting—it's incrementally improving while keeping tests green.

### The Safe Refactoring Process

**1. Ensure Test Coverage**

Before touching code:
```java
@Test
public void testBookCheckout_Success() {
    // Arrange
    BookEntity book = createTestBook();
    book.setBookStatus("AVAILABLE");

    // Act
    bookService.checkoutBook(book);

    // Assert
    assertEquals("CHECKED_OUT", book.getBookStatus());
    verify(bookRepository).save(book);
}

@Test
public void testBookCheckout_AlreadyCheckedOut_ThrowsException() {
    // Arrange
    BookEntity book = createTestBook();
    book.setBookStatus("CHECKED_OUT");

    // Act & Assert
    assertThrows(BookNotAvailableException.class, () -> {
        bookService.checkoutBook(book);
    });
}
```

**If tests don't exist, write them first.** You can't safely refactor without a safety net.

**2. Make Small, Reversible Changes**

Don't refactor everything at once. One transformation at a time:

**Refactoring Sequence Example:**

```java
// Original (everything in one method)
public void searchBook() throws InterruptedException {
    ComponentFlow flow = componentFlowBuilder.clone()
        .withSingleItemSelector("searchType")
        .name("How would you like to search?")
        .selectItems(buildSearchOptions())
        .max(10)
        .and()
        .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    String searchType = result.getContext().get("searchType", String.class);

    if (searchType.equalsIgnoreCase("author")){
        searchByAuthor();
    } else if(searchType.equalsIgnoreCase("title")){
        searchByTitle();
    }
}

// Step 1: Extract flow creation
private ComponentFlow createSearchTypeFlow() {
    return componentFlowBuilder.clone()
        .withSingleItemSelector("searchType")
        .name("How would you like to search?")
        .selectItems(buildSearchOptions())
        .max(10)
        .and()
        .build();
}

public void searchBook() throws InterruptedException {
    ComponentFlow flow = createSearchTypeFlow();
    ComponentFlow.ComponentFlowResult result = flow.run();
    String searchType = result.getContext().get("searchType", String.class);

    if (searchType.equalsIgnoreCase("author")){
        searchByAuthor();
    } else if(searchType.equalsIgnoreCase("title")){
        searchByTitle();
    }
}

// Run tests. Still pass? Continue.

// Step 2: Extract search type extraction
private String getSearchTypeFromResult(ComponentFlowResult result) {
    return result.getContext().get("searchType", String.class);
}

public void searchBook() throws InterruptedException {
    ComponentFlow flow = createSearchTypeFlow();
    ComponentFlowResult result = flow.run();
    String searchType = getSearchTypeFromResult(result);

    if (searchType.equalsIgnoreCase("author")){
        searchByAuthor();
    } else if(searchType.equalsIgnoreCase("title")){
        searchByTitle();
    }
}

// Run tests. Still pass? Continue.

// Step 3: Replace if-else with strategy pattern
private void executeSearchStrategy(String searchType) {
    switch (searchType.toLowerCase()) {
        case "author" -> searchByAuthor();
        case "title" -> searchByTitle();
        default -> throw new UnsupportedSearchTypeException(searchType);
    }
}

public void searchBook() throws InterruptedException {
    ComponentFlow flow = createSearchTypeFlow();
    ComponentFlowResult result = flow.run();
    String searchType = getSearchTypeFromResult(result);
    executeSearchStrategy(searchType);
}

// Run tests. Still pass? You're done.
```

**After each step:** Run tests. Commit if green. If red, revert and try differently.

**3. Use Your IDE's Refactoring Tools**

IntelliJ IDEA has safe refactorings:
- **Extract Method**: Highlight code → Right-click → Refactor → Extract Method
- **Extract Variable**: Highlight expression → Refactor → Extract Variable
- **Rename**: Click symbol → Shift+F6 → Rename (updates all references)
- **Change Signature**: Adds/removes parameters safely
- **Inline**: Removes unnecessary variables/methods

These update all usages automatically and maintain syntax correctness.

**4. Document Why, Not What**

```java
// Bad comment (says what code does)
// Set book status to CHECKED_OUT
book.setBookStatus(BookStatus.CHECKED_OUT.name());

// Good comment (says why)
// Mark as checked out to prevent concurrent checkouts via database constraint
book.setBookStatus(BookStatus.CHECKED_OUT.name());

// Better (no comment needed, code is self-documenting)
preventConcurrentCheckouts(book);

private void preventConcurrentCheckouts(BookEntity book) {
    book.setBookStatus(BookStatus.CHECKED_OUT.name());
}
```

**5. Update devLog**

After significant refactoring:
```markdown
# 2025-01-15 - BookCommands Refactoring

## What Changed
Extracted ComponentFlow creation and search strategy dispatching from
monolithic searchBook() method.

## Why
- Single Responsibility: searchBook() now orchestrates, doesn't implement
- Testability: Can test flow creation and strategy dispatch independently
- Maintainability: Adding new search types no longer requires changing searchBook()

## Patterns Applied
- Extract Method refactoring
- Strategy pattern for search type dispatch
- Guard clauses for validation

## Before/After Metrics
- Before: 45 lines, cyclomatic complexity 6
- After: 15 lines in main method, 3 extracted methods, complexity 2

## Industrial Connection
Like separating operational procedures: one procedure describes the process,
subordinate procedures handle specific tasks. Easier to train new operators,
easier to audit compliance.
```

## Code Review Checklist for Production Systems

Use this checklist before every pull request (even to yourself):

### Functionality
- [ ] Feature works as specified
- [ ] Edge cases handled (null, empty, unexpected input)
- [ ] Error conditions handled gracefully
- [ ] No hardcoded values (use configuration or constants)

### Readability
- [ ] Variable names are descriptive and contextual
- [ ] Method names clearly indicate purpose
- [ ] No magic numbers (extract to named constants)
- [ ] Code reads like English prose
- [ ] No clever tricks that need explanation

### Structure
- [ ] Each method has single responsibility
- [ ] Classes have cohesive responsibilities
- [ ] No methods over 20-30 lines (guideline, not rule)
- [ ] No deep nesting (max 2-3 levels)
- [ ] Guard clauses used for validation

### Principles
- [ ] DRY: No repeated code blocks
- [ ] YAGNI: No speculative features "for the future"
- [ ] SOLID principles followed
- [ ] Composition favored over inheritance
- [ ] Dependencies injected, not hardcoded

### Testing
- [ ] Unit tests for business logic
- [ ] Integration tests for data access
- [ ] Edge cases tested
- [ ] Test names describe scenarios
- [ ] Tests are fast and deterministic

### Logging & Monitoring
- [ ] Appropriate log levels (DEBUG, INFO, WARN, ERROR)
- [ ] Structured logging (key=value pairs)
- [ ] No sensitive data in logs (passwords, PII)
- [ ] Log meaningful context (IDs, parameters)

### Performance
- [ ] No obvious N+1 queries
- [ ] Database queries use indexes
- [ ] Large datasets handled with pagination
- [ ] No resource leaks (connections, streams)

### Security
- [ ] Input validation on all external data
- [ ] SQL injection prevented (using JPA, not string concat)
- [ ] No secrets in code (use environment variables)
- [ ] Appropriate access control

### Documentation
- [ ] Public API methods have Javadoc
- [ ] Complex logic has explanatory comments
- [ ] README updated if needed
- [ ] devLog entry for significant changes

### Industrial Mindset
- [ ] Would this work at 2am with an incident?
- [ ] Are error messages actionable?
- [ ] Can this be understood 6 months from now?
- [ ] Does it handle real-world operational scenarios?

## Exercises for This Section

### Exercise 1: SOLID Refactoring (4-6 hours)

**Pick one class in Bibby that violates Single Responsibility Principle.**

Suggested: `BookCommands.java` (does UI, business logic, formatting)

**Steps:**
1. Write tests for current behavior (2 hours)
2. Identify all responsibilities the class has (30 min)
3. Create separate classes for each responsibility (2 hours)
4. Refactor incrementally, keeping tests green (1-2 hours)
5. Document in devLog (30 min)

**Deliverable:** Pull request with refactored code, tests passing, devLog updated

### Exercise 2: State Machine Enhancement (3 hours)

**Enhance `BookStatus` enum with state transition logic.**

**Tasks:**
1. Add `allowedTransitions()` method to each state (1 hour)
2. Add `validateTransition()` method (30 min)
3. Update `BookService` to use validation (1 hour)
4. Write tests for invalid transitions (30 min)

**Deliverable:** Enhanced enum with tests proving invalid transitions are caught

### Exercise 3: Naming Audit (2 hours)

**Audit all variables, methods, and classes in Bibby.**

**Create spreadsheet:**
| Current Name | Type | Quality (1-4) | Suggested Improvement | Reason |
|--------------|------|---------------|----------------------|--------|
| `b` | Variable | 1 | `requestedBook` | Single letter, unclear |
| `findBook` | Method | 3 | `findBookByTitle` | More specific |

**Then refactor top 10 worst offenders.**

**Deliverable:** Spreadsheet + refactored code

### Exercise 4: Comprehensive Code Review (2 hours)

**Review one feature in Bibby using the production checklist.**

Suggested: Book checkout workflow

**For each checklist item:**
- ✅ if satisfied
- ❌ if not, with note on what's missing
- ⚠️ if partially satisfied

**Create action items for every ❌ and ⚠️.**

**Deliverable:** Completed checklist with action items

### Exercise 5: Extract Method Refactoring (2 hours)

**Find the longest method in Bibby. Refactor using Extract Method.**

**Process:**
1. Identify the method (likely in `BookCommands`)
2. Write tests for its current behavior
3. Extract logical chunks into separate methods
4. Give extracted methods descriptive names
5. Ensure main method reads like a high-level process
6. Run tests after each extraction

**Goal:** Reduce method to <15 lines of high-level orchestration

**Deliverable:** Refactored method with tests

### Exercise 6: Add State Transition Audit (3-4 hours)

**Implement comprehensive audit trail for book state changes.**

**Tasks:**
1. Create `BookStateTransition` entity (30 min)
2. Create repository and service (30 min)
3. Update `BookService` to record all transitions (1 hour)
4. Add query methods to retrieve transition history (30 min)
5. Write tests (1 hour)
6. Add CLI command to view book history (optional, 1 hour)

**Deliverable:** Working audit system with tests

## Action Items for Week 5

### Critical (Must Complete)
1. ✅ Complete Exercise 1: SOLID refactoring of one major class
2. ✅ Complete Exercise 2: State machine enhancement
3. ✅ Complete Exercise 4: Comprehensive code review with checklist
4. ✅ Apply production checklist to all new code this week

### Important (Should Complete)
5. ⬜ Complete Exercise 3: Naming audit and top 10 refactoring
6. ⬜ Complete Exercise 5: Extract Method refactoring
7. ⬜ Review all recent commits using checklist, create backlog of improvements
8. ⬜ Update your personal code review template based on learnings

### Bonus (If Time Permits)
9. ⬜ Complete Exercise 6: State transition audit trail
10. ⬜ Add comprehensive Javadoc to all public methods
11. ⬜ Create architectural decision records (ADRs) for major design choices
12. ⬜ Pair program (remotely) with another developer on refactoring

## Key Takeaways

1. **Code is communication.** You're not writing for the compiler—you're writing for the next developer (often future you).

2. **SOLID isn't academic.** Each principle prevents real maintenance pain. Apply them pragmatically.

3. **Naming is hard and critical.** Spend time on names. Rename ruthlessly. Names are your primary documentation.

4. **State machines prevent bugs.** Invalid states are impossible if you encode business rules in the type system.

5. **Refactor incrementally.** Small steps, tests always green, reversible changes. Not big-bang rewrites.

6. **Production mindset matters.** Code that works in development might fail in production. Think about 2am debugging.

7. **Document decisions, not implementation.** The code shows *what*. Comments should explain *why*.

8. **Your operational experience is your advantage.** Apply operational thinking to code: clear procedures, validation, audit trails, error handling.

## What's Next

In Section 05, we'll dive into **Algorithms & Problem Solving I**:
- Two Pointers pattern mastery
- Frequency Counter methods
- Sliding Window techniques
- Java implementations
- LeetCode strategy for backend roles
- Connecting patterns to Bibby and industrial applications

We'll make DS&A practical and directly applicable to your target roles.

---

**Word Count:** ~6,700 words

**Time Investment Week 5:** 15-20 hours
- Exercises: 12-15 hours (SOLID refactoring, state machine, code review)
- Reading and reflection: 2-3 hours
- Applying checklist to ongoing work: 1-2 hours

**Expected Outcome:**
- At least one major class refactored using SOLID principles
- State machine enhanced with transition validation
- Production code review checklist internalized
- Naming quality significantly improved
- Deeper understanding of why clean code matters in production
- Bibby code approaching Level 4 quality (maintainable)

**Success Metrics:**
- Method lengths: 90% under 20 lines
- Variable naming: No single-letter variables except loop counters
- Tests: All refactored code has test coverage
- Code review: Can confidently explain every design decision
- Industrial connection: Can relate every pattern to operational experience

---

*Code quality isn't about perfection. It's about respect—respect for future maintainers, respect for users depending on your system, respect for the craft. You bring operational respect to software engineering. It shows.*
