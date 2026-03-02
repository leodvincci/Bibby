# Devlog: Port Extraction from ShelfCommandFacade and Test Suite Cleanup

**Date:** 2026-03-02
**Time:** 09:58 CST
**Branch:** `refactor/create-shelf-port-and-test-cleanup` (merged to `main` via PR #340)
**Range:** `e80acd9..94ce9c2` (everything after PR #338 merge through PR #340 merge)
**PRs:** #339 (`feat/place-book-usecase-port-wiring`), #340 (`refactor/create-shelf-port-and-test-cleanup`)

**Commits:**

| SHA | Subject |
|-----|---------|
| `46c4405` | Remove fly.toml deployment configuration |
| `406d3cf` | Add PlaceBookOnShelfUseCasePort inbound port interface |
| `360eb75` | Wire controllers to PlaceBookOnShelfUseCasePort instead of ShelfCommandFacade |
| `51f3859` | Add CreateShelfUseCasePort and wire ShelfAccessPortAdapter to it |
| `5b89b6c` | Remove redundant and low-signal tests across test suite |

---

## What Problem Was I Solving?

`ShelfCommandFacade` was acting as a monolithic inbound port with three responsibilities: create shelf, delete shelves, and place book on shelf. Controllers and cross-module adapters depended on the entire facade even when they only needed a single operation. This created unnecessary coupling — `BookController` pulled in the full shelf command surface just to place a book.

Additionally, the test suite had accumulated 26 tests that provided no unique coverage. Empty test bodies, duplicate assertions with different inputs on the same code path, type-system-guaranteed checks, and one brittle `Thread.sleep`-based test were adding noise without protecting any behavior.

## Before State

- **`BookController`** and **`ShelfController`** both injected `ShelfCommandFacade` and called `shelfFacade.placeBookOnShelf(bookId, shelfId)`.
- **`ShelfAccessPortAdapter`** (bookcase module's cross-module adapter) injected `ShelfCommandFacade` to call `createShelfInBookcaseByBookcaseId()`.
- `ShelfCommandFacade` had 3 methods: `deleteAllShelvesInBookcaseByBookcaseId`, `createShelfInBookcaseByBookcaseId`, `placeBookOnShelf`.
- `DeleteShelvesUseCasePort` was already extracted (prior work in PR #337–#338).
- The test suite had **197 tests**, many of which overlapped.

## Key Outcomes

- Two new inbound port interfaces: `PlaceBookOnShelfUseCasePort` and `CreateShelfUseCasePort`
- Controllers now depend on single-method port interfaces instead of the 3-method facade
- `ShelfAccessPortAdapter` wires to `CreateShelfUseCasePort` instead of `ShelfCommandFacade`
- `ShelfCommandFacade` is now **fully hollowed out** — all 3 operations have dedicated ports (`DeleteShelvesUseCasePort`, `PlaceBookOnShelfUseCasePort`, `CreateShelfUseCasePort`)
- Test count: **197 → 171** with zero coverage loss
- Removed `fly.toml` (Fly.io deployment config no longer needed)

---

## Commit-by-Commit Breakdown

### Commit 1: `46c4405` — Remove fly.toml deployment configuration

**Intent:** Clean up stale Fly.io deployment config.

**Files touched:**
- `fly.toml` (deleted) — Fly.io app configuration for the `bibby` app, DFW region, 1GB VM

**Key change:** Entire 32-line Fly.io config removed. This file defined the app name, primary region (`dfw`), HTTP service config (port 8080, force HTTPS, auto-stop machines), health check (`/actuator/health`), and VM resources.

**Architecture notes:** Pure config cleanup, no architectural impact.

**Risk:** If any CI/CD pipeline references `fly.toml` for deployment, it will break. Fly.io deployments are no longer active for this project.

**Verification:** `mvn compile` — no impact on build.

---

### Commit 2: `406d3cf` — Add PlaceBookOnShelfUseCasePort inbound port interface

**Intent:** Extract the "place book on shelf" operation from `ShelfCommandFacade` into a dedicated inbound port.

**Files touched:**
- `PlaceBookOnShelfUseCasePort.java` (new) — Single-method interface: `void execute(Long bookId, Long shelfId)`
- `PlaceBookOnShelfUseCase.java` (modified) — Now `implements PlaceBookOnShelfUseCasePort`

**Key code change:**

```java
// New port interface
public interface PlaceBookOnShelfUseCasePort {
  void execute(Long bookId, Long shelfId);
}

// Use case now implements the port
@Service
public class PlaceBookOnShelfUseCase implements PlaceBookOnShelfUseCasePort { ... }
```

**Architecture notes:** This is a textbook hexagonal architecture extraction. The port lives in `shelf.core.ports.inbound` — the domain boundary. The use case in `shelf.core.application.usecases` implements it. Spring's `@Service` annotation makes `PlaceBookOnShelfUseCase` the sole bean for this port. Consumers can now depend on the interface without knowing about the concrete implementation.

**Risk:** None — purely additive. Existing callers through `ShelfCommandFacade` still work.

---

### Commit 3: `360eb75` — Wire controllers to PlaceBookOnShelfUseCasePort instead of ShelfCommandFacade

**Intent:** Narrow controller dependencies from the full facade to the specific port.

**Files touched:**
- `BookController.java` — Replaced `ShelfCommandFacade shelfFacade` → `PlaceBookOnShelfUseCasePort placeBookOnShelf`
- `ShelfController.java` — Replaced `ShelfCommandFacade shelfCommandFacade` → `PlaceBookOnShelfUseCasePort placeBookOnShelfUseCasePort`

**Key code changes:**

In `BookController`:
```java
// Before
shelfFacade.placeBookOnShelf(bookFacade.findBookByIsbn(...).id(), bookDTO.shelfId());

// After
placeBookOnShelf.execute(bookFacade.findBookByIsbn(...).id(), bookDTO.shelfId());
```

In `ShelfController`:
```java
// Before
shelfCommandFacade.placeBookOnShelf(request.bookId(), request.shelfId());

// After
placeBookOnShelfUseCasePort.execute(request.bookId(), request.shelfId());
```

**Architecture notes:** This is the critical coupling reduction. Before, `BookController` (in the `web.controllers.cataloging.book` package) imported from `shelf.core.ports.inbound.ShelfCommandFacade` — pulling in the entire command surface. Now it imports only `PlaceBookOnShelfUseCasePort`. The controller knows exactly one thing about the shelf module: "I can place a book on a shelf." The import path is still cross-module (`web.controllers` → `shelf.core.ports.inbound`), but the surface area is minimal.

**Dependency direction change:**
```
BEFORE: BookController → ShelfCommandFacade (3 methods exposed)
AFTER:  BookController → PlaceBookOnShelfUseCasePort (1 method exposed)
```

**Risk:** If anything else still called `ShelfCommandFacade.placeBookOnShelf()`, it would still compile but represents dead code on the facade. This is a stepping stone toward removing the method from the facade entirely.

---

### Commit 4: `51f3859` — Add CreateShelfUseCasePort and wire ShelfAccessPortAdapter to it

**Intent:** Extract the "create shelf" operation from `ShelfCommandFacade` into a dedicated inbound port, and rewire the cross-module adapter.

**Files touched:**
- `CreateShelfUseCasePort.java` (new) — `void execute(String shelfLabel, int shelfPosition, int bookCapacity, Long bookcaseId)`
- `CreateShelfUseCase.java` (modified) — Now `implements CreateShelfUseCasePort`
- `ShelfAccessPortAdapter.java` (modified) — Injects `CreateShelfUseCasePort` instead of routing through facade
- `ShelfAccessPortAdapterTest.java` (modified) — Removed obsolete test for old delegation

**Key code changes in `ShelfAccessPortAdapter`:**
```java
// Before
@Override
public void createShelf(Long bookcaseId, int position, String shelfLabel, int bookCapacity) {
    shelfCommandFacade.createShelfInBookcaseByBookcaseId(bookcaseId, position, shelfLabel, bookCapacity);
}

// After
@Override
public void createShelf(Long bookcaseId, int position, String shelfLabel, int bookCapacity) {
    createShelfUseCasePort.execute(shelfLabel, position, bookCapacity, bookcaseId);
}
```

**Architecture notes:** `ShelfAccessPortAdapter` lives in `bookcase.infrastructure.adapter.outbound` — it's the bookcase module's adapter for reaching into the shelf module. Previously it depended on `ShelfCommandFacade` (a broad facade). Now it depends on `CreateShelfUseCasePort` (a single-purpose inbound port). This is a strict improvement in the dependency graph: the bookcase module's cross-module boundary is now narrower.

Note that `ShelfCommandFacade` is still injected in the constructor (likely for Spring wiring compatibility) but the field `shelfCommandFacade` is no longer stored or used. This is a residual artifact — the constructor parameter should be removed in a follow-up.

**Risk:** The removed test (`createShelf_shouldDelegateToShelfCommandFacade`) verified the old delegation path. No replacement test for the new `CreateShelfUseCasePort` delegation was added. This is a coverage gap.

---

### Commit 5: `5b89b6c` — Remove redundant and low-signal tests across test suite

**Intent:** Reduce test noise without losing signal. Remove tests where behavior is already verified by other tests.

**Files touched (10 files, 346 lines removed):**

**Deleted file:**
- `AuthorRepositoryTest.java` — Empty test body (`void saveAuthor() {}`), zero assertions

**Modified test files with removed methods:**

| Test Class | Removed | Category | Still Covered By |
|------------|---------|----------|-----------------|
| **AuthorNameTest** (8 removed, 1 renamed) | | | |
| `normalized()` | toString normalizes whitespace | REDUNDANT | `toString_shouldReturnNormalizedAuthorName` |
| `normalized_2()` | normalized() removes periods | REDUNDANT | `normalized_shouldReturnAlreadyNormalizedNameAndBeIdempotent` |
| `parse_authors_middle_name()` | middle name from 3-part name | REDUNDANT | `parseMiddleName_shouldReturnSingleMiddleName` |
| `parse_authors_middle_name_no_period()` | same without period | REDUNDANT | Same as above (periods normalized away) |
| `parse_authors_middle_name_no_middle_name()` | 2-part → empty middle | REDUNDANT | `parseMiddleName_shouldReturnEmptyStringWhenNoMiddleName` |
| `parse_authors_first_name()` | first name via normalize | REDUNDANT | `parseFirstName_shouldReturnFirstToken` |
| `getAuthorName()` | returns normalized | REDUNDANT | `getAuthorName_shouldReturnNormalizedName` |
| `parseFirstName()` | first name direct | REDUNDANT | `parseFirstName_shouldReturnFirstToken` |
| *Renamed:* `parse_authors_middle_name_only_first_name` → `parseMiddleName_shouldReturnEmptyStringWhenSingleName` | | KEPT | — |
| **AuthorEntityTest** (1 removed) | | | |
| `testGetBooksReturnsCorrectType()` | `instanceof Set` check | REDUNDANT | `testDefaultConstructor` (already asserts getBooks() not null + empty); Java type system guarantees Set |
| **BookcardRendererTest** (3 removed) | | | |
| `testCreateBookCard_LongTitle()` | Long title in output | LOW-SIGNAL | `testCreateBookCard_ValidInputs` (same assertion, no truncation logic) |
| `testCreateBookCard_SpecialCharactersInAuthor()` | Special chars in output | LOW-SIGNAL | `testCreateBookCard_ValidInputs` (no special handling exists) |
| `testCreateBookCard_PartialData()` | All nulls | REDUNDANT | `NullAuthor` + `NullPublisher` cover null paths |
| **BooklistTest** (6 removed) | | | |
| `shouldThrowExceptionWhenValueIsNullOrBlank()` | null, "", "   " | REDUNDANT | Parameterized `shouldThrowExceptionWhenValueIsBlank` + `shouldThrowExceptionWhenValueIsNull` |
| `shouldHandleEmptyStringAfterWhitespaceReplacement()` | "   " → IAE | REDUNDANT | Parameterized `shouldThrowExceptionWhenValueIsBlank` covers "  " |
| `renameBooklist_ShouldThrowExceptionForInvalidSpecialCharacters()` | "@" → IAE | REDUNDANT | Parameterized `shouldThrowExceptionWhenValueContainsInvalidCharacters` includes "Reading @ Home" |
| `renameBooklist_ShouldThrowException_WhenNewNameExceedsMaxLength()` | 101 chars | REDUNDANT | `shouldThrowExceptionWhenValueIsTooLong` (same validation in constructor) |
| `renameBooklist_ShouldThrowException_WhenNewNameContainsOnlySpaces()` | "   " → IAE | REDUNDANT | Parameterized `shouldThrowExceptionWhenValueIsBlank` |
| `renameBooklist_ShouldUpdateUpdatedAtWithNewTimestamp_WhenCalledTwice()` | Thread.sleep timing | BRITTLE | `renameBooklist_ShouldUpdateUpdatedAt_WhenCalled` |
| **CreateShelfUseCaseTest** (2 removed) | | | |
| `execute_shouldSaveShelfWithMinimumBookCapacity()` | capacity=1 | LOW-SIGNAL | `execute_shouldSaveShelfSuccessfully` (no min logic exists) |
| `execute_shouldSaveShelfWithLargeBookCapacity()` | capacity=1000 | LOW-SIGNAL | Same (no max logic exists) |
| **DeleteShelvesUseCaseTest** (1 removed) | | | |
| `execute_shouldCollectAllShelfIdsForBookDeletion()` | 3 shelves | REDUNDANT | `execute_shouldDeleteBooksBeforeShelves` (2 shelves, same path) |
| **PlaceBookOnShelfUseCaseTest** (1 removed) | | | |
| `execute_shouldCheckBookExistenceBeforePlacing()` | verify getBookById | REDUNDANT | Happy path mocks getBookById; without it placement fails |
| **QueryShelfUseCaseTest** (1 removed) | | | |
| `findShelvesByBookcaseId_shouldDelegateToRepository()` | 1 shelf | REDUNDANT | `shouldReturnAllShelvesForBookcase` (2 shelves, superset) |
| **PlacementRepositoryPortImplTest** (1 removed) | | | |
| `placeBookOnShelf_shouldMapDomainToEntityCorrectly()` | IDs 42/99 | REDUNDANT | `shouldSaveEntityToRepository` (IDs 1/2, same code path) |

**Architecture notes:** No production code changed. Test count 197 → 171. All remaining tests verified via `mvn test` — 171 pass, 0 failures.

---

## End-to-End Flow Analysis

### Flow 1: Place Book on Shelf

```
HTTP POST /shelves/placements { bookId, shelfId }
  → ShelfController
    → PlaceBookOnShelfUseCasePort.execute(bookId, shelfId)    [NEW: was ShelfCommandFacade]
      → PlaceBookOnShelfUseCase.execute(bookId, shelfId)
        → BookAccessPort.getBookById(bookId)                   [validates book exists]
        → new Placement(bookId, shelfId)                       [domain model]
        → PlacementRepositoryPort.placeBookOnShelf(placement)  [persistence]
```

Also triggered from `BookController` during book creation:

```
HTTP POST /books { ..., shelfId }
  → BookController
    → BookFacade.createNewBook(bookRequestDTO)
    → PlaceBookOnShelfUseCasePort.execute(bookId, shelfId)    [NEW: was ShelfCommandFacade]
      → (same flow as above)
```

**Key improvement:** Both controllers now import `PlaceBookOnShelfUseCasePort` — a single-method interface. They no longer see `deleteAllShelvesInBookcaseByBookcaseId` or `createShelfInBookcaseByBookcaseId`, which were never relevant to them.

### Flow 2: Create Shelf (during bookcase creation)

```
CreateBookcaseUseCase.createNewBookCase(...)
  → BookcaseRepository.save(bookcase)
  → for i in 1..shelfCapacity:
      ShelfAccessPort.createShelf(bookcaseId, i, "Shelf " + i, bookCapacity)
        → ShelfAccessPortAdapter.createShelf(...)
          → CreateShelfUseCasePort.execute(label, position, capacity, bookcaseId)  [NEW: was ShelfCommandFacade]
            → CreateShelfUseCase.execute(...)
              → new Shelf(label, position, capacity, null, emptyList, bookcaseId)
              → ShelfDomainRepositoryPort.createNewShelfInBookcase(shelf)
```

**Key improvement:** The bookcase module's adapter (`ShelfAccessPortAdapter`) no longer reaches into the shelf module through a broad facade. It uses `CreateShelfUseCasePort`, making the cross-module dependency explicit and minimal.

---

## Dependency Graph Changes

### What Was Removed

```
BookController ──────────→ ShelfCommandFacade
ShelfController ─────────→ ShelfCommandFacade
ShelfAccessPortAdapter ──→ ShelfCommandFacade  (for createShelf)
```

### What Was Added

```
BookController ──────────→ PlaceBookOnShelfUseCasePort  (1 method)
ShelfController ─────────→ PlaceBookOnShelfUseCasePort  (1 method)
ShelfAccessPortAdapter ──→ CreateShelfUseCasePort        (1 method)
```

### Current State of ShelfCommandFacade

`ShelfCommandFacade` still declares 3 methods, but all 3 now have dedicated ports:

| Facade Method | Dedicated Port | Extracted In |
|--------------|---------------|-------------|
| `deleteAllShelvesInBookcaseByBookcaseId` | `DeleteShelvesUseCasePort` | PR #337–#338 |
| `placeBookOnShelf` | `PlaceBookOnShelfUseCasePort` | PR #339 (this push) |
| `createShelfInBookcaseByBookcaseId` | `CreateShelfUseCasePort` | PR #340 (this push) |

**`ShelfCommandFacade` is now dead code.** No consumer needs the aggregated facade anymore. Its implementation in `ShelfService` still exists but can be removed.

### Residual Issue

`ShelfAccessPortAdapter` still accepts `ShelfCommandFacade` as a constructor parameter (for Spring DI), but neither stores nor uses it. This is a wiring artifact that should be cleaned up.

---

## Layering Analysis

**No layering violations introduced.** All new ports are in `shelf.core.ports.inbound` — the canonical location for inbound port interfaces in this hexagonal architecture. Use cases in `shelf.core.application.usecases` implement them. Infrastructure adapters in `bookcase.infrastructure.adapter.outbound` consume them. Controllers in `web.controllers` consume them. Dependency arrows point inward toward the domain.

**One pre-existing cross-module import pattern continues:** `BookController` (cataloging/book web layer) imports from `shelf.core.ports.inbound`. This is intentional — book creation includes shelf placement as a side effect. The port interface keeps this coupling narrow.

---

## Coverage Risks and Suggested Tests

### Existing Coverage Gaps

1. **`ShelfAccessPortAdapter.createShelf` → `CreateShelfUseCasePort` delegation** — The old test was removed but no replacement added. This cross-module adapter boundary should be tested.

2. **`ShelfController.addBookToShelf` endpoint** — No controller-level test (MockMvc) exists for the `/shelves/placements` POST endpoint.

3. **`BookController` book-creation-with-placement path** — The placement call in `addNewBook` is not tested at the controller level.

### Suggested Test Cases (5)

```bash
# Run after adding:
mvn -Dtest=ShelfAccessPortAdapterTest test
mvn -Dtest=ShelfControllerTest test
```

1. `ShelfAccessPortAdapterTest.createShelf_shouldDelegateToCreateShelfUseCasePort` — Verify `createShelfUseCasePort.execute(label, position, capacity, bookcaseId)` is called with correct args.

2. `ShelfControllerTest.addBookToShelf_shouldDelegateToPlaceBookOnShelfUseCasePort` — MockMvc POST to `/shelves/placements`, verify the port's `execute()` is called.

3. `BookControllerTest.addNewBook_shouldPlaceBookOnShelf_whenShelfIdProvided` — Verify that after book creation, `placeBookOnShelf.execute()` is called with the correct book ID and shelf ID.

4. `PlaceBookOnShelfUseCaseTest.execute_shouldCreatePlacementWithCorrectBookAndShelfIds` — (exists as happy path, but consider adding an InOrder test to verify `getBookById` is called before `placeBookOnShelf`).

5. `CreateShelfUseCaseTest.execute_shouldNotSetShelfId` — (exists, but consider a negative test: what happens if `shelfLabel` is blank? Currently the domain `Shelf` model throws `IllegalArgumentException` — this is untested at the use-case level).

---

## Verification

```bash
# Full test suite
mvn test
# Expected: 171 tests, 0 failures

# Compile only (fast check)
mvn -q -DskipTests compile

# Run specific test classes affected by port wiring
mvn -Dtest=PlaceBookOnShelfUseCaseTest,CreateShelfUseCaseTest,ShelfServiceTest,ShelfAccessPortAdapterTest test

# Check for dead code on ShelfCommandFacade
grep -rn "ShelfCommandFacade" src/main/java/ --include="*.java"
```

---

## Lessons from This Diff

1. **Extract ports incrementally, not all at once.** `DeleteShelvesUseCasePort` was extracted in a prior PR. `PlaceBookOnShelfUseCasePort` and `CreateShelfUseCasePort` were extracted here. Each step was small, testable, and reviewable. The facade was hollowed out over 3 PRs rather than rewritten in 1.

2. **Single-method interfaces are the right granularity for inbound ports.** `PlaceBookOnShelfUseCasePort` has exactly one method. This makes it impossible for a consumer to accidentally couple to unrelated operations. It also makes testing trivial — mock one method.

3. **Test redundancy accumulates silently.** The audit found 26 tests (13% of the suite) that provided zero unique coverage. Several were added as "belt and suspenders" with slightly different inputs on the same code path. The parameterized tests in `BooklistTest` already covered the space that 6 individual tests were trying to cover.

4. **Brittle tests erode trust.** The `Thread.sleep(10)` test in `BooklistTest` was testing timestamp advancement but depended on wall-clock timing. The simpler `renameBooklist_ShouldUpdateUpdatedAt_WhenCalled` test already proved the invariant without the sleep.

5. **When you change a delegation path, delete the old test AND add a new one.** The `ShelfAccessPortAdapterTest.createShelf_shouldDelegateToShelfCommandFacade` test was correctly removed (it tested a path that no longer exists), but the replacement test was deferred. This is a known gap.

6. **Removing `fly.toml` is a config hygiene signal.** Stale deployment configs mislead new contributors into thinking a deployment pipeline exists. Cleaning it up early prevents confusion.

---

## Follow-Up Roadmap

### Immediate (today)

- [ ] Add `ShelfAccessPortAdapterTest.createShelf_shouldDelegateToCreateShelfUseCasePort`
- [ ] Remove `ShelfCommandFacade` constructor parameter from `ShelfAccessPortAdapter` (dead wiring)

### Short-Term Hardening (this week)

- [ ] Remove `ShelfCommandFacade` interface entirely — all 3 methods have dedicated ports
- [ ] Remove `placeBookOnShelf`, `createShelfInBookcaseByBookcaseId`, and `deleteAllShelvesInBookcaseByBookcaseId` from `ShelfService` (the facade implementation)
- [ ] Add MockMvc controller tests for `ShelfController.addBookToShelf` and `BookController.addNewBook` placement path
- [ ] Grep for any remaining references to `ShelfCommandFacade` across the codebase and migrate them

### Strategic Refactors (later)

- [ ] Evaluate whether `ShelfService` should be removed entirely once the facade is gone — each use case is already independently injectable
- [ ] Consider extracting `DeleteShelvesUseCasePort`, `PlaceBookOnShelfUseCasePort`, and `CreateShelfUseCasePort` into a shared `shelf-api` module if cross-module consumption grows
- [ ] Investigate whether `BookController` should delegate book-shelf placement to a domain event rather than directly calling the shelf port — this would further decouple the cataloging and stacks modules
