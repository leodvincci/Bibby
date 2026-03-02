# Weekly Engineering Review
**Week Ending:** 2026-03-02 (Monday)
**Branch(es):** `300-bookcontroller-bypasses-inbound-port-depends-on-shelfservice-concrete-class`, `refactor/bookcase-shelf-access-port`, `refactor/bookcase-domain-model-and-use-cases`, `refactor/remove-unused-fields-and-dependencies`, `refactor/shelf-hex-boundary-and-method-renames`, `refactor/shelf-port-response-model`, `refactor/shelf-findall-response-and-docs`, `refactor/shelf-domain-hardening-and-mapper-relocation`, `feat/shelf-placement-and-book-mapper-relocation`, `feat/place-book-usecase-port-wiring`, `refactor/create-shelf-port-and-test-cleanup`, `refactor/bookcase-infra-restructure-and-usecase-port`
**Repo State:** Clean, synced with origin
**Volume:** 92 non-merge commits, 28 PRs merged (#314–#342), 153 files changed, +14,152 / -2,770 lines

---

## 1) What I Shipped (Outcomes)

- Decoupled BookController from Stacks module internals by moving `placeBookOnShelf` orchestration behind `BookFacade` (closes #300)
- Introduced `ShelfAccessPort` as an outbound port in bookcase module, eliminating direct bookcase-to-shelf core dependency
- Extracted bookcase use cases (`CreateBookcaseUseCase`, `DeleteBookcaseUseCase`, `QueryBookcaseUseCase`) from monolithic `BookcaseService`
- Split `ShelfFacade` into `ShelfQueryFacade` and `ShelfCommandFacade` (CQRS-style read/write separation)
- Introduced `ShelfResponse` port model to stop leaking `Shelf` domain objects through inbound ports
- Hardened `Shelf` domain model with defensive setters and renamed `bookIds` to `books`
- Extracted `CreateBookcaseUseCasePort`, `PlaceBookOnShelfUseCasePort`, `CreateShelfUseCasePort`, and `DeleteShelvesUseCasePort` as fine-grained inbound port interfaces
- Wired controllers and CLI commands to use-case ports instead of monolithic facades
- Restructured bookcase and shelf infrastructure into `persistence/` subpackages with consistent `*Adapter` naming
- Removed unused dependencies from `AuthorFacadeImpl`, `BookDomainRepositoryImpl`, and `BookBuilder`
- Added 183+ lines of Shelf domain model tests (validation guards, positive cases, mapper coverage)
- Wrote ADR-0001 (ShelfAccessPort), ADR-0007 (shelf placement consolidation), and 4 devlog entries documenting the week's decisions

## 2) Themes (Grouped Work)

### Theme 1: Cross-Module Boundary Enforcement (Issue #300)
- **Summary:** BookController was reaching directly into ShelfService and BookcaseService. Orchestration was relocated behind `BookFacade`, a new `bookCommandUseCases` class validates shelf existence/capacity via `ShelfAccessPort`, and `BookDomainRepositoryImpl` was stripped to persistence-only.
- **Evidence:** `ed999a4`, `6c61a49`, `2064cbf`, `67dbef7`, `9da6619`, `2c3b1f0`, `cb1f076`
- **Key files:** `bookCommandUseCases.java` (new), `BookFacade.java`, `BookDomainRepositoryImpl.java`, `ShelfAccessPort.java` (bookcase), `BookCaseController.java`
- **Impact:** Eliminated the most egregious hexagonal boundary violation. Book module no longer imports any Stacks concrete class. Controller is now a thin adapter.
- **Risks:** `bookCommandUseCases` throws `IllegalStateException` — needs a global handler (added in `df6da16`).
- **Verification:** PR #301, #316 merged. `mvn test` passes.

### Theme 2: Bookcase Domain Model & Use-Case Extraction
- **Summary:** Enriched `Bookcase` domain model (added `userId`, `BookcaseMapper`), extracted three use cases from `BookcaseService`, migrated facade return types from `BookcaseDTO` to `Bookcase` domain type, and extracted `CreateBookcaseUseCasePort`.
- **Evidence:** `4065480`, `3cd7d48`, `f662174`, `8b6fcec`, `a326ac8`, `83e1fd7`, `79404a9`, `5b1e281`
- **Key files:** `CreateBookcaseUseCase.java`, `DeleteBookcaseUseCase.java`, `QueryBookcaseUseCase.java`, `BookcaseService.java`, `BookcaseFacade.java`, `CreateBookcaseUseCasePort.java` (new)
- **Impact:** BookcaseService went from 131-line god class to pure delegator. Each use case is independently testable. DTO conversion pushed to the web/CLI boundary where it belongs.
- **Risks:** `findBookCaseById` changed from `Optional<BookcaseDTO>` to `Bookcase` (null return) — API contract change.
- **Verification:** PR #317–#319, #342 merged. Use-case tests added in `a231962`.

### Theme 3: Shelf Facade Split & Port Model Migration
- **Summary:** `ShelfFacade` split into `ShelfQueryFacade` (6 methods) and `ShelfCommandFacade` (3 methods). Introduced `ShelfResponse` record as the inbound port model, replacing raw `Shelf` domain objects at the boundary. Migrated all consumers.
- **Evidence:** `121d910`, `a974cee`, `94daea4`, `5c6de4c`, `5a82ef9`, `12ac516`
- **Key files:** `ShelfQueryFacade.java` (new), `ShelfCommandFacade.java` (new), `ShelfResponse.java` (new), `ShelfPortModelMapper.java` (new), `QueryShelfUseCase.java`
- **Impact:** Consumers declare exactly what they need (read vs write). Domain model no longer leaks through the port. 17 files updated to new facades — every consumer migrated in one pass.
- **Risks:** `ShelfService` implements both interfaces; Spring autowires by type, which works but could confuse if a second implementation appears.
- **Verification:** PR #322–#327 merged. `BookCreateIsbnCommandsTest` and `PromptOptionsTest` mocks updated.

### Theme 4: Shelf Domain Hardening & Test Coverage
- **Summary:** Moved validation into defensive setters, renamed `bookIds` to `books`, added null guards, and built comprehensive test coverage for the Shelf model.
- **Evidence:** `c6ebf3c`, `0f4d88f`, `d5691cc`, `dc81961`, `887cb7e`, `da44278`
- **Key files:** `Shelf.java`, `ShelfTest.java` (+183 lines), `ShelfPortModelMapperTest.java`
- **Impact:** Shelf domain is now the strongest-tested domain model in the codebase. Validation guards catch bad data at construction time, not at persistence time.
- **Risks:** `setShelfId` intentionally allows null for the create path (ID assigned by DB). Documented in commit, but could confuse future contributors.
- **Verification:** PR #328–#331 merged. Tests cover all setters, edge cases, and the mapper.

### Theme 5: Infrastructure Restructuring & Naming Consistency
- **Summary:** Relocated persistence classes into `infrastructure/persistence/` subpackages across bookcase, shelf, and cataloging modules. Renamed `*Impl`/`*PortImpl` to `*Adapter`. Flattened unnecessary `outbound` subpackages. Renamed `ShelfOptionResponse` to `ShelfOptionResponseDTO`.
- **Evidence:** `e721247`, `b34fdbb`, `0a72d47`, `c448ede`, `566785f`
- **Key files:** `BookcaseRepositoryAdapter.java`, `BookDomainRepositoryAdaptor.java`, `PlacementRepositoryAdapter.java`, `ShelfRepositoryAdapter.java`
- **Impact:** Package structure now consistently communicates architectural intent. All adapters follow the same naming convention.
- **Risks:** High rename volume across 37 files. If any consumer outside the repo depends on FQCNs (e.g., Spring bean names by class), they'd break.
- **Verification:** PR #338, #341, #342 merged. `mvn test` passes after each commit.

### Theme 6: Use-Case Port Wiring (Fine-Grained Inbound Ports)
- **Summary:** Extracted 4 dedicated inbound port interfaces (`PlaceBookOnShelfUseCasePort`, `CreateShelfUseCasePort`, `DeleteShelvesUseCasePort`, `CreateBookcaseUseCasePort`) and wired controllers/CLI/cross-module adapters to them instead of monolithic facades.
- **Evidence:** `406d3cf`, `360eb75`, `51f3859`, `a9303da`, `0ec22ba`, `1af23cb`, `5b1e281`
- **Key files:** `PlaceBookOnShelfUseCasePort.java`, `CreateShelfUseCasePort.java`, `DeleteShelvesUseCasePort.java`, `CreateBookcaseUseCasePort.java`, `ShelfAccessPortAdapter.java`
- **Impact:** Each caller depends on exactly the capability it needs. Facade methods can be retired incrementally.
- **Risks:** Facades still expose the same methods — dual paths exist temporarily. Cleanup needed.
- **Verification:** PR #339–#342 merged. All tests pass.

---

## 3) Risk Register

| # | Risk | Technical Reasoning | Likelihood | Impact | Mitigation |
|---|------|-------------------|------------|--------|------------|
| 1 | **BookcaseFacade still exposes `createNewBookCase`** alongside the new `CreateBookcaseUseCasePort` | Dual paths for the same operation; one could drift out of sync or confuse new contributors | Med | Low | Remove from facade once all callers are confirmed on the port. Track as follow-up. |
| 2 | **`findBookCaseById` returns nullable `Bookcase` instead of `Optional`** | Callers that don't null-check will get NPEs at runtime; this is a silent contract change | Med | Med | Audit all call sites. Consider reverting to `Optional<Bookcase>` for safety. |
| 3 | **`ShelfService` implements both `ShelfQueryFacade` and `ShelfCommandFacade`** | Spring autowires by type, which works with one implementation. A second impl would cause `NoUniqueBeanDefinitionException` | Low | Med | Acceptable for now. Document the single-impl assumption. |
| 4 | **`setShelfId` allows null** | Domain model intentionally allows null ID for new entities, but nothing prevents accidentally nulling an existing shelf's ID | Low | Med | Consider a factory method (`Shelf.create(...)`) that returns ID-less shelf, vs a separate path for hydrating from DB. |
| 5 | **92 commits touching 153 files in one week — high velocity, high churn** | Increased probability of subtle regression that tests don't cover (especially integration paths) | Med | Med | Run full integration/E2E suite before next feature work. |
| 6 | **`BookcaseMapper` (DTO) now lives in `web.controllers` but CLI commands also import it** | CLI layer depends on a web-layer class — a layering inversion | Med | Low | Extract to a shared mapping package or define a separate CLI mapper. |
| 7 | **Unused `ShelfCommandFacade` still injected in bookcase `ShelfAccessPortAdapter` constructor** | Dead parameter; confusing to readers and wastes a Spring injection | Low | Low | Remove the parameter. Identified, not yet fixed. |
| 8 | **`BookDomainRepositoryAdaptor` spelling (not `Adapter`)** | Inconsistent with every other adapter in the codebase (`*Adapter`) | Low | Low | Rename to `BookDomainRepositoryAdapter` for consistency. |
| 9 | **Test packages don't always match source packages after renames** | e.g., `ShelfAccessPortAdapterTest` is still in `adapter.outbound` while the source is in `adapter` | Low | Low | Realign test packages in a cleanup pass. |

---

## 4) Tech Debt Ledger

### Debt Paid Down
- **BookController no longer orchestrates across 3 modules** — orchestration moved to `bookCommandUseCases` behind `BookFacade`
- **BookDomainRepositoryImpl stripped of business logic** — no longer calls `ShelfFacade` or holds unused `AuthorService`
- **Bookcase module decoupled from shelf internals** via `ShelfAccessPort` outbound port
- **ShelfFacade monolith eliminated** — replaced with focused query/command facades
- **Domain model no longer leaks through ports** — `ShelfResponse` port model introduced
- **Shelf validation guards centralized in setters** — no more scattered null checks
- **Unused dependencies removed** from `AuthorFacadeImpl`, `BookBuilder`, `BookDomainRepositoryImpl`
- **Package structure reflects architectural intent** — persistence classes grouped, adapters consistently named

### Debt Created (or Deferred)
- **Dual facade + port paths for bookcase creation** — `BookcaseFacade.createNewBookCase` and `CreateBookcaseUseCasePort.createNewBookCase` both exist. Cost if left: confusion, potential drift. Fix: remove facade method.
- **`BookcaseFacade` still returns `BookcaseEntity` in some methods** — persistence type leaking through inbound port. Cost if left: coupling, same problem we fixed for shelf. Fix: introduce `BookcaseResponse` port model.
- **`BookcaseMapper` shared between web and CLI layers** — lives in `web.controllers` but used by CLI. Cost if left: layering violation. Fix: extract to shared package.
- **Test packages misaligned with source packages** after renames. Cost if left: confusion when navigating tests. Fix: batch rename.
- **`ShelfOptionResponseDTO` still used by cataloging's `ShelfAccessPort`** — shelf API DTO crossing module boundary. Cost if left: coupling. Fix: define cataloging-owned port model.
- **No integration test verifying the full bookcase creation flow** end-to-end (controller -> port -> use case -> repo -> shelf provisioning).

---

## 5) Quality & Testing

### Tests Added/Changed
- `ShelfTest.java` — +183 lines covering all setter validation guards, positive construction, edge cases
- `ShelfPortModelMapperTest.java` — mapper unit tests added
- `BookDomainRepositoryAdaptorTest.java` — expanded coverage; class/field renames applied
- `CreateBookcaseUseCaseTest.java`, `BookcaseServiceTest.java` — updated for use-case extraction and import changes
- `PlacementRepositoryAdapterTest.java`, `BookAccessPortAdapterTest.java`, `ShelfAccessPortAdapterTest.java` — updated imports/references
- `BookCreateIsbnCommandsTest`, `PromptOptionsTest` — mocks updated for facade split
- Redundant/low-signal tests pruned in `5b89b6c`

### Tests Run
- `mvn test` run and passing as of the final commit (`5b1e281`)

### Minimum Confidence Suite
```bash
mvn clean test                          # Full unit suite
mvn -pl . spring-boot:run               # Smoke-test the app boots (no wiring errors)
```

### Targeted Tests to Add Next Week
1. **Integration test for bookcase creation** — POST `/api/bookcases/create` end-to-end, verifying shelves are provisioned
2. **Integration test for `placeBookOnShelf`** — verify capacity check + persistence in one flow
3. **`CreateBookcaseUseCasePort` unit test** — verify the port contract is satisfied by `CreateBookcaseUseCase`
4. **`BookcaseMapper` (web) unit test** — simple but catches regressions if `Bookcase` fields change
5. **`Shelf.create()` null-ID path test** — explicitly document and test the ID-less creation path
6. **Negative test for `DeleteBookcaseUseCase`** — what happens when bookcase ID doesn't exist?

---

## 6) Architecture Notes

### Dependency Direction Wins
The week's central achievement: **every cross-module call now flows through a port**.

```
Before (Mon 02/23 morning):
  BookController -> ShelfService (concrete)
  BookController -> BookcaseService (concrete)
  BookDomainRepositoryImpl -> ShelfFacade (cross-module from infra)

After (Mon 03/02):
  BookController -> BookFacade (inbound port)
      BookService -> bookCommandUseCases -> ShelfAccessPort (outbound port)
          ShelfAccessPortAdapter -> ShelfQueryFacade (shelf inbound port)

  BookCaseController -> CreateBookcaseUseCasePort (inbound port)
      CreateBookcaseUseCase -> ShelfAccessPort (outbound port)
          ShelfAccessPortAdapter -> CreateShelfUseCasePort (shelf inbound port)
```

### Boundary Wins
- `ShelfQueryFacade` / `ShelfCommandFacade` — CQRS-style split; consumers declare read vs write intent
- `ShelfResponse` port model — domain model no longer leaks through the shelf inbound port
- `ShelfAccessPort` (bookcase) — bookcase no longer imports any shelf core class
- Use-case ports (`PlaceBookOnShelfUseCasePort`, etc.) — callers depend on single-method contracts

### Boundary Violations Remaining
- `BookcaseFacade` returns `BookcaseEntity` in some methods (persistence type through inbound port)
- `BookcaseMapper` (DTO) in web package imported by CLI commands
- `ShelfOptionResponseDTO` crosses from shelf API into cataloging's port adapter

---

## 7) Next Week Focus Plan

### Must-Do (Highest Leverage)
1. **Introduce `BookcaseResponse` port model**
   - Files: `BookcaseFacade.java`, new `BookcaseResponse.java` in `bookcase/core/ports/inbound/`, `QueryBookcaseUseCase.java`, `BookcaseMapper.java`
   - Done when: `BookcaseFacade` returns only port models, never `BookcaseEntity` or `Bookcase` domain objects

2. **Remove `createNewBookCase` from `BookcaseFacade`**
   - Files: `BookcaseFacade.java`, `BookcaseService.java`
   - Done when: all callers use `CreateBookcaseUseCasePort` and the facade method is deleted

3. **Add integration test for bookcase creation flow**
   - Files: new `BookcaseCreationIntegrationTest.java`
   - Done when: test covers POST -> use case -> repo -> shelf provisioning -> response, with Spring context

4. **Fix `BookcaseMapper` layering violation**
   - Files: move or duplicate mapper so CLI and web don't cross layers
   - Done when: CLI imports no `web.controllers` class

### Should-Do (Hardening)
1. Realign test packages to match source packages after all the renames
2. Remove unused `ShelfCommandFacade` parameter from bookcase `ShelfAccessPortAdapter`
3. Fix `BookDomainRepositoryAdaptor` spelling -> `BookDomainRepositoryAdapter`
4. Add unit test for `DeleteBookcaseUseCase` (negative case: nonexistent ID)
5. Audit `BookcaseFacade.findBookCaseById` callers for null-safety (or revert to `Optional`)

### Nice-to-Have (Exploration)
1. Spike: ArchUnit test to enforce port/adapter dependency rules automatically
2. Spike: extract a `BookcaseResponse` port model and see if `BookcaseDTO` can be generated from it
3. Investigate whether `@Lazy` on `BookAccessPortAdapter` (added in `4672154`) masks a deeper circular dependency that should be structurally resolved
4. Consider a `ShelfCapacityPolicy` domain service to encapsulate capacity checks instead of inline logic in `bookCommandUseCases`

---

## 8) Lessons (Tied to This Week's Evidence)

1. **Small, focused inbound ports are easier to wire than monolithic facades.** The `CreateBookcaseUseCasePort` extraction (1 method, 1 interface) was trivial to wire into both controller and CLI. The old `BookcaseFacade` (7+ methods) forced every caller to depend on capabilities they didn't use.

2. **Rename-heavy refactors compound risk.** 37 files changed in the infrastructure restructuring commit alone. Splitting renames into their own commits (as we did: renames first, behavior second) made each commit reviewable and revertable.

3. **Port models pay off immediately.** Introducing `ShelfResponse` broke the Shelf domain model leak in one pass. Every consumer that used to reach into `Shelf.getBooks()` now gets a flat record. The cost was one mapper class and one record — trivial.

4. **Facade splits reveal who reads vs writes.** After splitting `ShelfFacade`, 14 of 17 consumers only needed `ShelfQueryFacade`. That's strong evidence the command surface is correctly narrow.

5. **Defensive setters > constructor-only validation.** Moving guards into `Shelf` setters (commit `0f4d88f`) meant the same rules apply whether constructing, hydrating from DB, or mutating. The test suite caught the `isFull()` null-check that became unnecessary.

6. **Dual paths (facade + port) are transitional debt, not design.** `BookcaseFacade.createNewBookCase` still exists alongside `CreateBookcaseUseCasePort`. This is fine *this week* because it enabled incremental migration. But it must be cleaned up next week or it becomes permanent confusion.

7. **Document decisions in the moment.** Writing ADR-0001 and ADR-0007 alongside the code changes (not after) captured context that commit messages alone couldn't convey. The devlogs will be invaluable when revisiting these decisions in 3 months.
