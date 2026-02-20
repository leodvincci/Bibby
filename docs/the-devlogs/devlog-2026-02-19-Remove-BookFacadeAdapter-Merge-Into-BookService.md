# Devlog: Remove BookFacadeAdapter — Merge Into BookService

**Date:** 2026-02-19  
**Type:** Refactor — Adapter Elimination  
**Scope:** `BookFacadeAdapter`, `BookService`, `BookFacade`  
**Status:** ✅ Complete

---

## 1. Goal

Eliminate the `BookFacadeAdapter` class by making `BookService` implement the `BookFacade` interface directly. The adapter was a thin delegation layer that added unnecessary indirection — most of its methods simply forwarded calls to `BookService` or `BookDomainRepository`, both of which `BookService` already had access to.

---

## 2. Architecture Before

```
CLI Layer (BookCirculationCommands, etc.)
    │
    ▼
BookFacade (interface)
    │
    ▼
BookFacadeAdapter (implements BookFacade)    ← THIN ADAPTER (to be removed)
    │
    ├──► BookService
    ├──► BookDomainRepository
    ├──► BookMapper
    ├──► IsbnLookupService
    └──► AuthorFacade
```

**Key Observation:** `BookFacadeAdapter` and `BookService` shared many of the same dependencies (`BookDomainRepository`, `BookMapper`, `BookJpaRepository`). Several methods existed in both classes with overlapping logic.

---

## 3. Architecture After

```
CLI Layer (BookCirculationCommands, etc.)
    │
    ▼
BookFacade (interface)
    │
    ▼
BookService (implements BookFacade)    ← SINGLE SERVICE, NO ADAPTER
    │
    ├──► BookJpaRepository
    ├──► BookDomainRepository
    ├──► BookMapper
    ├──► IsbnLookupService
    ├──► ShelfAccessPort (@Lazy)
    └──► BookcaseJpaRepository
```

---

## 4. Step-by-Step Changes

### Step 1: Make BookService Implement BookFacade

```java
// BEFORE
@Service
public class BookService {
    // ...
}

// AFTER
@Service
public class BookService implements BookFacade {
    // ...
}
```

### Step 2: Remove the Circular BookFacade Dependency

`BookService` previously injected `BookFacade` (which resolved to `BookFacadeAdapter`) in order to call `findByShelfId()` from its own `findBooksByShelf()` method. This created a circular call chain:

```
BookService.findBooksByShelf()
    → bookFacade.findByShelfId()           (BookFacadeAdapter)
        → bookService.findBooksByShelf()   (back to BookService!) — CYCLE
```

**Fix:** Remove the `BookFacade` field from `BookService` and have `findBooksByShelf()` call the local `findByShelfId()` method directly.

```java
// BEFORE
private final BookFacade bookFacade;

public List<Book> findBooksByShelf(Long id) {
    return bookFacade.findByShelfId(id);
}

// AFTER (no bookFacade field)
public List<Book> findBooksByShelf(Long id) {
    return findByShelfId(id);  // calls the local @Override method
}
```

### Step 3: Add @Override to Existing Methods

Methods that already existed in `BookService` and matched `BookFacade` signatures received `@Override`:

| Method | Notes |
|--------|-------|
| `findBookById(Long)` | Already existed in BookService |
| `findBookByTitle(String)` | Already existed, also added `@Transactional` |
| `findBookByIsbn(String)` | Already existed |
| `checkOutBook(BookDTO)` | Already existed |
| `checkInBook(String)` | Already existed |
| `getBooksForShelf(Long)` | Already existed |

### Step 4: Migrate Methods From BookFacadeAdapter

These methods were moved from `BookFacadeAdapter` into `BookService`:

| Method | Implementation Source |
|--------|---------------------|
| `updateTheBooksShelf(BookDTO, Long)` | Uses `bookMapper.toDomainFromDTO()` → `bookDomainRepository.updateTheBooksShelf()` |
| `findBookBriefByShelfId(Long)` | Uses `bookDomainRepository.getBookById()` → `bookMapper.toBookBriefFromEntity()` |
| `findBookMetaDataByIsbn(String)` | Uses `isbnLookupService.lookupBook()` → `bookMapper.toBookMetaDataResponseFromGoogleBooksResponse()` |
| `createBookFromMetaData(...)` | Delegates to `bookDomainRepository.createBookFromMetaData()` |
| `createNewBook(BookRequestDTO)` | Uses `bookMapper.toDomainFromBookRequestDTO()` → `bookDomainRepository.registerBook()` |
| `getBookDetails(Long)` | Delegates to `bookDomainRepository.getBookDetailView()` |
| `getBooksByAuthorId(Long)` | Uses `bookDomainRepository.getThreeBooksByAuthorId()` → maps to title strings |
| `getBriefBibliographicRecordsByShelfId(Long)` | Uses `bookDomainRepository.getBooksByShelfId()` → `bookMapper.toBookBriefListFromBookDTOs()` |
| `updatePublisher(String, String)` | Delegates to `bookDomainRepository.updatePublisher()` |
| `isDuplicate(String)` | Checks `bookDomainRepository.findBookByIsbn(isbn) != null` |
| `deleteByShelfIdIn(List<Long>)` | Delegates to `bookDomainRepository.deleteByShelfIdIn()` |
| `findByShelfId(Long)` | Uses `bookJpaRepository.findByShelfId()` → `bookMapper.toDomainFromEntity()` |

### Step 5: Delete BookFacadeAdapter.java

```
DELETED: src/main/java/.../book/api/adapters/BookFacadeAdapter.java
```

No other Java file imported `BookFacadeAdapter` directly — all consumers injected the `BookFacade` interface, so Spring auto-resolves to `BookService` now.

---

## 5. Bug Fix: Circular Dependency at Runtime

### The Problem

After the initial refactor, the `BibbyApplicationTests` context load failed with:

```
BeanCurrentlyInCreationException: Error creating bean with name 'shelfService':
Requested bean is currently in creation: Is there an unresolvable circular reference?
```

### Root Cause Analysis

The circular dependency chain:

```
ShelfService
    → needs BookFacade (now BookService)
        → needs ShelfAccessPort (ShelfAccessPortAdapter)
            → needs ShelfFacade (ShelfService)    ← CYCLE!
```

**Why it worked before:** `BookFacadeAdapter` was a separate bean that did NOT depend on `ShelfAccessPort`. So the cycle never formed. Now that `BookService` (which depends on `ShelfAccessPort`) IS the `BookFacade` bean, the cycle is exposed.

### The Fix: `@Lazy` Initialization

Added `@Lazy` to the `ShelfAccessPort` constructor parameter in `BookService`:

```java
public BookService(
        // ... other params ...
        @Lazy ShelfAccessPort shelfAccessPort,    // ← breaks the cycle
        BookcaseJpaRepository bookcaseJpaRepository) {
```

### How @Lazy Works

| Without @Lazy | With @Lazy |
|---------------|------------|
| Spring must fully create `ShelfAccessPortAdapter` before `BookService` can be constructed | Spring injects a **proxy** for `ShelfAccessPort` immediately |
| This triggers `ShelfService` creation, which needs `BookService` — **deadlock** | The real `ShelfAccessPortAdapter` is only resolved when a method is actually called on it |
| ❌ `BeanCurrentlyInCreationException` | ✅ By the time the proxy is used, all beans are fully initialized |

---

## 6. Concepts & Patterns Covered

### Facade Pattern
- `BookFacade` is a **facade interface** — it provides a unified API for the CLI layer to interact with the book domain
- Previously implemented by a dedicated adapter class; now implemented directly by the service

### Adapter Pattern (Removed)
- `BookFacadeAdapter` was an adapter between the `BookFacade` port and internal services
- When the adapter is just forwarding calls without transformation, it's unnecessary indirection — better to let the service implement the interface directly

### Circular Dependency
- **Constructor-based circular dependencies** cannot be resolved by Spring without intervention
- Common solutions:
  1. **`@Lazy`** — injects a proxy, defers actual creation (used here)
  2. **Setter injection** — allows partial construction
  3. **Redesign** — restructure to eliminate the cycle entirely
  4. **`@PostConstruct`** — wire dependencies after construction

### Hexagonal Architecture (Ports & Adapters)
- `BookFacade` = **inbound port** (how the outside world talks to the domain)
- `ShelfAccessPort` = **outbound port** (how the domain talks to external modules)
- `ShelfAccessPortAdapter` = **outbound adapter** (implements the port using `ShelfFacade`)
- The key insight: the **inbound port** can be implemented by the service itself when no translation logic is needed

---

## 7. Files Changed

| File | Action |
|------|--------|
| `BookService.java` | Modified — implements `BookFacade`, absorbed adapter methods, added `@Lazy` |
| `BookFacadeAdapter.java` | **Deleted** |

---

## 8. Test Verification

- ✅ `mvn compile` — clean, no errors
- ✅ `mvn test` — all tests pass
- ✅ `BibbyApplicationTests.contextLoads()` — Spring context loads successfully
- No test files needed changes (all tests mock `BookFacade` interface, not the adapter class)

---

## 9. Key Takeaways

1. **Thin adapters are a code smell** — if an adapter only forwards calls without transformation, the service can implement the interface directly.
2. **Merging beans can expose hidden circular dependencies** — when two beans are merged, their combined dependency graphs may create cycles that didn't exist before.
3. **`@Lazy` is the least-invasive fix** for constructor-based circular dependencies when a full architectural redesign isn't warranted.
4. **Always run the full test suite** after refactoring dependency injection — compilation success doesn't guarantee the Spring context will load.
5. **Interface-based injection pays off** — because all consumers depended on `BookFacade` (not `BookFacadeAdapter`), deleting the adapter required zero changes to any consumer class.

