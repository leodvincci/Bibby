# Devlog: Hexagonal Architecture - Complete Migration

**Date:** 2025-12-03  
**Focus:** Applying hexagonal (ports & adapters) architecture to all remaining domain modules  
**Commit Type:** `refactor(architecture)`

---

## Summary

Completed the hexagonal architecture migration by restructuring the author, bookcase, and shelf modules to match the pattern established in the book module. All domain modules now follow a consistent structure with clear boundaries between API contracts, application logic, domain core, and infrastructure concerns.

---

## Final Package Structure

### Author Module
```
com.penrose.bibby.library.author/
├── api/
│   └── AuthorFacade.java              # Cross-domain contract
├── application/
│   └── AuthorService.java             # Use case orchestration
├── domain/
│   ├── Author.java                    # Domain model
│   └── AuthorFactory.java             # Domain object creation
└── infrastructure/
    ├── entity/
    │   └── AuthorEntity.java          # JPA entity
    ├── mapping/
    │   ├── AuthorMapper.java          # Entity ↔ Domain
    │   └── AuthorMapperTwo.java       # Alternative mapper
    └── repository/
        └── AuthorRepository.java      # Data access
```

### Shelf Module
```
com.penrose.bibby.library.shelf/
├── api/
│   ├── ShelfFacade.java               # Cross-domain contract
│   ├── ShelfOptionResponse.java       # API response DTO
│   └── ShelfSummary.java              # Summary projection
├── application/
│   └── ShelfService.java              # Use case orchestration
├── domain/
│   ├── Shelf.java                     # Domain model
│   ├── ShelfFactory.java              # Domain object creation
│   ├── ShelfDomainRepository.java     # Domain repository interface
│   └── ShelfDomainRepositoryImpl.java # Domain repository impl
└── infrastructure/
    ├── entity/
    │   └── ShelfEntity.java           # JPA entity
    ├── mapping/
    │   └── ShelfMapper.java           # Entity ↔ Domain
    └── repository/
        └── ShelfJpaRepository.java    # Spring Data JPA
```

### Bookcase Module (Simplified)
```
com.penrose.bibby.library.bookcase/
├── api/
│   ├── BookcaseDTO.java               # API transfer object
│   └── BookcaseFacade.java            # Cross-domain contract
├── application/
│   └── BookcaseService.java           # Use case orchestration
├── domain/
│   └── Bookcase.java                  # Domain model
└── infrastructure/
    ├── BookcaseEntity.java            # JPA entity
    ├── BookcaseMapper.java            # Entity ↔ Domain
    └── BookcaseRepository.java        # Data access
```

### Web Layer
```
com.penrose.bibby.web/
├── author/
│   └── AuthorController.java
├── book/
│   ├── BookController.java
│   └── BookImportController.java
├── bookcase/
│   └── BookCaseController.java
└── shelf/
    └── ShelfController.java
```

---

## Key Decisions Made

### 1. Domain Repository Interface Placement

Placed `ShelfDomainRepository` and `ShelfDomainRepositoryImpl` in the `domain/` package rather than `infrastructure/`.

**Rationale**: The domain repository interface defines the contract the domain needs—it's a **port** in hexagonal terms. The implementation happens to use JPA, but the interface itself is domain-owned.

```
domain/
├── ShelfDomainRepository.java      # Port (interface)
└── ShelfDomainRepositoryImpl.java  # Adapter (implementation)
```

**Trade-off acknowledged**: Having the implementation in domain/ is unusual. Pure hexagonal would put implementations in infrastructure/. However, this keeps the domain self-contained for now. Can revisit if it causes issues.

### 2. Bookcase Infrastructure Flattening

Chose NOT to create sub-packages (entity/, mapping/, repository/) within bookcase's infrastructure. Instead, kept files directly in `infrastructure/`.

**Rationale**: Bookcase is a simpler domain with fewer infrastructure concerns. Creating three sub-packages for three files felt like over-organization. The flat structure is still readable.

**Consistency trade-off**: This means bookcase differs from author/shelf/book in structure depth. Acceptable because:
- Each module can choose appropriate granularity
- The key boundaries (api/application/domain/infrastructure) are consistent
- Can refactor later if bookcase grows

### 3. Wildcard Import Elimination

Converted all wildcard imports to explicit imports throughout the codebase.

**Before:**
```java
import com.penrose.bibby.library.shelf.*;
```

**After:**
```java
import com.penrose.bibby.library.shelf.application.ShelfService;
import com.penrose.bibby.library.shelf.domain.Shelf;
import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.shelf.infrastructure.mapping.ShelfMapper;
```

**Benefits:**
- Clear visibility of exact dependencies
- Easier to track what each class actually uses
- No hidden coupling through wildcards

---

## Files Changed Summary

| Category | Files Modified |
|----------|----------------|
| Author module | 6 files |
| Bookcase module | 5 files |
| Shelf module | 9 files |
| Book module | 2 files (import updates) |
| CLI layer | 4 files |
| Web layer | 4 files |
| Documentation | 3 files |
| Utility | 1 file |

**Total: ~34 files touched**

---

## Architectural Consistency Achieved

All four domain modules now share the same top-level structure:

| Layer | Author | Book | Bookcase | Shelf |
|-------|--------|------|----------|-------|
| `api/` | ✓ | ✓ | ✓ | ✓ |
| `application/` | ✓ | ✓ | ✓ | ✓ |
| `domain/` | ✓ | ✓ | ✓ | ✓ |
| `infrastructure/` | ✓ | ✓ | ✓ | ✓ |

This makes navigation predictable. When looking for:
- **How to call this domain from outside** → check `api/`
- **What operations are available** → check `application/`
- **Core business rules** → check `domain/`
- **Database/external integration** → check `infrastructure/`

---

## Struggle Journal

### Challenge: Circular Dependency Risk

Moving files between packages revealed some tight coupling. For example:
- `ShelfService` needed `BookcaseEntity` and `BookcaseRepository`
- `BookService` needed `ShelfEntity` and `ShelfService`

**Current state**: Cross-domain imports go directly to infrastructure entities. This works but isn't ideal.

**Future improvement**: Use facades for all cross-domain communication:
```java
// Instead of:
import com.penrose.bibby.library.bookcase.infrastructure.BookcaseEntity;

// Should be:
import com.penrose.bibby.library.bookcase.api.BookcaseFacade;
```

### Challenge: Documentation Drift (Again)

Multiple documentation files needed updating:
- `devlog-2025-11-22-FactoryPattern-ShelfEntity-Improve-OptionalUsage.md`
- `devlog-2025-12-03-ImportUpdates-PersistenceTypoFix.md`
- `001-DomainEntity-Layer-Separation.md` (spec document)

**Lesson reinforced**: Docs are part of the codebase. When refactoring, include docs in the scope.

### Challenge: Mapper Duplication

Both `AuthorMapper` and `AuthorMapperTwo` exist. Same pattern in Book module. During this refactor, I moved both but didn't consolidate.

**TODO**: Investigate and potentially merge these mapper classes. The naming suggests `MapperTwo` was created as an alternative during development.

---

## Dependency Flow Verification

Verified dependencies point inward across all modules:

```
Author Module:
✓ api/ → imports from domain/, infrastructure/entity
✓ application/ → imports from infrastructure/, domain/
✓ domain/ → imports from infrastructure/entity (pragmatic violation)
✓ infrastructure/ → imports from domain/

Shelf Module:
✓ api/ → imports from infrastructure/entity
✓ application/ → imports from infrastructure/, api/
✓ domain/ → imports from infrastructure/, book/infrastructure
✓ infrastructure/ → imports from domain/, api/

Bookcase Module:
✓ api/ → no internal imports
✓ application/ → imports from infrastructure/, shelf/
✓ domain/ → no internal imports
✓ infrastructure/ → imports from domain/
```

**Known violations** (pragmatic, documented):
- `AuthorFactory` imports `AuthorEntity` for `fromEntity()` method
- `ShelfFactory` imports `ShelfEntity` for `fromEntity()` method
- `ShelfDomainRepositoryImpl` imports from infrastructure (expected for impl)

---

## Interview Talking Points

### "Walk me through your hexagonal architecture implementation."

> We have four domain modules—Author, Book, Bookcase, and Shelf—each following the same structure. The `api/` package contains facades and DTOs that define how other modules can interact with this domain. `application/` holds services that orchestrate use cases. `domain/` contains the core business logic—entities, factories, and domain services. `infrastructure/` handles persistence and external integrations.

> The web layer lives completely outside the domain modules in a separate `web/` package. Controllers only depend on `api/` and `application/` packages—they never reach into domain internals.

### "What trade-offs did you make?"

> The main one is factory classes importing entity classes. In pure hexagonal, factories would only work with domain objects. But for practical conversion between JPA entities and domain models, having `fromEntity()` methods in factories keeps the mapping logic centralized. I documented this as a pragmatic violation.

> Another trade-off: bookcase has a flatter infrastructure package than the other modules. I optimized for "right-sized structure" rather than rigid consistency. Three files don't need three sub-packages.

### "How would you enforce these boundaries?"

> Right now it's convention. The next step would be adding ArchUnit tests to verify architectural rules programmatically. Something like: "Classes in `domain/` packages cannot import from `infrastructure/` packages except for entity classes." That makes violations fail the build rather than relying on code review.

---

## What's Next

1. **Implement facade contracts**: Convert placeholder facades to real implementations
2. **Eliminate cross-domain entity imports**: Route through facades instead
3. **Consolidate mapper classes**: Merge `*Mapper` and `*MapperTwo` where appropriate
4. **Add ArchUnit tests**: Enforce package dependencies automatically
5. **Consider Java modules**: `module-info.java` for stronger encapsulation

---

## Lessons Learned

1. **Batch similar changes**: Doing all modules at once maintained momentum and ensured consistency. Doing them one at a time over days would have led to drift.

2. **IDE refactoring helps but verify**: IntelliJ's "Move Class" handled most renames, but I still found import issues in test files and documentation that needed manual fixes.

3. **Documentation is code**: Three documentation files needed updating. If I hadn't checked, they'd have stale package paths confusing future readers.

4. **Consistency vs pragmatism**: Not every module needs identical depth. Bookcase being simpler justified a flatter structure. The key is consistent boundaries, not identical file counts.

---

## Architecture Evolution Timeline

```
Before: Flat package structure
    ↓
Step 1: Introduce facade interfaces (scaffolding)
    ↓
Step 2: Reorganize bookcase as pilot
    ↓
Step 3: Apply hexagonal to book module (most complex)
    ↓
Step 4: Apply hexagonal to author, shelf, complete bookcase  ← YOU ARE HERE
    ↓
Next: Enforce with ArchUnit, implement facades
```

The codebase is now architecturally consistent. The foundation is laid for proper bounded contexts and eventual microservices extraction if needed.
