# Devlog: Fix ID Write-back, userId Association, and Repository Contract Hardening

Date: 2026-02-24
Branch: `refactor/bookcase-domain-model-and-use-cases`
Range: `ea561f8..HEAD` (commits added after PR #317 merge, merged to main via PR #318)
Commits:
- `a326ac8` — Add userId field to Bookcase domain model
- `83e1fd7` — Return saved Bookcase from repository and update use case
- `79404a9` — Update tests for userId parameter and save return type
- `0c2c2ff` — Document refactor of Bookcase domain model and use-case extraction

---

## Summary

**What problem was I solving?**

Two production bugs were identified in the previous session's devlog (2026-02-23) and addressed here:

1. **ID write-back bug**: `BookcaseRepositoryImpl.save(Bookcase)` called `bookcaseJpaRepository.save(entity)` but never read the JPA-generated `bookcaseId` back into the `Bookcase` domain object. In production, `bookcase.getBookcaseId()` was `null` after every save, meaning `shelfAccessPort.createShelf(null, ...)` was called for every shelf.
2. **Silent data-loss bug**: `BookcaseMapper.toEntity()` mapped only 4 of 6 fields — `bookcaseZone` and `bookcaseIndex` were silently omitted on every save.

Additionally, `Bookcase` had no `userId` field, making it impossible to associate a bookcase with an owner at the domain level.

**What was the "before" state?**

- `BookcaseRepository.save()` was `void`; callers could not retrieve the persisted entity with its generated ID.
- `CreateBookcaseUseCase` called `bookcaseRepository.save(bookcase)` then immediately used `bookcase.getBookcaseId()` — which was always `null` because save returned nothing and didn't mutate the input.
- `BookcaseMapper.toEntity()` set only `shelfCapacity`, `bookCapacity`, `bookcaseLocation`, and `bookcaseId` (the last one wrong — JPA generates this, so setting it pre-save is a no-op at best and a conflict at worst). `bookcaseZone` and `bookcaseIndex` were dropped every time.
- Tests masked the save bug using `doAnswer` to manually set `bookcaseId` on the passed `Bookcase` — a test-only workaround that hid the production failure.
- `Bookcase` had no ownership concept (`userId`); the domain model couldn't express "this bookcase belongs to user X."

**Most important outcomes:**

- `BookcaseRepository.save()` now returns `Bookcase` — the repository returns a fully hydrated domain object with the JPA-generated ID.
- `BookcaseRepositoryImpl.save()` properly chains: `toDomain(jpaRepo.save(toEntity(bookcase)))`, writing the generated ID back through the return value.
- `BookcaseMapper.toEntity()` now maps all 6 fields including `bookcaseZone` and `bookcaseIndex`. No more silent data loss.
- `CreateBookcaseUseCase` re-assigns `bookcase = bookcaseRepository.save(bookcase)`, so subsequent shelf creation receives the real `bookcaseId`.
- `Bookcase` domain model gains `userId` (Long) with full constructor support, getter/setter, and `toString()`.
- `BookcaseMapper.toDomain()` carries `userId` from entity to domain.
- Tests simplified: `doAnswer` workaround replaced with clean `when(repo.save(...)).thenReturn(savedBookcase)`.
- ADR-0003 written documenting all architectural decisions from both sessions.

---

## Per-Commit Breakdown

---

### Commit `a326ac8` — Add userId field to Bookcase domain model

**Intent:** Promote `Bookcase` to a user-aware domain model and fix the silent field-omission bug in `BookcaseMapper.toEntity()`.

**Files touched:**

| File | Reason |
|---|---|
| `core/domain/model/Bookcase.java` | Add `userId` field, update 6→7 arg constructor, add getter/setter, add `toString()` |
| `core/domain/BookcaseMapper.java` | Pass `userId` through `toDomain()`; fix `toEntity()` to map `bookcaseZone`, `bookcaseIndex`; add SLF4J logger |
| `infrastructure/entity/BookcaseEntity.java` | Add `toString()` for logging support |

**Key code changes:**

`Bookcase.java` — constructor signature changed from 6 to 7 args:
```java
public Bookcase(
    Long bookcaseId,
    Long userId,          // NEW
    int shelfCapacity,
    int bookCapacity,
    String bookcaseLocation,
    String bookcaseZone,
    String bookcaseIndex)
```
`toString()` added for structured logging (does not include `userId` — minor omission, see risks).

`BookcaseMapper.toEntity()` — fixed to map all fields:
```java
bookcaseEntity.setUserId(bookcase.getUserId());
bookcaseEntity.setBookcaseZone(bookcase.getBookcaseZone());
bookcaseEntity.setBookcaseIndex(bookcase.getBookcaseIndex());
```
Note: `setBookcaseId(...)` is intentionally **removed** here — JPA auto-generates the ID via `@GeneratedValue`. Setting it before `save()` would be ignored for new entities and could cause confusion for updates.

`BookcaseMapper.toDomain()` — now passes `bookcaseEntity.getUserId()` as second arg to `Bookcase` constructor.

`BookcaseEntity.toString()` — added standard format including all 7 fields. Used by `BookcaseMapper`'s new `logger.info("Mapping Bookcase to Entity: {}", bookcaseEntity)`.

**Architecture notes:**

- `Bookcase` now carries ownership (`userId`), making it possible to filter bookcases by user at the domain level without re-querying infrastructure.
- `BookcaseMapper` still resides in `core.domain` and imports both `BookcaseEntity` (infrastructure) and `BookcaseDTO` (API). ADR-0003 explicitly justifies this as the anti-corruption boundary. This is a pragmatic decision; the strategic refactor would be to move `BookcaseMapper` to `core.application` or a dedicated mapping module.
- Adding a SLF4J logger to a `static`-method-only class (`BookcaseMapper`) is slightly unusual. Since all methods are static, the logger is declared `final static private`, which is correct. However, the log statement fires in `toEntity()` only, not `toDomain()` or `toDTO()` — inconsistent coverage.

**Risk / edge cases:**

- **Breaking constructor change**: All callers of `new Bookcase(...)` must now pass `userId` as the second argument. Commit `79404a9` handles this for all test files; the question is whether any production call sites outside of `BookcaseMapper` construct `Bookcase` directly — see analysis below.
- `Bookcase.toString()` omits `userId`. If this string is used in any security-sensitive logging, the omission is safe; if it's used for debugging user-ownership issues, it will be misleading.
- `toEntity()` removes the `setBookcaseId(...)` call. This is correct for INSERT (new entities) but means that if `save()` is ever called for an UPDATE path (existing entity with known ID), the entity will be treated as a new insert by JPA. Currently `save()` is only called from `createNewBookCase()` where `bookcaseId` is always `null`, so this is safe — but it warrants a comment.

**Verification:**

```bash
mvn test -pl . -Dtest=BookcaseTest,BookcaseServiceTest,CreateBookcaseUseCaseTest
```

---

### Commit `83e1fd7` — Return saved Bookcase from repository and update use case

**Intent:** Fix the ID write-back bug by changing `BookcaseRepository.save()` return type from `void` to `Bookcase`, and fixing the null-pointer risk in `findBookcaseByBookcaseLocation`.

**Files touched:**

| File | Reason |
|---|---|
| `core/ports/outbound/BookcaseRepository.java` | Change `void save(Bookcase)` → `Bookcase save(Bookcase)` |
| `infrastructure/adapter/outbound/BookcaseRepositoryImpl.java` | Implement returned save; add null-guard to findByLocation |
| `core/application/usecases/CreateBookcaseUseCase.java` | Re-assign `bookcase = repo.save(bookcase)`; pass `userId`; clean up logging |

**Key code changes:**

`BookcaseRepository.java` (outbound port):
```java
// Before
void save(Bookcase bookcaseEntity);
// After
Bookcase save(Bookcase bookcase);
```
This is a contract change on an outbound port. All implementations and all callers must be updated.

`BookcaseRepositoryImpl.save()`:
```java
public Bookcase save(Bookcase bookcase) {
    Bookcase savedBookcase = BookcaseMapper.toDomain(
        bookcaseJpaRepository.save(BookcaseMapper.toEntity(bookcase)));
    logger.info("Bookcase saved successfully with ID: {}", savedBookcase.getBookcaseId());
    return savedBookcase;
}
```
This is the core fix. `bookcaseJpaRepository.save(entity)` returns the JPA-managed entity with the generated ID. `BookcaseMapper.toDomain(...)` converts it to a domain object. The caller now gets back a `Bookcase` with a real `bookcaseId`.

`BookcaseRepositoryImpl.findBookcaseByBookcaseLocation()` — null-guard added:
```java
// Before: NPE if JPA returns null
return BookcaseMapper.toDomain(jpaRepo.findBookcaseEntityByBookcaseLocation(loc));

// After: null-safe with a warning log
if (jpaRepo.findBookcaseEntityByBookcaseLocation(loc) != null) {
    return BookcaseMapper.toDomain(jpaRepo.findBookcaseEntityByBookcaseLocation(loc));
} else {
    logger.warn("No bookcase found at location: {}", bookcaseLocation);
    return null;
}
```
Note: This calls the JPA repository **twice** when the result is non-null (one for the null check, one for the actual fetch). This is a minor inefficiency — see micro-fixes.

`CreateBookcaseUseCase.createNewBookCase()`:
```java
// Before: save returned void, ID stayed null
bookcaseRepository.save(bookcase);
for (int i = 1; i <= bookcase.getShelfCapacity(); i++) {
    shelfAccessPort.createShelf(bookcase.getBookcaseId(), ...); // was null!
}

// After: re-assign from save result
bookcase = bookcaseRepository.save(bookcase);
for (int i = 1; i <= bookcase.getShelfCapacity(); i++) {
    shelfAccessPort.createShelf(bookcase.getBookcaseId(), ...); // now has real ID
}
```
Also: `new Bookcase(null, userId, shelfCapacity, ...)` — `userId` is now passed from the use case parameter.

Minor: The unused import `org.apache.logging.slf4j.SLF4JLogger` was added alongside the correct `org.slf4j.Logger`. This is dead code — see micro-fixes.

**Architecture notes:**

- Changing the outbound port contract (`save()` return type) is an architecture-level decision. The port now has two responsibilities: persist the aggregate and return its identity-enriched state. This is a common pattern in hexagonal architecture (sometimes called "create and return"). It is appropriate here because JPA generates the ID and the domain cannot know it without a round-trip.
- An alternative would have been to generate the ID in the domain layer (e.g., UUID strategy) before calling `save()`, keeping `save()` as `void`. That approach avoids the return-type coupling but requires a different ID strategy. Since this project uses database-assigned `Long` IDs, the return-type approach is the pragmatic correct choice.
- The null return from `findBookcaseByBookcaseLocation` is still unguarded in the caller (`CreateBookcaseUseCase` already handles `null` as "not found"). The broader null-vs-Optional question remains open.

**Risk / edge cases:**

- **Double JPA query in `findBookcaseByBookcaseLocation`**: The null-guard calls `findBookcaseEntityByBookcaseLocation` once to check and again to retrieve. Under concurrent writes, the second call could theoretically return a different result. Simple fix: extract to a local variable.
- If `bookcaseJpaRepository.save(entity)` throws a `DataIntegrityViolationException` (e.g., duplicate unique constraint), it will propagate up through `BookcaseRepositoryImpl.save()` as an unchecked Spring exception. `CreateBookcaseUseCase` currently does a pre-check via `findBookcaseByBookcaseLocation`, but there is a TOCTOU (time-of-check-time-of-use) window under concurrency. No change here, but worth noting.
- The stray `import org.apache.logging.slf4j.SLF4JLogger;` in `CreateBookcaseUseCase` compiles (it's on the classpath via spring-boot-starter) but is never used.

**Verification:**

```bash
mvn test -Dtest=CreateBookcaseUseCaseTest
```

---

### Commit `79404a9` — Update tests for userId parameter and save return type

**Intent:** Propagate the breaking `Bookcase` constructor change (new `userId` as 2nd arg) across all 5 affected test files, and simplify the `CreateBookcaseUseCaseTest` mock from `doAnswer` to `when/thenReturn`.

**Files touched:**

| File | Changes |
|---|---|
| `BookcaseTest.java` | 3 constructor calls: `new Bookcase(1L, 5, ...)` → `new Bookcase(1L, null, 5, ...)` |
| `BookcaseServiceTest.java` | 5 constructor calls updated to include `1L` as userId |
| `QueryBookcaseUseCaseTest.java` | 8 constructor calls updated to include `1L` as userId |
| `CreateBookcaseUseCaseTest.java` | `doAnswer` mock replaced; constructor calls updated |
| `PromptOptionsTest.java` | 7 constructor calls updated to include `1L` as userId |

**Key code changes:**

`CreateBookcaseUseCaseTest` — mock simplification:
```java
// Before (workaround for void save):
doAnswer(invocation -> {
    Bookcase b = invocation.getArgument(0);
    b.setBookcaseId(100L);
    return null;
}).when(bookcaseRepository).save(any(Bookcase.class));

// After (clean, since save now returns Bookcase):
Bookcase savedBookcase = new Bookcase(100L, userId, shelfCapacity, ...);
when(bookcaseRepository.save(any(Bookcase.class))).thenReturn(savedBookcase);
```
This is a meaningful improvement. The `doAnswer` pattern was a code smell that indicated a design problem (void save with side-effect mutation). The `when/thenReturn` pattern is idiomatic Mockito and clearly expresses intent.

**Architecture notes:**

- All test `Bookcase` instances now carry a placeholder `userId` of `1L`. This is acceptable for unit tests but test data factories or builders would make future constructor changes less painful to propagate.
- `BookcaseTest.java` uses `null` for `userId` — correct since those tests verify `shelfCapacity` clamping behavior, where `userId` is irrelevant.

**Risk / edge cases:**

- No production risk. These are pure test changes.
- If the `Bookcase` constructor is extended again in the future, all these call sites will need updating. Consider a `BookcaseBuilder` or test factory to reduce the blast radius of future domain model changes.

**Verification:**

```bash
mvn test
# Expected: 141 tests, 0 failures
```

---

### Commit `0c2c2ff` — Document refactor of Bookcase domain model and use-case extraction

**Intent:** Add ADR-0003, persist the prior session's devlog, and apply minor code cleanups observed during review.

**Files touched:**

| File | Reason |
|---|---|
| `docs/ADR-0003-bookcase-domain-model-enrichment-and-use-case-extraction.md` | New — 219-line ADR documenting all decisions from both sessions |
| `docs/the-devlogs/2026-02-23__...md` | Previous session's devlog committed to docs |
| `CreateBookcaseUseCase.java` | Minor: rename `log` → `logger`; remove an unused blank line; fix indentation |
| `BookcaseMapper.java` | Minor: fix indentation in `toDTO()` |
| `Bookcase.java` | Minor: trailing newline cleanup |
| `BookcaseRepositoryImpl.java` | Minor: whitespace cleanup |
| `BookcaseEntity.java` | Minor: field ordering / whitespace |
| `CreateBookcaseUseCaseTest.java` | Minor: test variable naming/formatting |

**Key code changes:**

`ADR-0003` documents:
- Context: anemic model, infrastructure bleed, DTO bleed, god-class `BookcaseService`
- Decision: enrich `Bookcase`, purge `BookcaseEntity` from ports, decompose into 3 use cases
- Boundary rules: core may not import `BookcaseEntity`, `BookcaseDTO`, or `jakarta.persistence`
- `BookcaseMapper` in `core.domain` is explicitly justified as the anti-corruption layer
- Considered alternatives: keep `Optional`, use UUID IDs, keep god-class service
- Known risks: nullable `findById`, stale Javadoc on `BookcaseFacade`, `BookcaseMapper` layering

**Architecture notes:**

- The ADR correctly captures that `BookcaseMapper` in `core.domain` is a pragmatic compromise (acknowledged anti-corruption layer), not a clean layering. The strategic refactor (move it out) is deferred.
- The ADR references `save()` returning `void` as a known risk and defers the fix — but commits `a326ac8` and `83e1fd7` in this same push already fix it. The ADR was written before those commits and should be updated to reflect the resolved status.

**Risk / edge cases:**

- No behavioral changes. Documentation and cosmetic cleanup only.

---

## End-to-End Flow Analysis

### Create bookcase flow (after this push)

```
CLI: AddBookcaseCommand.run()
  → BookcaseFacade.createNewBookCase(userId, label, zone, index, shelfCap, bookCap, location)
    → BookcaseService.createNewBookCase(...)        [pure delegator]
      → CreateBookcaseUseCase.createNewBookCase(...)
          1. bookcaseRepository.findBookcaseByBookcaseLocation(label) → null (no duplicate)
          2. new Bookcase(null, userId, shelfCap, bookCap*shelfCap, location, zone, index)
          3. bookcase = bookcaseRepository.save(bookcase)
               → BookcaseMapper.toEntity(bookcase)  [maps all 7 fields, ID stays null for JPA]
               → bookcaseJpaRepository.save(entity) → entity WITH generated bookcaseId
               → BookcaseMapper.toDomain(entity)    → Bookcase with real bookcaseId + userId
          4. for i in 1..shelfCapacity:
               shelfAccessPort.createShelf(bookcase.getBookcaseId(), i, "Shelf "+i, bookCap)
          5. return new CreateBookcaseResult(bookcase.getBookcaseId())
```

The critical fix is step 3: the saved entity comes back from JPA with the generated ID, is mapped to domain, and re-assigned to `bookcase`. Step 4 then uses the real ID.

### What changed vs the before state

| Step | Before | After |
|---|---|---|
| `save()` return | `void` | `Bookcase` with JPA-generated ID |
| `bookcase.getBookcaseId()` after save | `null` | Real Long ID |
| `shelfAccessPort.createShelf(id, ...)` | Called with `null` | Called with real ID |
| `toEntity()` zone/index | Silently dropped | Persisted correctly |
| `toDomain()` userId | Not mapped | Carried through |
| `toEntity()` bookcaseId | Set (wrong for new) | Not set (correct for JPA auto-gen) |

---

## Dependency Analysis

**Added dependencies (this push):**
- `org.apache.logging.slf4j.SLF4JLogger` import in `CreateBookcaseUseCase` — unused, should be removed
- `org.slf4j.Logger` / `LoggerFactory` in `BookcaseMapper` — appropriate

**Removed dependencies:**
- `doAnswer` / `void` mock pattern in tests — removed entirely

**Layering violations (carried forward, not introduced here):**
- `BookcaseMapper` in `core.domain` imports `BookcaseEntity` (infrastructure) and `BookcaseDTO` (API). Justified in ADR-0003 as anti-corruption layer. Strategic refactor: move to `core.application` or dedicated mapping module.
- `CreateBookcaseUseCase` is in `core.application` and imports `org.springframework.stereotype.Service` and `org.springframework.web.server.ResponseStatusException`. Spring annotations in the application layer are a soft layering violation — the use case is coupled to Spring's exception model instead of throwing a domain exception.

---

## Micro-fixes Recommended

**1. Remove unused import in `CreateBookcaseUseCase.java`:**
```java
// Remove this line:
import org.apache.logging.slf4j.SLF4JLogger;
```

**2. Fix double-query in `BookcaseRepositoryImpl.findBookcaseByBookcaseLocation()`:**
```java
// Current (queries twice):
if (jpaRepo.findBookcaseEntityByBookcaseLocation(loc) != null) {
    return BookcaseMapper.toDomain(jpaRepo.findBookcaseEntityByBookcaseLocation(loc));
}

// Better (single query):
BookcaseEntity entity = jpaRepo.findBookcaseEntityByBookcaseLocation(loc);
if (entity != null) {
    return BookcaseMapper.toDomain(entity);
}
logger.warn("No bookcase found at location: {}", loc);
return null;
```

**3. Update ADR-0003 to reflect that the save() void return risk is now resolved** — the ADR lists it as a known risk, but it was fixed in the same push.

**4. Add `userId` to `Bookcase.toString()`:**
```java
// Current omits userId; include it for complete debug logging:
return String.format(
    "Bookcase{id=%d, userId=%d, location='%s', zone='%s', index='%s', shelfCapacity=%d, bookCapacity=%d}",
    bookcaseId, userId, bookcaseLocation, bookcaseZone, bookcaseIndex, shelfCapacity, bookCapacity);
```

**5. Replace Spring `ResponseStatusException` in `CreateBookcaseUseCase` with a domain exception:**
```java
// Replace the Spring exception with a domain-specific one:
throw new BookcaseDuplicateException("Bookcase with label already exists: " + label);
// Map to HTTP 409 in the web controller / exception handler
```

---

## Tests

### What exists:

| Test Class | Tests | Coverage |
|---|---|---|
| `BookcaseTest` | 3 | `shelfCapacity` clamping invariant |
| `BookcaseServiceTest` | ~10 | Delegation to use cases |
| `CreateBookcaseUseCaseTest` | 3 | Happy path, duplicate detection, shelf count |
| `QueryBookcaseUseCaseTest` | 11 | All query paths, null returns |
| `PromptOptionsTest` | 5+ | CLI prompt construction from Bookcase list |

**Run all bookcase tests:**
```bash
mvn test -Dtest="BookcaseTest,BookcaseServiceTest,CreateBookcaseUseCaseTest,QueryBookcaseUseCaseTest,PromptOptionsTest"
```

**Run full suite:**
```bash
mvn test
# Expected: 141 tests passing
```

### Tests that should be added:

**`BookcaseMapperTest`** — No tests exist for `BookcaseMapper`. Critical gap given two bugs were in this class.
```java
@Test void toDomain_shouldMapAllSevenFields();
@Test void toEntity_shouldMapAllFieldsExceptId();    // assert bookcaseId is null after mapping
@Test void toEntity_shouldNotDropZoneOrIndex();      // regression for the silent-drop bug
@Test void roundTrip_domainToEntityToDomain_shouldPreserveAllFields();
```

**`BookcaseRepositoryImplTest`** — Integration test (or slice test with `@DataJpaTest`):
```java
@Test void save_shouldReturnBookcaseWithGeneratedId();
@Test void save_shouldPersistAllSevenFields();       // regression for zone/index data loss
@Test void findByLocation_shouldReturnNull_whenNotFound();
```

**`CreateBookcaseUseCaseTest` — additional cases:**
```java
@Test void createNewBookCase_shouldPassUserIdToSavedBookcase();
@Test void createNewBookCase_shouldUseReturnedIdForShelfCreation();  // regression for null-ID bug
```

---

## Lessons Learned

- **A `void save()` on an outbound port is often wrong in systems with database-generated IDs.** If the persistence layer generates an identifier, that identifier needs to travel back to the caller. The void contract forces mutation hacks (writing back through the parameter) or makes callers silently work with null IDs. Prefer `Entity save(Entity)` from the start.
- **Test workarounds that use `doAnswer` to simulate behavior are red flags.** The `doAnswer` mock that manually set `bookcaseId` was masking a production-level null pointer. When a test requires `doAnswer` to simulate what a real implementation would do, it means the real implementation is broken.
- **Silent field omission in mappers is one of the highest-risk bugs in hexagonal architecture.** `toEntity()` dropped two fields with no warning, no exception, and no test coverage. Every new field added to a domain model should have a corresponding mapper test that verifies round-trip fidelity.
- **Constructor changes with many parameters propagate widely.** Adding `userId` as the second constructor argument required updating 20+ call sites across 5 test files. A builder pattern or test factory object (e.g., `BookcaseFixtures.aBookcase()`) would reduce the blast radius of future domain model changes to a single place.
- **Static utility classes with loggers are a code smell when the logger fires in only one of three methods.** `BookcaseMapper.logger` logs in `toEntity()` but not in `toDomain()` or `toDTO()`. If logging is valuable in one direction, it's likely valuable in all three — or should be removed from all three and handled at a higher level.
- **ADRs should be updated when the risks they list are resolved in the same work session.** ADR-0003 lists `save()` returning `void` as a known risk, but the fix was committed in the same push. The ADR reads as if the risk is still open.
- **Double-querying for a null check is a concurrency hazard and a performance inefficiency.** Pattern `if (repo.find(x) != null) { use repo.find(x); }` should always be `val result = repo.find(x); if (result != null) { use result; }`.

---

## Follow-up Action Plan

### Immediate (today)
- Remove the unused `import org.apache.logging.slf4j.SLF4JLogger` from `CreateBookcaseUseCase.java`
- Fix the double-query in `BookcaseRepositoryImpl.findBookcaseByBookcaseLocation()` to use a local variable
- Update ADR-0003 to mark the `save()` void-return risk as **resolved**

### Short-term hardening (this week)
- Write `BookcaseMapperTest` covering all three methods with a round-trip test — this is the highest-priority gap given two bugs were found in `BookcaseMapper`
- Write a `@DataJpaTest` integration test for `BookcaseRepositoryImpl.save()` that asserts the returned `Bookcase` has a non-null ID and all 7 fields populated correctly
- Add `userId` to `Bookcase.toString()` for complete debug logging
- Add `createNewBookCase_shouldUseReturnedIdForShelfCreation()` regression test to `CreateBookcaseUseCaseTest`

### Strategic refactors (later)
- Move `BookcaseMapper` out of `core.domain` to eliminate the layering violation (imports `BookcaseEntity` and `BookcaseDTO`). Target package: `infrastructure.adapter.outbound` for entity mapping, `api.mappers` for DTO mapping, or a dedicated `core.application.mappers` package
- Replace `ResponseStatusException` in `CreateBookcaseUseCase` with a domain-level `BookcaseDuplicateException`, mapped to HTTP 409 in a `@ControllerAdvice` — removes Spring dependency from application layer
- Add a `BookcaseFixtures` or `BookcaseBuilder` test utility to centralize test data creation and reduce the blast radius of future domain model constructor changes
- Consider whether `findById()` and `findBookCaseById()` in `QueryBookcaseUseCase` should be collapsed — they are currently identical delegators
- Evaluate whether `BookcaseFacade` Javadoc should be updated to reflect the now-current return types (it still references old `Optional<BookcaseDTO>` descriptions in some places)
