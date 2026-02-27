# Devlog: Complete Shelf Inbound Port Model Migration

**Date:** 2026-02-25
**Time:** 10:18 CST
**Branch:** `refactor/shelf-findall-response-and-docs`
**Range:** `origin/main..HEAD`
**Commits:**

| SHA       | Subject                                     |
|-----------|---------------------------------------------|
| `5a82ef9` | Complete shelf inbound port model migration |
| `5793f41` | Document shelf inbound port model migration |

---

## TL;DR

- **What problem was I solving?** The `ShelfFacade` inbound port still leaked domain types (`Shelf`, `ShelfSummary`)
  across the hexagonal boundary. Two methods — `findShelfById` and `getShelfSummariesForBookcaseByBookcaseId` — returned
  raw domain objects, and `ShelfResponse` was missing `bookcaseId`, forcing consumers to reach back through the domain
  to get it.
- **What was the "before" state?** After PR #325, `findShelvesByBookcaseId` and `findAll` already returned
  `ShelfResponse`, but `findShelfById` still returned `Optional<Shelf>`, `getShelfSummariesForBookcaseByBookcaseId`
  still returned `List<ShelfSummary>` (a record living in the domain layer), and 7 CLI command classes imported `Shelf`
  directly.
- **Most important outcomes:**
    - `ShelfFacade` imports **zero** domain types — the boundary is fully sealed
    - `ShelfSummary` renamed to `ShelfSummaryResponse` and relocated from `core.domain.model` to
      `core.ports.inbound.inboundPortModels`
    - `ShelfResponse` extended with `bookcaseId` field
    - `findShelfById` returns `Optional<ShelfResponse>` — mapping happens inside `QueryShelfUseCase`
    - All 7 CLI command classes + `ShelfAccessPortAdapter` updated from getter-style calls to record accessors
    - All 3 affected test classes updated; 142/142 tests pass

---

## Commit 1: `5a82ef9` — Complete shelf inbound port model migration

### Intent

Seal the `ShelfFacade` inbound port boundary so that no domain type (`Shelf`, `ShelfSummary`, `ShelfId`) is visible to
any consumer outside the shelf core. This is the "last mile" of the port model migration started in PRs #324 and #325.

### Files touched (20 files, +215 −105)

| File                            | Why                                                                                                                                                                                                                          |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ShelfResponse.java`            | Added `bookcaseId` field (6th record component)                                                                                                                                                                              |
| `ShelfSummaryResponse.java`     | **New.** Replaces `ShelfSummary` — renamed and relocated to `inboundPortModels` package                                                                                                                                      |
| `ShelfSummary.java`             | **Deleted.** Was a domain-layer record that didn't belong there                                                                                                                                                              |
| `ShelfFacade.java`              | Changed `findShelfById` return from `Optional<Shelf>` → `Optional<ShelfResponse>`, `getShelfSummaries…` from `List<ShelfSummary>` → `List<ShelfSummaryResponse>`, removed `Shelf`/`ShelfSummary` imports, added full Javadoc |
| `QueryShelfUseCase.java`        | `findShelfById` now maps `Shelf` → `ShelfResponse` via `ShelfPortModelMapper`; `getShelfSummariesForBookcaseById` returns `ShelfSummaryResponse`; added class-level and method-level Javadoc                                 |
| `ShelfService.java`             | Aligned signatures with updated `ShelfFacade`; replaced inline Javadoc with `{@inheritDoc}`; added class/constructor Javadoc                                                                                                 |
| `ShelfPortModelMapper.java`     | Added `shelf.getBookcaseId()` as 6th argument to `ShelfResponse` constructor                                                                                                                                                 |
| `Shelf.java`                    | Moved `bookcaseId` field declaration up (before getter/setter), purely cosmetic                                                                                                                                              |
| `ShelfMapper.java`              | `toSummaryFromEntity` now returns `ShelfSummaryResponse` instead of `ShelfSummary`                                                                                                                                           |
| `ShelfAccessPortAdapter.java`   | Switched from getter-style (`shelf.getId()`) to record accessors (`shelf.id()`)                                                                                                                                              |
| `BookCirculationCommands.java`  | `Optional<Shelf>` → `Optional<ShelfResponse>`, record accessors (2 locations: checkout + checkin)                                                                                                                            |
| `BookSearchCommands.java`       | `Optional<Shelf>` → `Optional<ShelfResponse>`, record accessors (2 locations: searchByIsbn + searchByTitle)                                                                                                                  |
| `BookPlacementCommands.java`    | Record accessors on `ShelfResponse` when building `ShelfDTO` for placement                                                                                                                                                   |
| `BookCreateIsbnCommands.java`   | `.getShelfLabel()` → `.shelfLabel()`                                                                                                                                                                                         |
| `BookCreateImportCommands.java` | `.getShelfLabel()` → `.shelfLabel()`                                                                                                                                                                                         |
| `LibraryCommands.java`          | `.getShelfLabel()` → `.shelfLabel()`                                                                                                                                                                                         |
| `BookcaseCommands.java`         | `ShelfSummary` → `ShelfSummaryResponse` in `selectShelf()`                                                                                                                                                                   |
| `QueryShelfUseCaseTest.java`    | `findShelfById` tests now assert on `ShelfResponse` fields; added `getBookcaseId()` stubs to all mocked `Shelf` objects; `ShelfSummary` → `ShelfSummaryResponse`                                                             |
| `ShelfServiceTest.java`         | `findShelfById` test uses `ShelfResponse` instead of mocked `Shelf`; `ShelfSummary` → `ShelfSummaryResponse`; all `ShelfResponse` constructors updated with `bookcaseId`                                                     |
| `PromptOptionsTest.java`        | All `ShelfResponse` constructors updated with 6th `bookcaseId` argument                                                                                                                                                      |

### Key code changes

**`ShelfFacade.java` — the central contract change:**

Before:

```java
Optional<Shelf> findShelfById(Long shelfId);

List<ShelfSummary> getShelfSummariesForBookcaseByBookcaseId(Long bookcaseId);
```

After:

```java
Optional<ShelfResponse> findShelfById(Long shelfId);

List<ShelfSummaryResponse> getShelfSummariesForBookcaseByBookcaseId(Long bookcaseId);
```

The imports section now contains **only** port model types. `Shelf` and `ShelfSummary` are gone.

**`QueryShelfUseCase.findShelfById` — behavior change:**

Before — returned the raw domain entity:

```java
public Optional<Shelf> findShelfById(Long shelfId) {
    Shelf shelf = shelfDomainRepositoryPort.getShelfByShelfId(new ShelfId(shelfId));
    if (shelf == null) {
        return Optional.empty();
    }
    return Optional.of(shelf);
}
```

After — maps through `ShelfPortModelMapper` before returning:

```java
public Optional<ShelfResponse> findShelfById(Long shelfId) {
    Shelf shelf = shelfDomainRepositoryPort.getShelfByShelfId(new ShelfId(shelfId));
    if (shelf == null) {
        return Optional.empty();
    }
    return Optional.of(ShelfPortModelMapper.toShelfResponse(shelf));
}
```

The `@Transactional` annotation was already present (for lazy-loaded book IDs), so the mapping happens within the
session boundary.

**`ShelfResponse.java` — record extension:**

```java
// Before: 5 components
public record ShelfResponse(
                Long id, int shelfPosition, String shelfLabel,
                int bookCapacity, List<Long> books) {
}

// After: 6 components
public record ShelfResponse(
        Long id, int shelfPosition, String shelfLabel,
        int bookCapacity, List<Long> books, Long bookcaseId) {
}
```

This is a breaking change to the record constructor. Every call site — production and test — was updated in this commit.

**`ShelfSummary` → `ShelfSummaryResponse` relocation:**

- Old location: `core.domain.model.ShelfSummary` — a record sitting in the domain layer that was really a read
  projection for the port
- New location: `core.ports.inbound.inboundPortModels.ShelfSummaryResponse` — correctly placed as a port model

The rename follows the `*Response` naming convention established by `ShelfResponse`.

### Architecture notes

**Dependency direction — before and after:**

```
BEFORE:
  CLI commands ──import──→ shelf.core.domain.model.Shelf
  CLI commands ──import──→ shelf.core.domain.model.ShelfSummary
  ShelfFacade  ──import──→ shelf.core.domain.model.Shelf
  ShelfFacade  ──import──→ shelf.core.domain.model.ShelfSummary

AFTER:
  CLI commands ──import──→ shelf.core.ports.inbound.inboundPortModels.ShelfResponse
  CLI commands ──import──→ shelf.core.ports.inbound.inboundPortModels.ShelfSummaryResponse
  ShelfFacade  ──import──→ shelf.core.ports.inbound.inboundPortModels.ShelfResponse
  ShelfFacade  ──import──→ shelf.core.ports.inbound.inboundPortModels.ShelfSummaryResponse
```

No external adapter or CLI command can reach `Shelf` (the domain aggregate) or `ShelfId` (the value object) anymore. The
domain is fully encapsulated behind the inbound port.

**Layering violations removed:**

- `BookCirculationCommands` imported `shelf.core.domain.model.Shelf` — **removed**
- `BookSearchCommands` imported `shelf.core.domain.model.Shelf` — **removed**
- `BookcaseCommands` imported `shelf.core.domain.model.ShelfSummary` — **removed**

**One remaining cross-layer note (inference):** `ShelfMapper` in the infrastructure layer now imports
`ShelfSummaryResponse` from the inbound port models package. This is acceptable because infrastructure adapters *are*
allowed to depend on port contracts. However, the `toSummaryFromEntity` method on `ShelfMapper` doesn't appear to be
called from any current production path (the summary is built inline in
`QueryShelfUseCase.getShelfSummariesForBookcaseById`). This method may be dead code.

### Risk / edge cases

- **Breaking record constructor:** Adding `bookcaseId` as the 6th field on `ShelfResponse` breaks binary compatibility.
  Since this is an internal port model (not serialized over the wire), this is safe. All call sites were updated
  atomically in this commit.
- **`bookcaseId` could be `null`:** The `Shelf` domain entity stores `bookcaseId` as `Long` (nullable). If a `Shelf` is
  constructed without a bookcase ID, `ShelfResponse.bookcaseId()` will return `null`. Consumers like
  `BookCirculationCommands` call `shelf.get().bookcaseId()` and pass it directly to
  `bookcaseFacade.findBookCaseById(...)`. A null `bookcaseId` would propagate as a null lookup — this existed before the
  refactor too, but it's worth noting.
- **`.get()` on `Optional` without `isPresent` check:** Several CLI commands (e.g., `BookCreateIsbnCommands:125`,
  `LibraryCommands:104`) call `shelfFacade.findShelfById(shelfId).get()` without checking presence. This is
  pre-existing, but now that the mapping happens inside the use case (including a database hit), a missing shelf would
  throw `NoSuchElementException`. Not introduced by this change, but exposed by the new flow.

### Verification

```bash
mvn test                              # Full suite: 142/142 pass
mvn test -Dtest=QueryShelfUseCaseTest  # 9 tests — core mapping logic
mvn test -Dtest=ShelfServiceTest       # 6 tests — delegation verification
mvn test -Dtest=PromptOptionsTest      # 7 tests — CLI prompt integration
```

---

## Commit 2: `5793f41` — Document shelf inbound port model migration

### Intent

Add decision records and devlogs capturing the architectural rationale for the port model migration.

### Files touched (3 files, +727 −122)

| File                                                                | Why                                                                            |
|---------------------------------------------------------------------|--------------------------------------------------------------------------------|
| `ADR-0005-return-port-model-from-shelf-facade-findall.md`           | **New.** Architecture Decision Record for the `findAll` port model return type |
| `devlog-2026-02-25-Shelf-FindAll-Port-Model-Migration.md`           | **New.** Detailed devlog for the findAll migration specifically                |
| `devlog-2026-02-19-ShelfFacade-Domain-Boundary-Mapping-Refactor.md` | Updated with expanded content reflecting the completed boundary work           |

### Architecture notes

Documentation-only commit. No behavior change, no risk.

### Verification

No tests needed. Markdown files only.

---

## End-to-End Flow Walkthrough

Here is how a shelf query flows through the system after this change, using `findShelfById` as the example since it
changed the most:

```
CLI Command (e.g., BookSearchCommands)
  │
  │  calls shelfFacade.findShelfById(shelfId)
  │  receives Optional<ShelfResponse>        ← port model, not domain
  ▼
ShelfFacade (inbound port interface)
  │
  │  implemented by ShelfService
  ▼
ShelfService.findShelfById(shelfId)
  │
  │  delegates to queryShelfUseCase.findShelfById(shelfId)
  ▼
QueryShelfUseCase.findShelfById(shelfId)        ← @Transactional
  │
  │  calls shelfDomainRepositoryPort.getShelfByShelfId(new ShelfId(shelfId))
  │  receives Shelf (domain entity)              ← domain stays here
  │
  │  maps via ShelfPortModelMapper.toShelfResponse(shelf)
  │  returns Optional<ShelfResponse>             ← port model crosses boundary
  ▼
ShelfDomainRepositoryPort (outbound port)
  │
  │  implemented by JPA adapter (infrastructure)
  ▼
Database
```

**Key insight:** The `Shelf` domain aggregate is retrieved from persistence, used for mapping inside the
`@Transactional` boundary (so lazy-loaded `books` are available), and then converted to `ShelfResponse` before leaving
the use case. No domain type escapes `QueryShelfUseCase`.

---

## Why This Design Is Better

1. **Sealed boundary.** Before: CLI commands imported `shelf.core.domain.model.Shelf` — a direct dependency on domain
   internals. After: they only see `ShelfResponse`/`ShelfSummaryResponse`. The domain model can evolve freely (rename
   fields, add behavior, restructure) without breaking any consumer.

2. **Consistent port model convention.** All eight methods on `ShelfFacade` now follow the same pattern: commands accept
   primitives, queries return port model records. No method returns a domain type.

3. **`bookcaseId` is self-contained.** Consumers no longer need to know that `bookcaseId` lives on the `Shelf` domain
   entity. `ShelfResponse` carries it, eliminating the need for a second domain lookup or an awkward chain of accessors.

4. **Record accessors are idiomatic.** Replacing `.getShelfLabel()` with `.shelfLabel()` across 7 CLI files isn't just
   cosmetic — it removes the illusion that `ShelfResponse` is a mutable JavaBean. The record accessor style signals
   immutability to the reader.

---

## Dependencies Removed and Added

**Removed (imports that no longer exist):**

- `BookCirculationCommands` → `shelf.core.domain.model.Shelf` ✕
- `BookSearchCommands` → `shelf.core.domain.model.Shelf` ✕
- `BookcaseCommands` → `shelf.core.domain.model.ShelfSummary` ✕
- `ShelfService` → `shelf.core.domain.model.Shelf` ✕
- `ShelfService` → `shelf.core.domain.model.ShelfSummary` ✕
- `QueryShelfUseCase` → `shelf.core.domain.model.ShelfSummary` ✕
- `ShelfServiceTest` → `shelf.core.domain.model.Shelf` ✕
- `ShelfServiceTest` → `shelf.core.domain.model.ShelfSummary` ✕
- `QueryShelfUseCaseTest` → `shelf.core.domain.model.ShelfSummary` ✕
- `ShelfMapper` → `shelf.core.domain.model.ShelfSummary` ✕

**Added:**

- `BookCirculationCommands` → `shelf.core.ports.inbound.inboundPortModels.ShelfResponse`
- `BookSearchCommands` → `shelf.core.ports.inbound.inboundPortModels.ShelfResponse`
- `BookcaseCommands` → `shelf.core.ports.inbound.inboundPortModels.ShelfSummaryResponse`
- `ShelfService` → `shelf.core.ports.inbound.inboundPortModels.ShelfSummaryResponse`
- `QueryShelfUseCase` → `shelf.core.ports.inbound.inboundPortModels.ShelfSummaryResponse`
- `ShelfMapper` → `shelf.core.ports.inbound.inboundPortModels.ShelfSummaryResponse`

**Net effect:** Every removed import pointed into `core.domain.model`. Every added import points into
`core.ports.inbound.inboundPortModels`. Dependency arrows now correctly point inward (adapters → ports), never from
adapters into domain internals.

---

## Import Layering Audit

After this change, the `ShelfFacade` import block is:

```java
import ...ports.inbound.inboundPortModels.ShelfResponse;
import ...ports.inbound.inboundPortModels.ShelfSummaryResponse;
import java.util.List;
import java.util.Optional;
```

No domain imports. No infrastructure imports. Clean.

**One item to watch:** `ShelfMapper` (infrastructure layer) now imports `ShelfSummaryResponse` from the port model
package. This follows hexagonal rules (infra may depend on ports), but `ShelfMapper.toSummaryFromEntity()` appears
unused — the summary mapping is done inline in `QueryShelfUseCase`. Consider removing the dead method.

---

## Test Coverage

### Tests that changed

| Test class              | # Tests | What changed                                                                                                                                                               |
|-------------------------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `QueryShelfUseCaseTest` | 9       | `findShelfById` tests assert on `ShelfResponse` fields; `getBookcaseId()` stubs added to all mocked `Shelf` objects; `ShelfSummary` → `ShelfSummaryResponse`               |
| `ShelfServiceTest`      | 6       | `findShelfById` test uses `ShelfResponse` record instead of mocked `Shelf`; `ShelfSummary` → `ShelfSummaryResponse`; all `ShelfResponse` constructors include `bookcaseId` |
| `PromptOptionsTest`     | 7       | `ShelfResponse` constructors updated with 6th `bookcaseId` argument                                                                                                        |

### Suggested additional test cases

1. **`QueryShelfUseCaseTest`: `findShelfById` should include `bookcaseId` in mapped response** — Stub
   `shelf.getBookcaseId()` to return `200L`, assert `result.get().bookcaseId()` equals `200L`. Currently the test only
   checks `id()` and `shelfLabel()`.

2. **`QueryShelfUseCaseTest`: `getShelfSummariesForBookcaseById` verify delegation** — Add a
   `verify(shelfDomainRepositoryPort).findByBookcaseId(bookcaseId)` to confirm the repository is called. The existing
   test asserts on return values but doesn't verify the interaction.

3. **`ShelfPortModelMapperTest` (new class)**: Unit-test `toShelfResponse` in isolation — pass a `Shelf` with known
   field values (including `bookcaseId`) and assert all 6 fields on the resulting `ShelfResponse`. This mapper is the
   single translation point for the entire boundary.

4. **`ShelfAccessPortAdapterTest`: verify `findShelfById` maps to `ShelfDTO` correctly** — Mock
   `shelfFacade.findShelfById(1L)` to return a `ShelfResponse` with all fields populated, assert the resulting
   `ShelfDTO` has matching values.

5. **Edge case: `findShelfById` when shelf has null `bookcaseId`** — Verify that `ShelfResponse.bookcaseId()` returns
   `null` gracefully and downstream consumers handle it (or that it's an invariant violation that should throw).

### Verification commands

```bash
mvn test                                           # Full suite
mvn test -Dtest=QueryShelfUseCaseTest              # Core use case mapping
mvn test -Dtest=ShelfServiceTest                   # Facade delegation
mvn test -Dtest=PromptOptionsTest                  # CLI prompt integration
mvn test -Dtest="QueryShelfUseCaseTest,ShelfServiceTest,PromptOptionsTest"  # All three
```

---

## Lessons From This Diff

1. **Record constructor changes are all-or-nothing.** Adding `bookcaseId` to `ShelfResponse` forced updates across 20
   files. In a hexagonal architecture, port model records are the "API surface" — changing them has the same blast
   radius as changing a REST DTO. Keep port model records stable once consumers exist, or accept coordinated changes.

2. **One leaked domain type creates a dependency chain.** `findShelfById` returning `Optional<Shelf>` meant every CLI
   command that called it needed `import Shelf`. Fixing one method signature eliminated 4 import violations across the
   consumer layer.

3. **`ShelfSummary` was misplaced from birth.** It was a record in `core.domain.model` but had no domain behavior — it
   was a read projection for the UI. The rename to `ShelfSummaryResponse` and move to `inboundPortModels` corrects the
   layering. Lesson: if a type exists only to carry data *out* of the boundary, it's a port model, not a domain model.

4. **Getter-style calls on records are a code smell.** Seeing `.getShelfLabel()` on a `ShelfResponse` record means the
   consumer was written expecting a JavaBean, not a record. The migration from `.getX()` to `.x()` across 7 CLI files
   was mechanical but revealed that the consumers hadn't been updated when the type changed from entity to record.

5. **`@Transactional` placement matters for mapping.** `QueryShelfUseCase.findShelfById` is `@Transactional` because
   `Shelf.books` may be lazily loaded. The mapping to `ShelfResponse` (which reads `shelf.getBookIds()`) must happen
   *inside* the transaction. Moving the mapping earlier (e.g., into `ShelfService`) would risk
   `LazyInitializationException`.

6. **Tests are the ground truth for port model shape.** `ShelfServiceTest` no longer mocks `Shelf` for `findShelfById` —
   it constructs `ShelfResponse` directly. This is the correct pattern: the service test verifies delegation across the
   port boundary, so it should speak in port model types, not domain types.

7. **Dead code detection via the diff.** `ShelfMapper.toSummaryFromEntity()` was updated to return
   `ShelfSummaryResponse`, but `QueryShelfUseCase.getShelfSummariesForBookcaseById` builds the summary inline without
   using `ShelfMapper`. If `toSummaryFromEntity()` has no other callers, it's dead code — a follow-up cleanup target.

---

## Follow-Up Roadmap

### Immediate (today)

- [ ] Verify `ShelfMapper.toSummaryFromEntity()` has callers — if not, delete it
- [ ] Add `bookcaseId` assertion to `QueryShelfUseCaseTest.findShelfById_shouldReturnShelfResponseWhenFound`

### Short-term hardening (this week)

- [ ] Create `ShelfPortModelMapperTest` — unit-test the mapper in isolation for all 6 fields
- [ ] Audit `BookPlacementCommands` — it still constructs a `ShelfDTO` from `ShelfResponse` manually (lines 55–63);
  consider whether `ShelfResponse` can replace `ShelfDTO` entirely in this flow
- [ ] Add null-safety for `.get()` calls on `Optional<ShelfResponse>` in CLI commands — replace bare `.get()` with
  `.orElseThrow(() -> ...)` for better error messages

### Strategic refactors (later)

- [ ] Apply the same port model pattern to `BookcaseFacade` — it likely still returns domain types across its boundary
- [ ] Consolidate `ShelfDTO` (API layer) and `ShelfResponse` (port model) — they carry nearly identical data; evaluate
  whether a single type suffices or if the API layer needs its own projection
- [ ] Extract `ShelfSummaryResponse` mapping from the inline lambda in
  `QueryShelfUseCase.getShelfSummariesForBookcaseById` into `ShelfPortModelMapper.toShelfSummaryResponse()` for
  consistency with the `ShelfResponse` mapping path
