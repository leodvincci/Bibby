# ADR 0004: Introduce Inbound Port Models to Replace Domain Object Leakage Through ShelfFacade

Date: 2026-02-24

Status: Accepted

Deciders: Unknown

Technical Story: N/A (branch `refactor/shelf-port-response-model`)

---

## Context

- **The inbound port leaked domain internals.** `ShelfFacade.findShelvesByBookcaseId()` returned `List<Shelf>`, exposing
  the full domain model — mutable state, domain behavior (`isFull()`, `getBookCount()`), and nested value objects (
  `ShelfId`) — to every consumer: CLI (`PromptOptions`), web (`ShelfResponseMapper`), and tests.

- **Consumers were structurally coupled to the domain model.** `PromptOptions` navigated two levels deep to extract the
  shelf ID: `shelf.getShelfId().shelfId()`. Any rename, restructure, or addition of validation to `Shelf` would break
  the CLI, web, and test layers simultaneously.

- **This contradicted the pattern established in ADR 0003.** ADR 0003 purged `BookcaseEntity` and `BookcaseDTO` from the
  Bookcase inbound port, returning the domain `Bookcase` type directly. However, it noted that adapters should own DTO
  mapping. The Shelf module took this further: rather than returning a raw domain object (which still leaks structure),
  it introduces a dedicated **inbound port model** — a read-only projection owned by the port contract itself.

- **Constraints that mattered:**
    - The project follows hexagonal architecture; the core must not depend on infrastructure types.
    - The inverse matters too: outer layers should depend on port contracts, not on domain internals.
    - The Shelf domain model uses `ShelfId` (a value object wrapping `Long`). Consumers outside the core should not need
      to know about this type.
    - The refactor must be incremental — not all `ShelfFacade` methods can change at once.

- **Evidence:**
    - Commits:
        - `a974cee` — Introduce ShelfResponse port model for inbound port boundary
        - `94daea4` — Update consumers and tests to use ShelfResponse port model
    - Key files:
        - `ShelfResponse.java` (new) — `ports/inbound/inboundPortModels/`
        - `ShelfPortModelMapper.java` (new) — `ports/inbound/mapper/`
        - `ShelfFacade.java` — signature change on `findShelvesByBookcaseId`
        - `QueryShelfUseCase.java` — mapping added at use-case level
        - `PromptOptions.java` — consumer migrated off `Shelf` domain type
        - `ShelfResponseMapper.java` — new overload accepting `ShelfResponse`

---

## Decision

- **I will introduce `ShelfResponse`, an immutable Java record, as the inbound port model for shelf query results.** It
  lives at `shelf.core.ports.inbound.inboundPortModels.ShelfResponse` and contains only flat primitives and a
  `List<Long>` — no value objects, no domain behavior.

- **I will map `Shelf` -> `ShelfResponse` inside the application layer (use case), not at the adapter boundary.**
  `QueryShelfUseCase.findShelvesByBookcaseId()` applies `ShelfPortModelMapper::toShelfResponse` after fetching from the
  repository. The domain `Shelf` never crosses the inbound port.

- **I will migrate one facade method at a time.** Only `findShelvesByBookcaseId` changes in this iteration. Other
  methods (`findShelfById`, `findAll`, `isFull`, `isEmpty`) continue to return domain types until follow-up work.

**Boundary and ownership rules introduced:**

- The inbound port (`ShelfFacade`) owns its return types. Port models live in `ports/inbound/inboundPortModels/`.
- The port-model mapper lives in `ports/inbound/mapper/` and depends inward on the domain model. This is legal: port
  contracts may know about the domain; the domain must not know about ports.
- Adapters (CLI, web) depend only on port models for query results. They must not import domain types for data they
  receive through the inbound port.

---

## Decision drivers

1. **Consumer coupling to `ShelfId` value object.** `PromptOptions` called `shelf.getShelfId().shelfId()` — two levels
   of indirection to get a `Long`. Evidence: `PromptOptions.java` diff in `94daea4`, lines replacing
   `.getShelfId().shelfId()` with `.id()`.

2. **Domain model changes ripple to outer layers.** If `Shelf` adds a constructor parameter, changes `getBookIds()`
   return type, or renames a getter, every consumer breaks. The port model absorbs this: only `ShelfPortModelMapper`
   needs updating.

3. **Test complexity.** Tests constructed `Shelf` objects with 6-arg constructors including `ShelfId` value objects just
   to test CLI prompt logic. After: `new ShelfResponse(10L, 1, "Top Shelf", 10, List.of(100L, 101L))`. Evidence:
   `PromptOptionsTest.java` diff in `94daea4`.

4. **Inconsistency with hexagonal intent.** The outbound port already maps between `ShelfEntity` and `Shelf` (via
   `ShelfMapper`). But the inbound port was passing raw domain objects outward — asymmetric hygiene. This decision
   closes that gap for query paths.

5. **Behavioral encapsulation at risk.** Returning `Shelf` means consumers can call `shelf.isFull()`,
   `shelf.getBookCount()`, etc. Port models expose only data, forcing consumers to compute derived values themselves or
   request them through dedicated port methods.

6. **Incremental migration path required.** Changing all 8 `ShelfFacade` methods at once would be a large, risky PR. The
   per-method approach lets each change be reviewed and tested independently.

---

## Considered options

### Option A: Inbound port model record (chosen)

- **Description:** Create a `ShelfResponse` record in `ports/inbound/inboundPortModels/`. Map `Shelf` -> `ShelfResponse`
  in the use case. `ShelfFacade` returns `ShelfResponse` for query methods.
- **Pros:**
    - Consumers depend on a stable, flat contract — immune to domain model changes
    - Records provide immutability, `equals`/`hashCode`, compact construction
    - Mapping at the use-case level keeps domain objects inside core
    - Incremental: one method at a time
- **Cons:**
    - Adds a new type and mapper class per migrated method
    - Temporary dual overloads in infrastructure mappers (e.g., `ShelfResponseMapper`)
    - `books` list is not defensively copied
- **Risks:**
    - If the record proliferates unchecked, the `inboundPortModels` package could become a parallel DTO layer
- **Where it shows up in code:**
    - `ShelfResponse.java`, `ShelfPortModelMapper.java`, `QueryShelfUseCase.java:29-33`, `ShelfFacade.java:11`

### Option B: Return domain `Shelf` directly (status quo)

- **Description:** Keep `ShelfFacade.findShelvesByBookcaseId()` returning `List<Shelf>`. Let consumers access domain
  getters.
- **Pros:**
    - Zero new types, zero mapping overhead
    - Simpler short-term — no mapper to maintain
- **Cons:**
    - Every consumer couples to `Shelf` internals (`ShelfId`, getter naming, list mutability)
    - Domain model changes require updating CLI, web, and test layers
    - Consumers can invoke domain behavior (`isFull()`, `getBookCount()`) which shouldn't be available at the adapter
      level
- **Risks:**
    - Growing coupling makes future `Shelf` refactors increasingly expensive
- **Where it shows up in code:**
    - Pre-branch state of `ShelfFacade.java:11`, `PromptOptions.java:51-56`

### Option C: Map at the adapter boundary (DTO in adapter layer)

- **Description:** Keep `ShelfFacade` returning `Shelf`, but have each adapter (CLI, web) map `Shelf` ->
  adapter-specific DTO at its own boundary.
- **Pros:**
    - Each adapter controls its own projection
    - No new types in core
- **Cons:**
    - Domain `Shelf` still crosses the port boundary — consumers still see it
    - Duplication: each adapter writes its own mapper for the same source type
    - Does not prevent accidental domain-behavior coupling in adapter code
- **Risks:**
    - Adapter mappers diverge over time, creating inconsistent views of the same data
- **Where it shows up in code:**
    - Would affect `ShelfResponseMapper.java` (web) and `PromptOptions.java` (CLI) independently

### Option D: Sealed interface / shared DTO module

- **Description:** Create a shared `ShelfView` interface or DTO in a separate module that both core and adapters depend
  on.
- **Pros:**
    - Single contract for all consumers
    - Could be reused across bounded contexts
- **Cons:**
    - Introduces a shared-kernel dependency — couples modules that should be independent
    - Over-engineered for a single-module scenario
    - Java records can't implement sealed interfaces with fields conveniently
- **Risks:**
    - Shared module becomes a dependency magnet
- **Where it shows up in code:**
    - Would require a new `shelf-api` or `shelf-contract` module

---

## Consequences

### Positive

1. **`PromptOptions` (CLI) no longer imports `Shelf` or `ShelfId`.** The CLI layer depends only on `ShelfResponse` from
   the port contract. Evidence: removed imports in `PromptOptionsTest.java` (`94daea4`).

2. **Test construction is dramatically simpler.** `new ShelfResponse(10L, 1, "Top Shelf", 10, List.of(100L, 101L))` vs.
   `new Shelf("Top Shelf", 1, 10, new ShelfId(10L), List.of(100L, 101L), 1L)`.

3. **Domain model can evolve independently.** Adding fields to `Shelf`, renaming getters, or changing `ShelfId`
   internals will not break consumers — only `ShelfPortModelMapper` needs updating.

4. **`QueryShelfUseCaseTest` now tests real behavior.** Before: asserted the same mock objects came back. After: asserts
   that specific field values map correctly from `Shelf` to `ShelfResponse`.

5. **Establishes a repeatable pattern.** Future port-model migrations for `findShelfById`, `findAll`, and
   `BookcaseFacade` methods have a clear template to follow.

6. **Behavioral leakage eliminated for this path.** Consumers can no longer accidentally call `shelf.isFull()` or
   `shelf.getBookCount()` on query results.

### Negative and tradeoffs

1. **Dual overloads in `ShelfResponseMapper`.** The web mapper now has both `toShelfOption(Shelf)` and
   `toShelfOption(ShelfResponse)`. The old overload is dead weight once all facade methods are migrated, but for now it
   must remain.

2. **Partial migration creates inconsistency.** `ShelfFacade` now returns `ShelfResponse` from `findShelvesByBookcaseId`
   but `Shelf` from `findShelfById`, `findAll`, `isFull`, and `isEmpty`. Consumers must reason about which methods
   return which type.

3. **No defensive copy of `books`.** `ShelfPortModelMapper` passes `shelf.getBookIds()` directly into the record. If the
   domain list is mutable, downstream consumers could theoretically mutate it. A `List.copyOf()` would harden this.

4. **Port model is a projection, not a contract guarantee.** If a consumer needs a field that `ShelfResponse` doesn't
   expose, the record must be extended and the mapper updated. There's no compiler enforcement that "all consumers have
   enough data."

5. **Mapping overhead at the use-case level.** Every `findShelvesByBookcaseId` call now streams and maps. For the
   current scale this is negligible, but it's a pattern to be aware of for hot paths.

6. **`inboundPortModels` package naming is non-standard.** Most hexagonal codebases use `dto`, `response`, or `model`.
   The camelCase package name and the `inboundPortModels` naming is verbose. This is a style concern, not a correctness
   issue.

### Follow-up work

| # | Action                                                      | File / Area                                                   | Done when                                                                                     |
|---|-------------------------------------------------------------|---------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| 1 | Add `ShelfPortModelMapperTest`                              | `test/.../ports/inbound/mapper/ShelfPortModelMapperTest.java` | Unit tests cover all fields including null/empty `books`                                      |
| 2 | Add defensive `List.copyOf()` for `books`                   | `ShelfPortModelMapper.java:14`                                | `books` in `ShelfResponse` is guaranteed immutable                                            |
| 3 | Migrate `findShelfById` to return `Optional<ShelfResponse>` | `ShelfFacade.java:13`, `QueryShelfUseCase.java:43`            | `ShelfFacade` no longer returns `Optional<Shelf>`                                             |
| 4 | Migrate `findAll` to return `List<ShelfResponse>`           | `ShelfFacade.java:22`, `QueryShelfUseCase.java:72`            | `ShelfFacade.findAll()` returns port model                                                    |
| 5 | Migrate `isFull`/`isEmpty` or keep as primitive returns     | `ShelfFacade.java:24-26`                                      | Decision documented: these return `boolean`, so no domain type leaks — may not need migration |
| 6 | Remove old `toShelfOption(Shelf)` overload                  | `ShelfResponseMapper.java:11-17`                              | Only `toShelfOption(ShelfResponse)` remains; old overload deleted                             |
| 7 | Add `ShelfResponseMapper` test for `ShelfResponse` overload | `test/.../web/.../ShelfResponseMapperTest.java`               | `hasSpace` edge case (books.size == bookCapacity) covered                                     |
| 8 | Apply pattern to `BookcaseFacade`                           | `BookcaseFacade.java`                                         | `BookcaseFacade` query methods return `BookcaseResponse` port model                           |
| 9 | Evaluate `inboundPortModels` package naming                 | `ports/inbound/inboundPortModels/`                            | Team agrees on naming convention (e.g., `response/` or `model/`)                              |

---

## Compliance and guardrails

**Rules that prevent regression:**

1. **Adapters must not import domain model types for data received through inbound ports.** If `ShelfFacade` returns
   `ShelfResponse`, the CLI and web layers must not also import `Shelf` for the same data path.

2. **Port models must be immutable.** Use Java records. No setters, no mutable collections.

3. **Mapping happens at the use-case or service level, not in adapters.** The port defines what comes out; the use case
   handles the translation.

4. **Domain types must not appear in port-model records.** `ShelfResponse` uses `Long`, not `ShelfId`. Port models speak
   in primitives and standard library types.

**How to detect violations:**

```bash
# Verify CLI layer doesn't import Shelf domain model
rg -n "import com\.penrose\.bibby\.library\.stacks\.shelf\.core\.domain\.model\.Shelf;" \
  src/main/java/com/penrose/bibby/cli/

# Verify no domain value objects leak into port models
rg -n "import.*\.domain\.valueobject\." \
  src/main/java/com/penrose/bibby/library/stacks/shelf/core/ports/inbound/inboundPortModels/

# Verify no infrastructure imports in core ports
rg -n "import.*\.infrastructure\." \
  src/main/java/com/penrose/bibby/library/stacks/shelf/core/

# Run all shelf-related tests
mvn test -pl . -Dtest="ShelfServiceTest,QueryShelfUseCaseTest,PromptOptionsTest" -DfailIfNoTests=false
```

---

## Appendix

### Code pointers (diff anchors)

| File                                | What changed                                                          | Why it matters                                                   |
|-------------------------------------|-----------------------------------------------------------------------|------------------------------------------------------------------|
| `ShelfResponse.java` (new)          | 6-line record at `ports/inbound/inboundPortModels/`                   | The new port contract type — flat, immutable, no domain types    |
| `ShelfPortModelMapper.java` (new)   | Static `toShelfResponse(Shelf)` method                                | Single translation point from domain to port model               |
| `ShelfFacade.java:11`               | `List<Shelf>` -> `List<ShelfResponse>`                                | The interface contract change that drives all downstream updates |
| `QueryShelfUseCase.java:29-33`      | Added `.stream().map(ShelfPortModelMapper::toShelfResponse).toList()` | Mapping happens at use-case level, before crossing the port      |
| `ShelfService.java:35`              | Return type change to `List<ShelfResponse>`                           | Delegation layer aligns with facade contract                     |
| `PromptOptions.java:51-56, 171-176` | `shelf.getShelfId().shelfId()` -> `shelf.id()`                        | Consumer decoupled from `ShelfId` value object                   |
| `ShelfResponseMapper.java:20-27`    | New `toShelfOption(ShelfResponse)` overload                           | Web adapter can consume the new port model type                  |
| `PromptOptionsTest.java:88-89`      | `Shelf` + `ShelfId` construction -> `ShelfResponse` record            | Test decoupled from domain internals                             |
| `QueryShelfUseCaseTest.java:28-40`  | Mock `Shelf` getters, assert `ShelfResponse` fields                   | Tests now verify mapping correctness, not just delegation        |

### Candidate ADRs

- **ADR candidate: Standardize `inboundPortModels` package naming convention** — The camelCase package name diverges
  from Java conventions (`lowercase.only`). As more port models are added across modules, a consistent naming policy (
  e.g., `response/`, `model/`, `contract/`) prevents fragmentation.

- **ADR candidate: Defensive copying policy for collections in port models** — `ShelfResponse.books()` currently holds
  the same list reference from the domain. A project-wide rule on whether port-model mappers must `List.copyOf()` would
  prevent subtle mutation bugs.

- **ADR candidate: Migrate `ShelfFacade` boolean methods (`isFull`, `isEmpty`) to a dedicated query port** — These
  methods return primitives and don't leak domain types, but they encode domain logic at the port level. A dedicated
  `ShelfCapacityQuery` port would separate data queries from business-rule queries.
