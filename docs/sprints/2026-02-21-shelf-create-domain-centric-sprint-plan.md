# EPIC + SPRINT PLAN: Domain-Centric CreateShelf Refactoring

---

## A) EPIC OVERVIEW

**Epic Name:** `SHELF-CREATE-DOMAIN-CENTRIC` — Eliminate transaction-script smell in CreateShelfUseCase

**Goal:**
Refactor the Shelf creation flow so that `CreateShelfUseCase` constructs a `Shelf` domain object (domain owns all
invariants), the outbound port accepts `Shelf` instead of primitives, a `CreateShelfCommand` record replaces raw
primitives at the inbound port boundary, and the command pattern flows all the way from `ShelfFacade` through to the use
case. `@Service` is kept for pragmatic consistency with the rest of the codebase, documented for future migration.

**Non-goals:**

- Removing `@Service` from use cases (follow-up ticket `SHELF-SERVICE-ANNOT`)
- Refactoring `QueryShelfUseCase` or `DeleteShelvesUseCase`
- Making `Shelf` domain model immutable (removing setters — follow-up `SHELF-DOMAIN-IMMUTABILITY`)
- Introducing CQRS, events, or new frameworks
- Changing `save()` return type to `Shelf` (follow-up `SHELF-SAVE-RETURN-ID`)

**Definition of Done:**

- `CreateShelfUseCase.execute()` accepts `CreateShelfCommand` and constructs a `Shelf` domain object — zero validation
  code in the use case
- `ShelfDomainRepository.save()` accepts `Shelf` — adapter handles persistence mapping via `ShelfMapper`
- `ShelfFacade.createShelf()` accepts `CreateShelfCommand` — command lives in `ports/inbound/`
- `Shelf` constructor enforces all invariants including `bookcaseId != null`
- All duplicated validation removed from `CreateShelfUseCase`
- `ShelfMapper.toEntity()` handles null `ShelfId` and maps `bookcaseId`
- All existing tests pass (updated); new domain invariant tests added
- `BookcaseService` updated to construct `CreateShelfCommand`

---

## B) SPRINT PLAN (6 Work Items)

### WI-1: Introduce `CreateShelfCommand` record in `ports/inbound/`

**Why:** Eliminates 4-primitive parameter list, makes the inbound port contract explicit, and provides an intentional
public API for the Shelf module.

**Scope:** New file: `shelf/core/ports/inbound/CreateShelfCommand.java`

**Steps:**

- [ ] Create `CreateShelfCommand.java` in `shelf/core/ports/inbound/`
- [ ] Define as Java record with fields: `Long bookcaseId`, `int position`, `String shelfLabel`, `int bookCapacity`
- [ ] No validation in the record — pure data carrier

```java
package com.penrose.bibby.library.stacks.shelf.core.ports.inbound;

public record CreateShelfCommand(
    Long bookcaseId,
    int position,
    String shelfLabel,
    int bookCapacity
) {}
```

**Acceptance Criteria:**

- Record compiles with 4 fields
- No validation logic, no Spring annotations
- Located in `core/ports/inbound/` alongside `ShelfFacade`

**Risk & Mitigation:**

- Purely additive — nothing depends on it yet; zero risk

**Effort:** S

---

### WI-2: Add `bookcaseId` null check to `Shelf` domain constructor

**Why:** The use case currently validates `bookcaseId != null`, but the domain doesn't. When we remove use-case
validation, this invariant would be lost. A shelf without a bookcase is nonsensical in the domain.

**Scope:** `shelf/core/domain/model/Shelf.java` — constructor only

**Steps:**

- [ ] Add null check before existing validation in `Shelf` constructor:
  ```java
  if (bookcaseId == null) {
      throw new IllegalArgumentException("Bookcase ID cannot be null");
  }
  ```

**Acceptance Criteria:**

- `new Shelf("A", 1, 10, null, List.of(), null)` throws `IllegalArgumentException("Bookcase ID cannot be null")`
- All existing tests still pass (no existing test creates a Shelf with null bookcaseId on the valid paths)

**Risk & Mitigation:**

- Risk: `ShelfMapper.toDomainFromEntity()` could pass null `bookcaseId` if DB has null values. Mitigated:
  `ShelfEntity.bookcaseId` is set via `BookcaseService` and is never null in practice. If paranoid, check DB
  constraints.

**Effort:** S

---

### WI-3: Change `ShelfDomainRepository.save()` signature + update adapter + fix mapper

**Why:** The outbound port bypasses the domain entirely. Changing to `save(Shelf)` ensures the domain model is the
contract between application and infrastructure.

**Scope:**

- `ShelfDomainRepository.java` — change interface method
- `ShelfDomainRepositoryImpl.java` — update implementation
- `ShelfMapper.java` — fix `toEntity()` to handle null `ShelfId` and map `bookcaseId`

**Steps:**

- [ ] In `ShelfDomainRepository.java`, replace:
  ```java
  void save(Long bookcaseId, int position, String shelfLabel, int bookCapacity);
  ```
  with:
  ```java
  void save(Shelf shelf);
  ```

- [ ] In `ShelfMapper.toEntity()`, fix null `ShelfId` handling and add `bookcaseId`:
  ```java
  public ShelfEntity toEntity(Shelf shelf) {
      ShelfEntity entity = new ShelfEntity();
      if (shelf.getShelfId() != null) {
          entity.setShelfId(shelf.getShelfId().shelfId());
      }
      entity.setShelfLabel(shelf.getShelfLabel());
      entity.setShelfPosition(shelf.getShelfPosition());
      entity.setBookCapacity(shelf.getBookCapacity());
      entity.setBookcaseId(shelf.getBookcaseId());
      return entity;
  }
  ```

- [ ] In `ShelfDomainRepositoryImpl.save()`, replace implementation:
  ```java
  @Override
  public void save(Shelf shelf) {
      ShelfEntity entity = shelfMapper.toEntity(shelf);
      jpaRepository.save(entity);
      logger.info("Shelf created with ID: {} for bookcase: {}",
          entity.getShelfId(), shelf.getBookcaseId());
  }
  ```

**Acceptance Criteria:**

- Port signature is `void save(Shelf shelf)`
- Adapter uses `shelfMapper.toEntity(shelf)` — no manual entity construction
- `ShelfMapper.toEntity()` handles null `ShelfId` without NPE
- `ShelfMapper.toEntity()` maps `bookcaseId`

**Risk & Mitigation:**

- Risk: `shelfMapper.toEntity()` currently sets `shelfId` unconditionally (
  `entity.setShelfId(shelf.getShelfId().shelfId())`) — this NPEs if `shelfId` is null. Fix with the null guard.
- Risk: Breaking compile — this WI, WI-4, and WI-5 must be committed atomically since they change interdependent
  signatures.

**Effort:** M

---

### WI-4: Refactor `CreateShelfUseCase` to construct `Shelf` domain object

**Why:** This is the core fix. The use case currently duplicates domain validation and passes primitives. It should
construct a `Shelf` (which enforces invariants) and delegate persistence.

**Scope:** `CreateShelfUseCase.java` — rewrite `execute()` method

**Steps:**

- [ ] Change `execute` signature to `execute(CreateShelfCommand command)`
- [ ] Remove all 4 `if` validation blocks (lines 16-28)
- [ ] Construct `Shelf` domain object:
  ```java
  Shelf shelf = new Shelf(
      command.shelfLabel(),
      command.position(),
      command.bookCapacity(),
      null,           // shelfId — DB generates it
      List.of(),      // new shelf, no books
      command.bookcaseId()
  );
  ```
- [ ] Call `shelfDomainRepositoryPort.save(shelf)`
- [ ] Keep `@Service` annotation; add TODO comment:
  ```java
  @Service // TODO: SHELF-SERVICE-ANNOT — move wiring to @Configuration in infrastructure
  ```
- [ ] Update imports (remove unused ones, add `CreateShelfCommand`, `Shelf`, `List`)

**Final state of `CreateShelfUseCase.java`:**

```java
package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.CreateShelfCommand;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;

import java.util.List;

import org.springframework.stereotype.Service;

@Service // TODO: SHELF-SERVICE-ANNOT — move wiring to @Configuration in infrastructure
public class CreateShelfUseCase {

    private final ShelfDomainRepositoryPort shelfDomainRepositoryPort;

    public CreateShelfUseCase(ShelfDomainRepositoryPort shelfDomainRepositoryPort) {
        this.shelfDomainRepositoryPort = shelfDomainRepositoryPort;
    }

    public void execute(CreateShelfCommand command) {
        Shelf shelf = new Shelf(
                command.shelfLabel(),
                command.position(),
                command.bookCapacity(),
                null,
                List.of(),
                command.bookcaseId()
        );
        shelfDomainRepositoryPort.save(shelf);
    }
}
```

**Acceptance Criteria:**

- Zero validation logic in the use case
- `Shelf` constructor is the sole enforcer of invariants
- `execute()` accepts `CreateShelfCommand`
- Invalid inputs still throw `IllegalArgumentException` with identical messages

**Risk & Mitigation:**

- Risk: Behavior change for `bookcaseId = null` — previously threw from use case; now throws from domain (same message
  after WI-2). No observable difference to callers.
- Risk: `Shelf` constructor with `null` `ShelfId` and `List.of()` books — confirmed constructor accepts both without
  error.

**Effort:** M

---

### WI-5: Update `ShelfFacade`, `ShelfService`, and `BookcaseService` for `CreateShelfCommand`

**Why:** Pushes the command pattern to the module boundary. `ShelfFacade` is the Shelf module's public API — it should
speak in commands, not primitives. `BookcaseService` (the sole external caller) constructs the command.

**Scope:**

- `ShelfFacade.java` — change `createShelf` signature
- `ShelfService.java` — pass-through
- `BookcaseService.java` — construct `CreateShelfCommand`

**Steps:**

- [ ] In `ShelfFacade.java`:
  ```java
  import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.CreateShelfCommand;
  // ...
  void createShelf(CreateShelfCommand command);
  ```
- [ ] In `ShelfService.java`:
  ```java
  @Override
  public void createShelf(CreateShelfCommand command) {
      createShelfUseCase.execute(command);
  }
  ```
- [ ] In `BookcaseService.addShelf()` (line 123-124), replace:
  ```java
  shelfFacade.createShelf(
      bookcaseEntity.getBookcaseId(), position, "Shelf " + label, bookCapacity);
  ```
  with:
  ```java
  shelfFacade.createShelf(new CreateShelfCommand(
      bookcaseEntity.getBookcaseId(), position, "Shelf " + label, bookCapacity));
  ```
- [ ] Add `CreateShelfCommand` import to `BookcaseService.java`

**Acceptance Criteria:**

- `ShelfFacade.createShelf(CreateShelfCommand)` — single parameter
- `BookcaseService` compiles and constructs the command
- No changes to `BookcaseService.addShelf()` validation logic (it still validates position, label, capacity before
  calling the facade)

**Risk & Mitigation:**

- Cross-module import: `BookcaseService` now imports `CreateShelfCommand` from `shelf.core.ports.inbound`. This is the
  intended public API surface — `ports/inbound/` is designed for cross-module consumption.
- Risk of test breakage in `BookcaseServiceTest` — covered in WI-6.

**Effort:** M

---

### WI-6: Update all tests

**Why:** Tests must reflect new signatures and verify invariants are enforced by the domain, not the use case.

**Scope:**

- `CreateShelfUseCaseTest.java` — rewrite
- `ShelfServiceTest.java` — update `createShelf` delegation test
- `BookcaseServiceTest.java` — update `verify(shelfFacade).createShelf(...)` calls
- `ShelfTest.java` (NEW) — domain constructor invariant tests

**Steps:**

- [ ] **Create `ShelfTest.java`** in `src/test/java/.../shelf/core/domain/model/`:
  ```java
  @Test void constructor_shouldCreateValidShelf() { /* happy path */ }
  @Test void constructor_shouldRejectNullLabel() { /* IllegalArgumentException */ }
  @Test void constructor_shouldRejectBlankLabel() { /* " " */ }
  @Test void constructor_shouldRejectEmptyLabel() { /* "" */ }
  @Test void constructor_shouldRejectZeroPosition() { /* 0 */ }
  @Test void constructor_shouldRejectNegativePosition() { /* -1 */ }
  @Test void constructor_shouldRejectZeroCapacity() { /* 0 */ }
  @Test void constructor_shouldRejectNegativeCapacity() { /* -5 */ }
  @Test void constructor_shouldRejectNullBookcaseId() { /* null */ }
  @Test void constructor_shouldAcceptMinimumValidValues() { /* position=1, capacity=1 */ }
  ```

- [ ] **Rewrite `CreateShelfUseCaseTest.java`**:
    - Happy path: `execute(new CreateShelfCommand(100L, 1, "Shelf A", 10))` → `ArgumentCaptor<Shelf>` verifies domain
      object fields
    - Validation tests: same edge cases, same exception messages, use `CreateShelfCommand`, verify
      `save(any(Shelf.class))` is never called
    - Example:
      ```java
      @Test
      void execute_shouldConstructShelfAndSave() {
          CreateShelfCommand command = new CreateShelfCommand(100L, 1, "Shelf A", 10);
          createShelfUseCase.execute(command);
  
          ArgumentCaptor<Shelf> captor = ArgumentCaptor.forClass(Shelf.class);
          verify(shelfDomainRepositoryPort).save(captor.capture());
  
          Shelf saved = captor.getValue();
          assertThat(saved.getShelfLabel()).isEqualTo("Shelf A");
          assertThat(saved.getShelfPosition()).isEqualTo(1);
          assertThat(saved.getBookCapacity()).isEqualTo(10);
          assertThat(saved.getBookcaseId()).isEqualTo(100L);
          assertThat(saved.getShelfId()).isNull();
          assertThat(saved.getBookIds()).isEmpty();
      }
  
      @Test
      void execute_shouldRejectBlankLabel() {
          CreateShelfCommand command = new CreateShelfCommand(100L, 1, "", 10);
          assertThatThrownBy(() -> createShelfUseCase.execute(command))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Shelf label cannot be null or blank");
          verify(shelfDomainRepositoryPort, never()).save(any(Shelf.class));
      }
      ```

- [ ] **Update `ShelfServiceTest.java`**:
  ```java
  @Test
  void createShelf_shouldDelegateToCreateUseCase() {
      CreateShelfCommand command = new CreateShelfCommand(100L, 1, "Shelf A", 10);
      shelfService.createShelf(command);
      verify(createShelfUseCase).execute(command);
  }
  ```

- [ ] **Update `BookcaseServiceTest.java`**: Replace all
  `verify(shelfFacade).createShelf(eq(Long), eq(int), eq(String), eq(int))` with either:
    - `verify(shelfFacade).createShelf(any(CreateShelfCommand.class))` (simpler), or
    - `ArgumentCaptor<CreateShelfCommand>` to assert field values (more thorough)

- [ ] Run full test suite: `./gradlew test` (or `mvn test`)

**Acceptance Criteria:**

- All tests pass
- `ShelfTest` covers all 9 constructor invariant edge cases + happy path
- `CreateShelfUseCaseTest` uses `CreateShelfCommand` and `save(Shelf)` signature throughout
- `BookcaseServiceTest` passes without changes to test logic (only verify signatures)
- At least one test uses `ArgumentCaptor<Shelf>` to prove the domain object was correctly constructed

**Risk & Mitigation:**

- Risk: `BookcaseServiceTest` has ~9 verify calls for `createShelf` — update them all. Easy but tedious.
- Risk: `@InjectMocks` still works with `@Service`-annotated classes — confirmed, no issue.

**Effort:** M

---

## C) TECHNICAL DESIGN NOTES

### Proposed `CreateShelfCommand` record

```java
package com.penrose.bibby.library.stacks.shelf.core.ports.inbound;

public record CreateShelfCommand(
    Long bookcaseId,
    int position,
    String shelfLabel,
    int bookCapacity
) {}
```

Located in `core/ports/inbound/` — it's part of the Shelf module's public API, consumed by `ShelfFacade` and
`BookcaseService`.

### Proposed `ShelfDomainRepository` interface (changed method)

```java
package com.penrose.bibby.library.stacks.shelf.core.ports.outbound;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;

import java.util.List;

public interface ShelfDomainRepository {

    Shelf getById(ShelfId id);

    void save(Shelf shelf);  // CHANGED: was (Long, int, String, int)

    void deleteByBookcaseId(Long bookcaseId);

    List<Shelf> findByBookcaseId(Long bookcaseId);

    Shelf findById(Long shelfId);

    List<Shelf> findAll();
}
```

### Proposed `CreateShelfUseCase` (refactored)

```java
package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.CreateShelfCommand;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;

import java.util.List;

import org.springframework.stereotype.Service;

@Service // TODO: SHELF-SERVICE-ANNOT — move wiring to @Configuration in infrastructure
public class CreateShelfUseCase {

    private final ShelfDomainRepositoryPort shelfDomainRepositoryPort;

    public CreateShelfUseCase(ShelfDomainRepositoryPort shelfDomainRepositoryPort) {
        this.shelfDomainRepositoryPort = shelfDomainRepositoryPort;
    }

    public void execute(CreateShelfCommand command) {
        Shelf shelf = new Shelf(
                command.shelfLabel(),
                command.position(),
                command.bookCapacity(),
                null,
                List.of(),
                command.bookcaseId()
        );
        shelfDomainRepositoryPort.save(shelf);
    }
}
```

No validation. Domain constructor does all the work.

### Proposed `ShelfDomainRepositoryImpl.save()` (refactored)

```java
@Override
public void save(Shelf shelf) {
    ShelfEntity entity = shelfMapper.toEntity(shelf);
    jpaRepository.save(entity);
    logger.info("Shelf created with ID: {} for bookcase: {}",
        entity.getShelfId(), shelf.getBookcaseId());
}
```

### Proposed `ShelfMapper.toEntity()` (fixed)

```java
public ShelfEntity toEntity(Shelf shelf) {
    ShelfEntity entity = new ShelfEntity();
    if (shelf.getShelfId() != null) {
        entity.setShelfId(shelf.getShelfId().shelfId());
    }
    entity.setShelfLabel(shelf.getShelfLabel());
    entity.setShelfPosition(shelf.getShelfPosition());
    entity.setBookCapacity(shelf.getBookCapacity());
    entity.setBookcaseId(shelf.getBookcaseId());
    return entity;
}
```

Note the null-guard on `shelfId` — for new shelves, `shelfId` is `null` and `shelf.getShelfId().shelfId()` would NPE.

### Where object construction happens and why

| Concern               | Where                                                              | Rationale                                                 |
|-----------------------|--------------------------------------------------------------------|-----------------------------------------------------------|
| `CreateShelfCommand`  | `BookcaseService` (cross-module caller) or `ShelfService` (facade) | Bridges external primitives → application command         |
| `Shelf` domain object | `CreateShelfUseCase`                                               | Use case is responsible for orchestrating domain creation |
| `ShelfEntity`         | `ShelfMapper` (infrastructure)                                     | Persistence mapping is an infrastructure concern          |

### How invariants are enforced / what code is removed

**Removed from `CreateShelfUseCase`:** All 4 `if` blocks (lines 16-28 of current file)

**Added to `Shelf` constructor:** `bookcaseId` null check:

```java
if (bookcaseId == null) {
    throw new IllegalArgumentException("Bookcase ID cannot be null");
}
```

**Already in `Shelf` constructor (unchanged):**

- `shelfLabel == null || shelfLabel.isBlank()` → `IllegalArgumentException`
- `shelfPosition < 1` → `IllegalArgumentException`
- `bookCapacity < 1` → `IllegalArgumentException`

### Wiring strategy

`@Service` is kept on `CreateShelfUseCase` for pragmatic consistency with the rest of the codebase. A
`TODO: SHELF-SERVICE-ANNOT` comment is added to track the follow-up migration to `@Configuration`/`@Bean` wiring.

### Migration strategy for `save()` signature change

**Impact assessment (confirmed via grep):**

- `ShelfDomainRepository.save()` is called from exactly **1 place**: `CreateShelfUseCase.java:29`
- No other use case, service, or adapter calls `save()` on the port
- The JPA `jpaRepository.save(entity)` call inside the impl is a different method (JPA's own `save`)

**Migration:** Change all 3 locations atomically in one commit:

1. `ShelfDomainRepository.java` — interface
2. `ShelfDomainRepositoryImpl.java` — implementation
3. `CreateShelfUseCase.java` — caller

**No follow-up migration needed.** The `ShelfFacade.createShelf(CreateShelfCommand)` change also happens in this sprint.

---

## D) TEST PLAN

### Unit tests to add/modify

| Test class                          | Action                  | What changes                                                                                                                                                    |
|-------------------------------------|-------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `CreateShelfUseCaseTest`            | **Rewrite**             | Use `CreateShelfCommand`; verify `save(any(Shelf.class))`; use `ArgumentCaptor<Shelf>` for happy path; validation tests still assert `IllegalArgumentException` |
| `ShelfServiceTest`                  | **Minor update**        | `createShelf_shouldDelegateToCreateUseCase`: verify `createShelfUseCase.execute(any(CreateShelfCommand.class))`                                                 |
| `BookcaseServiceTest`               | **Update verify calls** | All `verify(shelfFacade).createShelf(...)` calls updated for new signature                                                                                      |
| `ShelfTest` (NEW)                   | **Create**              | Constructor validation: null label, blank label, position 0, capacity 0, null bookcaseId; happy path construction                                               |
| `ShelfMapperTest` (NEW or existing) | **Add**                 | `toEntity()` with null `ShelfId` should not NPE; should map `bookcaseId`                                                                                        |

### What to delete

Nothing needs to be deleted entirely. The validation tests in `CreateShelfUseCaseTest` are **kept but reframed** — they
still test the same edge cases, but through the use case (which now delegates to the domain constructor). The tests
prove the use case rejects invalid input; they just don't prove it's the use case doing the rejecting (that's the
point).

### Edge cases to cover

| Edge case                                    | Where tested                          | Expected behavior                                                   |
|----------------------------------------------|---------------------------------------|---------------------------------------------------------------------|
| `shelfLabel = null`                          | `ShelfTest`, `CreateShelfUseCaseTest` | `IllegalArgumentException("Shelf label cannot be null or blank")`   |
| `shelfLabel = ""`                            | `ShelfTest`, `CreateShelfUseCaseTest` | Same                                                                |
| `shelfLabel = "   "`                         | `ShelfTest`, `CreateShelfUseCaseTest` | Same                                                                |
| `position = 0`                               | `ShelfTest`, `CreateShelfUseCaseTest` | `IllegalArgumentException("Shelf position must be greater than 0")` |
| `position = -1`                              | `ShelfTest`, `CreateShelfUseCaseTest` | Same                                                                |
| `bookCapacity = 0`                           | `ShelfTest`, `CreateShelfUseCaseTest` | `IllegalArgumentException("Book capacity cannot be negative")`      |
| `bookCapacity = -5`                          | `ShelfTest`, `CreateShelfUseCaseTest` | Same                                                                |
| `bookcaseId = null`                          | `ShelfTest`, `CreateShelfUseCaseTest` | `IllegalArgumentException("Bookcase ID cannot be null")`            |
| `bookCapacity = 1` (boundary)                | `ShelfTest`, `CreateShelfUseCaseTest` | Succeeds                                                            |
| `bookCapacity = 1000` (large)                | `CreateShelfUseCaseTest`              | Succeeds                                                            |
| `ShelfMapper.toEntity()` with null `ShelfId` | `ShelfMapperTest`                     | No NPE, `shelfId` not set on entity                                 |

### How to assert invariants are enforced via domain constructor

```java
@Test
void execute_shouldRejectBlankLabel_viaDomainConstructor() {
    CreateShelfCommand command = new CreateShelfCommand(100L, 1, "", 10);

    assertThatThrownBy(() -> createShelfUseCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf label cannot be null or blank");

    verify(shelfDomainRepositoryPort, never()).save(any(Shelf.class));
}
```

The key insight: the use case has no validation code, so the exception **must** come from the `Shelf` constructor. The
`ShelfTest` unit tests independently verify the constructor throws for each case, providing double coverage.

---

## E) DIFF GUIDANCE (REPO-AWARE CHECKLIST)

| #  | File                                | Path                                                                       | What to verify in review                                                                            |
|----|-------------------------------------|----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| 1  | `CreateShelfCommand.java` **(NEW)** | `.../shelf/core/ports/inbound/CreateShelfCommand.java`                     | Record, 4 fields, no logic, no annotations                                                          |
| 2  | `Shelf.java`                        | `.../shelf/core/domain/model/Shelf.java`                                   | `bookcaseId != null` check added in constructor                                                     |
| 3  | `ShelfDomainRepository.java`        | `.../shelf/core/ports/outbound/ShelfDomainRepository.java`                 | `save(Shelf shelf)` — no primitives                                                                 |
| 4  | `ShelfMapper.java`                  | `.../shelf/infrastructure/mapping/ShelfMapper.java`                        | `toEntity()`: null-guard on `getShelfId()`, `setBookcaseId()` added                                 |
| 5  | `ShelfDomainRepositoryImpl.java`    | `.../shelf/infrastructure/adapter/outbound/ShelfDomainRepositoryImpl.java` | `save(Shelf)` uses `shelfMapper.toEntity()`                                                         |
| 6  | `CreateShelfUseCase.java`           | `.../shelf/core/application/usecases/CreateShelfUseCase.java`              | All `if` blocks gone; constructs `Shelf`; `execute(CreateShelfCommand)`; keeps `@Service` with TODO |
| 7  | `ShelfFacade.java`                  | `.../shelf/core/ports/inbound/ShelfFacade.java`                            | `createShelf(CreateShelfCommand)`                                                                   |
| 8  | `ShelfService.java`                 | `.../shelf/core/application/ShelfService.java`                             | `createShelf(CreateShelfCommand)` pass-through                                                      |
| 9  | `BookcaseService.java`              | `.../bookcase/core/application/BookcaseService.java`                       | Constructs `CreateShelfCommand` at line ~123; import added                                          |
| 10 | `ShelfTest.java` **(NEW)**          | `src/test/.../shelf/core/domain/model/ShelfTest.java`                      | 10 constructor tests                                                                                |
| 11 | `CreateShelfUseCaseTest.java`       | `src/test/.../shelf/core/application/usecases/CreateShelfUseCaseTest.java` | Full rewrite with `CreateShelfCommand`, `ArgumentCaptor<Shelf>`                                     |
| 12 | `ShelfServiceTest.java`             | `src/test/.../shelf/core/application/ShelfServiceTest.java`                | 1 test updated                                                                                      |
| 13 | `BookcaseServiceTest.java`          | `src/test/.../bookcase/core/application/BookcaseServiceTest.java`          | ~9 verify calls updated                                                                             |

**Files that should NOT change:**

- `QueryShelfUseCase.java`, `DeleteShelvesUseCase.java` — out of scope
- `ShelfController.java` — doesn't call createShelf
- `ShelfEntity.java` — no changes needed
- `ShelfJpaRepository.java` — no changes needed
- `BookAccessPort.java` — unrelated

---

## Follow-up Tickets

1. **`SHELF-SERVICE-ANNOT`**: Remove `@Service` from all use cases + `ShelfService`; wire via `@Configuration` in
   infrastructure
2. **`SHELF-DOMAIN-IMMUTABILITY`**: Remove public setters from `Shelf`; add factory/builder for reconstitution
3. **`SHELF-SAVE-RETURN-ID`**: Change `save(Shelf)` to return `Shelf` (or `ShelfId`) so callers get the generated ID
4. **`SHELF-BOOKCASE-DDD`**: Apply same pattern to `BookcaseService` (currently has no use case extraction)

---

## PR Description

```markdown
## Shelf: Domain-centric CreateShelf refactoring

### Problem
`CreateShelfUseCase` was acting as a transaction script:
- Validated inputs with rules **duplicated** from `Shelf` domain constructor
- Passed raw primitives through `ShelfDomainRepository.save(Long, int, String, int)`
- Adapter constructed `ShelfEntity` directly, **bypassing the domain model**
- `ShelfFacade` inbound port also used primitives instead of an intentional command

### Changes
- **`CreateShelfCommand` record** — replaces 4-primitive parameter list at the
  module boundary (`ports/inbound/`); used by `ShelfFacade`, `ShelfService`,
  `CreateShelfUseCase`, and `BookcaseService`
- **`CreateShelfUseCase` refactored** — constructs a `Shelf` domain object
  (domain enforces all invariants); all duplicated validation removed
- **`ShelfDomainRepository.save(Shelf)`** — outbound port now accepts domain
  object; persistence mapping handled by `ShelfMapper` in the adapter
- **`ShelfMapper.toEntity()` fixed** — handles null `ShelfId` (new entities),
  maps `bookcaseId` (was missing)
- **`Shelf` constructor** — added `bookcaseId != null` invariant (was only
  checked in use case, not domain)
- **`ShelfFacade` + `ShelfService`** — accept `CreateShelfCommand`
- **`BookcaseService`** — constructs `CreateShelfCommand` when calling facade
- **Tests**: `ShelfTest` (new, domain invariants), `CreateShelfUseCaseTest`
  (rewritten), `ShelfServiceTest` + `BookcaseServiceTest` (updated)

### Architecture pattern established
```

BookcaseService
-> ShelfFacade.createShelf(CreateShelfCommand)     [inbound port]
-> ShelfService                                   [facade impl]
-> CreateShelfUseCase.execute(CreateShelfCommand)
-> new Shelf(...)                             [domain validates]
-> ShelfDomainRepository.save(Shelf)          [outbound port]
-> ShelfMapper.toEntity(Shelf) -> JPA save  [adapter]

```

### What's NOT changed
- `@Service` kept on `CreateShelfUseCase` for codebase consistency (TODO added)
- `QueryShelfUseCase`, `DeleteShelvesUseCase` unchanged
- `ShelfController` unchanged (read-only endpoints)

### Testing
- All 30+ existing tests updated and passing
- New `ShelfTest` with 10 constructor invariant tests
- `BookcaseServiceTest` passes with updated verify signatures
```
