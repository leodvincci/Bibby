# ADR 0001: Introduce ShelfAccessPort to Decouple Bookcase Core from Shelf Module

Date: 2026-02-23

Status: Accepted

Deciders: leodvincci

Technical Story: Continuation of PR #301 (`300-bookcontroller-bypasses-inbound-port-depends-on-shelfservice-concrete-class`). Branch `refactor/bookcase-shelf-access-port`.

## Context

The bookcase module (`library.stacks.bookcase`) had two dependency violations that undermined hexagonal architecture boundaries:

1. **`BookcaseService` imported `ShelfFacade` directly.** The bookcase core (`core.application.BookcaseService`) held a compile-time dependency on `library.stacks.shelf.core.ports.inbound.ShelfFacade` — the shelf module's inbound port. This meant the bookcase core could not be compiled, tested, or reasoned about without the shelf module on the classpath. The dependency arrow pointed from one module's core directly into another module's core, violating the rule that cross-module communication should flow through owned ports and infrastructure adapters.

2. **`BookCaseController` orchestrated cross-module business logic.** The delete endpoint called `shelfFacade.deleteAllShelvesInBookcase()` followed by `bookcaseFacade.deleteBookcase()` — placing multi-step domain coordination in the web layer. The controller also depended on `BookcaseService` (the concrete class) in addition to `BookcaseFacade` (the inbound port), bypassing the port abstraction for several endpoints.

This pattern had already been identified and corrected in the book/cataloging module (see `devlog-2025-12-09-ShelfAccessPort-CrossModule-Decoupling.md`), where a `ShelfAccessPort` owned by the book module was introduced with the same rationale. The bookcase module had not yet been brought into alignment.

### Constraints

- The bookcase and shelf modules are siblings within the `library.stacks` bounded context, so the coupling was not cross-context — but the hexagonal architecture style adopted by this project requires each module's core to depend only on its own ports, regardless of bounded context proximity.
- `BookcaseService` needs two shelf operations: creating shelves during bookcase creation, and deleting all shelves during bookcase deletion.
- The delete operation must be atomic: shelves and bookcase must be removed together or not at all.

### Evidence

- Commits: `cb1f076` Decouple bookcase module from ShelfFacade via ShelfAccessPort, `3bc7829` Add logging to shelf creation and deletion operations
- Key files:
  - `bookcase/core/ports/outbound/ShelfAccessPort.java` (new)
  - `bookcase/infrastructure/adapter/outbound/ShelfAccessPortAdapter.java` (new)
  - `bookcase/core/application/BookcaseService.java` (modified)
  - `web/controllers/stacks/bookcase/BookCaseController.java` (modified)

## Decision

- **We will introduce a `ShelfAccessPort` interface in `bookcase.core.ports.outbound`**, owned by the bookcase module, defining only the shelf operations the bookcase module requires (`createShelf`, `deleteAllShelvesInBookcase`).
- **We will implement `ShelfAccessPortAdapter` in `bookcase.infrastructure.adapter.outbound`**, delegating to the shelf module's `ShelfFacade`. This adapter is the only class in the bookcase module that imports from the shelf module. It is registered with an explicit bean name (`@Component("bookcaseShelfAccessPortAdapter")`) to avoid a `ConflictingBeanDefinitionException` with the book module's identically-named `ShelfAccessPortAdapter`.
- **We will move cross-module orchestration out of `BookCaseController`** and into `BookcaseService`, making the controller depend solely on `BookcaseFacade` (the inbound port). The `deleteBookcase()` service method now handles shelf cleanup internally and is marked `@Transactional`.

### Boundary and ownership rules introduced

- **Core owns its ports and contracts.** `ShelfAccessPort` lives in `bookcase.core.ports.outbound`, not in the shelf module.
- **Adapters bridge modules.** `ShelfAccessPortAdapter` in `bookcase.infrastructure` is the single point of cross-module wiring.
- **Controllers depend only on inbound ports.** `BookCaseController` depends on `BookcaseFacade` and nothing else from the domain or application layer.

## Decision drivers

- **Consistency with established pattern.** The book/cataloging module already uses `ShelfAccessPort` (at `cataloging.book.core.port.outbound.ShelfAccessPort`) with a corresponding adapter. The bookcase module was the remaining outlier.
- **Testability of `BookcaseService`.** With a direct `ShelfFacade` dependency, tests for `BookcaseService` had to construct or mock a shelf module port. With `ShelfAccessPort`, tests mock a narrow interface owned by the bookcase module itself.
- **Controller had business logic.** The delete endpoint in `BookCaseController` orchestrated a two-step delete (shelves then bookcase). This logic belongs in the service layer where it can be transactional and tested independently of HTTP concerns.
- **Controller bypassed its own inbound port.** `BookCaseController` injected `BookcaseService` (concrete class) alongside `BookcaseFacade` (port), using the concrete class for some endpoints. This defeated the purpose of the facade abstraction.
- **Delete order was incorrect.** Prior to this change, `deleteBookcase()` called `bookcaseRepository.deleteById()` before shelf deletion, risking orphaned shelves or foreign key violations. Moving the logic to the service and correcting the order fixes this.
- **Compilation independence.** The bookcase core package can now be compiled without the shelf core package on the classpath (only the port interface is needed).

## Considered options

### Option A: Keep `ShelfFacade` as a direct dependency in `BookcaseService` (status quo)

- **Description:** `BookcaseService` continues to import and inject `ShelfFacade` from the shelf module. No new port or adapter.
- **Pros:** No new classes. Simpler wiring. Pragmatic given both modules share the `stacks` context.
- **Cons:** Bookcase core has a compile-time dependency on shelf core. Inconsistent with the pattern used in the book module. Tests must mock an interface they don't own.
- **Risks:** As the shelf module's `ShelfFacade` evolves (new methods, signature changes), the bookcase module is silently affected.
- **Where it shows up in code:** `BookcaseService.java` previously imported `com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade`.

### Option B: Introduce `ShelfAccessPort` in bookcase core with an infrastructure adapter (chosen)

- **Description:** The bookcase module defines its own outbound port (`ShelfAccessPort`) with only the operations it needs. An adapter in bookcase infrastructure bridges to `ShelfFacade`.
- **Pros:** Bookcase core is fully decoupled from shelf internals. Consistent with the book module's pattern. Narrow interface improves testability. Single point of cross-module contact in the adapter.
- **Cons:** Two new files (port + adapter) for what is currently a pass-through delegation. Slightly more wiring.
- **Risks:** If the shelf module changes `ShelfFacade` method signatures, the adapter must be updated — but the breakage is isolated to one file rather than scattered through the core.
- **Where it shows up in code:** `bookcase/core/ports/outbound/ShelfAccessPort.java`, `bookcase/infrastructure/adapter/outbound/ShelfAccessPortAdapter.java`.

### Option C: Use the book module's existing `ShelfAccessPort`

- **Description:** Have `BookcaseService` depend on `cataloging.book.core.port.outbound.ShelfAccessPort` instead of creating a new one.
- **Pros:** No new interface to maintain.
- **Cons:** Creates a dependency from the bookcase module to the book/cataloging module — architecturally worse than the original problem. The book module's port only exposes `findShelfById` and `isFull`, which don't match the bookcase module's needs (`createShelf`, `deleteAllShelvesInBookcase`).
- **Risks:** Violates module ownership boundaries. Would require expanding the book module's port to serve a different module's needs.
- **Where it shows up in code (would have):** `BookcaseService` would import `com.penrose.bibby.library.cataloging.book.core.port.outbound.ShelfAccessPort`.

## Consequences

### Positive

- `BookcaseService` no longer imports anything from `library.stacks.shelf`. The only cross-module reference is in the infrastructure adapter.
- `BookCaseController` has a single constructor dependency (`BookcaseFacade`), making it trivially testable and clearly scoped to HTTP concerns.
- The `deleteBookcase()` method is now `@Transactional`, ensuring shelf deletion and bookcase deletion are atomic.
- Delete order is corrected: shelves are deleted before the bookcase entity, preventing orphaned shelf records.
- Consistent hexagonal pattern across all three modules (book, bookcase, shelf) when accessing shelf operations.
- `ShelfAccessPort` is a narrow interface (2 methods), making it easy to mock in unit tests.

### Negative and tradeoffs

- Two new files (`ShelfAccessPort.java`, `ShelfAccessPortAdapter.java`) that are currently pure pass-throughs with no transformation logic. This is deliberate structural overhead for decoupling.
- If `ShelfFacade` method signatures change, the adapter must be updated manually. There is no compile-time link from the port to the facade (they are independent interfaces).
- The bookcase module now has two outbound ports (`BookcaseRepository`, `ShelfAccessPort`). As the module grows, the number of outbound ports may increase; this should be monitored for over-abstraction.
- `BookcaseServiceTest` must be updated to mock `ShelfAccessPort` instead of `ShelfFacade`, since the constructor signature changed.
- **Bean name collision risk.** Both the book module and bookcase module now have a class named `ShelfAccessPortAdapter`. The bookcase adapter uses `@Component("bookcaseShelfAccessPortAdapter")` to disambiguate. If additional modules introduce their own `ShelfAccessPort` adapters, they must also use explicit bean names. The book module's adapter (`cataloging.book.infrastructure.adapter.outbound.ShelfAccessPortAdapter`) currently uses the default name and should be considered for an explicit qualifier for consistency.

### Follow-up work

- **Update `BookcaseServiceTest` to use `ShelfAccessPort` mock.**
  - File: `src/test/java/com/penrose/bibby/library/stacks/bookcase/core/application/BookcaseServiceTest.java`
  - Done when: Test compiles and passes with `ShelfAccessPort` injected instead of `ShelfFacade`.

- **Add `@Transactional` to `createNewBookCase` in `BookcaseService`.**
  - File: `src/main/java/com/penrose/bibby/library/stacks/bookcase/core/application/BookcaseService.java`
  - Done when: The `createNewBookCase` method that calls `addShelf` in a loop is transactional, ensuring partial shelf creation doesn't leave an inconsistent bookcase.

- **Add integration test for bookcase deletion cascade.**
  - File: new test in `src/test/java/com/penrose/bibby/library/stacks/bookcase/`
  - Done when: A test creates a bookcase with shelves, deletes the bookcase, and verifies both the bookcase and all associated shelves are removed.

- **Consider ArchUnit rule for bookcase module boundary.**
  - File: existing or new ArchUnit test
  - Done when: A rule enforces that `bookcase.core` does not import from `shelf.core`.

- **Evaluate whether `BookcaseEntity` import in `BookcaseFacade` is a layer violation.**
  - File: `src/main/java/com/penrose/bibby/library/stacks/bookcase/core/ports/inbound/BookcaseFacade.java` (line: `Optional<BookcaseEntity> findById`)
  - Done when: `findById` returns a DTO or domain object instead of an infrastructure entity.

- **Add explicit bean name to book module's `ShelfAccessPortAdapter` for consistency.**
  - File: `src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/adapter/outbound/ShelfAccessPortAdapter.java`
  - Done when: `@Component` is changed to `@Component("bookShelfAccessPortAdapter")` to match the bookcase module's naming convention and prevent future collisions.

## Compliance and guardrails

### Rules

- **Bookcase core must not import shelf core.** No class in `bookcase.core` may import from `library.stacks.shelf.core`.
- **No adapter calls another bounded context directly.** Cross-module calls must flow through an owned outbound port.
- **Controllers depend only on inbound ports (facades).** No controller may inject a `*Service` concrete class or an outbound port.
- **Outbound port adapters with shared class names must use explicit bean names.** Any `ShelfAccessPortAdapter` (or similarly named adapter across modules) must declare `@Component("<module>ShelfAccessPortAdapter")` to prevent Spring bean name collisions.

### How to detect violations

```bash
# Bookcase core must not import shelf module
rg -n "import com\.penrose\.bibby\.library\.stacks\.shelf" \
  src/main/java/com/penrose/bibby/library/stacks/bookcase/core/

# Controllers must not import concrete service classes
rg -n "import.*\.core\.application\." \
  src/main/java/com/penrose/bibby/web/controllers/

# Detect duplicate default bean names for ShelfAccessPortAdapter
rg -n "class ShelfAccessPortAdapter" src/main/java/

# Compile and test
mvn -q test
```

## Appendix

### Code pointers (diff anchors)

- **`bookcase/core/ports/outbound/ShelfAccessPort.java`** (new): Defines the contract the bookcase core uses to interact with shelves. Two methods: `deleteAllShelvesInBookcase(Long)` and `createShelf(Long, int, String, int)`.
- **`bookcase/infrastructure/adapter/outbound/ShelfAccessPortAdapter.java`** (new): `@Component("bookcaseShelfAccessPortAdapter")` adapter that delegates to `ShelfFacade`. Only class in the bookcase module that imports from the shelf module. Explicit bean name avoids collision with `cataloging.book.infrastructure.adapter.outbound.ShelfAccessPortAdapter`, which Spring would otherwise register under the same default name.
- **`bookcase/core/application/BookcaseService.java`** (modified): Constructor changed from `(BookcaseRepository, ShelfFacade)` to `(BookcaseRepository, ShelfAccessPort)`. `deleteBookcase()` now calls `shelfAccessPort.deleteAllShelvesInBookcase()` before `bookcaseRepository.deleteById()`, wrapped in `@Transactional`. `addShelf()` delegates to `shelfAccessPort.createShelf()`.
- **`web/controllers/stacks/bookcase/BookCaseController.java`** (modified): Removed `ShelfFacade` and `BookcaseService` dependencies. Constructor takes only `BookcaseFacade`. All endpoints delegate through `bookcaseFacade`. Delete endpoint no longer orchestrates shelf cleanup.
- **`shelf/core/application/ShelfService.java`** (modified): Added `logger.info` after `deleteAllShelvesInBookcase` execution.
- **`shelf/core/application/usecases/CreateShelfUseCase.java`** (modified): Added `logger.info` after shelf persistence with shelf ID and bookcase ID.

### Candidate ADRs

- **ADR candidate: Enforce `@Transactional` policy for multi-entity lifecycle operations** — `createNewBookCase` creates a bookcase then loops to create shelves, but is not transactional. A consistent policy for when `@Transactional` is required would prevent partial-write bugs.
- **ADR candidate: Remove `BookcaseEntity` from `BookcaseFacade` return types** — The inbound port `BookcaseFacade.findById` returns `Optional<BookcaseEntity>`, leaking an infrastructure entity through the port contract. This violates the hexagonal boundary between core and infrastructure.
