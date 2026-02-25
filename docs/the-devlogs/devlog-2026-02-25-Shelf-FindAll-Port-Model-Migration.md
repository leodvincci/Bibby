# Devlog: Migrate Shelf findAll to Return ShelfResponse Port Model

**Date:** 2026-02-25
**Time:** ~09:11 CST (America/Chicago)
**Branch:** `refactor/shelf-findall-response-and-docs`
**Range:** `origin/main..HEAD`
**Commits:**

| SHA | Subject |
|-----|---------|
| `5c6de4c` | Return ShelfResponse from shelf findAll boundary |
| `54a86ac` | Strengthen shelf findAll tests for response mapping |
| `49e81d5` | Document shelf inbound port model decision |

---

## TL;DR

- **What problem was I solving?** `ShelfFacade.findAll()` still returned raw `List<Shelf>` domain objects across the inbound port boundary — the last remaining list-query method that hadn't been migrated to the `ShelfResponse` port model introduced in PR #324.
- **What was the "before" state?** After PR #324 merged, `findShelvesByBookcaseId()` returned `List<ShelfResponse>`, but `findAll()` still leaked `List<Shelf>`. The port interface was inconsistent: two list-query methods, two different return types.
- **Most important outcomes:**
  - `ShelfFacade.findAll()` now returns `List<ShelfResponse>` — matching `findShelvesByBookcaseId()`
  - `QueryShelfUseCase.findAll()` maps through `ShelfPortModelMapper::toShelfResponse` before returning
  - Tests upgraded from passthrough-mock assertions to field-level mapping verification
  - New `ShelfServiceTest.findAll_shouldDelegateToQueryUseCase()` test added
  - ADR 0004 and a companion devlog document the full port-model decision, alternatives considered, and follow-up work

---

## Commit-by-Commit Analysis

### Commit 1: `5c6de4c` — Return ShelfResponse from shelf findAll boundary

**Intent:** Extend the inbound port-model migration to `findAll()`. After PR #324 migrated `findShelvesByBookcaseId()`, this commit applies the identical pattern to the remaining list-query method.

**Files touched:**

| File | Reason |
|------|--------|
| `ShelfFacade.java` | Interface signature: `List<Shelf>` → `List<ShelfResponse>` on `findAll()` |
| `QueryShelfUseCase.java` | Adds `.stream().map(ShelfPortModelMapper::toShelfResponse).toList()` to `findAll()` |
| `ShelfService.java` | Return type alignment: delegation layer now returns `List<ShelfResponse>` |

**Key code changes:**

`ShelfFacade.java:22` — the contract change that drives everything:

```java
// BEFORE
List<Shelf> findAll();

// AFTER
List<ShelfResponse> findAll();
```

`QueryShelfUseCase.java:72-76` — mapping added at the use-case level, same pattern as `findShelvesByBookcaseId`:

```java
public List<ShelfResponse> findAll() {
    return shelfDomainRepositoryPort.findAll().stream()
        .map(ShelfPortModelMapper::toShelfResponse)
        .toList();
}
```

`ShelfService.java:80` — pure delegation passthrough, return type updated to match facade:

```java
@Override
public List<ShelfResponse> findAll() {
    return queryShelfUseCase.findAll();
}
```

**Architecture notes:**
- This is the **second method** in `ShelfFacade` to return `ShelfResponse`. The pattern established in PR #324 is now repeated — `findAll()` follows the exact same mapping path: repository → domain `Shelf` → `ShelfPortModelMapper::toShelfResponse` → `ShelfResponse` out through the port.
- `ShelfFacade` now has two query methods returning `ShelfResponse` (`findShelvesByBookcaseId`, `findAll`) and one returning raw `Shelf` (`findShelfById`). The inconsistency is shrinking.
- No new types or mappers needed — reuses the existing `ShelfPortModelMapper` and `ShelfResponse` from PR #324.

**Risk / edge cases:**
- Any downstream caller of `ShelfFacade.findAll()` that expected `List<Shelf>` will break at compile time. This is a safe, loud failure — not a silent behavior change.
- The mapping overhead (`.stream().map().toList()`) on every `findAll()` call is negligible at current scale but establishes a pattern that should be monitored if shelf counts grow large.

**Verification:**
```bash
mvn test -Dtest="ShelfServiceTest,QueryShelfUseCaseTest" -DfailIfNoTests=false
```

---

### Commit 2: `54a86ac` — Strengthen shelf findAll tests for response mapping

**Intent:** Upgrade tests from weak passthrough assertions to field-level mapping verification. Add missing `ShelfServiceTest` coverage for the `findAll` delegation path.

**Files touched:**

| File | Reason |
|------|--------|
| `ShelfServiceTest.java` | New test: `findAll_shouldDelegateToQueryUseCase()` |
| `QueryShelfUseCaseTest.java` | `findAll_shouldReturnAllShelves()` now mocks domain getters and asserts on `ShelfResponse` fields |

**Key code changes:**

`ShelfServiceTest.java` — new test verifying `findAll` delegation:

```java
@Test
void findAll_shouldDelegateToQueryUseCase() {
    List<ShelfResponse> expected =
        List.of(new ShelfResponse(1L, 1, "Shelf A", 10, List.of()));
    when(queryShelfUseCase.findAll()).thenReturn(expected);

    List<ShelfResponse> result = shelfService.findAll();

    assertThat(result).isEqualTo(expected);
    verify(queryShelfUseCase).findAll();
}
```

`QueryShelfUseCaseTest.java:137-177` — upgraded from mock-passthrough to mapping verification:

```java
// BEFORE: just checked same mocks came back
List<Shelf> result = queryShelfUseCase.findAll();
assertThat(result).hasSize(2).containsExactly(shelf1, shelf2);

// AFTER: mocks domain getters, asserts mapped ShelfResponse fields
when(shelf1.getId()).thenReturn(1L);
when(shelf1.getShelfPosition()).thenReturn(1);
when(shelf1.getShelfLabel()).thenReturn("Shelf A");
when(shelf1.getBookCapacity()).thenReturn(10);
when(shelf1.getBookIds()).thenReturn(List.of());
// ...
List<ShelfResponse> result = queryShelfUseCase.findAll();
assertThat(result).hasSize(2);
assertThat(result.get(0).id()).isEqualTo(1L);
assertThat(result.get(1).id()).isEqualTo(2L);
```

**Architecture notes:**
- This is a meaningful test quality improvement. The old test verified delegation (same objects in, same objects out). The new test verifies **mapping correctness** — that domain getter values end up in the right `ShelfResponse` fields.
- `ShelfServiceTest` now covers the `findAll` path, which was previously untested. The delegation layer is thin, but having the test catches accidental regressions if `ShelfService` is modified.

**Risk / edge cases:**
- The `QueryShelfUseCaseTest` asserts on `id()` only, not all fields. This is sufficient for proving the mapping runs but won't catch a bug where, say, `shelfLabel` and `shelfPosition` are swapped. **Suggested improvement:** assert on all fields for at least one shelf.
- The empty-list test (`findAll_shouldReturnEmptyListWhenNoShelvesExist`) was updated to `List<ShelfResponse>` return type but the assertion (`assertThat(result).isEmpty()`) is type-agnostic — still valid.

**Verification:**
```bash
mvn test -Dtest="ShelfServiceTest,QueryShelfUseCaseTest" -DfailIfNoTests=false
```

---

### Commit 3: `49e81d5` — Document shelf inbound port model decision

**Intent:** Record the architectural decision and implementation journal for the full `ShelfResponse` port-model migration (PR #324 + this branch).

**Files touched:**

| File | Reason |
|------|--------|
| `docs/ADR-0004-inbound-port-model-for-shelf-query-boundary.md` (new, 240 lines) | Formal ADR: context, decision, drivers, alternatives, consequences, follow-up |
| `docs/the-devlogs/devlog-2026-02-24-Shelf-Response-Port-Model.md` (new, 323 lines) | Devlog covering PR #324 commits (`a974cee`, `94daea4`) |

**Key content in ADR 0004:**
- Documents four considered options: (A) inbound port model record (chosen), (B) return domain directly (status quo), (C) map at adapter boundary, (D) sealed interface / shared module
- Six decision drivers grounded in specific code evidence (commit SHAs, file paths, line numbers)
- Nine follow-up items in a prioritized table
- Compliance guardrails with `rg` commands for detecting layering violations

**Key content in the devlog:**
- Commit-by-commit analysis of PR #324's four commits
- Before/after end-to-end flow diagrams (request → adapter → port → core → persistence → response)
- Dependency direction table proving no layering violations
- Lessons learned tied to specific code changes

**Architecture notes:**
- ADR 0004 extends the lineage from ADR 0003 (which removed `BookcaseEntity`/`BookcaseDTO` from the Bookcase inbound port). ADR 0004 goes further: instead of returning raw domain objects, it introduces a **dedicated port model** — a read-only projection owned by the port contract.
- The ADR explicitly calls out `inboundPortModels` as a non-standard package name and flags it as a candidate ADR.

**Risk / edge cases:**
- Commit message says "Document shelf inbound port model decision" but the devlog inside covers PR #324 (the previous branch), not this branch. Slightly confusing provenance but harmless.
- Documentation-only commit — no runtime risk.

**Verification:** N/A — docs only.

---

## End-to-End Flow: `findAll` Query Path

### Before (pre-branch)

```
Any consumer
  → ShelfFacade.findAll()                    // returns List<Shelf> ← DOMAIN LEAK
    → ShelfService.findAll()                 // passthrough
      → QueryShelfUseCase.findAll()          // passthrough
        → shelfDomainRepositoryPort.findAll() // returns List<Shelf>
      ← returns List<Shelf> raw
    ← returns List<Shelf> raw
  ← Consumer accesses shelf.getShelfId().shelfId(), shelf.getBookCapacity(), etc.
```

**Problem:** Domain `Shelf` objects — with mutable state, nested value objects (`ShelfId`), and domain behavior (`isFull()`, `getBookCount()`) — flow through the port to outer layers.

### After (this branch)

```
Any consumer
  → ShelfFacade.findAll()                    // returns List<ShelfResponse> ← PORT MODEL
    → ShelfService.findAll()                 // passthrough
      → QueryShelfUseCase.findAll()
        → shelfDomainRepositoryPort.findAll() // returns List<Shelf>
        → .stream().map(ShelfPortModelMapper::toShelfResponse).toList()  // MAPPING HERE
      ← returns List<ShelfResponse>
    ← returns List<ShelfResponse>
  ← Consumer accesses shelf.id(), shelf.shelfLabel(), shelf.bookCapacity()
```

**Key change:** The mapping boundary is at `QueryShelfUseCase` — the last component that touches domain `Shelf` objects. Everything outward sees only `ShelfResponse`.

---

## Why This Design Is Better Than the Old One

1. **Consistent port contract.** Both list-query methods (`findShelvesByBookcaseId`, `findAll`) now return the same type. Consumers don't need to reason about which methods leak domain types.
2. **Reuses existing infrastructure.** No new types or mappers — `ShelfResponse` and `ShelfPortModelMapper` from PR #324 handle both methods.
3. **Domain isolation.** `Shelf` can add constructors, rename getters, or restructure `ShelfId` without affecting any consumer of `findAll()`.
4. **Test quality.** Tests now verify mapping correctness instead of just delegation. A broken mapper will be caught.

---

## Dependency Direction Analysis

| From | To | Direction | Status |
|------|----|-----------|--------|
| `ShelfService` (application) | `QueryShelfUseCase` (application) | Same layer | Correct — delegation |
| `QueryShelfUseCase` (application) | `ShelfPortModelMapper` (port mapper) | App → Port | Correct |
| `QueryShelfUseCase` (application) | `ShelfResponse` (port model) | App → Port | Correct |
| `ShelfPortModelMapper` (port mapper) | `Shelf` (domain) | Port → Domain | Correct (inward) |
| `ShelfFacade` (port interface) | `ShelfResponse` (port model) | Port → Port | Correct (same package) |

No layering violations. All dependencies point inward or stay within the port layer.

---

## What Dependencies Changed

**Removed:**
- Any consumer of `ShelfFacade.findAll()` that imported `Shelf` for the return type no longer needs that import.

**Added:**
- No new types. Reuses `ShelfResponse` and `ShelfPortModelMapper` from PR #324.

**Import audit for `ShelfFacade.java`:**
- Still imports `Shelf` for `findShelfById(Long)` return type (`Optional<Shelf>`) — expected, that method hasn't been migrated yet.
- Still imports `ShelfSummary` for `getShelfSummariesForBookcaseByBookcaseId()` — separate concern.
- Imports `ShelfResponse` — correct, owns this type.

---

## Current `ShelfFacade` Migration Status

| Method | Return Type | Migrated? |
|--------|-------------|-----------|
| `findShelvesByBookcaseId(Long)` | `List<ShelfResponse>` | Yes (PR #324) |
| `findAll()` | `List<ShelfResponse>` | **Yes (this branch)** |
| `findShelfById(Long)` | `Optional<Shelf>` | No — still leaks domain |
| `getShelfSummariesForBookcaseByBookcaseId(Long)` | `List<ShelfSummary>` | No — uses separate summary type |
| `isFull(Long)` / `isEmpty(Long)` | `boolean` | N/A — primitives don't leak |
| `createShelfInBookcaseByBookcaseId(...)` | `void` | N/A — command, not query |
| `deleteAllShelvesInBookcaseByBookcaseId(Long)` | `void` | N/A — command, not query |

---

## Tests: What Exists and What Should Be Added

### Tests updated in this branch:
- `QueryShelfUseCaseTest.findAll_shouldReturnAllShelves()` — mocks domain getters, asserts `ShelfResponse` field values
- `QueryShelfUseCaseTest.findAll_shouldReturnEmptyListWhenNoShelvesExist()` — return type updated
- `ShelfServiceTest.findAll_shouldDelegateToQueryUseCase()` — **new**, verifies delegation

### Suggested additional tests:

1. **`QueryShelfUseCaseTest`: assert ALL fields, not just `id()`** — Current assertion only checks `result.get(0).id()`. Add assertions for `shelfPosition`, `shelfLabel`, `bookCapacity`, and `bookIds` to catch field-mapping bugs (e.g., swapped parameters in the `ShelfResponse` constructor).

2. **`ShelfPortModelMapperTest`** — The mapper is tested indirectly through `QueryShelfUseCaseTest`, but a dedicated unit test would cover edge cases:
   - `null` bookIds list
   - Empty bookIds list
   - Very long shelfLabel
   - `null` shelfLabel

3. **`QueryShelfUseCaseTest`: findAll with single shelf at max capacity** — Verify mapping when `bookIds.size() == bookCapacity` (edge case for downstream `hasSpace` logic).

4. **`QueryShelfUseCaseTest`: findAll with shelf containing many books** — Verify `bookIds` list integrity through the mapping.

### Run all related tests:
```bash
mvn test -Dtest="ShelfServiceTest,QueryShelfUseCaseTest" -DfailIfNoTests=false
```

---

## Suggested Micro-Fixes

1. **Assert all `ShelfResponse` fields in `QueryShelfUseCaseTest`** — The test mocks five getters on each `Shelf` but only asserts `id()`. One extra line per field would catch constructor-ordering bugs.

2. **Consider `List.copyOf()` in `ShelfPortModelMapper`** — `shelf.getBookIds()` is passed directly into the `ShelfResponse` record. If the domain list is mutable, downstream consumers could theoretically mutate it. This was flagged in ADR 0004 as follow-up item #2.

3. **`ShelfFacade` still imports `Shelf`** — Needed for `findShelfById()`. Once that method is migrated, the import can be removed entirely, completing the domain isolation for this port.

---

## Lessons Learned (from this diff)

1. **Incremental port-model migration pays off.** This commit is a 10-line production change because all the infrastructure (`ShelfResponse`, `ShelfPortModelMapper`) was built in PR #324. The second method migration is nearly free.

2. **Test upgrades are the real value of port-model migration.** The old `findAll` test asserted `containsExactly(shelf1, shelf2)` — checking object identity, not correctness. The new test mocks five domain getters and asserts on mapped field values. This is a qualitative leap in test effectiveness.

3. **Documenting decisions during the refactor, not after, captures context accurately.** ADR 0004 was written alongside the code changes. The alternative considered, the commit SHAs referenced, and the follow-up table are all grounded in the actual work — not reconstructed from memory.

4. **Consistent return types across port methods reduce cognitive load.** Having `findShelvesByBookcaseId` return `ShelfResponse` while `findAll` returned `Shelf` was a paper-cut for every developer who touched both paths. Now they match.

5. **Reuse of existing mappers keeps the changeset small.** No new classes were created in the production code — only the wiring in `QueryShelfUseCase.findAll()` changed. This is the benefit of extracting the mapper as a separate class in PR #324 rather than inlining it.

---

## Roadmap

### Immediate follow-ups (today)
- [ ] Strengthen `QueryShelfUseCaseTest.findAll_shouldReturnAllShelves()` to assert all five `ShelfResponse` fields, not just `id()`
- [ ] Add dedicated `ShelfPortModelMapperTest` for edge cases (null/empty bookIds, null label)
- [ ] Consider adding `List.copyOf()` to `ShelfPortModelMapper.toShelfResponse()` for defensive immutability

### Short-term hardening (this week)
- [ ] Migrate `findShelfById()` to return `Optional<ShelfResponse>` — last remaining query method returning a domain type
- [ ] Remove `Shelf` import from `ShelfFacade.java` once `findShelfById` migration is complete
- [ ] Evaluate whether `ShelfSummary` should become a port model or remain as a domain projection
- [ ] Remove old `toShelfOption(Shelf)` overload from `ShelfResponseMapper` (web layer) once all callers are migrated

### Strategic refactors (later)
- [ ] Apply port-model pattern to `BookcaseFacade` — return `BookcaseResponse` instead of raw `Bookcase`
- [ ] Apply to `BookFacade` — return `BookResponse` for query methods
- [ ] Standardize `inboundPortModels` package naming (consider `response/` or `model/` per ADR 0004 candidate)
