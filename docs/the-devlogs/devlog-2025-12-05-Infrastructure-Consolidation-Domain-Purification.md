# Devlog: Infrastructure Consolidation and Domain Purification

**Date:** 2025-12-05  
**Focus:** Architectural cleanup—organizing infrastructure adapters and purifying domain models  
**Commit Type:** `refactor`

---

## Summary

Two related refactors to strengthen hexagonal architecture boundaries:
1. Consolidated web controllers and config under `infrastructure/` package
2. Purified the `Shelf` domain model by removing infrastructure dependencies

---

## Part 1: Infrastructure Package Consolidation

### What Changed

Relocated all web-related code under a unified `infrastructure/` package:

**Before:**
```
com.penrose.bibby/
├── util/
│   └── WebClientConfig.java
├── web/
│   ├── author/AuthorController.java
│   ├── book/BookController.java
│   ├── book/BookImportController.java
│   ├── bookcase/BookCaseController.java
│   └── shelf/ShelfController.java
└── library/
    └── [domain modules]
```

**After:**
```
com.penrose.bibby/
├── infrastructure/
│   ├── config/
│   │   └── WebClientConfig.java
│   └── web/
│       ├── author/AuthorController.java
│       ├── book/BookController.java
│       ├── book/BookImportController.java
│       ├── bookcase/BookCaseController.java
│       └── shelf/ShelfController.java
├── cli/
│   └── [CLI adapters]
└── library/
    └── [domain modules]
```

### Why This Matters

In hexagonal architecture, controllers are **driving adapters**—they adapt external requests (HTTP) to calls on your application layer. Grouping them under `infrastructure/` makes this role explicit.

Now the top-level package structure tells the architectural story:

| Package | Role |
|---------|------|
| `library/` | Domain modules (the hexagon core) |
| `cli/` | Driving adapter (CLI protocol) |
| `infrastructure/web/` | Driving adapter (HTTP protocol) |
| `infrastructure/config/` | Framework configuration |

CLI and web are parallel—both are entry points that call into the domain, just over different protocols.

---

## Part 2: Shelf Domain Purification

### What Changed

Cleaned up the `Shelf` domain class to remove infrastructure leakage:

**Removed imports:**

```java
-
- 
```

**Simplified constructor:**
```java
// Before
public Shelf(Bookcase bookCase, String shelfLabel, int shelfPosition, int bookCapacity)

// After
public Shelf(String shelfLabel, int shelfPosition, int bookCapacity)
```

**Deleted dead code:**
- Commented-out `addBook()` method
- Commented-out `setBookCase()` method
- Empty `addToShelf(BookEntity book)` method
- Commented-out bookCase field references

### Before vs After

**Before:**

```java
package com.penrose.bibby.library.shelf.domain;

import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;  // ❌ Infrastructure leak
import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;              // ❌ Cross-aggregate coupling

public class Shelf {
    private Long id;
    //    private Bookcase bookCase;  // Commented but still imported
    private String shelfLabel;
    // ...

    public Shelf(Bookcase bookCase, String shelfLabel, int shelfPosition, int bookCapacity) {
        // ...
    }

    public void addToShelf(BookEntity book) {  // ❌ Infrastructure type in domain
        // Empty method
    }

//    public void addBook(Book book){  // Dead code
//        ...
//    }
}
```

**After:**

```java
package com.penrose.bibby.library.shelf.domain;

import com.penrose.bibby.library.cataloging.book.core.domain.Book;  // ✓ Domain-to-domain only

import java.util.List;

public class Shelf {
    private Long id;
    private String shelfLabel;
    private String shelfDescription;
    private int shelfPosition;
    private int bookCapacity;
    private List<Book> books;

    public Shelf(String shelfLabel, int shelfPosition, int bookCapacity) {
        this.shelfLabel = shelfLabel;
        this.shelfPosition = shelfPosition;
        this.bookCapacity = bookCapacity;
    }

    public boolean isFull() {
        return books.size() >= bookCapacity;
    }

    // ... clean getters/setters
}
```

### Why This Matters

**Domain purity principle:** Domain classes should only depend on:
1. Other domain classes (same or different bounded context)
2. Java standard library
3. Value objects and primitives

They should **never** depend on:
- JPA entities (`BookEntity`)
- Framework annotations
- Infrastructure concerns

The `Shelf` domain now only knows about `Book` (another domain model)—no infrastructure types leak in.

---

## Architectural Insight: Aggregate References

The original code had `Shelf` holding a direct reference to `Bookcase`:

```java
private Bookcase bookCase;

public Shelf(Bookcase bookCase, ...) {
    this.bookCase = bookCase;
}
```

**Problem:** This creates tight coupling between aggregates. In DDD, aggregates should reference each other by ID, not by direct object reference.

**Better approach:**
```java
private Long bookcaseId;  // Reference by ID

public Shelf(Long bookcaseId, String shelfLabel, ...) {
    this.bookcaseId = bookcaseId;
}
```

This keeps aggregates loosely coupled. If you need the full `Bookcase` object, you fetch it through a repository—the domain model doesn't hold the reference directly.

The current refactor removed the `Bookcase` parameter entirely. If shelf-to-bookcase association is needed later, it should be added as an ID reference, not an object reference.

---

## Interview Talking Points

### "Why move controllers to infrastructure?"

> In hexagonal architecture, controllers are driving adapters—they translate HTTP requests into calls on the application layer. They're not part of the domain; they're infrastructure that connects the outside world to the domain. Putting them in `infrastructure/web/` makes this explicit. Now I have `cli/` and `infrastructure/web/` as parallel entry points, both calling into `library/` where the domain lives.

### "Why is importing BookEntity in a domain class problematic?"

> It breaks the dependency rule. Domain classes should be the innermost layer—they shouldn't know about persistence concerns. If my `Shelf` domain imports `BookEntity`, I've coupled my business logic to my database schema. If I change how I persist books, I have to touch domain code. By keeping the domain pure, I can change infrastructure without touching business rules.

### "What's wrong with aggregates holding references to other aggregates?"

> It creates tight coupling and makes it unclear who owns what. In DDD, aggregates are consistency boundaries—they should be loadable and savable independently. If `Shelf` holds a direct `Bookcase` reference, loading a shelf means loading its bookcase too. That's a hidden dependency. Using ID references keeps aggregates independent—if I need the bookcase, I explicitly fetch it. It's more honest about the actual data access patterns.

---

## Files Changed

### Part 1: Infrastructure Consolidation
| File | Change |
|------|--------|
| `WebClientConfig.java` | `util/` → `infrastructure/config/` |
| `AuthorController.java` | `web/author/` → `infrastructure/web/author/` |
| `BookController.java` | `web/book/` → `infrastructure/web/book/` |
| `BookImportController.java` | `web/book/` → `infrastructure/web/book/` |
| `BookCaseController.java` | `web/bookcase/` → `infrastructure/web/bookcase/` |
| `ShelfController.java` | `web/shelf/` → `infrastructure/web/shelf/` |

### Part 2: Domain Purification
| File | Change |
|------|--------|
| `Shelf.java` | Removed imports, simplified constructor, deleted dead code |

---

## Lessons Learned

1. **Package structure is documentation.** When someone opens the project, `infrastructure/web/` immediately communicates "these are HTTP adapters." `util/` communicates nothing.

2. **Commented code is debt.** The `Shelf` class had multiple commented-out methods that were never coming back. They cluttered the file and created confusion about what was actually used. Delete aggressively.

3. **Infrastructure leaks are subtle.** It's easy to accidentally import an entity class in a domain model—the code compiles fine. Regular audits of import statements catch these violations before they spread.

---

## What's Next

1. **Audit other domain models** for infrastructure imports
2. **Add ArchUnit tests** to enforce package dependencies
3. **Consider ID references** for remaining cross-aggregate relationships
4. **Apply same infrastructure pattern** to any remaining scattered config classes
