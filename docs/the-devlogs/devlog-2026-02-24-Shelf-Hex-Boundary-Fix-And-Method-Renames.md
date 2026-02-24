# Devlog: Shelf Hexagonal Boundary Fix and Method Renames

**Date:** 2026-02-24
**Branch:** `refactor/shelf-hex-boundary-and-method-renames`
**Range:** `origin/main..HEAD`
**Commits:**

| SHA | Subject |
|-----|---------|
| `620e98c` | Relocate BookAccessAdapter from cataloging to shelf module |
| `12ac516` | Rename shelf port methods and refactor CreateShelfUseCase ownership |
| `39cc142` | Remove unused imports from test files |
| `585b12a` | Fix circular bean dependency and relocate BookAccessAdapter test |
| `ccde7a5` | Refactor BookcaseCommands and ShelfService for improved readability |
| `10d6bce` | Rename bookCommandUseCases to BookCommandUseCases |

---

## Summary

- **What problem was I solving?** The `BookAccessAdapter` lived in the wrong module (`cataloging/book`, the provider side), bypassed Book's public API by calling `BookDomainRepository` directly, and was inconsistent with every other cross-module adapter in the codebase. Shelf port and repository methods also had vague, non-intent-revealing names, and `CreateShelfUseCase` didn't own its own domain object construction.

- **What was the "before" state?** `BookAccessAdapter` sat in `cataloging/book/infrastructure/adapter/outbound/`, implemented Shelf's `BookAccessPort`, and reached into `BookDomainRepository`. ShelfFacade had methods like `createShelf()`, `getShelfSummariesForBookcase()`, `deleteAllShelvesInBookcase()`. ShelfDomainRepositoryPort had generic names like `save()`, `getById()`, and a duplicate `findById()`. `CreateShelfUseCase.execute()` accepted a pre-built `Shelf` object from `ShelfService`. The use-case class `bookCommandUseCases` had a lowercase class name.

- **Key outcomes:**
  - `BookAccessPortAdapter` now lives in `shelf/infrastructure/adapter/outbound/` (consumer-side) and delegates through `BookFacade`—consistent with all other cross-module adapters
  - No foreign DTOs leak into the Shelf module; the adapter's only cross-module import is `BookFacade`
  - Shelf facade and repository port methods are intent-revealing (`createShelfInBookcaseByBookcaseId`, `getShelfByShelfId`, `createNewShelfInBookcase`)
  - Domain object construction moved from `ShelfService` into `CreateShelfUseCase` where it belongs
  - Circular Spring bean dependency resolved with `@Lazy` at the cross-module boundary
  - `bookCommandUseCases` renamed to `BookCommandUseCases` (Java convention)
  - All 141 tests pass

---

## Commit-by-Commit Analysis

### Commit 1: `620e98c` — Relocate BookAccessAdapter from cataloging to shelf module

**Intent:** Fix the hexagonal architecture violation where a cross-module adapter lived in the provider module and bypassed the provider's public API.

**Files touched:**
| File | Reason |
|------|--------|
| `BookAccessAdapter.java` (deleted) | Old provider-side adapter removed from `cataloging/book` |
| `BookAccessPortAdapter.java` (new) | New consumer-side adapter in `shelf/infrastructure` |
| `BookFacade.java` | Added `getBookIdsByShelfId()` and `deleteByShelfId()` to Book's public API |
| `BookService.java` | Implemented the two new facade methods |
| `bookCommandUseCases.java` | Added delegation methods (later removed in commit 4) |
| `BookDomainRepository.java` | Added `getBookIdsByShelfId()` contract |
| `BookDomainRepositoryImpl.java` | Implemented `getBookIdsByShelfId()` via JPA |
| `BookAccessPort.java` | Updated Javadoc to reflect new adapter location |

**Key code changes:**

The old adapter reached into Book internals:
```java
// DELETED: cataloging/book/infrastructure/adapter/outbound/BookAccessAdapter.java
private final BookDomainRepository bookDomainRepository;
List<BookDTO> books = bookDomainRepository.getBooksByShelfId(shelfId);
return books.stream().map(book -> book.id()).collect(Collectors.toList());
```

The new adapter delegates through the public facade with zero foreign types:
```java
// NEW: stacks/shelf/infrastructure/adapter/outbound/BookAccessPortAdapter.java
private final BookFacade bookFacade;
return bookFacade.getBookIdsByShelfId(shelfId);
```

**Architecture notes:**
- Dependency arrow before: `BookAccessAdapter (cataloging) → BookDomainRepository (cataloging) ← imports → BookAccessPort (shelf)`
- Dependency arrow after: `BookAccessPortAdapter (shelf) → BookFacade (cataloging public API)`
- This matches the pattern established by `ShelfAccessPortAdapter` in both `bookcase` and `cataloging/book` modules
- `BookFacade` gained two new methods (`getBookIdsByShelfId`, `deleteByShelfId`) that expose existing capabilities through the public contract
- `BookDomainRepositoryImpl.getBookIdsByShelfId()` uses `bookJpaRepository.findByShelfId()` mapped to IDs, avoiding the `BookDTO` intermediary the old adapter needed

**Risk:** Low. Behavior is identical—same data, same operations, different wiring path.

---

### Commit 2: `12ac516` — Rename shelf port methods and refactor CreateShelfUseCase ownership

**Intent:** Make method names intent-revealing and push domain object construction into the use case.

**Files touched (12):**
| File | Reason |
|------|--------|
| `ShelfFacade.java` | Renamed 3 inbound port methods |
| `ShelfDomainRepositoryPort.java` | Renamed 2 methods, removed duplicate `findById` |
| `ShelfService.java` | Updated delegation, added Javadoc, removed unused logger |
| `CreateShelfUseCase.java` | Now accepts primitives, constructs `Shelf` internally |
| `QueryShelfUseCase.java` | Updated to use renamed repo methods |
| `ShelfDomainRepositoryPortImpl.java` | Implemented renames, removed duplicate, uses `ShelfMapper` |
| `ShelfMapper.java` | Fixed `toEntity` to include `bookcaseId` |
| `ShelfAccessPortAdapter.java` (bookcase) | Updated to renamed facade methods |
| `BookcaseCommands.java` | Updated to renamed facade method |
| `ShelfServiceTest.java` | Updated for renames, simplified `createShelf` test |
| `CreateShelfUseCaseTest.java` | Tests primitive API, verifies construction via `ArgumentCaptor` |
| `QueryShelfUseCaseTest.java` | Updated for renamed methods |

**Key renames:**

| Before | After |
|--------|-------|
| `ShelfFacade.createShelf()` | `createShelfInBookcaseByBookcaseId()` |
| `ShelfFacade.getShelfSummariesForBookcase()` | `getShelfSummariesForBookcaseByBookcaseId()` |
| `ShelfFacade.deleteAllShelvesInBookcase()` | `deleteAllShelvesInBookcaseByBookcaseId()` |
| `ShelfDomainRepositoryPort.getById()` | `getShelfByShelfId()` |
| `ShelfDomainRepositoryPort.save()` | `createNewShelfInBookcase()` |
| `ShelfDomainRepositoryPort.findById()` | Removed (duplicate of `getById`) |

**Responsibility shift — `CreateShelfUseCase`:**

Before: ShelfService constructed the `Shelf` domain object and passed it to the use case.
```java
// ShelfService (BEFORE)
Shelf shelf = new Shelf(shelfLabel, position, bookCapacity, null, List.of(), bookcaseId);
createShelfUseCase.execute(shelf);
```

After: The use case owns domain construction. ShelfService passes only primitives.
```java
// CreateShelfUseCase (AFTER)
public void execute(String shelfLabel, int shelfPosition, int bookCapacity, Long bookcaseId) {
    Shelf shelf = new Shelf(shelfLabel, shelfPosition, bookCapacity, null, List.of(), bookcaseId);
    shelfDomainRepositoryPort.createNewShelfInBookcase(shelf);
}
```

This is better because:
- The use case is the correct place to own domain logic—it's the command boundary
- `ShelfService` is just a thin routing layer delegating to use cases
- Tests for domain construction now live in `CreateShelfUseCaseTest` where they actually matter

**Architecture notes:**
- `getShelfByShelfId()` now throws `RuntimeException` instead of returning `null`—this is a behavior change in `ShelfDomainRepositoryPortImpl`
- `ShelfMapper.toEntity()` was fixed to include `bookcaseId` (was missing before, which would have caused bugs on `createNewShelfInBookcase`)
- Duplicate `findById(Long)` removed from the port—only `getShelfByShelfId(ShelfId)` remains

**Risk:** Medium. Broad rename across 12 files. The `RuntimeException` on not-found is a behavior change that callers like `QueryShelfUseCase.findShelfById()` handle via null-check (now dead code since the method throws instead). This inconsistency should be cleaned up.

---

### Commit 3: `39cc142` — Remove unused imports from test files

**Intent:** Cleanup.

**Files touched:**
- `BookcardRendererTest.java` — removed unused `@SpringBootTest` import
- `AuthorRepositoryTest.java` — removed unused `import static org.junit.jupiter.api.Assertions.*`

**Risk:** None.

---

### Commit 4: `585b12a` — Fix circular bean dependency and relocate BookAccessAdapter test

**Intent:** Resolve the circular Spring dependency introduced by commit 1, and relocate the adapter test to match the adapter's new module.

**The circular dependency chain:**
```
ShelfService → QueryShelfUseCase → ShelfDomainRepositoryPortImpl
  → BookAccessPortAdapter → BookFacade (BookService)
    → bookCommandUseCases → ShelfAccessPortAdapter → ShelfFacade (ShelfService) ← CYCLE
```

**Fix applied — two changes:**

1. **`@Lazy` on `BookAccessPortAdapter`'s `BookFacade` constructor param** — Spring creates a lazy proxy at startup; the real bean is resolved on first method call. Safe because `BookAccessPort` is never invoked during bean initialization.

2. **Route `BookService.deleteByShelfId()` and `getBookIdsByShelfId()` directly through `BookDomainRepository`** instead of `bookCommandUseCases`. This avoids the unnecessary detour through `bookCommandUseCases → ShelfAccessPort` for operations that don't need shelf validation.

**Test relocation:**
- Deleted `BookAccessAdapterTest` from `cataloging/book/infrastructure/adapter/` (mocked `BookDomainRepository`)
- Created `BookAccessPortAdapterTest` in `shelf/infrastructure/adapter/outbound/` (mocks `BookFacade`, verifies delegation for both `getBookIdsByShelfId` and `deleteBooksOnShelves`)

**Risk:** `@Lazy` is a well-understood Spring pattern, but it does mean the dependency is resolved at runtime rather than startup. If `BookFacade` bean creation fails, the error surfaces on first call rather than at application start. This is acceptable for a cross-module boundary.

---

### Commit 5: `ccde7a5` — Refactor BookcaseCommands and ShelfService for improved readability

**Intent:** Formatting and Javadoc cleanup.

**Files touched:** `BookcaseCommands.java`, `ShelfService.java`, `CreateShelfUseCase.java`, `ShelfFacade.java`, `ShelfDomainRepositoryPortImpl.java`, `BookcardRendererTest.java`

**Risk:** None. Formatting only.

---

### Commit 6: `10d6bce` — Rename bookCommandUseCases to BookCommandUseCases

**Intent:** Fix Java naming convention violation (class name must start with uppercase).

**Files touched:**
- `bookCommandUseCases.java` → `BookCommandUseCases.java` (class + filename)
- `BookService.java` — updated import and field type

**Risk:** None. Pure rename.

---

## End-to-End Flow: Shelf Querying Book IDs

The critical cross-module flow after this change:

```
CLI/Controller
  → ShelfFacade.findShelfById(shelfId)
    → ShelfService (thin delegation)
      → QueryShelfUseCase.findShelfById(shelfId)
        → ShelfDomainRepositoryPortImpl.getShelfByShelfId(ShelfId)
          → ShelfJpaRepository.findById()        // shelf entity
          → BookAccessPort.getBookIdsByShelfId()  // cross-module call
            → BookAccessPortAdapter (@Lazy BookFacade)
              → BookFacade.getBookIdsByShelfId()  // public API
                → BookService (implements BookFacade)
                  → BookDomainRepository.getBookIdsByShelfId()
                    → BookJpaRepository.findByShelfId() → map to IDs
          → ShelfMapper.toDomainFromEntity(entity, bookIds)
        → return Optional<Shelf>
```

**Why this is better:**
- Shelf never touches `BookDomainRepository`, `BookDTO`, or any Book internal type
- The only cross-module type is `BookFacade` (Book's public inbound port)
- Consistent with how Bookcase accesses Shelf (via `ShelfFacade`)
- Consistent with how Book accesses Shelf (via `ShelfAccessPort` → `ShelfFacade`)

**Dependencies removed:**
- `BookAccessAdapter` no longer imports `BookDomainRepository` or `BookDTO` from Shelf's perspective
- Shelf's compile-time boundary is clean: only `BookFacade` crosses the module line

**Imports that could be flagged:**
- `BookAccessPortAdapter` imports `BookFacade` — this is intentional and correct (infrastructure layer is allowed to cross module boundaries)
- No domain-layer imports cross module boundaries

---

## Test Coverage

**Existing tests (updated):**
- `ShelfServiceTest` — 4 tests covering delegation to all three use cases
- `CreateShelfUseCaseTest` — 3 tests, now uses `ArgumentCaptor` to verify domain construction
- `QueryShelfUseCaseTest` — 5 tests, updated for renamed methods
- `BookAccessPortAdapterTest` — 3 tests (new), verifies `BookFacade` delegation

**Verification commands:**
```bash
mvn test                                          # all 141 tests
mvn test -pl . -Dtest=BookAccessPortAdapterTest   # adapter delegation
mvn test -pl . -Dtest=CreateShelfUseCaseTest       # domain construction
mvn test -pl . -Dtest=ShelfServiceTest             # service delegation
mvn test -pl . -Dtest=QueryShelfUseCaseTest        # query use case
```

**Tests that should be added:**
1. `ShelfDomainRepositoryPortImplTest` — `getShelfByShelfId()` now throws `RuntimeException` on not-found instead of returning null. No test covers this behavior change.
2. `BookService` integration — `getBookIdsByShelfId()` and `deleteByShelfId()` are new facade methods with no unit test coverage via `BookService` directly.
3. `CreateShelfUseCase` edge cases — what happens with null/empty `shelfLabel`, negative `bookCapacity`, null `bookcaseId`?
4. `BookAccessPortAdapter` — test with `@Lazy` proxy in a Spring integration test to verify the lazy resolution actually works.

---

## Lessons Learned

1. **Moving an adapter between modules isn't free.** The relocation introduced a circular dependency that didn't exist before because the old adapter bypassed the public API. Going through facades creates longer bean chains and increases cycle risk.

2. **`@Lazy` is a pragmatic fix, not an architectural one.** It breaks the cycle at the Spring wiring level, but the bidirectional dependency between Book and Shelf modules still exists in the code. The real fix is separating the Book module's read-only queries from its shelf-dependent commands.

3. **Domain construction belongs in the use case, not the service.** `ShelfService.createShelf()` was doing two things: building the `Shelf` object and delegating. After the refactor, `ShelfService` is purely a delegation layer and `CreateShelfUseCase` owns the full command.

4. **Generic method names hide intent.** `save()` tells you nothing about what's being saved or where. `createNewShelfInBookcase()` communicates the operation, the entity, and the context. The same pattern (`getById` → `getShelfByShelfId`) makes the port self-documenting.

5. **`ShelfMapper.toEntity()` was missing `bookcaseId`.** This was a latent bug—the old `save()` method in `ShelfDomainRepositoryPortImpl` set `bookcaseId` manually, bypassing the mapper. The refactor exposed it by routing through `ShelfMapper.toEntity()`.

6. **Test placement should follow adapter placement.** When `BookAccessAdapterTest` stayed in `cataloging/book` after the adapter moved to `shelf`, it couldn't even compile. Tests and the code they cover should always live in the same module.

---

## Follow-Ups

### Immediate (today)
- Clean up the null-check in `QueryShelfUseCase.findShelfById()` — it checks for null, but `getShelfByShelfId()` now throws `RuntimeException`. Either catch the exception or revert the throw to return null.
- Verify that `ShelfMapper.toEntity()` handles the null `ShelfId` case for new shelves (before persistence assigns the ID).

### Short-term hardening (this week)
- Add unit test for `ShelfDomainRepositoryPortImpl.getShelfByShelfId()` covering the not-found throw behavior.
- Add unit tests for `BookService.getBookIdsByShelfId()` and `BookService.deleteByShelfId()`.
- Consider whether `BookFacade` is growing too large (now 20+ methods). Splitting into `BookQueryFacade` / `BookCommandFacade` would reduce the surface area of the `@Lazy` proxy.

### Strategic refactors (later)
- Extract a lightweight `BookQueryPort` in the Book module that doesn't depend on `ShelfAccessPort`, eliminating the circular dependency entirely and removing the need for `@Lazy`.
- `BookService` is 287 lines with many responsibilities. Continue decomposing into use cases (as done for Shelf).
- Audit all `@Lazy` usages across the codebase — each one signals a module boundary that could be architecturally cleaner.
