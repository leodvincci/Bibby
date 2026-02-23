# Weekly Engineering Review
Week Ending: 2026-02-22 (Sunday)
Branch(es): `refactor/create-shelf-accept-domain-object` (current), `main`, plus 10 feature branches merged via PRs
Repo State: Clean, up to date with `origin/refactor/create-shelf-accept-domain-object`

**Activity Summary:** 85 non-merge commits, 34 PRs merged, ~11,400 insertions / ~4,700 deletions across ~120 unique files. Extremely high-velocity week focused almost entirely on architectural refactoring.

---

## 1) What I Shipped (Outcomes)

- Completed Repository Pattern implementation for both Shelf and Bookcase domains, eliminating direct JPA coupling from service layers.
- Restructured Shelf domain model into proper `core/domain/model/` package with a `BookAccessPort` to decouple Shelf from the Book module.
- Restructured Book domain with proper value objects (`BookId`, `Isbn`, `Title`) and eliminated 161 lines of redundant code from `BookService`.
- Relocated all ports and DTOs across the codebase to enforce hexagonal architecture (`core/ports/inbound`, `core/ports/outbound`, `api/dtos`).
- Migrated user registration from `library.registration` into a dedicated `identity` bounded context.
- Extracted Shelf application logic into explicit use cases (`CreateShelfUseCase`, `QueryShelfUseCase`, `DeleteShelvesUseCase`) with dedicated tests.
- Decoupled `ShelfFacade` and `ShelfService` from DTO mapping; deleted `ShelfDTOMapper` from core — mapping is now a boundary concern.
- Renamed `findAllShelves` to `findShelvesByBookcaseId` to match actual query semantics.
- Refactored `CreateShelfUseCase` to accept a `Shelf` domain object instead of four primitives, pushing validation into the domain constructor.
- Removed redundant validation from `BookcaseService.addShelf` (now owned by `Shelf` domain model).
- Removed `BookFacadeAdapter` indirection; `BookService` now implements `BookFacade` directly.
- Removed `ShelfFactory`, unused `BrowseShelfUseCase`, and unused `AuthorAccessPortAdapter` — dead code eliminated.

---

## 2) Themes (Grouped Work)

### Theme 1: Hexagonal Architecture Enforcement
- **Summary:** Massive structural refactoring to place all port interfaces in `core/ports/{inbound,outbound}` and DTOs in `api/dtos/`. Affected Author, Book, Bookcase, and Shelf modules.
- **Evidence:** Commits `63ae13d`, `90e89a8`, `255c2b9`. Key files: 46 files in the port relocation commit alone. Touched every CLI command, every controller, every adapter.
- **Impact:** Dependency arrows now consistently point inward (Infra → Core ← API). Enables independent module evolution and makes architecture violations immediately visible via package structure.
- **Risks:** Pure structural change, no behavior change — but high blast radius (46 files in one commit). Inconsistent port naming (`port` vs `ports`) remains.
- **Verification:** `./mvnw test` — all imports should resolve; any breakage is a compile error, not a runtime surprise.

### Theme 2: Shelf Use Case Extraction & Domain-Centric Refactor
- **Summary:** Decomposed monolithic `ShelfService` into three focused use cases, then evolved `CreateShelfUseCase` to accept a `Shelf` domain object. Deleted `ShelfDTOMapper` from core, moved DTO mapping to boundary layers.
- **Evidence:** Commits `4cd5c98`, `802b6b2`, `e5a7bdc`, `92c6f5f`, `b1b5920`, `e02ce9b`. Key files: `CreateShelfUseCase.java`, `ShelfService.java`, `ShelfDomainRepository.java`, `ShelfMapper.java`.
- **Impact:** Validation now lives in one place (Shelf constructor). Use cases are independently testable. `ShelfService` is a thin facade. 457 lines of focused use case code added; ~150 lines of duplicated validation removed.
- **Risks:** Validation removal from `BookcaseService.addShelf` — if any caller bypasses Shelf construction, validation is skipped. Mitigated: all paths go through `Shelf` constructor.
- **Verification:** `./mvnw test -pl . -Dtest="*ShelfServiceTest,*CreateShelfUseCaseTest,*BookcaseServiceTest"` — verify no regressions in validation behavior.

### Theme 3: Repository Pattern Completion (Shelf + Bookcase)
- **Summary:** Introduced `ShelfDomainRepository` and `BookcaseRepository` ports with infrastructure adapters, removing direct JPA repository usage from application services.
- **Evidence:** Commits `3ad35c7`, `25389fa`. Key files: `ShelfDomainRepository.java`, `ShelfDomainRepositoryImpl.java`, `BookcaseRepositoryImpl.java`, `BookcaseService.java`.
- **Impact:** Application services now depend only on domain port interfaces. JPA is an implementation detail, swappable without touching business logic.
- **Risks:** `save()` method signature changed to accept primitives (Shelf), then later refactored to accept `Shelf` domain object — verify final state is consistent.
- **Verification:** `./mvnw test -Dtest="*ShelfServiceTest,*BookcaseServiceTest,*BookDomainRepositoryImplTest"`

### Theme 4: Cross-Module Decoupling (Book ↔ Shelf)
- **Summary:** Created `BookAccessPort` (outbound port in Shelf's domain) and `BookAccessAdapter` (in Book's infrastructure) to break the direct dependency between Shelf and Book modules.
- **Evidence:** Commits `90e89a8`, `3e84a2b`, `4473aca`, `f55df60`. Key files: `BookAccessPort.java`, `BookAccessAdapter.java`, `ShelfDomainRepositoryImpl.java`.
- **Impact:** Shelf module no longer imports anything from Book. Dependency arrow flipped: Book adapts to Shelf's port (DIP compliance). Also removed dead `AuthorAccessPortAdapter` and `BookFacadeAdapter`.
- **Risks:** Low — structural only. `BookAccessAdapter` introduces a thin indirection; verify it doesn't silently swallow exceptions.
- **Verification:** `./mvnw test -Dtest="*BookAccessAdapterTest"`

### Theme 5: Bounded Context Reorganization
- **Summary:** Migrated user registration into a standalone `identity` bounded context. Migrated Booklist from `classification` to `cataloging`. Removed `bookcaseLabel` in favor of `bookcaseLocation`.
- **Evidence:** Commits `453d3a9`, `08335ed`, `afcc956`, `7ea84a0`. Key files: all `identity/` package files, `Booklist.java`, `BookcaseEntity.java`.
- **Impact:** Domain boundaries now match the ubiquitous language. Identity is no longer buried inside `library`. Bookcase model is cleaner (single location field).
- **Risks:** `bookcaseLabel` → `bookcaseLocation` rename may break any external consumers or saved queries referencing the old field name.
- **Verification:** `./mvnw test` — full suite; search codebase for any remaining `bookcaseLabel` references.

### Theme 6: Documentation & Engineering Artifacts
- **Summary:** Added 12+ devlogs, architecture review documents, bounded-context READMEs, and a sprint plan. Refreshed the main README.
- **Evidence:** Commits `a4d0adc`, `f82585b`, `653b143`, `5370bad`, `d589e2e`, `520e4fb`, `7fb4f82`, and others in `docs/`.
- **Impact:** Decision trail is preserved. Future-you (or any collaborator) can understand *why* each refactor happened, not just *what* changed.
- **Risks:** None. Documentation-only.

---

## 3) Risk Register

| # | Risk | Why it's a risk | Likelihood | Impact | Mitigation |
|---|------|----------------|------------|--------|------------|
| 1 | **Validation now solely in Shelf constructor** — removed from `BookcaseService.addShelf` and `CreateShelfUseCase` | If any code path creates a Shelf without the constructor (e.g., reflection, deserialization), validation is bypassed | Low | High | All creation paths use the constructor. Add a test that verifies the constructor rejects invalid inputs. |
| 2 | **Inconsistent port package naming** (`core/port/` vs `core/ports/`) | Makes it harder to enforce architectural rules via package checks or ArchUnit | Med | Low | Standardize to `core/ports/` everywhere in a follow-up. |
| 3 | **46-file port relocation in a single commit** (`63ae13d`) | If a single import was missed, it's a compile error discovered only at build time; hard to review | Low | Low | Already merged and tests pass. Low residual risk. |
| 4 | **`bookcaseLabel` removal** (`afcc956`) | Any external integration, saved report, or DB query referencing `bookcaseLabel` will break silently | Low | Med | Search for `bookcaseLabel` across the full stack including SQL/HQL. Verified via `09f9118` fix to repository method. |
| 5 | **`BookService` directly implements `BookFacade`** (removed `BookFacadeAdapter` indirection) | Tight coupling: changes to the facade contract now directly affect the service class | Low | Low | Acceptable trade-off — the adapter was pure pass-through. Monitor if `BookFacade` grows. |
| 6 | **Removed tests from `BookcaseServiceTest`** (`e02ce9b`) | 31 lines of validation tests deleted; coverage must exist elsewhere | Low | Med | Validation is covered in Shelf domain tests. Verify with `./mvnw test -Dtest="*ShelfTest"` or equivalent. |
| 7 | **ShelfDomainRepository.save() signature changed twice** (primitives → Shelf) | If any caller wasn't updated, it's a compile error — but churn adds review risk | Low | Low | Final state is clean (accepts `Shelf`). Already compiles. |

---

## 4) Tech Debt Ledger

### Debt Paid Down
- Eliminated `ShelfDTOMapper` from core — DTO mapping no longer leaks into domain layer
- Removed `ShelfFactory` — replaced with simpler `Shelf` constructor + `ShelfFacade`
- Removed `BookFacadeAdapter` — unnecessary indirection eliminated
- Removed `BrowseShelfUseCase` and its test — dead code
- Removed `AuthorAccessPortAdapter` — unused bridge
- Removed `bookMapper` method and unused `AuthorDTO` import from `BookService`
- Eliminated `bookcaseLabel` / derived state from `BookcaseEntity`
- Consolidated 161 lines of redundant code out of `BookService`
- Consolidated redundant query methods in `ShelfService`, `ShelfFacade`, and `ShelfDomainRepository`

### Debt Created (or Deferred)
- **Inconsistent port package naming** (`port` vs `ports`) — deferred because the rename touches many files and is purely cosmetic. Cost if left: confusing for new contributors, harder to enforce with ArchUnit.
- **`BookJpaRepository` still injected in `ShelfService`** (noted in `3ad35c7`) — candidate for port extraction. Cost if left: one remaining infrastructure dependency in the application layer.
- **No ArchUnit tests** to enforce the hexagonal package structure — all the work this week could regress via a careless import. Cost if left: silent architecture erosion.
- **Unused imports in `ShelfDomainRepository`** (noted in commit message) — minor but signals review gaps.

---

## 5) Quality & Testing

### Tests Added/Changed
- `CreateShelfUseCaseTest` — 125 lines added, then simplified to delegation-only (final: focused)
- `DeleteShelvesUseCaseTest` — 71 lines (new)
- `QueryShelfUseCaseTest` — 137 lines (new)
- `BookAccessAdapterTest` — 56 lines (new, cross-module adapter)
- `PromptOptionsTest` — new, covers bookcase option menu rendering
- `AuthorEntityTest` — comprehensive unit tests (new)
- `BookDomainRepositoryImplTest` — expanded with 136+ lines of new scenarios
- `ShelfServiceTest` — updated twice (repository mock swap + `ArgumentCaptor` for Shelf)
- `BookcaseServiceTest` — updated; 31 lines of redundant validation tests removed

### Suggested Minimum Confidence Suite
```bash
./mvnw test
```
If time-constrained:
```bash
./mvnw test -Dtest="*ShelfServiceTest,*CreateShelfUseCaseTest,*DeleteShelvesUseCaseTest,*QueryShelfUseCaseTest,*BookcaseServiceTest,*BookAccessAdapterTest,*BookDomainRepositoryImplTest,*PromptOptionsTest"
```

### High-Value Test Cases to Add Next
1. **Shelf constructor validation test** — verify all invariants (null bookcaseId, invalid position, empty label, zero capacity) throw on construction
2. **ArchUnit test** — enforce `core` packages never import from `infrastructure` or `api`
3. **Integration test for Shelf creation end-to-end** — controller → use case → repository → DB
4. **`BookcaseService.addShelf` integration** — confirm validation fires via Shelf constructor in the full call chain
5. **`findShelvesByBookcaseId` with no results** — verify empty-list behavior post-rename

---

## 6) Architecture Notes

### Dependency Direction (Post-Refactor)
```
CLI Commands ──→ Facades (ports/inbound)
                       │
Web Controllers ──→ Core (domain + application)
                       │
                  ports/outbound
                       │
              Infrastructure (adapters, JPA, mappers)
```

### Boundary Wins
- **Shelf ↔ Book decoupling via `BookAccessPort`**: Shelf defines what it needs; Book adapts. True Dependency Inversion.
- **DTO mapping pushed to boundaries**: `ShelfDTOMapper` deleted from core. Controllers and adapters own the mapping. Domain stays pure.
- **Identity bounded context created**: User registration is no longer a sub-package of `library`. Clean separation of concerns.
- **Use case classes**: `CreateShelfUseCase`, `QueryShelfUseCase`, `DeleteShelvesUseCase` give each operation a named, testable home.

### Remaining Violations
- `BookJpaRepository` still referenced directly in `ShelfService` (noted as future port extraction candidate)
- `port` vs `ports` naming inconsistency across Book and Shelf modules

---

## 7) Next Week Focus Plan

### Must-Do (Highest Leverage)
1. **Standardize port package naming to `core/ports/`**
   - Files: Book module's `core/port/` → `core/ports/`
   - Done when: `find . -path "*/core/port/*" | wc -l` returns 0

2. **Extract `BookJpaRepository` dependency from `ShelfService` into a port**
   - Files: `ShelfService.java`, create port in `shelf/core/ports/outbound/`
   - Done when: `ShelfService` has zero infrastructure imports

3. **Add ArchUnit test to enforce hexagonal boundaries**
   - Files: New test class `ArchitectureTest.java`
   - Done when: Test fails if any `core` class imports from `infrastructure` or `api`

4. **Merge `refactor/create-shelf-accept-domain-object` into `main`**
   - Done when: PR approved, merged, branch deleted

### Should-Do (Hardening)
1. **Add Shelf domain constructor validation tests** — cover all edge cases in a dedicated `ShelfTest`
2. **Add integration test for full Shelf creation flow** (controller → DB round-trip)
3. **Clean up unused imports in `ShelfDomainRepository`** flagged in commit `3ad35c7`
4. **Verify no remaining `bookcaseLabel` references** in SQL, HQL, or templates
5. **Review `BookMapper` / `BookMapperTwo` duplication** — consolidate if overlapping

### Nice-to-Have (Exploration)
1. **Spike: ArchUnit rules for bounded context isolation** (prevent `stacks` importing from `cataloging` directly)
2. **Spike: Event-based decoupling between Shelf and Book** (replace `BookAccessPort` with domain events)
3. **Evaluate test coverage report** (`jacoco`) to find gaps in the new use case classes
4. **Consider a `ShelfBuilder`** similar to `BookBuilder` for complex construction scenarios

---

## 8) Lessons (Tied to This Week's Evidence)

- **Pushing validation into the domain constructor is a force multiplier.** Once `Shelf` owned its invariants, redundant validation in `BookcaseService` and `CreateShelfUseCase` could be safely deleted — 42+ lines of defensive code removed with higher confidence (`b1b5920`, `e02ce9b`).

- **Deleting code is the highest-leverage refactoring.** `ShelfDTOMapper` (40 lines), `BookFacadeAdapter`, `BrowseShelfUseCase`, `ShelfFactory` — each removal simplified the dependency graph. The codebase got *smaller* while gaining *more structure* (`92c6f5f`, `ff095f0`).

- **Port-adapter pattern pays off immediately for cross-module boundaries.** The `BookAccessPort` creation (`90e89a8`) broke the Shelf → Book coupling in one commit. The adapter in Book's infrastructure is trivial, but the architectural benefit is permanent.

- **Renaming methods to match semantics prevents future bugs.** `findAllShelves` → `findShelvesByBookcaseId` (`7958027`) — the old name was a lie. It always filtered by bookcase ID. Anyone reading the old name would assume it returned *all* shelves globally.

- **Large structural refactors (46 files) are safer than they look *if* they're import-only.** The port relocation commit (`63ae13d`) touched 46 files but changed zero behavior. The compiler is the test suite for this kind of change.

- **Writing devlogs at commit time captures intent that commit messages can't.** The 12+ devlogs written this week document *why* each refactor happened, architectural trade-offs considered, and follow-up recommendations — invaluable for the next time this code is revisited.

- **Use case decomposition makes `ShelfService` *boring* — and that's the goal.** After extracting `CreateShelfUseCase`, `QueryShelfUseCase`, and `DeleteShelvesUseCase`, `ShelfService` is a thin delegation layer with no business logic. Boring services are correct services (`4cd5c98`, `802b6b2`).
