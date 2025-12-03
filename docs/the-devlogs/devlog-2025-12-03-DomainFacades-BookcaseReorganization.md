# Devlog: Domain Facades and Bookcase Package Reorganization

**Date:** 2025-12-03  
**Focus:** Establishing domain boundaries with facade interfaces and restructuring the bookcase module  
**Commit Type:** `refactor`

---

## Summary

Introduced facade interfaces as contracts for cross-domain communication and reorganized the bookcase package from a flat structure into layered sub-packages. Also cleaned up dead code by removing unused `Genre` and `User` classes.

---

## What Changed

### New Facade Interfaces

Created facade interfaces for each domain to define explicit boundaries:

```java
// AuthorFacade.java
public interface AuthorFacade {
    AuthorEntity getOrCreateAuthorEntity(Author author);
}

// BookFacade.java (placeholder)
public class BookFacade {
}

// BookcaseFacade.java (placeholder)
public class BookcaseFacade {
}

// ShelfFacade.java (placeholder)
public class ShelfFacade {
}
```

### Bookcase Package Restructure

**Before:**
```
com.penrose.bibby.library.bookcase/
├── BookCaseController.java
├── Bookcase.java
├── BookcaseDTO.java
├── BookcaseEntity.java
├── BookcaseMapper.java
├── BookcaseRepository.java
└── BookcaseService.java
```

**After:**
```
com.penrose.bibby.library.bookcase/
├── BookcaseFacade.java
├── controller/
│   └── BookCaseController.java
├── domain/
│   └── Bookcase.java
├── dto/
│   └── BookcaseDTO.java
├── mapping/
│   └── BookcaseMapper.java
├── persistance/          # Note: typo to be fixed
│   └── BookcaseEntity.java
├── repository/
│   └── BookcaseRepository.java
└── service/
    └── BookcaseService.java
```

### Removed Dead Code

Deleted unused classes that were cluttering the codebase:

- `com.penrose.bibby.library.genre.Genre` - 37 lines of unused code
- `com.penrose.bibby.library.user.User` - Empty placeholder class

---

## Why Facades?

### The Problem They Solve

Without facades, domains reach directly into each other's internals:

```java
// BookService directly uses ShelfService, ShelfEntity, ShelfRepository...
// Creates tight coupling between domains
```

With facades, domains communicate through explicit contracts:

```java
// BookService uses ShelfFacade.getShelfEntityById()
// Doesn't know or care how Shelf domain implements it
```

### Bounded Context Preparation

This is groundwork for proper DDD bounded contexts. Each domain module will eventually:

1. Expose a facade interface (the "public API" of that domain)
2. Hide internal implementation details
3. Communicate with other domains only through facades

### The AuthorFacade Contract

`AuthorFacade` is the most developed, defining a key cross-domain operation:

```java
AuthorEntity getOrCreateAuthorEntity(Author author);
```

This method encapsulates the "find existing or create new author" logic that the Book domain needs but shouldn't own. The Author domain owns the rules about author identity and deduplication.

---

## Why Package-by-Feature Sub-packages?

### Flat Structure Problems

With everything in one package:
- Hard to see architectural layers at a glance
- Easy to create inappropriate dependencies
- No visual cue about what's public vs internal

### Layered Sub-packages Benefits

The new structure makes architectural intent visible:

| Package | Purpose | Should Be Accessed By |
|---------|---------|----------------------|
| `controller/` | HTTP endpoints | External (Spring MVC) |
| `domain/` | Business logic | Internal + Facade |
| `dto/` | Data transfer | Controller ↔ Service |
| `mapping/` | Entity ↔ Domain conversion | Service layer |
| `persistance/` | JPA entities | Repository only |
| `repository/` | Data access | Service layer |
| `service/` | Use case orchestration | Controller + Facade |

---

## Struggle Journal

### Challenge: Facade as Interface vs Class

Started with `AuthorFacade` as an interface because it defines a contract. But created the others (`BookFacade`, `BookcaseFacade`, `ShelfFacade`) as empty classes.

**The inconsistency**: Interfaces define contracts without implementation. Classes suggest there's something implemented.

**Resolution for next commit**: Convert all facades to interfaces with method signatures that define cross-domain operations.

### Challenge: Where Does the Facade Live?

Options considered:
1. At package root (chose this) - `bookcase/BookcaseFacade.java`
2. In a `facade/` sub-package - `bookcase/facade/BookcaseFacade.java`
3. In `api/` sub-package - `bookcase/api/BookcaseFacade.java`

**Rationale for root placement**: The facade IS the public interface of the domain. Putting it at the root makes it the first thing you see, signaling "this is how you interact with this domain."

### Challenge: The `persistance` Typo

Created the package as `persistance` instead of `persistence`. Small thing, but it'll bug me every time I see it.

**TODO**: Fix in follow-up commit when updating imports.

---

## Dead Code Removal Rationale

### Genre.java

A fully-implemented class with:
- Private fields (`id`, `genreName`, `genreDescription`)
- Constructor
- Getters and setters

But zero usages anywhere in the codebase. Likely created during initial design but never integrated. Removed rather than carrying forward technical debt.

### User.java

Empty class—just a package declaration and empty class body. Placeholder from early project setup that never materialized. No reason to keep it.

**Lesson**: Regularly audit for dead code. It accumulates silently and creates confusion about what's actually used.

---

## Interview Talking Points

### "What's the purpose of the facade pattern in your architecture?"

> Facades define the public contract of a domain module. When the Book domain needs to work with authors, it calls `AuthorFacade.getOrCreateAuthorEntity()` rather than reaching into Author's internal services or repositories. This creates a clear boundary—I can refactor Author's internals without breaking Book, as long as the facade contract holds.

### "Why did you delete the Genre class? Couldn't it be useful later?"

> Dead code is worse than no code. It creates confusion about what's actually used, clutters navigation, and can mislead future developers into thinking it's integrated when it isn't. If I need genre functionality later, I'll design it properly for that use case rather than resurrecting speculative code.

### "How do you decide the granularity of sub-packages?"

> I follow the principle that package structure should reveal architectural intent. When someone opens the `bookcase/` package, they should immediately understand: "There's a domain model, a service that orchestrates it, a repository for persistence, DTOs for the API boundary, and a controller for HTTP." Each sub-package has a clear, single purpose.

---

## What's Next

1. **Fix the `persistance` → `persistence` typo**
2. **Update imports across codebase** to use new package paths
3. **Convert placeholder facades to interfaces** with meaningful method signatures
4. **Apply same structure to other domains** (Author, Shelf, Book)

---

## Files Changed

- 4 files added (facade interfaces/classes)
- 7 files renamed (bookcase package reorganization)
- 2 files deleted (Genre.java, User.java)

---

## Architectural Note

This commit does two things that could have been separate:
1. Introduce facades
2. Reorganize bookcase package

In retrospect, atomic commits would be cleaner. Noting for future discipline—each logical change deserves its own commit for clearer git history and easier rollback if needed.
