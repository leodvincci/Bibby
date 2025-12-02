# Domain/Entity Layer Separation Specification

## Problem Statement

The `Book` domain model currently violates architectural layer separation by directly depending on `AuthorEntity`, a persistence layer class. Domain models should only depend on other domain models, not on JPA entities.

**Current violation:**

java

```java
// Book.java (domain model)
private AuthorEntity authorEntity;  // ❌ Domain → Persistence dependency
```

## Current State Analysis

### Files Affected

- `com.penrose.bibby.library.book.domain.Book` - Domain model with persistence dependency
- `com.penrose.bibby.library.book.BookMapper` - Expects `AuthorEntity` parameter
- `com.penrose.bibby.library.author.AuthorEntity` - Persistence entity that needs domain counterpart

### What Exists

- ✅ `BookMapper` exists and establishes the pattern to follow
- ✅ `AuthorEntity` exists as persistence layer
- ❌ No `Author` domain model
- ❌ No `AuthorMapper`

### Architecture Pattern to Follow

Based on `BookMapper`, the established pattern is:

- Domain models live in their package (e.g., `com.penrose.bibby.library.book.domain.Book`)
- Entities live in their package with "Entity" suffix (e.g., `BookEntity`)
- Mappers are static utility classes with `toDomain()` and potentially `toEntity()` methods
- Mappers handle null checking

## Desired End State

### 1. Create Author Domain Model

**File:** `com.penrose.bibby.library.author.Author`

**Requirements:**

- Plain Java class (no JPA annotations)
- Fields: `id`, `firstName`, `lastName`, `fullName`
- Standard getters/setters
- Constructor(s) matching AuthorEntity
- `toString()`, `equals()`, `hashCode()` based on `id`
- Should NOT reference `AuthorEntity` or any entity classes

**Template to follow:** Structure similar to `Book` domain model (minus the entity references)

### 2. Create AuthorMapper

**File:** `com.penrose.bibby.library.author.AuthorMapper`

**Requirements:**

- Static utility class
- `public static Author toDomain(AuthorEntity e)` method
  - Null check at start
  - Map all fields from AuthorEntity to Author
  - Return Author instance
- Follow exact pattern from `BookMapper.toDomain()`

**Example pattern:**

java

```java
public static Author toDomain(AuthorEntity e) {
    if (e == null) {
        return null;
    }
    Author author = new Author();
    author.setId(e.getAuthorId());
    // ... map remaining fields
    return author;
}
```

### 3. Update Book Domain Model

**File:** `com.penrose.bibby.library.book.domain.Book`

**Changes required:**

- Replace: `private AuthorEntity authorEntity;`
- With: `private Author author;`
- Update getter/setter names: `getAuthor()` / `setAuthor(Author author)`
- Update constructor `Book(Long id, String title, AuthorEntity authorEntity)` parameter type
- Update `toString()` to reference `author` field
- Update any other references to `authorEntity` field

### 4. Update BookMapper

**File:** `com.penrose.bibby.library.book.BookMapper`

**Changes required:**

- Add import: `import com.penrose.bibby.library.author.AuthorMapper;`
- Change line: `book.setAuthor(authorEntity);`
- To: `book.setAuthor(AuthorMapper.toDomain(authorEntity));`
- Keep method signature accepting `AuthorEntity` (that's correct - mapper receives entities)

## Implementation Plan

### Phase 1: Create New Classes

1. Create `Author` domain model
2. Create `AuthorMapper` with `toDomain()` method
3. Verify compilation (no dependencies on these yet)

### Phase 2: Update Existing Classes

1. Update `Book` class to use `Author` instead of `AuthorEntity`
2. Update `BookMapper` to use `AuthorMapper.toDomain()`
3. Fix any compilation errors in dependent code

### Phase 3: Validation

1. Run existing tests (if any)
2. Verify no compilation errors
3. Code review for consistency with established patterns

## Success Criteria

-  `Author` domain model created following Book pattern
-  `AuthorMapper` created following BookMapper pattern
-  `Book.authorEntity` field renamed to `author` with type `Author`
-  `BookMapper` uses `AuthorMapper.toDomain()` for conversion
-  No compilation errors
-  All existing tests pass (if applicable)
-  No domain model has direct dependencies on entity classes
-  Clean git history with descriptive commit messages

## Future Considerations

**After this refactoring, audit:**

- `Genre` - Is this an entity or enum? If entity, needs domain model + mapper
- `Shelf` - Is this an entity or enum? If entity, needs domain model + mapper

**Pattern to watch for:** Any time you see an entity class (JPA annotations, "Entity" suffix) referenced in a domain model, that's a layer violation needing the same treatment.

## Notes for Implementation

- Keep changes minimal and focused
- Follow existing code style and patterns exactly
- Test after each phase before moving to next
- Git commits should map to phases for easy rollback