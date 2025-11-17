# SECTION 3: AGGREGATES & AGGREGATE ROOTS

## Understanding Consistency Boundaries

Welcome to Section 3, where we tackle one of the most powerful (and initially confusing) patterns in DDD: **Aggregates**.

In Sections 1 and 2, we learned about ubiquitous language, entities, and value objects. Now we need to answer a critical question: **How do we group these objects together in meaningful ways that maintain consistency and enforce business rules?**

That's where aggregates come in.

---

## What is an Aggregate?

An **Aggregate** is a cluster of domain objects (entities and value objects) that are treated as a single unit for data changes. One entity in the aggregate is designated as the **Aggregate Root** - the only entity that external objects can hold references to.

### The Real-World Analogy

Think about ordering food at a restaurant. Your **Order** contains multiple **OrderItems** (burger, fries, drink).

**Question**: Can the kitchen modify just one OrderItem without going through the Order? No! Why not?
- The Order total needs to be recalculated
- Tax needs to be updated
- Discounts might apply differently
- The Order timestamp shows when it was modified

The **Order is an aggregate root**, and **OrderItems are internal to the aggregate**. External code doesn't manipulate OrderItems directly - it goes through the Order.

This ensures **consistency** - the order total always matches its items, tax is always correct, etc.

### Aggregates in Software

In code, an aggregate is a boundary around a group of objects. The aggregate enforces **invariants** (business rules that must always be true).

**Example invariant**: "An order's total must equal the sum of its items plus tax"

If external code could modify OrderItems directly, this invariant could be violated. By forcing all changes through the Order (the aggregate root), we guarantee consistency.

---

## Aggregate Characteristics

An aggregate has these properties:

### 1. Aggregate Root as Entry Point
- **One entity is designated as the aggregate root**
- External objects can only hold references to the root
- Internal entities are hidden from the outside world
- All operations go through the root

### 2. Consistency Boundary
- **Invariants are enforced within the aggregate**
- Business rules that must always be true
- The root ensures rules aren't violated
- Consistency is guaranteed within one aggregate

### 3. Transaction Boundary
- **One transaction = one aggregate**
- Don't modify multiple aggregates in the same transaction
- If you need to update multiple aggregates, use eventual consistency (domain events)

### 4. Unit of Persistence
- **Load and save the entire aggregate as a unit**
- Don't load just part of an aggregate
- Repository methods work with roots only

### 5. Identity-Based References Between Aggregates
- **Aggregates reference each other by ID, not direct object references**
- A Book references a Shelf by shelfId, not a Shelf object
- This enforces the aggregate boundary

---

## Identifying Aggregates in Bibby

Let's analyze your domain and identify the aggregates.

### Potential Aggregate 1: Book Aggregate

**Aggregate Root**: Book
**Internal Entities**: None (Book is standalone)
**Value Objects**: ISBN, Title, PublicationYear, Publisher, Edition, Description

**Invariants**:
- A book must have at least one author
- A book's checkout status must be valid (can't be both AVAILABLE and CHECKED_OUT)
- Checkout count must be non-negative
- ISBN must be valid and unique

**Boundaries**:
- Authors are NOT part of the Book aggregate (they're separate entities that can exist independently)
- Shelf is NOT part of the Book aggregate (a shelf can exist without books)
- The Book aggregate is just the Book entity and its value objects

**Why this makes sense**:
- You can modify a book's details without affecting other books
- Checking out a book is a self-contained operation
- The book's consistency rules are internal to itself

### Potential Aggregate 2: Bookcase Aggregate

**Aggregate Root**: Bookcase
**Internal Entities**: Shelf (multiple shelves)
**Value Objects**: BookcaseLabel, ShelfLabel, Location

**Invariants**:
- A bookcase cannot have more shelves than its capacity
- Shelf positions within a bookcase must be unique
- Each shelf must belong to exactly one bookcase

**Boundaries**:
- Books are NOT part of the Bookcase aggregate (they're a separate aggregate)
- A Book references a Shelf by ID, not direct reference

**Why this makes sense**:
- Adding/removing shelves affects the bookcase structure
- Shelves don't make sense without a bookcase
- Shelf positioning and capacity are bookcase-level concerns

### Potential Aggregate 3: Author Aggregate

**Aggregate Root**: Author
**Internal Entities**: None
**Value Objects**: AuthorName

**Invariants**:
- An author must have a valid name

**Boundaries**:
- Books are NOT part of the Author aggregate
- Author references books by ID (or more commonly, Books reference Authors)

**Why this makes sense**:
- Authors exist independently of books
- Adding a new book by an author doesn't change the author entity
- Simple aggregate with no internal complexity

### Current Problem in Your Code

Looking at your current implementation, I see **aggregate boundaries are violated**:

**In BookEntity.java**:
```java
@ManyToMany
@JoinTable(name = "book_authors")
private Set<AuthorEntity> authors = new HashSet<>();
```

**In BookRepository.java**:
```java
@Query(value = """
    SELECT b.book_id, b.title, ..., bc.bookcase_label, s.shelf_label
    FROM books b
    JOIN book_authors ba ON b.book_id = ba.book_id
    JOIN authors a ON ba.author_id = a.author_id
    JOIN shelves s ON s.shelf_id = b.shelf_id
    JOIN bookcases bc ON bc.bookcase_id = s.bookcase_id
    ...
""")
```

**The problem**: This query crosses multiple aggregate boundaries in one operation (Book → Author → Shelf → Bookcase). This creates tight coupling and makes it harder to enforce consistency.

---

## Aggregate Rules (The Golden Rules)

Eric Evans established clear rules for aggregates. These are not guidelines - they're rules:

### Rule 1: Reference Only the Aggregate Root

**External objects can only hold references to the aggregate root, never to internal entities.**

❌ Bad:
```java
Shelf shelf = bookcase.getShelves().get(0);  // Direct access to internal entity
shelf.setPosition(5);  // Bypassing the aggregate root
```

✓ Good:
```java
bookcase.repositionShelf(shelfId, 5);  // Through the root
```

### Rule 2: Use Identity for Inter-Aggregate References

**Aggregates reference other aggregates by ID, not by object reference.**

❌ Bad (tight coupling):
```java
public class Book {
    private Shelf shelf;  // Direct object reference to different aggregate
}
```

✓ Good (loose coupling):
```java
public class Book {
    private ShelfId shelfId;  // ID reference only
}

// To get the actual shelf:
Shelf shelf = shelfRepository.findById(book.getShelfId());
```

### Rule 3: One Transaction = One Aggregate

**Modify and save only one aggregate per transaction.**

❌ Bad:
```java
@Transactional
public void moveBook(Long bookId, Long newShelfId) {
    Book book = bookRepository.findById(bookId);
    Shelf oldShelf = shelfRepository.findById(book.getShelfId());
    Shelf newShelf = shelfRepository.findById(newShelfId);

    oldShelf.removeBook(book);  // Modifying one aggregate
    newShelf.addBook(book);      // Modifying another aggregate
    book.setShelf(newShelf);     // Modifying a third aggregate

    // Three aggregates in one transaction - violation!
    bookRepository.save(book);
    shelfRepository.save(oldShelf);
    shelfRepository.save(newShelf);
}
```

✓ Good:
```java
@Transactional
public void moveBook(Long bookId, Long newShelfId) {
    Book book = bookRepository.findById(bookId);
    book.placeOnShelf(newShelfId);  // Just update the ID reference
    bookRepository.save(book);       // One aggregate

    // Use domain events to update shelf counts eventually
    domainEventPublisher.publish(new BookMovedEvent(bookId, oldShelfId, newShelfId));
}
```

### Rule 4: Small Aggregates

**Keep aggregates as small as possible while maintaining invariants.**

Smaller aggregates = better performance, easier to reason about, fewer conflicts.

**Question to ask**: "Does this entity need to be modified in the same transaction as the root?"
- Yes → Part of the aggregate
- No → Separate aggregate

---

## Designing the Bibby Aggregates

Let me show you proper aggregate design for your domain.

### Aggregate 1: Book Aggregate

```java
// domain/model/Book.java (Aggregate Root)
public class Book {
    // Identity
    private final BookId id;

    // Value Objects (part of aggregate)
    private Title title;
    private ISBN isbn;
    private Publisher publisher;
    private PublicationYear publicationYear;
    private Edition edition;
    private Description description;

    // State
    private AvailabilityStatus status;
    private int checkoutCount;

    // References to OTHER aggregates (by ID only!)
    private Set<AuthorId> authorIds;  // Not Author objects!
    private ShelfId currentShelfId;    // Not Shelf object!

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor ensures valid creation
    public Book(BookId id, Title title, ISBN isbn, Set<AuthorId> authorIds) {
        this.id = Objects.requireNonNull(id);
        this.title = Objects.requireNonNull(title);
        this.isbn = Objects.requireNonNull(isbn);
        this.authorIds = new HashSet<>(Objects.requireNonNull(authorIds));

        if (authorIds.isEmpty()) {
            throw new IllegalArgumentException("Book must have at least one author");
        }

        this.status = AvailabilityStatus.AVAILABLE;
        this.checkoutCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Behavior that enforces invariants
    public void checkOut() {
        if (!status.canCheckOut()) {
            throw new BookNotAvailableException(
                "Cannot check out book with status: " + status
            );
        }
        this.status = AvailabilityStatus.CHECKED_OUT;
        this.checkoutCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void returnToLibrary() {
        if (status == AvailabilityStatus.AVAILABLE) {
            throw new IllegalStateException("Book is already available");
        }
        this.status = AvailabilityStatus.AVAILABLE;
        this.updatedAt = LocalDateTime.now();
    }

    public void placeOnShelf(ShelfId shelfId) {
        this.currentShelfId = Objects.requireNonNull(shelfId);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDetails(Title title, Description description) {
        this.title = Objects.requireNonNull(title);
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void addAuthor(AuthorId authorId) {
        this.authorIds.add(Objects.requireNonNull(authorId));
        this.updatedAt = LocalDateTime.now();
    }

    // Getters (no setters!)
    public BookId getId() { return id; }
    public Title getTitle() { return title; }
    public ISBN getIsbn() { return isbn; }
    public Set<AuthorId> getAuthorIds() { return new HashSet<>(authorIds); }
    public ShelfId getCurrentShelfId() { return currentShelfId; }
    public AvailabilityStatus getStatus() { return status; }
    public int getCheckoutCount() { return checkoutCount; }

    // Equality based on ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        return id.equals(((Book) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
```

**Key points**:
- `authorIds` is a Set<AuthorId>, not Set<Author>
- `currentShelfId` is ShelfId, not Shelf
- All business logic is in behavior methods
- Invariants enforced (e.g., can't check out if not available)
- No setters - changes through behavior methods

### Aggregate 2: Bookcase Aggregate

```java
// domain/model/Shelf.java (Entity, but NOT aggregate root)
public class Shelf {
    private final ShelfId id;
    private ShelfLabel label;
    private int position;
    private final BookcaseId bookcaseId;  // Reference to parent

    // Package-private constructor - only Bookcase can create
    Shelf(ShelfId id, ShelfLabel label, int position, BookcaseId bookcaseId) {
        this.id = Objects.requireNonNull(id);
        this.label = Objects.requireNonNull(label);
        this.position = position;
        this.bookcaseId = Objects.requireNonNull(bookcaseId);
    }

    // Package-private methods - only Bookcase can modify
    void updateLabel(ShelfLabel newLabel) {
        this.label = Objects.requireNonNull(newLabel);
    }

    void updatePosition(int newPosition) {
        if (newPosition < 0) {
            throw new IllegalArgumentException("Position cannot be negative");
        }
        this.position = newPosition;
    }

    // Public getters
    public ShelfId getId() { return id; }
    public ShelfLabel getLabel() { return label; }
    public int getPosition() { return position; }
    public BookcaseId getBookcaseId() { return bookcaseId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Shelf)) return false;
        return id.equals(((Shelf) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

// domain/model/Bookcase.java (Aggregate Root)
public class Bookcase {
    private final BookcaseId id;
    private BookcaseLabel label;
    private Location location;
    private int shelfCapacity;

    // Internal entities - part of the aggregate
    private final List<Shelf> shelves;

    public Bookcase(BookcaseId id, BookcaseLabel label, Location location, int shelfCapacity) {
        this.id = Objects.requireNonNull(id);
        this.label = Objects.requireNonNull(label);
        this.location = Objects.requireNonNull(location);
        this.shelfCapacity = shelfCapacity;
        this.shelves = new ArrayList<>();

        if (shelfCapacity <= 0) {
            throw new IllegalArgumentException("Shelf capacity must be positive");
        }
    }

    // Behavior that enforces aggregate invariants
    public Shelf addShelf(ShelfLabel label, int position) {
        if (shelves.size() >= shelfCapacity) {
            throw new BookcaseFullException(
                "Bookcase at capacity: " + shelfCapacity + " shelves"
            );
        }

        if (isPositionTaken(position)) {
            throw new IllegalArgumentException("Position " + position + " already occupied");
        }

        ShelfId shelfId = ShelfId.generate();
        Shelf shelf = new Shelf(shelfId, label, position, this.id);
        shelves.add(shelf);
        return shelf;
    }

    public void removeShelf(ShelfId shelfId) {
        Shelf shelf = findShelf(shelfId);
        shelves.remove(shelf);
    }

    public void repositionShelf(ShelfId shelfId, int newPosition) {
        if (isPositionTaken(newPosition)) {
            throw new IllegalArgumentException("Position already occupied");
        }

        Shelf shelf = findShelf(shelfId);
        shelf.updatePosition(newPosition);  // Modifying internal entity
    }

    public void relabelShelf(ShelfId shelfId, ShelfLabel newLabel) {
        Shelf shelf = findShelf(shelfId);
        shelf.updateLabel(newLabel);  // Modifying internal entity
    }

    // Helper methods
    private boolean isPositionTaken(int position) {
        return shelves.stream()
            .anyMatch(s -> s.getPosition() == position);
    }

    private Shelf findShelf(ShelfId shelfId) {
        return shelves.stream()
            .filter(s -> s.getId().equals(shelfId))
            .findFirst()
            .orElseThrow(() -> new ShelfNotFoundException(shelfId));
    }

    // Getters - return defensive copies
    public BookcaseId getId() { return id; }
    public BookcaseLabel getLabel() { return label; }
    public Location getLocation() { return location; }
    public int getShelfCapacity() { return shelfCapacity; }
    public List<Shelf> getShelves() { return List.copyOf(shelves); }  // Unmodifiable!

    public int getShelfCount() { return shelves.size(); }
    public int getAvailableShelfSlots() { return shelfCapacity - shelves.size(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bookcase)) return false;
        return id.equals(((Bookcase) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
```

**Key design decisions**:
- **Shelf** is an entity but NOT an aggregate root
- Shelf constructor and modifying methods are package-private
- External code cannot create or modify shelves directly
- All shelf operations go through Bookcase
- Invariants enforced: capacity, unique positions
- `getShelves()` returns unmodifiable copy

### Aggregate 3: Author Aggregate

```java
// domain/model/Author.java (Aggregate Root)
public class Author {
    private final AuthorId id;
    private AuthorName name;

    // References to other aggregates (by ID)
    private Set<BookId> bookIds;  // Not Book objects!

    public Author(AuthorId id, AuthorName name) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.bookIds = new HashSet<>();
    }

    public void changeName(AuthorName newName) {
        this.name = Objects.requireNonNull(newName);
    }

    public void addBook(BookId bookId) {
        this.bookIds.add(Objects.requireNonNull(bookId));
    }

    public void removeBook(BookId bookId) {
        this.bookIds.remove(bookId);
    }

    // Getters
    public AuthorId getId() { return id; }
    public AuthorName getName() { return name; }
    public Set<BookId> getBookIds() { return new HashSet<>(bookIds); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Author)) return false;
        return id.equals(((Author) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
```

---

## Inter-Aggregate References: The Right Way

### Current Problem in Your Code

```java
// BookEntity.java - Current implementation
public class Book {
    private Shelf shelf;  // ❌ Direct reference to different aggregate

    @ManyToMany
    private Set<AuthorEntity> authors;  // ❌ Direct references
}
```

**Why this is problematic**:
1. Can't save Book without loading entire Shelf aggregate
2. Can't load Book without loading all Authors
3. Changes to Shelf affect Book loading
4. Violates aggregate independence
5. Creates tight coupling

### The Solution: ID-Based References

```java
// Book.java - Proper implementation
public class Book {
    private ShelfId currentShelfId;  // ✓ ID reference only
    private Set<AuthorId> authorIds;  // ✓ ID references only
}

// To get the actual shelf:
Shelf shelf = shelfRepository.findById(book.getCurrentShelfId());

// To get authors:
Set<Author> authors = book.getAuthorIds().stream()
    .map(authorRepository::findById)
    .map(Optional::get)
    .collect(Collectors.toSet());
```

**Benefits**:
- Book aggregate is independent
- Can save Book without touching Shelf or Author
- Lazy loading by default
- Clear aggregate boundaries
- Loose coupling

---

## Aggregate Size: How Big Should They Be?

### The Guideline: As Small As Possible

**Vaughn Vernon's Rule**: "Design small aggregates"

**Why?**
1. **Performance**: Smaller aggregates load faster
2. **Concurrency**: Fewer conflicts when multiple users modify data
3. **Simplicity**: Easier to understand and maintain
4. **Transactions**: Shorter transactions reduce lock time

### The Question to Ask

"Does modifying X require modifying Y in the same transaction to maintain consistency?"

**If YES**: Same aggregate
**If NO**: Separate aggregates

### Example Decision: Book and Shelf

**Question**: If I move a book to a different shelf, do I need to update the shelf's book count in the same transaction?

**Answer**: No! You can use eventual consistency.

**Therefore**: Book and Shelf should be separate aggregates.

**Implementation**:
```java
// Immediate consistency: Book references shelf
book.placeOnShelf(newShelfId);
bookRepository.save(book);

// Eventual consistency: Update shelf counts via domain event
eventPublisher.publish(new BookMovedToShelfEvent(bookId, oldShelfId, newShelfId));

// Event handler updates shelf statistics eventually
@EventListener
public void handleBookMoved(BookMovedToShelfEvent event) {
    updateShelfStatistics(event.getNewShelfId());
}
```

---

## Common Aggregate Design Mistakes

### Mistake 1: Aggregates That Are Too Large

```java
// ❌ Bad - God aggregate
public class Library {
    private List<Bookcase> bookcases;  // Internal
    private List<Book> books;          // Should be separate!
    private List<Author> authors;      // Should be separate!
    private List<Member> members;      // Should be separate!

    // Hundreds of methods...
}
```

**Problem**: Everything is in one transaction. Modifying any aspect locks the entire library.

### Mistake 2: Aggregates That Are Too Small

```java
// ❌ Bad - anemic aggregate
public class BookTitle {  // Just a value object masquerading as aggregate
    private String value;
}
```

**Problem**: No behavior, no invariants to protect. This should be a value object, not an aggregate.

### Mistake 3: Violating Aggregate Boundaries

```java
// ❌ Bad - bypassing the root
public void moveShelf(BookcaseId bookcaseId, ShelfId shelfId, int newPosition) {
    Shelf shelf = shelfRepository.findById(shelfId);  // Direct access!
    shelf.setPosition(newPosition);  // Bypassing Bookcase!
    shelfRepository.save(shelf);
}
```

**Problem**: Bookcase invariants not enforced (position conflict possible).

### Mistake 4: Multiple Aggregates in One Transaction

```java
// ❌ Bad - modifying multiple aggregates
@Transactional
public void loanBookToMember(BookId bookId, MemberId memberId) {
    Book book = bookRepository.findById(bookId);
    Member member = memberRepository.findById(memberId);

    book.checkOut();  // Modifying Book aggregate
    member.addLoan(book);  // Modifying Member aggregate

    bookRepository.save(book);
    memberRepository.save(member);
}
```

**Solution**: Use domain events for cross-aggregate coordination.

---

## Action Items

### 1. Identify Bibby's Aggregates (2-3 hours)

Create a document listing:
- All aggregates in your domain
- Which is the root for each
- What entities/value objects are internal
- What invariants each aggregate protects
- How aggregates reference each other

**Question to answer for each**:
- Why is this an aggregate?
- Why is this entity the root?
- What would break if boundaries were violated?

### 2. Refactor Book to Use ID References (3-4 hours)

Modify your Book class:
- Change `Shelf shelf` to `ShelfId shelfId`
- Change `Set<AuthorEntity> authors` to `Set<AuthorId> authorIds`
- Remove JPA `@ManyToMany` direct relationships
- Update service layer to load related entities when needed

### 3. Design the Bookcase Aggregate (2-3 hours)

Implement:
- Bookcase as aggregate root
- Shelf as internal entity (package-private constructor)
- Methods on Bookcase to manage shelves
- Invariant enforcement (capacity, position uniqueness)

### 4. Draw Aggregate Diagrams (1-2 hours)

Create visual diagrams showing:
- Aggregate boundaries (boxes around each aggregate)
- Aggregate roots (highlighted)
- ID references between aggregates (dashed lines)
- Value objects within aggregates

Tools: Draw.io, Lucidchart, or even pen and paper

### 5. Review Your Repository Layer (1 hour)

Check `BookRepository.java`:
- Does it work with aggregate roots only?
- Are there methods that cross aggregate boundaries?
- Mark any violations to fix later

---

## Key Takeaways

### 1. Aggregates are Consistency Boundaries
- All business rules within one aggregate are immediately consistent
- Between aggregates, use eventual consistency

### 2. One Aggregate Root per Aggregate
- External code only interacts with the root
- Internal entities are protected

### 3. ID References Between Aggregates
- Never direct object references
- Keeps aggregates independent
- Enables lazy loading

### 4. One Transaction = One Aggregate
- Don't modify multiple aggregates in one transaction
- Use domain events for coordination

### 5. Design Small Aggregates
- Only include what must be consistent together
- Err on the side of smaller aggregates

---

## Further Study

**Books**:
- "Domain-Driven Design" by Eric Evans - Chapter 6 (Aggregates)
- "Implementing Domain-Driven Design" by Vaughn Vernon - Chapter 10 (Aggregates)
- Vaughn Vernon's "Effective Aggregate Design" series (free online)

**Articles**:
- Martin Fowler: "DDD Aggregate"
- Vaughn Vernon: "Effective Aggregate Design Part I, II, III"

**Key Concept to Research**:
- Eventual consistency
- Domain events for aggregate coordination
- Aggregate design patterns

---

## Mentor's Note

Aggregates are where DDD gets challenging. The concepts seem abstract until you try to apply them to real code. You'll make mistakes - everyone does. The key learning comes from:

1. **Trying** to design aggregates
2. **Discovering** where boundaries are wrong
3. **Refactoring** based on pain points

Don't expect to get aggregate design perfect on the first try. It's an iterative process. Start with your best guess, implement it, and adjust as you learn more about your domain's needs.

The most common mistake is making aggregates too large. When in doubt, split them up. It's easier to merge later than to split.

In Section 4, we'll learn about Repositories - how to persist and load aggregates. This is where aggregate design becomes concrete.

You're doing great. Aggregates are tough, but mastering them makes you a significantly better software engineer.

See you in Section 4!

---

**Section 3 Complete** | Next: Section 4 - Repositories
