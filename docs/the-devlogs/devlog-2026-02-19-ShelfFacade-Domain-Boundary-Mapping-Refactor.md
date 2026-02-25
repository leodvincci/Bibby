# Devlog: Purge DTO Mapping from the Core — ShelfFacade Now Speaks Domain

**Date:** February 19, 2026 (America/Chicago)
**Branch:** `refactor/rename-shelf-dto-mapper-method`
**PR:** #272 — merged into `main`
**Range:** `9c92af3..9eeae42`

**Commits (chronological):**
| SHA | Subject |
|---|---|
| `face4fe` | refactor: rename ShelfDTOMapper.toDTO to toDTOFromDomain |
| `b9452ed` | docs: add devlog for shelf mapper layer separation |
| `6db901b` | docs: remove ddd-layers-and-mappers from engineering directory |
| `e732ba1` | refactor: move ShelfSummary to core domain model and update imports |
| `c45f6d9` | refactor: add bookcaseId field to Shelf domain model |
| `92c6f5f` | refactor: decouple ShelfFacade and ShelfService from DTO mapping; delete ShelfDTOMapper |
| `30e20ce` | fix: update ShelfJpaRepository JPQL to reference ShelfSummary in core domain package |
| `2b445a0` | refactor: move Shelf-to-DTO mapping to web and adapter boundary layers |
| `2c2a880` | refactor: update CLI book commands to use Shelf domain model from ShelfFacade |
| `a38f6a8` | refactor: update PromptOptions to use ShelfFacade.findAllShelves and bookFacade.findByShelfId |
| `4751b37` | test: update ShelfServiceTest to reflect ShelfService no longer depending on ShelfDTOMapper |
| `8238dc3` | test: add PromptOptionsTest for bookcase option menu rendering |

---

## Context

### What problem was I solving?

The previous PR (#271) introduced `ShelfDTOMapper` as a Spring-managed component in the **core application layer** (
`library.stacks.shelf.core.mappers`). The idea was to give the application service a single, named place to translate
`Shelf → ShelfDTO`. Reasonable instinct, but the placement violated the dependency rule: the core should not know about
DTOs, API contracts, or presentation concerns at all.

The symptoms were clear in `ShelfService`:

- Constructor injected `ShelfDTOMapper` — a presentation-concern dependency in the heart of the domain
- Every query method (`getAllShelves`, `findShelfById`, `findByBookcaseId`) returned `ShelfDTO` or
  `ShelfOptionResponse` — API types, not domain types
- `ShelfFacade` (the inbound port) imported `BookDTO` from the cataloging module's API package — a cross-module API
  coupling at the port interface level
- `ShelfFacade` declared `findBooksByShelf(Long)` — a query that has nothing to do with the shelf port; it belongs on
  `BookFacade`
- `ShelfFacade` declared `isFull(ShelfDTO)` — accepted a DTO as input to a core port, which is backwards
- `ShelfSummary` lived in `api.dtos` even though it is a domain read-model, not an HTTP response type

### What was the "before" state?

```
ShelfFacade (port/inbound)
  ├── getAllDTOShelves(Long) → List<ShelfDTO>        ← DTO in port signature
  ├── findShelfById(Long) → Optional<ShelfDTO>       ← DTO in port signature
  ├── findByBookcaseId(Long) → List<ShelfDTO>        ← DTO in port signature
  ├── findBooksByShelf(Long) → List<BookDTO>         ← wrong module's concern
  └── isFull(ShelfDTO) → Boolean                    ← DTO as input to core port

ShelfService (application, implements ShelfFacade)
  ├── @Inject ShelfDTOMapper                        ← presentation mapper in core
  ├── getAllShelves → stream → ShelfDTOMapper.toDTOFromDomain(...)
  └── getShelfOptions → stream → ShelfDTOMapper.toShelfOption(...)

ShelfSummary
  └── location: api.dtos                            ← domain concept in API package
```

---

## High-level Summary (The 60-second version)

- **`ShelfFacade` is now a clean domain port.** It returns `Shelf` and `ShelfSummary` — no DTOs, no cross-module API
  types, no mis-assigned responsibilities.
- **`ShelfDTOMapper` was deleted from core.** The 40-line mapper class is gone; mapping responsibility lives at the
  boundaries that actually need it.
- **`ShelfSummary` moved** from `api.dtos` → `core.domain.model`. It was always a domain read-model; now it lives where
  it belongs.
- **`Shelf` got a `bookcaseId` field.** The domain model is now self-contained, eliminating a secondary
  `getBookcaseIdByShelfId(Long)` repository call that every caller was paying for.
- **DTO mapping pushed to four boundary points:** the web controller layer, the anti-corruption adapter, and two
  CLI-layer classes. Each maps `Shelf → ShelfDTO` (or `→ ShelfOptionResponse`) independently.
- **`ShelfResponseMapper`** was added as a dedicated web-layer Spring component for the `ShelfController` endpoints.
- **`findBooksByShelf` and `isFull(ShelfDTO)` removed** from `ShelfFacade`. Neither belongs on the shelf port.
- **136 tests pass, 0 failures.**
- **7 new `PromptOptionsTest` cases** added to cover the previously untested `bookCaseOptions()` menu builder.

---

## Commit-by-commit Breakdown

### refactor: rename ShelfDTOMapper.toDTO to toDTOFromDomain (`face4fe`)

**Intent:** Naming clarity. `toDTO` is ambiguous — it doesn't communicate what the *input* is. The rename mirrors the
established `toDomainFromEntity` / `toDomainFromDTO` direction-naming convention already in the codebase.

**Files touched:**

- `ShelfDTOMapper.java` — rename `toDTO(Shelf, Long)` → `toDTOFromDomain(Shelf, Long)`
- `ShelfService.java` — 3 call-site updates
- `ShelfServiceTest.java` — 7 `when()` / `verify()` references updated

**Key code changes:** Pure rename; zero behavior change. The mapper's method now self-documents the data direction:
*from* domain, *to* DTO.

**Architecture notes:** At this point `ShelfDTOMapper` still lives in core — this commit is just a prep step that makes
the impending deletion easier to understand in history.

**Risk:** None. The rename is mechanical.

**Verification:**

```
mvn -q -DskipTests compile
```

---

### docs: add devlog for shelf mapper layer separation (`b9452ed`) + docs: remove ddd-layers-and-mappers (`6db901b`)

**Intent:** Document the reasoning in-flight. The engineering reference doc `ddd-layers-and-mappers.md` was superseded
by the work being done and removed to avoid contradicting the emerging pattern. A devlog capturing the decision
rationale was added in its place.

**Files touched:** Files under `docs/engineering/` and `docs/the-devlogs/`.

**Risk:** Docs-only; no runtime impact.

---

### refactor: move ShelfSummary to core domain model and update imports (`e732ba1`)

**Intent:** `ShelfSummary` is a lightweight projection used to display shelf name + book count in the bookcase browser.
That is domain knowledge, not an HTTP response contract. It should not live in `api.dtos`.

**Files touched:**

- `ShelfSummary.java` — moved and repackaged: `api.dtos` → `core.domain.model`
- `ShelfService.java`, `ShelfFacade.java`, `ShelfMapper.java`, `ShelfJpaRepository.java` — import updates
- `BookcaseCommands.java` (CLI) — import update

**Key code changes:** Package declaration change only. The record definition is unchanged:
`record ShelfSummary(Long shelfId, String label, long bookCount)`.

**Architecture notes:** This move means the inbound port `ShelfFacade` no longer imports anything from `api.dtos` for
its `getShelfSummariesForBookcase` method — a clean step toward a fully API-free port. Note: `ShelfJpaRepository` uses a
JPQL `SELECT NEW` projection against this class; the package in the JPQL string must match the Java package (corrected
in commit `30e20ce`).

**Risk:** JPQL string-based projection references are not caught at compile time — this introduced a latent runtime
error that was fixed in the very next meaningful commit.

---

### refactor: add bookcaseId field to Shelf domain model (`c45f6d9`)

**Intent:** The `Shelf` domain object previously had no knowledge of which bookcase it belonged to. Any code that needed
`bookcaseId` had to make a second repository call: `shelfDomainRepositoryPort.getBookcaseIdByShelfId(shelfId)`. This
forced the application service to keep extra state and bloated every mapping site.

**Files touched:**

- `Shelf.java` — add `private Long bookcaseId` field, getter, setter, constructor parameter
- `ShelfMapper.java` — pass `shelfEntity.getBookcaseId()` as the new constructor arg in `toDomainFromEntity`

**Key code changes:**

```java
// Before
public Shelf(String shelfLabel, int shelfPosition, int bookCapacity,
             ShelfId shelfId, List<Long> bookIds) { ... }

// After
public Shelf(String shelfLabel, int shelfPosition, int bookCapacity,
             ShelfId shelfId, List<Long> bookIds, Long bookcaseId) { ... }
```

**Architecture notes:** This enriches the domain model, making `Shelf` more self-contained. It removes the N+1-style
secondary lookup from `getAllShelves` (which was calling `getBookcaseIdByShelfId` per shelf in a stream). The
infrastructure mapper populates it from the entity at load time — one query path instead of two.

**Risk:** Any code constructing a `Shelf` directly (e.g., tests using `new Shelf(...)`) must be updated to pass the new
parameter. All construction sites were updated in this or subsequent commits. Tests that construct real `Shelf`
objects (e.g., `PromptOptionsTest`) now pass `bookcaseId` as the 6th argument.

---

### refactor: decouple ShelfFacade and ShelfService from DTO mapping; delete ShelfDTOMapper (`92c6f5f`)

**Intent:** The central architectural correction of this PR. The core layer is stripped of all DTO/API awareness. This
is the commit where `ShelfDTOMapper.java` is permanently deleted (40 lines, delete mode).

**Files touched:**

- `ShelfService.java` — massive simplification (−70 lines, +34 lines)
- `ShelfDTOMapper.java` — deleted
- `ShelfFacade.java` — interface cleaned to domain-only signatures

**Key code changes — ShelfService before:**

```java
// Required 3 constructor args including the mapper
public ShelfService(ShelfDomainRepository repo, BookFacade bookFacade, ShelfDTOMapper mapper) { ... }

public List<ShelfDTO> getAllShelves(Long bookCaseId) {
  return shelfDomainRepositoryPort.findByBookcaseId(bookCaseId).stream()
      .map(shelf -> {
        Long bookcaseId = shelfDomainRepositoryPort.getBookcaseIdByShelfId(shelf.getShelfId().shelfId());
        return shelfDTOMapper.toDTOFromDomain(shelf, bookcaseId);
      }).collect(Collectors.toList());
}
```

**After:**

```java
public ShelfService(ShelfDomainRepository repo, BookFacade bookFacade) { ... }

public List<Shelf> getAllShelves(Long bookCaseId) {
  return shelfDomainRepositoryPort.findByBookcaseId(bookCaseId);
}
```

The service is now a thin delegation layer. No mapping. No secondary repository calls. No DTO imports.

**ShelfFacade before/after comparison:**

| Method                         | Before                     | After                                     |
|--------------------------------|----------------------------|-------------------------------------------|
| `getAllDTOShelves`             | `List<ShelfDTO>`           | renamed → `findAllShelves`: `List<Shelf>` |
| `findShelfById`                | `Optional<ShelfDTO>`       | `Optional<Shelf>`                         |
| `findByBookcaseId`             | `List<ShelfDTO>`           | `List<Shelf>`                             |
| `findBooksByShelf`             | `List<BookDTO>`            | **removed**                               |
| `isFull`                       | `Boolean isFull(ShelfDTO)` | **removed**                               |
| `getShelfSummariesForBookcase` | unchanged                  | unchanged                                 |
| `deleteAllShelvesInBookcase`   | unchanged                  | unchanged                                 |
| `createShelf`                  | unchanged                  | unchanged                                 |

The `ShelfDomainRepository` field also gained `private final` — it was previously package-private, which could allow
unintentional access from subclasses or same-package code.

**Architecture notes:** `ShelfFacade` now imports exclusively from `core.domain.model`. Zero API package references at
the port boundary. This is the correct dependency direction: inbound ports belong to the core and define contracts in
core vocabulary.

**Risk:** All callers of `ShelfFacade` are now broken until updated (covered in the next three commits). The repo's
build would fail mid-PR in isolation — this is an expected intermediate state in a multi-commit refactor.

---

### fix: update ShelfJpaRepository JPQL to reference ShelfSummary in core domain package (`30e20ce`)

**Intent:** Companion fix to the `ShelfSummary` move. JPQL `SELECT NEW` projections reference the fully-qualified class
name as a string. The old package path was still present after `e732ba1`.

**Files touched:**

- `ShelfJpaRepository.java` — one-line change in the JPQL query string

**Key code change:**

```sql
-- Before
SELECT new com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfSummary(...)

-- After
SELECT new com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfSummaryResponse(...)
```

**Risk:** JPQL string errors surface at runtime (Hibernate resolves the projection class during query execution), not at
compile time. This fix must land before `getShelfSummariesForBookcase` is exercised in any integration test or
production path.

---

### refactor: move Shelf-to-DTO mapping to web and adapter boundary layers (`2b445a0`)

**Intent:** Restore working behavior at the boundary points that must produce DTOs — the HTTP controllers and the
anti-corruption adapter — now that the core no longer provides pre-mapped DTOs.

**Files touched:**

- `ShelfResponseMapper.java` (**new**) — `web.controllers.stacks.shelf.mappers`
- `ShelfController.java` — inject `ShelfResponseMapper`; stream-map Shelf → ShelfOptionResponse
- `BookController.java` — inline `Shelf → ShelfDTO` mapping before bookcase lookup
- `ShelfAccessPortAdapter.java` — inline `Shelf → ShelfDTO`; drop dead `ShelfService` field

**Key code change — ShelfController:**

```java
// Before
public List<ShelfOptionResponse> getShelfOptions() {
  return shelfService.getShelfOptions();  // service returned pre-mapped list
}

// After
public List<ShelfOptionResponse> getShelfOptions() {
  return shelfService.getShelfOptions().stream()
      .map(shelfResponseMapper::toShelfOption)
      .toList();
}
```

**ShelfResponseMapper** is a small, focused Spring component. It encapsulates exactly one mapping:
`Shelf → ShelfOptionResponse`, computing `hasSpace = bookCount < bookCapacity` from the domain fields.

**ShelfAccessPortAdapter** is the anti-corruption adapter sitting between the `cataloging.book` module and the
`stacks.shelf` module. It now does the translation from `Shelf` (shelf module's domain language) to `ShelfDTO` (book
module's expected input). This is the correct role for an adapter: convert between bounded contexts.

**Architecture notes:** `ShelfResponseMapper` is deliberately placed in `web.controllers` rather than a shared library.
The risk of premature sharing outweighs the DRY benefit at this stage. If a second web controller needs the same
mapping, extract it then.

**Risk:** `ShelfAccessPortAdapter` previously had a dangling `ShelfService shelfService` field (package-private, never
injected via constructor) — removed in this commit. This was a dead field, not a runtime issue, but it was a code smell.

---

### refactor: update CLI book commands to use Shelf domain model from ShelfFacade (`2c2a880`)

**Intent:** The six CLI command classes that used `ShelfFacade` were consuming `Optional<ShelfDTO>` or `List<ShelfDTO>`.
They must now consume `Optional<Shelf>` / `List<Shelf>` and access data via getters.

**Files touched (all CLI):**

- `BookCirculationCommands.java` — `Optional<ShelfDTO>` → `Optional<Shelf>`; `.bookcaseId()` → `.getBookcaseId()`,
  `.shelfLabel()` → `.getShelfLabel()`
- `BookSearchCommands.java` — same pattern, two call sites
- `BookCreateImportCommands.java` — `getShelfLabel()` call
- `BookCreateIsbnCommands.java` — `getShelfLabel()` call
- `LibraryCommands.java` — `getShelfLabel()` call
- `BookPlacementCommands.java` — **inline mapping**: `Shelf → ShelfDTO` to perform the capacity check

**Key code detail — BookPlacementCommands:**
This is the one CLI file that needs a `ShelfDTO` shape post-lookup, because it uses `shelfDTO.bookCapacity()` and
`shelfDTO.bookIds().size()` for the capacity guard. The fix is an inline `.map(shelf -> new ShelfDTO(...))` on the
`Optional<Shelf>`:

```java
Optional<ShelfDTO> shelfDTO = shelfFacade
        .findShelfById(newShelfId)
        .map(shelf -> new ShelfDTO(
                shelf.getShelfId().shelfId(), shelf.getShelfLabel(),
                bookCaseId, shelf.getShelfPosition(),
                shelf.getBookCapacity(), shelf.getBookIds()));
```

This works, but it is slightly over-engineered for what it actually needs. See **Next Steps** for the cleaner path.

**Architecture notes:** CLI commands are an application boundary, analogous to a web controller. They receive domain
objects from the core and are responsible for their own presentation mapping. This is the correct model.

---

### refactor: update PromptOptions to use ShelfFacade.findAllShelves and bookFacade.findByShelfId (`a38f6a8`)

**Intent:** `PromptOptions` is the CLI prompt-building class. Two of its menu builders relied on the now-removed
`getAllDTOShelves` and `findBooksByShelf` methods from `ShelfFacade`.

**Files touched:**

- `PromptOptions.java` — two methods updated

**Key changes:**

1. `shelfOptions(Long bookcaseId)`: `getAllDTOShelves` → `findAllShelves` + inline `Shelf → ShelfDTO` mapping
2. `bookCaseOptions()`: `findByBookcaseId` + inline `Shelf → ShelfDTO` mapping; `shelfFacade.findBooksByShelf` →
   `bookFacade.findByShelfId`

The second change is architecturally meaningful: **books are now queried through `BookFacade`**, not through
`ShelfFacade`. `ShelfFacade.findBooksByShelf` was a cross-module responsibility that didn't belong on the shelf port.
Routing the call through `BookFacade.findByShelfId` respects module ownership.

**Risk:** `PromptOptions` now depends on both `ShelfFacade` and `BookFacade` for the `bookCaseOptions()` method. It
already had both injected — no new dependencies were introduced.

---

### test: update ShelfServiceTest to reflect ShelfService no longer depending on ShelfDTOMapper (`4751b37`)

**Intent:** `ShelfServiceTest` had `@Mock ShelfDTOMapper shelfDTOMapper` and 20+ `when/verify` stubs for the mapper. All
of those are invalid now that the mapper is gone.

**Files touched:**

- `ShelfServiceTest.java` — −100 lines, +15 lines

**Key changes:**

- `@Mock ShelfDTOMapper` removed
- All `when(shelfDTOMapper.toDTOFromDomain(...)).thenReturn(...)` removed
- All `verify(shelfDTOMapper)...` removed
- Assertions changed from `containsExactly(dto1, dto2)` to `containsExactly(shelf1, shelf2)`
- `isFull_shouldReturnTrue/False` tests deleted (method removed from service)
- `getAllShelves_shouldInvokeMapperForEachShelf` renamed to `getAllShelves_shouldDelegateToRepository`

The tests are now simpler and more honest: they verify that the service delegates to the repository and passes through
results unmodified. That is exactly what the service does.

---

### test: add PromptOptionsTest for bookcase option menu rendering (`8238dc3`)

**Intent:** `PromptOptions.bookCaseOptions()` had zero test coverage. After being updated to use the new `ShelfFacade`
API and `bookFacade.findByShelfId`, it became an ideal candidate for focused Mockito unit tests.

**Files touched:**

- `PromptOptionsTest.java` (**new**, 171 lines) — 7 test cases

**Test coverage added:**

| Test                                                             | What it asserts                                       |
|------------------------------------------------------------------|-------------------------------------------------------|
| `bookCaseOptions_shouldAlwaysContainCancelAsFirstEntry`          | "cancel" is always the first map value                |
| `bookCaseOptions_shouldReturnOnlyCancelWhenNoBookcasesExist`     | Map has exactly 1 entry when bookcases empty          |
| `bookCaseOptions_shouldContainOneEntryPerBookcase`               | N bookcases → N+1 entries (cancel + one per bookcase) |
| `bookCaseOptions_shouldCorrectlyCountBooksAcrossShelves`         | Book count summed across shelves appears in key       |
| `bookCaseOptions_shouldFormatKeyWithBookcaseLocationInUppercase` | Location string is uppercased in the display key      |
| `bookCaseOptions_shouldCountZeroBooksWhenShelvesAreEmpty`        | Zero count shown correctly                            |
| `bookCaseOptions_shouldPreservesInsertionOrder`                  | `LinkedHashMap` insertion order preserved             |

The tests also validate the `Shelf` constructor update: `PromptOptionsTest` constructs real `Shelf` objects with the new
6-argument signature, confirming the domain model change integrates cleanly.

---

## Deep Dive: The Main Refactor

### The core problem: a leaking abstraction at the inbound port

The fundamental issue was that `ShelfFacade` — the inbound port, the contract the core defines for the outside world —
was using types from the outside world (API DTOs) in its own signature. That is a dependency direction inversion: the
core was depending on its callers, not the other way around.

### The correct flow (after this PR)

```
HTTP Request
    │
    ▼
ShelfController (web layer)
    │  calls shelfService.getShelfOptions() → List<Shelf>
    │  maps: shelfResponseMapper.toShelfOption(shelf) → ShelfOptionResponse
    ▼
ShelfService (application) implements ShelfFacade
    │  calls shelfDomainRepositoryPort.findAll() → List<Shelf>
    ▼
ShelfDomainRepositoryImpl (infrastructure)
    │  calls ShelfJpaRepository.findAll() → List<ShelfEntity>
    │  maps: ShelfMapper.toDomainFromEntity(entity) → Shelf  ← bookcaseId included here
    ▼
ShelfEntity (persistence)
```

The `Shelf` object that exits the repository is **fully hydrated** — it now carries `bookcaseId` directly. No caller
needs to make a second lookup to answer "which bookcase does this shelf belong to?"

### The boundary mapping pattern

Each boundary type does its own mapping:

```
Web controller  → ShelfResponseMapper.toShelfOption(shelf)  → ShelfOptionResponse (HTTP)
Web controller  → new ShelfDTO(shelf.getId(), ...)           → ShelfDTO (HTTP, BookController)
CLI adapter     → shelf.getShelfLabel(), shelf.getBookcaseId() → direct getter usage
ACA adapter     → new ShelfDTO(shelf.getId(), ...)           → ShelfDTO (cross-module)
```

This pattern puts mapping responsibility at the point of use. Each boundary adapts domain objects to its own local
representation — which is what boundaries are for.

### Why this design is better than the old one

| Concern                            | Before                                       | After                                           |
|------------------------------------|----------------------------------------------|-------------------------------------------------|
| Core layer DTO imports             | `ShelfDTO`, `ShelfOptionResponse`, `BookDTO` | None                                            |
| Secondary repo calls per shelf     | 1 (`getBookcaseIdByShelfId`)                 | 0                                               |
| `ShelfFacade` cross-module methods | `findBooksByShelf → List<BookDTO>`           | removed                                         |
| Mapper location                    | `core.mappers`                               | `web.controllers.mappers` (web) or inline (CLI) |
| `ShelfService` constructor args    | 3 (repo, bookFacade, mapper)                 | 2 (repo, bookFacade)                            |
| Test complexity (ShelfServiceTest) | Mocked mapper in 20+ stubs                   | No mapper — 0 stubs for mapping                 |

---

## Dependency & Boundary Audit

### Dependencies removed

| From                     | Removed import                                                               |
|--------------------------|------------------------------------------------------------------------------|
| `ShelfFacade` (port)     | `library.stacks.shelf.api.dtos.ShelfDTO`                                     |
| `ShelfFacade` (port)     | `library.stacks.shelf.api.dtos.ShelfSummary` (moved to core)                 |
| `ShelfFacade` (port)     | `library.cataloging.book.api.dtos.BookDTO`                                   |
| `ShelfService`           | `library.stacks.shelf.api.dtos.ShelfDTO`                                     |
| `ShelfService`           | `library.stacks.shelf.api.dtos.ShelfOptionResponse`                          |
| `ShelfService`           | `library.stacks.shelf.core.mappers.ShelfDTOMapper`                           |
| `ShelfService`           | `library.cataloging.book.api.dtos.BookDTO`                                   |
| `ShelfAccessPortAdapter` | `library.stacks.shelf.core.application.ShelfService` (leaked concrete class) |

### Dependencies added

| To                   | Added import                                                       |
|----------------------|--------------------------------------------------------------------|
| `ShelfController`    | `web.controllers.stacks.shelf.mappers.ShelfResponseMapper`         |
| `ShelfFacade` (port) | `library.stacks.shelf.core.domain.model.Shelf`                     |
| `ShelfFacade` (port) | `library.stacks.shelf.core.domain.model.ShelfSummary` (moved here) |

### Potential layering concern: inline DTO construction at CLI/web boundaries

`BookPlacementCommands`, `BookController`, `ShelfAccessPortAdapter`, and `PromptOptions` all construct
`new ShelfDTO(...)` directly. This means `ShelfDTO`'s constructor signature is implicitly coupled to four boundary
sites. If the `ShelfDTO` record gains or loses a field, all four break simultaneously.

This is acceptable at the current scale, but a shared boundary-layer factory or builder could centralize this if
`ShelfDTO` evolves frequently.

### One import still worth reviewing

`BookController` (web layer) constructs `ShelfDTO` inline using `shelfDomain.getShelfId().shelfId()`. This is fine — but
`BookController` also calls `shelfService.findShelfById(...)` directly, which bypasses `ShelfFacade`. Verify that
`shelfService` is injected as `ShelfFacade` (the interface), not as `ShelfService` (the concrete class). If it's the
concrete class, that is a layering violation to fix.

---

## Testing & Verification

### Current test coverage

| Test class               | Count               | What it covers                                                      |
|--------------------------|---------------------|---------------------------------------------------------------------|
| `ShelfServiceTest`       | 18 tests            | ShelfService delegation, shelf creation, validation, capacity rules |
| `BrowseShelfUseCaseTest` | 1 test              | Browse use case integration                                         |
| `PromptOptionsTest`      | 7 tests             | bookCaseOptions menu building (new in this PR)                      |
| `ShelfAccessPortAdapter` | (no dedicated test) | Only exercised via integration                                      |

**Total suite:** 136 tests, 0 failures, 0 errors.

### Run verification commands

```bash
# Full test suite
mvn test

# Shelf module only
mvn test -pl . -Dtest="*Shelf*"

# Shelf service specifically
mvn test -Dtest=ShelfServiceTest

# New PromptOptions tests
mvn test -Dtest=PromptOptionsTest

# Compile check only (fast)
mvn -q -DskipTests compile
```

### Suggested tests not yet written

1. **`ShelfResponseMapperTest`** — verify `toShelfOption(Shelf)` correctly computes
   `hasSpace = bookCount < bookCapacity` for full, partially full, and empty shelves. This is a pure unit test, no
   Spring context needed.

2. **`ShelfAccessPortAdapterTest`** — verify that `findShelfById` correctly maps `Shelf` → `ShelfDTO` with all fields,
   and returns `Optional.empty()` when the facade returns empty.

3. **`BookPlacementCommands` capacity guard** — verify that when `shelf.bookIds.size() >= bookCapacity`, an
   `IllegalStateException` is thrown before any placement is attempted.

4. **`PromptOptionsTest.shelfOptions`** — the `shelfOptions(Long bookcaseId)` method has no test coverage yet. It should
   be covered similarly to `bookCaseOptions`: assert cancel appears, assert one entry per shelf, assert `shelfId` is the
   map value.

5. **JPQL smoke test** — `ShelfJpaRepository.getShelfSummariesForBookcase` uses `SELECT NEW` with a string-typed class
   reference. An `@DataJpaTest` or application context test that exercises this query would catch future package renames
   before they reach production.

---

## What I Learned

1. **Inbound ports must speak domain language.** If your port interface imports from `api.dtos`, you've leaked the
   boundary. The port defines the core's surface — it cannot depend on the outside world. Fix it by returning domain
   objects and making callers do the translation.

2. **Moving a class that is referenced in JPQL string literals doesn't show up at compile time.** The `ShelfSummary`
   move broke the JPQL query silently. A regression test that actually executes the query is the only reliable safety
   net — `@DataJpaTest` would have caught this immediately.

3. **Domain model enrichment removes N+1 patterns.** Adding `bookcaseId` directly to `Shelf` eliminated
   `getBookcaseIdByShelfId` calls that were happening per-shelf in every stream pipeline. The mapper populates it once
   at load time.

4. **Deleting the mapper simplified the tests more than it simplified the production code.** The `ShelfServiceTest` lost
   100 lines of mock setup. That's a signal: when a test class is heavily mocked, the subject has too many
   collaborators. Fewer collaborators = fewer mocks = cleaner tests.

5. **`findBooksByShelf` on `ShelfFacade` was a cross-module API violation hiding in plain sight.** The shelf bounded
   context shouldn't be the entry point for querying books. Routing through `BookFacade.findByShelfId` correctly places
   ownership. Module boundaries are defined by which domain owns the query, not which entity holds the foreign key.

6. **Inline DTO construction at four boundary sites is a smell, but not the right time to fix it.** Extracting a shared
   `ShelfDTOFactory` now would be premature abstraction. Wait until a fifth site appears or the constructor signature
   changes twice — then extract.

7. **Commit granularity paid off here.** Splitting 12 commits by layer (domain → port → service → infrastructure → web →
   CLI → tests) made the history a legible story of an architectural correction. The git log is now the documentation of
   the "why" — not just the "what".

---

## Next Steps

### Immediate follow-ups (today)

- **`BookPlacementCommands` — use `shelf.isFull()` directly.** The current code maps `Shelf → ShelfDTO` purely to check
  `shelfDTO.bookCapacity() <= shelfDTO.bookIds().size()`. The `Shelf` domain model already has `isFull()`. Replace the
  entire inline mapping with:
  ```java
  Optional<Shelf> shelf = shelfFacade.findShelfById(newShelfId);
  if (shelf.get().isFull()) { throw new IllegalStateException("Shelf is full"); }
  ```
  This removes one of the four inline `new ShelfDTO(...)` sites and makes the intent clearer.

- **Verify `BookController` injects `ShelfFacade` (interface), not `ShelfService` (concrete class).** If it injects the
  concrete class, that is a layering violation introduced by the inline mapping change.

- **Add `ShelfResponseMapperTest`** — 5 minutes of work, covers an edge case (full shelf `hasSpace = false`).

### Short-term hardening (this week)

- **Write `ShelfAccessPortAdapterTest`.** The adapter is the anti-corruption boundary between two bounded contexts — it
  deserves dedicated tests for the `Shelf → ShelfDTO` mapping fidelity.

- **Write `PromptOptions.shelfOptions` test.** Seven tests were added for `bookCaseOptions`; the parallel `shelfOptions`
  method has no coverage.

- **Add a `@DataJpaTest` for `ShelfJpaRepository.getShelfSummariesForBookcase`.** The JPQL `SELECT NEW` projection is
  fragile. An integration-level test against an in-memory H2 database will catch future renames at test time rather than
  runtime.

- **Consider a `@SpringBootTest` smoke test for `ShelfController`.** Both `/options` and `/options/{bookcaseId}`
  endpoints went through a behavioral change (mapping now happens in the controller). A thin `@WebMvcTest` would add a
  safety net.

### Strategic refactors (later)

- **Centralize `Shelf → ShelfDTO` construction.** Four sites construct `new ShelfDTO(...)` manually (
  `BookPlacementCommands`, `BookController`, `ShelfAccessPortAdapter`, `PromptOptions`). Once/if `ShelfDTO` gains
  required fields, all four break. A shared `ShelfDTOAssembler` in the web/CLI boundary layer (not the core) would
  eliminate the duplication without violating architecture rules.

- **Rename `getShelfShelfOptionResponse` on `ShelfDomainRepository`.** The method name was meaningful when it returned
  `ShelfOptionResponse` objects. Now that it returns `List<Shelf>`, the name is misleading. `findByBookcaseIdForOptions`
  or simply reusing `findByBookcaseId` with a dedicated query would be cleaner.

- **Evaluate whether `ShelfFacade.getAllShelves` and `ShelfFacade.findAllShelves` should be merged.** `ShelfService` has
  both `getAllShelves(Long)` (public, not `@Override`) and `findAllShelves(Long)` (the port method) doing identical
  things. `getAllShelves` exists for legacy compatibility. Once no callers use it directly, delete it and keep only the
  facade method.
