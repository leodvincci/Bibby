# DDD Aggregates Explained Using the Bibby Codebase

This document explains Domain-Driven Design (DDD) aggregates using concrete examples from the Bibby library management system.

## Table of Contents

- [What is an Aggregate?](#what-is-an-aggregate)
- [Aggregate #1: Bookcase Aggregate](#aggregate-1-bookcase-aggregate)
- [Aggregate #2: Book Aggregate](#aggregate-2-book-aggregate)
- [Aggregate #3: Shelf (Potential Improvement)](#aggregate-3-shelf-potential-improvement)
- [Aggregate Design Patterns in the Codebase](#aggregate-design-patterns-in-the-codebase)
- [Key DDD Aggregate Principles](#key-ddd-aggregate-principles)
- [Recommendations](#recommendations-for-strengthening-aggregates)
- [Summary](#summary)

---

## What is an Aggregate?

An **aggregate** is a cluster of domain objects that can be treated as a single unit for data changes. It has:

- **An aggregate root** - The main entity that controls access to the aggregate
- **A consistency boundary** - What must stay consistent together
- **A transaction boundary** - What changes together atomically
- **Invariants** - Business rules that must always be true

---

## Aggregate #1: Bookcase Aggregate

**Aggregate Root:** `Bookcase`
**Members:** `Shelf` entities
**Location:** `src/main/java/com/penrose/bibby/library/bookcase/BookcaseService.java:27-43`

### The Pattern in the Code

```java
public String createNewBookCase(String label, int shelfCapacity, int bookCapacity){
    // Step 1: Create the aggregate root
    bookcaseEntity = new BookcaseEntity(label, shelfCapacity, bookCapacity*shelfCapacity);
    bookcaseRepository.save(bookcaseEntity);

    // Step 2: Create all member entities through the root
    for(int i = 0; i < bookcaseEntity.getShelfCapacity(); i++){
        addShelf(bookcaseEntity, i, i, bookCapacity);  // ← Controlled by aggregate root
    }
}
```

### Why This is an Aggregate

1. **Consistency Boundary:** When you create a bookcase, ALL its shelves must be created together
2. **Transaction Boundary:** The bookcase + shelves are a single logical unit
3. **Controlled Access:** Shelves are created through `addShelf()` method, controlled by the bookcase
4. **Invariant:** A bookcase with `shelfCapacity=5` must have exactly 5 shelves

### The DDD Rule Being Applied

> "External objects should only hold references to the aggregate root, not to members inside the aggregate."

**What this means for the code:**
- ✅ Good: Creating shelves through `BookcaseService.createNewBookCase()`
- ⚠️ Current issue: `ShelfService` allows direct access to shelves, bypassing the bookcase aggregate

---

## Aggregate #2: Book Aggregate

**Aggregate Root:** `Book`
**Members:** `AvailabilityStatus` (value object), references to `Author` (external)
**Location:** `src/main/java/com/penrose/bibby/library/book/Book.java:38-48`

### The Pattern in the Code

```java
public boolean checkout(){
    // Business invariant: can't checkout an already checked-out book
    if(this.availabilityStatus != AvailabilityStatus.AVAILABLE){
        throw new IllegalStateException("Book is already checked out");
    }

    // State change controlled by the aggregate root
    this.availabilityStatus = AvailabilityStatus.CHECKED_OUT;
    return true;
}
```

### How the Service Uses It

From `src/main/java/com/penrose/bibby/library/book/BookService.java:146-156`:

```java
public void checkOutBook(BookEntity bookEntity){
    Set<AuthorEntity> authorEntities = authorService.findByBookId(bookEntity.getBookId());

    // Step 1: Convert to domain model (aggregate root)
    Book book = bookMapper(bookEntity, new HashSet<>(authorEntities));

    // Step 2: Let the aggregate enforce its invariants
    book.checkout(); // ← Business logic in domain model!

    // Step 3: Persist the result
    bookEntity.setAvailabilityStatus(book.getAvailabilityStatus().name());
    saveBook(bookEntity);
}
```

### Why This is an Aggregate

1. **Consistency Boundary:** Book + its availability status must change together atomically
2. **Business Invariant:** The rule "can't checkout a checked-out book" is enforced by the aggregate
3. **Encapsulation:** Status can only change through domain methods like `checkout()`, not by direct setter abuse
4. **Single Source of Truth:** All checkout logic lives in ONE place

### The DDD Rule Being Applied

> "Aggregates enforce invariants and protect consistency boundaries."

---

## Aggregate #3: Shelf (Potential Improvement)

**Current State:** Shelf has aggregate-like behavior
**Location:** `src/main/java/com/penrose/bibby/library/shelf/Shelf.java:34-36`

### The Pattern in the Code

```java
public boolean isFull(){
    return books.size() >= bookCapacity;  // ← Enforcing capacity invariant
}
```

### Used in the CLI

When adding a book to a shelf, the capacity is checked:

```java
Shelf shelfDomain = shelfDomainRepository.getById(shelfId);
if(shelfDomain.isFull()){
    throw new IllegalStateException("Shelf is full");
}
```

### Why This is Partial Aggregate Behavior

✅ **Good:**
- Shelf enforces its own capacity invariant
- Books are loaded with the shelf (aggregate-like loading)

⚠️ **Issues:**
- Books can exist without shelves (weak lifecycle dependency)
- No `addBook()` method to enforce capacity at write-time (commented out on line 26)
- Shelf is accessed directly, not through Bookcase aggregate

---

## Aggregate Design Patterns in the Codebase

### Pattern 1: Entity/Domain Separation

The codebase has a clean separation that supports aggregates:

```
BookEntity (JPA)  →  BookMapper  →  Book (Domain)
     ↓                                    ↓
  Persistence Layer              Business Logic Layer
```

This is **excellent** because:
- Domain models can enforce business rules without database concerns
- Aggregates can be pure domain objects
- Mappers handle the translation

### Pattern 2: Domain Repository Pattern

From `BookDomainRepository`:

```java
public interface BookDomainRepository {
    List<Book> getBooksByShelfId(Long shelfId);
    Book getById(Long id);
}
```

This pattern ensures you **always get complete aggregates**, not partial entities.

### Pattern 3: Aggregate Loading

From `src/main/java/com/penrose/bibby/library/shelf/ShelfDomainRepositoryImpl.java`:

```java
public Shelf getById(Long id) {
    ShelfEntity entity = jpaRepository.findById(id).orElse(null);
    List<Book> books = bookDomainRepository.getBooksByShelfId(id);
    return shelfMapper.toDomain(entity, books);
}
```

When loading a Shelf, all its Books are loaded together - treating the Shelf as an aggregate unit.

---

## Key DDD Aggregate Principles

### 1. Aggregate Size Rule

> "Keep aggregates small. Only include what must change together."

**Book aggregate:** ✅ Correct size
- Book + AvailabilityStatus = small, focused aggregate
- Authors are referenced, not owned (they can exist independently)
- Shelf is referenced by ID, not composition

**Bookcase aggregate:** ✅ Reasonable size
- Bookcase + its Shelves = makes sense physically
- Books are NOT part of the Bookcase aggregate (they can move between shelves)

### 2. Consistency Boundary Rule

> "Everything inside an aggregate must be consistent at all times."

**Example from Book:** `src/main/java/com/penrose/bibby/library/book/Book.java:38-48`
```java
// This enforces consistency: if status is not AVAILABLE, checkout fails
if(this.availabilityStatus != AvailabilityStatus.AVAILABLE){
    throw new IllegalStateException("Book is already checked out");
}
```

**Example from Shelf:** `src/main/java/com/penrose/bibby/library/shelf/Shelf.java:34-36`
```java
// This enforces consistency: can't exceed capacity
public boolean isFull(){
    return books.size() >= bookCapacity;
}
```

### 3. Transaction Boundary Rule

> "Aggregate boundaries = transaction boundaries. Save entire aggregates, not pieces."

**Current code at** `src/main/java/com/penrose/bibby/library/bookcase/BookcaseService.java:34-39`:

⚠️ **Missing `@Transactional`** - This should be:

```java
@Transactional  // ← Missing! This should wrap the whole operation
public String createNewBookCase(String label, int shelfCapacity, int bookCapacity){
    bookcaseEntity = new BookcaseEntity(...);
    bookcaseRepository.save(bookcaseEntity);

    // These shelves should be saved in the same transaction
    for(int i = 0; i < bookcaseEntity.getShelfCapacity(); i++){
        addShelf(bookcaseEntity, i, i, bookCapacity);
    }
}
```

**Why this matters:** If shelf creation fails halfway through, you'll have an incomplete bookcase. `@Transactional` would roll back the entire aggregate.

### 4. Reference Other Aggregates by ID

> "Don't embed other aggregates. Reference them by ID."

**The code does this correctly:**

```java
// BookEntity.java - References shelf by ID, not embedding the entire shelf
private Long shelfId;  // ← Reference by ID ✅

// ShelfEntity.java - References bookcase by ID
private Long bookcaseId;  // ← Reference by ID ✅
```

This prevents:
- Massive object graphs
- Circular dependencies
- Performance issues from loading too much data

### 5. One Repository Per Aggregate

Each aggregate should have its own repository:

- `BookRepository` - For Book aggregate
- `BookcaseRepository` - For Bookcase aggregate
- `ShelfJpaRepository` - For Shelf (though it should ideally be accessed through Bookcase)
- `AuthorRepository` - For Author aggregate

✅ The codebase follows this pattern well.

---

## Recommendations for Strengthening Aggregates

### 1. Make Bookcase a True Aggregate Root

**Current issue:** Shelves can be accessed directly via `ShelfService`

**Better approach:**

```java
// In Bookcase.java - Add method to access shelves
public List<Shelf> getShelves() {
    return shelfRepository.findByBookcaseId(this.bookcaseId);
}

// In Bookcase.java - Add method to get specific shelf
public Shelf getShelf(Long shelfId) {
    Shelf shelf = shelfRepository.findById(shelfId);
    if (!shelf.getBookcaseId().equals(this.bookcaseId)) {
        throw new IllegalArgumentException("Shelf doesn't belong to this bookcase");
    }
    return shelf;
}
```

### 2. Add `@Transactional` to Aggregate Operations

**Location:** `src/main/java/com/penrose/bibby/library/bookcase/BookcaseService.java:27`

```java
@Transactional  // ← Add this annotation
public String createNewBookCase(String label, int shelfCapacity, int bookCapacity){
    // ... existing code
}
```

### 3. Move Validation to Domain Models

**Current:** Validation in `src/main/java/com/penrose/bibby/library/book/BookService.java:68-91`
**Better:** Move to `Book` constructor or factory

```java
// In Book.java or BookFactory
public static Book create(String title, Set<Author> authors) {
    if (title == null || title.isBlank()) {
        throw new IllegalArgumentException("Book title cannot be blank");
    }
    if (authors == null || authors.isEmpty()) {
        throw new IllegalArgumentException("Book must have at least one author");
    }
    return new Book(title, authors);
}
```

This way, **it's impossible to create an invalid Book**, because the domain model enforces it.

### 4. Implement Shelf's `addBook()` Method

**Location:** `src/main/java/com/penrose/bibby/library/shelf/Shelf.java:26` (currently commented out)

```java
public void addBook(Book book){
    if(books.size() >= bookCapacity){
        throw new IllegalStateException("Shelf is full - capacity: " + bookCapacity);
    }
    books.add(book);
}
```

This enforces the capacity invariant at write-time, not just at check-time, preventing race conditions.

### 5. Add Domain Events (Advanced)

For operations that need to notify other parts of the system:

```java
// In Book.java
public boolean checkout(){
    if(this.availabilityStatus != AvailabilityStatus.AVAILABLE){
        throw new IllegalStateException("Book is already checked out");
    }

    this.availabilityStatus = AvailabilityStatus.CHECKED_OUT;

    // Raise domain event
    registerEvent(new BookCheckedOutEvent(this.id, LocalDateTime.now()));

    return true;
}
```

This allows other parts of the system to react to checkout events (e.g., send notifications, update statistics).

---

## Summary

The Bibby codebase already demonstrates strong DDD aggregate patterns:

### Strengths

✅ **Bookcase → Shelf:** Clear aggregate with lifecycle control
✅ **Book + AvailabilityStatus:** Business logic in domain model
✅ **Reference by ID:** Prevents massive object graphs
✅ **Domain/Entity separation:** Clean architecture
✅ **Domain repositories:** Abstract away persistence concerns
✅ **Mappers:** Clean boundary between layers

### Current Aggregate Implementations

| Aggregate | Root | Members | Strength |
|-----------|------|---------|----------|
| Bookcase | Bookcase | Shelf[] | Strong - lifecycle control |
| Book | Book | AvailabilityStatus | Strong - invariant enforcement |
| Shelf | Shelf | Book[] (reference) | Medium - partial aggregate behavior |

### Key Improvements

1. Add `@Transactional` to `BookcaseService.createNewBookCase()`
2. Enforce access to Shelves through Bookcase aggregate root
3. Move validation logic into domain model constructors
4. Implement `Shelf.addBook()` to enforce capacity at write-time
5. Consider adding domain events for important state changes

### Aggregate Boundaries Visualization

```
┌─────────────────────────────────────┐
│ Bookcase Aggregate                   │
│ ┌─────────────┐                      │
│ │  Bookcase   │ (root)               │
│ └──────┬──────┘                      │
│        │                             │
│        ├──> Shelf 1                  │
│        ├──> Shelf 2                  │
│        └──> Shelf 3                  │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ Book Aggregate                       │
│ ┌─────────────┐                      │
│ │    Book     │ (root)               │
│ └──────┬──────┘                      │
│        │                             │
│        └──> AvailabilityStatus       │
│                                      │
│ References (by ID):                  │
│  - Author (external aggregate)       │
│  - Shelf (external aggregate)        │
└─────────────────────────────────────┘
```

---

## Further Reading

- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Implementing Domain-Driven Design by Vaughn Vernon](https://vaughnvernon.com/implementing-domain-driven-design/)
- [Aggregate Design Canvas](https://github.com/ddd-crew/aggregate-design-canvas)
- Specification documents in `/docs/specs/`:
  - `000-Move-CheckOut-Logic-to-DomainModel.md`
  - `001-DomainEntity-Layer-Separation.md`

---

**Document created:** 2025-11-24
**Codebase version:** Based on analysis of Bibby library management system
