# ADR 0005: Return Port Model from `ShelfFacade.findAll`

Date: 2026-02-25 CST (Central Time)

Status: Accepted

Deciders: Unknown

Technical Story: N/A

## Context

- `origin/main` already exposed `List<ShelfResponse>` for `ShelfFacade.findShelvesByBookcaseId(...)`, but `ShelfFacade.findAll()` still returned `List<Shelf>`, creating a mixed inbound contract in the same port.
- In `origin/main`, `QueryShelfUseCase.findAll()` returned domain objects directly (`return shelfDomainRepositoryPort.findAll();`), so domain `Shelf` still crossed the inbound boundary on this path.
- The pushed range (`origin/main..HEAD`) contains one production contract change commit, one test-hardening commit, and one documentation commit.
- Constraints that mattered: keep the migration incremental, keep API boundary explicit, and keep regression risk low with focused tests.
- Evidence:
- Commits: `5c6de4c Return ShelfResponse from shelf findAll boundary`; `54a86ac Strengthen shelf findAll tests for response mapping`; `49e81d5 Document shelf inbound port model decision`.
- Key files: `src/main/java/com/penrose/bibby/library/stacks/shelf/core/ports/inbound/ShelfFacade.java`, `src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases/QueryShelfUseCase.java`, `src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/ShelfService.java`, `src/test/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases/QueryShelfUseCaseTest.java`, `src/test/java/com/penrose/bibby/library/stacks/shelf/core/application/ShelfServiceTest.java`, `docs/ADR-0004-inbound-port-model-for-shelf-query-boundary.md`.

## Decision

- I will change `ShelfFacade.findAll()` to return `List<ShelfResponse>` instead of `List<Shelf>`.
- I will perform `Shelf -> ShelfResponse` translation in `QueryShelfUseCase.findAll()` using `ShelfPortModelMapper::toShelfResponse`.
- I will keep `ShelfService.findAll()` as a delegating boundary method that exposes only `List<ShelfResponse>`.

Boundary and ownership rules introduced:

- Core inbound port owns query return contracts (`ShelfResponse`) for shelf list queries.
- Application use cases own domain-to-port-model mapping.
- Domain objects can be used inside core logic, but must not be returned by this inbound query boundary.

## Decision drivers

- The same port interface had inconsistent query contracts (`findShelvesByBookcaseId` returned `ShelfResponse`, `findAll` returned `Shelf`), which increased boundary ambiguity.
- `origin/main` clearly showed domain leakage in `QueryShelfUseCase.findAll()` with direct `List<Shelf>` passthrough.
- Return-type change at the port (`ShelfFacade`) gives compile-time enforcement for contract alignment.
- `QueryShelfUseCaseTest` previously validated passthrough identity semantics for `findAll`; the new assertions validate mapped `ShelfResponse` content.
- `ShelfServiceTest` now asserts delegation with `ShelfResponse`, tightening service-level contract expectations.
- **Inference:** commit message in `5c6de4c` states this is a continuation of the inbound-port-model migration strategy, so the decision favors incremental migration over broad interface redesign.

## Considered options

### Option A: Migrate `findAll` to `List<ShelfResponse>` in the inbound port (chosen)

- Description: Update `ShelfFacade`, `ShelfService`, and `QueryShelfUseCase` so `findAll()` returns a port model.
- Pros: Consistent inbound contract; explicit boundary ownership; compile-time safety on callers.
- Cons: Adds mapping step and response-model dependency in the query path.
- Risks: Remaining methods (`findShelfById`) still return domain type, so migration is partial.
- Where it shows up in code: `src/main/java/com/penrose/bibby/library/stacks/shelf/core/ports/inbound/ShelfFacade.java`, `src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases/QueryShelfUseCase.java`, `src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/ShelfService.java`.

### Option B: Keep `findAll()` as `List<Shelf>` (status quo in `origin/main`)

- Description: Leave `findAll()` unchanged and rely on consumers to handle domain objects.
- Pros: No refactor work now; no extra mapping operation.
- Cons: Domain leakage remains on inbound boundary; inconsistent with existing `findShelvesByBookcaseId` response model.
- Risks: Continued coupling to domain structure and behavior in outer layers.
- Where it shows up in code (before this push): `origin/main` version of `ShelfFacade.findAll()` and `QueryShelfUseCase.findAll()`.

### Option C: Keep port as `List<Shelf>` and map only in adapters

- Description: Perform conversion in CLI/web adapters rather than in the application use case.
- Pros: Leaves core API unchanged.
- Cons: Boundary still leaks domain type; mapping logic can duplicate across adapters.
- Risks: Divergent adapter projections and accidental domain-behavior use outside core.
- Where it would have shown up: adapter mapping layers instead of `QueryShelfUseCase.findAll()`.

## Consequences

### Positive

- `ShelfFacade.findAll()` now matches the response-model direction already used by `findShelvesByBookcaseId(...)`.
- `QueryShelfUseCase.findAll()` now enforces boundary translation through a single mapper call.
- `ShelfService.findAll()` remains simple delegation while exposing the corrected port contract.
- `QueryShelfUseCaseTest` now checks mapped IDs (`result.get(0).id()`, `result.get(1).id()`), improving regression detection for mapping behavior.
- `ShelfServiceTest` now verifies `findAll()` delegation on `List<ShelfResponse>`, reinforcing contract-level behavior.

### Negative and tradeoffs

- Mapping introduces additional object creation for each `findAll()` result element.
- Migration is still incomplete because `findShelfById` returns `Optional<Shelf>`.
- `ShelfPortModelMapper` currently passes `shelf.getBookIds()` directly; defensive copy is not enforced in this change.
- Documentation footprint increased (`ADR` and `devlog`), which adds maintenance overhead if future changes diverge.

### Follow-up work

- Specific next action: Migrate `findShelfById` to `Optional<ShelfResponse>`.
- Exact file or area: `src/main/java/com/penrose/bibby/library/stacks/shelf/core/ports/inbound/ShelfFacade.java`, `src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases/QueryShelfUseCase.java`.
- Done when criteria: No inbound query method returns domain `Shelf`.
- Specific next action: Add unit tests for mapper behavior and list immutability expectations.
- Exact file or area: `src/test/java/.../ports/inbound/mapper/ShelfPortModelMapperTest.java`.
- Done when criteria: All mapped fields and collection behavior are asserted.
- Specific next action: Decide and implement defensive copy policy for `bookIds`.
- Exact file or area: `src/main/java/com/penrose/bibby/library/stacks/shelf/core/ports/inbound/mapper/ShelfPortModelMapper.java`.
- Done when criteria: Policy is documented and enforced in code.
- Specific next action: Keep ADR history aligned with current branch/base assumptions.
- Exact file or area: `docs/ADR-0004-inbound-port-model-for-shelf-query-boundary.md`, `docs/the-devlogs/devlog-2026-02-24-Shelf-Response-Port-Model.md`.
- Done when criteria: Document examples reflect current method signatures and commit range.

## Compliance and guardrails

- `ShelfFacade` query methods must not return domain `Shelf` unless explicitly documented by a new ADR.
- Mapping from domain `Shelf` to response model must happen in application use cases, not in adapters.
- `ShelfService` must remain orchestration/delegation and not bypass use-case mapping.
- Tests for query boundaries must assert response-model fields, not raw domain mock identity.

Also include:

- How to detect violations (commands)
- `mvn test`
- `rg -n "import org\\.springframework" src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain`
- `rg -n "jakarta\\.persistence|javax\\.persistence" src/main/java/com/penrose/bibby/library/stacks/shelf/core`

```bash
mvn test -Dtest=ShelfServiceTest,QueryShelfUseCaseTest -DfailIfNoTests=false
rg -n "List<Shelf> findAll\\(" src/main/java/com/penrose/bibby/library/stacks/shelf/core
rg -n "return shelfDomainRepositoryPort\\.findAll\\(\\);" src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases
rg -n "import org\\.springframework" src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain
rg -n "jakarta\\.persistence|javax\\.persistence" src/main/java/com/penrose/bibby/library/stacks/shelf/core
```

## Appendix

### Code pointers (diff anchors)

- `src/main/java/com/penrose/bibby/library/stacks/shelf/core/ports/inbound/ShelfFacade.java`: `findAll()` contract changed to `List<ShelfResponse>`.
- `src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases/QueryShelfUseCase.java`: `findAll()` now maps repository results through `ShelfPortModelMapper`.
- `src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/ShelfService.java`: service boundary now returns `List<ShelfResponse>`.
- `src/test/java/com/penrose/bibby/library/stacks/shelf/core/application/usecases/QueryShelfUseCaseTest.java`: test now stubs `Shelf` getters and asserts mapped response IDs.
- `src/test/java/com/penrose/bibby/library/stacks/shelf/core/application/ShelfServiceTest.java`: added service-level delegation test for `findAll()` on `ShelfResponse`.
- `docs/ADR-0004-inbound-port-model-for-shelf-query-boundary.md`: documentation commit captures broader inbound-port-model rationale.
- `docs/the-devlogs/devlog-2026-02-24-Shelf-Response-Port-Model.md`: devlog commit records implementation narrative and follow-up ideas.

### Candidate ADRs (optional)

- ADR candidate: Migrate `ShelfFacade.findShelfById` to a response model contract. This is the main remaining query boundary leak of domain `Shelf`.
- ADR candidate: Define collection immutability policy for inbound port models. `ShelfPortModelMapper` currently forwards `bookIds` without defensive copy.
