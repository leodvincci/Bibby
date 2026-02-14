# Bibby — Architecture Analysis & Improvement Plan

**Date:** 2026-02-14
**Scope:** Backend-only (Spring Boot 3.5.7 / Java 17 / Maven)
**Entry point:** `src/main/java/com/penrose/bibby/BibbyApplication.java`

---

## 1) Repo Summary

| Attribute | Value |
|-----------|-------|
| Language / Runtime | Java 17 |
| Framework | Spring Boot 3.5.7 + Spring Shell 3.4.1 |
| Build tool | Maven (wrapper) |
| Database | PostgreSQL (prod), H2 (test) |
| External APIs | Google Books API (via WebFlux `WebClient`) |
| Auth | Spring Security (HTTP Basic / form + BCrypt) |
| Code style | Spotless (Google Java Format) |
| Arch testing | ArchUnit 1.3.0 (1 test present) |
| API docs | springdoc-openapi 2.8.0 |
| Deployment | Fly.io, Docker, GitHub Actions CI |
| Main source files | ~123 Java files |
| Test files | 13 |

**What is Bibby?** A personal-library management system that lets users catalogue books (including via ISBN barcode scan), organize them onto shelves and bookcases, and track circulation (checkout / check-in). It exposes both a Spring Shell CLI and a REST API.

---

## 2) Architecture Inference

### 2.1 — Intended Architecture

The codebase is **explicitly designed around Hexagonal Architecture (Ports & Adapters) with DDD tactical patterns**, organized as a **modular monolith by bounded context**. Evidence:

- Each bounded context (e.g., `library/cataloging/book`) has a `contracts/` (ports + DTOs), `core/` (application + domain), and `infrastructure/` (entities, repos, mappers) sub-tree.
- Inbound ports are modeled as `*Facade` interfaces; outbound cross-context dependencies use `*AccessPort` interfaces.
- Domain objects use value objects (`BookId`, `Isbn`, `Title`, `AuthorRef`, `ShelfId`, etc.).
- Factories (`BookFactory`, `AuthorFactory`, `ShelfFactory`) encapsulate creation.
- An ArchUnit test (`BookCommandLineTest.java`) enforces that CLI cannot depend on infrastructure.

### 2.2 — Identified Bounded Contexts

| # | Context | Root package | Maturity |
|---|---------|-------------|----------|
| 1 | **Cataloging — Book** | `library.cataloging.book` | Most developed. Full hexagonal. |
| 2 | **Cataloging — Author** | `library.cataloging.author` | Complete but thinner. |
| 3 | **Stacks — Bookcase** | `library.stacks.bookcase` | Complete hexagonal. |
| 4 | **Stacks — Shelf** | `library.stacks.shelf` | Complete hexagonal. |
| 5 | **Registration** | `library.registration` | Partial; has duplicate contract files. |
| 6 | **Classification** | `library.classification` | Skeletal; domain-only, no infra. |
| 7 | **Discovery** | `library.discovery` | Minimal; one use case + one DTO. |
| 8 | **CLI** | `cli` | Shell commands + prompt mini-hexagonal. |
| 9 | **Web** | `web.controllers` | REST controllers, grouped by context. |
| 10 | **Config** | `config` | Security + WebClient beans. |

### 2.3 — Actual Architecture Style

While the *intention* is hexagonal, the *reality* is a **hybrid layered/hexagonal** system where several shortcuts have eroded the port boundaries. The CLI layer respects boundaries well (enforced by ArchUnit); the web layer does not. The domain layer is "DDD-ish" — value objects exist but the `Book` aggregate is anemic (all-setter, no invariant enforcement beyond `checkout()`).

---

## 3) Smells & Risks (with path evidence)

### SMELL 1 — God Controller: `BookController`

**File:** `src/main/java/com/penrose/bibby/web/controllers/cataloging/book/BookController.java`

This controller has **8 injected dependencies** spanning 4 bounded contexts:

```
BookService, BookFacade, AuthorJpaRepository, IsbnLookupService,
IsbnEnrichmentService, ShelfService, BookcaseService, AuthorFacade
```

Problems:
- **Lines 6, 37:** `AuthorJpaRepository` is injected directly into a controller — bypasses both the author facade and the book facade.
- **Lines 12–14:** Application-layer services (`BookService`, `IsbnLookupService`, `IsbnEnrichmentService`) are injected alongside the `BookFacade` that should wrap them, creating two parallel paths to the same logic.
- **Lines 3, 86, 105:** `BookcardRenderer` (a CLI presentation component) is imported and `new`-ed inside a REST controller.
- **Lines 19–21:** Cross-context services (`ShelfService`, `BookcaseService`) injected directly rather than through outbound ports.
- **Line 121:** Method returns `BookEntity` (JPA infrastructure type) from `assignBookToShelf`.

**Why this hurts:** Any change to book persistence, author lookup, or shelf logic requires touching this controller. It's the coupling nexus of the entire system.

---

### SMELL 2 — Infrastructure Leakage into Contract Layer

**File:** `src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookDTO.java`

```java
import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;  // line 3
import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;       // line 5
```

`BookDTO.fromEntity()` (line 26) creates a hard dependency from the contracts layer → infrastructure layer. The contracts package is supposed to be the public API of this bounded context; importing JPA entities into it defeats the purpose of having a `contracts/` boundary.

**Also in:** `BookFacadeAdapter.java:49,60` calls `BookDTO.fromEntity()` directly.

---

### SMELL 3 — BookService Depends on Infrastructure of Other Contexts

**File:** `src/main/java/com/penrose/bibby/library/cataloging/book/core/application/BookService.java`

- **Line 17:** `import ...stacks.bookcase.infrastructure.repository.BookcaseJpaRepository;`
- **Line 16:** `import ...stacks.bookcase.infrastructure.entity.BookcaseEntity;`
- **Line 36:** Field `BookcaseJpaRepository bookcaseJpaRepository`

The Book application service directly depends on the Bookcase module's **infrastructure** internals. This is a cross-context reach-through that skips the port boundary entirely.

- **Line 23:** `import org.springframework.web.server.ResponseStatusException;` — an HTTP concern in the application layer (line 302).

---

### SMELL 4 — Mapper Dependency on Facades

**File:** `src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/mapping/BookMapper.java`

- **Lines 3-4:** Imports `AuthorDTO` and `AuthorFacade` from the Author context.
- **Line 23:** Constructor-injects `AuthorFacade`.
- **Lines 350-357:** `toEntityFromBookMetaDataResponse()` calls `authorFacade.getAuthorById()` during mapping.

Mappers should be pure data-transformation functions. A mapper that invokes a cross-context facade during mapping interleaves I/O with transformation, making it untestable without a live Spring context or mocks.

---

### SMELL 5 — Duplicate Mapper Classes

| Primary | Duplicate | Status |
|---------|-----------|--------|
| `BookMapper.java` (413 lines) | `BookMapperTwo.java` (47 lines) | BookMapperTwo has one used method |
| `AuthorMapper.java` | `AuthorMapperTwo.java` (14 lines) | AuthorMapperTwo is an **empty shell** — no methods at all |

**Paths:**
- `library/cataloging/book/infrastructure/mapping/BookMapperTwo.java`
- `library/cataloging/author/infrastructure/mapping/AuthorMapperTwo.java`

Both are `@Component`-annotated, so they're loaded by Spring regardless. `AuthorMapperTwo` is dead code.

---

### SMELL 6 — Duplicate Contract Files (Registration)

Two identical `RegisterUserCommand` records exist at:
- `library/registration/contracts/RegisterUserCommand.java`
- `library/registration/contracts/commands/RegisterUserCommand.java`

Same for `RegisterUserResult`:
- `library/registration/contracts/RegisterUserResult.java`
- `library/registration/contracts/results/RegisterUserResult.java`

This is a leftover from a restructuring. One set is dead code.

---

### SMELL 7 — Anemic Domain Model

**File:** `src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/Book.java`

The `Book` aggregate is entirely setter-driven. The only behavior is `checkout()` (line 57). No invariants on construction, no protection of internal state. For example:
- `setIsbn()`, `setTitle()`, `setShelfId()` allow arbitrary mutation at any time.
- Shelf assignment has no domain validation — the capacity check lives in `BookService.assignBookToShelf()` (line 262), not in the domain.

**Why this hurts here:** The hexagonal architecture's value proposition is that the domain is the source of truth for rules. When the domain is anemic, the architecture's ceremony (ports, adapters, factories, mappers) costs complexity without delivering safety.

---

### SMELL 8 — BookcaseRepository Interface in Infrastructure

**File:** `src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/repository/BookcaseRepository.java`

This interface returns `BookcaseEntity` (a JPA entity). It sits in `infrastructure/` rather than `core/domain/`. Compare with `BookDomainRepository` and `ShelfDomainRepository`, which correctly live in `core/domain/`. Inconsistent placement.

---

### SMELL 9 — 96 `System.out.println` Calls in Production Code

Widespread throughout `BookController`, `BookService`, `BookFacadeAdapter`, etc. These should be SLF4J logger calls. In production, they produce unstructured output and cannot be filtered or level-controlled.

---

### SMELL 10 — Massive Commented-Out Code

`BookService.java` is ~345 lines but roughly **40%** is commented-out code (lines 64-120, 130-153, 187-229, 243-245, 273-282, 293-296). This obscures the actual active logic and makes the file harder to navigate.

---

### SMELL 11 — Inconsistent Adapter Placement

The `AuthorFacadeImpl` (the author's inbound adapter) lives at:
`library/cataloging/author/infrastructure/adapters/AuthorFacadeImpl.java`

But the `BookFacadeAdapter` lives at:
`library/cataloging/book/contracts/adapters/BookFacadeAdapter.java`

One is under `infrastructure/adapters/`, the other under `contracts/adapters/`. The `contracts/` directory should not contain implementations.

---

### SMELL 12 — Domain Holds Primitive Foreign Key (`shelfId: Long`)

**File:** `library/cataloging/book/core/domain/Book.java:16` — `private Long shelfId;`

The Book domain directly holds a raw `Long` for `shelfId` instead of a typed `ShelfId` value object (which exists at `library/stacks/shelf/core/domain/valueobject/ShelfId.java`). This allows arbitrary Long values and creates an implicit coupling.

---

## 4) Architecture Scorecard

| Dimension | Score | Justification |
|-----------|-------|---------------|
| **Cohesion** | 6/10 | Bounded contexts are well-identified and package-grouped. Within contexts, the contracts/core/infrastructure split works. Dragged down by `BookController` orchestrating 4 contexts and `BookMapper` performing I/O. |
| **Coupling** | 4/10 | Significant cross-context reach-throughs: `BookService` → `BookcaseJpaRepository`, `BookController` → `AuthorJpaRepository`, `BookMapper` → `AuthorFacade`. The contracts layer is compromised by entity imports. |
| **Discoverability** | 7/10 | The package naming (`contracts/`, `core/`, `infrastructure/`) is consistent and self-documenting. A new developer can locate the book domain in seconds. Dock 3 points for dead code, duplicate files, and `*Two` mapper names. |
| **Testability** | 4/10 | 13 tests for 123 source files (~10% coverage). `BookMapper` requires a Spring context due to facade injection. Domain classes are testable but undertested. `BookController` requires mocking 8 dependencies. |
| **Change Safety** | 4/10 | Only 1 ArchUnit rule exists (CLI→infra). No arch rules for web→domain, domain→infra, or cross-context isolation. Changing BookEntity ripples through BookDTO.fromEntity(), BookMapper, BookService, and BookController. |
| **Feature Scalability** | 7/10 | The bounded-context structure supports adding new contexts (Classification, Discovery) without restructuring. The existing template (contracts/core/infra) is repeatable. Score reduced because cross-cutting patterns aren't formalized. |

**Overall: 5.3 / 10** — Good structural foundation with DDD intent, undermined by boundary violations that accumulate coupling debt.

---

## 5) Recommended Target Structure

### Approach: **Fix the boundaries, don't rearchitect**

The existing bounded-context-by-package structure is sound. The target is to enforce the hexagonal layering that's already intended but locally violated. No big-bang restructuring needed.

### Target Package Structure (Book context as exemplar)

```
com.penrose.bibby
├── BibbyApplication.java
│
├── config/                                    # Cross-cutting infrastructure config
│   ├── WebClientConfig.java
│   └── security/
│       └── WebSecurityConfigs.java
│
├── cli/                                       # CLI adapter (inbound)
│   ├── command/
│   │   ├── book/
│   │   ├── bookcase/
│   │   ├── booklist/
│   │   ├── library/
│   │   └── shelf/
│   ├── prompt/
│   │   ├── application/
│   │   ├── contracts/
│   │   ├── domain/
│   │   └── infrastructure/
│   └── ui/
│
├── web/                                       # REST adapter (inbound)
│   └── controllers/
│       ├── cataloging/
│       │   ├── author/
│       │   │   └── AuthorController.java      # Depends ONLY on AuthorFacade
│       │   └── book/
│       │       ├── BookController.java        # Depends ONLY on BookFacade
│       │       └── BookImportController.java  # Depends ONLY on BookFacade
│       ├── registration/
│       └── stacks/
│           ├── bookcase/                      # Depends ONLY on BookcaseFacade
│           └── shelf/                         # Depends ONLY on ShelfFacade
│
├── library/
│   ├── cataloging/
│   │   ├── author/
│   │   │   ├── contracts/                     # PUBLIC API of this context
│   │   │   │   ├── AuthorDTO.java             # No entity imports
│   │   │   │   └── ports/
│   │   │   │       └── inbound/
│   │   │   │           └── AuthorFacade.java
│   │   │   ├── core/                          # INTERNAL — no framework imports
│   │   │   │   ├── application/
│   │   │   │   │   └── AuthorService.java
│   │   │   │   └── domain/
│   │   │   │       ├── Author.java
│   │   │   │       ├── AuthorId.java
│   │   │   │       ├── AuthorName.java
│   │   │   │       ├── AuthorFactory.java     # No @Component — pure Java
│   │   │   │       └── AuthorRepository.java  # Interface — no JPA types
│   │   │   └── infrastructure/                # PRIVATE — framework lives here
│   │   │       ├── adapters/
│   │   │       │   └── AuthorFacadeImpl.java  # Implements AuthorFacade
│   │   │       ├── entity/
│   │   │       │   └── AuthorEntity.java
│   │   │       ├── mapping/
│   │   │       │   └── AuthorMapper.java      # No facade calls; pure mapping
│   │   │       └── repository/
│   │   │           ├── AuthorJpaRepository.java
│   │   │           └── AuthorRepositoryImpl.java
│   │   │
│   │   └── book/
│   │       ├── contracts/                     # PUBLIC API
│   │       │   ├── dtos/
│   │       │   │   ├── BookDTO.java           # NO fromEntity() — mapper does this
│   │       │   │   ├── BookRequestDTO.java
│   │       │   │   ├── BookSummary.java
│   │       │   │   ├── BookDetailView.java
│   │       │   │   ├── BriefBibliographicRecord.java
│   │       │   │   ├── BookMetaDataResponse.java
│   │       │   │   ├── BookPlacementResponse.java
│   │       │   │   ├── BookReference.java
│   │       │   │   └── BookShelfAssignmentRequest.java
│   │       │   └── ports/
│   │       │       ├── inbound/
│   │       │       │   └── BookFacade.java
│   │       │       └── outbound/
│   │       │           ├── AuthorAccessPort.java
│   │       │           └── ShelfAccessPort.java
│   │       ├── core/                          # INTERNAL
│   │       │   ├── application/
│   │       │   │   ├── BookService.java       # No JPA repos, no HttpStatus
│   │       │   │   ├── IsbnLookupService.java
│   │       │   │   └── IsbnEnrichmentService.java
│   │       │   └── domain/
│   │       │       ├── Book.java              # Rich: enforce invariants
│   │       │       ├── BookId.java
│   │       │       ├── Title.java
│   │       │       ├── Isbn.java
│   │       │       ├── AuthorRef.java
│   │       │       ├── AuthorName.java
│   │       │       ├── BookMetaData.java
│   │       │       ├── AvailabilityStatus.java
│   │       │       ├── BookFactory.java
│   │       │       └── BookDomainRepository.java
│   │       └── infrastructure/                # PRIVATE
│   │           ├── adapters/
│   │           │   ├── BookFacadeAdapter.java  # MOVED from contracts/adapters/
│   │           │   ├── AuthorAccessPortAdapter.java
│   │           │   └── ShelfAccessPortAdapter.java
│   │           ├── entity/
│   │           │   └── BookEntity.java
│   │           ├── external/
│   │           │   ├── GoogleBooksResponse.java
│   │           │   └── ... (other API DTOs)
│   │           ├── mapping/
│   │           │   └── BookMapper.java        # MERGED; no facade dependency
│   │           └── repository/
│   │               ├── BookJpaRepository.java
│   │               └── BookDomainRepositoryImpl.java
│   │
│   ├── stacks/
│   │   ├── bookcase/
│   │   │   ├── contracts/
│   │   │   │   ├── dtos/
│   │   │   │   └── ports/inbound/
│   │   │   ├── core/
│   │   │   │   ├── application/
│   │   │   │   └── domain/
│   │   │   │       └── BookcaseRepository.java  # MOVED from infrastructure/
│   │   │   └── infrastructure/
│   │   └── shelf/
│   │       └── (same pattern)
│   │
│   ├── classification/                        # Grow when needed
│   │   └── core/domain/
│   ├── discovery/                             # Grow when needed
│   │   ├── contracts/dtos/
│   │   └── core/application/
│   └── registration/
│       ├── contracts/
│       │   ├── commands/                      # SINGLE location
│       │   │   └── RegisterUserCommand.java
│       │   ├── results/
│       │   │   └── RegisterUserResult.java
│       │   └── dtos/
│       ├── core/
│       └── infrastructure/
```

### Key Structural Principles

**What a context's `contracts/` folder owns:**
- Inbound port interfaces (`*Facade`)
- Outbound port interfaces (`*AccessPort`)
- DTOs / commands / results (plain records, no entity imports)

**What a context's `core/` folder owns:**
- Application services (orchestration, transactions)
- Domain model (entities, value objects, factories, domain repo interfaces)
- No Spring framework imports (except `@Service`, `@Transactional` in application layer)

**What a context's `infrastructure/` folder owns:**
- Facade adapter implementations
- Port adapter implementations
- JPA entities, JPA repositories, mappers
- External API clients

**Cross-context interaction rule:**
Context A can only depend on Context B's `contracts/` package. Never on `core/` or `infrastructure/`.

---

## 6) Folder Contracts + Rules

### Rule 1 — Controllers depend only on Facades

Controllers may only inject `*Facade` interfaces. No services, no repositories, no mappers, no infrastructure types.

```
// ALLOWED
BookController → BookFacade

// FORBIDDEN
BookController → BookService
BookController → AuthorJpaRepository
BookController → BookcardRenderer
```

### Rule 2 — Contracts contain no infrastructure imports

Files in any `contracts/` package must not import from any `infrastructure/` package. DTOs must not have `fromEntity()` methods.

### Rule 3 — Domain contains no framework imports

Classes in `core/domain/` must not import Spring, JPA, or web framework types. Domain repository interfaces accept/return domain types only.

### Rule 4 — Application layer owns transactions, not controllers

`@Transactional` annotations belong on application services or facade adapters, not on controller methods.

### Rule 5 — Cross-context access goes through ports only

`BookService` must not import from `library.stacks.bookcase.infrastructure`. It must use a `BookcaseAccessPort` or go through `BookcaseFacade`.

### Rule 6 — Mappers are pure functions

Mappers must not inject facades or repositories. They transform data in → data out. If a mapping requires a lookup, that lookup happens in the calling application service, and the result is passed to the mapper.

### Rule 7 — No `System.out.println` in production code

All logging must use SLF4J (`Logger`). Enforceable via Spotless or Checkstyle rule.

### Enforcement Mechanisms

**ArchUnit tests** (expand from existing 1 test to cover all rules):

```java
// Rule 1: Controllers only depend on facades
noClasses().that().resideInAPackage("..web.controllers..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("..core.application..", "..core.domain..",
                         "..infrastructure..", "..cli..")
    .check(classes);

// Rule 2: Contracts contain no infrastructure imports
noClasses().that().resideInAPackage("..contracts..")
    .should().dependOnClassesThat()
    .resideInAPackage("..infrastructure..")
    .check(classes);

// Rule 3: Domain has no framework imports
noClasses().that().resideInAPackage("..core.domain..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("org.springframework..", "jakarta.persistence..")
    .check(classes);

// Rule 5: Cross-context isolation
noClasses().that().resideInAPackage("..cataloging.book..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("..stacks..infrastructure..", "..registration..infrastructure..")
    .check(classes);

// Rule 6: Mappers don't inject facades
noClasses().that().haveSimpleNameEndingWith("Mapper")
    .should().dependOnClassesThat()
    .haveSimpleNameEndingWith("Facade")
    .check(classes);
```

**Spotless / Checkstyle:**
- Ban `System.out.println` via regex rule in `spotless-maven-plugin` or a Checkstyle `RegexpSinglelineJava` check.

**CI gate:**
- Run ArchUnit tests as part of `mvn verify` in GitHub Actions (already configured).

---

## 7) Migration Plan

### Step 1 — Delete dead code (Quick Win)

**What to do:**
- Delete `AuthorMapperTwo.java` (empty shell, 0 methods)
- Delete `library/registration/contracts/RegisterUserCommand.java` (duplicate of `contracts/commands/RegisterUserCommand.java`)
- Delete `library/registration/contracts/RegisterUserResult.java` (duplicate of `contracts/results/RegisterUserResult.java`)
- Remove all commented-out code in `BookService.java` (~100 lines)
- Remove `old_banner.txt`

**What to verify:**
- Run `mvn compile` — no compilation errors
- Run `mvn test` — all tests pass
- `grep -r "AuthorMapperTwo"` to confirm no remaining references (only `BookMapperTwo` imports it — handle in Step 2)

**Risk:** Very low. Dead code removal.

---

### Step 2 — Consolidate BookMapperTwo into BookMapper

**What to do:**
- Move `BookMapperTwo.toDomainFromJSON()` logic into `BookMapper` (or better: into `IsbnEnrichmentService` which owns the Google Books flow)
- Delete `BookMapperTwo.java`
- Update any references (likely `IsbnEnrichmentService` or `BookFacadeAdapter`)

**What to verify:**
- `mvn compile && mvn test`
- Search for all `BookMapperTwo` references

**Risk:** Low. Single method relocation.

---

### Step 3 — Remove entity imports from BookDTO

**What to do:**
- Delete `BookDTO.fromEntity()` static method
- Move the conversion logic into `BookMapper.toDTOfromEntity()` (which already exists and does similar work)
- Update all callers: `BookFacadeAdapter.findBookByIsbn()`, `BookFacadeAdapter.findBookByTitle()`, `BookService.findBookById()`, `BookService.findBookByTitleIgnoreCase()`
- Remove `AuthorEntity` and `BookEntity` imports from `BookDTO.java`

**What to verify:**
- `mvn compile && mvn test`
- Confirm `BookDTO.java` has zero infrastructure imports

**Risk:** Low-medium. Multiple callers to update, but mechanical replacement.

---

### Step 4 — Move BookFacadeAdapter to infrastructure/adapters

**What to do:**
- Move `library/cataloging/book/contracts/adapters/BookFacadeAdapter.java` → `library/cataloging/book/infrastructure/adapters/BookFacadeAdapter.java`
- Move `library/cataloging/book/contracts/adapters/AuthorAccessPortAdapter.java` → `library/cataloging/book/infrastructure/adapters/AuthorAccessPortAdapter.java`
- Move `library/cataloging/book/contracts/adapters/ShelfAccessPortAdapter.java` → `library/cataloging/book/infrastructure/adapters/ShelfAccessPortAdapter.java`
- Update package declarations

**What to verify:**
- `mvn compile && mvn test`
- Confirm `contracts/` packages contain only interfaces and DTOs

**Risk:** Low. Package rename only; Spring component scan is package-agnostic within the base package.

---

### Step 5 — Decouple BookMapper from AuthorFacade

**What to do:**
- Remove `AuthorFacade` constructor parameter from `BookMapper`
- In `BookMapper.toEntityFromBookMetaDataResponse()`: change the method signature to accept `List<AuthorDTO>` (already resolved) instead of `List<Long>` author IDs. The caller (`BookFacadeAdapter.createBookFromMetaData`) should resolve author IDs to DTOs before calling the mapper.
- Make `BookMapper` a stateless utility or keep as `@Component` but with zero injected dependencies.

**What to verify:**
- `mvn compile && mvn test`
- Confirm `BookMapper` has no facade/service imports

**Risk:** Medium. Requires updating the mapping call chain.

---

### Step 6 — Slim down BookController

**What to do:**
- Remove all direct service/repository injections. BookController should depend only on `BookFacade`.
- Move orchestration logic (e.g., `placeBookOnShelf` lines 114-148, `addNewBook` lines 152-168) into `BookFacadeAdapter` methods.
- Remove `BookcardRenderer` usage from the controller entirely (it's a CLI concern).
- Remove `AuthorJpaRepository`, `IsbnLookupService`, `IsbnEnrichmentService`, `ShelfService`, `BookcaseService`, `AuthorFacade` injections.
- Replace `System.out.println` with `log.debug()` calls.
- Remove `BookEntity` return types — all public methods should return DTOs.

**Target constructor:**
```java
public BookController(BookFacade bookFacade) { ... }
```

**What to verify:**
- `mvn compile && mvn test`
- All existing REST endpoints still return the same response shapes
- OpenAPI spec still valid (`/swagger-ui/`)

**Risk:** Medium-high. Largest single change. Consider splitting into sub-steps (one endpoint at a time).

---

### Step 7 — Remove BookcaseJpaRepository from BookService

**What to do:**
- Create a `BookcaseAccessPort` outbound port interface in `library/cataloging/book/contracts/ports/outbound/`
- Implement it in `library/cataloging/book/infrastructure/adapters/BookcaseAccessPortAdapter.java`
- Replace `BookcaseJpaRepository` usage in `BookService.getBookLocation()` with `BookcaseAccessPort`
- Remove `org.springframework.web.server.ResponseStatusException` from `BookService` — throw domain-specific exceptions instead

**What to verify:**
- `mvn compile && mvn test`
- `BookService` has zero imports from `..stacks..infrastructure..`

**Risk:** Medium. New interface + adapter, but mechanical.

---

### Step 8 — Move BookcaseRepository interface to domain

**What to do:**
- Move `library/stacks/bookcase/infrastructure/repository/BookcaseRepository.java` → `library/stacks/bookcase/core/domain/BookcaseRepository.java`
- Change its method signatures to use domain types (or at minimum, keep current Entity types as a transition step and change later)
- Update `BookcaseRepositoryImpl` to reference the new location

**What to verify:**
- `mvn compile && mvn test`

**Risk:** Low. Package move.

---

### Step 9 — Expand ArchUnit test suite

**What to do:**
- Create `src/test/java/com/penrose/bibby/ArchitectureRulesTest.java`
- Implement all 6 architectural rules from Section 6 as ArchUnit tests
- Run them as part of CI

**What to verify:**
- All rules pass after Steps 1-8 are complete
- Add to GitHub Actions CI pipeline

**Risk:** Low. Test-only change, but provides ongoing enforcement.

---

### Step 10 — Replace System.out.println with SLF4J (Later Win)

**What to do:**
- Global search-and-replace across all `System.out.println` calls (~96 occurrences)
- Add proper SLF4J `Logger` fields where missing
- Use appropriate log levels (`debug` for trace-style, `info` for business events, `warn`/`error` for failures)

**What to verify:**
- `mvn compile && mvn test`
- Application still logs expected output in dev mode

**Risk:** Low. Mechanical replacement.

---

## 8) Quick Wins (< 60 minutes each)

| # | Win | Effort | Impact |
|---|-----|--------|--------|
| 1 | **Delete `AuthorMapperTwo.java`** — empty class, 0 methods, still component-scanned | 5 min | Removes dead code confusion |
| 2 | **Delete duplicate registration contracts** (`contracts/RegisterUserCommand.java`, `contracts/RegisterUserResult.java`) | 5 min | Eliminates "which one do I use?" ambiguity |
| 3 | **Remove all commented-out code from `BookService.java`** (~100 lines) | 10 min | Makes the actual logic readable |
| 4 | **Remove `BookcardRenderer` from `BookController`** (lines 3, 86, 105) — it's a CLI component in a REST controller | 10 min | Eliminates cross-layer dependency |
| 5 | **Delete `BookDTO.fromEntity()`** and route through `BookMapper.toDTOfromEntity()` | 30 min | Breaks the contracts→infrastructure dependency |
| 6 | **Replace `System.out.println` in `BookController`** with SLF4J | 15 min | Structured logging for one critical file |
| 7 | **Add 3 ArchUnit rules** (controllers→facades only, contracts→no infrastructure, domain→no framework) | 45 min | Prevents future regressions from day one |

---

## Appendix A — Dependency Graph (Simplified)

```
                    ┌──────────┐
                    │   CLI    │
                    │ commands │
                    └────┬─────┘
                         │ depends on
                    ┌────▼─────┐       ┌───────────┐
                    │ Facades  │◄──────│   Web     │
                    │(contracts│       │Controllers│
                    │  ports)  │       └───────────┘
                    └────┬─────┘              │
                         │                    │ VIOLATION: also depends on
                    ┌────▼─────┐         ┌────▼────────┐
                    │Application│         │ Services,   │
                    │ Services  │         │ JPA Repos,  │
                    └────┬─────┘         │ Entities    │
                         │               └─────────────┘
                    ┌────▼─────┐
                    │  Domain  │
                    │ (core)   │
                    └────┬─────┘
                         │ implemented by
                    ┌────▼──────────┐
                    │Infrastructure │
                    │(JPA, mappers, │
                    │ external APIs)│
                    └───────────────┘
```

**Target: Remove the "VIOLATION" arrow so Web Controllers only touch Facades.**

---

## Appendix B — File Count by Layer

| Layer | Files | Notes |
|-------|-------|-------|
| CLI commands | 12 | Well-isolated |
| CLI prompt | 4 | Mini-hexagonal, clean |
| CLI UI | 2 | Utility helpers |
| Web controllers | 6 | Need facade-only deps |
| Config | 2 | Standard Spring config |
| Contracts (ports + DTOs) | ~25 | Need entity import cleanup |
| Core (application + domain) | ~30 | Need framework import cleanup |
| Infrastructure | ~35 | Correct location for framework code |
| Tests | 13 | Need significant expansion |
| **Total** | **~130** | |
