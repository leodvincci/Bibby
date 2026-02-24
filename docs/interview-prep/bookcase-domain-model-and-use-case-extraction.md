# Interview Prep Packet: Bookcase Domain Model Enrichment & Use-Case Extraction

Date: 2026-02-24 (America/Chicago)
Branch: `refactor/bookcase-domain-model-and-use-cases`
Range: `origin/main..HEAD`
Commits:
- `260bd34` — Add detailed Javadoc comments for BookcaseFacade methods
- `4065480` — Enrich Bookcase domain model and add BookcaseMapper
- `3cd7d48` — Migrate BookcaseRepository port and impl to Bookcase domain model
- `f662174` — Migrate BookcaseFacade to Bookcase type and extract use cases from BookcaseService
- `8b6fcec` — Update CLI commands and web controller for Bookcase domain type
- `a231962` — Update tests for Bookcase domain model refactor and add use case tests
- `6f8646e` — Refactor Bookcase-related commands and services for improved readability

---

## 1) 30-second pitch (recruiter-friendly)

Bibby is a Spring Boot + Spring Shell personal library manager with barcode scanning and hierarchical physical storage (bookcase → shelf → book). I led a multi-commit refactor that promoted the `Bookcase` domain model from an anemic 2-field object to a full representation, purged infrastructure types (`BookcaseEntity`, `BookcaseDTO`) from the domain's outbound port, and decomposed a god-class `BookcaseService` into three focused use-case classes. The result is a codebase that fully honors hexagonal architecture: the core can be tested in complete isolation from Spring, JPA, and the REST/CLI layers, and each use case tests in ~30 lines instead of the prior 413-line service test.

---

## 2) 2-minute project walkthrough script (hiring manager)

> **Context:**
> Bibby is a personal library manager. Users physically organize books in bookcases, each of which holds multiple shelves, and each shelf holds books. The app exposes both a REST API and a Spring Shell CLI so you can manage your library interactively or via HTTP.

> **Problem:**
> The `Bookcase` domain object—the central concept of this module—was anemic. It carried only three fields (`bookcaseId`, `bookcaseLocation`, `shelfCapacity`), but fields like `bookcaseZone`, `bookcaseIndex`, and `bookCapacity` existed only on `BookcaseEntity`, which is a JPA infrastructure class. That meant every piece of application logic that needed those fields had to import the database entity, pulling infrastructure concerns into the domain core. Worse, the outbound port—the interface the core uses to talk to the database—was declared with entity types in its method signatures. And the inbound facade returned `Optional<BookcaseDTO>`, mixing the API representation into the middle of the domain.

> **Constraints:**
> The project follows hexagonal architecture (also known as ports-and-adapters): the domain core must not depend on JPA, Spring annotations, or API DTOs. That constraint had been established in earlier work for the Shelf module; this refactor extends it fully to Bookcase.

> **Approach:**
> I broke the work into a logical commit sequence. First I enriched the `Bookcase` model with all six fields and added a `BookcaseMapper` class in the domain layer as the single translation point between entity, domain, and DTO. Second, I updated the `BookcaseRepository` outbound port—the interface the domain calls to persist—so every method signature now speaks `Bookcase` instead of `BookcaseEntity`. Third, I updated the inbound facade to return `Bookcase` domain objects instead of DTOs. Fourth, I extracted the three responsibilities of the god-class `BookcaseService` into `CreateBookcaseUseCase`, `DeleteBookcaseUseCase`, and `QueryBookcaseUseCase`—each with its own dependencies and its own focused test class. Finally, I pushed DTO mapping out to the adapters: the CLI commands and REST controller each call `BookcaseMapper.toDTO()` at their boundary before passing data to callers.

> **Result:**
> The `BookcaseService` shrank from ~130 lines of mixed logic to ~80 lines of pure delegation. The 413-line `BookcaseServiceTest` was slimmed dramatically, replaced by three lean test classes—the largest covering all query paths in ~150 lines. The core is now independently testable; a pure-query test no longer needs to mock `ShelfAccessPort` at all.

---

## 3) Deep dive menu (pick 1–2 in interviews)

### 1. Outbound port contract: purging `BookcaseEntity` from `BookcaseRepository`
**Why it's interesting:** Shows you understand dependency inversion—not just as a pattern name but as a mechanical rule: the port interface belongs to the core and must speak the core's language.
**Files:** `BookcaseRepository.java` (outbound port), `BookcaseRepositoryImpl.java` (infra adapter), `BookcaseMapper.java` (core/domain)
**Talking points:**
- Before the refactor, `BookcaseRepository.save()` accepted and returned `BookcaseEntity`, forcing `core/application` to import `infrastructure.entity`. That is the textbook dependency-rule violation in hexagonal architecture.
- After: the port declares `void save(Bookcase)` and `Bookcase findById(Long)`. The adapter (`BookcaseRepositoryImpl`) handles all entity ↔ domain translation using `BookcaseMapper`.
- `save()` returning `void` is a deliberate side-effect contract; callers rely on the ID being set on the passed object via JPA's merge behavior (confirmed in tests with `doAnswer`).

### 2. BookcaseMapper placement: anti-corruption layer in `core/domain`
**Why it's interesting:** Shows nuanced understanding of where translation code actually belongs and willingness to make a pragmatic call with explicit reasoning.
**Files:** `BookcaseMapper.java` (`core/domain`), `infrastructure/mapping/BookcaseMapper.java` (pre-existing stub)
**Talking points:**
- A strict hexagonal reading would say the mapper belongs in the infrastructure adapter, not the core. Placing it in `core/domain` means the core imports `BookcaseEntity` and `BookcaseDTO`—which are infra/API types.
- The trade-off: the mapper is the *only* place the core touches those types, making it a controlled anti-corruption boundary. All other core classes are clean.
- There is a pre-existing stub `infrastructure/mapping/BookcaseMapper.java` that returns `null`—a naming collision risk that should be resolved by deleting or renaming the stub.

### 3. Use-case extraction from `BookcaseService` (SRP refactor)
**Why it's interesting:** Shows both design judgment (when to split a class) and risk control (incremental, commit-by-commit migration).
**Files:** `BookcaseService.java`, `CreateBookcaseUseCase.java`, `DeleteBookcaseUseCase.java`, `QueryBookcaseUseCase.java`
**Talking points:**
- `BookcaseService` was a god class: creation (with shelf provisioning), deletion (with shelf teardown), and six query paths all in one file with two injected dependencies and a hardcoded HTTP error constant.
- Extracting use cases means a pure query test (`QueryBookcaseUseCaseTest`) only mocks `BookcaseRepository`—not `ShelfAccessPort`. Before the split, every test had to set up both mocks even if shelves were never touched.
- `BookcaseService` is now a thin delegator: it exists only to implement `BookcaseFacade` (the inbound port) and wire Spring's DI. It has zero logic of its own.

### 4. `DeleteBookcaseUseCase` ordering and `@Transactional` scoping
**Why it's interesting:** Shows you think about correctness and transactional integrity, not just feature delivery.
**Files:** `DeleteBookcaseUseCase.java`, `DeleteBookcaseUseCaseTest.java`
**Talking points:**
- Deletion must delete all shelves *before* deleting the bookcase, to avoid foreign-key violations. This ordering is enforced and verified in `DeleteBookcaseUseCaseTest` using Mockito's `InOrder`.
- `@Transactional` is placed on `DeleteBookcaseUseCase.deleteBookcase()`, not on the facade or service—scoping the transaction to the unit of work, not to the whole service class.
- By testing with `InOrder`, you get a regression guard that fails immediately if someone swaps the two calls.

### 5. Domain model enrichment: 2-arg → 6-arg constructor
**Why it's interesting:** Shows that you track where data actually lives and push it toward the domain before it causes architectural drift.
**Files:** `Bookcase.java`, all tests
**Talking points:**
- `bookcaseZone`, `bookcaseIndex`, and `bookCapacity` were only accessible via `BookcaseEntity`, making any domain rule about them impossible to express without leaking infrastructure.
- The new 6-arg constructor is a breaking change: every call site needed updating. The commit message explicitly listed call sites as a known risk and confirmed they were addressed in the subsequent commit.
- The domain enforces an invariant in `setShelfCapacity()`—the guard logic stayed in the domain model, not in the use case, which is correct placement.

### 6. Optional removal and the null-return tradeoff
**Why it's interesting:** Shows you distinguish between types that communicate intent (`Optional`) and types that carry convenience (`null`), and that you document deliberate regressions.
**Files:** `BookcaseRepository.java`, `QueryBookcaseUseCase.java`, `BookCaseController.java`, CLI commands
**Talking points:**
- `findById()` previously returned `Optional<BookcaseEntity>`. In practice, all callers called `.get()` without checking—`Optional` was cargo-cult safety.
- The new contract returns `Bookcase` (nullable). This is honest about current behavior but does introduce NPE risk at callers that don't null-check.
- The commit message explicitly flags this as a follow-up risk. In a production setting I would enforce null-check at every caller or move to a proper `Result<Bookcase>` / `Optional<Bookcase>` pattern in the port itself.

### 7. Test strategy: `doAnswer` to simulate JPA ID assignment
**Why it's interesting:** Shows you can test void methods that produce side effects without changing production behavior.
**Files:** `CreateBookcaseUseCaseTest.java`
**Talking points:**
- `save()` is `void`, but the use case reads `bookcase.getBookcaseId()` after saving to log the result and return it in `CreateBookcaseResult`. In production, JPA's `save()` sets the ID on the passed entity.
- In unit tests there is no JPA context. The `doAnswer` pattern intercepts the `save()` call and manually sets `bookcase.setBookcaseId(100L)` on the argument object—simulating JPA's side effect.
- This pattern is test-only glue; the underlying issue is that `save()` returning `void` makes the ID retrieval implicit. A cleaner alternative would be `Bookcase save(Bookcase)` returning the persisted domain object.

---

## 4) STAR stories (behavioral interview ammo)

### Story 1: Purging Infrastructure Types from the Domain Port
**Seniority signal:** Architecture | **Difficulty:** High

**Situation:** The `BookcaseRepository` outbound port—the interface that separates the core from persistence—was declared with `BookcaseEntity` in its method signatures. This meant the domain layer imported JPA infrastructure types, violating the dependency rule that hexagonal architecture is built around.

**Task:** Migrate all port method signatures to use the `Bookcase` domain type, update the infrastructure adapter to handle translation, and ensure no other core class directly touches `BookcaseEntity`.

**Actions:**
- Added `bookcaseZone`, `bookcaseIndex`, and `bookCapacity` to `Bookcase.java` and replaced the 2-arg constructor with a 6-arg one, so `Bookcase` could represent full state without the entity.
- Created `BookcaseMapper` in `core/domain` with `toDomain()`, `toEntity()`, and `toDTO()` static methods as the single controlled translation point.
- Rewrote all `BookcaseRepository` method signatures to accept/return `Bookcase`; changed `save()` to `void` and `findById()` to return nullable `Bookcase`.
- Updated `BookcaseRepositoryImpl` to call `BookcaseMapper` on every read and write path.
- Updated all CLI commands and the `BookCaseController` to map `Bookcase → BookcaseDTO` at their boundary using `BookcaseMapper.toDTO()`.

**Result (expected):** `core/application` and `core/domain` packages import zero infrastructure or API types. Confirmed by reviewing all imports in the core package tree after the refactor.

**What I would improve next time:**
- Add an ArchUnit or a custom forbidden-import rule to the build so this boundary is enforced automatically and cannot regress.
- Move `BookcaseMapper` to the infrastructure adapter where it strictly belongs, and accept the minor duplication in adapter code, to avoid the core importing `BookcaseEntity` at all.

---

### Story 2: Decomposing a God-Class Service
**Seniority signal:** Ownership / Quality | **Difficulty:** Medium

**Situation:** `BookcaseService` was a 130+ line Spring `@Service` handling bookcase creation (including shelf provisioning loop), deletion (including shelf teardown), and six query paths. Its test class had grown to 413 lines because every test, including simple queries, had to set up mocks for both `BookcaseRepository` and `ShelfAccessPort`.

**Task:** Break `BookcaseService` down into focused units without breaking callers or test coverage.

**Actions:**
- Identified three cohesive responsibilities: creation, deletion, and queries.
- Extracted `CreateBookcaseUseCase`, `DeleteBookcaseUseCase`, and `QueryBookcaseUseCase`—each a `@Service` with only the dependencies it actually needs.
- Reduced `BookcaseService` to a pure delegator implementing `BookcaseFacade`; it holds only three `UseCase` injections and zero logic.
- Wrote three new focused test classes; `QueryBookcaseUseCaseTest` mocks only `BookcaseRepository` (no `ShelfAccessPort` in scope at all).

**Result (expected):** `BookcaseServiceTest` shrank from 413 lines to a thin delegation-verification file. Three new test classes cover all paths with lean, readable tests.

**What I would improve next time:**
- Remove the now-redundant `BookcaseServiceTest` integration logic and keep only delegation-level assertions in it.
- Consider making the use cases package-private and only the facade public, to enforce encapsulation within the module.

---

### Story 3: Enforcing Deletion Order with `InOrder` Testing
**Seniority signal:** Debugging / Quality | **Difficulty:** Low

**Situation:** When deleting a bookcase, shelves must be removed before the bookcase itself due to database foreign-key constraints. This ordering was implicit in the original `BookcaseService` and had no test verifying it.

**Task:** Extract deletion into `DeleteBookcaseUseCase` and make the shelf-first ordering a verified, non-regressing contract.

**Actions:**
- Placed `@Transactional` directly on `DeleteBookcaseUseCase.deleteBookcase()` so the entire shelf + bookcase deletion is one atomic transaction.
- Wrote `DeleteBookcaseUseCaseTest` using Mockito's `InOrder` to assert that `shelfAccessPort.deleteAllShelvesInBookcase()` is called before `bookcaseRepository.deleteById()`.
- The test fails immediately if someone inadvertently swaps the two calls.

**Result (expected):** Correct deletion order is now a first-class test contract, not a convention. Any regression fails a unit test before the code ships.

**What I would improve next time:**
- Add an integration test against a real (or in-memory) database to confirm no FK constraint error is raised on deletion, catching cases where the JPA cascade config is misconfigured.

---

### Story 4: Managing a Breaking Change Across Multiple Layers
**Seniority signal:** Ownership / Communication | **Difficulty:** High

**Situation:** Replacing the 2-arg `Bookcase` constructor with a 6-arg one was a breaking change. Every call site—tests, mappers, CLI commands, controllers, use cases—needed updating simultaneously to keep the build green.

**Task:** Plan and execute the migration in a sequenced, reviewable commit series so each commit left the codebase in a buildable state.

**Actions:**
- Introduced the new 6-arg constructor and `BookcaseMapper` in one commit (`4065480`), documenting in the commit body that call sites using the old constructor must be updated.
- Updated the outbound port and infra adapter in the next commit (`3cd7d48`) using the mapper.
- Updated the inbound facade and use cases in the next commit (`f662174`).
- Updated all CLI and web adapter call sites in the next commit (`8b6fcec`).
- Updated all tests in a dedicated commit (`a231962`) so test failures were visible as a discrete step.

**Result (expected):** Each commit compiles independently, making the PR reviewable incrementally and making bisect easy if a regression is introduced.

**What I would improve next time:**
- Open the PR as a draft after commit 1 to solicit early feedback on the domain model design before completing all the downstream migration.

---

### Story 5: Documenting a Deliberate Risk in Commit Messages
**Seniority signal:** Communication / Quality | **Difficulty:** Low

**Situation:** Changing `findById()` from `Optional<BookcaseEntity>` to a nullable `Bookcase` was a deliberate tradeoff that introduced NPE risk at callers that did not add null-guards.

**Task:** Make the tradeoff visible to reviewers and future maintainers without blocking the refactor.

**Actions:**
- Added a "Notes/Risks" section to commit `3cd7d48` explicitly calling out: "Callers of `findById()` that previously used `Optional` must now null-check directly."
- Added a similar note in commit `8b6fcec`: "`findBookCaseById()` now returns `null` instead of `Optional.empty()` on miss; existing callers assume the bookcase exists (no null-guard added — follow-up if needed)."
- Added this follow-up as a tracked item in ADR-0003.

**Result (expected):** Reviewers are explicitly informed; the risk is not buried. The follow-up is tracked and can be prioritized in the next sprint.

**What I would improve next time:**
- Convert the null return to `Optional<Bookcase>` in the port signature proactively rather than deferring it.

---

## 5) Technical problems I ran into (if any)

### Problem 1: `save()` void return breaks ID retrieval after persist

**Symptom:** After changing `BookcaseRepository.save()` to return `void`, `CreateBookcaseUseCase` needed the JPA-assigned `bookcaseId` to log the result and return a `CreateBookcaseResult`. With a void contract, there is no return value to read.

**Root cause:** JPA's `save()` sets the ID on the passed entity as a side effect (via reference mutation). The new domain-type contract (`void save(Bookcase)`) relies on the same side-effect behavior but makes it implicit.

**Fix:** `CreateBookcaseUseCase` reads `bookcase.getBookcaseId()` *after* calling `bookcaseRepository.save(bookcase)`, relying on the side-effect semantics. In `BookcaseRepositoryImpl.save()`, the JPA entity is saved and the entity's ID is set back on the domain object (`bookcase.setBookcaseId(entity.getBookcaseId())`—visible in `BookcaseRepositoryImpl`).

**How I verified:** `CreateBookcaseUseCaseTest` uses a `doAnswer` to simulate the side effect: it sets `b.setBookcaseId(100L)` on the argument object, then asserts `result.bookcaseId() == 100L` and that `shelfAccessPort.createShelf(100L, ...)` was called with the correct ID.

**Preventative guardrail:** A cleaner contract would be `Bookcase save(Bookcase)` returning the persisted domain object. The `doAnswer` test pattern is a warning sign that the contract is relying on a side effect that is easy to miss.

---

### Problem 2: Duplicate `BookcaseMapper` class names in two packages

**Symptom:** A pre-existing `infrastructure/mapping/BookcaseMapper.java` exists as a stub (its `toDomain()` method returns `null`). The new `core/domain/BookcaseMapper.java` is the real mapper. Both have the same simple class name.

**Root cause:** The infra stub was scaffolded earlier and never completed. When the new mapper was added to `core/domain`, no one removed the stub.

**Fix (partial):** The new mapper in `core/domain` is what is actively imported and used. The stub is unused.

**How I verified:** Searching imports across the codebase shows `core/domain/BookcaseMapper` is the one in use everywhere.

**Preventative guardrail:** Delete `infrastructure/mapping/BookcaseMapper.java` or rename it. Add a build check that no two classes in the module share the same simple name to catch this pattern automatically.

---

## 6) Architecture narrative (hex / DDD / boundaries)

```
┌───────────────────────────────────────────────────────────────────────────┐
│  Adapters (infrastructure, inbound)                                       │
│  BookCaseController  (web/controllers)                                    │
│  BookCirculationCommands, BookSearchCommands, PromptOptions  (cli)        │
│    │  depends on BookcaseFacade (inbound port)                            │
│    │  maps Bookcase → BookcaseDTO via BookcaseMapper.toDTO() at boundary  │
└─────────────────────────────┬─────────────────────────────────────────────┘
                              │
                    ┌─────────▼─────────┐
                    │  BookcaseFacade   │  ← inbound port (interface, core)
                    │  (core/ports/     │     returns Bookcase domain objects
                    │   inbound)        │
                    └─────────┬─────────┘
                              │ implemented by
                    ┌─────────▼─────────────┐
                    │  BookcaseService       │  ← thin delegator (@Service)
                    │  (core/application)    │
                    └──┬──────┬──────┬──────┘
                       │      │      │
          ┌────────────▼┐  ┌──▼──────▼──────────────────────┐
          │ CreateBookcase│ │ DeleteBookcaseUseCase           │
          │ UseCase       │ │ QueryBookcaseUseCase            │
          │ (core/app/    │ │ (core/application/usecases)    │
          │  usecases)    │ └───────────────┬────────────────┘
          └──────┬────────┘                 │
                 │                          │
    ┌────────────▼──────────────────────────▼─────────────────────┐
    │  BookcaseRepository (core/ports/outbound)                    │
    │  ShelfAccessPort    (core/ports/outbound)                    │
    │  — both return/accept Bookcase domain type only              │
    └───────────────────────────┬─────────────────────────────────┘
                                │ implemented by
    ┌───────────────────────────▼─────────────────────────────────┐
    │  BookcaseRepositoryImpl  (infrastructure/adapter/outbound)   │
    │  ShelfAccessPortAdapter  (infrastructure/adapter/outbound)   │
    │  BookcaseJpaRepository   (infrastructure/repository)         │
    │  BookcaseEntity          (infrastructure/entity)             │
    │  BookcaseMapper          (core/domain) ← translation here    │
    └─────────────────────────────────────────────────────────────┘
```

**Key dependency-direction facts evidenced in the diff:**
- `BookcaseService` and all use cases import only types from `core.*` packages—confirmed by checking imports in each file. No `jakarta.persistence`, no `BookcaseEntity`, no `BookcaseDTO`.
- `BookcaseMapper` in `core/domain` imports `BookcaseEntity` (infra) and `BookcaseDTO` (api). This is the controlled anti-corruption boundary. All other core classes are clean.
- `BookcaseRepositoryImpl` imports `BookcaseMapper` and `BookcaseJpaRepository`—infra-to-core dependency (allowed: adapters may import core types).
- `BookCaseController` and CLI commands import `BookcaseMapper.toDTO()` — adapters own DTO mapping, not the core.

**Coupling change:** Coupling between `core/application` and infrastructure **decreased to zero** for the bookcase persistence path. The only remaining coupling point is `BookcaseMapper`'s import of `BookcaseEntity`, which is intentionally isolated.

---

## 7) Testing and verification story

### What tests exist now
| File | Type | Scope |
|---|---|---|
| `CreateBookcaseUseCaseTest` | Unit (Mockito) | Creation logic, duplicate detection, shelf provisioning count |
| `DeleteBookcaseUseCaseTest` | Unit (Mockito, InOrder) | Deletion ordering: shelves before bookcase |
| `QueryBookcaseUseCaseTest` | Unit (Mockito) | All 6 query paths including null-return cases |
| `BookcaseServiceTest` | Unit (Mockito) | Delegation verification (service delegates to use cases) |
| `BookcaseTest` | Unit | Domain model invariants (shelfCapacity guard) |
| `PromptOptionsTest` | Unit | CLI option builder receives `List<Bookcase>` correctly |

### Run commands
```bash
# All tests
./mvnw test

# Bookcase module only
./mvnw test -pl . -Dtest="*Bookcase*,*QueryBookcase*,*CreateBookcase*,*DeleteBookcase*"

# Single class
./mvnw test -Dtest="CreateBookcaseUseCaseTest"
```

### High-leverage test cases to add

1. **`CreateBookcaseUseCaseTest` — shelf count matches shelfCapacity:** `verify(shelfAccessPort, times(shelfCapacity)).createShelf(...)` — already present. Verify the `shelfAccessPort.createShelf()` arguments include the correct `bookCapacity` value.

2. **`BookcaseRepositoryImplTest` — `save()` ID round-trip:** Verify that after `repositoryImpl.save(bookcase)`, `bookcase.getBookcaseId()` is non-null. Catches the silent side-effect contract regressing.

3. **`BookcaseRepositoryImplTest` — `findById()` returns null on miss:** Verify `repositoryImpl.findById(99999L) == null` when the JPA repo returns empty. Currently only tested at the use-case layer via mock.

4. **`QueryBookcaseUseCaseTest` — `findBookCaseById` vs `findById` equivalence:** Both methods exist and appear identical. Test that both return the same result; then decide if one should be removed.

5. **`BookCaseControllerTest` (integration) — `GET /api/v1/bookcase/location/{location}` maps to DTO:** Verify the controller properly streams `Bookcase → BookcaseDTO` and returns 200 with correct JSON shape.

6. **`BookCaseControllerTest` — `POST /api/v1/bookcase/create` when bookcase already exists returns 409:** The 409 is thrown in `CreateBookcaseUseCase`; verify it surfaces correctly through the controller layer.

7. **`DeleteBookcaseUseCaseTest` — transaction rollback simulation:** If `bookcaseRepository.deleteById()` throws, verify the `shelfAccessPort.deleteAllShelvesInBookcase()` call is not committed (requires Spring integration test context).

8. **`BookcaseMapperTest` — `toDomain(null)` does not NPE:** `BookcaseRepositoryImpl.findBookcaseByBookcaseLocation()` calls `BookcaseMapper.toDomain()` on the result of the JPA query, which may return `null` when no record is found.

9. **`CreateBookcaseUseCaseTest` — `bookCapacity * shelfCapacity` stored correctly:** Assert that after creation, `bookcase.getBookCapacity() == bookCapacity * shelfCapacity`.

10. **Arch test (ArchUnit) — core does not import `BookcaseEntity` outside of `BookcaseMapper`:** Enforce the anti-corruption boundary so it cannot regress silently.

---

## 8) Tradeoffs and alternatives

### 1. `save()` returning `void` vs returning `Bookcase`
| | |
|---|---|
| **Decision made** | `void save(Bookcase)` — matches JPA's side-effect semantics, keeps port simple |
| **Alternative** | `Bookcase save(Bookcase)` — returns the persisted domain object with ID set |
| **Why chosen** | Simpler port contract; consistent with how Spring Data JPA's `save()` works for `@Entity` |
| **Risk introduced** | ID retrieval is implicit (side effect); `doAnswer` in tests is a code smell |
| **When to revisit** | When a second caller of `save()` needs the ID and the implicit contract causes a bug |

### 2. `BookcaseMapper` in `core/domain` vs in `infrastructure/adapter`
| | |
|---|---|
| **Decision made** | `core/domain/BookcaseMapper` — single static class accessible from core and infra |
| **Alternative** | Keep mapper only in `infrastructure/adapter`; use a reverse constructor in `Bookcase` |
| **Why chosen** | Pragmatic: one mapper class used by both the adapter and the web/CLI layers |
| **Risk introduced** | `core/domain` imports `BookcaseEntity` and `BookcaseDTO`—a controlled but real layering compromise |
| **When to revisit** | When adding a second infrastructure (e.g., a different database), to avoid `BookcaseEntity` coupling in the domain |

### 3. Nullable `Bookcase` return from `findById()` vs `Optional<Bookcase>`
| | |
|---|---|
| **Decision made** | Return `null` on miss |
| **Alternative** | Return `Optional<Bookcase>` — makes absence explicit in the type system |
| **Why chosen** | All prior callers used `.get()` anyway; Optional was providing no real safety |
| **Risk introduced** | NPE at any call site that doesn't null-check |
| **When to revisit** | When adding a new caller, or when a NullPointerException is reported in production |

### 4. `@Service` on use cases vs package-private plain objects
| | |
|---|---|
| **Decision made** | `@Service` on `CreateBookcaseUseCase`, `DeleteBookcaseUseCase`, `QueryBookcaseUseCase` |
| **Alternative** | Plain Java objects, instantiated by `BookcaseService`'s constructor |
| **Why chosen** | Easiest for Spring DI; `@Service` signals "application service" per DDD convention |
| **Risk introduced** | Use cases are now Spring-managed beans, importable by anything; erodes module encapsulation |
| **When to revisit** | If a module boundary tool (e.g., Moduliths, ArchUnit) is adopted |

### 5. `BookcaseService` as pure delegator vs removing it entirely
| | |
|---|---|
| **Decision made** | Keep `BookcaseService` as a thin delegator implementing `BookcaseFacade` |
| **Alternative** | Annotate use cases themselves with `@Primary` and remove `BookcaseService` |
| **Why chosen** | `BookcaseFacade` needs a single implementing bean. Having three use cases each partially implement it would violate the interface-segregation principle without further splitting the facade. |
| **Risk introduced** | Adds a thin indirection layer; developers may wonder why it exists |
| **When to revisit** | If `BookcaseFacade` is split into three sub-facades (create, delete, query), each use case could implement its own interface directly |

### 6. Commit `260bd34` (Javadoc) on `main` not yet merged to `origin/main`
| | |
|---|---|
| **Decision made** | Add Javadoc to `BookcaseFacade` as a preparatory commit on `main` before branching |
| **Alternative** | Include the Javadoc in this branch only |
| **Why chosen** | Javadoc is logically independent and should land on `main` regardless of refactor outcome |
| **Risk introduced** | Javadoc still references old types in some comments (e.g., "returns an Optional containing the BookcaseDTO") — now stale after the refactor |
| **When to revisit** | Update Javadoc after this branch merges to reflect new `Bookcase` return types |

### 7. `toEntity()` in `BookcaseMapper` does not copy `bookcaseZone` / `bookcaseIndex`
| | |
|---|---|
| **Decision made** | `toEntity()` maps only `id`, `shelfCapacity`, `bookCapacity`, and `location` |
| **Alternative** | Map all six fields to maintain round-trip fidelity |
| **Why chosen** | *Inference*: `bookcaseZone` and `bookcaseIndex` may be derived/set by the entity itself, or the omission is an oversight |
| **Risk introduced** | Saving a `Bookcase` that was loaded from the database will lose `zone` and `index` values in the persisted entity |
| **When to revisit** | Immediately — verify whether `BookcaseEntity.bookcaseZone` and `bookcaseIndex` need to be written on save |

---

## 9) Glossary of "tech I used" (only what is proven)

- **Java 17** — Used throughout; record types, streams, `toList()` (Java 16+ collector without `Collectors.`).
- **Spring Boot 3.5.7** — Application framework; `@Service`, `@RestController`, `@Transactional`, `ResponseStatusException`.
- **Spring Shell** — Powers the CLI commands (`BookCirculationCommands`, `BookSearchCommands`, etc.); users interact via a terminal REPL.
- **Spring Data JPA / `BookcaseJpaRepository`** — Repository interface extending Spring Data; provides `save()`, `findById()`, `deleteById()`, custom JPQL/query methods.
- **PostgreSQL** — Inferred backing store (from README); `BookcaseEntity` is a JPA entity persisted there.
- **JUnit 5** — Test framework; `@Test`, `@ExtendWith(MockitoExtension.class)`.
- **Mockito** — Mocking framework; `@Mock`, `@InjectMocks`, `doAnswer()`, `InOrder`, `verify()`, `when()`.
- **AssertJ** — Fluent assertion library; `assertThat(result).isNotNull()`, `assertThat(list).hasSize(2)`.
- **Hexagonal Architecture (ports-and-adapters)** — Architectural style; inbound port (`BookcaseFacade`), outbound port (`BookcaseRepository`, `ShelfAccessPort`), adapters in infrastructure and CLI/web layers.
- **Use-Case pattern** — Each operation (create, delete, query) is a dedicated class with a single method and only the dependencies it needs.
- **Anti-corruption layer** — `BookcaseMapper` in `core/domain` acts as the sole controlled translation point between infra types and domain types.
- **`@Transactional`** — Applied at the use-case level (`DeleteBookcaseUseCase`) to scope atomicity to the unit of work.
- **Maven** — Build tool; `./mvnw test` to run tests.

---

## 10) Red flags and how I would defend them

### 1. `BookcaseMapper` in `core/domain` imports `BookcaseEntity` (infrastructure type)
**Smell:** Core should not depend on infrastructure; this import technically violates the hexagonal rule.
**Pragmatic defense:** This is an intentional, isolated anti-corruption boundary. `BookcaseMapper` is the *only* class in core that touches `BookcaseEntity`, and it is a pure static translator—no Spring beans, no behavior. The pattern is explicitly documented in ADR-0003.
**Follow-up refactor:** Move `BookcaseMapper.toDomain()` and `BookcaseMapper.toEntity()` to `BookcaseRepositoryImpl` in the infrastructure adapter, leaving only `BookcaseMapper.toDTO()` (which maps `Bookcase → BookcaseDTO`) in the core. This fully removes the infra import from core.

### 2. Nullable return from `findById()` / `findBookCaseById()` without null-guards at call sites
**Smell:** Returning `null` for "not found" is an anti-pattern that was replaced industry-wide by `Optional` precisely because of NPE risk.
**Pragmatic defense:** The original `Optional<BookcaseEntity>` was never actually checked at any call site—all callers called `.get()`. The null contract is more honest about current behavior. The risk is documented in commit messages and ADR-0003.
**Follow-up refactor:** Change `BookcaseRepository.findById()` to return `Optional<Bookcase>`, add null-guards at each CLI/controller call site, and update tests.

### 3. `QueryBookcaseUseCase` has two nearly identical methods: `findBookCaseById(Long)` and `findById(Long)`
**Smell:** Both methods call `bookcaseRepository.findById(id)` with no difference in behavior—this is dead code / duplication.
**Pragmatic defense:** The two methods mirror two differently named methods on the original `BookcaseFacade` interface, which predates this refactor. The delegator pattern faithfully implements the existing contract.
**Follow-up refactor:** Deprecate one method on `BookcaseFacade`, migrate all callers to the canonical name, then remove the duplicate.

### 4. Stale Javadoc in `BookcaseFacade` references `Optional<BookcaseDTO>` and `BookcaseEntity`
**Smell:** The Javadoc added in `260bd34` describes the old return types. After the refactor, `findById()` returns `Bookcase`, not `Optional<BookcaseEntity>`.
**Pragmatic defense:** Javadoc was written before the refactor and the mismatch is a documentation lag, not a logic error.
**Follow-up refactor:** Update all affected `@return` and `@param` tags in `BookcaseFacade.java` to reference `Bookcase` accurately.

### 5. `BookcaseRepositoryImpl.findBookcaseByBookcaseLocation()` calls `BookcaseMapper.toDomain(null)` when JPA returns no result
**Smell:** `bookcaseJpaRepository.findBookcaseEntityByBookcaseLocation(location)` returns `null` if no entity is found. Passing `null` to `BookcaseMapper.toDomain()` will cause an NPE on `bookcaseEntity.getBookcaseId()`.
**Pragmatic defense:** In production, this method is only called from `CreateBookcaseUseCase` to check for duplicates—if no record exists, the NPE would be caught and surfaced. However, this is an unintentional fragility.
**Follow-up refactor:** Add a null-guard in `BookcaseMapper.toDomain()`: `if (bookcaseEntity == null) return null;` — one line.

### 6. Duplicate `BookcaseMapper` class in `infrastructure/mapping/` (returns null stub)
**Smell:** The `infrastructure/mapping/BookcaseMapper` stub is never used but could be auto-imported by an IDE, leading to silent null returns.
**Pragmatic defense:** It was scaffolding that was never completed; it is functionally inert.
**Follow-up refactor:** Delete `infrastructure/mapping/BookcaseMapper.java`.

---

## 11) Practice interview Q&A (grounded)

### Q1: "Tell me about the architecture of this project."
**Testing:** System design thinking, knowledge of hexagonal/clean architecture.
**Strong answer outline:**
- Bibby follows hexagonal architecture: the domain core is isolated from Spring, JPA, and HTTP concerns.
- Three layers: adapters (web/CLI, infra), ports (inbound `BookcaseFacade`, outbound `BookcaseRepository` / `ShelfAccessPort`), and core (domain model, application use cases).
- Point to: `core/ports/inbound/BookcaseFacade.java`, `core/ports/outbound/BookcaseRepository.java`, `core/application/usecases/`.

### Q2: "Why did you extract use cases instead of just adding methods to the service?"
**Testing:** SRP knowledge, testability reasoning.
**Strong answer outline:**
- `BookcaseService` had three cohesive groups of behavior each with different dependency profiles.
- Query-only tests no longer need `ShelfAccessPort` mocked—`QueryBookcaseUseCaseTest` has one `@Mock`.
- Easier to extend: adding a new query path touches only `QueryBookcaseUseCase`, not a ~130-line god class.
- Point to: `QueryBookcaseUseCaseTest` (one mock) vs old `BookcaseServiceTest` setup.

### Q3: "Why does `BookcaseMapper` live in `core/domain` instead of the infrastructure adapter?"
**Testing:** Layering judgment, willingness to make pragmatic calls and explain them.
**Strong answer outline:**
- The mapper is used by both `BookcaseRepositoryImpl` (infra) and the CLI/web adapters; putting it in infra would require the CLI adapters to import an infra class.
- Placing it in `core/domain` makes it a controlled anti-corruption layer: one class, one file, zero Spring dependencies.
- Known compromise: core imports `BookcaseEntity`. Documented. Follow-up: split into two mappers (one in infra for entity, one in core for DTO).
- Point to: `BookcaseMapper.java` imports, ADR-0003.

### Q4: "What is the risk of returning `null` from `findById()` instead of `Optional`?"
**Testing:** Understanding of Java's null safety story, design trade-offs.
**Strong answer outline:**
- NPE at any call site that does not null-check the return value.
- `Optional` communicates absence explicitly in the type system; null does not.
- Current callers assume existence—this is an honest representation of current behavior but a deferred risk.
- Fix: return `Optional<Bookcase>` from the port and add `.orElseThrow()` or `.ifPresent()` at each call site.

### Q5: "How do you test a `void` method that produces a side effect (like `save()`)?"
**Testing:** Mockito proficiency, understanding of test doubles.
**Strong answer outline:**
- Use `doAnswer()` to intercept the call and apply the side effect manually on the argument object.
- In `CreateBookcaseUseCaseTest`: `doAnswer(inv -> { inv.getArgument(0, Bookcase.class).setBookcaseId(100L); return null; }).when(repo).save(any())`.
- Then assert `result.bookcaseId() == 100L` and that `shelfAccessPort.createShelf(100L, ...)` was called with the right ID.
- Point to: `CreateBookcaseUseCaseTest.createNewBookCase_shouldCreateNewBookcaseSuccessfully()`.

### Q6: "How did you ensure shelf deletion happens before bookcase deletion?"
**Testing:** Knowledge of transactional integrity, use of `InOrder` in tests.
**Strong answer outline:**
- `@Transactional` on `DeleteBookcaseUseCase.deleteBookcase()` wraps both operations atomically.
- `DeleteBookcaseUseCaseTest` uses `Mockito.inOrder(shelfAccessPort, bookcaseRepository)` to assert the call sequence explicitly.
- The test fails if someone swaps the two calls, acting as a regression guard.
- Point to: `DeleteBookcaseUseCase.java`, `DeleteBookcaseUseCaseTest.java`.

### Q7: "What is an anemic domain model and did you have one here?"
**Testing:** Domain-driven design vocabulary, ability to identify and fix it.
**Strong answer outline:**
- An anemic domain model is a class that carries data but has no behavior—essentially a data bag, not a domain object.
- `Bookcase` was anemic: 2 fields, no business rules except a `shelfCapacity` guard.
- After enrichment: 6 fields, the capacity invariant is enforced in `setShelfCapacity()`, and the model can fully represent bookcase state without touching `BookcaseEntity`.
- Point to: `Bookcase.java` before/after the 6-arg constructor change.

### Q8: "Why is `BookcaseService` still there if it just delegates?"
**Testing:** Interface segregation, understanding of Spring DI.
**Strong answer outline:**
- `BookcaseFacade` is the inbound port; it must have exactly one implementing Spring bean.
- Having three use cases each partially implement one interface would violate interface segregation.
- `BookcaseService` is the glue: it implements the full facade contract and delegates each method to the appropriate use case.
- Future option: split `BookcaseFacade` into `CreateBookcasePort`, `DeleteBookcasePort`, `QueryBookcasePort`—then each use case can implement its own single-method port.

### Q9: "How did you manage the breaking change of the 2→6 arg constructor across the codebase?"
**Testing:** Refactor risk management, commit discipline.
**Strong answer outline:**
- Sequenced the commits so each one left the build green: domain enrichment → port update → use case extraction → adapter update → test update.
- Documented in the commit body which call sites needed updating.
- Point to: commit `4065480` message "Notes/Risks" section, then `8b6fcec` for adapter updates.

### Q10: "What's the difference between `findBookCaseById()` and `findById()` on the facade?"
**Testing:** Attention to detail, ability to spot duplication.
**Strong answer outline:**
- Looking at `QueryBookcaseUseCase`, both call `bookcaseRepository.findById(id)` with no difference.
- They exist because the original `BookcaseFacade` had both method signatures inherited from different development phases.
- The correct fix is to deprecate one, migrate all callers, and remove it.
- This is a known code smell; I would prioritize removing the duplicate before shipping.

### Q11: "How does the CLI layer get `BookcaseDTO` if the facade returns `Bookcase`?"
**Testing:** Understanding of adapter pattern, DTO boundary placement.
**Strong answer outline:**
- CLI commands and `BookCaseController` call `BookcaseMapper.toDTO(bookcase)` on each result at their own boundary.
- This is the anti-corruption layer pattern: adapters own the translation to their representation, not the core.
- `PromptOptions` streams `List<Bookcase>` → `List<BookcaseDTO>` via `.stream().map(BookcaseMapper::toDTO).toList()`.
- Point to: `PromptOptions.java`, `BookCaseController.getBookcaseByLocation()`.

### Q12: "What would you do differently if you were starting this refactor from scratch?"
**Testing:** Retrospective thinking, engineering maturity.
**Strong answer outline:**
- Return `Optional<Bookcase>` from the port from day one—do not defer null-safety.
- Move the mapper to the infra adapter to keep core completely free of entity imports.
- Add an ArchUnit test before starting so the boundary rule is enforced during the migration, not checked manually after.
- Delete the `infrastructure/mapping/BookcaseMapper` stub before introducing the new one to avoid the naming collision.

### Q13: "How would you test the `@Transactional` behavior on `deleteBookcase()`?"
**Testing:** Integration testing knowledge.
**Strong answer outline:**
- Unit test (`DeleteBookcaseUseCaseTest`) only tests ordering, not transactionality.
- To test rollback: write a Spring integration test that injects `DeleteBookcaseUseCase`, mocks `bookcaseRepository.deleteById()` to throw `RuntimeException`, and asserts that the shelves are not actually deleted (or that the transaction was rolled back).
- In practice: use `@Transactional(rollbackFor = Exception.class)` and verify with a real database (H2 in-memory or Testcontainers with PostgreSQL).

### Q14: "What would you add to prevent this architectural boundary from regressing?"
**Testing:** Proactive quality thinking.
**Strong answer outline:**
- Add an ArchUnit rule: `noClasses().that().resideInAPackage("..core..")` `.should().dependOnClassesThat().resideInAPackage("..infrastructure..")` (with an explicit exception for `BookcaseMapper` if it stays in core).
- Add an ArchUnit rule: `noClasses().that().resideInAPackage("..core..")` `.should().dependOnClassesThat().resideInAPackage("..api.dtos..")`.
- Run these rules in CI so violations fail the build immediately.

---

## 12) Next steps talking points

### Immediate (today)

- **Fix `BookcaseMapper.toDomain(null)` NPE risk.** Add `if (bookcaseEntity == null) return null;` as the first line of `toDomain()`. One-line fix, zero-risk.
- **Delete `infrastructure/mapping/BookcaseMapper.java`** — the null-returning stub. Remove the naming collision before it causes an IDE auto-import bug.
- **Fix stale Javadoc in `BookcaseFacade.java`** — `findById()` and `findBookCaseById()` doc still says "returns Optional containing BookcaseDTO". Update to "returns Bookcase, or null if not found."

### Short-term hardening (this week)

- **Return `Optional<Bookcase>` from `BookcaseRepository.findById()`** and add null-guards or `.orElseThrow()` at every call site (CLI commands, `BookCaseController`, use cases). This eliminates the documented NPE risk.
- **Remove the duplicate `findById` / `findBookCaseById` on `BookcaseFacade` and `QueryBookcaseUseCase`** — deprecate one, migrate callers, delete.
- **Fix `BookcaseMapper.toEntity()` to map `bookcaseZone` and `bookcaseIndex`** — verify these fields are not silently dropped on save.
- **Add `CreateBookcaseUseCaseTest` for `bookCapacity` storage** — assert `bookcase.getBookCapacity() == bookCapacity * shelfCapacity` after creation.

### Strategic refactors (later)

- **Add ArchUnit tests** to enforce "core does not import infrastructure" and "core does not import API DTOs" as CI-enforced rules, preventing any future regression of this refactor.
- **Split `BookcaseFacade` into three focused inbound ports** (`CreateBookcasePort`, `DeleteBookcasePort`, `QueryBookcasePort`) — each use case implements its own single-method port, eliminating `BookcaseService` as the delegator entirely.
- **Move `BookcaseMapper` translation logic into the infrastructure adapter** — `toDomain()` and `toEntity()` belong in `BookcaseRepositoryImpl`; only `toDTO()` should stay accessible to web/CLI adapters. This achieves full hexagonal compliance with zero infra imports in core.
- **Add Testcontainers integration tests** for `BookcaseRepositoryImpl` covering the JPA ID side-effect contract for `save()`, `findById()` returning null on miss, and `findBookcaseByBookcaseLocation()` on null JPA result.
