# PR Review: Enforce hex boundary — move placeBookOnShelf behind BookFacade

Date: 2026-02-23 (America/Chicago)

Branch: `300-bookcontroller-bypasses-inbound-port-depends-on-shelfservice-concrete-class`

Range: `34968e8..ab831a2`

Commits:
- `2c3b1f0` Add isFull and isEmpty capacity queries to Shelf module
- `9da6619` Add isFull to Book module's ShelfAccessPort
- `ed999a4` Move placeBookOnShelf orchestration behind BookFacade
- `6c61a49` Remove Stacks dependencies from BookController
- `2064cbf` Refactor BookDomainRepositoryImpl to remove unused dependencies
- `67dbef7` Refactor BookDomainRepositoryImpl constructor cleanup
- `4e86637` Remove unused BookControllerTest imports
- `ab831a2` Remove unused BookControllerTest imports

---

## 0) Executive Summary

- **Ship or No-Ship:** ⚠️ Ship with fixes
- **Top 3 risks:**
  1. **Race condition:** `bookCommandUseCases` checks `isFull` then calls `placeBookOnShelf` — a concurrent request can place a book between the check and the write, exceeding shelf capacity
  2. **No `@Transactional` on `placeBookOnShelf`** — the validate-then-persist flow in `bookCommandUseCases` has no transaction boundary; partial failure leaves no rollback guarantee
  3. **Breaking API contract:** `POST /{bookId}/shelf` response changed from `BookPlacementResponse` body to raw `HttpStatus` enum serialized as JSON — clients will get `"OK"` string instead of placement data
- **Top 3 recommended fixes:**
  1. Add `@Transactional` to `bookCommandUseCases.placeBookOnShelf()`
  2. Fix `ResponseEntity.ok(HttpStatus.OK)` — this serializes the enum; use `ResponseEntity.ok().build()` or `ResponseEntity.status(HttpStatus.OK).build()`
  3. Add unit tests for `bookCommandUseCases` (currently zero tests shipped)
- **Test confidence:** Low — no tests survived the final diff (BookControllerTest was added then removed across fix commits)

---

## 1) What Changed (diff-anchored)

- **BookController:** Removed 5 Stacks-context dependencies (`ShelfService`, `BookcaseService`, `IsbnEnrichmentService`, `ShelfDTO`, `BookcaseDTO`). Constructor went from 7 params to 4. `placeBookOnShelf` reduced from 50-line orchestration to single `bookFacade.placeBookOnShelf()` delegation.
- **BookFacade (inbound port):** Added `placeBookOnShelf(Long bookId, BookShelfAssignmentRequest)` — new public contract method.
- **BookService:** Implements new facade method by delegating to `bookCommandUseCases`.
- **bookCommandUseCases (new):** Use case class validates shelf existence and capacity via `ShelfAccessPort`, then delegates persistence to `BookDomainRepository`.
- **ShelfAccessPort (outbound port):** Added `boolean isFull(Long)`.
- **ShelfAccessPortAdapter:** Implements `isFull` by delegating to `ShelfFacade.isFull()`.
- **ShelfFacade (inbound port):** Added `boolean isFull(Long)` and `boolean isEmpty(Long)`.
- **ShelfService:** Implements `isFull`/`isEmpty` by delegating to `QueryShelfUseCase`.
- **QueryShelfUseCase:** Added `isFull` and `isEmpty` methods that load shelf from repository and delegate to domain model.
- **BookDomainRepositoryImpl:** Removed `ShelfFacade` and `AuthorService` dependencies. `placeBookOnShelf` stripped to pure persistence (find, set, save). Constructor went from 4 params to 2.
- **BookMapper:** Moved from `controllers/cataloging/book/` to `controllers/cataloging/book/mappers/` subpackage.
- **API contract change:** `POST /{bookId}/shelf` return type changed from `ResponseEntity<BookPlacementResponse>` to `ResponseEntity<HttpStatus>`.

---

## 2) Blockers (must fix before merge)

### B1: `ResponseEntity.ok(HttpStatus.OK)` serializes the enum, not a status code

**Finding:** `BookController.placeBookOnShelf()` returns `ResponseEntity.ok(HttpStatus.OK)`. `ResponseEntity.ok(T body)` treats the argument as the response body. The client receives the JSON serialization of the `HttpStatus` enum: `"OK"` as a string, not a proper empty 200 response.

**Evidence:** `BookController.java` line 101: `return ResponseEntity.ok(HttpStatus.OK);`

**Why it matters:** Any client parsing the response as JSON will get `"OK"` (a string), not a proper HTTP response. If the client expects a JSON object or an empty body, it will fail or behave unexpectedly. The generic type `ResponseEntity<HttpStatus>` is also semantically misleading — it declares the body type is `HttpStatus`.

**Concrete fix:**
```java
@PostMapping("/{bookId}/shelf")
public ResponseEntity<Void> placeBookOnShelf(
    @PathVariable Long bookId, @RequestBody BookShelfAssignmentRequest shelfAssignmentRequest) {
  bookFacade.placeBookOnShelf(bookId, shelfAssignmentRequest);
  return ResponseEntity.ok().build();
}
```

**Verification:** `curl -X POST localhost:8080/api/v1/books/1/shelf -H 'Content-Type: application/json' -d '{"shelfId":1}' -v` — verify response body is empty, not `"OK"`.

---

### B2: No `@Transactional` on the validate-then-persist flow

**Finding:** `bookCommandUseCases.placeBookOnShelf()` performs three I/O operations (findShelfById, isFull, placeBookOnShelf) with no transaction boundary. If the `bookDomainRepository.placeBookOnShelf()` call fails after validation passes, there's no rollback. More critically, the validation reads and the write are not atomic.

**Evidence:** `bookCommandUseCases.java` — no `@Transactional` annotation on the class or method. Three sequential calls to two different ports.

**Why it matters:** Under concurrent requests, shelf capacity can be exceeded. The check-then-act pattern without a transaction (or optimistic locking) means two threads can both see `isFull() == false` and both proceed to place a book.

**Concrete fix:**
```java
@Transactional
public void placeBookOnShelf(Long bookId, BookShelfAssignmentRequest shelfAssignmentRequest) {
```

**Verification:** Write an integration test that concurrently places two books on a shelf with capacity 1 — only one should succeed.

*Note: `@Transactional` alone does not fully solve the race condition — you'd need database-level locking (SELECT FOR UPDATE) or optimistic locking on the Shelf entity. But `@Transactional` is the minimum floor.*

---

### B3: No tests shipped

**Finding:** The `BookControllerTest` was added in commit `6c61a49`, then fully deleted in commits `4e86637` and `ab831a2`. The final diff contains zero test files.

**Evidence:** `git diff --stat 34968e8..ab831a2` shows no files under `src/test/`. Commit `4e86637` removes 64 lines from `BookControllerTest.java`.

**Why it matters:** New use case class (`bookCommandUseCases`) with validation logic, new port methods (`isFull`, `isEmpty`), and a changed API contract — all untested. The refactor shifts where validation happens (repository -> use case), and there's no verification that the new location behaves identically.

**Concrete fix:** Add at minimum:
1. `bookCommandUseCasesTest` — unit test with mocked `ShelfAccessPort` and `BookDomainRepository`
2. `BookControllerTest` — integration test verifying `POST /{bookId}/shelf` returns 200 on success

**Verification:** `mvn test -Dtest="bookCommandUseCasesTest,BookControllerTest"`

---

## 3) High-Risk Issues (should fix soon)

### H1: Triple shelf lookup per request

**Finding:** `bookCommandUseCases.placeBookOnShelf()` makes three separate calls to the Shelf context:
1. `shelfAccessPort.findShelfById()` — loads shelf, checks existence
2. `shelfAccessPort.isFull()` — loads shelf again (via `ShelfFacade.isFull()` -> `QueryShelfUseCase.isFull()` -> `shelfDomainRepositoryPort.getById()`)
3. These are two round-trips that each independently load the shelf domain object

**Evidence:** `bookCommandUseCases.java` lines 28 and 33. `QueryShelfUseCase.isFull()` (line 48) does its own `shelfDomainRepositoryPort.getById()`.

**Why it matters:** Two database queries for one operation. In the current monolith this is just overhead. If `ShelfAccessPort` becomes an HTTP client (microservices), this doubles latency.

**Concrete fix:** Combine into one call. Either:
- Use `findShelfById()` result to check capacity locally (but `ShelfDTO` would need a `bookCount` field), or
- Add `ShelfAccessPort.validatePlacement(Long shelfId)` that checks both existence and capacity in one call

**Verification:** Query logging (`spring.jpa.show-sql=true`) should show one shelf query per placement, not two.

---

### H2: Wrong exception type for "shelf not found" vs "shelf id required"

**Finding:** `bookCommandUseCases` throws `IllegalStateException` for three semantically different errors: missing request data (line 25), shelf not found (line 29), and shelf full (line 34). Callers cannot distinguish between a client error (bad input) and a domain constraint (shelf full) or a not-found case.

**Evidence:** `bookCommandUseCases.java` lines 25, 29, 34 — all `IllegalStateException`.

**Why it matters:** Without distinct exception types, the controller cannot map to appropriate HTTP status codes (400 vs 404 vs 409). The old controller had explicit `ResponseStatusException` mapping: `BAD_REQUEST`, `NOT_FOUND`, `CONFLICT`.

**Concrete fix:** Use distinct exceptions:
- `IllegalArgumentException` for null/missing shelfId (400)
- `IllegalStateException("Shelf not found")` or a custom `NotFoundException` (404)
- `IllegalStateException("Shelf is full")` (409)

Or add a `@ControllerAdvice` that maps these to HTTP statuses based on message patterns.

**Verification:** `curl` with invalid shelfId should return 400, nonexistent shelfId should return 404, full shelf should return 409.

---

### H3: `ShelfDTO` import still in `BookDomainRepositoryImpl`

**Finding:** `BookDomainRepositoryImpl` still imports `com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO` (line 12) and uses it on line 39 (`ShelfDTO shelfDTO = null;`). This is a cross-context import that survives the cleanup.

**Evidence:** `BookDomainRepositoryImpl.java` line 12.

**Why it matters:** The goal of this PR was to remove Stacks dependencies from the Book module's infrastructure. This import remains.

**Concrete fix:** If `ShelfDTO` is only used as an unused local variable (`shelfDTO = null` on line 39), remove both the import and the variable.

**Verification:** `rg "ShelfDTO" src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/repository/BookDomainRepositoryImpl.java` returns no results.

---

### H4: `bookCommandUseCases` class naming violates Java convention

**Finding:** Class name `bookCommandUseCases` starts with lowercase.

**Evidence:** `bookCommandUseCases.java` line 11: `public class bookCommandUseCases`

**Why it matters:** Violates Java naming convention (PascalCase for classes). IDEs may flag this. Other developers will notice. In code review, this signals carelessness about conventions.

**Concrete fix:** Rename to `BookCommandUseCases` or `PlaceBookOnShelfUseCase` (to follow the existing `DeleteShelvesUseCase` pattern).

**Verification:** Compile succeeds after rename; all references updated.

---

## 4) Architecture and boundary audit (DDD and Hex)

### Dependency direction wins

- **BookController:** Zero Stacks imports. Depends only on `BookFacade` (inbound port) + framework. This is the primary goal of the PR and it's achieved.
- **BookDomainRepositoryImpl:** Removed `ShelfFacade` (Stacks inbound port) from constructor. Repository no longer crosses module boundaries for business logic.
- **bookCommandUseCases:** Depends only on `BookDomainRepository` and `ShelfAccessPort` — both outbound ports owned by the Book module. Correct.

```
BookController (web adapter)
    |---> BookFacade (inbound port)
              |---> BookService (application)
                        |---> bookCommandUseCases (use case)
                                  |---> ShelfAccessPort (outbound port, Book-owned)
                                  |         |---> ShelfAccessPortAdapter (Book infra)
                                  |                   |---> ShelfFacade (Shelf inbound port)
                                  |---> BookDomainRepository (outbound port, Book-owned)
                                            |---> BookDomainRepositoryImpl (Book infra)
```

### Remaining violations

1. **`BookDomainRepositoryImpl` imports `ShelfDTO`** (line 12) — cross-context type from `shelf.api.dtos`. Likely leftover from a method in the same class (line 39: `ShelfDTO shelfDTO = null;`). Should be removed.

2. **`BookService` imports `BookcaseJpaRepository`** (pre-existing, not introduced in this PR) — `BookService` directly uses `BookcaseJpaRepository` from Stacks infrastructure in `getBookLocation()`. This is a known out-of-scope issue but worth documenting.

3. **`ShelfAccessPort` returns `ShelfDTO`** — the outbound port returns a type owned by the Shelf module (`shelf.api.dtos.ShelfDTO`). Purist hex says the port should define its own return type owned by the Book module. Pragmatically acceptable since `shelf.api` is meant as a shared contract layer.

### Cross-context coupling map

| From (module) | To (module) | Via | Verdict |
|---|---|---|---|
| BookController | BookFacade | Inbound port | Correct |
| bookCommandUseCases | ShelfAccessPort | Outbound port (Book-owned) | Correct |
| ShelfAccessPortAdapter | ShelfFacade | Inbound port (Shelf-owned) | Correct |
| BookDomainRepositoryImpl | ShelfDTO | Direct import | Violation (pre-existing, partially cleaned) |

---

## 5) Naming and API semantics audit

### `bookCommandUseCases` — misleading name
- Lowercase, plural, generic. Sounds like a collection of use cases rather than a class.
- Currently contains exactly one command: `placeBookOnShelf`.
- **Recommend:** `PlaceBookOnShelfUseCase` (follows `DeleteShelvesUseCase`, `CreateShelfUseCase` patterns in codebase).

### `isFull(Long aLong)` — parameter name in `ShelfAccessPort`
- `aLong` is auto-generated and meaningless.
- **Recommend:** `isFull(Long shelfId)`

### `ResponseEntity<HttpStatus>` — type lie
- Declares the body type is `HttpStatus`. The body is actually the serialized `HttpStatus.OK` enum.
- **Recommend:** `ResponseEntity<Void>` with `.ok().build()`

### `placeBookOnShelf` — correct
- Command name matches behavior. Imperative. Clear.
- Consistently used across `BookFacade`, `BookService`, `bookCommandUseCases`, `BookDomainRepository`.

### `findShelfById` followed by `isFull` — redundant existence check
- `bookCommandUseCases` checks shelf existence with `findShelfById`, then calls `isFull` which internally also loads the shelf and throws if not found.
- The existence check on line 28 is redundant because `isFull` on line 33 will also throw `IllegalStateException` if the shelf doesn't exist (see `QueryShelfUseCase.isFull()` line 49-51).
- **Recommend:** Remove the explicit `findShelfById` check. Let `isFull` handle not-found.

---

## 6) Data, persistence, transaction review

### Missing `@Transactional` on the write path

`bookCommandUseCases.placeBookOnShelf()` performs a read-validate-write sequence with no `@Transactional`. The chain is:
1. `shelfAccessPort.findShelfById()` — read from Shelf DB
2. `shelfAccessPort.isFull()` — read from Shelf DB
3. `bookDomainRepository.placeBookOnShelf()` — write to Book DB

Without a transaction, step 3 can succeed while step 1-2 were stale. Two concurrent requests can both pass validation and exceed capacity.

**Recommendation:** At minimum, add `@Transactional` to the method. For true correctness, the Shelf module would need to participate in the capacity check atomically (e.g., optimistic locking on Shelf entity).

### `BookDomainRepositoryImpl.placeBookOnShelf` — no `@Transactional`

The repository method does `findById` + `setShelfId` + `save`. If the entity is managed (within a Hibernate session), `save` may be redundant. If not managed, the `set` + `save` are two operations. This is likely fine in a Spring Data context (auto-flush on transaction commit) but without an explicit transaction, it relies on Spring's default `REQUIRED` propagation from an outer transaction that doesn't exist.

### Redundant book existence check

`bookCommandUseCases` doesn't validate that the book exists. `BookDomainRepositoryImpl.placeBookOnShelf()` does (line 201: `bookJpaRepository.findById(bookId)` with empty check). If the book doesn't exist, the user gets a `RuntimeException("Book not found")` — which will surface as a 500 without a `@ControllerAdvice`. The old controller caught `IllegalArgumentException` and mapped to 404.

---

## 7) Security and safety (lite, but real)

### No AuthZ check on book placement
- `POST /{bookId}/shelf` has no authentication or authorization annotation. Any caller can place any book on any shelf.
- *Inference:* This is likely intentional for a personal project, but worth flagging for interview prep.

### No input sanitization on `bookId` and `shelfId`
- Both are `Long` path/body params — Spring handles type coercion. No SQL injection risk via JPA.
- `shelfAssignmentRequest.shelfId()` is validated for null but not for negative values. Negative IDs would pass validation and hit the database (likely returning not-found).

### `@CrossOrigin(origins = "*")` on other endpoints
- Present on `findBookByIsbn` and `addNewBook` but not on `placeBookOnShelf`. Inconsistent CORS policy.
- *Not introduced in this PR* — pre-existing.

---

## 8) Tests and "minimum confidence suite"

### Current state
- **Zero tests in the final diff.** `BookControllerTest` was added in commit `6c61a49` (65 lines) then fully removed in commits `4e86637` and `ab831a2`.
- `BibbyApplicationTests` (Spring context load) exists pre-PR and was the test that surfaced the circular dependency.

### Gaps tied to risk
1. `bookCommandUseCases.placeBookOnShelf()` — untested. Contains all validation logic.
2. `QueryShelfUseCase.isFull()` / `isEmpty()` — untested. New methods.
3. `BookController.placeBookOnShelf()` — untested. Changed API contract.
4. `BookDomainRepositoryImpl.placeBookOnShelf()` — untested after logic removal.
5. `ShelfAccessPortAdapter.isFull()` — untested. New adapter method.

### Targeted test cases

1. **Unit: `bookCommandUseCases` — null request throws `IllegalStateException`**
2. **Unit: `bookCommandUseCases` — null shelfId throws `IllegalStateException`**
3. **Unit: `bookCommandUseCases` — shelf not found throws `IllegalStateException`**
4. **Unit: `bookCommandUseCases` — shelf full throws `IllegalStateException`**
5. **Unit: `bookCommandUseCases` — happy path calls `bookDomainRepository.placeBookOnShelf(bookId, shelfId)`**
6. **Unit: `QueryShelfUseCase.isFull` — null shelf throws `IllegalStateException`**
7. **Unit: `QueryShelfUseCase.isFull` — delegates to `Shelf.isFull()`**
8. **Integration: `POST /api/v1/books/{id}/shelf` — returns 200 with empty body**
9. **Integration: `POST /api/v1/books/{id}/shelf` with nonexistent shelf — returns appropriate error**
10. **Context: `BibbyApplicationTests` — context loads (no circular deps)**

### Commands

```bash
mvn test                                          # full suite
mvn -Dtest=BibbyApplicationTests test             # context load
mvn -Dtest=bookCommandUseCasesTest test           # use case unit tests (to be created)
mvn -Dtest=QueryShelfUseCaseTest test             # shelf query tests (to be created)
```

---

## 9) Suggested micro-refactors (small, high leverage)

1. **Fix `ResponseEntity.ok(HttpStatus.OK)`** → `ResponseEntity.ok().build()` with return type `ResponseEntity<Void>`. One-line fix. Prevents broken client responses.

2. **Remove redundant `findShelfById` check in `bookCommandUseCases`** (line 28). `isFull()` already throws on not-found. Removes one unnecessary DB call per request.

3. **Rename `bookCommandUseCases` → `PlaceBookOnShelfUseCase`**. Matches codebase convention (`DeleteShelvesUseCase`, `CreateShelfUseCase`). Java naming compliance.

4. **Rename `isFull(Long aLong)` → `isFull(Long shelfId)` in `ShelfAccessPort`**. Parameter name fix.

5. **Remove `ShelfDTO` import and unused variable from `BookDomainRepositoryImpl`** (line 12, line 39). Completes the cross-context cleanup.

6. **Add `@Transactional` to `bookCommandUseCases.placeBookOnShelf()`**. Minimum safety for the validate-then-write flow.

7. **Use `Optional.ifPresentOrElse` pattern** in `BookDomainRepositoryImpl.placeBookOnShelf()` instead of `bookEntity.get()` repeated 3 times (lines 206-209). Extract the entity once: `BookEntity entity = bookEntity.orElseThrow(...)`.

8. **Differentiate exception types** in `bookCommandUseCases`: `IllegalArgumentException` for bad input (null shelfId), keep `IllegalStateException` for domain constraints (shelf full, not found).

---

## 10) Questions I would ask in a real PR review

1. **"The response type changed from `BookPlacementResponse` to `HttpStatus`. Is there a frontend or API consumer that depends on the old response body?"** — `BookController.java` line 96. This is a silent breaking change.

2. **"Why was `BookControllerTest` added and then fully removed? Was the test wrong, or do you plan to re-add it?"** — Commits `6c61a49` → `4e86637`. Zero tests in the final diff.

3. **"What happens when two requests concurrently try to fill the last slot on a shelf? Both pass `isFull()` and both succeed."** — `bookCommandUseCases.java` lines 33-38. No locking or transaction boundary.

4. **"`bookDomainRepository.placeBookOnShelf()` still does its own book-not-found check (throws RuntimeException). Should the use case validate book existence too, with a proper exception type?"** — `BookDomainRepositoryImpl.java` line 203: `throw new RuntimeException(...)`.

5. **"Is `ShelfDTO` on line 12 of `BookDomainRepositoryImpl` still used after this refactor? If it's only on the unused `shelfDTO = null` line, can we remove both?"** — `BookDomainRepositoryImpl.java` lines 12 and 39.

6. **"The existing `BookService.assignBookToShelf()` (line 121) is still public and does the same thing via a different path (`bookDomainRepository.placeBookOnShelf`). Is this now dead code? Two methods doing the same thing is a maintenance trap."** — `BookService.java` line 121 vs line 272.

7. **"`QueryShelfUseCase.isEmpty()` uses `shelf.getBookCount() == 0` while `isFull()` uses `shelf.isFull()`. Why not `shelf.isEmpty()` if the domain model has it? If not, should it?"** — `QueryShelfUseCase.java` lines 52 vs 60.

8. **"`ShelfAccessPort.isFull()` only checks capacity. Should it also verify the book isn't already on this shelf (duplicate placement)?"** — `bookCommandUseCases.java` — no duplicate check.

9. **"The old controller caught `IllegalStateException` and mapped to 409 CONFLICT. Now `IllegalStateException` from the use case will surface as 500. Is there a `@ControllerAdvice` that handles this?"** — `BookController.java` — no exception handling.

10. **"`BookFacade.placeBookOnShelf` accepts `BookShelfAssignmentRequest` (a DTO). Should an inbound port accept a DTO, or should it accept primitives (`Long bookId, Long shelfId`) and leave DTO unpacking to the adapter?"** — `BookFacade.java` line 75.
