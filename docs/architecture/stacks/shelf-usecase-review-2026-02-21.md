# Shelf Use Case Architecture Review

**Date:** 2026-02-21
**Branch:** `refactor/rename-shelf-query-method`
**Last Commit:** `4300aaf Add devlog for findAllShelves to findShelvesByBookcaseId rename`
**Scope:** `com.penrose.bibby.library.stacks.shelf.core.application.usecases`

---

## 1) Repo Intake Findings

| Item                  | Value                                                                     |
|-----------------------|---------------------------------------------------------------------------|
| **Repo root**         | `/home/leodpenrose/IdeaProjects/Bibby`                                    |
| **Branch**            | `refactor/rename-shelf-query-method`                                      |
| **Last commit**       | `4300aaf Add devlog for findAllShelves to findShelvesByBookcaseId rename` |
| **Build system**      | Maven (`pom.xml`, `mvnw`)                                                 |
| **Shelf module root** | `src/main/java/com/penrose/bibby/library/stacks/shelf/`                   |
| **Use case dir**      | `.../shelf/core/application/usecases/`                                    |
| **Use case classes**  | `CreateShelfUseCase`, `DeleteShelvesUseCase`, `QueryShelfUseCase`         |
| **Test coverage**     | All 3 use cases + `ShelfService` have dedicated unit tests with Mockito   |

### Framework leakage in usecases

- `@Service` — on all 3 use cases (Spring stereotype)
- `@Transactional` — on `QueryShelfUseCase.findShelfById()` and `DeleteShelvesUseCase.execute()`
- `org.slf4j.Logger` — in `DeleteShelvesUseCase` (logging facade, not infrastructure, but noted)

### Clean areas

- No JPA entities imported in usecases
- No DTOs imported in usecases
- No controller/adapter imports in usecases

---

## 2) Current Shelf Architecture Map

### A) Directory Map

```
shelf/
  api/
    dtos/
      ShelfDTO.java              <- API-layer DTO (record)
      ShelfOptionResponse.java   <- API-layer response DTO (record)
  core/
    application/
      ShelfService.java          <- Inbound port IMPL (facade orchestrator)
      usecases/
        CreateShelfUseCase.java  <- Application use case
        DeleteShelvesUseCase.java<- Application use case
        QueryShelfUseCase.java   <- Application use case
    domain/
      model/
        Shelf.java               <- Domain aggregate/entity
        ShelfSummary.java        <- Domain projection (record)
      valueobject/
        ShelfId.java             <- Value object (record)
    ports/
      inbound/
        ShelfFacade.java         <- Inbound port (interface)
      outbound/
        ShelfDomainRepository.java <- Outbound port (interface)
        BookAccessPort.java      <- Outbound port (cross-BC, interface)
  infrastructure/
    adapter/outbound/
      ShelfDomainRepositoryImpl.java <- Outbound adapter
    entity/
      ShelfEntity.java           <- JPA entity
    mapping/
      ShelfMapper.java           <- Entity<->Domain mapper
    repository/
      ShelfJpaRepository.java    <- Spring Data JPA repo
```

### B) Dependency Direction Diagram

```
                    INBOUND                          OUTBOUND
                    ------                           --------

  ShelfController --+
  CLI Commands -----+
  BookcaseService --+
        |
        v
  ShelfFacade (inbound port, interface)
        |
        v
  ShelfService (implements ShelfFacade)
        |
        +---> CreateShelfUseCase  ---> ShelfDomainRepository (port)
        +---> QueryShelfUseCase   ---> ShelfDomainRepository (port)
        +---> DeleteShelvesUseCase---> ShelfDomainRepository (port)
                                  ---> BookAccessPort (port)
                                          |
                          +---------------+
                          v
              ShelfDomainRepositoryImpl ---> ShelfJpaRepository
                                        ---> ShelfMapper
                                        ---> BookAccessPort (!!!)
              BookAccessAdapter ---> BookDomainRepository
```

**Dependency rule compliance:** Mostly correct. Use cases only depend on domain types and outbound port interfaces.
Issues detailed below.

---

## 3) Use Case Reviews

### 3a) CreateShelfUseCase

**File:** `shelf/core/application/usecases/CreateShelfUseCase.java`

#### Intent & boundary

Validates inputs and delegates persistence of a new Shelf. Legitimate application-layer command use case.

#### Inputs/Outputs

- **Inputs:** `Long bookcaseId, int position, String shelfLabel, int bookCapacity` — all primitives. No command object.
- **Output:** `void` — fire-and-forget.
- No leakage of web/persistence types.

#### Dependencies

| Dependency              | Category      | OK?      |
|-------------------------|---------------|----------|
| `ShelfDomainRepository` | Outbound Port | Yes      |
| `@Service`              | Framework     | **Flag** |

#### Transactionality & consistency

- No `@Transactional` — single `save()` call, so acceptable.
- No multi-step atomicity concern.

#### Naming & language

- "CreateShelfUseCase" — reflects business intent well.
- `execute()` method name is fine for a single-method use case.

#### Issues

1. **Duplicated validation logic.** The `Shelf` domain constructor already validates `shelfLabel`, `shelfPosition`, and
   `bookCapacity` with identical rules. The use case duplicates all four checks. This violates "domain owns its
   invariants" and creates dual-maintenance risk.

2. **Repository `save()` bypasses domain object construction.** The port signature is
   `save(Long bookcaseId, int position, String shelfLabel, int bookCapacity)` — raw primitives. The use case never
   creates a `Shelf` domain object. The entity is constructed directly in `ShelfDomainRepositoryImpl.save()`. This
   means:
    - Domain invariants in the `Shelf` constructor are never exercised during creation.
    - The use case is a "transaction script" rather than domain-centric.
    - The outbound port's contract leaks persistence concerns (it knows how to decompose a shelf into columns).

3. **No command object.** Four primitive parameters is at the threshold where a `CreateShelfCommand` record would
   improve readability and extensibility.

#### Recommendations

1. **(High)** Change the repository port to accept a `Shelf` domain object: `void save(Shelf shelf)`. Construct the
   `Shelf` inside the use case, letting the domain enforce invariants. Remove the duplicated validation from the use
   case.
2. **(Medium)** Extract a `CreateShelfCommand` record in `core/application/usecases/` (or a `commands/` subpackage) to
   replace the 4 primitives.
3. **(Low)** Remove `@Service` from the use case (wire it via explicit `@Bean` in a config class in infrastructure, or
   accept the pragmatic tradeoff).

---

### 3b) QueryShelfUseCase

**File:** `shelf/core/application/usecases/QueryShelfUseCase.java`

#### Intent & boundary

Provides read-only query methods for shelves. Application-layer query use case.

#### Inputs/Outputs

- **Inputs:** `Long bookcaseId`, `Long shelfId` — primitive IDs.
- **Outputs:** `List<Shelf>`, `Optional<Shelf>`, `List<ShelfSummary>` — domain objects.
- No DTO leakage in this class.

#### Dependencies

| Dependency              | Category      | OK?      |
|-------------------------|---------------|----------|
| `ShelfDomainRepository` | Outbound Port | Yes      |
| `@Service`              | Framework     | **Flag** |
| `@Transactional`        | Framework     | **Flag** |

#### Transactionality & consistency

- `@Transactional` on `findShelfById()` only. This is a read operation; if needed for lazy-loading, it suggests the JPA
  session concern is bleeding through the port abstraction. Since `ShelfDomainRepository.getById()` should return a
  fully-hydrated `Shelf` domain object, `@Transactional` here is suspicious.
- `findShelvesByBookcaseId()` and `findAll()` have no `@Transactional` — inconsistent.

#### Naming & language

- "QueryShelfUseCase" is a "catch-all query bag" — it bundles 4 different query methods. In CQRS-leaning architectures,
  each query would be its own handler. Currently acceptable for a small domain, but as queries grow this will become a
  god class.
- `getShelfSummariesForBookcase()` does mapping logic (Shelf -> ShelfSummary) inside the use case. This could be argued
  as application-layer projection, which is borderline OK, but `ShelfSummary` could also be produced directly by a
  dedicated read-model query on the repository.

#### Issues

1. **`@Transactional` on a query use case method.** The adapter already returns fully-hydrated domain objects (
   `ShelfDomainRepositoryImpl` eagerly loads books). The annotation is unnecessary. Its presence suggests either a bug
   or leftover from when lazy loading was used.

2. **`getShelfSummariesForBookcase()` does in-memory mapping** — fetches full `Shelf` objects (including all books via
   cross-BC call), then maps to `ShelfSummary`. This is an N+1-flavored inefficiency: each shelf triggers a
   `bookAccessPort.getBookIdsByShelfId()` call inside the adapter, but the summary only needs `bookCount`, not the full
   ID list. A dedicated repository port method like `findShelfSummariesByBookcaseId()` would be more efficient.

3. **`findAll()` without pagination.** This will become a problem at scale.

4. **Inconsistent use of `ShelfId` value object.** `findShelfById()` wraps `Long` into `ShelfId` inside the use case,
   while `findShelvesByBookcaseId()` uses raw `Long bookcaseId`. No `BookcaseId` value object exists.

#### Recommendations

1. **(High)** Remove `@Transactional` from `findShelfById()` — verify the adapter returns fully-hydrated domain
   objects (it does). If some future lazy-load need arises, the transaction boundary belongs in the adapter, not the use
   case.
2. **(Medium)** Add a `findShelfSummariesByBookcaseId(Long)` method to `ShelfDomainRepository` port that returns
   `List<ShelfSummary>` directly, avoiding the N+1 cross-BC calls. Move the mapping into the outbound adapter.
3. **(Low)** Consider splitting into `FindShelfByIdUseCase`, `FindShelvesByBookcaseUseCase`, etc., if the query count
   grows beyond 4-5.

---

### 3c) DeleteShelvesUseCase

**File:** `shelf/core/application/usecases/DeleteShelvesUseCase.java`

#### Intent & boundary

Deletes all shelves in a bookcase, first cascading deletion of books on those shelves via the cross-BC `BookAccessPort`.
Legitimate application-layer orchestration use case.

#### Inputs/Outputs

- **Input:** `Long bookcaseId` — primitive.
- **Output:** `void`.

#### Dependencies

| Dependency              | Category                 | OK?        |
|-------------------------|--------------------------|------------|
| `ShelfDomainRepository` | Outbound Port            | Yes        |
| `BookAccessPort`        | Outbound Port (cross-BC) | Yes        |
| `Logger` (SLF4J)        | Logging facade           | Acceptable |
| `@Service`              | Framework                | **Flag**   |
| `@Transactional`        | Framework                | **Flag**   |

#### Transactionality & consistency

- `@Transactional` is correctly placed here — this is a multi-step mutation (delete books, then delete shelves) that
  must be atomic.
- **However**, `BookAccessPort.deleteBooksOnShelves()` crosses a bounded context boundary. If the Book module uses a
  separate database/transaction manager, this `@Transactional` will NOT provide atomicity across both operations. This
  is a latent consistency risk. Fine for now (monolith, single DB) but architecturally fragile.

#### Naming & language

- "DeleteShelvesUseCase" — clear and correct.
- `execute()` method name is fine for a single-action use case.

#### Issues

1. **Cross-BC deletion ordering risk.** The use case first deletes books via `bookAccessPort`, then deletes shelves. If
   the shelf deletion fails after books are deleted, data is inconsistent. The `@Transactional` helps only if both
   operations share the same transaction. Fine for now (monolith, single DB) but should be documented as a known
   coupling point.

2. **Logger instantiation.** `private final Logger logger = LoggerFactory.getLogger(...)` — fine, but convention is
   `private static final Logger`. Minor.

#### Recommendations

1. **(Medium)** Add a comment/ADR documenting that this cross-BC deletion assumes a shared transaction boundary. If you
   ever split into microservices, this becomes a saga.
2. **(Low)** Make logger `private static final`.

---

## 4) Cross-Cutting Smells

### Smell 1: Anemic Domain / Fat Repository

The `Shelf` domain object is essentially a data carrier with validation in its constructor and setters. The
`CreateShelfUseCase` never constructs a `Shelf` — it passes primitives directly to the repository port, which constructs
a `ShelfEntity` in the adapter. **The domain model is bypassed during creation.** This is the single most significant
DDD violation: the aggregate's factory/constructor invariants are not exercised on the write path.

The `ShelfDomainRepository.save(Long, int, String, int)` signature is the root cause — it accepts decomposed primitives
rather than a domain object.

### Smell 2: `@Service` and `@Transactional` in Core

All three use cases are annotated with `@Service` (Spring) and two use `@Transactional` (Spring). This couples the
core/application layer to the Spring framework. In strict hexagonal, the core should be framework-free, with Spring
wiring done in a configuration adapter. **Pragmatic tradeoff:** In a Spring Boot monolith, this is extremely common and
arguably acceptable. If you plan to enforce framework-free core, this should be addressed. If not, acknowledge it as a
conscious decision.

### Smell 3: `ShelfDTO` imports `ShelfEntity`

`ShelfDTO.java:3` — `import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity`. The DTO in
`api/dtos/` has a direct dependency on the infrastructure entity. This violates layering — the API layer should not know
about persistence entities. The `fromEntity()` and `fromEntityWithBookId()` static factory methods on `ShelfDTO` are the
culprits.

### Smell 4: `ShelfMapper` lives in infrastructure but knows about `ShelfDTO`

`ShelfMapper.java:3` — `import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO`. The infrastructure mapper
imports an API-layer DTO. This creates a bidirectional dependency between `api/` and `infrastructure/`. The mapper
should either:

- Live in `api/` for DTO mapping, and a separate mapper in `infrastructure/` for entity mapping.
- Or the DTO mapping methods should be extracted out.

### Smell 5: `ShelfFacade` (inbound port) returns domain objects to callers

`ShelfFacade` returns `Shelf` and `ShelfSummary` domain objects. External consumers (controllers, CLI commands, the Book
BC's `ShelfAccessPortAdapter`) then access domain internals like `shelf.getShelfId().shelfId()`, `shelf.getBookIds()`,
etc. This leaks domain model details outside the bounded context. In strict hexagonal:

- The inbound port should return DTOs or dedicated response types.
- OR consumers should only depend on the DTO layer, not the domain.

Currently, `ShelfResponseMapper` in the controller layer and `ShelfAccessPortAdapter` in the Book BC both manually map
`Shelf` to DTOs. This mapping is repeated in multiple places.

### Smell 6: Cross-BC coupling via `ShelfDTO`

The Book bounded context's **core** layer (`ShelfAccessPort.java:3`) directly imports
`com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO`. A BC's core should not depend on another BC's types. The
Book BC should define its own representation of what it needs from a shelf (e.g., `BookShelfView` owned by the Book BC).

### Smell 7: `ShelfDomainRepositoryImpl` depends on `BookAccessPort`

`ShelfDomainRepositoryImpl.java:19` — the outbound repository adapter has a dependency on `BookAccessPort` to hydrate
book IDs into `Shelf` domain objects. This means every shelf query (even those that don't need book data) triggers
cross-BC calls. This is:

- An N+1 performance risk (each shelf -> 1 call for its books).
- A violation of separation of concerns: the repository adapter should only handle persistence, not cross-BC
  orchestration.
- The hydration of `books` should happen either in the use case or as a separate enrichment step.

### Smell 8: `ShelfFacade` parameter naming

`ShelfFacade.java:18` — `createShelf(Long bookcaseId, int position, String s, int bookCapacity)`. The parameter is named
`s` instead of `shelfLabel`. Minor code quality issue.

---

## 5) Recommended Refactor Plan

### Step 1: Fix `ShelfDTO` -> `ShelfEntity` dependency

**Priority:** P0 (High)

- **Files to change:** `ShelfDTO.java`
- **What to do:** Remove the `fromEntity()` and `fromEntityWithBookId()` static factory methods from `ShelfDTO`. Move
  them to `ShelfMapper` in infrastructure (they already exist there as `toDTOFromEntity()`). Update any callers of
  `ShelfDTO.fromEntity()` / `ShelfDTO.fromEntityWithBookId()` to use the mapper instead.
- **Why:** API layer must not depend on infrastructure entities.
- **Verify:** `./mvnw test -Dtest="*Shelf*"`

### Step 2: Fix `ShelfFacade` parameter name `s` -> `shelfLabel`

**Priority:** P2 (Low)

- **Files to change:** `ShelfFacade.java`
- **What to do:** Rename parameter `String s` to `String shelfLabel` on line 18.
- **Why:** Code readability, ubiquitous language.
- **Verify:** `./mvnw compile`

### Step 3: Remove `@Transactional` from `QueryShelfUseCase.findShelfById()`

**Priority:** P2 (Low)

- **Files to change:** `QueryShelfUseCase.java`
- **What to do:** Remove `@Transactional` annotation from `findShelfById()` (line 25). The adapter already returns a
  fully-hydrated domain object.
- **Why:** Framework annotation in core on a read path that does not need transactional semantics. If transactional is
  needed, it belongs on the adapter method.
- **Verify:** `./mvnw test -Dtest="QueryShelfUseCaseTest"`

### Step 4: Change `ShelfDomainRepository.save()` to accept a `Shelf` domain object

**Priority:** P0 (High)

- **Files to change:**
    - `ShelfDomainRepository.java` — change `save(Long, int, String, int)` to `save(Shelf shelf)`
    - `CreateShelfUseCase.java` — construct `Shelf` domain object, pass to `save()`
    - `ShelfDomainRepositoryImpl.java` — update `save()` impl to accept `Shelf`, use mapper to convert to entity
    - `CreateShelfUseCaseTest.java` — update test expectations
- **What to do:**
    1. Change port: `void save(Shelf shelf);`
    2. In `CreateShelfUseCase.execute()`: construct `Shelf` with a null/placeholder `ShelfId` (since ID is generated),
       empty `books` list, and the provided parameters. Remove the duplicated validation (the domain constructor handles
       it).
    3. In `ShelfDomainRepositoryImpl.save()`: use `shelfMapper.toEntity(shelf)` then `jpaRepository.save(entity)`.
- **Why:** Enforces domain invariants on the write path. Eliminates duplicated validation. Aligns with DDD — the use
  case orchestrates domain objects, not primitives.
- **Verify:** `./mvnw test -Dtest="CreateShelfUseCaseTest,ShelfServiceTest"`

### Step 5: Extract `BookAccessPort` usage out of `ShelfDomainRepositoryImpl`

**Priority:** P1 (Medium)

- **Files to change:**
    - `ShelfDomainRepositoryImpl.java` — remove `BookAccessPort` dependency; return `Shelf` objects without `books` (or
      with empty list)
    - `QueryShelfUseCase.java` — add `BookAccessPort` dependency; enrich `Shelf` objects with books after repository
      call where needed
    - Or better: add a new outbound port method like `findShelfSummariesByBookcaseId()` that does not need book data at
      all
- **What to do:** The repository adapter should only do persistence. Cross-BC data enrichment (fetching books from the
  Book BC) should be orchestrated by the use case layer.
- **Why:** Single Responsibility for the adapter. Eliminates N+1 cross-BC calls for queries that don't need book data.
  Makes the dependency graph cleaner.
- **Verify:** `./mvnw test -Dtest="*Shelf*"`

### Step 6: Split `ShelfMapper` into API mapper and infrastructure mapper

**Priority:** P1 (Medium)

- **Files to change:**
    - Keep `infrastructure/mapping/ShelfMapper.java` — only entity-to-domain methods
    - Create `api/mapping/ShelfDtoMapper.java` (or let controller-side mappers handle it) — domain-to-DTO methods
    - Remove DTO-related methods from the infrastructure `ShelfMapper`
- **What to do:** Infrastructure mapper should only know about `ShelfEntity` and `Shelf`. DTO mapping should be in the
  API layer.
- **Why:** Eliminates the `infrastructure` -> `api` cross-dependency in `ShelfMapper`.
- **Verify:** `./mvnw compile && ./mvnw test`

### Step 7: Make Book BC own its own shelf representation

**Priority:** P1 (Medium)

- **Files to change:**
    - `ShelfAccessPort.java` (Book BC) — replace `ShelfDTO` with a Book-BC-owned type like `ShelfInfo`
    - `ShelfAccessPortAdapter.java` — map from `Shelf`/`ShelfFacade` output to `ShelfInfo`
    - Book BC callers — update references
- **What to do:** Define `com.penrose.bibby.library.cataloging.book.core.port.outbound.ShelfInfo` record with only the
  fields the Book BC actually needs. The adapter maps into it.
- **Why:** Eliminates cross-BC coupling at the core level. Book BC's core should not import anything from Shelf's
  packages.
- **Verify:** `./mvnw test`

---

## 6) Proposed Target Structure

Based on the current codebase, here is the clean target:

```
shelf/
  api/
    dtos/
      ShelfDTO.java                  <- Record, NO entity imports
      ShelfOptionResponse.java       <- Record
    mapping/
      ShelfDtoMapper.java            <- NEW: Domain -> DTO mapping

  core/
    application/
      ShelfService.java              <- Implements ShelfFacade, delegates to use cases
      usecases/
        CreateShelfUseCase.java      <- Accepts Shelf domain object (or CreateShelfCommand)
        DeleteShelvesUseCase.java    <- Orchestrates cross-BC deletion
        QueryShelfUseCase.java       <- Pure delegation (no @Transactional)
      commands/                      <- OPTIONAL
        CreateShelfCommand.java      <- Record with (bookcaseId, position, label, capacity)
    domain/
      model/
        Shelf.java                   <- Aggregate root, owns invariants
        ShelfSummary.java            <- Read projection
      valueobject/
        ShelfId.java                 <- Typed ID
    ports/
      inbound/
        ShelfFacade.java             <- Interface (fixed param name)
      outbound/
        ShelfDomainRepository.java   <- save(Shelf), find*, delete* -- NO BookAccessPort usage
        BookAccessPort.java          <- Cross-BC port, owned by Shelf

  infrastructure/
    adapter/
      outbound/
        ShelfDomainRepositoryImpl.java <- Only persistence, NO BookAccessPort dep
    entity/
      ShelfEntity.java               <- JPA entity
    mapping/
      ShelfMapper.java               <- Only Entity <-> Domain (NO DTO methods)
    repository/
      ShelfJpaRepository.java        <- Spring Data JPA
```

### Key changes from current state

1. `ShelfDTO` loses its entity import
2. `ShelfMapper` in infrastructure loses DTO methods; a new `ShelfDtoMapper` in `api/mapping/` handles DTO conversion
3. `ShelfDomainRepositoryImpl` loses its `BookAccessPort` dependency — cross-BC enrichment moves to use case layer
4. `ShelfDomainRepository.save()` accepts `Shelf` domain object
5. Use cases lose `@Transactional` where unnecessary (keep only on `DeleteShelvesUseCase`)
6. (Optional) `CreateShelfCommand` record added for cleaner use case inputs

### What stays the same

- Overall 3-ring layout (`api/`, `core/`, `infrastructure/`)
- `ShelfFacade` as inbound port in `core/ports/inbound/`
- `BookAccessPort` as outbound port in `core/ports/outbound/`
- `ShelfService` as facade implementation in `core/application/`
- The use case class names and the general delegation pattern

---

## Severity Summary

| Priority        | Issue                                                               | Impact                                  |
|-----------------|---------------------------------------------------------------------|-----------------------------------------|
| **P0 (High)**   | `save()` bypasses domain model; duplicated validation               | DDD violation, dual-maintenance         |
| **P0 (High)**   | `ShelfDTO` imports `ShelfEntity`                                    | Layer violation                         |
| **P1 (Medium)** | `ShelfDomainRepositoryImpl` depends on `BookAccessPort` (N+1 + SRP) | Performance, coupling                   |
| **P1 (Medium)** | `ShelfMapper` cross-references api and infrastructure               | Bidirectional dependency                |
| **P1 (Medium)** | Book BC core imports Shelf's `ShelfDTO`                             | Cross-BC coupling                       |
| **P2 (Low)**    | `@Service`/`@Transactional` in core                                 | Framework coupling (pragmatic tradeoff) |
| **P2 (Low)**    | `ShelfFacade` param named `s`                                       | Readability                             |
| **P2 (Low)**    | `QueryShelfUseCase.findShelfById` has unnecessary `@Transactional`  | Misleading                              |
