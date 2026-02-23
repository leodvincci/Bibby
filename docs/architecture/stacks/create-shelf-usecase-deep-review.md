# CreateShelfUseCase - Architecture Deep Review

> **Reviewed**: 2026-02-21
> **Scope**: `shelf/core/application/usecases/CreateShelfUseCase.java`
> **Style**: Staff-engineer mentor review (DDD + Hexagonal)

---

## 1. Quick Mental Model: DDD + Hexagonal in Plain Language

### The Three Rings

Think of your application as three concentric rings.
The **only rule** is: arrows point inward. Nothing in an inner ring knows about an outer ring.

```
 ┌─────────────────────────────────────────────────┐
 │  INFRASTRUCTURE / ADAPTERS  (outermost)         │
 │  Spring @Controller, JPA @Entity, ShelfMapper,  │
 │  ShelfDomainRepositoryImpl                      │
 │                                                 │
 │   ┌─────────────────────────────────────────┐   │
 │   │  APPLICATION  (middle ring)             │   │
 │   │  Use cases, command objects,            │   │
 │   │  orchestration logic                    │   │
 │   │                                         │   │
 │   │   ┌─────────────────────────────────┐   │   │
 │   │   │  DOMAIN  (innermost)            │   │   │
 │   │   │  Shelf, ShelfId,               │   │   │
 │   │   │  invariants, business rules     │   │   │
 │   │   └─────────────────────────────────┘   │   │
 │   │                                         │   │
 │   └─────────────────────────────────────────┘   │
 │                                                 │
 └─────────────────────────────────────────────────┘

 Dependency direction:  Adapters ──▶ Application ──▶ Domain
                        (outer)                      (inner)
```

### Key Principles

| Principle | Meaning |
|-----------|---------|
| **Domain owns the rules** | If "book capacity must be > 0" is a business rule, it lives in `Shelf`, period. |
| **Ports are contracts owned by core** | `ShelfDomainRepository` is an interface in `core/ports`. Core *defines* what it needs; adapters *provide* it. The port speaks the domain's language, not the database's. |
| **Adapters are translators** | `ShelfDomainRepositoryImpl` translates between `Shelf` (domain) and `ShelfEntity` (JPA). It's plumbing. |
| **Use cases orchestrate** | A use case coordinates: validate preconditions the domain can't check (e.g., "does the bookcase exist?"), construct domain objects, call ports. It does **not** contain business rules. |

### Where Invariants Live

```
 Invariant: "shelfLabel cannot be blank"

 ✅  Shelf constructor       ← domain enforces it, always
 ✅  Controller / DTO        ← fast-fail for UX (optional, defense-in-depth)
 ❌  Use case                ← wrong place: duplicates domain, can drift
 ❌  Repository adapter      ← way too late, DB-level concern
```

**Why this matters**: If the Shelf constructor is the *single* enforcement point, you literally cannot create an invalid Shelf anywhere in the system. Not from a use case, not from a test, not from a future batch import, not from an event handler. That's the whole point.

---

## 2. Walkthrough of the Current Design

### What the Code Does Today

```
CreateShelfUseCase.execute(bookcaseId, position, shelfLabel, bookCapacity)
   │
   ├── validates all four params (if/throw x4)
   │
   └── shelfDomainRepositoryPort.save(bookcaseId, position, shelfLabel, bookCapacity)
          │
          └── ShelfDomainRepositoryImpl.save(...)
                 │
                 ├── new ShelfEntity()
                 ├── entity.setBookcaseId(bookcaseId)
                 ├── entity.setShelfPosition(position)
                 ├── ...
                 └── jpaRepository.save(entity)
```

Notice what's **missing** from this flow: `new Shelf(...)` is never called. The domain model is completely bypassed during creation.

### Why It Feels OK at First

This pattern *works*. It saves data. Tests pass. The shelf shows up in the database. You ship the feature in an afternoon. So what's the problem?

The problem is that you've written a **transaction script**.

### What Is a Transaction Script?

A transaction script is a procedural pattern where your use case is essentially:
1. Receive raw inputs
2. Validate them inline
3. Talk directly to the database (or a thin wrapper around it)

It's the default gravitational pull of CRUD apps. You think: "I just need to save some fields" — and you reach straight for the repo with primitives. There's no domain object in the middle because it feels like "extra ceremony."

**Why it creeps in**:
- CRUD operations feel too simple for domain objects
- The domain class exists but is only used on *reads*, never on *writes*
- The port was designed around "what the database needs" rather than "what the domain expresses"
- Deadlines. (Always deadlines.)

**The trap**: Transaction scripts work *great* for V1. Then requirements change. Now `Shelf` needs a derived field (`remainingCapacity`), or a new invariant ("a shelf in position 1 must have a label starting with 'A'"), or an event ("ShelfCreated"). Every one of those changes means hunting through *use cases* to find where shelves are born — rather than going to the single source of truth.

---

## 3. Issue-by-Issue Deep Dive

### Issue #1: Duplicated Validation Logic

**Current state**: `CreateShelfUseCase` lines 30-41 perform four validation checks. `Shelf` constructor lines 31-39 perform the *same* checks with the *same* error messages.

**Why this is harmful**:

1. **Drift risk**: Invariants will diverge over time. Someone adds a max-length check to `Shelf` but forgets the use case (or vice versa). Now you have two sources of truth that disagree.

2. **Maintenance cost**: Every new invariant requires changes in two places. In a codebase with multiple use cases (`UpdateShelfUseCase`, `ImportShelvesUseCase`), it multiplies further.

3. **False confidence**: Tests on the use case pass, but the domain object was never exercised. You *think* you have validation coverage. You don't — you have coverage of a copy.

**Fix**: Delete the validation from the use case. Construct the `Shelf` domain object instead. Let the constructor enforce invariants. The use case's job is to *orchestrate*, not to *gatekeep rules that the domain already owns*.

```java
// BEFORE (use case does the domain's job)
if (bookCapacity <= 0) throw ...
if (shelfLabel == null || shelfLabel.isBlank()) throw ...
// ...
repository.save(bookcaseId, position, shelfLabel, bookCapacity);

// AFTER (domain does its own job)
Shelf shelf = new Shelf(shelfLabel, position, bookCapacity, null, List.of(), bookcaseId);
repository.save(shelf);
// If shelfLabel is blank, the Shelf constructor throws. Period.
```

---

### Issue #2: Repository `save()` Bypasses Domain Object Construction (DEEP DIVE)

This is the most important issue. Let's break it apart.

#### 2a. Invariant Enforcement Failure

Here's the current `save()` port signature:

```java
void save(Long bookcaseId, int position, String shelfLabel, int bookCapacity);
```

And here's what the adapter does with it:

```java
ShelfEntity entity = new ShelfEntity();
entity.setBookcaseId(bookcaseId);
entity.setShelfPosition(position);
entity.setShelfLabel(shelfLabel);
entity.setBookCapacity(bookCapacity);
jpaRepository.save(entity);
```

The domain model `Shelf` has a constructor with invariant checks — but **none of them run on the create path**. The `Shelf` class might as well not exist for this flow. You could delete the Shelf class entirely and creation would still work. That's a red flag.

**What goes wrong later**: Imagine a future developer adds a new invariant to `Shelf`:

```java
// New business rule: max 50 books per shelf
if (bookCapacity > 50) {
    throw new IllegalArgumentException("Shelf capacity cannot exceed 50");
}
```

They add it to `Shelf` and feel confident. But `CreateShelfUseCase` never creates a `Shelf`. The new rule is silently dead on the create path. A user creates a shelf with `bookCapacity = 200`. No error. Corrupt data in the DB. Discovered three months later during an audit.

#### 2b. Why a Primitive-Only `save()` Port Is a Smell

A port is a **contract owned by core**. It should speak the **domain's language**.

```java
// Port speaks database columns (bad)
void save(Long bookcaseId, int position, String shelfLabel, int bookCapacity);

// Port speaks domain language (good)
void save(Shelf shelf);
```

When the port accepts primitives, it says: "I know the decomposition of a Shelf — its fields, their types, their order." That's not the port's business. The port should say: "Give me a Shelf. I'll figure out how to persist it."

**The litmus test**: If you add a field to `Shelf`, how many files change?

| Port design | Files that change |
|---|---|
| `save(Shelf)` | `Shelf.java`, `ShelfEntity.java`, mapper, migration |
| `save(Long, int, String, int)` | All of the above **+ port interface + every use case that calls save + every test that mocks save** |

The primitive-parameter port creates **coupling fan-out**.

#### 2c. Coupling to Persistence Schema

"But they're just primitives, not JPA types!" — True, but the *set* of primitives mirrors the database columns. If the DB schema changes (e.g., `shelfLabel` splits into `shelfName` + `shelfCode`), the port interface changes, which means core changes. Core should not change because of a DB restructuring. That violates the dependency rule.

```
 Schema change: split shelfLabel into shelfName + shelfCode

 With save(Shelf):
   Shelf stays the same (shelfLabel is domain concept)
   Adapter changes mapping: shelf.getShelfLabel() → entity.setShelfName(...) + entity.setShelfCode(...)
   Core is untouched. ✅

 With save(Long, int, String, int):
   Port signature changes → core changes. ❌
   Use case changes → core changes. ❌
   Every mock in tests changes. ❌
```

#### 2d. Impact on Testing Strategy

**With primitive-parameter save()** (current):
- Unit-testing the use case means mocking `save(Long, int, String, int)` and verifying the right primitives were passed. You're testing *argument forwarding*. Low-value.
- You can't test that domain invariants run on create — because they don't.
- The *interesting* behavior (invariant enforcement) only runs if you integration-test through the adapter. But the adapter is persistence code — slow, needs a DB.

**With domain-object save()** (target):
- Unit-testing the use case means: call `execute(...)`, verify that a `Shelf` with the right state was passed to `save()`. You capture the argument and assert domain properties.
- Domain invariants run in the use case (via `new Shelf(...)`), so a plain unit test catches invalid inputs — no DB needed.
- Adapter tests become pure mapping tests: given a `Shelf`, does it produce the right `ShelfEntity`? Simple, fast, isolated.

```
 CURRENT: Hard to unit-test creation invariants
 ┌──────────────┐       ┌─────────────────────┐
 │ Use case     │──────▶│ save(primitives)     │  invariants? nowhere.
 │ (validates)  │       │ (builds entity)      │
 └──────────────┘       └─────────────────────┘
        ▲
        │ unit test can only check: "were the right primitives passed?"

 TARGET: Easy to unit-test creation invariants
 ┌──────────────┐       ┌─────────────────────┐
 │ Use case     │──────▶│ save(Shelf)          │  adapter maps.
 │ (new Shelf)  │       │                      │
 └──────────────┘       └─────────────────────┘
        ▲
        │ unit test: capture Shelf arg, assert .getShelfLabel(), .getBookCapacity(), etc.
        │ PLUS: if input is invalid, Shelf constructor throws before save() — pure unit test.
```

#### 2e. Impact on Refactors

What breaks when `Shelf` evolves?

| Evolution | `save(Shelf)` port | `save(primitives)` port |
|---|---|---|
| Add `ShelfLabel` value object | Shelf constructor changes; adapter maps `shelf.getShelfLabel().value()` → entity field. Port unchanged. | Port signature changes: `String shelfLabel` → what? `ShelfLabel`? Now the port must import a value object that didn't exist before. Or keep `String` and lose type safety. |
| Add derived field `remainingCapacity` | Shelf computes it. Port unchanged. Adapter ignores it (not persisted). | N/A — derived fields never enter the picture because there's no Shelf object. Some other code will need to compute it, likely duplicating logic. |
| Add `ShelfCreated` domain event | Shelf (or use case) emits event after construction. Port unchanged. | Where does the event come from? The adapter? Now infrastructure is producing domain events. Dependency rule violated. |

#### 2f. Undermining Ubiquitous Language

The port should model domain operations, not database operations.

```java
// This reads as: "save these five database columns"
void save(Long bookcaseId, int position, String shelfLabel, int bookCapacity);

// This reads as: "persist this Shelf"
void save(Shelf shelf);
```

When a domain expert reads the first signature, they see plumbing. When they read the second, they see their language. Ubiquitous language isn't just about naming — it's about *what concepts appear at what level*. A port in `core/ports` should never expose `bookcaseId` as a raw `Long` if the domain has a concept of "the bookcase this shelf belongs to."

#### 2g. Concrete Before/After

**BEFORE** (current):

```java
// CreateShelfUseCase.java
public void execute(Long bookcaseId, int position, String shelfLabel, int bookCapacity) {
    // duplicated validation...
    shelfDomainRepositoryPort.save(bookcaseId, position, shelfLabel, bookCapacity);
}

// ShelfDomainRepositoryImpl.java
@Override
public void save(Long bookcaseId, int position, String shelfLabel, int bookCapacity) {
    ShelfEntity entity = new ShelfEntity();
    entity.setBookcaseId(bookcaseId);
    entity.setShelfPosition(position);
    entity.setShelfLabel(shelfLabel);
    entity.setBookCapacity(bookCapacity);
    jpaRepository.save(entity);
}
```

**AFTER** (target):

```java
// CreateShelfUseCase.java
public void execute(CreateShelfCommand command) {
    Shelf shelf = new Shelf(
        command.shelfLabel(),
        command.position(),
        command.bookCapacity(),
        null,                   // ShelfId — assigned by persistence
        List.of(),              // no books yet
        command.bookcaseId()
    );
    shelfDomainRepositoryPort.save(shelf);
}

// ShelfDomainRepository.java (port)
void save(Shelf shelf);

// ShelfDomainRepositoryImpl.java (adapter)
@Override
public void save(Shelf shelf) {
    ShelfEntity entity = shelfMapper.toEntity(shelf);
    jpaRepository.save(entity);
    logger.info("Shelf created with ID: {} for bookcase: {}",
        entity.getShelfId(), shelf.getBookcaseId());
}
```

Look at what happened:
- Use case constructs a domain object → invariants **always** run
- Port accepts `Shelf` → speaks domain language
- Adapter uses mapper → single place for domain-to-entity translation
- Validation code in use case → deleted (domain handles it)

#### 2h. Two Alternative Designs and Tradeoffs

**Option A: Repo accepts `Shelf`** (recommended)

```java
void save(Shelf shelf);
```

| Pro | Con |
|---|---|
| Strongest domain guarantee: a Shelf was constructed and validated | Requires a valid Shelf at call time, including fields that might be unknown (e.g., `ShelfId` is null before insert) |
| Port is maximally stable — survives field additions, value object introductions | Shelf must tolerate partial state (null ID for unsaved entities) |
| Adapter is a pure translator | — |

**Handling the null-ID issue**: This is common. Two approaches:
1. `ShelfId` is nullable on a new Shelf (simplest; your current `Shelf` already accepts null `ShelfId`)
2. Use a factory method: `Shelf.createNew(label, position, capacity, bookcaseId)` that internally passes null ID

**Option B: Repo accepts a command / DTO**

```java
void save(CreateShelfCommand command);
```

| Pro | Con |
|---|---|
| Avoids the "null ID" design question | **Domain invariants are still bypassed** — the command is just primitives in a record |
| Cleaner than four primitives | Port knows about a use-case-specific type, reducing its generality |
| — | If you later need `save()` for update paths, you need a second method or a generic type |

**Verdict**: Option A is the right default. It keeps the domain model as the single source of truth. The null-ID inconvenience is minor and well-understood in DDD (entities have an "unsaved" state). Option B is sometimes acceptable for CQRS-style architectures where commands and queries are strictly separated, but for your modular monolith, Option A is cleaner.

#### 2i. Failure Story

**The Silent Corruption Bug**:

Team ships V1 of shelf management. Everything works. Six months later, product asks: "Shelves should have a minimum capacity of 5 for our new premium tier."

A developer adds to `Shelf`:
```java
if (bookCapacity < 5) {
    throw new IllegalArgumentException("Minimum shelf capacity is 5");
}
```

They write unit tests for `new Shelf(...)` — all pass. They update `UpdateShelfUseCase` which already constructs a Shelf — that path is protected.

But `CreateShelfUseCase` never builds a `Shelf`. It calls `save(Long, int, String, int)`. The new invariant doesn't fire on create. Users happily create shelves with capacity 1. The bug lives in production for weeks before someone notices shelves with capacity < 5 in the database.

Root cause: the create path bypassed the domain model. The domain looked like the source of truth but **wasn't** for creation.

---

### Issue #3: No Command Object (DEEP DIVE)

#### 3a. Primitive Obsession and Readability

Current signature:
```java
public void execute(Long bookcaseId, int position, String shelfLabel, int bookCapacity)
```

Problems at call sites:

```java
// Which int is position and which is capacity?
// A tired developer writes:
createShelfUseCase.execute(42L, 10, "Fiction", 3);
// Was that position=10, capacity=3? Or position=3, capacity=10?
// Java won't help you. Both are int.
```

This is **primitive obsession**: using raw types where a named structure would prevent errors. The compiler can't catch `execute(42L, 3, "Fiction", 10)` vs `execute(42L, 10, "Fiction", 3)`.

#### 3b. Extensibility

Product says: "We'd like to add an optional shelf color and max weight."

**Without a command object**:
```java
// V1
void execute(Long bookcaseId, int position, String shelfLabel, int bookCapacity)

// V2 - two more params, one nullable
void execute(Long bookcaseId, int position, String shelfLabel, int bookCapacity,
             String shelfColor, Double maxWeight)

// V3 - oh no, now we need a shelf type enum too
void execute(Long bookcaseId, int position, String shelfLabel, int bookCapacity,
             String shelfColor, Double maxWeight, ShelfType type)
```

This grows unboundedly. You end up with telescoping parameters or multiple overloads. Every addition touches the use case signature, every controller, every test.

**With a command object**:
```java
// V1, V2, V3 — signature never changes
void execute(CreateShelfCommand command)

// You just add fields to the record:
public record CreateShelfCommand(
    Long bookcaseId,
    int position,
    String shelfLabel,
    int bookCapacity,
    String shelfColor,     // added in V2
    Double maxWeight,      // added in V2
    ShelfType type         // added in V3
) {}
```

The use case method signature is **closed for modification, open for extension**. That's not just OCP — it's practical sanity.

#### 3c. Validation and Error Reporting

Command objects give you a natural place to validate *input shape* (as opposed to domain invariants):

```java
public record CreateShelfCommand(
    Long bookcaseId,
    int position,
    String shelfLabel,
    int bookCapacity
) {
    public CreateShelfCommand {
        // Input-shape validation (pre-domain):
        // "Is this a well-formed request?"
        Objects.requireNonNull(bookcaseId, "bookcaseId is required");
        if (shelfLabel == null || shelfLabel.isBlank()) {
            throw new IllegalArgumentException("shelfLabel is required");
        }
    }
}
```

Wait — doesn't this duplicate the `Shelf` constructor validation? Not quite. There's a distinction:

| Layer | Question | Example |
|---|---|---|
| Command validation | "Is this request well-formed?" | "bookcaseId must not be null" (input gate) |
| Domain validation | "Is this a valid Shelf?" | "bookCapacity must be >= 1" (business rule) |

In practice, some checks overlap. That's OK — the command is **defense-in-depth** at the boundary, and the domain is the **source of truth**. If you want to be strict, keep the command's compact constructor minimal (just null checks) and let the domain handle business rules.

The key benefit: error messages from a command object are *about the request*, and error messages from the domain are *about the business*. This separation improves API error responses.

#### 3d. How Command Objects Interact with Hexagonal Boundaries

```
  Controller (Adapter, inbound)
      │
      │  maps: ShelfRequest DTO ──▶ CreateShelfCommand
      │
      ▼
  CreateShelfUseCase (Application)
      │
      │  reads: command.bookcaseId(), command.shelfLabel(), ...
      │  builds: new Shelf(...)
      │
      ▼
  ShelfDomainRepository (Port, outbound)
      │
      ▼
  ShelfDomainRepositoryImpl (Adapter, outbound)
      │
      │  maps: Shelf ──▶ ShelfEntity
      │
      ▼
  JPA / Database
```

The command object lives in **application layer** (`core/application/commands/`). It's a *core* type — controllers can depend on it (adapters depend inward). This means:
- Controller tests can construct commands directly
- Use case tests can construct commands directly
- No DTO-to-primitive unpacking at the use case boundary

#### 3e. Java-Specific Best Practices

**Records are perfect for commands**: immutable, compact, auto-generated `equals`/`hashCode`/`toString`, support compact constructors for validation.

```java
package com.penrose.bibby.library.stacks.shelf.core.application.commands;

import java.util.Objects;

public record CreateShelfCommand(
    Long bookcaseId,
    int position,
    String shelfLabel,
    int bookCapacity
) {
    public CreateShelfCommand {
        Objects.requireNonNull(bookcaseId, "bookcaseId is required");
        Objects.requireNonNull(shelfLabel, "shelfLabel is required");
    }
}
```

**Naming conventions**:
- `Create___Command`, `Update___Command`, `Delete___Command`
- Lives in `core/application/commands/` (or `core/application/dto/` — I prefer `commands/`)
- Use case method: `execute(CreateShelfCommand command)` — reads naturally

**Package placement**:
```
shelf/
  core/
    application/
      commands/
        CreateShelfCommand.java      ← here
      usecases/
        CreateShelfUseCase.java
    domain/
      model/
        Shelf.java
    ports/
      outbound/
        ShelfDomainRepository.java
```

#### 3f. How `execute(command)` Reads at Call Sites

```java
// BEFORE: what does 3 mean? what does 10 mean?
createShelfUseCase.execute(42L, 3, "Fiction", 10);

// AFTER: self-documenting
var command = new CreateShelfCommand(42L, 3, "Fiction", 10);
createShelfUseCase.execute(command);

// Even better in a controller:
@PostMapping
public ResponseEntity<Void> createShelf(@RequestBody ShelfRequest request) {
    var command = new CreateShelfCommand(
        request.bookcaseId(),
        request.position(),
        request.label(),
        request.capacity()
    );
    createShelfUseCase.execute(command);
    return ResponseEntity.status(HttpStatus.CREATED).build();
}
```

The command acts as a named boundary between "what the outside world sent" and "what the use case needs." It's translatable from any inbound adapter (REST, CLI, message queue, test) without the use case knowing or caring.

---

## 4. Recommended Target Design

### Proposed Signatures

```java
// Command (core/application/commands/)
public record CreateShelfCommand(
    Long bookcaseId,
    int position,
    String shelfLabel,
    int bookCapacity
) {
    public CreateShelfCommand {
        Objects.requireNonNull(bookcaseId, "bookcaseId is required");
        Objects.requireNonNull(shelfLabel, "shelfLabel is required");
    }
}

// Use case (core/application/usecases/)
public class CreateShelfUseCase {

    private final ShelfDomainRepository shelfDomainRepositoryPort;

    public CreateShelfUseCase(ShelfDomainRepository shelfDomainRepositoryPort) {
        this.shelfDomainRepositoryPort = shelfDomainRepositoryPort;
    }

    public void execute(CreateShelfCommand command) {
        Shelf shelf = new Shelf(
            command.shelfLabel(),
            command.position(),
            command.bookCapacity(),
            null,           // ID assigned by persistence
            List.of(),      // empty book list for new shelf
            command.bookcaseId()
        );
        shelfDomainRepositoryPort.save(shelf);
    }
}

// Port (core/ports/outbound/)
public interface ShelfDomainRepository {
    Shelf getById(ShelfId id);
    void save(Shelf shelf);                         // ← changed
    void deleteByBookcaseId(Long bookcaseId);
    List<Shelf> findByBookcaseId(Long bookcaseId);
    Shelf findById(Long shelfId);
    List<Shelf> findAll();
}

// Adapter (infrastructure/adapter/outbound/)
@Override
public void save(Shelf shelf) {
    ShelfEntity entity = shelfMapper.toEntity(shelf);
    jpaRepository.save(entity);
    logger.info("Shelf created with ID: {} for bookcase: {}",
        entity.getShelfId(), shelf.getBookcaseId());
}
```

### Where `@Service` Should Live

`@Service` is a Spring framework annotation. It belongs **outside core** in a strict hexagonal architecture. Two options:

| Option | How | When to use |
|---|---|---|
| **Pragmatic** (recommended for now) | Keep `@Service` on the use case | You're in a modular monolith and Spring is pervasive. The annotation is metadata, not behavior. Fight this battle later or never. |
| **Strict** | Remove `@Service`. Wire via a `@Configuration` class in infrastructure: `@Bean CreateShelfUseCase createShelfUseCase(ShelfDomainRepository repo) { return new CreateShelfUseCase(repo); }` | When you want core to be framework-free (portable, testable without Spring context). |

For Bibby today, the pragmatic approach is fine. Note it as tech debt if you want, but don't let it block the more important refactors.

### What Stays the Same

- `Shelf` domain model — unchanged (it already has the right constructor)
- `ShelfEntity` — unchanged
- `ShelfMapper` — may need a `toEntity(Shelf)` method if it doesn't have one
- JPA repository — unchanged
- Other port methods (`findById`, `findAll`, etc.) — unchanged
- Inbound adapter/controller — small change to construct `CreateShelfCommand` instead of passing primitives

---

## 5. Step-by-Step Refactor Plan

Each step is small, safe, and independently deployable.

### Step 1: Create `CreateShelfCommand` record

**Do**: Create `core/application/commands/CreateShelfCommand.java`.

**Test**: Write a unit test that constructs the command with valid inputs and verifies accessors. Write a test that null `bookcaseId` throws.

**Risk**: Zero. Nothing depends on it yet.

### Step 2: Change use case signature to accept command

**Do**: Change `execute(Long, int, String, int)` → `execute(CreateShelfCommand command)`. Internally, still forward primitives to `save()` (don't change the port yet).

```java
public void execute(CreateShelfCommand command) {
    // Still forwarding primitives — temporary
    shelfDomainRepositoryPort.save(
        command.bookcaseId(), command.position(),
        command.shelfLabel(), command.bookCapacity()
    );
}
```

**Test**: Update existing use case tests to construct a command. They should still pass — behavior unchanged.

**Also**: Update any inbound adapters (controllers) that call `execute()`.

**Risk**: Low. Mechanical signature change.

### Step 3: Add `Shelf` construction in use case, remove duplicated validation

**Do**: Replace the primitive forwarding with `new Shelf(...)`. Remove the inline validation (the Shelf constructor handles it).

```java
public void execute(CreateShelfCommand command) {
    Shelf shelf = new Shelf(
        command.shelfLabel(), command.position(), command.bookCapacity(),
        null, List.of(), command.bookcaseId()
    );
    // Still calling old save(primitives) — temporary
    shelfDomainRepositoryPort.save(
        shelf.getBookcaseId(), shelf.getShelfPosition(),
        shelf.getShelfLabel(), shelf.getBookCapacity()
    );
}
```

**Test**: Add a test that `execute(command with bookCapacity=0)` throws `IllegalArgumentException` — verifying domain invariants now fire on the create path. Verify the error message matches the domain's.

**Risk**: Low. We're adding Shelf construction but haven't changed the port yet.

### Step 4: Change port signature to `save(Shelf)`

**Do**: Change `ShelfDomainRepository.save(Long, int, String, int)` → `save(Shelf shelf)`.

**Do**: Update `ShelfDomainRepositoryImpl.save()` to accept `Shelf`, use mapper:
```java
@Override
public void save(Shelf shelf) {
    ShelfEntity entity = shelfMapper.toEntity(shelf);
    jpaRepository.save(entity);
}
```

**Do**: Ensure `ShelfMapper` has a `toEntity(Shelf)` method. If not, add it.

**Do**: Update use case to call `shelfDomainRepositoryPort.save(shelf)`.

**Test**: Update adapter/integration tests. Verify that saving a domain `Shelf` results in the correct `ShelfEntity` in the database.

**Risk**: Medium — this is the biggest single change. The mapper is the key: make sure it handles null `ShelfId` correctly (for inserts, the DB generates the ID).

### Step 5: Clean up and verify

**Do**:
- Remove any dead code from the old primitive-parameter path
- Run full test suite
- Review that `@Service` placement is acceptable for now (document decision)

**Test**: Full regression. Pay special attention to integration tests that create shelves.

---

## 6. Rules of Thumb

Reusable heuristics for future use case reviews:

### The Rules

1. **"If a use case doesn't touch a domain object, it's a transaction script."**
   Use cases should construct or retrieve domain objects, then act on them. If `execute()` just forwards primitives to a port, you've lost the domain layer.

2. **"Ports speak domain language, not database language."**
   Port parameters should be domain types (`Shelf`, `ShelfId`, `BookcaseId`) — not columns (`Long bookcaseId, int position, String shelfLabel`). If your port signature looks like a SQL INSERT statement, redesign it.

3. **"The domain constructor is the gatekeeper."**
   All creation paths must flow through the domain constructor (or a factory). If there's a path from "user request" to "row in DB" that skips the constructor, invariants are unenforceable.

4. **"If a method has 3+ primitives of the same type, introduce a named structure."**
   Two `int` parameters next to each other is a swap-bug waiting to happen. A command record makes parameter order explicit and self-documenting.

5. **"Duplicated validation = two sources of truth = zero sources of truth."**
   Business rules live in the domain. Use case validation is limited to application-level concerns the domain can't check (e.g., "does this bookcaseId exist?").

6. **"Count the files that change when you add a field."**
   If adding one field to your domain model forces changes in the port interface, every use case, and every test mock — your abstractions are leaking. Good port design absorbs field changes.

7. **"Adapters translate, they don't decide."**
   An adapter (repo impl) should map domain → persistence and persistence → domain. It should never construct domain objects from scratch, validate business rules, or make business decisions.

8. **"Test the seam, not the wiring."**
   If your unit test only asserts "these primitives were forwarded to the mock" — you're testing wiring, not behavior. Test that the *right domain object* was produced, with the *right state*, enforcing the *right invariants*.

9. **"Commands are input; domain objects are truth."**
   A `CreateShelfCommand` represents *what the user wants*. A `Shelf` represents *what the system accepts*. The use case is where "want" becomes "truth" — by constructing a domain object.

10. **"When in doubt, follow the dependency arrow."**
    If something in `core/` imports something from `infrastructure/`, stop. If a port signature requires knowing about a persistence detail, stop. Dependencies flow inward. Always.

---

### Check Your Understanding

Before you refactor, see if you can answer these:

1. After the refactor, if someone adds a new invariant to `Shelf` (e.g., "label max 100 chars"), how many files need to change for it to be enforced on creation?

2. Why is it OK for the `Shelf` constructor to receive `null` for `ShelfId` on creation, but the adapter should never return a `Shelf` with null `ShelfId` on a read?

3. If a future `BatchImportShelvesUseCase` needs to create 100 shelves, how does the domain-centric design protect you compared to the transaction script?

Think about those. If the answers feel obvious, you've internalized the model. If not, re-read section 2 — that's where the intuition lives.
