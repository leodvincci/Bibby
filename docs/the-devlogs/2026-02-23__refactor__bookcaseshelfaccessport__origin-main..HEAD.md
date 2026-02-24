# Devlog: Decouple Bookcase Module from Shelf via ShelfAccessPort

Date: 2026-02-23 (America/Chicago)
Branch: `refactor/bookcase-shelf-access-port`
Range: `main..HEAD` (note: `origin/main` already includes this branch via merged PR #314; using local `main` at `4797c13` as the fork point)
Commits:
- `cb1f076` Decouple bookcase module from ShelfFacade via ShelfAccessPort
- `3bc7829` Add logging to shelf creation and deletion operations
- `d629633` Introduce ADR 0001 for ShelfAccessPort Implementation
- `df578de` Fix conflicting bean name for bookcase ShelfAccessPortAdapter
- `ce95354` Enhance ShelfAccessPortAdapter implementation and address bean name collision

---

## Summary

- **What problem was I solving?** The bookcase module's core (`BookcaseService`) held a compile-time dependency on the shelf module's inbound port (`ShelfFacade`), and `BookCaseController` both orchestrated cross-module business logic and bypassed its own inbound port by injecting the concrete `BookcaseService` alongside `BookcaseFacade`.
- **What was the "before" state?** `BookcaseService` imported `ShelfFacade` directly. `BookCaseController` had three constructor dependencies (`BookcaseService`, `BookcaseFacade`, `ShelfFacade`), called `bookCaseService.*` for some endpoints (bypassing the facade), and orchestrated the two-step delete (shelves then bookcase) in the HTTP layer. Delete order was also wrong — the bookcase entity was removed before its shelves.
- **Most important outcomes:**
  - `BookcaseService` depends only on its own outbound port (`ShelfAccessPort`), not on `ShelfFacade`
  - `BookCaseController` has a single dependency: `BookcaseFacade`
  - Delete orchestration moved to service layer with `@Transactional` and correct order (shelves first)
  - Consistent hexagonal pattern across book, bookcase, and shelf modules
  - Bean name collision between bookcase and cataloging `ShelfAccessPortAdapter` classes resolved via explicit `@Component("bookcaseShelfAccessPortAdapter")`
  - ADR 0001 documents the decision, constraints, alternatives, and follow-up work
  - Observability improved: shelf creation and bulk deletion now emit `INFO`-level logs

---

## Commit-by-Commit Analysis

### Commit 1: `cb1f076` — Decouple bookcase module from ShelfFacade via ShelfAccessPort

**Intent:** Remove the bookcase core's compile-time dependency on the shelf module's inbound port, and eliminate business logic from the controller.

**Files touched:**

| File | Why |
|------|-----|
| `bookcase/core/ports/outbound/ShelfAccessPort.java` (new) | New outbound port interface owned by the bookcase module |
| `bookcase/infrastructure/adapter/outbound/ShelfAccessPortAdapter.java` (new) | Infrastructure adapter bridging `ShelfAccessPort` → `ShelfFacade` |
| `bookcase/core/application/BookcaseService.java` (modified) | Swap `ShelfFacade` for `ShelfAccessPort`; move delete orchestration here |
| `web/controllers/stacks/bookcase/BookCaseController.java` (modified) | Strip down to single `BookcaseFacade` dependency |

**Key code changes:**

- **`ShelfAccessPort`** — A 2-method interface defining exactly the shelf operations the bookcase module needs:
  - `void deleteAllShelvesInBookcase(Long bookcaseId)`
  - `void createShelf(Long bookcaseId, int position, String string, int bookCapacity)`

- **`ShelfAccessPortAdapter`** — Pure pass-through `@Component` delegating to `ShelfFacade`. This is the only class in the bookcase module that imports from the shelf module.

- **`BookcaseService`** — Constructor changed from `(BookcaseRepository, ShelfFacade)` to `(BookcaseRepository, ShelfAccessPort)`. The `deleteBookcase()` method now:
  1. Calls `shelfAccessPort.deleteAllShelvesInBookcase(bookcaseId)` (shelves first)
  2. Calls `bookcaseRepository.deleteById(bookcaseId)` (bookcase second)
  3. Is annotated `@Transactional` for atomicity

  Previously, `deleteBookcase()` only called `bookcaseRepository.deleteById()` — shelf cleanup was in the controller.

- **`BookCaseController`** — Reduced from 3 constructor params to 1 (`BookcaseFacade`). Removed imports of `BookcaseService` and `ShelfFacade`. All four endpoints now route through `bookcaseFacade.*`:
  - `createBookCase` → was `bookCaseService.createNewBookCase`, now `bookcaseFacade.createNewBookCase`
  - `deleteBookcase` → was `bookshelfFacade.deleteAllShelvesInBookcase` + `bookcaseFacade.deleteBookcase`, now just `bookcaseFacade.deleteBookcase`
  - `getAllBookcaseLocations` → was `bookCaseService.getAllBookcaseLocations`, now `bookcaseFacade.getAllBookcaseLocations`
  - `getBookcaseByLocation` → was `bookCaseService.getAllBookcasesByLocation`, now `bookcaseFacade.getAllBookcasesByLocation`

**Architecture notes:**
- Dependency direction fixed: `BookcaseService` (core) → `ShelfAccessPort` (core/ports/outbound) ← `ShelfAccessPortAdapter` (infrastructure). The adapter depends inward on the port and outward on `ShelfFacade`. Core never touches infrastructure or another module's core.
- The pattern mirrors what was already done in the book/cataloging module (`cataloging.book.core.port.outbound.ShelfAccessPort`).

**Risk / edge cases:**
- The `@Transactional` annotation uses `jakarta.transaction.Transactional`, not `org.springframework.transaction.annotation.Transactional`. Both work with Spring Boot, but `jakarta.transaction.Transactional` does not support `readOnly`, `propagation`, or `rollbackFor` attributes. If finer control is needed later, the import should switch to Spring's variant.
- The delete order fix (shelves before bookcase) is a **behavior change**. If there was code relying on the old order (bookcase deleted first while shelves still exist), it will break. This is almost certainly a correctness improvement, not a regression — but it should be verified against any foreign key constraints in the schema.
- `ShelfAccessPortAdapter` was initially registered with `@Component` (no explicit name), which collided with the cataloging module's identically-named class. This is fixed in commit `df578de`.

**Verification:**
- `mvn test -pl . -Dtest=BookcaseServiceTest` — verify constructor change doesn't break existing tests
- `mvn spring-boot:run` — verify no `ConflictingBeanDefinitionException` (fixed in later commit)

---

### Commit 2: `3bc7829` — Add logging to shelf creation and deletion operations

**Intent:** Improve observability for shelf lifecycle events that previously had no log output.

**Files touched:**

| File | Why |
|------|-----|
| `shelf/core/application/ShelfService.java` (modified) | Add `logger.info` after bulk shelf deletion |
| `shelf/core/application/usecases/CreateShelfUseCase.java` (modified) | Add `logger.info` after shelf creation |

**Key code changes:**

- **`ShelfService.deleteAllShelvesInBookcase()`** — Added `logger.info("Deleted all shelves in bookcase with ID: {}", bookcaseId)` after `deleteShelvesUseCase.execute(bookcaseId)`.

- **`CreateShelfUseCase.execute()`** — Added `logger.info("Shelf created with ID: {} for bookcase: {}", shelf.getShelfId(), shelf.getBookcaseId())` after `shelfDomainRepositoryPort.save(shelf)`.

- Both classes initialize their logger as an instance field (`private final Logger logger = LoggerFactory.getLogger(...)`) rather than a `static final`. This is a minor style inconsistency — `BookcaseService` uses `private static final Logger log`. Not a bug, but worth normalizing.

**Architecture notes:**
- These changes are within the shelf module's application layer. No cross-module boundaries touched.

**Risk / edge cases:**
- `shelf.getShelfId()` in `CreateShelfUseCase` may return `null` if the repository's `save()` does not flush and return the generated ID synchronously. The log statement would print `null` rather than failing, so it's safe but potentially misleading. Verify whether `shelfDomainRepositoryPort.save()` populates the ID before the log line executes.

**Verification:**
- Run any shelf-creation flow and check logs for the new messages.

---

### Commit 3: `d629633` — Introduce ADR 0001 for ShelfAccessPort Implementation

**Intent:** Document the architectural decision formally and update `BookcaseServiceTest` to align with the new constructor signature.

**Files touched:**

| File | Why |
|------|-----|
| `docs/architecture/stacks/adr-0001-bookcase-shelf-access-port.md` (new) | ADR documenting the decision, alternatives, and consequences |
| `BookcaseServiceTest.java` (modified) | Replace `@Mock ShelfFacade shelfFacade` with `@Mock ShelfAccessPort shelfAccessPort` |

**Key code changes:**

- **`BookcaseServiceTest`** — All `verify(shelfFacade).createShelf(...)` calls changed to `verify(shelfAccessPort).createShelf(...)`. The `deleteBookcase` test now also verifies `verify(shelfAccessPort).deleteAllShelvesInBookcase(bookcaseId)` before `verify(bookcaseRepository).deleteById(bookcaseId)`, matching the new service implementation.

- **ADR 0001** — Comprehensive document covering context (two dependency violations), the decision (owned port + adapter), three considered alternatives (status quo, owned port, reuse book module's port), positive consequences, negative tradeoffs, and follow-up work items.

**Architecture notes:**
- The ADR identifies a remaining layering concern: `BookcaseFacade.findById()` returns `Optional<BookcaseEntity>` — an infrastructure entity leaking through an inbound port. This is flagged as a follow-up.

**Risk / edge cases:**
- None. Documentation and test alignment only.

**Verification:**
- `mvn test -Dtest=BookcaseServiceTest` — all 13 test methods should pass

---

### Commit 4: `df578de` — Fix conflicting bean name for bookcase ShelfAccessPortAdapter

**Intent:** Resolve `ConflictingBeanDefinitionException` at startup caused by two classes named `ShelfAccessPortAdapter` in different packages.

**Files touched:**

| File | Why |
|------|-----|
| `bookcase/infrastructure/adapter/outbound/ShelfAccessPortAdapter.java` (modified) | Change `@Component` to `@Component("bookcaseShelfAccessPortAdapter")` |

**Key code changes:**
- Single-line change: `@Component` → `@Component("bookcaseShelfAccessPortAdapter")`.
- Spring's default bean naming derives the name from the simple class name (`shelfAccessPortAdapter`). Since `cataloging.book.infrastructure.adapter.outbound.ShelfAccessPortAdapter` already exists with the same default name, the explicit qualifier disambiguates.

**Architecture notes:**
- This is a Spring wiring concern, not a domain concern. The adapter's behavior is unchanged.
- The cataloging module's `ShelfAccessPortAdapter` still uses the default bean name. The ADR flags this as a follow-up for consistency.

**Risk / edge cases:**
- If any `@Qualifier("shelfAccessPortAdapter")` references existed elsewhere, they would now resolve to the cataloging module's adapter only. No such references were found in the diff.

**Verification:**
- `mvn spring-boot:run` — app should start without bean definition errors.

---

### Commit 5: `ce95354` — Enhance ShelfAccessPortAdapter implementation and address bean name collision

**Intent:** Update the ADR to reflect the bean name fix and clean up test formatting.

**Files touched:**

| File | Why |
|------|-----|
| `docs/architecture/stacks/adr-0001-bookcase-shelf-access-port.md` (modified) | Add bean-collision documentation, follow-up items, compliance rules |
| `BookcaseServiceTest.java` (modified) | Reformat long `verify()` calls for line-length compliance |

**Key code changes:**
- ADR additions: bean name collision risk section, new compliance rule ("outbound port adapters with shared class names must use explicit bean names"), detection command (`rg -n "class ShelfAccessPortAdapter" src/main/java/`), and a follow-up to add explicit bean name to the book module's adapter.
- Test formatting: multi-line `verify(shelfAccessPort).createShelf(...)` calls split for readability. Import reordering (`ShelfAccessPort` before `BookcaseEntity`).

**Architecture notes:**
- No behavioral changes. Documentation hardening.

**Risk / edge cases:**
- None.

---

## End-to-End Flow Walkthrough

### Delete bookcase flow (the most architecturally significant change)

**Before:**
```
Client → DELETE /api/v1/bookcase/delete/{id}
  → BookCaseController.deleteBookcase()
    → bookshelfFacade.deleteAllShelvesInBookcase(id)  // controller orchestration
    → bookcaseFacade.deleteBookcase(id)                // no shelf cleanup in service
      → BookcaseService.deleteBookcase()
        → bookcaseRepository.deleteById(id)            // bookcase deleted FIRST
```

**After:**
```
Client → DELETE /api/v1/bookcase/delete/{id}
  → BookCaseController.deleteBookcase()
    → bookcaseFacade.deleteBookcase(id)                // single call
      → BookcaseService.deleteBookcase()               // @Transactional
        → shelfAccessPort.deleteAllShelvesInBookcase(id)  // shelves FIRST
          → ShelfAccessPortAdapter
            → ShelfFacade.deleteAllShelvesInBookcase(id)
              → ShelfService → DeleteShelvesUseCase
        → bookcaseRepository.deleteById(id)            // bookcase SECOND
```

### Create bookcase flow (dependency change only)

```
Client → POST /api/v1/bookcase/create
  → BookCaseController.createBookCase()
    → bookcaseFacade.createNewBookCase(...)             // was bookCaseService.*
      → BookcaseService.createNewBookCase()
        → bookcaseRepository.save(entity)
        → for each shelf: shelfAccessPort.createShelf() // was shelfFacade.*
          → ShelfAccessPortAdapter
            → ShelfFacade.createShelf(...)
```

---

## Why This Design Is Better

1. **Core isolation.** `BookcaseService` has zero imports from `library.stacks.shelf`. The only cross-module coupling is in `ShelfAccessPortAdapter` (infrastructure), which is designed to be the single point of breakage if `ShelfFacade` changes.

2. **Controller is pure HTTP.** `BookCaseController` maps HTTP verbs to a single facade. It no longer decides the order of multi-step domain operations or picks between concrete classes and interfaces.

3. **Correct delete semantics.** Shelves are deleted before the bookcase entity. The `@Transactional` annotation ensures atomicity — if shelf deletion fails, the bookcase is not orphaned.

4. **Testability.** `BookcaseServiceTest` mocks `ShelfAccessPort` (a 2-method interface the bookcase module owns) rather than `ShelfFacade` (a 7+ method interface the shelf module owns). Narrower mock surface, clearer test intent.

5. **Consistent pattern.** The book/cataloging module already uses this exact pattern. The bookcase module was the last outlier. All three modules (book, bookcase, shelf) now follow: core → own outbound port ← infrastructure adapter → external facade.

---

## Dependencies Changed

**Removed from `BookcaseService`:**
- `com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade`

**Added to `BookcaseService`:**
- `com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.ShelfAccessPort` (own module)
- `jakarta.transaction.Transactional`

**Removed from `BookCaseController`:**
- `com.penrose.bibby.library.stacks.bookcase.core.application.BookcaseService`
- `com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade`

**New files:**
- `ShelfAccessPort.java` — no external dependencies (pure interface)
- `ShelfAccessPortAdapter.java` — depends on `ShelfFacade` (intentional bridge)

---

## Layering Violation Check

**Resolved violations:**
- `BookcaseService` (core) no longer imports from `shelf.core` — fixed
- `BookCaseController` (web) no longer imports `BookcaseService` (concrete class) — fixed
- `BookCaseController` (web) no longer imports `ShelfFacade` (another module's port) — fixed

**Remaining violations (pre-existing, not introduced by this diff):**
- `BookcaseFacade` (inbound port in core) references `BookcaseEntity` (infrastructure entity) in its `findById` return type. The port should return a DTO or domain object.
- `BookcaseService` (core) imports `BookcaseEntity` from `bookcase.infrastructure.entity`. This is a core → infrastructure dependency. The entity should either live in the domain layer or be mapped at the adapter boundary.

---

## Suggested Micro-Fixes

1. **Rename `String string` parameter in `ShelfAccessPort.createShelf()`.** The third parameter is named `string` — should be `shelfName` or `label` for clarity.

2. **Use `private static final Logger` consistently.** `ShelfService` and `CreateShelfUseCase` use instance-level `private final Logger logger`, while `BookcaseService` uses `private static final Logger log`. Pick one convention.

3. **Consider `@Transactional` on `createNewBookCase()`.** This method creates a bookcase entity and then loops to create shelves. If shelf creation fails midway, the bookcase exists with fewer shelves than expected. The ADR already flags this as a follow-up.

---

## Test Coverage

**Tests changed:**
- `BookcaseServiceTest.java` — 8 test methods updated to mock `ShelfAccessPort` instead of `ShelfFacade`. One new verification added (`deleteAllShelvesInBookcase` is called before `deleteById`).

**Tests that should be added:**

1. **`BookcaseService.deleteBookcase()` — verify ordering.** Use Mockito `InOrder` to assert `shelfAccessPort.deleteAllShelvesInBookcase()` is called before `bookcaseRepository.deleteById()`. The current test verifies both are called but does not enforce order.

2. **`BookCaseController` unit test.** No controller test was changed in this diff. Add a test verifying the controller calls only `bookcaseFacade.deleteBookcase()` for the delete endpoint (no direct shelf calls).

3. **`ShelfAccessPortAdapter` unit test.** Verify pass-through delegation: calling `adapter.createShelf(...)` invokes `shelfFacade.createShelf(...)` with identical arguments.

4. **Integration test for cascading delete.** Create a bookcase with shelves via the service, delete the bookcase, and verify both the bookcase and all shelves are removed from the database.

5. **`BookcaseService.createNewBookCase()` — partial failure test.** Mock `shelfAccessPort.createShelf()` to throw on the 3rd invocation. Verify behavior (currently: bookcase exists with 2 shelves — is this acceptable or should it roll back?).

**Run existing tests:**
```bash
mvn test -Dtest=BookcaseServiceTest
mvn test -Dtest="com.penrose.bibby.library.stacks.bookcase.**"
mvn test
```

---

## Lessons from This Diff

1. **The second module is where a pattern proves itself.** The book module's `ShelfAccessPort` was introduced months ago. Applying the same pattern to the bookcase module validated the approach and revealed a new concern (bean name collision) that wouldn't have surfaced with a single instance.

2. **Controllers accumulate business logic silently.** `BookCaseController` had evolved to orchestrate shelf deletion, inject concrete service classes, and bypass its own facade — three distinct violations that all stem from "just adding one more thing" in the controller.

3. **Delete order bugs hide behind single-module tests.** The old `deleteBookcase()` deleted the bookcase first and relied on the controller to delete shelves beforehand. Unit tests for `BookcaseService` alone couldn't catch the ordering problem because they didn't test the controller's orchestration. Moving the logic into the service makes it unit-testable.

4. **Spring bean names are global.** Two classes with the same simple name in different packages produce the same default bean name. This only surfaces at startup, not at compile time. Explicit `@Component("...")` names are cheap insurance in multi-module monoliths.

5. **ADRs pay for themselves in the same session.** Writing ADR 0001 while the decision was fresh forced explicit enumeration of the alternatives (especially Option C: reuse the book module's port) and identified follow-ups (bean naming, `@Transactional` on create, `BookcaseEntity` in facade) that would otherwise have been forgotten.

---

## Next Steps

### Immediate follow-ups (today)
- Add explicit bean name to the book/cataloging module's `ShelfAccessPortAdapter` (`@Component("bookShelfAccessPortAdapter")`) for consistency and collision prevention
- Rename `String string` → `String shelfName` in `ShelfAccessPort.createShelf()` signature
- Add `InOrder` verification to `BookcaseServiceTest.deleteBookcase()` to enforce shelves-before-bookcase ordering

### Short-term hardening (this week)
- Add `@Transactional` to `BookcaseService.createNewBookCase()` to prevent partial bookcase+shelf creation
- Remove `BookcaseEntity` from `BookcaseFacade.findById()` return type — replace with a domain object or DTO
- Write unit test for `ShelfAccessPortAdapter` (pass-through verification)
- Write integration test for cascading bookcase deletion
- Normalize logger declaration style across shelf and bookcase modules (`static final` vs instance)

### Strategic refactors (later)
- Evaluate whether `BookcaseEntity` should be moved out of `infrastructure.entity` into the domain layer, or whether a proper domain model + mapper should sit between the service and the JPA entity
- Consider an ArchUnit rule enforcing `bookcase.core` cannot import `shelf.core`
- Assess whether the `ShelfAccessPort` pattern should be codified as a project convention (e.g., a Cursor rule or `AGENTS.md` guideline) so new modules adopt it from the start
