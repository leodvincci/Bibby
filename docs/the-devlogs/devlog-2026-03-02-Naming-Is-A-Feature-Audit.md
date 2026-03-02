# Naming Is a Feature Audit

**PR:** #342 — `refactor/bookcase-infra-restructure-and-usecase-port`
**Range:** `38b9e84..5b1e281` (4 commits, 37 files, +545 / −325)
**Date:** 2026-03-02

---

## 1. Naming Rubric

Every name in the codebase is scored on five axes. A name that scores **4+** across all axes is production-grade; anything below **3** on any axis is flagged.

| Axis | 5 (Excellent) | 3 (Passable) | 1 (Misleading) |
|------|--------------|--------------|-----------------|
| **Truthfulness** | Name describes exactly what the code does, nothing more | Name is vague but not wrong | Name implies behavior the code does not have |
| **Cardinality** | Singular/plural matches actual multiplicity | Ambiguous but not incorrect | Singular name wraps a collection or vice-versa |
| **Layer Fidelity** | Name reveals which architectural layer it belongs to | Layer is guessable from context | Name suggests a different layer than it lives in |
| **Consistency** | Follows the same suffix/prefix conventions as siblings | Minor deviation from siblings | Contradicts established project convention |
| **Grep-ability** | Unique enough to find with a single `rg` query | Requires package-qualified search | Name collision across modules, impossible to disambiguate without path |

---

## 2. Misleading Names Found

### 2.1 `BookDomainRepositoryAdaptor` — Spelling Inconsistency

| Axis | Score | Reason |
|------|-------|--------|
| Truthfulness | 4 | Accurately describes an adapter over `BookDomainRepository` |
| Consistency | **1** | Every other adapter in the project uses **"Adapter"**: `BookcaseRepositoryAdapter`, `PlacementRepositoryAdapter`, `ShelfRepositoryAdapter`, `BookAccessPortAdapter`, `ShelfAccessPortAdapter`. This is the only "Adaptor". |
| Grep-ability | **2** | `rg Adapter` misses it; `rg Adaptor` only finds this one file |

**Current:** `BookDomainRepositoryAdaptor`
**Proposed:** `BookDomainRepositoryAdapter`
**Location:** `cataloging.book.infrastructure.adapter.outbound`

---

### 2.2 `BookCaseController` — Casing Break

| Axis | Score | Reason |
|------|-------|--------|
| Consistency | **1** | The domain model is `Bookcase` (one word). DTO is `BookcaseDTO`. Mapper is `BookcaseMapper`. Facade is `BookcaseFacade`. The controller alone capitalizes the C: `BookCaseController`. |
| Grep-ability | **2** | Searching `Bookcase` won't find it; searching `BookCase` only finds this controller |

**Current:** `BookCaseController`
**Proposed:** `BookcaseController`
**Location:** `web.controllers.stacks.bookcase`

---

### 2.3 `createNewBookCase` Method — Casing Leak in Port Interface

| Axis | Score | Reason |
|------|-------|--------|
| Consistency | **2** | `CreateBookcaseUseCasePort` uses "Bookcase" in the type name, but the method inside it says `createNewBookCase` with a capital C. Same issue in `BookcaseFacade.createNewBookCase`. |
| Truthfulness | 3 | "New" is redundant — create already implies new |

**Current:** `createNewBookCase(...)` on `CreateBookcaseUseCasePort` and `BookcaseFacade`
**Proposed:** `createBookcase(...)` or at minimum `createNewBookcase(...)`
**Location:** `bookcase.core.ports.inbound.CreateBookcaseUseCasePort:6`, `BookcaseFacade:39`

---

### 2.4 Duplicate `ShelfAccessPort` / `ShelfAccessPortAdapter` Across Modules

| Axis | Score | Reason |
|------|-------|--------|
| Grep-ability | **1** | Two interfaces named `ShelfAccessPort` and two classes named `ShelfAccessPortAdapter` exist in different bounded contexts. `rg ShelfAccessPort` returns hits from both `bookcase` and `cataloging.book` with no way to distinguish without reading paths. |
| Layer Fidelity | 3 | Both are outbound ports, which is correct, but their contracts are completely different |

**Module A — Bookcase:**
- `bookcase.core.ports.outbound.ShelfAccessPort` → create/delete shelves
- `bookcase.infrastructure.adapter.ShelfAccessPortAdapter` → delegates to `CreateShelfUseCasePort` / `DeleteShelvesUseCasePort`

**Module B — Cataloging (Book):**
- `cataloging.book.core.port.outbound.ShelfAccessPort` → query shelf by ID, check capacity
- `cataloging.book.infrastructure.adapter.outbound.ShelfAccessPortAdapter` → delegates to `ShelfQueryFacade`

**Proposed renames:**
| Current | Proposed | Rationale |
|---------|----------|-----------|
| `bookcase...ShelfAccessPort` | `ShelfCommandAccessPort` | This port only writes (create, delete) |
| `bookcase...ShelfAccessPortAdapter` | `ShelfCommandAccessPortAdapter` | Matches port rename |
| `cataloging.book...ShelfAccessPort` | `ShelfQueryAccessPort` | This port only reads (find, isFull) |
| `cataloging.book...ShelfAccessPortAdapter` | `ShelfQueryAccessPortAdapter` | Matches port rename |

---

### 2.5 Duplicate `BookcaseMapper` — Same Simple Name, Different Layers

| Axis | Score | Reason |
|------|-------|--------|
| Grep-ability | **1** | Two classes named `BookcaseMapper`. `rg "class BookcaseMapper"` returns two hits. |
| Layer Fidelity | **2** | One maps Entity↔Domain (infrastructure), the other maps Domain→DTO (web). Neither name reveals its layer. |

**Infrastructure mapper:** `bookcase.infrastructure.persistence.mapping.BookcaseMapper` (Entity ↔ Domain)
**Web mapper:** `web.controllers.stacks.bookcase.BookcaseMapper` (Domain → DTO)

**Proposed renames:**
| Current | Proposed | Rationale |
|---------|----------|-----------|
| `infrastructure...BookcaseMapper` | `BookcaseEntityMapper` | Clarifies it maps entities |
| `web...BookcaseMapper` | `BookcaseDtoMapper` | Clarifies it maps DTOs |

---

### 2.6 `portModel` Package — Unconventional Casing

| Axis | Score | Reason |
|------|-------|--------|
| Consistency | **2** | Java package convention is all-lowercase. Every other package in the project follows this (`inbound`, `outbound`, `dtos`, `usecases`). `portModel` is the only camelCase package. |

**Current:** `bookcase.core.ports.portModel.CreateBookcaseResult`
**Proposed:** `bookcase.core.ports.inbound.CreateBookcaseResult` (colocate with the port that returns it) or `bookcase.core.ports.model.CreateBookcaseResult`

---

### 2.7 `inboundPortModels` Package — Same Issue, Shelf Module

| Axis | Score | Reason |
|------|-------|--------|
| Consistency | **2** | `shelf.core.ports.inbound.inboundPortModels` — camelCase and redundant (`inbound` already appears in the parent package) |

**Current:** `shelf.core.ports.inbound.inboundPortModels`
**Proposed:** `shelf.core.ports.inbound.models` or just `shelf.core.ports.inbound.model`

---

### 2.8 `ShelfCommandFacade` — Ghost Import / Dead Interface

| Axis | Score | Reason |
|------|-------|--------|
| Truthfulness | **1** | Still imported in `ShelfAccessPortAdapter` constructor, accepted as a parameter, but **never used**. All three of its methods now have dedicated use-case ports. The facade is effectively dead code. |

**Current:** `ShelfAccessPortAdapter` constructor takes `ShelfCommandFacade shelfCommandFacade` but ignores it
**Proposed:** Remove the `ShelfCommandFacade` parameter from the constructor. Separately, deprecate or delete `ShelfCommandFacade` entirely — all call sites now use `PlaceBookOnShelfUseCasePort`, `CreateShelfUseCasePort`, `DeleteShelvesUseCasePort`.

---

### 2.9 Outbound Port Suffix Inconsistency: `*Port` vs Bare Name

| Axis | Score | Reason |
|------|-------|--------|
| Consistency | **2** | Shelf outbound ports use the `*Port` suffix: `ShelfDomainRepositoryPort`, `PlacementRepositoryPort`, `BookAccessPort`. Bookcase outbound port omits it: `BookcaseRepository`. Both are port interfaces in `core.ports.outbound`. |

**Current:** `bookcase.core.ports.outbound.BookcaseRepository`
**Proposed:** `BookcaseRepositoryPort` — to match shelf module convention and make port identity grep-able

---

### 2.10 `AuthorMapperTwo` / `BookMapperTwo` — Meaningless Suffixes

| Axis | Score | Reason |
|------|-------|--------|
| Truthfulness | **1** | "Two" conveys nothing about what makes this mapper different from `AuthorMapper` / `BookMapper` |
| Grep-ability | 3 | Findable, but confusing when found |

**Current:** `AuthorMapperTwo`, `BookMapperTwo`
**Proposed:** Investigate what makes them different, then rename to reflect it (e.g., `AuthorEntityMapper` vs `AuthorDtoMapper`, or `AuthorDomainMapper` vs `AuthorResponseMapper`)

---

### 2.11 `BookcaseFacade` — Stale Javadoc

| Axis | Score | Reason |
|------|-------|--------|
| Truthfulness | **2** | `findBookCaseById` Javadoc says `@param aLong` (line 16) and `@return an Optional<BookcaseDTO>` (line 17), but the method signature is `Bookcase findBookCaseById(Long bookcaseId)` — no Optional, no DTO. Also `findBookCaseById` has the capital-C "BookCase" inconsistency. |

**Current:** `findBookCaseById` with stale docs
**Proposed:** Rename to `findBookcaseById`, fix Javadoc to match `Bookcase` return type

---

## 3. Safe Rename Plan

Renames are ordered by risk (lowest first). Each step is independently deployable.

### Step 1 — Zero-Risk: Fix Spelling (1 file + test)

| # | Change | Files |
|---|--------|-------|
| 1a | Rename `BookDomainRepositoryAdaptor` → `BookDomainRepositoryAdapter` | `BookDomainRepositoryAdaptor.java`, `BookDomainRepositoryAdaptorTest.java`, any import sites |

**Verification:** `mvn test` — all tests pass, no behavior change.

### Step 2 — Low Risk: Fix Casing (2 files + tests)

| # | Change | Files |
|---|--------|-------|
| 2a | Rename `BookCaseController` → `BookcaseController` | `BookCaseController.java`, test file if exists |
| 2b | Rename method `createNewBookCase` → `createBookcase` | `CreateBookcaseUseCasePort.java`, `BookcaseFacade.java`, `CreateBookcaseUseCase.java`, `BookcaseService.java`, `BookCaseController.java`, `BookcaseCommands.java` |
| 2c | Rename method `findBookCaseById` → `findBookcaseById` | `BookcaseFacade.java`, `BookcaseService.java`, call sites |

**Verification:** `mvn test` — IDE rename refactor handles all call sites.

### Step 3 — Medium Risk: Disambiguate Duplicate Names (4 files + tests)

| # | Change | Files |
|---|--------|-------|
| 3a | Rename `bookcase...ShelfAccessPort` → `ShelfCommandAccessPort` | Port interface + adapter + test |
| 3b | Rename `cataloging.book...ShelfAccessPort` → `ShelfQueryAccessPort` | Port interface + adapter + test |
| 3c | Rename `infrastructure...BookcaseMapper` → `BookcaseEntityMapper` | Mapper + all static call sites |
| 3d | Rename `web...BookcaseMapper` → `BookcaseDtoMapper` | Mapper + controller call sites |

**Verification:** `mvn test` + ArchUnit rules still pass.

### Step 4 — Medium Risk: Package Normalization (move, no logic change)

| # | Change | Files |
|---|--------|-------|
| 4a | Rename package `portModel` → `model` | `CreateBookcaseResult.java` + import sites |
| 4b | Rename package `inboundPortModels` → `models` | `ShelfResponse.java`, `ShelfSummaryResponse.java`, `ShelfPortModelMapper.java` + import sites |

**Verification:** `mvn test` — package-level moves only.

### Step 5 — Higher Risk: Remove Dead Code

| # | Change | Files |
|---|--------|-------|
| 5a | Remove `ShelfCommandFacade` parameter from `ShelfAccessPortAdapter` constructor | `ShelfAccessPortAdapter.java`, `ShelfAccessPortAdapterTest.java` |
| 5b | Deprecate `ShelfCommandFacade` interface (or delete if no other callers remain) | `ShelfCommandFacade.java`, `ShelfService.java` |
| 5c | Add `Port` suffix to `BookcaseRepository` → `BookcaseRepositoryPort` | Port interface + adapter + all injection sites |

**Verification:** `mvn test` + verify no runtime bean-wiring failures with `mvn spring-boot:run` smoke test.

---

## 4. False Generality Watchlist

These names are not wrong today but could become misleading as the codebase grows:

| Name | Risk | Why |
|------|------|-----|
| `BookAccessPort` (shelf module) | Medium | Sounds like it could do anything book-related. Actually only provides `getBookById` and `getBookIdsByShelfId` and `deleteBooksOnShelves`. Consider `ShelfBookAccessPort` to scope it to shelf's view of books. |
| `ShelfAccessPort` (bookcase module) | High | Already collides (see 2.4). Even after renaming, "access" is too vague — it does create + delete, not read. |
| `BookService` / `BookcaseService` | Low | These are facade implementations (`@Service`), not domain services. Fine for now, but if true domain services appear, the names will collide in meaning. |
| `PromptOptions` | Low | Lives in `cli.prompt.domain` — "domain" here means CLI domain, not DDD domain. Could confuse developers who associate "domain" with the library bounded contexts. |

---

## 5. Consistency Patterns Discovered

### What the PR Got Right

| Pattern | Example | Assessment |
|---------|---------|------------|
| `*RepositoryAdapter` for persistence adapters | `BookcaseRepositoryAdapter`, `PlacementRepositoryAdapter`, `ShelfRepositoryAdapter` | Excellent — consistent, grep-able, layer-revealing |
| `*UseCasePort` for single-method inbound ports | `CreateBookcaseUseCasePort`, `CreateShelfUseCasePort`, `PlaceBookOnShelfUseCasePort` | Excellent — verb-first, cardinality-correct, truthful |
| `ShelfOptionResponseDTO` rename from `ShelfOptionResponse` | Added `DTO` suffix to match `BookcaseDTO`, `BookDTO`, etc. | Good — makes the DTO nature explicit |
| Moving persistence concerns into `persistence` subpackage | `infrastructure/persistence/adapter/`, `infrastructure/persistence/entity/`, `infrastructure/persistence/mapping/` | Good — separates persistence from other infra concerns |

### What Needs Harmonization

| Inconsistency | Module A | Module B |
|--------------|----------|----------|
| Port suffix | `ShelfDomainRepositoryPort` | `BookcaseRepository` |
| Adapter spelling | `BookDomainRepositoryAdaptor` | `*Adapter` everywhere else |
| Class casing | `Bookcase` (everywhere) | `BookCase` (controller, methods) |
| Package casing | `dtos`, `usecases`, `model` | `portModel`, `inboundPortModels` |
| Mapper disambiguation | None needed (shelf module) | Two `BookcaseMapper` classes |

---

## 6. Interview Talking Points

If asked "walk me through your naming decisions in this PR" in a technical interview:

1. **"We standardized adapter suffixes across modules."** The rename from `BookDomainRepositoryImpl` → `BookDomainRepositoryAdaptor` was a directional improvement (dropping "Impl" for an architecture-revealing name), but introduced a spelling inconsistency with the rest of the codebase. The follow-up fix to `Adapter` completes the intent.

2. **"We discovered that cross-module ports need module-scoped names."** Two `ShelfAccessPort` interfaces emerged independently in the bookcase and cataloging modules. When both are visible in the same IDE workspace, the collision creates cognitive overhead. Prefixing with `Command`/`Query` aligns with CQRS semantics and makes the port's contract obvious from the name alone.

3. **"We applied the Single Responsibility Principle to naming."** The `ShelfCommandFacade` was a 3-method interface that combined create, delete, and place-book operations. Decomposing it into `CreateShelfUseCasePort`, `DeleteShelvesUseCasePort`, and `PlaceBookOnShelfUseCasePort` means each name is a complete sentence: "this port does one thing, and its name tells you exactly what."

4. **"We treat package names as part of the public API."** Renaming `portModel` to `model` and `inboundPortModels` to `models` isn't just cosmetic — package names appear in every import statement, and camelCase packages break the Java convention that tools and developers expect.

5. **"We know when NOT to rename."** `BookcaseFacade` is a broad read+write interface that still has live callers. We extracted the `CreateBookcaseUseCasePort` from it rather than renaming the facade, because renaming a heavily-used interface creates churn without removing complexity. The facade will shrink naturally as more use-case ports are extracted.
