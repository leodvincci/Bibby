# Devlog: ShelfFactory to ShelfFacade Refactor

**Date:** 2026-02-18
**Branch:** `refactor/shelf-factory-to-facade`
**Theme:** Facade Pattern / Decoupling / Domain Cleanup

---

## Context

`ShelfFactory` had been sitting in the domain layer doing a simple job: stamp out `ShelfEntity` instances with a few setter calls. It worked, but it carried the wrong kind of weight. Factory classes living in the domain that reach down into infrastructure are a layering smell — domain shouldn't know about entities, and a factory that only sets four fields is more overhead than it's worth.

This refactor replaces the factory with a proper `createShelf()` method behind the existing `ShelfFacade` interface. The result is a simpler dependency graph, better-validated shelf creation, and a cleaner `BookcaseService` that delegates through the facade instead of juggling a raw JPA repository and a factory bean side-by-side.

A secondary cleanup removed `BookcaseFacade` from `ShelfService`, which had crept in to support `toShelfOption()` but introduced a circular-style dependency between two services in the same bounded context.

---

## Commit Breakdown

### Commit 1: `662425a` — Replace ShelfFactory with ShelfFacade pattern

**Files changed:** 9 files, +907 lines, -142 lines

**Domain (`Shelf.java`):**
- Reorganized class into clear sections: fields → constructor → business logic → getters/setters → object methods
- Fixed a null-safety bug in `isFull()` — previously would NPE if `bookIds` was null; now guards with `bookIds != null`
- Removed the no-arg default constructor (construction now goes through the canonical constructor)

**Application (`ShelfService.java`):**
- Added `createShelf(Long bookcaseId, int position, String shelfLabel, int bookCapacity)` implementation
- Validation guards: `bookCapacity <= 0`, blank/null `shelfLabel`, `position < 0`
- Entity instantiation uses an anonymous initializer block — functional, though flagged as a candidate for a builder pattern later
- Added `createShelf()` signature to `ShelfFacade` interface

**Application (`BookcaseService.java`):**
- Dropped `ShelfJpaRepository` and `ShelfFactory` dependencies
- Now delegates shelf creation to `ShelfFacade.createShelf()`
- Added validation for `bookCapacity`, `bookcaseEntity`, `position`, and `label` before delegating

**Infrastructure (`ShelfMapper.java`):**
- `toDomain()` updated to use the canonical constructor instead of chained setters — simpler and ensures all required fields are set at construction time
- Removed dead `updateEntity()` method (was a pass-through that did nothing)

**Tests:**
- `ShelfServiceTest`: 10 new test cases covering all validation paths and happy-path creation
- `BookcaseServiceTest`: comprehensive service-layer test coverage (450 lines)

---

### Commit 2: `7179599` — Complete removal of ShelfFactory.java

**Files changed:** 1 file, -18 lines

Deleted `ShelfFactory` from the infrastructure package. The class had been moved there in a prior step as a staging area before full removal. This commit finishes the job — no references remain.

**Before:**
```java
@Component
public class ShelfFactory {
    public ShelfEntity createEntity(Long bookCaseId, int shelfPosition, String shelfLabel, int bookCapacity) {
        ShelfEntity shelfEntity = new ShelfEntity();
        shelfEntity.setBookcaseId(bookCaseId);
        shelfEntity.setShelfLabel(shelfLabel);
        shelfEntity.setShelfPosition(shelfPosition);
        shelfEntity.setBookCapacity(bookCapacity);
        return shelfEntity;
    }
}
```

**After:** File deleted. `ShelfService.createShelf()` handles this inline via the facade.

---

### Commit 3: `b4cc44a` — Remove BookcaseFacade dependency from ShelfService

**Files changed:** 2 files, +3 lines, -12 lines

`ShelfService` had been injecting `BookcaseFacade` to resolve bookcase labels inside `toShelfOption()`. That cross-service dependency was unnecessary for the core responsibility of `ShelfService` and introduced coupling between two peers in the stacks bounded context.

**Removed:**
- `BookcaseFacade` constructor injection from `ShelfService`
- Bookcase label resolution from `toShelfOption()`
- `bookcaseLabel` field from `ShelfOptionResponse`

**ShelfOptionResponse before:**
```java
public record ShelfOptionResponse(
    Long shelfId,
    String shelfLabel,
    String bookcaseLabel,
    int bookCapacity,
    long bookCount,
    boolean hasSpace) {}
```

**ShelfOptionResponse after:**
```java
public record ShelfOptionResponse(
    Long shelfId, String shelfLabel, int bookCapacity, long bookCount, boolean hasSpace) {}
```

The bookcase label field is now the caller's responsibility to resolve if needed — `ShelfService` stays within its own lane.

---

### Commit 4: `8175982` — Simplify ShelfService and trim test cases

**Files changed:** 3 files, +48 lines, -149 lines

Follow-up cleanup after the `BookcaseFacade` removal. The test suite had been written to verify facade interactions that no longer existed, so stale assertions and mocks were pruned. The `ShelfService` implementation was also tightened — commented-out bookcase resolution code from commit 3 was fully removed.

**Net result:** `ShelfServiceTest` went from 286 lines down to ~130, focused entirely on validation behavior and repository interactions that are actually in scope.

---

## Architecture Before vs. After

### Before

```
BookcaseService
  ├── ShelfJpaRepository  (direct infra dependency)
  └── ShelfFactory        (creates ShelfEntity directly)

ShelfService
  └── BookcaseFacade      (cross-service dep for label resolution)
```

### After

```
BookcaseService
  └── ShelfFacade         (delegates through interface)

ShelfService              (no cross-service deps)
  └── ShelfJpaRepository  (owns its own persistence)
```

`ShelfFacade` is now the single entry point for shelf creation. `BookcaseService` expresses intent through the interface; `ShelfService` owns the implementation details.

---

## Files Modified

| File | Change |
|------|--------|
| `ShelfFactory.java` (domain) | Deleted |
| `ShelfFactory.java` (infrastructure) | Deleted |
| `ShelfFacade.java` | Added `createShelf()` signature |
| `ShelfService.java` | Added `createShelf()`, removed `BookcaseFacade`, removed `mapToDomain()` |
| `ShelfOptionResponse.java` | Removed `bookcaseLabel` field |
| `Shelf.java` | Reorganized, null-safety fix on `isFull()` |
| `ShelfMapper.java` | Constructor-based mapping, removed dead `updateEntity()` |
| `BookcaseService.java` | Delegates shelf creation via `ShelfFacade` |
| `ShelfServiceTest.java` | Rewritten to match current implementation |
| `BookcaseServiceTest.java` | Added (450 lines, comprehensive coverage) |

---

## Notes / Open Items

- The anonymous initializer block in `ShelfService.createShelf()` is functional but unconventional — a builder or static factory method on `ShelfEntity` would be cleaner long-term
- `toShelfOption()` now omits bookcase context — if consumers need that data, consider a richer projection or a separate query method rather than re-introducing the cross-service call
- The `// todo: remove shelfLabel` and `// todo: remove shelfDescription` comments in `Shelf.java` are still pending — those fields have survived a few refactors; worth scheduling the removal before they get more entrenched

---

**Author:** leodvincci
**Date:** Feb 18, 2026
**Branch:** `refactor/shelf-factory-to-facade` → `main`