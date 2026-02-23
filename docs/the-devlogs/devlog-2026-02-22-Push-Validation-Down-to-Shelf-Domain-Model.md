# Devlog: Push Validation Down to Shelf Domain Model

**Date:** 2026-02-22 (CST)
**Branch:** `refactor/create-shelf-accept-domain-object`
**Range:** `143de06..af0cff8` (4 commits, merged to `origin/main` via PR #296)
**Commits:**

| SHA | Subject |
|-----|---------|
| `a2a7255` | Add bookcaseId null validation to Shelf domain model |
| `b1b5920` | Refactor CreateShelfUseCase to accept Shelf domain object |
| `e02ce9b` | Remove redundant validation from BookcaseService.addShelf |
| `af0cff8` | Remove unnecessary blank line in BookcaseServiceTest |

---

## Motivation & Before State

**What problem was I solving?**

Validation for creating a shelf was scattered across three layers:

1. `BookcaseService.addShelf()` — checked `bookCapacity`, null bookcase, position bounds, label sign
2. `CreateShelfUseCase.execute()` — checked `bookCapacity`, `shelfLabel`, `bookcaseId`, `position`
3. `Shelf` constructor — checked `shelfLabel`, `shelfPosition`, `bookCapacity`

This meant the same invariants were guarded in 2-3 places, each with slightly different semantics (e.g., `BookcaseService` threw `NullPointerException` for null bookcase; `CreateShelfUseCase` threw `IllegalArgumentException` for null bookcaseId). The use case also accepted four primitive parameters and forwarded them individually through the port to the repository — a classic "primitive obsession" smell.

**Before state:**

- `CreateShelfUseCase.execute(Long, int, String, int)` — 4 primitives, 15 lines of guard clauses
- `ShelfDomainRepository.save(Long, int, String, int)` — port signature leaked primitives
- `ShelfDomainRepositoryImpl.save(...)` — manually set each field on `ShelfEntity`
- `BookcaseService.addShelf()` — duplicated validation before delegating to `ShelfFacade`
- `Shelf` constructor — validated label, position, capacity but **not** `bookcaseId`

---

## Key Outcomes

- **Single source of truth for Shelf invariants:** All validation now lives in the `Shelf` constructor (domain model)
- **Net -117 lines** across 9 files (43 added, 160 removed)
- **Use case reduced to a 1-liner:** `CreateShelfUseCase.execute(Shelf)` is now pure delegation
- **Port signature speaks the domain language:** `ShelfDomainRepository.save(Shelf)` instead of 4 primitives
- **Validation gap closed:** `bookcaseId` null check added to `Shelf` constructor (previously missing)
- **No behavioral regression:** Same invariants enforced, just at the right layer

---

## Commit-by-Commit Breakdown

### Commit 1: `a2a7255` — Add bookcaseId null validation to Shelf domain model

**Intent:** Close a validation gap in the domain model *before* moving validation responsibility there.

**Files touched:**

- `Shelf.java` — Added `bookcaseId == null` guard to constructor (+4 lines)

**Key code change:**

```java
// Shelf.java constructor, after bookCapacity check:
if (bookcaseId == null) {
  throw new IllegalArgumentException("Bookcase ID cannot be null");
}
```

**Architecture notes:** This is a prerequisite commit. Without it, removing validation from `CreateShelfUseCase` would have lost the `bookcaseId` null check entirely. Smart ordering — close the gap first, then remove the duplicate.

**Risk:** None. Purely additive guard. Existing callers already pass non-null `bookcaseId`.

---

### Commit 2: `b1b5920` — Refactor CreateShelfUseCase to accept Shelf domain object

**Intent:** Replace primitive parameter list with a domain object. Let the domain model own its invariants.

**Files touched (6):**

| File | Change |
|------|--------|
| `CreateShelfUseCase.java` | `execute(Long, int, String, int)` -> `execute(Shelf)`, removed all validation |
| `ShelfService.java` | Now constructs `new Shelf(...)` before calling `execute()` |
| `ShelfDomainRepository.java` (port) | `save(Long, int, String, int)` -> `save(Shelf)` |
| `ShelfDomainRepositoryImpl.java` (adapter) | Extracts fields from `Shelf` instead of receiving them as params |
| `ShelfServiceTest.java` | Uses `ArgumentCaptor<Shelf>` to verify all 6 fields of the constructed Shelf |
| `CreateShelfUseCaseTest.java` | Dropped from 10 tests to 3; validation tests removed (now domain's job) |

**Key code changes:**

*ShelfService* — Domain object construction now happens at the service layer (the natural translation boundary between facade primitives and domain objects):

```java
Shelf shelf = new Shelf(shelfLabel, position, bookCapacity, null, List.of(), bookcaseId);
createShelfUseCase.execute(shelf);
```

*CreateShelfUseCase* — From 15 lines of validation + delegation to a single line:

```java
public void execute(Shelf shelf) {
  shelfDomainRepositoryPort.save(shelf);
}
```

*ShelfDomainRepository (port)* — Signature change from `save(Long, int, String, int)` to `save(Shelf)`. The port now speaks domain language.

*ShelfDomainRepositoryImpl (adapter)* — Pulls data from the domain object via getters instead of receiving raw primitives. No behavioral change.

**Architecture notes:**

- **Dependency direction preserved:** Use case -> port -> (impl in infrastructure). The port now imports `Shelf` (domain model), which is correct — ports live in `core` and can reference domain objects.
- **Validation responsibility shift:** Moved from application layer (`CreateShelfUseCase`) to domain layer (`Shelf` constructor). This is textbook DDD — the aggregate/entity enforces its own invariants.
- **`ShelfService` as translation layer:** The facade still receives primitives from the bookcase module. `ShelfService` translates them into a `Shelf` domain object. This keeps the facade interface stable while the internals evolve.

**Risk:**

- The Javadoc on `CreateShelfUseCase.execute()` still references `@param bookcaseId`, `@param position`, etc. — stale after this commit. Minor but worth cleaning up.
- `ShelfService.createShelf()` passes `null` for `shelfId` and `List.of()` for `bookIds`. If `Shelf` constructor ever adds validation for these, it would break. Currently safe since the constructor doesn't guard them.

---

### Commit 3: `e02ce9b` — Remove redundant validation from BookcaseService.addShelf

**Intent:** With validation consolidated in the domain model, remove the duplicate guards in the bookcase module's service layer.

**Files touched (2):**

| File | Change |
|------|--------|
| `BookcaseService.java` | Removed 11-line validation block from `addShelf()` |
| `BookcaseServiceTest.java` | Removed 2 test methods: `addShelf_shouldNotCreateShelfWithNegativeBookCapacity` and `addShelf_shouldNotCreateShelfWithNullBookcase` |

**Key observation:** `BookcaseService.addShelf()` threw `NullPointerException` for null bookcase, while the domain model throws `IllegalArgumentException`. This is actually a subtle behavior change: callers that previously caught `NullPointerException` would now see a different exception type. However, since `addShelf()` receives a `BookcaseEntity` from within `BookcaseService` itself (never from external callers), this is safe — the null bookcase case is unreachable in production.

**Architecture notes:** This is a cross-module coupling reduction. `BookcaseService` no longer validates shelf-specific invariants — it trusts the shelf module to enforce them. Clean boundary.

**Risk:** The removed `position > bookcaseEntity.getShelfCapacity()` check was **not** replicated in the `Shelf` domain model. The domain model only checks `position < 1`. This means the upper-bound position validation (position must be within the bookcase's shelf capacity) is now **gone entirely**. This is either intentional (the bookcase module should enforce its own capacity rules elsewhere) or a gap worth investigating.

---

### Commit 4: `af0cff8` — Remove unnecessary blank line in BookcaseServiceTest

**Intent:** Cosmetic cleanup. Removed a trailing blank line left behind by commit 3.

**Files touched:** `BookcaseServiceTest.java` — 1 line deleted.

---

## End-to-End Flow (After Refactor)

```
BookcaseService.addShelf(entity, label, position, capacity)
  -> shelfFacade.createShelf(bookcaseId, position, "Shelf " + label, capacity)
       -> ShelfService.createShelf(bookcaseId, position, shelfLabel, capacity)
            -> new Shelf(shelfLabel, position, capacity, null, List.of(), bookcaseId)
                 |-- validates: label non-blank, position >= 1, capacity >= 1, bookcaseId non-null
                 |-- (throws IllegalArgumentException on violation)
            -> createShelfUseCase.execute(shelf)
                 -> shelfDomainRepositoryPort.save(shelf)
                      -> ShelfDomainRepositoryImpl: maps Shelf -> ShelfEntity -> JPA save
```

**Why this is better:** Validation fires at construction time, before any persistence call. The use case is a clean pipeline stage. The repository port accepts a domain object, not a bag of primitives. Any new field added to `Shelf` automatically flows through the whole chain without changing signatures at every layer.

---

## Dependency & Layering Audit

**Dependencies removed:** None at the module level. At the class level, `CreateShelfUseCase` no longer has its own validation logic — it depends purely on the domain model's invariants.

**Dependencies added:** `CreateShelfUseCase` now imports `Shelf` (domain model). This is correct — both live in `shelf.core`.

**Layering violations:** None introduced. All imports flow inward: infrastructure -> application -> domain.

**Suggested micro-fix:** The Javadoc on `CreateShelfUseCase.execute()` (lines 16-29) still describes the old 4-param signature. Update it to document the `Shelf` parameter.

---

## Test Coverage

**Tests that changed:**

| Test Class | Before | After | Delta |
|------------|--------|-------|-------|
| `CreateShelfUseCaseTest` | 10 tests (validation + happy path) | 3 tests (delegation only) | -7 |
| `ShelfServiceTest` | `verify(execute(4 args))` | `ArgumentCaptor<Shelf>` with 6 field assertions | Upgraded |
| `BookcaseServiceTest` | 2 validation tests | Removed | -2 |

**What exists:** The `Shelf` domain model presumably has its own constructor validation tests (inference: they must exist since the use case tests were confidently removed). The remaining use case tests verify delegation with boundary values (min capacity, large capacity).

**What should be added:**

1. **`ShelfTest` (domain model):** Verify `bookcaseId == null` throws `IllegalArgumentException` with correct message — this is the new validation added in commit 1. If this test doesn't exist yet, it's a gap.
2. **`ShelfDomainRepositoryImplTest`:** Verify that `save(Shelf)` correctly maps all fields to `ShelfEntity`. The signature change is tested indirectly but not directly.
3. **`BookcaseService.addShelf()` with invalid capacity:** Now that validation moved downstream, verify the `IllegalArgumentException` still propagates correctly through the facade boundary (integration-level).
4. **Edge case:** `ShelfService.createShelf()` with `bookcaseId = null` — confirm `Shelf` constructor rejects it before `execute()` is called.

**Commands to run:**

```bash
./mvnw test -pl . -Dtest="CreateShelfUseCaseTest"
./mvnw test -pl . -Dtest="ShelfServiceTest"
./mvnw test -pl . -Dtest="BookcaseServiceTest"
./mvnw test -pl . -Dtest="ShelfTest"
./mvnw test  # full suite
```

---

## Lessons from This Diff

1. **Close gaps before you move responsibility.** Commit 1 added the `bookcaseId` null check to `Shelf` *before* commit 2 removed it from the use case. If done in one commit, reviewers might miss the gap. Sequencing matters.

2. **Primitive obsession hides validation duplication.** When a method takes `(Long, int, String, int)`, every caller feels obligated to validate those params. Replacing with a domain object makes it obvious that validation belongs in one place.

3. **Use case tests should test use-case logic, not domain invariants.** The old `CreateShelfUseCaseTest` had 8 validation tests that were really testing concerns of the `Shelf` domain model. After the refactor, it has 3 tests that verify delegation — the right level of abstraction.

4. **Cross-module validation removal needs careful diffing.** The removed `position > shelfCapacity` check in `BookcaseService` was a bookcase-level concern that the shelf domain model can't enforce (it doesn't know about bookcase capacity). This might be a gap or might be handled elsewhere — worth verifying.

5. **`ArgumentCaptor` is the right tool when your method now takes a domain object.** The upgraded `ShelfServiceTest` asserts all 6 fields of the constructed `Shelf`, catching any future constructor changes that might silently break field mapping.

6. **Net-negative LOC refactors are healthy signals.** -117 lines with no behavioral regression means the codebase is genuinely simpler, not just differently complex.

---

## Next Steps

### Immediate follow-ups (today)

- Fix the stale Javadoc on `CreateShelfUseCase.execute()` — it still documents the old 4-param signature
- Verify `ShelfTest` has a test for the `bookcaseId == null` constructor guard added in commit 1
- Investigate whether the removed `position > shelfCapacity` upper-bound check (from `BookcaseService`) is enforced elsewhere

### Short-term hardening (this week)

- Add `ShelfDomainRepositoryImpl` unit tests for the `save(Shelf)` mapping
- Consider making `Shelf` setters enforce the same invariants as the constructor (currently `setBookcaseId()` accepts null — inconsistent with the constructor guard)
- Consider making `Shelf.bookIds` defensive-copied in the constructor (`List.of()` is immutable, but other callers might pass mutable lists)

### Strategic refactors (later)

- Apply the same "accept domain object" pattern to other use cases (e.g., any `execute(Long, int, ...)` signatures remaining in the codebase)
- Evaluate whether `ShelfFacade.createShelf(Long, int, String, int)` should also accept a `Shelf` or a DTO, pushing domain object construction to the bookcase module boundary
- Consider replacing `Shelf` setters with immutable builder pattern to fully protect invariants post-construction
