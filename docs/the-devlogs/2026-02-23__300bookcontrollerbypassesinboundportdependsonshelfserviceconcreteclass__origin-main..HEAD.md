# Devlog: Fix Hexagonal Boundary Violation — BookController No Longer Bypasses Inbound Port or Depends on ShelfService Concrete Class

**Date:** 2026-02-23 (America/Chicago)
**Branch:** `300-bookcontroller-bypasses-inbound-port-depends-on-shelfservice-concrete-class`
**Range:** `34968e8..HEAD` (branch diverged from last merge into main)
**Commits:**

| SHA | Subject |
|-----|---------|
| `2c3b1f0` | Add isFull and isEmpty capacity queries to Shelf module |
| `9da6619` | Add isFull to Book module's ShelfAccessPort |
| `ed999a4` | Move placeBookOnShelf orchestration behind BookFacade |
| `6c61a49` | Remove Stacks dependencies from BookController |
| `2064cbf` | Refactor BookDomainRepositoryImpl to remove unused dependencies and improve book retrieval logic |
| `67dbef7` | Refactor BookDomainRepositoryImpl constructor and remove unnecessary whitespace |
| `4e86637` | Remove unused BookControllerTest imports and simplify test structure |
| `ab831a2` | Remove unused BookControllerTest imports and simplify test structure |

---

## TL;DR

- **What problem was I solving?** `BookController` directly depended on `ShelfService` (concrete class) and `BookcaseService` (concrete class) from the Stacks bounded context, bypassing the Book module's inbound port (`BookFacade`) and violating hexagonal architecture boundaries. Additionally, `BookDomainRepositoryImpl` contained business logic (shelf capacity validation) and called `ShelfFacade` directly from the infrastructure layer.
- **What was the "before" state?** The `placeBookOnShelf` endpoint in `BookController` orchestrated across three bounded contexts in ~50 lines: it called `bookService.assignBookToShelf`, then `shelfService.findShelfById`, then `bookcaseService.findBookCaseById`, manually mapped DTOs, and assembled a `BookPlacementResponse`. The repository layer (`BookDomainRepositoryImpl`) also held a `ShelfFacade` dependency and performed an `isFull()` capacity check inside `placeBookOnShelf`.
- **Most important outcomes:**
  - BookController now makes a single call: `bookFacade.placeBookOnShelf(bookId, request)` — zero Stacks imports remain
  - All shelf validation logic (existence, capacity) lives in a new `bookCommandUseCases` use case class inside the Book module's application layer
  - `BookDomainRepositoryImpl` is stripped to pure persistence — no more `ShelfFacade` or `AuthorService` dependencies
  - Cross-module communication flows exclusively through ports: `ShelfAccessPort` (outbound from Book) → `ShelfFacade` (inbound to Shelf)
  - Shelf module gained `isFull(Long)` and `isEmpty(Long)` as first-class facade operations

---

## Per-Commit Breakdown

### Commit 1: `2c3b1f0` — Add isFull and isEmpty capacity queries to Shelf module

**Intent:** Expose shelf capacity queries as proper inbound port operations so other modules can ask "is this shelf full?" without reaching into Shelf domain internals.

**Files touched:**
- `ShelfFacade.java` — Added `isFull(Long)` and `isEmpty(Long)` to the inbound port interface
- `ShelfService.java` — Implements both methods, delegates to `QueryShelfUseCase`
- `QueryShelfUseCase.java` — New `isFull` and `isEmpty` methods that load the `Shelf` aggregate via `shelfDomainRepositoryPort.getById()` and delegate to the domain model's `shelf.isFull()` / `shelf.getBookCount() == 0`

**Key code changes:**
- `QueryShelfUseCase.isFull(Long)` loads the shelf by ID, throws `IllegalStateException` if not found, then returns `shelf.isFull()` — the domain model owns the capacity rule.
- `QueryShelfUseCase.isEmpty(Long)` follows the same pattern with `shelf.getBookCount() == 0`.

**Architecture notes:** Clean ports-and-adapters. The capacity check is a domain concern expressed through the domain model (`Shelf.isFull()`), exposed via the application service layer, surfaced through the inbound port. No layering violation.

**Risk / edge cases:**
- `isEmpty` uses `shelf.getBookCount() == 0` rather than a dedicated `shelf.isEmpty()` domain method — minor inconsistency with `isFull()` which delegates to a domain method. Consider adding `Shelf.isEmpty()` for symmetry.
- Both methods throw `IllegalStateException` for missing shelves. Callers must handle this.

**Verification:** `mvn test -pl . -Dtest=QueryShelfUseCaseTest` (if it exists), or `mvn test` full suite.

---

### Commit 2: `9da6619` — Add isFull to Book module's ShelfAccessPort

**Intent:** Give the Book module's application layer the ability to check shelf capacity through its own outbound port, without a direct dependency on the Shelf module.

**Files touched:**
- `ShelfAccessPort.java` (Book's outbound port) — Added `boolean isFull(Long aLong)`
- `ShelfAccessPortAdapter.java` (Book's infrastructure adapter) — Implements `isFull` by delegating to `shelfFacade.isFull(aLong)`

**Architecture notes:** This is the bridge. Book module asks "is shelf X full?" through its own `ShelfAccessPort`. The adapter in Book's infrastructure layer translates that into a `ShelfFacade.isFull()` call. Dependency direction: Book's core defines the port; Book's infrastructure implements it by calling Shelf's inbound port. Clean.

**Risk / edge cases:**
- Parameter name `aLong` is an IDE-generated placeholder — should be renamed to `shelfId` for clarity.

---

### Commit 3: `ed999a4` — Move placeBookOnShelf orchestration behind BookFacade

**Intent:** This is the core commit. Extract all `placeBookOnShelf` business logic from `BookController` and `BookDomainRepositoryImpl` into a proper use case class.

**Files touched:**
- `bookCommandUseCases.java` (NEW) — New use case class in `book.core.application.usecases`
- `BookFacade.java` — Added `placeBookOnShelf(Long, BookShelfAssignmentRequest)` to the inbound port
- `BookService.java` — Implements the new facade method, delegates to `bookCommandUseCases`
- `BookDomainRepositoryImpl.java` — Stripped shelf validation logic; now does pure persistence only

**Key code changes in `bookCommandUseCases.placeBookOnShelf`:**
```java
// 1. Validate input
if (shelfAssignmentRequest == null || shelfAssignmentRequest.shelfId() == null)
    throw new IllegalStateException("Shelf id is required");

// 2. Validate shelf exists (through ShelfAccessPort)
if (!shelfAccessPort.findShelfById(shelfAssignmentRequest.shelfId()).isPresent())
    throw new IllegalStateException("Shelf not found");

// 3. Validate shelf has capacity (through ShelfAccessPort)
if (shelfAccessPort.isFull(shelfAssignmentRequest.shelfId()))
    throw new IllegalStateException("Shelf is full");

// 4. Persist (through BookDomainRepository)
bookDomainRepository.placeBookOnShelf(bookId, shelfAssignmentRequest.shelfId());
```

**Key code changes in `BookDomainRepositoryImpl.placeBookOnShelf`:**
- **Before:** Loaded book, checked `shelfFacade.findShelfById(shelfId).get().isFull()`, set shelf ID, saved.
- **After:** Loads book, sets shelf ID, saves. No shelf validation. Pure persistence.

**Architecture notes:**
- `bookCommandUseCases` depends only on `BookDomainRepository` (outbound port) and `ShelfAccessPort` (outbound port) — no concrete infrastructure classes.
- The validation chain (existence → capacity → persist) is now in the application layer where it belongs.
- `BookDomainRepositoryImpl` no longer imports `ShelfFacade` — the cross-module dependency is removed from infrastructure.

**Risk / edge cases:**
- Class name `bookCommandUseCases` starts with a lowercase letter — violates Java naming conventions. Should be `BookCommandUseCases`.
- `bookCommandUseCases` is annotated `@Service` — correct for Spring DI, but the name "use cases" (plural) suggests it may accumulate methods. Consider whether each command deserves its own class or if this is an intentional aggregate.
- The method does two port calls (`findShelfById` + `isFull`) that each hit the shelf module separately. `isFull` internally loads the shelf again. That's 2-3 shelf lookups for one operation. Not a correctness issue, but a performance consideration.
- `IllegalStateException` is thrown for all validation failures — controller layer will need to map these to appropriate HTTP statuses (400 vs 404 vs 409).

---

### Commit 4: `6c61a49` — Remove Stacks dependencies from BookController

**Intent:** The payoff commit. Strip all Stacks (`ShelfService`, `BookcaseService`, `ShelfDTO`, `BookcaseDTO`) imports and logic from `BookController`.

**Files touched:**
- `BookController.java` — Massive reduction: ~50-line `placeBookOnShelf` method → 3 lines. Removed `ShelfService`, `BookcaseService`, `IsbnEnrichmentService` from constructor.
- `BookMapper.java` → `mappers/BookMapper.java` — Moved to a `mappers` subpackage.
- `BookControllerTest.java` (NEW, then deleted in later commits) — Initially added, then removed.

**Key code changes:**
```java
// BEFORE: ~50 lines of cross-context orchestration
BookDTO updatedBook = BookMapper.toDTOFromDomain(bookService.assignBookToShelf(...));
ShelfDTO shelf = shelfService.findShelfById(...).map(...).orElseThrow(...);
BookcaseDTO bookcase = bookcaseService.findBookCaseById(shelf.bookcaseId()).orElseThrow(...);
BookPlacementResponse response = new BookPlacementResponse(...);
return ResponseEntity.ok(response);

// AFTER: single delegation
bookFacade.placeBookOnShelf(bookId, shelfAssignmentRequest);
return ResponseEntity.ok(HttpStatus.OK);
```

**Architecture notes:**
- BookController now depends only on: `BookService`, `BookFacade`, `IsbnLookupService`, `AuthorFacade` — all inbound ports or book-module-internal services.
- Zero imports from `library.stacks.*` remain. The hex boundary is restored.
- `BookMapper` moved to `mappers/` subpackage — minor organizational improvement.

**Risk / edge cases:**
- **Breaking API change:** Response type changed from `BookPlacementResponse` (with book title, shelf label, bookcase location) to raw `HttpStatus.OK`. Any frontend or client consuming the response body will break.
- `ResponseEntity.ok(HttpStatus.OK)` returns `HttpStatus.OK` as the response body (the enum value serialized as JSON: `"OK"`). This is unusual — typically you'd use `ResponseEntity.ok().build()` or `ResponseEntity.noContent().build()` for void operations.
- `BookController` still depends on `BookService` (concrete class) in addition to `BookFacade` (interface). Ideally, the controller should depend only on `BookFacade`.

---

### Commits 5-6: `2064cbf`, `67dbef7` — Clean up BookDomainRepositoryImpl

**Intent:** Remove now-unused dependencies (`AuthorService`, `ShelfFacade`, `@Lazy` annotation) from `BookDomainRepositoryImpl` and clean up whitespace.

**Files touched:**
- `BookDomainRepositoryImpl.java` — Constructor simplified from 4 parameters to 2 (`BookMapper`, `BookJpaRepository`). Removed imports for `AuthorService`, `ShelfFacade`, `@Lazy`.

**Architecture notes:** This is the cleanup that was enabled by commit `ed999a4`. The repository is now a pure persistence adapter with no cross-module dependencies.

---

### Commits 7-8: `4e86637`, `ab831a2` — Remove BookControllerTest

**Intent:** The test file added in commit `6c61a49` was deleted across these two commits (first the content, then the last empty line).

**Risk:** The test that was supposed to verify the new controller behavior was removed. This leaves `BookController.placeBookOnShelf` untested.

---

## End-to-End Flow: placeBookOnShelf

**Request → Response (after this change):**

```
HTTP POST /api/books/{bookId}/shelf
  Body: { "shelfId": 42 }

→ BookController.placeBookOnShelf(bookId, request)
  → BookFacade.placeBookOnShelf(bookId, request)          [inbound port]
    → BookService.placeBookOnShelf(bookId, request)        [application service]
      → bookCommandUseCases.placeBookOnShelf(bookId, req)  [use case]
        → ShelfAccessPort.findShelfById(42)                [outbound port]
          → ShelfAccessPortAdapter                         [infrastructure adapter]
            → ShelfFacade.findShelfById(42)                [cross-module inbound port]
              → ShelfService → QueryShelfUseCase → repo
        → ShelfAccessPort.isFull(42)                       [outbound port]
          → ShelfAccessPortAdapter
            → ShelfFacade.isFull(42)
              → ShelfService → QueryShelfUseCase → Shelf.isFull()
        → BookDomainRepository.placeBookOnShelf(bookId, 42) [outbound port]
          → BookDomainRepositoryImpl                        [infrastructure]
            → BookJpaRepository.save(entity)
→ ResponseEntity.ok(HttpStatus.OK)
```

**Why this design is better:**
1. **Single Responsibility:** BookController is a thin HTTP adapter. It translates HTTP → domain call → HTTP response. No business logic.
2. **Dependency Direction:** Book's core defines `ShelfAccessPort`. Infrastructure implements it. Core never sees `ShelfFacade` or `ShelfService`.
3. **Testability:** `bookCommandUseCases` can be unit-tested with mocked `ShelfAccessPort` and `BookDomainRepository` — no Spring context needed.
4. **Bounded Context Isolation:** Cataloging (Book) and Stacks (Shelf/Bookcase) communicate only through defined ports. BookController has zero Stacks imports.

**What dependencies were removed:**
- `BookController` → `ShelfService` (concrete class): **REMOVED**
- `BookController` → `BookcaseService` (concrete class): **REMOVED**
- `BookController` → `IsbnEnrichmentService`: **REMOVED** (was unused)
- `BookDomainRepositoryImpl` → `ShelfFacade`: **REMOVED**
- `BookDomainRepositoryImpl` → `AuthorService`: **REMOVED** (was unused)

**What dependencies were added:**
- `bookCommandUseCases` → `ShelfAccessPort` (interface): **ADDED** (correct direction)
- `bookCommandUseCases` → `BookDomainRepository` (interface): **ADDED** (correct direction)
- `BookService` → `bookCommandUseCases` (concrete class): **ADDED** (intra-module, acceptable)
- `ShelfAccessPort` → `isFull(Long)`: **ADDED** (port extension)
- `ShelfFacade` → `isFull(Long)`, `isEmpty(Long)`: **ADDED** (port extension)

**Remaining layering concern:** `BookController` still depends on `BookService` (concrete) in addition to `BookFacade` (interface). The controller should ideally depend only on `BookFacade`.

---

## Testing Assessment

**Tests that exist:** None. The `BookControllerTest` was added in commit `6c61a49` and then deleted in commits `4e86637`/`ab831a2`. The final state has no test file.

**Suggested test cases:**

1. **`bookCommandUseCases.placeBookOnShelf` — happy path:** Mock `ShelfAccessPort` to return a shelf that is not full, mock `BookDomainRepository.placeBookOnShelf` to succeed. Verify both ports are called with correct arguments.

2. **`bookCommandUseCases.placeBookOnShelf` — shelf not found:** Mock `ShelfAccessPort.findShelfById` to return `Optional.empty()`. Assert `IllegalStateException` with message containing "Shelf not found".

3. **`bookCommandUseCases.placeBookOnShelf` — shelf is full:** Mock `ShelfAccessPort.findShelfById` to return a shelf, mock `isFull` to return `true`. Assert `IllegalStateException` with message containing "is full".

4. **`bookCommandUseCases.placeBookOnShelf` — null request / null shelfId:** Assert `IllegalStateException` with "Shelf id is required".

5. **`BookController.placeBookOnShelf` integration test:** Use `@WebMvcTest` with mocked `BookFacade`, verify POST to `/{bookId}/shelf` returns 200 and delegates to `bookFacade.placeBookOnShelf`.

**Run commands:**
```bash
mvn test
mvn test -Dtest=bookCommandUseCasesTest
mvn test -Dtest=BookControllerTest
```

---

## Lessons Learned (from the diff)

1. **Controllers that orchestrate across bounded contexts are a design smell.** The old `BookController.placeBookOnShelf` was 50 lines of cross-context wiring. That's application-layer logic leaking into the web adapter. The fix: push orchestration behind the facade.

2. **Repository classes should not contain business validation.** `BookDomainRepositoryImpl` was checking `shelfFacade.isFull()` — a business rule enforced inside a persistence adapter. Moving it to `bookCommandUseCases` puts the rule where it can be tested without a database.

3. **Ports grow incrementally.** `ShelfAccessPort` started with just `findShelfById`. When a new use case needed `isFull`, the port was extended. The adapter was updated. The Shelf module exposed the capability through its facade. Each layer did its part.

4. **Unused dependencies accumulate silently.** `BookDomainRepositoryImpl` had `AuthorService` and `@Lazy ShelfFacade` injected but only `ShelfFacade` was used (and only in one method). `BookController` had `IsbnEnrichmentService` injected but never used it. These were removed as part of the cleanup.

5. **Test files need to survive the commit that adds them.** `BookControllerTest` was added and deleted within the same push. This suggests the test was scaffolded but not implemented, then cleaned up prematurely.

6. **Java naming conventions matter for readability.** `bookCommandUseCases` (lowercase class name) will trip up every code reviewer and IDE inspection. This is a small fix with high readability payoff.

7. **Response contract changes are breaking changes.** Switching from `BookPlacementResponse` (structured data) to `HttpStatus.OK` (enum string) silently breaks any client expecting the old response shape. This should be documented or versioned.

---

## Next Steps

### Immediate follow-ups (today)
- **Rename `bookCommandUseCases` → `BookCommandUseCases`** to follow Java class naming conventions.
- **Fix `ResponseEntity.ok(HttpStatus.OK)`** — this serializes the enum as the response body. Use `ResponseEntity.ok().build()` or `ResponseEntity.noContent().build()` instead.
- **Rename `isFull(Long aLong)` → `isFull(Long shelfId)`** in `ShelfAccessPort` for clarity.

### Short-term hardening (this week)
- **Write unit tests for `bookCommandUseCases`** — all four scenarios (happy path, shelf not found, shelf full, null input).
- **Write `@WebMvcTest` for `BookController.placeBookOnShelf`** — verify HTTP status codes and delegation.
- **Remove `BookService` concrete dependency from `BookController`** — depend only on `BookFacade`.
- **Add `Shelf.isEmpty()` domain method** for symmetry with `Shelf.isFull()`, and use it in `QueryShelfUseCase.isEmpty`.
- **Consider error translation:** `bookCommandUseCases` throws `IllegalStateException` for everything. The controller or an `@ExceptionHandler` should map these to 400/404/409 as appropriate.

### Strategic refactors (later)
- **Reduce redundant shelf lookups:** `placeBookOnShelf` calls `findShelfById` then `isFull`, each loading the shelf independently. Consider a single `validateShelfAcceptsBook(shelfId)` method on `ShelfAccessPort` that does both checks in one call.
- **Evaluate whether `bookCommandUseCases` should be split** — if more commands are added (e.g., `removeBookFromShelf`, `moveBook`), each could be its own use case class following the existing `QueryShelfUseCase` pattern.
- **Audit remaining cross-module dependencies** — run ArchUnit tests to ensure no other controllers or repositories bypass inbound ports.
