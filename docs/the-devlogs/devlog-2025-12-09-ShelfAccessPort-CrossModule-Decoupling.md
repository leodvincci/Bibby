# Devlog: ShelfAccessPort Introduction & Cross-Module Decoupling

**Date:** 2025-12-09  
**Modules:** Book, Shelf, CLI  
**Type:** Refactoring + Feature  
**Status:** Complete

---

## Summary

Introduced `ShelfAccessPort` as Book module's outbound port for accessing shelf data, completing the pattern established with `AuthorAccessPort`. This removes Book's direct dependency on `ShelfService`, routing all cross-module communication through explicit port interfaces.

Also expanded `BookDomainRepository` with update and lookup methods, fixed dependency injection issues, and renamed the CLI search command.

---

## Part 1: ShelfAccessPort

### Problem

`BookService` directly imported `ShelfService`:

```java
import com.penrose.bibby.library.stacks.shelf.core.application.ShelfService;

public class BookService implements BookFacade {
    private final ShelfService shelfService;  // Direct coupling
}
```

This created a compile-time dependency from Book → Shelf's implementation, not its contract.

### Solution

Created an outbound port following the same pattern as `AuthorAccessPort`:

**ShelfAccessPort (Book defines what it needs):**
```java
package com.penrose.bibby.library.book.contracts.ports.outbound;

public interface ShelfAccessPort {
    Optional<ShelfDTO> findShelfById(Long shelfId);
}
```

**ShelfAccessPortAdapter (Book adapts from ShelfFacade):**
```java
package com.penrose.bibby.library.book.contracts.adapters;

@Component
public class ShelfAccessPortAdapter implements ShelfAccessPort {
    private final ShelfFacade shelfFacade;

    public ShelfAccessPortAdapter(ShelfFacade shelfFacade) {
        this.shelfFacade = shelfFacade;
    }

    @Override
    public Optional<ShelfDTO> findShelfById(Long shelfId) {
        return shelfFacade.findShelfById(shelfId);
    }
}
```

**BookService now uses the port:**
```java
public class BookService implements BookFacade {
    private final ShelfAccessPort shelfAccessPort;  // Port, not service
    
    public BookEntity assignBookToShelf(Long bookId, Long shelfId) {
        ShelfDTO shelf = shelfAccessPort.findShelfById(shelfId)
                .orElseThrow(() -> new IllegalArgumentException("Shelf not found"));
        // ...
    }
}
```

### Adapter Pattern Choice

We discussed two approaches:

1. **Dependency Inversion** — Provider (Shelf) implements consumer's (Book) port
2. **Consumer Adapts** — Consumer (Book) adapts from provider's facade

Chose option 2 for Bibby because:
- Simpler to reason about
- Shelf is a stable foundation module
- Both adapters (`AuthorAccessPortAdapter`, `ShelfAccessPortAdapter`) follow the same pattern
- Adapters use facades (not services) for consistency

```
book/contracts/adapters/
├── AuthorAccessPortAdapter.java  (uses AuthorFacade)
└── ShelfAccessPortAdapter.java   (uses ShelfFacade)
```

---

## Part 2: BookDomainRepository Expansion

### New Methods

```java
public interface BookDomainRepository {
    // Existing
    List<Book> getBooksByShelfId(Long shelfId);
    void registerBook(Book book);
    
    // New
    void updateBook(Book book);
    BookEntity findBookEntityByTitle(String bookTitle);
}
```

### updateBook() Implementation

```java
@Override
public void updateBook(Book book) {
    BookEntity bookEntity = bookJpaRepository.findById(book.getBookId().getId()).get();
    bookEntity.setIsbn(book.getIsbn().isbn);
    bookEntity.setTitle(book.getTitle().title());
    bookEntity.setShelfId(book.getShelfId());
    bookEntity.setGenre(book.getGenre());
    bookEntity.setDescription(book.getDescription());
    bookEntity.setAvailabilityStatus(book.getAvailabilityStatus().toString());
    bookEntity.setAuthors(bookMapper.toEntitySetFromAuthorRefs(book.getAuthors()));
    bookEntity.setPublicationYear(book.getPublicationYear());
    bookEntity.setUpdatedAt(LocalDate.now());
    bookJpaRepository.save(bookEntity);
}
```

This replaced direct `registerBook()` calls in `assignBookToShelf()` and `setShelfForBook()` with proper update semantics.

### Note: findBookEntityByTitle Returns Entity

`findBookEntityByTitle()` returns `BookEntity`, not a domain object. This is a pragmatic compromise—ideally domain repositories return domain objects. Consider refactoring to:

```java
Optional<Book> findByTitle(String title);
```

---

## Part 3: ShelfDTO Enhancement

### Problem

CLI needed to check if shelf was full, but `ShelfDTO` didn't include book count information.

### Solution

Added `bookIds` field and factory method:

```java
public record ShelfDTO(
    Long shelfId,
    String shelfLabel,
    Long bookcaseId,
    int shelfPosition,
    int bookCapacity,
    String shelfDescription,
    List<Long> bookIds  // New
) {
    public static ShelfDTO fromEntityWithBookId(ShelfEntity shelfEntity, List<Long> bookIds) {
        return new ShelfDTO(
            shelfEntity.getShelfId(),
            shelfEntity.getShelfLabel(),
            shelfEntity.getBookcaseId(),
            shelfEntity.getShelfPosition(),
            shelfEntity.getBookCapacity(),
            shelfEntity.getShelfDescription(),
            bookIds
        );
    }
}
```

**ShelfService.findShelfById() now populates book IDs:**

```java
@Transactional
public Optional<ShelfDTO> findShelfById(Long shelfId) {
    ShelfEntity shelfEntity = shelfJpaRepository.findById(shelfId).orElse(null);
    
    List<BookEntity> bookEntities = bookJpaRepository.findByShelfId(shelfId);
    List<Long> bookIds = bookEntities.stream()
            .map(BookEntity::getBookId)
            .toList();

    return Optional.of(ShelfDTO.fromEntityWithBookId(shelfEntity, bookIds));
}
```

**CLI can now check capacity inline:**

```java
if (shelfDTO.get().bookCapacity() <= shelfDTO.get().bookIds().size()) {
    throw new IllegalStateException("Shelf is full");
}
```

---

## Part 4: Fixes & Cleanup

### ShelfMapper Injection Fixed

**Before (NPE risk):**
```java
public ShelfService(...) {
    this.shelfMapper = new ShelfMapper();  // Manual instantiation
}
```

**After:**
```java
public ShelfService(..., ShelfMapper shelfMapper) {
    this.shelfMapper = shelfMapper;  // Proper injection
}
```

### isFull() Improved

**Before:**
```java
public Boolean isFull(ShelfDTO shelfDTO) {
    Optional<ShelfEntity> shelfEntity = shelfJpaRepository.findById(shelfDTO.shelfId());
    Shelf shelf = shelfEntity.map(shelfMapper::toDomain).orElse(null);
    assert shelf != null;  // Bad
    return shelf.isFull();
}
```

**After:**
```java
public Boolean isFull(ShelfDTO shelfDTO) {
    return shelfJpaRepository.findById(shelfDTO.shelfId())
            .map(shelfMapper::toDomain)
            .map(Shelf::isFull)
            .orElseThrow(() -> new RuntimeException("Shelf not found with ID: " + shelfDTO.shelfId()));
}
```

### AuthorAccessPortAdapter Field Visibility

```java
// Before
AuthorFacade authorFacade;

// After
private final AuthorFacade authorFacade;
```

### Command Renamed

```java
// Before
@Command(command = "search", description = "Search for books...")
public void searchBook()

// After
@Command(command = "find", description = "Find a book...")
public void findBook()
```

---

## Architecture State

Book module now has two outbound ports:

```
BookService
    │
    ├──▶ AuthorAccessPort ──▶ AuthorAccessPortAdapter ──▶ AuthorFacade
    │
    └──▶ ShelfAccessPort  ──▶ ShelfAccessPortAdapter  ──▶ ShelfFacade
```

All cross-module communication goes through facades via adapters. No direct service imports.

---

## Files Changed

### Book Module
- `BookService.java` — Use ShelfAccessPort, use bookDomainRepository.updateBook()
- `ShelfAccessPort.java` — New outbound port interface
- `ShelfAccessPortAdapter.java` — New adapter using ShelfFacade
- `AuthorAccessPortAdapter.java` — Added private final
- `BookDomainRepository.java` — Added updateBook(), findBookEntityByTitle()
- `BookDomainRepositoryImpl.java` — Implemented new methods

### Shelf Module
- `ShelfDTO.java` — Added bookIds field, fromEntityWithBookId()
- `ShelfService.java` — Fixed mapper injection, improved isFull(), populate bookIds
- `ShelfMapper.java` — Added explicit constructor

### CLI
- `BookCommands.java` — Renamed search→find, inline capacity check

---

## Interview Talking Points

**"Why create ShelfAccessPort when you could just use ShelfFacade directly?"**
> The port makes Book's dependencies explicit. BookService declares "I need shelf lookup capability" through ShelfAccessPort. The adapter is the wiring detail. If I wanted to test BookService in isolation, I'd mock ShelfAccessPort—I don't need to know about ShelfFacade at all. It's also consistent with AuthorAccessPort, so all cross-module dependencies follow the same pattern.

**"Why does the adapter live in Book's module, not Shelf's?"**
> We chose "consumer adapts to provider" over full dependency inversion. Book knows it needs shelf data, so Book owns the translation. Shelf just exposes its facade and doesn't care who consumes it. This is simpler than having Shelf implement Book's interfaces, which would create coupling in the other direction.

**"What's the difference between registerBook() and updateBook()?"**
> registerBook() creates a new book entity from a domain object. updateBook() finds an existing entity by ID and patches its fields from the domain object. Different semantics—create vs update. The domain repository now has both.

---

## Commit

```
feat(book): introduce ShelfAccessPort for cross-module decoupling

- Add ShelfAccessPort as Book's outbound port for shelf operations
- Add ShelfAccessPortAdapter using ShelfFacade
- Replace direct ShelfService import with ShelfAccessPort in BookService
- Add BookDomainRepository.updateBook() and findBookEntityByTitle()
- Enhance ShelfDTO with book IDs for capacity checking
- Fix ShelfMapper dependency injection
- Improve ShelfService.isFull() with proper Optional handling
- Rename 'search' command to 'find'
- Add private final to AuthorAccessPortAdapter field
```

---

## Outstanding Work

1. **BookDomainRepository.findBookEntityByTitle() returns entity** — Should return `Optional<Book>` for proper domain isolation
2. **Commented code in BookCommands** — `// Boolean isFull = shelfFacade.isFull(...)` should be removed
3. **Consider BookcaseAccessPort** — If Book ever needs bookcase data, follow the same pattern
