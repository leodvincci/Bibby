# Devlog: Rename `findAllShelves` to `findShelvesByBookcaseId`

**Date:** 2026-02-21 (Saturday)  
**Branch:** `refactor/rename-shelf-query-method`  
**Range:** `b1cf057..7958027` (origin/main..HEAD at time of push; PR #291 now merged)  
**Commits:**

| SHA | Subject |
|-----|---------|
| `7958027` | Rename findAllShelves to findShelvesByBookcaseId |

---

## Motivation

### What problem was I solving?

The `ShelfFacade` interface had a method called `findAllShelves(Long bookCaseId)`. This name had three compounding problems:

1. **Semantic contradiction with `findAll()`.**  
   `ShelfFacade` exposes both `findAll()` (returns every shelf, no filter) and `findAllShelves(Long bookCaseId)` (returns shelves filtered by bookcase). The word "All" in the filtered version implies no filter, but the parameter *is* a filter. Scanning the interface, a reader has to inspect signatures to tell which one is truly "all." This is the kind of ambiguity that causes wrong-method bugs during rapid development.

2. **Violated the `findXByY` convention already in use.**  
   The same interface already declares `findShelfById(Long shelfId)` — a clear `findEntityByField` pattern. `findAllShelves(bookCaseId)` broke that pattern without reason, making the API less predictable.

3. **Inconsistent parameter casing.**  
   The parameter was named `bookCaseId` (capital `C`) in `ShelfFacade` and `ShelfService`, while every other method in the codebase uses `bookcaseId` (lowercase `c`). This is a minor nuisance in isolation, but it creates noise in IDE search/refactoring and signals sloppiness in the domain vocabulary.

### What was the "before" state?

```java
// ShelfFacade.java — the interface
List<Shelf> findAllShelves(Long bookCaseId);   // filtered query
List<Shelf> findAll();                          // unfiltered query
Optional<Shelf> findShelfById(Long shelfId);   // follows findXByY
```

The naming made it look like `findAllShelves` was the "real" findAll and `findAll()` was a convenience wrapper — the opposite of reality.

---

## Summary of Outcomes

- **Renamed** `findAllShelves(Long bookCaseId)` → `findShelvesByBookcaseId(Long bookcaseId)` across the entire call chain (interface → service → use case → controller → CLI → all tests).
- **Fixed** parameter casing `bookCaseId` → `bookcaseId` at every declaration site.
- **Zero behavioral change.** This is a pure rename refactor. 25 lines changed, 25 lines removed — perfectly balanced diff.
- **All existing tests updated** and verified passing.

---

## Commit Analysis

### `7958027` — Rename findAllShelves to findShelvesByBookcaseId

**Intent:** Align the filtered shelf-query method with the `findXByY` convention and eliminate the semantic clash with `findAll()`.

**Files touched (8 files, 25 insertions / 25 deletions):**

| File | Layer | What changed |
|------|-------|-------------|
| `ShelfFacade.java` | ports/inbound | Method signature renamed + param casing fixed |
| `QueryShelfUseCase.java` | application/usecases | Method renamed (delegates to `shelfDomainRepositoryPort.findByBookcaseId`) |
| `ShelfService.java` | application | `@Override` method renamed + param casing fixed; delegates to `QueryShelfUseCase` |
| `ShelfController.java` | api/web | Call site updated in `getShelfOptionsByBookcase()` |
| `PromptOptions.java` | cli/domain | 2 call sites updated: `bookShelfOptions()` and `bookCaseOptions()` |
| `ShelfServiceTest.java` | tests | 1 test method renamed + mock/verify calls updated |
| `QueryShelfUseCaseTest.java` | tests | 3 test methods renamed + mock/verify calls updated |
| `PromptOptionsTest.java` | tests | 7 mock stub call sites updated across 5 test methods |

**Key code changes:**

The rename flows through four architectural layers in a straight line:

```
ShelfFacade (port)
  └─ ShelfService (application, implements facade)
       └─ QueryShelfUseCase (application/usecases)
            └─ ShelfDomainRepository.findByBookcaseId() (outbound port — unchanged)
```

The repository method was *already* named `findByBookcaseId`. The rename brings the upper layers into alignment with what the repository has been saying all along. The call chain now reads naturally top-to-bottom:

```
ShelfController.getShelfOptionsByBookcase(bookcaseId)
  → shelfFacade.findShelvesByBookcaseId(bookcaseId)
    → queryShelfUseCase.findShelvesByBookcaseId(bookcaseId)
      → shelfDomainRepositoryPort.findByBookcaseId(bookcaseId)
```

Every layer uses the same vocabulary: **"by bookcase ID."**

**Architecture notes:**

- **No dependency direction changes.** The inbound port (`ShelfFacade`) still faces the web/CLI adapters. The outbound port (`ShelfDomainRepository`) still faces persistence. Nothing moved.
- **No layering violations introduced or removed.** The rename is purely within the existing boundaries.
- **Port contract tightened semantically.** The new name `findShelvesByBookcaseId` makes the contract self-documenting — a caller knows exactly what query predicate is in play without reading the implementation.

**Risk / edge cases:**

- **Risk: None.** This is a mechanical rename with no behavioral change. The method body is untouched.
- **Edge case: None.** The method still returns `List<Shelf>` (empty list for missing bookcase, populated list for found bookcase). No null handling or exception paths changed.
- **Migration risk: None.** This is an internal API (Java interface, not HTTP). No external consumers exist.

**Verification:**

```bash
mvn -q test -Dtest="ShelfServiceTest,QueryShelfUseCaseTest,PromptOptionsTest"
```

All three test classes pass. The tests exercise the full delegation chain from facade through use case to repository.

---

## Architecture Walkthrough

### The call flow (request → adapter → port → core → persistence → response)

**Web path** — `GET /api/v1/shelves/options/{bookcaseId}`:

1. `ShelfController.getShelfOptionsByBookcase(@PathVariable Long bookcaseId)` receives the request.
2. Calls `shelfFacade.findShelvesByBookcaseId(bookcaseId)` — the inbound port.
3. `ShelfService` (the facade implementation) delegates to `queryShelfUseCase.findShelvesByBookcaseId(bookcaseId)`.
4. `QueryShelfUseCase` calls `shelfDomainRepositoryPort.findByBookcaseId(bookcaseId)` — the outbound port.
5. The JPA adapter behind `ShelfDomainRepository` executes the query and returns `List<Shelf>`.
6. `ShelfController` maps each `Shelf` to `ShelfOptionResponse` via `shelfResponseMapper::toShelfOption` and returns the list.

**CLI path** — `PromptOptions.bookShelfOptions(Long bookcaseId)`:

1. Calls `shelfFacade.findShelvesByBookcaseId(bookcaseId)` directly (same inbound port).
2. Maps each `Shelf` to `ShelfDTO` inline for CLI menu rendering.

Both paths share the same port method. The rename makes both call sites equally readable.

### Why this design is better than the old one

The old name `findAllShelves` forced the reader to look at the parameter list to understand the query semantics. The new name `findShelvesByBookcaseId` encodes the predicate directly, matching the Spring Data convention (`findXByY`) that Java developers recognize instantly. It also eliminates the confusing pairing of `findAllShelves(id)` alongside `findAll()`.

---

## Dependency & Import Analysis

- **No dependencies added or removed.** No new imports in any file.
- **No layering violations.** All imports stay within their architectural layer:
  - `ShelfController` imports from `ports.inbound` (correct: adapter → port)
  - `PromptOptions` imports from `ports.inbound` (correct: adapter → port)
  - `ShelfService` imports from `usecases` and `ports.inbound` (correct: application layer)
  - `QueryShelfUseCase` imports from `ports.outbound` and `domain.model` (correct: use case → outbound port)

### Remaining inconsistencies spotted in the diff context

1. **`getShelfSummariesForBookcase(Long bookCaseId)`** in `ShelfFacade` (line 14) still uses `bookCaseId` (capital C) — the same casing bug this commit fixed for `findShelvesByBookcaseId`.
2. **`getShelfSummariesForBookcase`** doesn't follow the `findXByY` or even a consistent `getXForY` pattern relative to the other methods. It could become `findShelfSummariesByBookcaseId` for full alignment.
3. **`createShelf(Long bookcaseId, int position, String s, int bookCapacity)`** — the parameter `String s` is a poor name (should be `shelfLabel` based on usage in `ShelfService`).

---

## Testing & Verification

### Tests that changed

| Test Class | Tests Updated | What Changed |
|-----------|---------------|-------------|
| `ShelfServiceTest` | 1 (`findShelvesByBookcaseId_shouldDelegateToQueryUseCase`) | Method name in test + mock/verify calls |
| `QueryShelfUseCaseTest` | 3 (`_shouldReturnAllShelvesForBookcase`, `_shouldReturnEmptyListWhenBookcaseHasNoShelves`, `_shouldDelegateToRepository`) | Method name in tests + use case invocations |
| `PromptOptionsTest` | 0 renamed, but 7 mock stubs updated across 5 tests | `when(shelfFacade.findShelvesByBookcaseId(...))` |

### Run commands

```bash
# Run only affected tests (fast)
mvn -q test -Dtest="ShelfServiceTest,QueryShelfUseCaseTest,PromptOptionsTest"

# Full test suite (thorough)
mvn test

# Compile check only (fastest)
mvn -q -DskipTests compile
```

### No new tests needed

This is a zero-behavior-change rename. The existing test coverage is sufficient — every delegation path is already verified by the tests that were updated.

---

## What I Learned

1. **Method names compound over refactors.** `findAllShelves` was originally `getAllDTOShelves` (per earlier devlogs). Each rename improved it, but the "All" survived long past its usefulness. The lesson: when renaming, don't just fix the worst part — re-evaluate the *entire* name against current conventions.

2. **`findAll` + filter parameter is an API smell.** If a method takes a filter argument, it's not "findAll" — it's "findBy." This pattern shows up in Spring Data repositories constantly. Application layer methods should mirror that vocabulary, not invent their own.

3. **Parameter casing inconsistencies (`bookCaseId` vs `bookcaseId`) are a signal.** When you see inconsistent casing for the same concept, it usually means the name was copied at different times from different sources. One pass of normalization across the interface prevents grep-based confusion down the road.

4. **Rename refactors should be atomic commits.** Splitting an interface method rename across multiple commits would create intermediate states that don't compile. This commit touches 8 files but they all *must* change together — the interface, its implementation, all callers, and all tests. That's the correct granularity.

5. **The repository already had the right name.** `ShelfDomainRepository.findByBookcaseId()` was clean from the start. The rename simply propagated that clarity upward through the use case, service, and facade layers. When in doubt, look at what the persistence layer calls it — it's often the most honest name because Spring Data forces a convention.

---

## Next Steps

### Immediate follow-ups (today)

- [ ] **Fix `bookCaseId` param casing in `getShelfSummariesForBookcase`.** `ShelfFacade` line 14 still declares `Long bookCaseId` with capital C. Same bug, same fix.
- [ ] **Rename `String s` to `String shelfLabel`** in `ShelfFacade.createShelf()` — this is a trivially bad parameter name that slipped through.

### Short-term hardening (this week)

- [ ] **Consider renaming `getShelfSummariesForBookcase` → `findShelfSummariesByBookcaseId`.** This would complete the `findXByY` alignment across the entire `ShelfFacade` interface. The `get` prefix without a clear semantic distinction from `find` is confusing.
- [ ] **Audit other facade interfaces** (`BookFacade`, `BookcaseFacade`) for the same `findAll + filter` anti-pattern. Apply the `findXByY` convention repo-wide.
- [ ] **Consider creating an `ArchUnit` rule** to enforce method naming conventions on `*Facade` interfaces (e.g., filtered queries must match `find.*By.*Id`).

### Strategic refactors (later)

- [ ] **Evaluate `ShelfFacade` method count.** The interface has 6 methods mixing queries (`find*`, `get*`), commands (`create*`, `delete*`), and aggregations (`getShelfSummaries*`). As the shelf module grows, consider splitting into `ShelfQueryPort` and `ShelfCommandPort` (CQRS at the port level).
- [ ] **Inline `ShelfDTO` mapping in `PromptOptions`.** Both `bookShelfOptions()` and `bookCaseOptions()` do identical `Shelf → ShelfDTO` mapping inline. This should be extracted to a mapper or the facade should return the DTO directly for CLI consumers.
- [ ] **Revisit whether `QueryShelfUseCase` needs to be a separate class.** It's a thin delegation layer with no business logic. If `ShelfService` is already acting as a facade, the extra use case indirection may be architecture for architecture's sake (noted in a prior devlog as well).
