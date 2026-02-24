# Devlog: Bookcase Domain Model Enrichment, Use Case Extraction, and Repository Boundary Cleanup

Date: 2026-02-23
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

## Summary

**What problem was I solving?**

The `Bookcase` domain model was anemic and infrastructure-tainted. It had only two fields (`bookcaseId`, `shelfCapacity`) while critical attributes like `bookcaseZone`, `bookcaseIndex`, and `bookCapacity` existed only on `BookcaseEntity` — an infrastructure (`@Entity`) class. The outbound port `BookcaseRepository` was defined in terms of `BookcaseEntity` and `Optional<BookcaseEntity>`, dragging infrastructure vocabulary into the core layer. The inbound port `BookcaseFacade` returned `Optional<BookcaseDTO>` and `List<BookcaseDTO>`, coupling every consumer to the presentation/API layer rather than the domain. `BookcaseService` carried all business logic inline — creation, deletion, and every query — violating single-responsibility and making it untestable in isolation.

**What was the "before" state?**

- `Bookcase` was a 2-field, 2-arg class. Zone, index, and book capacity were stranded on `BookcaseEntity`.
- `BookcaseRepository` (outbound port): all methods returned `BookcaseEntity` or `Optional<BookcaseEntity>` — the JPA entity leaked directly into the domain.
- `BookcaseFacade` (inbound port): returned `Optional<BookcaseDTO>` and `List<BookcaseDTO>` — forcing every CLI command and controller to call `.get()` on an Optional holding a DTO.
- `BookcaseService`: a 140-line monolith injecting `BookcaseRepository` and `ShelfAccessPort` directly, doing creation, shelf provisioning, manual DTO projection (inline `new BookcaseDTO(...)` with six fields copied out by hand), `@Transactional` deletion, and all queries.
- No dedicated use-case classes.
- Tests were brittle: `BookcaseServiceTest` mocked the repository directly (bypassing the use-case boundary); `Bookcase` objects were constructed with only `(id, shelfCapacity)`.

**Most important outcomes:**

- `Bookcase` is now a complete domain value carrier: `bookcaseId`, `shelfCapacity`, `bookCapacity`, `bookcaseLocation`, `bookcaseZone`, `bookcaseIndex`.
- `BookcaseRepository` port is clean: all methods speak `Bookcase`; `save()` is void; `findById()` returns nullable `Bookcase`.
- `BookcaseFacade` port returns `Bookcase` / `List<Bookcase>` — no DTOs cross the port.
- `BookcaseService` is a 60-line pure delegator.
- Three focused use cases: `CreateBookcaseUseCase`, `DeleteBookcaseUseCase`, `QueryBookcaseUseCase`.
- `BookcaseMapper` centralizes all `BookcaseEntity ↔ Bookcase ↔ BookcaseDTO` translation.
- 141 tests pass. Three new use-case test classes added.

---

## Per-Commit Breakdown

---

### Commit 1 — `260bd34`
**Add detailed Javadoc comments for BookcaseFacade methods**

**Intent:** Preparatory documentation pass on `BookcaseFacade` before its signature is changed.

**Files touched:**
- `BookcaseFacade.java` — Javadoc added to all 8 interface methods.

**Key code changes:**
- All `BookcaseFacade` methods get `@param` / `@return` Javadoc. The Javadoc describes the old return types (`Optional<BookcaseDTO>`, `List<BookcaseDTO>`, `Optional<BookcaseEntity>`) — these become inaccurate by commit 4. That mismatch is a known follow-up.

**Architecture notes:**
- No behavioral change. Documentation-only.

**Risk / edge cases:**
- The Javadoc comments on `findBookCaseById` and `findById` document the old `Optional`-returning contracts. Subsequent commits change the signatures but leave some of those comments partially stale (they reference `Optional` and `BookcaseEntity` in the prose).

**Verification:**
```
mvn -q -DskipTests compile
```

---

### Commit 2 — `4065480`
**Enrich Bookcase domain model and add BookcaseMapper**

**Intent:** Promote the domain model to carry the full set of bookcase attributes, and introduce a single place for all type translations.

**Files touched:**
- `Bookcase.java` — Added 3 fields + replaced 2-arg constructor with 6-arg + added getters/setters.
- `BookcaseMapper.java` — New class with `toDomain()`, `toEntity()`, `toDTO()`.

**Key code changes:**

`Bookcase` before:
```java
public Bookcase(Long bookcaseId, int shelfCapacity) { ... }
```

`Bookcase` after:
```java
public Bookcase(Long bookcaseId, int shelfCapacity, int bookCapacity,
                String bookcaseLocation, String bookcaseZone, String bookcaseIndex) { ... }
```

`setShelfCapacity()` applies a guard: values `< 1` are clamped to `1`. This guard is inherited from the old model.

`BookcaseMapper`:
```java
public static Bookcase toDomain(BookcaseEntity e)   // entity → domain
public static BookcaseEntity toEntity(Bookcase b)    // domain → entity (for persistence)
public static BookcaseDTO toDTO(Bookcase b)          // domain → DTO (for presentation)
```

**Architecture notes:**
- `BookcaseMapper` lives in `core.domain` — but it imports `BookcaseEntity` (from `infrastructure.entity`) and `BookcaseDTO` (from `api.dtos`). **This is a layering concern.** The domain layer now has a compile-time dependency on both infrastructure and API. See _Suggested Micro-Fixes_ below.
- The alternative (moving the mapper to an application or infrastructure layer) would be cleaner long-term.

**Risk / edge cases:**
- `toEntity()` maps only 4 of 6 fields: it sets `bookcaseId`, `shelfCapacity`, `bookCapacity`, `bookcaseLocation` — but **does NOT map `bookcaseZone` or `bookcaseIndex`**. This means any `save()` call silently loses zone and index data. **This is a data-loss bug** in the persistence path.
- `toDomain()` and `toDTO()` map all 6 fields correctly.

**Verification:**
```
mvn -q -DskipTests compile
mvn test -Dtest=BookcaseTest
```

---

### Commit 3 — `3cd7d48`
**Migrate BookcaseRepository port and impl to Bookcase domain model**

**Intent:** Purge `BookcaseEntity` from the outbound port so the core layer no longer depends on the JPA entity type.

**Files touched:**
- `BookcaseRepository.java` (outbound port) — All method signatures changed.
- `BookcaseRepositoryImpl.java` (infrastructure adapter) — Implements new signatures using `BookcaseMapper`.

**Key code changes:**

Port changes (every method signature):
```
findBookcaseEntityByBookcaseLocation(String) → findBookcaseByBookcaseLocation(String): Bookcase
save(BookcaseEntity): BookcaseEntity       → save(Bookcase): void
findById(Long): Optional<BookcaseEntity>  → findById(Long): Bookcase (null on miss)
findAll(): List<BookcaseEntity>           → findAll(): List<Bookcase>
findByLocation(String): List<...Entity>  → findByLocation(String): List<Bookcase>
findByAppUserId(Long): List<...Entity>   → findByAppUserId(Long): List<Bookcase>
```

Impl for `findById`:
```java
return Optional.ofNullable(bookcaseJpaRepository.findById(id).orElse(null))
    .map(BookcaseMapper::toDomain)
    .orElse(null);
```
This is verbose; `bookcaseJpaRepository.findById(id).map(BookcaseMapper::toDomain).orElse(null)` is equivalent and cleaner.

**Architecture notes:**
- Excellent: `BookcaseRepository` (an outbound port in the core layer) no longer imports `BookcaseEntity`. The core is now isolated from the JPA infrastructure.
- `BookcaseRepositoryImpl` (infrastructure layer) still imports `BookcaseMapper` from `core.domain`. That's acceptable — infra can depend on core.
- Note: a stray `import com.penrose.bibby.library.cataloging.book.core.domain.model.Book;` appeared in `BookcaseRepositoryImpl` and was removed in commit 7. This was an accidental cross-module import.

**Risk / edge cases:**
- All callers of `findById()` that previously pattern-matched on `Optional.isPresent()` now get a raw `Bookcase` that may be `null`. No null-guards have been added to any CLI call site. An NPE will occur if `findBookCaseById()` is called for a non-existent bookcase (e.g., in `BookCirculationCommands.findBookCaseById(shelf.get().getBookcaseId())`).
- `save()` returns void; the bookcase ID is populated via mutation of the domain object by the JPA `save()` (relying on the auto-generated ID flowing back through `BookcaseMapper.toEntity()` and being set on the original `Bookcase`). **However**, because `toEntity()` creates a new `BookcaseEntity` object rather than mutating the passed `Bookcase`, the generated ID is NOT written back to the `Bookcase` instance. This means `bookcase.getBookcaseId()` after `bookcaseRepository.save(bookcase)` may remain `null`. (See `CreateBookcaseUseCase` for context on how this is handled.)

**Verification:**
```
mvn -q -DskipTests compile
mvn test -Dtest=QueryBookcaseUseCaseTest
```

---

### Commit 4 — `f662174`
**Migrate BookcaseFacade to Bookcase type and extract use cases from BookcaseService**

**Intent:** Remove DTOs from the inbound port contract. Decompose the monolithic `BookcaseService` into three single-purpose use cases.

**Files touched:**
- `BookcaseFacade.java` (inbound port) — Return types changed.
- `CreateBookcaseUseCase.java` — New `@Service`.
- `DeleteBookcaseUseCase.java` — New `@Service`.
- `QueryBookcaseUseCase.java` — New `@Service`.
- `BookcaseService.java` — Gutted to a thin delegator.

**Key code changes:**

`BookcaseFacade` method signatures:
```
findBookCaseById(Long): Optional<BookcaseDTO>  → Bookcase
findById(Long): Optional<BookcaseEntity>       → Bookcase
getAllBookcases(): List<BookcaseDTO>           → List<Bookcase>
getAllBookcasesByLocation(String): List<...>   → List<Bookcase>
getAllBookcasesByUserId(Long): List<...>       → List<Bookcase>
```

`BookcaseService` went from ~140 lines (with inline DTO construction, logging, error handling, shelf provisioning, `@Transactional`) to ~60 lines of pure delegation:
```java
@Override public Bookcase findBookCaseById(Long id) {
    return queryBookcaseUseCase.findBookCaseById(id);
}
```

`CreateBookcaseUseCase.createNewBookCase()` holds all the creation logic:
1. Duplicate-label guard (looks up by `label`, not `location` — see Risks).
2. Construct `Bookcase(null, shelfCapacity, bookCapacity * shelfCapacity, location, zone, index)`.
3. `bookcaseRepository.save(bookcase)` — void; ID is expected to be set by JPA.
4. Loop `i = 1..shelfCapacity`: `shelfAccessPort.createShelf(bookcase.getBookcaseId(), i, "Shelf " + i, bookCapacity)`.
5. Return `new CreateBookcaseResult(bookcase.getBookcaseId())`.

**Important:** Step 4 calls `bookcase.getBookcaseId()` post-`save()`. As noted in commit 3, `toEntity()` creates a fresh `BookcaseEntity` — its auto-generated `bookcaseId` is never written back to the `Bookcase` instance. In production, if `BookcaseRepositoryImpl.save()` also calls `entity.setBookcaseId(...)` on the input `Bookcase`, this works; otherwise, shelf creation will pass a `null` bookcase ID. The tests simulate this with `doAnswer` that explicitly calls `b.setBookcaseId(100L)`.

`DeleteBookcaseUseCase.deleteBookcase()` owns the `@Transactional` annotation and deletion order:
```java
@Transactional
public void deleteBookcase(Long bookcaseId) {
    shelfAccessPort.deleteAllShelvesInBookcase(bookcaseId);
    bookcaseRepository.deleteById(bookcaseId);
}
```

`QueryBookcaseUseCase` has two methods that do the same thing:
- `findBookCaseById(Long id)` → `bookcaseRepository.findById(id)`
- `findById(Long bookcaseId)` → `bookcaseRepository.findById(bookcaseId)`

Both exist only because `BookcaseFacade` has both method names (legacy). They are redundant; one can delegate to the other.

**Architecture notes:**
- `BookcaseService` no longer imports `BookcaseRepository`, `ShelfAccessPort`, `@Transactional`, `Logger`, or `ResponseStatusException`. Its dependency footprint is now just three use-case classes.
- Use cases (`CreateBookcaseUseCase`, `DeleteBookcaseUseCase`, `QueryBookcaseUseCase`) are `@Service` beans in the `core.application.usecases` package. They depend on outbound ports only (`BookcaseRepository`, `ShelfAccessPort`) — correct direction.
- The inbound port `BookcaseFacade` is now fully free of infrastructure types (`BookcaseEntity`, `BookcaseDTO`). Clean.

**Risk / edge cases:**
- `CreateBookcaseUseCase` calls `findBookcaseByBookcaseLocation(label)` — the duplicate check uses the `label` param, not `location`. Label is used as a logical identifier; location is a physical place. If two bookcases can share a location but have different labels, this is intentional. If the intent was to check by physical location, this is a latent bug.
- `createNewBookCase()` stores `bookCapacity * shelfCapacity` as `bookCapacity` in the domain object but the parameter `bookCapacity` is per-shelf. This means `Bookcase.bookCapacity` is the total capacity, not per-shelf capacity. This semantic divergence from the field name is undocumented.

**Verification:**
```
mvn test -Dtest=BookcaseServiceTest,CreateBookcaseUseCaseTest,DeleteBookcaseUseCaseTest,QueryBookcaseUseCaseTest
```

---

### Commit 5 — `8b6fcec`
**Update CLI commands and web controller for Bookcase domain type**

**Intent:** Adapt all consumers of `BookcaseFacade` to the new return types. Map to `BookcaseDTO` at the consumer boundary where a DTO is needed.

**Files touched:**
- `BookCirculationCommands.java` — 2 call sites updated.
- `BookCreateImportCommands.java` — 2 call sites updated.
- `BookCreateIsbnCommands.java` — 2 call sites updated.
- `BookSearchCommands.java` — 4 call sites updated.
- `LibraryCommands.java` — 2 call sites updated.
- `PromptOptions.java` — 3 call sites updated.
- `BookCaseController.java` — 2 endpoints updated.
- `Shelf.java` — Whitespace-only (blank line added/removed).

**Key code changes:**

Pattern 1 — direct attribute access (no DTO needed):
```java
// Before:
bookcaseFacade.findBookCaseById(bookcaseId).get().location()
// After:
bookcaseFacade.findBookCaseById(bookcaseId).getBookcaseLocation()
```

Pattern 2 — DTO needed at call site:
```java
// Before:
Optional<BookcaseDTO> bookcaseDTO = bookcaseFacade.findBookCaseById(...);
bookcaseLocation = bookcaseDTO.get().location();
// After:
BookcaseDTO bookcaseDTO = BookcaseMapper.toDTO(bookcaseFacade.findBookCaseById(...));
bookcaseLocation = bookcaseDTO.location();
```

Pattern 3 — list with DTO conversion:
```java
// Before:
List<BookcaseDTO> bookcaseDTOs = bookcaseFacade.getAllBookcases();
// After:
List<BookcaseDTO> bookcaseDTOs = bookcaseFacade.getAllBookcases()
    .stream().map(BookcaseMapper::toDTO).toList();
```

**Architecture notes:**
- This is the anti-corruption layer in action: the domain type crosses the facade boundary; consumers translate to their preferred representation at their own boundary.
- `BookCaseController` still works with `BookcaseDTO` on its response types, which is correct — the REST API contract doesn't need to change.
- CLI commands import `BookcaseMapper` from `core.domain` — CLI (a presentation/infrastructure concern) depending on a domain package is acceptable since `BookcaseMapper` is a utility on the domain type.

**Risk / edge cases:**
- All 5 CLI commands that call `bookcaseFacade.findBookCaseById(...)` dereference the result without null-checking. If the bookcase doesn't exist, an NPE will be thrown. This was previously "safe" because the callers used `.get()` on an Optional (which also threw an exception, but a more specific `NoSuchElementException`). Net behavior: still crashes, but the exception type changes.

**Verification:**
```
mvn test -Dtest=BookCreateIsbnCommandsTest,BookManagementCommandsTest
```

---

### Commit 6 — `a231962`
**Update tests for Bookcase domain model refactor and add use case tests**

**Intent:** Fix all broken test compilations and add dedicated unit tests for the new use cases.

**Files touched:**
- `BookcaseTest.java` — 3 constructor call fixes.
- `BookcaseServiceTest.java` — Major rewrite.
- `PromptOptionsTest.java` — Stub type changes.
- `CreateBookcaseUseCaseTest.java` — New (108 lines, 3 tests).
- `DeleteBookcaseUseCaseTest.java` — New (34 lines, 1 test).
- `QueryBookcaseUseCaseTest.java` — New (152 lines, 11 tests).

**Key code changes:**

`BookcaseTest` — updated constructor:
```java
new Bookcase(1L, 5)    → new Bookcase(1L, 5, 0, null, null, null)
new Bookcase(1L, 0)    → new Bookcase(1L, 0, 0, null, null, null)
new Bookcase(1L, -42)  → new Bookcase(1L, -42, 0, null, null, null)
```

`BookcaseServiceTest` — Optional removal: all `thenReturn(Optional.of(bookcase))` → `thenReturn(bookcase)`, `Optional.empty()` → `null`, `result.get().getBookcaseId()` → `result.getBookcaseId()`, `assertThat(result).isPresent()` → `assertThat(result).isNotNull()`.

`CreateBookcaseUseCaseTest` — uses `doAnswer` to simulate JPA ID generation:
```java
doAnswer(invocation -> {
    Bookcase b = invocation.getArgument(0);
    b.setBookcaseId(100L);
    return null;
}).when(bookcaseRepository).save(any(Bookcase.class));
```
This reveals the production assumption: `save()` must mutate the passed-in `Bookcase` object with its generated ID. That contract is not enforced by the type system.

`DeleteBookcaseUseCaseTest` — verifies ordering:
```java
InOrder inOrder = Mockito.inOrder(shelfAccessPort, bookcaseRepository);
inOrder.verify(shelfAccessPort).deleteAllShelvesInBookcase(bookcaseId);
inOrder.verify(bookcaseRepository).deleteById(bookcaseId);
```

`QueryBookcaseUseCaseTest` — covers: find by ID (found/not found), `findById` (duplicate of above), `getAllBookcases` (populated/empty), `getAllBookcaseLocations`, `getAllBookcasesByLocation` (populated/empty), `getAllBookcasesByUserId` (populated/empty).

**Architecture notes:**
- `CreateBookcaseUseCaseTest` imports `BookcaseEntity` was removed (correct: the test shouldn't know about entity types).
- The `doAnswer` workaround highlights that `BookcaseRepositoryImpl.save()` must call `bookcase.setBookcaseId(...)` after `jpaRepository.save()`. This is currently NOT in the production code — see Risks.

**Risk / edge cases:**
- The test's `doAnswer` is a test-only workaround. The production `BookcaseRepositoryImpl.save()` currently does NOT write the generated ID back to the `Bookcase` instance. In real execution, `bookcase.getBookcaseId()` will be `null` after `save()`, causing `shelfAccessPort.createShelf(null, ...)` to be called.

**Verification:**
```
mvn test -Dtest=BookcaseServiceTest,BookcaseTest,PromptOptionsTest,CreateBookcaseUseCaseTest,DeleteBookcaseUseCaseTest,QueryBookcaseUseCaseTest
```

---

### Commit 7 — `6f8646e`
**Refactor Bookcase-related commands and services for improved readability**

**Intent:** Polish pass — reformatting, removing dead imports, cleaning up formatting inconsistencies introduced by the earlier commits.

**Files touched (15 files, mostly formatting):**
- `BookCirculationCommands.java`, `BookSearchCommands.java` — Line-break formatting.
- `PromptOptions.java` — Multi-line stream formatting.
- `BookcaseService.java` — 3 lines removed (dead imports).
- `DeleteBookcaseUseCase.java` — 1 blank line removed.
- `QueryBookcaseUseCase.java` — Dead imports removed (`BookcaseEntity`, `Optional`).
- `BookcaseMapper.java` — Reformatted from 4-space indent (non-standard for project) to 2-space; unused `import java.util.List` removed.
- `Bookcase.java` — Constructor formatted multi-line; extra blank lines removed.
- `BookcaseRepository.java` — 2 dead imports removed (`BookcaseEntity`, `Optional`).
- `BookcaseRepositoryImpl.java` — Dead imports removed (`Book` from cataloging module, `BookcaseEntity`, `Optional`); stream chains formatted multi-line.
- `Shelf.java` — Blank lines removed (reverts commit 5's whitespace noise).
- `BookCaseController.java` — Multi-line stream formatting for both endpoints.
- `BookcaseServiceTest.java` — 3-line format fix.
- `CreateBookcaseUseCaseTest.java` — 29 lines restructured.
- `DeleteBookcaseUseCaseTest.java` — 2 lines removed.

**Key code changes:**
- `BookcaseRepositoryImpl` had `import com.penrose.bibby.library.cataloging.book.core.domain.model.Book` — a cross-module import from the `cataloging` domain into the `stacks.bookcase` infrastructure. Removed here. This was introduced in commit 3 and should never have been added.
- `BookcaseMapper` coding style normalized from 4-space (IntelliJ default) to 2-space (project standard).

**Architecture notes:**
- Clean-up only. No behavior changes. The stray `Book` import removal is the most architecturally meaningful fix in this commit.

**Verification:**
```
mvn test   # All 141 tests should pass
```

---

## End-to-End Flow: Create Bookcase Request

```
HTTP POST /bookcase  (or CLI createBookcase command)
  │
  ▼
BookCaseController.createBookcase(request, principal)
  │  calls BookcaseFacade.createNewBookCase(userId, label, zone, index, shelfCap, bookCap, location)
  │
  ▼
BookcaseService.createNewBookCase(...)           ← inbound port impl
  │  delegates to →
  ▼
CreateBookcaseUseCase.createNewBookCase(...)     ← use case
  │  1. bookcaseRepository.findBookcaseByBookcaseLocation(label)  → null or Bookcase
  │  2. new Bookcase(null, shelfCap, bookCap*shelfCap, location, zone, index)
  │  3. bookcaseRepository.save(bookcase)        ← outbound port
  │       └─ BookcaseRepositoryImpl.save(bookcase)
  │             └─ BookcaseMapper.toEntity(bookcase)  → BookcaseEntity
  │             └─ bookcaseJpaRepository.save(entity) → JPA/Hibernate → DB
  │             ⚠  ID NOT written back to bookcase object (see Risks)
  │  4. for i in 1..shelfCap:
  │       shelfAccessPort.createShelf(bookcase.getBookcaseId(), i, "Shelf "+i, bookCap)
  │  5. return new CreateBookcaseResult(bookcase.getBookcaseId())
  │
  ▼
CreateBookcaseResult(bookcaseId)  → ResponseEntity.ok(result)
```

---

## Why This Design Is Better

| Before | After |
|--------|-------|
| `BookcaseRepository` port returns `BookcaseEntity` (infra leak into core) | Port returns `Bookcase` (domain model only) |
| `BookcaseFacade` returns `Optional<BookcaseDTO>` (DTO in port contract) | Port returns `Bookcase` (domain model only) |
| Consumers call `.get()` on Optional<DTO> with no safety | Consumers deal with a plain domain object (still needs null-guard) |
| `BookcaseService` mixes creation logic, shelf provisioning, DTO projection, error handling, `@Transactional`, logging | `BookcaseService` is 60 lines; logic lives in focused use cases |
| Inline `new BookcaseDTO(entity.getX(), entity.getY(), ...)` in 3+ places | `BookcaseMapper.toDTO()` is the single translation point |
| `Bookcase` domain object had 2 fields, 2 constructors | `Bookcase` carries all 6 attributes; fully round-trippable |
| No dedicated test for creation, deletion, or query logic | `CreateBookcaseUseCaseTest`, `DeleteBookcaseUseCaseTest`, `QueryBookcaseUseCaseTest` |

---

## Dependency Direction Changes

**Removed dependencies from core:**
- `BookcaseService` no longer depends on `BookcaseRepository` or `ShelfAccessPort` directly.
- `BookcaseRepository` (port interface) no longer imports `BookcaseEntity` or `Optional`.
- `BookcaseFacade` no longer imports `BookcaseDTO`, `BookcaseEntity`, or `Optional`.

**New dependencies:**
- `BookcaseMapper` (in `core.domain`) imports `BookcaseEntity` and `BookcaseDTO` — a layering concern.
- CLI commands now import `BookcaseMapper` from `core.domain`.

---

## Layering Concerns (Imports that Violate Intended Boundaries)

| File | Violating Import | Concern |
|------|-----------------|---------|
| `BookcaseMapper` (`core.domain`) | `infrastructure.entity.BookcaseEntity` | Domain layer importing infrastructure |
| `BookcaseMapper` (`core.domain`) | `api.dtos.BookcaseDTO` | Domain layer importing API/presentation |
| CLI commands | `core.domain.BookcaseMapper` | Acceptable — presentation layer can depend on domain utilities |

The mapper placement is the most significant outstanding layering concern. `BookcaseMapper` belongs in the `infrastructure` layer (for `toEntity`/`toDomain`) and possibly the `api` or `application` layer (for `toDTO`), not in `core.domain`.

---

## Suggested Micro-Fixes

1. **Fix `BookcaseRepositoryImpl.save()` to write back the generated ID:**
   ```java
   public void save(Bookcase bookcase) {
       BookcaseEntity entity = BookcaseMapper.toEntity(bookcase);
       entity = bookcaseJpaRepository.save(entity);
       bookcase.setBookcaseId(entity.getBookcaseId());  // ← add this line
   }
   ```
   Without this, `CreateBookcaseUseCase` creates shelves with a `null` bookcase ID in production.

2. **Fix `BookcaseMapper.toEntity()` to map zone and index:**
   ```java
   bookcaseEntity.setBookcaseZone(bookcase.getBookcaseZone());
   bookcaseEntity.setBookcaseIndex(bookcase.getBookcaseIndex());
   ```
   Currently these two fields are silently lost on every `save()`.

3. **Simplify `BookcaseRepositoryImpl.findById()`:**
   ```java
   // Before (verbose):
   return Optional.ofNullable(bookcaseJpaRepository.findById(id).orElse(null))
       .map(BookcaseMapper::toDomain).orElse(null);
   // After (clean):
   return bookcaseJpaRepository.findById(id).map(BookcaseMapper::toDomain).orElse(null);
   ```

4. **Collapse `QueryBookcaseUseCase.findBookCaseById()` and `findById()` — they are identical.** One can delegate to the other, or both can be removed and callers unified on one name.

5. **Update stale Javadoc in `BookcaseFacade`:** The `findBookCaseById` Javadoc says "returns Optional containing BookcaseDTO" — it now returns `Bookcase` directly.

6. **Add null-guards in CLI commands** for `findBookCaseById()` call sites in `BookCirculationCommands`, `BookCreateImportCommands`, `BookCreateIsbnCommands`, `BookSearchCommands`, `LibraryCommands`.

7. **Move `BookcaseMapper` out of `core.domain`** to eliminate the layer violation. Options:
   - Split into two: `BookcaseEntityMapper` (infrastructure layer) and `BookcaseDtoMapper` (application or api layer).
   - Or accept a single mapper in `application` layer that knows both entity and DTO.

---

## Tests

### Tests That Exist

| Test Class | Tests | What It Covers |
|---|---|---|
| `BookcaseTest` | 3 | `Bookcase` constructor; `shelfCapacity` clamping guard |
| `BookcaseServiceTest` | 10 | `BookcaseService` delegation to all 3 use cases |
| `CreateBookcaseUseCaseTest` | 3 | Creation happy path, duplicate detection, correct shelf count |
| `DeleteBookcaseUseCaseTest` | 1 | Deletion ordering (shelves before bookcase) |
| `QueryBookcaseUseCaseTest` | 11 | All query paths: found, not found, empty lists |
| `PromptOptionsTest` | 7 | `bookCaseOptions()` formatting and ordering |

### Recommended Additional Tests

1. **`BookcaseRepositoryImplTest` (integration):** Verify that `save()` writes the generated ID back to the `Bookcase` object. This would immediately catch the production bug described above.

2. **`CreateBookcaseUseCaseTest` — shelf null ID test:** Assert that shelves are NOT created with a null bookcase ID. Currently the test works because `doAnswer` manually sets the ID; a test that verifies the production `save()` contract would be more valuable.

3. **`CreateBookcaseUseCaseTest` — label vs location:** Add a test that creates two bookcases with the same location but different labels to confirm whether the duplicate check is intentionally by label or by location.

4. **`BookcaseMapperTest`:** Unit test that `toEntity()` correctly maps all 6 fields (would immediately expose the `bookcaseZone`/`bookcaseIndex` omission bug).

5. **`BookCaseControllerTest`:** Integration test for `GET /bookcase/location/{location}` and `GET /bookcase/user` to confirm the `BookcaseMapper.toDTO()` projection is correct in the HTTP layer.

### Run Commands

```bash
# Full suite
mvn test

# Targeted use case tests only
mvn test -Dtest=CreateBookcaseUseCaseTest,DeleteBookcaseUseCaseTest,QueryBookcaseUseCaseTest

# Bookcase domain model tests
mvn test -Dtest=BookcaseTest,BookcaseServiceTest

# With verbose output for a specific class
mvn test -Dtest=CreateBookcaseUseCaseTest -pl . -e
```

---

## Lessons from This Diff

1. **Void `save()` is a hidden contract.** Switching from `BookcaseEntity save(BookcaseEntity)` to `void save(Bookcase)` moved the ID-back-write responsibility into the impl silently. The type system can't enforce it, and the production code currently doesn't do it.

2. **`Optional` elimination in ports is the right move, but null is not inherently safer.** Replacing `Optional<BookcaseDTO>` with `Bookcase` removed one layer of ceremony, but the callers now have a raw nullable instead of an expressive container. The real win is removing the DTO from the port — the null question is orthogonal.

3. **Use case extraction makes the right things testable.** `BookcaseServiceTest` went from testing business logic to testing delegation. `CreateBookcaseUseCaseTest` now owns the creation behavior. This is the right split.

4. **Mapper placement matters.** `BookcaseMapper` in `core.domain` feels convenient but creates a domain→infra import cycle. It's worth the small upfront cost to place it in a layer that is allowed to know both sides.

5. **Naming symmetry reveals semantic gaps.** Two methods (`findBookCaseById` and `findById`) with the same implementation on the same class signals an unresolved naming decision. The facade retains both for backward compatibility, but the use case shouldn't have two names for one concept.

6. **`toEntity()` field omissions are silent data corruption.** There's no compiler warning when you add a field to a domain object and forget to add it to the entity mapper. A mapper unit test is the only reliable guard.

7. **The `@Transactional` boundary moved to the right place.** Previously on `BookcaseService.deleteBookcase()`, it now lives on `DeleteBookcaseUseCase.deleteBookcase()`. The use case owns the unit of work — that's exactly correct.

---

## Follow-Up Actions

### Immediate (Today)
- [ ] Fix `BookcaseRepositoryImpl.save()` to write back the generated ID to the `Bookcase` instance — **production bug**, shelves get `null` bookcase ID.
- [ ] Fix `BookcaseMapper.toEntity()` to map `bookcaseZone` and `bookcaseIndex` — **data loss bug**.
- [ ] Add null-guards or `Optional.ofNullable()` wrappers to the 5+ CLI call sites that dereference `findBookCaseById()` result.

### Short-Term Hardening (This Week)
- [ ] Add `BookcaseMapperTest` to expose both mapper bugs above before they hit production.
- [ ] Add `BookcaseRepositoryImplTest` (or at least a focused integration test) for the ID write-back contract.
- [ ] Collapse `QueryBookcaseUseCase.findBookCaseById()` and `findById()` into one method; update `BookcaseService` and `BookcaseFacade`.
- [ ] Update `BookcaseFacade` Javadoc to reflect actual return types (not the stale `Optional<BookcaseDTO>` prose from commit 1).

### Strategic Refactors (Later)
- [ ] Move `BookcaseMapper` out of `core.domain` to eliminate the domain→infra and domain→api import violations. Consider splitting into `BookcaseEntityMapper` (infra layer) and `BookcaseDtoMapper` (application or presentation layer).
- [ ] Consider replacing the null-return pattern on `findById()` with `Optional<Bookcase>` on the port — this makes the "might not exist" contract explicit without leaking DTOs.
- [ ] Clarify the `label` vs `location` semantic in `CreateBookcaseUseCase`: the duplicate guard checks `label`; the bookcase stores `location`. Document or fix.
- [ ] Evaluate whether `BookcaseService` should be kept at all, or whether consumers can inject use cases directly once the facade interface is stable.
