# Devlog: Extract Shelf Application Use Cases and Thin Service/Controller Wiring
Date: 2026-02-20 (America/Chicago)  
Branch: `refactor/shelf-usecase-layer-extraction`  
Range: `origin/main..HEAD`  
Commits:  
- `4cd5c98` Add shelf application use case classes  
- `802b6b2` Refactor ShelfService to delegate to use cases  
- `e5a7bdc` Wire ShelfController directly to query use case  

## Context
- **Problem being solved:** `ShelfService` was carrying mixed responsibilities (validation, querying, deletion orchestration, mapping to summaries), which made it a broad application service and caused test bulk/duplication.
- **Before state:** shelf business flows were centralized in `ShelfService` and `ShelfController` depended on `ShelfService` even for read-only endpoints.
- **Important note about range:** `origin/main..HEAD` is currently empty (branch head appears already contained in `origin/main`). For this devlog, I analyzed the **most recent pushed commit batch** on this branch (`4cd5c98..e5a7bdc`), which matches your latest push.

## High-level Summary (The 60-second version)
- **Product/User impact:** no intentional endpoint behavior changes; shelf options/listing APIs should behave the same.
- **Architecture impact:** extracted explicit application use cases (`CreateShelfUseCase`, `DeleteShelvesUseCase`, `QueryShelfUseCase`) and turned `ShelfService` into a thinner facade.
- **Implementation impact:** moved create/delete/query logic out of `ShelfService`; updated tests to target use cases directly and simplified `ShelfServiceTest` to delegation checks.
- **Dependency change:** `ShelfController` now depends on `QueryShelfUseCase` directly (instead of `ShelfService`) for read endpoints.
- **Risk profile:** low-to-moderate refactor risk; behavior-preserving intent, but wiring and transaction-boundary shifts need regression coverage.

## Commit-by-commit Breakdown

### Add shelf application use case classes (`4cd5c98`)
- **Intent:** introduce explicit command/query use case classes to isolate business actions and make shelf flows easier to reason about/test.
- **Files touched (with quick reason per file):**
  - `src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases/CreateShelfUseCase.java`  
    Added creation validation + persistence delegation (`execute`).
  - `src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases/DeleteShelvesUseCase.java`  
    Added delete orchestration: collect shelf IDs, delete books first, then shelves.
  - `src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases/QueryShelfUseCase.java`  
    Added read methods and summary projection logic.
  - `src/test/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases/CreateShelfUseCaseTest.java`  
    Added extensive validation and happy-path tests.
  - `src/test/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases/DeleteShelvesUseCaseTest.java`  
    Added ordering and aggregation tests.
  - `src/test/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases/QueryShelfUseCaseTest.java`  
    Added read and summary mapping tests.
- **Key code changes (behavior):**
  - `CreateShelfUseCase.execute(...)` enforces input constraints and delegates to `ShelfDomainRepository.save(...)`.
  - `DeleteShelvesUseCase.execute(...)` maintains critical ordering (book delete before shelf delete).
  - `QueryShelfUseCase.getShelfSummariesForBookcase(...)` maps `Shelf` domain objects to `ShelfSummary`.
- **Architecture notes (ports/adapters/DDD boundaries):**
  - Positive: use cases depend on core outbound ports (`ShelfDomainRepository`, `BookAccessPort`) — good dependency direction.
  - Positive: command/query intent is explicit at application layer.
- **Risk / edge cases:**
  - Validation logic moved location (from service to use case) but not semantics.
  - Logging moved into `DeleteShelvesUseCase`.
- **Verification (tests run or recommended commands):**
  - Ran: `mvn -q -Dtest=CreateShelfUseCaseTest,DeleteShelvesUseCaseTest,QueryShelfUseCaseTest test`

### Refactor ShelfService to delegate to use cases (`802b6b2`)
- **Intent:** reduce `ShelfService` to an inbound orchestration facade and remove embedded business logic.
- **Files touched (with quick reason per file):**
  - `src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/ShelfService.java`  
    Replaced direct logic with delegation to injected use cases.
  - `src/test/java/com/penrose/bibby/library/stacks/shelf/core/application/ShelfServiceTest.java`  
    Reworked from business-rule tests to delegation/interaction tests.
- **Key code changes (behavior):**
  - `findAllShelves`, `findShelfById`, `getShelfSummariesForBookcase`, `createShelf`, `deleteAllShelvesInBookcase` now route to use cases.
  - Removed direct dependencies from service to `ShelfDomainRepository` / `BookAccessPort`.
- **Architecture notes:**
  - Positive: `ShelfService` is now a clearer boundary surface (`ShelfFacade` implementation).
  - Tradeoff: one extra indirection layer for simple paths.
- **Risk / edge cases:**
  - Existing service tests that asserted validation internals were removed; those checks now rely on dedicated use case tests.
- **Verification:**
  - Ran: `mvn -q -Dtest=ShelfServiceTest test`

### Wire ShelfController directly to query use case (`e5a7bdc`)
- **Intent:** align web adapter with query-specific application entrypoint.
- **Files touched (with quick reason per file):**
  - `src/main/java/com/penrose/bibby/web/controllers/stacks/shelf/ShelfController.java`  
    Inject `QueryShelfUseCase` and use it for `/options` endpoints.
- **Key code changes (behavior):**
  - `getShelfOptions()` now calls `queryShelfUseCase.findAll()`.
  - `getShelfOptionsByBookcase(...)` now calls `queryShelfUseCase.findAllShelves(bookcaseId)`.
- **Architecture notes:**
  - Mixed: this is CQRS-friendly (read adapter calling query use case directly).
  - Tradeoff: controller now depends on a concrete use case class rather than facade/port abstraction.
- **Risk / edge cases:**
  - Endpoint output should remain stable; risk mainly in DI wiring and regression coverage gaps.
- **Verification:**
  - Ran compile check: `mvn -q -DskipTests compile`

## Deep Dive: The Main Refactor / Feature
Critical flow now has clearer command/query separation.

### Read flow (`GET /options/{bookcaseId}`)
`HTTP request -> ShelfController -> QueryShelfUseCase -> ShelfDomainRepository -> mapper -> ShelfOptionResponse`

Small excerpt:
```java
@GetMapping("/options/{bookcaseId}")
public List<ShelfOptionResponse> getShelfOptionsByBookcase(@PathVariable Long bookcaseId) {
  return queryShelfUseCase.findAllShelves(bookcaseId).stream()
      .map(shelfResponseMapper::toShelfOption)
      .toList();
}
```

### Command flow (delete shelves in a bookcase)
`inbound facade (ShelfService) -> DeleteShelvesUseCase -> BookAccessPort + ShelfDomainRepository`

Small excerpt:
```java
public void execute(Long bookcaseId) {
  List<Shelf> shelves = shelfDomainRepository.findByBookcaseId(bookcaseId);
  List<Long> shelfIds = shelves.stream().map(shelf -> shelf.getShelfId().shelfId()).toList();
  bookAccessPort.deleteBooksOnShelves(shelfIds);
  shelfDomainRepository.deleteByBookcaseId(bookcaseId);
}
```

### Why this design is better than the old one
- `ShelfService` no longer accumulates unrelated concerns.
- Business rules are tested at the class where they live.
- Clear command/query entrypoints improve maintainability and future extension (e.g., authorization/events per use case).

## Dependency & Boundary Audit
- **Dependencies removed/added:**
  - Removed in `ShelfService`: direct `ShelfDomainRepository`, `BookAccessPort`, local validation logic.
  - Added in `ShelfService`: `QueryShelfUseCase`, `CreateShelfUseCase`, `DeleteShelvesUseCase`.
  - Changed in `ShelfController`: `ShelfService` -> `QueryShelfUseCase`.
- **Layering checks:**
  - Good: use cases depend on ports, not infrastructure classes.
  - Watchpoint: controller imports concrete `QueryShelfUseCase` (`core/application/usecases`) directly. This is acceptable in many clean architectures, but if you prefer strict inbound-port usage, consider a query facade interface.
- **Suggested micro-fixes:**
  - Introduce a read-side interface (e.g., `ShelfQueryFacade`) to decouple controller from concrete class.
  - Keep package naming consistent (`usecases` vs any existing `usecase` variants).

## Testing & Verification
- **Tests added/updated:**
  - Added:
    - `CreateShelfUseCaseTest`
    - `DeleteShelvesUseCaseTest`
    - `QueryShelfUseCaseTest`
  - Updated:
    - `ShelfServiceTest` (delegation-focused)
- **Commands already run:**
  - `mvn -q -Dtest=CreateShelfUseCaseTest,DeleteShelvesUseCaseTest,QueryShelfUseCaseTest test`
  - `mvn -q -Dtest=ShelfServiceTest test`
  - `mvn -q -DskipTests compile`
- **Recommended additional commands:**
  - `mvn -q test`
  - `mvn -q -Dtest=ShelfControllerTest test` (if controller tests exist)
- **Targeted tests to add (if missing):**
  1. Controller test for `/options` and `/options/{bookcaseId}` response mapping integrity.
  2. Integration test that verifies delete ordering still holds through full Spring wiring.
  3. Contract test that `ShelfSummary` fields remain stable after query use case mapping.

## What I Learned
- Extracting use cases dramatically simplifies service classes and clarifies responsibilities.
- Moving tests alongside extracted behavior keeps confidence high during refactors.
- Delegation-focused service tests are leaner and less brittle than large mixed-behavior tests.
- Read-side controller wiring can be cleaner with query use cases, but it should be balanced against abstraction policy.
- Command sequencing (delete books before shelves) is a critical invariant worth explicit unit tests.

## Next Steps
- **Immediate follow-ups (today):**
  - Add/confirm controller-level tests for shelf option endpoints.
  - Run full test suite to catch wiring regressions.
- **Short-term hardening (this week):**
  - Add a read facade interface to reduce controller coupling to concrete use case.
  - Add a transactional integration test for `DeleteShelvesUseCase` path.
- **Strategic refactors (later):**
  - Standardize application-layer naming conventions (`usecases` package and command/query class names).
  - Consider explicit command/query DTOs if use case signatures start to grow.
  - Evaluate applying same extraction pattern to adjacent modules (Book/Bookcase) for consistency.
