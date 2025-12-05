# Devlog: Wiring Up the Facade Pattern

**Date:** 2025-12-05  
**Focus:** Implementing the facade pattern for true module decoupling  
**Commit Type:** `feat` / `refactor`

---

## Summary

This commit is a milestone. After scaffolding facade interfaces across several refactoring sessions, I finally wired one up end-to-end. `CliPromptService` now depends on `ShelfFacade` (the interface), not `ShelfService` (the implementation). This is the architectural pattern I've been building toward.

---

## What Changed

### 1. ShelfFacade Wired Up

**Before:**
```java
@Component
public class CliPromptService {
    private final ShelfService shelfService;  // Concrete class
    
    public CliPromptService(..., ShelfService shelfService) {
        this.shelfService = shelfService;
    }
}
```

**After:**
```java
@Component
public class CliPromptService {
    private final ShelfFacade shelfFacade;    // Interface only
    
    public CliPromptService(..., ShelfFacade shelfFacade) {
        this.shelfFacade = shelfFacade;
    }
}
```

### 2. ShelfService Implements the Facade

```java
@Service
public class ShelfService implements ShelfFacade {
    
    @Override
    public List<ShelfDTO> getAllDTOShelves(Long bookcaseId) {
        return getAllShelves(bookcaseId).stream()
                .map(ShelfDTO::fromEntity)
                .toList();
    }
}
```

### 3. ShelfDTO at the Boundary

The facade returns `ShelfDTO`, not `ShelfEntity` or `Shelf`:

```java
public interface ShelfFacade {
    List<ShelfDTO> getAllDTOShelves(Long bookcaseId);
}
```

This keeps infrastructure types (`ShelfEntity`) and domain types (`Shelf`) internal to the shelf module.

### 4. BookFacade Scaffolded

```java
// Changed from empty class to interface
public interface BookFacade {
}

// BookService now implements it
@Service
public class BookService implements BookFacade {
    // ...
}
```

Ready for when Book needs to expose operations to other modules.

### 5. Shelf Domain Fully Decoupled

```java
// Before
private List<Book> books;      // Direct object reference

// After
private List<Long> bookIds;    // ID reference only
```

The `Shelf` domain class no longer imports anything from the Book module.

---

## Why This Matters

### The Dependency Inversion in Action

```
BEFORE:
┌─────────────────────────────────────────┐
│ CLI Layer                               │
│   CliPromptService                      │
│         │                               │
│         ▼ (depends on concrete class)   │
│   ShelfService                          │
└─────────────────────────────────────────┘

AFTER:
┌─────────────────────────────────────────┐
│ CLI Layer                               │
│   CliPromptService                      │
│         │                               │
│         ▼ (depends on interface)        │
│   ShelfFacade  ◄─── (implements)        │
│         ▲                               │
│         │                               │
│   ShelfService                          │
└─────────────────────────────────────────┘
```

The arrow flip is everything. `CliPromptService` no longer knows `ShelfService` exists. It only knows the contract (`ShelfFacade`).

### What This Enables

1. **Testability:** I can mock `ShelfFacade` in CLI tests without spinning up the real service.

2. **Module extraction:** If I ever extract Shelf to a separate deployable, the CLI code doesn't change—it still depends on the interface.

3. **Enforced boundaries:** I can now write ArchUnit tests that fail if CLI imports from `application/` packages.

4. **Parallel development:** Another developer could work on `ShelfService` internals while I work on CLI features. We only coordinate on the facade contract.

---

## The Journey

This commit is the payoff from several sessions of prep work:

| Session | What I Did |
|---------|------------|
| Earlier | Created empty facade interfaces (scaffolding) |
| Earlier | Reorganized packages into hexagonal structure |
| Earlier | Changed aggregate references from objects to IDs |
| Earlier | Introduced DTOs at API boundaries |
| **Now** | **Wired facade end-to-end** |

Each step was necessary. You can't wire up a facade if:
- You don't have the interface
- Your packages aren't organized to support it  
- Your domain models leak infrastructure types
- You don't have DTOs for the boundary

---

## Technical Details

### DTO with Static Factory

```java
// ShelfDTO.java (in api/ package)
public record ShelfDTO(Long shelfId, String shelfLabel, Long bookcaseId) {
    
    public static ShelfDTO fromEntity(ShelfEntity entity) {
        return new ShelfDTO(
            entity.getShelfId(),
            entity.getShelfLabel(),
            entity.getBookcaseId()
        );
    }
}
```

The `fromEntity()` factory lives on the DTO. This is a pragmatic choice—the DTO knows how to create itself from an entity. The mapping happens at the boundary, inside the module that owns the entity.

### Removed Direct Imports

**CliPromptService no longer imports:**
```java
- import com.penrose.bibby.library.author.domain.Author;
- import com.penrose.bibby.library.bookcase.application.BookcaseService;
- import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
- import com.penrose.bibby.library.shelf.application.ShelfService;
```

**Now only imports:**

```java
+import com.penrose.bibby.library.author.contracts.AuthorDTO;
import com.penrose.bibby.library.shelf.contracts.ShelfDTO;
import com.penrose.bibby.library.shelf.contracts.ShelfFacade;
+
+ 
```

The CLI layer only sees `api/` packages. Domain and infrastructure are hidden.

---

## Struggle Journal

### Challenge: What Should the Facade Return?

Options considered:
1. Return `ShelfEntity` — No, that's infrastructure leakage
2. Return `Shelf` — No, that's domain leakage  
3. Return `ShelfDTO` — Yes, DTOs are meant for boundaries

The facade is an API boundary. APIs speak in DTOs.

### Challenge: Where Does Mapping Happen?

The `ShelfDTO.fromEntity()` call happens inside `ShelfService`:

```java
public List<ShelfDTO> getAllDTOShelves(Long bookcaseId) {
    return getAllShelves(bookcaseId).stream()
            .map(ShelfDTO::fromEntity)
            .toList();
}
```

This keeps the mapping inside the shelf module. Callers never see entities.

### TODO: Clean Up

1. Remove debug output: `System.out.println(s.bookcaseId());`
2. Fix getter name: `getBooks()` should be `getBookIds()` to match the field

---

## Interview Talking Points

### "Explain the facade pattern and why you used it."

> The facade provides a simplified interface to a module's capabilities. In my case, `ShelfFacade` is the contract that other modules use to interact with shelf functionality. The CLI depends on this interface, not the concrete `ShelfService`. This means I can change how shelves work internally—different database, different logic—without touching CLI code. It's dependency inversion: high-level modules don't depend on low-level modules; both depend on abstractions.

### "How does this support a modular monolith?"

> Each domain module (Book, Shelf, Author, Bookcase) exposes a facade interface in its `api/` package. Other modules only import from `api/`—they never reach into `application/`, `domain/`, or `infrastructure/`. This means each module is logically independent even though they deploy together. If I needed to extract Shelf to a microservice later, I'd implement the same `ShelfFacade` interface over HTTP. The callers wouldn't change.

### "What's the difference between a facade and just using a service?"

> When you inject a service directly, you're coupled to that implementation. You can see all its public methods, you depend on its package location, and you're importing concrete classes. With a facade interface, you're coupled only to the contract. The implementation can live anywhere, be swapped out, or be mocked for tests. It's the difference between "I need ShelfService" and "I need something that can give me shelf data."

---

## What's Next

1. **Wire up AuthorFacade:** Same pattern—`BookService` should depend on `AuthorFacade`, not `AuthorService`

2. **Wire up BookcaseFacade:** CLI still has some direct bookcase dependencies

3. **Add ArchUnit test:** Enforce that `cli/` packages only import from `*/api/` packages

4. **Consolidate mappers:** Still have `BookMapper` and `BookMapperTwo` floating around

5. **Add facade method for BookFacade:** Currently empty interface, needs actual operations

---

## Files Changed

| File | Change |
|------|--------|
| `CliPromptService.java` | Depend on `ShelfFacade` instead of `ShelfService` |
| `ShelfFacade.java` | Define `getAllDTOShelves()` contract |
| `ShelfService.java` | Implement `ShelfFacade` |
| `ShelfDTO.java` | Add DTO with `fromEntity()` factory |
| `BookFacade.java` | Convert from class to interface |
| `BookService.java` | Implement `BookFacade` |
| `Shelf.java` | Change `List<Book>` to `List<Long> bookIds` |

---

## Reflection

This felt like a breakthrough. I'd been reading about dependency inversion and facade patterns, but actually wiring one up—seeing the imports change, seeing the concrete dependency disappear—made it click.

The prep work mattered. If I'd tried to do this with a flat package structure and entities leaking everywhere, it would have been a mess. The incremental refactoring built the foundation.

Next step: do the same for Author and Bookcase, then enforce with tests.
