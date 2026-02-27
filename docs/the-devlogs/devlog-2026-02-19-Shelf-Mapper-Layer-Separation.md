# Devlog: Shelf Mapper Layer Separation — Infrastructure vs Application

**Date:** February 19, 2026
**Branch:** `refactor/add-book-id-validation-and-formatting`
**Range:** `f8a7df1..7856ce1` (PR #271, the second batch of commits pushed from this branch)
**Commits:**

- `a38a403` — refactor: consolidate ShelfMapper to entity-domain mapping only
- `fb32571` — feat: add ShelfDTOMapper for application-layer DTO conversions
- `a2e1f18` — refactor: update Shelf constructor and adapt all usages
- `7856ce1` — docs: add DDD layering principles and mapper guidelines

> **Scope note:** This branch produced two PRs. PR #270 (`f71bd67`, `e2c9a90`) handled minor hardening (null-safety on
`Book.getBookId()`, whitespace formatting in `ShelfEntity`). This devlog covers PR #271 — the substantive architectural
> work.

---

## Background — What Problem Was I Solving?

`ShelfMapper` had become a grab-bag. It lived in `infrastructure.mapping` but it knew about `ShelfDTO`,
`ShelfOptionResponse`, and application-level concerns. The result was two violations that compounded each other:

1. **Layer violation in `ShelfService`**: the application service imported directly from
   `infrastructure.mapping.ShelfMapper`. The application layer was reaching downward into the infrastructure layer to
   get something that had nothing to do with persistence.
2. **Inconsistent hydration**: `toDomainFromEntity(ShelfEntity)` (no `books` arg) and
   `toDomainFromDTO(ShelfEntity, List<Long>)` (with `books`) both existed — callers had to know which to call, and
   several repository methods (`findByBookcaseId`, `findAll`, `getShelfShelfOptionResponse`) silently chose the version
   that returned a `Shelf` with a `null` `books` list. Any downstream call to `shelf.getBookIds().size()` on those
   objects was a latent NPE.

The `Shelf` constructor also accepted construction without `books`, which meant the compiler couldn't protect you from
building an incomplete domain object.

---

## Summary of Outcomes

- `ShelfMapper` (infrastructure) now exclusively owns the `ShelfEntity ↔ Shelf` boundary — persistence in, persistence
  out.
- `ShelfDTOMapper` (application, `core/mappers`) now exclusively owns the `Shelf → ShelfDTO / ShelfOptionResponse`
  boundary.
- `Shelf`'s constructor is sealed: `books` is required at construction time — no more partially-hydrated domain objects.
- `ShelfService` no longer imports anything from the infrastructure layer.
- All five repository read methods now consistently hydrate `books` via `bookAccessPort`.
- `ShelfServiceTest` updated throughout; two new `isFull()` test cases added.
- Architectural decision documented in `docs/engineering/ddd-layers-and-mappers.md`.

---

## Commit-by-Commit Breakdown

### `a38a403` — Consolidate ShelfMapper to entity-domain mapping only

**Intent:** Strip `ShelfMapper` down to a single responsibility: translating between `ShelfEntity` (JPA) and `Shelf` (
domain). Delete everything that doesn't belong there.

**Files touched:**

- `ShelfMapper.java` — 45 lines deleted, 9 lines added (net −36 lines)

**Key code changes:**

- **Removed** `toDomain(ShelfEntity)` — was a duplicate of `toDomainFromEntity`.
- **Removed** `toDomainFromDTO(ShelfEntity, List<Long>)` — was a leaky method that called `toDomain()` and then
  *immediately re-set every field via setters*. A sign the original code was stitched together rather than designed.
- **Removed** `toDTO(Shelf, Long)` and `toShelfOption(Shelf)` — application-layer concerns, moved to `ShelfDTOMapper`.
- **Implemented** `toEntity(Shelf)` — previously a stub (`return null`). Now properly maps `ShelfId → Long`,
  `shelfLabel`, `shelfPosition`, `bookCapacity`.
- **Updated** `toDomainFromEntity` signature: `(ShelfEntity)` → `(ShelfEntity, List<Long> books)`. `books` is now passed
  in and forwarded to the `Shelf` constructor.

**Architecture notes:** This is a pure scope reduction. `ShelfMapper` was crossing the persistence↔application boundary;
after this commit it only crosses persistence↔domain. It still imports `ShelfDTO` for `toDTOFromEntity` — worth
verifying no dead imports remain.

**Risk / edge cases:**

- `toEntity` still does not set `bookcaseId` on the returned `ShelfEntity`. If `save()` is ever called through this
  path, the bookcase association will be null. This is a pre-existing gap that was hidden by the stub; now it's an
  implemented-but-incomplete method.
- This is a **breaking change** for any caller of `toDomainFromEntity(shelfEntity)` (no args) — but all callers are in
  `ShelfDomainRepositoryImpl`, updated in commit `a2e1f18`.

**Verification:** `mvn test -pl . -Dtest=ShelfServiceTest`

---

### `fb32571` — Add ShelfDTOMapper for application-layer DTO conversions

**Intent:** Create the companion mapper that lives in the right layer and owns the API boundary.

**Files touched:**

- `ShelfDTOMapper.java` — new file, 40 lines in `core/mappers/`

**Key code changes:**

```java
// core/mappers/ShelfDTOMapper.java
@Component
public class ShelfDTOMapper {
    public ShelfDTO toDTO(Shelf shelf, Long bookcaseId) { ...}

    public ShelfOptionResponse toShelfOption(Shelf shelf) { ...}

    public Shelf toDomainFromDTO(Shelf shelf, List<Long> books) { ...}
}
```

- `toDTO` and `toShelfOption` are straight lifts from the old `ShelfMapper`.
- `toDomainFromDTO(Shelf shelf, List<Long> books)` — **name is misleading**. It takes a `Shelf`, not a DTO. It's
  actually a "hydrate shelf with books" utility. This should be renamed (see follow-ups).

**Architecture notes:**

- Package: `com.penrose.bibby.library.stacks.shelf.core.mappers` — correctly inside the application core.
- It imports from `api.dtos` (`ShelfDTO`, `ShelfOptionResponse`). In this codebase's hexagonal structure the api layer
  appears to be the outbound adapter / port definitions, so this dependency direction (core → api DTOs) may need
  scrutiny. If `api.dtos` are truly output port types, this is fine. If they're adapter-layer types, this is still a
  mild layering issue.
- `@Component` makes it injectable by Spring without requiring manual wiring.

**Risk / edge cases:**

- `toShelfOption` calls `shelf.getBookIds().size()` — with the new constructor guarantee this is safe. It was a
  potential NPE before.

**Verification:** No dedicated unit tests for `ShelfDTOMapper` yet (see test recommendations).

---

### `a2e1f18` — Update Shelf constructor and adapt all usages

**Intent:** Seal the domain model against partial construction, then wire all callsites to the new mapper arrangement.

**Files touched:**

- `Shelf.java` — constructor signature change (+1 `List<Long> books` param)
- `ShelfDomainRepositoryImpl.java` — all 5 `toDomainFromEntity` callsites updated (+`bookAccessPort` calls)
- `ShelfService.java` — `ShelfMapper` → `ShelfDTOMapper` throughout, constructor parameter reorder
- `ShelfServiceTest.java` — mock swap + 2 new tests

**Key code changes:**

*`Shelf.java` — constructor sealed:*

```java
// Before
public Shelf(String shelfLabel, int shelfPosition, int bookCapacity, ShelfId shelfId) { ...}

// After
public Shelf(String shelfLabel, int shelfPosition,
             int bookCapacity, ShelfId shelfId, List<Long> books) { ...}
```

*`ShelfDomainRepositoryImpl.java` — consistent hydration:*

```java
// findAll() — before: books silently missing
.map(shelfEntity -> shelfMapper.toDomainFromEntity(shelfEntity))

// findAll() — after: books always fetched
.map(shelfEntity -> shelfMapper.toDomainFromEntity(
    shelfEntity, bookAccessPort.getBookIdsByShelfId(shelfEntity.getShelfId())))
```

Same pattern applied to: `findByBookcaseId`, `findShelfSummariesByBookcaseId`, `findById`,
`getShelfShelfOptionResponse`.

*`ShelfService.java` — dependency direction corrected:*

```java
// Before
import com.penrose.bibby.library.stacks.shelf.infrastructure.mapping.ShelfMapper;
ShelfMapper shelfMapper;

// After
import com.penrose.bibby.library.stacks.shelf.core.mappers.ShelfDTOMapper;
ShelfDTOMapper shelfDTOMapper;
```

Constructor parameter order also changed from `(mapper, repo, facade)` to `(repo, facade, mapper)`, making the primary
collaborators appear first.

*`ShelfServiceTest.java` — two new `isFull()` tests:*

```java
@Test
void isFull_shouldReturnTrueWhenShelfIsFull()    // findById → shelf.isFull() → true
@Test
void isFull_shouldReturnFalseWhenShelfHasSpace()  // findById → shelf.isFull() → false
```

**Architecture notes:**

- The dependency arrow `ShelfService → ShelfMapper` (core → infrastructure) is now **gone**. This is the most
  significant architectural fix in the PR.
- `ShelfDomainRepositoryImpl` still calls `bookAccessPort.getBookIdsByShelfId()` per shelf — `bookAccessPort` is a
  port (interface), so this is acceptable within the hexagonal model (outbound port to book module). The repository is
  allowed to call another outbound port.

**Risk / edge cases:**

- **N+1 queries added to 4 methods** (`findAll`, `findByBookcaseId`, `findShelfSummariesByBookcaseId`,
  `getShelfShelfOptionResponse`). Before this commit those methods returned shelves with *no* book IDs. After this
  commit they each emit one `getBookIdsByShelfId` call per shelf. For a user with 10 shelves, `findAll` now makes 11
  database round-trips instead of 1. The commit message acknowledges this as "minimal impact due to existing N+1
  patterns" — accurate for now, but worth watching as the library grows.
- **`findShelfSummariesByBookcaseId`** now fetches book IDs despite being a "summary" method. Summaries are usually
  lightweight projections. Either rename this method or introduce a real summary projection (see follow-ups).
- **`toEntity` still omits `bookcaseId`** — this gap from the previous commit is not addressed here.

**Verification:**

```bash
mvn test -Dtest=ShelfServiceTest
mvn test -Dtest=ShelfDomainRepositoryImplTest  # if it exists
```

---

### `7856ce1` — Add DDD layering principles and mapper guidelines

**Intent:** Document the "why" behind the refactor so future contributors don't revert the pattern.

**Files touched:**

- `docs/engineering/ddd-layers-and-mappers.md` — new file, 84 lines

**Key content:**

- Explains `ShelfEntity.shelfId` (persistence identity, `Long`) vs `Shelf.shelfId` (`ShelfId` value object, domain
  identity) — a question that comes up frequently when onboarding to DDD.
- Explicit mapper placement table: `ShelfMapper` → infrastructure boundary; `ShelfDTOMapper` → application boundary.
- Notes the NPE risk if `Shelf.getId()` is called before persistence assigns an ID.

**Architecture notes:** Pure documentation. No production code changes. The examples in the doc match the actual code
structure.

**Risk:** Docs rot. The table references specific class names and package paths — these will diverge if classes are
renamed or moved without updating the doc.

---

## Critical Flow — End to End

Here is how a `getShelfOptions()` request flows through the system after this PR:

```
HTTP GET /shelves/options
  → ShelfController
  → ShelfFacade.getShelfOptions()                              [inbound port]
    → ShelfService.getShelfOptions()                           [application]
      → ShelfDomainRepository.findAll()                        [outbound port]
        → ShelfDomainRepositoryImpl.findAll()                  [infrastructure adapter]
          → jpaRepository.findAll()                            [JPA → DB: 1 query]
          for each ShelfEntity:
            → bookAccessPort.getBookIdsByShelfId(id)           [outbound port → DB: N queries]
            → shelfMapper.toDomainFromEntity(entity, books)  [infrastructure mapper]
              → new Shelf(label, pos, cap, new ShelfId(id), books)  [domain model]
      → stream of Shelf domain objects (fully hydrated)
      for each Shelf:
        → shelfDTOMapper.toShelfOption(shelf)                  [application mapper]
          → new ShelfOptionResponse(id, label, cap, count, hasSpace)
  → List<ShelfOptionResponse> serialized to JSON
```

**Why this is better than before:** In the old flow, `shelfMapper.toDomainFromEntity(shelfEntity)` (no books) produced a
`Shelf` where `books = null`. When `shelfDTOMapper.toShelfOption` called `shelf.getBookIds().size()`, it would NPE. The
bug was silently waiting for a `findAll` path to be hit in production. The new constructor contract makes this
impossible at compile time.

---

## Dependency Audit

**Dependencies removed:**

- `ShelfService` no longer imports `infrastructure.mapping.ShelfMapper` — the downward dependency is gone.

**Dependencies added:**

- `ShelfService` now imports `core.mappers.ShelfDTOMapper` — same layer, clean.
- `ShelfDTOMapper` imports from `api.dtos.ShelfDTO` and `api.dtos.ShelfOptionResponse`.

**Potential concern — `ShelfDTOMapper` → `api.dtos`:**
Check whether `api.dtos` is in the `core` module or in an adapter/controller module. If it's a sibling package in the
same Maven module (or in a shared contract module), the dependency is fine. If `api` is truly an outbound adapter
package, the application core depending on it is a mild inversion. Worth verifying via package structure once — it
doesn't need fixing now but should be tracked.

**No infrastructure imports remain in `ShelfService` or `ShelfDTOMapper`** — the primary goal is clean.

---

## Test Coverage

**What changed:**

- `ShelfServiceTest` — all `shelfMapper` mocks replaced with `shelfDTOMapper`; `isFull_shouldReturnTrueWhenShelfIsFull`
  and `isFull_shouldReturnFalseWhenShelfHasSpace` added.

**What's missing:**

1. **`ShelfDTOMapperTest`** — no unit tests for the new class.
   ```java
   // Suggested cases:
   void toDTO_shouldMapAllFieldsCorrectly()
   void toShelfOption_shouldSetHasSpaceTrue_whenBookCountLessThanCapacity()
   void toShelfOption_shouldSetHasSpaceFalse_whenBookCountEqualsCapacity()
   void toDomainFromDTO_shouldPreserveBookIds()
   ```

2. **`ShelfMapperTest` (updated)** — the `toDomainFromEntity` signature changed; any existing tests will fail unless
   updated to pass `books`.

3. **`ShelfDomainRepositoryImplTest`** — the hydration change across 5 methods needs integration or slice tests to
   verify that `bookAccessPort.getBookIdsByShelfId` is actually called and that shelves returned have non-null `books`.

4. **`Shelf` constructor test** — the new required `books` param has no test asserting constructor behavior when `books`
   is `null` or empty.

**Run commands:**

```bash
# Fast unit test run
mvn test -Dtest="ShelfServiceTest,ShelfMapperTest,ShelfDTOMapperTest"

# Full shelf module
mvn test -Dtest="com.penrose.bibby.library.stacks.shelf.**"

# Confirm nothing else in the codebase still calls old ShelfMapper methods
grep -r "shelfMapper\.toDTO\|shelfMapper\.toShelfOption\|toDomainFromDTO\|toDomain(" src/
```

---

## Lessons From This Diff

1. **Stubs are time bombs.** `toEntity(Shelf shelf) { return null; }` was sitting in production code for at least
   several commits. A stub that compiles but does nothing is indistinguishable from a correct implementation until the
   path is exercised. Implement or throw `UnsupportedOperationException`.

2. **Constructor arity is the cheapest consistency enforcement you have.** Changing `new Shelf(label, pos, cap, id)` to
   require `books` cost one line and eliminated an entire class of partial-hydration bugs that setter-based construction
   never could.

3. **When a mapper has two concerns, neither gets tested well.** `ShelfMapper` had 7+ methods across two boundaries.
   Moving `toDTO`/`toShelfOption` to `ShelfDTOMapper` makes the test surface smaller and the intent clearer for each
   class.

4. **Naming "From" methods requires discipline.** `toDomainFromDTO(Shelf, List<Long>)` doesn't take a DTO — it takes a
   `Shelf`. The "From" in the name is misleading. At minimum this should be `hydrateWithBookIds` or `withBookIds`.

5. **"Summary" methods shouldn't silently become full loads.** `findShelfSummariesByBookcaseId` now fetches book IDs for
   each shelf, making it functionally identical to `findByBookcaseId`. Either it should be a real lightweight
   projection (no `books`) or the distinction should be removed.

6. **N+1 patterns accepted as baseline have long-term cost.** The PR correctly notes that N+1 already existed — but
   normalizing it by spreading the pattern to 4 more methods pushes the performance cliff closer. Document the N+1
   explicitly now so it's easy to batch later.

7. **Architecture decisions documented same-day are the ones that stick.** The `ddd-layers-and-mappers.md` was added in
   the same commit batch. Future-you will thank you when the question "why do we have two mappers?" comes up in code
   review.

---

## Follow-ups

### Immediate (today)

- **Rename `ShelfDTOMapper.toDomainFromDTO`** → `toDomainWithBookIds` or `hydrateBooks`. The current name implies a DTO
  input that doesn't exist.
- **Write `ShelfDTOMapperTest`** — the new class has zero test coverage and three distinct methods to verify.
- **Fix `toEntity` in `ShelfMapper`** — `bookcaseId` is not being set on the returned `ShelfEntity`. If `save` ever
  routes through this method the bookcase relationship will be null.

### Short-term hardening (this week)

- **Audit `findShelfSummariesByBookcaseId`** — either return a true lightweight projection (no `bookAccessPort` call)
  with a `ShelfSummary` return type, or merge it with `findByBookcaseId` and remove the duplication.
- **Add `ShelfDomainRepositoryImpl` tests** confirming all 5 repository methods return fully-hydrated shelves with
  non-null `books`.
- **Verify `api.dtos` package placement** relative to `core.mappers` — confirm the dependency direction from
  `ShelfDTOMapper` into `api.dtos` is intentional and document it if so.
- **Update `ShelfMapperTest`** if it exists — the `toDomainFromEntity` signature change is a breaking test change.

### Strategic refactors (later)

- **Batch `bookAccessPort` calls in repository list methods** (`findAll`, `findByBookcaseId`). Currently each shelf
  triggers its own `getBookIdsByShelfId` query. A single `getBookIdsByShelfIds(List<Long> ids)` port method would
  collapse N queries to 1. This is the natural follow-up once the N+1 baseline is acknowledged and becomes a pain point.
- **Evaluate `Shelf.books` as a lazy association** vs. eager hydration. Right now every load of a shelf fetches its book
  IDs. For read-heavy summary views (shelf list screens) this is wasteful. A `ShelfSummary` projection (no `books`)
  routed through a separate repository method would let the UI choose what it needs.
- **Consider a `ShelfMapper` interface** so that infrastructure-layer tests can mock the mapping boundary cleanly,
  consistent with how port interfaces work elsewhere in the hexagonal structure.
