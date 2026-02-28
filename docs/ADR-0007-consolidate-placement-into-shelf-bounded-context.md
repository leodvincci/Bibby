# ADR 0007: Consolidate Placement Into Shelf Bounded Context

**Date:** 2026-02-27
**Time:** 17:38 CST
**Status:** Accepted
**Deciders:** leodvincci
**Technical Story:** PR #332 (`feat/shelf-placement-and-book-mapper-relocation`)

## Context

A standalone `placement` module existed at `library/stacks/placement/` with its own domain model,
facade, ports, adapters, entity, repository, and controller. Its sole responsibility was placing a
book on a shelf — a single write operation that created a row in a `placements` table linking a
`bookId` to a `shelfId`.

This created several structural problems:

1. **No independent lifecycle.** A placement cannot exist without a shelf. It has no queries, no
   updates, no business rules beyond "the book must exist." It is not a bounded context — it is a
   single operation on the shelf aggregate.

2. **Cross-module wiring overhead.** The standalone module required its own `PlacementFacade`,
   `PlacementBookAccessPort`, `PlacementBookAccessPortAdapter`, `PlacementRepositoryPort`, and a
   dedicated controller at `web/controllers/stacks/placement/`. `BookController` depended on
   `PlacementFacade` to call placement after book creation.

3. **Duplicated book-access concern.** Both the shelf module (`BookAccessPort`) and the placement
   module (`PlacementBookAccessPort`) needed to reach into the cataloging context to verify book
   existence. Two separate adapters did essentially the same thing.

4. **Shelf already owned the relationship.** `BookAccessPort` in the shelf module already had
   `getBookIdsByShelfId` and `deleteBooksOnShelves` — methods that manage the book-shelf
   relationship. Placement was the write side of the same concern, split into a separate module.

Evidence:

- Commits: `6286566`, `6840b3a`, `a945752`, `df6da16`, `11484a3`, `4f4da43`, `4672154`
- Key files created:
  - `shelf/core/domain/model/Placement.java`
  - `shelf/core/application/usecases/PlaceBookOnShelfUseCase.java`
  - `shelf/core/ports/outbound/PlacementRepositoryPort.java`
  - `shelf/infrastructure/entity/PlacementEntity.java`
  - `shelf/infrastructure/repository/PlacementJpaRepository.java`
  - `shelf/infrastructure/adapter/outbound/PlacementRepositoryPortImpl.java`
  - `shelf/infrastructure/mapping/PlacementMapper.java`
- Key files modified:
  - `shelf/core/ports/inbound/ShelfFacade.java` — added `placeBookOnShelf`
  - `shelf/core/ports/outbound/BookAccessPort.java` — added `getBookById`
  - `shelf/core/application/ShelfService.java` — wired `PlaceBookOnShelfUseCase`
  - `shelf/infrastructure/adapter/outbound/BookAccessPortAdapter.java` — implemented `getBookById`, added `@Lazy`
  - `web/controllers/stacks/shelf/ShelfController.java` — added `POST /placements`
  - `web/controllers/cataloging/book/BookController.java` — replaced `PlacementFacade` with `ShelfFacade`
- Files deleted: entire `library/stacks/placement/` and `web/controllers/stacks/placement/` directories

## Decision

- I will consolidate the placement module into the shelf bounded context, making `Placement` a
  domain model under `shelf/core/domain/model/` and `PlaceBookOnShelfUseCase` a use case under
  `shelf/core/application/usecases/`.
- I will expose placement through the existing `ShelfFacade` inbound port (new method:
  `placeBookOnShelf(Long bookId, Long shelfId)`) rather than maintaining a separate `PlacementFacade`.
- I will validate book existence through the shelf module's existing `BookAccessPort` outbound port
  (new method: `getBookById(Long bookId)`) rather than a dedicated `PlacementBookAccessPort`.

Boundary and ownership rules introduced:

- **Shelf owns all book-shelf relationship operations**: queries (`getBookIdsByShelfId`), writes
  (`placeBookOnShelf`), and deletes (`deleteBooksOnShelves`) all route through `ShelfFacade` and
  `BookAccessPort`.
- **Cross-context book lookups go through `BookAccessPort`**: the shelf core never imports from
  `library.cataloging.book`. The adapter (`BookAccessPortAdapter`) translates `BookFacade` responses
  into shelf-local types.
- **Domain enforces invariants**: `Placement` validates null `bookId`/`shelfId` at construction and
  via setters. `PlaceBookOnShelfUseCase` validates book existence before creating a `Placement`.

## Decision drivers

- **Single-operation module overhead.** The placement module had 8+ files (facade, ports, adapter,
  domain, entity, repository, mapper, controller) for one write operation. Merging eliminates all of
  them and adds the behavior to existing shelf infrastructure.
- **Shelf already managed the read side.** `BookAccessPort.getBookIdsByShelfId` and
  `BookAccessPort.deleteBooksOnShelves` already treated book-shelf relationships as shelf behavior.
  Placement was the write side of the same concern, artificially separated.
- **Duplicated cross-context wiring.** Both modules needed an adapter to call `BookFacade`. Merging
  consolidates this into a single `BookAccessPortAdapter` with one `@Lazy` resolution point.
- **No independent aggregate identity.** A placement has no ID-based lifecycle (no "get placement by
  ID", no "update placement"). It is a side effect of a shelf operation, not a first-class entity
  requiring its own bounded context.
- **Controller simplification.** `BookController` no longer needs to depend on a separate
  `PlacementFacade`. It now calls `ShelfFacade.placeBookOnShelf` after book creation, keeping the
  dependency graph simpler.
- **Circular dependency surfaced by the merge.** Wiring `BookAccessPortAdapter → BookFacade →
  BookService → BookCommandUseCases → ShelfAccessPortAdapter → ShelfService` created a cycle. This
  was resolved with `@Lazy` on `BookAccessPortAdapter`'s constructor parameter — the same pattern
  already used by `BookService` for `ShelfAccessPort` (line 48, `BookService.java`). The cycle
  existed latently; the merge made it explicit and forced a documented resolution.

## Considered options

### Option A: Keep placement as a standalone module (status quo)

- **Description:** Maintain `library/stacks/placement/` with its own facade, ports, domain, and
  controller. `BookController` continues to depend on `PlacementFacade`.
- **Pros:**
  - No migration effort.
  - Module boundary is explicit — placement changes cannot accidentally break shelf code.
- **Cons:**
  - 8+ files for one operation with no independent lifecycle.
  - Duplicated `BookAccessPort` pattern across two modules.
  - `BookController` depends on two stacks facades (`ShelfFacade` + `PlacementFacade`).
  - Ongoing maintenance cost of keeping two modules in sync for the same aggregate relationship.
- **Risks:** As the shelf module grows (e.g., reorder books on a shelf, move a book between
  shelves), placement logic will need to coordinate with shelf state, creating tight coupling
  between two modules that should be one.
- **Where it shows up in code:** Deleted `library/stacks/placement/` directory,
  deleted `web/controllers/stacks/placement/` directory.

### Option B: Merge placement into the shelf module (chosen)

- **Description:** Move `Placement` domain model, use case, ports, entity, repository, mapper, and
  controller endpoint into the existing shelf module structure. Expose via `ShelfFacade`. Delete the
  standalone module.
- **Pros:**
  - Single module owns all book-shelf relationship behavior.
  - Eliminates duplicated `BookAccessPort` pattern.
  - `BookController` depends on one stacks facade (`ShelfFacade`).
  - Follows the principle that operations on an aggregate belong to that aggregate's module.
- **Cons:**
  - `ShelfFacade` grows (now 9 methods). Risk of becoming a "god facade" over time.
  - Introduced a circular dependency requiring `@Lazy` on `BookAccessPortAdapter`.
  - Shelf module now has two domain models (`Shelf`, `Placement`) and two persistence concerns.
- **Risks:** If placement later needs its own query surface (e.g., "find all placements for a book
  across shelves"), the shelf module may become overloaded.
- **Where it shows up in code:**
  - `shelf/core/domain/model/Placement.java` (new)
  - `shelf/core/application/usecases/PlaceBookOnShelfUseCase.java` (new)
  - `shelf/core/ports/outbound/PlacementRepositoryPort.java` (new)
  - `ShelfFacade.java:88-96` (added `placeBookOnShelf`)
  - `BookAccessPort.java:25-32` (added `getBookById`)
  - `ShelfController.java:39-46` (`POST /placements` endpoint)

### Option C: Merge placement into the book (cataloging) module

- **Description:** Treat placement as a book-side concern. `BookFacade` would expose a
  `placeBookOnShelf` method. The `placements` table would be managed by the cataloging module.
- **Pros:**
  - Book already knows its `shelfId` (via `BookEntity.shelfId`). Placement is conceptually
    "assigning a book to a location."
- **Cons:**
  - Violates the existing ownership model: the stacks layer (bookcase → shelf → books-on-shelf)
    manages physical organization. Cataloging manages bibliographic data.
  - `BookService` is already the largest service class in the codebase. Adding placement logic would
    increase its responsibilities further.
  - The shelf module would lose visibility into its own contents, needing to call back into
    cataloging to answer "what books are on this shelf?"
- **Risks:** Architectural erosion — the stacks/cataloging separation exists for a reason.
  Placement is a physical operation (putting a book on a shelf), not a bibliographic one.
- **Where it would show up:** `BookService.java`, `BookFacade.java` — already over 80 lines of
  interface methods.

## Consequences

### Positive

- **Single ownership of book-shelf relationship.** All CRUD operations for the book-shelf
  association now live in one module. No cross-module coordination needed for placement.
- **Reduced file count.** Eliminated ~10 files from the standalone placement module (facade, ports,
  adapter, domain, entity, repository, mapper, controller, and their test counterparts).
- **Simpler controller dependency graph.** `BookController` depends on `ShelfFacade` instead of
  `ShelfFacade` + `PlacementFacade`. One facade, one responsibility boundary.
- **Consolidated cross-context adapter.** `BookAccessPortAdapter` is the single point where the
  shelf module talks to the cataloging context. Book existence checks and book-list queries go
  through the same adapter.
- **Domain validation in the right place.** `Placement` domain model enforces null guards.
  `PlaceBookOnShelfUseCase` validates book existence before persisting. The use case throws
  `IllegalArgumentException`, caught by the new `GlobalExceptionHandler` and returned as 400.
- **Test coverage across all layers.** 16 new/updated tests: domain model (6), use case (3),
  repository adapter (2), mapper (2), service delegation (1), book-access adapter (2).

### Negative and tradeoffs

- **`ShelfFacade` is growing.** Now at 9 methods. If more operations are added (move book,
  reorder, swap shelves), this facade risks becoming a catch-all. Consider splitting into
  `ShelfQueryFacade` and `ShelfCommandFacade` if it reaches 12+ methods.
- **Circular dependency managed with `@Lazy`.** The chain `ShelfService → ... →
  BookAccessPortAdapter → BookFacade/BookService → ... → ShelfAccessPortAdapter → ShelfService`
  is broken by `@Lazy` on `BookAccessPortAdapter`'s constructor. This is a runtime proxy —
  errors in the `BookFacade` wiring won't surface until the first method call, not at startup.
- **`BookAccessPortAdapter` has stub methods.** `getBookIdsByShelfId` returns `List.of()` and
  `deleteBooksOnShelves` is a no-op. These were delegating to `BookFacade` before this branch
  but were stubbed out during the merge. This is tech debt that will cause bugs when those
  code paths are exercised.
- **Two domain models in one module.** `Shelf` and `Placement` are separate aggregates sharing a
  module. If `Placement` grows to need its own invariants (e.g., max books per shelf, duplicate
  detection), it may warrant re-extraction.
- **`PlaceBookOnShelfUseCase` uses `@Service`.** Use cases in this codebase are annotated with
  `@Service`, coupling the application layer to Spring. This is a pre-existing pattern, not
  introduced here, but worth noting.

### Follow-up work

- **Restore `BookAccessPortAdapter` delegation methods.**
  - File: `shelf/infrastructure/adapter/outbound/BookAccessPortAdapter.java`
  - `getBookIdsByShelfId` should delegate to `bookFacade.getBookIdsByShelfId()` instead of
    returning `List.of()`. `deleteBooksOnShelves` should delegate to
    `bookFacade.deleteByShelfId()` instead of being a no-op.
  - Done when: both methods delegate to `BookFacade` and have passing unit tests.

- **Add shelf-existence validation to `PlaceBookOnShelfUseCase`.**
  - File: `shelf/core/application/usecases/PlaceBookOnShelfUseCase.java`
  - Currently validates book existence but not shelf existence. A placement with a non-existent
    `shelfId` will fail at the database level with a foreign key violation (or silently succeed
    if there's no FK constraint).
  - Done when: use case throws `IllegalArgumentException` for invalid `shelfId` before persisting.

- **Add duplicate placement guard.**
  - File: `shelf/core/application/usecases/PlaceBookOnShelfUseCase.java`
  - Nothing prevents placing the same book on the same shelf twice, creating duplicate rows.
  - Done when: use case checks for existing placement and throws or returns early.

- **Add constructor validation to `Placement` domain model.**
  - File: `shelf/core/domain/model/Placement.java`
  - Constructor accepts null `bookId`/`shelfId` without throwing. Null guard only triggers on
    `getBookId()` (throws `IllegalStateException`) and `setBookId()` (throws
    `IllegalArgumentException`). Constructor should reject nulls eagerly.
  - Done when: `new Placement(null, 1L)` throws at construction time.

- **Evaluate `ShelfFacade` method count.**
  - File: `shelf/core/ports/inbound/ShelfFacade.java`
  - Currently at 9 methods. If it exceeds 12, consider splitting into `ShelfQueryFacade` and
    `ShelfCommandFacade`.
  - Done when: a decision is documented (either "split" or "keep as-is with rationale").

- **Remove latent `@Component` / `@Lazy` pattern debt.**
  - File: `BookAccessPortAdapter.java` (uses `@Lazy`), `BookService.java` line 48 (uses `@Lazy`
    on `ShelfAccessPort`)
  - Two `@Lazy` annotations mask a bidirectional module dependency between shelf and book. A
    cleaner fix is an event-driven or mediator pattern so neither module directly depends on the
    other's facade.
  - Done when: the circular dependency is broken structurally (e.g., domain events) rather than
    with `@Lazy` proxies.

- **Verify `placements` table schema exists.**
  - File: database migration scripts (Flyway/Liquibase if present, or manual DDL)
  - `PlacementEntity` maps to `@Table(name = "placements")` but no migration was included in
    this PR. If the table doesn't exist, the app will fail at runtime on first placement.
  - Done when: a migration script creates the `placements` table, or the table's existence is
    confirmed in the target database.

## Compliance and guardrails

### Boundary rules

- **Core must not import infrastructure.** `shelf/core/` must never import from
  `shelf/infrastructure/`, `jakarta.persistence`, or `org.springframework.*` (except `@Service`
  on use cases, which is a known pre-existing pattern).
- **No adapter calls another bounded context directly.** `BookAccessPortAdapter` is the only class
  in the shelf module that imports from `library.cataloging.book`. All cross-context access goes
  through `BookAccessPort` (outbound port) → `BookAccessPortAdapter` (adapter) → `BookFacade`
  (inbound port of cataloging).
- **DTO/entity mapping occurs only in infrastructure.** `PlacementMapper` lives in
  `shelf/infrastructure/mapping/`. The `PlacementRepositoryPort` accepts a `Placement` domain
  model, not a `PlacementEntity`.
- **Outbound port interfaces must not carry Spring annotations.** `PlacementRepositoryPort` and
  `BookAccessPort` are plain Java interfaces. `@Repository` and `@Component` belong on the
  adapter implementations only.

### Violation detection commands

```bash
# Core must not import Spring framework (except @Service on use cases)
rg -n "import org\.springframework" \
  src/main/java/com/penrose/bibby/library/stacks/shelf/core/ \
  --glob '!**/usecases/**'

# Core must not import JPA/persistence
rg -n "jakarta\.persistence|javax\.persistence" \
  src/main/java/com/penrose/bibby/library/stacks/shelf/core/

# Core must not import infrastructure
rg -n "import com\.penrose\.bibby\.library\.stacks\.shelf\.infrastructure" \
  src/main/java/com/penrose/bibby/library/stacks/shelf/core/

# Only BookAccessPortAdapter should import from cataloging.book
rg -n "import com\.penrose\.bibby\.library\.cataloging\.book" \
  src/main/java/com/penrose/bibby/library/stacks/shelf/ \
  --glob '!**/BookAccessPortAdapter.java'

# Outbound ports must not have Spring annotations
rg -n "@Component|@Service|@Repository" \
  src/main/java/com/penrose/bibby/library/stacks/shelf/core/ports/outbound/

# Run all unit tests
mvn test
```

## Appendix

### Code pointers (diff anchors)

- **`ShelfFacade.java:88-96`** — Added `placeBookOnShelf(Long bookId, Long shelfId)`. This is the
  inbound contract that external consumers (controllers, other modules) use. Replaces the deleted
  `PlacementFacade`.
- **`BookAccessPort.java:25-32`** — Added `Long getBookById(Long bookId)`. Returns the book ID if
  found, `null` otherwise. This is the contract the use case depends on for book-existence
  validation.
- **`PlaceBookOnShelfUseCase.java:20-26`** — Core business logic: validate book exists via port,
  create domain model, persist via port. No infrastructure imports.
- **`Placement.java:1-36`** — Domain model with defensive setters. Note: constructor does not
  validate nulls (see follow-up work).
- **`BookAccessPortAdapter.java:22-24`** — Constructor uses `@Lazy BookFacade` to break the
  circular dependency `ShelfService → ... → BookAccessPortAdapter → BookService → ... →
  ShelfService`.
- **`BookAccessPortAdapter.java:28-30`** — `getBookIdsByShelfId` stubbed to `List.of()`. Was
  previously delegating to `bookFacade.getBookIdsByShelfId()`. This is a regression (see
  follow-up work).
- **`BookController.java:118-120`** — After `createNewBook`, calls `shelfFacade.placeBookOnShelf`.
  Previously used `PlacementFacade` from the deleted placement module.
- **`ShelfController.java:39-46`** — New `POST /api/v1/shelves/placements` endpoint with inline
  `AddBookToShelfRequest` record. Provides a dedicated API for placement independent of book
  creation.
- **`PlacementRepositoryPortImpl.java:1-29`** — Adapter maps `Placement` → `PlacementEntity` via
  `PlacementMapper`, then saves via `PlacementJpaRepository`. Clean separation: port accepts
  domain, adapter handles translation.
- **`GlobalExceptionHandler.java:1-16`** — `@RestControllerAdvice` catching
  `IllegalArgumentException` → 400 with `{"error": "..."}`. Ensures domain validation errors
  from `PlaceBookOnShelfUseCase` reach the client as structured responses.

### Candidate ADRs

- **ADR candidate: Relocate book mappers to `infrastructure/adapter/mapping`** — Commit `6286566`
  moved `BookMapper` and `BookMapperTwo` from `infrastructure/mapping/` to
  `infrastructure/adapter/mapping/` to match the shelf module's convention. Establishes a
  codebase-wide rule that mappers live under `adapter/` when they translate between domain and
  infrastructure types.
- **ADR candidate: `@Lazy` as circular-dependency resolution pattern** — Commits `6840b3a` and
  `4672154` show `@Component` added then removed from `BookFacade`, with `@Lazy` on
  `BookAccessPortAdapter` as the final resolution. Documents the project's stance on
  bidirectional module dependencies and how to manage them short of event-driven decoupling.
- **ADR candidate: Global `IllegalArgumentException` → 400 handler** — Commit `df6da16` introduces
  a cross-cutting decision that all `IllegalArgumentException` instances thrown from any layer
  become 400 responses. This has broad implications: any misuse of `IllegalArgumentException` in
  non-validation contexts will now return 400 instead of 500.
