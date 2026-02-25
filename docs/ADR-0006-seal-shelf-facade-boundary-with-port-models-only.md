# ADR 0006: Seal ShelfFacade Boundary With Port Models Only

**Date:** 2026-02-25
**Time:** 10:23 CST
**Status:** Accepted
**Deciders:** leodvincci
**Technical Story:** PR #326 (`refactor/shelf-findall-response-and-docs`)

## Context

After PRs #324 and #325, the `ShelfFacade` inbound port returned `ShelfResponse` from two of its query methods (`findShelvesByBookcaseId`, `findAll`), but two other methods still leaked domain types across the boundary:

| Method | Return type (before) | Problem |
|--------|---------------------|---------|
| `findShelfById` | `Optional<Shelf>` | Consumers imported the `Shelf` domain aggregate directly |
| `getShelfSummariesForBookcaseByBookcaseId` | `List<ShelfSummary>` | `ShelfSummary` was a record living in `core.domain.model` — a projection masquerading as a domain type |

This created two concrete violations:

1. **CLI commands imported domain internals.** `BookCirculationCommands`, `BookSearchCommands`, and `BookcaseCommands` imported `shelf.core.domain.model.Shelf` or `shelf.core.domain.model.ShelfSummary` — reaching past the port layer into the domain.
2. **`ShelfResponse` was incomplete.** It lacked `bookcaseId`, forcing consumers (e.g., `BookCirculationCommands:60`, `BookSearchCommands:87`) to call `shelf.get().getBookcaseId()` on the domain entity, then pass it to `bookcaseFacade.findBookCaseById(...)`. The domain entity was the only way to get the bookcase ID.

Additionally, `ShelfSummary` was misplaced from inception. It carried no domain behavior — it was a read-only projection (shelf ID, label, book count) used exclusively by the `BookcaseCommands` UI flow. Its package (`core.domain.model`) implied it was part of the domain model, but it served purely as a port response.

**Constraints:**
- The `@Transactional` annotation on `findShelfById` must remain — `Shelf.bookIds` is lazily loaded, and the mapping to `ShelfResponse` reads that collection.
- All 7 CLI command files and 1 cross-module adapter (`ShelfAccessPortAdapter`) consume `ShelfFacade`. Any contract change cascades.
- 142 existing tests must continue to pass.

**Evidence:**

- Commits: `5a82ef9` (Complete shelf inbound port model migration), `5793f41` (Document shelf inbound port model migration), `377cc8d` (Refactor documentation and formatting)
- Key files: `ShelfFacade.java`, `QueryShelfUseCase.java`, `ShelfService.java`, `ShelfResponse.java`, `ShelfSummaryResponse.java` (new), `ShelfSummary.java` (deleted), `ShelfPortModelMapper.java`, 7 CLI command classes, `ShelfAccessPortAdapter.java`, 3 test classes

## Decision

- **I will make `ShelfFacade` return only port model types (`ShelfResponse`, `ShelfSummaryResponse`) from all query methods — no domain type (`Shelf`, `ShelfSummary`, `ShelfId`) may appear in its import block or method signatures.**
- **I will rename `ShelfSummary` to `ShelfSummaryResponse` and relocate it from `core.domain.model` to `core.ports.inbound.inboundPortModels`**, establishing that read projections are port models, not domain models.
- **I will add `bookcaseId` to `ShelfResponse`** so that consumers never need to reach into the domain to resolve a shelf's parent bookcase.

Boundary and ownership rules introduced:

- **The inbound port (`ShelfFacade`) owns the response contract.** All return types live in `core.ports.inbound.inboundPortModels`.
- **Domain-to-port-model mapping happens inside the use case layer** (`QueryShelfUseCase`), not in the service facade or in consumers.
- **No adapter or CLI command may import from `core.domain.model`** for the shelf bounded context.

## Decision drivers

1. **Four CLI classes imported `Shelf` directly** — `BookCirculationCommands`, `BookSearchCommands`, `BookPlacementCommands` (via getter-style calls on `ShelfResponse`), and `BookcaseCommands` (imported `ShelfSummary`). This meant any rename or restructuring of `Shelf` would break the CLI layer. Evidence: removed imports in commit `5a82ef9`.

2. **`ShelfSummary` was in the wrong package.** A record with no domain behavior (`Long shelfId, String label, long bookCount`) sat in `core.domain.model`. It existed to serve the UI, not to enforce invariants. Evidence: `ShelfSummary.java` deleted from `core.domain.model`; `ShelfSummaryResponse.java` created in `core.ports.inbound.inboundPortModels`.

3. **`ShelfResponse` without `bookcaseId` forced domain leakage.** Consumers needed `shelf.getBookcaseId()` on the domain `Shelf` entity to look up the parent bookcase. Adding `bookcaseId` to the port model eliminated this dependency. Evidence: `ShelfResponse.java` diff adding 6th field; `ShelfPortModelMapper.java` mapping `shelf.getBookcaseId()`.

4. **Inconsistency in the port contract.** `findShelvesByBookcaseId` and `findAll` returned `ShelfResponse`, but `findShelfById` returned `Optional<Shelf>`. Consumers couldn't rely on a uniform port model contract. Evidence: `ShelfFacade.java` diff changing `Optional<Shelf>` → `Optional<ShelfResponse>`.

5. **Getter-style calls on records.** `BookPlacementCommands` called `shelf.getShelfId().shelfId()`, `shelf.getShelfLabel()`, etc. on what was actually a `ShelfResponse` record. The code was written for a JavaBean, not a record. Evidence: `BookPlacementCommands.java` diff changing 6 accessor calls.

6. **Testability.** `ShelfServiceTest.findShelfById` mocked a `Shelf` domain entity to test a facade delegation method. After the change, it constructs a `ShelfResponse` record — the test speaks the language of the port, not the domain. Evidence: `ShelfServiceTest.java` diff.

## Considered options

### Option A: Seal the boundary — return port models from all ShelfFacade methods (chosen)

- **Description:** Change `findShelfById` to return `Optional<ShelfResponse>`, `getShelfSummariesForBookcaseByBookcaseId` to return `List<ShelfSummaryResponse>`. Add `bookcaseId` to `ShelfResponse`. Rename and relocate `ShelfSummary`.
- **Pros:**
  - Zero domain types in `ShelfFacade` imports — boundary is airtight
  - Consumers depend only on stable port model records
  - `Shelf` aggregate can evolve (add methods, rename fields) without consumer impact
  - Uniform convention: all queries return `*Response` types
- **Cons:**
  - 20-file coordinated change — all consumers and tests updated atomically
  - Adding `bookcaseId` to `ShelfResponse` is a breaking record constructor change
  - Mapping overhead: `findShelfById` now runs `ShelfPortModelMapper.toShelfResponse()` inside a `@Transactional` boundary
- **Risks:**
  - If `Shelf.bookcaseId` is null, `ShelfResponse.bookcaseId()` returns null — consumers calling `.get().bookcaseId()` could NPE downstream (pre-existing, not new)
- **Where it shows up in code:**
  - `ShelfFacade.java`: signature changes
  - `QueryShelfUseCase.findShelfById`: `Optional.of(shelf)` → `Optional.of(ShelfPortModelMapper.toShelfResponse(shelf))`
  - All CLI command files: `Optional<Shelf>` → `Optional<ShelfResponse>`, `.getX()` → `.x()`

### Option B: Return `Shelf` from `findShelfById` and let consumers map

- **Description:** Keep `findShelfById` returning `Optional<Shelf>`. Each consumer maps to whatever type it needs (DTO, response, etc.).
- **Pros:**
  - No port model change needed
  - Consumer-local control over mapping
- **Cons:**
  - Every consumer must import `Shelf` — the boundary leak persists
  - Duplicate mapping logic across 4+ CLI classes
  - Domain changes ripple to all consumers
  - Breaks the convention set by `findShelvesByBookcaseId` and `findAll`
- **Risks:**
  - As the domain model evolves, the blast radius of changes grows linearly with the number of consumers
- **Where it shows up in code (would have):** No change to `ShelfFacade.java`; each CLI command class keeps `import Shelf`

### Option C: Return `ShelfDTO` (API-layer DTO) from the facade instead of a port model

- **Description:** Use the existing `ShelfDTO` (in `shelf.api.dtos`) as the return type for all facade methods, eliminating the need for a separate port model.
- **Pros:**
  - Fewer types — no `ShelfResponse` and `ShelfDTO` duplication
  - CLI commands already know `ShelfDTO`
- **Cons:**
  - `ShelfDTO` lives in the API layer (`shelf.api.dtos`) — the core port would depend on an outer layer, violating hexagonal dependency direction
  - `ShelfDTO` has `fromEntity(ShelfEntity)` factory methods that tie it to the persistence layer
  - API-layer concerns (serialization annotations, validation) would leak into the port contract
- **Risks:**
  - Hard to add a REST adapter later without conflicting with CLI-specific DTO shapes
- **Where it shows up in code (would have):** `ShelfFacade.java` would import `shelf.api.dtos.ShelfDTO` — an inward dependency violation

### Option D: Introduce a shared `ShelfView` in a new `shared` module

- **Description:** Create a cross-cutting `ShelfView` record in a shared module that both core and consumers depend on.
- **Pros:**
  - Clean separation — neither core nor consumer "owns" the contract
  - Works well in multi-module builds
- **Cons:**
  - Over-engineering for a single bounded context
  - Bibby is a single-module application — no multi-module build to justify this
  - Adds a new dependency target with no clear owner
- **Risks:**
  - Shared modules tend to accumulate types from every context, becoming a coupling magnet
- **Where it shows up in code (would have):** A new `shared/` source root or package

## Consequences

### Positive

1. **`ShelfFacade` has zero domain imports.** Its import block contains only `ShelfResponse`, `ShelfSummaryResponse`, and `java.util.*`. The boundary is verifiable by grepping imports.
2. **CLI commands no longer reach into `core.domain.model`.** `BookCirculationCommands`, `BookSearchCommands`, `BookcaseCommands`, and `BookPlacementCommands` import only from `core.ports.inbound.inboundPortModels`.
3. **`ShelfSummary` correctly classified.** It was a read projection, not a domain concept. Its new home (`inboundPortModels.ShelfSummaryResponse`) matches its purpose.
4. **`bookcaseId` is self-contained.** No consumer needs a second domain lookup or a chain through the `Shelf` entity to resolve a shelf's parent bookcase.
5. **Uniform port model convention.** All 4 query methods on `ShelfFacade` (`findShelvesByBookcaseId`, `findShelfById`, `getShelfSummariesForBookcaseByBookcaseId`, `findAll`) return port model types. This consistency reduces cognitive load.
6. **Test isolation improved.** `ShelfServiceTest` no longer mocks `Shelf` to test delegation — it constructs `ShelfResponse` records directly. The test speaks the port's language.
7. **Record accessor consistency.** All consumer code uses idiomatic record accessors (`.id()`, `.shelfLabel()`, `.bookcaseId()`) instead of mixed JavaBean getters.

### Negative and tradeoffs

1. **20-file atomic change.** The coordinated update across 17 source files and 3 test files is large. If any file had been missed, compilation would fail. This is inherent to sealing a port boundary with existing consumers.
2. **`ShelfResponse` constructor is now a 6-argument record.** Adding `bookcaseId` increased the positional parameter count. Future additions will make construction more error-prone. Consider a builder or a factory method if the record grows beyond 7 fields.
3. **Mapping cost on `findShelfById`.** The method now invokes `ShelfPortModelMapper.toShelfResponse(shelf)` inside a `@Transactional` boundary. The cost is negligible (field copying), but it's a behavioral change — previously the raw `Shelf` was returned, and consumers accessed fields directly.
4. **`ShelfMapper.toSummaryFromEntity()` may be dead code.** The infrastructure-layer mapper was updated to return `ShelfSummaryResponse`, but `QueryShelfUseCase.getShelfSummariesForBookcaseById` builds the response inline without using `ShelfMapper`. If `toSummaryFromEntity` has no other callers, it's dead weight.
5. **Null `bookcaseId` propagation.** `Shelf.bookcaseId` is `Long` (nullable). If a shelf lacks a bookcase assignment, `ShelfResponse.bookcaseId()` returns `null`, and consumers like `BookCirculationCommands` pass `null` to `bookcaseFacade.findBookCaseById(null)`. This is pre-existing behavior but is now more visible through the port model.
6. **No `ShelfPortModelMapperTest`.** The mapper that translates `Shelf` → `ShelfResponse` has no dedicated unit test. It is covered indirectly via `QueryShelfUseCaseTest`, but a dedicated test would catch field-mapping regressions.

### Follow-up work

| # | Action | File / Area | Done when |
|---|--------|-------------|-----------|
| 1 | Verify `ShelfMapper.toSummaryFromEntity()` has callers | `ShelfMapper.java` | Method deleted if unused, or caller identified and documented |
| 2 | Create `ShelfPortModelMapperTest` | `src/test/.../mapper/ShelfPortModelMapperTest.java` | All 6 fields (including `bookcaseId`) asserted in a dedicated test |
| 3 | Add `bookcaseId` assertion to `findShelfById` test | `QueryShelfUseCaseTest.java` | Test asserts `result.get().bookcaseId()` equals expected value |
| 4 | Replace bare `.get()` on `Optional<ShelfResponse>` with `.orElseThrow()` | `BookCreateIsbnCommands`, `LibraryCommands`, `BookCreateImportCommands` | No bare `.get()` calls remain on `Optional<ShelfResponse>` |
| 5 | Audit `BookcaseFacade` for same domain leakage pattern | `BookcaseFacade.java` | ADR written if domain types are found in its return signatures |
| 6 | Evaluate consolidating `ShelfDTO` and `ShelfResponse` | `ShelfDTO.java`, `ShelfResponse.java` | Decision documented — either types merged or separation justified |
| 7 | Extract `ShelfSummaryResponse` mapping into `ShelfPortModelMapper` | `QueryShelfUseCase.getShelfSummariesForBookcaseById` | Inline lambda replaced with `ShelfPortModelMapper.toShelfSummaryResponse(shelf)` |

## Compliance and guardrails

### Rules

1. **`ShelfFacade` must not import any `core.domain.model` type.** It returns only types from `core.ports.inbound.inboundPortModels`.
2. **No CLI command or adapter may import `shelf.core.domain.model.*`.** They interact with the shelf context exclusively through `ShelfFacade` and its port models.
3. **Domain-to-port-model mapping happens in `QueryShelfUseCase`**, not in `ShelfService`, not in adapters, not in CLI commands.
4. **Read projections (summaries, views) are port models**, not domain types. They live in `core.ports.inbound.inboundPortModels`, not in `core.domain.model`.

### Violation detection commands

```bash
# Rule 1: ShelfFacade must not import domain types
rg -n "import com\.penrose\.bibby\.library\.stacks\.shelf\.core\.domain" \
  src/main/java/com/penrose/bibby/library/stacks/shelf/core/ports/inbound/ShelfFacade.java

# Rule 2: CLI/adapters must not import shelf domain model
rg -n "import com\.penrose\.bibby\.library\.stacks\.shelf\.core\.domain\.model" \
  src/main/java/com/penrose/bibby/cli/ \
  src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/adapter/

# Rule 3: Tests pass (full suite)
mvn test

# Rule 4: No ShelfSummary remaining in domain.model
rg -rn "ShelfSummary" src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/
```

Expected result for rules 1, 2, and 4: **no output** (zero matches). Rule 3: **142/142 pass**.

## Appendix

### Code pointers (diff anchors)

- **`ShelfFacade.java`**: Removed imports of `Shelf` and `ShelfSummary`. Changed `findShelfById` return from `Optional<Shelf>` → `Optional<ShelfResponse>`. Changed `getShelfSummariesForBookcaseByBookcaseId` return from `List<ShelfSummary>` → `List<ShelfSummaryResponse>`. Added comprehensive Javadoc to all 8 methods. This is the central contract change that drives everything else.

- **`ShelfResponse.java`**: Added `Long bookcaseId` as 6th record component. This is a breaking constructor change — every instantiation site (3 test files, mapper) was updated.

- **`ShelfSummaryResponse.java` (new)**: Created at `core.ports.inbound.inboundPortModels.ShelfSummaryResponse`. Same fields as deleted `ShelfSummary` (`Long shelfId, String label, long bookCount`), but correctly placed as a port model.

- **`ShelfSummary.java` (deleted)**: Removed from `core.domain.model`. Was a read projection misclassified as a domain type.

- **`QueryShelfUseCase.findShelfById`**: Changed from `return Optional.of(shelf)` to `return Optional.of(ShelfPortModelMapper.toShelfResponse(shelf))`. This is the behavioral change — domain-to-port mapping now happens inside the `@Transactional` boundary.

- **`ShelfPortModelMapper.toShelfResponse`**: Added `shelf.getBookcaseId()` as 6th argument. The single mapping point for `Shelf` → `ShelfResponse`.

- **`BookCirculationCommands.java`, `BookSearchCommands.java`**: Replaced `import ...domain.model.Shelf` with `import ...inboundPortModels.ShelfResponse`. Changed `Optional<Shelf>` → `Optional<ShelfResponse>`. Switched from `.getBookcaseId()` / `.getShelfLabel()` to `.bookcaseId()` / `.shelfLabel()`.

- **`BookcaseCommands.java`**: Replaced `import ...domain.model.ShelfSummary` with `import ...inboundPortModels.ShelfSummaryResponse`. Changed loop variable type.

- **`ShelfServiceTest.findShelfById`**: No longer mocks `Shelf` — constructs `ShelfResponse` record directly. Tests the delegation contract in port-model terms.

- **`QueryShelfUseCaseTest`**: Added `when(shelf.getBookcaseId()).thenReturn(...)` stubs to all mocked `Shelf` objects. `findShelfById` test renamed to `findShelfById_shouldReturnShelfResponseWhenFound` and asserts on `ShelfResponse` record fields.

### Candidate ADRs

- **ADR candidate: Consolidate `ShelfDTO` and `ShelfResponse`** — These two types carry nearly identical data (id, position, label, capacity, bookIds, bookcaseId). `ShelfDTO` lives in `shelf.api.dtos` and has `fromEntity` factory methods tied to `ShelfEntity`. Evaluate whether one type can serve both roles or whether the API/port separation is justified.

- **ADR candidate: Apply port model pattern to `BookcaseFacade`** — `BookcaseFacade` likely still returns domain types (e.g., `Bookcase`) from its query methods. The same boundary-sealing pattern applied here should be evaluated for the bookcase context.

- **ADR candidate: Transaction boundary policy for port model mapping** — `findShelfById` maps inside `@Transactional` due to lazy-loaded collections. Document when mapping must occur inside vs. outside a transaction, and whether `@Transactional(readOnly = true)` should be the default for query use cases.
