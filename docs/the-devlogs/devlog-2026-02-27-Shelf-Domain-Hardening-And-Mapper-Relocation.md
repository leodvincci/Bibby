# Devlog: Shelf Domain Hardening and Mapper Relocation

**Date:** 2026-02-27
**Time:** 11:54 CST
**Branch:** `refactor/shelf-domain-hardening-and-mapper-relocation`
**Range:** `0916f72..71c8002` (3 commits ahead of the merge base with `origin/main`)
**Commits:**

| SHA       | Subject                                                        |
|-----------|----------------------------------------------------------------|
| `8232755` | Relocate ShelfPortModelMapper into inboundPortModels package   |
| `c6ebf3c` | Add null/blank validation guard to Shelf#setShelfLabel         |
| `71c8002` | Rename parameter 'string' to 'shelfLabel' in ShelfAccessPort   |

> **Note:** This branch was merged into `origin/main` via PR #328 (`d5dcf2a`). The range analyzed is the branch's own
> 3 commits against the prior merge-base at `0916f72`.

---

## TL;DR

- **What problem was I solving?** Three small but distinct hygiene issues accumulated during the shelf port model
  migration: (1) the `ShelfPortModelMapper` lived outside the package it mapped for, (2) the `Shelf` domain model
  accepted null/blank labels silently, and (3) the `ShelfAccessPort` interface had a meaningless `string` parameter
  name that obscured intent.
- **What was the "before" state?** After the port model migration (PRs #324–#327), the shelf hexagonal boundary was
  sealed, but the internal code quality still had rough edges — a misplaced mapper, a missing domain guard, and a
  naming slip in a cross-module port.
- **Most important outcomes:**
    - `ShelfPortModelMapper` is now co-located with its port models under `inboundPortModels/mapper/`
    - `Shelf#setShelfLabel` enforces a non-null, non-blank invariant at the domain level
    - `ShelfAccessPort.createShelf(...)` parameter renamed from `string` → `shelfLabel` for self-documenting API
    - All changes are zero-behavior-change (except the new validation guard, which is intentionally stricter)
    - A new `ShelfTest` class exists in the working tree (untracked) covering the validation guard

---

## Commit 1: `8232755` — Relocate ShelfPortModelMapper into inboundPortModels package

### Intent

Co-locate the port model mapper with the port models it serves. Before this commit, `ShelfPortModelMapper` lived in
`ports/inbound/mapper/`, a sibling of `ports/inbound/inboundPortModels/`. This split made discovery harder and violated
package-by-feature conventions — the mapper exists solely to translate `Shelf` domain objects into `ShelfResponse` port
models, so it belongs alongside those models.

### Files touched

| File | Why |
|------|-----|
| `ShelfPortModelMapper.java` | **Moved** from `ports/inbound/mapper/` → `ports/inbound/inboundPortModels/mapper/`. Package declaration updated. |
| `QueryShelfUseCase.java` | Updated import from `...ports.inbound.mapper.ShelfPortModelMapper` → `...ports.inbound.inboundPortModels.mapper.ShelfPortModelMapper` |

### Key code changes

The mapper itself is unchanged — only the `package` statement moves:

```java
// Before
package com.penrose.bibby.library.stacks.shelf.core.ports.inbound.mapper;

// After
package com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.mapper;
```

`QueryShelfUseCase` (the sole consumer within the shelf module) had its import updated accordingly.

### Architecture notes

- **Package cohesion improved.** The `inboundPortModels` package now owns everything related to inbound port models:
  the records (`ShelfResponse`, `ShelfSummaryResponse`) and the mapper that produces them.
- **No layering violation.** The mapper still lives inside `core.ports.inbound`, well within the hexagonal core.
  It depends inward on `core.domain.model.Shelf` (allowed) and on its sibling port models (also allowed).
- **The old `ports/inbound/mapper/` package is now empty** and can be deleted in cleanup.

### Risk / edge cases

- Any consumer outside `QueryShelfUseCase` that imported the old path will get a compile error. Since this branch was
  merged cleanly, no such consumers exist on `main`.
- If other modules were reaching into `ports.inbound.mapper` via reflection or Spring component scanning with explicit
  base-package filters, those would break — but this mapper is a plain utility class (no `@Component`), so that risk
  is effectively zero.

### Verification

```bash
mvn test -pl . -Dtest="QueryShelfUseCaseTest" -am
```

---

## Commit 2: `c6ebf3c` — Add null/blank validation guard to Shelf#setShelfLabel

### Intent

Push domain validation down into the `Shelf` aggregate. Before this commit, `setShelfLabel` was the only setter on
`Shelf` without a guard clause — every other field (`shelfId`, `bookcaseId`, `shelfPosition`, `bookCapacity`, `books`)
already threw `IllegalArgumentException` on invalid input. This commit closes that gap.

### Files touched

| File | Why |
|------|-----|
| `Shelf.java` | Added 3-line guard clause to `setShelfLabel` |

### Key code changes

```java
public void setShelfLabel(String shelfLabel) {
    if (shelfLabel == null || shelfLabel.isBlank()) {
        throw new IllegalArgumentException("Shelf label cannot be null or blank");
    }
    this.shelfLabel = shelfLabel;
}
```

This is consistent with the pattern already established in the class:

- `setShelfId` → `"Shelf ID cannot be null"`
- `setBookcaseId` → `"Bookcase ID cannot be null"`
- `setShelfPosition` → `"Shelf position must be greater than 0"`
- `setBookCapacity` → `"Book capacity cannot be negative"`
- `setBooks` → `"Books cannot be null"`

Now all six setters enforce invariants. The constructor delegates to setters, so the guard also protects construction.

### Architecture notes

- **Domain model self-validates.** This is textbook DDD — the aggregate root rejects invalid state transitions. No
  upstream caller (use case, adapter, controller) needs to pre-validate the label because the domain model guarantees
  the invariant.
- **Fail-fast semantics.** A null or blank label throws immediately rather than propagating a corrupt `Shelf` into
  persistence and downstream consumers.

### Risk / edge cases

- **Behavioral change:** Any code path that previously set a null or blank label will now throw. This is intentional,
  but could surface latent bugs if upstream code was relying on the permissive behavior.
- **Existing persisted data:** The read path (JPA entity → `ShelfMapper` → `Shelf`) hydrates `Shelf` via the
  constructor, which calls `setShelfLabel`. If any rows have null or blank `shelf_label` in the database, they will
  blow up on read. **Recommendation:** Run a quick DB query to verify no blank labels exist:
  `SELECT * FROM shelf WHERE shelf_label IS NULL OR TRIM(shelf_label) = ''`.
- **Whitespace-only labels:** `isBlank()` rejects `"   "` (whitespace-only), which is correct for a user-visible label.

### Verification

A `ShelfTest` class exists in the working tree (untracked, not yet committed) with three tests covering null, empty,
and blank-whitespace inputs:

```bash
mvn test -pl . -Dtest="ShelfTest" -am
```

---

## Commit 3: `71c8002` — Rename parameter 'string' to 'shelfLabel' in ShelfAccessPort

### Intent

Fix a naming slip in the cross-module port interface. The `ShelfAccessPort.createShelf(...)` method had a parameter
named `string` — almost certainly an IDE auto-generated name from a type-first declaration. Renaming it to `shelfLabel`
makes the interface self-documenting and consistent with the domain vocabulary.

### Files touched

| File | Why |
|------|-----|
| `ShelfAccessPort.java` | Renamed `String string` → `String shelfLabel` in `createShelf` signature |
| `ShelfAccessPortAdapter.java` | Updated `@Override` implementation to match: parameter and delegation call both use `shelfLabel` |

### Key code changes

```java
// Before (ShelfAccessPort)
void createShelf(Long bookcaseId, int position, String string, int bookCapacity);

// After
void createShelf(Long bookcaseId, int position, String shelfLabel, int bookCapacity);
```

```java
// Before (ShelfAccessPortAdapter)
public void createShelf(Long bookcaseId, int position, String string, int bookCapacity) {
    shelfFacade.createShelfInBookcaseByBookcaseId(bookcaseId, position, string, bookCapacity);
}

// After
public void createShelf(Long bookcaseId, int position, String shelfLabel, int bookCapacity) {
    shelfFacade.createShelfInBookcaseByBookcaseId(bookcaseId, position, shelfLabel, bookCapacity);
}
```

### Architecture notes

- **Cross-module port boundary.** `ShelfAccessPort` lives in `bookcase.core.ports.outbound` — it is the bookcase
  module's outbound port that the shelf module's adapter implements. Clean parameter naming here is especially
  important because this interface is the contract between two bounded contexts.
- **No dependency direction change.** The bookcase module still depends on the port interface it owns; the shelf
  module's adapter still implements it. The coupling direction is unchanged.

### Risk / edge cases

- Pure rename, no behavioral change. Binary-compatible at the JVM level (parameter names are not part of the method
  signature in bytecode, only in debug info).
- Any Javadoc or external documentation referencing the old `string` parameter name should be updated.

### Verification

```bash
mvn test -pl . -Dtest="ShelfAccessPortAdapterTest" -am
```

---

## End-to-End Flow Analysis

These three commits don't change the runtime flow — they are structural and defensive improvements. But here is how the
affected components participate in the existing shelf-creation flow:

1. **Request** → `BookcaseCommands` (CLI) or controller calls `BookcaseService`
2. **Bookcase use case** calls `ShelfAccessPort.createShelf(bookcaseId, position, shelfLabel, bookCapacity)` — now with
   a properly named `shelfLabel` parameter (Commit 3)
3. **ShelfAccessPortAdapter** delegates to `ShelfFacade.createShelfInBookcaseByBookcaseId(...)`
4. **CreateShelfUseCase** builds a `Shelf` domain object — the constructor calls `setShelfLabel(shelfLabel)`, which
   now validates against null/blank (Commit 2)
5. **Shelf** is persisted via `ShelfDomainRepositoryPort`
6. On the query side, `QueryShelfUseCase` maps `Shelf` → `ShelfResponse` using `ShelfPortModelMapper`, now located in
   `inboundPortModels/mapper/` (Commit 1)

### Why this design is better than before

- **Consistency.** Every `Shelf` setter now guards its invariant. The domain model is fully self-validating — no
  half-guarded state.
- **Discoverability.** The mapper lives next to the models it produces. A developer exploring `inboundPortModels/`
  finds everything in one place: the records and the factory that builds them.
- **Readability.** `String shelfLabel` in a port interface is immediately clear; `String string` was noise.

### Dependencies removed or added

None. No new library dependencies. No new Spring beans. No configuration changes.

### Import layering check

All imports are clean:
- `ShelfPortModelMapper` imports from `core.domain.model` (inward) and `core.ports.inbound.inboundPortModels` (sibling)
  — both allowed.
- `QueryShelfUseCase` imports from `core.ports.inbound.inboundPortModels.mapper` — same hexagonal layer, allowed.
- `ShelfAccessPort` has no imports (primitive + `String` parameters only).
- `ShelfAccessPortAdapter` imports from `bookcase.core.ports.outbound` and `shelf.core.ports.inbound` — cross-module
  via the port boundary, which is the intended coupling point.

---

## Test Coverage

### Existing tests

- `CreateShelfUseCaseTest` and `QueryShelfUseCaseTest` cover the use case layer and were not changed in this branch.
- These tests should continue to pass with the mapper relocation (Commit 1) since they import use cases, not the
  mapper directly.

### New test (untracked)

`ShelfTest.java` exists in the working tree at:
`src/test/java/com/penrose/bibby/library/stacks/shelf/core/domain/model/ShelfTest.java`

It covers three cases for the new `setShelfLabel` guard:
- `setShelfLabel_shouldThrowWhenNull` — null input
- `setShelfLabel_shouldThrowWhenEmpty` — empty string `""`
- `setShelfLabel_shouldThrowWhenBlank` — whitespace-only `"   "`

### Suggested additional test cases

1. **`setShelfLabel_shouldAcceptValidLabel`** — positive case: verify that a valid label like `"Shelf A"` is accepted
   and stored correctly.
2. **`constructor_shouldThrowWhenLabelIsNull`** — verify the guard fires during construction, not just via the setter
   directly.
3. **`constructor_shouldThrowWhenLabelIsBlank`** — same as above but with blank input.
4. **`ShelfPortModelMapper_shouldMapAllFieldsCorrectly`** — unit test for the mapper to verify all 6 fields are
   mapped from `Shelf` → `ShelfResponse`. This is especially valuable now that the mapper moved packages.
5. **`ShelfAccessPortAdapter_shouldDelegateCreateShelf`** — verify the adapter passes `shelfLabel` through to the
   facade (may already exist; if not, it's a good micro-integration test).

### Run all shelf tests

```bash
mvn test -pl . -Dtest="ShelfTest,CreateShelfUseCaseTest,QueryShelfUseCaseTest" -am
```

---

## Lessons Learned

1. **Close validation gaps incrementally.** The `Shelf` model had guards on 5 of 6 setters. It's easy to assume the
   missing one is fine — it wasn't. A quick audit of all setters during a hardening pass catches these.
2. **Co-locate mappers with their output types.** Having `ShelfPortModelMapper` in a separate `mapper/` package at the
   same level as `inboundPortModels/` created a subtle discoverability problem. Package-by-feature > package-by-role
   at the leaf level.
3. **IDE-generated parameter names leak.** `String string` was almost certainly auto-generated. Reviewing port/interface
   signatures specifically — not just implementations — catches these before they calcify into the API contract.
4. **Small branches merge cleanly.** Three focused commits, 5 files, +8/−5 lines. This branch was trivially
   reviewable and merged without conflict. Compare with the 20-file port model migration PRs that preceded it.
5. **Domain validation guards protect the read path too.** Because `Shelf`'s constructor delegates to `setShelfLabel`,
   the guard also fires when hydrating from the database. This means any corrupt persisted data will surface as an
   exception rather than silently propagating — which is the right trade-off for a domain invariant.

---

## Next Steps

### Immediate follow-ups (today)

- [ ] Commit and push `ShelfTest.java` — it's sitting untracked in the working tree
- [ ] Delete the now-empty `ports/inbound/mapper/` package (if the IDE hasn't already)
- [ ] Verify no blank `shelf_label` rows exist in the database before the guard hits production reads

### Short-term hardening (this week)

- [ ] Add positive-case test for `setShelfLabel` (valid label accepted)
- [ ] Add constructor-level tests for the label guard (null and blank via constructor)
- [ ] Add a `ShelfPortModelMapper` unit test now that it has its own package
- [ ] Audit other domain models (`Bookcase`, `Book`) for missing setter guards — apply the same pattern

### Strategic refactors (later)

- [ ] Consider replacing the `setShelfLabel` setter with an immutable builder or `with`-style copy method to prevent
  mutation after construction
- [ ] Evaluate whether `ShelfAccessPort` should use a command object (`CreateShelfCommand`) instead of 4 primitive
  parameters — this would make the cross-module contract richer and more evolvable
- [ ] Investigate ArchUnit rules that enforce "all domain model setters must have guard clauses" to prevent regression
