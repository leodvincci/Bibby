# Shelf Module Architecture Review

**Date:** 2026-02-23
**Branch:** `refactor/create-shelf-accept-domain-object`
**Reviewer:** Architecture Review (DDD + Hexagonal + Modular Monolith)
**Scope:** `library.stacks.shelf.*`, cross-module touchpoints

---

## 1. Executive Summary

### Strengths

- **Clean hexagonal skeleton.** The `core/ports/inbound` (ShelfFacade), `core/ports/outbound` (ShelfDomainRepository,
  BookAccessPort), and `infrastructure/adapter/outbound` layers are correctly separated. Dependency arrows point inward.
- **Good cross-context port design.** `BookAccessPort` is owned by the Shelf module and implemented by the Book module's
  `BookAccessAdapter` -- textbook Dependency Inversion across bounded contexts.
- **Use-case extraction is underway.** `CreateShelfUseCase`, `QueryShelfUseCase`, `DeleteShelvesUseCase` are split out
  and unit-tested. This is a strong foundation.

### Highest-Risk Issues (ranked)

1. **BookController bypasses the inbound port and depends on `ShelfService` concrete class** -- breaks hex boundary and
   makes Shelf internals a compile-time dependency for the Cataloging context.
2. **`ShelfDTO` (api layer) imports `ShelfEntity` (infrastructure layer)** -- inner layer depends on outer layer; DTO
   becomes a persistence-aware type.
3. **`ShelfFacade.createShelf()` accepts 4 primitives instead of a domain object** -- the facade's contract is anemic
   and forces callers to know construction order; domain model is bypassed at the boundary.
4. **Domain model `Shelf` has public setters that bypass invariants** -- `setShelfLabel(null)` is possible,
   `setBookIds()` allows unbounded mutation. The constructor guards can be sidestepped.
5. **`ShelfAccessPort` (Book module's outbound port) returns `ShelfDTO`** -- a DTO from the Shelf module leaks into the
   Book module's core, coupling the port to a representation type from another context.

### Quick Wins

- Remove `ShelfDTO.fromEntity()` / `fromEntityWithBookId()` static methods -- they are the source of the entity import
  in the api package. Move that mapping to `ShelfMapper`.
- Make `Shelf` fields `final` and remove all public setters -- the constructor already validates; setters undermine it.

---

## 2. Hexagonal Architecture Audit

### Inbound Adapters (controllers / CLI handlers)

| File                                                 | Verdict                                                                                                                        |
|------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `web.controllers.stacks.shelf.ShelfController`       | **Correct.** Depends on `ShelfFacade` (inbound port).                                                                          |
| `web.controllers.stacks.bookcase.BookCaseController` | **Correct.** Uses `ShelfFacade` for shelf deletion.                                                                            |
| `cli.command.shelf.ShelfScanCommands`                | **Neutral.** Placeholder; no dependencies yet.                                                                                 |
| `web.controllers.cataloging.book.BookController`     | **VIOLATED.** Line 19 imports `ShelfService` (concrete class), line 18 imports `ShelfDTO` (api DTO). Should use `ShelfFacade`. |

### Inbound Ports (use case interfaces)

| File                             | Notes                                                                                                                                                                            |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `core.ports.inbound.ShelfFacade` | Present and used correctly by most consumers. However, `createShelf(Long, int, String, int)` is a primitive-obsessed signature -- should accept a command object or domain type. |

No separate use-case interfaces exist (the use cases are concrete classes, not interfaces). This is acceptable at
current scale but means the facade is the only inbound contract.

### Application Layer (use cases / services)

| File                   | Verdict                                                                                                                                     |
|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| `ShelfService`         | **Correct.** Implements `ShelfFacade`, delegates to use cases. Only Spring annotation is `@Service`.                                        |
| `CreateShelfUseCase`   | **Correct.** Depends only on outbound port.                                                                                                 |
| `DeleteShelvesUseCase` | **Correct.** Depends on outbound ports.                                                                                                     |
| `QueryShelfUseCase`    | **Mostly correct.** Contains `ShelfSummary` construction logic (lines 34-41) that arguably belongs on the domain model or a domain service. |

### Domain Layer (entities / value objects)

| File           | Verdict                                                                                                                                                                                                       |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Shelf`        | **Mixed.** Constructor validation is good. But public setters break encapsulation, `books` is a raw `List<Long>` (not a value object), and `bookcaseId` is a raw `Long` (could be `BookcaseId` value object). |
| `ShelfSummary` | **Correct.** Immutable record.                                                                                                                                                                                |
| `ShelfId`      | **Correct.** Value object as a record.                                                                                                                                                                        |

### Outbound Ports (repositories / gateways)

| File                    | Verdict                                                                                                                                          |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `ShelfDomainRepository` | **Correct.** Interface in core, speaks domain types. Has a duplicate lookup: both `getById(ShelfId)` and `findById(Long)` -- should consolidate. |
| `BookAccessPort`        | **Correct.** Owned by Shelf, implemented by Book module. Clean DIP.                                                                              |

### Outbound Adapters (JPA, messaging, external clients)

| File                        | Verdict                                                                                                                                                               |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ShelfDomainRepositoryImpl` | **Correct placement.** Maps between entity and domain.                                                                                                                |
| `ShelfJpaRepository`        | **Correct.** Spring Data in infrastructure. Has unused method `getShelfEntitiesByBookcaseId`.                                                                         |
| `ShelfEntity`               | **Correct placement.** JPA entity in infrastructure.                                                                                                                  |
| `ShelfMapper`               | **Partially violated.** Contains `toDTO()` and `toDTOFromEntity()` methods that map to an api-layer type. An infrastructure mapper should only map domain <-> entity. |

### Dependency Direction Verdict: **Mostly Correct**

The main violations:

1. `BookController` -> `ShelfService` (adapter importing application concrete class across contexts)
2. `ShelfDTO` -> `ShelfEntity` (api importing infrastructure)
3. `ShelfMapper` -> `ShelfDTO` (infrastructure importing api)
4. `ShelfAccessPort` (Book core) -> `ShelfDTO` (Shelf api) -- cross-context type leakage

---

## 3. DDD Audit

### Aggregate Boundaries

**Current state:** `Shelf` is the aggregate root of the Shelf aggregate. It owns `ShelfId`, `shelfLabel`,
`shelfPosition`, `bookCapacity`, and a list of `books`.

**Assessment:**

- `Shelf` as aggregate root makes sense. It is the consistency boundary for shelf-level invariants (capacity, position).
- `books` (List<Long>) is a weak reference to the Book aggregate -- this is acceptable in a modular monolith where books
  live in another bounded context. However, it should be a `List<BookId>` value object for type safety.
- `bookcaseId` (Long) is a reference to the parent aggregate in the Bookcase context -- correct as a foreign reference,
  but should be `BookcaseId` value object.

### Invariants

| Invariant               | Where enforced                                               | Assessment                                                                                           |
|-------------------------|--------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| Label not blank         | `Shelf` constructor (line 31)                                | Good, but `setShelfLabel` (line 77) does NOT validate -- **bypass**                                  |
| Position >= 1           | `Shelf` constructor (line 34) + `setShelfPosition` (line 86) | Good                                                                                                 |
| Capacity >= 1           | `Shelf` constructor (line 37) + `setBookCapacity` (line 97)  | Good, but error message says "cannot be negative" when threshold is `< 1` -- **misleading**          |
| BookcaseId not null     | `Shelf` constructor (line 41)                                | Good, but `setBookcaseId` (line 18) does NOT validate -- **bypass**                                  |
| Shelf not over capacity | `Shelf.isFull()` (line 53) is a query only                   | **Not enforced** -- no method prevents adding a book beyond capacity. `setBookIds` accepts any list. |

### Domain Model Purity

**Anemic domain smell: Medium.**

- `Shelf` has some behavior (`isFull()`, `getBookCount()`) but no mutation behavior (no `addBook(BookId)`, no
  `removeBook(BookId)`, no `relocate(int newPosition)`).
- The `setBookIds` / `setBooks` methods are data-transfer style, not domain operations.
- `ShelfSummary` construction lives in `QueryShelfUseCase` (lines 34-41) rather than as a factory method on `Shelf` (
  e.g., `shelf.summarize()`).

**Transaction script smell: Low-Medium.**

- `CreateShelfUseCase.execute()` is pass-through (just saves). This is fine for a simple create, but the
  `ShelfService.createShelf()` method constructs the `Shelf` object with `null` ShelfId -- the domain object creation
  logic lives in the application service rather than a factory.
- `DeleteShelvesUseCase` orchestrates cross-aggregate cleanup (deleting books on shelves before deleting shelves). This
  is correct application-layer coordination.

### Ubiquitous Language Issues

| Current                                                                              | Problem                                                     | Suggested                                                            |
|--------------------------------------------------------------------------------------|-------------------------------------------------------------|----------------------------------------------------------------------|
| `ShelfFacade.createShelf(Long bookcaseId, int position, String s, int bookCapacity)` | Parameter named `s` (line 18 of ShelfFacade)                | `String shelfLabel`                                                  |
| `ShelfEntity.setBookcaseId(Long bookCaseLabel)`                                      | Parameter named `bookCaseLabel` (line 35 of ShelfEntity)    | `Long bookcaseId`                                                    |
| `ShelfDomainRepository.getById()` vs `findById()`                                    | Two methods doing similar things with different signatures  | Pick one convention: `findById(ShelfId)` returning `Optional<Shelf>` |
| `Shelf.setBooks()` vs `Shelf.setBookIds()`                                           | Duplicate setters with different names                      | Remove both; make `books` immutable                                  |
| `bookshelfFacade` in `BookCaseController` (line 24)                                  | Inconsistent naming -- "bookshelf" vs "shelf"               | `shelfFacade`                                                        |
| `ShelfScanCommands`                                                                  | "Scan" doesn't match the described behavior (import/shelve) | `ShelfImportCommands`                                                |

---

## 4. Boundary Hygiene & Coupling

### Compile-time coupling to other bounded contexts

| From                                      | To                                         | Via                            | Severity                                                                                   |
|-------------------------------------------|--------------------------------------------|--------------------------------|--------------------------------------------------------------------------------------------|
| `BookController` (web)                    | `shelf.core.application.ShelfService`      | Direct class import (line 19)  | **High** -- bypasses port                                                                  |
| `BookController` (web)                    | `shelf.api.dtos.ShelfDTO`                  | Direct import (line 18)        | **Med** -- DTO coupling across contexts                                                    |
| `book.core.port.outbound.ShelfAccessPort` | `shelf.api.dtos.ShelfDTO`                  | Return type (line 8)           | **High** -- Book core depends on Shelf api type                                            |
| `BookcaseService` (stacks)                | `shelf.core.ports.inbound.ShelfFacade`     | Constructor injection (line 8) | **Correct** -- via inbound port                                                            |
| `BookAccessAdapter` (book infra)          | `shelf.core.ports.outbound.BookAccessPort` | Implements interface (line 5)  | **Correct** -- adapter implements cross-context port                                       |
| `BookDomainRepositoryImpl` (book infra)   | `shelf.core.ports.inbound.ShelfFacade`     | `@Lazy` injection (line 35)    | **Suspicious** -- a repository implementation should not call a facade from another module |

### Core importing adapter violations

| Violation                        | File                           | Line   |
|----------------------------------|--------------------------------|--------|
| `ShelfDTO` imports `ShelfEntity` | `shelf/api/dtos/ShelfDTO.java` | Line 3 |

This is the only direct "core/api imports infrastructure" violation within the Shelf module itself.

### DTOs/entities leaking inward

- `ShelfDTO` appears in `ShelfAccessPort` (Book module's core port) -- a DTO from one module's api layer is used as a
  domain contract in another module's core.
- `BookAccessAdapter` uses `BookDTO` internally (line 3 of BookAccessAdapter) to retrieve data and then strips it to
  `List<Long>` -- this is correct adapter behavior.

### Mapping done in the wrong layer

| Mapping                        | Current location                           | Should be                                                            |
|--------------------------------|--------------------------------------------|----------------------------------------------------------------------|
| `Shelf -> ShelfDTO`            | `ShelfMapper` (infrastructure)             | Controller/adapter layer or a dedicated api mapper                   |
| `ShelfEntity -> ShelfDTO`      | `ShelfDTO.fromEntity()` (api layer)        | `ShelfMapper` (infrastructure)                                       |
| `Shelf -> ShelfDTO`            | Inline in `BookController` (lines 131-138) | Dedicated mapper in web layer                                        |
| `Shelf -> ShelfOptionResponse` | `ShelfResponseMapper` (web layer)          | **Correct**                                                          |
| `Shelf -> ShelfSummary`        | `QueryShelfUseCase` (application layer)    | Domain model (`Shelf.summarize()`) or keep in use case -- acceptable |

---

## 5. Findings Table

| #  | Finding                                                                            | Severity | Evidence                                           | Why It Matters                                                                                                                 | Suggested Fix                                                                                                                           |
|----|------------------------------------------------------------------------------------|----------|----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| 1  | `BookController` depends on `ShelfService` concrete class instead of `ShelfFacade` | **High** | `BookController.java:19,39,48`                     | Breaks hex boundary; Cataloging context compiles against Shelf internals; any refactor of ShelfService breaks BookController   | Change `ShelfService` to `ShelfFacade` in BookController. Add needed methods to `ShelfFacade` if missing.                               |
| 2  | `ShelfDTO` imports `ShelfEntity` via static factory methods                        | **High** | `ShelfDTO.java:3,13-21,23-31`                      | API layer depends on infrastructure; any entity change forces DTO recompile; violates dependency direction                     | Remove `fromEntity()` and `fromEntityWithBookId()`. Move that mapping to `ShelfMapper` in infrastructure.                               |
| 3  | `ShelfAccessPort` (Book core) returns `ShelfDTO` from Shelf module                 | **High** | `book/core/port/outbound/ShelfAccessPort.java:3,8` | Book's core port depends on Shelf's api type; tight cross-context coupling at the core level                                   | Define a Book-owned value type (e.g., `ShelfInfo` record in `book.core.port.outbound`) and map in `ShelfAccessPortAdapter`.             |
| 4  | `ShelfFacade.createShelf()` accepts 4 primitives                                   | **Med**  | `ShelfFacade.java:18`                              | Primitive obsession; parameter `s` is unnamed; callers must know argument order; can't add validation at the boundary          | Accept `Shelf` domain object or a `CreateShelfCommand` record. (Branch work is already heading this direction.)                         |
| 5  | `Shelf` domain model has public setters that bypass constructor invariants         | **Med**  | `Shelf.java:18,65,77,96,107,111`                   | `setShelfLabel(null)`, `setBookcaseId(null)`, `setBookIds(hugeList)` all succeed, breaking invariants the constructor enforces | Make fields `final`. Remove all setters. Use constructor or builder pattern. If mutation is needed, use domain methods with validation. |
| 6  | `ShelfDomainRepository` has duplicate ID lookups                                   | **Med**  | `ShelfDomainRepository.java:9,17`                  | `getById(ShelfId)` and `findById(Long)` do the same thing with different signatures; confusing contract                        | Remove `findById(Long)`, use `getById(ShelfId)` everywhere. Return `Optional<Shelf>` from `getById`.                                    |
| 7  | `ShelfMapper` maps to `ShelfDTO` (api type)                                        | **Low**  | `ShelfMapper.java:3,25,35`                         | Infrastructure mapper knows about api types; violates single-responsibility; couples infra to api                              | Remove `toDTO()` and `toDTOFromEntity()` from `ShelfMapper`. If DTO mapping is needed, put it in a separate api-layer mapper.           |
| 8  | `BookDomainRepositoryImpl` injects `ShelfFacade`                                   | **Med**  | `BookDomainRepositoryImpl.java:14,28,35`           | A repository adapter should not orchestrate cross-context calls; this is application-layer responsibility                      | Move the shelf lookup out of the repository into a use case or application service in the Book module.                                  |
| 9  | `Shelf.books` is `List<Long>` not `List<BookId>`                                   | **Low**  | `Shelf.java:12`                                    | Weak typing; easy to accidentally pass shelf IDs or bookcase IDs where book IDs are expected                                   | Create `BookId` value object (record) or reuse one from a shared kernel.                                                                |
| 10 | `ShelfJpaRepository` has unused method                                             | **Low**  | `ShelfJpaRepository.java:13`                       | `getShelfEntitiesByBookcaseId` duplicates `findByBookcaseId`                                                                   | Delete `getShelfEntitiesByBookcaseId`.                                                                                                  |
| 11 | Capacity validation message is misleading                                          | **Low**  | `Shelf.java:38,98`                                 | Says "cannot be negative" but rejects `0` too (threshold is `< 1`)                                                             | Change to "Book capacity must be at least 1"                                                                                            |
| 12 | `BookCaseController.bookshelfFacade` naming                                        | **Low**  | `BookCaseController.java:24`                       | Inconsistent with ubiquitous language ("bookshelf" vs "shelf")                                                                 | Rename to `shelfFacade`                                                                                                                 |

---

## 6. Proposed Target Structure

```
library/stacks/shelf/
  api/                                    # Published language (shared with other modules)
    dtos/
      ShelfOptionResponse.java            # Response DTO for REST/CLI
      ShelfSummaryResponse.java           # (rename ShelfDTO or split purpose)
    CreateShelfCommand.java               # Command record replacing primitive args
  core/
    domain/
      model/
        Shelf.java                        # Aggregate root (immutable fields, domain methods)
      valueobject/
        ShelfId.java                      # Value object (record)
        BookcaseId.java                   # Value object (record) - or shared kernel
    application/
      ShelfService.java                   # Facade implementation, orchestrates use cases
      usecases/
        CreateShelfUseCase.java
        DeleteShelvesUseCase.java
        QueryShelfUseCase.java
    ports/
      inbound/
        ShelfFacade.java                  # Inbound port (the ONLY way in)
      outbound/
        ShelfDomainRepository.java        # Outbound port for persistence
        BookAccessPort.java               # Outbound port for cross-context book data
  infrastructure/
    adapter/
      outbound/
        ShelfDomainRepositoryImpl.java    # JPA adapter
    entity/
      ShelfEntity.java                    # JPA entity (never leaves this package)
    mapping/
      ShelfMapper.java                    # Maps ONLY domain <-> entity
    repository/
      ShelfJpaRepository.java             # Spring Data interface
```

Key changes from current:

- `ShelfDTO.fromEntity()` removed; all entity mapping lives in `ShelfMapper`
- `ShelfMapper` only maps domain <-> entity (no DTO methods)
- `CreateShelfCommand` replaces primitive arguments on `ShelfFacade`
- `Shelf` is immutable (no setters)
- `ShelfDomainRepository` has one ID lookup method returning `Optional<Shelf>`
- Web-layer response mapping stays in `web/controllers/stacks/shelf/mappers/`

---

## 7. Three Refactor PRs (Ordered by Impact)

### PR 1: Seal the Shelf Domain Model

**Goal:** Make `Shelf` a proper aggregate root with enforced invariants and no setter escape hatches.

**Steps:**

1. Make all fields in `Shelf.java` `final`.
2. Remove all public setters (`setShelfLabel`, `setShelfPosition`, `setBookCapacity`, `setBookIds`, `setBooks`,
   `setBookcaseId`, `setShelfId`).
3. Fix the capacity validation message: "Book capacity must be at least 1".
4. Make `books` defensively copied in the constructor: `this.books = List.copyOf(books)`.
5. Add a static factory: `Shelf.create(String label, int position, int capacity, Long bookcaseId)` for new shelves (no
   ID, empty book list).
6. Add a static reconstitution factory:
   `Shelf.reconstitute(ShelfId id, String label, int position, int capacity, List<Long> books, Long bookcaseId)` for
   hydrating from persistence.
7. Update `ShelfMapper.toDomainFromEntity()` to use `Shelf.reconstitute()`.
8. Update `ShelfService.createShelf()` to use `Shelf.create()`.
9. Update `ShelfDomainRepositoryImpl.save()` to read fields via getters (already does this).
10. Run tests; fix any compilation errors from removed setters.

**Affected files:**

- `shelf/core/domain/model/Shelf.java`
- `shelf/infrastructure/mapping/ShelfMapper.java`
- `shelf/core/application/ShelfService.java`
- `shelf/infrastructure/adapter/outbound/ShelfDomainRepositoryImpl.java`
- `shelf/core/application/usecases/CreateShelfUseCaseTest.java`
- `shelf/core/application/ShelfServiceTest.java`

**Done when:**

- [ ] `Shelf` has no public setters
- [ ] All fields are `final`
- [ ] `Shelf.create()` and `Shelf.reconstitute()` are the only ways to construct a Shelf
- [ ] `books` is an unmodifiable list
- [ ] All existing tests pass
- [ ] New tests verify invariant enforcement (label blank, position 0, capacity 0, null bookcaseId)

---

### PR 2: Fix ShelfDTO Entity Dependency and ShelfAccessPort Cross-Context Leak

**Goal:** Eliminate the two highest-severity dependency direction violations.

**Steps:**

1. Remove `fromEntity()` and `fromEntityWithBookId()` from `ShelfDTO.java`. Remove the `ShelfEntity` import.
2. If those static methods are called anywhere, move the mapping to `ShelfMapper.toDTOFromEntity()` (which already
   exists).
3. In the Book module, create a `ShelfInfo` record in `book/core/port/outbound/` with fields:
   `Long shelfId, String label, Long bookcaseId, int position, int capacity, List<Long> books`.
4. Change `ShelfAccessPort.findShelfById()` to return `Optional<ShelfInfo>` instead of `Optional<ShelfDTO>`.
5. Update `ShelfAccessPortAdapter` to map `Shelf` -> `ShelfInfo` instead of `Shelf` -> `ShelfDTO`.
6. Update all callers of `ShelfAccessPort` in the Book module to use `ShelfInfo`.
7. Remove `ShelfMapper.toDTO()` and `ShelfMapper.toDTOFromEntity()` -- infrastructure mapper should only do domain <->
   entity.
8. Verify no remaining imports of `ShelfDTO` in `ShelfMapper`.

**Affected files:**

- `shelf/api/dtos/ShelfDTO.java`
- `shelf/infrastructure/mapping/ShelfMapper.java`
- `book/core/port/outbound/ShelfAccessPort.java`
- `book/infrastructure/adapter/outbound/ShelfAccessPortAdapter.java`
- Any Book module callers of `ShelfAccessPort`

**Done when:**

- [ ] `ShelfDTO` has zero imports from `infrastructure`
- [ ] `ShelfAccessPort` returns a type owned by the Book module, not Shelf
- [ ] `ShelfMapper` only maps domain <-> entity
- [ ] No compile errors; all tests pass

---

### PR 3: Route BookController Through ShelfFacade

**Goal:** Eliminate the concrete `ShelfService` dependency in `BookController` and remove inline DTO mapping.

**Steps:**

1. In `BookController`, change `ShelfService shelfService` to `ShelfFacade shelfFacade`.
2. The `placeBookOnShelf` method (lines 111-158) manually constructs a `ShelfDTO` inline (lines 131-138). Extract this
   to use `ShelfResponseMapper` or a dedicated mapper method.
3. If `BookController` needs methods not on `ShelfFacade`, add them to the facade interface first.
4. Verify `BookController` has zero imports from `shelf.core.application` and `shelf.infrastructure`.
5. Evaluate whether the `BookController.placeBookOnShelf()` orchestration (book assignment + shelf lookup + bookcase
   lookup) should be a dedicated use case in the Book module instead of controller-level logic.

**Affected files:**

- `web/controllers/cataloging/book/BookController.java`
- `shelf/core/ports/inbound/ShelfFacade.java` (if methods need adding)
- `shelf/core/application/ShelfService.java` (if methods need adding)

**Done when:**

- [ ] `BookController` depends only on `ShelfFacade` (not `ShelfService`)
- [ ] No inline `ShelfDTO` construction in controllers
- [ ] All REST endpoints still work as before
- [ ] No compile-time path from `web.controllers.cataloging` to `shelf.core.application`

---

## Architectural Tradeoffs Worth Naming

1. **ShelfFacade exposes domain types (`Shelf`, `ShelfSummary`) not DTOs.** This means any consumer of the facade gets a
   mutable domain object. This is a conscious tradeoff between simplicity (one mapping layer) and safety (leaking
   aggregate internals). PR 1 mitigates this by making `Shelf` immutable. An alternative is to have the facade return
   read-only projection types, but at current scale this would be over-engineering.

2. **`BookAccessPort.deleteBooksOnShelves()` is a command that crosses aggregate boundaries.** This is acceptable
   application-layer orchestration (in `DeleteShelvesUseCase`), but if the system grows, this should become an
   eventual-consistency pattern (domain event: "ShelvesDeleted" -> Book module reacts).

3. **`bookcaseId` on Shelf is a raw Long, not a value object.** Creating a `BookcaseId` value object improves type
   safety but adds ceremony. Worth doing if more than 2-3 methods pass bookcase IDs around (they do -- ShelfFacade,
   ShelfDomainRepository, DeleteShelvesUseCase all take `Long bookcaseId`).
