# Devlog: Split ShelfFacade Into ShelfQueryFacade and ShelfCommandFacade

**Date:** 2026-02-27
**Time:** 18:11 CST
**Branch:** `feat/shelf-placement-and-book-mapper-relocation`
**Range:** `origin/main..HEAD`
**Commits:**
- `121d910` — Split ShelfFacade into ShelfQueryFacade and ShelfCommandFacade
- `3c14ebb` — Add ADR-0007 and devlog for shelf placement consolidation
- `71eeb91` — Refactor ShelfService and ShelfController for improved readability

---

## What problem was I solving?

`ShelfFacade` had grown into a 9-method blob that mixed read-only queries (`findShelfById`,
`findAll`, `isFull`, …) with state-mutating commands (`createShelfInBookcaseByBookcaseId`,
`deleteAllShelvesInBookcaseByBookcaseId`, `placeBookOnShelf`). Every consumer — CLI commands, REST
controllers, cross-module port adapters — had to declare a dependency on the entire interface even
when they only needed one or two methods from one side of that divide.

## What was the "before" state?

```
ShelfFacade  (9 methods — queries + commands mixed)
    └── implemented by ShelfService
    └── injected by 12 consumers (CLI commands, REST controllers, cross-module adapters)
```

All 12 injection sites held the full `ShelfFacade` type. The bookcase module's
`ShelfAccessPortAdapter` (which only needs `deleteAllShelves` and `createShelf`) and the cataloging
module's `ShelfAccessPortAdapter` (which only needs `findShelfById` and `isFull`) were both forced
to depend on an interface that contained the other side's responsibilities.

## Most important outcomes

- `ShelfFacade` deleted; replaced by `ShelfQueryFacade` (6 methods) and `ShelfCommandFacade` (3 methods).
- `ShelfService` now declares `implements ShelfQueryFacade, ShelfCommandFacade` — no behavior change, compile-clean.
- 10 out of 12 consumers narrowed to `ShelfQueryFacade` only (they never mutated state).
- `BookController` and `bookcase/ShelfAccessPortAdapter` now depend only on `ShelfCommandFacade`.
- `ShelfController` (stacks REST) is the only consumer that injects both facades — one POST endpoint writes, two GET endpoints read.
- Mocks in `BookCreateIsbnCommandsTest` and `PromptOptionsTest` updated to the narrower query type.
- ADR-0007 and a prior devlog committed alongside the code.

---

## Commit-by-Commit Analysis

---

### Commit `121d910` — Split ShelfFacade into ShelfQueryFacade and ShelfCommandFacade

**Intent:** Decompose the monolithic inbound port along the query/command axis so consumers only
depend on the capability they actually use.

**Files touched:**

| File | Change | Reason |
|---|---|---|
| `ShelfQueryFacade.java` (new) | A | New read-only inbound port — 6 query methods |
| `ShelfCommandFacade.java` (new) | A | New write inbound port — 3 command methods |
| `ShelfFacade.java` | D (renamed → ShelfQueryFacade) | Deleted — Git detected 58% similarity rename |
| `ShelfService.java` | M | `implements ShelfQueryFacade, ShelfCommandFacade` |
| `bookcase/ShelfAccessPortAdapter.java` | M | Field type → `ShelfCommandFacade` |
| `cataloging/ShelfAccessPortAdapter.java` | M | Field type → `ShelfQueryFacade` |
| `ShelfController.java` (stacks) | M | Injects both facades separately |
| `BookController.java` | M | Field type → `ShelfCommandFacade` |
| 6× CLI command files | M | Field types → `ShelfQueryFacade` |
| `PromptOptions.java` | M | Field type → `ShelfQueryFacade` |
| `BookCreateIsbnCommandsTest.java` | M | Mock type → `ShelfQueryFacade` |
| `PromptOptionsTest.java` | M | Mock type → `ShelfQueryFacade` |

**Key code changes:**

`ShelfQueryFacade` — pure read contract:
```java
public interface ShelfQueryFacade {
  List<ShelfResponse>        findShelvesByBookcaseId(Long bookcaseId);
  Optional<ShelfResponse>    findShelfById(Long shelfId);
  List<ShelfSummaryResponse> getShelfSummariesForBookcaseByBookcaseId(Long bookcaseId);
  List<ShelfResponse>        findAll();
  boolean isFull(Long shelfId);
  boolean isEmpty(Long shelfId);
}
```

`ShelfCommandFacade` — pure write contract:
```java
public interface ShelfCommandFacade {
  void deleteAllShelvesInBookcaseByBookcaseId(Long bookcaseId);
  void createShelfInBookcaseByBookcaseId(Long bookcaseId, int shelfPosition,
                                         String shelfLabel, int bookCapacity);
  void placeBookOnShelf(Long bookId, Long shelfId);
}
```

`ShelfService` — single Spring bean satisfying both ports:
```java
@Service
public class ShelfService implements ShelfQueryFacade, ShelfCommandFacade { … }
```

`ShelfController` — only adapter that needs both ports (one write, two reads):
```java
public ShelfController(ShelfQueryFacade shelfQueryFacade,
                       ShelfCommandFacade shelfCommandFacade,
                       ShelfResponseMapper shelfResponseMapper) { … }
```

**Architecture notes:**

The two interfaces live at `library/stacks/shelf/core/ports/inbound/` — correctly inside the core
module, following hexagonal architecture. Adapters (CLI, REST, cross-module port adapters) depend
**inward** on these ports; the core does not depend on the adapters. The split does not change that
dependency direction; it only narrows each external dependency to its minimal required surface.

Cross-module note: the `bookcase` module's `ShelfAccessPortAdapter` was already bridging
`ShelfAccessPort` (outbound port of bookcase) to `ShelfFacade` (inbound port of shelf). It now
bridges to `ShelfCommandFacade`, which is the correct narrower type — the bookcase module creates
and deletes shelves; it never queries them.

Spring autowiring: `ShelfService` is annotated `@Service`. Because it is the only bean implementing
`ShelfQueryFacade` and the only bean implementing `ShelfCommandFacade`, Spring resolves each
injection point without ambiguity. No `@Primary` or `@Qualifier` needed.

**Risk / edge cases:**

- No behavior change — method bodies in `ShelfService` are identical; only the declared interface
  type on each injection site changed.
- If a future developer adds a second `@Service` implementing `ShelfQueryFacade` without a
  `@Primary`, Spring will throw a `NoUniqueBeanDefinitionException`. This is actually desirable
  — the ambiguity will be explicit rather than silent.
- `ShelfController` is the only class in the codebase where both facades are co-located. This is
  intentional (it owns the full shelf REST surface) but worth flagging in code review — if the
  controller grows, it may be a signal to split it.

**Verification:**

```bash
./mvnw test   # all tests passed at commit time
```

---

### Commit `3c14ebb` — Add ADR-0007 and devlog for shelf placement consolidation

**Intent:** Record the architectural decision that dissolved the standalone `placement` module and
document the session narrative for the `feat/shelf-placement-and-book-mapper-relocation` branch.

**Files touched:**

| File | Change | Reason |
|---|---|---|
| `docs/ADR-0007-consolidate-placement-into-shelf-bounded-context.md` | A | ADR for placement module absorption decision |
| `docs/the-devlogs/devlog-2026-02-27-Shelf-Placement-Feature-And-Book-Mapper-Relocation.md` | A | Full session devlog for the branch |

**Key content:**

ADR-0007 captures the `Status: Accepted` decision to move `placeBookOnShelf` into the Shelf bounded
context (rather than keep it in a standalone `placement` module). The decision was motivated by the
lack of an independent lifecycle, the absence of domain rules inside placement, and the fact that
the operation is fundamentally a command on the `Shelf` aggregate.

**Architecture notes:** Documentation only. No production or test code changed.

**Risk:** None.

---

### Commit `71eeb91` — Refactor ShelfService and ShelfController for improved readability

**Intent:** Cosmetic cleanup of the two files most structurally changed in `121d910`. Javadoc line
wrapping in `ShelfService` and constructor parameter formatting in `ShelfController`.

**Files touched:**

| File | Change | Reason |
|---|---|---|
| `ShelfService.java` | M | Reflow multi-line Javadoc paragraph to stay within column limit |
| `ShelfController.java` (stacks) | M | Break long constructor signature into one-parameter-per-line format |

**Key code change:**

Before (`ShelfController` constructor — all args on one line):
```java
public ShelfController(ShelfQueryFacade shelfQueryFacade, ShelfCommandFacade shelfCommandFacade, ShelfResponseMapper shelfResponseMapper) {
```

After (formatted):
```java
public ShelfController(
    ShelfQueryFacade shelfQueryFacade,
    ShelfCommandFacade shelfCommandFacade,
    ShelfResponseMapper shelfResponseMapper) {
```

**Risk:** None. Pure formatting; no logic change.

---

## End-to-End Flow Walkthrough

### Read path: `GET /api/v1/shelves/options/{bookcaseId}`

```
HTTP GET /api/v1/shelves/options/{bookcaseId}
  → ShelfController.getShelfOptionsByBookcase(bookcaseId)
      → shelfQueryFacade.findShelvesByBookcaseId(bookcaseId)   // ShelfQueryFacade
          → ShelfService.findShelvesByBookcaseId(bookcaseId)   // ShelfService implements ShelfQueryFacade
              → QueryShelfUseCase.findShelvesByBookcaseId(bookcaseId)
                  → ShelfRepository (JPA)
              ← List<Shelf> domain objects mapped to List<ShelfResponse>
          ← List<ShelfResponse>
      → shelfResponseMapper.toShelfOption(each)
  ← List<ShelfOptionResponse> (JSON)
```

### Write path: `POST /api/v1/shelves/placements`

```
HTTP POST /api/v1/shelves/placements  { bookId, shelfId }
  → ShelfController.addBookToShelf(request)
      → shelfCommandFacade.placeBookOnShelf(bookId, shelfId)   // ShelfCommandFacade
          → ShelfService.placeBookOnShelf(bookId, shelfId)     // ShelfService implements ShelfCommandFacade
              → PlaceBookOnShelfUseCase.execute(bookId, shelfId)
                  → validate book exists (BookRepository)
                  → update Shelf aggregate (ShelfRepository)
  ← 200 OK (void)
```

### Cross-module write path: bookcase deletion cascade

```
BookcaseFacade.deleteBookCase(bookcaseId)
  → BookcaseService
      → ShelfAccessPort.deleteAllShelvesInBookcase(bookcaseId)   // outbound port, bookcase module
          → bookcase/ShelfAccessPortAdapter.deleteAllShelvesInBookcase(bookcaseId)
              → shelfCommandFacade.deleteAllShelvesInBookcaseByBookcaseId(bookcaseId)  // ShelfCommandFacade
                  → ShelfService → DeleteShelvesUseCase
```

---

## Why This Design Is Better

| Old design | New design |
|---|---|
| All 12 consumers depended on 9-method `ShelfFacade` | Each consumer depends on only the methods it actually calls |
| `bookcase/ShelfAccessPortAdapter` could call read methods on shelf (cross-module leakage risk) | `bookcase/ShelfAccessPortAdapter` now holds `ShelfCommandFacade` — it is **structurally impossible** for it to call query methods |
| Mocks in tests had to stub/satisfy a 9-method interface | Mocks declare a 6- or 3-method interface; test setup is tighter |
| New read-only consumers would silently get write capability | New consumers must explicitly choose `ShelfQueryFacade` or `ShelfCommandFacade` |

---

## Dependencies — What Changed

**Removed:**
- `ShelfFacade` — deleted; no consumer imports it anymore.

**Added:**
- `ShelfQueryFacade` — imported by 11 consumers.
- `ShelfCommandFacade` — imported by 3 consumers (`ShelfController`, `bookcase/ShelfAccessPortAdapter`, `BookController`).

**Unchanged:**
- `ShelfService` dependency on `QueryShelfUseCase`, `CreateShelfUseCase`, `DeleteShelvesUseCase`, `PlaceBookOnShelfUseCase` — untouched.
- All outbound ports and repository dependencies — untouched.

---

## Layering Check

All imports point inward:

- `ShelfController` (infrastructure/web) → `ShelfQueryFacade`, `ShelfCommandFacade` (core/ports/inbound) ✅
- `BookcaseCommands` (infrastructure/cli) → `ShelfQueryFacade` (core/ports/inbound, shelf module) ✅
- `bookcase/ShelfAccessPortAdapter` (infrastructure, bookcase module) → `ShelfCommandFacade` (core/ports/inbound, shelf module) ✅

No new layering violations introduced. The only cross-module dependency is the bookcase and cataloging modules' adapters reaching into `shelf/core/ports/inbound`. That was already the pattern before this change; it is the intended cross-module access point.

---

## Suggested Micro-Fixes

1. **Update `stacks/README.md`** — Still references `ShelfFacade.java` in several places (lines ~69, 89, 229, 250, 255). Should be updated to `ShelfQueryFacade` / `ShelfCommandFacade`.
2. **`BookPlacementCommands` still builds `ShelfDTO` manually** — Line 56–64 maps `ShelfResponse` fields into a `ShelfDTO` inline. This is a manual projection inside a CLI command; it belongs in a mapper or factory.
3. **`isEmpty` has no caller** — `ShelfQueryFacade.isEmpty(Long shelfId)` is declared and implemented but has no call site in the current codebase (checked via grep). Candidate for removal if no feature needs it.

---

## Tests

### What exists

| Test class | What it covers |
|---|---|
| `PromptOptionsTest` | `PromptOptions.bookCaseOptions()` — queries shelves per bookcase, verifies formatting, ordering, book counts. Mocks `ShelfQueryFacade`. |
| `BookCreateIsbnCommandsTest` | `BookCreateIsbnCommands.createBookScan()` — confirms shelf lookup (`findShelfById`) is called (or not) based on user flow. Mocks `ShelfQueryFacade`. |

### Recommended new tests

```bash
# Run existing tests
./mvnw test

# Run only the affected test classes
./mvnw test -pl . -Dtest="PromptOptionsTest,BookCreateIsbnCommandsTest"
```

**Test cases to add:**

1. **`ShelfControllerTest`** (missing entirely)
   - `GET /options` calls `shelfQueryFacade.findAll()` exactly once and maps results.
   - `GET /options/{bookcaseId}` calls `shelfQueryFacade.findShelvesByBookcaseId(id)` exactly once.
   - `POST /placements` calls `shelfCommandFacade.placeBookOnShelf(bookId, shelfId)` exactly once.
   - Verify the two facades are injected independently (Spring context test).

2. **`BookControllerTest` for `placeBookOnShelf` delegation**
   - Confirm `BookController` calls `shelfCommandFacade.placeBookOnShelf(...)` not a query method.

3. **`bookcase/ShelfAccessPortAdapterTest`**
   - Confirm that deleting a bookcase delegates to `ShelfCommandFacade.deleteAllShelvesInBookcaseByBookcaseId`.
   - Confirm that creating shelves delegates to `ShelfCommandFacade.createShelfInBookcaseByBookcaseId` with correct argument mapping.

4. **`cataloging/ShelfAccessPortAdapterTest`**
   - `findShelfById` maps the `ShelfResponse` to a `ShelfDTO` correctly (all fields).
   - `isFull` delegates and returns the boolean from `ShelfQueryFacade.isFull`.

5. **Integration smoke test**
   - Spring context loads without ambiguous bean resolution for `ShelfQueryFacade` and `ShelfCommandFacade`.

---

## Lessons From This Diff

- **Interface bloat is a coupling smell.** When every consumer of an interface uses only a subset of its methods, the interface is doing too much. The method count (9 on `ShelfFacade`) was the first signal; the variety of callers (CLI, REST, cross-module adapters) was the second.
- **CQRS at the port boundary is cheap and high-value.** The split required no behavior change — only type signature updates. The benefit is structural: it is now impossible for a read-only adapter to accidentally call a write method.
- **Git rename detection matters.** Git correctly identified `ShelfFacade.java → ShelfQueryFacade.java` as a rename (58% similarity). If the file had been deleted and re-created independently, history would be lost. Staging the delete and add in the same commit preserved the rename metadata.
- **The only consumer needing both facades reveals architectural intent.** `ShelfController` (stacks REST) is the single adapter that needs both `ShelfQueryFacade` and `ShelfCommandFacade`. That is the right place for it — a REST controller is a thin adapter over the full surface of a resource.
- **Cross-module adapter types tell a story.** `bookcase/ShelfAccessPortAdapter` → `ShelfCommandFacade` tells you that the bookcase module is a *producer* of shelf lifecycle events. `cataloging/ShelfAccessPortAdapter` → `ShelfQueryFacade` tells you that the cataloging module is a *reader* of shelf state. The new types make that architectural narrative legible at a glance.
- **`isEmpty` with no caller is dead API.** It passed through the split unchanged, but the refactor surfaced it. Leaving dead interface methods is technical debt — they widen the mock surface in tests and imply a contract that nothing holds.

---

## Follow-Ups

### Immediate (today)

- [ ] Update `src/main/java/com/penrose/bibby/library/stacks/README.md` — replace all `ShelfFacade` references with `ShelfQueryFacade` / `ShelfCommandFacade`.
- [ ] Add `ShelfControllerTest` covering all three endpoints against mocked facades.
- [ ] Decide whether to keep or delete `ShelfQueryFacade.isEmpty` (no current caller).

### Short-term hardening (this week)

- [ ] Add `bookcase/ShelfAccessPortAdapterTest` and `cataloging/ShelfAccessPortAdapterTest` — both adapters are currently untested.
- [ ] Extract the inline `ShelfResponse → ShelfDTO` mapping in `BookPlacementCommands.addToShelf` into a dedicated mapper method.
- [ ] Add an integration/context test that asserts `ShelfService` is the unique bean for both ports and the Spring context loads cleanly.

### Strategic refactors (later)

- [ ] Consider whether `ShelfController` (stacks web) should be split into `ShelfQueryController` and `ShelfCommandController` to mirror the port split and enforce the `@GetMapping` / `@PostMapping` boundary at the class level.
- [ ] Evaluate whether `PromptOptions` belongs in the `infrastructure/cli` layer — it currently lives in `cli/prompt/domain`, which is a mild layering imprecision (it depends on multiple facades from different modules).
- [ ] As `ShelfCommandFacade` grows with new write operations, re-evaluate whether a single `@Service` implementing both facades remains the right pattern, or whether a delegating adapter per facade becomes cleaner.
