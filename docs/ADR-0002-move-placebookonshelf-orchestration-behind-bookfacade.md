# ADR 0001: Move placeBookOnShelf Orchestration Behind BookFacade and Enforce Port-Only Cross-Module Communication

**Date:** 2026-02-23 (America/Chicago)

**Status:** Accepted

**Deciders:** Unknown

**Technical Story:** Closes [#300](https://github.com/leodvincci/Bibby/issues/300) — "BookController bypasses inbound port, depends on ShelfService concrete class"

---

## Context

`BookController.placeBookOnShelf` orchestrated across three bounded contexts in ~50 lines of controller code:

1. Called `bookService.assignBookToShelf(bookId, shelfId)` (Book core)
2. Called `shelfService.findShelfById(shelfId)` (Shelf concrete service — **not** through a port)
3. Called `bookcaseService.findBookCaseById(shelf.bookcaseId())` (Bookcase concrete service — **not** through a port)
4. Manually mapped `Shelf` domain → `ShelfDTO`, assembled a `BookPlacementResponse`

This meant:

- **`BookController` imported concrete Stacks classes** (`ShelfService`, `BookcaseService`, `ShelfDTO`, `BookcaseDTO`) — a compile-time dependency from the Cataloging web adapter into Stacks internals.
- **`BookDomainRepositoryImpl` held a `ShelfFacade` dependency** and performed a business-logic check (`shelfFacade.findShelfById(shelfId).get().isFull()`) inside a persistence adapter — business logic in infrastructure.
- **`BookDomainRepositoryImpl` also injected `AuthorService`** (unused), adding a gratuitous coupling.
- The hexagonal boundary was violated in two directions: the web adapter reached into another module's core, and the persistence adapter performed cross-module validation.

**Constraints that mattered:**

- **Modularity:** Bibby uses a ports-and-adapters / hexagonal architecture per module. Each module (Book, Shelf, Bookcase) exposes an inbound port (facade interface) and defines outbound ports for cross-module queries.
- **Testability:** Controller logic that orchestrates across three services is hard to unit-test without wiring the full Spring context.
- **Coupling:** Any change to `ShelfService` or `BookcaseService` signatures would break `BookController` — a web adapter in a different module.

**Evidence:**

- Commits: `2c3b1f0`, `9da6619`, `ed999a4`, `6c61a49`, `2064cbf`, `67dbef7`, `4e86637`, `ab831a2`
- Key files:
  - `src/main/java/com/penrose/bibby/web/controllers/cataloging/book/BookController.java`
  - `src/main/java/com/penrose/bibby/library/cataloging/book/core/application/usecases/bookCommandUseCases.java` (new)
  - `src/main/java/com/penrose/bibby/library/cataloging/book/core/port/inbound/BookFacade.java`
  - `src/main/java/com/penrose/bibby/library/cataloging/book/core/port/outbound/ShelfAccessPort.java`
  - `src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/adapter/outbound/ShelfAccessPortAdapter.java`
  - `src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/repository/BookDomainRepositoryImpl.java`
  - `src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases/QueryShelfUseCase.java`
  - `src/main/java/com/penrose/bibby/library/stacks/shelf/core/ports/inbound/ShelfFacade.java`

---

## Decision

- **We will route all `placeBookOnShelf` orchestration through `BookFacade` (the Book module's inbound port), delegating to a new `bookCommandUseCases` use case class that validates shelf state exclusively through `ShelfAccessPort` (the Book module's outbound port).**
- **We will strip `BookDomainRepositoryImpl` to pure persistence** — no cross-module calls, no business validation. Shelf existence and capacity checks belong in the application layer.
- **We will remove all Stacks-module imports (`ShelfService`, `BookcaseService`, `ShelfDTO`, `BookcaseDTO`) from `BookController`**, making the controller a thin HTTP-to-facade adapter.

**Boundary and ownership rules introduced:**

- The Book module's **core** defines `ShelfAccessPort` — the contract for how it queries shelf state.
- The Book module's **infrastructure** implements `ShelfAccessPortAdapter`, which delegates to `ShelfFacade` (Shelf module's inbound port).
- Business validation (shelf exists? shelf full?) lives in `bookCommandUseCases` (application layer), not in controllers or repositories.
- `BookDomainRepositoryImpl` is a persistence adapter only — it writes to the database, nothing more.

---

## Decision drivers

1. **BookController was a cross-boundary orchestrator.** It imported 4 classes from `library.stacks.*`, making the Cataloging web layer a compile-time dependent on Stacks internals. Any Stacks refactor could break the Book controller. (Evidence: removed imports of `ShelfService`, `BookcaseService`, `ShelfDTO`, `BookcaseDTO` in commit `6c61a49`)

2. **Business logic leaked into the persistence layer.** `BookDomainRepositoryImpl.placeBookOnShelf` called `shelfFacade.findShelfById(shelfId).get().isFull()` — a business-rule check inside an infrastructure class. This made the repository hard to test and blurred the adapter's responsibility. (Evidence: `isFull()` check removed from `BookDomainRepositoryImpl` in commit `ed999a4`)

3. **Unused dependencies accumulated.** `BookDomainRepositoryImpl` injected `AuthorService` (never used) and required `@Lazy ShelfFacade` (only used for the capacity check). `BookController` injected `IsbnEnrichmentService` (never used). (Evidence: removed in commits `2064cbf`, `67dbef7`, `6c61a49`)

4. **The Book module already had the port pattern for cross-module shelf access.** `ShelfAccessPort` existed with `findShelfById(Long)`. Extending it with `isFull(Long)` was a natural, low-risk addition. (Evidence: commit `9da6619` added `isFull` to `ShelfAccessPort`)

5. **Shelf capacity is a Shelf domain concern.** The `Shelf` aggregate already has an `isFull()` method. Exposing it through `QueryShelfUseCase` → `ShelfFacade` → `ShelfAccessPort` keeps the rule in the Shelf domain while making it queryable from Book. (Evidence: commit `2c3b1f0` added `QueryShelfUseCase.isFull`)

6. **Controller testability.** A 50-line controller method with 3 service dependencies and inline DTO mapping is effectively untestable without integration wiring. A one-line delegation to `bookFacade.placeBookOnShelf(...)` can be tested with a single mock. (Evidence: commit `6c61a49` reduced `placeBookOnShelf` to 3 lines)

---

## Considered options

### Option A: Move orchestration to BookFacade + dedicated use case class (chosen)

- **Description:** Add `placeBookOnShelf` to `BookFacade`. Create `bookCommandUseCases` in the Book application layer. It validates via `ShelfAccessPort` (outbound port), then delegates persistence to `BookDomainRepository`. Controller becomes a thin adapter.
- **Pros:**
  - BookController has zero Stacks imports
  - Validation logic is testable with mocked ports
  - Repository stays pure persistence
  - Cross-module communication flows through defined ports only
- **Cons:**
  - Adds a new class (`bookCommandUseCases`) and a new method on `BookFacade`
  - `BookService` now delegates to `bookCommandUseCases`, adding an extra hop
  - Two separate port calls (`findShelfById` + `isFull`) to check shelf state — the shelf is loaded twice
- **Risks:**
  - The new `bookCommandUseCases` class name violates Java naming conventions (lowercase first letter)
  - `IllegalStateException` thrown for all validation failures — controller layer needs exception-to-HTTP-status mapping
- **Where it shows up in code:**
  - `bookCommandUseCases.java` (new): `com.penrose.bibby.library.cataloging.book.core.application.usecases`
  - `BookFacade.java`: added `placeBookOnShelf(Long, BookShelfAssignmentRequest)`
  - `BookService.java`: implements facade method, delegates to use case
  - `BookController.java`: reduced to `bookFacade.placeBookOnShelf(bookId, request)`

### Option B: Move orchestration to BookService directly (no separate use case class)

- **Description:** Put the validation logic directly in `BookService.placeBookOnShelf` instead of creating a new `bookCommandUseCases` class.
- **Pros:**
  - One fewer class to maintain
  - `BookService` already implements `BookFacade`, so no extra delegation hop
- **Cons:**
  - `BookService` is already large (270+ lines). Adding orchestration logic makes it larger.
  - Mixes command orchestration with existing query/CRUD methods — lower cohesion
  - Harder to extract later if more commands are added
- **Risks:**
  - `BookService` becomes a god class over time
- **Where it would show up:** All logic in `BookService.placeBookOnShelf` instead of delegating to `bookCommandUseCases`

### Option C: Keep orchestration in BookController but use ports instead of concrete classes

- **Description:** Keep the controller orchestrating the flow, but inject `BookFacade` and `ShelfAccessPort` instead of `ShelfService` / `BookcaseService`.
- **Pros:**
  - No new application-layer classes
  - Controller still controls the HTTP response shape directly
- **Cons:**
  - Controller remains a business-logic orchestrator — wrong layer
  - Controller tests still need to mock multiple ports
  - The "place book on shelf" use case is scattered across the controller (validation, persistence, response assembly) instead of encapsulated
  - A port defined in Book's core (`ShelfAccessPort`) would be injected into a web adapter — unusual and arguably a layering smell
- **Risks:**
  - Other controllers may copy the pattern, spreading business logic across the web layer
- **Where it would show up:** `BookController` with different dependencies but same ~50-line method

### Option D: Event-driven decoupling (domain event + listener)

- **Description:** `BookService` publishes a `BookShelfAssignmentRequested` event. A listener in the Shelf module validates capacity and either confirms or rejects.
- **Pros:**
  - True bounded-context decoupling — Book module doesn't need `ShelfAccessPort` at all
  - Extensible: other modules could react to the event
- **Cons:**
  - Adds significant infrastructure complexity (event bus, eventual consistency, compensating actions)
  - Overkill for a synchronous request-response flow
  - Harder to reason about failures (what if the event is lost? what if validation fails asynchronously?)
  - No existing event infrastructure in the codebase
- **Risks:**
  - Introducing eventual consistency for a simple validation check is over-engineering
- **Where it would show up:** New event classes, event publisher in Book, event listener in Shelf

---

## Consequences

### Positive

1. **BookController has zero imports from `library.stacks.*`.** The Cataloging web adapter is fully decoupled from Stacks internals at compile time.

2. **BookDomainRepositoryImpl is a pure persistence adapter.** Constructor reduced from 4 dependencies (`BookMapper`, `BookJpaRepository`, `AuthorService`, `@Lazy ShelfFacade`) to 2 (`BookMapper`, `BookJpaRepository`). No cross-module calls.

3. **Shelf capacity validation is testable in isolation.** `bookCommandUseCases` depends on two interfaces (`ShelfAccessPort`, `BookDomainRepository`). Unit tests can mock both without Spring context.

4. **Cross-module communication is explicit and traceable.** Book → `ShelfAccessPort` → `ShelfAccessPortAdapter` → `ShelfFacade` → `ShelfService`. Every hop is visible in the dependency graph.

5. **Shelf module gained reusable capacity queries.** `ShelfFacade.isFull(Long)` and `ShelfFacade.isEmpty(Long)` are now first-class API operations available to any module, not just inline checks on domain objects.

6. **Unused dependencies removed.** `AuthorService` from `BookDomainRepositoryImpl`, `IsbnEnrichmentService` from `BookController` — reducing the dependency graph and constructor complexity.

### Negative and tradeoffs

1. **Breaking API change.** The `POST /{bookId}/shelf` response changed from `BookPlacementResponse` (structured JSON with book title, shelf label, bookcase location) to `HttpStatus.OK` (the string `"OK"`). Any client consuming the response body will break.

2. **`ResponseEntity.ok(HttpStatus.OK)` is semantically wrong.** This serializes the `HttpStatus` enum as the response body rather than returning an empty 200. Should be `ResponseEntity.ok().build()` or `ResponseEntity.noContent().build()`.

3. **Double shelf lookup.** `bookCommandUseCases` calls `shelfAccessPort.findShelfById(shelfId)` and then `shelfAccessPort.isFull(shelfId)`. The `isFull` implementation in `QueryShelfUseCase` loads the shelf again via `shelfDomainRepositoryPort.getById()`. That's 2-3 database hits for one operation.

4. **No exception-to-HTTP-status mapping.** `bookCommandUseCases` throws `IllegalStateException` for all validation failures (null input, shelf not found, shelf full). Without an `@ExceptionHandler` or `@ControllerAdvice`, Spring returns 500 for all of these instead of 400/404/409.

5. **Class naming convention violation.** `bookCommandUseCases` starts with a lowercase letter, violating Java conventions and likely triggering IDE/linter warnings.

6. **No tests shipped.** `BookControllerTest` was added in commit `6c61a49` and deleted in commits `4e86637`/`ab831a2`. The refactored flow has zero test coverage.

7. **`BookController` still depends on `BookService` (concrete class)** in addition to `BookFacade` (interface). The controller has not fully migrated to port-only dependencies.

### Follow-up work

| # | Action | File / Area | Done when |
|---|--------|-------------|-----------|
| 1 | Rename `bookCommandUseCases` → `BookCommandUseCases` | `book/core/application/usecases/bookCommandUseCases.java` | Class name starts with uppercase; all references updated; compiles clean |
| 2 | Fix response: `ResponseEntity.ok().build()` or `noContent().build()` | `BookController.placeBookOnShelf` | Endpoint returns empty body with 200 or 204 status |
| 3 | Rename parameter `aLong` → `shelfId` | `ShelfAccessPort.isFull(Long aLong)` | Parameter name is descriptive in interface and adapter |
| 4 | Add unit tests for `bookCommandUseCases` | New test class `bookCommandUseCasesTest` | 4 cases covered: happy path, shelf not found, shelf full, null input |
| 5 | Add `@WebMvcTest` for `BookController.placeBookOnShelf` | New test class `BookControllerTest` | POST `/{bookId}/shelf` returns correct status; `bookFacade.placeBookOnShelf` is called |
| 6 | Add `@ControllerAdvice` or `@ExceptionHandler` for `IllegalStateException` | `BookController` or shared advice class | Null input → 400, shelf not found → 404, shelf full → 409 |
| 7 | Remove `BookService` concrete dependency from `BookController` | `BookController` constructor and field declarations | Controller depends only on `BookFacade` (interface) |
| 8 | Add `Shelf.isEmpty()` domain method | `Shelf` domain model | `QueryShelfUseCase.isEmpty` delegates to `shelf.isEmpty()` instead of `shelf.getBookCount() == 0` |
| 9 | Consolidate shelf lookup: single `validateShelfAcceptsBook(Long)` | `ShelfAccessPort` + adapter | One port call replaces `findShelfById` + `isFull`; shelf loaded once |
| 10 | Run ArchUnit to detect remaining cross-module boundary violations | ArchUnit test suite | `mvn test -Dtest=ArchitectureTest` passes with no new violations |

---

## Compliance and guardrails

**Rules to prevent regression:**

1. **No web adapter may import a concrete service from another bounded context.** Controllers in `web.controllers.cataloging.*` must not import classes from `library.stacks.*` core or application packages. Use the module's inbound port (facade interface) only.

2. **No infrastructure adapter calls another bounded context directly.** `BookDomainRepositoryImpl` must not import `ShelfFacade`, `ShelfService`, or any Stacks class. Cross-module queries go through outbound ports defined in the Book module's core.

3. **Business validation belongs in the application layer (use cases or services), not in repositories or controllers.**

4. **Outbound port parameters should use descriptive names**, not IDE-generated placeholders like `aLong`.

**How to detect violations:**

```bash
# BookController must not import Stacks classes
rg -n "import com\.penrose\.bibby\.library\.stacks" \
  src/main/java/com/penrose/bibby/web/controllers/cataloging/book/

# BookDomainRepositoryImpl must not import ShelfFacade or ShelfService
rg -n "import com\.penrose\.bibby\.library\.stacks" \
  src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/

# Book core must not import Spring framework classes (except annotations)
rg -n "import org\.springframework\.(beans|context|web)" \
  src/main/java/com/penrose/bibby/library/cataloging/book/core/

# Run full test suite
mvn test

# Run ArchUnit architecture tests specifically
mvn test -Dtest=ArchitectureTest
```

---

## Appendix

### Code pointers (diff anchors)

- **`BookController.java`**: `placeBookOnShelf` reduced from ~50 lines of cross-context orchestration to 3 lines delegating to `bookFacade.placeBookOnShelf(bookId, request)`. Removed constructor dependencies on `ShelfService`, `BookcaseService`, `IsbnEnrichmentService`. (Commit `6c61a49`)

- **`bookCommandUseCases.java` (new)**: Encapsulates the place-book-on-shelf workflow: validate input → check shelf exists via `ShelfAccessPort.findShelfById` → check shelf capacity via `ShelfAccessPort.isFull` → persist via `BookDomainRepository.placeBookOnShelf`. (Commit `ed999a4`)

- **`BookFacade.java`**: Added `void placeBookOnShelf(Long bookId, BookShelfAssignmentRequest shelfAssignmentRequest)` to the inbound port contract. (Commit `ed999a4`)

- **`BookService.java`**: Implements the new facade method by delegating to `bookCommandUseCases.placeBookOnShelf`. Added `bookCommandUseCases` as a constructor dependency. (Commit `ed999a4`)

- **`ShelfAccessPort.java`**: Added `boolean isFull(Long aLong)` to the Book module's outbound port for cross-module shelf capacity queries. (Commit `9da6619`)

- **`ShelfAccessPortAdapter.java`**: Implements `isFull` by delegating to `shelfFacade.isFull(aLong)`. (Commit `9da6619`)

- **`BookDomainRepositoryImpl.java`**: Stripped to pure persistence. Removed `ShelfFacade` and `AuthorService` dependencies. Constructor reduced from 4 params to 2. `placeBookOnShelf` no longer checks shelf capacity — just loads book, sets shelf ID, saves. (Commits `ed999a4`, `2064cbf`, `67dbef7`)

- **`QueryShelfUseCase.java`**: Added `isFull(Long)` and `isEmpty(Long)`. Both load the shelf via `shelfDomainRepositoryPort.getById`, throw `IllegalStateException` if not found, then delegate to the domain model. (Commit `2c3b1f0`)

- **`ShelfFacade.java`**: Added `boolean isFull(Long shelfId)` and `boolean isEmpty(Long shelfId)` to the Shelf module's inbound port. (Commit `2c3b1f0`)

- **`ShelfService.java`**: Implements both new facade methods, delegating to `QueryShelfUseCase`. (Commit `2c3b1f0`)

- **`BookMapper.java`**: Moved from `web.controllers.cataloging.book` to `web.controllers.cataloging.book.mappers` subpackage. (Commit `6c61a49`)

### Candidate ADRs

- **ADR candidate: Standardize exception-to-HTTP-status mapping for domain validation errors** — `bookCommandUseCases` throws `IllegalStateException` for three distinct failure modes (bad input, not found, conflict). Without structured exception handling, all return 500. A `@ControllerAdvice` or typed exception hierarchy is needed.

- **ADR candidate: Define a policy for cross-module query consolidation** — The current design makes two port calls (`findShelfById` + `isFull`) that each independently load the shelf. A policy on whether outbound ports should offer composite queries (e.g., `validateShelfAcceptsBook`) vs. atomic ones would reduce redundant database hits and clarify port granularity.

- **ADR candidate: Eliminate dual BookService/BookFacade dependency in BookController** — `BookController` injects both `BookService` (concrete) and `BookFacade` (interface), which are the same object. This indicates an incomplete migration to port-only controller dependencies.
