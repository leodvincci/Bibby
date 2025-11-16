# SECTION 5: OBJECTS AND DATA STRUCTURES
## Clean Code Chapter 6 Applied to Your Bibby Codebase

---

## Table of Contents
1. [Principles Overview](#principles-overview)
2. [Your Code Analysis](#your-code-analysis)
3. [Refactoring Examples](#refactoring-examples)
4. [Patterns You Should Recognize](#patterns-you-should-recognize)
5. [Action Items](#action-items)
6. [Key Takeaways](#key-takeaways)
7. [Further Study](#further-study)

---

## Principles Overview

### The Fundamental Dichotomy

Uncle Bob presents one of the most important and often misunderstood concepts in software: **the difference between objects and data structures**.

**Data structures:**
- Expose their data
- Have no meaningful behavior
- Are operated on by external functions
- Example: DTOs, POJOs with getters/setters

**Objects:**
- Hide their data behind abstractions
- Expose behavior (methods)
- Operate on their own data
- Example: Domain models with business logic

**The confusion:** Many developers create "objects" that are actually just data structures with getters and setters. This is called an **anemic domain model**, and it's an anti-pattern.

### Data/Object Anti-Symmetry

Here's the paradox Uncle Bob explains:

**Procedural code (using data structures):**
- Makes it easy to add new functions without changing data structures
- Makes it hard to add new data structures (requires changing all functions)

**Object-oriented code:**
- Makes it easy to add new classes without changing existing functions
- Makes it hard to add new functions (requires changing all classes)

**The lesson:** Sometimes data structures are the right choice. Sometimes objects are. Know the difference and choose appropriately.

### The Law of Demeter

A module should not know about the innards of the objects it manipulates.

**The rule:** A method `f` of class `C` should only call methods of:
- `C` itself
- An object created by `f`
- An object passed as an argument to `f`
- An object held in an instance variable of `C`

**Violation (train wreck):**
```java
String bookcaseLabel = book.getShelf().getBookcase().getLabel();
```

This violates the Law of Demeter because we're reaching through multiple objects.

**Better:**
```java
String bookcaseLabel = book.getBookcaseLabel();
```

The book should handle reaching through to get its bookcase label.

### Hybrids: The Worst of Both Worlds

The worst designs are hybrids that try to be both object and data structure:
- They have functions that do significant things
- They also have public variables or accessors that make private variables public

**Your code has this problem.** Your entities have both getters/setters (data structure) AND business logic methods (object). Pick one.

### Data Transfer Objects (DTOs)

DTOs are the quintessential data structures:
- No business logic
- Just public variables or accessors
- Used to transfer data between layers
- Perfectly fine for their purpose

**Example from your code:**
```java
public record BookRequestDTO(String title, String firstName, String lastName) {}
```

This is PERFECT. It's clearly a data structure, not trying to be an object.

---

## Your Code Analysis

Now let's examine the objects vs data structures issues in your Bibby codebase. I found several significant violations.

### Violation #1: Anemic Domain Model

**Location:** `Book.java`, `Bookcase.java`, `Shelf.java`

**Current Code (Book.java):**
```java
public class Book {
    private Long id;
    private String title;
    private AuthorEntity authorEntity;
    private BookStatus status;
    private Integer checkoutCount;
    private Shelf shelf;
    // ... 14 fields total

    // 14 getters
    // 14 setters
    // equals, hashCode, toString

    // NO BUSINESS LOGIC
}
```

**Why This Hurts:**

This is a classic **anemic domain model**. The `Book` class is just a bag of data with no behavior. All the business logic lives in `BookService`:

```java
// BookService.java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

The service is doing work that the `Book` should do. The `Book` knows nothing about what it means to be checked out.

**How to Fix - Add Behavior to Domain Model:**

```java
public class Book {
    private Long id;
    private String title;
    private Set<Author> authors;
    private BookStatus status;
    private int checkoutCount;
    private Shelf shelf;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    // Constructor
    public Book(String title, Set<Author> authors) {
        this.title = title;
        this.authors = authors;
        this.status = BookStatus.AVAILABLE;
        this.checkoutCount = 0;
        this.createdAt = LocalDate.now();
    }

    // BEHAVIOR, not just getters
    public void checkout() {
        if (isCheckedOut()) {
            throw new BookAlreadyCheckedOutException(
                "Cannot checkout '" + title + "' - already checked out"
            );
        }
        this.status = BookStatus.CHECKED_OUT;
        this.checkoutCount++;
        this.updatedAt = LocalDate.now();
    }

    public void checkin() {
        if (!isCheckedOut()) {
            throw new IllegalStateException(
                "Cannot check in '" + title + "' - not currently checked out"
            );
        }
        this.status = BookStatus.AVAILABLE;
        this.updatedAt = LocalDate.now();
    }

    public boolean isAvailable() {
        return status == BookStatus.AVAILABLE;
    }

    public boolean isCheckedOut() {
        return status == BookStatus.CHECKED_OUT;
    }

    public void assignToShelf(Shelf shelf) {
        if (shelf == null) {
            throw new IllegalArgumentException("Shelf cannot be null");
        }
        this.shelf = shelf;
        this.updatedAt = LocalDate.now();
    }

    public String getBookcaseLabel() {
        if (shelf == null) {
            return "No shelf assigned";
        }
        return shelf.getBookcaseLabel();
    }

    public String getShelfLabel() {
        if (shelf == null) {
            return "No shelf assigned";
        }
        return shelf.getLabel();
    }

    // Read-only accessors (no setters for most fields)
    public String getTitle() { return title; }
    public Set<Author> getAuthors() { return Collections.unmodifiableSet(authors); }
    public BookStatus getStatus() { return status; }
    public int getCheckoutCount() { return checkoutCount; }

    // NO SETTERS (encapsulation!)
}
```

**Now the service becomes simple:**

```java
@Service
public class BookService {
    private final BookRepository bookRepository;

    public void checkOutBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        book.checkout();  // Business logic in the domain model!
        bookRepository.save(book);
    }
}
```

**Notice:**
- Business logic lives in `Book`
- Service is just orchestration (find, call method, save)
- `Book` protects its own invariants
- Can't accidentally put a book in invalid state

---

### Violation #2: Law of Demeter (Train Wrecks)

**Location:** `BookCommands.java:362-363, 475-477, 551-553`

**Current Code:**
```java
Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
String bookcaseLabel = bookcaseEntity.get().getBookcaseLabel();
String bookshelfLabel = shelfEntity.get().getShelfLabel();
```

**Why This Hurts:**

This is a **train wreck** - chaining method calls that reach through multiple objects:
- `shelfEntity.get().getBookcaseId()` - reaching into shelf to get bookcase ID
- `bookcaseEntity.get().getBookcaseLabel()` - reaching into bookcase to get label

You're violating the Law of Demeter. The calling code knows too much about the internal structure of these objects.

Also notice the dangerous `.get()` calls on `Optional` without checking if they're present.

**How to Fix - Tell, Don't Ask:**

Instead of asking the book for its shelf, then asking the shelf for its bookcase, then asking the bookcase for its label, **tell the book what you need**:

```java
// In Book class
public String getLocationDescription() {
    if (shelf == null) {
        return "No location assigned";
    }
    return shelf.getFullLocation();
}

// In Shelf class
public String getFullLocation() {
    if (bookcase == null) {
        return "Shelf " + shelfLabel + " (no bookcase)";
    }
    return "Bookcase " + bookcase.getLabel() + ", Shelf " + shelfLabel;
}

// Usage (simple!)
String location = book.getLocationDescription();
System.out.println("Location: " + location);
```

**Alternative - Use a Query Object:**

```java
public record BookLocation(String bookcaseLabel, String shelfLabel) {
    public static BookLocation unassigned() {
        return new BookLocation("No bookcase", "No shelf");
    }

    public String format() {
        return String.format("Bookcase: %s, Shelf: %s", bookcaseLabel, shelfLabel);
    }
}

// In Book class
public BookLocation getLocation() {
    if (shelf == null) {
        return BookLocation.unassigned();
    }
    return new BookLocation(
        shelf.getBookcaseLabel(),
        shelf.getLabel()
    );
}

// Usage
BookLocation location = book.getLocation();
System.out.println(location.format());
```

---

### Violation #3: Exposing Implementation Details

**Location:** `BookEntity.java:24, 104-109`

**Current Code:**
```java
@Entity
@Table(name = "books")
public class BookEntity {
    private Long shelfId;  // Storing ID, not object!

    public Long getShelfId() {
        return shelfId;
    }

    public void setShelfId(Long shelfId) {
        this.shelfId = shelfId;
    }
}
```

**Why This Hurts:**

The entity exposes its internal implementation (storing a foreign key ID). This violates encapsulation. Code that uses this entity has to know:
- How shelves are stored (by ID)
- How to look up the shelf separately
- The relationship structure

**How to Fix - Encapsulate the Relationship:**

```java
@Entity
@Table(name = "books")
public class BookEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id")
    private ShelfEntity shelf;  // Object, not ID!

    // Business method, not just accessor
    public void assignToShelf(ShelfEntity shelf) {
        this.shelf = shelf;
        this.updatedAt = LocalDate.now();
    }

    public String getShelfLabel() {
        return shelf != null ? shelf.getShelfLabel() : "No shelf";
    }

    public String getBookcaseLabel() {
        return shelf != null ? shelf.getBookcaseLabel() : "No bookcase";
    }

    // Protected accessor (package-private or for JPA)
    ShelfEntity getShelf() {
        return shelf;
    }

    // NO public setShelf or setShelfId
}
```

**Why This Is Better:**
- Encapsulates the relationship
- Provides business methods, not just accessors
- JPA can still manage the relationship
- Calling code doesn't need to know about IDs

---

### Violation #4: Setter That Doesn't Set

**Location:** `BookEntity.java:54-56`

**Current Code:**
```java
public void setAuthors(AuthorEntity authors) {
    this.authors.add(authors);  // ADDS, doesn't SET!
}
```

**Why This Hurts:**

We covered this in Section 1 (naming), but it's also an objects/data structures issue. This method signature says "data structure" (setter) but the behavior says "object" (adds to collection).

**How to Fix - Choose One:**

**Option 1: Make it a proper setter (data structure approach)**
```java
public void setAuthors(Set<AuthorEntity> authors) {
    this.authors = authors;
}
```

**Option 2: Make it an object method (OO approach - BETTER)**
```java
public void addAuthor(AuthorEntity author) {
    if (author == null) {
        throw new IllegalArgumentException("Author cannot be null");
    }
    this.authors.add(author);
}

public void removeAuthor(AuthorEntity author) {
    this.authors.remove(author);
}

public Set<AuthorEntity> getAuthors() {
    return Collections.unmodifiableSet(authors);  // Defensive copy
}

// NO setAuthors method at all
```

---

### Violation #5: Duplicate Domain Models

**Location:** `Book.java` vs `BookEntity.java`

**Current Situation:**

You have BOTH:
- `Book.java` - Domain model (in `library.book`)
- `BookEntity.java` - JPA entity (in `library.book`)

They have nearly identical fields. Which one should you use?

**Why This Is Confusing:**

Having both creates questions:
- Which one represents the "real" book?
- When do I use which?
- How do I convert between them?
- Why do they both exist?

**How to Fix - Choose One Approach:**

**Option 1: Use BookEntity as the domain model (simpler)**

For small-to-medium applications, your JPA entities CAN be your domain models:

```java
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookId;

    private String title;

    @ManyToMany
    @JoinTable(name = "book_authors")
    private Set<AuthorEntity> authors = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private BookStatus status = BookStatus.AVAILABLE;

    private int checkoutCount = 0;

    // BUSINESS LOGIC METHODS
    public void checkout() {
        if (isCheckedOut()) {
            throw new BookAlreadyCheckedOutException();
        }
        this.status = BookStatus.CHECKED_OUT;
        this.checkoutCount++;
    }

    public void checkin() {
        if (!isCheckedOut()) {
            throw new IllegalStateException("Book not checked out");
        }
        this.status = BookStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return status == BookStatus.AVAILABLE;
    }

    public boolean isCheckedOut() {
        return status == BookStatus.CHECKED_OUT;
    }

    // Getters (no setters for most fields)
}
```

Delete `Book.java`. Use `BookEntity` everywhere.

**Option 2: Separate domain and persistence (complex applications)**

For large applications with complex domain logic:

```java
// BookEntity.java - Pure data structure for persistence
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookId;
    private String title;
    private String status;
    private int checkoutCount;

    // ONLY getters and setters, NO business logic
}

// Book.java - Rich domain model
public class Book {
    private final Long id;
    private final String title;
    private final Set<Author> authors;
    private BookStatus status;
    private int checkoutCount;

    // BUSINESS LOGIC HERE
    public void checkout() { ... }
    public void checkin() { ... }
    public boolean isAvailable() { ... }
}

// BookMapper.java - Converts between them
@Component
public class BookMapper {
    public Book toDomain(BookEntity entity) {
        return new Book(
            entity.getBookId(),
            entity.getTitle(),
            mapAuthors(entity.getAuthors()),
            BookStatus.valueOf(entity.getStatus()),
            entity.getCheckoutCount()
        );
    }

    public BookEntity toEntity(Book book) {
        BookEntity entity = new BookEntity();
        entity.setBookId(book.getId());
        entity.setTitle(book.getTitle());
        entity.setStatus(book.getStatus().toString());
        entity.setCheckoutCount(book.getCheckoutCount());
        return entity;
    }
}
```

**My Recommendation for Bibby:**

Use **Option 1**. Your application isn't complex enough to justify the mapping overhead. Put business logic directly in your entities.

Delete `Book.java`, `Bookcase.java`, `Shelf.java` and use the Entity classes as your domain models.

---

### Violation #6: Public Fields in Domain Classes

**Location:** Various DTOs and entities

**Issue:**

While your entities don't have public fields (good!), you do expose everything via getters and setters, which is functionally equivalent to public fields.

**The Principle:**

Objects should expose behavior, not data. Every getter/setter pair is a red flag that says "this field should probably be private and behavior should be added."

**Example - Before (data-centric):**

```java
BookEntity book = bookService.findById(id);
if (book.getBookStatus().equals("CHECKED_OUT")) {
    System.out.println("Already checked out");
} else {
    book.setBookStatus("CHECKED_OUT");
    book.setCheckoutCount(book.getCheckoutCount() + 1);
    bookService.save(book);
}
```

**After (behavior-centric):**

```java
Book book = bookService.findById(id);
if (book.isCheckedOut()) {
    System.out.println("Already checked out");
} else {
    book.checkout();  // Handles status AND count internally
    bookService.save(book);
}
```

---

## Refactoring Examples

### Example 1: Transform Anemic Model to Rich Domain Model

**Before - Anemic:**

```java
public class Shelf {
    private Long id;
    private Bookcase bookCase;
    private String shelfLabel;
    private int shelfPosition;

    public Shelf() {}

    // Getters and setters
    public String getLabel() { return shelfLabel; }
    public void setLabel(String label) { this.shelfLabel = label; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // EMPTY method!
    public void addToShelf(Book book){ }
}
```

**After - Rich:**

```java
public class Shelf {
    private final Long id;
    private final Bookcase bookcase;
    private final String label;
    private final int position;
    private final List<Book> books = new ArrayList<>();

    public Shelf(Bookcase bookcase, String label, int position) {
        this.bookcase = requireNonNull(bookcase);
        this.label = requireNonNull(label);
        this.position = position;
    }

    public void addBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
        if (isFull()) {
            throw new ShelfFullException(
                "Shelf " + label + " is full (max " + getCapacity() + " books)"
            );
        }
        books.add(book);
        book.assignToShelf(this);
    }

    public void removeBook(Book book) {
        books.remove(book);
    }

    public boolean isFull() {
        return books.size() >= getCapacity();
    }

    public int getAvailableSpace() {
        return getCapacity() - books.size();
    }

    public int getBookCount() {
        return books.size();
    }

    public String getFullLocation() {
        return bookcase.getLabel() + ", Shelf " + label;
    }

    public String getBookcaseLabel() {
        return bookcase.getLabel();
    }

    private int getCapacity() {
        // Business rule: standard shelf holds 20 books
        return 20;
    }

    // Read-only accessors
    public String getLabel() { return label; }
    public int getPosition() { return position; }
    public List<Book> getBooks() { return Collections.unmodifiableList(books); }

    // NO SETTERS
}
```

**Notice:**
- Constructor enforces required fields
- Business logic methods (`addBook`, `isFull`, `getAvailableSpace`)
- Protects its own invariants (can't add to full shelf)
- No setters - immutable where possible
- Returns unmodifiable collections

---

### Example 2: Fix Law of Demeter Violation

**Before:**

```java
// In BookCommands
BookEntity bookEntity = bookService.findBookByTitle(title);
if (bookEntity != null && bookEntity.getShelfId() != null) {
    Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
    if (shelfEntity.isPresent()) {
        Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(
            shelfEntity.get().getBookcaseId()
        );
        if (bookcaseEntity.isPresent()) {
            System.out.println("Bookcase: " + bookcaseEntity.get().getBookcaseLabel());
            System.out.println("Shelf: " + shelfEntity.get().getShelfLabel());
        }
    }
}
```

**After:**

```java
// In Book class
public Optional<BookLocation> getLocation() {
    if (shelf == null) {
        return Optional.empty();
    }
    return Optional.of(new BookLocation(
        shelf.getBookcaseLabel(),
        shelf.getLabel()
    ));
}

// In BookCommands
Book book = bookService.findBookByTitle(title);
book.getLocation().ifPresentOrElse(
    location -> System.out.println(location.format()),
    () -> System.out.println("No location assigned")
);
```

**Improvement:**
- Single method call instead of 6+
- No knowledge of internal structure
- No dangerous `.get()` on Optionals
- Clean and readable

---

## Patterns You Should Recognize

### Pattern #1: The Anemic Domain Model

**Symptoms:**
- Classes with only getters and setters
- All business logic in service classes
- Objects are just data bags
- Method names like `getX()`, `setX()` dominate

**How to spot it in your code:**

Count the ratio of getters/setters to business logic methods in your domain classes. If it's 20:1 or worse, you have anemic models.

### Pattern #2: The Service Layer God Classes

**When you have anemic domain models, the service layer becomes bloated:**

```java
@Service
public class BookService {
    // 50+ methods doing what the Book class should do
    public void checkOutBook(BookEntity book) { ... }
    public void checkInBook(BookEntity book) { ... }
    public boolean isBookAvailable(BookEntity book) { ... }
    public boolean isBookOverdue(BookEntity book) { ... }
    public void updateBookStatus(BookEntity book, String status) { ... }
    // etc.
}
```

All this logic should be IN the Book class.

### Pattern #3: The Train Wreck

Any time you see multiple dots in a row (except for fluent interfaces):

```java
book.getShelf().getBookcase().getLabel();
result.getContext().get("key", String.class);
entity.get().getSomething().getSomethingElse();
```

This is a Law of Demeter violation.

---

## Action Items

### 🔴 High Priority (This Week)

#### 1. **Choose: Entity as Domain Model or Separate Models**
**Files:** `Book.java`, `BookEntity.java`, `Bookcase.java`, `BookcaseEntity.java`, `Shelf.java`, `ShelfEntity.java`
- [ ] Decide: Use entities as domain models (recommended) or keep them separate
- [ ] If using entities as domain models: Delete duplicate domain classes
- [ ] If keeping separate: Create proper mappers
- **Estimated time:** 1 hour (decision + deletion) or 4 hours (mapper implementation)
- **Impact:** Eliminates confusion, establishes clear architecture

#### 2. **Add Business Logic to BookEntity**
**File:** `BookEntity.java`
- [ ] Add `checkout()` method
- [ ] Add `checkin()` method
- [ ] Add `isAvailable()` method
- [ ] Add `isCheckedOut()` method
- [ ] Add `assignToShelf(ShelfEntity)` method
- [ ] Remove setters for `status` and `checkoutCount`
- **Estimated time:** 1.5 hours
- **Impact:** Proper encapsulation, safer code

#### 3. **Fix Law of Demeter Violations in BookCommands**
**File:** `BookCommands.java:362-363, 475-477, 551-553`
- [ ] Create `getLocationDescription()` in Book/BookEntity
- [ ] Create `BookLocation` record for returning location data
- [ ] Replace train wrecks with single method calls
- **Estimated time:** 1 hour
- **Impact:** Cleaner code, better encapsulation

#### 4. **Fix setAuthors() Method**
**File:** `BookEntity.java:54-56`
- [ ] Rename to `addAuthor(AuthorEntity author)`
- [ ] Add `removeAuthor(AuthorEntity author)` if needed
- [ ] Make `getAuthors()` return unmodifiable set
- [ ] Remove `setAuthors()` entirely
- **Estimated time:** 15 minutes
- **Impact:** Correct naming, better encapsulation

---

### 🟡 Medium Priority (Next 2 Weeks)

#### 5. **Add Business Logic to ShelfEntity**
**File:** `ShelfEntity.java`
- [ ] Implement `addBook()` method properly
- [ ] Add `removeBook()` method
- [ ] Add `isFull()` method
- [ ] Add `getAvailableSpace()` method
- [ ] Add `getFullLocation()` method that includes bookcase
- **Estimated time:** 1.5 hours
- **Impact:** Rich domain model

#### 6. **Add Business Logic to BookcaseEntity**
**File:** `BookcaseEntity.java`
- [ ] Add `addShelf()` method
- [ ] Add `isFull()` method (if shelf capacity is enforced)
- [ ] Add `getTotalBookCount()` method
- [ ] Protect invariants with proper constructors
- **Estimated time:** 1 hour
- **Impact:** Complete rich domain model

#### 7. **Refactor Service Layer**
**Files:** `BookService.java`, `ShelfService.java`, `BookcaseService.java`
- [ ] Move business logic from services to entities
- [ ] Make services thin orchestration layers
- [ ] Services should: find entity, call method, save entity
- **Estimated time:** 3 hours
- **Impact:** Proper separation of concerns

---

### 🟢 Low Priority (Nice to Have)

#### 8. **Create Value Objects for Complex Concepts**
- [ ] Create `ISBN` value object with validation
- [ ] Create `BookLocation` record for location data
- [ ] Create `AuthorName` value object
- **Estimated time:** 2 hours
- **Impact:** Type safety, validation

#### 9. **Make Entities More Immutable**
- [ ] Use final fields where possible
- [ ] Remove unnecessary setters
- [ ] Use builder pattern for complex construction
- **Estimated time:** 2 hours
- **Impact:** Thread safety, clearer intent

---

## Key Takeaways

### What You're Doing Right

1. **Using DTOs properly:** `BookRequestDTO` is correctly just data
2. **Using records:** You're using Java records for DTOs, which is perfect
3. **Separation of entity and DTO:** You understand there's a difference

### What Needs Work

1. **Anemic domain models:** Book, Shelf, Bookcase have no behavior
2. **Law of Demeter violations:** Train wrecks throughout commands
3. **Exposing internals:** `setShelfId(Long)` exposes implementation
4. **Duplicate models:** Both Book and BookEntity exist with same fields

### The Big Lesson

**Objects hide data and expose behavior. Data structures expose data and have no behavior. Don't create hybrids.**

Your current entities are hybrids:
- They have getters/setters (data structure behavior)
- But they're trying to be domain models (object intent)
- This causes confusion and spreads business logic into services

**Fix:** Choose one approach and commit to it. For Bibby, I recommend **rich domain models** where your entities have business logic.

After refactoring:
- Services become thin orchestration layers
- Business rules live in domain models
- You can't accidentally put objects in invalid states
- Code is easier to test and understand

### Uncle Bob's Wisdom

> "Objects expose behavior and hide data. This makes it easy to add new kinds of objects without changing existing behaviors. It also makes it hard to add new behaviors to existing objects."

> "Data structures expose data and have no significant behavior. This makes it easy to add new behaviors to existing data structures. It also makes it hard to add new data structures because all the functions must change."

Choose which you need, then commit to it.

---

## Further Study

### From Clean Code (Chapter 6)
- Re-read pages 95-106
- Focus on the Data/Object Anti-Symmetry section
- Study the "Train Wrecks" section carefully

### Domain-Driven Design (Eric Evans)
- Anemic Domain Model anti-pattern
- Rich domain models with behavior
- Value objects vs entities
- Aggregates and aggregate roots

### Effective Java (Joshua Bloch)
- Item 15: Minimize mutability
- Item 16: Favor composition over inheritance
- Item 17: Design and document for inheritance or else prohibit it
- Item 50: Make defensive copies when needed

### Martin Fowler Articles
- "Anemic Domain Model" (martinfowler.com)
- "Tell Don't Ask" principle
- "Law of Demeter" explanation

---

## Final Thoughts from Your Mentor

Your codebase shows you understand the basics of objects and separation of concerns. You have entities, DTOs, services - all the right pieces.

The problem is **where the logic lives**.

Right now, your entities are just data bags. All the interesting behavior - checking out books, validating state, managing relationships - lives in the service layer. This is the anemic domain model anti-pattern.

The fix is conceptually simple: **move business logic into your domain models**.

Instead of:
```java
// Service doing the work
bookEntity.setBookStatus("CHECKED_OUT");
bookEntity.setCheckoutCount(bookEntity.getCheckoutCount() + 1);
```

Do this:
```java
// Domain model doing the work
book.checkout();
```

This isn't just about moving code around. It's about **where responsibilities belong**.

The `Book` class knows what it means to be checked out. It knows the business rules. It should protect its own invariants.

The service layer should just orchestrate: "Find this book, tell it to check out, save it."

Start with `BookEntity`. Add the methods I showed you (`checkout()`, `checkin()`, `isAvailable()`). You'll immediately see how much cleaner your service becomes.

Then tackle the Law of Demeter violations. Those train wrecks in your commands are screaming for encapsulation.

This is one of the most important sections because it affects your entire architecture. Get this right, and your code will be dramatically better.

---

**End of Section 5: Objects and Data Structures**
