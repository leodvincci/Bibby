# ADR 0003: Bookcase Domain Model Enrichment, Port Purge of Infrastructure Types, and Use-Case Extraction

Date: 2026-02-24

Status: Accepted

Deciders: Unknown

Technical Story: N/A

---

## Context

- **What problem existed?**
  The `Bookcase` domain model was anemic: it held only `bookcaseId`, `bookcaseLocation`, and `shelfCapacity`. Fields such as `bookcaseZone`, `bookcaseIndex`, and `bookCapacity` lived exclusively in `BookcaseEntity` (the JPA infrastructure type). This forced the application layer to depend on the infrastructure entity to access the full state of a bookcase.

- **The outbound port leaked infrastructure.** `BookcaseRepository` (in `core/ports/outbound`) declared methods that accepted and returned `BookcaseEntity`. The domain layer was therefore importing `com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity`, violating the dependency rule of hexagonal architecture (core must not depend on infrastructure).

- **The inbound port leaked DTOs.** `BookcaseFacade` (in `core/ports/inbound`) returned `Optional<BookcaseDTO>` and `List<BookcaseDTO>`. This coupled the domain application layer to the API DTO type and forced the use of `Optional` as a null-safety mechanism throughout all callers.

- **`BookcaseService` was a god class.** A single `@Service` handled creation (with shelf provisioning), deletion (with shelf teardown), and all query paths. This made the class hard to test in isolation and impossible to extend a single concern without touching unrelated logic.

- **Constraints that mattered:** The project follows hexagonal architecture (ports-and-adapters); core may not import `jakarta.persistence`, `BookcaseEntity`, or `BookcaseDTO`. Adapters (CLI, web controller) are the correct DTO-mapping boundary.

- **Evidence:**
  - Commits: `4065480` · Enrich Bookcase domain model and add BookcaseMapper
  - Commits: `3cd7d48` · Migrate BookcaseRepository port and impl to Bookcase domain model
  - Commits: `f662174` · Migrate BookcaseFacade to Bookcase type and extract use cases from BookcaseService
  - Commits: `8b6fcec` · Update CLI commands and web controller for Bookcase domain type
  - Commits: `a231962` · Update tests for Bookcase domain model refactor and add use case tests
  - Commits: `6f8646e` · Refactor Bookcase-related commands and services for improved readability
  - Key files: `Bookcase.java`, `BookcaseMapper.java`, `BookcaseRepository.java`, `BookcaseRepositoryImpl.java`, `BookcaseFacade.java`, `BookcaseService.java`, `CreateBookcaseUseCase.java`, `DeleteBookcaseUseCase.java`, `QueryBookcaseUseCase.java`

---

## Decision

- **I will promote `Bookcase` to a full domain model**, adding `bookcaseZone`, `bookcaseIndex`, and `bookCapacity` fields (with a 6-arg constructor), so the entire bookcase state is expressible in domain terms without touching `BookcaseEntity`.

- **I will purge `BookcaseEntity` from all domain ports.** `BookcaseRepository` (outbound port) now speaks only `Bookcase`; all `BookcaseEntity` ↔ `Bookcase` translation is confined to `BookcaseRepositoryImpl` via `BookcaseMapper`. The `save()` method returns `void` (side-effect contract); `findById()` returns `Bookcase` (nullable) instead of `Optional<BookcaseEntity>`.

- **I will decompose `BookcaseService` into three focused use cases** — `CreateBookcaseUseCase`, `DeleteBookcaseUseCase`, and `QueryBookcaseUseCase` — and reduce `BookcaseService` to a pure delegator. `BookcaseFacade` (inbound port) returns `Bookcase` domain objects; adapters (CLI, web controller) map to `BookcaseDTO` via `BookcaseMapper.toDTO()` at their own boundary.

**Boundary and ownership rules introduced:**

- Core (`core/domain`, `core/application`, `core/ports`) must not import `BookcaseEntity`, `BookcaseDTO`, or any `jakarta.persistence` type.
- `BookcaseMapper` lives in `core/domain` and is the sole translation point; it is allowed to import `BookcaseEntity` and `BookcaseDTO` because it sits at the anti-corruption boundary.
- Adapters (`infrastructure/adapter/outbound`, `cli/command`, `web/controllers`) own all DTO and entity mapping.
- Domain enforces invariants (e.g., `shelfCapacity` validation stays in `Bookcase.setShelfCapacity()`).

---

## Decision drivers

- **Infrastructure bleed into core**: `BookcaseRepository` imported `BookcaseEntity`; reverting this was the minimum required to uphold the hexagonal architecture contract already established by ADR-0001 and ADR-0002.
- **DTO bleed into core**: `BookcaseFacade` returning `List<BookcaseDTO>` forced core to know about API representations — a violation of the same rule.
- **Anemic domain object**: Three fields (`bookcaseZone`, `bookcaseIndex`, `bookCapacity`) were not representable in `Bookcase`, causing any domain logic that needed them to either reach for `BookcaseEntity` or re-derive them from the database.
- **God-class testability**: `BookcaseServiceTest` required mocking both `BookcaseRepository` and `ShelfAccessPort` for every test, including pure query tests that never touched shelves. Extracting use cases allows `QueryBookcaseUseCaseTest` to mock only `BookcaseRepository`.
- **Optional misuse as an error signal**: `findById()` returning `Optional<BookcaseEntity>` gave callers the impression of safe absence; in practice all callers called `.get()` anyway. An explicit null-return contract clarifies the current behavior (follow-up tracked below).
- **Consistency with ADR-0001 precedent**: The ShelfAccessPort decoupling (ADR-0001) established that bookcase core must not directly call shelf infrastructure. This refactor extends that precedent inward to the bookcase's own persistence port.

---

## Considered options

### Option A: Enrich the domain model and purge infrastructure types from ports (chosen)

- **Description:** Promote `Bookcase` to carry all fields, introduce `BookcaseMapper` in `core/domain`, update `BookcaseRepository` port to return `Bookcase`, and decompose `BookcaseService` into three use cases.
- **Pros:** Full hexagonal compliance; each use case is independently testable; `BookcaseService` becomes a thin facade; adapters own DTO concerns.
- **Cons:** All call sites must be updated at once; `save()` returning `void` loses the JPA-assigned ID immediately — callers must re-read or use `doAnswer` patterns in tests.
- **Risks:** `findById()` returns `null` instead of `Optional.empty()`; null-guards are not yet added at every call site (see follow-up work).
- **Where it shows up:** `Bookcase.java` (6-arg constructor), `BookcaseMapper.java` (new file), `BookcaseRepository.java` (all signatures changed), `BookcaseRepositoryImpl.java` (all methods use `BookcaseMapper`), `CreateBookcaseUseCase.java`, `DeleteBookcaseUseCase.java`, `QueryBookcaseUseCase.java` (all new), `BookcaseService.java` (pure delegator), `BookCaseController.java` + CLI commands (stream-map to DTO).

### Option B: Keep `BookcaseEntity` in the port, add a domain-facing wrapper

- **Description:** Leave `BookcaseRepository` returning `BookcaseEntity`, but wrap usage sites in an adapter class inside `core/application` that converts on the fly.
- **Pros:** Lower migration blast-radius; no constructor changes.
- **Cons:** The core/application package would still transitively import `BookcaseEntity`; the wrapper would be a second mapping layer, not a cleaner one. Infrastructure bleed is not resolved, only wrapped.
- **Risks:** Future developers would copy the pattern, increasing spread.
- **Where it shows up:** Would have added a `BookcaseEntityAdapter` in `core/application` — no such file exists.

### Option C: Keep `BookcaseService` monolithic; extract domain model only

- **Description:** Enrich `Bookcase` and update the port signatures, but keep all logic in `BookcaseService`.
- **Pros:** Smaller diff; no new files.
- **Cons:** The god-class problem persists; creation and query logic remain entangled; `BookcaseService` test class stays bloated (it was 413 lines before this refactor).
- **Risks:** Adding any new bookcase behavior (e.g., capacity enforcement) would require modifying a file with unrelated concerns.
- **Where it shows up:** `BookcaseService.java` before this commit — 134 lines of mixed creation, deletion, and query logic.

### Option D: Replace `BookcaseFacade` with separate inbound ports per use case

- **Description:** Instead of one `BookcaseFacade` interface, define `CreateBookcasePort`, `DeleteBookcasePort`, and `QueryBookcasePort` as inbound ports.
- **Pros:** Perfect interface-segregation alignment; each consumer depends on the minimal surface.
- **Cons:** More interfaces to maintain; existing consumers (`BookCaseController`, CLI commands) wire to one bean — splitting requires multiple injection points.
- **Risks:** Over-engineering for a module that currently has one web and one CLI consumer.
- **Where it shows up:** Not implemented; the existing `BookcaseFacade` contract was preserved and enriched.

---

## Consequences

### Positive

- **Hexagonal boundary is clean**: `core/application` and `core/domain` no longer import `BookcaseEntity` or `BookcaseDTO`.
- **Testability improved**: `CreateBookcaseUseCaseTest`, `DeleteBookcaseUseCaseTest`, and `QueryBookcaseUseCaseTest` each mock only what they need — one or two collaborators.
- **`BookcaseServiceTest` is right-sized**: Reduced from 413 lines to a thin delegator test; each use case carries its own test class.
- **Domain model is self-contained**: `Bookcase` now represents the full physical structure (zone, index, shelf capacity, book capacity) without consulting the entity.
- **DTO mapping is at the adapter boundary**: CLI commands and `BookCaseController` own the `BookcaseMapper.toDTO()` call — precisely where the ports-and-adapters pattern requires it.
- **Shelf provisioning logic is co-located with creation**: `CreateBookcaseUseCase` owns the loop that calls `ShelfAccessPort.createShelf()`, making the invariant "a bookcase always has exactly `shelfCapacity` shelves at creation" visible in one place.

### Negative and tradeoffs

- **Null return contract**: `findById()` returns `null` on miss instead of `Optional.empty()`. This is a regression in explicitness; callers that previously used `Optional.isPresent()` now perform implicit null checks or NPE.
- **`save()` is void**: JPA sets the generated ID on the entity, not on the `Bookcase` object passed to `save()`. The `Bookcase.bookcaseId` is `null` after `save()` returns, requiring `doAnswer` patterns in tests (visible in `CreateBookcaseUseCaseTest`).
- **`BookcaseMapper` imports infrastructure**: `BookcaseMapper` (in `core/domain`) imports `BookcaseEntity`. This is intentional as an anti-corruption layer, but its `toEntity()` method must not be called from `core/application` directly — only from `BookcaseRepositoryImpl`.
- **`BookcaseMapper.toEntity()` is incomplete**: The `toEntity()` in the initial commit did not set `bookcaseZone` or `bookcaseIndex`; the final `6f8646e` commit corrected this in `BookcaseRepositoryImpl`, but the static method remains partial (it maps only 4 of 6 fields).
- **No `Optional` at port boundary**: Replacing `Optional<BookcaseEntity>` with nullable `Bookcase` trades one form of implicit contract for another. Null is less composable than `Optional` and easier to miss in code review.

### Follow-up work

- **Fix `save()` to return the persisted `Bookcase` (or at least the generated ID).**
  - File: `BookcaseRepository.java` (signature) and `BookcaseRepositoryImpl.java` (implementation)
  - Done when: `save()` returns `Bookcase` with `bookcaseId` populated; `CreateBookcaseUseCase` no longer relies on `doAnswer` for ID retrieval in tests.

- **Add null-guards or restore `Optional` at `findById()` / `findBookCaseById()` call sites.**
  - Files: `BookCirculationCommands.java`, `BookSearchCommands.java`, `BookCaseController.java`
  - Done when: every caller of `findById()` handles the null case explicitly, and either a `NotFoundException` is thrown or an `Optional` wrapper is restored on the port.

- **Complete `BookcaseMapper.toEntity()` field mapping.**
  - File: `BookcaseMapper.java` (the `toEntity()` static method)
  - Done when: `toEntity()` maps all six fields (`bookcaseId`, `shelfCapacity`, `bookCapacity`, `bookcaseLocation`, `bookcaseZone`, `bookcaseIndex`) and a unit test in `BookcaseMapperTest` asserts round-trip fidelity.

- **Add a `BookcaseMapperTest` with round-trip assertions.**
  - File: new `BookcaseMapperTest.java` under `core/domain`
  - Done when: `toDomain(toEntity(bookcase))` produces an equal `Bookcase` for all fields.

- **Consider interface-segregation for `BookcaseFacade`.**
  - File: `BookcaseFacade.java`; new `QueryBookcaseFacade.java`, `CreateBookcaseFacade.java`
  - Done when: each consumer (`BookCaseController`, CLI commands) depends on the minimal port surface.

- **Validate `bookCapacity > 0` and `bookcaseZone` non-null in the `Bookcase` constructor.**
  - File: `Bookcase.java`
  - Done when: the constructor throws `IllegalArgumentException` on invalid input, and `BookcaseTest` covers the guard cases.

- **Remove the stale `BookcaseEntity` import from `BookcaseRepository.java`.**
  - File: `BookcaseRepository.java` — the old `import com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity` line was not cleaned up in all commits.
  - Done when: `BookcaseRepository.java` has zero imports from the `infrastructure` package.

---

## Compliance and guardrails

The following rules prevent regression of the hexagonal boundary:

- **Core must not import `BookcaseEntity`** (except `BookcaseMapper`, which is the designated anti-corruption layer).
- **Core must not import `BookcaseDTO`** (DTO mapping belongs at adapters).
- **No adapter calls `BookcaseService` directly**; all calls go through `BookcaseFacade`.
- **`BookcaseMapper.toEntity()` is called only from `BookcaseRepositoryImpl`** (infrastructure adapter). It must not be called from `core/application` or `core/domain`.
- **Each use case (`CreateBookcaseUseCase`, `DeleteBookcaseUseCase`, `QueryBookcaseUseCase`) has a dedicated test class**; `BookcaseServiceTest` may only test delegation behaviour, not business logic.

### Detection commands

```bash
# Verify no core/application or core/domain files import BookcaseEntity
rg -n "BookcaseEntity" \
  src/main/java/com/penrose/bibby/library/stacks/bookcase/core/

# Verify no core files import BookcaseDTO
rg -n "BookcaseDTO" \
  src/main/java/com/penrose/bibby/library/stacks/bookcase/core/

# Verify no core/application files import jakarta.persistence
rg -n "jakarta\.persistence|javax\.persistence" \
  src/main/java/com/penrose/bibby/library/stacks/bookcase/core/

# Run all tests
mvn test

# Confirm BookcaseMapper is the only class in core/domain that references BookcaseEntity
rg -n "BookcaseEntity" \
  src/main/java/com/penrose/bibby/library/stacks/bookcase/core/domain/
```

---

## Appendix

### Code pointers (diff anchors)

- `core/domain/model/Bookcase.java`: Replaced 2-arg constructor `(Long, int)` with 6-arg `(Long, int, int, String, String, String)`; added `bookcaseZone`, `bookcaseIndex`, `bookCapacity` fields with getters/setters. Matters because it is the foundation of the port purge — without a self-sufficient domain type, the ports cannot return it.

- `core/domain/BookcaseMapper.java` *(new file)*: Static utility with `toDomain(BookcaseEntity)`, `toEntity(Bookcase)`, and `toDTO(Bookcase)`. Lives in `core/domain` to be accessible from both `BookcaseRepositoryImpl` (entity mapping) and adapters (DTO mapping) without creating a circular dependency.

- `core/ports/outbound/BookcaseRepository.java`: All six methods now accept/return `Bookcase`. `save()` changed from `BookcaseEntity save(BookcaseEntity)` to `void save(Bookcase)`. `findById()` changed from `Optional<BookcaseEntity>` to nullable `Bookcase`. Name of `findBookcaseEntityByBookcaseLocation` corrected to `findBookcaseByBookcaseLocation`.

- `infrastructure/adapter/outbound/BookcaseRepositoryImpl.java`: All methods now call `BookcaseMapper.toDomain()` on exit and `BookcaseMapper.toEntity()` on entry. `save()` delegates to `bookcaseJpaRepository.save()` but discards the return value — this is the root cause of the ID-after-save problem.

- `core/application/usecases/CreateBookcaseUseCase.java` *(new)*: Owns duplicate-check, `Bookcase` construction, `bookcaseRepository.save()`, and the shelf-provisioning loop via `ShelfAccessPort`. Extracted from `BookcaseService.createNewBookCase()`.

- `core/application/usecases/DeleteBookcaseUseCase.java` *(new)*: Owns shelf cleanup before bookcase deletion; `DeleteBookcaseUseCaseTest` verifies shelf deletion precedes bookcase deletion (ordering invariant).

- `core/application/usecases/QueryBookcaseUseCase.java` *(new)*: All read paths delegated to `BookcaseRepository`; no business logic. Note: contains a stale import of `BookcaseEntity` and `Optional` from the initial draft (cleaned up in `6f8646e`).

- `core/application/BookcaseService.java`: Reduced from ~134 lines to a pure delegator — three fields (one per use case), three constructors, each method is a one-liner delegation.

- `core/ports/inbound/BookcaseFacade.java`: Return types changed from `Optional<BookcaseDTO>` / `List<BookcaseDTO>` to `Bookcase` / `List<Bookcase>`. The inbound port is now domain-typed end to end.

- `cli/command/book/BookCirculationCommands.java`, `BookSearchCommands.java`, `cli/prompt/domain/PromptOptions.java`, `web/controllers/stacks/bookcase/BookCaseController.java`: Each now calls `BookcaseMapper.toDTO()` (or `BookcaseMapper.toDTO()` in a stream) to convert `Bookcase` to `BookcaseDTO` at the adapter boundary.

---

### Candidate ADRs (optional)

- **ADR candidate: Restore `Optional` (or introduce `Result<T>`) at the `BookcaseRepository.findById()` port boundary** — the current null-return contract is error-prone and inconsistent with the rest of the codebase's use of `Optional`.

- **ADR candidate: Use-case interface segregation for `BookcaseFacade`** — splitting the single 8-method facade into `QueryBookcaseFacade` and `CreateBookcaseFacade` would align the inbound port with the use-case decomposition already done internally.

- **ADR candidate: Make `BookcaseRepository.save()` return the persisted `Bookcase`** — the void contract requires callers to perform a separate `findById()` call to obtain the generated ID, and it forces brittle `doAnswer` stubs in unit tests.
