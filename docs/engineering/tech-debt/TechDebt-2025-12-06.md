# Technical Debt Register

> Last updated: 2025-12-06
> Philosophy: Debt is a natural byproduct of learning. Track it, prioritize it, tell stories about it.

---

## 🔴 High Priority — Blocking Architectural Goals

### Half-Migrated Mapper Infrastructure  
**Location:** `BookMapper` vs `BookMapperTwo`, `AuthorMapper` vs `AuthorMapperTwo`  
**Symptom:** Two parallel mapping approaches exist  
**Why it matters:** Confuses future-you, inconsistent patterns, cognitive load  
**Path forward:** 
1. `git grep "BookMapper " --exclude="*MapperTwo*"` to find usages
2. Migrate all to one mapper
3. Delete redundant mapper
4. Rename to single `BookMapper`  
**Interview angle:** "I learned that half-migrated code is worse than old code."

---

### ShelfDomainRepositoryImpl Incomplete
**Location:** `ShelfDomainRepositoryImpl.java`  
**Symptom:** Duplicate `jpaRepository` field, `save()` method doesn't sync domain changes back to entity  
**Why it matters:** Broken persistence for domain model changes  
**Path forward:** Remove duplicate field, implement proper `save()` with entity update logic  
**Interview angle:** Domain repository pattern, aggregate persistence strategies

---

## 🟡 Medium Priority — Code Smells, Interview Material

### Redundant Service + Facade Dependencies
**Location:** `BookCommandLine.java`  
**Symptom:** Constructor has both `BookService` AND `BookFacade`, both `ShelfService` AND `ShelfFacade`  
**Why it matters:** Facades exist to replace direct service usage—having both defeats the purpose  
**Path forward:** Remove concrete service dependencies, use only facades  
**Interview angle:** "The refactoring wasn't complete until I removed the old dependencies, not just added the new ones."

---

### N+1 Query in Book Loading
**Location:** `BookDomainRepositoryImpl`  
**Symptom:** Loading books triggers individual author queries  
**Why it matters:** Performance at scale, demonstrates you understand query optimization  
**Path forward:** Create `AuthorRepository.findAllByBookIds()` for batch loading  
**Interview angle:** "I identified the N+1 problem and here's how I'd solve it with batch fetching."

---

### ShelfDomainRepositoryImpl in Wrong Package
**Location:** `library/shelf/domain/ShelfDomainRepositoryImpl.java`  
**Symptom:** Implementation class in `domain/` package instead of `infrastructure/`  
**Why it matters:** Domain package should contain only domain logic, not JPA implementations  
**Path forward:** Move to `infrastructure/repository/`  
**Interview angle:** "The interface is domain-owned, but the implementation is infrastructure."

---

### Factory Pattern Inconsistency
**Location:** Various entity/domain classes  
**Symptom:** Some use factories, some use constructors directly  
**Why it matters:** Inconsistent patterns increase cognitive load  
**Path forward:** Audit all entities, standardize on factory approach  
**Interview angle:** Demonstrates systematic thinking about codebase consistency

---

### Validation Logic Scattered
**Location:** Controllers, services, domain objects  
**Symptom:** Same validations duplicated across layers  
**Why it matters:** Invariant enforcement unclear, potential for inconsistency  
**Path forward:** Consolidate validation in domain layer using value objects  
**Interview angle:** "Where should validation live?" is a classic DDD interview question

---

### Missing BookcaseFacade
**Location:** `BookCommandLine.java`, `CliPromptService.java`  
**Symptom:** Still using `BookcaseService` directly while other modules use facades  
**Why it matters:** Inconsistent pattern—Author, Book, Shelf have facades but Bookcase doesn't  
**Path forward:** Create `BookcaseFacade` interface, have `BookcaseService` implement it  
**Interview angle:** "I applied the pattern systematically across all modules."

---

## 🟢 Parked — Acknowledged, Not Acting Yet

### Aggregate Loading Strategy
**Question:** Should `Shelf` eagerly load `List<Book>` or hold `List<BookId>`?  
**Status:** Resolved for now—using `List<Long> bookIds`  
**Trigger to revisit:** When you need full Book objects and lazy loading becomes relevant

---

### Bidirectional Relationship Handling
**Question:** How to keep domain model in sync when related entities change?  
**Why parked:** Current unidirectional approach works, complexity not justified yet  
**Trigger to revisit:** When you need to update a Book and have Shelf reflect it automatically

---

### Set vs List for Authors
**Question:** Should `Book.authors` be a `Set` (uniqueness) or `List` (order)?  
**Why parked:** Works fine as-is, semantic distinction doesn't matter for current features  
**Trigger to revisit:** When duplicate author bugs appear, or when author ordering becomes a feature

---

### Debug Output in Production Code
**Location:** Various files  
**Symptom:** `System.out.println()` calls for debugging  
**Why parked:** Low priority, doesn't affect architecture  
**Trigger to revisit:** Before any demo or code review

---

## ❓ Open Questions — Future Research

- How to handle cross-aggregate transactions in a modular monolith?
- When is eager loading justified vs lazy loading?
- Should facades return domain objects or only DTOs?
- How to version API contracts when modules evolve independently?

---

## 📋 Prioritization Heuristic

For this project, weight toward:

1. **Interview story value** — Does fixing this create a compelling narrative?
2. **Architectural consistency** — Does it close the gap between docs and reality?
3. **Learning depth** — Will fixing it teach you something transferable?

Weight away from:
- Pure performance optimization (no real users)
- Premature abstraction (YAGNI)
- Cosmetic cleanup that doesn't demonstrate skills

---

## ✅ Recently Resolved

| Item | Resolution | Date |
|------|------------|------|
| NPE in `findBooksByShelf()` | Fixed null list initialization, now uses stream mapping | 2025-12-06 |
| CLI Layer Violates Contract Boundaries | Created facades (Author, Book, Shelf), removed all infrastructure imports from CLI. **ArchUnit test now passes.** | 2025-12-06 |
| Infrastructure types in CLI | Replaced `BookEntity`→`BookDTO`, `ShelfEntity`→`ShelfDTO`, `GoogleBooksResponse`→`BookMetaDataResponse` | 2025-12-06 |
| Mapper imports in CLI | Removed `BookMapper`, `ShelfMapper` imports; mapping now internal to services | 2025-12-06 |
| CliPromptService Dependency Coupling | Now uses `ShelfFacade` instead of `ShelfService` directly | 2025-12-06 |
| Constructor bloat (11 deps) | Reduced to 9 by removing mapper dependencies | 2025-12-06 |
| `api` → `contracts` terminology | Renamed all packages | 2025-12-05 |
| Package-by-feature structure | Reorganized all modules | 2025-12-03 |
| Dead code (Genre.java, User.java) | Deleted | 2025-12-05 |
| Aggregate object references | Changed `Shelf.books` and `Book.shelf` to ID references | 2025-12-03 |

---

## 🏆 Milestone: Architecture Enforcement

**Date:** 2025-12-06 @ 1:15 AM

The ArchUnit test `cli_should_not_depend_on_infrastructure()` now passes:

```
✓ 1 test passed, 1 test total, 139ms
```

This means:
- CLI layer imports only from `contracts/` packages
- No entity, mapper, or infrastructure types leak into CLI
- Future violations will fail the build
- Architecture is self-defending

This was a full day of refactoring to achieve. The pattern is now established for all modules.
