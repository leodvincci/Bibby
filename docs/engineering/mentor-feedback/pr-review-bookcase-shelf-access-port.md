# PR Review: Decouple Bookcase Core from Shelf Module via ShelfAccessPort

Date: February 23, 2026 (America/Chicago)

Branch: `refactor/bookcase-shelf-access-port`

Range: `4797c13..HEAD` (branch diverged from local `main`; already merged to `origin/main` via PR #314)

Commits:
- `cb1f076` Decouple bookcase module from ShelfFacade via ShelfAccessPort
- `3bc7829` Add logging to shelf creation and deletion operations
- `d629633` Introduce ADR 0001 for ShelfAccessPort Implementation
- `df578de` Fix conflicting bean name for bookcase ShelfAccessPortAdapter
- `ce95354` Enhance ShelfAccessPortAdapter implementation and address bean name collision

---

## 0) Executive Summary

**Ship or No-Ship:** ⚠️ Ship with fixes

**Top 3 risks:**
1. `createNewBookCase()` has no `@Transactional` — saves a bookcase then loops N shelf-creation calls; any mid-loop failure leaves an orphaned bookcase with partial shelves.
2. `BookcaseService` uses `jakarta.transaction.Transactional` while the downstream `DeleteShelvesUseCase` uses `org.springframework.transaction.annotation.Transactional` — functionally compatible today, but inconsistent and fragile for future rollback-attribute changes.
3. Core layer (`BookcaseService`, `BookcaseFacade`, `BookcaseRepository`) imports `infrastructure.entity.BookcaseEntity` — the very dependency-direction violation this PR set out to fix persists in the other direction (entity leaking upward).

**Top 3 recommended fixes:**
1. Add `@Transactional` to `createNewBookCase()` to make bookcase + shelf creation atomic.
2. Standardize on one `@Transactional` annotation (prefer Spring's `org.springframework.transaction.annotation.Transactional`) across the bookcase module.
3. Rename `ShelfAccessPort.createShelf`'s third parameter from `String string` to `String shelfLabel`.

**Test confidence: Medium** — Unit tests cover the happy path and mock boundaries correctly for the refactor, but miss operation-order verification on delete, and no integration test validates that the `@Transactional` boundary actually works end-to-end.

---

## 1) What Changed (diff-anchored)

- **New outbound port:** `ShelfAccessPort` added in `bookcase.core.ports.outbound` with two methods: `createShelf` and `deleteAllShelvesInBookcase`. Owned by the bookcase module.
- **New adapter:** `ShelfAccessPortAdapter` in `bookcase.infrastructure.adapter.outbound` delegates to `ShelfFacade`. Explicit bean name `"bookcaseShelfAccessPortAdapter"` avoids collision with the cataloging module's identically-named class.
- **`BookcaseService` rewired:** Depends on `ShelfAccessPort` instead of `ShelfFacade`. `deleteBookcase()` now orchestrates shelf deletion *before* bookcase deletion and is marked `@Transactional`.
- **Controller simplified:** `BookCaseController` constructor reduced to a single `BookcaseFacade` dependency. Removed `ShelfFacade` and `BookcaseService` (concrete) injections. All endpoints now route through the facade.
- **Logging added** to `ShelfService.deleteAllShelvesInBookcase()` and `CreateShelfUseCase.execute()`.
- **Tests updated:** `BookcaseServiceTest` now mocks `ShelfAccessPort` instead of `ShelfFacade`. `deleteBookcase` test added verification of `shelfAccessPort.deleteAllShelvesInBookcase`.
- **ADR added:** `adr-0001-bookcase-shelf-access-port.md` documents the decision, alternatives, and consequences in detail.
- **Public surface area change:** `BookCaseController` constructor signature changed (3 args → 1). No endpoint paths, HTTP verbs, or response shapes changed.

---

## 2) Blockers (must fix before merge)

*No true merge-blockers.* The PR is already merged. The items below under High-Risk are "fix soon."

---

## 3) High-Risk Issues (should fix soon)

### 3.1) `createNewBookCase()` is not `@Transactional` — partial-state hazard

**Finding:** The method saves a `BookcaseEntity`, then loops `shelfCapacity` times calling `shelfAccessPort.createShelf()`. Each shelf creation is an independent persistence call (no `@Transactional` on `CreateShelfUseCase.execute()`). If any shelf creation fails, the bookcase exists with incomplete shelves and no rollback.

**Evidence:** `BookcaseService.java` lines 31–63 — `createNewBookCase()` calls `bookcaseRepository.save()` then loops `addShelf()` with no transaction boundary.

**Why it matters:** Partial failure leaves an inconsistent bookcase — it claims N shelves in its capacity field but only M < N shelves exist. Downstream queries will produce confusing results.

**Concrete fix:** Add `@Transactional` to `createNewBookCase()`. Since this is a write-heavy orchestration method, it should own the transaction boundary the same way `deleteBookcase()` does.

**Verification:** Write a test where `shelfAccessPort.createShelf()` throws on the 3rd invocation. Assert the bookcase entity was *not* persisted (requires an integration test with a real or embedded database, since Mockito mocks won't exercise rollback behavior).

---

### 3.2) Mixed `@Transactional` annotations: Jakarta vs Spring

**Finding:** `BookcaseService` imports `jakarta.transaction.Transactional` (line 9). `DeleteShelvesUseCase` uses `org.springframework.transaction.annotation.Transactional`. Spring supports both, but they have different attribute sets (`rollbackOn` vs `rollbackFor`). Mixing makes it easy for a developer to set a `rollbackFor` on the Jakarta annotation and get silently ignored.

**Evidence:**
- `BookcaseService.java` line 9: `import jakarta.transaction.Transactional;`
- `DeleteShelvesUseCase.java` line 10: `import org.springframework.transaction.annotation.Transactional;`

**Why it matters:** Future risk — inconsistent conventions lead to incorrect rollback configuration.

**Concrete fix:** Standardize on `org.springframework.transaction.annotation.Transactional` in `BookcaseService`. It's more expressive (`propagation`, `isolation`, `rollbackFor`) and consistent with the rest of the codebase.

**Verification:** Global search for `jakarta.transaction.Transactional` and replace with Spring's.

---

### 3.3) `ShelfAccessPort.createShelf` parameter named `string`

**Finding:** The third parameter of `createShelf` in the port interface is `String string`.

**Evidence:**
- `ShelfAccessPort.java` line 6: `void createShelf(Long bookcaseId, int position, String string, int bookCapacity);`
- `ShelfAccessPortAdapter.java` line 21: `public void createShelf(Long bookcaseId, int position, String string, int bookCapacity) {`

**Why it matters:** The port is a public contract. `String string` communicates zero intent. Callers must read the implementation to understand what to pass. The actual value is `"Shelf " + label` — this is a shelf label/name.

**Concrete fix:** Rename to `String shelfLabel` in both the port and adapter. Also rename the corresponding parameter `String s` in `ShelfFacade.createShelf` for consistency.

**Verification:** Compile.

---

### 3.4) `deleteBookcase` test does not verify operation order

**Finding:** The test verifies that both `deleteAllShelvesInBookcase` and `deleteById` are called, but does not assert the order. The entire point of the fix (ADR section: "Delete order was incorrect") was to ensure shelves are deleted *before* the bookcase entity.

**Evidence:** `BookcaseServiceTest.java` lines 335–343 — uses two independent `verify()` calls with no `InOrder`.

**Why it matters:** If someone refactors the service and flips the order, this test will still pass. The test encodes the *what* but not the *contract* (order matters for data integrity).

**Concrete fix:**

```java
InOrder inOrder = inOrder(shelfAccessPort, bookcaseRepository);
inOrder.verify(shelfAccessPort).deleteAllShelvesInBookcase(bookcaseId);
inOrder.verify(bookcaseRepository).deleteById(bookcaseId);
```

**Verification:** `mvn -Dtest=BookcaseServiceTest test`

---

## 4) Architecture and Boundary Audit (DDD + Hex)

### Dependency direction wins

- **Controller → Facade only.** `BookCaseController` now has a single constructor dependency on `BookcaseFacade`. This is a clean improvement — no more concrete `BookcaseService` injection, no more `ShelfFacade` import from another module.
- **Core → Own port only.** `BookcaseService` depends on `ShelfAccessPort` (its own outbound port) instead of `ShelfFacade` (another module's inbound port). The dependency arrow is correct: Core defines the port, infrastructure adapts it.
- **Adapter isolates cross-module coupling.** `ShelfAccessPortAdapter` is the only class in the bookcase module that imports from the shelf module.
- **Pattern consistency.** The bookcase module now mirrors the cataloging/book module's approach to shelf access.

### Violations (pre-existing, but relevant)

**Core imports infrastructure entity.** Three files in `bookcase.core` import `BookcaseEntity` from `bookcase.infrastructure.entity`:

- `BookcaseService.java` (line 8)
- `BookcaseFacade.java` (line 5 — `findById` returns `Optional<BookcaseEntity>`)
- `BookcaseRepository.java` (line 3 — multiple methods accept/return `BookcaseEntity`)

This means the core package has a compile-time dependency on the infrastructure entity, which is a JPA-annotated class. The domain has no proper `Bookcase` domain model — `BookcaseEntity` IS the domain model, but it lives in infrastructure.

```
Ideal:     Controller → Facade → Service → Domain Model
                                              ↓ (port)
                                           Repository Port
                                              ↓ (adapter)
                                           JPA Entity

Actual:    Controller → Facade → Service → BookcaseEntity (infra)
                                              ↓ (port)
                                           Repository Port → BookcaseEntity (infra)
```

*Inference:* This is the next architectural refactor — introduce a `Bookcase` domain model in core, and have the repository adapter translate between `Bookcase` and `BookcaseEntity`. The shelf module already does this (has `Shelf` domain model distinct from persistence).

### Cross-context coupling

- Bookcase → Shelf: via `ShelfAccessPort` (correct, through owned port + adapter)
- Cataloging/Book → Shelf: via its own `ShelfAccessPort` (same pattern)
- The cataloging module's `ShelfAccessPortAdapter` still uses `@Component` without an explicit bean name. If a third module adds one, the collision will recur.

---

## 5) Naming and API Semantics Audit

| Current Name | Issue | Recommended |
|---|---|---|
| `ShelfAccessPort.createShelf(..., String string, ...)` | `string` says nothing | `String shelfLabel` |
| `ShelfFacade.createShelf(..., String s, ...)` | Same problem upstream | `String shelfLabel` |
| `BookcaseFacade.findBookCaseById(Long aLong)` | `aLong` says nothing | `Long bookcaseId` |
| `BookcaseFacade.findById(Long bookcaseId)` | Two find-by-id methods with different return types (`Optional<BookcaseDTO>` vs `Optional<BookcaseEntity>`) on the same facade | Consolidate or clarify naming: `findBookcaseDTOById` vs `findBookcaseEntityById`, or remove entity-returning method from the facade |
| `addShelf(BookcaseEntity, int label, int position, ...)` | `label` is an `int` but used as string `"Shelf " + label` | Consider `int shelfNumber` to convey it's an ordinal |
| `BookcaseService.existingRecordError` (field) | A pre-constructed exception with a fixed stack trace | Create new instance at throw site |

---

## 6) Data, Persistence, Transaction Review

### Transaction boundaries

| Method | `@Transactional`? | Risk |
|---|---|---|
| `deleteBookcase()` | Yes (`jakarta.transaction`) | Correct. Shelf deletion + bookcase deletion in one tx. |
| `createNewBookCase()` | **No** | **Partial shelves on failure.** This is the highest-risk gap. |
| `CreateShelfUseCase.execute()` | No | Each shelf save is auto-committed independently. |
| `DeleteShelvesUseCase.execute()` | Yes (Spring `@Transactional`) | Correct. Books + shelves deleted atomically. |

### Consistency concerns

- **Delete flow:** `deleteBookcase` → `deleteAllShelvesInBookcase` (deletes books first via `BookAccessPort`, then shelves) → `deleteById` (deletes bookcase). Order is correct. Transaction covers all three steps.
- **Create flow:** No transaction wraps the full `save(bookcase) + N * createShelf()` sequence. If the DB connection drops after shelf #3, 3 shelves are orphaned (no bookcase FK enforcement visible in the entity, relying on application-level consistency).

### Query patterns

- `getAllBookcases()`, `getAllBookcaseLocations()`, `findByLocation()`, `findByAppUserId()` — all unbounded. No pagination. Acceptable at current scale but worth monitoring. `findAll()` in both the bookcase and shelf modules is an unbounded fetch.

---

## 7) Security and Safety (lite)

- **AuthZ:** `deleteBookcase` endpoint accepts a `bookcaseId` path variable with no ownership check. Any authenticated user can delete any bookcase. The `createBookCase` and `getAllBookcases` endpoints correctly use `@AuthenticationPrincipal`. *Inference:* If this is a multi-user system, `deleteBookcase` should verify the caller owns the bookcase.
- **Input validation:** `bookcaseId` is a `Long` from `@PathVariable` — no null check, but Spring handles type conversion. No explicit validation on the `CreateBookcaseRequest` body (no `@Valid`, no `@NotNull`).
- **Logging:** `createBookcaseRequest.location()` is logged directly. Low risk, but worth sanitizing if location can contain user-controlled input.

---

## 8) Tests and Minimum Confidence Suite

### What exists

`BookcaseServiceTest` (417 lines) covers:
- `addShelf` — 5 tests (various labels, positions, capacities)
- `createNewBookCase` — 2 tests (success, duplicate conflict)
- `getAllBookcaseLocations` — 1 test
- `findById` — 2 tests (found, not found)
- `getAllBookcasesByLocation` — 2 tests (found, empty)
- `getAllBookcasesByUserId` — 2 tests (found, empty)
- `deleteBookcase` — 1 test (happy path only)
- `getAllBookcases` — 2 tests (found, empty)
- `findBookCaseById` — 2 tests (found, empty)

### Gaps tied to risk

1. **No order verification on delete** (see 3.4)
2. **No test for `deleteBookcase` when shelves fail** — what happens if `shelfAccessPort.deleteAllShelvesInBookcase()` throws? Does the bookcase survive?
3. **No integration test for `@Transactional` behavior** — the unit test mocks everything, so the transaction annotation is never exercised.
4. **No controller test** for the simplified constructor and endpoint routing (verifying the controller actually compiles and wires with one dependency).
5. **No negative test for `createNewBookCase` partial failure** — shelf creation throws mid-loop.

### 10 targeted test cases

1. `deleteBookcase_shouldDeleteShelvesBeforeBookcase` — use `InOrder`
2. `deleteBookcase_shouldNotDeleteBookcase_whenShelfDeletionFails` — mock `shelfAccessPort.deleteAllShelvesInBookcase()` to throw, verify `deleteById` is never called
3. `createNewBookCase_shouldRollbackOnShelfCreationFailure` — (integration test) verify no bookcase persisted when shelf creation throws
4. `createNewBookCase_shouldCreateExactlyNShelves` — verify `createShelf` called with positions 1..N
5. `deleteBookcase_withNullId_shouldThrow` — edge case
6. `createShelf_viaPort_delegatesToShelfFacade` — unit test for `ShelfAccessPortAdapter`
7. `deleteAllShelves_viaPort_delegatesToShelfFacade` — unit test for `ShelfAccessPortAdapter`
8. `BookCaseController_deleteEndpoint_returns204` — Spring MVC test
9. `BookCaseController_createEndpoint_returns201` — Spring MVC test
10. `BookCaseController_onlyDependsOnBookcaseFacade` — reflection test or compile-time verification

### Commands

```bash
mvn test
mvn -Dtest=BookcaseServiceTest test
mvn -Dtest=BookCaseControllerTest test  # if it exists
```

---

## 9) Suggested Micro-Refactors (small, high leverage)

1. **Rename `String string` → `String shelfLabel`** in `ShelfAccessPort` and `ShelfAccessPortAdapter`. One-line change per file, large clarity gain.

2. **Add `@Transactional` to `createNewBookCase()`**. One annotation, prevents partial-state.

3. **Replace `jakarta.transaction.Transactional` with `org.springframework.transaction.annotation.Transactional`** in `BookcaseService`. One import change.

4. **Make `addShelf()` package-private.** It's not on the `BookcaseFacade` interface, only called internally and from tests (same package). Reducing visibility prevents accidental external use.

5. **Stop reusing `existingRecordError` as a field.** Replace with `throw new ResponseStatusException(HttpStatus.CONFLICT, "...")` at the throw site. This gives correct stack traces and avoids sharing mutable state in the exception object.

6. **Add explicit bean name to cataloging's `ShelfAccessPortAdapter`** (`@Component("catalogingShelfAccessPortAdapter")`). The bookcase adapter was fixed; the cataloging one is still using the default name and is one new module away from the same collision.

7. **Use `InOrder` in `deleteBookcase` test.** Three-line change, encodes the contract.

8. **Extract the `BookcaseEntity → BookcaseDTO` mapping** into a static method on `BookcaseDTO` or a dedicated mapper. The same 6-arg constructor call is duplicated 4 times in `BookcaseService` (lines 80–87, 95–102, 120–129, and `findBookCaseById`).

---

## 10) Questions I Would Ask in a Real PR Review

1. **Why is `createNewBookCase()` not `@Transactional`?** If shelf #4 of 5 fails, the bookcase exists with 3 shelves. Is this the intended behavior, or an oversight? (Ref: `BookcaseService.java` lines 31–63)

2. **Is there a foreign key between shelves and bookcases at the DB level?** If so, `deleteById(bookcaseId)` might fail with a FK violation if shelves aren't deleted first — which the PR fixed. But the test doesn't verify order. (Ref: `BookcaseService.java` lines 107–111)

3. **Should `deleteBookcase` verify the bookcase exists before deleting shelves?** Currently it calls `deleteAllShelvesInBookcase` even if the bookcase ID is invalid. `deleteAllShelvesInBookcase` would just be a no-op, but `deleteById` might silently succeed (Spring Data `deleteById` does nothing for non-existent IDs). No error is returned to the caller. (Ref: `BookcaseService.java` lines 108–110)

4. **Why does `BookcaseFacade` expose `Optional<BookcaseEntity>`?** The facade is an inbound port — it should not leak infrastructure types. `findById` and `findBookCaseById` on the same facade do the same thing but return different types. Who consumes `findById`? (Ref: `BookcaseFacade.java` line 30)

5. **Was the `jakarta.transaction.Transactional` import intentional, or an IDE auto-import artifact?** The shelf module uses Spring's `@Transactional`. (Ref: `BookcaseService.java` line 9)

6. **Should the cataloging module's `ShelfAccessPortAdapter` also get an explicit bean name?** The ADR mentions the collision and the fix, but only applies it to one of the two adapters. (Ref: `cataloging/book/infrastructure/adapter/outbound/ShelfAccessPortAdapter.java` line 9)

7. **Does the delete endpoint need authorization?** `@DeleteMapping("/delete/{bookcaseId}")` takes a raw ID with no ownership check. `createBookCase` and `getAllBookcases` both use `@AuthenticationPrincipal`. (Ref: `BookCaseController.java` lines 47–51)

8. **Why is `addShelf` public?** It's not declared in `BookcaseFacade`. If it's only an internal helper, its visibility should match. (Ref: `BookcaseService.java` line 113)

9. **The `shelf.getShelfId()` in `CreateShelfUseCase` logging — is `shelfId` populated at that point?** The shelf was just constructed with `null` as its ID (see `ShelfService.createShelf`, line 54), and `save()` may not mutate the domain object in-place. (Ref: `CreateShelfUseCase.java` lines 34–35)

10. **Is `Set.copyOf(bookcaseFacade.getAllBookcaseLocations())` necessary?** The service returns a `List`. If the intent is deduplication, should that happen in the service/repository query (`SELECT DISTINCT`)? If the intent is immutability, `List.copyOf()` would suffice. (Ref: `BookCaseController.java` line 56)
