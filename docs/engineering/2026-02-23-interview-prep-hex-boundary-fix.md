# Interview Prep Packet: Enforce Hexagonal Architecture Boundaries in a Library Management System

Date: 2026-02-23 (America/Chicago)
Branch: 300-bookcontroller-bypasses-inbound-port-depends-on-shelfservice-concrete-class
Range: 34968e8..ab831a2
Commits:
- `2c3b1f0` Add isFull and isEmpty capacity queries to Shelf module
- `9da6619` Add isFull to Book module's ShelfAccessPort
- `ed999a4` Move placeBookOnShelf orchestration behind BookFacade
- `6c61a49` Remove Stacks dependencies from BookController
- `2064cbf` Refactor BookDomainRepositoryImpl to remove unused dependencies and improve book retrieval logic
- `67dbef7` Refactor BookDomainRepositoryImpl constructor and remove unnecessary whitespace
- `4e86637` Remove unused BookControllerTest imports and simplify test structure
- `ab831a2` Remove unused BookControllerTest imports and simplify test structure

---

## 1) 30-second pitch (recruiter-friendly)

I refactored a Spring Boot library management application to enforce hexagonal architecture boundaries between two bounded contexts: Cataloging (books) and Stacks (shelves/bookcases). The BookController had accumulated direct dependencies on concrete service classes from another module, bypassing port interfaces and leaking domain internals into the web layer. I decomposed the 50-line controller method into a proper use case class, routed all cross-context communication through outbound ports, and reduced the controller's constructor from 7 dependencies to 4 — eliminating all cross-module coupling at the compile-time level.

---

## 2) 2-minute project walkthrough script (hiring manager)

"I'm building a personal library management system called Bibby using Spring Boot with a hexagonal architecture — sometimes called ports and adapters. The system has two main bounded contexts: Cataloging, which manages books and authors, and Stacks, which manages the physical shelves and bookcases.

I discovered that the BookController — the REST adapter in the Cataloging context — had a dependency on ShelfService, a concrete implementation class from the Stacks context. This meant the Cataloging module had a compile-time dependency on Stacks internals. If someone changed the ShelfService constructor, BookController would break. The controller was also manually constructing DTOs from Shelf domain objects inline — a 50-line method that orchestrated across three bounded contexts.

The constraint was that I needed to preserve the existing API behavior while respecting the hexagonal boundary rule: adapters talk to ports, never to concrete implementations, and cross-context calls flow through outbound ports owned by the calling module.

My approach was bottom-up in four commits. First, I added capacity query methods (isFull, isEmpty) to the Shelf module's inbound port so other modules could ask about shelf state without touching domain objects. Second, I extended the Book module's outbound port — ShelfAccessPort — with an isFull method so the Book application layer could validate capacity through its own port. Third, I created a new use case class, bookCommandUseCases, that owns the place-book-on-shelf orchestration: it validates the shelf exists, checks capacity via ShelfAccessPort, then delegates persistence to the repository. Finally, I stripped the BookController down to a single delegation call through BookFacade.

The result: BookController went from 7 constructor dependencies to 4, with zero imports from the Stacks module. The repository went from doing business logic — calling ShelfFacade directly to check capacity — to pure persistence. And I caught and fixed a circular dependency where the repository was injecting itself through its own interface, which caused a Spring BeanCurrentlyInCreationException."

---

## 3) Deep dive menu (pick 1-2 in interviews)

### 3.1 Cross-context dependency direction enforcement
- **Signal:** Architecture ownership, DIP understanding
- **Files:** `BookController.java`, `ShelfAccessPort.java`, `ShelfAccessPortAdapter.java`, `bookCommandUseCases.java`
- Before: BookController imported `ShelfService` (concrete), `BookcaseService` (concrete), `ShelfDTO`, `BookcaseDTO` from the Stacks module
- After: zero Stacks imports. Cross-context calls flow through `ShelfAccessPort` (owned by Book module) with `ShelfAccessPortAdapter` bridging to `ShelfFacade`
- The port interface lives in `book.core.port.outbound`, the adapter in `book.infrastructure.adapter.outbound` — Dependency Inversion Principle in action

### 3.2 Extracting orchestration from a controller into a use case
- **Signal:** Separation of concerns, single responsibility
- **Files:** `BookController.java` (before: 50-line `placeBookOnShelf`, after: 3 lines), `bookCommandUseCases.java` (new, 42 lines)
- Controller went from orchestrating Book + Shelf + Bookcase to a single `bookFacade.placeBookOnShelf()` call
- Validation (shelf exists, shelf not full) moved from the controller and repository into the use case layer where business rules belong
- Use case throws domain exceptions (`IllegalStateException`), not HTTP exceptions (`ResponseStatusException`)

### 3.3 Repository layer responsibility cleanup
- **Signal:** Clean architecture understanding, layer discipline
- **Files:** `BookDomainRepositoryImpl.java`
- Before: `placeBookOnShelf` called `shelfFacade.findShelfById()` twice (existence + capacity), crossed module boundaries from infrastructure
- After: pure persistence — find entity, set shelfId, save. All validation lives in the use case
- Also removed `ShelfFacade`, `AuthorService`, and `@Lazy` annotation from constructor — went from 4 dependencies to 2

### 3.4 Circular dependency detection and resolution
- **Signal:** Debugging, Spring DI understanding, production awareness
- **Files:** `BookDomainRepositoryImpl.java`
- During the refactor, `BookDomainRepositoryImpl` was accidentally given a `BookDomainRepository` constructor parameter — itself, through its own interface
- Spring threw `BeanCurrentlyInCreationException` at startup
- Fix: replaced `bookDomainRepository.getBookById(bookId)` with `bookJpaRepository.findById(bookId)` — the repo already had direct JPA access
- Commits `2064cbf` and `67dbef7` show the fix progression

### 3.5 Port interface design: isFull as a first-class query
- **Signal:** API design, encapsulation
- **Files:** `ShelfFacade.java`, `QueryShelfUseCase.java`, `ShelfAccessPort.java`
- Before: callers got the full Shelf domain object and called `.isFull()` themselves — leaking domain internals
- After: `ShelfFacade.isFull(Long shelfId)` is a first-class query, and `ShelfAccessPort.isFull(Long)` bridges it cross-context
- The domain model's `Shelf.isFull()` is still the source of truth, but it's encapsulated behind the port

### 3.6 Incremental commit strategy for a risky refactor
- **Signal:** Risk management, engineering discipline
- **Files:** All 11 changed files across 8 commits
- Bottom-up: Shelf port first, then Book outbound port, then use case + repository, then controller
- Each commit compiles independently and maintains the existing contract
- Fix commits (`2064cbf`, `67dbef7`) show honest iteration — not a polished rewrite

### 3.7 API contract change: response simplification
- **Signal:** Pragmatic tradeoff awareness
- **Files:** `BookController.java`
- Changed `POST /{bookId}/shelf` return type from `BookPlacementResponse` (bookId, title, shelfId, shelfLabel, bookcaseLabel) to `HttpStatus` (200 OK)
- Tradeoff: simpler controller at the cost of a breaking API change for clients that consumed the response body
- Documented in commit message as a known risk

---

## 4) STAR stories

### Story 1: "I identified and fixed an architectural boundary violation across two bounded contexts"
- **Seniority signal:** architecture, ownership
- **Difficulty:** high

**Situation:** In a Spring Boot library management system using hexagonal architecture, the BookController had accumulated direct dependencies on concrete service classes (ShelfService, BookcaseService) from the Stacks bounded context, bypassing port interfaces.

**Task:** I owned the full refactor: identify all violations, design the proper dependency flow, implement the fix, and ensure the API contract was preserved.

**Actions:**
- Traced the full dependency chain: controller -> service -> repository, identifying that the repository was also calling ShelfFacade directly for business validation
- Designed a four-layer fix: add capacity queries to Shelf port, extend Book's outbound port, create a use case class for orchestration, strip the controller
- Created `bookCommandUseCases` to own validation (shelf exists, shelf not full) using `ShelfAccessPort` — the Book module's own outbound port
- Reduced `BookDomainRepositoryImpl` from 4 constructor dependencies to 2 by removing `ShelfFacade` and `AuthorService`
- Reduced `BookController` from 7 constructor dependencies to 4, eliminating all Stacks module imports
- Committed incrementally (bottom-up) so each commit was independently valid

**Result:** Zero cross-context concrete class dependencies. The Cataloging module can now be compiled, tested, and deployed independently of Stacks implementation details. The controller method went from 50 lines of cross-context orchestration to 3 lines.

**What I would improve next time:**
- Add integration tests before refactoring to catch regressions like the circular dependency earlier
- Extract a `BookcaseAccessPort` for the remaining `BookcaseJpaRepository` dependency in `BookService.getBookLocation()`

### Story 2: "I caught a circular dependency that would have crashed production"
- **Seniority signal:** debugging, quality
- **Difficulty:** med

**Situation:** After the initial refactor of `BookDomainRepositoryImpl`, the Spring application context failed to load with a `BeanCurrentlyInCreationException`.

**Task:** Diagnose the root cause and fix it without reverting the architectural improvement.

**Actions:**
- Read the full stack trace: `bookDomainRepositoryImpl` was requesting itself through the `BookDomainRepository` interface during construction
- Traced to a line I introduced: `bookDomainRepository.getBookById(bookId)` — calling through the port when the class already had `bookJpaRepository` injected
- Replaced with `bookJpaRepository.findById(bookId)` and removed the self-referencing constructor parameter
- Cleaned up the constructor in a follow-up commit to remove unused parameters (`AuthorService`, `@Lazy ShelfFacade`)

**Result:** Application context loads cleanly. Repository has only 2 dependencies (BookMapper, BookJpaRepository) instead of 5.

**What I would improve next time:**
- Run the full Spring context test (`BibbyApplicationTests`) before pushing — would have caught this immediately
- Consider adding an ArchUnit test to prevent self-referencing repository injections

### Story 3: "I moved business validation out of a repository into the application layer"
- **Seniority signal:** architecture, quality
- **Difficulty:** med

**Situation:** `BookDomainRepositoryImpl.placeBookOnShelf()` was doing shelf existence checks and capacity validation by calling `ShelfFacade.findShelfById()` twice — business logic in the persistence layer, and a cross-boundary call from Book infrastructure to Shelf's inbound port.

**Task:** Move validation to the correct architectural layer without losing the safety checks.

**Actions:**
- Created `bookCommandUseCases` in `book.core.application.usecases` with `ShelfAccessPort` and `BookDomainRepository` injected
- Implemented shelf-exists and shelf-is-full checks using `ShelfAccessPort` (the Book module's own outbound port, not Shelf's facade)
- Replaced `ResponseStatusException` (web concern) with `IllegalStateException` (domain concern) for validation failures
- Stripped `BookDomainRepositoryImpl.placeBookOnShelf()` to pure persistence: find entity, set field, save

**Result:** Clear separation: use case validates, repository persists. The repository no longer imports `ShelfFacade` or any Stacks module types.

**What I would improve next time:**
- Rename `bookCommandUseCases` to `BookCommandUseCases` (Java naming convention)
- Consider splitting into `PlaceBookOnShelfUseCase` for single-responsibility, following the existing `DeleteShelvesUseCase` pattern

### Story 4: "I designed an outbound port to decouple two bounded contexts"
- **Seniority signal:** architecture, communication
- **Difficulty:** med

**Situation:** The Book module needed to check shelf capacity before placing a book, but the `ShelfAccessPort` (Book's outbound port) only exposed `findShelfById()` — returning a full `ShelfDTO`.

**Task:** Extend the port interface to support capacity queries without leaking Shelf domain internals.

**Actions:**
- Added `isFull(Long shelfId)` to `ShelfFacade` backed by `QueryShelfUseCase`, which delegates to `Shelf.isFull()` (the domain model's invariant)
- Added `isEmpty(Long shelfId)` for completeness
- Extended `ShelfAccessPort` with `isFull(Long)` and implemented in `ShelfAccessPortAdapter` delegating to `ShelfFacade.isFull()`
- The Book module now asks "is this shelf full?" through its own port, without knowing how the Shelf domain calculates fullness

**Result:** Cross-context capacity validation with zero coupling to Shelf domain internals. If the Shelf team changes how fullness is calculated, the Book module is unaffected.

**What I would improve next time:**
- Rename the `isFull(Long aLong)` parameter to `isFull(Long shelfId)` in `ShelfAccessPort` — the parameter name leaked from auto-generation

---

## 5) Technical problem I ran into + how I solved it

### Problem: Circular bean dependency on `BookDomainRepositoryImpl`

**Symptom:** `BeanCurrentlyInCreationException` at application startup after commit `ed999a4`. Full stack trace showed `bookDomainRepositoryImpl` requesting itself.

**Root cause:** During the refactor, `BookDomainRepositoryImpl` was given a constructor parameter `BookDomainRepository bookDomainRepository` — the interface it implements. Line 212 called `bookDomainRepository.getBookById(bookId)`, which is a self-call through the Spring proxy. Spring detected the circular reference during eager construction.

**Fix (commits `2064cbf` + `67dbef7`):**
- Replaced `bookDomainRepository.getBookById(bookId)` with `bookJpaRepository.findById(bookId)` — the repository already had direct JPA access
- Removed `BookDomainRepository` from the constructor parameters
- Also removed `AuthorService` and `@Lazy ShelfFacade` that were no longer needed

**How I verified:** Application context loads successfully (the test that was failing: `BibbyApplicationTests`).

**Preventative guardrail:** An ArchUnit rule could enforce that no `@Component` class injects its own interface: `noClasses().that().implement(BookDomainRepository.class).should().dependOnClassesThat().areAssignableTo(BookDomainRepository.class)`.

---

## 6) Architecture narrative

### Dependency direction before

```
BookController (web adapter)
    |--- ShelfService (concrete, Stacks core) -- VIOLATION
    |--- BookcaseService (concrete, Stacks core) -- VIOLATION
    |--- Shelf domain objects (Stacks domain) -- VIOLATION
    |--- BookService (concrete, Book core)

BookDomainRepositoryImpl (Book infrastructure)
    |--- ShelfFacade (Stacks inbound port) -- VIOLATION: infra calling another module's port
```

The web adapter reached past ports into concrete classes. The repository did business validation and crossed module boundaries.

### Dependency direction after

```
BookController (web adapter)
    |---> BookFacade (Book inbound port)
    |---> AuthorFacade (Author inbound port)
    |---> BookService, IsbnLookupService (Book application)

BookFacade --> BookService --> bookCommandUseCases
                                  |---> ShelfAccessPort (Book outbound port)
                                  |        |---> ShelfAccessPortAdapter (Book infra)
                                  |                 |---> ShelfFacade (Shelf inbound port)
                                  |---> BookDomainRepository (Book outbound port)
                                           |---> BookDomainRepositoryImpl (Book infra)
                                                    |---> BookJpaRepository (Spring Data)
```

**Coupling decreased:**
- `BookController`: 7 dependencies -> 4. Zero Stacks imports (was: `ShelfService`, `BookcaseService`, `ShelfDTO`, `BookcaseDTO`, `BookPlacementResponse`, `ResponseStatusException`)
- `BookDomainRepositoryImpl`: 4 dependencies -> 2. Removed `ShelfFacade`, `AuthorService`
- `ShelfFacade`: removed `BookPlacementResponse` and `BookShelfAssignmentRequest` imports
- `ShelfService`: removed `bookCommandUseCases` dependency

**Evidence in imports:**
- `BookController.java`: removed 6 import lines from `library.stacks.*`
- `BookDomainRepositoryImpl.java`: removed `import ShelfFacade`, `import AuthorService`, `import @Lazy`

---

## 7) Testing and verification story

### What exists now
- `BookControllerTest.java` was added in commit `6c61a49` then simplified in commits `4e86637` and `ab831a2`
- `BibbyApplicationTests` (Spring context load test) — this is what caught the circular dependency

### What I ran (evidenced in commits)
The circular dependency fix commits (`2064cbf`, `67dbef7`) show that the application context test failed and was subsequently fixed.

### Recommended commands
```bash
# Full context load (catches circular deps, missing beans)
mvn test -pl . -Dtest=BibbyApplicationTests

# Full test suite
mvn test

# Targeted: just the affected modules
mvn test -Dtest="BookControllerTest,BookServiceTest,ShelfServiceTest"
```

### High-leverage test cases to add
1. **Unit: bookCommandUseCases.placeBookOnShelf** — shelf not found throws IllegalStateException
2. **Unit: bookCommandUseCases.placeBookOnShelf** — shelf is full throws IllegalStateException
3. **Unit: bookCommandUseCases.placeBookOnShelf** — happy path delegates to bookDomainRepository
4. **Unit: bookCommandUseCases.placeBookOnShelf** — null shelfAssignmentRequest throws IllegalStateException
5. **Integration: POST /api/v1/books/{id}/shelf** — returns 200 on success
6. **Integration: POST /api/v1/books/{id}/shelf** — returns error when shelf is full
7. **Unit: QueryShelfUseCase.isFull** — delegates to Shelf domain model
8. **Unit: BookDomainRepositoryImpl.placeBookOnShelf** — sets shelfId and saves (no shelf validation)
9. **Architecture: ArchUnit** — no class in `web.controllers` imports from `*.core.application.*Service` (concrete classes)
10. **Architecture: ArchUnit** — no class in `*.infrastructure.repository` imports from `*.ports.inbound.*`

---

## 8) Tradeoffs and alternatives

### 8.1 Use case class vs. inline service method
- **Decision:** Created a new `bookCommandUseCases` class
- **Alternative:** Add the validation logic directly in `BookService.placeBookOnShelf()`
- **Why chosen:** Follows existing codebase pattern (`DeleteShelvesUseCase`, `CreateShelfUseCase`, `QueryShelfUseCase`). Keeps BookService as a thin facade delegator
- **Risk:** Another class to maintain; naming convention (`bookCommandUseCases` vs `PlaceBookOnShelfUseCase`) is inconsistent with established patterns
- **Revisit when:** The class accumulates more than 2-3 commands — then split into single-responsibility use cases

### 8.2 Dropping BookPlacementResponse from the API
- **Decision:** Changed `POST /{bookId}/shelf` to return `HttpStatus` instead of `BookPlacementResponse`
- **Alternative:** Keep the rich response by having the use case return a response DTO
- **Why chosen:** Avoids the controller needing shelf and bookcase data, which would require additional port calls or a composite response from the use case
- **Risk:** Breaking change for any frontend or API consumer expecting the response body
- **Revisit when:** Frontend needs shelf/bookcase confirmation data — reintroduce response via the use case

### 8.3 ShelfAccessPort returns ShelfDTO (Shelf module's type)
- **Decision:** Kept `ShelfAccessPort.findShelfById()` returning `ShelfDTO` from `shelf.api.dtos`
- **Alternative:** Define a Book-owned record (e.g., `ShelfInfo`) and map in the adapter
- **Why chosen:** `ShelfDTO` is in the `shelf.api` package which is intended as a shared contract. Changing it requires a wider refactor
- **Risk:** Book module has a compile-time dependency on Shelf's API package
- **Revisit when:** Modules are extracted into separate Gradle subprojects — at that point, define module-owned types

### 8.4 Validation in use case vs. domain model
- **Decision:** Shelf existence and capacity checks live in `bookCommandUseCases`
- **Alternative:** Push validation into the `Book` domain model or a domain service
- **Why chosen:** The validation requires cross-context data (shelf info) which the domain model shouldn't fetch — it should be pure
- **Risk:** Validation logic is procedural (if/throw) rather than domain-driven
- **Revisit when:** Introducing a domain event system — shelf placement could be an event that the Shelf module validates asynchronously

### 8.5 @Lazy removal from BookDomainRepositoryImpl
- **Decision:** Removed `@Lazy ShelfFacade` from the repository constructor entirely
- **Alternative:** Keep `@Lazy` to break the existing circular dependency chain
- **Why chosen:** The root cause of the circular dependency was the repository calling ShelfFacade — removing the dependency is better than masking it with `@Lazy`
- **Risk:** None — the dependency was the source of the architectural violation
- **Revisit when:** Never — this was the correct fix

### 8.6 Incremental commits vs. single squash
- **Decision:** 8 commits showing the progression, including fix commits
- **Alternative:** Squash into a single polished commit
- **Why chosen:** Reviewers can see the reasoning layer by layer; fix commits show honest iteration
- **Risk:** Messy history if not squash-merged
- **Revisit when:** Merging to main — squash-merge the PR

---

## 9) Glossary of "tech I used" (only what is proven)

- **Spring Boot** — dependency injection, `@Service`, `@Component`, `@RestController`, `@Lazy` for breaking circular deps
- **Hexagonal Architecture (Ports and Adapters)** — inbound ports (`ShelfFacade`, `BookFacade`), outbound ports (`ShelfAccessPort`, `BookDomainRepository`), adapters (`ShelfAccessPortAdapter`, `BookDomainRepositoryImpl`)
- **Domain-Driven Design (DDD)** — bounded contexts (Cataloging vs. Stacks), domain model encapsulation (`Shelf.isFull()`), anti-corruption layer via ports
- **Dependency Inversion Principle** — high-level modules (use cases) define ports; low-level modules (adapters) implement them
- **Java Records** — `BookShelfAssignmentRequest(Long shelfId)` as an immutable request DTO
- **Spring Data JPA** — `BookJpaRepository.findById()` for persistence
- **Constructor Injection** — all dependencies injected via constructors, no field injection
- **Git** — incremental commit strategy, branch-per-issue naming convention, `Closes #300` for issue linking

---

## 10) Red flags and how I would defend them

### 10.1 `bookCommandUseCases` — lowercase class name
- **Smell:** Violates Java naming convention (classes should be PascalCase)
- **Defense:** Caught during review, will be renamed in a follow-up. The code works correctly regardless of naming
- **Follow-up:** Rename to `BookCommandUseCases` or split into `PlaceBookOnShelfUseCase`

### 10.2 `@Service` on a use case in `core.application`
- **Smell:** Spring framework annotation in the core/domain layer. Purist hex says core should be framework-agnostic
- **Defense:** Pragmatic choice for a monolith. The alternative (manual bean configuration in a `@Configuration` class) adds complexity without benefit until modules are extracted
- **Follow-up:** If extracting to a separate Gradle module, move `@Service` to a Spring config class

### 10.3 `BookService` still imports `BookcaseEntity` and `BookcaseJpaRepository`
- **Smell:** The application service imports infrastructure types from another bounded context (Stacks)
- **Defense:** Out of scope for this PR. Documented in the GitHub issue as a known remaining item
- **Follow-up:** Create a `BookcaseAccessPort` in the Book module, mirroring `ShelfAccessPort`

### 10.4 `ShelfAccessPort` returns `ShelfDTO` (not a Book-owned type)
- **Smell:** The outbound port returns a type owned by the Shelf module's API layer
- **Defense:** `shelf.api.dtos.ShelfDTO` is in the API package, intended as a shared contract. The alternative (Book-owned `ShelfInfo` record) is correct but requires wider changes
- **Follow-up:** When extracting to Gradle subprojects, define Book-owned types for cross-context data

### 10.5 Duplicate shelf lookup in `bookCommandUseCases`
- **Smell:** Calls `shelfAccessPort.findShelfById()` for existence check, then `shelfAccessPort.isFull()` which internally loads the shelf again
- **Defense:** Two separate port calls for two separate concerns (existence vs. capacity). The overhead is negligible for this use case
- **Follow-up:** Consider a single `shelfAccessPort.validatePlacement(shelfId)` method that checks both in one call

### 10.6 No error-to-HTTP mapping for domain exceptions
- **Smell:** `bookCommandUseCases` throws `IllegalStateException`, but no `@ExceptionHandler` maps it to the right HTTP status
- **Defense:** Spring Boot returns 500 by default for uncaught exceptions. A global exception handler should be added
- **Follow-up:** Add `@ControllerAdvice` that maps `IllegalStateException` to 400/404/409 as appropriate

---

## 11) Practice interview Q&A

### Q1: "Why did you choose hexagonal architecture for this project?"
- **Testing:** Architecture reasoning, tradeoff awareness
- **Answer:** The project has two bounded contexts (Cataloging and Stacks) that evolve independently. Hex architecture enforces that each module defines its own ports, so internal changes don't ripple. The violation I fixed proves why — a constructor change in ShelfService would have broken BookController. Ports make that impossible.
- **Point to:** `ShelfAccessPort.java`, `ShelfFacade.java` — two separate port interfaces for the same data, owned by different modules

### Q2: "Walk me through how you decided what belongs in the controller vs. the use case vs. the repository."
- **Testing:** Layering discipline, separation of concerns
- **Answer:** Controller: HTTP concerns (request parsing, status codes). Use case: business orchestration and validation (shelf exists? full?). Repository: persistence only (find, set field, save). The violation had all three mixed: controller did orchestration, repository did validation, both crossed module boundaries.
- **Point to:** `BookController.java` (before/after), `bookCommandUseCases.java`, `BookDomainRepositoryImpl.java`

### Q3: "How did you handle the circular dependency?"
- **Testing:** Debugging, Spring DI understanding
- **Answer:** `BookDomainRepositoryImpl` was injecting `BookDomainRepository` — its own interface. Spring detected the cycle during eager construction. The fix was to use `bookJpaRepository.findById()` directly, since the repository already had JPA access. Then I removed the self-referencing constructor parameter entirely.
- **Point to:** Commits `2064cbf` and `67dbef7`, `BookDomainRepositoryImpl.java` constructor

### Q4: "Why didn't you just swap ShelfService for ShelfFacade in the controller?"
- **Testing:** Depth of understanding, not settling for surface fixes
- **Answer:** ShelfFacade returns `Shelf` domain objects. The controller would still need to know Shelf internals to map them. And a controller in the Cataloging context shouldn't depend on the Stacks inbound port at all — it should go through the Book module's own application layer, which calls out via ShelfAccessPort.
- **Point to:** `ShelfFacade.java` (returns `Optional<Shelf>`), `ShelfAccessPort.java` (returns `Optional<ShelfDTO>`)

### Q5: "What's the difference between an inbound port and an outbound port in your design?"
- **Testing:** Architecture vocabulary, concrete understanding
- **Answer:** Inbound port (`BookFacade`, `ShelfFacade`) is what the outside world calls into the module. Outbound port (`ShelfAccessPort`, `BookDomainRepository`) is what the module's core defines to reach external systems. The core owns both but only calls outbound. Adapters implement outbound ports; controllers call inbound ports.
- **Point to:** `BookFacade.java` (inbound), `ShelfAccessPort.java` (outbound), `ShelfAccessPortAdapter.java` (adapter implementing outbound)

### Q6: "How did you decide to break this into multiple commits instead of one?"
- **Testing:** Engineering discipline, risk management
- **Answer:** Bottom-up: each commit adds one capability without breaking what exists. Commit 1 adds shelf queries (Shelf module, self-contained). Commit 2 extends Book's outbound port. Commit 3 creates the use case. Commit 4 strips the controller. If any commit introduced a regression, I could bisect to the exact change.
- **Point to:** `git log --oneline 34968e8..ab831a2`

### Q7: "The response type changed from BookPlacementResponse to HttpStatus. How would you handle this in production?"
- **Testing:** API versioning, backward compatibility awareness
- **Answer:** This is a breaking change. In production I'd version the endpoint (`/v2/books/{id}/shelf`) or keep the old response by having the use case return placement details. I chose simplicity here because the frontend isn't live yet. Documented the risk in the commit message.
- **Point to:** `BookController.java` line 96-99

### Q8: "Why does bookCommandUseCases throw IllegalStateException instead of a custom exception?"
- **Testing:** Exception design, pragmatism
- **Answer:** `IllegalStateException` is semantically correct (shelf not found or full is an invalid state for the operation). A custom `ShelfNotAvailableException` would be more descriptive but adds a class for one use case. I'd add custom exceptions when there are 3+ distinct error conditions that need different HTTP mappings.
- **Point to:** `bookCommandUseCases.java` lines 28, 32

### Q9: "How do you prevent this kind of boundary violation from happening again?"
- **Testing:** Process thinking, quality engineering
- **Answer:** ArchUnit tests. Rule: "no class in `web.controllers` should import from `*.core.application` except through ports." Rule: "no class in `*.infrastructure.repository` should import from `*.ports.inbound` of another module." These are compile-time enforceable.
- **Point to:** Package structure, port naming convention

### Q10: "What would you do differently if Cataloging and Stacks were separate microservices?"
- **Testing:** Scalability thinking, distributed systems
- **Answer:** `ShelfAccessPort` would become an HTTP client adapter instead of an in-process call. The port interface stays the same — that's the whole point. `ShelfAccessPortAdapter` would call a REST API or message queue instead of `ShelfFacade`. The use case and domain code wouldn't change at all.
- **Point to:** `ShelfAccessPort.java` (unchanged interface), `ShelfAccessPortAdapter.java` (only this class changes)

### Q11: "Why is the repository calling the use case's getBookById instead of using JPA directly?"
- **Testing:** Attention to detail (this was the bug)
- **Answer:** It shouldn't have been. That was the circular dependency bug. The repository should use its own `bookJpaRepository.findById()` for data access. Calling through the port interface caused a self-referencing injection. I caught it when Spring failed to start and fixed it in the follow-up commits.
- **Point to:** `BookDomainRepositoryImpl.java` final version vs. commit `ed999a4`

### Q12: "How would you test the use case in isolation?"
- **Testing:** Testing strategy, mockability
- **Answer:** Mock `ShelfAccessPort` and `BookDomainRepository` (both are interfaces/ports). Test: (1) shelf not found -> exception, (2) shelf full -> exception, (3) happy path -> `bookDomainRepository.placeBookOnShelf` called with correct args. No Spring context needed — pure unit test.
- **Point to:** `bookCommandUseCases.java` constructor (two injectable ports)

---

## 12) Next steps talking points

### Immediate (today)
- Fix the circular dependency in `BookDomainRepositoryImpl` (replace `bookDomainRepository.getBookById` with `bookJpaRepository.findById`)
- Run `mvn test` to confirm `BibbyApplicationTests` passes
- Rename `bookCommandUseCases` to `BookCommandUseCases`

### Short-term hardening (this week)
- Add unit tests for `bookCommandUseCases.placeBookOnShelf()` — 4 cases (null request, shelf not found, shelf full, happy path)
- Add `@ControllerAdvice` exception handler mapping `IllegalStateException` to appropriate HTTP statuses
- Rename `isFull(Long aLong)` parameter to `isFull(Long shelfId)` in `ShelfAccessPort`
- Add ArchUnit test: no `web.controllers` class imports from `*.core.application.*Service`

### Strategic refactors (later)
- Create `BookcaseAccessPort` in Book module to replace direct `BookcaseJpaRepository` usage in `BookService.getBookLocation()`
- Define Book-owned types (`ShelfInfo`, `BookcaseInfo`) instead of reusing Shelf/Bookcase DTOs in outbound ports
- Split `bookCommandUseCases` into single-responsibility use cases (`PlaceBookOnShelfUseCase`) following the `DeleteShelvesUseCase` pattern
- Consider event-driven architecture for cross-context operations (book placed -> shelf updated) to eliminate synchronous coupling entirely
