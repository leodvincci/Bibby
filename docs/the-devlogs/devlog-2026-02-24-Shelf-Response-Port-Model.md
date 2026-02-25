# Devlog: Introduce ShelfResponse Port Model for Inbound Boundary

**Date:** 2026-02-24 (America/Chicago)
**Branch:** `refactor/shelf-port-response-model`
**Range:** `origin/main..HEAD`
**Commits:**

| SHA | Subject |
|-----|---------|
| `aa691bb` | Refactor DeleteShelvesUseCase and ShelfService for improved logging and documentation |
| `4cf22b7` | Merge branch 'main' of https://github.com/leodvincci/Bibby |
| `a974cee` | Introduce ShelfResponse port model for inbound port boundary |
| `94daea4` | Update consumers and tests to use ShelfResponse port model |

---

## TL;DR

- **What problem was I solving?** `ShelfFacade.findShelvesByBookcaseId()` returned raw `Shelf` domain objects across the inbound port boundary. Consumers (CLI prompt logic, web mappers) were coupled directly to domain internals—getters, value objects (`ShelfId`), and mutable state.
- **What was the "before" state?** `ShelfFacade` exposed `List<Shelf>` from `findShelvesByBookcaseId()`. Every consumer had to know about `Shelf.getShelfId().shelfId()`, `Shelf.getBookCapacity()`, etc. Any domain model change rippled outward through the CLI and web layers.
- **Most important outcomes:**
  - New immutable `ShelfResponse` record sits at the port boundary (`ports/inbound/inboundPortModels/`)
  - `ShelfFacade.findShelvesByBookcaseId()` now returns `List<ShelfResponse>` instead of `List<Shelf>`
  - `ShelfPortModelMapper` handles the `Shelf -> ShelfResponse` translation inside the application layer
  - All consumers (CLI `PromptOptions`, web `ShelfResponseMapper`) and all tests updated to use the new contract
  - Domain `Shelf` is no longer visible outside the core for this query path

---

## Commit-by-Commit Analysis

### Commit 1: `aa691bb` — Refactor DeleteShelvesUseCase and ShelfService for improved logging and documentation

**Intent:** Add interview-prep documentation files for previous refactoring work (Bookcase domain model, UserId association).

**Files touched:**
| File | Reason |
|------|--------|
| `docs/interview-prep/bookcase-domain-model-and-use-case-extraction.md` | New — 589 lines of interview prep documenting the Bookcase refactor |
| `docs/interview-prep/userid-association-and-repository-hardening.md` | New — 471 lines documenting UserId association changes |

**Key code changes:** None — documentation only. No production or test code modified.

**Architecture notes:** No impact. These are reference documents living outside the build.

**Risk / edge cases:** None. Commit message is slightly misleading (mentions "DeleteShelvesUseCase and ShelfService" but the diff is only documentation). Likely a stale commit message from a prior amend.

**Verification:** N/A — docs only.

---

### Commit 2: `4cf22b7` — Merge branch 'main' of Bibby

**Intent:** Sync local `main` with `origin/main` before branching.

**Files touched:** Merge commit bringing in upstream changes from PRs #319–#323 (Bookcase domain model refactors, shelf hex boundary fixes, method renames, unused dependency cleanup, QueryShelfUseCase Javadoc).

**Architecture notes:** This merge brings the branch up to date. All prior refactors (use-case extraction, port renames, BookAccessAdapter relocation) are now in the baseline.

**Risk / edge cases:** Clean merge, no conflicts.

---

### Commit 3: `a974cee` — Introduce ShelfResponse port model for inbound port boundary

**Intent:** Create the port-model type and rewire the internal plumbing so domain objects stop leaking through `ShelfFacade`.

**Files touched:**
| File | Reason |
|------|--------|
| `ShelfResponse.java` (new) | Immutable record at `ports/inbound/inboundPortModels/` — the new contract type |
| `ShelfPortModelMapper.java` (new) | Static mapper: `Shelf` -> `ShelfResponse`, lives at `ports/inbound/mapper/` |
| `ShelfFacade.java` | Interface signature change: `List<Shelf>` -> `List<ShelfResponse>` |
| `ShelfService.java` | Delegation now returns `List<ShelfResponse>` to match facade |
| `QueryShelfUseCase.java` | Core change: maps repository results through `ShelfPortModelMapper::toShelfResponse` |
| `ShelfResponseMapper.java` (web layer) | New overload `toShelfOption(ShelfResponse)` for the web adapter |

**Key code changes:**

`ShelfResponse` record — flat, immutable, no domain behavior:

```java
public record ShelfResponse(
    Long id, int shelfPosition, String shelfLabel,
    int bookCapacity, List<Long> bookIds) {}
```

`ShelfPortModelMapper.toShelfResponse()` — extracts primitives from domain `Shelf`:

```java
public static ShelfResponse toShelfResponse(Shelf shelf) {
    return new ShelfResponse(
        shelf.getId(), shelf.getShelfPosition(),
        shelf.getShelfLabel(), shelf.getBookCapacity(),
        shelf.getBookIds());
}
```

`QueryShelfUseCase.findShelvesByBookcaseId()` — mapping happens at the use-case level, before returning through the port:

```java
public List<ShelfResponse> findShelvesByBookcaseId(Long bookcaseId) {
    return shelfDomainRepositoryPort.findByBookcaseId(bookcaseId).stream()
        .map(ShelfPortModelMapper::toShelfResponse)
        .toList();
}
```

**Architecture notes:**
- **Correct boundary placement.** `ShelfResponse` lives in `ports/inbound/inboundPortModels/` — it's part of the port contract, not the domain, not the adapter. This is textbook hexagonal: the port defines its own types.
- **Mapper lives at `ports/inbound/mapper/`** — close to the port definition, used by the application layer. The mapper depends inward (on `Shelf`), which is legal. Outer layers depend on the port model, not the domain.
- **Only `findShelvesByBookcaseId` is migrated.** Other `ShelfFacade` methods (`findShelfById`, `addShelf`, etc.) still return raw `Shelf` or `ShelfSummary`. This is an incremental, safe migration.
- **Web layer gets a parallel overload**, not a replacement. `ShelfResponseMapper` now has both `toShelfOption(Shelf)` and `toShelfOption(ShelfResponse)`. The old overload can be removed once all facade methods are migrated.

**Risk / edge cases:**
- `ShelfResponse.bookIds()` returns the same `List<Long>` reference from the domain. If the domain list is mutable, downstream consumers could theoretically mutate it. **Inference:** Records don't defensively copy. A `List.copyOf()` in the mapper would harden this.
- The `id` field in `ShelfResponse` is the raw `Long`, not `ShelfId`. This is intentional — port models should use primitives — but it means consumers lose type-safety on shelf vs. bookcase IDs.

**Verification:**
```bash
mvn test -pl . -Dtest="QueryShelfUseCaseTest" -DfailIfNoTests=false
mvn test -pl . -Dtest="ShelfServiceTest" -DfailIfNoTests=false
```

---

### Commit 4: `94daea4` — Update consumers and tests to use ShelfResponse port model

**Intent:** Align every consumer and test with the new `ShelfResponse` contract.

**Files touched:**
| File | Reason |
|------|--------|
| `PromptOptions.java` | Switched from `Shelf` getters to `ShelfResponse` record accessors |
| `PromptOptionsTest.java` | Constructs `ShelfResponse` instead of `Shelf`; removes `ShelfId` import |
| `ShelfServiceTest.java` | Delegation test uses `ShelfResponse` type and concrete instances |
| `QueryShelfUseCaseTest.java` | Mocks `Shelf` getter returns, asserts on `ShelfResponse` fields |

**Key code changes:**

`PromptOptions.java` — before vs. after:

```java
// BEFORE: coupled to domain Shelf
shelf.getShelfId().shelfId()   // nested value object
shelf.getShelfLabel()          // domain getter
shelf.getBookCapacity()        // domain getter

// AFTER: flat record accessors
shelf.id()                     // primitive Long
shelf.shelfLabel()             // record accessor
shelf.bookCapacity()           // record accessor
```

`QueryShelfUseCaseTest` — tests now verify the mapping behavior, not just delegation:

```java
Shelf shelf1 = mock(Shelf.class);
when(shelf1.getId()).thenReturn(1L);
when(shelf1.getShelfLabel()).thenReturn("Shelf A");
// ...
List<ShelfResponse> result = queryShelfUseCase.findShelvesByBookcaseId(bookcaseId);
assertThat(result.get(0).id()).isEqualTo(1L);
```

This is a meaningful test upgrade: previously the test just checked that the same `Shelf` objects came back. Now it verifies that the mapping actually produces correct `ShelfResponse` values.

`PromptOptionsTest` — simplified construction:

```java
// BEFORE: needed ShelfId value object, bookcaseId coupling
Shelf shelf1 = new Shelf("Top Shelf", 1, 10, new ShelfId(10L), List.of(100L, 101L), 1L);

// AFTER: flat record, no value objects, no extraneous fields
ShelfResponse shelf1 = new ShelfResponse(10L, 1, "Top Shelf", 10, List.of(100L, 101L));
```

**Architecture notes:**
- `PromptOptions` (CLI layer) no longer imports anything from `shelf.core.domain`. It only depends on `ports.inbound.inboundPortModels.ShelfResponse`. This is a clean layering win.
- `PromptOptionsTest` drops `Shelf` and `ShelfId` imports entirely — the CLI test has zero knowledge of domain internals.
- `ShelfServiceTest` now uses concrete `ShelfResponse` instead of `mock(Shelf.class)`, which is better: mocking a record/DTO is an anti-pattern.

**Risk / edge cases:**
- None introduced. This commit is purely mechanical alignment.

**Verification:**
```bash
mvn test -pl . -Dtest="PromptOptionsTest,ShelfServiceTest,QueryShelfUseCaseTest" -DfailIfNoTests=false
```

---

## End-to-End Flow: Query Shelves by Bookcase ID

### Before (pre-branch)

```
CLI PromptOptions
  -> ShelfFacade.findShelvesByBookcaseId(bookcaseId)  // returns List<Shelf>
    -> ShelfService delegates to QueryShelfUseCase
      -> QueryShelfUseCase calls shelfDomainRepositoryPort.findByBookcaseId()
        -> returns List<Shelf>  // domain objects flow ALL the way out
  <- PromptOptions accesses shelf.getShelfId().shelfId(), shelf.getBookCapacity(), etc.
```

**Problem:** Domain `Shelf` objects leak through the port boundary. Every consumer is coupled to the domain model's internal structure, including nested value objects like `ShelfId`.

### After (this branch)

```
CLI PromptOptions
  -> ShelfFacade.findShelvesByBookcaseId(bookcaseId)  // returns List<ShelfResponse>
    -> ShelfService delegates to QueryShelfUseCase
      -> QueryShelfUseCase calls shelfDomainRepositoryPort.findByBookcaseId()
        -> returns List<Shelf>  // domain stays inside core
      -> ShelfPortModelMapper::toShelfResponse  // mapping happens HERE
        -> returns List<ShelfResponse>  // flat, immutable record
  <- PromptOptions accesses shelf.id(), shelf.shelfLabel(), etc.  // record accessors
```

**Key insight:** The mapping boundary is at the use-case level, not the adapter level. `QueryShelfUseCase` is the last place that touches domain `Shelf` objects. Everything outward sees only `ShelfResponse`.

---

## Why This Design Is Better

1. **Domain isolation.** `Shelf` can evolve (add validation, change internal structure, rename fields) without touching any consumer. Only `ShelfPortModelMapper` needs updating.
2. **Simpler consumer code.** Record accessors (`shelf.id()`) are cleaner than navigating nested value objects (`shelf.getShelfId().shelfId()`).
3. **Testability.** Tests construct `ShelfResponse` with a one-liner constructor. No mocking domain objects, no setting up value objects.
4. **Explicit contract.** `ShelfResponse` documents exactly what data crosses the port boundary — no more, no less. Consumers can't accidentally call domain behavior.
5. **Incremental migration.** Only `findShelvesByBookcaseId` is converted. Other methods can follow the same pattern without a big-bang refactor.

---

## Dependency Direction Analysis

| From | To | Direction | Status |
|------|----|-----------|--------|
| `PromptOptions` (CLI) | `ShelfResponse` (port model) | Outer -> Port | Correct |
| `PromptOptions` (CLI) | `Shelf` (domain) | Outer -> Domain | **Removed** |
| `QueryShelfUseCase` (application) | `ShelfPortModelMapper` (port mapper) | App -> Port | Correct |
| `QueryShelfUseCase` (application) | `ShelfResponse` (port model) | App -> Port | Correct |
| `ShelfPortModelMapper` (port mapper) | `Shelf` (domain) | Port -> Domain | Correct (inward) |
| `ShelfResponseMapper` (web infra) | `ShelfResponse` (port model) | Infra -> Port | Correct |

No layering violations detected. All dependencies point inward or toward port contracts.

---

## What Dependencies Changed

**Removed:**
- `PromptOptions` no longer imports `Shelf` or `ShelfId` from `shelf.core.domain`
- `PromptOptionsTest` no longer imports `Shelf` or `ShelfId`

**Added:**
- `ShelfResponse` (new type, `ports.inbound.inboundPortModels`)
- `ShelfPortModelMapper` (new class, `ports.inbound.mapper`)
- `ShelfResponseMapper` gains a new overload (accepts `ShelfResponse`)

---

## Tests: What Exists and What Should Be Added

### Existing tests updated:
- `QueryShelfUseCaseTest` — now verifies mapping from `Shelf` to `ShelfResponse`
- `ShelfServiceTest` — confirms delegation returns `ShelfResponse`
- `PromptOptionsTest` — exercises the full CLI prompt flow with `ShelfResponse`

### Suggested additional tests:

1. **`ShelfPortModelMapperTest`** — Unit test the mapper in isolation. Verify all fields map correctly, especially edge cases:
   - `null` bookIds list
   - Empty bookIds list
   - `null` shelfLabel

2. **`ShelfResponseMapper.toShelfOption(ShelfResponse)` test** — Verify the web-layer overload produces correct `ShelfOptionResponse`, especially the `hasSpace` calculation when `bookIds.size() == bookCapacity`.

3. **`QueryShelfUseCaseTest` edge case: single shelf with max books** — Verify mapping works when `bookIds` list is at capacity.

4. **Integration test: full flow through `ShelfFacade` to verify `ShelfResponse` is returned** — Particularly if there's an existing integration test suite.

### Run all related tests:
```bash
mvn test -pl . -Dtest="QueryShelfUseCaseTest,ShelfServiceTest,PromptOptionsTest" -DfailIfNoTests=false
```

---

## Lessons Learned (from this diff)

1. **Port models should be records.** `ShelfResponse` as a Java record gives you immutability, auto-generated accessors, `equals`/`hashCode`, and concise construction — all for free. This made test construction dramatically simpler.

2. **Mapping at the use-case level is the right call.** Putting `ShelfPortModelMapper::toShelfResponse` inside `QueryShelfUseCase` (not in `ShelfService` or an adapter) keeps the transformation close to the business logic that produces the data.

3. **Incremental port-model migration works.** Converting one method at a time (`findShelvesByBookcaseId` first) keeps PRs small and reviewable. The old `Shelf`-returning methods still work — no big-bang needed.

4. **Nested value objects in domain models are fine internally but toxic at boundaries.** `ShelfId` wrapping a `Long` is good DDD inside the core. But forcing consumers to call `shelf.getShelfId().shelfId()` is unnecessarily complex. The port model flattens this correctly.

5. **Test quality improves automatically with port models.** The old tests used `mock(Shelf.class)` and only checked that the same mock came back. The new tests construct real `ShelfResponse` instances and verify actual field values. More meaningful coverage for free.

6. **Parallel overloads during migration are acceptable technical debt.** `ShelfResponseMapper` now has both `toShelfOption(Shelf)` and `toShelfOption(ShelfResponse)`. This is fine temporarily but should be cleaned up once all facade methods return port models.

---

## Roadmap

### Immediate follow-ups (today)
- [ ] Add `ShelfPortModelMapperTest` — unit test the mapper in isolation
- [ ] Add `ShelfResponseMapper` test for the new `toShelfOption(ShelfResponse)` overload
- [ ] Consider adding `List.copyOf()` in `ShelfPortModelMapper` for defensive copying of `bookIds`

### Short-term hardening (this week)
- [ ] Migrate `findShelfById()` in `ShelfFacade` to return `Optional<ShelfResponse>` instead of `Optional<Shelf>`
- [ ] Migrate remaining `ShelfFacade` methods (`addShelf`, `deleteShelf`, etc.) to port models
- [ ] Remove the old `toShelfOption(Shelf)` overload from `ShelfResponseMapper` once all callers are migrated
- [ ] Evaluate whether `ShelfSummary` should also become a port model or remain as a domain type

### Strategic refactors (later)
- [ ] Apply the same port-model pattern to `BookcaseFacade` — return `BookcaseResponse` instead of raw `Bookcase`
- [ ] Apply to `BookFacade` — return `BookResponse` instead of raw domain `Book`
- [ ] Create a shared convention/base package for all inbound port models across modules (e.g., standardize the `inboundPortModels/` package name — consider `response/` or `dto/` for consistency)
- [ ] Consider whether `ShelfPortModelMapper` should be a Spring `@Component` with injectable dependencies for more complex future mappings, or if the current static approach is sufficient
