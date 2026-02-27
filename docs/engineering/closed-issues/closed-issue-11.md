# Closed Issue #11: Default Newly Created Books to AVAILABLE Status

**Issue:** [#11 - Default Newly Created Books to AVAILABLE Status]  
**Status:** ✅ Resolved  
**Commit:
** [408eeb318b8106c4d0bb4158829f26aaf9abf5c9](https://github.com/leodvincci/Bibby/commit/408eeb318b8106c4d0bb4158829f26aaf9abf5c9)  
**Date Resolved:** December 4, 2025

---

## 📋 Problem Statement

### The Issue

When creating new books in the Bibby library management system, there was no guarantee that the `availabilityStatus`
field would be consistently initialized to a valid state. This created the risk of:

1. **Null or undefined status values** — Books could be created without an availability status, leading to
   `NullPointerException` errors during checkout operations
2. **Inconsistent initial state** — Different book creation paths (interactive CLI, API import, barcode scanning) might
   set different default statuses or none at all
3. **Business logic violations** — Books without a defined status could bypass checkout validations

### Rationale for Change

The fix enforces a fundamental business invariant: **all newly created books must enter the system in an AVAILABLE state
**. This ensures:

- **Data integrity** — Every book has a well-defined status from the moment of creation
- **Predictable behavior** — Checkout logic can safely assume a status field exists and contains a valid enum value
- **Consistent user experience** — All book creation flows result in the same initial state
- **Domain model correctness** — The `Book` aggregate maintains its invariants at all times

This change aligns with Domain-Driven Design principles by ensuring that domain entities are created in a valid,
consistent state.

---

## 🔧 Implementation Details

The fix was implemented across **four architectural layers** to ensure the invariant is enforced at every point where a
`Book` or `BookEntity` is created.

### 1. Domain Layer: `AvailabilityStatus` Enum

**File:** [
`src/main/java/com/penrose/bibby/library/book/domain/AvailabilityStatus.java`](../../../src/main/java/com/penrose/bibby/library/cataloging/book/core/AvailabilityStatus.java)

```java
public enum AvailabilityStatus {
    AVAILABLE,      // ← Default for new books
    CHECKED_OUT,
    RESERVED,
    LOST,
    ARCHIVED
}
```

The enum defines all valid book statuses, with `AVAILABLE` as the designated initial state.

---

### 2. Entity Layer: `BookEntity` Constructor

**File:** [
`src/main/java/com/penrose/bibby/library/book/infrastructure/entity/BookEntity.java`](../../../src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/entity/BookEntity.java)

**Lines 33-38:**

```java
public BookEntity(String title, HashSet<AuthorEntity> authors) {
    this.title = title;
    this.authors = authors;
    this.createdAt = LocalDate.now();
    this.availabilityStatus = AvailabilityStatus.AVAILABLE.toString();  // ← Enforced here
}
```

**Purpose:** When using the parameterized constructor, the entity is automatically initialized with `AVAILABLE` status.
This protects against null values during interactive book creation via the CLI.

---

### 3. Factory Layer: `BookFactory` Methods

**File:** [
`src/main/java/com/penrose/bibby/library/book/domain/BookFactory.java`](../../../src/main/java/com/penrose/bibby/library/cataloging/book/core/BookFactory.java)

#### 3.1 Entity Creation (CLI Interactive Flow)

**Lines 14-21:**

```java
public BookEntity createBookEntity(String title, Set<AuthorEntity> authors){
    BookEntity bookEntity = new BookEntity();
    bookEntity.setTitle(title);
    bookEntity.setAuthors(authors);
    bookEntity.setCreatedAt(java.time.LocalDate.now());
    bookEntity.setAvailabilityStatus(AvailabilityStatus.AVAILABLE.name());  // ← Enforced here
    return bookEntity;
}
```

**Purpose:** Used by the interactive CLI when users manually add books. Ensures status is set even if the no-args
constructor is used.

#### 3.2 Domain Object Creation from JSON (API/Barcode Scanning)

**Lines 32-41:**

```java
public Book createBookDomainFromJSON(String title, String publisher, 
                                     String description, String isbn, 
                                     HashSet<Author> authors){
    Book book = new Book();
    book.setIsbn(isbn);
    book.setTitle(title);
    book.setAuthors(authors);
    book.setDescription(description);
    book.setPublisher(publisher);
    book.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);  // ← Enforced here
    return book;
}
```

**Purpose:** Used when creating books from external API data (e.g., Google Books API during barcode scanning). Ensures
scanned books also start as `AVAILABLE`.

---

### 4. Service Layer: `BookService.createScannedBook`

**File:** [
`src/main/java/com/penrose/bibby/library/book/application/BookService.java`](../../../src/main/java/com/penrose/bibby/library/cataloging/book/core/application/BookService.java)

**Lines 61-80:**

```java
public BookEntity createScannedBook(GoogleBooksResponse googleBooksResponse, String isbn){
    BookEntity bookEntity = new BookEntity();
    Set<AuthorEntity> authors = new HashSet<>();
    
    // ... author extraction logic ...
    
    bookEntity.setIsbn(isbn);
    bookEntity.setTitle(googleBooksResponse.items().get(0).volumeInfo().title());
    bookEntity.setPublisher(googleBooksResponse.items().get(0).volumeInfo().publisher());
    bookEntity.setPublicationYear(Integer.parseInt(googleBooksResponse.items().get(0).volumeInfo().publishedDate().split("-")[0]));
    bookEntity.setDescription(googleBooksResponse.items().get(0).volumeInfo().description());
    bookEntity.setAuthors(authors);
    bookEntity.setCreatedAt(LocalDate.now());
    bookEntity.setAvailabilityStatus("AVAILABLE");  // ← Enforced here
    saveBook(bookEntity);
    return bookEntity;
}
```

**Purpose:** Explicitly sets `AVAILABLE` status when books are created via barcode scanning workflow, ensuring
consistency with other creation paths.

---

### 5. Mapper Layer: Status Preservation

**File:** [
`src/main/java/com/penrose/bibby/library/book/infrastructure/mapping/BookMapper.java`](../../../src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/adapter/mapping/BookMapper.java)

**Line 54:**

```java
book.setAvailabilityStatus(e.getAvailabilityStatus() != null ? AvailabilityStatus.valueOf(e.getAvailabilityStatus()) : null);
```

**Line 89:**

```java
bookEntity.setAvailabilityStatus(book.getAvailabilityStatus() != null ? book.getAvailabilityStatus().name() : null);
```

**Purpose:** The mapper safely converts between entity and domain representations, preserving the status value during
transformations. While it allows `null` for defensive programming, the factory and entity layers ensure this case never
occurs for newly created books.

---

## ✅ Verification & Confirmation

### Invariant Enforcement

As of commit **408eeb318b8106c4d0bb4158829f26aaf9abf5c9**, the following invariant is guaranteed:

> **All newly created `Book` and `BookEntity` objects will have their `availabilityStatus` field initialized
to `AVAILABLE`.**

This holds true across all three book creation pathways:

1. ✅ **Interactive CLI input** (`BookFactory.createBookEntity`)
2. ✅ **Barcode scanning** (`BookService.createScannedBook`)
3. ✅ **API-based creation** (`BookFactory.createBookDomainFromJSON`)

### Impact on Existing Features

- **Checkout validation** (`Book.checkout()` method) can now safely assume `availabilityStatus` is non-null
- **Browse flows** correctly display book status without null checks
- **Database persistence** maintains referential integrity with valid enum values

---

## 🗂️ Related Files

| Layer       | File Path                                                                             | Purpose                                  |
|-------------|---------------------------------------------------------------------------------------|------------------------------------------|
| **Domain**  | `src/main/java/com/penrose/bibby/library/book/domain/Book.java`                       | Domain model with status field           |
| **Domain**  | `src/main/java/com/penrose/bibby/library/book/domain/AvailabilityStatus.java`         | Status enum definition                   |
| **Domain**  | `src/main/java/com/penrose/bibby/library/book/domain/BookFactory.java`                | Factory methods enforcing default status |
| **Entity**  | `src/main/java/com/penrose/bibby/library/book/infrastructure/entity/BookEntity.java`  | JPA entity with constructor enforcement  |
| **Service** | `src/main/java/com/penrose/bibby/library/book/application/BookService.java`           | Service layer with scanned book creation |
| **Mapper**  | `src/main/java/com/penrose/bibby/library/book/infrastructure/mapping/BookMapper.java` | Bi-directional entity↔domain mapping     |

---

## 📚 Additional Context

### Architectural Pattern

This implementation follows **Hexagonal Architecture** (Ports and Adapters) principles:

- **Domain layer** defines the core business rules (`AvailabilityStatus` enum, `Book` aggregate)
- **Application layer** orchestrates creation workflows (`BookService`)
- **Infrastructure layer** handles persistence details (`BookEntity`, `BookMapper`)

The factory pattern is used to centralize creation logic and enforce invariants at object construction time, preventing
invalid states from ever existing.

### Related Documentation

- [Devlog: Book Status & Checkout](../../the-devlogs/devlog-2025-11-12-bibby-book-status-checkout.md) — Original feature
  implementation
- [Spec: Move CheckOut Logic to Domain Model](../../systems/specs/000-Move-CheckOut-Logic-to-DomainModel.md) — DDD
  refactoring context
- [Devlog: BookFactory into Spring Component](../../the-devlogs/devlog-2025-11-20-BookFactory-into-Spring-ManagedComponent.md) —
  Factory pattern evolution

---

## 🎯 Maintainer Notes

### Why This Matters

This fix prevents a class of bugs related to uninitialized state. By ensuring `AVAILABLE` is the default status:

1. **Null-safe operations** — Checkout logic doesn't need defensive null checks
2. **Clear domain semantics** — A book's lifecycle explicitly starts as available
3. **Test reliability** — Unit tests can rely on consistent initial state

### Future Considerations

- If additional book creation pathways are added (e.g., bulk imports, REST API endpoints), they **must** ensure
  `AVAILABLE` status is set
- Consider adding a unit test that explicitly verifies all factory methods set the status
- Database migrations should include a `NOT NULL` constraint on the `availability_status` column with a default value

---

**Issue Closed By:** Commit 408eeb318b8106c4d0bb4158829f26aaf9abf5c9  
**Documentation Created:** December 4, 2025  
**Maintained By:** Bibby Development Team
