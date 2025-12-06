# Devlog: Facade Pattern Maturity and CLI Cleanup

**Date:** 2025-12-06  
**Focus:** Deepening facade adoption, cleaning up mappers, reorganizing CLI  
**Commit Type:** `refactor`

---

## Summary

The facade pattern is now maturing beyond just "CLI uses facades." The service layer itself is starting to use facades for cross-module communication. Also reorganized CLI packages, simplified the BookMapper, and continued removing old code paths.

---

## What Changed

### 1. BookService Uses AuthorFacade

This is a significant shift. Previously only the CLI used facades. Now the service layer respects the same boundaries:

**Before:**
```java
public class BookService {
    private final AuthorService authorService;  // Direct dependency
    
    public void checkOutBook(BookDTO bookDTO) {
        Set<AuthorDTO> authors = authorService.findByBookId(bookDTO.id());
    }
}
```

**After:**
```java
public class BookService implements BookFacade {
    private final AuthorFacade authorFacade;    // Interface dependency
    
    public void checkOutBook(BookDTO bookDTO) {
        Set<AuthorDTO> authors = authorFacade.findByBookId(bookDTO.id());
    }
}
```

**Why this matters:** The facade pattern isn't just for external consumers (CLI). Internal services should also communicate through contracts when crossing module boundaries. Book module → Author module communication now goes through `AuthorFacade`.

---

### 2. BookFacade Expanded

Added `createNewBook()` to the facade interface:

```java
public interface BookFacade {
    BookDTO findBookByIsbn(String isbn);
    void setShelfForBook(Long bookId, Long shelfId);
    BookMetaDataResponse findBookMetaDataByIsbn(String isbn);
    void createBookFromMetaData(BookMetaDataResponse response, String isbn, Long shelfId);
    void createNewBook(BookRequestDTO bookRequestDTO);  // ← New
}
```

The facade is growing organically based on what consumers need. That's the right evolution—don't pre-design the API, let usage drive it.

---

### 3. CLI Package Reorganization

**Before:**
```
cli/
├── BookCommands.java
├── BookcaseCommands.java
├── book/
│   └── BookCommandLine.java
└── bookcase/
    └── BookcaseCommandLine.java
```

**After:**
```
cli/
├── commands/
│   ├── BookCommands.java
│   └── BookcaseCommands.java
├── book/
│   └── BookCommandLine.java
└── bookcase/
    └── BookcaseCommandLine.java
```

The `commands/` sub-package groups the shell command classes together.

---

### 4. BookMapper Simplified

Removed unnecessary dependencies from BookMapper:

**Before:**
```java
public class BookMapper {
    private final ShelfDomainRepositoryImpl shelfDomainRepositoryImpl;
    private final AuthorService authorService;
    ShelfMapper shelfMapper;

    private BookMapper(ShelfMapper shelfMapper, 
                       ShelfDomainRepositoryImpl shelfDomainRepositoryImpl, 
                       AuthorService authorService) {
        // ...
    }
}
```

**After:**
```java
public class BookMapper {
    ShelfMapper shelfMapper;

    private BookMapper(ShelfMapper shelfMapper) {
        this.shelfMapper = shelfMapper;
    }
}
```

**What was removed:**
- `ShelfDomainRepositoryImpl` — Mapper was loading Shelf domain objects during mapping. Now it just uses the shelfId directly.
- `AuthorService` — Mapper was looking up authors by name. Now it receives author data as parameters.

**Principle:** Mappers should map, not fetch. If you need data, get it before calling the mapper.

---

### 5. Method Rename

```java
// Before
@Command(command = "add")
public void addBook(...) { }

// After
@Command(command = "add")
public void registerBook(...) { }
```

The command is still `book add`, but the method name is more descriptive of the action.

---

### 6. Fixed findBooksByShelf()

Cleaned up the null-check logic:

**Before:**
```java
public List<BookDTO> findBooksByShelf(Long shelfId) {
    List<BookEntity> books = bookRepository.findByShelfId(shelfId);
    List<BookDTO> bookDTOs = new ArrayList<>();
    if (books == null) {
        return List.of();
    }
    return books.stream()
            .map(BookDTO::fromEntity)
            .collect(Collectors.toList());
}
```

**After:**
```java
@Transactional
public List<BookDTO> findBooksByShelf(Long shelfId) {
    List<BookEntity> books = bookRepository.findByShelfId(shelfId);
    return books.stream()
            .map(BookDTO::fromEntity)
            .collect(Collectors.toList());
}
```

The unused `bookDTOs` variable and redundant null check are gone. Added `@Transactional` for proper session management.

---

### 7. Fixed ShelfDTO Parameter Naming

**Before:**
```java
public static ShelfDTO fromEntity(ShelfEntity shelfDTO) {  // Misleading name
    return new ShelfDTO(shelfDTO.getShelfId(), ...);
}
```

**After:**
```java
public static ShelfDTO fromEntity(ShelfEntity shelfEntity) {  // Accurate name
    return new ShelfDTO(shelfEntity.getShelfId(), ...);
}
```

Small thing, but naming matters. A parameter called `shelfDTO` that's actually a `ShelfEntity` causes confusion.

---

## Architecture Evolution

### Before Today

```
┌─────────────────────────────────────────────────────────┐
│ CLI                                                     │
│   ├── BookFacade ─────────────────┐                    │
│   ├── AuthorFacade ───────────────┤                    │
│   └── ShelfFacade ────────────────┤                    │
│                                   ▼                    │
│                           ┌───────────────┐            │
│                           │ Service Layer │            │
│                           │               │            │
│                           │ BookService ──┼──► AuthorService (direct)
│                           │               │            │
│                           └───────────────┘            │
└─────────────────────────────────────────────────────────┘
```

### After Today

```
┌─────────────────────────────────────────────────────────┐
│ CLI                                                     │
│   ├── BookFacade ─────────────────┐                    │
│   ├── AuthorFacade ───────────────┤                    │
│   └── ShelfFacade ────────────────┤                    │
│                                   ▼                    │
│                           ┌───────────────┐            │
│                           │ Service Layer │            │
│                           │               │            │
│                           │ BookService ──┼──► AuthorFacade (interface)
│                           │               │            │
│                           └───────────────┘            │
└─────────────────────────────────────────────────────────┘
```

The key change: BookService now uses AuthorFacade, not AuthorService. Cross-module communication is going through contracts at all layers, not just the CLI.

---

## Bug Fixed / Bug Noted

### Fixed: Variable Naming Confusion

`shelfDTO` was used as a parameter name for `ShelfEntity` objects. Fixed to `shelfEntity`.

### Noted: Duplicate Book Creation

In the current code:
```java
bookFacade.createNewBook(bookRequestDTO);
bookService.createNewBook(bookRequestDTO);  // ← Creates book twice!
```

**TODO:** Remove the `bookService.createNewBook()` call. The facade call is the correct path.

---

## Interview Talking Points

### "How deep does the facade pattern go?"

> Initially I only used facades at the CLI layer—the outermost boundary. But I realized that cross-module communication should also go through facades. When BookService needs author data, it should call AuthorFacade, not AuthorService directly. This keeps module boundaries consistent regardless of who the caller is.

### "How do you decide what goes in a facade?"

> I let usage drive it. When a consumer needs something, I add it to the facade. `createNewBook()` was added because the CLI needed to create books. I don't pre-design the entire API—I evolve it based on actual needs. This avoids over-engineering.

### "What's wrong with mappers having dependencies?"

> Mappers should have a single responsibility: converting between representations. If a mapper needs to fetch data from a repository or call a service, it's doing too much. That logic should happen before the mapper is called. I removed `ShelfDomainRepositoryImpl` and `AuthorService` from BookMapper—now it just receives data and maps it.

---

## What's Next

1. **Remove duplicate book creation** — Delete `bookService.createNewBook()` call in CLI
2. **Apply same pattern to ShelfService** — Use BookFacade when ShelfService needs book data
3. **Create BookcaseFacade** — Last module without a facade
4. **Consolidate mappers** — Still have `BookMapperTwo` floating around

---

## Files Changed

| File | Change |
|------|--------|
| `BookCommands.java` | Moved to `cli.commands`, added facade call, renamed `addBook` → `registerBook` |
| `BookcaseCommands.java` | Moved to `cli.commands` |
| `BookService.java` | Now uses `AuthorFacade` instead of `AuthorService` |
| `BookFacade.java` | Added `createNewBook()` method |
| `BookMapper.java` | Removed `ShelfDomainRepositoryImpl` and `AuthorService` dependencies |
| `ShelfService.java` | Cleaned up `findBooksByShelf()`, added `@Transactional` |
| `ShelfDTO.java` | Fixed parameter naming (`shelfDTO` → `shelfEntity`) |
| `Author.java` | Removed blank lines between constructors (formatting) |

---

## Metrics

| Metric | Before | After |
|--------|--------|-------|
| BookMapper dependencies | 3 | 1 |
| Services using facades internally | 0 | 1 (BookService → AuthorFacade) |
| Facade methods (BookFacade) | 4 | 5 |

---

## Reflection

The facade pattern is clicking now. It's not just about hiding implementation from the CLI—it's about defining clear contracts between modules. When BookService needs to talk to the Author module, it goes through AuthorFacade. The module boundary is respected regardless of who's calling.

The mapper cleanup felt good too. `BookMapper` was a tangled mess of dependencies. Now it's simple: give it data, get mapped data back. That's what a mapper should be.

Still have some cleanup to do (duplicate book creation, consolidate mappers), but the architecture is maturing.
