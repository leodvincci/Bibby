# Devlog: All Four Modules Now Have Facades

**Date:** 2025-12-06  
**Focus:** Completing facade coverage across the entire domain layer  
**Commit Type:** `feat` / `refactor`

---

## Summary

**Milestone achieved:** All four domain modules now have facade interfaces. The modular monolith architecture is complete at the contract level.

| Module | Facade | Status |
|--------|--------|--------|
| Author | `AuthorFacade` | ✅ Complete |
| Book | `BookFacade` | ✅ Complete |
| Shelf | `ShelfFacade` | ✅ Complete |
| Bookcase | `BookcaseFacade` | ✅ Complete (today) |

---

## What Changed

### 1. BookcaseFacade Created

**Interface:**
```java
public interface BookcaseFacade {
    Optional<BookcaseDTO> findBookCaseById(Long id);
    List<BookcaseDTO> getAllBookcases();
}
```

**Implementation:**
```java
@Service
public class BookcaseService implements BookcaseFacade {
    // ...existing methods now fulfill the contract
}
```

### 2. BookCommands Migrated to Facades

**Before:**
```java
public class BookCommands {
    final BookcaseService bookcaseService;  // Concrete
    final IsbnLookupService isbnLookupService;  // Unnecessary
    // ...
}
```

**After:**
```java
public class BookCommands {
    private final BookcaseFacade bookcaseFacade;  // Interface
    // IsbnLookupService removed
    // ...
}
```

### 3. Bug Fixed: Wrong ID Passed to Bookcase Lookup

**Before (bug):**
```java
Optional<BookcaseDTO> bookcase = bookcaseService.findBookCaseById(shelf.get().shelfId());
//                                                                        ^^^^^^^^
//                                                               Wrong! This is the shelf's ID
```

**After (fixed):**
```java
Optional<BookcaseDTO> bookcase = bookcaseFacade.findBookCaseById(shelf.get().bookcaseId());
//                                                                          ^^^^^^^^^^
//                                                                  Correct! Bookcase's ID
```

This bug existed in two places and would have returned the wrong bookcase (or empty) at runtime.

### 4. Field Visibility Cleaned Up

Changed all fields to `private final`:

```java
// Before
final AuthorFacade authorFacade;
final ShelfFacade shelfFacade;

// After
private final AuthorFacade authorFacade;
private final BookFacade bookFacade;
private final BookcaseFacade bookcaseFacade;
private final ShelfFacade shelfFacade;
```

### 5. Constructor Cleaned Up

**Before (10 parameters):**
```java
public BookCommands(
    ComponentFlow.Builder componentFlowBuilder, 
    BookService bookService, 
    BookcaseService bookcaseService,  // Removed
    ShelfService shelfService, 
    AuthorFacade authorFacade, 
    ShelfFacade shelfFacade, 
    CliPromptService cliPrompt, 
    IsbnLookupService isbnLookupService,  // Removed
    ShelfDomainRepositoryImpl shelfDomainRepositoryPort, 
    BookFacade bookFacade
)
```

**After (9 parameters):**
```java
public BookCommands(
    ComponentFlow.Builder componentFlowBuilder,
    BookService bookService,
    ShelfService shelfService,
    AuthorFacade authorFacade,
    ShelfFacade shelfFacade,
    CliPromptService cliPrompt,
    ShelfDomainRepositoryImpl shelfDomainRepositoryPort,
    BookFacade bookFacade,
    BookcaseFacade bookcaseFacade  // Added
)
```

Net: removed `BookcaseService` and `IsbnLookupService`, added `BookcaseFacade`.

---

## The Complete Facade Map

```
┌─────────────────────────────────────────────────────────────────────┐
│                           CLI Layer                                  │
│                                                                     │
│   BookCommands ──┬── AuthorFacade ────► AuthorService               │
│                  ├── BookFacade ──────► BookService                 │
│                  ├── ShelfFacade ─────► ShelfService                │
│                  └── BookcaseFacade ──► BookcaseService             │
│                                                                     │
│   BookcaseCommands ─┬── ShelfFacade ──► ShelfService                │
│                     └── BookcaseFacade ► BookcaseService            │
│                                                                     │
│   CliPromptService ─── ShelfFacade ───► ShelfService                │
└─────────────────────────────────────────────────────────────────────┘
```

Every cross-module communication from the CLI goes through a facade interface.

---

## Facade Method Inventory

### AuthorFacade
```java
Set<AuthorDTO> findByBookId(Long bookId);
```

### BookFacade
```java
BookDTO findBookByIsbn(String isbn);
void setShelfForBook(Long bookId, Long shelfId);
BookMetaDataResponse findBookMetaDataByIsbn(String isbn);
void createBookFromMetaData(BookMetaDataResponse response, String isbn, Long shelfId);
void createNewBook(BookRequestDTO bookRequestDTO);
```

### ShelfFacade
```java
List<ShelfDTO> getAllDTOShelves(Long bookcaseId);
Optional<ShelfDTO> findShelfById(Long shelfId);
List<ShelfDTO> findByBookcaseId(Long bookcaseId);
List<BookDTO> findBooksByShelf(Long shelfId);
List<ShelfSummary> getShelfSummariesForBookcase(Long bookcaseId);
```

### BookcaseFacade
```java
Optional<BookcaseDTO> findBookCaseById(Long id);
List<BookcaseDTO> getAllBookcases();
```

---

## What's Still Directly Injected

The CLI still has some non-facade dependencies:

```java
private final BookService bookService;           // Should use BookFacade only
private final ShelfService shelfService;         // Should use ShelfFacade only
private final ShelfDomainRepositoryImpl shelfDomainRepositoryPort;  // Implementation detail
private final ComponentFlow.Builder componentFlowBuilder;       // Framework, OK
private final CliPromptService cliPrompt;        // CLI internal, OK
```

**Next steps:**
1. Migrate remaining `bookService` calls to `bookFacade`
2. Migrate remaining `shelfService` calls to `shelfFacade`
3. Hide `ShelfDomainRepositoryImpl` behind a facade method

---

## The Day's Journey

| Time | Action |
|------|--------|
| Morning | Received mentor feedback on facade pattern |
| Midday | Completed BookMapper cleanup, removed unnecessary dependencies |
| Afternoon | Created BookcaseFacade, fixed shelfId/bookcaseId bug |
| Evening | All four modules have facades, architecture tests pass |

---

## Interview Talking Points

### "How did you approach modularizing a monolith?"

> I introduced facade interfaces for each domain module—Author, Book, Shelf, and Bookcase. The CLI layer only imports from `contracts/` packages, which contain the facades and DTOs. The services implement these interfaces but stay hidden. This means I can change how BookService works internally without touching CLI code. If I ever need to extract a module to a microservice, the facade becomes the API contract.

### "What bug did the architecture help you catch?"

> I had a subtle bug where I was passing `shelf.shelfId()` to a method expecting a bookcase ID. The code compiled fine because both are `Long`. When I refactored to use `BookcaseFacade`, I had to think carefully about what data flows where. The explicit contracts made me realize I was passing the wrong ID. Without the refactoring, that bug might have gone to production.

### "Why four separate facades instead of one big one?"

> Each module owns its domain. Author, Book, Shelf, and Bookcase have different responsibilities and change for different reasons. If I put everything in one facade, changes to book logic could break shelf consumers. Separate facades mean separate contracts. When I add a method to `BookFacade`, only book-related code is affected. It's the Interface Segregation Principle in action.

---

## Metrics

| Metric | Before Today | After Today |
|--------|--------------|-------------|
| Modules with facades | 3 | 4 |
| BookCommands service deps | 3 (Book, Bookcase, Shelf) | 2 (Book, Shelf)* |
| BookCommands facade deps | 3 | 4 |
| Bugs fixed | — | 1 (wrong ID) |
| Constructor params | 10 | 9 |

*Still have some direct service usage to migrate.

---

## Files Changed

| File | Change |
|------|--------|
| `BookcaseFacade.java` | Created interface with `findBookCaseById()`, `getAllBookcases()` |
| `BookcaseService.java` | Implements `BookcaseFacade` |
| `BookCommands.java` | Use `BookcaseFacade`, fix bookcaseId bug, remove unused deps |
| `BookFacade.java` | Added imports (cleanup) |

---

## What's Next

1. **Migrate remaining service calls** — `BookService` and `ShelfService` still used directly in places
2. **Hide ShelfDomainRepositoryImpl** — Shouldn't be in CLI constructor
3. **Add ArchUnit rule** — CLI should only depend on facades, not services
4. **Consolidate mappers** — Still have `*MapperTwo` classes

---

## Reflection

Today felt like a completion. After days of incremental refactoring—introducing facades one by one, moving things to contracts packages, fixing import violations—all four modules now have consistent facades.

The bug fix was a good reminder of why this work matters. A simple `shelfId` vs `bookcaseId` mistake is easy to make and hard to catch. The refactoring forced me to look at every call site, and that's when I spotted it.

The architecture is now:
- **Documented** — Package structure tells the story
- **Enforced** — ArchUnit tests fail on violations  
- **Complete** — All modules have facades
- **Consistent** — Same pattern everywhere

Still have cleanup to do (remove remaining service deps, consolidate mappers), but the structural work is done.
