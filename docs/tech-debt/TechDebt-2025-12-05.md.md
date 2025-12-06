# Technical Debt Register

> Last updated: 2025-12-05
> Philosophy: Debt is a natural byproduct of learning. Track it, prioritize it, tell stories about it.

---

## 🔴 High Priority — Blocking Architectural Goals

### CLI Layer Violates Contract Boundaries
**Location:** `BookCommandLine.java`, `BookcaseCommandLine.java`  
**Symptom:** Direct imports of `AuthorEntity`, `BookcaseEntity`, `AuthorService`, `BookService`  
**Why it matters:** Your devlog says "CLI only sees contracts packages" but code doesn't match. This is the gap between documented architecture and reality.  
**Path forward:** Create `BookFacade`, `AuthorFacade` that expose DTOs. CLI calls facades only.  
**Interview angle:** "I renamed packages to 'contracts' and that exposed coupling I hadn't enforced yet."

---

### Half-Migrated Mapper Infrastructure  
**Location:** `BookMapper` vs `BookMapperTwo`, `AuthorMapper` vs `AuthorMapperTwo`  
**Symptom:** Two parallel mapping approaches exist  
**Why it matters:** Confuses future-you, inconsistent patterns, cognitive load  
**Path forward:** 
1. `git grep "BookMapper " --exclude="*MapperTwo*"` to find usages
2. Migrate all to MapperTwo
3. Delete old mappers
4. Rename MapperTwo → Mapper  
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

### N+1 Query in Book Loading
**Location:** `BookDomainRepositoryImpl`  
**Symptom:** Loading books triggers individual author queries  
**Why it matters:** Performance at scale, demonstrates you understand query optimization  
**Path forward:** Create `AuthorRepository.findAllByBookIds()` for batch loading  
**Interview angle:** "I identified the N+1 problem and here's how I'd solve it with batch fetching."

---

### CliPromptService Dependency Coupling
**Location:** `CliPromptService.java`  
**Symptom:** Injects `BookcaseService`, `ShelfService` to fetch data for prompts  
**Why it matters:** Service depends on services instead of receiving data — harder to test, tighter coupling  
**Path forward:** Refactor methods to accept data as parameters, let callers fetch  
**Interview angle:** "I improved cohesion but accidentally increased coupling. Here's how I recognized and fixed it."

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

### Code Duplication in CLI
**Location:** `BookCommands.java`, `CliPromptService.java`  
**Symptom:** `yesNoOptions()` exists in both classes  
**Why it matters:** DRY violation, maintenance burden  
**Path forward:** Single source of truth in CliPromptService  

---

### Incomplete CLI Refactoring
**Location:** `checkInBook()` method  
**Symptom:** Still has inline ComponentFlow instead of using CliPromptService  
**Why it matters:** Inconsistent with rest of refactoring  
**Path forward:** Extract to CliPromptService like other prompts  

---

## 🟢 Parked — Acknowledged, Not Acting Yet

### Aggregate Loading Strategy
**Question:** Should `Shelf` eagerly load `List<Book>` or hold `List<BookId>`?  
**Why parked:** No performance problems yet, premature optimization  
**Trigger to revisit:** When shelf listing becomes slow, or when you need to demonstrate lazy loading knowledge

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

## ❓ Open Questions — Future Research

- Should domain aggregates hold IDs instead of full objects? (performance vs convenience)
- When is eager loading justified vs lazy loading?
- How to handle cross-aggregate transactions in a modular monolith?

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
| `api` → `contracts` terminology | Renamed all packages | 2025-12-05 |
| Package-by-feature structure | Reorganized all modules | 2025-12-03 |
| Dead code (Genre.java, User.java) | Deleted | 2025-12-05 |

