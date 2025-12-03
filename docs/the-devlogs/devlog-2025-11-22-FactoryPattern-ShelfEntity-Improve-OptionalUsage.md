## Devlog Entry

**Date**: 2025-11-22

### Refactor: Apply Factory Pattern to ShelfEntity and Improve Optional Usage

**Context**

Continuing the systematic application of factory patterns across entity creation and Optional idioms for null safety. This refactoring extends the factory pattern to `ShelfEntity` creation and improves validation logic in `BookService` to use more idiomatic Optional methods.

**Motivation**

- **Consistency**: Apply the same factory pattern used in `BookFactory` and `AuthorEntityFactory` to shelf creation
- **Idiomatic Java**: Use `Optional.ifPresent()` instead of manual presence checks
- **Maintainability**: Centralize entity construction logic in dedicated factory classes
- **SQL Accuracy**: Update native queries to reflect renamed database columns

**Changes Made**

#### 1. Updated Native SQL Query for Renamed Column

**File:** `BookRepository.java`

**Before:**

```sql
SELECT b.book_id, b.title,
       STRING_AGG(CONCAT(a.first_name, ' ', a.last_name), ', ') as authors,
       bc.bookcase_label, s.shelf_label, b.book_status
FROM books b
-- ...
WHERE b.book_id = :bookId
GROUP BY b.book_id, b.title, bc.bookcase_label, s.shelf_label, b.book_status
```

**After:**

```sql
SELECT b.book_id, b.title,
       STRING_AGG(CONCAT(a.first_name, ' ', a.last_name), ', ') as authors,
       bc.bookcase_label, s.shelf_label, b.availability_status
FROM books b
-- ...
WHERE b.book_id = :bookId
GROUP BY b.book_id, b.title, bc.bookcase_label, s.shelf_label, b.availability_status
```

**Impact:** Updated column references from `book_status` to `availability_status` to match the entity field rename from an earlier refactoring. This prevents SQL errors when executing the native query.

#### 2. Improved Optional Usage in Validation

**File:** `BookService.java`

**Before:**

```java
private void validateBookDoesNotExist(BookRequestDTO bookDTO){
    Optional<BookEntity> bookEntity = findBookByTitleIgnoreCase(bookDTO.title());
    if (bookEntity.isPresent()) {
        throw new IllegalArgumentException("Book Already Exists: " + bookDTO.title());
    }
}
```

**After:**

```java
private void validateBookDoesNotExist(BookRequestDTO bookDTO){
    Optional<BookEntity> bookEntity = findBookByTitleIgnoreCase(bookDTO.title());
    bookEntity.ifPresent(existingBook -> {
        throw new IllegalArgumentException("Book Already Exists: " + existingBook.getTitle());
    });
}
```

**Improvements:**

- Replaced manual `if (isPresent())` check with idiomatic `ifPresent()` lambda
- Uses the actual entity's title instead of DTO (handles case normalization better)
- More functional, declarative style
- Reduced from 3 lines to 2 lines

#### 3. Introduced ShelfFactory for Entity Creation

**File:** `BookcaseService.java`

**Before:**

```java
public class BookcaseService {
    private static final Logger log = LoggerFactory.getLogger(BookcaseService.class);
    private final BookcaseRepository bookcaseRepository;
    private final ShelfRepository shelfRepository;
    
    public BookcaseService(BookcaseRepository bookcaseRepository, ShelfRepository shelfRepository) {
        this.bookcaseRepository = bookcaseRepository;
        this.shelfRepository = shelfRepository;
    }
    
    public void addShelf(BookcaseEntity bookcaseEntity, int label, int position){
        ShelfEntity shelfEntity = new ShelfEntity();
        shelfEntity.setBookcaseId(bookcaseEntity.getBookcaseId());
        shelfEntity.setShelfLabel("Shelf " + Integer.valueOf(label).toString());
        shelfEntity.setShelfPosition(position);
        shelfRepository.save(shelfEntity);
    }
}
```

**After (Initial Implementation):**

```java

import com.penrose.bibby.library.shelf.domain.ShelfFactory;
import com.penrose.bibby.library.shelf.infrastructure.repository.ShelfJpaRepository;

public class BookcaseService {
    private final ShelfFactory shelfFactory;
    private static final Logger log = LoggerFactory.getLogger(BookcaseService.class);
    private final BookcaseRepository bookcaseRepository;
    private final ShelfJpaRepository shelfRepository;

    public BookcaseService(BookcaseRepository bookcaseRepository, ShelfJpaRepository shelfRepository, ShelfFactory shelfFactory) {
        this.bookcaseRepository = bookcaseRepository;
        this.shelfRepository = shelfRepository;
        this.shelfFactory = shelfFactory;
    }

    public void addShelf(BookcaseEntity bookcaseEntity, int label, int position) {
        shelfRepository.save(
                shelfFactory.createEntity(
                        bookcaseEntity.getBookcaseId(),
                        position,
                        String.format("Shelf %s", Integer.valueOf(label).toString())
                )
        );
    }
}
```

**Benefits:**

- Centralized shelf creation logic in factory
- Reduced method from 5 lines to 1 line
- Consistent with `BookFactory` and `AuthorEntityFactory` patterns
- Service layer no longer knows entity construction details

#### 4. Code Cleanup

**Minor changes:**

- Removed unnecessary blank line in `BookService.findBookByTitle()`

**Issues Identified During Code Review**

#### Issue 1: Wildcard Import Anti-Pattern

**Problem:**

```java
import com.penrose.bibby.library.shelf.*;
```

Wildcard imports obscure class origins and can cause naming conflicts.

**Corrected:**

```java

```

#### Issue 2: Overcomplicated Type Conversion

**Problem:**

```java
String.format("Shelf %s", Integer.valueOf(label).toString())
```

This performs unnecessary conversions: `int` → `Integer` (boxing) → `String` → formatted String.

**Corrected:**

```java
"Shelf " + label  // Simple and clear
// OR
String.format("Shelf %d", label)  // If you prefer String.format (use %d for int)
```

#### Issue 3: Field Ordering Convention

**Problem:**

```java
private final ShelfFactory shelfFactory;
private static final Logger log = LoggerFactory.getLogger(BookcaseService.class);
private final BookcaseRepository bookcaseRepository;
```

Static fields should come before instance fields.

**Corrected:**

```java
private static final Logger log = LoggerFactory.getLogger(BookcaseService.class);
private final BookcaseRepository bookcaseRepository;
private final ShelfRepository shelfRepository;
private final ShelfFactory shelfFactory;
```

**Final Corrected Implementation**

```java
package com.penrose.bibby.library.bookcase;

import com.penrose.bibby.library.bookcase.infrastructure.BookcaseEntity;
import com.penrose.bibby.library.bookcase.infrastructure.BookcaseRepository;
import com.penrose.bibby.library.shelf.domain.ShelfFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookcaseService {
    private static final Logger log = LoggerFactory.getLogger(BookcaseService.class);
    private final BookcaseRepository bookcaseRepository;
    private final com.penrose.bibby.library.shelf.infrastructure.repository.ShelfJpaRepository shelfRepository;
    private final ShelfFactory shelfFactory;
    private final ResponseStatusException existingRecordError =
            new ResponseStatusException(HttpStatus.CONFLICT, "Bookcase with the label already exist");

    public BookcaseService(
            BookcaseRepository bookcaseRepository,
            com.penrose.bibby.library.shelf.infrastructure.repository.ShelfJpaRepository shelfJpaRepository,
            ShelfFactory shelfFactory
    ) {
        this.bookcaseRepository = bookcaseRepository;
        this.shelfRepository = shelfRepository;
        this.shelfFactory = shelfFactory;
    }

    public void addShelf(BookcaseEntity bookcaseEntity, int label, int position) {
        String shelfLabel = "Shelf " + label;
        shelfRepository.save(
                shelfFactory.createEntity(
                        bookcaseEntity.getBookcaseId(),
                        position,
                        shelfLabel
                )
        );
    }

    // ... other methods
}
```

**Architectural Progress**

Factory pattern now consistently applied across all domain entities:

- ✓ `BookFactory` - book entity creation
- ✓ `AuthorEntityFactory` - author entity creation
- ✓ `ShelfFactory` - shelf entity creation

This creates a uniform approach to entity construction throughout the application.

**Key Learnings**

- **Optional.ifPresent()**: More idiomatic than manual `if (isPresent())` checks
- **Avoid wildcard imports**: They obscure dependencies and can cause conflicts
- **Simplify conversions**: Don't over-engineer type conversions (`int` → `Integer` → `String`)
- **Field ordering**: Static fields before instance fields by convention
- **Factory pattern consistency**: Applying the same pattern across similar operations improves maintainability

**Testing Impact**

The factory pattern simplifies mocking in tests:

```java
@Mock
private ShelfFactory shelfFactory;

@Test
void testAddShelf() {
    when(shelfFactory.createEntity(anyLong(), anyInt(), anyString()))
        .thenReturn(mockShelfEntity);
    // ...
}
```

**Files Modified**

- `BookRepository.java` - updated native SQL query column references
- `BookService.java` - improved Optional usage in validation
- `BookcaseService.java` - introduced ShelfFactory dependency, refactored shelf creation

------

## Git Commit Message

```
refactor: apply factory pattern to shelf creation and improve Optional usage

- Add ShelfFactory dependency to BookcaseService
- Refactor addShelf() to use factory pattern for entity creation
- Use Optional.ifPresent() in validateBookDoesNotExist() for idiomatic null handling
- Update native SQL query to use availability_status column
- Replace explicit imports for shelf package (fix wildcard import)
- Simplify shelf label generation (remove unnecessary type conversions)

This change completes factory pattern application across all entity
creation and improves code idiomaticity with better Optional usage.
```

**Post-Review Corrections Required:**

1. Replace wildcard import with explicit imports
2. Simplify `String.format()` to simple concatenation
3. Reorder fields (static before instance)