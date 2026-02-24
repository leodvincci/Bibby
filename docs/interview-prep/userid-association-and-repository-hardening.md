# Interview Prep Packet: UserId Association, Repository Hardening & Save-Return Contract

Date: 2026-02-24 (America/Chicago)
Branch: `refactor/bookcase-domain-model-and-use-cases`
Range: `ea561f8..HEAD` (commits after PR #317 merge, pushed as continuation of same branch)
Commits:
- `a326ac8` — Add userId field to Bookcase domain model
- `83e1fd7` — Return saved Bookcase from repository and update use case
- `79404a9` — Update tests for userId parameter and save return type
- `0c2c2ff` — Document refactor of Bookcase domain model and use-case extraction

---

## 1) 30-second pitch (recruiter-friendly)

Bibby is a Spring Boot personal library manager where users organize physical books into bookcases and shelves. I extended the Bookcase domain model to carry user ownership (`userId`), changed the repository's `save()` method from fire-and-forget (`void`) to a return-the-persisted-entity pattern, and fixed a null-pointer bug in location lookups. The change touched 11 Java files across domain, ports, infrastructure, and 5 test classes--all 141 tests pass. It sets up multi-user support and eliminates a class of ID-writeback bugs.

---

## 2) 2-minute project walkthrough script (hiring manager)

> **Context:**
> Bibby follows hexagonal architecture. The Bookcase module manages physical furniture that holds shelves, which hold books. In the previous refactor, I decomposed a god-class `BookcaseService` into focused use cases (`CreateBookcaseUseCase`, `QueryBookcaseUseCase`, `DeleteBookcaseUseCase`) and purged JPA entity types from port contracts. That left two gaps.

> **Problem:**
> First, the `Bookcase` domain model had no concept of which user owned it. The `userId` existed only on `BookcaseEntity` in the infrastructure layer, meaning any multi-user query would need to reach through the domain boundary into JPA-land. Second, the `BookcaseRepository.save()` port returned `void`. The use case was relying on JPA's side-effect of mutating the passed object's ID field after persist--a hidden contract that only worked because the domain object and entity happened to share the same reference. That's fragile: if the mapper ever copies instead of mutating, the ID writeback silently breaks. Third, `findBookcaseByBookcaseLocation()` would throw a `NullPointerException` when no matching bookcase existed, because it unconditionally called `BookcaseMapper.toDomain()` on a null entity.

> **Approach:**
> I split this into three commits to keep each reviewable. First, I added `userId` to `Bookcase`'s constructor and updated the mapper to carry it through `toDomain()` and `toEntity()`. Second, I changed the port contract: `save()` now returns `Bookcase`, the adapter maps the JPA-saved entity back to domain, and the use case assigns the result. I also wrapped the location lookup in a null-check. Third, I updated all 5 test files--adding the `userId` constructor argument everywhere and replacing the old `doAnswer` ID-mutation mocks with clean `when(...).thenReturn(savedBookcase)` stubs that match the new return-type contract.

> **Result:**
> 141 tests pass. The domain model now fully owns user association. The save contract is explicit--no hidden side-effects. And the location lookup is null-safe. This unblocks the next feature: querying "all bookcases for user X" purely through the domain layer.

---

## 3) Deep dive menu (pick 1-2 in interviews)

### 1. Save-return contract: from void to Bookcase
**Why it's interesting:** Demonstrates understanding of hidden coupling, side-effect contracts, and why explicit return types are safer than mutation.
**Files:** `BookcaseRepository.java` (port), `BookcaseRepositoryImpl.java` (adapter), `CreateBookcaseUseCase.java`
**Talking points:**
- The old `void save(Bookcase)` relied on JPA's merge behavior mutating the passed object's `bookcaseId` in-place. That only works when the same object reference flows from use case through mapper to JPA and back. If the mapper ever creates a copy (which `toEntity()` does), the ID never arrives.
- The fix: `Bookcase save(Bookcase)` -- the adapter calls `BookcaseMapper.toEntity()`, saves via JPA, then maps the returned entity back to domain via `BookcaseMapper.toDomain()`, which carries the generated ID.
- The test change is equally telling: `doAnswer` with side-effect mutation was replaced by a simple `when(...).thenReturn(savedBookcase)`. Simpler mocks = the contract is better.

### 2. Null-safety in repository adapters
**Why it's interesting:** Classic infrastructure-boundary bug -- shows you think about what happens when the database returns nothing.
**Files:** `BookcaseRepositoryImpl.findBookcaseByBookcaseLocation()`
**Talking points:**
- Before: `BookcaseMapper.toDomain(jpaRepo.findBy...())` -- if JPA returns null, `toDomain()` throws NPE.
- After: explicit null-check before mapping, with a `logger.warn()` for observability.
- This is the adapter's responsibility, not the domain's. The port contract says "return null if not found," so the adapter must enforce that.

### 3. Domain model enrichment: adding userId
**Why it's interesting:** Shows you think about where ownership belongs in DDD -- on the aggregate, not buried in infrastructure.
**Files:** `Bookcase.java` (domain model), `BookcaseMapper.java`, `BookcaseEntity.java`
**Talking points:**
- `userId` previously only existed on `BookcaseEntity`. Any code that needed user filtering had to either (a) query through JPA directly or (b) accept entity types in the domain layer -- both violate hexagonal boundaries.
- Adding it to the domain constructor forces every caller to supply it, making the association impossible to forget.
- The mapper was updated in both directions: `toDomain()` carries `userId` from entity, `toEntity()` carries it back. Also fixed: `toEntity()` was missing `bookcaseZone` and `bookcaseIndex` mappings.

### 4. Breaking constructor change and blast radius management
**Why it's interesting:** Shows you can make a breaking API change safely across a codebase.
**Files:** All 5 test files, `CreateBookcaseUseCase.java`, `BookcaseMapper.java`
**Talking points:**
- Adding a required constructor parameter to `Bookcase` is an intentionally breaking change -- the compiler finds every call site.
- The commit was split so commit 1 (model) compiles but tests fail, commit 2 (use case + port) adds the runtime logic, commit 3 (tests) restores green. In a team setting, these could be squashed, but the split shows disciplined reasoning.
- 30+ constructor call sites were updated across 5 test files and 2 production files.

### 5. Mapper completeness: toEntity was dropping fields
**Why it's interesting:** A subtle data-loss bug caught during this refactor.
**Files:** `BookcaseMapper.toEntity()`
**Talking points:**
- Before: `toEntity()` set `bookcaseId`, `shelfCapacity`, `bookCapacity`, `bookcaseLocation` -- but NOT `bookcaseZone` or `bookcaseIndex`. Those fields were silently null in persisted entities.
- After: all six fields are mapped, plus `userId`. Also: `bookcaseId` was removed from `toEntity()` since JPA generates it -- setting it manually could cause merge/persist confusion.
- The old `toEntity()` was also setting `bookcaseId` on the entity, which is wrong for new records -- JPA should generate IDs, not the mapper.

### 6. Test mock strategy: doAnswer vs when/thenReturn
**Why it's interesting:** Shows understanding of test design and how test smells mirror production smells.
**Files:** `CreateBookcaseUseCaseTest.java`
**Talking points:**
- The old test used `doAnswer` to mutate the `Bookcase` argument's ID field inside the mock. That's a test smell -- it mirrors the production code's reliance on side-effect mutation.
- The new test creates a `savedBookcase` with ID=100L and uses `when(repo.save(any())).thenReturn(savedBookcase)`. The mock is now pure and declarative.
- Rule of thumb: if your mock needs `doAnswer`, ask whether the production contract is too implicit.

---

## 4) STAR stories (behavioral interview ammo)

### Story 1: "I found a hidden ID-writeback bug and redesigned the save contract"
**Seniority signal:** architecture, ownership
**Difficulty:** medium

- **Situation:** The Bookcase module's `CreateBookcaseUseCase` needed to return the newly created bookcase's ID to the caller. The repository `save()` method returned `void`, and the code relied on JPA silently mutating the domain object's ID field after persist.
- **Task:** I needed to make the ID available reliably after save, especially since the mapper creates a *new* entity object (breaking the reference chain that side-effect mutation depends on).
- **Actions:**
  - Traced the data flow: `Bookcase` -> `BookcaseMapper.toEntity()` -> new `BookcaseEntity` -> JPA `save()` -> entity gets ID -> but the original `Bookcase` object never receives it.
  - Changed the `BookcaseRepository` port interface from `void save(Bookcase)` to `Bookcase save(Bookcase)`.
  - Updated `BookcaseRepositoryImpl` to map the JPA-returned entity back to domain, carrying the generated ID.
  - Updated `CreateBookcaseUseCase` to use the returned object: `bookcase = bookcaseRepository.save(bookcase)`.
  - Simplified the test mocks from `doAnswer` side-effect stubs to clean `when/thenReturn`.
- **Result:** All 141 tests pass. The ID is now carried explicitly through the return value -- no hidden side-effects. The test mocks became simpler, confirming the design improvement.
- **What I would improve next time:**
  - Introduce a `BookcaseId` value object to make the ID's lifecycle (null before persist, non-null after) type-safe.
  - Add an integration test that actually hits H2 to verify the full save-and-return round-trip.

### Story 2: "I added user ownership to a domain model without breaking production"
**Seniority signal:** ownership, quality
**Difficulty:** medium

- **Situation:** The Bookcase domain model had no concept of user ownership. `userId` existed only on the JPA entity, meaning multi-user features would require violating hexagonal boundaries.
- **Task:** Add `userId` to the domain model and update all layers and tests without leaving the build broken at any point.
- **Actions:**
  - Added `userId` to `Bookcase`'s constructor as a required parameter, forcing the compiler to find all 30+ call sites.
  - Updated `BookcaseMapper` in both directions to carry `userId` through persistence.
  - Discovered that `toEntity()` was also missing `bookcaseZone` and `bookcaseIndex` mappings -- fixed those.
  - Updated 5 test files, touching every `Bookcase` constructor call.
  - Ran the full 141-test suite to confirm green.
- **Result:** The domain model now owns user association. The mapper is complete. The fix for the missing zone/index fields prevents silent data loss that would have been hard to trace in production.
- **What I would improve next time:**
  - Use a builder or factory method on `Bookcase` to avoid the growing constructor parameter list (now 7 params).

### Story 3: "I caught a NullPointerException at the infrastructure boundary"
**Seniority signal:** debugging, quality
**Difficulty:** low

- **Situation:** `BookcaseRepositoryImpl.findBookcaseByBookcaseLocation()` unconditionally called `BookcaseMapper.toDomain()` on the JPA result. If no bookcase existed at that location, the JPA method returns null, and `toDomain(null)` throws NPE.
- **Task:** Make the adapter null-safe while keeping the port contract clean.
- **Actions:**
  - Added a null-check before calling the mapper.
  - Added a `logger.warn()` for the not-found case to aid debugging in production.
  - Confirmed the use case (`CreateBookcaseUseCase`) already checks for `null` return, so the contract is consistent.
- **Result:** The location lookup no longer throws NPE for new locations. The log message provides observability for operations teams.
- **What I would improve next time:**
  - Consider using `Optional<Bookcase>` as the return type to make null-safety compile-time enforced.

### Story 4: "I simplified test mocks by improving the production contract"
**Seniority signal:** quality, architecture
**Difficulty:** low

- **Situation:** `CreateBookcaseUseCaseTest` used `doAnswer` with argument mutation to simulate JPA's ID writeback -- a 7-line mock block that was hard to read and tightly coupled to implementation details.
- **Task:** Update the test to match the new `save()` return-type contract.
- **Actions:**
  - Created a pre-built `savedBookcase` object with `bookcaseId=100L` and all fields populated.
  - Replaced the `doAnswer` block with a single `when(repo.save(any())).thenReturn(savedBookcase)`.
  - Verified that the test still asserts the correct ID (100L) on the `CreateBookcaseResult`.
- **Result:** The mock is 1 line instead of 7. It's declarative, not imperative. The test now documents the contract: "save returns a fully hydrated domain object."
- **What I would improve next time:**
  - Extract a test fixture factory for `Bookcase` objects to reduce constructor boilerplate across test classes.

---

## 5) Technical problem(s) I ran into + how I solved them

### Problem 1: ID never reaching the use case after save
- **Symptom:** After refactoring the mapper to create a *new* `BookcaseEntity` in `toEntity()`, the `Bookcase` object passed to `save()` would never get its `bookcaseId` populated. The `CreateBookcaseResult` would contain `null` for the ID.
- **Root cause:** `save()` returned `void`. The JPA `save()` mutates the *entity* object's ID, but since `toEntity()` creates a new entity, the original `Bookcase` domain object is disconnected. The ID mutation never propagates back.
- **Fix:** Changed `BookcaseRepository.save()` to return `Bookcase`. The adapter maps the JPA-returned entity (which has the generated ID) back to a domain object. The use case assigns: `bookcase = bookcaseRepository.save(bookcase)`.
- **How I verified:** Updated `CreateBookcaseUseCaseTest` -- the mock returns a `Bookcase` with `id=100L`, and the assertion confirms `result.bookcaseId() == 100L`. All 141 tests pass.
- **Preventative guardrail:** The return type makes the contract explicit. Any future caller *must* use the returned object to get the ID. The compiler enforces this -- you can't ignore a `Bookcase` return the way you can ignore a side-effect on a `void` method.

### Problem 2: Mapper silently dropping fields during toEntity
- **Symptom:** (Discovered during review, not a runtime failure yet.) `BookcaseMapper.toEntity()` was not setting `bookcaseZone` or `bookcaseIndex` on the entity, meaning those fields would persist as `null` even when the domain object had values.
- **Root cause:** The original `toEntity()` was written when `Bookcase` only had 3 fields. When zone and index were added to the domain model in the earlier refactor, `toEntity()` was not updated to include them.
- **Fix:** Added `bookcaseEntity.setBookcaseZone(bookcase.getBookcaseZone())` and `bookcaseEntity.setBookcaseIndex(bookcase.getBookcaseIndex())` to `toEntity()`.
- **How I verified:** The `CreateBookcaseUseCaseTest` creates a `Bookcase` with zone="A" and index="1", saves it, and verifies the returned object carries those values.
- **Preventative guardrail:** A mapper-specific unit test that round-trips `Bookcase -> Entity -> Bookcase` and asserts field equality would catch this class of bug. That test doesn't exist yet -- it's a recommended follow-up.

### Problem 3: NPE in findBookcaseByBookcaseLocation
- **Symptom:** When creating a bookcase with a new location, the "check for existing" call to `findBookcaseByBookcaseLocation()` would NPE if no entity existed.
- **Root cause:** `BookcaseMapper.toDomain(null)` -- the adapter passed the JPA result directly to the mapper without checking for null.
- **Fix:** Added `if (jpaRepo.find...() != null)` guard in the adapter. Returns `null` (per the port contract) with a warning log when not found.
- **How I verified:** The `CreateBookcaseUseCaseTest` mocks `findBookcaseByBookcaseLocation("BC001")` to return `null`, then asserts the use case proceeds to save. If the NPE were still present, the test would throw.
- **Preventative guardrail:** Consider changing the port return type to `Optional<Bookcase>` to make null-handling compile-time enforced.

---

## 6) Architecture narrative (hex / DDD / boundaries)

The Bookcase module follows hexagonal (ports-and-adapters) architecture:

```
CLI Commands / REST Controller
        |
        v
  [Inbound Port: BookcaseFacade]
        |
        v
  [Application: CreateBookcaseUseCase, QueryBookcaseUseCase, DeleteBookcaseUseCase]
        |
        v
  [Outbound Ports: BookcaseRepository, ShelfAccessPort]
        ^                                ^
        |                                |
  [Adapter: BookcaseRepositoryImpl]  [Adapter: ShelfAccessPortAdapter]
        |                                |
  [JPA: BookcaseJpaRepository]     [ShelfFacade (another module)]
```

**What this change improved:**

1. **Domain completeness:** `Bookcase.java` (`core/domain/model/`) now carries `userId`, making user ownership a first-class domain concept. Before, you had to reach into `infrastructure/entity/BookcaseEntity.java` to get it -- a dependency-rule violation.

2. **Port contract accuracy:** `BookcaseRepository.save()` (`core/ports/outbound/`) changed from `void` to `Bookcase`. The port now expresses: "I will persist this and return the hydrated version." The adapter (`infrastructure/adapter/outbound/BookcaseRepositoryImpl`) handles the entity-domain translation internally. No infrastructure type leaks outward.

3. **Mapper completeness:** `BookcaseMapper` (`core/domain/`) now maps all fields in both directions. Before, `toEntity()` was dropping `bookcaseZone`, `bookcaseIndex`, and `userId`, and was incorrectly setting `bookcaseId` on new entities (JPA should generate it).

4. **Coupling direction is preserved:** All `import` statements in `core/` reference only `core/` packages. `BookcaseRepositoryImpl` imports from both `core/` and `infrastructure/`, which is correct -- adapters depend inward. No core class imports an entity or Spring annotation.

---

## 7) Testing and verification story

### What tests exist and were run:
- **141 tests total**, all passing after the change.
- Tests touched directly by this change:
  - `BookcaseTest` (3 tests) -- domain model constructor validation
  - `CreateBookcaseUseCaseTest` (3 tests) -- happy path, conflict, shelf count
  - `QueryBookcaseUseCaseTest` (11 tests) -- all query paths
  - `BookcaseServiceTest` (10 tests) -- delegation to use cases
  - `PromptOptionsTest` (7 tests) -- CLI prompt option building

### Commands to run:
```bash
# All tests
mvn test

# Just the bookcase module tests
mvn test -Dtest="BookcaseTest,CreateBookcaseUseCaseTest,QueryBookcaseUseCaseTest,DeleteBookcaseUseCaseTest,BookcaseServiceTest,PromptOptionsTest"
```

### High-leverage test cases to add (tied to the diff):

1. **`BookcaseMapper` round-trip test:** `Bookcase -> toEntity() -> toDomain()` should return an equivalent object. Catches field-mapping omissions like the zone/index bug.
2. **`BookcaseMapper.toDomain(null)` test:** Should handle null gracefully (return null or throw a clear exception) -- the NPE was hiding here.
3. **`BookcaseMapper.toEntity()` does not set bookcaseId test:** Verify that for new bookcases, the entity's ID is null (JPA generates it).
4. **`CreateBookcaseUseCase` verifies userId is passed through:** Assert that the `Bookcase` passed to `save()` has the correct `userId`.
5. **`BookcaseRepositoryImpl.save()` integration test (H2):** Verify the full round-trip: domain -> entity -> JPA save -> entity -> domain, with a real database.
6. **`BookcaseRepositoryImpl.findBookcaseByBookcaseLocation()` returns null for unknown location:** Integration test with H2.
7. **`CreateBookcaseUseCase` with null userId:** Should it be allowed? If not, add domain validation and a test.
8. **`Bookcase.toString()` test:** Verify the format string handles null `bookcaseId` without NPE (it uses `%d` which will print "null" for null `Long`, which is fine).
9. **`BookcaseEntity.toString()` test:** Same as above for the entity.
10. **`BookcaseTest` for userId getter/setter:** Trivial but completes domain model coverage.

---

## 8) Tradeoffs and alternatives

### 1. Required userId in constructor vs. optional setter
- **Decision made:** `userId` is a required constructor parameter.
- **Alternative:** Add only a setter, keep the old constructor signature.
- **Why chosen:** A required param makes the compiler enforce user association at every construction site. You can't accidentally create an orphan bookcase.
- **Risk introduced:** 30+ call sites had to change, increasing blast radius of the commit.
- **When I would revisit:** If bookcases can be "unowned" (e.g., shared library), make it `Optional<Long>` or nullable with explicit documentation.

### 2. save() returns Bookcase vs. returns Long (just the ID)
- **Decision made:** Return the full `Bookcase` domain object.
- **Alternative:** Return just the generated `Long` ID.
- **Why chosen:** Returning the full object is more flexible and follows Spring Data's convention. The use case might need other fields that the database populated (defaults, timestamps).
- **Risk introduced:** Slightly more work in the adapter (full toDomain mapping instead of just extracting the ID).
- **When I would revisit:** If performance profiling shows the extra mapping is costly on high-throughput paths (unlikely for this app).

### 3. Null-check in adapter vs. Optional return type on port
- **Decision made:** Null-check with explicit `return null` in the adapter.
- **Alternative:** Change the port to return `Optional<Bookcase>`.
- **Why chosen:** Consistency with the existing port contract and other methods that already return nullable `Bookcase`. Changing to `Optional` would ripple across all callers.
- **Risk introduced:** Callers must remember to null-check. No compile-time enforcement.
- **When I would revisit:** When touching all port methods holistically, migrate to `Optional` everywhere at once.

### 4. Splitting into 3 commits vs. 1 squashed commit
- **Decision made:** 3 logical commits (model, logic, tests).
- **Alternative:** Squash into a single atomic commit.
- **Why chosen:** Easier code review -- each commit has a single intent. Reviewer can verify model changes in isolation, then logic, then test updates.
- **Risk introduced:** Intermediate commits don't compile/pass tests (commit 1 breaks callers until commit 3 fixes tests).
- **When I would revisit:** In a CI pipeline that tests every commit individually, I would squash or reorder to keep each commit green.

### 5. Removing bookcaseId from toEntity() vs. keeping it
- **Decision made:** Removed `bookcaseEntity.setBookcaseId(bookcase.getBookcaseId())` from `toEntity()`.
- **Alternative:** Keep setting it (needed for updates/merges).
- **Why chosen:** For new entities, setting the ID interferes with JPA's `@GeneratedValue`. The current save path is only used for creation.
- **Risk introduced:** If `toEntity()` is later used for update operations, the missing ID will cause JPA to insert instead of update.
- **When I would revisit:** When implementing bookcase updates -- likely need separate `toNewEntity()` and `toExistingEntity()` methods, or re-add the ID conditionally.

### 6. Logger instance per class vs. centralized logging
- **Decision made:** Each class gets its own `private static final Logger logger`.
- **Alternative:** Use a shared logging utility or aspect.
- **Why chosen:** Standard SLF4J pattern. Each logger is named after its class, making log filtering trivial.
- **Risk introduced:** None. This is the industry standard.
- **When I would revisit:** If cross-cutting logging concerns (audit trails, request tracing) become complex, introduce an AOP aspect.

---

## 9) Glossary of "tech I used" (only what is proven)

- **Java 17+** -- `Long` boxing, `String.format()`, standard class structure
- **Spring Boot / Spring Framework** -- `@Service` annotation on use cases, `ResponseStatusException` for HTTP-aware error handling
- **Spring Data JPA** -- `JpaRepository.save()` returns the persisted entity with generated ID; `findBy...` query derivation
- **SLF4J** -- `LoggerFactory.getLogger()` for structured logging at info/warn/error levels
- **Hexagonal Architecture (Ports & Adapters)** -- outbound port (`BookcaseRepository` interface in `core/ports/outbound/`), adapter (`BookcaseRepositoryImpl` in `infrastructure/adapter/outbound/`)
- **DDD (Domain-Driven Design)** -- domain model (`Bookcase` in `core/domain/model/`), use-case classes in `core/application/usecases/`, mapper in `core/domain/`
- **JUnit 5** -- `@Test`, `@ExtendWith(MockitoExtension.class)`
- **Mockito** -- `@Mock`, `@InjectMocks`, `when/thenReturn`, `verify`, `any()`, `eq()`, `times()`, `never()`
- **AssertJ** -- `assertThat(...).isNotNull()`, `.isEqualTo()`, `.isInstanceOf()`, `.hasMessageContaining()`
- **Maven** -- `mvn test`, `mvn compile`, surefire plugin for test execution

---

## 10) Red flags and how I would defend them

### 1. `@Service` on `CreateBookcaseUseCase` (Spring annotation in core)
- **Smell:** The `core/application/usecases/` package should ideally be framework-agnostic. `@Service` couples the use case to Spring's component scanning.
- **Pragmatic defense:** The annotation has no behavioral effect beyond bean registration. Removing it would require explicit `@Bean` factory methods in a configuration class, adding complexity without changing testability (tests already use `@InjectMocks`, not Spring context).
- **Follow-up refactor:** Move `@Service` to a Spring `@Configuration` class in the infrastructure layer that wires the use cases explicitly.

### 2. `BookcaseMapper` lives in `core/domain/` but imports `infrastructure/entity/BookcaseEntity`
- **Smell:** The domain package depends inward AND outward -- it imports an infrastructure type. This violates the dependency rule.
- **Pragmatic defense:** The mapper is a translation boundary. Placing it in `core/domain/` keeps it close to the domain model it translates. The alternative (placing it in infrastructure) would require the infrastructure to know about DTOs from the API layer.
- **Follow-up refactor:** Move `BookcaseMapper` to a dedicated `core/domain/mapping/` or `infrastructure/mapping/` package. Better yet, split it: `toDomain()` and `toEntity()` live in infrastructure (where entity types are native), `toDTO()` lives in the API adapter.

### 3. Mutable domain model (setters on `Bookcase`)
- **Smell:** `Bookcase` has public setters (`setBookcaseId`, `setUserId`, etc.), making it mutable. Domain objects in DDD are ideally immutable or controlled-mutation.
- **Pragmatic defense:** JPA entity mapping and the save-return pattern benefit from mutability in this codebase's current state. The constructor enforces required fields; setters exist for framework interop.
- **Follow-up refactor:** Make `Bookcase` immutable (all fields final, no setters). Use a builder for construction. The mapper creates new instances instead of mutating.

### 4. Stray semicolon in `BookcaseRepositoryImpl`
- **Smell:** Line `if (...) {;` has a stray `;` after the opening brace. Harmless but sloppy.
- **Pragmatic defense:** No behavioral impact. Linter would catch it.
- **Follow-up refactor:** Remove the stray semicolon. Add Checkstyle or SpotBugs to CI.

### 5. `BookcaseMapper` has info-level logging in `toEntity()`
- **Smell:** Logging at INFO level inside a mapper is noisy. Mappers are called frequently and the log adds no operational value beyond debugging.
- **Pragmatic defense:** Useful during active development/debugging of this refactor. Can be downgraded.
- **Follow-up refactor:** Change to `logger.debug()` or remove entirely. Mapper operations should be silent in production.

### 6. `existingRecordError` is a pre-instantiated exception field
- **Smell:** `CreateBookcaseUseCase` stores a `ResponseStatusException` as an instance field and reuses it. Exception objects capture stack traces at construction time, so the stack trace will point to class initialization, not the actual throw site.
- **Pragmatic defense:** The message is what matters, not the stack trace, for a 409 CONFLICT response.
- **Follow-up refactor:** Create the exception at the throw site: `throw new ResponseStatusException(HttpStatus.CONFLICT, "...")`. This gives an accurate stack trace.

---

## 11) Practice interview Q&A (grounded)

### Q1: "Why did you change save() from void to returning Bookcase?"
**Testing:** Do you understand side-effect contracts vs. explicit return values?
**Answer:**
- The old void contract relied on JPA mutating the entity's ID field as a side-effect.
- The mapper creates a *new* entity in `toEntity()`, breaking the reference chain -- the domain object never gets the ID.
- Returning `Bookcase` makes the contract explicit: "I persisted this and here's the hydrated version with the generated ID."
- Evidence: `CreateBookcaseUseCase.java:49` -- `bookcase = bookcaseRepository.save(bookcase)`.
**Files:** `BookcaseRepository.java`, `BookcaseRepositoryImpl.java`, `CreateBookcaseUseCase.java`

### Q2: "How did you decide where userId belongs?"
**Testing:** DDD understanding -- aggregate ownership.
**Answer:**
- `userId` was on `BookcaseEntity` but not on `Bookcase` (domain model). That forced any multi-user logic to depend on the infrastructure layer.
- In DDD, ownership is a domain concept. The aggregate root (`Bookcase`) should carry it.
- The constructor enforces it: you can't build a `Bookcase` without specifying who owns it.
**Files:** `Bookcase.java`, `BookcaseMapper.java`

### Q3: "How did you manage the blast radius of a breaking constructor change?"
**Testing:** Risk management, refactoring discipline.
**Answer:**
- Adding a required parameter to `Bookcase()` is intentionally breaking -- the compiler finds every call site.
- I split into 3 commits: model change, logic change, test fixes. Each is reviewable independently.
- I ran all 141 tests after the final commit to verify green.
- The compiler did the work of finding all 30+ call sites -- no manual searching needed.
**Files:** All 5 test files, `CreateBookcaseUseCase.java`

### Q4: "What was the NPE bug and how did you find it?"
**Testing:** Debugging methodology.
**Answer:**
- `BookcaseRepositoryImpl.findBookcaseByBookcaseLocation()` called `BookcaseMapper.toDomain()` on the JPA result without null-checking.
- If no bookcase exists at that location (which is the normal case during *creation*), JPA returns null, and `toDomain(null)` throws NPE.
- Found during code review of the adapter while updating the save contract.
- Fixed with a null-guard and added a warning log for observability.
**Files:** `BookcaseRepositoryImpl.java:23-30`

### Q5: "Why didn't you use Optional instead of null?"
**Testing:** API design tradeoffs.
**Answer:**
- The existing port contract returns nullable `Bookcase` across all methods (`findById`, `findByLocation`, etc.).
- Switching one method to `Optional` would create inconsistency.
- The right move is to migrate all port methods to `Optional` in a dedicated commit, not piecemeal.
- For now, the null-check in the adapter is sufficient and consistent.
**Files:** `BookcaseRepository.java`

### Q6: "Why is BookcaseMapper in the core/domain package if it imports an infrastructure entity?"
**Testing:** Dependency rule understanding.
**Answer:**
- This is a known smell. The mapper imports `BookcaseEntity` from the infrastructure layer, which violates the dependency rule.
- Pragmatic reason: it's the single translation point between all three types (entity, domain, DTO). Placing it in infrastructure would require infrastructure to also know about API DTOs.
- The proper fix is to split the mapper: entity-domain translation in infrastructure, domain-DTO translation in the API adapter.
**Files:** `BookcaseMapper.java` (import of `BookcaseEntity`)

### Q7: "Walk me through the data flow of creating a bookcase."
**Testing:** End-to-end understanding, hexagonal flow.
**Answer:**
- CLI/Controller calls `BookcaseFacade.createNewBookCase(userId, label, ...)`.
- Facade delegates to `CreateBookcaseUseCase.createNewBookCase()`.
- Use case calls `bookcaseRepository.findBookcaseByBookcaseLocation(label)` -- if exists, throw 409 CONFLICT.
- Constructs `new Bookcase(null, userId, shelfCapacity, ...)` -- null ID because it's new.
- Calls `bookcaseRepository.save(bookcase)` -- returns hydrated `Bookcase` with generated ID.
- Loops `shelfCapacity` times, calling `shelfAccessPort.createShelf()` for each.
- Returns `CreateBookcaseResult(bookcase.getBookcaseId())`.
**Files:** `CreateBookcaseUseCase.java`

### Q8: "How did your test strategy change with the save() return type?"
**Testing:** Test design, mock philosophy.
**Answer:**
- Old: `doAnswer` with 7 lines of argument-mutation logic to simulate JPA's side-effect.
- New: `when(repo.save(any())).thenReturn(savedBookcase)` -- 1 line, declarative, no mutation.
- The simplification in the test *validates* that the production contract improved. Complex mocks are a signal of complex contracts.
**Files:** `CreateBookcaseUseCaseTest.java`

### Q9: "What fields were missing from toEntity() and why does that matter?"
**Testing:** Attention to detail, data integrity.
**Answer:**
- `bookcaseZone` and `bookcaseIndex` were not being set in `toEntity()`. They would persist as null in the database even when the domain object had values.
- Also, `toEntity()` was setting `bookcaseId` explicitly, which can conflict with JPA's `@GeneratedValue` strategy on new records.
- Fixed by adding the missing setters and removing the ID assignment.
**Files:** `BookcaseMapper.toEntity()`

### Q10: "How do you ensure your hexagonal boundaries hold over time?"
**Testing:** Architecture governance.
**Answer:**
- Package structure: `core/domain/model/`, `core/ports/outbound/`, `infrastructure/adapter/outbound/`. The naming makes violations visible.
- Evidence: the project has a `CliArchitectureTest` (visible in test output) that likely enforces dependency rules with ArchUnit.
- Code review: I specifically checked that no `core/` class imports from `infrastructure/` (except the known mapper smell).
- Port interfaces: the outbound port (`BookcaseRepository`) speaks only domain types (`Bookcase`, `Long`, `String`, `List`).
**Files:** `BookcaseRepository.java`, package structure

### Q11: "Why use ResponseStatusException instead of a custom domain exception?"
**Testing:** Error handling philosophy, layer separation.
**Answer:**
- `ResponseStatusException` is a Spring Web class. Using it in `CreateBookcaseUseCase` (core layer) leaks HTTP semantics into the domain.
- Pragmatic defense: the app is small, the use case is only invoked via HTTP/CLI, and the exception message is domain-meaningful.
- Proper fix: throw a custom `BookcaseAlreadyExistsException` from the domain, catch it in the controller adapter, and translate to 409 CONFLICT there.
**Files:** `CreateBookcaseUseCase.java:16`

### Q12: "What would you do differently if this were a team project?"
**Testing:** Collaboration, process maturity.
**Answer:**
- Ensure each commit compiles and passes tests independently (the current split has an intermediate broken state).
- Add the mapper round-trip test before making changes (test-first).
- Open the PR with a description linking the ADR and showing before/after dependency diagrams.
- Request review from the team member who owns the Shelf module (since `ShelfAccessPort` is a cross-module concern).

---

## 12) Next steps talking points

### Immediate (today)
- Remove the stray semicolon in `BookcaseRepositoryImpl.findBookcaseByBookcaseLocation()`.
- Downgrade `BookcaseMapper.toEntity()` logging from INFO to DEBUG.
- Remove the unused `org.apache.logging.slf4j.SLF4JLogger` import in `CreateBookcaseUseCase` (if still present after formatting commit).

### Short-term hardening (this week)
- Add a `BookcaseMapper` round-trip unit test: `Bookcase -> toEntity() -> toDomain()` should preserve all fields.
- Add a `BookcaseMapper.toDomain(null)` test to codify null-handling behavior.
- Consider a `Bookcase.Builder` to replace the 7-parameter constructor.
- Add an integration test for `BookcaseRepositoryImpl.save()` using an in-memory H2 database.
- Add domain validation: should `userId` be nullable? If not, enforce it in the constructor with a null-check.

### Strategic refactors (later)
- Move `BookcaseMapper` out of `core/domain/` -- split into infrastructure mapper (entity <-> domain) and API mapper (domain <-> DTO).
- Replace `ResponseStatusException` in use cases with custom domain exceptions (`BookcaseAlreadyExistsException`), mapped to HTTP status in the controller adapter.
- Migrate all `BookcaseRepository` port methods to return `Optional<Bookcase>` instead of nullable.
- Make `Bookcase` immutable (all fields final, builder pattern, no setters).
- Introduce `BookcaseId` and `UserId` value objects to replace raw `Long` identifiers -- prevents accidental parameter swapping.
- Evaluate whether `toEntity()` needs an update path (set `bookcaseId` for existing entities) vs. creation-only.
