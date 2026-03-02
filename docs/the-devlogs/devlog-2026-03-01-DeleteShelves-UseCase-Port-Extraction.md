# Devlog: Extract DeleteShelvesUseCasePort — Applying ISP to Cross-Module Shelf Deletion

**Date:** 2026-03-01
**Time:** 18:56 CST
**Branch:** `feat/shelf-placement-and-book-mapper-relocation`
**Range:** `origin/main..HEAD`
**Commits:**

| SHA       | Subject                                                                                       |
|-----------|-----------------------------------------------------------------------------------------------|
| `a9303da` | Add deleteShelvesUseCasePort and implement in DeleteShelvesUseCase                            |
| `0ec22ba` | Wire ShelfAccessPortAdapter to deleteShelvesUseCasePort                                       |
| `1af23cb` | Refactor deleteShelvesUseCasePort naming and update references in ShelfAccessPortAdapter and DeleteShelvesUseCase |

---

## What Problem Was I Solving?

The **bookcase module** needed to delete all shelves when a bookcase is deleted. Previously, this was done by depending on `ShelfCommandFacade` — a broad interface that bundles `deleteAllShelvesInBookcaseByBookcaseId`, `createShelfInBookcaseByBookcaseId`, and `placeBookOnShelf`. The `ShelfAccessPortAdapter` (in the bookcase infrastructure layer) only needed one of those methods for deletion, but was forced to depend on the entire facade.

This violated the **Interface Segregation Principle (ISP)**: the bookcase module's deletion path was coupled to an interface containing unrelated command methods. More importantly, it routed `delete` through `ShelfCommandFacade → ShelfService → DeleteShelvesUseCase`, adding an unnecessary indirection hop through the application service when the adapter could go straight to the use case.

## What Was the "Before" State?

```
BookcaseService (bookcase core)
  → ShelfAccessPort.deleteAllShelvesInBookcase(bookcaseId)       [outbound port]
    → ShelfAccessPortAdapter                                      [bookcase infra]
      → ShelfCommandFacade.deleteAllShelvesInBookcaseByBookcaseId [shelf inbound port — too broad]
        → ShelfService                                            [shelf application service]
          → DeleteShelvesUseCase.execute(bookcaseId)              [actual use case]
```

- `ShelfAccessPortAdapter` depended only on `ShelfCommandFacade`.
- The adapter's field was named `shelfFacade` — generic and ambiguous.
- No tests existed for `ShelfAccessPortAdapter`.
- `DeleteShelvesUseCase` was a concrete class with no inbound port interface.

## Key Outcomes

- **New inbound port:** `DeleteShelvesUseCasePort` — a single-method interface (`void execute(Long bookcaseId)`) in the shelf core's inbound port layer.
- **`DeleteShelvesUseCase` now implements `DeleteShelvesUseCasePort`** — making the use case injectable by interface rather than concrete class.
- **`ShelfAccessPortAdapter` now injects `DeleteShelvesUseCasePort` directly** — the deletion path bypasses `ShelfCommandFacade` and `ShelfService`, going straight to the use case.
- **`ShelfCommandFacade` is still used for `createShelf`** — only the deletion path was extracted.
- **New test class: `ShelfAccessPortAdapterTest`** — verifies delegation behavior for both `deleteAllShelvesInBookcase` and `createShelf`.

---

## Commit-by-Commit Analysis

### Commit 1: `a9303da` — Add deleteShelvesUseCasePort and implement in DeleteShelvesUseCase

**Intent:** Define the narrow inbound port interface and wire the existing use case to implement it.

**Files touched:**

| File | Reason |
|------|--------|
| `shelf/core/ports/inbound/DeleteShelvesUseCasePort.java` (new) | New single-method inbound port interface |
| `shelf/core/application/usecases/DeleteShelvesUseCase.java` | Now `implements DeleteShelvesUseCasePort` |
| `DeleteShelvesUseCaseTest.java` | Import update (the test was already testing `execute()`) |

**Key code changes:**

- `DeleteShelvesUseCasePort` is a minimal interface:
  ```java
  public interface DeleteShelvesUseCasePort {
    void execute(Long bookcaseId);
  }
  ```
- `DeleteShelvesUseCase` class declaration changed from:
  ```java
  public class DeleteShelvesUseCase {
  ```
  to:
  ```java
  public class DeleteShelvesUseCase implements DeleteShelvesUseCasePort {
  ```
- The `execute(Long bookcaseId)` method already existed with the correct signature — no body changes needed.

**Architecture notes:**

- The port lives in `shelf.core.ports.inbound` — the correct layer for inbound ports in the hexagonal layout.
- This follows the same pattern as `ShelfCommandFacade` and `ShelfQueryFacade` — interfaces in the core that infrastructure depends on.
- The original file was created with a lowercase name (`deleteShelvesUseCasePort.java`), corrected in commit 3.

**Risk / edge cases:**

- None — this is purely additive. Existing call paths through `ShelfCommandFacade → ShelfService → DeleteShelvesUseCase` are unaffected.

**Verification:**

```bash
mvn test -Dtest="DeleteShelvesUseCaseTest"
```

---

### Commit 2: `0ec22ba` — Wire ShelfAccessPortAdapter to deleteShelvesUseCasePort

**Intent:** Rewire the bookcase module's shelf deletion path to use the new narrow port instead of the broad facade.

**Files touched:**

| File | Reason |
|------|--------|
| `bookcase/infrastructure/adapter/outbound/ShelfAccessPortAdapter.java` | Inject `DeleteShelvesUseCasePort`, rewire `deleteAllShelvesInBookcase` |
| `bookcase/infrastructure/adapter/outbound/ShelfAccessPortAdapterTest.java` (new) | Test delegation for both methods |

**Key code changes in `ShelfAccessPortAdapter`:**

Before:
```java
private final ShelfCommandFacade shelfFacade;

public void deleteAllShelvesInBookcase(Long bookcaseId) {
    shelfFacade.deleteAllShelvesInBookcaseByBookcaseId(bookcaseId);
}
```

After:
```java
private final ShelfCommandFacade shelfCommandFacade;
private final DeleteShelvesUseCasePort deleteShelvesUseCasePort;

public void deleteAllShelvesInBookcase(Long bookcaseId) {
    deleteShelvesUseCasePort.execute(bookcaseId);
}
```

- The `createShelf` method still delegates to `shelfCommandFacade` — only the deletion path changed.
- Field renamed from `shelfFacade` → `shelfCommandFacade` for clarity (it's the command facade, not the query one).

**New test: `ShelfAccessPortAdapterTest`:**

- `deleteAllShelvesInBookcase_shouldDelegateToDeleteShelvesUseCasePort` — verifies `execute(bookcaseId)` is called, confirms `shelfCommandFacade` receives no interactions.
- `createShelf_shouldDelegateToShelfCommandFacade` — verifies correct delegation with all 4 parameters, confirms `deleteShelvesUseCasePort` receives no interactions.

**Architecture notes:**

- The dependency direction is now: `bookcase.infrastructure → shelf.core.ports.inbound.DeleteShelvesUseCasePort`. This is a clean cross-module dependency — infrastructure depends on another module's inbound port, never on its internals.
- `ShelfAccessPortAdapter` now has two dependencies instead of one. This is intentional — each dependency has a focused responsibility.

**Risk / edge cases:**

- Spring wiring: `DeleteShelvesUseCase` is annotated `@Service` and implements `DeleteShelvesUseCasePort`, so Spring will auto-wire it. If another bean also implemented this port, there would be an ambiguity error. Currently there's only one implementation.
- The `@Component("bookcaseShelfAccessPortAdapter")` qualifier remains — this is needed because there's also a `ShelfAccessPortAdapter` in the cataloging shelf infrastructure layer.

**Verification:**

```bash
mvn test -Dtest="ShelfAccessPortAdapterTest"
mvn test -Dtest="BookcaseServiceTest"
```

---

### Commit 3: `1af23cb` — Refactor deleteShelvesUseCasePort naming

**Intent:** Fix the lowercase filename and standardize naming conventions across all references.

**Files touched:**

| File | Reason |
|------|--------|
| `shelf/core/ports/inbound/DeleteShelvesUseCasePort.java` | Renamed from `deleteShelvesUseCasePort.java` (lowercase) |
| `shelf/core/application/usecases/DeleteShelvesUseCase.java` | Updated import |
| `bookcase/infrastructure/adapter/outbound/ShelfAccessPortAdapter.java` | Updated import |
| `bookcase/infrastructure/adapter/outbound/ShelfAccessPortAdapterTest.java` | Updated import |
| `DeleteShelvesUseCaseTest.java` | Removed now-unnecessary import |

**Architecture notes:**

- Java convention: interface files should be PascalCase. The initial commit created `deleteShelvesUseCasePort.java` which would compile but violates naming standards.

**Risk / edge cases:**

- On case-insensitive filesystems (macOS default), git may not detect the rename. The Linux host used here tracks it correctly.

---

## Architecture: End-to-End Flow (After)

### Deletion path (changed):

```
BookcaseService.deleteBookcase(bookcaseId)
  → ShelfAccessPort.deleteAllShelvesInBookcase(bookcaseId)       [bookcase outbound port]
    → ShelfAccessPortAdapter                                      [bookcase infrastructure]
      → DeleteShelvesUseCasePort.execute(bookcaseId)              [shelf inbound port — narrow]
        → DeleteShelvesUseCase                                    [shelf application use case]
          → ShelfDomainRepositoryPort.findByBookcaseId(...)       [shelf outbound port]
          → BookAccessPort.deleteBooksOnShelves(shelfIds)         [shelf outbound port → cataloging]
          → ShelfDomainRepositoryPort.deleteByBookcaseId(...)     [shelf outbound port]
```

### Creation path (unchanged):

```
BookcaseService.createBookcase(...)
  → ShelfAccessPort.createShelf(bookcaseId, position, label, capacity)
    → ShelfAccessPortAdapter
      → ShelfCommandFacade.createShelfInBookcaseByBookcaseId(...)
        → ShelfService → CreateShelfUseCase
```

### Why This Design Is Better

1. **One hop eliminated:** The deletion path no longer goes through `ShelfCommandFacade → ShelfService`. The adapter talks directly to the use case port.
2. **ISP compliance:** `ShelfAccessPortAdapter` depends on exactly what it needs — `DeleteShelvesUseCasePort` for deletion, `ShelfCommandFacade` for creation. Neither dependency carries unused methods for the respective call path.
3. **Testability:** Each delegation path can be verified in isolation. The new tests prove that `deleteAllShelvesInBookcase` touches only `deleteShelvesUseCasePort` and `createShelf` touches only `shelfCommandFacade`.
4. **Progressive facade decomposition:** This continues the trajectory started by splitting `ShelfFacade` into `ShelfQueryFacade` and `ShelfCommandFacade`. Eventually, each use case can have its own port, and the facades become optional convenience groupings.

### Dependencies Added

| From (consumer) | To (dependency) | Type |
|---|---|---|
| `ShelfAccessPortAdapter` | `DeleteShelvesUseCasePort` | New inbound port dependency |

### Dependencies Removed

| From (consumer) | To (dependency) | Notes |
|---|---|---|
| `ShelfAccessPortAdapter` (delete path) | `ShelfCommandFacade` | Still used for `createShelf`, but no longer for deletion |

### Import Layering Check

- `bookcase.infrastructure.adapter.outbound.ShelfAccessPortAdapter` imports `shelf.core.ports.inbound.DeleteShelvesUseCasePort` — **correct** (infra → inbound port of another module).
- `bookcase.infrastructure.adapter.outbound.ShelfAccessPortAdapter` imports `shelf.core.ports.inbound.ShelfCommandFacade` — **correct** (same pattern).
- No domain → infrastructure violations detected.

---

## Tests: What Exists and What Should Be Added

### Existing tests:

| Test class | Status |
|---|---|
| `ShelfAccessPortAdapterTest` (new) | 2 tests — covers both delegation paths |
| `DeleteShelvesUseCaseTest` | 3 tests — covers the execute flow (pre-existing) |
| `BookcaseServiceTest` | 10 tests — integration-level coverage of bookcase operations |
| `ShelfServiceTest` | 7 tests — covers `deleteAllShelvesInBookcaseByBookcaseId` delegation |

### Verification commands:

```bash
# Run all related tests
mvn test -Dtest="ShelfAccessPortAdapterTest,DeleteShelvesUseCaseTest,BookcaseServiceTest,ShelfServiceTest"

# Full suite
mvn test
```

### Suggested additional tests:

1. **`ShelfAccessPortAdapterTest` — constructor injection failure:** Verify that Spring context fails to load if `DeleteShelvesUseCasePort` has no implementation (integration test).
2. **`DeleteShelvesUseCaseTest` — verify `@Transactional` behavior:** Ensure that if `bookAccessPort.deleteBooksOnShelves` throws, `shelfDomainRepositoryPort.deleteByBookcaseId` is not called (rollback).
3. **`BookcaseServiceTest` — end-to-end deletion:** Mock the `ShelfAccessPort` and verify the full bookcase deletion flow reaches the use case port.

---

## Lessons From This Diff

1. **A broad facade is a stepping stone, not a destination.** `ShelfCommandFacade` was right when the module was young. As consumers differentiated, extracting per-use-case ports became the natural next step.
2. **Renaming a field from `shelfFacade` to `shelfCommandFacade` matters.** When a class has two injected dependencies, ambiguous names create confusion. Be explicit about which facade variant you're holding.
3. **Lowercase Java filenames compile but create friction.** Commit 3 exists solely to fix a naming mistake from commit 1 — catching this before push saves a cleanup commit.
4. **Test the negative path of mock interactions.** `verifyNoInteractions(shelfCommandFacade)` in the delete test is just as important as `verify(deleteShelvesUseCasePort).execute(...)`. It proves the paths are truly separated.
5. **Cross-module adapters are the right place to apply ISP.** The adapter sits at the boundary between bookcase and shelf modules — that's exactly where you want the narrowest possible interface contract.

---

## Follow-Ups

### Immediate (today)

- [ ] Verify all 195+ tests pass after the merge: `mvn test`
- [ ] Confirm `deleteShelvesUseCasePort.java` → `DeleteShelvesUseCasePort.java` rename is correctly tracked on the remote

### Short-term hardening (this week)

- [ ] Extract `CreateShelfUseCasePort` from `ShelfCommandFacade` following the same pattern — `ShelfAccessPortAdapter.createShelf` would then depend on `CreateShelfUseCasePort` instead of the facade
- [ ] Extract `PlaceBookOnShelfUseCasePort` — the third method on `ShelfCommandFacade`
- [ ] Once all three use case ports exist, evaluate whether `ShelfCommandFacade` can be deprecated or reduced to a convenience grouping

### Strategic refactors (later)

- [ ] Apply the same per-use-case port extraction to `ShelfQueryFacade` — split `findById`, `findAll`, `isFull`, `isEmpty` into individual query ports where consumers need only one
- [ ] Consider whether `ShelfService` (which currently delegates to use cases) still provides value once all callers go through use case ports directly
- [ ] Document the inbound port naming convention (`<Action>UseCasePort`) in an ADR so future use cases follow the same pattern
