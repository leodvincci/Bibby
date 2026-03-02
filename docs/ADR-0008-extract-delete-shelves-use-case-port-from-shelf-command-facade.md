# ADR 0008: Extract DeleteShelvesUseCasePort from ShelfCommandFacade for Cross-Module Deletion

Date: 2026-03-01

Time: 18:57 CST

Status: Accepted

Deciders: leodvincci

Technical Story: PR #337 (`feat/shelf-placement-and-book-mapper-relocation`), commits `a9303da`, `0ec22ba`, `1af23cb`

## Context

- The bookcase module's `ShelfAccessPortAdapter` delegated shelf deletion through `ShelfCommandFacade.deleteAllShelvesInBookcaseByBookcaseId()`. This worked, but `ShelfCommandFacade` is a broad inbound port bundling three unrelated operations: shelf creation, shelf deletion, and book placement. The adapter only needed deletion, yet it depended on the full facade interface — violating the Interface Segregation Principle (ISP).
- ADR-0001 (bookcase-shelf-access-port) established that cross-module shelf operations flow through the bookcase-owned `ShelfAccessPort` with a corresponding infrastructure adapter. That adapter delegated both `deleteAllShelvesInBookcase` and `createShelf` through `ShelfCommandFacade`. This meant two conceptually distinct use cases (delete vs. create) were funneled through the same facade dependency, coupling the adapter to the facade's full surface area.
- The `DeleteShelvesUseCase` already existed as a dedicated `@Service` class encapsulating the deletion workflow (find shelves → delete associated books → delete shelves), but it had no inbound port interface. It was invoked indirectly through `ShelfService`, which delegated to it from the `ShelfCommandFacade` implementation.

### Constraints

- The shelf module follows hexagonal architecture: core owns ports, infrastructure implements adapters.
- Cross-module calls must flow through ports, not concrete classes (established in ADR-0001).
- The deletion workflow involves a multi-step cascade (books then shelves), so the consumer should not need to know the orchestration details — it should invoke a single port method.

### Evidence

- Commits:
  - `a9303da` — Add deleteShelvesUseCasePort and implement in DeleteShelvesUseCase
  - `0ec22ba` — Wire ShelfAccessPortAdapter to deleteShelvesUseCasePort
  - `1af23cb` — Refactor deleteShelvesUseCasePort naming (lowercase → PascalCase fix)
- Key files:
  - `shelf/core/ports/inbound/DeleteShelvesUseCasePort.java` (new)
  - `shelf/core/application/usecases/DeleteShelvesUseCase.java` (modified — now implements `DeleteShelvesUseCasePort`)
  - `bookcase/infrastructure/adapter/outbound/ShelfAccessPortAdapter.java` (modified — now injects `DeleteShelvesUseCasePort`)
  - `bookcase/infrastructure/adapter/outbound/ShelfAccessPortAdapterTest.java` (new)

## Decision

- I will extract a `DeleteShelvesUseCasePort` interface in `shelf.core.ports.inbound` with a single method `execute(Long bookcaseId)`, and have `DeleteShelvesUseCase` implement it directly.
- I will rewire the bookcase module's `ShelfAccessPortAdapter` to inject `DeleteShelvesUseCasePort` for the deletion path, while keeping `ShelfCommandFacade` for shelf creation only. This splits the adapter's two responsibilities across two narrower ports.

### Boundary and ownership rules introduced

- **One use case, one inbound port.** Each use case class that serves cross-module consumers should expose its own dedicated inbound port interface rather than being accessed through an aggregate facade.
- **Adapters pick the narrowest port.** When a cross-module adapter needs a single operation, it should depend on the use-case-level port, not the facade that bundles unrelated operations.

## Decision drivers

- **Interface Segregation Principle.** `ShelfCommandFacade` exposes `deleteAllShelvesInBookcaseByBookcaseId`, `createShelfInBookcaseByBookcaseId`, and `placeBookOnShelf`. The bookcase adapter's `deleteAllShelvesInBookcase` method only needs deletion. Depending on the full facade forces the adapter to "see" methods it never calls.
- **Reduced coupling surface.** If `ShelfCommandFacade` gains new methods or changes signatures for creation or placement, the deletion path in the adapter is unaffected because it depends on `DeleteShelvesUseCasePort` — a single-method interface.
- **The use case already existed.** `DeleteShelvesUseCase` was already a standalone `@Service` with its own orchestration logic (find shelves, delete books via `BookAccessPort`, delete shelves). It lacked only a port interface to be directly injectable by external modules.
- **Consistency with hexagonal port granularity.** The project already has fine-grained inbound ports (`ShelfQueryFacade`, `ShelfCommandFacade`, `BookAccessPort`). Adding `DeleteShelvesUseCasePort` continues the trend toward narrow, purpose-specific contracts.
- **Testability improvement.** The new `ShelfAccessPortAdapterTest` verifies that `deleteAllShelvesInBookcase` delegates to `DeleteShelvesUseCasePort` and `createShelf` delegates to `ShelfCommandFacade`, with `verifyNoInteractions` confirming each path is isolated.

## Considered options

### Option A: Keep delegating deletion through ShelfCommandFacade (status quo)

- **Description:** `ShelfAccessPortAdapter` continues to call `shelfCommandFacade.deleteAllShelvesInBookcaseByBookcaseId()` for deletion.
- **Pros:** No new interfaces. Fewer files to maintain.
- **Cons:** Adapter depends on a 3-method facade when it only needs 1 method for deletion. Any signature change to unrelated facade methods still triggers recompilation of the adapter's dependency.
- **Risks:** As `ShelfCommandFacade` grows (more placement or creation variants), the adapter silently acquires broader coupling.
- **Where it shows up in code:** The pre-change `ShelfAccessPortAdapter` injected only `ShelfCommandFacade shelfFacade` and called `shelfFacade.deleteAllShelvesInBookcaseByBookcaseId(bookcaseId)`.

### Option B: Extract DeleteShelvesUseCasePort and wire adapter to it (chosen)

- **Description:** A new `DeleteShelvesUseCasePort` interface with `execute(Long bookcaseId)` is added in `shelf.core.ports.inbound`. `DeleteShelvesUseCase` implements it. The bookcase adapter injects this port for deletion and keeps `ShelfCommandFacade` for creation.
- **Pros:** Narrowest possible contract for deletion. Adapter's two responsibilities are each backed by the most specific port. Use case is directly reachable without routing through `ShelfService`.
- **Cons:** One more interface file. The adapter now has two constructor dependencies instead of one.
- **Risks:** If more use cases get their own ports, the adapter's constructor may accumulate dependencies — but each is narrow and purposeful.
- **Where it shows up in code:** `DeleteShelvesUseCasePort.java`, `DeleteShelvesUseCase implements DeleteShelvesUseCasePort`, `ShelfAccessPortAdapter(ShelfCommandFacade, DeleteShelvesUseCasePort)`.

### Option C: Split ShelfCommandFacade into per-operation facades

- **Description:** Replace `ShelfCommandFacade` entirely with `CreateShelfFacade`, `DeleteShelvesFacade`, and `PlaceBookOnShelfFacade`.
- **Pros:** Maximum ISP compliance. Every consumer depends on exactly the contract it needs.
- **Cons:** Three new interfaces replacing one. Existing consumers of `ShelfCommandFacade` (e.g., `ShelfController`, CLI commands) would need to inject multiple facades, increasing wiring complexity.
- **Risks:** Over-fragmentation for consumers that legitimately need multiple operations (e.g., `ShelfController` uses all three). Premature for the current number of consumers.
- **Where it shows up in code (would have):** Three new files in `shelf.core.ports.inbound`, modifications to `ShelfService`, `ShelfController`, and every CLI command that uses `ShelfCommandFacade`.

## Consequences

### Positive

- `ShelfAccessPortAdapter.deleteAllShelvesInBookcase` depends only on `DeleteShelvesUseCasePort`, a single-method interface — the narrowest possible contract.
- `DeleteShelvesUseCase` is now directly addressable as a Spring bean through its port interface, not only reachable indirectly through `ShelfService`.
- The adapter test (`ShelfAccessPortAdapterTest`) verifies that each adapter method delegates to the correct port and does not interact with the other, providing regression safety.
- The change is fully backwards-compatible: `ShelfCommandFacade` still exists and still exposes `deleteAllShelvesInBookcaseByBookcaseId` for consumers that use the full facade (e.g., `ShelfController`).
- Constructor dependency intent is clearer: `ShelfAccessPortAdapter(ShelfCommandFacade, DeleteShelvesUseCasePort)` explicitly communicates that the adapter bridges two distinct shelf capabilities.

### Negative and tradeoffs

- One additional interface (`DeleteShelvesUseCasePort`) to maintain. If the deletion signature changes, both the port and the use case must be updated.
- `ShelfAccessPortAdapter` constructor grew from 1 to 2 dependencies. This is minor but trends toward more wiring if future use cases also get their own ports.
- `ShelfCommandFacade` still exposes `deleteAllShelvesInBookcaseByBookcaseId`, creating a dual path for deletion: consumers can reach it through the facade (via `ShelfService`) or directly through `DeleteShelvesUseCasePort` (via `DeleteShelvesUseCase`). This is intentional — the facade remains the entry point for internal consumers (controllers, CLI), while the use case port is for cross-module adapters.
- The naming convention diverges slightly: `DeleteShelvesUseCasePort.execute(Long)` vs `ShelfCommandFacade.deleteAllShelvesInBookcaseByBookcaseId(Long)`. The port uses a generic `execute` method, which is concise but less self-documenting than the facade method name.

### Follow-up work

- **Extract `CreateShelfUseCasePort` for the creation path in `ShelfAccessPortAdapter`.**
  - File: `shelf/core/ports/inbound/CreateShelfUseCasePort.java` (new), `ShelfAccessPortAdapter.java` (modified)
  - Done when: The adapter injects `CreateShelfUseCasePort` instead of `ShelfCommandFacade`, completing the ISP split for both adapter methods.

- **Evaluate whether `ShelfCommandFacade.deleteAllShelvesInBookcaseByBookcaseId` should delegate to `DeleteShelvesUseCasePort` internally.**
  - File: `shelf/core/application/ShelfService.java`
  - Done when: `ShelfService.deleteAllShelvesInBookcaseByBookcaseId()` delegates to `DeleteShelvesUseCase` (via the port), or a decision is recorded to keep the current direct delegation.

- **Consider renaming `DeleteShelvesUseCasePort.execute` to a more descriptive method name.**
  - File: `shelf/core/ports/inbound/DeleteShelvesUseCasePort.java`
  - Done when: Method is renamed (e.g., `deleteAllShelvesInBookcase(Long bookcaseId)`) or a decision is recorded that `execute` is the preferred convention for single-method use case ports.

- **Add explicit bean name to `DeleteShelvesUseCase` for consistency with `ShelfAccessPortAdapter` naming convention.**
  - File: `shelf/core/application/usecases/DeleteShelvesUseCase.java`
  - Done when: `@Service` is changed to `@Service("deleteShelvesUseCase")` or a decision is recorded that default naming is acceptable for use cases.

- **Extract use case ports for `PlaceBookOnShelfUseCase` following the same pattern.**
  - File: `shelf/core/ports/inbound/PlaceBookOnShelfUseCasePort.java` (new)
  - Done when: `PlaceBookOnShelfUseCase` implements its own port interface, consistent with `DeleteShelvesUseCasePort`.

- **Add ArchUnit rule enforcing that cross-module adapters do not depend on aggregate facades when a use-case-level port exists.**
  - File: new ArchUnit test
  - Done when: A test fails if a `*AccessPortAdapter` imports a `*Facade` interface for an operation that has a dedicated `*UseCasePort`.

## Compliance and guardrails

### Rules

- **One use case, one inbound port (for cross-module consumers).** If a use case is invoked by an adapter in another module, it must expose its own port interface in `core.ports.inbound`.
- **Adapters depend on the narrowest available port.** A cross-module adapter must not inject a facade when a dedicated use case port covers the needed operation.
- **Core must not import `org.springframework.*`.** Port interfaces remain framework-free.
- **No adapter calls another bounded context directly. Use a port.**

### How to detect violations

```bash
# Verify DeleteShelvesUseCasePort has no Spring imports
rg -n "import org\.springframework" \
  src/main/java/com/penrose/bibby/library/stacks/shelf/core/ports/inbound/DeleteShelvesUseCasePort.java

# Verify shelf core ports have no framework dependencies
rg -n "import org\.springframework|import jakarta\.|import javax\." \
  src/main/java/com/penrose/bibby/library/stacks/shelf/core/ports/

# Verify bookcase core does not import shelf core
rg -n "import com\.penrose\.bibby\.library\.stacks\.shelf" \
  src/main/java/com/penrose/bibby/library/stacks/bookcase/core/

# Compile and run tests
mvn -q test
```

## Appendix

### Code pointers (diff anchors)

- **`shelf/core/ports/inbound/DeleteShelvesUseCasePort.java`** (new): Single-method interface `execute(Long bookcaseId)`. Lives alongside `ShelfCommandFacade` and `ShelfQueryFacade` in the inbound ports package, establishing the convention that use cases can have their own ports parallel to facades.
- **`shelf/core/application/usecases/DeleteShelvesUseCase.java`** (modified): Added `implements DeleteShelvesUseCasePort`. No behavioral change — the `execute(Long bookcaseId)` method already existed with the correct signature. The use case is now directly injectable via its port interface.
- **`bookcase/infrastructure/adapter/outbound/ShelfAccessPortAdapter.java`** (modified): Constructor changed from `(ShelfCommandFacade)` to `(ShelfCommandFacade, DeleteShelvesUseCasePort)`. `deleteAllShelvesInBookcase` now delegates to `deleteShelvesUseCasePort.execute()` instead of `shelfCommandFacade.deleteAllShelvesInBookcaseByBookcaseId()`. Field renamed from `shelfFacade` to `shelfCommandFacade` for clarity.
- **`bookcase/infrastructure/adapter/outbound/ShelfAccessPortAdapterTest.java`** (new): Two tests verifying delegation isolation — deletion delegates to `DeleteShelvesUseCasePort` with `verifyNoInteractions(shelfCommandFacade)`, creation delegates to `ShelfCommandFacade` with `verifyNoInteractions(deleteShelvesUseCasePort)`.

### Candidate ADRs

- **ADR candidate: Standardize `execute` vs descriptive method names on use case ports** — The project now has both styles (`DeleteShelvesUseCasePort.execute()` vs `ShelfCommandFacade.deleteAllShelvesInBookcaseByBookcaseId()`). A convention decision would prevent inconsistency as more use case ports are extracted.
- **ADR candidate: Extract CreateShelfUseCasePort to complete ISP split in ShelfAccessPortAdapter** — The adapter still depends on `ShelfCommandFacade` for shelf creation. Extracting a `CreateShelfUseCasePort` would make both adapter methods depend on single-method ports, fully realizing the ISP pattern started in this ADR.
